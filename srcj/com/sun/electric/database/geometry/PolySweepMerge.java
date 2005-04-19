package com.sun.electric.database.geometry;

import com.sun.electric.technology.Layer;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Area;
import java.awt.Shape;
import java.util.*;

/**
 * Class to implement geometric sweep algorithm in 2D for areas.
 * @author  Gilda Garreton
 * To change this template use File | Settings | File Templates.
 */
public class PolySweepMerge extends GeometryHandler
{
    public static final int ONE_FRONTIER_MODE = 1;
    public static final int TWO_FRONTIER_MODE = 2;

    //private int mode = ONE_FRONTIER_MODE;

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

    /**
     * Method to switch between a sweep algorithm with one
     * or two frontiers
     * @param mode
     */
    public void setMode(int mode)
    {
        //just to  get rest of
        //this.mode = mode;
    }

    private static class PolySweepContainer
    {
        private List areas = null; // Needs to be a list to apply sort

        public PolySweepContainer(boolean createPolyList)
        {
            areas = (createPolyList) ? new ArrayList() : null;
        }

        public void add(Object value)
        {
            if (value instanceof Shape)
                value = new Area((Shape)value);
            else
                System.out.println("Error: invalid class for addition in PolySweepMerge");
            areas.add(value);
        }

        public void subtract(Object element)
        {
            Area elem = null;
            if (element instanceof Shape)
                elem = new Area((Shape)element);
            else
                System.out.println("Error: invalid class for subtraction in PolySweepMerge");
            for (int i = 0; i < areas.size(); i++)
            {
                Area a = (Area)areas.get(i);
                a.subtract(elem);
            }
        }
    }

    // To insert new element into handler
	public void add(Object key, Object element, boolean fasterAlgorithm)
    {
        PolySweepContainer container = (PolySweepContainer)layers.get(key);

        if (container == null)
        {
            container = new PolySweepContainer(true);
            layers.put(key, container);
        }
        container.add(element);
    }

    /**
	 * Method to subtract a geometrical object from the merged collection.
	 * @param key the key that this Object sits on.
	 * @param element the Object to merge.
	 */
    public void subtract(Object key, Object element)
    {
        PolySweepContainer container = (PolySweepContainer)layers.get(key);

        if (container == null) return;
        container.subtract(element);
    }

    /**
     * Method to subtract all geometries stored in hash map from corresponding layers
     * @param map
     */
    public void subtractAll(HashMap map)
    {
       // Need to add exclusion before final calculation
       for (Iterator it = map.keySet().iterator(); it.hasNext();)
       {
           Object key = it.next();
           List list = (List)map.get(key);
           PolySweepContainer container = (PolySweepContainer)layers.get(key);
           if (container == null) continue;
           for (int i = 0; i < list.size(); i++)
              container.subtract(list.get(i));
       }
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
                if (tTrans != null) area.transform(tTrans);
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

    public void postProcess(boolean merge)
    {
        if (merge) mergeProcess();
        else disjointProcess();
    }

    /**
     * Method to generate a set of disjoint polygons from original set
     */
    private void disjointProcess()
    {
        for (Iterator it = getKeyIterator(); it.hasNext();)
        {
            Layer layer = (Layer)it.next();
            PolySweepContainer container = (PolySweepContainer)layers.get(layer);
            if (container == null) continue;

            //Collections.sort(container.polyList, shapeSort);
            Collections.sort(container.areas, areaSort);
            double maxXSweep = -Double.MAX_VALUE;
            Area areaXTmp = null;
            List twoFrontierAreas = new ArrayList();
            List tmp = new ArrayList();

            for (int i = 0; i < container.areas.size(); i++)
            {
                Area geomArea = (Area)container.areas.get(i);
                Rectangle2D rectX = geomArea.getBounds2D();
                double minX = rectX.getX();
                double maxX = rectX.getMaxX();

                if (minX > maxXSweep)
                {
                    // Previous area is 100% disconnected
                    if (areaXTmp != null)
                    {
                       sweepYFrontier(twoFrontierAreas, tmp, false);
                       areaXTmp = null;
                    }
                    tmp.clear();
                }
                tmp.add(geomArea);
                if (areaXTmp == null)
                {
                    // First one!
                    areaXTmp = geomArea;
                }
                if (maxX > maxXSweep)
                    maxXSweep = maxX;
            }
            sweepYFrontier(twoFrontierAreas, tmp, false);
            container.areas = twoFrontierAreas;
            //container.polyList = null;
        }
    }

    private void mergeProcess()
    {
        for (Iterator it = getKeyIterator(); it.hasNext();)
        {
            Layer layer = (Layer)it.next();
            PolySweepContainer container = (PolySweepContainer)layers.get(layer);
            if (container == null) continue;

            //Collections.sort(container.polyList, shapeSort);
            Collections.sort(container.areas, areaSort);
            double maxXSweep = -Double.MAX_VALUE;
            Area areaXTmp = null;
            List areas = new ArrayList();
            //List twoFrontierAreas = new ArrayList();
            //List tmp = new ArrayList();

            for (int i = 0; i < container.areas.size(); i++)
            {
                Area geom = (Area)container.areas.get(i);
                Rectangle2D rectX = geom.getBounds2D();
                double minX = rectX.getX();
                double maxX = rectX.getMaxX();
                if (minX > maxXSweep)
                {
                    // Previous area is 100% disconnected
                    if (areaXTmp != null)
                    {
                       // Note: this comparison is not meant to call Area.equals!
                       if (!areas.contains(areaXTmp))
                           areas.add(areaXTmp);
//                        if (!container.areas.contains(areaXTmp))
//                           container.areas.add(areaXTmp);
                       //sweepYFrontier(twoFrontierAreas, tmp, true);
                       areaXTmp = null;
                    }
                    //tmp.clear();
                }
                if (areaXTmp == null)
                {
                    // First one!
                    areaXTmp = geom; //new Area(geom);
                    areas.add(areaXTmp);
                    //container.areas.add(areaXTmp);
                    //tmp.add(areaXTmp);
                }
                else
                {
                    //Area geomArea = new Area(geom);
                    areaXTmp.add(geom); //geomArea);
                    //tmp.add(geomArea);
                }
                if (maxX > maxXSweep)
                    maxXSweep = maxX;
            }
            // Can't use HashSet due to Comparator
            if (areaXTmp != null && !areas.contains(areaXTmp))
                areas.add(areaXTmp);
            container.areas = areas;
//            if (areaXTmp != null && !container.areas.contains(areaXTmp))
//                container.areas.add(areaXTmp);
            //sweepYFrontier(twoFrontierAreas, tmp, true);

//            if (Main.LOCALDEBUGFLAG)
//            {
//                // Testing if we get same amount of single polygons
//                Collection set1 = getObjects(layer, false, true);
//                // Another tmp list
//                List set2 = new ArrayList();
//                for (Iterator iter = twoFrontierAreas.iterator(); iter.hasNext(); )
//                {
//                    Area area = (Area)iter.next();
//                    PolyBase.getPointsInArea(area, (Layer)layer, true, false, set2);
//                }
//                if (set2.size() != set1.size())
//                    System.out.println("Wrong calculation");
//            }
//            if (mode == TWO_FRONTIER_MODE)
//            {
//                if (container.areas.size() != twoFrontierAreas.size())
//                     System.out.println("differrent set");
//                 container.areas = twoFrontierAreas;
//            }
            //container.polyList = null;
        }
    }

    private static void sweepYFrontier(List twoFrontierAreas, List tmp, boolean merge)
    {
        // No fully tested yet
        if (merge) return;

        // Y frontier
        Collections.sort(tmp, areaSort);
        double maxYSweep = -Double.MAX_VALUE;
        Area areaYTmp = null;
        for (int j = 0; j < tmp.size(); j++)
        {
           Area area = (Area)tmp.get(j);
           Rectangle2D rectY = area.getBounds2D();
           double minY = rectY.getY();
           double maxY = rectY.getMaxY();

           // Done with this piece of geometry
           if (minY > maxYSweep)
           {
               if (areaYTmp != null)
               {
                   if (!twoFrontierAreas.contains(areaYTmp))
                       twoFrontierAreas.add(areaYTmp);
               }
               areaYTmp = null;
           }
            if (areaYTmp == null)
            {
                // first one on Y
                // In case no merge, areaYTmp will keep the entire region merged
                // for substraction purposes
                if (!merge)
                {
                    areaYTmp = (Area)area.clone();
                    twoFrontierAreas.add(area);
                }
                else
                {
                    areaYTmp = area;
                    twoFrontierAreas.add(areaYTmp);
                }
            }
            else
            {
                if (merge)
                    areaYTmp.add(area);
                else
                {
                    Area clone = (Area)area.clone();
                    clone.intersect(areaYTmp);
                    if (!clone.isEmpty())
                        area.subtract(areaYTmp);
                    if (!area.isEmpty())
                    {
                        twoFrontierAreas.add(area);
                        areaYTmp.add(area);
                    }
                }
            }

            if (maxY > maxYSweep)
                maxYSweep = maxY;
       }
        if (areaYTmp != null && merge && !twoFrontierAreas.contains(areaYTmp))
            twoFrontierAreas.add(areaYTmp);
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
