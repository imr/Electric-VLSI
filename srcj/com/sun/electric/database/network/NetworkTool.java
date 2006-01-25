/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NetworkTool.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
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

import com.sun.electric.database.CellBackup;
import com.sun.electric.database.CellId;
import com.sun.electric.database.ImmutableArcInst;
import com.sun.electric.database.ImmutableElectricObject;
import com.sun.electric.database.ImmutableExport;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.Snapshot;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.ErrorLogger;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import javax.swing.SwingUtilities;

/**
 * This is the Network tool.
 */
public class NetworkTool extends Listener
{
    /**
	 * Signals that a method has been invoked at an illegal or
	 * inappropriate time.  In other words, the Java environment or
	 * Java application is not in an appropriate state for the requested
	 * operation.
	 */
	public static class NetlistNotReady extends RuntimeException {
	    /**
		 * Constructs an IllegalStateException with no detail message.
		 * A detail message is a String that describes this particular exception.
		 */
		public NetlistNotReady() {
			super("User netlist is not ready");
		}

		/**
		 * Constructs an IllegalStateException with the specified detail
		 * message.  A detail message is a String that describes this particular
		 * exception.
		 *
		 * @param s the String that contains a detailed message
		 */
		public NetlistNotReady(String s) {
			super(s);
		}
	}

	/**
	 * Method to renumber the netlists.
	 */
	public static void renumberNetlists()
	{
		new RenumberJob();
	}

	private static class RenumberJob extends Job
    {
		private RenumberJob()
        {
            super("Renumber All Networks", NetworkTool.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            startJob();
        }

        public boolean doIt() throws JobException
        {
			redoNetworkNumbering(true);
            return true;
        }
    }

	// ---------------------- private and protected methods -----------------

	/** the Network tool. */						private static final NetworkTool tool = new NetworkTool();
	/** flag for debug print. */					static boolean debug = false;
	/** flag for information print. */				static boolean showInfo = true;

	/** NetCells. */								private static NetCell[] cells;
	/** All cells have networks up-to-date */ 		private static boolean networksValid = false;
	/** Mutex object */								private static Object mutex = new Object();

    /** The logger for logging Network errors */    public static ErrorLogger errorLogger = ErrorLogger.newInstance("Network Errors", true);
    /** sort keys for sorting network errors */     static final int errorSortNetworks = 0;
                                                    static final int errorSortNodes = 1;
                                                    static final int errorSortPorts = 2;

	/**
	 * The constructor sets up the Network tool.
	 */
	private NetworkTool()
	{
		super("network");
		reload();
	}

    /**
     * Method to retrieve the singleton associated with the Network tool.
     * @return the Network tool.
     */
    public static NetworkTool getNetworkTool() { return tool; }

	/**
	 * Reloads cell information after major changes such as librairy read.
	 */
	static private void reload()
	{
		int maxCell = 1;
		for (Iterator<Library> lit = Library.getLibraries(); lit.hasNext(); )
		{
			Library lib = (Library)lit.next();
			for (Iterator<Cell> cit = lib.getCells(); cit.hasNext(); )
			{
				Cell c = (Cell)cit.next();
				while (c.getCellIndex() >= maxCell) maxCell *= 2;
			}
		}
		cells = new NetCell[maxCell];
		for (Iterator <Library>lit = Library.getLibraries(); lit.hasNext(); )
		{
			Library lib = (Library)lit.next();
			for (Iterator<Cell> cit = lib.getCells(); cit.hasNext(); )
			{
				Cell c = (Cell)cit.next();
				if (getNetCell(c) != null) continue;
				if (c.isIcon() || c.isSchematic())
					new NetSchem(c);
				else
					new NetCell(c);
			}
		}
	}

	static void exportsChanged(Cell cell) {
		NetCell netCell = NetworkTool.getNetCell(cell);
		netCell.exportsChanged();
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

    private static void redoNetworkNumbering(boolean reload)
    {
		// Check that we are in changing thread
		assert Job.canComputeNetlist();

        long startTime = System.currentTimeMillis();
		if (reload) {
			reload();
		}
        int ncell = 0;
        for(Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
        {
            Library lib = it.next();
            // Handling clipboard case (one type of hidden libraries)
            if (lib.isHidden()) continue;

            for(Iterator<Cell> cit = lib.getCells(); cit.hasNext(); )
            {
                Cell cell = (Cell)cit.next();
                ncell++;
                cell.getNetlist(false);
            }
        }
        long endTime = System.currentTimeMillis();
        float finalTime = (endTime - startTime) / 1000F;
		if (ncell != 0 && reload && showInfo)
			System.out.println("**** Renumber networks of " + ncell + " cells took " + finalTime + " seconds");

		synchronized(mutex) {
			networksValid = true;
			mutex.notify();
		}
    }

	private static void invalidate() {
		// Check that we are in changing thread
		assert Job.canComputeNetlist();

		if (!networksValid)
			return;
		synchronized(mutex) {
			networksValid = false;
		}
	}

	/**
	 * Method to set the subsequent changes to be "quiet".
	 * Quiet changes are not passed to constraint satisfaction, not recorded for Undo and are not broadcast.
	 */
	public static void changesQuiet(boolean quiet) {
		if (quiet) {
			invalidate();
		} else {
			redoNetworkNumbering(true);
		}
    }

	/**
	 * Method to set the level of information that is displayed.
	 * When libraries are being read "quietly", no information should be output.
	 * @param infoOutput true for normal information output, false for quiet.
	 */
	public static void setInformationOutput(boolean infoOutput)
	{
		showInfo = infoOutput;
	}

	/****************************** PUBLIC METHODS ******************************/

	/**
	 * Returns Netlist for a given cell obtain with user-default set of options.
	 * @param cell cell to get Netlist.
	 * @return Netlist of this cell.
	 */
	public static Netlist acquireUserNetlist(Cell cell) {
		Netlist netlist = null;
		try {
			netlist = getNetlist(cell, isIgnoreResistors_());
		} catch (NetlistNotReady e) {
		}
		return netlist;
	}

	/**
	 * Returns Netlist for a given cell obtain with user-default set of options.
	 * @param cell cell to get Netlist.
	 * @return Netlist of this cell.
	 */
	public static Netlist getUserNetlist(Cell cell) {
		if (Job.canComputeNetlist()) {
			NetCell netCell = getNetCell(cell);
			return netCell.getNetlist(isIgnoreResistors_());
		}
        if (Job.getDebug() && SwingUtilities.isEventDispatchThread())
		{
			System.out.println("getUserNetlist() used in GUI thread");
		}
		boolean shortResistors = isIgnoreResistors_();
		synchronized(mutex) {
			while (!networksValid) {
				try {
					System.out.println("Waiting for User Netlist...");
					mutex.wait(1000);
					if (!networksValid)
						throw new NetlistNotReady();
				} catch (InterruptedException e) {
				} catch (NetlistNotReady e) {
					e.printStackTrace(System.err);
				}
			}
			NetCell netCell = getNetCell(cell);
			return netCell.getNetlist(shortResistors);
		}
	}

	/** Recompute the Netlist structure for given Cell.
	 * @param cell cell to recompute Netlist structure.
     * <p>Because shorting resistors is a fairly common request, it is 
     * implemented in the method if @param shortResistors is set to true.
	 * @return the Netlist structure for Cell.
     */
	public static Netlist getNetlist(Cell cell, boolean shortResistors) {
		if (Job.canComputeNetlist()) {
			NetCell netCell = getNetCell(cell);
			return netCell.getNetlist(shortResistors);
		}
		synchronized(mutex) {
			if (!networksValid)
				throw new NetlistNotReady();
			NetCell netCell = getNetCell(cell);
			return netCell.getNetlist(shortResistors);
		}
	}

    /**
     * Method to retrieve all networks for a portInst.
     * Used by Highlighter and Connection
     * @param pi the PortInst being considered.
     * @param netlist the netlist being searched.
     * @param nets a set into which all found networks will be added.
     * @return set the set of found networks.
     */
    public static Set<Network> getNetworksOnPort(PortInst pi, Netlist netlist, Set<Network> nets)
    {
        boolean added = false;
        if (nets == null) nets = new HashSet<Network>();

        for(Iterator<Connection> aIt = pi.getConnections(); aIt.hasNext(); )
        {
            Connection con = (Connection)aIt.next();
            ArcInst ai = con.getArc();
            int wid = netlist.getBusWidth(ai);
            for(int i=0; i<wid; i++)
            {
                Network net = netlist.getNetwork(ai, i);
                if (net != null)
                {
                    added = true;
                    nets.add(net);
                }
            }
        }
        if (!added)
        {
            Network net = netlist.getNetwork(pi);
            if (net != null) nets.add(net);
        }
        return nets;
    }

    /**
     * Method to retrieve all networks on a Geometric object.
     * @param geom the Geometric being considered.
     * @param netlist the netlist being searched.
     * @param nets a set into which all found networks will be added.
     * @return set the set of found networks.
     */
    public static Set<Network> getNetworks(Geometric geom, Netlist netlist, Set<Network> nets)
    {
        if (nets == null) nets = new HashSet<Network>();
        else nets.clear();

        if (geom instanceof ArcInst)
            nets.add(netlist.getNetwork((ArcInst)geom, 0));
        else
        {
            NodeInst ni = (NodeInst)geom;
            for (Iterator<PortInst> pIt = ni.getPortInsts(); pIt.hasNext(); )
            {
                PortInst pi = (PortInst)pIt.next();
                nets = getNetworksOnPort(pi, netlist, nets);
                //nets.add(netlist.getNetwork(ni, pi.getPortProto(), 0));
                //nets.add(netlist.getNetwork(pi));
            }
        }
        return nets;
    }

    /****************************** CHANGE LISTENER ******************************/

	/**
	 * Method to initialize a tool.
	 */
	public void init()
	{
		setOn();
		if (!debug) return;
		System.out.println("NetworkTool.init()");
	}

	public void request(String cmd)
	{
		if (!debug) return;
		System.out.println("NetworkTool.request("+cmd+")");
	}

	public void examineCell(Cell cell)
	{
		if (!debug) return;
		System.out.println("NetworkTool.examineCell("+cell+")");
	}

	public void slice()
	{
		if (!debug) return;
		System.out.println("NetworkTool.slice()");
	}

	public void startBatch(Tool tool, boolean undoRedo)
	{
		if (!debug) return;
		System.out.println("NetworkTool.startBatch("+tool+","+undoRedo+")");
	}

	public void endBatch()
	{
		try {
			redoNetworkNumbering(false);
		} catch (Exception e) {
			e.printStackTrace(System.err);
			System.err.println("Full Network renumbering after crash.");
			e.printStackTrace(System.out);
			System.out.println("Full Network renumbering after crash.");
			redoNetworkNumbering(true);
		}
		if (!debug) return;
		System.out.println("NetworkTool.endBatch()");
	}

	public void modifyNodeInst(NodeInst ni, ImmutableNodeInst oD)
	{
		if (ni.getNameKey() != oD.name)	{
            invalidate();
            getNetCell(ni.getParent()).setNetworksDirty();
        }
		if (!debug) return;
		System.out.println("NetworkTool.modifyNodeInst("+ni+","+oD+")");
	}

	public void modifyArcInst(ArcInst ai, ImmutableArcInst oD)
	{
		if (ai.getNameKey() != oD.name)	{
            invalidate();
            getNetCell(ai.getParent()).setNetworksDirty();
        }
		if (!debug) return;
		System.out.println("NetworkTool.modifyArcInst("+ai+","+oD+")");
	}

	public void modifyExport(Export pp, ImmutableExport oD)
	{
        invalidate();
        exportsChanged((Cell)pp.getParent());
		if (!debug) return;
		System.out.println("NetworkTool.modifyExport("+pp+","+oD+")");
	}

	/**
	 * Method to announce a move of a Cell int CellGroup.
	 * @param cell the cell that was moved.
	 * @param oCellGroup the old CellGroup of the Cell.
	 */
	public void modifyCellGroup(Cell cell, Cell.CellGroup oCellGroup)
	{
		invalidate();
		if (cell.isIcon() || cell.isSchematic()) {
			NetSchem.updateCellGroup(oCellGroup);
			NetSchem.updateCellGroup(cell.getCellGroup());
		}
		if (!debug) return;
		System.out.println("NetworkTool.modifyCellGroup(" + cell + ",_)");
	}

	public void newObject(ElectricObject obj)
	{
		invalidate();
		Cell cell = obj.whichCell();
		if (obj instanceof Cell)
		{
			if (cell.isIcon() || cell.isSchematic())
				new NetSchem(cell);
			else
				new NetCell(cell);
		} else if (obj instanceof Export) {
			exportsChanged(cell);
		} else {
			if (cell != null) getNetCell(cell).setNetworksDirty();
		}
		if (!debug) return;
		System.out.println("NetworkTool.newObject("+obj+")");
	}

	public void killObject(ElectricObject obj)
	{
		invalidate();
		Cell cell = obj.whichCell();
		if (obj instanceof Cell)
		{
			setCell(cell, null);
			if (cell.isIcon() || cell.isSchematic())
				NetSchem.updateCellGroup(cell.getCellGroup());
		} else if (obj instanceof Export) {
            exportsChanged(cell);
        } else {
			if (cell != null) getNetCell(cell).setNetworksDirty();
		}
		if (!debug) return;
		System.out.println("NetworkTool.killObject("+obj+")");
	}

	public void renameObject(ElectricObject obj, Object oldName)
	{
		invalidate();
		if (!debug) return;
		System.out.println("NetworkTool.reanameObject("+obj+","+oldName+")");
	}

	public void modifyVariables(ElectricObject obj, ImmutableElectricObject oldImmutable) {
		if (!debug) return;
		System.out.println("NetworkTool.modifyVariables("+obj+")");
	}

	public void readLibrary(Library lib)
	{
		if (!debug) return;
		System.out.println("NetworkTool.readLibrary("+lib+")");
	}

	public void eraseLibrary(Library lib)
	{
		if (!debug) return;
		System.out.println("NetworkTool.eraseLibrary("+lib+")");
	}

	public void writeLibrary(Library lib)
	{
		if (!debug) return;
		System.out.println("NetworkTool.writeLibrary("+lib+")");
	}

    /**
     * Update network information from old immutable snapshot to new immutable snapshot.
     * @param oldSnapshot old immutable snapshot.
     * @param newSnapshot new immutable snapshot.
     */
    public static void updateAll(Snapshot oldSnapshot, Snapshot newSnapshot) {
        invalidate();
        int maxCells = Math.max(oldSnapshot.cellBackups.size(), newSnapshot.cellBackups.size());
        if (cells.length < maxCells) {
            NetCell[] newCells = new NetCell[Math.max(cells.length*2, maxCells)];
            System.arraycopy(cells, 0, newCells, 0, cells.length);
            cells = newCells;
        }
        // killed Cells
        for (int i = 0; i < maxCells; i++) {
            CellBackup oldBackup = oldSnapshot.getCell(i);
            CellBackup newBackup = newSnapshot.getCell(i);
            if (newBackup != null || oldBackup == null) continue;
            cells[i] = null;
        }
        // new Cells
        for (int i = 0; i < maxCells; i++) {
            CellBackup oldBackup = oldSnapshot.getCell(i);
            CellBackup newBackup = newSnapshot.getCell(i);
            if (newBackup == null || oldBackup != null) continue;
            Cell cell = (Cell)CellId.getByIndex(i).inCurrentThread();
            if (cell.isIcon() || cell.isSchematic())
                new NetSchem(cell);
            else
                new NetCell(cell);
        }
        // Changed CellGroups
        if (oldSnapshot.cellGroups != newSnapshot.cellGroups) {
            // Lower Cell changed
            for (int i = 0; i < newSnapshot.cellGroups.length; i++) {
                if (newSnapshot.cellGroups[i] != i) continue;
                if (i < oldSnapshot.cellGroups.length && i == oldSnapshot.cellGroups[i]) continue;
                Cell cell = (Cell)CellId.getByIndex(i).inCurrentThread();
                NetSchem.updateCellGroup(cell.getCellGroup());
            }
            // Lower Cell same, but some cells deleted
            for (int i = 0; i < oldSnapshot.cellGroups.length; i++) {
                int l = oldSnapshot.cellGroups[i];
                if (l < 0 || l >= newSnapshot.cellGroups.length || newSnapshot.cellGroups[l] != l) continue;
                if (i < newSnapshot.cellGroups.length && newSnapshot.cellGroups[i] == l) continue;
                Cell cell = (Cell)CellId.getByIndex(l).inCurrentThread();
                NetSchem.updateCellGroup(cell.getCellGroup());
            }
        }
        // Main schematics changed
        for (int i = 0; i < maxCells; i++) {
            CellBackup newBackup = newSnapshot.getCell(i);
            CellBackup oldBackup = oldSnapshot.getCell(i);
            if (newBackup == null || oldBackup == null) continue;
            if (oldBackup.isMainSchematics == newBackup.isMainSchematics) continue;
            Cell cell = (Cell)CellId.getByIndex(i).inCurrentThread();
            NetSchem.updateCellGroup(cell.getCellGroup());
        }
        // Cell contents changed
        for (int i = 0; i < maxCells; i++) {
            CellBackup oldBackup = oldSnapshot.getCell(i);
            CellBackup newBackup = newSnapshot.getCell(i);
            if (newBackup == null || oldBackup == null) continue;
            if (oldBackup == newBackup) continue;
            Cell cell = (Cell)CellId.getByIndex(i).inCurrentThread();
            boolean exportsChanged = !newBackup.sameExports(oldBackup);
            if (!exportsChanged) {
                for (int j = 0; j < newBackup.exports.length; j++) {
                    if (newBackup.exports[j].name != oldBackup.exports[j].name)
                        exportsChanged = true;
                }
            }
            NetCell netCell = NetworkTool.getNetCell(cell);
            if (exportsChanged)
                netCell.exportsChanged();
            else
                netCell.setNetworksDirty();
        }
        redoNetworkNumbering(false);
    }
    
	/****************************** OPTIONS ******************************/

//	private static Pref cacheUnifyPowerAndGround = Pref.makeBooleanPref("UnifyPowerAndGround", NetworkTool.tool.prefs, false);
//    static { cacheUnifyPowerAndGround.attachToObject(NetworkTool.tool, "Tools/Network tab", "Networks unify Power and Ground"); }
//	/**
//	 * Method to tell whether all Power nets are unified and all Ground nets are unified.
//	 * @return true if all Power nets are unified and all Ground nets are unified.
//	 */
//	public static boolean isUnifyPowerAndGround() { return cacheUnifyPowerAndGround.getBoolean(); }
//	/**
//	 * Method to set whether all Power nets are unified and all Ground nets are unified.
//	 * @param u true if all Power nets are unified and all Ground nets are to be unified.
//	 */
//	public static void setUnifyPowerAndGround(boolean u) { cacheUnifyPowerAndGround.setBoolean(u); }

//	private static Pref cacheUnifyLikeNamedNets = Pref.makeBooleanPref("UnifyLikeNamedNets", NetworkTool.tool.prefs, false);
//    static { cacheUnifyLikeNamedNets.attachToObject(NetworkTool.tool, "Tools/Network tab", "Networks unify all like-named nets"); }
//	/**
//	 * Method to tell whether all like-named nets are unified.
//	 * Typically, like-named nets (two networks with the same name) are unified only in a schematic.
//	 * With this option, the unification happens in layout cells as well (not recommended).
//	 * @return true if all like-named nets are unified.
//	 */
//	public static boolean isUnifyLikeNamedNets() { return cacheUnifyLikeNamedNets.getBoolean(); }
//	/**
//	 * Method to set whether all like-named nets are unified.
//	 * Typically, like-named nets (two networks with the same name) are unified only in a schematic.
//	 * With this option, the unification happens in layout cells as well (not recommended).
//	 * @param u true if all like-named nets are unified.
//	 */
//	public static void setUnifyLikeNamedNets(boolean u) { cacheUnifyLikeNamedNets.setBoolean(u); }

	private static Pref cacheIgnoreResistors = Pref.makeBooleanPref("IgnoreResistors", NetworkTool.tool.prefs, false);
    static { cacheIgnoreResistors.attachToObject(NetworkTool.tool, "Tools/Network tab", "Networks ignore Resistors"); }
	/**
	 * Method to tell whether resistors are ignored in the circuit.
	 * When ignored, they appear as a "short", connecting the two sides.
	 * When included, they appear as a component with different networks on either side.
	 * @return true if resistors are ignored in the circuit.
	 */
	public static boolean isIgnoreResistors() { return cacheIgnoreResistors.getBoolean(); }
	private static boolean isIgnoreResistors_() { return false; }
	/**
	 * Method to set whether resistors are ignored in the circuit.
	 * When ignored, they appear as a "short", connecting the two sides.
	 * When included, they appear as a component with different networks on either side.
	 * @param i true if resistors are ignored in the circuit.
	 */
	public static void setIgnoreResistors(boolean i) { cacheIgnoreResistors.setBoolean(i); }

//	private static Pref cacheUnificationPrefix = Pref.makeStringPref("UnificationPrefix", NetworkTool.tool.prefs, "");
//    static { cacheUnificationPrefix.attachToObject(NetworkTool.tool, "Tools/Network tab", "Network unification prefix"); }
//	/**
//	 * Method to return the list of unification prefixes.
//	 * Unification prefixes are strings which, when two nets both start with them, cause the networks to be unified.
//	 * For example, the prefix "vdd" would cause networks "vdd_1" and "vdd_dirty" to be unified.
//	 * @return the list of unification prefixes.
//	 */
//	public static String getUnificationPrefix() { return cacheUnificationPrefix.getString(); }
//	/**
//	 * Method to set the list of unification prefixes.
//	 * Unification prefixes are strings which, when two nets both start with them, cause the networks to be unified.
//	 * For example, the prefix "vdd" would cause networks "vdd_1" and "vdd_dirty" to be unified.
//	 * @param p the list of unification prefixes.
//	 */
//	public static void setUnificationPrefix(String p) { cacheUnificationPrefix.setString(p); }

//	private static Pref cacheBusBaseZero = Pref.makeBooleanPref("BusBaseZero", NetworkTool.tool.prefs, false);
//    static { cacheBusBaseZero.attachToObject(NetworkTool.tool, "Tools/Network tab", "Default busses starting index"); }
//	/**
//	 * Method to tell whether unnamed busses should be zero-based.
//	 * The alternative is 1-based.
//	 * @return true if unnamed busses should be zero-based.
//	 */
//	public static boolean isBusBaseZero() { return cacheBusBaseZero.getBoolean(); }
//	/**
//	 * Method to set whether unnamed busses should be zero-based.
//	 * The alternative is 1-based.
//	 * @param z true if unnamed busses should be zero-based.
//	 */
//	public static void setBusBaseZero(boolean z) { cacheBusBaseZero.setBoolean(z); }

	private static Pref cacheBusAscending = Pref.makeBooleanPref("BusAscending", NetworkTool.tool.prefs, false);
    static { cacheBusAscending.attachToObject(NetworkTool.tool, "Tools/Network tab", "Default busses are ascending"); }
	/**
	 * Method to tell whether unnamed busses should be numbered ascending.
	 * The alternative is descending.
	 * @return true if unnamed busses should be numbered ascending.
	 */
	public static boolean isBusAscending() { return cacheBusAscending.getBoolean(); }
	/**
	 * Method to set whether unnamed busses should be numbered ascending.
	 * The alternative is descending.
	 * @param a true if unnamed busses should be numbered ascending.
	 */
	public static void setBusAscending(boolean a) { cacheBusAscending.setBoolean(a); }
}
