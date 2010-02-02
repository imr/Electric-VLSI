/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: HP80000.java
 * Written by David Hopkins, Sun Microsystems.
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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

public class HP80000 extends Equipment {

    public static int SYSTEM=0, SYSTEMCLOCK=1, DATAGEN1GHZ=2, DG1GHZ128K=3;

    private int selectedInst;

    /**
     * Creates a new instance of HP80000
     * @param name  
     */
    public HP80000(String name) {
        super(name);
        selectInstrument(HP80000.SYSTEM);
    }

    public void reset() {
        write("*CLS");
        write("*RST");
        try { Thread.sleep(300); } catch (InterruptedException _) { }
    }
    
    public void start() {
        write(":INST:SEL SYSTEMCLOCK");
        write(":TRIG:STAR");
        try { Thread.sleep(100); } catch (InterruptedException _) { }
    }
    
    public void stop() {
        write(":INST:SEL SYSTEMCLOCK");
        write(":TRIG:STOP");
        try { Thread.sleep(100); } catch (InterruptedException _) { }
    }
    
    public void pause() {
        write(":INST:SEL SYSTEMCLOCK");
        write(":TRIG:PAUS");
        try { Thread.sleep(100); } catch (InterruptedException _) { }
    }
    
    public void cont() {
        write(":INST:SEL SYSTEMCLOCK");
        write(":TRIG:CONT");
        try { Thread.sleep(100); } catch (InterruptedException _) { }
    }
    
    

    /**
     * Select an instrument. Most commands must be run after selecting
     * the proper instrument.
     * @param inst the instrument
     */
    public void selectInstrument(int inst) {

        write(":INST:SEL "+instToString(inst));
        selectedInst = inst;
    }

    private String instToString(int inst) {
        switch(inst) {
            case 0: return "System";
            case 1: return "SystemClock";
            case 2: return "DATAGEN1GHZ";
            case 3: return "DG1GHZ128K";
        }
        return "Unknown";
    }

    // ------------------------- System Clock -------------------------

    /**
     * Set the clock source to internal or external.
     * @param internal true for internal, false for external
     */
    public void setClockSource(boolean internal) {
        if (!isSelectedInstSystemClock()) return;
        if (internal)
            write(":CLOC:SOUR INT");
        else
            write(":CLOC:SOUR EXT");
    }

    /**
     * Set the internal system clock frequency. Default value is 1GHz.
     * The frequency will be set to the nearest available frequency setting.
     * The available range is 7.8125 MHz to 1 GHz.
     * @param freq the frequency
     * @param units the units: "M" or "KHz" or "MHz".
     */
    public void setClockFrequency(double freq, String units) {
        if (!isSelectedInstSystemClock()) return;
        if (!units.toLowerCase().endsWith("hz")) units = units + "HZ";
        write(":CLOC:INT:FREQ "+freq+" "+units);
    }


    // ----------------------- Managing Groups --------------------------

    public void printGroups() {
        if (!isSelectedInstDataGen()) return;
        write(":DIG:GRO:CAT?");
        String s = read(200);
        System.out.println("Groups: "+s);
    }

    /**
     * Define a group and assign channels to the group. The instrument must
     * be a DATAGEN1GHZ or DG1GHZ128K; the group name is whatever you choose,
     * and the slotsAndChannels argument sets which channels on which slots (modules)
     * are assigned to this group.
     * <P>
     * For example, the following strings all assign channels 0 to 3 on slot 0
     * and slot 2 to this group:
     * <pre>
     * Individual Logical Channels:
     * 0,1,2,3,8,9,10,11
     * Logical Channel Ranges:
     * 0:3,8:11
     * Module slot and channel:
     * 0(0:3),2(0:3)
     * </pre>
     *
     * @param groupName a new group name
     * @param slotsAndChannels the slots and channels
     */
    public void defineGroup(String groupName, String slotsAndChannels) {
        if (!isSelectedInstDataGen()) return;
        write(":DIG:GRO:DEF "+groupName+",(@"+slotsAndChannels+")");
    }

    /**
     * Delete the group on the instrument. Note that the group "ALL" will delete all groups
     * @param groupName which group to delete
     */
    public void deleteGroup(String groupName) {
        if (!isSelectedInstDataGen()) return;
        if (groupName.equals("ALL"))
            write(":DIG:GRO:DEL:ALL");
        else
            write(":DIG:GRO:DEL "+groupName);
    }

    /**
     * Select given group for currently selected instrument
     * (currently selected instrument must be DATAGEN1GHZ or DG1GHZ128K)
     * Note that the group "ALL" will select ALL groups. Also,
     * the group "NONE" will select no groups, and all subsequent group-oriented
     * commands will be disabled.
     * @param groupName which group
     */
    public void selectGroup(String groupName) {
        if (!isSelectedInstDataGen()) return;
        write(":DIG:GRO:SEL "+groupName);
    }

    // ------------------- Group Level Commands for Data Gen ----------------

    /**
     * Set the pattern cycling for the currently selected group. Note that
     * this does not set the actual pattern itself, only the number of bits
     * and cycling of the pattern. Note that start+length must be les than 16384.
     * @param startLength number of start bits at start, generated once, must be 0 or 4 or more
     * @param cycleLength number of repeated bits, must be 0 or multiple of 8
     * @param cycleTimes number of times to loop, 0 to 255, or -1 for infinite looping
     */
    public void setGroupPatternCycle(int startLength, int cycleLength, int cycleTimes) {
        if (!isSelectedInstDataGen()) return;

        String l = cycleTimes < 0 ? "INF" : String.valueOf(cycleTimes);
        if (startLength < 4 && startLength != 0) {
            System.out.println("Error: pattern cycle start value must be 0, or 4 or more");
            return;
        }
        if (cycleLength % 8 != 0) {
            System.out.println("Error: pattern cycle length value must be 0 or multiple of 8");
            return;
        }
        if (cycleTimes > 255) {
            System.out.println("Warning: pattern cycle loop value maximum is 255, dropping "+cycleTimes+" to 255");
            cycleTimes = 255;
        }
        write(":PATT:CYCL "+startLength+","+cycleLength+","+l);
        try { Thread.sleep(100); } catch (InterruptedException _) { }
    }

    /**
     * Set the data for the current group. The array size must equal the number of
     * channels in the group. The length of the binData Strings must equal the number
     * of bits in the sequence. The string format is binary, for example, "010010110".
     * @param binData the binary data sequences for each channel.
     */
    public void setGroupData(String [] binData) {
        if (!isSelectedInstDataGen()) return;
        if (binData.length == 0) return;

        int numChannels = binData.length;
        int datalen = binData[0].length();

        for (int i=0; i<binData.length; i++) {
            if (binData[i].length() != datalen) {
                System.out.println("Error: in setData, all arrays must be the same length");
                return;
            }
        }
        for (int i=0; i<datalen; i++) {
            StringBuffer cmd = new StringBuffer();
            cmd.append(":PATT:MOD ");
            cmd.append(i);
            cmd.append(",#B");
            for (int j=numChannels-1; j >= 0; j--) {
                cmd.append(binData[j].charAt(i));
            }
            write(cmd.toString());
            try { Thread.sleep(100); } catch (InterruptedException _) { }
            //System.out.println("wrote: "+cmd.toString());
        }
        write(":PATT:UPD");
        try { Thread.sleep(100); } catch (InterruptedException _) { }
    }

    /**
     * Print data for signal in a group. Unfortunately this only seems to
     * work if there is only one signal in a group
     * @param start
     * @param end
     * @param numSignals
     */
    public void printGroupData(int start, int end, int numSignals) {
        if (!isSelectedInstDataGen()) return;

        write(":FORM ASC,"+numSignals);
        write(":PATT:MOD? "+start+","+end);
        String s = read(400);
        System.out.println(s);
/*
        String vectors [] = s.split(",");
        for (int i=0; i<numSignals; i++) {
            System.out.print("Signal"+i+": ");
            int mask = 1 << i;
            for (int j=0; j<vectors.length; j++) {
                if ((Integer.valueOf(vectors[j]) & mask) == 0)
                    System.out.print("0");
                else
                    System.out.print("1");
            }
            System.out.println();
        }
*/
    }

    /**
     * Set the data for the given channel
     * @param signal which signal in the group
     * @param dataPat string of 1's and 0's of length "length" specified by setPatternCycle
     * @deprecated can't get it to work
     */
    private void setPatternData(int signal, String dataPat) {
        if (!isSelectedInstDataGen()) return;
        write(":FORM PACK,1");
        write(":PATT:DATA"+signal+" "+dataPat);
        write(":PATT:UPD");
    }

    /**
     * Print the data for the given channel
     * @param signal which signal in the group
     * @param start start
     * @param end end
     * @deprecated useless
     */
    private void printPatternData(int signal, int start, int end) {
        if (!isSelectedInstDataGen()) return;
        write(":FORM HEX,8");
        //write(":PATT:DATA"+signal+"?");
        //write(":PATT:DATA?");
        write(":PATT:DATA"+signal+"? "+start+","+end);
        try { Thread.sleep(2000); } catch (InterruptedException _) { }
        String s = read(600);
        System.out.println("Data for signal "+signal+" is: (len="+s.length()+")");
        System.out.println(s);
    }

    /**
     * Set the signal format for the currently selected Group
     * Note that there is only one signal polarity setting for a module, so other
     * groups on the same module will also be affected.
     * @param NonReturntoZero true to set to non-return-to-zero, false to set return-to-zero
     */
    public void setSignalFormat(boolean NonReturntoZero) {
        if (!isSelectedInstDataGen()) return;
        if (NonReturntoZero)
            write(":SIGN:FORM NRZ");
        else
            write(":SIGN:FORM RZ");
    }

    /**
     * Set the signal polarity (normal or inverted) for all signals
     * in the currently selected Group.
     * @param normal true for normal, false for inverted
     */
    public void setGroupSignalPolarity(boolean normal) {
        setGroupSignalPolarity(-1, normal);
    }

    /**
     * Set the signal polarity (normal or inverted) for a signal
     * in the currently selected Group.
     * @param signal which signal in the group, or -1 for all signals
     * @param normal true for normal, false for inverted
     */
    public void setGroupSignalPolarity(int signal, boolean normal) {
        if (!isSelectedInstDataGen()) return;
        String s = signal < 0 ? "" : String.valueOf(signal);
        if (normal)
            write(":SIGN:POL"+s+" NORM");
        else
            write(":SIGN:POL"+s+" INV");
    }

    /**
     * Turn on or off the output state
     * (whether output is enabled) for all signals in the currently selected Group.
     * @param on true to enable, false to disable
     */
    public void setOutputState(boolean on) {
        setOutputState(-1, on);
    }

    /**
     * Turn on or off the output state
     * (whether output is enabled) for the signal in the currently selected Group.
     * Or, set signal to -1 to set the state for all signals in the group.
     * @param signal -1 for all signals, or which signal in the currently selected group.
     * @param on true to enable, false to disable
     */
    public void setOutputState(int signal, boolean on) {
        if (!isSelectedInstDataGen()) return;
        String s = signal < 0 ? "" : String.valueOf(signal);
        if (on)
            write(":OUTP"+s+" ON");
        else
            write(":OUTP"+s+" OFF");
    }

    /**
     * Set the output termination for all signals in the currently selected group
     * The state can be one of the following:
     * <pre>
     * 0: 50 ohms terminated to ground (GROUND)
     * 1: 50 ohms terminated to -2V (ECL)
     * 2: 50 ohms terminated to +3V (PECL)
     * 3: open circuit (OPEN)
     * </pre>
     * Note that the output state must be switched off to change this setting
     * @param state which termination to set
     */
    public void setOutputTermination(int state) {
        setOutputTermination(-1, state);
    }

    /**
     * Set the output termination for the signal in the currently selected group,
     * or all signals in the group if signal is -1.
     * The state can be one of the following:
     * <pre>
     * 0: 50 ohms terminated to ground (GROUND)
     * 1: 50 ohms terminated to -2V (ECL)
     * 2: 50 ohms terminated to +3V (PECL)
     * 3: open circuit (OPEN)
     * </pre>
     * Note that the output state must be switched off to change this setting
     * @param signal which signal in the group, or -1 for all signals in the group
     * @param state which termination to set
     */
    public void setOutputTermination(int signal, int state) {
        if (!isSelectedInstDataGen()) return;
        String s = signal < 0 ? "" : String.valueOf(signal);
        switch (state) {
            case 0: write(":OUTP"+s+":TERM GRO"); break;
            case 1: write(":OUTP"+s+":TERM ECL"); break;
            case 2: write(":OUTP"+s+":TERM PECL"); break;
            case 3: write(":OUTP"+s+":TERM OPEN"); break;
            default:
                System.out.println("State "+state+" is not a valid output termination state");
        }
    }

    /**
     * Set the delay of a signal in the currently selected group.
     * The valid range is -2000 to +2000 ps
     * @param ps how many picoseconds to delay the data relative to the clock
     */
    public void setGroupDelay(int ps) {
        setGroupDelay(-1, ps);
    }

    /**
     * Set the delay of a signal in the currently selected group.
     * The valid range is -2000 to +2000 ps. Reset value is 0.
     * @param signal which signal in the currently selected group, -1 for all signals
     * @param ps how many picoseconds to delay the data relative to the clock
     */
    public void setGroupDelay(int signal, int ps) {
        if (!isSelectedInstDataGen()) return;
        String s = signal < 0 ? "" : String.valueOf(signal);
        if (ps < -2000 || ps > 2000) {
            System.out.println("Valid range for group delay is -2000ps to 2000ps: not "+ps);
            return;
        }
        write(":SOUR:PULS:DEL"+s+" "+ps+" PS");
    }

    /**
     * Set the high voltage level for all signals in the currently selected group
     * Reset value is +1V. Range depends on termination and Low value setting.
     * The difference between high and low is constrained such that:
     * <pre>
     * Term    Min and Max Magnitude of Voltage Swing
     * Ground  0.5V <= (High - Low) <= 2.5V
     * ECL     0.5V <= (High - Low) <= 2.5V
     * PECL    0.5V <= (High - Low) <= 2.5V
     * OPEN    1.0V <= (High - Low) <= 5.0V
     * </pre>
     * Absolute limits are:
     * <pre>
     * Term    Limits
     * Ground  -1.5V to 3.0V
     * ECL     -1.5V to 1.0V
     * PECL     0V   to 4.5V
     * OPEN    -2.5V to 4.5V
     * </pre>
     * Note that the output state must be switched off to change this setting
     * @param voltage the voltage for a high value
     */
    public void setGroupHighVoltageLevel(double voltage) {
        setGroupHighVoltageLevel(-1, voltage);
    }

    /**
     * Set the high voltage level for a signal in the currently selected group
     * Reset value is +1V. Range depends on termination and Low value setting.
     * The difference between high and low is constrained such that:
     * <pre>
     * Term    Min and Max Magnitude of Voltage Swing
     * Ground  0.5V <= (High - Low) <= 2.5V
     * ECL     0.5V <= (High - Low) <= 2.5V
     * PECL    0.5V <= (High - Low) <= 2.5V
     * OPEN    1.0V <= (High - Low) <= 5.0V
     * </pre>
     * Absolute limits are:
     * <pre>
     * Term    Limits
     * Ground  -1.5V to 3.0V
     * ECL     -1.5V to 1.0V
     * PECL     0V   to 4.5V
     * OPEN    -2.5V to 4.5V
     * </pre>
     * Note that the output state must be switched off to change this setting
     * @param signal which signal in the currently selected group, -1 for all signals
     * @param voltage the voltage for a high value
     */
    public void setGroupHighVoltageLevel(int signal, double voltage) {
        if (!isSelectedInstDataGen()) return;
        String s = signal < 0 ? "" : String.valueOf(signal);
        write(":SOUR:VOLT:LEV:IMM:HIGH"+s+" "+voltage+" V");
    }

    /**
     * Set the low voltage level for all signals in the currently selected group
     * Reset value is -1V. Range depends on termination and High value setting.
     * The difference between high and low is constrained such that:
     * <pre>
     * Term    Min and Max Magnitude of Voltage Swing
     * Ground  0.5V <= (High - Low) <= 2.5V
     * ECL     0.5V <= (High - Low) <= 2.5V
     * PECL    0.5V <= (High - Low) <= 2.5V
     * OPEN    1.0V <= (High - Low) <= 5.0V
     * </pre>
     * Absolute limits are:
     * <pre>
     * Term    Limits
     * Ground  -1.5V to 3.0V
     * ECL     -1.5V to 1.0V
     * PECL     0V   to 4.5V
     * OPEN    -2.5V to 4.5V
     * </pre>
     * Note that the output state must be switched off to change this setting
     * @param voltage the voltage for a high value
     */
    public void setGroupLowVoltageLevel(double voltage) {
        setGroupLowVoltageLevel(-1, voltage);
    }

    /**
     * Set the low voltage level for a signal in the currently selected group
     * Reset value is -1V. Range depends on termination and High value setting.
     * The difference between high and low is constrained such that:
     * <pre>
     * Term    Min and Max Magnitude of Voltage Swing
     * Ground  0.5V <= (High - Low) <= 2.5V
     * ECL     0.5V <= (High - Low) <= 2.5V
     * PECL    0.5V <= (High - Low) <= 2.5V
     * OPEN    1.0V <= (High - Low) <= 5.0V
     * </pre>
     * Absolute limits are:
     * <pre>
     * Term    Limits
     * Ground  -1.5V to 3.0V
     * ECL     -1.5V to 1.0V
     * PECL     0V   to 4.5V
     * OPEN    -2.5V to 4.5V
     * </pre>
     * Note that the output state must be switched off to change this setting
     * @param signal which signal in the currently selected group, -1 for all signals
     * @param voltage the voltage for a high value
     */
    public void setGroupLowVoltageLevel(int signal, double voltage) {
        if (!isSelectedInstDataGen()) return;
        String s = signal < 0 ? "" : String.valueOf(signal);
        write(":SOUR:VOLT:LEV:IMM:LOW"+s+" "+voltage+" V");
    }

    //------------------------------------------------------------------------------

    private void printResponse() {
        String s = read(200);
        System.out.println(s);
    }

    private boolean isSelectedInstSystem() {
        if (selectedInst != HP80000.SYSTEM) {
            System.out.println("Error, issuing SYSTEM related command when currently " +
                    "selected instrument is not SYSTEM");
            return false;
        }
        return true;
    }

    private boolean isSelectedInstSystemClock() {
        if (selectedInst != HP80000.SYSTEMCLOCK) {
            System.out.println("Error, issuing SYSTEMCLOCK related command when currently " +
                    "selected instrument is not SYSTEMCLOCK");
            return false;
        }
        return true;
    }

    private boolean isSelectedInstDataGen() {
        if (selectedInst != HP80000.DATAGEN1GHZ && selectedInst != HP80000.DG1GHZ128K) {
            System.out.println("Error, issuing DATAGEN1GHZ/DG1GHZ128K related command when currently " +
                    "selected instrument is not DATAGEN1GHZ/DG1GHZ128K");
            return false;
        }
        return true;
    }

    private void printErrorQueue() {
        write(":SYST:ERR?");
        String s = read(200);
        System.out.println(s);
    }

    // ------------------------------- Main ---------------------------------

    public static void main(String args[]) {
        // which gpib controller (ethernet-to-gpib box) the HP80000 is connected to
        Infrastructure.gpibControllers = new int[]{2};
        // create instrument based on assigned name in ibconf program on electron
        HP80000 datagen = new HP80000("HP80000");

        datagen.reset();
        datagen.selectInstrument(HP80000.DG1GHZ128K);
        datagen.deleteGroup("ALL");
        datagen.defineGroup("foo", "0(2:3)");
        datagen.printGroups();
        datagen.selectGroup("foo");
        datagen.setGroupPatternCycle(4, 8, 1);
        String [] data = new String [] { "000100110111",
                                         "100011001000" };
        datagen.printGroupData(0, 12, 2);
        datagen.setGroupData(data);
		datagen.deleteGroup("ALL");
		datagen.defineGroup("clk", "0(2)");
        datagen.printGroups();
        datagen.selectGroup("clk");
        datagen.setGroupPatternCycle(0, 16, -1);
		datagen.setGroupData(new String [] { "0000111111110000" });
        datagen.printGroupData(0, 15, 1);
        System.out.println("done");
    }
    
}
