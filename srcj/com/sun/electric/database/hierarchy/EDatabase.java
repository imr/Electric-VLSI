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
import com.sun.electric.database.IdManager;
import com.sun.electric.database.LibId;
import com.sun.electric.database.LibraryBackup;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.network.NetworkManager;
import com.sun.electric.database.text.ImmutableArrayList;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Tool;
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
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Electric run-time database is a graph of ElectricObjects.
 */
public class EDatabase {
    private static final Logger logger = Logger.getLogger("com.sun.electric.database");
    private static final String CLASS_NAME = EDatabase.class.getName();
    
    public static EDatabase theDatabase = new EDatabase(new IdManager());
    public static EDatabase serverDatabase() { return theDatabase; }
    public static EDatabase clientDatabase() { return theDatabase; }
    
    /** IdManager which keeps Ids of objects in this database.*/private final IdManager idManager;
	/** list of linked libraries indexed by libId. */           private final ArrayList<Library> linkedLibs = new ArrayList<Library>();
	/** map of libraries sorted by name */                      final TreeMap<String,Library> libraries = new TreeMap<String,Library>(TextUtils.STRING_NUMBER_ORDER);
	/** static list of all linked cells indexed by CellId. */	final ArrayList<Cell> linkedCells = new ArrayList<Cell>();
    /** Last snapshot */                                        private Snapshot snapshot;
    /** True if database matches snapshot. */                   private boolean snapshotFresh;
	/** Flag set when database invariants failed. */            private boolean invariantsFailed;
    
    /** Network manager for this database. */                   private final NetworkManager networkManager;
    /** Thread which locked database for writing. */            private volatile Thread writingThread;
    /** True if writing thread can changing. */                 private boolean canChanging;
    /** True if writing thread can undoing. */                  private boolean canUndoing;
    /** Tool which initiated changing. */                       private Tool changingTool;
    
    /** Creates a new instance of EDatabase */
    public EDatabase(IdManager idManager) {
        this.idManager = idManager;
        snapshot = getInitialSnapshot();
        networkManager = new NetworkManager(this);
    }
    
    public IdManager getIdManager() { return idManager; }
    
    public Snapshot getInitialSnapshot() { return idManager.getInitialSnapshot(); }
    
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
        while (!linkedLibs.isEmpty() && linkedLibs.get(linkedLibs.size() - 1) == null)
            linkedLibs.remove(linkedLibs.size() - 1);
        libraries.remove(oldLib.getName());
    }
    
    public Cell getCell(CellId cellId) { return getCell(cellId.cellIndex); }
    
    void addCell(Cell cell) {
        int cellIndex = cell.getCellIndex();
        while (cellIndex >= linkedCells.size()) linkedCells.add(null);
        Cell oldCell = linkedCells.set(cellIndex, cell);
        assert oldCell == null;
    }
    
    void removeCell(CellId cellId) {
        Cell oldCell = linkedCells.set(cellId.cellIndex, null);
        assert oldCell != null;
        while (!linkedCells.isEmpty() && linkedCells.get(linkedCells.size() - 1) == null)
            linkedCells.remove(linkedCells.size() - 1);
    }
    
    Library getLib(int libIndex) { return libIndex < linkedLibs.size() ? linkedLibs.get(libIndex) : null; }
    
    Cell getCell(int cellIndex) { return cellIndex < linkedCells.size() ? linkedCells.get(cellIndex) : null; } 
    
    /**
     * Locks the database.
     * Lock may be either exclusive (for writing) or shared (for reading).
     * @param exclusive true if lock is for writing.
     */
    public void lock(boolean exclusive) {
        assert writingThread == null;
        if (exclusive)
            writingThread = Thread.currentThread();
        canChanging = canUndoing = false;
    }
    
    /**
     * Unlocks the database.
     */
    public void unlock() {
        writingThread = null;
    }
    
	/**
	 * Method to check whether changing of database is allowed by current thread.
	 * @throws IllegalStateException if changes are not allowed.
	 */
	public void checkChanging() {
        if (Thread.currentThread() == writingThread && canChanging) return;
        IllegalStateException e = new IllegalStateException("Database changes are forbidden");
        logger.logp(Level.WARNING, CLASS_NAME, "checkChanging", e.getMessage(), e);
        throw e;
	}

	/**
	 * Method to check whether changing of whole database is allowed.
	 * @throws IllegalStateException if changes are not allowed.
	 */
	public void checkUndoing() {
        if (Thread.currentThread() == writingThread && canUndoing) return;
        IllegalStateException e = new IllegalStateException("Database undo is forbidden");
        logger.logp(Level.WARNING, CLASS_NAME, "checkUndoing", e.getMessage(), e);
        throw e;
	}
    
    public boolean canComputeBounds() { return Thread.currentThread() == writingThread && (canChanging || canUndoing); }

    public boolean canComputeNetlist() { return Thread.currentThread() == writingThread && (canChanging || canUndoing); }
    
	/**
	 * Method to check whether examining of database is allowed.
	 */
    public void checkExamine() {
        if (writingThread == null || Thread.currentThread() == writingThread) return;
        IllegalStateException e = new IllegalStateException("Cuncurrent database examine");
//        e.printStackTrace();
        logger.logp(Level.FINE, CLASS_NAME, "checkExamine", e.getMessage(), e);
//        throw e;
    }
    
    /**
     * Low-level method to begin changes in database.
     * @param changingTool tool which initiated
     */
    public void lowLevelBeginChanging(Tool changingTool) {
        if (Thread.currentThread() != writingThread)
            checkChanging();
        canChanging = true;
        this.changingTool = changingTool;
    }
    
    /**
     * Low-level method to permit changes in database.
     */
    public void lowLevelEndChanging() {
        if (Thread.currentThread() != writingThread)
            checkChanging();
        changingTool = null;
        canChanging = false;
    }
    
    /**
     * Low-level method to permit undos in database.
     */
    public void lowLevelSetCanUndoing(boolean b) {
        if (Thread.currentThread() != writingThread)
            checkUndoing();
        canUndoing = b;
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
        checkChanging();
        snapshotFresh = false;
    }
    
   /**
     * Create Snapshot from the current state of Electric database.
     * @return snapshot of the current state of Electric database.
     */
    public Snapshot backup() {
        if (snapshotFresh) return snapshot;
        return doBackup();
    }
        
    private Snapshot doBackup() {    
//        long startTime = System.currentTimeMillis();
        LibraryBackup[] libBackups = new LibraryBackup[linkedLibs.size()];
        boolean libsChanged = libBackups.length != snapshot.libBackups.size();
        for (int libIndex = 0; libIndex < libBackups.length; libIndex++) {
            Library lib = linkedLibs.get(libIndex);
            LibraryBackup libBackup = lib != null ? lib.backup() : null;
            libBackups[libIndex] = libBackup;
            libsChanged = libsChanged || snapshot.libBackups.get(libIndex) != libBackup; 
        }
        if (!libsChanged) libBackups = null;
        
        CellBackup[] cellBackups = new CellBackup[linkedCells.size()];
        ERectangle[] cellBounds = new ERectangle[cellBackups.length];
        boolean cellsChanged = cellBackups.length != snapshot.cellBackups.size();
        boolean cellBoundsChanged = cellsChanged;
        for (int cellIndex = 0; cellIndex < cellBackups.length; cellIndex++) {
            Cell cell = getCell(cellIndex);
			if (cell != null) {
				cellBackups[cellIndex] = cell.backup();
                cellBounds[cellIndex] = cell.getBounds();
            }
            cellsChanged = cellsChanged || cellBackups[cellIndex] != snapshot.getCell(cellIndex);
            cellBoundsChanged = cellBoundsChanged || cellBounds[cellIndex] != snapshot.getCellBounds(cellIndex);
        }
        if (!cellsChanged) cellBackups = null;
        if (!cellBoundsChanged) cellBounds = null;
        snapshot = snapshot.with(changingTool, cellBackups, cellBounds, libBackups);
//        checkFresh(snapshot);
        snapshotFresh = true;
//        long endTime = System.currentTimeMillis();
//        if (Job.getDebug()) System.out.println("backup took: " + (endTime - startTime) + " msec");
        return snapshot;
    }

    /**
     * Force database to specified state.
     * This method can recover currupted database.
     * @param snapshot snapshot to recover.
     */
    public void recover(Snapshot snapshot) {
        long startTime = System.currentTimeMillis();
        this.snapshot = snapshot;
        recoverLibraries();
        recycleCells();
        BitSet recovered = new BitSet();
        for (CellBackup newBackup: snapshot.cellBackups) {
            if (newBackup != null)
                recoverRecursively(newBackup.d.cellId, recovered);
        }
        for (Library lib: libraries.values())
            lib.collectCells();
        recoverCellGroups();
        snapshotFresh = true;
        long endTime = System.currentTimeMillis();
        if (Job.getDebug()) {
            System.out.println("recover took: " + (endTime - startTime) + " msec");
            checkInvariants();
        }
    }
    
    private void recoverRecursively(CellId cellId, BitSet recovered) {
        int cellIndex = cellId.cellIndex;
        if (recovered.get(cellIndex)) return;
        CellBackup newBackup = snapshot.getCell(cellId);
        for (int i = 0, numUsages = cellId.numUsagesIn(); i < numUsages; i++) {
            CellUsage u = cellId.getUsageIn(i);
            if (newBackup.getInstCount(u) <= 0) continue;
            recoverRecursively(u.protoId, recovered);
        }
        Cell cell = getCell(cellId);
        cell.recover(newBackup, snapshot.getCellBounds(cellId));
    	recovered.set(cellIndex);
    }

    /**
     * Force database to specified state.
     * This method assumes that database is in valid state.
     * @param snapshot snapshot to undo.
     */
    public void undo(Snapshot snapshot) {
        long startTime = System.currentTimeMillis();
        Snapshot oldSnapshot = backup();
        if (oldSnapshot == snapshot) return;
        this.snapshot = snapshot;
        boolean cellGroupsChanged = oldSnapshot.cellGroups != snapshot.cellGroups;
        if (oldSnapshot.libBackups != snapshot.libBackups) {
            recoverLibraries();
            cellGroupsChanged = true;
        }
        recycleCells();

        BitSet cellNamesChangedInLibrary = new BitSet();
        ImmutableArrayList<CellBackup> cellBackups = snapshot.cellBackups;
        if (oldSnapshot.cellBackups.size() == cellBackups.size()) {
            for (int cellIndex = 0; cellIndex < cellBackups.size(); cellIndex++) {
                CellBackup oldBackup = oldSnapshot.getCell(cellIndex);
                CellBackup newBackup = snapshot.getCell(cellIndex);
                if (oldBackup == newBackup) continue;
                if (oldBackup == null) {
                    cellNamesChangedInLibrary.set(newBackup.d.libId.libIndex);
                    assert cellGroupsChanged;
                } else if (newBackup == null) {
                    cellNamesChangedInLibrary.set(oldBackup.d.libId.libIndex);
                    assert cellGroupsChanged;
                } else {
                    boolean moved = oldBackup.d.libId != newBackup.d.libId;
                    if (moved || oldBackup.d.cellName != newBackup.d.cellName) {
                        cellNamesChangedInLibrary.set(newBackup.d.libId.libIndex);
                        cellNamesChangedInLibrary.set(oldBackup.d.libId.libIndex);
                    }
                    if (moved)
                        cellGroupsChanged = true;
                }
            }
        } else {
            cellGroupsChanged = true;
            if (snapshot.libBackups.size() > 0) // Bug in BitSet.set(int,int) on Sun JDK
                cellNamesChangedInLibrary.set(0, snapshot.libBackups.size());
        }
        
        BitSet updated = new BitSet();
        BitSet exportsModified = new BitSet();
        BitSet boundsModified = new BitSet();
        for (CellBackup newBackup: snapshot.cellBackups) {
            if (newBackup != null)
                undoRecursively(oldSnapshot, newBackup.d.cellId, updated, exportsModified, boundsModified);
        }
        if (!cellNamesChangedInLibrary.isEmpty()) {
            for (Library lib: libraries.values()) {
                if (cellNamesChangedInLibrary.get(lib.getId().libIndex))
                    lib.collectCells();
            }
        }
        if (cellGroupsChanged)
            recoverCellGroups();
        snapshotFresh = true;
        long endTime = System.currentTimeMillis();
        if (Job.getDebug()) {
            System.out.println("undo took: " + (endTime - startTime) + " msec");
//            checkFresh(snapshot);
        }
    }
    
    private void undoRecursively(Snapshot oldSnapshot, CellId cellId, BitSet updated, BitSet exportsModified, BitSet boundsModified) {
        int cellIndex = cellId.cellIndex;
    	if (updated.get(cellIndex)) return;
        CellBackup newBackup = snapshot.getCell(cellId);
        assert cellId != null;
        boolean subCellsExportsModified = false;
        boolean subCellsBoundsModified = false;
        for (int i = 0, numUsages = cellId.numUsagesIn(); i < numUsages; i++) {
            CellUsage u = cellId.getUsageIn(i);
            if (newBackup.getInstCount(u) <= 0) continue;
            undoRecursively(oldSnapshot, u.protoId, updated, exportsModified, boundsModified);
            int subCellIndex = u.protoId.cellIndex;
            if (exportsModified.get(subCellIndex))
                subCellsExportsModified = true;
            if (boundsModified.get(subCellIndex))
                subCellsBoundsModified = true;
        }
        Cell cell = getCell(cellId);
        CellBackup oldBackup = oldSnapshot.getCell(cellId);
        ERectangle oldBounds = oldSnapshot.getCellBounds(cellId);
        cell.undo(newBackup, snapshot.getCellBounds(cellId),
                subCellsExportsModified ? exportsModified : null,
                subCellsBoundsModified ? boundsModified : null);
    	updated.set(cellIndex);
        if (oldBackup == null || !newBackup.sameExports(oldBackup))
            exportsModified.set(cellIndex);
        if (oldBackup == null || snapshot.getCellBounds(cellId) != oldBounds)
            boundsModified.set(cellIndex);
    }

    private void recoverLibraries() {
        while (linkedLibs.size() > snapshot.libBackups.size()) {
            Library lib  = linkedLibs.remove(linkedLibs.size() - 1);
            if (lib != null) lib.cells.clear();
        }
        while (linkedLibs.size() < snapshot.libBackups.size()) linkedLibs.add(null);
        for (int libIndex = 0; libIndex < snapshot.libBackups.size(); libIndex++) {
            LibraryBackup libBackup = snapshot.libBackups.get(libIndex);
            Library lib = linkedLibs.get(libIndex);
            if (libBackup == null && lib != null) {
                linkedLibs.set(libIndex, null);
            } else if (libBackup != null && lib == null) {
                linkedLibs.set(libIndex, new Library(this, libBackup.d));
            }
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
        libraries.clear();
        for (int libIndex = 0; libIndex < snapshot.libBackups.size(); libIndex++) {
            LibraryBackup libBackup = snapshot.libBackups.get(libIndex);
            if (libBackup == null) continue;
            Library lib = linkedLibs.get(libIndex);
            lib.recover(libBackup);
            libraries.put(lib.getName(), lib);
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
    
    private void recycleCells() {
        ImmutableArrayList<CellBackup> cellBackups = snapshot.cellBackups;
        while (linkedCells.size() > cellBackups.size()) linkedCells.remove(linkedCells.size() - 1);
        while (linkedCells.size() < cellBackups.size()) linkedCells.add(null);
        for (int cellIndex = 0; cellIndex < cellBackups.size(); cellIndex++) {
            CellBackup newBackup = cellBackups.get(cellIndex);
            Cell cell = linkedCells.get(cellIndex);
            if (newBackup == null) {
                if (cell != null) linkedCells.set(cellIndex, null);
            } else if (cell == null) {
                linkedCells.set(cellIndex, new Cell(this, newBackup.d));
            }
        }
    }
    
    private void recoverCellGroups() {
        ArrayList<TreeSet<Cell>> groups = new ArrayList<TreeSet<Cell>>();
        for (int cellIndex = 0; cellIndex < snapshot.cellBackups.size(); cellIndex++) {
            CellBackup cellBackup = snapshot.cellBackups.get(cellIndex);
            int cellGroupIndex = snapshot.cellGroups[cellIndex];
            if (cellBackup == null) continue;
            if (cellGroupIndex == groups.size())
                groups.add(new TreeSet<Cell>());
            Cell cell = getCell(cellIndex);
            assert cell != null;
            groups.get(cellGroupIndex).add(cell);
        }
        for (int i = 0; i < groups.size(); i++)
            new Cell.CellGroup(groups.get(i));
    }
    
	/**
	 * Method to check invariants in all Libraries.
	 * @return true if invariants are valid
	 */
	public boolean checkInvariants()
	{
		try
		{
			long startTime = System.currentTimeMillis();
            idManager.checkInvariants();
            backup();
            snapshot.check();
            check();
			long endTime = System.currentTimeMillis();
			float finalTime = (endTime - startTime) / 1000F;
            if (Job.getDebug())
                System.out.println("**** Check Invariants took " + finalTime + " seconds");
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

	/**
	 * Method to check invariants in this EDatabase.
	 * @exception AssertionError if invariants are not valid
	 */
    private void check() {
        if (snapshotFresh) {
            assert linkedLibs.size() == snapshot.libBackups.size();
            assert linkedCells.size() == snapshot.cellBackups.size();
        }
        
        for (int libIndex = 0; libIndex < linkedLibs.size(); libIndex++) {
            Library lib = linkedLibs.get(libIndex);
            if (lib == null) {
                if (snapshotFresh) assert snapshot.libBackups.get(libIndex) == null;
                continue;
            }
            assert lib.getId() == getIdManager().getLibId(libIndex);
            assert libraries.get(lib.getName()) == lib;
            lib.check();
            if (snapshotFresh)
                assert lib.libBackupFresh && lib.backup == snapshot.libBackups.get(libIndex);
        }
        
        for (int cellIndex = 0; cellIndex < linkedCells.size(); cellIndex++) {
            Cell cell = linkedCells.get(cellIndex);
            if (cell == null) {
                if (snapshotFresh) assert snapshot.cellBounds.get(cellIndex) == null;
                continue;
            }
            assert cell.getId() == idManager.getCellId(cellIndex);
            Library lib = cell.getLibrary();
            assert lib.cells.get(cell.getCellName()) == cell;
            cell.check();
            if (snapshotFresh) {
                assert cell.cellBackupFresh;
                assert cell.backup == snapshot.cellBackups.get(cellIndex);
                assert cell.getBounds() == snapshot.cellBounds.get(cellIndex);
            }
        }
        
        TreeSet<String> libNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
        for (Map.Entry<String,Library> e : libraries.entrySet()) {
            String libName = (String)e.getKey();
            Library lib = (Library)e.getValue();
            assert libName == lib.getName();
            assert linkedLibs.get(lib.getId().libIndex) == lib;
            
            assert !libNames.contains(libName) : "case insensitive " + libName;
            libNames.add(libName);
        }
        
        if (snapshotFresh) {
            HashMap<Cell.CellGroup,Integer> groupNums = new HashMap<Cell.CellGroup,Integer>();
            for (int i = 0; i < snapshot.cellBackups.size(); i++) {
                CellBackup cellBackup = snapshot.getCell(i);
                if (cellBackup == null) {
                    assert snapshot.cellGroups[i] == -1;
                    continue;
                }
                Cell cell = getCell(cellBackup.d.cellId);
                Cell.CellGroup cellGroup = cell.getCellGroup();
                Integer gn = groupNums.get(cellGroup);
                if (gn == null) {
                    gn = Integer.valueOf(groupNums.size());
                    groupNums.put(cellGroup, gn);
                }
                assert snapshot.cellGroups[i] == gn.intValue();
            }
        }
    }
    
    /**
     * Checks that Electric database has the expected state.
     * @param expectedSnapshot expected state.
     */
    public void checkFresh(Snapshot expectedSnapshot) {
        assert snapshotFresh && snapshot == expectedSnapshot;
        check();
    }

}
