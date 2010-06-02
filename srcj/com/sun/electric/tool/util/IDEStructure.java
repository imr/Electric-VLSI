/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IDEStructure.java
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
package com.sun.electric.tool.util;

/**
 * 
 * Base class for double ended data structures. Common queues allows to put
 * objects at one end and retrieve objects from the other end. Double ended data
 * structures provide a interface to put objects on one side of the data
 * structure, but retrieving is possible on both sides. This is helpful for work
 * stealing algorithms.
 * 
 * @param <T> type of elements to be stored
 */
public abstract class IDEStructure<T> extends IStructure<T> {

    /**
     * retreive a element
     * @return a element of type T
     */
	public abstract T getFromTop();

    /**
     * true, if the data structure is full. This is important for bounded datastructures
     * @return
     */
	public abstract boolean isFull();

    /**
     * try to add element of type T. If it is not possible to add the element, return false;
     * otherwise return true.
     */
	public abstract boolean tryAdd(T item);
}
