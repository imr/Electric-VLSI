/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ExportId.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.id;

import com.sun.electric.database.CellRevision;
import com.sun.electric.database.ImmutableExport;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Export;

/**
 * The ExportId immutable class identifies a type of PortInst independently of threads.
 * It differs from Export objects, which will be owned by threads in transactional database.
 * This class is thread-safe except inCurrentThread method .
 */
public final class ExportId extends PortProtoId {

    /**
     * ExportId constructor.
     */
    ExportId(CellId parentId, int chronIndex, String externalId) {
        super(parentId, externalId, chronIndex);
//        if (externalId.length() == 0)
//            throw new IllegalArgumentException("ExportId");
    }

    /**
     * Method to return the parent NodeProtoId of this ExportId.
     * @return the parent NodeProtoId of this ExportId.
     */
    @Override
    public CellId getParentId() {
        return (CellId) parentId;
    }

    /**
     * Method to return the name of this PortProtoId in a specified Snapshot.
     * @param snapshot snapshot for name search.
     * @return the name of this PortProtoId.
     */
    @Override
    public String getName(Snapshot snapshot) {
        return inSnapshot(snapshot).name.toString();
    }

    /**
     * Method to return the ImmutableExport representing ExportId in the specified Snapshot.
     * @param snapshot Snapshot where to get from.
     * @return the ImmutableExport representing ExportId in the specified snapshot.
     */
    public ImmutableExport inSnapshot(Snapshot snapshot) {
        CellRevision cellRevision = snapshot.getCellRevision(getParentId());
        return cellRevision != null ? cellRevision.getExport(this) : null;
    }

    /**
     * Method to return the Export representing ExportId in the specified EDatabase.
     * @param database EDatabase where to get from.
     * @return the Export representing ExportId in the specified database.
     * This method is not properly synchronized.
     */
    @Override
    public Export inDatabase(EDatabase database) {
        Cell cell = database.getCell(getParentId());
        return cell != null ? cell.getExportChron(chronIndex) : null;
    }

    /**
     * Check invariants of this ExportId.
     * @throws AssertionError if this ExportId is not valid.
     */
    @Override
    void check() {
        super.check();
//         assert externalId.length() > 0;
    }
}
