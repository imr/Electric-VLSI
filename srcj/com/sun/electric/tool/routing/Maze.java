/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Maze.java
 * Maze routing
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
import com.sun.electric.database.geometry.Geometric;
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
import java.awt.geom.AffineTransform;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Comparator;
import java.util.Collections;

public class Maze
{
	/** bit width of long word */		private static final int SRMAXLAYERS = 64;
	/** maximum size of maze */			private static final int MAXGRIDSIZE = 1000;
	/** max grid points to "excavate" for initial grid access from a port */	private static final int BLOCKAGELIMIT =  10;

	/** draw only on vertical layer */	private static final int SCH_HORILAYER = 0;
	/** draw all layers */				private static final int SCH_VERTLAYER = 1;
	/** draw all layers */				private static final int SCH_ALLLAYER  = 2;

	// common bit masks
	/** clear all bits */				private static final byte SR_GCLEAR = (byte)0xFF;
	/** grid set (permanent) */			private static final byte SR_GSET   = (byte)0x80;
	/** no via is allowed */			private static final byte SR_GNOVIA = 0x40;
	/** is a wavefront poin */			private static final byte SR_GWAVE  = 0x20;
	/** is a port */					private static final byte SR_GPORT  = 0x10;

	// for maze cells
	/** mask 4 bits */					private static final byte SR_GMASK  = 0x0F;
	/** maximum mark value */			private static final byte SR_GMAX   = 15;
	/** start value */					private static final byte SR_GSTART = 1;

	// miscellaneous defines, return codes from ro_mazeexpand_wavefront
	private static final int SRSUCCESS  = 0;
	private static final int SRERROR    = 1;
	private static final int SRROUTED   = 2;
	private static final int SRBLOCKED  = 3;
	private static final int SRUNROUTED = 4;

	private ArcProto ro_mazevertwire, ro_mazehoriwire;
	private NodeProto ro_mazesteiner;
	private SRREGION  ro_theregion = null;
	private int ro_mazegridx, ro_mazegridy;
	private int ro_mazeoffsetx, ro_mazeoffsety;
	private int ro_mazeboundary;

	static class SRDIRECTION {}
	SRDIRECTION SRVERTPREF = new SRDIRECTION();
	SRDIRECTION SRHORIPREF = new SRDIRECTION();
	SRDIRECTION SRALL = new SRDIRECTION();

	static class SRMODE {}
	SRMODE SR_MOR = new SRMODE();
	SRMODE SR_MAND = new SRMODE();
	SRMODE SR_MSET = new SRMODE();

	/**
	 * Path segment definition, note x, y values are in world coordinates.
	 * End always defines vias (even if the original technology does not have any).
	 */
	static class SREND {}
	SREND SREPORT = new SREND();
	SREND SREVIA = new SREND();
	SREND SREVIAUP = new SREND();
	SREND SREVIADN = new SREND();
	SREND SREEND = new SREND();

	static class SRPTYPE {}
	SRPTYPE SRPFIXED = new SRPTYPE();
	SRPTYPE SRPROUTED = new SRPTYPE();

	/**
	 * Defines a routing region to the router
	 */
	static class SRREGION
	{
		/** lower bound of the region */	int        lx, ly;
		/** upper bound of the region */	int        hx, hy;
		/** the array of layers */			SRLAYER [] layers;
		/** the list of nets */				SRNET      nets;

		SRREGION()
		{
			layers = new SRLAYER[SRMAXLAYERS];
		}
	};

	static class SRLAYER
	{
												byte     [] hused, vused;
		/** the layer index (sort order) */		int         index;
		/** the layer mask */					long        mask;
		/** translation value for grid */		int         transx, transy;
		/** the width and height of the grid */	int         wid, hei;
		/** the grid units */					int         gridx, gridy;
		/** bounds of the current maze */		int         lx, ly, hx, hy;
		/** the two dimensional grid array */	byte   [][] grids;
		/** up/down pointer to next layer */	SRLAYER     up, down;
		/** allowed direction of routes */		SRDIRECTION dir;
	};

	/**
	 * Defines a net in a region. Note that no existing segments of a net is predefined.
	 * Only ports are allowed. Not all nets in region need to be defined.
	 */
	static class SRNET
	{
		/** route state flag */				boolean  routed;
		/** the parent region */			SRREGION region;
		/** the list of ports on the net */	SRPORT   ports;
		/** the list of paths */			SRPATH   paths;
		/** the last path in the list */	SRPATH   lastpath;
		/** next in the list of nets */		SRNET    next;
	};

	/**
	 * Defines a port on a net. Bounds of the port are in grid units.
	 * An index of 0 means all layers, 1 = 1st layer, 2 = 2nd layer, 4 - 3rd layer, etc.
	 */
	static class SRPORT
	{
		/** the port index */					int       index;
		/** the layer mask */					long      layers;
		/** bounds of the port */				int       lx, ly, hx, hy;
		/** node that this port comes from */	NodeInst  ni;
		/** port on the originating node */		PortProto pp;
		/** the master (connected) port */		SRPORT    master;
		/** the list of connected paths */		SRPATH    paths;
		/** the last path in the list */		SRPATH    lastpath;
		/** the current wave front */			SRWAVEPT  wavefront;
		/** the parent net */					SRNET     net;
		/** next in the list of ports */		SRPORT    next;
	};

	static class SRPATH
	{
		/** end points of the path */		int []   x, y;
		/** end pt in world units */		int []   wx, wy;
		/** the layer of the path */		SRLAYER  layer;
		/** end style of the path */		SREND [] end;
		/** the type of path */				SRPTYPE  type;
		/** the port path is attached to */	SRPORT   port;
		/** next in the list of paths */	SRPATH   next;

		SRPATH()
		{
			x = new int[2];
			y = new int[2];
			wx = new int[2];
			wy = new int[2];
			end = new SREND[2];
		}
	};

	/**
	 * routing data types
	 */
	static class SRWAVEPT
	{
		/** x y location of the point */		int      x, y;
		/** the layer for the point */			SRLAYER  layer;
		/** the port for this point */			SRPORT   port;
		/** next/prev in the list of points */	SRWAVEPT next, prev;
	};

	/************************************* TOP-LEVEL CONTROL CODE *************************************/

	public static void mazeRoute()
	{
		Cell curCell = WindowFrame.needCurCell();
		if (curCell == null) return;
		MazeRouteJob job = new MazeRouteJob(curCell);
	}

	private static class MazeRouteJob extends Job
	{
		private Cell cell;

		protected MazeRouteJob(Cell cell)
		{
			super("Maze Route", Routing.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			startJob();
		}

		public boolean doIt()
		{
			Maze router = new Maze();
			router.ro_mazerouteselected(cell);
			return true;
		}
	}

	/**
	 * Method to replace the selected unrouted arcs with routed geometry
	 */
	private void ro_mazerouteselected(Cell cell)
	{
		WindowFrame wf = WindowFrame.getCurrentWindowFrame();
		if (wf == null) return;
		Highlighter hi = wf.getContent().getHighlighter();
		if (hi == null) return;

		Netlist netList = cell.getUserNetlist();
		Set nets = hi.getHighlightedNetworks();
		if (nets == null)
		{
			nets = new HashSet();
			for(Iterator it = netList.getNetworks(); it.hasNext(); )
			{
				Network net = (Network)it.next();
				nets.add(net);
			}
		}

		for(Iterator it = nets.iterator(); it.hasNext(); )
		{
			Network net = (Network)it.next();

			// see if there are unrouted
			if (ro_mazeroutenet(net, netList, hi)) return;
		}
	}

	/**
	 * Method to reroute networks "net".  Returns true on error.
	 */
	private boolean ro_mazeroutenet(Network net, Netlist netList, Highlighter hi)
	{
		// get extent of net and mark nodes and arcs on it
		HashSet arcsToDelete = new HashSet();
		HashSet nodesToDelete = new HashSet();
		List netEnds = Routing.findNetEnds(net, arcsToDelete, nodesToDelete, netList);
		int count = netEnds.size();
		if (count != 2)
		{
			System.out.println("Can only route nets with 2 ends, this has " + count);
			return true;
		}

		// determine grid size
		ro_mazegridy = ro_mazegridx = 1;
		ro_mazeboundary = ro_mazegridx * 20;

		// determine bounds of this networks
		Cell cell = net.getParent();
		Rectangle2D routingBounds = null;
		for(Iterator it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			Network aNet = netList.getNetwork(ai, 0);
			if (aNet != net) continue;
			Rectangle2D arcBounds = ai.getBounds();
			if (routingBounds == null) routingBounds = arcBounds; else
			{
				Rectangle2D.union(routingBounds, arcBounds, routingBounds);
			}
		}
		if (routingBounds == null)
		{
			System.out.println("Internal error: no bounding area for routing");
			return true;
		}

		// turn off highlighting
		hi.clear();
		hi.finished();

		// remove marked networks
		for(Iterator it = arcsToDelete.iterator(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			ai.kill();
		}
		for(Iterator it = nodesToDelete.iterator(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			ni.kill();
		}

		// now create the routing region
		int lx = (int)routingBounds.getMinX();
		int hx = (int)routingBounds.getMaxX();
		int ly = (int)routingBounds.getMinY();
		int hy = (int)routingBounds.getMaxY();
		SRREGION region = ro_mazedefine_region(cell, lx, ly, hx, hy);
		if (region == null) return true;

		// create the net in the region
		SRNET srnet = ro_mazeadd_net(region);
		if (srnet == null)
		{
			System.out.println("Could not allocate internal net");
			return true;
		}

		// add the ports to the net
		for(int i=0; i<count; i++)
		{
			PortInst pi = (PortInst)netEnds.get(i);
			Poly poly = pi.getPoly();
			double cX = poly.getCenterX();
			double cY = poly.getCenterY();
			SRPORT fsp = ro_mazeadd_port(srnet, ro_mazedetermine_dir(pi.getNodeInst(), cX, cY), lx, ly, hx, hy, pi);
			if (fsp == null)
			{
				System.out.println("Port could not be defined");
				return true;
			}
		}

		// do maze routing
		if (ro_mazeroute_net(srnet))
		{
			System.out.println("Could not route net");
			return true;
		}

		// extract paths to create arcs
		if (ro_mazeextract_paths(cell, srnet))
		{
			System.out.println("Could not create paths");
			return true;
		}

		return false;
	}

	/************************************* CODE TO TRAVERSE THE MAZE BUFFER *************************************/

	private boolean ro_mazeroute_net(SRNET net)
	{
		// presume routing with the current arc
		boolean ret = false;
		ArcProto routingarc = User.tool.getCurrentArcProto();
		if (routingarc == Generic.tech.unrouted_arc) routingarc = null;
		if (routingarc != null)
		{
			// see if the default arc can be used to route
			for(SRPORT port = net.ports; port != null; port = port.next)
			{
				PortProto pp = port.pp;
				ArcProto [] connections = pp.getBasePort().getConnections();
				boolean found = false;
				for(int i = 0; i < connections.length; i++)
					if (connections[i] == routingarc) { found = true;   break; }
				if (!found) { routingarc = null;   break; }
			}
		}

		// if current arc cannot run, look for any that can
		if (routingarc == null)
		{
			// check out all arcs for use in this route
			HashSet arcsUsed = new HashSet();
			for(SRPORT port = net.ports; port != null; port = port.next)
			{
				PortProto pp = port.pp;
				ArcProto [] connections = pp.getBasePort().getConnections();
				for(int i = 0; i < connections.length; i++)
					arcsUsed.add(connections[i]);
			}
			for(Iterator it = arcsUsed.iterator(); it.hasNext(); )
			{
				ArcProto ap = (ArcProto)it.next();
				boolean allFound = true;
				for(SRPORT port = net.ports; port != null; port = port.next)
				{
					PortProto pp = port.pp;
					ArcProto [] connections = pp.getBasePort().getConnections();
					boolean found = false;
					for(int i = 0; i < connections.length; i++)
						if (connections[i] == ap) { found = true;   break; }
					if (!found) { allFound = false;   break; }
				}
				if (allFound) { routingarc = ap;   break; }
			}
		}
		if (routingarc == null)
		{
			System.out.println("Cannot find wire to route");
			return true;
		}
		ro_mazevertwire = routingarc;
		ro_mazehoriwire = routingarc;
		ro_mazesteiner = ((PrimitiveArc)routingarc).findPinProto();

		// initialize all layers and ports for this route
		for (int index = 0; index < SRMAXLAYERS; index++)
		{
			SRLAYER layer = net.region.layers[index];
			if (layer != null)
			{
				// unused bounds
				layer.lx = layer.wid; layer.hx = -1;
				layer.ly = layer.hei; layer.hy = -1;

				// now set the vertical and horizontal preference
				int count = 0;
				for (int x = 0; x < layer.wid; x++)
				{
					if ((layer.vused[x] & SR_GSET)  != 0)
					{
						if (count != 0)
						{
							for (int i = 1, prio = count>>1; i <= count; i++, prio--)
								layer.vused[x-i] = (byte)Math.abs(prio);
							count = 0;
						}
					} else count++;
				}

				// get the remainder
				if (count != 0)
				{
					for (int i = 1, prio = count>>1; i < count; i++)
					{
						layer.vused[layer.wid-i] = (byte)Math.abs(prio);
						if (--prio == 0 && (count&1) == 0) prio = -1;
					}
				}

				// now horizontal tracks
				count = 0;
				for (int y = 0; y < layer.hei; y++)
				{
					if ((layer.hused[y] & SR_GSET) != 0)
					{
						if (count != 0)
						{
							for (int i = 1, prio = count>>1; i <= count; i++, prio--)
								layer.hused[y-i] = (byte)Math.abs(prio);
							count = 0;
						}
					} else count++;
				}

				// and the remainder ...
				if (count != 0)
				{
					for (int i = 1, prio = count>>1; i < count; i++)
					{
						layer.hused[layer.hei-i] = (byte)Math.abs(prio);
						if (--prio == 0 && (count&1) == 0) prio = -1;
					}
				}
			}
		}

		// prepare each port for routing
		int pcount = 0;
		SRPORT port = null;
		for (port = net.ports; port != null; port = port.next, pcount++)
			ro_mazecreate_wavefront(port);

		// now begin routing until all ports merged
		byte code = SR_GSTART;
		do
		{
			if (++code > SR_GMAX) code = SR_GSTART;
			int blocked = 0;
			int status = 0;
			for (port = net.ports; port != null; port = port.next)
			{
				// if part of other wavefront, get the next one
				if (port.master != null) continue;

				// expand the wavefront
				status = ro_mazeexpand_wavefront(port, code);
				if (status == SRERROR) return true;
				if (status == SRROUTED) break;
				if (status == SRBLOCKED) blocked++;
			}

			// check for successful routing
			if (port != null && status == SRROUTED)
			{
				// now clear routing region and restart expansion
				ro_mazeclear_maze(net);
				if (--pcount > 1)
				{
					// prepare each port for routing
					for (port = net.ports; port != null; port = port.next)
						ro_mazecreate_wavefront(port);
				}
				code = SR_GSTART;
			} else
			{
				// check for blocked net
				if (blocked == pcount)
				{
					ret = true;
					ro_mazeclear_maze(net);
					break;
				}
			}
		} while (pcount > 1);

		// move all the port paths to the net
		for (port = net.ports; port != null; port = port.next)
		{
			if (net.lastpath == null)
			{
				net.paths = port.paths;
				if (net.paths != null) net.lastpath = port.lastpath;
			} else
			{
				net.lastpath.next = port.paths;
				if (net.lastpath.next != null) net.lastpath = port.lastpath;
			}
			port.paths = null;
			port.lastpath = null;
		}
		if (!ret) net.routed = true;
		return ret;
	}

	private void ro_mazecreate_wavefront(SRPORT port)
	{
		SRPORT master = port.master;
		if (master == null) master = port;

		// first assign each layer of the port as wavefront points
		for (int index = 0, mask = 1; index < SRMAXLAYERS; index++, mask = mask<<1)
		{
			if ((mask & port.layers) != 0)
			{
				SRLAYER layer = port.net.region.layers[index];
				if (layer == null) continue;

				// convert to grid points
				int lx = GRIDX(port.lx, layer);   int ly = GRIDY(port.ly, layer);
				int hx = GRIDX(port.hx, layer);   int hy = GRIDY(port.hy, layer);
				if (lx >= layer.wid || hx < 0 || ly >= layer.hei || hy < 0) continue;

				// clip to window
				if (lx < 0) lx = 0;
				if (hx >= layer.wid) hx = layer.wid - 1;
				if (ly < 0) ly = 0;
				if (hy >= layer.hei) hy = layer.hei - 1;

				/*
				 * added detection of immediate blockage ... smr
				 */
				boolean onedge = false;
				for (int x = lx; x <= hx; x++)
				{
					for (int y = ly; y <= hy; y++)
					{
						ro_mazeadd_wavept(master, layer, x, y, SR_GSTART);
						if (x < layer.wid-1 && layer.grids[x+1][y] == 0) onedge = true;
						if (x > 0 && layer.grids[x-1][y] == 0) onedge = true;
						if (y < layer.hei-1 && layer.grids[x][y+1] == 0) onedge = true;
						if (y > 0 && layer.grids[x][y-1] == 0) onedge = true;
					}
				}
				if (!onedge)
				{
					// port is inside of blocked area: search for opening
					int cx = (lx + hx) / 2;
					int cy = (ly + hy) / 2;
					int angrange = port.pp.getBasePort().getAngleRange();
					int ang = port.pp.getBasePort().getAngle();
					ang += (port.ni.getAngle()+5) / 10;
					if (port.ni.isMirroredAboutXAxis() != port.ni.isMirroredAboutYAxis()) { ang = 270 - ang; if (ang < 0) ang += 360; }
					if (ro_anglediff(ang, 0) <= angrange)
					{
						// port faces right
						for(int spread=1; spread<BLOCKAGELIMIT; spread++)
						{
							if (hx+spread >= layer.wid) break;
							if (layer.grids[hx+spread][cy] == 0) { onedge = true;   break; }
							layer.grids[hx+spread][cy] = 0;
						}
					}
					if (ro_anglediff(ang, 90) <= angrange)
					{
						// port faces up
						for(int spread=1; spread<BLOCKAGELIMIT; spread++)
						{
							if (hy+spread >= layer.hei) break;
							if (layer.grids[cx][hy+spread] == 0) { onedge = true;   break; }
							layer.grids[cx][hy+spread] = 0;
						}
					}
					if (ro_anglediff(ang, 180) <= angrange)
					{
						// port faces left
						for(int spread=1; spread<BLOCKAGELIMIT; spread++)
						{
							if (lx-spread < 0) break;
							if (layer.grids[lx-spread][cy] == 0) { onedge = true;   break; }
							layer.grids[lx-spread][cy] = 0;
						}
					}
					if (ro_anglediff(ang, 270) <= angrange)
					{
						// port faces down
						for(int spread=1; spread<BLOCKAGELIMIT; spread++)
						{
							if (ly-spread < 0) break;
							if (layer.grids[cx][ly-spread] == 0) { onedge = true;   break; }
							layer.grids[cx][ly-spread] = 0;
						}
					}
					if (!onedge)
					{
						System.out.println("Node %s is blocked" + port.ni.describe());
						return;
					}
				}
			}
		}

		// now assign the paths of the port
		for (SRPATH path = port.paths; path != null; path = path.next)
		{
			// note paths are always in the working area
			if (path.x[0] == path.x[1])
			{
				// vertical path
				int dy = -1;
				if (path.y[0] < path.y[1]) dy = 1;
				for (int x = path.x[0], y = path.y[0];
					(dy < 0) ? y >= path.y[1] : y <= path.y[1]; y += dy)
				{
					ro_mazeadd_wavept(master, path.layer, x, y, SR_GSTART);
				}
			} else if (path.y[0] == path.y[1])
			{
				// horizontal path
				int dx = -1;
				if (path.x[0] < path.x[1]) dx = 1;
				for (int y = path.y[0], x = path.x[0];
					(dx < 0) ? x >= path.x[1] : x <= path.x[1]; x += dx)
				{
					ro_mazeadd_wavept(master, path.layer, x, y, SR_GSTART);
				}
			} else
			{
				// a 45 degree path, note assume x,y difference is equal
				int dx = -1;
				if (path.x[0] < path.x[1]) dx = 1;
				int dy = -1;
				if (path.y[0] < path.y[1]) dy = 1;

				for (int x = path.x[0], y = path.y[0];
					(dx < 0) ? x >= path.x[1] : x <= path.x[1]; x += dx, y += dy)
				{
					ro_mazeadd_wavept(master, path.layer, x, y, SR_GSTART);
				}
			}
		}
	}

	/* routing commands */
	private void ro_mazeadd_wavept(SRPORT port, SRLAYER layer, int x, int y, byte code)
	{
		SRWAVEPT wavept = new SRWAVEPT();
		wavept.x = x;
		wavept.y = y;

		// set the grid
		layer.grids[x][y] = (byte)((layer.grids[x][y] & ~SR_GMASK) | code | SR_GWAVE);
		wavept.layer = layer;

		// set maze bounds
		if (layer.lx > x) layer.lx = x;
		if (layer.hx < x) layer.hx = x;
		if (layer.ly > y) layer.ly = y;
		if (layer.hy < y) layer.hy = y;

		wavept.prev = null;
		if (port.master != null)
		{
			wavept.port = port.master;
			wavept.next = port.master.wavefront;
			port.master.wavefront = wavept;
		} else
		{
			wavept.port = port;
			wavept.next = port.wavefront;
			port.wavefront = wavept;
		}
		if (wavept.next != null)
			wavept.next.prev = wavept;
	}

	private int ro_anglediff(int ang1, int ang2)
	{
		int diff = Math.abs(ang1 - ang2);
		if (diff > 180) diff = 360 - diff;
		return diff;
	}

	private int ro_mazeexpand_wavefront(SRPORT port, byte code)
	{
		// begin expansion of all wavepts
		// disconnect wavepts from the port
		SRWAVEPT wavept = port.wavefront;
		if (wavept == null) return SRBLOCKED;

		SRWAVEPT next = null;
		int status = SRSUCCESS;
		boolean found = false;
		int bx = 0, by = 0;
		SRLAYER blayer = null;
		SRWAVEPT bwavept = new SRWAVEPT();
		for (wavept = port.wavefront; wavept != null ; wavept = next)
		{
			boolean connected = false;
			SRLAYER layer = wavept.layer;
			if (layer.dir == SRALL || layer.dir == SRHORIPREF)
			{
				// try horizontal route
				int x = wavept.x + 1;
				if (x != layer.wid)
				{
					status = ro_mazeexamine_pt(port, layer, x, wavept.y, code);
					if (status == SRROUTED)
					{
						// "bwavept" used in proper order
						if (!found || (layer.hused[bwavept.y] & SR_GSET) != 0 ||
							((layer.hused[wavept.y] & SR_GSET) == 0 &&
							layer.hused[wavept.y] < layer.hused[bwavept.y]))
						{
							bwavept.x = wavept.x;   bwavept.y = wavept.y;   
							bwavept.layer = wavept.layer;   bwavept.port = wavept.port;
							bx = x;  by = wavept.y;
							blayer = layer;
						}
						found = true;
						connected = true;
					} else if (status == SRERROR) return SRERROR;
				}
				if (!connected && (x = wavept.x - 1) >= 0)
				{
					status = ro_mazeexamine_pt(port, layer, x, wavept.y, code);
					if (status == SRROUTED)
					{
						if (!found ||
							(layer.hused[bwavept.y] & SR_GSET) != 0  ||
							((layer.hused[wavept.y] & SR_GSET) == 0 &&
							layer.hused[wavept.y] < layer.hused[bwavept.y]))
						{
							bwavept.x = wavept.x;   bwavept.y = wavept.y;   
							bwavept.layer = wavept.layer;   bwavept.port = wavept.port;
							bx = x; by = wavept.y;
							blayer = layer;
						}
						found = true;
						connected = true;
					} else if (status == SRERROR) return SRERROR;
				}
			}
			if (layer.dir == SRALL || layer.dir == SRVERTPREF)
			{
				// try vertical route
				int y = wavept.y + 1;
				if (!connected && y != layer.hei)
				{
					status = ro_mazeexamine_pt(port, layer, wavept.x, y, code);
					if (status == SRROUTED)
					{
						if (!found ||
							(layer.vused[bwavept.x] & SR_GSET) != 0  ||
							((layer.vused[wavept.x] & SR_GSET) == 0 &&
							layer.vused[wavept.x] < layer.vused[bwavept.x]))
						{
							bwavept.x = wavept.x;   bwavept.y = wavept.y;   
							bwavept.layer = wavept.layer;   bwavept.port = wavept.port;
							bx = wavept.x; by = y;
							blayer = layer;
						}
						found = true;
						connected = true;
					} else if (status == SRERROR) return SRERROR;
				}
				if (!connected && (y = wavept.y - 1) >= 0)
				{
					status = ro_mazeexamine_pt(port, layer, wavept.x, y, code);
					if (status == SRROUTED)
					{
						if (!found ||
							(layer.vused[bwavept.x] & SR_GSET) != 0  ||
							((layer.vused[wavept.x] & SR_GSET) == 0 &&
							layer.vused[wavept.x] < layer.vused[bwavept.x]))
						{
							bwavept.x = wavept.x;   bwavept.y = wavept.y;   
							bwavept.layer = wavept.layer;   bwavept.port = wavept.port;
							bx = wavept.x; by = y;
							blayer = layer;
						}
						found = true;
						connected = true;
					} else if (status == SRERROR) return SRERROR;
				}
			}
			if (!connected && layer.up != null)
			{
				// try via up
				status = ro_mazeexamine_pt(port, layer.up, wavept.x, wavept.y, code);
				if (status == SRROUTED)
				{
					if (!found)
					{
						bwavept.x = wavept.x;   bwavept.y = wavept.y;   
						bwavept.layer = wavept.layer;   bwavept.port = wavept.port;
						bx = wavept.x; by = wavept.y;
						blayer = layer.up;
					}
					found = true;
					connected = true;
				} else if (status == SRERROR) return SRERROR;
			}
			if (!connected && layer.down != null)
			{
				// try via down
				status = ro_mazeexamine_pt(port, layer.down, wavept.x, wavept.y, code);
				if (status == SRROUTED)
				{
					if (!found)
					{
						bwavept.x = wavept.x;   bwavept.y = wavept.y;   
						bwavept.layer = wavept.layer;   bwavept.port = wavept.port;
						bx = wavept.x; by = wavept.y;
						blayer = layer.down;
					}
					found = true;
					connected = true;
				} else if (status == SRERROR) return SRERROR;
			}
			next = wavept.next;

			// now release this wavept
			if (wavept.prev == null)
			{
				port.wavefront = wavept.next;
				if (wavept.next != null)
				wavept.next.prev = null;
			} else
			{
				wavept.prev.next = wavept.next;
				if (wavept.next != null)
				wavept.next.prev = wavept.prev;
			}

			// set the grid point to a core point
			if (!connected) layer.grids[wavept.x][wavept.y] &= ~SR_GWAVE;
		}

		if (found)
			return ro_mazeinit_path(port, blayer, bwavept, bx, by);

		if (port.wavefront == null) return SRBLOCKED;
		return SRSUCCESS;
	}

	private int ro_mazeinit_path(SRPORT port, SRLAYER layer, SRWAVEPT wavept, int x, int y)
	{
		// search for others
		SRPORT target = null;
		SRWAVEPT twavept = null;
		for (target = port.net.ports; target != null; target = target.next)
		{
			if (target == port || target.master != null) continue;
			twavept = ro_mazesearch_wavefront(target, layer, x, y);
			if (twavept != null) break;
		}
		if (target == null) return SRERROR;

		/* now move the target's paths to the master. This is done to retain the
		 * original path creation order (port out), and also insures the existance
		 * of the arc in t-junction connections
		 */
		if (port.lastpath != null)
		{
			if ((port.lastpath.next = target.paths) != null)
				port.lastpath = target.lastpath;
		} else
		{
			// this should never happen
			port.paths = target.paths;
			port.lastpath = target.lastpath;
		}
		target.paths = null;
		target.lastpath = null;

		// connect the port with target
		SRPATH path = null;
		if (wavept.layer == twavept.layer)
		{
			path = ro_mazeget_path(port, layer, SREEND, SREEND, wavept.x, wavept.y,
				twavept.x, twavept.y);
			if (path == null) return SRERROR;
		}

		// now create paths to each target point
		if (ro_mazefind_paths(wavept, path) != SRSUCCESS) return SRERROR;
		if (ro_mazefind_paths(twavept, path) != SRSUCCESS) return SRERROR;

		// now set the target master
		target.master = port;

		// now scan through all ports and change target as master to port
		for (SRPORT sport = port.net.ports; sport != null; sport = sport.next)
		{
			if (sport.master == target) sport.master = port;
		}

		// now move the rest of the paths to the master port
		if (port.lastpath != null)
		{
			if ((port.lastpath.next = target.paths) != null)
				port.lastpath = target.lastpath;
		} else
		{
			// this should never happen
			port.paths = target.paths;
			port.lastpath = target.lastpath;
		}
		target.paths = null;
		target.lastpath = null;

		return SRROUTED;
	}

	/**
	 * Method to find the path through the maze to the target point.
	 * Will merge the starting point path with the first internal path if possible.
	 */
	private int ro_mazefind_paths(SRWAVEPT wavept, SRPATH path)
	{
		// Start scan from the first point
		int sx = wavept.x;
		int sy = wavept.y;

		SRLAYER layer = wavept.layer;
		byte code = (byte)(layer.grids[sx][sy] & SR_GMASK);
		if (code == SR_GSTART) code = SR_GMAX;
			else code--;
		int pstart = 0;
		int ex = 0, ey = 0;
		for(;;)
		{
			int dx = 0, dy = 0;

			// scan around the point
			for(;;)
			{
				if (pstart == 1)
				{
					// always try to jump layer after the first path
					// now try jumping layers
					if (layer.up != null)
					{
						ex = sx; ey = sy;
						int status = ro_mazetest_pt(layer.up.grids[ex][ey], code);
						if (status == SRROUTED) return SRSUCCESS;
						if (status == SRSUCCESS)
						{
							layer = layer.up;
							if (code == SR_GSTART) code = SR_GMAX;
								else code--;
							pstart = 2;
							continue;
						}
					}
					if (layer.down != null)
					{
						ex = sx; ey = sy;
						int status = ro_mazetest_pt(layer.down.grids[ex][ey], code);
						if (status == SRROUTED) return SRSUCCESS;
						if (status == SRSUCCESS)
						{
							layer = layer.down;
							if (code == SR_GSTART) code = SR_GMAX;
								else code--;
							pstart = 2;
							continue;
						}
					}
				}
				if (layer.dir == SRALL || layer.dir == SRHORIPREF)
				{
					// try right first
					if ((ex = sx + 1) != layer.wid)
					{
						ey = sy;
						int status = ro_mazetest_pt(layer.grids[ex][ey], code);
						if (status == SRROUTED)
						{
							// check for common original path
							if (path != null && path.layer == layer &&
								path.y[0] == path.y[1] && path.y[0] == ey &&
									Math.max(path.x[0], path.x[1]) == sx)
							{
								if (path.x[0] == sx)
								{
									path.x[0] = ex;
									path.wx[0] = WORLDX(ex, layer);
								} else
								{
									path.x[1] = ex;
									path.wx[1] = WORLDX(ex, layer);
								}
								ro_mazeset_line(path.layer, (byte)(SR_GPORT | SR_GSET),
									path.wx[0], path.wy[0], path.wx[1], path.wy[1], SR_MOR);
								return SRSUCCESS;
							}
							if (ro_mazeget_path(wavept.port, layer, SREEND, SREPORT, sx, sy, ex, ey) == null)
								return SRERROR;
							return SRSUCCESS;
						} else if (status == SRSUCCESS)
						{
							dx = 1;
							break;
						}
					}
				}
				if (layer.dir == SRALL || layer.dir == SRVERTPREF)
				{
					// try down first
					if ((ey = sy - 1) >= 0)
					{
						ex = sx;
						int status = ro_mazetest_pt(layer.grids[ex][ey], code);
						if (status == SRROUTED)
						{
							// check for common original path
							if (path != null && path.layer == layer &&
								path.x[0] == path.x[1] && path.x[0] == ex &&
									Math.min(path.y[0], path.y[1]) == sy)
							{
								if (path.y[0] == sy)
								{
									path.y[0] = ey;
									path.wy[0] = WORLDY(ey, layer);
								} else
								{
									path.y[1] = ey;
									path.wy[1] = WORLDY(ey, layer);
								}
								ro_mazeset_line(path.layer, (byte)(SR_GPORT | SR_GSET),
									path.wx[0], path.wy[0], path.wx[1], path.wy[1], SR_MOR);
								return SRSUCCESS;
							}
							if (ro_mazeget_path(wavept.port, layer, SREEND, SREPORT, sx, sy, ex, ey) == null)
								return SRERROR;
							return SRSUCCESS;
						} else if (status == SRSUCCESS)
						{
							dy = -1;
							break;
						}
					}
				}
				if (layer.dir == SRALL || layer.dir == SRHORIPREF)
				{
					// try left
					if ((ex = sx - 1) >= 0)
					{
						ey = sy;
						int status = ro_mazetest_pt(layer.grids[ex][ey], code);
						if (status == SRROUTED)
						{
							// check for common original path
							if (path != null && path.layer == layer &&
								path.y[0] == path.y[1] && path.y[0] == ey &&
									Math.min(path.x[0], path.x[1]) == sx)
							{
								if (path.x[0] == sx)
								{
									path.x[0] = ex;
									path.wx[0] = WORLDX(ex, layer);
								} else
								{
									path.x[1] = ex;
									path.wx[1] = WORLDX(ex, layer);
								}
								ro_mazeset_line(path.layer, (byte)(SR_GPORT | SR_GSET),
									path.wx[0], path.wy[0], path.wx[1], path.wy[1], SR_MOR);
								return SRSUCCESS;
							}
							if (ro_mazeget_path(wavept.port, layer, SREEND, SREPORT, sx, sy, ex, ey) == null)
								return SRERROR;
							return SRSUCCESS;
						} else if (status == SRSUCCESS)
						{
							dx = -1;
							break;
						}
					}
				}
				if (layer.dir == SRALL || layer.dir == SRVERTPREF)
				{
					// try up
					if ((ey = sy + 1) != layer.hei)
					{
						ex = sx;
						int status = ro_mazetest_pt(layer.grids[ex][ey], code);
						if (status == SRROUTED)
						{
							// check for common original path
							if (path != null && path.layer == layer &&
								path.x[0] == path.x[1] && path.x[0] == ex &&
									Math.max(path.y[0], path.y[1]) == sy)
							{
								if (path.y[0] == sy)
								{
									path.y[0] = ey;
									path.wy[0] = WORLDY(ey, layer);
								} else
								{
									path.y[1] = ey;
									path.wy[1] = WORLDY(ey, layer);
								}
								ro_mazeset_line(path.layer, (byte)(SR_GPORT | SR_GSET),
									path.wx[0], path.wy[0], path.wx[1], path.wy[1], SR_MOR);
								return SRSUCCESS;
							}
							if (ro_mazeget_path(wavept.port, layer, SREEND, SREPORT, sx, sy, ex, ey) == null)
								return SRERROR;
							return SRSUCCESS;
						} else if (status == SRSUCCESS)
						{
							dy = 1;
							break;
						}
					}
				}

				// now try jumping layers
				if (pstart == 0)
				{
					if (layer.up != null)
					{
						ex = sx; ey = sy;
						int status = ro_mazetest_pt(layer.up.grids[ex][ey], code);
						if (status == SRROUTED) return SRSUCCESS;
						if (status == SRSUCCESS)
						{
							layer = layer.up;
							if (code == SR_GSTART) code = SR_GMAX;
								else code--;
							continue;
						}
					}
					if (layer.down != null)
					{
						ex = sx; ey = sy;
						int status = ro_mazetest_pt(layer.down.grids[ex][ey], code);
						if (status == SRROUTED) return SRSUCCESS;
						if (status == SRSUCCESS)
						{
							layer = layer.down;
							if (code == SR_GSTART) code = SR_GMAX;
								else code--;
							continue;
						}
					}
				}

				// could not start route, just return
				return SRERROR;
			}

			// set path started
			pstart = 1;

			// now continue scan until the end of the path
			for(;;)
			{
				if (code == SR_GSTART) code = SR_GMAX;
					else code--;
				if (dx != 0)
				{
					// horizontal scan
					int nx = ex + dx;
					int ny = ey;
					int status = ro_mazetest_pt(layer.grids[nx][ny], code);
					if (status == SRROUTED)
					{
						// check for common original path
						if (path != null && path.layer == layer &&
							path.y[0] == path.y[1] && path.y[0] == sy &&
							((dx < 0 && Math.min(path.x[0], path.x[1]) == sx) ||
							(dx > 0 && Math.max(path.x[0], path.x[1]) == sx)))
						{
							if (path.x[0] == sx)
							{
								path.x[0] = nx;
								path.wx[0] = WORLDX(nx, layer);
							} else
							{
								path.x[1] = nx;
								path.wx[1] = WORLDX(nx, layer);
							}
							ro_mazeset_line(path.layer, (byte)(SR_GPORT | SR_GSET),
								path.wx[0], path.wy[0], path.wx[1], path.wy[1], SR_MOR);
							return SRSUCCESS;
						}
						if (ro_mazeget_path(wavept.port, layer, SREEND, SREPORT, sx, sy, nx, ny) == null)
							return SRERROR;
						return SRSUCCESS;
					} else if (status == SRSUCCESS)
					{
						ex = nx;
						continue;
					}
				}
				if (dy != 0)
				{
					// veritical scan
					int nx = ex;
					int ny = ey + dy;
					int status = ro_mazetest_pt(layer.grids[nx][ny], code);
					if (status == SRROUTED)
					{
						// check for common original path
						if (path != null && path.layer == layer &&
							path.x[0] == path.x[1] && path.x[0] == sx &&
							((dy < 0 && Math.min(path.y[0], path.y[1]) == sy) ||
							(dy > 0 && Math.max(path.y[0], path.y[1]) == sy)))
						{
							if (path.y[0] == sy)
							{
								path.y[0] = ny;
								path.wy[0] = WORLDY(ny, layer);
							} else
							{
								path.y[1] = ny;
								path.wy[1] = WORLDY(ny, layer);
							}
							ro_mazeset_line(path.layer, (byte)(SR_GPORT | SR_GSET),
								path.wx[0], path.wy[0], path.wx[1], path.wy[1], SR_MOR);
							return SRSUCCESS;
						}
						if (ro_mazeget_path(wavept.port, layer, SREEND, SREPORT, sx, sy, nx, ny) == null)
							return SRERROR;
						return SRSUCCESS;
					} else if (status == SRSUCCESS)
					{
						ey = ny;
						continue;
					}
				}

				// end of the path, add and break loop

				// check for common original path
				if (path != null && path.layer == layer &&
					// horizontal path check
					((sy == ey && path.y[0] == path.y[1] && path.y[0] == ey &&
					(path.x[0] == sx || path.x[1] == sx)) ||
					// vertical path check
					(sx == ex && path.x[0] == path.x[1] && path.x[0] == ex &&
					(path.y[0] == sy || path.y[1] == sy))))
				{
					// vertical path ?
					if (sx == ex)
					{
						if (sy == path.y[0])
						{
							path.y[0] = ey;
							path.wy[0] = WORLDY(ey, layer);
						} else
						{
							path.y[1] = ey;
							path.wy[1] = WORLDY(ey, layer);
						}
					}

					// horizontal path ?
					else
					{
						if (sx == path.x[0])
						{
							path.x[0] = ex;
							path.wx[0] = WORLDX(ex, layer);
						} else
						{
							path.x[1] = ex;
							path.wx[1] = WORLDX(ex, layer);
						}
					}

					ro_mazeset_line(path.layer, (byte)(SR_GPORT | SR_GSET),
						path.wx[0], path.wy[0], path.wx[1], path.wy[1], SR_MOR);
					path = null;
				} else if (ro_mazeget_path(wavept.port, layer, SREEND, SREEND, sx, sy, ex, ey) == null)
					return SRERROR;
				sx = ex;
				sy = ey;
				break;
			}
		}
	}

	private int ro_mazetest_pt(byte pt, int code)
	{
		// don't check other wavefront points
		if ((pt & SR_GWAVE) == 0)
		{
			// check for permanent grid object (blockage or port)
			if ((pt & SR_GSET) != 0)
			{
				// check for port
				if ((pt & SR_GPORT) != 0 && (pt & SR_GMASK) == code) return SRROUTED;
			} else
			{
				// not permanent, check for matching code
				if ((pt & SR_GMASK) == code) return SRSUCCESS;
			}
		}
		return SRUNROUTED;
	}

	private SRPATH ro_mazeget_path(SRPORT port, SRLAYER layer, SREND e1, SREND e2, int x1, int y1, int x2, int y2)
	{
		SRPATH path = new SRPATH();

		path.x[0] = x1; path.y[0] = y1;
		path.x[1] = x2; path.y[1] = y2;
		path.end[0] = e1; path.end[1] = e2;
		path.layer = layer;
		path.port = port;
		path.type = SRPROUTED;

		path.wx[0] = WORLDX(x1, layer); path.wy[0] = WORLDY(y1, layer);
		path.wx[1] = WORLDX(x2, layer); path.wy[1] = WORLDY(y2, layer);

		// insert the path at the end of the list
		path.next = null;
		if (port.lastpath == null) port.paths = path; else
			port.lastpath.next = path;
		port.lastpath = path;

		// now draw it
		ro_mazeset_line(path.layer, (byte)(SR_GPORT | SR_GSET),
			path.wx[0], path.wy[0], path.wx[1], path.wy[1], SR_MOR);

		return path;
	}

	private int ro_mazeexamine_pt(SRPORT port, SRLAYER layer, int x, int y, byte code)
	{
		// point is set
		if ((layer.grids[x][y] & SR_GWAVE) != 0)
		{
			// look for common point in this wavefront
			if (ro_mazesearch_wavefront(port, layer, x, y) == null)
			{
				return SRROUTED;
			}
		} else if (layer.grids[x][y] == 0)
		{
			// point is not set
			ro_mazeadd_wavept(port, layer, x, y, code);
			return SRSUCCESS;
		}
		return SRBLOCKED;
	}

	private SRWAVEPT ro_mazesearch_wavefront(SRPORT port, SRLAYER layer, int x, int y)
	{
		// scans port's wavefront for common point
		for (SRWAVEPT wavept = port.wavefront; wavept != null; wavept = wavept.next)
		{
			if (wavept.layer == layer && wavept.x == x && wavept.y == y) return wavept;
		}
		return null;
	}

	private void ro_mazeclear_maze(SRNET net)
	{
		// clear each region, and reset bounds
		for (int index = 0; index < SRMAXLAYERS; index++)
		{
			SRLAYER layer = net.region.layers[index];
			if (layer != null)
			{
				byte mask = ~(SR_GMASK | SR_GWAVE);
				for (int x = layer.lx; x <= layer.hx; x++)
				{
					for (int y = layer.ly; y <= layer.hy; y++)
					{
						layer.grids[x][y] = (byte)(layer.grids[x][y] & mask);
					}
				}
				layer.lx = layer.wid; layer.ly = layer.hei;
				layer.hx = -1; layer.hy = -1;
			}
		}
		for (SRPORT port = net.ports; port != null; port = port.next)
		{
			port.wavefront = null;
		}
		return;
	}

	/************************************* CODE TO CREATE THE MAZE BUFFER *************************************/

	private SRREGION ro_mazedefine_region(Cell cell, int lx, int ly, int hx, int hy)
	{
		// determine routing region bounds
		lx = lx - ro_mazegridx - ro_mazeboundary;
		hx = hx + ro_mazegridx + ro_mazeboundary;
		ly = ly - ro_mazegridy - ro_mazeboundary;
		hy = hy + ro_mazegridy + ro_mazeboundary;

		// allocate region and layers
		SRREGION region = ro_mazeget_region(lx, ly, hx, hy);
		if (region == null)
		{
			System.out.println("Could not allocate routing region (" + lx + "<=X<=" + hx + " " + ly + "<=Y<=" + hy + ")");
			return null;
		}

		// search region for nodes/arcs to add to database
		Rectangle2D searchBounds = new Rectangle2D.Double(lx, ly, hx-lx, hy-ly);
		for(Geometric.Search sea = new Geometric.Search(searchBounds, cell); sea.hasNext(); )
		{
			Geometric geom = (Geometric)sea.next();
			if (geom instanceof NodeInst)
			{
				// draw this cell
				ro_mazedraw_cell((NodeInst)geom, GenMath.MATID, region);
			} else
			{
				// draw this arc
				ro_mazedraw_arcinst((ArcInst)geom, GenMath.MATID, region);
			}
		}
		return region;
	}

	/**
	 * Method to output a specific symbol cell
	 */
	private void ro_mazedraw_cell(NodeInst ni, AffineTransform prevTrans, SRREGION region)
	{
		if (ni.getProto() == ro_mazesteiner && ni.getNumConnections() > 0)
		{
//			if (ni->firstportarcinst->conarcinst->network->temp1 != 0) return;
		}

		// make transformation matrix within the current nodeinst
		if (ni.getAngle() == 0 && !ni.isMirroredAboutXAxis() && !ni.isMirroredAboutYAxis())
		{
			ro_mazedraw_nodeinst(ni, prevTrans, region);
		} else
		{
			AffineTransform localTran = ni.rotateOut(prevTrans);
			ro_mazedraw_nodeinst(ni, localTran, region);
		}
	}

	/**
	 * Method to symbol "ni" when transformed through "prevtrans".
	 */
	private void ro_mazedraw_nodeinst(NodeInst ni, AffineTransform prevTrans, SRREGION region)
	{
		// don't draw invisible pins
		NodeProto np = ni.getProto();
		if (np == Generic.tech.invisiblePinNode) return;

		AffineTransform rotateNode = ni.rotateOut(prevTrans);
		if (np instanceof PrimitiveNode)
		{
			// primitive nodeinst: ask the technology how to draw it
			Technology tech = np.getTechnology();
			Poly [] polys = tech.getShapeOfNode(ni);
			int high = polys.length;
			for (int j = 0; j < high; j++)
			{
				// get description of this layer
				Poly poly = polys[j];

				// draw the nodeinst
				poly.transform(rotateNode);

				// draw the nodeinst and restore the color
				ro_mazedraw_showpoly(poly, region, SCH_ALLLAYER);
			}
		} else
		{
			// draw cell rectangle
			Poly poly = new Poly(ni.getTrueCenterX(), ni.getTrueCenterY(), ni.getXSize(), ni.getYSize());
			AffineTransform localPureTrans = ni.rotateOutAboutTrueCenter(prevTrans);
			poly.transform(localPureTrans);
			poly.setStyle(Poly.Type.CLOSED);
			ro_mazedraw_showpoly(poly, region, SCH_ALLLAYER);

			// transform into the cell for display of its guts
			AffineTransform subRot = ni.translateOut(rotateNode);

			// search through cell
			Cell subCell = (Cell)np;
			for(Iterator it = subCell.getNodes(); it.hasNext(); )
			{
				NodeInst iNo = (NodeInst)it.next();
				ro_mazedraw_cell(iNo, subRot, region);
			}
			for(Iterator it = subCell.getArcs(); it.hasNext(); )
			{
				ArcInst iAr = (ArcInst)it.next();
				ro_mazedraw_arcinst(iAr, subRot, region);
			}
		}
	}

	/**
	 * Method to draw an arcinst.  Returns indicator of what else needs to
	 * be drawn.  Returns negative if display interrupted
	 */
	private void ro_mazedraw_arcinst(ArcInst ai, AffineTransform trans, SRREGION region)
	{
		// if selected net or group route and temp1 is set
//		if (ai->network != null && ai->network->temp1 == 1) return;

		// get the polygons of the arcinst, force line for path generation?
		Technology tech = ai.getProto().getTechnology();
//		double width = ai->width;
//		ai->width = 0;
		Poly [] polys = tech.getShapeOfArc(ai);
//		ai->width = width;
		int total = polys.length;
		for (int j = 0; j < total; j++)
		{
			// generate a polygon
			Poly poly = polys[j];

			// transform the polygon
			poly.transform(trans);

			// draw the polygon
			Point2D [] points = poly.getPoints();
			if (points[0].getX() == points[1].getX())
			{
				ro_mazedraw_showpoly(poly, region, SCH_VERTLAYER);
			} else
			{
				if (points[0].getY() == points[1].getY())
					ro_mazedraw_showpoly(poly, region, SCH_HORILAYER); else
						ro_mazedraw_showpoly(poly, region, SCH_ALLLAYER);
			}
		}
	}

	private SRPORT ro_mazeadd_port(SRNET net, byte layers, int wx1, int wy1, int wx2, int wy2, PortInst pi)
	{
		SRPORT port = new SRPORT();

		if (wx1 < wx2) { port.lx = wx1; port.hx = wx2; }
			else { port.lx = wx2; port.hx = wx1; }
		if (wy1 < wy2) { port.ly = wy1; port.hy = wy2; }
			else { port.ly = wy2; port.hy = wy1; }

		port.layers = layers;
		port.wavefront = null;

		for (int index = 0, mask = 1; index < SRMAXLAYERS; index++, mask = mask<<1)
		{
			if (layers != 0 & mask != 0 && net.region.layers[index] != null)
			{
				ro_mazeset_box(net.region.layers[index], (byte)(SR_GPORT | SR_GSET),
					port.lx, port.ly, port.hx, port.hy, SR_MOR);
			}
		}

		// link into net
		port.next = null;
		port.master = null;
		port.paths = null;
		port.lastpath = null;
		port.net = net;
		port.ni = pi.getNodeInst();
		port.pp = pi.getPortProto();
		SRPORT lport = net.ports;
		int index = 0;
		if (lport == null)
		{
			net.ports = port;
		} else
		{
			index = 1;
			while (lport.next != null) { index++; lport = lport.next; }
			lport.next = port;
		}
		port.index = index;

		return port;
	}

	private byte ro_mazedetermine_dir(NodeInst ni, double cx, double cy)
	{
		if (ni == null) return 3;

		// get the center of the NODEINST
		double ncx = ni.getTrueCenterX();
		double ncy = ni.getTrueCenterY();

		// center, all edges
		if (ncx == cx && ncy == cy) return 3; // all layers
		double dx = ni.getBounds().getMaxX() - ncx;
		double dy = ni.getBounds().getMaxY() - ncy;
		double pdx = Math.abs(cx - ncx);
		double pdy = Math.abs(cy - ncy);

		/* consider a point on a triangle, if left/right the seq center, port,
		 * upper left/right edge will be counter-clockwise, if top/bottom the seq.
		 * will be clock-wise :
		 * x1 * y2 + x2 * y3 + x3 * y1 - y1 * x2 - y2 * x3 - y3 * x1 == abs(area)
		 * where area < 0 == clockwise, area > 0 == counter-clockwise
		 */
		double area = pdx * dy - pdy * dx;

		if (area > 0.0) return 1;		// horizontal
		if (area < 0.0) return 2;		// vertical
		return 3;						// corner, all layers
	}

	/**
	 * routing definition functions
	 */
	private SRNET ro_mazeadd_net(SRREGION region)
	{
		SRNET net = new SRNET();

		net.routed = false;
		net.ports = null;
		net.paths = null;
		net.lastpath = null;
		net.region = region;

		// link into region list
		net.next = region.nets;
		region.nets = net;

		return net;
	}

	/**
	 * Method to write polys into the maze buffer.
	 */
	private void ro_mazedraw_showpoly(Poly obj, SRREGION region, int layer)
	{
		// now draw the polygon
		Point2D [] points = obj.getPoints();
		if (obj.getStyle() == Poly.Type.CIRCLE || obj.getStyle() == Poly.Type.THICKCIRCLE || obj.getStyle() == Poly.Type.DISC)
		{
			double radius = points[0].distance(points[1]);
			Rectangle2D circleBounds = new Rectangle2D.Double(points[0].getX()-radius, points[0].getY()-radius, radius*2, radius*2);
			DRAW_BOX(circleBounds, layer, region);
			return;
		}
		if (obj.getStyle() == Poly.Type.CIRCLEARC || obj.getStyle() == Poly.Type.THICKCIRCLEARC)
		{
			// arcs at [i] points [1+i] [2+i] clockwise
			if (points.length == 0) return;
			if ((points.length % 3) != 0) return;
			for (int i = 0; i < points.length; i += 3)
			{
				Point2D si = GenMath.computeArcCenter(points[i], points[i+1], points[i+2]);
				DRAW_LINE(points[i+1], si, layer, region);
				DRAW_LINE(si, points[i+2], layer, region);
			}
			return;
		}

		if (obj.getStyle() == Poly.Type.FILLED || obj.getStyle() == Poly.Type.CLOSED)
		{
			Rectangle2D objBounds = obj.getBox();
			if (objBounds != null)
			{
				DRAW_BOX(objBounds, layer, region);
				return;
			}
			for (int i = 1; i < points.length; i++)
				DRAW_LINE(points[i-1], points[i], layer, region);

			// close the region
			if (points.length > 2)
				DRAW_LINE(points[points.length-1], points[0], layer, region);
			return;
		}

		if (obj.getStyle() == Poly.Type.OPENED || obj.getStyle() == Poly.Type.OPENEDT1 ||
			obj.getStyle() == Poly.Type.OPENEDT1 || obj.getStyle() == Poly.Type.OPENEDT3)
		{
			Rectangle2D objBounds = obj.getBox();
			if (objBounds != null)
			{
				DRAW_BOX(objBounds, layer, region);
				return;
			}

			for (int i = 0; i < points.length; i++)
				DRAW_LINE(points[i], points[i+1], layer, region);
			return;
		}

		if (obj.getStyle() == Poly.Type.VECTORS)
		{
			for (int i = 0; i < points.length; i += 2)
				DRAW_LINE(points[i], points[i+1], layer, region);
			return;
		}
	}

	private void DRAW_BOX(Rectangle2D box, int layer, SRREGION region)
	{
		int Mlx = (int)box.getMinX();
		int Mhx = (int)box.getMaxX();
		int Mly = (int)box.getMinY();
		int Mhy = (int)box.getMaxY();
		if (layer == SCH_HORILAYER || layer == SCH_ALLLAYER)
			ro_mazeset_box(region.layers[SCH_HORILAYER], SR_GSET, Mlx, Mly, Mhx, Mhy, SR_MSET);
		if (layer == SCH_VERTLAYER || layer == SCH_ALLLAYER)
			ro_mazeset_box(region.layers[SCH_VERTLAYER], SR_GSET, Mlx, Mly, Mhx, Mhy, SR_MSET);
	}

	private void DRAW_LINE(Point2D from, Point2D to, int layer, SRREGION region)
	{
		int wX1 = (int)from.getX();
		int wY1 = (int)from.getY();
		int wX2 = (int)to.getX();
		int wY2 = (int)to.getY();
		if (layer == SCH_HORILAYER || layer == SCH_ALLLAYER)
			ro_mazeset_line(region.layers[SCH_HORILAYER], SR_GSET, wX1, wY1, wX2, wY2, SR_MSET);
		if (layer == SCH_VERTLAYER || layer == SCH_ALLLAYER)
			ro_mazeset_line(region.layers[SCH_VERTLAYER], SR_GSET, wX1, wY1, wX2, wY2, SR_MSET);
	}

	private void ro_mazeset_line(SRLAYER layer, byte type, int wx1, int wy1,
		int wx2, int wy2, SRMODE mode)
	{
		// convert to grid coordinates
		int [] x = new int[2];
		int [] y = new int[2];
		x[0] = GRIDX(wx1, layer);   x[1] = GRIDX(wx2, layer);
		y[0] = GRIDY(wy1, layer);   y[1] = GRIDY(wy2, layer);
		int lx = 1, hx = 0;
		if (wx1 < wx2) { lx = 0; hx = 1; }
		int ly = 1, hy = 0;
		if (wy1 < wy2) { ly = 0; hy = 1; }

		if (x[hx] < 0 || x[lx] >= layer.wid || y[hy] < 0 || y[ly] >= layer.hei) return;

		int dx = x[hx] - x[lx];
		int dy = y[hy] - y[ly];

		// clip x
		int diff = -x[lx];
		if (diff > 0)
		{
			y[lx] += (y[hx] - y[lx]) * diff / dx;
			x[lx] = 0;
		}
		diff = x[hx] - (layer.wid - 1);
		if (diff > 0)
		{
			y[hx] -= (y[hx] - y[lx]) * diff / dx;
			x[hx] = layer.wid - 1;
		}

		// now clip y
		diff = -y[ly];
		if (diff > 0)
		{
			x[ly] += (x[hy] - x[ly]) * diff / dy;
			y[ly] = 0;
		}
		diff = y[hy] - (layer.hei - 1);
		if (diff > 0)
		{
			x[hy] -= (x[hy] - x[ly]) * diff / dy;
			y[hy] = layer.hei - 1;
		}

		// after clip ...
		dx = x[hx] - x[lx];
		dy = y[hy] - y[ly];

		// use Bresenham's algorithm to set intersecting grid points
		if (dy < dx)
		{
			// for 0 <= dy <= dx
			int e = (dy<<1) - dx;
			int yi = y[lx];
			if (y[hx] - y[lx] < 0) diff = -1;
				else diff = 1;
			for (int xi = x[lx]; xi <= x[hx]; xi++)
			{
				ro_mazeset_point(layer, type, xi, yi, mode);
				if (e > 0)
				{
					yi += diff;
					e = e + (dy<<1) - (dx<<1);
				} else e = e + (dy<<1);
			}
		} else
		{
			// for 0 <= dx < dy
			int e = (dx<<1) - dy;
			int xi = x[ly];
			if (x[hy] - x[ly] < 0) diff = -1;
				else diff = 1;
			for (int yi = y[ly]; yi <= y[hy]; yi++)
			{
				ro_mazeset_point(layer, type, xi, yi, mode);
				if (e > 0)
				{
					xi += diff;
					e = e + (dx<<1) - (dy<<1);
				} else e = e + (dx<<1);
			}
		}
	}

	private void ro_mazeset_box(SRLAYER layer, byte type, int wx1, int wy1, int wx2, int wy2, SRMODE mode)
	{
		int lx = GRIDX(wx1, layer);   int ly = GRIDY(wy1, layer);
		int hx = GRIDX(wx2, layer);   int hy = GRIDY(wy2, layer);
		if (lx > hx) { int x = lx; lx = hx; hx = x; }
		if (ly > hy) { int y = ly; ly = hy; hy = y; }

		if (hx < 0 || lx >= layer.wid || hy < 0 || ly >= layer.hei) return;

		// clip (simple orthogonal)
		if (lx < 0) lx = 0;
		if (hx >= layer.wid) hx = layer.wid - 1;
		if (ly < 0) ly = 0;
		if (hy >= layer.hei) hy = layer.hei - 1;

		// now fill the box
		for (int x = lx; x <= hx; x++)
			for (int y = ly; y <= hy; y++)
				ro_mazeset_point(layer, type, x, y, mode);
	}

	/**
	 * drawing function
	 */
	void ro_mazeset_point(SRLAYER layer, byte type, int x, int y, SRMODE mode)
	{
		if (mode == SR_MOR)
		{
			layer.grids[x][y] |= type;
			layer.vused[x] |= type;
			layer.hused[y] |= type;
			return;
		}
		if (mode == SR_MAND)
		{
			layer.grids[x][y] &= type;
			layer.vused[x] &= type;
			layer.hused[y] &= type;
			return;
		}
		if (mode == SR_MSET)
		{
			layer.grids[x][y] = type;
			layer.vused[x] = type;
			layer.hused[y] = type;
			return;
		}
	}

	/* general control commands */
	private SRREGION ro_mazeget_region(int wlx, int wly, int whx, int why)
	{
		if (wlx > whx || wly > why) return null;

		if (ro_theregion == null)
		{
			ro_theregion = new SRREGION();
			for (int index = 0; index < SRMAXLAYERS; index++)
				ro_theregion.layers[index] = null;
			ro_theregion.nets = null;
		} else
		{
			ro_mazecleanout_region(ro_theregion);
		}

		// now set bounds
		ro_theregion.lx = wlx;
		ro_theregion.hx = whx;
		ro_theregion.ly = wly;
		ro_theregion.hy = why;

		SRLAYER hlayer = ro_mazeadd_layer(ro_theregion, SCH_HORILAYER, SRHORIPREF,
			ro_mazegridx, ro_mazegridy, ro_mazeoffsetx, ro_mazeoffsety);
		if (hlayer == null)
		{
			System.out.println("Could not allocate horizontal layer");
			return null;
		}
		SRLAYER vlayer = ro_mazeadd_layer(ro_theregion, SCH_VERTLAYER, SRVERTPREF,
			ro_mazegridx, ro_mazegridy, ro_mazeoffsetx, ro_mazeoffsety);
		if (vlayer == null)
		{
			System.out.println("Could not allocate vertical layer");
			return null;
		}

		return ro_theregion;
	}

	private void ro_mazecleanout_region(SRREGION region)
	{
		region.nets = null;
	}

	private SRLAYER ro_mazeadd_layer(SRREGION region, int index, SRDIRECTION direction,
		int gridx, int gridy, int alignx, int aligny)
	{
		// check for common index
		SRLAYER layer = region.layers[index];
		if (layer == null)
		{
			// allocate and initialize the layer
			layer = new SRLAYER();
			region.layers[index] = layer;
			layer.grids = null;
			layer.vused = null;
			layer.hused = null;
		}

		layer.index = index;
		layer.mask = 1<<index;
		layer.dir = direction;

		// determine the range in grid units
		int tx = alignx % gridx;
		if (tx < 0) tx += gridx;
		int ty = aligny % gridy;
		if (ty < 0) ty += gridy;

		// determine the actual bounds of the world
		// round low bounds up, high bounds down
		int lx = ((region.lx - tx + gridx - 1) / gridx) * gridx + tx;
		int ly = ((region.ly - ty + gridy - 1) / gridy) * gridy + ty;
		int hx = ((region.hx - tx) / gridx) * gridx + tx;
		int hy = ((region.hy - ty) / gridy) * gridy + ty;

		// translate the region lx, ly into grid units
		layer.wid = (hx - lx) / gridx + 1;
		layer.hei = (hy - ly) / gridy + 1;
		layer.transx = lx; // + tx;
		layer.transy = ly; // + ty;
		layer.gridx = gridx;
		layer.gridy = gridy;

		// sensibility check
		if (layer.wid > MAXGRIDSIZE || layer.hei > MAXGRIDSIZE)
		{
			System.out.println("This route is too large to solve (limit is " + MAXGRIDSIZE + "x" + MAXGRIDSIZE +
				" grid, this is " + layer.wid + "x" + layer.hei + ")");
			return null;
		}

		// check that the hx, hy of grid is in bounds of region
		if (WORLDX(layer.wid - 1, layer) > region.hx) layer.wid--;
		if (WORLDY(layer.hei - 1, layer) > region.hy) layer.hei--;

		// now allocate a grid array
		layer.grids = new byte[layer.wid][];
		layer.vused = new byte[layer.wid];
		for (int x = 0; x < layer.wid; x++)
		{
			// get address for a column of grid points
			layer.grids[x] = new byte[layer.hei];

			// clear all points
			for (int y = 0; y < layer.hei; y++)
				layer.grids[x][y] = 0;

			// clear the V-used flags
			layer.vused[x] = 0;
		}

		// clear the H-used flags
		layer.hused = new byte[layer.hei];
		for (int y = 0; y < layer.hei; y++) layer.hused[y] = 0;

		// set up/down pointers
		layer.up = layer.down = null;
		if (index != 0)
		{
			SRLAYER alayer = region.layers[index-1];
			if (alayer != null)
			{
				layer.down = alayer;
				alayer.up = layer;
			}
		}
		if (index < SRMAXLAYERS - 1)
		{
			SRLAYER alayer = region.layers[index+1];
			if (alayer != null)
			{
				layer.up = alayer;
				alayer.down = layer;
			}
		}

		return layer;
	}

	private int GRIDX(int Mv, SRLAYER Ml) { return (((Mv) - Ml.transx) / Ml.gridx); }

	private int GRIDY(int Mv, SRLAYER Ml) { return (((Mv) - Ml.transy) / Ml.gridy); }

	private int WORLDX(int Mv, SRLAYER Ml) { return (((Mv) * Ml.gridx) + Ml.transx); }

	private int WORLDY(int Mv, SRLAYER Ml) { return (((Mv) * Ml.gridy) + Ml.transy); }

	/************************************* CODE TO CREATE RESULTS IN THE CIRCUIT *************************************/

	/**
	 * routing grid database methods
	 */
	private boolean ro_mazeextract_paths(Cell parent, SRNET net)
	{
		// adjust paths to account for precise port location
		int fx = 0, fy = 0;
		for (SRPATH path = net.paths; path != null; path = path.next)
		{
			if (path.type == SRPFIXED) continue;
			SRPORT port = path.port;
			fx = path.wx[0];   fy = path.wy[0];
			int ofx = fx, ofy = fy;
			if (path.end[0] == SREPORT && port.ni != null)
			{
				if (fx < port.lx) fx = port.lx;
				if (fx > port.hx) fx = port.hx;
				if (fy < port.ly) fy = port.ly;
				if (fy > port.hy) fy = port.hy;
				if (fx != ofx || fy != ofy)
					ro_mazeadjustpath(net.paths, path, 0, fx-ofx, fy-ofy);
			} else
			{
				ArcProto ap = path.layer.index != 0 ? ro_mazevertwire : ro_mazehoriwire;
				List portInstList = ro_mazefindport(parent, fx, fy, ap, true);
				if (portInstList.size() > 0)
				{
					PortInst pi = (PortInst)portInstList.get(0);
					Poly portPoly = pi.getPoly();
					Point2D closest = portPoly.closestPoint(new Point2D.Double(fx, fy));
					if (closest.getX() != ofx || closest.getY() != ofy)
						ro_mazeadjustpath(net.paths, path, 0, (int)(closest.getX()-ofx), (int)(closest.getY()-ofy));
				}
			}

			fx = ofx = path.wx[1];   fy = ofy = path.wy[1];
			if (path.end[1] == SREPORT && port.ni != null)
			{
				if (fx < port.lx) fx = port.lx;
				if (fx > port.hx) fx = port.hx;
				if (fy < port.ly) fy = port.ly;
				if (fy > port.hy) fy = port.hy;
				if (fx != ofx || fy != ofy)
					ro_mazeadjustpath(net.paths, path, 1, fx-ofx, fy-ofy);
			} else
			{
				ArcProto ap = path.layer.index != 0 ? ro_mazevertwire : ro_mazehoriwire;
				List portInstList = ro_mazefindport(parent, fx, fy, ap, true);
				if (portInstList.size() > 0)
				{
					PortInst pi = (PortInst)portInstList.get(0);
					Poly portPoly = pi.getPoly();
					Point2D closest = portPoly.closestPoint(new Point2D.Double(fx, fy));
					if (closest.getX() != ofx || closest.getY() != ofy)
						ro_mazeadjustpath(net.paths, path, 1, (int)(closest.getX()-ofx), (int)(closest.getY()-ofy));
				}
			}
		}

		for (SRPATH path = net.paths; path != null; path = path.next)
		{
			if (path.type == SRPFIXED) continue;
			ArcProto ap = path.layer.index != 0 ? ro_mazevertwire : ro_mazehoriwire;

			// create arc between the end points
			fx = path.wx[0];   fy = path.wy[0];
			List fromPortInstList = ro_mazefindport(parent, fx, fy, ap, false);
			if (fromPortInstList.size() == 0)
			{
				// create the from pin
				double xs = ro_mazesteiner.getDefWidth();
				double ys = ro_mazesteiner.getDefHeight();
				NodeInst ni = NodeInst.makeInstance(ro_mazesteiner, new Point2D.Double(fx, fy), xs, ys, parent);
				if (ni == null)
				{
					System.out.println("Could not create pin");
					return true;
				}
				fromPortInstList.add(ni.getPortInst(0));
			}

			int tx = path.wx[1];   int ty = path.wy[1];
			List toPortInstList = ro_mazefindport(parent, tx, ty, ap, false);
			if (toPortInstList.size() == 0)
			{
				// create the from pin
				double xs = ro_mazesteiner.getDefWidth();
				double ys = ro_mazesteiner.getDefHeight();
				NodeInst ni = NodeInst.makeInstance(ro_mazesteiner, new Point2D.Double(tx, ty), xs, ys, parent);
				if (ni == null)
				{
					System.out.println("Could not create pin");
					return true;
				}
				toPortInstList.add(ni.getPortInst(0));
			}

			// now connect (note only nodes for now, no bus like connections)
			if (fromPortInstList.size() > 0 && toPortInstList.size() > 0)
			{
				// now make the connection (simple wire to wire for now)
				PortInst fPi = (PortInst)fromPortInstList.get(0);
				PortInst tPi = (PortInst)toPortInstList.get(0);
				ArcInst ai = ArcInst.makeInstance(ap, ap.getDefaultWidth(), fPi, tPi);
				if (ai == null)
				{
					System.out.println("Could not create path (arc)");
					return true;
				}
//				if (ai->network != null) ai->network->temp1 = 2;
			}
		}

		return false;
	}

	/**
	 * Method to recursively adjust paths to account for port positions that may not
	 * be on the grid.
	 */
	private void ro_mazeadjustpath(SRPATH paths, SRPATH thispath, int end, int dx, int dy)
	{
		if (dx != 0)
		{
			int formerthis = thispath.wx[end];
			int formerother = thispath.wx[1-end];
			thispath.wx[end] += dx;
			if (formerthis == formerother)
			{
				int formerx = thispath.wx[1-end];
				int formery = thispath.wy[1-end];
				thispath.wx[1-end] += dx;
				for (SRPATH opath = paths; opath != null; opath = opath.next)
				{
					if (opath.wx[0] == formerx && opath.wy[0] == formery)
					{
						ro_mazeadjustpath(paths, opath, 0, dx, 0);
						break;
					}
					if (opath.wx[1] == formerx && opath.wy[1] == formery)
					{
						ro_mazeadjustpath(paths, opath, 1, dx, 0);
						break;
					}
				}
			}
		}

		if (dy != 0)
		{
			int formerthis = thispath.wy[end];
			int formerother = thispath.wy[1-end];
			thispath.wy[end] += dy;
			if (formerthis == formerother)
			{
				int formerx = thispath.wx[1-end];
				int formery = thispath.wy[1-end];
				thispath.wy[1-end] += dy;
				for (SRPATH opath = paths; opath != null; opath = opath.next)
				{
					if (opath.wx[0] == formerx && opath.wy[0] == formery)
					{
						ro_mazeadjustpath(paths, opath, 0, 0, dy);
						break;
					}
					if (opath.wx[1] == formerx && opath.wy[1] == formery)
					{
						ro_mazeadjustpath(paths, opath, 1, 0, dy);
						break;
					}
				}
			}
		}
	}

	private List ro_mazefindport(Cell cell, int x, int y, ArcProto ap, boolean forcefind)
	{
		List portInstList = new ArrayList();
		ArcInst ai = ro_mazefindport_geom(cell, x, y, ap, portInstList, forcefind);
		if (portInstList.size() == 0 && ai != null)
		{
			// direct hit on an arc, verify connection
			ArcProto nap = ai.getProto();
			NodeProto np = ((PrimitiveArc)nap).findPinProto();
			if (np == null) return null;
			PortProto pp = np.getPort(0);
			ArcProto [] connectList = pp.getBasePort().getConnections();
			boolean found = false;
			for (int j = 0; j < connectList.length; j++)
				if (connectList[j] == ap) { found = true;   break; }
			if (!found) return null;

			// try to split arc (from us_getnodeonarcinst)*/
			// break is at (prefx, prefy): save information about the arcinst
			PortInst fpi = ai.getHead().getPortInst();
			PortInst tpi = ai.getTail().getPortInst();
			Point2D fPt = ai.getHead().getLocation();
			Point2D tPt = ai.getTail().getLocation();
			double wid = ai.getWidth();  Cell pnt = ai.getParent();

			// create the splitting pin
			double xs = np.getDefWidth();
			double ys = np.getDefHeight();
			Point2D loc = new Point2D.Double(x, y);
			NodeInst ni = NodeInst.makeInstance(np, loc, xs, ys, pnt);
			if (ni == null)
			{
				System.out.println("Cannot create splitting pin");
				return null;
			}

			// set the node, and port
			PortInst pi = ni.findPortInstFromProto(pp);
			portInstList.add(pi);

			// create the two new arcinsts
			ArcInst ar1 = ArcInst.makeInstance(nap, wid, fpi, pi, fPt, loc, ai.getName());
			ArcInst ar2 = ArcInst.makeInstance(nap, wid, pi, tpi, loc, tPt, ai.getName());
			if (ar1 == null || ar2 == null)
			{
				System.out.println("Error creating the split arc parts");
				return portInstList;
			}
			ar1.copyPropertiesFrom(ai);

			if (GenMath.figureAngle(fPt, loc) != GenMath.figureAngle(loc, tPt))
			{
				ar1.setFixedAngle(false);
				ar2.setFixedAngle(false);
			}

			// delete the old arcinst
			ai.kill();
		}

		return portInstList;
	}

	/**
	 * Method to locate the nodeinstance and portproto corresponding to
	 * to a direct intersection with the given point.
	 * inputs:
	 * cell - cell to search
	 * x, y  - the point to exam
	 * ap    - the arc used to connect port (must match pp)
	 * nis   - pointer to ni pointer buffer.
	 * pps   - pointer to portproto pointer buffer.
	 * outputs:
	 * returns cnt if found, 0 not found, -1 on error
	 * ni = found ni instance
	 * pp = found pp proto.
	 */
	private ArcInst ro_mazefindport_geom(Cell cell, int x, int y, ArcProto ap,
		List portInstList, boolean forcefind)
	{
		ArcInst foundAi = null;
		Point2D searchPoint = new Point2D.Double(x, y);
		double bestdist = 0;
		PortInst closestpi = null;
		Rectangle2D searchBounds = new Rectangle2D.Double(x, y, 0, 0);
		for(Geometric.Search sea = new Geometric.Search(searchBounds, cell); sea.hasNext(); )
		{
			Geometric geom = (Geometric)sea.next();

			if (geom instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)geom;
//				if (ni.getProto() instanceof PrimitiveNode &&
//					(ni.getFunction() == PrimitiveNode.Function.PIN &&
//					ni.getNumConnections() > 0 &&
//					ni->firstportarcinst->conarcinst->network != null &&
//					ni->firstportarcinst->conarcinst->network->temp1 < 2) break;

				// now locate a portproto
				for(Iterator it = ni.getPortInsts(); it.hasNext(); )
				{
					PortInst pi = (PortInst)it.next();
					Poly portPoly = pi.getPoly();
					if (portPoly.isInside(searchPoint))
					{
						// check if port connects to arc ...*/
						if (pi.getPortProto().getBasePort().connectsTo(ap))
						{
							portInstList.add(pi);
						}
					} else
					{
						double dist = portPoly.polyDistance(new Rectangle2D.Double(x, y, 0, 0));

						// LINTED "bestdist" used in proper order
						if (closestpi == null || dist < bestdist)
						{
							bestdist = dist;
							closestpi = pi;
						}
					}
				}
			} else
			{
				ArcInst ai = (ArcInst)geom;
//				if (ai->network != null && ai->network->temp1 != 2) break;
				foundAi = ai;
			}
		}

		if (portInstList.size() == 0 && forcefind && closestpi != null && bestdist < ro_mazegridx)
		{
			portInstList.add(closestpi);
		}
		return foundAi;
	}

}
