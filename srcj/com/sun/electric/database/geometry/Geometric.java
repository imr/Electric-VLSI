/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Geometric.java
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
package com.sun.electric.database.geometry;

import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.hierarchy.Cell;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;

/**
 * This class is the superclass for all Electric classes that have visual
 * bounds on the screen.  These include NodeInst and ArcInst, but NOT
 * NodeProto and its ilk, which have size, but aren't visual objects on
 * the screen.
 *
 * The methods in this class will take care of geometry caching: what
 * were R-trees in Electric may turn out to be many-to-many rectangular
 * buckets on the Java side.
 */
public class Geometric extends ElectricObject
{
	/** lower bound on R-tree node size */			private static final int MINRTNODESIZE = 4;
	/** upper bound on R-tree node size */			private static final int MAXRTNODESIZE = (MINRTNODESIZE*2);

	public static class Search
	{
		/** maximum depth of search */			private static final int MAXDEPTH = 100;

		/** current depth of search */			private int depth;
		/** RTNode stack of search */			private RTNode [] rtn;
		/** index stack of search */			private int [] position;
		/** lower-left corner of search area */	private double lX, lY;
		/** size of search area */				private double sX, sY;
		/** desired search bounds */			private Rectangle2D.Double bounds;

		public Search(Rectangle2D.Double bounds, Cell cell)
		{
			this.depth = 0;
			this.rtn = new RTNode[MAXDEPTH];
			this.position = new int[MAXDEPTH];
			this.rtn[0] = cell.getRTree();
			this.lX = bounds.getMinX();
			this.lY = bounds.getMinY();
			this.sX = bounds.getWidth();
			this.sY = bounds.getHeight();
			this.bounds = new Rectangle2D.Double();
			this.bounds.setRect(bounds);
		}

		/*
		 * second routine for searches: takes the search module returned by
		 * "initsearch" and returns the next geometry module in the
		 * search area.  If there are no more, this returns NOGEOM.
		 */
		public Geometric nextObject()
		{
			for(;;)
			{
				RTNode rtnode = rtn[depth];
				int i = position[depth]++;
				if (i < rtnode.getTotal())
				{
					Rectangle2D.Double bounds = rtnode.getBBox(i);
					if (sX == 0 && sY == 0)
					{
						if (!bounds.contains(lX, lY)) continue;
					} else
					{
						if (!bounds.intersects(lX, lY, sX, sY)) continue;
					}
					if (rtnode.getFlag()) return((Geometric)rtnode.getChild(i));

					/* look down the hierarchy */
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
					/* pop up the hierarchy */
					if (depth == 0) break;
					depth--;
				}
			}
			return null;
		}
	}

	public static class RTNode
	{
		/** bounds of this node and its children */	private Rectangle2D.Double bounds;
		/** number of children */					private int total;
		/** children */								private Object [] pointers;
		/** nonzero if children are terminal */		private boolean flag;
		/** parent node */							private RTNode parent;

		private RTNode()
		{
			pointers = new Object[MAXRTNODESIZE];
			bounds = new Rectangle2D.Double();
		}

		/** Routine to get the number of children of this RTNode. */
		public int getTotal() { return total; }
		/** Routine to set the number of children of this RTNode. */
		public void setTotal(int total) { this.total = total; }

		/** Routine to get the parent of this RTNode. */
		public RTNode getParent() { return parent; }
		/** Routine to set the parent of this RTNode. */
		public void setParent(RTNode parent) { this.parent = parent; }

		/** Routine to get the number of children of this RTNode. */
		public Object getChild(int index) { return pointers[index]; }
		/** Routine to set the number of children of this RTNode. */
		public void setChild(int index, Object obj) { this.pointers[index] = obj; }

		/** Routine to get the leaf/branch flag of this RTNode. */
		public boolean getFlag() { return flag; }
		/** Routine to set the leaf/branch flag of this RTNode. */
		public void setFlag(boolean flag) { this.flag = flag; }

		/** Routine to get the bounds of this RTNode. */
		public Rectangle2D.Double getBounds() { return bounds; }
		/** Routine to set the bounds of this RTNode. */
		public void setBounds(Rectangle2D.Double bounds) { this.bounds.setRect(bounds); }
		/** Routine to extend the bounds of this RTNode by "bounds". */
		public void unionBounds(Rectangle2D.Double bounds) { Rectangle2D.Double.union(this.bounds, bounds, this.bounds); }

		/**
		 * routine to create the top-level structure for a new cell.
		 */
		public static RTNode makeTopLevel()
		{
			RTNode top = new RTNode();
			top.total = 0;
			top.flag = true;
			top.parent = null;
			return top;
		}

		/**
		 * routine to get the bounding box of child "child" of this R-tree node.
		 */
		public Rectangle2D.Double getBBox(int child)
		{
			if (flag)
			{
				Geometric geom = (Geometric)pointers[child];
				return geom.getBounds();
			} else
			{
				RTNode subrtn = (RTNode)pointers[child];
				return subrtn.getBounds();
			}
		}


		/**
		 * routine to recompute the bounds of this R-tree node.
		 */
		void figBounds()
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

		private static int branchCount;

		public void printRTree(int indent)
		{
			if (indent == 0) branchCount = 0;

			String line = "";
			for(int i=0; i<indent; i++) line += " ";
			line += "RTNode";
			if (flag)
			{
				branchCount++;
				line += " NUMBER " + branchCount;
			}
			line += " X(" + bounds.getMinX() + "-" + bounds.getMaxX() + ") Y(" + bounds.getMinY() + "-" + bounds.getMaxY() + ") has " +
				total + " children:";
			System.out.println(line);

			for(int j=0; j<total; j++)
			{
				if (flag)
				{
					line = "";
					for(int i=0; i<indent+3; i++) line += " ";
					Geometric child = (Geometric)getChild(j);
					child.setTempInt(branchCount);
					Rectangle2D.Double childBounds = child.getBounds();
					line += "Child X(" + childBounds.getMinX() + "-" + childBounds.getMaxX() + ") Y(" +
						childBounds.getMinY() + "-" + childBounds.getMaxY() + ")";
					System.out.println(line);
				} else
				{
					((RTNode)getChild(j)).printRTree(indent+3);
				}
			}
		}

		/**
		 * routine to add object "rtnInsert" to this R-tree node, which is in cell "cell".  Routine may have to
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
				Rectangle2D.Double bounds;
				if (rtnInsert instanceof Geometric)
				{
					Geometric geom = (Geometric)rtnInsert;
					bounds = geom.getBounds();
				} else
				{
					RTNode subrtn = (RTNode)rtnInsert;
					bounds = subrtn.getBounds();
				}
				Point2D.Double thisCenter = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
				double newDist = 0;
				int newN = 0;
				for(int i=0; i<temp.getTotal(); i++)
				{
					Rectangle2D.Double thisv = temp.getBBox(i);
					double dist = EMath.computeDistance(new Point2D.Double(thisv.getCenterX(), thisv.getCenterY()), thisCenter);
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
				for(int i=0; i<temp.getTotal(); i++)
				{
					if (i == newN) continue;
					Rectangle2D.Double thisv = temp.getBBox(i);
					double dist = EMath.computeDistance(new Point2D.Double(thisv.getCenterX(), thisv.getCenterY()), thisCenter);
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
				Rectangle2D.Double newBounds = newrtn.getBBox(0);
				newrtn.setBounds(newBounds);
				double newArea = newBounds.getWidth() * newBounds.getHeight();

				// initialize the old R-tree node and put in the other seed element
				obj = temp.getChild(oldN);
				temp.setChild(oldN, null);
				setChild(0, obj);
				for(int i=1; i<getTotal(); i++) setChild(i, null);
				setTotal(1);
				if (!getFlag()) ((RTNode)obj).setParent(this);
				Rectangle2D.Double oldBounds = getBBox(0);
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

						Rectangle2D.Double newUnion = new Rectangle2D.Double();
						Rectangle2D.Double oldUnion = new Rectangle2D.Double();
						Rectangle2D.Double.union(newBounds, bounds, newUnion);
						Rectangle2D.Double.union(oldBounds, bounds, oldUnion);
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

							Rectangle2D.Double oldUnion = new Rectangle2D.Double();
							Rectangle2D.Double.union(oldBounds, bounds, oldUnion);
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
			Rectangle2D.Double bounds = getBBox(curPos);
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
		}
	}

	/**
	 * Routine to link this geometry module into the R-tree that is in cell "parnt".
	 */
	protected void linkGeom(Cell parnt)
	{
		// find the bottom-level branch (a RTNode with leafs) that would expand least by adding this Geometric
		RTNode rtn = parnt.getRTree();
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
				Rectangle2D.Double bounds = subrtn.getBounds();
				double area = bounds.getWidth() * bounds.getHeight();

				// get area of sub-node with new element
				Rectangle2D.Double newUnion = new Rectangle2D.Double();
				Rectangle2D.Double.union(visBounds, bounds, newUnion);
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
		rtn.addToRTNode(this, parnt);
	}

	// ------------------------------- private data ------------------------------

	// The internal representation of position and orientation is the
	// 2D transformation matrix:
	// --                                       --
	// |   sX cos(angleJ)    sY sin(angleJ)   0  |
	// |  -sX sin(angleJ)    sY cos(angleJ)   0  |
	// |        cX                cY          1  |
	// --                                       --

	/** Cell containing this Geometric object */			protected Cell parent;
	/** bounds after transformation */						private Rectangle2D.Double visBounds;
	/** center coordinate of this geometric */				protected double cX, cY;
	/** size of this geometric */							protected double sX, sY;
	/** angle of this geometric */							protected double angle;
	/** temporary integer value for the node or arc */		private int tempInt;

	// ------------------------ private and protected methods--------------------

	// create a new geometric object
	protected Geometric()
	{
	}

	protected void setParent(Cell parent) { this.parent = parent; }

	/** remove this geometric thing */
	public void remove()
	{
	}

	protected void getInfo()
	{
		System.out.println(" Parent: " + parent.describe());
		System.out.println(" Location: (" + cX + "," + cY + "), size: " + sX + "x" + sY + ", rotated " + angle * 180.0 / Math.PI);
		System.out.println(" Bounds: (" + visBounds.getCenterX() + "," + visBounds.getCenterY() + "), size: " +
			visBounds.getWidth() + "x" + visBounds.getHeight());
	}

	private AffineTransform rotateTranspose = null;

	public AffineTransform rotateAbout(double angle, double sX, double sY, double cX, double cY)
	{
		AffineTransform transform = new AffineTransform();
		if (sX < 0 || sY < 0)
		{
			// must do transposition, so it is trickier
			double cosine = Math.cos(angle);
			double sine = Math.sin(angle);
			if (rotateTranspose == null) rotateTranspose = new AffineTransform();
			rotateTranspose.setTransform(-sine, -cosine, -cosine, sine, 0.0, 0.0);
			transform.setToTranslation(cX, cY);
			transform.concatenate(rotateTranspose);
			transform.translate(-cX, -cY);
		} else
		{
			transform.setToRotation(angle, cX, cY);
		}
		return transform;
	}
	
	public void updateGeometricBounds()
	{
		// start with a unit polygon, centered at the origin
		Poly poly = new Poly(0.0, 0.0, 1.0, 1.0);

		// transform by the relevant amount
		AffineTransform scale = new AffineTransform();
		scale.setToScale(sX, sY);
		AffineTransform rotate = rotateAbout(angle, sX, sY, 0, 0);
		AffineTransform translate = new AffineTransform();
		translate.setToTranslation(cX, cY);
		rotate.concatenate(scale);
		translate.concatenate(rotate);

		poly.transform(translate);

		// return its bounds
		visBounds = poly.getBounds2DDouble();
	}

	// ------------------------ public methods -----------------------------

	/** Cell containing this Geometric object */
	public Cell getParent() { return parent; }

	Point2D.Double getCenter()
	{
		return new Point2D.Double(cX, cY);
	}

	/** Get X coordinate of this object's origin.  If this object is a
	 * NodeInst then get the X coordinate of the NodeProto's
	 * reference point. */
	public double getCenterX() { return cX; }

	/** Get Y coordinate of this object's origin.  If this object is a
	 * NodeInst then get the Y coordinate of the NodeProto's
	 * reference point. */
	public double getCenterY() { return cY; }

	/** Returns the angle of rotation in radians */
	public double getAngle() { return angle; }
	/** Returns the X size of the ArcInst or NodeInst. */
	public double getXSize() { return Math.abs(sX); }
	/** Returns the Y size of the ArcInst or NodeInst. */
	public double getYSize() { return Math.abs(sY); }
	/** Returns the bounds of the ArcInst or NodeInst. */
	public Rectangle2D.Double getBounds() { return visBounds; }

	/** Returns the temporary integer value. */
	public int getTempInt() { return tempInt; }
	/** Sets the temporary integer value. */
	public void setTempInt(int tempInt) { this.tempInt = tempInt; }

}
