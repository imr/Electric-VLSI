package com.sun.electric.tool.simulation.eventsim.handshakeNet.channel;

import com.sun.electric.tool.simulation.eventsim.core.engine.Command;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.CompositeEntity;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.InputTerminal;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.OutputTerminal;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.Terminal;


/**
 * A channel that has only one input and one output terminal attached to it.
 * @author ib27688
 */
public class PointToPointChannel extends Channel {
	
	static enum State { WAIT_DATA, WAIT_ACK, WAIT_DATA_AND_ACK };
	State state= State.WAIT_DATA;
	/** the input terimnal */
	protected InputTerminal inputTerminal= null;
	/** the output terminal */
	protected OutputTerminal outputTerminal= null;
	// inform the data destination (input terminal) that data is available
	protected Command inputAvailableCmd= new InputAvailableCommand();
	// inform the data source (output terminal) that the data has been received
	protected Command outputAckCmd= new OutputAckCommand();	
	
	public PointToPointChannel(String n) {
		super(n);
	}

	public PointToPointChannel(String name, CompositeEntity g) {
		super(name, g);
	}

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
	public boolean isAttached(Terminal t) {
		return (t == inputTerminal) || (t == outputTerminal);
	} // isAttached
	
	@Override
	protected void distributeInputs() {
		inputAvailableCmd.trigger(inputTerminal, calculateDelay(outputAvailableDelay, outputAvailableDelayVariation));
	} // distributeInputs

	@Override
	public void outputAvailable(OutputTerminal t, Object data) throws EventSimErrorException {
		if ( t == outputTerminal) {
			if (state == State.WAIT_DATA) {
				// buffer the data
				myData= data;
				// wait for next data and the ack for the data
				// that has just been sent out
				state= State.WAIT_DATA_AND_ACK;			// log what happened
				logOutputReceived(t, data);		
				// send data to attached input terminals
				distributeInputs();
				// acknowledge that data has been received
				ackOutput();
	
			}
			else if (state == State.WAIT_DATA_AND_ACK) {
				// log data arrival
				logOutputReceived(t, data);
				// data is here, wait for the ack
				state= State.WAIT_ACK;
			}
			else {
				// state = wait ack, data should not have arrived
				fatalError( "Communication interference: "
						+ "Terminal " + t.getName() + ", " + t.getID()
						+ ", produced an output without receiving an ack");			
			}
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
	public void ackInput(InputTerminal t) throws EventSimErrorException {
		if (state == State.WAIT_ACK) {
			// ack is expected
			// log that an ack was received
			logInputAcknowledged(t);
			// wait for next data and the ack for the data
			// that has just been sent out
			state= State.WAIT_DATA_AND_ACK;	
			// the next data item is here
			// send it out, and acknowledge receipts of the output
			// get the data
			myData= outputTerminal.getData();
			// pass the data to input terminals
			distributeInputs();
			// acknowledge that data has been received
			ackOutput();
		}
		else if (state == State.WAIT_DATA_AND_ACK) {
			// log that an ack was received
			logInputAcknowledged(t);
			state= State.WAIT_DATA;
		}
		else {
			// state = State.WAIT_DATA
			// error
			fatalError("Terminal " + t.getAlias() + ", " + t.getID() + ", "
					+ "produced an ack when not required.");	
		}
	} // ackInput


	/** acknowledge that the output has been received */
	@Override
	public void ackOutput() {
		outputAckCmd.trigger(null, calculateDelay(outputAckDelay, outputAckDelayVariation));
	} // ackOutput

	@Override
	public void init() throws EventSimErrorException {
		state= State.WAIT_DATA;
		if (channelInitialized) triggerInitialization();
	} // init

	
	/**
	 * Self check.
	 * Verify consistency of attachments.
	 * Verify that all commands have been initialized.
	 */
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

} // class PointToPointChannel
