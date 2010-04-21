/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SeaOfGatesEngine.java
 * Routing tool: Sea of Gates routing
 * Written by: Steven M. Rubin
 *
 * Copyright (c) 2007 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.routing.experimentalSeaOfGates;

import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.Environment;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.RTBounds;
import com.sun.electric.database.topology.RTNode;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.routing.RoutingFrame;
import com.sun.electric.tool.user.ErrorLogger;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Semaphore;

/**
 * Class to do sea-of-gates routing inside of a routing frame.
 * This router replaces unrouted arcs with real geometry.  It has these features:
 * > The router only works in layout, and only routes metal wires.
 * > The router uses vias to move up and down the metal layers.
 *   > Understands multiple vias and multiple via orientations.
 * > The router is not tracked: it runs on the Electric grid
 *   > Tries to cover multiple grid units in a single jump
 * > Routes power and ground first, then goes by length (shortest nets first)
 * > Prefers to run odd metal layers on horizontal, even layers on vertical
 * > Routes in both directions (from A to B and from B to A) and chooses the one that completes first
 *   > Serial method alternates advancing each wavefront, stopping when one completes
 *   > Parallel option runs both wavefronts at once and aborts the slower one
 * > Routes are made as wide as the widest arc already connected to any point
 *   > User preference can limit width
 * > Cost penalty also includes space left in the track on either side of a segment
 */
public class RoutingFrameSeaOfGates extends RoutingFrame
{
	/** True to display each step in the search. */								private static final boolean DEBUGSTEPS = false;
	/** True to debug "infinite" loops. */										private static final boolean DEBUGLOOPS = false;

	/** Percent of min cell size that route must stay inside. */				private static final double PERCENTLIMIT = 7;
	/** Number of steps per unit when searching. */								private static final double GRANULARITY = 1;
	/** Size of steps when searching. */										private static final double GRAINSIZE = (1/GRANULARITY);
	/** Cost of routing in wrong direction (alternating horizontal/vertical) */	private static final int COSTALTERNATINGMETAL = 100;
	/** Cost of changing layers. */												private static final int COSTLAYERCHANGE = 8;
	/** Cost of routing away from the target. */								private static final int COSTWRONGDIRECTION = 15;
	/** Cost of making a turn. */												private static final int COSTTURNING = 1;
	/** Cost of having coordinates that are off-grid. */						private static final int COSTOFFGRID = 15;

	private SearchVertex svAborted = new SearchVertex(0, 0, 0, 0, null, 0, null);
	private SearchVertex svExhausted = new SearchVertex(0, 0, 0, 0, null, 0, null);
	private SearchVertex svLimited = new SearchVertex(0, 0, 0, 0, null, 0, null);

	/** Cell in which routing occurs. */										private Cell cell;
	/** Cell size. */															private Rectangle2D cellBounds;
	/** R-Trees for metal blockage in the cell. */								private Map<RoutingLayer,RTNode> metalTrees;
	/** R-Trees for via blockage in the cell. */								private Map<RoutingLayer,RTNode> viaTrees;
	/** number of metal layers in the technology. */							private int numMetalLayers;
	/** metal layers in the technology. */										private RoutingLayer [] metalLayers;
	/** vias to use to go up from each metal layer. */							private MetalVias [] metalVias;
	/** true to run to/from and from/to routing in parallel */					private boolean parallelDij;
	/** for logging errors */													private ErrorLogger errorLogger;

	public RoutingParameter maxArcWidth = new RoutingParameter("maxarcwidth", "Maximum arc width:", 10);
	public RoutingParameter complexityLimit = new RoutingParameter("complexity", "Complexity limit:", 200000);
	public RoutingParameter useParallelFromToRoutes = new RoutingParameter("parallelFromToRoutes", "Use two processors per route", false);
	public RoutingParameter useParallelRoutes = new RoutingParameter("parallelRoutes", "Do multiple routes in parallel", false);

	/**
	 * Method to return the name of this routing algorithm.
	 * @return the name of this routing algorithm.
	 */
	public String getAlgorithmName() { return "Sea-of-Gates in Framework"; }

	/**
	 * Method to return a list of parameters for this routing algorithm.
	 * @return a list of parameters for this routing algorithm.
	 */
	public List<RoutingParameter> getParameters()
	{
		List<RoutingParameter> allParams = new ArrayList<RoutingParameter>();
		allParams.add(maxArcWidth);
		allParams.add(complexityLimit);
		allParams.add(useParallelFromToRoutes);
		allParams.add(useParallelRoutes);
		return allParams;
	}

	/**
	 * Method to do Sea of Gates Routing.
	 */
	protected void runRouting(Cell cell, List<RoutingSegment> segmentsToRoute, List<RoutingLayer> allLayers,
		List<RoutingContact> allContacts, List<RoutingGeometry> blockages)
	{
		this.cell = cell;
		routeIt(Job.getRunningJob(), segmentsToRoute, allLayers, allContacts, blockages);
	}

	/**
	 * Class to hold a "batch" of routes, all on the same network.
	 */
	private static class RouteBatches
	{
		int segsInBatch;
	}

	private class Wavefront
	{
		/** The route that this is part of. */								private NeededRoute nr;
		/** Wavefront name (for debugging). */								private String name;
		/** List of active search vertices while running wavefront. */		private TreeSet<SearchVertex> active;
		/** Resulting list of vertices found for this wavefront. */			private List<SearchVertex> vertices;
		/** Set true to abort this wavefront's search. */					private boolean abort;
		/** The starting and ending ports of the wavefront. */				private RoutingEnd from, to;
		/** The starting X/Y coordinates of the wavefront. */				private double fromX, fromY;
		/** The starting metal layer of the wavefront. */					private int fromZ;
		/** The ending X/Y coordinates of the wavefront. */					private double toX, toY;
		/** The ending metal layer of the wavefront. */						private int toZ;
		/** debugging state */												private final boolean debug;
		/** Gridless search vertices found while propagating wavefront. */	private Map<Double,Set<Double>> [] searchVertexPlanesDBL;
		/** minimum spacing between this metal and itself. */				private Map<Double,Map<Double,Double>>[] layerSurround;

		Wavefront(NeededRoute nr, RoutingEnd from, double fromX, double fromY, int fromZ,
			RoutingEnd to, double toX, double toY, int toZ, String name, boolean debug)
		{
			this.nr = nr;
			this.from = from;  this.fromX = fromX;  this.fromY = fromY;  this.fromZ = fromZ;
			this.to = to;      this.toX = toX;      this.toY = toY;      this.toZ = toZ;
			this.name = name;
			this.debug = debug;
			active = new TreeSet<SearchVertex>();
			vertices = null;
			abort = false;
			searchVertexPlanesDBL = new Map[numMetalLayers];
			layerSurround = new Map[numMetalLayers];
			for(int i=0; i<numMetalLayers; i++)
				layerSurround[i] = new HashMap<Double,Map<Double,Double>>();

			if (debug) System.out.println("----------- SEARCHING FROM ("+TextUtils.formatDouble(fromX)+","+
				TextUtils.formatDouble(fromY)+",M"+(fromZ+1)+") TO ("+TextUtils.formatDouble(toX)+","+
				TextUtils.formatDouble(toY)+",M"+(toZ+1)+") -----------");

			SearchVertex svStart = new SearchVertex(fromX, fromY, fromZ, 0, null, 0, this);
			svStart.cost = 0;
			setVertex(fromX, fromY, fromZ);
			active.add(svStart);
		}

		/**
		 * Method to get the SearchVertex at a given coordinate.
		 * @param x the X coordinate desired.
		 * @param y the Y coordinate desired.
		 * @param z the Z coordinate (metal layer) desired.
		 * @return the SearchVertex at that point (null if none).
		 */
		public boolean getVertex(double x, double y, int z)
		{
			Map<Double,Set<Double>> plane = searchVertexPlanesDBL[z];
			if (plane == null) return false;
			Set<Double> row = plane.get(new Double(y));
			if (row == null) return false;
			boolean found = row.contains(new Double(x));
			return found;
		}

		/**
		 * Method to mark a given coordinate.
		 * @param x the X coordinate desired.
		 * @param y the Y coordinate desired.
		 * @param z the Z coordinate (metal layer) desired.
		 */
		public void setVertex(double x, double y, int z)
		{
			Map<Double,Set<Double>> plane = searchVertexPlanesDBL[z];
			if (plane == null)
			{
				plane = new HashMap<Double,Set<Double>>();
				searchVertexPlanesDBL[z] = plane;
			}
			Double iY = new Double(y);
			Set<Double> row = plane.get(iY);
			if (row == null)
			{
				row = new HashSet<Double>();
				plane.put(iY, row);
			}
			row.add(new Double(x));
		}

		/**
		 * Method to determine the design rule spacing between two pieces of a given layer.
		 * @param layer the layer index.
		 * @param width the width of one of the pieces (-1 to use default).
		 * @param length the length of one of the pieces (-1 to use default).
		 * @return the design rule spacing (0 if none).
		 */
		public double getSpacingRule(int layer, double width, double length)
		{
			// use default width if none specified
			if (width < 0) width = metalLayers[layer].getMinWidth();
			if (length < 0) length = 50;

			// convert these to the next largest integers
			Double wid = new Double(upToGrain(width));
			Double len = new Double(upToGrain(length));

			// see if the rule is cached6
			Map<Double,Double> widMap = layerSurround[layer].get(wid);
			if (widMap == null)
			{
				widMap = new HashMap<Double,Double>();
				layerSurround[layer].put(wid, widMap);
			}
			Double value = widMap.get(len);
			if (value == null)
			{
				// rule not cached: compute it
				RoutingLayer lay = metalLayers[layer];
				double v = lay.getMinSpacing(lay);
				value = new Double(v);
				widMap.put(len, value);
			}
			return value.doubleValue();
		}
	}

	/**
	 * Class to hold a route that must be run.
	 */
	private class NeededRoute
	{
		String routeName;
		int netID;
		double minWidth;
		int batchNumber;
		int routeInBatch;
		Rectangle2D routeBounds;
		double minimumSearchBoundX;
		double maximumSearchBoundX;
		double minimumSearchBoundY;
		double maximumSearchBoundY;
		Rectangle2D jumpBound;
		Wavefront dir1, dir2;
		Wavefront winningWF;
		RoutingSegment rs;

		NeededRoute(String routeName, double fromX, double fromY, int fromZ, double toX, double toY, int toZ,
			int netID, double minWidth, int batchNumber, int routeInBatch, RoutingSegment rs)
		{
			this.routeName = routeName;
			this.netID = netID;
			this.minWidth = minWidth;
			this.batchNumber = batchNumber;
			this.routeInBatch = routeInBatch;
			this.rs = rs;

			cellBounds = cell.getBounds();
			minimumSearchBoundX = downToGrain(cellBounds.getMinX());
			maximumSearchBoundX = upToGrain(cellBounds.getMaxX());
			minimumSearchBoundY = downToGrain(cellBounds.getMinY());
			maximumSearchBoundY = upToGrain(cellBounds.getMaxY());
			if (PERCENTLIMIT > 0)
			{
				double maxStrayFromRouteBounds = Math.min(cellBounds.getWidth(), cellBounds.getHeight()) * PERCENTLIMIT / 100;
				routeBounds = new Rectangle2D.Double(
					Math.min(fromX, toX)-maxStrayFromRouteBounds, Math.min(fromY, toY)-maxStrayFromRouteBounds,
					Math.abs(fromX-toX)+maxStrayFromRouteBounds*2, Math.abs(fromY-toY)+maxStrayFromRouteBounds*2);
				minimumSearchBoundX = routeBounds.getMinX();
				maximumSearchBoundX = routeBounds.getMaxX();
				minimumSearchBoundY = routeBounds.getMinY();
				maximumSearchBoundY = routeBounds.getMaxY();
			}
			jumpBound = new Rectangle2D.Double(Math.min(fromX, toX), Math.min(fromY, toY),
				Math.abs(fromX-toX), Math.abs(fromY-toY));

			// make two wavefronts going in both directions
			dir1 = new Wavefront(this, rs.getStartEnd(), fromX, fromY, fromZ, rs.getFinishEnd(), toX, toY, toZ, "a->b", DEBUGSTEPS);
			dir2 = new Wavefront(this, rs.getFinishEnd(), toX, toY, toZ, rs.getStartEnd(), fromX, fromY, fromZ, "b->a", false);
		}

		public void cleanSearchMemory()
		{
			dir1.searchVertexPlanesDBL = null;
			dir1.active = null;
			if (dir1.vertices != null)
			{
				for(SearchVertex sv : dir1.vertices)
					sv.clearCuts();
			}

			dir2.searchVertexPlanesDBL = null;
			dir2.active = null;
			if (dir2.vertices != null)
			{
				for(SearchVertex sv : dir2.vertices)
					sv.clearCuts();
			}
		}
	}

	/************************************** ROUTING **************************************/

	/**
	 * This is the public interface for Sea-of-Gates Routing when done in batch mode.
	 * @param cell the cell to be Sea-of-Gates-routed.
	 * @param arcsToRoute a List of ArcInsts on networks to be routed.
	 */
	public void routeIt(Job job, List<RoutingSegment> segmentsToRoute, List<RoutingLayer> allLayers,
		List<RoutingContact> allContacts, List<RoutingGeometry> blockages)
	{
		// initialize information about the technology
		if (initializeDesignRules(allLayers, allContacts)) return;

		// user-interface initialization
		Job.getUserInterface().startProgressDialog("Routing " + segmentsToRoute.size() + " nets", null);
		Job.getUserInterface().setProgressNote("Building blockage information...");

		// create an error logger
		errorLogger = ErrorLogger.newInstance("Routing (Sea of gates)");

		// get all blockage information into R-Trees
		metalTrees = new HashMap<RoutingLayer, RTNode>();
		viaTrees = new HashMap<RoutingLayer, RTNode>();

		// add all blockage geometry to the list
		for(RoutingGeometry rg : blockages)
		{
			RoutingLayer layer = rg.getLayer();
			Rectangle2D bounds = rg.getBounds();
			if (layer.isMetal())
			{
				addRectangle(bounds, layer, rg.getNetID());
			} else
			{
				addVia(new Point2D.Double(bounds.getCenterX(), bounds.getCenterY()), layer, rg.getNetID());
			}
		}
		addBlockagesAtPorts(segmentsToRoute);

		// make a list of all routes that are needed
		List<NeededRoute> allRoutes = new ArrayList<NeededRoute>();
		int numBatches = segmentsToRoute.size();
		RouteBatches [] routeBatches = new RouteBatches[numBatches];
		for(int b=0; b<numBatches; b++)
		{
			// get list of PortInsts that comprise this net
			RoutingSegment rs = segmentsToRoute.get(b);
			routeBatches[b] = new RouteBatches();
			routeBatches[b].segsInBatch = 0;

			// determine the minimum width of arcs on this net
			double minWidth = Math.max(rs.getWidestArcAtStart(), rs.getWidestArcAtStart());

			// find a path between the ends of the network
			int batchNumber = 1;

			// get information about one end of the path
			List<RoutingLayer> fromArcs = rs.getStartLayers();
			RoutingLayer fromArc = fromArcs.get(0);

			// get information about the other end of the path
			List<RoutingLayer> toArcs = rs.getFinishLayers();
			RoutingLayer toArc = toArcs.get(0);

			// determine the coordinates of the route
			Point2D fromLoc = rs.getStartEnd().getLocation();
			Point2D toLoc = rs.getFinishEnd().getLocation();
			double fromX = fromLoc.getX(), fromY = fromLoc.getY();
			double toX = toLoc.getX(), toY = toLoc.getY();
			if (toLoc.getX() < fromLoc.getX())
			{
				toX = upToGrain(toLoc.getX());
				fromX = downToGrain(fromLoc.getX());
			} else if (toLoc.getX() > fromLoc.getX())
			{
				toX = downToGrain(toLoc.getX());
				fromX = upToGrain(fromLoc.getX());
			} else
			{
				toX = fromX = upToGrain(fromLoc.getX());
			}
			if (toLoc.getY() < fromLoc.getY())
			{
				toY = upToGrain(toLoc.getY());
				fromY = downToGrain(fromLoc.getY());
			} else if (toLoc.getY() > fromLoc.getY())
			{
				toY = downToGrain(toLoc.getY());
				fromY = upToGrain(fromLoc.getY());
			} else
			{
				toY = fromY = upToGrain(fromLoc.getY());
			}
			int fromZ = fromArc.getMetalNumber()-1;
			int toZ = toArc.getMetalNumber()-1;

			// see if access is blocked
			double metalSpacing = Math.max(metalLayers[fromZ].getMinWidth(), minWidth) / 2;

			// determine "from" surround
			RoutingLayer lay = metalLayers[fromZ];
			double surround = lay.getMinSpacing(lay);
			SOGBound block = getMetalBlockage(rs.getNetID(), fromZ, metalSpacing, metalSpacing, surround, fromX, fromY);
			if (block != null)
			{
				// see if gridding caused the blockage
				fromX = fromLoc.getX();
				fromY = fromLoc.getY();
				block = getMetalBlockage(rs.getNetID(), fromZ, metalSpacing, metalSpacing, surround, fromX, fromY);
				if (block != null)
				{
					String errorMsg = "Cannot Route to port " + rs.getStartEnd().describe() + " at (" + TextUtils.formatDistance(fromX) + "," +
						TextUtils.formatDistance(fromY) + ") because it is blocked on layer " + metalLayers[fromZ].getName() +
						" [needs " + TextUtils.formatDistance(metalSpacing+surround) + " all around, blockage is " +
						TextUtils.formatDistance(block.bound.getMinX()) + "<=X<=" + TextUtils.formatDistance(block.bound.getMaxX()) + " and " +
						TextUtils.formatDistance(block.bound.getMinY()) + "<=Y<=" + TextUtils.formatDistance(block.bound.getMaxY()) + "]";
					System.out.println(errorMsg);
					List<PolyBase> polyList = new ArrayList<PolyBase>();
					polyList.add(new PolyBase(fromX, fromY, (metalSpacing+surround)*2, (metalSpacing+surround)*2));
					polyList.add(new PolyBase(block.bound));
					List<EPoint> lineList = new ArrayList<EPoint>();
					lineList.add(new EPoint(block.bound.getMinX(), block.bound.getMinY()));
					lineList.add(new EPoint(block.bound.getMaxX(), block.bound.getMaxY()));
					lineList.add(new EPoint(block.bound.getMinX(), block.bound.getMaxY()));
					lineList.add(new EPoint(block.bound.getMaxX(), block.bound.getMinY()));
					errorLogger.logMessageWithLines(errorMsg, polyList, lineList, cell, 0, true);
					continue;
				}
			}
			metalSpacing = Math.max(metalLayers[toZ].getMinWidth(), minWidth) / 2;

			// determine "to" surround
			lay = metalLayers[toZ];
			surround = lay.getMinSpacing(lay);
			block = getMetalBlockage(rs.getNetID(), toZ, metalSpacing, metalSpacing, surround, toX, toY);
			if (block != null)
			{
				// see if gridding caused the blockage
				toX = toLoc.getX();
				toY = toLoc.getY();
				block = getMetalBlockage(rs.getNetID(), toZ, metalSpacing, metalSpacing, surround, toX, toY);
				if (block != null)
				{
					String errorMsg = "Cannot route to port " + rs.getFinishEnd().describe() + " at (" + TextUtils.formatDistance(toX) + "," +
						TextUtils.formatDistance(toY) + ") because it is blocked on layer " + metalLayers[toZ].getName() +
						" [needs " + TextUtils.formatDistance(metalSpacing+surround) + " all around, blockage is " +
						TextUtils.formatDistance(block.bound.getMinX()) + "<=X<=" + TextUtils.formatDistance(block.bound.getMaxX()) + " and " +
						TextUtils.formatDistance(block.bound.getMinY()) + "<=Y<=" + TextUtils.formatDistance(block.bound.getMaxY()) + "]";
					System.out.println("ERROR: " + errorMsg);
					List<PolyBase> polyList = new ArrayList<PolyBase>();
					polyList.add(new PolyBase(toX, toY, (metalSpacing+surround)*2, (metalSpacing+surround)*2));
					polyList.add(new PolyBase(block.bound));
					List<EPoint> lineList = new ArrayList<EPoint>();
					lineList.add(new EPoint(block.bound.getMinX(), block.bound.getMinY()));
					lineList.add(new EPoint(block.bound.getMaxX(), block.bound.getMaxY()));
					lineList.add(new EPoint(block.bound.getMinX(), block.bound.getMaxY()));
					lineList.add(new EPoint(block.bound.getMaxX(), block.bound.getMinY()));
					errorLogger.logMessageWithLines(errorMsg, polyList, lineList, cell, 0, true);
					continue;
				}
			}
			NeededRoute nr = new NeededRoute(rs.getNetName(), fromX, fromY, fromZ, toX, toY, toZ,
				rs.getNetID(), minWidth, b, batchNumber++, rs);
			routeBatches[b].segsInBatch++;
			allRoutes.add(nr);
		}

		// now do the actual routing
		boolean parallel = useParallelRoutes.getBooleanValue();
		parallelDij = useParallelFromToRoutes.getBooleanValue();

		int numberOfProcessors = Runtime.getRuntime().availableProcessors();
		if (numberOfProcessors <= 1) parallelDij = false;
		int numberOfThreads = numberOfProcessors;
		if (parallelDij) numberOfThreads /= 2;
		if (!parallel) numberOfThreads = 1;
		if (numberOfThreads == 1) parallel = false;

		// show what is being done
		System.out.println("Sea-of-gates router finding " + allRoutes.size() + " paths on " + numBatches + " networks");
		if (parallel || parallelDij)
		{
			String message = "NOTE: System has " + numberOfProcessors + " processors so";
			if (parallel) message += " routing " + numberOfThreads + " paths in parallel";
			if (parallelDij)
			{
				if (parallel) message += " and";
				message += " routing both directions of each path in parallel";
			}
			System.out.println(message);
		}
        Environment env = Environment.getThreadEnvironment();
        EditingPreferences ep = cell.getEditingPreferences();
		if (numberOfThreads > 1)
		{
			doRoutingParallel(numberOfThreads, allRoutes, routeBatches, env, ep);
		} else
		{
			doRouting(allRoutes, routeBatches, job, env, ep);
		}

		// clean up at end
		Job.getUserInterface().stopProgressDialog();
		errorLogger.termLogging(true);
	}

	/**
	 * Method to do the routing in a single thread.
	 * @param allRoutes the routes that need to be done.
	 * @param routeBatches the routing batches (by network)
	 * @param job the job that invoked this routing.
	 */
	private void doRouting(List<NeededRoute> allRoutes, RouteBatches [] routeBatches, Job job, Environment env, EditingPreferences ep)
	{
		int totalRoutes = allRoutes.size();
		for(int r=0; r<totalRoutes; r++)
		{
			if (job != null && job.checkAbort())
			{
				System.out.println("Sea-of-gates routing aborted");
				break;
			}

			// get information on the segment to be routed
			NeededRoute nr = allRoutes.get(r);
			Job.getUserInterface().setProgressValue(r*100/totalRoutes);
			String routeName = nr.routeName;
			if (routeBatches[nr.batchNumber].segsInBatch > 1)
				routeName += " (" + nr.routeInBatch + " of " + routeBatches[nr.batchNumber].segsInBatch + ")";
			Job.getUserInterface().setProgressNote("Network " + routeName);
			System.out.println("Routing network " + routeName + "...");

			// route the segment
			findPath(nr, env, ep);

			// if the routing was good, place the results
			if (nr.winningWF != null && nr.winningWF.vertices != null)
				createRoute(nr);
		}
	}

	private void doRoutingParallel(int numberOfThreads, List<NeededRoute> allRoutes, RouteBatches [] routeBatches, Environment env, EditingPreferences ep)
	{
		// create threads and other threading data structures
		RouteInThread[] threads = new RouteInThread[numberOfThreads];
		for(int i=0; i<numberOfThreads; i++) threads[i] = new RouteInThread("Route #" + (i+1), env, ep);
		NeededRoute [] routesToDo = new NeededRoute[numberOfThreads];
		int [] routeIndices = new int[numberOfThreads];
		Semaphore outSem = new Semaphore(0);

		// create list of routes and blocked areas
		List<NeededRoute> myList = new ArrayList<NeededRoute>();
		for(NeededRoute nr : allRoutes) myList.add(nr);
		List<Rectangle2D> blocked = new ArrayList<Rectangle2D>();

		// now run the threads
		int totalRoutes = allRoutes.size();
		int routesDone = 0;
		while (myList.size() > 0)
		{
			int threadAssign = 0;
			blocked.clear();
			for(int i=0; i<myList.size(); i++)
			{
				NeededRoute nr = myList.get(i);
				boolean isBlocked = false;
				for(Rectangle2D block : blocked)
				{
					if (block.intersects(nr.routeBounds)) { isBlocked = true;   break; }
				}
				if (isBlocked) continue;

				// this route can be done: start it
				blocked.add(nr.routeBounds);
				routesToDo[threadAssign] = nr;
				routeIndices[threadAssign] = i;
				threads[threadAssign].startRoute(nr, outSem);
				threadAssign++;
				if (threadAssign >= numberOfThreads) break;
			}

			String routes = "";
			for(int i=0; i<threadAssign; i++)
			{
				String routeName = routesToDo[i].routeName;
				if (routeBatches[routesToDo[i].batchNumber].segsInBatch > 1)
					routeName += "(" + routesToDo[i].routeInBatch + "/" + routeBatches[routesToDo[i].batchNumber].segsInBatch + ")";
				if (routes.length() > 0) routes += ", ";
				routes += routeName;
			}
			System.out.println("Parallel routing " + routes + "...");
			Job.getUserInterface().setProgressNote(routes);

			// now wait for routing threads to finish
			outSem.acquireUninterruptibly(threadAssign);

			// all done, now handle the results
			for(int i=0; i<threadAssign; i++)
			{
				if (routesToDo[i].winningWF != null && routesToDo[i].winningWF.vertices != null)
					createRoute(routesToDo[i]);
			}
			for(int i=threadAssign-1; i>=0; i--)
				myList.remove(routeIndices[i]);
			routesDone += threadAssign;
			Job.getUserInterface().setProgressValue(routesDone*100/totalRoutes);
		}

		// terminate the threads
		for(int i=0; i<numberOfThreads; i++) threads[i].startRoute(null, null);
	}

	private class RouteInThread extends Thread
	{
		private Semaphore inSem = new Semaphore(0);
		private NeededRoute nr;
		private Semaphore whenDone;
        private Environment env;
        private EditingPreferences ep;

		public RouteInThread(String name, Environment env, EditingPreferences ep)
		{
			super(name);
            this.env = env;
            this.ep = ep;
			start();
		}

		public void startRoute(NeededRoute nr, Semaphore whenDone)
		{
			this.nr = nr;
			this.whenDone = whenDone;
			inSem.release();
		}

		public void run()
		{
            Environment.setThreadEnvironment(env);
            EditingPreferences.setThreadEditingPreferences(ep);
			for (;;)
			{
				inSem.acquireUninterruptibly();
				if (nr == null) return;
				findPath(nr, env, ep);
				whenDone.release();
			}
		}
	}

	/**
	 * Method to initialize technology information, including design rules.
	 * @return true on error.
	 */
	private boolean initializeDesignRules(List<RoutingLayer> allLayers, List<RoutingContact> allContacts)
	{
		// find the metal layers, arcs, and contacts
		numMetalLayers = 0;
		for(RoutingLayer rl : allLayers) if (rl.isMetal()) numMetalLayers++;
		metalLayers = new RoutingLayer[numMetalLayers];
		int metNo = 0;
		for(RoutingLayer rl : allLayers)
		{
			if (rl.isMetal()) metalLayers[metNo++] = rl;
		}
		metalVias = new MetalVias[numMetalLayers-1];
		for(int i=0; i<numMetalLayers-1; i++) metalVias[i] = new MetalVias();
		for(RoutingContact rc : allContacts)
		{
			for(int i=0; i<numMetalLayers-1; i++)
			{
				if ((rc.getFirstLayer() == metalLayers[i] && rc.getSecondLayer() == metalLayers[i+1]) ||
					(rc.getSecondLayer() == metalLayers[i] && rc.getFirstLayer() == metalLayers[i+1]))
				{
					metalVias[i].addVia(rc, 0);

					// see if the node is asymmetric and should exist in rotated states
					boolean square = true, offCenter = false;
					List<RoutingGeometry> geoms = rc.getGeometry();
					for(RoutingGeometry rg : geoms)
					{
						RoutingLayer conLayer = rg.getLayer();
						if (conLayer.isMetal())
						{
							Rectangle2D conRect = rg.getBounds();
							if (conRect.getWidth() != conRect.getHeight()) square = false;
							if (conRect.getCenterX() != 0 || conRect.getCenterY() != 0) offCenter = true;
						}
					}
					if (offCenter)
					{
						// off center: test in all 4 rotations
						metalVias[i].addVia(rc, 90);
						metalVias[i].addVia(rc, 180);
						metalVias[i].addVia(rc, 270);
					} else if (!square)
					{
						// centered but not square: test in 90-degree rotation
						metalVias[i].addVia(rc, 90);
					}
					break;
				}
			}
		}
		for(int i=0; i<numMetalLayers; i++)
		{
			if (metalLayers[i] == null)
			{
				System.out.println("ERROR: Cannot find layer for Metal " + (i+1));
				return true;
			}
			if (i < numMetalLayers-1)
			{
				if (metalVias[i].getVias().size() == 0)
				{
					System.out.println("ERROR: Cannot find contact node between Metal " + (i+1) + " and Metal " + (i+2));
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Method to add extra blockage information that corresponds to ends of unrouted arcs.
	 * @param segmentsToRoute the list of segments to route.
	 * @param tech the technology to use.
	 */
	private void addBlockagesAtPorts(List<RoutingSegment> segmentsToRoute)
	{
		for(RoutingSegment rs : segmentsToRoute)
		{
			// determine the minimum width of arcs on this net
			double minWidth = Math.min(rs.getWidestArcAtStart(), rs.getWidestArcAtFinish());
			addBlockage(rs.getStartEnd().getLocation(), rs.getStartLayers(), minWidth, rs.getNetID());
			addBlockage(rs.getFinishEnd().getLocation(), rs.getFinishLayers(), minWidth, rs.getNetID());
		}
	}

	private void addBlockage(Point2D loc, List<RoutingLayer> layers, double minWidth, int netID)
	{
		int lowMetal = -1, highMetal = -1;
		for(RoutingLayer rl : layers)
		{
			int level = rl.getMetalNumber();
			if (lowMetal < 0) lowMetal = highMetal = level; else
			{
				lowMetal = Math.min(lowMetal, level);
				highMetal = Math.max(highMetal, level);
			}
		}
		if (lowMetal < 0) return;

		// reserve space on layers above and below
		for(int via = lowMetal-2; via < highMetal; via++)
		{
			if (via < 0 || via >= numMetalLayers-1) continue;
			MetalVia mv = metalVias[via].getVias().get(0);
			for(RoutingGeometry rg : mv.via.getGeometry())
			{
				Rectangle2D centeredBounds = rg.getBounds();
				Rectangle2D bounds = new Rectangle2D.Double(centeredBounds.getMinX() + loc.getX(),
					centeredBounds.getMinY() + loc.getY(), centeredBounds.getWidth(), centeredBounds.getHeight());
				addRectangle(bounds, rg.getLayer(), netID);
			}
		}
	}

	/**
	 * Method to create the geometry for a route.
	 * Places nodes and arcs to make the route, and also updates the R-Tree data structure.
	 * @param nr the route information.
	 */
	private void createRoute(NeededRoute nr)
	{
		Wavefront wf = nr.winningWF;
		Point2D pt = wf.to.getLocation();
		RoutingContact startContact = RoutingContact.STARTPOINT;
		RoutingContact finishContact = RoutingContact.FINISHPOINT;
		if (wf == nr.dir2)
		{
			startContact = RoutingContact.FINISHPOINT;
			finishContact = RoutingContact.STARTPOINT;
		}
		int endIndex = wf.vertices.size();
		RoutePoint lastRP = new RoutePoint(finishContact, pt, 0);
		if (!DBMath.doublesClose(pt.getX(), wf.toX) || !DBMath.doublesClose(pt.getY(), wf.toY))
		{
			// end of route is off-grid: adjust it
			if (endIndex >= 2)
			{
				SearchVertex v1 = wf.vertices.get(0);
				SearchVertex v2 = wf.vertices.get(1);
				RoutingLayer type = metalLayers[wf.toZ];
				double width = Math.max(type.getMinWidth(), nr.minWidth);
				RoutingContact pin = type.getPin();
				if (v1.getX() == v2.getX())
				{
					// first line is vertical: run a horizontal bit
					RoutePoint rp = makeNodeInst(pin, new Point2D.Double(v1.getX(), pt.getY()), 0, nr);
					makeArcInst(type, width, rp, lastRP, nr);
					lastRP = rp;
				} else if (v1.getY() == v2.getY())
				{
					// first line is horizontal: run a vertical bit
					RoutePoint rp = makeNodeInst(pin, new Point2D.Double(pt.getX(), v1.getY()), 0, nr);
					makeArcInst(type, width, rp, lastRP, nr);
					lastRP = rp;
				}
			}
		}

		for(int i=0; i<endIndex; i++)
		{
			SearchVertex sv = wf.vertices.get(i);
			boolean madeContacts = false;
			while (i < endIndex-1)
			{
				SearchVertex svNext = wf.vertices.get(i+1);
				if (sv.getX() != svNext.getX() || sv.getY() != svNext.getY() || sv.getZ() == svNext.getZ()) break;
				List<MetalVia> nps = metalVias[Math.min(sv.getZ(), svNext.getZ())].getVias();
				int whichContact = sv.getContactNo();
				MetalVia mv = nps.get(whichContact);
				RoutePoint rp = makeNodeInst(mv.via, new Point2D.Double(sv.getX(), sv.getY()), mv.orientation, nr);
				RoutingLayer type = metalLayers[sv.getZ()];
				double width = Math.max(type.getMinWidth(), nr.minWidth);
				makeArcInst(type, width, lastRP, rp, nr);
				madeContacts = true;
				sv = svNext;
				i++;
				lastRP = rp;
			}
			if (madeContacts && i != endIndex-1) continue;

			RoutingContact np = metalLayers[sv.getZ()].getPin();
			RoutePoint rp = null;
			if (i == endIndex-1)
			{
				Point2D fPoint = wf.from.getLocation();
				rp = new RoutePoint(startContact, fPoint, 0);
				if (!DBMath.doublesClose(fPoint.getX(), sv.getX()) || !DBMath.doublesClose(fPoint.getY(), sv.getY()))
				{
					// end of route is off-grid: adjust it
					if (endIndex >= 2)
					{
						SearchVertex v1 = wf.vertices.get(wf.vertices.size()-2);
						SearchVertex v2 = wf.vertices.get(wf.vertices.size()-1);
						RoutingLayer type = metalLayers[wf.fromZ];
						double width = Math.max(type.getMinWidth(), nr.minWidth);
						if (v1.getX() == v2.getX())
						{
							// last line is vertical: run a horizontal bit
							RoutingContact pNp = metalLayers[wf.fromZ].getPin();
							RoutePoint intRP = makeNodeInst(pNp, new Point2D.Double(v1.getX(), fPoint.getY()), 0, nr);
							makeArcInst(type, width, lastRP, intRP, nr);
							lastRP = intRP;
						} else if (v1.getY() == v2.getY())
						{
							// last line is horizontal: run a vertical bit
							RoutingContact pNp = metalLayers[wf.fromZ].getPin();
							RoutePoint intRP = makeNodeInst(pNp, new Point2D.Double(fPoint.getX(), v1.getY()), 0, nr);
							makeArcInst(type, width, lastRP, intRP, nr);
							lastRP = intRP;
						}
					}
				}
			} else
			{
				rp = makeNodeInst(np, new Point2D.Double(sv.getX(), sv.getY()), 0, nr);
			}
			if (lastRP != null)
			{
				RoutingLayer type = metalLayers[sv.getZ()];
				double width = Math.max(type.getMinWidth(), nr.minWidth);
				makeArcInst(type, width, lastRP, rp, nr);
			}
			lastRP = rp;
		}
	}

	/**
	 * Method to create a NodeInst and update the R-Trees.
	 * @param np the prototype of the new NodeInst.
	 * @param loc the location of the new NodeInst.
	 * @param wid the width of the new NodeInst.
	 * @param hei the height of the new NodeInst.
	 * @param orient the orientation of the new NodeInst.
	 * @param cell the Cell in which to place the new NodeInst.
	 * @param netID the network ID of geometry in this NodeInst.
	 */
	private RoutePoint makeNodeInst(RoutingContact np, Point2D loc, int angle, NeededRoute nr)
	{
		RoutePoint rp = new RoutePoint(np, loc, angle);
		nr.rs.addWireEnd(rp);

		List<RoutingGeometry> geoms = np.getGeometry();
		if (geoms != null)
		{
			for(RoutingGeometry rg : geoms)
			{
				addLayer(rg.getLayer(), rg.getBounds(), GenMath.MATID, nr.netID, false);
			}
		}
		return rp;
	}

	/**
	 * Method to create an ArcInst and update the R-Trees.
	 * @param type the prototype of the new ArcInst.
	 * @param wid the width of the new ArcInst.
	 * @param from the head PortInst of the new ArcInst.
	 * @param to the tail PortInst of the new ArcInst.
	 * @param netID the network ID of geometry in this ArcInst.
	 * @return the ArcInst that was created (null on error).
	 */
	private RouteWire makeArcInst(RoutingLayer type, double wid, RoutePoint from, RoutePoint to, NeededRoute nr)
	{
		RouteWire rw = new RouteWire(type, from, to, wid);
		nr.rs.addWire(rw);
//System.out.println("PLAN ARC ON LAYER "+type.getName()+" FROM "+from.getContact().getName()+
//	" AT ("+from.getLocation().getX()+","+from.getLocation().getY()+") TO "+to.getContact().getName()+
//	" AT ("+to.getLocation().getX()+","+to.getLocation().getY()+")");
		Rectangle2D box = makeArcBox(from.getLocation(), to.getLocation(), wid);
		if (box != null)
			addLayer(type, box, GenMath.MATID, nr.netID, false);

		return rw;
	}

	public Rectangle2D makeArcBox(Point2D from, Point2D to, double width)
	{
		double w2 = width / 2;
		int angle = DBMath.figureAngle(from, to);
        switch (angle)
        {
            case 0:
            case 1800:
            	double y = to.getY();
                double x1 = to.getX();
                double x2 = from.getX();
                return new Rectangle2D.Double(Math.min(x1, x2)-w2, y - w2, Math.abs(x1-x2)+width, width);
            case 900:
            case 2700:
            	double x = to.getX();
                double y1 = to.getY();
                double y2 = from.getY();
                return new Rectangle2D.Double(x-w2, Math.min(y1, y2)-w2, width, Math.abs(y1-y2)+width);
        }
        return null;
    }

	/**
	 * Method to sum up the distance that a route takes.
	 * @param vertices the list of SearchVertices in the route.
	 * @return the length of the route.
	 */
	private double getVertexLength(List<SearchVertex> vertices)
	{
		if (vertices == null) return Double.MAX_VALUE;
		if (vertices.size() == 0) return Double.MAX_VALUE;
		double sum = 0;
		SearchVertex last = null;
		for(SearchVertex sv : vertices)
		{
			if (last != null)
				sum += Math.abs(sv.getX() - last.getX()) +
					Math.abs(sv.getY() - last.getY()) +
					Math.abs(sv.getZ() - last.getZ())*10;
			last = sv;
		}
		return sum;
	}

	/**
	 * Method to find a path between two ports.
	 * @param nr the NeededRoute object with all necessary information.
	 * If successful, the NeededRoute's "vertices" field is filled with the route data.
	 */
	private void findPath(NeededRoute nr, Environment env, EditingPreferences ep)
	{
		// special case when route is null length
		Wavefront d1 = nr.dir1;
		if (DBMath.areEquals(d1.toX, d1.fromX) && DBMath.areEquals(d1.toY, d1.fromY) && d1.toZ == d1.fromZ)
		{
			nr.winningWF = d1;
			nr.winningWF.vertices = new ArrayList<SearchVertex>();
			SearchVertex sv = new SearchVertex(d1.toX, d1.toY, d1.toZ, 0, null, 0, nr.winningWF);
			nr.winningWF.vertices.add(sv);
			nr.cleanSearchMemory();
			return;
		}

		if (parallelDij)
		{
			// create threads and start them running
			Semaphore outSem = new Semaphore(0);
			new DijkstraInThread("Route a->b", nr.dir1, nr.dir2, outSem, env, ep);
			new DijkstraInThread("Route b->a", nr.dir2, nr.dir1, outSem, env, ep);

			// wait for threads to complete and get results
			outSem.acquireUninterruptibly(2);
		} else
		{
			// run both wavefronts in parallel (interleaving steps)
			doTwoWayDijkstra(nr);
		}

		// analyze the winning wavefront
		Wavefront wf = nr.winningWF;
		double verLength = Double.MAX_VALUE;
		if (wf != null) verLength = getVertexLength(wf.vertices);
		if (verLength == Double.MAX_VALUE)
		{
			// failed to route
			String errorMsg;
			if (wf == null) wf = nr.dir1;
			if (wf.vertices == null)
			{
				errorMsg = "Search too complex (exceeds complexity limit of " +
					complexityLimit.getIntValue() + " steps)";
			} else
			{
				errorMsg = "Failed to route from port " + wf.from.describe() + " to port " + wf.to.describe();
			}
			System.out.println("ERROR: " + errorMsg);
			List<EPoint> lineList = new ArrayList<EPoint>();
			lineList.add(new EPoint(wf.toX, wf.toY));
			lineList.add(new EPoint(wf.fromX, wf.fromY));
			errorLogger.logMessageWithLines(errorMsg, null, lineList, cell, 0, true);
		}
		nr.cleanSearchMemory();
	}

	private void doTwoWayDijkstra(NeededRoute nr)
	{
		SearchVertex result = null;
		int numSearchVertices = 0;
		while (result == null)
		{
			// stop if the search is too complex
			numSearchVertices++;
			if (numSearchVertices > complexityLimit.getIntValue()) return;

			SearchVertex resultA = advanceWavefront(nr.dir1);
			SearchVertex resultB = advanceWavefront(nr.dir2);
			if (resultA != null || resultB != null)
			{
				result = resultA;
				nr.winningWF = nr.dir1;
				if (result == null || result == svExhausted)
				{
					result = resultB;
					nr.winningWF = nr.dir2;
				}
			}
		}
		if (result == svAborted || result == svExhausted)
		{
			nr.winningWF = null;
			return;
		}

		List<SearchVertex> realVertices = getOptimizedList(result);
		nr.winningWF.vertices = realVertices;
	}

	private class DijkstraInThread extends Thread
	{
		private Wavefront wf;
		private Wavefront otherWf;
		private Semaphore whenDone;
        private Environment env;
        private EditingPreferences ep;

		public DijkstraInThread(String name, Wavefront wf, Wavefront otherWf, Semaphore whenDone, Environment env, EditingPreferences ep)
		{
			super(name);
			this.wf = wf;
			this.otherWf = otherWf;
			this.whenDone = whenDone;
            this.env = env;
            this.ep = ep;
			start();
		}

		public void run()
		{
            Environment.setThreadEnvironment(env);
            EditingPreferences.setThreadEditingPreferences(ep);
			SearchVertex result = null;
			int numSearchVertices = 0;
			while (result == null)
			{
				// stop if the search is too complex
				numSearchVertices++;
				if (numSearchVertices > complexityLimit.getIntValue())
				{
					result = svLimited;
				} else
				{
					if (wf.abort) result = svAborted; else
					{
						result = advanceWavefront(wf);
					}
				}
			}
			if (result != svAborted && result != svExhausted && result != svLimited)
			{
				if (DEBUGLOOPS)
					System.out.println("    Wavefront " + wf.name + " first completion");
				wf.vertices = getOptimizedList(result);
				wf.nr.winningWF = wf;
				otherWf.abort = true;
			} else
			{
				if (DEBUGLOOPS)
				{
	                String status = "completed";
	                if (result == svAborted) status = "aborted"; else
	                	if (result == svExhausted) status = "exhausted"; else
	                		if (result == svLimited) status = "limited";
					System.out.println("    Wavefront " + wf.name + " " + status);
				}
			}
			whenDone.release();
		}
	}

	private SearchVertex advanceWavefront(Wavefront wf)
	{
		// get the lowest cost point
		if (wf.active.size() == 0) return svExhausted;
		SearchVertex svCurrent = wf.active.first();
		wf.active.remove(svCurrent);
		double curX = svCurrent.getX();
		double curY = svCurrent.getY();
		int curZ = svCurrent.getZ();

		if (wf.debug) System.out.print("AT ("+TextUtils.formatDouble(curX)+","+TextUtils.formatDouble(curY)+",M"
			+(curZ+1)+")C="+svCurrent.cost+" WENT");

		// look at all directions from this point
		for(int i=0; i<6; i++)
		{
			// compute a neighboring point
			double dx = 0, dy = 0;
			int dz = 0;
			switch (i)
			{
				case 0:
					dx = -GRAINSIZE;
					double intermediate = upToGrainAlways(curX+dx);
					if (intermediate != curX+dx) dx = intermediate - curX;
					break;
				case 1:
					dx =  GRAINSIZE;
					intermediate = downToGrainAlways(curX+dx);
					if (intermediate != curX+dx) dx = intermediate - curX;
					break;
				case 2:
					dy = -GRAINSIZE;
					intermediate = upToGrainAlways(curY+dy);
					if (intermediate != curY+dy) dy = intermediate - curY;
					break;
				case 3:
					dy =  GRAINSIZE;
					intermediate = downToGrainAlways(curY+dy);
					if (intermediate != curY+dy) dy = intermediate - curY;
					break;
				case 4: dz = -1;   break;
				case 5: dz =  1;   break;
			}

			// extend the distance if heading toward the goal
			boolean stuck = false;
			if (dz == 0)
			{
				boolean goFarther = false;
				if (dx != 0)
				{
					if ((wf.toX-curX) * dx > 0) goFarther = true;
				} else
				{
					if ((wf.toY-curY) * dy > 0) goFarther = true;
				}
				if (goFarther)
				{
					double jumpSize = getJumpSize(curX, curY, curZ, dx, dy, wf);
					if (dx > 0)
					{
						if (jumpSize <= 0) stuck = true;
						dx = jumpSize;
					}
					if (dx < 0)
					{
						if (jumpSize >= 0) stuck = true;
						dx = jumpSize;
					}
					if (dy > 0)
					{
						if (jumpSize <= 0) stuck = true;
						dy = jumpSize;
					}
					if (dy < 0)
					{
						if (jumpSize >= 0) stuck = true;
						dy = jumpSize;
					}
				}
			}
			double nX = curX + dx;
			double nY = curY + dy;
			int nZ = curZ + dz;

			if (wf.debug)
			{
				switch (i)
				{
					case 0: System.out.print("  X-"+TextUtils.formatDouble(Math.abs(dx)));   break;
					case 1: System.out.print("  X+"+TextUtils.formatDouble(dx));   break;
					case 2: System.out.print("  Y-"+TextUtils.formatDouble(Math.abs(dy)));   break;
					case 3: System.out.print("  Y+"+TextUtils.formatDouble(dy));   break;
					case 4: System.out.print("  -Z");   break;
					case 5: System.out.print("  +Z");   break;
				}
			}
			if (stuck)
			{
				if (wf.debug) System.out.print(":CannotMove");
				continue;
			}

			if (nX < wf.nr.minimumSearchBoundX)
			{
				nX = wf.nr.minimumSearchBoundX;
				dx = nX - curX;
				if (dx == 0) { if (wf.debug) System.out.print(":OutOfBounds");   continue; }
			}
			if (nX > wf.nr.maximumSearchBoundX)
			{
				nX = wf.nr.maximumSearchBoundX;
				dx = nX - curX;
				if (dx == 0) { if (wf.debug) System.out.print(":OutOfBounds");   continue; }
			}
			if (nY < wf.nr.minimumSearchBoundY)
			{
				nY = wf.nr.minimumSearchBoundY;
				dy = nY - curY;
				if (dy == 0) { if (wf.debug) System.out.print(":OutOfBounds");   continue; }
			}
			if (nY > wf.nr.maximumSearchBoundY)
			{
				nY = wf.nr.maximumSearchBoundY;
				dy = nY - curY;
				if (dy == 0) { if (wf.debug) System.out.print(":OutOfBounds");   continue; }
			}
			if (nZ < 0 || nZ >= numMetalLayers) { if (wf.debug) System.out.print(":OutOfBounds");   continue; }

			// see if the adjacent point has already been visited
			if (wf.getVertex(nX, nY, nZ)) { if (wf.debug) System.out.print(":AlreadyVisited");   continue; }

			// see if the space is available
			int whichContact = 0;
			Point2D [] cuts = null;
			if (dz == 0)
			{
				// running on one layer: check surround
				double width = Math.max(metalLayers[nZ].getMinWidth(), wf.nr.minWidth);
				double metalSpacing = width / 2;
				boolean allClear = false;
				for(;;)
				{
					SearchVertex prevPath = svCurrent;
					double checkX = (curX+nX)/2, checkY = (curY+nY)/2;
					double halfWid = metalSpacing + Math.abs(dx)/2;
					double halfHei = metalSpacing + Math.abs(dy)/2;
					while (prevPath != null && prevPath.last != null)
					{
						if (prevPath.zv != nZ || prevPath.last.zv != nZ) break;
						if (prevPath.xv == prevPath.last.xv && dx == 0)
						{
							checkY = (prevPath.last.yv + nY) / 2;
							halfHei = metalSpacing + Math.abs(prevPath.last.yv - nY)/2;
							prevPath = prevPath.last;
						} else if (prevPath.yv == prevPath.last.yv && dy == 0)
						{
							checkX = (prevPath.last.xv + nX) / 2;
							halfWid = metalSpacing + Math.abs(prevPath.last.xv - nX)/2;
							prevPath = prevPath.last;
						} else break;
					}
					SOGBound sb = getMetalBlockageAndNotch(wf,  nZ, halfWid, halfHei, checkX, checkY, prevPath);
					if (sb == null) { allClear = true;   break; }

					// see if it can be backed out slightly
					if (i == 0)
					{
						// moved left too far...try a bit to the right
						double newNX = downToGrainAlways(nX + GRAINSIZE);
						if (newNX >= curX) break;
						dx = newNX - curX;
					} else if (i == 1)
					{
						// moved right too far...try a bit to the left
						double newNX = upToGrainAlways(nX - GRAINSIZE);
						if (newNX <= curX) break;
						dx = newNX - curX;
					} else if (i == 2)
					{
						// moved down too far...try a bit up
						double newNY = downToGrainAlways(nY + GRAINSIZE);
						if (newNY >= curY) break;
						double newDY = newNY - curY;
						dy = newDY;
					} else if (i == 3)
					{
						// moved up too far...try a bit down
						double newNY = upToGrainAlways(nY - GRAINSIZE);
						if (newNY <= curY) break;
						dy = newNY - curY;
					}

					nX = curX + dx;
					nY = curY + dy;
				}
				if (!allClear)
				{
					if (wf.debug)
					{
						double checkX = (curX+nX)/2, checkY = (curY+nY)/2;
						double halfWid = metalSpacing + Math.abs(dx)/2;
						double halfHei = metalSpacing + Math.abs(dy)/2;
						double surround = metalLayers[nZ].getMaxSurround();
						SOGBound sb = getMetalBlockage(wf.nr.netID, nZ, halfWid, halfHei, surround, checkX, checkY);
						if (sb != null) System.out.print(":Blocked"); else
							System.out.print(":BlockedNotch");
					}
					continue;
				}
			} else
			{
				int lowMetal = Math.min(curZ, nZ);
				int highMetal = Math.max(curZ, nZ);
				List<MetalVia> nps = metalVias[lowMetal].getVias();
				whichContact = -1;
				for(int contactNo = 0; contactNo < nps.size(); contactNo++)
				{
					MetalVia mv = nps.get(contactNo);
					RoutingContact np = mv.via;
					List<RoutingGeometry> conGeoms = mv.via.getGeometry();
					AffineTransform trans = null;
					if (mv.orientation != 0) trans = makePureTransform(mv.orientation);

					// count the number of cuts and make an array for the data
					int cutCount = 0;
					for(RoutingGeometry rg : conGeoms)
						if (!rg.getLayer().isMetal()) cutCount++;

					Point2D [] curCuts = new Point2D[cutCount];
					cutCount = 0;
					boolean failed = false;
					for(RoutingGeometry rg : conGeoms)
					{
						Rectangle2D conRect = rg.getBounds();
						if (trans != null)
						{
							conRect = (Rectangle2D)conRect.clone();
							DBMath.transformRect(conRect, trans);
						}
						RoutingLayer conLayer = rg.getLayer();
						if (conLayer.isMetal())
						{
							int metalNo = conLayer.getMetalNumber() - 1;
							double halfWid = conRect.getWidth()/2;
							double halfHei = conRect.getHeight()/2;
							if (getMetalBlockageAndNotch(wf, metalNo, halfWid, halfHei,
								conRect.getCenterX(), conRect.getCenterY(), svCurrent) != null)
							{
								failed = true;
								break;
							}
						} else
						{
							// make sure vias don't get too close
							double conCX = conRect.getCenterX();
							double conCY = conRect.getCenterY();
							double surround = np.getViaSpacing();
							if (getViaBlockage(wf.nr.netID, conLayer, surround, surround, conCX, conCY) != null)
							{
								failed = true;
								break;
							}
							curCuts[cutCount++] = new Point2D.Double(conCX, conCY);

							// look at all previous cuts in this path
							for(SearchVertex sv = svCurrent; sv != null; sv = sv.last)
							{
								SearchVertex lastSv = sv.last;
								if (lastSv == null) break;
								if (Math.min(sv.getZ(), lastSv.getZ()) == lowMetal &&
									Math.max(sv.getZ(), lastSv.getZ()) == highMetal)
								{
									// make sure the cut isn't too close
									Point2D [] svCuts;
									if (sv.getCutLayer() == lowMetal) svCuts = sv.getCuts(); else
										svCuts = lastSv.getCuts();
									if (svCuts != null)
									{
										for(Point2D cutPt : svCuts)
										{
											if (Math.abs(cutPt.getX() - conCX) >= surround ||
												Math.abs(cutPt.getY() - conCY) >= surround) continue;
											failed = true;
											break;
										}
									}
									if (failed) break;
								}
							}
							if (failed) break;
						}
					}
					if (failed) continue;
					whichContact = contactNo;
					cuts = curCuts;
					break;
				}
				if (whichContact < 0) { if (wf.debug) System.out.print(":Blocked");   continue; }
			}

			// we have a candidate next-point
			SearchVertex svNext = new SearchVertex(nX, nY, nZ, whichContact, cuts, Math.min(curZ, nZ), wf);
			svNext.last = svCurrent;

			// stop if we found the destination
			boolean foundDest = DBMath.areEquals(nX, wf.toX) && DBMath.areEquals(nY, wf.toY);
			if (foundDest && nZ == wf.toZ)
			{
				if (wf.debug) System.out.print(":FoundDestination");
				return svNext;
			}

			// compute the cost
			svNext.cost = svCurrent.cost;
			if (dx != 0)
			{
				if (wf.toX == curX) svNext.cost += COSTWRONGDIRECTION/2; else
					if ((wf.toX-curX) * dx < 0) svNext.cost += COSTWRONGDIRECTION;
				if ((nZ%2) == 0)
				{
					int c = COSTALTERNATINGMETAL * (int)Math.abs(dx) / (int)cellBounds.getWidth();
					svNext.cost += c;
				}
			}
			if (dy != 0)
			{
				if (wf.toY == curY) svNext.cost += COSTWRONGDIRECTION/2; else
					if ((wf.toY-curY) * dy < 0) svNext.cost += COSTWRONGDIRECTION;
				if ((nZ%2) != 0)
				{
					int c = COSTALTERNATINGMETAL * (int)Math.abs(dy) / (int)cellBounds.getHeight();
					svNext.cost += c;
				}
			}
			if (dz != 0)
			{
				if (wf.toZ == curZ) svNext.cost += COSTLAYERCHANGE; else
					if ((wf.toZ-curZ) * dz < 0) svNext.cost += COSTLAYERCHANGE * COSTWRONGDIRECTION;
			} else
			{
				// not changing layers: compute penalty for unused tracks on either side of run
				double jumpSize1 = Math.abs(getJumpSize(nX, nY, nZ, dx, dy, wf));
				double jumpSize2 = Math.abs(getJumpSize(curX, curY, curZ, -dx, -dy, wf));
				if (jumpSize1 > GRAINSIZE && jumpSize2 > GRAINSIZE)
				{
					svNext.cost += (jumpSize1 * jumpSize2) / 10;
				}

				// not changing layers: penalize if turning in X or Y
				if (svCurrent.last != null)
				{
					boolean xTurn = svCurrent.getX() != svCurrent.last.getX();
					boolean yTurn = svCurrent.getY() != svCurrent.last.getY();
					if (xTurn != (dx != 0) || yTurn != (dy != 0)) svNext.cost += COSTTURNING;
				}
			}
			if (downToGrainAlways(nX) != nX && nX != wf.toX) svNext.cost += COSTOFFGRID;
			if (downToGrainAlways(nY) != nY && nY != wf.toY) svNext.cost += COSTOFFGRID;

			// add this vertex into the data structures
			wf.setVertex(nX, nY, nZ);
			wf.active.add(svNext);
			if (wf.debug)
				System.out.print("("+TextUtils.formatDouble(svNext.getX())+","+TextUtils.formatDouble(svNext.getY())+
					",M"+(svNext.getZ()+1)+")C="+svNext.cost);

			// add intermediate steps along the way if it was a jump
			if (dz == 0)
			{
				if (Math.abs(dx) > 1)
				{
					double inc = dx < 0 ? 1 : -1;
					double lessDX = dx + inc;
					int cost = svNext.cost;
					int countDown = 0;
					for(;;)
					{
						double nowX = curX+lessDX;
						cost++;
						if (!wf.getVertex(nowX, nY, nZ))
						{
							SearchVertex svIntermediate = new SearchVertex(nowX, nY, nZ, whichContact, cuts, curZ, wf);
							svIntermediate.last = svCurrent;
							svIntermediate.cost = cost;
							wf.setVertex(nowX, nY, nZ);
							wf.active.add(svIntermediate);
						}
						lessDX += inc;
						if (inc < 0)
						{
							if (lessDX < 1) break;
						} else
						{
							if (lessDX > -1) break;
						}
						if (countDown++ >= 10) { countDown = 0;   inc += inc; }
					}
				}
				if (Math.abs(dy) > 1)
				{
					double inc = dy < 0 ? 1 : -1;
					double lessDY = dy + inc;
					int cost = svNext.cost;
					int countDown = 0;
					for(;;)
					{
						double nowY = curY+lessDY;
						cost++;
						if (!wf.getVertex(nX, nowY, nZ))
						{
							SearchVertex svIntermediate = new SearchVertex(nX, nowY, nZ, whichContact, cuts, curZ, wf);
							svIntermediate.last = svCurrent;
							svIntermediate.cost = cost;
							wf.setVertex(nX, nowY, nZ);
							wf.active.add(svIntermediate);
						}
						lessDY += inc;
						if (inc < 0)
						{
							if (lessDY < 1) break;
						} else
						{
							if (lessDY > -1) break;
						}
						if (countDown++ >= 10) { countDown = 0;   inc += inc; }
					}
				}
			}
		}
		if (wf.debug) System.out.println();
		return null;
	}

	/**
	 * Method to convert a linked list of SearchVertex objects to an optimized path.
	 * @param initialThread the initial SearchVertex in the linked list.
	 * @return a List of SearchVertex objects optimized to consolidate runs in the X
	 * or Y axes.
	 */
	private List<SearchVertex> getOptimizedList(SearchVertex initialThread)
	{
		List<SearchVertex> realVertices = new ArrayList<SearchVertex>();
		SearchVertex thread = initialThread;
		if (thread != null)
		{
			SearchVertex lastVertex = thread;
			realVertices.add(lastVertex);
			thread = thread.last;
			while (thread != null)
			{
				if (lastVertex.getZ() != thread.getZ())
				{
					realVertices.add(thread);
					lastVertex = thread;
					thread = thread.last;
				} else
				{
					// gather a run of vertices on this layer
					double dx = thread.getX() - lastVertex.getX();
					double dy = thread.getY() - lastVertex.getY();
					lastVertex = thread;
					thread = thread.last;
					while (thread != null)
					{
						if (lastVertex.getZ() != thread.getZ()) break;
						if ((thread.getX() - lastVertex.getX() != 0 && dx == 0) ||
							(thread.getY() - lastVertex.getY() != 0 && dy == 0)) break;
						lastVertex = thread;
						thread = thread.last;
					}
					realVertices.add(lastVertex);
				}
			}
		}
		return realVertices;
	}

	private double getJumpSize(double curX, double curY, int curZ, double dx, double dy, Wavefront wf)
	{
		Rectangle2D jumpBound = wf.nr.jumpBound;
		double width = Math.max(metalLayers[curZ].getMinWidth(), wf.nr.minWidth);
		double metalToMetal = wf.getSpacingRule(curZ, width, -1);
		double metalSpacing = width / 2 + metalToMetal;
		double lX = curX - metalSpacing, hX = curX + metalSpacing;
		double lY = curY - metalSpacing, hY = curY + metalSpacing;
		if (dx > 0) hX = jumpBound.getMaxX()+metalSpacing; else
			if (dx < 0) lX = jumpBound.getMinX()-metalSpacing; else
				if (dy > 0) hY = jumpBound.getMaxY()+metalSpacing; else
					if (dy < 0) lY = jumpBound.getMinY()-metalSpacing;

		RTNode rtree = metalTrees.get(metalLayers[curZ]);
		if (rtree != null)
		{
			// see if there is anything in that area
			Rectangle2D searchArea = new Rectangle2D.Double(lX, lY, hX-lX, hY-lY);
			for(RTNode.Search sea = new RTNode.Search(searchArea, rtree, true); sea.hasNext(); )
			{
				SOGBound sBound = (SOGBound)sea.next();
				if (Math.abs(sBound.getNetID()) == wf.nr.netID) continue;
				Rectangle2D bound = sBound.getBounds();
				if (bound.getMinX() >= hX || bound.getMaxX() <= lX ||
					bound.getMinY() >= hY || bound.getMaxY() <= lY) continue;
				if (dx > 0 && bound.getMinX() < hX) hX = bound.getMinX();
				if (dx < 0 && bound.getMaxX() > lX) lX = bound.getMaxX();
				if (dy > 0 && bound.getMinY() < hY) hY = bound.getMinY();
				if (dy < 0 && bound.getMaxY() > lY) lY = bound.getMaxY();
			}
		}
		if (dx > 0)
		{
			dx = downToGrain(hX-metalSpacing)-curX;
			if (curX+dx > wf.toX && curY == wf.toY && curZ == wf.toZ) dx = wf.toX-curX;
			if (curX+dx != wf.toX) dx = downToGrainAlways(hX-metalSpacing)-curX;
			return dx;
		}
		if (dx < 0)
		{
			dx = upToGrain(lX+metalSpacing)-curX;
			if (curX+dx < wf.toX && curY == wf.toY && curZ == wf.toZ) dx = wf.toX-curX;
			if (curX+dx != wf.toX) dx = upToGrainAlways(lX+metalSpacing)-curX;
			return dx;
		}
		if (dy > 0)
		{
			dy = downToGrain(hY-metalSpacing)-curY;
			if (curX == wf.toX && curY+dy > wf.toY && curZ == wf.toZ) dy = wf.toY-curY;
			if (curY+dy != wf.toY) dy = downToGrainAlways(hY-metalSpacing)-curY;
			return dy;
		}
		if (dy < 0)
		{
			dy = upToGrain(lY+metalSpacing)-curY;
			if (curX == wf.toX && curY+dy < wf.toY && curZ == wf.toZ) dy = wf.toY-curY;
			if (curY+dy != wf.toY) dy = upToGrainAlways(lY+metalSpacing)-curY;
			return dy;
		}
		return 0;
	}

	/**
	 * Method to round a value up to the nearest routing grain size.
	 * @param v the value to round up.
	 * @return the granularized value.
	 */
	private double upToGrain(double v)
	{
		return v;
	}

	/**
	 * Method to round a value up to the nearest routing grain size.
	 * @param v the value to round up.
	 * @return the granularized value.
	 */
	private double upToGrainAlways(double v)
	{
		return Math.ceil(v * GRANULARITY) * GRAINSIZE;
	}

	/**
	 * Method to round a value down to the nearest routing grain size.
	 * @param v the value to round down.
	 * @return the granularized value.
	 */
	private double downToGrain(double v)
	{
		return v;
	}

	/**
	 * Method to round a value down to the nearest routing grain size.
	 * @param v the value to round down.
	 * @return the granularized value.
	 */
	private double downToGrainAlways(double v)
	{
		return Math.floor(v * GRANULARITY) * GRAINSIZE;
	}

	/************************************** BLOCKAGE DATA STRUCTURE **************************************/

	/**
	 * Class to define an R-Tree leaf node for geometry in the blockage data structure.
	 */
	private static class SOGBound implements RTBounds
	{
		private Rectangle2D bound;
		private int netID;

		SOGBound(Rectangle2D bound, int netID)
		{
			this.bound = bound;
			this.netID = netID;
		}

		public Rectangle2D getBounds() { return bound; }

		public int getNetID() { return netID; }

		public String toString() { return "SOGBound on net " + netID; }
	}

	/**
	 * Class to define an R-Tree leaf node for vias in the blockage data structure.
	 */
	private static class SOGVia implements RTBounds
	{
		private Point2D loc;
		private int netID;

		SOGVia(Point2D loc, int netID)
		{
			this.loc = loc;
			this.netID = netID;
		}

		public Rectangle2D getBounds() { return new Rectangle2D.Double(loc.getX(), loc.getY(), 0, 0); }

		public int getNetID() { return netID; }

		public String toString() { return "SOGVia on net " + netID; }
	}

	/**
	 * Method to see if a proposed piece of metal has DRC errors.
	 * @param wf the Wavefront being processed.
	 * @param metNo the level of the metal.
	 * @param halfWidth half of the width of the metal.
	 * @param halfHeight half of the height of the metal.
	 * @param x the X coordinate at the center of the metal.
	 * @param y the Y coordinate at the center of the metal.
	 * @param svCurrent the list of SearchVertex's for finding notch errors in the current path.
	 * @return a blocking SOGBound object that is in the area.
	 * Returns null if the area is clear.
	 */
	private SOGBound getMetalBlockageAndNotch(Wavefront wf, int metNo, double halfWidth, double halfHeight,
		double x, double y, SearchVertex svCurrent)
	{
		// get the R-Tree data for the metal layer
		RoutingLayer layer = metalLayers[metNo];
		RTNode rtree = metalTrees.get(layer);
		if (rtree == null) return null;

		// determine the size and width/length of this piece of metal
		int netID = wf.nr.netID;
		double minWidth = wf.nr.minWidth;
		double metLX = x - halfWidth, metHX = x + halfWidth;
		double metLY = y - halfHeight, metHY = y + halfHeight;
		Rectangle2D metBound = new Rectangle2D.Double(metLX, metLY, metHX-metLX, metHY-metLY);
		double metWid = Math.min(halfWidth, halfHeight) * 2;
		double metLen = Math.max(halfWidth, halfHeight) * 2;

		// determine the area to search about the metal
		double surround = metalLayers[metNo].getMaxSurround();
		double lX = metLX - surround, hX = metHX + surround;
		double lY = metLY - surround, hY = metHY + surround;
		Rectangle2D searchArea = new Rectangle2D.Double(lX, lY, hX-lX, hY-lY);

		// prepare for notch detection
		List<Rectangle2D> recsOnPath;

		// make a list of rectangles on the path
		recsOnPath = new ArrayList<Rectangle2D>();
		if (svCurrent != null)
		{
			List<SearchVertex> svList = getOptimizedList(svCurrent);
			for(int ind=1; ind<svList.size(); ind++)
			{
				SearchVertex sv = svList.get(ind);
				SearchVertex lastSv = svList.get(ind-1);
				if (sv.getZ() != metNo && lastSv.getZ() != metNo) continue;
				if (sv.getZ() != lastSv.getZ())
				{
					// changed layers: compute via rectangles
					List<MetalVia> nps = metalVias[Math.min(sv.getZ(), lastSv.getZ())].getVias();
					int whichContact = lastSv.getContactNo();
					MetalVia mv = nps.get(whichContact);
					RoutingContact np = mv.via;
					AffineTransform trans = null;
					if (mv.orientation != 0) trans = makePureTransform(mv.orientation);
					List<RoutingGeometry> conPolys = np.getGeometry();
					for(RoutingGeometry rg : conPolys)
					{
						if (rg.getLayer() != layer) continue;
						Rectangle2D bound = rg.getBounds();
						if (trans != null)
						{
							bound = (Rectangle2D)bound.clone();
							DBMath.transformRect(bound, trans);
						}
						if (bound.getMaxX() <= lX || bound.getMinX() >= hX ||
							bound.getMaxY() <= lY || bound.getMinY() >= hY) continue;
						recsOnPath.add(bound);
					}
					continue;
				}

				// stayed on one layer: compute arc rectangle
				RoutingLayer type = metalLayers[metNo];
				double width = Math.max(type.getMinWidth(), minWidth);
				Point2D head = new Point2D.Double(sv.getX(), sv.getY());
				Point2D tail = new Point2D.Double(lastSv.getX(), lastSv.getY());
				Rectangle2D bound = makeArcBox(head, tail, width);
				if (bound != null)
				{
					if (bound.getMaxX() <= lX || bound.getMinX() >= hX ||
						bound.getMaxY() <= lY || bound.getMinY() >= hY) continue;
					recsOnPath.add(bound);
				}
			}
		}

		for(RTNode.Search sea = new RTNode.Search(searchArea, rtree, true); sea.hasNext(); )
		{
			SOGBound sBound = (SOGBound)sea.next();
			Rectangle2D bound = sBound.getBounds();

			// eliminate if out of worst surround
			if (bound.getMaxX() <= lX || bound.getMinX() >= hX ||
				bound.getMaxY() <= lY || bound.getMinY() >= hY) continue;

			// see if it is within design-rule distance
			double drWid = Math.max(Math.min(bound.getWidth(), bound.getHeight()), metWid);
			double drLen = Math.max(Math.max(bound.getWidth(), bound.getHeight()), metLen);
			double spacing = wf.getSpacingRule(metNo, drWid, drLen);
			double lXAllow = metLX - spacing, hXAllow = metHX + spacing;
			double lYAllow = metLY - spacing, hYAllow = metHY + spacing;
			if (DBMath.isLessThanOrEqualTo(bound.getMaxX(), lXAllow) || DBMath.isGreaterThanOrEqualTo(bound.getMinX(), hXAllow) ||
				DBMath.isLessThanOrEqualTo(bound.getMaxY(), lYAllow) || DBMath.isGreaterThanOrEqualTo(bound.getMinY(), hYAllow)) continue;

			// too close for DRC: allow if on the same net
			if (Math.abs(sBound.getNetID()) == netID)
			{
				// on same net: make sure there is no notch error
				if (sBound.getNetID() >= 0)
				{
					if (foundANotch(rtree, metBound, sBound.bound, netID, recsOnPath, spacing)) return sBound;
				}
				continue;
			}

			// DRC error found: return the offending geometry
			return sBound;
		}

		// consider notch errors in the existing path
		if (svCurrent != null)
		{
			double spacing = wf.getSpacingRule(metNo, metWid, metLen);
			List<SearchVertex> svList = getOptimizedList(svCurrent);
			for(int ind=1; ind<svList.size(); ind++)
			{
				SearchVertex sv = svList.get(ind);
				SearchVertex lastSv = svList.get(ind-1);
				if (sv.getZ() != metNo && lastSv.getZ() != metNo) continue;
				if (sv.getZ() != lastSv.getZ())
				{
					// changed layers: analyze the contact for notches
					List<MetalVia> nps = metalVias[Math.min(sv.getZ(), lastSv.getZ())].getVias();
					int whichContact = lastSv.getContactNo();
					MetalVia mv = nps.get(whichContact);
					RoutingContact np = mv.via;
					AffineTransform trans = null;
					if (mv.orientation != 0) trans = makePureTransform(mv.orientation);
					List<RoutingGeometry> conPolys = np.getGeometry();
					for(RoutingGeometry rg : conPolys)
					{
						if (rg.getLayer() != layer) continue;
						Rectangle2D bound = rg.getBounds();
						if (trans != null)
						{
							bound = (Rectangle2D)bound.clone();
							DBMath.transformRect(bound, trans);
						}
						if (bound.getMaxX() <= lX || bound.getMinX() >= hX ||
							bound.getMaxY() <= lY || bound.getMinY() >= hY) continue;
						SOGBound sBound = new SOGBound(bound, netID);
						if (foundANotch(rtree, metBound, bound, netID, recsOnPath, spacing)) return sBound;
					}
					continue;
				}

				// stayed on one layer: analyze the arc for notches
				RoutingLayer type = metalLayers[metNo];
				double width = Math.max(type.getMinWidth(), minWidth);
				Point2D head = new Point2D.Double(sv.getX(), sv.getY());
				Point2D tail = new Point2D.Double(lastSv.getX(), lastSv.getY());
				Rectangle2D bound = makeArcBox(head, tail, width);
				if (bound != null)
				{
					if (bound.getMaxX() <= lX || bound.getMinX() >= hX ||
						bound.getMaxY() <= lY || bound.getMinY() >= hY) continue;
					SOGBound sBound = new SOGBound(bound, netID);
					if (foundANotch(rtree, metBound, bound, netID, recsOnPath, spacing)) return sBound;
				}
			}
		}
		return null;
	}

	private AffineTransform makePureTransform(int angle)
	{
        int sect = angle / 45;
        int ang = angle % 45;
        if (sect % 2 != 0) ang = 45 - ang;
        double cos0, sin0;
        if (ang == 0) {
            cos0 = 1;
            sin0 = 0;
        } else if (ang == 45) {
            cos0 = sin0 = StrictMath.sqrt(0.5);
        } else {
            double alpha = ang * Math.PI / 180.0;
            cos0 = StrictMath.cos(alpha);
            sin0 = StrictMath.sin(alpha);
        }
        double cos = 0, sin = 0;
        switch (sect) {
            case 0: cos =  cos0; sin =  sin0; break;
            case 1: cos =  sin0; sin =  cos0; break;
            case 2: cos = -sin0; sin =  cos0; break;
            case 3: cos = -cos0; sin =  sin0; break;
            case 4: cos = -cos0; sin = -sin0; break;
            case 5: cos = -sin0; sin = -cos0; break;
            case 6: cos =  sin0; sin = -cos0; break;
            case 7: cos =  cos0; sin = -sin0; break;
        }
		double[] matrix = new double[4];
		matrix[0] = cos;
		matrix[1] = sin;
		matrix[2] = sin;
		matrix[3] = cos;
		return new AffineTransform(matrix);
	}

	/**
	 * Method to tell whether there is a notch between two pieces of metal.
	 * @param rtree the R-Tree with the metal information.
	 * @param metBound one piece of metal.
	 * @param bound another piece of metal.
	 * @return true if there is a notch error between the pieces of metal.
	 */
	private boolean foundANotch(RTNode rtree, Rectangle2D metBound, Rectangle2D bound, int netID,
		List<Rectangle2D> recsOnPath, double dist)
	{
		// see if they overlap in X or Y
		boolean hOverlap = metBound.getMinX() <= bound.getMaxX() && metBound.getMaxX() >= bound.getMinX();
		boolean vOverlap = metBound.getMinY() <= bound.getMaxY() && metBound.getMaxY() >= bound.getMinY();

		// if they overlap in both, they touch and it is not a notch
		if (hOverlap && vOverlap) return false;

		// if they overlap horizontally then they line-up vertically
		if (hOverlap)
		{
			double ptY;
			if (metBound.getCenterY() > bound.getCenterY())
			{
				if (metBound.getMinY() - bound.getMaxY() > dist) return false;
				ptY = (metBound.getMinY() + bound.getMaxY()) / 2;
			} else
			{
				if (bound.getMinY() - metBound.getMaxY() > dist) return false;
				ptY = (metBound.getMaxY() + bound.getMinY()) / 2;
			}
			double pt1X = Math.max(metBound.getMinX(), bound.getMinX());
			double pt2X = Math.min(metBound.getMaxX(), bound.getMaxX());
			double pt3X = (pt1X + pt2X) / 2;
			if (!pointInRTree(rtree, pt1X, ptY, netID, recsOnPath)) return true;
			if (!pointInRTree(rtree, pt2X, ptY, netID, recsOnPath)) return true;
			if (!pointInRTree(rtree, pt3X, ptY, netID, recsOnPath)) return true;
			return false;
		}

		// if they overlap vertically then they line-up horizontally
		if (vOverlap)
		{
			double ptX;
			if (metBound.getCenterX() > bound.getCenterX())
			{
				if (metBound.getMinX() - bound.getMaxX() > dist) return false;
				ptX = (metBound.getMinX() + bound.getMaxX()) / 2;
			} else
			{
				if (bound.getMinX() - metBound.getMaxX() > dist) return false;
				ptX = (metBound.getMaxX() + bound.getMinX()) / 2;
			}
			double pt1Y = Math.max(metBound.getMinY(), bound.getMinY());
			double pt2Y = Math.min(metBound.getMaxY(), bound.getMaxY());
			double pt3Y = (pt1Y + pt2Y) / 2;
			if (!pointInRTree(rtree, ptX, pt1Y, netID, recsOnPath)) return true;
			if (!pointInRTree(rtree, ptX, pt2Y, netID, recsOnPath)) return true;
			if (!pointInRTree(rtree, ptX, pt3Y, netID, recsOnPath)) return true;
			return false;
		}

		// they are diagonal, ensure that one of the "L"s is filled
		if (metBound.getMinX() > bound.getMaxX() && metBound.getMinY() > bound.getMaxY())
		{
			// metal to upper-right of test area
			double pt1X = metBound.getMinX();   double pt1Y = bound.getMaxY();
			double pt2X = bound.getMaxX();      double pt2Y = metBound.getMinY();
			if (Math.sqrt((pt1X-pt2X)*(pt1X-pt2X) + (pt1Y-pt2Y)*(pt1Y-pt2Y)) > dist) return false;
			if (pointInRTree(rtree, pt1X, pt1Y, netID, recsOnPath)) return false;
			if (pointInRTree(rtree, pt2X, pt2Y, netID, recsOnPath)) return false;
			return true;
		}
		if (metBound.getMaxX() < bound.getMinX() && metBound.getMinY() > bound.getMaxY())
		{
			// metal to upper-left of test area
			double pt1X = metBound.getMaxX();   double pt1Y = bound.getMaxY();
			double pt2X = bound.getMinX();      double pt2Y = metBound.getMinY();
			if (Math.sqrt((pt1X-pt2X)*(pt1X-pt2X) + (pt1Y-pt2Y)*(pt1Y-pt2Y)) > dist) return false;
			if (pointInRTree(rtree, pt1X, pt1Y, netID, recsOnPath)) return false;
			if (pointInRTree(rtree, pt2X, pt2Y, netID, recsOnPath)) return false;
			return true;
		}
		if (metBound.getMaxX() < bound.getMinX() && metBound.getMaxY() < bound.getMinY())
		{
			// metal to lower-left of test area
			double pt1X = metBound.getMaxX();   double pt1Y = bound.getMinY();
			double pt2X = bound.getMinX();      double pt2Y = metBound.getMaxY();
			if (Math.sqrt((pt1X-pt2X)*(pt1X-pt2X) + (pt1Y-pt2Y)*(pt1Y-pt2Y)) > dist) return false;
			if (pointInRTree(rtree, pt1X, pt1Y, netID, recsOnPath)) return false;
			if (pointInRTree(rtree, pt2X, pt2Y, netID, recsOnPath)) return false;
			return true;
		}
		if (metBound.getMinX() > bound.getMaxX() && metBound.getMaxY() < bound.getMinY())
		{
			// metal to lower-right of test area
			double pt1X = metBound.getMinX();   double pt1Y = bound.getMinY();
			double pt2X = bound.getMaxX();      double pt2Y = metBound.getMaxY();
			if (Math.sqrt((pt1X-pt2X)*(pt1X-pt2X) + (pt1Y-pt2Y)*(pt1Y-pt2Y)) > dist) return false;
			if (pointInRTree(rtree, pt1X, pt1Y, netID, recsOnPath)) return false;
			if (pointInRTree(rtree, pt2X, pt2Y, netID, recsOnPath)) return false;
			return true;
		}
		return false;
	}

	private boolean pointInRTree(RTNode rtree, double x, double y, int netID, List<Rectangle2D> recsOnPath)
	{
		Rectangle2D searchArea = new Rectangle2D.Double(x, y, 0, 0);
		for(RTNode.Search sea = new RTNode.Search(searchArea, rtree, true); sea.hasNext(); )
		{
			SOGBound sBound = (SOGBound)sea.next();
			if (sBound.netID != netID) continue;
			if (sBound.bound.getMinX() > x || sBound.bound.getMaxX() < x ||
				sBound.bound.getMinY() > y || sBound.bound.getMaxY() < y) continue;
			return true;
		}

		// now see if it is on the path
		for(Rectangle2D bound : recsOnPath)
		{
			if (bound.getMinX() > x || bound.getMaxX() < x ||
				bound.getMinY() > y || bound.getMaxY() < y) continue;
			return true;
		}
		return false;
	}

	/**
	 * Method to see if a proposed piece of metal has DRC errors (ignoring notches).
	 * @param netID the network ID of the desired metal (blockages on this netID are ignored).
	 * @param metNo the level of the metal.
	 * @param halfWidth half of the width of the metal.
	 * @param halfHeight half of the height of the metal.
	 * @param surround is the maximum possible DRC surround around the metal.
	 * @param x the X coordinate at the center of the metal.
	 * @param y the Y coordinate at the center of the metal.
	 * @return a blocking SOGBound object that is in the area.
	 * Returns null if the area is clear.
	 */
	private SOGBound getMetalBlockage(int netID, int metNo, double halfWidth, double halfHeight, double surround, double x, double y)
	{
		// get the R-Tree data for the metal layer
		RoutingLayer layer = metalLayers[metNo];
		RTNode rtree = metalTrees.get(layer);
		if (rtree == null) return null;

		// compute the area to search
		double lX = x - halfWidth - surround, hX = x + halfWidth + surround;
		double lY = y - halfHeight - surround, hY = y + halfHeight + surround;
		Rectangle2D searchArea = new Rectangle2D.Double(lX, lY, hX-lX, hY-lY);

		// see if there is anything in that area
		for(RTNode.Search sea = new RTNode.Search(searchArea, rtree, true); sea.hasNext(); )
		{
			SOGBound sBound = (SOGBound)sea.next();
			Rectangle2D bound = sBound.getBounds();
			if (DBMath.isLessThanOrEqualTo(bound.getMaxX(), lX) || DBMath.isGreaterThanOrEqualTo(bound.getMinX(), hX) ||
				DBMath.isLessThanOrEqualTo(bound.getMaxY(), lY) || DBMath.isGreaterThanOrEqualTo(bound.getMinY(), hY))
					continue;

			// ignore if on the same net
			if (Math.abs(sBound.getNetID()) == netID) continue;

			return sBound;
		}
		return null;
	}

	/**
	 * Method to find a via blockage in the R-Tree.
	 * @param netID the network ID of the desired space (vias at this point and on this netID are ignored).
	 * @param layer the via layer being examined.
	 * @param halfWidth half of the width of the area to examine.
	 * @param halfHeight half of the height of the area to examine.
	 * @param x the X coordinate at the center of the area to examine.
	 * @param y the Y coordinate at the center of the area to examine.
	 * @return a blocking SOGVia object that is in the area.
	 * Returns null if the area is clear.
	 */
	private SOGVia getViaBlockage(int netID, RoutingLayer layer, double halfWidth, double halfHeight, double x, double y)
	{
		RTNode rtree = viaTrees.get(layer);
		if (rtree == null) return null;

		// see if there is anything in that area
		Rectangle2D searchArea = new Rectangle2D.Double(x-halfWidth, y-halfHeight, halfWidth*2, halfHeight*2);
		for(RTNode.Search sea = new RTNode.Search(searchArea, rtree, true); sea.hasNext(); )
		{
			SOGVia sLoc = (SOGVia)sea.next();
			if (sLoc.getNetID() == netID)
			{
				if (sLoc.loc.getX() == x && sLoc.loc.getY() == y) continue;
			}
			return sLoc;
		}
		return null;
	}

	/**
	 * Method to add geometry to the R-Tree.
	 * @param poly the polygon to add (only rectangles are added, so the bounds is used).
	 * @param trans a transformation matrix to apply to the polygon.
	 * @param netID the global network ID of the geometry.
	 * @param canPlacePseudo true if pseudo-layers should be considered (converted to nonpseudo and stored).
	 * False to ignore pseudo-layers.
	 */
	private void addLayer(RoutingLayer layer, Rectangle2D box, AffineTransform trans, int netID, boolean canPlacePseudo)
	{
		if (layer.isMetal())
		{
			addRectangle(box, layer, netID);
		} else
		{
			addVia(new Point2D.Double(box.getCenterX(), box.getCenterY()), layer, netID);
		}
	}

	/**
	 * Method to add a rectangle to the metal R-Tree.
	 * @param bounds the rectangle to add.
	 * @param layer the metal layer on which to add the rectangle.
	 * @param netID the global network ID of the geometry.
	 */
	private void addRectangle(Rectangle2D bounds, RoutingLayer layer, int netID)
	{
		RTNode root = metalTrees.get(layer);
		if (root == null)
		{
			root = RTNode.makeTopLevel();
			metalTrees.put(layer, root);
		}
		RTNode newRoot = RTNode.linkGeom(null, root, new SOGBound(bounds, netID));
		if (newRoot != root) metalTrees.put(layer, newRoot);
	}

	/**
	 * Method to add a point to the via R-Tree.
	 * @param loc the point to add.
	 * @param layer the via layer on which to add the point.
	 * @param netID the global network ID of the geometry.
	 */
	private void addVia(Point2D loc, RoutingLayer layer, int netID)
	{
		RTNode root = viaTrees.get(layer);
		if (root == null)
		{
			root = RTNode.makeTopLevel();
			viaTrees.put(layer, root);
		}
		RTNode newRoot = RTNode.linkGeom(null, root, new SOGVia(loc, netID));
		if (newRoot != root) viaTrees.put(layer, newRoot);
	}

	/**
	 * Class to define a list of possible nodes that can connect two layers.
	 * This includes orientation
	 */
	private static class MetalVia
	{
		RoutingContact via;
		int orientation;

		MetalVia(RoutingContact v, int o) { via = v;   orientation = o; }
	}

	/**
	 * Class to define a list of possible nodes that can connect two layers.
	 * This includes orientation
	 */
	private static class MetalVias
	{
		List<MetalVia> vias = new ArrayList<MetalVia>();

		void addVia(RoutingContact pn, int o)
		{
			vias.add(new MetalVia(pn, o));
			Collections.sort(vias, new PrimsBySize());
		}

		List<MetalVia> getVias() { return vias; }
	}

	/**
	 * Comparator class for sorting primitives by their size.
	 */
	private static class PrimsBySize implements Comparator<MetalVia>
	{
		/**
		 * Method to sort primitives by their size.
		 */
		public int compare(MetalVia mv1, MetalVia mv2)
		{
			RoutingContact pn1 = mv1.via;
			RoutingContact pn2 = mv2.via;
			double sz1 = pn1.getDefWidth() * pn1.getDefHeight();
			double sz2 = pn2.getDefWidth() * pn2.getDefHeight();
			if (sz1 < sz2) return -1;
			if (sz1 > sz2) return 1;
			return 0;
		}
	}

	/************************************** DIJKSTRA PATH SEARCHING **************************************/

	/**
	 * Class to define a vertex in the Dijkstra search.
	 */
	private class SearchVertex implements Comparable<SearchVertex>
	{
		/** the coordinate of the search vertex. */	private double xv, yv;
		/** the layer of the search vertex. */		private int zv;
		/** the cost of search to this vertex. */	private int cost;
		/** the layer of cuts in "cuts". */			private int cutLayer;
		/** the cuts in the contact. */				private Point2D [] cuts;
		/** the previous vertex in the search. */	private SearchVertex last;
		/** the routing state. */					private Wavefront w;

		/**
		 * Method to create a new SearchVertex.
		 * @param x the X coordinate of the SearchVertex.
		 * @param y the Y coordinate of the SearchVertex.
		 * @param z the Z coordinate (metal layer) of the SearchVertex.
		 * @param whichContact the contact number to use if switching layers.
		 * @param cuts an array of cuts in this contact (if switching layers).
		 * @param cl the layer of the cut (if switching layers).
		 * @param nr the NeededRoute that this SearchVertex is part of.
		 */
		SearchVertex(double x, double y, int z, int whichContact, Point2D [] cuts, int cl, Wavefront w)
		{
			xv = x;
			yv = y;
			zv = (z<<8) + (whichContact & 0xFF);
			this.cuts = cuts;
			cutLayer = cl;
			this.w = w;
		}

		double getX() { return xv; }

		double getY() { return yv; }

		int getZ() { return zv >> 8; }

		int getContactNo() { return zv & 0xFF; }

		Point2D [] getCuts() { return cuts; }

		void clearCuts() { cuts = null; }

		int getCutLayer() { return cutLayer; }

		/**
		 * Method to sort SearchVertex objects by their cost.
		 */
		public int compareTo(SearchVertex sv)
		{
			int diff = cost - sv.cost;
			if (diff != 0) return diff;
			if (w != null)
			{
				double thisDist = Math.abs(xv-w.toX) + Math.abs(yv-w.toY) + Math.abs(zv-w.toZ);
				double otherDist = Math.abs(sv.xv-w.toX) + Math.abs(sv.yv-w.toY) + Math.abs(sv.zv-w.toZ);
				if (thisDist < otherDist) return -1;
				if (thisDist > otherDist) return 1;
			}
			return 0;
		}
	}

//	private void showSearchVertices(Map<Integer,Set<Integer>> [] planes, boolean horiz)
//	{
//		EditWindow_ wnd = Job.getUserInterface().getCurrentEditWindow_();
//		for(int i=0; i<numMetalLayers; i++)
//		{
//			double offset = i;
//			offset -= (numMetalLayers-2) / 2.0;
//			offset /= numMetalLayers+2;
//			Map<Integer,Set<Integer>> plane = planes[i];
//			if (plane == null) continue;
//			for(Integer y : plane.keySet())
//			{
//				double yv = y.doubleValue();
//				Set<Integer> row = plane.get(y);
//				for(Iterator<Integer> it = row.iterator(); it.hasNext(); )
//				{
//					Integer x = it.next();
//					double xv = x.doubleValue();
//					Point2D pt1, pt2;
//					if (horiz)
//					{
//						pt1 = new Point2D.Double(xv-0.5, yv+offset);
//						pt2 = new Point2D.Double(xv+0.5, yv+offset);
//					} else
//					{
//						pt1 = new Point2D.Double(xv+offset, yv-0.5);
//						pt2 = new Point2D.Double(xv+offset, yv+0.5);
//					}
//					wnd.addHighlightLine(pt1, pt2, cell, false, false);
//				}
//			}
//		}
//	}
}
