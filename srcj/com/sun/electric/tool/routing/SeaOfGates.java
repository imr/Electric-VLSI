/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SeaOfGates.java
 * Routing tool: Sea of Gates control
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class to control sea-of-gates routing.
 */
public class SeaOfGates
{
	/**
	 * Method to run Sea-of-Gates routing on user-selected cell
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
			if (ai.getProto() != Generic.tech().unrouted_arc) continue;
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

        // Run seaOfGatesRoute on selected unrouted arcs
        seaOfGatesRoute(netList, netsToRoute);
	}

	/**
	 * Method to run Sea-of-Gates routing on specified cell
	 */
	public static void seaOfGatesRoute(Cell cell) {
        Netlist netList = cell.getUserNetlist();
        
		Set<Network> netsToRoute = new HashSet<Network>();
		for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = it.next();
			if (ai.getProto() != Generic.tech().unrouted_arc) continue;
			netsToRoute.add(netList.getNetwork(ai, 0));
		}
        
        // Run seaOfGatesRoute on unrouted arcs
        seaOfGatesRoute(netList, netsToRoute);
    }
    
	/**
	 * Method to run Sea-of-Gates routing
	 */
	private static void seaOfGatesRoute(Netlist netList, Set<Network> netsToRoute)
	{
		// get cell and network information
		UserInterface ui = Job.getUserInterface();
        Cell cell = netList.getCell();

		// order the nets appropriately
		List<NetsToRoute> orderedNetsToRoute = new ArrayList<NetsToRoute>();
		for(Network net : netsToRoute)
		{
            if (net.getNetlist() != netList)
                throw new IllegalArgumentException("netList");
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
				if (ai.getProto() != Generic.tech().unrouted_arc) continue;
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
        private int[] arcIdsToRoute;

		protected SeaOfGatesJob(Cell cell, List<ArcInst> arcsToRoute)
		{
			super("Sea-Of-Gates Route", Routing.getRoutingTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
            arcIdsToRoute = new int[arcsToRoute.size()];
            for (int i = 0; i < arcsToRoute.size(); i++)
                arcIdsToRoute[i] = arcsToRoute.get(i).getD().arcId;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			SeaOfGatesEngine router = new SeaOfGatesEngine();
            List<ArcInst> arcsToRoute = new ArrayList<ArcInst>();
            for (int arcId: arcIdsToRoute)
                arcsToRoute.add(cell.getArcById(arcId));
			router.routeIt(this, cell, arcsToRoute);
			return true;
		}
	}
}
