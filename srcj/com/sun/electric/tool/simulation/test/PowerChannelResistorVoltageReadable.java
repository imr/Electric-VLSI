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

    private PowerChannel pc;
    private double ohms;
    private VoltageReadable vr;

    public void setCurrent(float amps) { pc.setCurrent(amps); }
    public float getCurrentSetpoint() { return pc.getCurrentSetpoint(); }
    public float readCurrent() {
        // FIXME: ohmage sanity check using pc.readCurrent()?
        return (float)(vr.readVoltage() / ohms);
    }
    public float readVoltage() { return pc.readVoltage() - vr.readVoltage(); }
    public void setVoltageNoWait(float volts) { throw new RuntimeException("cannot do this in a "+this.getClass().getName()); }
    public void waitForVoltage(float setVolts) { throw new RuntimeException("cannot do this in a "+this.getClass().getName()); }
    public float getVoltageSetpoint() { throw new RuntimeException("cannot do this in a "+this.getClass().getName()); }
    public void setVoltageWait(float volts) {
        
    }

    public PowerChannelResistorVoltageReadable(PowerChannel pc,
                                               float ohmsOfResistor,
                                               VoltageReadable voltMeterAcrossResistor) {
        this.pc = pc;
        this.ohms = ohmsOfResistor;
        this.vr = vr;
    }

    /*
    // for turning on debug mode, default is off

      private static boolean debug = false;

      // power supplies
      private static Pst3202Channel Supply_tempcurrent;
      private static Pst3202Channel Supply_mc;
      private static Pst3202Channel Supply_ibias;
      private static Pst3202Channel Supply_vdd18;
      private static Pst3202Channel Supply_vdd10;
      public static Pst3202Channel Supply_null = null;

      // multimeters
      private static HP34401A Multi_tempcurrent;
      private static HP34401A Multi_probe1;
      private static HP34401A Multi_probe2;
      private static Equipment nvMeter;

      public static void initialize() {
              // set up the gpib
              Infrastructure.gpibControllers = new int[]{0};

              // ps1
              if (debug) {
                      System.out.println("ps1");
              }
              Supply_tempcurrent = new Pst3202Channel("tempcurrent", "ps1", 1);
              Supply_mc = new Pst3202Channel("masterclear", "ps1", 2);

              // ps2
              if (debug) {
                      System.out.println("ps2");
              }
              Supply_ibias = new Pst3202Channel("ibias", "ps2", 1);
              Supply_vdd18 = new Pst3202Channel("vdd18", "ps2", 2);
              Supply_vdd10 = new Pst3202Channel("vdd10", "ps2", 3);

              // multi1
              if (debug) {
                      System.out.println("multi1");
              }
              Multi_tempcurrent = new HP34401A("multi1");

              // H34401C
              if (debug) {
                      System.out.println("H34401C");
              }
              Multi_probe1 = new HP34401A("H34401C");

              // 34401_TOP
              if (debug) {
                      System.out.println("34401_TOP");
              }
              Multi_probe2 = new HP34401A("34401_TOP");

              // multi2
              if (debug) {
                      System.out.println("multi2");
              }
              nvMeter = new Equipment("multi2");
      }

      private static void configureNanoMeter(int channel) {
              double range = 1.2;
              double resolution = 0.0001;
              nvMeter.write("INP:FILT:STAT OFF");
              nvMeter.write("CONF:VOLT:DC " + range + ", " + resolution + ", (@FRONt" + channel + ")");
      }

      public static float readNanoMeter(int channel) {
              configureNanoMeter(channel);
              nvMeter.write("READ?");
              String s = nvMeter.read(40);

              return Float.parseFloat(s);
      }

      public static void configureMicroMeter(String name) {
              double range = 30;
              double resolution = 0.001;
              if (name.matches("tempcurrent")) {
                      Multi_tempcurrent.write("INP:IMP:AUTO ON");
                      Multi_tempcurrent.write("CONF:VOLT:DC " + range + ", " + resolution);
              } else if (name.matches("probe1")) {
                      Multi_probe1.write("INP:IMP:AUTO ON");
                      Multi_probe1.write("CONF:VOLT:DC " + range + ", " + resolution);
              } else if (name.matches("probe2")) {
                      Multi_probe2.write("INP:IMP:AUTO ON");
                      Multi_probe2.write("CONF:VOLT:DC " + range + ", " + resolution);
              }
      }

      public static float readMicroMeter(String name) {
              if (name.matches("tempcurrent")) {
                      configureMicroMeter(name);
                      Multi_tempcurrent.write("READ?");
                      String s = Multi_tempcurrent.read(40);

                      return Float.parseFloat(s);
              } else if (name.matches("probe1")) {
                      configureMicroMeter(name);
                      Multi_probe1.write("READ?");
                      String s = Multi_probe1.read(40);

                      return Float.parseFloat(s);
              } else if (name.matches("probe2")) {
                      configureMicroMeter(name);
                      Multi_probe2.write("READ?");
                      String s = Multi_probe2.read(40);

                      return Float.parseFloat(s);
              } else {
                      return -1.0f;
              }

      }

      public static float readPowerSupplyVoltage(String name) {
              if (name.matches("tempcurrent")) {
                      return Supply_tempcurrent.readVoltage();
              } else if (name.matches("ibias")) {
                      return Supply_ibias.readVoltage();
              } else if (name.matches("vdd18")) {
                      return Supply_vdd18.readVoltage();
              } else if (name.matches("vdd10")) {
                      return Supply_vdd10.readVoltage();
              } else if (name.matches("mc")) {
                      return Supply_mc.readVoltage();
              } else {
                      return -1.0f;
              }
      }

      public static float readPowerSupplyCurrent(String name) {
              if (name.matches("tempcurrent")) {
                      return Supply_tempcurrent.readCurrent();
              } else if (name.matches("ibias")) {
                      return Supply_ibias.readCurrent();
              } else if (name.matches("vdd18")) {
                      return Supply_vdd18.readCurrent();
              } else if (name.matches("vdd10")) {
                      return Supply_vdd10.readCurrent();
              } else if (name.matches("mc")) {
                      return Supply_mc.readCurrent();
              } else {
                      return -1.0f;
              }
      }

      public static void setPowerSupplyVoltage(String name, float volts) {
              if (name.matches("tempcurrent")) {
                      Supply_tempcurrent.setVoltageNoWait(volts);
              } else if (name.matches("ibias")) {
                      Supply_ibias.setVoltageNoWait(volts);
              } else if (name.matches("vdd18")) {
                      Supply_vdd18.setVoltageNoWait(volts);
              } else if (name.matches("vdd10")) {
                      Supply_vdd10.setVoltageNoWait(volts);
              } else if (name.matches("mc")) {
                      Supply_mc.setVoltageNoWait(volts);
              }
      }

      public static void setPowerSupplyCurrent(String name, float amps) {
              if (name.matches("tempcurrent")) {
                      Supply_tempcurrent.setCurrent(amps);
              } else if (name.matches("ibias")) {
                      Supply_ibias.setCurrent(amps);
              } else if (name.matches("vdd18")) {
                      Supply_vdd18.setCurrent(amps);
              } else if (name.matches("vdd10")) {
                      Supply_vdd10.setCurrent(amps);
              } else if (name.matches("mc")) {
                      Supply_mc.setCurrent(amps);
              }
      }

      public static void main(String[] args) {
              debug = true;
              initialize();

              // power supplies
              System.out.println("Testing power supplies...\n");

              System.out.println("Current for temperature sensing (tempcurrent)");
              System.out.println(readPowerSupplyVoltage("tempcurrent"));

              System.out.println("Master Clear (mc)");
              System.out.println(readPowerSupplyVoltage("mc"));

              System.out.println("iBias for the pads (ibias)");
              System.out.println(readPowerSupplyVoltage("ibias"));

              System.out.println("1.0V supply (vdd10)");
              System.out.println(readPowerSupplyVoltage("vdd10"));

              System.out.println("1.8V supply (vdd18)");
              System.out.println(readPowerSupplyVoltage("vdd18"));

              // read each multimeter
              System.out.println("\n\nTesting multimeters...\n");

              System.out.println("Tempcurrent");
              System.out.println(readMicroMeter("tempcurrent"));

              System.out.println("Probe 1");
              System.out.println(readMicroMeter("probe1"));

              System.out.println("Probe 2");
              System.out.println(readMicroMeter("probe2"));

              System.out.println("\n\nTesting NanoVolt multimeter...\n");

              System.out.println("Channel 1");
              System.out.println(readNanoMeter(1));

              System.out.println("Channel 2");
              System.out.println(readNanoMeter(2));

              // if we get this far, all is well
              System.out.println("\n\nAll tests passed");
      }
    */
}
