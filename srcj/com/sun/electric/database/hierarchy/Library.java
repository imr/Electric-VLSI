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

import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.tool.user.ui.ExplorerTree;

import java.util.List;
import java.util.Comparator;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.File;

/**
 * A Library represents a collection of Cells.
 * To get hold of a Library, you use Electric.getLibrary(String name)
 * Electric.newLibrary(String name) to create a brand new library, or
 * Electric.getCurrent() to get the current Library.
 * <p>
 * Once you have a Library, you can create a new Cell, find an existing
 * Cell, get an Enumeration of all Cells, or find the Cell that the user
 * is currently editing.
 */
public class Library extends ElectricObject
{
	// ------------------------ private data ------------------------------

	/** library has changed significantly */				private static final int LIBCHANGEDMAJOR =           01;
//	/** set to see library in explorer */					private static final int OPENINEXPLORER =            02;
	/** set if library came from disk */					private static final int READFROMDISK =              04;
	/** internal units in library (see INTERNALUNITS) */	private static final int LIBUNITS =                 070;
	/** right shift for LIBUNITS */							private static final int LIBUNITSSH =                 3;
	/** library has changed insignificantly */				private static final int LIBCHANGEDMINOR =         0100;
	/** library is "hidden" (clipboard library) */			private static final int HIDDENLIBRARY =           0200;
//	/** library is unwanted (used during input) */			private static final int UNWANTEDLIB =             0400;

	/** name of this library  */							private String libName;
	/** file location of this library */					private String libFile;
	/** list of Cells in this library */					private ArrayList cells;
	/** Cell currently being edited */						private Cell curCell;
	/** flag bits */										private int userBits;

	/** static list of all libraries in Electric */			private static List libraries = new ArrayList();
	/** counter for enumerating libraries */				private static int libNumber = 0;
	/** the current library in Electric */					private static Library curLib = null;

	// ----------------- private and protected methods --------------------

	/**
	 * A Name is a text-parsing object for Library names.
	 * Library names have a full path, a file name, and an extension.
	 */
	public static class Name
	{
		private String path;
		private String name;
		private String extension;

		private Name() {}

		/**
		 * Routine to return the path to the Library name.
		 * The path does not include the actual file name or extension.
		 * @return the path to the Library name.
		 */
		public String getPath() { return path; }

		/**
		 * Routine to return the name of the Library name.
		 * The name does not include the full path to the file or extension.
		 * @return the name of the Library.
		 */
		public String getName() { return name; }

		/**
		 * Routine to set the name of the Library name.
		 * The name does not include the full path to the file or extension.
		 * @param name the name of the Library.
		 */
		public void setName(String name) { this.name = name; }

		/**
		 * Routine to return the extension of the Library name.
		 * The extension does not include the dot, so it is "elib" or "txt", etc.
		 * @return the extension of the Library.
		 */
		public String getExtension() { return extension; }

		/**
		 * Routine to set the extension of the Library name.
		 * The extension does not include the dot, so it is "elib" or "txt", etc.
		 * @param extension the extension of the Library name.
		 */
		public void setExtension(String extension) { this.extension = extension; }

		/**
		 * Routine to create a new Name object from the specified Library file name.
		 * @param libFile the full name of the Library disk file.
		 * @return a Name object with the path, file name, and extension broken out.
		 */
		public static Name newInstance(String libFile)
		{
			// figure out the actual library name and library file
			Name n = new Name();
			File f = new File(libFile);
			n.path = f.getParent();
			n.name = f.getName();
			int dotPos = n.name.lastIndexOf('.');
			if (dotPos < 0) n.extension = null; else
			{
				n.extension = n.name.substring(dotPos+1);
				n.name = n.name.substring(0, dotPos);
			}
			return n;
		}

		/**
		 * Routine to convert this Name object to a full path name.
		 * @return the full path name to the Library file.
		 */
		public String makeName()
		{
			String libFile = path + File.separator + name;
			if (extension != null) libFile += "." + extension;
			return libFile;
		}
	}

	/**
	 * The constructor is never called.  Use the factor method "newInstance" instead.
	 */
	private Library()
	{
		setIndex(libNumber++);
	}

	/**
	 * This routine is a factory to create new libraries.
	 * A Library has both a name and a file.
	 * @param libName the name of the library (for example, "gates").
	 * Library names must be unique, and they must not contain spaces.
	 * @param libFile the full path to the disk file (for example "/home/strubin/gates.elib").
	 * If the Library is being created, the libFile can be null.
	 * If the Library file is given and it points to an existing file, then the I/O system
	 * can be told to read that file and populate the Library.
	 * @return the Library object.
	 */
	public static Library newInstance(String libName, String libFile)
	{
		// make sure the name is legal
		String legalName = libName.replace(' ', '-');
		if (!legalName.equalsIgnoreCase(libName))
			System.out.println("Warning: library renamed to '" + legalName + "'");
		
		// see if the library name already exists
		Library existingLibrary = Library.findLibrary(legalName);
		if (existingLibrary != null)
		{
			System.out.println("Error: library '" + legalName + "' already exists");
			return null;
		}
		
		// create the library
		Library lib = new Library();
		lib.cells = new ArrayList();
		lib.curCell = null;
		lib.libName = legalName;
		lib.libFile = libFile;
	
		// add the library to the global list
		libraries.add(lib);

		return lib;
	}

	/**
	 * Routine to delete this Library.
	 */
	public void kill()
	{
		// cannot delete the current library
		if (curLib == this)
		{
			System.out.println("Cannot delete the current library");
			return;
		}

		// make sure it is in the list of libraries
		if (!libraries.contains(this))
		{
			System.out.println("Cannot delete library " + this);
			return;
		}

		// remove all cells in the library
		erase();

		// remove it from the list of libraries
		libraries.remove(this);
	}

	/**
	 * Routine to remove all contents from this Library.
	 */
	public void erase()
	{
		// remove all cells in the library
		cells.clear();
	}

	void addCell(Cell c)
	{
		// sanity check: make sure Cell isn't already in the list
		if (cells.contains(c))
		{
			System.out.println("Tried to re-add a cell to a library: " + c);
			return;
		}
		cells.add(c);
		ExplorerTree.explorerTreeChanged();
	}

	void removeCell(Cell c)
	{
		// sanity check: make sure Cell is in the list
		if (!cells.contains(c))
		{
			System.out.println("Tried to remove a non-existant Cell from a library: " + c);
			return;
		}
		cells.remove(c);
		ExplorerTree.explorerTreeChanged();
	}

	// ----------------- public interface --------------------

	/**
	 * Routine to check and repair data structure errors in this Library.
	 */
	public int checkAndRepair()
	{
		int errorCount = 0;
		for(Iterator it = getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			errorCount += cell.checkAndRepair();
		}
		return errorCount;
	}

	/**
	 * Routine to indicate that this Library has changed in a major way.
	 * Major changes include creation, deletion, or modification of circuit elements.
	 */
	public void setChangedMajor() { userBits |= LIBCHANGEDMAJOR; }

	/**
	 * Routine to indicate that this Library has not changed in a major way.
	 * Major changes include creation, deletion, or modification of circuit elements.
	 */
	public void clearChangedMajor() { userBits &= ~LIBCHANGEDMAJOR; }

	/**
	 * Routine to return true if this Library has changed in a major way.
	 * Major changes include creation, deletion, or modification of circuit elements.
	 * @return true if this Library has changed in a major way.
	 */
	public boolean isChangedMajor() { return (userBits & LIBCHANGEDMAJOR) != 0; }

	/**
	 * Routine to indicate that this Library has changed in a minor way.
	 * Minor changes include changes to text and other things that are not essential to the circuitry.
	 */
	public void setChangedMinor() { userBits |= LIBCHANGEDMINOR; }

	/**
	 * Routine to indicate that this Library has not changed in a minor way.
	 * Minor changes include changes to text and other things that are not essential to the circuitry.
	 */
	public void clearChangedMinor() { userBits &= ~LIBCHANGEDMINOR; }

	/**
	 * Routine to return true if this Library has changed in a minor way.
	 * Minor changes include changes to text and other things that are not essential to the circuitry.
	 * @return true if this Library has changed in a minor way.
	 */
	public boolean isChangedMinor() { return (userBits & LIBCHANGEDMINOR) != 0; }

	/**
	 * Routine to indicate that this Library came from disk.
	 * Libraries that come from disk are saved without a file-selection dialog.
	 */
	public void setFromDisk() { userBits |= READFROMDISK; }

	/**
	 * Routine to indicate that this Library did not come from disk.
	 * Libraries that come from disk are saved without a file-selection dialog.
	 */
	public void clearFromDisk() { userBits &= ~READFROMDISK; }

	/**
	 * Routine to return true if this Library came from disk.
	 * Libraries that come from disk are saved without a file-selection dialog.
	 * @return true if this Library came from disk.
	 */
	public boolean isFromDisk() { return (userBits & READFROMDISK) != 0; }

	/**
	 * Routine to indicate that this Library is hidden.
	 * Hidden libraries are not seen by the user.
	 * For example, the "clipboard" library is hidden because it is only used
	 * internally for copying and pasting circuitry.
	 */
	public void setHidden() { userBits |= HIDDENLIBRARY; }

	/**
	 * Routine to indicate that this Library is not hidden.
	 * Hidden libraries are not seen by the user.
	 * For example, the "clipboard" library is hidden because it is only used
	 * internally for copying and pasting circuitry.
	 */
	public void clearHidden() { userBits &= ~HIDDENLIBRARY; }

	/**
	 * Routine to return true if this Library is hidden.
	 * Hidden libraries are not seen by the user.
	 * For example, the "clipboard" library is hidden because it is only used
	 * internally for copying and pasting circuitry.
	 * @return true if this Library is hidden.
	 */
	public boolean isHidden() { return (userBits & HIDDENLIBRARY) != 0; }

	/**
	 * Routine to set the Units value for this Library.
	 * The Units indicate which display units to use for distance, voltage, current, etc.
	 */
	public void setUnits(int value) { userBits = (userBits & ~LIBUNITS) | (value << LIBUNITSSH); }

	/**
	 * Routine to get the Units value for this Library.
	 * The Units indicate which display units to use for distance, voltage, current, etc.
	 * @return the Units value for this Library.
	 */
	public int getUnits() { return (userBits & LIBUNITS) >> LIBUNITSSH; }

	/**
	 * Routine to return the current Library.
	 * @return the current Library.
	 */
	public static Library getCurrent() { return curLib; }
	
	/**
	 * Routine to set the current Library.
	 * @param lib the new current Library.
	 */
	public static void setCurrent(Library lib) { curLib = lib; }
	
	/**
	 * Low-level routine to get the user bits.
	 * The "user bits" are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the binary ".elib"
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @return the "user bits".
	 */
	public int lowLevelGetUserBits() { return userBits; }
	
	/**
	 * Low-level routine to set the user bits.
	 * The "user bits" are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the binary ".elib"
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @param userBits the new "user bits".
	 */
	public void lowLevelSetUserBits(int userBits) { this.userBits = userBits; }

	/**
	 * Routine to find a Library with the specified name.
	 * @param libName the name of the Library.
	 * Note that this is the Library name, and not the Library file.
	 * @return the Library, or null if there is no known Library by that name.
	 */
	public static Library findLibrary(String libName)
	{
		for (int i = 0; i < libraries.size(); i++)
		{
			Library l = (Library) libraries.get(i);
			if (l.getLibName().equalsIgnoreCase(libName))
				return l;
		}
		return null;
	}

	/**
	 */
	public static void clearChangeLocks()
	{
		for (int i = 0; i < libraries.size(); i++)
		{
			Library l = (Library) libraries.get(i);
			for (int j = 0; j < l.cells.size(); j++)
			{
				Cell c = (Cell) l.cells.get(j);
				c.clearChangeLock();
			}
		}
	}

	// why must this class be "static"?   --smr
	private static class VisibleLibraryIterator implements Iterator
	{
		private Iterator uit;
		private Library nextLib;

		VisibleLibraryIterator()
		{
			uit = getLibraries();
			nextLib = nextLibrary();
		}

		private Library nextLibrary()
		{
			while (uit.hasNext())
			{
				Library lib = (Library)uit.next();
				if (!lib.isHidden()) return lib;
			}
			return null;
		}

		public boolean hasNext()
		{
			return nextLib != null;
		}

		public Object next()
		{
			if (nextLib == null) return uit.next(); // throw NoSuchElementException
			Library lib = nextLib;
			nextLib = nextLibrary();
			return lib;
		}

		public void remove() { throw new UnsupportedOperationException("VisibleLibraryIterator.remove()"); };
	}

	/**
	 * Routine to return an iterator over all libraries.
	 * @return an iterator over all libraries.
	 */
	public static Iterator getLibraries()
	{
		return libraries.iterator();
	}

	/**
	 * Routine to return the number of libraries.
	 * @return the number of libraries.
	 */
	public static int getNumLibraries()
	{
		return libraries.size();
	}

	/**
	 * Routine to return an iterator over all libraries.
	 * @return an iterator over all libraries.
	 */
	public static Iterator getVisibleLibraries()
	{
		return new VisibleLibraryIterator();
	}

	/**
	 * Routine to return the number of libraries.
	 * @return the number of libraries.
	 */
	public static int getNumVisibleLibraries()
	{
		// surely there is a better way to do this...  smr
		int numVis = 0;
		for(Iterator it = getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			if (!lib.isHidden()) numVis++;
		}
		return numVis;
	}

	/**
	 * Routine to return a List of all libraries, sorted by name.
	 * The list excludes hidden libraries (i.e. the clipboard).
	 * @return a List of all libraries, sorted by name.
	 */
	public static List getVisibleLibrariesSortedByName()
	{
		List sortedList = new ArrayList();
		for(Iterator it = new VisibleLibraryIterator(); it.hasNext(); )
			sortedList.add(it.next());
		Collections.sort(sortedList, new LibCaseInsensitive());
		return sortedList;
	}

	static class LibCaseInsensitive implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			Library l1 = (Library)o1;
			Library l2 = (Library)o2;
			String s1 = l1.getLibName();
			String s2 = l2.getLibName();
			return s1.compareToIgnoreCase(s2);
		}
	}

	/**
	 * Routine to return the name of this Library.
	 * @return the name of this Library.
	 */
	public String getLibName() { return libName; }

	/**
	 * Routine to set the name of this Library.
	 * @param libName the new name of this Library.
	 */
	public void setLibName(String libName)
	{
		this.libName = libName;
	}

	/**
	 * Routine to return the disk file of this Library.
	 * @return the disk file of this Library.
	 */
	public String getLibFile() { return libFile; }

	/**
	 * Routine to set the disk file of this Library.
	 * @param libFile the new disk file of this Library.
	 */
	public void setLibFile(String libFile)
	{
		this.libFile = libFile;
	}

	/**
	 * Returns a printable version of this Library.
	 * @return a printable version of this Library.
	 */
	public String toString()
	{
		return "Library " + libName;
	}

	// ----------------- cells --------------------

	/**
	 * Routine to get the current Cell in this Library.
	 * @return the current Cell in this Library.
	 * Returns NULL if there is no current Cell.
	 */
	public Cell getCurCell() { return curCell; }

	/**
	 * Routine to insist on a current Cell.
	 * Prints an error message if there is no current Cell.
	 * @return the current Cell in the current Library.
	 * Returns NULL if there is no current Cell.
	 */
	public static Cell needCurCell()
	{
		Cell curCell = curLib.getCurCell();
		if (curCell == null)
		{
			System.out.println("There is no current cell for this operation");
		}
		return curCell;
	}

	/**
	 * Routine to set the current Cell in this Library.
	 * @param curCell the new current Cell in this Library.
	 */
	public void setCurCell(Cell curCell) { this.curCell = curCell; }

	/**
	 * Routine to find the Cell with the given name in this Library.
	 * @param name the name of the desired Cell.
	 * @return the Cell with the given name in this Library.
	 */
	public Cell findNodeProto(String name)
	{
		CellName n = CellName.parseName(name);
		if (n == null) return null;

		Cell onlyWithName = null;
		for (Iterator it = cells.iterator(); it.hasNext();)
		{
			Cell c = (Cell) it.next();
			if (!n.getName().equalsIgnoreCase(c.getProtoName())) continue;
			onlyWithName = c;
			if (n.getView() != c.getView()) continue;
			if (n.getVersion() > 0 && n.getVersion() != c.getVersion()) continue;
			return c;
		}
		if (n.getView() == View.UNKNOWN && onlyWithName != null) return onlyWithName;
		return null;
	}

	static class CellCaseInsensitive implements Comparator
	{
		public int compare(Object o1, Object o2)
		{
			Cell c1 = (Cell)o1;
			Cell c2 = (Cell)o2;
			String s1 = c1.describe();
			String s2 = c2.describe();
			return s1.compareToIgnoreCase(s2);
		}
	}

	/**
	 * Routine to return an Iterator over all Cells in this Library.
	 * @return an Iterator over all Cells in this Library.
	 */
	public Iterator getCells()
	{
		return cells.iterator();
	}

	/**
	 * Routine to return an iterator over all libraries.
	 * @return an iterator over all libraries.
	 */
	public List getCellsSortedByName()
	{
		List sortedList = new ArrayList();
		for(Iterator it = getCells(); it.hasNext(); )
			sortedList.add(it.next());
		Collections.sort(sortedList, new CellCaseInsensitive());
		return sortedList;
	}

}
