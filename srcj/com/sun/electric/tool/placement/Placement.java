/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Placement.java
 *
 * Copyright (c) 2009 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.placement;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.variable.UserInterface;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.compaction.Compaction;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.WindowFrame;

/**
 * Class to place cells for better routing.
 */
public class Placement extends Tool
{
	/** the Placement tool. */					private static Placement tool = new Placement();

	/**
	 * The constructor sets up the Placement tool.
	 */
	private Placement()
	{
		super("placement");
	}

	/**
	 * Method to initialize the Placement tool.
	 */
	public void init() {}

    /**
     * Method to retrieve the singleton associated with the Placement tool.
     * @return the Placement tool.
     */
    public static Placement getPlacementTool() { return tool; }

	/**
	 * Method to run placement on the current cell.
	 */
	public static void placeCurrentCell()
	{
		// get cell and network information
		UserInterface ui = Job.getUserInterface();
		Cell cell = ui.needCurrentCell();
		if (cell == null) return;

		new PlaceJob(cell);
	}

	/**
	 * Class to do placement in a new job.
	 */
	private static class PlaceJob extends Job
	{
		private Cell cell;
		private Cell newCell;

		private PlaceJob(Cell cell)
		{
			super("Place cells", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.cell = cell;
			startJob();
		}

		public boolean doIt() throws JobException
		{
			PlacementFrame pla = null;
			String algName = getAlgorithmName();
			for(PlacementFrame pfObj : PlacementFrame.getPlacementAlgorithms())
			{
				if (algName.equals(pfObj.getAlgorithmName())) pla = pfObj;
			}
			if (pla == null) pla = PlacementFrame.getPlacementAlgorithms().get(0);
			newCell = pla.doPlacement(cell);
			fieldVariableChanged("newCell");
            return true;
		}

		public void terminateOK()
		{
			if (newCell != null)
				WindowFrame.createEditWindow(newCell);
		}
	}

	/************************ PREFERENCES ***********************/

	private static Pref cacheAlgorithmName = Pref.makeStringPref("AlgorithmName", tool.prefs, "Min-Cut");
	/**
	 * Method to tell the name of the Placement algorithm to use.
	 * The default is "Min-Cut".
	 * @return the name of the Placement algorithm to use.
	 */
	public static String getAlgorithmName() { return cacheAlgorithmName.getString(); }
	/**
	 * Method to set the name of the Placement algorithm to use.
	 * @param u the name of the Placement algorithm to use.
	 */
	public static void setAlgorithmName(String u) { cacheAlgorithmName.setString(u); }
	/**
	 * Method to tell the default name of the Placement algorithm to use.
	 * @return the default name of the Placement algorithm to use.
	 */
	public static String getFactoryAlgorithmName() { return cacheAlgorithmName.getStringFactoryValue(); }
}
