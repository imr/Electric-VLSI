/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LibraryFiles.java
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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.MoCMOS;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;


/**
 * This class reads Library files (ELIB or readable dump) format.
 */
public class LibraryFiles extends Input
{
	// the cell information
	/** The number of Cells in the file. */									protected int nodeProtoCount;
	/** A list of cells being read. */										protected Cell [] nodeProtoList;
	/** lambda value for each cell of the library */						protected double [] cellLambda;
	/** total number of cells in all read libraries */						protected static int totalCells;
	/** number of cells constructed so far. */								protected static int cellsConstructed;
	/** a List of scaled Cells that got created */							protected List scaledCells;
	/** a List of wrong-size Cells that got created */						protected List skewedCells;
	/** The Electric version in the library file. */						protected int emajor, eminor, edetail;
	/** the Electric version in the library file. */						protected Version version;

	protected static class NodeInstList
	{
		protected NodeInst []  theNode;
		protected NodeProto [] protoType;
		protected Name []      name;
		protected int []       lowX;
		protected int []       highX;
		protected int []       lowY;
		protected int []       highY;
		protected short []     rotation;
		protected int []       transpose;
		protected int []       userBits;
	};

	/** collection of libraries and their input objects. */					private static HashMap libsBeingRead;
	protected static final boolean VERBOSE = false;

	LibraryFiles() {}

	// *************************** THE CREATION INTERFACE ***************************

	public static void initializeLibraryInput()
	{
		libsBeingRead = new HashMap();
	}

	public boolean readInputLibrary()
	{
		// add this reader to the list
		libsBeingRead.put(lib, this);
		scaledCells = new ArrayList();
		skewedCells = new ArrayList();

		return readLib();
	}

	protected void scanNodesForRecursion(Cell cell, FlagSet markCellForNodes, NodeProto [] nil, int start, int end)
	{
		// scan the nodes in this cell and recurse
		for(int j=start; j<end; j++)
		{
			NodeProto np = nil[j];
			if (np instanceof PrimitiveNode) continue;
			Cell otherCell = (Cell)np;
			if (otherCell == null) continue;

			// subcell: make sure that cell is setup
			if (otherCell.isBit(markCellForNodes)) continue;

			LibraryFiles reader = this;
			if (otherCell.getLibrary() != cell.getLibrary())
				reader = getReaderForLib(otherCell.getLibrary());

			// subcell: make sure that cell is setup
			if (reader != null)
				reader.realizeCellsRecursively(otherCell, markCellForNodes, null, 0, 0);
		}
		cell.setBit(markCellForNodes);
	}

	/**
	 * Method to read a Library.
	 * This method is never called.
	 * Instead, it is always overridden by the appropriate read subclass.
	 * @return true on error.
	 */
	protected boolean readLib() { return true; }

	/**
	 * Method to find the View to use for an old View name.
	 * @param viewName the old View name.
	 * @return the View to use (null if not found).
	 */
	protected View findOldViewName(String viewName)
	{
		if (version.getMajor() < 8)
		{
			if (viewName.equals("compensated")) return View.LAYOUTCOMP;
			if (viewName.equals("skeleton")) return View.LAYOUTSKEL;
			if (viewName.equals("simulation-snapshot")) return View.DOCWAVE;
			if (viewName.equals("netlist-netlisp-format")) return View.NETLISTNETLISP;
			if (viewName.equals("netlist-rsim-format")) return View.NETLISTRSIM;
			if (viewName.equals("netlist-silos-format")) return View.NETLISTSILOS;
			if (viewName.equals("netlist-quisc-format")) return View.NETLISTQUISC;
			if (viewName.equals("netlist-als-format")) return View.NETLISTALS;
		}
		return null;
	}

	public static void cleanupLibraryInput()
	{
		progress.setNote("Constructing cell contents...");
		progress.setProgress(0);

		// clear flag bits for scanning the library hierarchically
		totalCells = 0;
		FlagSet markCellForNodes = NodeProto.getFlagSet(1);
		for(Iterator it = libsBeingRead.values().iterator(); it.hasNext(); )
		{
			LibraryFiles reader = (LibraryFiles)it.next();
			totalCells += reader.nodeProtoCount;
			for(int cellIndex=0; cellIndex<reader.nodeProtoCount; cellIndex++)
			{
				Cell cell = reader.nodeProtoList[cellIndex];
				if (cell == null) continue;
				if (cell.getLibrary() != reader.lib) continue;
				reader.cellLambda[cellIndex] = reader.computeLambda(cell, cellIndex);
				cell.setTempInt(cellIndex);
				cell.clearBit(markCellForNodes);
			}
		}
		cellsConstructed = 0;

		// now recursively adjust lambda sizes
		if (LibraryFiles.VERBOSE)
			System.out.println("Preparing to compute scale factors");
		for(int i=0; i<20; i++)
		{
			boolean unchanged = true;
			for(Iterator it = libsBeingRead.values().iterator(); it.hasNext(); )
			{
				LibraryFiles reader = (LibraryFiles)it.next();
				for(int cellIndex=0; cellIndex<reader.nodeProtoCount; cellIndex++)
				{
					Cell cell = reader.nodeProtoList[cellIndex];
					if (cell == null) continue;
					if (cell.getLibrary() != reader.lib) continue;
					if (reader.spreadLambda(cell, cellIndex))
					{
						unchanged = false;
					}
				}
			}
			if (unchanged) break;
		}
		if (LibraryFiles.VERBOSE)
			System.out.println("Finished computing scale factors");

		// recursively create the cell contents
		for(Iterator it = libsBeingRead.values().iterator(); it.hasNext(); )
		{
			LibraryFiles reader = (LibraryFiles)it.next();
			for(int cellIndex=0; cellIndex<reader.nodeProtoCount; cellIndex++)
			{
				Cell cell = reader.nodeProtoList[cellIndex];
				if (cell == null) continue;
				if (cell.isBit(markCellForNodes)) continue;
				reader.realizeCellsRecursively(cell, markCellForNodes, null, 0, 0);
			}
		}
		markCellForNodes.freeFlagSet();

		// tell which libraries had extra "scaled" cells added
		boolean first = true;
		for(Iterator it = libsBeingRead.values().iterator(); it.hasNext(); )
		{
			LibraryFiles reader = (LibraryFiles)it.next();
			if (reader.scaledCells != null && reader.scaledCells.size() != 0)
			{
				if (first)
				{
					System.out.println("WARNING: to accommodate scaling inconsistencies, these cells were created:");
					first = false;
				}
				StringBuffer sb = new StringBuffer();
				sb.append("   Library " + reader.lib.getLibName() + ":");
				for(Iterator sIt = reader.scaledCells.iterator(); sIt.hasNext(); )
				{
					Cell cell = (Cell)sIt.next();
					sb.append(" " + cell.noLibDescribe());
				}
				System.out.println(sb.toString());
			}
			if (reader.skewedCells != null && reader.skewedCells.size() != 0)
			{
				if (first)
				{
					System.out.println("ERROR: because of library inconsistencies, these stretched cells were created:");
					first = false;
				}
				StringBuffer sb = new StringBuffer();
				sb.append("   Library " + reader.lib.getLibName() + ":");
				for(Iterator sIt = reader.skewedCells.iterator(); sIt.hasNext(); )
				{
					Cell cell = (Cell)sIt.next();
					sb.append(" " + cell.noLibDescribe());
				}
				System.out.println(sb.toString());
			}
		}

		// adjust for old library conversion
		convertOldLibraries();
	}

	private static void convertOldLibraries()
	{
		// see if the MOSIS CMOS technology now has old-style state information
		MoCMOS.tech.convertOldState();
	}

	protected LibraryFiles getReaderForLib(Library lib) { return (LibraryFiles)libsBeingRead.get(lib); }

	// *************************** THE CELL CLEANUP INTERFACE ***************************

	protected double computeLambda(Cell cell, int cellIndex) { return 1; }

	protected boolean spreadLambda(Cell cell, int cellIndex) { return false; }

	/**
	 * Method to recursively create the contents of each cell in the library.
	 */
	protected void realizeCellsRecursively(Cell cell, FlagSet recursiveSetupFlag, String scaledCellName, double scaleX, double scaleY)
	{
	}

	protected boolean readerHasExport(Cell c, String portName)
	{
		return false;
	}

}
