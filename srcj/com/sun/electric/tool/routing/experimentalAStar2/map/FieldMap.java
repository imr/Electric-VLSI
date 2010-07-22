/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: FieldMap.java
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

import java.util.Vector;

import com.sun.electric.tool.routing.experimentalAStar2.algorithm.AStarMapBase;
import com.sun.electric.tool.routing.experimentalAStar2.algorithm.AStarMapVisitorBase;
import com.sun.electric.tool.routing.experimentalAStar2.algorithm.AStarNodeBase;

/**
 * Remembers which fields are free and which are blocked and determines the free
 * neighbours of a field.
 */
public class FieldMap<T extends AStarNodeBase<T>> extends AStarMapBase<T>
{

  protected int wx, wy, wz; // width

  protected boolean blockages[];

  protected Vector<T> nodes;

  /*
   * Does not copy nodes array!
   */
  protected FieldMap(FieldMap<T> clone)
  {
    this.wx = clone.wx;
    this.wy = clone.wy;
    this.wz = clone.wz;
    int sz = this.wx * this.wy * this.wz;
    this.blockages = new boolean[sz];
    for (int i = 0; i < sz; ++i)
      this.blockages[i] = clone.blockages[i];
    this.nodes = new Vector<T>(sz);
    this.nodes.setSize(sz);
    this.objectPool = clone.objectPool;
  }

  public FieldMap(int wx, int wy, int wz, int ox, int oy, int oz)
  {
    assert (ox == 0 && oy == 0 && oz == 0);

    this.wx = wx;
    this.wy = wy;
    this.wz = wz;

    int sz = this.wx * this.wy * this.wz;
    this.blockages = new boolean[sz];
    this.nodes = new Vector<T>(sz);
    this.nodes.setSize(sz);
  }

  public void visitNeighboursOf(T origin, AStarMapVisitorBase<T> visitor)
  {
    visitNeighboursOf4(origin, visitor);
  }

  public boolean inRange(int x, int y, int z)
  {
    if (x < 0 || y < 0 || z < 0 || x >= wx || y >= wy || z >= wz)
      return false;
    else
      return true;
  }

  /*
   * Return -1 on error.
   */
  private int getOff(int x, int y, int z)
  {
    if (x < 0 || y < 0 || z < 0 || x >= wx || y >= wy || z >= wz)
      return -1;
    int off = x + (y * wx) + (z * wx * wy);
    // assert (off >= 0 && off < wx * wy * wz);
    return off;
  }

  public boolean isTileBlocked(int x, int y, int z)
  {
    int off = getOff(x, y, z);

    // Treat everything outside the map as blocked.
    if (off == -1)
      return true;

    return blockages[off];
  }

  public void setTileBlocked(int x, int y, int z, boolean blockedStatus)
  {
    int off = getOff(x, y, z);
    assert (off != -1);
    blockages[off] = blockedStatus;
  }

  public T nodeAt(int x, int y, int z)
  {
    int off = getOff(x, y, z);
    if (off == -1)
      return null;

    T node = nodes.elementAt(off);
    if (node == null)
    {
      node = objectPool.acquire();
      node.initialize(null, 0, 0, 0, x, y, z);
      nodes.setElementAt(node, off);
    }
    return node;
  }

  public T getNode(int x, int y, int z)
  {
    int off = getOff(x, y, z);

    // Treat everything outside the map as blocked.
    if (off == -1)
      return null;

    return nodes.elementAt(off);
  }

  public void setNode(int x, int y, int z, T node)
  {
    int off = getOff(x, y, z);
    assert (off != -1);
    nodes.setElementAt(node, off);
  }

  public void clearMapKeepBlocks()
  {
    for (int i = 0; i < this.wx * this.wy * this.wz; ++i)
      this.nodes.setElementAt(null, i);
  }

  public AStarMapBase<T> clone()
  {
    return new FieldMap<T>(this);
  }

  public int getMaxXNodes()
  {
    return wx;
  }

  public int getMaxYNodes()
  {
    return wy;
  }

  public int getMaxZNodes()
  {
    return wz;
  }
}
