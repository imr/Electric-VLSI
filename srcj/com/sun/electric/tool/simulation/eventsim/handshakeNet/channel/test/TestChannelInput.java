package com.sun.electric.tool.simulation.eventsim.handshakeNet.channel.test;

import java.util.Random;

import com.sun.electric.tool.simulation.eventsim.core.engine.Command;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.CompositeEntity;
import com.sun.electric.tool.simulation.eventsim.core.simulation.Delay;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.OutputTerminal;


public class TestChannelInput extends OutputTerminal {
	
	// what data to send out
	int[] dataToSend;
	// where in the list of data we are
	int index= 0;
	// delay before produucting the first item on the list
	Delay startDelay= new Delay(0);
	// delay between acknowledgment and production of the next item on the list
	Delay cycleDelay= new Delay(0);
	// what variation existst in the delay
	// default is 0 = no variation
	// in general, delay to acknowledge an input is an interval
	// D= [ ackDelay - variation, ackDelay + variation ]
	int delayVariation= 0;
	int delayVariationRange=0;
	// how many outputs are produced. If num > dataToSend.size() then 
	// the component cycles through the array
	int numOutputs;
	// count how many outputs have been produced
	int count= 0;
	// triggered to produce an output - which is an input to the channel
	Command produceOutputCmd= new ProduceOutputCommand();
	
	// get a new random generator
	Random rndGen= new Random();

	public TestChannelInput(String n, int[] d, int num) {
		super(n);
		dataToSend= d;
		numOutputs= num;
	}

	public TestChannelInput(String name, int[] d, int num, CompositeEntity g) {
		super(name, g);
		dataToSend= d;
		numOutputs= num;
	}

	public TestChannelInput(String n) {
		super(n);
	}

	public TestChannelInput(String name, CompositeEntity g) {
		super(name, g);
	}

	
	// set the data to be sent out
	public void setData(int[] d) {
		dataToSend= d;
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
	
	void scheduleOutput(Delay delay) {
		// get the data
		myData= dataToSend[index++];
		// cycle around if needed
		if (index >= dataToSend.length) index= 0;
		// update the count
		count++;
		// schedule the output
		produceOutputCmd.trigger(myData, delay);
	}
	
	/**
	 * Called by the channel to acknowledge output.
	 */
	@Override
	public void outputAck() {
		if (count < numOutputs) {
			// not done, so schedule the next output
			scheduleOutput(calculateDelay(cycleDelay));
		}
	} // outputAck

	@Override
	public void outputAvailable(Object data) throws EventSimErrorException {
		fatalError("outputAvailable not implemented");
	}

	@Override
	public void outputAvailable(Object data, Delay d) throws EventSimErrorException {
		fatalError("outputAvailable not implemented");
	}

	@Override
	public void outputUnavailable() throws EventSimErrorException {
		fatalError("outputUnavailable not implemented");
	}

	@Override
	public void init() {
		// go to the beginning of the data
		index= 0;
		// start counting from 0
		count= 0;
		// schedule the first output
		if (numOutputs > 0) scheduleOutput(calculateDelay(startDelay));
	} // init
	
	class ProduceOutputCommand extends Command {
		public void execute(Object param) throws EventSimErrorException {
			myChannel.outputAvailable(TestChannelInput.this, param);
		} // execute
	} // class produceOutputCommand

}
