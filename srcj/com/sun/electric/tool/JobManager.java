/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JobManager.java
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
package com.sun.electric.tool;

import com.sun.electric.database.Snapshot;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 */
public abstract class JobManager {
    private final ReentrantLock lock = new ReentrantLock();
    /** started jobs */ final ArrayList<EJob> startedJobs = new ArrayList<EJob>();
    /** waiting jobs */ final ArrayList<EJob> waitingJobs = new ArrayList<EJob>();
    
    void lock() { lock.lock(); }
    void unlock() { lock.unlock(); }
    Condition newCondition() { return lock.newCondition(); }
    
    abstract void runLoop();
    abstract void addJob(EJob ejob, boolean onMySnapshot);
    /** Remove job from list of jobs */
    abstract void removeJob(Job j);
    abstract EJob selectEJob(EJob finishedEJob);
    abstract void setProgress(EJob ejob, String progress);
    abstract Iterator<Job> getAllJobs();
    abstract void wantUpdateGui();
    
    /**
     * Find some valid snapshot in cache.
     * @return some valid snapshot
     */
    public static Snapshot findValidSnapshot() {
        return EThread.findValidSnapshot();
    }
}
    
