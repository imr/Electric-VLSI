/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NodeInfo.java
 * Technology Editor, node information
 * Written by Steven M. Rubin, Sun Microsystems.
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
package com.sun.electric.tool.user.tecEdit;

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;

import java.util.Iterator;
import java.util.List;

/**
 * This class defines information about nodes in the Technology Editor.
 */
public class NodeInfo extends Info
{

	static class PortDetails
	{
		String                 name;
		ArcInfo []             connections;
		int                    angle;
		int                    range;
		int                    netIndex;
		Technology.TechPoint[] values;
	}

	static class LayerDetails
	{
		LayerInfo              layer;
		Poly.Type              style;
		int                    representation;
		Technology.TechPoint[] values;
		Sample                 ns;
		int                    portIndex;
		boolean                multiCut;				/* true if a multi-cut layer */
		double                 multiXS, multiYS;			/* size of multicut */
		double                 multiIndent, multiSep;	/* indent and separation of multicuts */
		double                 lWidth, rWidth, extendT, extendB;		/* serpentine transistor information */
	}

	String                 name;
	String                 abbrev;
	PrimitiveNode          generated;
	PrimitiveNode.Function func;
	boolean                serp;
	boolean                square;
	boolean                wipes;
	boolean                lockable;
	LayerDetails []        nodeLayers;
	PortDetails []         nodePortDetails;
	PrimitivePort[]        primPorts;
	SizeOffset             so;
	int                    specialType;
	double []              specialValues;
	double                 xSize, ySize;

	static SpecialTextDescr [] nodeTextTable =
	{
		new SpecialTextDescr(0, 15, NODEFUNCTION),
		new SpecialTextDescr(0, 12, NODESERPENTINE),
		new SpecialTextDescr(0,  9, NODESQUARE),
		new SpecialTextDescr(0,  6, NODEWIPES),
		new SpecialTextDescr(0,  3, NODELOCKABLE)
	};

	NodeInfo()
	{
		func = PrimitiveNode.Function.UNKNOWN;
	}

	/**
	 * Method to return an array of cells that comprise the nodes in a technology library.
	 * @param lib the technology library.
	 * @return an array of cells for each node (in the proper order).
	 */
	public static Cell [] getNodeCells(Library lib)
	{
		Library [] oneLib = new Library[1];
		oneLib[0] = lib;
		return findCellSequence(oneLib, "node-", NODESEQUENCE_KEY);
	}

	/**
	 * Method to build the appropriate descriptive information for a node into
	 * cell "np".  The function is in "func", the serpentine transistor factor
	 * is in "serp", the node is square if "square" is true, the node
	 * is invisible on 1 or 2 arcs if "wipes" is true, and the node is lockable
	 * if "lockable" is true.
	 */
	void generate(Cell np)
	{
		// load up the structure with the current values
		loadTableEntry(nodeTextTable, NODEFUNCTION, func);
		loadTableEntry(nodeTextTable, NODESERPENTINE, new Boolean(serp));
		loadTableEntry(nodeTextTable, NODESQUARE, new Boolean(square));
		loadTableEntry(nodeTextTable, NODEWIPES, new Boolean(wipes));
		loadTableEntry(nodeTextTable, NODELOCKABLE, new Boolean(lockable));

		// now create those text objects
		createSpecialText(np, nodeTextTable);
	}

	/**
	 * Method to parse the node cell in "np" and return an NodeInfo object that describes it.
	 */
	static NodeInfo parseCell(Cell np)
	{
		NodeInfo nIn = new NodeInfo();

		// look at all nodes in the arc description cell
		for(Iterator<NodeInst> it = np.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			Variable var = ni.getVar(OPTION_KEY);
			if (var == null) continue;
			String str = getValueOnNode(ni);

			switch (((Integer)var.getObject()).intValue())
			{
				case NODEFUNCTION:
					nIn.func = PrimitiveNode.Function.UNKNOWN;
					List<PrimitiveNode.Function> allFuncs = PrimitiveNode.Function.getFunctions();
					for(PrimitiveNode.Function fun : allFuncs)
					{
						if (fun.toString().equalsIgnoreCase(str))
						{
							nIn.func = fun;
							break;
						}
					}
					break;
				case NODESQUARE:
					nIn.square = str.equalsIgnoreCase("yes");
					break;
				case NODEWIPES:
					nIn.wipes = str.equalsIgnoreCase("yes");
					break;
				case NODELOCKABLE:
					nIn.lockable = str.equalsIgnoreCase("yes");
					break;
				case NODESERPENTINE:
					nIn.serp = str.equalsIgnoreCase("yes");
					break;
			}
		}
		return nIn;
	}

	/**
	 * Method to compact a Node technology-edit cell
	 */
	static void compactCell(Cell cell)
	{
		// move the examples
		Example neList = Example.getExamples(cell, true);
		if (neList == null) return;
		int numExamples = 0;
		Example smallest = neList;
		Example biggest = neList;
		for(Example ne = neList; ne != null; ne = ne.nextExample)
		{
			numExamples++;
			if (ne.hx-ne.lx > biggest.hx-biggest.lx) biggest = ne;
		}
		if (numExamples == 1)
		{
			moveExample(neList, -(neList.lx + neList.hx) / 2, -neList.hy);
			return;
		}
		if (numExamples != 4) return;

		Example stretchX = null;
		Example stretchY = null;
		for(Example ne = neList; ne != null; ne = ne.nextExample)
		{
			if (ne == biggest || ne == smallest) continue;
			if (stretchX == null) stretchX = ne; else
				if (stretchY == null) stretchY = ne;
		}
		if (stretchX.hx-stretchX.lx < stretchY.hx-stretchY.lx)
		{
			Example swap = stretchX;
			stretchX = stretchY;
			stretchY = swap;
		}

		double separation = Math.min(smallest.hx - smallest.lx, smallest.hy - smallest.ly);
		double totalWid = (stretchX.hx-stretchX.lx) + (smallest.hx-smallest.lx) + separation;
		double totalHei = (stretchY.hy-stretchY.ly) + (smallest.hy-smallest.ly) + separation;

		// center the smallest (main) example
		double cX = -totalWid / 2 - smallest.lx;
		double cY = -smallest.hy - 1;
		moveExample(smallest, cX, cY);

		// center the stretch-x (upper-right) example
		cX = totalWid/2 - stretchX.hx;
		cY = -stretchX.hy - 1;
		moveExample(stretchX, cX, cY);

		// center the stretch-y (lower-left) example
		cX = -totalWid/2 - stretchY.lx;
		cY = -totalHei - stretchY.ly - 1;
		moveExample(stretchY, cX, cY);

		// center the biggest (lower-right) example
		cX = totalWid/2 - biggest.hx;
		cY = -totalHei - biggest.ly - 1;
		moveExample(biggest, cX, cY);
	}

	private static void moveExample(Example ne, double dX, double dY)
	{
		for(Sample ns : ne.samples)
		{
			ns.node.move(dX, dY);
		}
	}
}
