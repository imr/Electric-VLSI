/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IdMapper.java
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
package com.sun.electric.database;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;

/**
 * Class to describe mapping of Electric database Ids (LibIds, CellIds, ExportIds).
 */
public class IdMapper implements Serializable {
    
    private final HashMap<LibId,LibId> libIdMap = new HashMap<LibId,LibId>();
    private final HashMap<CellId,CellId> cellIdMap = new HashMap<CellId,CellId>();
    private final HashMap<ExportId,ExportId> exportIdMap = new HashMap<ExportId,ExportId>();
    
    /** Creates a new instance of IdMapper */
    public IdMapper() {
    }
    
    public static IdMapper renameLibrary(Snapshot snapshot, LibId oldLibId, LibId newLibId) {
        IdMapper idMapper = new IdMapper();
        idMapper.libIdMap.put(oldLibId, newLibId);
        for (CellBackup cellBackup: snapshot.cellBackups) {
            if (cellBackup == null) continue;
            if (cellBackup.d.libId != oldLibId) continue;
            CellId oldCellId = cellBackup.d.cellId;
            CellId newCellId = newLibId.newCellId(null);
            idMapper.cellIdMap.put(oldCellId, newCellId);
            String[] externalIds = new String[cellBackup.exports.size()];
            for (int i = 0; i < externalIds.length; i++) {
                ImmutableExport e = cellBackup.exports.get(i);
                externalIds[i] = e.exportId.externalId;
            }
            newCellId.newExportIds(externalIds);
            for (int i = 0; i < externalIds.length; i++) {
                idMapper.exportIdMap.put(cellBackup.exports.get(i).exportId, newCellId.findExportId(externalIds[i]));
            }
        }
        return idMapper;
    }

    /**
     * Get mappinmg of LibId.
     * @param key key LibId.
     * @return LibId which is the mapping of the key.
     */
    public LibId get(LibId key) {
        LibId value = libIdMap.get(key);
        return value != null ? value : key;
    }
    
    /**
     * Get mappinmg of CellId.
     * @param key key CellId.
     * @return CellId which is the mapping of the key.
     */
    public CellId get(CellId key) {
        CellId value = cellIdMap.get(key);
        return value != null ? value : key;
    }
    
    /**
     * Get mappinmg of ExportId.
     * @param key key ExportId.
     * @return ExportId which is the mapping of the key.
     */
    public ExportId get(ExportId key) {
        ExportId value = exportIdMap.get(key);
        return value != null ? value : key;
    }

    public Collection<CellId> getNewCellIds() { return cellIdMap.values(); }
    public Collection<ExportId> getNewExportIds() { return exportIdMap.values(); }
    
}
