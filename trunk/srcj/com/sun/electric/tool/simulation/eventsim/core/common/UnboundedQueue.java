package com.sun.electric.tool.simulation.eventsim.core.common;

import com.sun.electric.tool.simulation.eventsim.core.engine.Command;
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
public abstract class UnboundedQueue<T> {

	/** clear the queue */
	abstract public void clear();
	
	/** add an item to the end of the queue */
	abstract public void put(T item);
	
	/** 
	 * Get and remove an item from the queue.
	 * The item will be returned by executing the provided command and the
	 * item will be passed as a parameter to the execute method.
	 * This allows time separation from a get request and the corresponding
	 * response. For example, if at the time of a get request the queue is 
	 * empty, an item will be returned when the first item is added to the queue.
	 * @param c the command used to return the value
	 * @throws EventSimErrorException 
	 */
	abstract public void get(Command c) throws EventSimErrorException;
	
} // UnboundedQueue
