/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Simulation.java
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
package com.sun.electric.tool.simulation;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.lib.LibFile;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.io.output.Verilog;
import com.sun.electric.tool.simulation.Signal;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.ui.WaveformWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.geom.Rectangle2D;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

/**
 * This is the Simulation Interface tool.
 */
public class Simulation extends Tool
{
	/** the Simulation tool. */		public static Simulation tool = new Simulation();

	/** key of Variable holding rise time. */				public static final Variable.Key RISE_DELAY_KEY = ElectricObject.newKey("SIM_rise_delay");
	/** key of Variable holding fall time. */				public static final Variable.Key FALL_DELAY_KEY = ElectricObject.newKey("SIM_fall_delay");
	/** key of Variable holding flag for weak nodes. */		public static final Variable.Key WEAK_NODE_KEY = ElectricObject.newKey("SIM_weak_node");
	/** key of Variable holding "M" factors. */				public static final Variable.Key M_FACTOR_KEY = ElectricObject.newKey("ATTR_M");

	private static boolean irsimChecked = false;
	private static Class irsimClass = null;
	private static Method irsimSimulateMethod;

	/**
	 * Method to tell whether the IRSIM simulator is available.
	 * IRSIM is packaged separately because it is from Stanford University.
	 * This method dynamically figures out whether the IRSIM module is present by using reflection.
	 * @return true if the IRSIM simulator is available.
	 */
	public static boolean hasIRSIM()
	{
		if (!irsimChecked)
		{
			irsimChecked = true;

			// find the IRSIM class
			try
			{
				irsimClass = Class.forName("com.sun.electric.plugins.irsim.Analyzer");
			} catch (ClassNotFoundException e)
			{
				irsimClass = null;
				return false;
			}

			// find the necessary methods on the IRSIM class
			try
			{
				irsimSimulateMethod = irsimClass.getMethod("simulateCell", new Class[] {Cell.class, VarContext.class, String.class});			
			} catch (NoSuchMethodException e)
			{
				irsimClass = null;
				return false;
			}
		}

		// if already initialized, return
		if (irsimClass == null) return false;
	 	return true;
	}

	/**
	 * Method to invoke the IRSIM on a Cell via reflection.
	 * @param cell the Cell to simulate.
	 */
	public static void simulateIRSIM(Cell cell, VarContext context, String fileName)
	{
		if (!hasIRSIM()) return;
		try
		{
			irsimSimulateMethod.invoke(irsimClass, new Object[] {cell, context, fileName});
			return;
		} catch (Exception e)
		{
			System.out.println("Unable to run the IRSIM simulator");
            e.printStackTrace(System.out);
		}
	}

	/**
	 * Method to send a command to the IRSIM simulator.
	 */
	public static void doIRSIMCommand(String command)
	{
		if (!hasIRSIM()) return;

		// find an IRSIM simulation engine to control
		Engine engine = null;
		for(Iterator it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = (WindowFrame)it.next();
			if (wf.getContent() instanceof WaveformWindow)
			{
				WaveformWindow ww = (WaveformWindow)wf.getContent();
				Engine e = ww.getSimEngine();
				if (e == null) continue;
				if (!irsimClass.isInstance(e)) continue;
				engine = e;
				if (wf == WindowFrame.getCurrentWindowFrame()) break;
			}
		}
		if (engine != null)
			engine.doCommand(command);
	}

	/**
	 * The constructor sets up the Simulation tool.
	 */
	private Simulation()
	{
		super("simulation");
	}

	/**
	 * Method to initialize the Simulation tool.
	 */
	public void init()
	{
	}

	/**
	 * Method to set a Spice model on the selected node.
	 */
	public static void setSpiceModel()
	{
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return;
        Highlighter highlighter = wf.getContent().getHighlighter();
        if (highlighter == null) return;

		NodeInst ni = (NodeInst)highlighter.getOneElectricObject(NodeInst.class);
		if (ni == null) return;
		SetSpiceModel job = new SetSpiceModel(ni);
	}

	/**
	 * Class to set a Spice Model in a new thread.
	 */
	private static class SetSpiceModel extends Job
	{
		NodeInst ni;
		protected SetSpiceModel(NodeInst ni)
		{
			super("Set Spice Model", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.ni = ni;
			startJob();
		}

		public boolean doIt()
		{
			Variable var = ni.newVar(Spice.SPICE_MODEL_KEY, "SPICE-Model");
			var.setDisplay(true);
			return true;
		}
	}

	/**
	 * Method to set the type of the currently selected wires.
	 * This is used by the Verilog netlister.
	 * @param type 0 for wire; 1 for trireg; 2 for default.
	 */
	public static void setVerilogWireCommand(int type)
	{
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return;
        Highlighter highlighter = wf.getContent().getHighlighter();
        if (highlighter == null) return;

		List list = highlighter.getHighlightedEObjs(false, true);
		if (list.size() == 0)
		{
			System.out.println("Must select arcs before setting their type");
			return;
		}
		SetWireType job = new SetWireType(list, type);
	}

	/**
	 * Class to set Verilog wire types in a new thread.
	 */
	private static class SetWireType extends Job
	{
		List list;
		int type;
		protected SetWireType(List list, int type)
		{
			super("Change Verilog Wire Types", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.list = list;
			this.type = type;
			startJob();
		}

		public boolean doIt()
		{
			for(Iterator it = list.iterator(); it.hasNext(); )
			{
				ArcInst ai = (ArcInst)it.next();
				switch (type)
				{
					case 0:		// set to "wire"
						Variable var = ai.newVar(Verilog.WIRE_TYPE_KEY, "wire");
						var.setDisplay(true);
						break;
					case 1:		// set to "trireg"
						var = ai.newVar(Verilog.WIRE_TYPE_KEY, "trireg");
						var.setDisplay(true);
						break;
					case 2:		// set to default
						if (ai.getVar(Verilog.WIRE_TYPE_KEY) != null)
							ai.delVar(Verilog.WIRE_TYPE_KEY);
						break;
				}
			}
			return true;
		}
	}

	/**
	 * Method to set the strength of the currently selected transistor.
	 * This is used by the Verilog netlister.
	 * @param weak true to set the currently selected transistor to be weak.
	 * false to make it normal strength.
	 */
	public static void setTransistorStrengthCommand(boolean weak)
	{
        WindowFrame wf = WindowFrame.getCurrentWindowFrame();
        if (wf == null) return;
        Highlighter highlighter = wf.getContent().getHighlighter();
        if (highlighter == null) return;

		NodeInst ni = (NodeInst)highlighter.getOneElectricObject(NodeInst.class);
		if (ni == null) return;
		SetTransistorStrength job = new SetTransistorStrength(ni, weak);
	}

	/**
	 * Class to set transistor strengths in a new thread.
	 */
	private static class SetTransistorStrength extends Job
	{
		NodeInst ni;
		boolean weak;
		protected SetTransistorStrength(NodeInst ni, boolean weak)
		{
			super("Change Transistor Strength", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.ni = ni;
			this.weak = weak;
			startJob();
		}

		public boolean doIt()
		{
			if (weak)
			{
				Variable var = ni.newVar(WEAK_NODE_KEY, "Weak");
				var.setDisplay(true);
			} else
			{
				if (ni.getVar(WEAK_NODE_KEY) != null)
					ni.delVar(WEAK_NODE_KEY);
			}
			return true;
		}
	}

	/**
	 * Method to display simulation data in a waveform window.
	 * @param sd the simulation data to display.
	 * @param ww the waveform window to load.
	 * If null, create a new waveform window.
	 */
	public static void showSimulationData(Stimuli sd, WaveformWindow ww)
	{
		// if the window already exists, update the data
		if (ww != null)
		{
			ww.setSimData(sd);
			return;
		}
		
		// create a waveform window
		WindowFrame wf = WindowFrame.createWaveformWindow(sd);
		ww = (WaveformWindow)wf.getContent();

		// set bounds of the window from extent of the data
		Rectangle2D bounds = sd.getBounds();
		double lowTime = bounds.getMinX();
		double highTime = bounds.getMaxX();
		double lowValue = bounds.getMinY();
		double highValue = bounds.getMaxY();
		double timeRange = highTime - lowTime;
		ww.setMainTimeCursor(timeRange*0.2 + lowTime);
		ww.setExtensionTimeCursor(timeRange*0.8 + lowTime);
		ww.setDefaultTimeRange(lowTime, highTime);

		// if the data has an associated cell, see if that cell remembers the signals that were in the waveform window
		if (sd.getCell() != null)
		{
			Variable var = sd.getCell().getVar(WaveformWindow.WINDOW_SIGNAL_ORDER);
			if (var != null && var.getObject() instanceof String[])
			{
				// load the window with previous signal set
				String [] signalNames = (String [])var.getObject();
				boolean isAnalog = sd.isAnalog();
				boolean showedSomething = false;
				for(int i=0; i<signalNames.length; i++)
				{
					String signalName = signalNames[i];
					WaveformWindow.Panel wp = null;
					boolean firstSignal = true;

					// add signals to the panel
					int start = 0;
					for(;;)
					{
						int tabPos = signalName.indexOf('\t', start);
						String sigName = null;
						if (tabPos < 0) sigName = signalName.substring(start); else
						{
							sigName = signalName.substring(start, tabPos);
							start = tabPos+1;
						}
						Signal sSig = sd.findSignalForNetwork(sigName);
						if (sSig != null)
						{
							if (firstSignal)
							{
								firstSignal = false;
								wp = new WaveformWindow.Panel(ww, isAnalog);
								wp.makeSelectedPanel();
								showedSomething = true;
							}
							new WaveformWindow.WaveSignal(wp, sSig);
						}
						if (tabPos < 0) break;
					}
				}
				if (showedSomething)
				{
					if (isAnalog)
					{
						for(Iterator it = ww.getPanels(); it.hasNext(); )
						{
							WaveformWindow.Panel wp = (WaveformWindow.Panel)it.next();
							List signals = wp.getSignals();
							boolean first = true;
							double lowY = 0, highY = 0;
							for(Iterator sIt = signals.iterator(); sIt.hasNext(); )
							{
								WaveformWindow.WaveSignal ws = (WaveformWindow.WaveSignal)sIt.next();
								Signal sSig = ws.getSignal();
								Rectangle2D sigBounds = sSig.getBounds();
								if (first)
								{
									lowY = sigBounds.getMinY();
									highY = sigBounds.getMaxY();
									first = false;
								} else
								{
									if (sigBounds.getMinY() < lowY) lowY = sigBounds.getMinY();
									if (sigBounds.getMaxY() > highY) highY = sigBounds.getMaxY();
								}
							}
							if (!first) wp.setValueRange(lowY, highY);
						}
					}
					return;
				}
			}
		}

		// nothing saved, so show a default set of signals (if it even exists)
		if (sd.isAnalog())
		{
			WaveformWindow.Panel wp = new WaveformWindow.Panel(ww, true);
			wp.setValueRange(lowValue, highValue);
			wp.makeSelectedPanel();
		} else
		{
			// put all top-level signals in
			makeBussedSignals(sd);
			List allSignals = sd.getSignals();
			for(int i=0; i<allSignals.size(); i++)
			{
				DigitalSignal sDSig = (DigitalSignal)allSignals.get(i);
				if (sDSig.getSignalContext() != null) continue;
				if (sDSig.isInBus()) continue;
				if (sDSig.getSignalName().indexOf('@') >= 0) continue;
				WaveformWindow.Panel wp = new WaveformWindow.Panel(ww, false);
				wp.makeSelectedPanel();
				new WaveformWindow.WaveSignal(wp, sDSig);
			}
		}
		ww.getPanel().validate();
	}

	private static void makeBussedSignals(Stimuli sd)
	{
		List signals = sd.getSignals();
		for(int i=0; i<signals.size(); i++)
		{
			Signal sSig = (Signal)signals.get(i);
			int thisBracketPos = sSig.getSignalName().indexOf('[');
			if (thisBracketPos < 0) continue;
			String prefix = sSig.getSignalName().substring(0, thisBracketPos);
			
			// see how many of the following signals are part of the bus
			int j = i+1;
			for( ; j<signals.size(); j++)
			{
				Signal nextSig = (Signal)signals.get(j);

				// other signal must have the same root
				int nextBracketPos = nextSig.getSignalName().indexOf('[');
				if (nextBracketPos < 0) break;
				if (thisBracketPos != nextBracketPos) break;
				if (!prefix.equals(nextSig.getSignalName().substring(0, nextBracketPos))) break;

				// other signal must have the same context
				if (sSig.getSignalContext() == null ^ nextSig.getSignalContext() == null) break;
				if (sSig.getSignalContext() != null)
				{
					if (!sSig.getSignalContext().equals(nextSig.getSignalContext())) break;
				}
			}

			// see how many signals are part of the bus
			int numSignals = j - i;
			if (numSignals <= 1) continue;

			// found a bus of signals: create the bus for it
			DigitalSignal busSig = new DigitalSignal(sd);
			busSig.setSignalName(prefix);
			busSig.setSignalContext(sSig.getSignalContext());
			busSig.buildBussedSignalList();
			for(int k=i; k<j; k++)
			{
				Signal subSig = (Signal)signals.get(k);
				busSig.addToBussedSignalList(subSig);
			}
			i = j - 1;
		}
	}

	/****************************** FAST HENRY OPTIONS ******************************/

	private static Pref cacheFastHenryUseSingleFrequency = Pref.makeBooleanPref("FastHenryUseSingleFrequency", Simulation.tool.prefs, false);
	/**
	 * Method to tell whether FastHenry deck generation should use a single frequency.
	 * The default is false.
	 * @return true if FastHenry deck generation should use a single frequency.
	 */
	public static boolean isFastHenryUseSingleFrequency() { return cacheFastHenryUseSingleFrequency.getBoolean(); }
	/**
	 * Method to set whether FastHenry deck generation should use a single frequency.
	 * @param s true if FastHenry deck generation should use a single frequency.
	 */
	public static void setFastHenryUseSingleFrequency(boolean s) { cacheFastHenryUseSingleFrequency.setBoolean(s); }

	private static Pref cacheFastHenryStartFrequency = Pref.makeDoublePref("FastHenryStartFrequency", Simulation.tool.prefs, 0);
	/**
	 * Method to return the FastHenry starting frequency (or only if using a single frequency).
	 * The default is 0.
	 * @return the FastHenry starting frequency (or only if using a single frequency).
	 */
	public static double getFastHenryStartFrequency() { return cacheFastHenryStartFrequency.getDouble(); }
	/**
	 * Method to set the FastHenry starting frequency (or only if using a single frequency).
	 * @param s the FastHenry starting frequency (or only if using a single frequency).
	 */
	public static void setFastHenryStartFrequency(double s) { cacheFastHenryStartFrequency.setDouble(s); }

	private static Pref cacheFastHenryEndFrequency = Pref.makeDoublePref("FastHenryEndFrequency", Simulation.tool.prefs, 0);
	/**
	 * Method to return the FastHenry ending frequency.
	 * The default is 0.
	 * @return the FastHenry ending frequency.
	 */
	public static double getFastHenryEndFrequency() { return cacheFastHenryEndFrequency.getDouble(); }
	/**
	 * Method to set the FastHenry ending frequency.
	 * @param e the FastHenry ending frequency.
	 */
	public static void setFastHenryEndFrequency(double e) { cacheFastHenryEndFrequency.setDouble(e); }

	private static Pref cacheFastHenryRunsPerDecade = Pref.makeIntPref("FastHenryRunsPerDecade", Simulation.tool.prefs, 1);
	/**
	 * Method to return the number of runs per decade for FastHenry deck generation.
	 * The default is 1.
	 * @return the number of runs per decade for FastHenry deck generation.
	 */
	public static int getFastHenryRunsPerDecade() { return cacheFastHenryRunsPerDecade.getInt(); }
	/**
	 * Method to set the number of runs per decade for FastHenry deck generation.
	 * @param r the number of runs per decade for FastHenry deck generation.
	 */
	public static void setFastHenryRunsPerDecade(int r) { cacheFastHenryRunsPerDecade.setInt(r); }
	
	private static Pref cacheFastHenryMultiPole = Pref.makeBooleanPref("FastHenryMultiPole", Simulation.tool.prefs, false);
	/**
	 * Method to tell whether FastHenry deck generation should make a multipole subcircuit.
	 * The default is false.
	 * @return true if FastHenry deck generation should make a multipole subcircuit.
	 */
	public static boolean isFastHenryMultiPole() { return cacheFastHenryMultiPole.getBoolean(); }
	/**
	 * Method to set whether FastHenry deck generation should make a multipole subcircuit.
	 * @param mp true if FastHenry deck generation should make a multipole subcircuit.
	 */
	public static void setFastHenryMultiPole(boolean mp) { cacheFastHenryMultiPole.setBoolean(mp); }

	private static Pref cacheFastHenryNumPoles = Pref.makeIntPref("FastHenryNumPoles", Simulation.tool.prefs, 20);
	/**
	 * Method to return the number of poles for FastHenry deck generation.
	 * The default is 20.
	 * @return the number of poles for FastHenry deck generation.
	 */
	public static int getFastHenryNumPoles() { return cacheFastHenryNumPoles.getInt(); }
	/**
	 * Method to set the number of poles for FastHenry deck generation.
	 * @param p the number of poles for FastHenry deck generation.
	 */
	public static void setFastHenryNumPoles(int p) { cacheFastHenryNumPoles.setInt(p); }

	private static Pref cacheFastHenryDefThickness = Pref.makeDoublePref("FastHenryDefThickness", Simulation.tool.prefs, 2);
	/**
	 * Method to return the FastHenry default wire thickness.
	 * The default is 2.
	 * @return the FastHenry default wire thickness.
	 */
	public static double getFastHenryDefThickness() { return cacheFastHenryDefThickness.getDouble(); }
	/**
	 * Method to set the FastHenry default wire thickness.
	 * @param t the FastHenry default wire thickness.
	 */
	public static void setFastHenryDefThickness(double t) { cacheFastHenryDefThickness.setDouble(t); }

	private static Pref cacheFastHenryWidthSubdivisions = Pref.makeIntPref("FastHenryWidthSubdivisions", Simulation.tool.prefs, 1);
	/**
	 * Method to return the default number of width subdivisions for FastHenry deck generation.
	 * The default is 1.
	 * @return the default number of width subdivisions for FastHenry deck generation.
	 */
	public static int getFastHenryWidthSubdivisions() { return cacheFastHenryWidthSubdivisions.getInt(); }
	/**
	 * Method to set the default number of width subdivisions for FastHenry deck generation.
	 * @param w the default number of width subdivisions for FastHenry deck generation.
	 */
	public static void setFastHenryWidthSubdivisions(int w) { cacheFastHenryWidthSubdivisions.setInt(w); }

	private static Pref cacheFastHenryHeightSubdivisions = Pref.makeIntPref("FastHenryHeightSubdivisions", Simulation.tool.prefs, 1);
	/**
	 * Method to return the default number of height subdivisions for FastHenry deck generation.
	 * The default is 1.
	 * @return the default number of height subdivisions for FastHenry deck generation.
	 */
	public static int getFastHenryHeightSubdivisions() { return cacheFastHenryHeightSubdivisions.getInt(); }
	/**
	 * Method to set the default number of height subdivisions for FastHenry deck generation.
	 * @param h the default number of height subdivisions for FastHenry deck generation.
	 */
	public static void setFastHenryHeightSubdivisions(int h) { cacheFastHenryHeightSubdivisions.setInt(h); }

	private static Pref cacheFastHenryMaxSegLength = Pref.makeDoublePref("FastHenryMaxSegLength", Simulation.tool.prefs, 0);
	/**
	 * Method to return the maximum segment length for FastHenry deck generation.
	 * The default is 0.
	 * @return the maximum segment length for FastHenry deck generation.
	 */
	public static double getFastHenryMaxSegLength() { return cacheFastHenryMaxSegLength.getDouble(); }
	/**
	 * Method to set the maximum segment length for FastHenry deck generation.
	 * @param s the maximum segment length for FastHenry deck generation.
	 */
	public static void setFastHenryMaxSegLength(double s) { cacheFastHenryMaxSegLength.setDouble(s); }

	/****************************** VERILOG OPTIONS ******************************/

	private static Pref cacheVerilogUseAssign = Pref.makeBooleanPref("VerilogUseAssign", Simulation.tool.prefs, false);
    static { cacheVerilogUseAssign.attachToObject(Simulation.tool, "Tools/Verilog tab", "Verilog uses Assign construct"); }
	/**
	 * Method to tell whether Verilog deck generation should use the Assign statement.
	 * The default is false.
	 * @return true if Verilog deck generation should use the Assign statement.
	 */
	public static boolean getVerilogUseAssign() { return cacheVerilogUseAssign.getBoolean(); }
	/**
	 * Method to set whether Verilog deck generation should use the Assign statement.
	 * @param use true if Verilog deck generation should use the Assign statement.
	 */
	public static void setVerilogUseAssign(boolean use) { cacheVerilogUseAssign.setBoolean(use); }

	private static Pref cacheVerilogUseTrireg = Pref.makeBooleanPref("VerilogUseTrireg", Simulation.tool.prefs, false);
    static { cacheVerilogUseTrireg.attachToObject(Simulation.tool, "Tools/Verilog tab", "Verilog presumes wire is Trireg"); }
	/**
	 * Method to tell whether Verilog deck generation should use Trireg by default.
	 * The alternative is to use the "wire" statement.
	 * The default is false.
	 * @return true if Verilog deck generation should use Trireg by default.
	 */
	public static boolean getVerilogUseTrireg() { return cacheVerilogUseTrireg.getBoolean(); }
	/**
	 * Method to set whether Verilog deck generation should use Trireg by default.
	 * The alternative is to use the "wire" statement.
	 * @param use true if Verilog deck generation should use Trireg by default.
	 */
	public static void setVerilogUseTrireg(boolean use) { cacheVerilogUseTrireg.setBoolean(use); }

	/****************************** CDL OPTIONS ******************************/

	private static Pref cacheCDLLibName = Pref.makeStringPref("CDLLibName", Simulation.tool.prefs, "");
//    static { cacheCDLLibName.attachToObject(Simulation.tool, "IO/CDL tab", "Cadence library name"); }
	/**
	 * Method to return the CDL library name.
	 * CDL is a weak form of a Spice deck, and it includes a Cadence library name.
	 * @return the CDL library name.
	 */
	public static String getCDLLibName() { return cacheCDLLibName.getString(); }
	/**
	 * Method to set the CDL library name.
	 * CDL is a weak form of a Spice deck, and it includes a Cadence library name.
	 * @param libName the CDL library name.
	 */
	public static void setCDLLibName(String libName) { cacheCDLLibName.setString(libName); }

	private static Pref cacheCDLLibPath = Pref.makeStringPref("CDLLibPath", Simulation.tool.prefs, "");
//    static { cacheCDLLibPath.attachToObject(Simulation.tool, "IO/CDL tab", "Cadence library path"); }
	/**
	 * Method to return the CDL library path.
	 * CDL is a weak form of a Spice deck, and it includes a Cadence library.
	 * @return the CDL library path.
	 */
	public static String getCDLLibPath() { return cacheCDLLibPath.getString(); }
	/**
	 * Method to set the CDL library path.
	 * CDL is a weak form of a Spice deck, and it includes a Cadence library.
	 * @param libName the CDL library path.
	 */
	public static void setCDLLibPath(String libName) { cacheCDLLibPath.setString(libName); }

	private static Pref cacheCDLConvertBrackets = Pref.makeBooleanPref("CDLConvertBrackets", Simulation.tool.prefs, false);
//    static { cacheCDLConvertBrackets.attachToObject(Simulation.tool, "IO/CDL tab", "CDL converts brackets"); }
	/**
	 * Method to tell whether CDL converts square bracket characters.
	 * CDL is a weak form of a Spice deck, and it includes a Cadence library.
	 * @return true if CDL converts square bracket characters.
	 */
	public static boolean isCDLConvertBrackets() { return cacheCDLConvertBrackets.getBoolean(); }
	/**
	 * Method to set if CDL converts square bracket characters.
	 * CDL is a weak form of a Spice deck, and it includes a Cadence library.
	 * @param c true if CDL converts square bracket characters.
	 */
	public static void setCDLConvertBrackets(boolean c) { cacheCDLConvertBrackets.setBoolean(c); }

    private static Pref cacheCDLIgnoreResistors = Pref.makeBooleanPref("CDLLIgnoreResistors", Simulation.tool.prefs, true);
//    static { cacheCDLLibName.attachToObject(Simulation.tool, "IO/CDL tab", "Cadence library name"); }
    /**
     * Method to get the state of "ignore resistors" for netlisting
     * @return the state of "ignore resistors"
     */
    public static boolean getCDLIgnoreResistors() { return cacheCDLIgnoreResistors.getBoolean(); }
    /**
     * Method to get the state of "ignore resistors" for netlisting
     * @param b the state of "ignore resistors"
     */
    public static void setCDLIgnoreResistors(boolean b) { cacheCDLIgnoreResistors.setBoolean(b); }

	/****************************** IRSIM OPTIONS ******************************/

	private static Pref cacheIRSIMResimulateEach = Pref.makeBooleanPref("IRSIMResimulateEach", Simulation.tool.prefs, false);
	/**
	 * Method to tell whether IRSIM resimulates after each change to the display.
	 * When false, the user must request resimulation after a set of changes is done.
	 * @return true if IRSIM resimulates after each change to the display.
	 */
	public static boolean isIRSIMResimulateEach() { return cacheIRSIMResimulateEach.getBoolean(); }
	/**
	 * Method to set if IRSIM resimulates after each change to the display.
	 * When false, the user must request resimulation after a set of changes is done.
	 * @param r true if IRSIM resimulates after each change to the display.
	 */
	public static void setIRSIMResimulateEach(boolean r) { cacheIRSIMResimulateEach.setBoolean(r); }

	private static Pref cacheIRSIMAutoAdvance = Pref.makeBooleanPref("IRSIMAutoAdvance", Simulation.tool.prefs, false);
	/**
	 * Method to tell whether IRSIM automatically advances the time cursor when stimuli are added.
	 * @return true if IRSIM automatically advances the time cursor when stimuli are added.
	 */
	public static boolean isIRSIMAutoAdvance() { return cacheIRSIMAutoAdvance.getBoolean(); }
	/**
	 * Method to set if IRSIM automatically advances the time cursor when stimuli are added.
	 * @param r true if IRSIM automatically advances the time cursor when stimuli are added.
	 */
	public static void setIRSIMAutoAdvance(boolean r) { cacheIRSIMAutoAdvance.setBoolean(r); }

	private static Pref cacheIRSIMShowsCommands = Pref.makeBooleanPref("IRSIMShowsCommands", Simulation.tool.prefs, false);
	/**
	 * Method to tell whether IRSIM shows commands that are issued (for debugging).
	 * @return true if IRSIM shows commands that are issued (for debugging).
	 */
	public static boolean isIRSIMShowsCommands() { return cacheIRSIMShowsCommands.getBoolean(); }
	/**
	 * Method to set if IRSIM shows commands that are issued (for debugging).
	 * @param r true if IRSIM shows commands that are issued (for debugging).
	 */
	public static void setIRSIMShowsCommands(boolean r) { cacheIRSIMShowsCommands.setBoolean(r); }

	private static Pref cacheIRSIMParasiticLevel = Pref.makeIntPref("IRSIMParasiticLevel", Simulation.tool.prefs, 0);
	/**
	 * Method to tell the parasitic level for IRSIM extraction.
	 * Values are 0: quick, 1: local, 2: full (hierarchical).
	 * @return the parasitic level for IRSIM extraction.
	 */
	public static int getIRSIMParasiticLevel() { return cacheIRSIMParasiticLevel.getInt(); }
	/**
	 * Method to set the parasitic level for IRSIM extraction.
	 * Values are 0: quick, 1: local, 2: full (hierarchical).
	 * @param p the parasitic level for IRSIM extraction.
	 */
	public static void setIRSIMParasiticLevel(int p) { cacheIRSIMParasiticLevel.setInt(p); }

	private static Pref cacheIRSIMDebugging = Pref.makeIntPref("IRSIMDebugging", Simulation.tool.prefs, 0);
	/**
	 * Method to tell the debugging level for IRSIM simulation.
	 * This is a combination of bits, where:
	 * Bit 1: debug event scheduling (Sim.DEBUG_EV)
	 * Bit 2: debug final value computation (Sim.DEBUG_DC)
	 * Bit 3: debug tau/delay computation (Sim.DEBUG_TAU)
	 * Bit 4: debug taup computation (Sim.DEBUG_TAUP)
	 * Bit 5: debug spike analysis (Sim.DEBUG_SPK)
	 * Bit 6: debug tree walk (Sim.DEBUG_TW)
	 * @return the debugging level for IRSIM simulation.
	 */
	public static int getIRSIMDebugging() { return cacheIRSIMDebugging.getInt(); }
	/**
	 * Method to set the debugging level for IRSIM simulation.
	 * This is a combination of bits, where:
	 * Bit 1: debug event scheduling (Sim.DEBUG_EV)
	 * Bit 2: debug final value computation (Sim.DEBUG_DC)
	 * Bit 3: debug tau/delay computation (Sim.DEBUG_TAU)
	 * Bit 4: debug taup computation (Sim.DEBUG_TAUP)
	 * Bit 5: debug spike analysis (Sim.DEBUG_SPK)
	 * Bit 6: debug tree walk (Sim.DEBUG_TW)
	 * @param p the debugging level for IRSIM simulation.
	 */
	public static void setIRSIMDebugging(int p) { cacheIRSIMDebugging.setInt(p); }

	private static Pref cacheIRSIMParameterFile = Pref.makeStringPref("IRSIMParameterFile", Simulation.tool.prefs, "scmos0.3.prm");
	/**
	 * Method to tell the parameter file to use for IRSIM.
	 * @return the parameter file to use for IRSIM.
	 */
	public static String getIRSIMParameterFile() { return cacheIRSIMParameterFile.getString(); }
	/**
	 * Method to set the parameter file to use for IRSIM.
	 * @param p the parameter file to use for IRSIM.
	 */
	public static void setIRSIMParameterFile(String p) { cacheIRSIMParameterFile.setString(p); }

	private static Pref cacheIRSIMStepModel = Pref.makeStringPref("IRSIMStepModel", Simulation.tool.prefs, "RC");
	/**
	 * Method to tell the stepping model to use for IRSIM.
	 * Possible choices are "RC" (the default) and "Linear".
	 * @return the stepping model to use for IRSIM.
	 */
	public static String getIRSIMStepModel() { return cacheIRSIMStepModel.getString(); }
	/**
	 * Method to set the stepping model to use for IRSIM.
	 * Possible choices are "RC" (the default) and "Linear".
	 * @param m the stepping model to use for IRSIM.
	 */
	public static void setIRSIMStepModel(String m) { cacheIRSIMStepModel.setString(m); }

    private static Pref cacheIRSIMDelayedX = Pref.makeBooleanPref("IRSIMDelayedX", Simulation.tool.prefs, true);
    /**
     * Get whether or not IRSIM uses a delayed X model, versus the old fast-propogating X model
     * @return true if using the delayed X model, false if using the old fast-propogating X model
     */
    public static boolean isIRSIMDelayedX() { return cacheIRSIMDelayedX.getBoolean(); }
    /**
     * Get whether or not IRSIM uses a delayed X model, versus the old fast-propogating X model
     * @param b true to use the delayed X model, false to use the old fast-propogating X model
     */
    public static void setIRSIMDelayedX(boolean b) { cacheIRSIMDelayedX.setBoolean(b); }

	/****************************** SPICE OPTIONS ******************************/

	/** Spice 2 engine. */		public static final int SPICE_ENGINE_2 = 0;
	/** Spice 3 engine. */		public static final int SPICE_ENGINE_3 = 1;
	/** HSpice engine. */		public static final int SPICE_ENGINE_H = 2;
	/** PSpice engine. */		public static final int SPICE_ENGINE_P = 3;
	/** GNUCap engine. */		public static final int SPICE_ENGINE_G = 4;
	/** SmartSpice engine. */	public static final int SPICE_ENGINE_S = 5;

	private static Pref cacheSpiceEngine = Pref.makeIntPref("SpiceEngine", Simulation.tool.prefs, 1);
//	static
//	{
//		Pref.Meaning m = cacheSpiceEngine.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice engine");
//		m.setTrueMeaning(new String[] {"Spice 2", "Spice 3", "HSpice", "PSpice", "GNUCap", "SmartSpice"});
//	}
	/**
	 * Method to tell which SPICE engine is being used.
	 * Since different versions of SPICE have slightly different syntax,
	 * this is needed to tell the deck generator which variation to target.
	 * @return which SPICE engine is being used.
	 * These constants are available: <BR>
	 * Simulation.SPICE_ENGINE_2 for Spice 2.<BR>
	 * Simulation.SPICE_ENGINE_3 for Spice 3.<BR>
	 * Simulation.SPICE_ENGINE_H for HSpice.<BR>
	 * Simulation.SPICE_ENGINE_P for PSpice.<BR>
	 * Simulation.SPICE_ENGINE_G for GNUCap.<BR>
	 * Simulation.SPICE_ENGINE_S for Smart Spice.
	 */
	public static int getSpiceEngine() { return cacheSpiceEngine.getInt(); }
	/**
	 * Method to set which SPICE engine is being used.
	 * Since different versions of SPICE have slightly different syntax,
	 * this is needed to tell the deck generator which variation to target.
	 * @param engine which SPICE engine is being used.
	 * These constants are available: <BR>
	 * Simulation.SPICE_ENGINE_2 for Spice 2.<BR>
	 * Simulation.SPICE_ENGINE_3 for Spice 3.<BR>
	 * Simulation.SPICE_ENGINE_H for HSpice.<BR>
	 * Simulation.SPICE_ENGINE_P for PSpice.<BR>
	 * Simulation.SPICE_ENGINE_G for GNUCap.<BR>
	 * Simulation.SPICE_ENGINE_S for Smart Spice.
	 */
	public static void setSpiceEngine(int engine) { cacheSpiceEngine.setInt(engine); }

	private static Pref cacheSpiceLevel = Pref.makeStringPref("SpiceLevel", Simulation.tool.prefs, "1");
//    static { cacheSpiceLevel.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice level"); }
	/**
	 * Method to tell which SPICE level is being used.
	 * SPICE can use 3 different levels of simulation.
	 * @return which SPICE level is being used (1, 2, or 3).
	 */
	public static String getSpiceLevel() { return cacheSpiceLevel.getString(); }
	/**
	 * Method to set which SPICE level is being used.
	 * SPICE can use 3 different levels of simulation.
	 * @param level which SPICE level is being used (1, 2, or 3).
	 */
	public static void setSpiceLevel(String level) { cacheSpiceLevel.setString(level); }

	private static Pref cacheSpiceOutputFormat = Pref.makeStringPref("SpiceOutputFormat", Simulation.tool.prefs, "Standard");
//    static { cacheSpiceOutputFormat.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice output format"); }
	/**
	 * Method to tell the type of output files expected from Spice.
	 * @return the type of output files expected from Spice.
	 * The values are:<BR>
	 * "Standard": Standard output (the default)<BR>
	 * "Raw" Raw output<BR>
	 * "Raw/Smart": Raw output from SmartSpice<BR>
	 */
	public static String getSpiceOutputFormat() { return cacheSpiceOutputFormat.getString(); }
	/**
	 * Method to set the type of output files expected from Spice.
	 * @param format the type of output files expected from Spice.
	 * The values are:<BR>
	 * "Standard": Standard output (the default)<BR>
	 * "Raw" Raw output<BR>
	 * "Raw/Smart": Raw output from SmartSpice<BR>
	 */
	public static void setSpiceOutputFormat(String format) { cacheSpiceOutputFormat.setString(format); }

    public static final String spiceRunChoiceDontRun = "Don't Run";
    public static final String spiceRunChoiceRunIgnoreOutput = "Run, Ingore Output";
    public static final String spiceRunChoiceRunReportOutput = "Run, Report Output";
    private static final String [] spiceRunChoices = {spiceRunChoiceDontRun, spiceRunChoiceRunIgnoreOutput, spiceRunChoiceRunReportOutput};

    private static Pref cacheSpiceRunChoice = Pref.makeIntPref("SpiceRunChoice", Simulation.tool.prefs, 0);
//    static {
//        Pref.Meaning m = cacheSpiceRunChoice.attachToObject(Simulation.tool, "Tool Options, Spice tab", "Spice Run Choice");
//        m.setTrueMeaning(new String[] {spiceRunChoiceDontRun, spiceRunChoiceRunIgnoreOutput, spiceRunChoiceRunReportOutput});
//    }
    /** Determines possible settings for the Spice Run Choice */
    public static String [] getSpiceRunChoiceValues() { return spiceRunChoices; }
    /** Get the current setting for the Spice Run Choice preference */
    public static String getSpiceRunChoice() { return spiceRunChoices[cacheSpiceRunChoice.getInt()]; }
    /** Set the setting for the Spice Run Choice preference. Ignored if invalid */
    public static void setSpiceRunChoice(String choice) {
        String [] values = getSpiceRunChoiceValues();
        for (int i=0; i<values.length; i++) {
            if (values[i].equals(choice)) { cacheSpiceRunChoice.setInt(i); return; }
        }
    }

    private static Pref cacheSpiceRunDir = Pref.makeStringPref("SpiceRunDir", Simulation.tool.prefs, "");
//    static { cacheSpiceRunDir.attachToObject(Simulation.tool, "Tool Options, Spice tab", "Spice Run Dir"); }
    /** Get the spice run directory */
    public static String getSpiceRunDir() { return cacheSpiceRunDir.getString(); }
    /** Set the spice run directory */
    public static void setSpiceRunDir(String dir) { cacheSpiceRunDir.setString(dir); }

    private static Pref cacheSpiceUseRunDir = Pref.makeBooleanPref("SpiceUseRunDir", Simulation.tool.prefs, false);
//    static { cacheSpiceUseRunDir.attachToObject(Simulation.tool, "Tool Options, Spice tab", "Use Run Dir"); }
    /** Get whether or not to use the user-specified spice run dir */
    public static boolean getSpiceUseRunDir() { return cacheSpiceUseRunDir.getBoolean(); }
    /** Set whether or not to use the user-specified spice run dir */
    public static void setSpiceUseRunDir(boolean b) { cacheSpiceUseRunDir.setBoolean(b); }

    private static Pref cacheSpiceOutputOverwrite = Pref.makeBooleanPref("SpiceOverwriteOutputFile", Simulation.tool.prefs, false);
//    static { cacheSpiceOutputOverwrite.attachToObject(Simulation.tool, "Tool Options, Spice tab", "Overwrite Output Spice File"); }
    /** Get whether or not we automatically overwrite the spice output file */
    public static boolean getSpiceOutputOverwrite() { return cacheSpiceOutputOverwrite.getBoolean(); }
    /** Set whether or not we automatically overwrite the spice output file */
    public static void setSpiceOutputOverwrite(boolean b) { cacheSpiceOutputOverwrite.setBoolean(b); }

    private static Pref cacheSpiceRunProbe = Pref.makeBooleanPref("SpiceRunProbe", Simulation.tool.prefs, false);
    /** Get whether or not to run the spice probe after running spice */
    public static boolean getSpiceRunProbe() { return cacheSpiceRunProbe.getBoolean(); }
    /** Set whether or not to run the spice probe after running spice */
    public static void setSpiceRunProbe(boolean b) { cacheSpiceRunProbe.setBoolean(b); }

    private static Pref cacheSpiceRunProgram = Pref.makeStringPref("SpiceRunProgram", Simulation.tool.prefs, "");
//    static { cacheSpiceRunProgram.attachToObject(Simulation.tool, "Tool Options, Spice tab", "Spice Run Program"); }
    /** Get the spice run program */
    public static String getSpiceRunProgram() { return cacheSpiceRunProgram.getString(); }
    /** Set the spice run program */
    public static void setSpiceRunProgram(String c) { cacheSpiceRunProgram.setString(c); }

    private static Pref cacheSpiceRunProgramArgs = Pref.makeStringPref("SpiceRunProgramArgs", Simulation.tool.prefs, "");
//    static { cacheSpiceRunProgramArgs.attachToObject(Simulation.tool, "Tool Options, Spice tab", "Spice Run Program Args"); }
    /** Get the spice run program args */
    public static String getSpiceRunProgramArgs() { return cacheSpiceRunProgramArgs.getString(); }
    /** Set the spice run program args */
    public static void setSpiceRunProgramArgs(String c) { cacheSpiceRunProgramArgs.setString(c); }

	private static Pref cacheSpicePartsLibrary = null;
	/**
	 * Method to return the name of the current Spice parts library.
	 * The Spice parts library is a library of icons that are used in Spice.
	 * @return the name of the current Spice parts library.
	 */
	public static String getSpicePartsLibrary()
	{
		if (cacheSpicePartsLibrary == null)
		{
			String [] libNames = LibFile.getSpicePartsLibraries();
			cacheSpicePartsLibrary = Pref.makeStringPref("SpicePartsLibrary", Simulation.tool.prefs, libNames[0]);
		}
		return cacheSpicePartsLibrary.getString();
	}
	/**
	 * Method to set the name of the current Spice parts library.
	 * The Spice parts library is a library of icons that are used in Spice.
	 * @param parts the name of the new current Spice parts library.
	 */
	public static void setSpicePartsLibrary(String parts)
	{
		if (cacheSpicePartsLibrary == null)
		{
			String [] libNames = LibFile.getSpicePartsLibraries();
			cacheSpicePartsLibrary = Pref.makeStringPref("SpicePartsLibrary", Simulation.tool.prefs, libNames[0]);
		}
		cacheSpicePartsLibrary.setString(parts);
	}

	private static Pref cacheSpiceHeaderCardInfo = Pref.makeStringPref("SpiceHeaderCardInfo", Simulation.tool.prefs, "");
//    static { cacheSpiceHeaderCardInfo.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice header card information"); }
	/**
	 * Method to get the Spice header card specification.
	 * Header cards can come from one of three places, depending on the specification:<BR>
	 * Specification="" means use built-in header cards.<BR>
	 * Specification="Extension XXX" means use header cards from the file TOPCELL.XXX
	 * where TOPCELL is the name of the top-level cell name and XXX is a specified extension.<BR>
	 * Specification="XXX" means use header cards from the file XXX.
	 * @return the Spice header card specification.
	 */
	public static String getSpiceHeaderCardInfo() { return cacheSpiceHeaderCardInfo.getString(); }
	/**
	 * Method to set the Spice header card specification.
	 * Header cards can come from one of three places, depending on the specification:<BR>
	 * Specification="" means use built-in header cards.<BR>
	 * Specification="Extension XXX" means use header cards from the file TOPCELL.XXX
	 * where TOPCELL is the name of the top-level cell name and XXX is a specified extension.<BR>
	 * Specification="XXX" means use header cards from the file XXX.
	 * @param spec the Spice header card specification.
	 */
	public static void setSpiceHeaderCardInfo(String spec) { cacheSpiceHeaderCardInfo.setString(spec); }

	private static Pref cacheSpiceTrailerCardInfo = Pref.makeStringPref("SpiceTrailerCardInfo", Simulation.tool.prefs, "");
//    static { cacheSpiceTrailerCardInfo.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice trailer card information"); }
	/**
	 * Method to get the Spice trailer card specification.
	 * Trailer cards can come from one of three places, depending on the specification:<BR>
	 * Specification="" means use built-in trailer cards.<BR>
	 * Specification="Extension XXX" means use trailer cards from the file TOPCELL.XXX
	 * where TOPCELL is the name of the top-level cell name and XXX is a specified extension.<BR>
	 * Specification="XXX" means use trailer cards from the file XXX.
	 * @return the Spice trailer card specification.
	 */
	public static String getSpiceTrailerCardInfo() { return cacheSpiceTrailerCardInfo.getString(); }
	/**
	 * Method to set the Spice trailer card specification.
	 * Trailer cards can come from one of three places, depending on the specification:<BR>
	 * Specification="" means use built-in trailer cards.<BR>
	 * Specification="Extension XXX" means use trailer cards from the file TOPCELL.XXX
	 * where TOPCELL is the name of the top-level cell name and XXX is a specified extension.<BR>
	 * Specification="XXX" means use trailer cards from the file XXX.
	 * @param spec the Spice trailer card specification.
	 */
	public static void setSpiceTrailerCardInfo(String spec) { cacheSpiceTrailerCardInfo.setString(spec); }

	private static Pref cacheSpiceUseParasitics = Pref.makeBooleanPref("SpiceUseParasitics", Simulation.tool.prefs, true);
//    static { cacheSpiceUseParasitics.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice uses parasitics"); }
	/**
	 * Method to tell whether or not to use parasitics in Spice output.
	 * The default is true.
	 * @return true to use parasitics in Spice output.
	 */
	public static boolean isSpiceUseParasitics() { return cacheSpiceUseParasitics.getBoolean(); }
	/**
	 * Method to set whether or not to use parasitics in Spice output.
	 * @param p true to use parasitics in Spice output.
	 */
	public static void setSpiceUseParasitics(boolean p) { cacheSpiceUseParasitics.setBoolean(p); }

	private static Pref cacheSpiceUseNodeNames = Pref.makeBooleanPref("SpiceUseNodeNames", Simulation.tool.prefs, true);
//    static { cacheSpiceUseNodeNames.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice uses node names"); }
	/**
	 * Method to tell whether or not to use node names in Spice output.
	 * If node names are off, then numbers are used.
	 * The default is true.
	 * @return true to use node names in Spice output.
	 */
	public static boolean isSpiceUseNodeNames() { return cacheSpiceUseNodeNames.getBoolean(); }
	/**
	 * Method to set whether or not to use node names in Spice output.
	 * If node names are off, then numbers are used.
	 * @param u true to use node names in Spice output.
	 */
	public static void setSpiceUseNodeNames(boolean u) { cacheSpiceUseNodeNames.setBoolean(u); }

	private static Pref cacheSpiceForceGlobalPwrGnd = Pref.makeBooleanPref("SpiceForceGlobalPwrGnd", Simulation.tool.prefs, false);
//    static { cacheSpiceForceGlobalPwrGnd.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice forces global VDD/GND"); }
	/**
	 * Method to tell whether or not to write global power and ground in Spice output.
	 * If this is off, then individual power and ground references are made.
	 * The default is false.
	 * @return true to write global power and ground in Spice output.
	 */
	public static boolean isSpiceForceGlobalPwrGnd() { return cacheSpiceForceGlobalPwrGnd.getBoolean(); }
	/**
	 * Method to set whether or not to write global power and ground in Spice output.
	 * If this is off, then individual power and ground references are made.
	 * @param g true to write global power and ground in Spice output.
	 */
	public static void setSpiceForceGlobalPwrGnd(boolean g) { cacheSpiceForceGlobalPwrGnd.setBoolean(g); }

	private static Pref cacheSpiceUseCellParameters = Pref.makeBooleanPref("SpiceUseCellParameters", Simulation.tool.prefs, false);
//    static { cacheSpiceForceGlobalPwrGnd.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice uses cell parameters"); }
	/**
	 * Method to tell whether or not to use cell parameters in Spice output.
	 * When cell parameters are used, any parameterized cell is written many times,
	 * once for each combination of parameter values.
	 * The default is false.
	 * @return true to use cell parameters in Spice output.
	 */
	public static boolean isSpiceUseCellParameters() { return cacheSpiceUseCellParameters.getBoolean(); }
	/**
	 * Method to set whether or not to use cell parameters in Spice output.
	 * When cell parameters are used, any parameterized cell is written many times,
	 * once for each combination of parameter values.
	 * @param p true to use cell parameters in Spice output.
	 */
	public static void setSpiceUseCellParameters(boolean p) { cacheSpiceUseCellParameters.setBoolean(p); }

	private static Pref cacheSpiceWriteTransSizeInLambda = Pref.makeBooleanPref("SpiceWriteTransSizeInLambda", Simulation.tool.prefs, false);
//    static { cacheSpiceWriteTransSizeInLambda.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice writes transistor sizes in lambda"); }
	/**
	 * Method to tell whether or not to write transistor sizes in "lambda" grid units in Spice output.
	 * Lambda grid units are the basic units of design.
	 * When writing in these units, the values are simpler, but an overriding scale factor brings them to the proper size.
	 * The default is false.
	 * @return true to write transistor sizes in "lambda" grid units in Spice output.
	 */
	public static boolean isSpiceWriteTransSizeInLambda() { return cacheSpiceWriteTransSizeInLambda.getBoolean(); }
	/**
	 * Method to set whether or not to write transistor sizes in "lambda" grid units in Spice output.
	 * Lambda grid units are the basic units of design.
	 * When writing in these units, the values are simpler, but an overriding scale factor brings them to the proper size.
	 * @param l true to write transistor sizes in "lambda" grid units in Spice output.
	 */
	public static void setSpiceWriteTransSizeInLambda(boolean l) { cacheSpiceWriteTransSizeInLambda.setBoolean(l); }
}
