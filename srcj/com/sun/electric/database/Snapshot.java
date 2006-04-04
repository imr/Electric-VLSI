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
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.ImmutableArrayList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
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
    public final ImmutableArrayList<CellBackup> cellBackups;
    public final int[] cellGroups;
    public final ImmutableArrayList<ERectangle> cellBounds;
    public final ImmutableArrayList<LibraryBackup> libBackups;

    /** Creates a new instance of Snapshot */
    private Snapshot(IdManager idManager, int snapshotId,
            ImmutableArrayList<CellBackup> cellBackups,
            int[] cellGroups,
            ImmutableArrayList<ERectangle> cellBounds,
            ImmutableArrayList<LibraryBackup> libBackups) {
        this.idManager = idManager;
        this.snapshotId = snapshotId;
        this.cellBackups = cellBackups;
        this.cellGroups = cellGroups;
        this.cellBounds = cellBounds;
        this.libBackups = libBackups;
    }
    
    /**
     * Creates empty snapshot.
     */
    Snapshot(IdManager idManager) {
        this(idManager, 0, CellBackup.EMPTY_LIST, new int[0], ERectangle.EMPTY_LIST, LibraryBackup.EMPTY_LIST);
    }
    
    /**
     * Creates a new instance of Snapshot which differs from this Snapshot.
     * Four array parameters are supplied. Each parameter may be null if its contents is the same as in this Snapshot.
     * @param cellBackups list indexed by cellIndex of new CellBackups.
     * @param cellGroups array indexed by cellIndex of cellGroups numbers.
     * @param cellBounds list indexed by cellIndex of cell bounds.
     * @param libBackups list indexed by cellIndex of LibraryBackups.
     * @return new snapshot which differs froms this Snapshot or this Snapshot.
     * @throws IllegalArgumentException on invariant violation.
     * @throws ArrayOutOfBoundsException on some invariant violations.
     */
    public Snapshot with(CellBackup[] cellBackupsArray, int[] cellGroups, ERectangle[] cellBoundsArray, LibraryBackup[] libBackupsArray) {
//        long startTime = System.currentTimeMillis();
        ImmutableArrayList<CellBackup> cellBackups = copyArray(cellBackupsArray, this.cellBackups);
        cellGroups = copyArray(cellGroups, this.cellGroups);
        ImmutableArrayList<ERectangle> cellBounds = copyArray(cellBoundsArray, this.cellBounds);
        ImmutableArrayList<LibraryBackup> libBackups = copyArray(libBackupsArray, this.libBackups);
        boolean namesChanged = this.cellGroups != cellGroups || this.libBackups != libBackups || this.cellBackups.size() != cellBackups.size();
        if (!namesChanged && this.cellBackups == cellBackups && this.cellBounds == cellBounds)
            return this;
        
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
                if (oldBackup == null ||
                        newBackup.d.libId != oldBackup.d.libId ||
                        newBackup.d.cellName != oldBackup.d.cellName ||
                        newBackup.isMainSchematics != oldBackup.isMainSchematics)
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
        if (namesChanged)
            checkNames(idManager, cellBackups, cellGroups, libBackups);
        
        Snapshot snapshot = new Snapshot(idManager, idManager.newSnapshotId(), cellBackups, cellGroups, cellBounds, libBackups);
//        long endTime = System.currentTimeMillis();
//        System.out.println("Creating snapshot took: " + (endTime - startTime) + " msec");
        return snapshot;
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
    
    public CellBackup getCell(CellId cellId) { return getCell(cellId.cellIndex); }
    
    public CellBackup getCell(int cellIndex) {
        return cellIndex < cellBackups.size() ? cellBackups.get(cellIndex) : null; 
    }
    
    public ERectangle getCellBounds(CellId cellId) { return getCellBounds(cellId.cellIndex); }
    
    public ERectangle getCellBounds(int cellIndex) {
        return cellIndex < cellBounds.size() ? cellBounds.get(cellIndex) : null; 
    }
    
    public LibraryBackup getLib(LibId libId) { return getLib(libId.libIndex); }
    
    private LibraryBackup getLib(int libIndex) {
        return libIndex < libBackups.size() ? libBackups.get(libIndex) : null; 
    }
    
    private boolean equals(Snapshot that) {
        return this.cellBackups.equals(that.cellBackups) &&
                this.libBackups.equals(that.libBackups) &&
                Arrays.equals(this.cellGroups, that.cellGroups) &&
                this.cellBounds.equals(that.cellBounds);
    }
    
    public void writeDiffs(SnapshotWriter writer, Snapshot oldSnapshot) throws IOException {
        writer.out.writeInt(snapshotId);
        boolean libsChanged = oldSnapshot.libBackups != libBackups;
        writer.out.writeBoolean(libsChanged);
        if (libsChanged) {
            writer.out.writeInt(libBackups.size());
            for (int i = 0; i < libBackups.size(); i++) {
                LibraryBackup oldBackup = oldSnapshot.getLib(i);
                LibraryBackup newBackup = getLib(i);
                if (oldBackup == newBackup) continue;
                if (oldBackup == null) {
                    writer.out.writeInt(i);
                    newBackup.write(writer);
                } else if (newBackup == null) {
                    writer.out.writeInt(~i);
                } else {
                    writer.out.writeInt(i);
                    newBackup.write(writer);
                }
            }
            writer.out.writeInt(Integer.MAX_VALUE);
        }

        writer.out.writeInt(cellBackups.size());
        boolean boundsChanged = oldSnapshot.cellBounds != cellBounds;
        writer.out.writeBoolean(boundsChanged);
        for (int i = 0; i < cellBackups.size(); i++) {
            CellBackup oldBackup = oldSnapshot.getCell(i);
            CellBackup newBackup = getCell(i);
            if (oldBackup == newBackup) continue;
            if (oldBackup == null) {
                writer.out.writeInt(i);
                newBackup.write(writer);
            } else if (newBackup == null) {
                writer.out.writeInt(~i);
            } else {
                writer.out.writeInt(i);
                newBackup.write(writer);
            }
        }
        writer.out.writeInt(Integer.MAX_VALUE);
        
        if (boundsChanged) {
            for (int i = 0; i < cellBackups.size(); i++) {
                CellBackup newBackup = getCell(i);
                if (newBackup == null) continue;
                ERectangle oldBounds = oldSnapshot.getCellBounds(i);
                ERectangle newBounds = getCellBounds(i);
                assert newBounds != null;
                if (oldBounds != newBounds) {
                    writer.out.writeInt(i);
                    writer.out.writeDouble(newBounds.getX());
                    writer.out.writeDouble(newBounds.getY());
                    writer.out.writeDouble(newBounds.getWidth());
                    writer.out.writeDouble(newBounds.getHeight());
                }
            }
            writer.out.writeInt(Integer.MAX_VALUE);
        }
        
        boolean cellGroupsChanged = cellGroups != oldSnapshot.cellGroups;
        writer.out.writeBoolean(cellGroupsChanged);
        if (cellGroupsChanged) {
            writer.out.writeInt(cellGroups.length);
            for (int i = 0; i < cellGroups.length; i++)
                writer.out.writeInt(cellGroups[i]);
        }
    }
    
    public static Snapshot readSnapshot(SnapshotReader reader, Snapshot oldSnapshot) throws IOException {
        int snapshotId = reader.in.readInt();
        ImmutableArrayList<LibraryBackup> libBackups = oldSnapshot.libBackups;
        boolean libsChanged = reader.in.readBoolean();
        if (libsChanged) {
            int libLen = reader.in.readInt();
            LibraryBackup[] libBackupsArray = new LibraryBackup[libLen];
            for (int libIndex = 0, numLibs = Math.min(oldSnapshot.libBackups.size(), libLen); libIndex < numLibs; libIndex++)
                libBackupsArray[libIndex] = oldSnapshot.libBackups.get(libIndex);
            for (;;) {
                int libIndex = reader.in.readInt();
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

        int cellLen = reader.in.readInt();
        int cellMax = Math.min(oldSnapshot.cellBackups.size(), cellLen);
        CellBackup[] cellBackupsArray = new CellBackup[cellLen];
        for (int cellIndex = 0; cellIndex < cellMax; cellIndex++)
            cellBackupsArray[cellIndex] = oldSnapshot.cellBackups.get(cellIndex);
        boolean boundsChanged = reader.in.readBoolean();
        ImmutableArrayList<ERectangle> cellBounds = oldSnapshot.cellBounds;
        ERectangle[] cellBoundsArray = null;
        if (boundsChanged) {
            cellBoundsArray = new ERectangle[cellLen];
            for (int cellIndex = 0, numCells = Math.min(oldSnapshot.cellBounds.size(), cellLen); cellIndex < numCells; cellIndex++)
                cellBoundsArray[cellIndex] = oldSnapshot.cellBounds.get(cellIndex);
        }
        for (;;) {
            int cellIndex = reader.in.readInt();
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
                int cellIndex = reader.in.readInt();
                if (cellIndex == Integer.MAX_VALUE) break;
                double x = reader.in.readDouble();
                double y = reader.in.readDouble();
                double width = reader.in.readDouble();
                double height = reader.in.readDouble();
                ERectangle newBounds = new ERectangle(x, y, width, height);
                cellBoundsArray[cellIndex] = newBounds;
            }
            cellBounds = new ImmutableArrayList<ERectangle>(cellBoundsArray);
        }
        
        int[] cellGroups = oldSnapshot.cellGroups;;
        boolean cellGroupsChanged = reader.in.readBoolean();
        if (cellGroupsChanged) {
            int cellGroupsLength = reader.in.readInt();
            cellGroups = new int[cellGroupsLength];
            for (int i = 0; i < cellGroups.length; i++)
                cellGroups[i] = reader.in.readInt();
        }
        for (int i = 0; i < cellBackups.size(); i++) {
            assert (cellBackups.get(i) != null) == (cellBounds.get(i) != null);
            assert (cellBackups.get(i) != null) == (cellGroups[i] >= 0);
        }
        return new Snapshot(oldSnapshot.idManager, snapshotId, cellBackups, cellGroups, cellBounds, libBackups);
    }
    
    /**
	 * Checks invariant of this Snapshot.
     * @throws IllegalArgumentException on invariant violation.
     * @throws ArrayOutOfBoundsException on some invariant violations.
     * @throws AssertionError if invariant is broken.
	 * @throws AssertionError if invariant is broken.
	 */
    public void check() {
        long startTime = System.currentTimeMillis();
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
        checkNames(idManager, cellBackups, cellGroups, libBackups);
        assert cellBackups.size() == cellBounds.size();
        for (int cellIndex = 0; cellIndex < cellBackups.size(); cellIndex++) {
            CellBackup cellBackup = cellBackups.get(cellIndex);
            if (cellBackup == null) {
                assert cellBounds.get(cellIndex) == null;
                continue;
            }
            assert cellBounds.get(cellIndex) != null;
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
        long endTime = System.currentTimeMillis();
        System.out.println("Checking snapshot invariants took: " + (endTime - startTime) + " msec");
    }
    
    /*
     * Checks name consistency of arrays intended for Sbapshot construction.
     * @throws IllegalArgumentException on invariant violation.
     * @throws ArrayOutOfBoundsException on some invariant violations.
     */
    private static void checkNames(IdManager idManager, ImmutableArrayList<CellBackup> cellBackups, int[] cellGroups, ImmutableArrayList<LibraryBackup> libBackups) {
        HashSet<String> libNames = new HashSet<String>();
        ArrayList<HashMap<String,Integer>> protoNameToGroup = new ArrayList<HashMap<String,Integer>>();
        ArrayList<HashSet<CellName>> cellNames = new ArrayList<HashSet<CellName>>();
        for (int libIndex = 0; libIndex < libBackups.size(); libIndex++) {
            LibraryBackup libBackup = libBackups.get(libIndex);
            if (libBackup == null) {
                protoNameToGroup.add(null);
                cellNames.add(null);
                continue;
            }
            protoNameToGroup.add(new HashMap<String,Integer>());
            cellNames.add(new HashSet<CellName>());
            if (libBackup.d.libId != idManager.getLibId(libIndex))
                throw new IllegalArgumentException("LibId");
            String libName = libBackup.d.libName;
            if (!libNames.add(libName))
                throw new IllegalArgumentException("duplicate libName");
            for (LibId libId: libBackup.referencedLibs) {
                if (libId != libBackups.get(libId.libIndex).d.libId)
                    throw new IllegalArgumentException("LibId in referencedLibs");
            }
        }
        assert protoNameToGroup.size() == libBackups.size() && cellNames.size() == libBackups.size();
        
        if (cellBackups.size() != cellGroups.length)
            throw new IllegalArgumentException("cellGroups.length");
        ArrayList<LibId> groupLibs = new ArrayList<LibId>();
        BitSet mainSchematicsFoundInGroup = new BitSet();
        for (int cellIndex = 0; cellIndex < cellBackups.size(); cellIndex++) {
            CellBackup cellBackup = cellBackups.get(cellIndex);
            if (cellBackup == null) {
                if (cellGroups[cellIndex] != -1)
                    throw new IllegalArgumentException("cellGroups");
                continue;
            }
            ImmutableCell d = cellBackup.d;
            CellId cellId = d.cellId;
            if (cellId != idManager.getCellId(cellIndex))
                throw new IllegalArgumentException("CellId");
            LibId libId = d.libId;
            int libIndex = libId.libIndex;
            if (libId != libBackups.get(libIndex).d.libId)
                throw new IllegalArgumentException("LibId in ImmutableCell");
            int cellGroup = cellGroups[cellIndex];
            if (cellGroup == groupLibs.size())
                groupLibs.add(libId);
            else if (groupLibs.get(cellGroup) != libId)
                throw new IllegalArgumentException("cellGroups mix of libraries");
            HashMap<String,Integer> cellNameToGroupInLibrary = protoNameToGroup.get(libId.libIndex);
            String protoName = d.cellName.getName();
            Integer gn = cellNameToGroupInLibrary.get(protoName);
            if (gn == null)
                cellNameToGroupInLibrary.put(protoName, Integer.valueOf(cellGroup));
            else if (gn.intValue() != cellGroup)
                throw new IllegalArgumentException("cells with same proto name in different groups");
            HashSet<CellName> cellNamesInLibrary = cellNames.get(libId.libIndex);
            if (!cellNamesInLibrary.add(d.cellName))
                throw new IllegalArgumentException("duplicate CellName in library");
            
            if (cellBackup.isMainSchematics) {
                if (mainSchematicsFoundInGroup.get(cellGroup))
                    throw new IllegalArgumentException("second main schematics in group");
                mainSchematicsFoundInGroup.set(cellGroup);
            }
        }
    }
}
