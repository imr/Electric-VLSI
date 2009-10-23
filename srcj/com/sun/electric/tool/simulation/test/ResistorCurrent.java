package com.sun.electric.tool.simulation.test;

/*
 * ResistorCurrent.java
 * 
 * Copyright (c) 2005 by Sun Microsystems, Inc.
 *
 * Created on January 18, 2005
 */

/**
 * Infers current through a resistor from a voltage measurement, implements
 * <code>CurrentReadable</code>.
 * 
 * @author Tom O'Neill (toneill)
 */

public class ResistorCurrent implements CurrentReadable {

    /** Resistance in Ohms */
    public float ohms;

    /** Readout of voltage across resistor */
    public VoltageReadable voltmeter;
    
    /**
     * Create object to indirectly measure current through a resistor.
     * 
     * @param ohms Resistance in Ohms
     * @param voltmeter Readout of voltage across resistor 
     */
    public ResistorCurrent(float ohms, VoltageReadable voltmeter) {
        super();
        this.ohms = ohms;
        this.voltmeter = voltmeter;
    }
    

    /**
     * Returns voltage across resistor divided by resistance.
     * 
     * @return current through the resistor
     * @see com.sun.electric.tool.simulation.test.CurrentReadable#readCurrent()
     */
    public float readCurrent() {
        return voltmeter.readVoltage()/ohms;
    }
    
    /**
     * Returns voltage across resistor divided by resistance.
     * 
     * @return current through the resistor
     * @see com.sun.electric.tool.simulation.test.CurrentReadable#readCurrent()
     */
    public float readCurrent(float ampsExpected, float ampsResolution) {
        Infrastructure.fatal("NOt implemented yet");
        return 0.f;
    }
    
    /** Unit test */
    public static void main(String[] args) {
    }

}
