/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Decode.java
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

import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;

import java.awt.geom.Rectangle2D;

/**
 * Class to generate the decoder part of MOSIS CMOS PLAs.
 */
public class Decode
{
	private PLA pla;

	Decode(PLA pla)
	{
		this.pla = pla;
	}

	Cell decodeGen(Library library, Cell pmosCell, Cell nmosCell, String cellName, boolean inputsOnTop)
	{
		Cell cell = Cell.makeInstance(library, cellName);
		NodeInst pNode = pla.makeInstance(cell, pmosCell, 0, 0, false);
		if (pNode == null) return null;
		pNode.rotate(Orientation.RRR);  // rotate by 90 degrees

		Rectangle2D nodeRect = pNode.getBounds();
		double x = nodeRect.getWidth();
		if (x < 0) x = -x;
		x += 12; // SEPARATION

		NodeInst nNode = pla.makeInstance(cell, nmosCell, x, 0, false);
		if (nNode == null) return null;
		nNode.rotate(Orientation.RRR);  // rotate by 90 degrees

		NodeInst [] both = decodeBufs(pmosCell, pNode, nmosCell, nNode, cell, x, inputsOnTop);
		if (both == null) return null;
		if (decodeRoute(pmosCell, pNode, nmosCell, nNode, cell, both[0], both[1], inputsOnTop)) return null;
		decExp(nmosCell, nNode, cell);

		return cell;
	}

	private NodeInst [] decodeBufs(Cell pmosCell, NodeInst pNode, Cell nmosCell,
		NodeInst nNode, Cell decodeCell, double X, boolean inputsOnTop)
	{
		NodeInst lastNode = null;
		NodeInst newNode = null;
		NodeInst firstPNode = null, lastPNode = null, lastNNode = null;
		int rows = 2;
		Variable var = pmosCell.getVar("PLA_access_rows");
		if (var != null) rows = ((Integer)var.getObject()).intValue(); else
			System.out.println("ACCESS_rows defaulting to 2");

		Export pwrWPort = null, pwrEPort = null, gndWPort = null, gndEPort = null;
		char side = 'w';
		if (inputsOnTop) side = 'e';
		Rectangle2D nodeBounds = pNode.getBounds();
		double lowX = nodeBounds.getMinX();
		double lowY = nodeBounds.getMinY();
		double highY = nodeBounds.getMaxY();

		Rectangle2D bufBounds = pla.decoderInv.getBounds();
		double diff = bufBounds.getHeight();
		if (diff < 0) diff = -diff;
		int limit = rows / 4;
		if (limit == 0 || (rows % 4) != 0) limit++;
		int cnt = 0;
		double y = highY;
		if (inputsOnTop) y = lowY - diff;
		y -= 13;
		double xOffset = lowX - 38;
		if ((rows %4) == 0) xOffset = lowX - 18;
		pwrWPort = pla.decoderInv.findExport("PWR.m-2.w");
		pwrEPort = pla.decoderInv.findExport("PWR.m-2.e");
		gndWPort = pla.decoderInv.findExport("GND.m-2.w");
		gndEPort = pla.decoderInv.findExport("GND.m-2.e");
		for (int i = limit; i > 0; i--)
		{
			newNode = pla.makeInstance(decodeCell, pla.decoderInv, i*50 + xOffset, y, !inputsOnTop);
			if (newNode == null) return(null);
			NodeInst newNode2 = pla.makeInstance(decodeCell, pla.decoderInv, X + i*50 + xOffset, y, !inputsOnTop);
			if (newNode2 == null) return(null);
			if (lastPNode == null && lastNNode == null)
			{
				Export.newInstance(decodeCell, newNode2.findPortInstFromProto(pwrEPort), "PWR.m-2.e");
				Export.newInstance(decodeCell, newNode2.findPortInstFromProto(gndEPort), "GND.m-2.e");
				firstPNode = lastPNode = newNode;
				lastNNode = lastNode = newNode2;
			} else
			{
				// get wired
				if (pwrWPort != null && pwrEPort != null)
				{
					pla.makeWire(pla.m2Arc, 14, lastPNode, pwrWPort, newNode, pwrEPort, decodeCell);
					pla.makeWire(pla.m2Arc, 14, lastNNode, pwrWPort, newNode2, pwrEPort, decodeCell);
				}
				if (gndWPort != null && gndEPort != null)
				{
					pla.makeWire(pla.m2Arc, 14, lastNNode, gndWPort, newNode, gndEPort, decodeCell);
					pla.makeWire(pla.m2Arc, 14, lastNNode, gndWPort, newNode2, gndEPort, decodeCell);
				}
				lastPNode = newNode;
				lastNNode = newNode2;
			}
			for (int x = cnt; x < cnt+4; x++)
			{
				String aName = "ACCESS" + x + ".m-1." + side;
				Export pPort = pmosCell.findExport(aName);
				Export nPort = pla.decoderInv.findExport("LINE" + (x % 4) + ".p.n");
				if (pPort != null && nPort != null)
					pla.makeWire(pla.pArc, 2, pNode, pPort, newNode, nPort, decodeCell);
				pPort = nmosCell.findExport(aName);
				if (nPort != null && pPort != null)
					pla.makeWire(pla.pArc, 2, nNode, pPort, newNode2, nPort, decodeCell);
			}
			cnt += 4;
		}
		if (pwrWPort != null && pwrEPort != null)
		{
			pla.makeWire(pla.m2Arc, 14, lastNNode, pwrWPort, firstPNode, pwrEPort, decodeCell);
			Export.newInstance(decodeCell, lastPNode.findPortInstFromProto(pwrWPort), "PWR.m-2.w");
		}
		if (gndWPort != null && gndEPort != null)
		{
			pla.makeWire(pla.m2Arc, 14, lastNNode, gndWPort, firstPNode, gndEPort, decodeCell);
			Export.newInstance(decodeCell, lastPNode.findPortInstFromProto(gndWPort), "GND.m-2.w");
		}
		NodeInst [] both = new NodeInst[2];
		both[0] = lastPNode;
		both[1] = lastNode;
		return both;
	}

	private void decExp(Cell nmosCell, NodeInst nNode, Cell decodeCell)
	{
		int x = 0;
		char side = 'n';
		String name = "DATA" + x + ".m-1." + side;
		Export pp = nmosCell.findExport(name);
		while (pp != null)
		{
			Export.newInstance(decodeCell, nNode.findPortInstFromProto(pp), name);
			x++;
			name = "DATA" + x + ".m-1." + side;
			pp = nmosCell.findExport(name);
		}
	}

	private boolean decodeRoute(Cell pmosCell, NodeInst pNode, Cell nmosCell,
		NodeInst nNode, Cell decodeCell, NodeInst firstNode, NodeInst lastNode, boolean inputsOnTop)
	{
		// Make connection between N and P planes
		Export pPort = pmosCell.findExport("DATA0.m-1.n");
		Export nPort = nmosCell.findExport("DATA0.m-1.s");
		int cnt = 0;
		while (pPort != null && nPort != null)
		{
			// Connect them up here
			pla.makeWire(pla.m1Arc, 4, pNode, pPort, nNode, nPort, decodeCell);

			// now find the next ports
			cnt += 1;
			pPort = pmosCell.findExport("DATA" + cnt + ".m-1.n");
			nPort = nmosCell.findExport("DATA" + cnt + ".m-1.s");
		}

		// Make connection between PWR and P-plane
		pPort = pmosCell.findExport("DATA0.m-1.s");
		nPort = pmosCell.findExport("PWR0.m-1.s");
		if (pPort != null && nPort != null)
			pla.makeWire(pla.m1Arc, 4, pNode, pPort, pNode, nPort, decodeCell);
		Poly poly = pNode.findPortInstFromProto(nPort).getPoly();
		double pX = poly.getCenterX();
		double pY = poly.getCenterY();
		NodeInst pmNode1 = pla.makePin(decodeCell, pX-13, pY, 6, pla.m12Con);
		if (pmNode1 == null) return true;
		pla.makeWire(pla.m1Arc, 4, pNode, nPort, pmNode1, pmNode1.getProto().getPort(0), decodeCell);
		if (!inputsOnTop) // Buffers are at the top of the cell
		{
			PortProto pwrPort = firstNode.getProto().findPortProto("PWR.m-2.w");
			NodeInst pmNode2 = pla.makePin(decodeCell, pX-13, pY+24, 14, pla.m2Pin);
			if (pmNode2 == null) return true;
			pla.makeWire(pla.m2Arc, 14, pmNode1, pmNode1.getProto().getPort(0), pmNode2, pmNode2.getProto().getPort(0), decodeCell);
			pla.makeWire(pla.m2Arc, 14, firstNode, pwrPort, pmNode2, pmNode2.getProto().getPort(0), decodeCell);
		}
		nPort = pPort;
		pPort = pmosCell.findExport("DATA1.m-1.s");
		cnt = 1;
		int pwrCnt = 0;
		while (pPort != null && nPort != null)
		{
			// Connect them up here
			pla.makeWire(pla.m1Arc, 4, pNode, pPort, pNode, nPort, decodeCell);

			// now find the next ports
			cnt += 1;
			nPort = pPort;
			if ((cnt % 4) == 0)
			{
				pwrCnt += 1;
				String name = "PWR" + pwrCnt + ".m-1.s";
				pPort = pmosCell.findExport(name);
				if (pPort != null)
					pla.makeWire(pla.m1Arc, 4, pNode, pPort, pNode, nPort, decodeCell);
				poly = pNode.findPortInstFromProto(pPort).getPoly();
				pX = poly.getCenterX();
				pY = poly.getCenterY();
				NodeInst pmNode2 = pla.makePin(decodeCell, pX-13, pY, 6, pla.m12Con);
				if (pmNode2 == null) return true;
				pla.makeWire(pla.m1Arc, 4, pNode, pPort, pmNode2, pmNode2.getProto().getPort(0), decodeCell);
				pla.makeWire(pla.m2Arc, 14, pmNode1, pmNode1.getProto().getPort(0), pmNode2, pmNode2.getProto().getPort(0), decodeCell);
				pmNode1 = pmNode2;
				nPort = pPort;
			}
			pPort = pmosCell.findExport("DATA" + cnt + ".m-1.s");
		}
		if (inputsOnTop) // Buffers are at the top of the cell
		{
			PortProto pwrPort = firstNode.getProto().findPortProto("PWR.m-2.w");
			NodeInst pmNode2 = pla.makePin(decodeCell, pX-13, pY-24, 14, pla.m2Pin);
			if (pmNode2 == null) return true;
			pla.makeWire(pla.m2Arc, 14, pmNode1, pmNode1.getProto().getPort(0), pmNode2, pmNode2.getProto().getPort(0), decodeCell);
			pla.makeWire(pla.m2Arc, 14, firstNode, pwrPort, pmNode2, pmNode2.getProto().getPort(0), decodeCell);
		}

		// Make connection between GND and N-plane
		nPort = nmosCell.findExport("GND0.m-1.n");
		poly = nNode.findPortInstFromProto(nPort).getPoly();
		pX = poly.getCenterX();
		pY = poly.getCenterY();
		pmNode1 = pla.makePin(decodeCell, pX+13, pY, 6, pla.m12Con);
		if (pmNode1 == null) return true;
		pla.makeWire(pla.m1Arc, 4, nNode, nPort, pmNode1, pmNode1.getProto().getPort(0), decodeCell);
		if (!inputsOnTop) // Buffers are at the top of the cell
		{
			PortProto pwrPort = lastNode.getProto().findPortProto("GND.m-2.e");
			NodeInst pmNode2 = pla.makePin(decodeCell, pX+13, pY+90, 14, pla.m2Pin);
			if (pmNode2 == null) return true;
			pla.makeWire(pla.m2Arc, 14, pmNode1, pmNode1.getProto().getPort(0), pmNode2, pmNode2.getProto().getPort(0), decodeCell);
			pla.makeWire(pla.m2Arc, 14, lastNode, pwrPort, pmNode2, pmNode2.getProto().getPort(0), decodeCell);
		}
		cnt = 1;
		nPort = nmosCell.findExport("GND" + cnt + ".m-1.n");
		while (nPort != null)
		{
			// Connect them up here
			poly = nNode.findPortInstFromProto(nPort).getPoly();
			pX = poly.getCenterX();
			pY = poly.getCenterY();
			NodeInst pmNode2 = pla.makePin(decodeCell, pX+13, pY, 6, pla.m12Con);
			if (pmNode2 == null) return true;
			pla.makeWire(pla.m1Arc, 4, nNode, nPort, pmNode2, pmNode2.getProto().getPort(0), decodeCell);
			pla.makeWire(pla.m2Arc, 14, pmNode1, pmNode1.getProto().getPort(0), pmNode2, pmNode2.getProto().getPort(0), decodeCell);
			pmNode1 = pmNode2;
			cnt++;
			nPort = nmosCell.findExport("GND" + cnt + ".m-1.n");
		}
		if (inputsOnTop) // Buffers are at the bottom of the cell
		{
			PortProto pwrPort = lastNode.getProto().findPortProto("GND.m-2.e");
			NodeInst pmNode2 = pla.makePin(decodeCell, pX+13, pY-90, 14, pla.m2Pin);
			if (pmNode2 == null) return true;
			pla.makeWire(pla.m2Arc, 14, pmNode1, pmNode1.getProto().getPort(0), pmNode2, pmNode2.getProto().getPort(0), decodeCell);
			pla.makeWire(pla.m2Arc, 14, lastNode, pwrPort, pmNode2, pmNode2.getProto().getPort(0), decodeCell);
		}

		char side = 's';
		char acSide = 'e';
		if (inputsOnTop)
		{
			side = 'n';
			acSide = 'w';
		}
		int columns = 2;
		Variable var = pmosCell.getVar("PLA_access_rows");
		if (var != null) columns = ((Integer)var.getObject()).intValue() / 2; else
			System.out.println("DATA_cols defaulting to 2");

		int initOffset = 8;
		int sep = 7;
		cnt = 0;
		String name = "ACCESS" + cnt + ".m-1." + acSide;
		pPort = pmosCell.findExport(name);
		nPort = nmosCell.findExport(name);
		while (pPort != null && nPort != null)
		{
			// wire it
			int inCnt = cnt/2;
			poly = pNode.findPortInstFromProto(pPort).getPoly();
			pX = poly.getCenterX();

			poly = nNode.findPortInstFromProto(nPort).getPoly();
			double nX = poly.getCenterX();
			double nY = poly.getCenterY();

			double y, oY;
			if (inputsOnTop)
			{
				y = nY + initOffset + inCnt*sep;
				oY = y + (columns - inCnt - 1)*sep;
			} else
			{
				y = nY - initOffset - inCnt*sep;
				oY = y - (columns - inCnt - 1)*sep;
			}
			pmNode1 = pla.makePin(decodeCell, pX, y, 6, pla.mpCon);
			if (pmNode1 == null) return true;
			NodeInst pmNode2 = pla.makePin(decodeCell, nX, y, 6, pla.mpCon);
			if (pmNode2 == null) return true;
			pla.makeWire(pla.m1Arc, 4, pmNode1, pmNode1.getProto().getPort(0), pmNode2, pmNode2.getProto().getPort(0), decodeCell);
			pla.makeWire(pla.pArc, 2, pNode, pPort, pmNode1, pmNode1.getProto().getPort(0), decodeCell);
			pla.makeWire(pla.pArc, 2, nNode, nPort, pmNode2, pmNode2.getProto().getPort(0), decodeCell);

			pmNode1 = pla.makePin(decodeCell, nX, oY, 4, pla.m1Pin);
			if (pmNode1 == null) return true;
			pla.makeWire(pla.m1Arc, 4, pmNode1,
				pmNode1.getProto().getPort(0), pmNode2, pmNode2.getProto().getPort(0), decodeCell);
			Export.newInstance(decodeCell, pmNode1.findPortInstFromProto(pmNode1.getProto().getPort(0)), "INPUT" + inCnt + ".m-1." + side);
			cnt += 2;  // Only route every second one
			name = "ACCESS" + cnt + ".m-1." + acSide;
			pPort = pmosCell.findExport(name);
			nPort = nmosCell.findExport(name);
		}
		return false;
	}
}
