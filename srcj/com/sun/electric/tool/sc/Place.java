/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Place.java
 * Silicon compiler tool (QUISC): placement
 * Written by Andrew R. Kostiuk, Queen's University.
 * Translated to Java by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.sc;

import com.sun.electric.database.hierarchy.Cell;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * The placement part of the Silicon Compiler tool.
 */
public class Place
{
	/** for debugging output */				private static final boolean DEBUG = false;
	/** TRUE = sort cluster tree */			private static final boolean SORTFLAG = true;
	/** TRUE = do net balance */			private static final boolean BALANCEFLAG = false;
	/** limit of movement */				private static final int     BALANCELIMIT = 2;
	/** scaling factor */					private static final int     VERTICALCOST = 2;

	/***** general placement information *****/
	static class SCPlace
	{
		/** number of instances */					int				numInst;
		/** total size of instances */				int				sizeInst;
		/** average size of inst */					int				avgSize;
		/** average height of inst */				int				avgHeight;
		/** number of rows */						int				numRows;
		/** target size of each row */				int				sizeRows;
		/** rows of placed cells */					List<RowList>   theRows;
		/** start of cell list */					NBPlace			plist;
		/** end of cell list */						NBPlace			endList;
	};

	private static final int BITS_PLACED	= 0x01;
	static final int BITS_EXTRACT			= 0x02;

	private static class Cluster
	{
		/** instance of cluster */					GetNetlist.SCNiTree node;
		/** number of cluster */					int				number;
		/** total size of members */				double			size;
	};

	private static class ClusterTree
	{
		/** cluster, null if intermediate node*/	Cluster			cluster;
		/** working bits */							int				bits;
		/** parent node */							ClusterTree		parent;
		/** pointer to nodes on same level */		ClusterTree		next;
		/** pointer to one group */					ClusterTree		lPtr;
		/** pointer to second group */				ClusterTree		rPtr;
	};

	private static class ClConnect
	{
		/** pointers to names of nodes */			ClusterTree  [] node;
		/** number of connections */				int				count;

		private ClConnect(ClusterTree ct0, ClusterTree ct1, int c)
		{
			node = new ClusterTree[2];
			node[0] = ct0;
			node[1] = ct1;
			count = c;
		}
	};

	static class RowList
	{
		/** start of row cells */					NBPlace		start;
		/** end of row cells */						NBPlace		end;
		/** row number (0 = bottom) */				int			rowNum;
		/** current row size */						int			rowSize;
	};

	static class NBPlace
	{
		/** pointer to cell */						GetNetlist.SCNiTree cell;
		/** x position (0 at left) */				double		xPos;
		/** pointer to last in list */				NBPlace		last;
		/** pointer to right in list */				NBPlace		next;
	};

	private static class Channel
	{
		/** number of channel */					int			number;
		/** list of trunks */						NBTrunk		trunks;
	};

	private static class NBTrunk
	{
		/** pointer to extracted node */			GetNetlist.ExtNode ext_node;
		/** minimum trunk going left */				double		minX;
		/** maximum trunk going right */			double		maxX;
		/** same in next channel */					NBTrunk		same;
		/** pointer to next trunk */				NBTrunk		next;
	};

	/************************* File variables *************************/

	/** global root of cluster tree */		private ClusterTree   gClusterTree;
	/** cost of current cluster tree */		private int           currentCost;

	/**
	 * Method to place the nodes in the current cell in optimal position for routing
	 * based upon number of connections between cells.
	 *
	 * Cluster Tree Creation:
	 *    o  Maxi-cut Algorithm
	 *    o  Minimum use for best pair choice for equal weight
	 * Cluster Tree Sorting:
	 *    o  Top-down cluster tree sort
	 *    o  Bottom-up cluster tree sort
	 * Net Balancing:
	 *    o  Independent routing channels
	 *    o  Cross refencing of trunks to improve speed
	 *    o  Effective calculation of rows in costing
	 */
	public String placeCells(GetNetlist gnl)
	{
		// check to see if currently working in a cell
		if (gnl.curSCCell == null) return "No cell selected";

		// create placement structure
		SCPlace place = new SCPlace();
		gnl.curSCCell.placement = place;
		place.numInst = 0;
		place.sizeInst = 0;
		place.avgSize = 0;
		place.avgHeight = 0;
		place.numRows = SilComp.getNumberOfRows();
		place.sizeRows = 0;
		place.theRows = new ArrayList<RowList>();
		place.plist = null;
		place.endList = null;

		// create clusters of cells
		List<Cluster> clusters = createClusters(gnl.curSCCell);
		int numCl = clusters.size();
		if (numCl == 0)
		{
			System.out.println("ERROR - No cells found to place.  Aborting.");
			return null;
		}

		// if there are fewer cells than rows, decrease the number of rows
		if (numCl < place.numRows) place.numRows = numCl;

		// create a cluster tree node for each cluster
		ClusterTree nStart = null;
		for (Cluster clus : clusters)
		{
			ClusterTree node = new ClusterTree();
			node.cluster = clus;
			node.parent = null;
			node.next = nStart;
			nStart = node;
			node.lPtr = null;
			node.rPtr = null;
		}

		// recursively create cluster tree
		gClusterTree = createCTreeRecurse(nStart, gnl.curSCCell);
		cTreeAddParents(gClusterTree, null);

		if (DEBUG)
		{
			System.out.println("************ Initial placement of Clusters");
			printClusterTree(gClusterTree, 0);
		}
		place.sizeRows = place.sizeInst / place.numRows;

		// place clusters in list by sorting groups
		if (SORTFLAG)
		{
			sortClusterTree(gClusterTree, gnl.curSCCell);
			if (DEBUG)
			{
				System.out.println("************ Placement of Clusters after Sorting");
				printClusterTree(gClusterTree, 0);
			}
		}

		// create first row structure
		RowList row = new RowList();
		row.start = null;
		row.end = null;
		row.rowNum = 0;
		row.rowSize = 0;
		gnl.curSCCell.placement.theRows.add(row);

		// create cell placement list from sorted cluster list
		createPlaceList(gClusterTree, gnl.curSCCell);

		// number placement
		numberPlacement(gnl.curSCCell.placement.theRows);
		if (DEBUG)
		{
			System.out.println("************ Placement before Net Balancing");
			showPlacement(gnl.curSCCell.placement.theRows);
		}

		// do net balance algorithm
		if (BALANCEFLAG)
		{
			netBalance(gnl.curSCCell);
			if (DEBUG)
			{
				System.out.println("************ Placement after Net Balancing");
				showPlacement(gnl.curSCCell.placement.theRows);
			}
		}

		// print process time for placement
		reorderRows(gnl.curSCCell.placement.theRows);

		return null;
	}

	/**
	 * Method to add the parent pointer to the cluster tree by doing a preorder transversal.
	 * @param node pointer to current node in transversal.
	 * @param parent pointer to parent node.
	 */
	private void cTreeAddParents(ClusterTree node, ClusterTree parent)
	{
		if (node == null) return;
		node.parent = parent;
		cTreeAddParents(node.lPtr, node);
		cTreeAddParents(node.rPtr, node);
	}

	/**
	 * Method to create "clusters" of cells of size one.
	 * @param cell pointer to complex cell.
	 * @return list of clusters.
	 */
	private List<Cluster> createClusters(GetNetlist.SCCell cell)
	{
		// find total 'size' and number of all the cells
		int size = 0;
		int num = 0;
		int height = 0;
		for (GetNetlist.SCNiTree iList : cell.niList)
		{
			if (iList.type == GetNetlist.LEAFCELL)
			{
				num++;
				size += iList.size;
				height += SilComp.leafCellYSize((Cell)iList.np);
			}
		}

		List<Cluster> clusters = new ArrayList<Cluster>();
		if (num == 0)
		{
			System.out.println("WARNING - No leaf cells found for placement");
			return clusters;
		}
		int avgSize = size / num;
		int avgHeight = height / num;
		if (DEBUG)
		{
			System.out.println("************ Cell Statistics");
			System.out.println("    Number of cells         = " + num);
			System.out.println("    Total length            = " + size);
			System.out.println("    Average size of cells   = " + avgSize);
			System.out.println("    Average height of cells = " + avgHeight);
		}
		cell.placement.numInst = num;
		cell.placement.sizeInst = size;
		cell.placement.avgSize = avgSize;
		cell.placement.avgHeight = avgHeight;

		// create cluster list
		int i = 0;
		boolean warn = false;
		for (GetNetlist.SCNiTree node : cell.niList)
		{
			if (node.type != GetNetlist.LEAFCELL)
			{
				if (node.type == GetNetlist.COMPLEXCELL) warn = true;
				continue;
			}
			Cluster cluster = new Cluster();
			cluster.node = node;
			cluster.size = node.size;
			cluster.number = i++;
			clusters.add(cluster);
		}
		if (warn)
		{
			System.out.println("WARNING - At least one complex cell found during Create_Clusters");
			System.out.println("        - Probable cause:  Forgot to do 'PULL' command");
		}

		return clusters;
	}

	/**
	 * Method to recursively create the cluster tree from the bottom up by pairing
	 * strongly connected tree nodes together.  When only one tree node
	 * exists, this is the root and can be written to the indicated address.
	 * @param nodes pointer to start of tree nodes.
	 * @param cell pointer to parent cell.
	 * @return tree root.
	 */
	private ClusterTree createCTreeRecurse(ClusterTree nodes, GetNetlist.SCCell cell)
	{
		// if no node, end
		if (nodes == null) return null;

		// if one node, write to root and end
		if (nodes.next == null) return nodes;

		// create list of connections between nodes
		List<ClConnect> connectList = cTreeNumConnects(nodes, cell);

		// pair by number of connects
		ClusterTree nStart = cTreePair(nodes, connectList);

		// recurse up a level
		return createCTreeRecurse(nStart, cell);
	}

	/**
	 * Method to create a list of the number of connections from all groups to all other groups.
	 * @param nodes List of current nodes.
	 * @param cell Pointer to parent cell.
	 */
	private List<ClConnect> cTreeNumConnects(ClusterTree nodes, GetNetlist.SCCell cell)
	{
		List<ClConnect> connections = new ArrayList<ClConnect>();
		int nodeNum = 0;

		// go through list of nodes
		for ( ; nodes != null; nodes = nodes.next)
		{
			// check all other node
			for (ClusterTree nextnode = nodes.next; nextnode != null; nextnode = nextnode.next)
			{
				nodeNum += 2;

				// mark all extracted nodes used by first node
				setExtNodesByCTree(nodes, nodeNum);

				// count number of common extracted nodes
				int common = countExtNodes(nextnode, nodeNum);

				if (common != 0)
				{
					ClConnect newCon = new ClConnect(nodes, nextnode, common);
					connections.add(newCon);
				}
			}
		}

		// sort number of connects from largest to smallest
        Collections.sort(connections, new ConnectsByCount());

        return connections;
	}

    private static class ConnectsByCount implements Comparator<ClConnect>
    {
        public int compare(ClConnect c1, ClConnect c2)
        {
            return c2.count - c1.count;
        }
    }

	/**
	 * Method to mark all extracted nodes references by any member of all the
	 * clusters in the indicated cluster tree.
	 * @param node pointer to cluster tree node.
	 * @param marker value to set flags field to.
	 */
	private void setExtNodesByCTree(ClusterTree node, int marker)
	{
		if (node == null) return;

		setExtNodesByCTree(node.lPtr, marker);

		// process node if cluster
		if (node.cluster != null)
		{
			// check every port of member
			for (GetNetlist.SCNiPort port = node.cluster.node.ports; port != null; port = port.next)
				port.extNode.flags = marker;
		}

		setExtNodesByCTree(node.rPtr, marker);
	}

	/**
	 * Method to return the number of extracted nodes which have flag bit set only
	 * and is accessed by subtree.
	 * @param node start of cluster tree node.
	 * @param marker value to look for.
	 */
	private int countExtNodes(ClusterTree node, int marker)
	{
		if (node == null) return 0;

		int count = countExtNodes(node.lPtr, marker);

		// process node if cluster
		if (node.cluster != null)
		{
			// check every port of member
			for (GetNetlist.SCNiPort port = node.cluster.node.ports; port != null; port = port.next)
			{
				if (port.extNode.flags == marker) count++;
			}
		}

		count += countExtNodes(node.rPtr, marker);
		return count;
	}

	/**
	 * Method to pair up the given nodes by using the information in the connection list.
	 * @param nodes pointer to start of list of nodes.
	 * @param nConnects pointer to start of list of connections.
	 * @return new list.
	 */
	private ClusterTree cTreePair(ClusterTree nodes, List<ClConnect> connectList)
	{
		// clear the placed flag in all tree nodes
		for (ClusterTree tPtr = nodes; tPtr != null; tPtr = tPtr.next)
			tPtr.bits &= ~BITS_PLACED;

		// go through connection list
		ClusterTree newStart = null;
		if (connectList.size() > 0)
		{
			for (int i=0; i<connectList.size(); )
			{
				ClConnect connect = (ClConnect)connectList.get(i);
	
				// if either placed, continue
				if ((connect.node[0].bits & BITS_PLACED) != 0 ||
					(connect.node[1].bits & BITS_PLACED) != 0)
				{
					i++;
					continue;
				}
	
				// get best choice
				ClConnect bConnect = bestPair(connectList, i);
	
				// create new cluster tree node
				ClusterTree newNode = new ClusterTree();
				newNode.cluster = null;
				newNode.bits = 0;
				newNode.parent = null;
				newNode.lPtr = bConnect.node[0];
				newNode.lPtr.parent = newNode;
				bConnect.node[0].bits |= BITS_PLACED;
				newNode.rPtr = bConnect.node[1];
				newNode.rPtr.parent = newNode;
				bConnect.node[1].bits |= BITS_PLACED;
				newNode.next = newStart;
				newStart = newNode;
	
				// remove from list
				connectList.remove(bConnect);
			}
		} else
		{
			// create new cluster tree node
			ClusterTree newNode = new ClusterTree();
			newNode.cluster = null;
			newNode.bits = 0;
			newNode.parent = null;
			newNode.lPtr = nodes;
			newNode.lPtr.parent = newNode;
			nodes.bits |= BITS_PLACED;
			newNode.rPtr = nodes.next;
			newNode.rPtr.parent = newNode;
			nodes.next.bits |= BITS_PLACED;
			newNode.next = newStart;
			newStart = newNode;
		}

		// add any remaining tree nodes as singular nodes
		for (ClusterTree tPtr = nodes; tPtr != null; tPtr = tPtr.next)
		{
			if ((tPtr.bits & BITS_PLACED) == 0)
			{
				// create new cluster tree node
				ClusterTree newNode = new ClusterTree();
				newNode.cluster = null;
				newNode.bits = 0;
				newNode.parent = null;
				newNode.lPtr = tPtr;
				newNode.lPtr.parent = newNode;
				tPtr.bits |= BITS_PLACED;
				newNode.rPtr = null;
				newNode.next = newStart;
				newStart = newNode;
			}
		}
		return newStart;
	}

	private static class Temp
	{
		ClusterTree	node;		/* cluster tree node */
		int			count;		/* number of times seen */
		ClConnect	ref;		/* first reference */
	}

	/**
	 * Method to find the best cluster connection.
	 * The best has both members unplaced, has the same weight as the one top on the list,
	 * and appears the smallest number of times.
	 * @param connect start of sorted list.
	 * @return pointer to best pair.
	 */
	private ClConnect bestPair(List<ClConnect> connectList, int index)
	{
		List<Temp> sList = new ArrayList<Temp>();
		ClConnect connect = (ClConnect)connectList.get(index);
		for(int oIndex=index; oIndex<connectList.size(); oIndex++)
		{
			ClConnect nConnect = (ClConnect)connectList.get(oIndex);
			if (nConnect.count < connect.count) break;
			if ((nConnect.node[0].bits & BITS_PLACED) != 0 ||
				(nConnect.node[1].bits & BITS_PLACED) != 0) continue;

			// check if nodes previously counted
			for(int i=0; i<2; i++)
			{
				Temp nList = null;
				for(Temp nl : sList)
				{
					if (nl.node == nConnect.node[i])
					{
						nList = nl;
						break;
					}
				}
				if (nList != null)
				{
					nList.count++;
				} else
				{
					nList = new Temp();
					nList.node = nConnect.node[i];
					nList.count = 1;
					nList.ref = nConnect;
					sList.add(nList);
				}
			}
		}

		// find the minimum count
		Temp best = null;
		for(Temp nList : sList)
		{
			if (best == null || nList.count <= best.count) best = nList;
		}
		return best.ref;
	}

	/**
	 * Method to sort the cluster tree into a list by sorting groups.
	 * Sorting attempts to optimize the placement of groups by
	 * minimizing length of connections between groups and locating groups
	 * close to any specified ports.
	 * @param cTree pointer to root of cluster tree.
	 * @param cell pointer to parent cell.
	 */
	private void sortClusterTree(ClusterTree cTree, GetNetlist.SCCell cell)
	{
		NBTrunk trunks = null;

		// create a list of trunks from the extracted nodes
		for (GetNetlist.ExtNode enode = cell.exNodes; enode != null; enode = enode.next)
		{
			NBTrunk newTrunk = new NBTrunk();
			newTrunk.ext_node = enode;
			newTrunk.minX = 0;
			newTrunk.maxX = 0;
			newTrunk.next = trunks;
			trunks = newTrunk;
			enode.ptr = newTrunk;		// back reference pointer
		}

		currentCost = costClusterTree(gClusterTree, trunks, cell);
		if (DEBUG)
			System.out.println("***** Cost of placement before cluster sorting = " + currentCost);

		// call top-down swapper
		sortSwapperTopDown(cTree, trunks, cell);

		// call bottom-up swapper
		sortSwapperBottomUp(cTree, trunks, cell);
		if (DEBUG)
			System.out.println("***** Cost of placement after cluster sorting = " + currentCost);
	}

	/**
	 * Method to do preorder transversal of cluster tree, swapping groups to try
	 * and sort tree into a more efficient placement.
	 * @param cTree root of cluster tree.
	 * @param trunks list of trunks for costing.
	 * @param cell pointer to parent cell.
	 */
	private void sortSwapperTopDown(ClusterTree cTree, NBTrunk trunks, GetNetlist.SCCell cell)
	{
		if (cTree == null) return;

		// process tree node if there are two subtrees
		if (cTree.lPtr != null && cTree.rPtr != null)
		{
			// swap groups
			switchSubtrees(cTree);

			// check new cost
			int cost2 = costClusterTree(gClusterTree, trunks, cell);

			// swap back if old cost is less than new
			if (currentCost < cost2)
			{
				switchSubtrees(cTree);
			} else
			{
				currentCost = cost2;
			}
		}

		sortSwapperTopDown(cTree.lPtr, trunks, cell);

		sortSwapperTopDown(cTree.rPtr, trunks, cell);
	}

	/**
	 * Method to do a postorder transversal of cluster tree, swapping groups to try
	 * and sort tree into a more efficient placement.
	 * @param cTree root of cluster tree.
	 * @param trunks list of trunks for costing.
	 * @param cell pointer to parent cell.
	 */
	private void sortSwapperBottomUp(ClusterTree cTree, NBTrunk trunks, GetNetlist.SCCell cell)
	{
		if (cTree == null) return;

		sortSwapperBottomUp(cTree.lPtr, trunks, cell);

		sortSwapperBottomUp(cTree.rPtr, trunks, cell);

		// process tree node if there are two subtrees
		if (cTree.lPtr != null && cTree.rPtr != null)
		{
			// swap groups
			switchSubtrees(cTree);

			// check new cost
			int cost2 = costClusterTree(gClusterTree, trunks, cell);

			// swap back if old cost is less than new
			if (currentCost < cost2)
			{
				switchSubtrees(cTree);
			} else
			{
				currentCost = cost2;
			}
		}
	}

	/**
	 * Method to switch the subtrees recursively to perform a mirror image operation along "main" axis.
	 * @param node pointer to top tree node.
	 */
	private void switchSubtrees(ClusterTree node)
	{
		if (node == null) return;

		ClusterTree temp = node.lPtr;
		node.lPtr = node.rPtr;
		node.rPtr = temp;
		switchSubtrees(node.lPtr);
		switchSubtrees(node.rPtr);
	}

	/**
	 * Method to return the "cost" of the indicated cluster tree sort.
	 * Cost is a function of the length of connections between clusters and placement to ports.
	 * @param cTree pointer to cluster tree node.
	 * @param trunks pointer to trunks to use to cost.
	 * @param cell pointer to parent cell.
	 * @return cost.
	 */
	private int costClusterTree(ClusterTree cTree, NBTrunk trunks, GetNetlist.SCCell cell)
	{
		// clear trunks to record lengths
		for (NBTrunk nTrunk = trunks; nTrunk != null; nTrunk = nTrunk.next)
		{
			nTrunk.minX = -1;
			nTrunk.maxX = -1;
		}

		// set trunks lengths
		double pos = costClusterTree2(cTree, trunks, 0);

		// calculate cost
		int cost = 0;
		for (NBTrunk nTrunk = trunks; nTrunk != null; nTrunk = nTrunk.next)
		{
			if (nTrunk.minX < 0) continue;
			cost += nTrunk.maxX - nTrunk.minX;
		}

		for (GetNetlist.SCPort pport = cell.ports; pport != null; pport = pport.next)
		{
			if ((pport.bits & GetNetlist.PORTDIRMASK) == 0) continue;
			NBTrunk nTrunk = (NBTrunk)pport.node.ports.extNode.ptr;
			if (nTrunk == null) continue;
			if ((pport.bits & GetNetlist.PORTDIRUP) != 0)
			{
				// add distance to top row
				int row = (int)(nTrunk.maxX / cell.placement.sizeRows);
				if ((row + 1) < cell.placement.numRows)
				{
					cost += (cell.placement.numRows - row - 1) *
						cell.placement.avgHeight * VERTICALCOST;
				}
			}
			if ((pport.bits & GetNetlist.PORTDIRDOWN) != 0)
			{
				// add distance to bottom row
				int row = (int)(nTrunk.minX / cell.placement.sizeRows);
				if (row != 0)
				{
					cost += row * cell.placement.avgHeight * VERTICALCOST;
				}
			}
		}

		return cost;
	}

	/**
	 * Method to set the limits of the trunks by doing an inorder transversal of the cluster tree.
	 * @param cTree pointer to cluster tree node.
	 * @param trunks pointer to trunks to use to cost.
	 * @param pos current position.
	 */
	private double costClusterTree2(ClusterTree cTree, NBTrunk trunks, double pos)
	{
		if (cTree == null) return pos;

		// do all nodes left
		pos = costClusterTree2(cTree.lPtr, trunks, pos);

		// process node
		if (cTree.cluster != null)
		{
			for (GetNetlist.SCNiPort port = cTree.cluster.node.ports; port != null; port = port.next)
			{
				NBTrunk nTrunk = (NBTrunk)port.extNode.ptr;
				if (nTrunk == null) continue;
				if (nTrunk.minX < 0)
					nTrunk.minX = pos + port.xPos;
				nTrunk.maxX = pos + port.xPos;
			}
			pos += cTree.cluster.size;
		}

		// do all nodes right
		return costClusterTree2(cTree.rPtr, trunks, pos);
	}

	/**
	 * Module to create the placement list by simply taking the clusters from the
	 * sorted cluster list and placing members in a snake pattern.
	 * Do an inorder transversal to create placelist.
	 * @param cTree pointer to start of cluster tree.
	 * @param cell pointer to parent cell.
	 */
	private void createPlaceList(ClusterTree cTree, GetNetlist.SCCell cell)
	{
		if (cTree == null) return;

		createPlaceList(cTree.lPtr, cell);

		// add clusters to placement list
		if (cTree.cluster != null)
		{
			addClusterToRow(cTree.cluster, cell.placement.theRows, cell.placement);
		}

		createPlaceList(cTree.rPtr, cell);
	}

	/**
	 * Method to add the members of the passed cluster to the indicated row.
	 * Add new rows as necessary and also maintain a global placement
	 * bidirectional list.
	 * @param culster pointer to cluster to add.
	 * @param row pointer to the current row.
	 * @param place pointer to placement information.
	 */
	private void addClusterToRow(Cluster cluster, List<RowList> theRows, SCPlace place)
	{
		if (cluster.node.type != GetNetlist.LEAFCELL) return;
		NBPlace newPlace = new NBPlace();
		newPlace.cell = cluster.node;
		newPlace.xPos = 0;
		cluster.node.tp = newPlace;
		newPlace.next = null;
		newPlace.last = place.endList;
		if (place.endList == null)
		{
			place.plist = place.endList = newPlace;
		} else
		{
			place.endList.next = newPlace;
			place.endList = newPlace;
		}

		// get the last entry in the list
		RowList row = (RowList)theRows.get(theRows.size() - 1);
		double oldCondition = place.sizeRows - row.rowSize;
		double newCondition = place.sizeRows - (row.rowSize + cluster.node.size);
		if ((row.rowNum + 1) < place.numRows &&
			Math.abs(oldCondition) < Math.abs(newCondition))
		{
			RowList row2 = new RowList();
			row2.start = null;
			row2.end = null;
			row2.rowNum = row.rowNum + 1;
			row2.rowSize = 0;
			theRows.add(row2);
			row = row2;
		}

		// add to row
		if ((row.rowNum % 2) != 0)
		{
			// odd row
			if (row.end == null)
				row.end = newPlace;
			row.start = newPlace;
		} else
		{
			// even row
			if (row.start == null)
				row.start = newPlace;
			row.end = newPlace;
		}
		row.rowSize += cluster.node.size;
	}

	/**
	 * Method to do a net balancing on the placelist.
	 * @param row pointer to start of row list.
	 * @param cell pointer to parent cell.
	 */
	private void netBalance(GetNetlist.SCCell cell)
	{
		// create channel list
		List<Channel> channels = new ArrayList<Channel>();
		int i = 0;
		NBTrunk sameTrunk = null;
		do
		{
			Channel newChan = new Channel();
			newChan.number = i;
			NBTrunk trunks = null, oldTrunk = null;

			// create trunk list for each channel
			for (GetNetlist.ExtNode nList = cell.exNodes; nList != null; nList = nList.next)
			{
				NBTrunk nTrunk = new NBTrunk();
				nTrunk.ext_node = nList;
				nTrunk.minX = 0;
				nTrunk.maxX = 0;
				nTrunk.same = null;
				if (sameTrunk == null)
				{
					nList.ptr = nTrunk;
				} else
				{
					sameTrunk.same = nTrunk;
					sameTrunk = sameTrunk.next;
				}
				nTrunk.next = null;
				if (oldTrunk == null)
				{
					trunks = oldTrunk = nTrunk;
				} else
				{
					oldTrunk.next = nTrunk;
					oldTrunk = nTrunk;
				}
			}
			newChan.trunks = trunks;
			channels.add(newChan);
			sameTrunk = trunks;
			i++;
		} while ((i + 1) < cell.placement.numRows);

		// report current placement evaluation
		if (DEBUG)
			System.out.println("Evaluation before Net-Balancing  = " +
				nBCost(cell.placement.theRows, channels, cell));

		// do the net balance for each cell
		nBAllCells(cell, channels);

		// number placement
		nBRebalanceRows(cell.placement.theRows, cell.placement);
		numberPlacement(cell.placement.theRows);

		// report new evaluation
		if (DEBUG)
			System.out.println("Evaluation after Net-Balancing   = %d" +
				nBCost(cell.placement.theRows, channels, cell));
	}

	/**
	 * Method to do a net balance for each cell on at a time.  Use the SCNiTree to
	 * insure that each cell is processed.
	 * @param cell pointer to parent cell.
	 * @param channels pointer to start of channel list.
	 */
	private void nBAllCells(GetNetlist.SCCell cell, List<Channel> channels)
	{
		// process cell
		for (GetNetlist.SCNiTree iList : cell.niList)
		{
			if (iList.type == GetNetlist.LEAFCELL)
				nBDoCell((NBPlace)iList.tp, channels, cell);
		}
	}

	/**
	 * Method to do a net balance for the indicated instance.
	 * @param place pointer to place of instance.
	 * @param channels pointer to channel list of trunks.
	 * @param cell parent complex cell.
	 */
	private void nBDoCell(NBPlace place, List<Channel> channels, GetNetlist.SCCell cell)
	{
		if (place == null) return;

		// find cost at present location and set as current minimum
		List<RowList> theRows = cell.placement.theRows;
		int minCost = nBCost(theRows, channels, cell);
		int pos = 0;

		// temporarily remove from list
		NBPlace oldLast = place.last;
		NBPlace oldNext = place.next;
		nBRemove(place, theRows);

		// check locations backwards for nb_limit
		int nPos = -1;
		for (NBPlace nPlace = oldLast; nPos >= -BALANCELIMIT; nPlace = nPlace.last)
		{
			if (nPlace != null)
			{
				// temporarily insert in list
				nBInsertBefore(place, nPlace, theRows);

				// check new cost
				int cost = nBCost(theRows, channels, cell);
				if (cost < minCost)
				{
					minCost = cost;
					pos = nPos;
				}

				// remove place from list
				nBRemove(place, theRows);
			} else
			{
				break;
			}
			nPos--;
		}

		// check forward locations for nb_limit
		nPos = 1;
		for (NBPlace nPlace = oldNext; nPos < BALANCELIMIT; nPlace = nPlace.next)
		{
			if (nPlace != null)
			{
				// temporarily insert in list
				nBInsertAfter(place, nPlace, theRows);

				// check new cost
				int cost = nBCost(theRows, channels, cell);
				if (cost < minCost)
				{
					minCost = cost;
					pos = nPos;
				}

				// remove place from list
				nBRemove(place, theRows);
			} else
			{
				break;
			}
			nPos++;
		}

		// move if necessary
		if (pos > 0)
		{
			while(pos-- > 1)
			{
				oldNext = oldNext.next;
			}
			nBInsertAfter(place, oldNext, theRows);
		} else if (pos < 0)
		{
			while(pos++ < -1)
			{
				oldLast = oldLast.last;
			}
			nBInsertBefore(place, oldLast, theRows);
		} else
		{
			if (oldLast != null)
			{
				nBInsertAfter(place, oldLast, theRows);
			} else
			{
				nBInsertBefore(place, oldNext, theRows);
			}
		}
	}

	/**
	 * Method to return cost of the indicated placement.
	 * @param rows pointer to start of list or rows.
	 * @param channels pointer to list of channels.
	 * @param cell pointer to parent cell.
	 * @return cost.
	 */
	private int nBCost(List<RowList> theRows, List<Channel> channels, GetNetlist.SCCell cell)
	{
		// initialize all trunks
		for(Channel nChan : channels)
		{
			for (NBTrunk nTrunk = nChan.trunks; nTrunk != null; nTrunk = nTrunk.next)
			{
				nTrunk.minX = Double.MAX_VALUE;
				nTrunk.maxX = Double.MIN_VALUE;
			}
		}

		// check all rows
		int chanPos = 0;
		Channel nChan = (Channel)channels.get(chanPos);
		boolean above = true;
		int dis = 0;
		int rowNum = 0;
		int maxRowSize = cell.placement.sizeRows + (cell.placement.avgSize >>1);
		RowList rows = (RowList)theRows.get(0);
		for (NBPlace nPlace = rows.start; nPlace != null; nPlace = nPlace.next)
		{
			// check for room in current row
			if ((rowNum % 2) != 0)
			{
				// odd row
				if ((dis - nPlace.cell.size) < 0)
				{
					if ((rowNum + 1) < cell.placement.numRows)
					{
						rowNum++;
						dis = 0;
						if (above ^= true)
						{
							chanPos++;
							nChan = (Channel)channels.get(chanPos);
						}
					}
				}
			} else
			{
				// even row
				if ((dis + nPlace.cell.size) > maxRowSize)
				{
					if ((rowNum + 1) < cell.placement.numRows)
					{
						rowNum++;
						dis = maxRowSize;
						if (above ^= true)
						{
							chanPos++;
							nChan = (Channel)channels.get(chanPos);
						}
					}
				}
			}

			// check all ports on instance
			for (GetNetlist.SCNiPort port = nPlace.cell.ports; port != null; port = port.next)
			{
				// find the correct trunk
				NBTrunk nTrunk = (NBTrunk)port.extNode.ptr;
				if (nTrunk == null) continue;
				for (int i = nChan.number; i != 0; i--)
					nTrunk = nTrunk.same;
				if (nTrunk.minX == Double.MAX_VALUE)
				{
					if (!above && nTrunk.same != null)
						nTrunk = nTrunk.same;
				}
				double pos = 0;
				if ((rowNum % 2) != 2)
				{
					pos = dis - port.xPos;
				} else
				{
					pos = dis + port.xPos;
				}
				nTrunk.minX = Math.min(nTrunk.minX, pos);
				nTrunk.maxX = Math.max(nTrunk.maxX, pos);
			}
			if ((rowNum % 2) != 2)
			{
				dis -= nPlace.cell.size;
			} else
			{
				dis += nPlace.cell.size;
			}
		}

		// calculate cost
		int cost = 0;

		// calculate horizontal costs
		for(Channel aChan : channels)
		{
			nChan = aChan;
			for (NBTrunk nTrunk = nChan.trunks; nTrunk != null; nTrunk = nTrunk.next)
			{
				if (nTrunk.minX != Double.MAX_VALUE)
					cost += Math.abs(nTrunk.maxX - nTrunk.minX);
			}
		}

		// calculate vertical cost
		for (NBTrunk nTrunk = ((Channel)channels.get(0)).trunks; nTrunk != null; nTrunk = nTrunk.next)
		{
			NBTrunk fTrunk = null;
			int fCount = 0, count = 0;
			for (NBTrunk sTrunk = nTrunk; sTrunk != null; sTrunk = sTrunk.same)
			{
				if (sTrunk.minX != Double.MAX_VALUE)
				{
					double fMinX = 0, fMaxX = 0;
					if (fTrunk == null)
					{
						fTrunk = sTrunk;
						fMinX = sTrunk.minX;
						fMaxX = sTrunk.maxX;
						fCount = count;
					} else
					{
						// add new vertical
						cost += (count - fCount) * cell.placement.avgHeight * VERTICALCOST;
						fCount = count;

						// additional horizontal
						if (fMaxX < sTrunk.minX)
						{
							cost += Math.abs(sTrunk.minX - fMaxX);
							fMaxX = sTrunk.maxX;
						} else if (fMinX > sTrunk.maxX)
						{
							cost += Math.abs(fMinX - sTrunk.maxX);
							fMinX = sTrunk.minX;
						} else
						{
							if (fMinX > sTrunk.minX) fMinX = sTrunk.minX;
							if (fMaxX < sTrunk.maxX) fMaxX = sTrunk.maxX;
						}

					}
				}
				count++;
			}
		}

		return cost;
	}

	/**
	 * Method to remove the indicated placed instance and clean up the rows structures.
	 * @param place pointer to place to be removed.
	 * @param rows pointer to start of row list.
	 */
	private void nBRemove(NBPlace place, List<RowList> theRows)
	{
		NBPlace oldNext = place.next;
		NBPlace oldLast = place.last;
		if (place.last != null)
			place.last.next = oldNext;
		if (place.next != null)
			place.next.last = oldLast;

		// check if row change
		for(RowList row : theRows)
		{
			if (row.start == place)
			{
				if ((row.rowNum % 2) != 0)
				{
					row.start = oldLast;
				} else
				{
					row.start = oldNext;
				}
			}
			if (row.end == place)
			{
				if ((row.rowNum % 2) != 2)
				{
					row.end = oldNext;
				} else
				{
					row.end = oldLast;
				}
			}
		}
	}

	/**
	 * Module to insert the indicated place before the indicated second place and
	 * clear up the row markers if necessary.
	 * @param place pointer to place to be inserted.
	 * @param oldPlace pointer to place to be inserted before.
	 * @param rows start of list of row markers.
	 */
	private void nBInsertBefore(NBPlace place, NBPlace oldPlace, List<RowList> theRows)
	{
		place.next = oldPlace;
		if (oldPlace != null)
		{
			place.last = oldPlace.last;
			if (oldPlace.last != null)
				oldPlace.last.next = place;
			oldPlace.last = place;
		} else
		{
			place.last = null;
		}

		// check if row change
		for(RowList row : theRows)
		{
			if (row.start == oldPlace)
			{
				if ((row.rowNum % 2) != 0)
				{
					// EMPTY
				} else
				{
					row.start = place;
				}
			}
			if (row.end == oldPlace)
			{
				if ((row.rowNum % 2) != 0)
					row.end = place;
			}
		}
	}

	/**
	 * Method to insert the indicated place after the indicated second place and
	 * clear up the row markers if necessary.
	 * @param place pointer to place to be inserted.
	 * @param oldPlace pointer to place to be inserted after.
	 * @param rows start of list of row markers.
	 */
	private void nBInsertAfter(NBPlace place, NBPlace oldPlace, List<RowList> theRows)
	{
		place.last = oldPlace;
		if (oldPlace != null)
		{
			place.next = oldPlace.next;
			if (oldPlace.next != null)
				oldPlace.next.last = place;
			oldPlace.next = place;
		} else
		{
			place.next = null;
		}

		// check if row change
		RowList rows = (RowList)theRows.get(0);
		for (RowList row : theRows)
		{
			if (row.start == oldPlace)
			{
				if ((row.rowNum % 2) != 0)
					row.start = place;
			}
			if (row.end == oldPlace)
			{
				if ((rows.rowNum % 2) != 0)
				{
					// EMPTY
				} else
				{
					row.end = place;
				}
			}
		}
	}

	/**
	 * Method to check balancing for rows as there has been a change in placement.
	 * @param rows pointer to start of row list.
	 * @param place pointer to global placement structure.
	 */
	private void nBRebalanceRows(List<RowList> theRows, SCPlace place)
	{
		int maxRowSize = place.sizeRows + (place.avgSize >> 1);
		int rowPos = 0;
		RowList rows = (RowList)theRows.get(rowPos);
		rows.rowSize = 0;
		for (NBPlace nPlace = rows.start; nPlace != null; nPlace = nPlace.next)
		{
			if ((rows.rowNum + 1) < place.numRows &&
				(rows.rowSize + nPlace.cell.size) > maxRowSize)
			{
				rowPos++;
				rows = (RowList)theRows.get(rowPos);
				rows.rowSize = 0;
				if ((rows.rowNum % 2) != 0)
				{
					rows.end = nPlace;
				} else
				{
					rows.start = nPlace;
				}
			}
			rows.rowSize += nPlace.cell.size;
			if ((rows.rowNum % 2) != 0)
			{
				rows.start = nPlace;
			} else
			{
				rows.end = nPlace;
			}
		}
	}

	/**
	 * Method to number the x position of all the cells in their rows.
	 * @param rows pointer to the start of the rows.
	 */
	private void numberPlacement(List<RowList> theRows)
	{
		for (RowList row : theRows)
		{
			int xPos = 0;
			NBPlace nPlace = row.start;
			while (nPlace != null)
			{
				nPlace.xPos = xPos;
				xPos += nPlace.cell.size;
				if (nPlace == row.end) break;
				if ((row.rowNum % 2) != 0)
				{
					nPlace = nPlace.last;
				} else
				{
					nPlace = nPlace.next;
				}
			}
		}
	}

	/**
	 * Method to clean up the placement rows structure by reversing the pointers
	 * of odd rows and breaking the snake pattern by row.
	 * @param rows pointer to start of row list.
	 */
	private void reorderRows(List<RowList> theRows)
	{
		for(RowList row : theRows)
		{
			if ((row.rowNum % 2) != 0)
			{
				// odd row
				for (NBPlace place = row.start; place != null; place = place.next)
				{
					NBPlace tPlace = place.next;
					place.next = place.last;
					place.last = tPlace;
					if (place == row.end) break;
				}
				row.start.last = null;
				row.end.next = null;
			} else
			{
				// even row
				row.start.last = null;
				row.end.next = null;
			}
		}
	}

	/**
	 * Method to print the cells in their rows of placement.
	 * @param rows pointer to the start of the rows.
	 */
	private void showPlacement(List<RowList> theRows)
	{
		for (RowList row : theRows)
		{
			System.out.println("For Row #" + row.rowNum + ", size " + row.rowSize+ ":");
			NBPlace inst;
			for (inst = row.start; inst != row.end;)
			{
				System.out.println("    " + inst.xPos + "    " + inst.cell.name);
				if ((row.rowNum % 2) != 0)
				{
					inst = inst.last;
				} else
				{
					inst = inst.next;
				}
			}
			System.out.println("    " + inst.xPos + "    " + inst.cell.name);
		}
	}

	/**
	 * Method to print the cluster placement tree by doing an inorder transversal.
	 * @param node pointer to cluster tree node.
	 * @param level current level of tree (0 = root).
	 */
	private void printClusterTree(ClusterTree node, int level)
	{
		if (node == null) return;

		printClusterTree(node.lPtr, level + 1);

		// process node
		int i = level << 2;
		StringBuffer sb = new StringBuffer();
		for(int j=0; j<i; j++) sb.append(" ");

		if (node.cluster != null)
		{
			sb.append("Cell " + node.cluster.node.name);
		} else
		{
			sb.append(level + "**");
			i = 36 - (level << 2);
			for(int j=0; j<i; j++) sb.append(" ");
		}
		System.out.println(sb.toString());

		printClusterTree(node.rPtr, level + 1);
	}

}
