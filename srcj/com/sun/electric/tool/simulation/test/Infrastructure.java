/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Infrastructure.java
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

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Convenience methods for Async test software, including error-printing
 * routines, a timed wait facility, and methods to obtain <code>String</code>
 * input from the user. Static, non-instantiable class.
 */
public class Infrastructure {

    /**
     * Constant indicating program continues with no error message when it
     * encounters a particular flavor of error in shift()
     */
    public final static int SEVERITY_NOMESSAGE = 0;

    /**
     * Constant indicating program prints error message and continues when it
     * encounters a particular flavor of error in shift()
     */
    public final static int SEVERITY_WARNING = 1;

    /**
     * Constant indicating program prints error message and stack trace, then
     * continues, when it encounters a particular flavor of error in shift()
     */
    public final static int SEVERITY_NONFATAL = 2;

    /**
     * Constant indicating program prints error message and exits when it
     * encounters a particular flavor of error in shift()
     */
    public final static int SEVERITY_FATAL = 3;

    /**
     * ID of GPIB controllers used in this setup, to prevent accidental control
     * of other peoples' devices. These are the integer board numbers from
     * <tt>gpibconf</tt>, which are one less than the GPIB network names.
     */
    public static int[] gpibControllers;

    /** Whether to output verbose diagnostics about scan chain operations */
    private static boolean log = false;

    /**
     * Default Vdd increment for Schmoo plots or other voltage sweeps, in mV
     * 
     * @see ChainTest#schmooPlot
     * @see HP548xxA#frequencyVsVoltage
     */
    public static final int DEFAULT_MV_STEP = 100;


    private static long timeMarker = 0;

    /** Suppress default constructor to make class non-instantiable */
    private Infrastructure() {
    }

    /**
     * Sane version of System.in allowing readLine() method.
     */
    public static BufferedReader console = new BufferedReader(
            new InputStreamReader(System.in));

    /**
     * Prints message and stack trace on standard error.
     * 
     * @param message
     *            message to print
     */
    public static void nonfatal(String message) {
        Exception err = new Exception(message);
        err.printStackTrace();
    }

    /**
     * Prints message and stack trace on standard error, then exits.
     * 
     * @param message
     *            message to print
     */
    public static void fatal(String message) {
        Exception err = new Exception(message);
        err.printStackTrace();
        Infrastructure.exit(1);
    }

    public static void exit(int exitValue) {
        SimulationModel.finishAll();
        System.exit(exitValue);
    }

    /**
     * Depending on severity, may print error message, print stack trace, and/or
     * exit the JVM.
     * 
     * @param severity
     *            one of the SEVERITY_* constants defining action to take
     * @param message
     *            message to print
     * @see #SEVERITY_NOMESSAGE
     * @see #SEVERITY_WARNING
     * @see #SEVERITY_NONFATAL
     * @see #SEVERITY_FATAL
     */
    public static void error(int severity, String message) {
        if (severity == Infrastructure.SEVERITY_NOMESSAGE) {
            return;
        } else if (severity == Infrastructure.SEVERITY_WARNING) {
            System.err.println(message);
        } else if (severity == Infrastructure.SEVERITY_NONFATAL) {
            Exception err = new Exception(message);
            err.printStackTrace();
        } else if (severity == Infrastructure.SEVERITY_FATAL) {
            Exception err = new Exception(message);
            err.printStackTrace();
            Infrastructure.exit(1);
        } else {
            Infrastructure.nonfatal(message);
            Infrastructure.fatal("Bad severity " + severity);
        }
    }

    /**
     * Display message <code>msg</code> on <code>stdout</code> when
     * <code>log</code> is <tt>true</tt>
     * 
     * @param msg
     *            message to print
     * @deprecated
     */
    public static void log(String msg) {
        if (log == true) {
            System.out.println(msg);
        }
    }

    /**
     * Returns next line from System.in
     * 
     * @return next line from System.in
     */
    public static String readln() {
        try {
            return console.readLine();
        } catch (Exception e) {
            System.err.println(e);
            Infrastructure.exit(1);
        }
        return null;
    }

    /**
     * Prompts for input and returns next line from System.in
     * 
     * @return next line from System.in
     */
    public static String readln(String prompt) {
        System.out.print(prompt);
        try {
            return console.readLine();
        } catch (Exception e) {
            System.err.println(e);
            Infrastructure.exit(1);
        }
        return null;
    }

    public static void markTime() {
        timeMarker = System.currentTimeMillis();
    }

    /**
     * Prints the time
     */
    public static String getTimeSinceMarker() {
        return getElapsedTime(System.currentTimeMillis()-timeMarker);
    }

    /**
	 * Method to describe a time value as a String.
	 * @param milliseconds the time span in milli-seconds.
	 * @return a String describing the time span with the
	 * format: days : hours : minutes : seconds
	 */
	public static String getElapsedTime(long milliseconds)
	{
		if (milliseconds < 60000)
		{
			// less than a minute: show fractions of a second
			return (milliseconds / 1000.0) + " secs";
		}
		StringBuffer buf = new StringBuffer();
		int seconds = (int)milliseconds/1000;
		if (seconds < 0) seconds = 0;
		int days = seconds/86400;
		if (days > 0) buf.append(days + " days, ");
		seconds = seconds - (days*86400);
		int hours = seconds/3600;
		if (hours > 0) buf.append(hours + " hrs, ");
		seconds = seconds - (hours*3600);
		int minutes = seconds/60;
		if (minutes > 0) buf.append(minutes + " mins, ");
		seconds = seconds - (minutes*60);
		buf.append(seconds + " secs");
		return buf.toString();
	}

    /**
     *
     * Pause for the specified number of seconds
     * 
     * @param seconds number of seconds to wait
     * @deprecated Replaced by ChipModel.wait()
     */
    public static void waitSeconds(float seconds) {
        wait(seconds);
    }

    protected static void wait(float seconds) {
        float milliSec = seconds * 1000;

        try {
            Thread.sleep((int) milliSec);
        } catch (InterruptedException ignore) {
        }
    }

    public static void main(String[] args) {
        fatal("Here is a crash message");
    }
}
