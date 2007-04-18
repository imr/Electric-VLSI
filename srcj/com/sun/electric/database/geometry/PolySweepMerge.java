/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PolySweepMerge.java
 * Written by Gilda Garreton, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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

import com.sun.electric.technology.Layer;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Area;
import java.awt.Shape;
import java.util.*;

/**
 * Class to implement geometric sweep algorithm in 2D for areas.
 */
public class PolySweepMerge extends GeometryHandler
{
//    public static final int ONE_FRONTIER_MODE = 1;
//    public static final int TWO_FRONTIER_MODE = 2;

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
        private List<Area> areas = null; // Needs to be a list to apply sort

        public PolySweepContainer(boolean createPolyList)
        {
            areas = (createPolyList) ? new ArrayList<Area>() : null;
        }

        public void add(Object value)
        {
            Area a = null;
            if (value instanceof Shape)
                a = new Area((Shape)value);
            else
                System.out.println("Error: invalid class for addition in PolySweepMerge");
            areas.add(a);
        }

        public void subtract(Object element)
        {
            Area elem = null;
            if (element instanceof Shape)
                elem = new Area((Shape)element);
            else
                System.out.println("Error: invalid class for subtraction in PolySweepMerge");
            for (Area a : areas)
            {
                a.subtract(elem);
            }
        }
    }

    // To insert new element into handler
	public void add(Layer key, Object element)
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
    public void subtractAll(HashMap<Layer,List<PolyBase>> map)
    {
       // Need to add exclusion before final calculation
        for (Map.Entry<Layer,List<PolyBase>> e : map.entrySet())
       {
           PolySweepContainer container = (PolySweepContainer)layers.get(e.getKey());
           if (container == null) continue;
            for (PolyBase p : e.getValue())
              container.subtract(p);
       }
    }

	// To add an entire GeometryHandler like collections
	public void addAll(GeometryHandler subMerge, AffineTransform tTrans)
    {
        PolySweepMerge other = (PolySweepMerge)subMerge;
        List<Area> list = new ArrayList<Area>();

        for (Map.Entry<Layer,Object> e : other.layers.entrySet())
		{
			Layer layer = e.getKey();
            PolySweepContainer container = (PolySweepContainer)layers.get(layer);
            PolySweepContainer otherContainer = (PolySweepContainer)e.getValue();

            // Nothing valid in top cell
            if (container == null)
            {
                // No need of creating polyListArray
                container = new PolySweepContainer(false);
                layers.put(layer, container);
                container.areas = new ArrayList<Area>(otherContainer.areas.size());
            }

            // Since I have to apply local transformation, I can't use the same array
            List<Area> otherAreas = new ArrayList<Area>(otherContainer.areas.size());
            for (Area area : otherContainer.areas)
            {
                otherAreas.add((Area)area.clone());
            }
            Collections.sort(otherAreas, areaSort);
            Collections.sort(container.areas, areaSort);

            for (Area area : otherAreas)
            {
                if (tTrans != null) area.transform(tTrans);
                Rectangle2D rect = area.getBounds2D();
                double areaMinX = rect.getMinX();
                double areaMaxX = rect.getMaxX();
//                boolean done = false;
                list.clear();
                
                // Search for all elements that might overlap
                for (Area thisArea : container.areas)
                {
                    Rectangle2D thisRect = thisArea.getBounds2D();
                    if (areaMaxX < thisRect.getMinX())
                    {
//                        done = true;
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
        for (Object obj : layers.values())
        {
            PolySweepContainer container = (PolySweepContainer)obj;
            if (container == null) continue;

            Collections.sort(container.areas, areaSort);
            double maxXSweep = -Double.MAX_VALUE;
            Area areaXTmp = null;
            List<Area> twoFrontierAreas = new ArrayList<Area>();
            List<Area> tmp = new ArrayList<Area>();

            for (Area geomArea : container.areas)
            {
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
        for (Object obj : layers.values())
        {
            PolySweepContainer container = (PolySweepContainer)obj;
            if (container == null) continue;

            Collections.sort(container.areas, areaSort);
            double maxXSweep = -Double.MAX_VALUE;
            Area areaXTmp = null;
            List<Area> areas = new ArrayList<Area>();

            for (Area geom : container.areas)
            {
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
                       //sweepYFrontier(twoFrontierAreas, tmp, true);
                       areaXTmp = null;
                    }
                }
                if (areaXTmp == null)
                {
                    // First one!
                    areaXTmp = geom;
                    areas.add(areaXTmp);
                }
                else
                {
                    areaXTmp.add(geom);
                }
                if (maxX > maxXSweep)
                    maxXSweep = maxX;
            }
            // Can't use HashSet due to Comparator
            if (areaXTmp != null && !areas.contains(areaXTmp))
                areas.add(areaXTmp);
            container.areas = areas;
            //sweepYFrontier(twoFrontierAreas, tmp, true);
        }
    }

    private static void sweepYFrontier(List<Area> twoFrontierAreas, List<Area> tmp, boolean merge)
    {
        // No fully tested yet
        if (merge) return;

        // Y frontier
        Collections.sort(tmp, areaSort);
        double maxYSweep = -Double.MAX_VALUE;
        Area areaYTmp = null;

        for (Area area : tmp)
        {
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
	public Collection<PolyBase> getObjects(Object layer, boolean modified, boolean simple)
    {
        PolySweepContainer container = (PolySweepContainer)layers.get(layer);

        if (container == null) return null;

        List<PolyBase> list = new ArrayList<PolyBase>();

        for (Area area : container.areas)
        {
            PolyBase.getPointsInArea(area, (Layer)layer, simple, false, list);
        }

        return list;
    }
}
