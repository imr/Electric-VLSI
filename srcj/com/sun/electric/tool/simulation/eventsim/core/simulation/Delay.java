/*
 * Created on Jan 14, 2005
 *
 */
package com.sun.electric.tool.simulation.eventsim.core.simulation;

import java.util.Random;

/**
 *
 * Copyright © 2005 Sun Microsystems Inc. All rights reserved. Use is subject to license terms.
 * 
 * @author ib27688
 *
 * Representation of a delay.
 * 
 */ 
 
/* Ivan's notes: 
 * Class Delay and class Time are closely related.  
 *  An instance of class Time represents a moment in time.  
 *  Long ago we though of making Time represent instead a time interval
 * to represent uncertainty, but the present implementation represents
 * a only moment.  Time is always a positive long integer.  It is 
 * measured in arbitrary units from the start of simulation.  
 *
 * Although Delay has the same units as Time, an instance
 * of Delay represents how long something will take regardless of when
 * the action starts.  Negative Delays [are/arenot] permitted.
 *
 * Thus one can add a Delay and a Time to get a Time, or 
 * one can add a Delay to a Delay to get a Delay.  It makes no 
 * sense to add a Time to a Time.  The difference between two
 * Times is a Delay.  
 * 
 * Compare a Time to another Time with the methods "earlierThan" and 
 * "laterThan" and "equals".  
 * Compare a Delay to another with the methods "longerThan"  and
 * "shorterThan" and "equals".  
 * It makes no sense to compare a Delay to a Time.   
 */

public class Delay implements Cloneable {
	
	protected long myDelay;
	// needed for delay randomization
	protected static Random rndGen= new Random();
	
	/**
	 * default constructor sets the delay to 0
	 *
	 */
	public Delay() {
		myDelay= 0;
	} // Delay
	
	public Object clone() {
		return new Delay(myDelay);
	} // clone
	
	/**
	 * Set the delay initially
	 */
	public Delay(long delay) {
		myDelay= delay;
	} // Delay (long d)
	
	/**
	 * @return the current delay value
	 */
	public long value() {
		return myDelay;
	} // value
	
	/**
	 * 
	 * @param d delay to compare to
	 * @return true if this delay is earlier or equal than parrameter d
	 */
	public boolean shorterThan(Delay d) {
		return myDelay <= d.value();
	} // beforeThan
	
	/**
	 * 
	 * @param d delay to compare to
	 * @return true if this delay is later or equal than parrameter d
	 */
	public boolean longerThan(Delay d) {
		return myDelay >= d.value();
	} //laterThan
	
	/**
	 * @parem d delay to compare to
	 * @return true if this delay is equal to d
	 */
	public boolean equals(Delay d) {
		return myDelay == d.value();
	} // equals
	
	/**
	 * @param d delay to compare to
	 * @return -1 is this delay is earlier than d, 0 is the two delays are equal, 
	 * 			1 if this delay is later than d
	 */
	public int compare (Delay d) {
		if (myDelay < d.value()) return -1;
		else if (myDelay == d.value()) return 0;
		else return 1;
	} // compare
	
	
	/**
	 * Add delay to current delay.	 * 
	 * @param d delay to be added to this delay
	 * @return the resulting delay
	 */
	public Delay addDelay(Delay del) {
		return new Delay(myDelay + del.value());
	} // addDelay

	public Delay addDelay(long d) {
		return new Delay(myDelay + d);
	} // adddelay
	
	
	public String toString() {
		return (new Long(myDelay)).toString();
	} // toString

//	/**
//	 * Calculate a random delay within the boundaries of delay variation.
//	 * @param defDelay base delay
//	 * @return randomized delay
//	 */
//	public static Delay randomizeDelay0(Delay defDelay, int delayVariation) {
//		Delay d= defDelay;
//		if (delayVariation > 0) {
//			// get the randomized delay variation
//			int delayVar= rndGen.nextInt(2*delayVariation) - delayVariation;
//			long newDelay= defDelay.value()-delayVar;
//			if (newDelay < 0) newDelay= 0;
//			// add the variation to the delay
//			d= new Delay(newDelay);
//		}
//		return d;
//	} // randomizeDelay

	/**
	 * Calculate a random delay within the boundaries of delay variation.
	 * @param defDelay base delay
	 * @return randomized delay
	 */	
	public static Delay randomizeDelay(Delay defDelay, int delayVariation) {
		Delay d= defDelay;
		if (delayVariation > 0) {
			// get the randomized delay variation
			int delayVar= rndGen.nextInt(delayVariation);
			long newDelay= defDelay.value() - delayVariation/2 + delayVar;
			if (newDelay < 0) newDelay= 0;
			// add the variation to the delay
			d= new Delay(newDelay);
		}
		return d;
	} // randomizeDelay
	
} // class Delay

