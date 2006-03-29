/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GeometryHandler.java
 * Written by Gilda Garreton, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
import java.awt.geom.Area;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * To handle merge operation. Two different classes have been proposed
 * and this interface would handle the implementation
 * @author  Gilda Garreton
 */
public abstract class GeometryHandler {
    HashMap<Layer,Object> layers;
    public enum GHMode // GH GeometryHandler mode
    {
	    ALGO_MERGE,   // using merge structure
	    ALGO_SWEEP; // using sweep structure
    }
    public static final ShapeSort shapeSort = new ShapeSort();
    public static final AreaSort areaSort = new AreaSort();

    /**
     * Method to create appropiate GeometryHandler depending on the mode.
     */
    public static GeometryHandler createGeometryHandler(GHMode mode, int initialSize)
    {
        switch (mode)
        {
            case ALGO_MERGE:
                return new PolyMerge();
            case ALGO_SWEEP:
                if (initialSize > 0)
                    return new PolySweepMerge(initialSize);
                else
                    return new PolySweepMerge();
        }
        return null;
    }

    public GeometryHandler()
    {
        layers = new HashMap<Layer,Object>();
    }

    /**
     * Special constructor in case of using huge amount of memory. E.g. ERC
     * @param initialSize
     */
    public GeometryHandler(int initialSize)
    {
        layers = new HashMap<Layer,Object>(initialSize);
    }

    // To insert new element into handler
	public void add(Layer key, Object value) {;}

	// To add an entire GeometryHandler like collections
	public void addAll(GeometryHandler subMerge, AffineTransform tTrans) {;}

    /**
	 * Method to subtract a geometrical object from the merged collection.
	 * @param key the key that this Object sits on.
	 * @param element the Object to merge.
	 */
    public void subtract(Object key, Object element)
    {
        System.out.println("Error: subtract not implemented for GeometryHandler subclass " + this.getClass().getName());
    }

    /**
     * Method to subtract all geometries stored in hash map from corresponding layers
     * @param map
     */
    public void subtractAll(HashMap<Layer,List<PolyBase>> map)
    {
        System.out.println("Error: subtractAll not implemented for GeometryHandler subclass " + this.getClass().getName());
    }

	/**
	 * Access to keySet to create a collection for example.
	 */
	public Set<Layer> getKeySet()
	{
		return (layers.keySet());
	}

	/**
	 * Access to keySet with iterator
	 * @return iterator for keys in hashmap
//	 */
//	public Iterator<Layer> getKeyIterator()
//	{
//		return (getKeySet().iterator());
//	}

	/**
	 * Iterator among all layers inserted.
	 * @return an iterator over all layers inserted.
	 */
//	public Iterator getIterator()
//	{
//		return (layers.values().iterator());
//	}

	/**
	 * To retrieve leave elements from internal structure
	 * @param layer current layer under analysis
	 * @param modified to avoid retrieving original polygons
	 * @param simple to obtain simple polygons
	 */
	public Collection<PolyBase> getObjects(Object layer, boolean modified, boolean simple)
    {
        System.out.println("Error: getObjects not implemented for GeometryHandler subclass " + this.getClass().getName());
        return null;
    }

    /**
     * Method to perform operations after no more elemenets will
     * be added. Valid for PolySweepMerge
     * @param merge true if polygons must be merged otherwise non-overlapping polygons will be generated.
     */
    public void postProcess(boolean merge)
    {
        if (!merge) System.out.println("Error: postProcess not implemented for GeometryHandler subclass " + this.getClass().getName());
    }

    /**
     * Auxiliar class to sort shapes in array based on X position
     */
	private static class ShapeSort implements Comparator<Shape>
    {
		public int compare(Shape s1, Shape s2)
        {
			double bb1 = s1.getBounds2D().getX();
			double bb2 = s2.getBounds2D().getX();
            // Sorting along X
            if (bb1 < bb2) return -1;
            else if (bb1 > bb2) return 1;
            return (0); // identical
        }
    }

    /**
     * Auxiliar class to sort areas in array based on X position
     */
    private static class AreaSort implements Comparator<Area>
    {
    	public int compare(Area a1, Area a2)
        {
    		double bb1 = a1.getBounds2D().getX();
    		double bb2 = a2.getBounds2D().getX();
            // Sorting along X
            if (bb1 < bb2) return -1;
            else if (bb1 > bb2) return 1;
            return (0); // identical
        }
    }
}
