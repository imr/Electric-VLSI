package com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal;

import com.sun.electric.tool.simulation.eventsim.core.engine.Command;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.CompositeEntity;
import com.sun.electric.tool.simulation.eventsim.core.simulation.Delay;

public class DelayedInputTerminal extends InputTerminal {

	protected Command inputAckCmd= new InputAckCommand();
	
	public DelayedInputTerminal(String n) {
		super(n);
	} // constructor

	public DelayedInputTerminal(String name, CompositeEntity g) {
		super(name, g);
	} // constructor

	/**
	 * Notify the worker that an input is available.
	 * This is done by scheduling execution of a command 
	 * that the worker provided to the terminal.
	 */
	@Override
	public void inputAvailable(Object data) {
		logInputAvailable();
		inputHere= true;
		inputAvailableCmd.trigger(data, Delay.randomizeDelay(inputAvailableDelay, inputAvailableDelayVariation));
	}

	@Override
	public void inputUnavailable() {
		logError("Output unavailable not implemented, requested by "
				+ "channel " + myChannel.getAlias() 
				+ ", " + myChannel.getID());
	} // inputUnavailable

	/**
	 * Acknowledge receipt of input.
	 * The Ack is delayed by the delay associated with this terminal.
	 * This method logs only at the invocation.
	 * Should the command also produce a log?
	 */
	@Override
	public void ackInput() {
		logAckInput();
		inputHere= false;
		inputAckCmd.trigger(this, Delay.randomizeDelay(inputAckDelay, inputAckDelayVariation));
	} // ackInput
	
	/**
	 * Ack with an extrra delay.
	 */
	@Override
	public void ackInput(Delay d) {
		logAckInput();
		inputHere= false;
		inputAckCmd.trigger(this, Delay.randomizeDelay(inputAckDelay.addDelay(d), inputAckDelayVariation));
	} // ackInput
	
	/** Initialize upon reset */
	@Override
	public void init() {
		inputHere= false;
	} // init
	
	/**
	 * Check whether connections are consistent;
	 * Make sure all commands are defined.
	 */
	public boolean selfCheck() {
		boolean check= super.selfCheck();
		
		if (inputAckCmd == null) {
			logError("Self check failed: inputAckCmd == null");
			check= false;
		}
		
		return check;
	} // selfCheck

	protected class InputAckCommand extends Command {
		public void execute(Object data) throws EventSimErrorException {
			myChannel.ackInput(DelayedInputTerminal.this);
		}
	} // inputAckCommand
	
} // class DelayedInputTerminal
