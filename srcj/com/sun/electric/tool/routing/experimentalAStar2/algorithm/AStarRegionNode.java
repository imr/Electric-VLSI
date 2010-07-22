/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AStarRegionNode.java
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

import java.util.ArrayList;
import java.util.HashSet;

import com.sun.electric.tool.routing.experimentalAStar2.concurrency.RoutingMain;
import com.sun.electric.tool.routing.experimentalAStar2.goal.SimpleGoal;
import com.sun.electric.tool.routing.experimentalAStar2.machine.AStarMachine;
import com.sun.electric.tool.routing.experimentalAStar2.machine.AStarMachineFast;
import com.sun.electric.tool.routing.experimentalAStar2.map.FieldMap;
import com.sun.electric.tool.routing.experimentalAStar2.memorymanager.AStarNodeObjectPool;

/**
 * An AStarRegionNode represents one layer of one region in the global region
 * map.
 */
public class AStarRegionNode extends AStarNodeBase<AStarRegionNode>
{
  /**
   * is used for checking blockages and giving the router a clean map to route
   * on.
   */
  public FieldMap<AStarNode> routingMap;

  /**
   * this map contains all nodes that are created during capacity calculation.
   * there is a seperate map for each horizontal and vertical calculation. this
   * map should not be asked for blockages as they can change during runtime,
   * but won't be updated here after initializing this RegionNode.
   */
  private FieldMap<AStarNode> hMap;

  private FieldMap<AStarNode> vMap;

  protected int hCap;

  protected int vCap;

  private ArrayList<Integer> hPaths = new ArrayList<Integer>();

  private ArrayList<Integer> vPaths = new ArrayList<Integer>();

  private HashSet<AStarNode> hPortals = new HashSet<AStarNode>();

  public HashSet<AStarNode> vPortals = new HashSet<AStarNode>();

  public int height;

  public int width;

  /* Dirty flags: Tells if the capacity of the region should be recalculated. */
  private boolean horizontalDirty = true;

  private boolean verticalDirty = true;

  private AStarNode entryPoint;

  private AStarNode exitPoint;

  /**
   * needed for special case: start/endpoint is within this region, which has no
   * capacity (it happens!). Then the capacity must NOT be asked to be !=0
   * (which usually is a required condition to walk in a specific direction).
   */
  private boolean isStartRegion = false;

  private boolean isGoalRegion = false;

  public void setAsStartRegion(boolean isStartRegion)
  {
    this.isStartRegion = isStartRegion;
  }

  public void setAsGoalRegion(boolean isGoalRegion)
  {
    this.isGoalRegion = isGoalRegion;
  }

  public boolean isTerminalRegion()
  {
    return this.isStartRegion || this.isGoalRegion;
  }

  public AStarRegionNode()
  {
  }

  /**
   * This class expects that (0,0) is in the upper left corner of the map.<br/>
   * <br/>
   * The capacity is calculated from x = 0 to x = <code>width</code>-1 and y = 0
   * to y = <code>height</code>-1. <br/>
   * <br/>
   * Since we calculate two different capacities (horizontal and vertical), the
   * map cannot contain the found paths directly (because then it would fail to
   * calculate the second capacity), so it offers them in two lists: <br/>
   * Both lists (<code>hPaths</code> and <code>vPaths</code>) contain lists of
   * <code>AStarNodes</code> connected by their <code>origin</code>-pointer
   * 
   * @param map The sub-map of the cell this AStarRegionNode represents a layer
   *          from.
   * @param layer The layer this AStarRegionNode represents
   * @param height
   * @param width
   */
  public AStarRegionNode(FieldMap<AStarNode> map, int width, int height)
  {
    this.routingMap = map;
    this.height = height;
    this.width = width;
  }

  public AStarRegionNode(FieldMap<AStarNode> map, int width, int height, int x, int y, int z)
  {
    this.routingMap = map;
    this.height = height;
    this.width = width;
    this.x = x;
    this.y = y;
    this.z = z;
  }

  /**
   * Returns the number of wires that can successfully be routed through this
   * section of the cell horizontally.
   * 
   * @return Number of possible horizontal wires.
   */
  public int getHorizontalCapacity()
  {
    if (this.horizontalDirty)
    {
      this.hCap = calculateCapacity(false);
      fillUpCapacityPathNumbers(false);
      this.horizontalDirty = false;
    }
    return this.hCap;
  }

  /**
   * Returns the number of wires that can successfully be routed through this
   * section of the cell vertically.
   * 
   * @return Number of possible vertical wires.
   */
  public int getVerticalCapacity()
  {
    if (this.verticalDirty)
    {
      this.vCap = calculateCapacity(true);
      fillUpCapacityPathNumbers(true);
      this.verticalDirty = false;
    }
    return this.vCap;
  }

  /**
   * Calculates and returns the number of wires that can successfully be routed
   * through this section of the cell, horizontally or vertically.
   * 
   * @param calcVertical <code>true</code> if the vertical capacity should be
   *          calculated, <code>false</code> otherwise.
   * @return Number of possible wires in the given direction.
   */
  private int calculateCapacity(boolean calcVertical)
  {
    FieldMap<AStarNode> capMap;

    if (calcVertical)
    {
      this.vPaths.clear();
      capMap = this.getMap(true);
    }
    else
    {
      this.hPaths.clear();
      capMap = this.getMap(false);
    }
    byte commingFrom;

    final byte LEFT = 0;

    final byte BOTTOM = 1;

    final byte RIGHT = 2;

    final byte TOP = 3;
    // Start at (0,0) for horizontal, (0,this.height-1) for vertical
    int x = 0;
    int y = calcVertical ? 0 : this.height - 1;
    // find path from left to right
    int k = 0;
    commingFrom = calcVertical ? TOP : LEFT;
    while ((!calcVertical && y > 0) || (calcVertical && x < this.width))
    {
      x = 0;
      y = calcVertical ? 0 : this.height - 1;
      k++;
      if (calcVertical)
        while (routingMap.isTileBlocked(x, y, 0) || capMap.nodeAt(x, y, 0).getVerticalCapacityPathNumber() != 0
            || !isPortal(x, y, true))
        {
          x++;
          if (x >= this.width)
            return k - 1;
        }
      else
        while (routingMap.isTileBlocked(x, y, 0) || capMap.nodeAt(x, y, 0).getHorizontalCapacityPathNumber() != 0
            || !isPortal(x, y, false))
        {
          y--;
          if (y < 0)
            return k - 1;
        }
      AStarNode origin = capMap.nodeAt(x, y, 0);
      if (calcVertical)
        origin.setVerticalCapacityPathNumber(k);
      else
        origin.setHorizontalCapacityPathNumber(k);
      while ((!calcVertical && x < this.width && y >= -1 && (x == 0 || !this.isPortal(x, y, false)))
          || (calcVertical && x < this.width && y <= this.height - 1 && (y == 0 || !this.isPortal(x, y, true))))
      {
        switch (commingFrom)
        {
        case LEFT:
          if (!routingMap.isTileBlocked(x, y + 1, 0))
            if (calcVertical ? capMap.nodeAt(x, y + 1, 0).getVerticalCapacityPathNumber() == 0 : capMap.nodeAt(x, y + 1, 0)
                .getHorizontalCapacityPathNumber() == 0)
            {
              y++;
              AStarNode oldOrigin = origin;
              origin = capMap.nodeAt(x, y, 0);
              origin.initialize(oldOrigin, 0, 0, 0, x, y, 0);
              if (calcVertical)
                origin.setVerticalCapacityPathNumber(k);
              else
                origin.setHorizontalCapacityPathNumber(k);
              commingFrom = TOP;
              break;
            }
          if (!routingMap.isTileBlocked(x + 1, y, 0))
            if (calcVertical ? capMap.nodeAt(x + 1, y, 0).getVerticalCapacityPathNumber() == 0 : capMap.nodeAt(x + 1, y, 0)
                .getHorizontalCapacityPathNumber() == 0)
            {
              x++;
              AStarNode oldOrigin = origin;
              origin = capMap.nodeAt(x, y, 0);
              origin.initialize(oldOrigin, 0, 0, 0, x, y, 0);
              if (calcVertical)
                origin.setVerticalCapacityPathNumber(k);
              else
                origin.setHorizontalCapacityPathNumber(k);
              commingFrom = LEFT;
              break;
            }
          if (!routingMap.isTileBlocked(x, y - 1, 0))
            if (calcVertical ? capMap.nodeAt(x, y - 1, 0).getVerticalCapacityPathNumber() == 0 : capMap.nodeAt(x, y - 1, 0)
                .getHorizontalCapacityPathNumber() == 0)
            {
              y--;
              AStarNode oldOrigin = origin;
              origin = capMap.nodeAt(x, y, 0);
              origin.initialize(oldOrigin, 0, 0, 0, x, y, 0);
              if (calcVertical)
                origin.setVerticalCapacityPathNumber(k);
              else
                origin.setHorizontalCapacityPathNumber(k);
              commingFrom = BOTTOM;
              break;
            }
          x--; // move one step back and start backtracking
          // start moving back with opposite direction
          commingFrom = RIGHT;
          origin = traceBack(capMap.nodeAt(x, y, 0), calcVertical);
          if (origin != null) // another way is possible
          {
            x = origin.getX();
            y = origin.getY();
            if (origin.origin != null)
            { // is valid node, so start
              // from here
              int oX = origin.origin.getX();
              int oY = origin.origin.getY();
              if (oX < x)
                commingFrom = LEFT;
              if (oX > x)
                commingFrom = RIGHT;
              if (oY > y)
                commingFrom = BOTTOM;
              if (oY < y)
                commingFrom = TOP;
            }
            else
              // must be beginning node (0,0) => Start again
              commingFrom = LEFT;
          }
          else
            return k - 1;
          break;
        case BOTTOM:
          if (!routingMap.isTileBlocked(x + 1, y, 0))
            if (calcVertical ? capMap.nodeAt(x + 1, y, 0).getVerticalCapacityPathNumber() == 0 : capMap.nodeAt(x + 1, y, 0)
                .getHorizontalCapacityPathNumber() == 0)
            {
              x++;
              AStarNode oldOrigin = origin;
              origin = capMap.nodeAt(x, y, 0);
              origin.initialize(oldOrigin, 0, 0, 0, x, y, 0);
              if (calcVertical)
                origin.setVerticalCapacityPathNumber(k);
              else
                origin.setHorizontalCapacityPathNumber(k);
              commingFrom = LEFT;
              break;
            }
          if (y == this.height)
            return k - 1;
          if (!routingMap.isTileBlocked(x, y - 1, 0))
            if (calcVertical ? capMap.nodeAt(x, y - 1, 0).getVerticalCapacityPathNumber() == 0 : capMap.nodeAt(x, y - 1, 0)
                .getHorizontalCapacityPathNumber() == 0)
            {
              y--;
              AStarNode oldOrigin = origin;
              origin = capMap.nodeAt(x, y, 0);
              origin.initialize(oldOrigin, 0, 0, 0, x, y, 0);
              if (calcVertical)
                origin.setVerticalCapacityPathNumber(k);
              else
                origin.setHorizontalCapacityPathNumber(k);
              commingFrom = BOTTOM;
              break;
            }
          if (!routingMap.isTileBlocked(x - 1, y, 0))
            if (calcVertical ? capMap.nodeAt(x - 1, y, 0).getVerticalCapacityPathNumber() == 0 : capMap.nodeAt(x - 1, y, 0)
                .getHorizontalCapacityPathNumber() == 0)
            {
              x--;
              AStarNode oldOrigin = origin;
              origin = capMap.nodeAt(x, y, 0);
              origin.initialize(oldOrigin, 0, 0, 0, x, y, 0);
              if (calcVertical)
                origin.setVerticalCapacityPathNumber(k);
              else
                origin.setHorizontalCapacityPathNumber(k);
              commingFrom = RIGHT;
              break;
            }
          y++; // move one step back and start backtracking
          // start moving back with opposite direction
          commingFrom = TOP;
          origin = traceBack(capMap.nodeAt(x, y, 0), calcVertical);
          if (origin != null) // another way is possible
          {
            x = origin.getX();
            y = origin.getY();
            if (origin.origin != null)
            { // is valid node, so start
              // from here
              int oX = origin.origin.getX();
              int oY = origin.origin.getY();
              if (oX < x)
                commingFrom = LEFT;
              if (oX > x)
                commingFrom = RIGHT;
              if (oY > y)
                commingFrom = BOTTOM;
              if (oY < y)
                commingFrom = TOP;
            }
            else
              // must be beginning node (0,0) => Start again
              commingFrom = LEFT;
          }
          else
            return k - 1;
          break;
        case RIGHT:
          if (!routingMap.isTileBlocked(x, y - 1, 0))
            if (calcVertical ? capMap.nodeAt(x, y - 1, 0).getVerticalCapacityPathNumber() == 0 : capMap.nodeAt(x, y - 1, 0)
                .getHorizontalCapacityPathNumber() == 0)
            {
              y--;
              AStarNode oldOrigin = origin;
              origin = capMap.nodeAt(x, y, 0);
              origin.initialize(oldOrigin, 0, 0, 0, x, y, 0);
              if (calcVertical)
                origin.setVerticalCapacityPathNumber(k);
              else
                origin.setHorizontalCapacityPathNumber(k);
              commingFrom = BOTTOM;
              break;
            }
          if (!routingMap.isTileBlocked(x - 1, y, 0))
            if (calcVertical ? capMap.nodeAt(x - 1, y, 0).getVerticalCapacityPathNumber() == 0 : capMap.nodeAt(x - 1, y, 0)
                .getHorizontalCapacityPathNumber() == 0)
            {
              x--;
              AStarNode oldOrigin = origin;
              origin = capMap.nodeAt(x, y, 0);
              origin.initialize(oldOrigin, 0, 0, 0, x, y, 0);
              if (calcVertical)
                origin.setVerticalCapacityPathNumber(k);
              else
                origin.setHorizontalCapacityPathNumber(k);
              commingFrom = RIGHT;
              break;
            }
          if (!routingMap.isTileBlocked(x, y + 1, 0))
            if (calcVertical ? capMap.nodeAt(x, y + 1, 0).getVerticalCapacityPathNumber() == 0 : capMap.nodeAt(x, y + 1, 0)
                .getHorizontalCapacityPathNumber() == 0)
            {
              y++;
              AStarNode oldOrigin = origin;
              origin = capMap.nodeAt(x, y, 0);
              origin.initialize(oldOrigin, 0, 0, 0, x, y, 0);
              if (calcVertical)
                origin.setVerticalCapacityPathNumber(k);
              else
                origin.setHorizontalCapacityPathNumber(k);
              commingFrom = TOP;
              break;
            }
          x++; // move one step back and start backtracking
          // start moving back with opposite direction
          commingFrom = LEFT;
          origin = traceBack(capMap.nodeAt(x, y, 0), calcVertical);
          if (origin != null) // another way is possible
          {
            x = origin.getX();
            y = origin.getY();
            if (origin.origin != null)
            { // is valid node, so start
              // from here
              int oX = origin.origin.getX();
              int oY = origin.origin.getY();
              if (oX < x)
                commingFrom = LEFT;
              if (oX > x)
                commingFrom = RIGHT;
              if (oY > y)
                commingFrom = BOTTOM;
              if (oY < y)
                commingFrom = TOP;
            }
            else
              // must be beginning node (0,0) => Start again
              commingFrom = LEFT;
          }
          else
            return k - 1;
          break;
        case TOP:
          if (!routingMap.isTileBlocked(x - 1, y, 0))
            if (calcVertical ? capMap.nodeAt(x - 1, y, 0).getVerticalCapacityPathNumber() == 0 : capMap.nodeAt(x - 1, y, 0)
                .getHorizontalCapacityPathNumber() == 0)
            {
              x--;
              AStarNode oldOrigin = origin;
              origin = capMap.nodeAt(x, y, 0);
              origin.initialize(oldOrigin, 0, 0, 0, x, y, 0);
              if (calcVertical)
                origin.setVerticalCapacityPathNumber(k);
              else
                origin.setHorizontalCapacityPathNumber(k);
              commingFrom = RIGHT;
              break;
            }
          if (!routingMap.isTileBlocked(x, y + 1, 0))
            if (calcVertical ? capMap.nodeAt(x, y + 1, 0).getVerticalCapacityPathNumber() == 0 : capMap.nodeAt(x, y + 1, 0)
                .getHorizontalCapacityPathNumber() == 0)
            {
              y++;
              AStarNode oldOrigin = origin;
              origin = capMap.nodeAt(x, y, 0);
              origin.initialize(oldOrigin, 0, 0, 0, x, y, 0);
              if (calcVertical)
                origin.setVerticalCapacityPathNumber(k);
              else
                origin.setHorizontalCapacityPathNumber(k);
              commingFrom = TOP;
              break;
            }
          if (!routingMap.isTileBlocked(x + 1, y, 0))
            if (calcVertical ? capMap.nodeAt(x + 1, y, 0).getVerticalCapacityPathNumber() == 0 : capMap.nodeAt(x + 1, y, 0)
                .getHorizontalCapacityPathNumber() == 0)
            {
              x++;
              AStarNode oldOrigin = origin;
              origin = capMap.nodeAt(x, y, 0);
              origin.initialize(oldOrigin, 0, 0, 0, x, y, 0);
              if (calcVertical)
                origin.setVerticalCapacityPathNumber(k);
              else
                origin.setHorizontalCapacityPathNumber(k);
              commingFrom = LEFT;
              break;
            }
          y--; // move one step back and start backtracking
          // start moving back with opposite direction
          commingFrom = BOTTOM;
          origin = traceBack(capMap.nodeAt(x, y, 0), calcVertical);
          if (origin != null) // another way is possible
          {
            x = origin.getX();
            y = origin.getY();
            if (origin.origin != null)
            { // is valid node, so start
              // from here
              int oX = origin.origin.getX();
              int oY = origin.origin.getY();
              if (oX < x)
                commingFrom = LEFT;
              if (oX > x)
                commingFrom = RIGHT;
              if (oY > y)
                commingFrom = BOTTOM;
              if (oY < y)
                commingFrom = TOP;
            }
            else
              // must be beginning node (0,0) => Start again
              commingFrom = LEFT;
          }
          else
            return k - 1;
          break;
        }
      }

      // add path to path list
      // List<AStarNode> path = new ArrayList<AStarNode>();
      // AStarNode pathNode = origin;
      // while (pathNode != null)
      // {
      // path.add(pathNode);
      // pathNode = pathNode.origin;
      // }
      if (calcVertical)
        this.vPaths.add(k);
      else
        this.hPaths.add(k);
    }
    return k;
  }

  private void fillUpCapacityPathNumbers(boolean calcVertical)
  {
    if (calcVertical)
    {
      // fill up path numbers in vertical portals
      for (int x = 0; x < this.width; x++)
      {
        int pathNumber = 0;

        // search from top to bottom
        int y = 0;
        if (isPortal(x, y, true) && this.getMap(true).nodeAt(x, y, 0).getVerticalCapacityPathNumber() == 0)
        {
          while (y < this.height)
          {
            if (this.routingMap.isTileBlocked(x, y, 0))
              break;
            pathNumber = this.getMap(true).nodeAt(x, y, 0).getVerticalCapacityPathNumber();
            if (pathNumber != 0)
              break;
            y++;
          }
          if (y != 0 && y != this.height && !this.routingMap.isTileBlocked(x, y, 0))
          {
            this.getMap(true).nodeAt(x, 0, 0).setVerticalCapacityPathNumber(pathNumber);
          }
        }

        // search from bottom to top
        y = this.height - 1;
        if (isPortal(x, y, true) && this.getMap(true).nodeAt(x, y, 0).getVerticalCapacityPathNumber() == 0)
        {
          while (y >= 0)
          {
            if (this.routingMap.isTileBlocked(x, y, 0))
              break;
            pathNumber = this.getMap(true).nodeAt(x, y, 0).getVerticalCapacityPathNumber();
            if (pathNumber != 0)
              break;
            y--;
          }
          if (y != this.height - 1 && y != -1 && !this.routingMap.isTileBlocked(x, y, 0))
          {
            this.getMap(true).nodeAt(x, this.height - 1, 0).setVerticalCapacityPathNumber(pathNumber);
          }
        }
      }
    }
    else
    {
      // fill up path numbers in horizontal portals
      for (int y = 0; y < this.height; y++)
      {
        int pathNumber = 0;
        // search from west to east
        int x = 0;
        if (isPortal(x, y, true) && this.getMap(false).nodeAt(x, y, 0).getHorizontalCapacityPathNumber() == 0)
        {
          while (x < this.width)
          {
            if (this.routingMap.isTileBlocked(x, y, 0))
              break;
            pathNumber = this.getMap(false).nodeAt(x, y, 0).getHorizontalCapacityPathNumber();
            if (pathNumber != 0)
              break;
            x++;
          }
          if (x != 0 && x != this.width && !this.routingMap.isTileBlocked(x, y, 0))
          {
            this.getMap(false).nodeAt(0, y, 0).setHorizontalCapacityPathNumber(pathNumber);
          }
        }

        // search from east to west
        x = this.width - 1;
        if (isPortal(x, y, true) && this.getMap(false).nodeAt(x, y, 0).getHorizontalCapacityPathNumber() == 0)
        {
          while (x >= 0)
          {
            if (this.routingMap.isTileBlocked(x, y, 0))
              break;
            pathNumber = this.getMap(false).nodeAt(x, y, 0).getHorizontalCapacityPathNumber();
            if (pathNumber != 0)
              break;
            x--;
          }
          if (x != this.width - 1 && x != -1 && !this.routingMap.isTileBlocked(x, y, 0))
          {
            this.getMap(false).nodeAt(this.width - 1, y, 0).setHorizontalCapacityPathNumber(pathNumber);
          }
        }
      }
    }
  }

  protected boolean isPortal(int x, int y, boolean vertical)
  {
    if (this.routingMap.isTileBlocked(x, y, 0))
    {
      AStarNode nodeAt = this.routingMap.nodeAt(x, y, 0);
      this.vPortals.remove(nodeAt);
      this.hPortals.remove(nodeAt);
      return false;
    }
    if (vertical)
      return this.vPortals.contains(routingMap.nodeAt(x, y, 0));
    return this.hPortals.contains(routingMap.nodeAt(x, y, 0));
  }

  public void setPortal(int x, int y, boolean vertical)
  {
    if (vertical)
      this.vPortals.add(routingMap.nodeAt(x, y, 0));
    else
      this.hPortals.add(routingMap.nodeAt(x, y, 0));
  }

  public boolean isPortalWithPath(int x, int y, boolean calcVertical)
  {
    if (this.routingMap.isTileBlocked(x, y, 0))
    {
      AStarNode nodeAt = this.routingMap.nodeAt(x, y, 0);
      this.vPortals.remove(nodeAt);
      this.hPortals.remove(nodeAt);
      return false;
    }
    if (!this.isPortal(x, y, calcVertical))
      return false;
    if (this.isTerminalRegion())
    {
      AStarNode to = this.isGoalRegion ? this.getExitPoint() : this.getEntryPoint();
      AStarMachine<AStarNode> machine = new AStarMachineFast(new AStarNodeObjectPool());
      AStarGoalBase<AStarNode> goal = new SimpleGoal();
      goal.setMaximumRevolutions(RoutingMain.MAX_REVOLUTIONS);
      machine.setUpSearchSpace(this.routingMap, goal);
      if (machine.findPath(x, y, 0, to.getX(), to.getY(), 0) != null)
        return true;
      // System.out.println("Could not find path to portal in TerminalRegion ("
      // + this.getX() + "/" + this.getY() + ") from (" + x
      // + "," + y + ") to (" + to.getX() + "," + to.getY() + ")");
      // this.print();
    }
    if (calcVertical)
      return this.vPaths.contains(this.getMap(true).nodeAt(x, y, 0).getVerticalCapacityPathNumber());
    return this.hPaths.contains(this.getMap(false).nodeAt(x, y, 0).getHorizontalCapacityPathNumber());
  }

  private AStarNode traceBack(AStarNode currentNode, boolean calcVertical)
  {
    int x;
    int y;
    AStarNode result = currentNode;
    if (calcVertical)
    {
      FieldMap<AStarNode> map = this.getMap(true);
      while (result != null)
      {
        x = result.getX();
        y = result.getY();
        if ((this.routingMap.isTileBlocked(x, y + 1, 0) || map.nodeAt(x, y + 1, 0).getVerticalCapacityPathNumber() > 0)
            && (this.routingMap.isTileBlocked(x, y - 1, 0) || map.nodeAt(x, y - 1, 0).getVerticalCapacityPathNumber() > 0)
            && (this.routingMap.isTileBlocked(x + 1, y, 0) || map.nodeAt(x + 1, y, 0).getVerticalCapacityPathNumber() > 0)
            && (this.routingMap.isTileBlocked(x - 1, y, 0) || map.nodeAt(x - 1, y, 0).getVerticalCapacityPathNumber() > 0))
        {
          result = result.origin; // might be null if first Node of
          // path =>
          // still
          // correct
        }
        else
          break;
      }
    }
    else
    {
      FieldMap<AStarNode> map = this.getMap(false);
      while (result != null)
      {
        x = result.getX();
        y = result.getY();
        if ((this.routingMap.isTileBlocked(x, y + 1, 0) || map.nodeAt(x, y + 1, 0).getHorizontalCapacityPathNumber() > 0)
            && (this.routingMap.isTileBlocked(x, y - 1, 0) || map.nodeAt(x, y - 1, 0).getHorizontalCapacityPathNumber() > 0)
            && (this.routingMap.isTileBlocked(x + 1, y, 0) || map.nodeAt(x + 1, y, 0).getHorizontalCapacityPathNumber() > 0)
            && (this.routingMap.isTileBlocked(x - 1, y, 0) || map.nodeAt(x - 1, y, 0).getHorizontalCapacityPathNumber() > 0))
        {
          result = result.origin; // might be null if first Node of
          // path =>
          // still
          // correct
        }
        else
          break;
      }
    }
    return result;
  }

  /**
   * removes a path from the path list. Should be called when node was actually
   * used by detailed routing.
   * 
   * @param pathNumber
   * @param vertical
   */
  public void removePathForNode(int x, int y)
  {
    if (this.vPaths.remove(Integer.valueOf(this.getMap(true).nodeAt(x, y, 0).getVerticalCapacityPathNumber())))
      this.vCap--;
    if (this.hPaths.remove(Integer.valueOf(this.getMap(false).nodeAt(x, y, 0).getHorizontalCapacityPathNumber())))
      this.hCap--;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  public int compareTo(AStarRegionNode o)
  {
    return this.getTotalCost() - o.getTotalCost();
  }

  public FieldMap<AStarNode> getMap(boolean vertical)
  {
    if (vertical)
      if (this.vMap == null)
        return this.vMap = (FieldMap<AStarNode>) this.routingMap.clone();
      else
        return this.vMap;
    else if (this.hMap == null)
      return this.hMap = (FieldMap<AStarNode>) this.routingMap.clone();
    else
      return this.hMap;
  }

  public boolean isTileBlocked(int x, int y)
  {
    return this.routingMap.isTileBlocked(x, y, 0);
  }

  public AStarNode getEntryPoint()
  {
    return this.entryPoint;
  }

  public void setEntryPoint(AStarNode node)
  {
    this.entryPoint = node;
  }

  public AStarNode getExitPoint()
  {
    return this.exitPoint;
  }

  public void setExitPoint(AStarNode node)
  {
    this.exitPoint = node;
  }

  public void print()
  {
    String string = "";
    for (int y = 0; y < this.height; y++)
    {
      for (int x = 0; x < this.width; x++)
      {
        String value = "";
        if (this.getEntryPoint() != null && this.getEntryPoint().equals(this.getMap(true).nodeAt(x, y, 0)))
          value = "E";
        else if (this.isTileBlocked(x, y))
          value = "X";
        else
          value = "_";
        if (this.isPortal(x, y, true) || this.isPortal(x, y, false))
          value = "P" + value;
        else
          value += "_";
        string += "[" + value + "]";
      }
      string += "\n";
    }
    System.out.println(string);
  }
}
