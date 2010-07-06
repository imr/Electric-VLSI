/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BookshelfNets.java
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

import java.awt.geom.Point2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable.Key;
import com.sun.electric.tool.io.input.bookshelf.BookshelfNodes.BookshelfNode;
import com.sun.electric.tool.io.input.bookshelf.BookshelfNodes.BookshelfPin;
import com.sun.electric.tool.util.CollectionFactory;

/**
 * @author fschmidt
 * 
 */
public class BookshelfNets {

	private String fileName;
	private Cell cell;

	public BookshelfNets(String fileName, Cell cell) {
		this.fileName = fileName;
		this.cell = cell;
	}

	public void parse() throws IOException {

		File file = new File(this.fileName);
		FileReader freader = new FileReader(file);
		BufferedReader rin = new BufferedReader(freader);

		Map<String, List<BookshelfPin>> pins = CollectionFactory.createHashMap();

		String line;
		while ((line = rin.readLine()) != null) {
			if (line.startsWith("NetDegree")) {
				NetDesc desc = parseNetDesc(line);
				BookshelfNet net = new BookshelfNet(desc.netName);
				for (int i = 0; i < desc.elements; i++) {
					line = rin.readLine();
					BookshelfPin pin = parsePin(line, net);
					if (!pins.containsKey(pin.getNodeName())) {
						pins.put(pin.getNodeName(), new ArrayList<BookshelfPin>());
					}
					pins.get(pin.getNodeName()).add(pin);
				}
			}
		}

		Iterator<NodeInst> niIt = cell.getNodes();
		while (niIt.hasNext()) {

			NodeInst ni = niIt.next();
			List<BookshelfPin> pinObjs = pins.get(ni.getName());
			if (pinObjs != null) {
				for (BookshelfPin pin : pinObjs) {
					PortProto pp = ni.getProto().getPort(0);
//					PortInst pi = PortInst.newInstance(pp, ni);
					PortInst pi = ni.findPortInstFromProto(pp);
					Export exp = Export.newInstance(cell, pi, pin.getNodeName() + pin.getNet().name);
					
					//TODO stuck here
				}
			}
		}
	}

	private BookshelfPin parsePin(String line, BookshelfNet net) {

		String[] splited = line.trim().split(" ");

		double xPos = 0;
		double yPos = 0;
		if (splited.length == 6) {
			xPos = Double.parseDouble(splited[4]);
			yPos = Double.parseDouble(splited[5]);
		}

		Point2D location = new Point2D.Double(xPos, yPos);
		return new BookshelfPin(location, net, splited[0]);
	}

	private NetDesc parseNetDesc(String line) {
		NetDesc result = new NetDesc();

		String[] splited = line.split(" ");
		result.elements = Integer.parseInt(splited[2]);
		result.netName = splited[4];

		return result;
	}

	private class NetDesc {
		public String netName;
		public int elements;
	}

	public static class BookshelfNet {
		private List<BookshelfNode> nodes;
		private List<BookshelfPin> pins;
		private String name;

		public BookshelfNet(String name) {
			nodes = CollectionFactory.createArrayList();
			pins = CollectionFactory.createArrayList();
			this.name = name;
		}

		/**
		 * @param nodes
		 *            the nodes to set
		 */
		public void setNodes(List<BookshelfNode> nodes) {
			this.nodes = nodes;
		}

		/**
		 * @return the nodes
		 */
		public List<BookshelfNode> getNodes() {
			return nodes;
		}

		/**
		 * @param pins
		 *            the pins to set
		 */
		public void setPins(List<BookshelfPin> pins) {
			this.pins = pins;
		}

		/**
		 * @return the pins
		 */
		public List<BookshelfPin> getPins() {
			return pins;
		}
	}

}
