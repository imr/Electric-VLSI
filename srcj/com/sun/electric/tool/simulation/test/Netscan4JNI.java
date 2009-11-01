package com.sun.electric.tool.simulation.test;

/* HeaterBringup.java
*
* Copyright (c) 2004 by Sun Microsystems, Inc.
*
* Created: September 21, 2004
*/

/**
 * JNI implementation of wrapper for Netscan C library
 * 
 * @author Alex Chow
 * @author Tom O'Neill (toneill)
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
