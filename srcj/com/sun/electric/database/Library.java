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
	protected Library(String libName, String fileName)
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

	protected void getInfo()
	{
		System.out.println("  name: " + libName);
		System.out.println("  file: " + fileName);
		System.out.println("  total cells: " + cells.size());
		System.out.println("  current cell: " + curCell);
		super.getInfo();
	}

	// ----------------- public interface --------------------
	
	public static Library newLibrary(String libraryName, String libraryFile)
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
	 * Create a new Cell in this library named "name".
	 * The name should be something like "foo;2{sch}".
	 */
	public Cell newCell(String name)
	{
		// see if this cell already exists
		Cell existingCell = findNodeProto(name);
		if (existingCell != null)
		{
			System.out.println("Cannot create cell " + name + " in library " + libName + " ...already exists");
			return(null);
		}
		
		// figure out the view and version of the cell
		View cellView = null;
		int openCurly = name.indexOf('{');
		int closeCurly = name.lastIndexOf('}');
		if (openCurly != -1 && closeCurly != -1)
		{
			String viewName = name.substring(openCurly+1, closeCurly);
			cellView = View.getView(viewName);
			if (cellView == null)
			{
				System.out.println("Unknown view: " + viewName);
				return null;
			}
		}
		
		// figure out the version
		int explicitVersion = 0;
		int semicolon = name.indexOf(';');
		if (semicolon != -1)
		{
//			explicitVersion = Integer.ParseInt(name.substring(semicolon), 10);
			if (explicitVersion <= 0)
			{
				System.out.println("Cell versions must be positive, this is " + explicitVersion);
				return null;
			}
		}

		// get the pure cell name
		if (semicolon == -1) semicolon = name.length();
		if (openCurly == -1) openCurly = name.length();
		int nameEnd = Math.min(semicolon, openCurly);
		String pureCellName = name.substring(0, nameEnd);

		// make sure this version isn't in use
		if (explicitVersion > 0)
		{
			for (int i = 0; i < cells.size(); i++)
			{
				Cell c = (Cell) cells.get(i);
				if (pureCellName.equals(c.getProtoName()) && cellView == c.getView() &&
					explicitVersion == c.getVersion())
				{
					System.out.println("Already a cell with this version");
					return null;
				}
			}
		} else
		{
			// find a new version
			explicitVersion = 1;
			for (int i = 0; i < cells.size(); i++)
			{
				Cell c = (Cell) cells.get(i);
				if (pureCellName.equals(c.getProtoName()) && cellView == c.getView() &&
					c.getVersion() >= explicitVersion)
						explicitVersion = c.getVersion() + 1;
			}
		}

		Cell c = new Cell(this, pureCellName, explicitVersion, cellView);
//		c.setReferencePoint(0, 0); // for 0, units don't matter
		return c;
	}

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
