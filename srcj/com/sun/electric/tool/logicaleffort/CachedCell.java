/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CachedCell.java
 * Written by Jonathan Gainsley, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.logicaleffort;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class CachedCell {

    /** source cell */                  private Cell cell;
    /** map of Nodable to LENodable */  private Map<Nodable,LENodable> lenodables;
    /** map of CachedCell instances */  private Map<Nodable,CellNodable> cellnodables; // key: Nodable, Object: CellNodable
    /** this cell or subcells contains le gates */  private boolean containsSizableGates;
    /** local networks */               private Map<Network,LENetwork> localNetworks; // key: Network, Object: LENetwork
    /** if this cell's nodes and subcell's nodes can be fully evaluated as if this cell was the top level */
                                        private Boolean contextFree;
    /** list of all cached nodables */  //private List allCachedNodables;

    private static final boolean DEBUG = false;

    protected static class CellNodable {
        Nodable no;
        CachedCell subCell;
        Variable mfactorVar;
    }

    protected CachedCell(Cell cell, Netlist netlist) {
        this.cell = cell;
        lenodables = new HashMap<Nodable,LENodable>();
        localNetworks = new HashMap<Network,LENetwork>();
        cellnodables = new HashMap<Nodable,CellNodable>();
        //allCachedNodables = new ArrayList();
        containsSizableGates = false;
        contextFree = null;
        if (netlist != null) {
            // populate local networks
            for (Iterator<Network> it = netlist.getNetworks(); it.hasNext(); ) {
                Network jnet = (Network)it.next();
                LENetwork net = new LENetwork(jnet.describe(false));
                if (localNetworks.containsKey(jnet))
                    System.out.println("Possible hashmap conflict in localNetworks!");
                localNetworks.put(jnet, net);
            }
        }
    }

    protected boolean isContainsSizableGates() { return containsSizableGates; }

    protected LENodable getLENodable(Nodable no) { return lenodables.get(no); }
    protected Iterator getLENodables() { return lenodables.values().iterator(); }

    protected CellNodable getCellNodable(Nodable no) { return cellnodables.get(no); }
    protected Iterator getCellNodables() { return cellnodables.values().iterator(); }

    protected Map<Network,LENetwork> getLocalNetworks() { return localNetworks; }

    //protected List getAllCachedNodables() { return allCachedNodables; }

    /**
     * Adds instance of LENodable to this cached cell.
     * @param no the corresponding nodable
     * @param leno the LENodable
     */
    protected void add(Nodable no, LENodable leno) {
        if (leno.isLeGate()) containsSizableGates = true;
        // hook up gate to local networks
        for (Iterator<LEPin> it = leno.getPins().iterator(); it.hasNext(); ) {
            LEPin pin = (LEPin)it.next();
            Network jnet = pin.getNetwork();
            LENetwork net = localNetworks.get(jnet);
 /*           if (net == null) {
                net = new LENetwork(jnet.describe());
                localNetworks.put(jnet, net);
            }*/
            net.add(pin);
        }

        if (lenodables.containsKey(no))
            System.out.println("Possible hash map conflict in lenodables!");
        lenodables.put(no, leno);
        //allCachedNodables.add(leno);
    }

    /**
     * Adds instance of cached cell "subCell" to this cached cell. This needs to be
     * called from Visitor.exitCell of subcell, because it requires subCell's CellInfo.
     * @param no the nodable for subcell (in parent)
     * @param info the parent info
     * @param subCell the subcell cached cell
     * @param subCellInfo the subcell CellInfo
     */
    protected void add(Nodable no, LENetlister2.LECellInfo info,
                       CachedCell subCell, LENetlister2.LECellInfo subCellInfo, LENetlister.NetlisterConstants constants) {
        CellNodable ceno = new CellNodable();
        ceno.no = no;
        ceno.subCell = subCell;
        ceno.mfactorVar = LETool.getMFactor(no);
        if (cellnodables.containsKey(no))
            System.out.println("Possible hash map conflict in cellnodables!");
        cellnodables.put(no, ceno);
        if (subCell.isContainsSizableGates()) {
            containsSizableGates = true;
            // don't bother caching cells with sizeable gates, they get tossed out later
            return;
        }
        if (!subCell.isContextFree(constants)) {
            // if the subcell in not context free, make a copy of it in this cell with
            // the context. This cell can be instantiated in some other cell and eventually
            // become context free, however then all the instances are tied to that context.
            // If they were simply linked, that context could conflict with some context elsewhere
            // in the hierarchy where the subcell appears.
            subCell = subCell.copy();
            ceno.subCell = subCell;
        }

        //System.out.println("Importing to "+cell.describe()+" from "+no.getName()+":");
        // map subCell networks to this cell's networks through global network id's
        for (Iterator<Map.Entry<Network,LENetwork>> it = subCell.getLocalNetworks().entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Network,LENetwork> entry = (Map.Entry<Network,LENetwork>)it.next();
            Network subJNet = (Network)entry.getKey();
            LENetwork subLENet = (LENetwork)entry.getValue();
            Network localJNet = subCellInfo.getNetworkInParent(subJNet);
            if (localJNet == null) continue;
            LENetwork net = localNetworks.get(localJNet);
            if (net == null) {
                net = new LENetwork(localJNet.describe(false));
                localNetworks.put(localJNet, net);
            }
            net.add(subLENet);
            if (DEBUG) {
                if (!net.getName().equals("vdd") && !net.getName().equals("gnd")) {
                    System.out.println("  Added to "+net.getName() +" in "+cell.describe(false)+": "+subLENet.getName()+" from "+subCell.cell.describe(false));
                    System.out.println("     subcell="+subCell.cell.describe(false)+" subnet="+subLENet.getName()+": ");
                    subLENet.print();
                    System.out.println("     result: localcell="+cell.describe(false)+" localnet="+net.getName()+": ");
                    net.print();
                }
            }
        }
    }

    protected boolean isContextFree(LENetlister.NetlisterConstants constants) {
        if (contextFree == null) {
            // this cell has not yet been evaluated, do it now
            if (isContainsSizableGates()) {
                contextFree = new Boolean(false);
            } else {
                if (DEBUG) System.out.println("**** Checking if "+cell+" is context free");
                boolean cf = isContextFreeRecurse(VarContext.globalContext, 1f, constants);
                contextFree = new Boolean(cf);
            }
            if (DEBUG) System.out.println(">>>> "+cell+" set context free="+contextFree);
        }
        return contextFree.booleanValue();
    }

    private boolean isContextFreeRecurse(VarContext context, float mfactor, LENetlister2.NetlisterConstants constants) {
        // check LENodables
        for (Iterator<LENodable> it = lenodables.values().iterator(); it.hasNext(); ) {
            LENodable leno = (LENodable)it.next();
            boolean b = leno.setOnlyContext(context, null, mfactor, 0, constants);
            if (DEBUG) System.out.println("  gate "+leno.getName()+" cached: "+b+", leX="+leno.leX+", ID="+leno.hashCode());
            if (!b) return false;
        }
        // check cached cell instances
        for (Iterator<Map.Entry<Nodable,CellNodable>> it = cellnodables.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Nodable,CellNodable> entry = (Map.Entry<Nodable,CellNodable>)it.next();
            Nodable no = (Nodable)entry.getKey();
            CellNodable ceno = (CellNodable)entry.getValue();
            // see if any mfactor var is evaluatable
            float subCellMFactor = mfactor;
            if (ceno.mfactorVar != null) {
                Object retVal = context.evalVar(ceno.mfactorVar);
                if (retVal == null) {
                    if (DEBUG) System.out.println("  subcell "+ceno.no.getName()+" has Mfactor that cannot be evaluated");
                    return false;
                }
                subCellMFactor *= VarContext.objectToFloat(retVal, 1);
            }
            boolean b = ceno.subCell.isContextFree(constants);
            if (b) {
                if (DEBUG) System.out.println("  subcell "+ceno.no.getName()+" is context free");
                continue;
            }
            if (!ceno.subCell.isContextFreeRecurse(context.push(no), subCellMFactor, constants)) {
                if (DEBUG) System.out.println("  subcell "+ceno.no.getName()+" is NOT recursively context free");
                return false;
            } else
                if (DEBUG) System.out.println("  subcell "+ceno.no.getName()+" is recursively context free");
        }
        return true;
    }

    /**
     * Create a copy of the cached cell. This also recursively creates
     * copies of all subcells (cached cells).
     * @return the copy
     */
    private CachedCell copy() {
        CachedCell copy = new CachedCell(cell, null);
        // copy all subcell structures
        Map<LENetwork,LENetwork> origSubNetsToCopySubNets = new HashMap<LENetwork,LENetwork>();
        for (Iterator<Map.Entry<Nodable,CellNodable>> it = cellnodables.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Nodable,CellNodable> entry = (Map.Entry<Nodable,CellNodable>)it.next();
            Nodable no = (Nodable)entry.getKey();
            CellNodable ceno = (CellNodable)entry.getValue();
            CellNodable cenoCopy = new CellNodable();
            cenoCopy.no = ceno.no;
            cenoCopy.mfactorVar = ceno.mfactorVar;
            cenoCopy.subCell = ceno.subCell.copy();
            if (copy.cellnodables.containsKey(no))
                System.out.println("Possible hash map conflict in copy.cellnodables!");
            copy.cellnodables.put(no, cenoCopy);
            // build table of original subnets to copied subnets, so we
            // can update subnet links when we copy local networks
            for (Iterator<Map.Entry<Network,LENetwork>> nit = ceno.subCell.localNetworks.entrySet().iterator(); nit.hasNext(); ) {
                Map.Entry<Network,LENetwork> netentry = (Map.Entry<Network,LENetwork>)nit.next();
                Network jnet = (Network)netentry.getKey();
                LENetwork origNet = (LENetwork)netentry.getValue();
                LENetwork copyNet = cenoCopy.subCell.localNetworks.get(jnet);
                origSubNetsToCopySubNets.put(origNet, copyNet);
            }
        }
        // create new networks
        // add on subnets, because they are the connectivity to subcells.
        for (Iterator<Map.Entry<Network,LENetwork>> it = localNetworks.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Network,LENetwork> entry = (Map.Entry<Network,LENetwork>)it.next();
            Network jnet = (Network)entry.getKey();
            LENetwork net = (LENetwork)entry.getValue();
            LENetwork netCopy = new LENetwork(net.getName());
            for (Iterator<LENetwork> nit = net.getSubNets(); nit.hasNext(); ) {
                LENetwork subnet = (LENetwork)nit.next();
                // find copy of subnet in cellnodables copy
                LENetwork copySubNet = origSubNetsToCopySubNets.get(subnet);
                netCopy.add(copySubNet);
            }
            if (copy.localNetworks.containsKey(jnet))
                System.out.println("Possible hashmap conflict in copy.localNetworks!");
            copy.localNetworks.put(jnet, netCopy);
        }
        // copy all lenodables: this sets pins of local networks.
        for (Iterator<LENodable> it = lenodables.values().iterator(); it.hasNext(); ) {
            LENodable leno = (LENodable)it.next();
            LENodable lenoCopy = leno.copy();
            copy.add(leno.getNodable(), lenoCopy);
        }
        copy.containsSizableGates = containsSizableGates;
        copy.contextFree = contextFree;
        //copy.allCachedNodables.addAll(allCachedNodables);
        return copy;
    }

    /**
     * Print the contents of the Cached cell
     * @param indent
     */
    protected void printContents(String indent, PrintStream out) {
        out.println(indent+"CachedCell "+cell.describe(true)+" contents:");
        for (Iterator<LENodable> it = lenodables.values().iterator(); it.hasNext(); ) {
            LENodable leno = (LENodable)it.next();
            out.println(leno.printOneLine(indent+"  "));
        }
        for (Iterator<Map.Entry<Network,LENetwork>> it = localNetworks.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Network,LENetwork> entry = (Map.Entry<Network,LENetwork>)it.next();
            Network jnet = (Network)entry.getKey();
            LENetwork net = (LENetwork)entry.getValue();
            net.print(indent+"  ", out);
        }
        for (Iterator<Map.Entry<Nodable,CellNodable>> it = cellnodables.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Nodable,CellNodable> entry = (Map.Entry<Nodable,CellNodable>)it.next();
            Nodable no = (Nodable)entry.getKey();
            CellNodable ceno = (CellNodable)entry.getValue();
            //ceno.subCell.printContents(indent+"  ", out);
            boolean subCachable = ceno.subCell.isContextFree(null);
            System.out.println(indent+indent+"contains subCachedCell for "+ceno.subCell.cell+" ("+
                    (subCachable?"cachable":"not cachable")+")");
        }
    }
}
