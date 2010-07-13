/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BookshelfNodes.java
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
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.tool.io.input.bookshelf.BookshelfNets.BookshelfNet;
import com.sun.electric.tool.util.CollectionFactory;

/**
 * @author fschmidt
 * 
 */
public class BookshelfNodes {

	private String nodesFile;
	private Library lib;
	private String name;
	private Cell cell;

	public BookshelfNodes(String nodesFile, Library lib, String name, Cell cell) {
		this.nodesFile = nodesFile;
		this.lib = lib;
		this.name = name;
		this.cell = cell;
	}

	public void parse() throws IOException {
		File file = new File(this.nodesFile);
		FileReader freader = new FileReader(file);
		BufferedReader rin = new BufferedReader(freader);

		String line;
		while ((line = rin.readLine()) != null) {
			if (line.startsWith("   ")) {
				StringTokenizer tokenizer = new StringTokenizer(line, " ");
				int i = 0;
				String name = "";
				int height = 0;
				int width = 0;
				while (tokenizer.hasMoreTokens()) {
					if (i == 0) {
						name = tokenizer.nextToken();
					} else if (i == 1) {
						width = Integer.parseInt(tokenizer.nextToken());
					} else if (i == 2) {
						height = Integer.parseInt(tokenizer.nextToken());
					} else {
						tokenizer.nextToken();
					}
					i++;
				}
				NodeProto np = Artwork.tech().boxNode;
				NodeInst ni = NodeInst.newInstance(np, new Point2D.Double(0, 0), width, height,
						cell, Orientation.IDENT, name);
			}
		}
	}

	public static class BookshelfNode {
		private String name;
		private int width;
		private int height;
		private List<BookshelfPin> pins;

		/**
		 * @return the name
		 */
		public String getName() {
			return name;
		}

		/**
		 * @param name
		 *            the name to set
		 */
		public void setName(String name) {
			this.name = name;
		}

		/**
		 * @return the width
		 */
		public int getWidth() {
			return width;
		}

		/**
		 * @param width
		 *            the width to set
		 */
		public void setWidth(int width) {
			this.width = width;
		}

		/**
		 * @return the height
		 */
		public int getHeight() {
			return height;
		}

		/**
		 * @param height
		 *            the height to set
		 */
		public void setHeight(int height) {
			this.height = height;
		}

		/**
		 * @param name
		 * @param width
		 * @param height
		 */
		public BookshelfNode(String name, int width, int height) {
			this.name = name;
			this.width = width;
			this.height = height;
			this.pins = CollectionFactory.createArrayList();
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

	public static class BookshelfPin {
		private Point2D location;
		private BookshelfNet net;
		private String nodeName;

		/**
		 * @param location
		 */
		public BookshelfPin(Point2D location, BookshelfNet net, String nodeName) {
			super();
			this.location = location;
			this.setNet(net);
			this.nodeName = nodeName;
		}

		/**
		 * @param location
		 *            the location to set
		 */
		public void setLocation(Point2D location) {
			this.location = location;
		}

		/**
		 * @return the location
		 */
		public Point2D getLocation() {
			return location;
		}

		/**
		 * @param net
		 *            the net to set
		 */
		public void setNet(BookshelfNet net) {
			this.net = net;
		}

		/**
		 * @return the net
		 */
		public BookshelfNet getNet() {
			return net;
		}

		/**
		 * @param nodeName the nodeName to set
		 */
		public void setNodeName(String nodeName) {
			this.nodeName = nodeName;
		}

		/**
		 * @return the nodeName
		 */
		public String getNodeName() {
			return nodeName;
		}
	}
}
