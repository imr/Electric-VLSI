/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SamplerControl.java
 * Written by Tom O'Neill, Sun Microsystems.
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

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

/**
 * Main API for sampler programming and calibration. The code assumes that a
 * node controls a sampler if and only if it satisfies the following conditions:
 * <ul>
 * <li>It is an instance of <code>SubchainNode</code> (e.g, it is a
 * <code>ChainNode</code>)</li>
 * <li>The node includes a <tt>pin</tt> attribute</li>
 * <li>The node contains one each of <tt>calibrate</tt>,<tt>enable</tt>,
 * and <tt>enable_f</tt>
 * <code>Subchain</code> nodes. (Other nodes are
 * allowed as well.)</li>
 * <li>The <tt>calibrate</tt>,<tt>enable</tt>, and <tt>enable_f</tt>
 * nodes each contain a single scan chain element.
 * </ul>
 * If the pin attribute cannot be set in the XML file, it may be set using
 * <code>ChainControl.setSubchainPin()</code>.
 */
public class SamplerControl extends Logger {

    /** Object containing scan chain model/APIs */
    ChainControl control;

    /**
     * Specifies whether chain control elements must be set <tt>HI</tt> to
     * enable the corresponding on-chip signal.
     */
    private final int polarity;

    /** Mapping from scan chain paths to pin names */
    public final Map map;

    /**
     * For each sampler control node, the value of the scan chain element
     * required to enable the control setting
     */
    private final String[] controlEnable = new String[CONTROL_NODES.length];

    /**
     * For each sampler control node, the value of the scan chain element
     * required to disable the control setting
     */
    private final String[] controlDisable = new String[CONTROL_NODES.length];

    /** Name of calibrate sampler control node */
    public final static String[] CONTROL_NODES = { "calibrate", "enable",
            "enable_f" };

    /** index of calibrate control node */
    public final static int IND_CALIBRATE = 0;

    public final static int IND_ENABLE = 1;

    public final static int IND_ENABLE_F = 2;

    /**
     * <code>polarity</code> value when calibrate, enable, enable_f scan chain
     * elements are set <tt>HI</tt> to set these signals true on chip.
     */
    public final static int POLARITY_NORMAL = 0;

    /**
     * <code>polarity</code> value when calibrate, enable, enable_f scan chain
     * elements are set <tt>LO</tt> to set these signals true on chip.
     */
    public final static int POLARITY_INVERTED = 1;

    /**
     * Scan chain value required to enable a sampler control signal, depends on
     * value of polarity.
     */
    private final static String[] CONTROL_ENABLE = { "1", "0" };

    /**
     * Scan chain value required to disable a sampler control signal, depends on
     * value of polarity.
     */
    private final static String[] CONTROL_DISABLE = { "0", "1" };

    /**
     * Constructor. Identifies samplers in <code>control</code> using the
     * conditions described at top. For each sampler, sets <code>inBits</code>
     * necessary to disable it. Does not shift any data, since the rest of the
     * chip state may not be specified.
     * <p>
     * Currently <code>polarity</code> value may be
     * <code>POLARITY_NORMAL</code> or <code>POLARITY_INVERTED</code>.
     * 
     * @param control
     *            Object containing scan chain model/APIs
     * @param polarity
     *            whether sampler control signals come from non-inverting scan
     *            chain output
     */
    public SamplerControl(ChainControl control, int polarity) {
        super();
        this.control = control;
        if (polarity != POLARITY_NORMAL && polarity != POLARITY_INVERTED) {
            Infrastructure.fatal("Bad polarity value " + polarity
                    + ", only POLARITY_NORMAL and POLARITY_INVERTED are "
                    + " supported");
        }
        this.polarity = polarity;
        this.map = new java.util.HashMap();

        // Fill controlEnable array according to polarity
        java.util.Arrays.fill(controlEnable, CONTROL_ENABLE[polarity]);
        java.util.Arrays.fill(controlDisable, CONTROL_DISABLE[polarity]);

        // Get all nodes in the experimental system
        String[] paths = control.getDescendents("");
        for (int ind = 0; ind < paths.length; ind++) {
            if (isSampler(paths[ind])) {
                map.put(paths[ind], control.getSubchainPin(paths[ind]));
                preclear(paths[ind]);
            }
        }
    }

    /**
     * Prints some information about the sampler object, including which
     * samplers are on each pin.
     */
    public String toString() {
        StringBuffer buffy = new StringBuffer("SamplerControl, control="
                + control);
        buffy.append("\n  Samplers on each pin:");

        // Convert Collection of values to TreeSet for unique + ordered
        java.util.Set pins = new java.util.TreeSet(map.values());

        // For each pin, add which samplers are on it
        for (java.util.Iterator iter = pins.iterator(); iter.hasNext();) {
            String pin = (String) iter.next();
            buffy.append("\n    " + pin + ": ");
            String[] samplers = getSamplersOnPin(pin);
            for (int ind = 0; ind < samplers.length; ind++) {
                buffy.append(" " + samplers[ind]);
            }
        }
        buffy.append("\n");
        return buffy.toString();
    }

    /**
     * Checks if path to sampler scan chain node has correct format and
     * identifies an actual sampler.
     * 
     * @param path
     *            Path to the scan chain node for the sampler
     */
    void checkPath(String path) {
        if (map.containsKey(path) == false) {
            Infrastructure.fatal("Path " + path + " is not a recognized "
                    + "sampler" + "\nSee SamplerControl javadoc for how to "
                    + "identify sampler nodes");
        }
    }

    /**
     * Disable the sampler at the specified <code>SubchainNode</code> by
     * setting the <tt>calibrate</tt>,<tt>enable</tt>, and
     * <tt>enable_f</tt> scan chain elements according to the sampler
     * polarity.
     * 
     * @param path
     *            Path to the scan chain node for the sampler
     */
    public void clear(String path) {
        preclear(path);
        logSet("SamplerControl.clear(): clearing " + path);
        control.shift(control.getParentChain(path), false, true);
    }

    /**
     * Set the <code>inBits</code> values to disable the sampler at the
     * specified <code>SubchainNode</code>, but do not shift the data into
     * the chip. The <tt>calibrate</tt>,<tt>enable</tt>, and
     * <tt>enable_f</tt> scan chain elements are set according to the sampler
     * polarity.
     * 
     * @param path
     *            Path to the subchain node for the sampler
     */
    private void preclear(String path) {
        checkPath(path);

        for (int ind = 0; ind < CONTROL_NODES.length; ind++) {
            control.setInBits(path + "." + CONTROL_NODES[ind],
                    controlDisable[ind]);
        }
    }

    /**
     * Enable the sampler at the specified <code>SubchainNode</code> by
     * setting the <tt>enable</tt> and <tt>enable_f</tt> scan chain elements
     * according to the sampler polarity. Disables the <tt>calibrate</tt> scan
     * chain element.
     * <p>
     * If <code>enable</code> or <code>enable_f</code> is <tt>true</tt>,
     * all samplers on the same output pin are disabled before setting the
     * requested sampler. This is to prevent interference with the measurement.
     * 
     * @param path
     *            Path to the scan chain node for the sampler
     * @param enable
     *            Whether to enable the standard version of the sampler
     * @param enable_f
     *            Whether to enable the source-follower version of the sampler
     */
    public void setEnables(String path, boolean enable, boolean enable_f) {
        checkPath(path);

        if (enable || enable_f) {
            clearSiblings(path);
        }

        setOne(path, false, enable, enable_f);
    }

    /**
     * Enable or disable calibration for the sampler at the specified
     * <code>SubchainNode</code> by setting the <tt>calibrate</tt> scan
     * chain element according to the sampler polarity.
     * 
     * @param path
     *            Path to the scan chain node for the sampler
     * @param calibrate
     *            Whether to configure the sampler for calibration
     */
    public void setCalibrate(String path, boolean calibrate) {
        checkPath(path);

        setControl(path, IND_CALIBRATE, calibrate);
        logSet("SamplerControl.setCalibrate(): setting " + path
                + " to calibrate");
        control.shift(control.getParentChain(path), false, true);
    }

    /**
     * Clears all samplers on same pin as specified sampler.
     * 
     * @param path
     *            Path to the scan chain node for the sampler
     */
    private void clearSiblings(String path) {
        String targetPin = (String) map.get(path);
        String[] paths = getSamplersOnPin(targetPin);
        for (int ind = 0; ind < paths.length; ind++) {
            clear(paths[ind]);
        }
    }

    /**
     * Calibrates the specified sampler, writing the current versus voltage to
     * the provided file. Assumes that <code>setEnables()</code> has been used
     * to set at least one of <code>enable</code> and <code>enable_f</code>.
     * 
     * @param fileName
     *            File to write the calibration data to
     * @param path
     *            Path to the scan chain node for the sampler
     * @param ivspec
     *            Object specifying the IV curve to measure
     * @throws IOException
     */
    public void calibrate(String fileName, String path, AmpsVsVolts ivspec)
            throws IOException {
        PrintWriter file = new PrintWriter(new FileWriter(fileName));
        file.println("# calibration (voltage, current) for sampler at path "
                + path);
        setCalibrate(path, true);
        ivspec.measure(file);
        setCalibrate(path, false);
        file.close();
    }

    /**
     * Returns <tt>true</tt> if the node at the specified path controls a
     * sampler. See comments at top for the necessary conditions.
     * 
     * @param path
     *            path name to potential sampler control
     * @return whether specified node controls a sampler
     */
    private boolean isSampler(String path) {
        TestNode node = (TestNode) control.findNode(path);
        if (node.getChildCount() < 3 || (node instanceof SubchainNode == false))
            return false;

        int[] numInstances = getNumInstances(node);

        boolean oneOfEach = true, anyPresent = false;
        for (int ind = 0; ind < CONTROL_NODES.length; ind++) {
            if (numInstances[ind] != 1)
                oneOfEach = false;
            if (numInstances[ind] > 0)
                anyPresent = true;
        }

        if (oneOfEach) {
            if (control.getSubchainPin(path).length() <= 0) {
                System.err.println("*** SamplerControl warning: node " + path
                        + "\nappears to be a sampler, but does not have"
                        + " a valid pin name.  Please set the "
                        + "\n'pin' attribute in the XML file or use"
                        + " ChainControl.setSubchainPin()");
                return false;
            }

            return true;
        }

        if (anyPresent) {
            System.err.println("*** SamplerControl warning: node " + path
                    + "\nmay be a sampler that does not contain "
                    + "required nodes."
                    + "\nSamplers must contain exactly one each of '"
                    + CONTROL_NODES[0] + "', '" + CONTROL_NODES[1] + "', and '"
                    + CONTROL_NODES[2] + "'");
            return false;
        }

        return false;
    }

    /**
     * Return path strings of all samplers on specified pin
     * 
     * @param targetPin
     *            name of sampler current output pin
     * @return path strings of all samplers on <code>targetPin</code>
     */
    public String[] getSamplersOnPin(String targetPin) {
        java.util.List list = new java.util.ArrayList();
        for (java.util.Iterator iter = map.keySet().iterator(); iter.hasNext();) {
            String path = (String) iter.next();
            String pin = (String) map.get(path);
            if (pin.equals(targetPin)) {
                list.add(path);
            }
        }
        String[] paths = new String[list.size()];
        for (int ind = 0; ind < paths.length; ind++) {
            paths[ind] = (String) list.get(ind);
        }
        return paths;
    }

    /**
     * Enable or disable the specified sampler control
     * 
     * @param path
     *            Path to sampler
     * @param ind
     *            Index of control in CONTROL_NODES array
     * @param enable
     *            Whether to enable the control
     */
    private void setControl(String path, int ind, boolean enable) {
        if (enable) {
            control.setInBits(path + "." + CONTROL_NODES[ind],
                    controlEnable[ind]);
        } else {
            control.setInBits(path + "." + CONTROL_NODES[ind],
                    controlDisable[ind]);
        }
    }

    /**
     * Enable the sampler at the specified <code>SubchainNode</code> by
     * setting the <tt>calibrate</tt>,<tt>enable</tt>, and
     * <tt>enable_f</tt> scan chain elements according to the sampler
     * polarity. Does not affect any other samplers.
     * 
     * @param path
     *            Path to the scan chain node for the sampler
     * @param calibrate
     *            Whether to configure the sampler for calibration
     * @param enable
     *            Whether to enable the standard version of the sampler
     * @param enable_f
     *            Whether to enable the source-follower version of the sampler
     */
    private void setOne(String path, boolean calibrate, boolean enable,
            boolean enable_f) {
        checkPath(path);
        if (calibrate && !enable && !enable_f) {
            System.out.println("WARNING: Odd setting for sampler at " + path
                    + ": calibrate true, but enable and enable_f false.");
        }

        setControl(path, IND_CALIBRATE, calibrate);
        setControl(path, IND_ENABLE, enable);
        setControl(path, IND_ENABLE_F, enable_f);

        logSet("SamplerControl.setOne(): setting " + path);
        control.shift(control.getParentChain(path), false, true);
    }

    /**
     * Count number of children that are named after each of the required nodes
     * 
     * @param node
     * @return number of children that have each of the required names
     */
    private int[] getNumInstances(TestNode node) {
        int numInstances[] = new int[CONTROL_NODES.length];
        for (int kidIndex = 0; kidIndex < node.getChildCount(); kidIndex++) {
            TestNode kid = (TestNode) node.getChildAt(kidIndex);
            String kidName = kid.getName();
            for (int ind = 0; ind < CONTROL_NODES.length; ind++) {
                if (kidName.equals(CONTROL_NODES[ind])) {
                    String kidPath = kid.getPathString(1);
                    int length = control.getLength(kidPath);
                    if (length == 1) {
                        numInstances[ind] += 1;
                    } else {
                        System.err.println("*** SamplerControl warning: node "
                                + kidPath + " has length " + length
                                + ", should have length 1");
                    }
                }
            }
        }
        return numInstances;
    }

    /** Unit test */
    public static void main(String[] args) {
        ChainControl control = new ChainControl("heater.xml");

        control.setSubchainPin("heater.SW_expC.transmit.sample_cT", "frog");
        control.setSubchainPin("heater.NW_expC.transmit.sample_cT", "frog");
        control.setSubchainPin("heater.SE_expC.transmit.sample_cT", "toad");
        control.setSubchainPin("heater.NE_expC.transmit.sample_cT", "bar");

        SamplerControl samplers = new SamplerControl(control, POLARITY_INVERTED);
    }

}
