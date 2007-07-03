package com.sun.electric.tool.simulation.eventsim.handshakeNet.channel;

import java.util.Random;

import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.CompositeEntity;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.Entity;
import com.sun.electric.tool.simulation.eventsim.core.simulation.Delay;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.InputTerminal;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.OutputTerminal;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.Terminal;


/** 
 * Communication channel.
 * Forms a connectin between input terminals and output terminals.
 * This class serves as a common denominator for different channel behaviors.
 * @author ib27688
 *
 */

abstract public class Channel extends Entity {

	/** data to be passed to attached input ports */
	protected Object myData= null;
	/** initial value - remember for reset */
	protected Object initialValue= null;
	/** flag that the channel is initialized */
	protected boolean channelInitialized= false;
	
	protected Delay outputAvailableDelay= ChannelDefaults.OUTPUT_AVAILABLE_DELAY_DEF;
	protected int outputAvailableDelayVariation= ChannelDefaults.OUTPUT_AVAILABLE_DELAY_VARIATION_DEF;
	protected Delay outputUnvailableDelay= ChannelDefaults.OUTPUT_UNAVAILABLE_DELAY_DEF;
	protected int outputUnvailableDelayVariation= ChannelDefaults.OUTPUT_UNAVAILABLE_DELAY_VARIATION_DEF;
	protected Delay outputAckDelay= ChannelDefaults.OUTPUT_ACK_DELAY_DEF;
	protected int outputAckDelayVariation= ChannelDefaults.OUTPUT_ACK_DELAY_VARIATION_DEF;
	// needed for delay variation
	protected Random rndGen= new Random();
	
	public Channel(String n) {
		super(n);
		setDelays();
	}

	public Channel(String name, CompositeEntity g) {
		super(name, g);
		setDelays();
	}
	
	/**
	 * Set delays with "global values", otherwise let them remain at their
	 * default values.
	 */
	protected void setDelays() {
		Integer oa= globals.intValue(ChannelDefaults.OUTPUT_AVAILABLE_DELAY);
		if (oa != null) outputAvailableDelay= new Delay(oa);
		Integer oav= globals.intValue(ChannelDefaults.OUTPUT_AVAILABLE_DELAY_VARIATION);
		if (oav != null) outputAvailableDelayVariation= oav;
		
		Integer oua= globals.intValue(ChannelDefaults.OUTPUT_UNAVAILABLE_DELAY);
		if (oua != null) outputUnvailableDelay= new Delay(oua);
		Integer ouav= globals.intValue(ChannelDefaults.OUTPUT_UNAVAILABLE_DELAY_VARIATION);
		if (ouav != null) outputUnvailableDelayVariation= ouav;
		
		Integer oack= globals.intValue(ChannelDefaults.OUTPUT_ACK_DELAY);
		if (oack != null) outputAckDelay= new Delay(oack);
		Integer oackv= globals.intValue(ChannelDefaults.OUTPUT_ACK_DELAY_VARIATION);
		if (oackv != null) outputAckDelayVariation= oackv;
		
		Boolean jc= globals.booleanValue(ChannelDefaults.JOURNAL_CHANNEL);
		if (jc != null) journalActivity= jc;
		
	} // setDelays
	
	/**
	 * Calculate a random delay within the boundaries of delay variation.
	 * @param defDelay base delay
	 * @return randomized delay
	 */
	Delay calculateDelay(Delay defDelay, int delayVariation) {
		Delay d= defDelay;
		if (delayVariation > 0) {
			// get the randomized delay variation
			int delayVar= rndGen.nextInt(2*delayVariation) - delayVariation;
			long newDelay= defDelay.value()-delayVar;
			if (newDelay < 0) newDelay= 0;
			// add the variation to the delay
			d= new Delay(newDelay);
		}
		return d;
	} // calculateDelay
	
	/** 
	 * Record that the channel is to be initialized. 
	 */
	public void initialize(Object data) {
		// assume the object is immutable
		// cloning might be more appropriate but would require
		// extensive changes - all Objects to Cloneable
		initialValue= data;
		// remember that the channel was initialized
		channelInitialized= true;
	} // initialize

	/** 
	 * Prime this channel with data to be produced immediately after reset. 
	 * @throws EventSimErrorException 
	 */
	protected void triggerInitialization() throws EventSimErrorException {
		// declare the output to be available
		outputAvailable(null, initialValue);		
	} // trigger initialization
	
	/** 
	 * distribute outputs to attached input terminals 
	 * @throws EventSimErrorException 
	 */
	abstract protected void distributeInputs() throws EventSimErrorException;
	
	/** 
	 * Called when an output is retracted. 
	 * Inform all inputs that an output was retracted.
	 * @param t the terminal that is retracting the data
	 * @throws EventSimErrorException 
	 */
	abstract public void outputUnavailable(OutputTerminal t) throws EventSimErrorException;
	
	/**
	 * Acknowledge an input - the caller has received the data.
	 * @param t the terminal that is acknowledging the data
	 * @throws EventSimErrorException 
	 */
	abstract public void ackInput(InputTerminal t) throws EventSimErrorException;
	
	/**
	 * Inform the channel that the data is available
	 * @param t the terminal that has data available
	 * @param data tha data that is being made available
	 * @throws EventSimErrorException 
	 */
	abstract public void outputAvailable(OutputTerminal t, Object data) throws EventSimErrorException;
	
	/** acknowledge that the output has been received 
	 * @throws EventSimErrorException */ 
	abstract protected void ackOutput() throws EventSimErrorException;
	
	/** attach an input terminal 
	 * @throws EventSimErrorException */
	abstract public boolean attach(InputTerminal t) throws EventSimErrorException;
	
	/** attach an output terminal 
	 * @throws EventSimErrorException */
	abstract public boolean attach(OutputTerminal t) throws EventSimErrorException;
		
	/**
	 * Is the terminal attached?
	 * @param t terminal
	 * @return true if t is attached to the channel, false otherwise
	 */
	abstract public boolean isAttached(Terminal t);
	
	/**
	 * Get the data that is being "held" by the channel 
	 * @return the data
	 */
	public Object getData() {
		return myData;
	} // getData
	

	/**
	 * Log that the channel received an output
	 * that was produced by OutputTerminal t
	 * @param t source of output
	 * @param data that was received
	 */
	protected final void logOutputReceived(OutputTerminal t, Object data) {
		if (journalActivity) {
			logEvent("Output received on Output Terminal " 
					+ t.getAlias() + ", " + t.getName() 
					+ ": " + data);
		}
	} // logOuputReceived
	
	/**
	 * Log that the channel passed data to the InputTerminal t
	 * @param t destination terminal for the data
	 * @param data data that is to be passed the InputTerminal
	 */
	protected final void logInputNotified(InputTerminal t, Object data) {
		if (journalActivity) {
			logEvent("Data passed to Input Terminal " 
					+ t.getAlias() + ", " + t.getName() 
					+ ": " + data);
		}
	} // logInputNotified
	
	/**
	 * Log that the channel received an acknowledgment that InputTerminal 
	 * t received data
	 * @param t the terminal that confirmed receiveing data
	 */
	protected final void logInputAcknowledged(InputTerminal t) {
		if (journalActivity) {
			logEvent("Acknowledgment received from Input Terminal " 
					+ t.getAlias() + ", " + t.getName() );
		}
	} // logInputAcknowledged
	
	/**
	 * Log that the channel acknowledged data receipt
	 * to OutputTerminal t
	 * @param t terminal that will receive an acknowledgment
	 */
	protected final void logOutputAcknowledged(OutputTerminal t) {
		if (journalActivity) {
			logEvent("Acknowledgment p Terminal " 
					+ t.getAlias() + ", " + t.getName() );
		}
	} // logOutputAcknowledged
	
	protected final void logOutputUnavailable(OutputTerminal t) {
		if (journalActivity) {
			logEvent("Channel " + getName() + ", " + getID()+ ", "
					+ "notified that Output Terminal " 
					+ t.getAlias() + ", " + t.getName() 
					+ ", retracted output");
		}
	} // logOutputUnavailable
	
} // class Channel
