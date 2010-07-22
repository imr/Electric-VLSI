/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: RegionGoal.java
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
import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarOpenListBase;
import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarRegionNode;

/**
 * @author Christian Harnisch
 * 
 */
public class RegionGoal implements AStarGoalBase<AStarRegionNode>
{
  /* Tuning parameters */
  /**
   * Parameter to set penalty for bends in wires. 1 is enough. Set 0 to disable.
   */
  private static final int BEND_PENALTY = 1;

  /**
   * Parameter to set penalty for using vias in paths.
   */
  private static final int VIA_PENALTY = 1;

  /*
   * From here on below: Private variables for internal use. Don't try to tune
   * these.
   */
  private AStarOpenListBase<AStarRegionNode> openList;

  private AStarRegionNode goalNode;

  private int maxRevolutions = 0;

  private int distance(int startX, int startY, int startZ, int endX, int endY, int endZ)
  {
    return Math.abs(startX - endX) + Math.abs(startY - endY) + (Math.abs(startZ - endZ));
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.sun.electric.tool.routing.astar.t3.algorithm.AStarGoalBase#distanceToGoal
   * (int, int, int)
   */
  public int distanceToGoal(int startX, int startY, int startZ)
  {
    int viaPenaltyEstimation = Math.abs(startZ - this.goalNode.getZ()) * VIA_PENALTY;
    return distance(startX, startY, startZ, this.goalNode.getX(), this.goalNode.getY(), this.goalNode.getZ())
        + viaPenaltyEstimation;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.sun.electric.tool.routing.astar.t3.algorithm.AStarGoalBase#getNodeCost
   * (com.sun.electric.tool.routing.astar.t3.algorithm.AStarNode, int, int, int)
   */
  public int getNodeCost(AStarRegionNode from, int toX, int toY, int toZ)
  {
    /*
     * If we go horizontally on one layer, we prevent vertical wires from being
     * routed. Thus, the cost of moving between two horizontally adjacent nodes
     * is the capacity we destroy that way. This is also true the other way
     * round (moving vertically prevents horizontal wires from being routed).
     * 
     * Obviously, the more accurate the capacity calculating algorithm can
     * determine the capacities, the better A* behaves.
     */
    int bendPenalty = 0;
    int viaPenalty = 0;

    if (from.getZ() != toZ)
      viaPenalty = VIA_PENALTY;

    // Moving horizontally
    if (from.getY() == toY)
    {
      // Bends are bad, even globally
      if (from.origin != null)
        if (from.origin.getY() != toY)
          bendPenalty = BEND_PENALTY;
      return (from.getVerticalCapacity() + bendPenalty + viaPenalty);
    }
    else
    // Moving vertically
    if (from.getX() == toX)
    {
      // Bends are bad, even globally
      if (from.origin != null)
        if (from.origin.getX() != toX)
          bendPenalty = BEND_PENALTY;
      return (from.getHorizontalCapacity() + bendPenalty + viaPenalty);
    }
    else
    {
      /*
       * Moving diagonally: Does not make sense for global routing. How do you
       * intend to route diagonally between two regions, without touching the
       * other adjacent regions?
       */
      throw new IllegalArgumentException("Routing diagonally is not allowed globally, tried to route from (" + from.getX() + ","
          + from.getY() + "," + from.getZ() + ") to (" + toX + "," + toY + "," + toZ + ").");
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.sun.electric.tool.routing.astar.t3.algorithm.AStarGoalBase#isPathFinished
   * (com.sun.electric.tool.routing.astar.t3.algorithm.AStarNode)
   */
  public boolean isPathFinished(AStarRegionNode currentNode)
  {
    // Arrived at goal node
    return this.goalNode.equals(currentNode);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.sun.electric.tool.routing.astar.t3.algorithm.AStarGoalBase#setGoalNode
   * (com.sun.electric.tool.routing.astar.t3.algorithm.AStarNode)
   */
  public void setGoalNode(AStarRegionNode goal)
  {
    this.goalNode = goal;
  }

  /*
   * (non-Javadoc)
   * 
   * @seecom.sun.electric.tool.routing.astar.t3.algorithm.AStarGoalBase#
   * setMaximumRevolutions(int)
   */
  public void setMaximumRevolutions(int maximum)
  {
    this.maxRevolutions = maximum;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.sun.electric.tool.routing.astar.t3.algorithm.AStarGoalBase#setNodeStorage
   * (com.sun.electric.tool.routing.astar.t3.algorithm.AStarOpenListBase)
   */
  public void setNodeStorage(AStarOpenListBase<AStarRegionNode> openList)
  {
    this.openList = openList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.sun.electric.tool.routing.astar.t3.algorithm.AStarGoalBase#shouldGiveUp
   * (int)
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
}
