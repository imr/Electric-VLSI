/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ImmutableNodeInst.java
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.database;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.technology.Technology;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

/**
 *
 */
public class ImmutableCell {
    
	/** CellId of this Cell. */                                     public final CellId cellId;
	/** The CellName of the Cell. */								public final CellName cellName;
	/** The CellGroup this Cell belongs to. */						public final Cell.CellGroup cellGroup;
	/** The library this Cell belongs to. */						public final LibId libId;
	/** The date this Cell was created. */							public final long creationDate;
	/** The date this Cell was last modified. */					public final long revisionDate;
	/** This Cell's Technology. */									public final Technology tech;
	/** Internal flag bits. */										public final int userBits;
    /** An array of Exports on the Cell by chronological index. */  public final ImmutableExport[] exports;
	/** A list of NodeInsts in this Cell. */						public final ImmutableNodeInst[] nodes;
//    /** Counts of NodeInsts for each CellUsage. */                  private int[] cellUsages = NULL_INT_ARRAY;
    /** A list of ArcInsts in this Cell. */							public final ImmutableArcInst[] arcs;
    /** Cell Variables. */                                          public final ImmutableElectricObject vars;

    /** Creates a new instance of ImmutableCell */
    public ImmutableCell(CellId cellId, CellName cellName, Cell.CellGroup cellGroup, LibId libId, long creationDate, long revisionDate, Technology tech, int userBits,
            ImmutableNodeInst[] nodes, ImmutableArcInst[] arcs, ImmutableExport[] exports, ImmutableElectricObject vars) {
        this.cellId = cellId;
        this.cellName = cellName;
        this.cellGroup = cellGroup;
        this.libId = libId;
        this.creationDate = creationDate;
        this.revisionDate = revisionDate;
        this.tech = tech;
        this.userBits = userBits;
        this.nodes = nodes;
        this.arcs = arcs;
        this.exports = exports;
        this.vars = vars;
    }
    
}
