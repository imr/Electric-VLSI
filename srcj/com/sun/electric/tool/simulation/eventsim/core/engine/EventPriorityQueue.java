/**
 * Copyright 2005, Oracle and/or its affiliates. All rights reserved.
 */
package com.sun.electric.tool.simulation.eventsim.core.engine;

import java.util.PriorityQueue;

import com.sun.electric.tool.simulation.eventsim.core.simulation.Event;


/**
 * @author ib27688
 *
 */
public class EventPriorityQueue extends EventQueue {
	
	PriorityQueue<Event> myQueue;
	
	public EventPriorityQueue() {
		myQueue= new PriorityQueue<Event>();
	}
	
	public int capacity() {
		return Integer.MAX_VALUE;
	}

	public Event nextEvent() {
		return myQueue.poll();
	}
	
	public Event peek() {
		return myQueue.peek();
	}
	
	public void insertEvent(Event newEvent) {
		myQueue.add(newEvent);
	}
	
	public int size() {
		return myQueue.size();
	}
	
	public boolean isEmpty() {
		return myQueue.isEmpty();
	}
	
	public void clear() {
		myQueue.clear();
	}
	
	public void print() {
		System.out.println("EventPriorityQueue:");
		for (Event e : myQueue) {
			System.out.println(e);
		}
		System.out.println();
	}
	
	public String toString() {
		return "EventPriorityQueue: " + myQueue.toString();
	}
	
} // EventPriorityQueue
