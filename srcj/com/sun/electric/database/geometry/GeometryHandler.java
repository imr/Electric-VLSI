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

import java.awt.geom.AffineTransform;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashMap;

/**
 * To handle merge operation. Two different classes have been proposed
 * and this interface would handle the implementation
 * @author  Gilda Garreton
 */
public abstract class GeometryHandler {
    HashMap layers;
    public static final int ALGO_MERGE = 0;
    public static final int ALGO_QTREE = 1;
    public static final int ALGO_SWEEP = 2;

    public GeometryHandler()
    {
        layers = new HashMap();
    }

    /**
     * Special constructor in case of using huge amount of memory. E.g. ERC
     * @param initialSize
     */
    public GeometryHandler(int initialSize)
    {
        layers = new HashMap(initialSize);
    }

    // To insert new element into handler
	public void add(Object key, Object value, boolean fasterAlgorithm) {;}

	// To add an entire GeometryHandler like collections
	public void addAll(GeometryHandler subMerge, AffineTransform tTrans) {;}

	/**
	 * Access to keySet to create a collection for example.
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

	/**
	 * Iterator among all layers inserted.
	 * @return an iterator over all layers inserted.
	 */
	public Iterator getIterator()
	{
		return (layers.values().iterator());
	}

	/**
	 * To retrieve leave elements from internal structure
	 * @param layer current layer under analysis
	 * @param modified to avoid retrieving original polygons
	 * @param simple to obtain simple polygons
	 */
	public Collection getObjects(Object layer, boolean modified, boolean simple)
    {
        return null;
    }
}
