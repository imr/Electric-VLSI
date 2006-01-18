/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NGrid.java
 * MOSIS CMOS PLA Generator.
 * Originally written by Wallace Kroeker at the University of Calgary
 * Translated to Java by Steven Rubin, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.generator.cmosPLA;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * Class to generate the N grid part of MOSIS CMOS PLAs.
 */
public class NGrid
{
	private PLA pla;

	NGrid(PLA pla)
	{
		this.pla = pla;
	}

	Cell nmosGrid(Library library, String fileName, String cellName)
	{
		int y = 0;
		int x = 3;
		int xOffset = PLA.X_SEP;
		int yOffset = PLA.Y_SEP;
		int yMOffset = PLA.Y_MIR_SEP;
		Cell cell = Cell.makeInstance(library, cellName);

		IO perFile = new IO();
		if (!perFile.readHeader(fileName))
		{
			System.out.println("Error reading height and width");
			return null;
		}
		int width = perFile.getWidth();
		int height = perFile.getHeight();
		int widthIn = perFile.getWidthIn();
		int heightIn = perFile.getHeightIn();

		cell.newVar("PLA_data_cols", new Integer(widthIn));
		cell.newVar("PLA_access_rows", new Integer(heightIn));
		cell.newVar("PLA_cols", new Integer(width));
		cell.newVar("PLA_rows", new Integer(height));

		// initialize the columns
		if (nmosInitColumns(width, x, y, xOffset, cell)) return null;

		// initialize the rows
		if (nmosInitRows(heightIn, x, y, yOffset, yMOffset, cell)) return null;

		y = -1 - yOffset;

		x = xOffset;
		int row = 0;
		int readRows = 0;

		while (readRows < heightIn)
		{
			// read in the two rows first
			int [] row1 = null, row2 = null;
			if (readRows < heightIn)
			{
				row1 = perFile.readRow();
				if (row1 == null) return null;
				readRows++;
			}

			if (readRows < heightIn)
			{
				row2 = perFile.readRow();
				if (row2 == null) return null;
				readRows++;
			}

			for (int i = 0; i < width; i++)
			{
				// place a nmos_one cell.  Connect to ground and cell up above
				if (((i % 5) == 0) && (i != 0)) // put in ground strap
				{
					if (gndStrap(i, (xOffset*i)+3, y, row, cell)) return null;
				} else
				{
					// put in bits
					if (row1[i] == 1)
					{
						// make an instance of a nmos_one
						if (nmosMakeOne(i, i*xOffset, y, false, row, cell)) return null;
					}
					if (row2[i] == 1)
					{
						// make an instance of a nmos_one
						// MIRROR object Horizontally
						if (nmosMakeOne(i, i*xOffset, y-yMOffset, true, row, cell)) return null;
					}
				}
			}

			// Now put the poly metal contact at on the other side of the row
			if (completeRow(row, xOffset*width, y, cell)) return null;
			if ((readRows % 4) == 0) y -= yOffset;

			row++;
			y -= 2*yOffset;
		}
		x = 3;
		if ((readRows % 4) == 0) y += 2*yOffset; else
			y += yOffset;
		if (finishColumns(width, x, y, xOffset, cell)) return null;

		// put a well node over the region
		Rectangle2D cellBounds = cell.getBounds();
		NodeInst ni = NodeInst.makeInstance(pla.pwNode, new Point2D.Double(cellBounds.getCenterX(), cellBounds.getCenterY()),
			cellBounds.getWidth(), cellBounds.getHeight(), cell);

		perFile.done();

		return(cell);
	}

	private boolean gndStrap(int i, int x, int y, int row, Cell arrayCell)
	{
		PLA.UCItem newItem = new PLA.UCItem();

		// put in Substrate contact first
		newItem.nodeInst = pla.makePin(arrayCell, x, y+11, 14, pla.mwBut);
		if (newItem.nodeInst == null) return true;

		// wire in the first ground strap
		PortProto nodeColPort = newItem.nodeInst.getProto().getPort(0);
		PortProto lastColPort = pla.columnList[i].lastitem.nodeInst.getProto().getPort(0);

		// connect to last column object
		pla.makeWire(pla.m1Arc, 4, pla.columnList[i].lastitem.nodeInst,
			lastColPort, newItem.nodeInst, nodeColPort, arrayCell);

		// only put this in a column list
		pla.columnList[i].lastitem.bottomItem = newItem;
		pla.columnList[i].lastitem = pla.columnList[i].lastitem.bottomItem;

		// put in metal diffusion contact
		newItem = new PLA.UCItem();
		newItem.nodeInst = pla.makePin(arrayCell, x, y+1, 14, pla.maCon);
		if (newItem.nodeInst == null) return true;

		// wire in the first ground strap
		nodeColPort = newItem.nodeInst.getProto().getPort(0);
		lastColPort = pla.columnList[i].lastitem.nodeInst.getProto().getPort(0);

		// connect to last column object
		pla.makeWire(pla.m1Arc, 4, pla.columnList[i].lastitem.nodeInst,
			lastColPort, newItem.nodeInst, nodeColPort, arrayCell);

		// connect to last row object
		if (pla.rowList[row][1].lastitem.nodeInst.getProto() != pla.nmosOne)
			lastColPort = pla.rowList[row][1].lastitem.nodeInst.getProto().getPort(0); else
				lastColPort = pla.rowList[row][1].lastitem.nodeInst.getProto().findPortProto("GND.d.s");
		pla.makeWire(pla.aArc, 14, pla.rowList[row][1].lastitem.nodeInst,
			lastColPort, newItem.nodeInst, nodeColPort, arrayCell);

		// insert into row and column lists
		pla.columnList[i].lastitem.bottomItem = newItem;
		pla.columnList[i].lastitem = pla.columnList[i].lastitem.bottomItem;
		pla.rowList[row][1].lastitem.rightItem = newItem;
		pla.rowList[row][1].lastitem = pla.rowList[row][1].lastitem.rightItem;
		return false;
	}

	private boolean nmosMakeOne(int i, int x, int y, boolean mirror, int row, Cell arrayCell)
	{
		/*
		 * a row has three positions 0 (a normal one object), 1 (a ground strap),
		 * and  2 (a mirrored one object).  NOTE: There is only a linked list of the
		 * ground objects
		 */
		int rowPos = 0;
		if (mirror)  // row index value for the one object
			rowPos = 2;

		NodeInst ni = pla.makeInstance(arrayCell, pla.nmosOne, x, y, mirror);
		if (ni == null) return true;

		// find the ports that need to be connected
		PortProto nodeColPort = ni.getProto().findPortProto("OUT.m-1.n");
		PortProto lastColPort;
		if (pla.columnList[i].lastitem.nodeInst.getProto() != pla.nmosOne) // a Metal pin
			lastColPort = pla.columnList[i].firstItem.nodeInst.getProto().getPort(0); else
				lastColPort = pla.columnList[i].lastitem.nodeInst.getProto().findPortProto("OUT.m-1.n");

		// connect to last column object
		pla.makeWire(pla.m1Arc, 4, pla.columnList[i].lastitem.nodeInst, lastColPort, ni, nodeColPort, arrayCell);

		// connect to last ground object
		if (pla.rowList[row][1].lastitem == null)
		{
			System.out.println("No UCITEM at row " + row);
			return true;
		}
		if (pla.rowList[row][1].lastitem.nodeInst == null)
		{
			System.out.println("No NODEINST at row 1 " + row);
			return true;
		}

		if (pla.rowList[row][1].lastitem.nodeInst.getProto() != pla.nmosOne)
			lastColPort = pla.rowList[row][1].lastitem.nodeInst.getProto().getPort(0); else
				lastColPort =  pla.rowList[row][1].lastitem.nodeInst.getProto().findPortProto("GND.d.s");
		if (lastColPort == null)
		{
			System.out.println("No NODEPROTO for GND.d.s");
			return true;
		}
		nodeColPort = ni.getProto().findPortProto("GND.d.s");
		pla.makeWire(pla.aArc, 14, pla.rowList[row][1].lastitem.nodeInst, lastColPort, ni, nodeColPort, arrayCell);

		// connect to last GATE object
		if (pla.rowList[row][rowPos].lastitem.nodeInst.getProto() != pla.nmosOne)
			lastColPort = pla.rowList[row][rowPos].lastitem.nodeInst.getProto().getPort(0); else
				lastColPort =  pla.rowList[row][rowPos].lastitem.nodeInst.getProto().findPortProto("GATE.p.e");
		nodeColPort = ni.getProto().findPortProto("GATE.p.w");
		pla.makeWire(pla.pArc, 0, pla.rowList[row][rowPos].lastitem.nodeInst, lastColPort,
			ni, nodeColPort, arrayCell);

		// put in column list
		pla.columnList[i].lastitem.bottomItem = new PLA.UCItem();
		pla.columnList[i].lastitem = pla.columnList[i].lastitem.bottomItem;
		pla.columnList[i].lastitem.nodeInst = ni;

		// put in ground row list
		pla.rowList[row][1].lastitem.rightItem = pla.columnList[i].lastitem;
		pla.rowList[row][1].lastitem = pla.rowList[row][1].lastitem.rightItem;

		// only keep track of the last item in this row
		// the ground list above (index 1) holds the complete list
		pla.rowList[row][rowPos].lastitem = pla.rowList[row][1].lastitem;
		return false;
	}

	private boolean completeRow(int row, int x, int y, Cell arrayCell)
	{
		PLA.UCItem newItem = new PLA.UCItem();
		newItem.nodeInst = pla.makePin(arrayCell, x, y+6, 6, pla.mpCon);
		if (newItem.nodeInst == null) return true;

		// connect to last GATE object
		PortProto lastColPort;
		if (pla.rowList[row][0].lastitem.nodeInst.getProto() != pla.nmosOne)
			lastColPort = pla.rowList[row][0].lastitem.nodeInst.getProto().getPort(0); else
				lastColPort =  pla.rowList[row][0].lastitem.nodeInst.getProto().findPortProto("GATE.p.e");
		PortProto nodeColPort = newItem.nodeInst.getProto().getPort(0);
		pla.makeWire(pla.pArc, 0, pla.rowList[row][0].lastitem.nodeInst, lastColPort, newItem.nodeInst, nodeColPort, arrayCell);
		pla.rowList[row][0].lastitem = newItem;

		// Now export port at beginning and end of this half of the row grouping
		PortProto pp = pla.rowList[row][0].firstItem.nodeInst.getProto().getPort(0);
		PortInst pi = pla.rowList[row][0].firstItem.nodeInst.findPortInstFromProto(pp);
		Export.newInstance(arrayCell, pi, "ACCESS" + (row * 2) + ".m-1.w");

		pp = pla.rowList[row][0].lastitem.nodeInst.getProto().getPort(0);
		pi = pla.rowList[row][0].lastitem.nodeInst.findPortInstFromProto(pp);
		Export.newInstance(arrayCell, pi, "ACCESS" + (row * 2) + ".m-1.e");

		pp = pla.rowList[row][0].firstItem.nodeInst.getProto().getPort(0);
		pi = pla.rowList[row][0].firstItem.nodeInst.findPortInstFromProto(pp);
		Export.newInstance(arrayCell, pi, "ACCESS" + (row * 2) + ".p.w");

		pp = pla.rowList[row][0].lastitem.nodeInst.getProto().getPort(0);
		pi = pla.rowList[row][0].lastitem.nodeInst.findPortInstFromProto(pp);
		Export.newInstance(arrayCell, pi, "ACCESS" + (row * 2) + ".p.e");

		newItem = new PLA.UCItem();
		newItem.nodeInst = pla.makePin(arrayCell, x, y-4, 6, pla.mpCon);
		if (newItem.nodeInst == null) return true;

		// connect to last GATE object
		if (pla.rowList[row][2].lastitem.nodeInst.getProto() != pla.nmosOne)
			lastColPort = pla.rowList[row][2].lastitem.nodeInst.getProto().getPort(0); else
				lastColPort =  pla.rowList[row][2].lastitem.nodeInst.getProto().findPortProto("GATE.p.e");
		nodeColPort = newItem.nodeInst.getProto().getPort(0);
		pla.makeWire(pla.pArc, 0, pla.rowList[row][2].lastitem.nodeInst, lastColPort, newItem.nodeInst, nodeColPort, arrayCell);
		pla.rowList[row][2].lastitem = newItem;

		pp = pla.rowList[row][2].firstItem.nodeInst.getProto().getPort(0);
		pi = pla.rowList[row][2].firstItem.nodeInst.findPortInstFromProto(pp);
		Export.newInstance(arrayCell, pi, "ACCESS" + (row * 2 + 1) + ".m-1.w");

		pp = pla.rowList[row][2].lastitem.nodeInst.getProto().getPort(0);
		pi = pla.rowList[row][2].lastitem.nodeInst.findPortInstFromProto(pp);
		Export.newInstance(arrayCell, pi, "ACCESS" + (row * 2 + 1) + ".m-1.e");

		pp = pla.rowList[row][2].firstItem.nodeInst.getProto().getPort(0);
		pi = pla.rowList[row][2].firstItem.nodeInst.findPortInstFromProto(pp);
		Export.newInstance(arrayCell, pi, "ACCESS" + (row * 2 + 1) + ".p.w");

		pp = pla.rowList[row][2].lastitem.nodeInst.getProto().getPort(0);
		pi = pla.rowList[row][2].lastitem.nodeInst.findPortInstFromProto(pp);
		Export.newInstance(arrayCell, pi, "ACCESS" + (row * 2 + 1) + ".p.e");
		return false;
	}

	private boolean nmosInitColumns(int width, int x, int y, int xOffset, Cell arrayCell)
	{
		int gndCnt = 0;
		for (int i = 0; i < width; i++)
		{
			pla.columnList[i].firstItem = new PLA.UCItem();
			pla.columnList[i].lastitem =  pla.columnList[i].firstItem;

			// put in a Ground  pin every 5th position
			String name;
			if ((i % 5) == 0)
			{
				name = "GND" + gndCnt + ".m-1.n";
				pla.columnList[i].firstItem.nodeInst = pla.makePin(arrayCell,
					xOffset * i + x, y, 14, pla.mwBut);
				if (pla.columnList[i].firstItem.nodeInst == null) return true;
				gndCnt++;
			} else
			{
				// must be a data pin, don't count ground pins
				name = "DATA" + (i - gndCnt) + ".m-1.n";
				pla.columnList[i].firstItem.nodeInst = pla.makePin(arrayCell, xOffset * i + x, y, 4, pla.m1Pin);
				if (pla.columnList[i].firstItem.nodeInst == null) return true;
			}
			PortProto pp = pla.columnList[i].firstItem.nodeInst.getProto().getPort(0);
			PortInst pi = pla.columnList[i].firstItem.nodeInst.findPortInstFromProto(pp);
			Export.newInstance(arrayCell, pi, name);
		}
		return false;
	}

	private boolean nmosInitRows(int heightIn, int x, int y, int yOffset, int yMOffset, Cell arrayCell)
	{
		int limit = (heightIn/2) + (heightIn % 2);
		for (int i = 0; i < limit; i++)
		{
			if (((i % 2) == 0) && (i != 0)) y -= yOffset;

			PLA.UCItem newItem = new PLA.UCItem();

			// put in Substrate contact first
			newItem.nodeInst = pla.makePin(arrayCell, x, y-yMOffset+10, 14, pla.mwBut);
			if (newItem.nodeInst == null) return true;

			// wire in the first ground strap
			PortProto nodeColPort = newItem.nodeInst.getProto().getPort(0);
			PortProto lastColPort = pla.columnList[0].lastitem.nodeInst.getProto().getPort(0);

			// connect to last column object
			pla.makeWire(pla.m1Arc, 4, pla.columnList[0].lastitem.nodeInst, lastColPort, newItem.nodeInst, nodeColPort, arrayCell);

			// only put this in a column list
			pla.columnList[0].lastitem.bottomItem = newItem;
			pla.columnList[0].lastitem = pla.columnList[0].lastitem.bottomItem;

			pla.rowList[i][0].firstItem = new PLA.UCItem();
			pla.rowList[i][0].lastitem =  pla.rowList[i][0].firstItem;
			pla.rowList[i][1].firstItem = new PLA.UCItem();
			pla.rowList[i][1].lastitem =  pla.rowList[i][1].firstItem;
			pla.rowList[i][2].firstItem = new PLA.UCItem();
			pla.rowList[i][2].lastitem =  pla.rowList[i][2].firstItem;

			pla.rowList[i][0].firstItem.nodeInst = pla.makePin(arrayCell, x-7, y-5, 6, pla.mpCon);
			if (pla.rowList[i][0].firstItem.nodeInst == null) return true;
			pla.rowList[i][1].firstItem.nodeInst = pla.makePin(arrayCell, x, y-yMOffset, 14, pla.maCon);
			if (pla.rowList[i][1].firstItem.nodeInst == null) return true;
			pla.rowList[i][2].firstItem.nodeInst = pla.makePin(arrayCell, x-7, y-15, 6, pla.mpCon);
			if (pla.rowList[i][2].firstItem.nodeInst == null) return true;

			// wire in the first ground strap
			nodeColPort = pla.rowList[i][1].firstItem.nodeInst.getProto().getPort(0);
			lastColPort = pla.columnList[0].lastitem.nodeInst.getProto().getPort(0);

			// connect to last column object
			pla.makeWire(pla.m1Arc, 4, pla.columnList[0].lastitem.nodeInst, lastColPort, pla.rowList[i][1].lastitem.nodeInst, nodeColPort, arrayCell);
			pla.columnList[0].lastitem.bottomItem = pla.rowList[i][1].lastitem;
			pla.columnList[0].lastitem = pla.columnList[0].lastitem.bottomItem;

			y -= 2*yOffset;
		}
		return false;
	}

	private boolean finishColumns(int width, int x, int y, int xOffset, Cell arrayCell)
	{
		int gndCnt = 0;
		for (int i = 0; i < width; i++)
		{
			PLA.UCItem newItem = new PLA.UCItem();

			// put in a Ground  pin every 5th position
			String name;
			PortProto lastColPort;
			if ((i % 5) == 0)
			{
				name = "GND" + gndCnt + ".m-1.s";
				newItem.nodeInst = pla.makePin(arrayCell, xOffset*i+x, y, 14, pla.mwBut);
				if (newItem.nodeInst == null) return true;
				lastColPort = pla.columnList[i].lastitem.nodeInst.getProto().getPort(0);
				gndCnt++;
			} else
			{
				// must be a data pin, don't count ground pins
				name = "DATA" + (i - gndCnt) + ".m-1.s";
				newItem.nodeInst = pla.makePin(arrayCell, xOffset * i + x, y, 4, pla.m1Pin);
				if (newItem.nodeInst == null) return true;
				if (pla.columnList[i].lastitem.nodeInst.getProto() != pla.nmosOne)
					lastColPort = pla.columnList[i].lastitem.nodeInst.getProto().getPort(0); else
						lastColPort = pla.columnList[i].lastitem.nodeInst.getProto().findPortProto("OUT.m-1.n");
			}

			// wire in the first ground strap
			PortProto nodeColPort = newItem.nodeInst.getProto().getPort(0);

			// connect to last column object
			pla.makeWire(pla.m1Arc, 4, pla.columnList[i].lastitem.nodeInst, lastColPort, newItem.nodeInst, nodeColPort, arrayCell);

			// only put this in a column list
			pla.columnList[i].lastitem.bottomItem = newItem;
			pla.columnList[i].lastitem = pla.columnList[i].lastitem.bottomItem;
			PortInst pi = newItem.nodeInst.findPortInstFromProto(newItem.nodeInst.getProto().getPort(0));
			Export.newInstance(arrayCell, pi, name);
		}
		return false;
	}
}
