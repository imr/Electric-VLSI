/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CompareList.java
 *
 * Copyright (c) 2003 Sun Microsystems and Free Software
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
package com.sun.electric.tool.ncc.basic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.generator.layout.LayoutLib;

/** A list of CellContexts to compare. Also, each CompareList has a 
 * boolean that says whether it's safe to compare sizes in addition to 
 * topologies. */
public class CompareList implements Iterable<CellContext> {
	private final List<CellContext> cellContexts = new ArrayList<CellContext>();
	private boolean safeToCheckSizes;

	private boolean hasSkipAnnotation(List<CellContext> cellCtxts) {
		for (CellContext cc : cellCtxts) {
			Cell c = cc.cell;
			NccCellAnnotations ann = NccCellAnnotations.getAnnotations(c);
			if (ann==null) continue;
			String reason = ann.getSkipReason();
			if (reason!=null) {
				System.out.println("Not checking: "+NccUtils.fullName(c)+
								   " because it has a \"skipNCC\" annotation "+
								   "with the comment: "+reason);
				return true;							   
			}
		}
		return false;
	}

	/** Very Subtle: If the exact same Cell is used by both designs being 
	 * compared (for example if you're comparing 2 schematics) then
	 * cellContextsInGroup will contain two CellContexts for that Cell, each 
	 * with a different context. If there are the only 2 objects in 
	 * cellContextsInGroup (ie these 2) then this is desireable because NCC
	 * will compare the two CellContexts thereby creating the SubcircuitInfo
	 * necessary to treat the Cell hierarchically for higher level comparisons.
	 *  
	 * <p>However, if cellContextsInGroup is larger than two then the two
	 * CellContexts that refer to the same Cell are unnecessary and can, in
	 * fact, be harmful.  For example, if cellContextsInGroup contained 
	 * {A, B, B} then NCC might compare {A, B} and {A, B}. The second 
	 * comparison is redundant. Worse, it confuses a lower layer of software 
	 * which panics when it tries to create a second Subcircuit model for B. */ 
	private void purgeUnnecessaryDuplicateCells(List<CellContext> compareList) {
		Set<Cell> cells = new HashSet<Cell>();
		for (Iterator<CellContext> it=compareList.iterator(); 
		     it.hasNext() && compareList.size()>2;) {
			Cell c = (it.next()).cell;
			if (cells.contains(c))  it.remove();
			else  cells.add(c);
		}
	}

	private boolean safeToCompareSizes(List<CellContext> cellContexts, CellUsage use1, CellUsage use2) {
		for (CellContext cc : cellContexts) {
			Cell c = cc.cell;
			if (c.isSchematic()) {
				if (use1.cellIsUsed(c) && !use1.cellHasOnlyOneSize(c)) return false;
				if (use2.cellIsUsed(c) && !use2.cellHasOnlyOneSize(c)) return false;
			}
		}
		return true;
	}

	/** Collect all Cells in cell's CellGroup that are used by our designs.
	 * These must be compared. 
	 * <p>Since Java-Electric's CellGroups can't span libraries, 
	 * simulate the addition of Cells (e.g. from other libraries) to a 
	 * CellGroup using the joinGroup annotation.
	 * 
	 * <p>Tricky: If a Cell is used in two layouts or two schematics then 
	 * that Cell will occur twice in returned List, each with a 
	 * different VarContext. This has advantages and pitfalls.
	 */
	public CompareList(Cell cell, CellUsage use1, CellUsage use2) {
		NccCellAnnotations ann = NccCellAnnotations.getAnnotations(cell);
		Cell.CellGroup group = cell.getCellGroup();

		// if a Cell is moving to another group then don't explore
		// its current group
		if (ann!=null && ann.getGroupToJoin()!=null &&
		    ann.getGroupToJoin()!=group) return;

		// make sure we have at least one cell from use1 and one from use2
		boolean used1=false, used2=false;
		
		// add Cells with "joinGroup" annotations
		Set<CellContext> compareSet = new HashSet<CellContext>();
		Set<CellContext> additions = use1.getGroupAdditions(group);
		if (!additions.isEmpty()) {used1=true; compareSet.addAll(additions);}
		additions = use2.getGroupAdditions(group);
		if (!additions.isEmpty()) {used2=true; compareSet.addAll(additions);}
		
		// add all Cells that are actually used
		for (Iterator<Cell> gi=group.getCells(); gi.hasNext();) {
			Cell c = gi.next();
			if (use1.cellIsUsed(c)) {used1=true; compareSet.add(use1.getCellContext(c));}
			if (use2.cellIsUsed(c)) {used2=true; compareSet.add(use2.getCellContext(c));}

			// if c is root of one design, add root of other design
			if (c==use1.getRoot()) {
				compareSet.add(use2.getCellContext(use2.getRoot()));
				used1 = used2 = true;
			}
			if (c==use2.getRoot()) {
				compareSet.add(use1.getCellContext(use1.getRoot()));
				used1 = used2 = true;
			}
		}

		LayoutLib.error(compareSet.size()==0, "Cell not in its own group?");

		// make sure we have at least one cell from use1 and one from use2
		if (!(used1 && used2)) return;

		cellContexts.addAll(compareSet);
		
		purgeUnnecessaryDuplicateCells(cellContexts);

		if (hasSkipAnnotation(cellContexts)) cellContexts.clear();
		
		safeToCheckSizes = safeToCompareSizes(cellContexts, use1, use2);
		
		LayoutLib.error(compareSet.size()<2, "nothing to compare?");
	}
	// useful for debugging
	public void printCells() {
		System.out.print("Compare List contains:=");
		for (Iterator<CellContext> it=iterator(); it.hasNext();) {
			CellContext cc = it.next();
			System.out.print(" "+cc.cell.getName());
		}
		System.out.println();
	}
	public Iterator<CellContext> iterator() {return cellContexts.iterator();}
	public boolean empty() {return cellContexts.size()==0;}
	public boolean isSafeToCheckSizes() {return safeToCheckSizes;}
}
