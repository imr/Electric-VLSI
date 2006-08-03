/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LENetlister2.java
 * Written by Jonathan Gainsley, Sun Microsystems.
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
 *
 * Created on November 11, 2003, 3:56 PM
 */

package com.sun.electric.tool.logicaleffort;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.ErrorLogger;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Creates a logical effort netlist to be sized by LESizer.
 * This is so the LESizer is independent of Electric's Database,
 * and can match George Chen's C++ version being developed for 
 * PNP.
 *
 * @author  gainsley
 */
public class LENetlister2 extends LENetlister {

    /** Netlister constants */                  protected NetlisterConstants constants;
    /** Map of Cells to CachedCells */          private Map<Cell,CachedCell> cellMap;
    /** Map of globalID's to LENetworks */      private Map<Integer,LENetwork> globalNetworks;
    /** List of sizeable unique LENodables */   private List<LENodable> sizableLENodables;
    /** List of all unique LENodables */        private List<LENodable> allLENodables;
    /** Map of Nodables to LENodable definitions */     private Map<Nodable,LENodable> nodablesDefinitions;

    /** Sizer */                                private LESizer2 sizer;
    /** Job we are part of */                   private Job job;
    /** Where to direct output */               private PrintStream out;

    /** True if we got aborted */               private boolean aborted;
    /** for logging errors */                   private ErrorLogger errorLogger;
    /** record definition errors so no multiple warnings */ private HashMap<Export,Export> lePortError;
    /** The top level cell netlisted */         private Cell topLevelCell;
    /** whether or not to disable caching */    private boolean disableCaching = true;


    private static final boolean DEBUG = false;
    private static final boolean DEBUG_FIRSTPASS = false;
    private static final boolean DEBUG_PRINTCACHEDCELLS = false;

    /** Creates a new instance of LENetlister */
    public LENetlister2(Job job) {
        // get preferences for this package
        Tool leTool = Tool.findTool("logical effort");
        constants = null;
        topLevelCell = null;

        this.job = job;
        this.cellMap = new HashMap<Cell,CachedCell>();
        this.globalNetworks = new HashMap<Integer,LENetwork>();
        this.sizableLENodables = new ArrayList<LENodable>();
        this.allLENodables = new ArrayList<LENodable>();
        this.nodablesDefinitions = new HashMap<Nodable,LENodable>();
        this.lePortError = new HashMap<Export,Export>();
        this.out = new PrintStream((OutputStream)System.out);

        errorLogger = null;
        aborted = false;
    }
    
    // Entry point: This netlists the cell
    public boolean netlist(Cell cell, VarContext context, boolean useCaching) {

        disableCaching = !useCaching;
        //ArrayList connectedPorts = new ArrayList();
        //connectedPorts.add(Schematics.tech.resistorNode.getPortsList());
        assert errorLogger == null;
//        if (errorLogger != null) errorLogger.delete();
        errorLogger = ErrorLogger.newInstance("LE Netlister");

//        Netlist netlist = cell.getNetlist(true);
        
        // read schematic-specific sizing options
        constants = getSettings(cell);
        if (constants == null) {
            constants = new NetlisterConstants(cell.getTechnology());
            if (!saveSettings(constants, cell)) {
                // couldn't save settings to cell, abort
                return false;
            }
        }

        topLevelCell = cell;
        FirstPassEnum firstPass = new FirstPassEnum(this);
        HierarchyEnumerator.enumerateCell(cell, context, firstPass, true);
//        HierarchyEnumerator.enumerateCell(cell, context, netlist, firstPass);
        firstPass.cleanup(disableCaching);
        System.out.println("Cached "+cellMap.size()+" cells");
        if (DEBUG_FIRSTPASS) {
            for (Map.Entry<Cell,CachedCell> entry : cellMap.entrySet()) {
                Cell acell = (Cell)entry.getKey();
                CachedCell cc = (CachedCell)entry.getValue();
                System.out.println("Cached "+acell);
            }
        }
        if (DEBUG_PRINTCACHEDCELLS) {
            String outputFile = System.getProperty("user.dir") + File.separator + "PrintCachedCells.txt";
            try {
                FileOutputStream fos = new FileOutputStream(outputFile, false);
                BufferedOutputStream bout = new BufferedOutputStream(fos);
                PrintStream out2 = new PrintStream(bout);
                // redirect stderr to the log file
                //System.setErr(new PrintStream(bout, true));
                for (Map.Entry<Cell,CachedCell> entry : cellMap.entrySet()) {
                    Cell acell = (Cell)entry.getKey();
                    CachedCell cc = (CachedCell)entry.getValue();
                    cc.printContents("  ", out2);
                }
                out2.close();
                System.out.println("Wrote debug to "+outputFile);
            } catch (IOException e) {
                System.out.println("Cannot write CachedCells debug: "+e.getMessage());
            }
        }
        HierarchyEnumerator.enumerateCell(cell, context, this, true);
//        HierarchyEnumerator.enumerateCell(cell, context, netlist, this);
        if (aborted) return false;
        return true;
    }

    /**
     * Size the netlist.
     * @return true on success, false otherwise.
     */
    public boolean size(LESizer.Alg algorithm) {
        //lesizer.printDesign();
        boolean verbose = false;
        // create a new sizer
        sizer = new LESizer2(algorithm, this, job, errorLogger);
        boolean success = sizer.optimizeLoops(constants.epsilon, constants.maxIterations, verbose, constants.alpha, constants.keeperRatio);
        //out.println("---------After optimization:------------");
        //lesizer.printDesign();
        // get rid of the sizer
        sizer = null;
        return success;
    }

    /**
     * Updates the size of all Logical Effort gates
     */
    public void getSizes(List<Float> sizes, List<String> varNames,
                         List<NodeInst> nodes, List<VarContext> contexts) {
        // iterator over all LEGATEs
        for (Iterator<LENodable> cit = getSizeableNodables(); cit.hasNext(); ) {
            LENodable leno = cit.next();
            Nodable no = leno.getNodable();
            NodeInst ni = no.getNodeInst();
            if (ni != null) no = ni;

            // ignore it if not a sizeable gate
            if (!leno.isLeGate()) continue;
            String varName = "LEDRIVE_" + leno.getName();
            //no.newVar(varName, new Float(leno.leX));
            //topLevelCell.newVar(varName, new Float(leno.leX));
            sizes.add(new Float(leno.leX));
            varNames.add(varName);
            nodes.add(ni);
            contexts.add(leno.context);
        }
    }

    /**
     * Updates the size of all Logical Effort gates
     */
    public void updateSizes() {
        // iterator over all LEGATEs
        for (Iterator<LENodable> cit = getSizeableNodables(); cit.hasNext(); ) {
            LENodable leno = cit.next();
            Nodable no = leno.getNodable();
            NodeInst ni = no.getNodeInst();
            if (ni != null) no = ni;

            // ignore it if not a sizeable gate
            if (!leno.isLeGate()) continue;
            String varName = "LEDRIVE_" + leno.getName();
            //no.newVar(varName, new Float(leno.leX));
            topLevelCell.newVar(varName, new Float(leno.leX));

            if (leno.leX < 1.0f) {
                String msg = "WARNING: Instance "+ni+" has size "+TextUtils.formatDouble(leno.leX, 3)+" less than 1 ("+leno.getName()+")";
                System.out.println(msg);
                if (ni != null) {
                    errorLogger.logWarning(msg, ni, ni.getParent(), leno.context, 2);
                }
            }
        }
    }

    public void done() {
        errorLogger.termLogging(true);
        //errorLogger = null;
    }

    public ErrorLogger getErrorLogger() { return errorLogger; }

    public void nullErrorLogger() { errorLogger = null; }

    public NetlisterConstants getConstants() { return constants; }

    protected Iterator<LENodable> getSizeableNodables() { return sizableLENodables.iterator(); }

    protected float getGlobalSU() { return constants.su; }

    protected LESizer2 getSizer() { return sizer; }

    protected float getKeeperRatio() { return constants.keeperRatio; }

    private LENetwork getNetwork(int globalID, HierarchyEnumerator.CellInfo info) {
        LENetwork net = globalNetworks.get(new Integer(globalID));
        if (net == null) {
            String name = (info == null) ? null : info.getUniqueNetName(globalID, ".");
            net = new LENetwork(name);
            globalNetworks.put(new Integer(globalID), net);
        }
        return net;
    }

    // ======================= Hierarchy Enumerator ==============================

    /**
     * Class to implement the first pass of definitions for all LENodables.
     * The first pass creates the definitions for all LENodables, and sees which
     * Cells can be cached (i.e. do not have parameters that need parent context
     * to evaluate, and do not have sizeable gates in them)
     */
    private static class FirstPassEnum extends HierarchyEnumerator.Visitor {

        /** LENetlister2 */                 private LENetlister2 netlister;

        private FirstPassEnum(LENetlister2 netlister) {
            this.netlister = netlister;
        }

        /**
         * Override the default Cell info to pass along logical effort specific information
         * @return a LECellInfo
         */
        public HierarchyEnumerator.CellInfo newCellInfo() { return new LECellInfo(); }

        public boolean enterCell(HierarchyEnumerator.CellInfo info) {
            if (netlister.aborted) return false;

            if (((LETool.AnalyzeCell)netlister.job).checkAbort(null)) {
                netlister.aborted = true;
                return false;
            }
            CachedCell cachedCell = netlister.cellMap.get(info.getCell());
            if (cachedCell == null) {
                cachedCell = new CachedCell(info.getCell(), info.getNetlist());
                if (netlister.cellMap.containsKey(info.getCell()))
                    System.out.println("Possible hash map conflict in netlister.cellMap!");
                netlister.cellMap.put(info.getCell(), cachedCell);
                if (DEBUG_FIRSTPASS) System.out.println(" === entering "+info.getCell());
                return true;
            } else {
                // because this cell is already cached, we will not be visiting nodeinsts,
                // and we will not be calling exit cell. So link into parent here, because
                // we won't be linking into parent from exit cell.
                // add this to parent cached cell if any
                if (DEBUG_FIRSTPASS) System.out.println(" === not entering, using cached version for "+info.getCell());
                HierarchyEnumerator.CellInfo parentInfo = info.getParentInfo();
                if (parentInfo != null) {
                    Cell parent = info.getParentInfo().getCell();
                    CachedCell parentCached = netlister.cellMap.get(parent);
                    Nodable no = info.getParentInst();
                    parentCached.add(no, (LECellInfo)info.getParentInfo(), cachedCell, (LECellInfo)info, netlister.constants);
                }
            }
            return false;
        }

        public boolean visitNodeInst(Nodable ni, HierarchyEnumerator.CellInfo info) {
            CachedCell cachedCell = netlister.cellMap.get(info.getCell());

            if (ni.getNodeInst().isCellInstance())
                if (DEBUG_FIRSTPASS) System.out.println(" === visiting "+ni.getName());
            // see if we can make an LENodable from the nodable
            LENodable.Type type = netlister.getType(ni, info);
            if (type == null) return true;                  // recurse
            if (type == LENodable.Type.IGNORE) return false;    // ignore
            LENodable leno = netlister.createLENodable(type, ni, info);
            // if no lenodable, recurse
            if (leno == null) return true;
            cachedCell.add(ni, leno);
            if (netlister.nodablesDefinitions.containsKey(ni))
                System.out.println("Possible hash map conflict in netlister.nodablesDefinitions!");
            netlister.nodablesDefinitions.put(ni, leno);
            return false;
        }

        public void exitCell(HierarchyEnumerator.CellInfo info) {
            CachedCell cachedCell = netlister.cellMap.get(info.getCell());

            if (DEBUG_FIRSTPASS) System.out.println(" === exiting "+info.getCell());
            // add this to parent cached cell if any
            HierarchyEnumerator.CellInfo parentInfo = info.getParentInfo();
            if (parentInfo != null) {
                Cell parent = info.getParentInfo().getCell();
                CachedCell parentCached = netlister.cellMap.get(parent);
                Nodable no = info.getParentInst();
                parentCached.add(no, (LECellInfo)info.getParentInfo(), cachedCell, (LECellInfo)info, netlister.constants);
            }
        }

        protected void cleanup(boolean disableCaching) {
            // remove all cachedCells that contain sizeable gates or are not context free
            HashMap<Cell,CachedCell> cachedMap = new HashMap<Cell,CachedCell>();
            for (Map.Entry<Cell,CachedCell> entry : netlister.cellMap.entrySet()) {
                Cell cell = (Cell)entry.getKey();
                CachedCell cachedCell = (CachedCell)entry.getValue();
                if (cachedCell.isContextFree(netlister.constants)) {
                    cachedMap.put(cell, cachedCell);
                }
            }
            netlister.cellMap = cachedMap;
            if (disableCaching) netlister.cellMap = new HashMap<Cell,CachedCell>();
        }
    }

    /**
     * Override the default Cell info to pass along logical effort specific information
     * @return a LECellInfo
     */
    public HierarchyEnumerator.CellInfo newCellInfo() { return new LECellInfo(); }

    /**
     * Enter cell initializes the LECellInfo.
     * @param info the LECellInfo
     * @return true to process the cell, false to ignore.
     */
    public boolean enterCell(HierarchyEnumerator.CellInfo info) {
        if (aborted) return false;

        if (((LETool.AnalyzeCell)job).checkAbort(null)) {
            aborted = true;
            return false;
        }

        LECellInfo leinfo = (LECellInfo)info;
        leinfo.leInit(constants);

        // check if conflicting settings
        if (topLevelCell != info.getCell()) {
            if (isSettingsConflict(leinfo.getSettings(), topLevelCell, info.getContext(), info.getCell())) {
                aborted = true;
                return false;
            }
        }


        boolean enter = true;
        // if there is a cachedCell, do not enter
        CachedCell cachedCell = cellMap.get(info.getCell());
        // if this was a cached cell, link cached networks into global network
        // note that a cached cell cannot by definition contain sizeable LE gates
        if ((cachedCell != null) && (leinfo.getMFactor() == 1f)) {
            for (Map.Entry<Network,LENetwork> entry : cachedCell.getLocalNetworks().entrySet()) {
                Network jnet = (Network)entry.getKey();
                LENetwork subnet = (LENetwork)entry.getValue();
                int globalID = info.getNetID(jnet);
                LENetwork net = (LENetwork)getNetwork(globalID, info);
                if (net == null) continue;
                net.add(subnet);
                if (DEBUG) {
                    if (net.getName().equals("vdd")) continue;
                    if (net.getName().equals("gnd")) continue;
                    System.out.println("  Added to global net "+net.getName() +" "+subnet.getName()+" from "+info.getCell().describe(false));
                    System.out.println("     subcell="+info.getCell().describe(false)+" subnet="+subnet.getName()+": ");
                    subnet.print();
                    System.out.println("     result: global net="+net.getName()+": ");
                    net.print();
                }
            }
            //for (Iterator it = cachedCell.getAllCachedNodables().iterator(); it.hasNext(); ) {
            //   allLENodables.add(it.next());
            //}
            enter = false;
        }

        return enter;
    }

    /**
     * Visit NodeInst creates a new Logical Effort instance from the
     * parameters found on the Nodable, if that Nodable is an LEGATE.
     * It also creates instances for wire models (LEWIREs).
     * @param ni the Nodable being visited
     * @param info the cell info
     * @return true to push down into the Nodable, false to continue.
     */
    public boolean visitNodeInst(Nodable ni, HierarchyEnumerator.CellInfo info) {
        LECellInfo leinfo = (LECellInfo)info;

        LENodable def = nodablesDefinitions.get(ni);
        if (def == null) return true;
        else {
            // create hierarchical unique instance from definition
            LENetwork outputNet = null;
            if (def.isLeGate()) {
                // get global output network
                Network outNet = def.getOutputNet();
                int globalID = info.getNetID(outNet);
                outputNet = getNetwork(globalID, info);
            }
            float localsu = constants.su;
            if (leinfo.getSU() != -1f) localsu = leinfo.getSU();
            LENodable uniqueLeno = def.createUniqueInstance(info.getContext(), outputNet,
                    leinfo.getMFactor(), localsu, constants);
            if (uniqueLeno.isLeGate())
                sizableLENodables.add(uniqueLeno);
            allLENodables.add(uniqueLeno);
            // add pins to global networks
            for (LEPin pin : uniqueLeno.getPins()) {
                int globalID = info.getNetID(pin.getNetwork());
                LENetwork net = getNetwork(globalID, info);
                net.add(pin);
            }
            //uniqueLeno.print();
            //uniqueLeno.printPins();
        }
        return false;
    }

    public void doneVisitNodeInst(Nodable ni, HierarchyEnumerator.CellInfo info) {}

    /**
     * Nothing to do for exitCell
     */
    public void exitCell(HierarchyEnumerator.CellInfo info) {

    }

    /**
     * Logical Effort Cell Info class.  Keeps track of:
     * <p>- M factors
     */
    public static class LECellInfo extends LENetlister.LECellInfo {

        /** the cached cell */                      private CachedCell cachedCell;

        protected void setCachedCell(CachedCell c) { cachedCell = c; }
        protected CachedCell getCachedCell() { return cachedCell; }
    }


    /**
     * Get the LENodable type of this Nodable. If it is not a valid type, return null.
     * @param ni the Nodable to examine
     * @param info the current info
     * @return the LENodable type, or null if not an LENodable
     */
    private LENodable.Type getType(Nodable ni, HierarchyEnumerator.CellInfo info) {

        Variable var = null;
        if ((var = ni.getParameter(ATTR_LEGATE)) != null) {
            // assume it is LEGATE if can't resolve value
            int gate = VarContext.objectToInt(info.getContext().evalVar(var), 1);
            if (gate == 1)
                return LENodable.Type.LEGATE;
        }
        else if ((var = ni.getParameter(ATTR_LEKEEPER)) != null) {
            // assume it is LEKEEPER if can't resolve value
            int gate = VarContext.objectToInt(info.getContext().evalVar(var), 1);
            if (gate == 1)
                return LENodable.Type.LEKEEPER;
        }
        else if (ni.getParameter(ATTR_LEWIRE) != null) {
            return LENodable.Type.WIRE;
        }
        else if ((ni.getProto() != null) && (ni.getProto().getFunction().isTransistor())) {
            return LENodable.Type.TRANSISTOR;
        }
        else if ((ni.getProto() != null) && (ni.getProto().getFunction() == PrimitiveNode.Function.CAPAC)) {
            return LENodable.Type.CAPACITOR;
        }
        else if ((var = ni.getVar(ATTR_LEIGNORE)) != null) {
            int ignore = VarContext.objectToInt(info.getContext().evalVar(var), 1);
            if (ignore == 1)
                return LENodable.Type.IGNORE;
        }
        else if ((var = ni.getParameter(ATTR_LEIGNORE)) != null) {
            int ignore = VarContext.objectToInt(info.getContext().evalVar(var), 1);
            if (ignore == 1)
                return LENodable.Type.IGNORE;
        }

        return null;
    }

    /**
     * Create an LENodable of the given type for the Nodable
     * @param type the type to create
     * @param ni the source nodable
     * @param info the current info
     * @return an LENodable, or null if error
     */
    private LENodable createLENodable(LENodable.Type type, Nodable ni, HierarchyEnumerator.CellInfo info) {
        if (type == null) return null;
        Variable var = null;

        if (DEBUG) System.out.println("------------------------------------");

        // Build an LENodable. M can be variable or parameter
        LENodable lenodable = new LENodable(ni, type, LETool.getMFactor(ni), ni.getParameter(ATTR_su), ni.getParameter(ATTR_LEPARALLGRP));
        Network outputNet = null;

		Netlist netlist = info.getNetlist();
		for (Iterator<PortProto> ppIt = ni.getProto().getPorts(); ppIt.hasNext();) {
			PortProto pp = ppIt.next();
            // Note: default 'le' value should be one
            float le = getLE(ni, type, pp, info);
            Network jnet = netlist.getNetwork(ni, pp, 0);
            LEPin.Dir dir = LEPin.Dir.INPUT;
            // if it's not an output, it doesn't really matter what it is.
            if (pp.getCharacteristic() == PortCharacteristic.OUT) {
                dir = LEPin.Dir.OUTPUT;
                // set output net
                if ((type == LENodable.Type.LEGATE || type == LENodable.Type.LEKEEPER) && outputNet != null) {
                    System.out.println("Error: Sizable gate "+ni.getNodeInst()+" has more than one output port!! Ignoring Gate");
                    return null;
                }
                outputNet = jnet;
                lenodable.setOutputNet(jnet);
            }
            if (type == LENodable.Type.TRANSISTOR) {
                // primitive Electric Transistors have their source and drain set to BIDIR, we
                // want them set to OUTPUT so that they count as diffusion capacitance
                if (pp.getCharacteristic() == PortCharacteristic.BIDIR) dir = LEPin.Dir.OUTPUT;
                if (dir == LEPin.Dir.INPUT) {
                    // gate load: check if length > 2, if so, increase LE to account for added capacitance
                    var = ni.getVar(Schematics.ATTR_LENGTH);
                    if (var == null) {
                        System.out.println("Error: transistor "+ni.getName()+" has no length in Cell "+ni.getParent());
                        //ErrorLogger.ErrorLog log = errorLogger.logError("Error: transistor "+ni+" has no length in Cell "+info.getCell(), info.getCell(), 0);
                        //log.addGeom(ni.getNodeInst(), true, info.getCell(), info.getContext());
                    }
                    float length = VarContext.objectToFloat(info.getContext().evalVar(var), (float)2.0);
                    // not exactly correct because assumes all cap is area cap, which it isn't
                    if (length != 2.0f)
                        le = le * length / 2.0f;                    
                }
            }
            lenodable.addPort(pp.getName(), dir, le, jnet);
            if (DEBUG) System.out.println("    Added "+dir+" pin "+pp.getName()+", le: "+le+", Network: "+jnet);
            if (type == LENodable.Type.WIRE) break;    // this is LEWIRE, only add one pin of it
        }

        return lenodable;
    }

    private float getLE(Nodable ni, LENodable.Type type, PortProto pp, HierarchyEnumerator.CellInfo info) {
        boolean leFound = false;
        // Note default 'le' value should be one
        float le = 1.0f;
		if (!(pp instanceof Export))
			return le;
        Variable var = ((Export)pp).getVar(ATTR_le);
        if (var != null) {
            leFound = true;
            le = VarContext.objectToFloat(info.getContext().evalVar(var), 1.0f);
        } else if ((pp.getCharacteristic() == PortCharacteristic.OUT) &&
                (type == LENodable.Type.LEGATE || type == LENodable.Type.LEKEEPER)) {
            // if this is an Sizeable gate's output, look for diffn and diffp
            float diff = 0;
            var = ((Export)pp).getVar(ATTR_diffn);
            if (var != null) {
                diff += VarContext.objectToFloat(info.getContext().evalVar(var), 0);
                leFound = true;
            }
            var = ((Export)pp).getVar(ATTR_diffp);
            if (var != null) {
                diff += VarContext.objectToFloat(info.getContext().evalVar(var), 0);
                leFound = true;
            }
            le = diff/3.0f;
        }
        if (!leFound && (type == LENodable.Type.LEGATE || type == LENodable.Type.LEKEEPER)) {
            Cell cell = (Cell)ni.getProto();
            Export exp = cell.findExport(pp.getName());
            if (exp != null && lePortError.get(exp) == null) {
                String msg = "Warning: Sizeable gate has no logical effort specified for port "+pp.getName()+" in "+cell;
                System.out.println(msg);
                errorLogger.logWarning(msg, exp, cell, info.getContext().push(ni), 0);
                lePortError.put(exp, exp);
            }
        }
        return le;
    }

    // =============================== Statistics ==================================


    public void printStatistics() {
        float totalsize = 0f;
        float instsize = 0f;
        int numLEGates = 0;
        int numLEWires = 0;
        // iterator over all LEGATEs
        for (LENodable leno : allLENodables) {
            // ignore it if not a sizeable gate
            if (leno.isLeGate()) {
                numLEGates++;
                instsize += leno.leX;
            }
            if (leno.getType() == LENodable.Type.WIRE)
                numLEWires++;
            totalsize += leno.leX;
        }
        System.out.println("Number of LEGATEs: "+numLEGates);
        //System.out.println("Number of Wires: "+numLEWires);
        System.out.println("Total size of all LEGATEs: "+instsize);
        //System.out.println("Total size of all instances (sized and loads): "+totalsize);
    }

    public float getTotalLESize() {
        float instsize = 0f;
        // iterator over all LEGATEs
        for (LENodable leno : allLENodables) {
            // ignore it if not a sizeable gate
            if (leno.isLeGate()) {
                instsize += leno.leX;
            }
        }
        return instsize;
    }

    public boolean printResults(Nodable no, VarContext context) {
        // if this is a NodeInst, convert to Nodable
        if (no instanceof NodeInst) {
            no = Netlist.getNodableFor((NodeInst)no, 0);
        }
        LENodable leno = null;
        for (LENodable aleno : allLENodables) {
            if (aleno.getNodable().getNodeInst() == no.getNodeInst()) {
                if (aleno.context.getInstPath(".").equals(context.getInstPath("."))) {
                    leno = aleno;
                    break;
                }
            }
        }
        if (leno == null) return false;

        // print netlister info
        System.out.println("Netlister: Gate Cap="+constants.gateCap+", Alpha="+constants.alpha);

        // print instance info
        leno.print();

        // collect info about what is driven
        LENetwork outputNet = leno.outputNetwork;

        ArrayList<LEPin> gatesDrivenPins = new ArrayList<LEPin>();
        ArrayList<LEPin> loadsDrivenPins = new ArrayList<LEPin>();
        ArrayList<LEPin> wiresDrivenPins = new ArrayList<LEPin>();
        ArrayList<LEPin> gatesFightingPins = new ArrayList<LEPin>();

        if (outputNet == null) return false;
        for (LEPin pin : outputNet.getAllPins()) {
            LENodable loopLeno = pin.getInstance();
            if (pin.getDir() == LEPin.Dir.INPUT) {
                if (loopLeno.isGate()) gatesDrivenPins.add(pin);
                if (loopLeno.getType() == LENodable.Type.LOAD) loadsDrivenPins.add(pin);
                if (loopLeno.getType() == LENodable.Type.TRANSISTOR) loadsDrivenPins.add(pin);
                if (loopLeno.getType() == LENodable.Type.CAPACITOR) loadsDrivenPins.add(pin);
                if (loopLeno.getType() == LENodable.Type.WIRE) wiresDrivenPins.add(pin);
            }
            if (pin.getDir() == LEPin.Dir.OUTPUT) {
                if (loopLeno.isGate()) gatesFightingPins.add(pin);
                if (loopLeno.getType() == LENodable.Type.TRANSISTOR) loadsDrivenPins.add(pin);
            }
        }
        System.out.println("Note: Load = Size * LE * M");
        System.out.println("Note: Load = Size * LE * M * Alpha, for Gates Fighting");

        float totalLoad = 0f;
        System.out.println("  -------------------- Gates Driven ("+gatesDrivenPins.size()+") --------------------");
        for (LEPin pin : gatesDrivenPins) {
            totalLoad += pin.getInstance().printLoadInfo(pin, constants.alpha);
        }
        System.out.println("  -------------------- Loads Driven ("+loadsDrivenPins.size()+") --------------------");
        for (LEPin pin : loadsDrivenPins) {
            totalLoad += pin.getInstance().printLoadInfo(pin, constants.alpha);
        }
        System.out.println("  -------------------- Wires Driven ("+wiresDrivenPins.size()+") --------------------");
        for (LEPin pin : wiresDrivenPins) {
            totalLoad += pin.getInstance().printLoadInfo(pin, constants.alpha);
        }
        System.out.println("  -------------------- Gates Fighting ("+gatesFightingPins.size()+") --------------------");
        for (LEPin pin : gatesFightingPins) {
            totalLoad += pin.getInstance().printLoadInfo(pin, constants.alpha);
        }
        System.out.println("*** Total Load: "+TextUtils.formatDouble(totalLoad, 2));
        //msgs.setFont(oldFont);
        return true;
    }

    // ---- TEST STUFF -----  REMOVE LATER ----
    public static void test1() {
        LESizer.test1();
    }
    
}
