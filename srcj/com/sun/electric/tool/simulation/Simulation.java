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

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.lib.LibFile;
import com.sun.electric.tool.Tool;

//import java.util.prefs.Preferences;
//import java.util.prefs.BackingStoreException;

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

	/****************************** VERILOG OPTIONS ******************************/

	private static Tool.Pref cacheVerilogUseAssign = Simulation.tool.makeBooleanPref("VerilogUseAssign", false);
	public static boolean getVerilogUseAssign() { return cacheVerilogUseAssign.getBoolean(); }
	public static void setVerilogUseAssign(boolean use) { cacheVerilogUseAssign.setBoolean(use); }

	private static Tool.Pref cacheVerilogUseTrireg = Simulation.tool.makeBooleanPref("VerilogUseTrireg", false);
	public static boolean getVerilogUseTrireg() { return cacheVerilogUseTrireg.getBoolean(); }
	public static void setVerilogUseTrireg(boolean use) { cacheVerilogUseTrireg.setBoolean(use); }

	/****************************** SPICE OPTIONS ******************************/

	private static Tool.Pref cacheSpiceEngine = Simulation.tool.makeStringPref("SpiceEngine", "Spice 3");
	public static String getSpiceEngine() { return cacheSpiceEngine.getString(); }
	public static void setSpiceEngine(String engine) { cacheSpiceEngine.setString(engine); }

	private static Tool.Pref cacheSpiceLevel = Simulation.tool.makeStringPref("SpiceLevel", "1");
	public static String getSpiceLevel() { return cacheSpiceLevel.getString(); }
	public static void setSpiceLevel(String level) { cacheSpiceLevel.setString(level); }

	private static Tool.Pref cacheSpiceOutputFormat = Simulation.tool.makeStringPref("SpiceOutputFormat", "Standard");
	public static String getSpiceOutputFormat() { return cacheSpiceOutputFormat.getString(); }
	public static void setSpiceOutputFormat(String format) { cacheSpiceOutputFormat.setString(format); }

	private static Tool.Pref cacheSpicePartsLibrary = null;
	public static String getSpicePartsLibrary()
	{
		if (cacheSpicePartsLibrary == null)
		{
			String [] libNames = LibFile.getSpicePartsLibraries();
			cacheSpicePartsLibrary = Simulation.tool.makeStringPref("SpicePartsLibrary", libNames[0]);
		}
		return cacheSpicePartsLibrary.getString();
	}
	public static void setSpicePartsLibrary(String parts)
	{
		if (cacheSpicePartsLibrary == null)
		{
			String [] libNames = LibFile.getSpicePartsLibraries();
			cacheSpicePartsLibrary = Simulation.tool.makeStringPref("SpicePartsLibrary", libNames[0]);
		}
		cacheSpicePartsLibrary.setString(parts);
	}

	private static Tool.Pref cacheSpiceHeaderCardInfo = Simulation.tool.makeStringPref("SpiceHeaderCardInfo", "");
	public static String getSpiceHeaderCardInfo() { return cacheSpiceHeaderCardInfo.getString(); }
	public static void setSpiceHeaderCardInfo(String info) { cacheSpiceHeaderCardInfo.setString(info); }

	private static Tool.Pref cacheSpiceTrailerCardInfo = Simulation.tool.makeStringPref("SpiceTrailerCardInfo", "");
	public static String getSpiceTrailerCardInfo() { return cacheSpiceTrailerCardInfo.getString(); }
	public static void setSpiceTrailerCardInfo(String info) { cacheSpiceTrailerCardInfo.setString(info); }

	private static Tool.Pref cacheSpiceUseParasitics = Simulation.tool.makeBooleanPref("SpiceUseParasitics", true);
	public static boolean isSpiceUseParasitics() { return cacheSpiceUseParasitics.getBoolean(); }
	public static void setSpiceUseParasitics(boolean v) { cacheSpiceUseParasitics.setBoolean(v); }

	private static Tool.Pref cacheSpiceUseNodeNames = Simulation.tool.makeBooleanPref("SpiceUseNodeNames", true);
	public static boolean isSpiceUseNodeNames() { return cacheSpiceUseNodeNames.getBoolean(); }
	public static void setSpiceUseNodeNames(boolean v) { cacheSpiceUseNodeNames.setBoolean(v); }

	private static Tool.Pref cacheSpiceForceGlobalPwrGnd = Simulation.tool.makeBooleanPref("SpiceForceGlobalPwrGnd", false);
	public static boolean isSpiceForceGlobalPwrGnd() { return cacheSpiceForceGlobalPwrGnd.getBoolean(); }
	public static void setSpiceForceGlobalPwrGnd(boolean v) { cacheSpiceForceGlobalPwrGnd.setBoolean(v); }

	private static Tool.Pref cacheSpiceUseCellParameters = Simulation.tool.makeBooleanPref("SpiceUseCellParameters", false);
	public static boolean isSpiceUseCellParameters() { return cacheSpiceUseCellParameters.getBoolean(); }
	public static void setSpiceUseCellParameters(boolean v) { cacheSpiceUseCellParameters.setBoolean(v); }

	private static Tool.Pref cacheSpiceWriteTransSizeInLambda = Simulation.tool.makeBooleanPref("SpiceWriteTransSizeInLambda", false);
	public static boolean isSpiceWriteTransSizeInLambda() { return cacheSpiceWriteTransSizeInLambda.getBoolean(); }
	public static void setSpiceWriteTransSizeInLambda(boolean v) { cacheSpiceWriteTransSizeInLambda.setBoolean(v); }
}
