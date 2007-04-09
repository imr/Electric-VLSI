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
import com.sun.electric.database.geometry.ERectangle;
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
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.RTBounds;
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
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;

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
	/** true to debug object creation */						private static final boolean DEBUGSTEPS = false;

	/** the current technology for extraction */				private Technology tech;
	/** layers to use for given arc functions */				private HashMap<Layer.Function,Layer> layerForFunction;
	/** the layer to use for "polysilicon" geometry */			private Layer polyLayer;
	/** temporary layers to use for geometric manipulation */	private Layer tempLayer1, tempLayer2;
	/** the layers to use for "active" geometry */				private Layer pActiveLayer, nActiveLayer;
	/** associates arc prototypes with layers */				private HashMap<Layer,ArcProto> arcsForLayer;
	/** map of extracted cells */								private HashMap<Cell,Cell> convertedCells;
	/** map of export indices in each extracted cell */			private HashMap<Cell,GenMath.MutableInteger> exportNumbers;
	/** list of Exports to restore after extraction */			private List<Export> exportsToRestore;
	/** true if this is a P-well process (presume P-well) */	private boolean pWellProcess;
	/** true if this is a N-well process (presume N-well) */	private boolean nWellProcess;
	/** true if all active layers should be unified */			private boolean unifyActive;
	/** the smallest polygon acceptable for merging */			private double smallestPoly;
	/** debugging: list of objects created */					private List<ERectangle> addedRectangles;
	/** debugging: list of objects created */					private List<ERectangle> addedLines;

	/**
	 * Method to examine the current cell and extract it's connectivity in a new one.
	 * @param recursive true to recursively extract the hierarchy below this cell.
	 */
	public static void extractCurCell(boolean recursive)
	{
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
		/** debugging: list of objects created */	private List<List<ERectangle>> addedBatchRectangles;
		/** debugging: list of objects created */	private List<List<ERectangle>> addedBatchLines;
		/** debugging: list of objects created */	private List<String> addedBatchNames;

		private ExtractJob(Cell cell, boolean recursive)
		{
			super("Extract Connectivity from " + cell, Extract.getExtractTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.recursive = recursive;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			Connectivity c = new Connectivity(cell);
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
			if (DEBUGSTEPS)
			{
				addedBatchRectangles = new ArrayList<List<ERectangle>>();
				addedBatchLines = new ArrayList<List<ERectangle>>();
				addedBatchNames = new ArrayList<String>();
			}
			newCell = c.doExtract(cell, recursive, pat, true, addedBatchRectangles, addedBatchLines, addedBatchNames);
			fieldVariableChanged("addedBatchRectangles");
			fieldVariableChanged("addedBatchLines");
			fieldVariableChanged("addedBatchNames");
			fieldVariableChanged("newCell");
			return true;
		}

		public void terminateOK()
		{
			UserInterface ui = Job.getUserInterface();
			EditWindow_ wnd = ui.displayCell(newCell);

			if (DEBUGSTEPS)
			{
				// show results of each step
				JFrame jf = null;
				if (!TopLevel.isMDIMode()) jf = TopLevel.getCurrentJFrame();
				ShowExtraction theDialog = new ShowExtraction(jf, addedBatchRectangles, addedBatchLines, addedBatchNames);
				theDialog.setVisible(true);
			} else
			{
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
	}

	/**
	 * Constructor to initialize connectivity extraction.
	 * @param tech the Technology to extract to.
	 */
	private Connectivity(Cell cell)
	{
		tech = cell.getTechnology();
		convertedCells = new HashMap<Cell,Cell>();
		exportNumbers = new HashMap<Cell,GenMath.MutableInteger>();
		smallestPoly = (SCALEFACTOR * SCALEFACTOR) * Extract.getSmallestPolygonSize();

		// see if active layers should be unified
		unifyActive = Extract.isUnifyActive();
		if (!unifyActive)
		{
			boolean haveNActive = false, havePActive = false;
			for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				if (ni.isCellInstance()) continue;
				Poly[] polys = ni.getProto().getTechnology().getShapeOfNode(ni);
				for(int i=0; i<polys.length; i++)
				{
					Layer layer = polys[i].getLayer();
					if (layer == null) continue;
					if (!layer.getFunction().isDiff()) continue;
					if (layer.getFunction() == Layer.Function.DIFFN) haveNActive = true;
					if (layer.getFunction() == Layer.Function.DIFFP) havePActive = true;
				}
				if (haveNActive && havePActive) break;
			}
			if (!haveNActive || !havePActive)
			{
				System.out.println("Found only one type of active layer...unifying all active layers");
				unifyActive = true;
			}
		}

		// find important layers
		polyLayer = null;
		tempLayer1 = tempLayer2 = null;
		pActiveLayer = nActiveLayer = null;
		for(Iterator<Layer> it = tech.getLayers(); it.hasNext(); )
		{
			Layer layer = it.next();
			Layer.Function fun = layer.getFunction();
			if (polyLayer == null && fun == Layer.Function.POLY1) polyLayer = layer;
			if (fun == Layer.Function.POLY1 && layer.isPseudoLayer()) tempLayer1 = layer;
			if (fun == Layer.Function.METAL1 && layer.isPseudoLayer()) tempLayer2 = layer;
			if (unifyActive)
			{
				if (pActiveLayer == null && fun.isDiff()) pActiveLayer = layer;
			} else
			{
				if (pActiveLayer == null && fun == Layer.Function.DIFFP) pActiveLayer = layer;
				if (nActiveLayer == null && fun == Layer.Function.DIFFN) nActiveLayer = layer;
			}
		}
		polyLayer = polyLayer.getNonPseudoLayer();
		pActiveLayer = pActiveLayer.getNonPseudoLayer();
		if (unifyActive) nActiveLayer = pActiveLayer; else
			nActiveLayer = nActiveLayer.getNonPseudoLayer();

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
			if (unifyActive)
			{
				if (fun == Layer.Function.DIFFP || fun == Layer.Function.DIFFN)
					fun = Layer.Function.DIFF;
			}
			if (layerForFunction.get(fun) == null)
				layerForFunction.put(fun, layer);
		}
	}

	/**
	 * Top-level method in extracting connectivity from a Cell.
	 * A new version of the cell is created that has real nodes (transistors, contacts) and arcs.
	 * This cell should have pure-layer nodes which will be converted.
	 */
	private Cell doExtract(Cell oldCell, boolean recursive, Pattern pat, boolean top,
		List<List<ERectangle>> addedBatchRectangles, List<List<ERectangle>> addedBatchLines, List<String> addedBatchNames)
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
						doExtract(subCell, recursive, pat, false, addedBatchRectangles, addedBatchLines, addedBatchNames);
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
		exportsToRestore = new ArrayList<Export>();
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
		initDebugging();
		extractVias(merge, originalMerge, newCell);
		termDebugging(addedBatchRectangles, addedBatchLines, addedBatchNames, "Vias");
		System.out.println("Extracted vias");

		// now extract transistors
		initDebugging();
		extractTransistors(merge, originalMerge, newCell);
		termDebugging(addedBatchRectangles, addedBatchLines, addedBatchNames, "Transistors");
		System.out.println("Extracted transistors");

		// extend geometry that sticks out in space
		initDebugging();
		extendGeometry(merge, originalMerge, newCell, true);
		termDebugging(addedBatchRectangles, addedBatchLines, addedBatchNames, "StickOuts");
		System.out.println("Extracted extensions");

		// look for wires and pins
		initDebugging();
		if (makeWires(merge, originalMerge, newCell)) return newCell;
		termDebugging(addedBatchRectangles, addedBatchLines, addedBatchNames, "Wires");
		System.out.println("Extracted wires");

		// convert any geometry that connects two networks
		initDebugging();
		extendGeometry(merge, originalMerge, newCell, false);
		termDebugging(addedBatchRectangles, addedBatchLines, addedBatchNames, "Bridges");
		System.out.println("Extracted connections");

		// dump any remaining layers back in as extra pure layer nodes
		initDebugging();
		convertAllGeometry(merge, originalMerge, newCell);
		termDebugging(addedBatchRectangles, addedBatchLines, addedBatchNames, "Pures");
		System.out.println("Extracted geometry");

		// reexport any that were there before
		restoreExports(oldCell, newCell);

		// cleanup by auto-stitching
		PolyMerge originalUnscaledMerge = new PolyMerge();
		double shrinkage = 1.0 / SCALEFACTOR;
		AffineTransform shrink = new AffineTransform(shrinkage, 0, 0, shrinkage, 0, 0);
		originalUnscaledMerge.addMerge(originalMerge, shrink);
		Set<ArcInst> allArcs = null;
		if (DEBUGSTEPS)
		{
			allArcs = new HashSet<ArcInst>();
			for(Iterator<ArcInst> it = newCell.getArcs(); it.hasNext(); )
				allArcs.add(it.next());
		}
		AutoStitch.runAutoStitch(newCell, null, null, originalUnscaledMerge, null, false);
		if (DEBUGSTEPS)
		{
			initDebugging();
			for(Iterator<ArcInst> it = newCell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = it.next();
				if (allArcs.contains(ai)) continue;
	            Poly arcPoly = ai.makeLambdaPoly(ai.getGridBaseWidth(), Poly.Type.CLOSED);
				addedRectangles.add(ERectangle.fromLambda(arcPoly.getBounds2D()));
			}
			termDebugging(addedBatchRectangles, addedBatchLines, addedBatchNames, "Stitches");
		}
		System.out.println("Extraction done.");
		return newCell;
	}

	private void initDebugging()
	{
		if (DEBUGSTEPS)
		{
			addedRectangles = new ArrayList<ERectangle>();
			addedLines = new ArrayList<ERectangle>();
		}
	}

	private void termDebugging(List<List<ERectangle>> addedBatchRectangles,
		List<List<ERectangle>> addedBatchLines, List<String> addedBatchNames, String descr)
	{
		if (DEBUGSTEPS)
		{
			addedBatchRectangles.add(addedRectangles);
			addedBatchLines.add(addedLines);
			addedBatchNames.add(descr);
		}
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

			// see if the size is at an odd coordinate (and may suffer rounding problems)
			EPoint ctr = ni.getAnchorCenter();
			boolean growABit = false;
			if (((int)(ni.getXSize() * DBMath.GRID) % 2) != 0 ||
				((int)(ni.getYSize() * DBMath.GRID) % 2) != 0) growABit = true;

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
				} else
				{
					// grow the polygon to account for rounding problems
					if (growABit)
					{
						double growth = DBMath.getEpsilon()/2;
						Point2D polyCtr = poly.getCenter();
						for(int i=0; i<points.length; i++)
						{
							double x = points[i].getX();
							double y = points[i].getY();
							if (x < polyCtr.getX()) x -= growth; else x += growth;
							if (y < polyCtr.getY()) y -= growth; else y += growth;
							points[i].setLocation(x, y);
						}
					}
				}
				for(int i=0; i<points.length; i++)
					points[i].setLocation(scaleUp(points[i].getX()), scaleUp(points[i].getY()));
				merge.addPolygon(layer, poly);
			}

			// save exports on pure-layer nodes for restoration later
			for(Iterator<Export> it = ni.getExports(); it.hasNext(); )
			{
				Export e = it.next();
				exportsToRestore.add(e);
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
				wireLayers.add(layer);
		}

		// examine each wire layer, looking for a skeletal structure that approximates it
		for (Layer layer : wireLayers)
		{
			// examine the geometry on the layer
			List<PolyBase> polyList = getMergePolys(merge, layer);
			for(PolyBase poly : polyList)
			{
				// figure out which arcproto to use here
				ArcProto ap = findArcProtoForPoly(layer, poly, originalMerge);
				if (ap == null) continue;

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
					MutableBoolean headExtend = new MutableBoolean(true), tailExtend = new MutableBoolean(true);
					if (!originalMerge.arcPolyFits(layer, loc1, loc2, cl.width, headExtend, tailExtend))
					{
						// arc does not fit, try reducing width
						cl.width = 0;
						if (!originalMerge.arcPolyFits(layer, loc1, loc2, cl.width, headExtend, tailExtend)) continue;
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
			// examine the geometry on the layer
			List<PolyBase> polyList = getMergePolys(merge, layer);
			for(PolyBase poly : polyList)
			{
				Rectangle2D bounds = poly.getBounds2D();
				Poly rectPoly = new Poly(bounds);
				if (!originalMerge.contains(layer, rectPoly)) continue;

				// figure out which arc proto to use for the layer
				ArcProto ap = findArcProtoForPoly(layer, poly, originalMerge);
				if (ap == null) continue;

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
	 * Method to figure out which ArcProto to use for a polygon on a layer.
	 * In the case of Active layers, it examines the well and select layers to figure out
	 * which arctive arc to use.
	 * @param layer the layer of the polygon.
	 * @param poly the polygon
	 * @param originalMerge the merged data for the cell
	 * @return the ArcProto to use (null if none can be found).
	 */
	private ArcProto findArcProtoForPoly(Layer layer, PolyBase poly, PolyMerge originalMerge)
	{
		Layer.Function fun = layer.getFunction();
		if (fun.isPoly() || fun.isMetal()) return arcsForLayer.get(layer);
		if (!fun.isDiff()) return null;

		// must further differentiate the active arcs...find implants
		Layer wellP = null, wellN = null, selectP = null, selectN = null;
		for(Layer l : originalMerge.getKeySet())
		{
			if (l.getFunction() == Layer.Function.WELLP) wellP = l;
			if (l.getFunction() == Layer.Function.WELLN) wellN = l;
			if (l.getFunction() == Layer.Function.IMPLANTP) selectP = l;
			if (l.getFunction() == Layer.Function.IMPLANTN) selectN = l;
		}

		ArcProto.Function neededFunction = null;
		if (originalMerge.intersects(selectP, poly))
		{
			// Active in P-Select could either be a P-Active arc or a P-Well arc
			neededFunction = ArcProto.Function.DIFFP;
			if (wellP == null)
			{
				// a P-Well process: just look for the absense of N-Well surround
				if (!originalMerge.intersects(wellN, poly)) neededFunction = ArcProto.Function.DIFFS;
			} else
			{
				if (originalMerge.intersects(wellP, poly)) neededFunction = ArcProto.Function.DIFFS;
			}
		} else if (originalMerge.intersects(selectN, poly))
		{
			// Active in N-Select could either be a N-Active arc or a N-Well arc
			neededFunction = ArcProto.Function.DIFFN;
			if (wellN == null)
			{
				// a N-Well process: just look for the absense of P-Well surround
				if (!originalMerge.intersects(wellP, poly)) neededFunction = ArcProto.Function.DIFFW;
			} else
			{
				if (originalMerge.intersects(wellN, poly)) neededFunction = ArcProto.Function.DIFFW;
			}
		}

		// now find the arc with the desired function
		for(Iterator<ArcProto> aIt = tech.getArcs(); aIt.hasNext(); )
		{
			ArcProto ap = aIt.next();
			if (ap.getFunction() == neededFunction) return ap;
		}
		return null;
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
		for(Iterator<RTBounds> it = newCell.searchIterator(bounds); it.hasNext(); )
		{
			RTBounds geom = it.next();
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
		NodeInst ni = createNode(ap.findPinProto(), pt, size, size, null, newCell);
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
			return "CENTERLINE from ("+(start.getX()/SCALEFACTOR)+","+(start.getY()/SCALEFACTOR)+") to ("+
				(end.getX()/SCALEFACTOR)+","+(end.getY()/SCALEFACTOR)+") wid="+width/SCALEFACTOR+
			", len="+(start.distance(end)/SCALEFACTOR);
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
			List<PortInst> possiblePorts = findPortInstsTouchingPoint(startPoint, layer, newCell, ap);
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
				if (!isHub && cl.start.distance(cl.end) > cl.width)
					cl.setStart(cl.start.getX() + xOff, cl.start.getY() + yOff);
				NodeInst ni = wantNodeAt(cl.startUnscaled, pin, cl.width / SCALEFACTOR, newCell);
				loc1.setLocation(cl.startUnscaled.getX(), cl.startUnscaled.getY());
				piRet = ni.getOnlyPortInst();
			} else
			{
				if (!isHub && cl.start.distance(cl.end) > cl.width)
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
		for(Iterator<RTBounds> it = newCell.searchIterator(bounds); it.hasNext(); )
		{
			RTBounds geom = it.next();
			if (!(geom instanceof NodeInst)) continue;
			NodeInst ni = (NodeInst)geom;
			if (ni.getProto() != pin) continue;
			if (ni.getAnchorCenter().equals(pt)) return ni;
		}
		NodeInst ni = createNode(pin, pt, size, size, null, newCell);
		return ni;
	}

	private List<PortInst> findPortInstsTouchingPoint(Point2D pt, Layer layer, Cell newCell, ArcProto ap)
	{
		List<PortInst> touchingNodes = new ArrayList<PortInst>();
		boolean mightCreateExports = false;
		Rectangle2D checkBounds = new Rectangle2D.Double(pt.getX(), pt.getY(), 0, 0);
		for(Iterator<RTBounds> it = newCell.searchIterator(checkBounds); it.hasNext(); )
		{
			RTBounds geom = it.next();
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

			// for pins, must be centered over the desired point
			if (ni.getFunction() == PrimitiveNode.Function.PIN)
			{
				if (!ni.getOnlyPortInst().getPortProto().connectsTo(ap)) continue;
				if (ni.getAnchorCenter().equals(pt))
				{
					touchingNodes.add(ni.getOnlyPortInst());
				}
			} else
			{
				// nonpins can have any touching and connecting layer
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
		}

		// if no ports were found but there were cells, should create new exports in the cells
		if (touchingNodes.size() == 0 && mightCreateExports)
		{
			for(Iterator<RTBounds> it = newCell.searchIterator(checkBounds); it.hasNext(); )
			{
				RTBounds geom = it.next();
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
		for(Iterator<RTBounds> it = subCell.searchIterator(bounds); it.hasNext(); )
		{
			RTBounds geom = it.next();
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

			// make a list of all necessary layers
			Set<Layer> layersToExamine = new HashSet<Layer>();
			for(PossibleVia pv : possibleVias)
			{
				for(int i=0; i<pv.layers.length; i++)
					layersToExamine.add(pv.layers[i]);
			}

			// the new way to extract vias
			for(PolyBase cut : cutList)
			{
				// figure out which of the layers are present at this cut point
				Rectangle2D cutBox = cut.getBox();
				if (cutBox == null) continue;
				Point2D ctr = new Point2D.Double(cutBox.getCenterX(), cutBox.getCenterY());
				Set<Layer> layersPresent = new HashSet<Layer>();
				for(Layer l : layersToExamine)
				{
					if (originalMerge.contains(l, ctr))
						layersPresent.add(geometricLayer(l));
				}
				boolean ignorePWell = false, ignoreNWell = false;
				if (pWellProcess)
				{
					// P-Well process (P-well is presumed where there is no N-Well)
					boolean foundNWell = false;
					for(Layer l : layersPresent)
					{
						if (l.getFunction() == Layer.Function.WELLN) { foundNWell = true;   break; }
					}
					if (!foundNWell) ignorePWell = true;
				}
				if (nWellProcess)
				{
					// N-Well process (N-well is presumed where there is no P-Well)
					boolean foundPWell = false;
					for(Layer l : layersPresent)
					{
						if (l.getFunction() == Layer.Function.WELLP) { foundPWell = true;   break; }
					}
					if (!foundPWell) ignoreNWell = true;
				}

				for(PossibleVia pv : possibleVias)
				{
					// quick test to see if this via could possibly exist
					boolean someLayersMissing = false;
					for(int i=0; i<pv.layers.length; i++)
					{
						if (!layersPresent.contains(pv.layers[i]))
						{
							if (ignorePWell && pv.layers[i].getFunction() == Layer.Function.WELLP) continue;
							if (ignoreNWell && pv.layers[i].getFunction() == Layer.Function.WELLN) continue;
							someLayersMissing = true;
							break;
						}
					}
					if (someLayersMissing) continue;

					// extract the via if its layers fit
					if (extractOneVia(pv, cut, merge, originalMerge, newCell, ignorePWell, ignoreNWell)) break;
				}				
			}
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
//				Layer.Function lFun = nLayer.getFunction();
//				if (lFun == Layer.Function.WELLP && pWellProcess) continue;
//				if (lFun == Layer.Function.WELLN && nWellProcess) continue;

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
				}
			}
			if (!hvSymmetry || !rotSymmetry)
			{
				// horizontal/vertical or rotational symmetry missing: add a 90-degree rotation
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

		Collections.sort(possibleVias, new ViasBySize());
		return possibleVias;
	}

	/**
	 * Class to sort PossibleVia objects by their size.
	 */
	private static class ViasBySize implements Comparator<PossibleVia>
	{
		public int compare(PossibleVia pv1, PossibleVia pv2)
		{
			double area1 = 0;
			Technology.NodeLayer [] layers1 = pv1.pNp.getLayers();
			double wid = pv1.pNp.getDefWidth();
			double hei = pv1.pNp.getDefHeight();
			for(int i=0; i<layers1.length; i++)
			{
				Technology.NodeLayer nl = layers1[i];
				if (nl.getLayer().getFunction().isSubstrate()) continue;
				double lowX = nl.getLeftEdge().getMultiplier() * wid + nl.getLeftEdge().getAdder();
				double highX = nl.getRightEdge().getMultiplier() * wid + nl.getRightEdge().getAdder();
				double lowY = nl.getBottomEdge().getMultiplier() * hei + nl.getBottomEdge().getAdder();
				double highY = nl.getTopEdge().getMultiplier() * hei + nl.getTopEdge().getAdder();
				area1 += (highX-lowX) * (highY-lowY);
			}

			double area2 = 0;
			Technology.NodeLayer [] layers2 = pv2.pNp.getLayers();
			wid = pv2.pNp.getDefWidth();
			hei = pv2.pNp.getDefHeight();
			for(int i=0; i<layers2.length; i++)
			{
				Technology.NodeLayer nl = layers2[i];
				if (nl.getLayer().getFunction().isSubstrate()) continue;
				double lowX = nl.getLeftEdge().getMultiplier() * wid + nl.getLeftEdge().getAdder();
				double highX = nl.getRightEdge().getMultiplier() * wid + nl.getRightEdge().getAdder();
				double lowY = nl.getBottomEdge().getMultiplier() * hei + nl.getBottomEdge().getAdder();
				double highY = nl.getTopEdge().getMultiplier() * hei + nl.getTopEdge().getAdder();
				area2 += (highX-lowX) * (highY-lowY);
			}

			if (area1 == area2) return 0;
			if (area1 < area2) return 1;
			return -1;
		}
	}

	/**
	 * Method to see if a via exists around a given cut.
	 * @param pv the possible via.
	 * @param cut the cut layer that may be the via.
	 * @param merge the merge data at this point (after extracted nodes are removed).
	 * @param originalMerge the original merge data.
	 * @param newCell the cell in which extracted geometry goes.
	 * @return true if a contact was extracted.
	 */
	private boolean extractOneVia(PossibleVia pv, PolyBase cut, PolyMerge merge, PolyMerge originalMerge,
		Cell newCell, boolean ignorePWell, boolean ignoreNWell)
	{
		// can only handle manhattan contacts now
		Rectangle2D cutBox = cut.getBox();
		if (cutBox == null) return false;
		double cX = cutBox.getCenterX();
		double cY = cutBox.getCenterY();
		for(int i=0; i<pv.layers.length; i++)
		{
			Layer l = pv.layers[i];
			if (ignorePWell && l.getFunction() == Layer.Function.WELLP) continue;
			if (ignoreNWell && l.getFunction() == Layer.Function.WELLN) continue;
			double lX = cX - pv.minWidth/2 + pv.shrinkL[i];
			double hX = cX + pv.minWidth/2 - pv.shrinkR[i];
			double lY = cY - pv.minHeight/2 + pv.shrinkB[i];
			double hY = cY + pv.minHeight/2 - pv.shrinkT[i];
			double layerCX = (lX + hX) / 2;
			double layerCY = (lY + hY) / 2;
			PolyBase layerPoly = new PolyBase(layerCX, layerCY, hX-lX, hY-lY);
			if (!originalMerge.contains(l, layerPoly))
			{
//System.out.println("NO,  CONTACT "+pv.pNp.describe(false)+" ROTATED "+pv.rotation+" DOES NOT FIT AT ("
//	+(cX/SCALEFACTOR)+","+(cY/SCALEFACTOR)+") BECAUSE LAYER "+l.getName()+" NEEDS "+
//	(lX/SCALEFACTOR)+"<=X<="+(hX/SCALEFACTOR)+" AND "+(lY/SCALEFACTOR)+"<=Y<="+(hY/SCALEFACTOR));
				return false;
			}
		}
//System.out.println("YES, CONTACT "+pv.pNp.describe(false)+" ROTATED "+pv.rotation+" FITS AT ("+
//	(cX/SCALEFACTOR)+","+(cY/SCALEFACTOR)+")");

		// found: create the node
		realizeNode(pv.pNp, cX, cY, pv.minWidth, pv.minHeight, pv.rotation*10, null, merge, newCell);
		return true;
	}

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
			findTransistors(nTransistor, nActiveLayer, merge, originalMerge, newCell);
		}
		if (pTransistor != null)
		{
			findTransistors(pTransistor, pActiveLayer, merge, originalMerge, newCell);
		}
	}

	private void findTransistors(PrimitiveNode transistor, Layer activeLayer, PolyMerge merge, PolyMerge originalMerge, Cell newCell)
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
			points[i] = new EPoint((points[i].getX()) / SCALEFACTOR, (points[i].getY()) / SCALEFACTOR);
		realizeNode(transistor, cX, cY, hX - lX, hY - lY, 0, points, merge, newCell);
	}

	/********************************************** CONVERT CONNECTING GEOMETRY **********************************************/

	/**
	 * Method to determine the width offset of a given layer in an ArcProto.
	 * For most situations, this is zero.
	 * For Active arcs, the active layer is smaller than the default ArcProto width,
	 * and the difference is returned.
	 * @param layer the layer in question.
	 * @param ap the ArcProto in question.
	 * @return the difference between the default width of the layer and the ArcProto.
	 */
	private double getLayerOffset(Layer layer, ArcProto ap)
	{
		for(int i=0; i<ap.getNumArcLayers(); i++)
		{
			Technology.ArcLayer al = ap.getArcLayer(i);
			if (layer == al.getLayer())
				return al.getLambdaOffset();
		}
		return 0;
	}

	/**
	 * Method to look for opportunities to place arcs that connect to existing geometry.
	 */
	private void extendGeometry(PolyMerge merge, PolyMerge originalMerge, Cell newCell, boolean justExtend)
	{
		for (Layer layer : merge.getKeySet())
		{
			ArcProto ap = arcsForLayer.get(layer);
			if (ap == null) continue;
			double wid = ap.getDefaultLambdaFullWidth();
            double arcLayerWidth = wid - getLayerOffset(layer, ap);

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
						originalMerge.arcPolyFits(layer, pt1, pt2, arcLayerWidth, headExtend, tailExtend);
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
						originalMerge.arcPolyFits(layer, pt1, pt2, arcLayerWidth, headExtend, tailExtend);
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
						NodeInst ni = createNode(np, containsIt, np.getDefWidth(), np.getDefHeight(), null, newCell);
						PortInst pi = ni.getOnlyPortInst();

						MutableBoolean headExtend = new MutableBoolean(true), tailExtend = new MutableBoolean(true);
						originalMerge.arcPolyFits(layer, pt1, containsIt, arcLayerWidth, headExtend, tailExtend);
						ArcInst ai1 = realizeArc(ap, pi1, pi, pt1, containsIt, wid, !headExtend.booleanValue(), !tailExtend.booleanValue(), merge);

						headExtend.setValue(true);   tailExtend.setValue(true);
						originalMerge.arcPolyFits(layer, pt2, containsIt, arcLayerWidth, headExtend, tailExtend);
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
					polyBounds.getHeight() / SCALEFACTOR, null, newCell);
				NodeInst ni2 = createNode(np, pinPt2, polyBounds.getHeight() / SCALEFACTOR,
					polyBounds.getHeight() / SCALEFACTOR, null, newCell);
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
					polyBounds.getWidth() / SCALEFACTOR, null, newCell);
				NodeInst ni2 = createNode(np, pinPt2, polyBounds.getWidth() / SCALEFACTOR,
					polyBounds.getWidth() / SCALEFACTOR, null, newCell);
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
			Point2D objPt = new Point2D.Double(polyCtr.getX(), portRect.getCenterY());
			Point2D objPtNormal = new Point2D.Double(polyCtr.getX() / SCALEFACTOR, portRect.getCenterY() / SCALEFACTOR);
			Point2D pinPt = null;
			Point2D pinPtNormal = null;
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
				pinPt = new Point2D.Double(polyCtr.getX(), polyBounds.getMaxY() - endExtension);
				pinPtNormal = new Point2D.Double(polyCtr.getX() / SCALEFACTOR,
					(polyBounds.getMaxY() - endExtension) / SCALEFACTOR);
			} else
			{
				// going down to the poly
				pinPt = new Point2D.Double(polyCtr.getX(), polyBounds.getMinY() + endExtension);
				pinPtNormal = new Point2D.Double(polyCtr.getX() / SCALEFACTOR,
					(polyBounds.getMinY() + endExtension) / SCALEFACTOR);
			}

			NodeInst ni1 = createNode(np, pinPtNormal, polyBounds.getWidth() / SCALEFACTOR,
				polyBounds.getWidth() / SCALEFACTOR, null, newCell);

			MutableBoolean headExtend = new MutableBoolean(endExtend), tailExtend = new MutableBoolean(endExtend);
			double wid = polyBounds.getWidth();
			double fullWid = wid / SCALEFACTOR + ap.getLambdaWidthOffset();
			originalMerge.arcPolyFits(layer, pinPt, objPt, wid, headExtend, tailExtend);
			ArcInst ai = realizeArc(ap, ni1.getOnlyPortInst(), pi, pinPtNormal, objPtNormal, fullWid,
				!headExtend.booleanValue(), !tailExtend.booleanValue(), merge);
			return;
		}

		// can we extend horizontally
		if (polyCtr.getY() >= portRect.getMinY() && polyCtr.getY() <= portRect.getMaxY())
		{
			// going left to the poly or right?
			Point2D objPt = new Point2D.Double(portRect.getCenterX(), polyCtr.getY());
			Point2D objPtNormal = new Point2D.Double(portRect.getCenterX() / SCALEFACTOR, polyCtr.getY() / SCALEFACTOR);
			Point2D pinPt = null;
			Point2D pinPtNormal = null;
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
				pinPt = new Point2D.Double(polyBounds.getMaxX() - endExtension, polyCtr.getY());
				pinPtNormal = new Point2D.Double((polyBounds.getMaxX() - endExtension) / SCALEFACTOR,
					polyCtr.getY() / SCALEFACTOR);
			} else
			{
				// going left to the poly
				pinPt = new Point2D.Double(polyBounds.getMinX() + endExtension, polyCtr.getY());
				pinPtNormal = new Point2D.Double((polyBounds.getMinX() + endExtension) / SCALEFACTOR,
					polyCtr.getY() / SCALEFACTOR);
			}
			NodeInst ni1 = createNode(np, pinPtNormal, polyBounds.getHeight() / SCALEFACTOR,
				polyBounds.getHeight() / SCALEFACTOR, null, newCell);
			MutableBoolean headExtend = new MutableBoolean(endExtend), tailExtend = new MutableBoolean(endExtend);
			double wid = polyBounds.getHeight();
			double fullWid = wid / SCALEFACTOR + ap.getLambdaWidthOffset();
			originalMerge.arcPolyFits(layer, pinPt, objPt, wid, headExtend, tailExtend);
			ArcInst ai = realizeArc(ap, ni1.getOnlyPortInst(), pi, pinPtNormal, objPtNormal,
				fullWid, !headExtend.booleanValue(), !tailExtend.booleanValue(), merge);
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
		for(Iterator<RTBounds> it = newCell.searchIterator(bounds); it.hasNext(); )
		{
			RTBounds geom = it.next();
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
		for(Iterator<RTBounds> it = newCell.searchIterator(bounds); it.hasNext(); )
		{
			RTBounds geom = it.next();
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
//System.out.println("====== ANALYZING "+polysToAnalyze.size()+" POLYGONS: ======");
			for(PolyBase aPoly : polysToAnalyze)
			{
				// first make a list of all parallel wires in the polygon
				aPoly.setLayer(layer);
//System.out.print("FOR POLY:");
//Point2D [] points = aPoly.getPoints();
//for(int i=0; i<points.length; i++) System.out.print(" ("+points[i].getX()/SCALEFACTOR+","+points[i].getY()/SCALEFACTOR+")");
//System.out.println();
				List<Centerline> centerlines = gatherCenterlines(aPoly, merge, originalMerge);

//System.out.println("GATHERED:");
//for(Centerline cl : centerlines)
//	System.out.println("    "+cl);
				// now pull out the relevant ones
				double lastWidth = -1;
				boolean lastWidthNonManhattan = false;
				for(Centerline cl : centerlines)
				{
					if (cl.width < minWidth) continue;

					// make the polygon to describe the centerline
					double length = cl.start.distance(cl.end);
					if (length < DBMath.getEpsilon()) continue;
					Poly clPoly = Poly.makeEndPointPoly(length, cl.width, cl.angle,
						cl.start, 0, cl.end, 0, Poly.Type.FILLED);

//merge.deleteLayer(tempLayer2);
//merge.addLayer(tempLayer1, tempLayer2);
//double areaLeftBefore = merge.getAreaOfLayer(tempLayer2);
//merge.subtract(tempLayer2, clPoly);
//double areaLeftAfter = merge.getAreaOfLayer(tempLayer2);
//System.out.println("SO......Removing "+cl.toString()+" reduces area from "+areaLeftBefore+" to "+areaLeftAfter);

					// see if this centerline actually covers new area
					if (!merge.intersects(tempLayer1, clPoly)) continue;

					// if wider than long, this cannot be the first centerline
					if (validCenterlines.size() == 0)
					{
						double len = cl.start.distance(cl.end);
						if (cl.width > len) continue;
					}

					// for nonmanhattan centerlines, do extra work to ensure uniqueness
					boolean isNonManhattan = false;
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
						isNonManhattan = true;
					}

					// if narrower centerlines have already been added, stop now
					if (lastWidth < 0)
					{
						lastWidth = cl.width;
						lastWidthNonManhattan = isNonManhattan;
					}
					if (Math.abs(cl.width - lastWidth) > 1)
					{
						if (lastWidthNonManhattan != isNonManhattan) break;
						double smallest = Math.min(cl.width, lastWidth);
						if (smallest != 0 && Math.max(cl.width, lastWidth) / smallest > 1.2) break;
					}

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

//for(int i=0; i<validCenterlines.size(); i++) System.out.println("MERGED CENTERLINE: "+validCenterlines.get(i));

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

//System.out.println("COMPARE "+cl+" WITH "+oCl);
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
//for(Centerline cl : validCenterlines) System.out.println("    "+cl);
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
		for (Layer layer : merge.getKeySet())
		{
			ArcProto ap = arcsForLayer.get(layer);
			List<PolyBase> polyList = getMergePolys(merge, layer);

			// implant layers may be large rectangles
			if (layer.getFunction().isSubstrate())
			{
				// make a list of rectangles that are proper approximations of polygons
				List<Rectangle2D> rectangleList = new ArrayList<Rectangle2D>();
				for(int i=polyList.size()-1; i >= 0; i--)
				{
					PolyBase poly = polyList.get(i);
					Rectangle2D polyBounds = poly.getBounds2D();
					if (originalMerge.contains(layer, polyBounds))
					{
						rectangleList.add(polyBounds);
						polyList.remove(i);
					}
				}

				// iterate over the list of rectangles and aggregate into larger rectangles
				for(;;)
				{
					Rectangle2D aggregateBounds = null;
					List<Rectangle2D> aggregatedList = new ArrayList<Rectangle2D>();
					for(Rectangle2D polyBounds : rectangleList)
					{
						if (aggregateBounds == null) aggregateBounds = polyBounds; else
						{
							Rectangle2D unionBounds = new Rectangle2D.Double();
							Rectangle2D.union(aggregateBounds, polyBounds, unionBounds);
							if (!originalMerge.contains(layer, unionBounds)) continue;
							aggregateBounds = unionBounds;
						}
						aggregatedList.add(polyBounds);
					}
					if (aggregateBounds == null) break;

					// valid rectangle: cover it all with a large pure layer rectangle
					PrimitiveNode pNp = layer.getPureLayerNode();
					double centerX = aggregateBounds.getCenterX() / SCALEFACTOR;
					double centerY = aggregateBounds.getCenterY() / SCALEFACTOR;
					Point2D center = new Point2D.Double(centerX, centerY);
					NodeInst ni = createNode(pNp, center, aggregateBounds.getWidth() / SCALEFACTOR,
						aggregateBounds.getHeight() / SCALEFACTOR, null, newCell);
					for(Rectangle2D polyBounds : aggregatedList)
						rectangleList.remove(polyBounds);
				}
			}

			for(PolyBase poly : polyList)
			{
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
								NodeInst ni1 = createNode(np, end1, height, height, null, newCell);
								NodeInst ni2 = createNode(np, end2, height, height, null, newCell);
								ArcInst ai = realizeArc(ap, ni1.getOnlyPortInst(), ni2.getOnlyPortInst(), end1, end2, height+ap.getLambdaWidthOffset(),
									false, false, merge);
							} else
							{
								// make a vertical arc
								Point2D end1 = new Point2D.Double(polyBounds.getCenterX() / SCALEFACTOR, (polyBounds.getMinY()+width/2) / SCALEFACTOR);
								Point2D end2 = new Point2D.Double(polyBounds.getCenterX() / SCALEFACTOR, (polyBounds.getMaxY()-width/2) / SCALEFACTOR);
								NodeInst ni1 = createNode(np, end1, width, width, null, newCell);
								NodeInst ni2 = createNode(np, end2, width, width, null, newCell);
								ArcInst ai = realizeArc(ap, ni1.getOnlyPortInst(), ni2.getOnlyPortInst(), end1, end2, width+ap.getLambdaWidthOffset(),
									false, false, merge);
							}
							continue;
						}
					}
				}

				// just generate more pure-layer nodes
				PrimitiveNode pNp = layer.getPureLayerNode();
				if (pNp == null)
				{
					System.out.println("CANNOT FIND PURE LAYER NODE FOR LAYER "+poly.getLayer().getName());
					continue;
				}
				Rectangle2D polyBounds = poly.getBounds2D();
				double centerX = polyBounds.getCenterX() / SCALEFACTOR;
				double centerY = polyBounds.getCenterY() / SCALEFACTOR;
				Point2D center = new Point2D.Double(centerX, centerY);

				// compute any trace information if the shape is nonmanhattan
				EPoint [] newPoints = null;
				if (poly.getBox() == null)
				{
					// store the trace
					Point2D [] points = poly.getPoints();
					newPoints = new EPoint[points.length];
					for(int i=0; i<points.length; i++)
						newPoints[i] = new EPoint(points[i].getX() / SCALEFACTOR, points[i].getY() / SCALEFACTOR);
				}
				NodeInst ni = createNode(pNp, center, polyBounds.getWidth() / SCALEFACTOR,
					polyBounds.getHeight() / SCALEFACTOR, newPoints, newCell);

				// connect to the rest if possible
				if (ap != null)
				{
					PortInst fPi = ni.getOnlyPortInst();
					Poly fPortPoly = fPi.getPoly();
					Rectangle2D searchBound = new Rectangle2D.Double(polyBounds.getMinX()/SCALEFACTOR, polyBounds.getMinY()/SCALEFACTOR,
						polyBounds.getWidth()/SCALEFACTOR, polyBounds.getHeight()/SCALEFACTOR);
					PortInst bestTPi = null;
					double bestLen = Double.MAX_VALUE;
					Point2D bestFLoc = null;
					for(Iterator<RTBounds> it = newCell.searchIterator(searchBound); it.hasNext(); )
					{
						RTBounds geom = it.next();
						if (!(geom instanceof NodeInst)) continue;
						NodeInst oNi = (NodeInst)geom;
						if (oNi == ni) continue;
						if (oNi.isCellInstance()) continue;
						if (oNi.getProto().getTechnology() != tech) continue;
						for(Iterator<PortInst> pIt = oNi.getPortInsts(); pIt.hasNext(); )
						{
							PortInst tPi = pIt.next();
							PortProto pp = tPi.getPortProto();
							if (!pp.connectsTo(ap)) continue;
							EPoint tLoc = tPi.getPoly().getCenter();
							EPoint tLocScaled = new EPoint(scaleUp(tLoc.getX()), scaleUp(tLoc.getY()));
							Point2D fLoc = fPortPoly.closestPoint(tLoc);
							EPoint fLocScaled = new EPoint(scaleUp(fLoc.getX()), scaleUp(fLoc.getY()));
							double len = scaleUp(fLoc.distance(tLoc));
							int angle = GenMath.figureAngle(fLoc, tLoc);
							Poly conPoly = Poly.makeEndPointPoly(len, 1, angle, fLocScaled, 0, tLocScaled, 0, Poly.Type.FILLED);
							if (originalMerge.contains(layer, conPoly))
							{
								if (len < bestLen)
								{
									bestLen = len;
									bestTPi = tPi;
									bestFLoc = fLoc;
								}
							}
						}
					}
					if (bestTPi != null)
					{
						Poly tPortPoly = bestTPi.getPoly();
						Point2D tLoc = tPortPoly.closestPoint(tPortPoly.getCenter());
						ArcInst ai = realizeArc(ap, fPi, bestTPi, bestFLoc, tLoc, 0, false, false, merge);
						if (ai != null) ai.setFixedAngle(false);
					} else
					{
						System.out.println("Unable to connect unextracted polygon");
					}
				}
			}
		}
	}

	/**
	 * Method to restore exports that were in the original cell.
	 * These exports were on pure-layer nodes and must now be placed back
	 * as close to the original as possible.
	 */
	private void restoreExports(Cell oldCell, Cell newCell)
	{
		for(Export e : exportsToRestore)
		{
			EPoint loc = e.getOriginalPort().getPoly().getCenter();
			boolean found = false;
			Rectangle2D bounds = new Rectangle2D.Double(loc.getX(), loc.getY(), 0, 0);
			for(Iterator<RTBounds> it = newCell.searchIterator(bounds); it.hasNext(); )
			{
				RTBounds geom = it.next();
				if (!(geom instanceof NodeInst)) continue;
				NodeInst ni = (NodeInst)geom;
				for(Iterator<PortInst> pIt = ni.getPortInsts(); pIt.hasNext(); )
				{
					PortInst pi = pIt.next();
					PortProto pp = pi.getPortProto();
					if (!sameConnection(e, pp)) continue;
					EPoint pLoc = pi.getPoly().getCenter();
					if (loc.equals(pLoc))
					{
						Export.newInstance(newCell, pi, e.getName());
						found = true;
						break;
					}
				}
			}
			if (!found)
			{
				// did not reexport: create the pin and export...let it get autorouted in
				PrimitiveNode pnUse = null;
				for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
				{
					PrimitiveNode pn = it.next();
					if (pn.getFunction() != PrimitiveNode.Function.PIN) continue;
					if (!sameConnection(e, pn.getPort(0))) continue;
					pnUse = pn;
					break;
				}
				if (pnUse == null) continue;
				NodeInst ni = NodeInst.makeInstance(pnUse, loc, pnUse.getDefWidth(), pnUse.getDefHeight(), newCell);
				Export.newInstance(newCell, ni.getOnlyPortInst(), e.getName());
			}
		}
	}

	/**
	 * Method to compare two ports to see that they connect to the same set of arcs.
	 * @param pp1 the first port.
	 * @param pp2 the second port.
	 * @return true if they connect to the same arcs.
	 */
	private boolean sameConnection(PortProto pp1, PortProto pp2)
	{
		ArcProto [] arcs1 = pp1.getBasePort().getConnections();
		ArcProto [] arcs2 = pp2.getBasePort().getConnections();
		if (arcs1 == arcs2) return true;
		boolean [] found = new boolean[arcs2.length];
		for(int i=0; i<arcs1.length; i++)
		{
			if (arcs1[i].getTechnology() == Generic.tech) continue;
			int j = 0;
			for( ; j<arcs2.length; j++)
			{
				if (arcs1[i] == arcs2[j])
				{
					found[j] = true;
					break;
				}
			}
			if (j >= arcs2.length) return false;
		}
		for(int j=0; j<arcs2.length; j++)
		{
			if (found[j]) continue;
			if (arcs2[j].getTechnology() != Generic.tech) return false;
		}
		return true;
	}

	private NodeInst createNode(NodeProto np, Point2D loc, double wid, double hei, EPoint [] points, Cell cell)
	{
		// pins cannot be smaller than their default size
		if (np.getFunction() == PrimitiveNode.Function.PIN)
		{
			if (wid < np.getDefWidth()) wid = np.getDefWidth();
			if (hei < np.getDefHeight()) hei = np.getDefHeight();
		}
		NodeInst ni = NodeInst.makeInstance(np, loc, wid, hei, cell);
		if (ni != null && points != null)
			ni.setTrace(points);
		if (DEBUGSTEPS)
		{
			if (np.getFunction() != PrimitiveNode.Function.PIN)
			{
	            Poly niPoly = Highlight.getNodeInstOutline(ni);
				addedRectangles.add(ERectangle.fromLambda(niPoly.getBounds2D()));
			}
		}
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
		double cX = centerX / SCALEFACTOR;
		double cY = centerY / SCALEFACTOR;
		NodeInst ni = NodeInst.makeInstance(pNp, new Point2D.Double(cX, cY),
			width / SCALEFACTOR, height / SCALEFACTOR, newCell, orient, null, 0);
		if (ni == null) return;
		if (points != null)
		{
			ni.setTrace(points);
			if (DEBUGSTEPS)
			{
				for(int i=1; i<points.length; i++)
				{
					double sx = points[i-1].getX();
					double sy = points[i-1].getY();
					double ex = points[i].getX();
					double ey = points[i].getY();
					addedLines.add(ERectangle.fromLambda(sx, sy, ex-sx, ey-sy));
				}
			}
		} else
		{
			if (DEBUGSTEPS)
			{
				if (pNp.getFunction() != PrimitiveNode.Function.PIN)
				{
		            Poly niPoly = Highlight.getNodeInstOutline(ni);
					addedRectangles.add(ERectangle.fromLambda(niPoly.getBounds2D()));
				}
			}
		}

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

		// remember this arc for debugging
		if (DEBUGSTEPS)
		{
            Poly arcPoly = ai.makeLambdaPoly(ai.getGridBaseWidth(), Poly.Type.CLOSED);
			addedRectangles.add(ERectangle.fromLambda(arcPoly.getBounds2D()));
		}

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
		return DBMath.round(v) * SCALEFACTOR;
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
		if (unifyActive)
		{
			if (fun == Layer.Function.DIFFP || fun == Layer.Function.DIFFN)
				fun = Layer.Function.DIFF;
		}

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

			// anything smaller than the minimum number of grid units is ignored
			double area = poly.getArea();
			if (area < smallestPoly) continue;

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

	/**
	 * Class for showing progress of extraction in a modeless dialog
	 */
	private static class ShowExtraction extends EDialog
	{
		private List<List<ERectangle>> addedBatchRectangles;
		private List<List<ERectangle>> addedBatchLines;
		private List<String> addedBatchNames;
		private int batchPosition;
		private JLabel comingUp;

		private ShowExtraction(Frame parent, List<List<ERectangle>> addedBatchRectangles,
			List<List<ERectangle>> addedBatchLines, List<String> addedBatchNames)
		{
			super(parent, false);
			this.addedBatchRectangles = addedBatchRectangles;
			this.addedBatchLines = addedBatchLines;
			this.addedBatchNames = addedBatchNames;

	        getContentPane().setLayout(new GridBagLayout());
	        setTitle("Extraction Progress");
	        setName("");
	        addWindowListener(new WindowAdapter()
	        {
	            public void windowClosing(WindowEvent evt) { closeDialog(); }
	        });

	        GridBagConstraints gbc;
	        comingUp = new JLabel("Next step:");
	        gbc = new GridBagConstraints();
	        gbc.gridx = 0;   gbc.gridy = 0;
	        gbc.gridwidth = 2;
	        gbc.anchor = GridBagConstraints.WEST;
	        gbc.insets = new Insets(4, 4, 4, 4);
	        getContentPane().add(comingUp, gbc);

	        JButton prev = new JButton("Prev");
	        gbc = new GridBagConstraints();
	        gbc.gridx = 0;   gbc.gridy = 1;
	        gbc.anchor = GridBagConstraints.WEST;
	        gbc.insets = new Insets(4, 4, 4, 4);
	        prev.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent evt) { advanceDisplay(false); }
            });
	        getContentPane().add(prev, gbc);

	        JButton next = new JButton("Next");
	        gbc = new GridBagConstraints();
	        gbc.gridx = 1;   gbc.gridy = 1;
	        gbc.anchor = GridBagConstraints.WEST;
	        gbc.insets = new Insets(4, 4, 4, 4);
	        next.addActionListener(new ActionListener()
            {
                public void actionPerformed(ActionEvent evt) { advanceDisplay(true); }
            });
	        getContentPane().add(next, gbc);

			batchPosition = -1;
			advanceDisplay(true);

			getRootPane().setDefaultButton(next);
			finishInitialization();
		}

		protected void escapePressed() { closeDialog(); }

		private void advanceDisplay(boolean forward)
		{
			if (forward)
			{
				batchPosition++;
				if (batchPosition >= addedBatchNames.size()) batchPosition = addedBatchNames.size()-1;
			} else
			{
				batchPosition--;
				if (batchPosition < 0) batchPosition = 0;
			}
			comingUp.setText("Batch " + (batchPosition+1) + ": " + addedBatchNames.get(batchPosition));
	        pack();

	        UserInterface ui = Job.getUserInterface();
			EditWindow_ wnd = ui.getCurrentEditWindow_();
			wnd.clearHighlighting();
			Cell cell = wnd.getCell();
			
			// highlight
			List<ERectangle> rects = addedBatchRectangles.get(batchPosition);
			for(ERectangle er : rects)
				wnd.addHighlightArea(er, cell);
			List<ERectangle> lines = addedBatchLines.get(batchPosition);
			for(ERectangle er : lines)
				wnd.addHighlightLine(new Point2D.Double(er.getMinX(), er.getMinY()),
					new Point2D.Double(er.getMaxX(), er.getMaxY()), cell, false);
			wnd.finishedHighlighting();
		}
	}
}
