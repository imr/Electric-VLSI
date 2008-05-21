package com.sun.electric.tool.simulation.eventsim.core.common;
import java.util.LinkedList;

import com.sun.electric.tool.simulation.eventsim.core.common.UnboundedQueue;
import com.sun.electric.tool.simulation.eventsim.core.engine.Command;

/**
 * An unbounded queue that can handle multipe outstanding requests.
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

public class SharedUnboundedQueue<T> extends UnboundedQueue<T> {
	// data queue
	protected LinkedList<T> queue= new LinkedList<T>();
	// command queue
	protected LinkedList<Command> commandQueue= new LinkedList<Command>();
	
	/** clear the queue */
	public void clear() {
		queue.clear();
		commandQueue.clear();
	}
	
	/**
	 * Append an item to the end of the queue
	 * @param item - the item to be appended
	 */
	public void put(T item) {
		queue.addLast(item);			
		// is there an outstanding request for an item
		while (!commandQueue.isEmpty() && !queue.isEmpty()) {
			// there are outstanding commands asking for data
			// note that at most one request can be satisfied, because outstanding
			// commands queue up only when the data queue is empty
			// get the return command
			Command getCommand= commandQueue.removeFirst();
			// return the value of the queue 
			getCommand.trigger(queue.removeFirst());
		}
	} // put
	
	/**
	 * Return the head of the queue if the queue is not empty
	 * If the queue is empty, remember the get so that a response will
	 * be issued when the first element arrives in the queue
	 * @param c
	 */
	public void get(Command c) {
		// put the command in the command queue
		// are there any
		commandQueue.addLast(c);
		while (!queue.isEmpty() && !commandQueue.isEmpty()) {
			Command getCommand= commandQueue.removeFirst();
			// the queue is not empty, respond right away
			getCommand.trigger(queue.removeFirst());
		}
	} // get
	
} // class SharedUnbounded queue
