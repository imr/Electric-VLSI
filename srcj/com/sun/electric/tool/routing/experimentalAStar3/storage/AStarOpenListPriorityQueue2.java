/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AStarOpenListPriorityQueue2.java
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

import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarMapBase;
import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarNode;
import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarOpenListBase;
import com.sun.electric.tool.routing.experimentalAStar3.datastructures.PriorityQueue;

/**
 * @author Michael Neumann
 * 
 */
public class AStarOpenListPriorityQueue2 implements AStarOpenListBase<AStarNode>
{
  private AStarNodePriorityQueue queue = new AStarNodePriorityQueue();

  private AStarMapBase<AStarNode> map;

  public void setMap(AStarMapBase<AStarNode> map)
  {
    this.map = map;
  }
  
  public void addNodeToOpenList(AStarNode node)
  {
    node.markAsOpen();
    if (node.pq_i == 0)
      queue.push(node);
    else
      queue.update(node);
  }

  public AStarNode findOpenNode(int x, int y, int z)
  {
    AStarNode node = map.nodeAt(x, y, z);
    if (node.isOpen())
      return node;
    return null;
  }

  public boolean isOpenListEmpty()
  {
    return queue.empty();
  }

  public AStarNode removeCheapestOpenNode()
  {
    if (queue.empty())
      return null;
    AStarNode node = queue.top();
    queue.pop();
    node.markAsNoList();
    assert(node.pq_i == 0);
    return node;
  }

  public Collection<AStarNode> dumpOpenList()
  {
    // XXX
    assert(false);
    //Collection<AStarNode> dump = new ArrayList<AStarNode>(queue);
    queue.clear();
    return null;
    //return dump;
  }

  public void removeNodeFromOpenList(AStarNode node)
  {
    assert(node.isOpen());
    node.markAsNoList();
    queue.remove(node);
  }

  public void clearOpenList()
  {
    AStarNode elements[] = queue.getElements();
    for (int i = 1; i <= queue.size(); ++i)
    {
      AStarNode e = elements[i];
      e.markAsNoList();
      e.pq_i = 0;
    }
    queue.setEmpty();
  }
}

class AStarNodePriorityQueue extends PriorityQueue<AStarNode> {
  
  protected boolean less(AStarNode a, AStarNode b)
  {
    return a.getTotalCost() < b.getTotalCost();
  }

  protected void set_index(AStarNode node, int new_index)
  {
    node.pq_i = new_index;
  }
  
  protected int get_index(AStarNode node)
  {
    return node.pq_i;
  }

  protected AStarNode[] alloc_array(int sz)
  {
    return new AStarNode[sz];
  }
}
