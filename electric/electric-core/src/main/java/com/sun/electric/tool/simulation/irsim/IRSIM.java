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
import com.sun.electric.tool.extract.ExtractedPBucket;
import com.sun.electric.tool.extract.RCPBucket;
import com.sun.electric.tool.extract.TransistorPBucket;
import com.sun.electric.tool.io.output.IRSIM.IRSIMPreferences;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.user.waveform.WaveformWindow;
import com.sun.electric.util.TextUtils;
import com.sun.electric.util.config.Configuration;

import java.io.File;
import java.net.URL;
import java.util.List;
import javax.swing.SwingUtilities;

/**
 * Class for interfacing to the IRSIM simulator by reflection.
 */
public class IRSIM
{
	/** initial size of simulation window: 10ns */		private static final double DEFIRSIMTIMERANGE = 10.0E-9f;
    
	private static boolean IRSIMAvailable;
	private static boolean IRSIMChecked = false;

	/**
     * Method to tell whether the IRSIM simulator is available.
     * IRSIM is packaged separately because it is from Stanford University.
     * This method dynamically figures out whether the IRSIM module is present by using reflection.
     * @return true if the IRSIM simulator is available.
     */
    public static boolean hasIRSIM()
    {
    	if (!IRSIMChecked)
    	{
    		IRSIMChecked = true;
        	IRSIMAvailable = Configuration.lookup(IAnalyzer.class) != null;
        	if (!IRSIMAvailable) TextUtils.recordMissingComponent("IRSIM");
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
    public static void runIRSIM(Cell cell, VarContext context, String fileName, IRSIMPreferences ip, boolean doNow)
    {
        try
        {
            URL parameterURL = null;
            String parameterFile = ip.parameterFile.trim();
            if (parameterFile.length() > 0)
            {
                File pf = new File(parameterFile);
                if (pf != null && pf.exists())
                {
                    parameterURL = TextUtils.makeURLToFile(parameterFile);
                } else
                {
                    parameterURL = LibFile.getLibFile(parameterFile);
                }
                if (parameterURL == null)
                {
                    System.out.println("Cannot find parameter file: " + parameterFile);
                }
            }
            IAnalyzer ianalyzer = Configuration.lookup(IAnalyzer.class);
            final IAnalyzer.EngineIRSIM analyzer = ianalyzer.createEngine(ip.steppingModel, parameterURL, ip.irDebug);
            synchronized(analyzer)
            {
                // Load network
                if (cell != null) {
                    System.out.println("Loading netlist for " + cell.noLibDescribe() + "...");
                } else {
                    System.out.println("Loading netlist for file " + fileName + "...");
                }
                
                // Load network
                if (fileName == null)
                {
                    // generate the components directly
                    List<Object>components = com.sun.electric.tool.io.output.IRSIM.getIRSIMComponents(cell, context, ip);
                    Technology layoutTech = Schematics.getDefaultSchematicTechnology();
                    double lengthOff = Schematics.getDefaultSchematicTechnology().getGateLengthSubtraction() / layoutTech.getScale();
                    // load the circuit from memory
                    for(Object obj : components)
                    {
                        ExtractedPBucket pb = (ExtractedPBucket)obj;

                        if (pb instanceof TransistorPBucket)
                        {
                            TransistorPBucket tb = (TransistorPBucket)pb;
                            analyzer.putTransistor(tb.gateName, tb.sourceName, tb.drainName,
                                    tb.getTransistorLength(lengthOff), tb.getTransistorWidth(),
                                    tb.getActiveArea(), tb.getActivePerim(),
                                    tb.ni.getAnchorCenterX(), tb.ni.getAnchorCenterY(),
                                    tb.getType() == 'n');
                        }
                        else if (pb instanceof RCPBucket)
                        {
                            RCPBucket rcb = (RCPBucket)pb;
                            switch (rcb.getType())
                            {
                                case 'r':
                                    analyzer.putResistor(rcb.net1, rcb.net2, rcb.rcValue);
                                    break;
                                case 'C':
                                    analyzer.putCapacitor(rcb.net1, rcb.net2, rcb.rcValue);
                                    break;
                            }
                        }
                    }
                } else
                {
                    // get a pointer to to the file with the network (.sim file)
                    URL fileURL = TextUtils.makeURLToFile(fileName);
                    if (analyzer.inputSim(fileURL)) return;
                }
    
                analyzer.convertStimuli();
                final Stimuli sd = analyzer.getStimuli();
                sd.setCell(cell);

                // make a waveform window
                if (doNow)
                {
                    WaveformWindow.showSimulationDataInNewWindow(sd);
                    WaveformWindow ww = sd.getWaveformWindow();
                    ww.setDefaultHorizontalRange(0.0, DEFIRSIMTIMERANGE);
                    ww.setMainXPositionCursor(DEFIRSIMTIMERANGE/5.0*2.0);
                    ww.setExtensionXPositionCursor(DEFIRSIMTIMERANGE/5.0*3.0);
                    analyzer.init(ww);
                } else
                {
                    SwingUtilities.invokeLater(new Runnable() { public void run()
                    {
                        WaveformWindow.showSimulationDataInNewWindow(sd);
                        WaveformWindow ww = sd.getWaveformWindow();
                        ww.setDefaultHorizontalRange(0.0, DEFIRSIMTIMERANGE);
                        ww.setMainXPositionCursor(DEFIRSIMTIMERANGE/5.0*2.0);
                        ww.setExtensionXPositionCursor(DEFIRSIMTIMERANGE/5.0*3.0);
                        analyzer.init(ww);
                    }});
                }
            }
        } catch (Exception e)
        {
            System.out.println("Unable to run the IRSIM simulator");
            e.printStackTrace(System.out);
        }
    }
}
