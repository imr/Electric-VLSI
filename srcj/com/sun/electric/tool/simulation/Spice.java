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
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.simulation.Simulation;

/**
 * Class for user-level changes to the circuit.
 */
public class Spice
{
	public static void writeSpiceDeck()
	{
		EditWindow wnd = EditWindow.getCurrent();
		if (wnd == null) return;
		if (wnd.getCell() == null) return;
		String fileName = OpenFile.chooseOutputFile(OpenFile.SPI, null, wnd.getCell().getProtoName());
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
