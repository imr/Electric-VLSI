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
import com.sun.electric.lib.LibFile;
import com.sun.electric.tool.Tool;

import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;

/**
 * This is the Simulation Interface tool.
 */
public class Simulation extends Tool
{
	// ---------------------- private and protected methods -----------------

	/** the Simulation tool. */		public static Simulation tool = new Simulation();

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

	private static void flushOptions()
	{
		try
		{
	        tool.prefs.flush();
		} catch (BackingStoreException e)
		{
			System.out.println("Failed to save Simulation options");
		}
	}

	/****************************** SPICE OPTIONS ******************************/

	public static String getSpiceEngine() { return tool.prefs.get("SpiceEngine", "Spice 3"); }
	public static void setSpiceEngine(String engine) { tool.prefs.put("SpiceEngine", engine);   flushOptions(); }

	public static String getSpiceLevel() { return tool.prefs.get("SpiceLevel", "1"); }
	public static void setSpiceLevel(String level) { tool.prefs.put("SpiceLevel", level);   flushOptions(); }

	public static String getSpiceOutputFormat() { return tool.prefs.get("SpiceOutputFormat", "Standard"); }
	public static void setSpiceOutputFormat(String format) { tool.prefs.put("SpiceOutputFormat", format);   flushOptions(); }

	public static String getSpicePartsLibrary()
	{
		String [] libNames = LibFile.getSpicePartsLibraries();
		return tool.prefs.get("SpicePartsLibrary", libNames[0]);
	}
	public static void setSpicePartsLibrary(String parts) { tool.prefs.put("SpicePartsLibrary", parts);   flushOptions(); }

	public static String getSpiceHeaderCardInfo() { return tool.prefs.get("SpiceHeaderCardInfo", ""); }
	public static void setSpiceHeaderCardInfo(String info) { tool.prefs.put("SpiceHeaderCardInfo", info);   flushOptions(); }

	public static String getSpiceTrailerCardInfo() { return tool.prefs.get("SpiceTrailerCardInfo", ""); }
	public static void setSpiceTrailerCardInfo(String info) { tool.prefs.put("SpiceTrailerCardInfo", info);   flushOptions(); }

	public static boolean isSpiceUseParasitics() { return tool.prefs.getBoolean("SpiceUseParasitics", true); }
	public static void setSpiceUseParasitics(boolean v) { tool.prefs.putBoolean("SpiceUseParasitics", v);   flushOptions(); }

	public static boolean isSpiceUseNodeNames() { return tool.prefs.getBoolean("SpiceUseNodeNames", true); }
	public static void setSpiceUseNodeNames(boolean v) { tool.prefs.putBoolean("SpiceUseNodeNames", v);   flushOptions(); }

	public static boolean isSpiceForceGlobalPwrGnd() { return tool.prefs.getBoolean("SpiceForceGlobalPwrGnd", false); }
	public static void setSpiceForceGlobalPwrGnd(boolean v) { tool.prefs.putBoolean("SpiceForceGlobalPwrGnd", v);   flushOptions(); }

	public static boolean isSpiceUseCellParameters() { return tool.prefs.getBoolean("SpiceUseCellParameters", false); }
	public static void setSpiceUseCellParameters(boolean v) { tool.prefs.putBoolean("SpiceUseCellParameters", v);   flushOptions(); }

	public static boolean isSpiceWriteTransSizeInLambda() { return tool.prefs.getBoolean("SpiceWriteTransSizeInLambda", false); }
	public static void setSpiceWriteTransSizeInLambda(boolean v) { tool.prefs.putBoolean("SpiceWriteTransSizeInLambda", v);   flushOptions(); }
}
