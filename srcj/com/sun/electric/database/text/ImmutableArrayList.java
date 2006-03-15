/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ImmutableArrayList.java
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
package com.sun.electric.database.text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;

/**
 * Constant-array implementation of the <tt>List</tt> interface.
 */
public class ImmutableArrayList<E> extends ArrayList<E> {
    
    /**
     * Constructs a list containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator. The <tt>ImmutableArrayList</tt> instance has capacity of
     * the size of the specified collection.
     *
     * @param c the collection whose elements are to be placed into this list.
     * @throws NullPointerException if the specified collection is null.
     */
    public ImmutableArrayList(Collection<? extends E> c) {
        super(c.size());
        super.addAll(c);
        if (size() != c.size()) throw new ConcurrentModificationException();
    }
    
    /**
     * Constructs a list containing the elements of the specified array.
     * The <tt>ImmutableArrayList</tt> instance has capacity of
     * the length of the specified array.
     *
     * @param a the array whose elements are to be placed into this list.
     * @throws NullPointerException if the specified array is null.
     */
    public ImmutableArrayList(E[] a) {
        super(a.length);
        for (E e: a)
            super.add(e);
        assert size() == a.length;
    }
    
    /**
     * Constructs a list containing the range of elements of the specified array.
     * The <tt>ImutableArrayList</tt> instance has capacity of the range length.
     *
     * @param a the array whose elements are to be placed into this list.
     * @param fromIndex
     * @param toIndex
     * @throws NullPointerException if the specified array is null.
     */
    public ImmutableArrayList(E[] a, int fromIndex, int toIndex) {
        super(toIndex - fromIndex);
        if (fromIndex < 0)
            throw new IndexOutOfBoundsException("fromIndex = " + fromIndex);
        if (toIndex > a.length)
            throw new IndexOutOfBoundsException("toIndex = " + toIndex);
        for (int i = fromIndex; i < toIndex; i++)
            super.add(a[i]);
        assert size() == toIndex - fromIndex;
    }
    
    public ImmutableArrayList<E> with(E[] a) {
        if (a == null) return this;
        int l;
        for (l = a.length; l > 0 && a[l - 1] == null; l--);
        if (l == size()) {
            int i = 0;
            while (i < size() && a[i] == get(i)) i++;
            if (i == l) return this;
        }
        return new ImmutableArrayList<E>(a, 0, l);
    }
    
    public void trimToSize() {}
    public void ensureCapacity(int minCapacity) {}
    public E set(int index, E element) { throw new UnsupportedOperationException(); }
    public boolean add(E o) { throw new UnsupportedOperationException(); }
    public void add(int index, E element) { throw new UnsupportedOperationException(); }
    public E remove(int index) { throw new UnsupportedOperationException(); }
    public boolean remove(Object o) { throw new UnsupportedOperationException(); }
    public void clear() { throw new UnsupportedOperationException(); }
    public boolean addAll(Collection<? extends E> c) { throw new UnsupportedOperationException(); }
    public boolean addAll(int index, Collection<? extends E> c) { throw new UnsupportedOperationException(); }
    protected void removeRange(int fromIndex, int toIndex) { throw new UnsupportedOperationException(); }
}
