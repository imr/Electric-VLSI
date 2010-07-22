/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: RegionBoundingBox.java
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
package com.sun.electric.tool.routing.experimentalAStar3.map;

import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarMapBase;
import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarMapVisitorBase;
import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarRegionNode;

/**
 * Acts as a proxy to the global region grid. This is used for storing the
 * bounds of a region bounding box and restrict the accesses of an
 * AStarRegionMachine.
 * 
 * @author Christian Harnisch
 * 
 */
public class RegionBoundingBox extends AStarMapBase<AStarRegionNode>
{
  /* Tuning parameters */
  /**
   * Depicts by how many regions the region grid is enlarged upon calling
   * <code>enlarge()</code>
   */
  private static final int GROWTH_RATE = 2;

  /*
   * From here on below: Private variables for internal use. Don't try to tune
   * these.
   */
  /**
   * The region grid this bounding box is located in.
   */
  private AStarMapBase<AStarRegionNode> regionGrid;

  /**
   * These members depict the bounds of the bounding box. The values are meant
   * inclusively, in other words, an upper bound of 0 and a lower bound of 1
   * means that this bounding box includes layers 0 and 1.
   */
  private int westernBound, easternBound, northernBound, southernBound, upperBound, lowerBound;

  /**
   * Creates a new bounding box around the given pair of coordinates. The
   * created box is minimal, and can later be increased in size by calling
   * <code>enlarge</code>
   * 
   * @param regionGrid The region grid this bounding box is located in.
   * @param startX X position of the start point.
   * @param startY Y position of the start point.
   * @param startZ Z position of the start point.
   * @param goalX X position of the goal point.
   * @param goalY Y position of the goal point.
   * @param goalZ Z position of the goal point.
   */
  public RegionBoundingBox(AStarMapBase<AStarRegionNode> regionGrid, int startX, int startY, int startZ, int goalX, int goalY,
      int goalZ)
  {
    // Calculate region bounding box
    westernBound = Math.min(startX, goalX);
    easternBound = Math.max(startX, goalX);
    northernBound = Math.min(startY, goalY);
    southernBound = Math.max(startY, goalY);
    upperBound = Math.min(startZ, goalZ) - 1;
    lowerBound = Math.max(startZ, goalZ) + 1;
    this.regionGrid = regionGrid;
  }

  /**
   * Sets the size of the bounding box
   * 
   * @deprecated The bounds are currently calculated internally.
   * 
   * @param westernBound
   * @param easternBound
   * @param northernBound
   * @param southernBound
   * @param upperBound
   * @param lowerBound
   */
  @Deprecated
  public void setBounds(int westernBound, int easternBound, int northernBound, int southernBound, int upperBound, int lowerBound)
  {
    this.westernBound = westernBound;
    this.easternBound = easternBound;
    this.northernBound = northernBound;
    this.southernBound = southernBound;
    this.upperBound = upperBound;
    this.lowerBound = lowerBound;
  }

  /**
   * Increases the size of the bounding box by one in all directions. Can be
   * used for retrying to find a global path in this bounding box if path search
   * failed before.
   */
  public void enlarge()
  {
    this.westernBound -= GROWTH_RATE;
    this.easternBound += GROWTH_RATE;
    this.northernBound -= GROWTH_RATE;
    this.southernBound += GROWTH_RATE;
    this.upperBound -= GROWTH_RATE;
    this.lowerBound += GROWTH_RATE;
  }

  /**
   * Returns if all regions are currently not occupied by another search.
   * 
   * @return
   */
  public boolean isBoundingBoxFree()
  {
    for (int x = westernBound; x <= easternBound; x++)
      for (int y = northernBound; y <= southernBound; y++)
        for (int z = upperBound; z <= lowerBound; z++)
          if (!isOutsideMap(x, y, z))
            if (regionGrid.isTileBlocked(x, y, z))
              return false;
    return true;
  }

  /**
   * Marks all AStarRegionNodes inside this bounding box as occupied.
   */
  public void occupyBoundingBox()
  {
    for (int x = westernBound; x <= easternBound; x++)
      for (int y = northernBound; y <= southernBound; y++)
        for (int z = upperBound; z <= lowerBound; z++)
          if (!isOutsideMap(x, y, z))
            regionGrid.setTileBlocked(x, y, z, true);
  }

  /**
   * Marks all AStarRegionNodes inside this bounding box as not occupied.
   */
  public void releaseBoundingBox()
  {
    for (int x = westernBound; x <= easternBound; x++)
      for (int y = northernBound; y <= southernBound; y++)
        for (int z = upperBound; z <= lowerBound; z++)
          if (!isOutsideMap(x, y, z))
            regionGrid.setTileBlocked(x, y, z, false);
  }

  /*
   * (non-Javadoc)
   * 
   * @seecom.sun.electric.tool.routing.astar.t3.algorithm.AStarMapBase#
   * clearMapKeepBlocks()
   */
  @Override
  public void clearMapKeepBlocks()
  {
    throw new UnsupportedOperationException(
        "The region boundinx box cannot be cleared, you must call this method in the region grid.");
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.sun.electric.tool.routing.astar.t3.algorithm.AStarMapBase#clone()
   */
  @Override
  public AStarMapBase<AStarRegionNode> clone()
  {
    throw new UnsupportedOperationException("The region bounding box shouldn't be cloned to ensure mutual exclusive access.");
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.sun.electric.tool.routing.astar.t3.algorithm.AStarMapBase#getMaxXNodes
   * ()
   */
  @Override
  public int getMaxXNodes()
  {
    return (easternBound - westernBound);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.sun.electric.tool.routing.astar.t3.algorithm.AStarMapBase#getMaxYNodes
   * ()
   */
  @Override
  public int getMaxYNodes()
  {
    return (southernBound - northernBound);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.sun.electric.tool.routing.astar.t3.algorithm.AStarMapBase#getMaxZNodes
   * ()
   */
  @Override
  public int getMaxZNodes()
  {
    return (lowerBound - upperBound);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.sun.electric.tool.routing.astar.t3.algorithm.AStarMapBase#isTileBlocked
   * (int, int, int)
   */
  @Override
  public boolean isTileBlocked(int x, int y, int z)
  {
    return (isOutsideBounds(x, y, z) || this.regionGrid.isTileBlocked(x, y, z));
  }

  private boolean isOutsideBounds(int x, int y, int z)
  {
    return (x < westernBound || x > easternBound || y < northernBound || y > southernBound || z < upperBound || z > lowerBound);
  }

  private boolean isOutsideMap(int x, int y, int z)
  {
    boolean outsideX = false;
    boolean outsideY = false;
    boolean outsideZ = false;
    if (regionGrid.getMaxXNodes() != 0)
      outsideX = (x < 0) || (x >= regionGrid.getMaxXNodes());
    if (regionGrid.getMaxYNodes() != 0)
      outsideY = (y < 0) || (y >= regionGrid.getMaxYNodes());
    if (regionGrid.getMaxZNodes() != 0)
      outsideZ = (z < 0) || (z >= regionGrid.getMaxZNodes());
    return (outsideX || outsideY || outsideZ);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.sun.electric.tool.routing.astar.t3.algorithm.AStarMapBase#nodeAt(int,
   * int, int)
   */
  @Override
  public AStarRegionNode nodeAt(int x, int y, int z)
  {
    if (isOutsideBounds(x, y, z))
      return null;
    else
      return this.regionGrid.nodeAt(x, y, z);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.sun.electric.tool.routing.astar.t3.algorithm.AStarMapBase#setTileBlocked
   * (int, int, int, boolean)
   */
  @Override
  public void setTileBlocked(int x, int y, int z, boolean blockedStatus)
  {
    this.regionGrid.setTileBlocked(x, y, z, blockedStatus);
  }

  /**
   * Lets the visitor visit the neighbours of <code>origin</code> in six
   * directions, that is four in the same layer and its neighbours above and
   * below.
   * 
   * @param origin
   * @param visitor
   */
  @Override
  public void visitNeighboursOf(AStarRegionNode origin, AStarMapVisitorBase<AStarRegionNode> visitor)
  {
    int x = origin.getX();
    int y = origin.getY();
    int z = origin.getZ();
    if (!isOutsideBounds(x, y - 1, z))
      if (!isOutsideMap(x, y - 1, z))
        visitor.visitNeighbour(origin, x, y - 1, z);
    if (!isOutsideBounds(x + 1, y, z))
      if (!isOutsideMap(x + 1, y, z))
        visitor.visitNeighbour(origin, x + 1, y, z);
    if (!isOutsideBounds(x, y + 1, z))
      if (!isOutsideMap(x, y + 1, z))
        visitor.visitNeighbour(origin, x, y + 1, z);
    if (!isOutsideBounds(x - 1, y, z))
      if (!isOutsideMap(x - 1, y, z))
        visitor.visitNeighbour(origin, x - 1, y, z);
    if (!isOutsideBounds(x, y, z + 1))
      if (!isOutsideMap(x, y, z + 1))
        visitor.visitNeighbour(origin, x, y, z + 1);
    if (!isOutsideBounds(x, y, z - 1))
      if (!isOutsideMap(x, y, z - 1))
        visitor.visitNeighbour(origin, x, y, z - 1);
  }
}
