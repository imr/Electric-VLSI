/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NetscanJNI.java
 * Written by Eric Kim and Tom O'Neill, Sun Microsystems.
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

/**
 * JNI implementation of wrapper for Netscan C library. Netscan provides the
 * public interface for these methods.
 */
class NetscanJNI {

    // Load libtest.so, the library including the native C methods whose
    // signatures are given below
    static {
        System.loadLibrary("NetscanJNIe");
    }

    // native function declarations
    // refer to the Netscan documentation from vendor for detailed information
    public native static int net_connect(String destination);

    public native static int net_configure(long kHz, short stop_state, int mV);

    public native static int net_set_trst(int signal);

    public native static int net_disconnect();

    // public native static int net_tms_reset();

    // public native static int net_move_to_state(int state);

    /**
     * Write the bits <code>scan_in</code> to the JTAG controller's instruction
     * register. The first bit scanned in to the chip is the LSB of
     * <code>scan_in[0]</code>, and the first bit scanned out from the chip is
     * the LSB of <code>scan_out[0]</code>.
     * <p>
     * Note our naming convention for scan_in and scan_out is opposite to that
     * of the Netscan user's manual (i.e., we <code>scan_in</code> to chip,
     * they <code>scan_in</code> to software).
     * 
     * @param scan_in
     *            Bit sequence to write to instruction register
     * @param bit_length
     *            Number of bits to write to instruction register
     * @param scan_out
     *            Bits scanned out of instruction register
     * @return 0x00 (success), 0x11 (transmit error), 0x33 (receive error)
     */
    public native static int net_scan_ir(short[] scan_in, long bit_length,
            short[] scan_out);

    /**
     * Write the bits <code>scan_in</code> to the JTAG controller's data
     * register, and read back the bits <code>scan_out</code>. The first bit
     * scanned in to the chip is the LSB of <code>scan_in[0]</code>, and the
     * first bit scanned out from the chip is the LSB of <code>scan_out[0]</code>.
     * <p>
     * Note our naming convention for scan_in and scan_out is opposite to that
     * of the Netscan user's manual (i.e., we <code>scan_in</code> to chip,
     * they <code>scan_in</code> to software).
     * 
     * @param scan_in
     *            Bit sequence to write to data register
     * @param bit_length
     *            Number of bits to write to instruction register
     * @param scan_out
     *            Bits scanned out of data register
     * @return 0x00 (success), 0x11 (transmit error), 0x33 (receive error)
     */
    public native static int net_scan_dr(short[] scan_in, long bit_length,
            short[] scan_out);

    public native static int net_set_parallel_io(short port_data);
}
