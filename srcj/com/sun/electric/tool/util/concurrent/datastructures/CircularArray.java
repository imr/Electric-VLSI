/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CircularArray.java
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
package com.sun.electric.tool.util.concurrent.datastructures;

import java.lang.reflect.Array;

import com.sun.electric.tool.util.IStructure;

/**
 * Circular Array - thread safe - resizable
 * 
 * @author Felix Schmidt
 * 
 */
public class CircularArray<T> extends IStructure<T> {

	private int logCapacity;
	private T[] currentElements;

	/**
	 * @param clazz
	 *            this parameter defines the class type of the objects that
	 *            should be stored in the array. Java could not create arrays
	 *            from generic type parameter, but it is possible to create a
	 *            array from the class object. So this class object is used to
	 *            create dynamic arrays.
	 * @param logCapacity
	 *            capacity of the initial array
	 */
	@SuppressWarnings("unchecked")
	public CircularArray(int logCapacity) {
		this.logCapacity = logCapacity;
		this.currentElements = (T[]) new Object[1 << logCapacity];
	}

	public int getCapacity() {
		return 1 << logCapacity;
	}

	public T get(int i) {
		return currentElements[i % getCapacity()];
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sun.electric.tool.util.IStructure#add(java.lang.Object)
	 */
	@Override
	@Deprecated
	public void add(T item) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void add(T item, int i) {
		System.out.println(i + ": " + getCapacity());
		currentElements[i % getCapacity()] = item;
	}

	public CircularArray<T> resize(int bottom, int top) {
		CircularArray<T> newArray = new CircularArray<T>(logCapacity + 1);
		for (int i = top; i < bottom; i++) {
			newArray.add(get(i), i);
		}
		return newArray;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sun.electric.tool.util.IStructure#isEmpty()
	 */
	@Override
	@Deprecated
	public boolean isEmpty() {
		throw new UnsupportedOperationException();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sun.electric.tool.util.IStructure#remove()
	 */
	@Override
	@Deprecated
	public T remove() {
		throw new UnsupportedOperationException();
	}

}
