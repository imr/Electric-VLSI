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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.VarContext;
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

		String genericFileName = this.filePath.substring(0, this.filePath.indexOf("."));

		BookshelfOutputWriter[] writers = new BookshelfOutputWriter[] { new BookshelfOutputAux(genericFileName),
				new BookshelfOutputNodes(genericFileName, cell), new BookshelfOutputPlacement(genericFileName, cell),
				new BookshelfOutputNets(genericFileName, cell) };

		int sortKey = 0;
		for (BookshelfOutputWriter writer : writers) {
			try {
				writer.write();
			} catch (IOException e) {
				errorLogger.logError("Could not write file " + writer.getFileName(), sortKey);
				sortKey++;
			}
		}

		return result;
	}
}
