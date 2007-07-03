package com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.test;

import java.util.Random;

import com.sun.electric.tool.simulation.eventsim.core.engine.Command;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.CompositeEntity;
import com.sun.electric.tool.simulation.eventsim.core.simulation.Delay;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.component.ComponentWorker;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.OutputTerminal;

/** source of data for output terminal JUNIT tests */
public class OutputTerminalTestSource extends ComponentWorker {

	static enum State { WAIT_ACK, WAIT_OUTPUT };
	State state;
	
	// terminal under test
	OutputTerminal outTerm=null;
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
	// triggered to produce an output - which is an input to the terminal
	Command produceOutputCmd= new ProduceOutputCommand();
	// Triggered when an output is acknowledged
	Command outputAckCommand= new OutputAckCommand();
	
	public OutputTerminalTestSource(String n) {
		super(n);

	}

	public OutputTerminalTestSource(String name, CompositeEntity g) {
		super(name, g);
	}

	@Override
	public void init() {
		index=0;
		count=0;
		// initiate the first output
		state= State.WAIT_OUTPUT;
		produceOutputCmd.trigger(null, calculateDelay(startDelay));
	} // init

	public void setTerminal(OutputTerminal t) {
		outTerm= t;
		attachOutput(outTerm, outputAckCommand);
	}
	
	// set the data to be sent out
	public void setData(int[] d) {
		testData= d;
	}
	
	public void setStartDelay(Delay d) {
		startDelay= d;
	}
	
	public void setCycleDelay(Delay d) {
		cycleDelay= d;
	}
	
	// set the variation. the range is two times the variation 
	public void setDelayVariation(int var) {
		delayVariation= var;
		delayVariationRange= 2 * delayVariation;
	}

	public void setNumOutputs(int num) {
		numOutputs= num;
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
	public boolean selfCheck() {
		return true;
	}

	
	class ProduceOutputCommand extends Command {
		public void execute(Object param) throws EventSimErrorException {
			if (state == State.WAIT_OUTPUT) {
				if (count < numOutputs) {
					state= State.WAIT_ACK;
					outTerm.outputAvailable(testData[index]);
					// increase the count of sent items
					count++;
					// update the index
					index++;
					if (index >= testData.length) index=0;
				}
			}
			else {
				fatalError("produce output command engaged in state "+ state);
			}
		} // execute
	} // class ProduceOutputCommand
	
	class OutputAckCommand extends Command {
		public void execute(Object param) throws EventSimErrorException {
			if (state == State.WAIT_ACK) {
				// schedule production of the next output
				// no output will be produced is the count has reached numOut
				state= State.WAIT_OUTPUT;
				produceOutputCmd.trigger(null, calculateDelay(cycleDelay));
			}
			else {
				fatalError("output ack command engaged in state "+ state);				
			}
				
		}
	} // class OutputAckCommand
	
} // OutputTerminalTestSource
