/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Netscan4JNI.java
 * Written by Alex Chow and Tom O'Neill, Sun Microsystems.
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

/**
 * JNI implementation of wrapper for Netscan C library
 */
class Netscan4JNI {
    static {
        System.out.print("Loading NetUSB library... ");
        System.loadLibrary("Netscan4JNIe");
        System.out.println("Done.");
    }

    /** Suppress default constructor to make class non-instantiable */
    private Netscan4JNI() {
    }

    // native function declarations
    // refer to the Netscan documentation from vendor for detailed information
    public native static int netUSB_Connect(String destination);

    public native static int netUSB_hard_reset(long kHz, int mV);

    public native static int netUSB_set_scan_clk(long kHz);

    public native static int netUSB_set_trst(int signal);

    public native static int netUSB_Disconnect();

    public native static int netUSB_tms_reset(int tap);

    // public native static int net_move_to_state(int state, int tap);

    // convention for in/out is wrong in the book
    public native static int netUSB_scan_ir(short[] scan_in, long bit_length,
            short[] scan_out, int tap);

    public native static int netUSB_scan_dr(short[] scan_in, long bit_length,
            short[] scan_out, int tap);

    public native static int netUSB_AccessScanGPIO(int tap, int gpio, int mode,
            int value);
}
