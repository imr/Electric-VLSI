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
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.generator.layout.LayoutLib;

/** Information needed to perform hierarchical netlist comparison */
public class HierarchyInfo {
	/** unique int assigned to the subcircuit being compared */
	private int subcktID;
	/** name (perhaps a Cell group name) of the subcircuit being compared */ 
	private String subcktName;
	/** information about descendent subcircuits */
	private Map cellToSubcktInfo = new HashMap();

	public HierarchyInfo() {}

	/** specify the name of the subcircuit being compared. Call this 
	 * when we begin a new subcircuit comparison */
	public void nextSubcircuit(String subcktName) {
		this.subcktName = subcktName;
		subcktID++;
	}
	/** name of the subcircuit being compared */
	public String currentSubcircuitName() {return subcktName;}
	/** unique int ID of subcircuit being compared */
	public int currentSubcircuitID() {return subcktID;}
	/** add new subcircuit information for the current subcircuit so it can
	 * be treated as a subcircuit primitive in a future hierarchical 
	 * comparison at a higher level */
	public void addSubcircuitInfo(Cell c, SubcircuitInfo subcktInfo) {
		LayoutLib.error(cellToSubcktInfo.containsKey(c),
		                "SubcircuitInfo already exists for Cell");
		cellToSubcktInfo.put(c, subcktInfo);
	}
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
