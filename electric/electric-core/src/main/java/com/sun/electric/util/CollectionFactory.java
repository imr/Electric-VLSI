/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ConcurrentCollectionFactory.java
 *
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.sun.electric.tool.util.concurrent.utils.ConcurrentCollectionFactory;
import com.sun.electric.util.collections.ImmutableList;

/**
 * This class provides factory methods for creating data structures. The
 * intension is that the generic generation of data structures <T> should be
 * hidden to make the code readable.
 * 
 * @author Felix Schmidt
 * 
 */
public class CollectionFactory extends ConcurrentCollectionFactory {

    private CollectionFactory() {

    }

    public static <T, K> HashMap<T, K> createHashMap() {
        return new HashMap<T, K>();
    }

    /**
     * 
     * @param <T>
     * @param source
     * @return
     */
    public static <T> Set<T> copySet(Set<T> source) {
        Set<T> result = CollectionFactory.createHashSet();

        doCopyCollection(source, result);

        return result;
    }

    public static <T> Set<T> copyListToSet(List<T> source) {
        Set<T> result = CollectionFactory.createHashSet();

        doCopyCollection(source, result);

        return result;
    }

    public static <T> List<T> copySetToList(Set<T> source) {
        List<T> result = CollectionFactory.createArrayList();

        doCopyCollection(source, result);

        return result;
    }

    public static <T> ImmutableList<T> copyListToImmutableList(List<T> source) {
        ImmutableList<T> immutableList = null;
        for (T element : source) {
            immutableList = ImmutableList.add(immutableList, element);
        }
        return immutableList;
    }

    public static <T> T[] arrayMerge(T[]... arrays) {

        Class objectClass = null;
        int count = 0;
        for(T[] array: arrays) {
            if(array != null) {
              count +=array.length;
              if(array.length > 0)
                  objectClass = array[0].getClass();
            }
        }
        if(objectClass == null)
            return null;
       List<T> mergedList = new ArrayList<T>();
       
       for(T[] array: arrays)
           if(array != null)
               mergedList.addAll(Arrays.asList(array));
       
       return mergedList.toArray((T[])Array.newInstance(objectClass, count));

    }
}
