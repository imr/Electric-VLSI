/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AStarOpenListCheapList.java
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
package com.sun.electric.tool.routing.experimentalAStar2.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;

import com.sun.electric.tool.routing.experimentalAStar2.algorithm.AStarMapBase;
import com.sun.electric.tool.routing.experimentalAStar2.algorithm.AStarNodeBase;
import com.sun.electric.tool.routing.experimentalAStar2.algorithm.AStarOpenListBase;

public class AStarOpenListCheapList<T extends AStarNodeBase<T>> implements AStarOpenListBase<T>
{
  /**
   * Find best value how many elements to be inserted into CheapList initially.
   */
  private int CHEAPLIST_INIT_SIZE = 15;

  LinkedList<T> openList = new LinkedList<T>();

  CheapList cheapList = new CheapList();

  AStarMapBase<T> map;

  public void setMap(AStarMapBase<T> map)
  {
    this.map = map;
  }

  /**
   * Reinitializes the CheapList. Either sort the openList in O(n log n) or run
   * through it <code>CHEAPLIST_INIT_SIZE</code> times to find (and remove) the
   * best ones. Then insert the found elements into the CheapList in O(
   * <code>CHEAPLIST_INIT_SIZE</code>&sup2;)
   * 
   * @param fillingSize number of elements to be move to CheapList.
   */
  public void reinitializeCheapList(int fillingSize)
  {
    Collections.sort(this.openList);
    int i = 0;
    while (i < fillingSize && !this.openList.isEmpty())
    {
      this.cheapList.forcedInsert(this.openList.removeFirst());
      i++;
    }
  }

  public void addNodeToOpenList(T node)
  {
    if (this.cheapList.isEmpty())
      reinitializeCheapList(CHEAPLIST_INIT_SIZE);
    if (this.cheapList.isEmpty() || !this.cheapList.cheapInsert(node))
      // lazy eval is important for correctness!
      this.openList.add(node);
    node.markAsOpen();
  }

  public Collection<T> dumpOpenList()
  {
    Collection<T> dump = new ArrayList<T>(this.cheapList.dump());
    dump.addAll(this.openList);
    this.openList.clear();
    return dump;
  }

  /*
   * (non-Javadoc)
   * 
   * @seecom.sun.electric.tool.routing.astar.t3.algorithm.AStarOpenListBase#
   * clearOpenList()
   */
  public void clearOpenList()
  {
    this.cheapList.clear();
    for (T node : this.openList)
    {
      node.markAsNoList();
    }
    this.openList.clear();
  }

  public T findOpenNode(int x, int y, int z)
  {
    T node = map.nodeAt(x, y, z);
    if (node != null && node.isOpen())
      return node;
    return null;
  }

  public boolean isOpenListEmpty()
  {
    if (this.cheapList.head != null)
      return false;
    return this.openList.isEmpty();
  }

  public T removeCheapestOpenNode()
  {
    if (this.cheapList.isEmpty())
      reinitializeCheapList(this.CHEAPLIST_INIT_SIZE);
    T result = this.cheapList.pop();
    if (this.cheapList.isEmpty())
      reinitializeCheapList(this.CHEAPLIST_INIT_SIZE);
    if (result != null)
      result.markAsNoList();
    return result;
  }

  public void removeNodeFromOpenList(T node)
  {
    if (this.cheapList.remove(node))
    {
      if (this.cheapList.isEmpty())
        reinitializeCheapList(CHEAPLIST_INIT_SIZE);
    }
    else
      this.openList.remove(node);
    node.markAsNoList();
  }

  /**
   * The cheapList initially contains the <code>CHEAPLIST_INIT_SIZE</code>
   * cheapest nodes of the openList. It will be filled up in the initialization
   * phase and only filled again when it is empty.<br>
   * <br>
   * <b>Runtime assumptions:</b><br>
   * <b>insert():</b> O(k) with k = a relatively small constant. O(1) for all
   * elements with higher cost than tail.<br>
   * <b>getCheapestNode():</b> O(1).<br>
   * <b>isEmpty():</b> O(1)<br>
   */
  class CheapList extends ADoubleLinkedList<T>
  {
    /**
     * Sorted insertion in O(k). Node is only inserted if better than tail =>
     * Small list of cheapest Nodes.
     */
    public boolean cheapInsert(T node)
    {
      AElement elementToInsert = new AElement(node);
      if (this.head == null && this.tail == null)
      {
        // initial insert
        this.head = elementToInsert;
        this.tail = elementToInsert;
        return true;
      }
      else if (node.getTotalCost() > this.tail.data.getTotalCost())
      {
        return false;
      }
      else
      {
        AElement current = this.head;
        while (current != null)
        {
          if (node.getTotalCost() < current.data.getTotalCost())
          /*
           * TODO: optimize with this perhaps: || (node.getTotalCost() ==
           * current.data.getTotalCost() && node.getCostToGoal() <
           * current.data.getCostToGoal()))
           */
          {
            if (current.hasPrev())
            {
              current.prev.next = elementToInsert;
              elementToInsert.prev = current.prev;
            }
            else
            {
              // no prev means: current is head
              this.head = elementToInsert;
            }
            current.prev = elementToInsert;
            elementToInsert.next = current;
            return true;
          }
          current = current.next;
        }
        if (node.getTotalCost() == this.tail.data.getTotalCost())
        {
          this.tail.next = elementToInsert;
          elementToInsert.prev = this.tail;
          this.tail = elementToInsert;
          return true;
        }
      }
      return false;
    }

    /**
     * Sorted insertion in O(k). Node is only inserted if better than tail =>
     * Small list of cheapest Nodes.
     */
    public boolean forcedInsert(T node)
    {
      AElement elementToInsert = new AElement(node);
      if (this.head == null && this.tail == null)
      {
        // initial insert
        this.head = elementToInsert;
        this.tail = elementToInsert;
        return true;
      }
      if (node.getTotalCost() >= this.tail.data.getTotalCost())
      {
        this.tail.next = elementToInsert;
        elementToInsert.prev = this.tail;
        this.tail = elementToInsert;
        return true;
      }
      AElement current = this.head;
      while (current != null)
      {
        if (node.getTotalCost() < current.data.getTotalCost())
        {
          if (current.hasPrev())
          {
            current.prev.next = elementToInsert;
            elementToInsert.prev = current.prev;
          }
          else
          {
            // no prev means: current is head
            this.head = elementToInsert;
          }
          current.prev = elementToInsert;
          elementToInsert.next = current;
          return true;
        }
        current = current.next;
      }
      return false;
    }

    public void print()
    {
      AElement current = head;
      while (current != null)
      {
        System.err.print("(" + current.data.getX() + "," + current.data.getY() + "),");
        current = current.next;
      }
    }

    public ArrayList<T> dump()
    {
      ArrayList<T> list = new ArrayList<T>();
      while (this.head != null)
      {
        T currentHead = this.head.data;
        list.add(currentHead);
        this.remove(currentHead);
      }
      return list;
    }

    public void clear()
    {
      while (this.head != null)
      {
        T currentHead = this.head.data;
        currentHead.markAsNoList();
        this.remove(currentHead);
      }
    }

    public T pop()
    {
      if (this.head == null)
        return null;
      T currentHead = this.head.data;
      this.remove(currentHead);
      return currentHead;
    }

    public boolean isEmpty()
    {
      return this.head == null;
    }

    /**
     * Retrieves an Object in O(n).
     * 
     * @return the found element, or null.
     */
    public T getNode(int x, int y, int z)
    {
      AElement current = this.head;
      while (current != null)
      {
        if (current.data.getX() == x && current.data.getY() == y && current.data.getZ() == z)
        {
          return current.data;
        }
        current = current.next;
      }
      return null;
    }
  }

  class ADoubleLinkedList<S>
  {
    // head and tail
    AElement head = null;

    AElement tail = null;

    /**
     * Inserts an Object in O(1).
     * 
     * @param data the Object which contains the actual data to be stored.
     * @return true if inserted successfully.
     */
    public boolean insert(S data)
    {
      AElement elementToInsert = new AElement(data);
      if (this.head == null && this.tail == null)
      {
        // initial insert
        this.head = elementToInsert;
        this.tail = elementToInsert;
        this.head.next = this.tail;
        this.tail.prev = this.head;
      }
      else
      {
        // append to tail
        this.tail.next = elementToInsert;
        this.tail = elementToInsert;
      }
      return true;
    }

    /**
     * Removes an Object in O(n) after finding it.
     * 
     * @return true if element was found, else false.
     */
    public boolean remove(S data)
    {
      AElement current = this.head;
      boolean found = false;
      while (current != null)
      {
        if (current.data.equals(data))
        {
          found = true;
          if (current.hasPrev())
          {
            current.prev.next = current.next;
            if (!current.hasNext())
              this.tail = current.prev;
          }
          if (current.hasNext())
          {
            current.next.prev = current.prev;
            if (!current.hasPrev())
              this.head = current.next;
          }
          if (!current.hasNext() && !current.hasPrev())
          {
            this.head = null;
            this.tail = null;
          }
          break;
        }
        current = current.next;
      }
      return found;
    }

    /**
     * The wrapping Element. This practically only adds two pointers to the
     * actual data object, that has to be stored in the list.
     * 
     */
    class AElement
    {
      AElement prev = null;

      AElement next = null;

      S data;

      public AElement(S data)
      {
        this.data = data;
      }

      public boolean hasNext()
      {
        return this.next != null;
      }

      public boolean hasPrev()
      {
        return this.prev != null;
      }
    }
  }
}