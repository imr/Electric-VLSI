package com.sun.electric.database.geometry;

import com.sun.electric.technology.Layer;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Area;
import java.awt.Shape;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Comparator;

/**
 * Class to implement geometric sweep algorithm in 2D for areas.
 * @author  Gilda Garreton
 * To change this template use File | Settings | File Templates.
 */
public class PolySweepMerge extends GeometryHandler
{
    static final PolySweepShapeSort shapeSort = new PolySweepShapeSort();
    static final PolySweepAreaSort areaSort = new PolySweepAreaSort();

    /**
     * Auxiliar class to sort shapes in array
     */
    private static class PolySweepShapeSort implements Comparator
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
     * Auxiliar class to sort areas in array
     */
    private static class PolySweepAreaSort implements Comparator
    {
        public int compare(Object o1, Object o2)
        {
            double bb1 = ((Area)o1).getBounds2D().getX();
            double bb2 = ((Area)o2).getBounds2D().getX();
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


	/**
	 * Method to create a new "merge" object.
	 */
	public PolySweepMerge(int initialSize)
	{
        super(initialSize);
	}

    private static class PolySweepContainer
    {
        List polyList = null;
        List areas = null; // Needs to be a list to apply sort

        public PolySweepContainer(boolean createPolyList)
        {
            polyList = (createPolyList) ? new ArrayList() : null;
        }
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
            container = new PolySweepContainer(true);
            layers.put(layer, container);
        }
        container.add(value);
    }

	// To add an entire GeometryHandler like collections
	public void addAll(GeometryHandler subMerge, AffineTransform tTrans)
    {
        PolySweepMerge other = (PolySweepMerge)subMerge;
        List list = new ArrayList();

		for(Iterator it = other.layers.keySet().iterator(); it.hasNext();)
		{
			Object layer = it.next();
            PolySweepContainer container = (PolySweepContainer)layers.get(layer);
            PolySweepContainer otherContainer = (PolySweepContainer)other.layers.get(layer);

            // Nothing valid in top cell
            if (container == null)
            {
                // No need of creating polyListArray
                container = new PolySweepContainer(false);
                layers.put(layer, container);
                container.areas = new ArrayList(otherContainer.areas.size());
            }

            // Since I have to apply local transformation, I can't use the same array
            List otherAreas = new ArrayList(otherContainer.areas.size());
            for (int i = 0; i < otherContainer.areas.size(); i++)
            {
                Area area = (Area)otherContainer.areas.get(i);
                otherAreas.add(area.clone());
            }
            Collections.sort(otherAreas, areaSort);
            Collections.sort(container.areas, areaSort);

            for (int i = 0; i < otherAreas.size(); i++)
            {
                Area area = (Area)otherAreas.get(i);
                area.transform(tTrans);
                Rectangle2D rect = area.getBounds2D();
                double areaMinX = rect.getMinX();
                double areaMaxX = rect.getMaxX();
                boolean done = false;
                list.clear();
                
                // Search for all elements that might overlap
                for (int j = 0; j < container.areas.size() && !done; j++)
                {
                    Area thisArea = (Area)container.areas.get(j);
                    Rectangle2D thisRect = thisArea.getBounds2D();
                    if (areaMaxX < thisRect.getMinX())
                    {
                        done = true;
                        break;
                    }
                    // They could collide
                    if (areaMinX <= thisRect.getMaxX())
                    {
                        list.add(thisArea);
                        area.add(thisArea);
                    }
                }
                // Remove elements with collisions
                container.areas.removeAll(list);
                container.areas.add(area);
            }
            otherAreas = null;
        }
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
            if (container == null) continue;

            Collections.sort(container.polyList, shapeSort);
            double maxSweep = -Double.MAX_VALUE;
            Area areaTmp = null;
            container.areas = new ArrayList();

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
                       if (!container.areas.contains(areaTmp))
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
            // Can't use HashSet due to Comparator
            if (areaTmp != null && !container.areas.contains(areaTmp))
                container.areas.add(areaTmp);
            container.polyList = null;
        }
    }

	/**
	 * To retrieve leave elements from internal structure
	 * @param layer current layer under analysis
	 * @param modified to avoid retrieving original polygons
	 * @param simple to obtain simple polygons
	 */
	public Collection getObjects(Object layer, boolean modified, boolean simple)
    {
        PolySweepContainer container = (PolySweepContainer)layers.get(layer);

        if (container == null) return null;

        List list = new ArrayList();
        for (Iterator it = container.areas.iterator(); it.hasNext(); )
        {
            Area area = (Area)it.next();
            PolyBase.getPointsInArea(area, (Layer)layer, simple, false, list);
        }

        return list;
    }
}
