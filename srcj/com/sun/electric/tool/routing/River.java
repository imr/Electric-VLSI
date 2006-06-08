/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: River.java
 * Routing tool: River Routing (busses).
 * Original C Code written by Telle Whitney, Schlumberger Palo Alto Research
 * Translated to Java by Steven M. Rubin, Sun Microsystems.
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
package com.sun.electric.tool.routing;

import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.user.menus.MenuCommands;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * Class to do river routing.
 * <P>
 * River Routing takes two sets of parallel points (connectors, ports, etc) and routes wires
 * between them.  All wires are routed in a single layer with non intersecting lines.
 * <PRE>
 *                       p1        p2         p3      p4
 *                        |        |           |       |  /\ cell_off2
 *                       _|        |           |   ____|  \/
 *                      |          |           |  |
 *                    __|    ______|           |  |
 *                   |      |                  |  |
 *                   |   ___|      ____________|  |
 *                   |  |         | /\ pitch      |
 *                 __|  |      ___| \/____________|
 *  cell_off1 /\  |     |     |    <>|
 *            \/  |     |     |      |
 *               a1    a2    a3     a4
 * </PRE>
 * Restrictions:
 * <UL>
 * <LI>The distance between the ports (p1..pn) and (a1..an) is >= "pitch"</LI>
 * <LI>The parameter "width" specifies the width of all wires<BR>
 *     The parameter "space" specifies the distance between wires<BR>
 *     pitch = 2*(width/2) + space = space + width</LI>
 * </UL>
 * The sides:
 * <PRE>
 *                        SIDE3
 *       ________________________________________
 *       |  route  |                  |  route  |
 *     S |  right  |                  |  left   | S
 *     I | (last)  |   normal right   | (last)  | I
 *     D |_________|  and left route  |_________| D
 *     E |  route  |     (middle)     |  route  | E
 *     4 |  left   |                  |  right  | 2
 *       | (first) |                  | (first) |
 *       |_________|__________________|_________|
 *                        SIDE1
 * </PRE>
 */
public class River
{
	private static final int ROUTEINX     = 1;
	private static final int ROUTEINY     = 2;
	private static final int ILLEGALROUTE = -1;
	/** bottom to top  -- side 1 to side 3 */			private static final int BOTTOP   = 1;
	/** side   to top  -- side 2 or side 4 to side 3 */	private static final int FROMSIDE = 2;
	/** bottom to side -- side 1 to side 3 or side 2 */	private static final int TOSIDE   = 3;

	/** list of RDESC objects */						private List<RDESC> rightP, leftP;
	/** the initial coordinate of the route */			private double      fromLine;
	/** final coordinate of the route */				private double      toLine;
	/**  ROUTEINX route in X, ROUTEINY route in Y */	private int         routDirection;
	/** where to start wires on the right */			private double      startRight;
	/** where to start wires on the left */				private double      startLeft;
	/** linked list of possible routing coordinates */	private RCOORD      xAxis, yAxis;
	private double   height;
	private double   routBoundLX, routBoundLY, routBoundHX, routBoundHY;
	private double   wireBoundLX, wireBoundLY, wireBoundHX, wireBoundHY;
	private NodeInst moveCell;
	private boolean  moveCellValid;

	/**
	 * Class for transformations.
	 */
	static class TRANSFORM
	{
		private double t11, t12;
		private double t21, t22;
		private double tx, ty;

		TRANSFORM(double t11, double t12, double t21, double t22, double tx, double ty)
		{
			this.t11 = t11;
			this.t12 = t12;
			this.t21 = t21;
			this.t22 = t22;
			this.tx = tx;
			this.ty = ty;
		}
	};
	/** X increasing, y2>y1 */							private static final TRANSFORM xfNoRot        = new TRANSFORM( 1,  0,  0,  1, 0, 0);
	/** Y decreasing, x2>x1 */							private static final TRANSFORM xfRot90        = new TRANSFORM( 0,  1, -1,  0, 0, 0);
	/** X decreasing, y2<y1 */							private static final TRANSFORM xfRot180       = new TRANSFORM(-1,  0,  0, -1, 0, 0);
	/** Y increasing, x2<x1 or rot -90 */				private static final TRANSFORM xfRot270       = new TRANSFORM( 0, -1,  1,  0, 0, 0);
	/** X decreasing, y2>y1 mirror X, around Y axis */	private static final TRANSFORM xfMirrorX      = new TRANSFORM(-1,  0,  0,  1, 0, 0);
	/** Y increasing, x2>x1 rot90 and mirror X */		private static final TRANSFORM xfRot90MirrorX = new TRANSFORM( 0,  1,  1,  0, 0, 0);
	/** X increasing, y2<y1 mirror Y, around X axis */	private static final TRANSFORM xfMirrorY      = new TRANSFORM( 1,  0,  0, -1, 0, 0);
	/** Y decreasing, x2<x1 mirror X, rot90 */			private static final TRANSFORM xfMirrorXRot90 = new TRANSFORM( 0, -1, -1,  0, 0, 0);
	/** tx,ty */										private static final TRANSFORM xfInverse      = new TRANSFORM( 1,  0,  0,  1, 0, 0);

	/**
	 * Class for points in the river routing.
	 */
	static class RPOINT
	{
		private static final int NOSIDE = -1;
		private static final int SIDE1  = 1;
		private static final int SIDE2  = 2;
		private static final int SIDE3  = 3;
		private static final int SIDE4  = 4;

		/** the side this point is on */	private int    side;
		/** points coordinates */			private double x, y;
		/** nonrotated coordinates */		private double first, second;
		/** next one in the list */			private RPOINT next;

		static String sideName(int side)
		{
			switch (side)
			{
				case SIDE1: return "bottom";
				case SIDE2: return "right";
				case SIDE3: return "top";
				case SIDE4: return "left";
			}
			return "unknown";
		}

		RPOINT(double xV, double yV, int s)
		{
			side = s;
			x = xV;
			y = yV;
			first = 0;
			second = 0;
			next = null;
		}

		RPOINT(RPATH rp, double fir, double sec, RPOINT next)
		{
			side = NOSIDE;
			x = 0;
			y = 0;
			first = fir;
			second = sec;
			this.next = next;
			if (next != null) return;
			rp.lastP = this;
		}
	};

	/**
	 * Class for paths in the river routing.
	 */
	static class RPATH
	{
		/** the width of this path */			private double   width;
		/** the paty type for this wire */		private ArcProto pathType;
		/** the path */							private RPOINT   pathDesc;
		/** the last point on the path */		private RPOINT   lastP;

		RPATH(double wid, ArcProto ptype)
		{
			width = wid;
			pathType = ptype;
			pathDesc = null;
			lastP = null;
		}
	};

	/**
	 * Class for river routing.
	 */
	static class RDESC
	{
		private RPOINT  from;
		private RPOINT  to;
		private double  sortVal;
		private ArcInst unroutedWire1;
		private ArcInst unroutedWire2;
		private int     unroutedEnd1;
		private int     unroutedEnd2;
		private RPATH   path;

		RDESC(double fx, double fy, int fside, double sx, double sy, int sside,
			ArcInst ai1, int ae1, ArcInst ai2, int ae2)
		{
			from = new RPOINT(fx, fy, fside);
			to = new RPOINT(sx, sy, sside);
			unroutedWire1 = ai1;   unroutedEnd1 = ae1;
			unroutedWire2 = ai2;   unroutedEnd2 = ae2;
			path = null;
		}
	};

	/**
	 * Class for coordinate values in the river routing.
	 */
	static class RCOORD
	{
		/** the coordinate */								private double val;
		/** number of wires voting for this coordinate */	private int    total;
															private RCOORD next;

		RCOORD(double c)
		{
			this.val = c;
			this.total = 0;
			this.next = null;
		}
	};

	/*************************************** MAIN CONTROL CODE ***************************************/

	public static void riverRoute()
	{
		UserInterface ui = Job.getUserInterface();
		Cell curCell = ui.needCurrentCell();
		if (curCell == null) return;
		RiverRouteJob job = new RiverRouteJob(curCell);
	}

	private static class RiverRouteJob extends Job
	{
		private Cell cell;

		protected RiverRouteJob(Cell cell)
		{
			super("River Route", Routing.getRoutingTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			River router = new River();
			router.river(cell);
			return true;
		}

        public void terminateOK()
        {
			UserInterface ui = Job.getUserInterface();
			EditWindow_ wnd = ui.getCurrentEditWindow_();
	        if (wnd != null)
	        {
	        	wnd.clearHighlighting();
	        	wnd.finishedHighlighting();
	        }
        }
	}

	/**
	 * This is the public interface for River Routing when done in batch mode.
	 * @param cell the cell to be River-routed.
	 */
	public void river(Cell cell)
	{
		// locate wires
		if (findWires(cell))
		{
			// make wires
			for(RDESC q : rightP)
			{
				checkTheCell(q.unroutedWire2.getPortInst(q.unroutedEnd2).getNodeInst());
			}
			for(RDESC q : leftP)
			{
				checkTheCell(q.unroutedWire2.getPortInst(q.unroutedEnd2).getNodeInst());
			}

			// if there is motion to be done, do it
			if (moveCellValid && moveCell != null)
			{
				if (moveInstance()) makeTheGeometry(cell);
			} else makeTheGeometry(cell);
		}
	}

	/*************************************** CODE TO DO RIVER ROUTING ***************************************/

	/**
	 * once the route occurs, make some geometry and move some cells around
	 */
	private void checkTheCell(NodeInst ni)
	{
		if (ni.isCellInstance())
		{
			// the node is nonprimitive
			if (!moveCellValid) return;
			if (moveCell == null)  // first one
				moveCell = ni;
			else if (moveCell != ni) moveCellValid = false;
		}
	}

	private boolean findWires(Cell cell)
	{
		initialize();

		// reset flags on all arcs in this cell
		HashSet<ArcInst> arcsSeen = new HashSet<ArcInst>();

		// make a list of RDESC objects
		List<RDESC> theList = new ArrayList<RDESC>();

		// get list of all highlighted arcs
		List<Geometric> allArcs = MenuCommands.getSelectedObjects(false, true);
		if (allArcs.size() != 0)
		{
			// add all highlighted arcs to the list of RDESC objects
			for(Geometric geom : allArcs)
			{
				ArcInst ai = (ArcInst)geom;
				addWire(theList, ai, arcsSeen);
			}
		} else
		{
			// add all arcs in the cell to the list of RDESC objects
			for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = it.next();
				addWire(theList, ai, arcsSeen);
			}
		}

		// determine bounds of the routes
		boolean first = true;
		for(RDESC rdesc : theList)
		{
			if (first)
			{
				routBoundLX = Math.min(rdesc.from.x, rdesc.to.x);
				routBoundLY = Math.min(rdesc.from.y, rdesc.to.y);
				routBoundHX = Math.max(rdesc.from.x, rdesc.to.x);
				routBoundHY = Math.max(rdesc.from.y, rdesc.to.y);
			} else
			{
				routBoundLX = Math.min(Math.min(routBoundLX, rdesc.from.x), rdesc.to.x);
				routBoundLY = Math.min(Math.min(routBoundLY, rdesc.from.y), rdesc.to.y);
				routBoundHX = Math.max(Math.max(routBoundHX, rdesc.from.x), rdesc.to.x);
				routBoundHY = Math.max(Math.max(routBoundHY, rdesc.from.y), rdesc.to.y);
			}
		}

		// figure out which ArcProto to use
		HashMap<ArcProto,GenMath.MutableInteger> arcProtoUsage = new HashMap<ArcProto,GenMath.MutableInteger>();
		for(RDESC rd : theList)
		{
			sumUp(rd.unroutedWire1.getPortInst(rd.unroutedEnd1), arcProtoUsage);
			sumUp(rd.unroutedWire2.getPortInst(rd.unroutedEnd2), arcProtoUsage);
		}

		// find the most popular ArcProto
		ArcProto wantAp = null;
		int mostUses = -1;
		int total = 0;
		for(ArcProto ap : arcProtoUsage.keySet())
		{
			GenMath.MutableInteger mi = (GenMath.MutableInteger)arcProtoUsage.get(ap);
			if (mi == null) continue;
			total += mi.intValue();
			if (mi.intValue() > mostUses)
			{
				mostUses = mi.intValue();
				wantAp = ap;
			}
		}
		if (wantAp == null)
		{
			System.out.println("River router: Cannot find arc that will connect");
			return false;
		}
		System.out.println("River routing with " + wantAp.describe() + " arcs");

		figureOutRails(total);
		setWiresToRails(theList);

		// figure out the worst design rule spacing for this type of arc
		Technology.ArcLayer [] arcLayers = wantAp.getLayers();
		Layer layer = arcLayers[0].getLayer();
		double amt = DRC.getMaxSurround(layer, Double.MAX_VALUE);
		if (amt < 0) amt = 1;
		return unsortedRivRot(wantAp, theList, wantAp.getDefaultWidth() - wantAp.getWidthOffset(), amt, amt, amt);
	}

	/**
	 * takes two unsorted list of ports and routes between them
	 * warning - if the width is not even, there will be round off problems
	 */
	private boolean unsortedRivRot(ArcProto layerDesc, List<RDESC> lists, double width,
		double space, double cellOff1, double cellOff2)
	{
		for(RDESC rd : lists)
		{
			rd.sortVal = (routDirection != ROUTEINX ? rd.from.x : rd.from.y);
		}
		Collections.sort(lists, new SortRDESC());

		return sortedRivRot(layerDesc, lists, width, space, cellOff1, cellOff2);
	}

	private static class SortRDESC implements Comparator<RDESC>
	{
		public int compare(RDESC r1, RDESC r2)
		{
			if (r1.sortVal == r2.sortVal) return 0;
			if (r1.sortVal > r2.sortVal) return 1;
			return -1;
		}
	}

	/**
	 * takes two sorted list of ports and routes between them
	 * warning - if the width is not even, there will be round off problems
	 */
	private boolean sortedRivRot(ArcProto layerDesc, List<RDESC> listR, double width,
		double space, double cellOff1, double cellOff2)
	{
		// ports invalid
		if (!checkPoints(listR, width, space)) return false;
		structurePoints(listR);				// put in left/right
		if (!checkStructuredPoints(rightP, leftP, cellOff1, width, space)) return false;
		if (processRight(width, layerDesc, rightP, cellOff1, space, -1)) return false;
		if (processLeft(width, layerDesc, leftP, cellOff1, space, 1)) return false;
		Double dHeight = calculateHeightAndProcess(rightP, leftP, width, cellOff2);
		if (dHeight == null) return false;
		calculateBB(rightP, leftP);
		height = dHeight.doubleValue();
		return true;
	}

	private void calculateBB(List<RDESC> right, List<RDESC> left)
	{
		routBoundLX = routBoundLY = Double.MAX_VALUE;
		routBoundHX = routBoundHY = Double.MIN_VALUE;
		for(RDESC rRight : right)
		{
			for(RPOINT rvp = rRight.path.pathDesc; rvp != null; rvp = rvp.next)
			{
				routBoundLX = Math.min(routBoundLX, rvp.x);
				routBoundLY = Math.min(routBoundLY, rvp.y);
				routBoundHX = Math.max(routBoundHX, rvp.x);
				routBoundHY = Math.max(routBoundHY, rvp.y);
			}
		}
		for(RDESC lLeft : left)
		{
			for(RPOINT rvp = lLeft.path.pathDesc; rvp != null; rvp = rvp.next)
			{
				routBoundLX = Math.min(routBoundLX, rvp.x);
				routBoundLY = Math.min(routBoundLY, rvp.y);
				routBoundHX = Math.max(routBoundHX, rvp.x);
				routBoundHY = Math.max(routBoundHY, rvp.y);
			}
		}
	}

	private Double calculateHeightAndProcess(List<RDESC> right, List<RDESC> left, double width, double co2)
	{
		double minHeight = 0;
		double maxHeight = Double.MIN_VALUE;
		for(RDESC rd : right)
		{
			maxHeight = Math.max(maxHeight, rd.path.lastP.second);
		}
		for(RDESC rd : left)
		{
			maxHeight = Math.max(maxHeight, rd.path.lastP.second);
		}

		if (minHeight != 0) maxHeight = Math.max(minHeight, maxHeight+(width/2)+co2);
			else maxHeight = maxHeight+(width/2)+co2;
		maxHeight = Math.max(maxHeight, toLine);

		// make sure its at least where the coordinates are
		for(RDESC rd : right)
		{
			RPOINT lastP = rd.path.lastP;
			if (lastP.side != RPOINT.SIDE2)
			{
				lastP.next = new RPOINT(rd.path, lastP.first, maxHeight, null);
			}
			remapPoints(rd.path.pathDesc, xfInverse);
		}
		for(RDESC rd : left)
		{
			RPOINT lastP = rd.path.lastP;
			if (lastP.side != RPOINT.SIDE4)
			{
				lastP.next = new RPOINT(rd.path, lastP.first, maxHeight, null);
			}
			remapPoints(rd.path.pathDesc, xfInverse);
		}
		toLine = remapSecond(toLine, xfInverse);
		fromLine = remapSecond(fromLine, xfInverse);
		return new Double(remapSecond(maxHeight, xfInverse));
	}

	/**
	 * calculate the height of the channel, and remap the points back into the
	 * original coordinate system
	 */
	private void remapPoints(RPOINT rp, TRANSFORM matrix)
	{
		for(; rp != null; rp = rp.next)
		{
			rp.x = (rp.first*matrix.t11) + (rp.second*matrix.t21);
			rp.y = (rp.first*matrix.t12) + (rp.second*matrix.t22);
		}
	}

	private double remapSecond(double sec, TRANSFORM matrix)
	{
		if (routDirection == ROUTEINY) return sec * matrix.t22;
		return sec * matrix.t12;
	}

	private boolean processLeft(double width, ArcProto ptype, List<RDESC> rout, double co1, double space, double dir)
	{
		boolean firstTime = true;
		RPATH lastP = null;
		double offset = startRight;
		for(RDESC rd : rout)
		{
			if (rd.from.side != RPOINT.SIDE2)
			{
				if (firstTime)
				{
					rd.path = makeOrigPath(width, ptype, co1, rd.from, rd.to);
					if (rd.path == null) return true;
					firstTime = false;
				} else
					rd.path = addPath(lastP, width, ptype, rd.from, rd.to, space, co1, dir);
				if (rd.path == null) return true;
			} else
			{
				if (firstTime)
				{
					rd.path = makeSideOrigPath(width, ptype, offset, rd.from, rd.to);
					if (rd.path == null) return true;
					firstTime = false;
				} else
				{
					rd.path = sideAddPath(lastP, width, ptype, rd.from, rd.to, space, offset, dir);
					if (rd.path == null) return true;
				}
				offset += space+width;
			}
			lastP = rd.path;
		}
		return false;
	}

	private boolean processRight(double width, ArcProto ptype, List<RDESC> rout, double co1, double space, int dir)
	{
		boolean firstTime = true;
		RPATH lastP = null;
		double offset = startLeft;

		reverse(rout);
		for(RDESC rd : rout)
		{
			if (rd.from.side != RPOINT.SIDE4)
			{
				// starting from bottom (side1)
				if (firstTime)
				{
					rd.path = makeOrigPath(width, ptype, co1, rd.from, rd.to);
					if (rd.path == null) return true;
					firstTime = false;
				} else
					rd.path = addPath(lastP, width, ptype, rd.from, rd.to, space, co1, dir);
				if (rd.path == null) return true;
			} else
			{
				if (firstTime)
				{
					rd.path = makeSideOrigPath(width, ptype, offset, rd.from, rd.to);
					if (rd.path == null) return true;
					firstTime = false;
				} else
				{
					rd.path = sideAddPath(lastP, width, ptype, rd.from, rd.to, space, offset, dir);
					if (rd.path == null) return true;
				}
				offset += space+width;
			}
			lastP = rd.path;
		}
		reverse(rout);  // return to normal
		return false;
	}

	private RPATH sideAddPath(RPATH path, double width, ArcProto ptype, RPOINT b, RPOINT t,
		double space, double offset, double dir)
	{
		RPATH rp = new RPATH(width, ptype);
		rp.pathDesc = new RPOINT(rp, b.first, offset, null);

		double minFirst = Math.min(b.first, t.first);
		double maxFirst = Math.max(b.first, t.first);
		RPOINT lp = path.pathDesc;
		RPOINT lastP = rp.lastP;

		double newfirst = lp.first+dir*(space+rp.width);
		while (lp != null && minFirst <= newfirst && newfirst <= maxFirst)
		{
			// if first point then inconsistent second(y) offset 
			if (lp == path.pathDesc)
				lastP.next = new RPOINT(rp, newfirst, Math.min(lastP.second, offset), null);
			else
				lastP.next = new RPOINT(rp, newfirst, Math.max(lp.second+space+rp.width, offset), null);
			lastP = lastP.next;   lp = lp.next;
			if (lp != null) newfirst = lp.first+dir*(space+rp.width);
		}
		lastP.next = new RPOINT(rp, t.first, lastP.second, null);
		rp.lastP.side = t.side;
		return(rp);
	}

	private RPATH addPath(RPATH path, double width, ArcProto ptype, RPOINT b, RPOINT t,
		double space, double co1, double dir)
	{
		RPATH rp = new RPATH(width, ptype);
		RPOINT i1 = new RPOINT(rp, b.first, b.second+(rp.width/2)+co1, null);
		rp.pathDesc = new RPOINT(rp, b.first, b.second, i1);
		double minFirst = Math.min(b.first, t.first);
		double maxFirst = Math.max(b.first, t.first);
		RPOINT lp = path.pathDesc;
		RPOINT lastP = rp.lastP;

		double newfirst = lp.first+dir*(space+rp.width);
		while (lp != null && minFirst <= newfirst && newfirst <= maxFirst)
		{
			// if first point then inconsistent second(y) offset
			if (lp == path.pathDesc)
				lastP.next = new RPOINT(rp, newfirst, lastP.second, null); else
					lastP.next = new RPOINT(rp, newfirst, lp.second+space+rp.width, null);
			lastP = lastP.next;   lp = lp.next;
			if (lp != null) newfirst = lp.first+dir*(space+rp.width);
		}
		lastP.next = new RPOINT(rp, t.first, lastP.second, null);
		rp.lastP.side = t.side;
		return rp;
	}

	private RPATH makeOrigPath(double width, ArcProto ptype, double co1, RPOINT b, RPOINT t)
	{
		RPATH rp = new RPATH(width, ptype);

		RPOINT i1 = new RPOINT(rp, t.first, b.second+(width/2)+co1, null);
		RPOINT i2 = new RPOINT(rp, b.first, b.second+(width/2)+co1, i1);
		rp.pathDesc = new RPOINT(rp, b.first, b.second, i2);
		rp.lastP.side = t.side;
		return rp;
	}

	private RPATH makeSideOrigPath(double width, ArcProto ptype, double startoff, RPOINT b, RPOINT t)
	{
		RPATH rp = new RPATH(width, ptype);
		RPOINT i1 = new RPOINT(rp, t.first, startoff, null);
		rp.pathDesc = new RPOINT(rp, b.first, startoff, i1);
		rp.lastP.side = t.side;
		return rp;
	}

	private void reverse(List<RDESC> p)
	{
		int total = p.size();
		if (total <= 1) return;

		for(int i=0; i<total/2; i++)
		{
			int otherI = total - i - 1;
			RDESC early = (RDESC)p.get(i);
			RDESC late = (RDESC)p.get(otherI);
			p.set(i, late);
			p.set(otherI, early);
		}
	}

	private boolean checkStructuredPoints(List<RDESC> right, List<RDESC> left, double co1, double width, double space)
	{
		boolean fromSide1 = false;
		boolean toSide2 = false;
		double botOffs2 = 0;

		// ensure ordering is correct
		for(RDESC r : right)
		{
			switch (r.from.side)
			{
				case RPOINT.SIDE1:
					fromSide1 = true;
					break;
				case RPOINT.SIDE4:
					if (fromSide1)
					{
						System.out.println("River router: Improper ordering of bottom right ports");
						return false;
					}
					break;
				default:
					System.out.println("River router: Improper sides for bottom right ports (" + RPOINT.sideName(r.from.side) + ")");
					return false;
			}
			switch (r.to.side)
			{
				case RPOINT.SIDE2:
					if (!toSide2) botOffs2 = fromLine+co1+(width/2);
						else botOffs2 += space+width;
					toSide2 = true;
					break;
				case RPOINT.SIDE3:
					if (toSide2)
					{
						System.out.println("River router: Improper ordering of top right ports");
						return false;
					}
					break;
				default:
					System.out.println("River router: Improper sides for top right ports");
					return false;
			}
		}

		boolean fromSide2 = false;   boolean toSide3 = false;   boolean toSide4 = false;   double botOffs4 = 0;
		for(RDESC l : left)
		{
			switch (l.from.side)
			{
				case RPOINT.SIDE1:
					if (fromSide2)
					{
						System.out.println("River router: Improper Ordering of Bottom Left Ports");
						return false;
					}
					break;
				case RPOINT.SIDE2:
					fromSide2 = true;
					break;
				default:
					System.out.println("River router: Improper sides for Bottom Left Ports");
					return false;
			}
			switch (l.to.side)
			{
				case RPOINT.SIDE3:
					toSide3 = true;
					break;
				case RPOINT.SIDE4:
					if (!toSide3)
					{
						if (!toSide4) botOffs4 = fromLine+co1+(width/2);
							else botOffs4 += space+width;
					} else
					{
						System.out.println("River router: Improper Ordering of Top Left Ports");
						return false;
					}
					toSide4 = true;
					break;
				default:
					System.out.println("River router: Improper sides for Top Left Ports");
					return false;
			}
		}
		if (botOffs2 == 0) startRight = fromLine+co1+(width/2);
			else	       startRight = botOffs2+space+width;
	
		if (botOffs4 == 0) startLeft = fromLine+co1+(width/2);
			else	       startLeft = botOffs4+space+width;
		return true;
	}

	private void structurePoints(List<RDESC> listr)
	{
		rightP = new ArrayList<RDESC>();
		leftP = new ArrayList<RDESC>();
		for(RDESC rd : listr)
		{
			if (rd.to.first >= rd.from.first) rightP.add(rd);
				else leftP.add(rd);
		}
	}

	private boolean checkPoints(List<RDESC> rdescList, double width, double space)
	{
		int numRDesc = rdescList.size();
		if (numRDesc == 0)
		{
			// need at least one point
			System.out.println("River router: Not enought points");
			return false;
		}

		RDESC listLast = (RDESC)rdescList.get(numRDesc-1);
		if (listLast.from == null || listLast.to == null)
		{
			System.out.println("River router: Not the same number of points");
			return false;
		}

		// decide route orientation
		RDESC listP = (RDESC)rdescList.get(0);
		TRANSFORM tMatrix = null;
		double val1 = 0, val2 = 0;
		if (routDirection == ROUTEINX)
		{
			// route in x direction
			if (listP.to.x >= listP.from.x)
			{											// x2>x1
				if (listLast.from.y >= listP.from.y)
					tMatrix = xfRot90MirrorX;			// Y increasing
						else tMatrix = xfRot90;			// Y decreasing
			} else
			{											// x2<x1
				if (listLast.from.y >= listP.from.y)
					tMatrix = xfRot270;					// Y increasing
						else tMatrix = xfMirrorXRot90;	// Y decreasing
			}
			val1 = fromLine = fromLine * tMatrix.t12;
			val2 = toLine = toLine * tMatrix.t12;
		} else if (routDirection == ROUTEINY)
		{
			// route in y direction
			if (listP.to.y >= listP.from.y)
			{											// y2>y1
				if (listLast.from.x >= listP.from.x)
					tMatrix = xfNoRot;					// X increasing
						else tMatrix = xfMirrorX;		// X decreasing
			} else
			{											// y2<y1
				if (listLast.from.x >= listP.from.x)
					tMatrix = xfMirrorY;				// X increasing
						else tMatrix = xfRot180;		// X decreasing
			}
			val1 = fromLine = fromLine * tMatrix.t22;
			val2 = toLine = toLine * tMatrix.t22;
		} else
		{
			System.out.println("River router: Not between two parallel lines");
			return false;		// not on manhattan parallel lines
		}

		// check ordering of coordinates
		for(int i=0; i<numRDesc-1; i++)
		{
			RDESC lList = (RDESC)rdescList.get(i);
			RDESC lListNext = (RDESC)rdescList.get(i+1);

			// make sure there are no crossings
			if (routDirection == ROUTEINY)
			{
				if ((lList.from.x > lListNext.from.x && lList.to.x < lListNext.to.x) ||
					(lList.from.x < lListNext.from.x && lList.to.x > lListNext.to.x))
				{
					System.out.println("River router: Connections may not cross");
					return false;
				}
			} else
			{
				if ((lList.from.y > lListNext.from.y && lList.to.y < lListNext.to.y) ||
					(lList.from.y < lListNext.from.y && lList.to.y > lListNext.to.y))
				{
					System.out.println("River router: Connections may not cross");
					return false;
				}
			}
		}

		double bound1 = routBoundLX * tMatrix.t11 + routBoundLY * tMatrix.t21;
		double bound2 = routBoundHX * tMatrix.t11 + routBoundHY * tMatrix.t21;
		if (bound2 < bound1)
		{
			double temp = bound2;   bound2 = bound1;   bound1 = temp;
		}
		RPOINT lastFrom = null;   RPOINT lastTo = null;

		// transform points and clip to boundary
		for(RDESC lList : rdescList)
		{
			lList.from.first = (lList.from.x * tMatrix.t11) + (lList.from.y * tMatrix.t21);
			lList.from.second = (lList.from.x * tMatrix.t12) + (lList.from.y * tMatrix.t22);
			lList.to.first = (lList.to.x * tMatrix.t11) + (lList.to.y * tMatrix.t21);
			lList.to.second = (lList.to.x * tMatrix.t12) + (lList.to.y * tMatrix.t22);
			if (lList.from.second != val1) clipWire(lList.from, bound1, bound2);
			if (lList.to.second != val2) clipWire(lList.to, bound1, bound2);

			if (lastFrom != null && lList.from.side == RPOINT.SIDE1)
			{
				double diff1 = Math.abs(lastFrom.first - lList.from.first);
				if (diff1 < width+space)
				{
					System.out.println("River router: Ports not design rule distance apart");
					return false;
				}
			}
			if (lastTo != null && lList.to.side == RPOINT.SIDE3)
			{
				double diff2 = Math.abs(lastTo.first - lList.to.first);
				if (diff2 < width+space)
				{
					System.out.println("River router: Ports not design rule distance apart");
					return false;
				}
			}

			// not far enough apart
			lastFrom = (lList.from.side == RPOINT.SIDE1) ? lList.from : null;
			lastTo = (lList.to.side == RPOINT.SIDE3) ? lList.to : null;
		}

		// matrix to take route back to original coordinate system
		xfInverse.t11 = tMatrix.t11;       xfInverse.t12 = tMatrix.t21;
		xfInverse.t21 = tMatrix.t12;       xfInverse.t22 = tMatrix.t22;
		xfInverse.tx = listP.from.first;   xfInverse.ty = listP.from.second;
		fromLine = val1;   toLine = val2;
		return true;
	}

	private void clipWire(RPOINT p, double b1, double b2)
	{
		double diff1 = Math.abs(b1 - p.first);
		double diff2 = Math.abs(b2 - p.first);
		if (diff1 < diff2)
		{
			p.first = b1;   p.side = RPOINT.SIDE4;
		} else
		{
			p.first = b2;   p.side = RPOINT.SIDE2;
		}
	}

	private void setWiresToRails(List<RDESC> lists)
	{
		for(RDESC r : lists)
		{
			double fVal = pointVal(r.from, routDirection);
			double tVal = pointVal(r.to, routDirection);
			if ((fVal != fromLine && tVal == fromLine) ||
				(tVal != toLine && fVal == toLine))
					swapPoints(r);
		}
	}

	private void swapPoints(RDESC r)
	{
		if (r.from.side != RPOINT.SIDE1 || r.to.side != RPOINT.SIDE3)
			System.out.println("River router: Unexpected side designation");

		RPOINT tmp = r.from;   r.from = r.to;   r.to = tmp;

		r.from.side = RPOINT.SIDE1;   r.to.side = RPOINT.SIDE3;
		ArcInst tmpwire = r.unroutedWire1;   int tmpe = r.unroutedEnd1;
		r.unroutedWire1 = r.unroutedWire2;   r.unroutedEnd1 = r.unroutedEnd2;
		r.unroutedWire2 = tmpwire;           r.unroutedEnd2 = tmpe;
	}

	private double pointVal(RPOINT rp, int xx)
	{
		return xx == ROUTEINX ? rp.x : rp.y;
	}

	private void figureOutRails(int total)
	{
		RCOORD lX = largest(xAxis);
		RCOORD lY = largest(yAxis);
		RCOORD nLX = nextLargest(xAxis, lX);
		RCOORD nLY = nextLargest(yAxis, lY);

		// determine the type of route
		RCOORD from = null;
		RCOORD to = null;
		int fxx = ILLEGALROUTE;
		if (lX != null && nLX != null && lX.total == total && nLX.total == total)
		{
			from = lX;   to = nLX;
			fxx = ROUTEINX;
		} else if (lY != null && nLY != null && lY.total == total && nLY.total == total)
		{
			from = lY;   to = nLY;
			fxx = ROUTEINY;
		} else if (lX != null && lX.total == (2*total))
		{
			from = to = lX;
			fxx = ROUTEINX;
		} else if (lY != null && lY.total == (2*total))
		{
			from = to = lY;
			fxx = ROUTEINY;
		}

		if (fxx == ILLEGALROUTE)
		{
			if (lX.total >= total)
			{
				// lX.total == total --- the other one an unusual case
				// lX.total > total  --- both go to the same line
				fxx = ROUTEINX;   from = lX;
				to = (lX.total > total ? lX : nLX);
			} else if (lY.total >= total)
			{
				// lY.total == total --- the other one an unusual case
				// lY.total > total  --- both go to the same line
				fxx = ROUTEINY;   from = lY;
				to = (lY.total > total ? lY : nLY);
			} else
			{
				fxx = (((lY.total+nLY.total)>=(lX.total+nLX.total)) ? ROUTEINY : ROUTEINX);
				from = (fxx == ROUTEINY ? lY : lX);
				to = (fxx == ROUTEINY ? nLY : nLX);
			}
		}

		if (to.val < from.val)
		{
			RCOORD tmp = from;   from = to;   to = tmp;
		}

		routDirection = fxx;
		fromLine = from.val;   toLine = to.val;
	}

	private RCOORD largest(RCOORD cc)
	{
		RCOORD largest = cc;

		for(; cc != null; cc = cc.next)
		{
			if (cc.total > largest.total) largest = cc;
		}
		return largest;
	}

	private RCOORD nextLargest(RCOORD cc, RCOORD largest)
	{
		RCOORD nLargest = null;

		for( ; cc != null; cc = cc.next)
		{
			if (nLargest == null && cc != largest) nLargest = cc; else
				if (nLargest != null && cc != largest && cc.total > nLargest.total) nLargest = cc;
		}
		return nLargest;
	}

	/**
	 * for every layer (or arcproto) that this PORT allows to connect to it,
	 * increment the flag bits (temp1) IN the prototype thus indicating that
	 * this river route point is allowed to connect to it
	 */
	private void sumUp(PortInst pi, HashMap<ArcProto,GenMath.MutableInteger> arcProtoUsage)
	{
		ArcProto [] possibleArcs = pi.getPortProto().getBasePort().getConnections();
		for(int i=0; i<possibleArcs.length; i++)
		{
			ArcProto ap = possibleArcs[i];
			if (ap.getTechnology() == Generic.tech) continue;
			GenMath.MutableInteger mi = (GenMath.MutableInteger)arcProtoUsage.get(ap);
			if (mi == null)
			{
				mi = new GenMath.MutableInteger(0);
				arcProtoUsage.put(ap, mi);
			}
			mi.increment();
		}
	}

	private void initialize()
	{
		rightP = null;
		leftP = null;
		fromLine = toLine = Double.MIN_VALUE;
		startRight = Double.MIN_VALUE;
		startLeft = Double.MIN_VALUE;
		height = Double.MIN_VALUE;
		routDirection = ILLEGALROUTE;

		xAxis = null;
		yAxis = null;
		for(RCOORD c = xAxis; c != null; c = c.next) c.total = -1;
		for(RCOORD c = yAxis; c != null; c = c.next) c.total = -1;
		moveCell = null;   moveCellValid = true;
	}

	/**
	 * figure out the wires to route at all
	 */
	private void addWire(List<RDESC> list, ArcInst ai, HashSet<ArcInst> arcsSeen)
	{
		if (!isInterestingArc(ai, arcsSeen)) return;

		arcsSeen.add(ai);
		ArcInst ae1 = ai;   int e1 = 0;
		for(;;)
		{
			NodeInst ni = ae1.getPortInst(e1).getNodeInst();
			if (!isUnroutedPin(ni)) break;
			ArcInst oAi = null;
			for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
			{
				Connection con = it.next();
				oAi = con.getArc();
				if (!arcsSeen.contains(oAi)) break;
				oAi = null;
			}
			if (oAi == null) break;
			arcsSeen.add(oAi);
			if (oAi.getPortInst(0).getNodeInst() == ni) e1 = 1; else e1 = 0;
			ae1 = oAi;
		}
		ArcInst ae2 = ai;   int e2 = 1;
		for(;;)
		{
			NodeInst ni = ae2.getPortInst(e2).getNodeInst();
			if (!isUnroutedPin(ni)) break;
			ArcInst oAi = null;
			for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
			{
				Connection con = it.next();
				oAi = con.getArc();
				if (!arcsSeen.contains(oAi)) break;
				oAi = null;
			}
			if (oAi == null) break;
			arcsSeen.add(oAi);
			if (oAi.getPortInst(0).getNodeInst() == ni) e2 = 1; else e2 = 0;
			ae2 = oAi;
		}

		PortInst pi1 = ae1.getPortInst(e1);
		Poly poly1 = pi1.getPoly();
		double bx = poly1.getCenterX();
		double by = poly1.getCenterY();
		PortInst pi2 = ae2.getPortInst(e2);
		Poly poly2 = pi2.getPoly();
		double ex = poly2.getCenterX();
		double ey = poly2.getCenterY();
		RDESC rd = new RDESC(bx, by, RPOINT.SIDE1, ex, ey, RPOINT.SIDE3, ae1, e1, ae2, e2);
		list.add(rd);
		vote(rd.from.x, rd.from.y, rd.to.x, rd.to.y);
	}

	private void vote(double ffx, double ffy, double ttx, double tty)
	{
		xAxis = tallyVote(xAxis, ffx);
		yAxis = tallyVote(yAxis, ffy);
		xAxis = tallyVote(xAxis, ttx);
		yAxis = tallyVote(yAxis, tty);
	}

	/**
	 * Figure out which way to route (x and y) and the top coordinate and
	 * bottom coordinate
	 */
	private RCOORD tallyVote(RCOORD cc, double c)
	{
		if (cc == null)
		{
			cc = new RCOORD(c);
			cc.total = 1;
			return cc;
		}

		RCOORD ccInit = cc;
		RCOORD ccLast = null;
		for( ; (cc != null && cc.total >= 0 && cc.val != c); ccLast = cc, cc = cc.next) ;
		if (cc == null)
		{
			cc = new RCOORD(c);
			ccLast.next = cc;
			cc.total = 1;
			return ccInit;
		} else
		{
			if (cc.total < 0)
			{
				cc.val = c;
				cc.total = 1;
			} else cc.total++;
		}
		return ccInit;
	}

	private boolean isInterestingArc(ArcInst ai, HashSet arcsSeen)
	{
		// skip arcs already considered
		if (arcsSeen.contains(ai)) return false;

		// only want "unrouted" arc in generic technology
		if (ai.getProto() != Generic.tech.unrouted_arc) return false;

		return true;
	}

	/**
	 * Method to return true if nodeinst "ni" is an unrouted pin
	 */
	private boolean isUnroutedPin(NodeInst ni)
	{
		// only want the unrouted pin
		if (ni.getProto() != Generic.tech.unroutedPinNode &&
			ni.getProto() != Generic.tech.universalPinNode) return false;

		// found one
		return true;
	}

	/*************************************** CODE TO GENERATE CIRCUITRY ***************************************/

	private void makeTheGeometry(Cell cell)
	{
		HashSet<ArcInst> arcsToDelete = new HashSet<ArcInst>();
		HashSet<NodeInst> nodesToDelete = new HashSet<NodeInst>();
		for(RDESC q : rightP)
		{
			makeGeometry(q, cell);
			markToBeDeleted(q.unroutedWire1, arcsToDelete, nodesToDelete);
			if (q.unroutedWire1 != q.unroutedWire2) markToBeDeleted(q.unroutedWire2, arcsToDelete, nodesToDelete);
		}
		for(RDESC q : leftP)
		{
			makeGeometry(q, cell);
			markToBeDeleted(q.unroutedWire2, arcsToDelete, nodesToDelete);
			if (q.unroutedWire1 != q.unroutedWire2) markToBeDeleted(q.unroutedWire2, arcsToDelete, nodesToDelete);
		}
		killWires(cell, arcsToDelete, nodesToDelete);
	}

	private void markToBeDeleted(ArcInst ai, HashSet<ArcInst> arcsToDelete, HashSet<NodeInst> nodesToDelete)
	{
		if (!isInterestingArc(ai, arcsToDelete)) return;

		setFlags(ai, arcsToDelete, nodesToDelete);
		ArcInst ae = ai;  int e = 0;
		for(;;)
		{
			NodeInst ni = ae.getPortInst(e).getNodeInst();
			if (!isUnroutedPin(ni)) break;
			ArcInst oAi = null;
			for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
			{
				Connection con = it.next();
				oAi = con.getArc();
				if (!arcsToDelete.contains(oAi)) break;
				oAi = null;
			}
			if (oAi == null) break;
			setFlags(oAi, arcsToDelete, nodesToDelete);
			if (oAi.getPortInst(0).getNodeInst() == ae.getPortInst(e).getNodeInst()) e = 1; else e = 0;
			ae = oAi;
		}
		ae = ai;  e = 1;
		for(;;)
		{
			NodeInst ni = ae.getPortInst(e).getNodeInst();
			if (!isUnroutedPin(ni)) break;
			ArcInst oAi = null;
			for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
			{
				Connection con = it.next();
				oAi = con.getArc();
				if (!arcsToDelete.contains(oAi)) break;
				oAi = null;
			}
			if (oAi == null) break;
			setFlags(oAi, arcsToDelete, nodesToDelete);
			if (oAi.getPortInst(0).getNodeInst() == ae.getPortInst(e).getNodeInst()) e = 1; else e = 0;
			ae = oAi;
		}
	}

	private void setFlags(ArcInst ai, HashSet<ArcInst> arcsToDelete, HashSet<NodeInst> nodesToDelete)
	{
		arcsToDelete.add(ai);
		NodeInst niH = ai.getHeadPortInst().getNodeInst();
		if (isUnroutedPin(niH)) nodesToDelete.add(niH);
		NodeInst niT = ai.getTailPortInst().getNodeInst();
		if (isUnroutedPin(niT)) nodesToDelete.add(niT);
	}

	private void killWires(Cell cell, HashSet<ArcInst> arcsToDelete, HashSet<NodeInst> nodesToDelete)
	{
		for(ArcInst ai : arcsToDelete)
		{
			ai.kill();
		}
		for(NodeInst ni : nodesToDelete)
		{
			if (isUnroutedPin(ni))
				delNodeInst(ni);
		}
	}

	private void delNodeInst(NodeInst ni)
	{
		// see if any arcs connect to this node
		if (ni.hasConnections()) return;
//		if (ni.getNumConnections() > 0) return;

		// see if this nodeinst is a portinst of the cell
		if (ni.hasExports()) return;
//		if (ni.getNumExports() > 0) return;

		// now erase the nodeinst
		ni.kill();
	}

	/**
	 * make electric geometry
	 */
	private void makeGeometry(RDESC rd, Cell cell)
	{
		RPATH path = rd.path;

		Poly poly1 = rd.unroutedWire1.getPortInst(rd.unroutedEnd1).getPoly();
		wireBoundLX = poly1.getCenterX();
		wireBoundLY = poly1.getCenterY();
		Poly poly2 = rd.unroutedWire2.getPortInst(rd.unroutedEnd2).getPoly();
		wireBoundHX = poly2.getCenterX();
		wireBoundHY = poly2.getCenterY();

		NodeProto defNode = path.pathType.findPinProto();
		PortProto defPort = defNode.getPort(0); // there is always only one

		RPOINT prev = path.pathDesc;
		NodeInst prevNodeInst = theNode(rd, defNode, prev, cell);
		PortProto prevPort = thePort(defPort, rd, prev);
		PortInst prevPi = prevNodeInst.findPortInstFromProto(prevPort);

		for(RPOINT rp = prev.next; rp != null; rp = rp.next)
		{
			if (rp.next != null)
			{
				if (prev.x == rp.x && rp.x == rp.next.x) continue;
				if (prev.y == rp.y && rp.y == rp.next.y) continue;
			}
			NodeInst rpNodeInst = theNode(rd, defNode, rp, cell);
			PortProto rpPort = thePort(defPort, rd, rp);
			PortInst rpPi = rpNodeInst.findPortInstFromProto(rpPort);

			ArcInst ai = ArcInst.makeInstance(path.pathType, path.width, prevPi, rpPi);
			prev = rp;   prevPi = rpPi;
		}
	}

	private NodeInst theNode(RDESC rd, NodeProto dn, RPOINT p, Cell cell)
	{
		if (p.x == wireBoundLX && p.y == wireBoundLY)
			return rd.unroutedWire1.getPortInst(rd.unroutedEnd1).getNodeInst();

		if (p.x == wireBoundHX && p.y == wireBoundHY)
			return rd.unroutedWire2.getPortInst(rd.unroutedEnd2).getNodeInst();

		double wid = dn.getDefWidth();
		double hei = dn.getDefHeight();
		NodeInst ni = NodeInst.makeInstance(dn, new Point2D.Double(p.x, p.y), wid, hei, cell);
		return ni;
	}

	private PortProto thePort(PortProto dp, RDESC rd, RPOINT p)
	{
		if (p.x == wireBoundLX && p.y == wireBoundLY)
			return rd.unroutedWire1.getPortInst(rd.unroutedEnd1).getPortProto();

		if (p.x == wireBoundHX && p.y == wireBoundHY)
			return rd.unroutedWire2.getPortInst(rd.unroutedEnd2).getPortProto();

		return dp;
	}

	private boolean moveInstance()
	{
		NodeInst ni = moveCell;
		if (!moveCellValid || ni == null)
		{
			System.out.println("River router: Cannot determine cell to move");
			return false;
		}

		double lx = (routDirection == ROUTEINX ? height + ni.getAnchorCenterX() - toLine : ni.getAnchorCenterX());
		double ly = (routDirection == ROUTEINY ? height + ni.getAnchorCenterY() - toLine : ni.getAnchorCenterY());
		if (lx == ni.getAnchorCenterX() && ly == ni.getAnchorCenterY()) return true;
		ni.move(lx - ni.getAnchorCenterX(), ly - ni.getAnchorCenterY());
		return true;
	}
}
