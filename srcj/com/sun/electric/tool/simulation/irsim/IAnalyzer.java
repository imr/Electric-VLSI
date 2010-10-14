/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IAnalyzer.java
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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

/**
 * IRSIM simulator interface that is implemented by plugin
 */
public interface IAnalyzer {
    
	/**
	 * Main entry point to start simulating a cell.
	 * @param cell the cell to simulate.
     * @param varContex the context to evaluate Variables
	 * @param fileName the file with the input deck (null to generate one)
     * @param ip IRSIM preferences
     * @param doNow true if invoke in the current thread else invoke in the Swing thread
	 */
	public void simulateCell(Cell cell, VarContext context, String fileName, IRSIMPreferences ip, boolean doNow);
}
