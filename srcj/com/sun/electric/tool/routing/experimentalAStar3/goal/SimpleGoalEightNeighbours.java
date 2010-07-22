/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SimpleGoalEightNeighbours.java
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
package com.sun.electric.tool.routing.experimentalAStar3.goal;

import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarGoalBase;
import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarNode;
import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarOpenListBase;

/**
 * Mostly the same as the SimpleGoal, but works with eight directions.
 * 
 * @author Christian Harnisch
 * 
 */
public class SimpleGoalEightNeighbours implements AStarGoalBase<AStarNode>
{
  private AStarOpenListBase<AStarNode> openList;

  private AStarNode goalNode;

  private int maxRevolutions = 0;

  /*
   * (non-Javadoc)
   * 
   * @see algorithm.AStarGoalBase#setGoalTile(algorithm.Tile)
   */
  public void setGoalNode(AStarNode goal)
  {
    this.goalNode = goal;
  }

  /*
   * (non-Javadoc)
   * 
   * @see algorithm.AStarGoalBase#distanceToGoal(algorithm.Tile)
   */
  public int distanceToGoal(int startX, int startY, int startZ)
  {
    return distance(startX, startY, startZ, this.goalNode.getX(), this.goalNode.getY(), this.goalNode.getZ());
  }

  /*
   * (non-Javadoc)
   * 
   * @see algorithm.AStarGoalBase#getTileCost(algorithm.Tile, algorithm.Tile)
   */
  public int getNodeCost(AStarNode node, int endX, int endY, int endZ)
  {
    // This simple implementation always returns the distance (so it's a cost of
    // 1 per step).
    return distance(node.getX(), node.getY(), node.getZ(), endX, endY, endZ);
  }

  /*
   * (non-Javadoc)
   * 
   * @see algorithm.AStarGoalBase#isPathFinished(algorithm.AStarNode)
   */
  public boolean isPathFinished(AStarNode currentNode)
  {
    // Arrived at goal node
    return goalNode.equals(currentNode);
  }

  /*
   * (non-Javadoc)
   * 
   * @see algorithm.AStarGoalBase#setNodeStorage(algorithm.AStarOpenListBase,
   * algorithm.AStarClosedListBase)
   */
  public void setNodeStorage(AStarOpenListBase<AStarNode> openList)
  {
    this.openList = openList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see algorithm.AStarGoalBase#setMaxRevolutions(int)
   */
  public void setMaximumRevolutions(int maximum)
  {
    maxRevolutions = maximum;
  }

  /*
   * (non-Javadoc)
   * 
   * @see algorithm.AStarGoalBase#shouldGiveUp(int)
   */
  public boolean shouldGiveUp(int currentRevolutions)
  {
    // Maximum search time exceeded
    if (maxRevolutions > 0 && currentRevolutions >= maxRevolutions)
      return true;
    // No more nodes to search, thus, no possible path exists.
    if (openList.isOpenListEmpty())
      return true;
    return false;
  }

  // Diagonal wires allowed here, thus use Euclidean distance
  private int distance(int startX, int startY, int startZ, int endX, int endY, int endZ)
  {
    int deltaX = startX - endX;
    int deltaY = startY - endY;
    return (int) (10 * Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2)));
  }

}
