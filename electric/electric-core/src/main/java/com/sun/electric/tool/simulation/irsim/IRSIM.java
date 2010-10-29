/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IRSIM.java
 *
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.tool.simulation.irsim;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.io.output.IRSIM.IRSIMPreferences;
import com.sun.electric.util.TextUtils;
import com.sun.electric.util.config.Configuration;

/**
 * Class for interfacing to the IRSIM simulator by reflection.
 */
public class IRSIM
{
	private static boolean IRSIMAvailable;
	private static boolean IRSIMChecked = false;

	/**
     * Method to tell whether the IRSIM simulator is available.
     * IRSIM is packaged separately because it is from Stanford University.
     * This method dynamically figures out whether the IRSIM module is present by using reflection.
     * @return true if the IRSIM simulator is available.
     */
    public static boolean hasIRSIM()
    {
    	if (!IRSIMChecked)
    	{
    		IRSIMChecked = true;
        	IRSIMAvailable = Configuration.lookup(IAnalyzer.class) != null;
        	if (!IRSIMAvailable) TextUtils.recordMissingComponent("IRSIM");
    	}
    	return IRSIMAvailable;
    }

    /**
     * Method to run the IRSIM simulator on a given cell, context or file.
     * Uses reflection to find the IRSIM simulator (if it exists).
     * @param cell the Cell to simulate.
     * @param context the context to the cell to simulate.
     * @param fileName the name of the file with the netlist.  If this is null, simulate the cell.
     * If this is not null, ignore the cell and simulate the file.
     */
    public static void runIRSIM(Cell cell, VarContext context, String fileName, IRSIMPreferences ip, boolean doNow)
    {
        try
        {
            Configuration.lookup(IAnalyzer.class).simulateCell(cell, context, fileName, ip, doNow);
        } catch (Exception e)
        {
            System.out.println("Unable to run the IRSIM simulator");
            e.printStackTrace(System.out);
        }
    }
}