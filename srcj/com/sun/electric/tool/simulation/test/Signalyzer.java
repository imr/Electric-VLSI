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
 * plus a (required?) Vref pin:
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
 * @author Adam Megacz (megacz)
 * @version 0.1 23.Oct.2009 
 *
 * Copyright (c) 2009 by Nuts, Deez.
 */
import java.io.*;

public class Signalyzer extends JtagTester {

    private ExecProcess urjtag;

    public Signalyzer() {
        urjtag = new ExecProcess("jtag", new String[0], new File("."), null, null);
        urjtag.run();
        urjtag.writeln("cable Signalyzer");
    }

    public void configure(float tapVolt, long kiloHerz) {
        // tapVolt is not configurable on this device
        urjtag.writeln("frequency " + (kiloHerz*1000));
    }

    public void reset() {
        urjtag.writeln("pod set TRST=0");
        // FIXME: sleep here
        urjtag.writeln("pod set TRST=1");
    }

    public void tms_reset() {
        urjtag.writeln("reset");
    }

    public void disconnect() {
        urjtag.destroyProcess();
    }

    public void setLogicOutput(int index, boolean newLevel) {
        if (index!=0) throw new RuntimeException("we only support one GPIO, pin 15");
        urjtag.writeln("pod set RESET="+(newLevel?"1":"0"));
    }

    public void shift(ChainNode chain, boolean readEnable, boolean writeEnable, int irBadSeverity) {
        throw new RuntimeException("yet not implemented");
    }
}
