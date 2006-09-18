/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AutoStitch.java
 * Routing tool: Auto-Stitcher (places wires where geometry touches).
 * Written by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
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
package com.sun.electric.tool.routing;

import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.ObjectQTree;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyMerge;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.PortOriginal;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.routing.RouteElement.RouteElementAction;
import com.sun.electric.tool.user.CircuitChangeJobs;
import java.util.Collections;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * Class which implements the Auto Stitching tool.
 */
public class AutoStitch
{
	/** true to use Quad-trees for port searching */				private static final boolean USEQTREE = true;

    /** router used to wire */  									private static InteractiveRouter router = new SimpleWirer();
	/** list of all routes to be created at end of analysis */		private static List<Route> allRoutes;
	/** sets of netlists that will be connected */					private static Pairs intendedPairs;
	/** list of pins that may be inline pins due to created arcs */	private static HashSet<NodeInst> possibleInlinePins;
	private static HashSet<NodeInst> nodeMark;

	/**
	 * Method to do auto-stitching.
	 * @param highlighted true to stitch only the highlighted objects.
	 * False to stitch the entire current cell.
	 * @param forced true if the stitching was explicitly requested (and so results should be printed).
	 */
	public static void autoStitch(boolean highlighted, boolean forced)
	{
		UserInterface ui = Job.getUserInterface();
		Cell cell = ui.needCurrentCell();
		if (cell == null) return;

		List<NodeInst> nodesToStitch = null;
		List<ArcInst> arcsToStitch = null;
		Rectangle2D limitBound = null;

		if (highlighted)
		{
			nodesToStitch = new ArrayList<NodeInst>();
            arcsToStitch = new ArrayList<ArcInst>();
	        EditWindow_ wnd = ui.getCurrentEditWindow_();
	        if (wnd == null) return;
			List<Geometric> highs = wnd.getHighlightedEObjs(true, true);
			limitBound = wnd.getHighlightedArea();
			for(Geometric geom : highs)
			{
				ElectricObject eObj = (ElectricObject)geom;
				if (eObj instanceof PortInst) eObj = ((PortInst)eObj).getNodeInst();
				if (eObj instanceof NodeInst)
				{
					NodeInst ni = (NodeInst)eObj;
					if (!ni.isCellInstance())
					{
						PrimitiveNode pnp = (PrimitiveNode)ni.getProto();
						if (pnp.getTechnology() == Generic.tech) continue;
						if (pnp.getFunction() == PrimitiveNode.Function.NODE) continue;
					}
					nodesToStitch.add((NodeInst)eObj);
				} else if (eObj instanceof ArcInst)
				{
					arcsToStitch.add((ArcInst)eObj);
				}
			}
            if (nodesToStitch.size() == 0 && arcsToStitch.size() == 0)
            {
                if (forced) System.out.println("Nothing selected to auto-route");
                return;
            }
		}

		double lX = 0, hX = 0, lY = 0, hY = 0;
		if (limitBound != null)
		{
			lX = limitBound.getMinX();
			hX = limitBound.getMaxX();
			lY = limitBound.getMinY();
			hY = limitBound.getMaxY();
		}

		// find out the prefered routing arc
		new AutoStitchJob(cell, nodesToStitch, arcsToStitch, lX, hX, lY, hY, forced);
	}

	/**
	 * Class to do auto-stitching in a new thread.
	 */
	private static class AutoStitchJob extends Job
	{
		private Cell cell;
		private List<NodeInst> nodesToStitch;
		private List<ArcInst> arcsToStitch;
		private double lX, hX, lY, hY;
		private boolean forced;
 
		private AutoStitchJob(Cell cell, List<NodeInst> nodesToStitch, List<ArcInst> arcsToStitch,
                              double lX, double hX, double lY, double hY, boolean forced)
		{
			super("Auto-Stitch", Routing.getRoutingTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.nodesToStitch = nodesToStitch;
			this.arcsToStitch = arcsToStitch;
			this.lX = lX;
			this.hX = hX;
			this.lY = lY;
			this.hY = hY;
			this.forced = forced;
            setReportExecutionFlag(true);
			startJob();
		}

		public boolean doIt() throws JobException
		{
			Rectangle2D limitBound = null;
			if (lX != hX && lY != hY)
				limitBound = new Rectangle2D.Double(lX, lY, hX-lX, hY-lY);
			runAutoStitch(cell, nodesToStitch, arcsToStitch, null, limitBound, forced);
			return true;
		}
	}

    /**
     * This is the public interface for Auto-stitching when done in batch mode.
     * @param cell the cell in which to stitch.
     * @param nodesToStitch a list of NodeInsts to stitch (null to use all in the cell).
     * @param arcsToStitch a list of ArcInsts to stitch (null to use all in the cell).
     * @param stayInside is the area in which to route (null to route arbitrarily).
	 * @param limitBound if not null, only consider errors that occur in this area.
     * @param forced true if the stitching was explicitly requested (and so results should be printed).
     */
	public static void runAutoStitch(Cell cell, List<NodeInst> nodesToStitch, List<ArcInst> arcsToStitch,
                                     PolyMerge stayInside, Rectangle2D limitBound, boolean forced)
	{
        ArcProto preferredArc = Routing.getPreferredRoutingArcProto();

        if (nodesToStitch == null) // no data from highlighter
        {
            nodesToStitch = new ArrayList<NodeInst>();
			for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				if (ni.isIconOfParent()) continue;
				if (!ni.isCellInstance())
				{
					PrimitiveNode pnp = (PrimitiveNode)ni.getProto();
					if (pnp.getTechnology() == Generic.tech) continue;
					if (pnp.getFunction() == PrimitiveNode.Function.NODE) continue;
				}
				nodesToStitch.add(ni);
			}
        }
        if (arcsToStitch == null)
        {
            arcsToStitch = new ArrayList<ArcInst>();
			for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
				arcsToStitch.add(it.next());
        }

		allRoutes = new ArrayList<Route>();
		intendedPairs = new Pairs();
        possibleInlinePins = new HashSet<NodeInst>();

		// next pre-compute bounds on all nodes in cells to be changed
		HashMap<NodeInst, Rectangle2D[]> nodeBounds = new HashMap<NodeInst, Rectangle2D[]>();
		HashMap<NodeInst, ObjectQTree> nodePortBounds = new HashMap<NodeInst, ObjectQTree>();
		for(Iterator<NodeInst> nIt = cell.getNodes(); nIt.hasNext(); )
		{
			NodeInst ni = nIt.next();

			// remember bounding box for each port
			int total = ni.getProto().getNumPorts();
			Rectangle2D [] bbArray = new Rectangle2D[total];
			int i = 0;
			for(Iterator<PortProto> pIt = ni.getProto().getPorts(); pIt.hasNext(); )
			{
				PortProto pp = pIt.next();
				PortOriginal fp = new PortOriginal(ni, pp);
				AffineTransform trans = fp.getTransformToTop();
				NodeInst rNi = fp.getBottomNodeInst();

				Rectangle2D bounds = new Rectangle2D.Double(rNi.getAnchorCenterX() - rNi.getXSize()/2, 
					rNi.getAnchorCenterY() - rNi.getYSize()/2, rNi.getXSize(), rNi.getYSize());
				DBMath.transformRect(bounds, trans);
				bbArray[i++] = bounds;
			}
			nodeBounds.put(ni, bbArray);

			// remember quad-tree for ports on the node
			if (USEQTREE)
			{
				ObjectQTree oqt = new ObjectQTree(ni.getBounds());
				for(Iterator<PortInst> it = ni.getPortInsts(); it.hasNext(); )
				{
					PortInst pi = it.next();
					PortProto pp = pi.getPortProto();
					PortOriginal fp = new PortOriginal(ni, pp);
					AffineTransform trans = fp.getTransformToTop();
					NodeInst rNi = fp.getBottomNodeInst();

					Rectangle2D bounds = new Rectangle2D.Double(rNi.getAnchorCenterX() - rNi.getXSize()/2, 
						rNi.getAnchorCenterY() - rNi.getYSize()/2, rNi.getXSize(), rNi.getYSize());
					DBMath.transformRect(bounds, trans);
					oqt.add(pi, bounds);
				}
				nodePortBounds.put(ni, oqt);
			}
		}

		// next mark nodes to be checked
		nodeMark = new HashSet<NodeInst>();
		for(NodeInst ni : nodesToStitch)
		{
			nodeMark.add(ni);
		}

		// finally, initialize the information about which layer is smallest on each arc
		HashMap<ArcProto,Layer> arcLayers = new HashMap<ArcProto,Layer>();

		// now run through the nodeinsts to be checked for stitching
		int count = 0;
        HashMap<ArcProto, Integer> arcCount = new HashMap<ArcProto, Integer>();
		for(NodeInst ni : nodesToStitch)
		{
			if (cell.isAllLocked()) continue;
			Netlist netlist = cell.acquireUserNetlist();
			if (netlist == null)
			{
				System.out.println("Sorry, a deadlock aborted auto-routing (network information unavailable).  Please try again");
				break;
			}
			count += checkStitching(ni, arcCount, nodeBounds, nodePortBounds, arcLayers, stayInside, netlist, limitBound, preferredArc);
		}

		// now run through the arcinsts to be checked for stitching
		for(ArcInst ai : arcsToStitch)
		{
			if (!ai.isLinked()) continue;
			if (cell.isAllLocked()) continue;

			// only interested in arcs that are wider than their nodes (and have geometry that sticks out)
			if (!arcTooWide(ai)) continue;

			Netlist netlist = cell.acquireUserNetlist();
			if (netlist == null)
			{
				System.out.println("Sorry, a deadlock aborted auto-routing (network information unavailable).  Please try again");
				break;
			}
			count += checkStitching(ai, arcCount, nodeBounds, nodePortBounds, arcLayers, stayInside, netlist, limitBound, preferredArc);
		}

		// report results
		if (forced)
		{
			if (count != 0)
			{
	            StringBuffer buf = new StringBuffer();
	            buf.append("AUTO ROUTING: added ");
	            boolean first = true;
	            for (ArcProto ap : arcCount.keySet()) {
	                if (!first) buf.append("; ");
	                Integer c = arcCount.get(ap);
	                buf.append(c + " " + ap.describe() + " wires");
	                first = false;
	            }
	            System.out.println(buf.toString());
			} else
			{
				System.out.println("No arcs added");
			}
		}

		// clean up
		nodeMark = null;

        // create the routes
		Collections.sort(allRoutes, new compRoutes());
        for (Route route : allRoutes)
        {
            RouteElement re = route.get(0);
            Cell c = re.getCell();

            // see if the route is unnecessary because of existing connections
            RouteElementPort start = route.getStart();
            RouteElementPort end = route.getEnd();
            PortInst startPi = start.getPortInst();
            PortInst endPi = end.getPortInst();
            if (startPi != null && endPi != null)
            {
				boolean already = false;
				for(Iterator<Connection> cIt = startPi.getConnections(); cIt.hasNext(); )
				{
					Connection con = cIt.next();
					ArcInst existingAI = con.getArc();
					if (existingAI.getHead() == con)
					{
						if (existingAI.getTail().getPortInst() == endPi) { already = true;   break; }
					} else
					{
						if (existingAI.getHead().getPortInst() == endPi) { already = true;   break; }
					}
				}
				if (already) continue;
            }

            // if requesting no new geometry, make sure all arcs are default width
            if (stayInside != null)
            {
	            for (RouteElement obj : route)
	            {
	                if (obj instanceof RouteElementArc)
	                {
	                    RouteElementArc reArc = (RouteElementArc)obj;
	                    if (reArc.getAction() != RouteElementAction.deleteArc)
	                    {
		                    Point2D head = reArc.getHeadConnPoint();
		                    Point2D tail = reArc.getTailConnPoint();
	
		                    // insist that minimum size arcs be used
		                    ArcProto ap = reArc.getArcProto();
			            	Layer arcLayer = ap.getLayers()[0].getLayer();
		                    double width = ap.getDefaultWidth();

		                    if (!arcInMerge(head, tail, reArc.getArcWidth(), stayInside, arcLayer))
		                    {
			                    // current arc doesn't fit, try reducing by a small amount
		                    	double tinyAmountLess = reArc.getArcWidth() - DBMath.getEpsilon();
			                    if (arcInMerge(head, tail, tinyAmountLess, stayInside, arcLayer))
			                    {
				                    // smaller width works: set it
		                    		reArc.setArcWidth(tinyAmountLess);
			                    } else if (arcInMerge(head, tail, ap.getDefaultWidth(), stayInside, arcLayer))
			                    {
				                    // default size arc fits, use it
			                    	reArc.setArcWidth(ap.getDefaultWidth());
			                    } else
			                    {
				                    // default size arc doesn't fit, make it zero-size
		                    		reArc.setArcWidth(ap.getWidthOffset());
			                    }
		                    }
	                    }
	                }
	            }
            }
            Router.createRouteNoJob(route, c, false, false);
        }

        // check for any inline pins due to created wires
        List<CircuitChangeJobs.Reconnect> pinsToPassThrough = new ArrayList<CircuitChangeJobs.Reconnect>();
        for (NodeInst ni : possibleInlinePins) {
            if (ni.isInlinePin()) {
            	CircuitChangeJobs.Reconnect re = CircuitChangeJobs.Reconnect.erasePassThru(ni, false, true);
                if (re != null) {
                    pinsToPassThrough.add(re);
                }
            }
        }
        if (pinsToPassThrough.size() > 0)
        {
        	CircuitChangeJobs.CleanupChanges job = new CircuitChangeJobs.CleanupChanges(cell, true, new ArrayList<NodeInst>(),
                pinsToPassThrough, new HashMap<NodeInst,EPoint>(), new ArrayList<NodeInst>(), new HashSet<ArcInst>(), 0, 0, 0);
        	try
        	{
        		job.doIt();
        	} catch (JobException e)
        	{
        	}
        }
	}

	public static class compRoutes implements Comparator<Route>
	{
    	public int compare(Route r1, Route r2)
        {
    		// separate nodes from arcs
			RouteElementPort r1s = r1.getStart();
			RouteElementPort r1e = r1.getEnd();
			RouteElementPort r2s = r2.getStart();
			RouteElementPort r2e = r2.getEnd();
			boolean r1ToArc = r1s.getPortInst() == null || r1e.getPortInst() == null;
			boolean r2ToArc = r2s.getPortInst() == null || r2e.getPortInst() == null;
			if (r1ToArc && !r2ToArc) return 1;
			if (!r1ToArc && r2ToArc) return -1;
			if (r1ToArc && r2ToArc)
			{
				ArcProto ap1 = null, ap2 = null;
				if (r1s.getNewArcs().hasNext()) ap1 = ((RouteElementArc)(r1s.getNewArcs().next())).getArcProto();
				if (r1e.getNewArcs().hasNext()) ap1 = ((RouteElementArc)(r1e.getNewArcs().next())).getArcProto();
				if (r2s.getNewArcs().hasNext()) ap2 = ((RouteElementArc)(r2s.getNewArcs().next())).getArcProto();
				if (r2e.getNewArcs().hasNext()) ap2 = ((RouteElementArc)(r2e.getNewArcs().next())).getArcProto();
				if (ap1 == null || ap2 == null) return 0;
				return ap1.compareTo(ap2);
			}

			// get the first route in proper order
			NodeInst n1s = r1s.getPortInst().getNodeInst();
			NodeInst n1e = r1e.getPortInst().getNodeInst();
			if (n1s.compareTo(n1e) < 0)
			{
				NodeInst s = n1s;   n1s = n1e;   n1e = s;
				RouteElementPort se = r1s;   r1s = r1e;   r1e = se;
			}

			// get the second route in proper order
			NodeInst n2s = r2s.getPortInst().getNodeInst();
			NodeInst n2e = r2e.getPortInst().getNodeInst();
			if (n2s.compareTo(n2e) < 0)
			{
				NodeInst s = n2s;   n2s = n2e;   n2e = s;
				RouteElementPort se = r2s;   r2s = r2e;   r2e = se;
			}

			// sort by the starting and ending nodes
			int res = n1s.compareTo(n2s);
			if (res != 0) return res;
			res = n1e.compareTo(n2e);
			if (res != 0) return res;

			// sort by the starting and ending port names
			res = r1s.getPortInst().getPortProto().getName().compareTo(r2s.getPortInst().getPortProto().getName());
			if (res != 0) return res;
			res = r1e.getPortInst().getPortProto().getName().compareTo(r2e.getPortInst().getPortProto().getName());
			if (res != 0) return res;

//			// sort by the starting and ending port locations
//			Poly p1s = r1s.getPortInst().getPoly();
//			Poly p2s = r2s.getPortInst().getPoly();
//			double x1 = p1s.getCenterX();
//			double y1 = p1s.getCenterY();
//			double x2 = p2s.getCenterX();
//			double y2 = p2s.getCenterY();
//			if (x1 < x2) return 1;
//			if (x1 > x2) return -1;
//			if (y1 < y2) return 1;
//			if (y1 > y2) return -1;
//
//			Poly p1e = r1e.getPortInst().getPoly();
//			Poly p2e = r2e.getPortInst().getPoly();
//			x1 = p1e.getCenterX();
//			y1 = p1e.getCenterY();
//			x2 = p2e.getCenterX();
//			y2 = p2e.getCenterY();
//			if (x1 < x2) return 1;
//			if (x1 > x2) return -1;
//			if (y1 < y2) return 1;
//			if (y1 > y2) return -1;
			return 0;
        }
	}
   
	/**
	 * Method to determine if an arc is too wide for its ends.
	 * Arcs that are wider than their nodes stick out from those nodes,
	 * and their geometry must be considered, even though the nodes have been checked.
	 * @param ai the ArcInst to check.
	 * @return true if the arc is wider than its end nodes
	 */
	private static boolean arcTooWide(ArcInst ai)
	{
		boolean headTooWide = true;
		NodeInst hNi = ai.getHeadPortInst().getNodeInst();
		if (hNi.isCellInstance()) headTooWide = false; else
			if (ai.getWidth() <= hNi.getXSize() && ai.getWidth() <= hNi.getYSize()) headTooWide = false;

		boolean tailTooWide = true;
		NodeInst tNi = ai.getTailPortInst().getNodeInst();
		if (tNi.isCellInstance()) tailTooWide = false; else
			if (ai.getWidth() <= tNi.getXSize() && ai.getWidth() <= tNi.getYSize()) tailTooWide = false;

		return headTooWide || tailTooWide;
	}

	/**
	 * Method to tell whether or not a proposed arc fits into a PolyMerge area.
	 * @param head the coordinate of the head of the arc
	 * @param tail the coordinate of the tail of the arc.
	 * @param width the width of the arc.
     * @param stayInside is the area in which to route (null to route arbitrarily).
	 * @param layer the layer in the PolyMerge to test.
	 * @return true if the proposed arc is completely contained in the merge area.
	 * False if any part of the arc is not in the merge area.
	 */
	private static boolean arcInMerge(Point2D head, Point2D tail, double width, PolyMerge stayInside, Layer layer)
	{
	    // first see if the arc is zero-length
	    if (head.equals(tail))
	    {
	    	// degenerate arc: make a square
	    	Rectangle2D arcRect = new Rectangle2D.Double(head.getX()-width/2, head.getY()-width/2, width, width);
	    	if (stayInside.contains(layer, arcRect)) return true;
	    } else
	    {
	    	// normal arc: construct it
			Poly arcPoly = Poly.makeEndPointPoly(head.distance(tail), width, GenMath.figureAngle(head, tail),
				head, width/2, tail, width/2, Poly.Type.FILLED);
	    	if (stayInside.contains(layer, arcPoly)) return true;
	    }
	    return false;
	}

	/**
	 * Method to check an object for possible stitching to neighboring objects.
	 * @param geom
	 * @param arcCount
	 * @param nodeBounds
	 * @param nodePortBounds
	 * @param arcLayers
     * @param stayInside is the area in which to route (null to route arbitrarily).
	 * @param netlist
	 * @param limitBound if not null, only consider errors that occur in this area.
	 * @param preferredArc
	 * @return
	 */
	private static int checkStitching(Geometric geom, HashMap<ArcProto, Integer> arcCount, HashMap<NodeInst, Rectangle2D[]> nodeBounds,
		HashMap<NodeInst, ObjectQTree> nodePortBounds,
		HashMap<ArcProto,Layer> arcLayers, PolyMerge stayInside, Netlist netlist, Rectangle2D limitBound, ArcProto preferredArc)
	{
		Cell cell = geom.getParent();
		NodeInst ni = null;
		if (geom instanceof NodeInst) ni = (NodeInst)geom;

		// make a list of other geometrics that touch or overlap this one (copy it because the main list will change)
		List<Geometric> geomsInArea = new ArrayList<Geometric>();
		Rectangle2D geomBounds = geom.getBounds();
		double epsilon = DBMath.getEpsilon();
		Rectangle2D searchBounds = new Rectangle2D.Double(geomBounds.getMinX()-epsilon, geomBounds.getMinY()-epsilon,
			geomBounds.getWidth()+epsilon*2, geomBounds.getHeight()+epsilon*2);
		for(Iterator<Geometric> it = cell.searchIterator(searchBounds); it.hasNext(); )
			geomsInArea.add(it.next());
		int count = 0;
		for(Geometric oGeom : geomsInArea)
		{
			// find another node in this area
			if (oGeom instanceof ArcInst)
			{
				// other geometric is an ArcInst
				ArcInst oAi = (ArcInst)oGeom;

				if (ni == null)
				{
					// only interested in arcs that are wider than their nodes (and have geometry that sticks out)
					if (!arcTooWide(oAi)) continue;

					// compare arc "geom" against arc "oAi"
					count += compareTwoArcs((ArcInst)geom, oAi, stayInside, netlist, limitBound);
					continue;
				} else
				{
					// compare node "ni" against arc "oAi"
					count += compareNodeWithArc(ni, oAi, stayInside, netlist, limitBound);
				}
			} else
			{
				// other geometric a NodeInst
				NodeInst oNi = (NodeInst)oGeom;
				if (!oNi.isCellInstance())
				{
					PrimitiveNode pnp = (PrimitiveNode)oNi.getProto();
					if (pnp.getTechnology() == Generic.tech) continue;
					if (pnp.getFunction() == PrimitiveNode.Function.NODE) continue;
				}

				if (ni == null)
				{
					// compare arc "geom" against node "oNi"
					count += compareNodeWithArc(oNi, (ArcInst)geom, stayInside, netlist, limitBound);
					continue;
				}

				// compare node "ni" against node "oNi"
				count += compareTwoNodes(ni, oNi, arcCount, nodeBounds, nodePortBounds, arcLayers, stayInside, netlist, limitBound, preferredArc);
			}
		}
		return count;
	}

	/**
	 * Method to compare two nodes and see if they should be connected
	 * @param ni the first NodeInst to compare.
	 * @param oNi the second NodeInst to compare.
	 * @param arcCount
	 * @param nodeBounds
	 * @param nodePortBounds
	 * @param arcLayers
     * @param stayInside is the area in which to route (null to route arbitrarily).
	 * @param netlist
	 * @param limitBound if not null, only consider errors that occur in this area.
	 * @param preferredArc
	 * @return the number of connections made.
	 */
	private static int compareTwoNodes(NodeInst ni, NodeInst oNi, HashMap<ArcProto, Integer> arcCount,
		HashMap<NodeInst, Rectangle2D[]> nodeBounds, HashMap<NodeInst, ObjectQTree> nodePortBounds,
		HashMap<ArcProto,Layer> arcLayers, PolyMerge stayInside,
		Netlist netlist, Rectangle2D limitBound, ArcProto preferredArc)
	{
		int count = 0;

		// if both nodes are being checked, examine them only once
		if (nodeMark.contains(oNi) && oNi.getNodeIndex() <= ni.getNodeIndex()) return count;

		// now look at every layer in this node
		Rectangle2D oBounds = oNi.getBounds();
		if (ni.isCellInstance())
		{
			// complex node instance: look at all ports
			if (USEQTREE)
			{
				// find ports near this bound
				ObjectQTree oqt = nodePortBounds.get(ni);
				Rectangle2D biggerBounds = new Rectangle2D.Double(oBounds.getMinX()-1, oBounds.getMinY()-1, oBounds.getWidth()+2, oBounds.getHeight()+2);
				Set set = oqt.find(biggerBounds);
				if (set != null)
				{
					for (Object obj : set)
					{
						PortInst pi = (PortInst)obj;
						PortProto pp = pi.getPortProto();

						// find the primitive node at the bottom of this port
						AffineTransform trans = ni.rotateOut();
						NodeInst rNi = ni;
						PortProto rPp = pp;
						while (rNi.isCellInstance())
						{
							AffineTransform temp = rNi.translateOut();
							temp.preConcatenate(trans);
							Export e = (Export)rPp;
							rNi = e.getOriginalPort().getNodeInst();
							rPp = e.getOriginalPort().getPortProto();

							trans = rNi.rotateOut();
							trans.preConcatenate(temp);
						}

						// determine the smallest layer for all possible arcs
						ArcProto [] connections = pp.getBasePort().getConnections();
						for(int i=0; i<connections.length; i++)
						{
							findSmallestLayer(connections[i], arcLayers);
						}

						// look at all polygons on this nodeinst
						boolean usePortPoly = false;
						Poly [] nodePolys = shapeOfNode(rNi);
						int tot = nodePolys.length;
						if (tot == 0 || rNi.getProto() == Generic.tech.simProbeNode)
						{
							usePortPoly = true;
							tot = 1;
						}
						Netlist subNetlist = rNi.getParent().getUserNetlist();
						for(int j=0; j<tot; j++)
						{
							Layer layer = null;
							Poly poly = null;
							if (usePortPoly)
							{
								poly = ni.getShapeOfPort(pp);
								layer = poly.getLayer();
							} else
							{
								poly = nodePolys[j];

								// only want electrically connected polygons
								if (poly.getPort() == null) continue;

								// only want polygons on correct part of this nodeinst
								if (!subNetlist.portsConnected(rNi, rPp, poly.getPort())) continue;

								// transformed polygon
								poly.transform(trans);

								// if the polygon layer is pseudo, substitute real layer
								layer = poly.getLayer();
								if (layer != null) layer = layer.getNonPseudoLayer();
							}

							// see which arc can make the connection
							boolean connected = false;
							for(int pass=0; pass<2; pass++)
							{
								for(int i=0; i<connections.length; i++)
								{
									ArcProto ap = connections[i];
									if (pass == 0)
									{
										if (ap != preferredArc) continue;
									} else
									{
										if (ap == preferredArc) continue;
		
										// arc must be in the same technology
										if (ap.getTechnology() != rNi.getProto().getTechnology()) continue;
									}

									// this polygon must be the smallest arc layer
									if (!usePortPoly)
									{
										Layer oLayer = arcLayers.get(ap);
										if (!layer.getTechnology().sameLayer(oLayer, layer)) continue;
									}

									// pass it on to the next test
									connected = testPoly(ni, pp, ap, poly, oNi, netlist, nodeBounds, nodePortBounds, arcLayers, stayInside, limitBound);
									if (connected) {
		                                Integer c = arcCount.get(ap);
		                                if (c == null) c = new Integer(0);
		                                c = new Integer(c.intValue()+1);
		                                arcCount.put(ap, c);
		                                count++;
		                                break;
		                            }
								}
								if (connected) break;
							}
							if (connected) break;
						}
					}
				}
			} else
			{
				Rectangle2D [] boundArray = nodeBounds.get(ni);
				int bbp = 0;
				for(Iterator<PortProto> pIt = ni.getProto().getPorts(); pIt.hasNext(); )
				{
					PortProto pp = pIt.next();

					// first do a bounding box check
					if (boundArray != null)
					{
						Rectangle2D bounds = boundArray[bbp++];
						if (bounds.getMinX() > oBounds.getMaxX() || bounds.getMaxX() < oBounds.getMinX() ||
							bounds.getMinY() > oBounds.getMaxY() || bounds.getMaxY() < oBounds.getMinY()) continue;
					}

					// stop now if already an arc on this port to other node
	                /*
					boolean found = false;
					for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
					{
						Connection con = cIt.next();
						PortInst pi = con.getPortInst();
						if (pi.getPortProto() != pp) continue;
						if (con.getArc().getHeadPortInst().getNodeInst() == oNi ||
							con.getArc().getTailPortInst().getNodeInst() == oNi) { found = true;   break; }
					}
					if (found) continue;
	                */

					// find the primitive node at the bottom of this port
					AffineTransform trans = ni.rotateOut();
					NodeInst rNi = ni;
					PortProto rPp = pp;
					while (rNi.isCellInstance())
					{
						AffineTransform temp = rNi.translateOut();
						temp.preConcatenate(trans);
						Export e = (Export)rPp;
						rNi = e.getOriginalPort().getNodeInst();
						rPp = e.getOriginalPort().getPortProto();

						trans = rNi.rotateOut();
						trans.preConcatenate(temp);
					}

					// determine the smallest layer for all possible arcs
					ArcProto [] connections = pp.getBasePort().getConnections();
					for(int i=0; i<connections.length; i++)
					{
						findSmallestLayer(connections[i], arcLayers);
					}

					// look at all polygons on this nodeinst
					boolean usePortPoly = false;
					Poly [] nodePolys = shapeOfNode(rNi);
					int tot = nodePolys.length;
					if (tot == 0 || rNi.getProto() == Generic.tech.simProbeNode)
					{
						usePortPoly = true;
						tot = 1;
					}
					Netlist subNetlist = rNi.getParent().getUserNetlist();
					for(int j=0; j<tot; j++)
					{
						Layer layer = null;
						Poly poly = null;
						if (usePortPoly)
						{
							poly = ni.getShapeOfPort(pp);
							layer = poly.getLayer();
						} else
						{
							poly = nodePolys[j];

							// only want electrically connected polygons
							if (poly.getPort() == null) continue;

							// only want polygons on correct part of this nodeinst
							if (!subNetlist.portsConnected(rNi, rPp, poly.getPort())) continue;

							// transformed polygon
							poly.transform(trans);

							// if the polygon layer is pseudo, substitute real layer
							layer = poly.getLayer();
							if (layer != null) layer = layer.getNonPseudoLayer();
						}

						// see which arc can make the connection
						boolean connected = false;
						for(int pass=0; pass<2; pass++)
						{
							for(int i=0; i<connections.length; i++)
							{
								ArcProto ap = connections[i];
								if (pass == 0)
								{
									if (ap != preferredArc) continue;
								} else
								{
									if (ap == preferredArc) continue;
	
									// arc must be in the same technology
									if (ap.getTechnology() != rNi.getProto().getTechnology()) continue;
								}

								// this polygon must be the smallest arc layer
								if (!usePortPoly)
								{
									Layer oLayer = arcLayers.get(ap);
									if (!layer.getTechnology().sameLayer(oLayer, layer)) continue;
								}

								// pass it on to the next test
								connected = testPoly(ni, pp, ap, poly, oNi, netlist, nodeBounds, nodePortBounds, arcLayers, stayInside, limitBound);
								if (connected) {
	                                Integer c = arcCount.get(ap);
	                                if (c == null) c = new Integer(0);
	                                c = new Integer(c.intValue()+1);
	                                arcCount.put(ap, c);
	                                count++;
	                                break;
	                            }
							}
							if (connected) break;
						}
						if (connected) break;
					}
				}
			}
		} else
		{
			// primitive node: check its layers
			AffineTransform trans = ni.rotateOut();

			// save information about the other node
			double oX = oNi.getAnchorCenterX();
			double oY = oNi.getAnchorCenterY();

			// look at all polygons on this nodeinst
			boolean usePortPoly = false;
			Poly [] polys = shapeOfNode(ni);
			int tot = polys.length;
			if (tot == 0 || ni.getProto() == Generic.tech.simProbeNode)
			{
				usePortPoly = true;
				tot = 1;
			}
			for(int j=0; j<tot; j++)
			{
				PortProto rPp = null;
				Poly polyPtr = null;
				if (usePortPoly)
				{
					// search all ports for the closest
					PortProto bestPp = null;
					double bestDist = 0;
					for(Iterator<PortProto> pIt = ni.getProto().getPorts(); pIt.hasNext(); )
					{
						PortProto tPp = pIt.next();

						// compute best distance to the other node
						Poly portPoly = ni.getShapeOfPort(tPp);
						double x = portPoly.getCenterX();
						double y = portPoly.getCenterY();
						double dist = Math.abs(x-oX) + Math.abs(y-oY);
						if (bestPp == null) {
                            bestDist = dist;
                            bestPp = tPp;
                        }
						if (dist > bestDist) continue;
						bestPp = tPp;   bestDist = dist;
					}
					if (bestPp == null) continue;
					rPp = bestPp;
					polyPtr = ni.getShapeOfPort(rPp);
				} else
				{
					polyPtr = polys[j];

					// only want electrically connected polygons
					if (polyPtr.getPort() == null) continue;

					// search all ports for the closest connected to this layer
					PortProto bestPp = null;
					double bestDist = 0;
					for(Iterator<PortProto> pIt = ni.getProto().getPorts(); pIt.hasNext(); )
					{
						PortProto tPp = pIt.next();
						if (!netlist.portsConnected(ni, tPp, polyPtr.getPort())) continue;

						// compute best distance to the other node
						Poly portPoly = ni.getShapeOfPort(tPp);
						double x = portPoly.getCenterX();
						double y = portPoly.getCenterY();
						double dist = Math.abs(x-oX) + Math.abs(y-oY);
						if (bestPp == null) bestDist = dist;
						if (dist > bestDist) continue;
						bestPp = tPp;   bestDist = dist;
					}
					if (bestPp == null) continue;
					rPp = bestPp;

					// transformed the polygon
					polyPtr.transform(trans);
				}

				// if the polygon layer is pseudo, substitute real layer
				Layer layer = polyPtr.getLayer();
				if (layer != null) layer = layer.getNonPseudoLayer();

				// stop now if already an arc on this port to other node
				boolean found = false;
				for(Iterator<Connection> cIt = ni.getConnections(); cIt.hasNext(); )
				{
					Connection con = cIt.next();
					PortInst pi = con.getPortInst();
					if (!netlist.portsConnected(ni, rPp, pi.getPortProto())) continue;
					if (con.getArc().getHeadPortInst().getNodeInst() == oNi ||
						con.getArc().getTailPortInst().getNodeInst() == oNi) { found = true;   break; }
				}
				if (found) continue;

				// see if an arc is possible
				boolean connected = false;
				ArcProto [] connections = rPp.getBasePort().getConnections();
				for(int pass=0; pass<2; pass++)
				{
					for(int i=0; i<connections.length; i++)
					{
						ArcProto ap = connections[i];
						if (pass == 0)
						{
							if (ap != preferredArc) continue;
						} else
						{
							if (ap == preferredArc) continue;
						}

						// arc must be in the same technology
						if (ap.getTechnology() != ni.getProto().getTechnology()) break;

						// this polygon must be the smallest arc layer
						findSmallestLayer(ap, arcLayers);
						if (!usePortPoly)
						{
							Layer oLayer = arcLayers.get(ap);
							if (!ap.getTechnology().sameLayer(oLayer, layer)) continue;
						}

						// pass it on to the next test
						connected = testPoly(ni, rPp, ap, polyPtr, oNi, netlist, nodeBounds, nodePortBounds, arcLayers, stayInside, limitBound);
						if (connected) {
                            Integer c = arcCount.get(ap);
                            if (c == null) c = new Integer(0);
                            c = new Integer(c.intValue()+1);
                            arcCount.put(ap, c);
                            count++;
                            break;
                        }
					}
					if (connected) break;
				}
				if (connected) break;
			}
		}
		return count;
	}

	/**
	 * Method to compare two arcs and see if they should be connected.
	 * @param ai1 the first ArcInst to compare.
	 * @param ai2 the second ArcInst to compare.
     * @param stayInside is the area in which to route (null to route arbitrarily).
	 * @param nl the Netlist information for the Cell with the arcs.
	 * @param limitBound if not null, only consider errors that occur in this area.
	 * @return the number of connections made (0 or 1).
	 */
	private static int compareTwoArcs(ArcInst ai1, ArcInst ai2, PolyMerge stayInside, Netlist nl, Rectangle2D limitBound)
	{
		// if connected, stop now
		if (ai1.getProto() != ai2.getProto()) return 0;
		Network net1 = nl.getNetwork(ai1, 0);
		Network net2 = nl.getNetwork(ai2, 0);
		if (net1 == net2) return 0;

		// look at all polygons on the first arcinst
		boolean usePortPoly = false;
		Poly [] polys1 = ai1.getProto().getTechnology().getShapeOfArc(ai1);
		int tot1 = polys1.length;
		Poly [] polys2 = ai2.getProto().getTechnology().getShapeOfArc(ai1);
		int tot2 = polys2.length;
		for(int i1=0; i1<tot1; i1++)
		{
			Poly poly1 = polys1[i1];
			Layer layer1 = poly1.getLayer();
			Layer.Function fun = layer1.getFunction();
			if (!fun.isMetal() && !fun.isDiff() && !fun.isPoly()) continue;
			Rectangle2D bounds1 = poly1.getBounds2D();

			// compare them against all of the polygons in the second arcinst
			for(int i2=0; i2<tot2; i2++)
			{
				Poly poly2 = polys2[i2];
				if (layer1 != poly2.getLayer()) continue;

				// two polygons on the same layer...are they even near each other?
				Rectangle2D bounds2 = poly2.getBounds2D();
				if (!bounds1.intersects(bounds2)) continue;

				// do precise test for touching

				// connect their closest ends
				Rectangle2D intersection = new Rectangle2D.Double();
				Rectangle2D.intersect(bounds1, bounds2, intersection);
				double x = intersection.getCenterX();
				double y = intersection.getCenterY();

				// run the wire
				connectObjects(ai1, net1, ai2, net2, ai1.getParent(), new Point2D.Double(x,y), stayInside, limitBound);
				return 1;
			}
		}
		return 0;
	}

	/**
	 * Method to compare a node and an arc to see if they touch and should be connected.
	 * @param ni the NodeInst to compare.
	 * @param ai the ArcInst to compare.
     * @param stayInside is the area in which to route (null to route arbitrarily).
	 * @param nl the Netlist information for the Cell with the node and arc.
	 * @param limitBound if not null, only consider errors that occur in this area.
	 * @return the number of connections made (0 or 1).
	 */
	private static int compareNodeWithArc(NodeInst ni, ArcInst ai, PolyMerge stayInside, Netlist nl, Rectangle2D limitBound)
	{
		if (ni.isCellInstance()) return 0;
		Network arcNet = nl.getNetwork(ai, 0);

		// gather information about the node
		Poly [] nodePolys = shapeOfNode(ni);
		int nTot = nodePolys.length;
		AffineTransform trans = ni.rotateOut();

		// look at all polygons on the arcinst
		Poly [] arcPolys = ai.getProto().getTechnology().getShapeOfArc(ai);
		int aTot = arcPolys.length;
		for(int i=0; i<aTot; i++)
		{
			Poly arcPoly = arcPolys[i];
			Layer arcLayer = arcPoly.getLayer();
			Layer.Function fun = arcLayer.getFunction();
			if (!fun.isMetal() && !fun.isDiff() && !fun.isPoly()) continue;
			Rectangle2D arcBounds = arcPoly.getBounds2D();
			double aCX = arcBounds.getCenterX();
			double aCY = arcBounds.getCenterY();

			// compare them against all of the polygons in the node
			for(int j=0; j<nTot; j++)
			{
				Poly nodePoly = nodePolys[j];
				nodePoly.transform(trans);

				// they must be on the same layer and touch
				Layer nodeLayer = nodePoly.getLayer();
				if (nodeLayer != null) nodeLayer = nodeLayer.getNonPseudoLayer();
				if (nodeLayer != arcLayer) continue;
				double polyDist = arcPoly.separation(nodePoly);
				if (polyDist > 0) continue;

				// only want electrically connected polygons
				if (nodePoly.getPort() == null) continue;

				// search all ports for the closest connected to this layer
				PortProto bestPp = null;
				double bestDist = 0;
				for(Iterator<PortProto> pIt = ni.getProto().getPorts(); pIt.hasNext(); )
				{
					PortProto tPp = pIt.next();
					if (!nl.portsConnected(ni, tPp, nodePoly.getPort())) continue;

					// compute best distance to the other node
					Poly portPoly = ni.getShapeOfPort(tPp);
					double portCX = portPoly.getCenterX();
					double portCY = portPoly.getCenterY();
					double dist = Math.abs(portCX-aCX) + Math.abs(portCY-aCY);
					if (bestPp == null) bestDist = dist;
					if (dist > bestDist) continue;
					bestPp = tPp;   bestDist = dist;
				}
				if (bestPp == null) continue;

				// run the wire
				PortInst pi = ni.findPortInstFromProto(bestPp);
				Poly portPoly = ni.getShapeOfPort(bestPp);
				double portCX = portPoly.getCenterX();
				double portCY = portPoly.getCenterY();
				Network nodeNet = nl.getNetwork(pi);
				if (arcNet == nodeNet) continue;
				Point2D bend1 = new Point2D.Double(portCX, aCY);
				Point2D bend2 = new Point2D.Double(aCX, portCY);
				if (stayInside != null)
				{
					if (!stayInside.contains(arcLayer, bend1)) bend1 = bend2;
				} else
				{
					if (!arcPoly.contains(bend1)) bend1 = bend2;
				}
				connectObjects(ai, arcNet, pi, nodeNet, ai.getParent(), bend1 /*new Point2D.Double(aCX, aCY)*/, stayInside, limitBound);
				return 1;
			}
		}
		return 0;
	}

	/**
	 * @param eobj1
	 * @param net1
	 * @param eobj2
	 * @param net2
	 * @param cell
	 * @param ctr
     * @param stayInside is the area in which to route (null to route arbitrarily).
	 * @param limitBound if not null, only consider errors that occur in this area.
	 * @return
	 */
	private static boolean connectObjects(ElectricObject eobj1, Network net1, ElectricObject eobj2, Network net2,
		Cell cell, Point2D ctr, PolyMerge stayInside, Rectangle2D limitBound)
	{
		// make sure this pair of networks isn't already scheduled for connection
		if (intendedPairs.contains(net1, net2)) return false;
		intendedPairs.add(net1, net2);

		// run the wire
		NodeInst ni1 = null;
		if (eobj1 instanceof NodeInst) ni1 = (NodeInst)eobj1; else
			if (eobj1 instanceof PortInst) ni1 = ((PortInst)eobj1).getNodeInst();

		NodeInst ni2 = null;
		if (eobj2 instanceof NodeInst) ni2 = (NodeInst)eobj2; else
			if (eobj2 instanceof PortInst) ni2 = ((PortInst)eobj2).getNodeInst();

        Route route = router.planRoute(cell, eobj1, eobj2, ctr, stayInside, true);
        if (route.size() == 0)
        {
//        	System.out.println("Unable to route from "+eobj1+" to "+eobj2);
        	return false;
        }
        allRoutes.add(route);

        // if either ni or oNi is a pin primitive, see if it is a candidate for clean-up
        if (ni1 != null)
        {
	        if (ni1.getFunction() == PrimitiveNode.Function.PIN &&
	        	!ni1.hasExports() && !ni1.hasConnections())
//	        	ni1.getNumExports() == 0 && ni1.getNumConnections() == 0)
	        {
	            possibleInlinePins.add(ni1);
	        }
        }
        if (ni2 != null)
        {
	        if (ni2.getFunction() == PrimitiveNode.Function.PIN &&
	        	!ni2.hasExports() && !ni2.hasConnections())
//	        	ni2.getNumExports() == 0 && ni2.getNumConnections() == 0)
	        {
	            possibleInlinePins.add(ni2);
	        }
        }
        return true;
	}

	/**
	 * Method to find exported polygons in node "oNi" that abut with the polygon
	 * in "poly" on the same layer.  When they do, these should be connected to
	 * nodeinst "ni", port "pp" with an arc of type "ap".  Returns the number of
	 * connections made (0 if none).
	 * @param ni
	 * @param pp
	 * @param ap
	 * @param poly
	 * @param oNi
	 * @param netlist
	 * @param nodeBounds
	 * @param nodePortBounds
	 * @param arcLayers
     * @param stayInside is the area in which to route (null to route arbitrarily).
	 * @param limitBound if not null, only consider errors that occur in this area.
	 * @return
	 */
	private static boolean testPoly(NodeInst ni, PortProto pp, ArcProto ap, Poly poly, NodeInst oNi, Netlist netlist,
		HashMap<NodeInst, Rectangle2D[]> nodeBounds, HashMap<NodeInst, ObjectQTree> nodePortBounds,
		HashMap<ArcProto,Layer> arcLayers, PolyMerge stayInside, Rectangle2D limitBound)
	{
		// get network associated with the node/port
		PortInst pi = ni.findPortInstFromProto(pp);

		// TODO: this will generate an error message if the port is a bus
		Network net = netlist.getNetwork(pi);

		// now look at every layer in this node
		if (oNi.isCellInstance())
		{
			// complex cell: look at all exports
			Rectangle2D bounds = poly.getBounds2D();

			if (USEQTREE)
			{
				// find ports near this bound
				ObjectQTree oqt = nodePortBounds.get(oNi);
				Rectangle2D biggerBounds = new Rectangle2D.Double(bounds.getMinX()-1, bounds.getMinY()-1, bounds.getWidth()+2, bounds.getHeight()+2);
				Set set = oqt.find(biggerBounds);
				if (set != null)
				{
					for (Object obj : set)
					{
						PortInst oPi = (PortInst)obj;
						PortProto mPp = oPi.getPortProto();

						// port must be able to connect to the arc
						if (!mPp.getBasePort().connectsTo(ap)) continue;

						// do not stitch where there is already an electrical connection
						Network oNet = netlist.getNetwork(oNi.findPortInstFromProto(mPp));
						if (net != null && oNet == net) continue;

						// do not stitch if there is already an arc connecting these two ports
		                boolean ignore = false;
		                for (Iterator<Connection> piit = oPi.getConnections(); piit.hasNext(); ) {
		                    Connection conn = piit.next();
		                    ArcInst ai = conn.getArc();
		                    if (ai.getHeadPortInst() == pi) ignore = true;
		                    if (ai.getTailPortInst() == pi) ignore = true;
		                }
		                if (ignore) continue;

						// find the primitive node at the bottom of this port
						AffineTransform trans = oNi.rotateOut();
						NodeInst rNi = oNi;
						PortProto rPp = mPp;
						while (rNi.isCellInstance())
						{
							AffineTransform temp = rNi.translateOut();
							temp.preConcatenate(trans);
							Export e = (Export)rPp;
							rNi = e.getOriginalPort().getNodeInst();
							rPp = e.getOriginalPort().getPortProto();

							trans = rNi.rotateOut();
							trans.preConcatenate(temp);
						}

						// see how much geometry is on this node
						Poly [] polys = shapeOfNode(rNi);
						int tot = polys.length;
						if (tot == 0)
						{
							// not a geometric primitive: look for ports that touch
							Poly oPoly = oNi.getShapeOfPort(mPp);
							if (comparePoly(oNi, mPp, oPoly, oNet, ni, pp, poly, net, ap, stayInside, netlist, limitBound))
								return true;
						} else
						{
							// a geometric primitive: look for ports on layers that touch
							Netlist subNetlist = rNi.getParent().getUserNetlist();
							for(int j=0; j<tot; j++)
							{
								Poly oPoly = polys[j];

								// only want electrically connected polygons
								if (oPoly.getPort() == null) continue;

								// only want polygons connected to correct part of nodeinst
								if (!subNetlist.portsConnected(rNi, rPp, oPoly.getPort())) continue;

								// if the polygon layer is pseudo, substitute real layer
								if (ni.getProto() != Generic.tech.simProbeNode)
								{
									Layer oLayer = oPoly.getLayer();
									if (oLayer != null) oLayer = oLayer.getNonPseudoLayer();
									Layer apLayer = arcLayers.get(ap);
									if (!oLayer.getTechnology().sameLayer(oLayer, apLayer)) continue;
								}

								// transform the polygon and pass it on to the next test
								oPoly.transform(trans);
								if (comparePoly(oNi, mPp, oPoly, oNet, ni, pp, poly, net, ap, stayInside, netlist, limitBound))
									return true;
							}
						}
					}
				}
			} else
			{
				// NOT USING QTREE
				Rectangle2D [] boundArray = nodeBounds.get(oNi);
				int bbp = 0;
				for(Iterator<PortProto> it = oNi.getProto().getPorts(); it.hasNext(); )
				{
					PortProto mPp = it.next();
	
					// first do a bounding box check
					if (boundArray != null)
					{
						Rectangle2D oBounds = boundArray[bbp++];
						if (oBounds.getMinX() > bounds.getMaxX() || oBounds.getMaxX() < bounds.getMinX() ||
							oBounds.getMinY() > bounds.getMaxY() || oBounds.getMaxY() < bounds.getMinY()) continue;
					}
	
					// port must be able to connect to the arc
					if (!mPp.getBasePort().connectsTo(ap)) continue;
	
					// do not stitch where there is already an electrical connection
					Network oNet = netlist.getNetwork(oNi.findPortInstFromProto(mPp));
					if (net != null && oNet == net) continue;
	
	                // do not stitch if there is already an arc connecting these two ports
	                PortInst oPi = oNi.findPortInstFromProto(mPp);
	                boolean ignore = false;
	                for (Iterator<Connection> piit = oPi.getConnections(); piit.hasNext(); ) {
	                    Connection conn = piit.next();
	                    ArcInst ai = conn.getArc();
	                    if (ai.getHeadPortInst() == pi) ignore = true;
	                    if (ai.getTailPortInst() == pi) ignore = true;
	                }
	                if (ignore) continue;
	
					// find the primitive node at the bottom of this port
					AffineTransform trans = oNi.rotateOut();
					NodeInst rNi = oNi;
					PortProto rPp = mPp;
					while (rNi.isCellInstance())
					{
						AffineTransform temp = rNi.translateOut();
						temp.preConcatenate(trans);
						Export e = (Export)rPp;
						rNi = e.getOriginalPort().getNodeInst();
						rPp = e.getOriginalPort().getPortProto();
	
						trans = rNi.rotateOut();
						trans.preConcatenate(temp);
					}
	
					// see how much geometry is on this node
					Poly [] polys = shapeOfNode(rNi);
					int tot = polys.length;
					if (tot == 0)
					{
						// not a geometric primitive: look for ports that touch
						Poly oPoly = oNi.getShapeOfPort(mPp);
						if (comparePoly(oNi, mPp, oPoly, oNet, ni, pp, poly, net, ap, stayInside, netlist, limitBound))
							return true;
					} else
					{
						// a geometric primitive: look for ports on layers that touch
						Netlist subNetlist = rNi.getParent().getUserNetlist();
						for(int j=0; j<tot; j++)
						{
							Poly oPoly = polys[j];
	
							// only want electrically connected polygons
							if (oPoly.getPort() == null) continue;
	
							// only want polygons connected to correct part of nodeinst
							if (!subNetlist.portsConnected(rNi, rPp, oPoly.getPort())) continue;
	
							// if the polygon layer is pseudo, substitute real layer
							if (ni.getProto() != Generic.tech.simProbeNode)
							{
								Layer oLayer = oPoly.getLayer();
								if (oLayer != null) oLayer = oLayer.getNonPseudoLayer();
								Layer apLayer = arcLayers.get(ap);
								if (!oLayer.getTechnology().sameLayer(oLayer, apLayer)) continue;
							}
	
							// transform the polygon and pass it on to the next test
							oPoly.transform(trans);
							if (comparePoly(oNi, mPp, oPoly, oNet, ni, pp, poly, net, ap, stayInside, netlist, limitBound))
								return true;
						}
					}
				}
			}
		} else
		{
			// primitive node: check its layers
			AffineTransform trans = oNi.rotateOut();

			// determine target point
			double ox = poly.getCenterX();
			double oy = poly.getCenterY();

			// look at all polygons on nodeinst oNi
			Poly [] polys = shapeOfNode(oNi);
			int tot = polys.length;
			if (tot == 0)
			{
				// not a geometric primitive: look for ports that touch
				PortProto bestPp = null;
				double bestDist = 0;
				for(Iterator<PortProto> pIt = oNi.getProto().getPorts(); pIt.hasNext(); )
				{
					PortProto rPp = pIt.next();

					// compute best distance to the other node
					Poly portPoly = oNi.getShapeOfPort(rPp);
					double dist = Math.abs(portPoly.getCenterX()-ox) + Math.abs(portPoly.getCenterY()-oy);
					if (bestPp == null) {
                        bestDist = dist;
                        bestPp = rPp;
                    }
					if (dist > bestDist) continue;
					bestPp = rPp;   bestDist = dist;
				}
				if (bestPp != null)
				{
					PortProto rPp = bestPp;
					Network oNet = netlist.getNetwork(oNi.findPortInstFromProto(bestPp));
					if (net == null || oNet != net)
					{
						// port must be able to connect to the arc
						if (rPp.getBasePort().connectsTo(ap))
						{
							// transformed the polygon and pass it on to the next test
							Poly oPoly = oNi.getShapeOfPort(rPp);
							if (comparePoly(oNi, rPp, oPoly, oNet, ni, pp, poly, net, ap, stayInside, netlist, limitBound))
								return true;
						}
					}
				}
			} else
			{
				// a geometric primitive: look for ports on layers that touch
				for(int j=0; j<tot; j++)
				{
					Poly oPoly = polys[j];

					// only want electrically connected polygons
					if (oPoly.getPort() == null) continue;

					// if the polygon layer is pseudo, substitute real layer
					Layer oLayer = oPoly.getLayer();
					if (oLayer != null) oLayer = oLayer.getNonPseudoLayer();

					// this must be the smallest layer on the arc
					Layer apLayer = arcLayers.get(ap);
					if (!apLayer.getTechnology().sameLayer(apLayer, oLayer)) continue;

					// do not stitch where there is already an electrical connection
					PortInst oPi = oNi.findPortInstFromProto(oPoly.getPort());
					Network oNet = netlist.getNetwork(oPi);
					if (net != null && oNet == net) continue;

					// search all ports for the closest connected to this layer
					PortProto bestPp = null;
					double bestDist = 0;
					for(Iterator<PortProto> pIt = oNi.getProto().getPorts(); pIt.hasNext(); )
					{
						PortProto rPp = pIt.next();
						if (!netlist.portsConnected(oNi, rPp, oPoly.getPort())) continue;

						// compute best distance to the other node
						Poly portPoly = oNi.getShapeOfPort(rPp);
						double dist = Math.abs(ox-portPoly.getCenterX()) + Math.abs(oy-portPoly.getCenterY());
						if (bestPp == null) bestDist = dist;
						if (dist > bestDist) continue;
						bestPp = rPp;   bestDist = dist;
					}
					if (bestPp == null) continue;
					PortProto rPp = bestPp;

					// port must be able to connect to the arc
					if (!rPp.getBasePort().connectsTo(ap)) continue;

					// transformed the polygon and pass it on to the next test
					oPoly.transform(trans);
					if (comparePoly(oNi, rPp, oPoly, oNet, ni, pp, poly, net, ap, stayInside, netlist, limitBound))
						return true;
				}
			}
		}
		return false;
	}

	/**
	 * Method to compare two polygons.  If these polygons touch
	 * or overlap then the two nodes should be connected.
	 * @param oNi the NodeInst responsible for the first polygon.
	 * @param opp the PortProto responsible for the first polygon.
	 * @param oPoly the first polygon.
	 * @param oNet the Network responsible for the first polygon.
	 * @param ni the NodeInst responsible for the second polygon.
	 * @param pp the PortProto responsible for the second polygon.
	 * @param poly the second polygon.
	 * @param net the Network responsible for the second polygon.
	 * @param ap the type of arc to use when stitching the nodes.
     * @param stayInside is the area in which to route (null to route arbitrarily).
	 * @param netlist
	 * @param limitBound if not null, only consider errors that occur in this area.
	 * @return true if the connection is made.
	 */
	private static boolean comparePoly(NodeInst oNi, PortProto opp, Poly oPoly, Network oNet,
		NodeInst ni, PortProto pp, Poly poly, Network net,
		ArcProto ap, PolyMerge stayInside, Netlist netlist, Rectangle2D limitBound)
	{
		// find the bounding boxes of the polygons
		if (poly.separation(oPoly) > 0) return false;

		// be sure the closest ports are being used
		Poly portPoly = ni.getShapeOfPort(pp);
		Point2D portCenter = new Point2D.Double(portPoly.getCenterX(), portPoly.getCenterY());
		Poly oPortPoly = oNi.getShapeOfPort(opp);
		Point2D oPortCenter = new Point2D.Double(oPortPoly.getCenterX(), oPortPoly.getCenterY());

		if (USEQTREE)
		{
			Rectangle2D polyBounds = portPoly.getBounds2D();
			Rectangle2D oPolyBounds = oPortPoly.getBounds2D();
	
			// quit now if bounding boxes don't intersect
			if ((polyBounds.getMinX() > oPolyBounds.getMaxX() || oPolyBounds.getMinX() > polyBounds.getMaxX()) &&
				(polyBounds.getMinY() > oPolyBounds.getMaxY() || oPolyBounds.getMinY() > polyBounds.getMaxY())) return false;
		}

		double dist = portCenter.distance(oPortCenter);
		for(Iterator<PortProto> it = oNi.getProto().getPorts(); it.hasNext(); )
		{
			PortProto tPp = it.next();
			if (tPp == opp) continue;
			if (!netlist.portsConnected(oNi, tPp, opp)) continue;
			portPoly = oNi.getShapeOfPort(tPp);
			Point2D tPortCenter = new Point2D.Double(portPoly.getCenterX(), portPoly.getCenterY());
			double tDist = portCenter.distance(tPortCenter);
			if (tDist >= dist) continue;
			dist = tDist;
			opp = tPp;
			oPortCenter.setLocation(tPortCenter);
		}
		for(Iterator<PortProto> it = ni.getProto().getPorts(); it.hasNext(); )
		{
			PortProto tPp = it.next();
			if (tPp == pp) continue;
			if (!netlist.portsConnected(ni, tPp, pp)) continue;
			portPoly = ni.getShapeOfPort(tPp);
			Point2D tPortCenter = new Point2D.Double(portPoly.getCenterX(), portPoly.getCenterY());
			double tDist = oPortCenter.distance(tPortCenter);
			if (tDist >= dist) continue;
			dist = tDist;
			pp = tPp;
			portCenter.setLocation(tPortCenter);
		}

		// reject connection if it is out of the limit bounds
		if (limitBound != null)
		{
			if (!GenMath.pointInRect(portCenter, limitBound) && !GenMath.pointInRect(oPortCenter, limitBound))
				return false;
		}

		// find some dummy position to help run the arc
		double x = (oPortCenter.getX() + portCenter.getX()) / 2;
		double y = (oPortCenter.getY() + portCenter.getY()) / 2;

		// run the wire
        PortInst pi = ni.findPortInstFromProto(pp);
        PortInst opi = oNi.findPortInstFromProto(opp);
        return connectObjects(pi, net, opi, oNet, ni.getParent(), new Point2D.Double(x,y), stayInside, limitBound);
	}

	/**
	 * Method to get the shape of a node as a list of Polys.
	 * The autorouter uses this instead of Technology.getShapeOfNode()
	 * because this insists on getting electrical layers, and this
	 * makes invisible pins be visible if they have coverage from connecting arcs.
	 * @param ni the node to inspect.  It must be primitive.
	 * @return an array of Poly objects that describe the node.
	 */
	private static Poly [] shapeOfNode(NodeInst ni)
	{
		// compute the list of polygons
		Technology tech = ni.getProto().getTechnology();
		Poly [] nodePolys = tech.getShapeOfNode(ni, null, null, true, true, null);
		if (nodePolys.length == 0) return nodePolys;

		// if this is a pin, check the arcs that cover it
		if (ni.getFunction() == PrimitiveNode.Function.PIN)
		{
			// pins must be covered by an arc that is extended and has enough width to cover the pin
			boolean gotOne = false;
			Rectangle2D coverage = null;
			Rectangle2D polyBounds = nodePolys[0].getBounds2D();
			for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
			{
				Connection con = it.next();
				ArcInst ai = con.getArc();
				if (ai.getWidth() >= ni.getXSize() && ai.getWidth() >= ni.getYSize() && ai.isHeadExtended() && ai.isTailExtended())
				{
					gotOne = true;
					break;
				}

				// figure out how much of the pin is covered by the arc
				Poly [] arcPolys = ai.getProto().getTechnology().getShapeOfArc(ai);
				if (arcPolys.length == 0) continue;
				Rectangle2D arcBounds = arcPolys[0].getBounds2D();
				arcBounds.intersects(polyBounds);
				if (coverage == null) coverage = arcBounds; else
				{
					// look for known and easy configurations
					if (coverage.getMinX() == arcBounds.getMinX() && coverage.getMaxX() == arcBounds.getMaxX() &&
						coverage.getMinY() >= arcBounds.getMaxY() && coverage.getMaxY() <= arcBounds.getMinY())
					{
						// they are stacked vertically
						double lX = Math.min(coverage.getMinX(), arcBounds.getMinX());
						double hX = Math.max(coverage.getMaxX(), arcBounds.getMaxX());
						coverage.setRect(lX, coverage.getMinY(), hX-lX, coverage.getHeight());
					} else if (coverage.getMinY() == arcBounds.getMinY() && coverage.getMaxY() == arcBounds.getMaxY())
					{
						// they are side-by-side
						if (coverage.getMinX() >= arcBounds.getMaxX() && coverage.getMaxX() <= arcBounds.getMinX())
						{
							double lY = Math.min(coverage.getMinY(), arcBounds.getMinY());
							double hY = Math.max(coverage.getMaxY(), arcBounds.getMaxY());
							coverage.setRect(coverage.getMinX(), lY, coverage.getWidth(), hY-lY);
						}
					} else
					{
						// not known, intersection is a bit restrictive...
						coverage.intersects(arcBounds);
					}
				}
			}
			if (!gotOne && ni.getNumExports() == 0)
			{
				if (coverage == null) return new Poly[0];

				Poly newPoly = new Poly(coverage);
				newPoly.setStyle(nodePolys[0].getStyle());
				newPoly.setLayer(nodePolys[0].getLayer());
				newPoly.setPort(nodePolys[0].getPort());
				nodePolys[0] = newPoly;				
			}
		}
		return nodePolys;
	}

	/**
	 * Method to find the smallest layer on arc proto "ap" and cache that information
	 * in the "temp1" field of the arc proto.
	 */
	public static void findSmallestLayer(ArcProto ap, HashMap<ArcProto,Layer> arcLayers)
	{
		// quit if the value has already been computed
		if (arcLayers.get(ap) != null) return;

		// get a dummy arc to analyze
		ArcInst ai = ArcInst.makeDummyInstance(ap, 100);

		// find the smallest layer
		boolean bestFound = false;
		double bestArea = 0;
		Technology tech = ap.getTechnology();
		Poly [] polys = tech.getShapeOfArc(ai);
		int tot = polys.length;
		for(int i=0; i<tot; i++)
		{
			Poly poly = polys[i];
			//double area = Math.abs(poly.getArea());
            //PolyBase.getArea is always positive
            double area = poly.getArea();

			if (bestFound && area >= bestArea) continue;
			bestArea = area;
			bestFound = true;
			arcLayers.put(ap, poly.getLayer());
		}
	}

	/**
	 * Class to implement pairs of objects (in any order).
	 */
	private static class Pairs
	{
		private HashMap<Object,HashSet<Object>> first;

		Pairs()
		{
			first = new HashMap<Object,HashSet<Object>>();
		}

		void add(Object o1, Object o2)
		{
			if (contains(o1, o2)) return;
			HashSet<Object> other1 = first.get(o1);
			if (other1 != null)
			{
				other1.add(o2);
				return;
			}
			HashSet<Object> other2 = first.get(o2);
			if (other2 != null)
			{
				other2.add(o1);
				return;
			}
			other1 = new HashSet<Object>();
			first.put(o1, other1);
			other1.add(o2);			
		}

		boolean contains(Object o1, Object o2)
		{
			HashSet other1 = first.get(o1);
			if (other1 != null && other1.contains(o2)) return true;
			HashSet other2 = first.get(o2);
			if (other2 != null && other2.contains(o1)) return true;
			return false;
		}
	}
}
