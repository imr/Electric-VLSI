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
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
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
 */
public class Connectivity
{
	private PolyMerge merge;
	private PolyMerge originalMerge;
	private Cell oldCell;
	private Cell newCell;
	private Technology tech;
	private HashMap layerForFunction;

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

		// build the mapping from any layer to the proper ones for the geometric database
		layerForFunction = new HashMap();
		for(Iterator it = tech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			Layer.Function fun = layer.getFunction();
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
				NodeInst newNi = NodeInst.makeInstance(ni.getProto(), ni.getAnchorCenter(), ni.getXSizeWithMirror(), ni.getYSizeWithMirror(), newCell);
				if (newNi == null)
				{
					System.out.println("Problem creating new cell instance of " + ni.getProto().describe());
					return;
				}
				continue;
			}

			// extract the geometry from the pure-layer node (has only 1 layer)
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
				merge.addPolygon(layer, poly);
			}
		}

		// throw all arcs into the new cell, too
		for(Iterator aIt = oldCell.getArcs(); aIt.hasNext(); )
		{
			ArcInst ai = (ArcInst)aIt.next();
		}

		// now remember the original merge
		originalMerge = new PolyMerge();
		originalMerge.addMerge(merge, new AffineTransform());

		// start by extracting vias
		extractVias();

		// now extract transistors
		extractTransistors();

		// look for wires and pins
		makeWires();

		// dump any remaining layers back in as extra pure layer nodes
		convertAllGeometry();

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

					// see what touches the layer
					List touchingNodes = findTouchingNodes(poly);
//					System.out.println("Geometry on layer "+layer.getName()+" touches "+touchingNodes.size()+" nodes");

					if (touchingNodes.size() == 2)
					{
						// connects two nodes: run wires
						if (poly.getBox() == null)
						{
							// complex shape: need to figure out path
							continue;
						}
						TouchingNode tn1 = (TouchingNode)touchingNodes.get(0);
						TouchingNode tn2 = (TouchingNode)touchingNodes.get(1);
						double width = Math.min(tn1.width, tn2.width);
						realizeArc(type, tn1.pi, tn2.pi, width);
					}

					if (touchingNodes.size() == 1)
					{
						// runs from a node to empty space
						if (poly.getBox() == null)
						{
							// complex shape: need to figure out path
							continue;
						}
						TouchingNode tn = (TouchingNode)touchingNodes.get(0);
						PortInst pi1 = tn.pi;
						
						// find the other end
						Point2D [] points = poly.getPoints();
						double bestDist = 0;
						Point2D oppositePoint = null;
						for(int i=0; i<points.length; i++)
						{
							int last = i-1;
							if (last < 0) last = points.length-1;
							if (points[last].equals(points[i])) continue;
							Point2D endPoint = new Point2D.Double((points[last].getX() + points[i].getX()) / 2, (points[last].getY() + points[i].getY()) / 2);
							double dist = endPoint.distance(tn.endPt);
							if (dist > bestDist)
							{
								bestDist = dist;
								oppositePoint = endPoint;
							}
						}
						PrimitiveNode pinProto = ((PrimitiveArc)type).findPinProto();
						NodeInst ni = NodeInst.makeInstance(pinProto, oppositePoint, tn.width, tn.width, newCell);
						PortInst pi2 = ni.getOnlyPortInst();
						if (oppositePoint != null)
						{
							
							realizeArc(type, pi1, pi2, tn.width);
						}
					}
				}
			}
		}
	}

	private List findTouchingNodes(PolyBase poly)
	{
		Layer layer = geometricLayer(poly.getLayer());
		List touchingNodes = new ArrayList();
		Rectangle2D checkBounds = poly.getBounds2D();
		checkBounds.setRect(checkBounds.getMinX() - 1, checkBounds.getMinY() - 1, checkBounds.getWidth() + 2, checkBounds.getHeight() + 2);
		for(Iterator it = newCell.searchIterator(checkBounds); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (!(geom instanceof NodeInst)) continue;
			NodeInst ni = (NodeInst)geom;
			Poly [] polys = tech.getShapeOfNode(ni, null, true, true, null);
			for(int i=0; i<polys.length; i++)
			{
				Poly oPoly = polys[i];
				Layer oLayer = geometricLayer(oPoly.getLayer());
				if (layer != oLayer) continue;

				Rectangle2D oPolyBox = oPoly.getBox();
				if (oPolyBox == null) continue;

				// do the polys touch?
				double dist = DBMath.round(poly.polyDistance(oPolyBox));
				if (dist <= 0)
				{
					TouchingNode tn = new TouchingNode();
					tn.ni = ni;
					tn.pi = ni.findPortInstFromProto(oPoly.getPort());
					if (tn.pi == null)
					{
						System.out.println("Can't find port for node "+ni.describe()+" and port "+oPoly.getPort());
						continue;
					}
					
					// see which side of this polygon touched the node
					Point2D [] points = poly.getPoints();
					for(int j=0; j<points.length; j++)
					{
						int last = j-1;
						if (last < 0) last = points.length-1;
//						if (points[last].equals(points[j])) continue;
						Point2D endPoint = new Point2D.Double((points[last].getX() + points[j].getX()) / 2, (points[last].getY() + points[j].getY()) / 2);
						if (oPoly.contains(endPoint))
						{
							tn.width = points[last].distance(points[j]);
							tn.endPt = endPoint;
							break;
						}
					}
					touchingNodes.add(tn);
					break;
				}
			}
		}
		return touchingNodes;
	}

	/********************************************** VIA EXTRACTION **********************************************/

	private static class PossibleVia
	{
		PrimitiveNode pNp;
		double minWidth, minHeight;
		Layer [] layers;
		double [] xShrink;
		double [] yShrink;

		PossibleVia(PrimitiveNode pNp)
		{
			this.pNp = pNp;
		}
	}

	/**
	 * Method to scan the geometric information for possible contacts and vias.
	 * Any vias found are created in the new cell and removed from the geometric information.
	 */
	private void extractVias()
	{
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
			for(Iterator pIt = polyList.iterator(); pIt.hasNext(); )
			{
				PolyBase poly = (PolyBase)pIt.next();
				double centerX = poly.getCenterX();
				double centerY = poly.getCenterY();

				// now look through the list to see if anything matches
				for(Iterator it = possibleVias.iterator(); it.hasNext(); )
				{
					PossibleVia pv = (PossibleVia)it.next();
					PrimitiveNode pNp = pv.pNp;

					// check that the cut/via is the right size

					// check all other layers
					boolean gotMinimum = true;
					for(int i=0; i<pv.layers.length; i++)
					{
						Layer nLayer = pv.layers[i];
						double minLayWid = pv.minWidth - pv.xShrink[i];
						double minLayHei = pv.minHeight - pv.yShrink[i];
						Rectangle2D rect = new Rectangle2D.Double(centerX - minLayWid/2, centerY - minLayHei/2, minLayWid, minLayHei);
						boolean foundLayer = false;
						if (originalMerge.contains(nLayer, rect)) foundLayer = true;
						if (!foundLayer)
						{
							gotMinimum = false;
							break;
						}
					}
					if (gotMinimum)
					{
						realizeNode(pNp, centerX, centerY, pv.minWidth, pv.minHeight);
					}
				}
			}
		}
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
			if (fun != PrimitiveNode.Function.CONTACT) continue;

			// create a PossibleVia object to test for them
			PossibleVia pv = new PossibleVia(pNp);
			Technology.NodeLayer [] nLayers = pNp.getLayers();
			pv.layers = new Layer[nLayers.length-1];
			pv.xShrink = new double[nLayers.length-1];
			pv.yShrink = new double[nLayers.length-1];
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

					pv.layers[fill] = nLay.getLayer();
					pv.xShrink[fill] = nLay.getLeftEdge().getAdder() * 2;
					pv.yShrink[fill] = nLay.getBottomEdge().getAdder() * 2;
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
		// find the poly and active layers
		Layer polyLayer = null;
		Layer pseudoPolyLayer = null;
		Layer nActiveLayer = null;
		Layer pActiveLayer = null;
		for(Iterator it = tech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			Layer.Function fun = layer.getFunction();
			if (polyLayer == null && fun == Layer.Function.POLY1) polyLayer = layer;
			if (fun == Layer.Function.POLY1 && (layer.getFunctionExtras()&Layer.Function.PSEUDO) != 0) pseudoPolyLayer = layer;
			if (nActiveLayer == null && fun == Layer.Function.DIFFN) nActiveLayer = layer;
			if (pActiveLayer == null && fun == Layer.Function.DIFFP) pActiveLayer = layer;
		}
		if (polyLayer == null) return;
		polyLayer = polyLayer.getNonPseudoLayer();

		// find the transistors to create
		PrimitiveNode pTransistor = null, nTransistor = null;
		for(Iterator it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode pNp = (PrimitiveNode)it.next();
			if (pTransistor == null && pNp.getFunction() == PrimitiveNode.Function.TRAPMOS) pTransistor = pNp;
			if (nTransistor == null && pNp.getFunction() == PrimitiveNode.Function.TRANMOS) nTransistor = pNp;
		}
		if (nActiveLayer != null && nTransistor != null)
		{
			nActiveLayer = nActiveLayer.getNonPseudoLayer();
			findTransistors(polyLayer, nActiveLayer, pseudoPolyLayer, nTransistor);
		}
		if (pActiveLayer != null && pTransistor != null)
		{
			pActiveLayer = pActiveLayer.getNonPseudoLayer();
			findTransistors(polyLayer, pActiveLayer, pseudoPolyLayer, pTransistor);
		}
	}

	private void findTransistors(Layer polyLayer, Layer activeLayer, Layer pseudoPolyLayer, PrimitiveNode transistor)
	{
		merge.intersectLayers(polyLayer, activeLayer, pseudoPolyLayer);

		// look at all of the pieces of this layer
		List polyList = merge.getMergedPoints(pseudoPolyLayer, true);
		if (polyList == null) return;
		for(Iterator pIt = polyList.iterator(); pIt.hasNext(); )
		{
			PolyBase poly = (PolyBase)pIt.next();
System.out.println("Transistor at ("+poly.getCenterX()+","+poly.getCenterY()+")");
			Rectangle2D transBox = poly.getBox();
			if (transBox != null)
			{
				// found a manhattan transistor
				SizeOffset so = transistor.getProtoSizeOffset();
				double width = transBox.getWidth() + so.getLowXOffset() + so.getHighXOffset();
				double height = transBox.getHeight() + so.getLowYOffset() + so.getHighYOffset();
				realizeNode(transistor, poly.getCenterX(), poly.getCenterY(), width, height);
			}
		}
		merge.deleteLayer(pseudoPolyLayer);
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
	private void realizeNode(PrimitiveNode pNp, double centerX, double centerY, double width, double height)
	{
//System.out.println("Found contact "+pNp.getName());
		NodeInst ni = NodeInst.makeInstance(pNp, new Point2D.Double(centerX, centerY), width, height, newCell);
		if (ni == null) return;

		// now remove the generated layers from the Merge
		Poly [] polys = tech.getShapeOfNode(ni);
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

	private Layer geometricLayer(Layer layer)
	{
		Layer.Function fun = layer.getFunction();

		// convert gate to poly1
		if (fun == Layer.Function.GATE)
		{
			Layer polyLayer = (Layer)layerForFunction.get(Layer.Function.POLY1);
			if (polyLayer != null) return polyLayer;
		}

		// ensure the first one for the given function
		Layer properLayer = (Layer)layerForFunction.get(fun);
		if (properLayer != null) return properLayer;
		return layer;

	}

	/**
	 * Debugging method to report the coordinates of all geometry on a given layer.
	 */
	private String describeLayer(Layer layer)
	{
		List polyList = merge.getMergedPoints(layer, true);
		if (polyList == null) return "DOES NOT EXIST";
		System.out.println("  layer is");
		StringBuffer sb = new StringBuffer();
		for(Iterator iit=polyList.iterator(); iit.hasNext(); )
		{
			PolyBase p = (PolyBase)iit.next();
			Point2D [] points = p.getPoints();
			sb.append(" [");
			for(int j=0; j<points.length; j++)
			{
				Point2D pt = points[j];
				if (j > 0) sb.append(",");
				sb.append("("+TextUtils.formatDouble(pt.getX())+","+TextUtils.formatDouble(pt.getY())+")");
			}
			sb.append("]");
		}
		return sb.toString();
	}
}
