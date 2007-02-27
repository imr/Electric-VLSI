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

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.View;
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
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.drc.DRC;

import java.awt.geom.AffineTransform;
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
 * > The router is not tracked: it runs on the Electric grid (all wires are on full-grid units).
 * > Routes power and ground first, then goes by length (shortest nets first)
 *
 * Things to do:
 *     Check via spacings
 *     Ability to route to any previous part of route when daisy-chaining?
 *     Ability to control arc widths
 *     Rip-up?
 */
public class SeaOfGates
{
	/** Cell in which routing occurs. */										private Cell cell;
	/** R-Trees for blockage in the cell. */									private Map<Layer,RTNode> metalTrees;
	/** Maps Arcs to network IDs in the R-Tree. */								private Map<ArcInst,Integer> netIDs;
	/** number of metal layers in the technology. */							private int numMetalLayers;
	/** metal layers in the technology. */										private Layer [] metalLayers;
	/** arcs to use for each metal layer. */									private ArcProto [] metalArcs;
	/** vias to use to go up from each metal layer. */							private PrimitiveNode [] metalVias;
	/** minimum distance from centerline to other metal. */						private double [] metalSpacing;
	/** minimum surround about center when making cut down to this metal. */	private double [] cutUpSpacing;
	/** minimum surround about center when making cut up to this metal. */		private double [] cutDownSpacing;
	/** intermediate seach vertices during Dijkstra search. */					private Map<Integer,Map<Integer,SearchVertex>> [] searchVertexPlanes;
	/** destination coordinate for the current Dijkstra path. */				private int toX, toY, toZ;

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
//System.out.println("Routing net "+ntr.net.describe(false)+" which has length "+ntr.length);
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
			SeaOfGates router = new SeaOfGates(cell);
	        long startTime = System.currentTimeMillis();
			router.routeIt(arcsToRoute);
	        long stopTime = System.currentTimeMillis();
	        System.out.println("Routing took " + TextUtils.getElapsedTime(stopTime-startTime));
			return true;
		}
	}

	private SeaOfGates(Cell cell)
	{
		this.cell = cell;
	}

	/************************************** ROUTING **************************************/

	/**
	 * This is the public interface for Sea-of-Gates Routing when done in batch mode.
	 * @param cell the cell to be Sea-of-Gates-routed.
	 * @param arcsToRoute a List of ArcInsts on networks to be routed.
	 */
	public void routeIt(List<ArcInst> arcsToRoute)
	{
		// find the metal layers, arcs, and contacts
		Technology tech = cell.getTechnology();
		numMetalLayers = tech.getNumMetals();
		metalLayers = new Layer[numMetalLayers];
		metalArcs = new ArcProto[numMetalLayers];
		metalVias = new PrimitiveNode[numMetalLayers-1];
		for(Iterator<Layer> it = tech.getLayers(); it.hasNext(); )
		{
			Layer lay = it.next();
			if (!lay.getFunction().isMetal()) continue;
			if ((lay.getFunctionExtras()&Layer.Function.PSEUDO) != 0) continue;
			int layerIndex = lay.getFunction().getLevel()-1;
			if (layerIndex < numMetalLayers) metalLayers[layerIndex] = lay;
		}
		for(Iterator<ArcProto> it = tech.getArcs(); it.hasNext(); )
		{
			ArcProto ap = it.next();
			for(int i=0; i<numMetalLayers; i++)
			{
				if (ap.getArcLayer(0).getLayer() == metalLayers[i])
				{
					metalArcs[i] = ap;
					break;
				}
			}
		}
		for(Iterator<PrimitiveNode> it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode np = it.next();
			if (np.getFunction() != PrimitiveNode.Function.CONTACT) continue;
			ArcProto [] conns = np.getPort(0).getConnections();
			for(int i=0; i<numMetalLayers-1; i++)
			{
				if ((conns[0] == metalArcs[i] && conns[1] == metalArcs[i+1]) ||
					(conns[1] == metalArcs[i] && conns[0] == metalArcs[i+1]))
				{
					if (metalVias[i] == null) metalVias[i] = np;
					break;
				}
			}
		}
		for(int i=0; i<numMetalLayers; i++)
		{
			if (metalLayers[i] == null)
			{
				System.out.println("ERROR: Cannot find layer for Metal " + (i+1));
				return;
			}
			if (metalArcs[i] == null)
			{
				System.out.println("ERROR: Cannot find arc for Metal " + (i+1));
				return;
			}
			if (i < numMetalLayers-1 && metalVias[i] == null)
			{
				System.out.println("ERROR: Cannot find contact between Metal " + (i+1) + " and Metal " + (i+2));
				return;
			}
		}

		// compute design rule spacings
		metalSpacing = new double[numMetalLayers];
		cutUpSpacing = new double[numMetalLayers];
		cutDownSpacing = new double[numMetalLayers];
		double [] layerSurround = new double[numMetalLayers];
		for(int i=0; i<numMetalLayers; i++)
		{
			Layer lay = metalLayers[i];
			layerSurround[i] = 1;
			DRCTemplate rule = DRC.getSpacingRule(lay, null, lay, null, false, -1, 10, 100);
			if (rule != null) layerSurround[i] = rule.getValue(0);
			metalSpacing[i] = metalArcs[i].getDefaultLambdaBaseWidth() / 2 + layerSurround[i];
		}
		for(int i=0; i<numMetalLayers-1; i++)
		{
			PrimitiveNode np = metalVias[i];
			NodeInst dummyNi = NodeInst.makeDummyInstance(np);
			PolyBase [] polys = tech.getShapeOfNode(dummyNi);
			for(int j=0; j<polys.length; j++)
			{
				Rectangle2D rect = polys[j].getBounds2D();
				double maxSpacing = Math.max(Math.max(Math.abs(rect.getMinX()), Math.abs(rect.getMaxX())),
					Math.max(Math.abs(rect.getMinY()), Math.abs(rect.getMaxY())));
				if (polys[j].getLayer() == metalLayers[i])
				{
					cutUpSpacing[i] = maxSpacing + layerSurround[i];
				} else if (polys[j].getLayer() == metalLayers[i+1])
				{
					cutDownSpacing[i+1] = maxSpacing + layerSurround[i+1];
				}
			}
		}

		// get all blockage information into R-Trees
		System.out.println("Building blockage information...");
		metalTrees = new HashMap<Layer, RTNode>();
		netIDs = new HashMap<ArcInst,Integer>();
		BlockageVisitor visitor = new BlockageVisitor(arcsToRoute);
		HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, visitor);
		addBlockagesAtPorts(arcsToRoute, tech);
//for(Iterator<Layer> it = metalTrees.keySet().iterator(); it.hasNext(); )
//{
//	Layer layer = it.next();
//	RTNode root = metalTrees.get(layer);
//	System.out.println("RTree for "+layer.getName()+" is:");
//	root.printRTree(2);
//}

		// route the networks
		for(ArcInst ai : arcsToRoute)
		{
			Netlist netList = cell.acquireUserNetlist();
			if (netList == null)
			{
				System.out.println("Sorry, a deadlock aborted routing (network information unavailable).  Please try again");
				break;
			}
			Network net = netList.getNetwork(ai, 0);
			System.out.println("Routing network " + net.getName() + "...");
			HashSet<ArcInst> arcsToDelete = new HashSet<ArcInst>();
			HashSet<NodeInst> nodesToDelete = new HashSet<NodeInst>();
			List<Connection> netEnds = Routing.findNetEnds(net, arcsToDelete, nodesToDelete, netList, true);
			List<PortInst> orderedPorts = makeOrderedPorts(net, netEnds);
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

			// find a path between the ends of the network
			boolean allRouted = true;
			int netID = -1;
			Integer netIDI = netIDs.get(ai);
			if (netIDI != null) netID = netIDI.intValue();
			for(int i=1; i<orderedPorts.size(); i++)
			{
				if (!findPath(netID, orderedPorts.get(i-1), orderedPorts.get(i))) allRouted = false;
			}
			if (allRouted)
			{
				// routed: remove the unrouted arcs
				for(ArcInst aiKill : arcsToDelete)
					aiKill.kill();
				for(NodeInst niKill : nodesToDelete)
					niKill.kill();
			}
		}
	}

	/**
	 * Method to add extra blockage information that corresponds to ends of unrouted arcs.
	 * @param arcsToRoute the list of arcs to route.
	 * @param tech the technology to use.
	 */
	private void addBlockagesAtPorts(List<ArcInst> arcsToRoute, Technology tech)
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
				if (lowMetal == highMetal)
				{
					// port on single metal layer: reserve space on layers above and below
					for(int via = lowMetal-2; via < lowMetal; via++)
					{
						if (via < 0 || via >= numMetalLayers-1) continue;
						NodeInst dummy = NodeInst.makeDummyInstance(metalVias[via]);
						PolyBase [] polys = tech.getShapeOfNode(dummy);
						for(int i=0; i<polys.length; i++)
						{
							PolyBase viaPoly = polys[i];
							Layer layer = viaPoly.getLayer();
							if (!layer.getFunction().isMetal()) continue;
							Rectangle2D viaBounds = viaPoly.getBounds2D();
							Rectangle2D bounds = new Rectangle2D.Double(viaBounds.getMinX() + polyBounds.getCenterX(),
								viaBounds.getMinY() + polyBounds.getCenterY(), viaBounds.getWidth(), viaBounds.getHeight());
							if (getBlockage(netID, layer, bounds.getWidth()/2, bounds.getHeight()/2,
								bounds.getCenterX(), bounds.getCenterY()) == null)
							{
								addRectangle(bounds, layer, netID);
//System.out.println("PLACING CONTACT " + metalVias[via].describe(false) + " SO RESERVING SPACE ON "+layer.getName()+" "+
//	bounds.getMinX()+"<=X<="+bounds.getMaxX()+" and "+bounds.getMinY()+"<=Y<="+bounds.getMaxY());
							}
						}
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
	 * @return true if routed, false if it failed.
	 */
	private boolean findPath(int netID, PortInst fromPi, PortInst toPi)
	{
		// get information about one end of the path
		ArcProto fromArc = null;
		ArcProto[] fromArcs = fromPi.getPortProto().getBasePort().getConnections();
		for(int i=0; i<fromArcs.length; i++)
			if (fromArcs[i].getFunction().isMetal()) { fromArc = fromArcs[i];   break; }
		if (fromArc == null)
		{
			System.out.println("ERROR: Cannot connect port " + fromPi.getPortProto().getName() +
				" of node " + fromPi.getNodeInst().describe(false) + " to port " + toPi.getPortProto().getName() +
				" of node " + toPi.getNodeInst().describe(false) + " because the first port has no metal connection");
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
			System.out.println("ERROR: Cannot connect port " + fromPi.getPortProto().getName() +
				" of node " + fromPi.getNodeInst().describe(false) + " to port " + toPi.getPortProto().getName() +
				" of node " + toPi.getNodeInst().describe(false) + " because the second port has no metal connection");
			return false;
		}
		EPoint toLoc = toPi.getPoly().getCenter();

		// determine the unit coordinates of the route
		int fromX, fromY;
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
		toZ = toArc.getFunction().getLevel()-1;

		if (fromArc.getTechnology() != cell.getTechnology() ||
			toArc.getTechnology() != cell.getTechnology())
		{
			System.out.println("Route from port " + fromPi.getPortProto().getName() + " of node " + fromPi.getNodeInst().describe(false) +
				" on arc " + fromArc.describe() + " cannot connect to port " + toPi.getPortProto().getName() + " of node " +
				toPi.getNodeInst().describe(false) + " on arc " + toArc.describe());
			return false;
		}

		// do the Dijkstra
		List<SearchVertex> vertices = doDijkstra(fromX, fromY, fromZ, netID);
		if (vertices == null)
		{
			System.out.println("ERROR: Failed to route from port " + fromPi.getPortProto().getName() +
				" of node " + fromPi.getNodeInst().describe(false) + " to port " + toPi.getPortProto().getName() +
				" of node " + toPi.getNodeInst().describe(false));
			return false;
		}
//System.out.println("Found "+vertices.size()+" points in path:");
//for(SearchVertex sv : vertices)
//	System.out.println("Metal " + (sv.z+1) + " at ("+sv.x+","+sv.y+")");
		PortInst lastPort = toPi;
		PolyBase toPoly = toPi.getPoly();
		if (toPoly.getCenterX() != toX || toPoly.getCenterY() != toY)
		{
			// end of route is off-grid: adjust it
			if (vertices.size() >= 2)
			{
				SearchVertex v1 = vertices.get(0);
				SearchVertex v2 = vertices.get(1);
				if (v1.x == v2.x)
				{
					// first line is vertical: run a horizontal bit
					PrimitiveNode np = metalArcs[toZ].findPinProto();
					NodeInst ni = makeNodeInst(np, new EPoint(v1.x, toPoly.getCenterY()), np.getDefWidth(), np.getDefHeight(), cell, netID);
					ArcProto type = metalArcs[toZ];
					ArcInst ai = makeArcInst(type, type.getDefaultLambdaFullWidth(), ni.getOnlyPortInst(), toPi, netID);
					lastPort = ni.getOnlyPortInst();
				} else if (v1.y == v2.y)
				{
					// first line is horizontal: run a vertical bit
					PrimitiveNode np = metalArcs[toZ].findPinProto();
					NodeInst ni = makeNodeInst(np, new EPoint(toPoly.getCenterX(), v1.y), np.getDefWidth(), np.getDefHeight(), cell, netID);
					ArcProto type = metalArcs[toZ];
					ArcInst ai = makeArcInst(type, type.getDefaultLambdaFullWidth(), ni.getOnlyPortInst(), toPi, netID);
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
				if (sv.x != svNext.x || sv.y != svNext.y || sv.z == svNext.z) break;
				PrimitiveNode np = metalVias[Math.min(sv.z, svNext.z)];
				NodeInst ni = makeNodeInst(np, new EPoint(sv.x, sv.y), np.getDefWidth(), np.getDefHeight(), cell, netID);
				ArcProto type = metalArcs[sv.z];
				ArcInst ai = makeArcInst(type, type.getDefaultLambdaFullWidth(), lastPort, ni.getOnlyPortInst(), netID);
				lastPort = ni.getOnlyPortInst();
				madeContacts = true;
				sv = svNext;
				i++;
			}
			if (madeContacts) continue;

			PrimitiveNode np = metalArcs[sv.z].findPinProto();
			PortInst pi = null;
			if (i == vertices.size()-1)
			{
				pi = fromPi;
				PolyBase fromPoly = fromPi.getPoly();
				if (fromPoly.getCenterX() != sv.x || fromPoly.getCenterY() != sv.y)
				{
					// end of route is off-grid: adjust it
					if (vertices.size() >= 2)
					{
						SearchVertex v1 = vertices.get(vertices.size()-2);
						SearchVertex v2 = vertices.get(vertices.size()-1);
						if (v1.x == v2.x)
						{
							// last line is vertical: run a horizontal bit
							PrimitiveNode pNp = metalArcs[fromZ].findPinProto();
							NodeInst ni = makeNodeInst(pNp, new EPoint(v1.x, fromPoly.getCenterY()), np.getDefWidth(), np.getDefHeight(), cell, netID);
							ArcProto type = metalArcs[fromZ];
							ArcInst ai = makeArcInst(type, type.getDefaultLambdaFullWidth(), ni.getOnlyPortInst(), fromPi, netID);
							pi = ni.getOnlyPortInst();
						} else if (v1.y == v2.y)
						{
							// last line is horizontal: run a vertical bit
							PrimitiveNode pNp = metalArcs[fromZ].findPinProto();
							NodeInst ni = makeNodeInst(pNp, new EPoint(fromPoly.getCenterX(), v1.y), np.getDefWidth(), np.getDefHeight(), cell, netID);
							ArcProto type = metalArcs[fromZ];
							ArcInst ai = makeArcInst(type, type.getDefaultLambdaFullWidth(), ni.getOnlyPortInst(), fromPi, netID);
							pi = ni.getOnlyPortInst();
						}
					}
				}
			} else
			{
				NodeInst ni = makeNodeInst(np, new EPoint(sv.x, sv.y), np.getDefWidth(), np.getDefHeight(), cell, netID);
				pi = ni.getOnlyPortInst();
			}
			if (lastPort != null)
			{
				ArcProto type = metalArcs[sv.z];
				ArcInst ai = makeArcInst(type, type.getDefaultLambdaFullWidth(), lastPort, pi, netID);
			}
			lastPort = pi;
		}
		return true;
	}

	/**
	 * Method to create a NodeInst and update the R-Trees.
	 * @param np the prototype of the new NodeInst.
	 * @param loc the location of the new NodeInst.
	 * @param wid the width of the new NodeInst.
	 * @param hei the height of the new NodeInst.
	 * @param cell the Cell in which to place the new NodeInst.
	 * @param netID the network ID of geometry in this NodeInst.
	 * @return the NodeInst that was created (null on error).
	 */
	private NodeInst makeNodeInst(NodeProto np, EPoint loc, double wid, double hei, Cell cell, int netID)
	{
		NodeInst ni = NodeInst.makeInstance(np, loc, wid, hei, cell);
		if (ni != null)
		{
			Poly [] nodeInstPolyList = np.getTechnology().getShapeOfNode(ni, true, false, null);
			for(int i=0; i<nodeInstPolyList.length; i++)
			{
				PolyBase poly = nodeInstPolyList[i];
				if (poly.getPort() == null) continue;
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
		ArcInst ai = ArcInst.makeInstance(type, wid, from, to);
		if (ai != null)
		{
			PolyBase [] polys = type.getTechnology().getShapeOfArc(ai);
			for(int i=0; i<polys.length; i++)
				addLayer(polys[i], GenMath.MATID, netID, false);
		}
		return ai;
	}

	/**
	 * Method to run a Dijkstra search to find a route path.
	 * @param fromX the X coordinate of the start of the search.
	 * @param fromY the Y coordinate of the start of the search.
	 * @param fromZ the Z coordinate (metal layer) of the start of the search.
	 * @param netID the network ID of geometry on this route path.
	 * @return a list of SearchVertex objects that define the path (null if no path found).
	 */
	private List<SearchVertex> doDijkstra(int fromX, int fromY, int fromZ, int netID)
	{
		Rectangle2D bounds = cell.getBounds();
		int lowX = (int)Math.floor(bounds.getMinX());
		int highX = (int)Math.ceil(bounds.getMaxX());
		int lowY = (int)Math.floor(bounds.getMinY());
		int highY = (int)Math.ceil(bounds.getMaxY());
//System.out.println("FINDING PATH FROM ("+fromX+","+fromY+","+fromZ+") TO ("+toX+","+toY+","+toZ+")");
		searchVertexPlanes = new Map[numMetalLayers];

		SearchVertex svStart = new SearchVertex(fromX, fromY, fromZ);
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
			int curX = svCurrent.x;
			int curY = svCurrent.y;
			int curZ = svCurrent.z;
//System.out.println("CONSIDER POINT ("+curX+","+curY+","+curZ+")");

			// look at all directions from this point
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
//System.out.println("  ADVANCE TO ("+(curX+dx)+","+(curY+dy)+","+(curZ+dz)+")");

				// TODO: finish this
				// extend the distance if heading toward the goal
				if (dz == 0)
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
						int hi;
						if (dx != 0) hi = toX - curX; else
							hi = toY - curY;
						while (Math.abs(hi) > 1)
						{
							int cX = curX+dx, cY = curY+dy;
							double halfWid = metalSpacing[curZ], halfHei = metalSpacing[curZ];
							if (dx != 0)
							{
								cX = curX + (dx + hi) / 2;
								halfWid += Math.abs(hi-dx)/2;
							} else
							{
								cY = curY + (dy + hi) / 2;
								halfHei += Math.abs(hi-dy)/2;
							}
							if (getBlockage(netID, metalLayers[curZ], halfWid, halfHei, cX, cY) == null) break;
							hi /= 2;
						}
						if (dx != 0) dx = hi; else dy = hi;
//System.out.println("    EXTENDING TO ("+(curX+dx)+","+(curY+dy)+","+curZ+")");
					}
				}

				int nX = curX + dx;
				int nY = curY + dy;
				int nZ = curZ + dz;
				if (nX < lowX || nX > highX) continue;
				if (nY < lowY || nY > highY) continue;
				if (nZ < 0 || nZ >= numMetalLayers) continue;

				// stop if we found the destination
				if (nX == toX && nY == toY && nZ == toZ) { thread = svCurrent;   break; }

				// see if the adjacent point has already been visited
				SearchVertex svNext = getVertex(nX, nY, nZ);
				if (svNext != null) continue;

				// see if the space is available
				if (dz == 0)
				{
					// running on one layer: check surround
					if (getBlockage(netID, metalLayers[nZ], metalSpacing[nZ], metalSpacing[nZ], nX, nY) != null) continue;
				} else
				{
					int lowMetal = Math.min(curZ, nZ);
					int highMetal = Math.max(curZ, nZ);
					if (getBlockage(netID, metalLayers[lowMetal], cutUpSpacing[lowMetal], cutUpSpacing[lowMetal],
						nX, nY) != null) continue;
					if (getBlockage(netID, metalLayers[highMetal], cutDownSpacing[highMetal], cutDownSpacing[highMetal],
						nX, nY) != null) continue;
				}

				// we have a candidate next-point
				svNext = new SearchVertex(nX, nY, nZ);
				svNext.cost = svCurrent.cost;
				svNext.last = svCurrent;
				if (dx != 0)
				{
					if (toX == curX) svNext.cost += 1; else
						if ((toX-curX) * dx < 0) svNext.cost += 2;
				}
				if (dy != 0)
				{
					if (toY == curY) svNext.cost += 1; else
						if ((toY-curY) * dy < 0) svNext.cost += 2;
				}
				if (dz != 0)
				{
					if (toZ == curZ) svNext.cost += 10; else
						if ((toZ-curZ) * dz < 0) svNext.cost += 20;
				} else
				{
					// not changing layers: penalize if turning in X or Y
					if (svCurrent.last != null)
					{
						int lastDx = svCurrent.x - svCurrent.last.x;
						int lastDy = svCurrent.y - svCurrent.last.y;
						if (lastDx != dx || lastDy != dy) svNext.cost++;
					}
				}
				setVertex(nX, nY, nZ, svNext);
				active.add(svNext);
//System.out.println("  VERTEX ("+curX+","+curY+","+curZ+") to ("+nX+","+nY+","+nZ+") has weight "+svNext.cost);
			}
			if (thread != null) break;
		}
		if (thread != null)
		{
			// found the path!
			List<SearchVertex> realVertices = new ArrayList<SearchVertex>();
			SearchVertex lastVertex = new SearchVertex(toX, toY, toZ);
			realVertices.add(lastVertex);
			while (thread != null)
			{
				if (lastVertex.z != thread.z)
				{
					realVertices.add(thread);
					lastVertex = thread;
					thread = thread.last;
				} else
				{
					// gather a run of vertices on this layer
					int dx = thread.x - lastVertex.x;
					int dy = thread.y - lastVertex.y;
					lastVertex = thread;
					thread = thread.last;
					while (thread != null)
					{
						if (thread.x - lastVertex.x != dx || thread.y - lastVertex.y != dy) break;
						lastVertex = thread;
						thread = thread.last;
					}
					realVertices.add(lastVertex);
				}
			}
			return realVertices;
		}
		return null;
	}

	/************************************** BLOCKAGE DATA STRUCTURE **************************************/

	/**
	 * Class to define an R-Tree leaf node for the blockage data structure.
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

	/**
	 * Method to find a blockage in the R-Tree.
	 * @param netID the network ID of the desired space (blockages on this ID are ignored).
	 * @param layer the layer being examined.
	 * @param halfWidth half of the width of the area needed.
	 * @param halfHeight half of the height of the area needed.
	 * @param x the X coordinate at the center of the area needed.
	 * @param y the Y coordinate at the center of the area needed.
	 * @return a bound object in the area that is a blockage.
	 * Returns null if the space is clear.
	 */
	private SOGBound getBlockage(int netID, Layer layer, double halfWidth, double halfHeight, double x, double y)
	{
		RTNode rtree = metalTrees.get(layer);
		if (rtree == null) return null;

		// see if there is anything in that space
		Rectangle2D searchArea = new Rectangle2D.Double(x-halfWidth, y-halfHeight, halfWidth*2, halfHeight*2);
		for(RTNode.Search sea = new RTNode.Search(searchArea, rtree, true); sea.hasNext(); )
		{
			SOGBound sBound = (SOGBound)sea.next();
			if (sBound.getNetID() == netID) continue;
			return sBound;
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
					addLayer(poly, trans, netID, canPlacePseudo);
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
		Layer layer = poly.getLayer();
		if (canPlacePseudo) layer = layer.getNonPseudoLayer(); else
		{
			if ((layer.getFunctionExtras()&Layer.Function.PSEUDO) != 0) return;
		}
		Layer.Function fun = layer.getFunction();
		if (!fun.isMetal()) return;
		Rectangle2D bounds = poly.getBounds2D();
		GenMath.transformRect(bounds, trans);
		addRectangle(bounds, layer, netID);
    }

    /**
     * Method to add a rectangle to the R-Tree.
     * @param bounds the rectangle to add.
     * @param layer the layer on which to add the rectangle.
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

	/************************************** DIJKSTRA PATH SEARCHING **************************************/

    /**
     * Class to define a vertex in the Dijkstra search.
     */
	private class SearchVertex implements Comparable
	{
		/** the coordinate of the search vertex. */	private int x, y, z;
		/** the cost of search to this vertex. */	private int cost;
		/** the previous vertex in the search. */	private SearchVertex last;

		SearchVertex(int x, int y, int z) { this.x = x;   this.y = y;   this.z = z; }

		/**
		 * Method to sort SearchVertex objects by their cost.
		 */
        public int compareTo(Object svo)
        {
        	SearchVertex sv = (SearchVertex)svo;
        	int diff = cost - sv.cost;
        	if (diff != 0) return diff;
        	int thisDist = Math.abs(x-toX) + Math.abs(y-toY) + Math.abs(z-toZ);
        	int otherDist = Math.abs(sv.x-toX) + Math.abs(sv.y-toY) + Math.abs(sv.z-toZ);
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
		Map<Integer,SearchVertex> row = plane.get(y);
		if (row == null) return null;
		SearchVertex item = row.get(x);
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
		Map<Integer,SearchVertex> row = plane.get(y);
		if (row == null)
		{
			row = new HashMap<Integer,SearchVertex>();
			plane.put(y, row);
		}
		row.put(x, sv);
	}

	/**
	 * Debugging method to dump an R-Tree plane.
	 * @param z the metal layer to dump.
	 */
	private void dumpPlane(int z)
	{
		System.out.println("************************* METAL " + (z+1) + " *************************");
		Map<Integer,Map<Integer,SearchVertex>> plane = searchVertexPlanes[z];
		if (plane == null) return;
		List<Integer> yValues = new ArrayList<Integer>();
		for(Iterator<Integer> it = plane.keySet().iterator(); it.hasNext(); )
			yValues.add(it.next());
		Collections.sort(yValues);
		int lowY = yValues.get(0).intValue();
		int highY = yValues.get(yValues.size()-1).intValue();
		int lowX = 1, highX = 0;
		for(Integer y : yValues)
		{
			Map<Integer,SearchVertex> row = plane.get(y);
			for(Iterator<Integer> it = row.keySet().iterator(); it.hasNext(); )
			{
				Integer x = it.next();
				int xv = x.intValue();
				if (lowX > highX) lowX = highX = xv; else
				{
					if (xv < lowX) lowX = xv;
					if (xv > highX) highX = xv;
				}
			}
		}
		String lowXStr = " " + lowX;
		String highXStr = " " + highX;
		int numXDigits = Math.max(lowXStr.length(), highXStr.length());
		String lowYStr = " " + lowY;
		String highYStr = " " + highY;
		int numYDigits = Math.max(lowYStr.length(), highYStr.length());

		String space = "   ";
		while (space.length() < numYDigits+3) space += " ";
		System.out.print(space);
		for(int x=lowX; x<=highX; x++)
		{
			String xCoord = " " + x;
			while (xCoord.length() < numXDigits) xCoord = " " + xCoord;
			System.out.print(xCoord);
		}
		System.out.println();

		for(int y=highY; y>=lowY; y--)
		{
			String yCoord = " " + y;
			while (yCoord.length() < numYDigits) yCoord = " " + yCoord;
			System.out.print("Row"+yCoord);
			Map<Integer,SearchVertex> row = plane.get(y);
			if (row != null)
			{
				for(int x=lowX; x<=highX; x++)
				{
					SearchVertex sv = row.get(x);
					String xCoord;
					if (sv == null) xCoord = " X"; else
						xCoord = " " + sv.cost;
					while (xCoord.length() < numXDigits) xCoord = " " + xCoord;
					System.out.print(xCoord);
				}
			}
			System.out.println();
		}
	}
}
