/*
 * LogicSettable.java
 * 
 * Copyright (c) 2004,2005 by Sun Microsystems, Inc.
 *
 * Created on ??
 */

package com.sun.electric.tool.simulation.test;

/**
 * Generic interface for setting logic levels for outputs to chip
 * @author Tom O'Neill (toneill)
 */
public interface LogicSettable {
	
    /**
     * @return Current value for Logic State
     */
    public abstract boolean isLogicStateHigh();
    
    /**
     * Sets logic state to requested value
     * @param logicState New value for logic state
     */
    public abstract void setLogicState(boolean logicState);
}
