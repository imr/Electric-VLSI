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
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Tool;

/**
 * This is the Network tool.
 */
public class Network extends Tool
{
	// ---------------------- private and protected methods -----------------

	/** the Network tool. */		public static Network tool = new Network();
	/** the Network tool. */		private boolean debug = false;

	/**
	 * The constructor sets up the Network tool.
	 */
	private Network()
	{
		super("network");
		
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
