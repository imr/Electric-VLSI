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
import com.sun.electric.database.text.TextUtils;

import java.util.Iterator;

/**
 * This is the placement part of the Silicon Compiler tool.
 */
public class Place
{
	private static final boolean DEBUG = false;

	/***** general placement information *****/
	public static class SCPLACE
	{
		/** number of instances */					int				num_inst;
		/** total size of instances */				int				size_inst;
		/** average size of inst */					int				avg_size;
		/** average height of inst */				int				avg_height;
		/** number of rows */						int				num_rows;
		/** target size of each row */				int				size_rows;
		/** rows of placed cells */					SCROWLIST		rows;
		/** start of cell list */					SCNBPLACE		plist;
		/** end of cell list */						SCNBPLACE		endlist;
	};

	private static final int SCBITS_PLACED		= 0x01;
	public static final int SCBITS_EXTRACT		= 0x02;

	private static class SCCLUSTER
	{
		/** instance of cluster */					SilComp.SCNITREE node;
		/** number of cluster */					int				number;
		/** total size of members */				double			size;
		/** pointer to last cluster */				SCCLUSTER		last;
		/** pointer to next cluster */				SCCLUSTER		next;
	};

	private static class SCCLUSTERTREE
	{
		/** cluster, null if intermediate node*/	SCCLUSTER		cluster;
		/** working bits */							int				bits;
		/** parent node */							SCCLUSTERTREE	parent;
		/** pointer to nodes on same level */		SCCLUSTERTREE	next;
		/** pointer to one group */					SCCLUSTERTREE	lptr;
		/** pointer to second group */				SCCLUSTERTREE	rptr;
	};

	private static class SCCLCONNECT
	{
		/** pointers to names of nodes */			SCCLUSTERTREE [] node;
		/** number of connections */				int				count;
		/** pointer to next list element */			SCCLCONNECT		next;
		/** pointer to previous list element*/		SCCLCONNECT		last;

		private SCCLCONNECT() { node = new SCCLUSTERTREE[2]; }
	};

	public static class SCROWLIST
	{
		/** start of row cells */					SCNBPLACE	start;
		/** end of row cells */						SCNBPLACE	end;
		/** row number (0 = bottom) */				int			row_num;
		/** current row size */						int			row_size;
		/** next in row list */						SCROWLIST	next;
		/** last in row list */						SCROWLIST	last;
	};

	public static class SCNBPLACE
	{
		/** pointer to cell */						SilComp.SCNITREE cell;
		/** x position (0 at left) */				double		xpos;
		/** pointer to last in list */				SCNBPLACE	last;
		/** pointer to right in list */				SCNBPLACE	next;
	};

	private static class SCCHANNEL
	{
		/** number of channel */					int			number;
		/** list of trunks */						SCNBTRUNK	trunks;
		/** last in list of channels */				SCCHANNEL	last;
		/** next in list of channels */				SCCHANNEL	next;
	};

	private static class SCNBTRUNK
	{
		/** pointer to extracted node */			SilComp.SCEXTNODE ext_node;
		/** minimum trunk going left */				double		minx;
		/** maximum trunk going right */			double		maxx;
		/** same in next channel */					SCNBTRUNK	same;
		/** pointer to next trunk */				SCNBTRUNK	next;
	};

	private static final int SC_PLACE_SORT_ALL_TREES	= 0x0000000F;
	private static final int SC_PLACE_SORT_TREE_0	= 0x00000001;
	private static final int SC_PLACE_SORT_TREE_1	= 0x00000002;
	private static final int SC_PLACE_SORT_TREE_2	= 0x00000004;
	private static final int SC_PLACE_SORT_TREE_3	= 0x00000008;
	private static final int SC_PLACE_SORT_MASK_1	= 0x0000000D;
	private static final int SC_PLACE_SORT_MASK_2	= 0x0000000B;
	private static final int SC_PLACE_SORT_CASE_1	= 0x00000005;
	private static final int SC_PLACE_SORT_CASE_2	= 0x0000000A;

	private static final int SCEXTNODECLUSE	= 0x0003;
	private static final int SCEXTNODEGROUP1	= 0x0001;
	private static final int SCEXTNODEGROUP2	= 0x0002;

	/** TRUE = sort cluster tree */			private static boolean	sort_flag = true;
	/** TRUE = do net balance */			private static boolean	net_balance_flag = false;
	/** limit of movement */				private static int		net_balance_limit = 2;
	/** scaling factor */					private static int		vertical_cost = 2;

	/************************* File variables *************************/

	/** global list of cluster groups */	private static SCCLUSTER     sc_gcluster;
	/** global root of cluster tree */		private static SCCLUSTERTREE sc_gcluster_tree;
	/** cost of current cluster tree */		private static int           sc_currentcost;

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
	public static String Sc_place()
	{
		// check to see if currently working in a cell
		if (SilComp.sc_curcell == null) return "No cell selected";

		// create placement structure
//		Sc_free_placement(sc_curcell->placement);
		SCPLACE place = new SCPLACE();
		SilComp.sc_curcell.placement = place;
		place.num_inst = 0;
		place.size_inst = 0;
		place.avg_size = 0;
		place.avg_height = 0;
		place.num_rows = SilComp.getNumberOfRows();
		place.size_rows = 0;
		place.rows = null;
		place.plist = null;
		place.endlist = null;

		System.out.println("Starting PLACEMENT...");
		long startTime = System.currentTimeMillis();

		// create clusters of cells
		sc_gcluster = Sc_place_create_clusters(SilComp.sc_curcell);
		if (sc_gcluster == null) return "WARNING - No leaf cells found for placement";

		if (sc_gcluster == null)
		{
			System.out.println("ERROR - No cells found to place.  Aborting.");
			return null;
		}

		// place clusters in a binary group tree
		int numcl = 0;
		for (SCCLUSTER cl = sc_gcluster; cl != null; cl = cl.next) numcl++;
		SCCLUSTER [] cllist = new SCCLUSTER[numcl];
		int i = 0;
		for (SCCLUSTER cl = sc_gcluster; cl != null; cl = cl.next) cllist[i++] = cl;
		sc_gcluster_tree = Sc_place_create_cluster_tree(cllist, numcl, SilComp.sc_curcell);
		if (DEBUG)
		{
			System.out.println("************ Initial placement of Clusters");
			Sc_place_print_cluster_tree(sc_gcluster_tree, 0);
		}
		place.size_rows = place.size_inst / place.num_rows;

		// place clusters in list by sorting groups
		if (sort_flag)
		{
			Sc_place_sort_cluster_tree(sc_gcluster_tree, SilComp.sc_curcell);
			if (DEBUG)
			{
				System.out.println("************ Placement of Clusters after Sorting");
				Sc_place_print_cluster_tree(sc_gcluster_tree, 0);
			}
		}

		// create first row structure
		SCROWLIST row = new SCROWLIST();
		row.start = null;
		row.end = null;
		row.row_num = 0;
		row.row_size = 0;
		row.next = null;
		row.last = null;
		SilComp.sc_curcell.placement.rows = row;

		// create cell placement list from sorted cluster list
		Sc_place_create_placelist(sc_gcluster_tree, SilComp.sc_curcell);

		// number placement
		Sc_place_number_placement(SilComp.sc_curcell.placement.rows);
		if (DEBUG)
		{
			System.out.println("************ Placement before Net Balancing");
			Sc_place_show_placement(SilComp.sc_curcell.placement.rows);
		}

		// do net balance algorithm
		if (net_balance_flag)
		{
			Sc_place_net_balance(SilComp.sc_curcell.placement.rows, SilComp.sc_curcell);
			if (DEBUG)
			{
				System.out.println("************ Placement after Net Balancing");
				Sc_place_show_placement(SilComp.sc_curcell.placement.rows);
			}
		}

		// print process time for placement
		Sc_place_reorder_rows(SilComp.sc_curcell.placement.rows);
        long endTime = System.currentTimeMillis();
        System.out.println("Done (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");

		return null;
	}

	/**
	 * Method to create "clusters" of cells of size one.
	 * @param cell pointer to complex cell.
	 * @return start of list.
	 */
	private static SCCLUSTER Sc_place_create_clusters(SilComp.SCCELL cell)
	{
		// find total 'size' and number of all the cells
		int size = 0;
		int num = 0;
		int height = 0;
		for (Iterator it = cell.nilist.iterator(); it.hasNext(); )
		{
			SilComp.SCNITREE ilist = (SilComp.SCNITREE)it.next();
			if (ilist.type == SilComp.SCLEAFCELL)
			{
				num++;
				size += ilist.size;
				height += SilComp.Sc_leaf_cell_ysize((Cell)ilist.np);
			}
		}

		if (num == 0)
		{
			System.out.println("WARNING - No leaf cells found for placement");
			return null;
		}
		int avg_size = size / num;
		int avg_height = height / num;
		if (DEBUG)
		{
			System.out.println("************ Cell Statistics");
			System.out.println("    Number of cells         = " + num);
			System.out.println("    Total length            = " + size);
			System.out.println("    Average size of cells   = " + avg_size);
			System.out.println("    Average height of cells = " + avg_height);
		}
		cell.placement.num_inst = num;
		cell.placement.size_inst = size;
		cell.placement.avg_size = avg_size;
		cell.placement.avg_height = avg_height;

		// create cluster list
		int i = 0;
		SCCLUSTER clusterlist = null;
		boolean warn = false;
		for (Iterator it = cell.nilist.iterator(); it.hasNext(); )
		{
			SilComp.SCNITREE node = (SilComp.SCNITREE)it.next();
			if (node.type != SilComp.SCLEAFCELL)
			{
				if (node.type == SilComp.SCCOMPLEXCELL) warn = true;
				continue;
			}
			SCCLUSTER cluster = new SCCLUSTER();
			cluster.node = node;
			cluster.size = node.size;
			cluster.number = i++;
			cluster.last = null;
			cluster.next = clusterlist;
			if (clusterlist != null)
				clusterlist.last = cluster;
			clusterlist = cluster;
		}
		if (warn)
		{
			System.out.println("WARNING - At least one complex cell found during Create_Clusters");
			System.out.println("        - Probable cause:  Forgot to do 'PULL' command");
		}

		return clusterlist;
	}

	/**
	 * Method to recursively create the cluster tree from a group of clusters.
	 * At each "node" in the tree, the goal is to pair groups of clusters
	 * which are strongly connected together.
	 * @param clusters array of pointer to the clusters.
	 * @param size size of array (i.e. number of clusters).
	 * @param cell pointer to cell being placed.
	 * @return pointer to cluster tree node.
	 */
	private static SCCLUSTERTREE Sc_place_create_cluster_tree(SCCLUSTER [] clusters, int size, SilComp.SCCELL cell)
	{
		// create a cluster tree node for each cluster
		SCCLUSTERTREE nstart = null;
		for(int i=0; i<size; i++)
		{
			SCCLUSTERTREE node = new SCCLUSTERTREE();
			node.cluster = clusters[i];
			node.parent = null;
			node.next = nstart;
			nstart = node;
			node.lptr = null;
			node.rptr = null;
		}

		// recursively create cluster tree
		SCCLUSTERTREE ctree = Sc_place_create_ctree_recurse(nstart, cell);
		Sc_place_ctree_add_parents(ctree, null);
		return ctree;
	}

	/**
	 * Method to recursively create the cluster tree from the bottom up by pairing
	 * strongly connected tree nodes together.  When only one tree node
	 * exists, this is the root and can be written to the indicated address.
	 * @param nodes pointer to start of tree nodes.
	 * @param cell pointer to parent cell.
	 * @return tree root.
	 */
	private static SCCLUSTERTREE Sc_place_create_ctree_recurse(SCCLUSTERTREE nodes, SilComp.SCCELL cell)
	{
		// if no node, end
		if (nodes == null) return null;

		// if one node, write to root and end
		if (nodes.next == null)
		{
			return nodes;
		}

		// pair nodes in groups
		// create list of connections between nodes
		SCCLCONNECT nconnects = Sc_place_ctree_num_connects(nodes, cell);

		// sort number of connects from largest to smallest
		nconnects = Sc_place_ctree_sort_connects(nconnects);

		// pair by number of connects
		SCCLUSTERTREE nstart = Sc_place_ctree_pair(nodes, nconnects);

		// recurse up a level
		return Sc_place_create_ctree_recurse(nstart, cell);
	}

	/**
	 * Method to add the parent pointer to the cluster tree by doing a preorder transversal.
	 * @param node pointer to current node in transversal.
	 * @param parent pointer to parent node.
	 */
	private static void Sc_place_ctree_add_parents(SCCLUSTERTREE node, SCCLUSTERTREE parent)
	{
		if (node == null) return;
		node.parent = parent;
		Sc_place_ctree_add_parents(node.lptr, node);
		Sc_place_ctree_add_parents(node.rptr, node);
	}

	/**
	 * Method to create a list of the number of connections from all groups to all other groups.
	 * @param nodes List of current nodes.
	 * @param cell Pointer to parent cell.
	 */
	private static SCCLCONNECT Sc_place_ctree_num_connects(SCCLUSTERTREE nodes, SilComp.SCCELL cell)
	{
		SCCLCONNECT start = null, end = null;
		int node_num = 0;

		// clear flags on all extracted nodes
		Sc_clear_ext_nodes(cell.ex_nodes);

		// go through list of nodes
		for ( ; nodes != null; nodes = nodes.next)
		{
			// check all other node
			for (SCCLUSTERTREE nextnode = nodes.next; nextnode != null; nextnode = nextnode.next)
			{
				node_num += 2;

				// mark all extracted nodes used by first node
				Sc_set_ext_nodes_by_ctree(nodes, node_num);

				// count number of common extracted nodes
				int common = Sc_count_ext_nodes(nextnode, node_num);

				if (common != 0)
				{
					SCCLCONNECT newcon = new SCCLCONNECT();
					newcon.node[0] = nodes;
					newcon.node[1] = nextnode;
					newcon.count = common;
					newcon.last = end;
					newcon.next = null;
					if (end != null)
					{
						end.next = newcon;
					} else
					{
						start = newcon;
					}
					end = newcon;
				}
			}
		}
		return start;
	}

	/**
	 * Method to set the flags field of all extracted nodes to clear.
	 */
	private static void Sc_clear_ext_nodes(SilComp.SCEXTNODE nodes)
	{
		for ( ; nodes != null; nodes = nodes.next)
			nodes.flags &= ~SCEXTNODECLUSE;
	}

	/**
	 * Method to mark all extracted nodes references by any member of all the
	 * clusters in the indicated cluster tree.
	 * @param node pointer to cluster tree node.
	 * @param marker value to set flags field to.
	 */
	private static void Sc_set_ext_nodes_by_ctree(SCCLUSTERTREE node, int marker)
	{
		if (node == null) return;

		Sc_set_ext_nodes_by_ctree(node.lptr, marker);

		// process node if cluster
		if (node.cluster != null)
		{
			// check every port of member
			for (SilComp.SCNIPORT port = node.cluster.node.ports; port != null; port = port.next)
				port.ext_node.flags = marker;
		}

		Sc_set_ext_nodes_by_ctree(node.rptr, marker);
	}

	/**
	 * Method to return the number of extracted nodes which have flag bit set only
	 * and is accessed by subtree.
	 * @param node start of cluster tree node.
	 * @param marker value to look for.
	 */
	private static int Sc_count_ext_nodes(SCCLUSTERTREE node, int marker)
	{
		if (node == null) return 0;

		int count = Sc_count_ext_nodes(node.lptr, marker);

		// process node if cluster
		if (node.cluster != null)
		{
			// check every port of member
			for (SilComp.SCNIPORT port = node.cluster.node.ports; port != null; port = port.next)
			{
				if (port.ext_node.flags == marker)
				{
					count++;
					port.ext_node.flags |= SCEXTNODEGROUP1;
				}
			}
		}

		count += Sc_count_ext_nodes(node.rptr, marker);
		return count;
	}

	/**
	 * Method to sort the passed list on number of connections from largest to smallest.
	 */
	private static SCCLCONNECT Sc_place_ctree_sort_connects(SCCLCONNECT list)
	{
		// order placement list highest to lowest
		if (list != null)
		{
			SCCLCONNECT pold = list;
			for (SCCLCONNECT pp = list.next; pp != null;)
			{
				if (pp.count > pold.count)
				{
					pold.next = pp.next;
					if (pp.next != null)
						pp.next.last = pold;
					SCCLCONNECT plast;
					for (plast = pold.last; plast != null; plast = plast.last)
					{
						if (plast.count >= pp.count)
							break;
					}
					if (plast == null)
					{
						pp.next = list;
						list.last = pp;
						list = pp;
						pp.last = null;
					} else
					{
						pp.next = plast.next;
						pp.next.last = pp;
						pp.last = plast;
						plast.next = pp;
					}
					pp = pold.next;
				} else
				{
					pold = pp;
					pp = pp.next;
				}
			}
		}
		return list;
	}

	/**
	 * Method to pair up the given nodes by using the information in the connection list.
	 * @param nodes pointer to start of list of nodes.
	 * @param nconnects pointer to start of list of connections.
	 * @return new list.
	 */
	private static SCCLUSTERTREE Sc_place_ctree_pair(SCCLUSTERTREE nodes, SCCLCONNECT nconnects)
	{
		// clear the placed flag in all tree nodes
		for (SCCLUSTERTREE tptr = nodes; tptr != null; tptr = tptr.next)
			tptr.bits &= ~SCBITS_PLACED;

		// go through connection list
		SCCLUSTERTREE newstart = null;
		for (SCCLCONNECT connect = nconnects; connect != null; )
		{
			// if either placed, continue
			if ((connect.node[0].bits & SCBITS_PLACED) != 0 ||
				(connect.node[1].bits & SCBITS_PLACED) != 0)
			{
				connect = connect.next;
				continue;
			}

			// get best choice
			SCCLCONNECT bconnect = Sc_place_best_pair(connect);

			// create new cluster tree node
			SCCLUSTERTREE newnode = new SCCLUSTERTREE();
			newnode.cluster = null;
			newnode.bits = 0;
			newnode.parent = null;
			newnode.lptr = bconnect.node[0];
			newnode.lptr.parent = newnode;
			bconnect.node[0].bits |= SCBITS_PLACED;
			newnode.rptr = bconnect.node[1];
			newnode.rptr.parent = newnode;
			bconnect.node[1].bits |= SCBITS_PLACED;
			newnode.next = newstart;
			newstart = newnode;

			// remove from list
			if (connect == bconnect)
			{
				connect = connect.next;
			} else
			{
				bconnect.last.next = bconnect.next;
				if (bconnect.next != null)
					bconnect.next.last = bconnect.last;
			}
		}

		// if no connections, arbitrarily combine two clusters
		if (nconnects == null)
		{
			// create new cluster tree node
			SCCLUSTERTREE newnode = new SCCLUSTERTREE();
			newnode.cluster = null;
			newnode.bits = 0;
			newnode.parent = null;
			newnode.lptr = nodes;
			newnode.lptr.parent = newnode;
			nodes.bits |= SCBITS_PLACED;
			newnode.rptr = nodes.next;
			newnode.rptr.parent = newnode;
			nodes.next.bits |= SCBITS_PLACED;
			newnode.next = newstart;
			newstart = newnode;
		}

		// add any remaining tree nodes as singular nodes
		for (SCCLUSTERTREE tptr = nodes; tptr != null; tptr = tptr.next)
		{
			if ((tptr.bits & SCBITS_PLACED) == 0)
			{
				// create new cluster tree node
				SCCLUSTERTREE newnode = new SCCLUSTERTREE();
				newnode.cluster = null;
				newnode.bits = 0;
				newnode.parent = null;
				newnode.lptr = tptr;
				newnode.lptr.parent = newnode;
				tptr.bits |= SCBITS_PLACED;
				newnode.rptr = null;
				newnode.next = newstart;
				newstart = newnode;
			}
		}
		return newstart;
	}

	private static class Temp
	{
		SCCLUSTERTREE	node;		/* cluster tree node */
		int			count;		/* number of times seen */
		SCCLCONNECT	ref;		/* first reference */
		Temp		next;		/* next in list */
	}

	/**
	 * Method to find the best cluster connection.
	 * The best has both members unplaced, has the same weight as the one top on the list,
	 * and appears the smallest number of times.
	 * @param connect start of sorted list.
	 * @return pointer to best pair.
	 */
	private static SCCLCONNECT Sc_place_best_pair(SCCLCONNECT connect)
	{
		Temp slist = null;
		for (SCCLCONNECT nconnect = connect; nconnect != null; nconnect = nconnect.next)
		{
			if (nconnect.count < connect.count) break;
			if ((nconnect.node[0].bits & SCBITS_PLACED) != 0 ||
				(nconnect.node[1].bits & SCBITS_PLACED) != 0) continue;

			// check if nodes previously counted
			Temp nlist;
			for (nlist = slist; nlist != null; nlist = nlist.next)
			{
				if (nlist.node == nconnect.node[0])
					break;
			}
			if (nlist != null)
			{
				nlist.count++;
			} else
			{
				nlist = new Temp();
				nlist.node = nconnect.node[0];
				nlist.count = 1;
				nlist.ref = nconnect;
				nlist.next = slist;
				slist = nlist;
			}

			for (nlist = slist; nlist != null; nlist = nlist.next)
			{
				if (nlist.node == nconnect.node[1])
					break;
			}
			if (nlist != null)
			{
				nlist.count++;
			} else
			{
				nlist = new Temp();
				nlist.node = nconnect.node[1];
				nlist.count = 1;
				nlist.ref = nconnect;
				nlist.next = slist;
				slist = nlist;
			}
		}

		// find the minimum count
		int minuse = slist.count;
		Temp blist = slist;
		for (Temp nlist = slist.next; nlist != null; nlist = nlist.next)
		{
			if (nlist.count < minuse)
			{
				minuse = nlist.count;
				blist = nlist;
			}
		}

		return blist.ref;
	}

	/**
	 * Method to sort the cluster tree into a list by sorting groups.
	 * Sorting attempts to optimize the placement of groups by
	 * minimizing length of connections between groups and locating groups
	 * close to any specified ports.
	 * @param ctree pointer to root of cluster tree.
	 * @param cell pointer to parent cell.
	 */
	private static void Sc_place_sort_cluster_tree(SCCLUSTERTREE ctree, SilComp.SCCELL cell)
	{
		SCNBTRUNK trunks = null;

		// create a list of trunks from the extracted nodes
		for (SilComp.SCEXTNODE enode = cell.ex_nodes; enode != null; enode = enode.next)
		{
			SCNBTRUNK newtrunk = new SCNBTRUNK();
			newtrunk.ext_node = enode;
			newtrunk.minx = 0;
			newtrunk.maxx = 0;
			newtrunk.next = trunks;
			trunks = newtrunk;
			enode.ptr = newtrunk;		// back reference pointer
		}

		sc_currentcost = Sc_place_cost_cluster_tree(sc_gcluster_tree, trunks, cell);
		if (DEBUG)
			System.out.println("***** Cost of placement before cluster sorting = " + sc_currentcost);

		// call top-down swapper
		Sc_place_sort_swapper_top_down(ctree, trunks, cell);

		// call bottom-up swapper
		Sc_place_sort_swapper_bottom_up(ctree, trunks, cell);
		if (DEBUG)
			System.out.println("***** Cost of placement after cluster sorting = " + sc_currentcost);
	}

	/**
	 * Method to do preorder transversal of cluster tree, swapping groups to try
	 * and sort tree into a more efficient placement.
	 * @param ctree root of cluster tree.
	 * @param trunks list of trunks for costing.
	 * @param cell pointer to parent cell.
	 */
	private static void Sc_place_sort_swapper_top_down(SCCLUSTERTREE ctree, SCNBTRUNK trunks, SilComp.SCCELL cell)
	{
		if (ctree == null) return;

		// process tree node if there are two subtrees
		if (ctree.lptr != null && ctree.rptr != null)
		{
			// swap groups
			Sc_place_switch_subtrees(ctree);

			// check new cost
			int cost2 = Sc_place_cost_cluster_tree(sc_gcluster_tree, trunks, cell);

			// swap back if old cost is less than new
			if (sc_currentcost < cost2)
			{
				Sc_place_switch_subtrees(ctree);
			} else
			{
				sc_currentcost = cost2;
			}
		}

		Sc_place_sort_swapper_top_down(ctree.lptr, trunks, cell);

		Sc_place_sort_swapper_top_down(ctree.rptr, trunks, cell);
	}

	/**
	 * Method to do a postorder transversal of cluster tree, swapping groups to try
	 * and sort tree into a more efficient placement.
	 * @param ctree root of cluster tree.
	 * @param trunks list of trunks for costing.
	 * @param cell pointer to parent cell.
	 */
	private static void Sc_place_sort_swapper_bottom_up(SCCLUSTERTREE ctree, SCNBTRUNK trunks, SilComp.SCCELL cell)
	{
		if (ctree == null) return;

		Sc_place_sort_swapper_bottom_up(ctree.lptr, trunks, cell);

		Sc_place_sort_swapper_bottom_up(ctree.rptr, trunks, cell);

		// process tree node if there are two subtrees
		if (ctree.lptr != null && ctree.rptr != null)
		{
			// swap groups
			Sc_place_switch_subtrees(ctree);

			// check new cost
			int cost2 = Sc_place_cost_cluster_tree(sc_gcluster_tree, trunks, cell);

			// swap back if old cost is less than new
			if (sc_currentcost < cost2)
			{
				Sc_place_switch_subtrees(ctree);
			} else
			{
				sc_currentcost = cost2;
			}
		}
	}

	/**
	 * Method to switch the subtrees recursively to perform a mirror image operation along "main" axis.
	 * @param node pointer to top tree node.
	 */
	private static void Sc_place_switch_subtrees(SCCLUSTERTREE node)
	{
		if (node == null) return;

		SCCLUSTERTREE temp = node.lptr;
		node.lptr = node.rptr;
		node.rptr = temp;
		Sc_place_switch_subtrees(node.lptr);
		Sc_place_switch_subtrees(node.rptr);
	}

	/**
	 * Method to return the "cost" of the indicated cluster tree sort.
	 * Cost is a function of the length of connections between clusters and placement to ports.
	 * @param ctree pointer to cluster tree node.
	 * @param trunks pointer to trunks to use to cost.
	 * @param cell pointer to parent cell.
	 * @return cost.
	 */
	private static int Sc_place_cost_cluster_tree(SCCLUSTERTREE ctree, SCNBTRUNK trunks, SilComp.SCCELL cell)
	{
		// clear trunks to record lengths
		for (SCNBTRUNK ntrunk = trunks; ntrunk != null; ntrunk = ntrunk.next)
		{
			ntrunk.minx = -1;
			ntrunk.maxx = -1;
		}

		// set trunks lengths
		double pos = Sc_place_cost_cluster_tree_2(ctree, trunks, 0);

		// calculate cost
		int cost = 0;
		for (SCNBTRUNK ntrunk = trunks; ntrunk != null; ntrunk = ntrunk.next)
		{
			if (ntrunk.minx < 0) continue;
			cost += ntrunk.maxx - ntrunk.minx;
		}

		for (SilComp.SCPORT pport = cell.ports; pport != null; pport = pport.next)
		{
			if ((pport.bits & SilComp.SCPORTDIRMASK) == 0) continue;
			SCNBTRUNK ntrunk = (SCNBTRUNK)pport.node.ports.ext_node.ptr;
			if (ntrunk == null) continue;
			if ((pport.bits & SilComp.SCPORTDIRUP) != 0)
			{
				// add distance to top row
				int row = (int)(ntrunk.maxx / cell.placement.size_rows);
				if ((row + 1) < cell.placement.num_rows)
				{
					cost += (cell.placement.num_rows - row - 1) *
						cell.placement.avg_height * vertical_cost;
				}
			}
			if ((pport.bits & SilComp.SCPORTDIRDOWN) != 0)
			{
				// add distance to bottom row
				int row = (int)(ntrunk.minx / cell.placement.size_rows);
				if (row != 0)
				{
					cost += row * cell.placement.avg_height * vertical_cost;
				}
			}
			if ((pport.bits & SilComp.SCPORTDIRLEFT) != 0)
			{
				// EMPTY
			}
			if ((pport.bits & SilComp.SCPORTDIRRIGHT) != 0)
			{
				// EMPTY
			}
		}

		return cost;
	}

	/**
	 * Method to set the limits of the trunks by doing an inorder transversal of the cluster tree.
	 * @param ctree pointer to cluster tree node.
	 * @param trunks pointer to trunks to use to cost.
	 * @param pos current position.
	 */
	private static double Sc_place_cost_cluster_tree_2(SCCLUSTERTREE ctree, SCNBTRUNK trunks, double pos)
	{
		if (ctree == null) return pos;

		// do all nodes left
		pos = Sc_place_cost_cluster_tree_2(ctree.lptr, trunks, pos);

		// process node
		if (ctree.cluster != null)
		{
			for (SilComp.SCNIPORT port = ctree.cluster.node.ports; port != null; port = port.next)
			{
				SCNBTRUNK ntrunk = (SCNBTRUNK)port.ext_node.ptr;
				if (ntrunk == null) continue;
				if (ntrunk.minx < 0)
					ntrunk.minx = pos + port.xpos;
				ntrunk.maxx = pos + port.xpos;
			}
			pos += ctree.cluster.size;
		}

		// do all nodes right
		return Sc_place_cost_cluster_tree_2(ctree.rptr, trunks, pos);
	}

	/**
	 * Module to create the placement list by simply taking the clusters from the
	 * sorted cluster list and placing members in a snake pattern.
	 * Do an inorder transversal to create placelist.
	 * @param ctree pointer to start of cluster tree.
	 * @param cell pointer to parent cell.
	 */
	private static void Sc_place_create_placelist(SCCLUSTERTREE ctree, SilComp.SCCELL cell)
	{
		if (ctree == null) return;

		Sc_place_create_placelist(ctree.lptr, cell);

		// add clusters to placement list
		if (ctree.cluster != null)
		{
			SCROWLIST row = cell.placement.rows;
			while (row.next != null)
				row = row.next;
			Sc_place_add_cluster_to_row(ctree.cluster, row, cell.placement);
		}

		Sc_place_create_placelist(ctree.rptr, cell);
	}

	/**
	 * Method to add the members of the passed cluster to the indicated row.
	 * Add new rows as necessary and also maintain a global placement
	 * bidirectional list.
	 * @param culster pointer to cluster to add.
	 * @param row pointer to the current row.
	 * @param place pointer to placement information.
	 */
	private static void Sc_place_add_cluster_to_row(SCCLUSTER cluster, SCROWLIST row, SCPLACE place)
	{
		if (cluster.node.type != SilComp.SCLEAFCELL) return;
		SCNBPLACE newplace = new SCNBPLACE();
		newplace.cell = cluster.node;
		newplace.xpos = 0;
		cluster.node.tp = newplace;
		newplace.next = null;
		newplace.last = place.endlist;
		if (place.endlist == null)
		{
			place.plist = place.endlist = newplace;
		} else
		{
			place.endlist.next = newplace;
			place.endlist = newplace;
		}
		double old_condition = place.size_rows - row.row_size;
		double new_condition = place.size_rows - (row.row_size + cluster.node.size);
		if ((row.row_num + 1) < place.num_rows &&
			Math.abs(old_condition) < Math.abs(new_condition))
		{
			SCROWLIST row2 = new SCROWLIST();
			row2.start = null;
			row2.end = null;
			row2.row_num = row.row_num + 1;
			row2.row_size = 0;
			row2.next = null;
			row2.last = row;
			row.next = row2;
			row = row2;
		}

		// add to row
		if ((row.row_num % 2) != 0)
		{
			// odd row
			if (row.end == null)
				row.end = newplace;
			row.start = newplace;
		} else
		{
			// even row
			if (row.start == null)
				row.start = newplace;
			row.end = newplace;
		}
		row.row_size += cluster.node.size;
	}

	/**
	 * Method to do a net balancing on the placelist.
	 * @param row pointer to start of row list.
	 * @param cell pointer to parent cell.
	 */
	private static void Sc_place_net_balance(SCROWLIST row, SilComp.SCCELL cell)
	{
		// create channel list
		SCCHANNEL channel = null, endchan = null;
		int i = 0;
		SCNBTRUNK sametrunk = null;
		do
		{
			SCCHANNEL newchan = new SCCHANNEL();
			newchan.number = i;
			SCNBTRUNK trunks = null, oldtrunk = null;

			// create trunk list for each channel
			for (SilComp.SCEXTNODE nlist = cell.ex_nodes; nlist != null; nlist = nlist.next)
			{
				SCNBTRUNK ntrunk = new SCNBTRUNK();
				ntrunk.ext_node = nlist;
				ntrunk.minx = 0;
				ntrunk.maxx = 0;
				ntrunk.same = null;
				if (sametrunk == null)
				{
					nlist.ptr = ntrunk;
				} else
				{
					sametrunk.same = ntrunk;
					sametrunk = sametrunk.next;
				}
				ntrunk.next = null;
				if (oldtrunk == null)
				{
					trunks = oldtrunk = ntrunk;
				} else
				{
					oldtrunk.next = ntrunk;
					oldtrunk = ntrunk;
				}
			}
			newchan.trunks = trunks;
			newchan.last = endchan;
			newchan.next = null;
			if (endchan != null)
			{
				endchan.next = newchan;
				endchan = newchan;
			} else
			{
				channel = endchan = newchan;
			}
			sametrunk = trunks;
			i++;
		} while ((i + 1) < cell.placement.num_rows);

		// report current placement evaluation
		if (DEBUG)
			System.out.println("Evaluation before Net-Balancing  = " +
				Sc_place_nb_cost(cell.placement.rows, channel, cell));

		// do the net balance for each cell
		Sc_place_nb_all_cells(cell, channel);

		// number placement
		Sc_place_nb_rebalance_rows(cell.placement.rows, cell.placement);
		Sc_place_number_placement(cell.placement.rows);

		// report new evaluation
		if (DEBUG)
			System.out.println("Evaluation after Net-Balancing   = %d" +
				Sc_place_nb_cost(cell.placement.rows, channel, cell));
	}

	/**
	 * Method to do a net balance for each cell on at a time.  Use the SCNITREE to
	 * insure that each cell is processed.
	 * @param cell pointer to parent cell.
	 * @param channels pointer to start of channel list.
	 */
	private static void Sc_place_nb_all_cells(SilComp.SCCELL cell, SCCHANNEL channels)
	{
		// process cell
		for (Iterator it = cell.nilist.iterator(); it.hasNext(); )
		{
			SilComp.SCNITREE ilist = (SilComp.SCNITREE)it.next();
			if (ilist.type == SilComp.SCLEAFCELL)
				Sc_place_nb_do_cell((SCNBPLACE)ilist.tp, channels, cell);
		}
	}

	/**
	 * Method to do a net balance for the indicated instance.
	 * @param place pointer to place of instance.
	 * @param channels pointer to channel list of trunks.
	 * @param cell parent complex cell.
	 */
	private static void Sc_place_nb_do_cell(SCNBPLACE place, SCCHANNEL channels, SilComp.SCCELL cell)
	{
		if (place == null) return;

		// find cost at present location and set as current minimum
		SCROWLIST rows = cell.placement.rows;
		int min_cost = Sc_place_nb_cost(rows, channels, cell);
		int pos = 0;

		// temporarily remove from list
		SCNBPLACE old_last = place.last;
		SCNBPLACE old_next = place.next;
		Sc_place_nb_remove(place, rows);

		// check locations backwards for nb_limit
		int npos = -1;
		for (SCNBPLACE nplace = old_last; npos >= -net_balance_limit; nplace = nplace.last)
		{
			if (nplace != null)
			{
				// temporarily insert in list
				Sc_place_nb_insert_before(place, nplace, rows);

				// check new cost
				int cost = Sc_place_nb_cost(rows, channels, cell);
				if (cost < min_cost)
				{
					min_cost = cost;
					pos = npos;
				}

				// remove place from list
				Sc_place_nb_remove(place, rows);
			} else
			{
				break;
			}
			npos--;
		}

		// check forward locations for nb_limit
		npos = 1;
		for (SCNBPLACE nplace = old_next; npos < net_balance_limit; nplace = nplace.next)
		{
			if (nplace != null)
			{
				// temporarily insert in list
				Sc_place_nb_insert_after(place, nplace, rows);

				// check new cost
				int cost = Sc_place_nb_cost(rows, channels, cell);
				if (cost < min_cost)
				{
					min_cost = cost;
					pos = npos;
				}

				// remove place from list
				Sc_place_nb_remove(place, rows);
			} else
			{
				break;
			}
			npos++;
		}

		// move if necessary
		if (pos > 0)
		{
			while(pos-- > 1)
			{
				old_next = old_next.next;
			}
			Sc_place_nb_insert_after(place, old_next, rows);
		} else if (pos < 0)
		{
			while(pos++ < -1)
			{
				old_last = old_last.last;
			}
			Sc_place_nb_insert_before(place, old_last, rows);
		} else
		{
			if (old_last != null)
			{
				Sc_place_nb_insert_after(place, old_last, rows);
			} else
			{
				Sc_place_nb_insert_before(place, old_next, rows);
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
	private static int Sc_place_nb_cost(SCROWLIST rows, SCCHANNEL channels, SilComp.SCCELL cell)
	{
		// initialize all trunks
		for (SCCHANNEL nchan = channels; nchan != null; nchan = nchan.next)
		{
			for (SCNBTRUNK ntrunk = nchan.trunks; ntrunk != null; ntrunk = ntrunk.next)
			{
				ntrunk.minx = Double.MAX_VALUE;
				ntrunk.maxx = Double.MIN_VALUE;
			}
		}

		// check all rows
		SCCHANNEL nchan = channels;
		boolean above = true;
		int dis = 0;
		int row_num = 0;
		int max_rowsize = cell.placement.size_rows + (cell.placement.avg_size >>1);
		for (SCNBPLACE nplace = rows.start; nplace != null; nplace = nplace.next)
		{
			// check for room in current row
			if ((row_num % 2) != 0)
			{
				// odd row
				if ((dis - nplace.cell.size) < 0)
				{
					if ((row_num + 1) < cell.placement.num_rows)
					{
						row_num++;
						dis = 0;
						if (above ^= true)
							nchan = nchan.next;
					}
				}
			} else
			{
				// even row
				if ((dis + nplace.cell.size) > max_rowsize)
				{
					if ((row_num + 1) < cell.placement.num_rows)
					{
						row_num++;
						dis = max_rowsize;
						if (above ^= true)
							nchan = nchan.next;
					}
				}
			}

			// check all ports on instance
			for (SilComp.SCNIPORT port = nplace.cell.ports; port != null; port = port.next)
			{
				// find the correct trunk
				SCNBTRUNK ntrunk = (SCNBTRUNK)port.ext_node.ptr;
				if (ntrunk == null) continue;
				for (int i = nchan.number; i != 0; i--)
					ntrunk = ntrunk.same;
				if (ntrunk.minx == Double.MAX_VALUE)
				{
					if (!above && ntrunk.same != null)
						ntrunk = ntrunk.same;
				}
				double pos = 0;
				if ((row_num % 2) != 2)
				{
					pos = dis - port.xpos;
				} else
				{
					pos = dis + port.xpos;
				}
				ntrunk.minx = Math.min(ntrunk.minx, pos);
				ntrunk.maxx = Math.max(ntrunk.maxx, pos);
			}
			if ((row_num % 2) != 2)
			{
				dis -= nplace.cell.size;
			} else
			{
				dis += nplace.cell.size;
			}
		}

		// calculate cost
		int cost = 0;

		// calculate horizontal costs
		for (nchan = channels; nchan != null; nchan = nchan.next)
		{
			for (SCNBTRUNK ntrunk = nchan.trunks; ntrunk != null; ntrunk = ntrunk.next)
			{
				if (ntrunk.minx != Double.MAX_VALUE)
					cost += Math.abs(ntrunk.maxx - ntrunk.minx);
			}
		}

		// calculate vertical cost
		for (SCNBTRUNK ntrunk = channels.trunks; ntrunk != null; ntrunk = ntrunk.next)
		{
			SCNBTRUNK ftrunk = null;
			int fcount = 0, count = 0;
			for (SCNBTRUNK strunk = ntrunk; strunk != null; strunk = strunk.same)
			{
				if (strunk.minx != Double.MAX_VALUE)
				{
					double fminx = 0, fmaxx = 0;
					if (ftrunk == null)
					{
						ftrunk = strunk;
						fminx = strunk.minx;
						fmaxx = strunk.maxx;
						fcount = count;
					} else
					{
						// add new vertical
						cost += (count - fcount) * cell.placement.avg_height * vertical_cost;
						fcount = count;

						// additional horizontal
						if (fmaxx < strunk.minx)
						{
							cost += Math.abs(strunk.minx - fmaxx);
							fmaxx = strunk.maxx;
						} else if (fminx > strunk.maxx)
						{
							cost += Math.abs(fminx - strunk.maxx);
							fminx = strunk.minx;
						} else
						{
							if (fminx > strunk.minx) fminx = strunk.minx;
							if (fmaxx < strunk.maxx) fmaxx = strunk.maxx;
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
	private static void Sc_place_nb_remove(SCNBPLACE place, SCROWLIST rows)
	{
		SCNBPLACE old_next = place.next;
		SCNBPLACE old_last = place.last;
		if (place.last != null)
			place.last.next = old_next;
		if (place.next != null)
			place.next.last = old_last;

		// check if row change
		for (SCROWLIST row = rows; row != null; row = row.next)
		{
			if (row.start == place)
			{
				if ((row.row_num % 2) != 0)
				{
					row.start = old_last;
				} else
				{
					row.start = old_next;
				}
			}
			if (row.end == place)
			{
				if ((row.row_num % 2) != 2)
				{
					row.end = old_next;
				} else
				{
					row.end = old_last;
				}
			}
		}
	}

	/**
	 * Module to insert the indicated place before the indicated second place and
	 * clear up the row markers if necessary.
	 * @param place pointer to place to be inserted.
	 * @param oldplace pointer to place to be inserted before.
	 * @param rows start of list of row markers.
	 */
	private static void Sc_place_nb_insert_before(SCNBPLACE place, SCNBPLACE oldplace, SCROWLIST rows)
	{
		place.next = oldplace;
		if (oldplace != null)
		{
			place.last = oldplace.last;
			if (oldplace.last != null)
				oldplace.last.next = place;
			oldplace.last = place;
		} else
		{
			place.last = null;
		}

		// check if row change
		for (SCROWLIST row = rows; row != null; row = row.next)
		{
			if (row.start == oldplace)
			{
				if ((row.row_num % 2) != 0)
				{
					// EMPTY
				} else
				{
					row.start = place;
				}
			}
			if (row.end == oldplace)
			{
				if ((row.row_num % 2) != 0)
					row.end = place;
			}
		}
	}

	/**
	 * Method to insert the indicated place after the indicated second place and
	 * clear up the row markers if necessary.
	 * @param place pointer to place to be inserted.
	 * @param oldplace pointer to place to be inserted after.
	 * @param rows start of list of row markers.
	 */
	private static void Sc_place_nb_insert_after(SCNBPLACE place, SCNBPLACE oldplace, SCROWLIST rows)
	{
		place.last = oldplace;
		if (oldplace != null)
		{
			place.next = oldplace.next;
			if (oldplace.next != null)
				oldplace.next.last = place;
			oldplace.next = place;
		} else
		{
			place.next = null;
		}

		// check if row change
		for (SCROWLIST row = rows; row != null; row = row.next)
		{
			if (row.start == oldplace)
			{
				if ((row.row_num % 2) != 0)
					row.start = place;
			}
			if (row.end == oldplace)
			{
				if ((rows.row_num % 2) != 0)
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
	private static void Sc_place_nb_rebalance_rows(SCROWLIST rows, SCPLACE place)
	{
		int max_rowsize = place.size_rows + (place.avg_size >> 1);
		rows.row_size = 0;
		for (SCNBPLACE nplace = rows.start; nplace != null; nplace = nplace.next)
		{
			if ((rows.row_num + 1) < place.num_rows &&
				(rows.row_size + nplace.cell.size) > max_rowsize)
			{
				rows = rows.next;
				rows.row_size = 0;
				if ((rows.row_num % 2) != 0)
				{
					rows.end = nplace;
				} else
				{
					rows.start = nplace;
				}
			}
			rows.row_size += nplace.cell.size;
			if ((rows.row_num % 2) != 0)
			{
				rows.start = nplace;
			} else
			{
				rows.end = nplace;
			}
		}
	}

	/**
	 * Method to number the x position of all the cells in their rows.
	 * @param rows pointer to the start of the rows.
	 */
	private static void Sc_place_number_placement(SCROWLIST rows)
	{
		for (SCROWLIST row = rows; row != null; row = row.next)
		{
			int xpos = 0;
			SCNBPLACE nplace = row.start;
			while (nplace != null)
			{
				nplace.xpos = xpos;
				xpos += nplace.cell.size;
				if (nplace == row.end)
				{
					break;
				}
				if ((row.row_num % 2) != 0)
				{
					nplace = nplace.last;
				} else
				{
					nplace = nplace.next;
				}
			}
		}
	}

	/**
	 * Method to clean up the placement rows structure by reversing the pointers
	 * of odd rows and breaking the snake pattern by row.
	 * @param rows pointer to start of row list.
	 */
	private static void Sc_place_reorder_rows(SCROWLIST rows)
	{
		for (SCROWLIST row = rows; row != null; row = row.next)
		{
			if ((row.row_num % 2) != 0)
			{
				// odd row
				for (SCNBPLACE place = row.start; place != null; place = place.next)
				{
					SCNBPLACE tplace = place.next;
					place.next = place.last;
					place.last = tplace;
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
	private static void Sc_place_show_placement(SCROWLIST rows)
	{
		for (SCROWLIST row = rows; row != null; row = row.next)
		{
			System.out.println("For Row #" + row.row_num + ", size " + row.row_size+ ":");
			SCNBPLACE inst;
			for (inst = row.start; inst != row.end;)
			{
				System.out.println("    " + inst.xpos + "    " + inst.cell.name);
				if ((row.row_num % 2) != 0)
				{
					inst = inst.last;
				} else
				{
					inst = inst.next;
				}
			}
			System.out.println("    " + inst.xpos + "    " + inst.cell.name);
		}
	}

	/**
	 * Method to print the cluster placement tree by doing an inorder transversal.
	 * @param node pointer to cluster tree node.
	 * @param level current level of tree (0 = root).
	 */
	private static void Sc_place_print_cluster_tree(SCCLUSTERTREE node, int level)
	{
		if (node == null) return;

		Sc_place_print_cluster_tree(node.lptr, level + 1);

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

		Sc_place_print_cluster_tree(node.rptr, level + 1);
	}

}
