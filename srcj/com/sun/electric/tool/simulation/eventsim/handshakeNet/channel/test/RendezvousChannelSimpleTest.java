package com.sun.electric.tool.simulation.eventsim.handshakeNet.channel.test;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

import com.sun.electric.tool.simulation.eventsim.core.engine.Director;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.simulation.Delay;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.channel.Channel;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.channel.RendezvousChannel;

public class RendezvousChannelSimpleTest {
	
	Director director= Director.getInstance();
	
	TestChannelInput source;
	TestChannelOutput destination;

	Channel channel;
	
	int[] testData= new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
	
	final Delay SLOW_SRC_DELAY= new Delay(1000);
	final Delay FAST_SRC_DELAY= new Delay(0);
	final Delay SLOW_DEST_DELAY= new Delay(1000);
	final Delay FAST_DEST_DELAY= new Delay(0);
	final Delay START_DELAY= new Delay(500);	
	
	final int DELAY_VAR= 500;
	
	// num outputs to produce
	final int NUM_OUTPUTS= 100;

	@Before
	public void setUp() throws Exception {
		// make the source and the destination
		source= new TestChannelInput("RendezvousChannelTestSource0");
		destination= new TestChannelOutput("RendezvousChannelTestDestination0");
		// make the channel and attach the source and the destination
		channel= new RendezvousChannel("RendezvousTestChannel");
		channel.attach(source);
		channel.attach(destination);
		
		source.setData(testData);
		source.setNumOutputs(NUM_OUTPUTS);
	}

	@After
	public void tearDown() throws Exception {
		director.masterClear();
	}
	
	@Test public void slowSrcSlowDest() throws EventSimErrorException {
		// set delays
		source.setStartDelay(START_DELAY);
		source.setCycleDelay(SLOW_SRC_DELAY);
		destination.setAckDelay(SLOW_DEST_DELAY);
		
		director.reset();
		director.run();
		
		// compare outputs produced and outputs received
		int index= 0;
		int count= 0;
		for (Object o : destination.receivedData()) {
			int ref= ((Integer)o).intValue();
			assertEquals(testData[index], ref);
			index++;
			if (index >= testData.length) index= 0;
			count++;
		}
		
		assertEquals(NUM_OUTPUTS, count);
		
	} // slowSrcSlowDest
	
	@Test public void fastSrcSlowDest() throws EventSimErrorException {
		// set delays
		source.setStartDelay(START_DELAY);
		source.setCycleDelay(FAST_SRC_DELAY);
		destination.setAckDelay(SLOW_DEST_DELAY);
		
		director.reset();
		director.run();
		
		// compare outputs produced and outputs received
		int index= 0;
		int count= 0;
		for (Object o : destination.receivedData()) {
			int ref= ((Integer)o).intValue();
			assertEquals(testData[index], ref);
			index++;
			if (index >= testData.length) index= 0;
			count++;
		}
		
		assertEquals(NUM_OUTPUTS, count);
		
	} // fastSrcSlowDest	

	@Test public void slowSrcFastDest() throws EventSimErrorException {
		// set delays
		source.setStartDelay(START_DELAY);
		source.setCycleDelay(SLOW_SRC_DELAY);
		destination.setAckDelay(FAST_DEST_DELAY);
		
		director.reset();
		director.run();
		
		// compare outputs produced and outputs received
		int index= 0;
		int count= 0;
		for (Object o : destination.receivedData()) {
			int ref= ((Integer)o).intValue();
			assertEquals(testData[index], ref);
			index++;
			if (index >= testData.length) index= 0;
			count++;
		}
		
		assertEquals(NUM_OUTPUTS, count);
		
	} // fastSrcfastDest
	
	@Test public void fastSrcFastDest() throws EventSimErrorException {
		// set delays
		source.setStartDelay(START_DELAY);
		source.setCycleDelay(FAST_SRC_DELAY);
		destination.setAckDelay(FAST_DEST_DELAY);
		
		director.reset();
		director.run();
		
		// compare outputs produced and outputs received
		int index= 0;
		int count= 0;
		for (Object o : destination.receivedData()) {
			int ref= ((Integer)o).intValue();
			assertEquals(testData[index], ref);
			index++;
			if (index >= testData.length) index= 0;
			count++;
		}
		
		assertEquals(NUM_OUTPUTS, count);
		
	} // fastSrcfastDest	
	
	@Test public void randomSrcRandomDest() throws EventSimErrorException {
		// set delays
		source.setStartDelay(START_DELAY);
		source.setCycleDelay(SLOW_SRC_DELAY);
		source.setDelayVariation(DELAY_VAR);
		destination.setAckDelay(SLOW_DEST_DELAY);
		destination.setDelayVariation(DELAY_VAR);
		
		director.reset();
		director.run();
		
		// compare outputs produced and outputs received
		int index= 0;
		int count= 0;
		for (Object o : destination.receivedData()) {
			int ref= ((Integer)o).intValue();
			assertEquals(testData[index], ref);
			index++;
			if (index >= testData.length) index= 0;
			count++;
		}
		
		assertEquals(NUM_OUTPUTS, count);
		
	} // randomSrcRandomDest	
	
} // class RendezvoustChannelTest
