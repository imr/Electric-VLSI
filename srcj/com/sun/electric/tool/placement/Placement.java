/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Placement.java
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.placement;

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.GenMath.MutableInteger;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Class to place cells for better routing.
 */
public class Placement
{
	private final double PADDING = 5;
	private final boolean DEBUG = false;

	private Cell cell;
	private Map<NodeInst,Map<NodeInst,MutableInteger>> connectivityMap;
	private Map<NodeInst,NodeInst> oldToNew;

	/**
	 * Method to run placement on the current cell.
	 */
	public static void placeCurrentCell()
	{
		// get cell and network information
		UserInterface ui = Job.getUserInterface();
		Cell cell = ui.needCurrentCell();
		if (cell == null) return;

		new PlaceJob(cell);
	}

	/**
	 * Class to do placement in a new job.
	 */
	private static class PlaceJob extends Job
	{
		private Cell cell;
		private Cell newCell;

		private PlaceJob(Cell cell)
		{
			super("Place cells", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			Placement pla = new Placement(cell);
			newCell = pla.doPlacement();
			fieldVariableChanged("newCell");
            return true;
		}

		public void terminateOK()
		{
			if (newCell != null)
				WindowFrame.createEditWindow(newCell);
		}
	}

	private Placement(Cell cell)
	{
		this.cell = cell;
	}

	private Cell doPlacement()
	{
		Netlist netList = cell.acquireUserNetlist();
		if (netList == null)
		{
			System.out.println("Sorry, a deadlock aborted routing (network information unavailable).  Please try again");
			return null;
		}

		// find all cells to be placed
		Set<NodeInst> cellsToPlace = new HashSet<NodeInst>();
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (ni.isIconOfParent()) continue;
			boolean validCell = ni.isCellInstance();
			if (!validCell)
			{
				if (ni.getProto().getTechnology() != Generic.tech())
				{
					PrimitiveNode.Function fun = ni.getFunction();
					if (fun != PrimitiveNode.Function.CONNECT && fun != PrimitiveNode.Function.CONTACT &&
						fun != PrimitiveNode.Function.PIN)
							validCell = true;
				}
			}
			if (validCell) cellsToPlace.add(ni);
		}

		// build the connectivity map
		connectivityMap = new HashMap<NodeInst,Map<NodeInst,MutableInteger>>();
		for(Iterator<Network> it = netList.getNetworks(); it.hasNext(); )
		{
			Network net = it.next();
			List<NodeInst> nodesOnNet = new ArrayList<NodeInst>();
			for(Iterator<NodeInst> nIt = net.getNodes(); nIt.hasNext(); )
			{
				NodeInst ni = nIt.next();
				if (cellsToPlace.contains(ni)) nodesOnNet.add(ni);
			}

			// add all combinations of the nodes on this net to the connectivity map
			for(int i=0; i<nodesOnNet.size(); i++)
			{
				NodeInst ni1 = nodesOnNet.get(i);
				for(int j=i+1; j<nodesOnNet.size(); j++)
				{
					NodeInst ni2 = nodesOnNet.get(j);
					incrementMap(ni1, ni2);
					incrementMap(ni2, ni1);
				}
			}
		}

		// make initial partition with all nodes
		Partition topPart = new Partition();
		topPart.depth = 0;
		for(NodeInst ni : cellsToPlace) topPart.allNodes.add(ni);

		// build list of partitions to organize
		List<Partition> partitionsToOrganize = new ArrayList<Partition>();
		partitionsToOrganize.add(topPart);

		// iteratively go through list and organize them
		while (partitionsToOrganize.size() > 0)
		{
			// get partition
			Partition part = partitionsToOrganize.get(0);
			partitionsToOrganize.remove(0);

			// split the partition randomly
			part.splitRandomly();
			if (DEBUG)
			{
				System.out.print("INITIAL NODES IN GROUP 1:");
				for(NodeInst ni : part.part1.allNodes) System.out.print(" "+ni.describe(false));
				System.out.println();
				System.out.print("INITIAL NODES IN GROUP 2:");
				for(NodeInst ni : part.part2.allNodes) System.out.print(" "+ni.describe(false));
				System.out.println();
			}

			// organize the two halves properly
			part.organize();
			if (DEBUG)
			{
				System.out.print("FINAL NODES IN GROUP 1:");
				for(NodeInst ni : part.part1.allNodes) System.out.print(" "+ni.describe(false));
				System.out.println();
				System.out.print("FINAL NODES IN GROUP 2:");
				for(NodeInst ni : part.part2.allNodes) System.out.print(" "+ni.describe(false));
				System.out.println();
			}

			if (part.part1.allNodes.size() > 2) partitionsToOrganize.add(part.part1);
			if (part.part2.allNodes.size() > 2) partitionsToOrganize.add(part.part2);
		}

		// now make a new cell
		oldToNew = new HashMap<NodeInst,NodeInst>(); 
		Cell newCell = Cell.makeInstance(cell.getLibrary(), cell.noLibDescribe());

		placePartition(topPart, new Point2D.Double(0, 0), newCell, netList);

		placeConnections(netList);

		return newCell;
	}

	private void placeConnections(Netlist netList)
	{
		for(Iterator<Network> it = netList.getNetworks(); it.hasNext(); )
		{
			Network net = it.next();
			List<PortInst> portsToConnect = new ArrayList<PortInst>();
			for(Iterator<PortInst> pIt = net.getPorts(); pIt.hasNext(); )
			{
				PortInst pi = pIt.next();
				NodeInst ni = pi.getNodeInst();
				NodeInst newNi = oldToNew.get(ni);
				if (newNi != null)
				{
					PortInst newPi = newNi.findPortInstFromProto(pi.getPortProto());
					portsToConnect.add(newPi);
				}
			}

			// make the connections
			for(int i=1; i<portsToConnect.size(); i++)
			{
				PortInst lastPi = portsToConnect.get(i-1);
				PortInst thisPi = portsToConnect.get(i);
				ArcInst.makeInstance(Generic.tech().unrouted_arc, lastPi, thisPi);
			}
		}
	}

	private Point2D placePartition(Partition part, Point2D offset, Cell newCell, Netlist netList)
	{
		String indent = "";
		if (DEBUG)
		{
			for(int i=0; i<part.depth; i++) indent += "   ";
			indent += part.depth + ": ";
			System.out.println(indent+"PARTITION AT OFFSET ("+offset.getX()+","+offset.getY()+")...");
		}
		Point2D off;
		if (part.part1 != null && part.part2 != null)
		{
			if (DEBUG) System.out.println(indent+"PLACING SUBPART 1 AT ("+offset.getX()+","+offset.getY()+")");
			Point2D off1 = placePartition(part.part1, offset, newCell, netList);
			if ((part.depth&1) == 0)
			{
				Point2D nextOff = new Point2D.Double(offset.getX(), offset.getY() + off1.getY());
				if (DEBUG) System.out.println(indent+"PLACING SUBPART 2 AT ("+nextOff.getX()+","+nextOff.getY()+")");
				Point2D off2 = placePartition(part.part2, nextOff, newCell, netList);
				off = new Point2D.Double(Math.max(off1.getX(), off2.getX()), off1.getY() + off2.getY());
			} else
			{
				Point2D nextOff = new Point2D.Double(offset.getX() + off1.getX(), offset.getY());
				if (DEBUG) System.out.println(indent+"PLACING SUBPART 2 AT ("+nextOff.getX()+","+nextOff.getY()+")");
				Point2D off2 = placePartition(part.part2, nextOff, newCell, netList);
				off = new Point2D.Double(off1.getX() + off2.getX(), Math.max(off1.getY(), off2.getY()));
			}
		} else
		{
			double widestX = 0, widestY = 0;
			double placeX = offset.getX(), placeY = offset.getY();
			Map<NodeInst,EPoint> destinationLocation = new HashMap<NodeInst,EPoint>();
			for(NodeInst ni : part.allNodes)
			{
				double width = ni.getXSize();
				double height = ni.getYSize();
				width = height = Math.max(width, height);
				EPoint thisOff = new EPoint(placeX, placeY);
				if ((part.depth&1) != 0)
				{
					widestX += width + PADDING;
					placeX += width + PADDING;
					widestY = Math.max(widestY, height + PADDING);
				} else
				{
					widestX = Math.max(widestX, width + PADDING);
					widestY += height + PADDING;
					placeY += height + PADDING;
				}
				destinationLocation.put(ni, thisOff);
			}
			Map<NodeInst,OrientationAndOffset> properOrientation = findOrientations(part.allNodes, netList, destinationLocation);
			for(NodeInst ni : part.allNodes)
			{
				double width = ni.getXSize();
				double height = ni.getYSize();
				EPoint thisOff = destinationLocation.get(ni);
				if (DEBUG) System.out.println(indent+"PLACING "+ni.describe(false)+" AT ("+thisOff.getX()+","+thisOff.getY()+")");
				OrientationAndOffset orOff = properOrientation.get(ni);
				Orientation or = ni.getOrient();
				if (orOff != null)
				{
					or = orOff.orient;
					thisOff = new EPoint(thisOff.getX() + orOff.offset.getX(), thisOff.getY() + orOff.offset.getY());
				}
				NodeInst newNi = NodeInst.makeInstance(ni.getProto(), thisOff, width, height, newCell, or, ni.getName(), ni.getTechSpecific());
				oldToNew.put(ni, newNi);
			}
			off = new Point2D.Double(widestX, widestY);
		}
		if (DEBUG) System.out.println(indent+"NEW OFFSET ("+off.getX()+","+off.getY()+")");
		return off;
	}

	private static class OrientationAndOffset
	{
		Orientation orient;
		Point2D offset;
	}

	private static class OrientationConnection
	{
		PortProto thisPP;
		NodeInst otherNI;
		PortProto otherPP;
	}

	private static class OrientationChoices
	{
		List<NodeInst> dummyNodes;
		NodeInst currentDummyNode;
		Map<PortInst,EPoint> portLocations;
		List<OrientationConnection> connections;

		OrientationChoices()
		{
			dummyNodes = new ArrayList<NodeInst>();
			portLocations = new HashMap<PortInst,EPoint>();
			connections = new ArrayList<OrientationConnection>();
		}

		double getLengthOfAllWires(NodeInst dummyNode, Map<NodeInst,OrientationChoices> allPossibilities)
		{
			double length = 0;
			for(OrientationConnection con : connections)
			{
				PortInst pi = dummyNode.findPortInstFromProto(con.thisPP);
				EPoint pt = portLocations.get(pi);
				OrientationChoices otherChoice = allPossibilities.get(con.otherNI);
				PortInst otherPi = otherChoice.currentDummyNode.findPortInstFromProto(con.otherPP);
				EPoint otherPt = otherChoice.portLocations.get(otherPi);
				double dist = pt.distance(otherPt);
				length += dist;
			}
			return length;
		}
	}

	private Point2D getOffset(NodeProto np, Orientation orient)
	{
		if (np instanceof PrimitiveNode) return new Point2D.Double(0, 0);
		Cell cell = (Cell)np;
		ERectangle bounds = cell.getBounds();
		Point2D offset = new Point2D.Double(-bounds.getCenterX(), -bounds.getCenterY());
		orient.pureRotate().transform(offset, offset);
		return offset;
	}

	/**
	 * Method to find the ideal orientation for all of the nodes at the bottom point.
	 * @param allNodes
	 * @return
	 */
	private Map<NodeInst,OrientationAndOffset> findOrientations(List<NodeInst> allNodes, Netlist netList, Map<NodeInst,EPoint> destinationLocation)
	{
		Map<NodeInst,OrientationAndOffset> properOrientation = new HashMap<NodeInst,OrientationAndOffset>();
		if (allNodes.size() > 1)
		{
			if (DEBUG) System.out.println("FINDING ORIENTATIONS FOR PARTITION WITH "+allNodes.size()+" NODES");
			// build OrientationChoices objects for all nodes in this partition
			Map<NodeInst,OrientationChoices> allPossibilities = new HashMap<NodeInst,OrientationChoices>();
			for(NodeInst ni : allNodes)
			{
				// create an OrientationChoices object for this NodeInst
				OrientationChoices oc = new OrientationChoices();
				allPossibilities.put(ni, oc);

				// add all of the possible orientations
				EPoint loc = destinationLocation.get(ni);
				Orientation [] standardEight = new Orientation[] {Orientation.IDENT, Orientation.R, Orientation.RR, Orientation.RRR,
					Orientation.X, Orientation.XR, Orientation.XRR, Orientation.XRRR};
				oc.currentDummyNode = null;
				for(int i=0; i<standardEight.length; i++)
				{
					Point2D offset = getOffset(ni.getProto(), standardEight[i]);
//System.out.println("CELL "+ni.getProto().describe(false)+" AT ORIENTATION "+standardEight[i].toString()+" IS OFFSET BY ("+offset.getX()+","+offset.getY()+")");
					EPoint offLoc = new EPoint(loc.getX() + offset.getX(), loc.getY() + offset.getY());
					NodeInst dummyNI = NodeInst.makeDummyInstance(ni.getProto(), ni.getTechSpecific(), offLoc,
						ni.getXSize(), ni.getYSize(), standardEight[i]);
					oc.dummyNodes.add(dummyNI);
					if (ni.getOrient() == standardEight[i]) oc.currentDummyNode = dummyNI;
				}
				if (oc.currentDummyNode == null)
				{
					Point2D offset = getOffset(ni.getProto(), ni.getOrient());
					EPoint offLoc = new EPoint(loc.getX() + offset.getX(), loc.getY() + offset.getY());
					oc.currentDummyNode = NodeInst.makeDummyInstance(ni.getProto(), ni.getTechSpecific(), offLoc,
						ni.getXSize(), ni.getYSize(), ni.getOrient());
					oc.dummyNodes.add(oc.currentDummyNode);
				}

				// add all of the port locations for all possible orientations
				for(NodeInst dummyNI : oc.dummyNodes)
				{
					for(Iterator<PortInst> it = dummyNI.getPortInsts(); it.hasNext(); )
					{
						PortInst pi = it.next();
						Poly poly = pi.getPoly();
						EPoint pt = poly.getCenter();
						oc.portLocations.put(pi, pt);						
					}
				}
			}

			// add all of the connections to other nodes in the partition
			for(NodeInst ni : allNodes)
			{
				OrientationChoices oc = allPossibilities.get(ni);
				for(Iterator<PortInst> it = ni.getPortInsts(); it.hasNext(); )
				{
					PortInst pi = it.next();
					Network net = netList.getNetwork(pi);
					for(Iterator<PortInst> pIt = net.getPorts(); pIt.hasNext(); )
					{
						PortInst otherPI = pIt.next();
						NodeInst otherNI = otherPI.getNodeInst();
						if (otherNI == ni) continue;
						if (allPossibilities.get(otherNI) == null) continue;

						OrientationConnection orc = new OrientationConnection();
						orc.thisPP = pi.getPortProto();
						orc.otherNI = otherNI;
						orc.otherPP = otherPI.getPortProto();
						oc.connections.add(orc);
					}
				}
			}

			// now find the optimal orientation choice for each NodeInst
			for(NodeInst ni : allNodes)
			{
				OrientationChoices oc = allPossibilities.get(ni);
				double bestDist = oc.getLengthOfAllWires(oc.currentDummyNode, allPossibilities);
				NodeInst betterOrientation = null;
				for(NodeInst otherOrientation : oc.dummyNodes)
				{
					if (otherOrientation == oc.currentDummyNode) continue;
					double dist = oc.getLengthOfAllWires(otherOrientation, allPossibilities);
					if (dist < bestDist)
					{
						bestDist = dist;
						betterOrientation = otherOrientation;
					}
				}
				if (betterOrientation != null)
				{
					oc.currentDummyNode = betterOrientation;
					OrientationAndOffset orOff = new OrientationAndOffset();
					orOff.orient = betterOrientation.getOrient();
					orOff.offset = getOffset(ni.getProto(), orOff.orient);
					properOrientation.put(ni, orOff);
				}
			}
		}
		return properOrientation;
	}

	private class Partition
	{
		List<NodeInst> allNodes;
		Partition part1, part2;
		int depth;

		Partition()
		{
			allNodes = new ArrayList<NodeInst>();
		}

		void splitRandomly()
		{
			part1 = new Partition();
			part2 = new Partition();
			part1.depth = depth+1;
			part2.depth = depth+1;
			boolean putIn1 = true;
			for(NodeInst ni : allNodes)
			{
				if (putIn1) part1.allNodes.add(ni); else
					part2.allNodes.add(ni);
				putIn1 = !putIn1;			
			}
		}

		void organize()
		{
			// make a list of swaps
			List<SwapNodes> allSwaps = new ArrayList<SwapNodes>();

			for(;;)
			{
				// determine the baseline connection count
				int startingCuts = findNumCuts();

				// now try swapping all nodes
				int bestGain = 0;
				int group1Member = -1, group2Member = -1;
				for(int i=0; i<part1.allNodes.size(); i++)
				{
					NodeInst ni1 = part1.allNodes.get(i);
					for(int j=0; j<part2.allNodes.size(); j++)
					{
						NodeInst ni2 = part2.allNodes.get(j);

						// swap them
						part1.allNodes.set(i, ni2);
						part2.allNodes.set(j, ni1);

						// see if the swap produces a gain
						int newCuts = findNumCuts();
						int gain = startingCuts - newCuts;
						if (gain > bestGain)
						{
							// gain found
							bestGain = gain;
							group1Member = i;
							group2Member = j;
						}

						// restore order
						part1.allNodes.set(i, ni1);
						part2.allNodes.set(j, ni2);
					}
				}

				// stop now if no gain was found
				if (bestGain == 0) break;

				// enter the gain, remove these nodes, and continue to search
				NodeInst ni1 = part1.allNodes.get(group1Member);
				NodeInst ni2 = part2.allNodes.get(group2Member);
				if (DEBUG) System.out.println("SWAPPING NODES "+ni1.describe(false)+" AND "+ni2.describe(false)+" FOR A GAIN OF "+bestGain);
				part1.allNodes.remove(group1Member);
				part2.allNodes.remove(group2Member);
				SwapNodes g = new SwapNodes(ni1, ni2);
				allSwaps.add(g);
			}

			// put the swaps back in, swapped
			for(SwapNodes g : allSwaps)
			{
				part1.allNodes.add(g.ni2);
				part2.allNodes.add(g.ni1);
			}
		}

		/**
		 * Method to scan two halves of the Partition and return the total number of
		 * connections that separate them.
		 * @return the number of connections between the two groups.
		 */
		private int findNumCuts()
		{
			int cuts = 0;
			for(NodeInst ni1 : part1.allNodes)
			{
				for(NodeInst ni2 : part2.allNodes)
					cuts += getConnectivity(ni1, ni2);
			}
			return cuts;
		}
	}

	private static class SwapNodes
	{
		NodeInst ni1, ni2;

		SwapNodes(NodeInst n1, NodeInst n2)
		{
			ni1 = n1;
			ni2 = n2;
		}
	}

	/**
	 * Method to return the number of connections between two NodeInsts.
	 * @param ni1 the first NodeInst.
	 * @param ni2 the second NodeInst.
	 * @return the number of connections between the NodeInsts.
	 */
	private int getConnectivity(NodeInst ni1, NodeInst ni2)
	{
		Map<NodeInst,MutableInteger> destMap = connectivityMap.get(ni1);
		if (destMap == null) return 0;
		MutableInteger mi = destMap.get(ni2);
		if (mi == null) return 0;
		return mi.intValue();
	}

	/**
	 * Method to build the connectivity map by adding a connection between two NodeInsts.
	 * This method is usually called twice with the NodeInsts in both orders because
	 * the mapping is not symmetric.
	 * @param ni1 the first NodeInst.
	 * @param ni2 the second NodeInst.
	 */
	private void incrementMap(NodeInst ni1, NodeInst ni2)
	{
		Map<NodeInst,MutableInteger> destMap = connectivityMap.get(ni1);
		if (destMap == null)
			connectivityMap.put(ni1, destMap = new HashMap<NodeInst,MutableInteger>());
		MutableInteger mi = destMap.get(ni2);
		if (mi == null) destMap.put(ni2, mi = new MutableInteger(0));
		mi.increment();
	}
}
