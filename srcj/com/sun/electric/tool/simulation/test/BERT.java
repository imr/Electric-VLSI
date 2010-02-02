/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BERT.java
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

import java.util.Comparator;
import java.util.Iterator;

/**
 * Interface for a HP81250 Bit Error Rate Tester.
 * A BERT is composed of several test modules. Each Module
 * is composed of one or more terminals.  A terminal drives a
 * single waveform (and it's complement for differential signals)
 * to the chip.
 * <P>
 * The BERT interface tries to hide the details from the user -
 * handles names, port locations, terminal locations, etc. Rather
 * than having to know what the address and port location of a Data Generator
 * is on the BERT, the user instead calls BERT.createDataGenerator().
 * This will return a DataGenerator for the user to use.
 * <P>
 * It is up to the implementation of the BERT to return a DataGenerator
 * and keep track of its address and port.  Or, to issue an error if
 * there are no (more) DataGenerators available on the BERT.
 */
public interface BERT {

    /**
     * Create a Data Module that is part of the BERT for use
     * by the user. May return null if not more data modules
     * can be created (i.e. are present in the real BERT).
     * @return a new DataModule
     */
    public DataModule createDataModule();

    /**
     * Run the BERT and its configured modules for the specified
     * time in nanoseconds. This is a replacement for doing
     * start, wait(nanoseconds), stop.
     * @param nanoseconds
     */
    public void run(double nanoseconds);

    /**
     * Same as {@link #run(double)}, but in minutes for
     * very long runs.
     * @param minutes number of minutes to run
     */
    public void runMinutes(double minutes);

    // ======================================================================

    /**
     * A Data Generator/Analyzer Module.
     */
    public interface DataModule {

        /**
         * Create a Data Generator from this module.
         * Returns null if not possible.  It is up
         * to the implementation of the Module to
         * know what is the number of data generators
         * available on this module.
         * @return a Data Generator.
         */
        public DataGenerator createDataGenerator();

        /**
         * Create a Data Analyzer from this module.
         * Returns null if not possible.  It is up
         * to the implementation of the Module to
         * know what is the number of data analyzers
         * available on this module.
         * @return a Data Analyzer.
         */
        public DataAnalyzer createDataAnalyzer();

        /**
         * This sets the frequency of the module,
         * which also dictates the frequency of all its terminals
         * @param freqKHz the frequency in Kilohertz
         */
        public void setFrequency(double freqKHz);

        /**
         * Get the frequency set for this module
         * @return the frequency in Kilohertz
         */
        public double getFrequency();

        /**
         * Return the frequency as a period, in nanoseconds.
         * @return the period in nanoseconds
         */
        public double getPeriod();
    }

    /**
     * A Terminal drives or reads a signal to or from a
     * chip.
     */
    public interface Terminal {

        /**
         * Get the parent Module that this Terminal is part of.
         * @return the parent Module
         */
        public DataModule getParentModule();

        /**
         * Enable or disable this Terminal
         * @param enable true to enable (should be default),
         * false to disable
         */
        public void setEnabled(boolean enable);

        /**
         * Get whether or not this Terminal is enabled.
         * @return true if it is enabled, false otherwise.
         */
        public boolean isEnabled();
    }

    /**
     * A Data Generator is a terminal that generates data
     */
    public interface DataGenerator extends Terminal {

        /**
         * Set the pattern that this generator will generate.
         * The frequency is set by the module this generator is in.
         * @param start the start of the pattern
         * @param repeat the pattern that will repeat after the start pattern
         */
        public void setPattern(BitVector start, BitVector repeat);

        /**
         * Set the pattern that this generator will generate.
         * The frequency is set by the module this generator is in.
         * @param start the start of the pattern
         * @param repeat the pattern that will repeat after the start pattern
         */
        public void setPattern(String start, String repeat);

        /**
         * Set the delay before the pattern starts. Defaults to zero.
         * @param ns the time delay, in nanoseconds
         */
        public void setDelay(double ns);

        /**
         * Set the way data is generated.
         * @param mode which mode to use.
         */
        public void setSignalMode(SignalMode mode);

        /**
         * Configure this terminal to generate a clock. This sets pattern
         * to be always 1, and mode to be Return to Zero. The clock
         * transitions high at time 0 and every period multiple thereafter.
         * You will still need to set the voltage and delay (if any).
         */
        public void setClock();

        /**
         * Configure this terminal to generate a DDR clock. This sets
         * the pattern to be alternating 1's and 0's, and mode to be
         * Non-return to zero. The clock transistions to high at time 0,
         * and then transistions to the opposite voltage every period
         * multiple thereafter.
         * You will still need to set the voltage and delay (if any).
         */ 
        public void setDDRClock();

        /**
         * Add the name of a pin this terminal connects to.
         * A terminal can connect to several pins by shorting them together.
         * @param pinName the pin name
         */
        public void addPinName(String pinName);

        /**
         * Many terminal outputs both True and Complement.
         * This adds the name of a the complement terminal connects to.
         * A terminal can connect to several pins by shorting them together.
         * @param pinName the pin name
         */
        public void addPinNameComplement(String pinName);

        /**
         * Get the names of pins connected to the true output
         * of this terminal (String iterator).
         * @return Iterator over pin names.
         */
        public Iterator getPinNames();

        /**
         * Get the names of pins connected to the complement output
         * of this terminal (String iterator).
         * @return Iterator over pin names in complement output.
         */
        public Iterator getPinNamesComplement();

        /**
         * Clear all pin assignments (both true and complement).
         */
        public void clearPins();

        /**
         * Set the voltage driven by Data Generator.  Note the complement
         * is only a complement value, so the voltage is never negative.
         * @param voltageLow the voltage for a logic low value
         * @param voltageHigh the voltage for a logic high value
         */
        public void setVoltage(double voltageLow, double voltageHigh);

        /**
         * Enable or disable complement signal
         * @param enable true to enable, false to disable
         */
        public void enableComplement(boolean enable);
    }

    /**
     * A Data Analyzer is a terminal that acquires/analyzes data.
     * It can be used in conjunction with a {@link DataGenerator}
     * to do bit error rate testing.
     */
    public interface DataAnalyzer extends Terminal {

        /**
         * Set the delay before starting to acquire data. The delay
         * is the sum of a number of periods (frequency set by the parent Module)
         * plus some number of absolute nanoseconds.
         * @param periods the number of periods
         * @param ns the number of nanoseconds
         */
        public void setDelay(int periods, double ns);

        /**
         * Measure the Bit Error Rate using expected data from the
         * given data generator. Any delay set on the generator is
         * added to the delay set on this Analyzer.
         * @param expectedData
         */
        public void measureBER(DataGenerator expectedData);

        /**
         * Get the Bit Error Rate measured the last time
         * the BERT was run.  If the analyzer was not
         * configured to measure the BER from a DataGenerator,
         * this returns zero.
         * @return the Bit Error Rate (num failed / num tested)
         */
        public double getMeasuredBER();

        /**
         * Set the name of the pin this terminal connects to.
         * @param pinName the pin name
         */
        public void setPinName(String pinName);

        /**
         * Get the name of the pin this terminal connects to.
         * @return the pin this analyzer is connected to
         */
        public String getPinName();

        /**
         * Set the voltage thresholds for distinguishing a logic low
         * and logic high value.
         * @param voltageLowThreshold voltages below this value will be considered logic low
         * @param voltageHighThreshold voltages above this value will be considered logic high
         */
        public void setVoltageThreshold(double voltageLowThreshold, double voltageHighThreshold);

        /**
         * Return a BitVector representing the captured data
         * @return the BitVector of the captured data.
         */
        public BitVector getCapturedData();
    }


    // ----------------------------------------------------------

    public static class StringComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            String s1 = (String)o1;
            String s2 = (String)o2;
            return s1.compareTo(s2);
        }
    }

    public static class SignalMode {
        private final String name;
        private SignalMode(String n) { name = n; }
        public String toString() { return name; }
    }
    /**
     * All data tends to be Non-return to zero. The signal does not
     * change until the next bit in the pattern.
     */
    public static final SignalMode NONRETURNTOZERO = new SignalMode("Non-Return to Zero");
    /**
     * Return to Zero signals return to zero halfway through the period.
     * A clock signal can be generated with a pattern of all 1's, if it
     * uses return to zero.
     */
    public static final SignalMode RETURNTOZERO = new SignalMode("Return to Zero");

}
