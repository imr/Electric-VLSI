/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GlobalRouteJob.java
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
package com.sun.electric.tool.routing.experimentalAStar3.concurrency;

import java.util.List;
import java.util.concurrent.Callable;

import com.sun.electric.tool.routing.experimentalAStar3.algorithm.AStarRegionNode;
import com.sun.electric.tool.routing.experimentalAStar3.datastructures.Point3D;
import com.sun.electric.tool.routing.experimentalAStar3.goal.RegionGoal;
import com.sun.electric.tool.routing.experimentalAStar3.machine.AStarRegionMachine;
import com.sun.electric.tool.routing.experimentalAStar3.memorymanager.AStarRegionNodeObjectPool;

/**
 * @author Christian Harnisch
 * 
 */
public class GlobalRouteJob extends RouteJob implements Callable<GlobalRouteJob>
{
  public RouteJob routeJob;

  /*
   * The region-local coordinates of the start/end points inside the from and to
   * regions.
   */
  private Point3D fromInsideRegion;

  private Point3D toInsideRegion;

  private AStarRegionMachine machine;

  private RegionGoal goal;

  public List<AStarRegionNode> resultPath;

  public GlobalRouteJob()
  {
    this.goal = new RegionGoal();
    this.goal.setMaximumRevolutions(RoutingMain.MAX_REVOLUTIONS);
    this.machine = new AStarRegionMachine(new AStarRegionNodeObjectPool());
  }

  public void initialize(RouteJob routeJob, Point3D fromRegion, Point3D toRegion, Point3D fromInsideRegion, Point3D toInsideRegion)
  {
    this.routeJob = routeJob;
    this.from = fromRegion;
    this.to = toRegion;
    this.fromInsideRegion = fromInsideRegion;
    this.toInsideRegion = toInsideRegion;
  }

  public RouteJob getRouteJob()
  {
    return this.routeJob;
  }

  /*
   * (non-Javadoc)
   * 
   * @see java.util.concurrent.Callable#call()
   */
  public GlobalRouteJob call() throws Exception
  {
    // Set entry point in start region and exit point in goal region
    AStarRegionNode startRegion = this.routeJob.getBoundingBox().nodeAt(this.from.getX(), this.from.getY(), this.from.getZ());
    AStarRegionNode goalRegion = this.routeJob.getBoundingBox().nodeAt(this.to.getX(), this.to.getY(), this.to.getZ());
    startRegion.setAsStartRegion(true);
    goalRegion.setAsGoalRegion(true);
    startRegion.setEntryPoint(startRegion.routingMap.nodeAt(this.fromInsideRegion.getX(), this.fromInsideRegion.getY(),
        this.fromInsideRegion.getZ()));
    goalRegion.setExitPoint(goalRegion.routingMap.nodeAt(this.toInsideRegion.getX(), this.toInsideRegion.getY(),
        this.toInsideRegion.getZ()));

    // Set up the machine's search space (-> map is actually a bounding box)
    this.machine.setUpSearchSpace(this.routeJob.getBoundingBox(), this.goal);

    // Call findPath()
    this.resultPath = this.machine.findPath(this.from.getX(), this.from.getY(), this.from.getZ(), this.to.getX(), this.to.getY(),
        this.to.getZ());
    startRegion.setAsStartRegion(false);
    goalRegion.setAsGoalRegion(false);
    return this;
  }
}