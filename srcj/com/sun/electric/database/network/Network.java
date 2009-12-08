/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Network.java
 * Written by: Dmitry Nadezhin, Sun Microsystems.
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.database.network;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.prototype.PortProto;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Networks represent connectivity.
 *
 * <p> For a Cell, each Network represents a collection of PortInsts
 * that are electrically connected.
 */
public class Network implements Comparable {
    // ------------------------- private data ------------------------------

    private final Netlist netlist; // Netlist that owns this Network
    private final int netIndex; // Index of this Network in Netlist.

    /**
     * Creates Network in a given netlist with specified index.
     * @param netlist Netlist where Network lives.
     * @param netIndex index of Network.
     */
    Network(Netlist netlist, int netIndex) {
        this.netlist = netlist;
        this.netIndex = netIndex;
    }

    // --------------------------- public methods ------------------------------
    /** Returns the Netlist of this Network.
     * @return Netlist of this Network.
     */
    public Netlist getNetlist() {
        return netlist;
    }

    /**
     * Networks are sorted by their description text.
     */
    public int compareTo(Object other) {
        String s = toString();
        String sOther = other.toString();
        return s.compareToIgnoreCase(sOther);
    }

    /** Returns parent cell of this Network.
     * @return parent cell of this Network.
     */
    public Cell getParent() {
        return netlist.netCell.cell;
    }

    /** Returns index of this Network in netlist. */
    public int getNetIndex() {
        return netIndex;
    }

    /** A net can have multiple names. Return alphabetized list of names. */
    public Iterator<String> getNames() {
        return netlist.getNames(netIndex);
    }

    /** A net can have multiple names. Return alphabetized list of names. */
    public Iterator<String> getExportedNames() {
        return netlist.getExportedNames(netIndex);
    }

    /**
     * Returns most appropriate name of the net.
     * Intitialized net has at least one name - user-defiend or temporary.
     */
    public String getName() {
        return netlist.getName(netIndex);
    }

    /** Returns true if nm is one of Network's names */
    public boolean hasName(String nm) {
        return netlist.hasName(netIndex, nm);
    }

    /** Get iterator over all PortInsts on Network.  Note that the
     * PortFilter class is useful for filtering out frequently excluded
     * PortInsts.
     * This is well-defined for Layout cells,
     * but what is PortInst in Schematic cell with buses ???
     */
    public Iterator<PortInst> getPorts() {
        List<PortInst> ports = getPortsList();
        return ports.iterator();
    }

    public List<PortInst> getPortsList() {
        ArrayList<PortInst> ports = new ArrayList<PortInst>();
        for (Iterator<NodeInst> it = getParent().getNodes(); it.hasNext();) {
            NodeInst ni = it.next();
            for (Iterator<PortInst> pit = ni.getPortInsts(); pit.hasNext();) {
                PortInst pi = pit.next();
                if (netlist.getNetIndex(ni, pi.getPortProto(), 0) == netIndex) {
                    ports.add(pi);
                }
            }
        }
        return ports;
    }

    /**
     * Get iterator over all NodeInsts on Network.
     */
    public Iterator<NodeInst> getNodes() {
        ArrayList<NodeInst> nodes = new ArrayList<NodeInst>();
        for (Iterator<NodeInst> it = getParent().getNodes(); it.hasNext();) {
            NodeInst ni = it.next();
            for (Iterator<PortInst> pit = ni.getPortInsts(); pit.hasNext();) {
                PortInst pi = pit.next();
                if (netlist.getNetIndex(ni, pi.getPortProto(), 0) == netIndex) {
                    nodes.add(ni);
                    break; // stop the loop here
                }
            }
        }
        return nodes.iterator();
    }

    /** Get iterator over all Globals on Network */
    public Iterator<Global> getGlobals() {
        Global.Set globals = getNetlist().getGlobals();
        ArrayList<Global> globalsOnNet = new ArrayList<Global>();
        for (int i = 0; i < globals.size(); i++) {
            Global g = globals.get(i);
            if (getNetlist().getNetIndex(g) == netIndex) {
                globalsOnNet.add(g);
            }

        }
        return globalsOnNet.iterator();
    }

    /** Get iterator over all Exports on Network */
    public Iterator<Export> getExports() {
        ArrayList<Export> exports = new ArrayList<Export>();
        for (Iterator<Export> it = getParent().getExports(); it.hasNext();) {
            Export e = it.next();
            int busWidth = netlist.getBusWidth(e);
            for (int i = 0; i < busWidth; i++) {
                if (netlist.getNetIndex(e, i) == netIndex) {
                    exports.add(e);
                    break;
                }
            }
        }
        return exports.iterator();
    }

    /** Get iterator over all ArcInsts on Network */
    public Iterator<ArcInst> getArcs() {
        ArrayList<ArcInst> arcs = new ArrayList<ArcInst>();
        for (Iterator<ArcInst> it = getParent().getArcs(); it.hasNext();) {
            ArcInst ai = it.next();
            int busWidth = netlist.getBusWidth(ai);
            for (int i = 0; i < busWidth; i++) {
                if (netlist.getNetIndex(ai, i) == netIndex) {
                    arcs.add(ai);
                    break;
                }
            }
        }
        return arcs.iterator();
    }

    /**
     * Method to tell whether this network has any exports or globals on it.
     * @return true if there are exports or globals on this Network.
     */
    public boolean isExported() {
        return netlist.isExported(netIndex);
    }

    /**
     * Method to tell whether this network has user-defined name.
     * @return true if this Network has user-defined name.
     */
    public boolean isUsernamed() {
        return netlist.isUsernamed(netIndex);
    }

    /**
     * Method to describe this Network as a string.
     * @param withQuotes to wrap description between quotes
     * @return a String describing this Network.
     */
    public String describe(boolean withQuotes) {
        Iterator<String> it = getNames();
        String name = it.next();
        while (it.hasNext()) {
            name += "/" + it.next();
        }
        if (withQuotes) {
            name = "'" + name + "'";
        }
        return name;
    }

    public Export findExportWithSameCharacteristic(PortProto p) {
        for (Iterator<Export> itP = getExports(); itP.hasNext();) {
            Export exp = itP.next();
            if (exp.getCharacteristic() == p.getCharacteristic()) {
                return exp;
            }
        }
        return null;
    }

    /**
     * Returns a printable version of this Network.
     * @return a printable version of this Network.
     */
    @Override
    public String toString() {
        return "network " + describe(true);
    }
}
