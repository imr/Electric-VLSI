/*
 * Created on Sep 7, 2004
 *
 * Copyright (c) 2004,2005 by Sun Microsystems, Inc.
 *
 */
package com.sun.electric.tool.simulation.test;

/**
 * Device-independent interface to something (e.g., power supply or
 * digitial multimeter) that can read back voltage
 * @author Tom O'Neill (toneill)
 */
public interface VoltageReadable {
    
    /** Returns voltage, in Volts */
    public abstract float readVoltage();
}
