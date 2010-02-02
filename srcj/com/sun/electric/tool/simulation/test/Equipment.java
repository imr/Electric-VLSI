/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Equipment.java
 * Written by Eric Kim and Tom O'Neill, Sun Microsystems.
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
 * Device-independent control of a piece of experimental equipment over GPIB.
 * Device-dependent APIs are provided by extending this class for each equipment
 * model.
 * <p>
 * Native command documentation including error codes can be found in the <a
 * href="../../../../../manuals/NI-488.2M_sw.pdf">NI-488.2M Software Reference
 * Manual </a> (also found <a href="http://www.ni.com/pdf/manuals/370963a.pdf">
 * here </a>) and in <a href="../../../../../ugpib.h"> <tt>ugpib.h</tt> </a>.
 */

public class Equipment extends Logger implements EquipmentInterface {

    /**
     * <code>option</code> value for {@link #ask}&nbsp that returns the ID
     * number of the GPIB controller that this device is connected to.
     */
    public final static int CONTROLLER_ID_NUMBER = 0x200;

    /** Unit descriptor for device */
    private int ud = -1;

    /** the name of the device, as it appears in <tt>ibconf</tt>. */
    private String name;

    /**
     * Default constructor, should be invoked by any class that extends
     * {@link Equipment}. Finds the device on GPIB and clears its internal
     * state (cf. {@link #clear}).
     * 
     * @param name
     *            Name of the device, as given in
     *            /proj/async/cad/linux/src/gpib/nienet/ibconf
     */
    public Equipment(String name) {
        this.name = name;
        if (isDisabled()) {
            Logger.logInit("  GPIB device " + name + " not enabled for this test.");
            return;
        }
        if (Infrastructure.gpibControllers == null) {
            Infrastructure.fatal("You must set Infrastructure.gpibControllers "
                    + "before initializing any GPIB devices");
        }
        
        ud = GPIB.findDevice(name);
        Logger.logInit("  Initializing GPIB device: " + name + ", ud: " + ud);

        int board = this.ask(CONTROLLER_ID_NUMBER);
        for (int ind = 0; ind < Infrastructure.gpibControllers.length; ind++) {
            if (board == Infrastructure.gpibControllers[ind]) {
                //clear();
                return;
            }
        }
        Infrastructure.fatal("Device " + name + " is on controller number "
                + board + ", which is not in Infrastructure.gpibControllers");
    }

    public String toString() {
        return name;
    }

    /**
     * Returns the name of the device, as it appears in <tt>ibconf</tt>.
     * 
     * @return Returns the name of the device.
     */
    public String getName() {
        return name;
    }

    /**
     * send write message to receiver
     * 
     * @param data
     *            data string
     */
    public void write(String data) {
        if (isDisabled()) { return; }
        if (ud < 0) {
            System.out.println("can't write to uninitized device:" + this);
        }
        GPIB.ibWrite(ud, name, data);
    }

    /**
     * Receive up to <code>length</code> bytes from the device.
     * 
     * @param length
     *            length of the data to read
     * @return data string which is read
     */
    public String read(int length) {
        if (isDisabled()) { return ""; }
        if (ud < 0) {
            System.out.println("can't read from uninitized device:" + this);
        }
        String result = GPIB.ibRead(ud, name, length).trim();
        //System.out.println(id + ": " + result + ". " + result.length());
        if (result.length() == 0) {
            Infrastructure.nonfatal("Empty string from id " + ud);
        }
        return result.trim();
    }

    /**
     * Read 80 characters from device.
     * 
     * @return data string which is read
     */
    public String readLine(){
	return read(80);
    }

    public float readFloat(int length) {
        if (isDisabled()) { return 0f; }
        String s = read(length);
        return Float.parseFloat(s);
    }

    /**
     * Return information about the GPIB software configuration parameters.
     * Valid <code>option</code> values can be found in the <tt>ibconfig</tt>
     * and <tt>ibask</tt> constants section in <a
     * href="../../../../../ugpib.h"> <tt>ugpib.h</tt> </a>. Currently
     * {@link #CONTROLLER_ID_NUMBER}&nbsp;is provided for convenience in
     * specifying <code>option</code>.
     * 
     * @param option
     *            constant identifying which configuration parameter to return
     * @return value of the requested configuration parameter
     */
    public int ask(int option) {
        if (isDisabled()) { return 0; }
        return GPIB.ibAsk(ud, name, option);
    }

    /**
     * Clear internal or device functions of the device. Among other things,
     * this should clear device GPIB error conditions and allow its use again.
     * For some devices, it appears that the clear does not work if it is too
     * soon after the error occurs.
     */
    public void clear() {
        if (isDisabled()) { return; }
        GPIB.ibClr(ud, name);
    }

    /**
     * Send GPIB interface messages to the device. The commands are listed in
     * Appendix A of <a href="../../../../../manuals/NI-488.2M_sw.pdf">NI-488.2M
     * Software Reference Manual </a>
     * 
     * @param command
     *            string containing characters to send over GPIB
     */
    public void command(String command) {
        if (isDisabled()) { return; }
        GPIB.ibCmd(ud, name, command);
    }

    /**
     * For device bringup and command testing, takes commands from the terminal
     * and sends them to the device. If a command includes a question mark, then
     * it waits for the reply from the device before prompting for the next
     * command to send.
     */
    public void interactive() {
        String command, response;
        do {
            command = Infrastructure.readln("Enter command: ");
            write(command);
            if (command.indexOf("?") > 0) {
                response = read(100);
                if (response.length() > 0) {
                    System.out.println("response: " + response);
                }
            }
        } while (true);
    }

    /**
     * Whether or not this piece of equipment is disabled.
     * Presently, all equipment is disabled when running on a
     * simulation rather than an actual chip.  In particular,
     * GPIB communication is disabled so that we do not
     * need to connect to GPIB devices.
     */
    protected boolean isDisabled() {
        return SimulationModel.isInUse();
    }

    /** test program */
    public static void main(String[] argv) {
        Equipment pulse = new Equipment("SRS535"); // Stanford Pulse Generator
        Equipment osc = new Equipment("HPINF54845A"); // HP Infinium Scope
        osc.write("MEAS:FREQ? CHAN1");
        System.out.println("Freq:" + osc.read(30));
    }
}
