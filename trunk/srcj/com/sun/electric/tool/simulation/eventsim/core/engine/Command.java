/*
 * Created on Nov 5, 2004
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All rights reserved. Use is subject to license terms.
 * 
 */
package com.sun.electric.tool.simulation.eventsim.core.engine;

import com.sun.electric.tool.simulation.eventsim.core.globals.Globals;
import com.sun.electric.tool.simulation.eventsim.core.simulation.Delay;



/**
 * Command captures actions associated with events
 * Each component defines its own operation through its commands
 * 
 * @author ib27688
 */

/* Ivan's comments:
 * Command is an abstract class with one method: 
 * execute(Object params).
 * Private sub-classes of Command represent specific
 * things that the simulator must do, such as 
 * activating other parts of the simulation.  
 */

public abstract class Command {
	
	/** access to parameter values */
	protected static Globals globals= Globals.getInstance();	
	/** director */
	protected static Director director= Director.getInstance();
	
	/**
	 * execute the command
	 * Note that execution is always to be triggered by command clients.
	 * The execute itself is to be invoked by simulation director only.
	 * @param params parameters to be passed to the Command
	 */
	protected abstract void execute(Object params) throws EventSimErrorException;
	
	/**
	 * Trigger command execution 
	 *
	 * @param params parameters for command execution
	 * @param d delay for command execution
	 */
	public void trigger(Object params, Delay d) {
		director.schedule(this, params, d);
	} // trigger
	
	/**
	 * Trigger command execution with no delay.
	 * This is useful in simulation - a new event is put on the event
	 * queue and ig guaranteed to be executed only after the triggering/originating
	 * event execution has completed. 
	 * This also helps us guard against stack overflow.
	 * 
	 * @param params
	 */
	public void trigger(Object params) {
		director.schedule(this, params, new Delay(0));
	} // trigger

} // class Command
