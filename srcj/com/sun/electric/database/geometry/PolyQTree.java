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
	public void addTreeRoot(Object obj, Rectangle2D bound)
	{
		if (layers.get(obj) == null)
		{
			PolyQNode root = new PolyQNode(bound);
			layers.put(obj, root);
		}
	}

	private class PolyQNode {
		private Rectangle2D box;
		private List nodes;
		private PolyQNode[] children;

		/**
		 * @param rect the bounding box of the tree leaf
		 */
		public PolyQNode (Rectangle2D rect)
		{
			box = rect;
		}

		public void insert(Rectangle2D obj)
		{
			if (!box.intersects(obj)) return;

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
				Rectangle2D r = new Rectangle2D.Double();
				double w = box.getWidth()/2;
				double h = box.getHeight()/2;
				double x = box.getX();
				double y = box.getY();

				for (int i = 0; i < 4; i++)
				{
					r.setRect(x+w*(i%2), y+h*((i+1)%2), w, h);
					children[i] = new PolyQNode(r);
				}
				for (Iterator it = nodes.iterator(); it.hasNext();)
				{
					for (int i = 0; i < 4; i++)
					{
						children[i].insert((Rectangle2D)it.next());
					}
				}
				//remove list?
			}
		}
	}
}
