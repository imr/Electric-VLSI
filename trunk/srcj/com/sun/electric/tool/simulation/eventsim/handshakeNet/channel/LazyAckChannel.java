package com.sun.electric.tool.simulation.eventsim.handshakeNet.channel;

import com.sun.electric.tool.simulation.eventsim.core.engine.Command;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.CompositeEntity;
import com.sun.electric.tool.simulation.eventsim.core.simulation.Delay;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.InputTerminal;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.OutputTerminal;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.Terminal;


public class LazyAckChannel extends Channel {

	/** the input terimnal */
	protected InputTerminal inputTerminal= null;
	/** the output terminal */
	protected OutputTerminal outputTerminal= null;
	// inform the data destination (input terminal) that data is available
	protected Command inputAvailableCmd= new InputAvailableCommand();
	// inform the data source (output terminal) that the data has been received
	protected Command outputAckCmd= new OutputAckCommand();	
	
	public LazyAckChannel(String n) {
		super(n);
	}

	public LazyAckChannel(String name, CompositeEntity g) {
		super(name, g);
	}

	@Override
	public void ackInput(InputTerminal t) throws EventSimErrorException {
		ackOutput();
	} // ackInput

	@Override
	protected void ackOutput() throws EventSimErrorException {
		outputAckCmd.trigger(null, Delay.randomizeDelay(outputAckDelay, outputAckDelayVariation));
	} // ackOutput

	/**
	 * Attach an input terminal.
	 * Succeeds only if the new terminal is not null;
	 * @param t terminal to be attached
	 * @return true if the new terminal was attached, false if not
	 */
	@Override	
	public boolean attach(InputTerminal t) {
		boolean returnValue= false;
		if (t != null) {
			inputTerminal= t;
			t.setChannel(this);
			returnValue= true;
		}
		return returnValue;
	} // attach

	/**
	 * Attach an output terminal.
	 * Succeeds only if thenew terminal is not null
	 * @param t terminal to be attached
	 * @return true if the new terminal was attached, false if not
	 */
	@Override
	public boolean attach(OutputTerminal t) {
		boolean returnValue= false;
		if (t != null) {
			outputTerminal= t;
			t.setChannel(this);
			returnValue= true;
		}
		return returnValue;
	} // attach

	@Override
	protected void distributeInputs() {
		inputAvailableCmd.trigger(inputTerminal, Delay.randomizeDelay(outputAvailableDelay, outputAvailableDelayVariation));
	} // distributeInputs

	@Override
	public boolean isAttached(Terminal t) {
		return (t == inputTerminal) || (t == outputTerminal);
	} // isAttached

	@Override
	public void outputAvailable(OutputTerminal t, Object data) throws EventSimErrorException {
		if ( t == outputTerminal) {
			// send data to attached input terminals
			myData= data;
			distributeInputs();
		}
		else {
			fatalError("Output arrived from a non-attached terminal " + t);
		}
	} // outputAvailable

	@Override
	public void outputUnavailable(OutputTerminal t) throws EventSimErrorException {
		fatalError("Output unavailable not implemented, requested by "
				+ "terminal " + t.getAlias() + ", " + t.getID());
	} // outputUnavalable

	@Override
	public void init() throws EventSimErrorException {
		if (channelInitialized) triggerInitialization();
	} // init

	@Override
	public boolean selfCheck() {
		boolean check= true;
		check= check && inputTerminal.isAttached(this);
		check= check && outputTerminal.isAttached(this);
		check= check && (outputAckCmd != null);
		check= check && (inputAvailableCmd != null);
		return check;
	}

	/**
	 * Inform the input terminal that it has an input waiting for them.
	 * @author ib27688
	 *
	 */
	protected class InputAvailableCommand extends Command {
		public void execute(Object data) {
			// log that the input was passed on
			logInputNotified(inputTerminal, myData);
			// notify the input terminal
			inputTerminal.inputAvailable(myData);
			logEvent("Terminal " 
					+ inputTerminal.getName() + ", " 
					+ inputTerminal.getID()+ ", "
					+ "informed that input data is available" + myData);
		} // execute
	} // class InputAvailableCommand
	
	/**
	 * Inform the OutputTerminal that was the last data source 
	 * that the data has been received and the it can produce new data.
	 * @author ib27688
	 *
	 */
	protected class OutputAckCommand extends Command {
		public void execute(Object data) {
			// log that the output was acknowledged
			logOutputAcknowledged(outputTerminal);
			// inform the output that the cycle is complete
			outputTerminal.outputAck();
		} // execute
	} // class OutputAckCommand
	
} // class LazyAckChannel
