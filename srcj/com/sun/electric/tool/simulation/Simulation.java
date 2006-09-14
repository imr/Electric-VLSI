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

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.lib.LibFile;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.io.output.Verilog;
import com.sun.electric.tool.simulation.als.ALS;
import com.sun.electric.tool.user.CompileVHDL;
import com.sun.electric.tool.user.GenerateVHDL;
import com.sun.electric.tool.user.projectSettings.ProjSettingsNode;
import com.sun.electric.tool.user.projectSettings.ProjSettings;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.TextWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.waveform.Panel;
import com.sun.electric.tool.user.waveform.WaveSignal;
import com.sun.electric.tool.user.waveform.WaveformWindow;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

/**
 * This is the Simulation Interface tool.
 */
public class Simulation extends Tool
{
	/** the Simulation tool. */		private static Simulation tool = new Simulation();

	/** key of Variable holding rise time. */				public static final Variable.Key RISE_DELAY_KEY = Variable.newKey("SIM_rise_delay");
	/** key of Variable holding fall time. */				public static final Variable.Key FALL_DELAY_KEY = Variable.newKey("SIM_fall_delay");
	/** key of Variable holding flag for weak nodes. */		public static final Variable.Key WEAK_NODE_KEY = Variable.newKey("SIM_weak_node");
	/** key of Variable holding "M" factors. */				public static final Variable.Key M_FACTOR_KEY = Variable.newKey("ATTR_M");

	/** constant for ALS simulation */						public static final int ALS_ENGINE = 0;
	/** constant for IRSIM simulation */					public static final int IRSIM_ENGINE = 1;

	private static boolean irsimChecked = false;
	private static Class irsimClass = null;
	private static Method irsimSimulateMethod;

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
     * Method to retrieve the singleton associated with the Simulation tool.
     * @return the Simulation tool.
     */
    public static Simulation getSimulationTool() { return tool; }

	/****************************** CONTROL OF SIMULATION ENGINES ******************************/

	private static final int CONVERT_TO_VHDL      =  1;
	private static final int COMPILE_VHDL_FOR_SIM =  4;

	/**
	 * Method to invoke a simulation engine.
	 * @param engine the simulation engine to run.
	 * @param forceDeck true to force simulation from a user-specified netlist file.
	 * @param prevCell the previous cell being simulated (null for a new simulation).
	 * @param prevEngine the previous simulation engine running (null for a new simulation).
	 */
	public static void startSimulation(int engine, boolean forceDeck, Cell prevCell, Engine prevEngine)
	{
    	Cell cell = null;
        VarContext context = null;
    	String fileName = null;
		if (prevCell != null) cell = prevCell; else
		{
			UserInterface ui = Job.getUserInterface();
	    	if (forceDeck)
	    	{
	    		fileName = OpenFile.chooseInputFile(FileType.IRSIM, "IRSIM deck to simulate");
	    		if (fileName == null) return;
				cell = ui.getCurrentCell();
	    	} else
	    	{
				cell = ui.needCurrentCell();
		        if (cell == null) return;
	            EditWindow_ wnd = ui.getCurrentEditWindow_();
	            if (wnd != null) context = wnd.getVarContext();
	    	}
		}
		switch (engine)
		{
			case ALS_ENGINE:
				// see if the current cell needs to be compiled
				Cell originalCell = cell;
				int activities = 0;
				if (cell.getView() != View.NETLISTALS)
				{
					if (cell.getView() == View.SCHEMATIC || cell.getView() == View.LAYOUT)
					{
						// current cell is Schematic.  See if there is a more recent netlist or VHDL
						Cell vhdlCell = cell.otherView(View.VHDL);
						if (vhdlCell != null && vhdlCell.getRevisionDate().after(cell.getRevisionDate())) cell = vhdlCell; else
							activities |= CONVERT_TO_VHDL | COMPILE_VHDL_FOR_SIM;
					}
					if (cell.getView() == View.VHDL)
					{
						// current cell is VHDL.  See if there is a more recent netlist
						Cell netListCell = cell.otherView(View.NETLISTQUISC);
						if (netListCell != null && netListCell.getRevisionDate().after(cell.getRevisionDate())) cell = netListCell; else
							activities |= COMPILE_VHDL_FOR_SIM;
					}
				}

				// now schedule the simulation work
				new DoALSActivity(cell, activities, originalCell, prevEngine);
				break;

			case IRSIM_ENGINE:
				if (!hasIRSIM()) return;
				runIRSIM(cell, context, fileName);
				break;
		}
	}

	/**
	 * Class to do the next silicon-compilation activity in a new thread.
	 */
	private static class DoALSActivity extends Job
	{
		private Cell cell, originalCell;
		private int activities;
		private transient Engine prevEngine;
		private List<Cell> textCellsToRedraw;

		private DoALSActivity(Cell cell, int activities, Cell originalCell, Engine prevEngine)
		{
			super("ALS Simulation", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			this.activities = activities;
			this.originalCell = originalCell;
			this.prevEngine = prevEngine;
			startJob();
		}

		public boolean doIt() throws JobException
		{
            Library destLib = cell.getLibrary();
			textCellsToRedraw = new ArrayList<Cell>();
			fieldVariableChanged("textCellsToRedraw");
			if ((activities&CONVERT_TO_VHDL) != 0)
			{
				// convert Schematic to VHDL
				System.out.print("Generating VHDL from '" + cell + "' ...");
				List<String> vhdlStrings = GenerateVHDL.convertCell(cell);
				if (vhdlStrings == null)
				{
					throw new JobException("No VHDL produced");
				}

				String cellName = cell.getName() + "{vhdl}";
				Cell vhdlCell = cell.getLibrary().findNodeProto(cellName);
				if (vhdlCell == null)
				{
					vhdlCell = Cell.makeInstance(cell.getLibrary(), cellName);
					if (vhdlCell == null) return false;
				}
				String [] array = new String[vhdlStrings.size()];
				for(int i=0; i<vhdlStrings.size(); i++) array[i] = (String)vhdlStrings.get(i);
				vhdlCell.setTextViewContents(array);
				textCellsToRedraw.add(vhdlCell);
				System.out.println(" Done, created " + vhdlCell);
				cell = vhdlCell;
				fieldVariableChanged("cell");
			}

			if ((activities&COMPILE_VHDL_FOR_SIM) != 0)
			{
				// compile the VHDL to a netlist
				System.out.print("Compiling VHDL in " + cell + " ...");
				CompileVHDL c = new CompileVHDL(cell);
				if (c.hasErrors())
				{
					throw new JobException("ERRORS during compilation, no netlist produced");
				}
				List<String> netlistStrings = c.getALSNetlist(destLib);
				if (netlistStrings == null)
				{
					throw new JobException("No netlist produced");
				}

				// store the ALS netlist
				String cellName = cell.getName() + "{net.als}";
				Cell netlistCell = cell.getLibrary().findNodeProto(cellName);
				if (netlistCell == null)
				{
					netlistCell = Cell.makeInstance(cell.getLibrary(), cellName);
					if (netlistCell == null) return false;
				}
				String [] array = new String[netlistStrings.size()];
				for(int i=0; i<netlistStrings.size(); i++) array[i] = (String)netlistStrings.get(i);
				netlistCell.setTextViewContents(array);
				textCellsToRedraw.add(netlistCell);
				System.out.println(" Done, created " + netlistCell);
				cell = netlistCell;
				fieldVariableChanged("cell");
			}
			return true;
		}

        public void terminateOK()
        {
            for(Cell cell : textCellsToRedraw) {
                TextWindow.updateText(cell);
            }
			if (prevEngine != null)
			{
				ALS.restartSimulation(cell, originalCell, (ALS)prevEngine);
			} else
			{
				ALS.simulateNetlist(cell, originalCell);
			}
        }
	}

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
	 * Method to run the IRSIM simulator on a given cell, context or file.
	 * Uses reflection to find the IRSIM simulator (if it exists).
	 * @param cell the Cell to simulate.
	 * @param context the context to the cell to simulate.
	 * @param fileName the name of the file with the netlist.  If this is null, simulate the cell.
	 * If this is not null, ignore the cell and simulate the file.
	 */
	public static void runIRSIM(Cell cell, VarContext context, String fileName)
	{
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
	 * Method to update the simulation (because some stimuli have changed).
	 */
	public static void update()
	{
		Engine engine = findEngine();
		if (engine == null) return;
		engine.update();
	}

	/**
	 * Method to set the currently-selected signal high at the current time.
	 */
	public static void setSignalHigh()
	{
		Engine engine = findEngine();
		if (engine == null) return;
		engine.setSignalHigh();
	}

	/**
	 * Method to set the currently-selected signal low at the current time.
	 */
	public static void setSignalLow()
	{
		Engine engine = findEngine();
		if (engine == null) return;
		engine.setSignalLow();
	}

	/**
	 * Method to set the currently-selected signal undefined at the current time.
	 */
	public static void setSignalX()
	{
		Engine engine = findEngine();
		if (engine == null) return;
		engine.setSignalX();
	}

	public static void setClock()
	{
		Engine engine = findEngine();
		if (engine == null) return;

		// prompt for clock information
		double period = ClockSpec.getClockSpec();
		if (period <= 0) return;
		engine.setClock(period);
	}

	/**
	 * Method to show information about the currently-selected signal.
	 */
	public static void showSignalInfo()
	{
		Engine engine = findEngine();
		if (engine == null) return;
		engine.showSignalInfo();
	}

	/**
	 * Method to remove all stimuli from the currently-selected signal.
	 */
	public static void removeStimuliFromSignal()
	{
		Engine engine = findEngine();
		if (engine == null) return;
		engine.removeStimuliFromSignal();
	}

	/**
	 * Method to remove the selected stimuli.
	 */
	public static void removeSelectedStimuli()
	{
		Engine engine = findEngine();
		if (engine == null) return;
		engine.removeSelectedStimuli();
	}

	/**
	 * Method to remove all stimuli from the simulation.
	 */
	public static void removeAllStimuli()
	{
		Engine engine = findEngine();
		if (engine == null) return;
		engine.removeAllStimuli();
	}

	/**
	 * Method to save the current stimuli information to disk.
	 */
	public static void saveStimuli()
	{
		Engine engine = findEngine();
		if (engine == null) return;
		engine.saveStimuli();
	}

	/**
	 * Method to restore the current stimuli information from disk.
	 */
	public static void restoreStimuli()
	{
		Engine engine = findEngine();
		if (engine == null) return;
		engine.restoreStimuli();
	}

	/**
	 * Method to locate the running simulation engine.
	 * @return the Engine that is running.
	 * Prints an error and returns null if there is none.
	 */
	private static Engine findEngine()
	{
		// find a simulation engine to control
		Engine engine = null;
		for(Iterator<WindowFrame> it = WindowFrame.getWindows(); it.hasNext(); )
		{
			WindowFrame wf = it.next();
			if (wf.getContent() instanceof WaveformWindow)
			{
				WaveformWindow ww = (WaveformWindow)wf.getContent();
				Engine e = ww.getSimEngine();
				if (e == null) continue;
				if (wf == WindowFrame.getCurrentWindowFrame()) return e;
				engine = e;
			}
		}
		if (engine == null)
			System.out.println("No simulator is ready to handle the command");
		return engine;
	}

	/****************************** MISCELLANEOUS CONTROLS ******************************/

	/**
	 * Method to set a Spice model on the selected node.
	 */
	public static void setSpiceModel()
	{
		UserInterface ui = Job.getUserInterface();
		EditWindow_ wnd = ui.getCurrentEditWindow_();
        if (wnd == null) return;

		NodeInst ni = (NodeInst)wnd.getOneElectricObject(NodeInst.class);
		if (ni == null) return;
		SetSpiceModel job = new SetSpiceModel(ni);
	}

	/**
	 * Class to set a Spice Model in a new thread.
	 */
	private static class SetSpiceModel extends Job
	{
		private NodeInst ni;

    	protected SetSpiceModel(NodeInst ni)
		{
			super("Set Spice Model", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.ni = ni;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			ni.newDisplayVar(Spice.SPICE_MODEL_KEY, "SPICE-Model");
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
		UserInterface ui = Job.getUserInterface();
		EditWindow_ wnd = ui.getCurrentEditWindow_();
        if (wnd == null) return;

		List<Geometric> list = wnd.getHighlightedEObjs(false, true);
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
		private List<Geometric> list;
		private int type;

    	protected SetWireType(List<Geometric> list, int type)
		{
			super("Change Verilog Wire Types", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.list = list;
			this.type = type;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			for(Geometric geom : list)
			{
				ArcInst ai = (ArcInst)geom;
				switch (type)
				{
					case 0:		// set to "wire"
						ai.newDisplayVar(Verilog.WIRE_TYPE_KEY, "wire");
						break;
					case 1:		// set to "trireg"
						ai.newDisplayVar(Verilog.WIRE_TYPE_KEY, "trireg");
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
		UserInterface ui = Job.getUserInterface();
		EditWindow_ wnd = ui.getCurrentEditWindow_();
        if (wnd == null) return;

		NodeInst ni = (NodeInst)wnd.getOneElectricObject(NodeInst.class);
		if (ni == null) return;
		SetTransistorStrength job = new SetTransistorStrength(ni, weak);
	}

	/**
	 * Class to set transistor strengths in a new thread.
	 */
	private static class SetTransistorStrength extends Job
	{
		private NodeInst ni;
		private boolean weak;

    	protected SetTransistorStrength(NodeInst ni, boolean weak)
		{
			super("Change Transistor Strength", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.ni = ni;
			this.weak = weak;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			if (weak)
			{
				ni.newDisplayVar(WEAK_NODE_KEY, "Weak");
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
		Iterator<Analysis> anIt = sd.getAnalyses();
		if (!anIt.hasNext())
		{
			System.out.println("ERROR: No simulation data found: waveform window not shown");
			return;
		}
		Analysis an = anIt.next();

		// create a waveform window
		WindowFrame wf = WindowFrame.createWaveformWindow(sd);
		ww = (WaveformWindow)wf.getContent();

		// if the data has an associated cell, see if that cell remembers the signals that were in the waveform window
		if (sd.getCell() != null)
		{
			String [] signalNames = WaveformWindow.getSignalOrder(sd.getCell());
			boolean isAnalog = sd.isAnalog();
			boolean showedSomething = false;
			boolean wantUnlockedTime = false;
			Analysis.AnalysisType onlyType = null;
			for(int i=0; i<signalNames.length; i++)
			{
				String signalName = signalNames[i];
				Signal xAxisSignal = null;
				if (signalName.startsWith("\t"))
				{
					// has panel type and X axis information
					int openPos = signalName.indexOf('(');
					int tabPos = signalName.indexOf('\t', 1);
					if (openPos >= 0) tabPos = openPos;
					String analysisName = signalName.substring(1, tabPos);
					Analysis.AnalysisType analysisType = Analysis.AnalysisType.findAnalysisType(analysisName);
					if (analysisType == null) continue;
					an = sd.findAnalysis(analysisType);
					if (an == null) continue;
					if (openPos >= 0)
					{
						int closePos = signalName.indexOf(')');
						String sigName = signalName.substring(openPos+1, closePos);
						xAxisSignal = an.findSignalForNetwork(sigName);
						wantUnlockedTime = true;
					}
				}
				if (onlyType == null) onlyType = an.getAnalysisType();
				if (an.getAnalysisType() != onlyType) wantUnlockedTime = true;
				Panel wp = null;
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
					Signal sSig = an.findSignalForNetwork(sigName);
					if (sSig != null)
					{
						if (firstSignal)
						{
							firstSignal = false;
							wp = new Panel(ww, sd.isAnalog(), an.getAnalysisType());
							if (xAxisSignal != null)
								wp.setXAxisSignal(xAxisSignal);
							wp.makeSelectedPanel();
							showedSomething = true;
						}
						new WaveSignal(wp, sSig);
					}
					if (tabPos < 0) break;
				}
			}
			if (showedSomething)
			{
				if (wantUnlockedTime)
				{
					ww.togglePanelXAxisLock();
					for(Iterator<Panel> it = ww.getPanels(); it.hasNext(); )
					{
						Panel panel = it.next();
						panel.makeSelectedPanel();
						ww.fillScreen();
					}
				} else
				{
					ww.fillScreen();
				}
				return;
			}
		}

        if (an == null) // wrong format?
        {
            System.out.println("ERROR: No simulation data found: waveform window not shown");
			return;
        }

		// nothing saved, so show a default set of signals (if it even exists)
		if (sd.isAnalog())
		{
			Panel wp = new Panel(ww, sd.isAnalog(), an.getAnalysisType());
			Rectangle2D bounds = an.getBounds();
			double lowValue = bounds.getMinY();
			double highValue = bounds.getMaxY();
			wp.setYAxisRange(lowValue, highValue);
			wp.makeSelectedPanel();
		} else
		{
			// put all top-level signals in, up to a limit
			int numSignals = 0;
			List<Signal> allSignals = an.getSignals();
			makeBussedSignals(an);
			for(int i=0; i<allSignals.size(); i++)
			{
				DigitalSignal sDSig = (DigitalSignal)allSignals.get(i);
				if (sDSig.getSignalContext() != null) continue;
				if (sDSig.isInBus()) continue;
				if (sDSig.getSignalName().indexOf('@') >= 0) continue;
				Panel wp = new Panel(ww, sd.isAnalog(), an.getAnalysisType());
				wp.makeSelectedPanel();
				new WaveSignal(wp, sDSig);
				numSignals++;
				if (numSignals > 15) break;
			}
		}
		ww.getPanel().validate();
		ww.fillScreen();
	}

	private static void makeBussedSignals(Analysis an)
	{
		List<Signal> signals = an.getSignals();
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
			DigitalSignal busSig = new DigitalSignal(an);
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

	/**
	 * Class to handle the "Clock specification" dialog.
	 */
	private static class ClockSpec extends EDialog
	{
		private double period = -1;
		private JRadioButton freqBut, periodBut;
		private JTextField freqField, periodField;

		public static double getClockSpec()
		{
			ClockSpec dialog = new ClockSpec(TopLevel.getCurrentJFrame(), true);
			dialog.setVisible(true);
			return dialog.period;
		}

		/** Creates new form Clock specification */
		public ClockSpec(Frame parent, boolean modal)
		{
			super(parent, modal);

	        getContentPane().setLayout(new GridBagLayout());
	        setTitle("Clock Specification");
	        setName("");
	        addWindowListener(new WindowAdapter()
	        {
	            public void windowClosing(WindowEvent evt) { closeDialog(evt); }
	        });
			ButtonGroup fp = new ButtonGroup();

			// the frequency and period section
			freqBut = new JRadioButton("Frequency:");
			GridBagConstraints gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 0;
			gbc.insets = new Insets(4, 4, 4, 4);
			gbc.anchor = GridBagConstraints.WEST;
	        getContentPane().add(freqBut, gbc);
			fp.add(freqBut);

			freqField = new JTextField();
			freqField.setColumns(12);
			gbc = new GridBagConstraints();
			gbc.gridx = 1;   gbc.gridy = 0;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets = new Insets(4, 4, 4, 4);
	        getContentPane().add(freqField, gbc);

			periodBut = new JRadioButton("Period:");
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 1;
			gbc.insets = new Insets(4, 4, 4, 4);
			gbc.anchor = GridBagConstraints.WEST;
	        getContentPane().add(periodBut, gbc);
			fp.add(periodBut);
			periodBut.setSelected(true);

			periodField = new JTextField();
			periodField.setColumns(12);
			periodField.setText("0.00000001");
			gbc = new GridBagConstraints();
			gbc.gridx = 1;   gbc.gridy = 1;
			gbc.fill = GridBagConstraints.HORIZONTAL;
			gbc.insets = new Insets(4, 4, 4, 4);
	        getContentPane().add(periodField, gbc);

			// the OK and Cancel buttons
			JButton cancel = new JButton("Cancel");
	        cancel.addActionListener(new ActionListener()
	        {
	            public void actionPerformed(ActionEvent evt) { cancel(evt); }
	        });
			gbc = new GridBagConstraints();
			gbc.gridx = 0;   gbc.gridy = 2;
			gbc.insets = new Insets(4, 4, 4, 4);
	        getContentPane().add(cancel, gbc);

			JButton ok = new JButton("OK");
	        ok.addActionListener(new ActionListener()
	        {
	            public void actionPerformed(ActionEvent evt) { ok(evt); }
	        });
			gbc = new GridBagConstraints();
			gbc.gridx = 1;   gbc.gridy = 2;
			gbc.insets = new Insets(4, 4, 4, 4);
	        getContentPane().add(ok, gbc);

	        pack();

			getRootPane().setDefaultButton(ok);
			finishInitialization();
		}

		protected void escapePressed() { cancel(null); }

		private void cancel(ActionEvent evt)
		{
			closeDialog(null);
		}

		private void ok(ActionEvent evt)
		{
			if (freqBut.isSelected())
			{
				double freq = TextUtils.atof(freqField.getText());
				if (freq != 0) period = 1.0 / freq;
			} else
			{
				period = TextUtils.atof(periodField.getText());
			}
			closeDialog(null);
		}

		/** Closes the dialog */
		private void closeDialog(WindowEvent evt)
		{
			setVisible(false);
			dispose();
		}
	}

	/****************************** FAST HENRY OPTIONS ******************************/

	private static Pref cacheFastHenryUseSingleFrequency = Pref.makeBooleanPref("FastHenryUseSingleFrequency", tool.prefs, false);
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

	private static Pref cacheFastHenryStartFrequency = Pref.makeDoublePref("FastHenryStartFrequency", tool.prefs, 0);
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

	private static Pref cacheFastHenryEndFrequency = Pref.makeDoublePref("FastHenryEndFrequency", tool.prefs, 0);
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

	private static Pref cacheFastHenryRunsPerDecade = Pref.makeIntPref("FastHenryRunsPerDecade", tool.prefs, 1);
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

	private static Pref cacheFastHenryMultiPole = Pref.makeBooleanPref("FastHenryMultiPole", tool.prefs, false);
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

	private static Pref cacheFastHenryNumPoles = Pref.makeIntPref("FastHenryNumPoles", tool.prefs, 20);
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

	private static Pref cacheFastHenryDefThickness = Pref.makeDoublePref("FastHenryDefThickness", tool.prefs, 2);
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

	private static Pref cacheFastHenryWidthSubdivisions = Pref.makeIntPref("FastHenryWidthSubdivisions", tool.prefs, 1);
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

	private static Pref cacheFastHenryHeightSubdivisions = Pref.makeIntPref("FastHenryHeightSubdivisions", tool.prefs, 1);
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

	private static Pref cacheFastHenryMaxSegLength = Pref.makeDoublePref("FastHenryMaxSegLength", tool.prefs, 0);
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

    private static Pref cacheVerilogUseAssign = Pref.makeBooleanSetting("VerilogUseAssign", tool.prefs, tool,
            tool.getProjectSettings(), null,
        "Verilog tab", "Verilog uses Assign construct", false);
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

	private static Pref cacheVerilogUseTrireg = Pref.makeBooleanSetting("VerilogUseTrireg", tool.prefs, tool,
            tool.getProjectSettings(), null,
        "Verilog tab", "Verilog presumes wire is Trireg", false);
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

	private static Pref cacheCDLLibName = Pref.makeStringPref("CDLLibName", tool.prefs, "");
//    static { cacheCDLLibName.attachToObject(tool, "IO/CDL tab", "Cadence library name"); }
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

	private static Pref cacheCDLLibPath = Pref.makeStringPref("CDLLibPath", tool.prefs, "");
//    static { cacheCDLLibPath.attachToObject(tool, "IO/CDL tab", "Cadence library path"); }
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

	private static Pref cacheCDLConvertBrackets = Pref.makeBooleanPref("CDLConvertBrackets", tool.prefs, false);
//    static { cacheCDLConvertBrackets.attachToObject(tool, "IO/CDL tab", "CDL converts brackets"); }
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

    /**
     * File to be included when writing CDL netlists. "" to not include any file
     */
    private static Pref cacheCDLIncludeFile = Pref.makeStringPref("CDLIncludeFile", tool.prefs, "");
    public static String getCDLIncludeFile() { return cacheCDLIncludeFile.getString(); }
    public static void setCDLIncludeFile(String file) { cacheCDLIncludeFile.setString(file); }

    private static Pref cacheCDLIgnoreResistors = Pref.makeBooleanPref("CDLLIgnoreResistors", tool.prefs, false);
//    static { cacheCDLLibName.attachToObject(tool, "IO/CDL tab", "Cadence library name"); }
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

	/****************************** BUILT-IN SIMULATION OPTIONS ******************************/

	private static Pref cacheBuiltInResimulateEach = Pref.makeBooleanPref("BuiltInResimulateEach", tool.prefs, true);
	/**
	 * Method to tell whether built-in simulators resimulate after each change to the display.
	 * When false, the user must request resimulation after a set of changes is done.
	 * @return true if built-in simulators resimulate after each change to the display.
	 */
	public static boolean isBuiltInResimulateEach() { return cacheBuiltInResimulateEach.getBoolean(); }
	/**
	 * Method to set if built-in simulators resimulate after each change to the display.
	 * When false, the user must request resimulation after a set of changes is done.
	 * @param r true if built-in simulators resimulate after each change to the display.
	 */
	public static void setBuiltInResimulateEach(boolean r) { cacheBuiltInResimulateEach.setBoolean(r); }

	private static Pref cacheBuiltInAutoAdvance = Pref.makeBooleanPref("BuiltInAutoAdvance", tool.prefs, false);
	/**
	 * Method to tell whether built-in simulators automatically advance the time cursor when stimuli are added.
	 * @return true if built-in simulators automatically advance the time cursor when stimuli are added.
	 */
	public static boolean isBuiltInAutoAdvance() { return cacheBuiltInAutoAdvance.getBoolean(); }
	/**
	 * Method to set if built-in simulators automatically advance the time cursor when stimuli are added.
	 * @param r true if built-in simulators automatically advance the time cursor when stimuli are added.
	 */
	public static void setBuiltInAutoAdvance(boolean r) { cacheBuiltInAutoAdvance.setBoolean(r); }

	private static Pref cacheWaveformDisplayMultiState = Pref.makeBooleanPref("WaveformDisplayMultiState", tool.prefs, false);
	/**
	 * Method to tell whether the waveform uses a multi-state display.
	 * Multi-state displays distinguish different strengths with different colors in the waveform window,
	 * and use different colors to distinguish different levels when drawing the cross-probing of the waveform
	 * in the schematics and layout window.
	 * The default is false.
	 * @return true if the waveform uses a multi-state display.
	 */
	public static boolean isWaveformDisplayMultiState() { return cacheWaveformDisplayMultiState.getBoolean(); }
	/**
	 * Method to set whether the waveform uses a multi-state display.
	 * Multi-state displays distinguish different strengths with different colors in the waveform window,
	 * and use different colors to distinguish different levels when drawing the cross-probing of the waveform
	 * in the schematics and layout window.
	 * @param m true if the waveform uses a multi-state display.
	 */
	public static void setWaveformDisplayMultiState(boolean m) { cacheWaveformDisplayMultiState.setBoolean(m); }

	/****************************** IRSIM OPTIONS ******************************/

	private static Pref cacheIRSIMShowsCommands = Pref.makeBooleanPref("IRSIMShowsCommands", tool.prefs, false);
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

	private static Pref cacheIRSIMDebugging = Pref.makeIntPref("IRSIMDebugging", tool.prefs, 0);
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

	private static Pref cacheIRSIMParameterFile = Pref.makeStringPref("IRSIMParameterFile", tool.prefs, "scmos0.3.prm");
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

	private static Pref cacheIRSIMStepModel = Pref.makeStringPref("IRSIMStepModel", tool.prefs, "RC");
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

    private static Pref cacheIRSIMDelayedX = Pref.makeBooleanPref("IRSIMDelayedX", tool.prefs, true);
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

    public enum SpiceEngine {
        /** Spice 2 engine. */		SPICE_ENGINE_2(0, "Spice 2"),
        /** Spice 3 engine. */		SPICE_ENGINE_3(1, "Spice 3"),
        /** HSpice engine. */		SPICE_ENGINE_H(2, "HSpice"),
        /** PSpice engine. */		SPICE_ENGINE_P(3, "PSpice"),
        /** GNUCap engine. */		SPICE_ENGINE_G(4, "Gnucap"),
        /** SmartSpice engine. */	SPICE_ENGINE_S(5, "SmartSpice"),
        /** HSpice engine for Assura. */	    SPICE_ENGINE_H_ASSURA(6, "HSpice for Assura"),
        /** HSpice engine for Calibre. */	    SPICE_ENGINE_H_CALIBRE(7, "HSpice for Calibre");

        private int code;
        private String name;
        private SpiceEngine(int val, String name)
        {
            this.code = val;
            this.name = name;
        }
        public int code() { return code; }
        public String toString() { return name;}
    }

	private static Pref cacheSpiceEngine = Pref.makeIntPref("SpiceEngine", tool.prefs, SpiceEngine.SPICE_ENGINE_H.code());
//	static
//	{
//		Pref.Meaning m = cacheSpiceEngine.attachToObject(tool, "Spice tab", "Spice engine");
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
	 * Simulation.SPICE_ENGINE_S for Smart Spice.<BR>
     * Simulation.SPICE_ENGINE_H_ASSURA for HSpice for Assura.<BR>
     * Where Simulation.SPICE_ENGINE_3 is the default.
	 */
	public static SpiceEngine getSpiceEngine()
    {
        int cache = cacheSpiceEngine.getInt();
        for (SpiceEngine p : SpiceEngine.values())
        {
            if (p.code() == cache) return p;
        }
        throw new Error("No Spice engine found");
    }
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
	 * Simulation.SPICE_ENGINE_S for Smart Spice.<BR>
     * Simulation.SPICE_ENGINE_H_ASSURA for HSpice for Assura.<BR>
     * Where Simulation.SPICE_ENGINE_3 is the default.
	 */
	public static void setSpiceEngine(SpiceEngine engine) { cacheSpiceEngine.setInt(engine.code()); }

	private static Pref cacheSpiceLevel = Pref.makeStringPref("SpiceLevel", tool.prefs, "1");
//    static { cacheSpiceLevel.attachToObject(tool, "Tools/Spice tab", "Spice level"); }
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

	private static Pref cacheSpiceOutputFormat = Pref.makeStringPref("SpiceOutputFormat", tool.prefs, "Standard");
//    static { cacheSpiceOutputFormat.attachToObject(tool, "Tools/Spice tab", "Spice output format"); }
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

    private static Pref cacheSpiceRunChoice = Pref.makeIntPref("SpiceRunChoice", tool.prefs, 0);
//    static {
//        Pref.Meaning m = cacheSpiceRunChoice.attachToObject(tool, "Tool Options, Spice tab", "Spice Run Choice");
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

    private static Pref cacheSpiceRunDir = Pref.makeStringPref("SpiceRunDir", tool.prefs, "");
//    static { cacheSpiceRunDir.attachToObject(tool, "Tool Options, Spice tab", "Spice Run Dir"); }
    /** Get the spice run directory */
    public static String getSpiceRunDir() { return cacheSpiceRunDir.getString(); }
    /** Set the spice run directory */
    public static void setSpiceRunDir(String dir) { cacheSpiceRunDir.setString(dir); }

    private static Pref cacheSpiceUseRunDir = Pref.makeBooleanPref("SpiceUseRunDir", tool.prefs, false);
//    static { cacheSpiceUseRunDir.attachToObject(tool, "Tool Options, Spice tab", "Use Run Dir"); }
    /** Get whether or not to use the user-specified spice run dir */
    public static boolean getSpiceUseRunDir() { return cacheSpiceUseRunDir.getBoolean(); }
    /** Set whether or not to use the user-specified spice run dir */
    public static void setSpiceUseRunDir(boolean b) { cacheSpiceUseRunDir.setBoolean(b); }

    private static Pref cacheSpiceOutputOverwrite = Pref.makeBooleanPref("SpiceOverwriteOutputFile", tool.prefs, false);
//    static { cacheSpiceOutputOverwrite.attachToObject(tool, "Tool Options, Spice tab", "Overwrite Output Spice File"); }
    /** Get whether or not we automatically overwrite the spice output file */
    public static boolean getSpiceOutputOverwrite() { return cacheSpiceOutputOverwrite.getBoolean(); }
    /** Set whether or not we automatically overwrite the spice output file */
    public static void setSpiceOutputOverwrite(boolean b) { cacheSpiceOutputOverwrite.setBoolean(b); }

    private static Pref cacheSpiceRunProbe = Pref.makeBooleanPref("SpiceRunProbe", tool.prefs, false);
    /** Get whether or not to run the spice probe after running spice */
    public static boolean getSpiceRunProbe() { return cacheSpiceRunProbe.getBoolean(); }
    /** Set whether or not to run the spice probe after running spice */
    public static void setSpiceRunProbe(boolean b) { cacheSpiceRunProbe.setBoolean(b); }

    private static Pref cacheSpiceRunProgram = Pref.makeStringPref("SpiceRunProgram", tool.prefs, "");
//    static { cacheSpiceRunProgram.attachToObject(tool, "Tool Options, Spice tab", "Spice Run Program"); }
    /** Get the spice run program */
    public static String getSpiceRunProgram() { return cacheSpiceRunProgram.getString(); }
    /** Set the spice run program */
    public static void setSpiceRunProgram(String c) { cacheSpiceRunProgram.setString(c); }

    private static Pref cacheSpiceRunProgramArgs = Pref.makeStringPref("SpiceRunProgramArgs", tool.prefs, "");
//    static { cacheSpiceRunProgramArgs.attachToObject(tool, "Tool Options, Spice tab", "Spice Run Program Args"); }
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
			cacheSpicePartsLibrary = Pref.makeStringPref("SpicePartsLibrary", tool.prefs, libNames[0]);
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
			cacheSpicePartsLibrary = Pref.makeStringPref("SpicePartsLibrary", tool.prefs, libNames[0]);
		}
		cacheSpicePartsLibrary.setString(parts);
	}

	private static Pref cacheSpiceHeaderCardInfo = Pref.makeStringPref("SpiceHeaderCardInfo", tool.prefs, "");
//    static { cacheSpiceHeaderCardInfo.attachToObject(tool, "Tools/Spice tab", "Spice header card information"); }
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

	private static Pref cacheSpiceTrailerCardInfo = Pref.makeStringPref("SpiceTrailerCardInfo", tool.prefs, "");
//    static { cacheSpiceTrailerCardInfo.attachToObject(tool, "Tools/Spice tab", "Spice trailer card information"); }
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

	private static Pref cacheSpiceUseParasitics = Pref.makeBooleanPref("SpiceUseParasitics", tool.prefs, true);
//    static { cacheSpiceUseParasitics.attachToObject(tool, "Tools/Spice tab", "Spice uses parasitics"); }
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

    public static Pref cacheParasiticsUseVerboseNaming = Pref.makeBooleanPref("ParasiticsUseVerboseNaming", tool.prefs, true);
    public static boolean isParasiticsUseVerboseNaming() { return cacheParasiticsUseVerboseNaming.getBoolean(); }
    public static void setParasiticsUseVerboseNaming(boolean b) { cacheParasiticsUseVerboseNaming.setBoolean(b); }

    public static Pref cacheParasiticsBackAnnotateLayout = Pref.makeBooleanPref("ParasiticsBackAnnotateLayout", tool.prefs, true);
    public static boolean isParasiticsBackAnnotateLayout() { return cacheParasiticsBackAnnotateLayout.getBoolean(); }
    public static void setParasiticsBackAnnotateLayout(boolean b) { cacheParasiticsBackAnnotateLayout.setBoolean(b); }

    public static Pref cacheParasiticsExtractPowerGround = Pref.makeBooleanPref("ParasiticsExtractPowerGround", tool.prefs, false);
    public static boolean isParasiticsExtractPowerGround() { return cacheParasiticsExtractPowerGround.getBoolean(); }
    public static void setParasiticsExtractPowerGround(boolean b) { cacheParasiticsExtractPowerGround.setBoolean(b); }

    public static Pref cacheParasiticsUseExemptedNetsFile = Pref.makeBooleanPref("UseExemptedNetsFile", tool.prefs, false);
    public static boolean isParasiticsUseExemptedNetsFile() { return cacheParasiticsUseExemptedNetsFile.getBoolean(); }
    public static void setParasiticsUseExemptedNetsFile(boolean b) { cacheParasiticsUseExemptedNetsFile.setBoolean(b); }

    public static Pref cacheParasiticsIgnoreExemptedNets = Pref.makeBooleanPref("IgnoreExemptedNets", tool.prefs, true);
    public static boolean isParasiticsIgnoreExemptedNets() { return cacheParasiticsIgnoreExemptedNets.getBoolean(); }
    public static void setParasiticsIgnoreExemptedNets(boolean b) { cacheParasiticsIgnoreExemptedNets.setBoolean(b); }
    
	private static Pref cacheSpiceUseNodeNames = Pref.makeBooleanPref("SpiceUseNodeNames", tool.prefs, true);
//    static { cacheSpiceUseNodeNames.attachToObject(tool, "Tools/Spice tab", "Spice uses node names"); }
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

	private static Pref cacheSpiceForceGlobalPwrGnd = Pref.makeBooleanPref("SpiceForceGlobalPwrGnd", tool.prefs, false);
//    static { cacheSpiceForceGlobalPwrGnd.attachToObject(tool, "Tools/Spice tab", "Spice forces global VDD/GND"); }
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

	private static Pref cacheSpiceUseCellParameters = Pref.makeBooleanPref("SpiceUseCellParameters", tool.prefs, false);
//    static { cacheSpiceForceGlobalPwrGnd.attachToObject(tool, "Tools/Spice tab", "Spice uses cell parameters"); }
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

	private static Pref cacheSpiceWriteTransSizeInLambda = Pref.makeBooleanPref("SpiceWriteTransSizeInLambda", tool.prefs, false);
//    static { cacheSpiceWriteTransSizeInLambda.attachToObject(tool, "Tools/Spice tab", "Spice writes transistor sizes in lambda"); }
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

    private static Pref cacheSpiceWriteSubcktTopCell = Pref.makeBooleanPref("SpiceWriteSubcktTopCell", tool.prefs, false);
    public static boolean isSpiceWriteSubcktTopCell() { return cacheSpiceWriteSubcktTopCell.getBoolean(); }
    public static void setSpiceWriteSubcktTopCell(boolean b) { cacheSpiceWriteSubcktTopCell.setBoolean(b); }

    private static Pref cacheSpiceWriteEmptySubckts = Pref.makeBooleanPref("SpiceWriteEmptySubckts", tool.prefs, true);
    public static boolean isSpiceWriteEmtpySubckts() { return cacheSpiceWriteEmptySubckts.getBoolean(); }
    public static void setSpiceWriteEmptySubckts(boolean b) { cacheSpiceWriteEmptySubckts.setBoolean(b); }

    private static Pref cacheSpiceEpicReaderProcess = Pref.makeBooleanPref("SpiceEpicReaderProcess", tool.prefs, true);
    public static boolean isSpiceEpicReaderProcess() { return cacheSpiceEpicReaderProcess.getBoolean(); }
    public static void setSpiceEpicReaderProcess(boolean b) { cacheSpiceEpicReaderProcess.setBoolean(b); }
    
	private static Pref cacheSpiceEpicReaderMemorySize = Pref.makeIntPref("SpiceEpicReaderMemorySize", tool.prefs, 1000);
	/**
	 * Method to tell the maximum memory to use for EpicReaderProcess, in megatybes.
	 * The default is 1000 (1 gigabyte).
	 * @return the maximum memory to use for EpicReaderProcess (in megabytes).
	 */
	public static int getSpiceEpicMemorySize() { return cacheSpiceEpicReaderMemorySize.getInt(); }
	/**
	 * Method to set the maximum memory to use for EpicReaderProcess.
	 * @param limit maximum memory to use for EpicReaderProcess (in megabytes).
	 */
	public static void setSpiceEpicMemorySize(int limit) { cacheSpiceEpicReaderMemorySize.setInt(limit); }
}
