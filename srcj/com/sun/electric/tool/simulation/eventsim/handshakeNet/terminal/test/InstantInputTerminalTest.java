package com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.test;

import static org.junit.Assert.assertEquals;

import java.util.LinkedList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.electric.tool.simulation.eventsim.core.engine.Director;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.simulation.Delay;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.InputTerminal;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.InstantInputTerminal;

public class InstantInputTerminalTest {

	Director director= Director.getInstance();
	
	// terminal under test
	InputTerminal inTerm;
	// test source and destination
	InputTerminalTestSource source;
	InputTerminalTestDestination destination;
	
	int[] testData= new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
	
	final Delay SLOW_SRC_DELAY= new Delay(1000);
	final Delay FAST_SRC_DELAY= new Delay(0);
	final Delay SLOW_DEST_DELAY= new Delay(1000);
	final Delay FAST_DEST_DELAY= new Delay(0);
	final Delay START_DELAY= new Delay(500);	
	
	final int DELAY_VAR= 500;
	
	// num outputs to produce
	final int numOutputs= 100;
	
	LinkedList<Integer> referenceData= new LinkedList<Integer>();
	
	@Before
	public void setUp() throws Exception {
		// make the pieces
		source= new InputTerminalTestSource("InputTerminalTestSource");
		destination= new InputTerminalTestDestination("InputTerminalTestDestination");
		inTerm= new InstantInputTerminal("InstantInputTerminalUnderTest");

		source.setData(testData);
		source.setNumOutputs(numOutputs);
		source.setTerminal(inTerm);
		
		destination.setTerminal(inTerm);
		
		//make the reference list
		for (int i=0; i<testData.length; i++) {
			referenceData.addLast(testData[i]);
		}
		
	} // setUp
	
	@After
	public void tearDown() throws Exception {
		director.masterClear();
	}
	
	@Test
	public void slowSrcSlowDest() throws EventSimErrorException {
		// set delays
		source.setStartDelay(START_DELAY);
		source.setCycleDelay(SLOW_SRC_DELAY);
		destination.setCycleDelay(SLOW_DEST_DELAY);
		
		director.reset();
		director.run();
		
		assertEquals(numOutputs, destination.dataReceived.size());
		
		int refIndex= 0;
		for (Integer rec : destination.dataReceived) {
			assertEquals(testData[refIndex], rec.intValue());
			refIndex= (refIndex + 1) % testData.length;
		}
		
	} // slowSrcSlowDest
	
	@Test
	public void slowSrcFastDest() throws EventSimErrorException {
		// set delays
		source.setStartDelay(START_DELAY);
		source.setCycleDelay(SLOW_SRC_DELAY);
		destination.setCycleDelay(FAST_DEST_DELAY);
		
		director.reset();
		director.run();
		
		assertEquals(numOutputs, destination.dataReceived.size());
		
		int refIndex= 0;
		for (Integer rec : destination.dataReceived) {
			assertEquals(testData[refIndex], rec.intValue());
			refIndex= (refIndex + 1) % testData.length;
		}
		
	} // slowSrcFastDest

	@Test
	public void fastSrcSlowDest() throws EventSimErrorException {
		// set delays
		source.setStartDelay(START_DELAY);
		source.setCycleDelay(FAST_SRC_DELAY);
		destination.setCycleDelay(SLOW_DEST_DELAY);
		
		director.reset();
		director.run();
		
		assertEquals(numOutputs, destination.dataReceived.size());
		
		int refIndex= 0;
		for (Integer rec : destination.dataReceived) {
			assertEquals(testData[refIndex], rec.intValue());
			refIndex= (refIndex + 1) % testData.length;
		}
		
	} // fastSrcSlowDest
	
	@Test
	public void fastSrcFastDest() throws EventSimErrorException {
		// set delays
		source.setStartDelay(START_DELAY);
		source.setCycleDelay(FAST_SRC_DELAY);
		destination.setCycleDelay(FAST_DEST_DELAY);
		
		director.reset();
		director.run();
		
		assertEquals(numOutputs, destination.dataReceived.size());
		
		int refIndex= 0;
		for (Integer rec : destination.dataReceived) {
			assertEquals(testData[refIndex], rec.intValue());
			refIndex= (refIndex + 1) % testData.length;
		}
		
	} // fastSrcFastDest
	
	@Test
	public void randomSrcRandomDest() throws EventSimErrorException {
		// set delays
		source.setStartDelay(START_DELAY);
		source.setCycleDelay(SLOW_SRC_DELAY);
		source.setDelayVariation(DELAY_VAR);
		destination.setCycleDelay(SLOW_DEST_DELAY);
		destination.setDelayVariation(DELAY_VAR);
		
		director.reset();
		director.run();
		
		assertEquals(numOutputs, destination.dataReceived.size());
		
		int refIndex= 0;
		for (Integer rec : destination.dataReceived) {
			assertEquals(testData[refIndex], rec.intValue());
			refIndex= (refIndex + 1) % testData.length;
		}
		
	} // randomSrcRandomDest
	
	
} // class InstantInputTerminalTest
