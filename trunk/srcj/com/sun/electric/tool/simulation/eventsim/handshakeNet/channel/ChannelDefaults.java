package com.sun.electric.tool.simulation.eventsim.handshakeNet.channel;

import com.sun.electric.tool.simulation.eventsim.core.simulation.Delay;

public class ChannelDefaults {

	// never make an instance
	private ChannelDefaults() {	}
	
	public static final int CHANNEL_DELAY_DEF= 0;
	
	public static final String JOURNAL_CHANNEL= "logChannel";
	public static final boolean JOURNAL_CHANNEL_DEF= false;
	
	public static final String OUTPUT_AVAILABLE_DELAY= "channelOutputAvailableDelay";
	public static final Delay OUTPUT_AVAILABLE_DELAY_DEF= new Delay(CHANNEL_DELAY_DEF);
	public static final String OUTPUT_AVAILABLE_DELAY_VARIATION= "channelOutputAvailableDelayVariation";
	public static final int OUTPUT_AVAILABLE_DELAY_VARIATION_DEF= 0;

	public static final String OUTPUT_UNAVAILABLE_DELAY= "channelOutputUnvailableDelay";
	public static final Delay OUTPUT_UNAVAILABLE_DELAY_DEF= new Delay(CHANNEL_DELAY_DEF);
	public static final String OUTPUT_UNAVAILABLE_DELAY_VARIATION= "channelOutputUnavailableDelayVariation";
	public static final int OUTPUT_UNAVAILABLE_DELAY_VARIATION_DEF= 0;

	public static final String OUTPUT_ACK_DELAY= "channelOutputAckDelay";
	public static final Delay OUTPUT_ACK_DELAY_DEF= new Delay(CHANNEL_DELAY_DEF);
	public static final String OUTPUT_ACK_DELAY_VARIATION= "channelOutputAckDelayVariation";
	public static final int OUTPUT_ACK_DELAY_VARIATION_DEF= 0;

	
} // class ChannelDefaults
