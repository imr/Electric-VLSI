/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Connectivity.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.tool.extract;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.geometry.PolyMerge;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.DRCRules;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.routing.AutoStitch;
import com.sun.electric.tool.routing.Routing;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.awt.geom.Area;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import java.util.prefs.Preferences;

/**
 * This is the Connectivity extractor.
 * 
 * Still to do:
 *    Better way to find large contacts
 *    Serpentine transistors
 *    Cell instances in wiring paths
 *    Nonmanhattan geometry (contacts, transistors)
 *    In via creation, check that the cut/via is the right size
 */
public class Connectivity
{
	private PolyMerge merge;
	private PolyMerge originalMerge;
	private Cell oldCell;
	private Cell newCell;
	private Technology tech;
	private HashMap layerForFunction;
	private Layer polyLayer;
	private Layer tempLayer1, tempLayer2;
	private Layer activeLayer;
	private HashMap arcsForLayer;

	/**
	 * Method to examine the current cell and extract it's connectivity in a new one.
	 */
	public static void extractCurCell()
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null)
		{
			System.out.println("Must be editing a cell with pure layer nodes.");
			return;
		}
		ExtractJob job = new ExtractJob(curCell);
	}

	private static class ExtractJob extends Job
	{
		private Cell cell;

		private ExtractJob(Cell cell)
		{
			super("Extract Connectivity", Extract.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			startJob();
		}

		public boolean doIt()
		{
			Connectivity c = new Connectivity(cell);
			c.doExtract();
			return false;
		}
	}
	
	private Connectivity(Cell oldCell)
	{
		this.oldCell = oldCell;
		tech = oldCell.getTechnology();
		merge = new PolyMerge();

		// find important layers
		polyLayer = null;
		tempLayer1 = tempLayer2 = null;
		activeLayer = null;
		for(Iterator it = tech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			Layer.Function fun = layer.getFunction();
			if (polyLayer == null && fun == Layer.Function.POLY1) polyLayer = layer;
			if (fun == Layer.Function.POLY1 && (layer.getFunctionExtras()&Layer.Function.PSEUDO) != 0) tempLayer1 = layer;
			if (fun == Layer.Function.METAL1 && (layer.getFunctionExtras()&Layer.Function.PSEUDO) != 0) tempLayer2 = layer;
			if (activeLayer == null && fun.isDiff()) activeLayer = layer;
		}
		polyLayer = polyLayer.getNonPseudoLayer();
		activeLayer = activeLayer.getNonPseudoLayer();

		// figure out which arcs to use for a layer
		arcsForLayer = new HashMap();
		for(Iterator it = tech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			Layer.Function fun = layer.getFunction();
			if (fun.isDiff() || fun.isPoly() || fun.isMetal())
			{
				ArcProto.Function oFun = null;
				if (fun.isMetal()) oFun = ArcProto.Function.getMetal(fun.getLevel());
				if (fun.isPoly()) oFun = ArcProto.Function.getPoly(fun.getLevel());
				if (oFun == null) continue;
				ArcProto type = null;
				for(Iterator aIt = tech.getArcs(); aIt.hasNext(); )
				{
					ArcProto ap = (ArcProto)aIt.next();
					if (ap.getFunction() == oFun) { type = ap;   break; }
				}
				if (type != null) arcsForLayer.put(layer, type);
			}
		}

		// build the mapping from any layer to the proper ones for the geometric database
		layerForFunction = new HashMap();
		for(Iterator it = tech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			Layer.Function fun = layer.getFunction();
			if (fun == Layer.Function.DIFFP || fun == Layer.Function.DIFFN)
				fun = Layer.Function.DIFF;
			if (layerForFunction.get(fun) == null)
				layerForFunction.put(fun, layer);
		}
	}

	/**
	 * Top-level method in extracting connectivity from a Cell.
	 * A new version of the cell is created that has real nodes (transistors, contacts) and arcs.
	 * This cell should have pure-layer nodes which will be converted.
	 */
	private void doExtract()
	{
		// create the new version of the cell
		String newCellName = oldCell.getName() + "{" + oldCell.getView().getAbbreviation() + "}";
		newCell = Cell.makeInstance(oldCell.getLibrary(), newCellName);
		if (newCell == null)
		{
			System.out.println("Cannot create new cell: " + newCellName);
			return;
		}

		System.out.println("Creating new version of cell " + newCell.describe() +
			" Old version is " + oldCell.describe());

		// convert the nodes
		HashMap newNodes = new HashMap();
		for(Iterator nIt = oldCell.getNodes(); nIt.hasNext(); )
		{
			NodeInst ni = (NodeInst)nIt.next();
			if (ni.getProto() == Generic.tech.cellCenterNode) continue;

			// save all pure-layer nodes, copy all else
			boolean copyIt = false;
			PrimitiveNode np = null;
			if (ni.getProto() instanceof Cell) copyIt = true; else
			{
				np = (PrimitiveNode)ni.getProto();
				if (np.getFunction() != PrimitiveNode.Function.NODE) copyIt = true;
			}

			// copy it now if requested
			if (copyIt)
			{
				NodeInst newNi = NodeInst.makeInstance(ni.getProto(), ni.getAnchorCenter(), ni.getXSizeWithMirror(), ni.getYSizeWithMirror(),
					newCell, ni.getAngle(), ni.getName(), ni.getTechSpecific());
				if (newNi == null)
				{
					System.out.println("Problem creating new cell instance of " + ni.getProto().describe());
					return;
				}
				newNodes.put(ni, newNi);
				continue;
			}

			// extract the geometry from the pure-layer node (has only 1 layer)
			AffineTransform trans = ni.rotateOut();
			Poly [] polys = tech.getShapeOfNode(ni);
			if (polys.length > 0)
			{
				Poly poly = polys[0];

				// get the layer for the geometry
				Layer layer = poly.getLayer();
				if (layer == null) continue;

				// make sure the geometric database is made up of proper layers
				layer = geometricLayer(layer);

				// finally add the geometrty to the merge
				poly.transform(trans);
				Point2D [] points = poly.getPoints();
				for(int i=0; i<points.length; i++)
					points[i].setLocation(DBMath.round(points[i].getX()), DBMath.round(points[i].getY()));
				merge.addPolygon(layer, poly);
			}
		}

		// throw all arcs into the new cell, too
		for(Iterator aIt = oldCell.getArcs(); aIt.hasNext(); )
		{
			ArcInst ai = (ArcInst)aIt.next();
			NodeInst end1 = (NodeInst)newNodes.get(ai.getHead().getPortInst().getNodeInst());
			NodeInst end2 = (NodeInst)newNodes.get(ai.getTail().getPortInst().getNodeInst());
			if (end1 == null || end2 == null) continue;
			PortInst pi1 = end1.findPortInstFromProto(ai.getHead().getPortInst().getPortProto());
			PortInst pi2 = end2.findPortInstFromProto(ai.getTail().getPortInst().getPortProto());
			ArcInst newAi = ArcInst.makeInstance(ai.getProto(), ai.getWidth(), pi1, pi2,
				ai.getHead().getLocation(), ai.getTail().getLocation(), ai.getName());
		}

		// now remember the original merge
		originalMerge = new PolyMerge();
		originalMerge.addMerge(merge, new AffineTransform());

		// start by extracting vias
		System.out.print("Extracting vias...");
		extractVias();
		System.out.println("done");

		// now extract transistors
		System.out.print("Extracting transistors...");
		extractTransistors();
		System.out.println("done");

		// convert any geometry that touches different networks
		System.out.print("Extending geometry (pass 1)...");
		extendGeometry();
		System.out.println("done");

		// look for wires and pins
		System.out.print("Making wires...");
		makeWires();
		System.out.println("done");

		// convert any geometry that touches different networks
		System.out.print("Extending geometry (pass 2)...");
		extendGeometry();
		System.out.println("done");

		// dump any remaining layers back in as extra pure layer nodes
		System.out.print("Converting remaining geometry...");
		convertAllGeometry();
		System.out.println("done");

		// cleanup by auto-stitching
		System.out.print("Connecting everything...");
//		AutoStitch.runAutoStitch(newCell, false, false);
		System.out.println("done");
		
		// show the new version
		WindowFrame.createEditWindow(newCell);
	}

	/********************************************** WIRE EXTRACTION **********************************************/

	private static class TouchingNode
	{
		NodeInst ni;
		PortInst pi;
		double width;
		Point2D endPt;
	}

	private void makeWires()
	{
		// make a list of layers that could be turned into wires
		List wireLayers = new ArrayList();
		for(Iterator lIt = merge.getKeyIterator(); lIt.hasNext(); )
		{
			Layer layer = (Layer)lIt.next();
			Layer.Function fun = layer.getFunction();
			if (fun.isDiff() || fun.isPoly() || fun.isMetal()) wireLayers.add(layer);
		}

		// examine each wire layer
		for(Iterator lIt = wireLayers.iterator(); lIt.hasNext(); )
		{
			Layer layer = (Layer)lIt.next();
			Layer.Function fun = layer.getFunction();

			// figure out which arc proto to use for the layer
			ArcProto ap = (ArcProto)arcsForLayer.get(layer);
			if (ap == null) continue;

			// examine the geometry on the layer
			List polyList = merge.getMergedPoints(layer, true);
			for(Iterator pIt = polyList.iterator(); pIt.hasNext(); )
			{
				PolyBase poly = (PolyBase)pIt.next();

				// convert a polygon to wires
				convertToWires(poly, layer, ap);
			}
		}
	}

	private List findPortInstsTouchingPoint(Point2D pt, Layer layer)
	{
		List touchingNodes = new ArrayList();
		Rectangle2D checkBounds = new Rectangle2D.Double(pt.getX(), pt.getY(), 0, 0);
		for(Iterator it = newCell.searchIterator(checkBounds); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (!(geom instanceof NodeInst)) continue;
			NodeInst ni = (NodeInst)geom;
			if (ni.getProto() instanceof Cell)
			{
				// TODO: recursive algorithm needed to find connectivity to a cell instance
				continue;
			}
			Poly [] polys = tech.getShapeOfNode(ni, null, true, true, null);
			AffineTransform trans = ni.rotateOut();
			for(int i=0; i<polys.length; i++)
			{
				Poly oPoly = polys[i];
				Layer oLayer = geometricLayer(oPoly.getLayer());
				if (layer != oLayer) continue;
				oPoly.transform(trans);

				// do the polys touch?
				if (oPoly.contains(pt))
				{
					PortInst touchingPi = findPortInstClosestToPoly(ni, (PrimitivePort)oPoly.getPort(), pt);
					if (touchingPi == null)
					{
						System.out.println("Can't find port for node "+ni.describe()+" and port "+oPoly.getPort());
						continue;
					}
					touchingNodes.add(touchingPi);
					break;
				}
			}
		}
		return touchingNodes;
	}

	/**
	 * Method to find the PortInst on a NodeInst that connects to a given PortProto and is closest to a given point.
	 * Because some primitive nodes (transistors) may have multiple ports that connect to each other
	 * (the poly ports) and because the system returns only one of those ports when describing the topology of
	 * a piece of geometry, it is necessary to find the closest port.
	 * @param ni the primitive NodeInst being examined.
	 * @param pp the primitive port on that node which defines the connection to the node.
	 * @param pt a point close to the desired port.
	 * @return the PortInst on the node that is electrically connected to the given primitive port, and closest to the point.
	 */
	private PortInst findPortInstClosestToPoly(NodeInst ni, PrimitivePort pp, Point2D pt)
	{
		PortInst touchingPi = ni.findPortInstFromProto(pp);
		PrimitiveNode pnp = (PrimitiveNode)ni.getProto();
		Poly touchingPoly = touchingPi.getPoly();
		double bestDist = pt.distance(new Point2D.Double(touchingPoly.getCenterX(), touchingPoly.getCenterY()));
		for(Iterator pIt = pnp.getPorts(); pIt.hasNext(); )
		{
			PrimitivePort prP = (PrimitivePort)pIt.next();
			if (prP.getTopology() == pp.getTopology())
			{
				PortInst testPi = ni.findPortInstFromProto(prP);
				Poly testPoly = testPi.getPoly();
				double dist = pt.distance(new Point2D.Double(testPoly.getCenterX(), testPoly.getCenterY()));
				if (dist < bestDist)
				{
					bestDist = dist;
					touchingPi = testPi;
				}
			}
		}
		return touchingPi;
	}

	private static class Centerline
	{
		Point2D start, end;
		boolean startHub, endHub;
		double width;

		Centerline(double width, Point2D start, Point2D end)
		{
			this.width = width;
			this.start = start;
			this.end = end;
			startHub = endHub = false;
		}
	}

	/**
	 * Method to convert a piece of geometry to wires.
	 * @param poly a polygon to convert to wires.
	 * @param layer the layer associated with the polygon.
	 * @param ap the type of arc to create when converting to wires.
	 */
	private void convertToWires(PolyBase poly, Layer layer, ArcProto ap)
	{
		// reduce the geometry to a skeleton of centerlines
		List lines = findCenterlines(poly, layer, ap);

		// now realize the wires
		for(Iterator it = lines.iterator(); it.hasNext(); )
		{
			Centerline cl = (Centerline)it.next();
			Point2D loc1 = new Point2D.Double();
			PortInst pi1 = locatePortOnCenterline(cl, loc1, layer, ap, true);
			Point2D loc2 = new Point2D.Double();
			PortInst pi2 = locatePortOnCenterline(cl, loc2, layer, ap, false);
//System.out.println("Centerline from ("+cl.start.getX()+","+cl.start.getY()+") to ("+cl.end.getX()+","+cl.end.getY()+") width="+cl.width);
//System.out.println("  will run from pi="+pi1+" at ("+loc1.getX()+","+loc1.getY()+") to pi="+pi2+" at ("+loc2.getX()+","+loc2.getY()+")");
			realizeArc(ap, pi1, pi2, loc1, loc2, cl.width);
		}
	}

	/**
	 * Method to return the port and location to use for one end of a Centerline.
	 * @param cl the Centerline to connect
	 * @param loc1 it's location (values returned through this object!)
	 * @param layer the layer associated with the Centerline.
	 * @param ap the type of arc to create.
	 * @param startSide true to 
	 * @return
	 */
	private PortInst locatePortOnCenterline(Centerline cl, Point2D loc1, Layer layer, ArcProto ap, boolean startSide)
	{
		PortInst piRet = null;
		boolean isHub = cl.endHub;
		Point2D startPoint = cl.end;
		if (startSide)
		{
			isHub = cl.startHub;
			startPoint = cl.start;
		}
		if (!isHub)
		{
			int angCenterLine = GenMath.figureAngle(cl.start, cl.end);
			List possiblePorts = findPortInstsTouchingPoint(startPoint, layer);
			for(Iterator pIt = possiblePorts.iterator(); pIt.hasNext(); )
			{
				PortInst pi = (PortInst)pIt.next();
				Poly portPoly = pi.getPoly();
				Point2D [] points = portPoly.getPoints();
//System.out.println((startSide?"Start":"End")+" side of centerline might connect to port "+pi.getPortProto().getName()+" of node "+pi.getNodeInst().describe()+" with "+points.length+" points");
				if (points.length == 1)
				{
				    Point2D iPt = GenMath.intersect(cl.start, angCenterLine, points[0], angCenterLine+900);
				    if (iPt != null)
					{
						loc1.setLocation(iPt.getX(), iPt.getY());
						piRet = pi;
						break;
					}
				} else
				{
					for(int i=0; i<points.length; i++)
					{
						int last = i-1;
						if (last < 0) last = points.length-1;
						Point2D portLineFrom = points[last];
						Point2D portLineTo = points[i];
						Point2D interPt = null;
						if (portLineFrom.equals(portLineTo))
						{
							interPt = GenMath.intersect(cl.start, angCenterLine, portLineFrom, angCenterLine+900);
						} else
						{
							int angPortLine = GenMath.figureAngle(portLineFrom, portLineTo);
							interPt = GenMath.intersect(portLineFrom, angPortLine, cl.start, angCenterLine);
							if (interPt != null)
							{
								if (interPt.getX() < Math.min(portLineFrom.getX(), portLineTo.getX()) ||
									interPt.getX() > Math.max(portLineFrom.getX(), portLineTo.getX()) ||
									interPt.getY() < Math.min(portLineFrom.getY(), portLineTo.getY()) ||
									interPt.getY() > Math.max(portLineFrom.getY(), portLineTo.getY())) interPt = null;
							}
						}
						if (interPt == null) continue;
						loc1.setLocation(interPt.getX(), interPt.getY());
						piRet = pi;
						break;
					}
					if (piRet != null) break;
				}
			}
		}
		if (piRet == null)
		{
			// shrink the end inward by half-width
			int angle = GenMath.figureAngle(cl.start, cl.end);
			PrimitiveNode pin = ((PrimitiveArc)ap).findPinProto();
			if (startSide)
			{
				if (!isHub)
					cl.start.setLocation(cl.start.getX() + GenMath.cos(angle)*cl.width/2,
						cl.start.getY() + GenMath.sin(angle)*cl.width/2);
				NodeInst ni = wantNodeAt(cl.start, pin, cl.width);
				loc1.setLocation(cl.start.getX(), cl.start.getY());
				piRet = ni.getOnlyPortInst();
			} else
			{
				if (!isHub)
					cl.end.setLocation(cl.end.getX() - GenMath.cos(angle)*cl.width/2,
						cl.end.getY() - GenMath.sin(angle)*cl.width/2);
				NodeInst ni = wantNodeAt(cl.end, pin, cl.width);
				loc1.setLocation(cl.end.getX(), cl.end.getY());
				piRet = ni.getOnlyPortInst();
			}
		}
		return piRet;
	}

	/**
	 * Method to find a list of centerlines that skeletonize a polygon.
	 * @param poly the Poly to skeletonize.
	 * @param layer the layer on which the polygon resides.
	 * @param ap the ArcProto to use when realizing the polygon.
	 * @return a List of Centerline objects that describe a single "bone" of the skeleton.
	 */
	private List findCenterlines(PolyBase poly, Layer layer, ArcProto ap)
	{
		// first make a list of all parallel wires in the polygon
		List centerlines = new ArrayList();
		Point2D [] points = poly.getPoints();
		for(int i=0; i<points.length; i++)
		{
			int lastI = i-1;
			if (lastI < 0) lastI = points.length-1;
			Point2D lastPt = points[lastI];
			Point2D thisPt = points[i];
			if (lastPt.equals(thisPt)) continue;
			int angle = GenMath.figureAngle(thisPt, lastPt) % 1800;

			// now find another line parallel to this one
			for(int j=i+2; j<points.length; j++)
			{
				Point2D oLastPt = points[j-1];
				Point2D oThisPt = points[j];
				if (oLastPt.equals(oThisPt)) continue;
				int oAngle = GenMath.figureAngle(oThisPt, oLastPt) % 1800;
				if (oAngle == angle)
				{
					// parallel lines: find the center line
					int perpAngle = angle + 900;
					Point2D oneSide = thisPt;
					Point2D otherSide = GenMath.intersect(thisPt, perpAngle, oThisPt, oAngle);
					Point2D centerPt = new Point2D.Double((oneSide.getX()+otherSide.getX()) / 2, (oneSide.getY()+otherSide.getY()) / 2);
					double width = oneSide.distance(otherSide);

					// now determine range along that centerline
					Point2D lastPtCL = GenMath.intersect(lastPt, perpAngle, centerPt, angle);
					Point2D thisPtCL = GenMath.intersect(thisPt, perpAngle, centerPt, angle);
					Point2D oLastPtCL = GenMath.intersect(oLastPt, perpAngle, centerPt, angle);
					Point2D oThisPtCL = GenMath.intersect(oThisPt, perpAngle, centerPt, angle);

					// determine extent of these points
					double minX = Math.min(Math.min(lastPtCL.getX(), thisPtCL.getX()), Math.min(oLastPtCL.getX(), oThisPtCL.getX()));
					double maxX = Math.max(Math.max(lastPtCL.getX(), thisPtCL.getX()), Math.max(oLastPtCL.getX(), oThisPtCL.getX()));
					double minY = Math.min(Math.min(lastPtCL.getY(), thisPtCL.getY()), Math.min(oLastPtCL.getY(), oThisPtCL.getY()));
					double maxY = Math.max(Math.max(lastPtCL.getY(), thisPtCL.getY()), Math.max(oLastPtCL.getY(), oThisPtCL.getY()));

					// determine distance along the line
					Point2D [] corners = new Point2D[4];
					corners[0] = new Point2D.Double(minX, minY);
					corners[1] = new Point2D.Double(minX, maxY);
					corners[2] = new Point2D.Double(maxX, maxY);
					corners[3] = new Point2D.Double(maxX, minY);

					// see which lines are at the corners (farthest extents)
					boolean lastCorner = false, thisCorner = false, oLastCorner = false, oThisCorner = false;
					Point2D aCorner = null;
					for(int k=0; k<4; k++)
					{
						if (lastPtCL.equals(corners[k])) { aCorner = lastPtCL;   lastCorner = true; }
						if (thisPtCL.equals(corners[k])) { aCorner = thisPtCL;   thisCorner = true; }
						if (oLastPtCL.equals(corners[k])) { aCorner = oLastPtCL;   oLastCorner = true; }
						if (oThisPtCL.equals(corners[k])) { aCorner = oThisPtCL;   oThisCorner = true; }
					}

					// handle one line encompasing the other
					if (!lastCorner && !thisCorner)
					{
						centerlines.add(new Centerline(width, lastPtCL, thisPtCL));
						continue;
					}
					if (!oLastCorner && !oThisCorner)
					{
						centerlines.add(new Centerline(width, oLastPtCL, oThisPtCL));
						continue;
					}

					// order them by distance from a corner
					double lastDist = aCorner.distance(lastPtCL);
					double thisDist = aCorner.distance(thisPtCL);
					double oLastDist = aCorner.distance(oLastPtCL);
					double oThisDist = aCorner.distance(oThisPtCL);
					if (lastDist > thisDist)
					{
						double swap = lastDist;   lastDist = thisDist;   thisDist = swap;
						Point2D swapPt = lastPtCL;   lastPtCL = thisPtCL;   thisPtCL = swapPt;
					}
					if (oLastDist > oThisDist)
					{
						double swap = oLastDist;   oLastDist = oThisDist;   oThisDist = swap;
						Point2D swapPt = oLastPtCL;   oLastPtCL = oThisPtCL;   oThisPtCL = swapPt;
					}

					// ignore if disjoint
					if (thisDist <= oLastDist || oThisDist <= lastDist) continue;

					Point2D start = lastPtCL;
					if (oLastDist > lastDist) start = oLastPtCL;
					Point2D end = thisPtCL;
					if (oThisDist < thisDist) end = oThisPtCL;
					centerlines.add(new Centerline(width, start, end));
				}
			}
		}

		// sort the parallel wires by width
		Collections.sort(centerlines, new ParallelWiresByWidth());
//System.out.print("For layer "+layer.getName()+", centerline widths are");
//for(Iterator it = centerlines.iterator(); it.hasNext(); )
//{
//	Centerline cl = (Centerline)it.next();
//	System.out.print(" "+cl.width);
//}
//System.out.println();
	
		// now pull out the relevant ones
		List validCenterlines = new ArrayList();
		merge.deleteLayer(tempLayer1);
		merge.addPolygon(tempLayer1, poly);
		double lastWidth = -1;
		for(Iterator it = centerlines.iterator(); it.hasNext(); )
		{
			Centerline cl = (Centerline)it.next();
			if (cl.width < ap.getDefaultWidth()) continue;
			double length = cl.start.distance(cl.end);
			if (length < cl.width) continue;

			// see if this centerline actually covers new area
			Poly clPoly = Poly.makeEndPointPoly(length, cl.width, GenMath.figureAngle(cl.start, cl.end),
				cl.start, 0, cl.end, 0);
			boolean intersects = merge.intersects(tempLayer1, clPoly);
			if (!intersects) continue;
			validCenterlines.add(cl);
			merge.subPolygon(tempLayer1, clPoly);
		}
		merge.deleteLayer(tempLayer1);
//System.out.println("  And valid centerlines are:");
//for(Iterator it = validCenterlines.iterator(); it.hasNext(); )
//{
//	Centerline cl = (Centerline)it.next();
//	System.out.println("    "+cl.width+" from ("+cl.start.getX()+","+cl.start.getY()+") to ("+cl.end.getX()+","+cl.end.getY()+")");
//}

		// now combine colinear centerlines
		int numCenterlines = validCenterlines.size();
		Centerline [] lines = new Centerline[numCenterlines];
		for(int i=0; i<numCenterlines; i++)
			lines[i] = (Centerline)validCenterlines.get(i);
		for(int i=0; i<numCenterlines; i++)
		{
			Centerline cl = lines[i];
			for(int j=i+1; j<numCenterlines; j++)
			{
				Centerline oCl = lines[j];
				if (isColinear(cl, oCl))
				{
					// delete the second line
					for(int k=j; k<numCenterlines-1; k++)
						lines[k] = lines[k+1];
					numCenterlines--;
					j--;
				}
			}
		}

		// now extend centerlines so they meet
		for(int i=0; i<numCenterlines; i++)
		{
			Centerline cl = lines[i];
			for(int j=i+1; j<numCenterlines; j++)
			{
				Centerline oCl = lines[j];
				double maxDist = (cl.width + oCl.width) / 2;
				if (cl.start.distance(oCl.start) <= maxDist || cl.start.distance(oCl.end) <= maxDist ||
					cl.end.distance(oCl.start) <= maxDist || cl.end.distance(oCl.end) <= maxDist)
				{
					// endpoints are near: see if they can be extended
					Point2D intersect = GenMath.intersect(cl.start, GenMath.figureAngle(cl.start, cl.end), oCl.start, GenMath.figureAngle(oCl.start, oCl.end));
					if (intersect == null) continue;
					Point2D newStart = cl.start, newEnd = cl.end;
					double extendStart = 0, extendEnd = 0;
					if (intersect.distance(newStart) < intersect.distance(newEnd))
					{
						newStart = intersect;
						extendStart = cl.width / 2;
					} else
					{
						newEnd = intersect;
						extendEnd = cl.width / 2;
					}
					Poly extended = Poly.makeEndPointPoly(newStart.distance(newEnd), cl.width, GenMath.figureAngle(newStart, newEnd),
						newStart, extendStart, newEnd, extendEnd);
					if (originalMerge.contains(layer, extended))
					{
						cl.start = newStart;
						cl.end = newEnd;
						if (extendStart != 0) cl.startHub = true;
						if (extendEnd != 0) cl.endHub = true;
					}

					newStart = oCl.start;
					newEnd = oCl.end;
					extendStart = extendEnd = 0;
					if (intersect.distance(newStart) < intersect.distance(newEnd))
					{
						newStart = intersect;
						extendStart = oCl.width / 2;
					} else
					{
						newEnd = intersect;
						extendEnd = oCl.width / 2;
					}
					extended = Poly.makeEndPointPoly(newStart.distance(newEnd), oCl.width, GenMath.figureAngle(newStart, newEnd),
						newStart, extendStart, newEnd, extendEnd);
					if (originalMerge.contains(layer, extended))
					{
						oCl.start = newStart;
						oCl.end = newEnd;
						if (extendStart != 0) oCl.startHub = true;
						if (extendEnd != 0) oCl.endHub = true;
					}
				}					
			}
		}

		// make a list of the final results
		List finalCenterlines = new ArrayList();
		for(int i=0; i<numCenterlines; i++)
			finalCenterlines.add(lines[i]);
		return finalCenterlines;
	}

	private static class ParallelWiresByWidth implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			Centerline cl1 = (Centerline)o1;
			Centerline cl2 = (Centerline)o2;
			double cll1 = cl1.start.distance(cl1.end);
			double cll2 = cl2.start.distance(cl2.end);
			if (cl1.width < cl2.width) return -1;
			if (cl1.width > cl2.width) return 1;
			double cla1 = cl1.width * cll1;
			double cla2 = cl2.width * cll2;
			if (cla1 > cla2) return -1;
			if (cla1 < cla2) return 1;
			return 0;
		}
	}

	/**
	 * Method to determine whether two Centerline objects are colinear.
	 * @param cl the first Centerline object.
	 * @param oCl the second Centerline object.
	 * @return true if they are colinear.  Also modifies the first one (c1) to contain the overall line.
	 */
	private boolean isColinear(Centerline cl, Centerline oCl)
	{
		// get information about the other line
		double fx = cl.start.getX();
		double fy = cl.start.getY();
		double tx = cl.end.getX();
		double ty = cl.end.getY();
		double oFx = oCl.start.getX();
		double oFy = oCl.start.getY();
		double oTx = oCl.end.getX();
		double oTy = oCl.end.getY();
		if (oFx == oTx && oFy == oTy) return false;

		// see if they are colinear
		double lowX = Math.min(fx, tx);
		double highX = Math.max(fx, tx);
		double lowY = Math.min(fy, ty);
		double highY = Math.max(fy, ty);
		if (fx == tx)
		{
			// vertical line
			double oLow = Math.min(oFy, oTy);
			double oHigh = Math.max(oFy, oTy);
			if (oFx != fx || oTx != fx) return false;
			if (lowY > oHigh || highY < oLow) return false;
			cl.start.setLocation(tx, Math.min(lowY, oLow));
			cl.end.setLocation(tx, Math.max(highY, oHigh));
			return true;
		}
		if (fy == ty)
		{
			// horizontal line
			double oLow = Math.min(oFx, oTx);
			double oHigh = Math.max(oFx, oTx);
			if (oFy != fy || oTy != fy) return false;
			if (lowX > oHigh || highX < oLow) return false;
			cl.start.setLocation(Math.min(lowX, oLow), ty);
			cl.end.setLocation(Math.max(highX, oHigh), ty);
			return true;
		}

		// general case
		int ang = GenMath.figureAngle(new Point2D.Double(fx, fy), new Point2D.Double(tx, ty));
		int oAng = GenMath.figureAngle(new Point2D.Double(oFx, oFy), new Point2D.Double(oTx, oTy));
		if (ang != oAng && Math.min(ang, oAng) + 1800 != Math.max(ang, oAng)) return false;
		if ((oFx-fx) * (ty-fy) / (tx-fx) != oFy-fy) return false;
		if ((oTx-fx) * (ty-fy) / (tx-fx) != oTy-fy) return false;
		double oLow = Math.min(oFy, oTy);
		double oHigh = Math.max(oFy, oTy);
		if (lowY >= oHigh || highY <= oLow) return false;
		oLow = Math.min(oFx, oTx);
		oHigh = Math.max(oFx, oTx);
		if (lowX >= oHigh || highX <= oLow) return false;

		// find the extreme points
		Point2D [] points = new Point2D[4];
		points[0] = cl.start;
		points[1] = cl.end;
		points[2] = oCl.start;
		points[3] = oCl.end;
		double bestDist = 0;
		int bestEnd = -1;
		for(int i=1; i<3; i++)
		{
			double dist = points[0].distance(points[i]);
			if (dist > bestDist)
			{
				bestDist = dist;
				bestEnd = i;
			}
		}
		if (bestEnd < 0) return false;
		bestDist = 0;
		int bestOtherEnd = -1;
		for(int i=0; i<4; i++)
		{
			if (i == bestEnd) continue;
			double dist = points[bestEnd].distance(points[i]);
			if (dist > bestDist)
			{
				bestDist = dist;
				bestOtherEnd = i;
			}
		}
		double newEndAX = points[bestEnd].getX();
		double newEndAY = points[bestEnd].getY();
		double newEndBX = points[bestOtherEnd].getX();
		double newEndBY = points[bestOtherEnd].getY();

		cl.start.setLocation(newEndAX, newEndAY);
		cl.end.setLocation(newEndBX, newEndBY);
		return true;
	}

	/********************************************** VIA/CONTACT EXTRACTION **********************************************/

	private static class PossibleVia
	{
		PrimitiveNode pNp;
		double minWidth, minHeight;
		double largestShrink;
		Layer [] layers;
		double [] shrink;

		PossibleVia(PrimitiveNode pNp) { this.pNp = pNp; }
	}

	/**
	 * Method to scan the geometric information for possible contacts and vias.
	 * Any vias found are created in the new cell and removed from the geometric information.
	 */
	private void extractVias()
	{
		// make map of PrimitiveNodes and their geometry
		HashMap possibleAreas = new HashMap();

		// look at all via layers and see if contacts can be extracted
		for(Iterator lIt = merge.getKeyIterator(); lIt.hasNext(); )
		{
			Layer layer = (Layer)lIt.next();
			Layer.Function fun = layer.getFunction();
			if (!fun.isContact()) continue;

			// compute the possible via nodes that this layer could become
			List possibleVias = findPossibleVias(layer);

			// look at all of the pieces of this layer
			List polyList = merge.getMergedPoints(layer, true);
			while (polyList.size() > 0)
			{
				PolyBase poly = (PolyBase)polyList.get(0);
				double centerX = poly.getCenterX();
				double centerY = poly.getCenterY();
//System.out.println("Consider contact layer "+layer.getName()+" at ("+centerX+","+centerY+")");

				// now look through the list to see if anything matches
				for(Iterator it = possibleVias.iterator(); it.hasNext(); )
				{
					PossibleVia pv = (PossibleVia)it.next();
					PrimitiveNode pNp = pv.pNp;

//System.out.println("  Could it be primitive "+pNp.getName());
					List contactArea = (List)possibleAreas.get(pNp);
					if (contactArea == null)
					{
						// merge all other layers and see how big the contact can be
						originalMerge.insetLayer(pv.layers[0], tempLayer1, (pv.largestShrink-pv.shrink[0])/2);
						for(int i=1; i<pv.layers.length; i++)
						{
							originalMerge.insetLayer(pv.layers[i], tempLayer2, (pv.largestShrink-pv.shrink[i])/2);
							originalMerge.intersectLayers(tempLayer1, tempLayer2, tempLayer1);
						}
						contactArea = originalMerge.getMergedPoints(tempLayer1, true);
						if (contactArea == null) contactArea = new ArrayList();
						possibleAreas.put(pNp, contactArea);
					}
					if (contactArea == null || contactArea.size() == 0) continue;

//					createLargestContact(centerX, centerY, pv, contactArea, polyList);
					Rectangle2D largest = findLargestRectangle(contactArea, centerX, centerY, pv.minWidth-pv.largestShrink, pv.minHeight-pv.largestShrink);
					if (largest == null) continue;
//System.out.println("  Contact is surrounded by area from "+largest.getMinX()+"<=X<="+largest.getMaxX()+" and "+largest.getMinY()+"<=Y<="+largest.getMaxY());
					centerX = largest.getCenterX();
					centerY = largest.getCenterY();
					double desiredWidth = largest.getWidth() + pv.largestShrink;
					double desiredHeight = largest.getHeight() + pv.largestShrink;

					// check all other layers
					for(;;)
					{
						boolean gotMinimum = true;
						for(int i=0; i<pv.layers.length; i++)
						{
							Layer nLayer = pv.layers[i];
							double minLayWid = desiredWidth - pv.shrink[i];
							double minLayHei = desiredHeight - pv.shrink[i];
							Rectangle2D rect = new Rectangle2D.Double(centerX - minLayWid/2, centerY - minLayHei/2, minLayWid, minLayHei);
							if (!originalMerge.contains(nLayer, rect))
							{
								gotMinimum = false;
								break;
							}
						}
						if (gotMinimum)
						{
							realizeNode(pNp, centerX, centerY, desiredWidth, desiredHeight, 0);
//System.out.println("  Is part of "+pNp.getName()+" contact which is "+desiredWidth+"x"+desiredHeight+" at ("+centerX+","+centerY+")");
							// remove other cuts in this area
							for(int o=1; o<polyList.size(); o++)
							{
								PolyBase oPoly = (PolyBase)polyList.get(o);
								double x = oPoly.getCenterX();
								double y = oPoly.getCenterY();
								if (largest.contains(x, y))
								{
//System.out.println("    and also includes cut at ("+x+","+y+")");
									polyList.remove(oPoly);
									o--;
								}
							}
							break;
						}
						desiredHeight--;
						desiredWidth--;
						if (desiredHeight < pv.minHeight || desiredWidth < pv.minWidth) break;
					}
				}
				polyList.remove(poly);
			}
		}
	}

//	private void createLargestContact(double centerX, double centerY, PossibleVia pv, List contactArea, List polyList)
//	{
//		if (pv.pNp.getSpecialType() == PrimitiveNode.MULTICUT)
//		{
//			/* determine distance to search for additional cuts
//			 *   cut size is specialValues[0] x specialValues[1]
//			 *   cut indented specialValues[2] x specialValues[3] from highlighting
//			 *   cuts spaced specialValues[4] apart for 2-neighboring CO
//			 *   cuts spaced specialValues[5] apart for 3-neighboring CO or more
//			 */
//			double [] specialValues = pv.pNp.getSpecialValues();
//			double cutIncrement = (Math.max(specialValues[0], specialValues[1]) + Math.max(specialValues[4], specialValues[5])) * 2;
//			
//		}
//	}

	private Rectangle2D findLargestRectangle(List contactArea, double centerX, double centerY, double minWidth, double minHeight)
	{
		double closestTop = Double.MAX_VALUE;
		double closestBottom = Double.MAX_VALUE;
		double closestLeft = Double.MAX_VALUE;
		double closestRight = Double.MAX_VALUE;
		for(Iterator it = contactArea.iterator(); it.hasNext(); )
		{
			PolyBase poly = (PolyBase)it.next();
			if (!poly.contains(centerX, centerY)) continue;

			// this is the part that contains the point: gather bounds
			Point2D [] points = poly.getPoints();
			for(int i=0; i<points.length; i++)
			{
				int last = i - 1;
				if (last < 0) last = points.length - 1;
				Point2D lastPt = points[last];
				Point2D thisPt = points[i];
				if (lastPt.getX() == thisPt.getX())
				{
					// vertical line: check left and right bounds
					double lowY = Math.min(thisPt.getY(), lastPt.getY());
					double highY = Math.max(thisPt.getY(), lastPt.getY());
					if (lowY >= centerY + minHeight/2) continue;
					if (highY <= centerY - minHeight/2) continue;
					double dist = lastPt.getX() - centerX;
					if (dist > 0)
					{
						// see if this is a new right edge
						if (dist < closestRight) closestRight = dist;
					} else
					{
						// see if this is a new right edge
						if (-dist < closestLeft) closestLeft = -dist;
					}
				}
				if (lastPt.getY() == thisPt.getY())
				{
					// horizontal line: check top and bottom bounds
					double lowX = Math.min(thisPt.getX(), lastPt.getX());
					double highX = Math.max(thisPt.getX(), lastPt.getX());
					if (lowX >= centerX + minWidth/2) continue;
					if (highX <= centerX - minWidth/2) continue;
					double dist = lastPt.getY() - centerY;
					if (dist > 0)
					{
						// see if this is a new top edge
						if (dist < closestTop) closestTop = dist;
					} else
					{
						// see if this is a new bottom edge
						if (-dist < closestBottom) closestBottom = -dist;
					}
				}
			}
			if (closestTop == Double.MAX_VALUE || closestBottom == Double.MAX_VALUE ||
				closestLeft == Double.MAX_VALUE || closestRight == Double.MAX_VALUE) return null;
			Rectangle2D largest = new Rectangle2D.Double(centerX-closestLeft, centerY-closestBottom,
				closestLeft+closestRight, closestBottom+closestTop);
			if (poly.contains(largest)) return largest;
		}
		return null;
	}

	/**
	 * Method to return a list of PossibleVia objects for a given cut/via layer.
	 * @param lay the cut/via layer to find.
	 * @return a List of PossibleVia objects that use the layer as a contact.
	 */
	private List findPossibleVias(Layer lay)
	{
		List possibleVias = new ArrayList();
		for(Iterator nIt = tech.getNodes(); nIt.hasNext(); )
		{
			PrimitiveNode pNp = (PrimitiveNode)nIt.next();
			PrimitiveNode.Function fun = pNp.getFunction();
			if (fun != PrimitiveNode.Function.CONTACT && fun != PrimitiveNode.Function.WELL &&
				fun != PrimitiveNode.Function.SUBSTRATE) continue;

			// create a PossibleVia object to test for them
			PossibleVia pv = new PossibleVia(pNp);
			Technology.NodeLayer [] nLayers = pNp.getLayers();
			pv.layers = new Layer[nLayers.length-1];
			pv.shrink = new double[nLayers.length-1];
			pv.minWidth = pNp.getDefWidth();
			pv.minHeight = pNp.getDefHeight();

			// load the layer information
			int fill = 0;
			boolean cutFound = false;
			for(int i=0; i<nLayers.length; i++)
			{
				// examine one layer of the primitive node
				Technology.NodeLayer nLay = nLayers[i];
				Layer nLayer = nLay.getLayer();
				boolean cutLayer = false;
				if (nLayer == lay)
				{
					// this is the target cut layer: mark it
					cutLayer = true;
				}
				if (!cutLayer)
				{
					// special case for active/poly cut confusion
					if (nLayer.getFunction() == lay.getFunction()) cutLayer = true;
				}
				if (cutLayer) cutFound = true; else
				{
					// non-cut layer: examine its rules
					if (nLay.getLeftEdge().getMultiplier() != -0.5 || nLay.getRightEdge().getMultiplier() != 0.5 ||
						nLay.getBottomEdge().getMultiplier() != -0.5 || nLay.getTopEdge().getMultiplier() != 0.5)
					{
						System.out.println("Cannot decipher the sizing rules for layer " +
							nLay.getLayer().getName() + " of node " + pNp.describe() +
							" LEFT="+nLay.getLeftEdge().getMultiplier() + " RIGHT="+nLay.getRightEdge().getMultiplier()+
							" BOTTOM="+nLay.getBottomEdge().getMultiplier() + " TOP="+nLay.getTopEdge().getMultiplier());
						break;
					}
					if (nLay.getLeftEdge().getAdder() != -nLay.getRightEdge().getAdder() ||
						nLay.getBottomEdge().getAdder() != -nLay.getTopEdge().getAdder())
					{
						System.out.println("Cannot decipher the sizing rules for layer " +
							nLay.getLayer().getName() + " of node " + pNp.describe() +
							" LEFT="+nLay.getLeftEdge().getAdder() + " RIGHT="+nLay.getRightEdge().getAdder()+
							" BOTTOM="+nLay.getBottomEdge().getAdder() + " TOP="+nLay.getTopEdge().getAdder());
						break;
					}

					// create the PossibleVia to describe the geometry
					if (fill >= nLayers.length-1) break;
					pv.layers[fill] = geometricLayer(nLay.getLayer());
					pv.shrink[fill] = Math.max(nLay.getLeftEdge().getAdder(), nLay.getBottomEdge().getAdder()) * 2;
					fill++;
				}
			}
			if (!cutFound) continue;

			// got the right cut layer, did we get all rules filled?
			if (fill != nLayers.length-1)
			{
				System.out.println("Errors found, not scanning for " + pNp.describe());
				continue;
			}
			
			// determine the range of shrinkage values for the layers in the contact
			pv.largestShrink = pv.shrink[0];
			for(int i=1; i<pv.layers.length; i++)
				if (pv.shrink[i] > pv.largestShrink) pv.largestShrink = pv.shrink[i];

			// add this to the list of possible vias
			possibleVias.add(pv);
		}
		return possibleVias;
	}

	/********************************************** TRANSISTOR EXTRACTION **********************************************/

	private void extractTransistors()
	{
		// must have a poly layer
		if (polyLayer == null || tempLayer1 == null) return;

		// find the transistors to create
		PrimitiveNode pTransistor = null, nTransistor = null;
		for(Iterator it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode pNp = (PrimitiveNode)it.next();
			if (pTransistor == null && pNp.getFunction() == PrimitiveNode.Function.TRAPMOS) pTransistor = pNp;
			if (nTransistor == null && pNp.getFunction() == PrimitiveNode.Function.TRANMOS) nTransistor = pNp;
		}
		if (nTransistor != null)
		{
			findTransistors(nTransistor);
		}
		if (pTransistor != null)
		{
			findTransistors(pTransistor);
		}
	}

	private void findTransistors(PrimitiveNode transistor)
	{
		originalMerge.intersectLayers(polyLayer, activeLayer, tempLayer1);
		Technology.NodeLayer [] layers = transistor.getLayers();
		for(int i=0; i<layers.length; i++)
		{
			Technology.NodeLayer lay = layers[i];
			Layer layer = geometricLayer(lay.getLayer());
			if (layer == polyLayer || layer == activeLayer) continue;
			originalMerge.intersectLayers(layer, tempLayer1, tempLayer1);
		}

		// look at all of the pieces of this layer
		List polyList = originalMerge.getMergedPoints(tempLayer1, true);
		if (polyList == null) return;
		for(Iterator pIt = polyList.iterator(); pIt.hasNext(); )
		{
			PolyBase poly = (PolyBase)pIt.next();
			Rectangle2D transBox = poly.getBox();
			if (transBox != null)
			{
				// found a manhattan transistor, determine orientation
				Point2D left = new Point2D.Double(transBox.getMinX() - 1, transBox.getCenterY());
				Point2D right = new Point2D.Double(transBox.getMaxX() + 1, transBox.getCenterY());
				Point2D bottom = new Point2D.Double(transBox.getCenterX(), transBox.getMinY() - 1);
				Point2D top = new Point2D.Double(transBox.getCenterX(), transBox.getMaxY() + 1);
				int angle = 0;
				double wid = transBox.getWidth();
				double hei = transBox.getHeight();
				if (originalMerge.contains(polyLayer, left) && originalMerge.contains(polyLayer, right) &&
					originalMerge.contains(activeLayer, top) && originalMerge.contains(activeLayer, bottom))
				{
				} else if (originalMerge.contains(activeLayer, left) && originalMerge.contains(activeLayer, right) &&
					originalMerge.contains(polyLayer, top) && originalMerge.contains(polyLayer, bottom))
				{
					angle = 900;
					wid = transBox.getHeight();
					hei = transBox.getWidth();
				} else
				{
					System.out.println("Transistor doesn't have proper tabs...ignored");
					continue;
				}
				SizeOffset so = transistor.getProtoSizeOffset();
				double width = wid + so.getLowXOffset() + so.getHighXOffset();
				double height = hei + so.getLowYOffset() + so.getHighYOffset();
				realizeNode(transistor, poly.getCenterX(), poly.getCenterY(), width, height, angle);
			}
		}
		originalMerge.deleteLayer(tempLayer1);
	}

	/********************************************** CONVERT CONNECTING GEOMETRY **********************************************/

	/**
	 * Method to look for opportunities to place arcs that connect to existing geometry.
	 */
	private void extendGeometry()
	{
		for(Iterator lIt = merge.getKeyIterator(); lIt.hasNext(); )
		{
			Layer layer = (Layer)lIt.next();
			ArcProto ap = (ArcProto)arcsForLayer.get(layer);
			if (ap == null) continue;
			List polyList = merge.getMergedPoints(layer, true);
			for(Iterator pIt = polyList.iterator(); pIt.hasNext(); )
			{
				PolyBase poly = (PolyBase)pIt.next();

				// find out what this polygon touches
				HashMap netsThatTouch = getNetsThatTouch(poly, layer);

				// make a list of port/arc ends that touch this polygon
				List objectsToConnect = new ArrayList();
				for(Iterator it = netsThatTouch.keySet().iterator(); it.hasNext(); )
				{
					Object entry = netsThatTouch.get(it.next());
					if (entry != null) objectsToConnect.add(entry);
				}

				// if only 1 object touches the polygon, see if it can be "wired" to cover
				if (objectsToConnect.size() == 1)
				{
					// touches just 1 object: see if that object can be extended to cover the polygon
					extendObject((ElectricObject)objectsToConnect.get(0), poly, layer, ap);
					continue;
				}

				// if two objects touch the polygon, see if an arc can connect them
				if (objectsToConnect.size() == 2)
				{
					ElectricObject obj1 = (ElectricObject)objectsToConnect.get(0);
					ElectricObject obj2 = (ElectricObject)objectsToConnect.get(1);
					if (obj1 instanceof ArcInst)
					{
						PortInst pi = findArcEnd((ArcInst)obj1, poly);
						if (pi == null)
						{
							findArcEnd((ArcInst)obj1, poly);
							continue;
						}
						obj1 = pi;
					}
					if (obj2 instanceof ArcInst)
					{
						PortInst pi = findArcEnd((ArcInst)obj2, poly);
						if (pi == null)
						{
							findArcEnd((ArcInst)obj2, poly);
							continue;
						}
						obj2 = pi;
					}
					PortInst pi1 = (PortInst)obj1;
					PortInst pi2 = (PortInst)obj2;

					// see if the ports can connect in a line
					Poly poly1 = pi1.getPoly();
					Poly poly2 = pi2.getPoly();
					Rectangle2D polyBounds1 = poly1.getBounds2D();
					Rectangle2D polyBounds2 = poly2.getBounds2D();
					if (polyBounds1.getMinX() <= polyBounds2.getMaxX() && polyBounds1.getMaxX() >= polyBounds2.getMinX())
					{
						// vertical connection
						double xpos = polyBounds1.getCenterX();
						if (xpos < polyBounds2.getMinX()) xpos = polyBounds2.getMinX();
						if (xpos > polyBounds2.getMaxX()) xpos = polyBounds2.getMaxX();
						Point2D pt1 = new Point2D.Double(xpos, polyBounds1.getCenterY());
						Point2D pt2 = new Point2D.Double(xpos, polyBounds2.getCenterY());
						ArcInst ai = realizeArc(ap, pi1, pi2, pt1, pt2, ap.getDefaultWidth());
						continue;
					}
					if (polyBounds1.getMinY() <= polyBounds2.getMaxY() && polyBounds1.getMaxY() >= polyBounds2.getMinY())
					{
						// horizontal connection
						double ypos = polyBounds1.getCenterY();
						if (ypos < polyBounds2.getMinY()) ypos = polyBounds2.getMinY();
						if (ypos > polyBounds2.getMaxY()) ypos = polyBounds2.getMaxY();
						Point2D pt1 = new Point2D.Double(polyBounds1.getCenterX(), ypos);
						Point2D pt2 = new Point2D.Double(polyBounds2.getCenterX(), ypos);
						ArcInst ai = realizeArc(ap, pi1, pi2, pt1, pt2, ap.getDefaultWidth());
						continue;
					}

					// see if a bend can be made through the polygon
					Point2D pt1 = new Point2D.Double(polyBounds1.getCenterX(), polyBounds1.getCenterY());
					Point2D pt2 = new Point2D.Double(polyBounds2.getCenterX(), polyBounds2.getCenterY());
					Point2D corner1 = new Point2D.Double(polyBounds1.getCenterX(), polyBounds2.getCenterY());
					Point2D corner2 = new Point2D.Double(polyBounds2.getCenterX(), polyBounds1.getCenterY());
					if (poly.contains(corner1))
					{
						PrimitiveNode np = ((PrimitiveArc)ap).findPinProto();
						NodeInst ni = NodeInst.makeInstance(np, corner1, np.getDefWidth(), np.getDefHeight(), newCell);
						PortInst pi = ni.getOnlyPortInst();
						ArcInst ai1 = realizeArc(ap, pi1, pi, pt1, corner1, ap.getDefaultWidth());
						ArcInst ai2 = realizeArc(ap, pi2, pi, pt2, corner1, ap.getDefaultWidth());
					}
					if (poly.contains(corner2))
					{
						PrimitiveNode np = ((PrimitiveArc)ap).findPinProto();
						NodeInst ni = NodeInst.makeInstance(np, corner2, np.getDefWidth(), np.getDefHeight(), newCell);
						PortInst pi = ni.getOnlyPortInst();
						ArcInst ai1 = realizeArc(ap, pi1, pi, pt1, corner2, ap.getDefaultWidth());
						ArcInst ai2 = realizeArc(ap, pi2, pi, pt2, corner2, ap.getDefaultWidth());
					}
				}
			}
		}
	}

	/**
	 * Method to see if a polygon can be covered by adding an arc.
	 */
	private void extendObject(ElectricObject obj, PolyBase poly, Layer layer, ArcProto ap)
	{
		// can only handle rectangles now
		Rectangle2D polyBounds = poly.getBox();
		if (polyBounds == null)
		{
			Rectangle2D totalBounds = poly.getBounds2D();
			if (originalMerge.contains(layer, totalBounds))
				polyBounds = totalBounds;
		}
		if (polyBounds == null) return;
		Point2D polyCtr = new Point2D.Double(polyBounds.getCenterX(), polyBounds.getCenterY());
		PrimitiveNode np = ((PrimitiveArc)ap).findPinProto();
		if (obj instanceof ArcInst)
		{
			ArcInst ai = (ArcInst)obj;
			double headDist = polyCtr.distance(ai.getHead().getLocation());
			double tailDist = polyCtr.distance(ai.getTail().getLocation());
			if (headDist < tailDist) obj = ai.getHead().getPortInst(); else
				obj = ai.getTail().getPortInst();
		}
		PortInst pi = (PortInst)obj;
		Poly portPoly = pi.getPoly();
		Rectangle2D portRect = portPoly.getBounds2D();

		double width = polyBounds.getWidth();
		double height = polyBounds.getHeight();
		double actualWidth = ap.getDefaultWidth() - ap.getWidthOffset();

		// can we extend vertically
		if (polyCtr.getX() >= portRect.getMinX() && polyCtr.getX() <= portRect.getMaxX())
		{
			// going up to the poly or down?
			Point2D objPt = new Point2D.Double(polyCtr.getX(), portRect.getCenterY());
			Point2D pinPt = null;
			boolean noEndExtend = false;
			double endExtension = polyBounds.getWidth() / 2;
			if (polyBounds.getHeight() < polyBounds.getWidth())
			{
				// arc is so short that it will stick out with end extension
				noEndExtend = true;
				endExtension = 0;
			}
			if (polyCtr.getY() > portRect.getCenterY())
			{
				// going up to the poly
				pinPt = new Point2D.Double(polyCtr.getX(), polyBounds.getMaxY() - endExtension);
			} else
			{
				// going down to the poly
				pinPt = new Point2D.Double(polyCtr.getX(), polyBounds.getMinY() + endExtension);
			}
			NodeInst ni1 = NodeInst.makeInstance(np, pinPt, polyBounds.getWidth(), polyBounds.getWidth(), newCell);
			ArcInst ai = realizeArc(ap, ni1.getOnlyPortInst(), pi, pinPt, objPt, polyBounds.getWidth());
			if (ai != null && noEndExtend) ai.setExtended(false);
			return;
		}

		// can we extend horizontally
		if (polyCtr.getY() >= portRect.getMinY() && polyCtr.getY() <= portRect.getMaxY())
		{
			// going left to the poly or right?
			Point2D objPt = new Point2D.Double(portRect.getCenterX(), polyCtr.getY());
			Point2D pinPt = null;
			boolean noEndExtend = false;
			double endExtension = polyBounds.getHeight() / 2;
			if (polyBounds.getWidth() < polyBounds.getHeight())
			{
				// arc is so short that it will stick out with end extension
				noEndExtend = true;
				endExtension = 0;
			}
			if (polyCtr.getX() > portRect.getCenterX())
			{
				// going right to the poly
				pinPt = new Point2D.Double(polyBounds.getMaxX() - endExtension, polyCtr.getY());
			} else
			{
				// going left to the poly
				pinPt = new Point2D.Double(polyBounds.getMinX() + endExtension, polyCtr.getY());
			}
			NodeInst ni1 = NodeInst.makeInstance(np, pinPt, polyBounds.getHeight(), polyBounds.getHeight(), newCell);
			ArcInst ai = realizeArc(ap, ni1.getOnlyPortInst(), pi, pinPt, objPt, polyBounds.getHeight());
			if (ai != null && noEndExtend) ai.setExtended(false);
		}
	}

	private PortInst findArcEnd(ArcInst ai, PolyBase poly)
	{
		// see if one end of the arc touches the poly
		Point2D head = ai.getHead().getLocation();
		Point2D tail = ai.getTail().getLocation();
		int ang = GenMath.figureAngle(tail, head);
		int angPlus = (ang + 900) % 3600;
		int angMinus = (ang + 2700) % 3600;
		double width = (ai.getWidth() - ai.getProto().getWidthOffset()) / 2;

		// see if the head end touches
		Point2D headButFarther = new Point2D.Double(head.getX() + width * GenMath.cos(ang), head.getY() + width * GenMath.sin(ang));
		if (poly.contains(headButFarther)) return ai.getHead().getPortInst();
		Point2D headOneSide = new Point2D.Double(head.getX() + width * GenMath.cos(angPlus), head.getY() + width * GenMath.sin(angPlus));
		if (poly.contains(headOneSide)) return ai.getHead().getPortInst();
		Point2D headOtherSide = new Point2D.Double(head.getX() + width * GenMath.cos(angPlus), head.getY() + width * GenMath.sin(angPlus));
		if (poly.contains(headOtherSide)) return ai.getHead().getPortInst();

		// see if the tail end touches
		Point2D tailButFarther = new Point2D.Double(tail.getX() - width * GenMath.cos(ang), tail.getY() - width * GenMath.sin(ang));
		if (poly.contains(tailButFarther)) return ai.getTail().getPortInst();
		Point2D tailOneSide = new Point2D.Double(tail.getX() - width * GenMath.cos(angPlus), tail.getY() - width * GenMath.sin(angPlus));
		if (poly.contains(tailOneSide)) return ai.getTail().getPortInst();
		Point2D tailOtherSide = new Point2D.Double(tail.getX() - width * GenMath.cos(angPlus), tail.getY() - width * GenMath.sin(angPlus));
		if (poly.contains(tailOtherSide)) return ai.getTail().getPortInst();
		
		return null;
	}

	private boolean polysTouch(PolyBase poly1, PolyBase poly2)
	{
		Point2D [] points1 = poly1.getPoints();
		Point2D [] points2 = poly2.getPoints();
		if (points1.length > points2.length)
		{
			Point2D [] swapPts = points1;   points1 = points2;   points2 = swapPts;
			PolyBase swapPoly = poly1;   poly1 = poly2;   poly2 = swapPoly;
		}

		// check every vertex in poly1 to see if any are in poly2
		for(int i=0; i<points1.length; i++)
			if (poly2.contains(points1[i])) return true;

		// check every midpoint in poly1 to see if any are in poly2
		for(int i=0; i<points1.length; i++)
		{
			int last = i-1;
			if (last < 0) last = points1.length-1;
			Point2D midPoint = new Point2D.Double((points1[last].getX() + points1[i].getX()) / 2,
				(points1[last].getY() + points1[i].getY()) / 2);
			if (poly2.contains(midPoint)) return true;
		}
		return false;
	}

	private HashMap getNetsThatTouch(PolyBase poly, Layer layer)
	{
		// make a map of networks that touch the polygon, and the objects on them
		HashMap netsThatTouch = new HashMap();

		// find nodes that touch
		Rectangle2D bounds = poly.getBounds2D();
		Point2D centerPoint = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
		Netlist nl = newCell.acquireUserNetlist();
		for(Iterator it = newCell.searchIterator(bounds); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (!(geom instanceof NodeInst)) continue;
			NodeInst ni = (NodeInst)geom;
			if (ni.getProto() instanceof Cell) continue;
			AffineTransform trans = ni.rotateOut();
			Technology tech = ni.getProto().getTechnology();
			Poly [] nodePolys = tech.getShapeOfNode(ni, null, true, true, null);
			for(int i=0; i<nodePolys.length; i++)
			{
				Poly nodePoly = nodePolys[i];
				if (geometricLayer(nodePoly.getLayer()) != layer) continue;
				nodePoly.transform(trans);
				if (polysTouch(nodePoly, poly))
				{
					// node touches the unconnected poly: get network information
					PrimitivePort pp = (PrimitivePort)nodePoly.getPort();
					if (pp == null) continue;
					PortInst pi = findPortInstClosestToPoly(ni, pp, centerPoint);
					Network net = nl.getNetwork(pi);
					if (net != null)
					{
						netsThatTouch.put(net, pi);
						break;
					}
				}
			}
		}

		// find arcs that touch (only include if no nodes are on the network)
		for(Iterator it = newCell.searchIterator(bounds); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (!(geom instanceof ArcInst)) continue;
			ArcInst ai = (ArcInst)geom;
			Technology tech = ai.getProto().getTechnology();
			Poly [] polys = tech.getShapeOfArc(ai);
			for(int i=0; i<polys.length; i++)
			{
				Poly arcPoly = polys[i];
				if (geometricLayer(arcPoly.getLayer()) != layer) continue;
				if (polysTouch(arcPoly, poly))
				{
					Network net = nl.getNetwork(ai, 0);
					if (net != null)
					{
						if (netsThatTouch.get(net) == null) netsThatTouch.put(net, ai);
						break;
					}
				}
			}
		}
		return netsThatTouch;
	}

	/********************************************** MISCELLANEOUS EXTRACTION HELPERS **********************************************/

	/**
	 * Method to scan the geometric information and convert it all to pure layer nodes in the new cell.
	 * When all other extractions are done, this is done to preserve any geometry that is unaccounted-for.
	 */
	private void convertAllGeometry()
	{
		for(Iterator lIt = merge.getKeyIterator(); lIt.hasNext(); )
		{
			Layer layer = (Layer)lIt.next();
			ArcProto ap = (ArcProto)arcsForLayer.get(layer);
			List polyList = merge.getMergedPoints(layer, true);
			for(Iterator pIt = polyList.iterator(); pIt.hasNext(); )
			{
				PolyBase poly = (PolyBase)pIt.next();

				// special case: a rectangle on a routable layer that is wide enough: make it an arc
				if (ap != null)
				{
					Rectangle2D polyBounds = poly.getBox();
					if (polyBounds == null)
					{
						Rectangle2D totalBounds = poly.getBounds2D();
						if (originalMerge.contains(layer, totalBounds))
							polyBounds = totalBounds;
					}
					if (polyBounds != null)
					{
						double width = polyBounds.getWidth();
						double height = polyBounds.getHeight();
						double actualWidth = ap.getDefaultWidth() - ap.getWidthOffset();
						if (width >= actualWidth && height >= actualWidth)
						{
							PrimitiveNode np = ((PrimitiveArc)ap).findPinProto();
							if (width > height)
							{
								// make a horizontal arc
								Point2D end1 = new Point2D.Double(polyBounds.getMinX()+height/2, polyBounds.getCenterY());
								Point2D end2 = new Point2D.Double(polyBounds.getMaxX()-height/2, polyBounds.getCenterY());
								NodeInst ni1 = NodeInst.makeInstance(np, end1, height, height, newCell);
								NodeInst ni2 = NodeInst.makeInstance(np, end2, height, height, newCell);
								ArcInst ai = realizeArc(ap, ni1.getOnlyPortInst(), ni2.getOnlyPortInst(), end1, end2, height);
							} else
							{
								// make a vertical arc
								Point2D end1 = new Point2D.Double(polyBounds.getCenterX(), polyBounds.getMinY()+width/2);
								Point2D end2 = new Point2D.Double(polyBounds.getCenterX(), polyBounds.getMaxY()-width/2);
								NodeInst ni1 = NodeInst.makeInstance(np, end1, width, width, newCell);
								NodeInst ni2 = NodeInst.makeInstance(np, end2, width, width, newCell);
								ArcInst ai = realizeArc(ap, ni1.getOnlyPortInst(), ni2.getOnlyPortInst(), end1, end2, width);
							}
							continue;
						}
					}
				}

				// just generate more pure-layer nodes
				double centerX = poly.getCenterX();
				double centerY = poly.getCenterY();
				Point2D center = new Point2D.Double(centerX, centerY);
				NodeInst ni = NodeInst.makeInstance(poly.getLayer().getPureLayerNode(), center,
					poly.getBounds2D().getWidth(), poly.getBounds2D().getHeight(), newCell);

				// add on trace information if the shape is nonmanhattan
				if (poly.getBox() == null)
				{
					// store the trace
					Point2D [] points = poly.getPoints();
					Point2D [] newPoints = new Point2D[points.length];
					for(int i=0; i<points.length; i++)
					{
						newPoints[i] = new Point2D.Double(points[i].getX() - centerX, points[i].getY() - centerY);
					}
					ni.newVar(NodeInst.TRACE, newPoints);
				}
			}
		}
	}

	/**
	 * Method to create a node and remove its geometry from the database.
	 * @param pNp the node to create.
	 * @param centerX the new node's X location.
	 * @param centerY the new node's Y location.
	 * @param width the new node's width.
	 * @param height the new node's height.
	 */
	private void realizeNode(PrimitiveNode pNp, double centerX, double centerY, double width, double height, int angle)
	{
		NodeInst ni = NodeInst.makeInstance(pNp, new Point2D.Double(centerX, centerY), width, height, newCell, angle, null, 0);
		if (ni == null) return;

		// now remove the generated layers from the Merge
		AffineTransform trans = ni.rotateOut();
		Poly [] polys = tech.getShapeOfNode(ni);
		for(int i=0; i<polys.length; i++)
		{
			Poly poly = polys[i];
			Layer layer = poly.getLayer();

			// make sure the geometric database is made up of proper layers
			layer = geometricLayer(layer);

			poly.transform(trans);
			merge.subPolygon(layer, poly);
		}
	}

	/**
	 * Method to create an arc and remove its geometry from the database.
	 * @param ap the arc to create.
	 * @param pi1 the first side of the arc.
	 * @param pi2 the second side of the arc
	 * @param width the width of the arc.
	 */
	private ArcInst realizeArc(ArcProto ap, PortInst pi1, PortInst pi2, Point2D pt1, Point2D pt2, double width)
	{
		ArcInst ai = ArcInst.makeInstance(ap, width, pi1, pi2, pt1, pt2, null);
		if (ai == null) return null;

		// now remove the generated layers from the Merge
		Poly [] polys = tech.getShapeOfArc(ai);
		for(int i=0; i<polys.length; i++)
		{
			Poly poly = polys[i];
			Layer layer = poly.getLayer();

			// make sure the geometric database is made up of proper layers
			layer = geometricLayer(layer);

			merge.subPolygon(layer, poly);
		}
		return ai;
	}

	/**
	 * Method to locate a node at a specific point with a specific type.
	 * @param pt the center location of the desired node.
	 * @param pin the type of the desired node.
	 * @param size the size of the node (if it must be created).
	 * @return a node of that type at that location.
	 * If there is none there, it is created.
	 */
	private NodeInst wantNodeAt(Point2D pt, NodeProto pin, double size)
	{
		Rectangle2D bounds = new Rectangle2D.Double(pt.getX(), pt.getY(), 0, 0);
		for(Iterator it = newCell.searchIterator(bounds); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (!(geom instanceof NodeInst)) continue;
			NodeInst ni = (NodeInst)geom;
			if (ni.getProto() != pin) continue;
			if (ni.getAnchorCenter().equals(pt)) return ni;
		}
		NodeInst ni = NodeInst.makeInstance(pin, pt, size, size, newCell);
		return ni;
	}

	private Layer geometricLayer(Layer layer)
	{
		Layer.Function fun = layer.getFunction();

		// convert gate to poly1
		if (fun == Layer.Function.GATE)
		{
			Layer polyLayer = (Layer)layerForFunction.get(Layer.Function.POLY1);
			if (polyLayer != null) return polyLayer;
		}

		// all active is one layer
		if (fun == Layer.Function.DIFFP || fun == Layer.Function.DIFFN)
			fun = Layer.Function.DIFF;

		// ensure the first one for the given function
		Layer properLayer = (Layer)layerForFunction.get(fun);
		if (properLayer != null) return properLayer;
		return layer;

	}

	/**
	 * Debugging method to report the coordinates of all geometry on a given layer.
	 */
	public static String describeLayer(PolyMerge merge, Layer layer)
	{
		List polyList = merge.getMergedPoints(layer, true);
		if (polyList == null) return "DOES NOT EXIST";
		StringBuffer sb = new StringBuffer();
		for(Iterator iit=polyList.iterator(); iit.hasNext(); )
		{
			PolyBase p = (PolyBase)iit.next();
			Point2D [] points = p.getPoints();
			sb.append(" [");
			for(int j=0; j<points.length; j++)
			{
				Point2D pt = points[j];
				if (j > 0) sb.append(" ");
				sb.append("("+TextUtils.formatDouble(pt.getX())+","+TextUtils.formatDouble(pt.getY())+")");
			}
			sb.append("]");
		}
		return sb.toString();
	}
}
