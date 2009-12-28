package com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal;

import com.sun.electric.tool.simulation.eventsim.core.engine.Command;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.CompositeEntity;
import com.sun.electric.tool.simulation.eventsim.core.simulation.Delay;

/**
 * OutputTerminal that ignores terminal delays - the output and
 * the acknowledgments are passed on immediately.
 * Note that no events/commands are scheduled, therefore one needs to 
 * be careful to avoid stack overflow.
 * 
 * @author ib27688
 *
 */
public class InstantOutputTerminal extends OutputTerminal {
	
	/** 
	 * To be engaged when an output becomes available and
	 * and additional delay is requested
	 */
	protected Command outputAvailableCmd= new OutputAvailableCommand();
	
	public InstantOutputTerminal(String n) {
		super(n);
	} // constructor

	public InstantOutputTerminal(String name, CompositeEntity g) {
		super(name, g);
	} // constructor

	@Override
	public void outputAvailable(Object data) throws EventSimErrorException {
		// log that the output became available
		logOutputAvailable(data);
		// remember the data
		myData= data;
		// no ack received for this output
		ackHere= false;
		//inform the channel immediately
		myChannel.outputAvailable(InstantOutputTerminal.this, data);
	} // outputAvailable
	

	@Override
	public void outputAvailable(Object data, Delay d) {
		// log that the output became available
		logOutputAvailable(data);
		// remember the data
		myData= data;
		// no ack received for this output
		ackHere= false;
		// schedule output delivery to the attached channel
		outputAvailableCmd.trigger(data, d);
	} // outputAvailable

	@Override
	public void outputUnavailable() throws EventSimErrorException {
		fatalError("Output unavailable not implemented, requested by "
				+ "ComponentWorker " + myWorker.getAlias() 
				+ ", " + myWorker.getID());
	}

	@Override
	public void outputAck() {
		// log that the outout has been acknowledged
		logAckOutput();
		// the ack has arrived
		ackHere= true;
		// schedule notification with no delay
		outputAckCmd.trigger(this);	
	}


	@Override
	public void init() {
		// nothing to do
	} // init


	/**
	 * A command to be executed when an output is available.
	 * This command allows a delay between the time the terminal has
	 * been notified of an output and the time when the channel is
	 * notified that an output is available. 
	 * @author ib27688
	 */
	protected class OutputAvailableCommand extends Command {
		public void execute(Object data) throws EventSimErrorException {
			myChannel.outputAvailable(InstantOutputTerminal.this, data);
		} // execuute
	} // class 
	
} // class InstantOutputTerminal
