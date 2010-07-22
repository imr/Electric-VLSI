/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AStarNodeBase.java
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
package com.sun.electric.tool.routing.experimentalAStar3.algorithm;

/**
 * @author Christian Harnisch
 * 
 */
public abstract class AStarNodeBase<T> implements Comparable<T>
{
  // Use by the PriorityQueue
  public int pq_i = 0;
  
  public T origin;

  protected int costFromStart;

  protected int costToGoal;

  protected int totalCost;

  protected int x, y, z;

  public String toString()
  {
    return "(" + x + "," + y + ")";
  }

  /**
   * Initialises a node for usage.
   * 
   * @param origin Node the node originated from.
   * @param costFromStart Movement costs from start position to the position
   *          associated with the node.
   * @param costToGoal Estimated cost from the position associated with the node
   *          to goal position.
   * @param totalCost Total cost from start to goal, moving via the node's
   *          position.
   * @param x X-position the node corresponds to.
   * @param y Y-position the node corresponds to.
   * @param z Z-position the node corresponds to.
   */
  public void initialize(T origin, int costFromStart, int costToGoal, int totalCost, int x, int y, int z)
  {
    this.origin = origin;
    this.costFromStart = costFromStart;
    this.costToGoal = costToGoal;
    this.totalCost = totalCost;
    this.x = x;
    this.y = y;
    this.z = z;
    this.state = 0;
    this.pq_i = 0;
  }

  /**
   * When the node is part of a path of nodes, <code>getOrigin</code> returns
   * the preceding node in the path, if available.
   * 
   * @return The preceding node, or <code>null</code> if the node is the first
   *         of the path.
   */
  // public abstract T getOrigin();

  /**
   * Sets the node's preceding node to the given one.
   * 
   * @param newOrigin New preceding node.
   */
  // public abstract void setOrigin(T newOrigin);

  /**
   * Returns the cost of the partial path from start node to the node.
   * 
   * @return Cost from start node.
   */
  public int getCostFromStart()
  {
    return costFromStart;
  }

  /**
   * Returns the probably inaccurate cost estimation for the partial part from
   * the node to the goal node.
   * 
   * @return Cost to goal node.
   */
  public int getCostToGoal()
  {
    return costToGoal;
  }

  /**
   * Returns the node's total cost.<br>
   * A node's total cost is the sum of the cost from the start position to the
   * node and the estimated cost from the node to the goal position.
   * 
   * @return The total cost of the node.
   */
  public int getTotalCost()
  {
    return totalCost;
  }

  /**
   * Returns the X-position of the node.
   * 
   * @return X-position.
   */
  public int getX()
  {
    return x;
  }

  /**
   * Returns the Y-position of the node.
   * 
   * @return Y-position.
   */
  public int getY()
  {
    return y;
  }

  /**
   * Returns the Z-position of the node.
   * 
   * @return Z-position.
   */
  public int getZ()
  {
    return z;
  }

  @Override
  public boolean equals(Object o)
  {
    if (o instanceof AStarNode)
    {
      return this.x == ((AStarNode) o).getX() && this.y == ((AStarNode) o).getY() && this.z == ((AStarNode) o).getZ();
    }
    return super.equals(o);
  }

  /*
   * This state stuff should probably be in its own class or interface. But
   * since we've decided to use this for all nodes, it is included here.
   */

  /**
   * 0 means in no list 1 means in openList 2 means in closedList
   */
  protected byte state = 0;

  /**
   * Marks the node as contained in the open list.
   */
  public void markAsOpen()
  {
    this.state = 1;
  }

  /**
   * Marks the node as contained in the closed list.
   */
  public void markAsClosed()
  {
    this.state = 2;
  }

  /**
   * Marks the node as contained in no list, neither in open list, nor in closed
   * list.
   */
  public void markAsNoList()
  {
    this.state = 0;
  }

  /**
   * Returns if the node is contained in the closed list.
   * 
   * @return <code>true</code> if the node is in the closed list,
   *         <code>false</code> otherwise.
   */
  public boolean isClosed()
  {
    return this.state == 2;
  }

  /**
   * Returns if the node is contained in the open list.
   * 
   * @return <code>true</code> if the node is in the open list,
   *         <code>false</code> otherwise.
   */
  public boolean isOpen()
  {
    return this.state == 1;
  }
}