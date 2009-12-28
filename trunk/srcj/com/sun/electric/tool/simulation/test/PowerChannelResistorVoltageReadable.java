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
 *
 * @author md227184 (wrote FastProxEquipment, from which this is derived)
 * @author megacz
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
