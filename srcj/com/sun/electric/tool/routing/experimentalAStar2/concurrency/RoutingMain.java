/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: RoutingMain.java
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.sun.electric.tool.routing.experimentalAStar2.algorithm.AStarMapBase;
import com.sun.electric.tool.routing.experimentalAStar2.algorithm.AStarNode;
import com.sun.electric.tool.routing.experimentalAStar2.algorithm.AStarRegionNode;
import com.sun.electric.tool.routing.experimentalAStar2.datastructures.Point3D;
import com.sun.electric.tool.routing.experimentalAStar2.map.FieldMap;
import com.sun.electric.tool.routing.experimentalAStar2.map.RegionBoundingBox;
import com.sun.electric.tool.routing.experimentalAStar2.memorymanager.AStarNodeObjectPool;
import com.sun.electric.tool.routing.experimentalAStar2.memorymanager.AStarRegionNodeObjectPool;
import com.sun.electric.tool.routing.experimentalAStar2.memorymanager.LinkedListObjectPool;
import com.sun.electric.tool.routing.experimentalAStar2.memorymanager.ObjectFactory;
import com.sun.electric.tool.routing.experimentalAStar2.memorymanager.ObjectPool;

/*
 * We are working only on node-coordinates here.
 */
public class RoutingMain
{
  /* Tuning parameters */

  public static final int MAX_REVOLUTIONS = 300000;

  /**
   * Maximum number of retries for routing one global job. Set to 0 for allowing
   * only the initial try.
   */
  private static final int MAX_RETRIES = 1;

  /* Below: Private members, not for tuning. */

  private int layerCount;

  // private FieldMap<AStarRegionNode> regionMap;

  // private Array2D<Region> regions;

  private AStarMapBase<AStarRegionNode> regionGrid;

  private int numRegionsPerSide;

  public static int regionWidth;

  public static int regionHeight;

  private long maxRuntimeMillis;

  private ExecutorService threadPool;

  private ExecutorCompletionService<GlobalRouteJob> globalCompletionService;

  private ExecutorCompletionService<LocalRouteJob> localCompletionService;

  ObjectPool<GlobalRouteJob> globalRouteJobPool;

  private int routeJobCount;

  private int completedRouteJobCount;

  private List<RouteJob> waitingRouteJobs = new LinkedList<RouteJob>();

  /**
   * Returns the current routing progress in percent (this means: In the range
   * from 0 to 100)
   * 
   * @return Current progress in percent.
   */
  public int getProgress()
  {
    return Math.min((completedRouteJobCount * 100) / routeJobCount, 100);
  }

  public void setMaxRuntimeMillis(long maxRuntimeMillis)
  {
    this.maxRuntimeMillis = maxRuntimeMillis;
  }

  public RoutingMain(int width, int height, int layerCount, int threadPoolSize)
  {
    assert (layerCount > 0);

    this.layerCount = layerCount;
    this.numRegionsPerSide = Math.min(16, Math.max(1, threadPoolSize / 2));
    regionWidth = width / this.numRegionsPerSide;
    regionHeight = height / this.numRegionsPerSide;

    /*
     * Initialise region grid. The region grid is a map of AStarRegionNodes. It
     * basically cuts the whole cell into quadratic one-layer submaps.
     */
    FieldMap<AStarRegionNode> newGrid = new FieldMap<AStarRegionNode>(this.numRegionsPerSide, this.numRegionsPerSide, layerCount,
        0, 0, 0);
    newGrid.setObjectPool(new AStarRegionNodeObjectPool());
    this.regionGrid = newGrid;
    for (int x = 0; x < this.numRegionsPerSide; x++)
    {
      for (int y = 0; y < this.numRegionsPerSide; y++)
      {
        for (int z = 0; z < layerCount; z++)
        {
          FieldMap<AStarNode> newRoutingMap = new FieldMap<AStarNode>(regionWidth, regionHeight, 1, 0, 0, 0);
          // Every map has it's own object-pool. This is to avoid to synchronize
          // access to the ObjectPool.
          newRoutingMap.setObjectPool(new AStarNodeObjectPool());

          AStarRegionNode node = new AStarRegionNode(newRoutingMap, regionWidth, regionHeight, x, y, z);
          newGrid.setNode(x, y, z, node);
        }
      }
    }

    /*
     * Initialize Regions.
     */
    // this.regions = new Array2D<Region>(this.numRegionsPerSide,
    // this.numRegionsPerSide, null, false);
    // for (int xr = 0; xr < this.numRegionsPerSide; ++xr)
    // {
    // for (int yr = 0; yr < this.numRegionsPerSide; ++yr)
    // {
    // Region region = new Region(); // TODO
    // region.coordinateOffsetX = xr * this.regionWidth;
    // region.coordinateOffsetY = yr * this.regionHeight;
    // region.tilePosX = xr;
    // region.tilePosY = yr;
    // region.map = new FieldMap<AStarNode>(this.regionWidth, this.regionHeight,
    // this.layerCount, 0, 0, 0);
    // // Every map has it's own object-pool. This is to avoid to synchronize
    // // access to the ObjectPool.
    // region.map.setObjectPool(new AStarNodeObjectPool());
    // this.regions.setAt(xr, yr, region);
    // }
    // }
    //
    // /*
    // * Initialize regionMap, which is used by the global A* path finding
    // * algorithm.
    // */
    // this.regionMap = new FieldMap<AStarRegionNode>(this.numRegionsPerSide,
    // this.numRegionsPerSide, this.layerCount, 0, 0, 0);
    // this.regionMap.setObjectPool(new AStarRegionNodeObjectPool());
    // for (int x = 0; x < numRegionsPerSide; ++x)
    // {
    // for (int y = 0; y < numRegionsPerSide; ++y)
    // {
    // for (int z = 0; z < this.layerCount; ++z)
    // {
    // AStarRegionNode rn = new AStarRegionNode(this.regions.getAt(x, y).map, z,
    // this.regionWidth, this.regionHeight);
    // rn.initialize(null, 0, 0, 0, x, y, z);
    // regionMap.setNode(x, y, z, rn);
    // }
    // }
    // }
    threadPool = Executors.newFixedThreadPool(threadPoolSize);
    localCompletionService = new ExecutorCompletionService<LocalRouteJob>(threadPool);
    globalCompletionService = new ExecutorCompletionService<GlobalRouteJob>(threadPool);
    globalRouteJobPool = new LinkedListObjectPool<GlobalRouteJob>(50, new ObjectFactory<GlobalRouteJob>()
    {
      public GlobalRouteJob create()
      {
        return new GlobalRouteJob();
      }
    });

    routeJobCount = 0;
    completedRouteJobCount = 0;
  }

  public void submitRouteJob(RouteJob job)
  {
    Point3D from = job.from;
    Point3D to = job.to;
    // Set from and to blocked. This has two reasons:
    // 1. Other segments don't accidently block start and end point when routing
    // 2. Multi-segment nets can unblock start and end point before routing
    setTileBlocked(from.getX(), from.getY(), from.getZ(), true);
    setTileBlocked(to.getX(), to.getY(), to.getZ(), true);

    assert (this.regionGrid != null);
    RegionBoundingBox boundingBox = new RegionBoundingBox(this.regionGrid, nodeToRegionX(from.getX()),
        nodeToRegionY(from.getY()), from.getZ(), nodeToRegionX(to.getX()), nodeToRegionY(to.getY()), to.getZ());
    job.boundingBox = boundingBox;
    routeJobCount++;
    waitingRouteJobs.add(job);
  }

  private void submitPossibleGlobalJobs()
  {
    // Search through list of waiting jobs
    Iterator<RouteJob> i = waitingRouteJobs.iterator();
    while (i.hasNext())
    {
      RouteJob job = i.next();
      // See if its bounding box is free
      if (job.boundingBox.isBoundingBoxFree())
      {
        // If yes, init and enqueue new GlobalRouteJob,
        // and remove waiting RouteJob from waiting list
        i.remove();
        job.boundingBox.occupyBoundingBox();
        GlobalRouteJob globalJob = globalRouteJobPool.acquire();

        // Unblock start and end point
        setTileBlocked(job.from.getX(), job.from.getY(), job.from.getZ(), false);
        setTileBlocked(job.to.getX(), job.to.getY(), job.to.getZ(), false);

        Point3D from = new Point3D(nodeToRegionX(job.from.getX()), nodeToRegionY(job.from.getY()), job.from.getZ());
        Point3D to = new Point3D(nodeToRegionX(job.to.getX()), nodeToRegionY(job.to.getY()), job.to.getZ());
        Point3D fromInsideRegion = new Point3D(nodeGlobalToLocalX(job.from.getX()), nodeGlobalToLocalY(job.from.getY()), 0);
        Point3D toInsideRegion = new Point3D(nodeGlobalToLocalX(job.to.getX()), nodeGlobalToLocalY(job.to.getY()), 0);

        globalJob.initialize(job, from, to, fromInsideRegion, toInsideRegion);

        globalCompletionService.submit(globalJob);
        // System.out.println("Global job started");
      }
      // If no, go to next iteration
    }
  }

  /**
   * Handler methods for finished jobs. Called by the main thread.
   */
  public void handleLocalRouteJobFinished(LocalRouteJob finishedJob)
  {
    // System.out.println("RoutingMain: handleLocalRouteJobFinished for region ("
    // + finishedJob.regionalNode.getX() + "/"
    // + finishedJob.regionalNode.getY() + "/" + finishedJob.regionalNode.getZ()
    // + ")");

    if (finishedJob.path == null)
    {
      // System.out.println("RoutingMain: Null path.");
    }
    else
    {
      for (Point3D pt : finishedJob.path)
      {
        // Since local path was routed without layers, set the correct
        // z-coordinate here.
        pt.setZ(finishedJob.regionalNode.getZ());
        // System.out.println("RoutingMain: " + pt.toString());
      }
    }

    finishedJob.routeJob.localRouteJobsCompleted.add(finishedJob);

    if (finishedJob.routeJob.localRouteJobsList.size() != finishedJob.routeJob.localRouteJobsCompleted.size())
    {
      // System.out.println("RoutingMain: Not complete yet");
      return;
    }

    // System.out.println("RoutingMain: All local paths done");

    // Block start and end point again
    setTileBlocked(finishedJob.routeJob.from.getX(), finishedJob.routeJob.from.getY(), finishedJob.routeJob.from.getZ(), true);
    setTileBlocked(finishedJob.routeJob.to.getX(), finishedJob.routeJob.to.getY(), finishedJob.routeJob.to.getZ(), true);

    //
    // Build global path
    //
    Collections.sort(finishedJob.routeJob.localRouteJobsCompleted, new LocalRouteJobComparator());
    List<Point3D> totalPath = new ArrayList<Point3D>();

    for (LocalRouteJob lrj : finishedJob.routeJob.localRouteJobsCompleted)
    {
      // TODO: Release regions on regionMap when all local route jobs are done
      // for one path

      if (lrj.getPath() == null)
      {
        // System.out.println("RoutingMain: SHIT");
        // if one of the local routes is null, we have to throw away the whole
        // global
        // routing job
        finishedJob.path = null;
        finishedJob.routeJob.path = null;
        ++completedRouteJobCount;
        finishedJob.routeJob.onCompletion();
        finishedJob.routeJob.boundingBox.releaseBoundingBox();

        // As one job is finished now, try to submit new ones.
        submitPossibleGlobalJobs();
        return;
      }
      // Append the local path to the global path, converting all coordinates to
      // global coordinates.
      for (Point3D pt : lrj.getPath())
      {
        Point3D pt2 = new Point3D(nodeLocalToGlobalX(pt.getX(), lrj.regionalNode),
            nodeLocalToGlobalY(pt.getY(), lrj.regionalNode), pt.getZ());
        totalPath.add(pt2);
      }
    }

    // Set blockages. All coordinates are global.
    for (Point3D pt : totalPath)
    {
      setTileBlocked(pt.getX(), pt.getY(), pt.getZ(), true);
    }

    finishedJob.routeJob.path = totalPath;
    ++completedRouteJobCount;
    finishedJob.routeJob.boundingBox.releaseBoundingBox();
    // As one global job is finished now, try to submit new ones.
    submitPossibleGlobalJobs();
    finishedJob.routeJob.onCompletion();
  }

  public void handleGlobalRouteJobFinished(GlobalRouteJob finishedJob)
  {
    // System.out.println("RoutingMain: handleGlobalRouteJobFinished");

    List<AStarRegionNode> path = finishedJob.resultPath;

    // No path found: Enlarge bounding box and re-schedule if maximum number of
    // retries is not reached yet
    if (path == null)
    {
      if (finishedJob.routeJob.retries < RoutingMain.MAX_RETRIES)
      {
        finishedJob.routeJob.retries++;
        finishedJob.routeJob.boundingBox.releaseBoundingBox();
        finishedJob.routeJob.boundingBox.enlarge();
        waitingRouteJobs.add(finishedJob.routeJob);
      }
      else
      {
        // System.out.println("RoutingMain: Cannot find global path");
        // Block start and end point again
        setTileBlocked(finishedJob.routeJob.from.getX(), finishedJob.routeJob.from.getY(), finishedJob.routeJob.from.getZ(), true);
        setTileBlocked(finishedJob.routeJob.to.getX(), finishedJob.routeJob.to.getY(), finishedJob.routeJob.to.getZ(), true);

        completedRouteJobCount++;
        finishedJob.routeJob.boundingBox.releaseBoundingBox();
        finishedJob.routeJob.path = null;
        finishedJob.routeJob.onCompletion();
      }
    }
    // Otherwise, dispatch individual detailed routing jobs
    else
    {
      // System.out.println("RoutingMain: Found global path");

      // XXX: Release bounding box again and just occupy individual regions
      // finishedJob.getRouteJob().boundingBox.releaseBoundingBox();

      for (int i = 0; i < path.size(); i++)
      {
        AStarRegionNode rn = path.get(i);
        if (i < (path.size() - 1))
        {
          AStarRegionNode nextRegion = path.get(i + 1);
          AStarNode entryPoint = path.get(i + 1).getEntryPoint();

          // If next region west of this
          if (nextRegion.getX() < rn.getX())
          {
            rn.setExitPoint(rn.getMap(true).nodeAt(0, entryPoint.getY(), 0));
          }
          else
          // Next region east?
          if (nextRegion.getX() > rn.getX())
          {
            rn.setExitPoint(rn.getMap(true).nodeAt(regionWidth - 1, entryPoint.getY(), 0));
          }
          else
          // Next region north?
          if (nextRegion.getY() < rn.getY())
          {
            rn.setExitPoint(rn.getMap(true).nodeAt(entryPoint.getX(), 0, 0));
          }
          else
          // Next region south?
          if (nextRegion.getY() > rn.getY())
          {
            rn.setExitPoint(rn.getMap(true).nodeAt(entryPoint.getX(), regionHeight - 1, 0));
          }
          else
          // Next region different layer?
          if (nextRegion.getZ() != rn.getZ())
          {
            rn.setExitPoint(rn.getMap(true).nodeAt(entryPoint.getX(), entryPoint.getY(), 0));
          }
          // else
          // System.out.println("RoutingMain: Region not one of the 6 manhattan neighbours!");
        }

        // System.out.println("(" + rn.getX() + "/" + rn.getY() + "/" +
        // rn.getZ() + ")");

        // Mark this region as occupied on the regionMap,
        // so other routing jobs don't interfere
        // regionMap.setTileBlocked(rn.getX(), rn.getY(), rn.getZ(), true);

        LocalRouteJob localRoute = new LocalRouteJob();
        // XXX
        localRoute.from = new Point3D(rn.getEntryPoint().getX(), rn.getEntryPoint().getY(), rn.getEntryPoint().getZ());
        // if (rn.getExitPoint() == null)
        // System.out.println("ExitPoint == null! Region (" + rn.getX() + "/" +
        // rn.getY() + "/" + rn.getZ() + ")");
        localRoute.to = new Point3D(rn.getExitPoint().getX(), rn.getExitPoint().getY(), rn.getExitPoint().getZ());
        localRoute.routeJob = finishedJob.getRouteJob();
        // localRoute.region = regions.getAt(rn.getX(), rn.getY());
        localRoute.regionalNode = rn;
        localRoute.numberInGlobalPath = i;
//        localRoute.routeJob.localRouteJobs.push(localRoute);
        localRoute.routeJob.localRouteJobsList.add(localRoute);
        localCompletionService.submit(localRoute);
      }
    }
    globalRouteJobPool.release(finishedJob);
    // As one global job is finished now, try to submit new ones.
    submitPossibleGlobalJobs();
  }

  public void waitForCompletion()
  {
    // Store start time
    long startTimeMillis = System.currentTimeMillis();
    long currentTimeMillis = startTimeMillis;
    // "Bootstrap" one time
    submitPossibleGlobalJobs();
    // Now, spin on completion events
    while ((completedRouteJobCount < routeJobCount) && ((currentTimeMillis - startTimeMillis) <= maxRuntimeMillis))
    {
      try
      {
        Future<LocalRouteJob> localJob = localCompletionService.poll();
        if (localJob != null)
          handleLocalRouteJobFinished(localJob.get());

        Future<GlobalRouteJob> globalJob = globalCompletionService.poll();
        if (globalJob != null)
          handleGlobalRouteJobFinished(globalJob.get());

        currentTimeMillis = System.currentTimeMillis();
      }
      catch (InterruptedException e)
      {
        e.printStackTrace();
      }
      catch (ExecutionException e)
      {
        e.printStackTrace();
      }
    }
    this.shutDown();
    // System.out.println("RoutingMain: Routing done.");
  }

  public void shutDown()
  {
    // Tell workers to cease operating.
    // Use shutdownNow() because we really want to shutdown ASAP
    this.threadPool.shutdownNow();
  }

  /**
   * Sets the rectangle defined by (sx, sy) to (ex, ex) in <em>all</em> layers
   * as a blockage.
   */
  public void setBlockage(int sx, int sy, int ex, int ey)
  {
    for (int x = sx; x <= ex; ++x)
    {
      setTileBlockedAllLayers(x, sy);
      setTileBlockedAllLayers(x, ey);
    }
    for (int y = sy; y <= ey; ++y)
    {
      setTileBlockedAllLayers(sx, y);
      setTileBlockedAllLayers(ex, y);
    }
  }

  /**
   * Sets the rectangle defined by (sx, sy) to (ex, ex) in <em>all</em> layers
   * as a blockage.
   */
  public void setBlockage(int sx, int sy, int ex, int ey, int z)
  {
    for (int x = sx; x <= ex; ++x)
    {
      setTileBlocked(x, sy, z, true);
      setTileBlocked(x, ey, z, true);
    }
    for (int y = sy; y <= ey; ++y)
    {
      setTileBlocked(sx, y, z, true);
      setTileBlocked(ex, y, z, true);
    }
  }

  // private void setTileBlocked(int x, int y, int z)
  // {
  // AStarRegionNode node = nodeCoordsToRegion(x, y, z);
  // node.routingMap.setTileBlocked(nodeGlobalToLocalX(x),
  // nodeGlobalToLocalY(y), 0, true);
  // }

  private void setTileBlocked(int x, int y, int z, boolean blockedStatus)
  {
    AStarRegionNode node = nodeCoordsToRegion(x, y, z);
    assert (node != null);
    node.routingMap.setTileBlocked(nodeGlobalToLocalX(x), nodeGlobalToLocalY(y), 0, blockedStatus);
  }

  private void setTileBlockedAllLayers(int x, int y)
  {
    for (int z = 0; z < this.layerCount; z++)
    {
      AStarRegionNode node = nodeCoordsToRegion(x, y, z);
      assert (node != null);
      node.routingMap.setTileBlocked(nodeGlobalToLocalX(x), nodeGlobalToLocalY(y), 0, true);
    }
  }

  /**
   * Places the portals on the borders of the maps of all regions. This must be
   * done <em>after</em> the blockages have been set.
   */
  public void placePortals()
  {
    AStarRegionNode myRegion;
    AStarRegionNode neighbourRegion;
    int maxXNodes = this.regionGrid.getMaxXNodes();
    int maxYNodes = this.regionGrid.getMaxYNodes();
    int maxZNodes = this.regionGrid.getMaxZNodes();
    for (int x = 0; x < maxXNodes; x++)
      for (int y = 0; y < maxYNodes; y++)
        for (int z = 0; z < maxZNodes; z++)
        {
          myRegion = this.regionGrid.nodeAt(x, y, z);

          // to the east
          if (x + 1 < maxXNodes)
          {
            neighbourRegion = this.regionGrid.nodeAt(x + 1, y, z);
            for (int rY = 0; rY < regionHeight; rY++)
              if (!myRegion.isTileBlocked(regionWidth - 1, rY) && !neighbourRegion.isTileBlocked(0, rY))
              {
                myRegion.setPortal(regionWidth - 1, rY, false);
                neighbourRegion.setPortal(0, rY, false);
              }

            // To the west, if this is a west-most region
            if (x == 0)
            {
              for (int rY = 0; rY < regionHeight; rY++)
                if (!myRegion.isTileBlocked(0, rY))
                {
                  myRegion.setPortal(0, rY, false);
                }
            }

          }
          else
          // if (x == (maxXNodes - 1))
          {
            for (int rY = 0; rY < regionHeight; rY++)
              if (!myRegion.isTileBlocked(regionWidth - 1, rY))
              {
                myRegion.setPortal(regionWidth - 1, rY, false);
              }
          }

          // to the north
          if (y > 0)
          {
            neighbourRegion = this.regionGrid.nodeAt(x, y - 1, z);
            for (int rX = 0; rX < regionWidth; rX++)
              if (!myRegion.isTileBlocked(rX, 0) && !neighbourRegion.isTileBlocked(rX, regionHeight - 1))
              {
                myRegion.setPortal(rX, 0, true);
                neighbourRegion.setPortal(rX, regionHeight - 1, true);
              }

            // To the south, if this is a region on the southern border of the
            // region grid
            if (y == (maxYNodes - 1))
            {
              for (int rX = 0; rX < regionWidth; rX++)
                if (!myRegion.isTileBlocked(rX, regionHeight - 1))
                {
                  myRegion.setPortal(rX, regionHeight - 1, true);
                }
            }

          }
          else
          // if (y == 0)
          {
            for (int rX = 0; rX < regionWidth; rX++)
              if (!myRegion.isTileBlocked(rX, 0))
              {
                myRegion.setPortal(rX, 0, true);
              }
          }
        }
  }

  /* Methods concerned with the region grid. */

  private int nodeToRegionX(int x)
  {
    return (int) Math.floor(x / regionWidth);
  }

  private int nodeToRegionY(int y)
  {
    return (int) Math.floor(y / regionHeight);
  }

  private int nodeGlobalToLocalX(int x)
  {
    return x % regionWidth;
  }

  private int nodeGlobalToLocalY(int y)
  {
    return y % regionHeight;
  }

  private int nodeLocalToGlobalX(int x, AStarRegionNode whichRegion)
  {
    return (x + regionToNodeX(whichRegion.getX()));
  }

  private int nodeLocalToGlobalY(int y, AStarRegionNode whichRegion)
  {
    return (y + regionToNodeY(whichRegion.getY()));
  }

  private int regionToNodeX(int x)
  {
    return x * regionWidth;
  }

  private int regionToNodeY(int y)
  {
    return y * regionHeight;
  }

  private AStarRegionNode nodeCoordsToRegion(int x, int y, int z)
  {
    return this.regionGrid.nodeAt(nodeToRegionX(x), nodeToRegionY(y), z);
  }
}

class LocalRouteJobComparator implements Comparator<LocalRouteJob>
{

  /*
   * (non-Javadoc)
   * 
   * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
   */
  public int compare(LocalRouteJob arg0, LocalRouteJob arg1)
  {
    if (arg0.numberInGlobalPath < arg1.numberInGlobalPath)
      return -1;
    if (arg0.numberInGlobalPath == arg1.numberInGlobalPath)
      return 0;
    return 1;
  }
}
