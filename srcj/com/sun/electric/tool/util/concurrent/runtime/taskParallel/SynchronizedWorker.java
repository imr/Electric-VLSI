/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SimpleWorker.java
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
package com.sun.electric.tool.util.concurrent.runtime.taskParallel;

import java.util.concurrent.Semaphore;

import com.sun.electric.tool.util.IStructure;
import com.sun.electric.tool.util.concurrent.patterns.PTask;
import com.sun.electric.tool.util.concurrent.runtime.ThreadID;
import com.sun.electric.tool.util.concurrent.runtime.WorkerStrategy;

/**
 * 
 * Synchronized thread pool worker
 * 
 * Some algorithms requires that the calculation of different steps should run
 * step-by-step in parallel. This worker implements the PRAM aspect that in one
 * time unit one instruction could be executed in parallel. This worker uses a
 * bigger grain size. N (#threads) tasks could be implemented in parallel. After
 * the parallel execution is a barrier which could be release by a timer or the
 * client application (ThreadPool.getThreadPool().trigger()).
 * 
 * @author Felix Schmidt
 * 
 */
public class SynchronizedWorker extends PoolWorkerStrategy {

	protected IStructure<PTask> taskPool = null;
	protected Semaphore sem;

	public SynchronizedWorker(IStructure<PTask> taskPool) {
		super();
		this.taskPool = taskPool;
		this.abort = false;
		sem = new Semaphore(0);
	}

	public void trigger() {
		sem.release();
	}

	/**
	 * This function iterates while the flag abort is false. <br>
	 * <b>Algorithm:</b>
	 * <ul>
	 * <li>wait for trigger</li>
	 * <li>pick one task from thread pool's task queue</li>
	 * <li>if task not equal to null, then set threadId and do some
	 * initialization work on the task object</li>
	 * <li>execute the task</li>
	 * <li>finalize work on the task object</li>
	 * <li>do it again ...</li>
	 * </ul>
	 */
	@Override
	public void execute() {
		this.threadId = ThreadID.get();
		this.executed = 0;
		while (!abort) {
			this.checkForWait();

			sem.acquireUninterruptibly();

			// retrieve a new task
			PTask task = taskPool.remove();
			if (task != null) {
				try {
					// set the current thread id
					task.setThreadID(ThreadID.get());
					// do something before execution
					task.before();
					// execute the task
					task.execute();
					this.executed++;
				} finally {
					// do some clean up work etc. after execution of the task
					task.after();

					// Debug

				}
			} else {
				Thread.yield();
			}
		}
	}

}
