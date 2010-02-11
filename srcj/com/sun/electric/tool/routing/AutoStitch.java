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
package com.sun.electric.tool.routing;

import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Dimension2D;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.ObjectQTree;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.geometry.PolyMerge;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortOriginal;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.RTBounds;
import com.sun.electric.database.topology.RTNode;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.CircuitChangeJobs;
import com.sun.electric.tool.user.User;

import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Class which implements the Auto Stitching tool.
 */
public class AutoStitch
{
	/** true to ignore true pin size */								private static final boolean ZEROSIZEPINS = true;

	/** router used to wire */  									private InteractiveRouter router;

	/** list of all routes to be created at end of analysis */		private List<Route> allRoutes;
	/** list of pins that may be inline pins due to created arcs */	private List<NodeInst> possibleInlinePins;
	/** cache of true pin sizes */									private Map<PortInst,Double> truePinSize;
	/** set of nodes to check (prevents duplicate checks) */		private Set<NodeInst> nodeMark;
	/** edge alignment for arcs */									private Dimension2D alignment;
	/** true to stitch pure-layer nodes */							private boolean includePureLayerNodes;

    /****************************************** CONTROL ******************************************/

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
				ElectricObject eObj = geom;
				if (eObj instanceof PortInst) eObj = ((PortInst)eObj).getNodeInst();
				if (eObj instanceof NodeInst)
				{
					NodeInst ni = (NodeInst)eObj;
					if (!ni.isCellInstance())
					{
						PrimitiveNode pnp = (PrimitiveNode)ni.getProto();
						if (pnp.getTechnology() == Generic.tech()) continue;
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
		private AutoOptions prefs;

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
			prefs = new AutoOptions();
			prefs.initFromUserDefaults();
			startJob();
		}

		public boolean doIt() throws JobException
		{
			Rectangle2D limitBound = null;
			if (lX != hX && lY != hY)
				limitBound = new Rectangle2D.Double(lX, lY, hX-lX, hY-lY);
			runAutoStitch(cell, nodesToStitch, arcsToStitch, this, null, limitBound, forced, false, prefs, false, null);
			return true;
		}
	}

	/**
	 * This is the public interface for Auto-stitching when done in batch mode.
	 * @param cell the cell in which to stitch.
	 * @param nodesToStitch a list of NodeInsts to stitch (null to use all in the cell).
	 * @param arcsToStitch a list of ArcInsts to stitch (null to use all in the cell).
	 * @param job the Job running this, for aborting.
	 * @param stayInside is the area in which to route (null to route arbitrarily).
	 * @param limitBound if not null, only consider connections that occur in this area.
	 * @param forced true if the stitching was explicitly requested (and so results should be printed).
	 * @param includePureLayerNodes true to route pure-layer nodes (normally ignorned).
	 * @param prefs routing preferences.
	 * @param showProgress true to show progress.
	 * @param alignment grid alignment for edges of arcs (null if none).
	 */
	public static void runAutoStitch(Cell cell, List<NodeInst> nodesToStitch, List<ArcInst> arcsToStitch, Job job,
		PolyMerge stayInside, Rectangle2D limitBound, boolean forced, boolean includePureLayerNodes,
		AutoOptions prefs, boolean showProgress, Dimension2D alignment)
	{
		// initialization
		if (cell.isAllLocked())
		{
			System.out.println("WARNING: Cell " + cell.describe(false) + " is locked: no changes can be made");
			return;
		}

		AutoStitch as = new AutoStitch(prefs.fatWires);
		as.alignment = alignment;
		as.includePureLayerNodes = includePureLayerNodes;
		as.runNow(cell, nodesToStitch, arcsToStitch, job, stayInside, limitBound, forced, prefs, showProgress);
	}

	private AutoStitch(boolean fatWires)
	{
		possibleInlinePins = new ArrayList<NodeInst>();
		truePinSize = new HashMap<PortInst,Double>();
        router = new SimpleWirer(fatWires);
	}

	private List<ArcInst> getArcsToStitch(Cell cell, List<ArcInst> arcsToStitch)
	{
		List<ArcInst> newArcsToStitch = new ArrayList<ArcInst>();
		if (arcsToStitch == null)
		{
			for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
				newArcsToStitch.add(it.next());
		} else
		{
			for(ArcInst ai : arcsToStitch)
				if (ai.isLinked()) newArcsToStitch.add(ai);
		}
		return newArcsToStitch;
	}

	private List<NodeInst> getNodesToStitch(Cell cell, List<NodeInst> nodesToStitch)
	{
		List<NodeInst> newNodesToStitch = new ArrayList<NodeInst>();
		if (nodesToStitch == null)
		{
			// no data from highlighter
			for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				if (ni.isIconOfParent()) continue;
				if (!ni.isCellInstance())
				{
					PrimitiveNode pnp = (PrimitiveNode)ni.getProto();
					if (pnp.getTechnology() == Generic.tech()) continue;
					if (!includePureLayerNodes && pnp.getFunction() == PrimitiveNode.Function.NODE) continue;
				}
				newNodesToStitch.add(ni);
			}
		} else
		{
			for(NodeInst ni : nodesToStitch)
				if (ni.isLinked()) newNodesToStitch.add(ni);
		}
		return newNodesToStitch;
	}

	/**
	 * Method to run auto-stitching.
	 * @param cell the cell in which to stitch.
	 * @param nodesToStitch a list of NodeInsts to stitch (null to use all in the cell).
	 * @param arcsToStitch a list of ArcInsts to stitch (null to use all in the cell).
	 * @param job the Job running this, for aborting.
	 * @param stayInside is the area in which to route (null to route arbitrarily).
	 * @param limitBound if not null, only consider connections that occur in this area.
	 * @param forced true if the stitching was explicitly requested (and so results should be printed).
	 * @param prefs routing preferences.
	 * @param showProgress true to show progress.
	 */
	private void runNow(Cell cell, List<NodeInst> origNodesToStitch, List<ArcInst> origArcsToStitch, Job job,
		PolyMerge stayInside, Rectangle2D limitBound, boolean forced, AutoOptions prefs, boolean showProgress)
	{
		if (showProgress) Job.getUserInterface().setProgressNote("Initializing routing");
		ArcProto preferredArc = prefs.preferredArc;

		// gather objects to stitch
		List<NodeInst> nodesToStitch = getNodesToStitch(cell, origNodesToStitch);
		List<ArcInst> arcsToStitch = getArcsToStitch(cell, origArcsToStitch);

		// next mark nodes to be checked
		nodeMark = new HashSet<NodeInst>();
		for(NodeInst ni : nodesToStitch)
			nodeMark.add(ni);
		Map<ArcProto,Integer> arcsCreatedMap = new HashMap<ArcProto,Integer>();
		Map<NodeProto,Integer> nodesCreatedMap = new HashMap<NodeProto,Integer>();
		allRoutes = new ArrayList<Route>();

		// compute the number of tasks to perform and start progress bar
		int totalToStitch = nodesToStitch.size() + arcsToStitch.size();
		if (prefs.createExports) totalToStitch *= 2;
		totalToStitch += arcsToStitch.size();
		int soFar = 0;

		if (job != null && job.checkAbort()) return;

		// if creating exports, make first pass in which exports must be created
		if (prefs.createExports)
		{
			if (showProgress) Job.getUserInterface().setProgressNote("Routing " + totalToStitch + " objects with export creation...");

            // make global network map
            GatherNetworksVisitor gatherNetworks = new GatherNetworksVisitor();
            HierarchyEnumerator.enumerateCell(cell, VarContext.globalContext, gatherNetworks);

            Map<Long,List<PolyConnection>> overlapMap = new HashMap<Long,List<PolyConnection>>();
            for(NodeInst ni : nodesToStitch)
			{
				soFar++;
				if (showProgress && (soFar%100) == 0)
				{
					if (job != null && job.checkAbort()) return;
					Job.getUserInterface().setProgressValue(soFar * 100 / totalToStitch);
				}
				checkExportCreationStitching(ni, overlapMap, gatherNetworks);
			}

			// now run through the arcinsts to be checked for export-creation stitching
			for(ArcInst ai : arcsToStitch)
			{
				soFar++;
				if (showProgress && (soFar%100) == 0)
				{
					if (job != null && job.checkAbort()) return;
					Job.getUserInterface().setProgressValue(soFar * 100 / totalToStitch);
				}

				// only interested in arcs that are wider than their nodes (and have geometry that sticks out)
				if (!arcTooWide(ai)) continue;
				if (!ai.isLinked()) continue;
				checkExportCreationStitching(ai, overlapMap, gatherNetworks);
			}

            if (showProgress) Job.getUserInterface().setProgressNote("Gathering " + totalToStitch + " objects for export creation...");

            // check for existing exports, or make export if needed
            for (Long netID : overlapMap.keySet())
            {
                List<PolyConnection> polyConns = overlapMap.get(netID);
                makeExport(polyConns);
            }

            // now run these arcs and reinitialize the list
			makeConnections(showProgress, arcsCreatedMap, nodesCreatedMap, stayInside);
			allRoutes = new ArrayList<Route>();
			if (showProgress) Job.getUserInterface().setProgressNote("Initializing routing");

			// reinitialize list of objects to examine
			nodesToStitch = getNodesToStitch(cell, origNodesToStitch);
			arcsToStitch = getArcsToStitch(cell, origArcsToStitch);
		}

		// next pre-compute bounds on all nodes in cell
		Map<NodeInst, ObjectQTree> nodePortBounds = new HashMap<NodeInst, ObjectQTree>();
		for(Iterator<NodeInst> nIt = cell.getNodes(); nIt.hasNext(); )
		{
			NodeInst ni = nIt.next();
			Rectangle2D niBounds = ni.getBounds();
			ObjectQTree oqt = new ObjectQTree(niBounds);
			for(Iterator<PortInst> it = ni.getPortInsts(); it.hasNext(); )
			{
				PortInst pi = it.next();
				PortProto pp = pi.getPortProto();
				PortOriginal fp = new PortOriginal(ni, pp);
				AffineTransform trans = fp.getTransformToTop();
				NodeInst rNi = fp.getBottomNodeInst();

				double xSize = rNi.getXSize(), ySize = rNi.getYSize();
				if (ZEROSIZEPINS && rNi.getFunction() == PrimitiveNode.Function.PIN)
				{
					double pinSize = getPortSize(pi);
					xSize = ySize = pinSize;
				}
				Rectangle2D bounds = new Rectangle2D.Double(rNi.getAnchorCenterX() - xSize/2,
					rNi.getAnchorCenterY() - ySize/2, xSize, ySize);
				DBMath.transformRect(bounds, trans);
				if (!oqt.add(pi, bounds))
					System.out.println("ERROR: Failed to construct quad-tree with port " + pp.getName() + " of node " + ni.describe(false));
			}
			nodePortBounds.put(ni, oqt);
		}

		// finally, initialize the information about which layer is smallest on each arc
		Map<ArcProto,Layer> arcLayers = new HashMap<ArcProto,Layer>();

		// get the topology object for knowing what is connected
		StitchingTopology top = new StitchingTopology(cell);

		if (showProgress) Job.getUserInterface().setProgressNote("Routing " + totalToStitch + " objects...");

		// first check for arcs that daisy-chain many nodes
		for(ArcInst ai : arcsToStitch)
		{
			soFar++;
			if (showProgress && (soFar%100) == 0)
			{
				if (job != null && job.checkAbort()) return;
				Job.getUserInterface().setProgressValue(soFar * 100 / totalToStitch);
			}
			checkDaisyChain(ai, nodePortBounds, stayInside, top);
		}

		if (allRoutes.size() > 0)
		{
			// found daisy-chain elements: do them now
			System.out.println("Auto-routing detected " + allRoutes.size() + " daisy-chained arcs");
			for(Route route : allRoutes)
			{
				boolean failure = Router.createRouteNoJob(route, cell, arcsCreatedMap, nodesCreatedMap);
				if (failure)
				{
					System.out.println("AUTO STITCHER FAILED TO MAKE DAISY-CHAIN ARC");
				}
			}

			// reset for the rest of the analysis
			allRoutes = new ArrayList<Route>();
			top = new StitchingTopology(cell);

			// reinitialize list of objects to examine
			nodesToStitch = getNodesToStitch(cell, origNodesToStitch);
			arcsToStitch = getArcsToStitch(cell, origArcsToStitch);
		}

		// now run through the nodeinsts to be checked for stitching
		for(NodeInst ni : nodesToStitch)
		{
			soFar++;
			if (showProgress && (soFar%100) == 0)
			{
				if (job != null && job.checkAbort()) return;
				Job.getUserInterface().setProgressValue(soFar * 100 / totalToStitch);
			}
			checkStitching(ni, nodePortBounds, arcLayers, stayInside, top, limitBound, preferredArc);
		}

		// now run through the arcinsts to be checked for stitching
		for(ArcInst ai : arcsToStitch)
		{
			soFar++;
			if (showProgress && (soFar%100) == 0)
			{
				if (job != null && job.checkAbort()) return;
				Job.getUserInterface().setProgressValue(soFar * 100 / totalToStitch);
			}

			if (!ai.isLinked()) continue;

			// only interested in arcs that are wider than their nodes (and have geometry that sticks out)
			if (!arcTooWide(ai)) continue;
			checkStitching(ai, nodePortBounds, arcLayers, stayInside, top, limitBound, preferredArc);
		}

		// create the routes
		makeConnections(showProgress, arcsCreatedMap, nodesCreatedMap, stayInside);

		// report results
        boolean beep = User.isPlayClickSoundsWhenCreatingArcs();
		if (forced) Router.reportRoutingResults("AUTO ROUTING", arcsCreatedMap, nodesCreatedMap, beep);

		// check for any inline pins due to created wires
		if (showProgress)
		{
			if (job != null && job.checkAbort()) return;
			Job.getUserInterface().setProgressValue(0);
			Job.getUserInterface().setProgressNote("Cleaning up pins...");
		}
		List<CircuitChangeJobs.Reconnect> pinsToPassThrough = new ArrayList<CircuitChangeJobs.Reconnect>();
		for (NodeInst ni : possibleInlinePins)
		{
			if (ni.isInlinePin())
			{
				CircuitChangeJobs.Reconnect re = CircuitChangeJobs.Reconnect.erasePassThru(ni, false, true);
				if (re != null)
				{
					pinsToPassThrough.add(re);
				}
			}
		}
		if (pinsToPassThrough.size() > 0)
		{
			CircuitChangeJobs.CleanupChanges ccJob = new CircuitChangeJobs.CleanupChanges(cell, true, Collections.<NodeInst>emptySet(),
				pinsToPassThrough, new HashMap<NodeInst,EPoint>(), new ArrayList<NodeInst>(), new HashSet<ArcInst>(), 0, 0, 0);
			try
			{
				ccJob.doIt();
			} catch (JobException e)
			{
			}
		}
	}

	private double getPortSize(PortInst pi)
	{
		Double size = truePinSize.get(pi);
		if (size != null) return size.doubleValue();

		double widestArc = 0;
		PortInst bottomPort = pi;
		NodeInst bottomNi;
		for(;;)
		{
			bottomNi = bottomPort.getNodeInst();
			PortProto bottomPp = bottomPort.getPortProto();

			// analyze all arcs connected to bottomPort
			for(Iterator<Connection> it = bottomPort.getConnections(); it.hasNext(); )
			{
				Connection con = it.next();
				ArcInst ai = con.getArc();
				int end = con.getEndIndex();
				if (!ai.isExtended(end)) continue;
				widestArc = Math.max(ai.getLambdaBaseWidth(), widestArc);
			}

			// if at the bottom, stop
        	if (!bottomNi.isCellInstance()) break;
            bottomPort = ((Export)bottomPp).getOriginalPort();
        }
		if (widestArc > bottomNi.getXSize() && widestArc > bottomNi.getYSize())
			widestArc = Math.max(bottomNi.getXSize(), bottomNi.getYSize());
		truePinSize.put(pi, new Double(widestArc));
		return widestArc;
	}

	private void makeConnections(boolean showProgress, Map<ArcProto,Integer> arcsCreatedMap,
		Map<NodeProto,Integer> nodesCreatedMap, PolyMerge stayInside)
	{
		// create the routes
		int totalToStitch = allRoutes.size();
		int soFar = 0;
		if (showProgress)
		{
			Job.getUserInterface().setProgressValue(0);
			Job.getUserInterface().setProgressNote("Creating " + totalToStitch + " wires...");
		}
		Collections.sort(allRoutes, new CompRoutes());
		for (Route route : allRoutes)
		{
			soFar++;
			if (showProgress && (soFar%100) == 0)
				Job.getUserInterface().setProgressValue(soFar * 100 / totalToStitch);

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

//			// if forced to fit in area, don't auto-rotate arcs
//			if (stayInside != null)
//			{
//		        for (RouteElement e : route)
//		        {
//		            if (e.getAction() == RouteElement.RouteElementAction.newArc)
//		            {
//		                RouteElementArc rea = (RouteElementArc)e;
//		                rea.setArcAngle(0);
//		            }
//		        }
//			}
			Router.createRouteNoJob(route, c, arcsCreatedMap, nodesCreatedMap);
		}
	}

	/****************************************** ARCS THAT DAISY-CHAIN ******************************************/

	private static class DaisyChainPoint
	{
		PortInst pi;
		EPoint location;

		DaisyChainPoint(PortInst p, Point2D loc)
		{
			pi = p;
			location = new EPoint(loc.getX(), loc.getY());
		}
	}

	/**
	 * Class to sort DaisyChainPoints.
	 */
	private static class SortDaisyPoints implements Comparator<DaisyChainPoint>
	{
		public int compare(DaisyChainPoint dcp1, DaisyChainPoint dcp2)
		{
			if (dcp1.location.getX() < dcp2.location.getX()) return 1;
			if (dcp1.location.getX() > dcp2.location.getX()) return -1;
			if (dcp1.location.getY() < dcp2.location.getY()) return 1;
			if (dcp1.location.getY() > dcp2.location.getY()) return -1;
			return 0;
		}
	}

	/**
	 * Method to see if an ArcInst daisy-chains over multiple ports.
	 * @param ai the ArcInst in question.
	 * @param nodePortBounds quad-tree bounds information for all nodes in the Cell.
	 * @param stayInside is the area in which to route (null to route arbitrarily).
	 * @param top network information for the Cell with these objects.
	 */
	private void checkDaisyChain(ArcInst ai, Map<NodeInst, ObjectQTree> nodePortBounds, PolyMerge stayInside, StitchingTopology top)
	{
		// do not daisy-chain busses
		if (ai.getProto() == Schematics.tech().bus_arc) return;
		
		// make a list of PortInsts that are on the centerline of this arc
		Cell cell = ai.getParent();

		Network arcNet = top.getArcNetwork(ai);
		Point2D e1 = ai.getHeadLocation();
		Point2D e2 = ai.getTailLocation();
		List<DaisyChainPoint> daisyPoints = new ArrayList<DaisyChainPoint>();
		Rectangle2D searchBounds = ai.getBounds();
		for(Iterator<RTBounds> it = cell.searchIterator(searchBounds); it.hasNext(); )
		{
			Geometric geom = (Geometric)it.next();
			if (geom instanceof NodeInst)
			{
				NodeInst ni = (NodeInst)geom;

				// find ports on this arc
				ObjectQTree oqt = nodePortBounds.get(ni);
				Set set = oqt.find(searchBounds);
				if (set != null)
				{
					for (Object obj : set)
					{
						PortInst pi = (PortInst)obj;
						if (!pi.getPortProto().getBasePort().connectsTo(ai.getProto())) continue;
						PolyBase portPoly = pi.getPoly();
						Point2D closest = GenMath.closestPointToSegment(e1, e2, portPoly.getCenter());

						// if this port can connect, save it
						if (DBMath.pointInRect(closest, portPoly.getBounds2D()))
						{
							// ignore if they are already connected
							Network portNet = top.getPortNetwork(pi);
							if (portNet == arcNet) continue;
							daisyPoints.add(new DaisyChainPoint(pi, closest));
						}
					}
				}
			}
		}

		// now see if there are multiple intermediate daisy-chain points
		if (daisyPoints.size() <= 1) return;
		Collections.sort(daisyPoints, new SortDaisyPoints());

		Route route = new Route();
        String name = ai.getName();
//System.out.println("===DELETING DAISY CHAIN ARC FROM ("+ai.getHeadLocation().getX()+","+ai.getHeadLocation().getY()+") TO ("+
//ai.getTailLocation().getX()+","+ai.getTailLocation().getY()+") IN CELL "+ai.getParent().describe(false));
//        route.add(RouteElementArc.deleteArc(ai));
name=null;

        RouteElementPort headRE = RouteElementPort.existingPortInst(ai.getHeadPortInst(), ai.getHeadLocation());
        RouteElementPort tailRE = RouteElementPort.existingPortInst(ai.getTailPortInst(), ai.getTailLocation());
        DaisyChainPoint firstDCP = daisyPoints.get(0);
        DaisyChainPoint lastDCP = daisyPoints.get(daisyPoints.size()-1);
        double distOK = firstDCP.location.distance(ai.getHeadLocation()) +
        	lastDCP.location.distance(ai.getTailLocation());
        double distSwap = firstDCP.location.distance(ai.getTailLocation()) +
    		lastDCP.location.distance(ai.getHeadLocation());
        if (distOK > distSwap)
        {
        	RouteElementPort swap = headRE;   headRE = tailRE;   tailRE = swap;
        }

//        if (headRE.getNodeInst().getNumConnections() == 1 && headRE.getLocation().equals(firstDCP.location))
//        {
//        	route.add(RouteElementPort.deleteNode(headRE.getNodeInst()));
//        	headRE = null;
//        }
//        if (tailRE.getNodeInst().getNumConnections() == 1 && tailRE.getLocation().equals(lastDCP.location))
//        {
//        	route.add(RouteElementPort.deleteNode(tailRE.getNodeInst()));
//        	tailRE = null;
//        }

        for(DaisyChainPoint dcp : daisyPoints)
        {
            RouteElementPort dcpRE = RouteElementPort.existingPortInst(dcp.pi, dcp.location);
            if (headRE != null && headRE.getPortInst() != tailRE.getPortInst())
            {
            	RouteElementArc re = RouteElementArc.newArc(cell, ai.getProto(), ai.getLambdaBaseWidth(), headRE, dcpRE,
	        		headRE.getConnectingSite().getCenter(), dcpRE.getConnectingSite().getCenter(), name,
	        		ai.getTextDescriptor(ArcInst.ARC_NAME), ai, ai.isHeadExtended(), ai.isTailExtended(), stayInside);
	            route.add(re);
            }
        	headRE = dcpRE;
        	name = null;
        }
        if (tailRE != null)
        {
        	if (headRE.getPortInst() != tailRE.getPortInst())
        	{
	        	RouteElementArc re = RouteElementArc.newArc(cell, ai.getProto(), ai.getLambdaBaseWidth(), headRE, tailRE,
		    		headRE.getConnectingSite().getCenter(), tailRE.getConnectingSite().getCenter(), name,
		    		ai.getTextDescriptor(ArcInst.ARC_NAME), ai, ai.isHeadExtended(), ai.isTailExtended(), stayInside);
		        route.add(re);
        	}
        }
		allRoutes.add(route);
	}

	/****************************************** NORMAL STITCHING ******************************************/

	/**
	 * Method to check an object for possible stitching to neighboring objects.
	 * @param geom the object to check for stitching.
	 * @param nodePortBounds quad-tree bounds information for all nodes in the Cell.
	 * @param arcLayers a map from ArcProtos to Layers.
	 * @param stayInside is the area in which to route (null to route arbitrarily).
	 * @param top network information for the Cell with these objects.
	 * @param limitBound if not null, only consider connections that occur in this area.
	 * @param preferredArc preferred ArcProto to use.
	 */
	private void checkStitching(Geometric geom, Map<NodeInst, ObjectQTree> nodePortBounds, Map<ArcProto,Layer> arcLayers,
		PolyMerge stayInside, StitchingTopology top, Rectangle2D limitBound, ArcProto preferredArc)
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
		for(Iterator<RTBounds> it = cell.searchIterator(searchBounds); it.hasNext(); )
		{
			Geometric oGeom = (Geometric)it.next();
			if (oGeom != geom) geomsInArea.add(oGeom);
		}
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
					compareTwoArcs((ArcInst)geom, oAi, stayInside, top);
					continue;
				}

				// compare node "ni" against arc "oAi"
				if (ni.isCellInstance())
				{
					compareNodeInstWithArc(ni, oAi, stayInside, top, nodePortBounds);
				} else
				{
					compareNodePrimWithArc(ni, oAi, stayInside, top);
				}
			} else
			{
				// other geometric a NodeInst
				NodeInst oNi = (NodeInst)oGeom;
				if (!oNi.isCellInstance())
				{
					PrimitiveNode pnp = (PrimitiveNode)oNi.getProto();
					if (pnp.getTechnology() == Generic.tech()) continue;
					if (!includePureLayerNodes && pnp.getFunction() == PrimitiveNode.Function.NODE) continue;
				}

				if (ni == null)
				{
					// compare arc "geom" against node "oNi"
					if (oNi.isCellInstance())
					{
						compareNodeInstWithArc(oNi, (ArcInst)geom, stayInside, top, nodePortBounds);
					} else
					{
						compareNodePrimWithArc(oNi, (ArcInst)geom, stayInside, top);
					}
					continue;
				}

				// compare node "ni" against node "oNi"
				compareTwoNodes(ni, oNi, nodePortBounds, arcLayers, stayInside, top, limitBound, preferredArc);
			}
		}
	}

	/**
	 * Method to compare two nodes and see if they should be connected.
	 * @param ni the first NodeInst to compare.
	 * @param oNi the second NodeInst to compare.
	 * @param nodePortBounds quad-tree bounds information for all nodes in the Cell.
	 * @param arcLayers a map from ArcProtos to Layers.
	 * @param stayInside is the area in which to route (null to route arbitrarily).
	 * @param top network information for the Cell with these nodes.
	 * @param limitBound if not null, only consider connections that occur in this area.
	 * @param preferredArc preferred ArcProto to use.
	 */
	private void compareTwoNodes(NodeInst ni, NodeInst oNi, Map<NodeInst,ObjectQTree> nodePortBounds,
		Map<ArcProto,Layer> arcLayers, PolyMerge stayInside,
		StitchingTopology top, Rectangle2D limitBound, ArcProto preferredArc)
	{
		// if both nodes are being checked, examine them only once
		if (nodeMark.contains(oNi) && oNi.getNodeIndex() <= ni.getNodeIndex()) return;

		// now look at every layer in this node
		Rectangle2D oBounds = oNi.getBounds();
		if (ni.isCellInstance())
		{
			// complex node instance: look at all ports near this bound
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
					if (tot == 0 || rNi.getProto() == Generic.tech().simProbeNode)
					{
						usePortPoly = true;
						tot = 1;
					}
					Netlist subNetlist = rNi.getParent().getNetlist();
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
								connected = testPoly(ni, pp, ap, poly, oNi, top, nodePortBounds, arcLayers, stayInside, limitBound);
								if (connected) break;
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
			if (tot == 0 || ni.getProto() == Generic.tech().simProbeNode)
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
						if (bestPp == null)
						{
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
						if (!top.portsConnected(ni, tPp, polyPtr.getPort())) continue;

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
					if (!top.portsConnected(ni, rPp, pi.getPortProto())) continue;
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
						connected = testPoly(ni, rPp, ap, polyPtr, oNi, top, nodePortBounds, arcLayers, stayInside, limitBound);
						if (connected) break;
					}
					if (connected) break;
				}
				if (connected) break;
			}
		}
	}

	/**
	 * Method to compare two arcs and see if they should be connected.
	 * @param ai1 the first ArcInst to compare.
	 * @param ai2 the second ArcInst to compare.
	 * @param stayInside is the area in which to route (null to route arbitrarily).
	 * @param top the Netlist information for the Cell with the arcs.
	 */
	private void compareTwoArcs(ArcInst ai1, ArcInst ai2, PolyMerge stayInside, StitchingTopology top)
	{
		// if connected, stop now
		if (ai1.getProto() != ai2.getProto()) return;
		Network net1 = top.getArcNetwork(ai1);
		Network net2 = top.getArcNetwork(ai2);
		if (net1 == net2) return;

		// look at all polygons on the first arcinst
		Poly [] polys1 = ai1.getProto().getTechnology().getShapeOfArc(ai1);
		int tot1 = polys1.length;
		Poly [] polys2 = ai2.getProto().getTechnology().getShapeOfArc(ai2);
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
				connectObjects(ai1, net1, ai2, net2, ai1.getParent(), new Point2D.Double(x,y), stayInside, top);
				return;
			}
		}
	}

	/**
	 * Method to compare a node instance and an arc to see if they touch and should be connected.
	 * @param ni the NodeInst to compare.
	 * @param ai the ArcInst to compare.
	 * @param stayInside is the area in which to route (null to route arbitrarily).
	 * @param top the Netlist information for the Cell with the node and arc.
	 * @param nodePortBounds quad-tree bounds information for all nodes in the Cell.
	 */
	private void compareNodeInstWithArc(NodeInst ni, ArcInst ai, PolyMerge stayInside, StitchingTopology top,
		Map<NodeInst,ObjectQTree> nodePortBounds)
	{
		Network arcNet = top.getArcNetwork(ai);

		// find all ports on the instance that are near the arc
		ObjectQTree oqt = nodePortBounds.get(ni);
		Rectangle2D aBounds = ai.getBounds();
		Rectangle2D biggerBounds = new Rectangle2D.Double(aBounds.getMinX()-1, aBounds.getMinY()-1, aBounds.getWidth()+2, aBounds.getHeight()+2);
		Set set = oqt.find(biggerBounds);
		if (set == null || set.size() == 0) return;

		// look at all polygons on the arcinst
		Poly [] arcPolys = ai.getProto().getTechnology().getShapeOfArc(ai);
		int aTot = arcPolys.length;
		for(int i=0; i<aTot; i++)
		{
			Poly arcPoly = arcPolys[i];
			Layer arcLayer = arcPoly.getLayer();
			Layer.Function arcLayerFun = arcLayer.getFunction();
			if (!arcLayerFun.isMetal() && !arcLayerFun.isDiff() && !arcLayerFun.isPoly()) continue;

			// find ports near the arc
			for (Object obj : set)
			{
				PortInst pi = (PortInst)obj;

				// ignore if already connected
				Network portNet = top.getPortNetwork(pi);
				if (portNet == arcNet) continue;

				// find the primitive node at the bottom of the port
				AffineTransform trans = ni.rotateOut();
				NodeInst rNi = ni;
				PortProto rPp = pi.getPortProto();
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

				// see if anything on the base node touches the arc
				Poly [] polys = shapeOfNode(rNi);
				int tot = polys.length;
				for(int j=0; j<tot; j++)
				{
					Poly baseNodePoly = polys[j];
					Layer nodeLayer = baseNodePoly.getLayer();
					if (nodeLayer == null) continue;
					nodeLayer = nodeLayer.getNonPseudoLayer();
					if (nodeLayer.getFunction() != arcLayerFun) continue;
					baseNodePoly.transform(trans);
					double polyDist = arcPoly.separation(baseNodePoly);
					if (polyDist >= DBMath.getEpsilon()) continue;

					// arc touches the port: connect them
					Poly portPoly = pi.getPoly();
					double portCX = portPoly.getCenterX();
					double portCY = portPoly.getCenterY();

					Rectangle2D arcBounds = arcPoly.getBounds2D();
					double aCX = arcBounds.getCenterX();
					double aCY = arcBounds.getCenterY();
					Point2D bend1 = new Point2D.Double(portCX, aCY);
					Point2D bend2 = new Point2D.Double(aCX, portCY);
					if (stayInside != null)
					{
						if (!stayInside.contains(arcLayer, bend1)) bend1 = bend2;
					} else
					{
						if (!arcPoly.contains(bend1)) bend1 = bend2;
					}
					connectObjects(ai, arcNet, pi, portNet, ai.getParent(), bend1, stayInside, top);
					return;
				}
			}
		}
	}

	/**
	 * Method to compare a primitive node and an arc to see if they touch and should be connected.
	 * @param ni the NodeInst to compare.
	 * @param ai the ArcInst to compare.
	 * @param stayInside is the area in which to route (null to route arbitrarily).
	 * @param top the Netlist information for the Cell with the node and arc.
	 */
	private void compareNodePrimWithArc(NodeInst ni, ArcInst ai, PolyMerge stayInside, StitchingTopology top)
	{
		Network arcNet = top.getArcNetwork(ai);

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
			Layer.Function arcLayerFun = arcLayer.getFunction();
			if (!arcLayerFun.isMetal() && !arcLayerFun.isDiff() && !arcLayerFun.isPoly()) continue;
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
				if (nodeLayer.getFunction() != arcLayerFun) continue;
				double polyDist = arcPoly.separation(nodePoly);
				if (polyDist >= DBMath.getEpsilon()) continue;

				// only want electrically connected polygons
				if (nodePoly.getPort() == null) continue;

				// search all ports for the closest connected to this layer
				PortProto bestPp = null;
				double bestDist = 0;
				for(Iterator<PortProto> pIt = ni.getProto().getPorts(); pIt.hasNext(); )
				{
					PortProto tPp = pIt.next();
					if (!top.portsConnected(ni, tPp, nodePoly.getPort())) continue;

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
				Network nodeNet = top.getPortNetwork(pi);
				if (arcNet == nodeNet) continue;
				if (alignment != null)
				{
					if (alignment.getWidth() > 0)
					{
						portCX = Math.round(portCX / alignment.getWidth()) * alignment.getWidth();
						aCX = Math.round(aCX / alignment.getWidth()) * alignment.getWidth();
					}
					if (alignment.getHeight() > 0)
					{
						portCY = Math.round(portCY / alignment.getHeight()) * alignment.getHeight();
						aCY = Math.round(aCY / alignment.getHeight()) * alignment.getHeight();
					}
				}
				Point2D bend1 = new Point2D.Double(portCX, aCY);
				Point2D bend2 = new Point2D.Double(aCX, portCY);
				if (stayInside != null)
				{
					if (!stayInside.contains(arcLayer, bend1)) bend1 = bend2;
				} else
				{
					if (!arcPoly.contains(bend1)) bend1 = bend2;
				}
				connectObjects(ai, arcNet, pi, nodeNet, ai.getParent(), bend1, stayInside, top);
				return;
			}
		}
	}

	/**
	 * Method to connect two nodes if they touch.
	 * @param ni the first node to test.
	 * @param pp the port on the first node to test.
	 * @param ap the arcproto to use when connecting the nodes.
	 * @param poly the polygon on the first node to test.
	 * @param oNi the second node to test.
	 * @param top network information for the cell with the nodes.
	 * @param nodePortBounds quad-tree bounds information for all nodes in the Cell.
	 * @param arcLayers a map from ArcProtos to Layers.
	 * @param stayInside is the area in which to route (null to route arbitrarily).
	 * @param limitBound if not null, only consider connections that occur in this area.
	 * @return the number of connections made (0 if none).
	 */
	private boolean testPoly(NodeInst ni, PortProto pp, ArcProto ap, Poly poly, NodeInst oNi, StitchingTopology top,
		Map<NodeInst, ObjectQTree> nodePortBounds,
		Map<ArcProto,Layer> arcLayers, PolyMerge stayInside, Rectangle2D limitBound)
	{
//System.out.println("FOR LAYER "+ap.getName()+" CONSIDER PORT "+pp.getName()+" OF NODE "+ni.describe(false));
		// get network associated with the node/port
		PortInst pi = ni.findPortInstFromProto(pp);

		// keep track of which networks we've already tied together
        Set<Network> netsConnectedTo = new TreeSet<Network>();
        Network net = top.getNodeNetwork(ni, pp);
        if (net == null) return false;
        netsConnectedTo.add(net);

        // now look at every layer in this node
		if (oNi.isCellInstance())
		{
			// complex cell: look at all exports
			Rectangle2D bounds = poly.getBounds2D();

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

//                    // keep track of which networks we've already tied together
//                    Cell subcell = (Cell)oNi.getProto();
//                    Netlist netlist = subcell.getNetlist();
//                    if (mPp instanceof Export) {
//                        Export mPpe = (Export)mPp;
//                        Network netm = netlist.getNetwork(mPpe, 0);
//                        assert netm != null;
//                        if (netsConnectedTo.contains(netm)) continue;
//                        netsConnectedTo.add(netm);
//                    }

					// port must be able to connect to the arc
					if (!mPp.getBasePort().connectsTo(ap)) continue;

					// do not stitch where there is already an electrical connection
					Network oNet = top.getPortNetwork(oNi.findPortInstFromProto(mPp));
					if (net != null && oNet == net) continue;

					// do not stitch if there is already an arc connecting these two ports
					boolean ignore = false;
					for (Iterator<Connection> piit = oPi.getConnections(); piit.hasNext(); )
					{
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

                    // keep track of which sub-networks we've already considered
                    Cell subcell = (Cell)oNi.getProto();
                    Netlist netlist = subcell.getNetlist();
                    if (mPp instanceof Export) {
                        Export mPpe = (Export)mPp;
                        Network netm = netlist.getNetwork(mPpe, 0);
                        assert netm != null;
                        if (netsConnectedTo.contains(netm))
                        {
//System.out.println("   ---PORT "+mPp.getName()+" OF NODE "+oPi.getNodeInst().describe(false)+" FAILS NETLIST");
                        	continue;
                        }
                        netsConnectedTo.add(netm);
                    }

					// see how much geometry is on this node
					Poly [] polys = shapeOfNode(rNi);
					int tot = polys.length;
					if (tot == 0)
					{
						// not a geometric primitive: look for ports that touch
						Poly oPoly = oNi.getShapeOfPort(mPp);
						comparePoly(oNi, mPp, oPoly, oNet, ni, pp, poly, net, ap, stayInside, top, limitBound);
					} else
					{
						// a geometric primitive: look for ports on layers that touch
						Netlist subNetlist = rNi.getParent().getNetlist();
						for(int j=0; j<tot; j++)
						{
							Poly oPoly = polys[j];

							// only want electrically connected polygons
							if (oPoly.getPort() == null) continue;

							// only want polygons connected to correct part of nodeinst
							if (!subNetlist.portsConnected(rNi, rPp, oPoly.getPort())) continue;

							// if the polygon layer is pseudo, substitute real layer
							if (ni.getProto() != Generic.tech().simProbeNode)
							{
								Layer oLayer = oPoly.getLayer();
								if (oLayer != null) oLayer = oLayer.getNonPseudoLayer();
								Layer apLayer = arcLayers.get(ap);
								if (!oLayer.getTechnology().sameLayer(oLayer, apLayer)) continue;
							}

							// transform the polygon and pass it on to the next test
							oPoly.transform(trans);
							if (comparePoly(oNi, mPp, oPoly, oNet, ni, pp, poly, net, ap, stayInside, top, limitBound))
								break;
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
					if (bestPp == null)
					{
						bestDist = dist;
						bestPp = rPp;
					}
					if (dist > bestDist) continue;
					bestPp = rPp;   bestDist = dist;
				}
				if (bestPp != null)
				{
					PortProto rPp = bestPp;
					Network oNet = top.getPortNetwork(oNi.findPortInstFromProto(bestPp));
					if (net == null || oNet != net)
					{
						// port must be able to connect to the arc
						if (rPp.getBasePort().connectsTo(ap))
						{
							// transformed the polygon and pass it on to the next test
							Poly oPoly = oNi.getShapeOfPort(rPp);
							if (comparePoly(oNi, rPp, oPoly, oNet, ni, pp, poly, net, ap, stayInside, top, limitBound))
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
					Network oNet = top.getPortNetwork(oPi);
					if (net != null && oNet == net) continue;

					// search all ports for the closest connected to this layer
					PortProto bestPp = null;
					double bestDist = 0;
					for(Iterator<PortProto> pIt = oNi.getProto().getPorts(); pIt.hasNext(); )
					{
						PortProto rPp = pIt.next();
						if (!top.portsConnected(oNi, rPp, oPoly.getPort())) continue;

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
					if (comparePoly(oNi, rPp, oPoly, oNet, ni, pp, poly, net, ap, stayInside, top, limitBound))
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
	 * @param top the netlist for the Cell with the polygons.
	 * @param limitBound if not null, only consider connections that occur in this area.
	 * @return true if the connection is made.
	 */
	private boolean comparePoly(NodeInst oNi, PortProto opp, Poly oPoly, Network oNet,
		NodeInst ni, PortProto pp, Poly poly, Network net,
		ArcProto ap, PolyMerge stayInside, StitchingTopology top, Rectangle2D limitBound)
	{
		double sep = poly.separation(oPoly);
//System.out.println("   DISTANCE BETWEEN PORT "+pp.getName()+" OF NODE "+ni.describe(false)+" AND PORT "+opp.getName()+" OF NODE "+oNi.describe(false)+" IS "+sep);
		// find the bounding boxes of the polygons
		if (sep > DBMath.getEpsilon()) return false;

		// be sure the closest ports are being used
		Poly portPoly = ni.getShapeOfPort(pp);
		Point2D portCenter = new Point2D.Double(portPoly.getCenterX(), portPoly.getCenterY());
		Poly oPortPoly = oNi.getShapeOfPort(opp);
		Point2D oPortCenter = new Point2D.Double(oPortPoly.getCenterX(), oPortPoly.getCenterY());

		if (stayInside == null)
		{
			if (ni.isCellInstance() || oNi.isCellInstance())
			{
				Rectangle2D polyBounds = portPoly.getBounds2D();
				Rectangle2D oPolyBounds = oPortPoly.getBounds2D();

				// quit now if bounding boxes don't intersect
				if ((polyBounds.getMinX() > oPolyBounds.getMaxX() || oPolyBounds.getMinX() > polyBounds.getMaxX()) &&
					(polyBounds.getMinY() > oPolyBounds.getMaxY() || oPolyBounds.getMinY() > polyBounds.getMaxY())) return false;
			}
		}

		double dist = portCenter.distance(oPortCenter);
		for(Iterator<PortProto> it = oNi.getProto().getPorts(); it.hasNext(); )
		{
			PortProto tPp = it.next();
			if (tPp == opp) continue;
			if (!top.portsConnected(oNi, tPp, opp)) continue;
			if (!tPp.getBasePort().connectsTo(ap)) continue;
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
			if (!top.portsConnected(ni, tPp, pp)) continue;
			if (!tPp.getBasePort().connectsTo(ap)) continue;
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
			if (!DBMath.pointInRect(portCenter, limitBound) && !DBMath.pointInRect(oPortCenter, limitBound))
				return false;
		}

		// find some dummy position to help run the arc
		double x = (oPortCenter.getX() + portCenter.getX()) / 2;
		double y = (oPortCenter.getY() + portCenter.getY()) / 2;
		if (alignment != null)
		{
			if (alignment.getWidth() > 0)
				x = Math.round(x / alignment.getWidth()) * alignment.getWidth();
			if (alignment.getHeight() > 0)
				y = Math.round(y / alignment.getHeight()) * alignment.getHeight();
		}

		// run the wire
		PortInst pi = ni.findPortInstFromProto(pp);
		PortInst opi = oNi.findPortInstFromProto(opp);
//System.out.println("   *** MAKING THE CONNECTION");
		return connectObjects(pi, net, opi, oNet, ni.getParent(), new Point2D.Double(x,y), stayInside, top);
	}

	/**
	 * Method to connect two objects if they touch.
	 * @param eobj1 the first object (either an ArcInst or a PortInst).
	 * @param net1 the network on which the first object resides.
	 * @param eobj2 the second object (either an ArcInst or a PortInst).
	 * @param net2 the network on which the second object resides.
	 * @param cell the Cell in which these objects reside.
	 * @param ctr bend point suggestion when making "L" connection.
	 * @param stayInside is the area in which to route (null to route arbitrarily).
	 * @param top the topology of the cell.
	 * @return true if a connection is made.
	 */
	private boolean connectObjects(ElectricObject eobj1, Network net1, ElectricObject eobj2, Network net2,
		Cell cell, Point2D ctr, PolyMerge stayInside, StitchingTopology top)
	{
		// run the wire
		NodeInst ni1 = null;
		if (eobj1 instanceof NodeInst) ni1 = (NodeInst)eobj1; else
			if (eobj1 instanceof PortInst) ni1 = ((PortInst)eobj1).getNodeInst();

		NodeInst ni2 = null;
		if (eobj2 instanceof NodeInst) ni2 = (NodeInst)eobj2; else
			if (eobj2 instanceof PortInst) ni2 = ((PortInst)eobj2).getNodeInst();

		Rectangle2D nullRect = null;
		Route route = router.planRoute(cell, eobj1, eobj2, ctr, stayInside, true, true, nullRect, alignment);
		if (route.size() == 0) return false;

        allRoutes.add(route);
		top.connect(net1, net2);

		// if either ni or oNi is a pin primitive, see if it is a candidate for clean-up
		if (ni1 != null)
		{
			if (ni1.getFunction().isPin() &&
				!ni1.hasExports() && !ni1.hasConnections())
			{
				if (!possibleInlinePins.contains(ni1))
					possibleInlinePins.add(ni1);
			}
		}
		if (ni2 != null)
		{
			if (ni2.getFunction().isPin() &&
				!ni2.hasExports() && !ni2.hasConnections())
			{
				if (!possibleInlinePins.contains(ni2))
					possibleInlinePins.add(ni2);
			}
		}
		return true;
	}

    /****************************************** EXPORT-CREATION STITCHING ******************************************/

	/**
	 * Method to check an object for possible stitching to neighboring objects with export creation.
	 * Actual stitching is not done, but necessary exports are created.
	 * @param geom the object to check for stitching.
	 */
	private void checkExportCreationStitching(Geometric geom, Map<Long,List<PolyConnection>> overlapMap, GatherNetworksVisitor gatherNetworks)
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
		for(Iterator<RTBounds> it = cell.searchIterator(searchBounds); it.hasNext(); )
		{
			Geometric oGeom = (Geometric)it.next();
			if (oGeom != geom) geomsInArea.add(oGeom);
		}
		for(Geometric oGeom : geomsInArea)
		{
			// find another node in this area
			if (oGeom instanceof ArcInst)
			{
				// other geometric is an ArcInst
				ArcInst oAi = (ArcInst)oGeom;

				if (ni == null) continue;

				// compare node "ni" against arc "oAi"
				if (ni.isCellInstance())
					compareNodeInstWithArcMakeExport(ni, oAi, overlapMap, gatherNetworks);
			} else
			{
				// other geometric a NodeInst
				NodeInst oNi = (NodeInst)oGeom;
				if (!oNi.isCellInstance())
				{
					PrimitiveNode pnp = (PrimitiveNode)oNi.getProto();
					if (pnp.getTechnology() == Generic.tech()) continue;
					if (!includePureLayerNodes && pnp.getFunction() == PrimitiveNode.Function.NODE) continue;
					if (includePureLayerNodes && pnp.getFunction() == PrimitiveNode.Function.NODE) {
                        // check that pure layer node has routable layer (this filters dummy layers, etc)
                        boolean hasValidLayer = false;
                        for (Iterator<Layer> layIt = pnp.getLayerIterator(); layIt.hasNext(); ) {
                            Layer l = layIt.next();
                            if (l.getFunction().isMetal() || l.getFunction().isDiff() || l.getFunction().isPoly()) {
                                hasValidLayer = true; break;
                            }
                        }
                        if (!hasValidLayer) continue;
                    } else {
                        continue;
                    }
                }

				if (ni == null)
				{
					// compare arc "geom" against node "oNi"
					if (oNi.isCellInstance())
						compareNodeInstWithArcMakeExport(oNi, (ArcInst)geom, overlapMap, gatherNetworks);
					continue;
				}

				// compare node "ni" against node "oNi"
				if (ni.isCellInstance())
				{
					compareTwoNodesMakeExport(ni, oNi, overlapMap, gatherNetworks);
				}
			}
		}
	}

	/**
	 * Method to compare a node instance and an arc to see if they touch and should be connected and an export created.
	 * @param ni the NodeInst to compare.
	 * @param ai the ArcInst to compare.
	 */
	private void compareNodeInstWithArcMakeExport(NodeInst ni, ArcInst ai, Map<Long,List<PolyConnection>> overlapMap, GatherNetworksVisitor gatherNetworks)
	{
		// get the polygon and layer that needs to connect
		Poly arcPoly = null;
		Poly [] arcPolys = ai.getProto().getTechnology().getShapeOfArc(ai);
		int aTot = arcPolys.length;
		for(int i=0; i<aTot; i++)
		{
			arcPoly = arcPolys[i];
			Layer arcLayer = arcPoly.getLayer();
			Layer.Function arcLayerFun = arcLayer.getFunction();
			if (arcLayerFun.isMetal() || arcLayerFun.isDiff() || arcLayerFun.isPoly()) break;
			arcPoly = null;
		}
		if (arcPoly == null) return;

        // this is the arc ai at the top level
        int netID = gatherNetworks.getGlobalNetworkID(VarContext.globalContext, ai.getHeadPortInst());
        SubPolygon sp2 = new SubPolygon(arcPoly, VarContext.globalContext, netID, ai, null);

		// look for geometry inside the cell that touches the arc, and make an export so it can connect
		ArcTouchVisitor atv = new ArcTouchVisitor(ai, arcPoly, ni, false, gatherNetworks);
		HierarchyEnumerator.enumerateCell(ni.getParent(), VarContext.globalContext, atv);
		SubPolygon sp = atv.getExportDrillLocation();
		if (sp != null)
		{
            registerPoly(overlapMap, new PolyConnection(sp, sp2));
            //makeExportDrill((NodeInst)sp.theObj, sp.poly.getPort(), sp.context, null, null);
			return;
		}

		// try arcs
		atv.setDoArcs(true);
		HierarchyEnumerator.enumerateCell(ni.getParent(), VarContext.globalContext, atv);
		sp = atv.getExportDrillLocation();
		if (sp != null)
		{
            registerPoly(overlapMap, new PolyConnection(sp, sp2));

//			// get arc transformed to top-level
//			ArcInst breakArc = (ArcInst)sp.theObj;
//			if (!breakArc.isLinked()) return;
//			Point2D head = breakArc.getHeadLocation();
//			head = new Point2D.Double(head.getX(), head.getY());
//			Point2D tail = breakArc.getTailLocation();
//			tail = new Point2D.Double(tail.getX(), tail.getY());
//			sp.xfToTop.transform(head, head);
//			sp.xfToTop.transform(tail, tail);
//			int angle = DBMath.figureAngle(head, tail);
//
//			// find where it intersects the top-level arc
//			Point2D breakPt = null;
//			if (angle%1800 == ai.getAngle()%1800)
//			{
//				if (DBMath.distToLine(head, tail, ai.getHeadLocation()) <
//					DBMath.distToLine(head, tail, ai.getTailLocation()))
//				{
//					breakPt = DBMath.intersect(head, angle, ai.getHeadLocation(), (ai.getAngle()+900)%3600);
//				} else
//				{
//					breakPt = DBMath.intersect(head, angle, ai.getTailLocation(), (ai.getAngle()+900)%3600);
//				}
//			} else
//			{
//				breakPt = DBMath.intersect(head, angle, ai.getHeadLocation(), ai.getAngle());
//			}
//			if (breakPt == null) return;
//
//			// transform the intersection point back down into low-level
//			try
//			{
//				sp.xfToTop.inverseTransform(breakPt, breakPt);
//			} catch (NoninvertibleTransformException e) { return; }
//
//			// break the arc at that point
//			PrimitiveNode pinType = breakArc.getProto().findPinProto();
//			NodeInst pin = NodeInst.newInstance(pinType, breakPt, pinType.getDefaultLambdaBaseWidth(ep),
//				pinType.getDefaultLambdaBaseHeight(ep), breakArc.getParent());
//			if (pin == null) return;
//
//			PortInst pi = pin.getOnlyPortInst();
//			PortInst headPort = breakArc.getHeadPortInst();
//            PortInst tailPort = breakArc.getTailPortInst();
//            Point2D headPt = breakArc.getHeadLocation();
//            Point2D tailPt = breakArc.getTailLocation();
//            double width = breakArc.getLambdaBaseWidth();
//            String arcName = breakArc.getName();
//
//            // create the new arcs
//            ArcInst newAi1 = ArcInst.makeInstanceBase(breakArc.getProto(), width, headPort, pi, headPt, breakPt, null);
//            ArcInst newAi2 = ArcInst.makeInstanceBase(breakArc.getProto(), width, pi, tailPort, breakPt, tailPt, null);
//            if (newAi1 == null || newAi2 == null) return;
//            newAi1.setHeadNegated(breakArc.isHeadNegated());
//            newAi1.setHeadExtended(breakArc.isHeadExtended());
//            newAi1.setHeadArrowed(breakArc.isHeadArrowed());
//            newAi2.setTailNegated(breakArc.isTailNegated());
//            newAi2.setTailExtended(breakArc.isTailExtended());
//            newAi2.setTailArrowed(breakArc.isTailArrowed());
//            breakArc.kill();
//            if (arcName != null)
//            {
//                if (headPt.distance(breakPt) > tailPt.distance(breakPt))
//                {
//                    newAi1.setName(arcName);
//                    newAi1.copyTextDescriptorFrom(breakArc, ArcInst.ARC_NAME);
//                } else
//                {
//                	newAi2.setName(arcName);
//                	newAi2.copyTextDescriptorFrom(breakArc, ArcInst.ARC_NAME);
//                }
//            }
//
//            // now drill the break pin to the top
//			makeExportDrill(pin, pi.getPortProto(), sp.context, null, null);
		}
	}

    /**
     * Make an export if necessary to connect the two networks.
     * Each pair of subpolys in the list represents the same two networks,
     * so being able to connect one pair means the others will also be connected.
     * @param polys A list of poly connections (pairs of subpolygons)
     */
    private void makeExport(List<PolyConnection> polys)
    {
        if (polys.size() == 0) return;

        // check for existing export
        Point2D sp1AtTop = null, sp2AtTop = null;
        for (PolyConnection p : polys)
        {
            sp1AtTop = isExportedToTop(p.sp1);
            sp2AtTop = isExportedToTop(p.sp2);
            if (sp1AtTop != null && sp2AtTop != null) return; // both exported to top
        }
        if (sp1AtTop != null) sp1AtTop = new Point2D.Double(sp1AtTop.getX(), sp1AtTop.getY()); //convert Epoint to Point2D
        if (sp2AtTop != null) sp2AtTop = new Point2D.Double(sp2AtTop.getX(), sp2AtTop.getY()); //convert Epoint to Point2D

        // none can connect at top level, export up first pair
        PolyConnection p = polys.get(0);
        List<Line2D> overlappingEdges = new ArrayList<Line2D>();
        List<PolyBase> intersectionList = p.sp1.poly.getIntersection(p.sp2.poly, overlappingEdges);
        PolyBase preferredExportArea = null;
        if (intersectionList != null && intersectionList.size() > 0) {
            preferredExportArea = intersectionList.get(0);
        } else if (overlappingEdges.size() > 0) {
            preferredExportArea = new PolyBase(overlappingEdges.get(0).getBounds());
        }

        // figure out which arc to use to connect the two - needed to decide how to create exports
        ArcProto ap = null;
        if (p.sp1.theObj instanceof ArcInst) {
            ap = ((ArcInst)p.sp1.theObj).getProto();
        }
        if (p.sp2.theObj instanceof ArcInst) {
            ap = ((ArcInst)p.sp2.theObj).getProto();
        }
        if (ap == null) {
            ap = Router.getArcToUse(p.sp1.poly.getPort(), p.sp2.poly.getPort());
        }

        // make nodeinst exports first to get locations for arc exports
        if (sp1AtTop == null && (p.sp1.theObj instanceof NodeInst)) {
            sp1AtTop = makeExportDrill((NodeInst)p.sp1.theObj, p.sp1.poly.getPort(), p.sp1.context, preferredExportArea, ap);
        }
        if (sp2AtTop == null && (p.sp2.theObj instanceof NodeInst)) {
            sp2AtTop = makeExportDrill((NodeInst)p.sp2.theObj, p.sp2.poly.getPort(), p.sp2.context, preferredExportArea, ap);
        }

        // make arc inst connections
        if (sp1AtTop == null && (p.sp1.theObj instanceof ArcInst) && sp2AtTop != null) {
            makeExportDrillOnArc(sp2AtTop, p.sp1, preferredExportArea);
            //System.out.println("Making export on arc for netID "+p.sp1.netID);
        }
        if (sp2AtTop == null && (p.sp2.theObj instanceof ArcInst) && sp1AtTop != null) {
            makeExportDrillOnArc(sp1AtTop, p.sp2, preferredExportArea);
            //System.out.println("Making export on arc for netID "+p.sp2.netID);
        }
    }

    /**
     * See if the subpolygon is exported all the way to the top level of the var context.
     * @param sp the subpolygon
     * @return true if exported to the top level, false otherwise
     */
    private Point2D isExportedToTop(SubPolygon sp)
    {
        Geometric geom = sp.theObj;
        if (geom instanceof NodeInst)
        {
            NodeInst ni = (NodeInst)geom;
            PortInst pi = ni.findPortInstFromProto(sp.poly.getPort());
            return isExportedToTop(pi, sp.context);
        }
        if (geom instanceof ArcInst)
        {
            ArcInst ai = (ArcInst)geom;
            PortInst pi1 = ai.getHead().getPortInst();
            PortInst pi2 = ai.getTail().getPortInst();
            Point2D point1 = isExportedToTop(pi1, sp.context);
            if (point1 != null) return point1;
            return isExportedToTop(pi2, sp.context);
        }
        return null;
    }

    /**
     * See if the portinst is exported all the way to the top level of the var context.
     * @param pi the port inst
     * @param context context
     * @return true if exported to the top level, false otherwise
     */
    private Point2D isExportedToTop(PortInst pi, VarContext context)
    {
        while (context != VarContext.globalContext)
        {
            if (pi.getExports().hasNext())
            {
                Export e = pi.getExports().next();
                Nodable no = context.getNodable();
                if (no instanceof NodeInst)
                {
                    NodeInst ni = (NodeInst)no;
                    pi = ni.findPortInstFromProto(e);
                    context = context.pop();
                } else
                {
                    return null;
                }
            } else
            {
                return null;
            }
        }
        return pi.getCenter();
    }

    /**
	 * HierarchyEnumerator subclass to find cell geometry that touches an arc at the upper level.
	 */
	private class ArcTouchVisitor extends HierarchyEnumerator.Visitor
	{
		private ArcInst arcOfInterest;
		private Poly arcPoly;
		private int arcNetID;
		private NodeInst cellOfInterest;
		private boolean doArcs;
		private Rectangle2D arcBounds;
		private SubPolygon bestSubPolygon;
        private GatherNetworksVisitor gatherNetworks;

        public ArcTouchVisitor(ArcInst arcOfInterest, Poly arcPoly, NodeInst cellOfInterest, boolean doArcs, GatherNetworksVisitor gatherNetworks)
		{
			this.arcOfInterest = arcOfInterest;
			this.arcPoly = arcPoly;
			this.cellOfInterest = cellOfInterest;
			this.doArcs = doArcs;
			arcNetID = -1;
			arcBounds = arcPoly.getBounds2D();
			bestSubPolygon = null;
            this.gatherNetworks = gatherNetworks;
        }

		public SubPolygon getExportDrillLocation() { return bestSubPolygon; }

		public void setDoArcs(boolean doArcs) { this.doArcs = doArcs; }

		public boolean enterCell(HierarchyEnumerator.CellInfo info) { return true; }

		public void exitCell(HierarchyEnumerator.CellInfo info)
		{
			if (info.isRootCell()) return;
			Netlist nl = info.getNetlist();

			if (doArcs)
			{
				// look at all arcs and see if they intersect the arc
				for(Iterator<ArcInst> it = info.getCell().getArcs(); it.hasNext(); )
				{
					ArcInst ai = it.next();
					ArcProto ap = ai.getProto();
					AffineTransform arcTrans = info.getTransformToRoot();
					Technology tech = ap.getTechnology();
					Poly [] arcInstPolyList = tech.getShapeOfArc(ai);
					for(int i=0; i<arcInstPolyList.length; i++)
					{
						PolyBase poly = arcInstPolyList[i];
						if (poly.getLayer() != arcPoly.getLayer()) continue;
						int netID = -1;
						Network net = nl.getNetwork(ai, 0);
						if (net != null) netID = info.getNetID(net);
						if (netID == arcNetID) continue;
						poly.transform(arcTrans);
						double dist = poly.separation(arcPoly);
						if (dist >= DBMath.getEpsilon()) continue;
						int netIDglobal = gatherNetworks.getGlobalNetworkID(info.getContext(), ai.getHeadPortInst());
                        SubPolygon sp = new SubPolygon(poly, info.getContext(), netIDglobal, ai, arcTrans);
						bestSubPolygon = sp;
					}
				}
			} else
			{
				// look at all nodes and see if they intersect the arc
				for(Iterator<NodeInst> it = info.getCell().getNodes(); it.hasNext(); )
				{
					NodeInst ni = it.next();
					if (ni.isCellInstance()) continue;
					PrimitiveNode pNp = (PrimitiveNode)ni.getProto();
					AffineTransform nodeTrans = ni.rotateOut(info.getTransformToRoot());
					Technology tech = pNp.getTechnology();
					Poly [] nodeInstPolyList = tech.getShapeOfNode(ni, true, true, null);
					for(int i=0; i<nodeInstPolyList.length; i++)
					{
						PolyBase poly = nodeInstPolyList[i];
						if (poly.getLayer() != arcPoly.getLayer()) continue;
						int netID = -1, netIDglobal = -1;
						if (poly.getPort() != null)
						{
                            netIDglobal = gatherNetworks.getGlobalNetworkID(info.getContext(), ni.findPortInstFromProto(poly.getPort()));
							Network net = nl.getNetwork(ni, poly.getPort(), 0);
							if (net != null) netID = info.getNetID(net);
						}
						if (netID == arcNetID) continue;
						poly.transform(nodeTrans);
						double dist = poly.separation(arcPoly);
						if (dist >= DBMath.getEpsilon()) continue;
						SubPolygon sp = new SubPolygon(poly, info.getContext(), netIDglobal, ni, null);
						if (bestSubPolygon != null)
						{
							if (!ni.hasExports()) continue;
						}
						bestSubPolygon = sp;
					}
				}
			}
		}

		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
		{
			NodeInst ni = no.getNodeInst();
			if (info.isRootCell())
			{
				// ignore any subcells that aren't the one being examined
				if (ni != cellOfInterest) return false;

				// cache the network ID of the arc
				if (arcNetID < 0)
				{
					Netlist nl = info.getNetlist();
					Network net = nl.getNetwork(arcOfInterest, 0);
					if (net != null) arcNetID = info.getNetID(net);
				}
				return true;
			}

			// only examine subcells if they intersect the arc
			if (!ni.isCellInstance()) return false;
			Rectangle2D b = ni.getBounds();
			Rectangle2D bounds = new Rectangle2D.Double(b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight());
			AffineTransform trans = info.getTransformToRoot();
			DBMath.transformRect(bounds, trans);
			if (DBMath.rectsIntersect(bounds, arcBounds)) return true;
			return false;
		}
	}

	/**
	 * Method to create a stack of exports to reach a NodeInst down the hierarchy.
	 * @param ni the NodeInst at the bottom of the hierarchy being exported.
	 * @param exportThis the port on the NodeInst being exported.
	 * @param where the hierarchical stack that defines the path to the top.
	 * @return the coordinate at the top-level where the drill happened.
	 */
	private Point2D makeExportDrill(NodeInst ni, PortProto exportThis, VarContext where, PolyBase preferredExportArea, ArcProto ap)
	{
		Point2D topCoord = new Point2D.Double(0, 0);
		while (where != VarContext.globalContext)
		{
			PortInst pi = ni.findPortInstFromProto(exportThis);
			if (pi == null) break;
			Iterator<Export> eIt = pi.getExports();
            boolean existingExportFound = false;
			if (eIt.hasNext())
			{
				exportThis = eIt.next();
			} else
			{
                Rectangle2D bounds = ni.getBounds();
                Cell cell = ni.getParent();

                String exportName = getExportNameInCell(cell, pi);
                // special case for primitive node (bottom most port) - export a pin at a different loc to make it
                // easier to connect to at the top level
                if (!ni.isCellInstance() && preferredExportArea != null && ap != null) // primitive node
                {
                    PrimitiveNode pn = (PrimitiveNode)ni.getProto();
                    // unfortunately preferredExportArea is relative to the top level, but we need it relative to the current level
                    for (Iterator<Nodable> noit = where.getPathIterator(); noit.hasNext(); )
                    {
                        Nodable no = noit.next();
                        if (!(no instanceof NodeInst)) break;
                        NodeInst niHier = (NodeInst)no;
                        AffineTransform trans = niHier.transformIn();
                        preferredExportArea.transform(trans);
                    }

                    // check if there is already an export on this network in the preferred area,
                    // and that it can connect to pi
                    Netlist netlist = cell.getNetlist();
                    Network net = netlist.getNetwork(pi);
                    for (Iterator<Export> eit = cell.getExports(); eit.hasNext(); )
                    {
                        Export ex = eit.next();
                        PortInst expi = ex.getOriginalPort();
                        if (preferredExportArea.contains(expi.getCenter()))
                        {
                            if (net != netlist.getNetwork(expi)) continue;
                            if (ex.connectsTo(ap)) {
                                existingExportFound = true;
                                exportThis = ex;
                                break;
                            }
                        }
                    }

                    if (!existingExportFound)
                    {
                        Point2D center = preferredExportArea.getCenter();
                        if (alignment != null) {
                            double x = center.getX();
                            double y = center.getY();
                            if (alignment.getWidth() > 0) x = Math.round(x/alignment.getWidth()) * alignment.getWidth();
                            if (alignment.getHeight() > 0) y = Math.round(y/alignment.getHeight()) * alignment.getHeight();
                            center = new Point2D.Double(x, y);
                        }

                        // if the new export center is still within bounds
                        if (DBMath.pointInRect(center, bounds))
                        {
                            // make the new export if it is a primitive node (avoid huge export area), or if the export would
                            // not be within the preferred area
                            if (!preferredExportArea.contains(pi.getCenter()) || pn.getFunction() == PrimitiveNode.Function.NODE)
                            {
                                // make export on pin and wire to pi, rather than exporting pi
                                PrimitiveNode pin = ap.findPinProto();
                                NodeInst pinNi = NodeInst.newInstance(pin, center, pin.getDefWidth(), pin.getDefHeight(), cell);
                                Route route = router.planRoute(cell, pinNi.getOnlyPortInst(), pi, center, null, false, false, null, null);
                                if (!Router.createRouteNoJob(route, cell, new HashMap<ArcProto,Integer>(), new HashMap<NodeProto,Integer>())) {
                                    pi = pinNi.getOnlyPortInst();
                                } else {
                                    if (pinNi != null) pinNi.kill(); // delete if route failed
                                }
                            }
                        }
                    }
                }
                if (!existingExportFound)
                    exportThis = Export.newInstance(cell, pi, exportName);
            }
			ni = where.getNodable().getNodeInst();
			where = where.pop();
			AffineTransform trans = ni.transformOut();
			trans.transform(pi.getPoly().getCenter(), topCoord);
		}
		return topCoord;
	}

	/**
	 * Method to create a stack of exports to reach an ArcInst down the hierarchy.
	 * @param topLoc the coordinate at the top level where the ArcInst should be broken.
	 * @param sp the context to the ArcInst.
	 */
	private void makeExportDrillOnArc(Point2D topLoc, SubPolygon sp, PolyBase preferredExportArea)
	{
		// save information about the arc
		ArcInst lowAI = (ArcInst)sp.theObj;
		if (!lowAI.isLinked()) return;
		String arcName = lowAI.getName();
		if (lowAI.getNameKey().isTempname()) arcName = null;
		int angle = lowAI.getAngle();
		ArcProto ap = lowAI.getProto();
		double width = lowAI.getLambdaBaseWidth();
		Cell cell = lowAI.getParent();

        for (Iterator<Nodable> noit = sp.context.getPathIterator(); noit.hasNext(); )
        {
            NodeInst niHier = noit.next().getNodeInst();
            AffineTransform trans = niHier.transformIn();
            trans.transform(topLoc, topLoc);
            if (preferredExportArea != null)
                preferredExportArea.transform(trans);            
        }

        // check if there is already an export on this network in the preferred area,
        // and that it can connect to ai
        Netlist netlist = cell.getNetlist();
        Network net = netlist.getNetwork(lowAI, 0);
        for (Iterator<Export> eit = cell.getExports(); eit.hasNext(); )
        {
            Export ex = eit.next();
            PortInst expi = ex.getOriginalPort();
            if (preferredExportArea.contains(expi.getCenter()))
            {
                if (net != netlist.getNetwork(expi)) continue;
                if (ex.connectsTo(ap)) {
                    // re-export up the hierarchy
                    makeExportDrill(ex.getOriginalPort().getNodeInst(), ex, sp.context.pop(), preferredExportArea, ap);
                    return;
                }
            }
        }

//      // see if there is already a pin/export here
//		NodeInst ni = null;
//		Rectangle2D searchBound = new Rectangle2D.Double(topLoc.getX(), topLoc.getY(), 0, 0);
//		for(Iterator<RTBounds> it = cell.searchIterator(searchBound); it.hasNext(); )
//		{
//			Geometric geom = (Geometric)it.next();
//			if (geom instanceof ArcInst) continue;
//			NodeInst foundNI = (NodeInst)geom;
//			if (foundNI.getAnchorCenterX() != topLoc.getX() || foundNI.getAnchorCenterY() != topLoc.getY()) continue;
//			if (foundNI.getFunction() != PrimitiveNode.Function.PIN) continue;
//			ni = foundNI;
//			break;
//		}

		// make a pin at the desired location on the arc
        NodeInst ni = null;

        // make sure the location is on the arc
        if (GenMath.distToLine(lowAI.getHeadLocation(), lowAI.getTailLocation(), topLoc) > 0)
            return;
        PrimitiveNode pNp = lowAI.getProto().findPinProto();
        ni = NodeInst.makeInstance(pNp, topLoc, pNp.getDefWidth(), pNp.getDefHeight(), cell);

        // insert the pin into the arc
        ArcInst newAi1 = ArcInst.makeInstanceBase(ap, width, lowAI.getHeadPortInst(), ni.getOnlyPortInst(), lowAI.getHeadLocation(), topLoc, null);
        ArcInst newAi2 = ArcInst.makeInstanceBase(ap, width, ni.getOnlyPortInst(), lowAI.getTailPortInst(), topLoc, lowAI.getTailLocation(), null);
        newAi1.setHeadNegated(lowAI.isHeadNegated());
        newAi1.setHeadExtended(lowAI.isHeadExtended());
        newAi1.setHeadArrowed(lowAI.isHeadArrowed());
        newAi2.setTailNegated(lowAI.isTailNegated());
        newAi2.setTailExtended(lowAI.isTailExtended());
        newAi2.setTailArrowed(lowAI.isTailArrowed());
        lowAI.kill();
        if (arcName != null)
        {
            if (lowAI.getHeadLocation().distance(topLoc) > lowAI.getTailLocation().distance(topLoc))
            {
                newAi1.setName(arcName);
                newAi1.copyTextDescriptorFrom(lowAI, ArcInst.ARC_NAME);
            } else
            {
                newAi2.setName(arcName);
                newAi2.copyTextDescriptorFrom(lowAI, ArcInst.ARC_NAME);
            }
        }
        newAi1.setAngle(angle);
        newAi2.setAngle(angle);

        makeExportDrill(ni, ni.getOnlyPortInst().getPortProto(), sp.context, preferredExportArea, ap);
	}

	private String getExportNameInCell(Cell cell, PortInst pi)
	{
		Netlist nl = cell.getNetlist();
		Network net = nl.getNetwork(pi);
		String exportName = null;
		for(Iterator<String> nIt = net.getExportedNames(); nIt.hasNext(); )
		{
			String eName = nIt.next();
			if (eName.startsWith("E") && eName.length() > 1 && TextUtils.isDigit(eName.charAt(1)))
				continue;
			if (exportName == null || exportName.length() < eName.length()) exportName = eName;
		}
		if (exportName == null) exportName = "E1";
		exportName = ElectricObject.uniqueObjectName(exportName, cell, PortProto.class, false, true);
		return exportName;
	}

	/**
	 * Class to define a polygon that is down in the hierarchy and has a context.
	 */
	private static class SubPolygon implements RTBounds
	{
		PolyBase poly;
		int netID;
		VarContext context;
		Geometric theObj;
		AffineTransform xfToTop;

		SubPolygon(PolyBase poly, VarContext context, int netID, Geometric theObj, AffineTransform xfToTop)
		{
			this.poly = poly;
			this.context = context;
			this.netID = netID;
			this.theObj = theObj;
			this.xfToTop = xfToTop;
		}

		public Rectangle2D getBounds() { return poly.getBounds2D(); }
	}

	/**
	 * Class to define two polygons that will connect down in the hierarchy.
	 */
	private static class PolyConnection
	{
		SubPolygon sp1, sp2;
		double distance;

		PolyConnection(SubPolygon sp1, SubPolygon sp2)
		{
			this.sp1 = sp1;
			this.sp2 = sp2;
			distance = sp1.poly.getCenter().distance(sp2.poly.getCenter());
		}
	}

	/**
	 * Method to compare two node instances to see if anything inside of them needs to connect.
	 * May have to create exports to make the connection.
	 * @param ni1 the first cell instance being checked.
	 * @param ni2 the second cell instance being checked.
	 */
	private void compareTwoNodesMakeExport(NodeInst ni1, NodeInst ni2, Map<Long,List<PolyConnection>> overlapMap, GatherNetworksVisitor gatherNetworks)
	{
		// force the second to be a cell instance
		if (!ni2.isCellInstance())
		{
			NodeInst swap = ni1;   ni1 = ni2;   ni2 = swap;
		}
		if (!ni2.isCellInstance()) return;

		// ignore stuff from different technologies
        if (ni1.getProto().getTechnology() != ni2.getProto().getTechnology()) return;

		// first find the area of intersection between the two nodes
		Rectangle2D bound1 = ni1.getBounds();
		Rectangle2D bound2 = ni2.getBounds();
		bound1 = new Rectangle2D.Double(bound1.getMinX()-DBMath.getEpsilon(), bound1.getMinY()-DBMath.getEpsilon(),
			bound1.getWidth()+DBMath.getEpsilon()*2, bound1.getHeight()+DBMath.getEpsilon()*2);
		bound2 = new Rectangle2D.Double(bound2.getMinX()-DBMath.getEpsilon(), bound2.getMinY()-DBMath.getEpsilon(),
			bound2.getWidth()+DBMath.getEpsilon()*2, bound2.getHeight()+DBMath.getEpsilon()*2);
		if (!DBMath.rectsIntersect(bound1, bound2)) return;
		Rectangle2D intersectArea = bound1.createIntersection(bound2);

		// now find all polygons in Node 1 that are in the intersection area
		RTNode rtree = null;
		if (ni1.isCellInstance())
		{
			GatherPolygonVisitor gpv = new GatherPolygonVisitor(intersectArea, ni1, gatherNetworks);
			HierarchyEnumerator.enumerateCell(ni1.getParent(), VarContext.globalContext, gpv);
			rtree = gpv.getRTree();
		} else
		{
			rtree = RTNode.makeTopLevel();
			Technology tech = ni1.getProto().getTechnology();
			Poly[] polys = tech.getShapeOfNode(ni1, true, true, null);
			AffineTransform trans = ni1.rotateOut();
			for(int i=0; i<polys.length; i++)
			{
				Poly poly = polys[i];
				poly.transform(trans);
                int netID = -1;
                if (poly.getPort() != null) {
                    netID = gatherNetworks.getGlobalNetworkID(VarContext.globalContext, ni1.findPortInstFromProto(poly.getPort()));
                }
                if (!DBMath.rectsIntersect(poly.getBounds2D(), intersectArea)) continue;
				rtree = RTNode.linkGeom(null, rtree, new SubPolygon(poly, VarContext.globalContext, netID, ni1, null));
			}
		}

		// now take the list into the second node and look for connections
		CheckPolygonVisitor cpv = new CheckPolygonVisitor(rtree, intersectArea, ni2, gatherNetworks);
		HierarchyEnumerator.enumerateCell(ni2.getParent(), VarContext.globalContext, cpv);
		List<PolyConnection> polysFound = cpv.getFoundConnections();

        // add to map of things that overlap should be connected
        for (PolyConnection p : polysFound) {
            registerPoly(overlapMap, p);
        }

//      // show what is matched
//		for(PolyConnection p : polysFound)
//		{
//			if (p.sp1.theObj instanceof ArcInst && p.sp2.theObj instanceof NodeInst)
//			{
//				if (p.sp1.context != VarContext.globalContext)
//				{
//					Point2D topLoc = makeExportDrill((NodeInst)p.sp2.theObj, p.sp2.poly.getPort(), p.sp2.context);
//					makeExportDrillOnArc(topLoc, p.sp1);
//				}
//				continue;
//			}
//			if (p.sp1.theObj instanceof NodeInst && p.sp2.theObj instanceof ArcInst)
//			{
//				if (p.sp2.context != VarContext.globalContext)
//				{
//					Point2D topLoc = makeExportDrill((NodeInst)p.sp1.theObj, p.sp1.poly.getPort(), p.sp1.context);
//					makeExportDrillOnArc(topLoc, p.sp2);
//				}
//				continue;
//			}
//			if (p.sp1.theObj instanceof NodeInst && p.sp2.theObj instanceof NodeInst)
//			{
//				makeExportDrill((NodeInst)p.sp1.theObj, p.sp1.poly.getPort(), p.sp1.context);
//				makeExportDrill((NodeInst)p.sp2.theObj, p.sp2.poly.getPort(), p.sp2.context);
//			}
//		}
	}

	/**
	 * HierarchyEnumerator subclass to check all geometry in a cell aganst polygons that may intersect.
	 */
	private class CheckPolygonVisitor extends HierarchyEnumerator.Visitor
	{
		private RTNode rtree;
		private Rectangle2D intersectArea;
		private NodeInst cellOfInterest;
		private List<PolyConnection> connectionsFound;
        private GatherNetworksVisitor gatherNetworks;

        public CheckPolygonVisitor(RTNode rtree, Rectangle2D intersectArea, NodeInst cellOfInterest, GatherNetworksVisitor gatherNetworks)
		{
			this.rtree = rtree;
			this.intersectArea = intersectArea;
			this.cellOfInterest = cellOfInterest;
            this.gatherNetworks = gatherNetworks;
            connectionsFound = new ArrayList<PolyConnection>();
		}

		public List<PolyConnection> getFoundConnections() { return connectionsFound; }

		public boolean enterCell(HierarchyEnumerator.CellInfo info) { return true; }

		public void exitCell(HierarchyEnumerator.CellInfo info)
		{
			AffineTransform toTop = info.getTransformToRoot();

			// check all nodes against the list
			for(Iterator<NodeInst> it = info.getCell().getNodes(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				if (ni.isCellInstance()) continue;
				AffineTransform nodeTrans = ni.rotateOut(toTop);
				Technology tech = ni.getProto().getTechnology();
				Poly[] polys = tech.getShapeOfNode(ni, true, true, null);
				for(int i=0; i<polys.length; i++)
				{
					Poly poly = polys[i];
					if (poly.getPort() == null) continue;
					poly.transform(nodeTrans);
					if (!DBMath.rectsIntersect(poly.getBounds2D(), intersectArea)) continue;
					for(RTNode.Search sea = new RTNode.Search(poly.getBounds2D(), rtree, true); sea.hasNext(); )
					{
						SubPolygon sp = (SubPolygon)sea.next();
						if (sp.poly.getLayer() != poly.getLayer()) continue;
						if (sp.poly.separation(poly) >= DBMath.getEpsilon()) continue;
						int netID = gatherNetworks.getGlobalNetworkID(info.getContext(), ni.findPortInstFromProto(poly.getPort()));
						SubPolygon sp2 = new SubPolygon(poly, info.getContext(), netID, ni, null);
						addConnection(sp, sp2);
					}
				}
			}

			// check all arcs against the list
			for(Iterator<ArcInst> it = info.getCell().getArcs(); it.hasNext(); )
			{
				ArcInst ai = it.next();
				Technology tech = ai.getProto().getTechnology();
				Poly [] arcPolyList = tech.getShapeOfArc(ai);
				for(int i=0; i<arcPolyList.length; i++)
				{
					PolyBase poly = arcPolyList[i];
					poly.transform(toTop);
					if (!DBMath.rectsIntersect(poly.getBounds2D(), intersectArea)) continue;
					for(RTNode.Search sea = new RTNode.Search(poly.getBounds2D(), rtree, true); sea.hasNext(); )
					{
						SubPolygon sp = (SubPolygon)sea.next();
						if (sp.poly.getLayer() != poly.getLayer()) continue;
						if (sp.poly.separation(poly) > 0) continue;
						int netID = gatherNetworks.getGlobalNetworkID(info.getContext(), ai.getHeadPortInst());
						SubPolygon sp2 = new SubPolygon(poly, info.getContext(), netID, ai, null);
						addConnection(sp, sp2);
					}
				}
			}
		}

		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
		{
			// only examine subcells if they intersect the area of interest
			NodeInst ni = no.getNodeInst();
			if (info.isRootCell())
			{
				// ignore any subcells that aren't the one being examined
				if (ni != cellOfInterest) return false;
			}
			if (!ni.isCellInstance()) return false;
			Rectangle2D b = ni.getBounds();
			Rectangle2D bounds = new Rectangle2D.Double(b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight());
			AffineTransform trans = info.getTransformToRoot();
			DBMath.transformRect(bounds, trans);
			if (DBMath.rectsIntersect(bounds, intersectArea)) return true;
			return false;
		}

		private void addConnection(SubPolygon sp1, SubPolygon sp2)
		{
			double distance = sp1.poly.getCenter().distance(sp2.poly.getCenter());
			boolean found = false;
			for(PolyConnection pc : connectionsFound)
			{
				if (pc.sp1.netID == sp1.netID && pc.sp2.netID == sp2.netID)
				{
					// found this connection: see if it is closer
					boolean replace = distance < pc.distance;
					int oldNodeCount = (pc.sp1.theObj instanceof NodeInst?1:0) + (pc.sp2.theObj instanceof NodeInst?1:0);
					int newNodeCount = (sp1.theObj instanceof NodeInst?1:0) + (sp2.theObj instanceof NodeInst?1:0);
					if (newNodeCount > oldNodeCount) replace = true; else
						if (newNodeCount < oldNodeCount) replace = false;
					if (replace)
					{
						pc.sp1 = sp1;
						pc.sp2 = sp2;
						pc.distance = distance;
					}
					found = true;
					break;
				}
			}
			if (!found)
			{
				connectionsFound.add(new PolyConnection(sp1, sp2));
			}
		}
	}

	/**
	 * HierarchyEnumerator subclass to find all geometry in a cell that touches a desired area at the upper level.
	 */
	private class GatherPolygonVisitor extends HierarchyEnumerator.Visitor
	{
		private RTNode rtree;
		private Rectangle2D intersectArea;
		private NodeInst cellOfInterest;
        private GatherNetworksVisitor gatherNetworks;

        public GatherPolygonVisitor(Rectangle2D intersectArea, NodeInst cellOfInterest, GatherNetworksVisitor gatherNetworks)
		{
			rtree = RTNode.makeTopLevel();
			this.intersectArea = intersectArea;
			this.cellOfInterest = cellOfInterest;
            this.gatherNetworks = gatherNetworks;
        }

		public RTNode getRTree() { return rtree; }

		public boolean enterCell(HierarchyEnumerator.CellInfo info) { return true; }

		public void exitCell(HierarchyEnumerator.CellInfo info)
		{
			AffineTransform toTop = info.getTransformToRoot();

			// add all nodes to the list
			for(Iterator<NodeInst> it = info.getCell().getNodes(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				if (ni.isCellInstance()) continue;
				AffineTransform nodeTrans = ni.rotateOut(toTop);
				Technology tech = ni.getProto().getTechnology();
				Poly[] polys = tech.getShapeOfNode(ni, true, true, null);
				for(int i=0; i<polys.length; i++)
				{
					Poly poly = polys[i];
					Layer.Function nodeLayerFun = poly.getLayer().getFunction();
					if (!nodeLayerFun.isMetal() && !nodeLayerFun.isDiff() && !nodeLayerFun.isPoly()) continue;
					if (poly.getPort() == null) continue;
					poly.transform(nodeTrans);
					if (!DBMath.rectsIntersect(poly.getBounds2D(), intersectArea)) continue;
					int netID = gatherNetworks.getGlobalNetworkID(info.getContext(), ni.findPortInstFromProto(poly.getPort()));
					rtree = RTNode.linkGeom(null, rtree, new SubPolygon(poly, info.getContext(), netID, ni, null));
				}
			}

			// add all arcs to the list
			for(Iterator<ArcInst> it = info.getCell().getArcs(); it.hasNext(); )
			{
				ArcInst ai = it.next();
				Technology tech = ai.getProto().getTechnology();
				Poly [] arcPolyList = tech.getShapeOfArc(ai);
				for(int i=0; i<arcPolyList.length; i++)
				{
					PolyBase poly = arcPolyList[i];
					Layer.Function arcLayerFun = poly.getLayer().getFunction();
					if (!arcLayerFun.isMetal() && !arcLayerFun.isDiff() && !arcLayerFun.isPoly()) continue;
					poly.transform(toTop);
					if (!DBMath.rectsIntersect(poly.getBounds2D(), intersectArea)) continue;
					int netID = gatherNetworks.getGlobalNetworkID(info.getContext(), ai.getHeadPortInst());
					rtree = RTNode.linkGeom(null, rtree, new SubPolygon(poly, info.getContext(), netID, ai, null));
				}
			}
		}

		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info)
		{
			// only examine subcells if they intersect the area of interest
			NodeInst ni = no.getNodeInst();
			if (info.isRootCell())
			{
				// ignore any subcells that aren't the one being examined
				if (ni != cellOfInterest) return false;
			}
			if (!ni.isCellInstance()) return false;
			Rectangle2D b = ni.getBounds();
			Rectangle2D bounds = new Rectangle2D.Double(b.getMinX(), b.getMinY(), b.getWidth(), b.getHeight());
			AffineTransform trans = info.getTransformToRoot();
			DBMath.transformRect(bounds, trans);
			if (DBMath.rectsIntersect(bounds, intersectArea)) return true;
			return false;
		}
	}

    private void registerPoly(Map<Long,List<PolyConnection>> overlapMap, PolyConnection p)
    {
        // make sure lower netID is in sp1
        if (p.sp1.netID > p.sp2.netID) {
            SubPolygon spTemp = p.sp1;
            p.sp1 = p.sp2;
            p.sp2 = spTemp;
        }

        long netID1 = p.sp1.netID;
        long netID2 = p.sp2.netID;
        if (netID1 == netID2) return; // already connected
        if (netID1 < 0 || netID2 < 0) {
            System.out.println("Ignoring poly "+p.sp1.theObj.describe(false)+", netID is "+netID1);
            System.out.println("Ignoring poly "+p.sp2.theObj.describe(false)+", netID is "+netID2);
            return;
        }

        // get unique key
        Long key = new Long((netID1 << Integer.SIZE) | (netID2));
        List<PolyConnection> polys = overlapMap.get(key);
        if (polys == null) {
            polys = new ArrayList<PolyConnection>();
            overlapMap.put(key,polys);
        }
        polys.add(p);
    }

    /**
     * HierarchyEnumerator subclass to gather a global consistent network map
     */
    private class GatherNetworksVisitor extends HierarchyEnumerator.Visitor
    {
        // String key is varContext.getInstPath(".")
        private Map<String,Map<PortInst,Integer>> networkToNetID = new HashMap<String,Map<PortInst,Integer>>();

        public boolean enterCell(HierarchyEnumerator.CellInfo info) {
            return true;
        }

        public void exitCell(HierarchyEnumerator.CellInfo info) {
            Cell cell = info.getCell();
            VarContext context = info.getContext();
            String key = context.getInstPath(".");
            Map<PortInst,Integer> netIdMap = networkToNetID.get(key);
            if (netIdMap == null) {
                netIdMap = new HashMap<PortInst,Integer>();
                networkToNetID.put(key, netIdMap);
            }

            Netlist netlist = info.getNetlist();
            for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); ) {
                NodeInst ni = it.next();
                for (Iterator<PortInst> pit = ni.getPortInsts(); pit.hasNext(); ) {
                    PortInst pi = pit.next();
                    Network net = netlist.getNetwork(pi);
                    if (net == null) continue;
                    int netID = info.getNetID(net);
                    netIdMap.put(pi, new Integer(netID));
                }
            }
        }

        public boolean visitNodeInst(Nodable ni, HierarchyEnumerator.CellInfo info) {
            return true;
        }

        private int getGlobalNetworkID(VarContext context, PortInst pi) {
            if (pi == null) return -1;
            if (context == null) return -1;
            String key = context.getInstPath(".");
            Map<PortInst,Integer> netIdMap = networkToNetID.get(key);
            if (netIdMap == null) return -1;
            if (!netIdMap.containsKey(pi)) return -1;
            return netIdMap.get(pi).intValue();
        }
    }

    /****************************************** SUPPORT ******************************************/

	/**
	 * Method to determine if an arc is too wide for its ends.
	 * Arcs that are wider than their nodes stick out from those nodes,
	 * and their geometry must be considered, even though the nodes have been checked.
	 * @param ai the ArcInst to check.
	 * @return true if the arc is wider than its end nodes
	 */
	private boolean arcTooWide(ArcInst ai)
	{
		boolean headTooWide = true;
		NodeInst hNi = ai.getHeadPortInst().getNodeInst();
		if (hNi.isCellInstance()) headTooWide = false; else
			if (ai.getLambdaBaseWidth() <= hNi.getLambdaBaseXSize() &&
				ai.getLambdaBaseWidth() <= hNi.getLambdaBaseYSize()) headTooWide = false;

		boolean tailTooWide = true;
		NodeInst tNi = ai.getTailPortInst().getNodeInst();
		if (tNi.isCellInstance()) tailTooWide = false; else
			if (ai.getLambdaBaseWidth() <= tNi.getLambdaBaseXSize() &&
				ai.getLambdaBaseWidth() <= tNi.getLambdaBaseYSize()) tailTooWide = false;

		return headTooWide || tailTooWide;
	}

	/**
	 * Method to get the shape of a node as a list of Polys.
	 * The autorouter uses this instead of Technology.getShapeOfNode()
	 * because this gets electrical layers and makes invisible pins be visible
	 * if they have coverage from connecting arcs.
	 * @param ni the node to inspect.  It must be primitive.
	 * @return an array of Poly objects that describe the node.
	 */
	private Poly [] shapeOfNode(NodeInst ni)
	{
		// compute the list of polygons
		Technology tech = ni.getProto().getTechnology();
		if (tech.isSchematics()) return new Poly[0];
		Poly [] nodePolys = tech.getShapeOfNode(ni, true, true, null);
		if (nodePolys.length == 0) return nodePolys;

		// if this is a pin, check the arcs that cover it
		if (ni.getFunction().isPin())
		{
			// pins must be covered by an arc that is extended and has enough width to cover the pin
			boolean gotOne = false;
			Rectangle2D coverage = null;
			for(Iterator<Connection> it = ni.getConnections(); it.hasNext(); )
			{
				Connection con = it.next();
				ArcInst ai = con.getArc();
				if (ai.getLambdaBaseWidth() >= ni.getLambdaBaseXSize() &&
					ai.getLambdaBaseWidth() >= ni.getLambdaBaseYSize() && ai.isHeadExtended() && ai.isTailExtended())
				{
					gotOne = true;
					break;
				}

				// figure out how much of the pin is covered by the arc
				Poly [] arcPolys = ai.getProto().getTechnology().getShapeOfArc(ai);
				if (arcPolys.length == 0) continue;
				Poly arcPoly = arcPolys[0];
				Rectangle2D arcBounds = arcPoly.getBounds2D();
				Rectangle2D arcBoundsLimited = new Rectangle2D.Double();
				Rectangle2D.intersect(nodePolys[0].getBounds2D(), arcBounds, arcBoundsLimited);
				if (coverage == null)
				{
					coverage = arcBoundsLimited;
				} else
				{
					// not known, intersection is a bit restrictive...
					Rectangle2D.union(coverage, arcBoundsLimited, coverage);
				}
			}
			if (!gotOne) // && !ni.hasExports())
			{
				if (coverage == null) return new Poly[0];

				Poly newPoly = new Poly(coverage);
				newPoly.setStyle(nodePolys[0].getStyle());
				newPoly.setLayer(nodePolys[0].getLayerOrPseudoLayer());
				newPoly.setPort(nodePolys[0].getPort());
				AffineTransform trans = ni.rotateIn();
				newPoly.transform(trans);
				nodePolys[0] = newPoly;
			}
		}
		return nodePolys;
	}

	/**
	 * Method to find and cache the smallest layer on an ArcProto.
	 * @param ap the ArcProto being examined.
	 * @param arcLayers a map from ArcProtos to their smallest Layers.
	 */
	private void findSmallestLayer(ArcProto ap, Map<ArcProto,Layer> arcLayers)
	{
		// quit if the value has already been computed
		if (arcLayers.get(ap) != null) return;

		// find the smallest layer
		Layer smallestLayer = ap.getLayer(0);
		int smallestGridExtend = ap.getLayerGridExtend(0);
        for (int arcLayer = 1; arcLayer < ap.getNumArcLayers(); arcLayer++) {
            int gridExtend = ap.getLayerGridExtend(arcLayer);
            if (gridExtend < smallestGridExtend) {
                smallestLayer = ap.getLayer(arcLayer);
                smallestGridExtend = gridExtend;
            }
        }
        arcLayers.put(ap, smallestLayer);
	}

	/**
	 * Class to sort Routes.
	 */
	private static class CompRoutes implements Comparator<Route>
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
			return 0;
		}
	}

	/**
	 * Class to handle complex topology in the cell.
	 * Accounts for existing as well as planned connections.
	 */
	private static class StitchingTopology
	{
		private Netlist netlist;
		private Map<Network,Network> connected;

		StitchingTopology(Cell cell)
		{
			netlist = cell.getNetlist();
			if (netlist == null)
			{
				System.out.println("Auto-router cannot get netlist information for cell " + cell.describe(false));
			}
			connected = new HashMap<Network,Network>();
		}

		/**
		 * Method to return the Network associated with a given node/port combination.
		 * @param ni the NodeInst in question.
		 * @param pp the PortProto on the NodeInst in question.
		 * @return the Network associated with that node/port.
		 */
		Network getNodeNetwork(NodeInst ni, PortProto pp)
		{
			Network net = netlist.getNetwork(ni, pp, 0);
			return getRealNet(net);
		}

		/**
		 * Method to return the Network associated with a given PortInst.
		 * @param pi the PortInst in question.
		 * @return the Network associated with that PortInst.
		 */
		Network getPortNetwork(PortInst pi)
		{
			Network net = netlist.getNetwork(pi);
			return getRealNet(net);
		}

		/**
		 * Method to return the Network associated with a given ArcInst.
		 * @param ai the ArcInst in question.
		 * @return the Network associated with that ArcInst.
		 */
		Network getArcNetwork(ArcInst ai)
		{
			Network net = netlist.getNetwork(ai, 0);
			return getRealNet(net);
		}

		/**
		 * Method to tell whether two ports on a node are connected.
		 * @param ni the NodeInst in question.
		 * @param pp1 the first PortProto on that NodeInst.
		 * @param pp2 the first PortProto on that NodeInst.
		 * @return true if the ports are connected.
		 */
		boolean portsConnected(NodeInst ni, PortProto pp1, PortProto pp2)
		{
			return netlist.portsConnected(ni, pp1, pp2);
		}

		/**
		 * Method to convert a Network to the actual one, once intended connections are made.
		 * @param net the original Network.
		 * @return the actual Network, for comparison purposes.
		 */
		private Network getRealNet(Network net)
		{
			for(;;)
			{
				Network nextNet = connected.get(net);
				if (nextNet == null) return net;
				net = nextNet;
			}
		}

		/**
		 * Method to plan for the connection of two Networks.
		 * @param net1 the first Network that will be connected.
		 * @param net2 the second Network that will be connected.
		 */
		void connect(Network net1, Network net2)
		{
			Network conNet1 = connected.get(net1);
			Network conNet2 = connected.get(net2);

			// if both nets are unknown, link one to the other
			if (conNet1 == null && conNet2 == null)
			{
				connected.put(net1, net2);
				return;
			}

			// if one net is unknown, link it to the known network
			if (conNet1 == null)
			{
				connected.put(net1, conNet2);
				return;
			}
			if (conNet2 == null)
			{
				connected.put(net2, conNet1);
				return;
			}

			// if both nets are known, link them
			connected.put(net2, conNet1);
		}
	}

	/**
	 * Class to package Preferences for the server.
	 */
	public static class AutoOptions implements Serializable
	{
		public boolean createExports;
		public ArcProto preferredArc;
        public boolean fatWires = true;

		public AutoOptions()
		{
			createExports = false;	
			preferredArc = Technology.getCurrent().getArcs().next();
		}

		public void initFromUserDefaults()
		{
			createExports = Routing.isAutoStitchCreateExports();
			preferredArc = Routing.getPreferredRoutingArcProto();
            fatWires = EditingPreferences.getThreadEditingPreferences().fatWires;
		}
	}

}
