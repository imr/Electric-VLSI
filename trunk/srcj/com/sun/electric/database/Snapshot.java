/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Snapshot.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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

import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.CellUsage;
import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.id.IdReader;
import com.sun.electric.database.id.IdWriter;
import com.sun.electric.database.id.LibId;
import com.sun.electric.database.id.TechId;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.ImmutableArrayList;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Tool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class Snapshot {

    public final IdManager idManager;
    public final int snapshotId;
    public final Tool tool;
    public final ImmutableArrayList<CellTree> cellTrees;
    public final ImmutableArrayList<CellBackup> cellBackups;
    public final int[] cellGroups;
    public final CellId[] groupMainSchematics;
    public final ImmutableArrayList<LibraryBackup> libBackups;
    public final Environment environment;
    public final TechPool techPool;
    final EquivalentSchematicExports[] equivSchemExports;

    /** Creates a new instance of Snapshot */
    private Snapshot(IdManager idManager, int snapshotId, Tool tool,
            ImmutableArrayList<CellTree> cellTrees,
            ImmutableArrayList<CellBackup> cellBackups,
            int[] cellGroups, CellId[] groupMainSchematics,
            ImmutableArrayList<LibraryBackup> libBackups,
            Environment environment) {
        this.idManager = idManager;
        this.snapshotId = snapshotId;
        this.tool = tool;
        this.cellTrees = cellTrees;
        this.cellBackups = cellBackups;
        this.cellGroups = cellGroups;
        this.groupMainSchematics = groupMainSchematics;
        this.libBackups = libBackups;
        this.environment = environment;
        techPool = environment.techPool;
        equivSchemExports = new EquivalentSchematicExports[cellTrees.size()];
//        if (Job.getDebug())
//            check();
    }

    /**
     * Creates empty snapshot.
     */
    public Snapshot(IdManager idManager) {
        this(idManager, 0, null, CellTree.EMPTY_LIST, CellBackup.EMPTY_LIST,
                new int[0], new CellId[0],
                LibraryBackup.EMPTY_LIST, idManager.getInitialEnvironment());
    }

    /**
     * Creates a new instance of Snapshot which differs from this Snapshot.
     * Four array parameters are supplied. Each parameter may be null if its contents is the same as in this Snapshot.
     * @param tool Tool which initiated database changes.
     * @param environment Environment of this Snapshot
     * @param cellBackupsArray array indexed by cellIndex of new CellBackups.
     * @param libBackupsArray array indexed by libIndex of LibraryBackups.
     * @return new snapshot which differs froms this Snapshot or this Snapshot.
     * @throws IllegalArgumentException on invariant violation.
     * @throws ArrayOutOfBoundsException on some invariant violations.
     */
    private Snapshot with(Tool tool, Environment environment, CellBackup[] cellBackupsArray, LibraryBackup[] libBackupsArray) {
        if (environment == null) {
            environment = this.environment;
        }
        CellTree[] cellTreesArray = null;
        if (cellBackupsArray != null) {
            cellTreesArray = computeCellTrees(new ImmutableArrayList<CellBackup>(cellBackupsArray), environment.techPool);
        }
        return with(tool, environment, cellTreesArray, libBackupsArray);
    }

    /**
     * Creates a new instance of Snapshot which differs from this Snapshot.
     * Four array parameters are supplied. Each parameter may be null if its contents is the same as in this Snapshot.
     * @param tool Tool which initiated database changes.
     * @param environment Environment of this Snapshot
     * @param cellTreesArray array indexed by cellIndex of new CellTrees.
     * @param libBackupsArray array indexed by libIndex of LibraryBackups.
     * @return new snapshot which differs froms this Snapshot or this Snapshot.
     * @throws IllegalArgumentException on invariant violation.
     * @throws ArrayOutOfBoundsException on some invariant violations.
     */
    public Snapshot with(Tool tool, Environment environment, CellTree[] cellTreesArray, LibraryBackup[] libBackupsArray) {
//        long startTime = System.currentTimeMillis();
        if (environment == null) {
            environment = this.environment;
        }
        ImmutableArrayList<CellTree> cellTrees = copyArray(cellTreesArray, this.cellTrees);
        ImmutableArrayList<LibraryBackup> libBackups = copyArray(libBackupsArray, this.libBackups);
        if (this.cellTrees == cellTrees && this.libBackups == libBackups && this.environment == environment) {
            return this;
        }
        TechPool techPool = environment.techPool;

        // Check usages in cells
        boolean namesChanged = this.libBackups != libBackups || this.cellTrees.size() != cellTrees.size();
        for (int cellIndex = 0; cellIndex < cellTrees.size(); cellIndex++) {
            CellTree newTree = cellTrees.get(cellIndex);
            CellTree oldTree = getCellTree(cellIndex);
            if (newTree == null) {
                if (oldTree != null) {
                    namesChanged = true;
                }
                continue;
            }
            CellBackup newBackup = newTree.top;
            ImmutableCell newCell = newBackup.cellRevision.d;
            if (!newBackup.techPool.isRestriction(techPool)) {
                throw new IllegalArgumentException();
            }
            // Check that sub trees match
            if (newCell.cellId.cellIndex != cellIndex) {
                throw new IllegalArgumentException();
            }
            if (newCell.cellId.idManager != idManager) {
                throw new IllegalArgumentException();
            }
            for (CellTree cellSubTree : newTree.subTrees) {
                if (cellSubTree == null) {
                    continue;
                }
                if (cellSubTree != cellTrees.get(cellSubTree.top.cellRevision.d.cellId.cellIndex)) {
                    throw new IllegalArgumentException();
                }
            }

            CellBackup oldBackup = getCell(cellIndex);
            if (oldBackup == null || newCell.groupName != oldBackup.cellRevision.d.groupName
                    || newCell.params != oldBackup.cellRevision.d.params) {
                namesChanged = true;
            }
        }
        ImmutableArrayList<CellBackup> cellBackups = this.cellBackups;
        if (this.cellTrees != cellTrees) {
            CellBackup[] cellBackupArray = new CellBackup[cellTrees.size()];
            for (int cellIndex = 0; cellIndex < cellTrees.size(); cellIndex++) {
                CellTree cellTree = cellTrees.get(cellIndex);
                if (cellTree != null) {
                    cellBackupArray[cellIndex] = cellTree.top;
                }
            }
            cellBackups = new ImmutableArrayList<CellBackup>(cellBackupArray);
        }

        // Check names
        int[] cellGroups = this.cellGroups;
        CellId[] groupMainSchematics = this.groupMainSchematics;
        if (namesChanged) {
            cellGroups = new int[cellBackups.size()];
            checkNames(idManager, cellBackups, cellGroups, libBackups);
            if (Arrays.equals(this.cellGroups, cellGroups)) {
                cellGroups = this.cellGroups;
            } else {
                int maxGroup = -1;
                for (int groupIndex : cellGroups) {
                    maxGroup = Math.max(maxGroup, groupIndex);
                }
                groupMainSchematics = new CellId[maxGroup + 1];
                for (int cellIndex = 0; cellIndex < cellBackups.size(); cellIndex++) {
                    CellBackup cellBackup = cellBackups.get(cellIndex);
                    if (cellBackup == null) {
                        continue;
                    }
                    CellId cellId = cellBackup.cellRevision.d.cellId;
                    if (!cellId.isSchematic()) {
                        continue;
                    }
                    int groupIndex = cellGroups[cellIndex];
                    CellId mainSchematics = groupMainSchematics[groupIndex];
                    if (mainSchematics == null || cellId.cellName.compareTo(mainSchematics.cellName) < 0) {
                        groupMainSchematics[groupIndex] = cellId;
                    }
                }
                if (Arrays.equals(this.groupMainSchematics, groupMainSchematics)) {
                    groupMainSchematics = this.groupMainSchematics;
                }
            }
        }

        Snapshot snapshot = new Snapshot(idManager, idManager.newSnapshotId(), tool,
                cellTrees, cellBackups, cellGroups, groupMainSchematics,
                libBackups, environment);

        // Try to reuse EquivalenSchematicExports
        for (CellTree cellTree: snapshot.cellTrees) {
            if (cellTree == null) continue;
            reuseSchemEq(snapshot, cellTree.top.cellRevision.d.cellId);
        }
//        long endTime = System.currentTimeMillis();
//        System.out.println("Creating snapshot took: " + (endTime - startTime) + " msec");
        return snapshot;
    }

    private EquivalentSchematicExports reuseSchemEq(Snapshot snapshot, CellId cellId) {
        EquivalentSchematicExports newSchemEq = snapshot.equivSchemExports[cellId.cellIndex];
        if (newSchemEq != null) return newSchemEq;
//        if (cellId.toString().equals("redFour:NMOS;1{sch}") || cellId.toString().equals("orangeST090nm:NMOSf;1{ic}"))
//            cellId = cellId;
        CellTree oldCellTree = getCellTree(cellId);
        CellTree newCellTree = snapshot.getCellTree(cellId);
        if (newCellTree != oldCellTree) return null;
        newSchemEq = equivSchemExports[cellId.cellIndex];
        if (newSchemEq == null) return null;
        assert cellId.isIcon() || cellId.isSchematic();
        CellId mainSchemId = snapshot.groupMainSchematics[snapshot.cellGroups[cellId.cellIndex]];
        if (mainSchemId != groupMainSchematics[cellGroups[cellId.cellIndex]]) return null;
        if (mainSchemId != cellId && mainSchemId != null) {
            EquivalentSchematicExports oldMainSchemEq = equivSchemExports[mainSchemId.cellIndex];
            assert oldMainSchemEq != null;
            EquivalentSchematicExports newMainSchemEq = reuseSchemEq(snapshot, mainSchemId);
            if (newMainSchemEq == null)
                newMainSchemEq = snapshot.getEquivExports(mainSchemId);
            if (newMainSchemEq != oldMainSchemEq)
                newSchemEq = null;
        }
        for (CellTree subTree: newCellTree.subTrees) {
            if (subTree == null) continue;
            CellId subCellId = subTree.top.cellRevision.d.cellId;
            if (subCellId.isIcon()) {
                if (cellId.isSchematic() && snapshot.cellGroups[cellId.cellIndex] == snapshot.cellGroups[subCellId.cellIndex]) {
                    // Icon of parent
                    continue;
                }
            } else if (!subCellId.isSchematic()) {
                continue;
            }
            EquivalentSchematicExports oldSubSchemEq = equivSchemExports[subCellId.cellIndex];
            assert oldSubSchemEq != null;
            EquivalentSchematicExports newSubSchemEq = reuseSchemEq(snapshot, subCellId);
            if (newSubSchemEq == null)
                newSubSchemEq = snapshot.getEquivExports(subCellId);
            if (!newSubSchemEq.equals(oldSubSchemEq)) {
                newSchemEq = null;
            }
        }
        if (newSchemEq != null)
            snapshot.equivSchemExports[cellId.cellIndex] = newSchemEq;
        return newSchemEq;
    }

    public boolean sameNetlist(Snapshot that, CellId cellId) {
        assert cellId.isIcon() || cellId.isSchematic();
        CellTree thisTree = this.getCellTree(cellId);
        CellTree thatTree = that.getCellTree(cellId);
        if (thisTree != thatTree) {
            return false;
        }
        CellId mainSchemId = that.groupMainSchematics[that.cellGroups[cellId.cellIndex]];
        if (mainSchemId != groupMainSchematics[cellGroups[cellId.cellIndex]]) {
            return false;
        }
        if (mainSchemId != cellId && mainSchemId != null) {
            EquivalentSchematicExports thisMainSchemEq = this.getEquivExports(mainSchemId);
            EquivalentSchematicExports thatMainSchemEq = that.getEquivExports(mainSchemId);
            if (!this.getEquivExports(mainSchemId).equals(that.getEquivExports(mainSchemId)))
                return false;
        }
        for (CellTree subTree: thatTree.subTrees) {
            if (subTree == null) continue;
            CellId subCellId = subTree.top.cellRevision.d.cellId;
            if (subCellId.isIcon()) {
                if (cellId.isSchematic() && cellGroups[cellId.cellIndex] == cellGroups[subCellId.cellIndex]) {
                    // Icon of parent
                    continue;
                }
            } else if (!subCellId.isSchematic()) {
                continue;
            }
            if (!this.getEquivExports(subCellId).equals(that.getEquivExports(subCellId))) {
                return false;
            }
        }
        return true;
     }

    public Snapshot with(Tool tool, Environment environment) {
        TechPool techPool = environment.techPool;
        CellBackup[] cellBackupArray = new CellBackup[cellBackups.size()];
        for (CellBackup cellBackup : cellBackups) {
            if (cellBackup == null) {
                continue;
            }
            cellBackupArray[cellBackup.cellRevision.d.cellId.cellIndex] = cellBackup.withTechPool(techPool);
        }
        return with(tool, environment, cellBackupArray, null);
    }

    private static <T> ImmutableArrayList<T> copyArray(T[] newArray, ImmutableArrayList<T> oldList) {
        if (newArray == null) {
            return oldList;
        }
        int l;
        for (l = newArray.length; l > 0 && newArray[l - 1] == null; l--);
        if (l == oldList.size()) {
            int i = 0;
            while (i < oldList.size() && newArray[i] == oldList.get(i)) {
                i++;
            }
            if (i == l) {
                return oldList;
            }
        }
        return new ImmutableArrayList<T>(newArray, 0, l);
    }

    private CellTree[] computeCellTrees(ImmutableArrayList<CellBackup> cellBackups, TechPool techPool) {
        CellTree[] result = new CellTree[cellBackups.size()];
        for (int cellIndex = 0; cellIndex < result.length; cellIndex++) {
            CellBackup cellBackup = cellBackups.get(cellIndex);
            if (cellBackup == null) {
                continue;
            }
            computeCellTrees(cellBackup.cellRevision.d.cellId, cellBackups, techPool, result);
        }
        return result;
    }

    private CellTree computeCellTrees(CellId topCellId, ImmutableArrayList<CellBackup> cellBackups, TechPool techPool, CellTree[] cellTrees) {
        int cellIndex = topCellId.cellIndex;
        CellTree cellTree = cellTrees[cellIndex];
        if (cellTree == null) {
            CellBackup top = cellBackups.get(cellIndex);
            int[] instCounts = top.cellRevision.getInstCounts();
            CellTree[] subTrees = new CellTree[instCounts.length];
            for (int i = 0; i < subTrees.length; i++) {
                if (instCounts[i] == 0) {
                    continue;
                }
                subTrees[i] = computeCellTrees(topCellId.getUsageIn(i).protoId, cellBackups, techPool, cellTrees);
            }
            cellTree = getCellTree(topCellId);
            if (cellTree == null) {
                cellTree = CellTree.newInstance(top.cellRevision.d, techPool);
            }
            cellTree = cellTree.with(top, subTrees, techPool);
            cellTrees[cellIndex] = cellTree;
        }
        return cellTree;
    }

    /**
     * Returns Snapshot which differs from this Snapshot by renamed Ids.
     * @param idMapper a map from old Ids to new Ids.
     * @return Snapshot with renamed Ids.
     */
    public Snapshot withRenamedIds(IdMapper idMapper, CellId fromGroup, String toGroup) {
        int maxCellIndex = -1;
        for (CellBackup cellBackup : cellBackups) {
            if (cellBackup == null) {
                continue;
            }
            maxCellIndex = Math.max(maxCellIndex, idMapper.get(cellBackup.cellRevision.d.cellId).cellIndex);
        }
        int maxLibIndex = -1;
        for (LibraryBackup libBackup : libBackups) {
            if (libBackup == null) {
                continue;
            }
            maxLibIndex = Math.max(maxLibIndex, idMapper.get(libBackup.d.libId).libIndex);
        }

        CellBackup[] cellBackupsArray = new CellBackup[maxCellIndex + 1];
        LibraryBackup[] libBackupsArray = new LibraryBackup[maxLibIndex + 1];

        BitSet cellBackupsChangedInLib = new BitSet();
        BitSet fromGroupCellIds = new BitSet();
        BitSet toGroupCellIds = new BitSet();
        CellName fromGroupName = null;
        CellName toGroupName = null;
        if (fromGroup != null) {
            assert getCell(fromGroup) != null;
            CellId toGroupCellId = idMapper.get(fromGroup);
            assert toGroupCellId != null;
            assert getCell(toGroupCellId) == null;
            int fromGroupIndex = cellGroups[fromGroup.cellIndex];
            assert fromGroupIndex >= 0;
            int toGroupIndex1 = -1, toGroupIndex2 = -1;
            if (toGroup == null) {
                toGroupIndex2 = fromGroupIndex;
            }
            for (int cellIndex = 0; cellIndex < cellBackups.size(); cellIndex++) {
                CellBackup cellBackup = cellBackups.get(cellIndex);
                if (cellBackup == null) {
                    continue;
                }
                CellId cellId = cellBackup.cellRevision.d.cellId;
                if (cellId.libId == fromGroup.libId) {
                    if (cellId.cellName.getName().equals(toGroupCellId.cellName.getName())) {
                        toGroupIndex1 = cellGroups[cellIndex];
                    }
                    if (toGroup != null && cellId.cellName.getName().equals(toGroup)) {
                        toGroupIndex2 = cellGroups[cellIndex];
                    }
                }
            }
            ArrayList<CellName> fromCellNames = new ArrayList<CellName>();
            ArrayList<CellName> toCellNames = new ArrayList<CellName>();
            toGroupCellIds.set(toGroupCellId.cellIndex);
            toCellNames.add(toGroupCellId.cellName);
            for (int cellIndex = 0; cellIndex < cellBackups.size(); cellIndex++) {
                CellBackup cellBackup = cellBackups.get(cellIndex);
                if (cellBackup == null || cellIndex == fromGroup.cellIndex) {
                    continue;
                }
                CellId cellId = cellBackup.cellRevision.d.cellId;
                if (cellGroups[cellIndex] == fromGroupIndex) {
                    fromGroupCellIds.set(cellIndex);
                    fromCellNames.add(cellId.cellName);
                }
                if (cellGroups[cellIndex] == toGroupIndex1 || cellGroups[cellIndex] == toGroupIndex2) {
                    toGroupCellIds.set(cellIndex);
                    toCellNames.add(cellId.cellName);
                }
            }
            if (!fromCellNames.isEmpty()) {
                fromGroupName = makeCellGroupName(fromCellNames);
            }
            if (!toCellNames.isEmpty()) {
                toGroupName = makeCellGroupName(toCellNames);
            }
        }
        for (int cellIndex = 0; cellIndex < cellBackups.size(); cellIndex++) {
            CellBackup oldCellBackup = cellBackups.get(cellIndex);
            if (oldCellBackup == null) {
                continue;
            }
            CellName newGroupName = oldCellBackup.cellRevision.d.groupName;
            if (fromGroup != null) {
                if (toGroupCellIds.get(cellIndex) || cellIndex == fromGroup.cellIndex) {
                    newGroupName = toGroupName;
                } else if (fromGroupCellIds.get(cellIndex)) {
                    newGroupName = fromGroupName;
                }
            }
            CellBackup newCellBackup = oldCellBackup.withRenamedIds(idMapper, newGroupName);
            if (newCellBackup != oldCellBackup) {
                cellBackupsChangedInLib.set(newCellBackup.cellRevision.d.cellId.libId.libIndex);
            }
            int newCellIndex = newCellBackup.cellRevision.d.cellId.cellIndex;
            cellBackupsArray[newCellIndex] = newCellBackup;
        }
        if (cellBackupsChangedInLib.isEmpty()) {
            cellBackupsArray = null;
        }

        boolean libBackupsChanged = false;
        for (int libIndex = 0; libIndex < libBackups.size(); libIndex++) {
            LibraryBackup oldLibBackup = libBackups.get(libIndex);
            if (oldLibBackup == null) {
                continue;
            }
            LibraryBackup newLibBackup = oldLibBackup.withRenamedIds(idMapper);
            if (cellBackupsChangedInLib.get(libIndex)) {
                newLibBackup = newLibBackup.withModified();
            }
            if (newLibBackup != oldLibBackup) {
                libBackupsChanged = true;
            }
            libBackupsArray[newLibBackup.d.libId.libIndex] = newLibBackup;
        }
        if (!libBackupsChanged) {
            libBackupsArray = null;
        }

        if (cellBackupsArray == null && libBackups == null) {
            return this;
        }
        return with(tool, null, cellBackupsArray, libBackupsArray);
    }

    public List<LibId> getChangedLibraries(Snapshot oldSnapshot) {
        if (oldSnapshot == null) {
            oldSnapshot = idManager.getInitialSnapshot();
        }
        if (idManager != oldSnapshot.idManager) {
            throw new IllegalArgumentException();
        }
        List<LibId> changed = null;
        if (oldSnapshot.libBackups != libBackups) {
            int numLibs = Math.max(oldSnapshot.libBackups.size(), libBackups.size());
            for (int i = 0; i < numLibs; i++) {
                LibraryBackup oldBackup = oldSnapshot.getLib(i);
                LibraryBackup newBackup = getLib(i);
                if (oldBackup == newBackup) {
                    continue;
                }
                if (changed == null) {
                    changed = new ArrayList<LibId>();
                }
                changed.add(idManager.getLibId(i));
            }
        }
        if (changed == null) {
            changed = Collections.emptyList();
        }
        return changed;
    }

    public List<CellId> getChangedCells(Snapshot oldSnapshot) {
        if (oldSnapshot == null) {
            oldSnapshot = idManager.getInitialSnapshot();
        }
        List<CellId> changed = null;
        int numCells = Math.max(oldSnapshot.cellBackups.size(), cellBackups.size());
        for (int i = 0; i < numCells; i++) {
            CellBackup oldBackup = oldSnapshot.getCell(i);
            CellBackup newBackup = getCell(i);
            if (oldBackup == newBackup) {
                continue;
            }
            if (changed == null) {
                changed = new ArrayList<CellId>();
            }
            changed.add(idManager.getCellId(i));
        }
        if (changed == null) {
            changed = Collections.emptyList();
        }
        return changed;
    }

    public Collection<CellId> getCellsDownTop() {
        LinkedHashSet<CellId> order = new LinkedHashSet<CellId>();
        for (CellBackup cellBackup : cellBackups) {
            if (cellBackup == null) {
                continue;
            }
            getCellsDownTop(cellBackup.cellRevision.d.cellId, order);
        }
        return order;
    }

    private void getCellsDownTop(CellId root, LinkedHashSet<CellId> order) {
        if (order.contains(root)) {
            return;
        }
        CellBackup cellBackup = getCell(root);
        CellRevision cellRevision = cellBackup.cellRevision;
        for (int i = 0; i < cellRevision.cellUsages.length; i++) {
            if (cellRevision.cellUsages[i] == null) {
                continue;
            }
            CellUsage cu = root.getUsageIn(i);
            getCellsDownTop(cu.protoId, order);
        }
        boolean added = order.add(root);
        assert added;
    }

    public CellTree getCellTree(CellId cellId) {
        if (cellId.getIdManager() != idManager) {
            throw new IllegalArgumentException();
        }
        return getCellTree(cellId.cellIndex);
    }

    public CellBackup getCell(CellId cellId) {
        if (cellId.getIdManager() != idManager) {
            throw new IllegalArgumentException();
        }
        return getCell(cellId.cellIndex);
    }

    public CellRevision getCellRevision(CellId cellId) {
        CellBackup cellBackup = getCell(cellId);
        return cellBackup != null ? cellBackup.cellRevision : null;
    }

    public CellTree getCellTree(int cellIndex) {
        return cellIndex < cellTrees.size() ? cellTrees.get(cellIndex) : null;
    }

    public CellBackup getCell(int cellIndex) {
        return cellIndex < cellBackups.size() ? cellBackups.get(cellIndex) : null;
    }

    public CellRevision getCellRevision(int cellIndex) {
        CellBackup cellBackup = getCell(cellIndex);
        return cellBackup != null ? cellBackup.cellRevision : null;
    }

    /**
     * Returns group name of group with specified CellNames.
     * Name of cell group is a base name of main schematics cell if any.
     * Otherwise it is a shortest base name among cells in the group.
     * @param cellNames collection of CellNames in a group.
     * @return name of cell group.
     * @throws InvalidArgumentException if cellNames is empty
     */
    public static CellName makeCellGroupName(Collection<CellName> cellNames) {
        if (cellNames.isEmpty()) {
            throw new IllegalArgumentException();
        }
        String groupName = null;
        for (CellName cellName : cellNames) {
            if (!cellName.isSchematic()) {
                continue;
            }
            String name = cellName.getName();
            if (groupName == null || TextUtils.STRING_NUMBER_ORDER.compare(name, groupName) < 0) {
                groupName = name;
            }
        }
        if (groupName == null) {
            for (CellName cellName : cellNames) {
                String name = cellName.getName();
                if (groupName == null || name.length() < groupName.length()
                        || name.length() == groupName.length() && TextUtils.STRING_NUMBER_ORDER.compare(name, groupName) < 0) {
                    groupName = name;
                }
            }
        }
        assert groupName != null;
        return CellName.parseName(groupName + "{sch}");
    }

    public EquivalentSchematicExports getEquivExports(CellId top) {
        EquivalentSchematicExports eq = equivSchemExports[top.cellIndex];
        if (eq == null) {
            ImmutableNetSchem netSchem = new ImmutableNetSchem(this, top);
            eq = new EquivalentSchematicExports(netSchem);
            equivSchemExports[top.cellIndex] = eq;
        }
        return eq;
    }

    public ERectangle getCellBounds(CellId cellId) {
        return getCellBounds(cellId.cellIndex);
    }

    public ERectangle getCellBounds(int cellIndex) {
        CellTree cellTree = getCellTree(cellIndex);
        return cellTree != null ? cellTree.getBounds() : null;
    }

    /** Returns TechPool of this Snapshot */
    public TechPool getTechPool() {
        return techPool;
    }

    /**
     * Get Technology by TechId
     * TechId must belong to same IdManager as TechPool
     * @param techId TechId to find
     * @return Technology b giben TechId or null
     * @throws IllegalArgumentException of TechId is not from this IdManager
     */
    public Technology getTech(TechId techId) {
        return techPool.getTech(techId);
    }

    /** Returns Artwork technology in this database */
    public Artwork getArtwork() {
        return techPool.getArtwork();
    }

    /** Returns Generic technology in this database */
    public Generic getGeneric() {
        return techPool.getGeneric();
    }

    /** Returns Schematic technology in this database */
    public Schematics getSchematics() {
        return techPool.getSchematics();
    }

//    private Technology getTech(int techIndex) {
//        return techIndex < technologies.size() ? technologies.get(techIndex) : null;
//    }
    /** Returns map from Setting to its value in this Snapshot */
    public Map<Setting, Object> getSettings() {
        return environment.getSettings();
    }

    public LibraryBackup getLib(LibId libId) {
        return getLib(libId.libIndex);
    }

    private LibraryBackup getLib(int libIndex) {
        return libIndex < libBackups.size() ? libBackups.get(libIndex) : null;
    }

//    private boolean equals(Snapshot that) {
//        return this.cellBackups.equals(that.cellBackups) &&
//                this.libBackups.equals(that.libBackups) &&
//                Arrays.equals(this.cellGroups, that.cellGroups) &&
//                this.cellBounds.equals(that.cellBounds);
//    }
    public void writeDiffs(IdWriter writer, Snapshot oldSnapshot) throws IOException {
        writer.writeDiffs();
        writer.writeInt(snapshotId);

        writer.writeBoolean(tool != null);
        if (tool != null) {
            writer.writeTool(tool);
        }

        environment.writeDiff(writer, oldSnapshot.environment);

        boolean libsChanged = oldSnapshot.libBackups != libBackups;
        writer.writeBoolean(libsChanged);
        if (libsChanged) {
            writer.writeInt(libBackups.size());
            for (int i = 0; i < libBackups.size(); i++) {
                LibraryBackup oldBackup = oldSnapshot.getLib(i);
                LibraryBackup newBackup = getLib(i);
                if (oldBackup == newBackup) {
                    continue;
                }
                if (oldBackup == null) {
                    writer.writeInt(i);
                    newBackup.write(writer);
                } else if (newBackup == null) {
                    writer.writeInt(~i);
                } else {
                    writer.writeInt(i);
                    newBackup.write(writer);
                }
            }
            writer.writeInt(Integer.MAX_VALUE);
        }

        writer.writeInt(cellBackups.size());
        for (int i = 0; i < cellBackups.size(); i++) {
            CellBackup oldBackup = oldSnapshot.getCell(i);
            CellBackup newBackup = getCell(i);
            if (oldBackup == newBackup) {
                continue;
            }
            if (oldBackup == null) {
                writer.writeInt(i);
                newBackup.write(writer);
            } else if (newBackup == null) {
                writer.writeInt(~i);
            } else {
                writer.writeInt(i);
                newBackup.write(writer);
            }
        }
        writer.writeInt(Integer.MAX_VALUE);

        boolean cellGroupsChanged = cellGroups != oldSnapshot.cellGroups;
        writer.writeBoolean(cellGroupsChanged);
        if (cellGroupsChanged) {
            assert cellGroups.length == cellBackups.size();
            for (int cellIndex = 0; cellIndex < cellGroups.length; cellIndex++) {
                writer.writeInt(cellGroups[cellIndex]);
            }
            for (int groupIndex = 0; groupIndex < groupMainSchematics.length; groupIndex++) {
                CellId mainSchematics = groupMainSchematics[groupIndex];
                writer.writeInt(mainSchematics != null ? mainSchematics.cellIndex : -1);
            }
        }
    }

    public static Snapshot readSnapshot(IdReader reader, Snapshot oldSnapshot) throws IOException {
        assert reader.idManager == oldSnapshot.idManager;
        reader.readDiffs();
        int snapshotId = reader.readInt();
        boolean hasTool = reader.readBoolean();
        Tool tool = hasTool ? reader.readTool() : null;

        Environment environment = Environment.readEnvironment(reader, oldSnapshot.environment);
        TechPool techPool = environment.techPool;
        boolean technologiesChanged = techPool != oldSnapshot.techPool;

        ImmutableArrayList<LibraryBackup> libBackups = oldSnapshot.libBackups;
        boolean libsChanged = reader.readBoolean();
        if (libsChanged) {
            int libLen = reader.readInt();
            LibraryBackup[] libBackupsArray = new LibraryBackup[libLen];
            for (int libIndex = 0, numLibs = Math.min(oldSnapshot.libBackups.size(), libLen); libIndex < numLibs; libIndex++) {
                libBackupsArray[libIndex] = oldSnapshot.libBackups.get(libIndex);
            }
            for (;;) {
                int libIndex = reader.readInt();
                if (libIndex == Integer.MAX_VALUE) {
                    break;
                }
                if (libIndex >= 0) {
                    LibraryBackup newBackup = LibraryBackup.read(reader);
                    libBackupsArray[libIndex] = newBackup;
                } else {
                    libIndex = ~libIndex;
                    assert libBackupsArray[libIndex] != null;
                    libBackupsArray[libIndex] = null;
                }
            }
            libBackups = new ImmutableArrayList<LibraryBackup>(libBackupsArray);
        }

        int cellLen = reader.readInt();
        int cellMax = Math.min(oldSnapshot.cellBackups.size(), cellLen);
        CellBackup[] cellBackupsArray = new CellBackup[cellLen];
        for (int cellIndex = 0; cellIndex < cellMax; cellIndex++) {
            cellBackupsArray[cellIndex] = oldSnapshot.cellBackups.get(cellIndex);
        }
        if (technologiesChanged) {
            for (int cellIndex = 0; cellIndex < cellLen; cellIndex++) {
                CellBackup cellBackup = cellBackupsArray[cellIndex];
                if (cellBackup == null) {
                    continue;
                }
                cellBackupsArray[cellIndex] = cellBackup.withTechPool(techPool);
            }
        }
        for (;;) {
            int cellIndex = reader.readInt();
            if (cellIndex == Integer.MAX_VALUE) {
                break;
            }
            if (cellIndex >= 0) {
                CellBackup newBackup = CellBackup.read(reader, techPool);
                cellBackupsArray[cellIndex] = newBackup;
            } else {
                cellIndex = ~cellIndex;
                assert cellBackupsArray[cellIndex] != null;
                cellBackupsArray[cellIndex] = null;
            }
        }
        ImmutableArrayList<CellBackup> cellBackups = new ImmutableArrayList<CellBackup>(cellBackupsArray);

        int[] cellGroups = oldSnapshot.cellGroups;
        CellId[] groupMainSchematics = oldSnapshot.groupMainSchematics;
        boolean cellGroupsChanged = reader.readBoolean();
        if (cellGroupsChanged) {
            cellGroups = new int[cellBackups.size()];
            int maxGroup = -1;
            for (int cellIndex = 0; cellIndex < cellGroups.length; cellIndex++) {
                int groupIndex = reader.readInt();
                maxGroup = Math.max(maxGroup, groupIndex);
                cellGroups[cellIndex] = groupIndex;
            }
            groupMainSchematics = new CellId[maxGroup + 1];
            for (int groupIndex = 0; groupIndex < groupMainSchematics.length; groupIndex++) {
                CellId mainSchematics = null;
                int cellIndex = reader.readInt();
                if (cellIndex >= 0) {
                    mainSchematics = reader.idManager.getCellId(cellIndex);
                }
                groupMainSchematics[groupIndex] = mainSchematics;
            }
        }
        for (int i = 0; i < cellBackups.size(); i++) {
            assert (cellBackups.get(i) != null) == (cellGroups[i] >= 0);
        }
        CellTree[] cellTreesArray = oldSnapshot.computeCellTrees(cellBackups, environment.techPool);
        ImmutableArrayList<CellTree> cellTrees = new ImmutableArrayList<CellTree>(cellTreesArray);
        return new Snapshot(oldSnapshot.idManager, snapshotId, tool, cellTrees, cellBackups,
                cellGroups, groupMainSchematics,
                libBackups, environment);
    }

    /**
     * Checks invariant of this Snapshot.
     * @throws IllegalArgumentException on invariant violation.
     * @throws ArrayOutOfBoundsException on some invariant violations.
     * @throws AssertionError if invariant is broken.
     * @throws AssertionError if invariant is broken.
     */
    public void check() {
//        long startTime = System.currentTimeMillis();
        techPool.check();
        environment.check();
        assert environment.techPool == techPool;
        assert techPool.idManager == idManager;
        for (LibraryBackup libBackup : libBackups) {
            if (libBackup == null) {
                continue;
            }
            libBackup.check();
        }
        assert cellTrees.size() == cellBackups.size();
        for (int cellIndex = 0; cellIndex < cellTrees.size(); cellIndex++) {
            CellTree cellTree = cellTrees.get(cellIndex);
            CellBackup cellBackup = cellBackups.get(cellIndex);
            if (cellTree == null) {
                assert cellBackup == null;
                continue;
            }
            assert cellBackup == cellTree.top;
            for (CellTree subCellTree : cellTree.subTrees) {
                if (subCellTree == null) {
                    continue;
                }
                CellId subCellId = subCellTree.top.cellRevision.d.cellId;
                assert subCellTree == cellTrees.get(subCellId.cellIndex);
            }
            cellTree.check();
            assert cellTree.techPool.isRestriction(techPool);
        }
        if (libBackups.size() > 0) {
            assert libBackups.get(libBackups.size() - 1) != null;
        }
        if (cellTrees.size() > 0) {
            assert cellTrees.get(cellTrees.size() - 1) != null;
        }
        checkRecursion(cellBackups);
        int[] cellGroups = new int[cellBackups.size()];
        checkNames(idManager, cellBackups, cellGroups, libBackups);
        assert Arrays.equals(this.cellGroups, cellGroups);
        int maxGroup = -1;
        for (int groupIndex : cellGroups) {
            maxGroup = Math.max(maxGroup, groupIndex);
        }
        assert groupMainSchematics.length == maxGroup + 1;
        BitSet foundMainSchematics = new BitSet();
        for (CellBackup cellBackup : cellBackups) {
            if (cellBackup == null) {
                continue;
            }
            CellId cellId = cellBackup.cellRevision.d.cellId;
            if (!cellId.isSchematic()) {
                continue;
            }
            int groupIndex = cellGroups[cellId.cellIndex];
            int cmp = cellId.cellName.compareTo(groupMainSchematics[groupIndex].cellName);
            assert cmp >= 0;
            if (cmp == 0) {
                assert groupMainSchematics[groupIndex] == cellId;
                foundMainSchematics.set(groupIndex);
            }
        }
        for (int groupIndex = 0; groupIndex < groupMainSchematics.length; groupIndex++) {
            assert foundMainSchematics.get(groupIndex) == (groupMainSchematics[groupIndex] != null);
        }
//        long endTime = System.currentTimeMillis();
//        System.out.println("Checking snapshot invariants took: " + (endTime - startTime) + " msec");
    }

    /*
     * Checks name consistency of arrays intended for Sbapshot construction.
     * @throws IllegalArgumentException on invariant violation.
     * @throws ArrayOutOfBoundsException on some invariant violations.
     */
    private static void checkNames(IdManager idManager, ImmutableArrayList<CellBackup> cellBackups, int[] cellGroups, ImmutableArrayList<LibraryBackup> libBackups) {
        HashSet<String> libNames = new HashSet<String>();
        ArrayList<HashMap<String, CellName>> protoNameToGroupName = new ArrayList<HashMap<String, CellName>>();
        ArrayList<HashSet<CellName>> cellNames = new ArrayList<HashSet<CellName>>();
        ArrayList<HashMap<CellName, Integer>> groupNameToGroupIndex = new ArrayList<HashMap<CellName, Integer>>();
        for (int libIndex = 0; libIndex < libBackups.size(); libIndex++) {
            LibraryBackup libBackup = libBackups.get(libIndex);
            if (libBackup == null) {
                protoNameToGroupName.add(null);
                cellNames.add(null);
                groupNameToGroupIndex.add(null);
                continue;
            }
            protoNameToGroupName.add(new HashMap<String, CellName>());
            cellNames.add(new HashSet<CellName>());
            groupNameToGroupIndex.add(new HashMap<CellName, Integer>());
            if (libBackup.d.libId != idManager.getLibId(libIndex)) {
                throw new IllegalArgumentException("LibId");
            }
            String libName = libBackup.d.libId.libName;
            if (!libNames.add(libName)) {
                throw new IllegalArgumentException("duplicate libName");
            }
            for (LibId libId : libBackup.referencedLibs) {
                if (libId != libBackups.get(libId.libIndex).d.libId) {
                    throw new IllegalArgumentException("LibId in referencedLibs");
                }
            }
        }
        assert protoNameToGroupName.size() == libBackups.size() && cellNames.size() == libBackups.size() && groupNameToGroupIndex.size() == libBackups.size();

        assert cellBackups.size() == cellGroups.length;
        Arrays.fill(cellGroups, -1);
        ArrayList<ImmutableCell> groupParamOwners = new ArrayList<ImmutableCell>();
        for (int cellIndex = 0; cellIndex < cellBackups.size(); cellIndex++) {
            CellBackup cellBackup = cellBackups.get(cellIndex);
            if (cellBackup == null) {
                continue;
            }
            ImmutableCell d = cellBackup.cellRevision.d;
            CellId cellId = d.cellId;
            if (cellId != idManager.getCellId(cellIndex)) {
                throw new IllegalArgumentException("CellId");
            }
            LibId libId = d.getLibId();
            int libIndex = libId.libIndex;
            if (libId != libBackups.get(libIndex).d.libId) {
                throw new IllegalArgumentException("LibId in ImmutableCell");
            }
            HashMap<String, CellName> cellNameToGroupNameInLibrary = protoNameToGroupName.get(libId.libIndex);
            HashSet<CellName> cellNamesInLibrary = cellNames.get(libId.libIndex);
            HashMap<CellName, Integer> groupNameToGroupIndexInLibrary = groupNameToGroupIndex.get(libId.libIndex);
            String protoName = cellId.cellName.getName();
            CellName groupName = cellNameToGroupNameInLibrary.get(protoName);
            if (groupName == null) {
                groupName = d.groupName;
                cellNameToGroupNameInLibrary.put(protoName, groupName);
            } else if (!d.groupName.equals(groupName)) {
                throw new IllegalArgumentException("cells with same proto name in different groups");
            }
            Integer gn = groupNameToGroupIndexInLibrary.get(groupName);
            if (gn == null) {
                gn = Integer.valueOf(groupParamOwners.size());
                groupParamOwners.add(null);
                groupNameToGroupIndexInLibrary.put(groupName, gn);
            }
            cellGroups[cellIndex] = gn.intValue();
            if (!cellNamesInLibrary.add(cellId.cellName)) {
                throw new IllegalArgumentException("duplicate CellName in library");
            }
            if (d.paramsAllowed()) {
                ImmutableCell paramOwner = groupParamOwners.get(gn.intValue());
                if (paramOwner != null) {
                    d.checkSimilarParams(paramOwner);
                } else {
                    groupParamOwners.set(gn.intValue(), paramOwner);
                }
            }
        }
    }

    private static void checkRecursion(ImmutableArrayList<CellBackup> cellBackups) {
        BitSet visited = new BitSet();
        BitSet checked = new BitSet();
        for (CellBackup cellBackup : cellBackups) {
            if (cellBackup == null) {
                continue;
            }
            checkRecursion(cellBackup.cellRevision.d.cellId, cellBackups, visited, checked);
        }
        assert visited.equals(checked);
    }

    private static void checkRecursion(CellId cellId, ImmutableArrayList<CellBackup> cellBackups, BitSet visited, BitSet checked) {
        int cellIndex = cellId.cellIndex;
        if (checked.get(cellIndex)) {
            return;
        }
        assert !visited.get(cellIndex);
        visited.set(cellIndex);
        CellBackup cellBackup = cellBackups.get(cellIndex);
        CellRevision cellRevision = cellBackup.cellRevision;
        for (int i = 0; i < cellRevision.cellUsages.length; i++) {
            CellRevision.CellUsageInfo cui = cellRevision.cellUsages[i];
            if (cui == null) {
                continue;
            }
            CellUsage u = cellId.getUsageIn(i);
            int subCellIndex = u.protoId.cellIndex;
            if (checked.get(subCellIndex)) {
                continue;
            }
            if (visited.get(subCellIndex)) {
                throw new IllegalArgumentException("Recursive instance of " + u.protoId + " in " + u.parentId);
            }
            checkRecursion(u.protoId, cellBackups, visited, checked);
        }
        checked.set(cellIndex);
    }
}
