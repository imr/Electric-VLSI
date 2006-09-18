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
import com.sun.electric.tool.user.ErrorHighlight;
import com.sun.electric.tool.user.ErrorLogger;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

/**
 *
 */
public class NetworkManager {
    
    /** Database to which this network manager belongs. */ private final EDatabase database;
    /** Database snapshot before undo */            private Snapshot lastSnapshot;
	/** NetCells. */								private NetCell[] cells = new NetCell[1];

    /** The cell for logging network errors */      private Cell currentErrorCell;
    /** buffer of highlights for next error */      private ArrayList<ErrorHighlight> errorHighlights = new ArrayList<ErrorHighlight>();
    /** list of errors for current cell */          private ArrayList<ErrorLogger.MessageLog> errors = new ArrayList<ErrorLogger.MessageLog>();
    
    /** Creates a new instance of NetworkManager */
	public NetworkManager(EDatabase database) {
        this.database = database;
        lastSnapshot = database.getInitialSnapshot();
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

    void redoNetworkNumbering(boolean reload)
    {
		// Check that we are in changing thread
		assert EDatabase.theDatabase.canComputeNetlist();

        long startTime = System.currentTimeMillis();
		if (reload) {
			lastSnapshot = database.getInitialSnapshot();
            cells = new NetCell[1];
		}
        advanceSnapshot();
        int ncell = 0;
        for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
        {
            Library lib = it.next();
            // Handling clipboard case (one type of hidden libraries)
            if (lib.isHidden()) continue;

            for(Iterator<Cell> cit = lib.getCells(); cit.hasNext(); )
            {
                Cell cell = (Cell)cit.next();
                ncell++;
                cell.getNetlist(false);
            }
        }
        long endTime = System.currentTimeMillis();
        float finalTime = (endTime - startTime) / 1000F;
		if (ncell != 0 && reload && NetworkTool.showInfo)
			System.out.println("**** Renumber networks of " + ncell + " cells took " + finalTime + " seconds");

		synchronized(NetworkTool.mutex) {
			NetworkTool.networksValid = true;
			NetworkTool.mutex.notify();
		}
    }

	private void invalidate() {
		// Check that we are in changing thread
		assert EDatabase.theDatabase.canComputeNetlist();

		if (!NetworkTool.networksValid)
			return;
		synchronized(NetworkTool.mutex) {
			NetworkTool.networksValid = false;
		}
	}
    
    void advanceSnapshot() {
        assert EDatabase.theDatabase.canComputeNetlist();
        Snapshot newSnapshot = EDatabase.theDatabase.backup();
        if (newSnapshot == lastSnapshot) return;
        assert !NetworkTool.networksValid;
        updateAll(lastSnapshot, newSnapshot);
        lastSnapshot = newSnapshot;
    }
    
    /****************************** CHANGE LISTENER ******************************/

	public void startBatch()
	{
        invalidate();
		if (!NetworkTool.debug) return;
		System.out.println("NetworkTool.startBatch()");
	}

   /**
     * Method to annonunce database changes of a Job.
     */
    public void endBatch()
	{
		try {
            redoNetworkNumbering(false);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.err.println("Full Network renumbering after crash.");
			e.printStackTrace(System.out);
			System.out.println("Full Network renumbering after crash.");
			redoNetworkNumbering(true);
		}
		if (!NetworkTool.debug) return;
		System.out.println("NetworkTool.endBatch()");
	}

    /**
     * Update network information from old immutable snapshot to new immutable snapshot.
     * @param oldSnapshot old immutable snapshot.
     * @param newSnapshot new immutable snapshot.
     */
    private void updateAll(Snapshot oldSnapshot, Snapshot newSnapshot) {
        invalidate();
        int maxCells = Math.max(oldSnapshot.cellBackups.size(), newSnapshot.cellBackups.size());
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
            CellId cellId = newBackup.d.cellId;
            Cell cell = database.getCell(cellId);
            if (cell.isIcon() || cell.isSchematic())
                new NetSchem(cell);
            else
                new NetCell(cell);
        }
        // Changed CellGroups
        if (oldSnapshot.cellGroups != newSnapshot.cellGroups) {
            int maxOldGroupIndex = -1;
            for (int oldGroupIndex: oldSnapshot.cellGroups)
                maxOldGroupIndex = Math.max(maxOldGroupIndex, oldGroupIndex);
            int[] oldGroupMap = new int[maxOldGroupIndex + 1];
            Arrays.fill(oldGroupMap, -2);
            for (int cellIndex = 0; cellIndex < oldSnapshot.cellGroups.length; cellIndex++) {
                int oldGroupIndex = oldSnapshot.cellGroups[cellIndex];
                if (oldGroupIndex < 0) continue;
                int newGroupIndex = cellIndex < newSnapshot.cellGroups.length ? newSnapshot.cellGroups[cellIndex] : -1;
                if (oldGroupMap[oldGroupIndex] == -2)
                    oldGroupMap[oldGroupIndex] = newGroupIndex;
                else if (oldGroupMap[oldGroupIndex] != newGroupIndex)
                    oldGroupMap[oldGroupIndex] = -3;
            }
            for (int oldGroupIndex: oldGroupMap)
                assert oldGroupIndex != -2;
            // oldGroupMap[oldGroupIndex] == newGroupIndex >= 0 ==> oldGroupIndex is a subset of newGroupIndex
            // oldGroupMap[oldGroupIndex] == -1 ==> oldGroupIndex contains only killed cells
            // oldGroupMap[oldGroupIndex] == -3 ==> oldGroupIndex contains new cells from different new groups or has both new and old cells
            
            int maxNewGroupIndex = -1;
            for (int newGroupIndex: newSnapshot.cellGroups)
                maxNewGroupIndex = Math.max(maxNewGroupIndex, newGroupIndex);
            int[] newGroupMap = new int[maxNewGroupIndex + 1];
            Arrays.fill(newGroupMap, -2);
            for (int cellIndex = 0; cellIndex < newSnapshot.cellGroups.length; cellIndex++) {
                int newGroupIndex = newSnapshot.cellGroups[cellIndex];
                if (newGroupIndex < 0) continue;
                int oldGroupIndex = cellIndex < oldSnapshot.cellGroups.length ? oldSnapshot.cellGroups[cellIndex] : -1;
                if (newGroupMap[newGroupIndex] == -2)
                    newGroupMap[newGroupIndex] = oldGroupIndex;
                else if (newGroupMap[newGroupIndex] != oldGroupIndex)
                    newGroupMap[newGroupIndex] = -3;
            }
            for (int newGroupIndex: newGroupMap)
                assert newGroupIndex != -2;
            // newGroupMap[newGroupIndex] == oldGroupIndex >= 0 ==> newGroupIndex is a subset of oldGroupIndex
            // newGroupMap[newGroupIndex] == -1 ==> newGroupIndex contains only new cells
            // newGroupMap[newGroupIndex] == -3 ==> newGroupIndex contains old cells from different old groups or has both new and old cells
            
            for (int cellIndex = 0; cellIndex < newSnapshot.cellGroups.length; cellIndex++) {
                int newGroupIndex = newSnapshot.cellGroups[cellIndex];
                if (newGroupIndex == -1) continue;
                int oldGroupIndex = newGroupMap[newGroupIndex];
                if (oldGroupIndex == -1) continue;
                if (oldGroupIndex >= 0 && oldGroupMap[oldGroupIndex] == newGroupIndex) continue;
                CellId cellId = newSnapshot.getCell(cellIndex).d.cellId;
                Cell cell = database.getCell(cellId);
                NetSchem.updateCellGroup(cell.getCellGroup());
                newGroupMap[newGroupIndex] = -1;
            }
        }
        // Cell contents changed
        for (int i = 0; i < maxCells; i++) {
            CellBackup oldBackup = oldSnapshot.getCell(i);
            CellBackup newBackup = newSnapshot.getCell(i);
            if (newBackup == null || oldBackup == null) continue;
            if (oldBackup == newBackup) continue;
            CellId cellId = newBackup.d.cellId;
            Cell cell = database.getCell(cellId);
            boolean exportsChanged = !newBackup.sameExports(oldBackup);
            if (!exportsChanged) {
                for (int j = 0; j < newBackup.exports.size(); j++) {
                    if (newBackup.exports.get(j).name != oldBackup.exports.get(j).name)
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
