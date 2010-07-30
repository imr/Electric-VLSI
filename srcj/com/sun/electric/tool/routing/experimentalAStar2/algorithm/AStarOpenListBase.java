/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AStarOpenListBase.java
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

import java.util.Collection;

public interface AStarOpenListBase<T extends AStarNodeBase<T>>
{
  /**
   * Returns the node with the cheapest total cost, and removes it from the open
   * list. <br>
   * <br>
   * The total cost is the sum of the accurate cost from the start position, and
   * the estimated cost to the goal position. Therefore, the cheapest node is
   * the most promising candidate for continuing path search.
   * 
   * @return Cheapest node if the list is not empty, <code>null</code>
   *         otherwise.
   */
  public T removeCheapestOpenNode();

  /**
   * Inserts a node in the list of nodes to be visited later while searching.
   * 
   * @param node The node to add.
   */
  public void addNodeToOpenList(T node);

  /**
   * If contained in the list of nodes to be visited later, the given node is
   * removed. Otherwise, the list remains unchanged.
   * 
   * @param node The node to be removed.
   */
  public void removeNodeFromOpenList(T node);

  /**
   * If this position has not yet been visited by the A* search, this method
   * returns the node associated with the given position.
   * 
   * @param x X-position of the map to be checked.
   * @param y Y-position of the map to be checked.
   * @param z Z-position of the map to be checked.
   * 
   * @return A node if one was found, <code>null</code> otherwise.
   */
  public T findOpenNode(int x, int y, int z);

  /**
   * Returns if there are no nodes to be visited.
   * 
   * @return <code>true</code> if there are no nodes left, <code>false</code>
   *         otherwise.
   */
  public boolean isOpenListEmpty();

//  /**
//   * Empties the open list, and returns a collection of all nodes formerly
//   * contained in the open list.
//   *
//   * @return All nodes formerly in the open list.
//   *
//   * @deprecated As requested nodes are stored in the map permanently, and never
//   *             released, dumping all nodes into a list is not necessary any
//   *             more.
//   */
//  @Deprecated
//  public Collection<T> dumpOpenList();

  /**
   * Removes all nodes from the open list.
   */
  public void clearOpenList();

}
