/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: RTNode.java 
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
package com.sun.electric.database.hierarchy;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

/**
 * The RTNode class implements R-Trees.
 * R-trees come from this paper: Guttman, Antonin, "R-Trees: A Dynamic Index Structure for Spatial Searching",
 * ACM SIGMOD, 14:2, 47-57, June 1984.
 * <P>
 * R-trees are height-balanced trees in which all leaves are at the same depth and contain Geometric objects (the
 * NodeInsts and ArcInsts). Entries higher in the tree store boundary information that tightly encloses the leaves
 * below. All nodes hold from M to 2M entries, where M is 4. The bounding boxes of two entries may
 * overlap, which allows arbitrary structures to be represented. A search for a point or an area is a simple
 * recursive walk through the tree to collect appropriate leaf nodes. Insertion and deletion, however, are more
 * complex operations.  The figure below illustrates how R-Trees work:
 * <P>
 * <CENTER><IMG SRC="doc-files/Geometric-1.gif"></CENTER>
 */
class RTNode
{
/** lower bound on R-tree node size */			private static final int MINRTNODESIZE = 4;
/** upper bound on R-tree node size */			private static final int MAXRTNODESIZE = (MINRTNODESIZE*2);

	/** bounds of this node and its children */	private Rectangle2D bounds;
	/** number of children */					private int total;
	/** children */								private Object [] pointers;
	/** nonzero if children are terminal */		private boolean flag;
	/** parent node */							private RTNode parent;

	private RTNode()
	{
		pointers = new Object[MAXRTNODESIZE];
		bounds = new Rectangle2D.Double();
	}

	/** Method to get the number of children of this RTNode. */
	private int getTotal() { return total; }
	/** Method to set the number of children of this RTNode. */
	private void setTotal(int total) { this.total = total; }

	/** Method to get the parent of this RTNode. */
	private RTNode getParent() { return parent; }
	/** Method to set the parent of this RTNode. */
	private void setParent(RTNode parent) { this.parent = parent; }

	/** Method to get the number of children of this RTNode. */
	private Object getChild(int index) { return pointers[index]; }
	/** Method to set the number of children of this RTNode. */
	private void setChild(int index, Object obj) { this.pointers[index] = obj; }

	/** Method to get the leaf/branch flag of this RTNode. */
	private boolean getFlag() { return flag; }
	/** Method to set the leaf/branch flag of this RTNode. */
	private void setFlag(boolean flag) { this.flag = flag; }

	/** Method to get the bounds of this RTNode. */
	private Rectangle2D getBounds() { return bounds; }
	/** Method to set the bounds of this RTNode. */
	private void setBounds(Rectangle2D bounds) { this.bounds.setRect(bounds); }
	/** Method to extend the bounds of this RTNode by "bounds". */
	private void unionBounds(Rectangle2D bounds) { Rectangle2D.union(this.bounds, bounds, this.bounds); }

	/**
	 * Method to create the top-level R-Tree structure for a new Cell.
	 * @return an RTNode object that is empty.
	 */
	static RTNode makeTopLevel()
	{
		RTNode top = new RTNode();
		top.total = 0;
		top.flag = true;
		top.parent = null;
		return top;
	}

	/**
	 * Method to link this Geometric into the R-tree of its parent Cell.
	 * @param cell the parent Cell.
	 */
	static void linkGeom(Cell cell, Geometric geom)
	{
		// find the bottom-level branch (a RTNode with leafs) that would expand least by adding this Geometric
		RTNode rtn = cell.getRTree();
		if (rtn == null) return;
		for(;;)
		{
			// if R-tree node contains primitives, exit loop
			if (rtn.getFlag()) break;

			// find sub-node that would expand the least
			double bestExpand = 0;
			int bestSubNode = 0;
			for(int i=0; i<rtn.getTotal(); i++)
			{
				// get bounds and area of sub-node
				RTNode subrtn = (RTNode)rtn.getChild(i);
				Rectangle2D bounds = subrtn.getBounds();
				double area = bounds.getWidth() * bounds.getHeight();

				// get area of sub-node with new element
				Rectangle2D newUnion = new Rectangle2D.Double();
				Rectangle2D.union(geom.getBounds(), bounds, newUnion);
				double newArea = newUnion.getWidth() * newUnion.getHeight();

				// accumulate the least expansion
				double expand = newArea - area;

				// remember the child that expands the least
				if (i != 0 && expand > bestExpand) continue;
				bestExpand = expand;
				bestSubNode = i;
			}

			// recurse down to sub-node that expanded least
			rtn = (RTNode)rtn.getChild(bestSubNode);
		}

		// add this geometry element to the correct leaf R-tree node
		rtn.addToRTNode(geom, cell);
	}

	/**
	 * Method to remove this geometry from the R-tree its parent cell.
	 * @param cell the parent Cell.
	 */
	static void unLinkGeom(Cell cell, Geometric geom)
	{
		// find this node in the tree
		RTNode whichRTN = null;
		int whichInd = 0;
		RTNode rtn = cell.getRTree();
		if (rtn == null) return;
		Object[] result = rtn.findGeom(geom);
		if (result != null)
		{
			whichRTN = (RTNode)result[0];
			whichInd = ((Integer)result[1]).intValue();
		} else
		{
			result = (cell.getRTree()).findGeomAnywhere(geom);
			if (result == null)
			{
				System.out.println("Internal error: " + cell + " cannot find " + geom + " in R-Tree...Rebuilding R-Tree");
				cell.setRTree(makeTopLevel());
				for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
					linkGeom(cell, (Geometric)it.next());
				for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
					linkGeom(cell, (Geometric)it.next());
				return;
			}
			whichRTN = (RTNode)result[0];
			whichInd = ((Integer)result[1]).intValue();
			System.out.println("Internal warning: " + geom + " not in proper R-Tree location in " + cell);
		}

		// delete geom from this R-tree node
		whichRTN.removeRTNode(whichInd, cell);
	}

	private static int branchCount;

	/**
	 * Debugging method to print this R-Tree.
	 * @param indent the level of the tree, for proper indentation.
	 */
	public void printRTree(int indent)
	{
		if (indent == 0) branchCount = 0;

		StringBuffer line = new StringBuffer();
		for(int i=0; i<indent; i++) line.append(" ");
		line.append("RTNode");
		if (flag)
		{
			branchCount++;
			line.append(" NUMBER " + branchCount);
		}
		line.append(" X(" + bounds.getMinX() + "-" + bounds.getMaxX() + ") Y(" + bounds.getMinY() + "-" + bounds.getMaxY() + ") has " +
			total + " children:");
		System.out.println(line);

		for(int j=0; j<total; j++)
		{
			if (flag)
			{
				line = new StringBuffer();
				for(int i=0; i<indent+3; i++) line.append(" ");
				Geometric child = (Geometric)getChild(j);
				Rectangle2D childBounds = child.getBounds();
				line.append("Child X(" + childBounds.getMinX() + "-" + childBounds.getMaxX() + ") Y(" +
					childBounds.getMinY() + "-" + childBounds.getMaxY() + ") is " + child.describe(true));
				System.out.println(line);
			} else
			{
				((RTNode)getChild(j)).printRTree(indent+3);
			}
		}
	}

	/**
	 * Method to check the validity of an RTree node.
	 * @param level the level of the node in the tree (for error reporting purposes).
	 * @param cell the Cell on which this node resides.
	 */
	public void checkRTree(int level, Cell cell)
	{
		Rectangle2D localBounds = new Rectangle2D.Double();
		if (total == 0)
		{
			localBounds.setRect(0, 0, 0, 0);
		} else
		{
			localBounds.setRect(getBBox(0));
			for(int i=1; i<total; i++)
				Rectangle2D.union(localBounds, getBBox(i), localBounds);
		}
		if (!localBounds.equals(bounds))
		{
			if (Math.abs(localBounds.getMinX() - bounds.getMinX()) >= DBMath.getEpsilon() ||
				Math.abs(localBounds.getMinY() - bounds.getMinY()) >= DBMath.getEpsilon() ||
				Math.abs(localBounds.getWidth() - bounds.getWidth()) >= DBMath.getEpsilon() ||
				Math.abs(localBounds.getHeight() - bounds.getHeight()) >= DBMath.getEpsilon())
			{
				System.out.println("Tree of "+cell.describe(true)+" at level "+level+" has bounds "+localBounds+" but stored bounds are "+bounds);
				for(int i=0; i<total; i++)
					System.out.println("  ---Child "+i+" is "+ getBBox(i));
			}
		}

		if (!flag)
		{
			for(int j=0; j<total; j++)
			{
				((RTNode)getChild(j)).checkRTree(level+1, cell);
			}
		}
	}

	/**
	 * Method to get the bounding box of child "child" of this R-tree node.
	 */
	private Rectangle2D getBBox(int child)
	{
		if (flag)
		{
			Geometric geom = (Geometric)pointers[child];
			// @TODO: GVG if pointers is null (bad file read in), we get an exception
			return geom.getBounds();
		} else
		{
			RTNode subrtn = (RTNode)pointers[child];
			return subrtn.getBounds();
		}
	}


	/**
	 * Method to recompute the bounds of this R-tree node.
	 */
	private void figBounds()
	{
		if (total == 0)
		{
			bounds.setRect(0, 0, 0, 0);
			return;
		}
		bounds.setRect(getBBox(0));
		for(int i=1; i<total; i++)
			unionBounds(getBBox(i));
	}

	/**
	 * Method to add object "rtnInsert" to this R-tree node, which is in cell "cell".  Method may have to
	 * split the node and recurse up the tree
	 */
	private void addToRTNode(Object rtnInsert, Cell cell)
	{
		// see if there is room in the R-tree node
		if (getTotal() >= MAXRTNODESIZE)
		{
			// no room: copy list to temp one
			RTNode temp = new RTNode();
			temp.setTotal(getTotal());
			temp.setFlag(getFlag());
			for(int i=0; i<getTotal(); i++)
				temp.setChild(i, getChild(i));

			// find the element farthest from new object
			Rectangle2D bounds;
			if (rtnInsert instanceof Geometric)
			{
				Geometric geom = (Geometric)rtnInsert;
				bounds = geom.getBounds();
			} else
			{
				RTNode subrtn = (RTNode)rtnInsert;
				bounds = subrtn.getBounds();
			}
			Point2D thisCenter = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
			double newDist = 0;
			int newN = 0;
			for(int i=0; i<temp.getTotal(); i++)
			{
				Rectangle2D thisv = temp.getBBox(i);
				double dist = thisCenter.distance(thisv.getCenterX(), thisv.getCenterY());
				if (dist >= newDist)
				{
					newDist = dist;
					newN = i;
				}
			}

			// now find element farthest from "newN"
			bounds = temp.getBBox(newN);
			thisCenter = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
			double oldDist = 0;
			int oldN = 0;
			if (oldN == newN) oldN++;
			for(int i=0; i<temp.getTotal(); i++)
			{
				if (i == newN) continue;
				Rectangle2D thisv = temp.getBBox(i);
				double dist = thisCenter.distance(thisv.getCenterX(), thisv.getCenterY());
				if (dist >= oldDist)
				{
					oldDist = dist;
					oldN = i;
				}
			}

			// allocate a new R-tree node
			RTNode newrtn = new RTNode();
			newrtn.setFlag(getFlag());
			newrtn.setParent(getParent());

			// put the first seed element into the new RTree
			Object obj = temp.getChild(newN);
			temp.setChild(newN, null);
			newrtn.setChild(0, obj);
			newrtn.setTotal(1);
			if (!newrtn.getFlag()) ((RTNode)obj).setParent(newrtn);
			Rectangle2D newBounds = newrtn.getBBox(0);
			newrtn.setBounds(newBounds);
			double newArea = newBounds.getWidth() * newBounds.getHeight();

			// initialize the old R-tree node and put in the other seed element
			obj = temp.getChild(oldN);
			temp.setChild(oldN, null);
			setChild(0, obj);
			for(int i=1; i<getTotal(); i++) setChild(i, null);
			setTotal(1);
			if (!getFlag()) ((RTNode)obj).setParent(this);
			Rectangle2D oldBounds = getBBox(0);
			setBounds(oldBounds);
			double oldArea = oldBounds.getWidth() * oldBounds.getHeight();

			// cluster the rest of the nodes
			for(;;)
			{
				// search for a cluster about each new node
				int bestNewNode = -1, bestOldNode = -1;
				double bestNewExpand = 0, bestOldExpand = 0;
				for(int i=0; i<temp.getTotal(); i++)
				{
					obj = temp.getChild(i);
					if (obj == null) continue;
					bounds = temp.getBBox(i);

					Rectangle2D newUnion = new Rectangle2D.Double();
					Rectangle2D oldUnion = new Rectangle2D.Double();
					Rectangle2D.union(newBounds, bounds, newUnion);
					Rectangle2D.union(oldBounds, bounds, oldUnion);
					double newAreaPlus = newUnion.getWidth() * newUnion.getHeight();
					double oldAreaPlus = oldUnion.getWidth() * oldUnion.getHeight();

					// remember the child that expands the new node the least
					if (bestNewNode < 0 || newAreaPlus-newArea < bestNewExpand)
					{
						bestNewExpand = newAreaPlus-newArea;
						bestNewNode = i;
					}

					// remember the child that expands the old node the least
					if (bestOldNode < 0 || oldAreaPlus-oldArea < bestOldExpand)
					{
						bestOldExpand = oldAreaPlus-oldArea;
						bestOldNode = i;
					}
				}

				// if there were no nodes added, all have been clustered
				if (bestNewNode == -1 && bestOldNode == -1) break;

				// if both selected the same object, select another "old node"
				if (bestNewNode == bestOldNode)
				{
					bestOldNode = -1;
					for(int i=0; i<temp.getTotal(); i++)
					{
						if (i == bestNewNode) continue;
						obj = temp.getChild(i);
						if (obj == null) continue;
						bounds = temp.getBBox(i);

						Rectangle2D oldUnion = new Rectangle2D.Double();
						Rectangle2D.union(oldBounds, bounds, oldUnion);
						double oldAreaPlus = oldUnion.getWidth() * oldUnion.getHeight();

						// remember the child that expands the old node the least
						if (bestOldNode < 0 || oldAreaPlus-oldArea < bestOldExpand)
						{
							bestOldExpand = oldAreaPlus-oldArea;
							bestOldNode = i;
						}
					}
				}

				// add to the proper "old node" to the old node cluster
				if (bestOldNode != -1)
				{
					// add this node to "rtn"
					obj = temp.getChild(bestOldNode);
					temp.setChild(bestOldNode, null);
					int curPos = getTotal();
					setChild(curPos, obj);
					setTotal(curPos+1);
					if (!getFlag()) ((RTNode)obj).setParent(this);
					unionBounds(getBBox(curPos));
					oldBounds = getBounds();
					oldArea = oldBounds.getWidth() * oldBounds.getHeight();
				}

				// add to proper "new node" to the new node cluster
				if (bestNewNode != -1)
				{
					// add this node to "newrtn"
					obj = temp.getChild(bestNewNode);
					temp.setChild(bestNewNode, null);
					int curPos = newrtn.getTotal();
					newrtn.setChild(curPos, obj);
					newrtn.setTotal(curPos+1);
					if (!newrtn.getFlag()) ((RTNode)obj).setParent(newrtn);
					newrtn.unionBounds(newrtn.getBBox(curPos));
					newBounds = newrtn.getBounds();
					newArea = newBounds.getWidth() * newBounds.getHeight();
				}
			}

			// sensibility check
			if (temp.getTotal() != getTotal() + newrtn.getTotal())
				System.out.println("R-trees: " + temp.getTotal() + " nodes split to " +
					getTotal()+ " and " + newrtn.getTotal() + "!");

			// now recursively insert this new element up the tree
			if (getParent() == null)
			{
				// at top of tree: create a new level
				RTNode newroot = new RTNode();
				newroot.setTotal(2);
				newroot.setChild(0, this);
				newroot.setChild(1, newrtn);
				newroot.setFlag(false);
				newroot.setParent(null);
				setParent(newroot);
				newrtn.setParent(newroot);
				newroot.figBounds();
				cell.setRTree(newroot);
			} else
			{
				// first recompute bounding box of R-tree nodes up the tree
				for(RTNode r = getParent(); r != null; r = r.getParent()) r.figBounds();

				// now add the new node up the tree
				getParent().addToRTNode(newrtn, cell);
			}
		}

		// now add "rtnInsert" to the R-tree node
		int curPos = getTotal();
		setChild(curPos, rtnInsert);
		setTotal(curPos+1);

		// compute the new bounds
		Rectangle2D bounds = getBBox(curPos);
		if (getTotal() == 1 && getParent() == null)
		{
			// special case when adding the first node in a cell
			setBounds(bounds);
			return;
		}

		// recursively update node sizes
		RTNode climb = this;
		for(;;)
		{
			climb.unionBounds(bounds);
			if (climb.getParent() == null) break;
			climb = climb.getParent();
		}

		// now check the RTree
//		checkRTree(0, cell);
	}

	/**
	 * Method to remove entry "ind" from this R-tree node in cell "cell"
	 */
	private void removeRTNode(int ind, Cell cell)
	{
		// delete entry from this R-tree node
		int j = 0;
		for(int i=0; i<getTotal(); i++)
			if (i != ind) setChild(j++, getChild(i));
		setTotal(j);

		// see if node is now too small
		if (getTotal() < MINRTNODESIZE)
		{
			// if recursed to top, shorten R-tree
			RTNode prtn = getParent();
			if (prtn == null)
			{
				// if tree has no hierarchy, allow short node
				if (getFlag())
				{
					// compute correct bounds of the top node
					figBounds();
					return;
				}

				// save all top-level entries
				RTNode temp = new RTNode();
				temp.setTotal(getTotal());
				temp.setFlag(true);
				for(int i=0; i<getTotal(); i++)
					temp.setChild(i, getChild(i));

				// erase top level
				setTotal(0);
				setFlag(true);

				// reinsert all data
				for(int i=0; i<temp.getTotal(); i++) ((RTNode)temp.getChild(i)).reInsert(cell);
				return;
			}

			// node has too few entries, must delete it and reinsert members
			int found = -1;
			for(int i=0; i<prtn.getTotal(); i++)
				if (prtn.getChild(i) == this) { found = i;   break; }
			if (found < 0) System.out.println("R-trees: cannot find entry in parent");

			// remove this entry from its parent
			prtn.removeRTNode(found, cell);

			// reinsert the entries
			reInsert(cell);
			return;
		}

		// recompute bounding box of this R-tree node and all up the tree
		RTNode climb = this;
		for(;;)
		{
			climb.figBounds();
			if (climb.getParent() == null) break;
			climb = climb.getParent();
		}
	}

	/**
	 * Method to reinsert the tree of nodes below this RTNode into cell "cell".
	 */
	private void reInsert(Cell cell)
	{
		if (getFlag())
		{
			for(int i=0; i<getTotal(); i++) linkGeom(cell, (Geometric)getChild(i));
		} else
		{
			for(int i=0; i<getTotal(); i++)
				((RTNode)getChild(i)).reInsert(cell);
		}
	}

	/**
	 * Method to find the location of geometry module "geom" in the R-tree
	 * below this.  The subnode that contains this module is placed in "subrtn"
	 * and the index in that subnode is placed in "subind".  The method returns
	 * null if it is unable to find the geometry module.
	 */
	private Object [] findGeom(Geometric geom)
	{
		// if R-tree node contains primitives, search for direct hit
		if (getFlag())
		{
			for(int i=0; i<getTotal(); i++)
			{
				if (getChild(i) == geom)
				{
					Object [] retObj = new Object[2];
					retObj[0] = this;
					retObj[1] = new Integer(i);
					return retObj;
				}
			}
			return null;
		}

		// recurse on all sub-nodes that would contain this geometry module
		Rectangle2D geomBounds = geom.getBounds();
		for(int i=0; i<getTotal(); i++)
		{
			// get bounds and area of sub-node
			Rectangle2D bounds = getBBox(i);

			if (bounds.getMaxX() < geomBounds.getMinX()-DBMath.getEpsilon()) continue;
			if (bounds.getMinX() > geomBounds.getMaxX()+DBMath.getEpsilon()) continue;
			if (bounds.getMaxY() < geomBounds.getMinY()-DBMath.getEpsilon()) continue;
			if (bounds.getMinY() > geomBounds.getMaxY()+DBMath.getEpsilon()) continue;
			Object [] subRet = ((RTNode)getChild(i)).findGeom(geom);
			if (subRet != null) return subRet;
		}
		return null;
	}

	/**
	 * Method to find the location of geometry module "geom" anywhere in the R-tree
	 * at "rtn".  The subnode that contains this module is placed in "subrtn"
	 * and the index in that subnode is placed in "subind".  The method returns
	 * false if it is unable to find the geometry module.
	 */
	private Object [] findGeomAnywhere(Geometric geom)
	{
		// if R-tree node contains primitives, search for direct hit
		if (getFlag())
		{
			for(int i=0; i<getTotal(); i++)
			{
				if (getChild(i) == geom)
				{
					Object [] retVal = new Object[2];
					retVal[0] = this;
					retVal[1] = new Integer(i);
					return retVal;
				}
			}
			return null;
		}

		// recurse on all sub-nodes
		for(int i=0; i<getTotal(); i++)
		{
			Object [] retVal = ((RTNode)getChild(i)).findGeomAnywhere(geom);
			if (retVal != null) return retVal;
		}
		return null;
	}

	/**
	 * Class to search a given area of a Cell.
	 * This class acts like an Iterator, returning Geometric objects that are inside the selected area.
	 * <P>
	 * For example, here is the code to search cell "myCell" in the area "bounds" (in database coordinates):
	 * <P>
	 * <PRE>
	 * for(RTNode.Search sea = <B>new RTNode.Search(bounds, cell)</B>; sea.hasNext(); )
	 * {
	 *     Geometric geom = (Geometric)sea.next();
	 *     if (geom instanceof NodeInst)
	 *     {
	 *         NodeInst ni = (NodeInst)geom;
	 *         // process NodeInst ni in the selected area
	 *     } else
	 *     {
	 *         ArcInst ai = (ArcInst)geom;
	 *         // process ArcInst ai in the selected area
	 *     }
	 * }
	 * </PRE>
	 */
	static class Search implements Iterator<Geometric>
	{
		/** maximum depth of search */			private static final int MAXDEPTH = 100;

		/** current depth of search */			private int depth;
		/** RTNode stack of search */			private RTNode [] rtn;
		/** index stack of search */			private int [] position;
		/** desired search bounds */			private Rectangle2D searchBounds;
		/** the next object to return */		private Geometric nextObj;
        /** includes objects on the search area edges */ private boolean includeEdges;

		Search(Rectangle2D bounds, Cell cell, boolean includeEdges)
		{
			this.depth = 0;
			this.rtn = new RTNode[MAXDEPTH];
			this.position = new int[MAXDEPTH];
			this.rtn[0] = cell.getRTree();
			this.searchBounds = new Rectangle2D.Double();
			this.searchBounds.setRect(bounds);
            this.includeEdges = includeEdges;
			this.nextObj = null;
		}

		/**
		 * Method to return the next object in the bounds of the search.
		 * @return the next object found.  Returns null when all objects have been reported.
		 */
		private Geometric nextObject()
		{
			for(;;)
			{
				RTNode rtnode = rtn[depth];
				int i = position[depth]++;
				if (i < rtnode.getTotal())
				{
					Rectangle2D nodeBounds = rtnode.getBBox(i);
                    if (includeEdges)
                    {
                        if (nodeBounds.getMaxX() < searchBounds.getMinX()) continue;
                        if (nodeBounds.getMinX() > searchBounds.getMaxX()) continue;
                        if (nodeBounds.getMaxY() < searchBounds.getMinY()) continue;
                        if (nodeBounds.getMinY() > searchBounds.getMaxY()) continue;
                    }
                    else
                    {
                        if (nodeBounds.getMaxX() <= searchBounds.getMinX()) continue;
                        if (nodeBounds.getMinX() >= searchBounds.getMaxX()) continue;
                        if (nodeBounds.getMaxY() <= searchBounds.getMinY()) continue;
                        if (nodeBounds.getMinY() >= searchBounds.getMaxY()) continue;
                    }
					if (rtnode.getFlag()) return((Geometric)rtnode.getChild(i));

					// look down the hierarchy
					if (depth >= MAXDEPTH-1)
					{
						System.out.println("R-trees: search too deep");
						continue;
					}
					depth++;
					rtn[depth] = (RTNode)rtnode.getChild(i);
					position[depth] = 0;
				} else
				{
					// pop up the hierarchy
					if (depth == 0) break;
					depth--;
				}
			}
			return null;
		}

		public boolean hasNext()
		{
			if (nextObj == null)
			{
				nextObj = nextObject();
			}
			return nextObj != null;
		}

		public Geometric next()
		{
			if (nextObj != null)
			{
				Geometric ret = nextObj;
				nextObj = null;
				return ret;
			}
			return nextObject();
		}

		public void remove() { throw new UnsupportedOperationException("Search.remove()"); };
	}
}

