/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Gallery.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.generator.layout;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.ListIterator;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;

public class Gallery {
	static final double PAGE_WIDTH = 1000;
	static final double HORIZONTAL_SPACE = 30;
	static final double VERTICAL_SPACE = 30;
	static final double TEXT_OFFSET_BELOW_CELL = 10;
	private PrimitiveNode textPin;
	private StdCellParams stdCell;
	private Library lib;

	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}
	ArrayList readLayoutCells(Library lib) {
		ArrayList cells = new ArrayList();
		Iterator it = lib.getCells();
		View layView = View.findView("layout");
		while (it.hasNext()) {
			Cell c = (Cell) it.next();
			if (c.getView()==layView)  cells.add(c);
		}
		return cells;
	}

	void sortCellsByName(ArrayList facets) {
		Collections.sort(facets, new Comparator() {
			public int compare(Object o1, Object o2) {
				String n1 = ((Cell)o1).getName();
				String n2 = ((Cell)o2).getName();
				return n1.compareTo(n2);
			}
		});
	}

	void printCells(ArrayList facets) {
		for (int i=0; i<facets.size(); i++) {
			Cell p = (Cell) facets.get(i);
			System.out.println(p.getName());
		}
	}

	ArrayList addOneInstOfEveryCell(ArrayList facets, Cell gallery) {
		ArrayList insts = new ArrayList();
		for (int i=0; i<facets.size(); i++) {
			NodeInst ni = LayoutLib.newNodeInst((Cell)facets.get(i), 0, 0, 0,
			                                    0, 0, gallery);
			insts.add(ni);
		}
		return insts;
	}

	double width(NodeInst ni) {
		return LayoutLib.getBounds(ni).getWidth();
	}

	double height(NodeInst ni) {
		return LayoutLib.getBounds(ni).getHeight();
	}

	double[] getRow(ArrayList row, ListIterator it) {
		double x = 0;
		double highestAboveCenter = Double.MIN_VALUE;
		double lowestBelowCenter = Double.MIN_VALUE;
		while (it.hasNext()) {
			NodeInst ni = (NodeInst) it.next();
			// always add at least 1 part to each row
			if (x!=0 && x+width(ni)>PAGE_WIDTH) {it.previous();  break;}
			row.add(ni);
			Rectangle2D bounds = LayoutLib.getBounds(ni);
			highestAboveCenter = Math.max(highestAboveCenter, bounds.getMaxY());
			lowestBelowCenter = Math.min(lowestBelowCenter, bounds.getMinY());
			x = x + width(ni) + HORIZONTAL_SPACE;
		}
		return new double[] {highestAboveCenter, lowestBelowCenter};
	}

	void placeRow(ArrayList row, double centerY, Cell gallery) {
		double curLeftX = 0;
		//System.out.println("Row at: "+y);
		for (int i=0; i<row.size(); i++) {
			NodeInst ni = (NodeInst) row.get(i);
			double x = LayoutLib.getBounds(ni).getMinX();
			double y = LayoutLib.getPosition(ni).getY();
//			System.out.println("Instance initial bounding box: "+r);
//			System.out.println("Put instance at: ("+x+", "+(y-r.getY())+")");

			LayoutLib.modNodeInst(ni, curLeftX-x, centerY-y, 0,0,false,false,0);

			// label instance with text
			double defSz = LayoutLib.DEF_SIZE;

			NodeInst ti = 
			  LayoutLib.newNodeInst(textPin, curLeftX+width(ni)/2, centerY-TEXT_OFFSET_BELOW_CELL, defSz, 
			                        defSz, 0, gallery);
			ti.setExpanded();
//			String partNm = ni.getProto().getName();
//			System.out.println("Cell: "+partNm+" has width: "+width(ni));
			//ti.setVar("ART_message", partNm);
			//String s = (String) ni.getVar("ART_message");
			//System.out.println("placeRow: NodeInst Width: "+width(ni));
			curLeftX = curLeftX + width(ni) + HORIZONTAL_SPACE;
		}
	}

	void placeInstsOnPage(ArrayList insts, Cell gallery) {
		double topY = 0;
		for (ListIterator it=insts.listIterator(); it.hasNext();) {
			ArrayList row = new ArrayList();
			double[] hiLo = getRow(row, it);
			double highestAboveCenter = hiLo[0];
			double lowestBelowCenter = hiLo[1];
			double centerY = topY - highestAboveCenter; 
			placeRow(row, centerY, gallery);
			topY = centerY + lowestBelowCenter - VERTICAL_SPACE;
		}
	}

	Gallery(Library lib) {
		this.lib = lib;

		Technology generic = Technology.findTechnology("generic");
		error(generic == null, "No generic technology?");
		textPin = generic.findNodeProto("Invisible-Pin");

		stdCell = new StdCellParams(lib);
	}

	Cell makeGallery1() {
		ArrayList cells = readLayoutCells(lib);
		System.out.println("Gallery contains: " + cells.size() + " Cells");

		sortCellsByName(cells);

		Cell gallery = Cell.newInstance(lib, "gallery{lay}");
		ArrayList insts = addOneInstOfEveryCell(cells, gallery);

		placeInstsOnPage(insts, gallery);

		return gallery;
	}

	/**
	 * Create a new Cell named "gallery" in Library "lib".  Into
	 * Gallery place one instance of every Cell in "lib".  Arrange
	 * instances in rows sorted alphabetically by Cell name.
	 */
	public static Cell makeGallery(Library lib) {
		Gallery galleryMaker = new Gallery(lib);
		return galleryMaker.makeGallery1();
	}

	// generate Gallery for currently open library
	public static void main(String[] args) {
		Library lib = Library.getCurrent();
		error(lib == null, "No currently open library?");
		makeGallery(lib);

		System.out.println("Done");
	}
}
