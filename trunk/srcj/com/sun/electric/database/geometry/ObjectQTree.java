/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ObjectQTree.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.geometry;

import java.util.Set;
import java.util.HashSet;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Point2D;

/**
 * User: Gilda
 * Date: Feb 14, 2006
 */
public class ObjectQTree {
    private static int MAX_NUM_CHILDREN = 4;
    private static int MAX_NUM_NODES = 10;
    private ObjectQNode root;

    /**
     * Constructor
     * @param box represents the bounding box of the root leaf
     */
    public ObjectQTree(Rectangle2D box)
    {
        root = new ObjectQNode(box);
    }

    /**
     * Method to insert new element into qTree
     * @param newObj
     * @param rect
     * @return true if the element was inserteed
     */
    public boolean add(Object newObj, Rectangle2D rect)
    {
        return (root.insert(new ObjectNode(newObj, rect)));
    }

    /**
     * Method to print the qTree elements
     */
    public void print()
    {
        if (root != null)
            root.print();
    }

    /**
     * Method to find set of elements overlaping the search box.
     * @param searchB
     * @return Set containding all objects inside the given bounding box
     */
    public Set find(Rectangle2D searchB)
    {
        return (root.find(searchB, null));
    }

    private static class ObjectNode
    {
        private Object elem;
        private Rectangle2D rect;
        private boolean isEmpty;

        ObjectNode(Object e, Rectangle2D r)
        {
            this.elem = e;
            this.rect = r;
            this.isEmpty = r.isEmpty();
        }

        boolean intersectsWithBox(Rectangle2D box)
        {
            if (!isEmpty)
                return box.intersects(rect);
            // if empty. Along the perimeter of the bounding box is also valid

            return DBMath.pointInRect(new Point2D.Double(rect.getMinX(), rect.getMinY()), box);
        }
        Rectangle2D getBounds() {return rect;}
    }

    private static class ObjectQNode
    {
		private Set<ObjectNode> nodes; // If Set, no need to check whether they are duplicated or not. Java will do it for you
		private ObjectQNode[] children;
        private Rectangle2D box; // bounding box of this quadrant

		/**
		 *
		 */
		private ObjectQNode (Rectangle2D b) { box = b; }

        public void print()
        {
            System.out.println("Node Box " + box.toString());
            if (nodes != null)
                for (ObjectNode node : nodes)
                    System.out.println("Node " + node.elem.toString());
            if (children != null)
			{
				for (int i = 0; i < MAX_NUM_CHILDREN; i++)
					if (children[i] != null)
                    {
                        System.out.print("Quadrant " + i);
                        children[i].print();
                    }
            }
        }

        /**
		 * To make sure new element is inserted in all childres
		 * @param centerX To avoid calculation inside function from object box
		 * @param centerY To avoid calculation inside function from object box
		 * @param obj ObjectNode to insert
		 * @return True if element was inserted
		 */
		protected boolean insertInAllChildren(double centerX, double centerY,
                                              ObjectNode obj)
		{
			int loc = GenMath.getQuadrants(centerX, centerY, obj.getBounds());
			boolean inserted = false;
			double w = box.getWidth()/2;
			double h = box.getHeight()/2;
			double x = box.getX();
			double y = box.getY();

			for (int i = 0; i < MAX_NUM_CHILDREN; i++)
			{
				if (((loc >> i) & 1) == 1)
				{
					Rectangle2D bb = GenMath.getQTreeBox(x, y, w, h, centerX, centerY, i);

					if (children[i] == null) children[i] = new ObjectQNode(bb);

					boolean done = children[i].insert(obj);

					inserted = (inserted) ? inserted : done;
				}
			}
			return (inserted);
		}

        protected Set<Object> find(Rectangle2D searchBox, Set<Object> list)
        {
            if (!box.intersects(searchBox)) return list;
            if (list == null)
                list = new HashSet<Object>();
            if (children != null)
            {
                for (int i = 0; i < MAX_NUM_CHILDREN; i++)
                {
                    list = children[i].find(searchBox, list);
                }
            }
            else
            {
                if (nodes != null)
                {
                    for (ObjectNode node : nodes)
                    {
                        // consider if node.rect is empty  -> it is a point (degenerated rectangle)
                        if (node.intersectsWithBox(searchBox))
//                        if (searchBox.intersects(node.rect))
                            list.add(node.elem);
                    }
                }
            }
            return list;
        }

		/**
		 * Method to insert the element in each quadrant
		 * @param obj ObjectNode to insert
		 * @return if node was really inserted
		 */
		protected boolean insert(ObjectNode obj)
		{
            // if bb is not empty, we check intersection. If empty (point), we check if it is contained in box
            if (!obj.intersectsWithBox(box))
			{
				// new element is outside of bounding box. Might need flag to avoid
				// double checking if obj is coming from findAndRemove
				return (false);
			}

			double centerX = box.getCenterX();
            double centerY = box.getCenterY();

			// Node has been split
			if (children != null)
			{
				return (insertInAllChildren(centerX, centerY, obj));
			}
			if (nodes == null)
			{
				nodes = new HashSet<ObjectNode>();
			}
			boolean inserted; // = false;

			if (nodes.size() < MAX_NUM_NODES)
			{
				inserted = nodes.add(obj);
			}
			else
			{
				// subdivides into MAX_NUM_CHILDREN. Might work only for 2^n
				children = new ObjectQNode[MAX_NUM_CHILDREN];
				double w = box.getWidth()/2;
				double h = box.getHeight()/2;
				double x = box.getX();
				double y = box.getY();

				// Redistributing existing elements in children
				for (int i = 0; i < MAX_NUM_CHILDREN; i++)
				{
					Rectangle2D bb = GenMath.getQTreeBox(x, y, w, h, centerX, centerY, i);
					children[i] = new ObjectQNode(bb);

					for (ObjectNode node : nodes)
					{
						children[i].insert(node);
					}
				}
//				nodes.clear(); // not sure about this clear yet
				nodes = null;
				inserted = insertInAllChildren(centerX, centerY, obj);
			}
			return (inserted);
		}
	}
}
