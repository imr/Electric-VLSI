package com.sun.electric.tool.simulation.eventsim.handshakeNet.component;

import java.util.HashSet;
import java.util.Set;

import com.sun.electric.tool.simulation.eventsim.core.engine.Command;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.CompositeEntity;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.Entity;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.InputTerminal;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.OutputTerminal;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal.Terminal;

/*
 * 
 */
abstract public class ComponentWorker extends Entity {
	
	protected Set<InputTerminal> inputTerminals= new HashSet<InputTerminal>();
	protected Set<OutputTerminal> outputTerminals= new HashSet<OutputTerminal>();

	public ComponentWorker(String n) {
		super(n);
	} // constructor

	public ComponentWorker(String name, CompositeEntity g) {
		super(name, g);
	} // constructor
	
	/** 
	 * Attach an input terminal.
	 * Note that a Command must be provided - this command
	 * will be executed/scheduled by the terminal when an input
	 * hat arrived
	 * @param t terminal to be attached
	 * @param c input notification command
	 * @return true if input was successfully attached
	 */
	public boolean attachInput(InputTerminal t, Command c) {
		t.setWorker(this);
		t.setInputAvailableCommand(c);
		return inputTerminals.add(t);
	} // attachInput
	
	/**
	 * Attach an output terminal.
	 * Note that a Command must be provided - this command
	 * will be executed/scheduled by the terminal when an output 
	 * acknowledgment has arrived.
	 * @param t terminal to be attached
	 * @param c input notification command
	 * @return true if the output terminal was successfully attached
	 */
	public boolean attachOutput(OutputTerminal t, Command c) {
		t.setWorker(this);
		t.setOutputAckCommand(c);
		return outputTerminals.add(t);
	} // attachOutput
	
	public boolean isAttached(Terminal t) {
		return (inputTerminals.contains(t) || outputTerminals.contains(t));
	} // isAttached
	
	/**
	 * Self check. Override if needed to check anything.
	 */
	public boolean selfCheck() {
		return true;
	}
	
	Iterable<InputTerminal> inputIterable() {
		return inputTerminals;
	} // inputIterable
	
	Iterable<OutputTerminal> outputIterable() {
		return outputTerminals;
	} // outputIterable
	
} // class ComponentWorker
