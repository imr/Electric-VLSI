/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Analyzer.java
 * IRSIM simulator
 * Translated by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (C) 1988, 1990 Stanford University.
 * Permission to use, copy, modify, and distribute this
 * software and its documentation for any purpose and without
 * fee is hereby granted, provided that the above copyright
 * notice appear in all copies.  Stanford University
 * makes no representations about the suitability of this
 * software for any purpose.  It is provided "as is" without
 * express or implied warranty.
 */
package com.sun.electric.plugins.irsim;

import com.sun.electric.api.irsim.IAnalyzer;

import java.net.URL;

/**
 *
 */
public class IAnalyzerImpl implements IAnalyzer {

    /**
     * Create IRSIM Simulation Engine to simulate a cell.
     * @param gui interface to GUI
     * @param steppingModel stepping model either "RC" or "Linear"
     * @param parameterURL URL of IRSIM parameter file
     * @param irDebug debug flags
     * @param showCommands tru to print issued IRSIM commands
     * @param isDelayedX true if using the delayed X model, false if using the old fast-propagating X model.
     */
    public EngineIRSIM createEngine(IAnalyzer.GUI gui, String steppingModel, URL parameterURL, int irDebug, boolean showCommands, boolean isDelayedX) {
        SimAPI sim = new Sim(irDebug, steppingModel, isDelayedX);
        Analyzer theAnalyzer = new Analyzer(gui, sim, irDebug, showCommands);

        // read the configuration file
        if (parameterURL != null) {
            sim.loadConfig(parameterURL, theAnalyzer);
        }
        sim.initNetwork();

        System.out.println("IRSIM, version " + Analyzer.simVersion);
        // now initialize the simulator
        theAnalyzer.initRSim();
        return new IAnalyzerLogger(theAnalyzer);
//                return theAnalyzer;
    }
}
