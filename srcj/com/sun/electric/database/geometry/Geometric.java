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
	/** lower bound on R-tree node size */	private static final int MINRTNODESIZE =              4;
	/** upper bound on R-tree node size */	private static final int MAXRTNODESIZE =   (MINRTNODESIZE*2);

	public static class RTNode
	{
		/** bounds of this node */					private Rectangle2D.Double bounds;
		/** number of pointers */					private int total;
		/** nonzero if children are terminal */		private boolean flag;
		/** pointers */								private Object [] pointers;
		/** parent node */							private RTNode parent;

		private RTNode()
		{
			pointers = new Object[MAXRTNODESIZE];
		}

		public static RTNode makeTopLevel()
		{
			RTNode top = new RTNode();
			top.bounds = new Rectangle2D.Double(0, 0, 0, 0);
			top.total = 0;
			top.flag = true;
			top.parent = null;
			return top;
		}

		/** Routine to get the number of children of this RTNode.*/
		public int getTotal() { return total; }
		/** Routine to set the number of children of this RTNode. */
		public void setTotal(int total) { this.total = total; }

		/** Routine to get the parent of this RTNode.*/
		public RTNode getParent() { return parent; }
		/** Routine to set the parent of this RTNode. */
		public void setParent(RTNode parent) { this.parent = parent; }

		/** Routine to get the number of children of this RTNode.*/
		public Object getChild(int index) { return pointers[index]; }
		/** Routine to set the number of children of this RTNode. */
		public void setChild(int index, Object obj) { this.pointers[index] = obj; }

		/** Routine to get the leaf/branch flag of this RTNode.*/
		public boolean getFlag() { return flag; }
		/** Routine to set the leaf/branch flag of this RTNode. */
		public void setFlag(boolean flag) { this.flag = flag; }

		/** Routine to get the bounds of this RTNode.*/
		public Rectangle2D.Double getBounds() { return bounds; }
		/** Routine to set the bounds of this RTNode.*/
		public void setBounds(Rectangle2D.Double bounds) { this.bounds = bounds; }

		/**
		 * routine to get the bounding box of child "child" of this R-tree node.
		 */
		Rectangle2D.Double getBBox(int child)
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
		 * routine to get the bounding box of child "child" of this R-tree node.
		 */
		static Rectangle2D.Double getBBox(Object obj)
		{
			if (obj instanceof Geometric)
			{
				Geometric geom = (Geometric)obj;
				return geom.getBounds();
			}
			if (obj instanceof RTNode)
			{
				RTNode subrtn = (RTNode)obj;
				return subrtn.getBounds();
			}
			return null;
		}
	}

	/**
	 * routine to link geometry module "geom" into the R-tree.  The parent
	 * nodeproto is in "parnt".
	 */
	void linkGeom(Cell parnt)
	{
//		/* find the leaf that would expand least by adding this node */
//		RTNode rtn = parnt.getRTree();
//		for(;;)
//		{
//			/* if R-tree node contains primitives, exit loop */
//			if (rtn.getFlag()) break;
//
//			/* find sub-node that would expand the least */
//			double bestExpand = 0;
//			int bestSubNode = -1;
//			for(int i=0; i<rtn.getTotal(); i++)
//			{
//				/* get bounds and area of sub-node */
//				RTNode subrtn = (RTNode)rtn.getChild(i);
//				double lxv = subrtn.getMinX();
//				double hxv = subrtn.getMaxX();
//				double lyv = subrtn.getMinY();
//				double hyv = subrtn.getMaxY();
//				double area = (hxv - lxv) * (hyv - lyv);
//
//				/* get area of sub-node with new element */
//				double geomLX = cX - sX/2;
//				double geomHX = cX + sX/2;
//				double geomLY = cY - sY/2;
//				double geomHY = cY + sY/2;
//				if (geomHX > hxv) hxv = geomHX;
//				if (geomLX < lxv) lxv = geomLX;
//				if (geomHY > hyv) hyv = geomHY;
//				if (geomLY < lyv) lyv = geomLY;
//				double newarea = (hxv - lxv) * (hyv - lyv);
//
//				/* accumulate the least expansion */
//				double expand = newarea - area;
//
//				/* LINTED "bestexpand" used in proper order */
//				if (i != 0 && expand > bestExpand) continue;
//				bestExpand = expand;
//				bestSubNode = i;
//			}
//
//			/* recurse down to sub-node that expanded least */
//			rtn = (RTNode)rtn.getChild(bestSubNode);
//		}
//
//		/* add this geometry element to the correct leaf R-tree node */
//		addToRTNode(rtn, parnt);
	}

	/**
	 * routine to add this Geometric to R-tree node "rtn".  Routine may have to
	 * split the node and recurse up the tree
	 */
	void addToRTNode(RTNode rtn, Cell cell)
	{
//		/* see if there is room in the R-tree node */
//		if (rtn.getTotal() >= MAXRTNODESIZE)
//		{
//			/*
//			 * no room: find the element farthest from new object, copy list
//			 */
//			RTNode temp = new RTNode();
//			double newDist = 0;
//			int newN = -1;
//			Rectangle2D.Double bounds = this.visBounds;
//			for(int i=0; i<rtn.getTotal(); i++)
//			{
//				Object obj = rtn.getChild(i);
//				temp.setChild(i, obj);
//				Rectangle2D.Double thisv = RTNode.getBBox(obj);
//				double dist = EMath.computeDistance(thisv.getCenterX(), thisv.getCenterY(),
//					bounds.getCenterX(), bounds.getCenterY());
//				if (dist >= newDist)
//				{
//					newDist = dist;
//					newN = i;
//				}
//			}
//			int oldcount = rtn.getTotal();
//
//			/* now find element farthest from "newN" */
//			double oldDist = 0;
//			int oldN = -1;
//			bounds = RTNode.getBBox(rtn.getChild(newN));
//			for(int i=0; i<rtn.getTotal(); i++)
//			{
//				if (i == newn) continue;
//				Rectangle2D.Double thisv = RTNode.getBBox(rtn.getChild(i));
//				double dist = computedistance(thisv.getCenterX(), thisv.getCenterY(),
//					bounds.getCenterX(), bounds.getCenterY());
//				if (dist >= oldDist)
//				{
//					oldDist = dist;
//					oldN = i;
//				}
//			}
//
//			/* allocate a new R-tree node and put in first seed element */
//			RTNode newrtn = new RTNode();
//			newrtn.setFlag(rtn.getFlag());
//			newrtn.setParent(rtn.getParent());
//
//			/* put the first seed element into the new RTree */
//			Object obj = temp.getChild(newN);
//			newrtn.setChild(0, obj);
//			temp.setChild(newN, null);
//			newrtn.setTotal(1);
//			Rectangle2D.Double newBounds = RTNode.getBBox(obj);
//			newrtn.setBounds(newBounds);
//			double newArea = newBounds.getWidth() * newBounds.getHeight();
////			if (!newrtn.getFlag()) ((RTNODE *)newrtn->pointers[0])->parent = newrtn;
//
//			/* initialize the old R-tree node and put in the other seed element */
//			Object obj = temp.getChild(oldN);
//			rtn.setChild(0, obj);
//			temp.setChild(oldN, null);
//			rtn.setTotal(1);
//			Rectangle2D.Double oldBounds = RTNode.getBBox(obj);
//			rtn.setBounds(oldBounds);
//			double oldArea = oldBounds.getWidth() * oldBounds.getHeight();
//	//		if (rtn->flag == 0) ((RTNODE *)rtn->pointers[0])->parent = rtn;
//
//			/* cluster the rest of the nodes */
//			for(;;)
//			{
//				/* search for a cluster about each new node */
//				int bestNewNode = -1, bestOldNode = -1;
//				double bestNewExpand, bestOldExpand;
//				for(int i=0; i<oldCount; i++)
//				{
//					Object obj = temp.getChild(i);
//					if (obj == null) continue;
//					bounds = RTNode.getBBox(obj);
//
//					Rectangle2D.Double newUnion, oldUnion;
//					Rectangle2D.Double.union(newBounds, bounds, newUnion);
//					Rectangle2D.Double.union(oldBounds, bounds, oldUnion);
//					double newAreaPlus = newUnion.getWidth() * newUnion.getHeight();
//					double oldAreaPlus = oldUnion.getWidth() * oldUnion.getHeight();
//
//					/* LINTED "bestnewexpand" used in proper order */
//					if (bestNewNode < 0 || newAreaPlus-newArea < bestNewExpand)
//					{
//						bestNewExpand = newAreaPlus-newArea;
//						bestNewNode = i;
//					}
//
//					/* LINTED "bestoldexpand" used in proper order */
//					if (bestOldNode < 0 || oldAreaPlus-oldArea < bestOldExpand)
//					{
//						bestOldExpand = oldAreaPlus-oldArea;
//						bestOldNode = i;
//					}
//				}
//
//				/* if there were no nodes added, all have been clustered */
//				if (bestNewNode == -1 && bestOldNode == -1) break;
//
//				/* if both selected the same object, select another "oldn" */
//				if (bestNewNode == bestOldNode)
//				{
//					bestOldNode = -1;
//					for(int i=0; i<oldcount; i++)
//					{
//						Object obj = temp.getChild(i);
//						if (obj == null) continue;
//						if (i == bestNewNode) continue;
//						bounds = RTNode.getBBox(obj);
//
////					Rectangle2D.Double newUnion, oldUnion;
////					Rectangle2D.Double.union(newBounds, bounds, newUnion);
////					Rectangle2D.Double.union(oldBounds, bounds, oldUnion);
////					double newAreaPlus = newUnion.getWidth() * newUnion.getHeight();
////					double oldAreaPlus = oldUnion.getWidth() * oldUnion.getHeight();
//
//						oldareaplus = mmaxi(hxv, rtn->highx) - mmini(lxv, rtn->lowx);
//						oldareaplus *= mmaxi(hyv, rtn->highy) - mmini(lyv, rtn->lowy);
//						if (bestOldNode < 0 || oldareaplus-oldarea < bestoldexpand)
//						{
//							bestoldexpand = oldareaplus-oldarea;
//							bestOldNode = i;
//						}
//					}
//				}
//
//				/* add to "oldn" cluster */
//				if (bestOldNode != -1)
//				{
//					/* add this node to "rtn" */
//					rtn->pointers[rtn->total] = temp.pointers[bestOldNode];
//					temp.pointers[bestOldNode] = (UINTBIG)NORTNODE;
//					if (rtn->flag == 0) ((RTNODE *)rtn->pointers[rtn->total])->parent = rtn;
//					db_rtnbbox(rtn, rtn->total, &lxv, &hxv, &lyv, &hyv);
//					rtn->total++;
//					if (lxv < rtn->lowx) rtn->lowx = lxv;
//					if (hxv > rtn->highx) rtn->highx = hxv;
//					if (lyv < rtn->lowy) rtn->lowy = lyv;
//					if (hyv > rtn->highy) rtn->highy = hyv;
//					oldarea = rtn->highx - rtn->lowx;
//					oldarea *= rtn->highy - rtn->lowy;
//				}
//
//				/* add to "newn" cluster */
//				if (bestNewNode != -1)
//				{
//					/* add this node to "newrtn" */
//					newrtn->pointers[newrtn->total] = temp.pointers[bestNewNode];
//					temp.pointers[bestNewNode] = (UINTBIG)NORTNODE;
//					if (newrtn->flag == 0) ((RTNODE *)newrtn->pointers[newrtn->total])->parent = newrtn;
//					db_rtnbbox(newrtn, newrtn->total, &lxv, &hxv, &lyv, &hyv);
//					newrtn->total++;
//					if (lxv < newrtn->lowx) newrtn->lowx = lxv;
//					if (hxv > newrtn->highx) newrtn->highx = hxv;
//					if (lyv < newrtn->lowy) newrtn->lowy = lyv;
//					if (hyv > newrtn->highy) newrtn->highy = hyv;
//					newarea = newrtn->highx - newrtn->lowx;
//					newarea *= newrtn->highy - newrtn->lowy;
//				}
//			}
//
//			/* sensibility check */
//			if (oldcount != rtn->total + newrtn->total)
//				ttyputerr(_("R-trees: %ld nodes split to %d and %d!"),
//					oldcount, rtn->total, newrtn->total);
//
//			/* now recursively insert this new element up the tree */
//			if (rtn->parent == NORTNODE)
//			{
//				/* at top of tree: create a new level */
//				newroot = allocrtnode(cell->lib->cluster);
//				if (newroot == 0) return(TRUE);
//				newroot->total = 2;
//				newroot->pointers[0] = (UINTBIG)rtn;
//				newroot->pointers[1] = (UINTBIG)newrtn;
//				newroot->flag = 0;
//				newroot->parent = NORTNODE;
//				rtn->parent = newrtn->parent = newroot;
//				newroot->lowx = mmini(rtn->lowx, newrtn->lowx);
//				newroot->highx = mmaxi(rtn->highx, newrtn->highx);
//				newroot->lowy = mmini(rtn->lowy, newrtn->lowy);
//				newroot->highy = mmaxi(rtn->highy, newrtn->highy);
//				cell->rtree = newroot;
//			} else
//			{
//				/* first recompute bounding box of R-tree nodes up the tree */
//				for(r = rtn->parent; r != NORTNODE; r = r->parent) db_figbounds(r);
//
//				/* now add the new node up the tree */
//				if (db_addtortnode((UINTBIG)newrtn, rtn->parent, cell)) return(TRUE);
//			}
//		}
//
//		/* now add this element to the R-tree node */
//		rtn->pointers[rtn->total] = object;
//		db_rtnbbox(rtn, rtn->total, &lxv, &hxv, &lyv, &hyv);
//		rtn->total++;
//
//		/* special case when adding the first node in a cell */
//		if (rtn->total == 1 && rtn->parent == NORTNODE)
//		{
//			rtn->lowx = lxv;
//			rtn->highx = hxv;
//			rtn->lowy = lyv;
//			rtn->highy = hyv;
//			return(FALSE);
//		}
//
//		/* recursively update node sizes */
//		for(;;)
//		{
//			rtn->lowx = mmini(rtn->lowx, lxv);
//			rtn->highx = mmaxi(rtn->highx, hxv);
//			rtn->lowy = mmini(rtn->lowy, lyv);
//			rtn->highy = mmaxi(rtn->highy, hyv);
//			if (rtn->parent == NORTNODE) break;
//			rtn = rtn->parent;
//		}
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
