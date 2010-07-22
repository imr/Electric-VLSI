/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AStarRegionMachine.java
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
import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarRegionNode;
import com.sun.electric.tool.routing.experimentalAStar3.memorymanager.ObjectPool;
import com.sun.electric.tool.routing.experimentalAStar3.storage.AStarClosedListReferencing;
import com.sun.electric.tool.routing.experimentalAStar3.storage.AStarOpenListCheapList;

/**
 * @author Christian Harnisch
 * 
 */
public class AStarRegionMachine implements AStarMachine<AStarRegionNode>, AStarMapVisitorBase<AStarRegionNode>
{
  private ObjectPool<AStarRegionNode> nodePool;

  private AStarOpenListCheapList<AStarRegionNode> openList;

  private AStarClosedListReferencing<AStarRegionNode> closedList;

  private AStarMapBase<AStarRegionNode> map = null;

  private AStarGoalBase<AStarRegionNode> goal = null;

  public AStarRegionMachine(ObjectPool<AStarRegionNode> nodePool)
  {
    this.nodePool = nodePool;
    this.openList = new AStarOpenListCheapList<AStarRegionNode>();
    this.closedList = new AStarClosedListReferencing<AStarRegionNode>();
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.sun.electric.tool.routing.astar.t3.machine.AStarMachine#findPath(int,
   * int, int, int, int, int)
   */
  public List<AStarRegionNode> findPath(int startX, int startY, int startZ, int goalX, int goalY, int goalZ)
  {
    if (map == null)
      throw new IllegalStateException("The searched map must be specified before path search can be done.");
    if (this.goal == null)
      throw new IllegalStateException("The goal object must be specified before path search can be done.");

    AStarRegionNode startRegion = map.nodeAt(startX, startY, startZ);
    AStarRegionNode goalRegion = map.nodeAt(goalX, goalY, goalZ);

    assert (startRegion != null);
    assert (goalRegion != null);

    this.goal.setGoalNode(goalRegion);
    int goalDistance = this.goal.distanceToGoal(startX, startY, startZ);
    goalRegion.initialize(null, goalDistance, 0, goalDistance, goalX, goalY, goalZ);

    startRegion.initialize(null, 0, goalDistance, goalDistance, startX, startY, startZ);
    // openList.clearOpenList();
    // closedList.clearClosedList();
    openList.addNodeToOpenList(startRegion);
    return AStar.findPath(openList, closedList, map, this.goal, nodePool, this);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.sun.electric.tool.routing.astar.t3.machine.AStarMachine#setUpSearchSpace
   * (com.sun.electric.tool.routing.astar.t3.algorithm.AStarMapBase,
   * com.sun.electric.tool.routing.astar.t3.algorithm.AStarGoalBase)
   */
  public void setUpSearchSpace(AStarMapBase<AStarRegionNode> newMap, AStarGoalBase<AStarRegionNode> newGoal)
  {
    if (newMap == null)
      throw new IllegalArgumentException("The map to search may not be null.");
    if (newGoal == null)
      throw new IllegalArgumentException("The goal object may not be null.");
    this.map = newMap;
    openList.setMap(map);
    closedList.setMap(map);
    this.goal = newGoal;
    this.goal.setNodeStorage(this.openList);
  }

  /**
   * Searches are done in a bounding rectangle around start and goal region, and
   * on a limited number of layers.<br>
   * By default, this rectangle and the number of layers is minimal. With
   * setting the margin width to non-zero, an additional margin of regions and
   * layers is used for search.<br>
   * It is also possible to define a minimum number of additional layers. This
   * is usefol to ensure that routing may switch to other layers, even when
   * start and end node are located on the same.
   * 
   * @param marginWidth The new margin width.
   * @param minimumLayerMargin The new number of additional layers to use.
   */
  /*
   * public void setMarginWidth(int marginWidth, int minimumLayerMargin) { if
   * (marginWidth < 0) throw new
   * IllegalArgumentException("Margin width must be greater or equal 0, but was "
   * + marginWidth); if (minimumLayerMargin < 0) throw new
   * IllegalArgumentException
   * ("Minimum layer margin must be greater or equal 0, but was " +
   * minimumLayerMargin); }
   */

  /*
   * (non-Javadoc)
   * 
   * @seecom.sun.electric.tool.routing.astar.t3.algorithm.AStarMapVisitorBase#
   * visitNeighbour
   * (com.sun.electric.tool.routing.astar.t3.algorithm.AStarNodeBase, int, int,
   * int)
   */
  public void visitNeighbour(AStarRegionNode origin, int x, int y, int z)
  {
    // If there's no capacity into this direction, don't visit.
    if (origin.getX() == x)
      if ((!origin.isTerminalRegion() && origin.getVerticalCapacity() == 0)
          || (!map.nodeAt(x, y, z).isTerminalRegion() && map.nodeAt(x, y, z).getVerticalCapacity() == 0))
        return;
    if (origin.getY() == y)
      if ((!origin.isTerminalRegion() && origin.getHorizontalCapacity() == 0)
          || (!map.nodeAt(x, y, z).isTerminalRegion() && map.nodeAt(x, y, z).getHorizontalCapacity() == 0))
        return;

    int costFromStart = origin.getCostFromStart() + goal.getNodeCost(origin, x, y, z);
    int costToGoal = goal.distanceToGoal(x, y, z);
    int totalCost = costFromStart + costToGoal;

    // Check if on open/closed list
    // If yes, check if new path is more efficient (lower f value)
    // and update path if so.
    AStarRegionNode foundNode = openList.findOpenNode(x, y, z);
    AStarNode portal = null;
    boolean foundCheaperPath = false;
    if (foundNode != null)
    {
      if (totalCost < foundNode.getTotalCost())
      {
        // If no portal can be found, don't visit.
        portal = this.findPortal(origin, foundNode);// TODO: Do
        // it!
        if (portal != null)
        {
          foundCheaperPath = true;
          openList.removeNodeFromOpenList(foundNode);
        }
      }
    }
    else
    {
      foundNode = closedList.findClosedNode(x, y, z);
      if (foundNode != null)
      {
        if (totalCost < foundNode.getTotalCost())
        {
          // If no portal can be found, don't visit.
          portal = this.findPortal(origin, foundNode); // TODO: Do
          // it!
          if (portal != null)
          {
            foundCheaperPath = true;
            closedList.removeNodeFromClosedList(foundNode);
          }
        }
      }
      else
      {
        foundNode = map.nodeAt(x, y, z);
        // If no portal can be found, don't visit.
        portal = this.findPortal(origin, foundNode);
        if (portal != null)
        {
          // Node not found in open and closed list, thus put a
          // new one on the open list.
          foundCheaperPath = true;

        }
      }
    }

    if (foundCheaperPath)
    {
      foundNode.initialize(origin, costFromStart, costToGoal, totalCost, x, y, z);
      foundNode.setEntryPoint(portal);
      openList.addNodeToOpenList(foundNode);
    }
  }

  public AStarNode findPortal(AStarRegionNode originRegion, AStarRegionNode targetRegion)
  {
    /*
     * TODO: hier werden die entry points gesetzt. Die exit points kann man erst
     * nachdem die komplette regionen-route feststeht, setzen.
     */
    AStarNode result = null;
    int viaX = originRegion.getEntryPoint().getX();
    int viaY = originRegion.getEntryPoint().getY();
    if (originRegion.getZ() != targetRegion.getZ())
    {
      // if (viaX == 0 || viaX == originRegion.width - 1)
      // {
      // int count = 0;
      // while (viaY + count < originRegion.height || viaY - count >= 0)
      // {
      // if (viaY + count < originRegion.height &&
      // !originRegion.isTileBlocked(viaX, viaY + count)
      // && !targetRegion.isTileBlocked(viaX, viaY + count)
      // && originRegion.routingMap.nodeAt(viaX, viaY + count,
      // 0).getTerminalCount() == 0
      // && targetRegion.routingMap.nodeAt(viaX, viaY + count,
      // 0).getTerminalCount() == 0)
      // return targetRegion.routingMap.nodeAt(viaX, viaY + count, 0);
      // if (viaY - count >= 0 && !originRegion.isTileBlocked(viaX, viaY -
      // count)
      // && !targetRegion.isTileBlocked(viaX, viaY - count)
      // && originRegion.routingMap.nodeAt(viaX, viaY - count,
      // 0).getTerminalCount() == 0
      // && targetRegion.routingMap.nodeAt(viaX, viaY - count,
      // 0).getTerminalCount() == 0)
      // return targetRegion.routingMap.nodeAt(viaX, viaY - count, 0);
      // count++;
      // }
      // }
      // else if (viaY == 0 || viaY == originRegion.height - 1)
      // {
      // int count = 0;
      // while (viaX + count < originRegion.width || viaX - count >= 0)
      // {
      // if (viaX + count < originRegion.width &&
      // !originRegion.isTileBlocked(viaX + count, viaY)
      // && !targetRegion.isTileBlocked(viaX + count, viaY)
      // && originRegion.routingMap.nodeAt(viaX + count, viaY,
      // 0).getTerminalCount() == 0
      // && targetRegion.routingMap.nodeAt(viaX + count, viaY,
      // 0).getTerminalCount() == 0)
      // return targetRegion.routingMap.nodeAt(viaX + count, viaY, 0);
      // if (viaX - count >= 0 && !originRegion.isTileBlocked(viaX - count,
      // viaY)
      // && !targetRegion.isTileBlocked(viaX - count, viaY)
      // && originRegion.routingMap.nodeAt(viaX - count, viaY,
      // 0).getTerminalCount() == 0
      // && targetRegion.routingMap.nodeAt(viaX - count, viaY,
      // 0).getTerminalCount() == 0)
      // return targetRegion.routingMap.nodeAt(viaX - count, viaY, 0);
      // count++;
      // }
      // }
      // else
      // somwhere in the middle
      // {
      int count = 0;
      while (viaY + count < originRegion.height || viaY - count >= 0 || viaX + count < originRegion.width || viaX - count >= 0)
      {
        if (viaX + count < originRegion.width && !originRegion.isTileBlocked(viaX + count, viaY)
            && !targetRegion.isTileBlocked(viaX + count, viaY)
            && originRegion.routingMap.nodeAt(viaX + count, viaY, 0).getTerminalCount() == 0
            && targetRegion.routingMap.nodeAt(viaX + count, viaY, 0).getTerminalCount() == 0)
          return targetRegion.routingMap.nodeAt(viaX + count, viaY, 0);
        if (viaX - count >= 0 && !originRegion.isTileBlocked(viaX - count, viaY)
            && !targetRegion.isTileBlocked(viaX - count, viaY)
            && originRegion.routingMap.nodeAt(viaX - count, viaY, 0).getTerminalCount() == 0
            && targetRegion.routingMap.nodeAt(viaX - count, viaY, 0).getTerminalCount() == 0)
          return targetRegion.routingMap.nodeAt(viaX - count, viaY, 0);
        if (viaY + count < originRegion.height && !originRegion.isTileBlocked(viaX, viaY + count)
            && !targetRegion.isTileBlocked(viaX, viaY + count)
            && originRegion.routingMap.nodeAt(viaX, viaY + count, 0).getTerminalCount() == 0
            && targetRegion.routingMap.nodeAt(viaX, viaY + count, 0).getTerminalCount() == 0)
          return targetRegion.routingMap.nodeAt(viaX, viaY + count, 0);
        if (viaY - count >= 0 && !originRegion.isTileBlocked(viaX, viaY - count)
            && !targetRegion.isTileBlocked(viaX, viaY - count)
            && originRegion.routingMap.nodeAt(viaX, viaY - count, 0).getTerminalCount() == 0
            && targetRegion.routingMap.nodeAt(viaX, viaY - count, 0).getTerminalCount() == 0)
          return targetRegion.routingMap.nodeAt(viaX, viaY - count, 0);
        count++;
      }
      // }
      return null;
    }

    int ox = originRegion.getX();
    int fx = targetRegion.getX();
    int oy = originRegion.getY();
    int fy = targetRegion.getY();

    // if foundNode is to the east
    if (ox == fx - 1)
    {
      int targetX = 0;
      int originX = originRegion.width - 1;
      int y = originRegion.getEntryPoint().getY();
      int count = 0;
      while (y + count < originRegion.height || y - count >= 0)
      {
        if (y + count < originRegion.height && originRegion.isPortalWithPath(originX, y + count, false)
            && targetRegion.isPortalWithPath(targetX, y + count, false))
          return targetRegion.getMap(false).nodeAt(targetX, y + count, 0);
        if (y - count >= 0 && originRegion.isPortalWithPath(originX, y - count, false)
            && targetRegion.isPortalWithPath(targetX, y - count, false))
          return targetRegion.getMap(false).nodeAt(targetX, y - count, 0);
        count++;
      }
    }
    // if foundNode is to the west
    else if (ox == fx + 1)
    {
      int targetX = originRegion.width - 1;
      int originX = 0;
      int y = originRegion.getEntryPoint().getY();
      int count = 0;
      while (y + count < originRegion.height || y - count >= 0)
      {
        if (y + count < originRegion.height && originRegion.isPortalWithPath(originX, y + count, false)
            && targetRegion.isPortalWithPath(targetX, y + count, false))
          return targetRegion.getMap(false).nodeAt(targetX, y + count, 0);
        if (y - count >= 0 && originRegion.isPortalWithPath(originX, y - count, false)
            && targetRegion.isPortalWithPath(targetX, y - count, false))
          return targetRegion.getMap(false).nodeAt(targetX, y - count, 0);
        count++;
      }
    }
    // if foundNode is to the north
    else if (oy == fy + 1)
    {
      int targetY = originRegion.height - 1;
      int originY = 0;
      int x = originRegion.getEntryPoint().getX();
      ;
      int count = 0;

      while (x + count < originRegion.width || x - count >= 0)
      {
        if (x + count < originRegion.width && originRegion.isPortalWithPath(x + count, originY, true)
            && targetRegion.isPortalWithPath(x + count, targetY, true))
          return targetRegion.getMap(false).nodeAt(x + count, targetY, 0);
        if (x - count >= 0 && originRegion.isPortalWithPath(x - count, originY, true)
            && targetRegion.isPortalWithPath(x - count, targetY, true))
          return targetRegion.getMap(false).nodeAt(x - count, targetY, 0);
        count++;
      }
    }
    // if foundNode is to the south
    else if (oy == fy - 1)
    {
      int targetY = 0;
      int originY = originRegion.height - 1;
      int x = originRegion.getEntryPoint().getX();
      ;
      int count = 0;
      while (x + count < originRegion.width || x - count >= 0)
      {
        if (x + count < originRegion.width && originRegion.isPortalWithPath(x + count, originY, true)
            && targetRegion.isPortalWithPath(x + count, targetY, true))
          return targetRegion.getMap(false).nodeAt(x + count, targetY, 0);
        if (x - count >= 0 && originRegion.isPortalWithPath(x - count, originY, true)
            && targetRegion.isPortalWithPath(x - count, targetY, true))
          return targetRegion.getMap(false).nodeAt(x - count, targetY, 0);
        count++;
      }
    }
    return result;
  }
}
