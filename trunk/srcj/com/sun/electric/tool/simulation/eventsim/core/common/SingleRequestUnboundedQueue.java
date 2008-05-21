package com.sun.electric.tool.simulation.eventsim.core.common;

import java.util.LinkedList;

import com.sun.electric.tool.simulation.eventsim.core.common.UnboundedQueue;
import com.sun.electric.tool.simulation.eventsim.core.engine.Command;
import com.sun.electric.tool.simulation.eventsim.core.engine.Director;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;


/**
 * An unbounded queue.
 * Elements are returned to the caller via a command.
 * This allows time separation between the get and 
 * the response to the get.
 * I see this as having a request acknowledge interface implemented 
 * at the output end of the queue.
 * Because the queue has no size limit, there is no need for a request
 * acknowledge interface on the input end of the queue.
 * @author ib27688
 *
 * @param <T>
 */

public class SingleRequestUnboundedQueue<T> extends UnboundedQueue<T> {
	
	protected LinkedList<T> queue= new LinkedList<T>();	
	
	protected Command getCommand= null;
	protected boolean outstandingGet= false;
	
	/** clear the queue */
	public void clear() {
		queue.clear();
		outstandingGet= false;
		getCommand= null;
	}
	
	/**
	 * Append an item to the end of the queue
	 * @param item - the item to be appended
	 */
	public void put(T item) {
		queue.addLast(item);			
		// is there an outstanding request for an item
		if (outstandingGet) {
			// this will only happen if the queue was empty when a get was called
			// so this must be the first element added to the queue
			// clear the flag
			outstandingGet= false;
			// return the head of the queue 
			getCommand.trigger(queue.removeFirst());
		}
	} // put
	
	/**
	 * Return the head of the queue if the queue is not empty
	 * If the queue is empty, remember the get so that a response will
	 * be issued when the first element arrives in the queue
	 * @param c
	 * @throws EventSimErrorException 
	 */
	public void get(Command c) throws EventSimErrorException {
		if (!outstandingGet) {
			getCommand= c;
			// anything to return?
			if (queue.isEmpty()) {
				// no, the queue is empty, recall there was a request
				outstandingGet= true;
			}
			else {
				// the queue is not empty, respond right away
				getCommand.trigger(queue.removeFirst());
			}
		}
		else {
			Director.getInstance().fatalError("Queue received two get's in a row without " +
					"a response in between");
		}
	} // get
	
} // class Unbounded queue
