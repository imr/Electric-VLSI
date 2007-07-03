package com.sun.electric.tool.simulation.eventsim.handshakeNet.channel.test;

import java.util.LinkedList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertTrue;

import com.sun.electric.tool.simulation.eventsim.core.engine.Command;
import com.sun.electric.tool.simulation.eventsim.core.engine.Director;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.simulation.Delay;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.channel.Channel;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.channel.RendezvousChannel;

public class RendezvousChannelComplexTest {
	
	Director director= Director.getInstance();
	
	TestChannelControlledInput[] sources;
	TestChannelOutput destinations[];

	Channel channel;
	
	int[] testData= new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
	
	LinkedList<Integer> refSequence= new LinkedList<Integer>();
	int refIndex=0;
	
	final int NUM_SOURCES= 3;
	final int NUM_DESTINATIONS= 3;
	
	final Delay SLOW_SRC_DELAY= new Delay(1000);
	final Delay FAST_SRC_DELAY= new Delay(0);
	final Delay SLOW_DEST_DELAY= new Delay(1000);
	final Delay FAST_DEST_DELAY= new Delay(0);
	final Delay START_DELAY= new Delay(500);	
	
	final int DELAY_VAR= 500;
	
	// num outputs to produce
	final int numOutputs= 10;

	// used for round robin selection of sources
	int index= 0;
	int count=0;
	
	Command deactivateCommand= new RoundRobinDeactivateCommand();
	
	@Before
	public void setUp() throws Exception {
		// make sources
		sources= new TestChannelControlledInput[NUM_SOURCES];
		for (int i=0; i< NUM_SOURCES; i++) {
			sources[i]= new TestChannelControlledInput("TestChannelControlledInput"+i);
		}
		// make destinations
		destinations= new TestChannelOutput[NUM_DESTINATIONS];
		for (int i= 0; i< NUM_DESTINATIONS; i++) {
			destinations[i]= new TestChannelOutput("RendezvousChannelTestDestination"+i);
		}
		// make the channel and attach the source and the destination
		channel= new RendezvousChannel("RendezvousTestChannel");
		// attach sources and set their data
		for (int i=0; i<NUM_SOURCES; i++) {
			channel.attach(sources[i]);
			sources[i].setData(testData);
		}
		// attach destinations
		for (int i=0; i< NUM_DESTINATIONS; i++) {
			channel.attach(destinations[i]);
		}
		
	} // setUp

	@After
	public void tearDown() throws Exception {
		director.masterClear();
	}
	
	@Test public void slowSrcSlowDest() throws EventSimErrorException {
		// set delays
		for (int i=0; i<NUM_SOURCES; i++) {
			sources[i].setCycleDelay(SLOW_SRC_DELAY);
		}
		for (int i=0; i<NUM_DESTINATIONS; i++) {
			destinations[i].setAckDelay(SLOW_DEST_DELAY);
		}
		
		director.reset();
		
		index= 0;
		count= 0;
		refIndex= 0;
		refSequence.clear();
		refSequence.addLast(testData[refIndex]);
		sources[0].activate(deactivateCommand);
		
		director.run();
		
		for (int i=0; i< NUM_DESTINATIONS; i++) {
			assertTrue(refSequence.equals(destinations[i].recordedData));
		}
			
	} // slowSrcSlowDest
	
	@Test public void fastSrcSlowDest() throws EventSimErrorException {
		// set delays
		for (int i=0; i<NUM_SOURCES; i++) {
			sources[i].setCycleDelay(FAST_SRC_DELAY);
		}
		for (int i=0; i<NUM_DESTINATIONS; i++) {
			destinations[i].setAckDelay(SLOW_DEST_DELAY);
		}
		
		director.reset();
		
		index= 0;
		count= 0;
		refIndex= 0;
		refSequence.clear();
		refSequence.addLast(testData[refIndex]);
		sources[0].activate(deactivateCommand);
		
		director.run();
		
		for (int i=0; i< NUM_DESTINATIONS; i++) {
			assertTrue(refSequence.equals(destinations[i].recordedData));
		}
	} // fastSrcSlowDest	

	@Test public void slowSrcFastDest() throws EventSimErrorException {
		// set delays
		for (int i=0; i<NUM_SOURCES; i++) {
			sources[i].setCycleDelay(SLOW_SRC_DELAY);
		}
		for (int i=0; i<NUM_DESTINATIONS; i++) {
			destinations[i].setAckDelay(FAST_DEST_DELAY);
		}
		
		director.reset();
		
		index= 0;
		count= 0;
		refIndex= 0;
		refSequence.clear();
		refSequence.addLast(testData[refIndex]);
		sources[0].activate(deactivateCommand);
		
		director.run();
		
		for (int i=0; i< NUM_DESTINATIONS; i++) {
			assertTrue(refSequence.equals(destinations[i].recordedData));
		}
	} // fastSrcfastDest	
	
	@Test public void fastSrcFastDest() throws EventSimErrorException {
		// set delays
		for (int i=0; i<NUM_SOURCES; i++) {
			sources[i].setCycleDelay(FAST_SRC_DELAY);
			sources[i].setDelayVariation(DELAY_VAR);
		}
		for (int i=0; i<NUM_DESTINATIONS; i++) {
			destinations[i].setAckDelay(FAST_DEST_DELAY);
			destinations[i].setDelayVariation(DELAY_VAR);
		}
		
		director.reset();
		
		index= 0;
		count= 0;
		refIndex= 0;
		refSequence.clear();
		refSequence.addLast(testData[refIndex]);
		sources[0].activate(deactivateCommand);
		
		director.run();
		
		for (int i=0; i< NUM_DESTINATIONS; i++) {
			assertTrue(refSequence.equals(destinations[i].recordedData));
		}
	} // fastSrcfastDest	
	
	@Test public void randomSrcRandomDest() throws EventSimErrorException {
		// set delays
		for (int i=0; i<NUM_SOURCES; i++) {
			sources[i].setCycleDelay(SLOW_SRC_DELAY);
		}
		for (int i=0; i<NUM_DESTINATIONS; i++) {
			destinations[i].setAckDelay(SLOW_DEST_DELAY);
		}
		
		director.reset();
		
		index= 0;
		count= 0;
		refIndex= 0;
		refSequence.clear();
		refSequence.addLast(testData[refIndex]);
		sources[0].activate(deactivateCommand);
		
		director.run();

		for (int i=0; i< NUM_DESTINATIONS; i++) {
			assertTrue(refSequence.equals(destinations[i].recordedData));
		}
	} // randomSrcRandomDest	


					
	
	class RoundRobinDeactivateCommand extends Command {
		public void execute(Object param) {
			// update the index
			index++;
			if (index >= sources.length) {
				index= 0;
				refIndex++;
				if (refIndex > testData.length) refIndex= 0;
			}
			count++;
			if (count < numOutputs) {
				// activate the next command
				refSequence.addLast(testData[refIndex]);
				sources[index].activate(deactivateCommand);

			}
		}
	} // class RoundRobinDeactivateCommand
	
} // class RendezvoustChannelComplexTest

