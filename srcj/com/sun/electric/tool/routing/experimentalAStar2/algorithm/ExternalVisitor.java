/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ExternalVisitor.java
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

public class ExternalVisitor<T extends AStarNodeBase<T>> implements AStarMapVisitorBase<T>
{
  /* Search context */
  private AStarOpenListBase<T> openList;

  private AStarClosedListBase<T> closedList;

  private AStarMapBase<T> map;

  private AStarGoalBase<T> goal;

  public ExternalVisitor(AStarOpenListBase<T> openList, AStarClosedListBase<T> closedList, AStarMapBase<T> map,
      AStarGoalBase<T> goal)
  {
    this.openList = openList;
    this.closedList = closedList;
    this.map = map;
    this.goal = goal;
  }

  /*
   * (non-Javadoc)
   * 
   * @seecom.sun.electric.tool.routing.astar.t3.algorithm.AStarMapVisitorBase#
   * visitNeighbour(com.sun.electric.tool.routing.astar.t3.algorithm.T, int,
   * int, int)
   */
  public void visitNeighbour(T origin, int x, int y, int z)
  {
    // Tile neighbour = neighbours[i];
    int costFromStart = origin.costFromStart + goal.getNodeCost(origin, x, y, z);
    int costToGoal = goal.distanceToGoal(x, y, z);
    int totalCost = costFromStart + costToGoal;

    // Check if on open/closed list
    // If yes, check if new path is more efficient (lower f value)
    // and update path if so.
    T foundNode = openList.findOpenNode(x, y, z);
    boolean foundCheaperPath = false;
    if (foundNode != null)
    {
      if (totalCost < foundNode.getTotalCost())
      {
        foundCheaperPath = true;
        openList.removeNodeFromOpenList(foundNode);
      }
    }
    else
    {
      foundNode = closedList.findClosedNode(x, y, z);
      if (foundNode != null)
      {
        if (totalCost < foundNode.getTotalCost())
        {
          foundCheaperPath = true;
          closedList.removeNodeFromClosedList(foundNode);
        }
      }
      else
      {
        // Node not found in open and closed list, thus put a
        // new one on the open list.
        foundNode = map.nodeAt(x, y, z);
        foundCheaperPath = true;
      }
    }

    if (foundCheaperPath)
    {
      foundNode.origin = origin;
      foundNode.costFromStart = costFromStart;
      foundNode.costToGoal = costToGoal;
      foundNode.totalCost = totalCost;
      openList.addNodeToOpenList(foundNode);
    }
  }

}
