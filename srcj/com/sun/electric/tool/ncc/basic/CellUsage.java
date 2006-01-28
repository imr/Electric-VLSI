/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellUsage.java
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.generator.layout.LayoutLib;

/** Find all Cells used in the design. Collect information about Cell usage. */
class CellUsage extends HierarchyEnumerator.Visitor {
	// map from Cell to CellContext
	private Map<Cell,CellContext> cellsInUse = new HashMap<Cell,CellContext>();
	private List<Cell> cellsInRevTopoOrder = new ArrayList<Cell>();
	private Map<Cell.CellGroup,Set<CellContext>> groupToAdditions = new HashMap<Cell.CellGroup,Set<CellContext>>();
	private Cell root;
	private Set<Cell> singleUseCells;
	
	private void processCellGroupAdditions(CellContext cellCtxt) {
		NccCellAnnotations ann = NccCellAnnotations.getAnnotations(cellCtxt.cell);
		if (ann==null) return;
		Cell.CellGroup group = ann.getGroupToJoin();
		if (group==null) return;
		Set<CellContext> additions = groupToAdditions.get(group);
		if (additions==null) {
			additions = new HashSet<CellContext>();
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
	
	private void addUse(Set<Cell> usedOnce, Set<Cell> usedMoreThanOnce, Cell c) {
		if (usedMoreThanOnce.contains(c)) return;
		if (usedOnce.contains(c)) {
			usedOnce.remove(c);
			usedMoreThanOnce.add(c);
		} else {
			usedOnce.add(c);
		}
	}
	private void findSingleUseCells() {
		Map<Cell,Set<Cell>> cellToUsedOnce = new HashMap<Cell,Set<Cell>>();
		Set<Cell> usedMoreThanOnce = new HashSet<Cell>();
		// For each Cell in the design: c in reverse topological order
		for (Iterator<Cell> it=cellsInReverseTopologicalOrder(); it.hasNext();) {
			Cell c = it.next();
			Set<Cell> usedOnce = new HashSet<Cell>();
			// For each child Cell of c: child 
			for (Iterator<Nodable> ni=c.getNetlist(true).getNodables(); ni.hasNext();) {
				NodeProto np = ni.next().getProto();
				if (!(np instanceof Cell)) continue;
				Cell child = (Cell) np;
				addUse(usedOnce, usedMoreThanOnce, child);

				// Subtle: If child is an icon without a schematic then child 
				// won't have usedOnce set. Apparently Nodables aren't created
				// for icons with no schematic.
				if (!cellToUsedOnce.containsKey(child))  continue;

				Set<Cell> childUsedOnce = cellToUsedOnce.get(child);
				// For each descendent of child instantiated exactly once by child
				for (Cell ci : childUsedOnce) {
					addUse(usedOnce, usedMoreThanOnce, ci);
				}
			}
			cellToUsedOnce.put(c, usedOnce);
			if (!it.hasNext()) {
				// Cell c is the root.  Create set of all cells
				// in the design that are instantiated exactly once.
				singleUseCells = new HashSet<Cell>();
				singleUseCells.add(c);
				singleUseCells.addAll(usedOnce);
			}
		}
	}

	private CellUsage() {}

	// ------------------ Here's the real interface ------------------------
	// static constructor
	public static CellUsage getCellUsage(CellContext root) {
		CellUsage visitor = new CellUsage();
		HierarchyEnumerator.enumerateCell(root.cell, root.context, visitor);
//		HierarchyEnumerator.enumerateCell(root.cell, root.context, null, visitor);
		visitor.findSingleUseCells();
		return visitor;
	}

	public boolean cellIsUsed(Cell cell) {return cellsInUse.containsKey(cell);}
	/** There is only one instance of this Cell in the design */
	public boolean cellIsUsedOnce(Cell cell) {return singleUseCells.contains(cell);}
	public Iterator<Cell> cellsInReverseTopologicalOrder() {
		return cellsInRevTopoOrder.iterator();
	}
	public CellContext getCellContext(Cell cell) {
		LayoutLib.error(!cellsInUse.containsKey(cell), "cell not found");
		return cellsInUse.get(cell);
	}
	/** @return a Set of CellContexts to add to group */
	public Set<CellContext> getGroupAdditions(Cell.CellGroup group) {
		Set<CellContext> additions = groupToAdditions.get(group);
		return additions!=null ? additions : new HashSet<CellContext>();
	}
	public Cell getRoot() {return root;}
} 
