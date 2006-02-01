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
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.netlist.NccNetlist;
import com.sun.electric.tool.ncc.processing.ExportChecker;
import com.sun.electric.tool.ncc.processing.HashCodePartitioning;
import com.sun.electric.tool.ncc.processing.HierarchyInfo;
import com.sun.electric.tool.ncc.processing.LocalPartitionResult;
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
	private List<NccNetlist> buildNccNetlists(List<Cell> cells, List<VarContext> contexts, List<Netlist> netlists, 
	                              boolean blackBox, HierarchyInfo hierInfo) {
		globals.error(cells.size()!=contexts.size() || 
					  contexts.size()!=netlists.size(),
					  "number of cells, contexts, and netlists must be the same");										
		List<NccNetlist> nccLists = new ArrayList<NccNetlist>();
		Iterator<Cell> itCell;
		Iterator<VarContext> itCon;
		Iterator<Netlist> itNet;
		for (itCell=cells.iterator(),
			 itCon=contexts.iterator(),
			 itNet=netlists.iterator(); itCell.hasNext();) {
			Cell cell = itCell.next();
			VarContext context = itCon.next();
			Netlist netlist = itNet.next();
			NccNetlist nccList = new NccNetlist(cell, context, netlist, 
			                                    hierInfo, blackBox, globals);
			nccLists.add(nccList);
		}
		return nccLists;
	}
	
	private int[] getNetObjCounts(EquivRecord rec, int numCells) {
		int[] counts = new int[numCells];
		// don't blow up if no parts or wires
		if (rec==null) return counts;
		int i=0;
		for (Iterator<Circuit> it=rec.getCircuits(); it.hasNext(); i++) {
			Circuit ckt = it.next();
			counts[i] = ckt.numNetObjs();
		}
		return counts;
	}
	
	/** Ivan wants this print out. */
	private void printWireComponentCounts() {
		int numCells = globals.getNumNetlistsBeingCompared();
		int[] partCounts = getNetObjCounts(globals.getParts(), numCells);
		int[] wireCounts = getNetObjCounts(globals.getWires(), numCells);
		int[] portCounts = getNetObjCounts(globals.getWires(), numCells);
		String[] cellNames = globals.getRootCellNames();
		VarContext[] contexts = globals.getRootContexts();
		for (int i=0; i<cellNames.length; i++) {
			globals.status1(
				"  Cell: "+cellNames[i]+" has "+wireCounts[i]+" wires, "+
				partCounts[i]+" parts, and "+portCounts[i]+
				" ports after series/parallel combination. "+
				"Instance path: "+
				contexts[i].getInstPath("/")
			);
		}
	}
	
	private NccResult designsMatch(HierarchyInfo hierInfo, boolean hierInfoOnly) {
        boolean hasError = globals.cantBuildNetlist();
		if (globals.getRoot()==null || hasError) {
			globals.status2("empty cell or netlist error");
			// Preserve the invariant: "part, wire, and port LeafLists exist" 
			// even if there are no Parts and no Wires OR there is a netlist error. 
			globals.initLeafLists();
			return new NccResult(!hasError, !hasError, true, globals);
		} else {
			Date d0 = new Date();
			ExportChecker expCheck = new ExportChecker(globals);
			expCheck.markPortsForRenaming();
			
			boolean expNamesOK = expCheck.matchByName();

			if (expNamesOK) {
				expCheck.saveInfoNeededToMakeMeASubcircuit(hierInfo);
			} else { 
				if (hierInfo!=null) hierInfo.purgeCurrentCompareList();
			}
			// Useless so far
			//expCheck.printExportTypeWarnings();
			Date d1 = new Date();
			globals.status1("  Export name matching took: "+
			                NccUtils.hourMinSec(d0, d1));
			
			if (globals.userWantsToAbort()) 
				return new NccResult(true, true, true, globals);
			
			SerialParallelMerge.doYourJob(globals);
			Date d2 = new Date();
			globals.status1("  Serial/parallel merge took: "+
					        NccUtils.hourMinSec(d1, d2));

			if (globals.userWantsToAbort()) 
				return new NccResult(true, true, true, globals);

			printWireComponentCounts();

			LocalPartitioning.doYourJob(globals);

			if (globals.userWantsToAbort()) 
				return new NccResult(true, true, true, globals);

			// Tricky: init leaf lists after Local Partitioning because Local
			// Partitioning can make an EquivRecord change from matched to
			// mismatched!
			globals.initLeafLists();

			LocalPartitionResult localRes = new LocalPartitionResult(globals);
			globals.getComparisonResult().setLocalPartitionResult(localRes);

			Date d3 = new Date();
			globals.status1("  Local partitioning took "+ 
					        NccUtils.hourMinSec(d2, d3));


			boolean topoOK = HashCodePartitioning.doYourJob(globals);
			localRes.printErrorReport();
			if (!localRes.matches()) return new NccResult(expNamesOK, false, false, globals);

			if (globals.userWantsToAbort()) 
				return new NccResult(true, true, true, globals);
			
			Date d4 = new Date();
			if (topoOK) expCheck.suggestPortMatchesBasedOnTopology();

			boolean expTopoOK = 
				expCheck.ensureExportsWithMatchingNamesAreOnEquivalentNets();
            Date d5 = new Date();
			globals.status1("  Export checking took "+NccUtils.hourMinSec(d4, d5));
            
			boolean sizesOK = StratCheckSizes.doYourJob(globals);
			Date d6 = new Date();
			globals.status1("  Size checking took "+NccUtils.hourMinSec(d5, d6));
			
			if (!topoOK) ReportHashCodeFailure.reportHashCodeFailure(globals);

			boolean exportsOK = expNamesOK && expTopoOK;
			boolean topologyOK = localRes.matches() && topoOK;
			return new NccResult(exportsOK, topologyOK, sizesOK, globals);
		}
	}
/*    
	private boolean netlistErrors(List nccNets) {
		boolean netlistErrors = false;
		for (Iterator it=nccNets.iterator(); it.hasNext();) {
			NccNetlist nets = it.next();
			netlistErrors |= nets.netlistErrors();
		}
		return netlistErrors;
	}
*/    
	private NccResult areEquivalent(List<Cell> cells, List<VarContext> contexts, 
					  		        List<Netlist> netlists, HierarchyInfo hierInfo,
					  		        boolean blackBox, 
					  		        NccOptions options, Aborter aborter) {
		globals = new NccGlobals(options, aborter);
		
		globals.status2("****************************************"+					  		
		                "****************************************");					  		

		// black boxing is implemented by building netlists that are empty 
		// except for their Exports.
		Date before = new Date();
		List<NccNetlist> nccNetlists = 
			buildNccNetlists(cells, contexts, netlists, blackBox, hierInfo);
		Date after = new Date();
		globals.status1("  NCC net list construction took "+NccUtils.hourMinSec(before, after)+".");
        globals.setInitialNetlists(nccNetlists);
        
		NccResult result = designsMatch(hierInfo, false);

		globals.status2("****************************************"+					  		
		                "****************************************");
		return result;		              					  				
	}
	
	private static NccResult compare2(Cell cell1, VarContext context1, 
									  Cell cell2, VarContext context2, 
									  HierarchyInfo hierInfo,
									  boolean blackBox,
									  NccOptions options,
									  Aborter aborter) {
		ArrayList<Cell> cells = new ArrayList<Cell>();
		cells.add(cell1);
		cells.add(cell2);
		ArrayList<VarContext> contexts = new ArrayList<VarContext>();
		contexts.add(context1);
		contexts.add(context2);
		ArrayList<Netlist> netlists = new ArrayList<Netlist>();
		netlists.add(cell1.getNetlist(true));
		netlists.add(cell2.getNetlist(true));
				
		return compareMany(cells, contexts, netlists, hierInfo, blackBox, 
		                   options, aborter);
	}
	private static NccResult compareMany(List<Cell> cells, List<VarContext> contexts, List<Netlist> netlists,
									     HierarchyInfo hierCompInfo,
									     boolean blackBox, 
									     NccOptions options, Aborter aborter) {
		NccEngine ncc = new NccEngine();
		return ncc.areEquivalent(cells, contexts, netlists, hierCompInfo, 
								 blackBox, options, aborter);
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
	public static NccResult compare(List<Cell> cells, List<VarContext> contexts, List<Netlist> netlists,
	                                HierarchyInfo hierCompInfo, 
	                                NccOptions options, Aborter aborter) {
		return compareMany(cells, contexts, netlists, hierCompInfo, false, 
		                   options, aborter);
	}
	/** compare two Cells */
	public static NccResult compare(Cell cell1, VarContext context1, 
	                                Cell cell2, VarContext context2, 
	                                HierarchyInfo hierInfo, 
	                                NccOptions options, Aborter aborter) {
		return compare2(cell1, context1, cell2, context2, hierInfo, false, 
				        options, aborter);
	}
	public static boolean buildBlackBoxes(Cell cell1, VarContext ctxt1, 
								          Cell cell2, VarContext ctxt2,
								          HierarchyInfo hierInfo, 
								          NccOptions options, Aborter aborter) {
		NccResult r = compare2(cell1, ctxt1, cell2, ctxt2, hierInfo, true, 
				               options, aborter);
		return r.exportMatch();
	}
}
