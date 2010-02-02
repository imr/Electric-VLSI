/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GPIB.java
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
 * Driver for National Instruments NI-488.2M GPIB controller, includes the JNI
 * native method signatures and Java convenience wrapper methods. This class
 * provides low-level control, but the API methods are provided by the
 * friendlier {@link Equipment}&nbsp;class.
 * <p>
 * Native command documentation including error codes can be found in <a
 * href="http://www.ni.com/pdf/manuals/370963a.pdf"> the NI-488.2M Software
 * Reference Manual </a> and in <a href="../../../../../ugpib.h">
 * <tt>ugpib.h</tt> </a>.
 */
class GPIB {

    /**
     * The return values from the native GPIB routines will have bit 15 set if
     * an error has occured.
     */
    private static final int ERR_BIT = 0x8000;

    /** Whether catchError() is in progress */
    private static boolean errorPending = false;

    /** String identifying unused status bit or error code */
    public static final String UNUSED = "unused";

    /** Meaning of the various bits in the ibsta status word */
    public static final String[] STATUS_BITS = {
            "DCAS: device clear state (brd)",
            "DTAS: device trigger state (brd)", "LACS: Listener (brd)",
            "TACS: Talker (brd)", "ATN: Attention is asserted",
            "CIC: Controller-In-Charge", "REM: Remote state",
            "LOK: Lockout state", "CMPL: I/O completed", UNUSED, UNUSED,
            "RQS: Device requesting service", "SRQI: SRQ interrupt received",
            "END: END or EOS detected", "TIMO: Time limit exceeded",
            "ERR: GPIB error" };

    /** Meaning of the possible decimal values of the iberr error code */
    public static final String[] ERROR_CODES = {
            "EDVR: UNIX error (code in ibcnt, which we currently don't return)",
            "ECIC: Function requires GPIB board to be CIC",
            "ENOL: Write handshake error (e.g., no Listener)",
            "EADR: GPIB board not addressed correctly",
            "EARG: Invalid argument to function call",
            "ESAC: GPIB board not System Controller as required",
            "EABO: I/O operation aborted (timeout)",
            "ENEB: Non-existent GPIB board", "EDMA: DMA hardware problem",
            "EBTO: DMA hardware bus timeout", UNUSED,
            "ECAP: No capability for operation", "EFSO: File system error",
            UNUSED, "EBUS: GPIB bus error",
            "ESTB: Serial Poll status byte queue overflow",
            "ESRQ: SRQ stuck in ON position", UNUSED, UNUSED, UNUSED,
            "ETAB: Table Problem" };

    /** Suppress default constructor to make class non-instantiable */
    private GPIB() {
    }

    /*
     * helper utilities
     */

    /**
     * Open GPIB device with the given name and return its unit descriptor.
     * 
     * @param name
     *            Name of the device to find, from <tt>ibconf</tt>
     * @return unit descriptor of the device
     */
    public static int findDevice(String name) {
        name = name + '\0'; //add EOS to be safe
        int[] istat_ierr = new int[2];
        int ud = GPIB.ibfind(name.getBytes(), istat_ierr);
        String function = "ibfind(" + name + ") when initializing GPIB";
        catchError(function, ud, name, istat_ierr[0], istat_ierr[1]);
        if (ud < 0) {
            Infrastructure.fatal("Cannot find device " + name + " on GPIB.");
        }
        return ud;
    }

    /**
     * Write data to a GPIB device
     * 
     * @param ud
     *            unit descriptor of the device
     * @param name
     *            name of the device
     * @param data
     *            data to write
     */
    public static void ibWrite(int ud, String name, String data) {
        data = data + '\n';
        byte[] bytes = data.getBytes();
        int length = bytes.length;
        int[] ierr = new int[1];
        int status = GPIB.ibwrt(ud, bytes, length, ierr);
        String function = "ibwrt(ud=" + ud + ", data=" + data + ", length="
                + length + ") during write to";
        catchError(function, ud, name, status, ierr[0]);
    }

    /**
     * Read data from a GPIB device
     * 
     * @param ud
     *            unit descriptor of the device
     * @param name
     *            name of the device
     * @param length
     *            maximum number of bytes to read
     */
    public static String ibRead(int ud, String name, int length) {
        byte[] bytes = new byte[length];
        int[] ierr = new int[1];
        int status = GPIB.ibrd(ud, bytes, length, ierr);
        String output = new String(bytes);
        String function = "ibrd(ud=" + ud + ", bytes, length=" + length
                + ") during read from";
        catchError(function, ud, name, status, ierr[0]);
        return output;
    }

    /**
     * Return information about the GPIB software configuration parameters.
     * Valid <code>option</code> values can be found in the <tt>ibconf</tt>
     * and <tt>ibask</tt> constants section in <a
     * href="../../../../../ugpib.h"> <tt>ugpib.h</tt> </a>. Currently
     * {@link Equipment#CONTROLLER_ID_NUMBER}&nbsp;is provided for convenience
     * in specifying <code>option</code>.
     * 
     * @param ud
     *            unit descriptor of the device
     * @param name
     *            name of the device
     * @param option
     *            constant identifying which configuration parameter to return
     * @return value of the requested configuration parameter
     */
    public static int ibAsk(int ud, String name, int option) {
        int[] value = new int[1];
        int[] ierr = new int[1];
        int status = GPIB.ibask(ud, option, value, ierr);
        String function = "ibask(ud=" + ud + ", option=" + option
                + ", value) during query of";
        catchError(function, ud, name, status, ierr[0]);
        return value[0];
    }

    /**
     * Clear internal or device functions of the specified device.
     * 
     * @param ud
     *            unit descriptor identifying device to clear
     * @param name
     *            name of the device
     */
    public static void ibClr(int ud, String name) {
        int[] ierr = new int[1];
        int status = GPIB.ibclr(ud, ierr);
        String function = "ibclr(ud=" + ud + ") during clear of";
        catchError(function, ud, name, status, ierr[0]);
    }

    /**
     * Send GPIB interface messages to the device. The commands are listed in
     * Appendix A of <a href="../../../../../manuals/NI-488.2M_sw.pdf">NI-488.2M
     * Software Reference Manual </a>
     * 
     * @param ud
     *            unit descriptor identifying device to send command to
     * @param name
     *            name of the device
     * @param command
     *            string containing characters to send over GPIB
     */
    public static void ibCmd(int ud, String name, String command) {
        int[] ierr = new int[1];
        command = command + '\n';
        byte[] bytes = command.getBytes();
        int length = bytes.length;
        int status = ibcmd(ud, bytes, length, ierr);
        String function = "ibcmd(ud=" + ud + ", cmd=" + bytes + ", cnt="
                + length + ") while sending interface message to";
        catchError(function, ud, name, status, ierr[0]);
    }

    /**
     * Issues fatal error on error from a GPIB native command function.
     * 
     * @param function
     *            description of native GPIB function being checked
     * @param ud
     *            unit descriptor identifying device function was used on
     * @param name
     *            name of the device
     * @param status
     *            NI-488.2M <code>ibsta</code> status word
     * @param iberr
     *            NI-488.2M <code>iberr</code> status word for the command
     */
    private static void catchError(String function, int ud, String name,
            int status, int iberr) {
        if ((status & ERR_BIT) != 0) {
            Infrastructure.nonfatal("Bad return value ibsta=" + status + " (0x"

            + Integer.toHexString(status) + ") from native method\nGPIB."
                    + function + "\ndevice " + name + ".  Error is iberr="
                    + iberr + ".");
            reportStatus(status);
            reportError(iberr);
            System.err.println("Find descriptions of these error conditions\n"
                    + "in Chapter 3 of ${TEST_ROOT}/manuals/NI-488.2M_sw.pdf.");
            if (errorPending == false) {
                errorPending = true;
                System.err.println("\nNow querying device " + name
                        + " itself about the error...");
                ibWrite(ud, name, "SYST:ERR?");
                String error = ibRead(ud, name, 200);
                System.err.println("Device " + name + " reports error: "
                        + error);
            }

            /*
             * Try to leave the device in a good state. To prevent infinite
             * loop, don't call this.ibClr()
             */
            int[] ierr = new int[1];
            status = GPIB.ibclr(ud, ierr);
            System.exit(0);
        }
    }

    private static void reportStatus(int status) {
        System.err
                .println("The following bits are set in the ibsta status word:");
        for (int bit = 0; bit < STATUS_BITS.length; bit++) {
            int value = (1 << bit);
            if ((status & value) != 0) {
                System.err.println("  Bit " + bit + " (0x"
                        + Integer.toHexString(value) + ") means '"
                        + STATUS_BITS[bit] + "'");
                if (STATUS_BITS[bit].equals(UNUSED)) {
                    System.err
                            .println("*** This status bit should not occur ***");
                }
            }
        }
    }

    private static void reportError(int iberr) {
        if (iberr < 0 || iberr >= ERROR_CODES.length
                || ERROR_CODES[iberr].equals(UNUSED)) {
            System.err.println("Unknown error code iberr=" + iberr);
        } else {
            System.err.println("The error code iberr=" + iberr + " means '"
                    + ERROR_CODES[iberr] + "'");
        }
    }

    private static void printCodes() {
        for (int bit = 0; bit < STATUS_BITS.length; bit++) {
            System.out.println(bit + " (0x" + Integer.toHexString(1 << bit)
                    + "): " + STATUS_BITS[bit]);
        }
        for (int err = 0; err < ERROR_CODES.length; err++) {
            System.out.println(err + ": " + ERROR_CODES[err]);
        }
    }

    /*----------------------------------------------------------------------
     * GPIB JNI interface. usually helper utility are preferred than these low
     * level native call byte in java = 8bits char in c = 8bits
     * --------------------------------------------------------------------
     */

    // Load the native C library
    static {
        System.loadLibrary("teste");
    }

    public native static int ibwrt(int ud, byte[] data, int length, int[] ierr);

    public native static int ibrd(int ud, byte[] data, int length, int[] ierr);

    /**
     * Open device and return the unit descriptor associated with the given
     * name. Note <code>istat_ierr[0]</code> is the NI-488M <code>ibsta</code>
     * status word and <code>istat_ierr[1]</code> is the <code>iberr</code>
     * error code.
     * 
     * @param name
     *            device name from <tt>ibconf</tt>
     * @param istat_ierr
     *            <code>ibsta</code> and <code>iberr</code> status words in
     *            array
     * @return number used by NI-488M native functions to identify the device
     */
    public native static int ibfind(byte[] name, int[] istat_ierr);

    /**
     * Return information about the GPIB software configuration parameters.
     * 
     * @param ud
     *            unit descriptor identifying device to clear
     * @param ierr
     *            NI-488.2M <code>iberr</code> status word
     * @return NI-488.2M <code>ibsta</code> status word
     */
    public native static int ibask(int ud, int option, int[] value, int[] ierr);

    /**
     * Clear specified device.
     * 
     * @param ud
     *            unit descriptor identifying device to clear
     * @param ierr
     *            NI-488.2M <code>iberr</code> status word
     * @return NI-488.2M <code>ibsta</code> status word
     */
    public native static int ibclr(int ud, int[] ierr);

    /**
     * Send GPIB interface messages to the device. The commands are listed in
     * Appendix A of <a href="../../../../../manuals/NI-488.2M_sw.pdf">NI-488.2M
     * Software Reference Manual </a>
     * 
     * @param ud
     *            unit descriptor identifying device to send command to
     * @param cmd
     *            characters to be sent over GPIB
     * @param cnt
     *            requested transfer count (maximum bytes to send)
     * @param ierr
     *            NI-488.2M <code>iberr</code> status word
     * @return NI-488.2M <code>ibsta</code> status word
     */
    public native static int ibcmd(int ud, byte[] cmd, long cnt, int[] ierr);

    public static void main(String[] args) {
        printCodes();

        int ista;
        do {
            String string = Infrastructure.readln("Enter ista in hex:");
            ista = Integer.parseInt(string, 16);
            reportStatus(ista);
        } while (ista != -999);
    }
}
