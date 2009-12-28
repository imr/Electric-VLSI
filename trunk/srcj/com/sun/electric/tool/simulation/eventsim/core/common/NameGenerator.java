/*
 * Created on Jan 14, 2005
 */
package com.sun.electric.tool.simulation.eventsim.core.common;


/**
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All rights reserved. Use is subject to license terms.
 * 
 * @author ib27688
 * 
 * This class is used to generate names. 
 * The current implementation is very ad-hoc and is 
 * intended only as a stop-gap measure until we return to this
 * problem.
 * 
 */
public class NameGenerator {
	
	/** Empty constructor. An instance of this class cannot be created. */
	private NameGenerator() {
		// empty
	} // constructor
		
	public static String newName(long id) {
		return "component_" + id;
	} // next ID
	
	public static String newName(String s) {
		return s + IDGenerator.newID();
	}
	
} // class NameGenerator
