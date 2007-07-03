/*
 * Created on Nov 2, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.sun.electric.tool.simulation.eventsim.core.simulation;

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
public class TimeClassTest extends TestCase {


	/**
	 * Constructor for TimeClasstest.
	 * @param arg0
	 */
	public TimeClassTest(String arg0) {
		super(arg0);
	}

	public static void main(String[] args) {
		TestRunner.run(TimeClassTest.class);
	}		
	
	/*
	 * Class under test for void Time()
	 * Constructor creates a non-null object
	 */
	public void testTime() {
		Time t= new Time();
		assertNotNull(t); // the object is not null
		assertEquals(t.value(), 0); // the object value is initiated to 0
	}

	/*
	 * Class under test for void Time(long)
	 */
	public void testTimelong() {
		Time t= new Time(123);
		assertNotNull(t); // the object is not null
		assertEquals(t.value(), 123); // the object value is initiated to 0
	}

	public void testValue() {
		// test lower bound
		Time t= new Time(Long.MIN_VALUE);
		assertEquals(t.value(), Long.MIN_VALUE);
		
		// test a regular value
		t= new Time(317);
		assertEquals(t.value(), 317);
		
		// test upper bound
		t= new Time(Long.MAX_VALUE);
		assertEquals(t.value(), Long.MAX_VALUE);
		
	} // testValue

	public void testSet() {
		Time t= new Time();
		
		// test lower bound
		t.set(Long.MIN_VALUE);
		assertEquals(t.value(), Long.MIN_VALUE);
		
		// test a regular value
		t.set(317);
		assertEquals(t.value(), 317);
		
		// test upper bound
		t.set(Long.MAX_VALUE);
		assertEquals(t.value(), Long.MAX_VALUE);
	} // testSet

	public void testBeforeThan() {
		Time t100= new Time(100);
		Time t500= new Time(500);
		
		assertTrue(t100.beforeThan(t500));
		assertFalse(t500.beforeThan(t100));
	} // testBeforeThan

	public void testLaterThan() {
		Time t100= new Time(100);
		Time t500= new Time(500);
		
		assertFalse(t100.laterThan(t500));

		assertTrue(t500.laterThan(t100));
	} // testLaterThan

	public void testEquals() {
		Time t100_1= new Time(100);
		Time t100_2 = new Time(100);
		
		Time t500= new Time(500);
		
		assertFalse(t100_1.equals(t500));

		assertTrue(t100_1.equals(t100_2));
		assertTrue(t100_1.equals(t100_1));
		
	}

	public void testCompare() {
		Time t100= new Time(100);
		Time t100_1= new Time(100);
		Time t500= new Time(500);
		
		assertEquals(t100.compare(t100_1), 0);
		
		assertEquals(t100.compare(t500), -1);
		
		assertEquals(t500.compare(t100), 1);
	} // testCompare
	
	
	public void testAddDelay() {
		Delay time100= new Delay(100);
		Delay timeMinus70= new Delay(-70);
		
		// regular case, boundary conditions not implemented
		// as we are not worried about time crossing Long.MAX_VALUE
		Time t= new Time(0);
		Time t1= t.addDelay(time100);
		assertEquals(t1.value(), 100);
		t1= t1.addDelay(timeMinus70);
		assertEquals(t1.value(), 30);
		
	} // testAddDelay

	
	public void testAddDelayLong() {
		// regular case, boundary conditions not implemented
		// as we are not worried about time crossing Long.MAX_VALUE
		// regular case
		Time t= new Time(0);
		Time t1= t.addDelay(100);
		assertEquals(t1.value(), 100);
		t1= t1.addDelay(-70);
		assertEquals(t1.value(), 30);
		
	} // testAddDelay

	
	/*
	 * Class under test for String toString()
	 */
	public void testToString() {
		Time tDef= new Time();
		assertEquals(tDef.toString(),"0");
		Time t= new Time(317);
		assertEquals(t.toString(), "317"  );
	} // testToString

} // TimeClassTest
