/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SeaOfGates.java
 * Routing tool: Sea of Gates
 * Written by: Steven M. Rubin
 *
 * Copyright (c) 2007 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.RTBounds;
import com.sun.electric.database.topology.RTNode;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.DRCTemplate;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.user.ErrorLogger;

import java.awt.Rectangle;
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
import java.util.TreeSet;

/**
 * Class to do sea-of-gates routing.
 * This router replaces unrouted arcs with real geometry.  It has these features:
 * > The router only works in layout, and only routes metal wires.
 * > The router uses vias to move up and down the metal layers.
 *   > Understands multiple vias and multiple via orientations.
 * > The router is not tracked: it runs on the Electric grid
 *   > All wires are on full-grid units
 *   > Tries to cover multiple grid units in a single jump
 * > Routes power and ground first, then goes by length (shortest nets first)
 * > Prefers to run odd metal layers on horizontal, even layers on vertical
 * > Routes in both directions (from A to B and from B to A) and chooses the best
 * > Users can request that some layers not be used, can request that some layers be favored
 * > Routes are made as wide as the widest arc already connected to any point
 * > Cost penalty also includes space left in the track on either side of a segment
 *
 * Things to do:
 *     At the end of routing, try again with those that failed
 *     Detect "river routes" and route specially
 *     Ability to route to any previous part of route when daisy-chaining?
 *     Lower cost if running over existing layout (on the same net)
 *     Ability to connect to anything on the destination net
 *     Rip-up
 *     Global routing
 */
public class SeaOfGates
{
	/** True to display the first routing failure. */							private static final boolean DEBUGSTEPS = false;
	/** True to display the first routing failure. */							private static final boolean DEBUGFAILURE = false;
	/** True to search by more than 1 grid unit at a time. */					private static final boolean SEARCHINJUMPS = true;
	/** True to search long stretches faster. */								private static final boolean FASTERJUMPS = true;

	/** Cost of routing in wrong direction (alternating horizontal/vertical) */	private static final int COSTALTERNATINGMETAL = 50;
	/** Cost of changing layers. */												private static final int COSTLAYERCHANGE = 8;
	/** Cost of routing away from the target. */								private static final int COSTWRONGDIRECTION = 5;
	/** Cost of running on non-favored layer. */								private static final int COSTUNFAVORED = 10;
	/** Cost of making a turn. */												private static final int COSTTURNING = 1;

	/** Cell in which routing occurs. */										private Cell cell;
	/** Technology to use for routing. */										private Technology tech;
	/** R-Trees for metal blockage in the cell. */								private Map<Layer,RTNode> metalTrees;
	/** R-Trees for via blockage in the cell. */								private Map<Layer,RTNode> viaTrees;
	/** Maps Arcs to network IDs in the R-Tree. */								private Map<ArcInst,Integer> netIDs;
	/** number of metal layers in the technology. */							private int numMetalLayers;
	/** metal layers in the technology. */										private Layer [] metalLayers;
	/** via layers in the technology. */										private Layer [] viaLayers;
	/** arcs to use for each metal layer. */									private ArcProto [] metalArcs;
	/** favoritism for each metal layer. */										private boolean [] favorArcs;
	/** avoidance for each metal layer. */										private boolean [] preventArcs;
	/** vias to use to go up from each metal layer. */							private MetalVias [] metalVias;
	/** minimum spacing between this metal and itself. */						private Map<Integer,Double>[] layerSurround;
	/** minimum spacing between the centers of two vias. */						private double [] viaSurround;
	/** intermediate seach vertices during Dijkstra search. */					private Map<Integer,Map<Integer,SearchVertex>> [] searchVertexPlanes;
	/** destination coordinate for the current Dijkstra path. */				private int destX, destY, destZ;
	/** the total length of wires routed */										private double totalWireLength;
	/** true if this is the first failure of a route (for debugging) */			private boolean firstFailure;
	/** for logging errors */													private ErrorLogger errorLogger;

	/************************************** CONTROL **************************************/

	/**
	 * Method to run Sea-of-Gates routing
	 */
	public static void seaOfGatesRoute()
	{
		// get cell and network information
		UserInterface ui = Job.getUserInterface();
		Cell cell = ui.needCurrentCell();
		if (cell == null) return;
		Netlist netList = cell.acquireUserNetlist();
		if (netList == null)
		{
			System.out.println("Sorry, a deadlock aborted routing (network information unavailable).  Please try again");
			return;
		}

		// get list of selected nets
		Set<Network> nets = null;
		boolean didSelection = false;
		EditWindow_ wnd = ui.getCurrentEditWindow_();
		if (wnd != null)
		{
			nets = wnd.getHighlightedNetworks();
			if (nets.size() > 0) didSelection = true;
		}
		if (!didSelection)
		{
			nets = new HashSet<Network>();
			for(Iterator<Network> it = netList.getNetworks(); it.hasNext(); )
				nets.add(it.next());
		}

		// only consider nets that have unrouted arcs on them
		Set<Network> netsToRoute = new HashSet<Network>();
		for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = it.next();
			if (ai.getProto() != Generic.tech.unrouted_arc) continue;
			Network net = netList.getNetwork(ai, 0);
			if (nets.contains(net)) netsToRoute.add(net);
		}

		// make sure there is something to route
		if (netsToRoute.size() <= 0)
		{
			ui.showErrorMessage(didSelection ? "Must select one or more Unrouted Arcs" :
				"There are no Unrouted Arcs in this cell", "Routing Error");
			return;
		}

		// order the nets appropriately
		List<NetsToRoute> orderedNetsToRoute = new ArrayList<NetsToRoute>();
		for(Network net : netsToRoute)
		{
			boolean isPwrGnd = false;
			for(Iterator<Export> it = net.getExports(); it.hasNext(); )
			{
				Export e = it.next();
				if (e.isGround() || e.isPower()) { isPwrGnd = true;   break; }
			}
			double length = 0;
			for(Iterator<ArcInst> it = net.getArcs(); it.hasNext(); )
			{
				ArcInst ai = it.next();
				length += ai.getLambdaLength();
				PortProto headPort = ai.getHeadPortInst().getPortProto();
				PortProto tailPort = ai.getTailPortInst().getPortProto();
				if (headPort.isGround() || headPort.isPower() ||
					tailPort.isGround() || tailPort.isPower()) isPwrGnd = true;
			}
			NetsToRoute ntr = new NetsToRoute(net, length, isPwrGnd);
			orderedNetsToRoute.add(ntr);
		}
		Collections.sort(orderedNetsToRoute, new NetsToRouteByLength());

		// convert to a list of Arcs to route because nets get redone after each one is routed
		List<ArcInst> arcsToRoute = new ArrayList<ArcInst>();
		for(NetsToRoute ntr : orderedNetsToRoute)
		{
			for(Iterator<ArcInst> it = ntr.net.getArcs(); it.hasNext(); )
			{
				ArcInst ai = it.next();
				if (ai.getProto() != Generic.tech.unrouted_arc) continue;
				arcsToRoute.add(ai);
				break;
			}
		}

		// do the routing in a separate job
		new SeaOfGatesJob(cell, arcsToRoute);
	}

	/**
	 * Class to define a network that needs to be routed.
	 * Extra information lets the nets be sorted by length and power/ground usage.
	 */
	private static class NetsToRoute
	{
		private Network net;
		private double length;
		private boolean isPwrGnd;

		NetsToRoute(Network net, double length, boolean isPwrGnd)
		{
			this.net = net;
			this.length = length;
			this.isPwrGnd = isPwrGnd;
		}
	}

	/**
	 * Comparator class for sorting NetsToRoute by their length and power/ground usage.
	 */
	public static class NetsToRouteByLength implements Comparator<NetsToRoute>
	{
		/**
		 * Method to sort NetsToRoute by their length and power/ground usage.
		 */
		public int compare(NetsToRoute ntr1, NetsToRoute ntr2)
		{
			// make power or ground nets come first
			if (ntr1.isPwrGnd != ntr2.isPwrGnd)
			{
				if (ntr1.isPwrGnd) return -1;
				return 1;
			}

			// make shorter nets come before longer ones
			if (ntr1.length < ntr2.length) return -1;
			if (ntr1.length > ntr2.length) return 1;
			return 0;
		}
	}

	/**
	 * Class to run sea-of-gates routing in a separate Job.
	 */
	private static class SeaOfGatesJob extends Job
	{
		private Cell cell;
		private List<ArcInst> arcsToRoute;

		protected SeaOfGatesJob(Cell cell, List<ArcInst> arcsToRoute)
		{
			super("Sea-Of-Gates Route", Routing.getRoutingTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.arcsToRoute = arcsToRoute;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			SeaOfGates router = new SeaOfGates();
			router.routeIt(this, cell, arcsToRoute);
			return true;
		}
	}

	/************************************** ROUTING **************************************/

	/**
	 * This is the public interface for Sea-of-Gates Routing when done in batch mode.
	 * @param cell the cell to be Sea-of-Gates-routed.
	 * @param arcsToRoute a List of ArcInsts on networks to be routed.
	 */
	public void routeIt(Job job, Cell cell, List<ArcInst> arcsToRoute)
	{
		// initialize information about the technology
		if (initializeDesignRules(cell)) return;

		// user-interface initialization
		long startTime = System.currentTimeMillis();
		Job.getUserInterface().startProgressDialog("Routing " + arcsToRoute.size() + " nets", null);
		Job.getUserInterface().setProgressNote("Building blockage information...");

		// create an error logger
		errorLogger = ErrorLogger.newInstance("Routing (Sea of gates)");

		// get all blockage information into R-Trees
		metalTrees = new HashMap<Layer, RTNode>();
		viaTrees = new HashMap<Layer, RTNode>();
		netIDs = new HashMap<ArcInst,Integer>();
		BlockageVisitor visitor = new BlockageVisitor(arcsToRoute);
		HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, visitor);
		addBlockagesAtPorts(arcsToRoute);
//for(Iterator<Layer> it = metalTrees.keySet().iterator(); it.hasNext(); )
//{
//	Layer layer = it.next();
//	RTNode root = metalTrees.get(layer);
//	System.out.println("RTree for "+layer.getName()+":");
//	root.printRTree(2);
//}

		// route the networks
		int numFailedRoutes = 0;
		int numRoutedSegments = 0, numSegments = 0;
		firstFailure = true;
		totalWireLength = 0;
		int numToRoute = arcsToRoute.size();
		for(int a=0; a<numToRoute; a++)
		{
			if (job.checkAbort())
			{
				System.out.println("Sea-of-gates routing aborted");
				break;
			}

			// get list of PortInsts that comprise this net
			ArcInst ai = arcsToRoute.get(a);
			Netlist netList = cell.acquireUserNetlist();
			if (netList == null)
			{
				System.out.println("Sorry, a deadlock aborted routing (network information unavailable).  Please try again");
				break;
			}
			Network net = netList.getNetwork(ai, 0);
			if (net == null)
			{
				System.out.println("Arc " + ai.describe(false) + " has no network!");
				continue;
			}
			Job.getUserInterface().setProgressValue(a*100/numToRoute);
			Job.getUserInterface().setProgressNote("Network " + net.getName());
			System.out.println("Routing network " + net.getName() + "...");
			HashSet<ArcInst> arcsToDelete = new HashSet<ArcInst>();
			HashSet<NodeInst> nodesToDelete = new HashSet<NodeInst>();
			List<Connection> netEnds = Routing.findNetEnds(net, arcsToDelete, nodesToDelete, netList, true);
			List<PortInst> orderedPorts = makeOrderedPorts(net, netEnds);
			if (orderedPorts == null)
			{
				System.out.println("No valid connection points found on the network.");
				continue;
			}
//EditWindow_ wnd = Job.getUserInterface().getCurrentEditWindow_();
//wnd.clearHighlighting();
//for(int i=0; i<orderedPorts.size(); i++)
//{
//	PortInst pi = orderedPorts.get(i);
//	PolyBase poly = pi.getPoly();
//	wnd.addHighlightMessage(cell, ""+i, poly.getCenter());
//	System.out.println("Number "+i+" is port "+pi.getPortProto().getName()+" of node "+pi.getNodeInst().describe(false));
//}
//wnd.finishedHighlighting();

			// determine the minimum width of arcs on this net
			double minWidth = getMinWidth(orderedPorts);

			// find a path between the ends of the network
			boolean allRouted = true;
			boolean [] segRouted = new boolean[orderedPorts.size()-1];
			int netID = -1;
			Integer netIDI = netIDs.get(ai);
			if (netIDI != null) netID = netIDI.intValue();
			for(int i=0; i<orderedPorts.size()-1; i++)
			{
				PortInst fromPi = orderedPorts.get(i);
				PortInst toPi = orderedPorts.get(i+1);
				numSegments++;
				if (inValidPort(fromPi) || inValidPort(toPi))
				{
					allRouted = false;
					continue;
				}

				segRouted[i] = findPath(netID, fromPi, toPi, minWidth);
				if (segRouted[i]) numRoutedSegments++; else
					allRouted = false;
			}
			if (allRouted)
			{
				// routed: remove the unrouted arcs
				for(ArcInst aiKill : arcsToDelete)
					aiKill.kill();
				cell.killNodes(nodesToDelete);
			} else
			{
				numFailedRoutes++;
				// remove arcs that are routed
				for(ArcInst aiKill : arcsToDelete)
				{
					int headPort = -1, tailPort = -1;
					for(int i=0; i<orderedPorts.size(); i++)
					{
						PortInst pi = orderedPorts.get(i);
						if (aiKill.getHeadPortInst() == pi) headPort = i; else
							if (aiKill.getTailPortInst() == pi) tailPort = i;
					}
					if (headPort >= 0 && tailPort >= 0)
					{
						boolean failed = false;
						if (headPort > tailPort) { int swap = headPort;   headPort = tailPort;   tailPort = swap; }
						for(int i=headPort; i<tailPort; i++)
							if (!segRouted[i]) failed = true;
						if (!failed) aiKill.kill();
					}
				}
			}
		}

		// clean up at end
		errorLogger.termLogging(true);
		long stopTime = System.currentTimeMillis();
		Job.getUserInterface().stopProgressDialog();
		System.out.println("Routed " + numRoutedSegments + " out of " + numSegments +
			" segments; total length of routed wires is " + TextUtils.formatDouble(totalWireLength) +
			"; took " + TextUtils.getElapsedTime(stopTime-startTime));
		if (numFailedRoutes > 0)
			System.out.println("NOTE: " + numFailedRoutes + " nets were not routed");
	}

	/**
	 * Method to initialize technology information, including design rules.
	 * @return true on error.
	 */
	private boolean initializeDesignRules(Cell c)
	{
		// find the metal layers, arcs, and contacts
		cell = c;
		tech = cell.getTechnology();
		numMetalLayers = tech.getNumMetals();
		metalLayers = new Layer[numMetalLayers];
		metalArcs = new ArcProto[numMetalLayers];
		favorArcs = new boolean[numMetalLayers];
		preventArcs = new boolean[numMetalLayers];
		viaLayers = new Layer[numMetalLayers-1];
		metalVias = new MetalVias[numMetalLayers-1];
		for(int i=0; i<numMetalLayers-1; i++) metalVias[i] = new MetalVias();
		for(Iterator<Layer> it = tech.getLayers(); it.hasNext(); )
		{
			Layer lay = it.next();
			if (!lay.getFunction().isMetal()) continue;
			if (lay.isPseudoLayer()) continue;
			int layerIndex = lay.getFunction().getLevel()-1;
			if (layerIndex < numMetalLayers) metalLayers[layerIndex] = lay;
		}
		boolean hasFavorites = false;
		for(Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); )
		{
			ArcProto ap = it.next();
			for(int i=0; i<numMetalLayers; i++)
			{
				if (ap.getLayer(0) == metalLayers[i])
				{
					metalArcs[i] = ap;
					favorArcs[i] = Routing.isSeaOfGatesFavor(ap);
					if (favorArcs[i]) hasFavorites = true;
					preventArcs[i] = Routing.isSeaOfGatesPrevent(ap);
					break;
				}
			}
		}
		if (!hasFavorites)
			for(int i=0; i<numMetalLayers; i++) favorArcs[i] = true;
		for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = it.next();
			if (np.isNotUsed()) continue;
			if (np.getFunction() != PrimitiveNode.Function.CONTACT) continue;
			ArcProto [] conns = np.getPort(0).getConnections();
			for(int i=0; i<numMetalLayers-1; i++)
			{
				if ((conns[0] == metalArcs[i] && conns[1] == metalArcs[i+1]) ||
					(conns[1] == metalArcs[i] && conns[0] == metalArcs[i+1]))
				{
					metalVias[i].addVia(np, 0);

					// see if the node is asymmetric and should exist in rotated states
					boolean square = true, offCenter = false;
					NodeInst dummyNi = NodeInst.makeDummyInstance(np);
					Poly [] conPolys = tech.getShapeOfNode(dummyNi);
					for(int p=0; p<conPolys.length; p++)
					{
						Poly conPoly = conPolys[p];
						Layer conLayer = conPoly.getLayer();
						Layer.Function lFun = conLayer.getFunction();
						if (lFun.isMetal())
						{
							Rectangle2D conRect = conPoly.getBounds2D();
							if (conRect.getWidth() != conRect.getHeight()) square = false;
							if (conRect.getCenterX() != 0 || conRect.getCenterY() != 0) offCenter = true;
						} else if (lFun.isContact())
						{
							viaLayers[i] = conLayer;
						}
					}
					if (offCenter)
					{
						// off center: test in all 4 rotations
						metalVias[i].addVia(np, 90);
						metalVias[i].addVia(np, 180);
						metalVias[i].addVia(np, 270);
					} else if (!square)
					{
						// centered but not square: test in 90-degree rotation
						metalVias[i].addVia(np, 90);
					}
					break;
				}
			}
		}
		for(int i=0; i<numMetalLayers; i++)
		{
			if (metalLayers[i] == null)
			{
				System.out.println("ERROR: Cannot find layer for Metal " + (i+1));
				return true;
			}
			if (metalArcs[i] == null)
			{
				System.out.println("ERROR: Cannot find arc for Metal " + (i+1));
				return true;
			}
			if (i < numMetalLayers-1)
			{
				if (metalVias[i].getVias().size() == 0)
				{
					System.out.println("ERROR: Cannot find contact node between Metal " + (i+1) + " and Metal " + (i+2));
					return true;
				}
				if (viaLayers[i] == null)
				{
					System.out.println("ERROR: Cannot find contact layer between Metal " + (i+1) + " and Metal " + (i+2));
					return true;
				}
			}
		}

		// compute design rule spacings
		layerSurround = new Map[numMetalLayers];
		for(int i=0; i<numMetalLayers; i++)
			layerSurround[i] = new HashMap<Integer,Double>();
		viaSurround = new double[numMetalLayers-1];
		for(int i=0; i<numMetalLayers-1; i++)
		{
			Layer lay = viaLayers[i];

			double spacing = 2;
			double arcWidth = metalArcs[i].getDefaultLambdaBaseWidth();
			DRCTemplate ruleSpacing = DRC.getSpacingRule(lay, null, lay, null, false, -1, arcWidth, 50);
			if (ruleSpacing != null) spacing = ruleSpacing.getValue(0);

			// determine cut size
			double width = 0;
			DRCTemplate ruleWidth = DRC.getMinValue(lay, DRCTemplate.DRCRuleType.NODSIZ);
			if (ruleWidth != null) width = ruleWidth.getValue(0);

			// extend to the size of the largest cut
			List<MetalVia> nps = metalVias[i].getVias();
			for(MetalVia mv : nps)
			{
				NodeInst dummyNi = NodeInst.makeDummyInstance(mv.via);
				Poly [] conPolys = tech.getShapeOfNode(dummyNi);
				for(int p=0; p<conPolys.length; p++)
				{
					Poly conPoly = conPolys[p];
					if (conPoly.getLayer().getFunction().isContact())
					{
						Rectangle2D bounds = conPoly.getBounds2D();
						width = Math.max(width, bounds.getWidth());
						width = Math.max(width, bounds.getHeight());
					}
				}
			}

			viaSurround[i] = spacing + width;
		}
		return false;
	}

	/**
	 * Method to determine the design rule spacing between two pieces of a given layer.
	 * @param layer the layer index.
	 * @param width the width of one of the pieces (-1 to use default).
	 * @return the design rule spacing (0 if none).
	 */
	private double getSpacingRule(int layer, double width)
	{
		// use default width if none specified
		if (width < 0) width = metalArcs[layer].getDefaultLambdaBaseWidth();

		// convert this to the next largest integer
		Integer wid = Integer.valueOf((int)Math.ceil(width));

		// see if the rule is cached
		Double value = layerSurround[layer].get(wid);
		if (value == null)
		{
			// rule not cached: compute it
			Layer lay = metalLayers[layer];
			DRCTemplate rule = DRC.getSpacingRule(lay, null, lay, null, false, -1, width, 50);
			double v = 0;
			if (rule != null) v = rule.getValue(0);
			value = new Double(v);
			layerSurround[layer].put(wid, value);
		}
		return value.doubleValue();
	}

	private double getMinWidth(List<PortInst> orderedPorts)
	{
		double minWidth = 0;
		for(PortInst pi : orderedPorts)
		{
			double widestAtPort = getWidestMetalArcOnPort(pi);
			if (widestAtPort > minWidth) minWidth = widestAtPort;
		}
		if (minWidth > Routing.getSeaOfGatesMaxWidth()) minWidth = Routing.getSeaOfGatesMaxWidth();
		return minWidth;
	}

	/**
	 * Get the widest metal arc already connected to a given PortInst.
	 * Looks recursively down the hierarchy.
	 * @param pi the PortInst to connect.
	 * @return the widest metal arc connect to that port (zero if none)
	 */
	private double getWidestMetalArcOnPort(PortInst pi)
	{
		// first check the top level
		double width = 0;
		for (Iterator<Connection> it = pi.getConnections(); it.hasNext(); )
		{
			Connection c = it.next();
			ArcInst ai = c.getArc();
			if (!ai.getProto().getFunction().isMetal()) continue;
			double newWidth = ai.getLambdaBaseWidth();
			if (newWidth > width) width = newWidth;
		}

		// now recurse down the hierarchy
		NodeInst ni = pi.getNodeInst();
		if (ni.isCellInstance())
		{
			Export export = (Export)pi.getPortProto();
			PortInst exportedInst = export.getOriginalPort();
			double width2 = getWidestMetalArcOnPort(exportedInst);
			if (width2 > width) width = width2;
		}
		return width;
	}

	private boolean inValidPort(PortInst pi)
	{
		ArcProto [] conns = pi.getPortProto().getBasePort().getConnections();
		boolean valid = false;
		for(int j=0; j<conns.length; j++)
		{
			ArcProto ap = conns[j];
			if (ap.getTechnology() != tech) continue;
			if (!ap.getFunction().isMetal()) continue;
			if (preventArcs[conns[j].getFunction().getLevel()-1]) continue;
			valid = true;
			break;
		}
		if (!valid)
		{
			System.out.println("Cannot connect to port " + pi.getPortProto().getName() +
				" on node " + pi.getNodeInst().describe(false) +
				" because all connecting layers have been prevented by Routing Preferences");
			return true;
		}
		return false;
	}

	/**
	 * Method to add extra blockage information that corresponds to ends of unrouted arcs.
	 * @param arcsToRoute the list of arcs to route.
	 * @param tech the technology to use.
	 */
	private void addBlockagesAtPorts(List<ArcInst> arcsToRoute)
	{
		Netlist netList = cell.acquireUserNetlist();
		if (netList == null) return;

		for(ArcInst ai : arcsToRoute)
		{
			int netID = -1;
			Integer netIDI = netIDs.get(ai);
			if (netIDI != null) netID = netIDI.intValue();
			Network net = netList.getNetwork(ai, 0);
			HashSet<ArcInst> arcsToDelete = new HashSet<ArcInst>();
			HashSet<NodeInst> nodesToDelete = new HashSet<NodeInst>();
			List<Connection> netEnds = Routing.findNetEnds(net, arcsToDelete, nodesToDelete, netList, true);
			List<PortInst> orderedPorts = makeOrderedPorts(net, netEnds);
			if (orderedPorts == null) continue;

			// determine the minimum width of arcs on this net
			double minWidth = getMinWidth(orderedPorts);

			for(PortInst pi : orderedPorts)
			{
				PolyBase poly = pi.getPoly();
				Rectangle2D polyBounds = poly.getBounds2D();
				ArcProto[] poss = pi.getPortProto().getBasePort().getConnections();
				int lowMetal = -1, highMetal = -1;
				for(int i=0; i<poss.length; i++)
				{
					if (poss[i].getTechnology() != tech) continue;
					if (!poss[i].getFunction().isMetal()) continue;
					int level = poss[i].getFunction().getLevel();
					if (lowMetal < 0) lowMetal = highMetal = level; else
					{
						lowMetal = Math.min(lowMetal, level);
						highMetal = Math.max(highMetal, level);
					}
				}
				if (lowMetal < 0) continue;

				// reserve space on layers above and below
				for(int via = lowMetal-2; via < highMetal; via++)
				{
					if (via < 0 || via >= numMetalLayers-1) continue;
					MetalVia mv = metalVias[via].getVias().get(0);
					PrimitiveNode np = mv.via;
					SizeOffset so = np.getProtoSizeOffset();
					double xOffset = so.getLowXOffset() + so.getHighXOffset();
					double yOffset = so.getLowYOffset() + so.getHighYOffset();
					double wid = Math.max(np.getDefWidth()-xOffset, minWidth) + xOffset;
					double hei = Math.max(np.getDefHeight()-yOffset, minWidth) + yOffset;
					NodeInst dummy = NodeInst.makeDummyInstance(np, EPoint.ORIGIN, wid, hei, Orientation.IDENT);
					PolyBase [] polys = tech.getShapeOfNode(dummy);
					for(int i=0; i<polys.length; i++)
					{
						PolyBase viaPoly = polys[i];
						Layer layer = viaPoly.getLayer();
						if (!layer.getFunction().isMetal()) continue;
						Rectangle2D viaBounds = viaPoly.getBounds2D();
						Rectangle2D bounds = new Rectangle2D.Double(viaBounds.getMinX() + polyBounds.getCenterX(),
							viaBounds.getMinY() + polyBounds.getCenterY(), viaBounds.getWidth(), viaBounds.getHeight());
						addRectangle(bounds, layer, netID);
					}
				}
			}
		}
	}

	/**
	 * Method to order a set of connections for optimal routing.
	 * @param net the Network being ordered.
	 * @param netEnds a list of Connections that must be routed.
	 * @return a list of PortInsts to connect which are ordered such that they
	 * form the proper sequence of routes to make.  Returns null on error.
	 */
	private List<PortInst> makeOrderedPorts(Network net, List<Connection> netEnds)
	{
		Set<PortInst> portEndSet = new HashSet<PortInst>();
		for(int i=0; i<netEnds.size(); i++)
		{
			PortInst pi = netEnds.get(i).getPortInst();
			if (!pi.getNodeInst().isCellInstance() &&
				((PrimitiveNode)pi.getNodeInst().getProto()).getTechnology() == Generic.tech)
					continue;
			portEndSet.add(pi);
		}
		int count = portEndSet.size();
		if (count == 0) return null;
		if (count == 1)
		{
			System.out.println("Error: Network " + net.describe(false) + " has only one end");
			return null;
		}
		PortInst [] portEnds = new PortInst[count];
		int k=0;
		for(PortInst pi : portEndSet) portEnds[k++] = pi;

		// find the closest two points
		int closest1 = 0, closest2 = 0;
		double closestDist = Double.MAX_VALUE;
		for(int i=0; i<count; i++)
		{
			PolyBase poly1 = portEnds[i].getPoly();
			for(int j=i+1; j<count; j++)
			{
				PolyBase poly2 = portEnds[j].getPoly();
				double dist = poly1.getCenter().distance(poly2.getCenter());
				if (dist < closestDist)
				{
					closestDist = dist;
					closest1 = i;
					closest2 = j;
				}
			}
		}
		List<PortInst> orderedPorts = new ArrayList<PortInst>();
		orderedPorts.add(portEnds[closest1]);
		orderedPorts.add(portEnds[closest2]);
		portEnds[closest1] = null;
		portEnds[closest2] = null;
		for(;;)
		{
			// find closest port to ends of current string
			boolean foundsome = false;
			double closestDist1 = Double.MAX_VALUE, closestDist2 = Double.MAX_VALUE;
			for(int i=0; i<count; i++)
			{
				if (portEnds[i] == null) continue;
				PolyBase poly = portEnds[i].getPoly();
				PolyBase poly1 = orderedPorts.get(0).getPoly();
				PolyBase poly2 = orderedPorts.get(orderedPorts.size()-1).getPoly();
				double dist1 = poly.getCenter().distance(poly1.getCenter());
				if (dist1 < closestDist1)
				{
					closestDist1 = dist1;
					closest1 = i;
					foundsome = true;
				}
				double dist2 = poly.getCenter().distance(poly2.getCenter());
				if (dist2 < closestDist2)
				{
					closestDist2 = dist2;
					closest2 = i;
					foundsome = true;
				}
			}
			if (!foundsome) break;
			if (closestDist1 < closestDist2)
			{
				orderedPorts.add(0, portEnds[closest1]);
				portEnds[closest1] = null;
			} else
			{
				orderedPorts.add(portEnds[closest2]);
				portEnds[closest2] = null;
			}
		}
		return orderedPorts;
	}

	/**
	 * Method to find a path between two ports.
	 * @param netID the network ID of the path.
	 * @param fromPi one end of the desired route.
	 * @param toPi the other end of the desired route.
	 * @param minWidth the minimum width of arcs on this net.
	 * @return true if routed, false if it failed.
	 */
	private boolean findPath(int netID, PortInst fromPi, PortInst toPi, double minWidth)
	{
		// get information about one end of the path
		ArcProto fromArc = null;
		ArcProto[] fromArcs = fromPi.getPortProto().getBasePort().getConnections();
		for(int i=0; i<fromArcs.length; i++)
			if (fromArcs[i].getFunction().isMetal()) { fromArc = fromArcs[i];   break; }
		if (fromArc == null)
		{
			String errorMsg = "Cannot connect port " + fromPi.getPortProto().getName() +
				" of node " + fromPi.getNodeInst().describe(false) + " because it has no metal connection";
			System.out.println("ERROR: " + errorMsg);
			List<PolyBase> polyList = new ArrayList<PolyBase>();
			polyList.add(fromPi.getPoly());
			errorLogger.logError(errorMsg, null, null, null, null, polyList, cell, 0);
			return false;
		}
		EPoint fromLoc = fromPi.getPoly().getCenter();

		// get information about the other end of the path
		ArcProto toArc = null;
		ArcProto[] toArcs = toPi.getPortProto().getBasePort().getConnections();
		for(int i=0; i<toArcs.length; i++)
			if (toArcs[i].getFunction().isMetal()) { toArc = toArcs[i];   break; }
		if (toArc == null)
		{
			String errorMsg = "Cannot connect port " + toPi.getPortProto().getName() +
				" of node " + toPi.getNodeInst().describe(false) + " because it has no metal connection";
			System.out.println("ERROR: " + errorMsg);
			List<PolyBase> polyList = new ArrayList<PolyBase>();
			polyList.add(toPi.getPoly());
			errorLogger.logError(errorMsg, null, null, null, null, polyList, cell, 0);
			return false;
		}
		EPoint toLoc = toPi.getPoly().getCenter();

		// determine the unit coordinates of the route
		int fromX, fromY, toX, toY;
		if (toLoc.getX() < fromLoc.getX())
		{
			toX = (int)Math.ceil(toLoc.getX());
			fromX = (int)Math.floor(fromLoc.getX());
		} else
		{
			toX = (int)Math.floor(toLoc.getX());
			fromX = (int)Math.ceil(fromLoc.getX());
		}
		if (toLoc.getY() < fromLoc.getY())
		{
			toY = (int)Math.ceil(toLoc.getY());
			fromY = (int)Math.floor(fromLoc.getY());
		} else
		{
			toY = (int)Math.floor(toLoc.getY());
			fromY = (int)Math.ceil(fromLoc.getY());
		}
		int fromZ = fromArc.getFunction().getLevel()-1;
		int toZ = toArc.getFunction().getLevel()-1;

		if (fromArc.getTechnology() != tech || toArc.getTechnology() != tech)
		{
			String errorMsg = "Route from port " + fromPi.getPortProto().getName() + " of node " + fromPi.getNodeInst().describe(false) +
				" on arc " + fromArc.describe() + " cannot connect to port " + toPi.getPortProto().getName() + " of node " +
				toPi.getNodeInst().describe(false) + " on arc " + toArc.describe() + " because they have different technologies";
			System.out.println("ERROR: " + errorMsg);
			List<PolyBase> polyList = new ArrayList<PolyBase>();
			PolyBase fromPoly = fromPi.getPoly();
			PolyBase toPoly = toPi.getPoly();
			polyList.add(fromPoly);
			polyList.add(toPoly);
			List<EPoint> lineList = new ArrayList<EPoint>();
			lineList.add(new EPoint(toPoly.getCenterX(), toPoly.getCenterY()));
			lineList.add(new EPoint(fromPoly.getCenterX(), fromPoly.getCenterY()));
			errorLogger.logError(errorMsg, null, null, lineList, null, polyList, cell, 0);
			return false;
		}

		// see if access is blocked
		double metalSpacing = Math.max(metalArcs[fromZ].getDefaultLambdaBaseWidth(), minWidth) / 2;
		double surround = getSpacingRule(fromZ, -1);
		SOGBound block = getMetalBlockage(netID, metalLayers[fromZ], metalSpacing, metalSpacing, surround,
			fromX, fromY, false, null, minWidth, 0, 0);
		if (block != null)
		{
			String errorMsg = "Cannot Route to port " + fromPi.getPortProto().getName() + " of node " + fromPi.getNodeInst().describe(false) +
				" because it is blocked on layer " + metalLayers[fromZ].getName() + " [needs " + TextUtils.formatDouble(metalSpacing+surround) +
				" all around, has blockage at (" + TextUtils.formatDouble(block.bound.getCenterX()) + "," +
				TextUtils.formatDouble(block.bound.getCenterY()) + ") that is " + TextUtils.formatDouble(block.bound.getWidth()) +
				"x" + TextUtils.formatDouble(block.bound.getHeight()) + "]";
			System.out.println(errorMsg);
			List<PolyBase> polyList = new ArrayList<PolyBase>();
			polyList.add(new PolyBase(fromX, fromY, (metalSpacing+surround)*2, (metalSpacing+surround)*2));
			polyList.add(new PolyBase(block.bound));
			List<EPoint> lineList = new ArrayList<EPoint>();
			lineList.add(new EPoint(block.bound.getMinX(), block.bound.getMinY()));
			lineList.add(new EPoint(block.bound.getMaxX(), block.bound.getMaxY()));
			lineList.add(new EPoint(block.bound.getMinX(), block.bound.getMaxY()));
			lineList.add(new EPoint(block.bound.getMaxX(), block.bound.getMinY()));
			errorLogger.logError(errorMsg, null, null, lineList, null, polyList, cell, 0);
			return false;
		}
		metalSpacing = Math.max(metalArcs[toZ].getDefaultLambdaBaseWidth(), minWidth) / 2;
		surround = getSpacingRule(toZ, -1);
		block = getMetalBlockage(netID, metalLayers[toZ], metalSpacing, metalSpacing, surround,
			toX, toY, false, null, minWidth, 0, 0);
		if (block != null)
		{
			String errorMsg = "Cannot route to port " + toPi.getPortProto().getName() +
				" of node " + toPi.getNodeInst().describe(false) +
				" because it is blocked on layer " + metalLayers[toZ].getName() +
				".  Needs " + TextUtils.formatDouble(metalSpacing+surround) + " all around, has blockage at (" +
				TextUtils.formatDouble(block.bound.getCenterX()) + "," + TextUtils.formatDouble(block.bound.getCenterY()) +
				") that is " + TextUtils.formatDouble(block.bound.getWidth()) + "x" + TextUtils.formatDouble(block.bound.getHeight());
			System.out.println("ERROR: " + errorMsg);
			List<PolyBase> polyList = new ArrayList<PolyBase>();
			polyList.add(new PolyBase(toX, toY, (metalSpacing+surround)*2, (metalSpacing+surround)*2));
			polyList.add(new PolyBase(block.bound));
			List<EPoint> lineList = new ArrayList<EPoint>();
			lineList.add(new EPoint(block.bound.getMinX(), block.bound.getMinY()));
			lineList.add(new EPoint(block.bound.getMaxX(), block.bound.getMaxY()));
			lineList.add(new EPoint(block.bound.getMinX(), block.bound.getMaxY()));
			lineList.add(new EPoint(block.bound.getMaxX(), block.bound.getMinY()));
			errorLogger.logError(errorMsg, null, null, lineList, null, polyList, cell, 0);
			return false;
		}

		// do the Dijkstra one way
		List<SearchVertex> vertices = doDijkstra(fromX, fromY, fromZ, toX, toY, toZ, netID, minWidth);
		Map<Integer,Map<Integer,SearchVertex>> [] saveD1Planes = null;
		if (DEBUGFAILURE && firstFailure) saveD1Planes = searchVertexPlanes;

		// do the Dijkstra the other way
		List<SearchVertex> verticesRev = doDijkstra(toX, toY, toZ, fromX, fromY, fromZ, netID, minWidth);
		int verLength = getVertexLength(vertices);
		int verLengthRev = getVertexLength(verticesRev);
		if (verLength == Integer.MAX_VALUE && verLengthRev == Integer.MAX_VALUE)
		{
			// failed to route
			String errorMsg;
			if (vertices == null && verticesRev == null)
			{
				errorMsg = "Search too complex (exceeds complexity limit parameter of " +
					Routing.getSeaOfGatesComplexityLimit() + ")";
			} else
			{
				errorMsg = "Failed to route from port " + fromPi.getPortProto().getName() +
					" of node " + fromPi.getNodeInst().describe(false) + " to port " + toPi.getPortProto().getName() +
					" of node " + toPi.getNodeInst().describe(false);
			}
			System.out.println("ERROR: " + errorMsg);
			List<EPoint> lineList = new ArrayList<EPoint>();
			lineList.add(new EPoint(toX, toY));
			lineList.add(new EPoint(fromX, fromY));
			errorLogger.logError(errorMsg, null, null, lineList, null, null, cell, 0);

			if (DEBUGFAILURE && firstFailure)
			{
				firstFailure = false;
				EditWindow_ wnd = Job.getUserInterface().getCurrentEditWindow_();
				wnd.clearHighlighting();
				showSearchVertices(saveD1Planes, true);
				showSearchVertices(searchVertexPlanes, false);
				wnd.finishedHighlighting();
			}
			return false;
		}
		if (verLength == Integer.MAX_VALUE || (verLength > verLengthRev))
		{
			// reverse path is better
			vertices = verticesRev;
			PortInst pi = toPi;   toPi = fromPi;   fromPi = pi;
			int s = toX;   toX = fromX;   fromX = s;
			s = toY;   toY = fromY;   fromY = s;
			s = toZ;   toZ = fromZ;   fromZ = s;
		}

		PortInst lastPort = toPi;
		PolyBase toPoly = toPi.getPoly();
		if (toPoly.getCenterX() != toX || toPoly.getCenterY() != toY)
		{
			// end of route is off-grid: adjust it
			if (vertices.size() >= 2)
			{
				SearchVertex v1 = vertices.get(0);
				SearchVertex v2 = vertices.get(1);
				ArcProto type = metalArcs[toZ];
				double width = Math.max(type.getDefaultLambdaBaseWidth(), minWidth);
				PrimitiveNode np = metalArcs[toZ].findPinProto();
				if (v1.getX() == v2.getX())
				{
					// first line is vertical: run a horizontal bit
					NodeInst ni = makeNodeInst(np, new EPoint(v1.getX(), toPoly.getCenterY()),
						np.getDefWidth(), np.getDefHeight(), Orientation.IDENT, cell, netID);
					makeArcInst(type, width, ni.getOnlyPortInst(), toPi, netID);
					lastPort = ni.getOnlyPortInst();
				} else if (v1.getY() == v2.getY())
				{
					// first line is horizontal: run a vertical bit
					NodeInst ni = makeNodeInst(np, new EPoint(toPoly.getCenterX(), v1.getY()),
						np.getDefWidth(), np.getDefHeight(), Orientation.IDENT, cell, netID);
					makeArcInst(type, width, ni.getOnlyPortInst(), toPi, netID);
					lastPort = ni.getOnlyPortInst();
				}
			}
		}
		for(int i=0; i<vertices.size(); i++)
		{
			SearchVertex sv = vertices.get(i);
			boolean madeContacts = false;
			while (i < vertices.size()-1)
			{
				SearchVertex svNext = vertices.get(i+1);
				if (sv.getX() != svNext.getX() || sv.getY() != svNext.getY() || sv.getZ() == svNext.getZ()) break;
				List<MetalVia> nps = metalVias[Math.min(sv.getZ(), svNext.getZ())].getVias();
				int whichContact = sv.getContactNo();
				MetalVia mv = nps.get(whichContact);
				PrimitiveNode np = mv.via;
				Orientation orient = Orientation.fromJava(mv.orientation*10, false, false);
				SizeOffset so = np.getProtoSizeOffset();
				double xOffset = so.getLowXOffset() + so.getHighXOffset();
				double yOffset = so.getLowYOffset() + so.getHighYOffset();
				double wid = Math.max(np.getDefWidth()-xOffset, minWidth) + xOffset;
				double hei = Math.max(np.getDefHeight()-yOffset, minWidth) + yOffset;
				NodeInst ni = makeNodeInst(np, new EPoint(sv.getX(), sv.getY()), wid, hei, orient, cell, netID);
				ArcProto type = metalArcs[sv.getZ()];
				double width = Math.max(type.getDefaultLambdaBaseWidth(), minWidth);
				makeArcInst(type, width, lastPort, ni.getOnlyPortInst(), netID);
				lastPort = ni.getOnlyPortInst();
				madeContacts = true;
				sv = svNext;
				i++;
			}
			if (madeContacts && i != vertices.size()-1) continue;

			PrimitiveNode np = metalArcs[sv.getZ()].findPinProto();
			PortInst pi = null;
			if (i == vertices.size()-1)
			{
				pi = fromPi;
				PolyBase fromPoly = fromPi.getPoly();
				if (fromPoly.getCenterX() != sv.getX() || fromPoly.getCenterY() != sv.getY())
				{
					// end of route is off-grid: adjust it
					if (vertices.size() >= 2)
					{
						SearchVertex v1 = vertices.get(vertices.size()-2);
						SearchVertex v2 = vertices.get(vertices.size()-1);
						ArcProto type = metalArcs[fromZ];
						double width = Math.max(type.getDefaultLambdaBaseWidth(), minWidth);
						if (v1.getX() == v2.getX())
						{
							// last line is vertical: run a horizontal bit
							PrimitiveNode pNp = metalArcs[fromZ].findPinProto();
							NodeInst ni = makeNodeInst(pNp, new EPoint(v1.getX(), fromPoly.getCenterY()),
								np.getDefWidth(), np.getDefHeight(), Orientation.IDENT, cell, netID);
							makeArcInst(type, width, ni.getOnlyPortInst(), fromPi, netID);
							pi = ni.getOnlyPortInst();
						} else if (v1.getY() == v2.getY())
						{
							// last line is horizontal: run a vertical bit
							PrimitiveNode pNp = metalArcs[fromZ].findPinProto();
							NodeInst ni = makeNodeInst(pNp, new EPoint(fromPoly.getCenterX(), v1.getY()),
								np.getDefWidth(), np.getDefHeight(), Orientation.IDENT, cell, netID);
							makeArcInst(type, width, ni.getOnlyPortInst(), fromPi, netID);
							pi = ni.getOnlyPortInst();
						}
					}
				}
			} else
			{
				NodeInst ni = makeNodeInst(np, new EPoint(sv.getX(), sv.getY()),
					np.getDefWidth(), np.getDefHeight(), Orientation.IDENT, cell, netID);
				pi = ni.getOnlyPortInst();
			}
			if (lastPort != null)
			{
				ArcProto type = metalArcs[sv.getZ()];
				double width = Math.max(type.getDefaultLambdaBaseWidth(), minWidth);
				makeArcInst(type, width, lastPort, pi, netID);
			}
			lastPort = pi;
		}
		return true;
	}


	/**
	 * Method to sum up the distance that a route takes.
	 * @param vertices the list of SearchVertices in the route.
	 * @return the length of the route.
	 */
	private int getVertexLength(List<SearchVertex> vertices)
	{
		if (vertices == null) return Integer.MAX_VALUE;
		if (vertices.size() == 0) return Integer.MAX_VALUE;
		int sum = 0;
		SearchVertex last = null;
		for(SearchVertex sv : vertices)
		{
			if (last != null)
				sum += Math.abs(sv.getX() - last.getX()) +
					Math.abs(sv.getY() - last.getY()) +
					Math.abs(sv.getZ() - last.getZ())*10;
			last = sv;
		}
		return sum;
	}

	/**
	 * Method to create a NodeInst and update the R-Trees.
	 * @param np the prototype of the new NodeInst.
	 * @param loc the location of the new NodeInst.
	 * @param wid the width of the new NodeInst.
	 * @param hei the height of the new NodeInst.
	 * @param orient the orientation of the new NodeInst.
	 * @param cell the Cell in which to place the new NodeInst.
	 * @param netID the network ID of geometry in this NodeInst.
	 * @return the NodeInst that was created (null on error).
	 */
	private NodeInst makeNodeInst(NodeProto np, EPoint loc, double wid, double hei, Orientation orient, Cell cell, int netID)
	{
		NodeInst ni = NodeInst.makeInstance(np, loc, wid, hei, cell, orient, null, 0);
		if (ni != null)
		{
			AffineTransform trans = ni.rotateOut();
			Poly [] nodeInstPolyList = tech.getShapeOfNode(ni, true, false, null);
			for(int i=0; i<nodeInstPolyList.length; i++)
			{
				PolyBase poly = nodeInstPolyList[i];
				if (poly.getPort() == null) continue;
				poly.transform(trans);
				addLayer(poly, GenMath.MATID, netID, false);
			}
		}
		return ni;
	}

	/**
	 * Method to create an ArcInst and update the R-Trees.
	 * @param type the prototype of the new ArcInst.
	 * @param wid the width of the new ArcInst.
	 * @param from the head PortInst of the new ArcInst.
	 * @param to the tail PortInst of the new ArcInst.
	 * @param netID the network ID of geometry in this ArcInst.
	 * @return the ArcInst that was created (null on error).
	 */
	private ArcInst makeArcInst(ArcProto type, double wid, PortInst from, PortInst to, int netID)
	{
		ArcInst ai = ArcInst.makeInstanceBase(type, wid, from, to);
		if (ai != null)
		{
			PolyBase [] polys = tech.getShapeOfArc(ai);
			for(int i=0; i<polys.length; i++)
				addLayer(polys[i], GenMath.MATID, netID, false);

			// accumulate the total length of wires placed
			PolyBase fromPoly = from.getPoly();
			PolyBase toPoly = to.getPoly();
			double length = fromPoly.getCenter().distance(toPoly.getCenter());
			totalWireLength += length;
		}
		return ai;
	}

	/**
	 * Method to run a Dijkstra search to find a route path.
	 * @param fromX the X coordinate of the start of the search.
	 * @param fromY the Y coordinate of the start of the search.
	 * @param fromZ the Z coordinate (metal layer) of the start of the search.
	 * @param toX the X coordinate of the end of the search.
	 * @param toY the Y coordinate of the end of the search.
	 * @param toZ the Z coordinate (metal layer) of the end of the search.
	 * @param netID the network ID of geometry on this route path.
	 * @param minWidth the minimum arc width for this network.
	 * @return a list of SearchVertex objects that define the path.
	 * Returns null if the search is too complex.
	 * Returns an empty list if no path can be found
	 */
	private List<SearchVertex> doDijkstra(int fromX, int fromY, int fromZ, int toX, int toY, int toZ, int netID, double minWidth)
	{
		Rectangle2D bounds = cell.getBounds();
		int lowX = (int)Math.floor(bounds.getMinX());
		int highX = (int)Math.ceil(bounds.getMaxX());
		int lowY = (int)Math.floor(bounds.getMinY());
		int highY = (int)Math.ceil(bounds.getMaxY());
		Rectangle jumpBound = new Rectangle(Math.min(fromX, toX), Math.min(fromY, toY), Math.abs(fromX-toX), Math.abs(fromY-toY));
		searchVertexPlanes = new Map[numMetalLayers];
		destX = toX;   destY = toY;   destZ = toZ;
		int numSearchVertices = 0;

		SearchVertex svStart = new SearchVertex(fromX, fromY, fromZ, 0, null, 0);
		svStart.cost = 0;
		setVertex(fromX, fromY, fromZ, svStart);
		TreeSet<SearchVertex> active = new TreeSet<SearchVertex>();
		active.add(svStart);

		SearchVertex thread = null;
		while (active.size() > 0)
		{
			// get the lowest cost point
			SearchVertex svCurrent = active.first();
			active.remove(svCurrent);
			int curX = svCurrent.getX();
			int curY = svCurrent.getY();
			int curZ = svCurrent.getZ();

			// look at all directions from this point
			SearchVertex [] costs;
			if (DEBUGSTEPS) costs = new SearchVertex[6];
			for(int i=0; i<6; i++)
			{
				// compute a neighboring point
				int dx = 0, dy = 0, dz = 0;
				switch (i)
				{
					case 0: dx = -1;   break;
					case 1: dx =  1;   break;
					case 2: dy = -1;   break;
					case 3: dy =  1;   break;
					case 4: dz = -1;   break;
					case 5: dz =  1;   break;
				}

				// extend the distance if heading toward the goal
				if (SEARCHINJUMPS && dz == 0)
				{
					boolean goFarther = false;
					if (dx != 0)
					{
						if ((toX-curX) * dx > 0) goFarther = true;
					} else
					{
						if ((toY-curY) * dy > 0) goFarther = true;
					}
					if (goFarther)
					{
						if (FASTERJUMPS)
						{
							int jumpSize = getJumpSize(curX, curY, curZ, dx, dy, jumpBound, netID, minWidth);
							if (dx > 0)
							{
								if (jumpSize <= 0) continue;
								dx = jumpSize;
							}
							if (dx < 0)
							{
								if (jumpSize >= 0) continue;
								dx = jumpSize;
							}
							if (dy > 0)
							{
								if (jumpSize <= 0) continue;
								dy = jumpSize;
							}
							if (dy < 0)
							{
								if (jumpSize >= 0) continue;
								dy = jumpSize;
							}
						} else
						{
							int hi, lo;
							if (dx != 0)
							{
								lo = dx;
								hi = toX - curX;
							} else
							{
								lo = dy;
								hi = toY - curY;
							}
							double width = Math.max(metalArcs[curZ].getDefaultLambdaBaseWidth(), minWidth);
							double metalSpacing = width / 2 + getSpacingRule(curZ, width);
							while (Math.abs(hi-lo) > 1)
							{
								int med = (hi + lo) / 2;
								int cX = curX+dx, cY = curY+dy;
								double halfWid = metalSpacing, halfHei = metalSpacing;
								if (dx != 0)
								{
									cX = curX + (dx + med) / 2;
									halfWid += Math.abs(med-dx)/2;
								} else
								{
									cY = curY + (dy + med) / 2;
									halfHei += Math.abs(med-dy)/2;
								}
								if (getMetalBlockage(netID, metalLayers[curZ], halfWid, halfHei, 0, cX, cY,
									true, svCurrent, minWidth, toX, toY) == null)
								{
									lo = med;
								} else
								{
									hi = med;
								}
							}
							if (dx != 0) dx = lo; else dy = lo;
						}
					}
				}

				int nX = curX + dx;
				int nY = curY + dy;
				int nZ = curZ + dz;
				if (nX < lowX || nX > highX) continue;
				if (nY < lowY || nY > highY) continue;
				if (nZ < 0 || nZ >= numMetalLayers) continue;
				if (preventArcs[nZ]) continue;

				// see if the adjacent point has already been visited
				if (getVertex(nX, nY, nZ) != null) continue;

				// see if the space is available
				int whichContact = 0;
				Point2D [] cuts = null;
				if (dz == 0)
				{
					// running on one layer: check surround
					double width = Math.max(metalArcs[nZ].getDefaultLambdaBaseWidth(), minWidth);
					double metalToMetal = getSpacingRule(nZ, width);
					double metalSpacing = width / 2;
					double checkX = (curX+nX)/2, checkY = (curY+nY)/2;
					double halfWid = metalSpacing + Math.abs(dx)/2;
					double halfHei = metalSpacing + Math.abs(dy)/2;
					SOGBound sb = getMetalBlockage(netID, metalLayers[nZ], halfWid, halfHei,
						metalToMetal, checkX, checkY, true, svCurrent, minWidth, toX, toY);
					if (sb != null) continue;
				} else
				{
					int lowMetal = Math.min(curZ, nZ);
					int highMetal = Math.max(curZ, nZ);
					List<MetalVia> nps = metalVias[lowMetal].getVias();
					whichContact = -1;
					for(int contactNo = 0; contactNo < nps.size(); contactNo++)
					{
						MetalVia mv = nps.get(contactNo);
						PrimitiveNode np = mv.via;
						Orientation orient = Orientation.fromJava(mv.orientation*10, false, false);
						SizeOffset so = np.getProtoSizeOffset();
						double conWid = Math.max(np.getDefWidth()-so.getLowXOffset()-so.getHighXOffset(), minWidth)+
							so.getLowXOffset()+so.getHighXOffset();
						double conHei = Math.max(np.getDefHeight()-so.getLowYOffset()-so.getHighYOffset(), minWidth)+
							so.getLowYOffset()+so.getHighYOffset();
						NodeInst dummyNi = NodeInst.makeDummyInstance(np, new EPoint(nX, nY), conWid, conHei, orient);
						Poly [] conPolys = tech.getShapeOfNode(dummyNi);
						AffineTransform trans = null;
						if (orient != Orientation.IDENT) trans = dummyNi.rotateOut();

						// count the number of cuts and make an array for the data
						int cutCount = 0;
						for(int p=0; p<conPolys.length; p++)
							if (conPolys[p].getLayer().getFunction().isContact()) cutCount++;
						Point2D [] curCuts = new Point2D[cutCount];
						cutCount = 0;

						boolean failed = false;
						for(int p=0; p<conPolys.length; p++)
						{
							Poly conPoly = conPolys[p];
							if (trans != null) conPoly.transform(trans);
							Layer conLayer = conPoly.getLayer();
							Layer.Function lFun = conLayer.getFunction();
							if (lFun.isMetal())
							{
								Rectangle2D conRect = conPoly.getBounds2D();
								int metalNo = lFun.getLevel() - 1;
								double surround = getSpacingRule(metalNo, Math.max(conRect.getWidth(), conRect.getHeight()));
								double halfWid = conRect.getWidth()/2;
								double halfHei = conRect.getHeight()/2;
								if (getMetalBlockage(netID, conLayer, halfWid, halfHei, surround,
									conRect.getCenterX(), conRect.getCenterY(), true, svCurrent, minWidth, toX, toY) != null)
								{
									failed = true;
									break;
								}
							} else if (lFun.isContact())
							{
								// make sure vias don't get too close
								Rectangle2D conRect = conPoly.getBounds2D();
								double conCX = conRect.getCenterX();
								double conCY = conRect.getCenterY();
								double surround = viaSurround[lowMetal];
								if (getViaBlockage(netID, conLayer, surround, surround, conCX, conCY) != null)
								{
									failed = true;
									break;
								}
								curCuts[cutCount++] = new Point2D.Double(conCX, conCY);

								// look at all previous cuts in this path
								for(SearchVertex sv = svCurrent; sv != null; sv = sv.last)
								{
									SearchVertex lastSv = sv.last;
									if (lastSv == null) break;
									if (Math.min(sv.getZ(), lastSv.getZ()) == lowMetal &&
										Math.max(sv.getZ(), lastSv.getZ()) == highMetal)
									{
										// make sure the cut isn't too close
										Point2D [] svCuts;
										if (sv.getCutLayer() == lowMetal) svCuts = sv.getCuts(); else
											svCuts = lastSv.getCuts();
										if (svCuts != null)
										{
											for(Point2D cutPt : svCuts)
											{
												if (Math.abs(cutPt.getX() - conCX) >= surround ||
													Math.abs(cutPt.getY() - conCY) >= surround) continue;
												failed = true;
												break;
											}
										}
										if (failed) break;
									}
								}
								if (failed) break;
							}
						}
						if (failed) continue;
						whichContact = contactNo;
						cuts = curCuts;
						break;
					}
					if (whichContact < 0) continue;
				}

				// we have a candidate next-point
				SearchVertex svNext = new SearchVertex(nX, nY, nZ, whichContact, cuts, Math.min(curZ, nZ));
				svNext.last = svCurrent;

				// stop if we found the destination
				if (nX == toX && nY == toY && nZ == toZ)
				{
					thread = svNext;
					break;
				}

				// stop if the search is too complex
				numSearchVertices++;
				if (numSearchVertices > Routing.getSeaOfGatesComplexityLimit()) return null;

				// compute the cost
				svNext.cost = svCurrent.cost;
				if (dx != 0)
				{
					if (toX == curX) svNext.cost += COSTWRONGDIRECTION/2; else
						if ((toX-curX) * dx < 0) svNext.cost += COSTWRONGDIRECTION;
					if (COSTALTERNATINGMETAL != 0 && (nZ%2) == 0) svNext.cost += COSTALTERNATINGMETAL;
				}
				if (dy != 0)
				{
					if (toY == curY) svNext.cost += COSTWRONGDIRECTION/2; else
						if ((toY-curY) * dy < 0) svNext.cost += COSTWRONGDIRECTION;
					if (COSTALTERNATINGMETAL != 0 && (nZ%2) != 0) svNext.cost += COSTALTERNATINGMETAL;
				}
				if (dz != 0)
				{
					if (toZ == curZ) svNext.cost += COSTLAYERCHANGE; else
						if ((toZ-curZ) * dz < 0) svNext.cost += COSTLAYERCHANGE * COSTWRONGDIRECTION;
				} else
				{
					// not changing layers: compute penalty for unused tracks on either side of run
					int jumpSize1 = Math.abs(getJumpSize(nX, nY, nZ, dx, dy, jumpBound, netID, minWidth));
					int jumpSize2 = Math.abs(getJumpSize(curX, curY, curZ, -dx, -dy, jumpBound, netID, minWidth));
					if (jumpSize1 > 1 && jumpSize2 > 1)
					{
						svNext.cost += (jumpSize1 * jumpSize2) / 10;
					}

					// not changing layers: penalize if turning in X or Y
					if (svCurrent.last != null)
					{
						boolean xTurn = svCurrent.getX() != svCurrent.last.getX();
						boolean yTurn = svCurrent.getY() != svCurrent.last.getY();
						if (xTurn != (dx != 0) || yTurn != (dy != 0)) svNext.cost += COSTTURNING;
					}
				}
				if (!favorArcs[nZ]) svNext.cost += (COSTLAYERCHANGE+COSTUNFAVORED)*Math.abs(dz) + COSTUNFAVORED*Math.abs(dx + dy);

				// add this vertex into the data structures
				setVertex(nX, nY, nZ, svNext);
				active.add(svNext);
				if (DEBUGSTEPS) costs[i] = svNext;
			}
			if (DEBUGSTEPS)
			{
				System.out.print("FROM ("+curX+","+curY+")C="+svCurrent.cost+" WENT");
				for(int i=0; i<6; i++)
				{
					if (costs[i] == null) continue;
					switch (i)
					{
						case 0: System.out.print("  -X");   break;
						case 1: System.out.print("  +X");   break;
						case 2: System.out.print("  -Y");   break;
						case 3: System.out.print("  +Y");   break;
						case 4: System.out.print("  -Z");   break;
						case 5: System.out.print("  +Z");   break;
					}
					System.out.print("("+costs[i].xv+","+costs[i].yv+")C="+costs[i].cost);
				}
				System.out.println();
			}
			if (thread != null) break;
		}

		List<SearchVertex> realVertices = new ArrayList<SearchVertex>();
		if (thread != null)
		{
			// found the path!
			SearchVertex lastVertex = thread;
			realVertices.add(lastVertex);
			thread = thread.last;
			while (thread != null)
			{
				if (lastVertex.getZ() != thread.getZ())
				{
					realVertices.add(thread);
					lastVertex = thread;
					thread = thread.last;
				} else
				{
					// gather a run of vertices on this layer
					int dx = thread.getX() - lastVertex.getX();
					int dy = thread.getY() - lastVertex.getY();
					lastVertex = thread;
					thread = thread.last;
					while (thread != null)
					{
						if (thread.getX() - lastVertex.getX() != dx ||
							thread.getY() - lastVertex.getY() != dy) break;
						lastVertex = thread;
						thread = thread.last;
					}
					realVertices.add(lastVertex);
				}
			}

			// optimize for backups
			for(int i=1; i<realVertices.size()-1; i++)
			{
				SearchVertex last = realVertices.get(i-1);
				SearchVertex cur = realVertices.get(i);
				SearchVertex next = realVertices.get(i+1);
				if (last.getZ() != cur.getZ() || next.getZ() != cur.getZ()) continue;
				if ((last.getX() == cur.getX() && next.getX() == cur.getX()) ||
					(last.getY() == cur.getY() && next.getY() == cur.getY()))
				{
					// colinear points
					realVertices.remove(i);
					i--;
					continue;
				}

			}
		} else
		{
//			dumpPlane(0);
//			dumpPlane(1);
//			dumpPlane(2);
		}
		return realVertices;
	}

	private int getJumpSize(int curX, int curY, int curZ, int dx, int dy, Rectangle jumpBound, int netID, double minWidth)
	{
		double width = Math.max(metalArcs[curZ].getDefaultLambdaBaseWidth(), minWidth);
		double metalToMetal = getSpacingRule(curZ, width);
		double metalSpacing = width / 2 + metalToMetal;
		double lX = curX - metalSpacing, hX = curX + metalSpacing;
		double lY = curY - metalSpacing, hY = curY + metalSpacing;
		if (dx > 0) hX = jumpBound.getMaxX()+metalSpacing; else
			if (dx < 0) lX = jumpBound.getMinX()-metalSpacing; else
				if (dy > 0) hY = jumpBound.getMaxY()+metalSpacing; else
					if (dy < 0) lY = jumpBound.getMinY()-metalSpacing;

		RTNode rtree = metalTrees.get(metalLayers[curZ]);
		if (rtree != null)
		{
			// see if there is anything in that area
			Rectangle2D searchArea = new Rectangle2D.Double(lX, lY, hX-lX, hY-lY);
			for(RTNode.Search sea = new RTNode.Search(searchArea, rtree, true); sea.hasNext(); )
			{
				SOGBound sBound = (SOGBound)sea.next();
				if (sBound.getNetID() == netID) continue;
				Rectangle2D bound = sBound.getBounds();
				if (bound.getMinX() >= hX || bound.getMaxX() <= lX ||
					bound.getMinY() >= hY || bound.getMaxY() <= lY) continue;
				if (dx > 0 && bound.getMinX() < hX) hX = bound.getMinX();
				if (dx < 0 && bound.getMaxX() > lX) lX = bound.getMaxX();
				if (dy > 0 && bound.getMinY() < hY) hY = bound.getMinY();
				if (dy < 0 && bound.getMaxY() > lY) lY = bound.getMaxY();
			}
		}
		if (dx > 0)
		{
			dx = (int)Math.floor(hX-metalSpacing)-curX;
			return dx;
		}
		if (dx < 0)
		{
			dx = (int)Math.ceil(lX+metalSpacing)-curX;
			return dx;
		}
		if (dy > 0)
		{
			dy = (int)Math.floor(hY-metalSpacing)-curY;
			return dy;
		}
		if (dy < 0)
		{
			dy = (int)Math.ceil(lY+metalSpacing)-curY;
			return dy;
		}
		return 0;
	}

	/************************************** BLOCKAGE DATA STRUCTURE **************************************/

	/**
	 * Class to define an R-Tree leaf node for geometry in the blockage data structure.
	 */
	private static class SOGBound implements RTBounds
	{
		private Rectangle2D bound;
		private int netID;

		SOGBound(Rectangle2D bound, int netID)
		{
			this.bound = bound;
			this.netID = netID;
		}

		public Rectangle2D getBounds() { return bound; }

		public int getNetID() { return netID; }

		public String toString() { return "SOGBound on net " + netID; }
	}

	private static class SOGPoly extends SOGBound
	{
		private PolyBase poly;

		SOGPoly(Rectangle2D bound, int netID, PolyBase poly)
		{
			super(bound, netID);
			this.poly = poly;
		}

		public PolyBase getPoly() { return poly; }
	}

	/**
	 * Class to define an R-Tree leaf node for vias in the blockage data structure.
	 */
	private static class SOGVia implements RTBounds
	{
		private Point2D loc;
		private int netID;

		SOGVia(Point2D loc, int netID)
		{
			this.loc = loc;
			this.netID = netID;
		}

		public Rectangle2D getBounds() { return new Rectangle2D.Double(loc.getX(), loc.getY(), 0, 0); }

		public int getNetID() { return netID; }

		public String toString() { return "SOGVia on net " + netID; }
	}

	private static final int LEFT = 0, RIGHT = 1, TOP = 2, BOTTOM = 3;

	/**
	 * Method to find a metal blockage in the R-Tree.
	 * @param netID the network ID of the desired space (blockages on this netID are ignored).
	 * @param layer the metal layer being examined.
	 * @param halfWidth half of the width of the area that will be filled.
	 * @param halfHeight half of the height of the area that will be filled.
	 * @param surround is the surround around the area that will be filled.
	 * @param x the X coordinate at the center of the area to examine.
	 * @param y the Y coordinate at the center of the area to examine.
	 * @param findNotches true to look for notch errors.
	 * @param svCurrent the list of SearchVertex's for finding notch errors in the current path.
	 * @param minWidth the minimum arc width on the current path.
	 * @param toX the X coordinate of the destination of the route.
	 * @param toY the Y coordinate of the destination of the route.
	 * @return a blocking SOGBound object that is in the area.
	 * Returns null if the area is clear.
	 */
	private SOGBound getMetalBlockage(int netID, Layer layer, double halfWidth, double halfHeight, double surround, double x, double y,
		boolean findNotches, SearchVertex svCurrent, double minWidth, double toX, double toY)
	{
		RTNode rtree = metalTrees.get(layer);
		if (rtree == null) return null;

		// compute the notch area
		double lXNotch = x - halfWidth, hXNotch = x + halfWidth;
		double lYNotch = y - halfHeight, hYNotch = y + halfHeight;
		Rectangle2D notchBound = new Rectangle2D.Double(lXNotch, lYNotch, hXNotch-lXNotch, hYNotch-lYNotch);
		SOGBound [] notchErrors = new SOGBound[4];
		double [] notchRangeLow = new double[4];
		double [] notchRangeHigh = new double[4];
		notchRangeLow[LEFT] = notchRangeLow[RIGHT] = hYNotch;
		notchRangeHigh[LEFT] = notchRangeHigh[RIGHT] = lYNotch;
		notchRangeLow[TOP] = notchRangeLow[BOTTOM] = hXNotch;
		notchRangeHigh[TOP] = notchRangeHigh[BOTTOM] = lXNotch;

		// see if there is anything in that area
		double lX = lXNotch - surround, hX = hXNotch + surround;
		double lY = lYNotch - surround, hY = hYNotch + surround;
		Rectangle2D searchArea = new Rectangle2D.Double(lX, lY, hX-lX, hY-lY);
		for(RTNode.Search sea = new RTNode.Search(searchArea, rtree, true); sea.hasNext(); )
		{
			SOGBound sBound = (SOGBound)sea.next();
			Rectangle2D bound = sBound.getBounds();
			if (bound.getMaxX() <= lX || bound.getMinX() >= hX ||
				bound.getMaxY() <= lY || bound.getMinY() >= hY) continue;

			// if on the same net, make sure there is no notch error
			if (sBound.getNetID() == netID)
			{
				if (findNotches)
				{
					if (toX < bound.getMinX() || toX > bound.getMaxX() ||
						toY < bound.getMinY() || toY > bound.getMaxY())
							processNotch(notchBound, sBound, notchErrors, notchRangeLow, notchRangeHigh);
				}
				continue;
			}

			// if this is a polygon, do closer examination
			if (sBound instanceof SOGPoly)
			{
				PolyBase poly = ((SOGPoly)sBound).getPoly();
				if (!poly.contains(searchArea)) continue;
			}
			return sBound;
		}

		// consider notch errors in the existing path
		if (findNotches && svCurrent != null)
		{
			for(SearchVertex sv = svCurrent; sv != null; sv = sv.last)
			{
				SearchVertex lastSv = sv.last;
				if (lastSv == null) break;
				if (sv.getZ() != lastSv.getZ()) continue;
				if (metalLayers[sv.getZ()] != layer) continue;
				ArcProto type = metalArcs[sv.getZ()];
				double width = Math.max(type.getDefaultLambdaBaseWidth(), minWidth);
				Point2D head = new Point2D.Double(sv.getX(), sv.getY());
				Point2D tail = new Point2D.Double(lastSv.getX(), lastSv.getY());
				int ang = 0;
				if (head.getX() != tail.getX() || head.getY() != tail.getY())
					ang = GenMath.figureAngle(tail, head);
				Poly poly = Poly.makeEndPointPoly(head.distance(tail), width, ang,
					head, width/2, tail, width/2, Poly.Type.FILLED);
				Rectangle2D bound = poly.getBounds2D();
				if (bound.getMaxX() <= lX || bound.getMinX() >= hX ||
					bound.getMaxY() <= lY || bound.getMinY() >= hY) continue;
				if (toX >= bound.getMinX() && toX <= bound.getMaxX() &&
					toY >= bound.getMinY() && toY <= bound.getMaxY()) continue;
				SOGBound sBound = new SOGBound(bound, netID);
				processNotch(notchBound, sBound, notchErrors, notchRangeLow, notchRangeHigh);
			}
		}

		// report any notch errors
		if (findNotches)
		{
			if (notchErrors[LEFT] != null && (notchRangeLow[LEFT] > lYNotch || notchRangeHigh[LEFT] < hYNotch))
				return notchErrors[LEFT];
			if (notchErrors[RIGHT] != null && (notchRangeLow[RIGHT] > lYNotch || notchRangeHigh[RIGHT] < hYNotch))
				return notchErrors[RIGHT];
			if (notchErrors[TOP] != null && (notchRangeLow[TOP] > lXNotch || notchRangeHigh[TOP] < hXNotch))
				return notchErrors[TOP];
			if (notchErrors[BOTTOM] != null && (notchRangeLow[BOTTOM] > lXNotch || notchRangeHigh[BOTTOM] < hXNotch))
				return notchErrors[BOTTOM];
		}
		return null;
	}

	/**
	 * Method to accumulate notch errors.
	 * @param notchBound the area to examine for notches.
	 * @param sBound the object that may cause a notch.
	 * @param notchErrors the errors found in all 4 directions.
	 * @param notchRangeLow the low range of notch boundaries in all 4 directions.
	 * @param notchRangeHigh the high range of notch boundaries in all 4 directions.
	 */
	private void processNotch(Rectangle2D notchBound, SOGBound sBound, SOGBound [] notchErrors,
		double [] notchRangeLow, double [] notchRangeHigh)
	{
		boolean touches = true;
		Rectangle2D bound = sBound.getBounds();
		if (bound.getMaxX() < notchBound.getMinX())
		{
			// could be a notch to the left of the area
			notchErrors[LEFT] = sBound;
			touches = false;
		}
		if (bound.getMinX() > notchBound.getMaxX())
		{
			// could be a notch to the right of the area
			notchErrors[RIGHT] = sBound;
			touches = false;
		}
		if (bound.getMinY() > notchBound.getMaxY())
		{
			// could be a notch to the top of the area
			notchErrors[TOP] = sBound;
			touches = false;
		}
		if (bound.getMaxY() < notchBound.getMinY())
		{
			// could be a notch to the bottom of the area
			notchErrors[BOTTOM] = sBound;
			touches = false;
		}

		// if objects touch, update notch bounds
		if (touches)
		{
			if (bound.getMinX() < notchBound.getMinX())
			{
				// extends on the left: update notch bounds
				notchRangeLow[LEFT] = Math.min(notchRangeLow[LEFT], bound.getMinY());
				notchRangeHigh[LEFT] = Math.max(notchRangeHigh[LEFT], bound.getMaxY());
			}
			if (bound.getMaxX() > notchBound.getMaxX())
			{
				// extends on the right: update notch bounds
				notchRangeLow[RIGHT] = Math.min(notchRangeLow[RIGHT], bound.getMinY());
				notchRangeHigh[RIGHT] = Math.max(notchRangeHigh[RIGHT], bound.getMaxY());
			}
			if (bound.getMinY() < notchBound.getMinY())
			{
				// extends on the bottom: update notch bounds
				notchRangeLow[BOTTOM] = Math.min(notchRangeLow[BOTTOM], bound.getMinX());
				notchRangeHigh[BOTTOM] = Math.max(notchRangeHigh[BOTTOM], bound.getMaxX());
			}
			if (bound.getMaxY() > notchBound.getMaxY())
			{
				// extends on the top: update notch bounds
				notchRangeLow[TOP] = Math.min(notchRangeLow[TOP], bound.getMinX());
				notchRangeHigh[TOP] = Math.max(notchRangeHigh[TOP], bound.getMaxX());
			}
		}
	}

	/**
	 * Method to find a via blockage in the R-Tree.
	 * @param netID the network ID of the desired space (vias at this point and on this netID are ignored).
	 * @param layer the via layer being examined.
	 * @param halfWidth half of the width of the area to examine.
	 * @param halfHeight half of the height of the area to examine.
	 * @param x the X coordinate at the center of the area to examine.
	 * @param y the Y coordinate at the center of the area to examine.
	 * @return a blocking SOGVia object that is in the area.
	 * Returns null if the area is clear.
	 */
	private SOGVia getViaBlockage(int netID, Layer layer, double halfWidth, double halfHeight, double x, double y)
	{
		RTNode rtree = viaTrees.get(layer);
		if (rtree == null) return null;

		// see if there is anything in that area
		Rectangle2D searchArea = new Rectangle2D.Double(x-halfWidth, y-halfHeight, halfWidth*2, halfHeight*2);
		for(RTNode.Search sea = new RTNode.Search(searchArea, rtree, true); sea.hasNext(); )
		{
			SOGVia sLoc = (SOGVia)sea.next();
			if (sLoc.getNetID() == netID)
			{
				if (sLoc.loc.getX() == x && sLoc.loc.getY() == y) continue;
			}
			return sLoc;
		}
		return null;
	}

	/**
	 * HierarchyEnumerator subclass to examine a cell for a given layer and fill an R-Tree.
	 */
	private class BlockageVisitor extends HierarchyEnumerator.Visitor
	{
		private List<ArcInst> arcsToRoute;
		private boolean didTopLevel;

		public BlockageVisitor(List<ArcInst> arcsToRoute)
		{
			this.arcsToRoute = arcsToRoute;
			didTopLevel = false;
		}

		public boolean enterCell(HierarchyEnumerator.CellInfo info) { return true; }

		public void exitCell(HierarchyEnumerator.CellInfo info)
		{
			Cell cell = info.getCell();
			Netlist nl = info.getNetlist();
			AffineTransform trans = info.getTransformToRoot();
			for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = it.next();
				int netID = -1;
				Network net = nl.getNetwork(ai, 0);
				if (net != null) netID = info.getNetID(net);
				Technology tech = ai.getProto().getTechnology();
				PolyBase [] polys = tech.getShapeOfArc(ai);
				for(int i=0; i<polys.length; i++)
					addLayer(polys[i], trans, netID, false);
			}
		}

		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
		{
			if (info.isRootCell() && !didTopLevel)
			{
				didTopLevel = true;
				if (arcsToRoute != null)
				{
					Netlist nl = info.getNetlist();
					for(ArcInst ai : arcsToRoute)
					{
						Network net = nl.getNetwork(ai, 0);
						int netID = info.getNetID(net);
						netIDs.put(ai, new Integer(netID));
					}
				}
			}
			NodeInst ni = no.getNodeInst();
			if (!ni.isCellInstance())
			{
				Netlist nl = info.getNetlist();
				AffineTransform trans = info.getTransformToRoot();
				AffineTransform nodeTrans = ni.rotateOut(trans);
				PrimitiveNode pNp = (PrimitiveNode)ni.getProto();
				Technology tech = pNp.getTechnology();
				Poly [] nodeInstPolyList = tech.getShapeOfNode(ni, true, false, null);
				boolean canPlacePseudo = info.isRootCell();
				if (!ni.hasExports()) canPlacePseudo = false;
				for(int i=0; i<nodeInstPolyList.length; i++)
				{
					PolyBase poly = nodeInstPolyList[i];
					int netID = -1;
					if (poly.getPort() != null)
					{
						Network net = nl.getNetwork(no, poly.getPort(), 0);
						if (net != null) netID = info.getNetID(net);
					}
					addLayer(poly, nodeTrans, netID, canPlacePseudo);
				}
			}
			return true;
		}
	}

	/**
	 * Method to add geometry to the R-Tree.
	 * @param poly the polygon to add (only rectangles are added, so the bounds is used).
	 * @param trans a transformation matrix to apply to the polygon.
	 * @param netID the global network ID of the geometry.
	 * @param canPlacePseudo true if pseudo-layers should be considered (converted to nonpseudo and stored).
	 * False to ignore pseudo-layers.
	 */
	private void addLayer(PolyBase poly, AffineTransform trans, int netID, boolean canPlacePseudo)
	{
		if (!canPlacePseudo && poly.isPseudoLayer()) return;
		Layer layer = poly.getLayer();
		if (canPlacePseudo) layer = layer.getNonPseudoLayer(); else
		{
			if (layer.isPseudoLayer()) return;
		}
		Layer.Function fun = layer.getFunction();
		if (fun.isMetal())
		{
			poly.transform(trans);
			Rectangle2D bounds = poly.getBox();
			if (bounds == null)
			{
				addPolygon(poly, layer, netID);
			} else
			{
				addRectangle(bounds, layer, netID);
			}
		} else if (fun.isContact())
		{
			Rectangle2D bounds = poly.getBounds2D();
			GenMath.transformRect(bounds, trans);
			addVia(new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()), layer, netID);
		}
	}

	/**
	 * Method to add a rectangle to the metal R-Tree.
	 * @param bounds the rectangle to add.
	 * @param layer the metal layer on which to add the rectangle.
	 * @param netID the global network ID of the geometry.
	 */
	private void addRectangle(Rectangle2D bounds, Layer layer, int netID)
	{
		RTNode root = metalTrees.get(layer);
		if (root == null)
		{
			root = RTNode.makeTopLevel();
			metalTrees.put(layer, root);
		}
		RTNode newRoot = RTNode.linkGeom(null, root, new SOGBound(bounds, netID));
		if (newRoot != root) metalTrees.put(layer, newRoot);
	}

	/**
	 * Method to add a polygon to the metal R-Tree.
	 * @param poly the polygon to add.
	 * @param layer the metal layer on which to add the rectangle.
	 * @param netID the global network ID of the geometry.
	 */
	private void addPolygon(PolyBase poly, Layer layer, int netID)
	{
		RTNode root = metalTrees.get(layer);
		if (root == null)
		{
			root = RTNode.makeTopLevel();
			metalTrees.put(layer, root);
		}
		Rectangle2D bounds = poly.getBounds2D();
		RTNode newRoot = RTNode.linkGeom(null, root, new SOGPoly(bounds, netID, poly));
		if (newRoot != root) metalTrees.put(layer, newRoot);
	}

	/**
	 * Method to add a point to the via R-Tree.
	 * @param loc the point to add.
	 * @param layer the via layer on which to add the point.
	 * @param netID the global network ID of the geometry.
	 */
	private void addVia(Point2D loc, Layer layer, int netID)
	{
		RTNode root = viaTrees.get(layer);
		if (root == null)
		{
			root = RTNode.makeTopLevel();
			viaTrees.put(layer, root);
		}
		RTNode newRoot = RTNode.linkGeom(null, root, new SOGVia(loc, netID));
		if (newRoot != root) viaTrees.put(layer, newRoot);
	}

	/**
	 * Class to define a list of possible nodes that can connect two layers.
	 * This includes orientation
	 */
	private static class MetalVia
	{
		PrimitiveNode via;
		int orientation;

		MetalVia(PrimitiveNode v, int o) { via = v;   orientation = o; }
	}

	/**
	 * Class to define a list of possible nodes that can connect two layers.
	 * This includes orientation
	 */
	private static class MetalVias
	{
		List<MetalVia> vias = new ArrayList<MetalVia>();

		void addVia(PrimitiveNode pn, int o)
		{
			vias.add(new MetalVia(pn, o));
			Collections.sort(vias, new PrimsBySize());
		}

		List<MetalVia> getVias() { return vias; }
	}

	/**
	 * Comparator class for sorting primitives by their size.
	 */
	private static class PrimsBySize implements Comparator<MetalVia>
	{
		/**
		 * Method to sort primitives by their size.
		 */
		public int compare(MetalVia mv1, MetalVia mv2)
		{
			PrimitiveNode pn1 = mv1.via;
			PrimitiveNode pn2 = mv2.via;
			double sz1 = pn1.getDefWidth() * pn1.getDefHeight();
			double sz2 = pn2.getDefWidth() * pn2.getDefHeight();
			if (sz1 < sz2) return -1;
			if (sz1 > sz2) return 1;
			return 0;
		}
	}

	/************************************** DIJKSTRA PATH SEARCHING **************************************/

	/**
	 * Class to define a vertex in the Dijkstra search.
	 */
	private class SearchVertex implements Comparable
	{
		/** the coordinate of the search vertex. */	private int xv, yv, zv;
		/** the cost of search to this vertex. */	private int cost;
		/** the layer of cuts in "cuts". */			private int cutLayer;
		/** the cuts in the contact. */				private Point2D [] cuts;
		/** the previous vertex in the search. */	private SearchVertex last;

		SearchVertex(int x, int y, int z, int whichContact, Point2D [] cuts, int cl)
		{
			xv = x;
			yv = y;
			zv = (z<<8) + (whichContact & 0xFF);
			this.cuts = cuts;
			cutLayer = cl;
		}

		int getX() { return xv; }

		int getY() { return yv; }

		int getZ() { return zv >> 8; }

		int getContactNo() { return zv & 0xFF; }

		Point2D [] getCuts() { return cuts; }

		int getCutLayer() { return cutLayer; }

		/**
		 * Method to sort SearchVertex objects by their cost.
		 */
		public int compareTo(Object svo)
		{
			SearchVertex sv = (SearchVertex)svo;
			int diff = cost - sv.cost;
			if (diff != 0) return diff;
			int thisDist = Math.abs(xv-destX) + Math.abs(yv-destY) + Math.abs(zv-destZ);
			int otherDist = Math.abs(sv.xv-destX) + Math.abs(sv.yv-destY) + Math.abs(sv.zv-destZ);
			return thisDist - otherDist;
		}
	}

	/**
	 * Method to get the SearchVertex at a given coordinate.
	 * @param x the X coordinate desired.
	 * @param y the Y coordinate desired.
	 * @param z the Z coordinate (metal layer) desired.
	 * @return the SearchVertex at that point (null if none).
	 */
	private SearchVertex getVertex(int x, int y, int z)
	{
		Map<Integer,Map<Integer,SearchVertex>> plane = searchVertexPlanes[z];
		if (plane == null) return null;
		Map<Integer,SearchVertex> row = plane.get(new Integer(y));
		if (row == null) return null;
		SearchVertex item = row.get(new Integer(x));
		return item;
	}

	/**
	 * Method to store a SearchVertex at a given coordinate.
	 * @param x the X coordinate desired.
	 * @param y the Y coordinate desired.
	 * @param z the Z coordinate (metal layer) desired.
	 * @param sv the SearchVertex to place at that coordinate.
	 */
	private void setVertex(int x, int y, int z, SearchVertex sv)
	{
		Map<Integer,Map<Integer,SearchVertex>> plane = searchVertexPlanes[z];
		if (plane == null)
		{
			plane = new HashMap<Integer,Map<Integer,SearchVertex>>();
			searchVertexPlanes[z] = plane;
		}
		Integer iY = new Integer(y);
		Map<Integer,SearchVertex> row = plane.get(iY);
		if (row == null)
		{
			row = new HashMap<Integer,SearchVertex>();
			plane.put(iY, row);
		}
		row.put(new Integer(x), sv);
	}

//	/**
//	 * Debugging method to dump an R-Tree plane.
//	 * @param z the metal layer to dump.
//	 */
//	private void dumpPlane(int z)
//	{
//		System.out.println("************************* METAL " + (z+1) + " *************************");
//		Map<Integer,Map<Integer,SearchVertex>> plane = searchVertexPlanes[z];
//		if (plane == null) return;
//		List<Integer> yValues = new ArrayList<Integer>();
//		for(Iterator<Integer> it = plane.keySet().iterator(); it.hasNext(); )
//			yValues.add(it.next());
//		Collections.sort(yValues);
//		int lowY = yValues.get(0).intValue();
//		int highY = yValues.get(yValues.size()-1).intValue();
//		int lowX = 1, highX = 0;
//		for(Integer y : yValues)
//		{
//			Map<Integer,SearchVertex> row = plane.get(y);
//			for(Iterator<Integer> it = row.keySet().iterator(); it.hasNext(); )
//			{
//				Integer x = it.next();
//				int xv = x.intValue();
//				if (lowX > highX) lowX = highX = xv; else
//				{
//					if (xv < lowX) lowX = xv;
//					if (xv > highX) highX = xv;
//				}
//			}
//		}
//		String lowXStr = " " + lowX;
//		String highXStr = " " + highX;
//		int numXDigits = Math.max(lowXStr.length(), highXStr.length());
//		String lowYStr = " " + lowY;
//		String highYStr = " " + highY;
//		int numYDigits = Math.max(lowYStr.length(), highYStr.length());
//
//		String space = "   ";
//		while (space.length() < numYDigits+3) space += " ";
//		System.out.print(space);
//		for(int x=lowX; x<=highX; x++)
//		{
//			String xCoord = " " + x;
//			while (xCoord.length() < numXDigits) xCoord = " " + xCoord;
//			System.out.print(xCoord);
//		}
//		System.out.println();
//
//		for(int y=highY; y>=lowY; y--)
//		{
//			String yCoord = " " + y;
//			while (yCoord.length() < numYDigits) yCoord = " " + yCoord;
//			System.out.print("Row"+yCoord);
//			Map<Integer,SearchVertex> row = plane.get(y);
//			if (row != null)
//			{
//				for(int x=lowX; x<=highX; x++)
//				{
//					SearchVertex sv = row.get(x);
//					String xCoord;
//					if (sv == null) xCoord = " X"; else
//						xCoord = " " + sv.cost;
//					while (xCoord.length() < numXDigits) xCoord = " " + xCoord;
//					System.out.print(xCoord);
//				}
//			}
//			System.out.println();
//		}
//	}

	private void showSearchVertices(Map<Integer,Map<Integer,SearchVertex>> [] planes, boolean horiz)
	{
		EditWindow_ wnd = Job.getUserInterface().getCurrentEditWindow_();
		for(int i=0; i<numMetalLayers; i++)
		{
			double offset = i;
			offset -= (numMetalLayers-2) / 2.0;
			offset /= numMetalLayers+2;
			Map<Integer,Map<Integer,SearchVertex>> plane = planes[i];
			if (plane == null) continue;
			for(Integer y : plane.keySet())
			{
				double yv = y.doubleValue();
				Map<Integer,SearchVertex> row = plane.get(y);
				for(Integer x : row.keySet())
				{
					double xv = x.doubleValue();
					Point2D pt1, pt2;
					if (horiz)
					{
						pt1 = new Point2D.Double(xv-0.5, yv+offset);
						pt2 = new Point2D.Double(xv+0.5, yv+offset);
					} else
					{
						pt1 = new Point2D.Double(xv+offset, yv-0.5);
						pt2 = new Point2D.Double(xv+offset, yv+0.5);
					}
					wnd.addHighlightLine(pt1, pt2, cell, false);
				}
			}
		}
	}
}
