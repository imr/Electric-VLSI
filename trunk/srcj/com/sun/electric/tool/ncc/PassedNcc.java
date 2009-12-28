package com.sun.electric.tool.ncc;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.tool.ncc.basic.CellContext;

/**
 * Cell pairs that have already passed NCC. PassedNcc is used to
 * implement NCC's incremental mode: check only cells that have
 * changed since the last NCC.
 */
public class PassedNcc {
	private static class Pair {
		private Cell c1, c2;
		public Pair(Cell c1, Cell c2) {this.c1=c1; this.c2=c2;}
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Pair)) return false;
			Pair p = (Pair) o;
			return (c1==p.c1 && c2==p.c2) || (c1==p.c2 && c2==p.c1);
		}
		@Override
		public int hashCode() {return c1.hashCode()*c2.hashCode();}
		public boolean changedAfter(Map<Cell,Date> treeRevisionDates, Date date) {
			Date d1 = treeRevisionDates.get(c1);
			Date d2 = treeRevisionDates.get(c2);
			
			return d1==null || d2==null || d1.after(date) || d2.after(date);
		}
	}
	/** For the subtree rooted at each Cell, find the latest Cell revision date 
	 * in that Cell */
	private static class TreeRevisionDates extends HierarchyEnumerator.Visitor {
		/** the latest modification date of a Cell and all it's descendants */
		private Map<Cell, Date> treeRevisionDates;

		@Override
		public boolean enterCell(HierarchyEnumerator.CellInfo info) {
			Cell cell = info.getCell();
			if (treeRevisionDates.containsKey(cell)) return false;

			treeRevisionDates.put(cell, cell.getRevisionDate());
			
			return true;
		}
		@Override
		public void exitCell(HierarchyEnumerator.CellInfo info) {
			if (!info.isRootCell()) {
				Cell me = info.getCell();
				Date myTreeDate = treeRevisionDates.get(me);
				Cell parent = info.getParentInfo().getCell();
				Date parentTreeDate = treeRevisionDates.get(parent);
				if (myTreeDate.after(parentTreeDate))  treeRevisionDates.put(parent, myTreeDate);
			}
		}
		@Override
		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info) {
			return true;
		}

		private TreeRevisionDates(Map<Cell, Date> treeRevisionDates) {
			this.treeRevisionDates = treeRevisionDates;
		}
		
		// ------------------ Here's the real interface ------------------------
		public static void addTreeRevisionDates(Map<Cell, Date> treeRevisionDates,
				                                CellContext root) {
			TreeRevisionDates visitor = new TreeRevisionDates(treeRevisionDates);
			HierarchyEnumerator.enumerateCell(root.cell, root.context, visitor);
		}
	}

	private Set<Pair> passed = new HashSet<Pair>();
	private Date lastNccRunDate;
	
	/** Remember that Cells c1 and c2 have passed NCC and are therefore topologically identical */
	public synchronized void setPassed(Cell c1, Cell c2) {passed.add(new Pair(c1, c2));}
	/** Did Cells c1 and c2 match the last time NCC was run?
	 * @param c1 Cell one
	 * @param c2 Cell two
	 * @return true if Cells c1 and c2 matched the last time NCC was run. Therefore
	 * it is safe to assume that c1 and c2 are topologically identical.
	 */
	public synchronized boolean getPassed(Cell c1, Cell c2) {
		return passed.contains(new Pair(c1, c2));
	}
	/** Purge all Cells that the user changed since the last NCC run.
	 * Then record the time of this new NCC Run. */
	public synchronized void removeCellsChangedSinceLastNccRun(CellContext[] cellContexts) {
		// for each Cell, compute latest revision date of all descendants 
		Map<Cell, Date> treeRevisionDates = new HashMap<Cell, Date>();
		for (CellContext cellContext : cellContexts) {
			TreeRevisionDates.addTreeRevisionDates(treeRevisionDates, cellContext);
		}
		
		for (Iterator<Pair> pIt=passed.iterator(); pIt.hasNext();) {
			Pair p = pIt.next();
			if (p.changedAfter(treeRevisionDates, lastNccRunDate)) pIt.remove();
		}
		lastNccRunDate = new Date();
	}
}

