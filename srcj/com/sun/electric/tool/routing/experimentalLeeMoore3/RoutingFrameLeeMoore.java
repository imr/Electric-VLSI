/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: RoutingFrameLeeMoore.java
 * Written by: Dennis Appelt, Sven Janko (Team 2)
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
package com.sun.electric.tool.routing.experimentalLeeMoore3;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.topology.RTBounds;
import com.sun.electric.database.topology.RTNode;
import com.sun.electric.tool.Job;
import com.sun.electric.util.TextUtils;
import com.sun.electric.util.math.DBMath;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Class to do the Lee-Moore routing inside the RoutingFrame.
 */
public class RoutingFrameLeeMoore extends BenchmarkRouter {
	private static final boolean DEBUG = false;
	// route segments with the same net id in parallel
	private static final boolean PARALLEL = true;

	// this option is still very experimental an will not find correct routes
	static final boolean GLOBALDETAILEDROUTING = false;

	/** The cell in which all the routing takes place. */
	private Cell cell;
	private Rectangle2D cellBounds;

	/** List of all metal layers on which the RoutingWires will be placed. */
	private RoutingLayer[] metalLayers;
	private int numMetalLayers;
	private double globalGridGranularity;
	// private double globalGridStepsize;
	private double detailedGridGranularity;
	private Wavefront dummyDetailedWavefront;

	/** vias to use to go up from each metal layer. */
	private MetalVias[] metalVias;
	private Map<RoutingLayer, RTNode> metalBlockages;
	private Map<RoutingLayer, RTNode> viaBlockages;

	/** Rating function used for influencing the quality of the routing */
	RatingFunction[] ratingFunctionsForDetailed = {
			new DistanceRatingFunction(),
			// new MinimalCrossingRatingFunction(),
			// new ShiftInDirectionRatingFunction(),
			new OutOfBoundsRatingFunction(),
			new ForcedDirectionRatingFunction() };

	RatingFunction[] ratingFunctionsForGlobal = {
			new DistanceRatingFunction(),
			// new MinimalCrossingRatingFunction(),
			// new ShiftInDirectionRatingFunction(),
			new OutOfBoundsRatingFunction(),
			new ForcedDirectionRatingFunction() };

	/** used for parallelisation **/
	// private static Object lockObj = new Object();
	private ThreadPoolExecutor workerThreadsExecutor;
	private ArrayBlockingQueue<Runnable> inQueue;
	private ArrayBlockingQueue<Wavefront> outQueue;

	/**
	 * Method to return the name of this routing algorithm.
	 * 
	 * @return the name of this routing algorithm.
	 */
	public String getAlgorithmName() {
		return "Lee/Moore - 3";
	}

	/**
	 * Method to do LeeMoore routing.
	 */
	protected void runRouting(Cell cell, List<RoutingSegment> segmentsToRoute,
			List<RoutingLayer> allLayers, List<RoutingContact> allContacts,
			List<RoutingGeometry> blockages) {

		// set default timeout
		if (maxRuntime.getTempIntValue() <= 0) {
			maxRuntime.setTempIntValue(800);
		}

		// stop the time so we can abort the routing if we exceed
		// <code>this.secTimeout</code> seconds
		long timeStart = System.currentTimeMillis();

		// create Workerthreadpool
		// Set max. number of threads according to the parameters
		int maxPoolSize = numThreads.getTempIntValue();
		maxPoolSize = maxPoolSize > 0 ? maxPoolSize : 4;
		int poolSize = maxPoolSize > 1 ? maxPoolSize / 2 : 1;
		long keepAliveTime = 10;
		inQueue = new ArrayBlockingQueue<Runnable>(40);
		outQueue = new ArrayBlockingQueue<Wavefront>(40);
		workerThreadsExecutor = new ThreadPoolExecutor(poolSize, maxPoolSize,
				keepAliveTime, TimeUnit.SECONDS, inQueue);

		Job.getUserInterface().startProgressDialog(
				"Collect startup information", null);
		Job.getUserInterface().setProgressNote("Initialize design roules");
		// initialize information about the technology
		if (initializeDesignRules(allLayers, allContacts))
			return;

		Job.getUserInterface().setProgressNote("Set up data structures");
		// Job.getUserInterface().setProgressValue(0);

		this.cell = cell;
		this.cellBounds = this.cell.getBounds();

		double x = cellBounds.getMaxX() - cellBounds.getMinX();
		double y = cellBounds.getMaxY() - cellBounds.getMinY();
		globalGridGranularity = 1 / (((x < y) ? x : y) / 32);
		// globalGridStepsize = 1 / globalGridGranularity;
		detailedGridGranularity = globalGridGranularity * 32;
		dummyDetailedWavefront = new Wavefront(segmentsToRoute.get(0),
				detailedGridGranularity, cellBounds,
				ratingFunctionsForDetailed, false);

		this.metalBlockages = new HashMap<RoutingLayer, RTNode>();
		this.viaBlockages = new HashMap<RoutingLayer, RTNode>();

		for (RoutingGeometry rg : blockages) {
			RoutingLayer l = rg.getLayer();
			Rectangle2D rect = rg.getBounds();
			if (l.isMetal())
				addRectangle(rect, l, rg.getNetID());
			else
				addVia(
						new Point2D.Double(rect.getCenterX(), rect.getCenterY()),
						l, rg.getNetID());
		}
		addBlockagesAtPorts(segmentsToRoute);

		Job.getUserInterface().stopProgressDialog();
		Job.getUserInterface().startProgressDialog("Routing segments", null);

		int numSegments = segmentsToRoute.size();
		int prevNetId = (numSegments > 0) ? segmentsToRoute.get(0).getNetID()
				: -1;
		for (int i = 0; i < numSegments; i++) {

			Job.getUserInterface().setProgressValue(
					(int) (((double) i / (double) numSegments) * 100));
			Job.getUserInterface().setProgressNote(
					"Routing segment " + (i + 1) + " of " + numSegments + ".");

			RoutingSegment currentSegment = segmentsToRoute.get(i);
			if (PARALLEL) {

				try {
					// if the current segment is from a different net id as the
					// last segment,
					// finish all previous wavefronts and do the backtracking.
					// Otherwise
					// the blockage information wouldn't be consistent.
					if (prevNetId != currentSegment.getNetID()) {
						while (outQueue.size() != 0
								|| workerThreadsExecutor.getQueue().size() != 0) {
							dequeueWavefrontJob();
						}
					}

					// the capacity of the job queue's is full, take some jobs
					// from the out queue
					if (workerThreadsExecutor.getQueue().size() > 39
							|| outQueue.size() > 30) {
						dequeueWavefrontJob();
					}

					if (GLOBALDETAILEDROUTING) {
						Wavefront[] detailedWavefronts = doGlobalRouting(currentSegment);
						for (Wavefront w : detailedWavefronts) {
							enqueueWavefrontJob(w);
						}
					} else {
						Wavefront w = new Wavefront(currentSegment, 0.2,
								cellBounds, ratingFunctionsForDetailed, false);
						enqueueWavefrontJob(w);
					}
				} catch (CouldNotCalculateVirtualTerminalException e2) {
					// computation of detailed segments not possible, try to
					// route
					// the global segment
					if (enableOutput.getTempBooleanValue())
						System.out.print(i
								+ ": (unable to calculate virtual Terminals) ");

					Wavefront w = new Wavefront(currentSegment, 0.2,
							cellBounds, ratingFunctionsForDetailed, false);
					enqueueWavefrontJob(w);
				} catch (RatingNotFoundException e) {
					// caused likely because the global backtracking didn't find
					// a rating
					// for a already visited point
					if (enableOutput.getTempBooleanValue())
						System.out
								.print(i
										+ ": (global backtracking did not find rating) ");

					Wavefront w = new Wavefront(currentSegment, 0.2,
							cellBounds, ratingFunctionsForDetailed, false);
					enqueueWavefrontJob(w);
				}

				prevNetId = currentSegment.getNetID();
			} else {
				// not parallel
				if (enableOutput.getTempBooleanValue())
					System.out.print(i + ": ");
				Wavefront w = new Wavefront(currentSegment, 0.2, cellBounds,
						ratingFunctionsForDetailed, DEBUG);
				w.propagate();

				if (w.foundRoute)
					new Backtracking(w).routeIt();
			}

			if ((System.currentTimeMillis() - timeStart) / 1000 > maxRuntime
					.getTempIntValue()) {
				if (enableOutput.getTempBooleanValue())
					System.out
							.println("Routing aborted. Maximum execution time ("
									+ maxRuntime.getTempIntValue()
									+ " seconds) reached.");
				break;
			}
		}
		// wait until all running wavefront jobs are finished
		while (outQueue.size() != 0
				|| workerThreadsExecutor.getQueue().size() != 0) {
			dequeueWavefrontJob();
		}

		Job.getUserInterface().stopProgressDialog();
	}

	private void enqueueWavefrontJob(Wavefront w) {
		boolean submitToWorkpool = false;
		do {
			try {
				workerThreadsExecutor.execute(new WavefrontExecutor(w));
				submitToWorkpool = true;
				// w.propagate();
			}
			// work pool for detailed routing is full
			catch (RejectedExecutionException e) {
				Wavefront doneWavefront;
				try {
					doneWavefront = outQueue.take();
					if (doneWavefront.foundRoute)
						new Backtracking(doneWavefront).routeIt();
				} catch (InterruptedException e1) {
					if (enableOutput.getTempBooleanValue())
						e1.printStackTrace();
				}
			}
		} while (!submitToWorkpool);
	}

	private void dequeueWavefrontJob() {
		Wavefront w;

		try {
			w = outQueue.take();
			if (w.foundRoute)
				new Backtracking(w).routeIt();
		} catch (InterruptedException e) {
			if (enableOutput.getTempBooleanValue())
				e.printStackTrace();
		}
	}

	public Wavefront[] doGlobalRouting(RoutingSegment globalSegment)
			throws CouldNotCalculateVirtualTerminalException,
			RatingNotFoundException {

		Wavefront globalWF = new Wavefront(globalSegment,
				globalGridGranularity, cellBounds, ratingFunctionsForGlobal,
				false, true);

		globalWF.propagate();

		ExperimentalGlobalBacktracking backtracking = new ExperimentalGlobalBacktracking(
				globalWF);
		List<ExperimentalGlobalBacktracking.Point> globalSections = backtracking
				.routeIt();

		ArrayList<Rectangle2D> detailedSegmentBounds = new ArrayList<Rectangle2D>();

		for (ExperimentalGlobalBacktracking.Point p : globalSections)
			detailedSegmentBounds.add(getBoundsForGridPoint(p, globalWF));

		Wavefront[] detailedWavefronts = calcVirtualTerminals(
				detailedSegmentBounds.toArray(new Rectangle2D[0]),
				globalSegment, backtracking, globalWF);

		return detailedWavefronts;
	}

	public Rectangle2D getBoundsForGridPoint(
			ExperimentalGlobalBacktracking.Point p, Wavefront wf) {
		double segmentSize = ((cellBounds.getMaxY() - cellBounds.getMinY()) / 32);
		double diffToBorder = ((cellBounds.getMaxY() - cellBounds.getMinY()) / 32) / 2;

		double min = 0.00000001;
		double sizeOfRect = ((long) ((segmentSize - min) * 100000000)) / 100000000.0;

		Rectangle2D rect = new Rectangle2D.Double(wf.toGrid(p.x) - diffToBorder
				+ min, wf.toGrid(p.y) - diffToBorder + min, sizeOfRect,
				sizeOfRect);

		return rect;
	}

	public Rectangle2D getBoundsForGridPoint(Point2D p, Wavefront wf) {

		double segmentSize = ((cellBounds.getMaxY() - cellBounds.getMinY()) / 32);
		double diffToBorder = ((cellBounds.getMaxY() - cellBounds.getMinY()) / 32) / 2;

		double min = 0.00000001;
		double sizeOfRect = ((long) ((segmentSize - min) * 100000000)) / 100000000.0;

		Rectangle2D rect = new Rectangle2D.Double(wf.toGrid(p.getX())
				- diffToBorder + min, wf.toGrid(p.getY()) - diffToBorder + min,
				sizeOfRect, sizeOfRect);

		return rect;
	}

	public Wavefront[] calcVirtualTerminals(
			Rectangle2D[] detailedSegmentBounds, RoutingSegment globalSegment,
			ExperimentalGlobalBacktracking dummy, Wavefront dummyWF)
			throws CouldNotCalculateVirtualTerminalException {
		Wavefront[] detailedSegments = new Wavefront[detailedSegmentBounds.length];

		Point2D startPoint = null, finishPoint = null, nextStartPoint = null;

		startPoint = globalSegment.getFinishEnd().getLocation();

		for (int i = 0; i + 1 < detailedSegmentBounds.length; i++) {
			boolean blockage = false;
			double rnd = Math.random();

			for (int j = 0; j < 5; j++) {
				double tmp = (rnd + (0.2 * j)) / 1.0;
				// calculate the X- and Y-coordinate for the virtual terminals
				Point2D[] points = calcPointsForVirtualTerminals(
						detailedSegmentBounds[i], detailedSegmentBounds[i + 1],
						tmp);
				finishPoint = points[0];
				nextStartPoint = points[1];

				// check if the virtual terminal is located within a blockage
				double minWidth = globalSegment.getWidestArcAtStart();
				RoutingLayer currentLayer = globalSegment.getStartLayers().get(
						0);
				double surround = currentLayer.getMinSpacing(currentLayer);
				double metalSpacing = Math.max(currentLayer.getMinWidth(),
						minWidth) / 2;

				blockage = false;
				// if the calculated virtual terminals are within blockages,
				// calculate new
				// virtual terminals
				for (Point2D p : points) {
					LMBound block = getMetalBlockage(globalSegment.getNetID(),
							currentLayer.getMetalNumber(), metalSpacing,
							metalSpacing, surround, p.getX(), p.getY());
					if (block != null) {
						if (j == 4)
							throw new CouldNotCalculateVirtualTerminalException();

						blockage = true;
						break;
					}
				}
				// found a valid virtual terminals, stop the searching
				if (!blockage)
					break;
			}

			detailedSegments[i] = new Wavefront(globalSegment, startPoint,
					finishPoint, globalSegment.getStartLayers().get(0),
					globalSegment.getStartLayers().get(0),
					detailedGridGranularity, detailedSegmentBounds[i],
					ratingFunctionsForDetailed, false, false);

			startPoint = nextStartPoint;
		}

		// add the wavefront which contains the finish end
		finishPoint = globalSegment.getStartEnd().getLocation();
		detailedSegments[detailedSegmentBounds.length - 1] = new Wavefront(
				globalSegment, startPoint, finishPoint, globalSegment
						.getStartLayers().get(0), globalSegment
						.getFinishLayers().get(0), detailedGridGranularity,
				detailedSegmentBounds[detailedSegmentBounds.length - 1],
				ratingFunctionsForDetailed, false, false);

		return detailedSegments;
	}

	private Point2D[] calcPointsForVirtualTerminals(
			Rectangle2D firstSementBounds, Rectangle2D secondSementBounds,
			double randomDouble) {
		double crossingXforp1 = 0, crossingXforp2 = 0;
		double crossingYforp1 = 0, crossingYforp2 = 0;

		Point2D finishPoint = null, nextStartPoint = null;

		if (firstSementBounds.getMinX() == secondSementBounds.getMinX()) {

			boolean firstSmaller = firstSementBounds.getMaxY() < secondSementBounds
					.getMaxY();

			if (firstSmaller) {
				crossingYforp1 = firstSementBounds.getMaxY();
				crossingYforp2 = secondSementBounds.getMinY();
			} else {
				crossingYforp1 = firstSementBounds.getMinY();
				crossingYforp2 = secondSementBounds.getMaxY();
			}

			// limit resolution of the random xCoord, otherwise there may be
			// rounding errors if transforming the xCoord to a grid point
			double xCoord = ((long) ((firstSementBounds.getMaxX() - firstSementBounds
					.getMinX())
					* randomDouble * 100000000)) / 100000000.0;
			xCoord += firstSementBounds.getMinX();

			xCoord = dummyDetailedWavefront.toGrid(xCoord);
			crossingYforp1 = dummyDetailedWavefront.toGrid(crossingYforp1);
			crossingYforp2 = dummyDetailedWavefront.toGrid(crossingYforp2);

			finishPoint = new Point2D.Double(xCoord, crossingYforp1);
			nextStartPoint = new Point2D.Double(xCoord, crossingYforp2);
		} else {
			boolean firstSmaller = firstSementBounds.getMaxX() < secondSementBounds
					.getMaxX();

			if (firstSmaller) {
				crossingXforp1 = firstSementBounds.getMaxX();
				crossingXforp2 = secondSementBounds.getMinX();
			} else {
				crossingXforp1 = firstSementBounds.getMinX();
				crossingXforp2 = secondSementBounds.getMaxX();
			}

			// limit resolution of the random yCoord, otherwise there may be
			// rounding errors if transforming the yCoord to a grid point
			double yCoord = ((long) ((firstSementBounds.getMaxY() - firstSementBounds
					.getMinY())
					* randomDouble * 100000000)) / 100000000.0;
			yCoord += firstSementBounds.getMinY();

			yCoord = dummyDetailedWavefront.toGrid(yCoord);
			crossingYforp1 = dummyDetailedWavefront.toGrid(crossingXforp1);
			crossingYforp2 = dummyDetailedWavefront.toGrid(crossingXforp2);

			finishPoint = new Point2D.Double(crossingXforp1, yCoord);
			nextStartPoint = new Point2D.Double(crossingXforp2, yCoord);
		}

		return new Point2D[] { finishPoint, nextStartPoint };
	}

	/**
	 * Class to find a route on a segment.
	 */
	private class Wavefront {

		private boolean debug;
		RoutingSegment segment;
		private RoutingLayer rlStart, rlFinish;
		private double xStart, yStart;
		/** These are the coords of the grid point next to the start point. */
		double xGridStart, yGridStart;
		int zStart;
		double xFinish, yFinish;
		/** These are the coords of the grid point next to the finish point. */
		double xGridFinish, yGridFinish;
		int zFinish;
		private String netName;
		private int netID;

		RatingFunction[] ratingFunctions;

		// variables related to the grid
		private double granularity;
		private double stepsize;
		private Rectangle2D bounds;

		boolean foundRoute;
		boolean runInGlobalMode;

		// the number of points the wavefront will visit (at most)
		private int maxPointsLimit;
		DataGrid[] grid;

		/**
		 * The first point in this queue is the next that will be visited by the
		 * wavefront.<br>
		 * Sort criterion is the ranking of the grid point. The higher the
		 * ranking of a point the faster that point will be visited.
		 */
		PriorityQueue<Gridpoint> pointsToVisit;

		Wavefront(RoutingSegment s, double granularity, Rectangle2D bounds,
				RatingFunction[] ratingFunctions, boolean debug) {

			// set start and finish layer
			this.rlStart = s.getStartLayers().get(0);
			this.rlFinish = s.getFinishLayers().get(0);

			// set start and finish coordinates
			this.xStart = s.getStartEnd().getLocation().getX();
			this.yStart = s.getStartEnd().getLocation().getY();
			this.xFinish = s.getFinishEnd().getLocation().getX();
			this.yFinish = s.getFinishEnd().getLocation().getY();

			// init everything else
			init(s, granularity, bounds, ratingFunctions, debug, false);

			// printInfo();
		}

		Wavefront(RoutingSegment s, Point2D startPoint, Point2D finishPoint,
				RoutingLayer startLayer, RoutingLayer finishLayer,
				double granularity, Rectangle2D bounds,
				RatingFunction[] ratingFunctions, boolean debug,
				boolean runInGlobalMode) {

			// set start and finish layer
			this.rlStart = startLayer;
			this.rlFinish = finishLayer;

			// set start and end point
			this.xStart = startPoint.getX();
			this.yStart = startPoint.getY();
			this.xFinish = finishPoint.getX();
			this.yFinish = finishPoint.getY();

			// init everything else
			init(s, granularity, bounds, ratingFunctions, debug,
					runInGlobalMode);
			// printInfo();
		}

		Wavefront(RoutingSegment s, double granularity, Rectangle2D bounds,
				RatingFunction[] ratingFunctions, boolean debug,
				boolean runInGlobalMode) {

			// set start and finish layer
			this.rlStart = s.getStartLayers().get(0);
			this.rlFinish = s.getFinishLayers().get(0);

			// set start and finish coordinates
			this.xStart = s.getStartEnd().getLocation().getX();
			this.yStart = s.getStartEnd().getLocation().getY();
			this.xFinish = s.getFinishEnd().getLocation().getX();
			this.yFinish = s.getFinishEnd().getLocation().getY();

			// init everything else
			init(s, granularity, bounds, ratingFunctions, debug,
					runInGlobalMode);
			// printInfo();
		}

		// helper method for constructor
		private void init(RoutingSegment s, double granularity,
				Rectangle2D bounds, RatingFunction[] ratingFunctions,
				boolean debug, boolean runInGlobalMode) {
			// set grid size
			this.granularity = granularity;
			stepsize = 1 / granularity;
			this.bounds = bounds;

			// default is detailed mode
			this.runInGlobalMode = runInGlobalMode;

			this.ratingFunctions = ratingFunctions;
			this.segment = s;
			this.debug = debug;
			this.netName = this.segment.getNetName();
			this.netID = this.segment.getNetID();
			this.foundRoute = false;

			// set grid start and finish coordinates
			this.xGridStart = toGrid(this.xStart);
			this.yGridStart = toGrid(this.yStart);
			this.xGridFinish = toGrid(this.xFinish);
			this.yGridFinish = toGrid(this.yFinish);
			this.zStart = rlStart.getMetalNumber() - 1;
			this.zFinish = rlFinish.getMetalNumber() - 1;

			// if the Start-/End Points are virtual Terminals, transform the
			// Start-/Endpoints to grid points
			if (GLOBALDETAILEDROUTING) {
				Point2D startPoint = segment.getStartEnd().getLocation();
				if (!this.runInGlobalMode && xFinish != startPoint.getX()
						&& yFinish != startPoint.getY()) {
					xFinish = toGrid(xFinish);
					yFinish = toGrid(yFinish);
				}
				Point2D finishPoint = segment.getFinishEnd().getLocation();
				if (!this.runInGlobalMode && xStart != finishPoint.getX()
						&& yStart != finishPoint.getY()) {
					xStart = toGrid(xStart);
					yStart = toGrid(yStart);
				}
			}

			// generate a DataGrid for each metal layer
			this.grid = new DataGrid[numMetalLayers];
			for (int i = 0; i < numMetalLayers; i++) {
				this.grid[i] = new DataGrid();
			}

			this.pointsToVisit = new PriorityQueue<Gridpoint>();
			// add the start point to the list
			this.pointsToVisit.add(new Gridpoint(this.xGridStart,
					this.yGridStart, this.zStart, null));
			this.grid[this.zStart].visit(this.xGridStart, this.yGridStart,
					new Rating());

			double tmp = DBMath.distBetweenPoints(this.segment.getStartEnd()
					.getLocation(), this.segment.getFinishEnd().getLocation());
			this.maxPointsLimit = (int) (tmp * this.granularity * 1800);
		}

		// helper method for constructor
		private void printInfo() {
			if (enableOutput.getTempBooleanValue()) {
				System.out.println((runInGlobalMode ? "global" : "detailed")
						+ " Wavefront \"" + this.netName + "\" started at ("
						+ TextUtils.formatDouble(this.xStart) + ", "
						+ TextUtils.formatDouble(this.yStart) + ", "
						+ this.zStart + ") heading for ("
						+ TextUtils.formatDouble(this.xFinish) + ", "
						+ TextUtils.formatDouble(this.yFinish) + ", "
						+ this.zFinish + ")" + " - NetID=" + this.netID + ", "
						+ "Thread ID: " + Thread.currentThread().getId());
			}
		}

		private void propagate() {
			printInfo();

			double x = this.xStart, y = this.yStart, nextX, nextY;
			int z = this.zStart, nextZ;

			RTNode blockages = metalBlockages.get(this.rlStart);

			// check if the finish point can be reached
			double minWidth = metalLayers[this.zFinish].getMinWidth();
			double minSpacing = metalLayers[this.zFinish]
					.getMinSpacing(metalLayers[this.zFinish]);
			double halfWidth = minWidth / 2;

			LMBound block = getMetalBlockage(netID, this.zFinish, halfWidth,
					halfWidth, minSpacing, this.xGridFinish, this.yGridFinish);
			if (block != null) {
				if (enableOutput.getTempBooleanValue())
					System.out
							.println("There is a blockage at the finish point. Wavefront will not start.");
				return;
			}

			Gridpoint curPoint = null;
			// count the number of visited grid points
			int counter = 0;
			while ((curPoint = this.pointsToVisit.poll()) != null) {
				counter++;

				if (counter > this.maxPointsLimit) {
					if (enableOutput.getTempBooleanValue())
						System.out
								.println("Wavefront did not find finish point (maximum number of visited points ("
										+ this.maxPointsLimit + ") reached).");
					return;

				}
				x = curPoint.getX();
				y = curPoint.getY();
				z = curPoint.getZ();
				if (this.debug) {
					this.segment.addWireEnd(new RoutePoint(metalLayers[z]
							.getPin(), new Point2D.Double(x, y), 0));
				}

				// see if the wavefront reached the finish point
				if (z == this.zFinish && x == this.xGridFinish
						&& y == this.yGridFinish) {

					this.foundRoute = true;
					if (enableOutput.getTempBooleanValue() && this.debug) {
						System.out.println("routing found");
						System.out.println("Visited " + counter
								+ " Grid Points.");
					}
					return;
				}

				// spread in all six directions
				for (int i = 0; i < 6; i++) {

					double dx = 0, dy = 0;
					int dz = 0;

					switch (i) {
					// 0-3: the 4 directions on the same layer
					case 0:
						dy = stepsize;
						break;
					case 1:
						dx = stepsize;
						break;
					case 2:
						dy = -stepsize;
						break;
					case 3:
						dx = -stepsize;
						break;
					// 4,5: change layer
					case 4:
						dz = -1;
						break;
					case 5:
						dz = 1;
						break;
					}
					nextX = toGrid(x + dx);
					nextY = toGrid(y + dy);
					nextZ = z + dz;
					if (nextZ < 0 || nextZ >= numMetalLayers
							|| this.grid[nextZ].isPointVisited(nextX, nextY))
						continue; // continue with next direction

					// if running in global Mode, ignore blockages
					if (!runInGlobalMode) {
						minWidth = metalLayers[nextZ].getMinWidth();
						minSpacing = metalLayers[nextZ]
								.getMinSpacing(metalLayers[nextZ]);

						// add some extra space (this.stepsize / 2)
						// to avoid spacing errors when we move wires
						// at start and finish ends
						// (see adjustment in backtracking)
						halfWidth = (minWidth + this.stepsize) / 2;

						block = getMetalBlockage(netID, nextZ, halfWidth,
								halfWidth, minSpacing, nextX, nextY);
						if (block != null) {
							continue;
						}
					}

					Gridpoint nextPoint = new Gridpoint(nextX, nextY, nextZ,
							curPoint);
					for (RatingFunction f : ratingFunctions) {
						f.doRating(nextPoint, curPoint, this.xFinish,
								this.yFinish, blockages, bounds);
					}
					nextPoint.getRating().calcRating();

					this.pointsToVisit.add(nextPoint);

					this.grid[nextZ].visit(nextX, nextY, nextPoint.getRating());
				}
			}
			if (enableOutput.getTempBooleanValue())
				System.out
						.println("Wavefront did not find finish point (stuck).");
		}

		public double getGranularity() {
			return granularity;
		}

		/**
		 * Method to round a value to the nearest point of our routing grid.
		 * 
		 * @param d
		 *            The value to round.
		 * @return The grid value.
		 */
		private double toGrid(double d) {
			return Math.round(d * granularity) * stepsize;
		}
	} // end class Wavefront

	/**
	 * Class to to the backtracking after the wavefront reached the finish
	 * point.
	 */
	private class Backtracking {
		private Wavefront w;
		private double granularity;
		private double stepsize;

		public Backtracking(Wavefront w) {
			this.w = w;
			granularity = w.getGranularity();
			stepsize = 1 / granularity;
		}

		private class Point {
			double x, y;
			int zLast, z;
			boolean isVia = false;

			Point(double x, double y, int zLast, int z) {
				this.x = x;
				this.y = y;
				this.zLast = zLast;
				this.z = z;
				this.isVia = true;
			}

			Point(double x, double y, int z) {
				this.x = x;
				this.y = y;
				this.z = z;
			}
		}

		public void routeIt() {
			List<Point> points = new ArrayList<Point>();

			double x = this.w.xGridFinish, y = this.w.yGridFinish;
			double xLast = x, yLast = y;
			double xNextToLast = xLast, yNextToLast = yLast;

			int z = w.zFinish, zLast = w.zFinish;

			// add finish route point
			points.add(new Point(this.w.xFinish, this.w.yFinish, z));

			int lastRating = 0;
			Rating tmp = w.grid[z].getRating(x, y);
			if (tmp != null)
				lastRating = tmp.getRating();
			else {
				if (enableOutput.getTempBooleanValue())
					System.out
							.println("Backtracking did not find a route (Nullpointer Rating)!");
				return;
			}

			Rating r = tmp;
			// loop until start point is reached
			while ((x != this.w.xGridStart) || (y != this.w.yGridStart)
					|| z != w.zStart) {

				// if there was a layer change
				// or if there was a shift in direction
				if (z != zLast) {
					points.add(new Point(x, y, zLast, z));
				} else if (x != xNextToLast && y != yNextToLast) {
					points.add(new Point(xLast, yLast, z));
				}

				for (int i = 0; i < 6; i++) {
					double dx = 0, dy = 0;
					int dz = 0;
					switch (i) {
					// 0-3: the 4 directions on the same layer
					case 0:
						dy = stepsize;
						break;
					case 1:
						dx = stepsize;
						break;
					case 2:
						dy = -stepsize;
						break;
					case 3:
						dx = -stepsize;
						break;
					// 4,5: change layer
					case 4:
						dz = -1;
						break;
					case 5:
						dz = 1;
						break;
					}

					// check bounds
					if (z + dz < 0 || z + dz >= numMetalLayers)
						continue;

					r = this.w.grid[z + dz].getRating(toGrid(x + dx), toGrid(y
							+ dy));
					if (r == null || r.getRating() > lastRating) {
						if (i == 5) {
							if (enableOutput.getTempBooleanValue())
								System.out
										.println("Backtracking did not find a route!");
							return;
						} else
							continue;
					}

					xNextToLast = xLast;
					xLast = x;
					yNextToLast = yLast;
					yLast = y;
					zLast = z;
					lastRating = r.getRating();
					x = toGrid(x + dx);
					y = toGrid(y + dy);
					z += dz;
					break;
				}
			}
			if (z != zLast) {
				points.add(new Point(this.w.xStart, this.w.yStart, zLast, z));
			} else {
				points.add(new Point(this.w.xStart, this.w.yStart, z));
			}

			int size = points.size();
			if (size == 2) {
				Point p1 = points.get(0);
				Point p2 = points.get(1);
				if (p1.x != p2.x && p1.y != p2.y) {
					points.add(1, new Point(p1.x, p2.y, p1.z));
				}
			} else if (size == 3) {
				Point p = points.get(1);
				if (Math.abs(p.x - this.w.xFinish) < Math.abs(p.y
						- this.w.yFinish)) {
					p.x = this.w.xFinish;
					p.y = this.w.yStart;
				} else {
					p.x = this.w.xStart;
					p.y = this.w.yFinish;
				}
			} else if (size == 4) {
				Point p1 = points.get(1), p2 = points.get(2);
				if (Math.abs(p1.x - this.w.xFinish) < Math.abs(p1.y
						- this.w.yFinish)) {
					p1.x = this.w.xFinish;
					if (p2.x != p1.x && p2.y != p1.y) {
						p2.x = p1.x;
						p2.y = this.w.yStart;
					} else {
						p2.x = this.w.xStart;
					}
				} else {
					p1.y = this.w.yFinish;
					if (p2.x != p1.x && p2.y != p1.y) {
						p2.y = p1.y;
						p2.x = this.w.xStart;
					} else {
						p2.y = this.w.yStart;
					}
				}
			} else if (size > 4) {
				Point p1 = points.get(1), p2 = points.get(size - 2);
				boolean adjustX = false, adjustY = false;
				if (Math.abs(p1.x - this.w.xFinish) < Math.abs(p1.y
						- this.w.yFinish)) {
					p1.x = this.w.xFinish;
					Point p = points.get(2);
					if (p1.isVia || (p.x != p1.x && p.y != p1.y))
						adjustX = true;
				} else {
					p1.y = this.w.yFinish;
					Point p = points.get(2);
					if (p1.isVia || (p.x != p1.x && p.y != p1.y))
						adjustY = true;
				}
				if (adjustX || adjustY) {
					x = p1.x;
					y = p1.y;
					int i = 2;
					while (true) {
						p1 = points.get(i);
						if (p1.x == x || p1.y == y || i == size - 1)
							break;
						if (adjustX)
							p1.x = this.w.xFinish;
						else
							p1.y = this.w.yFinish;
						if (!p1.isVia)
							break;
						x = p1.x;
						y = p1.y;
						i++;
					}
				}
				adjustX = adjustY = false;
				if (Math.abs(p2.x - this.w.xStart) < Math.abs(p2.y
						- this.w.yStart)) {
					p2.x = this.w.xStart;
					Point p = points.get(size - 3);
					if (p2.isVia || (p.x != p2.x && p.y != p2.y))
						adjustX = true;
				} else {
					p2.y = this.w.yStart;
					Point p = points.get(size - 3);
					if (p2.isVia || (p.x != p2.x && p.y != p2.y))
						adjustY = true;
				}

				if (adjustX || adjustY) {
					x = p2.x;
					y = p2.y;
					int i = size - 3;
					while (true) {
						p2 = points.get(i);
						if (p2.x == x || p2.y == y || i == 0)
							break;
						if (adjustX)
							p2.x = this.w.xStart;
						else
							p2.y = this.w.yStart;
						if (!p2.isVia)
							break;
						x = p2.x;
						y = p2.y;
						i--;
					}
				}
			}
			RoutingContact pin;

			RoutePoint rpCurrent = null, rpLast = null, rpFix = null;
			RoutingLayer l = null;
			int i = 0;
			for (Point p : points) {
				if (i == 0) {
					if (GLOBALDETAILEDROUTING) {
						Point2D startPoint = this.w.segment.getStartEnd()
								.getLocation();
						if (p.x == startPoint.getX()
								&& p.y == startPoint.getY()) {
							pin = RoutingContact.STARTPOINT;
							l = metalLayers[p.z];
						} else {
							if (p.isVia) {
								pin = metalVias[Math.min(p.z, p.zLast)]
										.getVias().get(0).via;
								l = metalLayers[p.zLast];
							} else {
								l = metalLayers[p.z];
								pin = l.getPin();
							}
						}
					} else {
						pin = RoutingContact.FINISHPOINT;
						l = metalLayers[p.z];
					}
				} else if (i == points.size() - 1) {
					if (GLOBALDETAILEDROUTING) {
						Point2D finishPoint = this.w.segment.getFinishEnd()
								.getLocation();
						if (p.x == finishPoint.getX()
								&& p.y == finishPoint.getY()) {
							pin = RoutingContact.FINISHPOINT;
							l = metalLayers[p.z];
						} else {
							if (p.isVia) {
								pin = metalVias[Math.min(p.z, p.zLast)]
										.getVias().get(0).via;
								l = metalLayers[p.zLast];
							} else {
								l = metalLayers[p.z];
								pin = l.getPin();
							}
						}
					} else {
						pin = RoutingContact.STARTPOINT;
						l = metalLayers[p.z];
					}
				} else {
					if (p.isVia) {
						pin = metalVias[Math.min(p.z, p.zLast)].getVias()
								.get(0).via;
						l = metalLayers[p.zLast];
					} else {
						l = metalLayers[p.z];
						pin = l.getPin();
					}
				}

				rpCurrent = new RoutePoint(pin, new Point2D.Double(p.x, p.y), 0);
				if (p.isVia) {
					this.addWireEndVia(rpCurrent);
				} else {
					this.addWireEnd(rpCurrent);
				}
				if (i == 0 && p.isVia) {
					rpFix = new RoutePoint(metalVias[Math.min(p.z, p.zLast)]
							.getVias().get(0).via,
							new Point2D.Double(p.x, p.y), 0);
					this.addWireEndVia(rpFix);
					l = metalLayers[p.zLast];
					this.addWire(l, l.getMinWidth(), rpCurrent, rpFix,
							this.w.netID);
					rpCurrent = rpFix;
				} else if (i == points.size() - 1 && p.isVia) {
					rpFix = new RoutePoint(metalVias[Math.min(p.z, p.zLast)]
							.getVias().get(0).via,
							new Point2D.Double(p.x, p.y), 0);
					this.addWireEndVia(rpFix);
					l = metalLayers[p.zLast];
					this.addWire(l, l.getMinWidth(), rpLast, rpFix,
							this.w.netID);
					rpLast = rpFix;
					l = metalLayers[p.z];
				}
				if (rpLast != null) {
					this.addWire(l, l.getMinWidth(), rpLast, rpCurrent,
							this.w.netID);
				}
				rpLast = rpCurrent;
				i++;
			}
		}

		/**
		 * Method to round a value to the nearest point of our routing grid.
		 * 
		 * @param d
		 *            The value to round.
		 * @return The corresponding grid value.
		 */
		private double toGrid(double d) {
			return Math.round(d * granularity) * stepsize;
		}

		private void addWireEnd(RoutePoint p) {
			this.w.segment.addWireEnd(p);
		}

		private void addWireEndVia(RoutePoint p) {
			this.w.segment.addWireEnd(p);
			RoutingContact rc = p.getContact();
			Point2D loc = p.getLocation();
			Rectangle2D rect = this
					.makeArcBox(loc, loc, rc.getViaSpacing() * 4);

			addRectangle(rect, rc.getFirstLayer(), -this.w.netID);
			addRectangle(rect, rc.getSecondLayer(), -this.w.netID);
		}

		/**
		 * Method to add a wire to the segment and update the R-Trees
		 * (blockages).
		 * 
		 * @param l
		 *            The routing layer.
		 * @param width
		 *            The width of the wire.
		 * @param from
		 *            Start point of the wire.
		 * @param to
		 *            End point of the wire.
		 * @param netID
		 *            The network ID.
		 * @return The RouteWire that was added.
		 */
		private RouteWire addWire(RoutingLayer l, double width,
				RoutePoint from, RoutePoint to, int netID) {
			RouteWire rw = new RouteWire(l, from, to, width);
			this.w.segment.addWire(rw);
			Rectangle2D box = makeArcBox(from.getLocation(), to.getLocation(),
					width);
			if (box != null)
				addRectangle(box, l, netID);

			return rw;
		}

		private Rectangle2D makeArcBox(Point2D from, Point2D to, double width) {
			double w2 = width / 2;
			int angle = DBMath.figureAngle(from, to);
			switch (angle) {
			case 0:
			case 1800:
				double y = to.getY();
				double x1 = to.getX();
				double x2 = from.getX();
				return new Rectangle2D.Double(Math.min(x1, x2) - w2, y - w2,
						Math.abs(x1 - x2) + width, width);
			case 900:
			case 2700:
				double x = to.getX();
				double y1 = to.getY();
				double y2 = from.getY();
				return new Rectangle2D.Double(x - w2, Math.min(y1, y2) - w2,
						width, Math.abs(y1 - y2) + width);
			}
			if (enableOutput.getTempBooleanValue())
				System.out.println("ERROR: Angle not allowed!");
			return null;
		}
	} // end class Backtracking

	/**
	 * Class to to the backtracking after the wavefront reached the finish
	 * point.
	 */
	private class ExperimentalGlobalBacktracking {
		private Wavefront w;
		private double granularity;
		private double stepsize;

		public ExperimentalGlobalBacktracking(Wavefront w) {
			this.w = w;
			granularity = w.getGranularity();
			stepsize = 1 / granularity;
		}

		protected class Point {
			double x, y;

			Point(double x, double y, int zLast, int z) {
				this.x = x;
				this.y = y;
			}

			Point(double x, double y, int z) {
				this.x = x;
				this.y = y;
			}
		}

		public List<Point> routeIt() throws RatingNotFoundException {

			List<Point> points = new ArrayList<Point>();

			double x = this.w.xGridFinish, y = this.w.yGridFinish;

			int z = w.zFinish, zLast = w.zFinish;

			// add finish route point
			// points.add(new Point(this.w.xFinish, this.w.yFinish, z));

			int lastRating = 0;
			Rating tmp = w.grid[z].getRating(x, y);
			if (tmp != null)
				lastRating = tmp.getRating();
			else {
				if (enableOutput.getTempBooleanValue())
					System.out
							.println("Backtracking did not find a route (Nullpointer Rating)!");
				throw new RatingNotFoundException();
			}

			// loop until start point is reached
			outer: while ((x != this.w.xGridStart) || (y != this.w.yGridStart)
					|| z != w.zStart) {

				// Save the visited point if there wasn't a layer change
				// (layer changes doesnt matter in global routing)
				// if (z == zLast && (x != xNextToLast || y != yNextToLast)) {
				if (z == zLast) {
					points.add(new Point(x, y, z));
				}

				for (int i = 0; i < 6; i++) {
					double dx = 0, dy = 0;
					int dz = 0;
					switch (i) {
					// 0-3: the 4 directions on the same layer
					case 0:
						dy = stepsize;
						break;
					case 1:
						dx = stepsize;
						break;
					case 2:
						dy = -stepsize;
						break;
					case 3:
						dx = -stepsize;
						break;
					// 4,5: change layer
					case 4:
						dz = -1;
						break;
					case 5:
						dz = 1;
						break;
					}

					// check bounds
					if (z + dz < 0 || z + dz >= numMetalLayers)
						continue;

					Rating r = this.w.grid[z + dz].getRating(toGrid(x + dx),
							toGrid(y + dy));
					if (r == null || r.getRating() >= lastRating) {
						if (i == 5) {
							if (enableOutput.getTempBooleanValue())
								System.out
										.println("Backtracking did not find a route!");
							break outer;
						} else
							continue;
					}

					zLast = z;
					lastRating = r.getRating();
					x = toGrid(x + dx);
					y = toGrid(y + dy);
					z += dz;
					break;
				}
			}
			if (z != zLast) {
				points.add(new Point(this.w.xStart, this.w.yStart, zLast, z));
			} else {
				points.add(new Point(this.w.xStart, this.w.yStart, z));
			}

			return points;
		}

		/**
		 * Method to round a value to the nearest point of our routing grid.
		 * 
		 * @param d
		 *            The value to round.
		 * @return The grid value.
		 */
		private double toGrid(double d) {
			return Math.round(d * granularity) * stepsize;
		}

	} // end class Backtracking

	private boolean initializeDesignRules(List<RoutingLayer> allLayers,
			List<RoutingContact> allContacts) {
		// find the metal layers, arcs, and contacts
		this.numMetalLayers = 0;
		for (RoutingLayer rl : allLayers)
			if (rl.isMetal())
				this.numMetalLayers++;
		this.metalLayers = new RoutingLayer[this.numMetalLayers];
		int metNo = 0;
		for (RoutingLayer rl : allLayers) {
			if (rl.isMetal())
				this.metalLayers[metNo++] = rl;
		}
		this.metalVias = new MetalVias[this.numMetalLayers - 1];
		for (int i = 0; i < this.numMetalLayers - 1; i++)
			this.metalVias[i] = new MetalVias();

		for (RoutingContact rc : allContacts) {
			for (int i = 0; i < this.numMetalLayers - 1; i++) {
				if ((rc.getFirstLayer() == this.metalLayers[i] && rc
						.getSecondLayer() == this.metalLayers[i + 1])
						|| (rc.getSecondLayer() == this.metalLayers[i] && rc
								.getFirstLayer() == this.metalLayers[i + 1])) {
					this.metalVias[i].addVia(rc);

					break;
				}
			}
		}
		for (int i = 0; i < numMetalLayers; i++) {
			if (metalLayers[i] == null) {
				if (enableOutput.getTempBooleanValue())
					System.out.println("ERROR: Cannot find layer for Metal "
							+ (i + 1));
				return true;
			}
			if (i < numMetalLayers - 1) {
				if (metalVias[i].getVias().size() == 0) {
					if (enableOutput.getTempBooleanValue())
						System.out
								.println("ERROR: Cannot find contact node between Metal "
										+ (i + 1) + " and Metal " + (i + 2));
					return true;
				}
			}
		}
		return false;
	}

	private class WavefrontExecutor implements Runnable {
		private Wavefront w;

		public WavefrontExecutor(Wavefront w) {
			this.w = w;
		}

		public void run() {
			w.propagate();
			try {
				outQueue.put(w);
			} catch (InterruptedException e) {
				if (enableOutput.getTempBooleanValue())
					e.printStackTrace();
			}
		}
	}

	/**
	 * Method to see if a proposed piece of metal has DRC errors (ignoring
	 * notches).
	 * 
	 * @param netID
	 *            the network ID of the desired metal (blockages on this netID
	 *            are ignored).
	 * @param metNo
	 *            the level of the metal.
	 * @param halfWidth
	 *            half of the width of the metal.
	 * @param halfHeight
	 *            half of the height of the metal.
	 * @param surround
	 *            is the maximum possible DRC surround around the metal.
	 * @param x
	 *            the X coordinate at the center of the metal.
	 * @param y
	 *            the Y coordinate at the center of the metal.
	 * @return a blocking SOGBound object that is in the area. Returns null if
	 *         the area is clear.
	 */
	private LMBound getMetalBlockage(int netID, int metNo, double halfWidth,
			double halfHeight, double surround, double x, double y) {
		// get the R-Tree data for the metal layer
		RoutingLayer layer = metalLayers[metNo];
		RTNode rtree = metalBlockages.get(layer);
		if (rtree == null)
			return null;

		// compute the area to search
		double lX = x - halfWidth - surround, hX = x + halfWidth + surround;
		double lY = y - halfHeight - surround, hY = y + halfHeight + surround;
		Rectangle2D searchArea = new Rectangle2D.Double(lX, lY, hX - lX, hY
				- lY);

		try {
			// see if there is anything in that area
			for (RTNode.Search sea = new RTNode.Search(searchArea, rtree, true); sea
					.hasNext();) {
				LMBound sBound = (LMBound) sea.next();

				// ignore if on the same net
				if (sBound.getNetID() == netID)
					continue;

				return sBound;
			}
		} catch (NullPointerException e) {
			return new LMBound(new Rectangle2D.Double(), -1);
		}
		return null;
	}

	/**
	 * Method to add extra blockage information that corresponds to ends of
	 * unrouted arcs.
	 * 
	 * @param segmentsToRoute
	 *            The list of segments to route.
	 * @param tech
	 *            The technology to use.
	 */
	private void addBlockagesAtPorts(List<RoutingSegment> segmentsToRoute) {
		for (RoutingSegment rs : segmentsToRoute) {
			addBlockage(rs.getStartEnd().getLocation(), rs.getStartLayers(), rs
					.getStartLayers().get(0).getMinWidth() + 2, rs.getNetID());
			addBlockage(rs.getFinishEnd().getLocation(), rs.getFinishLayers(),
					rs.getFinishLayers().get(0).getMinWidth() + 2, rs
							.getNetID());
		}
	}

	private void addBlockage(Point2D loc, List<RoutingLayer> layers,
			double minWidth, int netID) {
		int lowMetal = -1, highMetal = -1;
		for (RoutingLayer rl : layers) {
			int level = rl.getMetalNumber();
			if (lowMetal < 0)
				lowMetal = highMetal = level;
			else {
				lowMetal = Math.min(lowMetal, level);
				highMetal = Math.max(highMetal, level);
			}
		}
		if (lowMetal < 0)
			return;
		// reserve space on layers above and below
		for (int via = lowMetal - 2; via < highMetal; via++) {
			if (via < 0 || via >= numMetalLayers - 1)
				continue;
			MetalVia mv = metalVias[via].getVias().get(0);
			for (RoutingGeometry rg : mv.via.getGeometry()) {
				Rectangle2D bounds = new Rectangle2D.Double(loc.getX()
						- minWidth, loc.getY() - minWidth, 2 * minWidth,
						2 * minWidth);
				addRectangle(bounds, rg.getLayer(), netID);
			}
		}
	}

	/**
	 * Method to add a rectangle to the metal R-Tree.
	 * 
	 * @param bounds
	 *            the rectangle to add.
	 * @param layer
	 *            the metal layer on which to add the rectangle.
	 * @param netID
	 *            the global network ID of the geometry.
	 */
	private void addRectangle(Rectangle2D bounds, RoutingLayer layer, int netID) {
		RTNode root = this.metalBlockages.get(layer);
		if (root == null) {
			root = RTNode.makeTopLevel();
			this.metalBlockages.put(layer, root);
		}
		RTNode newRoot = RTNode
				.linkGeom(null, root, new LMBound(bounds, netID));
		if (newRoot != root)
			this.metalBlockages.put(layer, newRoot);
	}

	/**
	 * Method to add a point to the via R-Tree.
	 * 
	 * @param loc
	 *            The point to add.
	 * @param layer
	 *            The layer on which to add the point.
	 * @param netID
	 *            The global network ID of the geometry.
	 */
	private void addVia(Point2D loc, RoutingLayer layer, int netID) {
		RTNode root = this.viaBlockages.get(layer);
		if (root == null) {
			root = RTNode.makeTopLevel();
			this.viaBlockages.put(layer, root);
		}
		RTNode newRoot = RTNode.linkGeom(null, root, new LMVia(loc, netID));
		if (newRoot != root)
			this.viaBlockages.put(layer, newRoot);
	}

	/**
	 * Class to define a list of possible nodes that can connect two layers.
	 */
	private static class MetalVia {
		RoutingContact via;

		MetalVia(RoutingContact v) {
			via = v;
		}
	}

	/**
	 * Class to define a list of possible nodes that can connect two layers.
	 * This includes orientation
	 */
	private static class MetalVias {
		List<MetalVia> vias = new ArrayList<MetalVia>();

		void addVia(RoutingContact pn) {
			vias.add(new MetalVia(pn));
			Collections.sort(vias, new PrimsBySize());
		}

		List<MetalVia> getVias() {
			return vias;
		}
	}

	/**
	 * Comparator class for sorting primitives by their size.
	 */
	private static class PrimsBySize implements Comparator<MetalVia> {
		/**
		 * Method to sort primitives by their size.
		 */
		public int compare(MetalVia mv1, MetalVia mv2) {
			RoutingContact pn1 = mv1.via;
			RoutingContact pn2 = mv2.via;
			double sz1 = pn1.getDefWidth() * pn1.getDefHeight();
			double sz2 = pn2.getDefWidth() * pn2.getDefHeight();
			if (sz1 < sz2)
				return -1;
			if (sz1 > sz2)
				return 1;
			return 0;
		}
	}

	/**
	 * Class to define an R-Tree leaf node for geometry in the blockage data
	 * structure.
	 */
	private static class LMBound implements RTBounds {
		private Rectangle2D bound;
		private int netID;

		LMBound(Rectangle2D bound, int netID) {
			this.bound = bound;
			this.netID = netID;
		}

		public Rectangle2D getBounds() {
			return bound;
		}

		public int getNetID() {
			return netID;
		}

		// public String toString() { return "LMBound on net " + netID; }
	}

	/**
	 * Class to define an R-Tree leaf node for vias in the blockage data
	 * structure.
	 */
	private static class LMVia implements RTBounds {
		private Point2D loc;
		private int netID;

		LMVia(Point2D loc, int netID) {
			this.loc = loc;
			this.netID = netID;
		}

		public Rectangle2D getBounds() {
			return new Rectangle2D.Double(loc.getX(), loc.getY(), 0, 0);
		}

		public String toString() {
			return "LMVia on net " + netID;
		}
	}
}
