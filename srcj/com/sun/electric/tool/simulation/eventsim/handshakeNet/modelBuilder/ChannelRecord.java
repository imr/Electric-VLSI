package com.sun.electric.tool.simulation.eventsim.handshakeNet.modelBuilder;

import java.util.Set;
import java.util.HashSet;

import com.sun.electric.tool.simulation.eventsim.core.common.Parameters;

/**
 * A record of a connection between components.
 * This connection corresponds to the concept of the channel
 *
 * @see com.sun.electric.tool.simulation.eventsim.handshakeNet.channel
 * @author ib27688
 * Created on May 16, 2005
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All rights reserved. Use is subject to license terms.
 */
public class ChannelRecord {

	/** connection name */
	public String name;
		
	/** parameters for the connection */
	public Parameters attributes=new Parameters();
	
	/** source line where the connection is defined */
	public int line;

	/** source connection points */
	public Set<ConnectionPoint> sources;
	/** destination connection points */
	public Set<ConnectionPoint> destinations;
	
	public ChannelRecord() {
		name= null;

		sources= new HashSet<ConnectionPoint>();
		destinations= new HashSet<ConnectionPoint>();
		
	} // ChannelRecord constructor 
	
	public String toString() {
		String result= "Connection record:\n"
			+ " name = " + name + "\n"
			+ "sources= " + sources + "\n" 
			+ "destinations=" + destinations;
		
		if (attributes != null) {
			result+= "attributes= " + attributes.toString() + "\n";
		}
		else {
			result+= "attributes= null\n";
		}
		
		return result;
	}
	
	
} // class Connection record
