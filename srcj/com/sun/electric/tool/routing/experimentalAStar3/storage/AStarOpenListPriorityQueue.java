/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AStarOpenListPriorityQueue.java
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarMapBase;
import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarNode;
import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarOpenListBase;

/**
 * @author Christian Harnisch
 * 
 */
public class AStarOpenListPriorityQueue implements AStarOpenListBase<AStarNode>
{

  private int initialCapacity = 100;

  private PriorityQueue<AStarNode> queue = new PriorityQueue<AStarNode>(initialCapacity, new AStarNodeComparator());

  /*
   * (non-Javadoc)
   * 
   * @see algorithm.AStarOpenListBase#addNodeToOpenList(algorithm.AStarNode)
   */
  public void addNodeToOpenList(AStarNode node)
  {
    queue.add(node);
    node.markAsOpen();
  }

  /*
   * (non-Javadoc)
   * 
   * @see algorithm.AStarOpenListBase#findOpenNode(algorithm.Tile)
   */
  public AStarNode findOpenNode(int x, int y, int z)
  {
    Iterator<AStarNode> i = queue.iterator();
    while (i.hasNext())
    {
      AStarNode current = i.next();
      if (current.getX() == x && current.getY() == y && current.getZ() == z)
        return current;
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see algorithm.AStarOpenListBase#isOpenListEmpty()
   */
  public boolean isOpenListEmpty()
  {
    return queue.size() == 0;
  }

  /*
   * (non-Javadoc)
   * 
   * @see algorithm.AStarOpenListBase#removeCheapestOpenNode()
   */
  public AStarNode removeCheapestOpenNode()
  {
    AStarNode node = queue.poll();
    if (node != null)
      node.markAsNoList();
    return node;
  }

  /*
   * (non-Javadoc)
   * 
   * @see algorithm.AStarOpenListBase#dumpOpenList()
   */
  public Collection<AStarNode> dumpOpenList()
  {
    Collection<AStarNode> dump = new ArrayList<AStarNode>(queue);
    queue.clear();
    return dump;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * algorithm.AStarOpenListBase#removeNodeFromOpenList(algorithm.AStarNode)
   */
  public void removeNodeFromOpenList(AStarNode node)
  {
    queue.remove(node);
    node.markAsNoList();
  }

  // @Override
  public void setMap(AStarMapBase<AStarNode> map)
  {
    // Not supported by this kind of open list
  }

  /*
   * (non-Javadoc)
   * 
   * @seecom.sun.electric.tool.routing.astar.t3.algorithm.AStarOpenListBase#
   * clearOpenList()
   */
  public void clearOpenList()
  {
    queue.clear();
  }
}

class AStarNodeComparator implements Comparator<AStarNode>
{

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
   */
  public int compare(AStarNode arg0, AStarNode arg1)
  {
    int total0 = arg0.getTotalCost();
    int total1 = arg1.getTotalCost();
    if (total0 < total1)
      return -1;
    else if (total0 == total1)
      return 0;
    else
      return 1;
  }
}