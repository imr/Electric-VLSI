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

import com.sun.electric.tool.logicaleffort.*;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.topology.*;
import com.sun.electric.database.prototype.*;
import com.sun.electric.database.variable.*;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ui.MessagesWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.technology.PrimitiveNode;

import java.awt.geom.AffineTransform;
import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.List;

/**
 * Creates a logical effort netlist to be sized by LESizer.
 * This is so the LESizer is independent of Electric's Database,
 * and can match George Chen's C++ version being developed for 
 * PNP.
 *
 * @author  gainsley
 */
public class LENetlister2 extends HierarchyEnumerator.Visitor implements LENetlister {

    /** Netlister constants */                  protected NetlisterConstants constants;
    /** Map of Cells to CachedCells */          private Map cellMap;
    /** Map of globalID's to LENetworks */      private Map globalNetworks;
    /** List of sizeable unique LENodables */   private List sizableLENodables;
    /** List of all unique LENodables */        private List allLENodables;
    /** Map of Nodables to LENodable definitions */     private Map nodablesDefinitions;

    /** Sizer */                                private LESizer2 sizer;
    /** Job we are part of */                   private Job job;
    /** Where to direct output */               private PrintStream out;

    /** True if we got aborted */               private boolean aborted;
    /** for logging errors */                   private ErrorLogger errorLogger;
    /** record definition errors so no multiple warnings */ private HashMap lePortError;

    private static final boolean DEBUG = false;
    private static final boolean DISABLE_CACHING = false;
    private static final boolean DEBUG_FIRSTPASS = false;
    private static final boolean DEBUG_PRINTCACHEDCELLS = false;

    public static final Variable.Key ATTR_su = ElectricObject.newKey("ATTR_su");
    public static final Variable.Key ATTR_le = ElectricObject.newKey("ATTR_le");

    protected static class NetlisterConstants {
        /** global step-up */                       protected float su;
        /** wire to gate cap ratio */               protected float wireRatio;
        /** convergence criteron */                 protected float epsilon;
        /** max number of iterations */             protected int maxIterations;
        /** gate cap, in fF/lambda */               protected float gateCap;
        /** ratio of diffusion to gate cap */       protected float alpha;
        /** ratio of keeper to driver size */       protected float keeperRatio;
    }

    /** Creates a new instance of LENetlister */
    public LENetlister2(Job job) {
        // get preferences for this package
        Tool leTool = Tool.findTool("logical effort");
        constants = new NetlisterConstants();
        constants.su = (float)LETool.getGlobalFanout();
        constants.epsilon = (float)LETool.getConvergenceEpsilon();
        constants.maxIterations = LETool.getMaxIterations();
        constants.gateCap = (float)LETool.getGateCapacitance();
        constants.wireRatio = (float)LETool.getWireRatio();
        constants.alpha = (float)LETool.getDiffAlpha();
        constants.keeperRatio = (float)LETool.getKeeperRatio();
        
        this.job = job;
        this.cellMap = new HashMap();
        this.globalNetworks = new HashMap();
        this.sizableLENodables = new ArrayList();
        this.allLENodables = new ArrayList();
        this.nodablesDefinitions = new HashMap();
        this.lePortError = new HashMap();
        this.out = new PrintStream((OutputStream)System.out);

        errorLogger = null;
        aborted = false;
    }
    
    // Entry point: This netlists the cell
    public void netlist(Cell cell, VarContext context) {

        //ArrayList connectedPorts = new ArrayList();
        //connectedPorts.add(Schematics.tech.resistorNode.getPortsList());
        if (errorLogger != null) errorLogger.delete();
        errorLogger = ErrorLogger.newInstance("LE Netlister");

        Netlist netlist = cell.getNetlist(true);
        
        // read schematic-specific sizing options
        for (Iterator instIt = cell.getNodes(); instIt.hasNext();) {
            NodeInst ni = (NodeInst)instIt.next();
            if (ni.getVar("ATTR_LESETTINGS") != null) {
                useLESettings(ni, context);            // get settings from object
                break;
            }
        }

        FirstPassEnum firstPass = new FirstPassEnum(this);
        HierarchyEnumerator.enumerateCell(cell, context, netlist, firstPass);
        firstPass.cleanup();
        System.out.println("Cached "+cellMap.size()+" cells");
        if (DEBUG_FIRSTPASS) {
            for (Iterator it = cellMap.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry)it.next();
                Cell acell = (Cell)entry.getKey();
                CachedCell cc = (CachedCell)entry.getValue();
                System.out.println("Cached cell "+acell.describe());
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
                for (Iterator it = cellMap.entrySet().iterator(); it.hasNext(); ) {
                    Map.Entry entry = (Map.Entry)it.next();
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
        HierarchyEnumerator.enumerateCell(cell, context, netlist, this);
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
    public void updateSizes() {
        // iterator over all LEGATEs
        for (Iterator cit = getSizeableNodables(); cit.hasNext(); ) {
            LENodable leno = (LENodable)cit.next();
            Nodable no = leno.getNodable();
            NodeInst ni = no.getNodeInst();
            if (ni != null) no = ni;

            // ignore it if not a sizeable gate
            if (!leno.isLeGate()) continue;
            String varName = "LEDRIVE_" + leno.getName();
            no.newVar(varName, new Float(leno.leX));

            if (leno.leX < 1.0f) {
                String msg = "WARNING: Instance "+ni.describe()+" has size "+TextUtils.formatDouble(leno.leX, 3)+" less than 1 ("+leno.getName()+")";
                System.out.println(msg);
                if (ni != null) {
                    ErrorLogger.MessageLog log = errorLogger.logError(msg, ni.getParent(), 2);
                    log.addGeom(ni, true, ni.getParent(), leno.context);
                }
            }
        }

        printStatistics();
        done();
    }

    public void done() {
        errorLogger.termLogging(true);
        errorLogger = null;
    }

    /** NodeInst should be an LESettings instance */
    private void useLESettings(NodeInst ni, VarContext context) {
        Variable var;
        if ((var = ni.getVar("ATTR_su")) != null) constants.su = VarContext.objectToFloat(context.evalVar(var), constants.su);
        if ((var = ni.getVar("ATTR_wire_ratio")) != null) constants.wireRatio = VarContext.objectToFloat(context.evalVar(var), constants.wireRatio);
        if ((var = ni.getVar("ATTR_epsilon")) != null) constants.epsilon = VarContext.objectToFloat(context.evalVar(var), constants.epsilon);
        if ((var = ni.getVar("ATTR_max_iter")) != null) constants.maxIterations = VarContext.objectToInt(context.evalVar(var), constants.maxIterations);
        if ((var = ni.getVar("ATTR_gate_cap")) != null) constants.gateCap = VarContext.objectToFloat(context.evalVar(var), constants.gateCap);
        if ((var = ni.getVar("ATTR_alpha")) != null) constants.alpha = VarContext.objectToFloat(context.evalVar(var), constants.alpha);
        if ((var = ni.getVar("ATTR_keeper_ratio")) != null) constants.keeperRatio = VarContext.objectToFloat(context.evalVar(var), constants.keeperRatio);
    }

    protected Iterator getSizeableNodables() { return sizableLENodables.iterator(); }

    protected float getGlobalSU() { return constants.su; }

    protected LESizer2 getSizer() { return sizer; }

    protected float getKeeperRatio() { return constants.keeperRatio; }

    private LENetwork getNetwork(int globalID, HierarchyEnumerator.CellInfo info) {
        LENetwork net = (LENetwork)globalNetworks.get(new Integer(globalID));
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
            CachedCell cachedCell = (CachedCell)netlister.cellMap.get(info.getCell());
            if (cachedCell == null) {
                cachedCell = new CachedCell(info.getCell(), info.getNetlist());
                netlister.cellMap.put(info.getCell(), cachedCell);
                if (DEBUG_FIRSTPASS) System.out.println(" === entering cell "+info.getCell().describe());
                return true;
            } else {
                // because this cell is already cached, we will not be visiting nodeinsts,
                // and we will not be calling exit cell. So link into parent here, because
                // we won't be linking into parent from exit cell.
                // add this to parent cached cell if any
                if (DEBUG_FIRSTPASS) System.out.println(" === not entering, using cached version for cell "+info.getCell().describe());
                HierarchyEnumerator.CellInfo parentInfo = info.getParentInfo();
                if (parentInfo != null) {
                    Cell parent = info.getParentInfo().getCell();
                    CachedCell parentCached = (CachedCell)netlister.cellMap.get(parent);
                    Nodable no = info.getParentInst();
                    parentCached.add(no, (LECellInfo)info.getParentInfo(), cachedCell, (LECellInfo)info, netlister.constants);
                }
            }
            return false;
        }

        public boolean visitNodeInst(Nodable ni, HierarchyEnumerator.CellInfo info) {
            CachedCell cachedCell = (CachedCell)netlister.cellMap.get(info.getCell());

            if (!(ni.getNodeInst().getProto() instanceof PrimitiveNode))
                if (DEBUG_FIRSTPASS) System.out.println(" === visiting "+ni.getName());
            // see if we can make an LENodable from the nodable
            LENodable.Type type = netlister.getType(ni, info);
            if (type == null) return true;                  // recurse
            if (type == LENodable.Type.IGNORE) return false;    // ignore
            LENodable leno = netlister.createLENodable(type, ni, info);
            // if no lenodable, recurse
            if (leno == null) return true;
            cachedCell.add(ni, leno);
            netlister.nodablesDefinitions.put(ni, leno);
            return false;
        }

        public void exitCell(HierarchyEnumerator.CellInfo info) {
            CachedCell cachedCell = (CachedCell)netlister.cellMap.get(info.getCell());

            if (DEBUG_FIRSTPASS) System.out.println(" === exiting cell "+info.getCell().describe());
            // add this to parent cached cell if any
            HierarchyEnumerator.CellInfo parentInfo = info.getParentInfo();
            if (parentInfo != null) {
                Cell parent = info.getParentInfo().getCell();
                CachedCell parentCached = (CachedCell)netlister.cellMap.get(parent);
                Nodable no = info.getParentInst();
                parentCached.add(no, (LECellInfo)info.getParentInfo(), cachedCell, (LECellInfo)info, netlister.constants);
            }
        }

        protected void cleanup() {
            // remove all cachedCells that contain sizeable gates or are not context free
            HashMap cachedMap = new HashMap();
            for (Iterator it = netlister.cellMap.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry)it.next();
                Cell cell = (Cell)entry.getKey();
                CachedCell cachedCell = (CachedCell)entry.getValue();
                if (cachedCell.isContextFree(netlister.constants)) {
                    cachedMap.put(cell, cachedCell);
                }
            }
            netlister.cellMap = cachedMap;
            if (DISABLE_CACHING) netlister.cellMap = new HashMap();
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
        leinfo.leInit();

        boolean enter = true;
        // if there is a cachedCell, do not enter
        CachedCell cachedCell = (CachedCell)cellMap.get(info.getCell());
        // if this was a cached cell, link cached networks into global network
        // note that a cached cell cannot by definition contain sizeable LE gates
        if ((cachedCell != null) && (leinfo.getMFactor() == 1f)) {
            for (Iterator it = cachedCell.getLocalNetworks().entrySet().iterator(); it.hasNext(); ) {
                Map.Entry entry = (Map.Entry)it.next();
                Network jnet = (Network)entry.getKey();
                LENetwork subnet = (LENetwork)entry.getValue();
                int globalID = info.getNetID(jnet);
                LENetwork net = (LENetwork)getNetwork(globalID, info);
                if (net == null) continue;
                net.add(subnet);
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

        LENodable def = (LENodable)nodablesDefinitions.get(ni);
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
            for (Iterator pit = uniqueLeno.getPins().iterator(); pit.hasNext(); ) {
                LEPin pin = (LEPin)pit.next();
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
    public static class LECellInfo extends HierarchyEnumerator.CellInfo {

        /** M-factor to be applied to size */       private float mFactor;
        /** SU to be applied to gates in cell */    private float cellsu;
        /** the cached cell */                      private CachedCell cachedCell;

        /** initialize LECellInfo: assumes CellInfo.init() has been called */
        protected void leInit() {

            HierarchyEnumerator.CellInfo parent = getParentInfo();

            // check for M-Factor from parent
            if (parent == null) mFactor = 1f;
            else mFactor = ((LECellInfo)parent).getMFactor();

            // check for su from parent
            if (parent == null) cellsu = -1f;
            else cellsu = ((LECellInfo)parent).getSU();

            // get info from node we pushed into
            Nodable ni = getContext().getNodable();
            if (ni == null) return;

            // get mfactor from instance we pushed into
            Variable mvar = LETool.getMFactor(ni);
            if (mvar != null) {
                Object mval = getContext().evalVar(mvar, null);
                if (mval != null)
                    mFactor = mFactor * VarContext.objectToFloat(mval, 1f);
            }

            // get su from instance we pushed into
            Variable suvar = ni.getVar(ATTR_su);
            if (suvar != null) {
                float su = VarContext.objectToFloat(getContext().evalVar(suvar, null), -1f);
                if (su != -1f) cellsu = su;
            }
        }
        
        /** get mFactor */
        protected float getMFactor() { return mFactor; }

        protected float getSU() { return cellsu; }

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
        if ((var = ni.getParameter("ATTR_LEGATE")) != null) {
            // assume it is LEGATE if can't resolve value
            int gate = VarContext.objectToInt(info.getContext().evalVar(var), 1);
            if (gate == 1)
                return LENodable.Type.LEGATE;
        }
        else if ((var = ni.getParameter("ATTR_LEKEEPER")) != null) {
            // assume it is LEKEEPER if can't resolve value
            int gate = VarContext.objectToInt(info.getContext().evalVar(var), 1);
            if (gate == 1)
                return LENodable.Type.LEKEEPER;
        }
        else if (ni.getParameter("ATTR_LEWIRE") != null) {
            return LENodable.Type.WIRE;
        }
        else if ((ni.getProto() != null) && (ni.getProto().getFunction().isTransistor())) {
            return LENodable.Type.TRANSISTOR;
        }
        else if ((ni.getProto() != null) && (ni.getProto().getFunction() == PrimitiveNode.Function.CAPAC)) {
            return LENodable.Type.CAPACITOR;
        }
        else if ((var = ni.getVar("ATTR_LEIGNORE")) != null) {
            int ignore = VarContext.objectToInt(info.getContext().evalVar(var), 1);
            if (ignore == 1)
                return LENodable.Type.IGNORE;
        }
        else if ((var = ni.getParameter("ATTR_LEIGNORE")) != null) {
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
        LENodable lenodable = new LENodable(ni, type, LETool.getMFactor(ni), ni.getParameter("ATTR_su"), ni.getParameter("ATTR_LEPARALLGRP"));
        Network outputNet = null;

		Netlist netlist = info.getNetlist();
		for (Iterator ppIt = ni.getProto().getPorts(); ppIt.hasNext();) {
			PortProto pp = (PortProto)ppIt.next();
            // Note: default 'le' value should be one
            float le = getLE(ni, type, pp, info);
            Network jnet = netlist.getNetwork(ni, pp, 0);
            LEPin.Dir dir = LEPin.Dir.INPUT;
            // if it's not an output, it doesn't really matter what it is.
            if (pp.getCharacteristic() == PortCharacteristic.OUT) {
                dir = LEPin.Dir.OUTPUT;
                // set output net
                if ((type == LENodable.Type.LEGATE || type == LENodable.Type.LEKEEPER) && outputNet != null) {
                    System.out.println("Error: Sizable gate "+ni.getNodeInst().describe()+" has more than one output port!! Ignoring Gate");
                    return null;
                }
                outputNet = jnet;
                lenodable.setOutputNet(jnet);
            }
            if (type == LENodable.Type.TRANSISTOR) {
                // primitive Electric Transistors have their source and drain set to BIDIR, we
                // want them set to OUTPUT so that they count as diffusion capacitance
                if (pp.getCharacteristic() == PortCharacteristic.BIDIR) dir = LEPin.Dir.OUTPUT;
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
        Variable var = ((Export)pp).getVar("ATTR_le");
        if (var != null) {
            leFound = true;
            le = VarContext.objectToFloat(info.getContext().evalVar(var), 1.0f);
        } else if ((pp.getCharacteristic() == PortCharacteristic.OUT) &&
                (type == LENodable.Type.LEGATE || type == LENodable.Type.LEKEEPER)) {
            // if this is an Sizeable gate's output, look for diffn and diffp
            float diff = 0;
            var = ((Export)pp).getVar("ATTR_diffn");
            if (var != null) {
                diff += VarContext.objectToFloat(info.getContext().evalVar(var), 0);
                leFound = true;
            }
            var = ((Export)pp).getVar("ATTR_diffp");
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
                String msg = "Warning: Sizeable gate has no logical effort specified for port "+pp.getName()+" in cell "+cell.describe();
                System.out.println(msg);
                ErrorLogger.MessageLog log = errorLogger.logError(msg, cell, 0);
                log.addExport(exp, true, cell, info.getContext().push(ni));
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
        for (Iterator cit = allLENodables.iterator(); cit.hasNext(); ) {
            LENodable leno = (LENodable)cit.next();
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

    public boolean printResults(Nodable no, VarContext context) {
        // if this is a NodeInst, convert to Nodable
        if (no instanceof NodeInst) {
            no = Netlist.getNodableFor((NodeInst)no, 0);
        }
        LENodable leno = null;
        for (Iterator it = allLENodables.iterator(); it.hasNext(); ) {
            LENodable aleno = (LENodable)it.next();
            if (aleno.getNodable() == no) {
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

        ArrayList gatesDrivenPins = new ArrayList();
        ArrayList loadsDrivenPins = new ArrayList();
        ArrayList wiresDrivenPins = new ArrayList();
        ArrayList gatesFightingPins = new ArrayList();

        if (outputNet == null) return false;
        for (Iterator it = outputNet.getAllPins().iterator(); it.hasNext(); ) {
            LEPin pin = (LEPin)it.next();
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
        for (Iterator it = gatesDrivenPins.iterator(); it.hasNext(); ) {
            LEPin pin = (LEPin)it.next(); totalLoad += pin.getInstance().printLoadInfo(pin, constants.alpha);
        }
        System.out.println("  -------------------- Loads Driven ("+loadsDrivenPins.size()+") --------------------");
        for (Iterator it = loadsDrivenPins.iterator(); it.hasNext(); ) {
            LEPin pin = (LEPin)it.next(); totalLoad += pin.getInstance().printLoadInfo(pin, constants.alpha);
        }
        System.out.println("  -------------------- Wires Driven ("+wiresDrivenPins.size()+") --------------------");
        for (Iterator it = wiresDrivenPins.iterator(); it.hasNext(); ) {
            LEPin pin = (LEPin)it.next(); totalLoad += pin.getInstance().printLoadInfo(pin, constants.alpha);
        }
        System.out.println("  -------------------- Gates Fighting ("+gatesFightingPins.size()+") --------------------");
        for (Iterator it = gatesFightingPins.iterator(); it.hasNext(); ) {
            LEPin pin = (LEPin)it.next(); totalLoad += pin.getInstance().printLoadInfo(pin, constants.alpha);
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
