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
import com.sun.electric.technology.DRCRules;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.routing.Routing;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * This is the Compaction tool.
 *
 * When compacting cell instances, the system only examines polygons
 * within a protection frame of the cell border.  This frame is the largest
 * design rule distance in the technology.  If the cell border is irregular,
 * there may be objects that are not seen, causing the cell to overlap
 * more than it should.
 */
public class Compaction extends Listener
{
	/** the Compaction tool. */		public static Compaction tool = new Compaction();

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
	 * Method to mimic the currently selected ArcInst.
	 */
	public static void compactNow()
	{
		Cell cell = WindowFrame.getCurrentCell();
		if (cell == null) return;

		// do the compaction
		CompactCell job = new CompactCell(cell);
	}

	/****************************** OPTIONS ******************************/

	private static Pref cacheAllowSpreading = Pref.makeBooleanPref("AllowSpreading", Compaction.tool.prefs, false);
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

	/****************************** COMPACTION CONTROL ******************************/

	/**
	 * Class to compact a cell.
	 */
	private static class CompactCell extends Job
	{
		Cell cell;

		private static final double DEFAULT_VAL = -99999999;
		private static final int HORIZONTAL  =        0;
		private static final int VERTICAL    =        1;

		static class COMPPOLYLIST
		{
			Poly          poly;
			Technology       tech;
			int            networknum;
			COMPPOLYLIST nextpolylist;
		};

		static class OBJECT
		{
			Geometric inst;
			boolean         isnode;
			COMPPOLYLIST    firstpolylist;
			double          lowx, highx, lowy, highy;
			OBJECT nextobject;
		};

		static class LINE
		{
			double        val;
			double        low, high;
			double        top, bottom;
			OBJECT        firstobject;
			LINE nextline;
			LINE prevline;
		};

		private double    com_maxboundary, com_lowbound;
		private int    com_flatindex;			/* counter for unique network numbers */

		private CompactCell(Cell cell)
		{
			super("Compact Cell", Compaction.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			startJob();
		}

		public boolean doIt()
		{

//			// special handling for technology-edit cells
//			if (el_curwindowpart != null && cell != null &&
//				(el_curwindowpart->state&WINDOWMODE) == WINDOWTECEDMODE)
//			{
//				// special compaction of a technology edit cell can be done by the tec-editor
//				arg[0] = "compact-current-cell";
//				us_tecedentry(1, arg);
//				return;
//			}

			// alternate vertical then horizontal compaction
			for(;;)
			{
				boolean vChange = com_examineonecell(cell, VERTICAL);
				boolean hChange = com_examineonecell(cell, VERTICAL);
				if (!vChange && !hChange) break;
			}
			return true;
		}

		/**
		 * Method to do vertical compaction (if "axis" is VERTICAL) or horizontal
		 * compaction (if "axis" is HORIZONTAL) to cell "np".  Displays state if
		 * "verbose" is nonzero.  Returns true if a change was made.
		 */
		private boolean com_examineonecell(Cell np, int axis)
		{
			// determine maximum drc surround for entire technology
			com_maxboundary = DRC.getWorstSpacingDistance(Technology.getCurrent());

			if (axis == HORIZONTAL) System.out.println("Doing a horizontal compaction"); else
				System.out.println("Doing a vertical compaction");

			// number ports of cell "np"
			HashMap portIndices = new HashMap();
			com_flatindex = 1;
			Netlist nl = np.getUserNetlist();
			for(Iterator it = np.getPorts(); it.hasNext(); )
			{
				Export pp = (Export)it.next();
				Network net = nl.getNetwork(pp, 0);

				// see if this port is on the same net as previously examined one
				Export found = null;
				for(Iterator oIt = np.getPorts(); oIt.hasNext(); )
				{
					Export oPp = (Export)oIt.next();
					if (oPp == pp) break;
					Network oNet = nl.getNetwork(oPp, 0);
					if (net == oNet) { found = oPp;   break; }
				}
				if (found != null)
				{
					Integer oIndex = (Integer)portIndices.get(found);
					portIndices.put(pp, oIndex);
				} else portIndices.put(pp, new Integer(com_flatindex++));
			}

			// copy port numbering onto arcs
			HashMap arcIndices = com_subsmash(np, portIndices);

			// clear "seen" information on every node
			HashSet nodesSeen = new HashSet();

			// clear object information
			LINE linecomp = null;
			OBJECT [] otherobject = new OBJECT[1];
			otherobject[0] = null;

			// now check every object
			for(Iterator it = np.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();

				// clear "thisobject" before calling com_createobject
				OBJECT [] thisobject = new OBJECT[1];
				thisobject[0] = null;
				com_createobjects(ni, axis, thisobject, otherobject, nodesSeen, arcIndices, portIndices, nl);

				// create object of layout
				if (thisobject[0] != null)
					linecomp = com_make_object_line(linecomp, thisobject[0]);
			}

			// create list of perpendicular line which need to be set stretchable
			LINE linestretch = null;
			if (otherobject[0] != null)
				linestretch = com_make_object_line(null, otherobject[0]);

			// sort the compacting line of objects
			linecomp = com_sort(linecomp, axis);

			// compute bounds for each line
			for(LINE cur_line = linecomp; cur_line != null; cur_line = cur_line.nextline)
				com_computeline_hi_and_low(cur_line, axis);

			// prevent the stretching line from sliding
			HashSet clearedArcs = com_noslide(linestretch);

			// set rigidity properly
			com_fixed_nonfixed(linecomp, linestretch);

			// do the compaction
			com_lowbound = com_findleastlow(linecomp, axis);
			boolean change = com_lineup_firstrow(linecomp, linestretch, axis, com_lowbound);
			change = com_compact(linecomp, linestretch, axis, change, np);

			// restore rigidity if no changes were made
			if (!change) com_undo_fixed_nonfixed(linecomp, linestretch);

			// allow the streteching line to slide again
			com_slide(clearedArcs);

			return change;
		}

		private boolean com_compact(LINE line, LINE other_line, int axis, boolean change, Cell cell)
		{
			boolean spread = isAllowsSpreading();

			// loop through all lines that may compact
			for(LINE cur_line = line.nextline; cur_line != null; cur_line = cur_line.nextline)
			{
				// look at every object in the line that may compact
				double best_motion = DEFAULT_VAL;
				OBJECT thisoreason = null, otheroreason = null;
				for(OBJECT cur_object = cur_line.firstobject; cur_object != null; cur_object = cur_object.nextobject)
				{
					// look at all previous lines
					for(LINE prev_line = cur_line.prevline; prev_line != null; prev_line = prev_line.prevline)
					{
						// no need to test this line if it is farther than best motion
						if (best_motion != DEFAULT_VAL &&
							cur_line.low - prev_line.high > best_motion) continue;

						// simple object compaction
						OBJECT [] oReason = new OBJECT[1];
						COMPPOLYLIST [] oPReason = new COMPPOLYLIST[1];
						COMPPOLYLIST [] pReason = new COMPPOLYLIST[1];
						double this_motion = com_checkinst(cur_object, prev_line, axis, oReason, oPReason, pReason, cell);
						if (this_motion == DEFAULT_VAL) continue;
						if (best_motion == DEFAULT_VAL || this_motion < best_motion)
						{
							best_motion = this_motion;
							if (oReason[0] != null)
							{
								thisoreason = oReason[0];
								otheroreason = cur_object;
							}
						}
					}
				}

				if (best_motion == DEFAULT_VAL)
				{
					// no constraints: allow overlap
					best_motion = cur_line.low - com_lowbound;
				}
				if (best_motion > 0 || (spread && best_motion < 0))
				{
					if (axis == HORIZONTAL)
						change = com_move(cur_line, best_motion, 0, change); else
							change = com_move(cur_line, 0, best_motion, change);
					com_fixed_nonfixed(line, other_line);
				}
			}
			return change;
		}

		private double com_checkinst(OBJECT object, LINE line, int axis, OBJECT [] oReason,
			COMPPOLYLIST [] oPReason, COMPPOLYLIST [] pReason, Cell cell)
		{
			double best_motion = DEFAULT_VAL;
			oReason[0] = null;
			for(COMPPOLYLIST polys = object.firstpolylist; polys != null; polys = polys.nextpolylist)
			{
				Poly poly = polys.poly;

				// translate any pseudo layers for this node
				Layer layer = poly.getLayer().getNonPseudoLayer();

				// find distance line can move toward this poly
				OBJECT [] subOReason = new OBJECT[1];
				COMPPOLYLIST [] subPReason = new COMPPOLYLIST[1];
				double this_motion = com_minseparate(object, layer, polys, line, axis,
					subOReason, subPReason, cell);
				if (this_motion == DEFAULT_VAL) continue;
				if (best_motion == DEFAULT_VAL || this_motion < best_motion)
				{
					best_motion = this_motion;
					oReason[0] = subOReason[0];
					oPReason[0] = subPReason[0];
					pReason[0] = polys;
				}
			}
			return best_motion;
		}

		/**
		 * Method finds the minimum distance which is necessary between polygon
		 * "obj" (from object "object" on layer "nlayer" with network connectivity
		 * "nindex") and the previous line in "line".  It returns the amount
		 * to move this object to get it closest to the line (DEFAULT_VAL if they can
		 * overlap).  The object "reason" is set to the object that is causing the
		 * constraint.
		 */
		private double com_minseparate(OBJECT object, Layer nlayer, COMPPOLYLIST npolys,
			LINE line, int axis, OBJECT [] oReason, COMPPOLYLIST [] pReason, Cell cell)
		{
			oReason[0] = null;
			Poly npoly = npolys.poly;
			double nminsize = npoly.getMinSize();
			Technology tech = npolys.tech;
			int nindex = npolys.networknum;

			// see how far around the box it is necessary to search
			double bound = DRC.getMaxSurround(nlayer, Double.MAX_VALUE);

			// if there is no separation, allow them to sit on top of each other
			if (bound < 0) return DEFAULT_VAL;

			// can only handle orthogonal rectangles for now
			Rectangle2D box = npoly.getBox();
			if (box == null) return bound;

			double best_motion = DEFAULT_VAL;
			double geom_lo = object.lowy;
			if (axis == HORIZONTAL) geom_lo = object.lowx;

			// search the line
			for(OBJECT cur_object = line.firstobject; cur_object != null; cur_object = cur_object.nextobject)
			{
				if (cur_object.isnode)
				{
					double ni_hi = cur_object.highy;
					if (axis == HORIZONTAL) ni_hi = cur_object.highx;

					if ((axis == HORIZONTAL && com_in_bound(box.getMinY()-bound, box.getMaxY()+bound,
						cur_object.lowy, cur_object.highy)) ||
							(axis == VERTICAL && com_in_bound(box.getMinX()-bound, box.getMaxX()+bound,
								cur_object.lowx, cur_object.highx)))
					{
						// examine every layer in this object
						for(COMPPOLYLIST polys = cur_object.firstpolylist; polys != null; polys = polys.nextpolylist)
						{
							// don't check between technologies
							if (polys.tech != tech) continue;

							Poly poly = polys.poly;
							Layer layer = poly.getLayer().getNonPseudoLayer();
							int pindex = polys.networknum;

							// see whether the two objects are electrically connected
							boolean con = false;
							if (pindex == nindex && pindex != -1) con = true;
							Rectangle2D polyBox = poly.getBox();
							if (polyBox == null)
							{
								double this_motion = geom_lo - ni_hi - bound;
								if (this_motion == DEFAULT_VAL) continue;
								if (this_motion < best_motion || best_motion == DEFAULT_VAL)
								{
									best_motion = this_motion;
									oReason[0] = cur_object;
									pReason[0] = polys;
								}
								continue;
							}

							// see how close they can get
							double minsize = poly.getMinSize();
							double dist = -1;
							DRCRules.DRCRule rule = DRC.getSpacingRule(nlayer, layer, con, false, 0);
							if (rule != null) dist = rule.value;
							if (dist < 0) continue;

							/*
							 * special rule for ignoring distance:
							 *   the layers are the same and either:
							 *     they connect and are *NOT* contact layers
							 *   or:
							 *     they don't connect and are implant layers (substrate/well)
							 */
							if (nlayer == layer)
							{
								Layer.Function fun = nlayer.getFunction();
								if (con)
								{
									if (!fun.isContact()) continue;
								} else
								{
									if (fun == Layer.Function.SUBSTRATE || fun == Layer.Function.WELL ||
										fun == Layer.Function.IMPLANT) continue;
								}
							}

							/*
							 * if the two layers are located on the y-axis so
							 * that there is no necessary contraint between them
							 */
							if ((axis == HORIZONTAL && !com_in_bound(box.getMinY()-dist, box.getMaxY()+dist, polyBox.getMinY(), polyBox.getMaxY())) ||
								(axis == VERTICAL && !com_in_bound(box.getMinX()-dist, box.getMaxX()+dist, polyBox.getMinX(), polyBox.getMaxX())))
									continue;

							// check the distance
							double this_motion = com_check(nlayer, nindex, object, polyBox, layer, pindex, cur_object, box, dist, axis);
							if (this_motion == DEFAULT_VAL) continue;
							if (this_motion < best_motion || best_motion == DEFAULT_VAL)
							{
								best_motion = this_motion;
								oReason[0] = cur_object;
								pReason[0] = polys;
							}
						}
					}
				} else
				{
					double ai_hi = cur_object.highy;
					if (axis == HORIZONTAL) ai_hi = cur_object.highx;

					if ((axis == HORIZONTAL && com_in_bound(box.getMinY()-bound, box.getMaxY()+bound,
						cur_object.lowy, cur_object.highy)) ||
							(axis == VERTICAL && com_in_bound(box.getMinX()-bound, box.getMaxX()+bound,
								cur_object.lowx, cur_object.highx)))
					{
						// prepare to examine every layer in this arcinst
						for(COMPPOLYLIST polys = cur_object.firstpolylist; polys != null; polys = polys.nextpolylist)
						{
							// don't check between technologies
							if (polys.tech != tech) continue;

							Poly poly = polys.poly;

							// see whether the two objects are electrically connected
							int pindex = polys.networknum;
							boolean con = false;
							if (nindex == -1 || pindex == nindex) con = true;

							// warning: non-manhattan arcs are ignored here
							Rectangle2D polyBox = poly.getBox();
							if (polyBox == null)
							{
								double this_motion = geom_lo - ai_hi - bound;
								if (this_motion == DEFAULT_VAL) continue;
								if (this_motion < best_motion || best_motion == DEFAULT_VAL)
								{
									best_motion = this_motion;
									oReason[0] = cur_object;
									pReason[0] = polys;
								}
								continue;
							}

							// see how close they can get
							double minsize = poly.getMinSize();
							double dist = -1;
							DRCRules.DRCRule rule = DRC.getSpacingRule(nlayer, poly.getLayer(), con, false, 0);
							if (rule != null) dist = rule.value;
							if (dist < 0) continue;

							/*
							 * if the two layers are so located on the y-axis so
							 * that there is no necessary contraint between them
							 */
							if ((axis == HORIZONTAL && !com_in_bound(box.getMinY()-dist, box.getMaxY()+dist, polyBox.getMinY(), polyBox.getMaxY())) ||
								(axis == VERTICAL && !com_in_bound(box.getMinX()-dist, box.getMaxX()+dist, polyBox.getMinX(), polyBox.getMaxX())))
									continue;

							// check the distance
							double this_motion = com_check(nlayer, nindex, object, polyBox, poly.getLayer(), pindex, cur_object, box, dist, axis);
							if (this_motion == DEFAULT_VAL) continue;
							if (this_motion < best_motion || best_motion == DEFAULT_VAL)
							{
								best_motion = this_motion;
								oReason[0] = cur_object;
								pReason[0] = polys;
							}
						}
					}
				}
			}
			return best_motion;
		}

		/**
		 * Method to see if the object in "object1" on layer "layer1" with electrical
		 * index "index1" comes within "dist" from the object in "object2" on layer
		 * "layer2" with electrical index "index2" in the perpendicular axis to "axis".
		 * The bounds of object "object1" are (lx1-hx1,ly1-hy1), and the bounds of object
		 * "object2" are (lx2-hx2,ly2-hy2).  If the objects are in bounds, the spacing
		 * between them is returned.  Otherwise, DEFAULT_VAL is returned.
		 */
		private double com_check(Layer layer1, int index1, OBJECT object1, Rectangle2D bound1,
			Layer layer2, int index2, OBJECT object2, Rectangle2D bound2, double dist, int axis)
		{
			// crop out parts of a box covered by a similar layer on the other node
			if (object1.isnode)
			{
				if (com_cropnodeinst(object1.firstpolylist, bound2, layer2, index2))
					return DEFAULT_VAL;
			}
			if (object2.isnode)
			{
				if (com_cropnodeinst(object2.firstpolylist, bound1, layer1, index1))
					return DEFAULT_VAL;
			}

			// now compare the box extents
			if (axis == HORIZONTAL)
			{
				if (bound1.getMaxY()+dist > bound2.getMinY() && bound1.getMinY()-dist < bound2.getMaxY()) return(bound2.getMinX() - bound1.getMaxX() - dist);
			} else if (bound1.getMaxX()+dist > bound2.getMinX() && bound1.getMinX()-dist < bound2.getMaxX()) return(bound2.getMinY() - bound1.getMaxY() - dist);
			return DEFAULT_VAL;
		}

		/**
		 * Method to crop the box on layer "nlayer", electrical index "nindex"
		 * and bounds (lx-hx, ly-hy) against the nodeinst "ni".  Only those layers
		 * in the nodeinst that are the same layer and the same electrical index
		 * are checked.  The routine returns true if the bounds are reduced
		 * to nothing.
		 */
		private boolean com_cropnodeinst(COMPPOLYLIST polys, Rectangle2D bound, Layer nlayer, int nindex)
		{
			for(COMPPOLYLIST cur_polys = polys; cur_polys != null; cur_polys = cur_polys.nextpolylist)
			{
				if (cur_polys.networknum != nindex) continue;
				if (cur_polys.poly.getLayer() != nlayer) continue;
				Rectangle2D polyBox = cur_polys.poly.getBox();
				if (polyBox == null) continue;
				int temp = Poly.cropBox(bound, polyBox);
				if (temp > 0) return true;
			}
			return false;
		}

		private boolean com_in_bound(double ll, double lh, double rl, double rh)
		{
			if (rh > ll && rl < lh) return true;
			return false;
		}

		private boolean com_lineup_firstrow(LINE line, LINE other_line, int axis, double lowbound)
		{
			boolean change = false;
			double i = line.low - lowbound;
			if (i > 0)
			{
				if (axis == HORIZONTAL) change = com_move(line, i, 0, change); else
					change = com_move(line, 0, i, change);
				com_fixed_nonfixed(line, other_line);
			}
			return change;
		}

		/**
		 * moves a object of instances distance (movex, movey), and returns a true if
		 * there is actually a move
		 */
		private boolean com_move(LINE line, double movex, double movey, boolean change)
		{
			double move = movex;
			if (movex == 0) move = movey;
			if (line == null) return false;
			if (!change && move != 0)
			{
	//			(void)asktool(us_tool, x_("clear"));
				change = true;
			}

			for(OBJECT cur_object = line.firstobject; cur_object != null; cur_object = cur_object.nextobject)
			{
				if (cur_object.isnode)
				{
					NodeInst ni = (NodeInst)cur_object.inst;
					ni.modifyInstance(-movex, -movey, 0, 0, 0);
					break;
				}
			}

			for(OBJECT cur_object = line.firstobject; cur_object != null; cur_object = cur_object.nextobject)
			{
				cur_object.lowx -= movex;
				cur_object.highx -= movex;
				cur_object.lowy -= movey;
				cur_object.highy -= movey;
				for(COMPPOLYLIST polys = cur_object.firstpolylist; polys != null; polys = polys.nextpolylist)
				{
					Point2D [] points = polys.poly.getPoints();
					for(int i=0; i<points.length; i++)
					{
						points[i].setLocation(points[i].getX() - movex, points[i].getY() - movey);
					}
				}
			}
			line.high -= move;
			line.low -= move;

			return change;
		}

		/**
		 * find least low of the line. re-set first line low in the list
		 * finds the smallest low value (lowx for VERTICAL, lowy for HORIZ case)
		 * stores it in line->low.
		 */
		private double com_findleastlow(LINE line, int axis)
		{
			if (line == null) return 0;

			// find smallest low for the each object
			boolean first_time = true;
			double low = 0;
			for(OBJECT cur_object = line.firstobject; cur_object != null; cur_object = cur_object.nextobject)
			{
				if (!cur_object.isnode) continue;
				double thislow = cur_object.lowy;
				if (axis == HORIZONTAL) thislow = cur_object.lowx;

				if (!first_time) low = Math.min(low, thislow); else
				{
					low = thislow;
					first_time = false;
				}
			}
			line.low = low;

			return low;
		}

		/**
		 * Method to temporarily make all arcs in fixline rigid and those
		 * in nfixline nonrigid in order to move fixline over
		 */
		private void com_fixed_nonfixed(LINE fixline, LINE nfixline)
		{
			for(LINE cur_line = fixline; cur_line != null; cur_line = cur_line.nextline)
			{
				for(OBJECT cur_object = cur_line.firstobject; cur_object != null; cur_object = cur_object.nextobject)
				{
					if (!cur_object.isnode)    // arc rigid
					{
						Layout.setTempRigid((ArcInst)cur_object.inst, true);
					}
				}
			}
			for(LINE cur_line = nfixline; cur_line != null; cur_line = cur_line.nextline)
			{
				for(OBJECT cur_object = cur_line.firstobject; cur_object != null; cur_object = cur_object.nextobject)
				{
					if (!cur_object.isnode)   // arc unrigid
					{
						Layout.setTempRigid((ArcInst)cur_object.inst, false);
					}
				}
			}
		}

		/**
		 * Method to reset temporary changes to arcs in fixline and nfixline
		 * so that they are back to their default values.
		 */
		private void com_undo_fixed_nonfixed(LINE fixline, LINE nfixline)
		{
			for(LINE cur_line = fixline; cur_line != null; cur_line = cur_line.nextline)
			{
				for(OBJECT cur_object = cur_line.firstobject; cur_object != null; cur_object = cur_object.nextobject)
				{
					if (!cur_object.isnode)
					{
						Layout.removeTempRigid((ArcInst)cur_object.inst);
					}
				}
			}
			for(LINE cur_line = nfixline; cur_line != null; cur_line = cur_line.nextline)
			{
				for(OBJECT cur_object = cur_line.firstobject; cur_object != null; cur_object = cur_object.nextobject)
				{
					if (!cur_object.isnode)
					{
						Layout.removeTempRigid((ArcInst)cur_object.inst);
					}
				}
			}
		}

		/**
		 * set the CANTSLIDE bit of userbits for each object in line so that this
		 * line will not slide.
		 */
		private HashSet com_noslide(LINE line)
		{
			HashSet clearedArcs = new HashSet();
			for(LINE cur_line = line; cur_line != null; cur_line = cur_line.nextline)
			{
				for(OBJECT cur_object = cur_line.firstobject; cur_object != null;
					cur_object = cur_object.nextobject)
				{
					if (!cur_object.isnode)
					{
						ArcInst ai = (ArcInst)cur_object.inst;
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
		private void com_slide(HashSet clearedArcs)
		{
			for(Iterator it = clearedArcs.iterator(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				ai.setSlidable(true);
			}
		}

		private void com_computeline_hi_and_low(LINE line, int axis)
		{
			// find smallest and highest vals for the each object
			boolean first_time = true;
			double lx = 0, hx = 0, ly = 0, hy = 0;
			for(OBJECT cur_object = line.firstobject; cur_object != null; cur_object = cur_object.nextobject)
			{
				if (!cur_object.isnode) continue;
				if (first_time)
				{
					lx = cur_object.lowx;
					hx = cur_object.highx;
					ly = cur_object.lowy;
					hy = cur_object.highy;
					first_time = false;
				} else
				{
					if (cur_object.lowx < lx) lx = cur_object.lowx;
					if (cur_object.highx > hx) hx = cur_object.highx;
					if (cur_object.lowy < ly) ly = cur_object.lowy;
					if (cur_object.highy > hy) hy = cur_object.highy;
				}
			}
			if (axis == HORIZONTAL)
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
		private LINE com_sort(LINE line, int axis)
		{
			if (line == null)
			{
				System.out.println("Error: com_sort called with null argument");
				return null;
			}

			// first figure out the weighting factor that will be sorted
			for(LINE cur_line = line; cur_line != null; cur_line = cur_line.nextline)
			{
				double ave = 0, totallen = 0;
				for(OBJECT cur_object = cur_line.firstobject; cur_object != null;
					cur_object = cur_object.nextobject)
				{
					double len = 0, ctr = 0;
					if (axis == HORIZONTAL)
					{
						len = cur_object.highy - cur_object.lowy;
						ctr = (cur_object.lowx+cur_object.highx) / 2;
					} else
					{
						len = cur_object.highx - cur_object.lowx;
						ctr = (cur_object.lowy+cur_object.highy) / 2;
					}

					ctr *= len;
					totallen += len;
					ave += ctr;
				}
				if (totallen != 0) ave /= totallen;
				cur_line.val = ave;
			}

			// now sort on the "val" field
			LINE new_line = null;
			for(;;)
			{
				if (line == null) break;
				boolean first = true;
				double bestval = 0;
				LINE bestline = null;
				for(LINE cur_line = line; cur_line != null; cur_line = cur_line.nextline)
				{
					if (first)
					{
						bestval = cur_line.val;
						bestline = cur_line;
						first = false;
					} else if (cur_line.val > bestval)
					{
						bestval = cur_line.val;
						bestline = cur_line;
					}
				}

				// remove bestline from the list
				if (bestline.prevline == null) line = bestline.nextline; else
					bestline.prevline.nextline = bestline.nextline;
				if (bestline.nextline != null)
					bestline.nextline.prevline = bestline.prevline;

				// insert at the start of this list
				if (new_line != null) new_line.prevline = bestline;
				bestline.nextline = new_line;
				bestline.prevline = null;
				new_line = bestline;
			}
			return(new_line);
		}

		/**
		 * create a new line with the element object and add it to the beginning of
		 * the given line
		 */
		private LINE com_make_object_line(LINE line, OBJECT object)
		{
			LINE new_line = new LINE();
			new_line.nextline = line;
			new_line.prevline = null;
			new_line.firstobject = object;
			if (line != null) line.prevline = new_line;
			return new_line;
		}

		private void com_createobjects(NodeInst ni, int axis, OBJECT [] thisobject, OBJECT [] otherobject, HashSet nodesSeen,
			HashMap arcIndices, HashMap portIndices, Netlist nl)
		{
			// if node has already been examined, quit now
			if (nodesSeen.contains(ni)) return;
			nodesSeen.add(ni);

			// if this is the first object, add it
			if (thisobject[0] == null)
				thisobject[0] = com_make_ni_object(ni, null, GenMath.MATID, axis, 0,0,0,0, arcIndices, portIndices, nl);
			double st_low = 0, st_high = 0;
			if (axis == HORIZONTAL)
			{
				st_low = thisobject[0].lowx;
				st_high = thisobject[0].highx;
			} else
			{
				st_low = thisobject[0].lowy;
				st_high = thisobject[0].highy;
			}

			// for each arc on node, find node at other end and add to object
			for(Iterator it = ni.getConnections(); it.hasNext(); )
			{
				Connection con = (Connection)it.next();
				ArcInst ai = con.getArc();
				NodeInst other_end = ai.getTail().getPortInst().getNodeInst();
				if (other_end == ni) other_end = ai.getHead().getPortInst().getNodeInst();

				// stop if other end has already been examined
				if (nodesSeen.contains(other_end)) continue;

				OBJECT new_object = com_make_ai_object(ai, null, GenMath.MATID, axis, 0,0,0,0, arcIndices);

				OBJECT second_object = com_make_ni_object(other_end, null, GenMath.MATID, axis, 0,0,0,0, arcIndices, portIndices, nl);

				double bd_low = 0, bd_high = 0;
				if (axis == HORIZONTAL)
				{
					bd_low = second_object.lowx;
					bd_high = second_object.highx;
				} else
				{
					bd_low = second_object.lowy;
					bd_high = second_object.highy;
				}
				if (bd_high > st_low && bd_low < st_high)
				{
					com_add_object_to_object(thisobject, new_object);
					com_add_object_to_object(thisobject, second_object);
					com_createobjects(other_end, axis, thisobject, otherobject, nodesSeen, arcIndices, portIndices, nl);
				} else
				{
					// arcs in object to be used later in fixed_non_fixed
					com_add_object_to_object(otherobject, new_object);
				}
			}
		}

		/**
		 * add object add_object to the beginning of list "*object"
		 */
		private void com_add_object_to_object(OBJECT [] object, OBJECT add_object)
		{
			add_object.nextobject = object[0];
			object[0] = add_object;
		}

		/**
		 * Method to build a object describing node "ni" in axis "axis".  If "object"
		 * is NOOBJECT, this node is at the top level, and a new OBJECT should be
		 * constructed for it.  Otherwise, the node is in a subcell and it must be
		 * transformed through "newtrans" and clipped to the two protection frames
		 * defined by "low1" to "high1" and "low2" to "high2" before being added to
		 * "object".
		 */
		private OBJECT com_make_ni_object(NodeInst ni, OBJECT object, AffineTransform newtrans,
			int axis, double low1, double high1, double low2, double high2, HashMap arcIndices, HashMap portIndices, Netlist nl)
		{
			OBJECT new_object = object;
			if (object == null)
			{
				new_object = new OBJECT();
				new_object.inst = ni;
				new_object.isnode = true;
				new_object.nextobject = null;
				new_object.firstpolylist = null;
				Rectangle2D bounds = ni.getBounds();
				new_object.lowx = bounds.getMinX();
				new_object.highx = bounds.getMaxX();
				new_object.lowy = bounds.getMinY();
				new_object.highy = bounds.getMaxY();
			}

			// propagate global network info to local port prototypes on "ni"
			HashMap localPortIndices = com_fillnode(ni, arcIndices, portIndices, nl);

			// create pseudo-object for complex ni
			if (ni.getProto() instanceof Cell)
			{
				Cell subCell = (Cell)ni.getProto();
				Netlist subNl = subCell.getUserNetlist();

				// compute transformation matrix from subnode to this space
				AffineTransform t1 = ni.rotateOut(newtrans);
				AffineTransform trans = ni.translateOut(newtrans);

				/*
				 * create a line for cell "ni->proto" at the current location and
				 * translation.  Put only the instances which are within com_maxboundary
				 * of the perimeter of the cell.
				 */
				HashMap localArcIndices = com_subsmash(subCell, localPortIndices);

				// compute protection frame if at the top level
				if (object == null)
				{
					Rectangle2D bounds = ni.getBounds();
					if (axis == HORIZONTAL)
					{
						low1 = bounds.getMinX();
						high1 = bounds.getMinX() + com_maxboundary;
						low2 = bounds.getMaxX() - com_maxboundary;
						high2 = bounds.getMaxX();
					} else
					{
						low1 = bounds.getMinY();
						high1 = bounds.getMinY() + com_maxboundary;
						low2 = bounds.getMaxY() - com_maxboundary;
						high2 = bounds.getMaxY();
					}
				}

				// include polygons from those nodes and arcs in the protection frame
				for(Iterator it = subCell.getNodes(); it.hasNext(); )
				{
					NodeInst subNi = (NodeInst)it.next();
					com_make_ni_object(subNi, new_object, trans, axis,
						low1, high1, low2, high2, localArcIndices, localPortIndices, subNl);
				}
				for(Iterator it = subCell.getArcs(); it.hasNext(); )
				{
					ArcInst subAi = (ArcInst)it.next();
					com_make_ai_object(subAi, new_object, trans, axis,
						low1, high1, low2, high2, localArcIndices);
				}
			} else
			{
				AffineTransform trans = ni.rotateOut(newtrans);
				Technology tech = ni.getProto().getTechnology();
				Poly [] polys = tech.getShapeOfNode(ni, null, true, true);
				int tot = polys.length;
				for(int j=0; j<tot; j++)
				{
					Poly poly = polys[j];
					poly.transform(trans);

					// make sure polygon is within protection frame
					if (object != null)
					{
						Rectangle2D bounds = poly.getBounds2D();
						if (axis == HORIZONTAL)
						{
							if ((bounds.getMaxX() < low1 || bounds.getMinX() > high1) && (bounds.getMaxX() < low2 || bounds.getMinX() > high2)) continue;
						} else
						{
							if ((bounds.getMaxY() < low1 || bounds.getMinY() > high1) && (bounds.getMaxY() < low2 || bounds.getMinY() > high2)) continue;
						}
					}

					int pIndex = -1;
					if (poly.getPort() != null)
					{
						Integer i = (Integer)localPortIndices.get(poly.getPort());
						if (i != null) pIndex = i.intValue();
					}
					com_add_poly_polylist(poly, new_object, pIndex, tech);
				}
			}
			return new_object;
		}

		private HashMap com_fillnode(NodeInst ni, HashMap arcIndices, HashMap portIndices, Netlist nl)
		{
			// initialize network information for this node instance
			HashMap localPortIndices = new HashMap();

			// set network numbers from arcs
			for(Iterator it = ni.getConnections(); it.hasNext(); )
			{
				Connection con = (Connection)it.next();
				ArcInst ai = con.getArc();
				PortProto pp = con.getPortInst().getPortProto();
				if (localPortIndices.get(pp) != null) continue;
				Integer aIndex = (Integer)arcIndices.get(ai);
				localPortIndices.put(pp, aIndex);
			}

			// set network numbers from exports
			for(Iterator it = ni.getExports(); it.hasNext(); )
			{
				Export e = (Export)it.next();
				PortProto pp = e.getOriginalPort().getPortProto();
				if (localPortIndices.get(pp) != null) continue;
				Integer pIndex = (Integer)portIndices.get(e);
				localPortIndices.put(pp, pIndex);
			}

			// look for unconnected ports and assign new network numbers
			for(Iterator it = ni.getProto().getPorts(); it.hasNext(); )
			{
				PortProto pp = (PortProto)it.next();
				if (localPortIndices.get(pp) != null) continue;

				// look for similar connected port
				boolean found = false;
				Network net = nl.getNetwork(ni, pp, 0);
				for(Iterator oIt = ni.getProto().getPorts(); oIt.hasNext(); )
				{
					PortProto oPp = (PortProto)oIt.next();
					Network oNet = nl.getNetwork(ni, oPp, 0);
					if (oNet == net)
					{
						Integer oIndex = (Integer)localPortIndices.get(oPp);
						if (oIndex != null)
						{
							localPortIndices.put(pp, oIndex);
							found = true;
							break;
						}
					}
				}
				if (!found) localPortIndices.put(pp, new Integer(com_flatindex++));
			}
			return localPortIndices;
		}

		/**
		 * Method to build a "object" structure that describes arc "ai".  If "object"
		 * is NOOBJECT, this arc is at the top level, and a new OBJECT should be
		 * constructed for it.  Otherwise, the arc is in a subcell and it must be
		 * transformed through "newtrans" and clipped to the two protection frames
		 * defined by "low1", "high1" and "low2", "high2" before being added to "object".
		 */
		private OBJECT com_make_ai_object(ArcInst ai, OBJECT object, AffineTransform newtrans,
			int axis, double low1, double high1, double low2, double high2, HashMap arcIndices)
		{
			// create the object if at the top level
			OBJECT new_object = object;
			if (object == null)
			{
				new_object = new OBJECT();
				new_object.inst = ai;
				new_object.isnode = false;
				new_object.nextobject = null;
				new_object.firstpolylist = null;
				Rectangle2D bounds = ai.getBounds();
				new_object.lowx = bounds.getMinX();
				new_object.highx = bounds.getMaxX();
				new_object.lowy = bounds.getMinY();
				new_object.highy = bounds.getMaxY();
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
					poly.transform(newtrans);
					Rectangle2D bounds = poly.getBounds2D();
					if (axis == HORIZONTAL)
					{
						if ((bounds.getMaxX() < low1 || bounds.getMinX() > high1) && (bounds.getMaxX() < low2 || bounds.getMinX() > high2)) continue;
					} else
					{
						if ((bounds.getMaxY() < low1 || bounds.getMinY() > high1) && (bounds.getMaxY() < low2 || bounds.getMinY() > high2)) continue;
					}
				}

				// add the polygon
				int aIndex = -1;
				Integer iv = (Integer)arcIndices.get(ai);
				if (iv != null) aIndex = iv.intValue();
				com_add_poly_polylist(poly, new_object, aIndex, tech);
			}
			return new_object;
		}

		/**
		 * Method to link polygon "poly" into object "object" with network number
		 * "networknum"
		 */
		private void com_add_poly_polylist(Poly poly, OBJECT object, int networknum, Technology tech)
		{
			COMPPOLYLIST new_polys = new COMPPOLYLIST();
			new_polys.poly = poly;
			new_polys.tech = tech;
			new_polys.networknum = networknum;
			new_polys.nextpolylist = object.firstpolylist;
			object.firstpolylist = new_polys;
		}

		/**
		 * copy network information from ports to arcs in cell "topcell"
		 */
		private HashMap com_subsmash(Cell topcell, HashMap portIndices)
		{
			Netlist nl = topcell.getUserNetlist();

			// first erase the arc node information
			HashMap arcIndices = new HashMap();

			// copy network information from ports to arcs
			for(Iterator it = topcell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();

				// ignore arcs that have already been numbered
				if (arcIndices.get(ai) != null) continue;

				// see if this arc connects to a port
				Network aNet = nl.getNetwork(ai, 0);
				boolean found = false;
				for(Iterator pIt = topcell.getPorts(); pIt.hasNext(); )
				{
					Export pp = (Export)pIt.next();
					Integer pIndex = (Integer)portIndices.get(pp);
					Network pNet = nl.getNetwork(pp, 0);
					if (pNet == aNet)
					{
						// propagate port numbers into all connecting arcs
						for(Iterator aIt = topcell.getArcs(); aIt.hasNext(); )
						{
							ArcInst oAi = (ArcInst)aIt.next();
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
					Integer pIndex = new Integer(com_flatindex++);
					for(Iterator aIt = topcell.getArcs(); aIt.hasNext(); )
					{
						ArcInst oAi = (ArcInst)aIt.next();
						Network oANet = nl.getNetwork(oAi, 0);
						if (oANet == aNet) arcIndices.put(oAi, pIndex);
					}
				}
			}
			return arcIndices;
		}
	}
}
