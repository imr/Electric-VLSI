/*
 * Created on Jan 14, 2005
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
 */
public class DelayClassTest extends TestCase {

	/**
	 * Constructor for DelayClasstest.
	 * @param arg0
	 */
	public DelayClassTest(String arg0) {
		super(arg0);
	}

	public static void main(String[] args){
		TestRunner.run(DelayClassTest.class);
	}
	
	/*
	 * Class under test for void Delay()
	 * Constructor creates a non-null object
	 */
	public void testDelay() {
		Delay d= new Delay();
		assertNotNull(d); // the object is not null
		assertEquals(d.value(), 0); // the object value is initiated to 0
	}

	/*
	 * Class under test for void Delay(long)
	 */
	public void testDelaylong() {
		Delay d= new Delay(123);
		assertNotNull(d); // the object is not null
		assertEquals(d.value(), 123); // the object value is initiated to 0
	}

	public void testValue() {
		// test lower bound
		Delay d= new Delay(Long.MIN_VALUE);
		assertEquals(d.value(), Long.MIN_VALUE);
		
		// test a regular value
		d= new Delay(317);
		assertEquals(d.value(), 317);
		
		// test upper bound
		d= new Delay(Long.MAX_VALUE);
		assertEquals(d.value(), Long.MAX_VALUE);
		
	} // testValue


	public void testBeforeThan() {
		Delay d100= new Delay(100);
		Delay d500= new Delay(500);
		
		assertTrue(d100.shorterThan(d500));
		assertFalse(d500.shorterThan(d100));
	} // testBeforeThan

	public void testLaterThan() {
		Delay d100= new Delay(100);
		Delay d500= new Delay(500);
		
		assertFalse(d100.longerThan(d500));

		assertTrue(d500.longerThan(d100));
	} // testLaterThan

	public void testEquals() {
		Delay d100_1= new Delay(100);
		Delay d100_2 = new Delay(100);
		
		Delay d500= new Delay(500);
		
		assertFalse(d100_1.equals(d500));

		assertTrue(d100_1.equals(d100_2));
		assertTrue(d100_1.equals(d100_1));
		
	}

	public void testCompare() {
		Delay d100= new Delay(100);
		Delay d100_1= new Delay(100);
		Delay d500= new Delay(500);
		
		assertEquals(d100.compare(d100_1), 0);
		
		assertEquals(d100.compare(d500), -1);
		
		assertEquals(d500.compare(d100), 1);
	} // testCompare
	
	
	public void testAddDelay() {
		Delay delay100= new Delay(100);

		Delay delayMinus70= new Delay(-70);

		
		// regular case
		Delay d= new Delay(0);
		Delay dPlus= d.addDelay(delay100);
		assertEquals(dPlus.value(), 100);
		Delay dMinus= dPlus.addDelay(delayMinus70);
		assertEquals(dMinus.value(), 30);
				
	} // testAddDelay

	
	public void testAddDelayLong() {
		// regular case
		Delay d= new Delay(0);
		Delay dPlus= d.addDelay(100);
		assertEquals(dPlus.value(), 100);
		Delay dMinus= dPlus.addDelay(-70);
		assertEquals(dMinus.value(), 30);	
	} // testAddDelay

	
	/*
	 * Class under test for String toString()
	 */
	public void testToString() {
		Delay dDef= new Delay();
		assertEquals(dDef.toString(),"0");
		Delay d= new Delay(317);
		assertEquals(d.toString(), "317"  );
	} // testToString

	
} // DelayClassTest
