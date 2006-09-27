/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Snapshot.java
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

import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.ImmutableArrayList;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.Tool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
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
    public final ImmutableArrayList<LibraryBackup> libBackups;
    private final ERectangle[] cellBounds;

    /** Creates a new instance of Snapshot */
    private Snapshot(IdManager idManager, int snapshotId, Tool tool,
            ImmutableArrayList<CellBackup> cellBackups,
            int[] cellGroups,
            ImmutableArrayList<LibraryBackup> libBackups,
            ERectangle[] cellBounds) {
        this.idManager = idManager;
        this.snapshotId = snapshotId;
        this.tool = tool;
        this.cellBackups = cellBackups;
        this.cellGroups = cellGroups;
        this.libBackups = libBackups;
        this.cellBounds = new ERectangle[cellBackups.size()];
        for (int cellIndex = 0; cellIndex < cellBounds.length; cellIndex++) {
            ERectangle r = cellBounds[cellIndex];
            if (r == null) continue;
            if (cellBackups.get(cellIndex) == null)
                throw new IllegalArgumentException();
            this.cellBounds[cellIndex] = r;
        }
    }
    
    /**
     * Creates empty snapshot.
     */
    Snapshot(IdManager idManager) {
        this(idManager, 0, null, CellBackup.EMPTY_LIST, new int[0], LibraryBackup.EMPTY_LIST, ERectangle.NULL_ARRAY);
    }
    
    /**
     * Creates a new instance of Snapshot which differs from this Snapshot.
     * Four array parameters are supplied. Each parameter may be null if its contents is the same as in this Snapshot.
     * @param tool tool which initiated database changes/
     * @param cellBackupArray array indexed by cellIndex of new CellBackups.
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
                    setCellBounds(cellBackups.get(cellIndex).d.cellId, r);
                }
            }
            return this;
        }
        
        // Check usages in libs
        for (int libIndex = 0; libIndex < libBackups.size(); libIndex++) {
            LibraryBackup libBackup = libBackups.get(libIndex);
            if (libBackup == null) continue;
            checkUsedLibs(libBackup.usedLibs);
            checkLibExports(libBackup);
        }

        // Check usages in cells
        for (int cellIndex = 0; cellIndex < cellBackups.size(); cellIndex++) {
            CellBackup newBackup = cellBackups.get(cellIndex);
            CellBackup oldBackup = getCell(cellIndex);
            CellId cellId;
            if (newBackup != null) {
                if (oldBackup == null || newBackup.d.groupName != oldBackup.d.groupName)
                    namesChanged = true;
                
                // Check lib usages.
                checkUsedLibs(newBackup.usedLibs);
                
                // If usages changed, check CellUsagesIn.
                cellId = newBackup.d.cellId;
                if (oldBackup == null || newBackup.cellUsages != oldBackup.cellUsages) {
                    for (int i = 0; i < newBackup.cellUsages.length; i++) {
                        CellBackup.CellUsageInfo cui = newBackup.cellUsages[i];
                        if (cui == null) continue;
                        if (oldBackup != null && i < oldBackup.cellUsages.length) {
                            CellBackup.CellUsageInfo oldCui = oldBackup.cellUsages[i];
                            if (oldCui != null && cui.usedExports == oldCui.usedExports) continue;
                        }
                        CellUsage u = cellId.getUsageIn(i);
                        CellBackup protoBackup = cellBackups.get(u.protoId.cellIndex);
                        cui.checkUsage(protoBackup);
                    }
                }
            } else {
                if (oldBackup == null) continue;
                cellId = oldBackup.d.cellId;
                namesChanged = true;
            }
            
            // If some exports deleted, check CellUsagesOf
            if (oldBackup == null) continue;
            if (newBackup != null && newBackup.definedExportsLength >= oldBackup.definedExportsLength &&
                    (newBackup.deletedExports == oldBackup.deletedExports ||
                    !newBackup.deletedExports.intersects(oldBackup.definedExports))) continue;
            for (int i = 0, numUsages = cellId.numUsagesOf(); i < numUsages; i++) {
                CellUsage u = cellId.getUsageOf(i);
                int parentCellIndex = u.parentId.cellIndex;
                if (parentCellIndex >= cellBackups.size()) continue;
                CellBackup parentBackup = cellBackups.get(parentCellIndex);
                if (parentBackup == null) continue;
                if (u.indexInParent >= parentBackup.cellUsages.length) continue;
                CellBackup.CellUsageInfo cui = parentBackup.cellUsages[u.indexInParent];
                if (cui == null) continue;
                cui.checkUsage(newBackup);
            }
        }
        
        // Check names
        int[] cellGroups = this.cellGroups;
        if (namesChanged) {
            cellGroups = new int[cellBackups.size()];
            checkNames(idManager, cellBackups, cellGroups, libBackups);
            if (Arrays.equals(this.cellGroups, cellGroups))
                cellGroups = this.cellGroups;
        }
        
        if (cellBoundsArray == null)
            cellBoundsArray = this.cellBounds;
        Snapshot snapshot = new Snapshot(idManager, idManager.newSnapshotId(), tool, cellBackups, cellGroups, libBackups, cellBoundsArray);
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
    
    private void checkUsedLibs(BitSet usedLibs) {
        if (usedLibs.isEmpty()) return;
        int usedLibsLength = usedLibs.length();
        if (usedLibsLength > libBackups.size())
            throw new IllegalArgumentException("usedLibsLength");
        for (int libIndex = 0; libIndex < usedLibsLength; libIndex++) {
            if (usedLibs.get(libIndex) && libBackups.get(libIndex) == null)
                throw new IllegalArgumentException("usedLibs");
        }
    }
    
    private void checkLibExports(LibraryBackup libBackup) {
        if (libBackup.usedExports == null) return;
        for (CellId cellId: libBackup.usedExports.keySet()) {
            CellBackup cellBackup = getCell(cellId);
            if (cellBackup == null)
                throw new IllegalArgumentException("usedCells");
            BitSet usedExportsCopy = (BitSet)libBackup.usedExports.get(cellId).clone();
            usedExportsCopy.andNot(cellBackup.definedExports);
            if (!usedExportsCopy.isEmpty())
                throw new IllegalArgumentException("usedExports");
        }
    }
    
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
    
    private static int[] copyArray(int[] newArray, int[] oldArray) {
        if (newArray == null) return oldArray;
        int l;
        for (l = newArray.length; l > 0 && newArray[l - 1] < 0; l--);
        if (l == oldArray.length) {
            int i = 0;
            while (i < oldArray.length && newArray[i] == oldArray[i]) i++;
            if (i == l) return oldArray;
        }
        int[] copyArray = new int[l];
        System.arraycopy(newArray, 0, copyArray, 0, l);
        if (l > 0 && copyArray[l - 1] < 0)
            throw new ConcurrentModificationException();
        return copyArray;
    }
    
	/**
	 * Returns Snapshot which differs from this Snapshot by renamed Ids.
	 * @param idMapper a map from old Ids to new Ids.
     * @return Snapshot with renamed Ids.
	 */
    public Snapshot withRenamedIds(IdMapper idMapper, CellId fromGroup, String toGroup) {
        int maxCellIndex = -1;
        for (CellBackup cellBackup: cellBackups) {
            if (cellBackup == null) continue;
            maxCellIndex = Math.max(maxCellIndex, idMapper.get(cellBackup.d.cellId).cellIndex);
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
                if (cellBackup.d.cellId.libId == fromGroup.libId) {
                    if (cellBackup.d.cellId.cellName.getName().equals(toGroupCellId.cellName.getName()))
                        toGroupIndex1 = cellGroups[cellIndex];
                    if (toGroup != null && cellBackup.d.cellId.cellName.getName().equals(toGroup))
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
                if (cellGroups[cellIndex] == fromGroupIndex) {
                    fromGroupCellIds.set(cellIndex);
                    fromCellNames.add(cellBackup.d.cellId.cellName);
                }
                if (cellGroups[cellIndex] == toGroupIndex1 || cellGroups[cellIndex] == toGroupIndex2) {
                    toGroupCellIds.set(cellIndex);
                    toCellNames.add(cellBackup.d.cellId.cellName);
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
            CellName newGroupName = oldCellBackup.d.groupName;
            if (fromGroup != null) {
                if (toGroupCellIds.get(cellIndex) || cellIndex == fromGroup.cellIndex)
                    newGroupName = toGroupName;
                else if (fromGroupCellIds.get(cellIndex))
                    newGroupName = fromGroupName;
            }
            CellBackup newCellBackup = oldCellBackup.withRenamedIds(idMapper, newGroupName);
            if (newCellBackup != oldCellBackup)
                cellBackupsChanged = true;
            int newCellIndex = newCellBackup.d.cellId.cellIndex;
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
    
    public CellBackup getCell(CellId cellId) {
        if (cellId.getIdManager() != idManager)
            throw new IllegalArgumentException();
        return getCell(cellId.cellIndex);
    }
    
    public CellBackup getCell(int cellIndex) {
        return cellIndex < cellBackups.size() ? cellBackups.get(cellIndex) : null; 
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
    
    public void writeDiffs(SnapshotWriter writer, Snapshot oldSnapshot) throws IOException {
        assert oldSnapshot.cellBoundsDefined();
        assert cellBoundsDefined();
        idManager.writeDiffs(writer);
        writer.writeInt(snapshotId);
        writer.writeBoolean(tool != null);
        if (tool != null)
            writer.writeTool(tool);
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
                    writer.writeCoord(newBounds.getX());
                    writer.writeCoord(newBounds.getY());
                    writer.writeCoord(newBounds.getWidth());
                    writer.writeCoord(newBounds.getHeight());
                }
            }
            writer.writeInt(Integer.MAX_VALUE);
        }
        
        boolean cellGroupsChanged = cellGroups != oldSnapshot.cellGroups;
        writer.writeBoolean(cellGroupsChanged);
        if (cellGroupsChanged) {
            writer.writeInt(cellGroups.length);
            for (int i = 0; i < cellGroups.length; i++)
                writer.writeInt(cellGroups[i]);
        }
    }
    
    public static Snapshot readSnapshot(SnapshotReader reader, Snapshot oldSnapshot) throws IOException {
        assert oldSnapshot.cellBoundsDefined();
        oldSnapshot.idManager.readDiffs(reader);
        int snapshotId = reader.readInt();
        boolean hasTool = reader.readBoolean();
        Tool tool = hasTool ? reader.readTool() : null;
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
        for (;;) {
            int cellIndex = reader.readInt();
            if (cellIndex == Integer.MAX_VALUE) break;
            if (cellIndex >= 0) {
                CellBackup newBackup = CellBackup.read(reader);
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
                double x = reader.readCoord();
                double y = reader.readCoord();
                double width = reader.readCoord();
                double height = reader.readCoord();
                ERectangle newBounds = new ERectangle(x, y, width, height);
                cellBoundsArray[cellIndex] = newBounds;
            }
        }
        
        int[] cellGroups = oldSnapshot.cellGroups;;
        boolean cellGroupsChanged = reader.readBoolean();
        if (cellGroupsChanged) {
            int cellGroupsLength = reader.readInt();
            cellGroups = new int[cellGroupsLength];
            for (int i = 0; i < cellGroups.length; i++)
                cellGroups[i] = reader.readInt();
        }
        for (int i = 0; i < cellBackups.size(); i++) {
            assert (cellBackups.get(i) != null) == (cellBoundsArray[i] != null);
            assert (cellBackups.get(i) != null) == (cellGroups[i] >= 0);
        }
        return new Snapshot(oldSnapshot.idManager, snapshotId, tool, cellBackups, cellGroups, libBackups, cellBoundsArray);
    }
    
    /**
     * Checks if all cell bounds are defined.
     * @returns true if all cell bounds are defined.
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
        for (LibraryBackup libBackup: libBackups) {
            if (libBackup == null) continue;
            libBackup.check();
            checkUsedLibs(libBackup.usedLibs);
            checkLibExports(libBackup);
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
        assert cellBackups.size() == cellBounds.length;
        for (int cellIndex = 0; cellIndex < cellBackups.size(); cellIndex++) {
            CellBackup cellBackup = cellBackups.get(cellIndex);
            if (cellBackup == null) {
                assert cellBounds[cellIndex] == null;
                continue;
            }
//            assert cellBounds.get(cellIndex) != null;
            checkUsedLibs(cellBackup.usedLibs);
            CellId cellId = cellBackup.d.cellId;
            for (int i = 0; i < cellBackup.cellUsages.length; i++) {
                CellBackup.CellUsageInfo cui = cellBackup.cellUsages[i];
                if (cui == null) continue;
                CellUsage u = cellId.getUsageIn(i);
                int subCellIndex = u.protoId.cellIndex;
                cui.checkUsage(cellBackups.get(subCellIndex));
            }
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
            ImmutableCell d = cellBackup.d;
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
