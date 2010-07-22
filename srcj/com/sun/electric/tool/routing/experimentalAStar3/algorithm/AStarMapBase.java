/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AStarMapBase.java
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

import java.util.List;

import com.sun.electric.tool.routing.experimentalAStar3.memorymanager.ObjectPool;

/**
 * The search space the A* algorithm operates on. It consists of individual
 * nodes, which can be blocked or free. From one node, it is possible to move to
 * its non-blocked neighbours, at a specified cost. No assumptions are made
 * about map size or allowed range of coordinates.
 * 
 * If an implementation intends to provide a non-infinite map, it should return
 * <code>null</code> for coordinates outside the map. Furthermore, it should
 * return its size in the methods <code>getMaxXNodes</code>,
 * <code>getMaxYNodes</code> and <code>getMaxZNodes</code>.
 * 
 * @author Christian Harnisch
 * 
 */
public abstract class AStarMapBase<T extends AStarNodeBase<T>>
{
  /**
   * Returns the node at the given coordinates. If it is desired that the map is
   * bounded, that coordinates may not be negative, and so on, this method
   * should return <code>null</code> in these cases.
   * 
   * @param x Horizontal position
   * @param y Vertical position
   * @return A node at the given position, or <code>null</code> if none exists
   *         there.
   */
  public abstract T nodeAt(int x, int y, int z);

  /**
   * Marks the specified position as blocked, which means that there is no valid
   * movement from any adjacent tile to this one.
   * 
   * @param x Horizontal position
   * @param y Vertical position
   * @param blockedStatus <code>true</code> if this position should be blocked,
   *          <code>false</code> otherwise.
   */
  public abstract void setTileBlocked(int x, int y, int z, boolean blockedStatus);

  /**
   * Returns if the specified position is blocked.
   * 
   * @param x Horizontal position
   * @param y Vertical position
   * @param z Layer
   * @return <code>true</code> if this position is blocked, <code>false</code>
   *         otherwise.
   */
  public abstract boolean isTileBlocked(int x, int y, int z);

  protected void visitIfNotBlocked(AStarMapVisitorBase<T> visitor, T origin, int x, int y, int z)
  {
    if (!isTileBlocked(x, y, z))
      visitor.visitNeighbour(origin, x, y, z);
  }

  /**
   * Lets the provided visitor visit all non-blocked neighbours of a given node.
   * 
   * @param origin Node whose neighbours shall be visited.
   * @param visitor Visitor to use.
   */
  public abstract void visitNeighboursOf(T origin, AStarMapVisitorBase<T> visitor);

  protected void visitNeighboursOf8(T origin, AStarMapVisitorBase<T> visitor)
  {
    int x = origin.getX();
    int y = origin.getY();
    int z = origin.getZ();

    visitIfNotBlocked(visitor, origin, x - 1, y, z);
    visitIfNotBlocked(visitor, origin, x + 1, y, z);
    visitIfNotBlocked(visitor, origin, x, y - 1, z);
    visitIfNotBlocked(visitor, origin, x, y + 1, z);
    // Diagonal neighbours

    if (!isTileBlocked(x - 1, y - 1, z) && !isTileBlocked(x - 1, y, z) && !isTileBlocked(x, y - 1, z))
      visitor.visitNeighbour(origin, x - 1, y - 1, z);

    if (!isTileBlocked(x + 1, y - 1, z) && !isTileBlocked(x + 1, y, z) && !isTileBlocked(x, y - 1, z))
      visitor.visitNeighbour(origin, x + 1, y - 1, z);

    if (!isTileBlocked(x - 1, y + 1, z) && !isTileBlocked(x - 1, y, z) && !isTileBlocked(x, y + 1, z))
      visitor.visitNeighbour(origin, x - 1, y + 1, z);

    if (!isTileBlocked(x + 1, y + 1, z) && !isTileBlocked(x + 1, y, z) && !isTileBlocked(x, y + 1, z))
      visitor.visitNeighbour(origin, x + 1, y + 1, z);
  }

  protected void visitNeighboursOf4(T origin, AStarMapVisitorBase<T> visitor)
  {
    int x = origin.getX();
    int y = origin.getY();
    int z = origin.getZ();

    visitIfNotBlocked(visitor, origin, x - 1, y, z);
    visitIfNotBlocked(visitor, origin, x + 1, y, z);
    visitIfNotBlocked(visitor, origin, x, y - 1, z);
    visitIfNotBlocked(visitor, origin, x, y + 1, z);
  }

  /**
   * Returns if the node on the specified position is in the open list.
   * 
   * @param x Horizontal position
   * @param y Vertical position
   * @param z Layer
   * @return <code>true</code> if this position is blocked, <code>false</code>
   *         otherwise.
   */
  // public boolean isTileOpen(int x, int y, int z);
  /**
   * Returns if the node on the specified position is in the closed list.
   * 
   * @param x Horizontal position
   * @param y Vertical position
   * @param z Layer
   * @return <code>true</code> if this position is blocked, <code>false</code>
   *         otherwise.
   */
  // public boolean isTileClosed(int x, int y, int z);

  /**
   * Returns the neighbour tiles of the given tile.<br>
   * <br>
   * Neighbours are those tiles that can be moved on from this tile. This
   * implies that the neighbours <strong>must not</strong> include blocked
   * tiles.
   * 
   * @param tile Tile whose neighbours are requested.
   * @return Set of neighbour tiles.
   */
  // public Set<Tile> getNeighboursOf(AStarNode node);

  public abstract void clearMapKeepBlocks();

  public abstract AStarMapBase<T> clone();

  public boolean tryInsertPath(List<T> path)
  {
    for (T node : path)
    {
      if (isTileBlocked(node.getX(), node.getY(), node.getZ()))
        return false;
    }

    for (T node : path)
    {
      setTileBlocked(node.getX(), node.getY(), node.getZ(), true);
    }
    return true;
  }

  /* Convenience methods */
  /* Not needed, we have getNeighboursOf() */
  /*
   * public Tile tileLeftOf( Tile tile );
   * 
   * public Tile tileRightOf( Tile tile );
   * 
   * public Tile tileAbove( Tile tile );
   * 
   * public Tile tileBelow( Tile tile );
   */

  /**
   * Returns the horizontal size of the map. If the map is unbounded, it should
   * return <code>0</code>.
   * 
   * @return Width of the map.
   */
  public abstract int getMaxXNodes();

  /**
   * Returns the vertical size of the map. If the map is unbounded, it should
   * return <code>0</code>.
   * 
   * @return Height of the map.
   */
  public abstract int getMaxYNodes();

  /**
   * Returns the number of layers of the map. If the map is unbounded, it should
   * return <code>0</code>.
   * 
   * @return Depth of the map.
   */
  public abstract int getMaxZNodes();

  protected ObjectPool<T> objectPool;

  public void setObjectPool(ObjectPool<T> objectPool)
  {
    this.objectPool = objectPool;
  }

}
