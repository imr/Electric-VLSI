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
package com.sun.electric.tool.user.tecEdit;

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;

import java.awt.geom.Dimension2D;
import java.util.ArrayList;
import java.util.BitSet;
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
        PortCharacteristic     characterisitic;
        boolean                isolated;
        boolean                negatable;
	}

	static class LayerDetails
	{
		LayerInfo              layer;
		Poly.Type              style;
		int                    representation;
		Technology.TechPoint[] values;
		Sample                 ns;
		int                    portIndex;
        boolean                inLayers;
        boolean                inElectricalLayers;
		boolean                multiCut;				/* true if a multi-cut layer */
		double                 multiXS, multiYS;			/* size of multicut */
		double                 multiIndent, multiSep, multiSep2D;	/* indent and separation of multicuts */
		double                 lWidth, rWidth, extendT, extendB;		/* serpentine transistor information */
        String                 message;
        TextDescriptor         descriptor;
        BitSet                 inNodes;

        public LayerDetails()
        {
        	inLayers = true;
        	inElectricalLayers = true;
        }

        public LayerDetails duplicate()
        {
        	LayerDetails dup = new LayerDetails();
        	dup.layer = layer;
        	dup.style = style;
        	dup.representation = representation;
        	dup.values = new Technology.TechPoint[values.length];
        	for(int i=0; i<values.length; i++) dup.values[i] = values[i];
        	dup.ns = ns;
        	dup.portIndex = portIndex;
        	dup.inLayers = inLayers;
        	dup.inElectricalLayers = inElectricalLayers;
        	dup.multiCut = multiCut;
        	dup.multiXS = multiXS;
        	dup.multiYS = multiYS;
        	dup.multiIndent = multiIndent;
        	dup.multiSep = multiSep;
        	dup.multiSep2D = multiSep2D;
        	dup.lWidth = lWidth;
        	dup.rWidth = rWidth;
        	dup.extendT = extendT;
        	dup.extendB = extendB;
        	dup.message = message;
        	dup.descriptor = descriptor;
        	return dup;
        }
	}

	String                 name;
	String                 abbrev;
	PrimitiveNode          generated;
	PrimitiveNode.Function func;
	boolean                serp;
    boolean                arcsShrink;
	boolean                square;
    boolean                canBeZeroSize;
	boolean                wipes;
	boolean                lockable;
    boolean                edgeSelect;
    boolean                skipSizeInPalette;
    boolean                notUsed;
    boolean                lowVt;
    boolean                highVt;
    boolean                nativeBit;
    boolean                od18;
    boolean                od25;
    boolean                od33;
	LayerDetails []        nodeLayers;
	PortDetails []         nodePortDetails;
	PrimitivePort[]        primPorts;
	SizeOffset             so;
    PrimitiveNode.NodeSizeRule nodeSizeRule;
    Dimension2D            autoGrowth;
	int                    specialType;
	double []              specialValues;
	double                 xSize, ySize;
	String                 spiceTemplate;
    List<String>           primitiveNodeGroupNames;

	static SpecialTextDescr [] nodeTextTable =
	{
		new SpecialTextDescr(0, 18, NODEFUNCTION),
		new SpecialTextDescr(0, 15, NODESERPENTINE),
		new SpecialTextDescr(0, 12, NODESQUARE),
		new SpecialTextDescr(0,  9, NODEWIPES),
		new SpecialTextDescr(0,  6, NODELOCKABLE),
		new SpecialTextDescr(0,  3, NODESPICETEMPLATE)
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
		loadTableEntry(nodeTextTable, NODESERPENTINE, Boolean.valueOf(serp));
		loadTableEntry(nodeTextTable, NODESQUARE, Boolean.valueOf(square));
		loadTableEntry(nodeTextTable, NODEWIPES, Boolean.valueOf(wipes));
		loadTableEntry(nodeTextTable, NODELOCKABLE, Boolean.valueOf(lockable));
		loadTableEntry(nodeTextTable, NODESPICETEMPLATE, spiceTemplate);

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

			switch (((Integer)var.getObject()).intValue())
			{
				case NODEFUNCTION:
					nIn.func = PrimitiveNode.Function.findName(getValueOnNode(ni));
					if (nIn.func == null) nIn.func = PrimitiveNode.Function.UNKNOWN;
					break;
				case NODESQUARE:
					nIn.square = getValueOnNode(ni).equalsIgnoreCase("yes");
					break;
				case NODEWIPES:
					nIn.wipes = getValueOnNode(ni).equalsIgnoreCase("yes");
					break;
				case NODELOCKABLE:
					nIn.lockable = getValueOnNode(ni).equalsIgnoreCase("yes");
					break;
				case NODESERPENTINE:
					nIn.serp = getValueOnNode(ni).equalsIgnoreCase("yes");
					break;
				case NODESPICETEMPLATE:
					nIn.spiceTemplate = getValueOnNode(ni);
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
		TechConversionResult tcr = new TechConversionResult();
		List<Example> neList = Example.getExamples(cell, true, tcr, null);
   		if (tcr.failed()) tcr.showError();
		if (neList == null || neList.size() == 0) return;
		Example firstEx = neList.get(0);
		int numExamples = neList.size();
		Example smallest = firstEx;
		Example biggest = firstEx;
		for(Example ne : neList)
		{
			if (ne.hx-ne.lx > biggest.hx-biggest.lx || ne.hy-ne.ly > biggest.hx-biggest.ly)
                biggest = ne;
		}
		if (numExamples == 1)
		{
			moveExample(firstEx, -(firstEx.lx + firstEx.hx) / 2, -firstEx.hy);
			return;
		}
		if (numExamples != 4) return;

		Example stretchX = null;
		Example stretchY = null;
		for(Example ne : neList)
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
