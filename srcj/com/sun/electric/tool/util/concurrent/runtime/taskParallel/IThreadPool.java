package com.sun.electric.tool.util.concurrent.runtime.taskParallel;

import com.sun.electric.tool.util.concurrent.patterns.PTask;

public interface IThreadPool {
    /**
     * start the thread pool
     */
    public void start();
    
    /**
     * shutdown the thread pool
     */
    public void shutdown() throws InterruptedException;


    /**
     * wait for termination
     * 
     * @throws InterruptedException
     */
    public void join() throws InterruptedException;

    /**
     * Set thread pool to state sleep. Constraint: current State = started
     */
    public void sleep();

    /**
     * Wake up the thread pool. Constraint: current State = sleeps
     */
    public void weakUp();

    /**
     * trigger workers (used for the synchronization)
     */
    public void trigger();

    /**
     * add a task to the pool
     * 
     * @param item
     */
    public void add(PTask item);

    /**
     * add a task to the pool
     * 
     * @param item
     */
    public void add(PTask item, int threadId);

    /**
     * 
     * @return the current thread pool size (#threads)
     */
    public int getPoolSize();
    
    

}
