/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: InputLibrary.java
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
package com.sun.electric.tool.io;

import com.sun.electric.database.geometry.EMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.io.File;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Date;
import java.util.HashMap;


/**
 * This class reads Library files (binary or readable dump) format.
 */
public class InputLibrary extends Input
{
	// the cell information
	/** The number of Cells in the file. */									protected int nodeProtoCount;
	/** A list of cells being read. */										protected Cell [] nodeProtoList;
	/** lambda value for each cell of the library */						protected double [] cellLambda;

	static class NodeInstList
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
	};

	/** collection of libraries and their input objects. */					private static HashMap libsBeingRead;
	protected static final boolean VERBOSE = false;

	InputLibrary() {}

	// *************************** THE CREATION INTERFACE ***************************

	public static void initializeLibraryInput()
	{
		libsBeingRead = new HashMap();
	}

	public boolean readInputLibrary()
	{
		// add this reader to the list
		libsBeingRead.put(lib, this);

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

			InputLibrary reader = this;
			if (otherCell.getLibrary() != cell.getLibrary())
				reader = getReaderForLib(otherCell.getLibrary());

			// subcell: make sure that cell is setup
			reader.realizeCellsRecursively(otherCell, markCellForNodes);
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

	public static void cleanupLibraryInput()
	{
		// clear flag bits for scanning the library hierarchically
		FlagSet markCellForNodes = NodeProto.getFlagSet(1);
		for(Iterator it = libsBeingRead.values().iterator(); it.hasNext(); )
		{
			InputLibrary reader = (InputLibrary)it.next();
			for(int cellIndex=0; cellIndex<reader.nodeProtoCount; cellIndex++)
			{
				Cell cell = reader.nodeProtoList[cellIndex];
				if (cell.getLibrary() != reader.lib) continue;
				reader.cellLambda[cellIndex] = reader.computeLambda(cell, cellIndex);
				cell.setTempInt(cellIndex);
				cell.clearBit(markCellForNodes);
			}
		}

		// now recursively adjust lambda sizes
		if (InputLibrary.VERBOSE)
			System.out.println("Preparing to compute scale factors");
		for(int i=0; i<20; i++)
		{
			boolean unchanged = true;
			for(Iterator it = libsBeingRead.values().iterator(); it.hasNext(); )
			{
				InputLibrary reader = (InputLibrary)it.next();
				for(int cellIndex=0; cellIndex<reader.nodeProtoCount; cellIndex++)
				{
					Cell cell = reader.nodeProtoList[cellIndex];
					if (cell.getLibrary() != reader.lib) continue;
					if (reader.spreadLambda(cell, cellIndex))
					{
						unchanged = false;
					}
				}
			}
			if (unchanged) break;
		}
		if (InputLibrary.VERBOSE)
			System.out.println("Finished computing scale factors");

		// recursively create the cell contents
		for(Iterator it = libsBeingRead.values().iterator(); it.hasNext(); )
		{
			InputLibrary reader = (InputLibrary)it.next();
			for(int cellIndex=0; cellIndex<reader.nodeProtoCount; cellIndex++)
			{
				Cell cell = reader.nodeProtoList[cellIndex];
				if (cell.isBit(markCellForNodes)) continue;
				reader.realizeCellsRecursively(cell, markCellForNodes);
			}
		}
		markCellForNodes.freeFlagSet();
	}

	protected InputLibrary getReaderForLib(Library lib) { return (InputLibrary)libsBeingRead.get(lib); }

	// *************************** THE CELL CLEANUP INTERFACE ***************************

	protected double computeLambda(Cell cell, int cellIndex) { return 1; }

	protected boolean spreadLambda(Cell cell, int cellIndex) { return false; }

	/**
	 * Method to recursively create the contents of each cell in the library.
	 */
	protected void realizeCellsRecursively(Cell cell, FlagSet recursiveSetupFlag)
	{
	}

	protected boolean readerHasExport(Cell c, String portName)
	{
		return false;
	}

}
