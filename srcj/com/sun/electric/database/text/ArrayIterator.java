/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ArrayIterarator.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.text;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator over an array.
 */
public class ArrayIterator<E> implements Iterator<E> {
	private final E[] array;
    private final int limit;
	private int cursor;

	private ArrayIterator(E[] array) {
        this.array = array;
        limit = array.length;
        cursor = 0;
    }

	private ArrayIterator(E[] array, int start, int limit)
	{
		this.array = array;
        this.limit = limit;
        cursor = start;
	}

	/**
	 * Returns iterator over elements of array.
	 * @param array array with elements or null.
	 * @return iterator over elements of the array or NULL_ITERATOR.
	 */
	public static <E> Iterator<E> iterator(E[] array)
	{
		if (array != null && array.length > 0) return new ArrayIterator<E>(array);
        Iterator<E> emptyIterator = emptyIterator();
        return emptyIterator;
	}

	/**
	 * Returns iterator over range [start,limit) of elements of array.
	 * @param array array with elements or null.
     * @param start start index of the range.
     * @param limit limit of the range
	 * @return iterator over range of elements of the array or EMPTY_ITERATOR.
     * @throws IndexOutOfBoundsException if start or limit are not correct
	 */
	public static <E> Iterator<E> iterator(E[] array, int start, int limit)
	{
        if (array != null) {
            if (start >= 0 && limit <= array.length) {
                if (start < limit) {
                    return new ArrayIterator<E>(array, start, limit);
                } else if (start == limit) {
                    Iterator<E> emptyIterator = emptyIterator();
                    return emptyIterator;
                }
            }
        } else if (start == 0 && limit == 0) {
            Iterator<E> emptyIterator = emptyIterator();
            return emptyIterator;
        }
        throw new IndexOutOfBoundsException();
	}

	/**
	 * Returns <tt>true</tt> if the iteration has more elements. (In other
	 * words, returns <tt>true</tt> if <tt>next</tt> would return an element
	 * rather than throwing an exception.)
	 *
	 * @return <tt>true</tt> if the iterator has more elements.
	 */
	public boolean hasNext()
	{
		return cursor < limit;
	}

	/**
	 * Returns the next element in the iteration.  Calling this method
	 * repeatedly until the {@link #hasNext()} method returns false will
	 * return each element in the underlying collection exactly once.
	 *
	 * @return the next element in the iteration.
	 * @exception NoSuchElementException iteration has no more elements.
	 */
	public E next()
	{
        if (cursor >= limit)
			throw new NoSuchElementException();
		E next = array[cursor];
		cursor++;
		return next;
    }

	/**
	 * Removes from the underlying collection the last element returned by the
	 * iterator (unsupported operation).
	 *
	 * @exception UnsupportedOperationException 
	 */
    public void remove()
    {
		throw new UnsupportedOperationException();
    }
    
	/**
	 * Null iterator which has no elements.
	 */
    private static class EmptyIterator extends ArrayIterator<Object> {
        EmptyIterator() { super(new Object[0]); }
    }
    

    /**
     * The empty iterator (immutable).
     *
     * @see #emptyIterator()
     */
	public static final EmptyIterator EMPTY_ITERATOR = new EmptyIterator();
    
    /**
     * Returns the empty iterator (immutable).
     * Unlike the like-named field, this method is parameterized.
     *
     * <p>This example illustrates the type-safe way to obtain an empty set:
     * <pre>
     *     Iterator&lt;String&gt; s = ArrayIterator.emptyIterator();
     * </pre>
     * Implementation note:  Implementations of this method need not
     * create a separate <tt>Iterator</tt> object for each call.   Using this
     * method is likely to have comparable cost to using the like-named
     * field.  (Unlike this method, the field does not provide type safety.)
     *
     * @see #EMPTY_ITERATOR
     */
    public static final <E> Iterator<E> emptyIterator() {
        return (Iterator<E>) EMPTY_ITERATOR;
    }
}
