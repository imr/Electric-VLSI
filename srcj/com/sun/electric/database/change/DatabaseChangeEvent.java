/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DatabaseChangeEvent.java
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.change;

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellId;
import com.sun.electric.database.ImmutableCell;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.variable.ElectricObject;

import java.util.Iterator;

/**
 * A semantic event which indicates that Electric database changed its state.
 */
public class DatabaseChangeEvent {
    public final Snapshot oldSnapshot;
    public final Snapshot newSnapshot;

    public DatabaseChangeEvent(Snapshot oldSnapshot, Snapshot newSnapshot) {
        this.oldSnapshot = oldSnapshot;
        this.newSnapshot = newSnapshot;
    }
        
    /**
     * Returns true if ElectricObject eObj was created, killed or modified
     * in the new database state.
     * @param eObj ElectricObject to test.
     * @return true if the ElectricObject was changed.
     */
    public boolean objectChanged(ElectricObject eObj)
    {
        return true;
//        for (Iterator<Undo.Change> it = batch.getChanges(); it.hasNext(); ) {
//            Undo.Change change = (Undo.Change)it.next();
//            if (change.getObject() == eObj) {
//                return true;
//            }
//        }
//        return false;
    }

    /**
     * Returns true if cell explorer tree was changed
     * in the new database state.
     * @return true if cell explorer tree was changed.
     */
    public boolean cellTreeChanged() {
        if (!newSnapshot.getChangedLibraries(oldSnapshot).isEmpty()) return true;
        for (CellId cellId: newSnapshot.getChangedCells(oldSnapshot)) {
            CellBackup oldBackup = oldSnapshot.getCell(cellId);
            CellBackup newBackup = newSnapshot.getCell(cellId);
            if (oldBackup == null || newBackup == null) return true;
            if (oldBackup.modified != newBackup.modified) return true;
            ImmutableCell oldD = oldBackup.d;
            ImmutableCell newD = newBackup.d;
            if (oldD.libId != newD.libId) return true;
            if (oldD.cellName != newD.cellName) return true;
            if (oldD.groupName != newD.groupName) return true;
            if (oldD.getVar(Cell.MULTIPAGE_COUNT_KEY) != newD.getVar(Cell.MULTIPAGE_COUNT_KEY)) return true;
        }
        return false;
//        for (Iterator<Undo.Change> it = batch.getChanges(); it.hasNext(); ) {
//            Undo.Change change = (Undo.Change)it.next();
//            if (change.getType() == Undo.Type.LIBRARYKILL ||
//                    change.getType() == Undo.Type.LIBRARYNEW ||
//                    change.getType() == Undo.Type.CELLKILL ||
//                    change.getType() == Undo.Type.CELLNEW ||
//                    change.getType() == Undo.Type.CELLGROUPMOD ||
//                    (change.getType() == Undo.Type.OBJECTRENAME && change.getObject() instanceof Library)) {
//                return true;
//            }
//            if (change.getType() == Undo.Type.VARIABLESMOD && change.getObject() instanceof Cell) {
//                ImmutableCell oldD = (ImmutableCell)change.getO1();
//                ImmutableCell newD = (ImmutableCell)change.getObject().getImmutable();
//                return oldD.cellName != newD.cellName ||
//                        oldD.getVar(Cell.MULTIPAGE_COUNT_KEY) != newD.getVar(Cell.MULTIPAGE_COUNT_KEY);
//            }
//        }
//        return false;
    }
}
