package com.sun.electric.database.geometry;

import com.sun.electric.technology.Layer;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Area;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Gilda
 * Date: Jan 14, 2005
 * Time: 4:39:34 PM
 * To change this template use File | Settings | File Templates.
 */
public class PolySweepMerge implements GeometryHandler
{
	private HashMap layers = new HashMap(); // should be more efficient here
    static final PolySweepSort polySweetSort = new PolySweepSort();

    /**
     * Auxiliar class to sort geometries in array
     */
    public static class PolySweepSort implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            double bb1 = ((Shape)o1).getBounds2D().getX();
            double bb2 = ((Shape)o2).getBounds2D().getX();
            // Sorting along X
            if (bb1 < bb2) return -1;
            else if (bb1 > bb2) return 1;
            return (0); // identical
        }
    }

	/**
	 * Method to create a new "merge" object.
	 */
	public PolySweepMerge()
	{
	}

    private static class PolySweepContainer
    {
        List polyList = new ArrayList();
        Set areas = null;

        public void add(Object value)
        {
            polyList.add(value);
        }
    }

    // To insert new element into handler
	public void add(Object key, Object value, boolean fasterAlgorithm)
    {
        Layer layer = (Layer)key;
        PolySweepContainer container = (PolySweepContainer)layers.get(layer);

        if (container == null)
        {
            container = new PolySweepContainer();
            layers.put(layer, container);
        }
        container.add(value);
    }

	// To add an entire GeometryHandler like collections
	public void addAll(GeometryHandler subMerge, AffineTransform tTrans)
    {
        System.out.println("addAll not implementd in PolySweepMerge");
    }

	/**
	 * Access to keySet to create a collection.
	 */
	public Collection getKeySet()
	{
		return (layers.keySet());
	}

	/**
	 * Access to keySet with iterator
	 * @return iterator for keys in hashmap
	 */
	public Iterator getKeyIterator()
	{
		return (getKeySet().iterator());
	}

    public void postProcess()
    {
        for (Iterator it = getKeyIterator(); it.hasNext();)
        {
            Layer layer = (Layer)it.next();
            PolySweepContainer container = (PolySweepContainer)layers.get(layer);
            if (container != null)
            {
                Collections.sort(container.polyList, polySweetSort);
                double minSweep = -Double.MAX_VALUE;
                double maxSweep = -Double.MAX_VALUE;
                Area areaTmp = null;
                container.areas = new HashSet();

                for (int i = 0; i < container.polyList.size(); i++)
                {
                    Shape geom = (Shape)container.polyList.get(i);
                    Rectangle2D rect = geom.getBounds2D();
                    double x = rect.getX();
                    double y = rect.getMaxX();
                    if (x > maxSweep)
                    {
                       // Previous area is 100% disconnected
                       if (areaTmp != null)
                       {
                           container.areas.add(areaTmp);
                           areaTmp = null;
                       }
                    }
                    if (areaTmp == null)
                    {
                        areaTmp = new Area(geom);
                        container.areas.add(areaTmp);
                    }
                    else
                        areaTmp.add(new Area(geom));
                    if (y > maxSweep)
                        maxSweep = y;
                }
                container.areas.add(areaTmp);
            }
        }
        System.out.println("postProcess not implementd in PolySweepMerge");
    }

	/**
	 * To retrieve leave elements from internal structure
	 * @param layer current layer under analysis
	 * @param modified to avoid retrieving original polygons
	 * @param simple to obtain simple polygons
	 */
	public Collection getObjects(Object layer, boolean modified, boolean simple)
    {
        System.out.println("getObjects not implementd in PolySweepMerge");
        return null;
    }
}
