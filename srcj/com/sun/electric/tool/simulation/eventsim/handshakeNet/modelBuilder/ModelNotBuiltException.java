/*
 * Created on May 18, 2005
 */
package com.sun.electric.tool.simulation.eventsim.handshakeNet.modelBuilder;

/**
 *
 * Copyright © 2005 Sun Microsystems Inc. All rights reserved. Use is subject to license terms.
 * 
 * @author ib27688
 *
 * Thrown when a component cannot be built.
 */
public class ModelNotBuiltException extends Exception {
	public ModelNotBuiltException(String msg) {
		super(msg);
	}

	public ModelNotBuiltException(String msg, Throwable t) {
		super(msg,t);
	}	
	
	public ModelNotBuiltException(Throwable t) {
		super(t);
	}
	
	static final long serialVersionUID= 42;
	
} // ModelNotBuiltException
