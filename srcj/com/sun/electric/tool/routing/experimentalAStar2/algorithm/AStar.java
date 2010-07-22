/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AStar.java
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

import java.util.LinkedList;
import java.util.List;

import com.sun.electric.tool.routing.experimentalAStar2.memorymanager.ObjectPool;

public abstract class AStar
{
  /**
   * Executes a path search on the provided data structures.
   * 
   * @param openList The open list to be used while searching.
   * @param closedList The closed list to be used while searching.
   * @param map The search space the A* algorithm operates on and retreives
   *          tiles from.
   * @param goal The goal object that provides data about goal tile, heuristic
   *          function, etc.
   * @param nodePool The object pool that is to provide nodes to the algorithm.
   * @param usedVisitor The visitor that is to be used for the map.
   * 
   * @return A path from start node to goal node. <code>null</code> if no path
   *         was found.
   */
  public static <T extends AStarNodeBase<T>> List<T> findPath(AStarOpenListBase<T> openList, AStarClosedListBase<T> closedList,
      AStarMapBase<T> map, AStarGoalBase<T> goal, ObjectPool<T> nodePool, AStarMapVisitorBase<T> usedVisitor)
  {
    T currentNode;
    int currentRevolutions = 0;
    while (true)
    {
      // Get the best choice so far
      currentNode = openList.removeCheapestOpenNode();
      if (currentNode == null)
        break;

      // If this returns true, then it's likely that we are not at the
      // goal and we have no more nodes to search through.
      if (goal.isPathFinished(currentNode))
        break;

      // Tell the map we want to evaluate the current node's neighbours
      map.visitNeighboursOf(currentNode, usedVisitor);

      currentRevolutions++;
      if (goal.shouldGiveUp(currentRevolutions))
        break;

      /*
       * We're done with the current node, move it to closed list.
       * 
       * IMPORTANT: This is done after checking for giving up on purpose: It
       * ensures that the current node is not stored both in the currentNode
       * variable and in the closed list at the same time. In the case we give
       * up searching, this simplifies the memory cleanup a bit, because it
       * doesn't need to check for this "duplicate" reference.
       */
      closedList.addNodeToClosedList(currentNode);
    }

    // Check if we arrived at the goal. If so, construct a path.
    List<T> path = null;
    if (currentNode != null && goal.distanceToGoal(currentNode.getX(), currentNode.getY(), currentNode.getZ()) == 0)
    {
      path = traceBackPath(currentNode);
      currentNode = null;
    }

    // Clean up memory
    openList.clearOpenList();
    closedList.clearClosedList();
    return path;
  }

  private static <T extends AStarNodeBase<T>> List<T> traceBackPath(T goalNode)
  {
    List<T> path = new LinkedList<T>();
    T currentNode = goalNode;
    path.add(currentNode);

    while (currentNode.origin != null)
    {
      currentNode = currentNode.origin;
      path.add(currentNode);
    }

    java.util.Collections.reverse(path);
    return path;
  }
}
