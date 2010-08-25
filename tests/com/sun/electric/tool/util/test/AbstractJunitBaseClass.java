/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AbstractJunitBaseClass.java
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
package com.sun.electric.tool.util.test;

import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import org.junit.Before;

import com.sun.electric.database.EditingPreferences;
import com.sun.electric.database.Environment;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.id.IdManager;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.TechPool;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.FileType;
import com.sun.electric.tool.io.input.LibraryFiles;

/**
 * @author Felix Schmidt
 * 
 */
public abstract class AbstractJunitBaseClass {

	public enum LoadLibraryType {
		resource, fileSystem;
	}

	public AbstractJunitBaseClass() {

	}

	@Before
	public void initElectric() {
		TextDescriptor.cacheSize();
		Tool.initAllTools();
		Pref.lockCreation();
		this.initDatabase();
		this.initEnvironment();
		this.initTech();
	}

	protected Library loadLibrary(String libName, String fileName) throws Exception {
		return this.loadLibrary(libName, fileName, LoadLibraryType.resource);
	}

	protected Library loadLibrary(String libName, String fileName, LoadLibraryType type) throws Exception {
		EDatabase.serverDatabase().lowLevelBeginChanging(null);

		URL fileURL = null;
		if (type.equals(LoadLibraryType.resource)) {
			fileURL = Object.class.getResource(fileName);
		} else {
			fileURL = TextUtils.makeURLToFile(fileName);
		}

		Library rootLib = Library.findLibrary(libName);
		if (rootLib == null) // attempt to read a JELIB if extension is missing
			rootLib = LibraryFiles.readLibrary(fileURL, libName, FileType.JELIB, true);
		if (rootLib == null) {
			System.out.println("Can't upload the library '" + libName + "'"); // error
			return null;
		}
		return rootLib;
	}
	
	protected Cell loadCell(String libName, String cellName, String fileName) throws Exception {
		return this.loadCell(libName, cellName, fileName, LoadLibraryType.resource);
	}

	protected Cell loadCell(String libName, String cellName, String fileName, LoadLibraryType type) throws Exception {
		Library lib = this.loadLibrary(libName, fileName, type);
		for (Iterator<Cell> cellIt = lib.getCells(); cellIt.hasNext();) {
			Cell cell = cellIt.next();
			if (cell.getName().equals(cellName)) {
				return cell;
			}
		}
		return null;
	}

	private void initTech() {
		EDatabase.serverDatabase().lowLevelSetCanUndoing(true);
		Map<String, Object> paramValuesByXmlPath = Technology.getParamValuesByXmlPath();
		Technology.initPreinstalledTechnologies(EDatabase.serverDatabase(), paramValuesByXmlPath);
	}

	private void initDatabase() {
		EDatabase database = new EDatabase(IdManager.stdIdManager.getInitialSnapshot(), "serverDB");
		Job.setUserInterface(new TestUserInterface());
		EDatabase.setServerDatabase(database);
		EDatabase.serverDatabase().lock(true);
	}

	private void initEnvironment() {
		Environment env = IdManager.stdIdManager.getInitialEnvironment();
		Environment.setThreadEnvironment(env);
		EditingPreferences ep = new EditingPreferences(true, TechPool.getThreadTechPool());
		EditingPreferences.setThreadEditingPreferences(ep);

	}
}
