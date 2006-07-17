/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IdManager.java
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

import com.sun.electric.database.text.CellName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class owns a set of LibIds and CellIds.
 */
public class IdManager {

    /** List of LibIds created so far. */
    private final ArrayList<LibId> libIds = new ArrayList<LibId>();
    /** HashMap of LibIds by their lib name. */
    private final HashMap<String,LibId> libIdsByName = new HashMap<String,LibId>();
    /** List of CellIds created so far. */
    private final ArrayList<CellId> cellIds = new ArrayList<CellId>();
    /** Count of Snapshots created with this IdManager. */
    private final AtomicInteger snapshotCount = new AtomicInteger();
    /** Initial Snapshot. */
    private final Snapshot initialSnapshot = new Snapshot(this);
    
    /** Creates a new instance of IdManager */
    public IdManager() {
    }
    
    /**
     * Returns LibId with specified libName.
     * @param libName library name.
     * @return LibId with specified libName.
     */
    public LibId newLibId(String libName) {
        synchronized(libIds) {
            LibId libId = libIdsByName.get(libName);
            return libId != null ? libId : newLibIdInternal(libName);
        }
    }
    
    /**
     * Returns LibId by given index.
     * @param libIndex given index.
     * @return LibId with given index.
     */
    public LibId getLibId(int libIndex) {
        synchronized(libIds) {
           return libIds.get(libIndex);
        }
    }
    
    private LibId newLibIdInternal(String libName) {
        int libIndex = libIds.size();
        LibId libId = new LibId(this, libName, libIndex);
        libIds.add(libId);
        libIdsByName.put(libName, libId);
        assert libIds.size() == libIdsByName.size();
        return libId;
    }
    
    /**
     * Returns new CellId with cellIndex unique in this IdManager.
     * @return new CellId.
     */
    CellId newCellId(CellName cellName) {
        synchronized(cellIds) {
            return newCellIdInternal(cellName);
        }
    }
    
    /**
     * Returns CellId by given index.
     * @param cellIndex given index.
     * @return CellId with given index.
     */
    public CellId getCellId(int cellIndex) {
        synchronized (cellIds) {
           return cellIds.get(cellIndex);
        }
    }
    
    private CellId newCellIdInternal(CellName cellName) {
        int cellIndex = cellIds.size();
        CellId cellId = new CellId(this, cellName, cellIndex);
        cellIds.add(cellId);
        return cellId;
    }
    
    public Snapshot getInitialSnapshot() { return initialSnapshot; }
    
    int newSnapshotId() { return snapshotCount.incrementAndGet(); }
    
    void writeDiffs(SnapshotWriter writer) throws IOException {
        LibId[] libIdsArray;
        CellId[] cellIdsArray;
        synchronized (libIds) {
            libIdsArray = libIds.toArray(LibId.NULL_ARRAY);
        }
        synchronized (cellIds) {
            cellIdsArray = cellIds.toArray(CellId.NULL_ARRAY);
        }
        writer.writeInt(libIdsArray.length);
        for (int libIndex = writer.libCount; libIndex < libIdsArray.length; libIndex++) {
            LibId libId = libIdsArray[libIndex];
            writer.writeString(libId.libName);
        }
        writer.setLibCount(libIdsArray.length);
        writer.writeInt(cellIdsArray.length);
        for (int cellIndex = writer.exportCounts.length; cellIndex < cellIdsArray.length; cellIndex++) {
            CellId cellId = cellIdsArray[cellIndex];
            writer.writeString(cellId.cellName.toString());
        }
        writer.setCellCount(cellIdsArray.length);
        for (int cellIndex = 0; cellIndex < cellIdsArray.length; cellIndex++) {
            CellId cellId = cellIdsArray[cellIndex];
            ExportId[] exportIds = cellId.getExportIds();
            int exportCount = writer.exportCounts[cellIndex];
            if (exportIds.length != exportCount) {
                writer.writeInt(cellIndex);
                int numNewExportIds = exportIds.length - exportCount;
                assert numNewExportIds > 0;
                writer.writeInt(numNewExportIds);
                for (int i = 0; i < numNewExportIds; i++)
                    writer.writeString(exportIds[exportCount + i].externalId);
                writer.exportCounts[cellIndex] = exportIds.length;
            }
        }
        writer.writeInt(-1);
    }
    
    void readDiffs(SnapshotReader reader) throws IOException {
        int libIdsCount = reader.readInt();
        synchronized (libIds) {
           while (libIdsCount > libIds.size())
               newLibIdInternal(reader.readString());
        }
        int cellIdsCount = reader.readInt();
        synchronized (cellIds) {
           while (cellIdsCount > cellIds.size())
               newCellIdInternal(CellName.parseName(reader.readString()));
        }
        for (;;) {
            int cellIndex = reader.readInt();
            if (cellIndex == -1) break;
            CellId cellId = getCellId(cellIndex);
            int numNewExportIds = reader.readInt();
            String[] newExportIds = new String[numNewExportIds];
            for (int i = 0; i < newExportIds.length; i++)
                newExportIds[i] = reader.readString();
            cellId.newExportIds(newExportIds);
        }
    }
    
	/**
	 * Method to check invariants in all Libraries.
	 */
	public void checkInvariants() {
        int numLibIds;
        synchronized (libIds) {
            numLibIds = libIds.size();
            assert numLibIds == libIdsByName.size();
        }
        for (int i = 0; i < numLibIds; i++) {
            LibId libId;
            synchronized (libIds) { libId = (LibId)libIds.get(i); }
            libId.check();
        }
        int numCellIds;
        synchronized (cellIds) { numCellIds = cellIds.size(); }
        for (int i = 0; i < numCellIds; i++) {
            CellId cellId;
            synchronized (cellIds) { cellId = (CellId)cellIds.get(i); }
            assert cellId.cellIndex == i;
            cellId.check();
        }
    }
}
