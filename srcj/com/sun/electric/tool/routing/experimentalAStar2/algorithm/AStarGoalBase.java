/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AStarGoalBase.java
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

/**
 * The goal object provides data to the A* algorithm, so that the algorithm can
 * be kept more generic.
 * 
 * Data provided includes estimated distance to goal, movement costs between
 * adjacent tiles, and whether the search should be ended.
 */
public interface AStarGoalBase<T extends AStarNodeBase<T>>
{
  /**
   * Tells the goal about the used storage containers.
   * 
   * @param openList
   * @param closedList
   */
  public void setNodeStorage(AStarOpenListBase<T> openList);

  /**
   * Sets the number of revolutions the A* search algorithm may execute, before
   * the goal declares the search unsuccessful. By default, there is no
   * specified maximum, thus the search may go on indefinitely.
   * 
   * @param maximum The number of revolutions.
   */
  public void setMaximumRevolutions(int maximum);

  // Currently not needed - perhaps later, if we support concurrent searches +
  // time-slicing
  // public boolean shouldPause();

  public void setGoalNode(T goal);

  /**
   * The heuristic function used by the A* algorithm to judge the cost of a
   * node.
   * 
   * @param tile The map tile whose distance to the goal tile is requested.
   * @return Estimate of the distance.
   */
  public int distanceToGoal(int startX, int startY, int startZ);

  // Don't see why we need this, as we get passable tiles only from the map
  // anyway when asking for a tile's neighbours.
  // public boolean isTileOpen();

  /**
   * Returns the actual cost accurately, for moving from node <code>from</code>
   * to the given set of coordinates. It is assumed that these are adjacent.
   * 
   * @param from Start node of movement.
   * @param toX X-position of destination.
   * @param toY Y-position of destination.
   * @param toZ Z-position of destination.
   * @return Cost of movement.
   */
  public int getNodeCost(T from, int toX, int toY, int toZ);

  /**
   * Returns if the given node corresponds to the goal tile on the map.
   * 
   * @param currentNode The node to check for corresponding to the goal tile.
   * @return <code>true</code> if the node's tile is the goal,
   *         <code>false</code> otherwise.
   */
  public boolean isPathFinished(T currentNode);

  // Used for pathfinding smoothing, we don't do this.
  // public boolean shouldReevaluateNode();

  /**
   * Returns if the taken search revolutions exceed the set maximum.
   * 
   * @param currentRevolutions Number of revolutions the search has already
   *          taken.
   * @return <code>true</code> if the search should be ended, <code>false</code>
   *         otherwise.
   */
  // This can't be handled in isPathFinished(), because the A* loop would then
  // quit too early!
  // Thus, it must be done at the end of an iteration.
  public boolean shouldGiveUp(int currentRevolutions);
}
