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
import com.sun.electric.lib.LibFile;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.io.output.Verilog;
import com.sun.electric.tool.user.Highlight;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.WaveformWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.Color;
import java.awt.geom.Rectangle2D;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This is the Simulation Interface tool.
 */
public class Simulation extends Tool
{
	public static class SimData
	{
		// logic levels and signal strengths in the window
		public static final int LOGIC         =  03;
		public static final int LOGIC_LOW     =   0;
		public static final int LOGIC_X       =   1;
		public static final int LOGIC_HIGH    =   2;
		public static final int LOGIC_Z       =   3;
		public static final int STRENGTH      = 014;
		public static final int OFF_STRENGTH  =   0;
		public static final int NODE_STRENGTH =  04;
		public static final int GATE_STRENGTH = 010;
		public static final int VDD_STRENGTH  = 014;

		private Cell cell;
		private OpenFile.Type type;
		private URL fileURL;
		private List signals;
		private double [] commonTime;

		/**
		 * Constructor to build a new Simulation Data object.
		 */
		public SimData()
		{
			signals = new ArrayList();
		}

		/**
		 * Method to get the list of signals in this Simulation Data object.
		 * @return a List of signals.
		 */
		public List getSignals() { return signals; }

		/**
		 * Method to add a new signal to this Simulation Data object.
		 * Signals can be either digital or analog.
		 * @param ws the signal to add.
		 * Instead of a "SimSignal", use either SimDigitalSignal or SimAnalogSignal.
		 */
		public void addSignal(SimSignal ws) { signals.add(ws); }

		/**
		 * Method to construct an array of time values that are common to all signals.
		 * Some simulation data has all of its stimuli at the same time interval for every signal.
		 * To save space, such data can use a common time array, kept in the Simulation Data.
		 * If a signal wants to use its own time values, that can be done by placing the time
		 * array in the signal.
		 * @param numEvents the number of time events in the common time array.
		 */
		public void buildCommonTime(int numEvents) { commonTime = new double[numEvents]; }

		/**
		 * Method to load an entry in the common time array.
		 * @param index the entry number.
		 * @param time the time value at
		 */
		public void setCommonTime(int index, double time) { commonTime[index] = time; }

		public void setCell(Cell cell) { this.cell = cell; }

		public Cell getCell() { return cell; }

		public void setDataType(OpenFile.Type type) { this.type = type; }

		public OpenFile.Type getDataType() { return type; }

		public void setFileURL(URL fileURL) { this.fileURL = fileURL; }

		public URL getFileURL() { return fileURL; }

		/**
		 * Method to compute the time and value bounds of this simulation data.
		 * @return a Rectangle2D that has time bounds in the X part and
		 * value bounds in the Y part.
		 */
		public Rectangle2D getBounds()
		{
			// determine extent of the data
			double lowTime=0, highTime=0, lowValue=0, highValue=0;
			boolean first = true;
			for(Iterator it = signals.iterator(); it.hasNext(); )
			{
				SimSignal sig = (SimSignal)it.next();
				if (sig instanceof SimAnalogSignal)
				{
					SimAnalogSignal as = (SimAnalogSignal)sig;
					for(int i=0; i<as.values.length; i++)
					{
						double time = 0;
						time = as.getTime(i);
						if (first)
						{
							first = false;
							lowTime = highTime = time;
							lowValue = highValue = as.values[i];
						} else
						{
							if (time < lowTime) lowTime = time;
							if (time > highTime) highTime = time;
							if (as.values[i] < lowValue) lowValue = as.values[i];
							if (as.values[i] > highValue) highValue = as.values[i];
						}
					}
				} else if (sig instanceof SimDigitalSignal)
				{
					SimDigitalSignal ds = (SimDigitalSignal)sig;
					if (ds.state == null) continue;
					for(int i=0; i<ds.state.length; i++)
					{
						double time = 0;
						time = ds.getTime(i);
						if (first)
						{
							first = false;
							lowTime = highTime = time;
						} else
						{
							if (time < lowTime) lowTime = time;
							if (time > highTime) highTime = time;
						}
					}
				}
			}
			return new Rectangle2D.Double(lowTime, lowValue, highTime-lowTime, highValue-lowValue);
		}

		public boolean isAnalog()
		{
			if (getSignals().size() > 0)
			{
				SimSignal sSig = (SimSignal)getSignals().get(0);
				if (sSig instanceof SimAnalogSignal) return true;
			}
			return false;
		}
	}

	public static class SimSignal
	{
		public SimSignal(SimData sd)
		{
			this.sd = sd;
			if (sd != null) sd.signals.add(this);
			this.signalColor = Color.RED;
		}

		private String signalName;
		private String signalContext;
		private Color signalColor;
		private SimData sd;
		private boolean useCommonTime;
		private double [] time;
		private List bussedSignals;
		public List tempList;

		public void setSignalName(String signalName) { this.signalName = signalName; }

		public String getSignalName() { return signalName; }

		public void setSignalContext(String signalContext) { this.signalContext = signalContext; }

		public String getSignalContext() { return signalContext; }

		public void setSignalColor(Color signalColor) { this.signalColor = signalColor; }

		public Color getSignalColor() { return signalColor; }

		public int getNumEvents() { return 0; }

		public void buildBussedSignalList() { bussedSignals = new ArrayList(); }

		public List getBussedSignals() { return bussedSignals; }

		public void addToBussedSignalList(SimSignal ws) { bussedSignals.add(ws); }

		public void setCommonTimeUse(boolean useCommonTime) { this.useCommonTime = useCommonTime; }

		public void buildTime(int numEvents) { time = new double[numEvents]; }

		public double getTime(int index)
		{
			if (useCommonTime) return sd.commonTime[index];
			return time[index];
		}

		public double [] getTimeVector() { return time; }

		public void setTimeVector(double [] time) { this.time = time; }

		public void setTime(int index, double t) { time[index] = t; }
	}

	public static class SimAnalogSignal extends SimSignal
	{
		private double [] values;

		public SimAnalogSignal(SimData sd) { super(sd); }

		public void buildValues(int numEvents) { values = new double[numEvents]; }

		public void setValue(int index, double value) { values[index] = value; }

		public double getValue(int index) { return values[index]; }

		public int getNumEvents() { return values.length; }
	}

	public static class SimDigitalSignal extends SimSignal
	{
		private int [] state;

		public SimDigitalSignal(SimData sd) { super(sd); }

		public void buildState(int numEvents) { state = new int[numEvents]; }

		public void setState(int index, int st) { state[index] = st; }

		public int getState(int index) { return state[index]; }

		public int [] getStateVector() { return state; }

		public void setStateVector(int [] state) { this.state = state; }

		public int getNumEvents() { return state.length; }
	}

	/** the Simulation tool. */		public static Simulation tool = new Simulation();

	/** key of Variable holding flag for weak nodes. */		public static final Variable.Key WEAK_NODE_KEY = ElectricObject.newKey("SIM_weak_node");

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
//		setOn();
	}

	/**
	 * Method to set a Spice model on the selected node.
	 */
	public static void setSpiceModel()
	{
		NodeInst ni = (NodeInst)Highlight.getOneElectricObject(NodeInst.class);
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
			var.setDisplay();
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
		List list = Highlight.getHighlighted(false, true);
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
						var.setDisplay();
						break;
					case 1:		// set to "trireg"
						var = ai.newVar(Verilog.WIRE_TYPE_KEY, "trireg");
						var.setDisplay();
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
		NodeInst ni = (NodeInst)Highlight.getOneElectricObject(NodeInst.class);
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
				Variable var = ni.newVar(Simulation.WEAK_NODE_KEY, "Weak");
				var.setDisplay();
			} else
			{
				if (ni.getVar(Simulation.WEAK_NODE_KEY) != null)
					ni.delVar(Simulation.WEAK_NODE_KEY);
			}
			return true;
		}
	}

	public static void showSimulationData(SimData sd, WaveformWindow ww)
	{
		// if the window already exists, update the data
		if (ww != null)
		{
			ww.setSimData(sd);
			return;
		}

		// determine extent of the data
		Rectangle2D bounds = sd.getBounds();
		double lowTime = bounds.getMinX();
		double highTime = bounds.getMaxX();
		double lowValue = bounds.getMinY();
		double highValue = bounds.getMaxY();
		double timeRange = highTime - lowTime;

		WindowFrame wf = WindowFrame.createWaveformWindow(sd);
		ww = (WaveformWindow)wf.getContent();
		ww.setMainTimeCursor(timeRange*0.2 + lowTime);
		ww.setExtensionTimeCursor(timeRange*0.8 + lowTime);
		ww.setDefaultTimeRange(lowTime, highTime);

		if (sd.cell != null)
		{
			Variable var = sd.cell.getVar(WaveformWindow.WINDOW_SIGNAL_ORDER);
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
						for(int j=0; j<sd.signals.size(); j++)
						{
							Simulation.SimSignal sSig= (Simulation.SimSignal)sd.signals.get(j);
							String aSigName = sSig.getSignalName();
							if (sSig.getSignalContext() != null) aSigName = sSig.getSignalContext() + aSigName;
							if (sigName.equals(aSigName))
							{
								if (firstSignal)
								{
									firstSignal = false;
									wp = new WaveformWindow.Panel(ww, isAnalog);
									if (isAnalog) wp.setValueRange(lowValue, highValue);
									wp.makeSelectedPanel();
									showedSomething = true;
								}
								new WaveformWindow.Signal(wp, sSig);
								break;
							}
						}
						if (tabPos < 0) break;
					}
				}
				if (showedSomething) return;
			}
		}

		// put the first waveform panels in it
		if (sd.signals.size() > 0)
		{
			Simulation.SimSignal sSig = (Simulation.SimSignal)sd.signals.get(0);
			boolean isAnalog = false;
			if (sSig instanceof SimAnalogSignal) isAnalog = true;
			if (isAnalog)
			{
				WaveformWindow.Panel wp = new WaveformWindow.Panel(ww, isAnalog);
				wp.setValueRange(lowValue, highValue);
				wp.makeSelectedPanel();
			} else
			{
				// put all top-level signals in
				for(int i=0; i<sd.signals.size(); i++)
				{
					Simulation.SimDigitalSignal sDSig = (Simulation.SimDigitalSignal)sd.signals.get(i);
					if (sDSig.getSignalContext() != null) continue;
					WaveformWindow.Panel wp = new WaveformWindow.Panel(ww, false);
					wp.makeSelectedPanel();
					new WaveformWindow.Signal(wp, sDSig);
				}
			}
		}
		ww.getPanel().validate();
	}

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
    static { cacheCDLLibName.attachToObject(Simulation.tool, "IO/CDL tab", "Cadence library name"); }
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
    static { cacheCDLLibPath.attachToObject(Simulation.tool, "IO/CDL tab", "Cadence library path"); }
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
    static { cacheCDLConvertBrackets.attachToObject(Simulation.tool, "IO/CDL tab", "CDL converts brackets"); }
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

	/****************************** SPICE OPTIONS ******************************/

	/** Spice 2 engine. */		public static final int SPICE_ENGINE_2 = 0;
	/** Spice 3 engine. */		public static final int SPICE_ENGINE_3 = 1;
	/** HSpice engine. */		public static final int SPICE_ENGINE_H = 2;
	/** PSpice engine. */		public static final int SPICE_ENGINE_P = 3;
	/** GNUCap engine. */		public static final int SPICE_ENGINE_G = 4;
	/** SmartSpice engine. */	public static final int SPICE_ENGINE_S = 5;

	private static Pref cacheSpiceEngine = Pref.makeIntPref("SpiceEngine", Simulation.tool.prefs, 1);
	static
	{
		Pref.Meaning m = cacheSpiceEngine.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice engine");
		m.setTrueMeaning(new String[] {"Spice 2", "Spice 3", "HSpice", "PSpice", "GNUCap", "SmartSpice"});
	}
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
    static { cacheSpiceLevel.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice level"); }
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
    static { cacheSpiceOutputFormat.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice output format"); }
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
    static { cacheSpiceHeaderCardInfo.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice header card information"); }
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
    static { cacheSpiceTrailerCardInfo.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice trailer card information"); }
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
    static { cacheSpiceUseParasitics.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice uses parasitics"); }
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
    static { cacheSpiceUseNodeNames.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice uses node names"); }
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
    static { cacheSpiceForceGlobalPwrGnd.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice forces global VDD/GND"); }
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
    static { cacheSpiceForceGlobalPwrGnd.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice uses cell parameters"); }
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
    static { cacheSpiceWriteTransSizeInLambda.attachToObject(Simulation.tool, "Tools/Spice tab", "Spice writes transistor sizes in lambda"); }
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
