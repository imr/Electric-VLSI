/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AStarNodeObjectPool.java
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
package com.sun.electric.tool.routing.experimentalAStar2.memorymanager;

import com.sun.electric.tool.routing.experimentalAStar2.algorithm.AStarNode;

/**
 * Optimized memory pool for AStarNode objects, without the need of an extrusive
 * list.
 * 
 * We are using the AStarNode's origin pointer for chaining all unused nodes.
 */
public class AStarNodeObjectPool implements ObjectPool<AStarNode>
{

  private AStarNode free_list;

  public AStarNodeObjectPool()
  {
    free_list = null;
  }

  public AStarNode acquire()
  {
    AStarNode object;

    if (free_list == null)
    {
      object = new AStarNode();
    }
    else
    {
      object = free_list;
      free_list = object.origin; // the next free object
    }

    return object;
  }

  public void release(AStarNode object)
  {
    object.origin = free_list;
    free_list = object;
  }
}
