/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Name.java
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

public class NanosimBERT implements BERT {

    // The Set of Events from all modules to issue to the Nanosim
    // simulator when the BERT is run().
    private Set events;

    // The Nanosim simulator
    private NanosimModel model;

    // all modules in this BERT
    private List modules;

    /**
     * Create a new NanosimBERT.
     * @param model the nanosim model that this BERT will use
     */
    public NanosimBERT(NanosimModel model) {
        this.model = model;
        events = new TreeSet();
        modules = new ArrayList();
    }

    public DataModule createDataModule() {
        DataModule mod = new NanosimDataModule();
        modules.add(mod);
        return mod;
    }

    /**
     * Run the BERT and its configured modules for the specified
     * time in nanoseconds. This is a replacement for doing
     * start, wait(nanoseconds), stop.
     * @param nanoseconds
     */
    public void run(double nanoseconds) {
        // current nanosim simulation time
        double nsStartTime = model.getTimeNS();

        // get all events from all modules
        events = getAllEvents(nsStartTime, nsStartTime+nanoseconds);

        // run all events in order until stop time
        double nsCurrentTime = nsStartTime;
        for (Iterator it = events.iterator(); it.hasNext(); ) {
            Event e = (Event)it.next();
            if (e.eventTimeNS.doubleValue() <= nsCurrentTime) {
                // issue event
                e.doIt(model);
            } else {
                // advance current time, then do it
                double waitTime = e.eventTimeNS.doubleValue() - nsCurrentTime;
                model.waitNS(waitTime);
                nsCurrentTime = model.getTimeNS();
                e.doIt(model);
            }
        }
        // if any analyzers, print out captured data for reference
        for (Iterator it = modules.iterator(); it.hasNext(); ) {
            NanosimDataModule module = (NanosimDataModule)it.next();
            for (Iterator it2 = module.getTerminals(); it2.hasNext(); ) {
                NanosimTerminal terminal = (NanosimTerminal)it2.next();
                if (!terminal.isEnabled()) continue;
                if (terminal instanceof NanosimDataAnalyzer) {
                    NanosimDataAnalyzer ana = (NanosimDataAnalyzer)terminal;
                    ana.printCapturedData();
                }
            }
        }
    }

    /**
     * Same as {@link #run(double)}, but in minutes for
     * very long runs.
     * @param minutes number of minutes to run
     */
    public void runMinutes(double minutes) {
        System.out.println("The Nanosim BERT does not support run(minutes) because it would never finish.");
    }

    private Set getAllEvents(double nsStartTime, double nsEndTime) {
        Set set = new TreeSet();
        for (Iterator it = modules.iterator(); it.hasNext(); ) {
            NanosimDataModule module = (NanosimDataModule)it.next();
            for (Iterator it2 = module.getTerminals(); it2.hasNext(); ) {
                NanosimTerminal terminal = (NanosimTerminal)it2.next();
                if (!terminal.isEnabled()) continue;
                if (terminal instanceof NanosimDataAnalyzer) {
                    NanosimDataAnalyzer ana = (NanosimDataAnalyzer)terminal;
                    ana.clearCapturedData();
                }
                Set modevents = terminal.generateEvents(nsStartTime, nsEndTime);
                set.addAll(modevents);
            }
        }
        return set;
    }

    // ======================== Support Classes ==========================

    public static class NanosimDataModule implements DataModule {

        // period in nanoseconds (1/freq)
        private double period;
        private List terminals;

        public NanosimDataModule() {
            period = 1;
            terminals = new ArrayList();
        }

        public DataGenerator createDataGenerator() {
            NanosimDataGen gen = new NanosimDataGen(this);
            terminals.add(gen);
            return gen;
        }

        public DataAnalyzer createDataAnalyzer() {
            NanosimDataAnalyzer ana = new NanosimDataAnalyzer(this);
            terminals.add(ana);
            return ana;
        }

        public void setFrequency(double freqKHz) {
            period = 1/freqKHz * 1000000;
        }

        public double getFrequency() {
            return 1/period * 1000000;
        }

        /** Return the period in nanoseconds */
        public double getPeriod() {
            return period;
        }

        /**
         * Return iterator over Terminal objects
         * that are part of this module
         * @return Iterator over Terminals.
         */
        protected Iterator getTerminals() {
            return terminals.iterator();
        }
    }

    public static abstract class NanosimTerminal implements Terminal {

        private NanosimDataModule parent;
        private boolean enabled;

        protected NanosimTerminal(NanosimDataModule parent) {
            this.parent = parent;
            enabled = true;
        }

        /**
         * Return a Set of {@link NanosimBERT.Event} objects
         * that characterize the behavior of the Module
         * when the BERT runs (BERT starts at time 0).
         * @param stopTimeNS the time to the BERT will stop,
         * with the BERT start time being time zero.
         * @return a Set of Events
         */
        public abstract Set generateEvents(double startTimeNS, double stopTimeNS);

        /**
         * Get the parent Module
         * @return the parent Module
         */
        public DataModule getParentModule() { return parent; }

        public void setEnabled(boolean enable) {
            this.enabled = enable;
        }

        public boolean isEnabled() { return enabled; }
    }


    /**
     * An Event to send to the Nanosim Simulator,
     * the same as the BERT would transition a signal
     * and send that to the real chip. Nanosim modules
     * generate such events.
     */
    public static class Event implements Comparable {
        private final Double eventTimeNS;
        private final int type;
        private final String node;
        private final double voltage;
        private final int state;
        private final NanosimDataAnalyzer analyzer;

        // Note that order is defined by type values, set should be last,
        // because a set and a get to the same node at the same time will return the old value,
        // not the new 'set' value
        private static final int TYPE_CAPTURENODE = 1;
        private static final int TYPE_CAPTUREANDCHECKNODE = 2;
        private static final int TYPE_SETNODE = 3;

        /**
         * Create a new Event to set the node to the value
         * at the specified time
         * @param eventTimeNS the time the event will occur
         * @param node the node to set
         * @param voltage the voltage to set the node to
         * @return the Event
         */
        protected static Event createSetNodeEvent(double eventTimeNS, String node,
                                                  double voltage) {
            // args state, analyzer are not used
            return new Event(eventTimeNS, TYPE_SETNODE, node, 0, voltage, null);
        }

        /**
         * Create a new Event to capture the node's value at the specified time.
         * @param eventTimeNS the time
         * @param node which node
         * @param analyzer the analyzer to report the capture to
         * @return the Event
         */
        protected static Event createCaptureNodeEvent(double eventTimeNS, String node,
                                                      NanosimDataAnalyzer analyzer) {
            // args state, voltage are not used
            return new Event(eventTimeNS, TYPE_CAPTURENODE, node, 0, 0, analyzer);
        }
        
        /**
         * Create a new Event to check the node for the given
         * value at the specified time. This also captures the data in the given analyzer.
         * @param eventTimeNS
         * @param node
         * @param expectedState
         * @param analyzer the analyzer to report the capture to
         * @return the Event
         */
        protected static Event createCaptureAndCheckNodeEvent(double eventTimeNS, String node,
                                                    int expectedState, NanosimDataAnalyzer analyzer) {
            // arg voltage is not used
            return new Event(eventTimeNS, TYPE_CAPTUREANDCHECKNODE, node, expectedState, 0, analyzer);
        }

        private Event(double eventTimeNS, int type, String node, int state, double voltage,
                      NanosimDataAnalyzer capturer) {
            this.eventTimeNS = new Double(eventTimeNS);
            this.type = type;
            this.node = node;
            this.state = state;
            this.voltage = voltage;
            this.analyzer = capturer;
        }

        private void doIt(NanosimModel model) {
            switch(type) {
                case TYPE_SETNODE: {
                    model.setNodeVoltage(node, voltage);
                    return;
                }
                case TYPE_CAPTURENODE: {
                    captureNode(model);
                    return;
                }
                case TYPE_CAPTUREANDCHECKNODE: {
                    int curState = captureNode(model);
                    if (curState != state) {
                        System.out.println("Check Node from BERT Data Analyzer on pin '"+
                                analyzer.getPinName()+"' failed @ sim time="+eventTimeNS+
                                "ns, expected "+state+" but was "+curState);
                        analyzer.logError();
                    }
                    return;
                }
            }
        }

        private int captureNode(NanosimModel model) {
            double curState = model.getNodeVoltage(node);
            int state = analyzer.voltageToState(curState);
            if (state == -1) {
                System.out.println("Error: analyzer captured node on pin "+analyzer.getPinName()+
                        " of voltage "+curState+" at time "+eventTimeNS+"ns, which is not a valid low or high value.");
                state = 0;
            }
            boolean b = state == 1 ? true : false;
            analyzer.captureBit(b);
            return state;
        }

        public int compareTo(Object o) {
            // check time first
            Event otherEvent = (Event)o;
            int compare = eventTimeNS.compareTo(otherEvent.eventTimeNS);
            if (compare == 0) {
                // if times are equal, check event type
                Integer me = new Integer(type);
                Integer other = new Integer(otherEvent.type);
                compare = me.compareTo(other);
                if (compare == 0) {
                    // if time and event type are equal, check pin name
                    return node.compareTo(otherEvent.node);
                }
                return compare;
            }
            return compare;
        }

        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append("Event @ ");
            buf.append(eventTimeNS+"ns: ");
            switch(type) {
                case TYPE_SETNODE: {
                    buf.append("set node "+node+" to "+state);
                    break;
                }
                case TYPE_CAPTURENODE: {
                    buf.append("capture node "+node);
                    break;
                }
                case TYPE_CAPTUREANDCHECKNODE: {
                    buf.append("capture node "+node+", check it is "+state);
                    break;
                }
            }
            return buf.toString();
        }

        public boolean equals(Object o) {
            if (eventTimeNS.equals(o))
                return true;
            return false;
        }
    }

    /** Unit Test */
    public static void main(String args[]) {
        NanosimModel model = new NanosimModel();
        BERT bert = new NanosimBERT(model);

        DataModule module = bert.createDataModule();
        module.setFrequency(1000000);

        DataGenerator gen = module.createDataGenerator();
        gen.setDelay(0.0);
        gen.addPinName("testpin");
        gen.addPinName("testpin2");
        gen.setPattern(new BitVector("1011101", ""), new BitVector("110100100", ""));

        DataAnalyzer ana = module.createDataAnalyzer();
        ana.setDelay(0, 0.1);
        ana.setPinName("testpinOut");
        ana.measureBER(gen);

        double time = 30;
        System.out.println("Events, time "+time+"ns:");
        printEvents(((NanosimBERT)bert).getAllEvents(0, time));

        gen.setDelay(0.5);
        ana.setDelay(1, 0.5);

        System.out.println("-----------------------------------");
        System.out.println("Events, time "+time+"ns: with delay 0.5 gen, 1.5 ana");
        printEvents(((NanosimBERT)bert).getAllEvents(0, time));
    }

    static void printEvents(Set set) {
        for (Iterator it = set.iterator(); it.hasNext(); ) {
            NanosimBERT.Event e = (NanosimBERT.Event)it.next();
            System.out.println(e);
        }
    }
}
