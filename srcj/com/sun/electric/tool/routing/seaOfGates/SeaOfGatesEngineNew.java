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
import java.util.concurrent.Semaphore;

import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.Environment;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.util.concurrent.datastructures.WorkStealingStructure;
import com.sun.electric.tool.util.concurrent.exceptions.PoolExistsException;
import com.sun.electric.tool.util.concurrent.patterns.PJob;
import com.sun.electric.tool.util.concurrent.patterns.PTask;
import com.sun.electric.tool.util.concurrent.runtime.ThreadPool;
import com.sun.electric.tool.util.concurrent.runtime.ThreadPool.ThreadPoolType;

/**
 * @author fs239085
 * 
 */
public class SeaOfGatesEngineNew extends SeaOfGatesEngine {

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
	protected void doRoutingParallel(int numberOfThreads, List<NeededRoute> allRoutes,
			RouteBatches[] routeBatches, Environment env, EditingPreferences ep) {

		if (Job.getDebug())
			System.out.println("Do routing parallel with new parallel Infrastructure");

		try {
			ThreadPool.initialize(WorkStealingStructure.createForThreadPool(numberOfThreads),
					numberOfThreads, true, ThreadPoolType.synchronizedPool);
			// ThreadPool.initialize(numberOfThreads, true);
		} catch (PoolExistsException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		PJob seaOfGatesJob = new PJob();
		seaOfGatesJob.execute(false);

		NeededRoute[] routesToDo = new NeededRoute[numberOfThreads];
		int[] routeIndices = new int[numberOfThreads];
		Semaphore outSem = new Semaphore(0);

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
				seaOfGatesJob.add(new RouteInTask(seaOfGatesJob, nr, ep, outSem), threadAssign);
				threadAssign++;
				if (threadAssign >= numberOfThreads)
					break;
			}

			ThreadPool.getThreadPool().trigger();

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
			ThreadPool.getThreadPool().shutdown();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private class RouteInTask extends PTask {
		private NeededRoute nr;
		private EditingPreferences ed;
		private Semaphore whenDone;

		public RouteInTask(PJob job, NeededRoute nr, EditingPreferences ed, Semaphore outSem) {
			super(job);
			this.nr = nr;
			this.ed = ed;
			this.whenDone = outSem;
		}

		public void execute() {

			EditingPreferences.setThreadEditingPreferences(ed);

			if (nr == null)
				return;
			findPath(nr, Environment.getThreadEnvironment(), ed);
			whenDone.release();
		}
	}
}
