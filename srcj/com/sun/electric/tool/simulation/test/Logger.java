/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Logger.java
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

/**
 * Provides user-configurable diagnostic messages to device classes that extend
 * this class. Initialization messages can be turned on or off for all
 * <code>Logger</code> descendents. Once a device object is created, the user
 * can use {@link #setLogSets}&nbsp; and {@link #setLogOthers}&nbsp; to
 * control the object's individual logging characteristics. In this manner, the
 * user can enable messages only from particular devices.
 */
public abstract class Logger {

    /** Whether to report device initialization */
    private static boolean logInits = false;

    /** Whether to report set (configure) events */
    private boolean logSets = false;

    /** Whether to report events other than set events */
    private boolean logOthers = false;

    public String toString() {
        return "Logger: logSets=" + isLogSets() + ", logOthers="
                + isLogOthers();
    }

    /**
     * @return Returns whether to report all device initialization.
     */
    public static boolean isLogInits() {
        return logInits;
    }

    /**
     * Set whether to report initialization of all devices.
     * 
     * @param logInits
     *            whether to report device initialization
     */
    public static void setLogInits(boolean logInits) {
        Logger.logInits = logInits;
    }

    //    /**
    //     * Copy the logging state of this object to the destination
    //     *
    //     * @param source
    //     * <code>Logger</code> to copy state to
    //     */
    //    void copyFrom(Logger source) {
    //        logSets = source.logSets;
    //        logOthers = source.logOthers;
    //    }

    /**
     * @return Returns whether to report set (configure) events
     */
    public boolean isLogSets() {
        return logSets;
    }

    /**
     * Set whether to report set (configure) events, such as voltage setting
     * 
     * @param logSets
     *            whether to report set events
     */
    public void setLogSets(boolean logSets) {
        this.logSets = logSets;
    }

    /**
     * @return Returns whether to report occurences other than initialization
     *         and set events
     */
    public boolean isLogOthers() {
        return logOthers;
    }

    /**
     * Set whether to report occurences other than initialization and set
     * events.
     * 
     * @param logOthers
     *            whether to report occurences other than initialization and set
     *            events
     */
    public void setLogOthers(boolean logOthers) {
        this.logOthers = logOthers;
    }

    /**
     * Set all optional diagnostic messages on or off
     * 
     * @param log
     *            whether to print optional diagnostic messages
     */
    public void setAllLogging(boolean log) {
        setLogSets(log);
        setLogOthers(log);
    }

    /**
     * Display message <code>msg</code> on <code>stdout</code> when
     * initialization-logging is enabled.
     * 
     * @param msg
     *            message to print
     */
    protected static void logInit(String msg) {
        if (isLogInits() == true) {
            System.out.println(msg);
        }
    }

    /**
     * Display message <code>msg</code> on <code>stdout</code> when
     * set-logging is enabled.
     * 
     * @param msg
     *            message to print
     */
    protected void logSet(String msg) {
        if (isLogSets() == true) {
            System.out.println(msg);
        }
    }

    /**
     * Display message <code>msg</code> on <code>stdout</code> when "other"
     * logging is enabled.
     * 
     * @param msg
     *            message to print
     */
    protected void logOther(String msg) {
        if (isLogOthers() == true) {
            System.out.println(msg);
        }
    }
}
