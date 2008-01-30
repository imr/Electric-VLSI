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
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.id.CellUsage;
import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.id.IdReader;
import com.sun.electric.database.id.IdWriter;
import com.sun.electric.database.id.LibId;
import com.sun.electric.database.id.TechId;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.ImmutableArrayList;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
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

/**
 *
 */
public class Snapshot {
    public final IdManager idManager;
    public final int snapshotId;
    public final Tool tool;
    public final ImmutableArrayList<CellBackup> cellBackups;
    public final int[] cellGroups;
    public final CellId[] groupMainSchematics;
    public final ImmutableArrayList<LibraryBackup> libBackups;
    public final TechPool techPool;
    private final ERectangle[] cellBounds;

    /** Creates a new instance of Snapshot */
    private Snapshot(IdManager idManager, int snapshotId, Tool tool,
            ImmutableArrayList<CellBackup> cellBackups,
            int[] cellGroups, CellId[] groupMainSchematics,
            ImmutableArrayList<LibraryBackup> libBackups,
            TechPool techPool,
            ERectangle[] cellBounds) {
        this.idManager = idManager;
        this.snapshotId = snapshotId;
        this.tool = tool;
        this.cellBackups = cellBackups;
        this.cellGroups = cellGroups;
        this.groupMainSchematics = groupMainSchematics;
        this.libBackups = libBackups;
        this.techPool = techPool;
        this.cellBounds = new ERectangle[cellBackups.size()];
        for (int cellIndex = 0; cellIndex < cellBounds.length; cellIndex++) {
            ERectangle r = cellBounds[cellIndex];
            if (r == null) continue;
            if (cellBackups.get(cellIndex) == null)
                throw new IllegalArgumentException();
            this.cellBounds[cellIndex] = r;
        }
        if (Job.getDebug())
            check();
    }
    
    /**
     * Creates empty snapshot.
     */
    public Snapshot(IdManager idManager) {
        this(idManager, 0, null, CellBackup.EMPTY_LIST,
                new int[0], new CellId[0],
                LibraryBackup.EMPTY_LIST, idManager.getInitialTechPool(), ERectangle.NULL_ARRAY);
    }
    
    /**
     * Creates a new instance of Snapshot which differs from this Snapshot by TechPool.
     * New TechPool must be extension of old TechPool
     * @param techPool new Technology pool
     * @return new snapshot which differs froms this Snapshot by TechPool
     * @throws IllegalArgumentException if new TechPool isn't extension of old TechPool.
     */
    public Snapshot withTechPool(TechPool techPool) {
        if (this.techPool == techPool) return this;
        CellBackup[] cellBackupArray = cellBackups.toArray(new CellBackup[cellBackups.size()]);
        for (int cellIndex = 0; cellIndex < cellBackupArray.length; cellIndex++) {
            CellBackup cellBackup = cellBackups.get(cellIndex);
            if (cellBackup == null) continue;
            cellBackupArray[cellIndex] = cellBackup.withTechPool(techPool);
        }
        return new Snapshot(idManager, idManager.newSnapshotId(), this.tool,
                new ImmutableArrayList(cellBackupArray),
                this.cellGroups, this.groupMainSchematics,
                this.libBackups, techPool, this.cellBounds);
    }
   
    /**
     * Creates a new instance of Snapshot which differs from this Snapshot.
     * Four array parameters are supplied. Each parameter may be null if its contents is the same as in this Snapshot.
     * @param tool tool which initiated database changes/
     * @param cellBackupsArray array indexed by cellIndex of new CellBackups.
     * @param cellBoundsArray array indexed by cellIndex of cell bounds.
     * @param libBackupsArray array indexed by libIndex of LibraryBackups.
     * @return new snapshot which differs froms this Snapshot or this Snapshot.
     * @throws IllegalArgumentException on invariant violation.
     * @throws ArrayOutOfBoundsException on some invariant violations.
     */
    public Snapshot with(Tool tool, CellBackup[] cellBackupsArray, ERectangle[] cellBoundsArray, LibraryBackup[] libBackupsArray) {
//        long startTime = System.currentTimeMillis();
        ImmutableArrayList<CellBackup> cellBackups = copyArray(cellBackupsArray, this.cellBackups);
        ImmutableArrayList<LibraryBackup> libBackups = copyArray(libBackupsArray, this.libBackups);
        boolean namesChanged = this.libBackups != libBackups || this.cellBackups.size() != cellBackups.size();
        if (!namesChanged && this.cellBackups == cellBackups) {
            if (cellBoundsArray != null) {
                for (int cellIndex = 0; cellIndex < cellBoundsArray.length; cellIndex++) {
                    ERectangle r = cellBoundsArray[cellIndex];
                    if (r == null) continue;
                    setCellBounds(cellBackups.get(cellIndex).cellRevision.d.cellId, r);
                }
            }
            return this;
        }
        
        // Check usages in cells
        BitSet newUsedTechs = new BitSet();
        for (int cellIndex = 0; cellIndex < cellBackups.size(); cellIndex++) {
            CellBackup newBackup = cellBackups.get(cellIndex);
            CellRevision newRevision = null;
            CellBackup oldBackup = getCell(cellIndex);
            CellId cellId;
            if (newBackup != null) {
                newRevision = newBackup.cellRevision;
                if (oldBackup == null || newRevision.d.groupName != oldBackup.cellRevision.d.groupName)
                    namesChanged = true;
                
                // If usages changed, check CellUsagesIn.
                newUsedTechs.or(newRevision.techUsages);
                cellId = newRevision.d.cellId;
                if (oldBackup == null || newRevision.cellUsages != oldBackup.cellRevision.cellUsages) {
                    for (int i = 0; i < newRevision.cellUsages.length; i++) {
                        CellRevision.CellUsageInfo cui = newRevision.cellUsages[i];
                        if (cui == null) continue;
                        if (oldBackup != null) {
                            CellRevision oldRevision = oldBackup.cellRevision;
                            if (i < oldRevision.cellUsages.length) {
                                CellRevision.CellUsageInfo oldCui = oldRevision.cellUsages[i];
                                if (oldCui != null && cui.usedExports == oldCui.usedExports) continue;
                            }
                        }
                        CellUsage u = cellId.getUsageIn(i);
                        CellRevision protoRevision = cellBackups.get(u.protoId.cellIndex).cellRevision;
                        cui.checkUsage(protoRevision);
                    }
                }
            } else {
                if (oldBackup == null) continue;
                cellId = oldBackup.cellRevision.d.cellId;
                namesChanged = true;
            }
            
            // If some exports deleted, check CellUsagesOf
            if (oldBackup == null) continue;
            CellRevision oldRevision = oldBackup.cellRevision;
            if (newRevision != null && newRevision.definedExportsLength >= oldRevision.definedExportsLength &&
                    (newRevision.deletedExports == oldRevision.deletedExports ||
                    !newRevision.deletedExports.intersects(oldRevision.definedExports))) continue;
            for (int i = 0, numUsages = cellId.numUsagesOf(); i < numUsages; i++) {
                CellUsage u = cellId.getUsageOf(i);
                int parentCellIndex = u.parentId.cellIndex;
                if (parentCellIndex >= cellBackups.size()) continue;
                CellBackup parentBackup = cellBackups.get(parentCellIndex);
                if (parentBackup == null) continue;
                CellRevision parentRevision = parentBackup.cellRevision;
                if (u.indexInParent >= parentRevision.cellUsages.length) continue;
                CellRevision.CellUsageInfo cui = parentRevision.cellUsages[u.indexInParent];
                if (cui == null) continue;
                cui.checkUsage(newBackup.cellRevision);
            }
        }
        for (int techIndex = 0; techIndex < newUsedTechs.length(); techIndex++) {
            if (!newUsedTechs.get(techIndex)) continue;
            TechId techId = idManager.getTechId(techIndex);
            if (techPool.getTech(techId) == null)
                throw new IllegalArgumentException();
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
                for (int groupIndex: cellGroups)
                    maxGroup = Math.max(maxGroup, groupIndex);
                groupMainSchematics = new CellId[maxGroup + 1];
                for (int cellIndex = 0; cellIndex < cellBackups.size(); cellIndex++) {
                    CellBackup cellBackup = cellBackups.get(cellIndex);
                    if (cellBackup == null) continue;
                    CellId cellId = cellBackup.cellRevision.d.cellId;
                    if (cellId.cellName.getView() != View.SCHEMATIC) continue;
                    int groupIndex = cellGroups[cellIndex];
                    CellId mainSchematics = groupMainSchematics[groupIndex];
                    if (mainSchematics == null || cellId.cellName.compareTo(mainSchematics.cellName) < 0)
                        groupMainSchematics[groupIndex] = cellId;
                }
                if (Arrays.equals(this.groupMainSchematics, groupMainSchematics))
                    groupMainSchematics = this.groupMainSchematics;
            }
        }
        
        if (cellBoundsArray == null)
            cellBoundsArray = this.cellBounds;
        Snapshot snapshot = new Snapshot(idManager, idManager.newSnapshotId(), tool, cellBackups,
                cellGroups, groupMainSchematics,
                libBackups, this.techPool, cellBoundsArray);
//        long endTime = System.currentTimeMillis();
//        System.out.println("Creating snapshot took: " + (endTime - startTime) + " msec");
        return snapshot;
    }
    
    /**
     * Sets cell bounds for a cell with given CellId
     * @param cellId given CellId
     * @param r cell boubds
     * @throws IllegalArgumentException if snapshot has not cell with given cellId or on bounds conflict.
     */
    public void setCellBounds(CellId cellId, ERectangle r) {
        if (r == null)
            throw new NullPointerException();
        CellBackup cellBackup = getCell(cellId);
        if (cellBackup == null)
            throw new IllegalArgumentException();
        int cellIndex = cellId.cellIndex;
        ERectangle oldR = cellBounds[cellIndex];
        if (oldR != null && oldR != r)
            throw new IllegalArgumentException();
        cellBounds[cellIndex] = r;
    }
    
//    private void checkUsedLibs(BitSet usedLibs) {
//        if (usedLibs.isEmpty()) return;
//        int usedLibsLength = usedLibs.length();
//        if (usedLibsLength > libBackups.size())
//            throw new IllegalArgumentException("usedLibsLength");
//        for (int libIndex = 0; libIndex < usedLibsLength; libIndex++) {
//            if (usedLibs.get(libIndex) && libBackups.get(libIndex) == null)
//                throw new IllegalArgumentException("usedLibs");
//        }
//    }
    
    private static <T> ImmutableArrayList<T> copyArray(T[] newArray, ImmutableArrayList<T> oldList) {
        if (newArray == null) return oldList;
        int l;
        for (l = newArray.length; l > 0 && newArray[l - 1] == null; l--);
        if (l == oldList.size()) {
            int i = 0;
            while (i < oldList.size() && newArray[i] == oldList.get(i)) i++;
            if (i == l) return oldList;
        }
        return new ImmutableArrayList<T>(newArray, 0, l);
    }
    
//    private static int[] copyArray(int[] newArray, int[] oldArray) {
//        if (newArray == null) return oldArray;
//        int l;
//        for (l = newArray.length; l > 0 && newArray[l - 1] < 0; l--);
//        if (l == oldArray.length) {
//            int i = 0;
//            while (i < oldArray.length && newArray[i] == oldArray[i]) i++;
//            if (i == l) return oldArray;
//        }
//        int[] copyArray = new int[l];
//        System.arraycopy(newArray, 0, copyArray, 0, l);
//        if (l > 0 && copyArray[l - 1] < 0)
//            throw new ConcurrentModificationException();
//        return copyArray;
//    }
    
	/**
	 * Returns Snapshot which differs from this Snapshot by renamed Ids.
	 * @param idMapper a map from old Ids to new Ids.
     * @return Snapshot with renamed Ids.
	 */
    public Snapshot withRenamedIds(IdMapper idMapper, CellId fromGroup, String toGroup) {
        int maxCellIndex = -1;
        for (CellBackup cellBackup: cellBackups) {
            if (cellBackup == null) continue;
            maxCellIndex = Math.max(maxCellIndex, idMapper.get(cellBackup.cellRevision.d.cellId).cellIndex);
        }
        int maxLibIndex = -1;
        for (LibraryBackup libBackup: libBackups) {
            if (libBackup == null) continue;
            maxLibIndex = Math.max(maxLibIndex, idMapper.get(libBackup.d.libId).libIndex);
        }
        
        CellBackup[] cellBackupsArray = new CellBackup[maxCellIndex + 1];
        ERectangle[] cellBoundsArray = new ERectangle[maxCellIndex + 1];
        LibraryBackup[] libBackupsArray = new LibraryBackup[maxLibIndex + 1];
        
        boolean cellBackupsChanged = false;
        boolean cellIdsChanged = false;
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
            if (toGroup == null)
                toGroupIndex2 = fromGroupIndex;
            for (int cellIndex = 0; cellIndex < cellBackups.size(); cellIndex++) {
                CellBackup cellBackup = cellBackups.get(cellIndex);
                if (cellBackup == null) continue;
                CellId cellId = cellBackup.cellRevision.d.cellId;
                if (cellId.libId == fromGroup.libId) {
                    if (cellId.cellName.getName().equals(toGroupCellId.cellName.getName()))
                        toGroupIndex1 = cellGroups[cellIndex];
                    if (toGroup != null && cellId.cellName.getName().equals(toGroup))
                        toGroupIndex2 = cellGroups[cellIndex];
                }
            }
            ArrayList<CellName> fromCellNames = new ArrayList<CellName>();
            ArrayList<CellName> toCellNames = new ArrayList<CellName>();
            toGroupCellIds.set(toGroupCellId.cellIndex);
            toCellNames.add(toGroupCellId.cellName);
            for (int cellIndex = 0; cellIndex < cellBackups.size(); cellIndex++) {
                CellBackup cellBackup = cellBackups.get(cellIndex);
                if (cellBackup == null || cellIndex == fromGroup.cellIndex) continue;
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
            if (!fromCellNames.isEmpty())
                fromGroupName = makeCellGroupName(fromCellNames);
            if (!toCellNames.isEmpty())
                toGroupName = makeCellGroupName(toCellNames);
        }
        for (int cellIndex = 0; cellIndex < cellBackups.size(); cellIndex++) {
            CellBackup oldCellBackup = cellBackups.get(cellIndex);
            if (oldCellBackup == null) continue;
            CellName newGroupName = oldCellBackup.cellRevision.d.groupName;
            if (fromGroup != null) {
                if (toGroupCellIds.get(cellIndex) || cellIndex == fromGroup.cellIndex)
                    newGroupName = toGroupName;
                else if (fromGroupCellIds.get(cellIndex))
                    newGroupName = fromGroupName;
            }
            CellBackup newCellBackup = oldCellBackup.withRenamedIds(idMapper, newGroupName);
            if (newCellBackup != oldCellBackup)
                cellBackupsChanged = true;
            int newCellIndex = newCellBackup.cellRevision.d.cellId.cellIndex;
            if (newCellIndex != cellIndex)
                cellIdsChanged = true;
            cellBackupsArray[newCellIndex] = newCellBackup;
            cellBoundsArray[newCellIndex] = this.cellBounds[cellIndex];
        }
        if (!cellBackupsChanged)
            cellBackupsArray = null;
        if (!cellIdsChanged)
            cellBoundsArray = null;
        
        boolean libBackupsChanged = false;
        for (int libIndex = 0; libIndex < libBackups.size(); libIndex++) {
            LibraryBackup oldLibBackup = libBackups.get(libIndex);
            if (oldLibBackup == null) continue;
            LibraryBackup newLibBackup = oldLibBackup.withRenamedIds(idMapper);
            if (newLibBackup != oldLibBackup)
                libBackupsChanged = true;
            libBackupsArray[newLibBackup.d.libId.libIndex] = newLibBackup;
        }
        if (!libBackupsChanged)
            libBackupsArray = null;
        
        if (cellBackupsArray == null && cellBounds == null && libBackups == null) return this;
        return with(tool, cellBackupsArray, cellBoundsArray, libBackupsArray);
    }
    
    public List<LibId> getChangedLibraries(Snapshot oldSnapshot) {
        if (oldSnapshot == null) oldSnapshot = idManager.getInitialSnapshot();
        if (idManager != oldSnapshot.idManager) throw new IllegalArgumentException();
        List<LibId> changed = null;
        if (oldSnapshot.libBackups != libBackups) {
            int numLibs = Math.max(oldSnapshot.libBackups.size(), libBackups.size());
            for (int i = 0; i < numLibs; i++) {
                LibraryBackup oldBackup = oldSnapshot.getLib(i);
                LibraryBackup newBackup = getLib(i);
                if (oldBackup == newBackup) continue;
                if (changed == null) changed = new ArrayList<LibId>();
                changed.add(idManager.getLibId(i));
            }
        }
        if (changed == null) changed = Collections.emptyList();
        return changed;
    }
    
    public List<CellId> getChangedCells(Snapshot oldSnapshot) {
        if (oldSnapshot == null) oldSnapshot = idManager.getInitialSnapshot();
        List<CellId> changed = null;
        int numCells = Math.max(oldSnapshot.cellBackups.size(), cellBackups.size());
        for (int i = 0; i < numCells; i++) {
            CellBackup oldBackup = oldSnapshot.getCell(i);
            CellBackup newBackup = getCell(i);
            if (oldBackup == newBackup) continue;
            if (changed == null) changed = new ArrayList<CellId>();
            changed.add(idManager.getCellId(i));
        }
        if (changed == null) changed = Collections.emptyList();
        return changed;
    }
    
    public Collection<CellId> getCellsDownTop() {
        LinkedHashSet<CellId> order = new LinkedHashSet<CellId>();
        for (CellBackup cellBackup: cellBackups) {
            if (cellBackup == null) continue;
            getCellsDownTop(cellBackup.cellRevision.d.cellId, order);
        }
        return order;
    }
    
    private void getCellsDownTop(CellId root, LinkedHashSet<CellId> order) {
        if (order.contains(root)) return;
        CellBackup cellBackup = getCell(root);
        CellRevision cellRevision = cellBackup.cellRevision; 
        for (int i = 0; i < cellRevision.cellUsages.length; i++) {
            if (cellRevision.cellUsages[i] == null) continue;
            CellUsage cu = root.getUsageIn(i);
            getCellsDownTop(cu.protoId, order);
        }
        boolean added = order.add(root);
        assert added;
    }
    
    public CellBackup getCell(CellId cellId) {
        if (cellId.getIdManager() != idManager)
            throw new IllegalArgumentException();
        return getCell(cellId.cellIndex);
    }
    
    public CellRevision getCellRevision(CellId cellId) {
        CellBackup cellBackup = getCell(cellId);
        return cellBackup != null ? cellBackup.cellRevision : null;
    }
    
    public CellBackup getCell(int cellIndex) {
        return cellIndex < cellBackups.size() ? cellBackups.get(cellIndex) : null; 
    }
    
    public CellRevision getCellRevision(int cellIndex) {
        CellBackup cellBackup = getCell(cellIndex);
        return cellBackup != null ? cellBackup.cellRevision : null;
    }
    
//    /**
//     * Returns group name of cell with specified CellId.
//     * Name of cell group is a base name of main schematics cell if any.
//     * Otherwise it is a shortest base name among cells in the group.
//     * If cell with the cellId is absent in this Snapshot, returns null.
//     * @param cellId cellId of a cell.
//     * @return name of cell group or null.
//     *
//     */
//    public String getCellGroupName(CellId cellId) {
//        int groupIndex = cellId.cellIndex < cellGroups.length ? cellGroups[cellId.cellIndex] : -1;
//        if (groupIndex < 0) return null;
//        ArrayList<CellName> cellNames = new ArrayList<CellName>();
//        for (CellBackup cellBackup: cellBackups) {
//            if (cellBackup == null) continue;
//            if (cellGroups[cellBackup.d.cellId.cellIndex] != groupIndex) continue;
//            cellNames.add(cellBackup.d.cellId.cellName);
//        }
//        if (cellNames.isEmpty()) return null;
//        return makeCellGroupName(cellNames).getName();
//    }
    
    /**
     * Returns group name of group with specified CellNames.
     * Name of cell group is a base name of main schematics cell if any.
     * Otherwise it is a shortest base name among cells in the group.
     * @param cellNames collection of CellNames in a group.
     * @return name of cell group.
     * @throws InvalidArgumentException if cellNames is empty
     */
    public static CellName makeCellGroupName(Collection<CellName> cellNames) {
        if (cellNames.isEmpty())
            throw new IllegalArgumentException();
        String groupName = null;
        for (CellName cellName: cellNames) {
            if (cellName.getView() != View.SCHEMATIC) continue;
            String name = cellName.getName();
            if (groupName == null || TextUtils.STRING_NUMBER_ORDER.compare(name, groupName) < 0)
                groupName = name;
        }
        if (groupName == null) {
            for (CellName cellName: cellNames) {
                String name = cellName.getName();
                if (groupName == null || name.length() < groupName.length() ||
                        name.length() == groupName.length() && TextUtils.STRING_NUMBER_ORDER.compare(name, groupName) < 0)
                    groupName = name;
            }
        }
        assert groupName != null;
        return CellName.parseName(groupName + "{sch}");
    }
    
    public ERectangle getCellBounds(CellId cellId) { return getCellBounds(cellId.cellIndex); }
    
    public ERectangle getCellBounds(int cellIndex) {
        return cellIndex < cellBounds.length ? cellBounds[cellIndex] : null; 
    }
    
    /** Returns TechPool of this Snapshot */
    public TechPool getTechPool() { return techPool; }
    
   /**
     * Get Technology by TechId
     * TechId must belong to same IdManager as TechPool
     * @param techId TechId to find
     * @return Technology b giben TechId or null
     * @throws IllegalArgumentException of TechId is not from this IdManager
     */
    public Technology getTech(TechId techId) { return techPool.getTech(techId); }
    
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
    
    public LibraryBackup getLib(LibId libId) { return getLib(libId.libIndex); }
    
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
        assert oldSnapshot.cellBoundsDefined();
        assert cellBoundsDefined();
        writer.writeDiffs();
        writer.writeInt(snapshotId);
        
        writer.writeBoolean(tool != null);
        if (tool != null)
            writer.writeTool(tool);
        
        boolean technologiesChanged = techPool != oldSnapshot.techPool;
        writer.writeBoolean(technologiesChanged);
        if (technologiesChanged)
            techPool.write(writer);
        
        boolean libsChanged = oldSnapshot.libBackups != libBackups;
        writer.writeBoolean(libsChanged);
        if (libsChanged) {
            writer.writeInt(libBackups.size());
            for (int i = 0; i < libBackups.size(); i++) {
                LibraryBackup oldBackup = oldSnapshot.getLib(i);
                LibraryBackup newBackup = getLib(i);
                if (oldBackup == newBackup) continue;
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
        boolean boundsChanged = oldSnapshot.cellBounds.length != cellBounds.length;
        for (int cellIndex = 0; cellIndex < cellBounds.length; cellIndex++) {
            if (cellBackups.get(cellIndex) == null) continue;
            boundsChanged = boundsChanged || cellBounds[cellIndex] != oldSnapshot.cellBounds[cellIndex];
        }
        writer.writeBoolean(boundsChanged);
        for (int i = 0; i < cellBackups.size(); i++) {
            CellBackup oldBackup = oldSnapshot.getCell(i);
            CellBackup newBackup = getCell(i);
            if (oldBackup == newBackup) continue;
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
        
        if (boundsChanged) {
            for (int i = 0; i < cellBackups.size(); i++) {
                CellBackup newBackup = getCell(i);
                if (newBackup == null) continue;
                ERectangle oldBounds = oldSnapshot.getCellBounds(i);
                ERectangle newBounds = getCellBounds(i);
                assert newBounds != null;
                if (oldBounds != newBounds) {
                    writer.writeInt(i);
                    writer.writeRectangle(newBounds);
                }
            }
            writer.writeInt(Integer.MAX_VALUE);
        }
        
        boolean cellGroupsChanged = cellGroups != oldSnapshot.cellGroups;
        writer.writeBoolean(cellGroupsChanged);
        if (cellGroupsChanged) {
            assert cellGroups.length == cellBackups.size();
            for (int cellIndex = 0; cellIndex < cellGroups.length; cellIndex++)
                writer.writeInt(cellGroups[cellIndex]);
            for (int groupIndex = 0; groupIndex < groupMainSchematics.length; groupIndex++) {
                CellId mainSchematics = groupMainSchematics[groupIndex];
                writer.writeInt(mainSchematics != null ? mainSchematics.cellIndex : -1);
            }
        }
    }
    
    public static Snapshot readSnapshot(IdReader reader, Snapshot oldSnapshot) throws IOException {
        assert reader.idManager == oldSnapshot.idManager;
        assert oldSnapshot.cellBoundsDefined();
        reader.readDiffs();
        int snapshotId = reader.readInt();
        boolean hasTool = reader.readBoolean();
        Tool tool = hasTool ? reader.readTool() : null;
        
        TechPool techPool = oldSnapshot.techPool;
        boolean technologiesChanged = reader.readBoolean();
        if (technologiesChanged)
            techPool = TechPool.read(reader);
        
        ImmutableArrayList<LibraryBackup> libBackups = oldSnapshot.libBackups;
        boolean libsChanged = reader.readBoolean();
        if (libsChanged) {
            int libLen = reader.readInt();
            LibraryBackup[] libBackupsArray = new LibraryBackup[libLen];
            for (int libIndex = 0, numLibs = Math.min(oldSnapshot.libBackups.size(), libLen); libIndex < numLibs; libIndex++)
                libBackupsArray[libIndex] = oldSnapshot.libBackups.get(libIndex);
            for (;;) {
                int libIndex = reader.readInt();
                if (libIndex == Integer.MAX_VALUE) break;
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
        for (int cellIndex = 0; cellIndex < cellMax; cellIndex++)
            cellBackupsArray[cellIndex] = oldSnapshot.cellBackups.get(cellIndex);
        boolean boundsChanged = reader.readBoolean();
        ERectangle[] cellBoundsArray = oldSnapshot.cellBounds;
        if (boundsChanged) {
            cellBoundsArray = new ERectangle[cellLen];
            for (int cellIndex = 0, numCells = Math.min(oldSnapshot.cellBounds.length, cellLen); cellIndex < numCells; cellIndex++)
                cellBoundsArray[cellIndex] = oldSnapshot.cellBounds[cellIndex];
        }
        if (technologiesChanged) {
            for (int cellIndex = 0; cellIndex < cellLen; cellIndex++) {
                CellBackup cellBackup = cellBackupsArray[cellIndex];
                if (cellBackup == null) continue;
                cellBackupsArray[cellIndex] = cellBackup.withTechPool(techPool);
            }
        }
        for (;;) {
            int cellIndex = reader.readInt();
            if (cellIndex == Integer.MAX_VALUE) break;
            if (cellIndex >= 0) {
                CellBackup newBackup = CellBackup.read(reader, techPool);
                cellBackupsArray[cellIndex] = newBackup;
            } else {
                cellIndex = ~cellIndex;
                assert cellBackupsArray[cellIndex] != null;
                cellBackupsArray[cellIndex] = null;
                assert boundsChanged;
                assert cellBoundsArray[cellIndex] != null;
                cellBoundsArray[cellIndex] = null;
            }
        }
        ImmutableArrayList<CellBackup> cellBackups = new ImmutableArrayList<CellBackup>(cellBackupsArray);
        
        if (boundsChanged) {
            for (;;) {
                int cellIndex = reader.readInt();
                if (cellIndex == Integer.MAX_VALUE) break;
                ERectangle newBounds = reader.readRectangle();
                cellBoundsArray[cellIndex] = newBounds;
            }
        }
        
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
                if (cellIndex >= 0)
                    mainSchematics = reader.idManager.getCellId(cellIndex);
                groupMainSchematics[groupIndex] = mainSchematics;
            }
        }
        for (int i = 0; i < cellBackups.size(); i++) {
            assert (cellBackups.get(i) != null) == (cellBoundsArray[i] != null);
            assert (cellBackups.get(i) != null) == (cellGroups[i] >= 0);
        }
        return new Snapshot(oldSnapshot.idManager, snapshotId, tool, cellBackups,
                cellGroups, groupMainSchematics,
                libBackups, techPool, cellBoundsArray);
    }
    
    /**
     * Checks if all cell bounds are defined.
     * @return true if all cell bounds are defined.
     */
    public boolean cellBoundsDefined() {
        assert cellBackups.size() == cellBounds.length;
        for (int cellIndex = 0; cellIndex < cellBackups.size(); cellIndex++) {
            if (cellBackups.get(cellIndex) == null) continue;
            if (cellBounds[cellIndex] == null) return false;
        }
        return true;
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
        for (LibraryBackup libBackup: libBackups) {
            if (libBackup == null) continue;
            libBackup.check();
        }
        for (CellBackup cellBackup: cellBackups) {
            if (cellBackup != null) cellBackup.check();
        }
        if (libBackups.size() > 0)
            assert libBackups.get(libBackups.size() - 1) != null;
        if (cellBackups.size() > 0)
            assert cellBackups.get(cellBackups.size() - 1) != null;
        int[] cellGroups = new int[cellBackups.size()];
        checkNames(idManager, cellBackups, cellGroups, libBackups);
        assert Arrays.equals(this.cellGroups, cellGroups);
        int maxGroup = -1;
        for (int groupIndex: cellGroups)
            maxGroup = Math.max(maxGroup, groupIndex);
        assert groupMainSchematics.length == maxGroup + 1;
        BitSet foundMainSchematics = new BitSet();
        for (CellBackup cellBackup: cellBackups) {
            if (cellBackup == null) continue;
            CellId cellId = cellBackup.cellRevision.d.cellId;
            if (cellId.cellName.getView() != View.SCHEMATIC) continue;
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
        assert cellBackups.size() == cellBounds.length;
        BitSet checkUsedTechs = new BitSet();
        for (int cellIndex = 0; cellIndex < cellBackups.size(); cellIndex++) {
            CellBackup cellBackup = cellBackups.get(cellIndex);
            if (cellBackup == null) {
                assert cellBounds[cellIndex] == null;
                continue;
            }
            CellRevision cellRevision = cellBackup.cellRevision;
//            assert cellBounds.get(cellIndex) != null;
            checkUsedTechs.or(cellRevision.techUsages);
            CellId cellId = cellBackup.cellRevision.d.cellId;
            for (int i = 0; i < cellRevision.cellUsages.length; i++) {
                CellRevision.CellUsageInfo cui = cellRevision.cellUsages[i];
                if (cui == null) continue;
                CellUsage u = cellId.getUsageIn(i);
                int subCellIndex = u.protoId.cellIndex;
                cui.checkUsage(cellBackups.get(subCellIndex).cellRevision);
            }
            assert cellBackup.techPool == techPool;
        }
        assert techPool.idManager == idManager;
        for (int techIndex = 0; techIndex < checkUsedTechs.length(); techIndex++) {
            if (!checkUsedTechs.get(techIndex)) continue;
            TechId techId = idManager.getTechId(techIndex);
            assert techId != null;
            assert techPool.getTech(techId) != null;
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
        ArrayList<HashMap<String,CellName>> protoNameToGroupName = new ArrayList<HashMap<String,CellName>>();
        ArrayList<HashSet<CellName>> cellNames = new ArrayList<HashSet<CellName>>();
        ArrayList<HashMap<CellName,Integer>> groupNameToGroupIndex = new ArrayList<HashMap<CellName,Integer>>();
        for (int libIndex = 0; libIndex < libBackups.size(); libIndex++) {
            LibraryBackup libBackup = libBackups.get(libIndex);
            if (libBackup == null) {
                protoNameToGroupName.add(null);
                cellNames.add(null);
                groupNameToGroupIndex.add(null);
                continue;
            }
            protoNameToGroupName.add(new HashMap<String,CellName>());
            cellNames.add(new HashSet<CellName>());
            groupNameToGroupIndex.add(new HashMap<CellName,Integer>());
            if (libBackup.d.libId != idManager.getLibId(libIndex))
                throw new IllegalArgumentException("LibId");
            String libName = libBackup.d.libId.libName;
            if (!libNames.add(libName))
                throw new IllegalArgumentException("duplicate libName");
            for (LibId libId: libBackup.referencedLibs) {
                if (libId != libBackups.get(libId.libIndex).d.libId)
                    throw new IllegalArgumentException("LibId in referencedLibs");
            }
        }
        assert protoNameToGroupName.size() == libBackups.size() && cellNames.size() == libBackups.size() && groupNameToGroupIndex.size() == libBackups.size();
        
        assert cellBackups.size() == cellGroups.length;
        Arrays.fill(cellGroups, -1);
        int groupCount = 0;
        for (int cellIndex = 0; cellIndex < cellBackups.size(); cellIndex++) {
            CellBackup cellBackup = cellBackups.get(cellIndex);
            if (cellBackup == null) continue;
            ImmutableCell d = cellBackup.cellRevision.d;
            CellId cellId = d.cellId;
            if (cellId != idManager.getCellId(cellIndex))
                throw new IllegalArgumentException("CellId");
            LibId libId = d.getLibId();
            int libIndex = libId.libIndex;
            if (libId != libBackups.get(libIndex).d.libId)
                throw new IllegalArgumentException("LibId in ImmutableCell");
            HashMap<String,CellName> cellNameToGroupNameInLibrary = protoNameToGroupName.get(libId.libIndex);
            HashSet<CellName> cellNamesInLibrary = cellNames.get(libId.libIndex);
            HashMap<CellName,Integer> groupNameToGroupIndexInLibrary = groupNameToGroupIndex.get(libId.libIndex);
            String protoName = cellId.cellName.getName();
            CellName groupName = cellNameToGroupNameInLibrary.get(protoName);
            if (groupName == null) {
                groupName = d.groupName;
                cellNameToGroupNameInLibrary.put(protoName, groupName);
            } else if (!d.groupName.equals(groupName))
                throw new IllegalArgumentException("cells with same proto name in different groups");
            Integer gn = groupNameToGroupIndexInLibrary.get(groupName);
            if (gn == null) {
                gn = Integer.valueOf(groupCount++);
                groupNameToGroupIndexInLibrary.put(groupName, gn);
            }
            cellGroups[cellIndex] = gn.intValue();
            if (!cellNamesInLibrary.add(cellId.cellName))
                throw new IllegalArgumentException("duplicate CellName in library");
        }
    }
}
