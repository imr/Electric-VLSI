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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.basic.CellContext;
import com.sun.electric.tool.ncc.basic.CompareList;
import com.sun.electric.tool.ncc.basic.CompareLists;

/** Information needed to perform hierarchical netlist comparison */
public class HierarchyInfo {
	/** name of the current CompareList (usually CellGroup name) */ 
	private String subcktName;
	/** unique int assigned to the current compareList */
	private int compareListID=0;
	/** the Cells we've added to the current compareList */
	private List<Cell> cellsInCompareList = new ArrayList<Cell>();
	/** flag indicating that we must remove SubcircuitInfo for all Cells in the 
	 * current Cell Group. */
	private boolean purgeCurrentCompareList;
	/** information for all Cells in all the compareLists we've encountered 
	 * so far */
	private Map<Cell,SubcircuitInfo> cellToSubcktInfo = new HashMap<Cell,SubcircuitInfo>();
	private Set<Cell> cellsInSharedCellGroups;

	// ----------------------------- public methods ---------------------------
	/** You must call this before you begin comparing Cells in a new 
	 * compareList. Then for each Cell in the compareList you must call 
	 * addSubcircuitInfo(). However, if a comparison reveals an Export name 
	 * mismatch then you must call purgeCompareList() after which it 
	 * doesn't matter what you do for the rest of the compareList. */
	public void beginNextCompareList(String subcktName) {
		this.subcktName = subcktName;
		compareListID++;
		purgeCurrentCompareList = false;
		cellsInCompareList.clear();
		cellsInSharedCellGroups = null;
	}
	/** Restrict subcircuit detection. We only want to detect a subcircuit if
	 * its corresponding CellGroup is instantiated by both Cells begin compared.
	 * When we perform a hierarchical comparison we need to rescan the 
	 * sub-hierarchies for each pair of Cells being compared. 
	 * <p> 
	 * Also, we should not detect a subcircuit if the Cell is in the current
	 * compare list. Here's the weird case that motivates this.  Suppose B{lay}
	 * instantiates A{lay}. Suppose the designer compares A{sch} with B{lay}. 
	 * NCC builds a compare list with A{sch}, A{lay}, and B{lay}. NCC might 
	 * first compare A{sch} with A{lay}. Then when NCC compares A{sch} with
	 * B{lay} it will fail because A gets treated as a subcircuit. However,
	 * if NCC first compares A{sch} with B{lay} then the second comparison:
	 * A{sch} with A{lay} will pass. Unfortunately, NCC's choice is random.
	 * Successive runs produce different results.
	 * <p>  
	 * In general the compare list might have n schematics and m layouts and 
	 * the schematics may instantiate one-another and the layouts might 
	 * instantiate one another. If we prevent all the schematics and layouts
	 * from being treated as subcells then any order of comparison should 
	 * pass. */
	public void restrictSubcktDetection(CellContext cc1, CellContext cc2,
			                            Set compareListCells) {
		List<CompareList> compareLists = CompareLists.getCompareLists(cc1, cc2);
		cellsInSharedCellGroups = new HashSet<Cell>();
		for (CompareList compareList : compareLists) {
			for (Iterator<CellContext> it2=compareList.iterator(); it2.hasNext();) {
				Cell c = it2.next().cell;
				if (!compareListCells.contains(c)) cellsInSharedCellGroups.add(c);
			}
		}
	}
	/** Add new subcircuit information for each Cell in the compareList so it
	 * can be treated as a subcircuit primitive in a future 
	 * hierarchical comparison at a higher level. This method must be called
	 * for each Cell in the compareList. 
	 * <p>However, if a comparison reveals an Export name mismatch then you must
	 * call purgeCurrentCompareList() after which it doesn't matter what 
	 * you do. */
	public void addSubcircuitInfo(Cell c, SubcircuitInfo subcktInfo) {
		if (purgeCurrentCompareList) return;
		LayoutLib.error(cellToSubcktInfo.containsKey(c),
						"SubcircuitInfo already exists for Cell");
		cellsInCompareList.add(c);
		cellToSubcktInfo.put(c, subcktInfo);
	}
	/** You must call this method if a Cell comparison reveals an Export name 
	 * mismatch. In that case we can no longer treat cells in compareList as a 
	 * subcircuit primitives at higher levels. Instead we must purge all 
	 * information related to Cells in this compareList so that we will flatten 
	 * through them when comparing from higher levels in the hierarchy. */
	public void purgeCurrentCompareList() {
		purgeCurrentCompareList = true;
		for (Cell c : cellsInCompareList) {
			LayoutLib.error(!cellToSubcktInfo.containsKey(c), "Cell not in map?");
			cellToSubcktInfo.remove(c);
		}
		cellsInCompareList.clear();
	}

	/** name of the subcircuit being compared */
	public String currentSubcircuitName() {return subcktName;}
	/** unique int ID of subcircuit being compared */
	public int currentSubcircuitID() {return compareListID;}
	/** should I treat an instance of this cell as a subcircuit primitive 
	 * in the current comparison? */
	public boolean treatAsPrimitive(Cell c) {
		return cellToSubcktInfo.containsKey(c) &&
		       (cellsInSharedCellGroups==null || 
		       	cellsInSharedCellGroups.contains(c));
	}
	/** get me information I need to treat an instance of this Cell as a
	 * subcircuit primitive */ 
	public SubcircuitInfo getSubcircuitInfo(Cell c) {
		return (SubcircuitInfo) cellToSubcktInfo.get(c);
	}
}
