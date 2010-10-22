/*
 * Created on May 17, 2005
 *
 */
package com.sun.electric.tool.simulation.eventsim.handshakeNet.modelBuilder;

/**
 *
 * Copyright (c) 2005, Oracle and/or its affiliates. All rights reserved.
 * 
 * @author ib27688
 * 
 * Representation of a connection point: A component and its terminal.
 *
 */
public class ConnectionPoint {
	
	// names of componens and terminals for connection point
	public String componentName;
	public String terminalName;
	
	/** the line where the connection definition starts */
	public int line;
	
	public boolean equals(Object o) {
		boolean eq= false;
		if (o instanceof ConnectionPoint) {
			// cast is safe
			ConnectionPoint cp= (ConnectionPoint)o;
			eq= componentName.equals(cp.componentName)
				&& terminalName.equals(cp.terminalName);
		}
		return eq;
	} // equals

} // class ConnectionPoint
