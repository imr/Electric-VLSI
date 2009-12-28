package com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal;

import com.sun.electric.tool.simulation.eventsim.core.engine.Command;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.CompositeEntity;
import com.sun.electric.tool.simulation.eventsim.core.simulation.Delay;

public class InstantInputTerminal extends InputTerminal {

	/** needed when a delay is explicitly requested for the ack */
	protected Command inputAckCmd= new InputAckCommand();
	
	public InstantInputTerminal(String n) {
		super(n);
	}

	public InstantInputTerminal(String name, CompositeEntity g) {
		super(name, g);
	}

	@Override
	public void inputAvailable(Object data) {
		logInputAvailable();
		inputHere= true;
		inputAvailableCmd.trigger(data);
	} // inputAvailable

	@Override
	public void inputUnavailable() throws EventSimErrorException {
		fatalError("Output unavailable not implemented, requested by "
				+ "channel " + myChannel.getAlias() 
				+ ", " + myChannel.getID());
	} //inputUnavailable

	@Override
	public void ackInput() throws EventSimErrorException {
		logAckInput();
		inputHere= false;
		myChannel.ackInput(InstantInputTerminal.this);
	} // ackInput

	@Override
	public void ackInput(Delay d) {
		logAckInput();
		inputHere= false;
		inputAckCmd.trigger(this, d);
	} // ackInput
	
	
	@Override
	public void init() {
		inputHere= false;
	} // init

	protected class InputAckCommand extends Command {
		public void execute(Object data) throws EventSimErrorException {
			myChannel.ackInput(InstantInputTerminal.this);
		}
	} // inputAckCommand
	
} // class InstantInputTerminal
