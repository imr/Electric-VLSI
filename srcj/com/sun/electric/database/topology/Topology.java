/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Topology.java
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.topology;

import com.sun.electric.database.CellId;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;

import java.awt.geom.Rectangle2D;
import java.util.Iterator;

/**
 * A class to manage nodes and arcs of a Cell.
 */
public class Topology {
    /** Owner cell of this Topology. */                             private final Cell cell;
	/** The geometric data structure. */							private RTNode rTree = RTNode.makeTopLevel();
    /** True of RTree matches node/arc sizes */                     private boolean rTreeFresh;
    
    /** Creates a new instance of Topology */
    public Topology(Cell cell) {
        this.cell = cell;
    }
    
    /**
	 * Method to return an interator over all Geometric objects in a given area of this Cell that allows
     * to ignore elements touching the area.
	 * @param bounds the specified area to search.
     * @param includeEdges true if Geometric objects along edges are considered in.
	 * @return an iterator over all of the Geometric objects in that area.
	 */
    public Iterator<Geometric> searchIterator(Rectangle2D bounds, boolean includeEdges) {
        return new RTNode.Search(bounds, getRTree(), includeEdges);
    }

    public void unfreshRTree() {
        rTreeFresh = false;
    }
    
	/**
	 * Method to R-Tree of this Cell.
	 * The R-Tree organizes all of the Geometric objects spatially for quick search.
	 * @return R-Tree of this Cell.
	 */
    private RTNode getRTree() {
        if (rTreeFresh) return rTree;
        EDatabase database = cell.getDatabase();
        if (database.canComputeBounds()) {
            rebuildRTree();
            rTreeFresh = true;
        } else {
            Snapshot snapshotBefore = database.getFreshSnapshot();
            rebuildRTree();
            rTreeFresh = snapshotBefore != null && database.getFreshSnapshot() == snapshotBefore;
        }
        return rTree;
    }

    private void rebuildRTree() {
//        long startTime = System.currentTimeMillis();
        CellId cellId = cell.getId();
        RTNode root = RTNode.makeTopLevel();
        for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); ) {
            NodeInst ni = it.next();
            root = RTNode.linkGeom(cellId, root, ni);
        }
        for (Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); ) {
            ArcInst ai = it.next();
            root = RTNode.linkGeom(cellId, root, ai);
        }
        root.checkRTree(0, cellId);
        rTree = root;
//        long stopTime = System.currentTimeMillis();
//        if (Job.getDebug()) System.out.println("Rebuilding R-Tree in " + this + " took " + (stopTime - startTime) + " msec");
    }
    
    /**
     * Method to check invariants in this Cell.
     * @exception AssertionError if invariants are not valid
     */
    public void check() {
        if (rTreeFresh)
            rTree.checkRTree(0, cell.getId());
    }
}
