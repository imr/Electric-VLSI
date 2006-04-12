/* -*- tab-width: 4 -*-
 * Electric(tm) VLSI Design System
 *
 * File: PostScriptColor.java
 * Input/output tool: PostScript color merged output
 * Written by: David Harris, 4/20/01 (David_Harris@hmc.edu)
 * Translated to Java by Steven Rubin: 12/05
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
package com.sun.electric.tool.io.output;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.user.User;

import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * This class writes files in PostScript format.
 * It handles color better than existing freely available
 * postscript generators.  It does not handle arbitrary rotations.
 *
 * Limitations:
 *  the code to handle quad trees is rather messy now
 *
 * Ideas:
 * 	center port labels
 * 	give options about aspect ratio / page size
 * 	put date on caption
 * 	print layers on caption
 * 	draw outlines around edges
 * 	handle black & white mode
 *
 * Things still to do:
 *    circles and arcs
 *    rotation of the plot
 *    eps
 */
public class PostScriptColor
{
	// Constants
	private static final int    MAXLAYERS = 1000;
	private static final int    TREETHRESHOLD = 500;
	private static final double SMALL_NUM = 0.000001;
	private static final String FONTNAME = "Helvetica";

	private static class PsPoly
	{
		double [] coords;
		int       layer;
	};

	private static class PsBox
	{
		double [] pos = new double[4];  // 0: dx, 1: dy, 2: left, 3: bot
		int       layer;
		boolean   visible;
	};

	private static class PsLabel
	{
		String         label;
		double []      pos = new double[4];
		Poly.Type      style;
		TextDescriptor descript;
	};

	private static class PsCell
	{
		int	             cellNum;
		List<PsBox>      boxes;
		List<PsPoly>     polys;
		List<PsLabel>    labels;
		List<PsCellInst> inst;
	};

	private static class PsCellInst
	{
		double []  transform = new double[9];
		PsCell     inst;
	};

	private static class PsBoxElement
	{
		double [] pos = new double[4]; // 0: dx, 1: dy, 2: left, 3: bot
		int       layer;
		boolean   visible;
	};

	private static class PxBoxQuadTree
	{
		int             numBoxes;
		PsBoxElement [] boxes;
		double []       bounds = new double[4]; // 0: dx, 1: dy, 2: left, 3: bot
		PxBoxQuadTree   tl;
		PxBoxQuadTree   tr;
		PxBoxQuadTree   bl;
		PxBoxQuadTree   br;
		PxBoxQuadTree   parent;
		int             level;
	};

	private static class LayerMap
	{
		Layer      layer;
		int        mix1, mix2; // layer made from mix of previous layers, or -1 if not
		double     r, g, b;
		double     opacity;
		boolean    foreground;
	};

	// Globals
	private double []            psBoundaries = new double[4];
	private List<PsCell>         allCells;
	private LayerMap  []         allLayers = new LayerMap[MAXLAYERS];
	private List<ArrayList<PsBox>>flattenedBoxes = new ArrayList<ArrayList<PsBox>>();
	private PxBoxQuadTree []     quadTrees = new PxBoxQuadTree[MAXLAYERS];
	private List<ArrayList<PsPoly>>flattenedPolys = new ArrayList<ArrayList<PsPoly>>();
	private int                  numLayers;
	private int                  totalBoxes = 0;
	private int                  totalCells = 0;
	private int                  totalPolys = 0;
	private int                  totalInstances = 0;
	private int                  cellNumber;
	private boolean              curveWarning;
	private HashSet<Technology>  techsSetup;
	private HashMap<Cell,PsCell> cellStructs;
	private PostScript           psObject;

	private PostScriptColor(PostScript psObject)
	{
		this.psObject = psObject;
	}

	/**
	 * Main entry point for color PostScript output.
	 * @param psObject the PostScript writer object.
	 * @param cell the Cell being written.
	 * @param epsFormat true to write encapsulated PostScript.
	 * @param usePlotter true for an infinitely-tall plotter, where page height is not a consideration.
	 * @param pageWid the paper width (in 1/75 of an inch).
	 * @param pageHei the paper height (in 1/75 of an inch).
	 * @param pageMargin the inset margins (in 1/75 of an inch).
	 */
	public static void psColorPlot(PostScript psObject, Cell cell, boolean epsFormat, boolean usePlotter,
		double pageWid, double pageHei, double pageMargin)
	{
		PostScriptColor psc = new PostScriptColor(psObject);
		psc.doPrinting(cell, epsFormat, usePlotter, pageWid, pageHei, pageMargin);
	}

	private void doPrinting(Cell cell, boolean epsFormat, boolean usePlotter,
		double pageWid, double pageHei, double pageMargin)
	{
		totalBoxes = totalCells = totalPolys = totalInstances = 0;
		psBoundaries[0] = 1<<30;
		psBoundaries[1] = 1<<30;
		psBoundaries[2] = -1<<30;
		psBoundaries[3] = -1<<30;

		for (int i=0; i<MAXLAYERS; i++)
			quadTrees[i] = null;
		cellNumber = 1;

		// initialize layer maps for the current technology
		numLayers = 0;
		techsSetup = new HashSet<Technology>();

		// mark all cells as "not written"
		cellStructs = new HashMap<Cell,PsCell>();
		getLayerMap(Technology.getCurrent());

		curveWarning = false;
		allCells = new ArrayList<PsCell>();
		extractDatabase(cell);
		mergeBoxes();
		flatten();
		genOverlapShapesAfterFlattening();
		writePS(cell, usePlotter, pageWid, pageHei, pageMargin);
//		printStatistics();
	}

	/**
	 * Method to get the print colors and load them into the layer map.
	 */
	private void getLayerMap(Technology tech)
	{
		// see if this technology has already been done
		if (techsSetup.contains(tech)) return;
		techsSetup.add(tech);

		// read layer map
		int startLayer = numLayers;
		List<Layer> layerSorts = new ArrayList<Layer>();
		for(int i=0; i<tech.getNumLayers(); i++)
		{
			Layer layer = tech.getLayer(i);
			Layer.Function fun = layer.getFunction();
			if ((layer.getFunctionExtras()&Layer.Function.PSEUDO) != 0) continue;
			layerSorts.add(layer);
		}

		// sort by layer height
		Collections.sort(layerSorts, new LayersByDepth());

		// load the layer information
		for(int i=0; i<layerSorts.size(); i++)
		{
			if (numLayers >= MAXLAYERS)
			{
				System.out.println("More than " + MAXLAYERS + " layers");
				break;
			}
			Layer layer = layerSorts.get(i);
			allLayers[numLayers] = new LayerMap();
			allLayers[numLayers].layer = layer;
			EGraphics graph = layer.getGraphics();
			allLayers[numLayers].opacity = graph.getOpacity();
			allLayers[numLayers].foreground = graph.getForeground();
			Color col = graph.getColor();
			allLayers[numLayers].r = col.getRed() / 255.0;
			allLayers[numLayers].g = col.getGreen() / 255.0;
			allLayers[numLayers].b = col.getBlue() / 255.0;
			allLayers[numLayers].mix1 = -1;
			allLayers[numLayers].mix2 = -1;
			if (allLayers[numLayers].opacity < 1)
			{
				// create new layers to provide transparency
				int curLayer = numLayers;
				for (int k=startLayer; k < curLayer; k++)
				{
					if (allLayers[k].foreground)
					{
						allLayers[++numLayers] = new LayerMap();
						allLayers[numLayers].opacity = 1;
						allLayers[numLayers].layer = null;
						allLayers[numLayers].foreground = true;
						allLayers[numLayers].r = allLayers[curLayer].r*allLayers[curLayer].opacity +
							allLayers[k].r*(1-allLayers[curLayer].opacity);
						allLayers[numLayers].g = allLayers[curLayer].g*allLayers[curLayer].opacity +
							allLayers[k].g*(1-allLayers[curLayer].opacity);
						allLayers[numLayers].b = allLayers[curLayer].b*allLayers[curLayer].opacity +
							allLayers[k].b*(1-allLayers[curLayer].opacity);
						allLayers[numLayers].mix1 = k;
						allLayers[numLayers].mix2 = curLayer;
					}
				}
			}
			numLayers++;
		}
	}

	/**
	 * Comparator class for sorting Layers by their height.
	 */
	private static class LayersByDepth implements Comparator<Layer>
	{
		/**
		 * Method to sort LayerSort by their height.
		 */
		public int compare(Layer l1, Layer l2)
		{
			double diff = l1.getDepth() - l2.getDepth();
			if (diff == 0.0) return 0;
			if (diff < 0.0) return -1;
			return 1;
		}
	}

	private void extractDatabase(Cell cell)
	{
		// check for subcells that haven't been written yet
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (!ni.isCellInstance()) continue;
			if (!ni.isExpanded()) continue;
			Cell subCell = (Cell)ni.getProto();
			if (cellStructs.get(subCell) != null) continue;
			extractDatabase(subCell);
		}

		// create a cell
		PsCell curCell = new PsCell();
		curCell.cellNum = cellNumber++;
		curCell.boxes = new ArrayList<PsBox>();
		curCell.polys = new ArrayList<PsPoly>();
		curCell.labels = new ArrayList<PsLabel>();
		curCell.inst = new ArrayList<PsCellInst>();
		totalCells++;

		// add to the lists
		allCells.add(curCell);
		cellStructs.put(cell, curCell);

		// examine all nodes in the cell
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			NodeProto np = ni.getProto();
			if (ni.isCellInstance())
			{
				// instance
				if (!ni.isExpanded())
				{
					// look for a black layer
					int i = 0;
					for( ; i<numLayers; i++)
						if (allLayers[i].r == 0 && allLayers[i].g == 0 &&
								allLayers[i].b == 0 && allLayers[i].opacity == 1) break;
					if (i < numLayers)
					{
						// draw a box by plotting 4 lines
						Rectangle2D niBounds = ni.getBounds();
						PsBox curBox = new PsBox();
						curBox.layer = i;
						curBox.visible = true;
						curBox.pos[0] = 1;
						curBox.pos[1] = niBounds.getHeight();
						curBox.pos[2] = niBounds.getMinX();
						curBox.pos[3] = niBounds.getMinY();
						curCell.boxes.add(curBox);

						curBox = new PsBox();
						curBox.layer = i;
						curBox.visible = true;
						curBox.pos[0] = niBounds.getWidth();
						curBox.pos[1] = 1;
						curBox.pos[2] = niBounds.getMinX();
						curBox.pos[3] = niBounds.getMinY();
						curCell.boxes.add(curBox);

						curBox = new PsBox();
						curBox.layer = i;
						curBox.visible = true;
						curBox.pos[0] = 1;
						curBox.pos[1] = niBounds.getHeight();
						curBox.pos[2] = niBounds.getMaxX();
						curBox.pos[3] = niBounds.getMinY();
						curCell.boxes.add(curBox);

						curBox = new PsBox();
						curBox.layer = i;
						curBox.visible = true;
						curBox.pos[0] = niBounds.getWidth();
						curBox.pos[1] = 1;
						curBox.pos[2] = niBounds.getMinX();
						curBox.pos[3] = niBounds.getMaxY();
						curCell.boxes.add(curBox);

						// add the cell name
						PsLabel curLabel = new PsLabel();
						curLabel.label = ni.getProto().describe(false);
						curLabel.pos[0] = niBounds.getMinX();
						curLabel.pos[1] = niBounds.getMaxX();
						curLabel.pos[2] = niBounds.getMinY();
						curLabel.pos[3] = niBounds.getMaxY();
						curLabel.style = Poly.Type.TEXTBOX;
						curLabel.descript = ni.getTextDescriptor(NodeInst.NODE_NAME);
						curCell.labels.add(curLabel);
					}
				} else
				{
					// expanded instance: make the invocation
					Cell npCell = (Cell)np;
					PsCell subCell = cellStructs.get(npCell);
					PsCellInst curInst = new PsCellInst();
					curInst.inst = subCell;
					newIdentityMatrix(curInst.transform);

					// account for instance position
					double [] transT = new double[9];
					newIdentityMatrix(transT);
					Rectangle2D cellBounds = npCell.getBounds();
					transT[2*3+0] -= cellBounds.getCenterX();
					transT[2*3+1] -= cellBounds.getCenterY();
					matrixMul(curInst.transform, curInst.transform, transT);

					// account for instance rotation
					double [] transR = new double[9];
					newIdentityMatrix(transR);
					Orientation o = ni.getOrient();
					double rotation = o.getCAngle() / 1800.0 * Math.PI;
					transR[0*3+0] = Math.cos(rotation); if (Math.abs(transR[0]) < SMALL_NUM) transR[0] = 0;
					transR[0*3+1] = Math.sin(rotation); if (Math.abs(transR[1]) < SMALL_NUM) transR[1] = 0;
					transR[1*3+0] = -Math.sin(rotation); if (Math.abs(transR[3]) < SMALL_NUM) transR[3] = 0;
					transR[1*3+1] = Math.cos(rotation); if (Math.abs(transR[4]) < SMALL_NUM) transR[4] = 0;

					matrixMul(curInst.transform, curInst.transform, transR);

					// account for instance transposition
					if (o.isCTranspose())
					{
						newIdentityMatrix(transR);
						transR[0*3+0] = 0;
						transR[1*3+1] = 0;
						transR[0*3+1] = -1;
						transR[1*3+0] = -1;
						matrixMul(curInst.transform, curInst.transform, transR);
					}

					// account for instance location
					newIdentityMatrix(transT);
					transT[2*3+0] = ni.getAnchorCenterX();
					transT[2*3+1] = ni.getAnchorCenterY();
					matrixMul(curInst.transform, curInst.transform, transT);
					curCell.inst.add(curInst);
				}
			} else
			{
				// primitive: generate its layers
				AffineTransform trans = ni.rotateOut();
				Poly [] polys = np.getTechnology().getShapeOfNode(ni);
				for(int i=0; i<polys.length; i++)
				{
					Poly poly = polys[i];
					poly.transform(trans);
					plotPolygon(poly, curCell);
				}
			}
		}

		// add geometry for all arcs
		for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = it.next();
			Poly [] polys = ai.getProto().getTechnology().getShapeOfArc(ai);
			for(int i=0; i<polys.length; i++)
			{
				Poly poly = polys[i];
				plotPolygon(poly, curCell);
			}
		}

		// add the name of all exports
		for(Iterator<Export> it = cell.getExports(); it.hasNext(); )
		{
			Export pp = it.next();
			PsLabel curLabel = new PsLabel();
			curLabel.label = pp.getName();
			Rectangle2D bounds = pp.getOriginalPort().getPoly().getBounds2D();
			curLabel.pos[0] = curLabel.pos[1] = bounds.getCenterX();
			curLabel.pos[2] = curLabel.pos[3] = bounds.getCenterY();
			curLabel.style = Poly.Type.TEXTCENT;
			curLabel.descript = pp.getTextDescriptor(Export.EXPORT_NAME);
			curCell.labels.add(curLabel);
		}
	}

	private void plotPolygon(Poly poly, PsCell curCell)
	{
		Layer layer = poly.getLayer();
		Technology tech = layer.getTechnology();
		if (tech == null) return;
		getLayerMap(tech);
		int j = 0;
		for( ; j<numLayers; j++)
			if (allLayers[j].layer == poly.getLayer()) break;
		if (j >= numLayers)
			return;
		Rectangle2D polyBox = poly.getBox();
		Poly.Type style = poly.getStyle();
		Point2D [] points = poly.getPoints();
		if (style == Poly.Type.FILLED)
		{
			if (polyBox != null)
			{
				PsBox curBox = new PsBox();
				curBox.layer = j;
				curBox.visible = true;
				curBox.pos[0] = polyBox.getWidth();
				curBox.pos[1] = polyBox.getHeight();
				curBox.pos[2] = polyBox.getCenterX();
				curBox.pos[3] = polyBox.getCenterY();
				curBox.pos[2] -= curBox.pos[0]/2; // adjust center x to left edge;
				curBox.pos[3] -= curBox.pos[1]/2; // adjust center y to bottom edge
				curCell.boxes.add(curBox);
			} else
			{
				PsPoly curPoly = new PsPoly();
				curPoly.layer = j;
				int numCoords = points.length * 2;
				curPoly.coords = new double[numCoords];
				for(int k=0; k<numCoords; k++)
				{
					if ((k%2) == 0) curPoly.coords[k] = points[k/2].getX(); else
						curPoly.coords[k] = points[k/2].getY();
				}
				curCell.polys.add(curPoly);
			}
		} else if (style == Poly.Type.CLOSED || style == Poly.Type.OPENEDT1 ||
			style == Poly.Type.OPENEDT2 || style == Poly.Type.OPENEDT3)
		{
			int type = 0;
			if (poly.getStyle() == Poly.Type.OPENEDT1) type = 1; else
				if (poly.getStyle() == Poly.Type.OPENEDT2) type = 2; else
					if (poly.getStyle() == Poly.Type.OPENEDT3) type = 3;
			for (int k = 1; k < points.length; k++)
				plotLine(j, points[k-1].getX(), points[k-1].getY(), points[k].getX(), points[k].getY(), type, curCell);
			if (poly.getStyle() == Poly.Type.CLOSED)
			{
				int k = points.length - 1;
				plotLine(j, points[k].getX(), points[k].getY(), points[0].getX(), points[0].getY(), type, curCell);
			}
		} else if (style == Poly.Type.VECTORS)
		{
			for(int k=0; k<points.length; k += 2)
				plotLine(j, points[k].getX(), points[k].getY(), points[k+1].getX(), points[k+1].getY(), 0, curCell);
		} else if (style == Poly.Type.CROSS || style == Poly.Type.BIGCROSS)
		{
			Rectangle2D bounds = poly.getBounds2D();
			double x = bounds.getCenterX();
			double y = bounds.getCenterY();
			plotLine(j, x-5, y, x+5, y, 0, curCell);
			plotLine(j, x, y+5, x, y-5, 0, curCell);
		} else if (style == Poly.Type.CROSSED)
		{
			Rectangle2D bounds = poly.getBounds2D();
			double lX = bounds.getMinX();
			double hX = bounds.getMaxX();
			double lY = bounds.getMinY();
			double hY = bounds.getMaxY();
			plotLine(j, lX, lY, lX, hY, 0, curCell);
			plotLine(j, lX, hY, hX, hY, 0, curCell);
			plotLine(j, hX, hY, hX, lY, 0, curCell);
			plotLine(j, hX, lY, lX, lY, 0, curCell);
			plotLine(j, hX, hY, lX, lY, 0, curCell);
			plotLine(j, hX, lY, lX, hY, 0, curCell);
		} else if (style == Poly.Type.DISC || style == Poly.Type.CIRCLE ||
			style == Poly.Type.THICKCIRCLE || style == Poly.Type.CIRCLEARC || style == Poly.Type.THICKCIRCLEARC)
		{
			if (!curveWarning)
				System.out.println("Warning: the 'merged color' PostScript option ignores curves.  Use other color options");
			curveWarning = true;
		} else if (style.isText())
		{
			PsLabel curLabel = new PsLabel();
			curLabel.label = poly.getString();
			Rectangle2D bounds = poly.getBounds2D();
			curLabel.pos[0] = bounds.getMinX();
			curLabel.pos[1] = bounds.getMaxX();
			curLabel.pos[2] = bounds.getMinY();
			curLabel.pos[3] = bounds.getMaxY();
			curLabel.style = poly.getStyle();
			curLabel.descript = poly.getTextDescriptor();
			curCell.labels.add(curLabel);
		}
	}

	/**
	 * Method to add a line from (fx,fy) to (tx,ty) on layer "layer" to the cell "curCell".
	 */
	private void plotLine(int layer, double fx, double fy, double tx, double ty, int texture, PsCell curCell)
	{
		PsPoly curPoly = new PsPoly();
		curPoly.layer = layer;
		curPoly.coords = new double[4];
		curPoly.coords[0] = fx;
		curPoly.coords[1] = fy;
		curPoly.coords[2] = tx;
		curPoly.coords[3] = ty;
		curCell.polys.add(curPoly);
	}

	private void genOverlapShapesAfterFlattening()
	{
		// traverse the list of boxes and create new layers where overlaps occur
		System.out.println("Generating overlap after flattening " + numLayers + " layers...");
		for (int i=0; i<numLayers; i++)
		{
			if (allLayers[i].mix1 != -1 && allLayers[i].mix2 != -1)
			{
				PxBoxQuadTree q1 = makeBoxQuadTree(allLayers[i].mix1);
				PxBoxQuadTree q2 = makeBoxQuadTree(allLayers[i].mix2);
				coaf1(q1, q2, i);
			}
		}
	}

	private PxBoxQuadTree makeBoxQuadTree(int layer)
	{
		// return if the quadtree is already generated
		if (quadTrees[layer] != null) return quadTrees[layer];

		// otherwise find number of boxes on this layer
		int numBoxes = 0;
		for(PsBox g : flattenedBoxes.get(layer))
		{
			if (g.visible) numBoxes++;
		}

		// and convert the list into a quad tree for faster processing

		// first allocate the quad tree
		quadTrees[layer] = new PxBoxQuadTree();
		quadTrees[layer].bounds[0] = psBoundaries[0];
		quadTrees[layer].bounds[1] = psBoundaries[1];
		quadTrees[layer].bounds[2] = psBoundaries[2];
		quadTrees[layer].bounds[3] = psBoundaries[3];
		quadTrees[layer].tl = null;
		quadTrees[layer].tr = null;
		quadTrees[layer].bl = null;
		quadTrees[layer].br = null;
		quadTrees[layer].numBoxes = numBoxes;
		quadTrees[layer].boxes = new PsBoxElement[numBoxes];
		quadTrees[layer].parent = null;
		quadTrees[layer].level = 0;

		// then copy the boxes into the tree
		int i = 0;
		for(PsBox g : flattenedBoxes.get(layer))
		{
			if (g.visible)
			{
				quadTrees[layer].boxes[i] = new PsBoxElement();
				quadTrees[layer].boxes[i].pos[0] = g.pos[0];
				quadTrees[layer].boxes[i].pos[1] = g.pos[1];
				quadTrees[layer].boxes[i].pos[2] = g.pos[2];
				quadTrees[layer].boxes[i].pos[3] = g.pos[3];
				quadTrees[layer].boxes[i].layer = g.layer;
				quadTrees[layer].boxes[i].visible = true;
				i++;
			}
		}

		// if there are too many boxes in this layer of the tree, create subtrees
		if (numBoxes > TREETHRESHOLD)
			recursivelyMakeBoxQuadTree(quadTrees[layer]);

		return quadTrees[layer];
	}

	private void recursivelyMakeBoxQuadTree(PxBoxQuadTree q)
	{
		q.tl = new PxBoxQuadTree();
		q.tl.parent = q; q.tl.level = q.level*10+1;
		q.tr = new PxBoxQuadTree();
		q.tr.parent = q; q.tr.level = q.level*10+2;
		q.bl = new PxBoxQuadTree();
		q.bl.parent = q; q.bl.level = q.level*10+3;
		q.br = new PxBoxQuadTree();
		q.br.parent = q; q.br.level = q.level*10+4;

		// split boxes into subtrees where they fit
		splitTree(q.numBoxes, q.boxes, q.bl, q.bounds[0], q.bounds[1],
			(q.bounds[0]+q.bounds[2])/2, (q.bounds[1]+q.bounds[3])/2);
		splitTree(q.numBoxes, q.boxes, q.br, (q.bounds[0]+q.bounds[2])/2, q.bounds[1],
			q.bounds[2], (q.bounds[1]+q.bounds[3])/2);
		splitTree(q.numBoxes, q.boxes, q.tl, q.bounds[0], (q.bounds[1]+q.bounds[3])/2,
			(q.bounds[0]+q.bounds[2])/2, q.bounds[3]);
		splitTree(q.numBoxes, q.boxes, q.tr, (q.bounds[0]+q.bounds[2])/2,
			(q.bounds[1]+q.bounds[3])/2, q.bounds[2], q.bounds[3]);

		// and leave boxes that span the subtrees at the top level
		int numBoxes = 0;
		for (int i=0; i<q.numBoxes; i++)
		{
			if (q.boxes[i].visible)
			{
				// q.boxes[numBoxes] = q.boxes[i];
				q.boxes[numBoxes].layer = q.boxes[i].layer;
				q.boxes[numBoxes].visible = true;
				q.boxes[numBoxes].pos[0] = q.boxes[i].pos[0];
				q.boxes[numBoxes].pos[1] = q.boxes[i].pos[1];
				q.boxes[numBoxes].pos[2] = q.boxes[i].pos[2];
				q.boxes[numBoxes].pos[3] = q.boxes[i].pos[3];
				numBoxes++;
			}
		}

		q.numBoxes = numBoxes;
	}

	private void splitTree(int numBoxes, PsBoxElement [] boxes, PxBoxQuadTree q,
		double left, double bot, double right, double top)
	{
		// count how many boxes are in subtree
		int count = 0;
		for (int i = 0; i<numBoxes; i++)
		{
			if (boxes[i].visible && boxes[i].pos[2] >= left && boxes[i].pos[3] >= bot &&
				(boxes[i].pos[2]+boxes[i].pos[0]) <= right && (boxes[i].pos[3]+boxes[i].pos[1]) <= top) count++;
		}

		// and copy them into a new array for the subtree
		q.boxes = new PsBoxElement[count];
		count = 0;
		for (int i=0; i<numBoxes; i++)
		{
			if (boxes[i].visible && boxes[i].pos[2] >= left && boxes[i].pos[3] >= bot &&
				(boxes[i].pos[2]+boxes[i].pos[0]) <= right && (boxes[i].pos[3]+boxes[i].pos[1]) <= top)
			{
				q.boxes[count] = new PsBoxElement();
				q.boxes[count].layer = boxes[i].layer;
				q.boxes[count].visible = true;
				q.boxes[count].pos[0] = boxes[i].pos[0];
				q.boxes[count].pos[1] = boxes[i].pos[1];
				q.boxes[count].pos[2] = boxes[i].pos[2];
				q.boxes[count].pos[3] = boxes[i].pos[3];
				boxes[i].visible = false; // mark box for removal from upper level
				count++;
			}
		}

		q.numBoxes = count;
		q.bounds[0] = left;
		q.bounds[1] = bot;
		q.bounds[2] = right;
		q.bounds[3] = top;
		q.tl = null;
		q.tr = null;
		q.bl = null;
		q.br = null;

		if (count > TREETHRESHOLD)
		{
			recursivelyMakeBoxQuadTree(q);
		}
	}

	private void mergeBoxes()
	{
		int numMerged = 0;

		System.out.println("Merging boxes for " + totalCells + " cells...");
		for(PsCell c : allCells)
		{
			boolean changed = false;
			do {
				changed = false;
				for(int i1 = 0; i1 < c.boxes.size(); i1++)
				{
					PsBox b1 = c.boxes.get(i1);
					for(int i2 = i1+1; i2 < c.boxes.size(); i2++)
					{
						PsBox b2 = c.boxes.get(i2);
						if (b1.layer == b2.layer && b1.visible && b2.visible)
							if (mergeBoxPair(b1, b2)) changed = true;
					}
				}
			} while (changed);
			numMerged++;
		}
	}

	private boolean mergeBoxPair(PsBox bx1, PsBox bx2)
	{
		double t0 = bx1.pos[3] + bx1.pos[1];
		double b0 = bx1.pos[3];
		double l0 = bx1.pos[2];
		double r0 = bx1.pos[2] + bx1.pos[0];
		double t1 = bx2.pos[3] + bx2.pos[1];
		double b1 = bx2.pos[3];
		double l1 = bx2.pos[2];
		double r1 = bx2.pos[2] + bx2.pos[0];

		// if the boxes coincide, expand the first one and hide the second one
		if (t0 == t1 && b0 == b1 && (Math.min(r0, r1) > Math.max(l0, l1)))
		{
			double l2 = Math.min(l0, l1);
			double r2 = Math.max(r0, r1);
			bx1.pos[0] = r2-l2;
			bx1.pos[1] = t0-b0;
			bx1.pos[2] = l2;
			bx1.pos[3] = b0;
			bx2.visible = false;
			return true;
		} else if (r0 == r1 && l0 == l1 && (Math.min(t0, t1) > Math.max(b0, b1)))
		{
			double b2 = Math.min(b0, b1);
			double t2 = Math.max(t0, t1);
			bx1.pos[0] = r0-l0;
			bx1.pos[1] = t2-b2;
			bx1.pos[2] = l0;
			bx1.pos[3] = b2;
			bx2.visible = false;
			return true;
		}
		// if one completely covers another, hide the covered box
		else if (r0 >= r1 && l0 <= l1 && t0 >= t1 && b0 <= b1)
		{
			bx2.visible = false;
			return true;
		} else if (r1 >= r0 && l1 <= l0 && t1 >= t0 && b1 <= b0)
		{
			bx1.visible = false;
			return true;
		}
		return false;
	}

	private void coaf1(PxBoxQuadTree q1, PxBoxQuadTree q2, int layerNum)
	{
		coaf2(q1, q2, layerNum);
		if (q1.tl != null)
		{
			coaf3(q1.tl, q2, layerNum);
			coaf3(q1.tr, q2, layerNum);
			coaf3(q1.bl, q2, layerNum);
			coaf3(q1.br, q2, layerNum);
			if (q2.tl != null)
			{
				coaf1(q1.tl, q2.tl, layerNum);
				coaf1(q1.tr, q2.tr, layerNum);
				coaf1(q1.bl, q2.bl, layerNum);
				coaf1(q1.br, q2.br, layerNum);
			} else
			{
				coaf4(q1.tl, q2, layerNum, false);
				coaf4(q1.tr, q2, layerNum, false);
				coaf4(q1.bl, q2, layerNum, false);
				coaf4(q1.br, q2, layerNum, false);
			}
		}
	}

	private void coaf2(PxBoxQuadTree q1, PxBoxQuadTree q2, int layerNum)
	{
		checkOverlapAfterFlattening(q1, q2, layerNum);
		if (q2.tl != null)
		{
			coaf2(q1, q2.tl, layerNum);
			coaf2(q1, q2.tr, layerNum);
			coaf2(q1, q2.bl, layerNum);
			coaf2(q1, q2.br, layerNum);
		}
	}

	private void coaf3(PxBoxQuadTree q1, PxBoxQuadTree q2, int layerNum)
	{
		checkOverlapAfterFlattening(q1, q2, layerNum);
		if (q2.parent != null)
			coaf3(q1, q2.parent, layerNum);
	}

	private void coaf4(PxBoxQuadTree q1, PxBoxQuadTree q2, int layerNum, boolean check)
	{
		if (check)
		{
			coaf3(q1, q2, layerNum);
		}
		if (q1.tl != null)
		{
			coaf4(q1.tl, q2, layerNum, true);
			coaf4(q1.tr, q2, layerNum, true);
			coaf4(q1.bl, q2, layerNum, true);
			coaf4(q1.br, q2, layerNum, true);
		}
	}

	private void checkOverlapAfterFlattening(PxBoxQuadTree q1, PxBoxQuadTree q2, int layerNum)
	{
		// check overlap of boxes at this level of the quad tree
		double [] t = new double[3];
		double [] b = new double[3];
		double [] l = new double[3];
		double [] r = new double[3];
		if (q1.numBoxes != 0 && q2.numBoxes != 0)
		{
			for (int j=0; j<q1.numBoxes; j++)
			{
				t[0] = q1.boxes[j].pos[3] + q1.boxes[j].pos[1];
				b[0] = q1.boxes[j].pos[3];
				l[0] = q1.boxes[j].pos[2];
				r[0] = q1.boxes[j].pos[2] + q1.boxes[j].pos[0];

				for (int k=0; k < q2.numBoxes; k++)
				{
					t[1] = q2.boxes[k].pos[3] + q2.boxes[k].pos[1];
					b[1] = q2.boxes[k].pos[3];
					l[1] = q2.boxes[k].pos[2];
					r[1] = q2.boxes[k].pos[2] + q2.boxes[k].pos[0];

					t[2] = t[0] < t[1] ? t[0] : t[1];
					b[2] = b[0] > b[1] ? b[0] : b[1];
					l[2] = l[0] > l[1] ? l[0] : l[1];
					r[2] = r[0] < r[1] ? r[0] : r[1];

					if (t[2] > b[2] && r[2] > l[2])
					{
						// create overlap layer
						PsBox curBox = new PsBox();
						curBox.layer = layerNum;
						curBox.pos[0] = (r[2]-l[2]);
						curBox.pos[1] = (t[2]-b[2]);
						curBox.pos[2] = (l[2]);
						curBox.pos[3] = (b[2]);
						curBox.visible = true;
						if (t[2] == t[0] && b[2] == b[0] && l[2] == l[0] && r[2] == r[0]) q1.boxes[j].visible = false;
						if (t[2] == t[1] && b[2] == b[1] && l[2] == l[1] && r[2] == r[1]) q2.boxes[k].visible = false;
						flattenedBoxes.get(layerNum).add(curBox);
					}
				}
			}
		}
	}

	private void flatten()
	{
		double [] ident = new double[9];
		newIdentityMatrix(ident);
		for (int i=0; i<numLayers; i++)
		{
			flattenedPolys.add(new ArrayList<PsPoly>());
			flattenedBoxes.add(new ArrayList<PsBox>());
		}

		// for now, assume last cell is top level.  Change this to recognize C *** at top of CIF
		System.out.println("Flattening...");
		int lastCell = allCells.size() - 1;
		PsCell topCell = allCells.get(lastCell);
		recursiveFlatten(topCell, ident);
	}

	private void recursiveFlatten(PsCell topCell, double [] m)
	{
		// add boxes from this cell
		for(PsBox box : topCell.boxes)
		{
			if (box.visible)
			{
				PsBox newBox = copyBox(box, m);
				flattenedBoxes.get(newBox.layer).add(newBox);
			}
		}

		// add polygons from this cell
		for(PsPoly poly : topCell.polys)
		{
			PsPoly newPoly = copyPoly(poly, m);
			flattenedPolys.get(newPoly.layer).add(newPoly);
		}

		// recursively traverse subinstances
		double [] tm = new double[9];
		for(PsCellInst inst : topCell.inst)
		{
			totalInstances++;
			matrixMul(tm, inst.transform, m);
			recursiveFlatten(inst.inst, tm);
		}
	}

	private void writePS(Cell cell, boolean usePlotter, double pageWidth, double pageHeight, double border)
	{
		// Header info
		PrintWriter printWriter = psObject.printWriter;
		printWriter.println("%%!PS-Adobe-1.0");
		printWriter.println("%%Title: " + cell.describe(false));
		if (User.isIncludeDateAndVersionInOutput())
		{
			printWriter.println("%%%%Creator: Electric VLSI Design System (David Harris's color PostScript generator) version " +
				Version.getVersion());
			printWriter.println("%%%%CreationDate: " + TextUtils.formatDate(new Date()));
		} else
		{
			printWriter.println("%%%%Creator: Electric VLSI Design System (David Harris's color PostScript generator)");
		}
		printWriter.println("%%%%Pages: 1");
		printWriter.println("%%%%BoundingBox: " + TextUtils.formatDouble(psBoundaries[0]) + " " +
			TextUtils.formatDouble(psBoundaries[1]) + " " +
			TextUtils.formatDouble(psBoundaries[2]) + " " +
			TextUtils.formatDouble(psBoundaries[3]));
		printWriter.println("%%%%DocumentFonts: " + FONTNAME);
		printWriter.println("%%%%EndComments");

		// Coordinate system
		printWriter.println("%% Min X: " + TextUtils.formatDouble(psBoundaries[0]) +
			"  min Y: " + TextUtils.formatDouble(psBoundaries[1]) +
			"  max X: " + TextUtils.formatDouble(psBoundaries[2]) +
			"  max Y: " + TextUtils.formatDouble(psBoundaries[3]));
		double reducedwidth = pageWidth - border*2;
		double reducedheight = pageHeight - border*2;
		double scale = reducedwidth/(psBoundaries[2]-psBoundaries[0]);
		if (usePlotter)
		{
			// plotter: infinite height
			printWriter.println(TextUtils.formatDouble(scale) + " " + TextUtils.formatDouble(scale) + " scale");
			double x = psBoundaries[0]+(psBoundaries[2]-psBoundaries[0]) / 2 -
				(reducedwidth/scale) / 2 - border/scale;
			double y = psBoundaries[1] - border/scale;
			printWriter.println(TextUtils.formatDouble(x) + " neg " + TextUtils.formatDouble(y) + " neg translate");
		} else
		{
			// printer: fixed height
			if (reducedheight/(psBoundaries[3]-psBoundaries[1]) < scale)
				scale = reducedheight/(psBoundaries[3]-psBoundaries[1]);
			printWriter.println(TextUtils.formatDouble(scale) + " " + TextUtils.formatDouble(scale) + " scale");
			double x = psBoundaries[0]+(psBoundaries[2]-psBoundaries[0]) / 2 -
				(reducedwidth/scale) / 2 - border/scale;
			double y = psBoundaries[1]+(psBoundaries[3]-psBoundaries[1]) / 2 -
				(reducedheight/scale) / 2 - border/scale;
			printWriter.println(TextUtils.formatDouble(x) + " neg " + TextUtils.formatDouble(y) + " neg translate");
		}

		// set font
		printWriter.println("/DefaultFont /" + FONTNAME + " def");
		printWriter.println("/scaleFont {");
		printWriter.println("    DefaultFont findfont");
		printWriter.println("    exch scalefont setfont} def");

		// define box command to make rectangles more memory efficient
		printWriter.println("\n/bx \n  { /h exch def /w exch def /x exch def /y exch def");
		printWriter.println("    newpath x y moveto w 0 rlineto 0 h rlineto w neg 0 rlineto closepath fill } def");

		// draw layers
		for (int i=0; i<numLayers; i++)
		{
			// skip drawing layers that are white
			if (allLayers[i].r != 1 || allLayers[i].g != 1 || allLayers[i].b != 1)
			{
				List<PsBox> g = flattenedBoxes.get(i);
				List<PsPoly> p = flattenedPolys.get(i);
				if (g.size() > 0 || p.size() > 0)
				{
					StringBuffer buf = new StringBuffer();
					makeLayerName(i, buf);
					printWriter.println();
					printWriter.println("%% Layer" + buf.toString());
					printWriter.println(TextUtils.formatDouble(allLayers[i].r) + " " +
						TextUtils.formatDouble(allLayers[i].g) + " " +
						TextUtils.formatDouble(allLayers[i].b) + " setrgbcolor");
				}
				for(PsBox gB : g)
				{
					if (gB.visible)
					{
						double w = gB.pos[0];
						double h = gB.pos[1];
						printWriter.println(gB.pos[3] + " " + gB.pos[2] + " " + w + " " + h + " bx");
						totalBoxes++;
					}
				}
				for(PsPoly pB : p)
				{
					if (pB.coords.length > 2)
					{
						printWriter.println("newpath " + TextUtils.formatDouble(pB.coords[0]) + " " +
							TextUtils.formatDouble(pB.coords[1]) + " moveto");
						for (int j=2; j<pB.coords.length; j+=2)
						{
							printWriter.println("        " + TextUtils.formatDouble(pB.coords[j]) + " " +
									TextUtils.formatDouble(pB.coords[j+1]) + " lineto");
						}
						printWriter.println("closepath " + (i==0 ? "stroke" : "fill"));
						totalPolys++;
					}
				}
			}
		}

		// label ports and cell instances
		PsCell topCell = allCells.get(0);
		printWriter.println();
		printWriter.println("%% Port and Cell Instance Labels");
		printWriter.println("0 0 0 setrgbcolor");
		for(int i=0; i<psObject.headerString.length; i++)
			printWriter.println(psObject.headerString[i]);
		for(PsLabel l : topCell.labels)
		{
			double size = 14;
			TextDescriptor.Size s = l.descript.getSize();
			if (s != null)
			{
				// absolute font sizes are easy
				if (s.isAbsolute()) size = s.getSize(); else
				{	
					// relative font: get size in grid units
					size = s.getSize();
				}
			}
			size *= User.getGlobalTextScale();
			double psLX = l.pos[0];   double psHX = l.pos[1];
			double psLY = l.pos[2];   double psHY = l.pos[3];
			if (l.style == Poly.Type.TEXTBOX)
			{
				printWriter.print(TextUtils.formatDouble((psLX+psHX)/2) + " " +
					TextUtils.formatDouble((psLY+psHY)/2) + " " +
					TextUtils.formatDouble(psHX-psLX) + " " +
					TextUtils.formatDouble(psHY-psLY) + " ");
				psObject.writePSString(l.label);
				printWriter.println(" " + TextUtils.formatDouble(size/scale) + " Boxstring");
			} else
			{
				double x = 0, y = 0;
				String opName = "";
				if (l.style == Poly.Type.TEXTCENT)
				{
					x = (psLX+psHX)/2;   y = (psLY+psHY)/2;
					opName = "Centerstring";
				} else if (l.style == Poly.Type.TEXTTOP)
				{
					x = (psLX+psHX)/2;   y = psHY;
					opName = "Topstring";
				} else if (l.style == Poly.Type.TEXTBOT)
				{
					x = (psLX+psHX)/2;   y = psLY;
					opName = "Botstring";
				} else if (l.style == Poly.Type.TEXTLEFT)
				{
					x = psLX;   y = (psLY+psHY)/2;
					opName = "Leftstring";
				} else if (l.style == Poly.Type.TEXTRIGHT)
				{
					x = psHX;   y = (psLY+psHY)/2;
					opName = "Rightstring";
				} else if (l.style == Poly.Type.TEXTTOPLEFT)
				{
					x = psLX;   y = psHY;
					opName = "Topleftstring";
				} else if (l.style == Poly.Type.TEXTTOPRIGHT)
				{
					x = psHX;   y = psHY;
					opName = "Toprightstring";
				} else if (l.style == Poly.Type.TEXTBOTLEFT)
				{
					x = psLX;   y = psLY;
					opName = "Botleftstring";
				} else if (l.style == Poly.Type.TEXTBOTRIGHT)
				{
					x = psHX;   y = psLY;
					opName = "Botrightstring";
				}
				double descenderOffset = size / 12;
				TextDescriptor.Rotation rot = l.descript.getRotation();
				if (rot == TextDescriptor.Rotation.ROT0) y += descenderOffset; else
					if (rot == TextDescriptor.Rotation.ROT90) x -= descenderOffset; else
						if (rot == TextDescriptor.Rotation.ROT180) y -= descenderOffset; else
							if (rot == TextDescriptor.Rotation.ROT270) x += descenderOffset;
				double xOff = x;   double yOff = y;
				if (rot != TextDescriptor.Rotation.ROT0)
				{
					if (rot == TextDescriptor.Rotation.ROT90 || rot == TextDescriptor.Rotation.ROT270)
					{
						if (l.style == Poly.Type.TEXTTOP) opName = "Rightstring"; else
							if (l.style == Poly.Type.TEXTBOT) opName = "Leftstring"; else
								if (l.style == Poly.Type.TEXTLEFT) opName = "Botstring"; else
									if (l.style == Poly.Type.TEXTRIGHT) opName = "Topstring"; else
										if (l.style == Poly.Type.TEXTTOPLEFT) opName = "Botrightstring"; else
											if (l.style == Poly.Type.TEXTBOTRIGHT) opName = "Topleftstring";
					}
					x = y = 0;
					if (rot == TextDescriptor.Rotation.ROT90)
					{
						printWriter.println(xOff + " " + yOff + " translate 90 rotate");
					} else if (rot == TextDescriptor.Rotation.ROT180)
					{
						printWriter.println(xOff + " " + yOff + " translate 180 rotate");
					} else if (rot == TextDescriptor.Rotation.ROT270)
					{
						printWriter.println(xOff + " " + yOff + " translate 270 rotate");
					}
				}
				printWriter.print(x + " " + y + " ");
				psObject.writePSString(l.label);
				printWriter.println(" " + size + " " + opName);
				if (rot != TextDescriptor.Rotation.ROT0)
				{
					if (rot == TextDescriptor.Rotation.ROT90)
					{
						printWriter.println("270 rotate " + (-xOff) + " " + (-yOff) + " translate");
					} else if (rot == TextDescriptor.Rotation.ROT180)
					{
						printWriter.println("180 rotate " + (-xOff) + " " + (-yOff) + " translate");
					} else if (rot == TextDescriptor.Rotation.ROT270)
					{
						printWriter.println("90 rotate " + (-xOff) + " " + (-yOff) + " translate");
					}
				}
			}
		}

		// Finish page
		printWriter.println("\nshowpage");
	}

	private void makeLayerName(int index, StringBuffer buf)
	{
		if (allLayers[index].layer != null)
		{
			buf.append(" ");
			buf.append(allLayers[index].layer.getName());
			return;
		}
		makeLayerName(allLayers[index].mix1, buf);
		makeLayerName(allLayers[index].mix2, buf);
	}

	private PsBox copyBox(PsBox g, double [] m)
	{
		PsBox newBox = new PsBox();
		newBox.layer = g.layer;
		transformBox(newBox.pos, g.pos, m);
		newBox.visible = g.visible;

		// Update bounding box
		double dx = newBox.pos[0];
		double dy = newBox.pos[1];
		if (newBox.pos[2] < psBoundaries[0]) psBoundaries[0] = newBox.pos[2];
		if (newBox.pos[3] < psBoundaries[1]) psBoundaries[1] = newBox.pos[3];
		if (newBox.pos[2]+dx > psBoundaries[2]) psBoundaries[2] = newBox.pos[2]+dx;
		if (newBox.pos[3]+dy > psBoundaries[3]) psBoundaries[3] = newBox.pos[3]+dy;
		return newBox;
	}

	private PsPoly copyPoly(PsPoly p, double [] m)
	{
		PsPoly newPoly = new PsPoly();
		newPoly.layer = p.layer;
		int numCoords = p.coords.length;
		newPoly.coords = new double[numCoords];
		transformPoly(newPoly, p, m);

		// Update bounding box
		for(int i=0; i<numCoords; i++)
		{
			if ((i%2) == 0)
			{
				if (newPoly.coords[i] < psBoundaries[0]) psBoundaries[0] = newPoly.coords[i];
				if (newPoly.coords[i] > psBoundaries[3]) psBoundaries[3] = newPoly.coords[i];
			} else
			{
				if (newPoly.coords[i] < psBoundaries[1]) psBoundaries[1] = newPoly.coords[i];
				if (newPoly.coords[i] > psBoundaries[2]) psBoundaries[2] = newPoly.coords[i];
			}
		}
		return newPoly;
	}

	private void matrixMul(double [] r, double [] a, double [] b)
	{
		double [] tmp = new double[9];

		for (int i=0; i<3; i++)
		{
			for (int j=0; j<3; j++)
			{
				tmp[i*3+j] = 0;
				for (int k=0; k<3; k++)
				{
					tmp[i*3+j] += a[i*3+k] * b[k*3+j];
				}
			}
		}
		for (int i=0; i<3; i++)
		{
			for (int j=0; j<3; j++)
			{
				r[i*3+j] = tmp[i*3+j];
			}
		}
	}

	private void newIdentityMatrix(double [] m)
	{
		for (int i=0; i<3; i++)
		{
			for (int j=0; j<3; j++)
			{
				m[i*3+j] = (i==j ? 1 : 0);
			}
		}
	}

	private void transformBox(double [] finall, double [] initial, double [] m)
	{
		double [] pos = new double[2];
		pos[0] = initial[2]+initial[0]/2;
		pos[1] = initial[3]+initial[1]/2;

		for (int i=0; i < 2; i++)
		{
			finall[i+2] = m[6+i];
			for (int j=0; j<2; j++)
			{
				finall[i+2] += m[i+j*3]*pos[j];
			}
		}

		if (m[1] == 0)
		{
			// rotation
			finall[0] = initial[0];
			finall[1] = initial[1];
		} else
		{
			finall[0] = initial[1];
			finall[1] = initial[0];
		}
		finall[2] -= finall[0]/2;
		finall[3] -= finall[1]/2;
	}

	private void transformPoly(PsPoly finall, PsPoly initial, double [] m)
	{
		for (int p=0; p<initial.coords.length/2; p++)
		{
			for (int i=0; i <2; i++)
			{
				finall.coords[p*2+i] = m[6+i];
				for (int j=0; j<2; j++)
				{
					finall.coords[p*2+i] += m[i+j*3]*initial.coords[p*2+j];
				}
			}
		}
	}

	private void printStatistics()
	{
		System.out.println("Plotting statistics:");
		System.out.println("  " + numLayers + " layers defined or transparencies implied in layer map");
		System.out.println("  " + totalCells + " cells");
		System.out.println("  " + totalInstances + " instances used");
		System.out.println("  " + totalBoxes + " boxes generated");
		System.out.println("  " + totalPolys + " polygons generated");
	}
}
