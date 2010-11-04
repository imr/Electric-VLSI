/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TextUtils.java
 *
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.plugins.irsim;

import com.sun.electric.util.TextUtils;
import com.sun.electric.lib.LibFile;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.simulation.SimulationTool;
import java.net.URL;

/**
 * Collects Electric dependencies in IRSIM code.
 */
class Electric {

    // Technology, Schematics
    
    static double getLengthOff() {
        Technology layoutTech = Schematics.getDefaultSchematicTechnology();
        double lengthOff = Schematics.getDefaultSchematicTechnology().getGateLengthSubtraction() / layoutTech.getScale();
        return lengthOff;
    }
    
    // LibFile
    
	/**
	 * Method to find a library file.
	 * @param fileName the name of the file in the library area.
	 * These files are typically readable dumps of essential files used by everyone.
	 * @return the file path.
	 */
	static URL getLibFile(String fileName)
	{
        return LibFile.getLibFile(fileName);
	}
    
    // Job.getUserInterface()

    /**
     * Method to start the display of a progress dialog.
     * @param msg the message to show in the progress dialog.
     * @param filePath the file being read (null if not reading a file).
     */
    static void startProgressDialog(String msg, String filePath) {
        Job.getUserInterface().startProgressDialog(msg, filePath);
    }

    /**
     * Method to stop the progress bar
     */
    static void stopProgressDialog() {
        Job.getUserInterface().stopProgressDialog();
    }

    /**
     * Method to update the progress bar
     * @param pct the percentage done (from 0 to 100).
     */
    static void setProgressValue(int pct) {
        Job.getUserInterface().setProgressValue(pct);
    }
    
    /**
     * Method to show an error message.
     * @param message the error message to show.
     * @param title the title of a dialog with the error message.
     */
    static void showErrorMessage(String message, String title) {
        Job.getUserInterface().showErrorMessage(message, title);
    }
    
    // SimulationTool
    
	/**
	 * Get whether or not IRSIM uses a delayed X model, versus the old fast-propagating X model.
	 * @return true if using the delayed X model, false if using the old fast-propagating X model.
	 */
	static boolean isIRSIMDelayedX() {
        return SimulationTool.isIRSIMDelayedX(); }
    
    // GenMath
    
    /**
     * Class to define an Double-like object that can be modified.
     */
    static class MutableDouble
    {
        private double value;

        /**
         * Constructor creates a MutableDouble object with an initial value.
         * @param value the initial value.
         */
        public MutableDouble(double value) { this.value = value; }

        /**
         * Method to change the value of this MutableDouble.
         * @param value the new value.
         */
        public void setValue(double value) { this.value = value; }

        /**
         * Method to return the value of this MutableDouble.
         * @return the current value of this MutableDouble.
         */
        public double doubleValue() { return value; }

        /**
         * Returns a printable version of this MutableDouble.
         * @return a printable version of this MutableDouble.
         */
        public String toString() { return Double.toString(value); }
    }
    
    /**
     * Small epsilon value.
     * set so that 1+DBL_EPSILON != 1
     */	private static double DBL_EPSILON = 2.2204460492503131e-016;

    /**
     * Method to compare two double-precision numbers within an acceptable epsilon.
     * @param a the first number.
     * @param b the second number.
     * @return true if the numbers are equal to 16 decimal places.
     */
    static boolean doublesEqual(double a, double b)
    {
        return doublesEqual(a, b, DBL_EPSILON);
    }

    /**
     * Method to compare two double-precision numbers within a given epsilon
     * @param a the first number.
     * @param b the second number.
     * @param myEpsilon the given epsilon
     * @return true if the values are close.
     */
    static boolean doublesEqual(double a, double b, double myEpsilon)
    {
        return (Math.abs(a-b) <= myEpsilon);
    }

	/**
	 * Method to compare two numbers and see if one is less than the other within an acceptable epsilon.
	 * @param a the first number.
     * @param b the second number.
     * @return true if "a" is less than "b" to 16 decimal places.
	 */
    static boolean doublesLessThan(double a, double b)
	{
		if (a+DBL_EPSILON < b) return true;
		return false;
	}
    
    // TextUtils
    
    /**
     * Returns canonic char for ignore-case comparison .
     * This is the same as Character.toLowerCase(Character.toUpperCase(ch)).
     * @param ch given char.
     * @return canonic char for the given char.
     */
    static char canonicChar(char ch) {
        return TextUtils.canonicChar(ch); 
    }

    /**
     * Returns canonic string for ignore-case comparison .
     * FORALL String s1, s2: s1.equalsIgnoreCase(s2) == canonicString(s1).equals(canonicString(s2)
     * FORALL String s: canonicString(canonicString(s)).equals(canonicString(s))
     * @param s given String
     * @return canonic String
     * Simple "toLowerCase" is not sufficient.
     * For example ("\u0131").equalsIgnoreCase("i") , but Character.toLowerCase('\u0131') == '\u0131' .
     */
    static String canonicString(String s) {
        return TextUtils.canonicString(s);
    }
    
    /**
     * Method to parse the floating-point number in a string.
     * There is one reason to use this method instead of Double.parseDouble:
     * this method does not throw an exception if the number is invalid (or blank).
     * @param text the string with a number in it.
     * @return the numeric value.
     */
    static double atof(String text) {
        return TextUtils.atof(text);
    }
    
    /**
     * Method to parse the number in a string.
     * <P>
     * There are many reasons to use this method instead of Integer.parseInt...
     * <UL>
     * <LI>This method can handle any radix.
     *     If the number begins with "0", presume base 8.
     *     If the number begins with "0b", presume base 2.
     *     If the number begins with "0x", presume base 16.
     *     Otherwise presume base 10.
     * <LI>This method can handle numbers that affect the sign bit.
     *     If you give 0xFFFFFFFF to Integer.parseInt, you get a numberFormatPostFix exception.
     *     This method properly returns -1.
     * <LI>This method does not require that the entire string be part of the number.
     *     If there is extra text after the end, Integer.parseInt fails (for example "123xx").
     * <LI>This method does not throw an exception if the number is invalid (or blank).
     * </UL>
     * @param s the string with a number in it.
     * @return the numeric value.
     */
    public static int atoi(String s) {
        return TextUtils.atoi(s);
    }
    
    /**
     * Method to convert a double to a string.
     * If the double has no precision past the decimal, none will be shown.
     * @param v the double value to format.
     * @return the string representation of the number.
     */
    public static String formatDouble(double v) {
        return TextUtils.formatDouble(v);
    }
    
    /**
     * Method to convert a file path to a URL.
     * @param fileName the path to the file.
     * @return the URL to that file (null on error).
     */
    public static URL makeURLToFile(String fileName) {
        return TextUtils.makeURLToFile(fileName);
    }
}
