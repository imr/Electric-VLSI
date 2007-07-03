/*
 * Created on Feb 17, 2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package com.sun.electric.tool.simulation.eventsim.core.engine;

import java.util.Random;

import com.sun.electric.tool.simulation.eventsim.core.simulation.Event;


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
public class EventListQueueTestClass extends TestCase {
	/*
	 * Class under test for void EventQueue()
	 */
	public void testEventQueue() {
		EventListQueue q= new EventListQueue();
		assertNotNull(q.queue);
		assertEquals(q.queue.capacity(), EventListQueue.DEFAULT_CAPACITY);
	} // testEventQueue

	public static void main(String[] args) {
		TestRunner.run(EventListQueueTestClass.class);
	}
	
	/*
	 * Class under test for void EventQueue(int)
	 */
	public void testEventQueueint() {
		EventListQueue q= new EventListQueue(-10);
		assertNotNull(q.queue);
		assertEquals(q.queue.capacity(), EventListQueue.DEFAULT_CAPACITY);

		q= new EventListQueue(1000);
		assertNotNull(q.queue);
		assertEquals(q.queue.capacity(), 1000);
	}

	public void testCapacity() {
		EventListQueue q= new EventListQueue();
		assertNotNull(q.queue);
		assertEquals(q.queue.capacity(), EventListQueue.DEFAULT_CAPACITY);
		
		q= new EventListQueue(-10);
		assertNotNull(q.queue);
		assertEquals(q.queue.capacity(), EventListQueue.DEFAULT_CAPACITY);

		q= new EventListQueue(1000);
		assertNotNull(q.queue);
		assertEquals(q.queue.capacity(), 1000);
		
		q= new EventListQueue();
		// fill up the queue
		for (int i= 0; i< EventListQueue.DEFAULT_CAPACITY; i++) {
			q.insertEvent(new Event(i));
		}
		assertEquals(q.capacity(), EventListQueue.DEFAULT_CAPACITY);
		q.insertEvent(new Event(1000));
		assertEquals(q.capacity(), EventListQueue.DEFAULT_CAPACITY +
				EventListQueue.DEFAULT_INCREMENT);
		
	}

	public void testNextEvent() {
		EventListQueue q= new EventListQueue();
		
		// populate the queue
		Random rnd= new Random();
		for (int i= 0; i< 15; i++) {
			int t= rnd.nextInt();
			q.insertEvent(new Event(t));
		}
		
		// take the events out and check whether they come out in the correct order
		Event current= q.nextEvent();
		// System.out.println("\n\n" + current.getTime().value());
		while (!q.isEmpty()) {
			Event next= q.nextEvent();		
			assertTrue(next.getTime().laterThan(current.getTime()));
			current= next;
		}
	} // testNextEvent

	public void testPeek() {
		EventListQueue q= new EventListQueue();
		
		// populate the queue
		Random rnd= new Random();
		for (int i= 0; i< 15; i++) {
			int t= rnd.nextInt();
			q.insertEvent(new Event(t));
		}
		
		int s= q.size();
		Event e= q.peek();
		assertNotNull(e);
		assertEquals(q.size(), s);
		Event n= q.nextEvent();
		assertEquals(e,n);
	} // testPeek

	public void testInsertEvent() {
		EventListQueue q= new EventListQueue(); // the heap to test inserts
		
		Event refEvent[]= new Event[10]; // reference event array - to be compared with the result
		
		
		// populate the queue, then check whether the correct events come out
		Event e= new Event(100);
		q.insertEvent(e);    // insert the event on the heap
		refEvent[5]= e; // populate the reference array
		
		e= new Event(200);
		q.insertEvent(e);
		refEvent[4]= e;
		
		e= new Event(50);
		q.insertEvent(e);
		refEvent[1]= e;
		
		e= new Event(1000);
		q.insertEvent(e);
		refEvent[3]= e;
		
		e= new Event(25);
		q.insertEvent(e);
		refEvent[0]= e;
		
		e= new Event(75);
		q.insertEvent(e);
		refEvent[2]= e;
		
		
		
		
		// populate the queue - it must grow its capacity
		q= new EventListQueue();
		Random rnd= new Random();
		for (int i= 0; i< EventListQueue.DEFAULT_CAPACITY+1; i++) {
			int t= rnd.nextInt();
			q.insertEvent(new Event(t));
		}		
		assertEquals(q.capacity(), EventListQueue.DEFAULT_CAPACITY
				+ EventListQueue.DEFAULT_INCREMENT);
	}

	public void testSize() {
		EventListQueue q= new EventListQueue();
		assertEquals(q.size(), 0);
		
		// add elements to the queue, check sizes
		int i;
		Random rnd= new Random();
		for (i=0; i< EventListQueue.DEFAULT_CAPACITY+20; i++) {
			Event e= new Event(rnd.nextInt());
			q.insertEvent(e);
			assertEquals(q.size(), i+1);
		}
		
		// now empty the heap, check sizes
		for (;i>0; i--) {
			q.nextEvent();
			assertEquals(q.size(), i-1);
		}		
	} // testSize

	public void testIsEmpty() {
		EventListQueue q= new EventListQueue();
		assertTrue(q.isEmpty());
		
		// add events to the queue, it is not empty any more
		for (int i= 0; i< EventListQueue.DEFAULT_CAPACITY+20; i++) {
			q.insertEvent(new Event(i));
			assertFalse(q.isEmpty());
		}
		
		// now empty the queue, leave only one evenet in there
		for (int i= 0; i< EventListQueue.DEFAULT_CAPACITY+19; i++) {
			q.nextEvent();
			assertFalse(q.isEmpty());
		}
		// take the last event out
		q.nextEvent();
		assertTrue(q.isEmpty());
		
	} // testIsEmpty

	public void testClear() {
		EventListQueue q= new EventListQueue();
		q.clear();
		assertTrue(q.isEmpty());
		
		// add some events to the queue
		for (int i= 0; i< 20; i++) {
			q.insertEvent(new Event(i));
		}
		q.clear();
		assertTrue(q.isEmpty());
	} //testClear

}
