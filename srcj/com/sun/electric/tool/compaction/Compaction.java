/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Compaction.java
 * Originally written by: Nora Ryan, Schlumberger Palo Alto Research
 * Rewritten and translated by: Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.compaction;

import com.sun.electric.database.constraint.Layout;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.technology.DRCTemplate;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.drc.DRC;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * This is the Compaction tool.
 *
 * When compacting cell instances, the system only examines polygons
 * within a protection frame of the cell border.  This frame is the largest
 * design rule distance in the technology.  If the cell border is irregular,
 * there may be objects that are not seen, causing the cell to overlap
 * more than it should.
 */
public class Compaction extends Tool
{
	/** the Compaction tool. */		private static Compaction tool = new Compaction();

	/****************************** TOOL INTERFACE ******************************/

	/**
	 * The constructor sets up the Compaction tool.
	 */
	private Compaction()
	{
		super("compaction");
	}

	/**
	 * Method to initialize the Compaction tool.
	 */
	public void init()
	{
	}

    /**
     * Method to retrieve the singleton associated with the Compaction tool.
     * @return the Compaction tool.
     */
    public static Compaction getCompactionTool() { return tool; }

	/****************************** COMPACTION CONTROL ******************************/

	/**
	 * Method to compact the current cell.
	 */
	public static void compactNow()
	{
		UserInterface ui = Job.getUserInterface();
		Cell cell = ui.getCurrentCell();
		if (cell == null) return;
		compactNow(cell);
	}

	/**
	 * Method to compact the requested cell.
	 */
	public static void compactNow(Cell cell)
	{
		// do the compaction in a job
		CompactCellJob job = new CompactCellJob(cell, true, CompactCell.Axis.HORIZONTAL);
        job.startJob();
	}
private static int limitLoops = 10;
	/**
	 * Class to compact a cell in a Job.
	 */
	private static class CompactCellJob extends Job
	{
		private Cell cell;
		private boolean lastTime;
		private CompactCell.Axis curAxis;

		private CompactCellJob(Cell cell, boolean lastTime, CompactCell.Axis curAxis)
		{
			super("Compact " + cell, tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.lastTime = lastTime;
			this.curAxis = curAxis;
		}

		public boolean doIt() throws JobException
		{
			// make the compaction object for the cell
			CompactCell cc = new CompactCell(cell);

			// alternate vertical then horizontal compaction
			boolean change = cc.compactOneDirection(curAxis);
if (--limitLoops <= 0) change = false;
			if (lastTime || change)
			{
				curAxis = (curAxis == CompactCell.Axis.HORIZONTAL) ? CompactCell.Axis.VERTICAL : CompactCell.Axis.HORIZONTAL;
				CompactCellJob job = new CompactCellJob(cell, change, curAxis);
                job.startJobOnMyResult();
			} else
			{
				System.out.println("Compaction complete");
//				if (completion != null)
//					completion.startJob();
			}
			return true;
		}
	}

	/****************************** OPTIONS ******************************/

	private static Pref cacheAllowSpreading = Pref.makeBooleanPref("AllowSpreading", tool.prefs, false);
	/**
	 * Method to tell whether the compactor can spread circuitry apart, or just compact it.
	 * The default is "false".
	 * @return true if the compactor can spread circuitry apart; false to just compact it.
	 */
	public static boolean isAllowsSpreading() { return cacheAllowSpreading.getBoolean(); }
	/**
	 * Method to set whether the compactor can spread circuitry apart, or just compact it.
	 * @param on true if the compactor can spread circuitry apart; false to just compact it.
	 */
	public static void setAllowsSpreading(boolean on) { cacheAllowSpreading.setBoolean(on); }

	/****************************** COMPACTION ******************************/

	/**
	 * Class to compact a cell.
	 */
	private static class CompactCell
	{
		private static final double DEFAULT_VAL = -99999999;

		private static enum Axis { HORIZONTAL, VERTICAL };

		private static class PolyList
		{
			private Poly       poly;
			private Technology tech;
			private int        networkNum;
			private PolyList   nextPolyList;
		};

		private static class GeomObj
		{
			private Geometric inst;
			private PolyList  firstPolyList;
			private double    lowx, highx, lowy, highy;
			private double    outerLowx, outerHighx, outerLowy, outerHighy;
			private GeomObj   nextObject;
		};

		private static class Line
		{
			private int      index;
			private double   val;
			private double   low, high;
			private double   top, bottom;
			private GeomObj  firstObject;
			private Line     nextLine;
			private Line     prevLine;
		};

		/** protection frame max size for technology */			private double  maxBoundary;
		/** lowest edge of current line */						private double  lowBound;
		/** counter for unique network numbers */				private int     flatIndex;
		/** used for numbering lines (debugging only) */		private int     lineIndex = 0;
		/** current axis of compaction */						private Axis    curAxis;
		/** cell being compacted */								private Cell    cell;

		private CompactCell(Cell cell)
		{
			this.cell = cell;
		}

		/**
		 * Method to do vertical compaction (if "axis" is VERTICAL) or horizontal
		 * compaction (if "axis" is HORIZONTAL) to cell "np".  Displays state if
		 * "verbose" is nonzero.  Returns true if a change was made.
		 */
		private boolean compactOneDirection(Axis curAxis)
		{
			this.curAxis = curAxis;

			// determine maximum drc surround for entire technology
			maxBoundary = DRC.getWorstSpacingDistance(Technology.getCurrent(), -1);

			if (curAxis == Axis.HORIZONTAL) System.out.println("Compacting horizontally"); else
				System.out.println("Compacting vertically");

			// number ports of cell "cell"
			HashMap<PortProto,Integer> portIndices = new HashMap<PortProto,Integer>();
			flatIndex = 1;
			Netlist nl = cell.acquireUserNetlist();
			if (nl == null)
			{
				System.out.println("Sorry, a deadlock aborted compaction (network information unavailable).  Please try again");
				return false;
			}
			for(Iterator<PortProto> it = cell.getPorts(); it.hasNext(); )
			{
				Export pp = (Export)it.next();
				Network net = nl.getNetwork(pp, 0);

				// see if this port is on the same net as previously examined one
				Export found = null;
				for(Iterator<PortProto> oIt = cell.getPorts(); oIt.hasNext(); )
				{
					Export oPp = (Export)oIt.next();
					if (oPp == pp) break;
					Network oNet = nl.getNetwork(oPp, 0);
					if (net == oNet) { found = oPp;   break; }
				}
				if (found != null)
				{
					Integer oIndex = portIndices.get(found);
					portIndices.put(pp, oIndex);
				} else portIndices.put(pp, new Integer(flatIndex++));
			}

			// copy port numbering onto arcs
			HashMap<ArcInst,Integer> arcIndices = subCellSmash(cell, portIndices);

			// clear "seen" information on every node
			HashSet<NodeInst> nodesSeen = new HashSet<NodeInst>();

			// clear object information
			Line lineComp = null;
			List<GeomObj> otherObjectList = new ArrayList<GeomObj>();

			// now check every object
			for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				if (ni.getProto() == Generic.tech.cellCenterNode ||
					ni.getProto() == Generic.tech.essentialBoundsNode) continue;

				// clear "thisObject" before calling createobject
				List<GeomObj> thisObjectList = new ArrayList<GeomObj>();
				createObjects(ni, thisObjectList, otherObjectList, nodesSeen, arcIndices, portIndices);

				// create object of layout
				if (thisObjectList.size() != 0)
					lineComp = makeObjectLine(lineComp, thisObjectList);
			}

			// create list of perpendicular line which need to be set stretchable
			Line lineStretch = null;
			if (otherObjectList.size() != 0)
				lineStretch = makeObjectLine(null, otherObjectList);

			// compute bounds for each line
			for(Line curLine = lineComp; curLine != null; curLine = curLine.nextLine)
				computeLineHiAndLow(curLine);

			// sort the compacting line of objects
			lineComp = sortLines(lineComp);

			// do the compaction
			lowBound = findLeastLow(lineComp);
//			boolean change = lineupFirstRow(lineComp, lineStretch, lowBound);
			boolean change = false;
			change = compactLine(lineComp, lineStretch, change, cell);

			return change;
		}

        
		private boolean compactLine(Line line, Line lineStretch, boolean change, Cell cell)
		{
//            System.out.println("Compacting line:");
//            for (Line curLine = line; curLine != null; curLine = curLine.nextLine) {
//                System.out.print("\t");
//                for (GeomObj obj = curLine.firstObject; obj != null; obj = obj.nextObject)
//                    System.out.print(" " + obj.inst);
//                System.out.println();
//            }
//            System.out.println("Stretch line:");
//            for (Line curLine = lineStretch; curLine != null; curLine = curLine.nextLine) {
//                System.out.print("\t");
//                for (GeomObj obj = curLine.firstObject; obj != null; obj = obj.nextObject)
//                    System.out.print(" " + obj.inst);
//                System.out.println();
//            }
           
			boolean spread = isAllowsSpreading();

			// loop through all lines that may compact
			for(Line curLine = line.nextLine; curLine != null; curLine = curLine.nextLine)
			{
				if (curLine.low <= line.low) continue;
				double bestMotion = DEFAULT_VAL;

				// look at all previous lines
				for(Line prevLine = curLine.prevLine; prevLine != null; prevLine = prevLine.prevLine)
				{
					// look at every object in the line that may compact
					for(GeomObj curObject = curLine.firstObject; curObject != null; curObject = curObject.nextObject)
					{
						// no need to test this line if it is farther than best motion
						if (bestMotion != DEFAULT_VAL &&
							curLine.low - prevLine.high > bestMotion) continue;

						// simple object compaction
						double thisMotion = checkInst(curObject, prevLine, cell);
						if (thisMotion == DEFAULT_VAL) continue;
						if (bestMotion == DEFAULT_VAL || thisMotion < bestMotion)
						{
							bestMotion = thisMotion;
						}
					}
				}

				if (bestMotion == DEFAULT_VAL)
				{
					// no constraints: allow overlap
					bestMotion = curLine.low - lowBound;
				}
				if (bestMotion > DBMath.getEpsilon() || (spread && bestMotion < -DBMath.getEpsilon()))
				{
					// initialize arcs: disable stretching line from sliding; make moving line rigid
					HashSet<ArcInst> clearedArcs = ensureSlidability(lineStretch);
					setupTemporaryRigidity(line, lineStretch);

					if (curAxis == Axis.HORIZONTAL)
						change = moveLine(curLine, bestMotion, 0, change); else
							change = moveLine(curLine, 0, bestMotion, change);

					// restore slidability on stretching lines
					restoreSlidability(clearedArcs);
				}
			}
			return change;
		}

		private double checkInst(GeomObj object, Line line, Cell cell)
		{
			double bestMotion = DEFAULT_VAL;
			for(PolyList polys = object.firstPolyList; polys != null; polys = polys.nextPolyList)
			{
				// translate any pseudo layers for this node
				Poly poly = polys.poly;
				Layer layer = poly.getLayer().getNonPseudoLayer();

				// find distance line can move toward this poly
				double thisMotion = minSeparate(object, layer, polys, line, cell);
				if (thisMotion == DEFAULT_VAL) continue;
				if (bestMotion == DEFAULT_VAL || thisMotion < bestMotion)
				{
					bestMotion = thisMotion;
				}
			}
			return bestMotion;
		}

		/**
		 * Method finds the minimum distance which is necessary between polygon
		 * "obj" (from object "object" on layer "nLayer" with network connectivity
		 * "nIndex") and the previous line in "line".  It returns the amount
		 * to move this object to get it closest to the line (DEFAULT_VAL if they can
		 * overlap).  The object "reason" is set to the object that is causing the
		 * constraint.
		 */
		private double minSeparate(GeomObj object, Layer nLayer, PolyList nPolys, Line line, Cell cell)
		{
			Poly nPoly = nPolys.poly;
			double nminsize = nPoly.getMinSize();
			Technology tech = nPolys.tech;
			int nIndex = nPolys.networkNum;

			// see how far around the box it is necessary to search
			double bound = DRC.getMaxSurround(nLayer, Double.MAX_VALUE);

			// if there is no separation, allow them to sit on top of each other
			if (bound < 0) return DEFAULT_VAL;

			// can only handle orthogonal rectangles for now
			Rectangle2D nbox = nPoly.getBox();
			if (nbox == null) return bound;

			double bestMotion = DEFAULT_VAL;
			double geomLow = object.lowy;
			if (curAxis == Axis.HORIZONTAL) geomLow = object.lowx;

			// search the line
			for(GeomObj curObject = line.firstObject; curObject != null; curObject = curObject.nextObject)
			{
				if (curAxis == Axis.HORIZONTAL)
				{
					if (!isInBound(nbox.getMinY()-bound, nbox.getMaxY()+bound, curObject.outerLowy, curObject.outerHighy)) continue;
				} else
				{
					if (!isInBound(nbox.getMinX()-bound, nbox.getMaxX()+bound, curObject.outerLowx, curObject.outerHighx)) continue;
				}

				// examine every layer in this object
				for(PolyList polys = curObject.firstPolyList; polys != null; polys = polys.nextPolyList)
				{
					// don't check between technologies
					if (polys.tech != tech) continue;

					Poly poly = polys.poly;
					Layer layer = poly.getLayer().getNonPseudoLayer();
					int pIndex = polys.networkNum;

					// see whether the two objects are electrically connected
					boolean con = false;
					if (pIndex == nIndex && pIndex != -1)
					{
						Layer.Function nFun = nLayer.getFunction();
						Layer.Function fun = layer.getFunction();
						if (!nFun.isSubstrate() && !fun.isSubstrate())
							con = true;
					}
					Rectangle2D polyBox = poly.getBox();
					if (polyBox == null)
					{
						double niHigh = curObject.outerHighy;
						if (curAxis == Axis.HORIZONTAL) niHigh = curObject.outerHighx;

						double thisMotion = geomLow - niHigh - bound;
						if (thisMotion == DEFAULT_VAL) continue;
						if (thisMotion < bestMotion || bestMotion == DEFAULT_VAL)
						{
							bestMotion = thisMotion;
						}
						continue;
					}

					// see how close they can get
					double dist = -1;
					DRCTemplate rule = DRC.getSpacingRule(nLayer, null, layer, null, con, -1, 0, 0);
					if (rule != null) dist = rule.value1;
					if (dist < 0) continue;

					/*
					 * special rule for ignoring distance:
					 *   the layers are the same and either:
					 *     they connect and are *NOT* contact layers
					 *   or:
					 *     they don't connect and are implant layers (substrate/well)
					 */
					if (nLayer == layer)
					{
						Layer.Function fun = nLayer.getFunction();
						if (con)
						{
							if (!fun.isContact()) continue;
						} else
						{
							if (fun.isSubstrate()) continue;
						}
					}

					// if the two layers are offset on the axis so that there is no possible contraint between them
					if (curAxis == Axis.HORIZONTAL)
					{
						if (!isInBound(nbox.getMinY()-dist, nbox.getMaxY()+dist, polyBox.getMinY(), polyBox.getMaxY())) continue;
					} else
					{
						if (!isInBound(nbox.getMinX()-dist, nbox.getMaxX()+dist, polyBox.getMinX(), polyBox.getMaxX())) continue;
					}

					// check the distance
					double thisMotion = checkLayers(nLayer, nIndex, object, polyBox, layer, pIndex, curObject, nbox, dist);
					if (thisMotion == DEFAULT_VAL) continue;
					if (thisMotion < bestMotion || bestMotion == DEFAULT_VAL)
					{
						bestMotion = thisMotion;
					}
				}
			}
			return bestMotion;
		}

		/**
		 * Method to see if the object in "object1" on layer "layer1" with electrical
		 * index "index1" comes within "dist" from the object in "object2" on layer
		 * "layer2" with electrical index "index2" in the perpendicular axis to "axis".
		 * The bounds of object "object1" are (lx1-hx1,ly1-hy1), and the bounds of object
		 * "object2" are (lx2-hx2,ly2-hy2).  If the objects are in bounds, the spacing
		 * between them is returned.  Otherwise, DEFAULT_VAL is returned.
		 */
		private double checkLayers(Layer layer1, int index1, GeomObj object1, Rectangle2D bound1,
			Layer layer2, int index2, GeomObj object2, Rectangle2D bound2, double dist)
		{
			// crop out parts of a box covered by a similar layer on the other node
			if (object1.inst instanceof NodeInst)
			{
				if (cropNodeInst(object1.firstPolyList, bound2, layer2, index2))
					return DEFAULT_VAL;
			}
			if (object2.inst instanceof NodeInst)
			{
				if (cropNodeInst(object2.firstPolyList, bound1, layer1, index1))
					return DEFAULT_VAL;
			}

			// now compare the box extents
			if (curAxis == Axis.HORIZONTAL)
			{
				if (bound1.getMaxY()+dist > bound2.getMinY() && bound1.getMinY()-dist < bound2.getMaxY())
				{
					double spacing = bound2.getMinX() - bound1.getMaxX() - dist;
					return spacing;
				}
			} else if (bound1.getMaxX()+dist > bound2.getMinX() && bound1.getMinX()-dist < bound2.getMaxX())
			{
				double spacing = bound2.getMinY() - bound1.getMaxY() - dist;
				return spacing;
			}
			return DEFAULT_VAL;
		}

		/**
		 * Method to crop the box on layer "nLayer", electrical index "nIndex"
		 * and bounds (lx-hx, ly-hy) against the nodeinst "ni".  Only those layers
		 * in the nodeinst that are the same layer and the same electrical index
		 * are checked.  The routine returns true if the bounds are reduced
		 * to nothing.
		 */
		private boolean cropNodeInst(PolyList polys, Rectangle2D bound, Layer nLayer, int nIndex)
		{
			for(PolyList curPoly = polys; curPoly != null; curPoly = curPoly.nextPolyList)
			{
				if (curPoly.networkNum != nIndex) continue;
				if (curPoly.poly.getLayer() != nLayer) continue;
				Rectangle2D polyBox = curPoly.poly.getBox();
				if (polyBox == null) continue;
				int temp = Poly.cropBox(bound, polyBox);
				if (temp > 0) return true;
			}
			return false;
		}

		private boolean isInBound(double ll, double lh, double rl, double rh)
		{
			if (rh > ll && rl < lh) return true;
			return false;
		}

		private boolean lineupFirstRow(Line line, Line lineStretch, double lowestBound)
		{
			boolean change = false;
			double i = line.low - lowestBound;
			if (i > DBMath.getEpsilon())
			{
				// initialize arcs: disable stretching line from sliding; make moving line rigid
				HashSet<ArcInst> clearedArcs = ensureSlidability(lineStretch);
				setupTemporaryRigidity(line, lineStretch);

				if (curAxis == Axis.HORIZONTAL) change = moveLine(line, i, 0, change); else
					change = moveLine(line, 0, i, change);

				// restore slidability on stretching lines
				restoreSlidability(clearedArcs);
			}
			return change;
		}

		/**
		 * moves a object of instances distance (movex, movey), and returns a true if
		 * there is actually a move
		 */
		private boolean moveLine(Line line, double moveX, double moveY, boolean change)
		{
			double move = moveX;
			if (moveX == 0) move = moveY;
			if (line == null) return false;
			if (!change && move != 0) change = true;

			for(GeomObj curObject = line.firstObject; curObject != null; curObject = curObject.nextObject)
			{
				if (curObject.inst instanceof NodeInst)
				{
					NodeInst ni = (NodeInst)curObject.inst;
					ni.move(-moveX, -moveY);
					break;
				}
			}

			for(GeomObj curObject = line.firstObject; curObject != null; curObject = curObject.nextObject)
			{
				curObject.lowx -= moveX;
				curObject.highx -= moveX;
				curObject.lowy -= moveY;
				curObject.highy -= moveY;

				curObject.outerLowx -= moveX;
				curObject.outerHighx -= moveX;
				curObject.outerLowy -= moveY;
				curObject.outerHighy -= moveY;
				for(PolyList polys = curObject.firstPolyList; polys != null; polys = polys.nextPolyList)
				{
					Point2D [] points = polys.poly.getPoints();
					for(int i=0; i<points.length; i++)
					{
						points[i].setLocation(points[i].getX() - moveX, points[i].getY() - moveY);
					}
				}
			}
			line.high -= move;
			line.low -= move;
			return change;
		}

		/**
		 * Method to find least low of the line.
		 * re-set first line low in the list
		 * finds the smallest low value (lowx for VERTICAL, lowy for HORIZ case)
		 * stores it in line->low.
		 */
		private double findLeastLow(Line line)
		{
			if (line == null) return 0;

			// find smallest low for the each object
			boolean first = true;
			double low = 0;
			for(GeomObj curObject = line.firstObject; curObject != null; curObject = curObject.nextObject)
			{
				if (!(curObject.inst instanceof NodeInst)) continue;
				double thisLow = curObject.lowy;
				if (curAxis == Axis.HORIZONTAL) thisLow = curObject.lowx;

				if (!first) low = Math.min(low, thisLow); else
				{
					low = thisLow;
					first = false;
				}
			}
			line.low = low;
			return low;
		}

		/**
		 * Method to temporarily make all arcs in fixline rigid and those
		 * in nfixline nonrigid in order to move fixline over
		 */
		private void setupTemporaryRigidity(Line fixLine, Line lineStretch)
		{
			for(Line curLine = fixLine; curLine != null; curLine = curLine.nextLine)
			{
				for(GeomObj curObject = curLine.firstObject; curObject != null; curObject = curObject.nextObject)
				{
					if (!(curObject.inst instanceof NodeInst))    // arc rigid
					{
						Layout.setTempRigid((ArcInst)curObject.inst, true);
					}
				}
			}
			for(Line curLine = lineStretch; curLine != null; curLine = curLine.nextLine)
			{
				for(GeomObj curObject = curLine.firstObject; curObject != null; curObject = curObject.nextObject)
				{
					if (!(curObject.inst instanceof NodeInst))   // arc unrigid
					{
						Layout.setTempRigid((ArcInst)curObject.inst, false);
					}
				}
			}
		}

		/**
		 * set the CANTSLIDE bit of userbits for each object in line so that this
		 * line will not slide.
		 */
		private HashSet<ArcInst> ensureSlidability(Line line)
		{
			HashSet<ArcInst> clearedArcs = new HashSet<ArcInst>();
			for(Line curLine = line; curLine != null; curLine = curLine.nextLine)
			{
				for(GeomObj curObject = curLine.firstObject; curObject != null;
				curObject = curObject.nextObject)
				{
					if (!(curObject.inst instanceof NodeInst))
					{
						ArcInst ai = (ArcInst)curObject.inst;
						if (ai.isSlidable())
						{
							ai.setSlidable(false);
							clearedArcs.add(ai);
						}
					}
				}
			}
			return clearedArcs;
		}

		/**
		 * restore the CANTSLIDE bit of userbits for each object in line
		 */
		private void restoreSlidability(HashSet<ArcInst> clearedArcs)
		{
			for(ArcInst ai : clearedArcs)
			{
				ai.setSlidable(true);
			}
		}

		private void computeLineHiAndLow(Line line)
		{
			// find smallest and highest vals for the each object
			boolean first = true;
			double lx = 0, hx = 0, ly = 0, hy = 0;
			for(GeomObj curObject = line.firstObject; curObject != null; curObject = curObject.nextObject)
			{
				if (!(curObject.inst instanceof NodeInst)) continue;
				if (first)
				{
					lx = curObject.outerLowx;
					hx = curObject.outerHighx;
					ly = curObject.outerLowy;
					hy = curObject.outerHighy;
					first = false;
				} else
				{
					if (curObject.outerLowx < lx) lx = curObject.outerLowx;
					if (curObject.outerHighx > hx) hx = curObject.outerHighx;
					if (curObject.outerLowy < ly) ly = curObject.outerLowy;
					if (curObject.outerHighy > hy) hy = curObject.outerHighy;
				}
			}
			if (curAxis == Axis.HORIZONTAL)
			{
				line.low = lx;
				line.high = hx;
				line.top = hy;
				line.bottom = ly;
			} else
			{
				line.low = ly;
				line.high = hy;
				line.top = hx;
				line.bottom = lx;
			}
		}

		/**
		 * Method to sort line by center val from least to greatest
		 */
		private Line sortLines(Line line)
		{
			if (line == null)
			{
				System.out.println("Error: sortLines called with null argument");
				return null;
			}

			// first figure out the weighting factor that will be sorted
			for(Line curLine = line; curLine != null; curLine = curLine.nextLine)
			{
				double ave = 0, totalLen = 0;
				for(GeomObj curObject = curLine.firstObject; curObject != null; curObject = curObject.nextObject)
				{
					double len = 0, ctr = 0;
					if (curAxis == Axis.HORIZONTAL)
					{
						len = curObject.highy - curObject.lowy;
						ctr = (curObject.lowx+curObject.highx) / 2;
					} else
					{
						len = curObject.highx - curObject.lowx;
						ctr = (curObject.lowy+curObject.highy) / 2;
					}

//					ctr *= len;
//					totalLen += len;
					totalLen++;
					ave += ctr;
				}
				if (totalLen != 0) ave /= totalLen;
				curLine.val = ave;
			}

			// now sort on the "val" field
			Line newLine = null;
			for(;;)
			{
				if (line == null) break;
				boolean first = true;
				double bestVal = 0;
				Line bestLine = null;
				for(Line curLine = line; curLine != null; curLine = curLine.nextLine)
				{
					if (first)
					{
						bestVal = curLine.val;
						bestLine = curLine;
						first = false;
					} else if (curLine.val > bestVal)
					{
						bestVal = curLine.val;
						bestLine = curLine;
					}
				}

				// remove bestLine from the list
				if (bestLine.prevLine == null) line = bestLine.nextLine; else
					bestLine.prevLine.nextLine = bestLine.nextLine;
				if (bestLine.nextLine != null)
					bestLine.nextLine.prevLine = bestLine.prevLine;

				// insert at the start of this list
				if (newLine != null) newLine.prevLine = bestLine;
				bestLine.nextLine = newLine;
				bestLine.prevLine = null;
				newLine = bestLine;
			}
			return newLine;
		}

		/**
		 * create a new line with the element object and add it to the beginning of
		 * the given line
		 */
		private Line makeObjectLine(Line line, List<GeomObj> objectList)
		{
			Line newLine = new Line();
			newLine.index = lineIndex++;
			newLine.nextLine = line;
			newLine.prevLine = null;
			newLine.firstObject = null;
			GeomObj lastObject = null;
			for(GeomObj gO : objectList)
			{
				if (lastObject == null) newLine.firstObject = gO; else
					lastObject.nextObject = gO;
				lastObject = gO;
			}
			if (line != null) line.prevLine = newLine;
			return newLine;
		}

		private void createObjects(NodeInst ni, List<GeomObj> thisObject, List<GeomObj> otherObject, HashSet<NodeInst> nodesSeen,
			HashMap<ArcInst,Integer> arcIndices, HashMap<PortProto,Integer> portIndices)
		{
			// if node has already been examined, quit now
			if (nodesSeen.contains(ni)) return;
			nodesSeen.add(ni);

			// if this is the first object, add it
			if (thisObject.size() == 0)
				thisObject.add(makeNodeInstObject(ni, null, GenMath.MATID, 0,0,0,0, arcIndices, portIndices));
			GeomObj firstObject = thisObject.get(0);
			double stLow, stHigh;
			if (curAxis == Axis.HORIZONTAL)
			{
				stLow = firstObject.lowx;
				stHigh = firstObject.highx;
			} else
			{
				stLow = firstObject.lowy;
				stHigh = firstObject.highy;
			}

			// for each arc on node, find node at other end and add to object
			for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
			{
				Connection con = it.next();
				ArcInst ai = con.getArc();
				NodeInst otherEnd = ai.getTailPortInst().getNodeInst();
				if (otherEnd == ni) otherEnd = ai.getHeadPortInst().getNodeInst();

				// stop if other end has already been examined
				if (nodesSeen.contains(otherEnd)) continue;
				GeomObj newObject = makeArcInstObject(ai, null, GenMath.MATID, 0,0,0,0, arcIndices);

				GeomObj secondObject = makeNodeInstObject(otherEnd, null, GenMath.MATID, 0,0,0,0, arcIndices, portIndices);

				double bdLow, bdHigh;
				boolean partOfLine = false;
				if (curAxis == Axis.HORIZONTAL)
				{
					bdLow = secondObject.lowx;
					bdHigh = secondObject.highx;
					if (ai.getHeadLocation().getX() == ai.getTailLocation().getX()) partOfLine = true;
					if (DBMath.doublesEqual(ni.getAnchorCenterX(), otherEnd.getAnchorCenterX())) partOfLine = true;
				} else
				{
					bdLow = secondObject.lowy;
					bdHigh = secondObject.highy;
					if (ai.getHeadLocation().getY() == ai.getTailLocation().getY()) partOfLine = true;
					if (DBMath.doublesEqual(ni.getAnchorCenterY(), otherEnd.getAnchorCenterY())) partOfLine = true;
				}
				if (bdHigh > stLow && bdLow < stHigh) partOfLine = true;
				if (partOfLine)
				{
					thisObject.add(newObject);
					thisObject.add(secondObject);
					createObjects(otherEnd, thisObject, otherObject, nodesSeen, arcIndices, portIndices);
				} else
				{
					// arcs in object to be used later in fixed_non_fixed
					otherObject.add(newObject);
				}
			}
		}

		/**
		 * Method to build a object describing node "ni" in axis "axis".  If "object"
		 * is null, this node is at the top level, and a new GeomObj should be
		 * constructed for it.  Otherwise, the node is in a subcell and it must be
		 * transformed through "newTrans" and clipped to the two protection frames
		 * defined by "low1" to "high1" and "low2" to "high2" before being added to
		 * "object".
		 */
		private GeomObj makeNodeInstObject(NodeInst ni, GeomObj object, AffineTransform newTrans,
			double low1, double high1, double low2, double high2, HashMap<ArcInst,Integer> arcIndices, HashMap<PortProto,Integer> portIndices)
		{
			GeomObj newObject = object;
			if (newObject == null)
			{
				newObject = new GeomObj();
				newObject.inst = ni;
				newObject.nextObject = null;
				newObject.firstPolyList = null;
				Rectangle2D bounds = ni.getBounds();
				newObject.outerLowx = bounds.getMinX();
				newObject.outerHighx = bounds.getMaxX();
				newObject.outerLowy = bounds.getMinY();
				newObject.outerHighy = bounds.getMaxY();
				if (ni.isCellInstance())
				{
					newObject.lowx = newObject.outerLowx;
					newObject.highx = newObject.outerHighx;
					newObject.lowy = newObject.outerLowy;
					newObject.highy = newObject.outerHighy;
				} else
				{
					double cX = ni.getTrueCenterX();
					double cY = ni.getTrueCenterY();
					double sX = ni.getXSize();
					double sY = ni.getYSize();
					SizeOffset so = ni.getSizeOffset();
					double lX = cX - sX/2 + so.getLowXOffset();
					double hX = cX + sX/2 - so.getHighXOffset();
					double lY = cY - sY/2 + so.getLowYOffset();
					double hY = cY + sY/2 - so.getHighYOffset();
					Rectangle2D bound = new Rectangle2D.Double(lX, lY, hX-lX, hY-lY);
					GenMath.transformRect(bound, ni.rotateOut());
					newObject.lowx = bound.getMinX();
					newObject.highx = bound.getMaxX();
					newObject.lowy = bound.getMinY();
					newObject.highy = bound.getMaxY();
				}
			}

			// propagate global network info to local port prototypes on "ni"
			HashMap<PortProto,Integer> localPortIndices = fillNode(ni, arcIndices, portIndices);

			// create pseudo-object for complex ni
			if (ni.isCellInstance())
			{
				Cell subCell = (Cell)ni.getProto();

				// compute transformation matrix from subnode to this space
				AffineTransform t1 = ni.rotateOut(newTrans);
				AffineTransform trans = ni.translateOut(newTrans);

				/*
				 * create a line for cell "ni->proto" at the current location and
				 * translation.  Put only the instances which are within maxBoundary
				 * of the perimeter of the cell.
				 */
				HashMap<ArcInst,Integer> localArcIndices = subCellSmash(subCell, localPortIndices);

				// compute protection frame if at the top level
				if (object == null)
				{
					Rectangle2D bounds = ni.getBounds();
					if (curAxis == Axis.HORIZONTAL)
					{
						low1 = bounds.getMinX();
						high1 = bounds.getMinX() + maxBoundary;
						low2 = bounds.getMaxX() - maxBoundary;
						high2 = bounds.getMaxX();
					} else
					{
						low1 = bounds.getMinY();
						high1 = bounds.getMinY() + maxBoundary;
						low2 = bounds.getMaxY() - maxBoundary;
						high2 = bounds.getMaxY();
					}
				}

				// include polygons from those nodes and arcs in the protection frame
				for(Iterator<NodeInst> it = subCell.getNodes(); it.hasNext(); )
				{
					NodeInst subNi = it.next();
					makeNodeInstObject(subNi, newObject, trans,
						low1, high1, low2, high2, localArcIndices, localPortIndices);
				}
				for(Iterator<ArcInst> it = subCell.getArcs(); it.hasNext(); )
				{
					ArcInst subAi = it.next();
					makeArcInstObject(subAi, newObject, trans,
						low1, high1, low2, high2, localArcIndices);
				}
			} else
			{
				AffineTransform trans = ni.rotateOut(newTrans);
				Technology tech = ni.getProto().getTechnology();
				Poly [] polys = tech.getShapeOfNode(ni, null, null, true, true, null);
				int tot = polys.length;
				for(int j=0; j<tot; j++)
				{
					Poly poly = polys[j];
					poly.transform(trans);

					// make sure polygon is within protection frame
					if (object != null)
					{
						Rectangle2D bounds = poly.getBounds2D();
						if (curAxis == Axis.HORIZONTAL)
						{
							if ((bounds.getMaxX() < low1 || bounds.getMinX() > high1) &&
								(bounds.getMaxX() < low2 || bounds.getMinX() > high2)) continue;
						} else
						{
							if ((bounds.getMaxY() < low1 || bounds.getMinY() > high1) &&
								(bounds.getMaxY() < low2 || bounds.getMinY() > high2)) continue;
						}
					}

					int pIndex = -1;
					if (poly.getPort() != null)
					{
						Integer i = localPortIndices.get(poly.getPort());
						if (i != null) pIndex = i.intValue();
					}
					addPolyToPolyList(poly, newObject, pIndex, tech);
				}
			}
			return newObject;
		}

		private HashMap<PortProto,Integer> fillNode(NodeInst ni, HashMap<ArcInst,Integer> arcIndices, HashMap<PortProto,Integer> portIndices)
		{
			// initialize network information for this node instance
			HashMap<PortProto,Integer> localPortIndices = new HashMap<PortProto,Integer>();

			// set network numbers from arcs
			for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
			{
				Connection con = it.next();
				ArcInst ai = con.getArc();
				PortProto pp = con.getPortInst().getPortProto();
				if (localPortIndices.get(pp) != null) continue;
				Integer aIndex = arcIndices.get(ai);
				localPortIndices.put(pp, aIndex);
			}

			// set network numbers from exports
			for(Iterator<Export> it = ni.getExports(); it.hasNext(); )
			{
				Export e = it.next();
				PortProto pp = e.getOriginalPort().getPortProto();
				if (localPortIndices.get(pp) != null) continue;
				Integer pIndex = portIndices.get(e);
				localPortIndices.put(pp, pIndex);
			}

			// look for unconnected ports and assign new network numbers
			Netlist nl = ni.getParent().getUserNetlist();
			for(Iterator<PortProto> it = ni.getProto().getPorts(); it.hasNext(); )
			{
				PortProto pp = it.next();
				if (localPortIndices.get(pp) != null) continue;

				// look for similar connected port
				boolean found = false;
				Network net = nl.getNetwork(ni, pp, 0);
				for(Iterator<PortProto> oIt = ni.getProto().getPorts(); oIt.hasNext(); )
				{
					PortProto oPp = oIt.next();
					Network oNet = nl.getNetwork(ni, oPp, 0);
					if (oNet == net)
					{
						Integer oIndex = localPortIndices.get(oPp);
						if (oIndex != null)
						{
							localPortIndices.put(pp, oIndex);
							found = true;
							break;
						}
					}
				}
				if (!found) localPortIndices.put(pp, new Integer(flatIndex++));
			}
			return localPortIndices;
		}

		/**
		 * Method to build a "object" structure that describes arc "ai".  If "object"
		 * is null, this arc is at the top level, and a new GeomObj should be
		 * constructed for it.  Otherwise, the arc is in a subcell and it must be
		 * transformed through "newTrans" and clipped to the two protection frames
		 * defined by "low1", "high1" and "low2", "high2" before being added to "object".
		 */
		private GeomObj makeArcInstObject(ArcInst ai, GeomObj object, AffineTransform newTrans,
			double low1, double high1, double low2, double high2, HashMap<ArcInst,Integer> arcIndices)
		{
			// create the object if at the top level
			GeomObj newObject = object;
			if (object == null)
			{
				newObject = new GeomObj();
				newObject.inst = ai;
				newObject.nextObject = null;
				newObject.firstPolyList = null;
                Poly poly = ai.makePoly(ai.getWidth() - ai.getProto().getWidthOffset(), Poly.Type.CLOSED);
				Rectangle2D bounds = poly.getBounds2D();
				newObject.lowx = bounds.getMinX();
				newObject.highx = bounds.getMaxX();
				newObject.lowy = bounds.getMinY();
				newObject.highy = bounds.getMaxY();

				poly = ai.makePoly(ai.getWidth(), Poly.Type.CLOSED);
				bounds = poly.getBounds2D();
				newObject.outerLowx = bounds.getMinX();
				newObject.outerHighx = bounds.getMaxX();
				newObject.outerLowy = bounds.getMinY();
				newObject.outerHighy = bounds.getMaxY();
			}

			Technology tech = ai.getProto().getTechnology();
			Poly [] polys = tech.getShapeOfArc(ai);
			int tot = polys.length;
			for(int j=0; j<tot; j++)
			{
				Poly poly = polys[j];
				if (poly.getLayer() == null) continue;

				// make sure polygon is within protection frame
				if (object != null)
				{
					poly.transform(newTrans);
					Rectangle2D bounds = poly.getBounds2D();
					if (curAxis == Axis.HORIZONTAL)
					{
						if ((bounds.getMaxX() < low1 || bounds.getMinX() > high1) &&
							(bounds.getMaxX() < low2 || bounds.getMinX() > high2)) continue;
					} else
					{
						if ((bounds.getMaxY() < low1 || bounds.getMinY() > high1) &&
							(bounds.getMaxY() < low2 || bounds.getMinY() > high2)) continue;
					}
				}

				// add the polygon
				int aIndex = -1;
				Integer iv = arcIndices.get(ai);
				if (iv != null) aIndex = iv.intValue();
				addPolyToPolyList(poly, newObject, aIndex, tech);
			}
			return newObject;
		}

		/**
		 * Method to link polygon "poly" into object "object" with network number
		 * "networkNum"
		 */
		private void addPolyToPolyList(Poly poly, GeomObj object, int networkNum, Technology tech)
		{
			PolyList newPolyList = new PolyList();
			newPolyList.poly = poly;
			newPolyList.tech = tech;
			newPolyList.networkNum = networkNum;
			newPolyList.nextPolyList = object.firstPolyList;
			object.firstPolyList = newPolyList;
		}

		/**
		 * copy network information from ports to arcs in cell "topCell"
		 */
		private HashMap<ArcInst,Integer> subCellSmash(Cell topCell, HashMap<PortProto,Integer> portIndices)
		{
			Netlist nl = topCell.getUserNetlist();

			// first erase the arc node information
			HashMap<ArcInst,Integer> arcIndices = new HashMap<ArcInst,Integer>();

			// copy network information from ports to arcs
			for(Iterator<ArcInst> it = topCell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = it.next();

				// ignore arcs that have already been numbered
				if (arcIndices.get(ai) != null) continue;

				// see if this arc connects to a port
				Network aNet = nl.getNetwork(ai, 0);
				boolean found = false;
				for(Iterator<PortProto> pIt = topCell.getPorts(); pIt.hasNext(); )
				{
					Export pp = (Export)pIt.next();
					Integer pIndex = portIndices.get(pp);
					Network pNet = nl.getNetwork(pp, 0);
					if (pNet == aNet)
					{
						// propagate port numbers into all connecting arcs
						for(Iterator<ArcInst> aIt = topCell.getArcs(); aIt.hasNext(); )
						{
							ArcInst oAi = aIt.next();
							Network oANet = nl.getNetwork(oAi, 0);
							if (oANet == aNet) arcIndices.put(oAi, pIndex);
						}
						found = true;
						break;
					}
				}

				// if not connected to a port, this is an internal network
				if (!found)
				{
					// copy new net number to all of these connected arcs
					Integer pIndex = new Integer(flatIndex++);
					for(Iterator<ArcInst> aIt = topCell.getArcs(); aIt.hasNext(); )
					{
						ArcInst oAi = aIt.next();
						Network oANet = nl.getNetwork(oAi, 0);
						if (oANet == aNet) arcIndices.put(oAi, pIndex);
					}
				}
			}
			return arcIndices;
		}
	}
}
