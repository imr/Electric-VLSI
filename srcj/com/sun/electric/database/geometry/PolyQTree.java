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

import java.awt.geom.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * This class represents a quad-tree to compute overlapping regions.
 * @author  Gilda Garreton
 * @version 0.1
 */
public class PolyQTree
        implements GeometryHandler
{
	private static int MAX_NUM_CHILDREN = 4;
	//private static int MAX_DEPTH = 10;
    private HashMap layers = new HashMap();
	private Rectangle2D rootBox;

	//--------------------------PUBLIC METHODS--------------------------
	public PolyQTree(Rectangle2D root)
	{
		rootBox = root;
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
	 * Retrieves list of leaf elements in the tree for a given layer
	 * @param layer Layer under analysis
	 * @param modified True if only the original elements should not be retrieved
	 * @param simple True if simple elements should be retrieved
	 * @return list of leaf elements
	 */
	public Collection getObjects(Object layer, boolean modified, boolean simple)
	{
		Set objSet = new HashSet();
		PolyQNode root = (PolyQNode)layers.get(layer);

		if (root != null)
		{
			root.getLeafObjects(objSet, modified, simple);
		}
		return (objSet);
	}

	/**
	 * Given a layer, insert the object obj into the qTree associated.
	 * @param layer Given layer to work with
	 * @param newObj
	 */
	public void add(Object layer, Object newObj)
	{
		PolyNode obj = (PolyNode)newObj;
		PolyQNode root = (PolyQNode)layers.get(layer);

		if (root == null)
		{
			root = new PolyQNode();
			layers.put(layer, root);
		};
		// Only if no other identical element was found, element is inserted
		Rectangle2D areaBB = obj.getBounds2D();
		boolean done = root.findAndRemoveObjects(rootBox, obj, areaBB);

		if (!done)
			done = root.insert(rootBox, obj, areaBB);
		//@TODO GVG Check this case
		// Could be comming from big-cross poly!!
		if (!done)
			System.out.println("Repeated element?");
	}

	/**
	 *  Merge two PolyQTree
	 * @param subMerge
	 * @param trans
	 */
	public void addAll(GeometryHandler subMerge, AffineTransform trans)
	{
		PolyQTree other = (PolyQTree)subMerge;

		for(Iterator it = other.layers.keySet().iterator(); it.hasNext();)
		{
			Object layer = it.next();
			Set set = (Set)other.getObjects(layer, false, false);

			for(Iterator i = set.iterator(); i.hasNext(); )
			{
				PolyNode geo = (PolyNode)i.next();
				geo.transform(trans);
				add(layer, geo);
			}
		}
	}

	//--------------------------PRIVATE METHODS--------------------------
	public static class PolyNode extends Area
	{
		private byte original;

		public PolyNode(Shape shape)
		{
			super(shape);
		}

		public boolean equals(Object obj)
		{
			// reflexive
			if (obj == this) return true;

			// should consider null case
			// symmetry but violates transitivity?
			// It seems Map doesn't provide obj as PolyNode
			if (!(obj instanceof Area))
				return obj.equals(this);

			Area a = (Area)obj;
			return (super.equals(a));
		}
		
		/**
		 *
		 * @return
		 */
		public Point2D[] getPoints()
		{
			PathIterator pi = getPathIterator(null);
			double coords[] = new double[6];
			List pointList = new ArrayList();
            Point2D lastMoveTo = null;

			while (!pi.isDone()) {
				int type = pi.currentSegment(coords);
				switch (type) {
	                case PathIterator.SEG_CLOSE:
						// next available loop
						if (lastMoveTo != null)
							pointList.add(lastMoveTo);
						lastMoveTo = null;
						break;
					default:
						Point2D pt = new Point2D.Double(coords[0], coords[1]);
						pointList.add(pt);

						// Adding the point at the beginning of the loop
						if (type == PathIterator.SEG_MOVETO)
						{
							lastMoveTo = pt;
							//throw new UnsupportedOperationException("Case not supported");
						}
	            }
	            pi.next();
			}
			Point2D [] points = new Point2D[pointList.size()];
			return ((Point2D [])(pointList.toArray(points)));
		}

		public double getPerimeter()
		{
			double [] coords = new double[6];
			List pointList = new ArrayList();
			double perimeter = 0;
            PathIterator pi = getPathIterator(null);

			while (!pi.isDone()) {
				switch (pi.currentSegment(coords)) {
	                case PathIterator.SEG_CLOSE:
						{
							Object [] points = pointList.toArray();
							for (int i = 0; i < pointList.size(); i++)
							{
								int j = (i + 1)% pointList.size();
								perimeter += ((Point2D)points[i]).distance((Point2D)points[j]);
							}
							pointList.clear();
						}
						break;
					case PathIterator.SEG_MOVETO:

					default:
						Point2D pt = new Point2D.Double(coords[0], coords[1]);
						pointList.add(pt);
	            }
	            pi.next();
			}
			return(perimeter);
		}

		public boolean doesTouch(PathIterator opi)
		{
			// Adding first edges into hashMap

			class PolyEdge
			{
				private Point2D p1, p2;

				PolyEdge(Point2D a, Point2D b)
				{
					this.p1 = a;
					this.p2 = b;
				}
				public int hashCode()
				{
					return (p1.hashCode() ^ p2.hashCode());
				}
				public boolean equals(Object obj)
				{
					if (obj == this) return (true);
					if (!(obj instanceof PolyEdge)) return (false);
					PolyEdge edge = (PolyEdge)obj;

					return ((p1.equals(edge.p1) && p2.equals(edge.p2)) ||
					        (p1.equals(edge.p2) && p2.equals(edge.p1)));
				}
			}
			HashMap edges = new HashMap();
			PathIterator pi = getPathIterator(null);
			double [] coords = new double[6];
			List pointList = new ArrayList();

			while (!pi.isDone()) {
				switch (pi.currentSegment(coords)) {
	                case PathIterator.SEG_CLOSE:
						{
							Object [] points = pointList.toArray();
							for (int i = 0; i < pointList.size(); i++)
							{
								int j = (i + 1)% pointList.size();
								PolyEdge edge = new PolyEdge((Point2D)points[i], (Point2D)points[j]);
								edges.put(edge, edge);
							}
							pointList.clear();
						}
						break;
					default:
						Point2D pt = new Point2D.Double(coords[0], coords[1]);
						pointList.add(pt);
	            }
	            pi.next();
			}

			// Adding the other polygon
			while (!opi.isDone()) {
				switch (opi.currentSegment(coords)) {
	                case PathIterator.SEG_CLOSE:
						{
							Object [] points = pointList.toArray();
							for (int i = 0; i < pointList.size(); i++)
							{
								int j = (i + 1)% pointList.size();
								Point2D p1 = (Point2D)points[i];
								Point2D p2 = (Point2D)points[j];
								if (p1.equals(p2))
									continue;
								PolyEdge edge = new PolyEdge(p1, p2);

                                if (edges.containsKey(edge))
									return (true);
								edges.put(edge, edge);
							}
							pointList.clear();
						}
						break;
					default:
						Point2D pt = new Point2D.Double(coords[0], coords[1]);
						pointList.add(pt);
	            }
	            opi.next();
			}

			return (false);
		}

		/**
		 * Calculates area
		 * @return area associated to the node
		 */
		public double getArea()
		{
			if (isRectangular())
			{
				Rectangle2D bounds = getBounds2D();
				return (bounds.getHeight()*bounds.getWidth());
			}
			// @TODO: GVG Missing part. Run more robust tests
            double [] coords = new double[6];
			List pointList = new ArrayList();
			double area = 0;
            PathIterator pi = getPathIterator(null);

			while (!pi.isDone()) {
				switch (pi.currentSegment(coords)) {
	                case PathIterator.SEG_CLOSE:
						{
							Object [] points = pointList.toArray();
							for (int i = 0; i < pointList.size(); i++)
							{
								int j = (i + 1)% pointList.size();
								area += ((Point2D)points[i]).getX()*((Point2D)points[j]).getY();
								area -= ((Point2D)points[j]).getX()*((Point2D)points[i]).getY();
							}
							pointList.clear();
						}
						break;
					default:
						Point2D pt = new Point2D.Double(coords[0], coords[1]);
						pointList.add(pt);
	            }
	            pi.next();
			}
			area /= 2;
			return(area < 0 ? -area : area);
		}

		/**
		 * Returns a printable version of this PolyNode.
		 * @return a printable version of this PolyNode.
		 */
		public String toString()
		{
			return ("PolyNode " + getBounds());
		}

		/**
		 * Overwriting original for Area to consider touching polygons
		 * @param a
		 * @return
		 */
		public boolean intersects (Area a)
		{
			if (a.isRectangular())
			{
				boolean inter = intersects(a.getBounds2D());
				if (inter) return (inter);

				// detecting if they touch each other...
				inter = doesTouch(a.getBounds2D().getPathIterator(null));

				return (inter);
			}
			else if (isRectangular())
			{
				return (a.intersects(getBounds2D()));
			}
			// @TODO: GVG Missing part. Doesn't detect if elements are touching
			// @TODO: GVG very expensive?
			Area area = (Area)this.clone();
			area.intersect(a);
			boolean inter = !area.isEmpty();

			return (inter);
		}
		private boolean isOriginal()
		{
			boolean value = ((original >> 0 & 1) == 0);
			return ((original >> 0 & 1) == 0);
		}
		private void setNotOriginal()
		{
			original |= 1 << 0;
		}
		public Collection getSimpleObjects()
		{
			Set set = new HashSet();
			/*
			if (isRectangular())
			{
				set.add(this);
			}
			else
			*/
			{
				// Possible not connected loops
				double [] coords = new double[6];
				List pointList = new ArrayList();
                PathIterator pi = getPathIterator(null);

				while (!pi.isDone()) {
					switch (pi.currentSegment(coords)) {
						case PathIterator.SEG_CLOSE:
							{
								Object [] points = pointList.toArray();
                                GeneralPath simplepath = new GeneralPath();
								for (int i = 0; i < pointList.size(); i++)
								{
									int j = (i + 1)% pointList.size();
									Line2D line = new Line2D.Double(((Point2D)points[i]), (Point2D)points[j]);
									simplepath.append(line, true);
								}
								pointList.clear();
								PolyNode node = new PolyNode(simplepath);
								set.add(node);
							}
							break;
						default:
							Point2D pt = new Point2D.Double(coords[0], coords[1]);
							pointList.add(pt);
					}
					pi.next();
				}
			}
			return set;
		}
	}
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

		/**
		 * Calculates the bounding box of a child depending on the location. Parameters are passed to avoid
		 * extra calculation
		 * @param x Parent x value
		 * @param y Parent y value
		 * @param w Child width (1/4 of parent if qtree)
		 * @param h Child height (1/2 of parent if qtree)
		 * @param centerX Parent center x value
		 * @param centerY Parent center y value
		 * @param loc Location in qtree
		 * @return
		 */
		private Rectangle2D getBox(double x, double y, double w, double h, double centerX, double centerY, int loc)
		{
			if ((loc >> 0 & 1) == 1)
			{
				x = centerX;
			}
			if ((loc >> 1 & 1) == 1)
			{
				y = centerY;
			}
			return (new Rectangle2D.Double(x, y, w, h));
		}

		/**
		 * Collects recursive leaf elements in a list. Uses set to avoid
		 * duplicate elements (qtree could sort same element in all quadrants
		 * @param set
		 * @param modified True if no original elements should be considered
		 * @param simple True if simple elements should be retrieved
		 */
		protected void getLeafObjects(Set set, boolean modified, boolean simple)
		{
			if (nodes != null)
			{
				// Not sure how efficient this is
				for (Iterator it = nodes.iterator(); it.hasNext();)
				{
					PolyNode node = (PolyNode)it.next();
					if (!modified || (modified && !node.isOriginal()))
					{
						if (node.isOriginal() || !simple)
							set.add(node);
						else
							set.addAll(node.getSimpleObjects());
					}
				}
			}
			if (children == null) return;
			for (int i = 0; i < PolyQTree.MAX_NUM_CHILDREN; i++)
			{
				if (children[i] != null) children[i].getLeafObjects(set, modified, simple);
			}

		}
		/**
		 *   print function for debugging purposes
		 */
		protected void print()
		{
			if (nodes == null) return;
			for (Iterator it = nodes.iterator(); it.hasNext();)
				System.out.println("Area " + it.next());
			if (children == null) return;
			for (int i = 0; i < PolyQTree.MAX_NUM_CHILDREN; i++)
			{
				if (children[i] != null) children[i].print();
			}
		}

		/**
		 * To compact nodes if child elements have been removed
		 * @return true if node can be removed
		 */
		private boolean compact()
		{

			//System.out.println("To implement") ;

			//@TODO GVG Compact tree
			if (children != null)
			{
				for (int i = 0; i < PolyQTree.MAX_NUM_CHILDREN; i++)
					if (children[i] != null)
					{
						//System.out.println("To implement") ;
						return (false);
					}
			}
			return (nodes == null || nodes.isEmpty());
			//return (false);
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
		protected boolean findAndRemoveObjects(Rectangle2D box, PolyNode obj, Rectangle2D areaBB)
		{
			double centerX = box.getCenterX();
            double centerY = box.getCenterY();

            // Node has been split
			if (children != null)
			{
				int loc = getQuadrants(centerX, centerY, areaBB);
				double w = box.getWidth()/2;
				double h = box.getHeight()/2;
				double x = box.getX();
				double y = box.getY();

				for (int i = 0; i < PolyQTree.MAX_NUM_CHILDREN; i++)
				{
					if (((loc >> i) & 1) == 1)
					{
						Rectangle2D bb = getBox(x, y, w, h, centerX, centerY, i);

						// if identical element was found, no need of re-insertion
						// No need of reviewing other quadrants?
						if (children[i] == null) continue;

						if (children[i].findAndRemoveObjects(bb, obj, areaBB))
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
					PolyNode node = (PolyNode)it.next();

					if (node.equals((Object)obj))
						return (true);


					//if (node.intersects(obj))
					{
						obj.add(node);
						obj.setNotOriginal();
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
		 * To make sure new element is inserted in all childres
		 * @param box Bounding box of current node
		 * @param centerX To avoid calculation inside function from object box
		 * @param centerY To avoid calculation inside function from object box
		 * @param obj Object to insert
		 * @param areaBB Bounding box of the object to insert
		 * @return True if element was inserted
		 */
		protected boolean insertInAllChildren(Rectangle2D box, double centerX, double centerY, PolyNode obj, Rectangle2D areaBB)
		{
			int loc = getQuadrants(centerX, centerY, areaBB);
			boolean inserted = false;
			double w = box.getWidth()/2;
			double h = box.getHeight()/2;
			double x = box.getX();
			double y = box.getY();

			for (int i = 0; i < PolyQTree.MAX_NUM_CHILDREN; i++)
			{
				if (((loc >> i) & 1) == 1)
				{
					Rectangle2D bb = getBox(x, y, w, h, centerX, centerY, i);

					if (children[i] == null) children[i] = new PolyQNode();

					boolean done = children[i].insert(bb, obj, areaBB);

					inserted = (inserted) ? inserted : done;
				}
			}
			return (inserted);
		}
		/**
		 *
		 * @param box Bounding box of the current PolyQNode
		 * @param obj Object to insert
		 * @param areaBB Bounding box of object to insert
		 */
		protected boolean insert(Rectangle2D box, PolyNode obj, Rectangle2D areaBB)
		{
			if (!box.intersects(areaBB))
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
				return (insertInAllChildren(box, centerX, centerY, obj, areaBB));
			}
			if (nodes == null)
			{
				nodes = new HashSet();
			}
			boolean inserted = false;

			if (nodes.size() < PolyQTree.MAX_NUM_CHILDREN)
			{
				inserted = nodes.add(obj);
				//  nodes.add(obj.clone());
			}
			else
			{
				// subdivides into PolyQTree.MAX_NUM_CHILDREN. Might work only for 2^n
				children = new PolyQNode[PolyQTree.MAX_NUM_CHILDREN];
				double w = box.getWidth()/2;
				double h = box.getHeight()/2;
				double x = box.getX();
				double y = box.getY();

				// Redistributing existing elements in children
				for (int i = 0; i < PolyQTree.MAX_NUM_CHILDREN; i++)
				{
					children[i] = new PolyQNode();
					Rectangle2D bb = getBox(x, y, w, h, centerX, centerY, i);

					for (Iterator it = nodes.iterator(); it.hasNext();)
					{
						PolyNode node = (PolyNode)it.next();

						children[i].insert(bb, node, node.getBounds2D());
					}
				}
				nodes.clear(); // not sure about this clear yet
				nodes = null;
				inserted = insertInAllChildren(box, centerX, centerY, obj, areaBB);
			}
			return (inserted);
		}
	}
}
