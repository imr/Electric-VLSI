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
    private HashMap layers = new HashMap();

	public PolyQTree()
	{
		;
	}
	/**
	 * Print all nodes in the tree. Debugging purpose only!.
	 */
	public void print()
	{
		for (Iterator it = layers.values().iterator(); it.hasNext();)
		{
			PolyQNodeRoot root = (PolyQNodeRoot)(it.next());
			if (root != null)
				root.print();
		}
	}

	/**
	 * Given a layer, insert the object obj into the qTree associated.
	 * @param layer Given layer to work with
	 * @param box Bounding box of the cell containing the layer
	 * @param obj Poly object to insert
	 */
	public void insert(Object layer, Rectangle2D box, Rectangle2D obj)
	{
		PolyQNodeRoot root = (PolyQNodeRoot)layers.get(layer);

		if (root == null)
		{
			root = new PolyQNodeRoot(box);
			layers.put(layer, root);
		};
		// check first if there is some overlap and removes previous elements
		obj = root.removeObjects(box, obj);
		root.insert(box, obj);
	}

	private static class PolyQNode
    {
		private List nodes;
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
		 *
		 */
		protected void print()
		{
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
		 * Removes from tree all objects overlapping with obj. Returns the overlapping region.
		 * @param box
		 * @param obj
		 * @return
		 */
		protected Rectangle2D removeObjects(Rectangle2D box, Rectangle2D obj)
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

						obj = children[i].removeObjects(bb, obj);
						if (children[i].compact())
							children[i] = null;
					}
				}
			}
			else if (nodes == null)
			{
				System.out.println("Should it happen!!?");
			}
			else
			{
				List deleteList = new ArrayList();

				for (Iterator it = nodes.iterator(); it.hasNext();)
				{
					Rectangle2D node = (Rectangle2D)it.next();

					if (node.intersects(box))
					{
						box.add(node);
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
			return (obj);
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
				nodes = new ArrayList();
			}
			if (nodes.size() < PolyQTree.MAX_NUM_CHILDREN)
			{
				nodes.add(obj);
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
	private class PolyQNodeRoot extends PolyQTree.PolyQNode
	{
		private Rectangle2D box; // contain the bounding box of the entire cell

		public PolyQNodeRoot (Rectangle2D bound)
		{
			box = bound;
		}
	}
}
