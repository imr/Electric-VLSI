/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellBrowser.java
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

import java.awt.geom.Rectangle2D;
import java.util.*;

/**
 * This class represents a quad-tree to compute overlapping regions.
 * @author  Gilda Garreton
 * @version 0.1
 */
public class PolyQTree {
	private static int MAX_NUM_CHILDREN = 4;
	private static int MAX_DEPTH = 10;
	private static Rectangle2D testBox = new Rectangle2D.Double();
    private HashMap layers = new HashMap();

	//--------------------------PUBLIC METHODS--------------------------
	public PolyQTree()
	{
		;
	}

	/**
	 * Print all nodes in the tree. Debugging purpose only!.
	 */
	public void print()
	{
		for (Iterator it = getIterator(); it.hasNext();)
		{
			PolyQNode root = (PolyQNode)(it.next());
			if (root != null)
				root.print();
		}
	}

	/**
	 * Access to keySet with iterator
	 * @return iterator for keys in hashmap
	 */
	public Iterator getKeyIterator()
	{
		return (layers.keySet().iterator());
	}

	/**
	 * Iterator among all layers inserted
	 * @return
	 */
	public Iterator getIterator()
	{
		return (layers.values().iterator());
	}

	/**
	 * Retrieves list of leaf elements in the tree give a layer
	 * @param layer
	 * @return List contains all leave elements
	 */
	public Set getObjects(Object layer, List excludeList)
	{
		Set objSet = new HashSet();
		PolyQNode root = (PolyQNode)layers.get(layer);

		if (root != null)
		{
			root.getLeafObjects(objSet, excludeList);
		}
		return (objSet);
	}

	/**
	 * Given a layer, insert the object obj into the qTree associated.
	 * @param layer Given layer to work with
	 * @param box Bounding box of the cell containing the layer
	 */
	public void insert(Object layer, Rectangle2D box, Rectangle2D obj)
	{
		PolyQNode root = (PolyQNode)layers.get(layer);

		if (root == null)
		{
			root = new PolyQNode();
			layers.put(layer, root);
		};
		// Check whether original got changed! shouldn't happen because they are by value
		testBox.setRect(obj);
		//testBox = root.removeObjects(box, testBox);
		// Only if no other identical element was found, element is inserted
		if (!root.findAndRemoveObjects(box, testBox))
			root.insert(box, testBox);
	}

	//--------------------------PRIVATE METHODS--------------------------
	private static class PolyQNode
    {
		private Set nodes; // If Set, no need to check whether they are duplicated or not. Java will do it for you
		private PolyQNode[] children;

		/**
		 *
		 */
		private PolyQNode () { ; }

		private int getQuadrants(double centerX, double centerY, Rectangle2D box)
		{
		   	int loc = 0;

			if (box.getMinY() < centerY)
			{
				// either 0 or 1 quadtrees
				if (box.getMinX() < centerX)
					loc |= 1 << 0;
				if (box.getMaxX() > centerX)
					loc |= 1 << 1;
			}
			if (box.getMaxY() > centerY)
			{
				// the other quadtrees
				if (box.getMinX() < centerX)
					loc |= 1 << 2;
				if (box.getMaxX() > centerX)
					loc |= 1 << 3;
			}
			return loc;
		}

		private Rectangle2D getBox(Rectangle2D box, double centerX, double centerY, int loc)
		{
			double w = box.getWidth()/2;
			double h = box.getHeight()/2;
			// Values for quadtree 0
			double x = box.getX();
			double y = box.getY();

			if ((loc >> 1 & 1) == 0)
			{
				x = centerX;
			}
			else if ((loc >> 2 & 1) == 0)
			{
				y = centerY;
			}
			else if ((loc >> 3 & 1) == 0)
			{
				x = centerX;
				y = centerY;
			}
			return (new Rectangle2D.Double(x, y, w, h));
		}

		/**
		 * Collects recursive leaf elements in a list. Uses set to avoid
		 * duplicate elements (qtree could sort same element in all quadrants
		 * @param set
		 * @param excludeList list of original geometries
		 */
		protected void getLeafObjects(Set set, List excludeList)
		{
			if (nodes != null)
			{
				set.addAll(nodes);
				if ( excludeList != null )
				{
					// Make sure objects are not the originals
					// Not sure how efficient this is
					set.removeAll(excludeList);
				}
			}
			if (children == null) return;
			for (int i = 0; i < PolyQTree.MAX_NUM_CHILDREN; i++)
				children[i].getLeafObjects(set, excludeList);
		}
		/**
		 *   print function for debugging purposes
		 */
		protected void print()
		{
			if (nodes == null) return;
			for (Iterator it = nodes.iterator(); it.hasNext();)
				System.out.println("Rectangle " + (Rectangle2D)it.next());
			if (children == null) return;
			for (int i = 0; i < PolyQTree.MAX_NUM_CHILDREN; i++)
				children[i].print();
		}

		/**
		 * To compact nodes if child elements have been removed
		 * @return true if node can be removed
		 */
		private boolean compact()
		{
			System.out.println("To implement") ;
			return (false);
		}

		/**
		 * Original Rectangle2D:intersects doesn't detect when two elements are touching
		 * @param a
		 * @param b
		 * @return
		 */
		private static boolean intersects(Rectangle2D a, Rectangle2D b)
		{
			double x = b.getX();
			double y = b.getY();
			double w = b.getWidth();
			double h = b.getHeight();

			if (a.isEmpty() || w <= 0 || h <= 0) {
	            return false;
			}
			double x0 = a.getX();
			double y0 = a.getY();
			return ((x + w) >= x0 &&
				(y + h) >= y0 &&
				x <= (x0 + a.getWidth()) &&
				y <= (y0 + a.getHeight()));
		}

		/**
		 * Removes from tree all objects overlapping with obj. Returns the overlapping region.
		 * @param box
		 * @param obj
		 * @return
		 */
		protected boolean findAndRemoveObjects(Rectangle2D box, Rectangle2D obj)
		{
			double centerX = box.getCenterX();
            double centerY = box.getCenterY();

            // Node has been split
			if (children != null)
			{
				int loc = getQuadrants(centerX, centerY, obj);
				for (int i = 0; i < PolyQTree.MAX_NUM_CHILDREN; i++)
				{
					if (((loc >> i) & 1) == 0)
					{
						Rectangle2D bb = getBox(box, centerX, centerY, i);
						/*
                            Object test = obj.clone();
						Rectangle2D test1 = children[i].removeObjects(bb, obj);
						if ( !test1.equals(obj))
						          System.out.println("Parar");
						          */
						// if identical element was found, no need of re-insertion
						// No need of reviewing other quadrants?
						if (children[i].findAndRemoveObjects(bb, obj))
							return (true);

						if (children[i].compact())
							children[i] = null;
					}
				}
			}
			else if (nodes != null)
			{
				List deleteList = new ArrayList();

				for (Iterator it = nodes.iterator(); it.hasNext();)
				{
					Rectangle2D node = (Rectangle2D)it.next();

					// I should test equals first?
					if (node.equals(obj))
						return (true);
					if (node.intersects(obj))
					//if (!node.equals(obj) && intersects(node, obj))
					{
						obj.add(node);
						deleteList.add(node);
					}
				}
				nodes.removeAll(deleteList);
				if (nodes.size() == 0)
				{
					// not sure yet
					nodes.clear();
					nodes = null;
				}
			}
			// No identical element found
			return (false);
		}

		/**
		 *
		 * @param box
		 * @param obj
		 */
		protected void insert(Rectangle2D box, Rectangle2D obj)
		{
			double centerX = box.getCenterX();
            double centerY = box.getCenterY();

			// Node has been split
			if (children != null)
			{
				int loc = getQuadrants(centerX, centerY, obj);
				for (int i = 0; i < PolyQTree.MAX_NUM_CHILDREN; i++)
				{
					if (((loc >> i) & 1) == 0)
					{
						Rectangle2D bb = getBox(box, centerX, centerY, i);

						children[i].insert(bb, obj);
					}
				}
			}
			if (nodes == null)
			{
				nodes = new HashSet();
			}
			if (nodes.size() < PolyQTree.MAX_NUM_CHILDREN)
			{
				nodes.add(obj.clone());
			}
			else
			{
				// subdivides into 4
				children = new PolyQNode[4];

				for (int i = 0; i < 4; i++)
				{
					children[i] = new PolyQNode();
				}

				for (Iterator it = nodes.iterator(); it.hasNext();)
				{
					for (int i = 0; i < 4; i++)
					{
						Rectangle2D bb = getBox(box, centerX, centerY, i);
						children[i].insert(bb, (Rectangle2D)it.next());
					}
				}
				nodes.clear(); // not sure about this clear yet
				nodes = null;
			}
		}
	}
}
