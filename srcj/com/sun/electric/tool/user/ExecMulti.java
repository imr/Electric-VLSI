package com.sun.electric.tool.user;

import java.util.List;
import java.util.ArrayList;

/**
 * Run multiple external processes in sequence (serial).
 * Each external process can have Exec.FinishedListeners attached.
 * Those will get executed before the next process is run, assuming
 * that all processes and finished listeners have been added before
 * calling the start() method of this class.
 */
public class ExecMulti implements Exec.FinishedListener {

    private List<Exec> execs;
    private List<String> preRunComments;
    private List<String> postRunComments;
    private List<Exec.FinishedListener> finishedListeners;
    private List<Exec.FinishedEvent> finishedEvents;
    private int nextExec;

    public ExecMulti() {
        execs = new ArrayList<Exec>();
        preRunComments = new ArrayList<String>();
        postRunComments = new ArrayList<String>();
        finishedListeners = new ArrayList<Exec.FinishedListener>();
        finishedEvents = new ArrayList<Exec.FinishedEvent>();
        nextExec = 0;
    }

    public void addExec(Exec e) {
        addExec(e, "", "");
    }

    public void addExec(Exec e, String preRunComment, String postRunComment) {
        execs.add(e);
        preRunComments.add(preRunComment);
        postRunComments.add(postRunComment);
    }

    public void start() {
        startNext();
    }
    
    private void startNext() {
        if (execs.size() == 0) return;

        if (nextExec >= execs.size()) {
            // done
            for (Exec.FinishedListener l : finishedListeners) {
                l.processFinished(finishedEvents.get(nextExec-1));
            }
            return;
        }

        Exec e = execs.get(nextExec);
        e.addFinishedListener(this);
        String pre = preRunComments.get(nextExec);
        if (pre != null && !pre.equals("")) System.out.println(pre);

        e.start();

        String post = postRunComments.get(nextExec);
        if (post != null && !post.equals("")) System.out.println(post);
    }

    public void processFinished(Exec.FinishedEvent e) {
        finishedEvents.add(e);
        nextExec++;
        startNext();
    }

    // ----------------------------------------------------------

    /**
     * Add a Exec.FinishedListener
     * @param a the listener
     */
    public void addFinishedListener(Exec.FinishedListener a) {
        synchronized(finishedListeners) {
            finishedListeners.add(a);
        }
    }

    /**
     * Remove a Exec.FinishedListener
     * @param a the listener
     */
    public void removeFinishedListener(Exec.FinishedListener a) {
        synchronized(finishedListeners) {
            finishedListeners.remove(a);
        }
    }

/*
    public synchronized void destroyProcess() {
        if (p != null) {
            p.destroy();
        }
    }

    public int getExitVal() { return exitVal; }
*/

}
