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
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.text.CellName;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 *
 */
public class Snapshot {
    
    public final CellBackup[] cellBackups;
    public final int[] cellGroups;
    public final ERectangle[] cellBounds;
    public final LibraryBackup[] libBackups;

    /** Creates a new instance of Snapshot */
    public Snapshot() {
        this(CellBackup.NULL_ARRAY, new int[0], ERectangle.NULL_ARRAY, LibraryBackup.NULL_ARRAY);
    }
    
    public Snapshot(CellBackup[] cellBackups, int[] cellGroups, ERectangle[] cellBounds, LibraryBackup[] libBackups) {
        this.cellBackups = cellBackups;
        this.cellGroups = cellGroups;
        this.cellBounds = cellBounds;
        this.libBackups = libBackups;
        checkTopLevel();
    }
    
    public List<LibId> getChangedLibraries(Snapshot oldSnapshot) {
        if (oldSnapshot == null) oldSnapshot = new Snapshot();
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
        if (oldSnapshot == null) oldSnapshot = new Snapshot();
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
        return new Snapshot(cellBackups, cellGroups, cellBounds, libBackups);
    }
    
    /**
	 * Checks invariant of this Snapshot.
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
        checkTopLevel();
        long endTime = System.currentTimeMillis();
        System.out.println("Checking snapshot invariants deeply took " + (endTime - startTime) + " msec");
    }
    
    /**
	 * Checks invariant of this Snapshot assuming that invariant of LibraryBackups and CellBackups are ok.
	 * @throws AssertionError if invariant is broken.
	 */
    private void checkTopLevel() {
        long startTime = System.currentTimeMillis();
        if (libBackups.length > 0)
            assert libBackups[libBackups.length - 1] != null;
        ArrayList<HashMap<String,Integer>> cellNameToGroup = new ArrayList<HashMap<String,Integer>>();
        ArrayList<HashSet<CellName>> cellNames = new ArrayList<HashSet<CellName>>();
        for (int i = 0; i < libBackups.length; i++) {
            LibraryBackup libBackup = libBackups[i];
            if (libBackup == null) {
                cellNameToGroup.add(null);
                cellNames.add(null);
                continue;
            }
            cellNameToGroup.add(new HashMap<String,Integer>());
            cellNames.add(new HashSet<CellName>());
            
//            libBackup.check();
            assert libBackup.d.libId.libIndex == i;
            for (LibId libId: libBackup.referencedLibs)
                assert libBackups[libId.libIndex] != null;
        }
        assert cellBackups.length == cellGroups.length && cellBackups.length == cellBounds.length;
        if (cellBackups.length > 0)
            assert cellBackups[cellBackups.length - 1] != null;
        ArrayList<BitSet> cellExports = new ArrayList<BitSet>();
        ArrayList<LibId> groupLibs = new ArrayList<LibId>();
        BitSet mainSchematics = new BitSet();
        for (int i = 0; i < cellBackups.length; i++) {
            CellBackup cellBackup = cellBackups[i];
            if (cellBackup == null) {
                assert cellGroups[i] == -1;
                assert cellBounds[i] == null;
                cellExports.add(null);
                continue;
            }
            assert cellBounds != null;
//            cellBackup.check();
            ImmutableCell d = cellBackup.d;
            CellId cellId = d.cellId;
            assert cellId.cellIndex == i;
            LibId libId = d.libId;
            assert libBackups[libId.libIndex] != null;
            int cellGroup = cellGroups[i];
            if (cellGroup >= groupLibs.size()) {
                assert cellGroup == groupLibs.size();
                groupLibs.add(libId);
            } else {
                assert groupLibs.get(cellGroup) == libId;
            }
            HashMap<String,Integer> cellNameToGroupInLibrary = cellNameToGroup.get(libId.libIndex);
            String protoName = d.cellName.getName();
            Integer gn = cellNameToGroupInLibrary.get(protoName);
            if (gn == null)
                cellNameToGroupInLibrary.put(protoName, Integer.valueOf(cellGroup));
            else
                assert gn.intValue() == cellGroup;
            HashSet<CellName> cellNamesInLibrary = cellNames.get(libId.libIndex);
            assert cellNamesInLibrary.add(d.cellName);
                
            if (cellBackup.isMainSchematics) {
                assert !mainSchematics.get(cellGroup);
                mainSchematics.set(cellGroup);
            }

            BitSet exports = new BitSet();
            for (ImmutableExport e: cellBackup.exports)
                exports.set(e.exportId.chronIndex);
            cellExports.add(exports);
            
        }
        for (CellBackup cellBackup: cellBackups) {
            if (cellBackup == null) continue;
            CellId cellId = cellBackup.d.cellId;
            for (int i = 0; i < cellBackup.cellUsages.length; i++) {
                if (cellBackup.cellUsages[i] == 0) continue;
                CellUsage u = cellId.getUsageIn(i);
                int subCellIndex = u.protoId.cellIndex;
                assert cellBackups[subCellIndex] != null;
                BitSet exportUsage = cellBackup.exportUsages[i];
                if (exportUsage != null) {
                    BitSet bs = (BitSet)exportUsage.clone();
                    bs.andNot(cellExports.get(subCellIndex));
                    assert bs.isEmpty();
                }
            }
        }
        long endTime = System.currentTimeMillis();
        System.out.println("Checking snapshot invariants took: " + (endTime - startTime) + " msec");
    }
 }
