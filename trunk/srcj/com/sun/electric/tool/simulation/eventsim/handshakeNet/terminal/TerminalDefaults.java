package com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal;

import com.sun.electric.tool.simulation.eventsim.core.simulation.Delay;

public class TerminalDefaults {

	// never make an instance of this class
	private TerminalDefaults() {}

	public static final String INPUT_AVAILABLE_DELAY= "terminalInputAvailableDelay";
	public static final Delay INPUT_AVAILABLE_DELAY_DEF= new Delay(0);
	public static final String INPUT_AVAILABLE_DELAY_VARIATION= "terminalInputAvailableDelayVariation";
	public static final int INPUT_AVAILABLE_DELAY_VARIATION_DEF= 0;

	public static final String INPUT_UNAVAILABLE_DELAY= "terminalInputUnavailableDelay";
	public static final Delay INPUT_UNAVAILABLE_DELAY_DEF= new Delay(0);
	public static final String INPUT_UNAVAILABLE_DELAY_VARIATION= "terminalInputUnavailableDelayVariation";
	public static final int INPUT_UNAVAILABLE_DELAY_VARIATION_DEF= 0;
	
	public static final String INPUT_ACK_DELAY= "terminalInputAckDelay";
	public static final Delay INPUT_ACK_DELAY_DEF= new Delay(0);
	public static final String INPUT_ACK_DELAY_VARIATION= "terminalInputAckDelayVariation";
	public static final int INPUT_ACK_DELAY_VARIATION_DEF= 0;
	
	public static final String OUTPUT_AVAILABLE_DELAY= "terminalOutputAvailableDelay";
	public static final Delay OUTPUT_AVAILABLE_DELAY_DEF= new Delay(0);
	public static final String OUTPUT_AVAILABLE_DELAY_VARIATION= "terminalOutputAvailableDelayVariation";
	public static final int OUTPUT_AVAILABLE_DELAY_VARIATION_DEF= 0;
	
	public static final String OUTPUT_UNAVAILABLE_DELAY= "terminalOutputUnavailableDelay";
	public static final Delay OUTPUT_UNAVAILABLE_DELAY_DEF= new Delay(0);
	public static final String OUTPUT_UNAVAILABLE_DELAY_VARIATION= "terminalOutputUnavailableDelayVariation";
	public static final int OUTPUT_UNAVAILABLE_DELAY_VARIATION_DEF= 0;
	
	public static final String OUTPUT_ACK_DELAY= "terminalOutputAckDelay";
	public static final Delay OUTPUT_ACK_DELAY_DEF= new Delay(0);
	public static final String OUTPUT_ACK_DELAY_VARIATION= "terminalOutputAckDelayVariation";
	public static final int OUTPUT_ACK_DELAY_VARIATION_DEF= 0;
	
} // class TerminalDefaults
