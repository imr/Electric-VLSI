/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MultiTaskJob.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This generic class supports map-reduce scheme of computation on Electric database.
 * Large computation has three stages:
 * 1) Large computation is splitted into smaller tasks.
 * Smaller tasks are identified by TaskKey class.
 * This stage is performed by prepareTasks method, which schedules each task by startTask method.
 * 2) Tasks run in parallel, each giving result of TaskResult type.
 * This stage is performed by runTask method for each instance of task.
 * 3) TaskResults are combinded into final result of Result type.
 * This stage is performed by mergeTaskResults method.
 * 4) Result is consumed on server.
 * This stage is performed by consumer.consume method.
 */
public abstract class MultiTaskJob<TaskKey,TaskResult,Result> extends Job {
    private transient LinkedHashMap<TaskKey,TaskJob> tasks;
    private Consumer<Result> consumer;
    
    /**
	 * Constructor creates a new instance of MultiTaskJob.
	 * @param jobName a string that describes this MultiTaskJob.
	 * @param t the Tool that originated this MultiTaskJob.
	 * @param jobType the Type of this Job (EXAMINE or CHANGE).
     * @param c interface which consumes the result on server
	 */
    public MultiTaskJob(String jobName, Tool t, Type jobType, Consumer<Result> c) {
        super(jobName, t, jobType, null, null, Job.Priority.USER);
        this.consumer = c;
    }
    
    /**
     * This abstract method split large computation into smaller task.
     * Smaller tasks are identified by TaskKey class.
     * Each task is scheduled by startTask method.
     * @throws com.sun.electric.tool.JobException
     */
    public abstract void prepareTasks() throws JobException; 
    
    /**
     * This abtract methods performs computation of each task.
     * @param taskKey task key which identifies the task
     * @return result of task computation
     * @throws com.sun.electric.tool.JobException
     */
    public abstract TaskResult runTask(TaskKey taskKey) throws JobException;
    
    /**
     * This abtract method combines task results into final result.
     * @param taskResults map which contains result of each completed task.
     * @return final result which is obtained by merging task results.
     * @throws com.sun.electric.tool.JobException
     */
    public abstract Result mergeTaskResults(Map<TaskKey,TaskResult> taskResults) throws JobException;
    
//    /**
//     * This method executes in the Client side after normal termination of full computation.
//     * This method should perform all needed termination actions.
//     * @param result result of full computation.
//     */
//    public void terminateOK(Result result) {}
    
    /**
     * Schedules task. Should be callled from prepareTasks or runTask methods only.
     * @param taskName task name which is appeared in Jobs Explorer Tree
     * @param taskKey task key which identifies the task.
     */
    public void startTask(String taskName, TaskKey taskKey) {
        TaskJob task = new TaskJob(taskName, taskKey);
        synchronized (this) {
            if (tasks.containsKey(taskKey))
                throw new IllegalArgumentException();
            tasks.put(taskKey, task);
        }
        task.startJobOnMyResult();
    }
    
    /**
     * This method is not overriden by subclasses.
     * Override methods prepareTasks, runTask, mergeTaskResults instead.
     * @throws JobException
     */
    @Override
    public final boolean doIt() throws JobException {
        tasks = new LinkedHashMap<TaskKey,TaskJob>();
        prepareTasks();
        (new MergeJob()).startJob();
        return true;
    }
    
    private class TaskJob extends Job {
        private transient final TaskKey taskKey;
        private transient TaskResult taskResult;
        
        private TaskJob(String taskName, TaskKey tK) {
            super(taskName, MultiTaskJob.this.tool, 
                    Job.Type.SERVER_EXAMINE, null, null, Job.Priority.USER);
            this.taskKey = tK;
        }
        
        @Override
        public boolean doIt() throws JobException {
            taskResult = runTask(taskKey);
            return true;
        }
        
        @Override
        public void abort() {
            MultiTaskJob.this.abort();
        }
    }
    
    private class MergeJob extends Job {
        private Result result;
        
        private MergeJob() {
            super(MultiTaskJob.this.ejob.jobName + "merge", MultiTaskJob.this.tool, 
                    Job.Type.CHANGE, null, null, Job.Priority.USER);
        }
        
        @Override
        public boolean doIt() throws JobException {
            LinkedHashMap<TaskKey,TaskResult> taskResults = new LinkedHashMap<TaskKey,TaskResult>();
            for (TaskJob task: tasks.values()) {
                if (task.taskResult != null)
                    taskResults.put(task.taskKey, task.taskResult);
            }
            result = mergeTaskResults(taskResults);
            if (consumer != null)
                consumer.consume(result);
//            fieldVariableChanged("result");
            return true;
        }
        
        @Override
        public void abort() {
            MultiTaskJob.this.abort();
        }
//        /**
//         * This method executes in the Client side after normal termination of doIt method.
//         * This method should perform all needed termination actions.
//         */
//        @Override
//        public void terminateOK() {
//            MultiTaskJob.this.terminateOK(result);
//        }
    }
}
