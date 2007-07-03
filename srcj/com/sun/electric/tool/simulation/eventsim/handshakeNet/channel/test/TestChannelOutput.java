package com.sun.electric.tool.simulation.eventsim.handshakeNet.channel.test;

import java.util.LinkedList;
import java.util.Random;

import com.sun.electric.tool.simulation.eventsim.core.engine.Command;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.CompositeEntity;
import com.sun.electric.tool.simulation.eventsim.core.simulation.Delay;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.InputTerminal;

/** replaces an input terminal that serves as a channel output */
public class TestChannelOutput extends InputTerminal {
	// stored data that was received by this component
	LinkedList<Integer> recordedData= new LinkedList<Integer>();
	// used to delay acknowledging the input
	Command inputAckCommand= new InputAckCommand();
	// get a new random generator
	Random rndGen= new Random();
	
	// delay between receiving an input and providing an acknowledgment
	Delay ackDelay= new Delay(100);
	// what variation existst in the delay
	// default is 0 = no variation
	// in general, delay to acknowledge an input is an interval
	// D= [ ackDelay - variation, ackDelay + variation ]
	int delayVariation= 0;
	int delayVariationRange=0;
	
	public TestChannelOutput(String n) {
		super(n);
	}

	public TestChannelOutput(String name, CompositeEntity g) {
		super(name, g);
	}
	
	public TestChannelOutput(String n, Delay ad) {
		super(n);
		ackDelay= ad;
	}

	public TestChannelOutput(String name, Delay ad, CompositeEntity g) {
		super(name, g);
		ackDelay= ad;
	}

	public void setAckDelay(Delay d) {
		ackDelay= d;
	}

	// set the variation. the range is two times the variation 
	public void setDelayVariation(int var) {
		delayVariation= var;
		delayVariationRange= 2 * delayVariation;
	}
	
	@Override
	public void ackInput() throws EventSimErrorException {
		fatalError("ackInput not implemented");
	}

	@Override
	public void ackInput(Delay d) throws EventSimErrorException {
		fatalError("ackInput not implemented");
	}

	@Override
	public void inputAvailable(Object data) {
		// record what data became available
		recordedData.addLast((Integer)data);
		// what delay to use?
		Delay d= ackDelay;
		// do we need to randomize the delay?
		if (delayVariation > 0) {
			// get the randomized delay variation
			int delayVar= rndGen.nextInt(delayVariationRange) - delayVariation;
			if (delayVar < 0) delayVar= 0;
			// add the variation to the delay
			d= ackDelay.addDelay(delayVar);
		}
		inputAckCommand.trigger(this, d);
	} // input available

	@Override
	public void inputUnavailable() throws EventSimErrorException {
		fatalError("Output unavailable not implemented, requested by "
				+ "channel " + myChannel.getAlias() 
				+ ", " + myChannel.getID());
	}
	
	Iterable<Integer> receivedData() {
		return recordedData;
	} // receivedData

	@Override
	public void init() {
		recordedData.clear();
	} // init
	
	class InputAckCommand extends Command {
		public void execute(Object param) throws EventSimErrorException {
			myChannel.ackInput(TestChannelOutput.this);
		}
	} // class InputAckCommand

} // class TestChannelOutput
