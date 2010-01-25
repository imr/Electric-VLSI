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
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.logicaleffort.LENetlister;
import com.sun.electric.tool.ncc.netlist.NccNetlist;
import com.sun.electric.tool.Job;

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
	@Override
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
	@Override
	public void exitCell(HierarchyEnumerator.CellInfo info) {
		cellsInRevTopoOrder.add(info.getCell());
	}
	@Override
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
			for (Iterator<Nodable> noIt=c.getNetlist(NccNetlist.SHORT_RESISTORS).getNodables(); noIt.hasNext();) {
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
		boolean attrLeGate = no.getParameterOrVariable(LENetlister.ATTR_LEGATE)!=null ||
			                 no.getParameterOrVariable(LENetlister.ATTR_LEKEEPER)!=null;
		boolean hasGetDrive = false;
		for (Iterator<Variable> vIt=no.getDefinedParameters(); vIt.hasNext();) {
//		for (Iterator<Variable> vIt=no.getVariables(); vIt.hasNext();) {
			Variable v = vIt.next();
			if (v.isJava()) {
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
			// This is no longer illegal. In the new standard cell
			// logical effort methodology
			// an LEGATE indicates that the LE tool should substitute a
			// standard cell of the appropriate size.
//			prln("  Warning: instance: "+no.getName()+" in Cell: "+
//				 no.getParent().describe(false)+
//				 " has a variable named LEGATE or LEKEEPER but has"+
//				 " no variable that calls getDrive()");
		}
		return hasGetDrive;
	}
	// find Cells that have at least one LEGATE descendent
	private Set<Cell> findCellsWithLeGate() {
		Set<Cell> cellsWithLeGate = new HashSet<Cell>();
		for (Iterator<Cell> it=cellsInReverseTopologicalOrder(); it.hasNext();) {
			Cell c = it.next();
			boolean hasLeGate = false;
			for (Iterator<Nodable> noIt=c.getNetlist(NccNetlist.SHORT_RESISTORS).getNodables(); noIt.hasNext();) {
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
        return c.hasParameters();
	}
	private void postProcess() {
		singleUseCells = findSingleUseCells();
		cellsWithLeGates = findCellsWithLeGate();
	}

	private CellUsage() {}

	// ------------------ Here's the real interface ------------------------
	// static constructor
	/** Scan the design hierarchy rooted at root and construct a list of
	 * all Cells instantiated by that design.
	 * @param root the root of the design
	 */
	public static CellUsage getCellUsage(CellContext root) {
		CellUsage visitor = new CellUsage();
		HierarchyEnumerator.enumerateCell(root.cell, root.context, visitor);
		visitor.postProcess();
		return visitor;
	}
	/** See if a particular Cell is used by the design.
	 * @param cell the Cell to look for.
	 * @return true if Cell occurs in the design
	 */
	public boolean cellIsUsed(Cell cell) {return cellsInUse.containsKey(cell);}
	/** Cell has only one size in the design */
	public boolean cellHasOnlyOneSize(Cell cell) {
        return singleUseCells.contains(cell) ||
           (!cellsWithLeGates.contains(cell)) && !cellIsParameterized(cell);
	}
	/** Get an iterator over a list of all the cells used in the design. The
	 * list is sorted in reverse topological order.
	 * @return the iterator over the list
	 */ 
	public Iterator<Cell> cellsInReverseTopologicalOrder() {
		return cellsInRevTopoOrder.iterator();
	}
	/** Get a CellContext for Cell cell. If the Cell is instantiated multiple
	 * times then arbitrarily select one instance and return the CellContext 
	 * for that instance. 
	 * @param cell the cell for which we want the CellContext
	 * @return a CellContext for the instance of Cell cell
	 */
	public CellContext getCellContext(Cell cell) {
		Job.error(!cellsInUse.containsKey(cell), "cell not found");
		return cellsInUse.get(cell);
	}
	/** A joinGroup annotation indicates that NCC should treat a Cell
	 * as belonging to a particular Electric CellGroup. The method
	 * getGroupAdditions passes along information from joinGroup
	 * annotations.  
	 * @return a Set of CellContexts to add to group */
	public Set<CellContext> getGroupAdditions(Cell.CellGroup group) {
		Set<CellContext> additions = groupToAdditions.get(group);
		return additions!=null ? additions : new HashSet<CellContext>();
	}
	/** Get the root Cell of the design */
	public Cell getRoot() {return root;}
}
