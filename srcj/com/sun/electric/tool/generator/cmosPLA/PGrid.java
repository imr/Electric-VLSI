/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PGrid.java
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

/**
 * Class to generate the P grid part of MOSIS CMOS PLAs.
 */
public class PGrid
{
	private PLA pla;

	PGrid(PLA pla)
	{
		this.pla = pla;
	}

	Cell pmosGrid(Library library, String fileName, String cellName)
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
		if (pmosInitColumns(width, x, y, xOffset, cell)) return null;

		// initialize the rows
		if (pmosInitRows(heightIn, x, y, yOffset, yMOffset, cell)) return null;

		y = 3 - yOffset;
		x = xOffset + 4;
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

			for(int i = 0; i < width; i++)
			{
				// place a pmos_one cell and connect to power, and cell up above
				if (((i % 5) == 0) && (i != 0)) // put in power strap
				{
					if (pwrStrap(i, ((xOffset*i)+3), y, cell)) return null;
				} else
				{
					// put in bits -- make an instance of a pmos_one
					if ((row1[i] == 1) && (row1[0] != -1))
					{
						// make an instance of a pmos_one
						if (pmosMakeOne(i, ((i*xOffset)+6), y, 0, row, cell)) return null;
					}
					if ((row2[i] == 1) && (row2[0] != -1))
					{
						// make an instance of a pmos_one
						if (pmosMakeOne(i, ((i*xOffset)+6), y-yMOffset, 2, row, cell)) return null;
					}
				}
			}

			// Now put the poly metal contact at on the other side of the row
			if (pmosCompleteRow(row, ((xOffset*width)+0), y, cell)) return null;

			if ((readRows % 4) == 0) y -= yOffset;

			row++;
			y -= 2*yOffset;
		}
		x = 3;
		if ((readRows % 4) == 0) y += 2*yOffset-3; else
			y += yOffset-3;
		if (pmosFinishColumns(width, x, y, xOffset, cell)) return null;

		perFile.done();
		return cell;
	}

	private boolean pmosInitColumns(int width, int x, int y, int xOffset, Cell arrayCell)
	{
		int pwrCnt = 0;
		for (int i = 0; i < width; i++)
		{
			pla.columnList[i].firstItem = new PLA.UCItem();
			pla.columnList[i].lastitem =  pla.columnList[i].firstItem;

			String name;
			if ((i % 5) == 0)
			{
				// put in a Power pin every 5th position
				name = "PWR" + pwrCnt + ".m-1.n";
				pla.columnList[i].firstItem.nodeInst = pla.makePin(arrayCell, xOffset * i + x, y, 14, pla.msBut);
				if (pla.columnList[i].firstItem.nodeInst == null) return true;
				pwrCnt++;
			} else
			{
				// must be a data pin, don't count power pins
				name = "DATA" + (i - pwrCnt) + ".m-1.n";
				pla.columnList[i].firstItem.nodeInst = pla.makePin(arrayCell, xOffset * i + x, y, 4, pla.m1Pin);
				if (pla.columnList[i].firstItem.nodeInst == null) return true;
			}
			PortProto pp = pla.columnList[i].firstItem.nodeInst.getProto().getPort(0);
			PortInst pi = pla.columnList[i].firstItem.nodeInst.findPortInstFromProto(pp);
			Export.newInstance(arrayCell, pi, name);
		}
		return false;
	}

	private boolean pmosInitRows(int heightIn, int x, int y, int yOffset, int yMOffset, Cell arrayCell)
	{
		int limit = (heightIn/2) + (heightIn % 2);

		for (int i = 0; i < limit; i++)
		{
			PLA.UCItem newItem = new PLA.UCItem();

			if (((i % 2) == 0) && (i != 0)) y -= yOffset;

			// put in Substrate contact first
			newItem.nodeInst = pla.makePin(arrayCell, x, y-yMOffset+10, 14, pla.msBut);
			if (newItem.nodeInst == null) return true;

			// wire in the first power strap
			// connect to last column object
			// only put this in a column list
			PortProto nodeColPort = newItem.nodeInst.getProto().getPort(0);
			PortProto lastColPort = pla.columnList[0].lastitem.nodeInst.getProto().getPort(0);
			pla.makeWire(pla.m1Arc, 4, pla.columnList[0].lastitem.nodeInst, lastColPort, newItem.nodeInst, nodeColPort, arrayCell);

			pla.columnList[0].lastitem.bottomItem = newItem;
			pla.columnList[0].lastitem = pla.columnList[0].lastitem.bottomItem;

			pla.rowList[i][0].firstItem = new PLA.UCItem();
			pla.rowList[i][0].lastitem =  pla.rowList[i][0].firstItem;
			pla.rowList[i][2].firstItem = new PLA.UCItem();
			pla.rowList[i][2].lastitem =  pla.rowList[i][2].firstItem;

			pla.rowList[i][0].firstItem.nodeInst = pla.makePin(arrayCell, x-7, y-5, 6, pla.mpCon);
			if (pla.rowList[i][0].firstItem.nodeInst == null) return true;
			pla.rowList[i][2].firstItem.nodeInst = pla.makePin(arrayCell, x-7, y-15, 6, pla.mpCon);
			if (pla.rowList[i][2].firstItem.nodeInst == null) return true;

			y -= 2*yOffset;
		}
		return false;
	}

	private boolean pwrStrap(int i, int x, int y, Cell arrayCell)
	{
		PLA.UCItem newItem = new PLA.UCItem();

		// put in Substrate contact first
		newItem.nodeInst = pla.makePin(arrayCell, x, y+7, 14, pla.msBut);
		if (newItem.nodeInst == null) return true;

		// wire in the first power strap and connect to last column object
		PortProto nodeColPort = newItem.nodeInst.getProto().getPort(0);
		PortProto lastColPort = pla.columnList[i].lastitem.nodeInst.getProto().getPort(0);

		pla.makeWire(pla.m1Arc, 4, pla.columnList[i].lastitem.nodeInst, lastColPort, newItem.nodeInst, nodeColPort, arrayCell);

		// only put this in a column list
		pla.columnList[i].lastitem.bottomItem = newItem;
		pla.columnList[i].lastitem = pla.columnList[i].lastitem.bottomItem;
		return false;
	}

	private boolean pmosMakeOne(int i, int x, int y, int rowPos, int row, Cell arrayCell)
	{
		NodeInst ni = pla.makeInstance(arrayCell, pla.pmosOne, x, y, false);
		if (ni == null) return true;

		// find ports that need to be connected and connect to last column object
		PortProto nodeColPort = ni.getProto().findPortProto("IN.m-1.n");
		PortProto lastColPort;
		if (pla.columnList[i].lastitem.nodeInst.getProto() != pla.pmosOne) // a Metal pin
			lastColPort = pla.columnList[i].firstItem.nodeInst.getProto().getPort(0); else
				lastColPort = pla.columnList[i].lastitem.nodeInst.getProto().findPortProto("OUT.m-1.s");

		pla.makeWire(pla.m1Arc, 4, pla.columnList[i].lastitem.nodeInst, lastColPort, ni, nodeColPort, arrayCell);

		// connect to last GATE object
		if (pla.rowList[row][rowPos].lastitem.nodeInst.getProto() != pla.pmosOne)
			lastColPort = pla.rowList[row][rowPos].lastitem.nodeInst.getProto().getPort(0); else
				lastColPort =  pla.rowList[row][rowPos].lastitem.nodeInst.getProto().findPortProto("GATE.p.e");
		nodeColPort = ni.getProto().findPortProto("GATE.p.w");

		pla.makeWire(pla.pArc, 0, pla.rowList[row][rowPos].lastitem.nodeInst, lastColPort, ni, nodeColPort, arrayCell);

		// put in column list
		pla.columnList[i].lastitem.bottomItem = new PLA.UCItem();
		pla.columnList[i].lastitem = pla.columnList[i].lastitem.bottomItem;
		pla.columnList[i].lastitem.nodeInst = ni;

		// put in row list
		pla.rowList[row][rowPos].lastitem.rightItem = pla.columnList[i].lastitem;
		pla.rowList[row][rowPos].lastitem = pla.rowList[row][rowPos].lastitem.rightItem;
		return false;
	}

	private boolean pmosCompleteRow(int row, int x, int y, Cell arrayCell)
	{
		PLA.UCItem newItem = new PLA.UCItem();
		newItem.nodeInst = pla.makePin(arrayCell, x, y+2, 6, pla.mpCon);
		if (newItem.nodeInst == null) return true;

		// connect to last GATE object
		PortProto lastColPort;
		if (pla.rowList[row][0].lastitem.nodeInst.getProto() != pla.pmosOne)
			lastColPort = pla.rowList[row][0].lastitem.nodeInst.getProto().getPort(0); else
				lastColPort = pla.rowList[row][0].lastitem.nodeInst.getProto().findPortProto("GATE.p.e");
		PortProto nodeColPort = newItem.nodeInst.getProto().getPort(0);
		pla.makeWire(pla.pArc, 0, pla.rowList[row][0].lastitem.nodeInst, lastColPort, newItem.nodeInst, nodeColPort, arrayCell);
		pla.rowList[row][0].lastitem = newItem;

		// Now export port at the beginning and end of this half of row grouping
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
		newItem.nodeInst = pla.makePin(arrayCell, x, y-8, 6, pla.mpCon);
		if (newItem.nodeInst == null) return true;

		// connect to last GATE object
		if (pla.rowList[row][2].lastitem.nodeInst.getProto() != pla.pmosOne)
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
		Export.newInstance(arrayCell, pi, "ACCESS" + (row * 2 + 1) + ".p1.w");

		pp = pla.rowList[row][2].lastitem.nodeInst.getProto().getPort(0);
		pi = pla.rowList[row][2].lastitem.nodeInst.findPortInstFromProto(pp);
		Export.newInstance(arrayCell, pi, "ACCESS" + (row * 2 + 1) + ".p.e");
		return false;
	}

	private boolean pmosFinishColumns(int width, int x, int y, int xOffset, Cell arrayCell)
	{
		int pwrCnt = 0;
		for (int i = 0; i < width; i++)
		{
			PLA.UCItem newItem = new PLA.UCItem();

			String name;
			PortProto lastColPort;
			if ((i % 5) == 0)	// put in a power pin every 5th position
			{
				name = "PWR" + pwrCnt + ".m-1.s";
				newItem.nodeInst = pla.makePin(arrayCell, xOffset * i + x, y, 14, pla.msBut);
				if (newItem.nodeInst == null) return true;
				lastColPort = pla.columnList[i].lastitem.nodeInst.getProto().getPort(0);
				pwrCnt++;
			} else
			{
				// must be a data pin, don't count power pins
				name = "DATA" + (i - pwrCnt) + ".m-1.s";
				newItem.nodeInst = pla.makePin(arrayCell, xOffset * i + x, y, 4, pla.m1Pin);
				if (newItem.nodeInst == null) return true;
				if (pla.columnList[i].lastitem.nodeInst.getProto() != pla.pmosOne)
					lastColPort = pla.columnList[i].lastitem.nodeInst.getProto().getPort(0); else
						lastColPort = pla.columnList[i].lastitem.nodeInst.getProto().findPortProto("OUT.m-1.s");
			}

			// wire in the first power strap
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
