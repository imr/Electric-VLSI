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

import com.sun.electric.tool.util.concurrent.datastructures.IStructure;
import com.sun.electric.tool.util.concurrent.patterns.PTask;
import com.sun.electric.tool.util.concurrent.runtime.ThreadID;

/**
 * 
 * Simple thread pool worker
 * 
 * @author Felix Schmidt
 * 
 */
public class SimpleWorker extends PoolWorkerStrategy {

	public SimpleWorker(IStructure<PTask> taskPool) {
		super();
		this.setTaskPool(taskPool);
		this.abort = false;
	}

	/**
	 * This function iterates while the flag abort is false. <br>
	 * <b>Algorithm:</b>
	 * <ul>
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

			// retrieve a new task
			PTask task = getTaskPool().remove();
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
