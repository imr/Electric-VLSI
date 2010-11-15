package com.sun.electric.plugins.irsim;

import com.sun.electric.api.irsim.IAnalyzer;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Unit test for simple App.
 */
public class AppTest
        extends TestCase {

    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public AppTest(String testName) {
        super(testName);
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite() {
        return new TestSuite(AppTest.class);
    }

    public void testIRSIM1() throws IOException {
        int irDebug = 0;
        String steppingModel = "RC";
        URL parameterURL = AppTest.class.getResource("scmos0.3.prm");
        boolean isDelayedX = true;
        boolean showCommands = true;
        IAnalyzer.EngineIRSIM x = new IAnalyzerImpl().createEngine(new FakeGUI(), steppingModel, parameterURL, irDebug, showCommands, isDelayedX);

        x.putTransistor("net@29", "gnd", "net@1", 2.0, 3.5, 10.5, 13.0, 14.0, -17.75, true);
        x.putTransistor("cc", "in", "net@29", 2.0, 5.0, 15.0, 16.0, 0.5, -18.5, true);
        x.putTransistor("in", "gnd", "net@26", 2.0, 10.0, 30.0, 26.0, -21.5, -19.0, true);
        x.putTransistor("net@1", "gnd", "out", 2.0, 20.0, 60.0, 46.0, 22.5, -25.5, true);
        x.putTransistor("cc", "net@6", "net@26", 2.0, 5.0, 15.0, 16.0, -13.0, -16.5, true);
        x.putTransistor("net@26", "gnd", "in", 2.0, 3.0, 9.0, 12.0, -8.5, -29.0, true);
        x.putTransistor("cc", "net@29", "net@6", 2.0, 3.0, 9.0, 12.0, -1.0, 8.5, false);
        x.putTransistor("in", "vdd", "net@26", 2.0, 10.0, 30.0, 26.0, -18.0, 12.0, false);
        x.putTransistor("net@26", "in", "vdd", 2.0, 5.0, 15.0, 16.0, -21.0, 33.5, false);
        x.putTransistor("in", "vdd", "net@6", 2.0, 5.0, 15.0, 16.0, -9.5, 9.5, false);
        x.putTransistor("net@6", "vdd", "out", 2.0, 20.0, 60.0, 46.0, 12.0, 21.5, false);
        x.putTransistor("net@6", "out", "vdd", 2.0, 20.0, 60.0, 46.0, 12.0, 29.5, false);
        x.putTransistor("net@26", "net@29", "vdd", 2.0, 5.0, 15.0, 16.0, 7.5, 9.5, false);
        x.putTransistor("net@29", "vdd", "net@1", 2.0, 6.0, 18.0, 18.0, 15.5, 9.0, false);
        x.finishNetwork();

        x.convertStimuli();
        x.init();
        x.clearAllVectors();
        InputStreamReader reader = new InputStreamReader(AppTest.class.getResourceAsStream("IRSIM-1.cmd"));
        x.restoreStimuli(reader);
        reader.close();
    }

    public void testIRSIM2() throws IOException {
        int irDebug = 0;
        String steppingModel = "RC";
        URL parameterURL = AppTest.class.getResource("scmos0.3.prm");
        boolean isDelayedX = true;
        boolean showCommands = true;
        IAnalyzer.EngineIRSIM x = new IAnalyzerImpl().createEngine(new FakeGUI(), steppingModel, parameterURL, irDebug, showCommands, isDelayedX);

        for (int i = 0; i < 12; i++) {
            x.putTransistor("a[" + i + "]", "out[" + i + "]", "vdd", 2.0, 3.0, 9.0, 12.0, -197.5, 44.5, true);
            x.putTransistor("b[" + i + "]", "vdd", "out[" + i + "]", 2.0, 3.0, 9.0, 12.0, -182.5, 44.5, true);
            x.putTransistor("b[" + i + "]", "and@" + i + "/net@21", "gnd", 2.0, 3.0, 9.0, 12.0, -190.0, 12.0, false);
            x.putTransistor("a[" + i + "]", "out[" + i + "]", "and@" + i + "/net@21", 2.0, 3.0, 9.0, 12.0, -190.0, 21.0, false);
        }
        x.finishNetwork();

        x.convertStimuli();
        x.init();
        x.clearAllVectors();
        InputStreamReader reader = new InputStreamReader(AppTest.class.getResourceAsStream("IRSIM-2.cmd"));
        x.restoreStimuli(reader);
        reader.close();
    }

    public void testIRSIM3() throws IOException {
        int irDebug = 0;
        String steppingModel = "RC";
        URL parameterURL = AppTest.class.getResource("scmos0.3.prm");
        boolean isDelayedX = true;
        boolean showCommands = true;
        IAnalyzer.EngineIRSIM x = new IAnalyzerImpl().createEngine(new FakeGUI(), steppingModel, parameterURL, irDebug, showCommands, isDelayedX);

        InputStreamReader simReader = new InputStreamReader(AppTest.class.getResourceAsStream("IRSIM-3.sim"));
        x.inputSim(simReader, "IRSIM-3.sim");
        simReader.close();
        double lambda = x.getLambda();
        x.finishNetwork();

        x.convertStimuli();
        x.init();
        x.clearAllVectors();
        InputStreamReader reader = new InputStreamReader(AppTest.class.getResourceAsStream("IRSIM-3.cmd"));
        x.restoreStimuli(reader);
        reader.close();
    }

    private class FakeGUI implements IAnalyzer.GUI {

        private List<TestSignal> signals = new ArrayList<TestSignal>();

        public IAnalyzer.GuiSignal makeSignal(String name) {
            // make a signal for it
            TestSignal sig = new TestSignal(name, null);
            signals.add(sig);
            return sig;
        }

        public void makeBusSignals(List<IAnalyzer.GuiSignal> sigList) {
        }

        public void createBus(String busName, IAnalyzer.GuiSignal... subsigs) {
            TestSignal sig = new TestSignal(busName, subsigs);
            signals.add(sig);
        }

        public Collection<IAnalyzer.GuiSignal> getSignals() {
            return Collections.<IAnalyzer.GuiSignal>unmodifiableCollection(signals);
        }

        public void setMainXPositionCursor(double curTime) {
            System.out.println("gui.setMainXPositionCursor(" + curTime + ");");
        }

        public void openPanel(Collection<IAnalyzer.GuiSignal> sigs) {
            System.out.print("gui.openPanel(");
            for (IAnalyzer.GuiSignal sig : sigs) {
                System.out.print(" " + sig.getFullName());
            }
            System.out.println(");");
        }

        public void closePanels() {
            System.out.println("gui.closePanels();");
        }

        public double getMaxPanelTime() {
            return Double.NEGATIVE_INFINITY;
        }

        public void repaint() {
            System.out.println("gui.repaint();");
        }

        public char canonicChar(char c) {
            return Character.toLowerCase(c);
        }
        
        public String canonicString(String s) {
            return s.toLowerCase();
        }

        public double atof(String text) {
            return Double.parseDouble(text);
        }
    
        public int atoi(String text) {
            text = text.trim();
            int v = 0;
            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c < '0' || c > '9') break;
                v = v*10 + (c - '0');
            }
            return v;
        }
    
        public String formatDouble(double v) {
            return Double.toString(v);
        } 
    }
    
    private class TestSignal implements IAnalyzer.GuiSignal {
        private String fullName;
        private IAnalyzer.GuiSignal[] busMembers;
        
        private TestSignal(String fullName, IAnalyzer.GuiSignal[] busMembers) {
            this.fullName = fullName;
            this.busMembers = busMembers;
        }
        
        public String getFullName() {
            return fullName;
        }
        public String getSignalName() {
            return fullName;
        }
        public IAnalyzer.GuiSignal[] getBusMembers() {
            return busMembers;
        }
        public void addControlPoint(double time) {
           System.out.println("findSignal(\""+fullName+"\").addControlPoint("+time+");");
        }
        public void removeControlPoint(double time) {
           System.out.println("findSignal(\""+fullName+"\").removeControlPoint("+time+");");
        }
        public void addSample(double t, IAnalyzer.LogicState v) {
//           System.out.println("findSignal(\""+fullName+"\").addSample("+t+","+v+");");
        }
    }
}
