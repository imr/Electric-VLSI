/*
 * Created on Nov 5, 2004
 */
package com.sun.electric.tool.simulation.eventsim.core.common;

/**
 *
 * Copyright © 2005 Sun Microsystems Inc. All rights reserved. Use is subject to license terms.
 * 
 * @author ib27688
 *
 * Generate unique IDs
 */
public class IDGenerator {
		
	/** empty constructor. An instance of this class cannot be created */
	private IDGenerator() {
		// empty
	} // constructor
	
	public static long nextID= 0;
	
	
	public static long newID() {
		return nextID++; // note how smart the compiler is!
	} // next ID
	
	/** reset the ID; used for testing only */
	static void reset() {
		nextID= 0;
	}
	
} // class IDGenerator
