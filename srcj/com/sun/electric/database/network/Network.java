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
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.NodeUsage;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
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
import java.util.Map;

/**
 * This is the Network tool.
 */
public class Network extends Tool
{

	// ---------------------- private and protected methods -----------------

	/** the Network tool. */						public static Network tool = new Network();
	/** flag for debug print. */					private static boolean debug = false;

	/** array of port equiv map for prim nodes.*/	private static int[][] primEquivPorts;
	/** index of resistor primitive. */				private static int resistorIndex;
	/** port equiv map of resistor primitive. */	private static int[] resistorEquivPorts;
	/** current valuse of shortResistors flag. */	private static boolean shortResistors;

	/** size of cell* arrays */						static NetCell[] cells;

	/**
	 * The constructor sets up the Network tool.
	 */
	private Network()
	{
		super("network");
		reload();
	}

	static public void reload()
	{
		int maxPrim = 0;
		for (Iterator tit = Technology.getTechnologies(); tit.hasNext(); )
		{
			Technology tech = (Technology)tit.next();
			for (Iterator nit = tech.getNodes(); nit.hasNext(); )
			{
				PrimitiveNode pn = (PrimitiveNode)nit.next();
				maxPrim = Math.max(maxPrim, -pn.getIndex());
			}
		}
		primEquivPorts = new int[maxPrim][];

		/* Gather PrimitiveNodes */
		for (Iterator tit = Technology.getTechnologies(); tit.hasNext(); )
		{
			Technology tech = (Technology)tit.next();
			for (Iterator nit = tech.getNodes(); nit.hasNext(); )
			{
				PrimitiveNode pn = (PrimitiveNode)nit.next();
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
				primEquivPorts[~pn.getIndex()] = equiv;
			}
		}
		resistorIndex = Schematics.tech.resistorNode.getIndex();
		resistorEquivPorts = new int[primEquivPorts[~resistorIndex].length]; // allocated zero-filled

		int maxCell = 1;
		for (Iterator lit = Library.getLibraries(); lit.hasNext(); )
		{
			Library lib = (Library)lit.next();
			for (Iterator cit = lib.getCells(); cit.hasNext(); )
			{
				Cell c = (Cell)cit.next();
				while (c.getIndex() >= maxCell) maxCell *= 2;
			}
		}
		cells = new NetCell[maxCell];
		for (Iterator lit = Library.getLibraries(); lit.hasNext(); )
		{
			Library lib = (Library)lit.next();
			for (Iterator cit = lib.getCells(); cit.hasNext(); )
			{
				Cell c = (Cell)cit.next();
				cells[c.getIndex()] = new NetCell(c);
			}
		}
	}

	static int[] getEquivPorts(NodeProto np)
 	{
		int ind = np.getIndex();
		if (ind >= 0) return cells[ind].equivPorts;
		if (shortResistors && ind == resistorIndex) return resistorEquivPorts;
		return primEquivPorts[~ind];
 	}

	/****************************** PUBLIC METHODS ******************************/

	/** Recompute the network structure for this Cell.
	 *
	 * @param connectedPorts this argument allows the user to tell the
	 * network builder to treat certain PortProtos of a NodeProto as a
	 * short circuit. For example, it is sometimes useful to build the
	 * net list as if the PortProtos of a resistor where shorted
	 * together.
	 *
	 * <p> <code>connectedPorts</code> must be either null or an
	 * ArrayList of ArrayLists of PortProtos.  All of the PortProtos in
	 * an ArrayList are treated as if they are connected.  All of the
	 * PortProtos in a single ArrayList must belong to the same
	 * NodeProto.
     *
     * <p>Because shorting resistors is a fairly common request, it is 
     * implemented in the method if @param shortResistors is set to true.
     */
	public static void rebuildNetworks(Cell cell, boolean shortResistors)
	{
		if (Network.shortResistors != shortResistors)
		{
			for (int i = 0; i < cells.length; i++)
			{
				NetCell netCell = cells[i];
				if (netCell != null) netCell.setInvalid(true);
			}
			Network.shortResistors = shortResistors;
			System.out.println("shortResistors="+shortResistors);
		}
		cells[cell.getIndex()].redoNetworks();
	}

	/**
	 * Get an iterator over all of the JNetworks of this Cell.
	 * <p> Warning: before getNetworks() is called, JNetworks must be
	 * build by calling Cell.rebuildNetworks()
	 */
	public static Iterator getNetworks(Cell cell) { return cells[cell.getIndex()].getNetworks(); }

	/*
	 * Get network by index in networks maps.
	 */
	public static JNetwork getNetwork(Nodable ni, PortProto portProto, int busIndex)
	{
		NetCell netCell = cells[ni.getParent().getIndex()];
		netCell.redoNetworks();
		if (ni.getIndex() < 0) return null; // Nonelectric node
		if (portProto.getParent() != ni.getProto())
		{
			System.out.println("Invalid portProto argument in Nodable.getNetwork");
			return null;
		}
		if (busIndex < 0 || busIndex >= portProto.getProtoNameKey().busWidth())
		{
			System.out.println("Nodable.getNetwork: invalid arguments busIndex="+busIndex+" portProto="+portProto);
			return null;
		}
		return netCell.getNetwork(ni.getIndex() + portProto.getIndex() + busIndex);
	}

	/****************************** CHANGE LISTENER ******************************/

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

	public void modifyTextDescript(ElectricObject obj, TextDescriptor descript, int oldDescript0, int oldDescript1)
	{
		if (!debug) return;
		System.out.println("Network.modifyTextDescript("+obj+",...)");
	}

	public void newObject(ElectricObject obj)
	{
		if (obj instanceof Cell)
		{
			Cell cell = (Cell)obj;
			int ind = cell.getIndex();
			if (ind >= cells.length)
			{
				int newLength = cells.length;
				while (ind >= newLength) newLength *= 2;
				NetCell[] newCells = new NetCell[newLength];
				for (int i = 0; i < cells.length; i++) newCells[i] = cells[i];
				cells = newCells;
			}
			cells[cell.getIndex()] = new NetCell(cell);
		} else if (obj instanceof Export)
		{
			Export e = (Export)obj;
			Cell cell = (Cell)e.getParent();
			cells[cell.getIndex()].setNetworksDirty();
			for (Iterator it = cell.getUsagesOf(); it.hasNext();)
			{
				NodeUsage nu = (NodeUsage) it.next();
				cells[nu.getParent().getIndex()].setNetworksDirty();
			}
		} else
		{
			Cell cell = obj.whichCell();
			if (cell != null) cells[cell.getIndex()].setNetworksDirty();
		}
		if (!debug) return;
		System.out.println("Network.newObject("+obj+")");
	}

	public void killObject(ElectricObject obj)
	{
		if (obj instanceof Cell)
		{
			Cell cell = (Cell)obj;
			cells[cell.getIndex()] = null;
		} else if (obj instanceof Export)
		{
			Export e = (Export)obj;
			Cell cell = (Cell)e.getParent();
			cells[cell.getIndex()].setNetworksDirty();
			for (Iterator it = cell.getUsagesOf(); it.hasNext();)
			{
				NodeUsage nu = (NodeUsage) it.next();
				cells[nu.getParent().getIndex()].setNetworksDirty();
			}
		} else
		{
			Cell cell = obj.whichCell();
			if (cell != null) cells[cell.getIndex()].setNetworksDirty();
		}
		if (!debug) return;
		System.out.println("Network.killObject("+obj+")");
	}

	public void newVariable(ElectricObject obj, Variable var)
	{
		if (!debug) return;
		System.out.println("Network.newVariable("+obj+","+var+")");
	}

	public void killVariable(ElectricObject obj, Variable var)
	{
		if (!debug) return;
		System.out.println("Network.killVariable("+obj+","+var+")");
	}

	public void modifyVariableFlags(ElectricObject obj, Variable var, int oldFlags)
	{
		if (!debug) return;
		System.out.println("Network.modifyVariableFlags("+obj+","+var+"."+oldFlags+")");
	}

	public void modifyVariable(ElectricObject obj, Variable var, int index, Object oldValue)
	{
		if (!debug) return;
		System.out.println("Network.modifyVariable("+obj+","+var+","+index+","+oldValue+")");
	}

	public void insertVariable(ElectricObject obj, Variable var, int index)
	{
		if (!debug) return;
		System.out.println("Network.insertVariable("+obj+","+var+","+index+")");
	}

	public void deleteVariable(ElectricObject obj, Variable var, int index, Object oldValue)
	{
		if (!debug) return;
		System.out.println("Network.deleteVariable("+obj+","+var+","+index+","+oldValue+")");
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
