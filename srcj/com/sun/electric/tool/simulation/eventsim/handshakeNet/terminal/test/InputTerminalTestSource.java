package com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.test;

import java.util.Random;

import com.sun.electric.tool.simulation.eventsim.core.engine.Command;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.CompositeEntity;
import com.sun.electric.tool.simulation.eventsim.core.simulation.Delay;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.channel.Channel;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.InputTerminal;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.OutputTerminal;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.Terminal;

public class InputTerminalTestSource extends Channel {

	static enum State { WAIT_ACK, WAIT_OUTPUT };
	State state;
	
	// terminal under test
	InputTerminal inTerm=null;
	// data used for testing
	int[] testData;
	// where in the test data we are
	int index= 0;
	// number of outputs produced
	int numOutputs=0;
	int count=0;
	// what variation existst in the delay
	// default is 0 = no variation
	// in general, delay to acknowledge an input is an interval
	// D= [ ackDelay - variation, ackDelay + variation ]
	int delayVariation= 0;
	int delayVariationRange=0;
	// delay before produucting the first item on the list
	Delay startDelay= new Delay(0);
	// delay between acknowledgment and production of the next item on the list
	Delay cycleDelay= new Delay(0);
	// get a new random generator
	Random rndGen= new Random();	
	// send the data to the input terminal
	Command inputAvailableCmd= new InputAvailableCommand();
	
	public InputTerminalTestSource(String n) {
		super(n);
	}

	public InputTerminalTestSource(String name, CompositeEntity g) {
		super(name, g);
	}

	// the same as attach
	public void setTerminal(InputTerminal t) {
		attach(t);
	} // setTerminal
	
	public void setCycleDelay(Delay d) {
		cycleDelay= d;
	}
	
	public void setStartDelay(Delay d) {
		startDelay= d;
	}
	
	// set the variation. the range is two times the variation 
	public void setDelayVariation(int var) {
		delayVariation= var;
		delayVariationRange= 2 * delayVariation;
	}
	
	public void setNumOutputs(int num) {
		numOutputs= num;
	}
	
	// set the data to be sent out
	public void setData(int[] d) {
		testData= d;
	}	
	
	// input terminal acknowledges data
	@Override
	public void ackInput(InputTerminal t) throws EventSimErrorException {
		if (state == State.WAIT_ACK) {
			// schedule production of the next output
			state= State.WAIT_OUTPUT;
			if (count < numOutputs) {
				inputAvailableCmd.trigger(null, calculateDelay(cycleDelay));
			}
		}
		else {
			fatalError("ack input called in state "+ state);				
		}
	} // ackInput
	
	@Override
	protected void ackOutput() throws EventSimErrorException {
		fatalError("ackOutput not implemented.");
	}

	@Override
	public boolean attach(InputTerminal t) {
		inTerm= t;
		t.setChannel(this);
		return true;
	}

	@Override
	public boolean attach(OutputTerminal t) throws EventSimErrorException {
		fatalError("attach OutputTeminal not implemented.");
		return false;
	}

	@Override
	protected void distributeInputs() throws EventSimErrorException {
		fatalError("distributeInputs not implemented.");
	}

	@Override
	public boolean isAttached(Terminal t) {
		return t == inTerm;
	}

	@Override
	public void outputAvailable(OutputTerminal t, Object data) throws EventSimErrorException {
		fatalError("outputAvailable not implemented.");
	}

	@Override
	public void outputUnavailable(OutputTerminal t) throws EventSimErrorException {
		fatalError("outputUnavailable not implemented.");

	}

	@Override
	public void init() {
		state= State.WAIT_OUTPUT;
		count= 0;
		index= 0;
		// initiate the first output
		inputAvailableCmd.trigger(null, calculateDelay(startDelay));
	}

	@Override
	public boolean selfCheck() {
		boolean check= true;
		return check;
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
	
	class InputAvailableCommand extends Command {
		public void execute(Object param) throws EventSimErrorException {
			if (state == State.WAIT_OUTPUT) {
				if (count < numOutputs) {
					state= State.WAIT_ACK;
					myData= testData[index];
					inTerm.inputAvailable(myData);
					// increase the count of sent items
					count++;
					// update the index
					index++;
					if (index >= testData.length) index=0;
				}
			}
			else {
				fatalError("input available command engaged in state "+ state);
			}
		} // execute
	} // class InputAvailableCommand
	
} // class InputTerminalTestSource
