/*
 * Created on Nov 1, 2004
 *
 */
package com.sun.electric.tool.simulation.eventsim.core.simulation;

/**
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All rights reserved. Use is subject to license terms.
 * 
 * Representation of the simulation time
 *
 * @author ib27688
 *
 */
public class Time implements Cloneable {
	
	private long myTime;
	
	/**
	 * default constructor sets the time to 0
	 *
	 */
	public Time() {
		myTime= 0;
	} // Time
	
	public Object clone() {
		return new Time(myTime);
	} // clone
	
	/**
	 * Set the time initially
	 */
	public Time(long t) {
		myTime= t;
	} // Time (long t)
	
	/**
	 * @return the current time value
	 */
	public long value() {
		return myTime;
	} // value
	
	/**
	 * Set the time
	 */
	public void set(long t) {
		myTime= t;
	} // set
	
	/**
	 * 
	 * @param t time to compare to
	 * @return true if this time is earlier or equal than parrameter t
	 */
	public boolean beforeThan(Time t) {
		return myTime <= t.value();
	} // beforeThan
	
	/**
	 * 
	 * @param t time to compare to
	 * @return true if this time is later or equal than parrameter t
	 */
	public boolean laterThan(Time t) {
		return myTime >= t.value();
	} //laterThan
	
	/**
	 * @param t time to compare to
	 * @return true if this time is equal to t
	 */
	public boolean equals(Time t) {
		return myTime == t.value();
	} // equals
	
	/**
	 * @param t time to compare to
	 * @return -1 is this time is earlier than t, 0 is the two times are equal, 
	 * 			1 if this time is later than t
	 */
	public int compare (Time t) {
		if (myTime < t.value()) return -1;
		else if (myTime == t.value()) return 0;
		else return 1;
	} // compare
	
	
/*
	public long addTime(Time t) {
		if (t.value() >=0  && Long.MAX_VALUE - t.value() <= myTime) {
			myTime= Long.MAX_VALUE;	
		}
		else if (t.value() < 0 && Long.MIN_VALUE - t.value() >= myTime) {
			myTime= Long.MIN_VALUE;				
		}
		else {
			myTime= myTime + t.value();
		} 
		return myTime;
	} // addTime
	
	public long addTime(long t) {
		if (t >=0  && Long.MAX_VALUE - t <= myTime) {
			myTime= Long.MAX_VALUE;	
		}
		else if (t < 0 && Long.MIN_VALUE - t >= myTime) {
			myTime= Long.MIN_VALUE;				
		}
		else {
			myTime= myTime + t;
		} 
		return myTime;		
	} // addTime
*/	
	
	public Time addDelay(Delay d) {
		return new Time(myTime + d.value());
	} // addDelay
	
	public Time addDelay(long d) {
		return new Time(myTime + d);
	} // addDelay

	
	public String toString() {
		return (new Long(myTime)).toString();
	} // toString

	

} // class Time
