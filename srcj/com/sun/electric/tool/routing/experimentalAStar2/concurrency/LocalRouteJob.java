/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LocalRouteJob.java
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
package com.sun.electric.tool.routing.experimentalAStar2.concurrency;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.sun.electric.tool.routing.experimentalAStar2.algorithm.AStarGoalBase;
import com.sun.electric.tool.routing.experimentalAStar2.algorithm.AStarNode;
import com.sun.electric.tool.routing.experimentalAStar2.algorithm.AStarRegionNode;
import com.sun.electric.tool.routing.experimentalAStar2.datastructures.Point3D;
import com.sun.electric.tool.routing.experimentalAStar2.goal.SimpleGoal;
import com.sun.electric.tool.routing.experimentalAStar2.machine.AStarMachine;
import com.sun.electric.tool.routing.experimentalAStar2.machine.AStarMachineFast;
import com.sun.electric.tool.routing.experimentalAStar2.memorymanager.AStarNodeObjectPool;

public class LocalRouteJob implements Callable<LocalRouteJob>
{
  public Point3D from;

  public Point3D to;

  public List<Point3D> path; // the result

  public RouteJob routeJob;

  // public Region region;

  public AStarRegionNode regionalNode;

  public int numberInGlobalPath = -1;

  public LocalRouteJob()
  {
  }

  public LocalRouteJob(Point3D from, Point3D to)
  {
    this.from = from;
    this.to = to;
    this.path = null;
  }

  public List<Point3D> getPath()
  {
    return this.path;
  }

  public LocalRouteJob call()
  {
    // System.out.println("LocalRouteJob started at (" +
    // this.regionalNode.getX() + "/" + this.regionalNode.getY() + "/"
    // + this.regionalNode.getZ() + ")");

    this.path = null;
    AStarMachine<AStarNode> machine = new AStarMachineFast(new AStarNodeObjectPool());
    AStarGoalBase<AStarNode> goal = new SimpleGoal();
    goal.setMaximumRevolutions(RoutingMain.MAX_REVOLUTIONS);

    machine.setUpSearchSpace(this.regionalNode.routingMap, goal);
    List<AStarNode> path = machine.findPath(from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ());

    if (path == null)
    {
      // System.out.println("Local routing couldn't find path");
    }
    else
    {
      /*
       * Returns a Point3D path. XXX: Optimize the path here!
       */
      this.path = new ArrayList<Point3D>();

      /*
       * Return optimized path
       */
      // AStarNode last = null;
      for (AStarNode node : path)
      {
        // if (last == null || last.getZ() != node.getZ() || (last.getX() !=
        // node.getX() && last.getY() != node.getY()))
        // {
        this.regionalNode.removePathForNode(node.getX(), node.getY());
        this.path.add(new Point3D(node.getX(), node.getY(), node.getZ()));
        // }
        // last = node;
      }
      this.regionalNode.setEntryPoint(null);
      this.regionalNode.setExitPoint(null);

      // this.path.add(new Point3D(last.getX(), last.getY(), last.getZ()));
    }

    return this;
  }
}
