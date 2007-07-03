package com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.test;

import java.util.LinkedList;
import java.util.Random;

import com.sun.electric.tool.simulation.eventsim.core.engine.Command;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.CompositeEntity;
import com.sun.electric.tool.simulation.eventsim.core.simulation.Delay;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.channel.Channel;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.InputTerminal;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.OutputTerminal;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.Terminal;

/** a destination for data coming from an output terminal - used in JUNIT testing */
public class OutputTerminalTestDestination extends Channel {

	static enum State {WAIT_DATA, WAIT_ACK};
	State state= State.WAIT_DATA;
	
	// the output terminal under test
	OutputTerminal outTerm;
	// inform the data source (output terminal) that the data has been received
	Command ackOutputCmd= new AckOutputCommand();
	// storage for data received
	LinkedList<Integer> dataReceived= new LinkedList<Integer>();
	// what variation existst in the delay
	// default is 0 = no variation
	// in general, delay to acknowledge an input is an interval
	// D= [ ackDelay - variation, ackDelay + variation ]
	int delayVariation= 0;
	int delayVariationRange=0;
	// delay between receiving data and producting acknowledgment
	Delay cycleDelay= new Delay(0);
	// get a new random generator
	Random rndGen= new Random();
	
	public OutputTerminalTestDestination(String n) {
		super(n);
	}

	public OutputTerminalTestDestination(String name, CompositeEntity g) {
		super(name, g);
	}
	
	@Override
	public void init() {
		state= State.WAIT_DATA;
		dataReceived.clear();
	} // init


	// the same as attach
	public void setTerminal(OutputTerminal t) {
		attach(t);
	} // setTerminal
	
	public void setCycleDelay(Delay d) {
		cycleDelay= d;
	}
	
	// set the variation. the range is two times the variation 
	public void setDelayVariation(int var) {
		delayVariation= var;
		delayVariationRange= 2 * delayVariation;
	}
	
	Delay calculateDelay(Delay defDelay) {
		Delay d= defDelay;
		if (delayVariation > 0) {
			// get the randomized delay variation
			int delayVar= rndGen.nextInt(delayVariationRange) - delayVariation;
			if (delayVar < 0) delayVar= 0;
			// add the variation to the delay
			d= defDelay.addDelay(delayVar);
		}
		return d;
	}
	
	
	@Override
	public void ackInput(InputTerminal t) throws EventSimErrorException {
		fatalError("ackInput not implemented.");
	}

	/** acknowledge that the output has been received */ 
	@Override
	protected void ackOutput() {
		// TODO ????
	} // ackOutput

	@Override
	public boolean attach(InputTerminal t) throws EventSimErrorException {
		fatalError("Attach input terminal not implemented.");
		return false;
	}

	@Override
	public boolean attach(OutputTerminal t) {
		outTerm= t;
		t.setChannel(this);
		return true;
	} // attach

	@Override
	protected void distributeInputs() throws EventSimErrorException {
		fatalError("distributeInputs not implemented.");
	}

	@Override
	public boolean isAttached(Terminal t) {
		return (t == outTerm);
	}

	@Override
	public void outputAvailable(OutputTerminal t, Object data) throws EventSimErrorException {
		if (t == outTerm) {
			if (state == State.WAIT_DATA) {
			    // assume only integers are sent for test purposes
				// record data received
				dataReceived.addLast((Integer)data);
				// wait to produce acknowledgment
				state= State.WAIT_ACK;
				// schedule acknowledgment
				ackOutputCmd.trigger(null, calculateDelay(cycleDelay));
			}
			else {
				fatalError("Output arrived before the previous output was acknowledged.");
			}
		}
		else {
			fatalError("Input received on a non-attached terminal " + t);
		}
	} // outputAvailable

	@Override
	public void outputUnavailable(OutputTerminal t) throws EventSimErrorException {
		fatalError("outputUnavailable not implemented.");
	}


	@Override
	public boolean selfCheck() {
		boolean check= true;
		return check;
	}

	// to acknowledge outputs received
	class AckOutputCommand extends Command {
		public void execute(Object param) throws EventSimErrorException {
			if (state == State.WAIT_ACK) {
				// about to produce an output ack, start waiting for the next data item
				state= State.WAIT_DATA;
				outTerm.outputAck();
			}
			else {
				fatalError("AckOutput command executed in state " + state);
			}
		} // execute
	} // class AckOutputCommand
	
} // class OutputTerminalTestDestination
