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

import com.sun.electric.Main;
import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellId;
import com.sun.electric.database.ImmutableElectricObject;
import com.sun.electric.database.ImmutableLibrary;
import com.sun.electric.database.LibId;
import com.sun.electric.database.LibraryBackup;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.change.Undo;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.user.ActivityLogger;
import com.sun.electric.tool.user.ErrorLogger;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * A Library represents a collection of Cells.
 * To find of a Library, you use Electric.getLibrary(String name).
 * Use Electric.newLibrary(String name) to create a new library, or
 * Electric.getCurrent() to get the current Library.
 * <p>
 * Once you have a Library, you can create a new Cell in it, find an existing
 * Cell, get an Enumeration of all Cells, or find the Cell that the user
 * is currently editing.
 */
public class Library extends ElectricObject implements Comparable<Library>
{
	/** key of Variable holding font associations. */		public static final Variable.Key FONT_ASSOCIATIONS = Variable.newKey("LIB_font_associations");

	// ------------------------ private data ------------------------------

	/** library has changed significantly */				private static final int LIBCHANGEDMAJOR =           01;
//	/** set to see library in explorer */					private static final int OPENINEXPLORER =            02;
	/** set if library came from disk */					private static final int READFROMDISK =              04;
//	/** internal units in library (see INTERNALUNITS) */	private static final int LIBUNITS =                 070;
//	/** right shift for LIBUNITS */							private static final int LIBUNITSSH =                 3;
	/** library has changed insignificantly */				private static final int LIBCHANGEDMINOR =         0100;
	/** library is "hidden" (clipboard library) */			private static final int HIDDENLIBRARY =           0200;
//	/** library is unwanted (used during input) */			private static final int UNWANTEDLIB =             0400;

    /** persistent data of this Library. */                 private ImmutableLibrary d;
	/** list of Cells in this library */					final TreeMap<CellName,Cell> cells = new TreeMap<CellName,Cell>();
	/** Preference for cell currently being edited */		private Pref curCellPref;
    /** list of referenced libs */                          private final List<Library> referencedLibs = new ArrayList<Library>();
    /** preferences for this library */                     Preferences prefs;

	/** preferences for all libraries */					private static Preferences allPrefs = null;
	/** list of linked libraries indexed by libId. */       private static final ArrayList<Library> linkedLibs = new ArrayList<Library>();
	/** map of libraries sorted by name */                  private static final TreeMap<String,Library> libraries = new TreeMap<String,Library>(TextUtils.STRING_NUMBER_ORDER);
	/** the current library in Electric */					private static Library curLib = null;

	// ----------------- private and protected methods --------------------

	/**
	 * The constructor is never called.  Use the factor method "newInstance" instead.
	 */
	private Library(ImmutableLibrary d)
	{
        this.d = d;
		if (allPrefs == null) allPrefs = Preferences.userNodeForPackage(getClass());
        prefs = allPrefs.node(d.libName);
        prefs.put("LIB", d.libName);
	}

	/**
	 * This method is a factory to create new libraries.
	 * A Library has both a name and a file.
	 * @param libName the name of the library (for example, "gates").
	 * Library names must be unique, and they must not contain spaces.
	 * @param libFile the URL to the disk file (for example "/home/strubin/gates.elib").
	 * If the Library is being created, the libFile can be null.
	 * If the Library file is given and it points to an existing file, then the I/O system
	 * can be told to read that file and populate the Library.
	 * @return the Library object.
	 */
	public static Library newInstance(String libName, URL libFile)
	{
		// make sure the name is legal
        String legalName = legalLibraryName(libName);
        if (legalName == null) return null;
		if (legalName != libName)
			System.out.println("Warning: library '" + libName + "' renamed to '" + legalName + "'");
		
		// see if the library name already exists
		Library existingLibrary = Library.findLibrary(legalName);
		if (existingLibrary != null)
		{
			System.out.println("Error: library '" + legalName + "' already exists");
			return existingLibrary;
		}
		
		// create the library
        return newInstance(new LibId(), legalName, libFile);
	}

	/**
	 * This method is a factory to create new libraries.
	 * A Library has both a name and a file.
     * @param libId ID of new Library. 
	 * @param legalName the name of the library (for example, "gates").
	 * Library names must be unique, and they must not contain spaces.
	 * @param libFile the URL to the disk file (for example "/home/strubin/gates.elib").
	 * If the Library is being created, the libFile can be null.
	 * If the Library file is given and it points to an existing file, then the I/O system
	 * can be told to read that file and populate the Library.
	 * @return the Library object.
     * @throws NullPoinerException if libId or legalName is null.
     * @throws IllegalArgumentException if libId is occupied or legalName is not legal or
     * library with such name exists
	 */
	public static Library newInstance(LibId libId, String legalName, URL libFile) {
        if (legalName == null) throw new NullPointerException();
        if (legalName != legalLibraryName(legalName)) throw new IllegalArgumentException(legalName);
        
		// create the library
        ImmutableLibrary d = ImmutableLibrary.newInstance(libId, legalName, libFile, null);
		Library lib = new Library(d);

		// add the library to the global list
		synchronized (libraries)
		{
            while (linkedLibs.size() <= libId.libIndex) linkedLibs.add(null);
            if (linkedLibs.get(libId.libIndex) != null) throw new IllegalArgumentException();
            linkedLibs.set(libId.libIndex, lib);
			libraries.put(legalName, lib);
		}

        // always broadcast library changes
//        Undo.setNextChangeQuiet(false);
        Undo.newObject(lib);
		return lib;
        
    }
    
	/**
	 * Method to delete this Library.
	 * @param reason the reason for deleting this library (replacement or deletion).
	 * @return true if the library was deleted.
	 * Returns false on error.
	 */
	public boolean kill(String reason)
	{
		if (!isLinked())
		{
			System.out.println("Library already killed");
			return false;
		}
		// cannot delete the current library
		Library newCurLib = null;
		if (curLib == this)
		{
			// find another library
			/*for(Library lib: libraries)*/
			for (Iterator<Library> it = libraries.values().iterator(); it.hasNext(); )
			{
				Library lib = (Library)it.next();
				if (lib == curLib) continue;
				if (lib.isHidden()) continue;
				newCurLib = lib;
				break;
			}
			if (newCurLib == null)
			{
				System.out.println("Cannot delete the last library");
				Main.getUserInterface().showInformationMessage("Cannot delete the last "+toString(),
					"Close library");
				return false;
			}
		}

		// make sure it is in the list of libraries
		if (libraries.get(d.libName) != this)
		{
			System.out.println("Cannot delete library " + this);
			Main.getUserInterface().showErrorMessage("Cannot delete "+toString(),
				"Close library");
			return false;
		}

		// make sure none of these cells are referenced by other libraries
		boolean referenced = false;
		/*for(Library lib: libraries)*/
		for (Iterator<Library> it = libraries.values().iterator(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			if (lib == this) continue;
			for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				for(Iterator<NodeInst> nIt = cell.getNodes(); nIt.hasNext(); )
				{
					NodeInst ni = (NodeInst)nIt.next();
					if (ni.getProto() instanceof Cell)
					{
						Cell subCell = (Cell)ni.getProto();
						if (subCell.getLibrary() == this)
						{
							Main.getUserInterface().showErrorMessage("Library close failed. Cannot " + reason + " " + toString() +
								" because one of its cells (" + subCell.noLibDescribe() + ") is being used (by " + cell.libDescribe() + ")",
								"Close library");
							 referenced = true;
							 break;
						}
					}
				}
				if (referenced) break;
			}
			if (referenced) break;
		}
		if (referenced) return false;

		// remove all cells in the library
		erase();

		// remove it from the list of libraries
		synchronized (libraries)
		{
			libraries.remove(d.libName);
            linkedLibs.set(d.libId.libIndex, null);
		}

		// set the new current library if appropriate
		if (newCurLib != null) newCurLib.setCurrent();

		// always broadcast library changes
//		Undo.setNextChangeQuiet(false);
		Undo.killObject(this);
		return true;
	}

	/**
	 * Method to remove all contents from this Library.
	 */
	public void erase()
	{
		// remove all cells in the library
        for (Iterator<Cell> it = getCells(); it.hasNext(); ) {
            Cell c = (Cell)it.next();
            c.kill();
        }
		cells.clear();
	}

	/**
	 * Method to add a Cell to this Library.
	 * @param c the Cell to add.
	 */
	void addCell(Cell c)
	{
		CellName cn = c.getCellName();
		// sanity check: make sure Cell isn't already in the list
        synchronized (cells)
        {
            if (cells.containsKey(cn))
            {
                System.out.println("Tried to re-add a cell to a library: " + c);
                return;
            }
			cells.put(cn, c);
		}
	}

	/**
	 * Method to remove a Cell from this Library.
	 * @param c the Cell to remove.
	 */
	void removeCell(Cell c)
	{
		CellName cn = c.getCellName();
		// sanity check: make sure Cell is in the list
        synchronized (cells)
        {
            if (cells.get(cn) != c)
            {
                System.out.println("Tried to remove a non-existant Cell from a library: " + c);
                return;
            }
			cells.remove(cn);
		}
	}

    /**
     * Adds lib as a referenced library. This also checks the dependency
     * would create a circular dependency, in which case the reference is
     * not logged, and the method returns a LibraryDependency object.
     * If everything is ok, the method returns null.
     * @param lib the library to be added as a referenced lib
     * @return null if ok, a LibraryDependency object if this would create circular dependency
     */
    LibraryDependency addReferencedLib(Library lib) {
        synchronized(referencedLibs) {
            if (referencedLibs.contains(lib)) return null;          // already a referenced lib, is ok
        }
        // check recursively if there is a circular dependency
        List<Library> libDependencies = new ArrayList<Library>();
        if (lib.isReferencedLib(this, libDependencies)) {
            // there is a dependency

            // trace the dependency (this is an expensive operation)
            LibraryDependency d = new LibraryDependency();
            d.startLib = lib;
            d.finalRefLib = this;
            Library startLib = lib;
            for (Iterator<Library> itLib = libDependencies.iterator(); itLib.hasNext(); ) {

                // startLib references refLib. Find out why
                Library refLib = (Library)itLib.next();
                boolean found = false;
                // find a cell instance that creates dependency
                for (Iterator<Cell> itCell = startLib.getCells(); itCell.hasNext(); ) {
                    Cell c = (Cell)itCell.next();
                    for (Iterator<NodeInst> it = c.getNodes(); it.hasNext(); ) {
                        NodeInst ni = (NodeInst)it.next();
                        NodeProto np = ni.getProto();
                        if (np instanceof Cell) {
                            Cell cc = (Cell)np;
                            if (cc.getLibrary() == refLib) {
                                d.dependencies.add(c);
                                d.dependencies.add(cc);
                                found = true;
                                break;
                            }
                        }
                    }
                    if (found) break;
                }
                if (!found) System.out.println("ERROR: Library.addReferencedLib dependency trace failed inexplicably");
                startLib = refLib;
            }

            return d;
        }

        // would not create circular dependency, add and return
        synchronized(referencedLibs) {
            referencedLibs.add(lib);
        }
        return null;
    }

    /**
     * Try to remove lib as a referenced library. If it is no longer referenced,
     * it is removed as a reference library.
     * @param lib the reference library that may no longer be referenced
     */
    void removeReferencedLib(Library lib) {
        if (lib == this) return;            // we don't store references to self
        synchronized(referencedLibs) {
            if (!referencedLibs.contains(lib)) return;
//            assert(referencedLibs.contains(lib));
        }
        boolean refFound = false;
        for (Iterator<Cell> itCell = getCells(); itCell.hasNext(); ) {
            Cell c = (Cell)itCell.next();
            for (Iterator<NodeInst> it = c.getNodes(); it.hasNext(); ) {
                NodeInst ni = (NodeInst)it.next();
                NodeProto np = ni.getProto();
                if (np instanceof Cell) {
                    Cell cc = (Cell)np;
                    if (cc.getLibrary() == lib) {
                        refFound = true;
                        break;
                    }
                }
            }
            if (refFound) break;
        }
        if (!refFound) {
            // no instance that would create reference found, ok to remove reference
            synchronized(referencedLibs) {
                referencedLibs.remove(lib);
            }
        }
    }

    /**
     * Returns true if this Library directly references the specified Library 'lib'.
     * It does not return true if the library is referenced through another library.
     * @param lib the possible referenced library
     * @return true if the library is directly referenced by this library, false otherwise
     */
    public boolean referencesLib(Library lib) {
        synchronized(referencedLibs) {
            if (referencedLibs.contains(lib))
                return true;
        }
        return false;
    }

    /**
     * Checks to see if lib is referenced by this library, or by
     * any libraries this library references, recursively.
     * @param lib the lib to check if it is a referenced lib
     * @return true if it is through any number of references, false otherwise
     */
    private boolean isReferencedLib(Library lib, List<Library> libDepedencies) {
        List<Library> reflibsCopy = new ArrayList<Library>();
        synchronized(referencedLibs) {
            if (referencedLibs.contains(lib)) {
                libDepedencies.add(lib);
                return true;
            }
            reflibsCopy.addAll(referencedLibs);
        }
        for (Iterator<Library> it = reflibsCopy.iterator(); it.hasNext(); ) {
            Library reflib = (Library)it.next();

            // if reflib already in dependency list, ignore
            if (libDepedencies.contains(reflib)) continue;

            // check recursively
            libDepedencies.add(reflib);
            if (reflib.isReferencedLib(lib, libDepedencies)) return true;
            // remove reflib in accumulated list, try again
            libDepedencies.remove(reflib);
        }
        return false;
    }

    static class LibraryDependency {
        private List<Cell> dependencies;
        private Library startLib;
        private Library finalRefLib;

        private LibraryDependency() {
            dependencies = new ArrayList<Cell>();
        }

        public String toString() {
            StringBuffer buf = new StringBuffer();
            buf.append(startLib + " depends on " + finalRefLib + " through the following references:\n");
            for (Iterator<Cell> it = dependencies.iterator(); it.hasNext(); ) {
                Cell libCell = (Cell)it.next();
                Cell instance = (Cell)it.next();
                buf.append("   " + libCell.libDescribe() + " instantiates " + instance.libDescribe() + "\n");
            }
            return buf.toString();
        }
    }

	// ----------------- public interface --------------------

    /**
     * Returns persistent data of this Library.
     * @return persistent data of this Library.
     */
    public ImmutableLibrary getD() { return d; }
    
    /**
     * Modifies persistend data of this Library.
     * @param newD new persistent data.
     * @return true if persistent data was modified.
     */
    private boolean setD(ImmutableLibrary newD) {
        checkChanging();
        ImmutableLibrary oldD = d;
        if (newD == oldD) return false;
        d = newD;
        Undo.modifyVariables(this, oldD);
        //setBatchModified();
        return true;
    }

    /**
     * Returns persistent data of this ElectricObject.
     * @return persistent data of this ElectricObject.
     */
    public ImmutableElectricObject getImmutable() { return d; }
    
    public void lowLevelModifyVariables(ImmutableLibrary d) { this.d = d; }
        
    /**
     * Method to add a Variable on this Library.
     * It may add repaired copy of this Variable in some cases.
     * @param var Variable to add.
     */
    public void addVar(Variable var) {
        setD(d.withVariable(var));
    }

	/**
	 * Method to delete a Variable from this Library.
	 * @param key the key of the Variable to delete.
	 */
	public void delVar(Variable.Key key)
	{
        setD(d.withoutVariable(key));
	}
    
    /**
     * Method to return LibId of this Library.
     * LibId identifies Library independently of threads.
     * @return LibId of this Library.
     */
    public LibId getId() { return d.libId; }
    
    /**
     * Returns a Library by LibId.
     * Returns null if the Library is not linked to the database.
     * @param libId LibId to find.
     * @return Library or null.
     */
    public static Library inCurrentThread(LibId libId) {
        try {
            return (Library)linkedLibs.get(libId.libIndex);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
    }
    
    /**
     * Returns true if this Library is linked into database.
     * @return true if this Library is linked into database.
     */
	public boolean isLinked()
	{
        return inCurrentThread(d.libId) == this;
	}

	/*
	 * Low-level method to backup this Library to LibraryBackup.
     * @return CellBackup which is the backup of this Cell.
	 */
    public LibraryBackup backup(LibraryBackup oldBackup) {
        LibId[] oldRefs = oldBackup != null ? oldBackup.referencedLibs : new LibId[0];
        LibId[] newRefs = backupReferencedLibs(oldRefs);
        if (oldBackup != null && d == oldBackup.d && newRefs == oldBackup.referencedLibs)
            return oldBackup;
        return new LibraryBackup(d, newRefs);
    }
    
    private LibId[] backupReferencedLibs(LibId[] oldReferencedLibs) {
        int numRefs = Math.min(oldReferencedLibs.length, referencedLibs.size());
        int matchedRefs = 0;
        while (matchedRefs < numRefs && oldReferencedLibs[matchedRefs] == referencedLibs.get(matchedRefs).getId())
            matchedRefs++;
        if (matchedRefs == oldReferencedLibs.length && matchedRefs == referencedLibs.size()) return oldReferencedLibs;
        LibId[] newRefs = new LibId[referencedLibs.size()];
        System.arraycopy(oldReferencedLibs, 0, newRefs, 0, matchedRefs);
        for (int i = matchedRefs; i < referencedLibs.size(); i++)
            newRefs[i] = referencedLibs.get(i).getId();
        return newRefs;
    }
    
    /**
     * Update database from old immutable snapshot to new immutable snapshot.
     * @param oldSnapshot old immutable snapshot.
     * @param newSnapshot new immutable snapshot.
     */
    public static boolean updateAll(Snapshot oldSnapshot, Snapshot newSnapshot) {
        boolean libChanged = false;
        for (int i = 0, maxLibs = Math.max(oldSnapshot.libBackups.size(), newSnapshot.libBackups.size()); i < maxLibs; i++) {
            LibraryBackup oldBackup = oldSnapshot.getLib(i);
            LibraryBackup newBackup = newSnapshot.getLib(i);
            if (oldBackup == newBackup) continue;
            libChanged = true;
            if (oldBackup == null) {
                Library lib = new Library(newBackup.d);
                assert newBackup.d.libId.libIndex == i;
                while (linkedLibs.size() <= i) linkedLibs.add(null);
                Library oldLib = linkedLibs.set(i, lib);
                assert oldLib == null;
            } else if (newBackup == null) {
                Library oldLib = linkedLibs.set(i, null);
                assert oldLib != null;
                oldLib.cells.clear();
            } else {
                Library lib = linkedLibs.get(i);
                lib.d = newBackup.d;
                String libName = lib.d.libName;
                if (!oldBackup.d.libName.equals(libName)) {
                    Cell curCell = lib.getCurCell();
                    lib.prefs = allPrefs.node(libName);
                    lib.prefs.put("LIB", libName);
                    lib.curCellPref = null;
                    lib.setCurCell(curCell);
                }
            }
        }
        if (libChanged) {
            System.out.println("Libraries changed");
            libraries.clear();
            for (int i = 0; i < linkedLibs.size(); i++) {
                Library lib = linkedLibs.get(i);
                if (lib == null) continue;
                libraries.put(lib.getName(), lib);
                LibraryBackup newBackup = newSnapshot.getLib(i);
                lib.referencedLibs.clear();
                for (int j = 0; j < newBackup.referencedLibs.length; j++)
                    lib.referencedLibs.add(inCurrentThread(newBackup.referencedLibs[j]));
            }
            if (curLib == null || !curLib.isLinked()) {
                curLib = null;
                for(Library lib: libraries.values()) {
    				if (lib.isHidden()) continue;
        			curLib = lib;
            		break;
                }
			}
        }
        
        boolean cellTreeChanged = oldSnapshot.cellGroups != newSnapshot.cellGroups;
        for (int i = 0, maxCells = Math.max(oldSnapshot.cellBackups.size(), newSnapshot.cellBackups.size()); i < maxCells; i++) {
            CellBackup oldBackup = oldSnapshot.getCell(i);
            CellBackup newBackup = newSnapshot.getCell(i);
            if (oldBackup == newBackup) continue;
            if (oldBackup != null && newBackup != null &&
                    oldBackup.cellName.equals(newBackup.cellName) && oldBackup.isMainSchematics == newBackup.isMainSchematics)
                cellTreeChanged = true;
        }
        if (cellTreeChanged)
            System.out.println("Cell tree changed");
        Cell.updateAll(oldSnapshot, newSnapshot);
        if (libChanged || cellTreeChanged) {
            for (Library lib: libraries.values())
                lib.cells.clear();
            for (Cell cell: Cell.linkedCells) {
                if (cell == null) continue;
                Library lib = cell.getLibrary();
                assert lib.isLinked();
                lib.cells.put(cell.getCellName(), cell);
            }
        }
        return libChanged || cellTreeChanged;
    }
    
	/**
	 * Method to check and repair data structure errors in this Library.
	 */
	public int checkAndRepair(boolean repair, ErrorLogger errorLogger)
	{
		int errorCount = 0;
		boolean verbose = !isHidden();
		if (verbose)
		{
	        System.out.print("Checking " + this);
	        if (repair) System.out.print(" for repair");
		}

		for(Iterator<Cell> it = getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			errorCount += cell.checkAndRepair(repair, errorLogger);
		}
		if (errorCount != 0)
        {
            if (repair)
            {
				if (verbose) System.out.println("... library repaired");
			    setChanged();
            } else
			{
				if (verbose) System.out.println("... library has to be repaired");
			}
		} else
		{
			if (verbose) System.out.println("... library checked");
		}
		return errorCount;
	}

	/**
	 * Method to check invariants in this Library.
	 * @exception AssertionError if invariants are not valid
	 */
	protected void check()
	{
        super.check();
		assert d.libName != null;
		assert d.libName.length() > 0;
		assert d.libName.indexOf(' ') == -1 && d.libName.indexOf(':') == -1 : d.libName;
		HashSet<Cell.CellGroup> cellGroups = new HashSet<Cell.CellGroup>();
		String protoName = null;
		Cell.CellGroup cellGroup = null;
		for(Iterator<Map.Entry<CellName,Cell>> it = cells.entrySet().iterator(); it.hasNext(); )
		{
			Map.Entry<CellName,Cell> e = (Map.Entry<CellName,Cell>)it.next();
			CellName cn = (CellName)e.getKey();
			Cell cell = (Cell)e.getValue();
			assert cell.getCellName().equals(cn);
			assert cell.getLibrary() == this;
			if (protoName == null || !cell.getName().equals(protoName))
			{
				protoName = cell.getName();
				cellGroup = cell.getCellGroup();
				assert cellGroup != null : cell;
				cellGroups.add(cellGroup);
			}
			assert cell.getCellGroup() == cellGroup : cell;
			cell.check();
		}
		for (Iterator<Cell.CellGroup> it = cellGroups.iterator(); it.hasNext(); )
		{
			cellGroup = (Cell.CellGroup)it.next();
			cellGroup.check();
		}
	}

	private static boolean invariantsFailed = false;

	/**
	 * Method to check invariants in all Libraries.
	 * @return true if invariants are valid
	 */
	public static boolean checkInvariants()
	{
		try
		{
			//long startTime = System.currentTimeMillis();
            CellId.checkInvariants();
            
			TreeSet<String> libNames = new TreeSet<String>(String.CASE_INSENSITIVE_ORDER);
			for (Iterator<Map.Entry<String,Library>> it = libraries.entrySet().iterator(); it.hasNext(); )
			{
				Map.Entry<String,Library> e = (Map.Entry<String,Library>)it.next();
				String libName = (String)e.getKey();
				Library lib = (Library)e.getValue();
				assert libName.equals(lib.d.libName) : libName + " " + lib;
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

    private void setFlag(int mask, boolean value) {
        d = d.withFlags(value ? d.flags | mask : d.flags & ~mask);
    }
    
    private boolean isFlag(int mask) {
        return (d.flags & mask) != 0;
    }
    
    /**
	 * Method to indicate that this Library has changed in a major way.
	 * Major changes include creation, deletion, or modification of circuit elements.
	 */
	public void setChangedMajor() { setFlag(LIBCHANGEDMAJOR, true); }

	/**
	 * Method to indicate that this Library has not changed in a major way.
	 * Major changes include creation, deletion, or modification of circuit elements.
	 */
	private void clearChangedMajor() { clearCellChanges(); setFlag(LIBCHANGEDMAJOR, false); }

	/**
	 * Method to return true if this Library has changed in a major way.
	 * Major changes include creation, deletion, or modification of circuit elements.
	 * @return true if this Library has changed in a major way.
	 */
	public boolean isChangedMajor() { return isFlag(LIBCHANGEDMAJOR); }

	/**
	 * Method to indicate that this Library has changed in a minor way.
	 * Minor changes include changes to text and other things that are not essential to the circuitry.
	 */
	public void setChangedMinor() { setFlag(LIBCHANGEDMINOR, true); }

	/**
	 * Method to indicate that this Library has not changed in a minor way.
	 * Minor changes include changes to text and other things that are not essential to the circuitry.
	 */
	private void clearChangedMinor() { clearCellChanges(); setFlag(LIBCHANGEDMINOR, false); }

	/**
	 * Method to return true if this Library has changed in a minor way.
	 * Minor changes include changes to text and other things that are not essential to the circuitry.
	 * @return true if this Library has changed in a minor way.
	 */
	public boolean isChangedMinor() { return isFlag(LIBCHANGEDMINOR); }

	/**
	 * Method to indicate that this Library has changed regardless if they are major or minor.
	 * Major changes include creation, deletion, or modification of circuit elements.
     * Minor changes include changes to text and other things that are not essential to the circuitry.
	 */
	public void setChanged() { setChangedMinor(); setChangedMajor(); }

	/**
	 * Method to indicate that this Library has not changed in a major way.
	 * Major changes include creation, deletion, or modification of circuit elements.
	 */
	public void clearChanged() { clearCellChanges(); clearChangedMinor(); clearChangedMajor(); }

	/**
	 * Method to return true if this Library has changed in a major way.
	 * Major changes include creation, deletion, or modification of circuit elements.
	 * @return true if this Library has changed in a major way.
	 */
	public boolean isChanged() { return isChangedMinor() || isChangedMajor(); }

    /**
     * Method to clear modified cells if changes are clear in library.
     */
    private void clearCellChanges()
    {
		for (Iterator<Cell> it = cells.values().iterator(); it.hasNext();)
		{
			Cell c = (Cell) it.next();
            c.clearModified();
		}
    }

	/**
	 * Method to indicate that this Library came from disk.
	 * Libraries that come from disk are saved without a file-selection dialog.
	 */
	public void setFromDisk() { setFlag(READFROMDISK, true); }

	/**
	 * Method to indicate that this Library did not come from disk.
	 * Libraries that come from disk are saved without a file-selection dialog.
	 */
	public void clearFromDisk() { setFlag(READFROMDISK, false); }

	/**
	 * Method to return true if this Library came from disk.
	 * Libraries that come from disk are saved without a file-selection dialog.
	 * @return true if this Library came from disk.
	 */
	public boolean isFromDisk() { return isFlag(READFROMDISK); }

	/**
	 * Method to indicate that this Library is hidden.
	 * Hidden libraries are not seen by the user.
	 * For example, the "clipboard" library is hidden because it is only used
	 * internally for copying and pasting circuitry.
	 */
	public void setHidden() { setFlag(HIDDENLIBRARY, true); }

	/**
	 * Method to indicate that this Library is not hidden.
	 * Hidden libraries are not seen by the user.
	 * For example, the "clipboard" library is hidden because it is only used
	 * internally for copying and pasting circuitry.
	 */
	public void clearHidden() { setFlag(HIDDENLIBRARY, false); }

	/**
	 * Method to return true if this Library is hidden.
	 * Hidden libraries are not seen by the user.
	 * For example, the "clipboard" library is hidden because it is only used
	 * internally for copying and pasting circuitry.
	 * @return true if this Library is hidden.
	 */
	public boolean isHidden() { return isFlag(HIDDENLIBRARY); }

	/**
	 * Method to return the current Library.
	 * @return the current Library.
	 */
	public static Library getCurrent() { return curLib; }
	
	/**
	 * Method to make this the current Library.
	 */
	public void setCurrent() { curLib = this; }

	/**
	 * Method to get the Preferences associated with this Library.
	 * @return the Preferences associated with this Library.
	 */
	public Preferences getPrefs() { return prefs; }
	
	/**
	 * Low-level method to get the user bits.
	 * The "user bits" are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the ELIB
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @return the "user bits".
	 */
	public int lowLevelGetUserBits() { return d.flags; }
	
	/**
	 * Low-level method to set the user bits.
	 * The "user bits" are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the ELIB
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @param userBits the new "user bits".
	 */
	public void lowLevelSetUserBits(int userBits) { d = d.withFlags(userBits); }

	/**
	 * Get list of cells contained in other libraries
	 * that refer to cells contained in this library
	 * @param elib to search for
	 * @return list of cells refering to elements in this library
	 */
	public static Set<Cell> findReferenceInCell(Library elib)
	{
		TreeSet<Cell> set = new TreeSet<Cell>();

		/*for (Library l: libraries)*/
		for (Iterator<Library> it = libraries.values().iterator(); it.hasNext(); )
		{
			Library l = (Library)it.next();
			// skip itself
			if (l == elib) continue;

			for (Iterator<Cell> cIt = l.cells.values().iterator(); cIt.hasNext(); )
			{
				Cell cell = (Cell) cIt.next();
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
	public static Library findLibrary(String libName)
	{
		if (libName == null) return null;
		Library lib = (Library)libraries.get(libName);
		if (lib != null) return lib;

		/*for (Library l: libraries)*/
		for (Iterator<Library> it = libraries.values().iterator(); it.hasNext(); )
		{
			Library l = (Library)it.next();
			if (l.getName().equalsIgnoreCase(libName))
				return l;
		}
		return null;
	}

	/**
	 * Method to return an iterator over all libraries.
	 * @return an iterator over all libraries.
	 */
	public static Iterator<Library> getLibraries()
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
	public static int getNumLibraries()
	{
		return libraries.size();
	}

	/**
	 * Method to return an iterator over all visible libraries.
	 * @return an iterator over all visible libraries.
	 */
	public static List<Library> getVisibleLibraries()
	{
        synchronized(libraries) {
			ArrayList<Library> visibleLibraries = new ArrayList<Library>();
			/*for (Library lib: libraries)*/
			for (Iterator<Library> it = libraries.values().iterator(); it.hasNext(); )
			{
				Library lib = (Library)it.next();
				if (!lib.isHidden()) visibleLibraries.add(lib);
			}
			return visibleLibraries;
        }
	}

	/**
	 * Method to return the name of this Library.
	 * @return the name of this Library.
	 */
	public String getName() { return d.libName; }

	/**
	 * Method to set the name of this Library.
	 * @param libName the new name of this Library.
	 * @return false if the library was renamed.
	 * True on error.
	 */
	public boolean setName(String libName)
	{
		if (d.libName.equals(libName)) return true;

		// make sure the name is legal
        if (legalLibraryName(libName) != libName) {
            System.out.println("Error: '"+libName+"' is not a valid name");
            return true;
        }
		Library already = findLibrary(libName);
		if (already != null)
		{
			System.out.println("Already a library called " + already.getName());
			return true;
		}

		String oldName = d.libName;
		lowLevelRename(libName);
		Undo.renameObject(this, oldName);
		return false;
	}

	/**
	 * Method to rename this Library.
	 * This method is for low-level use by the database, and should not be called elsewhere.
	 * @param libName the new name of the Library.
	 */
	public void lowLevelRename(String libName)
	{
		String newLibFile = TextUtils.getFilePath(d.libFile) + libName;
		String extension = TextUtils.getExtension(d.libFile);
		if (extension.length() > 0) newLibFile += "." + extension;
		URL libFile = TextUtils.makeURLToFile(newLibFile);

        libraries.remove(d.libName);
        d = d.withName(libName, libFile);
		libraries.put(libName, this);
        
        Cell curCell = getCurCell();
        prefs = allPrefs.node(libName);
        prefs.put("LIB", libName);
        curCellPref = null;
        setCurCell(curCell);
        for (Iterator<Cell> it = getCells(); it.hasNext(); ) {
            Cell cell = (Cell)it.next();
            cell.expandStatusChanged();
        }
	}

    /**
     * Checks that string is legal library name.
     * If not, tries to convert string to legal library name.
     * @param libName specified library name.
     * @return legal library name obtained from specified library name,
     *   or null if speicifed name is null or empty.
     */
    public static String legalLibraryName(String libName) {
        if (libName == null || libName.length() == 0) return null;
        int i = 0;
        for (; i < libName.length(); i++) {
            char ch = libName.charAt(i);
            if (Character.isWhitespace(ch) || ch == ':') break;
        }
        if (i == libName.length()) return libName;
        
        char[] chars = libName.toCharArray();
        for (; i < libName.length(); i++) {
            char ch = chars[i];
            chars[i] = Character.isWhitespace(ch) || ch == ':' ? '-' : ch;
        }
        return new String(chars);
    }
    
	/**
	 * Method to return the URL of this Library.
	 * @return the URL of this Library.
	 */
	public URL getLibFile() { return d.libFile; }

	/**
	 * Method to set the URL of this Library.
	 * @param libFile the new URL of this Library.
	 */
	public void setLibFile(URL libFile)
	{
		d = d.withName(d.libName, libFile);
	}

    /**
     * Compares two <code>Library</code> objects.
     * @param that the Library to be compared.
     * @return the result of comparison.
     */
/*5*/public int compareTo(Library that)
//4*/public int compareTo(Object o)
	{
//4*/	Library that = (Library)o;
		return TextUtils.STRING_NUMBER_ORDER.compare(d.libName, that.d.libName);
    }

	/**
	 * Returns a printable version of this Library.
	 * @return a printable version of this Library.
	 */
	public String toString()
	{
		return "library '" + d.libName + "'";
	}

	// ----------------- cells --------------------

	private void getCurCellPref()
	{
		if (curCellPref == null)
		{
			curCellPref = Pref.makeStringPref("CurrentCell", prefs, "");
		}
        
	}

	/**
	 * Method to get the current Cell in this Library.
	 * @return the current Cell in this Library.
	 * Returns NULL if there is no current Cell.
	 */
	public Cell getCurCell()
	{
		getCurCellPref();
		String cellName = curCellPref.getString();
		if (cellName.length() == 0) return null;
		Cell cell = this.findNodeProto(cellName);
		if (cell == null) curCellPref.setString("");
		return cell;
	}

	/**
	 * Method to set the current Cell in this Library.
	 * @param curCell the new current Cell in this Library.
	 */
	public void setCurCell(Cell curCell)
	{
		getCurCellPref();
		String cellName = "";
		if (curCell != null) cellName = curCell.noLibDescribe();
		curCellPref.setString(cellName);
	}

    /**
     * Method to save isExpanded status of NodeInsts in this Library to Preferences.
     */
    public static void saveExpandStatus() throws BackingStoreException {
            for (Iterator<Library> lit = getLibraries(); lit.hasNext(); ) {
                Library lib = (Library)lit.next();
                for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
                    Cell cell = (Cell)it.next();
                    cell.saveExpandStatus();
                }
                lib.prefs.flush();
            }
    }

    public static Cell findCellInLibraries(String name, View view)
    {
        for (Iterator<Library> it = Library.getLibraries(); it.hasNext();)
        {
            Library lib = (Library)it.next();
            for (Iterator<Cell> cIt = lib.getCells(); cIt.hasNext();)
            {
                Cell cell = (Cell)cIt.next();
                if (cell.getName().indexOf(name) != -1)
                {
                    // Either first match in name or check that view matches
                    if (view == null || cell.getView() == view)
                        return cell;
                }
            }
        }
        return null;
    }

    /**
	 * Method to find the Cell with the given name in this Library.
	 * @param name the name of the desired Cell.
	 * @return the Cell with the given name in this Library.
	 */
	public Cell findNodeProto(String name)
	{
		if (name == null) return null;
		CellName n = CellName.parseName(name);
		if (n == null) return null;
		Cell cell = (Cell)cells.get(n);
		if (cell != null) return cell;

		Cell onlyWithName = null;
		for (Iterator<Cell> it = cells.values().iterator(); it.hasNext();)
		{
			Cell c = (Cell) it.next();
			if (!n.getName().equalsIgnoreCase(c.getName())) continue;
			onlyWithName = c;
			if (n.getView() != c.getView()) continue;
			if (n.getVersion() > 0 && n.getVersion() != c.getVersion()) continue;
			if (n.getVersion() == 0 && c.getNewestVersion() != c) continue;
			return c;
		}
		if (n.getView() == View.UNKNOWN && onlyWithName != null) return onlyWithName;
		return null;
	}

	/**
	 * Returns true, if cell is contained in this Library.
	 */
	boolean contains(Cell cell) { return cells.get(cell.getCellName()) == cell; }

	/**
	 * Method to return an Iterator over all Cells in this Library.
	 * @return an Iterator over all Cells in this Library.
	 */
	public Iterator<Cell> getCells()
	{
        synchronized(cells) {
			ArrayList<Cell> cellsCopy = new ArrayList<Cell>(cells.values());
			return cellsCopy.iterator();
        }
	}

	/**
	 * Method to return an Iterator over all Cells in this Library after given CellName.
	 * @param cn starting CellName
	 * @return an Iterator over all Cells in this Library after given CellName.
	 */
	Iterator<Cell> getCellsTail(CellName cn)
	{
		return cells.tailMap(cn).values().iterator();
	}

	/**
	 * Returns verison of Electric which wrote this library.
	 * Returns null for ReadableDumps, for new libraries and for dummy libraries.
	 * @return version
	 */
	public Version getVersion()
	{
		return d.version;
	}

	/**
	 * Method to set library version found in header.
	 * @param version
	 */
	public void setVersion(Version version)
	{
		d = d.withVersion(version);
	}
}
