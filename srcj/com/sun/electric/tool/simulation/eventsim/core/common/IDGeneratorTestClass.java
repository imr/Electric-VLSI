/*
 * Created on Nov 8, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.sun.electric.tool.simulation.eventsim.core.common;


import junit.framework.TestCase;
import junit.textui.TestRunner;

/**
 *
 * Copyright © 2005 Sun Microsystems Inc. All rights reserved. Use is subject to license terms.
 * 
 * @author ib27688
 *
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
public class IDGeneratorTestClass extends TestCase {	
	
	public static void main(String[] args) {
		TestRunner.run(IDGeneratorTestClass.class);
	}
	
	public void testNewID() {
		// make sure to reset the ID count
		IDGenerator.reset();
		for (int i= 0; i< 20; i++) {
			long id= IDGenerator.newID();
			assertEquals(id, i);
		}
	} // testNewID

} // IDGeneratorTestClass
