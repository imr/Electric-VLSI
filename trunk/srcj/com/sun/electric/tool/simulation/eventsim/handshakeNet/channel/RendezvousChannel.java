package com.sun.electric.tool.simulation.eventsim.handshakeNet.channel;

import java.util.HashSet;
import java.util.Set;

import com.sun.electric.tool.simulation.eventsim.core.engine.Command;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.CompositeEntity;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.InputTerminal;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.OutputTerminal;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.Terminal;

/**
 * Only one output terminal at a time may provide data.
 * All input terminals get the data.
 * 
 * @author ib27688
 *
 */
public class RendezvousChannel extends Channel {

	static enum State { WAIT_DATA, WAIT_ACK, WAIT_DATA_AND_ACK };
	State state= State.WAIT_DATA;
	
	/** input terminals attached to the channel */
	protected Set<InputTerminal> inputTerminals= new HashSet<InputTerminal>();
	/** output terminals attached to the channel */
	protected Set<OutputTerminal> outputTerminals= new HashSet<OutputTerminal>();
	// triggered by an output terminal that provides an input
	// to the channel
	protected Command inputAvailableCmd= new InputAvailableCommand();
	// triggered by the channel to acknowledge output has been
	// received - sends an ack back to an output terminal
	protected Command outputAckCmd= new OutputAckCommand();
	// terminal that produce the last "legal" output
	// null means that the output was the initial one
	protected OutputTerminal lastSource= null;
	// the source of the next data item, relevant when the
	// next data arrives before the ack
	protected OutputTerminal nextSource= null;
	// inputs that have not acknowledged the output yet
	protected HashSet<InputTerminal> outstandingAck= new HashSet<InputTerminal>();

	public RendezvousChannel(String n) {
		super(n);
	}

	public RendezvousChannel(String name, CompositeEntity g) {
		super(name, g);
	}

	/**
	 * Attach an InputTerminal to the channel
	 * @param t - the InputTerminal to be attached
	 * @return true if the terminal had not already been attached
	 */
	public boolean attach(InputTerminal t) {
		boolean returnValue= false;
		if (t != null) {
			t.setChannel(this);
			returnValue= inputTerminals.add(t);
		}
		return returnValue;
	} // attach
	
	/**
	 * Attach an OutputTerminal to the channel
	 * @param t - the OutputTerminal to be attached
	 * @return true if the terminal had not already been attached
	 */
	public boolean attach(OutputTerminal t) {
		boolean returnValue= false;
		if (t != null) {
			t.setChannel(this);
			returnValue= outputTerminals.add(t);
		}
		return returnValue;
	} // attach

	/** Is the terminal attached? */
	public boolean isAttached(Terminal t) {
		return (inputTerminals.contains(t) || outputTerminals.contains(t));
	} // isAttached


	@Override
	protected void distributeInputs() {
		for (InputTerminal in : inputTerminals) {
			inputAvailableCmd.trigger(in, calculateDelay(outputAvailableDelay, outputAvailableDelayVariation));
		} // for
	} //distributeInputs

	@Override
	public void outputUnavailable(OutputTerminal t) throws EventSimErrorException {
		fatalError("Output unavailable not implemented, requested by "
				+ "terminal " + t.getAlias() + ", " + t.getID());
	} // outputUnavailable

	@Override
	public void ackInput(InputTerminal t) throws EventSimErrorException {		
		if (state == State.WAIT_ACK) {
			// first check whether the channel is waiting for an acknowledgment
			// from this InputTerminal
			if (outstandingAck.remove(t)) {
				// ack was expected
				// log that an ack was received
				logInputAcknowledged(t);
				// if all acks have been received, produce an ack for
				// the output terminal that was the origin of the data
				if (outstandingAck.isEmpty()) {
					// the next data item is here
					// send it out, and acknowledge receipts of the output
					// remember where the data came from
					lastSource= nextSource;
					// get the data
					myData= lastSource.getData();
					// pass the data to input terminals
					distributeInputs();
					// acknowledge that data has been received
					ackOutput();
					// wait for next data and the ack for the data
					// that has just been sent out
					state= State.WAIT_DATA_AND_ACK;
				}
			}
			else {
				// if the ack was unwarranted, report an error
				fatalError("Terminal " + t.getAlias() + ", " + t.getID() + ", "
						+ "produced an ack when not required.");			
			} // else
		}
		else if (state == State.WAIT_DATA_AND_ACK) {
			if (outstandingAck.remove(t)) {
				// ack was expected
				// log that an ack was received
				logInputAcknowledged(t);
				// if all acks have been received, produce an ack for
				// the output terminal that was the origin of the data
				if (outstandingAck.isEmpty()) {
					// the data is not here yet, start waiting for data
					state= State.WAIT_DATA;
				}
			}
			else {
				// if the ack was unwarranted, report an error
				fatalError("Terminal " + t.getAlias() + ", " + t.getID() + ", "
						+ "produced an ack when not required.");				
			}
		}
		else { // state = State.WAIT_DATA
			fatalError("Communication interference: "
					+ "Two acks received without data "
					+ "being offered to an input terminal.");
		}
	} // ackInput

	/**
	 * Inform the channel that the data is available
	 * @param t the terminal that has data available
	 * @param data tha data that is being made available
	 * @throws EventSimErrorException 
	 */
	@Override
	public void outputAvailable(OutputTerminal t, Object data) throws EventSimErrorException {
		if (state == State.WAIT_DATA) {
			// buffer the data
			myData= data;
			// remember where the data came from
			lastSource= t;
			// log what happened
			logOutputReceived(t, data);
			// send data to attached input terminals
			distributeInputs();
			// acknowledge that data has been received
			outputAckCmd.trigger(null, calculateDelay(outputAckDelay, outputAckDelayVariation));
			// wait for next data and the ack for the data
			// that has just been sent out
			state= State.WAIT_DATA_AND_ACK;
		}
		else if (state == State.WAIT_DATA_AND_ACK) {
			// remember where the data will come from
			nextSource= t;
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
	} // outputAvailable

	
	/** acknowledge that the output has been received */ 
	@Override
	public void ackOutput() {
		outputAckCmd.trigger(null, calculateDelay(outputAckDelay, outputAckDelayVariation));
	} // ackOutput

	/**
	 * Reset operations.
	 * @throws EventSimErrorException 
	 */
	@Override
	public void init() throws EventSimErrorException {
		lastSource= null;
		nextSource= null;
		state= State.WAIT_DATA;
		if (channelInitialized) triggerInitialization();
	}

	/**
	 * Self check.
	 * Verify consistency of attachments.
	 * Verify that all commands have been initialized.
	 */
	@Override
	public boolean selfCheck() {
		boolean check= true;
		
		if (inputTerminals == null) {
			check= false;
			logError("Self check failed: inputTerminals == null");				

		}
		else {
			for (InputTerminal inT : inputTerminals) {
				if (!inT.isAttached(this)) {
					check= false;
					logError("Self check failed: input terminal " + inT + " is not attached");				
				}
			}
		}

		if (outputTerminals == null) {
			check= false;
			logError("Self check failed: outputTerminals == null");				

		}
		for (OutputTerminal outTT : outputTerminals) {
			if (!outTT.isAttached(this)) {
				check= false;
				logError("Self check failed: output terminal " + outTT + " is not attached");								
			}
		}
		
		if (outputAckCmd == null) {
			check= false;
			logError("Self check failed: outputAckCmd == null");				

		}

		if (inputAvailableCmd == null) {
			check= false;
			logError("Self check failed: inputAvailableCmd == null");				

		}

		return check;
	} // selfCheck
	
	/**
	 * Inform input terminals that they have an input waiting for them.
	 * Note that input terminals are informed one at a time, but all with the same delay
	 * Possible extension: separate delay for each input
	 * @author ib27688 
	 */
	protected class InputAvailableCommand extends Command {
		
		public void execute(Object data) throws EventSimErrorException {
			if (data instanceof InputTerminal) {
				InputTerminal in= (InputTerminal)data;
				// record that the channel requires an ack from this terminal
				outstandingAck.add(in);
				// log that the input was passed on
				logInputNotified(in, myData);
				// let the terminal know that an input is available
				in.inputAvailable(myData);
			}
			else {
				fatalError("intputAvailable notification requested for "
						+ " an object that is not an InputTerminal. The class is "
						+ data.getClass());
			} // else
		} // execute
	} // InputAvailableCommand
	
	/**
	 * Inform the OutputTerminal that was the last data source 
	 * that the data has been received and the it can produce new data.
	 * @author ib27688
	 */
	protected class OutputAckCommand extends Command {
		public void execute(Object data) {
			// this cycle is complete
			// note, lastTerminal is not reset
			// it will be null only for the initial value, indicating that
			// there was no source for it
			// log that the output was acknowledged
			logOutputAcknowledged(lastSource);
			// inform the output the communication cycle is complete
			lastSource.outputAck();
		} // execute
	} // class OutputAckCommand
	
} // class RendezvousChannel
