/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LayoutNetlist.java
 *
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.tool.generator.flag;

import java.util.List;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.generator.flag.router.ToConnect;

/** Contains instance and connectivity information for the layout Cell */
public class LayoutNetlist {
	private final Cell layCell;
    private final List<NodeInst> layInsts;
    private final List<ToConnect> toConns;
    LayoutNetlist(Cell layCell, List<NodeInst> layInsts, List<ToConnect> toConns) {
    	this.layCell = layCell;
    	this.layInsts = layInsts;
    	this.toConns = toConns;
    }
    
    // ---------------------------- public methods ----------------------------
    public Cell getLayoutCell() {return layCell;}
    public List<NodeInst> getLayoutInstancesSortedBySchematicPosition() {
    	return layInsts;
    }
    public List<ToConnect> getToConnects() {return toConns;}
}
