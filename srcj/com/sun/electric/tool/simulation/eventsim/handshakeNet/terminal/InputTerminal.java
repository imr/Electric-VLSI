package com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal;


import com.sun.electric.tool.simulation.eventsim.core.engine.Command;
import com.sun.electric.tool.simulation.eventsim.core.engine.EventSimErrorException;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.CompositeEntity;
import com.sun.electric.tool.simulation.eventsim.core.simulation.Delay;

/**
 * Input terminal connects a Component to a Channel, and acts as an input to the
 * component.
 * @author ib27688
 */
abstract public class InputTerminal extends Terminal {
	
	protected Delay inputAvailableDelay= TerminalDefaults.INPUT_AVAILABLE_DELAY_DEF;
	protected int inputAvailableDelayVariation= TerminalDefaults.INPUT_AVAILABLE_DELAY_VARIATION_DEF;
	protected Delay inputUnavailableDelay= TerminalDefaults.INPUT_UNAVAILABLE_DELAY_DEF;
	protected int inputUnavailableDelayVariation= TerminalDefaults.INPUT_UNAVAILABLE_DELAY_VARIATION_DEF;
	protected Delay inputAckDelay= TerminalDefaults.INPUT_ACK_DELAY_DEF;
	protected int inputAckDelayVariation= TerminalDefaults.INPUT_ACK_DELAY_VARIATION_DEF;
	
	/** to be engaged when an input becomes available */
	protected Command inputAvailableCmd= null;
	/** to be engaged when an input is retracted */
	protected Command inputUnavailableCmd= null;
	
	protected boolean inputHere= false;
	
	public InputTerminal(String n) {
		super(n);
		setDelays();
	} // constructor

	public InputTerminal(String name, CompositeEntity g) {
		super(name, g);
		setDelays();
	} // constructor
	
	/** Set the command that the terminal invokes when an input becomes available */
	public void setInputAvailableCommand(Command c) {
		inputAvailableCmd= c;
	} // setInputAvailableCommand

	/** 
	 * Set the command that the terminal invokes when an input is retracted.
	 * It is the responsibilty of the component worket to provide this command.
	 */
	public void setInputUnavailableCommand(Command c) {
		inputUnavailableCmd= c;
	} // setInputAvailableCommand
	
	/**
	 * Set delays with "global values", otherwise let them remain at their
	 * default values.
	 */
	protected void setDelays() {
		Integer ad= globals.intValue(TerminalDefaults.INPUT_AVAILABLE_DELAY);
		if (ad != null) inputAvailableDelay= new Delay(ad);
		Integer adv= globals.intValue(TerminalDefaults.INPUT_AVAILABLE_DELAY_VARIATION);
		if (adv != null) inputAvailableDelayVariation= adv;

		Integer uad= globals.intValue(TerminalDefaults.INPUT_UNAVAILABLE_DELAY);
		if (uad != null) inputUnavailableDelay= new Delay(uad);
		Integer uadv= globals.intValue(TerminalDefaults.INPUT_UNAVAILABLE_DELAY_VARIATION);
		if (uadv != null) inputUnavailableDelayVariation= uadv;
		
		Integer ackd= globals.intValue(TerminalDefaults.INPUT_ACK_DELAY);
		if (ackd != null) inputAckDelay= new Delay(ackd);
		Integer ackdv= globals.intValue(TerminalDefaults.INPUT_ACK_DELAY_VARIATION);
		if (ackdv != null) inputAckDelayVariation= ackdv;
		
	} // setDelays

	
	/**
	 * Get data from the attached channel.
	 * @returnthe data that the channel is passing to the input terminal
	 */
	public Object getData() {
		return myChannel.getData();
	} // getData
	
	/**
	 * The channel informs the terminal that an input is available.
	 * The input informs the component worker of the input,
	 * possibly after a delay.
	 */
	abstract public void inputAvailable(Object data);
	
	/**
	 * The channel informs the terminal that an input has been retracted.
	 * The input informs the component worker of the input retraction,
	 * possibly after a delay.
	 * @throws EventSimErrorException 
	 */	
	abstract public void inputUnavailable() throws EventSimErrorException;
	
	/**
	 * Let the channel know that the input has been consumed.
	 * @throws EventSimErrorException 
	 */
	abstract public void ackInput() throws EventSimErrorException;

	/** Acknowldege with an extra delay 
	 * which is cummulative with terminal's own delay
	 * @param d extra delay for the acknowledgment 
	 * @throws EventSimErrorException 
	 */
	abstract public void ackInput(Delay d) throws EventSimErrorException;
	
	// delay setters and accessors
	public void setInputAvailableDelay(Delay d) {
		inputAvailableDelay= d;
	} // setInputAvailableDelay
	
	public Delay getInputAvailableDelay() {
		return inputAvailableDelay;
	} // getInputAvailableDelay
	
	public void setInputUnavailableDelay(Delay d) {
		inputUnavailableDelay= d;
	} // setInputUnavailableDelay
	
	public Delay getInputUnavailableDelay() {
		return inputUnavailableDelay;
	} // getInputUnavailableDelay
	
	public void setInputAckDelay(Delay d) {
		inputAckDelay= d;
	} // setInputAckDelay
	
	public Delay getInputAckDelay() {
		return inputAckDelay;
	} // getInputAckDelay
	
	public boolean hasInput() {
		return inputHere;
	}
	
	public boolean selfCheck() {
		boolean check= super.selfCheck();
		
		if (inputAvailableCmd == null) {
			logError("Self check failed: inputAvailableCmd == null");
			check= false;
		}
		
		// not implemented
		// check= check && (inputUnavailableCmd != null);
		return check;
	} // selfCheck
	
	public final void logInputAvailable() {
		if (journalActivity) {
			logEvent("Input arrived from Channel "
					+ myChannel.getAlias() + ", id= " + myChannel.getID() 
					+ ": " + getData());
		}
	} // logInputAvailable
	
	public final void logInputUnavailable() {
		if (journalActivity) {
			logEvent("Channel retracted data" 
					+ myChannel.getAlias() + ", id= " + myChannel.getID());
		}
	} // logInputUnavailable
	
	public final void logAckInput() {
		if (journalActivity) {
			logEvent("Input acknowledged by component worker "
					+ myWorker.getAlias() + ", " + myWorker.getID());
		}
	} // logAckInput
	
	
} // class InputTerminal
