/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemManager.java
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

/**
 * NccEngine performs an n-way netlist comparison.  NccEngine 
 * must be passed all Ncc options as procedural arguments; it does not access 
 * the User NCC options directly.  This allows programs to call the 
 * engine without becoming involved with how User's options are stored.
 */

package com.sun.electric.tool.ncc;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.generator.layout.LayoutLib; 
import com.sun.electric.database.hierarchy.Cell;

import com.sun.electric.tool.ncc.lists.*;
import com.sun.electric.tool.ncc.trees.JemCircuit;
import com.sun.electric.tool.ncc.trees.JemEquivRecord;
import com.sun.electric.tool.ncc.jemNets.NccNetlist;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.strategy.JemStratFixed;
import com.sun.electric.tool.ncc.strategy.JemStratVariable;
import com.sun.electric.tool.ncc.strategy.JemStratDebug;
import com.sun.electric.tool.ncc.strategy.JemStratResult;
import com.sun.electric.tool.ncc.strategy.JemExportChecker;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.File;
import java.io.FileOutputStream;

public class NccEngine {
	// ------------------------------ private data ----------------------------
	private JemLeafList mismatchList = new JemLeafList();
	private JemLeafList activeList = new JemLeafList();
	private NccGlobals globals;
	private boolean matched;

	// ----------------------- private methods --------------------------------
	private List buildNccNetlists(List cells, List contexts, List netlists) {
		globals.error(cells.size()!=contexts.size() || 
						contexts.size()!=netlists.size(),
						"number of cells, contexts, and netlists must be the same");										
		List nccLists = new ArrayList();
		Iterator itCell, itCon, itNet;
		for (itCell=cells.iterator(),
			 itCon=contexts.iterator(),
			 itNet=netlists.iterator(); itCell.hasNext();) {
			Cell cell = (Cell) itCell.next();
			VarContext context = (VarContext) itCon.next();
			Netlist netlist = (Netlist) itNet.next();
			NccNetlist nccList = new NccNetlist(cell, context, netlist, globals);
			nccLists.add(nccList);
		}
		return nccLists;
	}
	
	private NccEngine(List cells, List contexts, List netlists,
					  NccOptions options) {
		globals = new NccGlobals(options);
				
		globals.print("****************************************");					  		
		globals.println("****************************************");					  		
		List nccNetlists = buildNccNetlists(cells, contexts, netlists);

		globals.setInitialNetlists(nccNetlists);
		
		if (globals.getRoot()==null) {
			globals.println("empty cell");
			matched = true;				  	
		} else {
			JemExportChecker expCheck = null;
			if (options.checkExports) expCheck = new JemExportChecker(globals);
			JemStratFixed.doYourJob(globals);
			JemStratVariable.doYourJob(globals);
			if (options.checkExports) 
			    matched = expCheck.ensureExportsWithMatchingNamesAreOnEquivalentNets();
			//JemStratDebug.doYourJob(globals);
			
			matched &= JemStratResult.doYourJob(mismatchList, activeList, globals);
		    if (!matched) JemStratDebug.doYourJob(globals);
		}

		globals.print("****************************************");					  		
		globals.println("****************************************");					  		
	}

	// -------------------------- public methods ------------------------------
	/** 
	 * Check to see if all cells are electrically equivalent.  Note that
	 * the NCC engine can compare any number of Cells at the same time.
	 * @param cells a list of cells to compare.
	 * @param contexts a list of VarContexts for the corresponding Cell. The
	 * VarContxt is used to evaluate schematic 
	 * variables. Use null if variables don't need to be evaluated. 
	 * @param netlists a list of Netlists for each Cell. This is needed when
	 * the caller wants a netlist with a particular model of connectivity, for
	 * example treat resistors as shorted. Use null if you want to
	 * use the Cell's current netlist. 
	 */
	public static boolean compare(List cells, List contexts, List netlists,
							      NccOptions options) {
		NccEngine ncc = new NccEngine(cells, contexts, netlists, options);
		return ncc.matched;
	}
	/** compare two Cells starting at their roots */
	public static boolean compare(Cell cell1, VarContext context1, 
	    						  Cell cell2, VarContext context2, 
	    						  NccOptions options) {
		ArrayList cells = new ArrayList();
		cells.add(cell1);
		cells.add(cell2);
		ArrayList contexts = new ArrayList();
		contexts.add(context1);
		contexts.add(context2);
		ArrayList netlists = new ArrayList();
		netlists.add(cell1.getNetlist(true));
		netlists.add(cell2.getNetlist(true));
		
		return compare(cells, contexts, netlists, options);
	}
}
