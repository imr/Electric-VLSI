/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AStarClosedListBase.java
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
package com.sun.electric.tool.routing.experimentalAStar2.algorithm;

public interface AStarClosedListBase<T extends AStarNodeBase<T>>
{
  /**
   * Marks a node as been visited and expanded, which means that his neighbour
   * nodes have been added to the open list.
   * 
   * @param node The node to add.
   */
  public void addNodeToClosedList(T node);

  /**
   * If contained in the list of visited and expanded nodes, the given node is
   * removed. Otherwise, the list remains unchanged.
   * 
   * @param node The node to be removed.
   */
  public void removeNodeFromClosedList(T node);

  /**
   * If this position has already been visited by the A* search, and its nodes
   * neighbours have been added to the open list, this method returns the node
   * associated with the given position.
   * 
   * @param x X-position of the map to be checked.
   * @param y Y-position of the map to be checked.
   * @param z Z-position of the map to be checked.
   * 
   * @return A node if one was found, <code>null</code> otherwise.
   */
  public T findClosedNode(int x, int y, int z);

//  /**
//   * Empties the closed list, and returns a collection of all nodes formerly
//   * contained in the closed list.
//   *
//   * @return All nodes formerly in the closed list.
//   *
//   * @deprecated As requested nodes are stored in the map permanently, and never
//   *             released, dumping all nodes into a list is not necessary any
//   *             more.
//   */
//  @Deprecated
//  public Collection<T> dumpClosedList();

  /**
   * Removes all nodes from the closed list.
   */
  public void clearClosedList();
}
