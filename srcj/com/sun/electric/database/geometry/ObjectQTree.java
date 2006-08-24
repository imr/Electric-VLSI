package com.sun.electric.database.geometry;

import java.util.Set;
import java.util.HashSet;
import java.awt.geom.Rectangle2D;

/**
 * Created by IntelliJ IDEA.
 * User: Gilda
 * Date: Feb 14, 2006
 * Time: 10:39:15 PM
 * To change this template use File | Settings | File Templates.
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

        ObjectNode(Object e, Rectangle2D r)
        {
            this.elem = e;
            this.rect = r;
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
                        if (searchBox.intersects(node.rect))
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
			if (!box.intersects(obj.getBounds()))
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
			boolean inserted = false;

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
