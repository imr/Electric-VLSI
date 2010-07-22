/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SimpleGoal.java
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
package com.sun.electric.tool.routing.experimentalAStar2.goal;

import com.sun.electric.tool.routing.experimentalAStar2.algorithm.AStarGoalBase;
import com.sun.electric.tool.routing.experimentalAStar2.algorithm.AStarNode;
import com.sun.electric.tool.routing.experimentalAStar2.algorithm.AStarOpenListBase;
import com.sun.electric.tool.routing.experimentalAStar2.concurrency.RoutingMain;

public class SimpleGoal implements AStarGoalBase<AStarNode>
{
  /* Tuning parameters */
  /**
   * Parameter to set penalty for changing direction. 1 is enough. Set 0 to
   * disable.
   */
  private static final int BEND_PENALTY = 1;

  /**
   * Parameter to set penalty for routing at the border of a region. Routing at
   * the border is bad, because it blocks portals.
   */
  private static final int BORDER_PENALTY = 1;

  /*
   * From here on below: Private variables for internal use. Don't try to tune
   * these.
   */
  private AStarOpenListBase<AStarNode> openList;

  private AStarNode goalNode;

  private int maxRevolutions = 0;

  /*
   * (non-Javadoc)
   * 
   * @see algorithm.AStarGoalBase#setGoalTile(algorithm.Tile)
   */
  // @Override
  public void setGoalNode(AStarNode goal)
  {
    this.goalNode = goal;
  }

  private int distance(int startX, int startY, int startZ, int endX, int endY, int endZ)
  {
    // Because distance should be "more important" than other penalties,
    // multiply by 10. This should make wires running far outside the area less
    // likely.
    return 10 * (Math.abs(startX - endX) + Math.abs(startY - endY));
  }

  /*
   * (non-Javadoc)
   * 
   * @see algorithm.AStarGoalBase#distanceToGoal(algorithm.Tile)
   */
  // @Override
  public int distanceToGoal(int startX, int startY, int startZ)
  {
    // Because we don't do diagonal wires, the Manhattan Distance works fine for
    // our purposes.
    return distance(startX, startY, startZ, this.goalNode.getX(), this.goalNode.getY(), this.goalNode.getZ());
  }

  /*
   * (non-Javadoc)
   * 
   * @see algorithm.AStarGoalBase#getTileCost(algorithm.Tile, algorithm.Tile)
   */
  // @Override
  public int getNodeCost(AStarNode from, int endX, int endY, int endZ)
  {
    int bendPenalty = 0;
    if (from.origin != null
        && ((from.origin.getX() == from.getX() && endX != from.getX()) || (from.origin.getY() == from.getY() && endY != from
            .getY())))
      bendPenalty = BEND_PENALTY;
    int borderPenalty = 0;
    // Path tries to stay away from the borders
    if (endX == 0 || endY == 0 || endX == (RoutingMain.regionWidth - 1) || endY == (RoutingMain.regionHeight - 1))
      borderPenalty = BORDER_PENALTY;
    // This simple implementation always returns the distance (so it's a cost
    // of
    // 1 per step).
    return distance(from.getX(), from.getY(), from.getZ(), endX, endY, endZ) + bendPenalty + borderPenalty;
  }

  /*
   * (non-Javadoc)
   * 
   * @see algorithm.AStarGoalBase#isPathFinished(algorithm.AStarNode)
   */
  // @Override
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
  // @Override
  public void setNodeStorage(AStarOpenListBase<AStarNode> openList)
  {
    this.openList = openList;
  }

  /*
   * (non-Javadoc)
   * 
   * @see algorithm.AStarGoalBase#setMaxRevolutions(int)
   */
  // @Override
  public void setMaximumRevolutions(int maximum)
  {
    maxRevolutions = maximum;
  }

  /*
   * (non-Javadoc)
   * 
   * @see algorithm.AStarGoalBase#shouldGiveUp(int)
   */
  // @Override
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
