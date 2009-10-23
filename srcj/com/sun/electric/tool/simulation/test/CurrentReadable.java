/*
 * Created on Aug 27, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package com.sun.electric.tool.simulation.test;

/**
 * Device-independent interface to something (e.g., power supply or digitial
 * multimeter) that can read back current
 * 
 * @author Tom O'Neill (toneill)
 *
 * Copyright (c) 2004,2005 by Sun Microsystems, Inc.
 *
 */
public interface CurrentReadable {

    /**
     * Measures current using default range and resolution.
     * 
     * @return current in Amps
     */
    public float readCurrent();

    /**
     * Measures current using range appropriate for <code>ampsExpected</code>,
     * and resolution of <code>ampsResolution</code>.
     * 
     * @param ampsExpected
     *            expected value of current in amps, for range setting
     * @param ampsResolution
     *            desired resolution for measurement, in amps
     * @return current in Amps
     */
    public float readCurrent(float ampsExpected, float ampsResolution);

}
