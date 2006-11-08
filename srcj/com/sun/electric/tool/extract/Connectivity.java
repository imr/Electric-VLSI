/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Connectivity.java
 * Module to do node extraction (extract connectivity from a pure-layout cell)
 * Written by Steven M. Rubin, Sun Microsystems.
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
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.geometry.PolyMerge;
import com.sun.electric.database.geometry.GenMath.MutableBoolean;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Technology.TechPoint;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.routing.AutoStitch;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * This is the Connectivity extractor.
 *
 * Still to do:
 *    Nonmanhattan contacts
 *    Explicit connection where two cells overlap
 */
public class Connectivity
{
	/** true to prevent objects smaller than minimum size */	private static final boolean ENFORCEMINIMUMSIZE = false;
	/** amount to scale values before merging */				private static final double SCALEFACTOR = DBMath.GRID;
	/** true to debug centerline determination */				private static final boolean DEBUGCENTERLINES = false;

	/** the current technology for extraction */				private Technology tech;
	/** layers to use for given arc functions */				private HashMap<Layer.Function,Layer> layerForFunction;
	/** the layer to use for "polysilicon" geometry */			private Layer polyLayer;
	/** temporary layers to use for geometric manipulation */	private Layer tempLayer1, tempLayer2;
	/** the layer to use for "active" geometry */				private Layer activeLayer;
	/** associates arc prototypes with layers */				private HashMap<Layer,ArcProto> arcsForLayer;
	/** map of extracted cells */								private HashMap<Cell,Cell> convertedCells;
	/** map of export indices in each extracted cell */			private HashMap<Cell,GenMath.MutableInteger> exportNumbers;
	/** true if this is a P-well process (presume P-well) */	private boolean pWellProcess;
	/** true if this is a N-well process (presume N-well) */	private boolean nWellProcess;

	/**
	 * Method to examine the current cell and extract it's connectivity in a new one.
	 * @param recursive true to recursively extract the hierarchy below this cell.
	 */
	public static void extractCurCell(boolean recursive)
	{
//double low = 147.85, high = 150.45;
//double d = high-low;
//System.out.println("distance between "+low+" and "+high+" is "+d);
		Cell curCell = Job.getUserInterface().needCurrentCell();
		if (curCell == null)
		{
			System.out.println("Must be editing a cell with pure layer nodes.");
			return;
		}
		new ExtractJob(curCell, recursive);
	}

	private static class ExtractJob extends Job
	{
		private Cell cell, newCell;
		private boolean recursive;

		private ExtractJob(Cell cell, boolean recursive)
		{
			super("Extract Connectivity from " + cell, Extract.getExtractTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.recursive = recursive;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			Connectivity c = new Connectivity(cell.getTechnology());
			String expansionPattern = Extract.getCellExpandPattern();
			Pattern pat;
            try
            {
                pat = Pattern.compile(expansionPattern, Pattern.CASE_INSENSITIVE);
            } catch(PatternSyntaxException e)
            {
            	System.out.println("Pattern syntax error on '" + expansionPattern + "': " + e.getMessage());
                pat = null;
            }
			newCell = c.doExtract(cell, recursive, pat, true);
			fieldVariableChanged("newCell");
			return true;
		}

		public void terminateOK()
		{
			// show the new version
			UserInterface ui = Job.getUserInterface();
			EditWindow_ wnd = ui.displayCell(newCell);
	
			// highlight pure layer nodes
			for(Iterator<NodeInst> it = newCell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				PrimitiveNode.Function fun = ni.getFunction();
				if (fun == PrimitiveNode.Function.NODE)
					wnd.addElectricObject(ni, newCell);
			}
		}
	}

	/**
	 * Constructor to initialize connectivity extraction.
	 * @param tech the Technology to extract to.
	 */
	private Connectivity(Technology tech)
	{
		this.tech = tech;
		convertedCells = new HashMap<Cell,Cell>();
		exportNumbers = new HashMap<Cell,GenMath.MutableInteger>();

		// find important layers
		polyLayer = null;
		tempLayer1 = tempLayer2 = null;
		activeLayer = null;
		for(Iterator<Layer> it = tech.getLayers(); it.hasNext(); )
		{
			Layer layer = it.next();
			Layer.Function fun = layer.getFunction();
			if (polyLayer == null && fun == Layer.Function.POLY1) polyLayer = layer;
			if (fun == Layer.Function.POLY1 && (layer.getFunctionExtras()&Layer.Function.PSEUDO) != 0) tempLayer1 = layer;
			if (fun == Layer.Function.METAL1 && (layer.getFunctionExtras()&Layer.Function.PSEUDO) != 0) tempLayer2 = layer;
			if (activeLayer == null && fun.isDiff()) activeLayer = layer;
		}
		polyLayer = polyLayer.getNonPseudoLayer();
		activeLayer = activeLayer.getNonPseudoLayer();

		// figure out which arcs to use for a layer
		arcsForLayer = new HashMap<Layer,ArcProto>();
		for(Iterator<Layer> it = tech.getLayers(); it.hasNext(); )
		{
			Layer layer = it.next();
			Layer.Function fun = layer.getFunction();
			if (fun.isDiff() || fun.isPoly() || fun.isMetal())
			{
				ArcProto.Function oFun = null;
				if (fun.isMetal()) oFun = ArcProto.Function.getMetal(fun.getLevel());
				if (fun.isPoly()) oFun = ArcProto.Function.getPoly(fun.getLevel());
				if (oFun == null) continue;
				ArcProto type = null;
				for(Iterator<ArcProto> aIt = tech.getArcs(); aIt.hasNext(); )
				{
					ArcProto ap = aIt.next();
					if (ap.getFunction() == oFun) { type = ap;   break; }
				}
				if (type != null) arcsForLayer.put(layer, type);
			}
		}

		// build the mapping from any layer to the proper ones for the geometric database
		layerForFunction = new HashMap<Layer.Function,Layer>();
		for(Iterator<Layer> it = tech.getLayers(); it.hasNext(); )
		{
			Layer layer = it.next();
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
	private Cell doExtract(Cell oldCell, boolean recursive, Pattern pat, boolean top)
	{
		if (recursive)
		{
			// first see if subcells need to be converted
			for(Iterator<NodeInst> it = oldCell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				if (ni.isCellInstance())
				{
					Cell subCell = (Cell)ni.getProto();

					// do not recurse if this subcell will be expanded
					if (pat != null)
					{
	                    Matcher mat = pat.matcher(subCell.noLibDescribe());
	                    if (mat.find()) continue;
					}

                    Cell convertedCell = convertedCells.get(subCell);
					if (convertedCell == null)
					{
						doExtract(subCell, recursive, pat, false);
					}
				}
			}
		}

		// create the new version of the cell
		String newCellName = oldCell.getName() + "{" + oldCell.getView().getAbbreviation() + "}";
		Cell newCell = Cell.makeInstance(oldCell.getLibrary(), newCellName);
		if (newCell == null)
		{
			System.out.println("Cannot create new cell: " + newCellName);
			return null;
		}
		convertedCells.put(oldCell, newCell);

		// create a merge for the geometry in the cell
		PolyMerge merge = new PolyMerge();

		// convert the nodes
		Set<Cell> expandedCells = new HashSet<Cell>();
		extractCell(oldCell, newCell, pat, expandedCells, merge, GenMath.MATID);
		if (expandedCells.size() > 0)
		{
			System.out.print("These cells were expanded:");
			for(Cell c : expandedCells)
				System.out.print(" " + c.describe(false));
			System.out.println();
		}

		// determine if this is a "P-well" or "N-well" process
		findMissingWells(merge);

		// now remember the original merge
		PolyMerge originalMerge = new PolyMerge();
		originalMerge.addMerge(merge, new AffineTransform());

		System.out.println("Extracting " + oldCell + "...");

		// start by extracting vias
		extractVias(merge, originalMerge, newCell);
		System.out.println("Extracted vias");

		// now extract transistors
		extractTransistors(merge, originalMerge, newCell);
		System.out.println("Extracted transistors");

		// extend geometry that sticks out in space
		extendGeometry(merge, originalMerge, newCell, true);
		System.out.println("Extracted extensions");

		// look for wires and pins
		if (makeWires(merge, originalMerge, newCell)) return newCell;
		System.out.println("Extracted wires");

		// convert any geometry that connects two networks
		extendGeometry(merge, originalMerge, newCell, false);
		System.out.println("Extracted connections");

		// dump any remaining layers back in as extra pure layer nodes
		convertAllGeometry(merge, originalMerge, newCell);
		System.out.println("Extracted geometry");

		// cleanup by auto-stitching
		PolyMerge originalUnscaledMerge = new PolyMerge();
		double shrinkage = 1.0 / SCALEFACTOR;
		AffineTransform shrink = new AffineTransform(shrinkage, 0, 0, shrinkage, 0, 0);
		originalUnscaledMerge.addMerge(originalMerge, shrink);
		AutoStitch.runAutoStitch(newCell, null, null, originalUnscaledMerge, null, false);
		System.out.println("Extraction done.");
		return newCell;
	}

	/**
	 * Method to extract a cell's contents into the merge.
	 * @param oldCell the cell being extracted.
	 * @param newCell the new cell being created.
	 * @param pat a pattern of subcell names that will be expanded.
	 * @param expandedCells a set of cells that matched the pattern and were expanded.
	 * @param merge the merge to be filled.
	 * @param prevTrans the transformation coming into this cell.
	 */
	private void extractCell(Cell oldCell, Cell newCell, Pattern pat, Set<Cell> expandedCells, PolyMerge merge, AffineTransform prevTrans)
	{
		Map<NodeInst,NodeInst> newNodes = new HashMap<NodeInst,NodeInst>();
		for(Iterator<NodeInst> nIt = oldCell.getNodes(); nIt.hasNext(); )
		{
			NodeInst ni = nIt.next();
			if (ni.getProto() == Generic.tech.cellCenterNode) continue;

			// see if the node can be copied or must be extracted
			NodeProto copyType = null;
			if (ni.isCellInstance())
			{
				Cell subCell = (Cell)ni.getProto();

				// if subcell is expanded, do it now
				if (pat != null)
				{
	                Matcher mat = pat.matcher(subCell.noLibDescribe());
	                if (mat.find())
	                {
	                	// expanding the subcell
	                	expandedCells.add(subCell);
	                	AffineTransform subTrans = ni.rotateOut(ni.translateOut(prevTrans));
	                	extractCell(subCell, newCell, pat, expandedCells, merge, subTrans);
	                	continue;
	                }
				}

            	// subcell not expanded, figure out what gets placed in the new cell
                copyType = convertedCells.get(subCell);
				if (copyType == null) copyType = subCell;
			} else
			{
				PrimitiveNode np = (PrimitiveNode)ni.getProto();
				if (np.getFunction() != PrimitiveNode.Function.NODE) copyType = ni.getProto();
			}

			// copy it now if requested
			if (copyType != null)
			{
				double sX = ni.getXSize();
				double sY = ni.getYSize();
				if (copyType instanceof Cell)
				{
					Rectangle2D cellBounds = ((Cell)copyType).getBounds();
					sX = cellBounds.getWidth();
					sY = cellBounds.getHeight();
				}
				Point2D instanceAnchor = new Point2D.Double(0, 0);
				prevTrans.transform(ni.getAnchorCenter(), instanceAnchor);
				NodeInst newNi = NodeInst.makeInstance(copyType, instanceAnchor, sX, sY,
					newCell, ni.getOrient(), ni.getName(), ni.getTechSpecific());
				if (newNi == null)
				{
					System.out.println("Problem creating new instance of " + ni.getProto());
					return;
				}
				newNodes.put(ni, newNi);

				// copy exports too
				for(Iterator<Export> it = ni.getExports(); it.hasNext(); )
				{
					Export e = it.next();
					PortInst pi = newNi.findPortInstFromProto(e.getOriginalPort().getPortProto());
					Export.newInstance(newCell, pi, e.getName());
				}
				continue;
			}

			// extract the geometry from the pure-layer node
			AffineTransform trans = ni.rotateOut(prevTrans);
			Poly [] polys = tech.getShapeOfNode(ni);
			for(int j=0; j<polys.length; j++)
			{
				Poly poly = polys[j];

				// get the layer for the geometry
				Layer layer = poly.getLayer();
				if (layer == null) continue;

				// make sure the geometric database is made up of proper layers
				layer = geometricLayer(layer);

				// finally add the geometry to the merge
				poly.transform(trans);
				Point2D [] points = poly.getPoints();
				if (Extract.isGridAlignExtraction())
				{
					for(int i=0; i<points.length; i++)
						Job.getUserInterface().alignToGrid(points[i]);
				}

				for(int i=0; i<points.length; i++)
					points[i].setLocation(scaleUp(points[i].getX()), scaleUp(points[i].getY()));
				merge.addPolygon(layer, poly);
			}
		}

		// throw all arcs into the new cell, too
		for(Iterator<ArcInst> aIt = oldCell.getArcs(); aIt.hasNext(); )
		{
			ArcInst ai = aIt.next();
			NodeInst end1 = newNodes.get(ai.getHeadPortInst().getNodeInst());
			NodeInst end2 = newNodes.get(ai.getTailPortInst().getNodeInst());
			if (end1 == null || end2 == null) continue;
			PortInst pi1 = end1.findPortInstFromProto(ai.getHeadPortInst().getPortProto());
			PortInst pi2 = end2.findPortInstFromProto(ai.getTailPortInst().getPortProto());
			Point2D headLocation = new Point2D.Double(0, 0);
			Point2D tailLocation = new Point2D.Double(0, 0);
			prevTrans.transform(ai.getHeadLocation(), headLocation);
			prevTrans.transform(ai.getTailLocation(), tailLocation);
			ArcInst newAi = ArcInst.makeInstance(ai.getProto(), ai.getLambdaFullWidth(), pi1, pi2,
				headLocation, tailLocation, ai.getName());
		}
	}

	/**
	 * Method to determine if this is a "p-well" or "n-well" process.
	 * Examines the merge to see which well layers are found.
	 * @param merge the merged geometry.
	 */
	private void findMissingWells(PolyMerge merge)
	{
		boolean hasPWell = false, hasNWell = false, hasWell = false;

        for (Layer layer : merge.getKeySet())
		{
			Layer.Function fun = layer.getFunction();
			if (fun == Layer.Function.WELL) hasWell = true;
			if (fun == Layer.Function.WELLP) hasPWell = true;
			if (fun == Layer.Function.WELLN) hasNWell = true;
		}
		if (!hasPWell)
		{
			pWellProcess = true;
			System.out.println("Presuming a P-well process");
			return;
		}
		if (!hasNWell && !hasWell)
		{
			nWellProcess = true;
			System.out.println("Presuming an N-well process");
		}
	}

	/********************************************** WIRE EXTRACTION **********************************************/

	private static class TouchingNode
	{
		NodeInst ni;
		PortInst pi;
		double width;
		Point2D endPt;
	}

	/**
	 * Method to extract wires from the merge.
	 * @param merge the merged geometry that remains (after contacts and transistors are extracted).
	 * @param originalMerge the original merge with all geometry.
	 * @param newCell the new Cell in which wires are placed.
	 * @return true on error.
	 */
	private boolean makeWires(PolyMerge merge, PolyMerge originalMerge, Cell newCell)
	{
		// make a list of layers that could be turned into wires
		List<Layer> wireLayers = new ArrayList<Layer>();
        for (Layer layer : merge.getKeySet())
		{
			Layer.Function fun = layer.getFunction();
			if (fun.isDiff() || fun.isPoly() || fun.isMetal())
			{
				// make sure there is an arc that exists for the layer
				ArcProto ap = arcsForLayer.get(layer);
				if (ap != null) wireLayers.add(layer);
			}
		}

		// examine each wire layer, looking for a skeletal structure that approximates it
		for (Layer layer : wireLayers)
		{
			// figure out which arc proto to use for the layer
			ArcProto ap = arcsForLayer.get(layer);

			// examine the geometry on the layer
			List<PolyBase> polyList = getMergePolys(merge, layer);
			for(PolyBase poly : polyList)
			{
				// reduce the geometry to a skeleton of centerlines
				double minWidth = 1;
				if (ENFORCEMINIMUMSIZE) minWidth = scaleUp(ap.getDefaultLambdaFullWidth());
				List<Centerline> lines = findCenterlines(poly, layer, minWidth, merge, originalMerge);

				// now realize the wires
				for(Centerline cl : lines)
				{
					Point2D loc1Unscaled = new Point2D.Double();
					PortInst pi1 = locatePortOnCenterline(cl, loc1Unscaled, layer, ap, true, newCell);
					Point2D loc2Unscaled = new Point2D.Double();
					PortInst pi2 = locatePortOnCenterline(cl, loc2Unscaled, layer, ap, false, newCell);
					Point2D loc1 = new Point2D.Double(scaleUp(loc1Unscaled.getX()), scaleUp(loc1Unscaled.getY()));
					Point2D loc2 = new Point2D.Double(scaleUp(loc2Unscaled.getX()), scaleUp(loc2Unscaled.getY()));

					// make sure the wire fits
					double wid = cl.width - scaleUp(ap.getLambdaWidthOffset());
					MutableBoolean headExtend = new MutableBoolean(true), tailExtend = new MutableBoolean(true);
					if (!originalMerge.arcPolyFits(layer, loc1, loc2, wid, headExtend, tailExtend))
					{
						// arc does not fit, try reducing width
						wid = scaleUp(ap.getLambdaWidthOffset());
						if (!originalMerge.arcPolyFits(layer, loc1, loc2, wid, headExtend, tailExtend)) continue;
						cl.width = 0;
					}

					// create the wire
					ArcInst ai = realizeArc(ap, pi1, pi2, loc1Unscaled, loc2Unscaled, cl.width / SCALEFACTOR + ap.getLambdaWidthOffset(),
						!headExtend.booleanValue(), !tailExtend.booleanValue(), merge);
					if (ai == null)
					{
						System.out.println("  Failed to run arc " + ap.getName() + " from (" + loc1Unscaled.getX() + "," +
							loc1Unscaled.getY() + ") on node " + pi1.getNodeInst().describe(false) + " to (" +
							loc2Unscaled.getX() + "," + loc2Unscaled.getY() + ") on node " + pi2.getNodeInst().describe(false));
						return true;
					}
				}
			}
		}

		// examine each wire layer, looking for a simple rectangle that covers it
		for(Layer layer : wireLayers)
		{
			Layer.Function fun = layer.getFunction();

			// figure out which arc proto to use for the layer
			ArcProto ap = arcsForLayer.get(layer);

			// examine the geometry on the layer
			List<PolyBase> polyList = getMergePolys(merge, layer);
			for(PolyBase poly : polyList)
			{
				Rectangle2D bounds = poly.getBounds2D();
				Poly rectPoly = new Poly(bounds);
				if (!originalMerge.contains(layer, rectPoly)) continue;

				// determine the endpoints of the arc
				Point2D loc1, loc2;
				double width = Math.min(bounds.getWidth(), bounds.getHeight());
				if (bounds.getWidth() > bounds.getHeight())
				{
					loc1 = new Point2D.Double((bounds.getMinX() + width/2) / SCALEFACTOR, bounds.getCenterY() / SCALEFACTOR);
					loc2 = new Point2D.Double((bounds.getMaxX() - width/2) / SCALEFACTOR, bounds.getCenterY() / SCALEFACTOR);
				} else
				{
					loc1 = new Point2D.Double(bounds.getCenterX() / SCALEFACTOR, (bounds.getMinY() + width/2) / SCALEFACTOR);
					loc2 = new Point2D.Double(bounds.getCenterX() / SCALEFACTOR, (bounds.getMaxY() - width/2) / SCALEFACTOR);
				}
				PortInst pi1 = wantConnectingNodeAt(loc1, ap, width / SCALEFACTOR, newCell);
				PortInst pi2 = wantConnectingNodeAt(loc2, ap, width / SCALEFACTOR, newCell);
				realizeArc(ap, pi1, pi2, loc1, loc2, width / SCALEFACTOR + ap.getLambdaWidthOffset(), false, false, merge);
			}
		}
		return false;
	}

	/**
	 * Method to locate a port on a node at a specific point with a specific connectivity.
	 * @param pt the center location of the desired node.
	 * @param ap the type of the arc that must connect.
	 * @param size the size of the node (if it must be created).
	 * @param newCell the cell in which to locate or place the node.
	 * @return the port on the node that is at the proper point.
	 * If there is none there, a node is created.
	 */
	private PortInst wantConnectingNodeAt(Point2D pt, ArcProto ap, double size, Cell newCell)
	{
		Rectangle2D bounds = new Rectangle2D.Double(pt.getX(), pt.getY(), 0, 0);
		for(Iterator<Geometric> it = newCell.searchIterator(bounds); it.hasNext(); )
		{
			Geometric geom = it.next();
			if (!(geom instanceof NodeInst)) continue;
			NodeInst ni = (NodeInst)geom;
			for(Iterator<PortInst> pIt = ni.getPortInsts(); pIt.hasNext(); )
			{
				PortInst pi = pIt.next();
				PortProto pp = pi.getPortProto();
				if (!pp.connectsTo(ap)) continue;
				Poly poly = pi.getPoly();
				if (poly.contains(pt)) return pi;
			}
		}
		NodeInst ni = createNode(ap.findPinProto(), pt, size, size, newCell);
		return ni.getOnlyPortInst();
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
		for(Iterator<PortProto> pIt = pnp.getPorts(); pIt.hasNext(); )
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
		EPoint startUnscaled, endUnscaled;
		boolean startHub, endHub;
		double width;
		boolean handled;
		int angle;

		Centerline(double width, Point2D start, Point2D end)
		{
			this.width = width;
			this.start = start;
			this.startUnscaled = new EPoint(start.getX() / SCALEFACTOR, start.getY() / SCALEFACTOR);
			this.endUnscaled = new EPoint(end.getX() / SCALEFACTOR, end.getY() / SCALEFACTOR);
			this.end = end;
			startHub = endHub = false;
			if (start.equals(end)) angle = -1; else
				angle = GenMath.figureAngle(start, end);
		}

		void setStart(double x, double y)
		{
			start.setLocation(x, y);
			startUnscaled = new EPoint(x / SCALEFACTOR, y / SCALEFACTOR);
		}

		void setEnd(double x, double y)
		{
			end.setLocation(x, y);
			endUnscaled = new EPoint(x / SCALEFACTOR, y / SCALEFACTOR);
		}

		public String toString()
		{
			return "CENTERLINE from ("+start.getX()+","+start.getY()+") to ("+end.getX()+","+end.getY()+") wid="+width+
			", len="+start.distance(end);
		}
	}

	/**
	 * Method to return the port and location to use for one end of a Centerline.
	 * @param cl the Centerline to connect
	 * @param loc1 it's location (values returned through this object!)
	 * @param layer the layer associated with the Centerline.
	 * @param ap the type of arc to create.
	 * @param startSide true to 
	 * @param newCell the Cell in which to find ports.
	 * @return the PortInst on the Centerline.
	 */
	private PortInst locatePortOnCenterline(Centerline cl, Point2D loc1, Layer layer, ArcProto ap, boolean startSide, Cell newCell)
	{
		PortInst piRet = null;
		boolean isHub = cl.endHub;
		EPoint startPoint = cl.endUnscaled;
		if (startSide)
		{
			isHub = cl.startHub;
			startPoint = cl.startUnscaled;
		}
		if (!isHub)
		{
			List<PortInst> possiblePorts = findPortInstsTouchingPoint(startPoint, layer, newCell);
			for(PortInst pi : possiblePorts)
			{
				Poly portPoly = pi.getPoly();
				Point2D [] points = portPoly.getPoints();
				if (points.length == 1)
				{
				    Point2D iPt = GenMath.intersect(cl.startUnscaled, cl.angle, points[0], (cl.angle+900)%3600);
				    if (iPt != null)
					{
						loc1.setLocation(iPt.getX(), iPt.getY());
						piRet = pi;
						break;
					}
				} else
				{
					if (portPoly.contains(startPoint))
					{
						loc1.setLocation(startPoint);
						piRet = pi;
						break;						
					}
					for(int i=0; i<points.length; i++)
					{
						int last = i-1;
						if (last < 0) last = points.length-1;
						Point2D portLineFrom = points[last];
						Point2D portLineTo = points[i];
						Point2D interPt = null;
						if (portLineFrom.equals(portLineTo))
						{
							interPt = GenMath.intersect(cl.startUnscaled, cl.angle, portLineFrom, (cl.angle+900)%3600);
						} else
						{
							int angPortLine = GenMath.figureAngle(portLineFrom, portLineTo);
							interPt = GenMath.intersect(portLineFrom, angPortLine, cl.startUnscaled, cl.angle);
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
						if (!portPoly.contains(loc1)) continue;
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
			PrimitiveNode pin = ap.findPinProto();
			int ang = GenMath.figureAngle(cl.start, cl.end);
			double xOff = GenMath.cos(ang) * cl.width/2;
			double yOff = GenMath.sin(ang) * cl.width/2;
			if (startSide)
			{
				if (!isHub)
					cl.setStart(cl.start.getX() + xOff, cl.start.getY() + yOff);
				NodeInst ni = wantNodeAt(cl.startUnscaled, pin, cl.width / SCALEFACTOR, newCell);
				loc1.setLocation(cl.startUnscaled.getX(), cl.startUnscaled.getY());
				piRet = ni.getOnlyPortInst();
			} else
			{
				if (!isHub)
					cl.setEnd(cl.end.getX() - xOff, cl.end.getY() - yOff);
				NodeInst ni = wantNodeAt(cl.endUnscaled, pin, cl.width / SCALEFACTOR, newCell);
				loc1.setLocation(cl.endUnscaled.getX(), cl.endUnscaled.getY());
				piRet = ni.getOnlyPortInst();
			}
		}
		return piRet;
	}

	/**
	 * Method to locate a node at a specific point with a specific type.
	 * @param pt the center location of the desired node.
	 * @param pin the type of the desired node.
	 * @param size the size of the node (if it must be created).
	 * @param newCell the cell in which to locate or place the node.
	 * @return a node of that type at that location.
	 * If there is none there, it is created.
	 */
	private NodeInst wantNodeAt(Point2D pt, NodeProto pin, double size, Cell newCell)
	{
		Rectangle2D bounds = new Rectangle2D.Double(pt.getX(), pt.getY(), 0, 0);
		for(Iterator<Geometric> it = newCell.searchIterator(bounds); it.hasNext(); )
		{
			Geometric geom = it.next();
			if (!(geom instanceof NodeInst)) continue;
			NodeInst ni = (NodeInst)geom;
			if (ni.getProto() != pin) continue;
			if (ni.getAnchorCenter().equals(pt)) return ni;
		}
		NodeInst ni = createNode(pin, pt, size, size, newCell);
		return ni;
	}

	private List<PortInst> findPortInstsTouchingPoint(Point2D pt, Layer layer, Cell newCell)
	{
		List<PortInst> touchingNodes = new ArrayList<PortInst>();
		boolean mightCreateExports = false;
		Rectangle2D checkBounds = new Rectangle2D.Double(pt.getX(), pt.getY(), 0, 0);
		for(Iterator<Geometric> it = newCell.searchIterator(checkBounds); it.hasNext(); )
		{
			Geometric geom = it.next();
			if (!(geom instanceof NodeInst)) continue;
			NodeInst ni = (NodeInst)geom;
			if (ni.isCellInstance())
			{
				Cell subCell = (Cell)ni.getProto();
				boolean found = false;
				for(Iterator<PortInst> pIt = ni.getPortInsts(); pIt.hasNext(); )
				{
					PortInst pi = pIt.next();
					Poly portPoly = pi.getPoly();
					if (portPoly.contains(pt))
					{
						touchingNodes.add(pi);
						found = true;
						break;
					}
				}
				if (found) continue;

				// remember that a cell was found...might have to create exports on it
				mightCreateExports = true;
				continue;
			}
			Poly [] polys = tech.getShapeOfNode(ni, true, true, null);
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
						System.out.println("Can't find port for "+ni+" and "+oPoly.getPort());
						continue;
					}
					touchingNodes.add(touchingPi);
					break;
				}
			}
		}

		// if no ports were found but there were cells, should create new exports in the cells
		if (touchingNodes.size() == 0 && mightCreateExports)
		{
			for(Iterator<Geometric> it = newCell.searchIterator(checkBounds); it.hasNext(); )
			{
				Geometric geom = it.next();
				if (!(geom instanceof NodeInst)) continue;
				NodeInst ni = (NodeInst)geom;
				if (!ni.isCellInstance()) continue;

				// can we create an export on the cell?
				PortInst pi = makePort(ni, layer, pt);
				if (pi != null)
				{
					touchingNodes.add(pi);
					break;
				}
			}
		}
		return touchingNodes;
	}

	/**
	 * Method to create an export so that a node can connect to a layer.
	 * @param ni the node that needs to connect.
	 * @param layer the layer on which to connect.
	 * @param pt the location on the node for the connection.
	 * @return the PortInst on the node (null if it cannot be created).
	 */
	private PortInst makePort(NodeInst ni, Layer layer, Point2D pt)
	{
		Cell subCell = (Cell)ni.getProto();

		AffineTransform transIn = ni.rotateIn(ni.translateIn());
		Point2D inside = new Point2D.Double();
		transIn.transform(pt, inside);
		Rectangle2D bounds = new Rectangle2D.Double(inside.getX(), inside.getY(), 0, 0);
		for(Iterator<Geometric> it = subCell.searchIterator(bounds); it.hasNext(); )
		{
			Geometric geom = it.next();
			if (geom instanceof ArcInst) continue;
			NodeInst subNi = (NodeInst)geom;
			PortInst foundPi = null;
			if (subNi.isCellInstance())
			{
				PortInst pi = makePort(subNi, layer, inside);
				if (pi != null) foundPi = pi;
			} else
			{
				Technology tech = subNi.getProto().getTechnology();
				AffineTransform trans = subNi.rotateOut();
				Poly [] polyList = tech.getShapeOfNode(subNi, true, true, null);
				for(int i=0; i<polyList.length; i++)
				{
					Poly poly = polyList[i];
					if (poly.getPort() == null) continue;
					if (geometricLayer(poly.getLayer()) != layer) continue;
					poly.transform(trans);
					if (poly.contains(inside))
					{
						// found polygon that touches the point.  Make the export
						foundPi = findPortInstClosestToPoly(subNi, (PrimitivePort)poly.getPort(), inside);
						break;
					}
				}
			}
			if (foundPi != null)
			{
				GenMath.MutableInteger exportNumber = exportNumbers.get(subCell);
				if (exportNumber == null)
				{
					exportNumber = new GenMath.MutableInteger(1);
					exportNumbers.put(subCell, exportNumber);
				}
				String exportName = "E" + exportNumber.intValue();
				exportNumber.increment();
				Export e = Export.newInstance(subCell, foundPi, exportName);
				return ni.findPortInstFromProto(e);
			}
		}
		return null;
	}

	/********************************************** VIA/CONTACT EXTRACTION **********************************************/

	private static class PossibleVia
	{
		PrimitiveNode pNp;
		int rotation;
		double minWidth, minHeight;
		double largestShrink;
		Layer [] layers;
		double [] shrinkL, shrinkR, shrinkT, shrinkB;

		PossibleVia(PrimitiveNode pNp, int numLayers)
		{
			this.pNp = pNp;
			layers = new Layer[numLayers];
			shrinkL = new double[numLayers];
			shrinkR = new double[numLayers];
			shrinkT = new double[numLayers];
			shrinkB = new double[numLayers];
		}
	}

	/**
	 * Method to scan the geometric information for possible contacts and vias.
	 * Any vias found are created in the new cell and removed from the geometric information.
	 */
	private void extractVias(PolyMerge merge, PolyMerge originalMerge, Cell newCell)
	{
		// make a list of all via/cut layers in the technology
		List<Layer> layers = new ArrayList<Layer>();
        for (Layer layer : merge.getKeySet())
		{
			Layer.Function fun = layer.getFunction();
			if (fun.isContact()) layers.add(layer);
		}

		// examine all vias/cuts for possible contacts
		for (Layer layer : layers)
		{
			// compute the possible via nodes that this layer could become
			List<PossibleVia> possibleVias = findPossibleVias(layer);

			// get all of the geometry on the cut/via layer
			List<PolyBase> cutList = getMergePolys(merge, layer);

			// the new way to extract vias
			for(PolyBase cut : cutList)
			{
				for(PossibleVia pv : possibleVias)
				{
					if (extractOneVia(layer, pv, cut, merge, originalMerge, newCell)) break;
				}				
			}

			// the old way to extract vias
//			for(PossibleVia pv : possibleVias)
//			{
//				// inexact contact extraction: merge all other layers and see how big the contact can be
//				extractAVia(layer, pv, cutList, merge, originalMerge, newCell);
//			}
		}
	}

	/**
	 * Method to return a list of PossibleVia objects for a given cut/via layer.
	 * @param lay the cut/via layer to find.
	 * @return a List of PossibleVia objects that use the layer as a contact.
	 */
	private List<PossibleVia> findPossibleVias(Layer lay)
	{
		List<PossibleVia> possibleVias = new ArrayList<PossibleVia>();
		for(Iterator<PrimitiveNode> nIt = tech.getNodes(); nIt.hasNext(); )
		{
			PrimitiveNode pNp = nIt.next();
			PrimitiveNode.Function fun = pNp.getFunction();
			if (fun != PrimitiveNode.Function.CONTACT && fun != PrimitiveNode.Function.WELL &&
				fun != PrimitiveNode.Function.SUBSTRATE) continue;

			// TODO do we really need to vet each contact?
			// For some reason, the MOCMOS technology with MOCMOS foundry shows the "A-" nodes (A-Metal-1-Metal-2-Con)
			// but these primitives are not fully in existence, and crash here
			boolean bogus = false;
			Technology.NodeLayer [] nLayers = pNp.getLayers();
			for(int i=0; i<nLayers.length; i++)
			{
				Technology.NodeLayer nLay = nLayers[i];
				TechPoint [] debugPoints = nLay.getPoints();
				if (debugPoints == null || debugPoints.length <= 0)
				{
					bogus = true;
					break;
				}
			}
			if (bogus) continue;

			// load the layer information
			boolean cutFound = false;
			List<Technology.NodeLayer> pvLayers = new ArrayList<Technology.NodeLayer>();
			for(int i=0; i<nLayers.length; i++)
			{
				// examine one layer of the primitive node
				Technology.NodeLayer nLay = nLayers[i];
				Layer nLayer = nLay.getLayer();

				// ignore well layers if the process doesn't have them
				Layer.Function lFun = nLayer.getFunction();
				if (lFun == Layer.Function.WELLP && pWellProcess) continue;
				if (lFun == Layer.Function.WELLN && nWellProcess) continue;

				boolean cutLayer = false;
				if (nLayer == lay)
				{
					// this is the target cut layer: mark it
					cutLayer = true;
				} else
				{
					// special case for active/poly cut confusion
					if (nLayer.getFunction() == lay.getFunction()) cutLayer = true;
				}
				if (cutLayer) cutFound = true; else
				{
					pvLayers.add(nLay);
				}
			}
			if (!cutFound) continue;

			// create a PossibleVia object to test for them
			PossibleVia pv = new PossibleVia(pNp, pvLayers.size());
			pv.rotation = 0;
			pv.minWidth = scaleUp(pNp.getDefWidth());
			pv.minHeight = scaleUp(pNp.getDefHeight());
			pv.largestShrink = 0;
			int fill = 0;
			for(Technology.NodeLayer nLay : pvLayers)
			{
				// create the PossibleVia to describe the geometry
				pv.layers[fill] = geometricLayer(nLay.getLayer());
				pv.shrinkL[fill] = scaleUp(pNp.getDefWidth() * (0.5 + nLay.getLeftEdge().getMultiplier()) + nLay.getLeftEdge().getAdder());
				pv.shrinkR[fill] = scaleUp(pNp.getDefWidth() * (0.5 - nLay.getRightEdge().getMultiplier()) - nLay.getRightEdge().getAdder());
				pv.shrinkT[fill] = scaleUp(pNp.getDefHeight() * (0.5 - nLay.getTopEdge().getMultiplier()) - nLay.getTopEdge().getAdder());
				pv.shrinkB[fill] = scaleUp(pNp.getDefHeight() * (0.5 + nLay.getBottomEdge().getMultiplier()) + nLay.getBottomEdge().getAdder());
				double ls = Math.max(Math.max(pv.shrinkL[fill], pv.shrinkR[fill]),
					Math.max(pv.shrinkT[fill], pv.shrinkB[fill]));
				if (fill == 0 || ls > pv.largestShrink) pv.largestShrink = ls;
				fill++;
			}

			// add this to the list of possible vias
			possibleVias.add(pv);

			// if this via is asymmetric, add others with different rotation
			boolean hvSymmetry = true, rotSymmetry = true;
			for(int i=0; i<pv.layers.length; i++)
			{
				if (pv.shrinkL[i] != pv.shrinkR[i] || pv.shrinkT[i] != pv.shrinkB[i])
				{
					rotSymmetry = false;
					break;
				}
				if (pv.shrinkL[i] != pv.shrinkT[i])
				{
					hvSymmetry = false;
					break;
				}
			}
			if (!hvSymmetry)
			{
				// horizontal/vertical symmetry missing: add a 90-degree rotation
				PossibleVia newPV = new PossibleVia(pv.pNp, pv.layers.length);
				newPV.rotation = 90;
				newPV.largestShrink = pv.largestShrink;
				newPV.minWidth = pv.minWidth;
				newPV.minHeight = pv.minHeight;
				for(int i=0; i<pv.layers.length; i++)
				{
					newPV.layers[i] = pv.layers[i];
					newPV.shrinkL[i] = pv.shrinkT[i];
					newPV.shrinkR[i] = pv.shrinkB[i];
					newPV.shrinkT[i] = pv.shrinkR[i];
					newPV.shrinkB[i] = pv.shrinkL[i];
				}
				possibleVias.add(newPV);
			}
			if (!rotSymmetry)
			{
				// rotational symmetry missing: add a 180-degree and 270-degree rotation
				PossibleVia newPV = new PossibleVia(pv.pNp, pv.layers.length);
				newPV.rotation = 180;
				newPV.largestShrink = pv.largestShrink;
				newPV.minWidth = pv.minWidth;
				newPV.minHeight = pv.minHeight;
				for(int i=0; i<pv.layers.length; i++)
				{
					newPV.layers[i] = pv.layers[i];
					newPV.shrinkL[i] = pv.shrinkR[i];
					newPV.shrinkR[i] = pv.shrinkL[i];
					newPV.shrinkT[i] = pv.shrinkB[i];
					newPV.shrinkB[i] = pv.shrinkT[i];
				}
				possibleVias.add(newPV);

				newPV = new PossibleVia(pv.pNp, pv.layers.length);
				newPV.rotation = 270;
				newPV.largestShrink = pv.largestShrink;
				newPV.minWidth = pv.minWidth;
				newPV.minHeight = pv.minHeight;
				for(int i=0; i<pv.layers.length; i++)
				{
					newPV.layers[i] = pv.layers[i];
					newPV.shrinkL[i] = pv.shrinkB[i];
					newPV.shrinkR[i] = pv.shrinkT[i];
					newPV.shrinkT[i] = pv.shrinkL[i];
					newPV.shrinkB[i] = pv.shrinkR[i];
				}
				possibleVias.add(newPV);
			}
		}
		return possibleVias;
	}

	/**
	 * Method to see if a via exists around a given cut layer.
	 * @param layer the layer in question.
	 * @param pv the possible via.
	 * @param cut the cut layer that may be the via.
	 * @param merge the merge data at this point (after extracted nodes are removed).
	 * @param originalMerge the original merge data.
	 * @param newCell the cell in which extracted geometry goes.
	 * @return true if a contact was extracted.
	 */
	private boolean extractOneVia(Layer layer, PossibleVia pv, PolyBase cut, PolyMerge merge, PolyMerge originalMerge, Cell newCell)
	{
		// can only handle manhattan contacts now
		Rectangle2D cutBox = cut.getBox();
		if (cutBox == null) return false;

		double cX = cutBox.getCenterX();
		double cY = cutBox.getCenterY();
//System.out.println("DOES "+pv.pNp.describe(false)+" ROTATED "+pv.rotation+" FIT AT ("+cX+","+cY+")?");
		for(int i=0; i<pv.layers.length; i++)
		{
			double lX = cX - pv.minWidth/2 + pv.shrinkL[i];
			double hX = cX + pv.minWidth/2 - pv.shrinkR[i];
			double lY = cY - pv.minHeight/2 + pv.shrinkB[i];
			double hY = cY + pv.minHeight/2 - pv.shrinkT[i];
			double layerCX = (lX + hX) / 2;
			double layerCY = (lY + hY) / 2;
			PolyBase layerPoly = new PolyBase(layerCX, layerCY, hX-lX, hY-lY);
			if (!originalMerge.contains(pv.layers[i], layerPoly))
			{
//System.out.println("  LAYER "+pv.layers[i].getName()+" DOES NOT (NEEDS "+lX+"<=X<="+hX+" AND "+lY+"<=Y<="+hY+")");
				return false;
			}
		}
//System.out.println("  YES!");

		// found: create the node
		realizeNode(pv.pNp, cX, cY, pv.minWidth, pv.minHeight, pv.rotation*10, null, merge, newCell);
		return true;
	}

//	/**
//	 * The old way of extracting vias.
//	 */
//	private void extractAVia(Layer layer, PossibleVia pv, List<PolyBase> cutList, PolyMerge merge, PolyMerge originalMerge, Cell newCell)
//	{
//		// build a polygon that covers all of the layers in this via
//		boolean subtractPoly = false;
//		for(int i=0; i<pv.layers.length; i++)
//		{
//			if (pv.layers[i] == activeLayer) subtractPoly = true;
//			double minShrink = Math.min(Math.min(pv.shrinkL[i], pv.shrinkR[i]), Math.min(pv.shrinkT[i], pv.shrinkB[i]));
//			double shrinkage = (pv.largestShrink-minShrink) / 2;
//			if (i == 0) originalMerge.insetLayer(pv.layers[i], tempLayer1, shrinkage); else
//			{
//				if (shrinkage == 0) originalMerge.intersectLayers(tempLayer1, pv.layers[i], tempLayer1); else
//				{
//					originalMerge.insetLayer(pv.layers[i], tempLayer2, shrinkage);
//					originalMerge.intersectLayers(tempLayer1, tempLayer2, tempLayer1);					
//				}
//			}
//		}
//
//		// if active was involved, subtract areas of polysilicon because the cuts cannot follow across a transistor
//		if (subtractPoly)
//		{
//			if (!originalMerge.isEmpty(polyLayer))
//				originalMerge.subtractLayers(tempLayer1, polyLayer, tempLayer1);
//		}
//
//		// get a list of areas that could possibly be contacts
//		List<PolyBase> contactArea = getMergePolys(originalMerge, tempLayer1);
//		if (contactArea == null) return;
//
//		// look at all of the cut/via pieces
//		for(int ind=0; ind < cutList.size(); ind++)
//		{
//			PolyBase cutPoly = cutList.get(ind);
//			double centerX = cutPoly.getCenterX();
//			double centerY = cutPoly.getCenterY();
//
//			Rectangle2D cutBounds = cutPoly.getBounds2D();
//			double minWid = cutBounds.getWidth(), minHei = cutBounds.getHeight();
//			if (ENFORCEMINIMUMSIZE)
//			{
//				minWid = Math.max(minWid, pv.minWidth-pv.largestShrink);
//				minHei = Math.max(minHei, pv.minHeight-pv.largestShrink);
//			}
//			double maxWid = 0, maxHei = 0;
//			if (Extract.isExactCutExtraction())
//			{
//				maxWid = pv.minWidth;
//				maxHei = pv.minHeight;
//			}
//			Rectangle2D largest = findLargestRectangle(contactArea, centerX, centerY, minWid, minHei, maxWid, maxHei);
//			if (largest == null) continue;//System.out.println("  Found "+pv.pNp.getName()+" in rectangle "+largest);
//			centerX = largest.getCenterX();
//			centerY = largest.getCenterY();
//			double desiredWidth = largest.getWidth() + pv.largestShrink;
//			double desiredHeight = largest.getHeight() + pv.largestShrink;
//
//			// check all other layers
//			for(;;)
//			{
//				boolean gotMinimum = true;
//				for(int i=0; i<pv.layers.length; i++)
//				{
//					Layer nLayer = pv.layers[i];
//					double minLayWid = desiredWidth - pv.shrinkL[i] - pv.shrinkR[i];
//					double minLayHei = desiredHeight - pv.shrinkT[i] - pv.shrinkB[i];
//					Rectangle2D rect = new Rectangle2D.Double(centerX - minLayWid/2, centerY - minLayHei/2, minLayWid, minLayHei);
//					if (!originalMerge.contains(nLayer, rect))
//					{
//						gotMinimum = false;
//						break;
//					}
//				}
//				if (gotMinimum)
//				{
//					checkCutSize(cutPoly, pv.pNp, layer);
//					realizeNode(pv.pNp, centerX, centerY, desiredWidth, desiredHeight, 0, null, merge, newCell);
//	
//					// must remove the contact geometry explicitly because the new contact may not have cuts in the exact location
//					merge.subtract(layer, cutPoly);
//					originalMerge.deleteLayer(tempLayer2);
//					double wid = desiredWidth - pv.largestShrink;
//					double hei = desiredHeight - pv.largestShrink;
//					Rectangle2D rect = new Rectangle2D.Double(centerX-wid/2, centerY-hei/2, wid, hei);
//					originalMerge.addPolygon(tempLayer2, new Poly(rect));
//					originalMerge.subtractLayers(tempLayer1, tempLayer2, tempLayer1);
//	
//					// remove other cuts in this area
//					for(int o=0; o<cutList.size(); o++)
//					{
//						if (o == ind) continue;
//						PolyBase oPoly = cutList.get(o);
//						double x = oPoly.getCenterX();
//						double y = oPoly.getCenterY();
//						if (largest.contains(x, y))
//						{
//							checkCutSize(oPoly, pv.pNp, layer);
//							cutList.remove(oPoly);
//							merge.subtract(layer, oPoly);
//							if (o < ind) ind--;
//							o--;
//						}
//					}
//					cutList.remove(cutPoly);
//					ind--;
//					break;
//				}
//				desiredHeight--;
//				desiredWidth--;
//				if (desiredHeight < pv.minHeight || desiredWidth < pv.minWidth) break;
//			}
//		}
//		originalMerge.deleteLayer(tempLayer1);
//		originalMerge.deleteLayer(tempLayer2);
//	}

//	/**
//	 * Method to ensure that contact cuts are the right size.
//	 * Prints an error message if the cut is the wrong size.
//	 * @param poly a polygon describing a contact cut.
//	 * @param pNp the primitive node that this cut will be part of.
//	 * @param cutLayer the cut layer that this polygon will become.
//	 */
//	private void checkCutSize(PolyBase poly, PrimitiveNode pNp, Layer cutLayer)
//	{
//		// find the desired size of the cut layer
//		double xSize = -1, ySize = -1;
//		if (pNp.getSpecialType() == PrimitiveNode.MULTICUT)
//		{
//			double [] values = pNp.getSpecialValues();
//			xSize = values[0];
//			ySize = values[1];
//		} else
//		{
//			Technology.NodeLayer [] nodeLayers = pNp.getLayers();
//			for(int i=0; i<nodeLayers.length; i++)
//			{
//				Technology.NodeLayer nodeLayer = nodeLayers[i];
//				if (nodeLayer.getLayer() == cutLayer)
//				{
//					EdgeH leftEdge = nodeLayer.getLeftEdge();
//					EdgeH rightEdge = nodeLayer.getRightEdge();
//					EdgeV topEdge = nodeLayer.getTopEdge();
//					EdgeV bottomEdge = nodeLayer.getBottomEdge();
//					double lX = leftEdge.getMultiplier() * pNp.getDefWidth() + leftEdge.getAdder();
//					double hX = rightEdge.getMultiplier() * pNp.getDefWidth() + rightEdge.getAdder();
//					double lY = bottomEdge.getMultiplier() * pNp.getDefHeight() + bottomEdge.getAdder();
//					double hY = topEdge.getMultiplier() * pNp.getDefHeight() + topEdge.getAdder();
//					xSize = hX - lX;
//					ySize = hY - lY;
//				}
//			}
//		}
//
//		// see if the cut polygon is the right size
//		boolean valid = true;
//		Rectangle2D polyBox = poly.getBox();
//		String reason = "irregular";
//		if (polyBox != null)
//		{
//			double polyWid = polyBox.getWidth() / SCALEFACTOR;
//			double polyHei = polyBox.getHeight() / SCALEFACTOR;
//			if (xSize != polyWid || ySize != polyHei)
//			{
//				reason = polyWid + "x" + polyHei;
//				polyBox = null;
//			}
//		}
//		if (polyBox == null)
//		{
//			double cX = poly.getCenterX() / SCALEFACTOR;
//			double cY = poly.getCenterY() / SCALEFACTOR;
//			System.out.println("Warning: layer " + cutLayer.getName() + " at (" + cX + "," + cY + ") is the wrong size (is " +
//				reason + ", should be " + xSize + "x" + ySize + ")");
//		}
//	}

//	/**
//	 * Method to examine a polygon and find the largest rectangle at a point.
//	 * @param contactArea the polygon to examine.
//	 * @param centerX the X coordinate of the point that must be in the resulting rectangle.
//	 * @param centerY the Y coordinate of the point that must be in the resulting rectangle.
//	 * @param minWidth the minimum X size of the rectangle.
//	 * @param minHeight the minimum Y size of the rectangle.
//	 * @param maxWidth the maximum X size of the rectangle (0 if no maximum limit).
//	 * @param maxHeight the maximum Y size of the rectangle (0 if no maximum limit).
//	 * @return the largest rectangle at the point that meets the min/max size criteria.
//	 */
//	private Rectangle2D findLargestRectangle(List<PolyBase> contactArea, double centerX, double centerY,
//		double minWidth, double minHeight, double maxWidth, double maxHeight)
//	{
//		double closestTop = Double.MAX_VALUE;
//		double closestBottom = Double.MAX_VALUE;
//		double closestLeft = Double.MAX_VALUE;
//		double closestRight = Double.MAX_VALUE;
//		TreeSet<Double> xPoints = new TreeSet<Double>();
//		TreeSet<Double> yPoints = new TreeSet<Double>();
//		for(PolyBase poly : contactArea)
//		{
//			if (!poly.contains(centerX, centerY)) continue;
//
//			// this is the polygon that contains the point: gather bounds
//			Point2D [] points = poly.getPoints();
//			for(int i=0; i<points.length; i++)
//			{
//				int last = i - 1;
//				if (last < 0) last = points.length - 1;
//				Point2D lastPt = points[last];
//				Point2D thisPt = points[i];
//				if (lastPt.getX() == thisPt.getX())
//				{
//					// vertical line: check left and right bounds
//					double lowY = Math.min(thisPt.getY(), lastPt.getY());
//					double highY = Math.max(thisPt.getY(), lastPt.getY());
//					if (lowY >= centerY + minHeight/2) continue;
//					if (highY <= centerY - minHeight/2) continue;
//					double dist = lastPt.getX() - centerX;
//					if (Math.abs(dist) < minWidth/2) continue;
//					if (dist > 0)
//					{
//						// see if this is a new right edge
//						if (dist < closestRight)
//						{
//							closestRight = dist;
//							yPoints.add(new Double(lowY));
//							yPoints.add(new Double(highY));
//						}
//					} else
//					{
//						// see if this is a new left edge
//						if (-dist < closestLeft)
//						{
//							closestLeft = -dist;
//							yPoints.add(new Double(lowY));
//							yPoints.add(new Double(highY));
//						}
//					}
//				}
//				if (lastPt.getY() == thisPt.getY())
//				{
//					// horizontal line: check top and bottom bounds
//					double lowX = Math.min(thisPt.getX(), lastPt.getX());
//					double highX = Math.max(thisPt.getX(), lastPt.getX());
//					if (lowX >= centerX + minWidth/2) continue;
//					if (highX <= centerX - minWidth/2) continue;
//					double dist = lastPt.getY() - centerY;
//					if (Math.abs(dist) < minHeight/2) continue;
//					if (dist > 0)
//					{
//						// see if this is a new top edge
//						if (dist < closestTop)
//						{
//							closestTop = dist;
//							xPoints.add(new Double(lowX));
//							xPoints.add(new Double(highX));
//						}
//					} else
//					{
//						// see if this is a new bottom edge
//						if (-dist < closestBottom)
//						{
//							closestBottom = -dist;
//							xPoints.add(new Double(lowX));
//							xPoints.add(new Double(highX));
//						}
//					}
//				}
//			}
//			if (closestTop == Double.MAX_VALUE || closestBottom == Double.MAX_VALUE ||
//				closestLeft == Double.MAX_VALUE || closestRight == Double.MAX_VALUE) return null;
//			Rectangle2D largest = new Rectangle2D.Double(centerX-closestLeft, centerY-closestBottom,
//				closestLeft+closestRight, closestBottom+closestTop);
//			if (poly.contains(largest))
//			{
//				adjustLargest(largest, centerX, centerY, maxWidth, maxHeight);
//				return largest;
//			}
//
//			// try reducing the rectangle until it does fit
//			double left = centerX - closestLeft;
//			double right = centerX + closestRight;
//			double top = centerY + closestTop;
//			double bottom = centerY - closestBottom;
//			double [] xValues = new double[xPoints.size()];
//			int i=0;  for(Double d : xPoints) xValues[i++] = d.doubleValue();
//			double [] yValues = new double[yPoints.size()];
//			i=0;  for(Double d : yPoints) yValues[i++] = d.doubleValue();
//			int leftPoint = 0, rightPoint = xPoints.size() - 1;
//			int bottomPoint = 0, topPoint = yPoints.size() - 1;
//			while (leftPoint < xPoints.size() && xValues[leftPoint] <= left) leftPoint++;
//			while (rightPoint >= 0 && xValues[rightPoint] >= right) rightPoint--;
//			while (bottomPoint < yPoints.size() && yValues[bottomPoint] <= bottom) bottomPoint++;
//			while (topPoint >= 0 && yValues[topPoint] >= top) topPoint--;
//			for(;;)
//			{
//				// shrink on left
//				if (leftPoint < xPoints.size())
//				{
//					double newLeft = xValues[leftPoint];
//					if (newLeft > left && newLeft < centerX-minWidth/2)
//					{
//						left = newLeft;
//						largest = new Rectangle2D.Double(left, bottom, right-left, top-bottom);
//						if (poly.contains(largest))
//						{
//							adjustLargest(largest, centerX, centerY, maxWidth, maxHeight);
//							return largest;
//						}
//						leftPoint++;
//						continue;
//					}
//				}
//
//				// shrink on bottom
//				if (bottomPoint < yPoints.size())
//				{
//					double newBottom = yValues[bottomPoint];
//					if (newBottom > bottom && newBottom < centerY-minHeight/2)
//					{
//						bottom = newBottom;
//						largest = new Rectangle2D.Double(left, bottom, right-left, top-bottom);
//						if (poly.contains(largest))
//						{
//							adjustLargest(largest, centerX, centerY, maxWidth, maxHeight);
//							return largest;
//						}
//						bottomPoint++;
//						continue;
//					}
//				}
//
//				// shrink on right
//				if (rightPoint >= 0)
//				{
//					double newRight = xValues[rightPoint];
//					if (newRight < right && newRight > centerX+minWidth/2)
//					{
//						right = newRight;
//						largest = new Rectangle2D.Double(left, bottom, right-left, top-bottom);
//						if (poly.contains(largest))
//						{
//							adjustLargest(largest, centerX, centerY, maxWidth, maxHeight);
//							return largest;
//						}
//						rightPoint--;
//						continue;
//					}
//				}
//
//				// shrink on top
//				if (topPoint >= 0)
//				{
//					double newTop = yValues[topPoint];
//					if (newTop < top && newTop > centerY+minHeight/2)
//					{
//						top = newTop;
//						largest = new Rectangle2D.Double(left, bottom, right-left, top-bottom);
//						if (poly.contains(largest))
//						{
//							adjustLargest(largest, centerX, centerY, maxWidth, maxHeight);
//							return largest;
//						}
//						topPoint--;
//						continue;
//					}
//				}
//				break;
//			}
//		}
//		return null;
//	}

//	/**
//	 * Method to adjust the rectangle so that it is no larger than a given size
//	 * and centered about a point.
//	 * @param rect the rectangle to adjust.
//	 * @param cX the X center of the largest rectangle.
//	 * @param cY the Y center of the largest rectangle.
//	 * @param maxX the width of the largest rectangle (0 to ignore the operation).
//	 * @param maxY the height of the largest rectangle (0 to ignore the operation).
//	 */
//	private void adjustLargest(Rectangle2D rect, double cX, double cY, double maxX, double maxY)
//	{
//		if (maxX > 0 && maxY > 0)
//		{
//			double lX = rect.getMinX(), hX = rect.getMaxX();
//			double lY = rect.getMinY(), hY = rect.getMaxY();
//			double halfWid = Math.min(cX - lX, hX - cX);
//			if (halfWid > maxX/2) halfWid = maxX / 2;
//			double halfHei = Math.min(cY - lY, hY - cY);
//			if (halfHei > maxY/2) halfHei = maxY / 2;
//			rect.setRect(cX - halfWid, cY - halfHei, halfWid*2, halfHei*2);
//		}
//	}

	/********************************************** TRANSISTOR EXTRACTION **********************************************/

	private void extractTransistors(PolyMerge merge, PolyMerge originalMerge, Cell newCell)
	{
		// must have a poly layer
		if (polyLayer == null || tempLayer1 == null) return;

		// find the transistors to create
		PrimitiveNode pTransistor = null, nTransistor = null;
		for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode pNp = it.next();
			if (pTransistor == null && pNp.getFunction() == PrimitiveNode.Function.TRAPMOS) pTransistor = pNp;
			if (nTransistor == null && pNp.getFunction() == PrimitiveNode.Function.TRANMOS) nTransistor = pNp;
		}
		if (nTransistor != null)
		{
			findTransistors(nTransistor, merge, originalMerge, newCell);
		}
		if (pTransistor != null)
		{
			findTransistors(pTransistor, merge, originalMerge, newCell);
		}
	}

	private void findTransistors(PrimitiveNode transistor, PolyMerge merge, PolyMerge originalMerge, Cell newCell)
	{
		originalMerge.intersectLayers(polyLayer, activeLayer, tempLayer1);
		Technology.NodeLayer [] layers = transistor.getLayers();
		for(int i=0; i<layers.length; i++)
		{
			Technology.NodeLayer lay = layers[i];
			Layer layer = geometricLayer(lay.getLayer());
			if (layer == polyLayer || layer == activeLayer) continue;

			// ignore well layers if the process doesn't have them
			Layer.Function fun = layer.getFunction();
			if (fun == Layer.Function.WELLP && pWellProcess) continue;
			if (fun == Layer.Function.WELLN && nWellProcess) continue;

			originalMerge.intersectLayers(layer, tempLayer1, tempLayer1);
		}

		// look at all of the pieces of this layer
		List<PolyBase> polyList = getMergePolys(originalMerge, tempLayer1);
		if (polyList == null) return;
		for(PolyBase poly : polyList)
		{
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
				double width = wid + scaleUp(so.getLowXOffset() + so.getHighXOffset());
				double height = hei + scaleUp(so.getLowYOffset() + so.getHighYOffset());
				realizeNode(transistor, poly.getCenterX(), poly.getCenterY(),
					width, height, angle, null, merge, newCell);
			} else
			{
				// complex polygon: extract angled or serpentine transistor
				extractNonManhattanTransistor(poly, transistor, merge, originalMerge, newCell);
			}
		}
		originalMerge.deleteLayer(tempLayer1);
	}

	/**
	 * Method to extract a transistor from a nonmanhattan polygon that defines the intersection of poly and active.
	 * @param poly the outline of poly/active to extract.
	 * @param transistor the type of transistor to create.
	 * @param merge the geometry collection to adjust when a transistor is extracted.
	 * @param originalMerge the original geometry collection (for examination).
	 * @param newCell the cell in which to create the extracted transistor
	 */
	private void extractNonManhattanTransistor(PolyBase poly, PrimitiveNode transistor, PolyMerge merge, PolyMerge originalMerge, Cell newCell)
	{
		// determine minimum width of polysilicon
		SizeOffset so = transistor.getProtoSizeOffset();
		double minWidth = transistor.getDefHeight() - so.getLowYOffset() - so.getHighYOffset();

		// reduce the geometry to a skeleton of centerlines
		List<Centerline> lines = findCenterlines(poly, tempLayer1, minWidth, merge, originalMerge);
		if (lines.size() == 0) return;

		// if just one line, it is simply an angled transistor
		if (lines.size() == 1)
		{
			Centerline cl = lines.get(0);
			double polySize = cl.start.distance(cl.end);
			double activeSize = cl.width;
			double cX = (cl.start.getX() + cl.end.getX()) / 2;
			double cY = (cl.start.getY() + cl.end.getY()) / 2;
			double sX = polySize + scaleUp(so.getLowXOffset() + so.getHighXOffset());
			double sY = activeSize + scaleUp(so.getLowYOffset() + so.getHighYOffset());
			realizeNode(transistor, cX, cY, sX, sY, cl.angle, null, merge, newCell);
			return;
		}

		// serpentine transistor: organize the lines into an array of points
		EPoint [] points = new EPoint[lines.size()+1];
		for(Centerline cl : lines)
		{
			cl.handled = false;
		}
		Centerline firstCL = lines.get(0);
		firstCL.handled = true;
		points[0] = new EPoint(firstCL.start.getX(), firstCL.start.getY());
		points[1] = new EPoint(firstCL.end.getX(), firstCL.end.getY());
		int pointsSeen = 2;
		while (pointsSeen < points.length)
		{
			boolean added = false;
			for(Centerline cl : lines)
			{
				if (cl.handled) continue;
				EPoint start = new EPoint(cl.start.getX(), cl.start.getY());
				EPoint end = new EPoint(cl.end.getX(), cl.end.getY());
				if (start.equals(points[0]))
				{
					// insert "end" point at start
					for(int i=pointsSeen; i>0; i--)
						points[i] = points[i-1];
					points[0] = end;
					pointsSeen++;
					cl.handled = true;
					added = true;
					break;
				}
				if (end.equals(points[0]))
				{
					// insert "start" point at start
					for(int i=pointsSeen; i>0; i--)
						points[i] = points[i-1];
					points[0] = start;
					pointsSeen++;
					cl.handled = true;
					added = true;
					break;
				}
				if (start.equals(points[pointsSeen-1]))
				{
					// add "end" at the end
					points[pointsSeen++] = end;
					cl.handled = true;
					added = true;
					break;
				}
				if (end.equals(points[pointsSeen-1]))
				{
					// add "start" at the end
					points[pointsSeen++] = start;
					cl.handled = true;
					added = true;
					break;
				}
			}
			if (!added) break;
		}

		// make sure all points are handled
		if (pointsSeen != points.length) return;

		// compute information about the transistor and create it
		double lX = points[0].getX(), hX = points[0].getX();
		double lY = points[0].getY(), hY = points[0].getY();
		for(int i=1; i<points.length; i++)
		{
			if (points[i].getX() < lX) lX = points[i].getX();
			if (points[i].getX() > hX) hX = points[i].getX();
			if (points[i].getY() < lY) lY = points[i].getY();
			if (points[i].getY() > hY) hY = points[i].getY();
		}
		double cX = (lX + hX) / 2;
		double cY = (lY + hY) / 2;
		for(int i=0; i<points.length; i++)
			points[i] = new EPoint((points[i].getX() - cX) / SCALEFACTOR, (points[i].getY() - cY) / SCALEFACTOR);
		realizeNode(transistor, cX, cY, hX - lX, hY - lY, 0, points, merge, newCell);
	}

	/********************************************** CONVERT CONNECTING GEOMETRY **********************************************/

	/**
	 * Method to look for opportunities to place arcs that connect to existing geometry.
	 */
	private void extendGeometry(PolyMerge merge, PolyMerge originalMerge, Cell newCell, boolean justExtend)
	{
        for (Layer layer : merge.getKeySet())
		{
			ArcProto ap = arcsForLayer.get(layer);
			if (ap == null) continue;
			List<PolyBase> polyList = getMergePolys(merge, layer);
			for(PolyBase poly : polyList)
			{
				// find out what this polygon touches
				HashMap<Network,Object> netsThatTouch = getNetsThatTouch(poly, newCell);

				// make a list of port/arc ends that touch this polygon
				List<Object> objectsToConnect = new ArrayList<Object>();
				for(Network net : netsThatTouch.keySet())
				{
					Object entry = netsThatTouch.get(net);
					if (entry != null) objectsToConnect.add(entry);
				}

				// if only 1 object touches the polygon, see if it can be "wired" to cover
				if (objectsToConnect.size() == 1)
				{
					// touches just 1 object: see if that object can be extended to cover the polygon
					extendObject((ElectricObject)objectsToConnect.get(0), poly, layer, ap, merge, originalMerge, newCell);
					continue;
				}

				// if two objects touch the polygon, see if an arc can connect them
				if (!justExtend && objectsToConnect.size() == 2)
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

						MutableBoolean headExtend = new MutableBoolean(true), tailExtend = new MutableBoolean(true);
						double wid = ap.getDefaultLambdaFullWidth();
						originalMerge.arcPolyFits(layer, pt1, pt2, wid, headExtend, tailExtend);
						ArcInst ai = realizeArc(ap, pi1, pi2, pt1, pt2, wid, !headExtend.booleanValue(), !tailExtend.booleanValue(), merge);
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

						MutableBoolean headExtend = new MutableBoolean(true), tailExtend = new MutableBoolean(true);
						double wid = ap.getDefaultLambdaFullWidth();
						originalMerge.arcPolyFits(layer, pt1, pt2, wid, headExtend, tailExtend);
						ArcInst ai = realizeArc(ap, pi1, pi2, pt1, pt2, wid, !headExtend.booleanValue(), !tailExtend.booleanValue(), merge);
						continue;
					}

					// see if a bend can be made through the polygon
					Point2D pt1 = new Point2D.Double(polyBounds1.getCenterX(), polyBounds1.getCenterY());
					Point2D pt2 = new Point2D.Double(polyBounds2.getCenterX(), polyBounds2.getCenterY());
					Point2D corner1 = new Point2D.Double(polyBounds1.getCenterX(), polyBounds2.getCenterY());
					Point2D corner2 = new Point2D.Double(polyBounds2.getCenterX(), polyBounds1.getCenterY());
					Point2D containsIt = null;
					if (poly.contains(corner1)) containsIt = corner1; else
						if (poly.contains(corner2)) containsIt = corner2;
					if (containsIt != null)
					{
						PrimitiveNode np = ap.findPinProto();
						NodeInst ni = createNode(np, containsIt, np.getDefWidth(), np.getDefHeight(), newCell);
						PortInst pi = ni.getOnlyPortInst();
						double wid = ap.getDefaultLambdaFullWidth();

						MutableBoolean headExtend = new MutableBoolean(true), tailExtend = new MutableBoolean(true);
						originalMerge.arcPolyFits(layer, pt1, containsIt, wid, headExtend, tailExtend);
						ArcInst ai1 = realizeArc(ap, pi1, pi, pt1, containsIt, wid, !headExtend.booleanValue(), !tailExtend.booleanValue(), merge);

						headExtend.setValue(true);   tailExtend.setValue(true);
						originalMerge.arcPolyFits(layer, pt2, containsIt, wid, headExtend, tailExtend);
						ArcInst ai2 = realizeArc(ap, pi2, pi, pt2, containsIt, wid, !headExtend.booleanValue(), !tailExtend.booleanValue(), merge);
					}
				}
			}
		}
	}

	/**
	 * Method to see if a polygon can be covered by adding an arc to an Arc or Port.
	 * @param obj the object that is being extended (either an ArcInst or a PortInst).
	 * @param poly the polygon that is being covered.
	 * @param layer the layer of the polygon.
	 * @param ap the ArcProto to use when covering the polygon.
	 * @param merge the merge area being replaced.
	 * @param originalMerge the original merge area being covered.
	 * @param newCell the Cell in which to place the new arc.
	 */
	private void extendObject(ElectricObject obj, PolyBase poly, Layer layer, ArcProto ap, PolyMerge merge, PolyMerge originalMerge, Cell newCell)
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

		// find the port that is being extended
		Point2D polyCtr = new Point2D.Double(polyBounds.getCenterX(), polyBounds.getCenterY());
		if (obj instanceof ArcInst)
		{
			ArcInst ai = (ArcInst)obj;
			double headDist = polyCtr.distance(ai.getHeadLocation());
			double tailDist = polyCtr.distance(ai.getTailLocation());
			if (headDist < tailDist) obj = ai.getHeadPortInst(); else
				obj = ai.getTailPortInst();
		}
		PortInst pi = (PortInst)obj;

		// prepare to cover the polygon
		Poly portPoly = pi.getPoly();
		Rectangle2D portRect = portPoly.getBounds2D();
		portRect.setRect(scaleUp(portRect.getMinX()), scaleUp(portRect.getMinY()),
			scaleUp(portRect.getWidth()), scaleUp(portRect.getHeight()));
		PrimitiveNode np = ap.findPinProto();

		// is the port inside of the area to be covered?
		if (polyCtr.getY() >= portRect.getMinY() && polyCtr.getY() <= portRect.getMaxY() &&
			polyCtr.getX() >= portRect.getMinX() && polyCtr.getX() <= portRect.getMaxX())
		{
			// decide whether to extend horizontally or vertically
			if (polyBounds.getWidth() > polyBounds.getHeight())
			{
				// wider area, make horizontal arcs
				double endExtension = polyBounds.getHeight() / 2;
				Point2D pinPt1 = new Point2D.Double((polyBounds.getMaxX() - endExtension) / SCALEFACTOR,
					polyCtr.getY() / SCALEFACTOR);
				Point2D pinPt2 = new Point2D.Double((polyBounds.getMinX() + endExtension) / SCALEFACTOR,
					polyCtr.getY() / SCALEFACTOR);
				Point2D objPt = new Point2D.Double(portRect.getCenterX() / SCALEFACTOR, polyCtr.getY() / SCALEFACTOR);
				NodeInst ni1 = createNode(np, pinPt1, polyBounds.getHeight() / SCALEFACTOR,
					polyBounds.getHeight() / SCALEFACTOR, newCell);
				NodeInst ni2 = createNode(np, pinPt2, polyBounds.getHeight() / SCALEFACTOR,
					polyBounds.getHeight() / SCALEFACTOR, newCell);
				realizeArc(ap, ni1.getOnlyPortInst(), pi, pinPt1, objPt,
					(polyBounds.getHeight()+ap.getLambdaWidthOffset()) / SCALEFACTOR, false, false, merge);
				realizeArc(ap, ni2.getOnlyPortInst(), pi, pinPt2, objPt,
					(polyBounds.getHeight()+ap.getLambdaWidthOffset()) / SCALEFACTOR, false, false, merge);
			} else
			{
				// taller area, make vertical arcs
				double endExtension = polyBounds.getWidth() / 2;
				Point2D pinPt1 = new Point2D.Double(polyCtr.getX() / SCALEFACTOR,
					(polyBounds.getMaxY() - endExtension) / SCALEFACTOR);
				Point2D pinPt2 = new Point2D.Double(polyCtr.getX() / SCALEFACTOR,
					(polyBounds.getMinY() + endExtension) / SCALEFACTOR);
				Point2D objPt = new Point2D.Double(polyCtr.getX() / SCALEFACTOR, portRect.getCenterY() / SCALEFACTOR);
				NodeInst ni1 = createNode(np, pinPt1, polyBounds.getWidth() / SCALEFACTOR,
					polyBounds.getWidth() / SCALEFACTOR, newCell);
				NodeInst ni2 = createNode(np, pinPt2, polyBounds.getWidth() / SCALEFACTOR,
					polyBounds.getWidth() / SCALEFACTOR, newCell);
				realizeArc(ap, ni1.getOnlyPortInst(), pi, pinPt1, objPt,
					(polyBounds.getWidth()+ap.getLambdaWidthOffset()) / SCALEFACTOR, false, false, merge);
				realizeArc(ap, ni2.getOnlyPortInst(), pi, pinPt2, objPt,
					(polyBounds.getWidth()+ap.getLambdaWidthOffset()) / SCALEFACTOR, false, false, merge);
			}
			return;
		}

		// can we extend vertically
		if (polyCtr.getX() >= portRect.getMinX() && polyCtr.getX() <= portRect.getMaxX())
		{
			// going up to the poly or down?
			Point2D objPt = new Point2D.Double(polyCtr.getX() / SCALEFACTOR, portRect.getCenterY() / SCALEFACTOR);
			Point2D pinPt = null;
			boolean endExtend = true;
			double endExtension = polyBounds.getWidth() / 2;
			if (polyBounds.getHeight() < polyBounds.getWidth())
			{
				// arc is so short that it will stick out with end extension
				endExtend = false;
				endExtension = 0;
			}
			if (polyCtr.getY() > portRect.getCenterY())
			{
				// going up to the poly
				pinPt = new Point2D.Double(polyCtr.getX() / SCALEFACTOR,
					(polyBounds.getMaxY() - endExtension) / SCALEFACTOR);
			} else
			{
				// going down to the poly
				pinPt = new Point2D.Double(polyCtr.getX() / SCALEFACTOR,
					(polyBounds.getMinY() + endExtension) / SCALEFACTOR);
			}

			NodeInst ni1 = createNode(np, pinPt, polyBounds.getWidth() / SCALEFACTOR,
				polyBounds.getWidth() / SCALEFACTOR, newCell);

			MutableBoolean headExtend = new MutableBoolean(endExtend), tailExtend = new MutableBoolean(endExtend);
			double wid = polyBounds.getWidth() + ap.getLambdaWidthOffset();
			originalMerge.arcPolyFits(layer, pinPt, objPt, wid, headExtend, tailExtend);
			ArcInst ai = realizeArc(ap, ni1.getOnlyPortInst(), pi, pinPt, objPt, wid / SCALEFACTOR,
				!headExtend.booleanValue(), !tailExtend.booleanValue(), merge);
			return;
		}

		// can we extend horizontally
		if (polyCtr.getY() >= portRect.getMinY() && polyCtr.getY() <= portRect.getMaxY())
		{
			// going left to the poly or right?
			Point2D objPt = new Point2D.Double(portRect.getCenterX() / SCALEFACTOR, polyCtr.getY() / SCALEFACTOR);
			Point2D pinPt = null;
			boolean endExtend = true;
			double endExtension = polyBounds.getHeight() / 2;
			if (polyBounds.getWidth() < polyBounds.getHeight())
			{
				// arc is so short that it will stick out with end extension
				endExtend = false;
				endExtension = 0;
			}
			if (polyCtr.getX() > portRect.getCenterX())
			{
				// going right to the poly
				pinPt = new Point2D.Double((polyBounds.getMaxX() - endExtension) / SCALEFACTOR,
					polyCtr.getY() / SCALEFACTOR);
			} else
			{
				// going left to the poly
				pinPt = new Point2D.Double((polyBounds.getMinX() + endExtension) / SCALEFACTOR,
					polyCtr.getY() / SCALEFACTOR);
			}
			NodeInst ni1 = createNode(np, pinPt, polyBounds.getHeight() / SCALEFACTOR,
				polyBounds.getHeight() / SCALEFACTOR, newCell);
			MutableBoolean headExtend = new MutableBoolean(endExtend), tailExtend = new MutableBoolean(endExtend);
			double wid = polyBounds.getHeight() + ap.getLambdaWidthOffset();
			originalMerge.arcPolyFits(layer, pinPt, objPt, wid, headExtend, tailExtend);
			ArcInst ai = realizeArc(ap, ni1.getOnlyPortInst(), pi, pinPt, objPt,
				wid / SCALEFACTOR, !headExtend.booleanValue(), !tailExtend.booleanValue(), merge);
		}
	}

	private PortInst findArcEnd(ArcInst ai, PolyBase poly)
	{
		// see if one end of the arc touches the poly
		Point2D head = ai.getHeadLocation();
		Point2D tail = ai.getTailLocation();
		int ang = GenMath.figureAngle(tail, head);
		int angPlus = (ang + 900) % 3600;
		int angMinus = (ang + 2700) % 3600;
		double width = ai.getLambdaBaseWidth() / 2;

		// see if the head end touches
		Point2D headButFarther = new Point2D.Double(head.getX() + width * GenMath.cos(ang), head.getY() + width * GenMath.sin(ang));
		if (poly.contains(headButFarther)) return ai.getHeadPortInst();
		Point2D headOneSide = new Point2D.Double(head.getX() + width * GenMath.cos(angPlus), head.getY() + width * GenMath.sin(angPlus));
		if (poly.contains(headOneSide)) return ai.getHeadPortInst();
		Point2D headOtherSide = new Point2D.Double(head.getX() + width * GenMath.cos(angPlus), head.getY() + width * GenMath.sin(angPlus));
		if (poly.contains(headOtherSide)) return ai.getHeadPortInst();

		// see if the tail end touches
		Point2D tailButFarther = new Point2D.Double(tail.getX() - width * GenMath.cos(ang), tail.getY() - width * GenMath.sin(ang));
		if (poly.contains(tailButFarther)) return ai.getTailPortInst();
		Point2D tailOneSide = new Point2D.Double(tail.getX() - width * GenMath.cos(angPlus), tail.getY() - width * GenMath.sin(angPlus));
		if (poly.contains(tailOneSide)) return ai.getTailPortInst();
		Point2D tailOtherSide = new Point2D.Double(tail.getX() - width * GenMath.cos(angPlus), tail.getY() - width * GenMath.sin(angPlus));
		if (poly.contains(tailOtherSide)) return ai.getTailPortInst();
		
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

	private HashMap<Network,Object> getNetsThatTouch(PolyBase poly, Cell newCell)
	{
		// scale the polygon back
		Point2D [] points = poly.getPoints();
		Point2D [] newPoints = new Point2D[points.length];
		for(int i=0; i<points.length; i++)
			newPoints[i] = new Point2D.Double(points[i].getX() / SCALEFACTOR, points[i].getY() / SCALEFACTOR);
		PolyBase newPoly = new PolyBase(newPoints);
		Layer layer = poly.getLayer();

		// make a map of networks that touch the polygon, and the objects on them
		HashMap<Network,Object> netsThatTouch = new HashMap<Network,Object>();

		// find nodes that touch
		Rectangle2D bounds = newPoly.getBounds2D();
		Point2D centerPoint = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
		Netlist nl = newCell.acquireUserNetlist();
		for(Iterator<Geometric> it = newCell.searchIterator(bounds); it.hasNext(); )
		{
			Geometric geom = it.next();
			if (!(geom instanceof NodeInst)) continue;
			NodeInst ni = (NodeInst)geom;
			if (ni.isCellInstance()) continue;
			AffineTransform trans = ni.rotateOut();
			Technology tech = ni.getProto().getTechnology();
			Poly [] nodePolys = tech.getShapeOfNode(ni, true, true, null);
			for(int i=0; i<nodePolys.length; i++)
			{
				Poly nodePoly = nodePolys[i];
				if (geometricLayer(nodePoly.getLayer()) != layer) continue;
				nodePoly.transform(trans);
				if (polysTouch(nodePoly, newPoly))
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
		for(Iterator<Geometric> it = newCell.searchIterator(bounds); it.hasNext(); )
		{
			Geometric geom = it.next();
			if (!(geom instanceof ArcInst)) continue;
			ArcInst ai = (ArcInst)geom;
			Technology tech = ai.getProto().getTechnology();
			Poly [] polys = tech.getShapeOfArc(ai);
			for(int i=0; i<polys.length; i++)
			{
				Poly arcPoly = polys[i];
				if (geometricLayer(arcPoly.getLayer()) != layer) continue;
				if (polysTouch(arcPoly, newPoly))
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
	 * Method to find a list of centerlines that skeletonize a polygon.
	 * @param poly the Poly to skeletonize.
	 * @param layer the layer on which the polygon resides.
	 * @param minWidth the minimum width of geometry on the layer.
	 * @param merge 
	 * @param originalMerge
	 * @return a List of Centerline objects that describe a single "bone" of the skeleton.
	 */
	private List<Centerline> findCenterlines(PolyBase poly, Layer layer, double minWidth, PolyMerge merge, PolyMerge originalMerge)
	{
		// the list of centerlines
		List<Centerline> validCenterlines = new ArrayList<Centerline>();

		// make a layer that describes the polygon
		merge.deleteLayer(tempLayer1);
		merge.addPolygon(tempLayer1, poly);

		List<PolyBase> polysToAnalyze = new ArrayList<PolyBase>();
		polysToAnalyze.add(poly);
		for(int loop=1; ; loop++)
		{
			// decompose all polygons
			boolean foundNew = false;
			for(PolyBase aPoly : polysToAnalyze)
			{
				// first make a list of all parallel wires in the polygon
				aPoly.setLayer(layer);
//System.out.print("FOR POLY:");
//Point2D [] points = aPoly.getPoints();
//for(int i=0; i<points.length; i++) System.out.print(" ("+points[i].getX()+","+points[i].getY()+")");
//System.out.println();
				List<Centerline> centerlines = gatherCenterlines(aPoly, merge, originalMerge);

//System.out.println("GATHERED:");
//for(Centerline cl : centerlines)
//	System.out.println("    "+cl.toString());
				// now pull out the relevant ones
				double lastWidth = -1;
				for(Centerline cl : centerlines)
				{
					if (cl.width < minWidth) continue;

					// make the polygon to describe the centerline
					double length = cl.start.distance(cl.end);
					if (length < DBMath.getEpsilon()) continue;
					Poly clPoly = Poly.makeEndPointPoly(length, cl.width, cl.angle,
						cl.start, 0, cl.end, 0, Poly.Type.FILLED);

					// see if this centerline actually covers new area
					if (!merge.intersects(tempLayer1, clPoly)) continue;

					// for nonmanhattan centerlines, do extra work to ensure uniqueness
					if (cl.startUnscaled.getX() != cl.endUnscaled.getX() &&
						cl.startUnscaled.getY() != cl.endUnscaled.getY())
					{
						boolean duplicate = false;
						for(Centerline oCl : validCenterlines)
						{
							if (cl.startUnscaled.equals(oCl.startUnscaled) && cl.endUnscaled.equals(oCl.endUnscaled))
							{
								duplicate = true;   break;
							}
							if (cl.startUnscaled.equals(oCl.endUnscaled) && cl.endUnscaled.equals(oCl.startUnscaled))
							{
								duplicate = true;   break;
							}
						}
						if (duplicate) continue;
					}

					// if narrower centerlines have already been added, stop now
					if (lastWidth < 0) lastWidth = cl.width;
					if (Math.abs(cl.width - lastWidth) > 1) break;

					// add this to the list of valid centerlines
					validCenterlines.add(cl);
					merge.subtract(tempLayer1, clPoly);
					foundNew = true;
				}
			}
			if (!foundNew) break;

			// now analyze the remaining geometry in the polygon
			polysToAnalyze = getMergePolys(merge, tempLayer1);
			if (polysToAnalyze == null) break;
		}
		merge.deleteLayer(tempLayer1);

//System.out.println("Centerlines are:");
//for(int i=0; i<validCenterlines.size(); i++) System.out.println("    "+validCenterlines.get(i).toString());

		// now extend centerlines so they meet
		Centerline [] both = new Centerline[2];
		for(int i=0; i<validCenterlines.size(); i++)
		{
			Centerline cl = validCenterlines.get(i);
			double minCLX = Math.min(cl.start.getX(), cl.end.getX());
			double maxCLX = Math.max(cl.start.getX(), cl.end.getX());
			double minCLY = Math.min(cl.start.getY(), cl.end.getY());
			double maxCLY = Math.max(cl.start.getY(), cl.end.getY());
			for(int j=i+1; j<validCenterlines.size(); j++)
			{
				Centerline oCl = validCenterlines.get(j);
				double minOCLX = Math.min(oCl.start.getX(), oCl.end.getX());
				double maxOCLX = Math.max(oCl.start.getX(), oCl.end.getX());
				double minOCLY = Math.min(oCl.start.getY(), oCl.end.getY());
				double maxOCLY = Math.max(oCl.start.getY(), oCl.end.getY());
				if (minOCLX > maxCLX || maxOCLX < minCLX || minOCLY > maxCLY || maxOCLY < minCLY) continue;

//System.out.println("COMPARE "+cl.toString()+" WITH "+oCl.toString());
				Point2D intersect = GenMath.intersect(cl.start, cl.angle, oCl.start, oCl.angle);
				if (intersect == null) continue;
//				if (cl.start.distance(intersect) <= oCl.width/2 || cl.end.distance(intersect) <= oCl.width/2 ||
//					oCl.start.distance(intersect) <= cl.width/2 || oCl.end.distance(intersect) <= cl.width/2)
				{
//System.out.println("  INTERSECTION AT ("+intersect.getX()+","+intersect.getY()+")");
					both[0] = cl;   both[1] = oCl;
					for(int b=0; b<2; b++)
					{
						Point2D newStart = both[b].start, newEnd = both[b].end;
						double distToStart = newStart.distance(intersect);
						double distToEnd = newEnd.distance(intersect);

						// see if intersection is deeply inside of the segment
						boolean makeT = insideSegment(newStart, newEnd, intersect) &&
							Math.min(distToStart, distToEnd) > both[b].width/2;

						// adjust the centerline to end at the intersection point
						double extendStart = 0, extendEnd = 0, extendAltStart = 0, extendAltEnd = 0, betterExtension = 0;
						Point2D altNewStart = new Point2D.Double(0,0), altNewEnd = new Point2D.Double(0,0);
						if (distToStart < distToEnd)
						{
							betterExtension = newStart.distance(intersect);
							altNewStart.setLocation(newStart);   altNewEnd.setLocation(intersect);
							newStart = intersect;
							extendAltEnd = extendStart = both[b].width / 2;							
						} else
						{
							betterExtension = newEnd.distance(intersect);
							altNewStart.setLocation(intersect);   altNewEnd.setLocation(newEnd);
							newEnd = intersect;
							extendAltStart = extendEnd = both[b].width / 2;
						}
						Poly extended = Poly.makeEndPointPoly(newStart.distance(newEnd), both[b].width, both[b].angle,
							newStart, extendStart, newEnd, extendEnd, Poly.Type.FILLED);
						if (!originalMerge.contains(layer, extended))
						{
							if (extendStart > 0) extendStart = betterExtension;
							if (extendEnd > 0) extendEnd = betterExtension;
							extended = Poly.makeEndPointPoly(newStart.distance(newEnd), both[b].width, both[b].angle,
								newStart, extendStart, newEnd, extendEnd, Poly.Type.FILLED);
						}
						if (originalMerge.contains(layer, extended))
						{
							both[b].setStart(newStart.getX(), newStart.getY());
							both[b].setEnd(newEnd.getX(), newEnd.getY());
							if (extendStart != 0) both[b].startHub = true;
							if (extendEnd != 0) both[b].endHub = true;
							if (makeT)
							{
								// too much shrinkage: split the centerline
								Centerline newCL = new Centerline(both[b].width, altNewStart, altNewEnd);
								if (extendAltStart != 0) newCL.startHub = true;
								if (extendAltEnd != 0) newCL.endHub = true;
								validCenterlines.add(newCL);
								continue;
							}
						}
					}
				}					
			}
		}
//System.out.println("FINAL CENTERLINE: ");
//for(Centerline cl : validCenterlines) System.out.println("    "+cl.toString());
		return validCenterlines;
	}

	/**
	 * Method to tell whether a point is inside of a line segment.
	 * It is assumed that the point is on the line defined by the segment.
	 * @param start one point on the segment.
	 * @param end another point on the segment.
	 * @param pt the point in question.
	 * @return true if the point in question is inside of the line segment.
	 */
	private boolean insideSegment(Point2D start, Point2D end, Point2D pt)
	{
        if (pt.getX() < Math.min(start.getX(),end.getX()) || pt.getX() > Math.max(start.getX(),end.getX()) ||
        	pt.getY() < Math.min(start.getY(),end.getY()) || pt.getY() > Math.max(start.getY(),end.getY()))
        		return false;
        return true;
	}

	/**
	 * Method to gather all of the Centerlines in a polygon.
	 * @param poly the Poly to analyze.
	 * @param merge the merge at this point (with extracted geometry removed).
	 * @param originalMerge the original collection of geometry.
	 * @return a List of Centerlines in the polygon.
	 */
	private List<Centerline> gatherCenterlines(PolyBase poly, PolyMerge merge, PolyMerge originalMerge)
	{
		// first make a list of all parallel wires in the polygon
		List<Centerline> centerlines = new ArrayList<Centerline>();
		Point2D [] points = poly.getPoints();
		if (DEBUGCENTERLINES)
		{
			System.out.print("POLYGON ON LAYER "+poly.getLayer().getName()+":");
			for(int i=0; i<points.length; i++) System.out.print(" ("+points[i].getX()+","+points[i].getY()+")");
			System.out.println();
		}
		for(int i=0; i<points.length; i++)
		{
			int lastI = i-1;
			if (lastI < 0) lastI = points.length-1;
			Point2D lastPt = points[lastI];
			Point2D thisPt = points[i];
			if (lastPt.equals(thisPt)) continue;
			double angle = GenMath.figureAngleRadians(thisPt, lastPt);
			while (angle < -Math.PI/2) angle += Math.PI;
			while (angle > Math.PI/2) angle -= Math.PI;

			// now find another line parallel to this one
			for(int j=i+2; j<points.length; j++)
			{
				Point2D oLastPt = points[j-1];
				Point2D oThisPt = points[j];
				if (oLastPt.equals(oThisPt)) continue;
				double oAngle = GenMath.figureAngleRadians(oThisPt, oLastPt);
				while (oAngle < -Math.PI/2) oAngle += Math.PI;
				while (oAngle > Math.PI/2) oAngle -= Math.PI;
				if (Math.abs(oAngle - angle) < 0.01)
				{
					// parallel lines: find the center line
					double perpAngle = angle + Math.PI/2;
					Point2D oneSide = thisPt;
					Point2D otherSide = intersectRadians(thisPt, perpAngle, oThisPt, oAngle);
					Point2D centerPt = new Point2D.Double((oneSide.getX()+otherSide.getX()) / 2, (oneSide.getY()+otherSide.getY()) / 2);
					double width = oneSide.distance(otherSide);

					// now determine range along that centerline
					Point2D lastPtCL = intersectRadians(lastPt, perpAngle, centerPt, angle);
					Point2D thisPtCL = intersectRadians(thisPt, perpAngle, centerPt, angle);
					Point2D oLastPtCL = intersectRadians(oLastPt, perpAngle, centerPt, angle);
					Point2D oThisPtCL = intersectRadians(oThisPt, perpAngle, centerPt, angle);

					// find the bounding box of the range lines
					double minX = Math.min(Math.min(lastPtCL.getX(), thisPtCL.getX()), Math.min(oLastPtCL.getX(), oThisPtCL.getX()));
					double maxX = Math.max(Math.max(lastPtCL.getX(), thisPtCL.getX()), Math.max(oLastPtCL.getX(), oThisPtCL.getX()));
					double minY = Math.min(Math.min(lastPtCL.getY(), thisPtCL.getY()), Math.min(oLastPtCL.getY(), oThisPtCL.getY()));
					double maxY = Math.max(Math.max(lastPtCL.getY(), thisPtCL.getY()), Math.max(oLastPtCL.getY(), oThisPtCL.getY()));

					// determine an extreme point along the centerline
					Point2D [] corners = new Point2D[4];
					corners[0] = new Point2D.Double(minX, minY);
					corners[1] = new Point2D.Double(minX, maxY);
					corners[2] = new Point2D.Double(maxX, maxY);
					corners[3] = new Point2D.Double(maxX, minY);
					boolean lastCorner = false, thisCorner = false, oLastCorner = false, oThisCorner = false;
					Point2D aCorner = null;
					for(int k=0; k<4; k++)
					{
						if (lastPtCL.equals(corners[k])) { aCorner = lastPtCL;   lastCorner = true; }
						if (thisPtCL.equals(corners[k])) { aCorner = thisPtCL;   thisCorner = true; }
						if (oLastPtCL.equals(corners[k])) { aCorner = oLastPtCL;   oLastCorner = true; }
						if (oThisPtCL.equals(corners[k])) { aCorner = oThisPtCL;   oThisCorner = true; }
					}

					// determine distance from the extreme corner
					double lastDist = aCorner.distance(lastPtCL);
					double thisDist = aCorner.distance(thisPtCL);
					double oLastDist = aCorner.distance(oLastPtCL);
					double oThisDist = aCorner.distance(oThisPtCL);

					// make sure the ranges overlap
					if (Math.min(lastDist, thisDist) >= Math.max(oLastDist, oThisDist) ||
						Math.min(oLastDist, oThisDist) >= Math.max(lastDist, thisDist)) continue;
					if (DEBUGCENTERLINES)
						System.out.println("PARALLEL LINES ("+lastPt.getX()+","+lastPt.getY()+") to ("+thisPt.getX()+","+thisPt.getY()+")"+
							" and ("+oLastPt.getX()+","+oLastPt.getY()+") to ("+oThisPt.getX()+","+oThisPt.getY()+")");

					// find the overlap
					if (lastDist > thisDist)
					{
						double swap = lastDist;    lastDist = thisDist;   thisDist = swap;
						Point2D swapPt = lastPtCL; lastPtCL = thisPtCL;   thisPtCL = swapPt;
					}
					if (oLastDist > oThisDist)
					{
						double swap = oLastDist;    oLastDist = oThisDist;   oThisDist = swap;
						Point2D swapPt = oLastPtCL; oLastPtCL = oThisPtCL;   oThisPtCL = swapPt;
					}
					Point2D start, finish, oStart, oFinish;
					if (lastDist < oLastDist)
					{
						start = oLastPtCL;
						oStart = lastPtCL;
					} else
					{
						start = lastPtCL;
						oStart = oLastPtCL;
					}
					if (thisDist > oThisDist)
					{
						finish = oThisPtCL;
						oFinish = thisPtCL;
					} else
					{
						finish = thisPtCL;
						oFinish = oThisPtCL;
					}

					// make a list of the centerline extent possibilities
					Point2D [] possibleStart = new Point2D[4];
					Point2D [] possibleEnd = new Point2D[4];
					possibleStart[0] = oStart;   possibleEnd[0] = oFinish;
					if (start.distance(oStart) < finish.distance(oFinish))
					{
						possibleStart[1] = oStart;   possibleEnd[1] = finish;
						possibleStart[2] = start;   possibleEnd[2] = oFinish;
					} else
					{
						possibleStart[1] = start;   possibleEnd[1] = oFinish;
						possibleStart[2] = oStart;   possibleEnd[2] = finish;
					}
					possibleStart[3] = start;   possibleEnd[3] = finish;

					// try all possible spans
					for(int p=0; p<4; p++)
					{
						double length = possibleStart[p].distance(possibleEnd[p]);
						Poly clPoly = makeEndPointPoly(length, width, angle,
							possibleStart[p], possibleEnd[p]);
//clPoly.roundPoints();
//Point2D [] ps = clPoly.getPoints();
//System.out.print("TRY (ANGLE="+angle+")");
//for(int t=0; t<ps.length; t++) System.out.print(" ("+ps[t].getX()+","+ps[t].getY()+")"); System.out.println();
						if (originalMerge.contains(poly.getLayer(), clPoly))
						{
							// if the width is much greater than the length, rotate the centerline 90 degrees
							if (width > length*2)
							{
								Point2D [] pts = clPoly.getPoints();
								Point2D [] edgeCtrs = new Point2D[pts.length];
								double bestDist = Double.MAX_VALUE;
								int bestPt = -1;
								for(int e=0; e<pts.length; e++)
								{
									Point2D last;
									if (e == 0) last = pts[pts.length-1]; else last = pts[e-1];
									edgeCtrs[e] = new Point2D.Double((pts[e].getX() + last.getX()) / 2,
										(pts[e].getY() + last.getY()) / 2);
									double dist = edgeCtrs[e].distance(possibleStart[p]);
									if (dist < bestDist) { bestDist = dist;   bestPt = e; }
								}
								width = length;
								int startPt = (bestPt + 1) % pts.length;
								int endPt = (bestPt + 3) % pts.length;
								possibleStart[p] = edgeCtrs[startPt];
								possibleEnd[p] = edgeCtrs[endPt];
								length = edgeCtrs[startPt].distance(edgeCtrs[endPt]);
							}

							// create the centerline
							Centerline newCL = new Centerline(width, possibleStart[p], possibleEnd[p]);
							if (newCL.angle >= 0) centerlines.add(newCL);
							if (DEBUGCENTERLINES) System.out.println("  MAKE "+newCL.toString());
							break;
						}
					}
				}
			}
		}

		// sort the parallel wires by length
		Collections.sort(centerlines, new ParallelWiresByLength());
		if (DEBUGCENTERLINES)
		{
			for(Centerline cl : centerlines)
				System.out.println("SORTED BY LENGTH "+cl.toString());
		}

		// remove redundant centerlines
		PolyMerge reCheck = new PolyMerge();
		for(int i=0; i<centerlines.size(); i++)
		{
			Centerline cl = centerlines.get(i);
			Poly clPoly = Poly.makeEndPointPoly(cl.start.distance(cl.end), cl.width, cl.angle,
				cl.start, 0, cl.end, 0, Poly.Type.FILLED);
			if (reCheck.contains(tempLayer1, clPoly))
			{
				if (DEBUGCENTERLINES)
					System.out.println("***REMOVE "+cl.toString()+" WHICH IS REDUNDANT");
				centerlines.remove(i);
				i--;
				continue;
			}
			reCheck.addPolygon(tempLayer1, clPoly);
		}

//		// old way to remove redundancy
//		for(int i=0; i<centerlines.size(); )
//		{
//			int nextI = i+1;
//			Centerline cl = centerlines.get(i);
//			Poly clPoly = Poly.makeEndPointPoly(cl.start.distance(cl.end), cl.width, cl.angle,
//				cl.start, 0, cl.end, 0, Poly.Type.FILLED);
//
//			// see if others are contained in it
//			for(int j=0; j<centerlines.size(); j++)
//			{
//				Centerline oCl = centerlines.get(j);
//				if (oCl == cl) continue;
//				Poly oClPoly = Poly.makeEndPointPoly(oCl.start.distance(oCl.end), oCl.width, oCl.angle,
//					oCl.start, 0, oCl.end, 0, Poly.Type.FILLED);
//				Rectangle2D oClBox = oClPoly.getBox();
//				if (oClBox == null) continue;
//				if (clPoly.contains(oClBox))
//				{
//					if (DEBUGCENTERLINES)
//						System.out.println("***REMOVE "+oCl.toString()+" WHICH IS COVERED BY "+cl.toString());
//					centerlines.remove(j);
//					if (nextI > j) nextI--;
//					j--;
//				}
//			}
//			i = nextI;
//		}

		// sort the parallel wires by width
		Collections.sort(centerlines, new ParallelWiresByWidth());
		if (DEBUGCENTERLINES)
		{
			for(Centerline cl : centerlines)
				System.out.println("FINALLY HAVE "+cl.toString());
		}
		return centerlines;
	}

	private static Point2D intersectRadians(Point2D thisPt, double perpAngle, Point2D oThisPt, double oAngle)
	{
		Point2D res2 = GenMath.intersect(thisPt, (int)(perpAngle/Math.PI*1800), oThisPt, (int)(oAngle/Math.PI*1800));
//		Point2D res1 = GenMath.intersectRadians(thisPt, perpAngle, oThisPt, oAngle);
//		if (!res1.equals(res2))
//			System.out.println("RADIANS SEZ ("+res1.getX()+","+res1.getY()+") BUT RIGHT ANSWER IS ("+res2.getX()+","+res2.getY()+")");
		return res2;
	}

	public static Poly makeEndPointPoly(double len, double wid, double angle, Point2D endH, Point2D endT)
	{
		double w2 = wid / 2;
		double x1 = endH.getX();   double y1 = endH.getY();
		double x2 = endT.getX();   double y2 = endT.getY();
		double xextra, yextra, xe1, ye1, xe2, ye2;
		if (len == 0)
		{
			double sa = Math.sin(angle);
			double ca = Math.cos(angle);
			xe1 = x1;
			ye1 = y1;
			xe2 = x2;
			ye2 = y2;
			xextra = ca * w2;
			yextra = sa * w2;
		} else
		{
			xe1 = x1;
			ye1 = y1;
			xe2 = x2;
			ye2 = y2;
			xextra = w2 * (x2-x1) / len;
			yextra = w2 * (y2-y1) / len;
		}
		Point2D.Double [] points = new Point2D.Double[] {
			new Point2D.Double(yextra + xe1, ye1 - xextra),
			new Point2D.Double(xe1 - yextra, xextra + ye1),
			new Point2D.Double(xe2 - yextra, xextra + ye2),
			new Point2D.Double(yextra + xe2, ye2 - xextra)};
		Poly poly = new Poly(points);
		poly.setStyle(Poly.Type.FILLED);
		return poly;
	}


	/**
	 * Class to sort Centerline objects by their width (and within that, by their length).
	 */
	private static class ParallelWiresByWidth implements Comparator<Centerline>
	{
		public int compare(Centerline cl1, Centerline cl2)
		{
			if (cl1.width < cl2.width) return 1;
			if (cl1.width > cl2.width) return -1;
			double cll1 = cl1.start.distance(cl1.end);
			double cll2 = cl2.start.distance(cl2.end);
			if (cll1 > cll2) return -1;
			if (cll1 < cll2) return 1;
			return 0;
		}
	}

	/**
	 * Class to sort Centerline objects by their length (and within that, by their width).
	 */
	private static class ParallelWiresByLength implements Comparator<Centerline>
	{
		public int compare(Centerline cl1, Centerline cl2)
		{
			double cll1 = cl1.start.distance(cl1.end);
			double cll2 = cl2.start.distance(cl2.end);
			if (cll1 > cll2) return -1;
			if (cll1 < cll2) return 1;
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
			cl.setStart(tx, Math.min(lowY, oLow));
			cl.setEnd(tx, Math.max(highY, oHigh));
			return true;
		}
		if (fy == ty)
		{
			// horizontal line
			double oLow = Math.min(oFx, oTx);
			double oHigh = Math.max(oFx, oTx);
			if (oFy != fy || oTy != fy) return false;
			if (lowX > oHigh || highX < oLow) return false;
			cl.setStart(Math.min(lowX, oLow), ty);
			cl.setEnd(Math.max(highX, oHigh), ty);
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

		cl.setStart(newEndAX, newEndAY);
		cl.setEnd(newEndBX, newEndBY);
		return true;
	}

	/**
	 * Method to scan the geometric information and convert it all to pure layer nodes in the new cell.
	 * When all other extractions are done, this is done to preserve any geometry that is unaccounted-for.
	 */
	private void convertAllGeometry(PolyMerge merge, PolyMerge originalMerge, Cell newCell)
	{
		double smallestPoly = (SCALEFACTOR * SCALEFACTOR) / 10;
        for (Layer layer : merge.getKeySet())
		{
			ArcProto ap = arcsForLayer.get(layer);
			List<PolyBase> polyList = getMergePolys(merge, layer);
//System.out.println("COMPLETE LAYER "+layer.getName()+": "+describeLayer(merge, layer));
			for(PolyBase poly : polyList)
			{
				double area = poly.getArea();
				if (area < smallestPoly) continue;

				// special case: a rectangle on a routable layer: make it an arc
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
						double width = polyBounds.getWidth() / SCALEFACTOR;
						double height = polyBounds.getHeight() / SCALEFACTOR;
						double actualWidth = 0;
						if (ENFORCEMINIMUMSIZE) actualWidth = ap.getDefaultLambdaBaseWidth();
						if (width >= actualWidth && height >= actualWidth)
						{
							PrimitiveNode np = ap.findPinProto();
							if (width > height)
							{
								// make a horizontal arc
								Point2D end1 = new Point2D.Double((polyBounds.getMinX()+height/2) / SCALEFACTOR, polyBounds.getCenterY() / SCALEFACTOR);
								Point2D end2 = new Point2D.Double((polyBounds.getMaxX()-height/2) / SCALEFACTOR, polyBounds.getCenterY() / SCALEFACTOR);
								NodeInst ni1 = createNode(np, end1, height, height, newCell);
								NodeInst ni2 = createNode(np, end2, height, height, newCell);
								ArcInst ai = realizeArc(ap, ni1.getOnlyPortInst(), ni2.getOnlyPortInst(), end1, end2, height+ap.getLambdaWidthOffset(),
									false, false, merge);
							} else
							{
								// make a vertical arc
								Point2D end1 = new Point2D.Double(polyBounds.getCenterX() / SCALEFACTOR, (polyBounds.getMinY()+width/2) / SCALEFACTOR);
								Point2D end2 = new Point2D.Double(polyBounds.getCenterX() / SCALEFACTOR, (polyBounds.getMaxY()-width/2) / SCALEFACTOR);
								NodeInst ni1 = createNode(np, end1, width, width, newCell);
								NodeInst ni2 = createNode(np, end2, width, width, newCell);
								ArcInst ai = realizeArc(ap, ni1.getOnlyPortInst(), ni2.getOnlyPortInst(), end1, end2, width+ap.getLambdaWidthOffset(),
									false, false, merge);
							}
							continue;
						}
					}
				} else
				{
					if (layer.getFunction().isSubstrate())
					{
						// implant layers may be large rectangles: see if bounds of poly is valid
						Rectangle2D polyBounds = poly.getBounds2D();
						if (originalMerge.contains(layer, polyBounds))
						{
							// valid rectangle: cover it all with a large pure layer rectangle
							PrimitiveNode pNp = layer.getPureLayerNode();
							double centerX = poly.getCenterX() / SCALEFACTOR;
							double centerY = poly.getCenterY() / SCALEFACTOR;
							Point2D center = new Point2D.Double(centerX, centerY);
							NodeInst ni = createNode(pNp, center,
								polyBounds.getWidth() / SCALEFACTOR, polyBounds.getHeight() / SCALEFACTOR, newCell);
							continue;
						}
					}
				}

				// just generate more pure-layer nodes
				double centerX = poly.getCenterX() / SCALEFACTOR;
				double centerY = poly.getCenterY() / SCALEFACTOR;
				Point2D center = new Point2D.Double(centerX, centerY);
				PrimitiveNode pNp = layer.getPureLayerNode();
				if (pNp == null)
				{
					System.out.println("CANNOT FIND PURE LAYER NODE FOR LAYER "+poly.getLayer().getName());
					continue;
				}
				NodeInst ni = createNode(pNp, center,
					poly.getBounds2D().getWidth() / SCALEFACTOR, poly.getBounds2D().getHeight() / SCALEFACTOR, newCell);

				// add on trace information if the shape is nonmanhattan
				if (poly.getBox() == null)
				{
					// store the trace
					Point2D [] points = poly.getPoints();
					EPoint [] newPoints = new EPoint[points.length];
					for(int i=0; i<points.length; i++)
					{
						newPoints[i] = new EPoint(points[i].getX() / SCALEFACTOR - centerX, points[i].getY() / SCALEFACTOR - centerY);
					}
					ni.newVar(NodeInst.TRACE, newPoints);
				}
			}
		}
	}

	private NodeInst createNode(NodeProto np, Point2D loc, double wid, double hei, Cell cell)
	{
		// pins cannot be smaller than their default size
		if (np.getFunction() == PrimitiveNode.Function.PIN)
		{
			if (wid < np.getDefWidth()) wid = np.getDefWidth();
			if (hei < np.getDefHeight()) hei = np.getDefHeight();
		}
		NodeInst ni = NodeInst.makeInstance(np, loc, wid, hei, cell);
		return ni;
	}

	/**
	 * Method to create a node and remove its geometry from the database.
	 * @param pNp the node to create.
	 * @param centerX the new node's X location.
	 * @param centerY the new node's Y location.
	 * @param width the new node's width.
	 * @param height the new node's height.
	 * @param angle the rotation of the new node.
	 * @param points an array of Point2D objects that define the new node's outline (null if there is no outline).
	 * @param merge the geometry collection.  The new node will be removed from the merge.
	 * @param newCell the cell in which to create the new node.
	 */
	private void realizeNode(PrimitiveNode pNp, double centerX, double centerY, double width, double height, int angle,
		Point2D [] points, PolyMerge merge, Cell newCell)
	{
		Orientation orient = Orientation.fromAngle(angle);
		NodeInst ni = NodeInst.makeInstance(pNp, new Point2D.Double(centerX / SCALEFACTOR, centerY / SCALEFACTOR),
			width / SCALEFACTOR, height / SCALEFACTOR, newCell, orient, null, 0);
		if (ni == null) return;
		if (points != null) ni.newVar(NodeInst.TRACE, points);

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
			removePolyFromMerge(merge, layer, poly);
		}
	}

	/**
	 * Method to create an arc and remove its geometry from the database.
	 * @param ap the arc to create.
	 * @param pi1 the first side of the arc.
	 * @param pi2 the second side of the arc
	 * @param width the width of the arc.
	 * @param noEndExtend true to NOT extend the arc ends.
	 */
	private ArcInst realizeArc(ArcProto ap, PortInst pi1, PortInst pi2, Point2D pt1, Point2D pt2, double width,
		boolean noHeadExtend, boolean noTailExtend, PolyMerge merge)
	{
		ArcInst ai = ArcInst.makeInstance(ap, width, pi1, pi2, pt1, pt2, null);
		if (ai == null) return null;
		if (noHeadExtend) ai.setHeadExtended(false);
		if (noTailExtend) ai.setTailExtended(false);

		// now remove the generated layers from the Merge
		Poly [] polys = tech.getShapeOfArc(ai);
		for(int i=0; i<polys.length; i++)
		{
			Poly poly = polys[i];
			Layer layer = poly.getLayer();

			// make sure the geometric database is made up of proper layers
			layer = geometricLayer(layer);

			removePolyFromMerge(merge, layer, poly);
		}
		return ai;
	}

	private void removePolyFromMerge(PolyMerge merge, Layer layer, Poly poly)
	{
		Point2D [] points = poly.getPoints();
		for(int i=0; i<points.length; i++)
			points[i].setLocation(scaleUp(points[i].getX()), scaleUp(points[i].getY()));
		poly.roundPoints();
		merge.subtract(layer, poly);
	}

	double scaleUp(double v)
	{
		return DBMath.round(v * SCALEFACTOR);
	}

	/**
	 * Method to compute the proper layer to use for a given other layer.
	 * Because pure-layer geometry may confuse similar layers that appear
	 * in Electric, all common layers are converted to a single one that will
	 * be unambiguous.  For example, all gate-poly is converted to poly-1.
	 * All diffusion layers are combined into one.  All layers that multiply-
	 * define a layer function are reduced to just one with that function.
	 * @param layer the layer found in the circuit.
	 * @return the layer to use instead.
	 */
	private Layer geometricLayer(Layer layer)
	{
		Layer.Function fun = layer.getFunction();

		// convert gate to poly1
		if (fun == Layer.Function.GATE)
		{
			Layer polyLayer = layerForFunction.get(Layer.Function.POLY1);
			if (polyLayer != null) return polyLayer;
		}

		// all active is one layer
		if (fun == Layer.Function.DIFFP || fun == Layer.Function.DIFFN)
			fun = Layer.Function.DIFF;

		// ensure the first one for the given function
		Layer properLayer = layerForFunction.get(fun);
		if (properLayer != null) return properLayer;
		return layer;

	}

	private static final double CLOSEDIST = SCALEFACTOR/50;

	private List<PolyBase> getMergePolys(PolyMerge merge, Layer layer)
	{
		List<PolyBase> polyList = merge.getMergedPoints(layer, true);
		if (polyList == null) return polyList;
		List<PolyBase> properPolyList = new ArrayList<PolyBase>();
		for(PolyBase poly : polyList)
		{
			// reduce common points
			Point2D [] origPoints = poly.getPoints();
			Point2D [] points = new Point2D[origPoints.length];
			int len = origPoints.length;
			for (int i=0; i<len; i++) points[i] = origPoints[i];
			for (int i=1; i<len; i++)
			{
				if (points[i].distance(points[i-1]) < CLOSEDIST)
				{
					for(int j=i; j<len; j++) points[j-1] = points[j];
					len--;
					i--;
				}
			}
			if (len > 1 && points[0].distance(points[len-1]) < CLOSEDIST) len--;

			// ignore polygons with no size
			if (len <= 2) continue;

			properPolyList.add(poly);
		}
		return properPolyList;
	}

	/**
	 * Debugging method to report the coordinates of all geometry on a given layer.
	 */
	public String describeLayer(PolyMerge merge, Layer layer)
	{
		List<PolyBase> polyList = getMergePolys(merge, layer);
		if (polyList == null) return "DOES NOT EXIST";
		StringBuffer sb = new StringBuffer();
		for(PolyBase p : polyList)
		{
			Point2D [] points = p.getPoints();
			sb.append(" [");
			for(int j=0; j<points.length; j++)
			{
				Point2D pt = points[j];
				if (j > 0) sb.append(" ");
				sb.append("("+pt.getX()+","+pt.getY()+")");
			}
			sb.append("]");
		}
		return sb.toString();
	}
}
