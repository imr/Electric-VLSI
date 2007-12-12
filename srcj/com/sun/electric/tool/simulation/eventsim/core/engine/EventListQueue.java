/*
 * Created on Feb 17, 2005
 *
 */
package com.sun.electric.tool.simulation.eventsim.core.engine;

import java.util.Vector;

import com.sun.electric.tool.simulation.eventsim.core.simulation.Event;
import com.sun.electric.tool.simulation.eventsim.core.simulation.Time;



/**
 *
 * Copyright © 2005 Sun Microsystems Inc. All rights reserved. Use is subject to license terms.
 * 
 * Implement Event queue with a linked list - use a Vector
 * 
 * @author ib27688
 *
 *
 */
public class EventListQueue extends EventQueue {
	
	public static final int DEFAULT_CAPACITY= 100;
	public static final int DEFAULT_INCREMENT= 50;
	
	protected Vector<Event> queue;
	
	/**
	 * Make an event queue of the default initial capacity
	 */
	public EventListQueue() {
		queue= new Vector<Event>(DEFAULT_CAPACITY, DEFAULT_INCREMENT);
	} // event queue constructor
	
	/**
	 * Make an event queue with the specified capacity
	 * @param capacity Initial queue capacity
	 */
	public EventListQueue(int capacity) {
		if (capacity < DEFAULT_CAPACITY) 
			capacity= DEFAULT_CAPACITY;
		queue= new Vector<Event>(capacity, DEFAULT_INCREMENT);
	} // EventHeap(int)
	
	/**
	 * Obtain current queue capacity
	 * @return current queue capacity
	 */
	public int capacity() {
		return queue.capacity();
	} // capacity
	
	/** 
	 * Remove the next event from the queue and pass it on for execution
	 * @return the next event from the queue
	 */	
	public Event nextEvent() {
		if (queue.size() == 0) return null;
		else return (Event) queue.remove(0);
	} // nextEvent
	
	public Event peek() {
		if (queue.size() == 0) return null;
		else return (Event) queue.get(0);
	} // peek	
	
	
	/**
	 * Insert a new event in the queue
	 * @param e the new event to be inserted to the queue
	 */	
	public void insertEvent(Event e) {
		Time t= e.getTime();
		boolean search= true;
		int index= 0;
		while (search && index < queue.size()) {
			Event x= (Event)queue.get(index);
			Time xt= x.getTime();
			if (xt.compare(t) > 0) {
				// found the position - the last event with time t
				search= false;
			}
			else {
				// move on
				index++;
			}
		} // while
		queue.add(index, e);
	} // InsertEvent
	
	/**
	 * 
	 * @return the size of the queue
	 */
	public int size() {
		return queue.size();
	} // size
	
	/**
	 * Is the queue empty 
	 * @return the boolean indicating whether the queue is empty
	 */
	public boolean isEmpty() {
		return (0 == size());
	} // is Empty
	
	/**
	 * clear the queue
	 *
	 */
	public void clear() {
		queue.clear();
	} // clear	
	
	
	public void print() {
		System.out.println("Queue:");
		for (int i=0; i< queue.size(); i++) {
			Event x= (Event)queue.get(i);
			System.out.println(x);
		}
		System.out.println();
	} // print
	
} // EventListQueue
