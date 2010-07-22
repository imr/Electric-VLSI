/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LinkedListObjectPool.java
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
package com.sun.electric.tool.routing.experimentalAStar3.memorymanager;

import java.util.LinkedList;

/**
 * Memory pool, implemented using a linked list.
 * 
 * @author Christian Harnisch
 * 
 */
public class LinkedListObjectPool<E> implements ObjectPool<E>
{
  private LinkedList<E> pool;

  private LinkedList<E> used;

  private final ObjectFactory<E> factory;

  public LinkedListObjectPool(int initialCapacity, ObjectFactory<E> factory)
  {
    if (initialCapacity < 1)
      throw new IllegalArgumentException("Capacity of the object pool must be 1 or greater.");
    pool = new LinkedList<E>();
    for (int i = 0; i < initialCapacity; ++i)
      pool.add(factory.create());
    used = new LinkedList<E>();
    this.factory = factory;
  }

  public synchronized E acquire()
  {
    E object;
    if (pool.isEmpty())
      object = factory.create();
    else
      object = pool.remove();
    used.add(object);
    return object;
  }

  public synchronized void release(E object)
  {
    if (!used.remove(object))
      throw new IllegalArgumentException("The given object does not belong to this object pool.");
    pool.add(object);
  }
}
