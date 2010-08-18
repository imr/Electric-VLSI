/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BookshelfOutputNets.java
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.input.bookshelf.Bookshelf.BookshelfFiles;

/**
 * @author Felix Schmidt
 * 
 */
public class BookshelfOutputNets extends BookshelfOutputWriter {

	private static final BookshelfFiles fileType = BookshelfFiles.nets;

	private Cell cell;

	/**
	 * 
	 */
	public BookshelfOutputNets(String genericFileName, Cell cell) {
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
		
		Job.getUserInterface().setProgressNote("Nets File: " + this.fileName);

		PrintWriter writer = new PrintWriter(this.fileName);
		
		writer.println(BookshelfOutput.createBookshelfHeader(fileType));
		
		int size = cell.getNetlist().getNumNetworks();

		int counter = 0;
		for (Iterator<Network> inet = cell.getNetlist().getNetworks(); inet.hasNext();) {
			if(counter % 20 == 0)
				Job.getUserInterface().setProgressValue((int)(((double)counter/(double)size) * 100.0));
			Network net = inet.next();
			writer.println("NetDegree : " + net.getPortsList().size() + "  " + net.getName());
			for (PortInst pi : net.getPortsList()) {
				writer.print("    " + pi.getNodeInst().getName() + " I  : ");
				double x = pi.getCenter().getX() - pi.getNodeInst().getTrueCenterX();
				double y = pi.getCenter().getY() - pi.getNodeInst().getTrueCenterY();
				writer.print(String.valueOf(x) + " ");
				writer.println(String.valueOf(y));
			}
			counter++;
		}

		writer.flush();
		writer.close();

	}
}
