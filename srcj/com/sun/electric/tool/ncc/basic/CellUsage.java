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
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.logicaleffort.LENetlister;

/** Find all Cells used in the design. Collect information about Cell usage. */
class CellUsage extends HierarchyEnumerator.Visitor {
	// map from Cell to CellContext
	private Map<Cell,CellContext> cellsInUse = new HashMap<Cell,CellContext>();
	private List<Cell> cellsInRevTopoOrder = new ArrayList<Cell>();
	private Map<Cell.CellGroup,Set<CellContext>> groupToAdditions = new HashMap<Cell.CellGroup,Set<CellContext>>();
	private Cell root;
	// Cells that have exactly one instantiation
	private Set<Cell> singleUseCells;
	// Cells with at least one LEGATE descendent
	private Set<Cell> cellsWithLeGates;
	
	private void prln(String s) {System.out.println(s);}
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
	private Set<Cell> findSingleUseCells() {
		Set<Cell> singleUseCells = new HashSet<Cell>();
		Map<Cell,Set<Cell>> cellToUsedOnce = new HashMap<Cell,Set<Cell>>();
		Set<Cell> usedMoreThanOnce = new HashSet<Cell>();
		// For each Cell in the design: c in reverse topological order
		for (Iterator<Cell> it=cellsInReverseTopologicalOrder(); it.hasNext();) {
			Cell c = it.next();
			// usedOnce holds all Cells instantiated exactly once by the design hierarchy
			// rooted at c
			Set<Cell> usedOnce = new HashSet<Cell>();
			// For each child Cell of c: child 
			for (Iterator<Nodable> noIt=c.getNetlist(false).getNodables(); noIt.hasNext();) {
				NodeProto np = noIt.next().getProto();
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
				singleUseCells.add(c);
				singleUseCells.addAll(usedOnce);
			}
		}
		return singleUseCells;
	}
	// I copied these tests from Spice.java. However, they
	// don't feel right to me.
	// I think the Cell should be marked LEGATE and not the instance. RKao
	private boolean isLeGate(Nodable no) {

		// The following two lines don't work because too many NodeInsts
		// of non-LE-Gates have the LEGATE attribute. This causes NCC
		// to flatten too much when size checking is turned on. This causes
		// NCC's hierarchy heuristics to behave badly.
//        return no.getVar(LENetlister.ATTR_LEGATE)!=null ||
//               no.getVar(LENetlister.ATTR_LEKEEPER)!=null;
		boolean attrLeGate = no.getVar(LENetlister.ATTR_LEGATE)!=null ||
			                 no.getVar(LENetlister.ATTR_LEKEEPER)!=null;
		boolean hasGetDrive = false;
		for (Iterator<Variable> vIt=no.getVariables(); vIt.hasNext();) {
			Variable v = vIt.next();
			if (v.getCode()==TextDescriptor.Code.JAVA) {
				String expr = v.getObject().toString();
				// It's spelled two different ways
				if (expr.indexOf("getDrive")!=-1 || expr.indexOf("getdrive")!=-1) {
					hasGetDrive = true;
				}
			}
		}
		if (hasGetDrive && !attrLeGate) {
			prln("  Warning: instance: "+no.getName()+" in Cell: "+
				 no.getParent().describe(false)+
				 " has a variable that calls getDrive but the instance "+
				 "has no variable named LEGATE or LEKEEPER");
		}
		if (attrLeGate && !hasGetDrive) {
			prln("  Warning: instance: "+no.getName()+" in Cell: "+
				 no.getParent().describe(false)+
				 " has a variable named LEGATE or LEKEEPER but has"+
				 " no variable that calls getDrive()");
		}
		return hasGetDrive;
	}
	// find Cells that have at least one LEGATE descendent
	private Set<Cell> findCellsWithLeGate() {
		Set<Cell> cellsWithLeGate = new HashSet<Cell>();
		for (Iterator<Cell> it=cellsInReverseTopologicalOrder(); it.hasNext();) {
			Cell c = it.next();
			boolean hasLeGate = false;
			for (Iterator<Nodable> noIt=c.getNetlist(false).getNodables(); noIt.hasNext();) {
				Nodable no = noIt.next();
				if (isLeGate(no)) {hasLeGate=true; break;}
				NodeProto np = no.getProto();
				if (!(np instanceof Cell)) continue;
				Cell child = (Cell) np;
				if (cellsWithLeGate.contains(child)) {hasLeGate=true; break;}
			}
			if (hasLeGate) {
//				// Debug
				System.out.println("  Cell contains LE gate: "+c.describe(false));
				cellsWithLeGate.add(c);
			}
		}
		return cellsWithLeGate;
	}
	private boolean cellIsParameterized(Cell c) {
		Iterator<Variable> vIt = c.getParameters();
		return vIt.hasNext();
	}
	private void postProcess() {
		singleUseCells = findSingleUseCells();
		cellsWithLeGates = findCellsWithLeGate();
	}

	private CellUsage() {}

	// ------------------ Here's the real interface ------------------------
	// static constructor
	public static CellUsage getCellUsage(CellContext root) {
		CellUsage visitor = new CellUsage();
		HierarchyEnumerator.enumerateCell(root.cell, root.context, visitor);
		visitor.postProcess();
		return visitor;
	}

	public boolean cellIsUsed(Cell cell) {return cellsInUse.containsKey(cell);}
	/** Cell has only one size in the design */
	public boolean cellHasOnlyOneSize(Cell cell) {
		final boolean NEW_ALGORITHM = true;
		
//		//debug
//		String cellNm = cell.getName();
//		if (cellNm.indexOf("rxSlice")!=-1) {
//			prln(" Testing rxSlice");
//			prln("    Single use: "+singleUseCells.contains(cell));
//			prln("    LE Gates: "+cellsWithLeGates.contains(cell));
//			prln("    Parameterized: "+cellIsParameterized(cell));
//		}
		
		if (NEW_ALGORITHM) {
			return singleUseCells.contains(cell) || 
		       (!cellsWithLeGates.contains(cell)) && !cellIsParameterized(cell);
		} else {
			return singleUseCells.contains(cell);
		}
	}
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
