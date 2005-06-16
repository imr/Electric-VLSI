/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CompareLists.java
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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.generator.layout.LayoutLib;

public class CompareLists {
	
	private boolean alreadyCompared(Set compared, CompareList compareList) {
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
			CompareList compareList = new CompareList(cell, use1, use2);

			// Really subtle! If A instantiates B and B instantiates C and if A, B, 
			// and C are all in the same Cell group then this loop will encounter
			// the Cell group {A, B, C} three times!  Only perform the comparison
			// once.
			if (alreadyCompared(compared, compareList)) continue;

			if (compareList.empty()) continue;
			
			for (Iterator it2=compareList.iterator(); it2.hasNext();) 
				compared.add(it2.next());
			compareLists.add(compareList);
		}
		return compareLists;
	}
	private List getCompareLists1(CellContext cc1, CellContext cc2) {
		CellUsage use1 = CellUsage.getCellUsage(cc1);
		CellUsage use2 = CellUsage.getCellUsage(cc2);
		return getCompareLists(use1, use2);
	}
	
	public static List getCompareLists(CellContext cc1, CellContext cc2) {
		return (new CompareLists()).getCompareLists1(cc1, cc2);
	}
}

