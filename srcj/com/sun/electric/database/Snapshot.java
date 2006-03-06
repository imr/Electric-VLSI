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

import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 */
public class Snapshot {
    private static final AtomicInteger snapshotCount = new AtomicInteger();
    public static final Snapshot EMPTY = new Snapshot(0, CellBackup.NULL_ARRAY, new int[0], ERectangle.NULL_ARRAY, LibraryBackup.NULL_ARRAY);
    
    public final int snapshotId;
    public final CellBackup[] cellBackups;
    public final int[] cellGroups;
    public final ERectangle[] cellBounds;
    public final LibraryBackup[] libBackups;

    /** Creates a new instance of Snapshot */
    private Snapshot(int snapshotId, CellBackup[] cellBackups, int[] cellGroups, ERectangle[] cellBounds, LibraryBackup[] libBackups) {
        this.snapshotId = snapshotId;
        this.cellBackups = cellBackups;
        this.cellGroups = cellGroups;
        this.cellBounds = cellBounds;
        this.libBackups = libBackups;
    }
    
    /**
     * Creates a new instance of Snapshot which differs from this Snapshot.
     * Four array parameters are supplied. Each parameter may be null if its contents is the same as in this Snapshot.
     * @param cellBackups array indexed by cellIndex of new CellBackups.
     * @param cellGroups array indexed by cellIndex of cellGroups numbers.
     * @param cellBounds array indexed by cellIndex of cell bounds.
     * @param libBackups array index by cellIndex of LibraryBackups.
     * @return new snapshot which differs froms this Snapshot or this Snapshot.
     * @throws IllegalArgumentException on invariant violation.
     * @throws ArrayOutOfBoundsException on some invariant violations.
     */
    public Snapshot with(CellBackup[] cellBackups, int[] cellGroups, ERectangle[] cellBounds, LibraryBackup[] libBackups) {
        long startTime = System.currentTimeMillis();
        cellBackups = copyArray(cellBackups, this.cellBackups);
        cellGroups = copyArray(cellGroups, this.cellGroups);
        cellBounds = copyArray(cellBounds, this.cellBounds);
        libBackups = copyArray(libBackups, this.libBackups);
        boolean namesChanged = this.cellGroups != cellGroups || this.libBackups != libBackups || this.cellBackups.length != cellBackups.length;
        if (!namesChanged && this.cellBackups == cellBackups && this.cellBounds == cellBounds)
            return this;
        
        // Gather changes
        HashSet<CellUsage> checkUsages = new HashSet<CellUsage>();
        for (int cellIndex = 0; cellIndex < cellBackups.length; cellIndex++) {
            CellBackup newBackup = cellBackups[cellIndex];
            CellBackup oldBackup = getCell(cellIndex);
            if (newBackup == oldBackup) continue;
            if (newBackup == null) {
                namesChanged = true;
                continue;
            }
            if (oldBackup == null || newBackup.d.cellName != oldBackup.d.cellName || newBackup.isMainSchematics != oldBackup.isMainSchematics)
                namesChanged = true;
            
            // Gather CellUsages to check.
            CellId cellId = newBackup.d.cellId;
            for (int i = 0; i < newBackup.exportUsages.length; i++) {
                BitSet bs = newBackup.exportUsages[i];
                if (bs == null) continue;
                if (oldBackup != null && i < oldBackup.exportUsages.length && bs == oldBackup.exportUsages[i]) continue;
                checkUsages.add(cellId.getUsageIn(i));
            }
            if (oldBackup == null || newBackup.definedExports != oldBackup.definedExports) {
                for (int i = 0, numUsages = cellId.numUsagesOf(); i < numUsages; i++) {
                    CellUsage u = cellId.getUsageOf(i);
                    int protoCellIndex = u.protoId.cellIndex;
                    if (protoCellIndex < cellBackups.length) continue;
                    CellBackup protoBackup = cellBackups[protoCellIndex];
                    if (protoBackup == null) continue;
                    if (u.indexInParent < protoBackup.exportUsages.length && protoBackup.exportUsages[u.indexInParent] != null)
                        checkUsages.add(u);
                }
            }
        }
        
        // Check usages
        for (CellUsage u: checkUsages) {
            CellBackup parentBackup = cellBackups[u.parentId.cellIndex];
            CellBackup protoBackup = cellBackups[u.protoId.cellIndex];
            BitSet bs = (BitSet)parentBackup.exportUsages[u.indexInParent].clone();
            bs.andNot(protoBackup.definedExports);
            if (!bs.isEmpty())
                throw new IllegalArgumentException("exportUsages");
        }
        
        // Check names
        if (namesChanged)
            checkNames(cellBackups, cellGroups, libBackups);
        
        Snapshot snapshot = new Snapshot(snapshotCount.incrementAndGet(), cellBackups, cellGroups, cellBounds, libBackups);
        long endTime = System.currentTimeMillis();
        System.out.println("Creating snapshot took: " + (endTime - startTime) + " msec");
        return snapshot;
    }
    
    private static <T> T[] copyArray(T[] newArray, T[] oldArray) {
        if (newArray == null) return oldArray;
        int l;
        for (l = newArray.length; l > 0 && newArray[l - 1] == null; l--);
        if (l == oldArray.length) {
            int i = 0;
            while (i < oldArray.length && newArray[i] == oldArray[i]) i++;
            if (i == l) return oldArray;
        }
        T[] copyArray = (T[])Array.newInstance(oldArray.getClass().getComponentType(), l);
        System.arraycopy(newArray, 0, copyArray, 0, l);
        if (l > 0 && copyArray[l - 1] == null)
            throw new ConcurrentModificationException();
        return copyArray;
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
        if (oldSnapshot == null) oldSnapshot = Snapshot.EMPTY;
        List<LibId> changed = null;
        if (oldSnapshot.libBackups != libBackups) {
            int numLibs = Math.max(oldSnapshot.libBackups.length, libBackups.length);
            for (int i = 0; i < numLibs; i++) {
                LibraryBackup oldBackup = oldSnapshot.getLib(i);
                LibraryBackup newBackup = getLib(i);
                if (oldBackup == newBackup) continue;
                if (changed == null) changed = new ArrayList<LibId>();
                changed.add(LibId.getByIndex(i));
            }
        }
        if (changed == null) changed = Collections.emptyList();
        return changed;
    }
    
    public List<CellId> getChangedCells(Snapshot oldSnapshot) {
        if (oldSnapshot == null) oldSnapshot = Snapshot.EMPTY;
        List<CellId> changed = null;
        int numCells = Math.max(oldSnapshot.cellBackups.length, cellBackups.length);
        for (int i = 0; i < numCells; i++) {
            CellBackup oldBackup = oldSnapshot.getCell(i);
            CellBackup newBackup = getCell(i);
            if (oldBackup == newBackup) continue;
            if (changed == null) changed = new ArrayList<CellId>();
            changed.add(CellId.getByIndex(i));
        }
        if (changed == null) changed = Collections.emptyList();
        return changed;
    }
    
    public CellBackup getCell(CellId cellId) {
        int cellIndex = cellId.cellIndex;
        return cellIndex < cellBackups.length ? cellBackups[cellIndex] : null; 
    }
    
    public CellBackup getCell(int cellIndex) {
        return cellIndex < cellBackups.length ? cellBackups[cellIndex] : null; 
    }
    
    public ERectangle getCellBounds(CellId cellId) {
        int cellIndex = cellId.cellIndex;
        return cellIndex < cellBounds.length ? cellBounds[cellIndex] : null; 
    }
    
    public ERectangle getCellBounds(int cellIndex) {
        return cellIndex < cellBounds.length ? cellBounds[cellIndex] : null; 
    }
    
    public LibraryBackup getLib(LibId libId) {
        int libIndex = libId.libIndex;
        return libIndex < libBackups.length ? libBackups[libIndex] : null; 
    }
    
    private LibraryBackup getLib(int libIndex) {
        return libIndex < libBackups.length ? libBackups[libIndex] : null; 
    }
    
    private boolean equals(Snapshot that) {
        return Arrays.equals(this.cellBackups, that.cellBackups) &&
                Arrays.equals(this.libBackups, that.libBackups) &&
                Arrays.equals(this.cellGroups, that.cellGroups) &&
                Arrays.equals(this.cellBounds, that.cellBounds);
    }
    
    public void writeDiffs(SnapshotWriter writer, Snapshot oldSnapshot) throws IOException {
        writer.out.writeInt(snapshotId);
        boolean libsChanged = oldSnapshot.libBackups != libBackups;
        writer.out.writeBoolean(libsChanged);
        if (libsChanged) {
            writer.out.writeInt(libBackups.length);
            for (int i = 0; i < libBackups.length; i++) {
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

        writer.out.writeInt(cellBackups.length);
        boolean boundsChanged = oldSnapshot.cellBounds != cellBounds;
        writer.out.writeBoolean(boundsChanged);
        for (int i = 0; i < cellBackups.length; i++) {
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
            for (int i = 0; i < cellBackups.length; i++) {
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
        LibraryBackup[] libBackups = oldSnapshot.libBackups;
        boolean libsChanged = reader.in.readBoolean();
        if (libsChanged) {
            int libLen = reader.in.readInt();
            libBackups = new LibraryBackup[libLen];
            System.arraycopy(oldSnapshot.libBackups, 0, libBackups, 0, Math.min(oldSnapshot.libBackups.length, libLen));
            for (;;) {
                int libIndex = reader.in.readInt();
                if (libIndex == Integer.MAX_VALUE) break;
                if (libIndex >= 0) {
                    LibraryBackup newBackup = LibraryBackup.read(reader);
                    libBackups[libIndex] = newBackup;
                } else {
                    libIndex = ~libIndex;
                    assert libBackups[libIndex] != null;
                    libBackups[libIndex] = null;
                }
            }
        }

        int cellLen = reader.in.readInt();
        CellBackup[] cellBackups = new CellBackup[cellLen];
        System.arraycopy(oldSnapshot.cellBackups, 0, cellBackups, 0, Math.min(oldSnapshot.cellBackups.length, cellLen));
        boolean boundsChanged = reader.in.readBoolean();
        ERectangle[] cellBounds = oldSnapshot.cellBounds;
        if (boundsChanged) {
            cellBounds = new ERectangle[cellLen];
            System.arraycopy(oldSnapshot.cellBounds, 0, cellBounds, 0, Math.min(oldSnapshot.cellBounds.length, cellLen));
        }
        for (;;) {
            int cellIndex = reader.in.readInt();
            if (cellIndex == Integer.MAX_VALUE) break;
            if (cellIndex >= 0) {
                CellBackup newBackup = CellBackup.read(reader);
                cellBackups[cellIndex] = newBackup;
            } else {
                cellIndex = ~cellIndex;
                assert cellBackups[cellIndex] != null;
                cellBackups[cellIndex] = null;
                assert boundsChanged;
                assert cellBounds[cellIndex] != null;
                cellBounds[cellIndex] = null;
            }
        }
        
        if (boundsChanged) {
            for (;;) {
                int cellIndex = reader.in.readInt();
                if (cellIndex == Integer.MAX_VALUE) break;
                double x = reader.in.readDouble();
                double y = reader.in.readDouble();
                double width = reader.in.readDouble();
                double height = reader.in.readDouble();
                ERectangle newBounds = new ERectangle(x, y, width, height);
                cellBounds[cellIndex] = newBounds;
            }
        }
        
        int[] cellGroups = oldSnapshot.cellGroups;;
        boolean cellGroupsChanged = reader.in.readBoolean();
        if (cellGroupsChanged) {
            int cellGroupsLength = reader.in.readInt();
            cellGroups = new int[cellGroupsLength];
            for (int i = 0; i < cellGroups.length; i++)
                cellGroups[i] = reader.in.readInt();
        }
        for (int i = 0; i < cellBackups.length; i++) {
            assert (cellBackups[i] != null) == (cellBounds[i] != null);
            assert (cellBackups[i] != null) == (cellGroups[i] >= 0);
        }
        return new Snapshot(snapshotId, cellBackups, cellGroups, cellBounds, libBackups);
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
            if (libBackup != null) libBackup.check();
        }
        for (CellBackup cellBackup: cellBackups) {
            if (cellBackup != null) cellBackup.check();
        }
        if (libBackups.length > 0)
            assert libBackups[libBackups.length - 1] != null;
        if (cellBackups.length > 0)
            assert cellBackups[cellBackups.length - 1] != null;
        checkNames(cellBackups, cellGroups, libBackups);
        assert cellBackups.length == cellBounds.length;
        for (int cellIndex = 0; cellIndex < cellBackups.length; cellIndex++) {
            CellBackup cellBackup = cellBackups[cellIndex];
            if (cellBackup == null) {
                assert cellBounds[cellIndex] == null;
                continue;
            }
            assert cellBounds[cellIndex] != null;
            CellId cellId = cellBackup.d.cellId;
            for (int i = 0; i < cellBackup.cellUsages.length; i++) {
                if (cellBackup.cellUsages[i] == 0) continue;
                CellUsage u = cellId.getUsageIn(i);
                int subCellIndex = u.protoId.cellIndex;
                assert cellBackups[subCellIndex] != null;
                BitSet exportUsage = cellBackup.exportUsages[i];
                if (exportUsage != null) {
                    BitSet bs = (BitSet)exportUsage.clone();
                    bs.andNot(cellBackups[subCellIndex].definedExports);
                    assert bs.isEmpty();
                }
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
    private static void checkNames(CellBackup[] cellBackups, int[] cellGroups, LibraryBackup[] libBackups) {
        HashSet<String> libNames = new HashSet<String>();
        ArrayList<HashMap<String,Integer>> protoNameToGroup = new ArrayList<HashMap<String,Integer>>();
        ArrayList<HashSet<CellName>> cellNames = new ArrayList<HashSet<CellName>>();
        for (int libIndex = 0; libIndex < libBackups.length; libIndex++) {
            LibraryBackup libBackup = libBackups[libIndex];
            if (libBackup == null) {
                protoNameToGroup.add(null);
                cellNames.add(null);
                continue;
            }
            protoNameToGroup.add(new HashMap<String,Integer>());
            cellNames.add(new HashSet<CellName>());
            if (libBackup.d.libId != LibId.getByIndex(libIndex))
                throw new IllegalArgumentException("LibId");
            String libName = libBackup.d.libName;
            if (!libNames.add(libName))
                throw new IllegalArgumentException("duplicate libName");
            for (LibId libId: libBackup.referencedLibs) {
                if (libId != libBackups[libId.libIndex].d.libId)
                    throw new IllegalArgumentException("LibId in referencedLibs");
            }
        }
        assert protoNameToGroup.size() == libBackups.length && cellNames.size() == libBackups.length;
        
        if (cellBackups.length != cellGroups.length)
            throw new IllegalArgumentException("cellGroups.length");
        ArrayList<LibId> groupLibs = new ArrayList<LibId>();
        BitSet mainSchematicsFoundInGroup = new BitSet();
        for (int cellIndex = 0; cellIndex < cellBackups.length; cellIndex++) {
            CellBackup cellBackup = cellBackups[cellIndex];
            if (cellBackup == null) {
                if (cellGroups[cellIndex] != -1)
                    throw new IllegalArgumentException("cellGroups");
                continue;
            }
            ImmutableCell d = cellBackup.d;
            CellId cellId = d.cellId;
            if (cellId != CellId.getByIndex(cellIndex))
                throw new IllegalArgumentException("CellId");
            LibId libId = d.libId;
            int libIndex = libId.libIndex;
            if (libId != libBackups[libIndex].d.libId)
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
