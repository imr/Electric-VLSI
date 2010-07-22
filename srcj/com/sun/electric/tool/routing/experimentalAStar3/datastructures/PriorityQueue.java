/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PriorityQueue.java
 * Written by: Christian Harnisch, Ingo Besenfelder, Michael Neumann (Team 3)
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
package com.sun.electric.tool.routing.experimentalAStar3.datastructures;

/**
 * Implements a priority queue.
 *
 * Taken from: http://www.ntecs.de/projects/yinspire++/src/Algorithms/BinaryHeap.h
 *
 * @author: Michael Neumann
 * @copyright: Michael Neumann
 */

public abstract class PriorityQueue<E> {

  private int capacity;
  private int size;
  private E elements[];

  final int MIN_CAPA = 32;

  public PriorityQueue()
  {
    this.capacity = 0;
    this.size = 0;
    this.elements = null;
  }

  public E[] getElements()
  {
    return elements;
  }
  
  public E top()
  {
    assert(this.size > 0);
    return this.elements[1];
  }

  public void pop()
  {
    remove(1);
  }
  
  /*
   * Remove all elements from the PQ.
   */
  public void clear()
  {
    for (int i = 1; i <= this.size; ++i)
    {
      set_index(this.elements[i], 0);  // detach from heap
    }
    this.size = 0;
  }
  
  // XXX: this is a hack!
  public void setEmpty()
  {
    this.size = 0;
  }

  private void remove(int i)
  {
    assert(i <= this.size);

    // 
    // Element i is removed from the heap and as such becomes
    // a "bubble" (free element). Move the bubble until
    // the bubble becomes a leaf element. 
    //
    set_index(this.elements[i], 0);  // detach from heap
    int bubble = move_bubble_down(i);

    //
    // Now take the last element and insert it at the position of
    // the bubble. In case the bubble is already the last element we
    // are done.
    //
    if (bubble != this.size)
    {
      insert_and_bubble_up(bubble, this.elements[this.size]);
    }
    --this.size;
  }

  public void push(E element)
  {
    if (this.size >= this.capacity) resize(2*this.capacity);
    insert_and_bubble_up(++this.size, element);
  }

  public int size()
  {
    return this.size;
  }

  public boolean empty()
  {
    return (this.size == 0);
  }

  /*
   * Insert +element+ into the heap beginning from
   * +i+ and searching upwards to the root for the 
   * right position (heap ordered) to insert.
   *
   * Element at index +i+ MUST be empty, i.e. unused!
   */
  private void insert_and_bubble_up(int i, E element)
  {
    for (;i >= 2 && less(element, this.elements[i/2]); i /= 2)
    {
      store_element(i, this.elements[i/2]);
    }

    // finally store it into the determined hole
    store_element(i, element);
  }

  /*
   * Move the bubble (empty element) at +i+ down in direction
   * to the leaves. When the bubble reaches a leaf, stop and
   * return the index of the leaf element which is now empty.
   */
  private int move_bubble_down(int i)
  {
    int sz = this.size;
    int right_child = i * 2 + 1;

    while (right_child <= sz) 
    {
      if (less(this.elements[right_child-1], this.elements[right_child]))
      {
        --right_child; // minimum child is left child
      }

      store_element(i, this.elements[right_child]);
      i = right_child;
      right_child = i * 2 + 1;
    }

    //
    // Edge case (comparison with the last element)
    //
    if (right_child-1 == sz)
    {
      store_element(i, this.elements[right_child-1]);
      i = right_child-1;
    }

    return i;
  }

  private void resize(int new_capacity)
  {	  
    E new_elements[];

    if (new_capacity < MIN_CAPA) this.capacity = MIN_CAPA;  
    else this.capacity = new_capacity;

    new_elements = alloc_array(this.capacity+1);

    if (this.elements != null)
    {
      /*
       * Copy elements to new array
       */
      for (int i = 1; i <= this.size; ++i)
      {
        new_elements[i] = this.elements[i];
      }
    }

    assert(new_elements != null);
    assert(this.capacity >= this.size);

    this.elements = new_elements; 
  }

  private void store_element(int i, E element)
  {
    this.elements[i] = element;
    set_index(this.elements[i], i); 
  }

  public void update(E element)
  {
    int i = get_index(element);
    if (i == 0)
    {
      push(element);
    }
    else
    {
      // FIXME: use propagate up/down instead
      set_index(this.elements[i], 0);  // detach from heap
      int bubble = move_bubble_down(i);
      insert_and_bubble_up(bubble, element); 
    }
  }
  
  public void remove(E element)
  {
    int i = get_index(element);
    if (i > 0)
      remove(i);
  }
  
  abstract protected boolean less(E a, E b);
  abstract protected int get_index(E e);
  abstract protected void set_index(E e, int new_index);
  abstract protected E[] alloc_array(int sz); 
}
