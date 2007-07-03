package com.sun.electric.tool.simulation.eventsim.infinity.components;

import java.util.LinkedList;
import java.util.Random;

import com.sun.electric.tool.simulation.eventsim.core.common.Parameters;
import com.sun.electric.tool.simulation.eventsim.core.engine.Command;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.CompositeEntity;
import com.sun.electric.tool.simulation.eventsim.core.simulation.Delay;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.component.ComponentWorker;

/**
 * A generic arbiter.
 * Requester provides a command to be executed when the request is granted.
 * This is a ComponentWorker, is intended to be a part of any component
 * that requires internal arbitration.
 * @author ib27688
 */

public class Arbiter extends ComponentWorker {
	
	public static final String ARBITER_DELAY= "arbiterDelay";
	public static final Delay ARBITER_DELAY_DEF= new Delay(0);
	public static final String ARBITER_DELAY_VARIATION= "arbiterDelayVariation";
	public static final int ARBITER_DELAY_VARIATION_DEF= 0;

	/** queu for outstanding request */
	LinkedList<Command> requestQueue= new LinkedList<Command>();
	/** arbiter states */
	static enum ArbiterState {WAIT_REQUEST, WAIT_RELEASE};
	/** arbiter state */
	ArbiterState arbiterState= ArbiterState.WAIT_REQUEST;
	/** arbitration delay */
	Delay delay= ARBITER_DELAY_DEF;
	int delayVariation= ARBITER_DELAY_VARIATION_DEF;
	Random rndGen= new Random();
	
	public Arbiter(String n) {
		super(n);
		setUp();
	}

	public Arbiter(String name, CompositeEntity g) {
		super(name, g);
		setUp();
	}

	// look for parameters
	protected void setUp() {
		Integer AD= globals.intValue(ARBITER_DELAY);
		if (AD != null) {
			delay= new Delay(AD);
		}
		Integer DV= globals.intValue(ARBITER_DELAY_VARIATION);
		if (DV != null) {
			delayVariation= DV;
		}
		
	} // setUp
	
	/** 
	 * set the arbiter delay
	 * useful when an arbiter is embedded in a component
	 * @param d the new delay
	 * @param dv delay variation
	 */
	public void setDelay(Delay d, int dv) {
		delay= d;
		delayVariation= dv;
	} // setDelay
	
	public void setDelay(Delay d) {
		delay= d;
	} // setDelay

	public void setDelayVariation(int dv) {
		delayVariation= dv;
	} // setDelayVariation
	
	/**
	 * Look whether arbitration delay is individualy parametrized and set the
	 * delay accordingly.
	 * @param params
	 * @throws EventSimErrorException 
	 */
	public void processParameters(Parameters params) throws EventSimErrorException {
		if (params != null) {
			for (int i= 0; i< params.size(); i++) {
				String pName= params.getName(i);
				String sValue= params.getValue(i);
				if (pName.equals(ARBITER_DELAY)) {
					try {
						int d= Integer.parseInt(sValue);
						delay= new Delay(d);
					}
					catch (Exception e) {
						fatalError("Non integer value provided for arbiter delay: " + sValue);
					}
				}
				else if (pName.equals(ARBITER_DELAY_VARIATION)) {
					try {
						delayVariation= Integer.parseInt(sValue);
					}
					catch (Exception e) {
						fatalError("Non integer value provided for arbiter delay variation: " + sValue);
					}
				}
				else {
					logError("Parameter " + pName + " unknown, ignored.");					
				}

			} // for
		} // if
	} // processParameters
	
	public void request(Command grantCommand) throws EventSimErrorException {
		if (grantCommand == null) {
			fatalError("null grant command provide with arbitration request");
		}
		else {
			requestQueue.add(grantCommand);
			// is grant to be issued right away?
			if (arbiterState == ArbiterState.WAIT_REQUEST) {
				grant();
			}
		}
	} // request
	
	/** schedule a grant of the request */
	protected void grant() {
		// grant will be scheduled, no going back
		arbiterState= ArbiterState.WAIT_RELEASE;
		// pull the top request from the queue
		Command gc= requestQueue.removeFirst();
		// trigger the grant command
		gc.trigger(null, Delay.randomizeDelay(delay, delayVariation));
	} // grant
	
	/**
	 * Release the arbiter.
	 * @param w component worker that had the last request granted
	 * @throws EventSimErrorException 
	 */
	public void release() throws EventSimErrorException {
		if (arbiterState != ArbiterState.WAIT_RELEASE) {
			fatalError("Arbiter release attempted when no request had been issued. ");
		}
		else {
			if (requestQueue.isEmpty()) {
				// no pending
				arbiterState= ArbiterState.WAIT_REQUEST;				
			}
			else {
				// the request queue is not empty, grant the next request in the queue
				grant();
			}
		} // else
	} // release
	
	@Override
	public void init() {
		requestQueue.clear();
		arbiterState= ArbiterState.WAIT_REQUEST;
	} // init

	@Override
	public boolean selfCheck() {
		return true;
	} // selfCheck

	// used for the request queue
	protected class WorkerCommandPair {
		ComponentWorker worker;
		Command command;
		
		WorkerCommandPair(ComponentWorker w, Command c) {
			worker= w;
			command= c;
		} // WorkerCommandPair
		
	} // class WorkerCommandPair
	
} // class Arbiter     
