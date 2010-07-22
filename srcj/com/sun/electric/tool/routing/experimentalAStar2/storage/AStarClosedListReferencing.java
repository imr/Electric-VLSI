/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AStarClosedListReferencing.java
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
import java.util.List;

import com.sun.electric.tool.routing.experimentalAStar2.algorithm.AStarClosedListBase;
import com.sun.electric.tool.routing.experimentalAStar2.algorithm.AStarMapBase;
import com.sun.electric.tool.routing.experimentalAStar2.algorithm.AStarNodeBase;

public class AStarClosedListReferencing<T extends AStarNodeBase<T>> implements AStarClosedListBase<T>
{
  private AStarMapBase<T> map;

  private List<T> nodes = new ArrayList<T>();

  public void setMap(AStarMapBase<T> map)
  {
    this.clearClosedList();
    this.map = map;
  }

  /*
   * (non-Javadoc)
   * 
   * @see algorithm.AStarClosedListBase#addNodeToClosedList(algorithm.T)
   */
  public void addNodeToClosedList(T node)
  {
    node.markAsClosed();
    nodes.add(node);
  }

  /*
   * (non-Javadoc)
   * 
   * @see algorithm.AStarClosedListBase#findClosedNode(algorithm.Tile)
   */
  public T findClosedNode(int x, int y, int z)
  {
    T node = map.nodeAt(x, y, z);
    if (node != null && node.isClosed())
      return node;
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see algorithm.AStarClosedListBase#dumpClosedList()
   */
  public Collection<T> dumpClosedList()
  {
    // nothing to dump, because all in map.
    return new ArrayList<T>();
  }

  /*
   * (non-Javadoc)
   * 
   * @see algorithm.AStarClosedListBase#removeNodeFromClosedList(algorithm.T )
   */
  public void removeNodeFromClosedList(T node)
  {
    // nothing to remove, just mark in map.
    node.markAsNoList();
  }

  /*
   * (non-Javadoc)
   * 
   * @seecom.sun.electric.tool.routing.astar.t3.algorithm.AStarClosedListBase#
   * clearClosedList()
   */
  public void clearClosedList()
  {
    for (T node : this.nodes)
    {
      this.removeNodeFromClosedList(node);
    }
    this.nodes.clear();
  }
}