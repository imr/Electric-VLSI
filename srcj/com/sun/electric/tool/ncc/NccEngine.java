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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.jemNets.NccNetlist;
import com.sun.electric.tool.ncc.processing.ExportChecker;
import com.sun.electric.tool.ncc.processing.HashCodePartitioning;
import com.sun.electric.tool.ncc.processing.HierarchyInfo;
import com.sun.electric.tool.ncc.processing.LocalPartitioning;
import com.sun.electric.tool.ncc.processing.ReportHashCodeFailure;
import com.sun.electric.tool.ncc.processing.SerialParallelMerge;
import com.sun.electric.tool.ncc.strategy.StratCheckSizes;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.ncc.trees.EquivRecord;

public class NccEngine {
	// ------------------------------ private data ----------------------------
	private NccGlobals globals;

	// ----------------------- private methods --------------------------------
	private List buildNccNetlists(List cells, List contexts, List netlists, 
	                              boolean blackBox, HierarchyInfo hierInfo) {
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
			NccNetlist nccList = new NccNetlist(cell, context, netlist, 
			                                    hierInfo, blackBox, globals);
			nccLists.add(nccList);
		}
		return nccLists;
	}
	
	private int[] getNetObjCounts(EquivRecord rec, int numCells) {
		int[] counts = new int[numCells];
		if (rec==null) return counts;
		int i=0;
		for (Iterator it=rec.getCircuits(); it.hasNext(); i++) {
			Circuit ckt = (Circuit) it.next();
			counts[i] = ckt.numNetObjs();
		}
		return counts;
	}
	
	/** Ivan wants this print out. */
	private void printWireComponentCounts() {
		int numCells = globals.getNumNetlistsBeingCompared();
		int[] partCounts = getNetObjCounts(globals.getParts(), numCells);
		int[] wireCounts = getNetObjCounts(globals.getWires(), numCells);
		String[] cellNames = globals.getRootCellNames();
		for (int i=0; i<cellNames.length; i++) {
			System.out.println(
				"  Cell: "+cellNames[i]+" has "+wireCounts[i]+" wires and "+
				partCounts[i]+" parts, after series/parallel combination"
			);
		}
	}
	
	private NccResult designsMatch(HierarchyInfo hierInfo, boolean hierInfoOnly) {
		if (globals.getRoot()==null) {
			globals.println("empty cell");
			return new NccResult(true, true, true);
		} else {
			ExportChecker expCheck = new ExportChecker(globals);
			expCheck.markPortsForRenaming();
			
			boolean expNamesOK = expCheck.matchByName();

			if (expNamesOK) {
				expCheck.saveInfoNeededToMakeMeASubcircuit(hierInfo);
			} else { 
				if (hierInfo!=null) hierInfo.purgeCurrentCellGroup();
			}
			// Useless so far
			//expCheck.printExportTypeWarnings();
			
			EquivRecord root = globals.getRoot();
			//StratCheck.doYourJob(root, globals);
			//StratCount.doYourJob(root, globals);

			SerialParallelMerge.doYourJob(globals);
			//StratCheck.doYourJob(root, globals);
			//StratCount.doYourJob(root, globals);
			
			printWireComponentCounts();

			boolean localOK = LocalPartitioning.doYourJob(globals); 
			if (!localOK) return new NccResult(expNamesOK, false, false);

			boolean topoOK = HashCodePartitioning.doYourJob(globals);
			expCheck.suggestPortMatchesBasedOnTopology();

			boolean expTopoOK = 
				expCheck.ensureExportsWithMatchingNamesAreOnEquivalentNets();
            
            boolean sizesOK = StratCheckSizes.doYourJob(globals);
			
			if (!topoOK) ReportHashCodeFailure.reportHashCodeFailure(globals);

			boolean exportsOK = expNamesOK && expTopoOK;
			boolean topologyOK = localOK && topoOK;
			return new NccResult(exportsOK, topologyOK, sizesOK);
		}
	}
	private boolean exportAssertionsOK(List nccNets) {
		boolean exportAssertionsOK = true;
		for (Iterator it=nccNets.iterator(); it.hasNext();) {
			NccNetlist nets = (NccNetlist) it.next();
			exportAssertionsOK &= nets.exportAssertionsOK();
		}
		return exportAssertionsOK;
	}
	private NccResult areEquivalent(List cells, List contexts, 
					  		        List netlists, HierarchyInfo hierInfo,
					  		        boolean blackBox, 
					  		        NccOptions options) {
		globals = new NccGlobals(options);
		
		globals.println("****************************************"+					  		
		                "****************************************");					  		

		// black boxing is implemented by building netlists that are empty 
		// except for their Exports.
		List nccNetlists = 
			buildNccNetlists(cells, contexts, netlists, blackBox, hierInfo);

		/** If export assertions aren't OK then some netlist is invalid */
		if (!exportAssertionsOK(nccNetlists)) {
			return new NccResult(false, false, true);
		}

		globals.setInitialNetlists(nccNetlists);
		NccResult result = designsMatch(hierInfo, false);
NccUtils.hang("NCC completed");
		globals.println("****************************************"+					  		
		                "****************************************");
		return result;		              					  				
	}
	
	private static NccResult compare2(Cell cell1, VarContext context1, 
									  Cell cell2, VarContext context2, 
									  HierarchyInfo hierInfo,
									  boolean blackBox,
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
				
		return compareMany(cells, contexts, netlists, hierInfo, blackBox, 
		                   options);
	}
	private static NccResult compareMany(List cells, List contexts, List netlists,
									     HierarchyInfo hierCompInfo,
									     boolean blackBox, 
									     NccOptions options) {
		NccEngine ncc = new NccEngine();
		return ncc.areEquivalent(cells, contexts, netlists, hierCompInfo, 
								 blackBox, options);
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
	public static NccResult compare(List cells, List contexts, List netlists,
	                                HierarchyInfo hierCompInfo, 
	                                NccOptions options) {
		return compareMany(cells, contexts, netlists, hierCompInfo, false, 
		                   options);
	}
	/** compare two Cells */
	public static NccResult compare(Cell cell1, VarContext context1, 
	                                Cell cell2, VarContext context2, 
	                                HierarchyInfo hierInfo, 
	                                NccOptions options) {
		return 
		  compare2(cell1, context1, cell2, context2, hierInfo, false, options);
	}
	public static boolean buildBlackBoxes(Cell cell1, VarContext ctxt1, 
								          Cell cell2, VarContext ctxt2,
								          HierarchyInfo hierInfo, 
								          NccOptions options) {
		NccResult r = 
			compare2(cell1, ctxt1, cell2, ctxt2, hierInfo, true, options);
		return r.exportMatch();
	}
}
