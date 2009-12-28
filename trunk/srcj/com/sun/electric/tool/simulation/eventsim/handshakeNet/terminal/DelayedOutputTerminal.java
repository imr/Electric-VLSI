package com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal;

import com.sun.electric.tool.simulation.eventsim.core.engine.Command;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.CompositeEntity;
import com.sun.electric.tool.simulation.eventsim.core.simulation.Delay;

/**
 * OutputTerminal that honors terminal delays.
 * @author ib27688
 */

public class DelayedOutputTerminal extends OutputTerminal {

	/** 
	 * To be engaged when an output becomes available -
	 * enables introducing a delay within the terminal.
	 */
	protected Command outputAvailableCmd= new OutputAvailableCommand();
	/** 
	 * This command is engaged when an output is retracted.
	 * Not in use now as this feature is not implemented.
	 */
	protected Command outputUnavailableCmd= null;
	
	public DelayedOutputTerminal(String n) {
		super(n);
	} // constructor

	public DelayedOutputTerminal(String name, CompositeEntity g) {
		super(name, g);
	} // constructor

	@Override
	public void outputAvailable(Object data) {
		// log that the output became available
		logOutputAvailable(data);
		// remember the data
		myData= data;
		// no ack received for this output
		ackHere= false;
		// schedule output delivery to the attached channel
		outputAvailableCmd.trigger(data, Delay.randomizeDelay(outputAvailableDelay, outputAvailableDelayVariation));
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
		outputAvailableCmd.trigger(data, Delay.randomizeDelay(outputAvailableDelay.addDelay(d), outputAvailableDelayVariation));
	} // outputAvailable
	
	@Override
	public void outputUnavailable() {
		logError("Output unavailable not implemented, requested by "
				+ "ComponentWorker " + myWorker.getAlias() 
				+ ", " + myWorker.getID());
	} // outputUnavailable

	/**
	 * Called by the channel to acknowledge output.
	 */
	@Override
	public void outputAck() {
		// log that the outout has been acknowledged
		logAckOutput();
		// the ack has arrived
		ackHere= true;
		// schedule notification
		outputAckCmd.trigger(this, Delay.randomizeDelay(outputAckDelay, outputAckDelayVariation));
	} // outputAck;

	@Override
	public void init() {
		// initially able to produce an output
		ackHere= true;
	} // init
	
	@Override
	public boolean selfCheck() {
		boolean check= super.selfCheck();
		if (outputAvailableCmd == null) {
			logError("Self check failed: outputAvailableCmd == null");
			check= false;
		}
		// Not implemented
		// check= check && (outputUnvailableCmd != null);
		return check;
	} // selfCheck

	/**
	 * A command to be executed when an output is available.
	 * This command allows a delay between the time the terminal has
	 * been notified of an output and the time when the channel is
	 * notified that an output is available. 
	 * @author ib27688
	 */
	protected class OutputAvailableCommand extends Command {
		public void execute(Object data) throws EventSimErrorException {
			myChannel.outputAvailable(DelayedOutputTerminal.this, data);
		} // execuute
	} // class 
	
} // class DelayedOutputTerminal
