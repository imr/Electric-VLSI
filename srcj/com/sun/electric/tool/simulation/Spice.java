/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Spice.java
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

import com.sun.electric.lib.LibFile;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.DialogOpenFile;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.simulation.Simulation;

import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;

/**
 * Class for user-level changes to the circuit.
 */
public class Spice
{
	private static Spice only = new Spice();
	private Preferences prefs = Preferences.userNodeForPackage(getClass());

	// constructor, used once
	Spice()
	{
	}
	
	public static void flushOptions()
	{
		try
		{
	        only.prefs.flush();
		} catch (BackingStoreException e)
		{
			System.out.println("Failed to save spice options");
		}
	}

	public static String getEngine() { return only.prefs.get("SpiceEngine", "Spice 3"); }
	public static void setEngine(String engine) { only.prefs.put("SpiceEngine", engine); }

	public static String getLevel() { return only.prefs.get("SpiceLevel", "1"); }
	public static void setLevel(String level) { only.prefs.put("SpiceLevel", level); }

	public static String getOutputFormat() { return only.prefs.get("SpiceOutputFormat", "Standard"); }
	public static void setOutputFormat(String format) { only.prefs.put("SpiceOutputFormat", format); }

	public static String getSpicePartsLibrary()
	{
		String [] libNames = LibFile.getSpicePartsLibraries();
		return only.prefs.get("SpicePartsLibrary", libNames[0]);
	}
	public static void setSpicePartsLibrary(String parts) { only.prefs.put("SpicePartsLibrary", parts); }


	public static String getHeaderCardInfo() { return only.prefs.get("SpiceHeaderCardInfo", ""); }
	public static void setHeaderCardInfo(String info) { only.prefs.put("SpiceHeaderCardInfo", info); }

	public static String getTrailerCardInfo() { return only.prefs.get("SpiceTrailerCardInfo", ""); }
	public static void setTrailerCardInfo(String info) { only.prefs.put("SpiceTrailerCardInfo", info); }

	public static boolean isUseParasitics() { return only.prefs.getBoolean("SpiceUseParasitics", true); }
	public static void setUseParasitics(boolean v) { only.prefs.putBoolean("SpiceUseParasitics", v); }

	public static boolean isUseNodeNames() { return only.prefs.getBoolean("SpiceUseNodeNames", true); }
	public static void setUseNodeNames(boolean v) { only.prefs.putBoolean("SpiceUseNodeNames", v); }

	public static boolean isForceGlobalPwrGnd() { return only.prefs.getBoolean("SpiceForceGlobalPwrGnd", false); }
	public static void setForceGlobalPwrGnd(boolean v) { only.prefs.putBoolean("SpiceForceGlobalPwrGnd", v); }

	public static boolean isUseCellParameters() { return only.prefs.getBoolean("SpiceUseCellParameters", false); }
	public static void setUseCellParameters(boolean v) { only.prefs.putBoolean("SpiceUseCellParameters", v); }

	public static boolean isWriteTransSizeInLambda() { return only.prefs.getBoolean("SpiceWriteTransSizeInLambda", false); }
	public static void setWriteTransSizeInLambda(boolean v) { only.prefs.putBoolean("SpiceWriteTransSizeInLambda", v); }

	public static void writeSpiceDeck()
	{
		EditWindow wnd = EditWindow.getCurrent();
		if (wnd == null) return;
		if (wnd.getCell() == null) return;
		String fileName = DialogOpenFile.chooseOutputFile(DialogOpenFile.SPI, null, wnd.getCell().getProtoName());
		if (fileName != null)
		{
			// start a job to do the deck writing
			WriteSpiceDeck job = new WriteSpiceDeck(fileName);
		}
	}

	/**
	 * Class to write a Spice deck in a new thread.
	 */
	protected static class WriteSpiceDeck extends Job
	{
		String fileName;
		protected WriteSpiceDeck(String fileName)
		{
			super("Write Spice Deck", Simulation.tool, Job.Type.EXAMINE, null, null, Job.Priority.USER);
			this.fileName = fileName;
			this.startJob();
		}

		public void doIt()
		{
			System.out.println("Can't write Spice decks yet");
		}
	}

}
