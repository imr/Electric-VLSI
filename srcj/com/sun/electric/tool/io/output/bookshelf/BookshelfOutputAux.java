/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BookshelfOutputAux.java
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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.input.bookshelf.Bookshelf.BookshelfFiles;

/**
 * @author Felix Schmidt
 * 
 */
public class BookshelfOutputAux extends BookshelfOutputWriter {

	private static final BookshelfFiles fileType = BookshelfFiles.aux;

	/**
	 * @param genericFileName
	 * @param fileType
	 */
	public BookshelfOutputAux(String genericFileName) {
		super(genericFileName, fileType);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sun.electric.tool.io.output.bookshelf.BookshelfOutputWriter#write()
	 */
	@Override
	public void write() throws IOException {
		
		Job.getUserInterface().setProgressNote("Aux File: " + this.fileName);
		
		File file = new File(fileName);
		String genericFileName = file.getName().substring(0, file.getName().indexOf("."));
		
		// RowBasedPlacement : zzz.nodes zzz.nets zzz.wts zzz.pl zzz.scl
		StringBuilder builder = new StringBuilder();
		builder.append("RowBasedPlacement : ");
		builder.append(genericFileName + "." + BookshelfFiles.nodes + " ");
		builder.append(genericFileName + "." + BookshelfFiles.nets + " ");
		builder.append(genericFileName + "." + BookshelfFiles.wts + " ");
		builder.append(genericFileName + "." + BookshelfFiles.pl + " ");
		builder.append(genericFileName + "." + BookshelfFiles.scl + " ");
		
		PrintWriter writer = new PrintWriter(this.fileName);
		writer.println(builder.toString());
		writer.flush();
		writer.close();

	}

}
