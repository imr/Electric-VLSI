package com.sun.electric.database.hierarchy;

import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.text.CellName;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

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
	/** The Library class */								public static Class CLASS      = (new Library()).getClass();
	/** The Library[] class */								public static Class ARRAYCLASS = (new Library[0]).getClass();

	// ------------------------ private data ------------------------------

	/** library has changed significantly */				private static final int LIBCHANGEDMAJOR =           01;
//	/** recheck networks in library */						private static final int REDOCELLLIB =               02;
	/** set if library came from disk */					private static final int READFROMDISK =              04;
	/** internal units in library (see INTERNALUNITS) */	private static final int LIBUNITS =                 070;
	/** right shift for LIBUNITS */							private static final int LIBUNITSSH =                 3;
	/** library has changed insignificantly */				private static final int LIBCHANGEDMINOR =         0100;
	/** library is "hidden" (clipboard library) */			private static final int HIDDENLIBRARY =           0200;
//	/** library is unwanted (used during input) */			private static final int UNWANTEDLIB =             0400;

	/** name of this library  */							private String libName;
	/** file location of this library */					private String fileName;
	/** list of Cells in this library */					private ArrayList cells;
	/** Cell currently being edited */						private Cell curCell;
	/** flag bits */										private int userBits;

	/** static list of all libraries in Electric */			private static List libraries = new ArrayList();
	/** the current library in Electric */					private static Library curLib = null;

	// ----------------- private and protected methods --------------------

	private Library()
	{
	}

	/**
	 * The factory to create new libraries.
	 */
	public static Library newInstance(String libraryName, String libraryFile)
	{
		// make sure the name is legal
		String legalName = libraryName.replace(' ', '-');
		if (!legalName.equalsIgnoreCase(libraryName))
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
		lib.fileName = libraryFile;
	
		// add the library to the global list
		libraries.add(lib);
		Library.setCurrent(lib);

		return lib;
	}

	void addCell(Cell c)
	{
		// sanity check: make sure Cell isn't already in the list
		if (cells.contains(c))
		{
			error("Tried to re-add a cell to a library: " + c);
		}
		cells.add(c);
	}

	void removeCell(Cell c)
	{
		// sanity check: make sure Cell is in the list
		if (!cells.contains(c))
		{
			error("Tried to remove a non-existant Cell from a library: " + c);
		}
		cells.remove(c);
	}

	// ----------------- public interface --------------------

	/** Set the Major-Change bit */
	public void setChangedMajor() { userBits |= LIBCHANGEDMAJOR; }
	/** Clear the Major-Change bit */
	public void clearChangedMajor() { userBits &= ~LIBCHANGEDMAJOR; }
	/** Get the Major-Change bit */
	public boolean isChangedMajor() { return (userBits & LIBCHANGEDMAJOR) != 0; }

	/** Set the Minor-Change bit */
	public void setChangedMinor() { userBits |= LIBCHANGEDMINOR; }
	/** Clear the Minor-Change bit */
	public void clearChangedMinor() { userBits &= ~LIBCHANGEDMINOR; }
	/** Get the Minor-Change bit */
	public boolean isChangedMinor() { return (userBits & LIBCHANGEDMINOR) != 0; }

	/** Set the Came-from-Disk bit */
	public void setFromDisk() { userBits |= READFROMDISK; }
	/** Clear the Came-from-Disk bit */
	public void clearFromDisk() { userBits &= ~READFROMDISK; }
	/** Get the Came-from-Disk bit */
	public boolean isFromDisk() { return (userBits & READFROMDISK) != 0; }

	/** Set the Hidden bit */
	public void setHidden() { userBits |= HIDDENLIBRARY; }
	/** Clear the Hidden bit */
	public void clearHidden() { userBits &= ~HIDDENLIBRARY; }
	/** Get the Hidden bit */
	public boolean isHidden() { return (userBits & HIDDENLIBRARY) != 0; }

	/** Set the Units value */
	public void setUnits(int value) { userBits = (userBits & ~LIBUNITS) | (value << LIBUNITSSH); }
	/** Get the Units value */
	public int getUnits() { return (userBits & LIBUNITS) >> LIBUNITSSH; }

	public void killLibrary()
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
			error("Cannot delete library " + this);
			return;
		}

		// remove all cells in the library
//		removeAll(cells);

		// remove it from the list of libraries
		libraries.remove(this);
	}
	
	/**
	 * Return the current Library
	 */
	public static Library getCurrent() { return curLib; }
	
	/**
	 * Set the current Library
	 */
	public static void setCurrent(Library lib) { curLib = lib; }
	
	/**
	 * Set the user bits for this Library
	 */
	public void lowLevelSetUserBits(int bits) { this.userBits = bits; }

	/**
	 * Check all currently loaded Libraries for one named <code>libName</code>.
	 * @param libName the name of the library.
	 * Note that this is not the name of the library file.
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
	 * Return an iterator over all libraries.
	 */
	public static Iterator getLibraries()
	{
		return libraries.iterator();
	}

	/**
	 * Get the name of this Library
	 */
	public String getLibName() { return libName; }

	/**
	 * Get the disk file of this Library
	 */
	public String getLibFile() { return fileName; }

	public String toString()
	{
		return "Library " + libName;
	}

	// ----------------- cells --------------------

	/**
	 * Get the cell that is currently being edited in this library,
	 * or NULL if there is no such cell.
	 */
	public Cell getCurCell() { return curCell; }

	/**
	 * Set the cell that is currently being edited in this library.
	 */
	public void setCurCell(Cell curCell) { this.curCell = curCell; }

	/**
	 * Get the cell from this library that has a particular name.  The
	 * name should be of the form "foo{sch}" or "foo;3{sch}"
	 */
	public Cell findNodeProto(String name)
	{
		CellName n = CellName.parseName(name);
		if (n == null) return null;

		for (Iterator it = cells.iterator(); it.hasNext();)
		{
			Cell c = (Cell) it.next();
			if (!n.getName().equalsIgnoreCase(c.getProtoName())) continue;
			if (n.getView() != c.getView()) continue;
			if (n.getVersion() > 0 && n.getVersion() != c.getVersion()) continue;
			return c;
		}
		return null;
	}

	public Iterator getCells()
	{
		return cells.iterator();
	}

}
