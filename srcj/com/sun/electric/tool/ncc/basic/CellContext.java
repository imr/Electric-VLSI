/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellContext.java
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

import java.io.Serializable;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.VarContext;

/** I might not need a CellContext because all the information might be 
 * derivable from the VarContext but I forget. */ 
public class CellContext implements Serializable {
	static final long serialVersionUID = 0;
	
	public final Cell cell;
	public final VarContext context;
	public CellContext(Cell cell, VarContext context) {
		this.cell = cell;
		this.context = context;
	}
	public boolean equals(Object o) {
		if (!(o instanceof CellContext)) return false;
		CellContext cc = (CellContext) o;
		return cell==cc.cell && context==cc.context;
	}
	public int hashCode() {
		return cell.hashCode() * context.hashCode();
	}
}
