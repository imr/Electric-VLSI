/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: River.java
 * Routines for the river-routing option of the routing tool
 * Written by: Telle Whitney, Schlumberger Palo Alto Research
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

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.NetworkTool;
import com.sun.electric.database.network.Global;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.menus.MenuCommands;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Collections;

/**
 *	River Route - takes two sets of parallel points (connectors, ports, etc) and routes wires
 *        between them.  All wires are routed in a single layer with non intersecting lines.
 *
 *
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
 *
 * Restrictions
 *   (1)     The distance between the ports (p1..pn) and (a1..an) is >= pitch
 *   (2)     The parameter "width" specifies the width of all wires
 *           The parameter "space" specifies the distance between wires
 *           pitch = 2*(width/2) + space = space + width
 *
 * Extension - allow routing to and from the two sides
 *
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
 */
public class River
{
	static class ROUTEINFO
	{
		private TRANSFORM     origmatrix, invmatrix;
		private List          rightp, leftp;		/* list of RDESC objects */
		private double        fromline;				/* the initial coordinate of the route */
		private double        toline;				/* final coordinate of the route */
		private double        startright;			/* where to start wires on the right */
		private double        startleft;			/* where to start wires on the left */
		private double        height;
		private double        llx, lly, urx, ury;
		private int           xx;					/*  ROUTEINX route in X direction,
												ROUTEINY route in Y direction */
		private RCOORD       xaxis, yaxis;		/* linked list of possible routing coordinates */
	};
	private ROUTEINFO ro_rrinfo = new ROUTEINFO();
	private double ro_rrbx, ro_rrby, ro_rrex, ro_rrey;
	private static NodeInst moveCell;
	private static boolean  moveCellValid;

	/******** TRANSFORM ********/

	static class TRANSFORM
	{
		private double t11, t12;					/* graphics transformation */
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
	private static final TRANSFORM ro_rrnorot     = new TRANSFORM( 1,  0,  0,  1, 0, 0);	/* X increasing, y2>y1 */
	private static final TRANSFORM ro_rrrot90     = new TRANSFORM( 0,  1, -1,  0, 0, 0);	/* Y decreasing, x2>x1 */
	private static final TRANSFORM ro_rrrot180    = new TRANSFORM(-1,  0,  0, -1, 0, 0);	/* X decreasing, y2<y1 */
	private static final TRANSFORM ro_rrrot270    = new TRANSFORM( 0, -1,  1,  0, 0, 0);	/* Y increasing, x2<x1 or rot -90 */
	private static final TRANSFORM ro_rrmirrorx   = new TRANSFORM(-1,  0,  0,  1, 0, 0);	/* X decreasing, y2>y1 mirror X coordinate, around Y axis */
	private static final TRANSFORM ro_rrrot90mirx = new TRANSFORM( 0,  1,  1,  0, 0, 0);	/* Y increasing, x2>x1 rot90 and mirror X */
	private static final TRANSFORM ro_rrmirrory   = new TRANSFORM( 1,  0,  0, -1, 0, 0);	/* X increasing, y2<y1 mirror Y coordinate, around X axis  */
	private static final TRANSFORM ro_rrmirxr90   = new TRANSFORM( 0, -1, -1,  0, 0, 0);	/* Y decreasing, x2<x1 mirror X, rot90 */
	private static final TRANSFORM ro_rrinverse   = new TRANSFORM( 1,  0,  0,  1, 0, 0);	/*tx,ty */

	private static final int ROUTEINX      = 1;
	private static final int ROUTEINY      = 2;
	private static final int ILLEGALROUTE  = -1;

	private static final int BOTTOP        = 1;					/* bottom to top  -- side 1 to side 3 */
	private static final int FROMSIDE      = 2;					/* side   to top  -- side 2 or side 4 to side 3 */
	private static final int TOSIDE        = 3;					/* bottom to side -- side 1 to side 3 or side 2 */

	/******** RPOINT ********/

	private static final int NOSIDE = -1;
	private static final int SIDE1  = 1;
	private static final int SIDE2  = 2;
	private static final int SIDE3  = 3;
	private static final int SIDE4  = 4;

	static class RPOINT
	{
		private int          	 side;				/* the side this point is on */
		private double           x, y;				/* points coordinates */
		private double           first, second;		/* nonrotated coordinates */
		private RPOINT           next;				/* next one in the list */

		RPOINT(double x, double y, int side)
		{
			this.side = side;
			this.x = x;
			this.y = y;
			this.first = 0;
			this.second = 0;
			this.next = null;
		}

		RPOINT(RPATH rp, double first, double sec, RPOINT next)
		{
			this.side = NOSIDE;
			this.x = 0;
			this.y = 0;
			this.first = first;
			this.second = sec;
			this.next = next;
			if (next != null) return;
			rp.lastp = this;
		}
	};

	/******** RPATH ********/

	static class RPATH
	{
		private double          width;				/* the width of this path */
		private ArcProto        pathtype;			/* the paty type for this wire */
		private int             routetype;			/* how the wire needs to be routed - as above */
		private RPOINT          pathdesc;			/* the path */
		private RPOINT          lastp;				/* the last point on the path */
		private RPATH           next;

		RPATH(double width, ArcProto ptype, int routetype)
		{
			this.width = width;
			this.pathtype = ptype;
			this.routetype = routetype;
			this.pathdesc = null;
			this.lastp = null;
			this.next = null;
		}
	};

	/******** RDESC ********/

	static class RDESC
	{
		private RPOINT         from;
		private RPOINT         to;
		private double         sortval;
		private ArcInst        unroutedwire1;
		private ArcInst        unroutedwire2;
		private int            unroutedend1;
		private int            unroutedend2;
		private RPATH          path;

		RDESC(double fx, double fy, int fside, double sx, double sy,
			int sside, ArcInst ai1, int ae1, ArcInst ai2, int ae2)
		{
			this.from = new RPOINT(fx, fy, fside);
			this.to = new RPOINT(sx, sy, sside);
			this.unroutedwire1 = ai1;   this.unroutedend1 = ae1;
			this.unroutedwire2 = ai2;   this.unroutedend2 = ae2;
			this.path = null;
		}
	};

	/******** RCOORD ********/

	static class RCOORD
	{
		private double       val;					/* the coordinate */
		private int          total;				/* number of wires voting for this coordinate */
		private RCOORD       next;

		RCOORD(double c)
		{
			this.val = c;
			this.total = 0;
			this.next = null;
		}
	};

	public static void riverRoute()
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;
		RiverRouteJob job = new RiverRouteJob(curCell);
	}

	private static class RiverRouteJob extends Job
	{
		private Cell cell;

		protected RiverRouteJob(Cell cell)
		{
			super("River Route", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			startJob();
		}

		public boolean doIt()
		{
			River router = new River();
			router.ro_river(cell);
			return true;
		}
	}

	private void ro_river(Cell cell)
	{
		// locate wires
		if (ro_findwires(cell))
		{
			// see if user selection is requested
//			if ((ro_state&SELECT) == 0)
//			{
				// make wires
				for(Iterator it = ro_rrinfo.rightp.iterator(); it.hasNext(); )
				{
					RDESC q = (RDESC)it.next();
					ro_checkthecell(q.unroutedwire2.getConnection(q.unroutedend2).getPortInst().getNodeInst());
				}
				for(Iterator it = ro_rrinfo.leftp.iterator(); it.hasNext(); )
				{
					RDESC q = (RDESC)it.next();
					ro_checkthecell(q.unroutedwire2.getConnection(q.unroutedend2).getPortInst().getNodeInst());
				}

				// if there is motion to be done, do it
				if (moveCellValid && moveCell != null)
				{
					if (ro_move_instance()) ro_makethegeometry(cell);
				} else ro_makethegeometry(cell);
//			} else
//			{
				// show where wires will go and allow user confirmation
//				ro_makepseudogeometry(cell);
//				if (ro_query_user()) ro_makethegeometry(cell);
//			}
		}
	}

	private void ro_makethegeometry(Cell cell)
	{
//		(void)asktool(us_tool, x_("clear"));
		HashSet arcsToDelete = new HashSet();
		HashSet nodesToDelete = new HashSet();
		for(Iterator it = ro_rrinfo.rightp.iterator(); it.hasNext(); )
		{
			RDESC q = (RDESC)it.next();
			ro_makegeometry(q, cell);
			ro_mark_tobedeleted(q.unroutedwire1, arcsToDelete, nodesToDelete);
			if (q.unroutedwire1 != q.unroutedwire2) ro_mark_tobedeleted(q.unroutedwire2, arcsToDelete, nodesToDelete);
		}
		for(Iterator it = ro_rrinfo.leftp.iterator(); it.hasNext(); )
		{
			RDESC q = (RDESC)it.next();
			ro_makegeometry(q, cell);
			ro_mark_tobedeleted(q.unroutedwire2, arcsToDelete, nodesToDelete);
			if (q.unroutedwire1 != q.unroutedwire2) ro_mark_tobedeleted(q.unroutedwire2, arcsToDelete, nodesToDelete);
		}
		ro_kill_wires(cell, arcsToDelete, nodesToDelete);
	}

	private void ro_mark_tobedeleted(ArcInst ai, HashSet arcsToDelete, HashSet nodesToDelete)
	{
		if (!ro_is_interesting_arc(ai, arcsToDelete)) return;

		ro_set_flags(ai, arcsToDelete, nodesToDelete);
		ArcInst ae = ai;  int e = 0;
		for(;;)
		{
			NodeInst ni = ae.getConnection(e).getPortInst().getNodeInst();
			if (!ro_isunroutedpin(ni)) break;
			ArcInst oAi = null;
			for(Iterator it = ni.getConnections(); it.hasNext(); )
			{
				Connection con = (Connection)it.next();
				oAi = con.getArc();
				if (!arcsToDelete.contains(oAi)) break;
				oAi = null;
			}
			if (oAi == null) break;
			ro_set_flags(oAi, arcsToDelete, nodesToDelete);
			if (oAi.getConnection(0).getPortInst().getNodeInst() == ae.getConnection(e).getPortInst().getNodeInst()) e = 1; else e = 0;
			ae = oAi;
		}
		ae = ai;  e = 1;
		for(;;)
		{
			NodeInst ni = ae.getConnection(e).getPortInst().getNodeInst();
			if (!ro_isunroutedpin(ni)) break;
			ArcInst oAi = null;
			for(Iterator it = ni.getConnections(); it.hasNext(); )
			{
				Connection con = (Connection)it.next();
				oAi = con.getArc();
				if (!arcsToDelete.contains(oAi)) break;
				oAi = null;
			}
			if (oAi == null) break;
			ro_set_flags(oAi, arcsToDelete, nodesToDelete);
			if (oAi.getConnection(0).getPortInst().getNodeInst() == ae.getConnection(e).getPortInst().getNodeInst()) e = 1; else e = 0;
			ae = oAi;
		}
	}

	private void ro_set_flags(ArcInst ai, HashSet arcsToDelete, HashSet nodesToDelete)
	{
		arcsToDelete.add(ai);
		NodeInst niH = ai.getHead().getPortInst().getNodeInst();
		if (ro_isunroutedpin(niH)) nodesToDelete.add(niH);
		NodeInst niT = ai.getTail().getPortInst().getNodeInst();
		if (ro_isunroutedpin(niT)) nodesToDelete.add(niT);
	}

	private void ro_kill_wires(Cell cell, HashSet arcsToDelete, HashSet nodesToDelete)
	{
		for(Iterator it = arcsToDelete.iterator(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			ai.kill();
		}
		for(Iterator it = nodesToDelete.iterator(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ro_isunroutedpin(ni))
				ro_delnodeinst(ni);
		}
	}

	private void ro_delnodeinst(NodeInst ni)
	{
		// see if any arcs connect to this node
		if (ni.getNumConnections() > 0) return;

		// see if this nodeinst is a portinst of the cell
		if (ni.getNumExports() > 0) return;

		// now erase the nodeinst
		ni.kill();
	}

	/**
	 * make electric geometry
	 */
	private void ro_makegeometry(RDESC rd, Cell cell)
	{
		RPATH path = rd.path;

		Poly poly1 = rd.unroutedwire1.getConnection(rd.unroutedend1).getPortInst().getPoly();
		ro_rrbx = poly1.getCenterX();
		ro_rrby = poly1.getCenterY();
		Poly poly2 = rd.unroutedwire2.getConnection(rd.unroutedend2).getPortInst().getPoly();
		ro_rrex = poly2.getCenterX();
		ro_rrey = poly2.getCenterY();

		NodeProto defnode = ((PrimitiveArc)path.pathtype).findPinProto();
		PortProto defport = defnode.getPort(0); // there is always only one

		RPOINT prev = path.pathdesc;
		NodeInst prevnodeinst = ro_thenode(rd, defnode, prev, cell);
		PortProto prevport = ro_theport(defport, rd, prev);

		for(RPOINT rp = prev.next; rp != null; rp = rp.next)
		{
			if (rp.next != null)
			{
				if (prev.x == rp.x && rp.x == rp.next.x) continue;
				if (prev.y == rp.y && rp.y == rp.next.y) continue;
			}
			NodeInst rpnodeinst = ro_thenode(rd, defnode, rp, cell);
			PortProto rpport = ro_theport(defport, rd, rp);

//			ArcInst ai = newarcinst(path.pathtype, path.width, FIXANG,
//				prevnodeinst, prevport, prev.x, prev.y, rpnodeinst, rpport, rp.x, rp.y, cell);
			prev = rp;   prevnodeinst = rpnodeinst;   prevport = rpport;
		}
	}

	private NodeInst ro_thenode(RDESC rd, NodeProto dn, RPOINT p, Cell cell)
	{
		if (p.x == ro_rrbx && p.y == ro_rrby)
			return rd.unroutedwire1.getConnection(rd.unroutedend1).getPortInst().getNodeInst();

		if (p.x == ro_rrex && p.y == ro_rrey)
			return rd.unroutedwire2.getConnection(rd.unroutedend2).getPortInst().getNodeInst();

		double wid = dn.getDefWidth();
		double hei = dn.getDefHeight();
		NodeInst ni = NodeInst.makeInstance(dn, new Point2D.Double(p.x, p.y), wid, hei, cell);
		return ni;
	}

	private PortProto ro_theport(PortProto dp, RDESC rd, RPOINT p)
	{
		if (p.x == ro_rrbx && p.y == ro_rrby)
			return rd.unroutedwire1.getConnection(rd.unroutedend1).getPortInst().getPortProto();

		if (p.x == ro_rrex && p.y == ro_rrey)
			return rd.unroutedwire2.getConnection(rd.unroutedend2).getPortInst().getPortProto();

		return dp;
	}

	private boolean ro_move_instance()
	{
		NodeInst ni = moveCell;
		if (!moveCellValid || ni == null)
		{
			System.out.println("River router: Cannot determine cell to move");
			return false;
		}

		double lx = (ro_rrinfo.xx == ROUTEINX ? ro_rrinfo.height + ni.getAnchorCenterX() - ro_rrinfo.toline : ni.getAnchorCenterX());
		double ly = (ro_rrinfo.xx == ROUTEINY ? ro_rrinfo.height + ni.getAnchorCenterY() - ro_rrinfo.toline : ni.getAnchorCenterY());
		if (lx == ni.getAnchorCenterX() && ly == ni.getAnchorCenterY()) return true;
		ni.modifyInstance(lx - ni.getAnchorCenterX(), ly - ni.getAnchorCenterY(), 0, 0, 0);
		return true;
	}

	/**
	 * once the route occurs, make some geometry and move some cells around
	 */
	private void ro_checkthecell(NodeInst ni)
	{
		if (ni.getProto() instanceof Cell)
		{
			// the node is nonprimitive
			if (!moveCellValid) return;
			if (moveCell == null)  // first one
				moveCell = ni;
			else if (moveCell != ni) moveCellValid = false;
		}
	}

	private boolean ro_findwires(Cell cell)
	{
		ro_initialize();

		// reset flags on all arcs in this cell
		HashSet arcsSeen = new HashSet();

		// make a list of RDESC objects
		List theList = new ArrayList();

		// get list of all highlighted arcs
		List allArcs = MenuCommands.getSelectedObjects(false, true);
		if (allArcs.size() != 0)
		{
			// add all highlighted arcs to the list of RDESC objects
			for(Iterator it = allArcs.iterator(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				ro_addwire(theList, ai, arcsSeen);
			}
		} else
		{
			// add all arcs in the cell to the list of RDESC objects
			for(Iterator it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				ro_addwire(theList, ai, arcsSeen);
			}
		}

		// determine bounds of the routes
		boolean first = true;
		for(Iterator it = theList.iterator(); it.hasNext(); )
		{
			RDESC rdesc = (RDESC)it.next();
			if (first)
			{
				ro_rrinfo.llx = Math.min(rdesc.from.x, rdesc.to.x);
				ro_rrinfo.lly = Math.min(rdesc.from.y, rdesc.to.y);
				ro_rrinfo.urx = Math.max(rdesc.from.x, rdesc.to.x);
				ro_rrinfo.ury = Math.max(rdesc.from.y, rdesc.to.y);
			} else
			{
				ro_rrinfo.llx = Math.min(Math.min(ro_rrinfo.llx, rdesc.from.x), rdesc.to.x);
				ro_rrinfo.lly = Math.min(Math.min(ro_rrinfo.lly, rdesc.from.y), rdesc.to.y);
				ro_rrinfo.urx = Math.max(Math.max(ro_rrinfo.urx, rdesc.from.x), rdesc.to.x);
				ro_rrinfo.ury = Math.max(Math.max(ro_rrinfo.ury, rdesc.from.y), rdesc.to.y);
			}
		}

		// figure out which ArcProto to use
		HashMap arcProtoUsage = new HashMap();
		for(Iterator it = theList.iterator(); it.hasNext(); )
		{
			RDESC rd = (RDESC)it.next();
			ro_sumup(rd.unroutedwire1.getConnection(rd.unroutedend1), arcProtoUsage);
			ro_sumup(rd.unroutedwire2.getConnection(rd.unroutedend2), arcProtoUsage);
		}

		// find the most popular ArcProto
		ArcProto wantAp = null;
		int mostUses = -1;
		int total = 0;
		for(Iterator it = arcProtoUsage.keySet().iterator(); it.hasNext(); )
		{
			ArcProto ap = (ArcProto)it.next();
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

		ro_figureoutrails(total);
		ro_set_wires_to_rails(theList);

		// figure out the worst design rule spacing for this type of arc
		Technology.ArcLayer [] arcLayers = ((PrimitiveArc)wantAp).getLayers();
		Layer layer = arcLayers[0].getLayer();
		double amt = DRC.getMaxSurround(layer, Double.MAX_VALUE);
		if (amt < 0) amt = 1;
		return ro_unsorted_rivrot(wantAp, theList, wantAp.getDefaultWidth() - wantAp.getWidthOffset(), amt, amt, amt);
	}

	/**
	 * takes two unsorted list of ports and routes between them
	 * warning - if the width is not even, there will be round off problems
	 */
	private boolean ro_unsorted_rivrot(ArcProto layerdesc, List lists, double width,
		double space, double celloff1, double celloff2)
	{
		for(Iterator it = lists.iterator(); it.hasNext(); )
		{
			RDESC rd = (RDESC)it.next();
			rd.sortval = (ro_rrinfo.xx != ROUTEINX ? rd.from.x : rd.from.y);
		}
		Collections.sort(lists, new SortRDESC());

		return ro_sorted_rivrot(layerdesc, lists, width, space,
			celloff1, celloff2);
	}

	public static class SortRDESC implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			RDESC s1 = (RDESC)o1;
			RDESC s2 = (RDESC)o2;
			if (s1.sortval == s2.sortval) return 0;
			if (s1.sortval > s2.sortval) return 1;
			return -1;
		}
	}

	/**
	 * takes two sorted list of ports and routes between them
	 * warning - if the width is not even, there will be round off problems
	 */
	private boolean ro_sorted_rivrot(ArcProto layerdesc, List listr, double width,
		double space, double celloff1, double celloff2)
	{
		// ports invalid
		if (!ro_check_points(listr, width, space)) return false;
		ro_structure_points(listr);				// put in left/right
		if (!ro_check_structured_points(ro_rrinfo.rightp, ro_rrinfo.leftp, celloff1, width, space))
			return false;
		if (ro_process_right(width, layerdesc, ro_rrinfo.rightp, celloff1, space, -1)) return false;
		if (ro_process_left(width, layerdesc, ro_rrinfo.leftp, celloff1, space, 1)) return false;
		double fixedheight = 0;
		Double height = ro_calculate_height_and_process(ro_rrinfo.rightp, ro_rrinfo.leftp, width, celloff2, fixedheight);
		if (height == null) return false;
		ro_calculate_bb(ro_rrinfo.rightp, ro_rrinfo.leftp);
		ro_rrinfo.height = height.doubleValue();
		return true;
	}

	private void ro_calculate_bb(List right, List left)
	{
		ro_rrinfo.llx = ro_rrinfo.lly = Double.MAX_VALUE;
		ro_rrinfo.urx = ro_rrinfo.ury = Double.MIN_VALUE;
		for(Iterator it = right.iterator(); it.hasNext(); )
		{
			RDESC rright = (RDESC)it.next();
			for(RPOINT rvp = rright.path.pathdesc; rvp != null; rvp = rvp.next)
			{
				ro_rrinfo.llx = Math.min(ro_rrinfo.llx, rvp.x);
				ro_rrinfo.lly = Math.min(ro_rrinfo.lly, rvp.y);
				ro_rrinfo.urx = Math.max(ro_rrinfo.urx, rvp.x);
				ro_rrinfo.ury = Math.max(ro_rrinfo.ury, rvp.y);
			}
		}
		for(Iterator it = left.iterator(); it.hasNext(); )
		{
			RDESC lleft = (RDESC)it.next();
			for(RPOINT rvp = lleft.path.pathdesc; rvp != null; rvp = rvp.next)
			{
				ro_rrinfo.llx = Math.min(ro_rrinfo.llx, rvp.x);
				ro_rrinfo.lly = Math.min(ro_rrinfo.lly, rvp.y);
				ro_rrinfo.urx = Math.max(ro_rrinfo.urx, rvp.x);
				ro_rrinfo.ury = Math.max(ro_rrinfo.ury, rvp.y);
			}
		}
	}

	private Double ro_calculate_height_and_process(List right, List left, double width, double co2, double minheight)
	{
		double maxheight = Double.MIN_VALUE;
		for(Iterator it = right.iterator(); it.hasNext(); )
		{
			RDESC rd = (RDESC)it.next();
			maxheight = Math.max(maxheight, rd.path.lastp.second);
		}
		for(Iterator it = left.iterator(); it.hasNext(); )
		{
			RDESC rd = (RDESC)it.next();
			maxheight = Math.max(maxheight, rd.path.lastp.second);
		}

		if (minheight != 0) maxheight = Math.max(minheight, maxheight+(width/2)+co2);
			else maxheight = maxheight+(width/2)+co2;
		maxheight = Math.max(maxheight, ro_rrinfo.toline);

		// make sure its at least where the coordinates are
		for(Iterator it = right.iterator(); it.hasNext(); )
		{
			RDESC rd = (RDESC)it.next();
			RPOINT lastp = rd.path.lastp;
			if (lastp.side != SIDE2)
			{
				lastp.next = new RPOINT(rd.path, lastp.first, maxheight, null);
			}
			ro_remap_points(rd.path.pathdesc, ro_rrinfo.invmatrix);
		}
		for(Iterator it = left.iterator(); it.hasNext(); )
		{
			RDESC rd = (RDESC)it.next();
			RPOINT lastp = rd.path.lastp;
			if (lastp.side != SIDE4)
			{
				lastp.next = new RPOINT(rd.path, lastp.first, maxheight, null);
			}
			ro_remap_points(rd.path.pathdesc, ro_rrinfo.invmatrix);
		}
		ro_rrinfo.toline = ro_remap_second(ro_rrinfo.toline, ro_rrinfo.invmatrix);
		ro_rrinfo.fromline = ro_remap_second(ro_rrinfo.fromline, ro_rrinfo.invmatrix);
		return new Double(ro_remap_second(maxheight, ro_rrinfo.invmatrix));
	}

	/**
	 * calculate the height of the channel, and remap the points back into the
	 * original coordinate system
	 */
	private void ro_remap_points(RPOINT rp, TRANSFORM matrix)
	{
		for(; rp != null; rp = rp.next)
		{
			rp.x = (rp.first*matrix.t11) + (rp.second*matrix.t21);
			rp.y = (rp.first*matrix.t12) + (rp.second*matrix.t22);
		}
	}

	private double ro_remap_second(double sec, TRANSFORM matrix)
	{
		if (ro_rrinfo.xx == ROUTEINY) return sec * matrix.t22;
		return sec * matrix.t12;
	}

	private boolean ro_process_left(double width, ArcProto ptype, List rout, double co1, double space, double dir)
	{
		boolean firsttime = true;
		RPATH lastp = null;
		double offset = ro_rrinfo.startright;
		for(Iterator it = rout.iterator(); it.hasNext(); )
		{
			RDESC rd = (RDESC)it.next();
			if (rd.from.side != SIDE2)
			{
				if (firsttime)
				{
					rd.path = ro_makeorigpath(width, ptype, co1, rd.from, rd.to);
					if (rd.path == null) return true;
					firsttime = false;
				} else
					rd.path = ro_addpath(lastp, width, ptype, rd.from, rd.to, space, co1, dir);
				if (rd.path == null) return true;
			} else
			{
				if (firsttime)
				{
					rd.path = ro_makesideorigpath(width, ptype, offset, rd.from, rd.to);
					if (rd.path == null) return true;
					firsttime = false;
				} else
				{
					rd.path = ro_sideaddpath(lastp, width, ptype, rd.from, rd.to, space, offset, dir);
					if (rd.path == null) return true;
				}
				offset += space+width;
			}
			lastp = rd.path;
		}
		return false;
	}

	private boolean ro_process_right(double width, ArcProto ptype, List rout, double co1, double space, int dir)
	{
		boolean firsttime = true;
		RPATH lastp = null;
		double offset = ro_rrinfo.startleft;

		ro_reverse(rout);
		for(Iterator it = rout.iterator(); it.hasNext(); )
		{
			RDESC rd = (RDESC)it.next();
			if (rd.from.side != SIDE4)
			{
				// starting from bottom (side1)
				if (firsttime)
				{
					rd.path = ro_makeorigpath(width, ptype, co1, rd.from, rd.to);
					if (rd.path == null) return true;
					firsttime = false;
				} else
					rd.path = ro_addpath(lastp, width, ptype, rd.from, rd.to, space, co1, dir);
				if (rd.path == null) return true;
			} else
			{
				if (firsttime)
				{
					rd.path = ro_makesideorigpath(width, ptype, offset, rd.from, rd.to);
					if (rd.path == null) return true;
					firsttime = false;
				} else
				{
					rd.path = ro_sideaddpath(lastp, width, ptype, rd.from, rd.to, space, offset, dir);
					if (rd.path == null) return true;
				}
				offset += space+width;
			}
			lastp = rd.path;
		}
		ro_reverse(rout);  // return to normal
		return false;
	}

	private RPATH ro_sideaddpath(RPATH path, double width, ArcProto ptype, RPOINT b, RPOINT t,
		double space, double offset, double dir)
	{
		RPATH rp = new RPATH(width, ptype, ro_routepathtype(b, t));
		rp.pathdesc = new RPOINT(rp, b.first, offset, null);

		double minfirst = Math.min(b.first, t.first);
		double maxfirst = Math.max(b.first, t.first);
		RPOINT lp = path.pathdesc;
		RPOINT lastp = rp.lastp;

		double newfirst = lp.first+dir*(space+rp.width);
		while (lp != null && minfirst <= newfirst && newfirst <= maxfirst)
		{
			// if first point then inconsistent second(y) offset 
			if (lp == path.pathdesc)
				lastp.next = new RPOINT(rp, newfirst, Math.min(lastp.second, offset), null);
			else
				lastp.next = new RPOINT(rp, newfirst, Math.max(lp.second+space+rp.width, offset), null);
			lastp = lastp.next;   lp = lp.next;
			if (lp != null) newfirst = lp.first+dir*(space+rp.width);
		}
		lastp.next = new RPOINT(rp, t.first, lastp.second, null);
		rp.lastp.side = t.side;
		return(rp);
	}

	private RPATH ro_addpath(RPATH path, double width, ArcProto ptype, RPOINT b, RPOINT t,
		double space, double co1, double dir)
	{
		RPATH rp = new RPATH(width, ptype, ro_routepathtype(b, t));
		RPOINT i1 = new RPOINT(rp, b.first, b.second+(rp.width/2)+co1, null);
		rp.pathdesc = new RPOINT(rp, b.first, b.second, i1);
		double minfirst = Math.min(b.first, t.first);
		double maxfirst = Math.max(b.first, t.first);
		RPOINT lp = path.pathdesc;
		RPOINT lastp = rp.lastp;

		double newfirst = lp.first+dir*(space+rp.width);
		while (lp != null && minfirst <= newfirst && newfirst <= maxfirst)
		{
			// if first point then inconsistent second(y) offset
			if (lp == path.pathdesc)
				lastp.next = new RPOINT(rp, newfirst, lastp.second, null); else
					lastp.next = new RPOINT(rp, newfirst, lp.second+space+rp.width, null);
			lastp = lastp.next;   lp = lp.next;
			if (lp != null) newfirst = lp.first+dir*(space+rp.width);
		}
		lastp.next = new RPOINT(rp, t.first, lastp.second, null);
		rp.lastp.side = t.side;
		return rp;
	}

	private RPATH ro_makeorigpath(double width, ArcProto ptype, double co1, RPOINT b, RPOINT t)
	{
		RPATH rp = new RPATH(width, ptype, ro_routepathtype(b, t));

		RPOINT i1 = new RPOINT(rp, t.first, b.second+(width/2)+co1, null);
		RPOINT i2 = new RPOINT(rp, b.first, b.second+(width/2)+co1, i1);
		rp.pathdesc = new RPOINT(rp, b.first, b.second, i2);
		rp.lastp.side = t.side;
		return rp;
	}

	private RPATH ro_makesideorigpath(double width, ArcProto ptype, double startoff, RPOINT b, RPOINT t)
	{
		RPATH rp = new RPATH(width, ptype, ro_routepathtype(b, t));
		RPOINT i1 = new RPOINT(rp, t.first, startoff, null);
		rp.pathdesc = new RPOINT(rp, b.first, startoff, i1);
		rp.lastp.side = t.side;
		return rp;
	}

	/**
	 * the type of route for this wire: side to top, bottom to side, bottom to top
	 */
	private int ro_routepathtype(RPOINT b, RPOINT t)
	{
		if (b != null && t != null)
		{
			if (b.side != SIDE1) return FROMSIDE;
			if (b.side != SIDE3) return TOSIDE;
		}
		return BOTTOP;
	}

	private void ro_reverse(List p)
	{
		int total = p.size();
		if (total <= 1) return;

		for(int i=0; i<total/2; i++)
		{
			int otherI = total - i - 1;
			Object early = p.get(i);
			Object late = p.get(otherI);
			p.set(i, late);
			p.set(otherI, early);
		}
	}

	private boolean ro_check_structured_points(List right, List left, double co1, double width, double space)
	{
		boolean fromside1 = false;
		boolean toside2 = false;
		double botoffs2 = 0;

		// ensure ordering is correct
		for(Iterator it = right.iterator(); it.hasNext(); )
		{
			RDESC r = (RDESC)it.next();
			switch (r.from.side)
			{
				case SIDE1:
					fromside1 = true;
					break;
				case SIDE4:
					if (fromside1)
					{
						System.out.println("River router: Improper ordering of bottom right ports");
						return false;
					}
					break;
				default:
					System.out.println("River router: Improper sides for bottom right ports");
					return false;
			}
			switch (r.to.side)
			{
				case SIDE2:
					if (!toside2) botoffs2 = ro_rrinfo.fromline+co1+(width/2);
						else botoffs2 += space+width;
					toside2 = true;
					break;
				case SIDE3:
					if (toside2)
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

		boolean fromside2 = false;   boolean toside3 = false;   boolean toside4 = false;   double botoffs4 = 0;
		for(Iterator it = left.iterator(); it.hasNext(); )
		{
			RDESC l = (RDESC)it.next();
			switch (l.from.side)
			{
				case SIDE1:
					if (fromside2)
					{
						System.out.println("River router: Improper Ordering of Bottom Left Ports");
						return false;
					}
					break;
				case SIDE2:
					fromside2 = true;
					break;
				default:
					System.out.println("River router: Improper sides for Bottom Left Ports");
					return false;
			}
			switch (l.to.side)
			{
				case SIDE3:
					toside3 = true;
					break;
				case SIDE4:
					if (!toside3)
					{
						if (!toside4) botoffs4 = ro_rrinfo.fromline+co1+(width/2);
							else botoffs4 += space+width;
					} else
					{
						System.out.println("River router: Improper Ordering of Top Left Ports");
						return false;
					}
					toside4 = true;
					break;
				default:
					System.out.println("River router: Improper sides for Top Left Ports");
					return false;
			}
		}
		if (botoffs2 == 0) ro_rrinfo.startright = ro_rrinfo.fromline+co1+(width/2);
			else	       ro_rrinfo.startright = botoffs2+space+width;
	
		if (botoffs4 == 0) ro_rrinfo.startleft = ro_rrinfo.fromline+co1+(width/2);
			else	       ro_rrinfo.startleft = botoffs4+space+width;
		return true;
	}

	private void ro_structure_points(List listr)
	{
		ro_rrinfo.rightp = new ArrayList();
		ro_rrinfo.leftp = new ArrayList();
		for(Iterator it = listr.iterator(); it.hasNext(); )
		{
			RDESC rd = (RDESC)it.next();
			if (rd.to.first >= rd.from.first) ro_rrinfo.rightp.add(rd);
				else ro_rrinfo.leftp.add(rd);
		}
	}

	private boolean ro_check_points(List rdescList, double width, double space)
	{
		int numRdesc = rdescList.size();
		if (numRdesc == 0)
		{
			// need at least one point
			System.out.println("River router: Not enought points");
			return false;
		}

		RDESC listlast = (RDESC)rdescList.get(numRdesc-1);
		if (listlast.from == null || listlast.to == null)
		{
			System.out.println("River router: Not the same number of points");
			return false;	// not the same number of points
		}

		// decide route orientation
		RDESC listp = (RDESC)rdescList.get(0);
		TRANSFORM tmatrix = null;
		double val1 = 0, val2 = 0;
		if (ro_rrinfo.xx == ROUTEINX)
		{
			// route in x direction
			if (listp.to.x >= listp.from.x)
			{											// x2>x1
				if (listlast.from.y >= listp.from.y)
					tmatrix = ro_rrrot90mirx;			// Y increasing
						else tmatrix = ro_rrrot90;		// Y decreasing
			} else
			{											// x2<x1
				if (listlast.from.y >= listp.from.y)
					tmatrix = ro_rrrot270;				// Y increasing
						else tmatrix = ro_rrmirxr90;	// Y decreasing
			}
			val1 = ro_rrinfo.fromline = ro_rrinfo.fromline * tmatrix.t12;
			val2 = ro_rrinfo.toline = ro_rrinfo.toline * tmatrix.t12;
		} else if (ro_rrinfo.xx == ROUTEINY)
		{
			// route in y direction
			if (listp.to.y >= listp.from.y)
			{											// y2>y1
				if (listlast.from.x >= listp.from.x)
					tmatrix = ro_rrnorot;				// X increasing
						else tmatrix = ro_rrmirrorx;	// X decreasing
			} else
			{											// y2<y1
				if (listlast.from.x >= listp.from.x)
					tmatrix = ro_rrmirrory;				// X increasing
						else tmatrix = ro_rrrot180;		// X decreasing
			}
			val1 = ro_rrinfo.fromline = ro_rrinfo.fromline * tmatrix.t22;
			val2 = ro_rrinfo.toline = ro_rrinfo.toline * tmatrix.t22;
		} else
		{
			System.out.println("River router: Not between two parallel lines");
			return false;		// not on manhattan parallel lines
		}

		// check ordering of coordinates
		for(int i=0; i<numRdesc-1; i++)
		{
			RDESC llist = (RDESC)rdescList.get(i);
			RDESC llistNext = (RDESC)rdescList.get(i+1);

			// make sure there are no crossings
			if (ro_rrinfo.xx == ROUTEINY)
			{
				if ((llist.from.x > llistNext.from.x && llist.to.x < llistNext.to.x) ||
					(llist.from.x < llistNext.from.x && llist.to.x > llistNext.to.x))
				{
					System.out.println("River router: Connections may not cross");
					return false;
				}
			} else
			{
				if ((llist.from.y > llistNext.from.y && llist.to.y < llistNext.to.y) ||
					(llist.from.y < llistNext.from.y && llist.to.y > llistNext.to.y))
				{
					System.out.println("River router: Connections may not cross");
					return false;
				}
			}
		}

		double bound1 = ro_rrinfo.llx * tmatrix.t11 + ro_rrinfo.lly * tmatrix.t21;
		double bound2 = ro_rrinfo.urx * tmatrix.t11 + ro_rrinfo.ury * tmatrix.t21;
		if (bound2 < bound1)
		{
			double temp = bound2;   bound2 = bound1;   bound1 = temp;
		}
		RPOINT lastfrom = null;   RPOINT lastto = null;

		// transform points and clip to boundary
		for(Iterator it = rdescList.iterator(); it.hasNext(); )
		{
			RDESC llist = (RDESC)it.next();
			llist.from.first = (llist.from.x * tmatrix.t11) + (llist.from.y * tmatrix.t21);
			llist.from.second = (llist.from.x * tmatrix.t12) + (llist.from.y * tmatrix.t22);
			llist.to.first = (llist.to.x * tmatrix.t11) + (llist.to.y * tmatrix.t21);
			llist.from.second = (llist.to.x * tmatrix.t12) + (llist.to.y * tmatrix.t22);
			if (llist.from.second != val1) ro_clipwire(llist.from, bound1, bound2);
			if (llist.to.second != val2)  ro_clipwire(llist.to, bound1, bound2);

			if (lastfrom != null && llist.from.side == SIDE1)
			{
				double diff1 = Math.abs(lastfrom.first - llist.from.first);
				if (diff1 < width+space)
				{
					System.out.println("River router: Ports not design rule distance apart");
					return false;
				}
			}
			if (lastto != null && llist.to.side == SIDE3)
			{
				double diff2 = Math.abs(lastto.first - llist.to.first);
				if (diff2 < width+space)
				{
					System.out.println("River router: Ports not design rule distance apart");
					return false;
				}
			}		// not far enough apart
			lastfrom = (llist.from.side == SIDE1) ? llist.from : null;
			lastto = (llist.to.side == SIDE3) ? llist.to : null;
		}

		// matrix to take route back to original coordinate system
		ro_rrinverse.t11 = tmatrix.t11;   ro_rrinverse.t12 = tmatrix.t21;
		ro_rrinverse.t21 = tmatrix.t12;   ro_rrinverse.t22 = tmatrix.t22;
		ro_rrinverse.tx = listp.from.first; ro_rrinverse.ty = listp.from.second;
									// right now these last terms are not used
		ro_rrinfo.origmatrix = tmatrix;   ro_rrinfo.invmatrix = ro_rrinverse;
		ro_rrinfo.fromline = val1;   ro_rrinfo.toline = val2;
		return true;
	}

	private void ro_clipwire(RPOINT p, double b1, double b2)
	{
		double diff1 = Math.abs(b1 - p.first);
		double diff2 = Math.abs(b2 - p.first);
		if (diff1 < diff2)
		{
			p.first = b1;   p.side = SIDE4;
		} else
		{
			p.first = b2;   p.side = SIDE2;
		}
	}

	private void ro_set_wires_to_rails(List lists)
	{
		for(Iterator it = lists.iterator(); it.hasNext(); )
		{
			RDESC r = (RDESC)it.next();
			double fval = ro_point_val(r.from, ro_rrinfo.xx);
			double tval = ro_point_val(r.to, ro_rrinfo.xx);
			if ((fval != ro_rrinfo.fromline && tval == ro_rrinfo.fromline) ||
				(tval != ro_rrinfo.toline && fval == ro_rrinfo.toline))
					ro_swap_points(r);
		}
	}

	private void ro_swap_points(RDESC r)
	{
		if (r.from.side != SIDE1 || r.to.side != SIDE3)
			System.out.println("River router: Unexpected side designation");

		RPOINT tmp = r.from;   r.from = r.to;   r.to = tmp;

		r.from.side = SIDE1;   r.to.side = SIDE3;
		ArcInst tmpwire = r.unroutedwire1;   int tmpe = r.unroutedend1;
		r.unroutedwire1 = r.unroutedwire2;   r.unroutedend1 = r.unroutedend2;
		r.unroutedwire2 = tmpwire;           r.unroutedend2 = tmpe;
	}

	private double ro_point_val(RPOINT rp, int xx)
	{
		return xx == ROUTEINX ? rp.x : rp.y;
	}

	private void ro_figureoutrails(int total)
	{
		RCOORD lx = ro_largest(ro_rrinfo.xaxis);
		RCOORD ly = ro_largest(ro_rrinfo.yaxis);
		RCOORD nlx = ro_next_largest(ro_rrinfo.xaxis, lx);
		RCOORD nly = ro_next_largest(ro_rrinfo.yaxis, ly);

		// determine the type of route
		RCOORD from = null;
		RCOORD to = null;
		int fxx = ILLEGALROUTE;
		if (lx != null && nlx != null && lx.total == total && nlx.total == total)
		{
			from = lx;   to = nlx;
			fxx = ROUTEINX;
		} else if (ly != null && nly != null && ly.total == total && nly.total == total)
		{
			from = ly;   to = nly;
			fxx = ROUTEINY;
		} else if (lx != null && lx.total == (2*total))
		{
			from = to = lx;
			fxx = ROUTEINX;
		} else if (ly != null && ly.total == (2*total))
		{
			from = to = ly;
			fxx = ROUTEINY;
		}

		if (fxx == ILLEGALROUTE)
		{
			if (lx.total >= total)
			{
				// lx.total == total --- the other one an unusual case
				// lx.total > total  --- both go to the same line
				fxx = ROUTEINX;   from = lx;
				to = (lx.total > total ? lx : nlx);
			} else if (ly.total >= total)
			{
				// ly.total == total --- the other one an unusual case
				// ly.total > total  --- both go to the same line
				fxx = ROUTEINY;   from = ly;
				to = (ly.total > total ? ly : nly);
			} else
			{
				fxx = (((ly.total+nly.total)>=(lx.total+nlx.total)) ? ROUTEINY : ROUTEINX);
				from = (fxx == ROUTEINY ? ly : lx);
				to = (fxx == ROUTEINY ? nly : nlx);
			}
		}

		if (to.val < from.val)
		{
			RCOORD tmp = from;   from = to;   to = tmp;
		}

		ro_rrinfo.xx = fxx;
		ro_rrinfo.fromline = from.val;   ro_rrinfo.toline = to.val;
	}

	private RCOORD ro_largest(RCOORD cc)
	{
		RCOORD largest = cc;

		for(; cc != null; cc = cc.next)
		{
			if (cc.total > largest.total) largest = cc;
		}
		return largest;
	}

	private RCOORD ro_next_largest(RCOORD cc, RCOORD largest)
	{
		RCOORD nlargest = null;

		for( ; cc != null; cc = cc.next)
		{
			if (nlargest == null && cc != largest) nlargest = cc; else
				if (nlargest != null && cc != largest && cc.total > nlargest.total) nlargest = cc;
		}
		return nlargest;
	}

	/**
	 * for every layer (or arcproto) that this PORT allows to connect to it,
	 * increment the flag bits (temp1) IN the prototype thus indicating that
	 * this river route point is allowed to connect to it
	 */
	private void ro_sumup(Connection con, HashMap arcProtoUsage)
	{
		ArcProto [] possibleArcs = con.getPortInst().getPortProto().getBasePort().getConnections();
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

	private void ro_initialize()
	{
		ro_rrinfo.rightp = null;
		ro_rrinfo.leftp = null;
		ro_rrinfo.origmatrix = null;
		ro_rrinfo.invmatrix = null;
		ro_rrinfo.fromline = ro_rrinfo.toline = Double.MIN_VALUE;
		ro_rrinfo.startright = Double.MIN_VALUE;
		ro_rrinfo.startleft = Double.MIN_VALUE;
		ro_rrinfo.height = Double.MIN_VALUE;
		ro_rrinfo.xx = ILLEGALROUTE;

		ro_rrinfo.xaxis = null;
		ro_rrinfo.yaxis = null;
		for(RCOORD c = ro_rrinfo.xaxis; c != null; c = c.next) c.total = -1;
		for(RCOORD c = ro_rrinfo.yaxis; c != null; c = c.next) c.total = -1;
		moveCell = null;   moveCellValid = true;
	}

	/**
	 * figure out the wires to route at all
	 */
	private void ro_addwire(List list, ArcInst ai, HashSet arcsSeen)
	{
		if (!ro_is_interesting_arc(ai, arcsSeen)) return;

		arcsSeen.add(ai);
		ArcInst ae1 = ai;   int e1 = 0;
		for(;;)
		{
			NodeInst ni = ae1.getConnection(e1).getPortInst().getNodeInst();
			if (!ro_isunroutedpin(ni)) break;
			ArcInst oAi = null;
			for(Iterator it = ni.getConnections(); it.hasNext(); )
			{
				Connection con = (Connection)it.next();
				oAi = con.getArc();
				if (!arcsSeen.contains(oAi)) break;
				oAi = null;
			}
			if (oAi == null) break;
			arcsSeen.add(oAi);
			if (oAi.getConnection(0).getPortInst().getNodeInst() == ni) e1 = 1; else e1 = 0;
			ae1 = oAi;
		}
		ArcInst ae2 = ai;   int e2 = 1;
		for(;;)
		{
			NodeInst ni = ae2.getConnection(e2).getPortInst().getNodeInst();
			if (!ro_isunroutedpin(ni)) break;
			ArcInst oAi = null;
			for(Iterator it = ni.getConnections(); it.hasNext(); )
			{
				Connection con = (Connection)it.next();
				oAi = con.getArc();
				if (!arcsSeen.contains(oAi)) break;
				oAi = null;
			}
			if (oAi == null) break;
			arcsSeen.add(oAi);
			if (oAi.getConnection(0).getPortInst().getNodeInst() == ni) e2 = 1; else e2 = 0;
			ae2 = oAi;
		}

		PortInst pi1 = ae1.getConnection(e1).getPortInst();
		Poly poly1 = pi1.getPoly();
		double bx = poly1.getCenterX();
		double by = poly1.getCenterY();
		PortInst pi2 = ae2.getConnection(e2).getPortInst();
		Poly poly2 = pi2.getPoly();
		double ex = poly2.getCenterX();
		double ey = poly2.getCenterY();
		RDESC rd = new RDESC(bx, by, SIDE1, ex, ey, SIDE3, ae1, e1, ae2, e2);
		list.add(rd);
		ro_vote(rd.from.x, rd.from.y, rd.to.x, rd.to.y);
	}

	private void ro_vote(double ffx, double ffy, double ttx, double tty)
	{
		ro_rrinfo.xaxis = ro_tallyvote(ro_rrinfo.xaxis, ffx);
		ro_rrinfo.yaxis = ro_tallyvote(ro_rrinfo.yaxis, ffy);
		ro_rrinfo.xaxis = ro_tallyvote(ro_rrinfo.xaxis, ttx);
		ro_rrinfo.yaxis = ro_tallyvote(ro_rrinfo.yaxis, tty);
	}

	/**
	 * Figure out which way to route (x and y) and the top coordinate and
	 * bottom coordinate
	 */
	private RCOORD ro_tallyvote(RCOORD cc, double c)
	{
		if (cc == null)
		{
			cc = new RCOORD(c);
			cc.total = 1;
			return cc;
		}

		RCOORD ccinit = cc;
		RCOORD cclast = null;
		for( ; (cc != null && cc.total >= 0 && cc.val != c); cclast = cc, cc = cc.next) ;
		if (cc == null)
		{
			cc = new RCOORD(c);
			cclast.next = cc;
			cc.total = 1;
			return ccinit;
		} else
		{
			if (cc.total < 0)
			{
				cc.val = c;
				cc.total = 1;
			} else cc.total++;
		}
		return ccinit;
	}

	private boolean ro_is_interesting_arc(ArcInst ai, HashSet arcsSeen)
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
	private boolean ro_isunroutedpin(NodeInst ni)
	{
		// only want the unrouted pin
		if (ni.getProto() != Generic.tech.unroutedPinNode &&
			ni.getProto() != Generic.tech.universalPinNode) return false;

		// found one
		return true;
	}
}

//	void ro_pseudomake(RDESC *rd, NODEPROTO *cell)
//	{
//		RPATH *path;
//		REGISTER RPOINT *rp, *prev;
//
//		path = rd->path;
//
//		prev = path->pathdesc;
//		for(rp = prev->next; rp != NULLRPOINT; rp = rp->next)
//		{
//			if (rp->next)
//			{
//				if (prev->x == rp->x && rp->x == rp->next->x) continue;
//				if (prev->y == rp->y && rp->y == rp->next->y) continue;
//			}
//			(void)asktool(us_tool, x_("show-line"), prev->x, prev->y, rp->x, rp->y, cell);
//			prev = rp;
//		}
//		ro_checkthecell(rd->unroutedwire2->end[rd->unroutedend2].nodeinst);
//	}
//	
//	/*
//	 * draw lines on the screen denoting what the route would look
//	 * like if it was done
//	 */
//	void ro_makepseudogeometry(NODEPROTO *cell)
//	{
//		RDESC *q;
//		INTBIG lambda;
//
//		lambda = lambdaofcell(cell);
//		ttyputmsg(_("Routing bounds %s <= X <= %s   %s <= Y <= %s"), latoa(ro_rrinfo.llx, lambda),
//			latoa(ro_rrinfo.urx, lambda), latoa(ro_rrinfo.lly, lambda), latoa(ro_rrinfo.ury, lambda));
//
//		// remove highlighting
//		(void)asktool(us_tool, x_("clear"));
//
//		for(q = ro_rrinfo.rightp; q != NULLRDESC; q = q->next) ro_pseudomake(q, cell);
//		for(q = ro_rrinfo.leftp; q != NULLRDESC; q = q->next) ro_pseudomake(q, cell);
//	}
//	
//	BOOLEAN ro_query_user(void)
//	{
//		CHAR *par[MAXPARS];
//		INTBIG count;
//
//		// wait for user response
//		for(;;)
//		{
//			count = ttygetparam(_("River-route option: "), &ro_riverp, MAXPARS, par);
//			if (count == 0) continue;
//			if (par[0][0] == 'r') break;
//			if (par[0][0] == 'm')
//			{
//				if (ro_move_instance()) return(TRUE);
//			}
//			if (par[0][0] == 'a') return(FALSE);
//		}
//		return(TRUE);
//	}
