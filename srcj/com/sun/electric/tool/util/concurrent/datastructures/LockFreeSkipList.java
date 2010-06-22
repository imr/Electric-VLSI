/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LockFreeSkipList.java
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

import java.util.concurrent.atomic.AtomicMarkableReference;

import com.sun.electric.tool.util.IStructure;
import com.sun.electric.tool.util.concurrent.runtime.MultiThreadedRandomizer;

/**
 * @author Felix Schmidt
 * 
 */
@Deprecated
public class LockFreeSkipList<T> extends IStructure<T> {

	private static final int MAX_LEVEL = 4;
	private MultiThreadedRandomizer randomizer;
	private final Node<T> head = new Node<T>(Integer.MAX_VALUE);
	@SuppressWarnings("unused")
	private final Node<T> tail = new Node<T>(Integer.MAX_VALUE);

	public LockFreeSkipList() {
		randomizer = new MultiThreadedRandomizer(0);

		for (int i = 0; i < head.next.length; i++) {
			head.next[i] = new AtomicMarkableReference<Node<T>>(null, false);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sun.electric.tool.util.IStructure#add(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void add(T item) {
		int topLevel = randomLevel();
		int bottomLevel = 0;
		Node<T>[] preds = (Node<T>[]) new Node[MAX_LEVEL + 1];
		Node<T>[] succs = (Node<T>[]) new Node[MAX_LEVEL + 1];

		while (true) {
			boolean found = find(item, preds, succs);
			if (!found) {
				Node<T> newNode = new Node<T>(item, topLevel);
				for (int level = bottomLevel; level <= topLevel; level++) {
					Node<T> succ = succs[level];
					newNode.next[level].set(succ, false);
				}
				Node<T> succ = succs[bottomLevel];
				Node<T> pred = preds[bottomLevel];
				newNode.next[bottomLevel].set(succ, false);

				if (!pred.next[bottomLevel].compareAndSet(succ, newNode, false, false))
					continue;

				for (int level = bottomLevel + 1; level <= topLevel; level++) {
					while (true) {
						pred = preds[level];
						succ = succs[level];
						if (pred.next[level].compareAndSet(succ, newNode, false, false)) {
							break;
						}
						find(item, preds, succs);
					}
				}
				return;
			} else {
				return;
			}
		}

	}

	private boolean find(T item, Node<T>[] preds, Node<T>[] succs) {
		int bottomLevel = 0;
		int key = item.hashCode();
		boolean[] marked = { false };
		boolean snip;
		Node<T> pred = null, curr = null, succ = null;
		retry: while (true) {
			pred = head;
			for (int level = MAX_LEVEL; level >= bottomLevel; level--) {
				curr = pred.next[level].getReference();
				while (true) {
					succ = curr.next[level].get(marked);
					while (marked[0]) {
						snip = pred.next[level].compareAndSet(curr, succ, false, false);
						if (!snip)
							continue retry;
						
						curr = pred.next[level].getReference();
						succ = curr.next[level].get(marked);
					}
					if(curr.key < key) {
						pred = curr;
						curr = succ;
					} else {
						break;
					}
				}
				preds[level] = pred;
				succs[level] = curr;
			}
			return (curr.key == key);
		}
	}

	private int randomLevel() {
		return randomizer.getRandomizer().nextInt(MAX_LEVEL + 1);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sun.electric.tool.util.IStructure#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return false;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sun.electric.tool.util.IStructure#remove()
	 */
	@Override
	public T remove() {
		return null;
	}

	public static final class Node<T> {
		@SuppressWarnings("unused")
		private final T value;
		private final int key;
		private final AtomicMarkableReference<Node<T>>[] next;
		@SuppressWarnings("unused")
		private int topLevel;

		/**
		 * Constructor for sentinel nodes
		 * 
		 * @param key
		 */
		@SuppressWarnings("unchecked")
		public Node(int key) {
			this.value = null;
			this.key = key;
			this.next = (AtomicMarkableReference<Node<T>>[]) new AtomicMarkableReference[MAX_LEVEL + 1];
			for (int i = 0; i < next.length; i++)
				next[i] = new AtomicMarkableReference<Node<T>>(null, false);
			topLevel = MAX_LEVEL;
		}

		/**
		 * Constructor for ordinary nodes
		 * 
		 * @param x
		 * @param height
		 */
		@SuppressWarnings("unchecked")
		public Node(T x, int height) {
			value = x;
			key = x.hashCode();
			this.next = (AtomicMarkableReference<Node<T>>[]) new AtomicMarkableReference[height + 1];
			for (int i = 0; i < next.length; i++)
				next[i] = new AtomicMarkableReference<Node<T>>(null, false);
			topLevel = height;
		}

	}

}
