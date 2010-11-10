/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IRSIM.java
 *
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.tool.simulation.irsim;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.lib.LibFile;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.extract.ExtractedPBucket;
import com.sun.electric.tool.extract.RCPBucket;
import com.sun.electric.tool.extract.TransistorPBucket;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.output.IRSIM.IRSIMPreferences;
import com.sun.electric.tool.simulation.BusSample;
import com.sun.electric.tool.simulation.DigitalSample;
import com.sun.electric.tool.simulation.DigitalSample.Value;
import com.sun.electric.tool.simulation.Engine;
import com.sun.electric.tool.simulation.MutableSignal;
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.simulation.SignalCollection;
import com.sun.electric.tool.simulation.SimulationTool;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.waveform.Panel;
import com.sun.electric.tool.user.waveform.WaveSignal;
import com.sun.electric.tool.user.waveform.WaveformWindow;
import com.sun.electric.util.TextUtils;
import com.sun.electric.util.config.Configuration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.swing.ProgressMonitorInputStream;
import javax.swing.SwingUtilities;

/**
 * Class for interfacing to the IRSIM simulator by reflection.
 */
public class IRSIM implements Engine, IAnalyzer.GUI {

    /** initial size of simulation window: 10ns */
    private static final double DEFIRSIMTIMERANGE = 10.0E-9f;
    private static boolean IRSIMAvailable;
    private static boolean IRSIMChecked = false;

    /**
     * Method to tell whether the IRSIM simulator is available.
     * IRSIM is packaged separately because it is from Stanford University.
     * This method dynamically figures out whether the IRSIM module is present by using reflection.
     * @return true if the IRSIM simulator is available.
     */
    public static boolean hasIRSIM() {
        if (!IRSIMChecked) {
            IRSIMChecked = true;
            IRSIMAvailable = Configuration.lookup(IAnalyzer.class) != null;
            if (!IRSIMAvailable) {
                TextUtils.recordMissingComponent("IRSIM");
            }
        }
        return IRSIMAvailable;
    }

    /**
     * Method to run the IRSIM simulator on a given cell, context or file.
     * Uses reflection to find the IRSIM simulator (if it exists).
     * @param cell the Cell to simulate.
     * @param context the context to the cell to simulate.
     * @param fileName the name of the file with the netlist.  If this is null, simulate the cell.
     * If this is not null, ignore the cell and simulate the file.
     */
    public static void runIRSIM(Cell cell, VarContext context, String fileName, IRSIMPreferences ip, boolean doNow) {
        try {
            URL parameterURL = null;
            String parameterFile = ip.parameterFile.trim();
            if (parameterFile.length() > 0) {
                File pf = new File(parameterFile);
                if (pf != null && pf.exists()) {
                    parameterURL = TextUtils.makeURLToFile(parameterFile);
                } else {
                    parameterURL = LibFile.getLibFile(parameterFile);
                }
                if (parameterURL == null) {
                    System.out.println("Cannot find parameter file: " + parameterFile);
                }
            }
            IAnalyzer ianalyzer = Configuration.lookup(IAnalyzer.class);
            final IRSIM irsim = new IRSIM();
            final IAnalyzer.EngineIRSIM analyzer = ianalyzer.createEngine(irsim,
                    ip.steppingModel,
                    parameterURL,
                    ip.irDebug,
                    SimulationTool.isIRSIMShowsCommands(),
                    SimulationTool.isIRSIMDelayedX());
            irsim.a = analyzer;
            synchronized (analyzer) {
                // Load network
                if (cell != null) {
                    System.out.println("Loading netlist for " + cell.noLibDescribe() + "...");
                } else {
                    System.out.println("Loading netlist for file " + fileName + "...");
                }

                // Load network
                if (fileName == null) {
                    // generate the components directly
                    List<Object> components = com.sun.electric.tool.io.output.IRSIM.getIRSIMComponents(cell, context, ip);
                    Technology layoutTech = Schematics.getDefaultSchematicTechnology();
                    double lengthOff = Schematics.getDefaultSchematicTechnology().getGateLengthSubtraction() / layoutTech.getScale();
                    // load the circuit from memory
                    for (Object obj : components) {
                        ExtractedPBucket pb = (ExtractedPBucket) obj;

                        if (pb instanceof TransistorPBucket) {
                            TransistorPBucket tb = (TransistorPBucket) pb;
                            analyzer.putTransistor(tb.gateName, tb.sourceName, tb.drainName,
                                    tb.getTransistorLength(lengthOff), tb.getTransistorWidth(),
                                    tb.getActiveArea(), tb.getActivePerim(),
                                    tb.ni.getAnchorCenterX(), tb.ni.getAnchorCenterY(),
                                    tb.getType() == 'n');
                        } else if (pb instanceof RCPBucket) {
                            RCPBucket rcb = (RCPBucket) pb;
                            switch (rcb.getType()) {
                                case 'r':
                                    analyzer.putResistor(rcb.net1, rcb.net2, rcb.rcValue);
                                    break;
                                case 'C':
                                    analyzer.putCapacitor(rcb.net1, rcb.net2, rcb.rcValue);
                                    break;
                            }
                        }
                    }
                } else {
                    // get a pointer to to the file with the network (.sim file)
                    URL fileURL = TextUtils.makeURLToFile(fileName);
                    if (irsim.inputSim(fileURL)) {
                        return;
                    }
                }
                analyzer.finishNetwork();

                irsim.sd.setEngine(irsim);

                irsim.sigCollection = Stimuli.newSignalCollection(irsim.sd, "SIGNALS");
                irsim.sd.setSeparatorChar('/');
                analyzer.convertStimuli();
                irsim.sd.setCell(cell);

                // make a waveform window
                if (doNow) {
                    WaveformWindow.showSimulationDataInNewWindow(irsim.sd);
                    WaveformWindow ww = irsim.sd.getWaveformWindow();
                    ww.setDefaultHorizontalRange(0.0, DEFIRSIMTIMERANGE);
                    ww.setMainXPositionCursor(DEFIRSIMTIMERANGE / 5.0 * 2.0);
                    ww.setExtensionXPositionCursor(DEFIRSIMTIMERANGE / 5.0 * 3.0);
                    irsim.ww = ww;
                    analyzer.init();
                } else {
                    SwingUtilities.invokeLater(new Runnable() {

                        public void run() {
                            WaveformWindow.showSimulationDataInNewWindow(irsim.sd);
                            WaveformWindow ww = irsim.sd.getWaveformWindow();
                            ww.setDefaultHorizontalRange(0.0, DEFIRSIMTIMERANGE);
                            ww.setMainXPositionCursor(DEFIRSIMTIMERANGE / 5.0 * 2.0);
                            ww.setExtensionXPositionCursor(DEFIRSIMTIMERANGE / 5.0 * 3.0);
                            irsim.ww = ww;
                            analyzer.init();
                        }
                    });
                }
            }
        } catch (Exception e) {
            System.out.println("Unable to run the IRSIM simulator");
            e.printStackTrace(System.out);
        }
    }
    private IAnalyzer.EngineIRSIM a;
    private WaveformWindow ww;
    /** the SignalCollection being displayed */
    private SignalCollection sigCollection;
    private final Stimuli sd = new Stimuli();

    // Engine
    /**
     * Returns FileType of vectors file.
     */
    public FileType getVectorsFileType() {
        return FileType.IRSIMVECTOR;
    }

    /**
     * Returns current Stimuli.
     */
    public Stimuli getStimuli() {
        return sd;
    }

    /**
     * Method to reload the circuit data.
     */
    public void refresh() {
    }

    /**
     * Method to update the simulation (because some stimuli have changed).
     */
    public void update() {
        a.playVectors();
    }

    /**
     * Method to set the currently-selected signal high at the current time.
     */
    public void setSignalHigh() {
        List<Signal<?>> signals = ww.getHighlightedNetworkNames();
        if (signals.isEmpty()) {
            Job.getUserInterface().showErrorMessage("Must select a signal before setting it High",
                    "No Signals Selected");
            return;
        }
        for (Signal<?> sig : signals) {
            String signalName = sig.getFullName().replace('.', '/');
            a.newContolPoint(signalName, ww.getMainXPositionCursor(), Value.HIGH);
        }
        if (SimulationTool.isBuiltInResimulateEach()) {
            a.playVectors();
        }
    }

    /**
     * Method to set the currently-selected signal low at the current time.
     */
    public void setSignalLow() {
        List<Signal<?>> signals = ww.getHighlightedNetworkNames();
        if (signals.isEmpty()) {
            Job.getUserInterface().showErrorMessage("Must select a signal before setting it Low",
                    "No Signals Selected");
            return;
        }
        for (Signal<?> sig : signals) {
            String signalName = sig.getFullName().replace('.', '/');
            a.newContolPoint(signalName, ww.getMainXPositionCursor(), Value.LOW);
        }
        if (SimulationTool.isBuiltInResimulateEach()) {
            a.playVectors();
        }
    }

    /**
     * Method to set the currently-selected signal undefined at the current time.
     */
    public void setSignalX() {
        List<Signal<?>> signals = ww.getHighlightedNetworkNames();
        if (signals.isEmpty()) {
            Job.getUserInterface().showErrorMessage("Must select a signal before setting it Undefined",
                    "No Signals Selected");
            return;
        }
        for (Signal<?> sig : signals) {
            String signalName = sig.getFullName().replace('.', '/');
            a.newContolPoint(signalName, ww.getMainXPositionCursor(), Value.X);
        }
        if (SimulationTool.isBuiltInResimulateEach()) {
            a.playVectors();
        }
    }

    /**
     * Method to set the currently-selected signal to have a clock with a given period.
     */
    public void setClock(double period) {
        System.out.println("IRSIM CANNOT HANDLE CLOCKS YET");
    }

    /**
     * Method to show information about the currently-selected signal.
     */
    public void showSignalInfo() {
        List<Signal<?>> signals = ww.getHighlightedNetworkNames();
        if (signals.isEmpty()) {
            Job.getUserInterface().showErrorMessage("Must select a signal before displaying it",
                    "No Signals Selected");
            return;
        }
        for (Signal<?> sig : signals) {
            a.showSignalInfo(sig);
        }
    }

    /**
     * Method to remove all stimuli from the currently-selected signal.
     */
    public void removeStimuliFromSignal() {
        List<Signal<?>> signals = ww.getHighlightedNetworkNames();
        if (signals.size() != 1) {
            Job.getUserInterface().showErrorMessage("Must select a single signal on which to clear stimuli",
                    "No Signals Selected");
            return;
        }
        Signal<?> sig = signals.get(0);
        sig.clearControlPoints();
        a.clearContolPoints(sig);
        if (SimulationTool.isBuiltInResimulateEach()) {
            a.playVectors();
        }
    }

    /**
     * Method to remove the selected stimuli.
     * @return true if stimuli were deleted.
     */
    public boolean removeSelectedStimuli() {
        boolean found = false;
        for (Iterator<Panel> it = ww.getPanels(); it.hasNext();) {
            Panel wp = it.next();
            for (WaveSignal ws : wp.getSignals()) {
                if (!ws.isHighlighted()) {
                    continue;
                }
                double[] selectedCPs = ws.getSelectedControlPoints();
                if (selectedCPs == null) {
                    continue;
                }
                for (int i = 0; i < selectedCPs.length; i++) {
                    if (a.clearControlPoint(ws.getSignal(), selectedCPs[i])) {
                        found = true;
                    }
                }
            }
        }
        if (!found) {
            System.out.println("There are no selected control points to remove");
            return false;
        }

        // resimulate if requested
        if (SimulationTool.isBuiltInResimulateEach()) {
            a.playVectors();
        }
        return true;
    }

    /**
     * Method to remove all stimuli from the simulation.
     */
    public void removeAllStimuli() {
        for (Iterator<Panel> it = ww.getPanels(); it.hasNext();) {
            Panel wp = it.next();
            for (WaveSignal ws : wp.getSignals()) {
                ws.getSignal().clearControlPoints();
            }
        }
        a.clearAllVectors();

        if (SimulationTool.isBuiltInResimulateEach()) {
            a.playVectors();
        }
    }

    /**
     * Method to save the current stimuli information to disk.
     * @param stimuliFile file to save stimuli information
     */
    public void saveStimuli(File stimuliFile) throws IOException {
        a.saveStimuli(stimuliFile);
    }

    /**
     * Method to restore the current stimuli information from URL.
     * @param reader Reader with stimuli information
     */
    public void restoreStimuli(URL stimuliURL) throws IOException {
        if (stimuliURL == null) {
            throw new NullPointerException();
        }
        Reader reader = new InputStreamReader(stimuliURL.openStream());
        try {

            // remove all vectors
            a.clearAllVectors();
            for (Iterator<Panel> it = ww.getPanels(); it.hasNext();) {
                Panel wp = it.next();
                for (WaveSignal ws : wp.getSignals()) {
                    ws.getSignal().clearControlPoints();
                }
            }
            a.restoreStimuli(reader);
        } finally {
            reader.close();
        }
    }

    // IAnalyzer.GUI
    public MutableSignal<DigitalSample> makeSignal(String name) {
        // make a signal for it
        int slashPos = name.lastIndexOf('/');
        MutableSignal<DigitalSample> sig =
                slashPos >= 0
                ? DigitalSample.createSignal(sigCollection, sd, name.substring(slashPos + 1), name.substring(0, slashPos))
                : DigitalSample.createSignal(sigCollection, sd, name, null);
        return sig;
    }

    public void createBus(String busName, Signal<DigitalSample>... subsigs) {
        BusSample.createSignal(sigCollection, sd, busName, null, true, subsigs);
    }

    public void makeBusSignals(List<Signal<?>> sigList) {
        sd.makeBusSignals(sigList, sigCollection);
    }

    public Collection<Signal<?>> getSignals() {
        return sigCollection.getSignals();
    }

    public void setMainXPositionCursor(double curTime) {
        if (SimulationTool.isBuiltInAutoAdvance()) {
            ww.setMainXPositionCursor(curTime + 10.0 / 1000000000.0);
        }
    }

    public void openPanel(Collection<Signal<DigitalSample>> sigs) {
        for (Signal<?> sig : sigs) {
            int height = User.getWaveformDigitalPanelHeight();
            Panel wp = new Panel(ww, height);
            wp.makeSelectedPanel(-1, -1);
            new WaveSignal(wp, sig);
        }
    }

    public void closePanels() {
        ww.clearHighlighting();
        List<Panel> allPanels = new ArrayList<Panel>();
        for (Iterator<Panel> it = ww.getPanels(); it.hasNext();) {
            allPanels.add(it.next());
        }
        for (Panel wp : allPanels) {
            wp.closePanel();
        }
    }

    public double getMaxPanelTime() {
        double maxPanelTime = Double.NEGATIVE_INFINITY;
        Iterator<Panel> it = ww.getPanels();
        if (it.hasNext()) {
            Panel wp = it.next();
            maxPanelTime = Math.max(maxPanelTime, wp.getMaxXAxis());
        }
        return maxPanelTime;
    }

    public void repaint() {
        ww.repaint();
    }
    
	/**
	 * Load a .sim file into memory.
	 *
	 * A .sim file consists of a series of lines, each of which begins with a key letter.
	 * The key letter beginning a line determines how the remainder of the line is interpreted.
	 * The following are the list of key letters understood.
	 *
	 *   | units: s tech: tech format: MIT|LBL|SU
	 *     If present, this must be the first line in the .sim file.
	 *     It identifies the technology of this circuit as tech and gives a scale factor for units of linear dimension as s.
	 *     All linear dimensions appearing in the .sim file are multiplied by s to give centimicrons.
	 *     The format field signifies the sim variant. Electric only recognizes SU format. 
	 *   type g s d l w x y g=gattrs s=sattrs d=dattrs
	 *     Defines a transistor of type type. Currently, type may be e or d for NMOS, or p or n for CMOS.
	 *     The name of the node to which the gate, source, and drain of the transistor are connected are given by g, s, and d respectively.
	 *     The length and width of the transistor are l and w. The next two tokens, x and y, are optional.
	 *     If present, they give the location of a point inside the gate region of the transistor.
	 *     The last three tokens are the attribute lists for the transistor gate, source, and drain.
	 *     If no attributes are present for a particular terminal, the corresponding attribute list may be absent
	 *     (i.e, there may be no g= field at all).
	 *     The attribute lists gattrs, etc. are comma-separated lists of labels.
	 *     The label names should not include any spaces, although some tools can accept label names with
	 *     spaces if they are enclosed in double quotes. In version 6.4.5 and later the default format
	 *     produced by ext2sim is SU. In this format the attribute of the gate starting with S_ is the substrate node of the fet.
	 *     The attributes of the gate, and source and substrate starting with A_, P_ are the area and perimeter
	 *     (summed for that node only once) of the source and drain respectively. This addition to the format is backwards compatible. 
	 *   C n1 n2 cap
	 *     Defines a capacitor between nodes n1 and n2. The value of the capacitor is cap femtofarads.
	 *     NOTE: since many analysis tools compute transistor gate capacitance themselves from the
	 *     transistor's area and perimeter, the capacitance between a node and substrate (GND!)
	 *     normally does not include the capacitance from transistor gates connected to that node.
	 *     If the .sim file was produced by ext2sim(1), check the technology file that was used to
	 *     produce the original .ext files to see whether transistor gate capacitance is included or excluded;
	 *     see "Magic Maintainer's Manual 2 - The Technology File for details. 
	 *   R node res
	 *     Defines the lumped resistance of node node to be res ohms.
	 *   r node1 node2 res
	 *     Defines an explicit resistor between nodes node1 and node2 of resistance res ohms.
	 *   N node darea dperim parea pperim marea mperim
	 *     As an alternative to computed capacitances, some tools expect the total perimeter and area
	 *     of the polysilicon, diffusion, and metal in each node to be reported in the .sim file.
	 *     The N construct associates diffusion area darea (in square centimicrons) and diffusion
	 *     perimeter dperim (in centimicrons) with node node, polysilicon area parea and perimeter pperim,
	 *     and metal area marea and perimeter mperim. This construct is technology dependent and obsolete. 
	 *   = node1 node2
	 *     Each node in a .sim file is named implicitly by having it appear in a transistor definition.
	 *     All node names appearing in a .sim file are assumed to be distinct.
	 *     Some tools, such as esim(1), recognize aliases for node names.
	 *     The = construct allows the name node2 to be defined as an alias for the name node1.
	 *     Aliases defined by means of this construct may not appear anywhere else in the .sim file.
	 */
	private boolean inputSim(URL simFileURL)
	{
		// read the file
		String fileName = simFileURL.getFile();
		try
		{
			URLConnection urlCon = simFileURL.openConnection();
//			String contentLength = urlCon.getHeaderField("content-length");
//			long fileLength = -1;
//			try {
//				fileLength = Long.parseLong(contentLength);
//			} catch (Exception e) {}
//			long readSoFar = 0;
			InputStream inputStream = urlCon.getInputStream();
			InputStreamReader is = new InputStreamReader(new ProgressMonitorInputStream(null, "import " + fileName, inputStream));
            int numErrors = a.inputSim(is, fileName);
			inputStream.close();
    		System.out.println("Loaded circuit, lambda=" + a.getLambda() + "u");
            return numErrors > 0;
		} catch (IOException e)
		{
			System.out.println("Error reading file");
            return false;
		}
	}
}
