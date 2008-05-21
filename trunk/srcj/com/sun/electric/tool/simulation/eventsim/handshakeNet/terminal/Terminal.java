package com.sun.electric.tool.simulation.eventsim.handshakeNet.terminal;

import java.util.Random;

import com.sun.electric.tool.simulation.eventsim.core.hierarchy.CompositeEntity;
import com.sun.electric.tool.simulation.eventsim.core.hierarchy.Entity;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.channel.Channel;
import com.sun.electric.tool.simulation.eventsim.handshakeNet.component.ComponentWorker;

/**
 * Common functionality for Input and Output terminals
 * @author ib27688
 *
 */

abstract public class Terminal extends Entity {
	
	/**
	 * component that the terminal is attached to
	 */
	protected ComponentWorker myWorker= null;
	protected Channel myChannel= null;
	
	// needed for delay variation
	protected Random rndGen= new Random();
	
	public Terminal(String n) {
		super(n);
	}

	public Terminal(String name, CompositeEntity g) {
		super(name, g);
	}
		
	// connection setters and accessors
	
	public void setWorker(ComponentWorker w) {
		myWorker= w;
	} // setWorker
	
	public ComponentWorker getWorker() {
		return myWorker;
	} // getComponent
	
	public void setChannel(Channel c) {
		myChannel= c;
	} // setChannel

	public Channel getChannel() {
		return myChannel;
	} // getChannel
	
	public boolean isAttached(Channel c) {
		return (myChannel==c);
	} // isAttached
	
	public boolean isAttached(ComponentWorker c) {
		return (myWorker==c);
	} // isAttached	
	
	@Override
	public boolean selfCheck() {
		boolean check= true;
		if (myWorker == null) {
			logError("Self check failed: myWorker == null");
			check= false;
		}
		
		if (myChannel == null) {
			logError("Self check failed: myChannel == null");
			check= false;
		}
		
		if (myChannel!= null && !myChannel.isAttached(this)) {
			logError("Self check failed: myChannel.isAttached(this) == false");
			check= false;
		}
		
		if (myWorker != null && !myWorker.isAttached(this)) {
			logError("Self check failed: myWorker.isAttached(this) == false");
			check= false;			
		}

		return check;
	}

	
} // class Terminal
