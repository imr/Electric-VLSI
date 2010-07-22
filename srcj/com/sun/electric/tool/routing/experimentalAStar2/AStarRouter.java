/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AStarRouter.java
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
package com.sun.electric.tool.routing.experimentalAStar2;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.routing.RoutingFrame.RoutingSegment;
import com.sun.electric.tool.routing.experimentalAStar2.concurrency.RouteJob;
import com.sun.electric.tool.routing.experimentalAStar2.concurrency.RoutingMain;
import com.sun.electric.tool.routing.experimentalAStar2.datastructures.Point3D;

public class AStarRouter extends BenchmarkRouter
{
  /* Below: Private members, not for tuning. */

  private static boolean isDebugModeOn;

  RoutingMain router;

  /**
   * Depicts the size of one node on the actual cell. This value is calculated
   * by the private method <code>calculateNodeSize</code> on initialisation of
   * the AStarRouter, so don't set it here.
   */
  private double nodeSize;

  /*
   * Used to translate node coordinates.
   */
  private int nodeOffsetX = 0;

  private int nodeOffsetY = 0;

  public AStarRouter()
  {
    super();
    setBenchmarkParameters(4, 60);
  }

  @Override
  public String getAlgorithmName()
  {
    return "A* - 2"; //$NON-NLS-1$
  }

  /**
   * Returns the next power of 2
   */
  private int nextPowerOf2(int value)
  {
    assert (value > 0);
    int x = 1;
    while (value > x)
      x *= 2;
    return x;
  }

  /**
   * Calculates the length of the sides of a node on the cell the router finds
   * paths on.
   * 
   * @param allLayers All layers of the cell.
   */
  private double calculateNodeSize(List<RoutingLayer> allLayers)
  {
    double maxMinWidth = 0;
    for (RoutingLayer layer : allLayers)
    {
      if (maxMinWidth < (layer.getMinWidth() + layer.getMinSpacing(layer)))
        maxMinWidth = (layer.getMinWidth() + layer.getMinSpacing(layer));
    }
    return maxMinWidth * 2;
  }

  /* Small helpers for unit conversion */
  /**
   * This method can be used for converting coordinates from the cell to node
   * coordinates, or other similar unit conversions.
   * 
   * @param c
   * @return
   */
  private int cellUnitToNode(double c)
  {
    return (int) Math.floor(c / nodeSize);
  }

//  /**
//   * This method can be used for converting node coordinates to cell
//   * coordinates, or other similar unit conversions.
//   * 
//   * @param n
//   * @return
//   */
//  private double nodeToCellUnit(int n)
//  {
//    return (double) (n * nodeSize);
//  }

  private int cellCoordXToNode(double c)
  {
    return cellUnitToNode(c) + this.nodeOffsetX;
  }

  private int cellCoordYToNode(double c)
  {
    return cellUnitToNode(c) + this.nodeOffsetY;
  }

  private double nodeCoordXToCell(int n)
  {
    return nodeSize * (n - this.nodeOffsetX) + nodeSize / 2;
  }

  private double nodeCoordYToCell(int n)
  {
    return nodeSize * (n - this.nodeOffsetY) + nodeSize / 2;
  }

  private RoutingLayer getLayerFromZ(int z)
  {
    return allLayers.get(z);
  }

  private int getZFromLayer(RoutingLayer layer)
  {
    return layer.getMetalNumber() - 1;
  }

  private List<RoutingLayer> allLayers;

  private List<RoutingContact> allContacts;

//  private void checkMetalLayers()
//  {
//    int lowestMetalLayer = Integer.MAX_VALUE;
//    int highestMetalLayer = -1;
//    int currentMetalLayer = -1;
//    int metalLayerCount = 0;
//    boolean hasSeenNonMetal = false;
//    for (RoutingLayer layer : allLayers)
//    {
//      if (layer.isMetal())
//      {
//        metalLayerCount++;
//        if (!hasSeenNonMetal)
//        {
//          if (layer.getMetalNumber() > currentMetalLayer)
//            currentMetalLayer = layer.getMetalNumber();
//          else
//            throw new IllegalStateException("Metal layers unsorted!");
//          if (currentMetalLayer < lowestMetalLayer)
//            lowestMetalLayer = currentMetalLayer;
//          if (currentMetalLayer > highestMetalLayer)
//            highestMetalLayer = currentMetalLayer;
//        }
//        else
//          throw new IllegalStateException("Metal layers below non-metal layers!");
//      }
//      else
//        hasSeenNonMetal = true;
//    }
//    DebugLog("Metal layers: count=" + metalLayerCount + ", lowest=" + lowestMetalLayer + ", highest=" + highestMetalLayer
//        + ", total layers=" + allLayers.size());
//  }

  @Override
  protected void runRouting(Cell cell, List<RoutingSegment> segmentsToRoute, List<RoutingLayer> allLayers,
      List<RoutingContact> allContacts, List<RoutingGeometry> blockages)
  {
    // Store start time for maximum runtime calculation
    long startTime = System.currentTimeMillis();

    /* Initialisation phase */
    isDebugModeOn = false;// enableOutput.getBooleanValue();

    this.allLayers = allLayers;
    this.allContacts = allContacts;
    for (RoutingContact contact : allContacts)
    {
      DebugLog("Contact Via Spacing: " + contact.getViaSpacing());
    }
    // User-interface initialization
    Job.getUserInterface().startProgressDialog("Routing " + segmentsToRoute.size() + " segments", null);
    Job.getUserInterface().setProgressNote("Initialising...");
    Job.getUserInterface().setProgressValue(0);
    nodeSize = calculateNodeSize(allLayers);

    //
    // Determine bounds
    //
    int min_x = cellUnitToNode(cell.getBounds().getMinX());
    int min_y = cellUnitToNode(cell.getBounds().getMinY());
    int max_x = cellUnitToNode(cell.getBounds().getMaxX());
    int max_y = cellUnitToNode(cell.getBounds().getMaxY());
    final int border = 10;
    int real_width = max_x - min_x + 1;
    int real_height = max_y - min_y + 1;
    int width = real_width + 2 * border;
    int height = real_height + 2 * border;
    int quadlen = nextPowerOf2((width > height) ? width : height);

    // XXX: we need a minimum width, otherwise we can't divide it well
    if (quadlen < 128)
      quadlen = 128;

    //
    // Node coordinate translation happens here.
    //
    nodeOffsetX = -min_x + (quadlen - real_width) / 2; // move into center
    nodeOffsetY = -min_y + (quadlen - real_height) / 2; // move into center

    // System.out.println("" + min_x + "/" + min_y + " -- " + max_x + "/" +
    // max_y);
    // System.out.println("" + quadlen);

    router = new RoutingMain(quadlen, quadlen, getMetalLayerCount(allLayers), numThreads.getTempIntValue());

    for (RoutingGeometry routingGeometry : blockages)
    {
      Rectangle2D bounds = routingGeometry.getBounds();
      int startX = cellCoordXToNode(bounds.getX());
      int startY = cellCoordYToNode(bounds.getY());
      int endX = cellCoordXToNode(Math.ceil(bounds.getX() + bounds.getWidth()));
      int endY = cellCoordYToNode(Math.ceil(bounds.getY() + bounds.getHeight()));
      int layer = getZFromLayer(routingGeometry.getLayer());
      router.setBlockage(startX, startY, endX, endY, layer);
    }

    // Place portals on region borders.
    router.placePortals();

    // Sort segments according to their expected length. This is good because 1)
    // with limited time, we can finish more paths successfully and 2) this
    // improves path quality since short paths don't make many detours.
    Collections.sort(segmentsToRoute, new SegmentComparator());

    /* End of init phase */

    Job.getUserInterface().setProgressNote("Scheduling segments to route...");
    for (RoutingSegment rs : segmentsToRoute)
    {
      RoutePoint rpSTART = new RoutePoint(RoutingContact.STARTPOINT, rs.getStartEnd().getLocation(), 0);
      RoutePoint rpEND = new RoutePoint(RoutingContact.FINISHPOINT, rs.getFinishEnd().getLocation(), 0);

      Point3D ptSTART = new Point3D(cellCoordXToNode(rpSTART.getLocation().getX()),
          cellCoordYToNode(rpSTART.getLocation().getY()), getZFromLayer(rs.getStartLayers().get(0)));

      Point3D ptEND = new Point3D(cellCoordXToNode(rpEND.getLocation().getX()), cellCoordYToNode(rpEND.getLocation().getY()),
          getZFromLayer(rs.getFinishLayers().get(0)));

      // System.out.print("Submit job: Segment (" +
      // (rpSTART.getLocation().getX()) + "/" + (rpSTART.getLocation().getY()) +
      // ")->("
      // + (rpEND.getLocation().getX()) + "/" + (rpEND.getLocation().getY()) +
      // ")");
      // System.out.print(" = Node-Route (" + (ptSTART.getX()) + "/" +
      // (ptSTART.getY()) + ")");
      // System.out.println("->(" + (ptEND.getX()) + "/" + (ptEND.getY()) +
      // ")");
      MyRouteJob job = new MyRouteJob(ptSTART, ptEND, this);
      job.routingSegment = rs;
      router.submitRouteJob(job);
    }

    // Now that all jobs are submitted,
    // we're ready to let the router do its work.
    Job.getUserInterface().setProgressNote("Routing segments...");
    // System.out.println("waitForCompletion");

    // After initialising and submitting jobs (which took time, too), use the
    // rest of the maxRuntime for routing.
    long restTime = (maxRuntime.getIntValue() * 1000) - (System.currentTimeMillis() - startTime);
    router.setMaxRuntimeMillis(restTime);
    try
    {
      router.waitForCompletion();
    }
    catch (Exception e)
    {
      router.shutDown();
    }

    Job.getUserInterface().stopProgressDialog();
  }

  private int getMetalLayerCount(List<RoutingLayer> allLayers)
  {
    int highestMetalNumber = 0;
    for (RoutingLayer routingLayer : allLayers)
    {
      if (routingLayer.isMetal())
      {
        DebugLog("Layer " + routingLayer.getMetalNumber() + " Via Spacing: " + routingLayer.getPin().getViaSpacing()
            + " MinSpacing: " + routingLayer.getMinWidth());
        highestMetalNumber = Math.max(highestMetalNumber, routingLayer.getMetalNumber());
      }
    }
    return highestMetalNumber;
  }

  synchronized void routeJobCompleted(RouteJob job)
  {
    Job.getUserInterface().setProgressValue(router.getProgress());
    List<Point3D> path = job.getPath();
    if (path != null)
    {
      RoutingSegment segment = job.routingSegment;

      // if (checkOnAngularPointsInPath(path))
      // System.err.println("AStarRouter: Path contains angular lines BEFORE converting! ("
      // + job.from.getX() + ","
      // + job.from.getY() + ") -> (" + job.to.getX() + "," + job.to.getY() +
      // ")");

      path = optimizePath(path);
      PathConversionData data = new PathConversionData();
      List<RoutePoint> routePoints = convertPathToRoutePoints(path, segment, data);
      // if (checkOnAngularRoutePoints(routePoints))
      // System.err.println("AStarRouter: Path contains angular lines AFTER converting! ("
      // + job.from.getX() + ","
      // + job.from.getY() + ") -> (" + job.to.getX() + "," + job.to.getY() +
      // ")");

      RoutingLayer layer;

      /* Really short path */
      if (path.size() == 1)
      {
        // Use finish layer, because path could switch layers right after the
        // first point
        layer = segment.getFinishLayers().get(0);
        segment.addWireEnd(routePoints.get(0));
        segment.addWireEnd(routePoints.get(1));
        segment.addWireEnd(routePoints.get(2));
        segment.addWire(new RouteWire(segment.getStartLayers().get(0), routePoints.get(0), routePoints.get(1), layer
            .getMinWidth()));
        segment.addWire(new RouteWire(layer, routePoints.get(1), routePoints.get(2), layer.getMinWidth()));
        return;
      }

      /*
       * Connect the start points (including the adapted points). These are
       * definitely on the same layer.
       */
      layer = segment.getStartLayers().get(0);
      // The conversion adds at least one point at the front of the path, so
      // this is safe to do.
      segment.addWireEnd(routePoints.get(0));
      segment.addWireEnd(routePoints.get(1));
      segment.addWire(new RouteWire(layer, routePoints.get(0), routePoints.get(1), layer.getMinWidth()));
      // When the conversion added another point at the front of the path, add
      // the third route point as well.
      if (data.startPointsAdded == 1)
      {
        segment.addWireEnd(routePoints.get(2));
        segment.addWire(new RouteWire(layer, routePoints.get(1), routePoints.get(2), layer.getMinWidth()));
      }

      /*
       * Connect the points in between start and end points.
       */
      for (int i = 0; i < (path.size() - 1); i++)
      {
        if (path.get(i).getZ() != path.get(i + 1).getZ())
        {
          layer = getLayerFromZ(path.get(i + 1).getZ());
          /*
           * int currentRoutePointIndex = i + 2 + data.startPointsAdded;
           * RoutingLayer startLayer = getLayerFromZ(path.get(i).getZ());
           * RoutingLayer finishLayer = getLayerFromZ(path.get(i+1).getZ());
           * RoutingContact viaContact = getVia(startLayer, finishLayer);
           * 
           * RoutePoint startPoint = routePoints.get(currentRoutePointIndex -
           * 1); Point2D.Double viaCoords = new
           * Point2D.Double(startPoint.getLocation().getX(),
           * startPoint.getLocation().getY()); // XXX: Coordinates of viaPoint
           * RoutePoint viaPoint = new RoutePoint(viaContact, viaCoords,
           * segment.getNetID()); RoutePoint finishPoint =
           * routePoints.get(currentRoutePointIndex);
           * 
           * RouteWire start_via = new RouteWire(startLayer, startPoint,
           * viaPoint, startLayer.getMinWidth()); RouteWire via_finish = new
           * RouteWire(finishLayer, viaPoint, finishPoint,
           * finishLayer.getMinWidth());
           * 
           * // startPoint has already been added as WireEnd in the last
           * iteration of the for loop segment.addWireEnd(finishPoint);
           * segment.addWireEnd(viaPoint);
           * 
           * segment.addWire(start_via); segment.addWire(via_finish);
           */
        }
        else
        {
          // No change in layer
          layer = getLayerFromZ(path.get(i).getZ());
          /*
           * int currentRoutePointIndex = i + 2 + data.startPointsAdded;
           * segment.addWireEnd(routePoints.get(currentRoutePointIndex));
           * segment.addWire(new RouteWire(layer,
           * routePoints.get(currentRoutePointIndex - 1),
           * routePoints.get(currentRoutePointIndex), layer.getMinWidth()));
           */
        }
        int currentRoutePointIndex = i + 2 + data.startPointsAdded;
        segment.addWireEnd(routePoints.get(currentRoutePointIndex));
        segment.addWire(new RouteWire(layer, routePoints.get(currentRoutePointIndex - 1),
            routePoints.get(currentRoutePointIndex), layer.getMinWidth()));
      }

      /*
       * Connect the end points (including the adapted points). The end points
       * are on the same layer, too.
       */
      // When the conversion added another point at the end of the path, add
      // the third last route point as well.
      if (data.endPointsAdded == 1)
      {
        segment.addWireEnd(routePoints.get(routePoints.size() - 2));
        segment.addWire(new RouteWire(layer, routePoints.get(routePoints.size() - 3), routePoints.get(routePoints.size() - 2),
            layer.getMinWidth()));
      }
      // The conversion adds at least one point at the end of the path, so
      // this is safe to do as well.
      layer = segment.getFinishLayers().get(0);
      // segment.addWireEnd(routePoints.get(routePoints.size() - 2));
      segment.addWireEnd(routePoints.get(routePoints.size() - 1));
      segment.addWire(new RouteWire(layer, routePoints.get(routePoints.size() - 2), routePoints.get(routePoints.size() - 1),
          layer.getMinWidth()));
    }
  }

  public RoutingContact getVia(RoutingLayer l1, RoutingLayer l2)
  {
    for (RoutingContact rc : allContacts)
    {
      if ((rc.getFirstLayer() == l1 && rc.getSecondLayer() == l2) || (rc.getFirstLayer() == l2 && rc.getSecondLayer() == l1))
      {
        return rc;
      }
    }
    throw new IllegalArgumentException("Between the layers " + l1.getName() + " and " + l2.getName()
        + ", no via contact could be found.");
  }

//  private boolean checkOnAngularRoutePoints(List<RoutePoint> routePoints)
//  {
//    RoutePoint last = null;
//    for (RoutePoint routePoint : routePoints)
//    {
//      if (last != null)
//        if ((routePoint.getLocation().getX() != last.getLocation().getX() && routePoint.getLocation().getY() != last
//            .getLocation().getY()))
//          // || (segment.getStartLayers().get(0)!=
//          // last.getContact().getFirstLayer() &&
//          // routePoint.getLocation().getY() != last
//          // .getLocation().getY())
//          // || (routePoint.getLocation().getX() != last.getLocation().getX() &&
//          // routePoint.getContact().getFirstLayer() != last
//          // .getContact().getFirstLayer()))
//          return true;
//      last = routePoint;
//    }
//    return false;
//  }

//  private boolean checkOnAngularPointsInPath(List<Point3D> path)
//  {
//    Point3D last = null;
//    for (Point3D point : path)
//    {
//      // if (point.getZ() != 0)
//      // System.err.println("Used layer " + point.getZ());
//      if (last != null)
//        if ((point.getX() != last.getX() && point.getY() != last.getY())
//            || (point.getX() != last.getX() && point.getZ() != last.getZ())
//            || (point.getY() != last.getY() && point.getZ() != last.getZ()))
//          return true;
//      last = point;
//    }
//    return false;
//  }

  private List<Point3D> optimizePath(List<Point3D> path)
  {
    if (path.size() < 3)
      return path;
    List<Point3D> result = new ArrayList<Point3D>();
    Point3D last = null;
    Point3D previousToLast = null;
    result.add(path.get(0));

    int i = 0;
    for (Point3D cur : path)
    {
      if (last != null)
      {
        if (last.getZ() != cur.getZ() && (i > 1))
        {
          result.add(last);
        }

        if (previousToLast != null)
          if ((previousToLast.getX() != cur.getX()) && (previousToLast.getY() != cur.getY()))
          {
            result.add(last);
          }
      }
      // if (checkOnAngularPointsInPath(result))
      // System.out.println("AStarRouter: Wrong conversion or true angular path detected! Path-ID:"
      // + path.hashCode());
      previousToLast = last;
      last = cur;
      ++i;
    }

    if (path.get(path.size() - 2).getZ() == path.get(path.size() - 1).getZ())
      result.add(path.get(path.size() - 1));

    // for (Point3D cur : result)
    // {
    // if (last != null)
    // {
    // if (last.getZ() == cur.getZ())
    // if (last.getY() == cur.getY())
    // if (last.getX() == cur.getX())
    // {
    // System.out.println("Warning: Duplicate points!");
    // for (Point3D current : result)
    // System.out.print(current.toString() + " ");
    // System.out.print("\n");
    // }
    //
    // }
    // last = cur;
    // }

    return result;

  }

  /**
   * 
   * @param path - the list of nodes in reversed order (the result list of the
   *          route() method after backtracking)
   * @param layer - the layer to operate on.
   * @return the list of RoutePoints in correct order from start to destination
   *         cell
   */
  private List<RoutePoint> convertPathToRoutePoints(List<Point3D> path, RoutingSegment segment, PathConversionData data)
  {
    List<RoutePoint> result = new ArrayList<RoutePoint>();

    // Start Point is always needed
    RoutePoint startPoint = new RoutePoint(RoutingContact.STARTPOINT, segment.getStartEnd().getLocation(), 0);
    result.add(startPoint);

    // End Point is always needed
    RoutePoint endPoint = new RoutePoint(RoutingContact.FINISHPOINT, segment.getFinishEnd().getLocation(), 0);
    RoutePoint smoothedEndPoint = null;

    Point3D first = null;
    Point3D second = null;

    if (path.size() > 1)
    {
      first = path.get(0);
      second = path.get(1);
      // Horizontal path starts here: Insert point on same y but with adapted x
      if (first.getX() != second.getX())
      {
        result.add(new RoutePoint(segment.getStartLayers().get(0).getPin(), new Point2D.Double(startPoint.getLocation().getX(),
            nodeCoordYToCell(first.getY())), 0));
        data.startPointsAdded = 1;
      }
      // Vertical path starts here: Insert point on same x but with adapted y
      else
      // if (first.getY() != second.getY())
      {
        result.add(new RoutePoint(segment.getStartLayers().get(0).getPin(), new Point2D.Double(nodeCoordXToCell(first.getX()),
            startPoint.getLocation().getY()), 0));
        data.startPointsAdded = 1;
      }

      Point3D last = null;
      for (Point3D cur : path)
      {
        RoutingContact pin = null;
        // If the two points reside on different layers, use a special via
        // contact.
        if (last != null)
        {
          if (last.getZ() != cur.getZ())
            pin = getVia(getLayerFromZ(last.getZ()), getLayerFromZ(cur.getZ()));
          else
            pin = getLayerFromZ(last.getZ()).getPin();
          result.add(new RoutePoint(pin, new Point2D.Double(nodeCoordXToCell(last.getX()), nodeCoordYToCell(last.getY())), 0));

          // Special case: Last point
          if ((cur == path.get(path.size() - 1)) && (getLayerFromZ(cur.getZ()) != segment.getFinishLayers().get(0)))
          {
            pin = getVia(getLayerFromZ(cur.getZ()), segment.getFinishLayers().get(0));
            result.add(new RoutePoint(pin, new Point2D.Double(nodeCoordXToCell(cur.getX()), nodeCoordYToCell(cur.getY())), 0));
          }
        }
        // if (checkOnAngularRoutePoints(result))
        // System.out.println("AStarRouter: Wrong conversion or true angular path detected! Path-ID:"
        // + path.hashCode());
        last = cur;
      }

      first = path.get(path.size() - 2);
      second = path.get(path.size() - 1);
      // Horizontal path ends here: Insert point on same y but with adapted x
      if (first.getX() != second.getX())
      {
        smoothedEndPoint = new RoutePoint(segment.getFinishLayers().get(0).getPin(), new Point2D.Double(endPoint.getLocation()
            .getX(), nodeCoordYToCell(first.getY())), 0);
        data.endPointsAdded = 1;
      }
      // Vertical path ends here: Insert point on same x but with adapted y
      else
      // if (first.getY() != second.getY())
      {
        smoothedEndPoint = new RoutePoint(segment.getFinishLayers().get(0).getPin(), new Point2D.Double(
            nodeCoordXToCell(first.getX()), endPoint.getLocation().getY()), 0);
        data.endPointsAdded = 1;
      }
    }
    else
    {
      smoothedEndPoint = new RoutePoint(segment.getFinishLayers().get(0).getPin(), new Point2D.Double(endPoint.getLocation()
          .getX(), startPoint.getLocation().getY()), 0);
      data.endPointsAdded = 1;
    }

    if (smoothedEndPoint != null)
      result.add(smoothedEndPoint);
    result.add(endPoint);

    return result;
  }

  // Method doesn't need synchronized keyword:
  // isDebugModeOn is set once at the beginning, and only read afterwards.
  // Furthermore, System.out.println() will handle the rest.
  public static void DebugLog(String message)
  {
    if (isDebugModeOn)
      System.out.println(message);
  }
}

class MyRouteJob extends RouteJob
{
  private AStarRouter astarrouter;

  public MyRouteJob(Point3D from, Point3D to, AStarRouter astarrouter)
  {
    super(from, to);
    this.astarrouter = astarrouter;
  }

  public void onCompletion()
  {
    astarrouter.routeJobCompleted(this);
  }
}

class SegmentComparator implements Comparator<RoutingSegment>
{
  /*
   * (non-Javadoc)
   * 
   * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
   */
  public int compare(RoutingSegment arg0, RoutingSegment arg1)
  {
    double length0 = arg0.getStartEnd().getLocation().distance(arg0.getFinishEnd().getLocation());
    double length1 = arg1.getStartEnd().getLocation().distance(arg1.getFinishEnd().getLocation());
    if (length0 < length1)
      return -1;
    else if (length0 == length1)
      return 0;
    else
      return 1;
  }
}

class PathConversionData
{
  public int startPointsAdded = 0;

  public int endPointsAdded = 0;
}