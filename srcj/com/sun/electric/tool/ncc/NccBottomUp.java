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
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.basic.NccCellAnnotations;
import com.sun.electric.tool.ncc.basic.CellContext;
import com.sun.electric.tool.ncc.processing.HierarchyInfo;
import com.sun.electric.tool.generator.layout.LayoutLib;

/** Get all Cells in the hierarchy */
class CellUsage extends HierarchyEnumerator.Visitor {
	// map from Cell to CellContext
	private Map cellsInUse = new HashMap();
	private List cellsInRevTopoOrder = new ArrayList();
	private Map groupToAdditions = new HashMap();
	private void processCellGroupAdditions(CellContext cellCtxt) {
		NccCellAnnotations ann = NccCellAnnotations.getAnnotations(cellCtxt.cell);
		if (ann==null) return;
		Cell.CellGroup group = ann.getGroupToJoin();
		if (group==null) return;
		Set additions = (Set) groupToAdditions.get(group);
		if (additions==null) {
			additions = new HashSet();
			groupToAdditions.put(group, additions);					
		}
		additions.add(cellCtxt);
	}
	public boolean enterCell(HierarchyEnumerator.CellInfo info) {
		Cell cell = info.getCell();
		VarContext context = info.getContext();
		if (cellsInUse.containsKey(cell))  return false;
		CellContext cellCtxt = new CellContext(cell, context); 
		cellsInUse.put(cell, cellCtxt);
		processCellGroupAdditions(cellCtxt);
		return true;
	}
	public void exitCell(HierarchyEnumerator.CellInfo info) {
		cellsInRevTopoOrder.add(info.getCell());
	}
	public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info) {
		return true;
	}
	
	// ------------------ Here's the real interface ------------------------
	public boolean cellIsUsed(Cell cell) {return cellsInUse.containsKey(cell);}
	public Iterator cellsInReverseTopologicalOrder() {
		return cellsInRevTopoOrder.iterator();
	}
	public CellContext getCellContext(Cell cell) {
		LayoutLib.error(!cellsInUse.containsKey(cell), "cell not found");
		return (CellContext) cellsInUse.get(cell);
	}
	/** @return a Set of CellContexts to add to group */
	public Set getGroupAdditions(Cell.CellGroup group) {
		Set additions = (Set) groupToAdditions.get(group);
		return additions!=null ? additions : new HashSet();
	}
} 
class Passed {
	private static class Pair {
		private Cell c1, c2;
		public Pair(Cell c1, Cell c2) {this.c1=c1; this.c2=c2;}
		public boolean equals(Object o) {
			if (!(o instanceof Pair)) return false;
			Pair p = (Pair) o;
			return (c1==p.c1 && c2==p.c2) || (c1==p.c2 && c2==p.c1);
		}
		public int hashCode() {return c1.hashCode()*c2.hashCode();}
	}
	private Set passed = new HashSet();
	public synchronized void setPassed(Cell c1, Cell c2) {passed.add(new Pair(c1, c2));}
	public synchronized boolean getPassed(Cell c1, Cell c2) {
		return passed.contains(new Pair(c1, c2));
	}
	public synchronized void clear() {passed = new HashSet();}
}

/** Run NCC hierarchically. By default, treat every Cell with both a layout and
 * a schematic view as a hierarchical entity. */
public class NccBottomUp {
	/** remember which Cells have already passed NCC */
	private static final Passed passed = new Passed(); 

	private CellUsage getCellUsage(Cell root) {
		CellUsage visitor = new CellUsage();
		HierarchyEnumerator.enumerateCell(root, null, null, visitor);
		return visitor;
	}

	/** Prefer a schematic reference cell because mismatch diagnostics
	 * will be easier for the user to understand. */
	private CellContext selectAndRemoveReferenceCellContext(List cellCtxts) {
		for (Iterator it=cellCtxts.iterator(); it.hasNext();) {
			CellContext cc = (CellContext) it.next();
			if (cc.cell.getView()==View.SCHEMATIC) {
				it.remove();
				return cc;
			}
		}
		Iterator it=cellCtxts.iterator(); 
		CellContext refCell = (CellContext) it.next();
		it.remove();
		return refCell;
	}
	private boolean hasSkipAnnotation(List cellCtxts) {
		for (Iterator it=cellCtxts.iterator(); it.hasNext();) {
			Cell c = ((CellContext) it.next()).cell;
			NccCellAnnotations ann = NccCellAnnotations.getAnnotations(c);
			if (ann==null) continue;
			String reason = ann.getSkipReason();
			if (reason!=null) {
				System.out.println("Skip NCC of "+NccUtils.fullName(c)+
								   " because "+reason);
				return true;							   
			}
		}
		return false;
	}
	
	private boolean hasBlackBoxAnnotation(List cellCtxts) {
		for (Iterator it=cellCtxts.iterator(); it.hasNext();) {
			Cell c = ((CellContext) it.next()).cell;
			NccCellAnnotations ann = NccCellAnnotations.getAnnotations(c);
			if (ann==null) continue;
			String reason = ann.getBlackBoxReason(); 
			if (reason!=null) {
				System.out.println("Black box: "+NccUtils.fullName(c)+
				                   " because "+reason);
				return true;
			} 			
		}
		return false;
	}
	/** @return the result of comparing all Cells in CellGroup. If we are
	 * supposed to build just a black box but we couldn't then return null */
	private NccResult compareCellsInGroup(List cellContextsInGroup, 
	                                      String groupName,
							   			  HierarchyInfo hierInfo,
							   			  boolean hierarchical,
							   			  boolean skipPassed, 
							   			  NccOptions options) {
		NccResult result = new NccResult(true, true, true);
		// make sure there's work to do
		// Subtle: empty Cell list may occur when a Cell joins a different 
		// group. In that case avoid blowing up.
		if (cellContextsInGroup.size()<2) return result;
		if (hasSkipAnnotation(cellContextsInGroup)) return result;
		
		hierInfo.beginNextCellGroup(groupName);

		// notSubcircuit means check it but don't use it as a subcircuit
		if (hasNotSubcircuitAnnotation(cellContextsInGroup))  
			hierInfo.purgeCurrentCellGroup();

		boolean blackBoxAnn = hasBlackBoxAnnotation(cellContextsInGroup);

		CellContext refCC = 
			selectAndRemoveReferenceCellContext(cellContextsInGroup);
		for (Iterator it=cellContextsInGroup.iterator(); it.hasNext();) {
			CellContext thisCC = (CellContext) it.next();
			if (blackBoxAnn || 
			    (skipPassed && passed.getPassed(refCC.cell, thisCC.cell))) {
				if (hierInfo==null) continue;
				boolean ok = 
				  NccUtils.buildBlackBoxes(refCC, thisCC, hierInfo, options);
				if (!ok) return null;
			} else {
				NccResult r = NccUtils.compareAndPrintStatus(refCC, thisCC, 
															 hierInfo, options); 
				result.and(r);
				if (r.match())  passed.setPassed(refCC.cell, thisCC.cell);
			}
		}
		if (!hierarchical) hierInfo.purgeCurrentCellGroup();
		return result;
	}
	
	/** For each cell in use1 collect all the Cells in its CellGroup. 
	 * Omit cells unused by our designs.  
	 * <p>Since Java-Electric's CellGroups aren't completely functional yet
	 * simulate the addition of Cells (e.g. from other libraries) to a 
	 * CellGroup using the joinGroup annotation.
	 * @return a list of CellContexts to compare */
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
			if (use1.cellIsUsed(c)) cellsInGroup.add(use1.getCellContext(c));
			if (use2.cellIsUsed(c)) cellsInGroup.add(use2.getCellContext(c));
		}
		LayoutLib.error(cellsInGroup.size()==0, "Cell not in its own group?");
		
		List cellsInGroupList = new ArrayList();
		cellsInGroupList.addAll(cellsInGroup);
		
		// No need for trickery. Cell used in two layouts or two schematics 
		// always adds two CellContexts to cellsInGroup.
//		/* Tricky: If the user compares two layouts or two schematics, we'd
//		 * like to perform the comparison hierarchically when the designs share
//		 * the same Cell. If the CellGroup size is >2 then this will 
//		 * automatically happen. However, if the CellGroup size is 1 then we 
//		 * need to force an artificial comparison of that Cell with itself in 
//		 * order to add the required information to HierarchyInfo. */
//		if (cellsInGroup.size()==1) {
//			Cell onlyCell = (Cell) cellsInGroup.iterator().next();
//			if (use1.cellIsUsed(onlyCell) && use2.cellIsUsed(onlyCell)) 
//				cellsInGroupList.add(onlyCell);
//		} 
		
		return cellsInGroupList;
	}
	
	private boolean hasNotSubcircuitAnnotation(List cellContextsInGroup) {
		for (Iterator it=cellContextsInGroup.iterator(); it.hasNext();) {
			Cell c = ((CellContext) it.next()).cell;
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
	                                    boolean skipPassed, 
	                                    NccOptions options) {
		NccResult result = new NccResult(true, true, true);
		HierarchyInfo hierInfo = new HierarchyInfo();
		for (Iterator it=use1.cellsInReverseTopologicalOrder(); it.hasNext();) {
			Cell cell = (Cell) it.next();
			List cellContextsInGroup = getUsedCellsInGroup(cell, use1, use2);
			String grpNm = cell.getLibrary().getName()+":"+cell.getName();
			NccResult r = compareCellsInGroup(cellContextsInGroup, grpNm,
											  hierInfo, hierarchical, 
											  skipPassed, options); 
			if (r==null) {
				System.out.println(
					"Halting multiple cell NCC because of failure to build " +					"a black box"
				);
				return result;
			}

			result.and(r);

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
	                               boolean skipPassed, NccOptions options) {
		// find all Cells in both hierarchies
		CellUsage use1 = getCellUsage(c1);
		CellUsage use2 = getCellUsage(c2);
		return compareCellGroups(use1, use2, hierarchical, skipPassed, options);
	}

	// --------------------------- public methods -----------------------------
	public static NccResult compare(Cell c1, Cell c2, boolean hierarchical,
	                                boolean skipPassed, NccOptions options) {
		NccBottomUp ncch = new NccBottomUp();
		return ncch.compareCells(c1, c2, hierarchical, skipPassed, options);
	}
	public static void clearPassedHistory() {passed.clear();}
}
