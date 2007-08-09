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
package com.sun.electric.database;

import com.sun.electric.database.text.CellName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class owns a set of LibIds and CellIds.
 */
public class IdManager {

    /** List of TechIds created so far. */
    private final ArrayList<TechId> techIds = new ArrayList<TechId>();
    /** HashMap of TechIds by their tech name. */
    private final HashMap<String,TechId> techIdsByName = new HashMap<String,TechId>();
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
     * Returns TechId with specified techName.
     * @param techName technology name.
     * @return TechId with specified techName.
     */
    public synchronized TechId newTechId(String techName) {
        TechId techId = techIdsByName.get(techName);
        return techId != null ? techId : newTechIdInternal(techName);
    }
    
    /**
     * Returns TechId by given index.
     * @param techIndex given index.
     * @return TechId with given index.
     */
    public synchronized TechId getTechId(int techIndex) {
        return techIds.get(techIndex);
    }
    
    private TechId newTechIdInternal(String techName) {
        int techIndex = techIds.size();
        TechId techId = new TechId(this, techName, techIndex);
        techIds.add(techId);
        techIdsByName.put(techName, techId);
        assert techIds.size() == techIdsByName.size();
        return techId;
    }
    
    /**
     * Returns LibId with specified libName.
     * @param libName library name.
     * @return LibId with specified libName.
     */
    public synchronized LibId newLibId(String libName) {
        LibId libId = libIdsByName.get(libName);
        return libId != null ? libId : newLibIdInternal(libName);
    }
    
    /**
     * Returns LibId by given index.
     * @param libIndex given index.
     * @return LibId with given index.
     */
    public synchronized LibId getLibId(int libIndex) {
        return libIds.get(libIndex);
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
     * @param libId library to which the Cell belongs
     * @param cellName name of the Cell.
     * @return new CellId.
     */
    synchronized CellId newCellId(LibId libId, CellName cellName) {
        assert libId.idManager == this;
        CellId cellId = libId.getCellId(cellName);
        return cellId != null ? cellId : newCellIdInternal(libId, cellName);
    }
    
    /**
     * Returns CellId by given index.
     * @param cellIndex given index.
     * @return CellId with given index.
     */
    public synchronized CellId getCellId(int cellIndex) {
        return cellIds.get(cellIndex);
    }
    
    private CellId newCellIdInternal(LibId libId, CellName cellName) {
        int cellIndex = cellIds.size();
        CellId cellId = new CellId(libId, cellName, cellIndex);
        cellIds.add(cellId);
        libId.putCellId(cellId);
        return cellId;
    }
    
    public Snapshot getInitialSnapshot() { return initialSnapshot; }
    
    int newSnapshotId() { return snapshotCount.incrementAndGet(); }
    
    void writeDiffs(SnapshotWriter writer) throws IOException {
        assert writer.idManager == this;
        TechId[] techIdsArray;
        LibId[] libIdsArray;
        CellId[] cellIdsArray;
        synchronized (this) {
            techIdsArray = techIds.toArray(TechId.NULL_ARRAY);
            libIdsArray = libIds.toArray(LibId.NULL_ARRAY);
            cellIdsArray = cellIds.toArray(CellId.NULL_ARRAY);
        }
        writer.writeInt(techIdsArray.length);
        for (int techIndex = writer.techCount; techIndex < techIdsArray.length; techIndex++) {
            TechId techId = techIdsArray[techIndex];
            writer.writeString(techId.techName);
        }
        writer.setTechCount(techIdsArray.length);
        writer.writeInt(libIdsArray.length);
        for (int libIndex = writer.libCount; libIndex < libIdsArray.length; libIndex++) {
            LibId libId = libIdsArray[libIndex];
            writer.writeString(libId.libName);
        }
        writer.setLibCount(libIdsArray.length);
        writer.writeInt(cellIdsArray.length);
        for (int cellIndex = writer.exportCounts.length; cellIndex < cellIdsArray.length; cellIndex++) {
            CellId cellId = cellIdsArray[cellIndex];
            writer.writeLibId(cellId.libId);
            writer.writeString(cellId.cellName.toString());
        }
        writer.setCellCount(cellIdsArray.length);
        for (int cellIndex = 0; cellIndex < cellIdsArray.length; cellIndex++) {
            CellId cellId = cellIdsArray[cellIndex];
            int numExportIds = cellId.numExportIds();
            int exportCount = writer.exportCounts[cellIndex];
            if (numExportIds != exportCount) {
                writer.writeInt(cellIndex);
                int numNewExportIds = numExportIds - exportCount;
                assert numNewExportIds > 0;
                writer.writeInt(numNewExportIds);
                for (int i = 0; i < numNewExportIds; i++)
                    writer.writeString(cellId.getPortId(exportCount + i).externalId);
                writer.exportCounts[cellIndex] = numExportIds;
            }
        }
        writer.writeInt(-1);
    }
    
    void readDiffs(SnapshotReader reader) throws IOException {
        int oldTechIdsCount, oldLibIdsCount, oldCellIdsCount;
        synchronized (this) {
            oldTechIdsCount = techIds.size();
            oldLibIdsCount = libIds.size();
            oldCellIdsCount = cellIds.size();
        }
        int techIdsCount = reader.readInt();
        for (int techIndex = oldTechIdsCount; techIndex < techIdsCount; techIndex++)
            newTechId(reader.readString());
        int libIdsCount = reader.readInt();
        for (int libIndex = oldLibIdsCount; libIndex < libIdsCount; libIndex++)
            newLibId(reader.readString());
        int cellIdsCount = reader.readInt();
        for (int cellIndex = oldCellIdsCount; cellIndex < cellIdsCount; cellIndex++) {
            LibId libId = reader.readLibId();
            newCellId(libId, CellName.parseName(reader.readString()));
        }
        synchronized (this) {
            assert techIdsCount == techIds.size();
            assert libIdsCount == libIds.size();
            assert cellIdsCount == cellIds.size();
        }
        for (;;) {
            int cellIndex = reader.readInt();
            if (cellIndex == -1) break;
            CellId cellId = getCellId(cellIndex);
            int numNewExportIds = reader.readInt();
            for (int i = 0; i < numNewExportIds; i++) {
                String exportIdString = reader.readString();
                cellId.newExportId(exportIdString);
            }
        }
    }
    
	/**
	 * Method to check invariants in all Libraries.
	 */
	public void checkInvariants() {
        int numTechIds;
        int numLibIds;
        int numCellIds;
        synchronized (this) {
            numTechIds = techIds.size();
            assert numTechIds == techIdsByName.size();
            for (int techIndex = 0; techIndex < numTechIds; techIndex++) {
                TechId techId = getTechId(techIndex);
                assert techId.idManager == this;
                assert techId.techIndex == techIndex;
                techId.check();
                assert techIdsByName.get(techId.techName) == techId;
            }
            numLibIds = libIds.size();
            assert numLibIds == libIdsByName.size();
            for (int libIndex = 0; libIndex < numLibIds; libIndex++) {
                LibId libId = getLibId(libIndex);
                assert libId.idManager == this;
                assert libId.libIndex == libIndex;
                libId.check();
                assert libIdsByName.get(libId.libName) == libId;
            }
            for (Map.Entry<String,LibId> e: libIdsByName.entrySet()) {
                LibId libId = e.getValue();
                assert libId.idManager == this;
                assert libId.libName == e.getKey();
                assert getLibId(libId.libIndex) == libId;
            }
            
            numCellIds = cellIds.size();
        }
        for (int cellIndex = 0; cellIndex < numCellIds; cellIndex++) {
            CellId cellId = getCellId(cellIndex);
            assert cellId.idManager == this;
            assert cellId.cellIndex == cellIndex;
            assert cellId.libId.getCellId(cellId.cellName) == cellId;
            cellId.check();
        }
    }
}
