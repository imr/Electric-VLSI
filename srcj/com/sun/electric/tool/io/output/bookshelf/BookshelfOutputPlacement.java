/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BookshelfOutputPlacement.java
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
package com.sun.electric.tool.io.output.bookshelf;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;


import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.input.bookshelf.Bookshelf.BookshelfFiles;

/**
 * @author Felix Schmidt
 * 
 */
public class BookshelfOutputPlacement extends BookshelfOutputWriter {

	private static final BookshelfFiles fileType = BookshelfFiles.pl;
	private Cell cell;

	public BookshelfOutputPlacement(String genericFileName, Cell cell) {
		super(genericFileName, fileType);
		this.cell = cell;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sun.electric.tool.io.output.bookshelf.BookshelfOutputWriter#write()
	 */
	@Override
	public void write() throws IOException {
		Job.getUserInterface().setProgressNote("Placement File: " + this.fileName);
		
		PrintWriter writer = new PrintWriter(this.fileName);
		
		writer.println(BookshelfOutput.createBookshelfHeader(fileType));

		for (Iterator<NodeInst> ini = cell.getNodes(); ini.hasNext();) {
			NodeInst ni = ini.next();

			writer.println(ni.getName() + " " + ni.getTrueCenterX() + " " + ni.getTrueCenterY() + " : "
					+ this.getOrientation(ni.getOrient()));
		}

		writer.flush();
		writer.close();

	}

	private String getOrientation(Orientation orient) {
		String result = "";

		// N, S, E, W, FN, FS, FE, FW
		if (orient.equals(Orientation.IDENT))
			result = "N";
		else if (orient.equals(Orientation.R))
			result = "E";
		else if (orient.equals(Orientation.RR))
			result = "S";
		else if (orient.equals(Orientation.RRR))
			result = "W";

		return result;
	}

}
