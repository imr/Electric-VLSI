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

import com.sun.electric.database.text.Pref;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.lib.LibFile;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.output.Spice;
import com.sun.electric.tool.io.output.Verilog;
import com.sun.electric.tool.user.Highlight;

import java.util.Iterator;
import java.util.List;

/**
 * This is the Simulation Interface tool.
 */
public class Simulation extends Tool
{
	// ---------------------- private and protected methods -----------------

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

		public void doIt()
		{
			Variable var = ni.newVar(Spice.SPICE_MODEL_KEY, "SPICE-Model");
			var.setDisplay();
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

		public void doIt()
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

		public void doIt()
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
		}
	}

	/****************************** VERILOG OPTIONS ******************************/

	private static Pref cacheVerilogUseAssign = Pref.makeBooleanPref("VerilogUseAssign", Simulation.tool.prefs, false);
    static { cacheVerilogUseAssign.attachToObject(Simulation.tool, "Tool Options, Verilog tab", "Verilog uses Assign construct"); }
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
    static { cacheVerilogUseTrireg.attachToObject(Simulation.tool, "Tool Options, Verilog tab", "Verilog presumes wire is Trireg"); }
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
    static { cacheCDLLibName.attachToObject(Simulation.tool, "IO Options, CDL tab", "Cadence library name"); }
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
    static { cacheCDLLibPath.attachToObject(Simulation.tool, "IO Options, CDL tab", "Cadence library path"); }
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
    static { cacheCDLConvertBrackets.attachToObject(Simulation.tool, "IO Options, CDL tab", "CDL converts brackets"); }
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
		Pref.Meaning m = cacheSpiceEngine.attachToObject(Simulation.tool, "Tool Options, Spice tab", "Spice engine");
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
    static { cacheSpiceLevel.attachToObject(Simulation.tool, "Tool Options, Spice tab", "Spice level"); }
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
    static { cacheSpiceOutputFormat.attachToObject(Simulation.tool, "Tool Options, Spice tab", "Spice output format"); }
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
    static { cacheSpiceHeaderCardInfo.attachToObject(Simulation.tool, "Tool Options, Spice tab", "Spice header card information"); }
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
    static { cacheSpiceTrailerCardInfo.attachToObject(Simulation.tool, "Tool Options, Spice tab", "Spice trailer card information"); }
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
    static { cacheSpiceUseParasitics.attachToObject(Simulation.tool, "Tool Options, Spice tab", "Spice uses parasitics"); }
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
    static { cacheSpiceUseNodeNames.attachToObject(Simulation.tool, "Tool Options, Spice tab", "Spice uses node names"); }
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
    static { cacheSpiceForceGlobalPwrGnd.attachToObject(Simulation.tool, "Tool Options, Spice tab", "Spice forces global VDD/GND"); }
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
    static { cacheSpiceForceGlobalPwrGnd.attachToObject(Simulation.tool, "Tool Options, Spice tab", "Spice uses cell parameters"); }
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
    static { cacheSpiceWriteTransSizeInLambda.attachToObject(Simulation.tool, "Tool Options, Spice tab", "Spice writes transistor sizes in lambda"); }
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
