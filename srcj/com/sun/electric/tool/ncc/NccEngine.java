/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NccEngine.java
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
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.Iterator;
import java.io.File;
import java.io.FileOutputStream;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.generator.layout.LayoutLib; 
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.ncc.lists.*;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.trees.EquivRecord;
import com.sun.electric.tool.ncc.jemNets.NccNetlist;
import com.sun.electric.tool.ncc.basic.*;
import com.sun.electric.tool.ncc.basic.Messenger;
import com.sun.electric.tool.ncc.processing.ExportChecker;
import com.sun.electric.tool.ncc.processing.HierarchyInfo;
import com.sun.electric.tool.ncc.processing.HashCodePartitioning;
import com.sun.electric.tool.ncc.processing.LocalPartitioning;
import com.sun.electric.tool.ncc.processing.SubcircuitInfo;
import com.sun.electric.tool.ncc.processing.SerialParallelMerge;
import com.sun.electric.tool.ncc.strategy.StratDebug;
import com.sun.electric.tool.ncc.strategy.StratResult;
import com.sun.electric.tool.ncc.strategy.StratCount;
import com.sun.electric.tool.ncc.strategy.StratCheck;
import com.sun.electric.tool.ncc.strategy.StratCheckSizes;

public class NccEngine {
	// ------------------------------ private data ----------------------------
	private NccGlobals globals;

	// ----------------------- private methods --------------------------------
	private List buildNccNetlists(List cells, List contexts, List netlists, 
	                              HierarchyInfo hierInfo) {
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
			NccNetlist nccList = 
				new NccNetlist(cell, context, netlist, hierInfo, globals);
			nccLists.add(nccList);
		}
		return nccLists;
	}
	
	private boolean designsMatch(HierarchyInfo hierInfo) {
		if (globals.getRoot()==null) {
			globals.println("empty cell");
			return true;
		} else {
			ExportChecker expCheck =  new ExportChecker(globals);
			boolean expNamesOK = expCheck.matchByName();

			expCheck.saveInfoNeededToMakeMeASubcircuit(hierInfo);
			
			EquivRecord root = globals.getRoot();
			StratCheck.doYourJob(root, globals);
			StratCount.doYourJob(root, globals);

			SerialParallelMerge.doYourJob(globals);
			StratCheck.doYourJob(root, globals);
			StratCount.doYourJob(root, globals);

			boolean localOK = LocalPartitioning.doYourJob(globals); 
			if (!localOK) return false;
			
			boolean topoOK = HashCodePartitioning.doYourJob(globals);

			boolean expTopoOK = 
				expCheck.ensureExportsWithMatchingNamesAreOnEquivalentNets();

			boolean sizesOK = StratCheckSizes.doYourJob(globals);
			
			boolean OK = localOK && expNamesOK && topoOK && expTopoOK && sizesOK; 
			if (!topoOK) StratDebug.doYourJob(globals);
			return OK;
		}
	}
	
	private boolean areEquivalent(List cells, List contexts, 
					  		      List netlists, HierarchyInfo hierInfo, 
					  		      NccOptions options) {
		globals = new NccGlobals(options);
		
		globals.println("****************************************"+					  		
		                "****************************************");					  		

		List nccNetlists = 
			buildNccNetlists(cells, contexts, netlists, hierInfo);
		globals.setInitialNetlists(nccNetlists);
		boolean match = designsMatch(hierInfo);

		globals.println("****************************************"+					  		
		                "****************************************");
		return match;		              					  				
	}

	// -------------------------- public methods ------------------------------
	/** 
	 * Check to see if all cells are electrically equivalent.  Note that
	 * the NCC engine can compare any number of Cells at the same time.
	 * @param hierCompInfo Information needed to perform hierarchical
	 * netlist comparison. For flat comparisons pass null.
	 * @param cells a list of cells to compare.
	 * @param contexts a list of VarContexts for the corresponding Cell. The
	 * VarContxt is used to evaluate schematic 
	 * variables. Use null if variables don't need to be evaluated. 
	 * @param netlists a list of Netlists for each Cell. This is needed when
	 * the caller wants a netlist with a particular model of connectivity, for
	 * example treat resistors as shorted. Use null if you want to
	 * use the Cell's current netlist. 
	 * @param options NCC options
	 */
	public static boolean compare(List cells, List contexts, List netlists,
	                              HierarchyInfo hierCompInfo, 
	                              NccOptions options) {
		NccEngine ncc = new NccEngine();
		return ncc.areEquivalent(cells, contexts, netlists, hierCompInfo, 
		                         options);
	}
	/** compare two Cells starting at their roots */
	public static boolean compare(Cell cell1, VarContext context1, 
	    						  Cell cell2, VarContext context2, 
	    						  HierarchyInfo hierInfo,
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
		
		return compare(cells, contexts, netlists, hierInfo, options);
	}
}
