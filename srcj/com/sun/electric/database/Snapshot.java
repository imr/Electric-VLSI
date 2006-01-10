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
import com.sun.electric.tool.user.ActivityLogger;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;

/**
 *
 */
public class Snapshot {
    
    public final ArrayList<CellBackup> cellBackups = new ArrayList<CellBackup>();
    public int[] cellGroups;
    public final ArrayList<ERectangle> cellBounds = new ArrayList<ERectangle>();
    public final ArrayList<LibraryBackup> libBackups = new ArrayList<LibraryBackup>();

    public static Snapshot currentSnapshot = new Snapshot();
    public static volatile SnapshotWriter writer = null;
    
    /** Creates a new instance of Snapshot */
    public Snapshot() {
        cellGroups = new int[0];
    }
    
    private Snapshot(Snapshot oldSnapshot) {
        for (Iterator<Library> lit = Library.getLibraries(); lit.hasNext(); ) {
            Library lib = lit.next();
            LibraryBackup oldLibBackup = oldSnapshot.getLib(lib.getId());
            int libIndex = lib.getId().libIndex;
            while (libBackups.size() <= libIndex) libBackups.add(null);
            assert libBackups.get(libIndex) == null;
            libBackups.set(libIndex, lib.backup(oldLibBackup));
            for (Iterator<Cell> cit = lib.getCells(); cit.hasNext(); ) {
                Cell cell = cit.next();
                CellBackup oldBackup = oldSnapshot.getCell((CellId)cell.getId());
                int cellIndex = cell.getCellIndex();
                while (cellBackups.size() <= cellIndex) {
                    cellBackups.add(null);
                    cellBounds.add(null);
                }
                assert cellBackups.get(cellIndex) == null;
                cellBackups.set(cellIndex, cell.backup(oldBackup));
                assert cellBounds.get(cellIndex) == null;
                ERectangle newBounds = cell.getBounds();
                assert newBounds != null;
                cellBounds.set(cellIndex, cell.getBounds());
            }
        }
        HashMap<Cell.CellGroup,Integer> groupNums = new HashMap<Cell.CellGroup,Integer>();
        cellGroups = new int[cellBackups.size()];
        Arrays.fill(cellGroups, -1);
        boolean changed = oldSnapshot == null || oldSnapshot.cellGroups.length != cellGroups.length;
        for (int i = 0; i < cellBackups.size(); i++) {
            CellBackup backup = cellBackups.get(i);
            if (backup == null) continue;
            Cell cell = Cell.inCurrentThread(backup.d.cellId);
            Cell.CellGroup cellGroup = cell.getCellGroup();
            Integer gn = groupNums.get(cellGroup);
            if (gn == null) {
                gn = Integer.valueOf(groupNums.size());
                groupNums.put(cellGroup, gn);
            }
            cellGroups[i] = gn.intValue();
            changed = changed || oldSnapshot.cellGroups[i] != cellGroups[i];
        }
        if (!changed)
            cellGroups = oldSnapshot.cellGroups;
    }
    
    public CellBackup getCell(CellId cellId) {
        int cellIndex = cellId.cellIndex;
        return cellIndex < cellBackups.size() ? cellBackups.get(cellIndex) : null; 
    }
    
    public CellBackup getCell(int cellIndex) {
        return cellIndex < cellBackups.size() ? cellBackups.get(cellIndex) : null; 
    }
    
    public ERectangle getCellBounds(int cellIndex) {
        return cellIndex < cellBounds.size() ? cellBounds.get(cellIndex) : null; 
    }
    
    public LibraryBackup getLib(LibId libId) {
        int libIndex = libId.libIndex;
        return libIndex < libBackups.size() ? libBackups.get(libIndex) : null; 
    }
    
    public LibraryBackup getLib(int libIndex) {
        return libIndex < libBackups.size() ? libBackups.get(libIndex) : null; 
    }
    
    /**
     * Initialize SnapshotWriter to file with given name.
     * @param dumpName file name of dump.
     */
    public static void initWriter(String dumpFile) {
        try {
            writer = new SnapshotWriter(new DataOutputStream(new BufferedOutputStream(new FileOutputStream(dumpFile))));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Initialize SnapshotWriter to socket with given port.
     * @param port port to write.
     */
    public static void initWriter(OutputStream outStream) {
        //try {
            writer = new SnapshotWriter(new DataOutputStream(new BufferedOutputStream(outStream)));
        //} catch (IOException e) {
        //    e.printStackTrace();
        //}
    }
    
    public static void advanceWriter() {
        if (writer == null) return;
        Snapshot oldSnapshot = currentSnapshot;
        Snapshot newSnapshot = new Snapshot(oldSnapshot);
        try {
            newSnapshot.writeDiffs(writer, oldSnapshot);
            writer.out.flush();
        } catch (IOException e) {
            ActivityLogger.logException(e);
        }
        currentSnapshot = newSnapshot;
    }

    private void writeDiffs(SnapshotWriter writer, Snapshot oldSnapshot) throws IOException {
        int numLibs = Math.max(oldSnapshot.libBackups.size(), libBackups.size());
        for (int i = 0; i < numLibs; i++) {
            LibraryBackup oldBackup = oldSnapshot.getLib(i);
            LibraryBackup newBackup = getLib(i);
            if (oldBackup == newBackup) continue;
            if (oldBackup == null) {
                System.out.println("Created library " + i + " " + newBackup.d.libName);
                writer.out.writeInt(i);
                newBackup.write(writer);
            } else if (newBackup == null) {
                System.out.println("Killed library " + i + " " + oldBackup.d.libName);
                writer.out.writeInt(~i);
            } else {
                System.out.print("Modified library " + i + " " + oldBackup.d.libName);
                if (newBackup.d.libName != oldBackup.d.libName)
                    System.out.print(" -> " + newBackup.d.libName);
                System.out.println();
                writer.out.writeInt(i);
                newBackup.write(writer);
            }
        }
        writer.out.writeInt(Integer.MAX_VALUE);

        int numCells = Math.max(oldSnapshot.cellBackups.size(), cellBackups.size());
        for (int i = 0; i < numCells; i++) {
            CellBackup oldBackup = oldSnapshot.getCell(i);
            CellBackup newBackup = getCell(i);
            if (oldBackup == newBackup) continue;
            if (oldBackup == null) {
                System.out.println("Created cell " + i + " " + newBackup.cellName);
                writer.out.writeInt(i);
                newBackup.write(writer);
            } else if (newBackup == null) {
                System.out.println("Killed cell " + i + " " + oldBackup.cellName);
                writer.out.writeInt(~i);
            } else {
                System.out.print("Modified cell " + i + " " + oldBackup.cellName);
                if (newBackup.cellName != oldBackup.cellName)
                    System.out.print(" -> " + newBackup.cellName);
                System.out.println();
                writer.out.writeInt(i);
                newBackup.write(writer);
            }
        }
        writer.out.writeInt(Integer.MAX_VALUE);
        
        for (int i = 0; i < numCells; i++) {
            CellBackup newBackup = getCell(i);
            if (newBackup == null) continue;
            ERectangle oldBounds = oldSnapshot.getCellBounds(i);
            ERectangle newBounds = getCellBounds(i);
            assert newBounds != null;
            if (oldBounds != newBounds) {
                System.out.println("New cell bounds " + i + " " + newBounds);
                writer.out.writeInt(i);
                writer.out.writeDouble(newBounds.getX());
                writer.out.writeDouble(newBounds.getY());
                writer.out.writeDouble(newBounds.getWidth());
                writer.out.writeDouble(newBounds.getHeight());
            }
        }
        writer.out.writeInt(Integer.MAX_VALUE);
        
        boolean cellGroupsChanged = cellGroups != oldSnapshot.cellGroups;
        writer.out.writeBoolean(cellGroupsChanged);
        if (cellGroupsChanged) {
            System.out.println("Changed cell groups");
            writer.out.writeInt(cellGroups.length);
            for (int i = 0; i < cellGroups.length; i++)
                writer.out.writeInt(cellGroups[i]);
        }
    }
    
    public static Snapshot readSnapshot(SnapshotReader reader, Snapshot oldSnapshot) throws IOException {
        Snapshot newSnapshot = new Snapshot();
        newSnapshot.read(reader, oldSnapshot);
        return newSnapshot;
    }
    
    private void read(SnapshotReader reader, Snapshot oldSnapshot) throws IOException {
        assert libBackups.size() == 0;
        libBackups.addAll(oldSnapshot.libBackups);
        assert libBackups.size() == oldSnapshot.libBackups.size();
        for (;;) {
            int libIndex = reader.in.readInt();
            if (libIndex == Integer.MAX_VALUE) break;
            if (libIndex >= 0) {
                LibraryBackup newBackup = LibraryBackup.read(reader);
                while (libIndex >= libBackups.size()) libBackups.add(null);
                libBackups.set(libIndex, newBackup);
            } else {
                libIndex = ~libIndex;
                LibraryBackup oldBackup = libBackups.set(libIndex, null);
                assert oldBackup != null;
            }
        }
        while (libBackups.size() > 0 && libBackups.get(libBackups.size() - 1) == null)
            libBackups.remove(libBackups.size() - 1);

        assert cellBackups.size() == 0;
        cellBackups.addAll(oldSnapshot.cellBackups);
        cellBounds.addAll(oldSnapshot.cellBounds);
        assert cellBackups.size() == oldSnapshot.cellBackups.size();
        for (;;) {
            int cellIndex = reader.in.readInt();
            if (cellIndex == Integer.MAX_VALUE) break;
            if (cellIndex >= 0) {
                CellBackup newBackup = CellBackup.read(reader);
                while (cellIndex >= cellBackups.size()) cellBackups.add(null);
                cellBackups.set(cellIndex, newBackup);
            } else {
                cellIndex = ~cellIndex;
                CellBackup oldBackup = cellBackups.set(cellIndex, null);
                assert oldBackup != null;
                ERectangle oldBounds = cellBounds.set(cellIndex, null);
                assert oldBounds != null;
            }
        }
        
        for (;;) {
            int cellIndex = reader.in.readInt();
            if (cellIndex == Integer.MAX_VALUE) break;
            double x = reader.in.readDouble();
            double y = reader.in.readDouble();
            double width = reader.in.readDouble();
            double height = reader.in.readDouble();
            ERectangle newBounds = new ERectangle(x, y, width, height);
            while (cellIndex >= cellBounds.size()) cellBounds.add(null);
            cellBounds.set(cellIndex, newBounds);
        }
        while (cellBackups.size() > 0 && cellBackups.get(cellBackups.size() - 1) == null)
            cellBackups.remove(cellBackups.size() - 1);
        while (cellBounds.size() > 0 && cellBounds.get(cellBounds.size() - 1) == null)
            cellBounds.remove(cellBounds.size() - 1);
        assert cellBackups.size() == cellBounds.size();
        for (int i = 0; i < cellBackups.size(); i++)
            assert (getCell(i) != null) == (getCellBounds(i) != null);
        
        boolean cellGroupsChanged = reader.in.readBoolean();
        if (cellGroupsChanged) {
            int cellGroupsLength = reader.in.readInt();
            cellGroups = new int[cellGroupsLength];
            for (int i = 0; i < cellGroups.length; i++)
                cellGroups[i] = reader.in.readInt();
        } else {
            cellGroups = oldSnapshot.cellGroups;
        }
              
    }
}
