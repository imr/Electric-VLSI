/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Bookshelf.java
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
package com.sun.electric.tool.io.input.bookshelf;

import java.net.URL;
import java.util.BitSet;
import java.util.Map;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.id.CellId;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.input.Input;
import com.sun.electric.tool.io.input.bookshelf.BookshelfNodes.BookshelfNode;

/**
 * @author fschmidt
 * 
 */
public class Bookshelf extends Input<Object> {

	public enum BookshelfFiles {
		aux, nodes, nets, wts, pl, scl;

		public static BookshelfFiles getFileType(String file) {
			if (file.endsWith("." + aux.toString())) {
				return BookshelfFiles.aux;
			} else if (file.endsWith("." + nodes.toString())) {
				return BookshelfFiles.nodes;
			} else if (file.endsWith("." + nets.toString())) {
				return BookshelfFiles.nets;
			} else if (file.endsWith("." + wts.toString())) {
				return BookshelfFiles.wts;
			} else if (file.endsWith("." + pl.toString())) {
				return BookshelfFiles.pl;
			} else if (file.endsWith("." + scl.toString())) {
				return BookshelfFiles.scl;
			}
			return null;
		}
	}

	private BookshelfPreferences preferences;

	private Bookshelf(BookshelfPreferences preferences) {
		this.preferences = preferences;
	}

	public static class BookshelfPreferences extends InputPreferences {

		private URL fileUrl = null;

		/**
		 * @param factory
		 */
		public BookshelfPreferences(boolean factory) {
			super(factory);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.sun.electric.tool.io.input.Input.InputPreferences#doInput(java
		 * .net.URL, com.sun.electric.database.hierarchy.Library,
		 * com.sun.electric.technology.Technology, java.util.Map, java.util.Map,
		 * com.sun.electric.tool.Job)
		 */
		@Override
		public Library doInput(URL fileURL, Library lib, Technology tech,
				Map<Library, Cell> currentCells, Map<CellId, BitSet> nodesToExpand, Job job) {
			Bookshelf bookshelf = new Bookshelf(this);

			this.fileUrl = fileURL;

			return bookshelf.importALibrary(lib, tech, currentCells);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sun.electric.tool.io.input.Input#importALibrary(com.sun.electric.
	 * database.hierarchy.Library, com.sun.electric.technology.Technology,
	 * java.util.Map)
	 */
	@Override
	protected Library importALibrary(Library lib, Technology tech, Map<Library, Cell> currentCells) {

		try {
			Cell cell = Cell.makeInstance(lib, lib.getName() + "{lay}");
			
			BookshelfAux auxParser = new BookshelfAux(preferences.fileUrl.getFile());
			Map<BookshelfFiles, String> files = auxParser.parse();
			String auxDir = auxParser.getAuxDir();
			BookshelfNodes nodes = new BookshelfNodes(auxDir + files.get(BookshelfFiles.nodes), lib, lib.getName(), cell);
			nodes.parse();
			
			BookshelfNets nets = new BookshelfNets(auxDir + files.get(BookshelfFiles.nets), cell);
			nets.parse();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return lib;
	}

}
