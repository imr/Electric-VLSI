/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: HelpMenu.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
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

package com.sun.electric.tool.user.menus;

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.lib.LibFile;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.user.dialogs.About;
import com.sun.electric.tool.user.help.ManualViewer;
import static com.sun.electric.tool.user.menus.EMenuItem.SEPARATOR;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.Client;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.simulation.AnalogSignal;
import com.sun.electric.tool.simulation.Analysis;
import com.sun.electric.tool.user.waveform.Panel;
import com.sun.electric.tool.simulation.Stimuli;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.waveform.WaveSignal;
import com.sun.electric.tool.user.waveform.WaveformWindow;

import javax.swing.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.awt.event.KeyEvent;

/**
 * Class to handle the commands in the "Help" pulldown menu.
 */
public class HelpMenu {

    static EMenu makeMenu() {
        /****************************** THE HELP MENU ******************************/

		// mnemonic keys available:  BC EFGHIJ  MNOPQ  T VWXYZ
        return new EMenu("_Help",

            !Client.isOSMac() ? new EMenuItem("_About Electric...") { public void run() {
                aboutCommand(); }} : null,
			!Client.isOSMac() ? SEPARATOR : null,

		    new EMenuItem("_User's Manual...") { public void run() {
                ManualViewer.userManualCommand(); }},

            ManualViewer.hasRussianManual() ? new EMenuItem("User's Manual (_Russian)...") { public void run() {
                ManualViewer.userManualRussianCommand(); }} : null,

            new EMenuItem("Show _Key Bindings") { public void run() {
                MenuCommands.menuBar().keyBindingManager.printKeyBindings();; }},

        // mnemonic keys available:  BCDEFGHIJK MNOPQRSTUVWXYZ
            new EMenu("_3D Showcase",
                new EMenuItem("_Load Library") { public void run() {
                    ManualViewer.loadSamplesLibrary("floatingGates", "topCell"); }},
                new EMenuItem("_3D View of Cage Cell") { public void run() {
                    ManualViewer.open3DSample("floatingGates" ,"topCell", "3D ShowCase"); }},
                new EMenuItem("_Animate Cage Cell") { public void run() {
                    ManualViewer.animate3DSample("demoCage.j3d"); }}),

		// mnemonic keys available: ABCDEFGHIJKL NO QR TUVWXYZ
            new EMenu("_Load Built-in Libraries",
                new EMenuItem("_Sample Cells") { public void run() {
                    ManualViewer.loadSamplesLibrary("samples", "tech-MOSISCMOS"); }},
                new EMenuItem("_MOSIS CMOS Pads") { public void run() {
                    loadBuiltInLibraryCommand("pads4u"); }},
                new EMenuItem("MI_PS Cells") { public void run() {
                    loadBuiltInLibraryCommand("mipscells"); }}),

		/****************************** ADDITIONS TO THE HELP MENU ******************************/

            Job.getDebug() ? SEPARATOR : null,
            Job.getDebug() ? new EMenuItem("Make fake circuitry MoCMOS") { public void run() {
                makeFakeCircuitryCommand("mocmos", true); }} : null,
            Job.getDebug() ? new EMenuItem("Make fake circuitry TSMC90") { public void run() {
				makeFakeCircuitryCommand("tsmc90", true); }} : null,
            Job.getDebug() ? new EMenuItem("Make fake analog simulation window") { public void run() {
                makeFakeWaveformCommand(); }} : null,
            Job.getDebug() ? new EMenuItem("Make fake interval simulation window")  { public void run() {
                makeFakeIntervalWaveformCommand(); }} : null);
    }

    // ---------------------- THE HELP MENU -----------------

	/**
	 * Method to invoke the "About" dialog.
	 */
	public static void aboutCommand()
    {
		About dialog = new About(TopLevel.getCurrentJFrame(), true);
        dialog.setVisible(true);
    }

	private static void loadBuiltInLibraryCommand(String libName)
	{
		if (Library.findLibrary(libName) != null) return;
		URL url = LibFile.getLibFile(libName + ".jelib");
		FileMenu.ReadLibrary job = new FileMenu.ReadLibrary(url, FileType.JELIB, null, null);
	}
    
	// ---------------------- Help Menu additions -----------------

    private static void makeFakeCircuitryCommand(String tech, boolean asJob)
    {
        // Using reflection to not force the loading of test plugin
        try
        {
            Class makeFakeCircuitry = Class.forName("com.sun.electric.plugins.tests.MakeFakeCircuitry");
            Method makeMethod = makeFakeCircuitry.getDeclaredMethod("makeFakeCircuitryCommand", new Class[] {String.class, String.class, Boolean.class});
            makeMethod.invoke(null, new Object[] {"noname", tech, new Boolean(asJob)});
        }
        catch (Exception ex) {};
    }

	/**
	 * Test method to build an analog waveform with fake data.
	 */
	public static void makeFakeWaveformCommand()
	{
		// make the waveform data
		int numEvents = 100;
		Stimuli sd = new Stimuli();
		Analysis an = new Analysis(sd, Analysis.ANALYSIS_SIGNALS);
		double timeStep = 0.0000000001;
		an.buildCommonTime(numEvents);
		for(int i=0; i<numEvents; i++)
			an.setCommonTime(i, i * timeStep);
		for(int i=0; i<18; i++)
		{
			AnalogSignal as = new AnalogSignal(an);
			as.setSignalName("Signal"+(i+1));
			as.buildValues(numEvents);
			for(int k=0; k<numEvents; k++)
			{
				as.setValue(k, Math.sin((k+i*10) / (2.0+i*2)) * 4);
			}
		}
		sd.setCell(null);

		// make the waveform window
		WindowFrame wf = WindowFrame.createWaveformWindow(sd);
		WaveformWindow ww = (WaveformWindow)wf.getContent();

		// make some waveform panels and put signals in them
		for(int i=0; i<6; i++)
		{
			Panel wp = new Panel(ww, true, Analysis.ANALYSIS_SIGNALS);
			wp.setYAxisRange(-5, 5);
			for(int j=0; j<(i+1)*3; j++)
			{
				AnalogSignal as = (AnalogSignal)an.getSignals().get(j);
				WaveSignal wsig = new WaveSignal(wp, as);
			}
		}
	}

    /**
     * Class to define an interval signal in the simulation waveform window.
     */
    private static class IntervalAnalogSignal extends AnalogSignal {
        private final int signalIndex;
        private final double timeStep;
        
        private IntervalAnalogSignal(Analysis an, double timeStep, int signalIndex) {
            super(an);
            this.signalIndex = signalIndex;
            this.timeStep = timeStep;
            setSignalName("Signal"+(signalIndex+1));
        }
        
        /**
         * Method to return the low end of the interval range for this signal at a given event index.
         * @param sweep sweep index.
         * @param index the event index (0-based).
         * @param result double array of length 3 to return (time, lowValue, highValue)
         */
        public void getEvent(int sweep, int index, double[] result) {
            result[0] = index * timeStep;
            double lowValue = Math.sin((index+signalIndex*10) / (2.0+signalIndex*2)) * 4;
            double increment = Math.sin((index+signalIndex*5) / (2.0+signalIndex));
            result[1] = Math.min(lowValue, lowValue + increment);
            result[2] = Math.max(lowValue, lowValue + increment);
        }
        
        /**
         * Method to return the number of events in this signal.
         * This is the number of events along the horizontal axis, usually "time".
         * @param sweep sweep index.
         * @return the number of events in this signal.
         */
        public int getNumEvents(int sweep) { return 100; }
    }
    
	private static void makeFakeIntervalWaveformCommand()
	{
		// make the interval waveform data
		Stimuli sd = new Stimuli();
		Analysis an = new Analysis(sd, Analysis.ANALYSIS_SIGNALS);
        final double timeStep = 0.0000000001;
		for(int i=0; i<6; i++)
		{
			AnalogSignal as = new IntervalAnalogSignal(an, timeStep, i);
		}
		sd.setCell(null);

		// make the waveform window
		WindowFrame wf = WindowFrame.createWaveformWindow(sd);
		WaveformWindow ww = (WaveformWindow)wf.getContent();
//		ww.setMainXPositionCursor(timeStep*22);
//		ww.setExtensionXPositionCursor(timeStep*77);
//		ww.setDefaultHorizontalRange(0, timeStep*100);

		// make some waveform panels and put signals in them
		int k = 0;
		for(int i=0; i<3; i++)
		{
			Panel wp = new Panel(ww, true, Analysis.ANALYSIS_SIGNALS);
			wp.setYAxisRange(-5, 5);
			for(int j=0; j<=i; j++)
			{
				AnalogSignal as = (AnalogSignal)an.getSignals().get(k++);
				WaveSignal wsig = new WaveSignal(wp, as);
			}
		}
	}
}
