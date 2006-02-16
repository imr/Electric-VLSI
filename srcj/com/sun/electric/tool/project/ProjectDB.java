/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ProjectDB.java
 * Project management tool database
 * Written by: Steven M. Rubin
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.project;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;


/**
 * This is the Project Management tool database.
 */
public class ProjectDB implements Serializable
{
	/** all libraries read in */	private HashMap<Library,ProjectLibrary> libraryProjectInfo = new HashMap<Library,ProjectLibrary>();

	List<ProjectLibrary> getProjectLibraries()
	{
		List<ProjectLibrary> pLibs = new ArrayList<ProjectLibrary>();
		for(Library lib : libraryProjectInfo.keySet())
			pLibs.add(libraryProjectInfo.get(lib));
		return pLibs;
	}

	/**
	 * Method to ensure that there is project information for a given library.
	 * @param lib the Library to check.
	 * @return a ProjectLibrary object for the Library.  If the library is marked
	 * as being part of a project, that project file is read in.  If the library is
	 * not in a project, the returned object has nothing in it.
	 */
	ProjectLibrary findProjectLibrary(Library lib)
	{
		// see if this library has a known project database
		ProjectLibrary pl = libraryProjectInfo.get(lib);
		if (pl != null) return pl;
		pl = ProjectLibrary.createProject(lib);
		libraryProjectInfo.put(lib, pl);
		return pl;
	}

	ProjectCell findProjectCell(Cell cell)
	{
		ProjectLibrary pl = findProjectLibrary(cell.getLibrary());
		ProjectCell pc = pl.findProjectCell(cell);
		return pc;
	}

	void clearDatabase() { libraryProjectInfo.clear(); }
}
