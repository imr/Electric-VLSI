package com.sun.electric.tool.ncc.basic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.generator.layout.LayoutLib;

public class CompareLists {
	private CellUsage getCellUsage(Cell root) {
		CellUsage visitor = new CellUsage();
		HierarchyEnumerator.enumerateCell(root, null, null, visitor);
		return visitor;
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
	private void purgeUnnecessaryDuplicateCells(List compareList) {
		Set cells = new HashSet();
		for (Iterator it=compareList.iterator(); 
		     it.hasNext() && compareList.size()>2;) {
			Cell c = ((CellContext)it.next()).cell;
			if (cells.contains(c))  it.remove();
			else  cells.add(c);
		}
	}

	private void printCells(List cellContextsInGroup) {
		System.out.print("Cells in group: ");
		for (Iterator it=cellContextsInGroup.iterator(); it.hasNext();) {
			CellContext cc = (CellContext) it.next();
			Cell c = cc.cell;
			System.out.print(c.getName()+" ");
		}
		System.out.println();
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
	 * @return a list of CellContexts to compare */
	private List getCompareList(Cell cell, CellUsage use1, CellUsage use2) {
		NccCellAnnotations ann = NccCellAnnotations.getAnnotations(cell);
		Cell.CellGroup group = cell.getCellGroup();

		// if a Cell is moving to another group then don't explore
		// its current group
		if (ann!=null && 
		    ann.getGroupToJoin()!=null &&
		    ann.getGroupToJoin()!=group) return new ArrayList();

		// add Cells with "joinGroup" annotations
		Set compareSet = new HashSet();
		compareSet.addAll(use1.getGroupAdditions(group));
		compareSet.addAll(use2.getGroupAdditions(group));
		
		// add all Cells that are actually used
		for (Iterator gi=group.getCells(); gi.hasNext();) {
			Cell c = (Cell) gi.next();
			if (use1.cellIsUsed(c)) compareSet.add(use1.getCellContext(c));
			if (use2.cellIsUsed(c)) compareSet.add(use2.getCellContext(c));
		}
		
		// finally the two root Cells should always be compared
		if (cell==use1.getRoot()) 
			compareSet.add(use2.getCellContext(use2.getRoot()));
		if (cell==use2.getRoot()) 
			compareSet.add(use1.getCellContext(use1.getRoot()));

		LayoutLib.error(compareSet.size()==0, "Cell not in its own group?");
		
		List compareList = new ArrayList();
		compareList.addAll(compareSet);
		
		purgeUnnecessaryDuplicateCells(compareList);

		if (hasSkipAnnotation(compareList)) compareList.clear();

		//		System.out.println("Seed cell: "+cell.getName());
//		printCells(cellsInGroupList);

		return compareList;
	}
	
	private boolean alreadyCompared(Set compared, List compareList) {
		int num=0, numComp=0;
		for (Iterator it=compareList.iterator(); it.hasNext();) {
			CellContext cc = (CellContext) it.next();
			if (compared.contains(cc)) numComp++;
			num++;
		}
		LayoutLib.error(numComp!=0 && numComp!=num, 
						"cell group partially processed");
		return numComp>0;
	}

	private List getCompareLists(CellUsage use1, CellUsage use2) {
		Set compared = new HashSet();

		List compareLists = new ArrayList();
		for (Iterator it=use1.cellsInReverseTopologicalOrder(); it.hasNext();) {
			Cell cell = (Cell) it.next();
			List compareList = getCompareList(cell, use1, use2);

			// Really subtle! If A instantiates B and B instantiates C and if A, B, 
			// and C are all in the same Cell group then this loop will encounter
			// the Cell group {A, B, C} three times!  Only perform the comparison
			// once.
			if (alreadyCompared(compared, compareList)) continue;

			// if only 1 CellContext then there's nothing to do
			if (compareList.size()<2) continue;
			
			compared.addAll(compareList);
			compareLists.add(compareList);
		}
		return compareLists;
	}
	private List getCompareLists1(Cell c1, Cell c2) {
		CellUsage use1 = getCellUsage(c1);
		CellUsage use2 = getCellUsage(c2);
		return getCompareLists(use1, use2);
	}
	
	public static List getCompareLists(Cell c1, Cell c2) {
		return (new CompareLists()).getCompareLists1(c1, c2);
	}
}

/** Get all Cells in the hierarchy */
class CellUsage extends HierarchyEnumerator.Visitor {
	// map from Cell to CellContext
	private Map cellsInUse = new HashMap();
	private List cellsInRevTopoOrder = new ArrayList();
	private Map groupToAdditions = new HashMap();
	private Cell root;
	
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
		if (root==null) root = cell;
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
	public Cell getRoot() {return root;}
} 
