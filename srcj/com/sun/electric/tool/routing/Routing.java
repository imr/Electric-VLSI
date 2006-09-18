/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Routing.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellId;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.GenMath.MutableInteger;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.PortProtoId;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.user.User;

import java.awt.event.ActionEvent;
import java.awt.geom.Point2D;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractButton;

/**
 * This is the Routing tool.
 */
public class Routing extends Listener
{
	/**
	 * Class to describe recent activity that pertains to routing.
	 */
	public static class Activity
	{
		int numCreatedArcs, numCreatedNodes;
		ArcInst [] createdArcs;
		NodeInst [] createdNodes;
		int numDeletedArcs, numDeletedNodes;
        ImmutableArcInst deletedArc;
        CellId deletedArcParent;
//		ArcInst [] deletedArcs;
//		NodeInst [] deletedNodes;
		PortProtoId [] deletedPorts;

		Activity()
		{
			numCreatedArcs = numCreatedNodes = 0;
			numDeletedArcs = numDeletedNodes = 0;
			createdArcs = new ArcInst[3];
			createdNodes = new NodeInst[3];
//			deletedArcs = new ArcInst[3];
//			deletedNodes = new NodeInst[2];
			deletedPorts = new PortProtoId[2];
		}
	}

	private Activity current, past = null;
	private boolean checkAutoStitch = false;

	/** the Routing tool. */		private static Routing tool = new Routing();

	/****************************** TOOL INTERFACE ******************************/

	/**
	 * The constructor sets up the Routing tool.
	 */
	private Routing()
	{
		super("routing");
	}

	/**
	 * Method to initialize the Routing tool.
	 */
	public void init()
	{
		setOn();
	}

    /**
     * Method to retrieve the singleton associated with the Routing tool.
     * @return the Routing tool.
     */
    public static Routing getRoutingTool() { return tool; }

//	/**
//	 * Method to announce the start of a batch of changes.
//	 * @param tool the tool that generated the changes.
//	 * @param undoRedo true if these changes are from an undo or redo command.
//	 */
//	public void startBatch(Tool tool, boolean undoRedo)
//	{
//		current = new Activity();
//		checkAutoStitch = false;
//	}

   /**
     * Handles database changes of a Job.
     * @param oldSnapshot database snapshot before Job.
     * @param newSnapshot database snapshot after Job and constraint propagation.
     * @param undoRedo true if Job was Undo/Redo job.
     */
    public void endBatch(Snapshot oldSnapshot, Snapshot newSnapshot, boolean undoRedo)
	{
		if (undoRedo) return;
		if (newSnapshot.tool == tool) return;
        current = new Activity();
        checkAutoStitch = false;
        for (CellId cellId: newSnapshot.getChangedCells(oldSnapshot)) {
            Cell cell = Cell.inCurrentThread(cellId);
            if (cell == null) continue;
            CellBackup oldBackup = oldSnapshot.getCell(cellId);
            if (oldBackup == null) continue; // Don't route in new cells
            if (oldBackup == cell.backup()) continue;
            ArrayList<ImmutableNodeInst> oldNodes = new ArrayList<ImmutableNodeInst>();
            for (ImmutableNodeInst n: oldBackup.nodes) {
                while (n.nodeId >= oldNodes.size()) oldNodes.add(null);
                oldNodes.set(n.nodeId, n);
            }
            ArrayList<ImmutableArcInst> oldArcs = new ArrayList<ImmutableArcInst>();
            for (ImmutableArcInst a: oldBackup.arcs) {
                while (a.arcId >= oldArcs.size()) oldArcs.add(null);
                oldArcs.set(a.arcId, a);
            }
            for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); ) {
                NodeInst ni = it.next();
                ImmutableNodeInst d = ni.getD();
                ImmutableNodeInst oldD = d.nodeId < oldNodes.size() ? oldNodes.get(d.nodeId) : null;
                if (oldD == null) {
                    if (current.numCreatedNodes < 3)
                        current.createdNodes[current.numCreatedNodes++] = ni;
                } else if (oldD != d) {
                    checkAutoStitch = true;
                }
            }
            for (Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); ) {
                ArcInst ai = it.next();
                ImmutableArcInst d = ai.getD();
                ImmutableArcInst oldD = d.arcId < oldArcs.size( ) ? oldArcs.get(d.arcId) : null;
                if (oldD == null) {
                    if (current.numCreatedArcs < 3)
                        current.createdArcs[current.numCreatedArcs++] = ai;
                }
            }
            for (ImmutableArcInst nid: oldBackup.arcs) {
                if (nid == null) continue;
                if (cell.getNodeById(nid.arcId) == null)
                    current.numDeletedNodes++;
            }
            for (ImmutableArcInst aid: oldBackup.arcs) {
                if (aid == null) continue;
                if (cell.getArcById(aid.arcId) == null) {
                    if (current.numDeletedArcs == 0) {
                        current.deletedArc = aid;
                        current.deletedArcParent = cell.getId();
                        current.deletedPorts[0] = aid.headPortId;
                        current.deletedPorts[1] = aid.tailPortId;
                    }
                    current.numDeletedArcs++;
                }
            }
        }
        
		if (current.numCreatedArcs > 0 || current.numCreatedNodes > 0 || current.deletedArc != null)
		{
			past = current;
			if (isMimicStitchOn())
			{
				MimicStitch.mimicStitch(false);
				return;
			}
		}
		if (checkAutoStitch && isAutoStitchOn())
		{
			AutoStitch.autoStitch(false, false);
		}
        // JFluid results
        current = null;
	}

//	/**
//	 * Method to announce a change to a NodeInst.
//	 * @param ni the NodeInst that was changed.
//	 * @param oD the old contents of the NodeInst.
//	 */
//	public void modifyNodeInst(NodeInst ni, ImmutableNodeInst oD)
//	{
//		checkAutoStitch = true;
//	}
//
//	/**
//	 * Method to announce the creation of a new ElectricObject.
//	 * @param obj the ElectricObject that was just created.
//	 */
//	public void newObject(ElectricObject obj)
//	{
//		if (obj instanceof NodeInst)
//		{
//			checkAutoStitch = true;
//			if (current.numCreatedNodes < 3)
//				current.createdNodes[current.numCreatedNodes++] = (NodeInst)obj;
//		} else if (obj instanceof ArcInst)
//		{
//			if (current.numCreatedArcs < 3)
//				current.createdArcs[current.numCreatedArcs++] = (ArcInst)obj;
//		}
//	}
//
//	/**
//	 * Method to announce the deletion of an ElectricObject.
//	 * @param obj the ElectricObject that was just deleted.
//	 */
//	public void killObject(ElectricObject obj)
//	{
//		if (obj instanceof NodeInst)
//		{
//			if (current.numDeletedNodes < 2)
//				current.deletedNodes[current.numDeletedNodes++] = (NodeInst)obj;
//		} else if (obj instanceof ArcInst)
//		{
//			ArcInst ai = (ArcInst)obj;
//            if (current.deletedArc == null) return;
//            current.deletedArc = ai.getD();
////			if (current.numDeletedArcs < 3)
////				current.deletedArcs[current.numDeletedArcs++] = ai;
//            current.deletedArcParent = ai.getParent().getCellId();
//			current.deletedNodes[0] = ai.getHeadPortInst().getNodeInst();
//			current.deletedPorts[0] = ai.getHeadPortInst().getPortProto().getId();
//			current.deletedNodes[1] = ai.getTailPortInst().getNodeInst();
//			current.deletedPorts[1] = ai.getTailPortInst().getPortProto().getId();
//			current.numDeletedNodes = 2;
//		}
//	}

	/****************************** COMMANDS ******************************/

	/**
	 * Method to mimic the currently selected ArcInst.
	 */
	public void mimicSelected()
	{
		UserInterface ui = Job.getUserInterface();
		EditWindow_ wnd = ui.getCurrentEditWindow_();
		if (wnd == null) return;

		ArcInst ai = (ArcInst)wnd.getOneElectricObject(ArcInst.class);
		if (ai == null) return;
		past = new Activity();
		past.createdArcs[past.numCreatedArcs++] = ai;
		MimicStitch.mimicStitch(false);
	}

	/**
	 * Method to convert the current network(s) to an unrouted wire.
	 */
	public static void unrouteCurrent()
	{
		// see what is highlighted
		UserInterface ui = Job.getUserInterface();
		EditWindow_ wnd = ui.getCurrentEditWindow_();
		if (wnd == null) return;
		Cell cell = wnd.getCell();
		Set<Network> nets = wnd.getHighlightedNetworks();
		if (nets.size() == 0)
		{
			System.out.println("Must select networks to unroute");
			return;
		}

		new UnrouteJob(cell, nets);
	}

	/**
	 * Method to determine the preferred ArcProto to use for routing.
	 * Examines preferences in the Routing tool and User interface.
	 * @return the preferred ArcProto to use for routing.
	 */
	public static ArcProto getPreferredRoutingArcProto()
	{
		ArcProto preferredArc = null;
		String preferredName = getPreferredRoutingArc();
		if (preferredName.length() > 0) preferredArc = ArcProto.findArcProto(preferredName);
		if (preferredArc == null)
		{
			// see if there is a default user arc
			ArcProto curAp = User.getUserTool().getCurrentArcProto();
			if (curAp != null) preferredArc = curAp;
		}
		return preferredArc;
	}

	private static class UnrouteJob extends Job
	{
		private Cell cell;
		private Set<Network> nets;
		private List<ArcInst> highlightThese;

		private UnrouteJob(Cell cell, Set<Network> nets)
		{
			super("Unroute", Routing.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.nets = nets;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			// convert requested nets
			Netlist netList = cell.acquireUserNetlist();
			if (netList == null)
				throw new JobException("Sorry, a deadlock aborted unrouting (network information unavailable).  Please try again");
			highlightThese = new ArrayList<ArcInst>();

			// make arrays of what to unroute
			int total = nets.size();
			Network [] netsToUnroute = new Network[total];
			ArrayList<List<Connection>> netEnds = new ArrayList<List<Connection>>();
			ArrayList<HashSet<ArcInst>> arcsToDelete = new ArrayList<HashSet<ArcInst>>();
			ArrayList<HashSet<NodeInst>> nodesToDelete = new ArrayList<HashSet<NodeInst>>();
			int i = 0;
			for(Network net : nets)
			{
				netsToUnroute[i] = net;
                HashSet<ArcInst> arcs = new HashSet<ArcInst>();
                HashSet<NodeInst> nodes = new HashSet<NodeInst>();
				arcsToDelete.add(arcs);
				nodesToDelete.add(nodes);
				netEnds.add(findNetEnds(net, arcs, nodes, netList));
				i++;
			}

			// do the unrouting
			for(int j=0; j<total; j++)
			{
				if (unrouteNet(netsToUnroute[j], arcsToDelete.get(j), nodesToDelete.get(j), netEnds.get(j), netList, highlightThese)) break;
			}
			fieldVariableChanged("highlightThese");
			return true;
		}

        public void terminateOK()
        {
    		UserInterface ui = Job.getUserInterface();
    		EditWindow_ wnd = ui.getCurrentEditWindow_();
    		if (wnd == null) return;
			wnd.clearHighlighting();
			for(ArcInst ai : highlightThese)
				wnd.addElectricObject(ai, ai.getParent());
			wnd.finishedHighlighting();
        }

		private static boolean unrouteNet(Network net, HashSet<ArcInst> arcsToDelete, HashSet<NodeInst> nodesToDelete,
			List<Connection> netEnds, Netlist netList, List<ArcInst> highlightThese)
		{
			// remove marked nodes and arcs
			for(ArcInst ai : arcsToDelete)
			{
				ai.kill();
			}
			for(NodeInst ni : nodesToDelete)
			{
				ni.kill();
			}

			// now create the new unrouted wires
			double wid = Generic.tech.unrouted_arc.getDefaultWidth();
			int count = netEnds.size();
			int [] covered = new int[count];
			Point2D [] points = new Point2D[count];
			for(int i=0; i<count; i++)
			{
				Connection con = (Connection)netEnds.get(i);
				PortInst pi = con.getPortInst();
				Poly poly = pi.getPoly();
				points[i] = new Point2D.Double(poly.getCenterX(), poly.getCenterY());
				covered[i] = 0;
			}
			for(int first=0; ; first++)
			{
				boolean found = true;
				double bestdist = 0;
				int besti = 0, bestj = 0;
				for(int i=0; i<count; i++)
				{
					for(int j=i+1; j<count; j++)
					{
						if (first != 0)
						{
							if (covered[i] + covered[j] != 1) continue;
						}
						double dist = points[i].distance(points[j]);

						if (!found && dist >= bestdist) continue;
						found = false;
						bestdist = dist;
						besti = i;
						bestj = j;
					}
				}
				if (found) break;

				covered[besti] = covered[bestj] = 1;
				PortInst head = ((Connection)netEnds.get(besti)).getPortInst();
				PortInst tail = ((Connection)netEnds.get(bestj)).getPortInst();
				ArcInst ai = ArcInst.makeInstance(Generic.tech.unrouted_arc, wid, head, tail);
				if (ai == null)
				{
					System.out.println("Could not create unrouted arc");
					return true;
				}
				highlightThese.add(ai);
			}
			return false;
		}
	}

	/**
	 * Method to find the endpoints of a network.
	 * @param net the network to "unroute".
	 * @param arcsToDelete a HashSet of arcs that should be deleted.
	 * @param nodesToDelete a HashSet of nodes that should be deleted.
	 * @param netList the netlist for the current cell.
	 * @return a List of Connection (PortInst/Point2D pairs) that should be wired together.
	 */
	public static List<Connection> findNetEnds(Network net, HashSet<ArcInst> arcsToDelete, HashSet<NodeInst> nodesToDelete, Netlist netList)
	{
		// initialize
		Cell cell = net.getParent();
		List<Connection> endList = new ArrayList<Connection>();

		// look at every arc and see if it is part of the network
		for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = it.next();
			Network aNet = netList.getNetwork(ai, 0);
			if (aNet != net) continue;
			arcsToDelete.add(ai);

			// see if an end of the arc is a network "end"
			for(int i=0; i<2; i++)
			{
				Connection thisCon = ai.getConnection(i);
				NodeInst ni = thisCon.getPortInst().getNodeInst();
				boolean term = true;
				for(Iterator<Connection> cIt = ni.getConnections(); cIt.hasNext(); )
				{
					Connection con = cIt.next();
					if (!con.equals(thisCon) && netList.getNetwork(con.getArc(), 0) == net) { term = false;   break; }
				}
				if (ni.hasExports()) term = true;
//				if (ni.getNumExports() > 0) term = true;
				if (ni.isCellInstance()) term = true;
				if (term)
				{
					// valid network end: see if it is in the list
					if (!endList.contains(thisCon))
						endList.add(thisCon);
				} else
				{
					// not a network end: mark the node for removal
					nodesToDelete.add(ni);
				}
			}
		}
		return endList;
	}

	/**
	 * Method to return the most recent routing activity.
	 */
	public Activity getLastActivity() { return past; }

	/****************************** SUN ROUTER INTERFACE ******************************/

    private static boolean sunRouterChecked = false;
	private static Class sunRouterClass = null;
	private static Method sunRouterMethod;

	/**
	 * Method to tell whether the Sun Router is available.
	 * This is a proprietary tool from Sun Microsystems.
	 * This method dynamically figures out whether the router is present by using reflection.
	 * @return true if the Sun Router is available.
	 */
	public static boolean hasSunRouter()
	{
		if (!sunRouterChecked)
		{
			sunRouterChecked = true;

			// find the Sun Router class
			try
			{
				sunRouterClass = Class.forName("com.sun.electric.plugins.sunRouter.SunRouter");
			} catch (ClassNotFoundException e)
			{
				sunRouterClass = null;
				return false;
			}

			// find the necessary method on the router class
			try
			{
				sunRouterMethod = sunRouterClass.getMethod("routeCell", new Class[] {Cell.class});
			} catch (NoSuchMethodException e)
			{
				sunRouterClass = null;
				return false;
			}
		}

		// if already initialized, return
		if (sunRouterClass == null) return false;
	 	return true;
	}

	/**
	 * Method to invoke the Sun Router via reflection.
	 */
	public static void sunRouteCurrentCell()
	{
		if (!hasSunRouter()) return;
		UserInterface ui = Job.getUserInterface();
		Cell curCell = ui.needCurrentCell();
		if (curCell == null) return;
		try
		{
			sunRouterMethod.invoke(sunRouterClass, new Object[] {curCell});
		} catch (Exception e)
		{
			System.out.println("Unable to run the Sun Router module");
            e.printStackTrace(System.out);
		}
	}

	/****************************** COPY / PASTE ROUTING TOPOLOGY ******************************/

	private static Cell copiedTopologyCell;

	/**
	 * Method called when the "Copy Routing Topology" command is issued.
	 * It remembers the topology in the current cell.
	 */
	public static void copyRoutingTopology()
	{
		UserInterface ui = Job.getUserInterface();
		Cell np = ui.needCurrentCell();
		if (np == null) return;
		copiedTopologyCell = np;
		System.out.println("Cell " + np.describe(true) + " will have its connectivity remembered");
	}

	/**
	 * Method called when the "Paste Routing Topology" command is issued.
	 * It applies the topology of the "copied" cell to the current cell.
	 */
	public static void pasteRoutingTopology()
	{
		if (copiedTopologyCell == null)
		{
			System.out.println("Must copy topology before pasting it");
			return;
		}

		// first validate the source cell
		if (!copiedTopologyCell.isLinked())
		{
			System.out.println("Copied cell is no longer valid");
			return;
		}

		// get the destination cell
		UserInterface ui = Job.getUserInterface();
		Cell toCell = ui.needCurrentCell();
		if (toCell == null) return;

		// make sure copy goes to a different cell
		if (copiedTopologyCell == toCell)
		{
			System.out.println("Topology must be copied to a different cell");
			return;
		}

		// do the copy
		new CopyRoutingTopology(copiedTopologyCell, toCell);
	}

	/**
	 * Class to delete a cell in a new thread.
	 */
	private static class CopyRoutingTopology extends Job
	{
		private Cell fromCell, toCell;

		protected CopyRoutingTopology(Cell fromCell, Cell toCell)
		{
			super("Copy Routing Topology", Routing.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.fromCell = fromCell;
			this.toCell = toCell;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			return copyTopology(fromCell, toCell);
		}
	}

	/**
	 * Method to copy the routing topology from one cell to another.
	 * @param fromCell the source of the routing topology.
	 * @param toCell the destination cell for the routing topology.
	 * @return true if successful.
	 */
	public static boolean copyTopology(Cell fromCell, Cell toCell)
	{
		System.out.println("Copying topology of " + fromCell + " to " + toCell);
		int wiresMade = 0;

		// reset association pointers in the destination cell
		HashMap<NodeInst,NodeInst> nodesAssoc = new HashMap<NodeInst,NodeInst>();

		// look for associations
		for(Iterator<NodeInst> it = toCell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (ni.getProto() == Generic.tech.cellCenterNode || ni.getProto() == Generic.tech.essentialBoundsNode) continue;
			if (nodesAssoc.get(ni) != null) continue;

			// ignore connecting nodes
			PrimitiveNode.Function fun = null;
			if (!ni.isCellInstance())
			{
				fun = ni.getFunction();
				if (fun == PrimitiveNode.Function.UNKNOWN || fun == PrimitiveNode.Function.PIN ||
					fun == PrimitiveNode.Function.CONTACT || fun == PrimitiveNode.Function.NODE)
				{
					if (ni.hasExports())
//					if (ni.getNumExports() > 0)
					{
						// an export on a simple node: find the equivalent
						for(Iterator<Export> eIt = ni.getExports(); eIt.hasNext(); )
						{
							Export e = eIt.next();
							Export fromE = fromCell.findExport(e.getName());
							if (fromE != null)
							{
								nodesAssoc.put(ni, fromE.getOriginalPort().getNodeInst());
								break;
							}
						}
					}
					continue;
				}
			}

			// count the number of this type of node in the two cells
			List<NodeInst> fromList = new ArrayList<NodeInst>();
			for(Iterator<NodeInst> nIt = fromCell.getNodes(); nIt.hasNext(); )
			{
				NodeInst oNi = nIt.next();
				if (ni.isCellInstance())
				{
					if (((Cell)oNi.getProto()).getCellGroup() == ((Cell)ni.getProto()).getCellGroup()) fromList.add(oNi);
				} else
				{
					PrimitiveNode.Function oFun = oNi.getFunction();
					if (oFun == fun) fromList.add(oNi);
				}
			}
			List<NodeInst> toList = new ArrayList<NodeInst>();
			for(Iterator<NodeInst> nIt = toCell.getNodes(); nIt.hasNext(); )
			{
				NodeInst oNi = nIt.next();
				if (ni.isCellInstance())
				{
					if (oNi.getProto() == ni.getProto()) toList.add(oNi);
				} else
				{
					PrimitiveNode.Function oFun = oNi.getFunction();
					if (oFun == fun) toList.add(oNi);
				}
			}

			// problem if the numbers don't match
			if (toList.size() != fromList.size())
			{
				if (fromList.size() == 0) continue;
				System.out.println("Warning: " + fromCell + " has " + fromList.size() + " of " + ni.getProto() +
					" but " + toCell + " has " + toList.size());
				return false;
			}

			// look for name matches
			List<NodeInst> copyList = new ArrayList<NodeInst>();
			for(NodeInst fNi : fromList) copyList.add(fNi);
			for(NodeInst fNi : copyList)
			{
				String fName = fNi.getName();
				NodeInst matchedNode = null;
				for(NodeInst tNi : toList)
				{
					String tName = tNi.getName();
					if (fName.equals(tName)) { matchedNode = tNi;   break; }
				}
				if (matchedNode != null)
				{
					// name match found: set the association
					nodesAssoc.put(matchedNode, fNi);
					fromList.remove(fNi);
				}
			}

			if (toList.size() != fromList.size())
			{
				System.out.println("Error: after name match, there are " + fromList.size() +
					" instances of " + ni.getProto() + " in source and " + toList.size() + " in destination");
				return false;
			}

			// sort the rest by position and force matches based on that
			if (fromList.size() == 0) continue;
			Collections.sort(fromList, new InstacesSpatially());
			Collections.sort(toList, new InstacesSpatially());
			for(int i=0; i<Math.min(toList.size(), fromList.size()); i++)
			{
				NodeInst tNi = (NodeInst)toList.get(i);
				NodeInst fNi = (NodeInst)fromList.get(i);
				nodesAssoc.put(tNi, fNi);
			}
		}

		// association made, now copy the topology
		Netlist fNl = fromCell.acquireUserNetlist();
		Netlist tNl = toCell.acquireUserNetlist();
		if (fNl == null || tNl == null)
		{
			System.out.println("Sorry, a deadlock aborted topology copying (network information unavailable).  Please try again");
			return false;
		}
		for(Iterator<NodeInst> tIt = toCell.getNodes(); tIt.hasNext(); )
		{
			NodeInst tNi = tIt.next();
			NodeInst fNi = (NodeInst)nodesAssoc.get(tNi);
			if (fNi == null) continue;

			// look for another node that may match
			for(Iterator<NodeInst> oTIt = toCell.getNodes(); oTIt.hasNext(); )
			{
				NodeInst oTNi = oTIt.next();
				if (tNi == oTNi) continue;
				NodeInst oFNi = (NodeInst)nodesAssoc.get(oTNi);
				if (oFNi == null) continue;

				// see if they share a connection in the original
				PortInst fPi = null;
				PortInst oFPi = null;
				for(Iterator<PortInst> fPIt = fNi.getPortInsts(); fPIt.hasNext(); )
				{
					PortInst pi = fPIt.next();
					Network net = fNl.getNetwork(pi);
					for(Iterator<PortInst> oFPIt = oFNi.getPortInsts(); oFPIt.hasNext(); )
					{
						PortInst oPi = oFPIt.next();
						Network oNet = fNl.getNetwork(oPi);
						if (net == oNet) { fPi = pi;   oFPi = oPi;   break; }
					}
					if (fPi != null) break;
				}
				if (fPi == null) continue;

				// this connection should be repeated in the other cell
				PortProto tPp = tNi.getProto().findPortProto(fPi.getPortProto().getName());
				PortInst tPi = null;
				if (tPp != null) tPi = tNi.findPortInstFromProto(tPp);
				if (tPi == null) continue;

				PortProto oTPp = oTNi.getProto().findPortProto(oFPi.getPortProto().getName());
				PortInst oTPi = null;
				if (oTPp != null) oTPi = oTNi.findPortInstFromProto(oTPp);
				if (oTPi == null) continue;
				// make the connection from "tni", port "tpp" to "otni" port "otpp"
				int result = makeUnroutedConnection(tPi, oTPi, toCell, tNl);
				if (result < 0) return false;
				wiresMade += result;
			}
		}

		// add in any exported but unconnected pins
		for(Iterator<NodeInst> tIt = toCell.getNodes(); tIt.hasNext(); )
		{
			NodeInst tNi = tIt.next();
			if (nodesAssoc.get(tNi) != null) continue;
			if (tNi.isCellInstance()) continue;
			if (!tNi.hasExports()) continue;
//			if (tNi.getNumExports() == 0) continue;
			PrimitiveNode.Function fun = tNi.getFunction();
			if (fun != PrimitiveNode.Function.PIN && fun != PrimitiveNode.Function.CONTACT) continue;

			// find that export in the source cell
			String matchName = (tNi.getExports().next()).getName();
			Network net = null;
			for(Iterator<PortProto> fIt = fromCell.getPorts(); fIt.hasNext(); )
			{
				Export fPp = (Export)fIt.next();
				int width = fNl.getBusWidth(fPp);
				for(int i=0; i<width; i++)
				{
					Network aNet = fNl.getNetwork(fPp, i);
					if (aNet == null) continue;
					if (aNet.toString().equalsIgnoreCase(matchName)) { net = aNet;   break; }
				}
				if (net != null) break;
			}
			if (net == null) continue;

			// check to see if this is connected elsewhere in the "to" cell
			PortInst oFPi = null;
			NodeInst oTNi = null;
			for(Iterator<NodeInst> oTIt = toCell.getNodes(); oTIt.hasNext(); )
			{
				NodeInst ni = oTIt.next();
				NodeInst oFNi = (NodeInst)nodesAssoc.get(ni);
				if (oFNi == null) continue;

				// see if they share a connection in the original
				for(Iterator<PortInst> oFPIt = oFNi.getPortInsts(); oFPIt.hasNext(); )
				{
					PortInst pi = oFPIt.next();
					Network oNet = fNl.getNetwork(pi);
					if (oNet == null) continue;
					if (oNet == net) { oFPi = pi;   break; }
				}
				if (oFPi != null) { oTNi = ni;   break; }
			}
			if (oTNi != null)
			{
				// find the proper port in this cell
				PortProto oTPp = oTNi.getProto().findPortProto(oFPi.getPortProto().getName());
				PortInst oPi = oTNi.findPortInstFromProto(oTPp);
				if (oPi == null) continue;

				// make the connection from "tni", port "tpp" to "otni" port "otpp"
				int result = makeUnroutedConnection(tNi.getPortInst(0), oPi, toCell, tNl);
				if (result < 0) return false;
				wiresMade += result;
			}
		}
		if (wiresMade == 0) System.out.println("No topology was copied"); else
			System.out.println("Created " + wiresMade + " arcs to copy the topology");
		return true;
	}

	/**
	 * Helper method to run an unrouted wire between node "fni", port "fpp" and node "tni", port
	 * "tpp".  If the connection is already there, the routine doesn't make another.
	 * @return number of arcs created (-1 on error).
	 */
	private static int makeUnroutedConnection(PortInst fPi, PortInst tPi, Cell cell, Netlist nl)
	{
		// see if they are already connected
		if (fPi != null && tPi != null)
		{
			Network fNet = nl.getNetwork(fPi);
			Network tNet = nl.getNetwork(tPi);
			if (fNet == tNet) return 0;
		}

		// make the connection from "tni", port "tpp" to "otni" port "otpp"
		Poly fPoly = fPi.getPoly();
		Poly tPoly = tPi.getPoly();
		Point2D fPt = new Point2D.Double(fPoly.getCenterX(), fPoly.getCenterY());
		Point2D tPt = new Point2D.Double(tPoly.getCenterX(), tPoly.getCenterY());
		double wid = Generic.tech.unrouted_arc.getDefaultWidth();
		ArcInst ai = ArcInst.makeInstance(Generic.tech.unrouted_arc, wid, fPi, tPi, fPt, tPt, null);
		if (ai == null) return -1;
		return 1;
	}

	private static class InstacesSpatially implements Comparator<NodeInst>
	{
		public int compare(NodeInst n1, NodeInst n2)
		{
			double x1 = n1.getAnchorCenterX();
			double y1 = n1.getAnchorCenterY();
			double x2 = n2.getAnchorCenterX();
			double y2 = n2.getAnchorCenterY();
			if (y1 == y2)
			{
				if (x1 == x2) return 0;
				if (x1 > x2) return 1;
				return -1;
			}
			if (y1 == y2) return 0;
			if (y1 > y2) return 1;
			return -1;
		}
	}

	/****************************** GENERAL ROUTING OPTIONS ******************************/

	private static Pref cachePreferredRoutingArc = Pref.makeStringPref("PreferredRoutingArc", Routing.tool.prefs, "");
	/**
	 * Method to return the name of the arc that should be used as a default by the stitching routers.
	 * The default is "".
	 * @return the name of the arc that should be used as a default by the stitching routers.
	 */
	public static String getPreferredRoutingArc() { return cachePreferredRoutingArc.getString(); }
	/**
	 * Method to set the name of the arc that should be used as a default by the stitching routers.
	 * @param arcName the name of the arc that should be used as a default by the stitching routers.
	 */
	public static void setPreferredRoutingArc(String arcName) { cachePreferredRoutingArc.setString(arcName); }

	/****************************** AUTO-STITCHING OPTIONS ******************************/

	private static Pref cacheAutoStitchOn = Pref.makeBooleanPref("AutoStitchOn", Routing.tool.prefs, false);
	/**
	 * Method to tell whether Auto-stitching should be done.
	 * The default is "false".
	 * @return true if Auto-stitching should be done.
	 */
	public static boolean isAutoStitchOn() { return cacheAutoStitchOn.getBoolean(); }
	/**
	 * Method to set whether Auto-stitching should be done.
	 * @param on true if Auto-stitching should be done.
	 */
	public static void setAutoStitchOn(boolean on) { cacheAutoStitchOn.setBoolean(on); }

	/****************************** MIMIC-STITCHING OPTIONS ******************************/

	private static Pref cacheMimicStitchOn = Pref.makeBooleanPref("MimicStitchOn", Routing.tool.prefs, false);
	/**
	 * Method to tell whether Mimic-stitching should be done.
	 * The default is "false".
	 * @return true if Mimic-stitching should be done.
	 */
	public static boolean isMimicStitchOn() { return cacheMimicStitchOn.getBoolean(); }
	/**
	 * Method to set whether Mimic-stitching should be done.
	 * @param on true if Mimic-stitching should be done.
	 */
	public static void setMimicStitchOn(boolean on) { cacheMimicStitchOn.setBoolean(on); }

//	private static Pref cacheMimicStitchCanUnstitch = Pref.makeBooleanPref("MimicStitchCanUnstitch", Routing.tool.prefs, false);
//	/**
//	 * Method to tell whether Mimic-stitching can remove arcs (unstitch).
//	 * The default is "false".
//	 * @return true if Mimic-stitching can remove arcs (unstitch).
//	 */
//	public static boolean isMimicStitchCanUnstitch() { return cacheMimicStitchCanUnstitch.getBoolean(); }
//	/**
//	 * Method to set whether Mimic-stitching can remove arcs (unstitch).
//	 * @param on true if Mimic-stitching can remove arcs (unstitch).
//	 */
//	public static void setMimicStitchCanUnstitch(boolean on) { cacheMimicStitchCanUnstitch.setBoolean(on); }

	private static Pref cacheMimicStitchInteractive = Pref.makeBooleanPref("MimicStitchInteractive", Routing.tool.prefs, false);
	/**
	 * Method to tell whether Mimic-stitching runs interactively.
	 * During interactive Mimic stitching, each new set of arcs is shown to the user for confirmation.
	 * The default is "false".
	 * @return true if Mimic-stitching runs interactively.
	 */
	public static boolean isMimicStitchInteractive() { return cacheMimicStitchInteractive.getBoolean(); }
	/**
	 * Method to set whether Mimic-stitching runs interactively.
	 * During interactive Mimic stitching, each new set of arcs is shown to the user for confirmation.
	 * @param on true if Mimic-stitching runs interactively.
	 */
	public static void setMimicStitchInteractive(boolean on) { cacheMimicStitchInteractive.setBoolean(on); }

    private static Pref cacheMimicStitchPinsKept = Pref.makeBooleanPref("MimicStitchPinsKept", Routing.tool.prefs, false);
	/**
	 * Method to tell whether Mimic-stitching keeps pins even if it has no arc connections.
	 * The default is "false".
	 * @return true if Mimic-stitching runs interactively.
	 */
	public static boolean isMimicStitchPinsKept() { return cacheMimicStitchPinsKept.getBoolean(); }
	/**
	 * Method to set whether Mimic-stitching keeps pins even if it has no arc connections.
	 * @param on true if Mimic-stitching runs interactively.
	 */
	public static void setMimicStitchPinsKept(boolean on) { cacheMimicStitchPinsKept.setBoolean(on); }

	private static Pref cacheMimicStitchMatchPorts = Pref.makeBooleanPref("MimicStitchMatchPorts", Routing.tool.prefs, false);
	/**
	 * Method to tell whether Mimic-stitching only works between matching ports.
	 * The default is "false".
	 * @return true if Mimic-stitching only works between matching ports.
	 */
	public static boolean isMimicStitchMatchPorts() { return cacheMimicStitchMatchPorts.getBoolean(); }
	/**
	 * Method to set whether Mimic-stitching only works between matching ports.
	 * @param on true if Mimic-stitching only works between matching ports.
	 */
	public static void setMimicStitchMatchPorts(boolean on) { cacheMimicStitchMatchPorts.setBoolean(on); }

	private static Pref cacheMimicStitchMatchPortWidth = Pref.makeBooleanPref("MimicStitchMatchPortWidth", Routing.tool.prefs, true);
	/**
	 * Method to tell whether Mimic-stitching only works between ports of the same width.
	 * This applies only in the case of busses.
	 * The default is "true".
	 * @return true if Mimic-stitching only works between matching ports.
	 */
	public static boolean isMimicStitchMatchPortWidth() { return cacheMimicStitchMatchPortWidth.getBoolean(); }
	/**
	 * Method to set whether Mimic-stitching only works between ports of the same width.
	 * This applies only in the case of busses.
	 * @param on true if Mimic-stitching only works between ports of the same width.
	 */
	public static void setMimicStitchMatchPortWidth(boolean on) { cacheMimicStitchMatchPortWidth.setBoolean(on); }

	private static Pref cacheMimicStitchMatchNumArcs = Pref.makeBooleanPref("MimicStitchMatchNumArcs", Routing.tool.prefs, false);
	/**
	 * Method to tell whether Mimic-stitching only works when the number of existing arcs matches.
	 * The default is "false".
	 * @return true if Mimic-stitching only works when the number of existing arcs matches.
	 */
	public static boolean isMimicStitchMatchNumArcs() { return cacheMimicStitchMatchNumArcs.getBoolean(); }
	/**
	 * Method to set whether Mimic-stitching only works when the number of existing arcs matches.
	 * @param on true if Mimic-stitching only works when the number of existing arcs matches.
	 */
	public static void setMimicStitchMatchNumArcs(boolean on) { cacheMimicStitchMatchNumArcs.setBoolean(on); }

	private static Pref cacheMimicStitchMatchNodeSize = Pref.makeBooleanPref("MimicStitchMatchNodeSize", Routing.tool.prefs, false);
	/**
	 * Method to tell whether Mimic-stitching only works when the node sizes are the same.
	 * The default is "false".
	 * @return true if Mimic-stitching only works when the node sizes are the same.
	 */
	public static boolean isMimicStitchMatchNodeSize() { return cacheMimicStitchMatchNodeSize.getBoolean(); }
	/**
	 * Method to set whether Mimic-stitching only works when the node sizes are the same.
	 * @param on true if Mimic-stitching only works when the node sizes are the same.
	 */
	public static void setMimicStitchMatchNodeSize(boolean on) { cacheMimicStitchMatchNodeSize.setBoolean(on); }

	private static Pref cacheMimicStitchMatchNodeType = Pref.makeBooleanPref("MimicStitchMatchNodeType", Routing.tool.prefs, true);
	/**
	 * Method to tell whether Mimic-stitching only works when the nodes have the same type.
	 * The default is "true".
	 * @return true if Mimic-stitching only works when the nodes have the same type.
	 */
	public static boolean isMimicStitchMatchNodeType() { return cacheMimicStitchMatchNodeType.getBoolean(); }
	/**
	 * Method to set whether Mimic-stitching only works when the nodes have the same type.
	 * @param on true if Mimic-stitching only works when the nodes have the same type.
	 */
	public static void setMimicStitchMatchNodeType(boolean on) { cacheMimicStitchMatchNodeType.setBoolean(on); }

	private static Pref cacheMimicStitchNoOtherArcsSameDir = Pref.makeBooleanPref("MimicStitchNoOtherArcsSameDir", Routing.tool.prefs, true);
	/**
	 * Method to tell whether Mimic-stitching only works when there are no other arcs running in the same direction.
	 * The default is "true".
	 * @return true if Mimic-stitching only works when there are no other arcs running in the same direction.
	 */
	public static boolean isMimicStitchNoOtherArcsSameDir() { return cacheMimicStitchNoOtherArcsSameDir.getBoolean(); }
	/**
	 * Method to set whether Mimic-stitching only works when there are no other arcs running in the same direction.
	 * @param on true if Mimic-stitching only works when there are no other arcs running in the same direction.
	 */
	public static void setMimicStitchNoOtherArcsSameDir(boolean on) { cacheMimicStitchNoOtherArcsSameDir.setBoolean(on); }

	/****************************** SUN ROUTER OPTIONS ******************************/

	private static Pref cacheSLRVerboseLevel = Pref.makeIntPref("SunRouterVerboseLevel", Routing.getRoutingTool().prefs, 2);
	/** verbose level can be 0: silent, 1: quiet, 2: normal 3: verbose */
	public static int getSunRouterVerboseLevel() { return cacheSLRVerboseLevel.getInt(); }
	public static void setSunRouterVerboseLevel(int v) { cacheSLRVerboseLevel.setInt(v); }

	private static Pref cacheSLRCostLimit = Pref.makeDoublePref("SunRouterCostLimit", Routing.getRoutingTool().prefs, 10);
	public static double getSunRouterCostLimit() { return cacheSLRCostLimit.getDouble(); }
	public static void setSunRouterCostLimit(double r) { cacheSLRCostLimit.setDouble(r); }

	private static Pref cacheSLRCutlineDeviation = Pref.makeDoublePref("SunRouterCutlineDeviation", Routing.getRoutingTool().prefs, 0.1);
	public static double getSunRouterCutlineDeviation() { return cacheSLRCutlineDeviation.getDouble(); }
	public static void setSunRouterCutlineDeviation(double r) { cacheSLRCutlineDeviation.setDouble(r); }

	private static Pref cacheSLRDelta = Pref.makeDoublePref("SunRouterDelta", Routing.getRoutingTool().prefs, 1);
	public static double getSunRouterDelta() { return cacheSLRDelta.getDouble(); }
	public static void setSunRouterDelta(double r) { cacheSLRDelta.setDouble(r); }

	private static Pref cacheSLRXBitSize = Pref.makeIntPref("SunRouterXBitSize", Routing.getRoutingTool().prefs, 20);
	public static int getSunRouterXBitSize() { return cacheSLRXBitSize.getInt(); }
	public static void setSunRouterXBitSize(int r) { cacheSLRXBitSize.setInt(r); }

	private static Pref cacheSLRYBitSize = Pref.makeIntPref("SunRouterYBitSize", Routing.getRoutingTool().prefs, 20);
	public static int getSunRouterYBitSize() { return cacheSLRYBitSize.getInt(); }
	public static void setSunRouterYBitSize(int r) { cacheSLRYBitSize.setInt(r); }

	private static Pref cacheSLRXTileSize = Pref.makeIntPref("SunRouterXTileSize", Routing.getRoutingTool().prefs, 40);
	public static int getSunRouterXTileSize() { return cacheSLRXTileSize.getInt(); }
	public static void setSunRouterXTileSize(int r) { cacheSLRXTileSize.setInt(r); }

	private static Pref cacheSLRYTileSize = Pref.makeIntPref("SunRouterYTileSize", Routing.getRoutingTool().prefs, 40);
	public static int getSunRouterYTileSize() { return cacheSLRYTileSize.getInt(); }
	public static void setSunRouterYTileSize(int r) { cacheSLRYTileSize.setInt(r); }

	private static Pref cacheSLRLayerAssgnCapF = Pref.makeDoublePref("SunRouterLayerAssgnCapF", Routing.getRoutingTool().prefs, 0.9);
	public static double getSunRouterLayerAssgnCapF() { return cacheSLRLayerAssgnCapF.getDouble(); }
	public static void setSunRouterLayerAssgnCapF(double r) { cacheSLRLayerAssgnCapF.setDouble(r); }

	private static Pref cacheSLRLengthLongNet = Pref.makeDoublePref("SunRouterLengthLongNet", Routing.getRoutingTool().prefs, 0);
	public static double getSunRouterLengthLongNet() { return cacheSLRLengthLongNet.getDouble(); }
	public static void setSunRouterLengthLongNet(double r) { cacheSLRLengthLongNet.setDouble(r); }

	private static Pref cacheSLRLengthMedNet = Pref.makeDoublePref("SunRouterLengthMedNet", Routing.getRoutingTool().prefs, 0);
	public static double getSunRouterLengthMedNet() { return cacheSLRLengthMedNet.getDouble(); }
	public static void setSunRouterLengthMedNet(double r) { cacheSLRLengthMedNet.setDouble(r); }

	private static Pref cacheSLRTilesPerPinLongNet = Pref.makeDoublePref("SunRouterTilesPerPinLongNet", Routing.getRoutingTool().prefs, 5);
	public static double getSunRouterTilesPerPinLongNet() { return cacheSLRTilesPerPinLongNet.getDouble(); }
	public static void setSunRouterTilesPerPinLongNet(double r) { cacheSLRTilesPerPinLongNet.setDouble(r); }

	private static Pref cacheSLRTilesPerPinMedNet = Pref.makeDoublePref("SunRouterTilesPerPinMedNet", Routing.getRoutingTool().prefs, 3);
	public static double getSunRouterTilesPerPinMedNet() { return cacheSLRTilesPerPinMedNet.getDouble(); }
	public static void setSunRouterTilesPerPinMedNet(double r) { cacheSLRTilesPerPinMedNet.setDouble(r); }
	
	private static Pref cacheSLROneTileFactor = Pref.makeDoublePref("SunRouterOneTileFactor", Routing.getRoutingTool().prefs, 2.65);
	public static double getSunRouterOneTileFactor() { return cacheSLROneTileFactor.getDouble(); }
	public static void setSunRouterOneTileFactor(double r) { cacheSLROneTileFactor.setDouble(r); }

	private static Pref cacheSLROverloadLimit = Pref.makeIntPref("SunRouterOverloadLimit", Routing.getRoutingTool().prefs, 10);
	public static int getSunRouterOverloadLimit() { return cacheSLROverloadLimit.getInt(); }
	public static void setSunRouterOverloadLimit(int r) { cacheSLROverloadLimit.setInt(r); }

	private static Pref cacheSLRPinFactor = Pref.makeIntPref("SunRouterPinFactor", Routing.getRoutingTool().prefs, 10);
	public static int getSunRouterPinFactor() { return cacheSLRPinFactor.getInt(); }
	public static void setSunRouterPinFactor(int r) { cacheSLRPinFactor.setInt(r); }

	private static Pref cacheSLRUPinDensityF = Pref.makeDoublePref("SunRouterUPinDensityF", Routing.getRoutingTool().prefs, 100);
	public static double getSunRouterUPinDensityF() { return cacheSLRUPinDensityF.getDouble(); }
	public static void setSunRouterUPinDensityF(double r) { cacheSLRUPinDensityF.setDouble(r); }

	private static Pref cacheSLRWindow = Pref.makeIntPref("SunRouterWindow", Routing.getRoutingTool().prefs, 30);
	public static int getSunRouterWindow() { return cacheSLRWindow.getInt(); }
	public static void setSunRouterWindow(int r) { cacheSLRWindow.setInt(r); }

	
	

	private static Pref cacheWireOffset = Pref.makeIntPref("SunRouterWireOffset", Routing.getRoutingTool().prefs, 0);
	public static int getSunRouterWireOffset() { return cacheWireOffset.getInt(); }
	public static void setSunRouterWireOffset(int r) { cacheWireOffset.setInt(r); }

	private static Pref cacheWireModulo = Pref.makeIntPref("SunRouterWireModulo", Routing.getRoutingTool().prefs, -1);
	public static int getSunRouterWireModulo() { return cacheWireModulo.getInt(); }
	public static void setSunRouterWireModulo(int r) { cacheWireModulo.setInt(r); }

	private static Pref cacheWireBlockageFactor = Pref.makeDoublePref("SunRouterWireBlockageFactor", Routing.getRoutingTool().prefs, 0);
	public static double getSunRouterWireBlockageFactor() { return cacheWireBlockageFactor.getDouble(); }
	public static void setSunRouterWireBlockageFactor(double r) { cacheWireBlockageFactor.setDouble(r); }

	private static Pref cacheRipUpMaximum = Pref.makeIntPref("SunRouterRipUpMaximum", Routing.getRoutingTool().prefs, 3);
	public static int getSunRouterRipUpMaximum() { return cacheRipUpMaximum.getInt(); }
	public static void setSunRouterRipUpMaximum(int r) { cacheRipUpMaximum.setInt(r); }

	private static Pref cacheRipUpPenalty = Pref.makeIntPref("SunRouterRipUpPenalty", Routing.getRoutingTool().prefs, 10);
	public static int getSunRouterRipUpPenalty() { return cacheRipUpPenalty.getInt(); }
	public static void setSunRouterRipUpPenalty(int r) { cacheRipUpPenalty.setInt(r); }

	private static Pref cacheRipUpExpansion = Pref.makeIntPref("SunRouterRipUpExpansion", Routing.getRoutingTool().prefs, 10);
	public static int getSunRouterRipUpExpansion() { return cacheRipUpExpansion.getInt(); }
	public static void setSunRouterRipUpExpansion(int r) { cacheRipUpExpansion.setInt(r); }

	private static Pref cacheZRipUpExpansion = Pref.makeIntPref("SunRouterZRipUpExpansion", Routing.getRoutingTool().prefs, 2);
	public static int getSunRouterZRipUpExpansion() { return cacheZRipUpExpansion.getInt(); }
	public static void setSunRouterZRipUpExpansion(int r) { cacheZRipUpExpansion.setInt(r); }

	private static Pref cacheRipUpSearches = Pref.makeIntPref("SunRouterRipUpSearches", Routing.getRoutingTool().prefs, 1);
	public static int getSunRouterRipUpSearches() { return cacheRipUpSearches.getInt(); }
	public static void setSunRouterRipUpSearches(int r) { cacheRipUpSearches.setInt(r); }

	private static Pref cacheGlobalPathExpansion = Pref.makeIntPref("SunRouterGlobalPathExpansion", Routing.getRoutingTool().prefs, 5);
	public static int getSunRouterGlobalPathExpansion() { return cacheGlobalPathExpansion.getInt(); }
	public static void setSunRouterGlobalPathExpansion(int r) { cacheGlobalPathExpansion.setInt(r); }

	private static Pref cacheSourceAccessExpansion = Pref.makeIntPref("SunRouterSourceAccessExpansion", Routing.getRoutingTool().prefs, 10);
	public static int getSunRouterSourceAccessExpansion() { return cacheSourceAccessExpansion.getInt(); }
	public static void setSunRouterSourceAccessExpansion(int r) { cacheSourceAccessExpansion.setInt(r); }

	private static Pref cacheSinkAccessExpansion = Pref.makeIntPref("SunRouterSinkAccessExpansion", Routing.getRoutingTool().prefs, 10);
	public static int getSunRouterSinkAccessExpansion() { return cacheSinkAccessExpansion.getInt(); }
	public static void setSunRouterSinkAccessExpansion(int r) { cacheSinkAccessExpansion.setInt(r); }

	private static Pref cacheDenseViaAreaSize = Pref.makeIntPref("SunRouterDenseViaAreaSize", Routing.getRoutingTool().prefs, 60);
	public static int getSunRouterDenseViaAreaSize() { return cacheDenseViaAreaSize.getInt(); }
	public static void setSunRouterDenseViaAreaSize(int r) { cacheDenseViaAreaSize.setInt(r); }

	private static Pref cacheRetryExpandRouting = Pref.makeIntPref("SunRouterRetryExpandRouting", Routing.getRoutingTool().prefs, 50);
	public static int getSunRouterRetryExpandRouting() { return cacheRetryExpandRouting.getInt(); }
	public static void setSunRouterRetryExpandRouting(int r) { cacheRetryExpandRouting.setInt(r); }

	private static Pref cacheRetryDenseViaAreaSize = Pref.makeIntPref("SunRouterRetryDenseViaAreaSize", Routing.getRoutingTool().prefs, 100);
	public static int getSunRouterRetryDenseViaAreaSize() { return cacheRetryDenseViaAreaSize.getInt(); }
	public static void setSunRouterRetryDenseViaAreaSize(int r) { cacheRetryDenseViaAreaSize.setInt(r); }

	private static Pref cachePathSearchControl = Pref.makeIntPref("SunRouterPathSearchControl", Routing.getRoutingTool().prefs, 10000);
	public static int getSunRouterPathSearchControl() { return cachePathSearchControl.getInt(); }
	public static void setSunRouterPathSearchControl(int r) { cachePathSearchControl.setInt(r); }

	private static Pref cacheSparseViaModulo = Pref.makeIntPref("SunRouterSparseViaModulo", Routing.getRoutingTool().prefs, 31);
	public static int getSunRouterSparseViaModulo() { return cacheSparseViaModulo.getInt(); }
	public static void setSunRouterSparseViaModulo(int r) { cacheSparseViaModulo.setInt(r); }

	private static Pref cacheLowPathSearchCost = Pref.makeIntPref("SunRouterLowPathSearchCost", Routing.getRoutingTool().prefs, 5);
	public static int getSunRouterLowPathSearchCost() { return cacheLowPathSearchCost.getInt(); }
	public static void setSunRouterLowPathSearchCost(int r) { cacheLowPathSearchCost.setInt(r); }

	private static Pref cacheMediumPathSearchCost = Pref.makeIntPref("SunRouterMediumPathSearchCost", Routing.getRoutingTool().prefs, 20);
	public static int getSunRouterMediumPathSearchCost() { return cacheMediumPathSearchCost.getInt(); }
	public static void setSunRouterMediumPathSearchCost(int r) { cacheMediumPathSearchCost.setInt(r); }

	private static Pref cacheHighPathSearchCost = Pref.makeIntPref("SunRouterHighPathSearchCost", Routing.getRoutingTool().prefs, 100);
	public static int getSunRouterHighPathSearchCost() { return cacheHighPathSearchCost.getInt(); }
	public static void setSunRouterHighPathSearchCost(int r) { cacheHighPathSearchCost.setInt(r); }

	private static Pref cacheTakenPathSearchCost = Pref.makeIntPref("SunRouterTakenPathSearchCost", Routing.getRoutingTool().prefs, 10000);
	public static int getSunRouterTakenPathSearchCost() { return cacheTakenPathSearchCost.getInt(); }
	public static void setSunRouterTakenPathSearchCost(int r) { cacheTakenPathSearchCost.setInt(r); }

}
