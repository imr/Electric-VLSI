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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.tool.user.ActivityLogger;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 */
public class Snapshot {
    
    public ArrayList<CellBackup> cellBackups = new ArrayList<CellBackup>();

    public static Snapshot currentSnapshot = new Snapshot();
    public static SnapshotWriter writer = null;
    
    /** Creates a new instance of Snapshot */
    private Snapshot() {
    }
    
    private Snapshot(Snapshot oldSnapshot) {
         for (Iterator<Library> lit = Library.getLibraries(); lit.hasNext(); ) {
            Library lib = lit.next();
            for (Iterator<Cell> cit = lib.getCells(); cit.hasNext(); ) {
                Cell cell = cit.next();
                CellBackup oldBackup = oldSnapshot.get((CellId)cell.getId());
                int cellIndex = cell.getCellIndex();
                while (cellBackups.size() <= cellIndex) cellBackups.add(null);
                assert cellBackups.get(cellIndex) == null;
                cellBackups.set(cellIndex, cell.backup(oldBackup));
            }
        }
    }

    public CellBackup get(CellId cellId) {
        int cellIndex = cellId.cellIndex;
        return cellIndex < cellBackups.size() ? cellBackups.get(cellIndex) : null; 
    }
    
    private CellBackup get(int cellIndex) {
        return cellIndex < cellBackups.size() ? cellBackups.get(cellIndex) : null; 
    }
    
    /**
     * Initialize SnapshotWriter to file with given name.
     * @param dumpName file name of dump.
     */
    public static void initWriter(String dumpFile) {
        try {
            writer = new SnapshotWriter(new DataOutputStream(new BufferedOutputStream(new FileOutputStream(dumpFile))));
        } catch (IOException e) {
            ActivityLogger.logException(e);
        }
    }
    
//    public static void initReader(String dumpFile) {
//        try {
//            reader = new SnapshotReader(new DataInputStream(new BufferedInputStream(new FileInputStream(dumpFile))));
//        } catch (IOException e) {
//            ActivityLogger.logException(e);
//        }
//        
//    }
    
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
        int numCells = Math.max(oldSnapshot.cellBackups.size(), cellBackups.size());
        for (int i = 0; i < numCells; i++) {
            CellBackup oldBackup = oldSnapshot.get(i);
            CellBackup newBackup = get(i);
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
    }
    
    public static void readDump(String dumpFile) {
        try {
            SnapshotReader reader = new SnapshotReader(new DataInputStream(new BufferedInputStream(new FileInputStream(dumpFile))));
            Snapshot oldSnapshot = new Snapshot();
            for (;;) {
                oldSnapshot = readSnapshot(reader, oldSnapshot);
                System.out.println("END OF SNAPSHOT");
            }
        } catch (IOException e) {
            System.out.println("END OF FILE");
        }
        
    }
    
    public static Snapshot readSnapshot(SnapshotReader reader, Snapshot oldSnapshot) throws IOException {
        Snapshot newSnapshot = new Snapshot();
        newSnapshot.read(reader, oldSnapshot);
        return newSnapshot;
    }
    
    private void read(SnapshotReader reader, Snapshot oldSnapshot) throws IOException {
        assert cellBackups.size() == 0;
        cellBackups.addAll(oldSnapshot.cellBackups);
        assert cellBackups.size() == oldSnapshot.cellBackups.size();
        for (;;) {
            int cellIndex = reader.in.readInt();
            if (cellIndex == Integer.MAX_VALUE) break;
            if (cellIndex >= 0) {
                System.out.println("Cell " + cellIndex);
                CellBackup newBackup = CellBackup.read(reader);
                while (cellIndex >= cellBackups.size()) cellBackups.add(null);
                cellBackups.set(cellIndex, newBackup);
            } else {
                cellIndex = ~cellIndex;
                System.out.println("Kill cell " + cellIndex);
                CellBackup oldBackup = cellBackups.set(cellIndex, null);
                assert oldBackup != null;
            }
        }
        while (cellBackups.size() > 0 && cellBackups.get(cellBackups.size() - 1) == null)
            cellBackups.remove(cellBackups.size() - 1);
    }
}
