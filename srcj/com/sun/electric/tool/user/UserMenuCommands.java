/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: UserMenuCommands.java
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
package com.sun.electric.tool.user;

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.user.ui.UIEditFrame;
import com.sun.electric.tool.user.ui.UIDialogOpenFile;
import com.sun.electric.tool.io.Input;
import com.sun.electric.tool.io.Output;

import java.util.Iterator;

public final class UserMenuCommands
{
	// ---------------------- private and protected methods -----------------
	static UIEditFrame lastFrame = null;

	// It is never useful for anyone to create an instance of this class
	private UserMenuCommands()
	{
	}

	// ---------------------- THE FILE MENU -----------------

	public static void openLibraryCommand()
	{
		String fileName = UIDialogOpenFile.ELIB.chooseInputFile();
		if (fileName != null)
		{
			long startTime = System.currentTimeMillis();
			Library lib = Input.ReadLibrary(fileName, null, Input.ImportType.BINARY);
			if (lib == null) return;
			long endTime = System.currentTimeMillis();
			float finalTime = (endTime - startTime) / 1000F;
			System.out.println("Library " + fileName + " read, took " + finalTime + " seconds");
			Library.setCurrent(lib);
			Cell cell = lib.getCurCell();
			if (cell == null)
			{
				System.out.println("No current cell in this library");
			} else
			{
				lastFrame = UIEditFrame.CreateEditWindow(cell);
			}
		}
	}

	public static void saveLibraryCommand()
	{
		Library lib = Library.getCurrent();
		String fileName;
		if (lib.isFromDisk())
		{
			fileName = lib.getLibFile();
		} else
		{
			fileName = UIDialogOpenFile.ELIB.chooseOutputFile(lib.getLibName()+".elib");
			if (fileName != null)
			{
				Library.LibraryName n = Library.LibraryName.newInstance(fileName);
				n.setExtension("elib");
				lib.setLibFile(n.makeName());
				lib.setLibName(n.getName());
			}
		}
		boolean error = Output.WriteLibrary(lib, Output.ExportType.BINARY);
		if (error)
		{
			System.out.println("Error writing the library file");
		}
	}

	public static void saveAsLibraryCommand()
	{
		Library lib = Library.getCurrent();
		lib.clearFromDisk();
		saveLibraryCommand();
	}

	public static void quitCommand()
	{
		System.exit(0);
	}

	// ---------------------- THE STEVE MENU -----------------

	public static void fullDisplayCommand()
	{
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			for(Iterator cit = lib.getCells(); cit.hasNext(); )
			{
				Cell cell = (Cell)cit.next();
				for(Iterator nit = cell.getNodes(); nit.hasNext(); )
				{
					NodeInst ni = (NodeInst)nit.next();
					ni.setExpanded();
				}
			}
		}
		if (lastFrame != null)
		{
			lastFrame.setTimeTracking(true);
			lastFrame.redraw();
		}
	}

	public static void showCellGroupsCommand()
	{
		// list things by cell group
		FlagSet cellFlag = NodeProto.getFlagSet(1);
		Library curLib = Library.getCurrent();
		for(Iterator it = curLib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			cell.clearBit(cellFlag);
		}
		for(Iterator it = curLib.getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			if (cell.isBit(cellFlag)) continue;

			// untouched: show whole cell group
			System.out.println("**** Cell group:");
			for(Iterator git = cell.getCellGroup().getCells(); git.hasNext(); )
			{
				Cell cellInGroup = (Cell)git.next();
				System.out.println("    " + cellInGroup.describe());
				cellInGroup.setBit(cellFlag);
			}
		}
		NodeProto.freeFlagSet(cellFlag);
	}
}
