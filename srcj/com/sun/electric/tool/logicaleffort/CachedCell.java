package com.sun.electric.tool.logicaleffort;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;

import java.util.*;

/**
 * Created by IntelliJ IDEA.
 * User: gainsley
 * Date: Aug 26, 2004
 * Time: 9:17:05 AM
 * To change this template use File | Settings | File Templates.
 */
public class CachedCell {

    /** source cell */                  private Cell cell;
    /** map of Nodable to LENodable */  private Map lenodables;
    /** map of CachedCell instances */  private Map cellnodables; // key: Nodable, Object: CellNodable
    /** this cell or subcells contains le gates */  private boolean containsSizableGates;
    /** local networks */               private Map localNetworks; // key: JNetwork, Object: LENetwork
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
        lenodables = new HashMap();
        localNetworks = new HashMap();
        cellnodables = new HashMap();
        //allCachedNodables = new ArrayList();
        containsSizableGates = false;
        contextFree = null;
        if (netlist != null) {
            // populate local networks
            for (Iterator it = netlist.getNetworks(); it.hasNext(); ) {
                JNetwork jnet = (JNetwork)it.next();
                LENetwork net = new LENetwork(jnet.describe());
                localNetworks.put(jnet, net);
            }
        }
    }

    protected boolean isContainsSizableGates() { return containsSizableGates; }

    protected LENodable getLENodable(Nodable no) { return (LENodable)lenodables.get(no); }
    protected Iterator getLENodables() { return lenodables.values().iterator(); }

    protected CellNodable getCellNodable(Nodable no) { return (CellNodable)cellnodables.get(no); }
    protected Iterator getCellNodables() { return cellnodables.values().iterator(); }

    protected Map getLocalNetworks() { return localNetworks; }

    //protected List getAllCachedNodables() { return allCachedNodables; }

    /**
     * Adds instance of LENodable to this cached cell.
     * @param no the corresponding nodable
     * @param leno the LENodable
     */
    protected void add(Nodable no, LENodable leno) {
        if (leno.isLeGate()) containsSizableGates = true;
        // hook up gate to local networks
        for (Iterator it = leno.getPins().iterator(); it.hasNext(); ) {
            LEPin pin = (LEPin)it.next();
            JNetwork jnet = pin.getJNetwork();
            LENetwork net = (LENetwork)localNetworks.get(jnet);
 /*           if (net == null) {
                net = new LENetwork(jnet.describe());
                localNetworks.put(jnet, net);
            }*/
            net.add(pin);
        }

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
                       CachedCell subCell, LENetlister2.LECellInfo subCellInfo, LENetlister2.NetlisterConstants constants) {
        CellNodable ceno = new CellNodable();
        ceno.no = no;
        ceno.subCell = subCell;
        ceno.mfactorVar = LETool.getMFactor(no);
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

        //System.out.println("Importing from "+no.getName()+":");
        // hook up networks: create temporary map from globalIDs to LENetworks
        Map globalNetworks = new HashMap();
        for (Iterator it = localNetworks.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry)it.next();
            JNetwork jnet = (JNetwork)entry.getKey();
            LENetwork net = (LENetwork)entry.getValue();
            int globalID = info.getNetID(jnet);
            globalNetworks.put(new Integer(globalID), net);
            //System.out.println("  Using local network "+net.getName());
        }
        // map subCell networks to this cell's networks through global network id's
        for (Iterator it = subCell.getLocalNetworks().entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry)it.next();
            JNetwork jnet = (JNetwork)entry.getKey();
            LENetwork subnet = (LENetwork)entry.getValue();
            int globalID = subCellInfo.getNetID(jnet);
            LENetwork net = (LENetwork)globalNetworks.get(new Integer(globalID));
            if (net == null) continue;
            //System.out.println("  Adding subnet "+ subnet.getName() + " to net "+ net.getName());
            net.add(subnet);
        }
/*        for (Iterator it = subCell.allCachedNodables.iterator(); it.hasNext(); ) {
            LENodable leno = (LENodable)it.next();
            //System.out.println("  Adding LENodable "+ leno.getName());
            allCachedNodables.add(leno);
        }*/
    }

    protected boolean isContextFree(LENetlister2.NetlisterConstants constants) {
        if (contextFree == null) {
            // this cell has not yet been evaluated, do it now
            if (isContainsSizableGates()) {
                contextFree = new Boolean(false);
            } else {
                if (DEBUG) System.out.println("**** Checking if "+cell.describe()+" is context free");
                boolean cf = isContextFreeRecurse(VarContext.globalContext, 1f, constants);
                contextFree = new Boolean(cf);
            }
            if (DEBUG) System.out.println(">>>> Cell "+cell.describe()+" set context free="+contextFree);
        }
        return contextFree.booleanValue();
    }

    private boolean isContextFreeRecurse(VarContext context, float mfactor, LENetlister2.NetlisterConstants constants) {
        // check LENodables
        for (Iterator it = lenodables.values().iterator(); it.hasNext(); ) {
            LENodable leno = (LENodable)it.next();
            boolean b = leno.setOnlyContext(context, null, mfactor, 0, constants);
            if (DEBUG) System.out.println("  gate "+context.getInstPath(".")+"."+leno.getName()+" cached: "+b);
            if (!b) return false;
        }
        // check cached cell instances
        for (Iterator it = cellnodables.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry)it.next();
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

    private CachedCell copy() {
        CachedCell copy = new CachedCell(cell, null);
        // copy all subcell structures
        Map origSubNetsToCopySubNets = new HashMap();
        for (Iterator it = cellnodables.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry)it.next();
            Nodable no = (Nodable)entry.getKey();
            CellNodable ceno = (CellNodable)entry.getValue();
            CellNodable cenoCopy = new CellNodable();
            cenoCopy.no = ceno.no;
            cenoCopy.mfactorVar = ceno.mfactorVar;
            cenoCopy.subCell = ceno.subCell.copy();
            copy.cellnodables.put(no, cenoCopy);
            // build table of original subnets to copied subnets, so we
            // can update subnet links when we copy local networks
            for (Iterator nit = ceno.subCell.localNetworks.entrySet().iterator(); nit.hasNext(); ) {
                Map.Entry netentry = (Map.Entry)nit.next();
                JNetwork jnet = (JNetwork)netentry.getKey();
                LENetwork origNet = (LENetwork)netentry.getValue();
                LENetwork copyNet = (LENetwork)cenoCopy.subCell.localNetworks.get(jnet);
                origSubNetsToCopySubNets.put(origNet, copyNet);
            }
        }
        // create new networks
        // add on subnets, because they are the connectivity to subcells.
        for (Iterator it = localNetworks.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry)it.next();
            JNetwork jnet = (JNetwork)entry.getKey();
            LENetwork net = (LENetwork)entry.getValue();
            LENetwork netCopy = new LENetwork(net.getName());
            for (Iterator nit = net.getSubNets(); nit.hasNext(); ) {
                LENetwork subnet = (LENetwork)nit.next();
                // find copy of subnet in cellnodables copy
                LENetwork copySubNet = (LENetwork)origSubNetsToCopySubNets.get(subnet);
                netCopy.add(copySubNet);
            }
            copy.localNetworks.put(jnet, netCopy);
        }
        // copy all lenodables: this sets pins of local networks.
        for (Iterator it = lenodables.values().iterator(); it.hasNext(); ) {
            LENodable leno = (LENodable)it.next();
            LENodable lenoCopy = leno.copy();
            copy.add(leno.getNodable(), lenoCopy);
        }
        copy.containsSizableGates = containsSizableGates;
        copy.contextFree = contextFree;
        //copy.allCachedNodables.addAll(allCachedNodables);
        return copy;
    }
}
