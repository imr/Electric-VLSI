/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NccHierarchical.java
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
package com.sun.electric.tool.ncc;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Date;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.basic.NccCellAnnotations;
import com.sun.electric.tool.ncc.processing.HierarchyInfo;
import com.sun.electric.tool.generator.layout.LayoutLib;

/** Get all Cells in the hierarchy */
class CellUsage extends HierarchyEnumerator.Visitor {
	private Set cellsInUse = new HashSet();
	private List cellsInRevTopoOrder = new ArrayList();
	private Map groupToAdditions = new HashMap();
	private void processCellGroupAdditions(Cell cell) {
		NccCellAnnotations ann = NccCellAnnotations.getAnnotations(cell);
		if (ann==null) return;
		Cell.CellGroup group = ann.getGroupToJoin();
		if (group==null) return;
		Set additions = (Set) groupToAdditions.get(group);
		if (additions==null) {
			additions = new HashSet();
			groupToAdditions.put(group, additions);					
		}
		additions.add(cell);
	}
	public boolean enterCell(HierarchyEnumerator.CellInfo info) {
		Cell cell = info.getCell();
		if (cellsInUse.contains(cell))  return false;
		cellsInUse.add(cell);
		processCellGroupAdditions(cell);
		return true;
	}
	public void exitCell(HierarchyEnumerator.CellInfo info) {
		cellsInRevTopoOrder.add(info.getCell());
	}
	public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info) {
		return true;
	}
	
	// ------------------ Here's the real interface ------------------------
	public boolean cellIsUsed(Cell cell) {return cellsInUse.contains(cell);}
	public Iterator cellsInReverseTopologicalOrder() {
		return cellsInRevTopoOrder.iterator();
	}
	/** @return a Set of Cells to add to group */
	public Set getGroupAdditions(Cell.CellGroup group) {
		Set additions = (Set) groupToAdditions.get(group);
		return additions!=null ? additions : new HashSet();
	}
} 

/** Run NCC hierarchically. By default, treat every Cell with both a layout and
 * a schematic view as a hierarchical entity. */
public class NccBottomUp {
	private CellUsage getCellUsage(Cell root) {
		CellUsage visitor = new CellUsage();
		HierarchyEnumerator.enumerateCell(root, null, null, visitor);
		return visitor;
	}

	private NccResult compareTwo(Cell schematic, Cell layout, 
	                             HierarchyInfo hierInfo,
	                             NccOptions options) {
	    if (NccUtils.hasSkipAnnotation(schematic) ||
		    NccUtils.hasSkipAnnotation(layout)) 
		    return new NccResult(true, true, true);
		return NccUtils.compareAndPrintStatus(schematic, null, layout, null, 
		                                      hierInfo, options);
	}

	/** Prefer a schematic reference cell because mismatch diagnostics
	 * will be easier for the user to understand. */
	private Cell selectAndRemoveReferenceCell(List cells) {
		for (Iterator it=cells.iterator(); it.hasNext();) {
			Cell cell = (Cell) it.next();
			if (cell.getView()==View.SCHEMATIC) {
				it.remove();
				return cell;
			}
		}
		Iterator it=cells.iterator(); 
		Cell refCell = (Cell) it.next();
		it.remove();
		return refCell;
	}

	private NccResult compareCellsInGroup(List cellsInGroup, String groupName,
							   			  HierarchyInfo hierInfo, 
							   			  NccOptions options) {
		NccResult result = new NccResult(true, true, true);
		// make sure there's work to do
		// Subtle: empty Cell list may occur when a Cell joins a different 
		// group. In that case avoid blowing up.
		if (cellsInGroup.size()<2) return result;
		
		// notSubcircuit means check it but don't use it as a subcircuit
		if (hierInfo!=null && dontTreatAsSubcircuit(cellsInGroup))  
			hierInfo.purgeCurrentCellGroup();

		Cell refCell = selectAndRemoveReferenceCell(cellsInGroup);

		if (hierInfo!=null)  hierInfo.beginNextCellGroup(groupName);
		for (Iterator it=cellsInGroup.iterator(); it.hasNext();) {
			Cell cell = (Cell) it.next();
			result.and(compareTwo(refCell, cell, hierInfo, options));
		}
		return result;
	}
	
	/** For each cell in use1 collect all the Cells in its CellGroup. 
	 * Omit cells unused by our designs.  
	 * <p>Since Java-Electric's CellGroups aren't completely functional yet
	 * simulate the addition of Cells (e.g. from other libraries) to a 
	 * CellGroup using the joinGroup annotation. */
	private List getUsedCellsInGroup(Cell cell, CellUsage use1, 
	                                 CellUsage use2) {
		NccCellAnnotations ann = NccCellAnnotations.getAnnotations(cell);
		Cell.CellGroup group = cell.getCellGroup();

		// if a Cell is moving to another group then don't explore
		// its current group
		if (ann!=null && 
		    ann.getGroupToJoin()!=null &&
		    ann.getGroupToJoin()!=group) return new ArrayList();

		// add Cells with "joinGroup" annotations
		Set cellsInGroup = new HashSet();
		cellsInGroup.addAll(use1.getGroupAdditions(group));
		cellsInGroup.addAll(use2.getGroupAdditions(group));
		
		// add all Cells that are actually used
		for (Iterator gi=group.getCells(); gi.hasNext();) {
			Cell c = (Cell) gi.next();
			if (use1.cellIsUsed(c) || use2.cellIsUsed(c)) cellsInGroup.add(c);		
		}
		LayoutLib.error(cellsInGroup.size()==0, "Cell not in its own group?");
		
		List cellsInGroupList = new ArrayList();
		cellsInGroupList.addAll(cellsInGroup);
		
		/* Tricky: If the user compares two layouts or two schematics, we'd
		 * like to perform the comparison hierarchically when the designs share
		 * the same Cell. If the CellGroup size is >2 then this will 
		 * automatically happen. However, if the CellGroup size is 1 then we 
		 * need to force an artificial comparison of that Cell with itself in 
		 * order to add the required information to HierarchyInfo. */
		if (cellsInGroup.size()==1) {
			Cell onlyCell = (Cell) cellsInGroup.iterator().next();
			if (use1.cellIsUsed(onlyCell) && use2.cellIsUsed(onlyCell)) 
				cellsInGroupList.add(onlyCell);
		} 
		
		return cellsInGroupList;
	}
	
	private boolean dontTreatAsSubcircuit(List cellsInGroup) {
		for (Iterator it=cellsInGroup.iterator(); it.hasNext();) {
			Cell c = (Cell) it.next();
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
	
	private NccResult compareCellGroups(CellUsage use1, CellUsage use2,
	                                    boolean hierarchical, 
	                                    NccOptions options) {
		NccResult result = new NccResult(true, true, true);
		HierarchyInfo hierInfo = hierarchical ? new HierarchyInfo() : null;
		for (Iterator it=use1.cellsInReverseTopologicalOrder(); it.hasNext();) {
			Cell cell = (Cell) it.next();
			List cellsInGroup = getUsedCellsInGroup(cell, use1, use2);
			String grpNm = cell.getLibrary().getName()+":"+cell.getName();

//			if (hierarchical && dontTreatAsSubcircuit(cellsInGroup))  continue;

			result.and(compareCellsInGroup(cellsInGroup, grpNm, hierInfo,options));

			if (!result.match() && options.haltAfterFirstMismatch) {
				System.out.println( 
					"Halting multiple cell NCC after finding first mismatch"
				);
				return result;
			}
		}
		return result;
	}

	private NccResult compareCells(Cell c1, Cell c2, boolean hierarchical, 
	                               NccOptions options) {
		// find all Cells in both hierarchies
		CellUsage use1 = getCellUsage(c1);
		CellUsage use2 = getCellUsage(c2);
		return compareCellGroups(use1, use2, hierarchical, options); 
	}
	
	public static NccResult compare(Cell c1, Cell c2, boolean hierarchical, 
	                                NccOptions options) {
		NccBottomUp ncch = new NccBottomUp();
		return ncch.compareCells(c1, c2, hierarchical, options);
	}
}
