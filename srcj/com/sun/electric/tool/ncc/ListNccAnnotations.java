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
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.User;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.WindowContent;

import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.basic.Messenger;
import com.sun.electric.tool.ncc.basic.NccCellAnnotations;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.database.variable.Variable;


class ScanHierForNccAnnot extends HierarchyEnumerator.Visitor {
	private HashSet enteredCells = new HashSet();

	private void scanForAnnot(Cell cell) {
		NccCellAnnotations ann = NccCellAnnotations.getAnnotations(cell);
		if (ann==null) return;
		System.out.println("  Cell: "+NccUtils.fullName(cell)+" annotations:");

		for (Iterator it=ann.getAnnotationText(); it.hasNext();) {
			System.out.println("    "+it.next());
		}
	}
	public boolean enterCell(HierarchyEnumerator.CellInfo info) {
		Cell cell = info.getCell();
		if (enteredCells.contains(cell))  return false;
		scanForAnnot(cell);
		enteredCells.add(cell);
		return true;
	}
	public void exitCell(HierarchyEnumerator.CellInfo info) {}
	public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info) {
		return true;
	}
} 

public class ListNccAnnotations extends Job {
    public boolean doIt() {
		Cell curCell = NccUtils.getCurrentCell();
		if (curCell==null) {
			System.out.println("Please open the root of the hierarchy for which you want to list NCC annotations.");
			return false;
		} 

		System.out.println("Listing all NCC annotations for hierarchy rooted at Cell: "+
						   NccUtils.fullName(curCell));
		
		ScanHierForNccAnnot visitor = new ScanHierForNccAnnot();
		HierarchyEnumerator.enumerateCell(curCell, null, null, visitor);
		
		System.out.println("Done listing NCC annotations");											   

		return true;
    }

	// ------------------------- public method --------------------------------
	public ListNccAnnotations() {
		super("List NCC Annotations", User.tool, Job.Type.CHANGE, 
			  null, null, Job.Priority.ANALYSIS);
		startJob();
	}
}
