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
 *    Proper way to find large contacts (currently too slow)
 *    Serpentine transistors
 *    When extracting skeletons, allow mix of narrower and wider
 *    Cell instances in wiring paths
 *    Implicit connections where high-level nodes overlap
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
		System.out.print("Geometry merged, now extracting vias...");
		extractVias();

		// now extract transistors
		System.out.print("done\nNow extracting transistors...");
		extractTransistors();

		// look for wires and pins
		System.out.print("done\nNow making wires...");
		makeWires();

		// dump any remaining layers back in as extra pure layer nodes
		System.out.print("done\nConverting remaining geometry...");
		convertAllGeometry();

		// show the new version
		System.out.println("done");
		WindowFrame.createEditWindow(newCell);
	}

	/********************************************** WIRE EXTRACTION **********************************************/

	private static final boolean NEWWIRES = true;

	private static class TouchingNode
	{
		NodeInst ni;
		PortInst pi;
		double width;
		Point2D endPt;
	}

	private void makeWires()
	{
		for(Iterator lIt = merge.getKeyIterator(); lIt.hasNext(); )
		{
			Layer layer = (Layer)lIt.next();
			Layer.Function fun = layer.getFunction();
			if (fun.isDiff() || fun.isPoly() || fun.isMetal())
			{
				// figure out which arc proto to use for the layer
				ArcProto.Function oFun = null;
				if (fun.isMetal()) oFun = ArcProto.Function.getMetal(fun.getLevel());
				if (fun.isPoly()) oFun = ArcProto.Function.getPoly(fun.getLevel());
				if (oFun == null) continue;
				ArcProto type = null;
				for(Iterator it = tech.getArcs(); it.hasNext(); )
				{
					ArcProto ap = (ArcProto)it.next();
					if (ap.getFunction() == oFun) { type = ap;   break; }
				}
				if (type == null) continue;

				// examine the geometry on the layer
				List polyList = merge.getMergedPoints(layer, true);
				
				for(Iterator pIt = polyList.iterator(); pIt.hasNext(); )
				{
					PolyBase poly = (PolyBase)pIt.next();

					// convert a polygon to wires
					convertToWires(poly, layer, type);
				}
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
					PrimitivePort touchingPort = (PrimitivePort)oPoly.getPort();
					PortInst touchingPi = ni.findPortInstFromProto(touchingPort);
					PrimitiveNode pnp = (PrimitiveNode)ni.getProto();
					Poly touchingPoly = touchingPi.getPoly();
					double bestDist = pt.distance(new Point2D.Double(touchingPoly.getCenterX(), touchingPoly.getCenterY()));
					for(Iterator pIt = pnp.getPorts(); pIt.hasNext(); )
					{
						PrimitivePort prP = (PrimitivePort)pIt.next();
						if (prP.getTopology() == touchingPort.getTopology())
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
			realizeArc(ap, pi1, pi2, cl.width);
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
				piRet = ni.getOnlyPortInst();
			} else
			{
				if (!isHub)
					cl.end.setLocation(cl.end.getX() - GenMath.cos(angle)*cl.width/2,
						cl.end.getY() - GenMath.sin(angle)*cl.width/2);
				NodeInst ni = wantNodeAt(cl.end, pin, cl.width);
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

		// now pull out the relevant ones
		List validCenterlines = new ArrayList();
		double goodWidth = -1;
		for(Iterator it = centerlines.iterator(); it.hasNext(); )
		{
			Centerline cl = (Centerline)it.next();
			if (cl.width < ap.getDefaultWidth()) continue;
			if (goodWidth < 0) goodWidth = cl.width;
			if (cl.width != goodWidth) break;
			validCenterlines.add(cl);
		}

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
			if (cl1.width < cl2.width) return -1;
			if (cl1.width > cl2.width) return 1;
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
System.out.println("Consider contact layer "+layer.getName()+" at ("+centerX+","+centerY+")");

				// now look through the list to see if anything matches
				for(Iterator it = possibleVias.iterator(); it.hasNext(); )
				{
					PossibleVia pv = (PossibleVia)it.next();
					PrimitiveNode pNp = pv.pNp;
					
					// determine the range of shrinkage values for the layers in the contact
					double largestShrink = pv.shrink[0];
					for(int i=1; i<pv.layers.length; i++)
						if (pv.shrink[i] > largestShrink) largestShrink = pv.shrink[i];

System.out.println("  Could it be primitive "+pNp.getName());
					List contactArea = (List)possibleAreas.get(pNp);
					if (contactArea == null)
					{
						// merge all other layers and see how big the contact can be
						originalMerge.insetLayer(pv.layers[0], tempLayer1, (largestShrink-pv.shrink[0])/2);
//System.out.println("    Inset layer "+pv.layers[0].getName()+" by "+((largestShrink-pv.shrink[0])/2));
						for(int i=1; i<pv.layers.length; i++)
						{
//List xx = originalMerge.getMergedPoints(pv.layers[i], true);
//System.out.println("    Inset layer "+pv.layers[i].getName()+" by "+((largestShrink-pv.shrink[i])/2)+" is "+xx);
							originalMerge.insetLayer(pv.layers[i], tempLayer2, (largestShrink-pv.shrink[i])/2);
							originalMerge.intersectLayers(tempLayer1, tempLayer2, tempLayer1);
						}
						contactArea = originalMerge.getMergedPoints(tempLayer1, true);
						possibleAreas.put(pNp, contactArea);
					}

					//if (contactArea == null) System.out.println("  Cannot be "+pNp.getName());
					if (contactArea == null) continue;
					Rectangle2D largest = findLargestRectangle(contactArea, centerX, centerY, pv.minWidth-largestShrink, pv.minHeight-largestShrink);
					if (largest == null) continue;
System.out.println("  Contact is surrounded by area from "+largest.getMinX()+"<=X<="+largest.getMaxX()+" and "+largest.getMinY()+"<=Y<="+largest.getMaxY());
					centerX = largest.getCenterX();
					centerY = largest.getCenterY();
					double desiredWidth = largest.getWidth() + largestShrink;
					double desiredHeight = largest.getHeight() + largestShrink;

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
							boolean foundLayer = false;
							if (originalMerge.contains(nLayer, rect)) foundLayer = true;
//System.out.println("    So, is there layer "+nLayer.getName()+" in area "+rect.getMinX()+"<=X<="+rect.getMaxX()+" and "+rect.getMinY()+"<=Y<="+rect.getMaxY()+" answer="+foundLayer);
							if (!foundLayer)
							{
//System.out.println("      That layer is:" +describeLayer(originalMerge, nLayer));
								gotMinimum = false;
								break;
							}
						}
						if (gotMinimum)
						{
							realizeNode(pNp, centerX, centerY, desiredWidth, desiredHeight, 0);
System.out.println("  Is part of "+pNp.getName()+" contact which is "+desiredWidth+"x"+desiredHeight+" at ("+centerX+","+centerY+")");
							// remove other cuts in this area
							for(int o=1; o<polyList.size(); o++)
							{
								PolyBase oPoly = (PolyBase)polyList.get(o);
								double x = oPoly.getCenterX();
								double y = oPoly.getCenterY();
								if (largest.contains(x, y))
								{
System.out.println("    and also includes cut at ("+x+","+y+")");
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
			List polyList = merge.getMergedPoints(layer, true);
			for(Iterator pIt = polyList.iterator(); pIt.hasNext(); )
			{
				PolyBase poly = (PolyBase)pIt.next();
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
	private void realizeArc(ArcProto ap, PortInst pi1, PortInst pi2, double width)
	{
		ArcInst ai = ArcInst.makeInstance(ap, width, pi1, pi2);
		if (ai == null) return;

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
