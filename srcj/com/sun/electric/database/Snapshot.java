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
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 */
public class Snapshot {
    
    public ArrayList<CellBackup> cellBackups = new ArrayList<CellBackup>();

    public static Snapshot currentSnapshot = new Snapshot();
    
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
    
    public static void advance() {
        Snapshot oldSnapshot = currentSnapshot;
        Snapshot newSnapshot = new Snapshot(oldSnapshot);
        newSnapshot.showDiffs(oldSnapshot);
        currentSnapshot = newSnapshot;
    }

    private void showDiffs(Snapshot oldSnapshot) {
        int numCells = Math.max(oldSnapshot.cellBackups.size(), cellBackups.size());
        for (int i = 0; i < numCells; i++) {
            CellBackup oldBackup = oldSnapshot.get(i);
            CellBackup newBackup = get(i);
            if (oldBackup == newBackup) continue;
            if (oldBackup == null)
                System.out.println("Created cell " + i + " " + newBackup.cellName);
            else if (newBackup == null)
                System.out.println("Killed cell " + i + " " + oldBackup.cellName);
            else {
                System.out.print("Modified cell " + i + " " + oldBackup.cellName);
                if (newBackup.cellName != oldBackup.cellName)
                    System.out.print(" -> " + newBackup.cellName);
                System.out.println();
            }
        }
    }
}
