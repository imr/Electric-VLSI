/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PJob.java
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
package com.sun.electric.tool.util.concurrent.patterns;

import com.sun.electric.tool.util.concurrent.runtime.ThreadPool;


/**
 * parallel job. This job ends if a new further tasks for this job are available.
 * 
 */
public class PJob {

	protected int numOfTasksTotal = 0;
	protected int numOfTasksFinished = 0;
	protected ThreadPool pool;
	
	public PJob() {
		this.pool = ThreadPool.getThreadPool(); 	
	}

    /**
     * Call back function for thread pool workers, to tell this job that a task
     * of this job is finished.
     */
	public synchronized void finishTask() {
		numOfTasksFinished++;
	}

    /**
     * Executor method of a job. This job ends
     */
	public void execute() {
		
		pool.start();

		while (numOfTasksFinished != numOfTasksTotal) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

    /**
     * Use this method to add tasks to this job. This will affect that the new
     * task is registered by this job.
     * @param task
     */
	public void add(PTask task) {		
		numOfTasksTotal++;
		pool.add(task);
	}

}
