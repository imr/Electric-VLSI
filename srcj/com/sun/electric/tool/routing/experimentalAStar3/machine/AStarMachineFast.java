/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AStarMachineFast.java
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
import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarGoalBase;
import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarMapBase;
import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarMapVisitorBase;
import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarNode;
import com.sun.electric.tool.routing.experimentalAStar3.memorymanager.ObjectPool;
import com.sun.electric.tool.routing.experimentalAStar3.storage.AStarClosedListReferencing;
import com.sun.electric.tool.routing.experimentalAStar3.storage.AStarOpenListCheapList;
import com.sun.electric.tool.routing.experimentalAStar3.storage.AStarOpenListPriorityQueue2;

/**
 * @author Christian Harnisch
 * 
 */
public class AStarMachineFast implements AStarMachine<AStarNode>, AStarMapVisitorBase<AStarNode>
{
  private ObjectPool<AStarNode> nodePool;

  private AStarOpenListCheapList<AStarNode> openList;
  //private AStarOpenListPriorityQueue2 openList;

  private AStarClosedListReferencing<AStarNode> closedList;

  private AStarMapBase<AStarNode> map = null;

  private AStarGoalBase<AStarNode> goal = null;

  public AStarMachineFast(ObjectPool<AStarNode> nodePool)
  {
    this.nodePool = nodePool;
    this.openList = new AStarOpenListCheapList<AStarNode>();
    //this.openList = new AStarOpenListPriorityQueue2();
    this.closedList = new AStarClosedListReferencing<AStarNode>();
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
    return AStar.findPath(openList, closedList, map, this.goal, nodePool, this);
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
    openList.setMap(map);
    closedList.setMap(map);
    goal = newGoal;
    goal.setNodeStorage(openList);
  }

  public void visitNeighbour(AStarNode origin, int x, int y, int z)
  {
    int costFromStart = origin.getCostFromStart() + goal.getNodeCost(origin, x, y, z);
    int costToGoal = goal.distanceToGoal(x, y, z);
    int totalCost = costFromStart + costToGoal;

    // Check if on open/closed list
    // If yes, check if new path is more efficient (lower f value)
    // and update path if so.
    AStarNode foundNode = openList.findOpenNode(x, y, z);
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
      foundNode.initialize(origin, costFromStart, costToGoal, totalCost, x, y, z);
      openList.addNodeToOpenList(foundNode);
    }
  }
}
