/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PowerChannelResistorVoltageReadable.java
 * Written by Michael Dayringer and Adam Megacz, Sun Microsystems.
 *
 * Copyright (c) 2008 Sun Microsystems and Static Free Software
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
 * Assuming you have a PowerChannel with a resistor attached to the
 * output and a VoltageReadable across the resistor, this will act
 * like a PowerChannel+CurrentReadable that lets you set the voltage
 * across the load as long as it maintains a reasonably stable current
 * draw.
 *
 *    +--------------+         resistor         +------+
 * +--| PowerChannel |---------/\/\/\/\---------| load |---+
 * |  +--------------+  ^                   ^   +------+   |
 * |                    |                   |              |
 * |                    +- VoltageReadable -+              |
 * |                                                       |
 * +-------------------------------------------------------+
 */
public class PowerChannelResistorVoltageReadable extends PowerChannel {

    private boolean fastConvergence;
    private PowerChannel pc;
    private double ohms;
    private VoltageReadable vr;
    private static float EPSILON_VOLTS = 0.01f;

    public void setCurrent(float amps) { pc.setCurrent(amps); }
    public float getCurrentSetpoint() { return pc.getCurrentSetpoint(); }
    public float readCurrent() {
        double pc_current = pc.readCurrent();
        double vr_current = vr.readVoltage() / ohms;
        if (Math.abs(pc_current - vr_current) > EPSILON_VOLTS * ohms)
            throw new RuntimeException("PowerChannel and VoltageReadable disagree on current; perhaps you gave the wrong resistor value?\n" +
                                       "  PowerChannel    says: " + pc_current + "\n" +
                                       "  VoltageReadable says: " + vr_current);
        return (float)vr_current;
    }
    public float readVoltage() {
        readCurrent();  // to force the sanity check
        return pc.readVoltage() - vr.readVoltage();
    }
    public void setVoltageNoWait(float volts) { throw new RuntimeException("cannot do this in a "+this.getClass().getName()); }
    public void waitForVoltage(float setVolts) { throw new RuntimeException("cannot do this in a "+this.getClass().getName()); }
    public float getVoltageSetpoint() { throw new RuntimeException("cannot do this in a "+this.getClass().getName()); }
    public void setVoltageWait(float v) {
        readCurrent();  // to force the sanity check
        while(true) {
            double vs = pc.readVoltage();
            double vread = vr.readVoltage();
            double i = vread/1000;
            double vl = (vs-ohms*i);
            System.err.print("\r\033[0K\r");
            System.err.print(this.getClass().getSimpleName()+
                             ".setVoltageWait():"+
                             " desired/actual="+v+"/"+vl);
            if (vl+EPSILON_VOLTS < v || vl-EPSILON_VOLTS > v) {
                if (fastConvergence) {
                    double delta = v+i*ohms - vs;
                    delta *= 1.5;
                    vs = vs + delta;
                } else {
                    vs = v+i*ohms;
                }
                pc.setVoltageWait((float)vs);
            } else {
                readCurrent();  // to force the sanity check
                System.err.print("\r\033[0K\r");
                break;
            }
        }
    }

    public PowerChannelResistorVoltageReadable(PowerChannel pc,
                                               float ohmsOfResistor,
                                               VoltageReadable voltMeterAcrossResistor,
                                               boolean fastConvergence) {
        this.pc = pc;
        this.ohms = ohmsOfResistor;
        this.vr = voltMeterAcrossResistor;
        this.fastConvergence = fastConvergence;
    }

}
