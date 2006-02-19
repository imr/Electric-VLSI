/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Library.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.hierarchy;

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellId;
import com.sun.electric.database.CellUsage;
import com.sun.electric.database.LibId;
import com.sun.electric.database.LibraryBackup;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.network.NetworkManager;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.user.ActivityLogger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Electric run-time database is a graph of ElectricObjects.
 */
public class EDatabase {
    public static EDatabase theDatabase = new EDatabase();
    public static EDatabase serverDatabase() { return theDatabase; }
    public static EDatabase clientDatabase() { return theDatabase; }
    
	/** list of linked libraries indexed by libId. */           private final ArrayList<Library> linkedLibs = new ArrayList<Library>();
	/** map of libraries sorted by name */                      final TreeMap<String,Library> libraries = new TreeMap<String,Library>(TextUtils.STRING_NUMBER_ORDER);
	/** static list of all linked cells indexed by CellId. */	final ArrayList<Cell> linkedCells = new ArrayList<Cell>();
    /** Last snapshot */                                        private Snapshot snapshot = new Snapshot();
    /** True if database matches snapshot. */                   private boolean snapshotFresh;
	/** Flag set when database invariants failed. */            private boolean invariantsFailed;
    
    /** Network manager for this database. */                   private final NetworkManager networkManager = new NetworkManager(this);
    /** Lock for access to the Database. */                     private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    /** True if database is locked for writing. */              private boolean exclusiveLock;
    
    /** Creates a new instance of EDatabase */
    private EDatabase() {}
    
    public NetworkManager getNetworkManager() { return networkManager; }
    
    public Library getLib(LibId libId) { return getLib(libId.libIndex); }
    
    void addLib(Library lib) {
        int libIndex = lib.getId().libIndex;
        while (libIndex >= linkedLibs.size()) linkedLibs.add(null);
        Library oldLib = linkedLibs.set(libIndex, lib);
        assert oldLib == null;
		libraries.put(lib.getName(), lib);
    }
    
    void removeLib(LibId libId) {
        Library oldLib = linkedLibs.set(libId.libIndex, null);
        libraries.remove(oldLib.getName());
    }
    
    public Cell getCell(CellId cellId) { return getCell(cellId.cellIndex); }
    
    int maxCellIndex() {
        int maxCellIndex = linkedCells.size() - 1;
        while (maxCellIndex >= 0 && linkedCells.get(maxCellIndex) == null) maxCellIndex--;
        return maxCellIndex;
    }
    
    void addCell(Cell cell) {
        int cellIndex = cell.getCellIndex();
        while (cellIndex >= linkedCells.size()) linkedCells.add(null);
        Cell oldCell = linkedCells.set(cellIndex, cell);
        assert oldCell == null;
    }
    
    void removeCell(CellId cellId) {
        Cell oldCell = linkedCells.set(cellId.cellIndex, null);
        assert oldCell != null;
    }
    
    Library getLib(int libIndex) { return libIndex < linkedLibs.size() ? linkedLibs.get(libIndex) : null; }
    
    Cell getCell(int cellIndex) { return cellIndex < linkedCells.size() ? linkedCells.get(cellIndex) : null; } 
    
    /**
     * Locks the database for writing.
     * Lock may be either excluseive (for writing) or shared (for reading).
     * @param exclusive if lock is for writing.
     */
    public void lock(boolean exclusive) {
        Lock lock = exclusive ? readWriteLock.writeLock() : readWriteLock.readLock();
        lock.lock();
        exclusiveLock = exclusive;
    }
    
    /**
     * Unlocks the database which was locked for writing.
     */
    public void unlock() {
        Lock lock = exclusiveLock ? readWriteLock.writeLock() : readWriteLock.readLock();
        lock.unlock();
    }
    
	/**
	 * Get list of cells contained in other libraries
	 * that refer to cells contained in this library
	 * @param elib to search for
	 * @return list of cells refering to elements in this library
	 */
	public Set<Cell> findReferenceInCell(Library elib)
	{
		TreeSet<Cell> set = new TreeSet<Cell>();

		for (Library l : libraries.values())
		{
			// skip itself
			if (l == elib) continue;

			for (Cell cell : l.cells.values())
			{
				cell.findReferenceInCell(elib, set);
			}
		}
		return set;
	}

	/**
	 * Method to find a Library with the specified name.
	 * @param libName the name of the Library.
	 * Note that this is the Library name, and not the Library file.
	 * @return the Library, or null if there is no known Library by that name.
	 */
	public Library findLibrary(String libName)
	{
		if (libName == null) return null;
		Library lib = libraries.get(libName);
		if (lib != null) return lib;

		for (Library l : libraries.values())
		{
			if (l.getName().equalsIgnoreCase(libName))
				return l;
		}
		return null;
	}

	/**
	 * Method to return an iterator over all libraries.
	 * @return an iterator over all libraries.
	 */
	public Iterator<Library> getLibraries()
	{
        synchronized(libraries) {
			ArrayList<Library> librariesCopy = new ArrayList<Library>(libraries.values());
			return librariesCopy.iterator();
        }
	}

	/**
	 * Method to return the number of libraries.
	 * @return the number of libraries.
	 */
	public int getNumLibraries()
	{
		return libraries.size();
	}
    
	/**
	 * Method to return an iterator over all visible libraries.
	 * @return an iterator over all visible libraries.
	 */
	public List<Library> getVisibleLibraries()
	{
        synchronized(libraries) {
			ArrayList<Library> visibleLibraries = new ArrayList<Library>();
			for (Library lib : libraries.values())
			{
				if (!lib.isHidden()) visibleLibraries.add(lib);
			}
			return visibleLibraries;
        }
	}

    void unfreshSnapshot() {
        snapshotFresh = false;
    }
    
   /**
     * Create Snapshot from the current state of Electric database.
     * @return snapshot of the current state of Electric database.
     */
    public Snapshot backup() {
        if (snapshotFresh) return snapshot;
        LibraryBackup[] libBackups = backupLibs(snapshot.libBackups);
        CellBackup[] cellBackups = new CellBackup[maxCellIndex() + 1];
        for (int i = 0; i < cellBackups.length; i++) {
            Cell cell = linkedCells.get(i);
            if (cell != null) cellBackups[i] = cell.backup();
        }
        ERectangle[] cellBounds = new ERectangle[cellBackups.length];
        int[] cellGroups = new int[cellBackups.length];
        Arrays.fill(cellGroups, -1);
        HashMap<Cell.CellGroup,Integer> groupNums = new HashMap<Cell.CellGroup,Integer>();
        boolean cellsChanged = cellBackups.length != snapshot.cellBackups.length;
        boolean cellBoundsChanged = cellsChanged;
        boolean cellGroupsChanged = cellsChanged;
        for (int cellIndex = 0; cellIndex < cellBackups.length; cellIndex++) {
            CellId cellId = CellId.getByIndex(cellIndex);
            Cell cell = Cell.inCurrentThread(cellId);
			if (cell != null) {
				cellBackups[cellIndex] = cell.backup();
                cellBounds[cellIndex] = cell.getBounds();
                
                Cell.CellGroup cellGroup = cell.getCellGroup();
                Integer gn = groupNums.get(cellGroup);
                if (gn == null) {
                    gn = Integer.valueOf(groupNums.size());
                    groupNums.put(cellGroup, gn);
                }
                cellGroups[cellIndex] = gn.intValue();
            }
            cellsChanged = cellsChanged || cellBackups[cellIndex] != snapshot.getCell(cellId);
            cellBoundsChanged = cellBoundsChanged || cellBounds[cellIndex] != snapshot.getCellBounds(cellId);
            cellGroupsChanged = cellGroupsChanged || cellGroups[cellIndex] != snapshot.cellGroups[cellIndex];
        }
        if (libBackups == snapshot.libBackups && !cellsChanged && !cellBoundsChanged && !cellGroupsChanged)
            return snapshot;
        if (!cellsChanged) cellBackups = snapshot.cellBackups;
        if (!cellBoundsChanged) cellBounds = snapshot.cellBounds;
        if (!cellGroupsChanged) cellGroups = snapshot.cellGroups;
        snapshot = new Snapshot(cellBackups, cellGroups, cellBounds, libBackups);
        checkFresh(snapshot);
        snapshotFresh = true;
        return snapshot;
    }

    /*
	 * Low-level method to backup all Libraries to array of LibraryBackups.
     * @param oldLibs LibararyBackups in old snapshot.
     * @return CellBackup which is the backup of this Cell.
	 */
    private LibraryBackup[] backupLibs(LibraryBackup[] oldLibs) {
        ArrayList<LibraryBackup> libBackups = new ArrayList<LibraryBackup>();
        for (Iterator<Library> lit = Library.getLibraries(); lit.hasNext(); ) {
            Library lib = lit.next();
            int libIndex = lib.getId().libIndex;
            LibraryBackup oldBackup = libIndex < oldLibs.length ? oldLibs[libIndex] : null;
            while (libBackups.size() <= libIndex) libBackups.add(null);
            assert libBackups.get(libIndex) == null;
            libBackups.set(libIndex, lib.backup(oldBackup));
        }
        if (libBackups.size() == oldLibs.length) {
            int i;
            for (i = 0; i < oldLibs.length; i++) {
                if (libBackups.get(i) != oldLibs[i])
                    break;
            }
            if (i == oldLibs.length) return oldLibs;
        }
        return libBackups.toArray(LibraryBackup.NULL_ARRAY);
    }
    
    /**
     * Force database to specified state.
     * @param undoSnapshot old immutable snapshot.
     */
    public void undo(Snapshot undoSnapshot) {
        List<LibId> changedLibs = undoSnapshot.getChangedLibraries(snapshot);
        if (changedLibs != null) {
            for (LibId libId: changedLibs) {
                LibraryBackup oldBackup = snapshot.getLib(libId);
                LibraryBackup newBackup = undoSnapshot.getLib(libId);
                int libIndex = libId.libIndex;
                if (oldBackup == null) {
                    Library lib = new Library(this, newBackup.d);
                    assert newBackup.d.libId == libId;
                    while (linkedLibs.size() <= libIndex) linkedLibs.add(null);
                    Library oldLib = linkedLibs.set(libIndex, lib);
                    assert oldLib == null;
                } else if (newBackup == null) {
                    Library oldLib = linkedLibs.set(libIndex, null);
                    assert oldLib != null;
                    oldLib.cells.clear();
                  /*
                } else {
                    
                Library lib = linkedLibs.get(libIndex);
                String libName = lib.getName();
                if (!oldBackup.d.libName.equals(libName)) {
                    Cell curCell = lib.getCurCell();
                    lib.prefs = allPrefs.node(libName);
                    lib.prefs.put("LIB", libName);
                    lib.curCellPref = null;
                    lib.setCurCell(curCell);
                }
                 */
                }
            }
            libraries.clear();
            for (Library lib: linkedLibs) {
                if (lib == null) continue;
                libraries.put(lib.getName(), lib);
                lib.undo(undoSnapshot.getLib(lib.getId()));
            }
            /* ???
            if (curLib == null || !curLib.isLinked()) {
                curLib = null;
                for(Library lib: libraries.values()) {
    				if (lib.isHidden()) continue;
        			curLib = lib;
            		break;
                }
			}
            */
        }
        
        boolean cellTreeChanged = snapshot.cellGroups != undoSnapshot.cellGroups;
        for (int i = 0, maxCells = Math.max(snapshot.cellBackups.length, undoSnapshot.cellBackups.length); i < maxCells; i++) {
            CellBackup oldBackup = snapshot.getCell(i);
            CellBackup newBackup = undoSnapshot.getCell(i);
            if (oldBackup == newBackup) continue;
            if (oldBackup == null || newBackup == null ||
                    !oldBackup.d.cellName.equals(newBackup.d.cellName) || oldBackup.isMainSchematics != newBackup.isMainSchematics)
                cellTreeChanged = true;
        }
        updateAll(snapshot, undoSnapshot);
        if (changedLibs != null || cellTreeChanged) {
            for (Library lib: libraries.values())
                lib.cells.clear();
            for (Cell cell: linkedCells) {
                if (cell == null) continue;
                Library lib = cell.getLibrary();
                assert lib.isLinked();
                lib.cells.put(cell.getCellName(), cell);
            }
        }
        snapshot = undoSnapshot;
        checkFresh(undoSnapshot);
        snapshotFresh = true;
    }
    
    void updateAll(Snapshot oldSnapshot, Snapshot newSnapshot) {
        BitSet updated = new BitSet();
        BitSet exportsModified = new BitSet();
        for (CellId cellId: newSnapshot.getChangedCells(oldSnapshot)) {
            CellBackup oldBackup = oldSnapshot.getCell(cellId);
            CellBackup newBackup = newSnapshot.getCell(cellId);
            if (newBackup != null)
                updateTree(oldSnapshot, newSnapshot, cellId, updated, exportsModified);
            else
                removeCell(cellId);
        }
        boolean mainSchematicsChanged = false;
        for (int i = 0; i < newSnapshot.cellBackups.length; i++) {
            CellBackup oldBackup = oldSnapshot.getCell(i);
            CellBackup newBackup = newSnapshot.getCell(i);
            if (oldBackup != null && newBackup != null && oldBackup.isMainSchematics != newBackup.isMainSchematics)
            	mainSchematicsChanged = true;
        }
        if (oldSnapshot.cellGroups != newSnapshot.cellGroups || mainSchematicsChanged)
            updateCellGroups(newSnapshot);
    }
    
    private void updateCellGroups(Snapshot newSnapshot) {
        ArrayList<TreeSet<Cell>> groups = new ArrayList<TreeSet<Cell>>();
        ArrayList<Cell> mainSchematics = new ArrayList<Cell>();
        for (int i = 0; i < newSnapshot.cellBackups.length; i++) {
            CellBackup cellBackup = newSnapshot.cellBackups[i];
            int cellGroupIndex = newSnapshot.cellGroups[i];
            if (cellBackup == null) continue;
            if (cellGroupIndex == groups.size()) {
                groups.add(new TreeSet<Cell>());
                mainSchematics.add(null);
            }
            Cell cell = getCell(i);
            assert cell != null;
            groups.get(cellGroupIndex).add(cell);
            if (cellBackup.isMainSchematics)
                mainSchematics.set(cellGroupIndex, cell);
        }
        for (int i = 0; i < groups.size(); i++)
            new Cell.CellGroup(this, groups.get(i), mainSchematics.get(i));
    }
    
    private void updateTree(Snapshot oldSnapshot, Snapshot newSnapshot, CellId cellId, BitSet updated, BitSet exportsModified) {
        int cellIndex = cellId.cellIndex;
    	if (updated.get(cellIndex)) return;
        CellBackup newBackup = newSnapshot.getCell(cellId);
        assert cellId != null;
        boolean subCellsModified = false;
        for (int i = 0; i < newBackup.cellUsages.length; i++) {
        	if (newBackup.cellUsages[i] <= 0) continue;
        	CellUsage u = cellId.getUsageIn(i);
        	if (exportsModified.get(u.protoId.cellIndex))
        		subCellsModified = true;
        	updateTree(oldSnapshot, newSnapshot, u.protoId, updated, exportsModified);
        }
        CellBackup oldBackup = oldSnapshot.getCell(cellId);
        Cell cell;
        if (oldBackup == null) {
            cell = new Cell(this, newBackup.d);
            cell.update(newBackup, exportsModified);
            assert cell.getCellId() == cellId;
            addCell(cell);
        } else {
            cell = getCell(cellId);
            if (newBackup != oldBackup) {
            	if (!newBackup.sameExports(oldBackup))
            		exportsModified.set(cellIndex);
            	cell.update(newBackup, subCellsModified ? exportsModified : null);
            } else if (subCellsModified)
                cell.updatePortInsts(exportsModified);
        }
        cell.undoCellBounds(newSnapshot.getCellBounds(cellId));
    	updated.set(cellIndex);
    }

    /**
     * Checks that Electric database has the expected state.
     * @expectedSnapshot expected state.
     */
    public void checkFresh(Snapshot expectedSnapshot) {
        for (Iterator<Library> lit = Library.getLibraries(); lit.hasNext(); ) {
            Library lib = lit.next();
            LibraryBackup libBackup = snapshot.getLib(lib.getId());
            assert libBackup.d == lib.getD();
            for (Iterator<Cell> cit = lib.getCells(); cit.hasNext(); ) {
                Cell cell = cit.next();
                CellBackup cellBackup = snapshot.getCell((CellId)cell.getId());
                assert cellBackup.d == cell.getD();
            }
        }
        for (int i = 0; i < snapshot.libBackups.length; i++) {
            LibraryBackup libBackup = snapshot.libBackups[i];
            if (libBackup == null) continue;
            LibId libId = libBackup.d.libId;
            assert libId.libIndex == i;
            Library lib = getLib(libId);
            lib.checkFresh(libBackup);
        }
        for (int i = 0; i < snapshot.cellBackups.length; i++) {
            CellBackup cellBackup = snapshot.getCell(i);
            if (cellBackup == null) continue;
            CellId cellId = cellBackup.d.cellId;
            assert cellId.cellIndex == i;
            Cell cell = getCell(cellId);
            cell.checkFresh(cellBackup);
            assert snapshot.getCellBounds(cellId) == cell.getBounds();
        }
        
        HashMap<Cell.CellGroup,Integer> groupNums = new HashMap<Cell.CellGroup,Integer>();
        for (int i = 0; i < snapshot.cellBackups.length; i++) {
            CellBackup cellBackup = snapshot.getCell(i);
            if (cellBackup == null) continue;
            Cell cell = Cell.inCurrentThread(cellBackup.d.cellId);
            Cell.CellGroup cellGroup = cell.getCellGroup();
            Integer gn = groupNums.get(cellGroup);
            if (gn == null) {
                gn = Integer.valueOf(groupNums.size());
                groupNums.put(cellGroup, gn);
            }
            assert snapshot.cellGroups[i] == gn.intValue();
        }
        assert snapshot == expectedSnapshot;
    }

	/**
	 * Method to check invariants in all Libraries.
	 * @return true if invariants are valid
	 */
	public boolean checkInvariants()
	{
		try
		{
			//long startTime = System.currentTimeMillis();
            CellId.checkInvariants();
            
			TreeSet<String> libNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
			for (Map.Entry<String,Library> e : libraries.entrySet())
			{
				String libName = (String)e.getKey();
				Library lib = (Library)e.getValue();
				assert libName.equals(lib.getName()) : libName + " " + lib;
				assert !libNames.contains(libName) : "case insensitive " + libName;
				libNames.add(libName);
				lib.check();
			}
			//long endTime = System.currentTimeMillis();
			//float finalTime = (endTime - startTime) / 1000F;
			//System.out.println("**** Check Invariants took " + finalTime + " seconds");
			return true;
		} catch (Throwable e)
		{
			if (!invariantsFailed)
			{
				System.out.println("Exception checking database invariants");
				e.printStackTrace();
				ActivityLogger.logException(e);
				invariantsFailed = true;
			}
		}
		return false;
	}

}
