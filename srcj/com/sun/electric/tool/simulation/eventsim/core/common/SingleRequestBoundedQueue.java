package com.sun.electric.tool.simulation.eventsim.core.common;

import java.util.LinkedList;

import com.sun.electric.tool.simulation.eventsim.core.engine.Command;
import com.sun.electric.tool.simulation.eventsim.core.engine.Director;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;



/**
 * An bounded queue with at most one outstanding get and put request.
 * A request to add an element to the queue is acknowledged via a command.
 * Elements are returned to the caller via a command.
 * This allows time separation between the get and 
 * the response to the get.
 * I see this as having a request acknowledge interface implemented 
 * at the input and output end of the queue.
 * @author ib27688
 *
 */
public class SingleRequestBoundedQueue<T> {
	
	private LinkedList<T> queue= new LinkedList<T>();
	private int capacity;
	private Command ackGetCmd= null;
	private Command ackPutCmd= null;
	
	public SingleRequestBoundedQueue(int maxC) {
		capacity= (maxC > 1)?  maxC : 1;
	}
	
	/**
	 * Append an item to the end of the queue
	 * @param item - the item to be appended
	 */
	public void put(T item, Command ackCmd) throws EventSimErrorException {
		if (ackPutCmd != null) {
			Director.getInstance().fatalError("Cannot add an item to a full queue.");
		}
		// first add an item
		queue.addLast(item);
		if (queue.size() < capacity && ackCmd != null) {
			// there is more space, acknowledge
			ackCmd.trigger(null);
		}
		else {
			// do not acknowledge until there is space for another entry
			ackPutCmd= ackCmd;
		}
		// is there an outstanding request for an item
		if (ackGetCmd != null) {
			// this will only happen if the queue was empty when a get was called
			// so this must be the first element added to the queue
			ackGetCmd.trigger(queue.removeFirst());
			// clear the command
			ackGetCmd= null;
		}
	} // put
	
	/**
	 * Return the head of the queue if the queue is not empty
	 * If the queue is empty, remember the get so that a response will
	 * be issued when the first element arrives in the queue
	 * @param c
	 * @throws EventSimErrorException 
	 */
	public void get(Command ackCmd) throws EventSimErrorException {
		if (ackGetCmd != null) {
			Director.getInstance().fatalError("Queue received two get's in a row without " +
			"an acknowledgment in between");			
		}
				
		// anything to return?
		if (queue.isEmpty()) {
			// no, the queue is empty, recall there was a request
			ackGetCmd= ackCmd;
		}
		else {
			// the queue is not empty, take an element out right away
			T item= queue.removeFirst();
			// the response cmd is not null, return the removed value
			if (ackCmd != null) {
				ackCmd.trigger(item);
			}
			// is there an unacknowledged put?
			if (ackPutCmd != null) {
				// acknowledge and erase ackPutCmd
				ackPutCmd.trigger(null);
				ackPutCmd= null;
			}
		}
	} // get

	
	public void setCapacity(int maxC) {
		capacity= maxC;
	}
	
	public int capacity() {
		return capacity;
	}
	
	public int size() {
		return queue.size();
	}
	
	public void clear() {
		queue.clear();
	}
	
} // class SingleRequestBoundedQueue
