/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SeaOfGatesEngineOld.java
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
package com.sun.electric.tool.routing.seaOfGates;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.Environment;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.routing.SeaOfGates.SeaOfGatesOptions;
import com.sun.electric.tool.routing.seaOfGates.SeaOfGatesEngine.NeededRoute;
import com.sun.electric.tool.routing.seaOfGates.SeaOfGatesEngine.RouteBatches;
import com.sun.electric.tool.util.concurrent.Parallel;
import com.sun.electric.tool.util.concurrent.datastructures.WorkStealingStructure;
import com.sun.electric.tool.util.concurrent.exceptions.PoolExistsException;
import com.sun.electric.tool.util.concurrent.patterns.PJob;
import com.sun.electric.tool.util.concurrent.patterns.PTask;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange1D;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.PForTask;
import com.sun.electric.tool.util.concurrent.runtime.Scheduler.SchedulingStrategy;
import com.sun.electric.tool.util.concurrent.runtime.Scheduler.UnknownSchedulerException;
import com.sun.electric.tool.util.concurrent.runtime.taskParallel.ThreadPool;
import com.sun.electric.tool.util.concurrent.runtime.taskParallel.ThreadPool.ThreadPoolType;

/**
 * @author Felix Schmidt
 * 
 */
public class SeaOfGatesEngineNew extends SeaOfGatesEngine {

	private ThreadPool[] pools;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sun.electric.tool.routing.seaOfGates.SeaOfGatesEngine#doRouting(java
	 * .util.List,
	 * com.sun.electric.tool.routing.seaOfGates.SeaOfGatesEngine.RouteBatches[],
	 * com.sun.electric.tool.Job, com.sun.electric.database.Environment,
	 * com.sun.electric.database.EditingPreferences)
	 */
	@Override
	protected void doRouting(List<NeededRoute> allRoutes, RouteBatches[] routeBatches, Job job, Environment env,
			EditingPreferences ep) {

		try {
			// ThreadPool.initialize(WorkStealingStructure.createForThreadPool(2),
			// 2, true);
			ThreadPool.initialize(SchedulingStrategy.workStealing, 2, ThreadPoolType.userDefined);
		} catch (PoolExistsException e1) {
		} catch (UnknownSchedulerException e) {
		}

		super.doRouting(allRoutes, routeBatches, job, env, ep);

		try {
			ThreadPool.getThreadPool().shutdown();
		} catch (InterruptedException e) {
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sun.electric.tool.routing.seaOfGates.SeaOfGatesEngine#doRoutingParallel
	 * (int, java.util.List,
	 * com.sun.electric.tool.routing.seaOfGates.SeaOfGatesEngine.RouteBatches[],
	 * com.sun.electric.database.Environment,
	 * com.sun.electric.database.EditingPreferences)
	 */
	@Override
	protected void doRoutingParallel(int numberOfThreads, List<NeededRoute> allRoutes, RouteBatches[] routeBatches,
			Environment env, EditingPreferences ep) {

		if (Job.getDebug())
			System.out.println("Do routing parallel with new parallel Infrastructure");

		try {

			if (parallelDij) {

				pools = ThreadPool.initialize(WorkStealingStructure.createForThreadPool(numberOfThreads),
						numberOfThreads, ThreadPoolType.userDefined, WorkStealingStructure
								.createForThreadPool(numberOfThreads), numberOfThreads, ThreadPoolType.userDefined);
			} else {
				ThreadPool.initialize(WorkStealingStructure.createForThreadPool(numberOfThreads), numberOfThreads,
						ThreadPoolType.synchronizedPool);
				// ThreadPool.initialize(numberOfThreads, true);
			}
		} catch (PoolExistsException e1) {
		}

		PJob seaOfGatesJob;
		if (pools != null) {
			seaOfGatesJob = new PJob(pools[0]);
		} else {
			seaOfGatesJob = new PJob();
		}
		// non-blocking execute
		seaOfGatesJob.execute(false);

		NeededRoute[] routesToDo = new NeededRoute[numberOfThreads];
		int[] routeIndices = new int[numberOfThreads];

		// create list of routes and blocked areas
		List<NeededRoute> myList = new ArrayList<NeededRoute>();
		for (NeededRoute nr : allRoutes)
			myList.add(nr);
		List<Rectangle2D> blocked = new ArrayList<Rectangle2D>();

		// now run the threads
		int totalRoutes = allRoutes.size();
		int routesDone = 0;
		while (myList.size() > 0) {
			int threadAssign = 0;
			blocked.clear();
			for (int i = 0; i < myList.size(); i++) {
				NeededRoute nr = myList.get(i);
				boolean isBlocked = false;
				for (Rectangle2D block : blocked) {
					if (block.intersects(nr.routeBounds)) {
						isBlocked = true;
						break;
					}
				}
				if (isBlocked)
					continue;

				// this route can be done: start it
				blocked.add(nr.routeBounds);
				routesToDo[threadAssign] = nr;
				routeIndices[threadAssign] = i;
				seaOfGatesJob.add(new RouteInTask(seaOfGatesJob, nr, ep), threadAssign);
				threadAssign++;
				if (threadAssign >= numberOfThreads)
					break;
			}

			if (pools != null) {
				pools[0].trigger();
			} else {
				ThreadPool.getThreadPool().trigger();
			}

			String routes = "";
			for (int i = 0; i < threadAssign; i++) {
				String routeName = routesToDo[i].routeName;
				if (routeBatches[routesToDo[i].batchNumber].segsInBatch > 1)
					routeName += "(" + routesToDo[i].routeInBatch + "/"
							+ routeBatches[routesToDo[i].batchNumber].segsInBatch + ")";
				if (routes.length() > 0)
					routes += ", ";
				routes += routeName;
			}
			System.out.println("Parallel routing " + routes + "...");
			Job.getUserInterface().setProgressNote(routes);

			// now wait for routing threads to finish
			// outSem.acquireUninterruptibly(threadAssign);
			seaOfGatesJob.join();

			// all done, now handle the results
			for (int i = 0; i < threadAssign; i++) {
				if (routesToDo[i].winningWF != null && routesToDo[i].winningWF.vertices != null)
					createRoute(routesToDo[i]);
			}
			for (int i = threadAssign - 1; i >= 0; i--)
				myList.remove(routeIndices[i]);
			routesDone += threadAssign;
			Job.getUserInterface().setProgressValue(routesDone * 100 / totalRoutes);
		}

		seaOfGatesJob.join();
		try {
			if (pools != null) {
				pools[0].shutdown();
				pools[1].shutdown();
			} else {
				ThreadPool.getThreadPool().shutdown();
			}
		} catch (InterruptedException e) {
		}

	}

	/**
	 * 
	 * @author Felix Schmidt
	 * 
	 */
	private class RouteInTask extends PTask {
		private NeededRoute nr;
		private EditingPreferences ed;

		public RouteInTask(PJob job, NeededRoute nr, EditingPreferences ed) {
			super(job);
			this.nr = nr;
			this.ed = ed;
		}

		public void execute() {

			EditingPreferences.setThreadEditingPreferences(ed);
			if (nr == null)
				return;
			findPath(nr, Environment.getThreadEnvironment(), ed);

		}
	}

	/**
	 * Method to find a path between two ports.
	 * 
	 * @param nr
	 *            the NeededRoute object with all necessary information. If
	 *            successful, the NeededRoute's "vertices" field is filled with
	 *            the route data.
	 */
	@Override
	protected void findPath(NeededRoute nr, Environment env, EditingPreferences ep) {
		// special case when route is null length
		Wavefront d1 = nr.dir1;
		if (DBMath.areEquals(d1.toX, d1.fromX) && DBMath.areEquals(d1.toY, d1.fromY) && d1.toZ == d1.fromZ) {
			nr.winningWF = d1;
			nr.winningWF.vertices = new ArrayList<SearchVertex>();
			SearchVertex sv = new SearchVertex(d1.toX, d1.toY, d1.toZ, 0, null, 0, nr.winningWF);
			nr.winningWF.vertices.add(sv);
			nr.cleanSearchMemory();
			return;
		}

		PJob dijkstraJob;
		if (pools.length == 2)
			dijkstraJob = new PJob(pools[1]);
		else
			dijkstraJob = new PJob();

		if (parallelDij) {
			dijkstraJob.add(new DijkstraInTask(dijkstraJob, nr.dir1, nr.dir2, ep));
			dijkstraJob.add(new DijkstraInTask(dijkstraJob, nr.dir2, nr.dir1, ep));

			dijkstraJob.execute();

		} else {
			// run both wavefronts in parallel (interleaving steps)
			doTwoWayDijkstra(nr);
		}

		// analyze the winning wavefront
		Wavefront wf = nr.winningWF;
		double verLength = Double.MAX_VALUE;
		if (wf != null)
			verLength = SeaOfGatesEngine.getVertexLength(wf.vertices);
		if (verLength == Double.MAX_VALUE) {
			// failed to route
			String errorMsg;
			if (wf == null)
				wf = nr.dir1;
			if (wf.vertices == null) {
				errorMsg = "Search too complex (exceeds complexity limit of " + nr.prefs.complexityLimit + " steps)";
			} else {
				errorMsg = "Failed to route from port " + wf.from.getPortProto().getName() + " of node "
						+ wf.from.getNodeInst().describe(false) + " to port " + wf.to.getPortProto().getName()
						+ " of node " + wf.to.getNodeInst().describe(false);
			}
			System.out.println("ERROR: " + errorMsg);
			List<EPoint> lineList = new ArrayList<EPoint>();
			lineList.add(new EPoint(wf.toX, wf.toY));
			lineList.add(new EPoint(wf.fromX, wf.fromY));
			errorLogger.logMessageWithLines(errorMsg, null, lineList, cell, 0, true);

			if (DEBUGFAILURE && firstFailure) {
				firstFailure = false;
				EditWindow_ wnd = Job.getUserInterface().getCurrentEditWindow_();
				wnd.clearHighlighting();
				showSearchVertices(nr.dir1.searchVertexPlanes, false, cell);
				wnd.finishedHighlighting();
			}
		}
		nr.cleanSearchMemory();
	}

	class DijkstraInTask extends PTask {
		private Wavefront wf;
		private Wavefront otherWf;
		private Environment env;
		private EditingPreferences ep;

		public DijkstraInTask(PJob job, Wavefront wf, Wavefront otherWf, EditingPreferences ep) {
			super(job);
			this.wf = wf;
			this.otherWf = otherWf;
			this.ep = ep;
		}

		public void execute() {
			EditingPreferences.setThreadEditingPreferences(ep);
			SearchVertex result = null;
			int numSearchVertices = 0;
			while (result == null) {
				// stop if the search is too complex
				numSearchVertices++;
				if (numSearchVertices > wf.nr.prefs.complexityLimit) {
					result = svLimited;
				} else {
					if (wf.abort)
						result = svAborted;
					else {
						result = advanceWavefront(wf);
					}
				}
			}
			if (result != svAborted && result != svExhausted && result != svLimited) {
				if (DEBUGLOOPS)
					System.out.println("    Wavefront " + wf.name + " first completion");
				wf.vertices = getOptimizedList(result);
				wf.nr.winningWF = wf;
				otherWf.abort = true;
			} else {
				if (DEBUGLOOPS) {
					String status = "completed";
					if (result == svAborted)
						status = "aborted";
					else if (result == svExhausted)
						status = "exhausted";
					else if (result == svLimited)
						status = "limited";
					System.out.println("    Wavefront " + wf.name + " " + status);
				}
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sun.electric.tool.routing.seaOfGates.SeaOfGatesEngine#makeListOfRoutes
	 * (int, int,
	 * com.sun.electric.tool.routing.seaOfGates.SeaOfGatesEngine.RouteBatches[],
	 * java.util.List, java.util.List,
	 * com.sun.electric.tool.routing.SeaOfGates.SeaOfGatesOptions)
	 */
	@Override
	protected void makeListOfRoutes(int numBatches, RouteBatches[] routeBatches, List<NeededRoute> allRoutes,
			List<ArcInst> arcsToRoute, SeaOfGatesOptions prefs, EditingPreferences ep) {

		try {
			ThreadPool.initialize(WorkStealingStructure.createForThreadPool(8));
		} catch (PoolExistsException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		Parallel.For(new BlockedRange1D(0, numBatches, numBatches / 8), new ParallelListOfRoutes(routeBatches,
				allRoutes, arcsToRoute, prefs, ep));

		try {
			ThreadPool.getThreadPool().shutdown();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public class ParallelListOfRoutes extends PForTask {

		private RouteBatches[] routeBatches;
		private List<NeededRoute> allRoutes;
		private List<ArcInst> arcsToRoute;
		private SeaOfGatesOptions prefs;
		private EditingPreferences ep;

		public ParallelListOfRoutes(RouteBatches[] routeBatches, List<NeededRoute> allRoutes,
				List<ArcInst> arcsToRoute, SeaOfGatesOptions prefs, EditingPreferences ep) {
			this.routeBatches = routeBatches;
			this.allRoutes = allRoutes;
			this.arcsToRoute = arcsToRoute;
			this.prefs = prefs;
			this.ep = ep;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.sun.electric.tool.util.concurrent.patterns.PForJob.PForTask#execute
		 * (com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange)
		 */
		@Override
		public void execute(BlockedRange range) {
			EditingPreferences.setThreadEditingPreferences(ep);
			BlockedRange1D tmpRange = (BlockedRange1D) range;
			doMakeListOfRoutes(tmpRange.start(), tmpRange.end(), routeBatches, allRoutes, arcsToRoute, prefs);

		}

	}

}
