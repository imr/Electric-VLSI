/* -*- tab-width: 4 -*-
 * Electric(tm) VLSI Design System
 *
 * File: PostScriptColor.java
 * Input/output tool: PostScript color merged output
 * Written by: David Harris, 4/20/01 (David_Harris@hmc.edu)
 * Updated to speed up handling transparencies, 1/20/02 (David Harris)
 * Integrated into Electric: 12/05 (Steven Rubin)
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

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.MutableTextDescriptor;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.Job;

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
 *
 * This code generates postscript plots of an IC layout in CIF format.
 * It handles color better than existing freely available
 * postscript generators.  It does not handle arbitrary rotations.
 *
 * To use ICPLOT
 * look at the output with RoPS
 * To print from windows in E-sized format (34" wide) *** outdated
 * 	use RoPS to print to the plotter (C3PO)
 * 	Application Page Size: ANSI E
 * 	set the options to Color, then under Advanced to E-sized sheet
 * 	be sure plotter driver under Windows is set to E-size (a D-size driver also exists)
 *
 * updated 12/10/01 for qbert
 * 	use RoPS to print to the plotter (C3PO)
 * 	Application Page Size: ANSI A, fit to page ANSI E
 * 	Advanced tab: process document in computer
 * 	be sure plotter driver under Windows is set to E-size (a D-size driver also exists)
 *
 * Limitations:
 * the code to handle quad trees is rather messy now
 *
 * ideas:
 * 	draw highlights around edges
 * 	center port labels
 * 	give options about aspect ratio / page size
 * 	put date in postscript and on caption
 * 	print layers on caption
 *
 * To do 1/28/03:
 * 	draw outlines around edges
 * 	handle black & white mode
 * 	center labels
 *
 * Things still to do:
 *    circles and arcs
 *    rotation of the plot
 *    eps
 *    plot text (an option?)
 */
public class PostScriptColor
{
	// Constants
	private static final int MAXLAYERS = 1000;
//	#define TREETHRESHOLD 500
//	#define SMALL_NUM 0.000001
	
	// Structures
	
	private static class POLYL
	{
		int [] coords;
		int numcoords;
		int layer;
		POLYL next;
	};
	
//	private static class OUTLINEL {
//		OUTLINEL next;
//	};
	
	private static class BOXL
	{
		int [] pos = new int[4];  // 0: dx, 1: dy, 2: left, 3: bot
		int layer;
		boolean visible;
		BOXL next;
	};
	
//	private static class LINEL {
//		INTBIG x[2];
//		int y;
//		boolean visible;
//		LINEL next;
//	};
	
	private static class LABELL
	{
		String   label;
//		int []  pos[4];
		int  style;
//		UINTBIG descript[TEXTDESCRIPTSIZE];
		LABELL next;
	};
	
	private static class CELLL
	{
		int	cellNum;
		BOXL   box;
		POLYL  poly;
		LABELL label;
		CELLL  next;
		CELLINSTL inst;
	};
	
	private static class CELLINSTL
	{
//		float      transform[9];
		CELLL     inst;
		CELLINSTL next;
	};
	
	private static class BOXELEM
	{
		int [] pos = new int[4]; // 0: dx, 1: dy, 2: left, 3: bot
		int layer;
		boolean visible;
	};
	
	private static class BOXQUADTREE
	{
		int numBoxes;
		BOXELEM boxes;
		int [] bounds = new int[4]; // 0: dx, 1: dy, 2: left, 3: bot
		BOXQUADTREE tl;
		BOXQUADTREE tr;
		BOXQUADTREE bl;
		BOXQUADTREE br;
		BOXQUADTREE parent;
		int level;
	};
	
	private static class LAYERMAP
	{
//		int layernumber;
		Layer layer;
		Technology tech;
		int mix1, mix2; // layer made from mix of previous layers, or -1 if not
		double r, g, b;
		double opacity;
		boolean foreground;
	};
	
	private static class LAYERSORT
	{
		Layer layer;
		Technology tech;
		double height;
		int r, g, b;
		double  opacity;
		boolean foreground;
	};

	// Globals
	int []       io_pscolor_cifBoundaries = new int[4];
	List<CELLL>       io_pscolor_cells;
	LAYERMAP  []   io_pscolor_layers = new LAYERMAP[MAXLAYERS];
//	BOXL        *io_pscolor_flattenedbox[MAXLAYERS];
	BOXQUADTREE [] io_pscolor_boxQuadTrees = new BOXQUADTREE[MAXLAYERS];
//	POLYL       *io_pscolor_flattenedpoly[MAXLAYERS];
	int       io_pscolor_numLayers;
	int       io_pscolor_numBoxes = 0;
	int       io_pscolor_numCells = 0;
	int       io_pscolor_numPolys = 0;
	int       io_pscolor_numInstances = 0;
	int       io_pscolor_totalBoxes = 0;
	int       io_pscolor_cellnumber;
	boolean      io_pscolor_curvewarning;
	String         io_pscolor_font;
	HashSet<Technology>    techsSetup;
	HashMap<Cell,CELLL>    cellStructs;
	
	/**
	 * Main entry point for color PostScript output.  The cell being written is "cell".
	 * The size of the paper is "pageWid" wide and "pageHei" tall with a margin of
	 * "pageMargin" (in 1/75 of an inch).  If "usePlotter" is TRUE, this is an infinitely-tall
	 * plotter, so height is not a consideration.  If "epsformat" is TRUE, write encapsulated
	 * PostScript.
	 */
	public static void psColorPlot(PrintWriter printWriter, Cell cell, boolean epsFormat, boolean usePlotter,
		double pageWid, double pageHei, double pageMargin)
	{
		PostScriptColor psc = new PostScriptColor();
		psc.doPrinting(printWriter, cell, epsFormat, usePlotter, pageWid, pageHei, pageMargin);
	}

	public void doPrinting(PrintWriter printWriter, Cell cell, boolean epsFormat, boolean usePlotter,
		double pageWid, double pageHei, double pageMargin)
	{
		io_pscolor_font = "Helvetica";
	
		io_pscolor_initICPlot();
	
		// initialize layer maps for the current technology
		io_pscolor_numLayers = 0;
		techsSetup = new HashSet<Technology>();
		// mark all cells as "not written"
		cellStructs = new HashMap<Cell,CELLL>();
		io_pscolor_getLayerMap(Technology.getCurrent());
	
		io_pscolor_curvewarning = false;
		io_pscolor_cells = new ArrayList<CELLL>();
//		io_pscolor_extractDatabase(cell);
		io_pscolor_mergeBoxes(); 
//		io_pscolor_flatten();
//		io_pscolor_genOverlapShapesAfterFlattening();
//		io_pscolor_writePS(cell, usePlotter, pageWid, pageHei, pageMargin);
//		io_pscolor_printStatistics();
	}
	
	void io_pscolor_initICPlot()
	{	
		io_pscolor_numBoxes = io_pscolor_numCells = io_pscolor_numPolys =
			io_pscolor_totalBoxes = io_pscolor_numInstances = 0;
		io_pscolor_cifBoundaries[0] = 1<<30;
		io_pscolor_cifBoundaries[1] = 1<<30;
		io_pscolor_cifBoundaries[2] = -1<<30;
		io_pscolor_cifBoundaries[3] = -1<<30;
	
		for (int i=0; i<MAXLAYERS; i++) {
			io_pscolor_boxQuadTrees[i] = null;
		}

		io_pscolor_cellnumber = 1;
	}
	
	/**
	 * Method to get the print colors and load them into the layer map.
	 */
	void io_pscolor_getLayerMap(Technology tech)
	{	
		// see if this technology has already been done
		if (techsSetup.contains(tech)) return;
		techsSetup.add(tech);
	
		// read layer map
		int startlayer = io_pscolor_numLayers;
		List<LAYERSORT> layerSorts = new ArrayList<LAYERSORT>();
		for(int i=0; i<tech.getNumLayers(); i++)
		{
			Layer layer = tech.getLayer(i);
			Layer.Function fun = layer.getFunction();
			if ((layer.getFunctionExtras()&Layer.Function.PSEUDO) != 0) continue;
			LAYERSORT ls = new LAYERSORT();
			ls.height = layer.getDepth();
			double thickness = layer.getThickness();
			ls.layer = layer;
			ls.tech = tech;
			Color col = layer.getGraphics().getColor();
			ls.r = col.getRed();
			ls.g = col.getGreen();
			ls.b = col.getBlue();
			ls.opacity = layer.getGraphics().getOpacity();
			ls.foreground = layer.getGraphics().getForeground();
			layerSorts.add(ls);
		}
	
		// sort by layer height
		Collections.sort(layerSorts, new LayersByHeight());
	
		// load the layer information
		for(int i=0; i<layerSorts.size(); i++)
		{
			if (io_pscolor_numLayers >= MAXLAYERS)
			{
				System.out.println("More than " + MAXLAYERS + " layers");
				break;
			}
			LAYERSORT ls = layerSorts.get(i);
			io_pscolor_layers[io_pscolor_numLayers].layer = ls.layer;
			io_pscolor_layers[io_pscolor_numLayers].tech = ls.tech;
			io_pscolor_layers[io_pscolor_numLayers].opacity = ls.opacity;
			io_pscolor_layers[io_pscolor_numLayers].foreground = ls.foreground;
			io_pscolor_layers[io_pscolor_numLayers].r = ls.r/255.0f;
			io_pscolor_layers[io_pscolor_numLayers].g = ls.g/255.0f;
			io_pscolor_layers[io_pscolor_numLayers].b = ls.b/255.0f;
			io_pscolor_layers[io_pscolor_numLayers].mix1 = -1;
			io_pscolor_layers[io_pscolor_numLayers].mix2 = -1;
			if (io_pscolor_layers[io_pscolor_numLayers].opacity < 1)
			{
				// create new layers to provide transparency
				int curLayer = io_pscolor_numLayers;
				for (int k=startlayer; k < curLayer; k++)
				{
					if (io_pscolor_layers[k].foreground)
					{
						io_pscolor_layers[++io_pscolor_numLayers].opacity = 1;
						io_pscolor_layers[io_pscolor_numLayers].layer = null;
//						io_pscolor_layers[io_pscolor_numLayers].layernumber = io_pscolor_layers[k].layernumber + 1000*io_pscolor_layers[curLayer].layernumber+1000000;
						io_pscolor_layers[io_pscolor_numLayers].tech = io_pscolor_layers[k].tech;
						io_pscolor_layers[io_pscolor_numLayers].foreground = true;
						io_pscolor_layers[io_pscolor_numLayers].r = io_pscolor_layers[curLayer].r*io_pscolor_layers[curLayer].opacity +
							io_pscolor_layers[k].r*(1-io_pscolor_layers[curLayer].opacity);
						io_pscolor_layers[io_pscolor_numLayers].g = io_pscolor_layers[curLayer].g*io_pscolor_layers[curLayer].opacity +
							io_pscolor_layers[k].g*(1-io_pscolor_layers[curLayer].opacity);
						io_pscolor_layers[io_pscolor_numLayers].b = io_pscolor_layers[curLayer].b*io_pscolor_layers[curLayer].opacity +
							io_pscolor_layers[k].b*(1-io_pscolor_layers[curLayer].opacity);
						io_pscolor_layers[io_pscolor_numLayers].mix1 = k;
						io_pscolor_layers[io_pscolor_numLayers].mix2 = curLayer;
					}
				}
			}
			io_pscolor_numLayers++;
		}
	}

	/**
	 * Comparator class for sorting LAYERSORT by their height.
	 */
    public static class LayersByHeight implements Comparator<LAYERSORT>
    {
		/**
		 * Method to sort LAYERSORT by their height.
		 */
        public int compare(LAYERSORT ls1, LAYERSORT ls2)
        {
    		double diff = ls1.height - ls2.height;
    		if (diff == 0.0) return 0;
    		if (diff < 0.0) return -1;
    		return 1;
        }
    }
	
	void io_pscolor_extractDatabase(Cell cell)
	{
//		REGISTER NODEINST *ni;
//		REGISTER NODEPROTO *np;
//		REGISTER ARCINST *ai;
//		REGISTER PORTPROTO *pp;
//		CELLL *curCell, *subCell;
//		CELLINSTL *curInst;
//		BOXL *curbox;
//		float transt[9], transr[9];
//		LABELL *curlabel;
//		INTBIG i;
//		REGISTER INTBIG tot;
//		INTBIG xp, yp;
//		static POLYGON *poly = NOPOLYGON;
//		XARRAY trans;
//	
		// check for subcells that haven't been written yet
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			NodeProto np = ni.getProto();
			if (!(np instanceof Cell)) continue;
			if (!ni.isExpanded()) continue;
			Cell subCell = (Cell)np;
			if (cellStructs.get(subCell) != null) continue;
			io_pscolor_extractDatabase(subCell);
		}
	
		// create a cell
		CELLL curCell = new CELLL();
		curCell.cellNum = io_pscolor_cellnumber++;
		curCell.box = null;
		curCell.poly = null;
		curCell.label = null;
		curCell.inst = null;
		curCell.next = null;
		io_pscolor_numCells++;

		// add to the lists
		io_pscolor_cells.add(curCell);
		cellStructs.put(cell, curCell);
	
		// examine all nodes in the cell
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			NodeProto np = ni.getProto();
			if (np instanceof Cell)
			{
				// instance
				if (!ni.isExpanded())
				{
//					// look for a black layer
//					for(i=0; i<io_pscolor_numLayers; i++)
//						if (io_pscolor_layers[i].r == 0 && io_pscolor_layers[i].g == 0 &&
//							io_pscolor_layers[i].b == 0 && io_pscolor_layers[i].opacity == 1) break;
//					if (i < io_pscolor_numLayers)
//					{
//						// draw a box by plotting 4 lines
//						curbox = (BOXL *)emalloc(sizeof(BOXL), io_tool.cluster);
//						curbox.layer = (INTSML)i;
//						curbox.next = null;
//						curbox.visible = true;
//						curbox.pos[0] = 1;
//						curbox.pos[1] = ni.geom.highy - ni.geom.lowy;
//						curbox.pos[2] = ni.geom.lowx;
//						curbox.pos[3] = ni.geom.lowy;
//						curCell.box = io_pscolor_insertbox(curbox, curCell.box);
//	
//						curbox = (BOXL *)emalloc(sizeof(BOXL), io_tool.cluster);
//						curbox.layer = (INTSML)i;
//						curbox.next = null;
//						curbox.visible = true;
//						curbox.pos[0] = ni.geom.highx - ni.geom.lowx;
//						curbox.pos[1] = 1;
//						curbox.pos[2] = ni.geom.lowx;
//						curbox.pos[3] = ni.geom.lowy;
//						curCell.box = io_pscolor_insertbox(curbox, curCell.box);
//	
//						curbox = (BOXL *)emalloc(sizeof(BOXL), io_tool.cluster);
//						curbox.layer = (INTSML)i;
//						curbox.next = null;
//						curbox.visible = true;
//						curbox.pos[0] = 1;
//						curbox.pos[1] = ni.geom.highy - ni.geom.lowy;
//						curbox.pos[2] = ni.geom.highx;
//						curbox.pos[3] = ni.geom.lowy;
//						curCell.box = io_pscolor_insertbox(curbox, curCell.box);
//	
//						curbox = (BOXL *)emalloc(sizeof(BOXL), io_tool.cluster);
//						curbox.layer = (INTSML)i;
//						curbox.next = null;
//						curbox.visible = true;
//						curbox.pos[0] = ni.geom.highx - ni.geom.lowx;
//						curbox.pos[1] = 1;
//						curbox.pos[2] = ni.geom.lowx;
//						curbox.pos[3] = ni.geom.highy;
//						curCell.box = io_pscolor_insertbox(curbox, curCell.box);
//	
//						// add the cell name
//						curlabel = (LABELL *)emalloc(sizeof(LABELL), io_tool.cluster);
//						(void)allocstring(&curlabel.label, describenodeproto(ni.proto), io_tool.cluster);
//						curlabel.pos[0] = ni.lowx;
//						curlabel.pos[1] = ni.highx;
//						curlabel.pos[2] = ni.lowy;
//						curlabel.pos[3] = ni.highy;
//						curlabel.style = TEXTBOX;
//						TDCOPY(curlabel.descript, ni.textdescript);
//						curCell.label = io_pscolor_insertlabel(curlabel, curCell.label);			
//					}
				} else
				{
//					// expanded instance: make the invocation
//					subCell = cellStructs.get(np);
//					curInst = (CELLINSTL *)emalloc(sizeof(CELLINSTL), io_tool.cluster);
//					curInst.next = null;
//					curInst.inst = subCell;
//					io_pscolor_newIdentityMatrix(curInst.transform);
//	
//					// account for instance position
//					io_pscolor_newIdentityMatrix(transt);
//					transt[2*3+0] -= (float)((np.lowx + np.highx) / 2);
//					transt[2*3+1] -= (float)((np.lowy + np.highy) / 2);
//					io_pscolor_matrixMul(curInst.transform, curInst.transform, transt);
//	
//					// account for instance rotation
//					io_pscolor_newIdentityMatrix(transr);
//					float rotation;
//					rotation = (float)(ni.rotation / 1800.0 * EPI);
//					transr[0*3+0] = (float)cos(rotation); if (fabs(transr[0]) < SMALL_NUM) transr[0] = 0;
//					transr[0*3+1] = (float)sin(rotation); if (fabs(transr[1]) < SMALL_NUM) transr[1] = 0;
//					transr[1*3+0] = -(float)sin(rotation); if (fabs(transr[3]) < SMALL_NUM) transr[3] = 0;
//					transr[1*3+1] = (float)cos(rotation); if (fabs(transr[4]) < SMALL_NUM) transr[4] = 0;
//					
//					io_pscolor_matrixMul(curInst.transform, curInst.transform, transr);
//	
//					// account for instance transposition
//					if (ni.transpose != 0)
//					{
//						io_pscolor_newIdentityMatrix(transr);
//						transr[0*3+0] = 0;
//						transr[1*3+1] = 0;
//						transr[0*3+1] = -1;
//						transr[1*3+0] = -1;
//						io_pscolor_matrixMul(curInst.transform, curInst.transform, transr);
//					}
//	
//					// account for instance location
//					io_pscolor_newIdentityMatrix(transt);
//					transt[2*3+0] = (float)((ni.lowx + ni.highx) / 2);
//					transt[2*3+1] = (float)((ni.lowy + ni.highy) / 2);
//					io_pscolor_matrixMul(curInst.transform, curInst.transform, transt);
//					curCell.inst = io_pscolor_insertInst(curInst, curCell.inst);
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
					io_pscolor_plotPolygon(poly, curCell);
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
				io_pscolor_plotPolygon(poly, curCell);
			}
		}
	
//		// add the name of all exports
//		for(pp = cell.firstportproto; pp != NOPORTPROTO; pp = pp.nextportproto)
//		{
//			curlabel = (LABELL *)emalloc(sizeof(LABELL), io_tool.cluster);
//			(void)allocstring(&curlabel.label, pp.protoname, io_tool.cluster);
//			portposition(pp.subnodeinst, pp.subportproto, &xp, &yp);
//			curlabel.pos[0] = curlabel.pos[1] = xp;
//			curlabel.pos[2] = curlabel.pos[3] = yp;
//			curlabel.style = TEXTCENT;
//			TDCOPY(curlabel.descript, pp.textdescript);
//			curCell.label = io_pscolor_insertlabel(curlabel, curCell.label);			
//		}
	}
	
	void io_pscolor_plotPolygon(Poly poly, CELLL curCell)
	{
//		BOXL *curbox;
//		POLYL *curpoly;
//		LABELL *curlabel;
//		REGISTER INTBIG j, k, type;
//		INTBIG lx, hx, ly, hy, x, y;
//		REGISTER BOOLEAN isabox;

		Layer layer = poly.getLayer();
		Technology tech = layer.getTechnology();
		if (tech == null) return;
		io_pscolor_getLayerMap(tech);
		int j = 0;
		for( ; j<io_pscolor_numLayers; j++)
			if (io_pscolor_layers[j].layer == poly.getLayer()) break;
		if (j >= io_pscolor_numLayers)
			return;
		Rectangle2D polyBox = poly.getBox();
//		isabox = isbox(poly, &lx, &hx, &ly, &hy);
//		if (!isabox) getbbox(poly, &lx, &hx, &ly, &hy);
//	
		Poly.Type style = poly.getStyle();
		Point2D [] points = poly.getPoints();
		if (style == Poly.Type.FILLED)
		{
//				if (isabox)
//				{
//					curbox = (BOXL *)emalloc(sizeof(BOXL), io_tool.cluster);
//					curbox.layer = (INTSML)j;
//					curbox.next = null;
//					curbox.visible = true;
//					curbox.pos[0] = hx-lx;
//					curbox.pos[1] = hy-ly;
//					curbox.pos[2] = (lx+hx)/2;
//					curbox.pos[3] = (ly+hy)/2;
//					curbox.pos[2] -= curbox.pos[0]/2; // adjust center x to left edge;
//					curbox.pos[3] -= curbox.pos[1]/2; // adjust center y to bottom edge
//					curCell.box = io_pscolor_insertbox(curbox, curCell.box);
//				} else
//				{
//					curpoly = (POLYL *)emalloc(sizeof(POLYL), io_tool.cluster);
//					if (curpoly == 0) return;
//					curpoly.layer = (INTSML)j;
//					curpoly.next = null;
//					curpoly.numcoords = poly.count*2;
//					curpoly.coords = (INTBIG *)emalloc(curpoly.numcoords * SIZEOFINTBIG, io_tool.cluster);
//					if (curpoly.coords == 0) return;
//					for(j=0; j<curpoly.numcoords; j++)
//					{
//						if ((j%2) == 0) curpoly.coords[j] = poly.xv[j/2]; else
//							curpoly.coords[j] = poly.yv[j/2];
//					}
//					curCell.poly = io_pscolor_insertpoly(curpoly, curCell.poly);			
//				}
		} else if (style == Poly.Type.CLOSED)
		{
//				if (isabox)
//				{
//					io_pscolor_plotline(j, lx, ly, lx, hy, 0, curCell);
//					io_pscolor_plotline(j, lx, hy, hx, hy, 0, curCell);
//					io_pscolor_plotline(j, hx, hy, hx, ly, 0, curCell);
//					io_pscolor_plotline(j, hx, ly, lx, ly, 0, curCell);
//					break;
//				}
				// FALLTHROUGH!!!!!!
	
		} else if (style == Poly.Type.CLOSED || style == Poly.Type.OPENEDT1 ||
			style == Poly.Type.OPENEDT2 || style == Poly.Type.OPENEDT3)
		{
//				switch (poly.style)
//				{
//					case OPENEDT1: type = 1; break;
//					case OPENEDT2: type = 2; break;
//					case OPENEDT3: type = 3; break;
//					default:       type = 0; break;
//				}
//				for (k = 1; k < poly.count; k++)
//					io_pscolor_plotline(j, poly.xv[k-1], poly.yv[k-1], poly.xv[k], poly.yv[k], type, curCell);
//				if (poly.style == CLOSED)
//				{
//					k = poly.count - 1;
//					io_pscolor_plotline(j, poly.xv[k], poly.yv[k], poly.xv[0], poly.yv[0], type, curCell);
//				}
		} else if (style == Poly.Type.VECTORS)
		{
//			case VECTORS:
//				for(k=0; k<poly.count; k += 2)
//					io_pscolor_plotline(j, poly.xv[k], poly.yv[k], poly.xv[k+1], poly.yv[k+1], 0, curCell);
		} else if (style == Poly.Type.CROSS || style == Poly.Type.BIGCROSS)
		{
//			case CROSS:
//			case BIGCROSS:
//				getcenter(poly, &x, &y);
//				io_pscolor_plotline(j, x-5, y, x+5, y, 0, curCell);
//				io_pscolor_plotline(j, x, y+5, x, y-5, 0, curCell);
		} else if (style == Poly.Type.CROSSED)
		{
//			case CROSSED:
//				io_pscolor_plotline(j, lx, ly, lx, hy, 0, curCell);
//				io_pscolor_plotline(j, lx, hy, hx, hy, 0, curCell);
//				io_pscolor_plotline(j, hx, hy, hx, ly, 0, curCell);
//				io_pscolor_plotline(j, hx, ly, lx, ly, 0, curCell);
//				io_pscolor_plotline(j, hx, hy, lx, ly, 0, curCell);
//				io_pscolor_plotline(j, hx, ly, lx, hy, 0, curCell);
		} else if (style == Poly.Type.DISC || style == Poly.Type.CIRCLE ||
			style == Poly.Type.THICKCIRCLE || style == Poly.Type.CIRCLEARC || style == Poly.Type.THICKCIRCLEARC)
		{
//				if (!io_pscolor_curvewarning)
//					ttyputmsg(_("Warning: the 'merged color' PostScript option ignores curves.  Use other color options"));
//				io_pscolor_curvewarning = TRUE;
		} else if (style.isText())
		{
//				curlabel = (LABELL *)emalloc(sizeof(LABELL), io_tool.cluster);
//				(void)allocstring(&curlabel.label, poly.string, io_tool.cluster);
//				curlabel.pos[0] = lx;
//				curlabel.pos[1] = hx;
//				curlabel.pos[2] = ly;
//				curlabel.pos[3] = hy;
//				curlabel.style = poly.style;
//				TDCOPY(curlabel.descript, poly.textdescript);
//				curCell.label = io_pscolor_insertlabel(curlabel, curCell.label);
		}
	}
	
	/*
	 * Routine to add a line from (fx,fy) to (tx,ty) on layer "layer" to the cell "curCell".
	 */
//	void io_pscolor_plotline(INTBIG layer, INTBIG fx, INTBIG fy, INTBIG tx, INTBIG ty, INTBIG texture, CELLL *curCell)
//	{
//		POLYL *curpoly;
//	
//		curpoly = (POLYL *)emalloc(sizeof(POLYL), io_tool.cluster);
//		if (curpoly == 0) return;
//		curpoly.layer = (INTSML)layer;
//		curpoly.next = null;
//		curpoly.numcoords = 4;
//		curpoly.coords = (INTBIG *)emalloc(4 * SIZEOFINTBIG, io_tool.cluster);
//		if (curpoly.coords == 0) return;
//		curpoly.coords[0] = fx;
//		curpoly.coords[1] = fy;
//		curpoly.coords[2] = tx;
//		curpoly.coords[3] = ty;
//		curCell.poly = io_pscolor_insertpoly(curpoly, curCell.poly);
//	}
	
//	void io_pscolor_genOverlapShapesAfterFlattening(void)
//	{
//		BOXQUADTREE *q1, *q2;
//		INTBIG i;
//	
//		// traverse the list of boxes and create new layers where overlaps occur
//		ttyputmsg(_("Generating overlap after flattening for %ld layers..."), io_pscolor_numLayers);
//		for (i=0; i<io_pscolor_numLayers; i++) {
//			if (io_pscolor_layers[i].mix1 != -1 && io_pscolor_layers[i].mix2 != -1) {
//				q1 = io_pscolor_makeBoxQuadTree(io_pscolor_layers[i].mix1);
//				q2 = io_pscolor_makeBoxQuadTree(io_pscolor_layers[i].mix2);
//				io_pscolor_coaf1(q1, q2, i);
//			}
//		}
//	}
	
//	BOXQUADTREE *io_pscolor_makeBoxQuadTree(INTBIG layer)
//	{
//		BOXL *g;
//		INTBIG i, numBoxes = 0;
//	
//		// return if the quadtree is already generated
//		if (io_pscolor_boxQuadTrees[layer] != null) return io_pscolor_boxQuadTrees[layer];
//	
//		// otherwise find number of boxes on this layer
//		g = io_pscolor_flattenedbox[layer];
//		while (g != null) {
//			if (g.visible) numBoxes++;
//			g = g.next;
//		}
//	
//		// and convert the list into a quad tree for faster processing
//	
//		// first allocate the quad tree
//		io_pscolor_boxQuadTrees[layer] = (BOXQUADTREE *)emalloc(sizeof(BOXQUADTREE), io_tool.cluster);
//		io_pscolor_boxQuadTrees[layer].bounds[0] = io_pscolor_cifBoundaries[0];
//		io_pscolor_boxQuadTrees[layer].bounds[1] = io_pscolor_cifBoundaries[1];
//		io_pscolor_boxQuadTrees[layer].bounds[2] = io_pscolor_cifBoundaries[2];
//		io_pscolor_boxQuadTrees[layer].bounds[3] = io_pscolor_cifBoundaries[3];
//		io_pscolor_boxQuadTrees[layer].tl = null;
//		io_pscolor_boxQuadTrees[layer].tr = null;
//		io_pscolor_boxQuadTrees[layer].bl = null;
//		io_pscolor_boxQuadTrees[layer].br = null;
//		io_pscolor_boxQuadTrees[layer].numBoxes = numBoxes;
//		io_pscolor_boxQuadTrees[layer].boxes = (BOXELEM *)emalloc(numBoxes*sizeof(BOXELEM), io_tool.cluster);
//		io_pscolor_boxQuadTrees[layer].parent = null;
//		io_pscolor_boxQuadTrees[layer].level = 0;
//	
//		// then copy the boxes into the tree
//		g = io_pscolor_flattenedbox[layer];
//		i = 0;
//		while (g != null) {
//			if (g.visible) {
//				io_pscolor_boxQuadTrees[layer].boxes[i].pos[0] = g.pos[0];
//				io_pscolor_boxQuadTrees[layer].boxes[i].pos[1] = g.pos[1];
//				io_pscolor_boxQuadTrees[layer].boxes[i].pos[2] = g.pos[2];
//				io_pscolor_boxQuadTrees[layer].boxes[i].pos[3] = g.pos[3];
//				io_pscolor_boxQuadTrees[layer].boxes[i].layer = g.layer;
//				io_pscolor_boxQuadTrees[layer].boxes[i].visible = true;
//				i++;
//			}
//			g = g.next;
//		}
//	
//		// if there are too many boxes in this layer of the tree, create subtrees
//		if (numBoxes > TREETHRESHOLD) 
//			io_pscolor_recursivelyMakeBoxQuadTree(io_pscolor_boxQuadTrees[layer]);
//		
//		return io_pscolor_boxQuadTrees[layer];
//	}
	
//	void io_pscolor_recursivelyMakeBoxQuadTree(BOXQUADTREE *q)
//	{
//		INTBIG i, numBoxes;
//	
//		q.tl = (BOXQUADTREE *)emalloc(sizeof(BOXQUADTREE), io_tool.cluster);
//		q.tl.parent = q; q.tl.level = q.level*10+1;
//		q.tr = (BOXQUADTREE *)emalloc(sizeof(BOXQUADTREE), io_tool.cluster);
//		q.tr.parent = q; q.tr.level = q.level*10+2;
//		q.bl = (BOXQUADTREE *)emalloc(sizeof(BOXQUADTREE), io_tool.cluster);
//		q.bl.parent = q; q.bl.level = q.level*10+3;
//		q.br = (BOXQUADTREE *)emalloc(sizeof(BOXQUADTREE), io_tool.cluster);
//		q.br.parent = q; q.br.level = q.level*10+4;
//	
//		// split boxes into subtrees where they fit
//		io_pscolor_splitTree(q.numBoxes, q.boxes, q.bl, q.bounds[0], q.bounds[1],
//			(q.bounds[0]+q.bounds[2])/2, (q.bounds[1]+q.bounds[3])/2);
//		io_pscolor_splitTree(q.numBoxes, q.boxes, q.br, (q.bounds[0]+q.bounds[2])/2, q.bounds[1],
//			q.bounds[2], (q.bounds[1]+q.bounds[3])/2);
//		io_pscolor_splitTree(q.numBoxes, q.boxes, q.tl, q.bounds[0], (q.bounds[1]+q.bounds[3])/2,
//			(q.bounds[0]+q.bounds[2])/2, q.bounds[3]);
//		io_pscolor_splitTree(q.numBoxes, q.boxes, q.tr, (q.bounds[0]+q.bounds[2])/2,
//			(q.bounds[1]+q.bounds[3])/2, q.bounds[2], q.bounds[3]);
//	
//		// and leave boxes that span the subtrees at the top level
//		numBoxes = 0;
//		for (i=0; i<q.numBoxes; i++) {
//			if (q.boxes[i].visible) {
//				// q.boxes[numBoxes] = q.boxes[i];
//				q.boxes[numBoxes].layer = q.boxes[i].layer;
//				q.boxes[numBoxes].visible = true;
//				q.boxes[numBoxes].pos[0] = q.boxes[i].pos[0];
//				q.boxes[numBoxes].pos[1] = q.boxes[i].pos[1];
//				q.boxes[numBoxes].pos[2] = q.boxes[i].pos[2];
//				q.boxes[numBoxes].pos[3] = q.boxes[i].pos[3];
//				numBoxes++;
//			}
//		}
//	
//		q.numBoxes = numBoxes;
//	}
	
//	void io_pscolor_splitTree(INTBIG numBoxes, BOXELEM *boxes, BOXQUADTREE *q,
//							  INTBIG left, INTBIG bot, INTBIG right, INTBIG top)
//	{
//		INTBIG i;
//		INTBIG count = 0;
//	
//		// count how many boxes are in subtree
//		for (i = 0; i<numBoxes; i++) {
//			if (boxes[i].visible && boxes[i].pos[2] >= left && boxes[i].pos[3] >= bot && 
//				(boxes[i].pos[2]+boxes[i].pos[0]) <= right && (boxes[i].pos[3]+boxes[i].pos[1]) <= top) count++;
//		}
//		// and copy them into a new array for the subtree
//		q.boxes = (BOXELEM *)emalloc(count*sizeof(BOXELEM), io_tool.cluster);
//		count = 0;
//		for (i=0; i<numBoxes; i++) {
//			if (boxes[i].visible && boxes[i].pos[2] >= left && boxes[i].pos[3] >= bot && 
//				(boxes[i].pos[2]+boxes[i].pos[0]) <= right && (boxes[i].pos[3]+boxes[i].pos[1]) <= top) {
//				//q.boxes[count] = boxes[i];
//				q.boxes[count].layer = boxes[i].layer;
//				q.boxes[count].visible = true;
//				q.boxes[count].pos[0] = boxes[i].pos[0];
//				q.boxes[count].pos[1] = boxes[i].pos[1];
//				q.boxes[count].pos[2] = boxes[i].pos[2];
//				q.boxes[count].pos[3] = boxes[i].pos[3];
//				boxes[i].visible = false; // mark box for removal from upper level
//				count++;
//			}
//		}
//			
//		q.numBoxes = count;
//		q.bounds[0] = left;
//		q.bounds[1] = bot;
//		q.bounds[2] = right;
//		q.bounds[3] = top;
//		q.tl = null;
//		q.tr = null;
//		q.bl = null;
//		q.br = null;
//		
//		if (count > TREETHRESHOLD) {
//			io_pscolor_recursivelyMakeBoxQuadTree(q);
//		}
//	}
	
	void io_pscolor_mergeBoxes()
	{
		int numMerged = 0;
	
		System.out.println("Merging boxes for " + io_pscolor_numCells + " cells...");
		for(Iterator<CELLL> it = io_pscolor_cells.iterator(); it.hasNext(); )
		{
			CELLL c = it.next();
			boolean changed = false;
			do {
				changed = false;
				BOXL b1 = c.box;
				while (b1 != null) {
					BOXL b2 = b1.next;
					while (b2 != null) {
						if (b1.layer == b2.layer && b1.visible && b2.visible) 
							if (io_pscolor_mergeBoxPair(b1, b2)) changed = true;
						b2 = b2.next;
					}
					b1 = b1.next;
				}
			} while (changed);
			numMerged++;
		}
	}
	
	private boolean io_pscolor_mergeBoxPair(BOXL bx1, BOXL bx2)
	{	
		int t0 = bx1.pos[3] + bx1.pos[1];
		int b0 = bx1.pos[3];
		int l0 = bx1.pos[2];
		int r0 = bx1.pos[2] + bx1.pos[0];
		int t1 = bx2.pos[3] + bx2.pos[1];
		int b1 = bx2.pos[3];
		int l1 = bx2.pos[2];
		int r1 = bx2.pos[2] + bx2.pos[0];
	
		// if the boxes coincide, expand the first one and hide the second one
	
		if (t0 == t1 && b0 == b1 && (Math.min(r0, r1) > Math.max(l0, l1))) {
			int l2 = Math.min(l0, l1);
			int r2 = Math.max(r0, r1);
			bx1.pos[0] = r2-l2;
			bx1.pos[1] = t0-b0;
			bx1.pos[2] = l2;
			bx1.pos[3] = b0;
			bx2.visible = false;
			return true;
		}
		else if (r0 == r1 && l0 == l1 && (Math.min(t0, t1) > Math.max(b0, b1))) {
			int b2 = Math.min(b0, b1);
			int t2 = Math.max(t0, t1);
			bx1.pos[0] = r0-l0;
			bx1.pos[1] = t2-b2;
			bx1.pos[2] = l0;
			bx1.pos[3] = b2;
			bx2.visible = false;
			return true;
		}
		// if one completely covers another, hide the covered box
		else if (r0 >= r1 && l0 <= l1 && t0 >= t1 && b0 <= b1) {
			bx2.visible = false;
			return true;
		}
		else if (r1 >= r0 && l1 <= l0 && t1 >= t0 && b1 <= b0) {
			bx1.visible = false;
			return true;
		}
		return false;
	}
	
//	void io_pscolor_coaf1(BOXQUADTREE *q1, BOXQUADTREE *q2, INTBIG layernum)
//	{
//		io_pscolor_coaf2(q1, q2, layernum);
//		if (q1.tl != null) {
//			io_pscolor_coaf3(q1.tl, q2, layernum);
//			io_pscolor_coaf3(q1.tr, q2, layernum);
//			io_pscolor_coaf3(q1.bl, q2, layernum);
//			io_pscolor_coaf3(q1.br, q2, layernum);
//			if (q2.tl != null) {
//				io_pscolor_coaf1(q1.tl, q2.tl, layernum);
//				io_pscolor_coaf1(q1.tr, q2.tr, layernum);
//				io_pscolor_coaf1(q1.bl, q2.bl, layernum);
//				io_pscolor_coaf1(q1.br, q2.br, layernum);
//			}
//			else {
//				io_pscolor_coaf4(q1.tl, q2, layernum, 0);
//				io_pscolor_coaf4(q1.tr, q2, layernum, 0);
//				io_pscolor_coaf4(q1.bl, q2, layernum, 0);
//				io_pscolor_coaf4(q1.br, q2, layernum, 0);
//			}
//		}
//	}
	
//	void io_pscolor_coaf2(BOXQUADTREE *q1, BOXQUADTREE *q2, INTBIG layernum)
//	{
//		io_pscolor_checkOverlapAfterFlattening(q1, q2, layernum);
//		if (q2.tl != null) {
//			io_pscolor_coaf2(q1, q2.tl, layernum);
//			io_pscolor_coaf2(q1, q2.tr, layernum);
//			io_pscolor_coaf2(q1, q2.bl, layernum);
//			io_pscolor_coaf2(q1, q2.br, layernum);
//		}
//	}
	
//	void io_pscolor_coaf3(BOXQUADTREE *q1, BOXQUADTREE *q2, INTBIG layernum)
//	{
//		io_pscolor_checkOverlapAfterFlattening(q1, q2, layernum);
//		if (q2.parent != null) 
//			io_pscolor_coaf3(q1, q2.parent, layernum);
//	}
	
//	void io_pscolor_coaf4(BOXQUADTREE *q1, BOXQUADTREE *q2, INTBIG layernum, INTBIG check)
//	{
//		if (check) {
//			io_pscolor_coaf3(q1, q2, layernum);
//		}
//		if (q1.tl != null) {
//			io_pscolor_coaf4(q1.tl, q2, layernum, 1);
//			io_pscolor_coaf4(q1.tr, q2, layernum, 1);
//			io_pscolor_coaf4(q1.bl, q2, layernum, 1);
//			io_pscolor_coaf4(q1.br, q2, layernum, 1);
//		}
//	}
	
//	void io_pscolor_checkOverlapAfterFlattening(BOXQUADTREE *q1, BOXQUADTREE *q2, INTBIG layerNum)
//	{
//		INTBIG j, k;
//		INTBIG t[3], b[3], l[3], r[3];
//		BOXL *curbox;
//	
//		// check overlap of boxes at this level of the quad tree
//		if (q1.numBoxes && q2.numBoxes) {
//			for (j=0; j<q1.numBoxes; j++) {
//				t[0] = q1.boxes[j].pos[3] + q1.boxes[j].pos[1];
//				b[0] = q1.boxes[j].pos[3];
//				l[0] = q1.boxes[j].pos[2];
//				r[0] = q1.boxes[j].pos[2] + q1.boxes[j].pos[0];
//	
//				for (k=0; k < q2.numBoxes; k++) {
//					t[1] = q2.boxes[k].pos[3] + q2.boxes[k].pos[1];
//					b[1] = q2.boxes[k].pos[3];
//					l[1] = q2.boxes[k].pos[2];
//					r[1] = q2.boxes[k].pos[2] + q2.boxes[k].pos[0];
//	
//					t[2] = t[0] < t[1] ? t[0] : t[1];
//					b[2] = b[0] > b[1] ? b[0] : b[1];
//					l[2] = l[0] > l[1] ? l[0] : l[1];
//					r[2] = r[0] < r[1] ? r[0] : r[1];
//	
//					if (t[2] > b[2] && r[2] > l[2]) {
//						// create overlap layer
//						curbox = (BOXL *)emalloc(sizeof(BOXL), io_tool.cluster);
//						curbox.layer = (INTSML)layerNum;
//						curbox.next = null;
//						curbox.pos[0] = (r[2]-l[2]);
//						curbox.pos[1] = (t[2]-b[2]);
//						curbox.pos[2] = (l[2]);
//						curbox.pos[3] = (b[2]);
//						curbox.visible = 1;
//						if (t[2] == t[0] && b[2] == b[0] && l[2] == l[0] && r[2] == r[0]) q1.boxes[j].visible = 0;
//						if (t[2] == t[1] && b[2] == b[1] && l[2] == l[1] && r[2] == r[1]) q2.boxes[k].visible = 0;
//						io_pscolor_flattenedbox[layerNum] = io_pscolor_insertbox(curbox, io_pscolor_flattenedbox[layerNum]);
//					}
//				}
//			}
//		}
//	}
	
//	void io_pscolor_flatten(void)
//	{
//		float ident[9];
//		INTBIG i;
//		
//		io_pscolor_newIdentityMatrix(ident);
//		for (i=0; i<io_pscolor_numLayers; i++) {
//			io_pscolor_flattenedbox[i] = null;
//		}
//	
//		// for now, assume last cell is top level.  Change this to recognize C *** at top of CIF
//	
//		ttyputmsg(_("Flattening..."));
//		io_pscolor_recursiveFlatten(io_pscolor_cells, ident);
//	}
	
//	void io_pscolor_recursiveFlatten(CELLL *top, float m[9])
//	{
//		CELLINSTL *inst;
//		BOXL *box, *newbox;
//		POLYL *poly, *newpoly;
//		float tm[9];
//	
//		// add boxes from this cell
//		box = top.box;
//		while (box != null) {
//			if (box.visible) {
//				newbox = io_pscolor_copybox(box, m);
//				io_pscolor_flattenedbox[newbox.layer] = io_pscolor_insertbox(newbox, io_pscolor_flattenedbox[newbox.layer]);
//			}
//			box = box.next;
//		}
//	
//		// add polygons from this cell
//		poly = top.poly;
//		while (poly != null) {
//			newpoly = io_pscolor_copypoly(poly, m);
//			io_pscolor_flattenedpoly[newpoly.layer] = io_pscolor_insertpoly(newpoly, io_pscolor_flattenedpoly[newpoly.layer]);
//			poly = poly.next;
//		}
//	
//		// recursively traverse subinstances
//		inst = top.inst;
//		while (inst != null) {
//			io_pscolor_numInstances++;
//			io_pscolor_matrixMul(tm, inst.transform, m);
//			io_pscolor_recursiveFlatten(inst.inst, tm);
//			inst = inst.next;
//		}
//	}
	
//	void io_pscolor_writePS(NODEPROTO *cell, BOOLEAN usePlotter,
//		INTBIG pageWidth, INTBIG pageHeight, INTBIG border)
//	{
//		INTBIG i, j, pslx, pshx, psly, pshy, size, x, y, descenderoffset, rot, xoff, yoff,
//			reducedwidth, reducedheight;
//		CHAR *opname;
//		POLYL *p;
//		BOXL *g;
//		LABELL *l;
//		float scale;
//		INTBIG w, h;
//		time_t curdate;
//		extern CHAR *io_psstringheader[];
//	
//		// Header info
//		io_pswrite(x_("%%!PS-Adobe-1.0\n"));
//		io_pswrite(x_("%%Title: %s\n"), describenodeproto(cell));
//		if ((us_useroptions&NODATEORVERSION) == 0)
//		{
//			io_pswrite(x_("%%%%Creator: Electric VLSI Design System (David Harris's color PostScript generator) version %s\n"), el_version);
//			curdate = getcurrenttime();
//			io_pswrite(x_("%%%%CreationDate: %s\n"), timetostring(curdate));
//		} else
//		{
//			io_pswrite(x_("%%%%Creator: Electric VLSI Design System (David Harris's color PostScript generator)\n"));
//		}
//		io_pswrite(x_("%%%%Pages: 1\n"));
//		io_pswrite(x_("%%%%BoundingBox: %ld %ld %ld %ld\n"), io_pscolor_cifBoundaries[0],
//			io_pscolor_cifBoundaries[1], io_pscolor_cifBoundaries[2], io_pscolor_cifBoundaries[3]);
//		io_pswrite(x_("%%%%DocumentFonts: %s\n"), io_pscolor_font);
//		io_pswrite(x_("%%%%EndComments\n"));
//	
//		// Coordinate system
//		io_pswrite(x_("%% Min X: %d  min Y: %d  max X: %d  max Y: %d\n"), io_pscolor_cifBoundaries[0],
//			io_pscolor_cifBoundaries[1], io_pscolor_cifBoundaries[2], io_pscolor_cifBoundaries[3]);
//		reducedwidth = pageWidth - border*2;
//		reducedheight = pageHeight - border*2;
//		scale = reducedwidth/(float)(io_pscolor_cifBoundaries[2]-io_pscolor_cifBoundaries[0]);
//		if (usePlotter)
//		{
//			// plotter: infinite height
//			io_pswrite(x_("%f %f scale\n"), scale, scale);
//			x = io_pscolor_cifBoundaries[0]+border - ((INTBIG)(pageWidth/scale) -
//				(io_pscolor_cifBoundaries[2]-io_pscolor_cifBoundaries[0])) / 2;
//			y = io_pscolor_cifBoundaries[1]+border;
//			io_pswrite(x_("%d neg %d neg translate\n"), x, y);
//		} else
//		{
//			// printer: fixed height
//			if (reducedheight/(float)(io_pscolor_cifBoundaries[3]-io_pscolor_cifBoundaries[1]) < scale)
//				scale = reducedheight/(float)(io_pscolor_cifBoundaries[3]-io_pscolor_cifBoundaries[1]); 
//			io_pswrite(x_("%f %f scale\n"), scale, scale);
//			x = io_pscolor_cifBoundaries[0]+border - ((INTBIG)(pageWidth/scale) -
//				(io_pscolor_cifBoundaries[2]-io_pscolor_cifBoundaries[0])) / 2;
//			y = io_pscolor_cifBoundaries[1]+border - ((INTBIG)(pageHeight/scale) -
//				(io_pscolor_cifBoundaries[3]-io_pscolor_cifBoundaries[1])) / 2;
//			io_pswrite(x_("%d neg %d neg translate\n"), x, y);
//		}
//	
//		// set font
//		io_pswrite(x_("/DefaultFont /%s def\n"), io_pscolor_font);
//		io_pswrite(x_("/scaleFont {\n"));
//		io_pswrite(x_("    DefaultFont findfont\n"));
//		io_pswrite(x_("    exch scalefont setfont} def\n"));
//	
//		// define box command to make rectangles more memory efficient
//		io_pswrite(x_("\n/bx \n  { /h exch def /w exch def /x exch def /y exch def\n"));
//		io_pswrite(x_("    newpath x y moveto w 0 rlineto 0 h rlineto w neg 0 rlineto closepath fill } def\n")); 
//	
//		// draw layers
//		for (i=0; i<io_pscolor_numLayers; i++)
//		{
//			// skip drawing layers that are white
//			if (io_pscolor_layers[i].r != 1 || io_pscolor_layers[i].g != 1 || io_pscolor_layers[i].b != 1)
//			{
//				g = io_pscolor_flattenedbox[i];
//				p = io_pscolor_flattenedpoly[i];
//				if (g != null || p != null)
//				{
//					io_pswrite(x_("\n%% Layer %s:%d"), io_pscolor_layers[i].tech.techname,
//						io_pscolor_layers[i].layernumber);
//					if (io_pscolor_layers[i].mix1 != -1)
//						io_pswrite(x_(" mix of %d and %d"), io_pscolor_layers[io_pscolor_layers[i].mix1].layernumber,
//							io_pscolor_layers[io_pscolor_layers[i].mix2].layernumber);
//					io_pswrite(x_("\n%f %f %f setrgbcolor\n"), io_pscolor_layers[i].r,
//						io_pscolor_layers[i].g, io_pscolor_layers[i].b);
//				}
//				while (g != null)
//				{
//					if (g.visible)
//					{
//						w = g.pos[0];
//						h = g.pos[1];
//						io_pswrite(x_("%d %d %d %d bx\n"),	g.pos[3], g.pos[2], w, h);	  
//						io_pscolor_numBoxes++;
//					}
//					g = g.next;
//					io_pscolor_totalBoxes++;
//				}
//				while (p != null)
//				{
//					if (p.numcoords > 2)
//					{
//						io_pswrite(x_("newpath %d %d moveto\n"), p.coords[0], p.coords[1]);
//						for (j=2; j<p.numcoords; j+=2) {
//							io_pswrite(x_("        %d %d lineto\n"), p.coords[j], p.coords[j+1]);
//						}
//						io_pswrite(x_("closepath %s\n"), i==0 ? x_("stroke") : x_("fill"));
//						io_pscolor_numPolys++;
//					}
//					p = p.next;
//				}
//	/*			LINEL *h = io_pscolor_horizsegs[i];
//				io_pswrite(x_("\n0 0 0 setrgbcolor\n newpath\n"));
//				while (h != null) {
//					io_pswrite(x_("%d %d moveto %d %d lineto\n"), h.x[0], h.y, h.x[1], h.y);
//					h = h.next;
//				}
//				io_pswrite(x_("closepath stroke\n"));
//	*/		
//			}
//		}
//	
//		// label ports and cell instances
//		l = io_pscolor_cells.label;
//		io_pswrite(x_("\n%% Port and Cell Instance Labels\n"));
//		io_pswrite(x_("0 0 0 setrgbcolor\n"));
//		for(i=0; io_psstringheader[i] != 0; i++)
//			io_pswrite(x_("%s\n"), io_psstringheader[i]);
//		while (l != null)
//		{
//			size = io_pscolor_truefontsize(TDGETSIZE(l.descript), el_curtech);
//			pslx = l.pos[0];   pshx = l.pos[1];
//			psly = l.pos[2];   pshy = l.pos[3];
//			if (l.style == TEXTBOX)
//			{
//				io_pswrite(x_("%ld %ld %ld %ld "), (pslx+pshx)/2, (psly+pshy)/2, pshx-pslx, pshy-psly);
//				io_pswritestring(l.label);
//				io_pswrite(x_(" %f Boxstring\n"), size/scale);
//			} else
//			{
//				switch (l.style)
//				{
//					case TEXTCENT:
//						x = (pslx+pshx)/2;   y = (psly+pshy)/2;
//						opname = x_("Centerstring");
//						break;
//					case TEXTTOP:
//						x = (pslx+pshx)/2;   y = pshy;
//						opname = x_("Topstring");
//						break;
//					case TEXTBOT:
//						x = (pslx+pshx)/2;   y = psly;
//						opname = x_("Botstring");
//						break;
//					case TEXTLEFT:
//						x = pslx;   y = (psly+pshy)/2;
//						opname = x_("Leftstring");
//						break;
//					case TEXTRIGHT:
//						x = pshx;   y = (psly+pshy)/2;
//						opname = x_("Rightstring");
//						break;
//					case TEXTTOPLEFT:
//						x = pslx;   y = pshy;
//						opname = x_("Topleftstring");
//						break;
//					case TEXTTOPRIGHT:
//						x = pshx;   y = pshy;
//						opname = x_("Toprightstring");
//						break;
//					case TEXTBOTLEFT:
//						x = pslx;   y = psly;
//						opname = x_("Botleftstring");
//						break;
//					case TEXTBOTRIGHT:
//						x = pshx;   y = psly;
//						opname = x_("Botrightstring");
//						break;
//				}
//				descenderoffset = size / 12;
//				rot = TDGETROTATION(l.descript);
//				switch (rot)
//				{
//					case 0: y += descenderoffset;   break;
//					case 1: x -= descenderoffset;   break;
//					case 2: y -= descenderoffset;   break;
//					case 3: x += descenderoffset;   break;
//				}
//				if (rot != 0)
//				{
//					if (rot == 1 || rot == 3)
//					{
//						switch (l.style)
//						{
//							case TEXTTOP:      opname = x_("Rightstring");     break;
//							case TEXTBOT:      opname = x_("Leftstring");      break;
//							case TEXTLEFT:     opname = x_("Botstring");       break;
//							case TEXTRIGHT:    opname = x_("Topstring");       break;
//							case TEXTTOPLEFT:  opname = x_("Botrightstring");  break;
//							case TEXTBOTRIGHT: opname = x_("Topleftstring");   break;
//						}
//					}
//					xoff = x;   yoff = y;
//					x = y = 0;
//					switch (rot)
//					{
//						case 1:		// 90 degrees counterclockwise
//							io_pswrite(x_("%ld %ld translate 90 rotate\n"), xoff, yoff);
//							break;
//						case 2:		// 180 degrees
//							io_pswrite(x_("%ld %ld translate 180 rotate\n"), xoff, yoff);
//							break;
//						case 3:		// 90 degrees clockwise
//							io_pswrite(x_("%ld %ld translate 270 rotate\n"), xoff, yoff);
//							break;
//					}
//				}
//				io_pswrite(x_("%ld %ld "), x, y);
//				io_pswritestring(l.label);
//				io_pswrite(x_(" %ld %s\n"), size, opname);
//				if (rot != 0)
//				{
//					switch (rot)
//					{
//						case 1:		// 90 degrees counterclockwise
//							io_pswrite(x_("270 rotate %ld %ld translate\n"), -xoff, -yoff);
//							break;
//						case 2:		// 180 degrees
//							io_pswrite(x_("180 rotate %ld %ld translate\n"), -xoff, -yoff);
//							break;
//						case 3:		// 90 degrees clockwise
//							io_pswrite(x_("90 rotate %ld %ld translate\n"), -xoff, -yoff);
//							break;
//					}
//				}
//			}
//			l = l.next;
//		}
//		
//		// Finish page
//		io_pswrite(x_("\nshowpage\n"));
//	}
	
//	INTBIG io_pscolor_truefontsize(INTBIG font, TECHNOLOGY *tech)
//	{
//		REGISTER INTBIG  lambda, height;
//	
//		// absolute font sizes are easy
//		if ((font&TXTPOINTS) != 0) return((font&TXTPOINTS) >> TXTPOINTSSH);
//	
//		// detemine default, min, and max size of font
//		lambda = el_curlib.lambda[tech.techindex];
//		height = TXTGETQLAMBDA(font);
//		height = height * lambda / 4;
//		return(height);
//	}
	
//	BOXL *io_pscolor_copybox(BOXL *g, float m[9])
//	{
//		BOXL *newbox;
//		INTBIG dx, dy;
//	
//		newbox = (BOXL *)emalloc(sizeof(BOXL), io_tool.cluster);
//		newbox.layer = g.layer;
//		newbox.next = null;
//		io_pscolor_transformBox(newbox.pos, g.pos, m);
//		newbox.visible = g.visible;
//	
//		// Update bounding box
//		dx = newbox.pos[0];
//		dy = newbox.pos[1];
//		if (newbox.pos[2] < io_pscolor_cifBoundaries[0]) io_pscolor_cifBoundaries[0] = newbox.pos[2];
//		if (newbox.pos[3] < io_pscolor_cifBoundaries[1]) io_pscolor_cifBoundaries[1] = newbox.pos[3];
//		if (newbox.pos[2]+dx > io_pscolor_cifBoundaries[2]) io_pscolor_cifBoundaries[2] = newbox.pos[2]+dx;
//		if (newbox.pos[3]+dy > io_pscolor_cifBoundaries[3]) io_pscolor_cifBoundaries[3] = newbox.pos[3]+dy;
//		return newbox;
//	}
	
//	POLYL *io_pscolor_copypoly(POLYL *p, float m[9])
//	{
//		POLYL *newpoly;
//		INTBIG i;
//	
//		newpoly = (POLYL *)emalloc(sizeof(POLYL), io_tool.cluster);
//		if (newpoly == 0) return(0);
//		newpoly.layer = p.layer;
//		newpoly.next = null;
//		newpoly.numcoords = p.numcoords;
//		newpoly.coords = (INTBIG *)emalloc(newpoly.numcoords * SIZEOFINTBIG, io_tool.cluster);
//		if (newpoly.coords == 0) return(0);
//		io_pscolor_transformPoly(newpoly, p, m);
//	
//		// Update bounding box
//		for(i=0; i<newpoly.numcoords; i++)
//		{
//			if ((i%2) == 0)
//			{
//				if (newpoly.coords[i] < io_pscolor_cifBoundaries[0]) io_pscolor_cifBoundaries[0] = newpoly.coords[i];
//				if (newpoly.coords[i] > io_pscolor_cifBoundaries[3]) io_pscolor_cifBoundaries[3] = newpoly.coords[i];
//			} else
//			{
//				if (newpoly.coords[i] < io_pscolor_cifBoundaries[1]) io_pscolor_cifBoundaries[1] = newpoly.coords[i];
//				if (newpoly.coords[i] > io_pscolor_cifBoundaries[2]) io_pscolor_cifBoundaries[2] = newpoly.coords[i];
//			}
//		}
//		return newpoly;
//	}
	
//	BOXL* io_pscolor_insertbox(BOXL *g, BOXL *list)
//	{
//		g.next = list;
//		list = g;
//		return list;
//	}
	
//	POLYL* io_pscolor_insertpoly(POLYL *p, POLYL *list)
//	{
//		p.next = list;
//		list = p;
//		return list;
//	}
	
//	LABELL* io_pscolor_insertlabel(LABELL *p, LABELL *list)
//	{
//		p.next = list;
//		list = p;
//		return list;
//	}
	
//	CELLINSTL* io_pscolor_insertInst(CELLINSTL *i, CELLINSTL *list)
//	{
//		i.next = list;
//		list = i;
//		return list;
//	}
	
//	void io_pscolor_matrixMul(float r[9], float a[9], float b[9])
//	{
//		INTBIG i, j, k;
//		float tmp[9];
//	
//		for (i=0; i<3; i++) {
//			for (j=0; j<3; j++) {
//				tmp[i*3+j] = 0;
//				for (k=0; k<3; k++) {
//					tmp[i*3+j] += a[i*3+k] * b[k*3+j];
//				}
//			}
//		}
//		for (i=0; i<3; i++) {
//			for (j=0; j<3; j++) {
//				r[i*3+j] = tmp[i*3+j];
//			}
//		}
//	}
	
//	void io_pscolor_newIdentityMatrix(float m[9])
//	{
//		INTBIG i,j;
//	
//		for (i=0; i<3; i++) {
//			for (j=0; j<3; j++) {
//				m[i*3+j] = (i==j);
//			}
//		}
//	}
	
//	void io_pscolor_transformBox(INTBIG final[4], INTBIG initial[4], float m[9])
//	{
//		INTBIG i,j;
//		INTBIG pos[2];
//	
//		pos[0] = initial[2]+initial[0]/2;
//		pos[1] = initial[3]+initial[1]/2;
//	
//		for (i=0; i < 2; i++) {
//			final[i+2] = (INTBIG)m[6+i];
//			for (j=0; j<2; j++) {
//				final[i+2] += (INTBIG)m[i+j*3]*pos[j];
//			}
//		}
//	
//		if (m[1] == 0) { // rotation
//			final[0] = initial[0];
//			final[1] = initial[1];
//		}
//		else {
//			final[0] = initial[1];
//			final[1] = initial[0];
//		}
//		final[2] -= final[0]/2;
//		final[3] -= final[1]/2;
//	}
	
//	void io_pscolor_transformPoly(POLYL *final, POLYL *initial, float m[9])
//	{
//		INTBIG p, i,j;
//		
//		for (p=0; p<initial.numcoords/2; p++) {
//			for (i=0; i <2; i++) {
//				final.coords[p*2+i] = (INTBIG)m[6+i];
//				for (j=0; j<2; j++) {
//					final.coords[p*2+i] += (INTBIG)m[i+j*3]*initial.coords[p*2+j];
//				}
//			}
//		}
//	}
	
//	void io_pscolor_printStatistics(void)
//	{
//		ttyputmsg("Plotting statistics:");
//		ttyputmsg("  %ld layers defined or transparencies implied in layer map", io_pscolor_numLayers);
//		ttyputmsg("  %ld cells", io_pscolor_numCells);
//		ttyputmsg("  %ld instances used", io_pscolor_numInstances);
//		ttyputmsg("  %ld boxes generated", io_pscolor_numBoxes);
//		ttyputmsg("  %ld polygons generated", io_pscolor_numPolys);
//	}
}
