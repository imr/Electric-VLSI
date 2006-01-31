/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Maze.java
 * Routing tool: Maze routing
 * Original C Code written by Glen M. Lawson
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
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.User;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class to do maze routing (single wire at a time).
 */
public class Maze
{
	/** bit width of long word */		private static final int SRMAXLAYERS = 64;
	/** maximum size of maze */			private static final int MAXGRIDSIZE = 1000;
	/** max grid points to "excavate" for initial grid access from a port */	private static final int BLOCKAGELIMIT =  10;

	/** draw only on vertical layer */	private static final int HORILAYER = 0;
	/** draw all layers */				private static final int VERTLAYER = 1;
	/** draw all layers */				private static final int ALLLAYERS = 2;

	// common bit masks
	/** grid set (permanent) */			private static final byte SR_GSET   = (byte)0x80;
	/** is a wavefront poin */			private static final byte SR_GWAVE  = 0x20;
	/** is a port */					private static final byte SR_GPORT  = 0x10;

	// for maze cells
	/** mask 4 bits */					private static final byte SR_GMASK  = 0x0F;
	/** maximum mark value */			private static final byte SR_GMAX   = 15;
	/** start value */					private static final byte SR_GSTART = 1;

	// miscellaneous defines, return codes from expandWavefront
	private static final int SRSUCCESS  = 0;
	private static final int SRERROR    = 1;
	private static final int SRROUTED   = 2;
	private static final int SRBLOCKED  = 3;
	private static final int SRUNROUTED = 4;

	/** the arc used for vertical wires */			private ArcProto mazeVertWire;
	/** the arc used for horizontal wires */		private ArcProto mazeHorizWire;
	/** the pin used to join arcs */				private NodeProto mazeSteinerNode;
	/** the routing region with all data */			private SRREGION theRegion = null;
	/** The netlist for the cell being routed */	private Netlist netList;

	/** Space around net to build search grid */	private int mazeBoundary = 20;

	static class SRDIRECTION {}
	SRDIRECTION SRVERTPREF = new SRDIRECTION();
	SRDIRECTION SRHORIPREF = new SRDIRECTION();
	SRDIRECTION SRALL = new SRDIRECTION();

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
		/** the Network object */			Network  eNet;
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
		/** true center of the port */			double    cX, cY;
		/** bounds of the port */				int       lx, ly, hx, hy;
		/** where this port comes from */		PortInst  pi;
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
		/** end pt in world units */		double []   wx, wy;
		/** the layer of the path */		SRLAYER  layer;
		/** end style of the path */		boolean [] end;
		/** the type of path */				SRPTYPE  type;
		/** the port path is attached to */	SRPORT   port;
		/** next in the list of paths */	SRPATH   next;

		SRPATH()
		{
			x = new int[2];
			y = new int[2];
			wx = new double[2];
			wy = new double[2];
			end = new boolean[2];
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
		UserInterface ui = Job.getUserInterface();
		Cell cell = ui.needCurrentCell();
		if (cell == null) return;
		EditWindow_ wnd = ui.getCurrentEditWindow_();
		if (wnd == null) return;

		Netlist netList = cell.acquireUserNetlist();
		if (netList == null)
		{
			System.out.println("Sorry, a deadlock aborted routing (network information unavailable).  Please try again");
			return;
		}
		Set<Network> nets = wnd.getHighlightedNetworks();
		if (nets.size() == 0)
		{
			for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = it.next();
				if (ai.getProto() != Generic.tech.unrouted_arc) continue;
				Network net = netList.getNetwork(ai, 0);
				nets.add(net);
			}
		}

		// turn off highlighting
		wnd.clearHighlighting();
		wnd.finishedHighlighting();

		MazeRouteJob job = new MazeRouteJob(cell, nets);
	}

	private static class MazeRouteJob extends Job
	{
		private Cell cell;
		private Set<Network> nets;

		protected MazeRouteJob(Cell cell, Set<Network> nets)
		{
			super("Maze Route", Routing.getRoutingTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.nets = nets;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			Maze router = new Maze();
			router.routeSelected(cell, nets);
			return true;
		}
	}

	/**
	 * This is the public interface for Maze Routing when done in batch mode.
	 * It replaces the selected unrouted arcs with routed geometry
	 * @param cell the cell to be Maze-routed.
	 */
	public void routeSelected(Cell cell, Set<Network> nets)
	{
		// turn this into a list of ArcInsts on each net so that it survives renetlisting after each route
		List<ArcInst> arcsToRoute = new ArrayList<ArcInst>();
		for(Network net : nets)
		{
			for(Iterator<ArcInst> aIt = net.getArcs(); aIt.hasNext(); )
			{
				ArcInst ai = aIt.next();
				arcsToRoute.add(ai);
				break;
			}
		}

		// now route each arc
		for(ArcInst ai : arcsToRoute)
		{
			// reacquire the netlist for the current configuration
			netList = cell.acquireUserNetlist();

			// route the unrouted arc
			Network net = netList.getNetwork(ai, 0);
			if (routeNet(net)) continue;
		}
	}

	/**
	 * Method to reroute networks "net".  Returns true on error.
	 */
	private boolean routeNet(Network net)
	{
		// get extent of net and mark nodes and arcs on it
		HashSet<ArcInst> arcsToDelete = new HashSet<ArcInst>();
		HashSet<NodeInst> nodesToDelete = new HashSet<NodeInst>();
		List<Connection> netEnds = Routing.findNetEnds(net, arcsToDelete, nodesToDelete, netList);
		int count = netEnds.size();
		if (count == 0) return false;
		if (count != 2)
		{
			System.out.println("Can only route nets with 2 ends, this has " + count);
			return true;
		}

		// determine bounds of this networks
		Cell cell = net.getParent();
		Rectangle2D routingBounds = null;
		for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = it.next();
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

		// now create the routing region
		int lx = (int)routingBounds.getMinX();
		int hx = (int)routingBounds.getMaxX();
		int ly = (int)routingBounds.getMinY();
		int hy = (int)routingBounds.getMaxY();
		SRREGION region = defineRegion(cell, lx, ly, hx, hy, arcsToDelete, nodesToDelete);
		if (region == null) return true;

		// create the net in the region
		SRNET srnet = addNet(region, net);
		if (srnet == null)
		{
			System.out.println("Could not allocate internal net");
			return true;
		}

		// add the ports to the net
		for(int i=0; i<count; i++)
		{
			Connection con = (Connection)netEnds.get(i);
			PortInst pi = con.getPortInst();
			double cXD = con.getLocation().getX();
			double cYD = con.getLocation().getY();
			SRPORT fsp = addPort(srnet, determineDir(pi.getNodeInst(), cXD, cYD), cXD, cYD, pi);
			if (fsp == null)
			{
				System.out.println("Port could not be defined");
				return true;
			}
		}
//		dumpLayer("BEFORE ROUTING", region, (byte)0xFF);

		// do maze routing
		if (routeANet(srnet))
		{
			System.out.println("Could not route net");
			return true;
		}

		// extract paths to create arcs
		if (extractPaths(cell, srnet))
		{
			System.out.println("Could not create paths");
			return true;
		}

		// remove marked networks
		for(ArcInst ai : arcsToDelete)
		{
			ai.kill();
		}
		for(NodeInst ni : nodesToDelete)
		{
			ni.kill();
		}

		return false;
	}

	/************************************* CODE TO TRAVERSE THE MAZE BUFFER *************************************/

	private boolean routeANet(SRNET net)
	{
		// presume routing with the current arc
		boolean ret = false;
		ArcProto routingArc = User.getUserTool().getCurrentArcProto();
		if (routingArc == Generic.tech.unrouted_arc) routingArc = null;
		if (routingArc != null)
		{
			// see if the default arc can be used to route
			for(SRPORT port = net.ports; port != null; port = port.next)
			{
				PortProto pp = port.pi.getPortProto();
				boolean found = pp.getBasePort().connectsTo(routingArc);
				if (!found) { routingArc = null;   break; }
			}
		}

		// if current arc cannot run, look for any that can
		if (routingArc == null)
		{
			// check out all arcs for use in this route
			HashSet<ArcProto> arcsUsed = new HashSet<ArcProto>();
			for(SRPORT port = net.ports; port != null; port = port.next)
			{
				PortProto pp = port.pi.getPortProto();
				ArcProto [] connections = pp.getBasePort().getConnections();
				for(int i = 0; i < connections.length; i++)
				{
					ArcProto ap = connections[i];
					if (ap.getTechnology() == Generic.tech) continue;
					arcsUsed.add(ap);
				}
			}
			for(Iterator<Technology> it = Technology.getTechnologies(); it.hasNext(); )
			{
				Technology tech = it.next();
				if (tech == Generic.tech) continue;
				for(Iterator<ArcProto> aIt = tech.getArcs(); aIt.hasNext(); )
				{
					ArcProto ap = aIt.next();
					if (!arcsUsed.contains(ap)) continue;
					boolean allFound = true;
					for(SRPORT port = net.ports; port != null; port = port.next)
					{
						PortProto pp = port.pi.getPortProto();
						if (!pp.getBasePort().connectsTo(ap)) { allFound = false;   break; }
					}
					if (allFound)
					{
						routingArc = ap;
						break;
					}
				}
				if (routingArc != null) break;
			}
		}
		if (routingArc == null)
		{
			System.out.println("Cannot find wire to route");
			return true;
		}
		mazeVertWire = routingArc;
		mazeHorizWire = routingArc;
		mazeSteinerNode = routingArc.findPinProto();

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
					if ((layer.vused[x] & SR_GSET) != 0)
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
		int pCount = 0;
		for (SRPORT port = net.ports; port != null; port = port.next, pCount++)
		{
			createWavefront(port);
		}

		// now begin routing until all ports merged
		byte code = SR_GSTART;
		do
		{
			if (++code > SR_GMAX) code = SR_GSTART;
			int blocked = 0;
			int status = 0;
			SRPORT port = null;
			for (port = net.ports; port != null; port = port.next)
			{
				// if part of other wavefront, get the next one
				if (port.master != null) continue;

				// expand the wavefront
				status = expandWavefront(port, code);
				if (status == SRERROR) return true;
				if (status == SRROUTED) break;
				if (status == SRBLOCKED) blocked++;
			}

			// check for successful routing
			if (port != null && status == SRROUTED)
			{
				// now clear routing region and restart expansion
				clearMaze(net);
				if (--pCount > 1)
				{
					// prepare each port for routing
					for (port = net.ports; port != null; port = port.next)
						createWavefront(port);
				}
				code = SR_GSTART;
			} else
			{
				// check for blocked net
				if (blocked == pCount)
				{
					ret = true;
					clearMaze(net);
					break;
				}
			}
		} while (pCount > 1);

		// move all the port paths to the net
		for (SRPORT port = net.ports; port != null; port = port.next)
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

	private void createWavefront(SRPORT port)
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
				int lx = getGridX(port.lx, layer);   int ly = getGridY(port.ly, layer);
				int hx = getGridX(port.hx, layer);   int hy = getGridY(port.hy, layer);
				if (lx >= layer.wid || hx < 0 || ly >= layer.hei || hy < 0) continue;

				// clip to window
				if (lx < 0) lx = 0;
				if (hx >= layer.wid) hx = layer.wid - 1;
				if (ly < 0) ly = 0;
				if (hy >= layer.hei) hy = layer.hei - 1;

				/*
				 * added detection of immediate blockage ... smr
				 */
				boolean onEdge = false;
				for (int x = lx; x <= hx; x++)
				{
					for (int y = ly; y <= hy; y++)
					{
						addWavePoint(master, layer, x, y, SR_GSTART);
						if (x < layer.wid-1 && layer.grids[x+1][y] == 0) onEdge = true;
						if (x > 0 && layer.grids[x-1][y] == 0) onEdge = true;
						if (y < layer.hei-1 && layer.grids[x][y+1] == 0) onEdge = true;
						if (y > 0 && layer.grids[x][y-1] == 0) onEdge = true;
					}
				}
				if (!onEdge)
				{
					// port is inside of blocked area: search for opening
					int cx = (lx + hx) / 2;
					int cy = (ly + hy) / 2;
					PrimitivePort prP = port.pi.getPortProto().getBasePort();
					int angRange = prP.getAngleRange();
					int ang = prP.getAngle();
					NodeInst ni = port.pi.getNodeInst();
					ang += (ni.getAngle()+5) / 10;
					if (ni.isMirroredAboutXAxis() != ni.isMirroredAboutYAxis()) { ang = 270 - ang; if (ang < 0) ang += 360; }
					if (angleDiff(ang, 0) <= angRange)
					{
						// port faces right
						for(int spread=1; spread<BLOCKAGELIMIT; spread++)
						{
							if (hx+spread >= layer.wid) break;
							if (layer.grids[hx+spread][cy] == 0) { onEdge = true;   break; }
							layer.grids[hx+spread][cy] = 0;
						}
					}
					if (angleDiff(ang, 90) <= angRange)
					{
						// port faces up
						for(int spread=1; spread<BLOCKAGELIMIT; spread++)
						{
							if (hy+spread >= layer.hei) break;
							if (layer.grids[cx][hy+spread] == 0) { onEdge = true;   break; }
							layer.grids[cx][hy+spread] = 0;
						}
					}
					if (angleDiff(ang, 180) <= angRange)
					{
						// port faces left
						for(int spread=1; spread<BLOCKAGELIMIT; spread++)
						{
							if (lx-spread < 0) break;
							if (layer.grids[lx-spread][cy] == 0) { onEdge = true;   break; }
							layer.grids[lx-spread][cy] = 0;
						}
					}
					if (angleDiff(ang, 270) <= angRange)
					{
						// port faces down
						for(int spread=1; spread<BLOCKAGELIMIT; spread++)
						{
							if (ly-spread < 0) break;
							if (layer.grids[cx][ly-spread] == 0) { onEdge = true;   break; }
							layer.grids[cx][ly-spread] = 0;
						}
					}
					if (!onEdge)
					{
						System.out.println("Node %s is blocked" + ni);
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
					addWavePoint(master, path.layer, x, y, SR_GSTART);
				}
			} else if (path.y[0] == path.y[1])
			{
				// horizontal path
				int dx = -1;
				if (path.x[0] < path.x[1]) dx = 1;
				for (int y = path.y[0], x = path.x[0];
					(dx < 0) ? x >= path.x[1] : x <= path.x[1]; x += dx)
				{
					addWavePoint(master, path.layer, x, y, SR_GSTART);
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
					addWavePoint(master, path.layer, x, y, SR_GSTART);
				}
			}
		}
	}

	/**
	 * routing commands
	 */
	private void addWavePoint(SRPORT port, SRLAYER layer, int x, int y, byte code)
	{
		SRWAVEPT wavePt = new SRWAVEPT();
		wavePt.x = x;
		wavePt.y = y;

		// set the grid
		layer.grids[x][y] = (byte)((layer.grids[x][y] & ~SR_GMASK) | code | SR_GWAVE);
		wavePt.layer = layer;

		// set maze bounds
		if (layer.lx > x) layer.lx = x;
		if (layer.hx < x) layer.hx = x;
		if (layer.ly > y) layer.ly = y;
		if (layer.hy < y) layer.hy = y;

		wavePt.prev = null;
		if (port.master != null)
		{
			wavePt.port = port.master;
			wavePt.next = port.master.wavefront;
			port.master.wavefront = wavePt;
		} else
		{
			wavePt.port = port;
			wavePt.next = port.wavefront;
			port.wavefront = wavePt;
		}
		if (wavePt.next != null)
			wavePt.next.prev = wavePt;
	}

	private int angleDiff(int ang1, int ang2)
	{
		int diff = Math.abs(ang1 - ang2);
		if (diff > 180) diff = 360 - diff;
		return diff;
	}

	private int expandWavefront(SRPORT port, byte code)
	{
		// begin expansion of all wavepts
		// disconnect wavepts from the port
		SRWAVEPT wavePt = port.wavefront;
		if (wavePt == null) return SRBLOCKED;

		SRWAVEPT next = null;
		int status = SRSUCCESS;
		boolean found = false;
		int bx = 0, by = 0;
		SRLAYER bLayer = null;
		SRWAVEPT bWavePt = new SRWAVEPT();
		for (wavePt = port.wavefront; wavePt != null ; wavePt = next)
		{
			boolean connected = false;
			SRLAYER layer = wavePt.layer;
			if (layer.dir == SRALL || layer.dir == SRHORIPREF)
			{
				// try horizontal route
				int x = wavePt.x + 1;
				if (x != layer.wid)
				{
					status = examinePoint(port, layer, x, wavePt.y, code);
					if (status == SRROUTED)
					{
						// "bWavePt" used in proper order
						if (!found || (layer.hused[bWavePt.y] & SR_GSET) != 0 ||
							((layer.hused[wavePt.y] & SR_GSET) == 0 &&
							layer.hused[wavePt.y] < layer.hused[bWavePt.y]))
						{
							bWavePt.x = wavePt.x;   bWavePt.y = wavePt.y;   
							bWavePt.layer = wavePt.layer;   bWavePt.port = wavePt.port;
							bx = x;  by = wavePt.y;
							bLayer = layer;
						}
						found = true;
						connected = true;
					}
				}
				if (!connected && (x = wavePt.x - 1) >= 0)
				{
					status = examinePoint(port, layer, x, wavePt.y, code);
					if (status == SRROUTED)
					{
						if (!found ||
							(layer.hused[bWavePt.y] & SR_GSET) != 0  ||
							((layer.hused[wavePt.y] & SR_GSET) == 0 &&
							layer.hused[wavePt.y] < layer.hused[bWavePt.y]))
						{
							bWavePt.x = wavePt.x;   bWavePt.y = wavePt.y;   
							bWavePt.layer = wavePt.layer;   bWavePt.port = wavePt.port;
							bx = x; by = wavePt.y;
							bLayer = layer;
						}
						found = true;
						connected = true;
					}
				}
			}
			if (layer.dir == SRALL || layer.dir == SRVERTPREF)
			{
				// try vertical route
				int y = wavePt.y + 1;
				if (!connected && y != layer.hei)
				{
					status = examinePoint(port, layer, wavePt.x, y, code);
					if (status == SRROUTED)
					{
						if (!found ||
							(layer.vused[bWavePt.x] & SR_GSET) != 0  ||
							((layer.vused[wavePt.x] & SR_GSET) == 0 &&
							layer.vused[wavePt.x] < layer.vused[bWavePt.x]))
						{
							bWavePt.x = wavePt.x;   bWavePt.y = wavePt.y;   
							bWavePt.layer = wavePt.layer;   bWavePt.port = wavePt.port;
							bx = wavePt.x; by = y;
							bLayer = layer;
						}
						found = true;
						connected = true;
					}
				}
				if (!connected && (y = wavePt.y - 1) >= 0)
				{
					status = examinePoint(port, layer, wavePt.x, y, code);
					if (status == SRROUTED)
					{
						if (!found ||
							(layer.vused[bWavePt.x] & SR_GSET) != 0  ||
							((layer.vused[wavePt.x] & SR_GSET) == 0 &&
							layer.vused[wavePt.x] < layer.vused[bWavePt.x]))
						{
							bWavePt.x = wavePt.x;   bWavePt.y = wavePt.y;   
							bWavePt.layer = wavePt.layer;   bWavePt.port = wavePt.port;
							bx = wavePt.x; by = y;
							bLayer = layer;
						}
						found = true;
						connected = true;
					}
				}
			}
			if (!connected && layer.up != null)
			{
				// try via up
				status = examinePoint(port, layer.up, wavePt.x, wavePt.y, code);
				if (status == SRROUTED)
				{
					if (!found)
					{
						bWavePt.x = wavePt.x;   bWavePt.y = wavePt.y;   
						bWavePt.layer = wavePt.layer;   bWavePt.port = wavePt.port;
						bx = wavePt.x; by = wavePt.y;
						bLayer = layer.up;
					}
					found = true;
					connected = true;
				}
			}
			if (!connected && layer.down != null)
			{
				// try via down
				status = examinePoint(port, layer.down, wavePt.x, wavePt.y, code);
				if (status == SRROUTED)
				{
					if (!found)
					{
						bWavePt.x = wavePt.x;   bWavePt.y = wavePt.y;   
						bWavePt.layer = wavePt.layer;   bWavePt.port = wavePt.port;
						bx = wavePt.x; by = wavePt.y;
						bLayer = layer.down;
					}
					found = true;
					connected = true;
				}
			}
			next = wavePt.next;

			// now release this wavept
			if (wavePt.prev == null)
			{
				port.wavefront = wavePt.next;
				if (wavePt.next != null)
					wavePt.next.prev = null;
			} else
			{
				wavePt.prev.next = wavePt.next;
				if (wavePt.next != null)
					wavePt.next.prev = wavePt.prev;
			}

			// set the grid point to a core point
			if (!connected) layer.grids[wavePt.x][wavePt.y] &= ~SR_GWAVE;
		}

		if (found)
			return initPath(port, bLayer, bWavePt, bx, by);

		if (port.wavefront == null) return SRBLOCKED;
		return SRSUCCESS;
	}

	private int initPath(SRPORT port, SRLAYER layer, SRWAVEPT wavePt, int x, int y)
	{
		// search for others
		SRPORT target = null;
		SRWAVEPT tWavePt = null;
		for (target = port.net.ports; target != null; target = target.next)
		{
			if (target == port || target.master != null) continue;
			tWavePt = searchWavefront(target, layer, x, y);
			if (tWavePt != null) break;
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
		if (wavePt.layer == tWavePt.layer)
		{
			path = getPath(port, layer, false, false, wavePt.x, wavePt.y, tWavePt.x, tWavePt.y);
		}

		// now create paths to each target point
		if (findPaths(wavePt, path) != SRSUCCESS) return SRERROR;
		if (findPaths(tWavePt, path) != SRSUCCESS) return SRERROR;

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
	private int findPaths(SRWAVEPT wavePt, SRPATH path)
	{
		// Start scan from the first point
		int sx = wavePt.x;
		int sy = wavePt.y;

		SRLAYER layer = wavePt.layer;
		byte code = (byte)(layer.grids[sx][sy] & SR_GMASK);
		if (code == SR_GSTART) code = SR_GMAX;
			else code--;
		int pStart = 0;
		int ex = 0, ey = 0;
		for(;;)
		{
			int dx = 0, dy = 0;

			// scan around the point
			for(;;)
			{
				if (pStart == 1)
				{
					// always try to jump layer after the first path
					// now try jumping layers
					if (layer.up != null)
					{
						ex = sx; ey = sy;
						int status = testPoint(layer.up.grids[ex][ey], code);
						if (status == SRROUTED) return SRSUCCESS;
						if (status == SRSUCCESS)
						{
							layer = layer.up;
							if (code == SR_GSTART) code = SR_GMAX;
								else code--;
							pStart = 2;
							continue;
						}
					}
					if (layer.down != null)
					{
						ex = sx; ey = sy;
						int status = testPoint(layer.down.grids[ex][ey], code);
						if (status == SRROUTED) return SRSUCCESS;
						if (status == SRSUCCESS)
						{
							layer = layer.down;
							if (code == SR_GSTART) code = SR_GMAX;
								else code--;
							pStart = 2;
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
						int status = testPoint(layer.grids[ex][ey], code);
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
									path.wx[0] = getWorldX(ex, layer);
								} else
								{
									path.x[1] = ex;
									path.wx[1] = getWorldX(ex, layer);
								}
								setLine(path.layer, (byte)(SR_GPORT | SR_GSET),
									path.wx[0], path.wy[0], path.wx[1], path.wy[1], true);
								return SRSUCCESS;
							}
							getPath(wavePt.port, layer, false, true, sx, sy, ex, ey);
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
						int status = testPoint(layer.grids[ex][ey], code);
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
									path.wy[0] = getWorldY(ey, layer);
								} else
								{
									path.y[1] = ey;
									path.wy[1] = getWorldY(ey, layer);
								}
								setLine(path.layer, (byte)(SR_GPORT | SR_GSET),
									path.wx[0], path.wy[0], path.wx[1], path.wy[1], true);
								return SRSUCCESS;
							}
							getPath(wavePt.port, layer, false, true, sx, sy, ex, ey);
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
						int status = testPoint(layer.grids[ex][ey], code);
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
									path.wx[0] = getWorldX(ex, layer);
								} else
								{
									path.x[1] = ex;
									path.wx[1] = getWorldX(ex, layer);
								}
								setLine(path.layer, (byte)(SR_GPORT | SR_GSET),
									path.wx[0], path.wy[0], path.wx[1], path.wy[1], true);
								return SRSUCCESS;
							}
							getPath(wavePt.port, layer, false, true, sx, sy, ex, ey);
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
						int status = testPoint(layer.grids[ex][ey], code);
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
									path.wy[0] = getWorldY(ey, layer);
								} else
								{
									path.y[1] = ey;
									path.wy[1] = getWorldY(ey, layer);
								}
								setLine(path.layer, (byte)(SR_GPORT | SR_GSET),
									path.wx[0], path.wy[0], path.wx[1], path.wy[1], true);
								return SRSUCCESS;
							}
							getPath(wavePt.port, layer, false, true, sx, sy, ex, ey);
							return SRSUCCESS;
						} else if (status == SRSUCCESS)
						{
							dy = 1;
							break;
						}
					}
				}

				// now try jumping layers
				if (pStart == 0)
				{
					if (layer.up != null)
					{
						ex = sx; ey = sy;
						int status = testPoint(layer.up.grids[ex][ey], code);
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
						int status = testPoint(layer.down.grids[ex][ey], code);
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
			pStart = 1;

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
					int status = testPoint(layer.grids[nx][ny], code);
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
								path.wx[0] = getWorldX(nx, layer);
							} else
							{
								path.x[1] = nx;
								path.wx[1] = getWorldX(nx, layer);
							}
							setLine(path.layer, (byte)(SR_GPORT | SR_GSET),
								path.wx[0], path.wy[0], path.wx[1], path.wy[1], true);
							return SRSUCCESS;
						}
						getPath(wavePt.port, layer, false, true, sx, sy, nx, ny);
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
					int status = testPoint(layer.grids[nx][ny], code);
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
								path.wy[0] = getWorldY(ny, layer);
							} else
							{
								path.y[1] = ny;
								path.wy[1] = getWorldY(ny, layer);
							}
							setLine(path.layer, (byte)(SR_GPORT | SR_GSET),
								path.wx[0], path.wy[0], path.wx[1], path.wy[1], true);
							return SRSUCCESS;
						}
						getPath(wavePt.port, layer, false, true, sx, sy, nx, ny);
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
							path.wy[0] = getWorldY(ey, layer);
						} else
						{
							path.y[1] = ey;
							path.wy[1] = getWorldY(ey, layer);
						}
					}

					// horizontal path ?
					else
					{
						if (sx == path.x[0])
						{
							path.x[0] = ex;
							path.wx[0] = getWorldX(ex, layer);
						} else
						{
							path.x[1] = ex;
							path.wx[1] = getWorldX(ex, layer);
						}
					}

					setLine(path.layer, (byte)(SR_GPORT | SR_GSET),
						path.wx[0], path.wy[0], path.wx[1], path.wy[1], true);
					path = null;
				} else
				{
					getPath(wavePt.port, layer, false, false, sx, sy, ex, ey);
				}
				sx = ex;
				sy = ey;
				break;
			}
		}
	}

	private int testPoint(byte pt, int code)
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

	private SRPATH getPath(SRPORT port, SRLAYER layer, boolean e1, boolean e2, int x1, int y1, int x2, int y2)
	{
		SRPATH path = new SRPATH();

		path.x[0] = x1; path.y[0] = y1;
		path.x[1] = x2; path.y[1] = y2;
		path.end[0] = e1; path.end[1] = e2;
		path.layer = layer;
		path.port = port;
		path.type = SRPROUTED;

		path.wx[0] = getWorldX(x1, layer); path.wy[0] = getWorldY(y1, layer);
		path.wx[1] = getWorldX(x2, layer); path.wy[1] = getWorldY(y2, layer);

		// insert the path at the end of the list
		path.next = null;
		if (port.lastpath == null) port.paths = path; else
			port.lastpath.next = path;
		port.lastpath = path;

		// now draw it
		setLine(path.layer, (byte)(SR_GPORT | SR_GSET),
			path.wx[0], path.wy[0], path.wx[1], path.wy[1], true);

		return path;
	}

	private int examinePoint(SRPORT port, SRLAYER layer, int x, int y, byte code)
	{
		// point is set
		if ((layer.grids[x][y] & SR_GWAVE) != 0)
		{
			// look for common point in this wavefront
			if (searchWavefront(port, layer, x, y) == null)
			{
				return SRROUTED;
			}
		} else if (layer.grids[x][y] == 0)
		{
			// point is not set
			addWavePoint(port, layer, x, y, code);
			return SRSUCCESS;
		}
		return SRBLOCKED;
	}

	private SRWAVEPT searchWavefront(SRPORT port, SRLAYER layer, int x, int y)
	{
		// scans port's wavefront for common point
		for (SRWAVEPT wavept = port.wavefront; wavept != null; wavept = wavept.next)
		{
			if (wavept.layer == layer && wavept.x == x && wavept.y == y) return wavept;
		}
		return null;
	}

	private void clearMaze(SRNET net)
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

	private SRREGION defineRegion(Cell cell, int lX, int lY, int hX, int hY, Set arcsToDelete, Set nodesToDelete)
	{
		// determine routing region bounds
		lX = lX - mazeBoundary;
		hX = hX + mazeBoundary;
		lY = lY - mazeBoundary;
		hY = hY + mazeBoundary;

		// allocate region and layers
		SRREGION region = getRegion(lX, lY, hX, hY);
		if (region == null)
		{
			System.out.println("Could not allocate routing region (" + lX + "<=X<=" + hX + " " + lY + "<=Y<=" + hY + ")");
			return null;
		}

		// search region for nodes/arcs to add to database
		Rectangle2D searchBounds = new Rectangle2D.Double(lX, lY, hX-lX, hY-lY);
		for(Iterator<Geometric> sea = cell.searchIterator(searchBounds); sea.hasNext(); )
		{
			Geometric geom = sea.next();
			if (geom instanceof NodeInst)
			{
				// draw this cell
				if (nodesToDelete.contains(geom)) continue;
				drawCell((NodeInst)geom, GenMath.MATID, region);
			} else
			{
				// draw this arc
				if (arcsToDelete.contains(geom)) continue;
				drawArcInst((ArcInst)geom, GenMath.MATID, region);
			}
		}
		return region;
	}

	/**
	 * Method to output a specific symbol cell
	 */
	private void drawCell(NodeInst ni, AffineTransform prevTrans, SRREGION region)
	{
		// make transformation matrix within the current nodeinst
		if (ni.getOrient().equals(Orientation.IDENT))
//		if (ni.getAngle() == 0 && !ni.isMirroredAboutXAxis() && !ni.isMirroredAboutYAxis())
		{
			drawNodeInst(ni, prevTrans, region);
		} else
		{
			AffineTransform localTran = ni.rotateOut(prevTrans);
			drawNodeInst(ni, localTran, region);
		}
	}

	/**
	 * Method to symbol "ni" when transformed through "prevtrans".
	 */
	private void drawNodeInst(NodeInst ni, AffineTransform prevTrans, SRREGION region)
	{
		// don't draw invisible pins
		NodeProto np = ni.getProto();
		if (np == Generic.tech.invisiblePinNode) return;

		AffineTransform rotateNode = ni.rotateOut(prevTrans);
		if (!ni.isCellInstance())
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
				drawPoly(poly, region, ALLLAYERS);
			}
		} else
		{
			// draw cell rectangle
			Poly poly = new Poly(ni.getTrueCenterX(), ni.getTrueCenterY(), ni.getXSize(), ni.getYSize());
			AffineTransform localPureTrans = ni.rotateOutAboutTrueCenter(prevTrans);
			poly.transform(localPureTrans);
			poly.setStyle(Poly.Type.CLOSED);
			drawPoly(poly, region, ALLLAYERS);

			// transform into the cell for display of its guts
			AffineTransform subRot = ni.translateOut(rotateNode);

			// search through cell
			Cell subCell = (Cell)np;
			for(Iterator<NodeInst> it = subCell.getNodes(); it.hasNext(); )
			{
				NodeInst iNo = it.next();
				drawCell(iNo, subRot, region);
			}
			for(Iterator<ArcInst> it = subCell.getArcs(); it.hasNext(); )
			{
				ArcInst iAr = it.next();
				drawArcInst(iAr, subRot, region);
			}
		}
	}

	/**
	 * Method to draw an arcinst.  Returns indicator of what else needs to
	 * be drawn.  Returns negative if display interrupted
	 */
	private void drawArcInst(ArcInst ai, AffineTransform trans, SRREGION region)
	{
		// get the polygons of the arcinst, force line for path generation?
		Technology tech = ai.getProto().getTechnology();
		Poly [] polys = tech.getShapeOfArc(ai);
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
				drawPoly(poly, region, VERTLAYER);
			} else
			{
				if (points[0].getY() == points[1].getY())
					drawPoly(poly, region, HORILAYER); else
						drawPoly(poly, region, ALLLAYERS);
			}
		}
	}

	private SRPORT addPort(SRNET net, byte layers, double cX, double cY, PortInst pi)
	{
		SRPORT port = new SRPORT();
		port.cX = cX;
		port.cY = cY;
		port.lx = (int)cX;   port.hx = (int)cX;
		port.ly = (int)cY;   port.hy = (int)cY;
		port.layers = layers;
		port.wavefront = null;

		for (int index = 0, mask = 1; index < SRMAXLAYERS; index++, mask = mask<<1)
		{
			if (layers != 0 & mask != 0 && net.region.layers[index] != null)
			{
				setBox(net.region.layers[index], (byte)(SR_GPORT | SR_GSET),
					port.lx, port.ly, port.hx, port.hy, true);
			}
		}

		// link into net
		port.next = null;
		port.master = null;
		port.paths = null;
		port.lastpath = null;
		port.net = net;
		port.pi = pi;
		SRPORT lPort = net.ports;
		int index = 0;
		if (lPort == null)
		{
			net.ports = port;
		} else
		{
			index = 1;
			while (lPort.next != null) { index++;   lPort = lPort.next; }
			lPort.next = port;
		}
		port.index = index;

		return port;
	}

	private byte determineDir(NodeInst ni, double cX, double cY)
	{
		if (ni == null) return 3;

		// get the center of the NODEINST
		double nCX = ni.getTrueCenterX();
		double nCY = ni.getTrueCenterY();

		// center, all edges
		if (nCX == cX && nCY == cY) return 3; // all layers
		double dX = ni.getBounds().getMaxX() - nCX;
		double dY = ni.getBounds().getMaxY() - nCY;
		double pDX = Math.abs(cX - nCX);
		double pDY = Math.abs(cY - nCY);

		/* consider a point on a triangle, if left/right the seq center, port,
		 * upper left/right edge will be counter-clockwise, if top/bottom the seq.
		 * will be clock-wise :
		 * x1 * y2 + x2 * y3 + x3 * y1 - y1 * x2 - y2 * x3 - y3 * x1 == abs(area)
		 * where area < 0 == clockwise, area > 0 == counter-clockwise
		 */
		double area = pDX * dY - pDY * dX;

		if (area > 0.0) return 1;		// horizontal
		if (area < 0.0) return 2;		// vertical
		return 3;						// corner, all layers
	}

	/**
	 * routing definition functions
	 */
	private SRNET addNet(SRREGION region, Network eNet)
	{
		SRNET srNet = new SRNET();

		srNet.routed = false;
		srNet.eNet = eNet;
		srNet.ports = null;
		srNet.paths = null;
		srNet.lastpath = null;
		srNet.region = region;

		// link into region list
		srNet.next = region.nets;
		region.nets = srNet;

		return srNet;
	}

	/**
	 * Method to write polys into the maze buffer.
	 */
	private void drawPoly(Poly obj, SRREGION region, int layer)
	{
		// now draw the polygon
		Point2D [] points = obj.getPoints();
		if (obj.getStyle() == Poly.Type.CIRCLE || obj.getStyle() == Poly.Type.THICKCIRCLE || obj.getStyle() == Poly.Type.DISC)
		{
			double radius = points[0].distance(points[1]);
			Rectangle2D circleBounds = new Rectangle2D.Double(points[0].getX()-radius, points[0].getY()-radius, radius*2, radius*2);
			drawBox(circleBounds, layer, region);
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
				drawLine(points[i+1], si, layer, region);
				drawLine(si, points[i+2], layer, region);
			}
			return;
		}

		if (obj.getStyle() == Poly.Type.FILLED || obj.getStyle() == Poly.Type.CLOSED)
		{
			Rectangle2D objBounds = obj.getBox();
			if (objBounds != null)
			{
				drawBox(objBounds, layer, region);
				return;
			}
			for (int i = 1; i < points.length; i++)
				drawLine(points[i-1], points[i], layer, region);

			// close the region
			if (points.length > 2)
				drawLine(points[points.length-1], points[0], layer, region);
			return;
		}

		if (obj.getStyle() == Poly.Type.OPENED || obj.getStyle() == Poly.Type.OPENEDT1 ||
			obj.getStyle() == Poly.Type.OPENEDT1 || obj.getStyle() == Poly.Type.OPENEDT3)
		{
			Rectangle2D objBounds = obj.getBox();
			if (objBounds != null)
			{
				drawBox(objBounds, layer, region);
				return;
			}

			for (int i = 1; i < points.length; i++)
				drawLine(points[i-1], points[i], layer, region);
			return;
		}

		if (obj.getStyle() == Poly.Type.VECTORS)
		{
			for (int i = 1; i < points.length; i += 2)
				drawLine(points[i-1], points[i], layer, region);
			return;
		}
	}

	private void drawBox(Rectangle2D box, int layer, SRREGION region)
	{
		int lX = (int)box.getMinX();
		int hX = (int)box.getMaxX();
		int lY = (int)box.getMinY();
		int hY = (int)box.getMaxY();
		if (layer == HORILAYER || layer == ALLLAYERS)
			setBox(region.layers[HORILAYER], SR_GSET, lX, lY, hX, hY, false);
		if (layer == VERTLAYER || layer == ALLLAYERS)
			setBox(region.layers[VERTLAYER], SR_GSET, lX, lY, hX, hY, false);
	}

	private void drawLine(Point2D from, Point2D to, int layer, SRREGION region)
	{
		double wX1 = from.getX();
		double wY1 = from.getY();
		double wX2 = to.getX();
		double wY2 = to.getY();
		if (layer == HORILAYER || layer == ALLLAYERS)
			setLine(region.layers[HORILAYER], SR_GSET, wX1, wY1, wX2, wY2, false);
		if (layer == VERTLAYER || layer == ALLLAYERS)
			setLine(region.layers[VERTLAYER], SR_GSET, wX1, wY1, wX2, wY2, false);
	}

	private void setLine(SRLAYER layer, byte type, double wX1, double wY1, double wX2, double wY2, boolean orMode)
	{
		// convert to grid coordinates
		int [] x = new int[2];
		int [] y = new int[2];
		x[0] = getGridX((int)wX1, layer);   x[1] = getGridX((int)wX2, layer);
		y[0] = getGridY((int)wY1, layer);   y[1] = getGridY((int)wY2, layer);
		int lx = 1, hx = 0;
		if (wX1 < wX2) { lx = 0; hx = 1; }
		int ly = 1, hy = 0;
		if (wY1 < wY2) { ly = 0; hy = 1; }

		// do obvious clip if completely outside of an edge
		if (x[hx] < 0 || x[lx] >= layer.wid || y[hy] < 0 || y[ly] >= layer.hei) return;

		// clip x
		if (x[lx] < 0)
		{
			y[lx] -= (y[hx] - y[lx]) * x[lx] / (x[hx] - x[lx]);
			x[lx] = 0;
		}
		if (x[hx] >= layer.wid)
		{
			y[hx] -= (y[hx] - y[lx]) * (x[hx] - (layer.wid-1)) / (x[hx] - x[lx]);
			x[hx] = layer.wid - 1;
		}

		// now clip y
		if (y[ly] < 0)
		{
			x[ly] -= (x[hy] - x[ly]) * y[ly] / (y[hy] - y[ly]);
			y[ly] = 0;
		}
		if (y[hy] >= layer.hei)
		{
			x[hy] -= (x[hy] - x[ly]) * (y[hy] - (layer.hei - 1)) / (y[hy] - y[ly]);
			y[hy] = layer.hei - 1;
		}

		// use Bresenham's algorithm to set intersecting grid points
		int dX = x[hx] - x[lx];
		int dY = y[hy] - y[ly];
		if (dY < dX)
		{
			// for 0 <= dY <= dX
			int e = (dY<<1) - dX;
			int yi = y[lx];
			int diff = 1;
			if (y[hx] < y[lx]) diff = -1;
			for (int xi = x[lx]; xi <= x[hx]; xi++)
			{
				setPoint(layer, type, xi, yi, orMode);
				if (e > 0)
				{
					yi += diff;
					e = e + (dY<<1) - (dX<<1);
				} else e = e + (dY<<1);
			}
		} else
		{
			// for 0 <= dX < dY
			int e = (dX<<1) - dY;
			int xi = x[ly];
			int diff = 1;
			if (x[hy] < x[ly]) diff = -1;
			for (int yi = y[ly]; yi <= y[hy]; yi++)
			{
				setPoint(layer, type, xi, yi, orMode);
				if (e > 0)
				{
					xi += diff;
					e = e + (dX<<1) - (dY<<1);
				} else e = e + (dX<<1);
			}
		}
	}

	private void setBox(SRLAYER layer, byte type, int wX1, int wY1, int wX2, int wY2, boolean orMode)
	{
		int lX = getGridX(wX1, layer);   int lY = getGridY(wY1, layer);
		int hX = getGridX(wX2, layer);   int hY = getGridY(wY2, layer);
		if (lX > hX) { int x = lX; lX = hX; hX = x; }
		if (lY > hY) { int y = lY; lY = hY; hY = y; }

		if (hX < 0 || lX >= layer.wid || hY < 0 || lY >= layer.hei) return;

		// clip (simple orthogonal)
		if (lX < 0) lX = 0;
		if (hX >= layer.wid) hX = layer.wid - 1;
		if (lY < 0) lY = 0;
		if (hY >= layer.hei) hY = layer.hei - 1;

		// now fill the box
		for (int x = lX; x <= hX; x++)
			for (int y = lY; y <= hY; y++)
				setPoint(layer, type, x, y, orMode);
	}

	/**
	 * drawing function
	 */
	private void setPoint(SRLAYER layer, byte type, int x, int y, boolean orMode)
	{
		if (orMode)
		{
			layer.grids[x][y] |= type;
			layer.vused[x] |= type;
			layer.hused[y] |= type;
		} else
		{
			layer.grids[x][y] = type;
			layer.vused[x] = type;
			layer.hused[y] = type;
		}
	}

	/* general control commands */
	private SRREGION getRegion(int wLX, int wLY, int wHX, int wHY)
	{
		if (wLX > wHX || wLY > wHY) return null;

		if (theRegion == null)
		{
			theRegion = new SRREGION();
			for (int index = 0; index < SRMAXLAYERS; index++)
				theRegion.layers[index] = null;
			theRegion.nets = null;
		} else
		{
			cleanoutRegion(theRegion);
		}

		// now set bounds
		theRegion.lx = wLX;
		theRegion.hx = wHX;
		theRegion.ly = wLY;
		theRegion.hy = wHY;

		SRLAYER hlayer = addLayer(theRegion, HORILAYER, SRHORIPREF);
		if (hlayer == null)
		{
			System.out.println("Could not allocate horizontal layer");
			return null;
		}
		SRLAYER vlayer = addLayer(theRegion, VERTLAYER, SRVERTPREF);
		if (vlayer == null)
		{
			System.out.println("Could not allocate vertical layer");
			return null;
		}

		return theRegion;
	}

	private void cleanoutRegion(SRREGION region)
	{
		region.nets = null;
	}

	private SRLAYER addLayer(SRREGION region, int index, SRDIRECTION direction)
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

		// determine the actual bounds of the world
		// round low bounds up, high bounds down
		int lX = region.lx - 1;
		int lY = region.ly - 1;
		int hX = region.hx + 1;
		int hY = region.hy + 1;

		// translate the region lx, ly into grid units
		layer.wid = (hX - lX) + 1;
		layer.hei = (hY - lY) + 1;
		layer.transx = lX;
		layer.transy = lY;

		// sensibility check
		if (layer.wid > MAXGRIDSIZE || layer.hei > MAXGRIDSIZE)
		{
			System.out.println("This route is too large to solve (limit is " + MAXGRIDSIZE + "x" + MAXGRIDSIZE +
				" grid, this is " + layer.wid + "x" + layer.hei + ")");
			return null;
		}

		// check that the hx, hy of grid is in bounds of region
		if (getWorldX(layer.wid - 1, layer) > region.hx) layer.wid--;
		if (getWorldY(layer.hei - 1, layer) > region.hy) layer.hei--;

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

	private int getGridX(int Mv, SRLAYER Ml) { return Mv - Ml.transx; }

	private int getGridY(int Mv, SRLAYER Ml) { return Mv - Ml.transy; }

	private int getWorldX(int Mv, SRLAYER Ml) { return Mv + Ml.transx; }

	private int getWorldY(int Mv, SRLAYER Ml) { return Mv + Ml.transy; }

	/************************************* CODE TO CREATE RESULTS IN THE CIRCUIT *************************************/

	/**
	 * routing grid database methods
	 */
	private boolean extractPaths(Cell parent, SRNET net)
	{
		// adjust paths to account for precise port location
		double fX = 0, fY = 0;
		for (SRPATH path = net.paths; path != null; path = path.next)
		{
			if (path.type == SRPFIXED) continue;
			SRPORT port = path.port;
			fX = path.wx[0];   fY = path.wy[0];
			double oFX = fX, oFY = fY;
			if (path.end[0] && port.pi != null)
			{
//				fX = port.cX;   fY = port.cY;
//				if (fX != oFX || fY != oFY)
//				{
//					adjustPath(net.paths, path, 0, fX-oFX, fY-oFY);
//				}
				Poly portPoly = port.pi.getPoly();
				Point2D closest = portPoly.closestPoint(new Point2D.Double(fX, fY));
				if (closest.getX() != oFX || closest.getY() != oFY)
					adjustPath(net.paths, path, 0, closest.getX()-oFX, closest.getY()-oFY);
			} else
			{
				ArcProto ap = path.layer.index != 0 ? mazeVertWire : mazeHorizWire;
				List<PortInst> portInstList = findPort(parent, fX, fY, ap, net, true);
				if (portInstList.size() > 0)
				{
					PortInst pi = (PortInst)portInstList.get(0);
					Poly portPoly = pi.getPoly();
					Point2D closest = portPoly.closestPoint(new Point2D.Double(fX, fY));
					if (closest.getX() != oFX || closest.getY() != oFY)
						adjustPath(net.paths, path, 0, closest.getX()-oFX, closest.getY()-oFY);
				}
			}

			fX = oFX = path.wx[1];   fY = oFY = path.wy[1];
			if (path.end[1] && port.pi != null)
			{
//				fX = port.cX;   fY = port.cY;
//				if (fX != oFX || fY != oFY)
//				{
//					adjustPath(net.paths, path, 1, fX-oFX, fY-oFY);
//				}
				Poly portPoly = port.pi.getPoly();
				Point2D closest = portPoly.closestPoint(new Point2D.Double(fX, fY));
				if (closest.getX() != oFX || closest.getY() != oFY)
					adjustPath(net.paths, path, 1, closest.getX()-oFX, closest.getY()-oFY);
			} else
			{
				ArcProto ap = path.layer.index != 0 ? mazeVertWire : mazeHorizWire;
				List<PortInst> portInstList = findPort(parent, fX, fY, ap, net, true);
				if (portInstList.size() > 0)
				{
					PortInst pi = (PortInst)portInstList.get(0);
					Poly portPoly = pi.getPoly();
					Point2D closest = portPoly.closestPoint(new Point2D.Double(fX, fY));
					if (closest.getX() != oFX || closest.getY() != oFY)
						adjustPath(net.paths, path, 1, closest.getX()-oFX, closest.getY()-oFY);
				}
			}
		}

		// now do the routing
		for (SRPATH path = net.paths; path != null; path = path.next)
		{
			if (path.type == SRPFIXED) continue;
			ArcProto ap = path.layer.index != 0 ? mazeVertWire : mazeHorizWire;
			SRPORT port = path.port;

			// create arc between the end points
			List<PortInst> fromPortInstList = null;
			fX = path.wx[0];   fY = path.wy[0];
			if (path.end[0] && port.pi != null)
			{
				fromPortInstList = new ArrayList<PortInst>();
				fromPortInstList.add(port.pi);
			} else
			{
				fromPortInstList = findPort(parent, fX, fY, ap, net, false);
				if (fromPortInstList.size() == 0)
				{
					// create the from pin
					double xS = mazeSteinerNode.getDefWidth();
					double yS = mazeSteinerNode.getDefHeight();
					NodeInst ni = NodeInst.makeInstance(mazeSteinerNode, new Point2D.Double(fX, fY), xS, yS, parent);
					if (ni == null)
					{
						System.out.println("Could not create pin");
						return true;
					}
					fromPortInstList.add(ni.getPortInst(0));
				}
			}

			List<PortInst> toPortInstList = null;
			double tX = path.wx[1];   double tY = path.wy[1];
			if (path.end[1] && port.pi != null)
			{
				toPortInstList = new ArrayList<PortInst>();
				toPortInstList.add(port.pi);
			} else
			{
				toPortInstList = findPort(parent, tX, tY, ap, net, false);
				if (toPortInstList.size() == 0)
				{
					// create the from pin
					double xS = mazeSteinerNode.getDefWidth();
					double yS = mazeSteinerNode.getDefHeight();
					NodeInst ni = NodeInst.makeInstance(mazeSteinerNode, new Point2D.Double(tX, tY), xS, yS, parent);
					if (ni == null)
					{
						System.out.println("Could not create pin");
						return true;
					}
					toPortInstList.add(ni.getPortInst(0));
				}
			}

			// now connect (note only nodes for now, no bus like connections)
			if (fromPortInstList.size() > 0 && toPortInstList.size() > 0)
			{
				// now make the connection (simple wire to wire for now)
				PortInst fPi = (PortInst)fromPortInstList.get(0);
				PortInst tPi = (PortInst)toPortInstList.get(0);
				ArcInst ai = ArcInst.makeInstance(ap, ap.getDefaultWidth(), fPi, tPi, new Point2D.Double(fX, fY), new Point2D.Double(tX, tY), null);
				if (ai == null)
				{
					System.out.println("Could not create path (arc)");
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Method to recursively adjust paths to account for port positions that may not
	 * be on the grid.
	 */
	private void adjustPath(SRPATH paths, SRPATH thisPath, int end, double dX, double dY)
	{
		if (dX != 0)
		{
			double formerThis = thisPath.wx[end];
			double formerOther = thisPath.wx[1-end];
			thisPath.wx[end] += dX;
			if (formerThis == formerOther)
			{
				double formerX = thisPath.wx[1-end];
				double formerY = thisPath.wy[1-end];
				thisPath.wx[1-end] += dX;
				for (SRPATH opath = paths; opath != null; opath = opath.next)
				{
					if (opath.wx[0] == formerX && opath.wy[0] == formerY)
					{
						adjustPath(paths, opath, 0, dX, 0);
						break;
					}
					if (opath.wx[1] == formerX && opath.wy[1] == formerY)
					{
						adjustPath(paths, opath, 1, dX, 0);
						break;
					}
				}
			}
		}

		if (dY != 0)
		{
			double formerThis = thisPath.wy[end];
			double formerOther = thisPath.wy[1-end];
			thisPath.wy[end] += dY;
			if (formerThis == formerOther)
			{
				double formerX = thisPath.wx[1-end];
				double formerY = thisPath.wy[1-end];
				thisPath.wy[1-end] += dY;
				for (SRPATH opath = paths; opath != null; opath = opath.next)
				{
					if (opath.wx[0] == formerX && opath.wy[0] == formerY)
					{
						adjustPath(paths, opath, 0, 0, dY);
						break;
					}
					if (opath.wx[1] == formerX && opath.wy[1] == formerY)
					{
						adjustPath(paths, opath, 1, 0, dY);
						break;
					}
				}
			}
		}
	}

	/**
	 * Method to locate the PortInsts corresponding to
	 * to a direct intersection with the given point.
	 * inputs:
	 * cell - cell to search
	 * x, y  - the point to exam
	 * ap    - the arc used to connect port (must match pp)
	 */
	private List<PortInst> findPort(Cell cell, double x, double y, ArcProto ap, SRNET srnet, boolean forceFind)
	{
		List<PortInst> portInstList = new ArrayList<PortInst>();
		Point2D searchPoint = new Point2D.Double(x, y);
		double bestDist = 0;
		PortInst closestPi = null;
		Rectangle2D searchBounds = new Rectangle2D.Double(x-0.5, y-0.5, 1, 1);
		for(Iterator<Geometric> sea = cell.searchIterator(searchBounds); sea.hasNext(); )
		{
			Geometric geom = sea.next();

			if (geom instanceof NodeInst)
			{
				// now locate a portproto
				NodeInst ni = (NodeInst)geom;
				for(Iterator<PortInst> it = ni.getPortInsts(); it.hasNext(); )
				{
					PortInst pi = it.next();
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

						// LINTED "bestDist" used in proper order
						if (closestPi == null || dist < bestDist)
						{
							bestDist = dist;
							closestPi = pi;
						}
					}
				}
			}
		}

		if (portInstList.size() == 0 && forceFind && closestPi != null && bestDist < 1)
		{
			portInstList.add(closestPi);
		}
		return portInstList;
	}

	/**
	 * Debugging code to show the maze.
	 */
	private void dumpLayer(String message, SRREGION region, byte layers)
	{
		// scan for the first layer
		SRLAYER layer = null;
		for (int index = 0, mask = 1; index < SRMAXLAYERS; index++, mask = mask<<1)
		{
			if ((mask & layers) == 0) continue;
			layer = region.layers[index];
			if (layer != null) break;
		}
		int hei = layer.hei;
		int wid = layer.wid;
		System.out.println("====================== " + message + " ======================");
		System.out.print("   ");
		for (int x = 0; x < wid; x++) System.out.print((x / 10));
		System.out.print("\n   ");
		for (int x = 0; x < wid; x++) System.out.print((x % 10));
		System.out.println("");
		for (int y = hei-1; y >= 0; y--)
		{
			System.out.print(y);
			for (int x = 0; x < wid; x++)
			{
				char gpt = ' ';
				for (int index = 0, mask = 1; index < SRMAXLAYERS; index++, mask = mask<<1)
				{
					if ((mask & layers) == 0) continue;
					layer = region.layers[index];
					if (layer == null) continue;
					if ((layer.grids[x][y] & SR_GSET) != 0)
					{
						if ((layer.grids[x][y] & SR_GPORT) != 0) gpt = 'P'; else
							gpt = '*';
					} else
					{
						if ((layer.grids[x][y] & SR_GWAVE) != 0) gpt = 'W'; else
							if (layer.grids[x][y] != 0)
								gpt = (char)('A' + (layer.grids[x][y] & SR_GMASK) - SR_GSTART);
					}
				}
				System.out.print(gpt);
			}
			System.out.println("");
		}
		System.out.println("");
	}

}
