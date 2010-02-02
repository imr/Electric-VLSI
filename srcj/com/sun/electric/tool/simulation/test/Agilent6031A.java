/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Agilent6031A.java
 * Written by Tom O'Neill and Nathaniel Pinckney, Sun Microsystems.
 *
 * Copyright (c) 2008 Sun Microsystems and Static Free Software
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

/**
 * This class extends the generic equipment class to include methods specific to
 * the Agilent 6031A 20V/120A single-channel power supply.
 * <P>
 * Rather than using this class directly, you should use {@link Agilent6031AChannel}
 */
public class Agilent6031A extends Equipment {

    String s = new String("null");

    /** Resolution in Volts */
    private static final float VOLTAGE_RESOLUTION = 0.005f;
    public static final int FOLDBACK_OFF = 0;
    public static final int FOLDBACK_CV = 1;
    public static final int FOLDBACK_CC = 2;

    /** Creates a new instance of Agilent6031A */
    protected Agilent6031A(String newName) {
        super(newName);
    }

    /**
     * Returns string indicating state of one of the power supply
     *
     * @return String indicating power supply state
     */
    public String getState() {
        String result;
        result = "Set: " + getVoltageSetpoint() + " V, "
                + getCurrentSetpoint() + " A\n";
        result += "Got " + readVoltage() + " V, " + readCurrent()
                + " A";
        return result;
    }

    /**
     * Returns voltage resolution
     *
     * @return Voltage resoluton
     */
    public static float getVoltageResolution() {
        return VOLTAGE_RESOLUTION;
    }

    /**
     * Measures the voltage of the power supply
     * 
     */
    public float readVoltage() {
        write("VOUT?");
        String s[] = read(200).split("\\s+");
        return Float.parseFloat(s[1]);
    }

    /**
     * Reads back the voltage setpoint from the power supply
     * 
     */
    public float getVoltageSetpoint() {
        logOther("Reading voltage setpoint on Agilent 6031A");
        write("VSET?");
        String s[] = read(200).split("\\s+");
        return Float.parseFloat(s[1]);
    }

    /** Sets the voltage of the power supply */
    public void setVoltage(float voltage) {
        write("VSET " + voltage);
    }

    /** Reads back the current from the power supply */
    public float readCurrent() {
        write("IOUT?");
        String s[] = read(200).split("\\s+");
        return Float.parseFloat(s[1]);
    }

    /**
     * Reads back the current setpoint from the power supply
     * 
     */
    public float getCurrentSetpoint() {
        write("ISET?");
		String s[] = read(200).split("\\s+");
        return Float.parseFloat(s[1]);
    }

    /**
     * Changes the current setpoint to <code>setAmps</code> Amps.
     * 
     */
    public void setCurrent(float setAmps) {
          write("ISET " + setAmps);
    }
  
    /**
     * Gets the foldback mode or turns it off.
     *
     */
    public int getFoldback() {
        write("FOLD?");
		String s[] = read(200).split("\\s+");
        return Integer.parseInt(s[1]);
    }
  
    /**
     * Sets the foldback mode or turns it off.
     *
     * @param mode
     *      Foldback mode to set.  Can be <code>Agilent6031A.FOLDBACK_CV</code>,
     *   <code>FOLDBACK_CC</code>, or <code>FOLDBACK_OFF</code>.
     * @throws IllegalArgumentException
     *             if mode not in 0..2
     */
    public void setFoldback(int mode) {
	if (mode < 0 || mode > 2) {
            throw new IllegalArgumentException("Fallback " + mode
                    + " outside of allowed range 0..2");
        }
        write("FOLD " + mode);
    }

	public float getOverVoltageProtection() {
        write("OVP?");
		String s[] = read(200).split("\\s+");
        return Float.parseFloat(s[1]);
	}


    public static void main(String args[]) {
        Infrastructure.gpibControllers = new int[] { 3 };
        Agilent6031A supply = new Agilent6031A("A6031A");
	//        supply.setVoltage(3, 1.8f);
        //System.out.println("current " + supply.readCurrent(3));
         //System.out.println(supply.getState());
         supply.interactive();
    }//end main

}
