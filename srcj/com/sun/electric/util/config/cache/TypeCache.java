/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TypeCache.java
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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
package com.sun.electric.util.config.cache;

import java.util.Map;
import java.util.Set;

import com.sun.electric.util.CollectionFactory;

/**
 * @author Felix Schmidt
 * 
 */
public class TypeCache {

    private static TypeCache instance = new TypeCache();

    private TypeCache() {
        this.createTypeHierarchy(Double.class);
        this.createTypeHierarchy(Integer.class);
        this.createTypeHierarchy(String.class);
        this.createTypeHierarchy(Boolean.class);
    }

    public static TypeCache getInstance() {
        return instance;
    }

    private Map<Class<?>, Set<Class<?>>> cache = CollectionFactory.createHashMap();

    public Set<Class<?>> get(Class<?> clazz) {
        return cache.get(clazz);
    }

    public void put(Class<?> clazz, Set<Class<?>> set) {
        cache.put(clazz, set);
    }

    public void newEntry(Class<?> clazz) {
        Set<Class<?>> set = CollectionFactory.createHashSet();
        this.put(clazz, set);
    }

    public boolean contains(Class<?> clazz) {
        return this.cache.containsKey(clazz);
    }

    public boolean contains(Class<?> key, Class<?> value) {
        if (this.contains(key)) {
            return this.get(key).contains(value);
        } else {
            return false;
        }
    }

    public void addEntry(Class<?> clazz, Class<?> entry) {
        Set<Class<?>> set = this.get(clazz);
        if (set == null) {
            this.newEntry(clazz);
            set = this.get(clazz);
        }

        set.add(entry);
        this.put(clazz, set);
    }

    public void createTypeHierarchy(Class<?> clazz) {
        if (this.contains(clazz)) {
            return;
        }

        this.walkThroughTypeHierarchy(clazz, clazz);
        return;
    }

    protected void walkThroughTypeHierarchy(Class<?> clazz, Class<?> father) {
        if (clazz == null)
            return;

        if (this.contains(clazz) && !clazz.equals(father)) {
            Set<Class<?>> path = this.get(clazz);
            for (Class<?> p : path) {
                this.addEntry(father, p);
            }
        } else {

            this.addEntry(father, clazz);

            for (Class<?> interfaces : clazz.getInterfaces()) {
                this.walkThroughTypeHierarchy(interfaces, father);
            }

            this.walkThroughTypeHierarchy(clazz.getSuperclass(), father);
        }
    }

}
