/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SimulationTool.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.simulation;

import com.sun.electric.database.Environment;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.EditWindow_;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.lib.LibFile;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.ToolSettings;
import com.sun.electric.tool.UserInterfaceExec;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.io.output.Verilog;
import com.sun.electric.tool.io.output.IRSIM.IRSIMPreferences;
import com.sun.electric.tool.simulation.als.ALS;
import com.sun.electric.tool.simulation.irsim.IRSIM;
import com.sun.electric.tool.user.dialogs.EDialog;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.waveform.WaveformWindow;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Iterator;
import java.util.List;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

/**
 * This is the Simulation Interface tool.
 */
public class SimulationTool extends Tool
{
	/** the Simulation tool. */							private static SimulationTool tool = new SimulationTool();

	/** key of Variable holding rise time. */			public static final Variable.Key RISE_DELAY_KEY = Variable.newKey("SIM_rise_delay");
	/** key of Variable holding fall time. */			public static final Variable.Key FALL_DELAY_KEY = Variable.newKey("SIM_fall_delay");
	/** key of Variable holding flag for weak nodes. */	public static final Variable.Key WEAK_NODE_KEY = Variable.newKey("SIM_weak_node");
	/** key of Variable holding "M" factors. */			public static final Variable.Key M_FACTOR_KEY = Variable.newKey("ATTR_M");

	/** constant for ALS simulation */					public static final int ALS_ENGINE = 0;
	/** constant for IRSIM simulation */				public static final int IRSIM_ENGINE = 1;

	private SimulationTool() { super("simulation"); }

	public void init() { }

	public static SimulationTool getSimulationTool() { return tool; }

	/****************************** CONTROL OF SIMULATION ENGINES ******************************/

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
                boolean convert = false;
                boolean compile = false;
				if (cell.getView() != View.NETLISTALS)
				{
					if (cell.isSchematic() || cell.getView() == View.LAYOUT)
					{
						// current cell is Schematic.  See if there is a more recent netlist or VHDL
						Cell vhdlCell = cell.otherView(View.VHDL);
						if (vhdlCell != null && vhdlCell.getRevisionDate().after(cell.getRevisionDate())) cell = vhdlCell; else {
                            convert = true;
                            compile = true;
                        }
					}
					if (cell.getView() == View.VHDL)
					{
						// current cell is VHDL.  See if there is a more recent netlist
						Cell netListCell = cell.otherView(View.NETLISTQUISC);
						if (netListCell != null && netListCell.getRevisionDate().after(cell.getRevisionDate())) cell = netListCell; else
                            compile = true;
					}
				}

				// now schedule the simulation work
				new ALS.DoALSActivity(cell, convert, compile, originalCell, prevEngine, tool);
				break;

			case IRSIM_ENGINE:
				if (!IRSIM.hasIRSIM()) return;
				new RunIRSIM(cell, context, fileName);
				break;
		}
	}

	private static class RunIRSIM extends Thread
	{
		private Cell cell;
		private VarContext context;
		private String fileName;
        private IRSIMPreferences ip;
        private final Environment launcherEnvironment;
        private final UserInterfaceExec userInterface;

		public RunIRSIM(Cell cell, VarContext context, String fileName)
		{
			this.cell = cell;
			this.context = context;
			this.fileName = fileName;
            ip = new IRSIMPreferences(true);

            launcherEnvironment = Environment.getThreadEnvironment();
            userInterface = new UserInterfaceExec();
            if (fileName != null)
            {
            	// when reading a file, do it in a new thread so that progress bars work
	            start();
            } else
            {
            	// when simulating a Cell, do it directly so that environments work
            	IRSIM.runIRSIM(cell, context, fileName, ip);
            }
		}

		public void run()
		{
            if (Thread.currentThread() == this)
            {
                Environment.setThreadEnvironment(launcherEnvironment);
                Job.setUserInterface(userInterface);
            }
			IRSIM.runIRSIM(cell, context, fileName, ip);
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
	 * @return true if stimuli were deleted.
	 */
	public static boolean removeSelectedStimuli()
	{
		Engine engine = findEngine();
		if (engine == null) return false;
		return engine.removeSelectedStimuli();
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
				Engine e = ww.getSimData().getEngine();
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
		new SetSpiceModel(ni);
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
		new SetWireType(list, type);
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
		new SetTransistorStrength(ni, weak);
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
	/**
	 * Method to tell whether FastHenry deck generation should use a single frequency, by default.
	 * @return true if FastHenry deck generation should use a single frequency, by default.
	 */
	public static boolean isFactoryFastHenryUseSingleFrequency() { return cacheFastHenryUseSingleFrequency.getBooleanFactoryValue(); }

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
	/**
	 * Method to return the FastHenry starting frequency (or only if using a single frequency), by default.
	 * @return the FastHenry starting frequency (or only if using a single frequency), by default.
	 */
	public static double getFactoryFastHenryStartFrequency() { return cacheFastHenryStartFrequency.getDoubleFactoryValue(); }

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
	/**
	 * Method to return the FastHenry ending frequency, by default.
	 * @return the FastHenry ending frequency, by default.
	 */
	public static double getFactoryFastHenryEndFrequency() { return cacheFastHenryEndFrequency.getDoubleFactoryValue(); }

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
	/**
	 * Method to return the number of runs per decade for FastHenry deck generation, by default.
	 * @return the number of runs per decade for FastHenry deck generation, by default.
	 */
	public static int getFactoryFastHenryRunsPerDecade() { return cacheFastHenryRunsPerDecade.getIntFactoryValue(); }

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
	/**
	 * Method to tell whether FastHenry deck generation should make a multipole subcircuit, by default.
	 * @return true if FastHenry deck generation should make a multipole subcircuit, by default.
	 */
	public static boolean isFactoryFastHenryMultiPole() { return cacheFastHenryMultiPole.getBooleanFactoryValue(); }

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
	/**
	 * Method to return the number of poles for FastHenry deck generation, by default.
	 * @return the number of poles for FastHenry deck generation, by default.
	 */
	public static int getFactoryFastHenryNumPoles() { return cacheFastHenryNumPoles.getIntFactoryValue(); }

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
	/**
	 * Method to return the FastHenry default wire thickness, by default.
	 * @return the FastHenry default wire thickness, by default.
	 */
	public static double getFactoryFastHenryDefThickness() { return cacheFastHenryDefThickness.getDoubleFactoryValue(); }

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
	/**
	 * Method to return the default number of width subdivisions for FastHenry deck generation, by default.
	 * @return the default number of width subdivisions for FastHenry deck generation, by default.
	 */
	public static int getFactoryFastHenryWidthSubdivisions() { return cacheFastHenryWidthSubdivisions.getIntFactoryValue(); }

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
	/**
	 * Method to return the default number of height subdivisions for FastHenry deck generation, by default.
	 * @return the default number of height subdivisions for FastHenry deck generation, by default.
	 */
	public static int getFactoryFastHenryHeightSubdivisions() { return cacheFastHenryHeightSubdivisions.getIntFactoryValue(); }

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
	/**
	 * Method to return the maximum segment length for FastHenry deck generation, by default.
	 * @return the maximum segment length for FastHenry deck generation, by default.
	 */
	public static double getFactoryFastHenryMaxSegLength() { return cacheFastHenryMaxSegLength.getDoubleFactoryValue(); }

	/****************************** VERILOG OPTIONS ******************************/

	/**
	 * Method to tell whether Verilog deck generation should use the Assign statement.
	 * The default is false.
	 * @return true if Verilog deck generation should use the Assign statement.
	 */
	public static boolean getVerilogUseAssign() { return getVerilogUseAssignSetting().getBoolean(); }
	/**
	 * Returns setting to tell whether Verilog deck generation should use the Assign statement.
	 * @return setting to tell whether Verilog deck generation should use the Assign statement.
	 */
	public static Setting getVerilogUseAssignSetting() { return ToolSettings.getVerilogUseAssignSetting(); }

	/**
	 * Method to tell whether Verilog deck generation should use Trireg by default.
	 * The alternative is to use the "wire" statement.
	 * The default is false.
	 * @return true if Verilog deck generation should use Trireg by default.
	 */
	public static boolean getVerilogUseTrireg() { return getVerilogUseTriregSetting().getBoolean(); }
	/**
	 * Returns setting to tell whether Verilog deck generation should use Trireg by default.
	 * The alternative is to use the "wire" statement.
	 * @return setting to tell whether Verilog deck generation should use Trireg by default.
	 */
	public static Setting getVerilogUseTriregSetting() { return ToolSettings.getVerilogUseTriregSetting(); }

	private static Pref cacheVerilogStopAtStandardCells = Pref.makeBooleanPref("VerilogStopAtStandardCells", tool.prefs, false);
	public static void setVerilogStopAtStandardCells(boolean b) { cacheVerilogStopAtStandardCells.setBoolean(b); }
	public static boolean getVerilogStopAtStandardCells() { return cacheVerilogStopAtStandardCells.getBoolean(); }
	public static boolean getFactoryVerilogStopAtStandardCells() { return cacheVerilogStopAtStandardCells.getBooleanFactoryValue(); }

    private static Pref cacheVerilogNetlistNonstandardCells = Pref.makeBooleanPref("VerliogNetlistNonstandardCells", tool.prefs, true);
    public static void setVerilogNetlistNonstandardCells(boolean b) { cacheVerilogNetlistNonstandardCells.setBoolean(b); }
    public static boolean getVerilogNetlistNonstandardCells() { return cacheVerilogNetlistNonstandardCells.getBoolean(); }
    public static boolean getFactoryVerilogNetlistNonstandardCells() { return cacheVerilogNetlistNonstandardCells.getBooleanFactoryValue(); }

    /**
     * BVE - Improved support for Verilog generation
     */
    private static Pref cachePreserveVerilogFormating = Pref.makeBooleanPref("PreserveVerilogFormating", tool.prefs, true);
    public static void setPreserveVerilogFormating(boolean b) { cachePreserveVerilogFormating.setBoolean(b); }
    public static boolean getPreserveVerilogFormating() { return cachePreserveVerilogFormating.getBoolean(); }
    public static boolean getFactoryPreserveVerilogFormating() { return cachePreserveVerilogFormating.getBooleanFactoryValue(); }

    private static Pref cacheVerilogParameterizeModuleNames = Pref.makeBooleanPref("VerilogParamertizeModuleNames", tool.prefs, false);
    public static void setVerilogParameterizeModuleNames(boolean b) { cacheVerilogParameterizeModuleNames.setBoolean(b); }
    public static boolean getVerilogParameterizeModuleNames() { return cacheVerilogParameterizeModuleNames.getBoolean(); }
    public static boolean getFactoryVerilogParameterizeModuleNames() { return cacheVerilogParameterizeModuleNames.getBooleanFactoryValue(); }

    private static Pref cacheVerilogWriteModuleForEachIcon = Pref.makeBooleanPref("VerilogWriteModuleForEachIcon", tool.prefs, false);
    public static void setVerilogWriteModuleForEachIcon(boolean b) { cacheVerilogWriteModuleForEachIcon.setBoolean(b); }
    public static boolean isVerilogWriteModuleForEachIcon() { return cacheVerilogWriteModuleForEachIcon.getBoolean(); }
    public static boolean isFactoryVerilogWriteModuleForEachIcon() { return cacheVerilogWriteModuleForEachIcon.getBooleanFactoryValue(); }

    private static Pref cacheVerilogRunPlacementTool = Pref.makeBooleanPref("cacheVerilogRunPlacementTool", tool.prefs, false);
    public static void setVerilogRunPlacementTool(boolean b) { cacheVerilogRunPlacementTool.setBoolean(b); }
    public static boolean getVerilogRunPlacementTool() { return cacheVerilogRunPlacementTool.getBoolean(); }
    public static boolean getFactoryVerilogRunPlacementTool() { return cacheVerilogRunPlacementTool.getBooleanFactoryValue(); }

    /****************************** CDL OPTIONS ******************************/

	private static Pref cacheCDLLibName = Pref.makeStringPref("CDLLibName", tool.prefs, "");
//	static { cacheCDLLibName.attachToObject(tool, "IO/CDL tab", "Cadence library name"); }
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
	/**
	 * Method to return the default CDL library name.
	 * CDL is a weak form of a Spice deck, and it includes a Cadence library name.
	 * @return the default CDL library name.
	 */
	public static String getFactoryCDLLibName() { return cacheCDLLibName.getStringFactoryValue(); }

	private static Pref cacheCDLLibPath = Pref.makeStringPref("CDLLibPath", tool.prefs, "");
//	static { cacheCDLLibPath.attachToObject(tool, "IO/CDL tab", "Cadence library path"); }
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
	/**
	 * Method to return the default CDL library path.
	 * CDL is a weak form of a Spice deck, and it includes a Cadence library.
	 * @return the default CDL library path.
	 */
	public static String getFactoryCDLLibPath() { return cacheCDLLibPath.getStringFactoryValue(); }

	private static Pref cacheCDLConvertBrackets = Pref.makeBooleanPref("CDLConvertBrackets", tool.prefs, false);
//	static { cacheCDLConvertBrackets.attachToObject(tool, "IO/CDL tab", "CDL converts brackets"); }
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
	 * Method to tell whether CDL converts square bracket characters by default.
	 * CDL is a weak form of a Spice deck, and it includes a Cadence library.
	 * @return true if CDL converts square bracket characters by default.
	 */
	public static boolean isFactoryCDLConvertBrackets() { return cacheCDLConvertBrackets.getBooleanFactoryValue(); }

	/**
	 * File to be included when writing CDL netlists. "" to not include any file
	 */
	private static Pref cacheCDLIncludeFile = Pref.makeStringPref("CDLIncludeFile", tool.prefs, "");
	public static String getCDLIncludeFile() { return cacheCDLIncludeFile.getString(); }
	public static void setCDLIncludeFile(String file) { cacheCDLIncludeFile.setString(file); }
	public static String getFactoryCDLIncludeFile() { return cacheCDLIncludeFile.getStringFactoryValue(); }

	private static Pref cacheCDLIgnoreResistors = Pref.makeBooleanPref("CDLLIgnoreResistors", tool.prefs, false);
//	static { cacheCDLLibName.attachToObject(tool, "IO/CDL tab", "Cadence library name"); }
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
	/**
	 * Method to tell whether built-in simulators resimulate after each change to the display, by default.
	 * When false, the user must request resimulation after a set of changes is done.
	 * @return true if built-in simulators resimulate after each change to the display, by default.
	 */
	public static boolean isFactoryBuiltInResimulateEach() { return cacheBuiltInResimulateEach.getBooleanFactoryValue(); }

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
	/**
	 * Method to tell whether built-in simulators automatically advance the time cursor when stimuli are added, by default.
	 * @return true if built-in simulators automatically advance the time cursor when stimuli are added, by default.
	 */
	public static boolean isFactoryBuiltInAutoAdvance() { return cacheBuiltInAutoAdvance.getBooleanFactoryValue(); }

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
	/**
	 * Method to tell whether the waveform uses a multi-state display, by default.
	 * Multi-state displays distinguish different strengths with different colors in the waveform window,
	 * and use different colors to distinguish different levels when drawing the cross-probing of the waveform
	 * in the schematics and layout window.
	 * @return true if the waveform uses a multi-state display, by default.
	 */
	public static boolean isFactoryWaveformDisplayMultiState() { return cacheWaveformDisplayMultiState.getBooleanFactoryValue(); }

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
	/**
	 * Method to tell whether IRSIM shows commands that are issued (for debugging), by default.
	 * @return true if IRSIM shows commands that are issued (for debugging), by default.
	 */
	public static boolean isFactoryIRSIMShowsCommands() { return cacheIRSIMShowsCommands.getBooleanFactoryValue(); }

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
	/**
	 * Method to tell the debugging level for IRSIM simulation, by default.
	 * This is a combination of bits, where:
	 * Bit 1: debug event scheduling (Sim.DEBUG_EV)
	 * Bit 2: debug final value computation (Sim.DEBUG_DC)
	 * Bit 3: debug tau/delay computation (Sim.DEBUG_TAU)
	 * Bit 4: debug taup computation (Sim.DEBUG_TAUP)
	 * Bit 5: debug spike analysis (Sim.DEBUG_SPK)
	 * Bit 6: debug tree walk (Sim.DEBUG_TW)
	 * @return the debugging level for IRSIM simulation, by default.
	 */
	public static int getFactoryIRSIMDebugging() { return cacheIRSIMDebugging.getIntFactoryValue(); }

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
	/**
	 * Method to tell the parameter file to use for IRSIM, by default.
	 * @return the parameter file to use for IRSIM, by default.
	 */
	public static String getFactoryIRSIMParameterFile() { return cacheIRSIMParameterFile.getStringFactoryValue(); }

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
	/**
	 * Method to tell the stepping model to use for IRSIM, by default.
	 * Possible choices are "RC" and "Linear".
	 * @return the stepping model to use for IRSIM, by default.
	 */
	public static String getFactoryIRSIMStepModel() { return cacheIRSIMStepModel.getStringFactoryValue(); }

	private static Pref cacheIRSIMDelayedX = Pref.makeBooleanPref("IRSIMDelayedX", tool.prefs, true);
	/**
	 * Get whether or not IRSIM uses a delayed X model, versus the old fast-propagating X model.
	 * @return true if using the delayed X model, false if using the old fast-propagating X model.
	 */
	public static boolean isIRSIMDelayedX() { return cacheIRSIMDelayedX.getBoolean(); }
	/**
	 * Get whether or not IRSIM uses a delayed X model, versus the old fast-propagating X model.
	 * @param b true to use the delayed X model, false to use the old fast-propagating X model.
	 */
	public static void setIRSIMDelayedX(boolean b) { cacheIRSIMDelayedX.setBoolean(b); }
	/**
	 * Get whether or not IRSIM uses a delayed X model, versus the old fast-propagating X model, by default.
	 * @return true if using the delayed X model, false if using the old fast-propagating X model, by default.
	 */
	public static boolean isFactoryIRSIMDelayedX() { return cacheIRSIMDelayedX.getBooleanFactoryValue(); }

	/****************************** SPICE OPTIONS ******************************/

	public enum SpiceEngine {
		/** Spice 2 engine. */		SPICE_ENGINE_2(0, "Spice 2"),
		/** Spice 3 engine. */		SPICE_ENGINE_3(1, "Spice 3"),
		/** HSpice engine. */		SPICE_ENGINE_H(2, "HSpice"),
		/** PSpice engine. */		SPICE_ENGINE_P(3, "PSpice"),
		/** GNUCap engine. */		SPICE_ENGINE_G(4, "Gnucap"),
		/** SmartSpice engine. */	SPICE_ENGINE_S(5, "SmartSpice"),
		/** Spice Opus engine. */	SPICE_ENGINE_O(8, "Spice Opus"),
		/** HSpice engine for Assura. */		SPICE_ENGINE_H_ASSURA(6, "HSpice for Assura"),
		/** HSpice engine for Calibre. */		SPICE_ENGINE_H_CALIBRE(7, "HSpice for Calibre");

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
	 * SimulationTool.SPICE_ENGINE_2 for Spice 2.<BR>
	 * SimulationTool.SPICE_ENGINE_3 for Spice 3.<BR>
	 * SimulationTool.SPICE_ENGINE_H for HSpice.<BR>
	 * SimulationTool.SPICE_ENGINE_P for PSpice.<BR>
	 * SimulationTool.SPICE_ENGINE_G for GNUCap.<BR>
	 * SimulationTool.SPICE_ENGINE_S for Smart Spice.<BR>
	 * SimulationTool.SPICE_ENGINE_O for Spice Opus.<BR>
	 * SimulationTool.SPICE_ENGINE_H_ASSURA for HSpice for Assura.<BR>
	 * Where SimulationTool.SPICE_ENGINE_3 is the default.
	 */
	public static SpiceEngine getSpiceEngine()
	{
		int cache = cacheSpiceEngine.getInt();
		for (SpiceEngine p : SpiceEngine.values())
		{
			if (p.code() == cache) return p;
		}
		return SpiceEngine.values()[0];
	}
	/**
	 * Method to set which SPICE engine is being used.
	 * Since different versions of SPICE have slightly different syntax,
	 * this is needed to tell the deck generator which variation to target.
	 * @param engine which SPICE engine is being used.
	 * These constants are available: <BR>
	 * SimulationTool.SpiceEngine.SPICE_ENGINE_2 for Spice 2.<BR>
	 * SimulationTool.SpiceEngine.SPICE_ENGINE_3 for Spice 3. (the default)<BR>
	 * SimulationTool.SpiceEngine.SPICE_ENGINE_H for HSpice.<BR>
	 * SimulationTool.SpiceEngine.SPICE_ENGINE_P for PSpice.<BR>
	 * SimulationTool.SpiceEngine.SPICE_ENGINE_G for GNUCap.<BR>
	 * SimulationTool.SpiceEngine.SPICE_ENGINE_S for Smart Spice.<BR>
	 * SimulationTool.SpiceEngine.SPICE_ENGINE_O for Spice Opus.<BR>
	 * SimulationTool.SpiceEngine.SPICE_ENGINE_H_ASSURA for HSpice for Assura.
	 */
	public static void setSpiceEngine(SpiceEngine engine) { cacheSpiceEngine.setInt(engine.code()); }
	/**
	 * Method to tell which SPICE engine is being used, by default.
	 * Since different versions of SPICE have slightly different syntax,
	 * this is needed to tell the deck generator which variation to target.
	 * @return which SPICE engine is being used, by default.
	 * These constants are available: <BR>
	 * SimulationTool.SPICE_ENGINE_2 for Spice 2.<BR>
	 * SimulationTool.SPICE_ENGINE_3 for Spice 3.<BR>
	 * SimulationTool.SPICE_ENGINE_H for HSpice.<BR>
	 * SimulationTool.SPICE_ENGINE_P for PSpice.<BR>
	 * SimulationTool.SPICE_ENGINE_G for GNUCap.<BR>
	 * SimulationTool.SPICE_ENGINE_S for Smart Spice.<BR>
	 * SimulationTool.SPICE_ENGINE_O for Spice Opus.<BR>
	 * SimulationTool.SPICE_ENGINE_H_ASSURA for HSpice for Assura.<BR>
	 */
	public static SpiceEngine getFactorySpiceEngine()
	{
		int cache = cacheSpiceEngine.getIntFactoryValue();
		for (SpiceEngine p : SpiceEngine.values())
		{
			if (p.code() == cache) return p;
		}
		return SpiceEngine.values()[0];
	}

	private static Pref cacheSpiceLevel = Pref.makeStringPref("SpiceLevel", tool.prefs, "1");
//	static { cacheSpiceLevel.attachToObject(tool, "Tools/Spice tab", "Spice level"); }
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
	/**
	 * Method to tell which SPICE level is being used, by default.
	 * SPICE can use 3 different levels of simulation.
	 * @return which SPICE level is being used (1, 2, or 3), by default.
	 */
	public static String getFactorySpiceLevel() { return cacheSpiceLevel.getStringFactoryValue(); }

	private static Pref cacheSpiceShortResistors = Pref.makeIntPref("SpiceShortResistors", tool.prefs, 0);
	/**
	 * Method to tell how SPICE should short resistors.
	 * These values can be: <BR>
	 * 0 for no resistor shorting.<BR>
	 * 1 for parasitic resistor shorting (normal resistors shorted, poly resistors not shorted).<BR>
	 * 2 for all resistor shorting.<BR>
	 * @return how SPICE should short resistors.
	 */
	public static int getSpiceShortResistors() { return cacheSpiceShortResistors.getInt(); }
	/**
	 * Method to set how SPICE should short resistors.
	 * @param sr how SPICE should short resistors: <BR>
	 * 0 for no resistor shorting.<BR>
	 * 1 for parasitic resistor shorting (normal resistors shorted, poly resistors not shorted).<BR>
	 * 2 for all resistor shorting.<BR>
	 */
	public static void setSpiceShortResistors(int sr) { cacheSpiceShortResistors.setInt(sr); }
	/**
	 * Method to tell how SPICE should short resistors, by default.
	 * These values can be: <BR>
	 * 0 for no resistor shorting.<BR>
	 * 1 for parasitic resistor shorting (normal resistors shorted, poly resistors not shorted).<BR>
	 * 2 for all resistor shorting.<BR>
	 * @return how SPICE should short resistors, by default.
	 */
	public static int getFactorySpiceShortResistors() { return cacheSpiceShortResistors.getIntFactoryValue(); }

	public static final String spiceRunChoiceDontRun = "Don't Run";
	public static final String spiceRunChoiceRunIgnoreOutput = "Run, Ignore Output";
	public static final String spiceRunChoiceRunReportOutput = "Run, Report Output";
	private static final String [] spiceRunChoices = {spiceRunChoiceDontRun, spiceRunChoiceRunIgnoreOutput, spiceRunChoiceRunReportOutput};

	private static Pref cacheSpiceRunChoice = Pref.makeIntPref("SpiceRunChoice", tool.prefs, 0);
//	static {
//		Pref.Meaning m = cacheSpiceRunChoice.attachToObject(tool, "Tool Options, Spice tab", "Spice Run Choice");
//		m.setTrueMeaning(new String[] {spiceRunChoiceDontRun, spiceRunChoiceRunIgnoreOutput, spiceRunChoiceRunReportOutput});
//	}
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
	/** Get the default setting for the Spice Run Choice preference */
	public static String getFactorySpiceRunChoice() { return spiceRunChoices[cacheSpiceRunChoice.getIntFactoryValue()]; }

	private static Pref cacheSpiceRunDir = Pref.makeStringPref("SpiceRunDir", tool.prefs, "");
//	static { cacheSpiceRunDir.attachToObject(tool, "Tool Options, Spice tab", "Spice Run Dir"); }
	/** Get the spice run directory */
	public static String getSpiceRunDir() { return cacheSpiceRunDir.getString(); }
	/** Set the spice run directory */
	public static void setSpiceRunDir(String dir) { cacheSpiceRunDir.setString(dir); }
	/** Get the factory default spice run directory */
	public static String getFactorySpiceRunDir() { return cacheSpiceRunDir.getStringFactoryValue(); }

	private static Pref cacheSpiceUseRunDir = Pref.makeBooleanPref("SpiceUseRunDir", tool.prefs, false);
//	static { cacheSpiceUseRunDir.attachToObject(tool, "Tool Options, Spice tab", "Use Run Dir"); }
	/** Get whether or not to use the user-specified spice run dir */
	public static boolean getSpiceUseRunDir() { return cacheSpiceUseRunDir.getBoolean(); }
	/** Set whether or not to use the user-specified spice run dir */
	public static void setSpiceUseRunDir(boolean b) { cacheSpiceUseRunDir.setBoolean(b); }
	/** Get whether or not to use the user-specified spice run dir, by default */
	public static boolean getFactorySpiceUseRunDir() { return cacheSpiceUseRunDir.getBooleanFactoryValue(); }

	private static Pref cacheSpiceOutputOverwrite = Pref.makeBooleanPref("SpiceOverwriteOutputFile", tool.prefs, false);
//	static { cacheSpiceOutputOverwrite.attachToObject(tool, "Tool Options, Spice tab", "Overwrite Output Spice File"); }
	/** Get whether or not we automatically overwrite the spice output file */
	public static boolean getSpiceOutputOverwrite() { return cacheSpiceOutputOverwrite.getBoolean(); }
	/** Set whether or not we automatically overwrite the spice output file */
	public static void setSpiceOutputOverwrite(boolean b) { cacheSpiceOutputOverwrite.setBoolean(b); }
	/** Get whether or not we automatically overwrite the spice output file, by default */
	public static boolean getFactorySpiceOutputOverwrite() { return cacheSpiceOutputOverwrite.getBooleanFactoryValue(); }

	private static Pref cacheSpiceRunProbe = Pref.makeBooleanPref("SpiceRunProbe", tool.prefs, false);
	/** Get whether or not to run the spice probe after running spice */
	public static boolean getSpiceRunProbe() { return cacheSpiceRunProbe.getBoolean(); }
	/** Set whether or not to run the spice probe after running spice */
	public static void setSpiceRunProbe(boolean b) { cacheSpiceRunProbe.setBoolean(b); }
	/** Get whether or not to run the spice probe after running spice, by default */
	public static boolean getFactorySpiceRunProbe() { return cacheSpiceRunProbe.getBooleanFactoryValue(); }

	private static Pref cacheSpiceRunProgram = Pref.makeStringPref("SpiceRunProgram", tool.prefs, "");
//	static { cacheSpiceRunProgram.attachToObject(tool, "Tool Options, Spice tab", "Spice Run Program"); }
	/** Get the spice run program */
	public static String getSpiceRunProgram() { return cacheSpiceRunProgram.getString(); }
	/** Set the spice run program */
	public static void setSpiceRunProgram(String c) { cacheSpiceRunProgram.setString(c); }
	/** Get the spice run program, by default */
	public static String getFactorySpiceRunProgram() { return cacheSpiceRunProgram.getStringFactoryValue(); }

	private static Pref cacheSpiceRunProgramArgs = Pref.makeStringPref("SpiceRunProgramArgs", tool.prefs, "");
//	static { cacheSpiceRunProgramArgs.attachToObject(tool, "Tool Options, Spice tab", "Spice Run Program Args"); }
	/** Get the spice run program args */
	public static String getSpiceRunProgramArgs() { return cacheSpiceRunProgramArgs.getString(); }
	/** Set the spice run program args */
	public static void setSpiceRunProgramArgs(String c) { cacheSpiceRunProgramArgs.setString(c); }
	/** Get the spice run program args, by default */
	public static String getFactorySpiceRunProgramArgs() { return cacheSpiceRunProgramArgs.getStringFactoryValue(); }

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
	/**
	 * Method to return the name of the default Spice parts library.
	 * The Spice parts library is a library of icons that are used in Spice.
	 * @return the name of the default Spice parts library.
	 */
	public static String getFactorySpicePartsLibrary()
	{
		if (cacheSpicePartsLibrary == null)
		{
			String [] libNames = LibFile.getSpicePartsLibraries();
			cacheSpicePartsLibrary = Pref.makeStringPref("SpicePartsLibrary", tool.prefs, libNames[0]);
		}
		return cacheSpicePartsLibrary.getStringFactoryValue();
	}

	private static Pref cacheSpiceHeaderCardInfo = Pref.makeStringPref("SpiceHeaderCardInfo", tool.prefs, "");
//	static { cacheSpiceHeaderCardInfo.attachToObject(tool, "Tools/Spice tab", "Spice header card information"); }
	/**
	 * Method to get the Spice header card specification.
	 * Header cards can come from one of three places, depending on the specification:<BR>
	 * Specification="N O N E XXX" means use built-in header cards (XXX is the former specification).<BR>
	 * Specification="Extension XXX" means use header cards from the file TOPCELL.XXX
	 * where TOPCELL is the name of the top-level cell name and XXX is a specified extension.<BR>
	 * Specification="XXX" means use header cards from the file XXX.
	 * @return the Spice header card specification.
	 */
	public static String getSpiceHeaderCardInfo() { return cacheSpiceHeaderCardInfo.getString(); }
	/**
	 * Method to set the Spice header card specification.
	 * Header cards can come from one of three places, depending on the specification:<BR>
	 * Specification="N O N E XXX" means use built-in header cards (XXX is the former specification).<BR>
	 * Specification="Extension XXX" means use header cards from the file TOPCELL.XXX
	 * where TOPCELL is the name of the top-level cell name and XXX is a specified extension.<BR>
	 * Specification="XXX" means use header cards from the file XXX.
	 * @param spec the Spice header card specification.
	 */
	public static void setSpiceHeaderCardInfo(String spec) { cacheSpiceHeaderCardInfo.setString(spec); }
	/**
	 * Method to get the Spice header card specification, by default.
	 * Header cards can come from one of three places, depending on the specification:<BR>
	 * Specification="N O N E XXX" means use built-in header cards (XXX is the former specification).<BR>
	 * Specification="Extension XXX" means use header cards from the file TOPCELL.XXX
	 * where TOPCELL is the name of the top-level cell name and XXX is a specified extension.<BR>
	 * Specification="XXX" means use header cards from the file XXX.
	 * @return the Spice header card specification, by default.
	 */
	public static String getFactorySpiceHeaderCardInfo() { return cacheSpiceHeaderCardInfo.getStringFactoryValue(); }

	private static Pref cacheSpiceTrailerCardInfo = Pref.makeStringPref("SpiceTrailerCardInfo", tool.prefs, "");
//	static { cacheSpiceTrailerCardInfo.attachToObject(tool, "Tools/Spice tab", "Spice trailer card information"); }
	/**
	 * Method to get the Spice trailer card specification.
	 * Trailer cards can come from one of three places, depending on the specification:<BR>
	 * Specification="N O N E XXX" means use built-in trailer cards (XXX is the former specification).<BR>
	 * Specification="Extension XXX" means use trailer cards from the file TOPCELL.XXX
	 * where TOPCELL is the name of the top-level cell name and XXX is a specified extension.<BR>
	 * Specification="XXX" means use trailer cards from the file XXX.
	 * @return the Spice trailer card specification.
	 */
	public static String getSpiceTrailerCardInfo() { return cacheSpiceTrailerCardInfo.getString(); }
	/**
	 * Method to set the Spice trailer card specification.
	 * Trailer cards can come from one of three places, depending on the specification:<BR>
	 * Specification="N O N E XXX" means use built-in trailer cards (XXX is the former specification).<BR>
	 * Specification="Extension XXX" means use trailer cards from the file TOPCELL.XXX
	 * where TOPCELL is the name of the top-level cell name and XXX is a specified extension.<BR>
	 * Specification="XXX" means use trailer cards from the file XXX.
	 * @param spec the Spice trailer card specification.
	 */
	public static void setSpiceTrailerCardInfo(String spec) { cacheSpiceTrailerCardInfo.setString(spec); }
	/**
	 * Method to get the Spice trailer card specification, by default.
	 * Trailer cards can come from one of three places, depending on the specification:<BR>
	 * Specification="N O N E XXX" means use built-in trailer cards (XXX is the former specification).<BR>
	 * Specification="Extension XXX" means use trailer cards from the file TOPCELL.XXX
	 * where TOPCELL is the name of the top-level cell name and XXX is a specified extension.<BR>
	 * Specification="XXX" means use trailer cards from the file XXX.
	 * @return the Spice trailer card specification, by default.
	 */
	public static String getFactorySpiceTrailerCardInfo() { return cacheSpiceTrailerCardInfo.getStringFactoryValue(); }

	public enum SpiceParasitics
	{
		/** simple parasitics (source/drain area&perimeter) */		SIMPLE(0, "Trans area/perim only"),
		/** RC parasitics to substrate. */							RC_CONSERVATIVE(1, "Conservative RC"),
		/** RC parasitics to substrate or intervening layers. */	RC_PROXIMITY(2, "Proximity-based RC");

		private int code;
		private String name;

		private SpiceParasitics(int val, String name)
		{
			this.code = val;
			this.name = name;
		}
		public int code() { return code; }
		public String toString() { return name;}
	}

	private static Pref cacheSpiceParasiticsLevel = Pref.makeIntPref("SpiceParasiticsLevel", tool.prefs, SpiceParasitics.SIMPLE.code());
	/**
	 * Method to tell the level of parasitics being used by Spice output.
	 * The default is PARASITICS_SIMPLE.
	 * @return the level of parasitics being used by Spice output.
	 */
	public static SpiceParasitics getSpiceParasiticsLevel()
	{
		int curCode = cacheSpiceParasiticsLevel.getInt();
		for (SpiceParasitics p : SpiceParasitics.values())
		{
			if (p.code() == curCode) return p;
		}
		return null;
	}
	/**
	 * Method to set the level of parasitics to be used by Spice output.
	 * @param p the level of parasitics to be used by Spice output.
	 */
	public static void setSpiceParasiticsLevel(SpiceParasitics p) { cacheSpiceParasiticsLevel.setInt(p.code); }
	/**
	 * Method to tell the level of parasitics being used by Spice output, by default.
	 * @return the level of parasitics being used by Spice output, by default.
	 */
	public static SpiceParasitics getFactorySpiceParasiticsLevel()
	{
		int curCode = cacheSpiceParasiticsLevel.getIntFactoryValue();
		for (SpiceParasitics p : SpiceParasitics.values())
		{
			if (p.code() == curCode) return p;
		}
		return null;
	}

	public static Pref cacheParasiticsUseVerboseNaming = Pref.makeBooleanPref("ParasiticsUseVerboseNaming", tool.prefs, true);
	public static boolean isParasiticsUseVerboseNaming() { return cacheParasiticsUseVerboseNaming.getBoolean(); }
	public static void setParasiticsUseVerboseNaming(boolean b) { cacheParasiticsUseVerboseNaming.setBoolean(b); }
	public static boolean isFactoryParasiticsUseVerboseNaming() { return cacheParasiticsUseVerboseNaming.getBooleanFactoryValue(); }

	public static Pref cacheParasiticsBackAnnotateLayout = Pref.makeBooleanPref("ParasiticsBackAnnotateLayout", tool.prefs, false);
	public static boolean isParasiticsBackAnnotateLayout() { return cacheParasiticsBackAnnotateLayout.getBoolean(); }
	public static void setParasiticsBackAnnotateLayout(boolean b) { cacheParasiticsBackAnnotateLayout.setBoolean(b); }
	public static boolean isFactoryParasiticsBackAnnotateLayout() { return cacheParasiticsBackAnnotateLayout.getBooleanFactoryValue(); }

	public static Pref cacheParasiticsExtractPowerGround = Pref.makeBooleanPref("ParasiticsExtractPowerGround", tool.prefs, false);
	public static boolean isParasiticsExtractPowerGround() { return cacheParasiticsExtractPowerGround.getBoolean(); }
	public static void setParasiticsExtractPowerGround(boolean b) { cacheParasiticsExtractPowerGround.setBoolean(b); }
	public static boolean isFactoryParasiticsExtractPowerGround() { return cacheParasiticsExtractPowerGround.getBooleanFactoryValue(); }

	public static Pref cacheParasiticsUseExemptedNetsFile = Pref.makeBooleanPref("UseExemptedNetsFile", tool.prefs, false);
	public static boolean isParasiticsUseExemptedNetsFile() { return cacheParasiticsUseExemptedNetsFile.getBoolean(); }
	public static void setParasiticsUseExemptedNetsFile(boolean b) { cacheParasiticsUseExemptedNetsFile.setBoolean(b); }
	public static boolean isFactoryParasiticsUseExemptedNetsFile() { return cacheParasiticsUseExemptedNetsFile.getBooleanFactoryValue(); }

	public static Pref cacheParasiticsIgnoreExemptedNets = Pref.makeBooleanPref("IgnoreExemptedNets", tool.prefs, true);
	public static boolean isParasiticsIgnoreExemptedNets() { return cacheParasiticsIgnoreExemptedNets.getBoolean(); }
	public static void setParasiticsIgnoreExemptedNets(boolean b) { cacheParasiticsIgnoreExemptedNets.setBoolean(b); }
	public static boolean isFactoryParasiticsIgnoreExemptedNets() { return cacheParasiticsIgnoreExemptedNets.getBooleanFactoryValue(); }

	public static Pref cacheParasiticsExtractsR = Pref.makeBooleanPref("ParasiticsExtractsR", tool.prefs, true);
	public static boolean isParasiticsExtractsR() { return cacheParasiticsExtractsR.getBoolean(); }
	public static void setParasiticsExtractsR(boolean b) { cacheParasiticsExtractsR.setBoolean(b); }
	public static boolean isFactoryParasiticsExtractsR() { return cacheParasiticsExtractsR.getBooleanFactoryValue(); }

	public static Pref cacheParasiticsExtractsC = Pref.makeBooleanPref("ParasiticsExtractsC", tool.prefs, true);
	public static boolean isParasiticsExtractsC() { return cacheParasiticsExtractsC.getBoolean(); }
	public static void setParasiticsExtractsC(boolean b) { cacheParasiticsExtractsC.setBoolean(b); }
	public static boolean isFactoryParasiticsExtractsC() { return cacheParasiticsExtractsC.getBooleanFactoryValue(); }

    public enum SpiceGlobal {
        NONE(0),  /** no special treatment of global signals in Spice output. */
        USEGLOBALBLOCK(1), /** Use .GLOBAL block for global signals in Spice output. */
        USESUBCKTPORTS(2); /** Write ports in .SUBCKT for global signals in Spice output. */
        private int code;
        SpiceGlobal(int c)
        {
            code = c;
        }
        public int getCode() {return code;}
        public static SpiceGlobal find(int c)
        {
            for (SpiceGlobal s : SimulationTool.SpiceGlobal.values())
            {
                if (s.code == c)
                    return s;
            }
            assert(false); // it should never reach this point
            return null;
        }
    }

//    /** no special treatment of global signals in Spice output. */		public static final int SPICEGLOBALSNONE = 0;
//	/** Use .GLOBAL block for global signals in Spice output. */		public static final int SPICEGLOBALSUSEGLOBALBLOCK = 1;
//	/** Write ports in .SUBCKT for global signals in Spice output. */	public static final int SPICEGLOBALSUSESUBCKTPORTS = 2;

	private static Pref cacheGlobalTreatment = Pref.makeIntPref("SpiceGlobalTreatment", tool.prefs,
        SpiceGlobal.USEGLOBALBLOCK.getCode());
	/**
	 * Method to tell how to treat globals in Spice output.
	 * Globals are signals such as power and ground.  The values can be:<BR>
	 * SPICEGLOBALSNONE: no special treatment of globals<BR>
	 * SPICEGLOBALSUSEGLOBALBLOCK: create a .GLOBAL block for globals (the default)<BR>
	 * SPICEGLOBALSUSESUBCKTPORTS: place globals as .SUBCKT parameters<BR>
	 * @return how to treat globals in Spice output.
	 */
	public static SimulationTool.SpiceGlobal getSpiceGlobalTreatment() { return SpiceGlobal.find(cacheGlobalTreatment.getInt()); }
	/**
	 * Method to set how to treat globals in Spice output.
	 * Globals are signals such as power and ground.  The values can be:<BR>
	 * SPICEGLOBALSNONE: no special treatment of globals<BR>
	 * SPICEGLOBALSUSEGLOBALBLOCK: create a .GLOBAL block for globals<BR>
	 * SPICEGLOBALSUSESUBCKTPORTS: place globals as .SUBCKT parameters<BR>
	 * @param g how to treat globals in Spice output.
	 */
	public static void setSpiceGlobalTreatment(SimulationTool.SpiceGlobal g) { cacheGlobalTreatment.setInt(g.getCode()); }
	/**
	 * Method to tell how to treat globals in Spice output, by default.
	 * Globals are signals such as power and ground.  The values can be:<BR>
	 * SPICEGLOBALSNONE: no special treatment of globals<BR>
	 * SPICEGLOBALSUSEGLOBALBLOCK: create a .GLOBAL block for globals (the default)<BR>
	 * SPICEGLOBALSUSESUBCKTPORTS: place globals as .SUBCKT parameters<BR>
	 * @return how to treat globals in Spice output, by default.
	 */
	public static SimulationTool.SpiceGlobal getFactorySpiceGlobalTreatment() { return SpiceGlobal.find(cacheGlobalTreatment.getIntFactoryValue()); }

    private static Pref cacheSpiceWritePwrGndInTopCell = Pref.makeBooleanPref("cacheSpiceWritePwrGndInTopCell", tool.prefs, true);
	/**
	 * Method to tell whether or not to write power and ground in Spice top cell section.
	 * The default is true. True is the default behavior until now.
	 * @return true to write power and ground in Spice top cell section.
	 */
	public static boolean isSpiceWritePwrGndInTopCell() { return cacheSpiceWritePwrGndInTopCell.getBoolean(); }
	/**
	 * Method to set whether or not to write power and ground in Spice top cell section.
	 * @param g true to write power and ground in Spice top cell section.
	 */
	public static void setSpiceWritePwrGndInTopCell(boolean g) { cacheSpiceWritePwrGndInTopCell.setBoolean(g); }
	/**
	 * Method to tell whether or not to write power and ground in Spice top cell section, by default.
	 * @return true to write power and ground in Spice top cell section.
	 */
	public static boolean isFactorySpiceWritePwrGndInTopCell() { return cacheSpiceWritePwrGndInTopCell.getBooleanFactoryValue(); }


    private static Pref cacheSpiceUseCellParameters = Pref.makeBooleanPref("SpiceUseCellParameters", tool.prefs, false);
//	static { cacheSpiceForceGlobalPwrGnd.attachToObject(tool, "Tools/Spice tab", "Spice uses cell parameters"); }
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
	/**
	 * Method to tell whether or not to use cell parameters in Spice output, by default.
	 * When cell parameters are used, any parameterized cell is written many times,
	 * once for each combination of parameter values.
	 * @return true to use cell parameters in Spice output, by default.
	 */
	public static boolean isFactorySpiceUseCellParameters() { return cacheSpiceUseCellParameters.getBooleanFactoryValue(); }

	private static Pref cacheSpiceWriteTransSizeInLambda = Pref.makeBooleanPref("SpiceWriteTransSizeInLambda", tool.prefs, false);
//	static { cacheSpiceWriteTransSizeInLambda.attachToObject(tool, "Tools/Spice tab", "Spice writes transistor sizes in lambda"); }
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
	/**
	 * Method to tell whether or not to write transistor sizes in "lambda" grid units in Spice output, by default.
	 * Lambda grid units are the basic units of design.
	 * When writing in these units, the values are simpler, but an overriding scale factor brings them to the proper size.
	 * @return true to write transistor sizes in "lambda" grid units in Spice output, by default.
	 */
	public static boolean isFactorySpiceWriteTransSizeInLambda() { return cacheSpiceWriteTransSizeInLambda.getBooleanFactoryValue(); }

	private static Pref cacheSpiceWriteSubcktTopCell = Pref.makeBooleanPref("SpiceWriteSubcktTopCell", tool.prefs, false);
	public static boolean isSpiceWriteSubcktTopCell() { return cacheSpiceWriteSubcktTopCell.getBoolean(); }
	public static void setSpiceWriteSubcktTopCell(boolean b) { cacheSpiceWriteSubcktTopCell.setBoolean(b); }
	public static boolean isFactorySpiceWriteSubcktTopCell() { return cacheSpiceWriteSubcktTopCell.getBooleanFactoryValue(); }

	private static Pref cacheSpiceWriteTopCellInstance = Pref.makeBooleanPref("SpiceWriteTopCellInstance", tool.prefs, true);
	public static boolean isSpiceWriteTopCellInstance() { return cacheSpiceWriteTopCellInstance.getBoolean(); }
	public static void setSpiceWriteTopCellInstance(boolean b) { cacheSpiceWriteTopCellInstance.setBoolean(b); }

	private static Pref cacheSpiceWriteEmptySubckts = Pref.makeBooleanPref("SpiceWriteEmptySubckts", tool.prefs, true);
	public static boolean isSpiceWriteEmtpySubckts() { return cacheSpiceWriteEmptySubckts.getBoolean(); }
	public static void setSpiceWriteEmptySubckts(boolean b) { cacheSpiceWriteEmptySubckts.setBoolean(b); }

	private static Pref cacheSpiceWriteFinalDotEnd = Pref.makeBooleanPref("SpiceWriteFinalDotEnd", tool.prefs, true);
	public static boolean isSpiceWriteFinalDotEnd() { return cacheSpiceWriteFinalDotEnd.getBoolean(); }
	public static void setSpiceWriteFinalDotEnd(boolean b) { cacheSpiceWriteFinalDotEnd.setBoolean(b); }
	public static boolean isFactorySpiceWriteFinalDotEnd() { return cacheSpiceWriteFinalDotEnd.getBooleanFactoryValue(); }

	private static Pref cachedSpiceIgnoreParasiticResistors = Pref.makeBooleanPref("SpiceIgnoreParasiticResistors", tool.prefs, false);
	public static boolean isSpiceIgnoreParasiticResistors() { return cachedSpiceIgnoreParasiticResistors.getBoolean(); }
	public static void setSpiceIgnoreParasiticResistors(boolean b) { cachedSpiceIgnoreParasiticResistors.setBoolean(b); }

    private static Pref cachedSpiceIgnoreModelFiles = Pref.makeBooleanPref("SpiceIgnoreModelFiles", tool.prefs, false);
    public static boolean isSpiceIgnoreModelFiles() { return cachedSpiceIgnoreModelFiles.getBoolean(); }
    public static void setSpiceIgnoreModelFiles(boolean b) { cachedSpiceIgnoreModelFiles.setBoolean(b); }
    public static boolean isFactorySpiceIgnoreModelFiles() { return cachedSpiceIgnoreModelFiles.getBooleanFactoryValue(); }

    private static Pref cacheSpiceExtractedNetDelimiter = Pref.makeStringPref("SpiceExtractedNetDelimiter", tool.prefs, ":");
    public static String getSpiceExtractedNetDelimiter() { return cacheSpiceExtractedNetDelimiter.getString(); }
    public static void setSpiceExtractedNetDelimiter(String s) { cacheSpiceExtractedNetDelimiter.setString(s); }
	public static String getFactorySpiceExtractedNetDelimiter() { return cacheSpiceExtractedNetDelimiter.getStringFactoryValue(); }

}
