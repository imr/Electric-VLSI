/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NccBottomUp.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.ncc;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.ncc.basic.CellContext;
import com.sun.electric.tool.ncc.basic.CompareList;
import com.sun.electric.tool.ncc.basic.CompareLists;
import com.sun.electric.tool.ncc.basic.NccCellAnnotations;
import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.processing.HierarchyInfo;
import com.sun.electric.tool.ncc.result.NccResult;
import com.sun.electric.tool.ncc.result.NccResults;


/** Run NCC hierarchically. By default, treat every Cell with both a layout and
 * a schematic view as a hierarchical entity. 
 * More specifically, when we are asked to compare a schematic and a layout,
 * we scan both hierarchies to identify cell groups that have one or more cells
 * in the schematic hierarchy AND one or more cells in the schematic hierarchy.
 */
public class NccBottomUp {
	
	private void prln(String s) {System.out.println(s);}

	/** Prefer a schematic reference cell because mismatch diagnostics
	 * will be easier for the user to understand. */
	private CellContext selectAndRemoveReferenceCellContext(List<CellContext> cellCtxts) {
		for (Iterator<CellContext> it=cellCtxts.iterator(); it.hasNext();) {
			CellContext cc = it.next();
			if (cc.cell.isSchematic()) {
				it.remove();
				return cc;
			}
		}
		Iterator<CellContext> it=cellCtxts.iterator(); 
		CellContext refCell = it.next();
		it.remove();
		return refCell;
	}

	private boolean hasBlackBoxAnnotation(CompareList compareList) {
		for (Iterator<CellContext> it=compareList.iterator(); it.hasNext();) {
			Cell c = it.next().cell;
			NccCellAnnotations ann = NccCellAnnotations.getAnnotations(c);
			if (ann==null) continue;
			String reason = ann.getBlackBoxReason(); 
			if (reason!=null) {
				prln("Black box: "+NccUtils.fullName(c)+ " because "+reason);
				return true;
			} 			
		}
		return false;
	}
	private NccResult compareAndPrintStatus(Cell cell1, VarContext ctxt1, 
			                                Cell cell2, VarContext ctxt2, 
                                            HierarchyInfo hierInfo,
                                            NccOptions options,
											Aborter aborter) {
        prln("Comparing: "+NccUtils.fullName(cell1)+
             " with: "+NccUtils.fullName(cell2));
        System.out.flush();
        Date before = new Date();
        NccResult result = NccEngine.compare(cell1, ctxt1, cell2, ctxt2,  
	                                         hierInfo, options, aborter);
        //NccUtils.hang("Just dumped Ncc Globals");
//        if (options.checkNetEquivalenceMap) 
//            result.getEquivalence().regressionTest(cell1, cell2);

        Date after = new Date();

        if (!aborter.userWantsToAbort()) {
        	String timeStr = NccUtils.hourMinSec(before, after);
        	prln(result.summary(options.checkSizes)+" in "+timeStr+".");
            System.out.flush();
        }

        return result;
    }
	/** @return true if error building black box */
	private boolean compareCellsInCompareList(NccResults results,
			                                  CompareList compareList, 
			                                  HierarchyInfo hierInfo,
			                                  boolean blackBoxAnn,
			                                  PassedNcc passed,
			                                  NccOptions options,
			                                  Aborter aborter) {
		// build our own list because we need to modify it
		List<CellContext> cellCntxts = new ArrayList<CellContext>();
		// build Set of Cells because we need to exclude them from subcircuit 
		// detection
		Set<Cell> compareListCells = new HashSet<Cell>();
		for (CellContext cc : compareList) { 
		    cellCntxts.add(cc);
		    compareListCells.add(cc.cell);
		}

		Cell cell = cellCntxts.iterator().next().cell;
		String grpNm = cell.getLibrary().getName()+":"+cell.getName();

		hierInfo.beginNextCompareList(grpNm);

		// notSubcircuit means check it but don't use it as a subcircuit
		if (hasNotSubcircuitAnnotation(cellCntxts))  
			hierInfo.purgeCurrentCompareList();

		CellContext refCC = selectAndRemoveReferenceCellContext(cellCntxts);
		for (CellContext thisCC : cellCntxts) {
			if (blackBoxAnn || 
			    (options.skipPassed && passed.getPassed(refCC.cell, thisCC.cell))) {
				if (hierInfo==null) continue;
				NccResult r = NccUtils.buildBlackBoxes(refCC, thisCC, hierInfo, 
						                               options, aborter);
				results.add(r);
				return !r.match();
			} else {
				hierInfo.restrictSubcktDetection(refCC, thisCC, compareListCells);

				// release storage from previous Cell pair comparisons
				if (options.operation==NccOptions.FLAT_EACH_CELL) 
					results.abandonPriorResults();
				
				NccResult r = compareAndPrintStatus(refCC.cell, refCC.context,
						                            thisCC.cell, thisCC.context, 
													hierInfo, options, aborter); 
				results.add(r);
				if (r.match())  passed.setPassed(refCC.cell, thisCC.cell);
				
				// Halt after first mismatch if that's what user wants
				if (!r.match() && options.haltAfterFirstMismatch) break;
				
				if (aborter.userWantsToAbort()) break;
			}
		}
		if (!blackBoxAnn && options.operation!=NccOptions.HIER_EACH_CELL) 
			hierInfo.purgeCurrentCompareList();
		return false;
	}
	
	private boolean hasNotSubcircuitAnnotation(List<CellContext> cellContextsInGroup) {
		for (CellContext cc : cellContextsInGroup) {
			Cell c = cc.cell;
			NccCellAnnotations anns = NccCellAnnotations.getAnnotations(c);
			if (anns==null) continue;
			String notSubcktReason = anns.getNotSubcircuitReason();
			if (notSubcktReason!=null) {
				System.out.println("For this hierarchical NCC I'm not treating "+
				                   NccUtils.fullName(c)+
				                   " as a subcircuit because "+notSubcktReason);
				return true;
			}
		}
		return false;
	}
	
	private NccResults processCompareLists(List<CompareList> compareLists,
			                               PassedNcc passed,
	                                       NccOptions options, 
										   Aborter aborter) {
		NccResults results = new NccResults();
		HierarchyInfo hierInfo = new HierarchyInfo();
		for (Iterator<CompareList> it=compareLists.iterator(); it.hasNext();) {
			CompareList compareList = it.next();

			boolean blackBoxAnn = hasBlackBoxAnnotation(compareList);

			// FLAT_TOP_CELL means do all black boxes and root cell
			if (options.operation==NccOptions.FLAT_TOP_CELL && !blackBoxAnn &&
			    it.hasNext()) continue;

			// release storage from previous Cell pair comparisons
			if (options.operation==NccOptions.FLAT_EACH_CELL) 
				results.abandonPriorResults();
			
			boolean blackBoxErr;
			if (!compareList.isSafeToCheckSizes() &&
			    options.operation!=NccOptions.FLAT_TOP_CELL &&
			    !blackBoxAnn) {
				// This cell isn't safe to compare with size 
				// checking because it is parameterized and it is instantiated 
				// more than once. Just compare without size checking but purge
				// any record of the fact that unsized comparison took place.
				// This guarantees that when NCC checks this cell's parent,
				// this cell will get flattened and therefore size checked.
				// Subtle: we purge this cell even if we aren't size checking
				// because want NCC to report the same errors with or without
				// size checking.
				NccOptions tmpOptions = new NccOptions(options);
				tmpOptions.checkSizes = false;

				blackBoxErr = 
					compareCellsInCompareList(results, compareList, hierInfo, 
						                      blackBoxAnn, passed, tmpOptions, 
						                      aborter); 

				hierInfo.purgeCurrentCompareList();
			} else {
				blackBoxErr = 
					compareCellsInCompareList(results, compareList, hierInfo, 
						                      blackBoxAnn, passed, options, 
						                      aborter); 
			}
			
			if (blackBoxErr) {
				prln(
					"Halting multiple cell NCC because of failure to build " +
					"a black box"
				);
				return results;
			}

			if (aborter.userWantsToAbort()) {
				return results;
			} else if ((!results.exportMatch() || ! results.topologyMatch()) 
					   && options.haltAfterFirstMismatch) {
				// Don't stop for size mismatches
				prln("Halting NCC after finding first mismatch");
				return results;
			}
		}
		return results;
	}

	private NccResults compareCells(CellContext cc1, CellContext cc2, 
								    PassedNcc passed, NccOptions options, 
								    Aborter aborter) {
		List<CompareList> compareLists = CompareLists.getCompareLists(cc1, cc2);
		return processCompareLists(compareLists, passed, options, aborter);
	}

	// --------------------------- public methods -----------------------------
	/** Compare the two designs: cc1 and cc2. Typically, cc1 is the schematic
	 * and cc2 is the layout. Note that NCC needs the VarContext 
	 * in order to evaluate variables such as transistor widths.
	 * @param cc1 the root Cell and VarContext of the first design.
	 * @param cc2 the root Cell and VarContext of the second design.
	 * @param passed an object that remembers which Cell pairs passed NCC the 
	 * last time the designer ran NCC.
	 * @param options all of the NCC options specified by the designer
	 * @param aborter an object that NCC queries to tell whether the designer
	 * wants to abort the NCC mid run. 
	 * @return an object that describes all the mismatches that NCC found
	 */
	public static NccResults compare(CellContext cc1, CellContext cc2, 
									 PassedNcc passed, NccOptions options,
									 Aborter aborter) {
		NccBottomUp bo = new NccBottomUp();
		return bo.compareCells(cc1, cc2, passed, options, aborter);
	}
}
