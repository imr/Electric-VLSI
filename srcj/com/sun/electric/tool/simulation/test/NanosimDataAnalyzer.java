/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NanosimDataAnalyzer.java
 * Written by Jonathan Gainsley, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Electric(tm) is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Electric(tm); see the file COPYING.  If not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, Mass 02111-1307, USA.
 */
package com.sun.electric.tool.simulation.test;

import java.util.*;

public class NanosimDataAnalyzer extends NanosimBERT.NanosimTerminal implements BERT.DataAnalyzer {

    private int delayPeriods;
    private double delayNS;
    private NanosimDataGen checkGenerator;
    private StringBuffer capturedData;
    private String pinName;
    private double voltageLowThreshold;
    private double voltageHighThreshold;
    private int numErrors;                 // true if error during check phase of BER testing

    /**
     * Constructor
     * @param parentModule the parent Module that this Terminal is part of
     */
    NanosimDataAnalyzer(NanosimBERT.NanosimDataModule parentModule) {
        super(parentModule);
        delayPeriods = 0;
        delayNS = 0;
        capturedData = new StringBuffer();
        pinName = null;
        voltageLowThreshold = voltageHighThreshold = 0.5;
        numErrors = 0;
    }

    public void measureBER(BERT.DataGenerator expectedData) {
        if (!(expectedData instanceof NanosimDataGen)) {
            System.out.println("Invalid argument to NanosimDataAcquire.acquireDataFor(): can only acquire data for NanosimDataGen");
            return;
        }
        checkGenerator = (NanosimDataGen)expectedData;
    }

    public void setDelay(int periods, double ns) {
        this.delayNS = ns;
        this.delayPeriods = periods;
    }

    public void setPinName(String pin) {
        pinName = pin;
    }

    public String getPinName() {
        return pinName;
    }

    public void setVoltageThreshold(double voltageLowThreshold, double voltageHighThreshold) {
        this.voltageLowThreshold = voltageLowThreshold;
        this.voltageHighThreshold = voltageHighThreshold;
    }

    public double getMeasuredBER() {
        if (capturedData.length() == 0) return 0;
        return (double)numErrors / (double)(capturedData.length());
    }

    /**
     * Convert the voltage to a valid state 0 or 1.
     * If the voltage does not translate to a valid state,
     * -1 is returned
     * @param voltage the voltage to convert
     * @return 0 or 1, or -1 if not a valid voltage state
     */
    public int voltageToState(double voltage) {
        if (voltage < voltageLowThreshold) return 0;
        if (voltage >= voltageHighThreshold) return 1;
        return -1;
    }

    private double getDelay() {
        double delay = getParentModule().getPeriod() * delayPeriods + delayNS;
        return delay;
    }

    void logError() {
        numErrors++;
    }

    /**
     * Return a Set of {@link NanosimBERT.Event} objects
     * that characterize the behavior of the Module
     * when the BERT runs (BERT starts at time 0).
     * @param stopTimeNS the time to the BERT will stop,
     * with the BERT start time being time zero.
     * @return a Set of Events
     */
    public Set generateEvents(double startTimeNS, double stopTimeNS) {
        Set set = new TreeSet();

        double curTime = startTimeNS;
        curTime += getDelay();

        String node = pinName;
        if (node == null) {
            System.out.println("Error: no pin defined for DataAnalyzer, will not capture or check data");
            return set;
        }

        if (checkGenerator != null) {
            // check and capture
            int bitIndex = 0;
            while (curTime < stopTimeNS) {
                boolean val = checkGenerator.getBit(bitIndex);
                NanosimBERT.Event e = NanosimBERT.Event.createCaptureAndCheckNodeEvent(curTime, node, val ? 1 : 0, this);
                set.add(e);
                curTime += getParentModule().getPeriod();
                bitIndex++;
            }
        } else {
            // capture only
            while (curTime < stopTimeNS) {
                NanosimBERT.Event e = NanosimBERT.Event.createCaptureNodeEvent(curTime, node, this);
                set.add(e);
                curTime += getParentModule().getPeriod();
            }
        }
        return set;
    }

    void captureBit(boolean b) {
        capturedData.append(b ? '1' : '0');
    }

    public BitVector getCapturedData() {
        return new BitVector(capturedData.toString(), "captured data");
    }

    public void clearCapturedData() {
        capturedData.delete(0, capturedData.length());
        numErrors = 0;
    }

    public void printCapturedData() {
        String status = checkGenerator == null ? "" : (numErrors != 0 ? "ERRORS!" : "OK");
        System.out.println("Analyzer on pin '"+getPinName()+"' captured "+status+": "+getCapturedData().getState());
    }

    /** Unit Test */
    public static void main(String args[]) {
        // create Module, set frequency (data gen needs frequency defined)
        NanosimBERT.NanosimDataModule module = new NanosimBERT.NanosimDataModule();
        module.setFrequency(1000000);
        // create Generator, define pin (data gen needs one pin defined)
        NanosimDataGen gen = (NanosimDataGen)module.createDataGenerator();
        gen.addPinName("testpin");
        // create Analyzer
        NanosimDataAnalyzer ana = (NanosimDataAnalyzer)module.createDataAnalyzer();
        ana.setPinName("testpinOut");
        ana.setDelay(1, 0.5);

        double time = 10;
        System.out.println("Capture only, "+time+"ns");
        NanosimBERT.printEvents(ana.generateEvents(0, time));

        time = 30;
        System.out.println("-----------------------------------");
        System.out.println("Capture plus Check, "+time+"ns");
        gen.setPattern(new BitVector("1011110", "blah"), new BitVector("0100001011", ""));
        ana.measureBER(gen);
        NanosimBERT.printEvents(ana.generateEvents(0, time));
    }
}
