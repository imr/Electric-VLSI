/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IThreadPool.java
 *
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.electric.tool.util.concurrent.patterns.PTask;

public abstract class IThreadPool {
	
	public static Integer NUM_THREADS = null; 
	
    /**
     * start the thread pool
     */
    public abstract void start();
    
    /**
     * shutdown the thread pool
     */
    public abstract void shutdown() throws InterruptedException;


    /**
     * wait for termination
     * 
     * @throws InterruptedException
     */
    public abstract void join() throws InterruptedException;

    /**
     * Set thread pool to state sleep. Constraint: current State = started
     */
    public abstract void sleep();

    /**
     * Wake up the thread pool. Constraint: current State = sleeps
     */
    public abstract void weakUp();

    /**
     * trigger workers (used for the synchronization)
     */
    public abstract void trigger();

    /**
     * add a task to the pool
     * 
     * @param item
     */
    public abstract void add(PTask item);

    /**
     * add a task to the pool
     * 
     * @param item
     */
    public abstract void add(PTask item, int threadId);

    /**
     * 
     * @return the current thread pool size (#threads)
     */
    public abstract int getPoolSize();
    
    

}
