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
import com.sun.electric.database.text.Pref;
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

	private static Pref cacheVerilogUseAssign = Simulation.tool.makeBooleanPref("VerilogUseAssign", false);
	public static boolean getVerilogUseAssign() { return cacheVerilogUseAssign.getBoolean(); }
	public static void setVerilogUseAssign(boolean use) { cacheVerilogUseAssign.setBoolean(use); }

	private static Pref cacheVerilogUseTrireg = Simulation.tool.makeBooleanPref("VerilogUseTrireg", false);
	public static boolean getVerilogUseTrireg() { return cacheVerilogUseTrireg.getBoolean(); }
	public static void setVerilogUseTrireg(boolean use) { cacheVerilogUseTrireg.setBoolean(use); }

	/****************************** CDL OPTIONS ******************************/

	private static Pref cacheCDLLibName = Simulation.tool.makeStringPref("CDLLibName", "");
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

	private static Pref cacheCDLLibPath = Simulation.tool.makeStringPref("CDLLibPath", "");
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

	private static Pref cacheCDLConvertBrackets = Simulation.tool.makeBooleanPref("CDLConvertBrackets", false);
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

	private static Pref cacheSpiceEngine = Simulation.tool.makeIntPref("SpiceEngine", 1);
	public static int getSpiceEngine() { return cacheSpiceEngine.getInt(); }
	public static void setSpiceEngine(int engine) { cacheSpiceEngine.setInt(engine); }

	private static Pref cacheSpiceLevel = Simulation.tool.makeStringPref("SpiceLevel", "1");
	public static String getSpiceLevel() { return cacheSpiceLevel.getString(); }
	public static void setSpiceLevel(String level) { cacheSpiceLevel.setString(level); }

	private static Pref cacheSpiceOutputFormat = Simulation.tool.makeStringPref("SpiceOutputFormat", "Standard");
	public static String getSpiceOutputFormat() { return cacheSpiceOutputFormat.getString(); }
	public static void setSpiceOutputFormat(String format) { cacheSpiceOutputFormat.setString(format); }

	private static Pref cacheSpicePartsLibrary = null;
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

	private static Pref cacheSpiceHeaderCardInfo = Simulation.tool.makeStringPref("SpiceHeaderCardInfo", "");
	public static String getSpiceHeaderCardInfo() { return cacheSpiceHeaderCardInfo.getString(); }
	public static void setSpiceHeaderCardInfo(String info) { cacheSpiceHeaderCardInfo.setString(info); }

	private static Pref cacheSpiceTrailerCardInfo = Simulation.tool.makeStringPref("SpiceTrailerCardInfo", "");
	public static String getSpiceTrailerCardInfo() { return cacheSpiceTrailerCardInfo.getString(); }
	public static void setSpiceTrailerCardInfo(String info) { cacheSpiceTrailerCardInfo.setString(info); }

	private static Pref cacheSpiceUseParasitics = Simulation.tool.makeBooleanPref("SpiceUseParasitics", true);
	public static boolean isSpiceUseParasitics() { return cacheSpiceUseParasitics.getBoolean(); }
	public static void setSpiceUseParasitics(boolean v) { cacheSpiceUseParasitics.setBoolean(v); }

	private static Pref cacheSpiceUseNodeNames = Simulation.tool.makeBooleanPref("SpiceUseNodeNames", true);
	public static boolean isSpiceUseNodeNames() { return cacheSpiceUseNodeNames.getBoolean(); }
	public static void setSpiceUseNodeNames(boolean v) { cacheSpiceUseNodeNames.setBoolean(v); }

	private static Pref cacheSpiceForceGlobalPwrGnd = Simulation.tool.makeBooleanPref("SpiceForceGlobalPwrGnd", false);
	public static boolean isSpiceForceGlobalPwrGnd() { return cacheSpiceForceGlobalPwrGnd.getBoolean(); }
	public static void setSpiceForceGlobalPwrGnd(boolean v) { cacheSpiceForceGlobalPwrGnd.setBoolean(v); }

	private static Pref cacheSpiceUseCellParameters = Simulation.tool.makeBooleanPref("SpiceUseCellParameters", false);
	public static boolean isSpiceUseCellParameters() { return cacheSpiceUseCellParameters.getBoolean(); }
	public static void setSpiceUseCellParameters(boolean v) { cacheSpiceUseCellParameters.setBoolean(v); }

	private static Pref cacheSpiceWriteTransSizeInLambda = Simulation.tool.makeBooleanPref("SpiceWriteTransSizeInLambda", false);
	public static boolean isSpiceWriteTransSizeInLambda() { return cacheSpiceWriteTransSizeInLambda.getBoolean(); }
	public static void setSpiceWriteTransSizeInLambda(boolean v) { cacheSpiceWriteTransSizeInLambda.setBoolean(v); }
}
