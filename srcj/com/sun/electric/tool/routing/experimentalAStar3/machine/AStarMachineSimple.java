/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AStarMachineSimple.java
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
package com.sun.electric.tool.routing.experimentalAStar3.machine;

import java.util.List;

import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStar;
import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarClosedListBase;
import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarGoalBase;
import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarMapBase;
import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarNode;
import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarOpenListBase;
import com.sun.electric.tool.routing.experimentalAStar3.algorithm.ExternalVisitor;
import com.sun.electric.tool.routing.experimentalAStar3.memorymanager.ObjectPool;
import com.sun.electric.tool.routing.experimentalAStar3.storage.AStarClosedListHashMap;
import com.sun.electric.tool.routing.experimentalAStar3.storage.AStarOpenListPriorityQueue;

/**
 * @author Christian Harnisch
 * 
 */
public class AStarMachineSimple implements AStarMachine<AStarNode>
{
  private ObjectPool<AStarNode> nodePool;

  private AStarOpenListBase<AStarNode> openList;

  private AStarClosedListBase<AStarNode> closedList;

  private AStarMapBase<AStarNode> map = null;

  private AStarGoalBase<AStarNode> goal = null;

  /*
   * This is the six-steps-procedure to get an instance of A* up and running.
   * It's the same for all machines, and is also similarly used in the unit
   * tests of A*.
   */
  // First, create memory manager(s)
  // Second, create storage containers
  // Third, create map
  // Fourth, create goal
  // Fifth, create A* instance
  // Sixth, run search
  public AStarMachineSimple(ObjectPool<AStarNode> nodePool)
  {
    this.nodePool = nodePool;
    this.openList = new AStarOpenListPriorityQueue();
    this.closedList = new AStarClosedListHashMap();
  }

  /*
   * (non-Javadoc)
   * 
   * @see machine.AStarMachine#findPath()
   */
  public List<AStarNode> findPath(int startX, int startY, int startZ, int endX, int endY, int endZ)
  {
    if (map == null)
      throw new IllegalStateException("The searched map must be specified before path search can be done.");
    if (this.goal == null)
      throw new IllegalStateException("The goal object must be specified before path search can be done.");

    AStarNode goalNode = map.nodeAt(endX, endY, endZ);
    int goalDistance = this.goal.getNodeCost(goalNode, startX, startY, startZ);
    goalNode.initialize(null, goalDistance, 0, goalDistance, endX, endY, endZ);
    this.goal.setGoalNode(goalNode);

    AStarNode startNode = map.nodeAt(startX, startY, startZ);
    startNode.initialize(null, 0, goalDistance, goalDistance, startX, startY, startZ);
    openList.addNodeToOpenList(startNode);
    return AStar.findPath(openList, closedList, map, this.goal, nodePool, new ExternalVisitor<AStarNode>(openList, closedList,
        map, this.goal));
  }

  /*
   * (non-Javadoc)
   * 
   * @see machine.AStarMachine#setUpSearchSpace(algorithm.AStarMapBase,
   * algorithm.AStarGoalBase)
   */
  public void setUpSearchSpace(AStarMapBase<AStarNode> newMap, AStarGoalBase<AStarNode> newGoal)
  {
    if (newMap == null)
      throw new IllegalArgumentException("The map to search may not be null.");
    if (newGoal == null)
      throw new IllegalArgumentException("The goal object may not be null.");
    map = newMap;
    goal = newGoal;
    goal.setNodeStorage(openList);
  }
}
