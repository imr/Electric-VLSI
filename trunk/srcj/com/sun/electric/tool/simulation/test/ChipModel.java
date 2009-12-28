package com.sun.electric.tool.simulation.test;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.*;

/*
 * ChipModel.java
 *
 * Copyright (c) 2004,2005 by Sun Microsystems, Inc.
 *
 * Created on Apr 20, 2005
 */

/**
 * The ChipModel is meant to abstract the underlying device under test. This
 * device may be a real chip, hooked up to measurement devices in the lab,
 * or it may be a Nanosim simulation, driven by Nanosim models of measurement
 * devices.
 * <P>
 * @author gainsley
 */
public interface ChipModel {

    /**
     * Wait for the specified number of seconds. During this
     * time the chip will run, assuming any activity is set to run
     * on the chip.
     * @param seconds the number of seconds to wait.
     */
    public void wait(float seconds);

    /**
     * Wait for the specified number of nanoseconds. During this
     * time the chip will run, assuming any activity is set to run
     * on the chip.
     * @param nanoseconds the number of nanoseconds to wait.
     */
    public void waitNS(double nanoseconds);

    /**
     * Wait for the specified number of picoseconds. During this
     * time the chip will run, assuming any activity is set to run
     * on the chip.
     * @param picoseconds the number of picoseconds to wait.
     */
    public void waitPS(double picoseconds);

    

}
