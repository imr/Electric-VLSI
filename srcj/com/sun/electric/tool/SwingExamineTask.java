/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SwingExamineTask.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
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
/**
 * User: gainsley
 * Date: Oct 7, 2004
 * Time: 2:15:59 PM
 */

package com.sun.electric.tool;

/**
 * Allows the GUI thread to attempt to examine the database immediately.  If that
 * fails, then the GUI can pass this object to the Job queue via Job.invokeExamineLater()
 * to have it run at a later time in the GUI thread.  A standard usage is as follows:
 * <p>
 * <pre><code>
 * SwingExamineTask task = new SwingExamineTask() {
 *     protected boolean doIt() {
 *         // do something that requires database examine and GUI modification
 *     }
 * }
 * // try to run the task immediately, if not possible, run at a later time
 * if (!task.runImmediately()) {
 *     Job.invokeExamineLater(task, null);
 * }
 * </code></pre>
 * <p>
 * Note: it is not necessary to acquire an examine lock in doIt(),
 * as the SwingExamineTask ensures that an examine lock already held
 * in all cases.
 *
 */
public abstract class SwingExamineTask implements Runnable {

	private boolean immediate = true;

    /**
     * This should only be called by the Job class.
     * It asserts that the Job class has created an active examine
     * Job which guarantees that this will be able to get
     * an examine lock from the GUI thread.
     */
    public final void run() {
        immediate = false;
        boolean b = runImmediately();
        assert(b);
        immediate = true;
    }

    /**
     * This tries to execute doIt() immediately by trying to
     * acquire an examine lock for the database. Returns false
     * if could not get lock.
     * @return true if lock acquired and doIt() run, false if
     * lock not acquired and nothing done.
     */
    public final boolean runImmediately() {

/*
        if (Job.acquireExamineLock(false)) {
            try {
                doIt(immediate);
                Job.releaseExamineLock();
            } catch (Error e) {
                Job.releaseExamineLock();
                throw e;
            }
            return true;
        }
        return false;
*/
        // locking not working right now, just run it
        doIt(immediate);
        return true;
    }

    /**
     * This should contain the code that needs to examine the database
     * while in the GUI thread
     * @param immediate true if run immediately using runImmediate(),
     * false if run through callback to run().
     * @return true if successful, false otherwise
     */
    protected abstract boolean doIt(boolean immediate);

}
