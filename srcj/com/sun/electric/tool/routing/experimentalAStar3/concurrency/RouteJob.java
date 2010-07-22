/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: RouteJob.java
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import com.sun.electric.tool.routing.RoutingFrame.RoutingSegment;
import com.sun.electric.tool.routing.experimentalAStar3.AStarRouter;
import com.sun.electric.tool.routing.experimentalAStar3.datastructures.Point3D;
import com.sun.electric.tool.routing.experimentalAStar3.map.RegionBoundingBox;

public class RouteJob
{
  public Point3D from;

  public Point3D to;

  public List<Point3D> path; // the result

  public Deque<LocalRouteJob> localRouteJobs = new ArrayDeque<LocalRouteJob>();

  public List<LocalRouteJob> localRouteJobsCompleted = new ArrayList<LocalRouteJob>();

  public RoutingSegment routingSegment;

  RegionBoundingBox boundingBox;

  protected AStarRouter astarrouter;

  public int retries = 0;

  public RouteJob()
  {
  }

  public RouteJob(Point3D from, Point3D to, AStarRouter router)
  {
    this.from = from;
    this.to = to;
    this.path = null;
    this.astarrouter = router;
  }

  public List<Point3D> getPath()
  {
    return this.path;
  }

  public RegionBoundingBox getBoundingBox()
  {
    return boundingBox;
  }

  /*
   * Implement this method.
   */
  public void onCompletion()
  {
  }
}
