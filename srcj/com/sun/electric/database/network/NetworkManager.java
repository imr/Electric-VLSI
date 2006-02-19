/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NetworkTool.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.network;

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellId;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.ErrorHighlight;
import com.sun.electric.tool.user.ErrorLogger;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Iterator;

/**
 *
 */
public class NetworkManager {
    
    /** Database to which this network manager belongs. */ private final EDatabase database;
    /** Database snapshot before undo */            private Snapshot lastSnapshot = new Snapshot();
	/** NetCells. */								private NetCell[] cells = new NetCell[1];

    /** The cell for logging network errors */      private Cell currentErrorCell;
    /** buffer of highlights for next error */      private ArrayList<ErrorHighlight> errorHighlights = new ArrayList<ErrorHighlight>();
    /** list of errors for current cell */          private ArrayList<ErrorLogger.MessageLog> errors = new ArrayList<ErrorLogger.MessageLog>();
    
    /** Creates a new instance of NetworkManager */
	public NetworkManager(EDatabase database) {
        this.database = database;
	}
    
	void setCell(Cell cell, NetCell netCell) {
		int cellIndex = cell.getCellIndex();
		if (cellIndex >= cells.length)
		{
			int newLength = cells.length;
			while (cellIndex >= newLength) newLength *= 2;
			NetCell[] newCells = new NetCell[newLength];
			for (int i = 0; i < cells.length; i++)
				newCells[i] = cells[i];
			cells = newCells;
		}
		cells[cellIndex] = netCell;
	}

	final NetCell getNetCell(Cell cell) { return cells[cell.getCellIndex()]; }

    /**
     * Purge memery occupied by netlists.
     */
    public void purge() {
        lastSnapshot = new Snapshot();
        cells = new NetCell[1];
    }
    
    void advanceSnapshot() {
//        assert Job.canComputeNetlist();
        Snapshot newSnapshot = EDatabase.theDatabase.backup();
        if (newSnapshot == lastSnapshot) return;
        updateAll(lastSnapshot, newSnapshot);
        lastSnapshot = newSnapshot;
    }
    
    /****************************** CHANGE LISTENER ******************************/

    /**
     * Update network information from old immutable snapshot to new immutable snapshot.
     * @param oldSnapshot old immutable snapshot.
     * @param newSnapshot new immutable snapshot.
     */
    private void updateAll(Snapshot oldSnapshot, Snapshot newSnapshot) {
//        invalidate();
        int maxCells = Math.max(oldSnapshot.cellBackups.length, newSnapshot.cellBackups.length);
        if (cells.length < maxCells) {
            NetCell[] newCells = new NetCell[Math.max(cells.length*2, maxCells)];
            System.arraycopy(cells, 0, newCells, 0, cells.length);
            cells = newCells;
        }
        // killed Cells
        for (int i = 0; i < maxCells; i++) {
            CellBackup oldBackup = oldSnapshot.getCell(i);
            CellBackup newBackup = newSnapshot.getCell(i);
            if (newBackup != null || oldBackup == null) continue;
            cells[i] = null;
        }
        // new Cells
        for (int i = 0; i < maxCells; i++) {
            CellBackup oldBackup = oldSnapshot.getCell(i);
            CellBackup newBackup = newSnapshot.getCell(i);
            if (newBackup == null || oldBackup != null) continue;
            Cell cell = CellId.getByIndex(i).inDatabase(database);
            if (cell.isIcon() || cell.isSchematic())
                new NetSchem(cell);
            else
                new NetCell(cell);
        }
        // Changed CellGroups
        if (oldSnapshot.cellGroups != newSnapshot.cellGroups) {
            // Lower Cell changed
            for (int i = 0; i < newSnapshot.cellGroups.length; i++) {
                if (newSnapshot.cellGroups[i] != i) continue;
                if (i < oldSnapshot.cellGroups.length && i == oldSnapshot.cellGroups[i]) continue;
                Cell cell = CellId.getByIndex(i).inDatabase(database);
                NetSchem.updateCellGroup(cell.getCellGroup());
            }
            // Lower Cell same, but some cells deleted
            for (int i = 0; i < oldSnapshot.cellGroups.length; i++) {
                int l = oldSnapshot.cellGroups[i];
                if (l < 0 || l >= newSnapshot.cellGroups.length || newSnapshot.cellGroups[l] != l) continue;
                if (i < newSnapshot.cellGroups.length && newSnapshot.cellGroups[i] == l) continue;
                Cell cell = CellId.getByIndex(l).inDatabase(database);
                NetSchem.updateCellGroup(cell.getCellGroup());
            }
        }
        // Main schematics changed
        for (int i = 0; i < maxCells; i++) {
            CellBackup newBackup = newSnapshot.getCell(i);
            CellBackup oldBackup = oldSnapshot.getCell(i);
            if (newBackup == null || oldBackup == null) continue;
            if (oldBackup.isMainSchematics == newBackup.isMainSchematics) continue;
            Cell cell = CellId.getByIndex(i).inDatabase(database);
            NetSchem.updateCellGroup(cell.getCellGroup());
        }
        // Cell contents changed
        for (int i = 0; i < maxCells; i++) {
            CellBackup oldBackup = oldSnapshot.getCell(i);
            CellBackup newBackup = newSnapshot.getCell(i);
            if (newBackup == null || oldBackup == null) continue;
            if (oldBackup == newBackup) continue;
            Cell cell = CellId.getByIndex(i).inDatabase(database);
            boolean exportsChanged = !newBackup.sameExports(oldBackup);
            if (!exportsChanged) {
                for (int j = 0; j < newBackup.exports.length; j++) {
                    if (newBackup.exports[j].name != oldBackup.exports[j].name)
                        exportsChanged = true;
                }
            }
            NetCell netCell = getNetCell(cell);
            if (exportsChanged)
                netCell.exportsChanged();
            else
                netCell.setNetworksDirty();
        }
 //       redoNetworkNumbering(false);
    }
    
    void startErrorLogging(Cell cell) {
        currentErrorCell = cell;
        errorHighlights.clear();
        errors.clear();
    }

    void pushHighlight(Export e) {
//        assert e.getParent() == currentErrorCell;
        errorHighlights.add(ErrorHighlight.newInstance(e));
    }
    
    void pushHighlight(Geometric geom) {
        assert geom.getParent() == currentErrorCell;
        errorHighlights.add(ErrorHighlight.newInstance(null, geom));
    }
    
    void pushHighlight(PortInst pi) {
        Poly poly = pi.getPoly();
        Point2D [] points = poly.getPoints();
        for(int i=0; i<points.length; i++)
        {
            int prev = i - 1;
            if (i == 0) prev = points.length - 1;
            errorHighlights.add(ErrorHighlight.newInstance(currentErrorCell, points[prev], points[i]));
        }
        
    }
    
    void logError(String message, int sortKey) {
        errors.add(new ErrorLogger.MessageLog(message, currentErrorCell, sortKey, errorHighlights));
        errorHighlights.clear();
    }
    
    void logWarning(String message, int sortKey) {
        errors.add(new ErrorLogger.WarningLog(message, currentErrorCell, sortKey, errorHighlights));
        errorHighlights.clear();
    }
    
    void finishErrorLogging() {
        Job.updateNetworkErrors(currentErrorCell, errors);
        errorHighlights.clear();
        NetworkTool.totalNumErrors += errors.size();
        errors.clear();
    }
}
