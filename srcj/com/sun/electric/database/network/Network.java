/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Network.java
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
package com.sun.electric.database.network;

import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Tool;

import java.util.Iterator;

/**
 * This is the Network tool.
 */
public class Network extends Tool
{

	// ---------------------- private and protected methods -----------------

	/** the Network tool. */						public static Network tool = new Network();
	/** the Network tool. */						private boolean debug = false;
	/** start of cells info in proto arrays */		private int cellsStart;
	/** size of proto* arrays */					private int protoNum;
	/** [i][j] offset of j-th PortProto of i-th NodeProto
	 * in port maps */	   							private int[][] protoPortBeg;
	/** [i][j][] port map if j-th NodeProt with i-th set of options
	 **/											private int[][][] protoPortEquiv;

	/**
	 * The constructor sets up the Network tool.
	 */
	private Network()
	{
		super("network");
		protoPortEquiv = new int[2][][];
	}

	private void reload()
	{
		protoPortBeg = null;
		protoPortEquiv = null;
		cellsStart = 0;
		for (Iterator tit = Technology.getTechnologies(); tit.hasNext(); )
		{
			Technology tech = (Technology)tit.next();
			for (Iterator nit = tech.getNodes(); nit.hasNext(); )
			{
				PrimitiveNode pn = (PrimitiveNode)nit.next();
				cellsStart = Math.max(cellsStart, -pn.getIndex());
			}
		}
		int maxCell = 0;
		for (Iterator lit = Library.getLibraries(); lit.hasNext(); )
		{
			Library lib = (Library)lit.next();
			for (Iterator cit = lib.getCells(); cit.hasNext(); )
			{
				Cell c = (Cell)cit.next();
				maxCell = Math.max(maxCell, c.getIndex());
			}
		}
		protoNum = cellsStart + maxCell + 1;
		protoPortBeg = new int[protoNum][];
		protoPortEquiv[0] = new int[protoNum][];
		protoPortEquiv[1] = new int[protoNum][];

		for (Iterator tit = Technology.getTechnologies(); tit.hasNext(); )
		{
			Technology tech = (Technology)tit.next();
			for (Iterator nit = tech.getNodes(); nit.hasNext(); )
			{
				PrimitiveNode pn = (PrimitiveNode)nit.next();
				int ind = cellsStart + pn.getIndex();
				int[] beg = new int[pn.getNumPorts()+1];
				for (int i = 0; i < beg.length; i++) beg[i] = i;
				protoPortBeg[ind] = beg;
				int[] equiv = new int[pn.getNumPorts()];
				int i = 0;
				for (Iterator it = pn.getPorts(); it.hasNext(); i++)
				{
					PrimitivePort pi = (PrimitivePort)it.next();
					int j = 0;
					for (Iterator jt = pn.getPorts(); j < i; j++)
					{
						PrimitivePort pj = (PrimitivePort)jt.next();
						if (pi.getTopology() == pj.getTopology())
							break;
					}
					equiv[i] = j;
				}
				protoPortEquiv[0][ind] = equiv;
				protoPortEquiv[1][ind] = equiv;
			}
		}
		int resInd = cellsStart + Schematics.tech.resistorNode.getIndex();
		int[] resMap = protoPortEquiv[1][resInd];
		resMap = new int[resMap.length];
		for (int i = 0; i < resMap.length; i++) resMap[i] = 0;
		protoPortEquiv[1][resInd] = resMap;
		for (Iterator lit = Library.getLibraries(); lit.hasNext(); )
		{
			Library lib = (Library)lit.next();
			for (Iterator cit = lib.getCells(); cit.hasNext(); )
			{
				Cell c = (Cell)cit.next();
				int ind = cellsStart + c.getIndex();
				int[] beg = new int[c.getNumPorts() + 1];
				for (int i = 0; i < beg.length; i++) beg[i] = i;
				protoPortBeg[ind] = beg;
			}
		}
	}

	public int[] getEquivPorts(int optionSet, NodeProto np)
	{
		return protoPortEquiv[optionSet][cellsStart + np.getIndex()];
	}

	public void init()
	{
		setOn();
		if (!debug) return;
		System.out.println("Network.init()");
	}

	public void request(String cmd)
	{
		if (!debug) return;
		System.out.println("Network.request("+cmd+")");
	}

	public void examineCell(Cell cell)
	{
		if (!debug) return;
		System.out.println("Network.examineCell("+cell+")");
	}

	public void slice()
	{
		if (!debug) return;
		System.out.println("Network.slice()");
	}

	public void startBatch(Tool tool, boolean undoRedo)
	{
		if (!debug) return;
		System.out.println("Network.startBatch("+tool+","+undoRedo+")");
	}

	public void endBatch()
	{
		if (!debug) return;
		System.out.println("Network.endBatch()");
	}

	public void modifyNodeInst(NodeInst ni, double oCX, double oCY, double oSX, double oSY, int oRot)
	{
		if (!debug) return;
		System.out.println("Network.modifyNodeInst("+ni+","+oCX+","+oCY+","+oSX+","+oSY+","+oRot+")");
	}

	public void modifyNodeInsts(NodeInst [] nis, double [] oCX, double [] oCY, double [] oSX, double [] oSY, int [] oRot)
	{
		if (!debug) return;
		System.out.println("Network.modifyNodeInsts("+nis.length+")");
	}

	public void modifyArcInst(ArcInst ai, double oHX, double oHY, double oTX, double oTY, double oWid)
	{
		if (!debug) return;
		System.out.println("Network.modifyArcInst("+ai+","+","+oHX+","+oTX+","+oTY+","+oWid+")");
	}

	public void modifyExport(Export pp, PortInst oldPi)
	{
		if (!debug) return;
		System.out.println("Network.modifyExport("+pp+","+oldPi+")");
	}

	public void modifyCell(Cell cell, double oLX, double oHX, double oLY, double oHY)
	{
		if (!debug) return;
		System.out.println("Network.modifyCell("+cell+","+oLX+","+oHX+","+oLY+","+oHY+")");
	}

	public void modifyTextDescript(ElectricObject obj, int key, Object oldValue)
	{
		if (!debug) return;
		System.out.println("Network.modifyTextDescript("+obj+","+key+","+oldValue+")");
	}

	public void newObject(ElectricObject obj)
	{
		if (!debug) return;
		System.out.println("Network.newObject("+obj+")");
	}

	public void killObject(ElectricObject obj)
	{
		if (!debug) return;
		System.out.println("Network.killObject("+obj+")");
	}

	public void newVariable(ElectricObject obj, Variable.Key key, int type)
	{
		if (!debug) return;
		System.out.println("Network.newVariable("+obj+","+key+","+type+")");
	}

	public void killVariable(ElectricObject obj, Variable.Key key, Object oldValue, TextDescriptor oldDescript)
	{
		if (!debug) return;
		System.out.println("Network.killVariable("+obj+","+key+","+oldValue+","+oldDescript+")");
	}

	public void modifyVariable(ElectricObject obj, Variable.Key key, int type, int index, Object oldValue)
	{
		if (!debug) return;
		System.out.println("Network.modifyVariable("+obj+","+key+","+type+","+index+","+oldValue+")");
	}

	public void insertVariable(ElectricObject obj, Variable.Key key, int type, int index)
	{
		if (!debug) return;
		System.out.println("Network.insertVariable("+obj+","+key+","+type+","+index+")");
	}

	public void deleteVariable(ElectricObject obj, Variable.Key key, int type, int index, Object oldValue)
	{
		if (!debug) return;
		System.out.println("Network.deleteVariable("+obj+","+key+","+type+","+index+","+oldValue+")");
	}

	public void readLibrary(Library lib)
	{
		if (!debug) return;
		System.out.println("Network.readLibrary("+lib+")");
	}

	public void eraseLibrary(Library lib)
	{
		if (!debug) return;
		System.out.println("Network.eraseLibrary("+lib+")");
	}

	public void writeLibrary(Library lib, boolean pass2)
	{
		if (!debug) return;
		System.out.println("Network.writeLibrary("+lib+","+pass2+")");
	}
}
