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

import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.DialogOpenFile;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.Prefs;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.simulation.Simulation;

/**
 * Class for user-level changes to the circuit.
 */
public class Spice
{
	private static Spice only = new Spice();

	// constructor, used once
	Spice()
	{
		// see if default preferences are set
		if (!Prefs.exists("SpiceEngine")) setEngine("Spice 3");
		if (!Prefs.exists("SpiceLevel")) setLevel("1");
		if (!Prefs.exists("SpiceUseNodeNames")) setUseNodeNames(true);
		if (!Prefs.exists("SpiceUseParasitics")) setUseParasitics(true);
	}

	public static String getEngine() { return Prefs.getStringOption("SpiceEngine"); }
	public static void setEngine(String engine) { Prefs.setStringOption("SpiceEngine", engine); }

	public static String getLevel() { return Prefs.getStringOption("SpiceLevel"); }
	public static void setLevel(String engine) { Prefs.setStringOption("SpiceLevel", engine); }

	public static String getOutputFormat() { return Prefs.getStringOption("SpiceOutputFormat"); }
	public static void setOutputFormat(String engine) { Prefs.setStringOption("SpiceOutputFormat", engine); }

	public static boolean isUseParasitics() { return Prefs.getBooleanOption("SpiceUseParasitics"); }
	public static void setUseParasitics(boolean v) { Prefs.setBooleanOption("SpiceUseParasitics", v); }

	public static boolean isUseNodeNames() { return Prefs.getBooleanOption("SpiceUseNodeNames"); }
	public static void setUseNodeNames(boolean v) { Prefs.setBooleanOption("SpiceUseNodeNames", v); }

	public static boolean isForceGlobalPwrGnd() { return Prefs.getBooleanOption("SpiceForceGlobalPwrGnd"); }
	public static void setForceGlobalPwrGnd(boolean v) { Prefs.setBooleanOption("SpiceForceGlobalPwrGnd", v); }

	public static boolean isUseCellParameters() { return Prefs.getBooleanOption("SpiceUseCellParameters"); }
	public static void setUseCellParameters(boolean v) { Prefs.setBooleanOption("SpiceUseCellParameters", v); }

	public static boolean isWriteTransSizeInLambda() { return Prefs.getBooleanOption("SpiceWriteTransSizeInLambda"); }
	public static void setWriteTransSizeInLambda(boolean v) { Prefs.setBooleanOption("SpiceWriteTransSizeInLambda", v); }

	public static void writeSpiceDeck()
	{
		EditWindow wnd = TopLevel.getCurrentEditWindow();
		if (wnd == null) return;
		if (wnd.getCell() == null) return;
		String fileName = DialogOpenFile.ELIB.chooseOutputFile(wnd.getCell().getProtoName());
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
		}

		public void doIt()
		{
			System.out.println("Can't write Spice decks yet");
		}
	}

}
