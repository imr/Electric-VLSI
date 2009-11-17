/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: IdManager.java
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

import com.sun.electric.database.Environment;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.text.CellName;
import com.sun.electric.technology.TechPool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class owns a set of LibIds and CellIds.
 */
public class IdManager {

    /** Standard IdManager */
    public static final IdManager stdIdManager = new IdManager();
    /** List of TechIds created so far. */
    final ArrayList<TechId> techIds = new ArrayList<TechId>();
    /** HashMap of TechIds by their tech name. */
    private final HashMap<String, TechId> techIdsByName = new HashMap<String, TechId>();
    /** List of LibIds created so far. */
    final ArrayList<LibId> libIds = new ArrayList<LibId>();
    /** HashMap of LibIds by their lib name. */
    private final HashMap<String, LibId> libIdsByName = new HashMap<String, LibId>();
    /** List of CellIds created so far. */
    final ArrayList<CellId> cellIds = new ArrayList<CellId>();
    /** Count of Snapshots created with this IdManager. */
    private final AtomicInteger snapshotCount = new AtomicInteger();
    /** Initial TechPool. */
    private final TechPool initialTechPool = new TechPool(this);
    /** Initial Environment. */
    private final Environment initialEnvironment = new Environment(this);
    /** Initial Snapshot. */
    private final Snapshot initialSnapshot = new Snapshot(this);
    volatile boolean readOnly;

    /** Creates a new instance of IdManager */
    public IdManager() {
    }

    /** Disallow creation of ids (except IdReader */
    public void setReadOnly() {
        readOnly = true;
    }

    /**
     * Returns TechId with specified techName.
     * @param techName technology name.
     * @return TechId with specified techName.
     */
    public synchronized TechId newTechId(String techName) {
        TechId techId = techIdsByName.get(techName);
        if (techId != null) {
            return techId;
        }
        assert !readOnly;
        return newTechIdInternal(techName);
    }

    /**
     * Returns TechId by given index.
     * @param techIndex given index.
     * @return TechId with given index.
     */
    public synchronized TechId getTechId(int techIndex) {
        return techIds.get(techIndex);
    }

    TechId newTechIdInternal(String techName) {
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
        assert !readOnly;
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

    LibId newLibIdInternal(String libName) {
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
        assert !readOnly;
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

    CellId newCellIdInternal(LibId libId, CellName cellName) {
        int cellIndex = cellIds.size();
        CellId cellId = new CellId(libId, cellName, cellIndex);
        cellIds.add(cellId);
        libId.putCellId(cellId);
        return cellId;
    }

    public TechPool getInitialTechPool() {
        return initialTechPool;
    }

    public Environment getInitialEnvironment() {
        return initialEnvironment;
    }

    public Snapshot getInitialSnapshot() {
        return initialSnapshot;
    }

    public int newSnapshotId() {
        return snapshotCount.incrementAndGet();
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
            for (Map.Entry<String, LibId> e : libIdsByName.entrySet()) {
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

    public void dump() {
        System.out.println(techIds.size() + " TechIds:");
        for (TechId techId : new TreeMap<String, TechId>(techIdsByName).values()) {
            System.out.println("TechId " + techId + " " + techId.primitiveNodeIds.size() + " " + techId.arcProtoIds.size());
        }
        System.out.println(libIds.size() + " LibIds:");
        for (LibId libId : new TreeMap<String, LibId>(libIdsByName).values()) {
            System.out.println("LibId " + libId);
            for (CellId cellId : cellIds) {
                if (cellId.libId != libId) {
                    continue;
                }
                System.out.println(cellId + " " + cellId.numUsagesIn() + " " + cellId.numUsagesOf() + " " + cellId.numExportIds());
            }
        }
    }
}
