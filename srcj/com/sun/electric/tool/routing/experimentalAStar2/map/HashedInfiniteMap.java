/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: HashedInfiniteMap.java
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
package com.sun.electric.tool.routing.experimentalAStar2.map;

import java.util.HashMap;

import com.sun.electric.tool.routing.experimentalAStar2.algorithm.AStarMapBase;
import com.sun.electric.tool.routing.experimentalAStar2.algorithm.AStarMapVisitorBase;
import com.sun.electric.tool.routing.experimentalAStar2.algorithm.AStarNodeBase;
import com.sun.electric.tool.routing.experimentalAStar2.memorymanager.ObjectPool;

public class HashedInfiniteMap<T extends AStarNodeBase<T>> extends AStarMapBase<T>
{
  protected ObjectPool<T> nodePool;

  private int initialCapacity = 100;

  private HashMap<String, T> map = new HashMap<String, T>(initialCapacity);

  protected HashMap<String, Boolean> blockedMap = new HashMap<String, Boolean>(initialCapacity);

  public HashedInfiniteMap(ObjectPool<T> nodePool)
  {
    this.nodePool = nodePool;
  }

  /*
   * (non-Javadoc)
   * 
   * @see algorithm.AStarMapBase#isTileBlocked(int, int)
   */
  public boolean isTileBlocked(int x, int y, int z)
  {
    Boolean result = blockedMap.get(calcKey(x, y, z));
    if (result == null)
      return false;
    return result;
  }

  /*
   * (non-Javadoc)
   * 
   * @see algorithm.AStarMapBase#setTileBlocked(int, int, boolean)
   */
  public void setTileBlocked(int x, int y, int z, boolean blockedStatus)
  {
    blockedMap.put(calcKey(x, y, z), blockedStatus);
  }

  /**
   * Returns the node at the given coordinates.<br>
   * As this implementation provides an unbounded map, <code>nodeAt</code> never
   * returns <code>null</code>.
   * 
   * @param x Horizontal position
   * @param y Vertical position
   * @return A node at the given position
   */
  public T nodeAt(int x, int y, int z)
  {
    String hash = calcKey(x, y, z);
    T node = map.get(hash);
    // If this node isn't in the HashMap yet, create a new one and put it
    // there.
    if (node == null)
    {
      node = nodePool.acquire();
      node.initialize(null, 0, 0, 0, x, y, z);
      map.put(hash, node);
    }
    return node;
  }

  private String calcKey(int x, int y, int z)
  {
    return x + "," + y + "," + z;

    // int sx = x + 4096;
    // int sy = y + 4096;
    // int sz = z + 4096;
    //
    // assert (sx >= 0 && sy >= 0 && sz >= 0);
    // assert (sx < 8192 && sy < 8192 && sz < 8192);
    //
    // return (sx << 32) | (sy << 16) | sz;
  }

  public void clearMapKeepBlocks()
  {
    this.map.clear();
  }

//  @SuppressWarnings("unchecked")
  public AStarMapBase<T> clone()
  {
    HashedInfiniteMap<T> newMap = new HashedInfiniteMap<T>(nodePool);
    // Cast needed: clone() returns an Object
    newMap.blockedMap = (HashMap<String, Boolean>) blockedMap.clone();
    return newMap;
  }

  // @Override
  public void visitNeighboursOf(T origin, AStarMapVisitorBase<T> visitor)
  {
    visitNeighboursOf4(origin, visitor);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.sun.electric.tool.routing.astar.t3.algorithm.AStarMapBase#getMaxXNodes
   * ()
   */
  public int getMaxXNodes()
  {
    return 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.sun.electric.tool.routing.astar.t3.algorithm.AStarMapBase#getMaxYNodes
   * ()
   */
  public int getMaxYNodes()
  {
    return 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.sun.electric.tool.routing.astar.t3.algorithm.AStarMapBase#getMaxZNodes
   * ()
   */
  public int getMaxZNodes()
  {
    return 0;
  }

}
