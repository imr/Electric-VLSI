/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellBackup.java
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
import com.sun.electric.database.text.CellName;
import com.sun.electric.technology.Technology;
import java.io.IOException;
/**
 *
 */
public class CellBackup {
    
    /** Cell persistent data. */                                    public final ImmutableCell d;
	/** This Cell is mainSchematics in its group. */				public final boolean isMainSchematics;
    /** An array of Exports on the Cell by chronological index. */  public final ImmutableExport[] exports;
	/** A list of NodeInsts in this Cell. */						public final ImmutableNodeInst[] nodes;
    /** Counts of NodeInsts for each CellUsage. */                  public final int[] cellUsages;
    /** A list of ArcInsts in this Cell. */							public final ImmutableArcInst[] arcs;

    /** Creates a new instance of ImmutableCell */
    public CellBackup(ImmutableCell d, boolean isMainSchematics,
            ImmutableNodeInst[] nodes, ImmutableArcInst[] arcs, ImmutableExport[] exports, int[] cellUsages) {
        this.d = d;
        this.isMainSchematics = isMainSchematics;
        this.nodes = nodes;
        this.arcs = arcs;
        this.exports = exports;
        this.cellUsages = cellUsages;
    }
    
    /**
     * Writes this CellBackup to SnapshotWriter.
     * @param writer where to write.
     */
    void write(SnapshotWriter writer) throws IOException {
        d.write(writer);
        writer.out.writeBoolean(isMainSchematics);
        writer.out.writeInt(nodes.length);
        for (int i = 0; i < nodes.length; i++)
            nodes[i].write(writer);
        writer.out.writeInt(arcs.length);
        for (int i = 0; i < arcs.length; i++)
            arcs[i].write(writer);
        writer.out.writeInt(exports.length);
        for (int i = 0; i < exports.length; i++)
            exports[i].write(writer);
    }
    
    /**
     * Reads CellBackup from SnapshotReader.
     * @param reader where to read.
     */
    static CellBackup read(SnapshotReader reader) throws IOException {
        ImmutableCell d = ImmutableCell.read(reader);
        boolean isMainSchematics = reader.in.readBoolean();
        int nodesLength = reader.in.readInt();
        ImmutableNodeInst[] nodes = new ImmutableNodeInst[nodesLength];
        int[] cellUsages = new int[0];
        for (int i = 0; i < nodesLength; i++) {
            ImmutableNodeInst nid = ImmutableNodeInst.read(reader);
            if (nid.protoId instanceof CellId) {
                CellId subCellId = (CellId)nid.protoId;
                CellUsage u = d.cellId.getUsageIn(subCellId);
                if (cellUsages.length <= u.indexInParent) {
                    int[] newCellUsages = new int[u.indexInParent + 1];
                    System.arraycopy(cellUsages, 0, newCellUsages, 0, cellUsages.length);
                    cellUsages = newCellUsages;
                }
                cellUsages[u.indexInParent]++;
            }
            nodes[i] = nid;
        }
        int arcsLength = reader.in.readInt();
        ImmutableArcInst[] arcs = new ImmutableArcInst[arcsLength];
        for (int i = 0; i < arcsLength; i++)
            arcs[i] = ImmutableArcInst.read(reader);
        int exportsLength = reader.in.readInt();
        ImmutableExport[] exports = new ImmutableExport[exportsLength];
        for (int i = 0; i < exportsLength; i++)
            exports[i] = ImmutableExport.read(reader);
        return new CellBackup(d, isMainSchematics, nodes, arcs, exports, cellUsages);
    }
    
    public boolean sameExports(CellBackup thatBackup) {
    	if (exports.length != thatBackup.exports.length)
    		return false;
    	for (int i = 0; i < exports.length; i++) {
    		if (exports[i].exportId != thatBackup.exports[i].exportId)
    			return false;
    	}
    	return true;
    }
}
