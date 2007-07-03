package com.sun.electric.tool.simulation.eventsim.handshakeNet.channel.test;

import java.util.Random;

import com.sun.electric.tool.simulation.eventsim.core.engine.Command;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.CompositeEntity;
import com.sun.electric.tool.simulation.eventsim.core.simulation.Delay;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.OutputTerminal;

/** 
 * active only for one cycle, then it passes control back to 
 * controller
 * @author ib27688
 *
 */
public class TestChannelControlledInput extends OutputTerminal {
	
	// is this active
	boolean active= false;
	// commad to be executed when control is relinquished
	Command deactivateCmd= null;
	
	// what data to send out
	int[] dataToSend;
	// where in the list of data we are
	int index= 0;
	// delay between acknowledgment and production of the next item on the list
	Delay cycleDelay= new Delay(0);
	// what variation existst in the delay
	// default is 0 = no variation
	// in general, delay to acknowledge an input is an interval
	// D= [ ackDelay - variation, ackDelay + variation ]
	int delayVariation= 0;
	int delayVariationRange=0;
	// how many outputs are produced. If num > dataToSend.size() then 
	// count how many outputs have been produced
	int count= 0;
	
	Command produceOutputCmd= new ProduceOutputCommand();
	
	// get a new random generator
	Random rndGen= new Random();

	public TestChannelControlledInput(String n, int[] d) {
		super(n);
		dataToSend= d;
	}

	public TestChannelControlledInput(String name, int[] d, CompositeEntity g) {
		super(name, g);
		dataToSend= d;
	}

	public TestChannelControlledInput(String n) {
		super(n);
	}

	public TestChannelControlledInput(String name, CompositeEntity g) {
		super(name, g);
	}

	
	public void activate(Command c) {
		// remembre the deactivation command
		deactivateCmd= c;
		// schedule the an output
		scheduleOutput(calculateDelay(cycleDelay));		
		
	} // activate
	
	// set the data to be sent out
	public void setData(int[] d) {
		dataToSend= d;
	}
	
	
	public void setCycleDelay(Delay d) {
		cycleDelay= d;
	}
	
	// set the variation. the range is two times the variation 
	public void setDelayVariation(int var) {
		delayVariation= var;
		delayVariationRange= 2 * delayVariation;
	}

	public int getCount() {
		return count;
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
		// relinquish communication with the channel
		deactivateCmd.trigger(null);
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
		// start out passive
		active= false;
	} // init
	
	class ProduceOutputCommand extends Command {
		public void execute(Object param) throws EventSimErrorException {
			myChannel.outputAvailable(TestChannelControlledInput.this, param);
		} // execute
	} // class produceOutputCommand

}
