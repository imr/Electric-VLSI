/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LENetlister.java
 * Written by Jonathan Gainsley, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.logicaleffort;

import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Nodable;

public interface LENetlister {

    /** Call to start netlisting */
    public void netlist(Cell cell, VarContext context);

    /** Call to stop or interrupt netlisting */
    public void done();

    /**
     * Call to size netlist with the specified algorithm
     * @return true if successful, false otherwise
     */
    public boolean size(LESizer.Alg algorithm);

    /** Call to update and save sizes */
    public void updateSizes();

    // ---------------------------- statistics ---------------------------------

    /** print the results for the Nodable
     * @return true if successful, false otherwise */
    public boolean printResults(Nodable no, VarContext context);

}
