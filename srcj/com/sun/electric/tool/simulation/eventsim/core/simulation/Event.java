/*
 * Created on Nov 1, 2004
 *
 */
package com.sun.electric.tool.simulation.eventsim.core.simulation;

import com.sun.electric.tool.simulation.eventsim.core.engine.Command;


/**
 *
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
 * 
 * @author ib27688
 *
 * Representation of an event. An event consists of a Time object
 * that determines when the event needs to take place, and a Command
 * object that defines the functionality that needs to be executed to
 * simulate the event.
 */

/* Ivan's comments:
 *  An Event represents something that is to happen, presumably
 * in the future.  Each instance of Event has a Time, a Command,
 * and an parameter Object which might be null.  
 * 
 * The simulator keeps a queue of Events ordered by their Times.  
 * As simulation progresses new Events enter this queue.  
 * Time advances step-wise to the time of the earliest Event 
 * in the queue which then "happens", which is to say that its
 * Command gets executed.  
 */


public class Event  implements Comparable<Event> {
	
	/** the time when the event is to take place */
	private Time myTime;
	/** Command to be executed when the event takes place */
	private Command myCommand;
	/** parameters to the command */
	private Object params;
	
	public Event(Command c, long t) {
		myTime= new Time(t);
		myCommand= c;
	} // Event constructor
	
	public Event(Command c, Object p, Time t) {
		myTime= t;
		myCommand= c;
		params= p;
	}
	
	/** set parameters to null  */
	public Event(Command c, Time t) {
		myTime= t;
		myCommand= c;
		params= null;
	} 
	
	/** 
	 * Constructor just for testing purposes, no command is used
	 * @param t
	 */
	public Event(long t) {
		myTime= new Time(t);
	}
	
	/** for testing purposes */
	Event(Time t) {
		myTime= t;
	}
	
	/** accessor for the time */
	public Time getTime() {
		return myTime;
	}
	
	/** accessor for the command */
	public Command getCommand() {
		return myCommand;
	}

	/** accessor for the parameters */
	public Object getParameters() {
		return params;
	}

	public String toString() {
		return myTime + " " + params + " " + myCommand; 
	}
	
	public int compareTo(Event e) {
		if (myTime.equals(e.myTime)) {
			return 0;
		}
		else if (myTime.beforeThan(e.myTime)) {
			return -1;
		}
		else {
			return 1;
		}
	}
	
} // class Event
