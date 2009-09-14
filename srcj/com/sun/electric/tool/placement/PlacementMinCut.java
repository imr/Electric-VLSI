/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PlacementMinCut.java
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

import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.GenMath.MutableInteger;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNetwork;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNode;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Placement algorithm to do Min-Cut placement.
 */
public class PlacementMinCut extends PlacementFrame
{
	private final double PADDING = 5;
	private final boolean DEBUG = false;

	private Map<PlacementNode,Map<PlacementNode,MutableInteger>> connectivityMap;

	/**
	 * Method to return the name of this placement algorithm.
	 * @return the name of this placement algorithm.
	 */
	public String getAlgorithmName() { return "Min-Cut"; }

	/**
	 * Method to do Min-Cut Placement.
	 * @param nodesToPlace a list of all nodes that are to be placed.
	 * @param allNetworks a list of all networks that connect the nodes.
	 * @param cellName the name of the cell being placed.
	 */
	protected void runPlacement(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks, String cellName)
	{
		// build the connectivity map with the number of connections between any two PlacementNodes
		connectivityMap = new HashMap<PlacementNode,Map<PlacementNode,MutableInteger>>();
		for(PlacementNetwork plNet : allNetworks)
		{
			// add all combinations of the nodes on this net to the connectivity map
			List<PlacementPort> portsInNetwork = plNet.getPortsOnNet();
			for(int i=0; i<portsInNetwork.size(); i++)
			{
				PlacementPort plPort1 = portsInNetwork.get(i);
				PlacementNode plNode1 = plPort1.getPlacementNode();
				for(int j=i+1; j<portsInNetwork.size(); j++)
				{
					PlacementPort plPort2 = portsInNetwork.get(j);
					PlacementNode plNode2 = plPort2.getPlacementNode();
					
					incrementMap(plNode1, plNode2);
					incrementMap(plNode2, plNode1);
				}
			}
		}

		// make initial partition with all nodes
		List<PlacementNode> singletons = new ArrayList<PlacementNode>();
		Partition topPart = new Partition();
		topPart.depth = 0;
		for(PlacementNode plNode : nodesToPlace)
		{
			boolean connected = false;
			for(PlacementPort plPort : plNode.getPorts())
			{
				PlacementNetwork net = plPort.getPlacementNetwork();
				if (net == null) continue;
				if (net.getPortsOnNet().size() > 1) { connected = true;   break; }
			}
			if (connected) topPart.allNodes.add(plNode); else
				singletons.add(plNode);
		}

		// build list of partitions to organize
		List<Partition> partitionsToOrganize = new ArrayList<Partition>();
		partitionsToOrganize.add(topPart);

		// iteratively go through list and organize them
		while (partitionsToOrganize.size() > 0)
		{
			// get partition
			Partition part = partitionsToOrganize.get(0);
			partitionsToOrganize.remove(0);
			if (part.allNodes.size() <= 2) continue;

			// split the partition randomly
			part.splitRandomly();
			if (DEBUG)
			{
				System.out.print("INITIAL NODES IN GROUP 1:");
				for(PlacementNode plNode : part.part1.allNodes) System.out.print(" "+plNode);
				System.out.println();
				System.out.print("INITIAL NODES IN GROUP 2:");
				for(PlacementNode plNode : part.part2.allNodes) System.out.print(" "+plNode);
				System.out.println();
			}

			// organize the two halves properly
			part.organize();
			if (DEBUG)
			{
				System.out.print("FINAL NODES IN GROUP 1:");
				for(PlacementNode plNode : part.part1.allNodes) System.out.print(" "+plNode);
				System.out.println();
				System.out.print("FINAL NODES IN GROUP 2:");
				for(PlacementNode plNode : part.part2.allNodes) System.out.print(" "+plNode);
				System.out.println();
			}

			if (part.part1.allNodes.size() > 2) partitionsToOrganize.add(part.part1);
			if (part.part2.allNodes.size() > 2) partitionsToOrganize.add(part.part2);
		}

		Point2D lastOffset = placePartitions(topPart, new Point2D.Double(0, 0));
		double x = lastOffset.getX(), y = lastOffset.getY();
		for(PlacementNode plNode : singletons)
		{
			plNode.setPlacement(x, y);
			x += PADDING;
		}
	}

	private Point2D placePartitions(Partition part, Point2D offset)
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
			Point2D off1 = placePartitions(part.part1, offset);
			if ((part.depth&1) == 0)
			{
				Point2D nextOff = new Point2D.Double(offset.getX(), offset.getY() + off1.getY());
				if (DEBUG) System.out.println(indent+"PLACING SUBPART 2 AT ("+nextOff.getX()+","+nextOff.getY()+")");
				Point2D off2 = placePartitions(part.part2, nextOff);
				off = new Point2D.Double(Math.max(off1.getX(), off2.getX()), off1.getY() + off2.getY());
			} else
			{
				Point2D nextOff = new Point2D.Double(offset.getX() + off1.getX(), offset.getY());
				if (DEBUG) System.out.println(indent+"PLACING SUBPART 2 AT ("+nextOff.getX()+","+nextOff.getY()+")");
				Point2D off2 = placePartitions(part.part2, nextOff);
				off = new Point2D.Double(off1.getX() + off2.getX(), Math.max(off1.getY(), off2.getY()));
			}
		} else
		{
			double widestX = 0, widestY = 0;
			double placeX = offset.getX(), placeY = offset.getY();
			for(PlacementNode plNode : part.allNodes)
			{
				double width = plNode.getWidth();
				double height = plNode.getHeight();
				width = height = Math.max(width, height);
				Point2D thisOff = new Point2D.Double(placeX, placeY);
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
				plNode.setPlacement(thisOff.getX(), thisOff.getY());
			}
			Map<PlacementNode,Orientation> properOrientation = findOrientations(part.allNodes);
			for(PlacementNode plNode : part.allNodes)
			{
				Orientation or = properOrientation.get(plNode);
				if (or != null)
					plNode.setOrientation(or);
			}
			off = new Point2D.Double(widestX, widestY);
		}
		if (DEBUG) System.out.println(indent+"NEW OFFSET ("+off.getX()+","+off.getY()+")");
		return off;
	}

	private static class OrientationConnection
	{
		PlacementPort thisPP;
		PlacementNode otherPN;
		PlacementPort otherPP;
	}

	/**
	 * Method to find the ideal orientation for all of the nodes at the bottom point.
	 * @param allNodes a List of PlacementNodes that have location, but not ideal orientation.
	 * @return a Map assigning orientation to each of the PlacementNodes in the list.
	 */
	private Map<PlacementNode,Orientation> findOrientations(List<PlacementNode> allNodes)
	{
		Map<PlacementNode,Orientation> properOrientation = new HashMap<PlacementNode,Orientation>();
		if (allNodes.size() > 1)
		{
//boolean debug = currentCellName.equals("spiceHier{sch}");
//if (debug) System.out.println("FINDING ORIENTATIONS FOR PARTITION WITH "+allNodes.size()+" NODES");
			Map<PlacementNode,List<OrientationConnection>> allPossibilities = new HashMap<PlacementNode,List<OrientationConnection>>();
			for(PlacementNode plNode : allNodes)
			{
				// create a List of OrientationConnection objects for this NodeInst
				List<OrientationConnection> oc = new ArrayList<OrientationConnection>();
				allPossibilities.put(plNode, oc);
			}

			// add all of the connections to other nodes in the partition
			for(PlacementNode plNode : allNodes)
			{
				List<OrientationConnection> oc = allPossibilities.get(plNode);
				
				for(PlacementPort plPort : plNode.getPorts())
				{
					PlacementNetwork plNet = plPort.getPlacementNetwork();
					if (plNet == null) continue;
					for(PlacementPort otherPlPort : plNet.getPortsOnNet())
					{
						PlacementNode otherPlNode = otherPlPort.getPlacementNode();
						if (otherPlNode == plNode) continue;
						if (allPossibilities.get(otherPlNode) == null) continue;

						OrientationConnection orc = new OrientationConnection();
						orc.thisPP = plPort;
						orc.otherPN = otherPlNode;
						orc.otherPP = otherPlPort;
						oc.add(orc);
					}
				}
			}

			// now find the optimal orientation choice for each NodeInst
			Orientation [] standardEight = new Orientation[] {Orientation.IDENT, Orientation.R, Orientation.RR, Orientation.RRR,
				Orientation.X, Orientation.XR, Orientation.XRR, Orientation.XRRR};
//			if (allNodes.size() == 2)
//			{
//				// try all combinations of the two
//				PlacementNode plNode1 = allNodes.get(0);
//				PlacementNode plNode2 = allNodes.get(1);
//				List<OrientationConnection> oc = allPossibilities.get(plNode1);
//				Orientation betterOrientation1 = null;
//				Orientation betterOrientation2 = null;
//				double bestDist = Double.MAX_VALUE;
//				for(int i=0; i<standardEight.length; i++)
//				{
//					plNode1.setOrientation(standardEight[i]);
//					for(int j=0; j<standardEight.length; j++)
//					{
//						plNode2.setOrientation(standardEight[j]);
//						double length = 0;
//						for(OrientationConnection con : oc)
//						{
//							Point2D pt1 = new Point2D.Double(plNode1.getPlacementX() + con.thisPP.getRotatedOffX(),
//								plNode1.getPlacementY() + con.thisPP.getRotatedOffY());
//							Point2D pt2 = new Point2D.Double(plNode2.getPlacementX() + con.otherPP.getRotatedOffX(),
//								plNode2.getPlacementY() + con.otherPP.getRotatedOffY());
//							double dist = pt1.distance(pt2);
//							length += dist;
//						}
//						if (betterOrientation1 == null || length < bestDist)
//						{
//							bestDist = length;
//							betterOrientation1 = standardEight[i];
//							betterOrientation2 = standardEight[j];
//						}
//					}
//				}
//				if (betterOrientation1 != null)
//				{
//					plNode1.setOrientation(betterOrientation1);
//					properOrientation.put(plNode1, betterOrientation1);
//					plNode2.setOrientation(betterOrientation2);
//					properOrientation.put(plNode2, betterOrientation2);
//				}
//			} else
			{
				for(PlacementNode plNode : allNodes)
				{
					List<OrientationConnection> oc = allPossibilities.get(plNode);
					double bestDist = Double.MAX_VALUE;
					Orientation betterOrientation = null;
					for(int i=0; i<standardEight.length; i++)
					{
						plNode.setOrientation(standardEight[i]);

//if (debug) System.out.println("COMPUTING LENGTH FOR NODE "+plNode+" ORIENTATION "+standardEight[i]);
						double length = 0;
						for(OrientationConnection con : oc)
						{
							Point2D pt = new Point2D.Double(plNode.getPlacementX() + con.thisPP.getRotatedOffX(),
								plNode.getPlacementY() + con.thisPP.getRotatedOffY());
							Point2D otherPt = new Point2D.Double(con.otherPN.getPlacementX() + con.otherPP.getRotatedOffX(),
								con.otherPN.getPlacementY() + con.otherPP.getRotatedOffY());
							double dist = pt.distance(otherPt);
//if (debug) System.out.println("  ("+pt.getX()+","+pt.getY()+") TO ("+otherPt.getX()+","+otherPt.getY()+") = "+dist);
							length += dist;
						}
//if (debug) System.out.println("  ======= LENGTH="+length);
						if (betterOrientation == null || length < bestDist)
						{
							bestDist = length;
							betterOrientation = standardEight[i];
						}
					}
					if (betterOrientation != null)
					{
						plNode.setOrientation(betterOrientation);
						properOrientation.put(plNode, betterOrientation);
					}
				}
			}
		}
		return properOrientation;
	}

	private class Partition
	{
		List<PlacementNode> allNodes;
		Partition part1, part2;
		int depth;

		Partition()
		{
			allNodes = new ArrayList<PlacementNode>();
		}

		void splitRandomly()
		{
			part1 = new Partition();
			part2 = new Partition();
			part1.depth = depth+1;
			part2.depth = depth+1;
			boolean putIn1 = true;
			for(PlacementNode plNode : allNodes)
			{
				if (putIn1) part1.allNodes.add(plNode); else
					part2.allNodes.add(plNode);
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
					PlacementNode plNode1 = part1.allNodes.get(i);
					for(int j=0; j<part2.allNodes.size(); j++)
					{
						PlacementNode plNode2 = part2.allNodes.get(j);

						// swap them
						part1.allNodes.set(i, plNode2);
						part2.allNodes.set(j, plNode1);

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
						part1.allNodes.set(i, plNode1);
						part2.allNodes.set(j, plNode2);
					}
				}

				// stop now if no gain was found
				if (bestGain == 0) break;

				// enter the gain, remove these nodes, and continue to search
				PlacementNode plNode1 = part1.allNodes.get(group1Member);
				PlacementNode plNode2 = part2.allNodes.get(group2Member);
				if (DEBUG) System.out.println("SWAPPING NODES "+plNode1+" AND "+plNode2+" FOR A GAIN OF "+bestGain);
				part1.allNodes.remove(group1Member);
				part2.allNodes.remove(group2Member);
				SwapNodes g = new SwapNodes(plNode1, plNode2);
				allSwaps.add(g);
			}

			// put the swaps back in, swapped
			for(SwapNodes g : allSwaps)
			{
				part1.allNodes.add(g.plNode2);
				part2.allNodes.add(g.plNode1);
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
			for(PlacementNode plNode1 : part1.allNodes)
			{
				for(PlacementNode plNode2 : part2.allNodes)
					cuts += getConnectivity(plNode1, plNode2);
			}
			return cuts;
		}
	}

	private static class SwapNodes
	{
		PlacementNode plNode1, plNode2;

		SwapNodes(PlacementNode n1, PlacementNode n2)
		{
			plNode1 = n1;
			plNode2 = n2;
		}
	}

	/**
	 * Method to return the number of connections between two PlacementNodes.
	 * @param plNode1 the first PlacementNode.
	 * @param plNode2 the second PlacementNode.
	 * @return the number of connections between the PlacementNodes.
	 */
	private int getConnectivity(PlacementNode plNode1, PlacementNode plNode2)
	{
		Map<PlacementNode,MutableInteger> destMap = connectivityMap.get(plNode1);
		if (destMap == null) return 0;
		MutableInteger mi = destMap.get(plNode2);
		if (mi == null) return 0;
		return mi.intValue();
	}

	/**
	 * Method to build the connectivity map by adding a connection between two PlacementNodes.
	 * This method is usually called twice with the PlacementNodes in both orders because
	 * the mapping is not symmetric.
	 * @param plNode1 the first PlacementNode.
	 * @param plNode2 the second PlacementNode.
	 */
	private void incrementMap(PlacementNode plNode1, PlacementNode plNode2)
	{
		Map<PlacementNode,MutableInteger> destMap = connectivityMap.get(plNode1);
		if (destMap == null)
			connectivityMap.put(plNode1, destMap = new HashMap<PlacementNode,MutableInteger>());
		MutableInteger mi = destMap.get(plNode2);
		if (mi == null) destMap.put(plNode2, mi = new MutableInteger(0));
		mi.increment();
	}
}
