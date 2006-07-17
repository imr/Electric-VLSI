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

import com.sun.electric.database.EObjectInputStream;
import com.sun.electric.database.IdMapper;
import com.sun.electric.database.ImmutableElectricObject;
import com.sun.electric.database.ImmutableLibrary;
import com.sun.electric.database.LibId;
import com.sun.electric.database.LibraryBackup;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.constraint.Constraints;
import com.sun.electric.database.constraint.Layout;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ErrorLogger;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.io.ObjectStreamException;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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

//	/** library has changed significantly */				private static final int LIBCHANGEDMAJOR =           01;
//	/** set to see library in explorer */					private static final int OPENINEXPLORER =            02;
	/** set if library came from disk */					private static final int READFROMDISK =              04;
//	/** internal units in library (see INTERNALUNITS) */	private static final int LIBUNITS =                 070;
//	/** right shift for LIBUNITS */							private static final int LIBUNITSSH =                 3;
//	/** library has changed insignificantly */				private static final int LIBCHANGEDMINOR =         0100;
	/** library is "hidden" (clipboard library) */			public static final int HIDDENLIBRARY =           0200;
//	/** library is unwanted (used during input) */			private static final int UNWANTEDLIB =             0400;

    /** Database to which this Library belongs. */          private final EDatabase database;
    /** persistent data of this Library. */                 private ImmutableLibrary d;
    /** true of library needs saving. */                    private boolean modified;
    /** list of referenced libs */                          private final List<Library> referencedLibs = new ArrayList<Library>();
    /** Last backup of this Library */                      LibraryBackup backup;
    /** True if library matches lib backup. */              boolean libBackupFresh;
	/** list of Cells in this library */					final TreeMap<CellName,Cell> cells = new TreeMap<CellName,Cell>();
	/** Preference for cell currently being edited */		private Pref curCellPref;
    /** preferences for this library */                     Preferences prefs;
    /** DELIB cell files. */                                private HashSet<String> delibCellFiles = new HashSet<String>();

	/** preferences for all libraries */					private static Preferences allPrefs = null;
	/** the current library in Electric */					private static Library curLib = null;

	// ----------------- private and protected methods --------------------

	/**
	 * The constructor is never called.  Use the factor method "newInstance" instead.
	 */
	Library(EDatabase database, ImmutableLibrary d) {
        if (database == null) throw new NullPointerException();
        this.database = database;
        this.d = d;
		if (allPrefs == null) allPrefs = Preferences.userNodeForPackage(getClass());
        prefs = allPrefs.node(getName());
        prefs.put("LIB", getName());
	}

	/**
	 * This method is a factory to create new libraries.
	 * A Library has both a name and a file.
	 * @param libName the name of the library (for example, "gates").
	 * Library names must be unique, and they must not contain spaces or colons.
	 * @param libFile the URL to the disk file (for example "/home/strubin/gates.elib").
	 * If the Library is being created, the libFile can be null.
	 * If the Library file is given and it points to an existing file, then the I/O system
	 * can be told to read that file and populate the Library.
	 * @return the Library object.
	 */
	public static Library newInstance(String libName, URL libFile)
	{
		// make sure the name is legal
        String legalName = LibId.legalLibraryName(libName);
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
        EDatabase database = EDatabase.theDatabase;
        return newInstance(database, database.getIdManager().newLibId(legalName), libFile);
	}

	/**
	 * This method is a factory to create new libraries.
	 * A Library has both a name and a file.
     * @param libId ID of new Library. 
	 * @param libFile the URL to the disk file (for example "/home/strubin/gates.elib").
	 * If the Library is being created, the libFile can be null.
	 * If the Library file is given and it points to an existing file, then the I/O system
	 * can be told to read that file and populate the Library.
	 * @return the Library object.
     * @throws NullPointerException if libId or legalName is null.
	 */
	private static Library newInstance(EDatabase edb, LibId libId, URL libFile) {
		// create the library
        ImmutableLibrary d = ImmutableLibrary.newInstance(libId, libFile, null);
		Library lib = new Library(edb, d);

		// add the library to the global list
        edb.addLib(lib);

        // always broadcast library changes
//        Undo.setNextChangeQuiet(false);
        edb.unfreshSnapshot();
        Constraints.getCurrent().newObject(lib);
		return lib;
        
    }
    
    /**
     * Method for serialization.
     */
    private Object writeReplace() throws ObjectStreamException { return new LibraryKey(this); }
    private Object readResolve() throws ObjectStreamException { throw new InvalidObjectException("Library"); }
    
    private static class LibraryKey extends EObjectInputStream.Key {
        LibId libId;
        
        private LibraryKey(Library lib) throws NotSerializableException {
            if (!lib.isLinked())
                throw new NotSerializableException(lib.toString());
            libId = lib.getId();
        }
        
        protected Object readResolveInDatabase(EDatabase database) throws InvalidObjectException {
            Library lib = database.getLib(libId);
            if (lib == null) throw new InvalidObjectException("Library");
            return lib;
        }
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
            curLib = null;

//		{
//			// find another library
//			for (Library lib : database.libraries.values())
//			{
//				if (lib == curLib) continue;
//				if (lib.isHidden()) continue;
//				newCurLib = lib;
//				break;
//			}
//			if (newCurLib == null)
//			{
//				System.out.println("Cannot delete the last library");
//				Job.getUserInterface().showInformationMessage("Cannot delete the last "+toString(),
//					"Close library");
//				return false;
//			}
//		}

		// make sure it is in the list of libraries
		if (database.libraries.get(getName()) != this)
		{
			System.out.println("Cannot delete library " + this);
			Job.getUserInterface().showErrorMessage("Cannot delete "+toString(),
				"Close library");
			return false;
		}

		// make sure none of these cells are referenced by other libraries
		boolean referenced = false;
		for (Library lib : database.libraries.values())
		{
			if (lib == this) continue;
			for(Iterator<Cell> cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = cIt.next();
				for(Iterator<NodeInst> nIt = cell.getNodes(); nIt.hasNext(); )
				{
					NodeInst ni = nIt.next();
					if (ni.isCellInstance())
					{
						Cell subCell = (Cell)ni.getProto();
						if (subCell.getLibrary() == this)
						{
							Job.getUserInterface().showErrorMessage("Library close failed. Cannot " + reason + " " + toString() +
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
        for (Library lib: database.libraries.values()) {
            if (lib == this) continue;
            lib.removeReferencedLib(this);
        }

		// remove it from the list of libraries
        database.removeLib(getId());

		// set the new current library if appropriate
		if (newCurLib != null) newCurLib.setCurrent();

		// always broadcast library changes
//		Undo.setNextChangeQuiet(false);
        database.unfreshSnapshot();
		Constraints.getCurrent().killObject(this);
		return true;
	}

	/**
	 * Method to remove all contents from this Library.
	 */
	public void erase()
	{
		// remove all cells in the library
        for (Iterator<Cell> it = getCells(); it.hasNext(); ) {
            Cell c = it.next();
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
            updateNewestVersions();
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
            c.newestVersion = null;
            updateNewestVersions();
		}
        setChanged();
	}
    
    /**
     * Collect cells from database snapshot into cells list of this Library.
     */
    void collectCells() {
        synchronized(cells) {
            cells.clear();
            for (int cellIndex = 0; cellIndex < database.linkedCells.size(); cellIndex++) {
                Cell cell = database.getCell(cellIndex);
                if (cell == null || cell.getLibrary() != this) continue;
                cells.put(cell.getCellName(), cell);
            }
            updateNewestVersions();
        }
    }
    
    /**
     * Update newestVersion fields of cells in this Library.
     */
    private void updateNewestVersions() {
        Cell newestVersion = null;
        for (Cell cell: cells.values()) {
            if (newestVersion == null || !newestVersion.getName().equals(cell.getName()) || newestVersion.getView() != cell.getView())
                newestVersion = cell;
            cell.newestVersion = newestVersion;
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
            for (Library refLib : libDependencies) {

                // startLib references refLib. Find out why
                boolean found = false;
                // find a cell instance that creates dependency
                for (Iterator<Cell> itCell = startLib.getCells(); itCell.hasNext(); ) {
                    Cell c = itCell.next();
                    for (Iterator<NodeInst> it = c.getNodes(); it.hasNext(); ) {
                        NodeInst ni = it.next();
                        if (ni.isCellInstance()) {
                            Cell cc = (Cell)ni.getProto();
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
            unfreshBackup();
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
            Cell c = itCell.next();
            for (Iterator<NodeInst> it = c.getNodes(); it.hasNext(); ) {
                NodeInst ni = it.next();
                if (ni.isCellInstance()) {
                    Cell cc = (Cell)ni.getProto();
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
                unfreshBackup();
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
        for (Library reflib : reflibsCopy) {
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
                Cell libCell = it.next();
                Cell instance = it.next();
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
//        setChanged();
        assert isLinked();
        unfreshBackup();
        Constraints.getCurrent().modifyLibrary(this, oldD);
        return true;
    }

    /**
     * Returns persistent data of this ElectricObject.
     * @return persistent data of this ElectricObject.
     */
    public ImmutableElectricObject getImmutable() { return d; }
    
    /**
     * Method to add a Variable on this Library.
     * It may add repaired copy of this Variable in some cases.
     * @param var Variable to add.
     */
    public void addVar(Variable var) {
        setD(getD().withVariable(var));
    }

	/**
	 * Method to delete a Variable from this Library.
	 * @param key the key of the Variable to delete.
	 */
	public void delVar(Variable.Key key)
	{
        setD(getD().withoutVariable(key));
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
    public static Library inCurrentThread(LibId libId) { return EDatabase.theDatabase.getLib(libId); }
    
    /**
     * Returns true if this Library is linked into database.
     * @return true if this Library is linked into database.
     */
	public boolean isLinked()
	{
        return inCurrentThread(getId()) == this;
	}

	/**
	 * Returns database to which this Library belongs.
     * @return database to which this Library belongs.
	 */
	public EDatabase getDatabase() { return database; }

     /*
	 * Low-level method to backup this Library to LibraryBackup.
     * @return LibraryBackup which is the backup of this Library.
	 */
    public LibraryBackup backup() {
        if (libBackupFresh) return backup;
        LibId[] oldRefs = backup != null ? backup.referencedLibs : new LibId[0];
        LibId[] newRefs = backupReferencedLibs(oldRefs);
        if (backup == null || d != backup.d || modified != backup.modified || newRefs != backup.referencedLibs)
            backup = new LibraryBackup(d, modified, newRefs);
        libBackupFresh = true;
        return backup;
    }
    
    void recover(LibraryBackup recoverBackup) {
        checkUndoing();
        backup = recoverBackup;
        this.d = recoverBackup.d;
        this.modified = recoverBackup.modified;
        referencedLibs.clear();
        for (LibId libId: recoverBackup.referencedLibs)
            referencedLibs.add(database.getLib(libId));
        libBackupFresh = true;
    }
    
    void checkFresh(LibraryBackup libBackup) {
        assert d == libBackup.d;
        assert modified == libBackup.modified;
        assert libBackup.referencedLibs.length == referencedLibs.size();
        for (int i = 0; i < libBackup.referencedLibs.length; i++)
            assert libBackup.referencedLibs[i] == referencedLibs.get(i).getId();
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
			Cell cell = it.next();
			errorCount += cell.checkAndRepair(repair, errorLogger);
		}
		if (errorCount != 0)
        {
            if (repair)
            {
				if (verbose) System.out.println("... library repaired");
//			    setChanged();
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
        if (libBackupFresh) {
            assert getD() == backup.d;
            assert modified == backup.modified;
            assert backup.referencedLibs.length == referencedLibs.size();
        }
        super.check();
        String libName = d.libId.libName;
		assert libName != null;
		assert libName.length() > 0;
		assert libName.indexOf(' ') == -1 && libName.indexOf(':') == -1 : libName;
        for (int i = 0; i < referencedLibs.size(); i++) {
            Library rLib = referencedLibs.get(i);
            assert rLib.isLinked() && rLib.database == database;
            if (libBackupFresh) assert backup.referencedLibs[i] == referencedLibs.get(i).getId();
        }
		HashSet<Cell.CellGroup> cellGroups = new HashSet<Cell.CellGroup>();
		String protoName = null;
		Cell.CellGroup cellGroup = null;
        Cell newestVersion = null;
		for(Map.Entry<CellName,Cell> e : cells.entrySet())
		{
			CellName cn = (CellName)e.getKey();
			Cell cell = (Cell)e.getValue();
            assert cell.isLinked() && cell.getDatabase() == database;
			assert cell.getCellName() == cn;
			assert cell.getLibrary() == this;
			if (protoName == null || !cell.getName().equals(protoName))
			{
				protoName = cell.getName();
				cellGroup = cell.getCellGroup();
				assert cellGroup != null : cell;
				cellGroups.add(cellGroup);
                newestVersion = cell;
			}
            if (cell.getView() != newestVersion.getView())
                newestVersion = cell;
			assert cell.getCellGroup() == cellGroup : cell;
            assert cell.newestVersion == newestVersion;
		}
		for (Iterator<Cell.CellGroup> it = cellGroups.iterator(); it.hasNext(); )
		{
			cellGroup = it.next();
			cellGroup.check();
		}
	}

    private void setFlag(int mask, boolean value) {
        setD(d.withFlags(value ? d.flags | mask : d.flags & ~mask));
    }
    
    private boolean isFlag(int mask) {
        return (d.flags & mask) != 0;
    }
    
	/**
	 * Method to indicate that this Library has changed.
	 */
	public void setChanged() {
        checkChanging();
        if (!modified)
            unfreshBackup();
        modified = true;
    }

	/**
	 * Method to indicate that this Library has not changed.
	 */
	public void clearChanged() {
        checkChanging();
        for (Cell cell: cells.values())
            cell.clearModified();
        if (modified)
            unfreshBackup();
        modified = false;
    }

    private void unfreshBackup() {
        libBackupFresh = false;
        database.unfreshSnapshot();
    }
    
	/**
	 * Method to return true if this Library has changed.
	 * @return true if this Library has changed.
	 */
	public boolean isChanged() { return modified; }

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
	public void lowLevelSetUserBits(int userBits) { setD(d.withFlags(userBits)); }

	/**
	 * Get list of cells contained in other libraries
	 * that refer to cells contained in this library
	 * @param elib to search for
	 * @return list of cells refering to elements in this library
	 */
	public static Set<Cell> findReferenceInCell(Library elib) { return EDatabase.theDatabase.findReferenceInCell(elib); }

	/**
	 * Method to find a Library with the specified name.
	 * @param libName the name of the Library.
	 * Note that this is the Library name, and not the Library file.
	 * @return the Library, or null if there is no known Library by that name.
	 */
	public static Library findLibrary(String libName) { return EDatabase.theDatabase.findLibrary(libName); }

	/**
	 * Method to return an iterator over all libraries.
	 * @return an iterator over all libraries.
	 */
	public static Iterator<Library> getLibraries() { return EDatabase.theDatabase.getLibraries(); }

	/**
	 * Method to return the number of libraries.
	 * @return the number of libraries.
	 */
	public static int getNumLibraries()	{ return EDatabase.theDatabase.getNumLibraries(); }
    
	/**
	 * Method to return an iterator over all visible libraries.
	 * @return an iterator over all visible libraries.
	 */
	public static List<Library> getVisibleLibraries() { return EDatabase.theDatabase.getVisibleLibraries(); }

	/**
	 * Method to return the name of this Library.
	 * @return the name of this Library.
	 */
	public String getName() { return d.libId.libName; }

	/**
	 * Method to set the name of this Library.
	 * @param libName the new name of this Library.
	 * @return mapping of Library/Cell/Export ids, null if the library was renamed.
	 */
	public IdMapper setName(String libName)
	{
		if (d.libId.libName.equals(libName)) return null;

		// make sure the name is legal
        if (LibId.legalLibraryName(libName) != libName) {
            System.out.println("Error: '"+libName+"' is not a valid name");
            return null;
        }
		Library already = findLibrary(libName);
		if (already != null)
		{
			System.out.println("Already a library called " + already.getName());
			return null;
		}

        Snapshot oldSnapshot = database.backup();
        LibId newLibId = oldSnapshot.idManager.newLibId(libName);
        IdMapper idMapper = IdMapper.renameLibrary(oldSnapshot, d.libId, newLibId);
        Snapshot newSnapshot = oldSnapshot.withRenamedIds(idMapper);
        LibraryBackup[] libBackups = newSnapshot.libBackups.toArray(new LibraryBackup[newSnapshot.libBackups.size()]);
        LibraryBackup libBackup = libBackups[newLibId.libIndex];
        
        String newLibFile = TextUtils.getFilePath(d.libFile) + libName;
        String extension = TextUtils.getExtension(d.libFile);
        if (extension.length() > 0) newLibFile += "." + extension;
        URL libFile = TextUtils.makeURLToFile(newLibFile);
        libBackups[newLibId.libIndex] = new LibraryBackup(libBackup.d.withLibFile(libFile), true, libBackup.referencedLibs);
        newSnapshot = newSnapshot.with(null, null, null, libBackups);
        checkChanging();
        boolean isCurrent = getCurrent() == this;
        database.lowLevelSetCanUndoing(true);
        database.undo(newSnapshot);
        database.lowLevelSetCanUndoing(false);
        Constraints.getCurrent().renameIds(idMapper);
        Library newLib = database.getLib(newLibId);
        if (isCurrent)
            newLib.setCurrent();
        return idMapper;
        
//		String oldName = d.libId.libName;
//		lowLevelRename(libName);
//		Constraints.getCurrent().renameObject(this, oldName);
////        setChanged();
//        assert isLinked();
//        database.unfreshSnapshot();
//        for (Cell cell: cells.values())
//            cell.notifyRename();
//		return this;
	}

//	/**
//	 * Method to rename this Library.
//	 * This method is for low-level use by the database, and should not be called elsewhere.
//	 * @param libName the new name of the Library.
//	 */
//	private void lowLevelRename(String libName)
//	{
//		String newLibFile = TextUtils.getFilePath(d.libFile) + libName;
//		String extension = TextUtils.getExtension(d.libFile);
//		if (extension.length() > 0) newLibFile += "." + extension;
//		URL libFile = TextUtils.makeURLToFile(newLibFile);
//
//        database.libraries.remove(d.libName);
//        setD(d.withName(libName, libFile));
//		database.libraries.put(libName, this);
//        assert isLinked();
//        database.unfreshSnapshot();
//        
//        Cell curCell = getCurCell();
//        prefs = allPrefs.node(libName);
//        prefs.put("LIB", libName);
//        curCellPref = null;
//        setCurCell(curCell);
//        for (Iterator<Cell> it = getCells(); it.hasNext(); ) {
//            Cell cell = it.next();
//            cell.expandStatusChanged();
//        }
//	}

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
		setD(d.withLibFile(libFile));
	}

    /**
     * Compares two <code>Library</code> objects.
     * @param that the Library to be compared.
     * @return the result of comparison.
     */
	public int compareTo(Library that)
	{
		return TextUtils.STRING_NUMBER_ORDER.compare(getName(), that.getName());
    }

	/**
	 * Returns a printable version of this Library.
	 * @return a printable version of this Library.
	 */
	public String toString()
	{
		return "library '" + getName() + "'";
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
                Library lib = lit.next();
                for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
                    Cell cell = it.next();
                    cell.saveExpandStatus();
                }
                lib.prefs.flush();
            }
    }

    public static Cell findCellInLibraries(String cellName, View view, String libraryName)
    {
        if (libraryName != null)
        {
            Library lib = findLibrary(libraryName);
            if (lib != null)
            {
                Cell cell = lib.findNodeProto(cellName);
                // Either first match in name or check that view matches
                if (cell != null && (view == null || cell.getView() == view))
                    return cell;
            }
            // search in other libraries if no library is found with given name
        }

        for (Iterator<Library> it = Library.getLibraries(); it.hasNext();)
        {
            Library lib = it.next();
            Cell cell = lib.findNodeProto(cellName);
            // Either first match in name or check that view matches
            if (cell != null && (view == null || cell.getView() == view))
                return cell;
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
        synchronized (cells) {
            Cell cell = cells.get(n);
            if (cell != null) return cell;
            
            Cell onlyWithName = null;
            for (Cell c : cells.values()) {
                if (!n.getName().equalsIgnoreCase(c.getName())) continue;
                onlyWithName = c;
                if (n.getView() != c.getView()) continue;
                if (n.getVersion() > 0 && n.getVersion() != c.getVersion()) continue;
                if (n.getVersion() == 0 && c.getNewestVersion() != c) continue;
                return c;
            }
            if (n.getView() == View.UNKNOWN && onlyWithName != null) return onlyWithName;
        }
		return null;
	}

    public int getNumCells()
    {
        synchronized(cells) { return cells.size(); }
    }

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
        synchronized(cells) {
            return cells.tailMap(cn).values().iterator();
        }
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
		setD(d.withVersion(version));
	}
    
    /**
     * Returns DELIB cell files.
     * @return DELIB cell files.
     */
    public Set<String> getDelibCellFiles() {
        return delibCellFiles;
    }
    
    /**
     * Sets DELIB cell files.
     * @param delibCellFiles DELIB cell files.
     */
    public void setDelibCellFiles(HashSet<String> delibCellFiles) {
        this.delibCellFiles.clear();
        this.delibCellFiles.addAll(delibCellFiles);
    }
}
