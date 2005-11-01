/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ListNccAnnotations.java
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
import java.util.HashSet;
import java.util.Iterator;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.tool.ncc.basic.NccCellAnnotations;
import com.sun.electric.tool.ncc.basic.NccUtils;

class ScanHierForNccAnnot extends HierarchyEnumerator.Visitor {
	private HashSet<Cell> enteredCells = new HashSet<Cell>();

	private void prln(String s) {System.out.println(s);}
	private void printAnn(Cell cell) {
		NccCellAnnotations ann = NccCellAnnotations.getAnnotations(cell);
		if (ann==null) return;
		prln("  Cell: "+NccUtils.fullName(cell)+" annotations:");

		for (Iterator<String> it=ann.getAnnotationText(); it.hasNext();) {
			prln("    "+it.next());
		}
	}
	public boolean enterCell(HierarchyEnumerator.CellInfo info) {
		Cell cell = info.getCell();
		if (enteredCells.contains(cell))  return false;
		printAnn(cell);
		enteredCells.add(cell);
		return true;
	}
	public void exitCell(HierarchyEnumerator.CellInfo info) {}
	public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info) {
		return true;
	}
} 

public class ListNccAnnotations {
	private static void prln(String s) {System.out.println(s);}
	private static void scanHierarchy(Cell cell) {
		prln("Listing NCC annotations for all Cells "+
						   "in the hierarchy rooted at Cell: "+
						   NccUtils.fullName(cell));
		ScanHierForNccAnnot visitor = new ScanHierForNccAnnot();
		HierarchyEnumerator.enumerateCell(cell, null, visitor);
//		HierarchyEnumerator.enumerateCell(cell, null, null, visitor);
	}


	// ------------------------- public method --------------------------------
	public static void doYourJob(Cell cell1, Cell cell2) {
		scanHierarchy(cell1);
		scanHierarchy(cell2);

		prln("Done listing NCC annotations");											   
	}
}
