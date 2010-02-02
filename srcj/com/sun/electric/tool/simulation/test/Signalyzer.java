/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SamplerControl.java
 * Written by Adam Megacz, Sun Microsystems.
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
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
 * Driver for the Signalyzer tool, via urjtag (must be installed).
 *
 *   http://microcontrollershop.com/product_info.php?products_id=1754
 *
 * NOTE THAT THE SIGNALYZER "LITE" DOES NOT HAVE THE SAME PINOUT AS
 * THE DUAL-CHANNEL SIGNALYZER!!
 *
 * The "Lite" version uses the "ARM Standard JTag 20 Pin Connector",
 * which happens to be the same pinout as on the WHITE Corolis boxes
 * plus a REQUIRED Vref pin:
 *
 *    VREF    1 2  n.c
 *    TRST_N  3 4  GND
 *    TDI     5 6  GND
 *    TMS     7 8  GND
 *    TCK     9 10 GND
 *    n.c.   11 12 GND
 *    TDO    13 14 GND
 *    SRST_N 15 16 GND
 *    n.c.   17 18 GND
 *    n.c.   19 20 GND
 * 
 * This is almost exactly the [Corelis] "White Box" connector.  The
 * only Differences are that pin 1 is VREF instead of "reserved" and
 * "master clear" is on pin 15 rather than 17.  You must connect VREF.
 */
import java.io.*;
import java.util.*;

public class Signalyzer extends NetscanGeneric /* even though it's not a NetScan */ {

    private ExecProcess urjtag;
    private BufferedReader br;
    private HashSet<String> defined = new HashSet<String>();
    private static boolean DEBUG = false;

    public Signalyzer() { this(8); }
    public Signalyzer(int irLen) {
        try {
            PipedOutputStream pos = new PipedOutputStream();
            br = new BufferedReader(new InputStreamReader(new PipedInputStream(pos)));
            urjtag = new ExecProcess("jtag -q", new String[0], new File("."),
                                     pos,
                                     new FileOutputStream("urjtag.err"));
            urjtag.setDaemon(true);
            urjtag.start();
            try { Thread.sleep(100); } catch (Exception e) { }
            writeln("cable Signalyzer");
            expectStart("Connected to");
            writeln("detect");
            expectStart("IR length: "+irLen);
            expectStart("Chain length: 1");
            writeln("part 0");
            writeln("instruction length "+irLen);
            
            logicOutput = new LogicSettableArray(1);
            setLogicOutput(0, true);
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public void configure(float tapVolt, long kiloHerz) {
        // tapVolt is not configurable on this device
        writeln("frequency " + (kiloHerz*1000));
        expectStart("Setting TCK frequency to");
    }

    public void reset() {
        writeln("reset");
    }

    public void tms_reset() {
        writeln("reset");
    }

    public void disconnect() {
        urjtag.destroyProcess();
    }

    public void setLogicOutput(int index, boolean newLevel) {
        if (index!=0) throw new RuntimeException("we only support one GPIO, pin 15");
        logicOutput.setLogicState(index, newLevel);
        writeln("pod RESET="+(newLevel?"1":"0"));
    }

    protected int hw_net_scan_ir(int numBits, short[] scanIn, short[] scanOut, int drBits) {
        if (DEBUG) {
            System.err.print("hw_net_scan_ir("+numBits+", {");
            for(int i=0; i<scanIn.length; i++)
                System.err.print(" "+scanIn[i]+",");
            System.err.print("}, {");
            for(int i=0; i<scanOut.length; i++)
                System.err.print(" "+scanOut[i]+",");
            System.err.println("})");
        }

        if (scanIn.length != 1) throw new RuntimeException("not implemented");

        StringBuffer sb = new StringBuffer();
        for(int i=numBits-1; i>=0; i--)
            sb.append( (scanIn[i/16] & (short)(1<<(i%16)))==0 ? "0" : "1" );
        String bits = sb.toString();
        if (!defined.contains(bits)) {
            defined.add(bits);
            writeln("register R"+bits+" "+drBits);
            writeln("instruction I"+bits+" "+bits+" R"+bits);
        }
        writeln("instruction I"+bits);
        writeln("shift ir");

        scanOut[0] = 1;
        return 0;
    }

    protected int hw_net_scan_dr(int numBits, short[] scanIn, short[] scanOut) {
        if (DEBUG) {
            System.err.print("hw_net_scan_dr("+numBits+", {");
            for(int i=0; i<scanIn.length; i++)
                System.err.print(" "+scanIn[i]+",");
            System.err.print("}, {");
            for(int i=0; i<scanOut.length; i++)
                System.err.print(" "+scanOut[i]+",");
            System.err.println("})");
        }

        StringBuffer sb = new StringBuffer();
        for(int i=numBits-1; i>=0; i--)
            sb.append( (scanIn[i/16] & (short)(1<<(i%16)))==0 ? "0" : "1" );
        String bits = sb.toString();
        writeln("dr " + bits);
        expectStart(bits);
        writeln("shift dr");
        writeln("dr");
        String dr = readln();
        if (dr.length()==0) throw new RuntimeException();
        for(int i=0; i<scanOut.length; i++) scanOut[i] = 0;
        int j = 0;
        for(int i=Math.min(dr.length(),numBits)-1; i>=0; i--) {
            switch(dr.charAt(i)) {
                case '0': j++; break;
                case '1': scanOut[j/16] |= (short)(1<<(j%16)); j++; break;
                default: throw new RuntimeException("unexpected char: " +
                                                    dr.charAt(i) + " = " + ((int)dr.charAt(i)));
            }
        }
        return 0;
    }

    private void writeln(String s) {
        if (DEBUG) System.err.println(s);
        urjtag.writeln(s);
    }

    private String readln() {
        try {
            String s;
            while(true) {
                s = br.readLine();
                if (s.startsWith("Device Id: unknown as bit")) continue;
                break;
            }
            if (DEBUG) System.err.println(s);
            return s;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void expectStart(String x) {
        String s = readln();
        if (!s.startsWith(x))
            throw new RuntimeException("expected a line starting with \""+x+"\" but got \""+s+"\"");
    }

}
