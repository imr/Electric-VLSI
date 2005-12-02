/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellGroupBackup.java
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
public class CellGroupBackup {
    
    public final CellId[] cellIds;
    public final CellId mainSchematics;
    
    /** Creates a new instance of CellGroupBackup */
    public CellGroupBackup(CellId[] cellIds, CellId mainSchematics) {
        this.cellIds = cellIds;
        this.mainSchematics = mainSchematics;
    }
    
    /**
     * Writes this CellGroupBackup to SnapshotWriter.
     * @param writer where to write.
     */
    void write(SnapshotWriter writer) throws IOException {
        writer.out.writeInt(cellIds.length);
        for (int i = 0; i < cellIds.length; i++)
            writer.writeNodeProtoId(cellIds[i]);
        writer.out.writeBoolean(mainSchematics != null);
        if (mainSchematics != null)
            writer.writeNodeProtoId(mainSchematics);
    }
    
    /**
     * Reads CellGroupBackup from SnapshotReader.
     * @param reader where to read.
     */
    static CellGroupBackup read(SnapshotReader reader) throws IOException {
        int length = reader.in.readInt();
        CellId[] cellIds = new CellId[length];
        for (int i = 0; i < length; i++)
            cellIds[i] = (CellId)reader.readNodeProtoId();
        boolean hasMain = reader.in.readBoolean();
        CellId mainSchematics = hasMain ? (CellId)reader.readNodeProtoId() : null;
        return new CellGroupBackup(cellIds, mainSchematics);
    }
}
