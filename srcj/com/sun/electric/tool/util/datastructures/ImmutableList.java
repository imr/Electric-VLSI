/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ImmutableList.java
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
package com.sun.electric.tool.util.datastructures;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author Felix Schmidt
 * 
 */
public class ImmutableList<T> implements Iterable<T> {

	private ImmutableList<T> next;
	private T item;

	private ImmutableList(ImmutableList<T> next, T item) {
		this.next = next;
		this.item = item;
	}

	public static <T> ImmutableList<T> add(ImmutableList<T> list, T item) {
		return new ImmutableList<T>(list, item);
	}

	public static <T> ImmutableList<T> remove(ImmutableList<T> list, T item) {
		if (list == null)
			return null;
		return list.remove(item);
	}

	private ImmutableList<T> remove(T target) {
		if (this.item == target) {
			return this.next;
		} else {
			ImmutableList<T> new_next = remove(this.next, target);
			if (new_next == this.next)
				return this;
			return new ImmutableList<T>(new_next, item);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Iterable#iterator()
	 */
	public Iterator<T> iterator() {
		return new ImmutableListIterator<T>(this);
	}

	public static class ImmutableListIterator<T> implements Iterator<T> {

		private ImmutableList<T> list;

		public ImmutableListIterator(ImmutableList<T> list) {
			this.list = list;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Iterator#hasNext()
		 */
		public boolean hasNext() {
			return list != null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Iterator#next()
		 */
		public T next() {
			if (list != null) {
				T obj = list.item;
				list = list.next;
				return obj;
			}
			throw new NoSuchElementException();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Iterator#remove()
		 */
		public void remove() {
			throw new UnsupportedOperationException();
		}

	}

}
