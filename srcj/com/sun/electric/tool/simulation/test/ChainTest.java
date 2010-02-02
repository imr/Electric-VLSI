/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ChainTest.java
 * Written by Tom O'Neill, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.Random;

/**
 * Class provides various methods for testing scan chains, including generation
 * of Schmoo plots and verification of scan chain lengths. Can also be used to
 * verify chain functionality or to measure or measure scan chain lengths.
 */
public class ChainTest {

    /**
     * Whether to assert the scan chain read line during chain testing. (Ignored
     * by <code>testOneChainShadow()</code> and
     * <code>schmooPlotShadow()</code>.)
     */
    public boolean readEnable = false;

    /**
     * Whether to assert the scan chain write line during chain testing.
     * (Ignored by {@link #testOneChainShadow}and {@link #schmooPlotShadow}.)
     */
    public boolean writeEnable = false;

    /**
     * Default TCK frequency increment for Schmoo plots, in kHz
     * 
     * @see #schmooPlot
     */
    public static final int DEFAULT_KHZ_STEP = 1000;

    /**
     * Number of times {@link #measureLength}&nbsp;should shift zeros into
     * chain to be sure it is full of zeros, when chain length is not known for
     * certain
     */
    public static final int LENGTH_MULTIPLIER = 100;

    static int seed = 1256; //random number seed

    static Random rand = new Random(seed);

    /** Index number of current Schmoo plot */
    static int count = 1;

    /** Object containing scan chain model/APIs */
    private ChainControl control;

    /** Number of iterations used in testing a scan chain */
    private int numTests = 5;

    /** Generic interface for setting Vdd */
    PowerChannel vddSupply;

    /** Voltage range for Schmoo plot */
    private int mvLow, mvHigh, mvStep = Infrastructure.DEFAULT_MV_STEP;

    /** Frequency range for Schmoo plot */
    private int khzLow, khzHigh, khzStep = DEFAULT_KHZ_STEP;

    /**
     * Create a scan chain tester for a particular experimental setup. Sets
     * initial Vdd and JTAG TCK frequency ranges for Schmoo plots to a small
     * range around the nominal values.
     * 
     * @see #schmooPlot
     * @param control
     *            Object providing scan chain programming
     * @param vddSupply
     *            Object providing control of chip Vdd
     */
    public ChainTest(ChainControl control, PowerChannel vddSupply) {
        this.control = control;
        this.vddSupply = vddSupply;

        mvLow = roundMillivolts(0.95f * 1000.f * control.getJtagVolts());
        mvHigh = roundMillivolts(1.05f * 1000.f * control.getJtagVolts());
        if (mvLow == mvHigh)
            mvLow = mvHigh - Infrastructure.DEFAULT_MV_STEP;

        khzLow = roundKHz(0.8f * control.getJtagKhz());
        khzHigh = control.getJtagKhz();
        if (khzLow == khzHigh)
            khzLow = khzHigh - DEFAULT_KHZ_STEP;
    }

    /**
     * Create a scan chain tester.     * 
     * @param control
     *            Object providing scan chain programming
     */
    public ChainTest(ChainControl control){
	this(control,null);
	this.vddSupply = new ManualPowerChannel("fake",false);
    }

    /**
     * Measure the number of scan chain elements in a root scan chain.
     * Optionally prints error message if length is different from claimed
     * length. Correct functioning requires that assumed length is within a
     * factor of LENGTH_MULTIPLIER of the true length.
     * 
     * @param chainRoot
     *            path to root scan chain, starting at the chip node
     * @param severity
     *            action when length differs from expected length
     * @return number of scan chain elements in chainRoot
     * @see Infrastructure#SEVERITY_NOMESSAGE
     * @see Infrastructure#SEVERITY_WARNING
     * @see Infrastructure#SEVERITY_NONFATAL
     * @see Infrastructure#SEVERITY_FATAL
     */
    public int measureLength(String chainRoot, int severity) {
        if (readEnable || writeEnable) {
            System.err.println("ChainTest.measureLength() warning: "
                    + "results may be incorrect when readEnable "
                    + "or writeEnable is true");
        }
        int expectedLength = control.getLength(chainRoot);
        BitVector inBits = new BitVector(expectedLength,
                "measureLength()-expectedLength");
        inBits.set(0, expectedLength, false);

        // Fill chain with zeros, assuming it is at most LENGTH_MULTIPLIER
        // times the actual length
        control.setInBits(chainRoot, inBits);
        for (int ind = 0; ind < LENGTH_MULTIPLIER; ind++) {
            control.shift(chainRoot, readEnable, writeEnable,
                    Infrastructure.SEVERITY_FATAL,
                    Infrastructure.SEVERITY_NOMESSAGE,
                    Infrastructure.SEVERITY_NOMESSAGE);
        }
        BitVector outBits = control.getOutBits(chainRoot);
        if (outBits.isEmpty() == false) {
            Infrastructure.fatal("Have shifted " + expectedLength
                    + " zeroes into chain " + chainRoot + " "
                    + LENGTH_MULTIPLIER
                    + " times in a row, and the final shift scanned out at "
                    + "least one non-zero bit.  The chain is broken or its"
                    + "true length is more than " + LENGTH_MULTIPLIER
                    + "times the claimed length of " + expectedLength);
        }

        // Single-shift in ones until we start seeing them
        inBits.set(expectedLength - 1);
        control.setInBits(chainRoot, inBits);
        int ind = 0;
        boolean bit = false;
        do {
            bit = control.shiftOneBit(chainRoot, readEnable, writeEnable,
                    Infrastructure.SEVERITY_FATAL);
            ind++;
        } while (bit == false);

        ind--;
        if (ind != expectedLength) {
            Infrastructure.error(severity, "Chain " + chainRoot
                    + " has claimed length of " + expectedLength
                    + ", but measured length is " + ind);
        }

        return ind;
    }

    /**
     * Compares lengths of all scan chains in the system to their claimed
     * values.
     * 
     * @param severity
     *            action when length differs from expected length
     * @return true if all chains had expected length
     * @see Infrastructure#SEVERITY_NOMESSAGE
     * @see Infrastructure#SEVERITY_WARNING
     * @see Infrastructure#SEVERITY_NONFATAL
     * @see Infrastructure#SEVERITY_FATAL
     */
    public boolean testLengths(int severity) {
        boolean status = true;
        String[] roots = control.getChainPaths();
        for (int iroot = 0; iroot < roots.length; iroot++) {
            System.out.print("Verifying length, chain " + iroot + ": "
                    + roots[iroot] + " ");
            int length = measureLength(roots[iroot], severity);
            if (length == control.getLength(roots[iroot])) {
                System.out.println("passed");
            } else if (severity == Infrastructure.SEVERITY_NOMESSAGE) {
                System.out.println("failed; change length to " + length);
                status = false;
            }
        }
        return status;
    }

    /**
     * Test a single root scan chain by shifting in and out up to
     * <code>numTests</code> random bit sequences. If incorrect bits are
     * shifted out or another shift error occurs, returns immediately.
     * 
     * @param chainRoot
     *            Path to root scan chain, starting at chip (e.g.,
     *            "miniHeater.eScan")
     * @param errTestSeverity
     *            action when consistency check fails
     * @return <tt>true</tt> if bits written, read were always the same
     * @see Infrastructure#SEVERITY_NOMESSAGE
     * @see Infrastructure#SEVERITY_WARNING
     * @see Infrastructure#SEVERITY_NONFATAL
     * @see Infrastructure#SEVERITY_FATAL
     */
    public boolean testOneChain(String chainRoot, int errTestSeverity) {
        int len = this.control.getLength(chainRoot);

        // Set bits to be written to a random sequence
        control.setInBits(chainRoot, getRandomBits(len));

        // Shift the random sequence into the scan chain. Ignore
        // any errors, because they might be due to a previous
        // shift at a bad voltage/frequency combination.
        control.shift(chainRoot, readEnable, writeEnable,
                Infrastructure.SEVERITY_NOMESSAGE,
                Infrastructure.SEVERITY_NOMESSAGE,
                Infrastructure.SEVERITY_NOMESSAGE);

        for (int iTest = 0; iTest < numTests; iTest++) {

            // Set bits to be written to a random sequence
            control.setInBits(chainRoot, getRandomBits(len));

            // Shift again, see if the bits scanned out are the same as those
            // scanned in during the previous shift. Fatal exception
            // if no bits can be compared (should be impossible).
            boolean result = control.shift(chainRoot, readEnable, writeEnable,
                    Infrastructure.SEVERITY_NOMESSAGE,
                    Infrastructure.SEVERITY_FATAL, errTestSeverity);
            if (result == false)
                return false;
        } //end for

        return true;
    }

    /**
     * Test a single root scan chain by writing and reading up to
     * <code>numTests</code> random bit sequences to/from the shadow registers
     * of a RWS (readable/writeable shadow register) scan chain. If an error is
     * found, returns immediately.
     * <p>
     * WARNING: Do not use on scan chains that can put the chip in undesirable
     * states, such as the Heater chip power scan chains. Also, incorrect
     * results may result when master clear is on.
     * <p>
     * Ignores state of <code>readEnable</code> and <code>writeEnable</code>
     * member variables.
     * 
     * @param chainRoot
     *            Path to root scan chain, starting at chip (e.g.,
     *            "miniHeater.eScan")
     * @param errTestSeverity
     *            action when consistency check fails
     * @return <tt>true</tt> if bits written, read were always the same
     * @see Infrastructure#SEVERITY_NOMESSAGE
     * @see Infrastructure#SEVERITY_WARNING
     * @see Infrastructure#SEVERITY_NONFATAL
     * @see Infrastructure#SEVERITY_FATAL
     */
    public boolean testOneChainShadow(String chainRoot, int errTestSeverity) {
        ChainNode chain = (ChainNode) control.findNode(chainRoot);
        if (chain.isReadable() == false || chain.isWriteable() == false
                || chain.usesShadow() == false) {
            Infrastructure.fatal("Chain " + chain
                    + " does not have RW shadow register, as required"
                    + " to use testOneChainShadow()");
        }
        int len = this.control.getLength(chainRoot);

        // Clear out any error conditions from previous shifts
        control.setInBits(chainRoot, getRandomBits(len));
        control.shift(chainRoot, false, false,
                Infrastructure.SEVERITY_NOMESSAGE,
                Infrastructure.SEVERITY_NOMESSAGE,
                Infrastructure.SEVERITY_NOMESSAGE);

        for (int iTest = 0; iTest < numTests; iTest++) {

            // Set bits to be written to a random sequence
            control.setInBits(chainRoot, getRandomBits(len));

            // Write the random sequence into the scan chain's shadow register
            control.shift(chainRoot, false, true,
                    Infrastructure.SEVERITY_NOMESSAGE,
                    Infrastructure.SEVERITY_NOMESSAGE,
                    Infrastructure.SEVERITY_NOMESSAGE);

            // This should force an error if the chain is not RWS
            control.setInBits(chainRoot, getRandomBits(len));

            // Read back from the shadow register, should get the same result
            if (control.shift(chainRoot, true, false,
                    Infrastructure.SEVERITY_NOMESSAGE,
                    Infrastructure.SEVERITY_FATAL, errTestSeverity) == false) {
                return false;
            }
        } //end for

        return true;
    }

    /**
     * Test all scan chains within a given chip by reading and writing random
     * sequences. Optionally prints a message telling whether each root scan
     * chain passes or fails.
     * 
     * @param chipName
     *            of the chip (e.g., "miniHeater")
     * @param errTestSeverity
     *            action when data register bit consistency check fails
     * @return Number of scan chains that failed
     * @see #testOneChain
     * @see Infrastructure#SEVERITY_NOMESSAGE
     * @see Infrastructure#SEVERITY_WARNING
     * @see Infrastructure#SEVERITY_NONFATAL
     * @see Infrastructure#SEVERITY_FATAL
     */
    public int testAllChains(String chipName, int errTestSeverity) {
        int numFail = 0;
        String[] roots = control.getChainPaths(chipName);
        for (int iroot = 0; iroot < roots.length; iroot++) {
            System.out.print("Trying dual shift to chain " + iroot + ": "
                    + roots[iroot] + "... ");
            if (testOneChain(roots[iroot], errTestSeverity)) {
                System.out.println(" passed.");
            } else {
                numFail++;
                System.out.println(" failed.");
            }
        }
        if (numFail > 0)
            System.err.println(numFail + " out of " + roots.length
                    + " chains failed on chip " + chipName);
        return numFail;
    } //end testAllChains

    /**
     * Test all scan chains in the system. Optionally prints a message telling
     * whether each root scan chain passes or fails.
     * 
     * @param errTestSeverity
     *            action when consistency check fails
     * @return True if all scan chains pass
     * @see #testOneChain
     * @see Infrastructure#SEVERITY_NOMESSAGE
     * @see Infrastructure#SEVERITY_WARNING
     * @see Infrastructure#SEVERITY_NONFATAL
     * @see Infrastructure#SEVERITY_FATAL
     */
    public int testAllChains(int errTestSeverity) {
        int numFail = 0;
        String[] chips = control.getChips();
        for (int ichip = 0; ichip < chips.length; ichip++) {
            numFail += testAllChains(chips[ichip], errTestSeverity);
        }
        if (numFail > 0)
            System.err.println(numFail + " chains failed in system!");
        return numFail;
    } //end testAllChains

    /**
     * Saves a Schmoo Plot to <code>failFile</code> and <code>passFile</code>.
     * Method {@link #testOneChain(String, int)}is used to determine if the
     * chain passes at each voltage and frequency specified.
     * <p>
     * Currently the JTAG tester is left at the default Vdd value, even though
     * the chip voltage is changed. This is required for correct JTAG tester
     * functioning, and does not noticeably pull up the chip voltage.
     *
     * @param chainRoot
     *            Path to root scan chain to test (e.g., "miniHeater.eScan")
     * @param failFile
     *            File containing voltage, frequency pairs that failed
     * @param passFile
     *            File containing voltage, frequency pairs that passed
     * @see #setKhzRange
     * @see #setVddRange
     */
    public void schmooPlot(String chainRoot, String failFile, String passFile) {
        schmooPlot(chainRoot, failFile, passFile, false, false);
    }

    /**
     * Saves a Schmoo Plot to <code>failFile</code> and <code>passFile</code>.
     * Method {@link #testOneChain(String, int)}is used to determine if the
     * chain passes at each voltage and frequency specified.
     * <p>
     * Currently the JTAG tester is left at the default Vdd value, even though
     * the chip voltage is changed. This is required for correct JTAG tester
     * functioning, and does not noticeably pull up the chip voltage.
     * 
     * @param chainRoot
     *            Path to root scan chain to test (e.g., "miniHeater.eScan")
     * @param failFile
     *            File containing voltage, frequency pairs that failed
     * @param passFile
     *            File containing voltage, frequency pairs that passed
     * @param pulseReset
     *            Reset jtag tester (using TRST) before each pass
     * @see #setKhzRange
     * @see #setVddRange
     */
    public void schmooPlot(String chainRoot, String failFile, String passFile, boolean pulseReset) {
        schmooPlot(chainRoot, failFile, passFile, false, pulseReset);
    }

    /**
     * Like {@link #schmooPlot}, but uses {@link #testOneChainShadow}instead
     * of {@link #testOneChain}. See warnings at {@link #testOneChainShadow}!
     * 
     * @param chainRoot
     *            Path to root scan chain to test (e.g., "miniHeater.eScan")
     * @param failFile
     *            File containing voltage, frequency pairs that failed
     * @param passFile
     *            File containing voltage, frequency pairs that passed
     * @param pulseReset
     *            Reset jtag tester (using TRST) before each pass
     * @see #setKhzRange
     * @see #setVddRange
     */
    public void schmooPlotShadow(String chainRoot, String failFile,
            String passFile, boolean pulseReset) {
        schmooPlot(chainRoot, failFile, passFile, true, pulseReset);
    }

    // Actual Schmoo plot implementation, using either testOneChain() or
    // testOneChainShadow() as specified by <code>shadow</code>
    private void schmooPlot(String chainRoot, String failFile, String passFile,
            boolean shadow, boolean pulseReset) {
        System.out.println("Generating Schmoo plot for chain " + chainRoot);
        try {
            PrintWriter fail = new PrintWriter(new FileWriter(failFile));
            fail.println("# voltage, frequency pairs that failed in Schmoo of "
                    + chainRoot);

            PrintWriter pass = new PrintWriter(new FileWriter(passFile));
            pass.println("# voltage, frequency pairs that passed in Schmoo of "
                    + chainRoot);

            for (int vdd_mV = mvLow; vdd_mV <= mvHigh; vdd_mV += mvStep) {
                float vdd = (float) vdd_mV / 1000;
                this.vddSupply.setVoltageWait(vdd);
                System.out.println("Setting Vdd = " + vdd + " V");
                if (pulseReset) {
                    System.out.println("Resetting Jtag Tester");
                    control.jtag.reset();
                }

                for (int kiloHerz = khzLow; kiloHerz <= khzHigh; kiloHerz += khzStep) {
                    control.jtag.configure(control.getJtagVolts(), kiloHerz);

                    boolean status;
                    if (shadow)
                        status = testOneChainShadow(chainRoot,
                                Infrastructure.SEVERITY_NOMESSAGE);
                    else
                        status = testOneChain(chainRoot,
                                Infrastructure.SEVERITY_NOMESSAGE);

                    if (status) {
                        pass.println(vdd_mV + " " + kiloHerz);
                    } else {
                        fail.println(vdd_mV + " " + kiloHerz);
                    }
                    System.out.println("freq " + kiloHerz + "kHz, voltage "
                            + vdd + " V");
                } //end inner for
            } //end outer for

            fail.close();
            pass.close();
        } //end try

        catch (Exception e) {
            System.err.println("exception occurred" + e);
        } //end catch

        // Return to sane levels
        //this.vddSupply.setVoltageWait(control.getJtagVolts());
        System.out.println("Please set vdd back to something correct");
        control.jtag.configure(control.getJtagVolts(), control.getJtagKhz());

        count++;
        System.out.println("finished testing " + chainRoot);

    } //end schmooPlot

    /**
     * Convienence method tests general scan chain functionality during bringup
     * of a chip or system. For each scan chain in the chip, verifies the length
     * and tests shifting data in and out. Then generate a Schmoo plot for the
     * longest scan chain in the system, using whatever Vdd and KHz ranges have
     * been set.
     * 
     * @param testLengths
     *            whether to test the lengths (can be slow)
     */
    public void bringup(boolean testLengths) {
        if (testLengths) {
            if (!testLengths(Infrastructure.SEVERITY_NONFATAL)) {
                Infrastructure.fatal("Fix lengths in xml file and run again");
            }
        }
        testAllChains(Infrastructure.SEVERITY_FATAL);

        // Find longest chain in system
        int maxLength = -1;
        String maxName = null;
        String[] chains = control.getChainPaths();
        for (int ichain = 0; ichain < chains.length; ichain++) {
            int length = control.getLength(chains[ichain]);
            if (length > maxLength) {
                maxLength = length;
                maxName = chains[ichain];
            }
        }

        // Now generate Schmoo plot for the longest chain
        schmooPlot(maxName, "fail." + maxName + ".dat", "pass." + maxName
                + ".dat", false);
    }

    /**
     * Returns highest frequency included in Schmoo plot
     * 
     * @return Highest frequency included in Schmoo plot, in kHz
     */
    public int getKhzHigh() {
        return khzHigh;
    }

    /**
     * Returns lowest frequency included in Schmoo plot
     * 
     * @return Lowest frequency included in Schmoo plot, in kHz
     */
    public int getKhzLow() {
        return khzLow;
    }

    /**
     * Returns frequency step in Schmoo plot
     * 
     * @return Frequency step in Schmoo plot, in kHz
     */
    public int getKhzStep() {
        return khzStep;
    }

    /**
     * Number of iterations used in testing a scan chain.
     * 
     * @return Number of iterations used in testing a scan chain
     * @see #testOneChain
     * @see #testAllChains
     */
    public int getNumTests() {
        return this.numTests;
    }

    /**
     * Returns high Vdd value in Schmoo plot, in Volts
     * 
     * @return High Vdd value in Schmoo plot, in Volts
     */
    public float getVddHigh() {
        return (this.mvHigh / 1000.f);
    }

    /**
     * Returns low Vdd value in Schmoo plot, in Volts
     * 
     * @return Low Vdd value in Schmoo plot, in Volts
     */
    public float getVddLow() {
        return (this.mvLow / 1000.f);
    }

    /**
     * Returns Vdd step size in Schmoo plot, in Volts
     * 
     * @return Vdd step size in Schmoo plot, in Volts
     */
    public float getVddStep() {
        return (this.mvStep / 1000.f);
    }

    /**
     * Set range of JTAG TCK frequencies covered by Schmoo plot
     * 
     * @param khzLow
     *            Low frequency in Schmoo plot, in kHz
     * @param khzHigh
     *            High frequency in Schmoo plot, in kHz
     * @param khzStep
     *            Frequency step size in Schmoo plot, in kHz
     */
    public void setKhzRange(int khzLow, int khzHigh, int khzStep) {
        this.khzLow = khzLow;
        this.khzHigh = khzHigh;
        this.khzStep = khzStep;
    }

    /**
     * Sets number of iterations used in testing a scan chain
     * 
     * @param numTests
     *            Number of iterations used in testing a scan chain
     * @see #testOneChain
     * @see #testAllChains
     */
    public void setNumTests(int numTests) {
        this.numTests = numTests;
    }

    /**
     * Set Vdd range for Schmoo plot (values only kept to nearest 0.001 V)
     * 
     * @param vddLow
     *            Low Vdd value in Schmoo plot, in Volts
     * @param vddHigh
     *            High Vdd value in Schmoo plot, in Volts
     * @param vddStep
     *            Vdd step size in Schmoo plot, in Volts
     */
    public void setVddRange(float vddLow, float vddHigh, float vddStep) {
        this.mvLow = Math.round(vddLow * 1000.f);
        this.mvHigh = Math.round(vddHigh * 1000.f);
        this.mvStep = Math.round(vddStep * 1000.f);
    }

    /**
     * Returns an bit vector with random true/false values
     * 
     * @param numBits
     *            Number of bits to return
     * @return random bit vector
     */
    public static BitVector getRandomBits(int numBits) {
        BitVector bits = new BitVector(numBits, "getRandomBits()-bits");
        for (int ind = 0; ind < numBits; ind++) {
            if (rand.nextBoolean()) {
                bits.set(ind);
            } else {
                bits.clear(ind);
            }
        }
        return bits;
    } //end getRandomBits

    /* Round mV value to nearest 100 mV */
    private static int roundMillivolts(float mV) {
        return (int) Math.round(mV / Infrastructure.DEFAULT_MV_STEP)
                * Infrastructure.DEFAULT_MV_STEP;
    }

    /* Round kHz value to nearest 1000 kHz */
    private static int roundKHz(float kHz) {
        return (int) Math.round(kHz / DEFAULT_KHZ_STEP) * DEFAULT_KHZ_STEP;
    }

} //end class
