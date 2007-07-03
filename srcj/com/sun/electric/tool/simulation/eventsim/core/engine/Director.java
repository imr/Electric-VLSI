
package com.sun.electric.tool.simulation.eventsim.core.engine;

import java.util.*;
import java.util.concurrent.SynchronousQueue;

import com.sun.electric.tool.simulation.eventsim.core.globals.Globals;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.Entities;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.Entity;
import com.sun.electric.tool.simulation.eventsim.core.simulation.Delay;
import com.sun.electric.tool.simulation.eventsim.core.simulation.Event;
import com.sun.electric.tool.simulation.eventsim.core.simulation.Time;

// import com.sunlabs.archsim.framework.EventHeapQueue;


/**
 *
 * Copyright © 2005 Sun Microsystems Inc. All rights reserved. Use is subject to license terms.
 * 
 * @author ib27688
 *
 * This is the mediator taking care of driving a simulation 
 * This is a singleton class, instances of Director can only be obtained
 * through class method getInstance 
 */
public class Director {

	public final static String LEGAL_NOTICE= "Copyright © 2005 Sun Microsystems, Inc., 4150 Network Circle, Santa Clara, California 95054, U.S.A. All rights reserved.Sun Microsystems, Inc. has intellectual property rights relating to technology embodied in the product that is described in this document. In particular, and without limitation, these intellectual property rights may include one or more of the U.S. patents listed at http://www.sun.com/patents and one or more additional patents or pending patent applications in the U.S. and in other countries.U.S. Government Rights - Commercial software.  Government users are subject to the Sun Microsystems, Inc. standard license agreement and applicable provisions of the FAR and its supplements.  Use is subject to license terms.  Sun,  Sun Microsystems and  the Sun logo are trademarks or registered trademarks of Sun Microsystems, Inc. in the U.S. and other countries.This product is covered and controlled by U.S. Export Control laws and may be subject to the export or import laws in other countries.  Nuclear, missile, chemical biological weapons or nuclear maritime end uses or end users, whether direct or indirect, are strictly prohibited.  Export or reexport to countries subject to U.S. embargo or to entities identified on U.S. export exclusion lists, including, but not limited to, the denied persons and specially designated nationals lists is strictly prohibited.";
	
	/** the Director manages a queue for events that need to be simulated */
	protected EventQueue queue;
	/** the Director acts as the time keeper */
	protected Time currentTime;
	
	/** the journal of the simulation */
	protected Journal scribe;
	
	/** the number of simulation steps perfomed since last reset */
	protected int noSteps;
	
	volatile protected boolean running= false;
	volatile protected boolean keepRunning= true;
	volatile private boolean extHalt= false;
	/** needed for external halt:
	 * 	external halt will not exit until the director puts a token into the 
	 *  randezvous queue.
	 */
	private SynchronousQueue<Boolean> haltAckQueue= new SynchronousQueue<Boolean>();
	
	/**
	 * Director constructor
	 * Protected so it cannot be invoked directly
	 */
	protected Director() {
		// queue=  new EventPriorityQueue(); 
		queue= new EventListQueue(); 
		currentTime= new Time();
		scribe= SimpleJournal.getInstance();
	} // Director constructor
	
	/**
	 * a pre-built instance of Director that everyone knows about
	 */
	protected static Director myInstance= null;
	
	/** 
	 * @return the instance of the Director
	 */
	public static Director getInstance() {
		if (myInstance == null) myInstance= new Director();
		return myInstance;
	} // get instance
	
	
	/** 
	 * @return a copy of the current time
	 */
	public Time getTime(){
		return (Time) ( currentTime.clone() );
	} // getTime
	
	
	/** Schedule directly execution of a command.
	 * 
	 * @param cmd command to be executed
	 * @param params parameters to the command
	 * @param d delay until the execution of the command
	 */
	public void schedule(Command cmd, Object params, Delay d) {
		Time t= currentTime.addDelay(d);
			if (t.value() >= 0) {
				Event e= new Event(cmd, params, t);
				queue.insertEvent(e);
			}
			else {
				error("Director asked to schedule an event with time out of bound. "
						+ "\nIgnored command: " + cmd 
						+ "\nwith parameters " + params);
			}
	} // schedule
	
	
	/** reset the simulation 
	 * @throws EventSimErrorException */
	public void reset() throws EventSimErrorException {
		//
		// reset the time to zero
		currentTime.set(0);
		noSteps= 0;
		queue.clear();
		keepRunning= true;
		running= false;
		extHalt= false;
		
		// initialize all entities
		Iterator ei= Entities.iterator();
		while (ei.hasNext()) {
			Entity e= (Entity)(ei.next());
			e.init();
		}
	} // reset
	
	/**
	 * Clear all structures/data.
	 *
	 */
	public void masterClear() {
		currentTime.set(0);
		noSteps= 0;
		queue.clear();
		keepRunning= true;
		running= false;
		extHalt= false;
		Entities.clear();
		Globals.getInstance().clear();
	}
	
	/** run for the specified time, or until the simulation queue is empty
	 *  no rewind/reply is supported at the time
	 * 
	 * @param t the time period for which to run a simulation
	 * @return the number of events simulated
	 * @throws EventSimErrorException 
	 */
	public int run(Delay t) throws EventSimErrorException {
		int count= 0;
		running= true;

		// first calculate the time when the simulation needs to end
		Time targetTime= currentTime.addDelay(t);
		// we will not simulate events backward in time
		if (targetTime.compare(currentTime) < 0) return count;
		
		while (!queue.isEmpty() 
				&& targetTime.compare(currentTime) >= 0 && keepRunning ) {
			// get the next event in the queue
			Event next= queue.nextEvent();
			// the current time is the time of the next event, preserve the same object
			currentTime.set(next.getTime().value());
			//execute the event
			next.getCommand().execute(next.getParameters());
			count++;
		}
		running= false;
		if (extHalt) haltAckQueue.add(true);
		return count;
	} // run
	
	/**
	 * Run the simulation until there are no events in the queue
	 * @throws EventSimErrorException 
	 */
	public int run() throws EventSimErrorException {
		int count= 0;
		running= true;
		
		while (!queue.isEmpty() && keepRunning) {
			// get the next event in the queue
			Event next= queue.nextEvent();
			// the current time is the time of the next event, preserve the same object
			currentTime.set(next.getTime().value());
			//execute the event
			next.getCommand().execute(next.getParameters());
			count++;
		}
		running= false;
		if (extHalt) haltAckQueue.add(true);
		return count;
 
	} // run
	
	
	/** run the simulation for the specified number of steps or until the
	 * simulation queue is empty
	 * no rewind/replay is supported at the time
	 * 
	 * @param n number of simulation steps to run
	 * @return the number of steps performed
	 * @throws EventSimErrorException 
	 */
	public int step(int n) throws EventSimErrorException {
		int count= 0;
		running= true;
		// we will not simulate events backward in time
		if (n <= 0) return count;
		while (!queue.isEmpty() && count < n && keepRunning) {
			// get the next event in the queue
			Event next= queue.nextEvent();
			// the current time is the time of the next event, preserve the same object
			currentTime.set(next.getTime().value());
			//execute the event
			next.getCommand().execute(next.getParameters());
			count++;
		}
		running= false;
		if (extHalt) haltAckQueue.add(true);
		
		return count;
	} // run
	
	/**
	 * Run one simulation step, if there is an event on the queue
	 * @return the number of steps ran (1 if there was an event to run, 0 if the event quque was empty
	 * @throws EventSimErrorException 
	 */
	public int step() throws EventSimErrorException {
		return step(1);
	} // step
	
	public void halt() {
		keepRunning= false;
	}
	
	public synchronized void extHalt() {
		try {
			// signal the dircetor to stop the run
			extHalt= true;
			halt();
			if (running) haltAckQueue.take();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	} // extHalt
	
	/** get queue size */
	public int queueSize() {
		return queue.size();
	}

	/** prints queue contents to stdio. for testing */
	public void printQueue() {
		queue.print();
	}
	
	/** simulation log update 
	 * 
	 * @param s string to be written
	 */
	public void journal(String s) {
		scribe.record(currentTime + ": " + s);
	} // journal

	public void journal(String name, int state) {
		scribe.record(currentTime + ":" + name + ":" + state);
	} // journal
	
	/** simulation log update with an error message 
	 * 
	 * @param s string to be written
	 */
	public void error(String s) {
		scribe.error(currentTime + ": ERROR: " + s);
	} // error
	
	/** record informational output */
	public void info(String s) {
		scribe.info(currentTime + " INFO: " + s);
	} // info
	

	/** Invoked when a fatal error took place:
	 *  simulation log is updated with an error message and
	 *  the simulation stops. 
	 * 
	 * @param s string to be written
	 */
	public void fatalError(String s) throws EventSimErrorException {
		error(s);
		// close the logs
		scribe.done();
		// terminate
		throw new EventSimErrorException();
	} // fatalError
	
	
	/** set the journal output to a file */
	public void setJournalFile(String file) {
		scribe.setJournalOutput(file);
	} // setJournalFile

	/** set the error output to a file */
	public void setErrorFile(String file) {
		scribe.setErrorOutput(file);
	} // setErrorFile
	
	/** set the info output to a file */
	public void setInfoFile(String file) {
		scribe.setInfoOutput(file);
	} // setInfoFile
	
	/** set the file for all outputs */
	public void setOutputFile(String file) {
		scribe.setOutput(file);
	} // setOutputFile
	
	
	/** close the journal */
	public void closeJournal() {
		scribe.done();
	} // closeJournal
	
			
} // class Director
