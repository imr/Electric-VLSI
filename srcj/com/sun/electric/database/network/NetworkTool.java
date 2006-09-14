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

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.projectSettings.ProjSettingsNode;
import com.sun.electric.tool.user.projectSettings.ProjSettings;

import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import javax.swing.SwingUtilities;

/**
 * This is the Network tool.
 */
public class NetworkTool extends Tool
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
            super("Renumber All Networks", User.getUserTool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            startJob();
        }

        public boolean doIt() throws JobException
        {
			EDatabase.serverDatabase().getNetworkManager().redoNetworkNumbering(true);
            return true;
        }
    }

	// ---------------------- private and protected methods -----------------

	/** the Network tool. */						private static final NetworkTool tool = new NetworkTool();
	/** All cells have networks up-to-date */ 		static boolean networksValid = false;
	/** Mutex object */								static Object mutex = new Object();
	/** flag for debug print. */					static boolean debug = false;
	/** flag for information print. */				static boolean showInfo = true;

    /** total number of errors for statistics */    public static int totalNumErrors = 0;
    /** sort keys for sorting network errors */     static final int errorSortNetworks = 0;
                                                    static final int errorSortNodes = 1;
                                                    static final int errorSortPorts = 2;

	/**
	 * The constructor sets up the Network tool.
	 */
	public NetworkTool()
	{
		super("network");
	}
    
    public static NetworkTool getNetworkTool() { return tool; }

//	/**
//	 * Method to set the subsequent changes to be "quiet".
//	 * Quiet changes are not passed to constraint satisfaction, not recorded for Undo and are not broadcast.
//	 */
//	public static void changesQuiet(boolean quiet) {
//		if (quiet) {
//			invalidate();
//		} else {
//			redoNetworkNumbering(true);
//		}
//    }

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
        NetworkManager mgr = cell.getDatabase().getNetworkManager();
		if (EDatabase.theDatabase.canComputeNetlist()) {
            mgr.advanceSnapshot();
			NetCell netCell = mgr.getNetCell(cell);
			return netCell.getNetlist(isIgnoreResistors_());
		}
        if (Job.getDebug() && SwingUtilities.isEventDispatchThread())
		{
			System.out.println("getUserNetlist() used in GUI thread");
		}
		boolean shortResistors = isIgnoreResistors_();
		synchronized(NetworkTool.mutex) {
			while (!NetworkTool.networksValid) {
				try {
					System.out.println("Waiting for User Netlist...");
					NetworkTool.mutex.wait(1000);
					if (!NetworkTool.networksValid)
						throw new NetlistNotReady();
				} catch (InterruptedException e) {
				} catch (NetlistNotReady e) {
					e.printStackTrace(System.err);
				}
			}
			NetCell netCell = mgr.getNetCell(cell);
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
        NetworkManager mgr = cell.getDatabase().getNetworkManager();
		if (EDatabase.theDatabase.canComputeNetlist()) {
            if (!cell.isLinked())
                return null;
            mgr.advanceSnapshot();
			NetCell netCell = mgr.getNetCell(cell);
			return netCell.getNetlist(shortResistors);
		}
		synchronized(NetworkTool.mutex) {
			if (!NetworkTool.networksValid)
				throw new NetlistNotReady();
			NetCell netCell = mgr.getNetCell(cell);
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
            // port may be exported, without wire attached, and may
            // connect by export name to other wires
            NodeInst ni = pi.getNodeInst();
            Set<PortInst> ports = new HashSet<PortInst>();
            ports.add(pi);
            for (Iterator<PortInst> it = ni.getPortInsts(); it.hasNext(); ) {
                // several ports on node may be connected together at lower level
                PortInst otherpi = it.next();
                if (otherpi == pi) continue;
                if (netlist.sameNetwork(ni, pi.getPortProto(), ni, otherpi.getPortProto()))
                    ports.add(otherpi);
            }
            for (Iterator<Export> it = ni.getParent().getExports(); it.hasNext(); ) {
                Export export = it.next();
                if (ports.contains(export.getOriginalPort())) {
                    Name name = export.getNameKey();
                    for (int i=0; i<name.busWidth(); i++) {
                        nets.add(netlist.getNetwork(pi.getNodeInst(), pi.getPortProto(), i));
                        added = true;
                    }
                    break;
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

	/**
	 * Method to initialize a tool.
	 */
	public void init()
	{
		setOn();
		if (!debug) return;
		System.out.println("NetworkTool.init()");
	}

	/****************************** OPTIONS ******************************/

    private static Pref cacheIgnoreResistors = Pref.makeBooleanSetting("IgnoreResistors", NetworkTool.tool.prefs, NetworkTool.tool,
            tool.getProjectSettings(), null,
        "Netlists tab", "Networks ignore Resistors", false);
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

	private static Pref cacheBusAscending = Pref.makeBooleanPref("BusAscending", NetworkTool.tool.prefs, false);
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
