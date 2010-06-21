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

/**
 * @author Felix Schmidt
 * 
 */
public class SeaOfGatesEngineOld extends SeaOfGatesEngine {

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
			System.out.println("Do routing parallel with raw threads");
		
		// create threads and other threading data structures
		RouteInThread[] threads = new RouteInThread[numberOfThreads];
		for (int i = 0; i < numberOfThreads; i++)
			threads[i] = new RouteInThread("Route #" + (i + 1), env, ep);
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
			for (int i = 0; i < threadAssign; i++) {
				String routeName = routesToDo[i].routeName;
				if (routeBatches[routesToDo[i].batchNumber].segsInBatch > 1)
					routeName += "(" + routesToDo[i].routeInBatch + "/"
							+ routeBatches[routesToDo[i].batchNumber].segsInBatch + ")";
				if (routes.length() > 0) routes += ", ";
				routes += routeName;
			}
			System.out.println("Parallel routing " + routes + "...");
			Job.getUserInterface().setProgressNote(routes);

			// now wait for routing threads to finish
			outSem.acquireUninterruptibly(threadAssign);

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

		// terminate the threads
		for (int i = 0; i < numberOfThreads; i++)
			threads[i].startRoute(null, null);

	}
	
	private class RouteInThread extends Thread {
		private Semaphore inSem = new Semaphore(0);
		private NeededRoute nr;
		private Semaphore whenDone;
		private Environment env;
		private EditingPreferences ep;

		public RouteInThread(String name, Environment env, EditingPreferences ep) {
			super(name);
			this.env = env;
			this.ep = ep;
			start();
		}

		public void startRoute(NeededRoute nr, Semaphore whenDone) {
			this.nr = nr;
			this.whenDone = whenDone;
			inSem.release();
		}

		public void run() {
			Environment.setThreadEnvironment(env);
			EditingPreferences.setThreadEditingPreferences(ep);
			for (;;) {
				inSem.acquireUninterruptibly();
				if (nr == null) return;
				findPath(nr, env, ep);
				whenDone.release();
			}
		}
	}

}
