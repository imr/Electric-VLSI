/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AStarClosedListHashMap.java
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
package com.sun.electric.tool.routing.experimentalAStar3.storage;

import java.util.Collection;
import java.util.HashMap;

import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarClosedListBase;
import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarMapBase;
import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarNode;

/**
 * @author Christian Harnisch
 * 
 */
public class AStarClosedListHashMap implements AStarClosedListBase<AStarNode>
{

  private HashMap<String, AStarNode> nodesMap = new HashMap<String, AStarNode>();

  private String calcKey(int x, int y, int z)
  {
    return x + "," + y + "," + z;
  }

  /*
   * (non-Javadoc)
   * 
   * @see algorithm.AStarClosedListBase#addNodeToClosedList(algorithm.AStarNode)
   */
  public void addNodeToClosedList(AStarNode node)
  {
    nodesMap.put(calcKey(node.getX(), node.getY(), node.getZ()), node);
  }

  /*
   * (non-Javadoc)
   * 
   * @see algorithm.AStarClosedListBase#findClosedNode(algorithm.AStarTileBase)
   */
  public AStarNode findClosedNode(int x, int y, int z)
  {
    return nodesMap.get(calcKey(x, y, z));
  }

  /*
   * (non-Javadoc)
   * 
   * @see algorithm.AStarClosedListBase#dumpClosedList()
   */
  public Collection<AStarNode> dumpClosedList()
  {
    Collection<AStarNode> dump = nodesMap.values();
    nodesMap.clear();
    return dump;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * algorithm.AStarClosedListBase#removeNodeFromClosedList(algorithm.AStarNode)
   */
  public void removeNodeFromClosedList(AStarNode node)
  {
    nodesMap.remove(node); // TODO: Check why this does not work
    nodesMap.put(calcKey(node.getX(), node.getY(), node.getZ()), null);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.sun.electric.tool.routing.astar.t3.algorithm.AStarClosedListBase#setMap
   * (com.sun.electric.tool.routing.astar.t3.algorithm.AStarMapBase)
   */
  public void setMap(AStarMapBase<AStarNode> map)
  {
    // Not supported by this kind of closed list
  }

  /*
   * (non-Javadoc)
   * 
   * @seecom.sun.electric.tool.routing.astar.t3.algorithm.AStarClosedListBase#
   * clearClosedList()
   */
  public void clearClosedList()
  {
    nodesMap.clear();
  }

}