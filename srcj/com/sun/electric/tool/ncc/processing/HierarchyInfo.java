/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: HierarchyInfo.java
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
package com.sun.electric.tool.ncc.processing;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.generator.layout.LayoutLib;

/** Information needed to perform hierarchical netlist comparison */
public class HierarchyInfo {
	/** name of the current CellGroup */ 
	private String subcktName;
	/** unique int assigned to the current CellGroup */
	private int cellGroupID=0;
	/** the Cells we've added to the current CellGroup */
	private List cellsInCellGroup = new ArrayList();
	/** flag indicating that at least one comparison has revealed an Export
	 * name mismatch for the current CellGroup */
	private boolean exportNameMismatchInCellGroup;
	/** information for all Cells in all the CellGroups we've encountered 
	 * so far */
	private Map cellToSubcktInfo = new HashMap();

	/** You must call this before you begin comparing Cells in a new CellGroup.
	 * Then for each Cell in the CellGroup you must call addSubcircuitInfo().
	 * However, if a comparison reveals an Export name mismatch then you must
	 * call exportNameMismatchInCellGroup() after which it doesn't matter what 
	 * you do for the rest of the CellGroup. */
	public void beginNextCellGroup(String subcktName) {
		this.subcktName = subcktName;
		cellGroupID++;
		exportNameMismatchInCellGroup = false;
		cellsInCellGroup.clear();
	}
	/** Add new subcircuit information for each Cell in the CellGroup so the
	 * CellGroup can be treated as a subcircuit primitive in a future 
	 * hierarchical comparison at a higher level. This method must be called
	 * for each Cell in the Cell group. 
	 * <p>However, if a comparison reveals an Export name mismatch then you must
	 * call exportNameMismatchInCellGroup() after which it doesn't matter what 
	 * you do. */
	public void addSubcircuitInfo(Cell c, SubcircuitInfo subcktInfo) {
		if (exportNameMismatchInCellGroup) return;
		LayoutLib.error(cellToSubcktInfo.containsKey(c),
						"SubcircuitInfo already exists for Cell");
		cellsInCellGroup.add(c);
		cellToSubcktInfo.put(c, subcktInfo);
	}
	/** You must call this method if a Cell comparison reveals an Export name 
	 * mismatch. In that case we can no longer treat the CellGroup as a 
	 * subcircuit primitive at higher levels. Instead we must purge all 
	 * information related to Cells in this CellGroup so that we will flatten 
	 * through this Cell group when comparing from higher levels in the 
	 * hierarchy. */
	public void exportNameMismatchInCellGroup() {
		exportNameMismatchInCellGroup = true;
		for (Iterator it=cellsInCellGroup.iterator(); it.hasNext();) {
			Cell c = (Cell) it.next();
			LayoutLib.error(!cellToSubcktInfo.containsKey(c), "Cell not in map?");
			cellToSubcktInfo.remove(c);
		}
		cellsInCellGroup.clear();
	}

	/** name of the subcircuit being compared */
	public String currentSubcircuitName() {return subcktName;}
	/** unique int ID of subcircuit being compared */
	public int currentSubcircuitID() {return cellGroupID;}
	/** should I treat an instance of this cell as a subcircuit primitive 
	 * in the current comparison? */
	public boolean treatAsPrimitive(Cell c) {
		return cellToSubcktInfo.containsKey(c);
	}
	/** get me information I need to treat an instance of this Cell as a
	 * subcircuit primitive */ 
	public SubcircuitInfo getSubcircuitInfo(Cell c) {
		return (SubcircuitInfo) cellToSubcktInfo.get(c);
	}
}
