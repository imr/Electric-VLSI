package com.sun.electric.database;

import java.util.ArrayList;
import java.util.HashMap;
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
	/** library has changed significantly */				public static final int LIBCHANGEDMAJOR=           01;
	/** recheck networks in library */						public static final int REDOCELLLIB=               02;
	/** set if library came from disk */					public static final int READFROMDISK=              04;
	/** internal units in library (see INTERNALUNITS) */	public static final int LIBUNITS=                 070;
	/** right shift for LIBUNITS */							public static final int LIBUNITSSH=                 3;
	/** library has changed insignificantly */				public static final int LIBCHANGEDMINOR=         0100;
	/** library is "hidden" (clipboard library) */			public static final int HIDDENLIBRARY=           0200;
	/** library is unwanted (used during input) */			public static final int UNWANTEDLIB=             0400;

	// ------------------------ private data ------------------------------
	private String libName; // name of this library 
	private String fileName; // file location of this library
	private HashMap lambdaSizes; // units are half-nanometers
	private ArrayList cells; // list of Cells in this library
	private Cell curCell; // Cell currently being edited

	// static list of all libraries in Electric
	private static ArrayList libraries = new ArrayList();
	private static Library curLib = null;

	// ----------------- private and protected methods --------------------
	private Library(String libName, String fileName)
	{
		this.cells = new ArrayList();
		this.lambdaSizes = new HashMap();
		this.curCell = null;
		this.libName = libName;
		this.fileName = fileName;
	
		// add the library to the global list
		libraries.add(this);
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

	protected void setLambda(double lambda, Technology tech)
	{
		lambdaSizes.put(tech, new Double(lambda));
	}

	// ----------------- public interface --------------------

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
		Library lib = new Library(legalName, libraryFile);
		Library.setCurrent(lib);

		return lib;
	}

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
		removeAll(cells);

		// remove it from the list of libraries
		libraries.remove(this);
	}
	
	/**
	 * Return the current Library
	 */
	public static Library getCurrent()
	{
		return curLib;
	}
	
	/**
	 * Set the current Library
	 */
	public static void setCurrent(Library lib)
	{
		curLib = lib;
	}

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
	 * get the size of a lambda, in half-namometers (base units)
	 * for a particular technology
	 */
	public double getLambdaSize(Technology tech)
	{
		Double lambdaObject = (Double) lambdaSizes.get(tech);
		double lambda = lambdaObject.doubleValue();
		if (lambda == 0.0)
		{
			return tech.getDefLambda();
		} else
		{
			return lambda;
		}
	}

	/**
	 * Get the name of this Library
	 */
	public String getLibName()
	{
		return libName;
	}

	/**
	 * Get the disk file of this Library
	 */
	public String getLibFile()
	{
		return fileName;
	}

	public String toString()
	{
		return "Library " + libName;
	}

	// ----------------- cells --------------------

	/**
	 * Get the cell that is currently being edited in this library,
	 * or NULL if there is no such cell.
	 */
	public Cell getCurCell()
	{
		return curCell;
	}

	/**
	 * Get the cell from this library that has a particular name.  The
	 * name should be of the form "foo{sch}" or "foo;3{sch}"
	 */
	public Cell findNodeProto(String name)
	{
		for (int i = 0; i < cells.size(); i++)
		{
			Cell f = (Cell) cells.get(i);
			if (name.equalsIgnoreCase(f.describeNodeProto()))
				return f;
		}
		return null;
	}

	public Iterator getCells()
	{
		return cells.iterator();
	}

}
