/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BookshelfOutput.java
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
import java.text.DateFormat;
import java.util.Date;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.input.bookshelf.Bookshelf.BookshelfFiles;
import com.sun.electric.tool.io.output.Output;

/**
 * @author Felix Schmidt
 * 
 */
public class BookshelfOutput extends Output {

	@SuppressWarnings("serial")
	public static class BookshelfOutputPreferences extends OutputPreferences {

		/**
		 * 
		 */
		public BookshelfOutputPreferences(boolean factory) {
			super(factory);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.sun.electric.tool.io.output.Output.OutputPreferences#doOutput
		 * (com.sun.electric.database.hierarchy.Cell,
		 * com.sun.electric.database.variable.VarContext, java.lang.String)
		 */
		@Override
		public Output doOutput(Cell cell, VarContext context, String filePath) {
			BookshelfOutput output = new BookshelfOutput(filePath);
			output.writeCell(cell, context);
			return output.finishWrite();
		}

	}

	private String filePath;

	private BookshelfOutput(String filePath) {
		this.filePath = filePath;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sun.electric.tool.io.output.Output#writeCell(com.sun.electric.database
	 * .hierarchy.Cell, com.sun.electric.database.variable.VarContext)
	 */
	@Override
	protected boolean writeCell(Cell cell, VarContext context) {
		boolean result = true;
		
		Job.getUserInterface().startProgressDialog("Export Bookshelf", null);

		String genericFileName = this.filePath.substring(0, this.filePath.indexOf("."));

		BookshelfOutputWriter[] writers = new BookshelfOutputWriter[] { new BookshelfOutputAux(genericFileName),
				new BookshelfOutputNodes(genericFileName, cell), new BookshelfOutputPlacement(genericFileName, cell),
				new BookshelfOutputNets(genericFileName, cell), new BookshelfOutputWeights(genericFileName, cell) };

		int sortKey = 0;
		for (BookshelfOutputWriter writer : writers) {
			try {
				writer.write();
			} catch (IOException e) {
				errorLogger.logError("Could not write file " + writer.getFileName(), sortKey);
				sortKey++;
			}
		}
		
		Job.getUserInterface().stopProgressDialog();

		return result;
	}

	public static String createBookshelfHeader(BookshelfFiles fileType) {
		StringBuilder builder = new StringBuilder();

		builder.append("Electric ");
		builder.append(fileType.toString());
		builder.append("\n");

		builder.append("# Created     : ");
		builder.append(DateFormat.getDateInstance(DateFormat.LONG).format(new Date(System.currentTimeMillis())));
		builder.append("\n");
		
		builder.append("# User        : ");
		builder.append(System.getProperty("user.name"));
		builder.append("\n");
		
		builder.append("# Platform    : ");
		builder.append(System.getProperty("os.name") + " ");
		builder.append(System.getProperty("os.arch") + " ");
		builder.append(System.getProperty("os.version"));
		builder.append("\n");

		return builder.toString();
	}
}
