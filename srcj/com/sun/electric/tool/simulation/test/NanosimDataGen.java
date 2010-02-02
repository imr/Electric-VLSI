/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NanosimDataGen.java
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

public class NanosimDataGen extends NanosimBERT.NanosimTerminal implements BERT.DataGenerator {

    private BitVector startPattern;         // start of pattern
    private BitVector repeatPattern;        // repeat of pattern
    private double delay;                   // delay before start of pattern
    private BERT.SignalMode signalMode;
    private List pinNames;
    private List pinNamesComplement;
    private double voltageLow;
    private double voltageHigh;
    private boolean enableComplement;

    NanosimDataGen(NanosimBERT.NanosimDataModule parentModule) {
        super(parentModule);
        startPattern = new BitVector(0, "start pattern");
        repeatPattern = new BitVector(0, "repeat pattern");
        signalMode = BERT.NONRETURNTOZERO;
        this.pinNames = new ArrayList();
        this.pinNamesComplement = new ArrayList();
        this.voltageLow = 0;
        this.voltageHigh = -1;
        enableComplement = false;
        delay = 0;
    }

    public void setPattern(BitVector start, BitVector repeat) {
        setPattern(start.getState(), repeat.getState());
    }

    public void setPattern(String start, String repeat) {
        startPattern = new BitVector(start, "start pattern");
        repeatPattern = new BitVector(repeat, "repeat pattern");
    }

    public void setDelay(double nanoseconds) {
        delay = nanoseconds;
    }

    public void setSignalMode(BERT.SignalMode mode) {
        this.signalMode = mode;
    }

    public void setClock() {
        setPattern("", "1");
        setSignalMode(BERT.RETURNTOZERO);
    }

    public void setDDRClock() {
        setPattern("", "10");
        setSignalMode(BERT.NONRETURNTOZERO);
    }

    public void addPinName(String pinName) {
        pinNames.add(pinName);
    }

    public void addPinNameComplement(String pinName) {
        pinNamesComplement.add(pinName);
    }

    public Iterator getPinNames() {
        return pinNames.iterator();
    }

    public Iterator getPinNamesComplement() {
        return pinNamesComplement.iterator();
    }

    public void clearPins() {
        pinNames.clear();
        pinNamesComplement.clear();
    }

    public void setVoltage(double voltageLow, double voltageHigh) {
        this.voltageLow = voltageLow;
        this.voltageHigh = voltageHigh;
    }

    public void enableComplement(boolean enable) {
        this.enableComplement = enable;
    }

    protected BitVector getStartPattern() { return startPattern; }
    protected BitVector getRepeatPattern() { return repeatPattern; }

    public String getStartState() { return startPattern.getState(); }
    public String getRepeatState() { return repeatPattern.getState(); }

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
        double curTime = startTimeNS + delay;
        int bitIndex = 0;

        boolean lastVal = false;
        while (curTime < stopTimeNS) {
            boolean val = getBit(bitIndex);
            if (bitIndex != 0 && val == lastVal) {
                // value has not changed, don't generate set event
            } else {
                // generate event to set new value on all pins
                for (Iterator it = getPinNames(); it.hasNext(); ) {
                    String node = (String)it.next();
                    NanosimBERT.Event e = NanosimBERT.Event.createSetNodeEvent(
                            curTime, node, val ? voltageHigh : voltageLow);
                    set.add(e);
                }
                // generate complement events if any
                if (enableComplement) {
                    for (Iterator it = getPinNamesComplement(); it.hasNext(); ) {
                        String node = (String)it.next();
                        NanosimBERT.Event e = NanosimBERT.Event.createSetNodeEvent(
                                curTime, node, val ? voltageLow : voltageHigh);
                        set.add(e);
                    }
                }
            }
            lastVal = val;
            // if using return to zero, advance half period and set to zero if necessary
            if (signalMode == BERT.RETURNTOZERO && val != false) {
                double rtzTime = curTime + getParentModule().getPeriod()/2;
                // generate event to set new value on all pins
                for (Iterator it = getPinNames(); it.hasNext(); ) {
                    String node = (String)it.next();
                    NanosimBERT.Event e = NanosimBERT.Event.createSetNodeEvent(
                            rtzTime, node, voltageLow);
                    set.add(e);
                }
                // generate complement events if any
                if (enableComplement) {
                    for (Iterator it = getPinNamesComplement(); it.hasNext(); ) {
                        String node = (String)it.next();
                        NanosimBERT.Event e = NanosimBERT.Event.createSetNodeEvent(
                                curTime, node, voltageHigh);
                        set.add(e);
                    }
                }
                lastVal = false;
            }
            bitIndex++;
            curTime += getParentModule().getPeriod();
        }
        return set;
    }

    /**
     * Get the bit at the specified location in the pattern, where zero
     * is the first bit in the pattern.
     * @param bitIndex location in the pattern
     * @return the bit.
     */
    public boolean getBit(int bitIndex) {
        // check if bitIndex is part of start pattern
        if (bitIndex < startPattern.getNumBits())
            return startPattern.get(bitIndex);
        // if not, must be part of repeat pattern
        int len = repeatPattern.getNumBits();
        bitIndex -= startPattern.getNumBits();
        if (len > 0) {
            int loc = bitIndex % len;
            return repeatPattern.get(loc);
        } else {
            // if repeat pattern empty, return last bit of start pattern
            if (startPattern.getNumBits() > 0)
                return startPattern.get(startPattern.getNumBits()-1);
            else
                return false;   // if both patterns empty, just return false
        }
    }

    /** Unit Test */
    public static void main(String args[]) {
        // create Module, set frequency (data gen needs frequency defined)
        NanosimBERT.NanosimDataModule module = new NanosimBERT.NanosimDataModule();
        module.setFrequency(1000000);
        // create Generator, define pin (data gen needs one pin defined)
        NanosimDataGen gen = (NanosimDataGen)module.createDataGenerator();
        gen.addPinName("testpin");

        // test generation of events
        System.out.println("Empty events:");
        NanosimBERT.printEvents(gen.generateEvents(0, 20));

        System.out.println("-----------------------------------");
        gen.setPattern(new BitVector("10010101011", "blah"), new BitVector("", ""));
        System.out.println("Start pattern only events: ");
        NanosimBERT.printEvents(gen.generateEvents(0, 20));

        System.out.println("-----------------------------------");
        gen.setPattern(new BitVector("", "blah"), new BitVector("0101010001011", ""));
        System.out.println("Repeat pattern only events: ");
        NanosimBERT.printEvents(gen.generateEvents(0, 20));

        System.out.println("-----------------------------------");
        gen.setPattern(new BitVector("10111100011010", "blah"), new BitVector("0100001011", ""));
        System.out.println("Start and Repeat pattern events: ");
        NanosimBERT.printEvents(gen.generateEvents(0, 50));

        System.out.println("-----------------------------------");
        System.out.println("Start and Repeat pattern events (ReturnToZero): ");
        gen.setSignalMode(BERT.RETURNTOZERO);
        NanosimBERT.printEvents(gen.generateEvents(0, 20));

        System.out.println("-----------------------------------");
        System.out.println("Clock mode: ");
        gen.setClock();
        NanosimBERT.printEvents(gen.generateEvents(0, 20));

    }

}
