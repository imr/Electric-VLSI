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
import com.sun.electric.database.hierarchy.NodeUsage;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.ErrorLogger;

import java.util.Collection;
import java.util.Iterator;

/**
 * This is the Network tool.
 */
public class Network extends Listener
{

	// ---------------------- private and protected methods -----------------

	/** the Network tool. */						public static final Network tool = new Network();
	/** flag for debug print. */					static boolean debug = false;

	/** current valuse of shortResistors flag. */	static boolean shortResistors;

	/** NetCells. */								private static NetCell[] cells;

    /** The logger for logging Network errors */    protected static ErrorLogger errorLogger = ErrorLogger.newInstance("Network Errors", true);
    /** sort keys for sorting network errors */     protected static final int errorSortNetworks = 0;
                                                    protected static final int errorSortNodes = 1;
                                                    protected static final int errorSortPorts = 2;

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
		int maxCell = 1;
		for (Iterator lit = Library.getLibraries(); lit.hasNext(); )
		{
			Library lib = (Library)lit.next();
			for (Iterator cit = lib.getCells(); cit.hasNext(); )
			{
				Cell c = (Cell)cit.next();
				while (c.getCellIndex() >= maxCell) maxCell *= 2;
			}
		}
		cells = new NetCell[maxCell];
		for (Iterator lit = Library.getLibraries(); lit.hasNext(); )
		{
			Library lib = (Library)lit.next();
			for (Iterator cit = lib.getCells(); cit.hasNext(); )
			{
				Cell c = (Cell)cit.next();
				if (getNetCell(c) != null) continue;
				if (c.isIcon() || c.isSchematicView())
					new NetSchem(c);
				else
					new NetCell(c);
			}
		}
	}

	static void exportsChanged(Cell cell) {
		NetCell netCell = Network.getNetCell(cell);
		netCell.setInvalid(true);
		for (Iterator it = cell.getUsagesOf(); it.hasNext();) {
			NodeUsage nu = (NodeUsage)it.next();
			if (nu.isIconOfParent()) continue;
			netCell = Network.getNetCell(nu.getParent());
			netCell.setInvalid(true);
		}
	}

	static void setCell(Cell cell, NetCell netCell) {
		int cellIndex = cell.getCellIndex();
		if (cellIndex >= cells.length)
		{
			int newLength = cells.length;
			while (cellIndex >= newLength) newLength *= 2;
			NetCell[] newCells = new NetCell[newLength];
			for (int i = 0; i < cells.length; i++)
				newCells[i] = cells[i];
			cells = newCells;
		}
		cells[cellIndex] = netCell;
	}

	static final NetCell getNetCell(Cell cell) { return cells[cell.getCellIndex()]; }

	/****************************** PUBLIC METHODS ******************************/

	public static synchronized Netlist getUserNetlist(Cell cell) {
        //synchronized(cells) {
            return getNetCell(cell).getUserNetlist();
		//}
    }

	/** Recompute the Netlist structure for given Cell.
	 * @param cell cell to recompute Netlist structure.
     * <p>Because shorting resistors is a fairly common request, it is 
     * implemented in the method if @param shortResistors is set to true.
	 * @return the Netlist structure for Cell.
     */
	public static synchronized Netlist getNetlist(Cell cell, boolean shortResistors) {
        //synchronized(cells) {
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
		    return getNetCell(cell).getUserNetlist();
		//}
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
		Cell cell = obj.whichCell();
		if (obj instanceof Cell)
		{
			if (cell.isIcon() || cell.isSchematicView())
				new NetSchem(cell);
			else
				new NetCell(cell);
		} else if (obj instanceof Export) {
			exportsChanged(cell);
		} else {
			if (cell != null) getNetCell(cell).setNetworksDirty();
		}
		if (!debug) return;
		System.out.println("Network.newObject("+obj+")");
	}

	public void killObject(ElectricObject obj)
	{
		Cell cell = obj.whichCell();
		if (obj instanceof Cell)
		{
			setCell(cell, null);
			if (cell.isIcon() || cell.isSchematicView())
				NetSchem.updateCellGroup(cell.getCellGroup());
		} else {
			if (cell != null) getNetCell(cell).setNetworksDirty();
		}
		if (!debug) return;
		System.out.println("Network.killObject("+obj+")");
	}

	public void killExport(Export pp, Collection oldPortInsts)
	{
		Cell cell = (Cell)pp.getParent();
		exportsChanged(cell);
		if (!debug) return;
		System.out.println("Network.killExport("+pp+","+oldPortInsts.size()+")");
	}

	public void renameObject(ElectricObject obj, Name oldName)
	{
		Cell cell = obj.whichCell();
		if (obj instanceof Geometric)
		{
			if (cell != null) getNetCell(cell).setNetworksDirty();
		}
		if (!debug) return;
		System.out.println("Network.reanameObject("+obj+","+oldName+")");
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

	public void writeLibrary(Library lib)
	{
		if (!debug) return;
		System.out.println("Network.writeLibrary("+lib+")");
	}

	/****************************** OPTIONS ******************************/

	private static Pref cacheUnifyPowerAndGround = Pref.makeBooleanPref("UnifyPowerAndGround", Network.tool.prefs, false);
    static { cacheUnifyPowerAndGround.attachToObject(Network.tool, "Tool Options, Network tab", "Networks unify Power and Ground"); }
	/**
	 * Method to tell whether all Power nets are unified and all Ground nets are unified.
	 * @return true if all Power nets are unified and all Ground nets are unified.
	 */
	public static boolean isUnifyPowerAndGround() { return cacheUnifyPowerAndGround.getBoolean(); }
	/**
	 * Method to set whether all Power nets are unified and all Ground nets are unified.
	 * @param u true if all Power nets are unified and all Ground nets are to be unified.
	 */
	public static void setUnifyPowerAndGround(boolean u) { cacheUnifyPowerAndGround.setBoolean(u); }


	private static Pref cacheUnifyLikeNamedNets = Pref.makeBooleanPref("UnifyLikeNamedNets", Network.tool.prefs, false);
    static { cacheUnifyLikeNamedNets.attachToObject(Network.tool, "Tool Options, Network tab", "Networks unify all like-named nets"); }
	/**
	 * Method to tell whether all like-named nets are unified.
	 * Typically, like-named nets (two networks with the same name) are unified only in a schematic.
	 * With this option, the unification happens in layout cells as well (not recommended).
	 * @return true if all like-named nets are unified.
	 */
	public static boolean isUnifyLikeNamedNets() { return cacheUnifyLikeNamedNets.getBoolean(); }
	/**
	 * Method to set whether all like-named nets are unified.
	 * Typically, like-named nets (two networks with the same name) are unified only in a schematic.
	 * With this option, the unification happens in layout cells as well (not recommended).
	 * @param u true if all like-named nets are unified.
	 */
	public static void setUnifyLikeNamedNets(boolean u) { cacheUnifyLikeNamedNets.setBoolean(u); }

	private static Pref cacheIgnoreResistors = Pref.makeBooleanPref("IgnoreResistors", Network.tool.prefs, false);
    static { cacheIgnoreResistors.attachToObject(Network.tool, "Tool Options, Network tab", "Networks ignore Resistors"); }
	/**
	 * Method to tell whether resistors are ignored in the circuit.
	 * When ignored, they appear as a "short", connecting the two sides.
	 * When included, they appear as a component with different networks on either side.
	 * @return true if resistors are ignored in the circuit.
	 */
	public static boolean isIgnoreResistors() { return cacheIgnoreResistors.getBoolean(); }
	/**
	 * Method to set whether resistors are ignored in the circuit.
	 * When ignored, they appear as a "short", connecting the two sides.
	 * When included, they appear as a component with different networks on either side.
	 * @param i true if resistors are ignored in the circuit.
	 */
	public static void setIgnoreResistors(boolean i) { cacheIgnoreResistors.setBoolean(i); }

	private static Pref cacheUnificationPrefix = Pref.makeStringPref("UnificationPrefix", Network.tool.prefs, "");
    static { cacheUnificationPrefix.attachToObject(Network.tool, "Tool Options, Network tab", "Network unification prefix"); }
	/**
	 * Method to return the list of unification prefixes.
	 * Unification prefixes are strings which, when two nets both start with them, cause the networks to be unified.
	 * For example, the prefix "vdd" would cause networks "vdd_1" and "vdd_dirty" to be unified.
	 * @return the list of unification prefixes.
	 */
	public static String getUnificationPrefix() { return cacheUnificationPrefix.getString(); }
	/**
	 * Method to set the list of unification prefixes.
	 * Unification prefixes are strings which, when two nets both start with them, cause the networks to be unified.
	 * For example, the prefix "vdd" would cause networks "vdd_1" and "vdd_dirty" to be unified.
	 * @param p the list of unification prefixes.
	 */
	public static void setUnificationPrefix(String p) { cacheUnificationPrefix.setString(p); }

	private static Pref cacheBusBaseZero = Pref.makeBooleanPref("BusBaseZero", Network.tool.prefs, false);
    static { cacheBusBaseZero.attachToObject(Network.tool, "Tool Options, Network tab", "Default busses starting index"); }
	/**
	 * Method to tell whether unnamed busses should be zero-based.
	 * The alternative is 1-based.
	 * @return true if unnamed busses should be zero-based.
	 */
	public static boolean isBusBaseZero() { return cacheBusBaseZero.getBoolean(); }
	/**
	 * Method to set whether unnamed busses should be zero-based.
	 * The alternative is 1-based.
	 * @param z true if unnamed busses should be zero-based.
	 */
	public static void setBusBaseZero(boolean z) { cacheBusBaseZero.setBoolean(z); }

	private static Pref cacheBusAscending = Pref.makeBooleanPref("BusAscending", Network.tool.prefs, false);
    static { cacheBusAscending.attachToObject(Network.tool, "Tool Options, Network tab", "Default busses are ascending"); }
	/**
	 * Method to tell whether unnamed busses should be numbered ascending.
	 * The alternative is descending.
	 * @return true if unnamed busses should be numbered ascending.
	 */
	public static boolean isBusAscending() { return cacheBusBaseZero.getBoolean(); }
	/**
	 * Method to set whether unnamed busses should be numbered ascending.
	 * The alternative is descending.
	 * @param a true if unnamed busses should be numbered ascending.
	 */
	public static void setBusAscending(boolean a) { cacheBusBaseZero.setBoolean(a); }
}
