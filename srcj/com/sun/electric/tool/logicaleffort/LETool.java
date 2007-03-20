/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LETool.java
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
 * Created on November 17, 2003, 10:16 AM
 */

package com.sun.electric.tool.logicaleffort;

import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.Setting;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.EvalJavaBsh;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.generator.sclibrary.SCLibraryGen;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.simulation.Simulation;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is the Logical Effort Tool.  It doesn't actually do
 * any work itself, but acts as a public API for all of the
 * logical effort tool functionality.
 *
 * @author  gainsley
 */
public class LETool extends Tool {
    
    /** The Logical Effort tool */              private static LETool tool = new LETool();

    private static final boolean DEBUG = false;

    /** Creates a new instance of LETool */
    private LETool() {
        super("logeffort");
    }

    /**
     * Method to retrieve the singleton associated with the LETool tool.
     * @return the LETool tool.
     */
    public static LETool getLETool() {
        return tool;
    }

    /** Initialize tool - add calls to Bean Shell Evaluator */
    public void init() {
		EvalJavaBsh.evalJavaBsh.setVariable("LE", tool);
   }


    // =========================== Java Parameter Evaluation ======================

    /**
     * Grabs a logical effort calculated size from the instance.
     * @return the size.
     */
    public Object getdrive() throws VarContext.EvalException {

        // info should be the node on which there is the variable with the getDrive() call
        Object info = EvalJavaBsh.evalJavaBsh.getCurrentInfo();
        if (!(info instanceof Nodable))
            throw new VarContext.EvalException("getdrive(): Not enough hierarchy");
        VarContext context = EvalJavaBsh.evalJavaBsh.getCurrentContext();
        if (context == null)
            throw new VarContext.EvalException("getdrive(): null VarContext");
        Nodable ni = (Nodable)info;

        // Try to find drive strength
        // if Nodeinst, get different sizes if arrayed
        Object val = null;
        if ( (ni instanceof NodeInst) && (ni.getNameKey().busWidth() > 1)) {
            Name name = ni.getNameKey();
            ArrayList<Object> sizes = new ArrayList<Object>();
            for (int i=0; i<name.busWidth(); i++) {
                Nodable no = Netlist.getNodableFor((NodeInst)ni, i);
                Variable var = getLEDRIVE(ni, context.push(no));
                Object size = null;
                if (var != null) size = var.getObject();
                sizes.add(size);
            }
            if (sizes.size() > 5) {
                Object [] objs = new Object[3];
                objs[0] = sizes.get(0);
                objs[1] = (Object)"...";
                objs[2] = sizes.get(sizes.size()-1);
                val = objs;
            } else {
                val = sizes.toArray();
            }
        } else {
            Variable var = getLEDRIVE(ni, context.push(ni));
            if (var == null) {
                // none found, try to find drive strength using old format from C-Electric
                var = getLEDRIVE_old(ni, context);
            }
            //if (var == null) return "No variable "+ledrive;
            if (var == null)
                throw new VarContext.EvalException("getdrive(): no size");
            val = var.getObject();
        }
        if (val == null)
            throw new VarContext.EvalException("getdrive(): size null");
        return val;
    }

    /**
     * Grab a paramter 'parName' from a nodeInst 'nodeName' in a sub cell.
     * @param nodeName name of the nodeInst
     * @param parName name of parameter to evaluate
     * @return the parameter.
     */
    public Object subdrive(String nodeName, String parName) throws VarContext.EvalException {

        // info should be the node on which there is the variable with the subDrive() call
        Object info = EvalJavaBsh.evalJavaBsh.getCurrentInfo();
        if (!(info instanceof Nodable)) throw new VarContext.EvalException("subdrive(): Not enough hierarchy information");
        Nodable no = (Nodable)info;                                 // this inst has LE.subdrive(...) on it
        if (no == null)
            throw new VarContext.EvalException("subdrive(): Not enough hierarchy");

        if (no instanceof NodeInst) {
            // networks have not been evaluated, calling no.getProto()
            // is going to give us icon cell, not equivalent schematic cell
            // We need to re-evaluate networks to get equivalent schematic cell
            NodeInst ni = (NodeInst)no;
            Cell parent = no.getParent();                               // Cell in which inst which has LE.subdrive is
            if (parent == null)
                throw new VarContext.EvalException("subdrive(): null parent");
			int arrayIndex = 0;                                         // just use first index
            no = Netlist.getNodableFor(ni, arrayIndex);
            if (no == null)
                throw new VarContext.EvalException("subdrive(): can't get equivalent schematic");
        }

        VarContext context = EvalJavaBsh.evalJavaBsh.getCurrentContext();  // get current context
        if (context == null)
            throw new VarContext.EvalException("subdrive(): null context");

        NodeProto np = no.getProto();                               // get contents of instance
        if (np == null)
            throw new VarContext.EvalException("subdrive(): null nodeProto");
        if (!no.isCellInstance())
            throw new VarContext.EvalException("subdrive(): NodeProto not a Cell");
        Cell cell = (Cell)np;

        NodeInst ni = cell.findNode(nodeName);                      // find nodeinst that has variable on it
        if (ni == null) {
            // try converting to JElectric default name
            ni = cell.findNode(convertToJElectricDefaultName(nodeName));
            if (ni == null)
                throw new VarContext.EvalException("subdrive(): no nodeInst named "+nodeName);
        }

        Variable var = ni.getVar(parName);                          // find variable on nodeinst
        if (var == null) var = ni.getVar("ATTR_"+parName);          // maybe it's an attribute
        //if (var == null) return "subdrive(): no variable of name "+parName.replaceFirst("ATTR_", "");
        if (var == null)
            throw new VarContext.EvalException(parName.replaceFirst("ATTR_", "")+" not found");
        return context.push(no).evalVarRecurse(var, ni);                       // evaluate variable and return it
    }

    /**
     * Attempt to get old style LEDRIVE off of <CODE>no</CODE> based
     * on the VarContext <CODE>context</CODE>.
     * Attemps to compensate for the situation when the user
     * had added extra hierarchy to the top of the hierarchy.
     * It cannot compensate for the user has less hierarchy than
     * is required to create the correct Variable name.
     * @param no nodable on which LEDRIVE_ var exists
     * @param context context of <CODE>no</CODE>
     * @return a variable if found, null otherwise
     */
    private Variable getLEDRIVE_old(Nodable no, VarContext context) {
        String drive = makeDriveStrOLDRecurse(context);
        Variable var = null;
        while (!drive.equals("")) {
            if (DEBUG) System.out.println("  Looking for: LEDRIVE_"+drive+";0;S");
            Variable.Key key = Variable.findKey("LEDRIVE_"+drive+";0;S");
            var = (key != null ? no.getVar(key) : null);
            if (var != null) return var;            // look for var
            int i = drive.indexOf(';');
            if (i == -1) break;
            drive = drive.substring(i+1);             // remove top level of hierarchy
        }
        // didn't find it: try converting new default names to old default style names
        

        // look for it at current level
        if (DEBUG) System.out.println("  Looking for: LEDRIVE_0;S");
        var = no.getVar(Variable.newKey("LEDRIVE_0;S"));
        if (var != null) return var;            // look for var
        return null;
    }

    /**
     * Attempt to get LEDRIVE off of <CODE>no</CODE> based
     * on the VarContext <CODE>context</CODE>.
     * @param no the nodable for which we want the size
     * @param context the context
     * @return a variable if found, null otherwise
     */
    private Variable getLEDRIVE(Nodable no, VarContext context) {
        // try the top level cell way
        Variable var = null;
        var = getLEDRIVEtop(no, context);
        // try the old way (on leaf cells) if none found
        if (var == null)
            var = getLEDRIVEleaf(no, context);
        return var;
    }

    private Variable getLEDRIVEtop(Nodable no, VarContext context) {
        String drive = context.getInstPath(".");
        Nodable topno = no;
        while (context != VarContext.globalContext) {
            topno = context.getNodable();
            context = context.pop();
        }
        Cell parent = topno.getParent();
        Variable.Key key = Variable.findKey("LEDRIVE_"+drive);
        if (key == null) return null;
        Variable var = parent.getVar(key);
        return var;
    }

    /**
     * Attempt to get LEDRIVE off of <CODE>no</CODE> based
     * on the VarContext <CODE>context</CODE>.
     * Attemps to compensate for the situation when the user
     * had added extra hierarchy to the top of the hierarchy.
     * It cannot compensate for the user has less hierarchy than
     * is required to create the correct Variable name.
     * @param no nodable on which LEDRIVE_ var exists
     * @param context context of <CODE>no</CODE>
     * @return a variable if found, null otherwise
     */
    private Variable getLEDRIVEleaf(Nodable no, VarContext context) {
        String drive = context.getInstPath(".");
        Variable var = null;
        while (!drive.equals("")) {
            if (DEBUG) System.out.println("  Looking for: LEDRIVE_"+drive);
            Variable.Key key = Variable.findKey("LEDRIVE_"+drive);
            var = (key != null ? no.getVar(key) : null);
            if (var != null) return var;            // look for var
            int i = drive.indexOf('.');
            if (i == -1) return null;
            drive = drive.substring(i+1);             // remove top level of hierarchy
        }
        return null;
    }

    /**
     * Makes a string denoting hierarchy path.
     * @param context var context of node
     * @return  a string denoting hierarchical path of node
     */
    private static String makeDriveStr(VarContext context) {
        return "LEDRIVE_" + context.getInstPath(".");
    }

    /**
     * Makes a string denoting hierarchy path.
     * This is the old version compatible with Java Electric.
     * @param context var context of node
     * @return  a string denoting hierarchical path of node
     */
    private static String makeDriveStrOLD(VarContext context) {
        String s = "LEDRIVE_"+makeDriveStrOLDRecurse(context)+";0;S";
        //System.out.println("name is "+s);
        return s;
    }

    private static String makeDriveStrOLDRecurse(VarContext context) {
        if (context == VarContext.globalContext) return "";

        String prefix = context.pop() == VarContext.globalContext ? "" : makeDriveStrOLDRecurse(context.pop());
        Nodable no = context.getNodable();
        if (no == null) {
            System.out.println("VarContext.getInstPath: context with null NodeInst?");
        }
        String me;
        String name = getCElectricDefaultName(no);
        me = name + ",0";

        if (prefix.equals("")) return me;
        return prefix + ";" + me;
    }

    private static String getCElectricDefaultName(Nodable no) {
        String name = no.getNodeInst().getName();
        int at = name.indexOf('@');
        if (at != -1) {
            // convert to old default name style if possible
            if ((at+1) < name.length()) {
                String num = name.substring(at+1, name.length());
                try {
                    Integer i = new Integer(num);
                    name = name.substring(0, at) + (i.intValue()+1);
                } catch (NumberFormatException e) {}
            }
        }
        return name;
    }

    private static final Pattern celecDefaultNamePattern = Pattern.compile("^(\\D+)(\\d+)$");

    private static String convertToJElectricDefaultName(String celectricDefaultName) {
        Matcher mat = celecDefaultNamePattern.matcher(celectricDefaultName);
        if (mat.matches()) {
            try {
                Integer i = new Integer(mat.group(2));
                int ii = i.intValue() - 1;
                if (ii >= 0) {
                    celectricDefaultName = mat.group(1) + "@" + ii;
                }
            } catch (NumberFormatException e) {}
        }
        return celectricDefaultName;
    }

    protected static Variable getMFactor(Nodable no) {
        Variable var = no.getVar(Simulation.M_FACTOR_KEY);
        if (var == null) var = no.getParameter(Simulation.M_FACTOR_KEY);
        return var;
    }

    /**
     * Quantize gate sizes so that the maximum error is less than or equal
     * to 'error'.  This result always returns a whole integer, unless
     * the number is less than or equal to the minValue.
     * @param d the number to quantize
     * @param error the percentage error as a number, so 0.1 for 10%
     * @param minValue the minimum allowed value for the return value
     * @return a quantized value for d
     */
    public static double quantize(double d, double error, double minValue) {
        if (d <= minValue) return minValue;

        // (1+error)^power = dd; dd is the quanitized value of d
        double power = Math.log10(d)/Math.log10(1+error);
        long p = Math.round(power);
        long quan = Math.round( Math.pow(1+error, p));
        return (double)quan;
    }

    // ============================== Menu Commands ===================================


    /**
     * Optimizes a Cell containing logical effort gates for equal gate delays.
     * @param cell the cell to be sized
     * @param context varcontext of the cell
     */
    public void optimizeEqualGateDelays(Cell cell, VarContext context, boolean newAlg) {
        AnalyzeCell acjob = new AnalyzeCell(LESizer.Alg.EQUALGATEDELAYS, cell, context, newAlg);
        acjob.startJob(true, false);
    }

    private static AnalyzeCell lastLEJobExecuted = null;

    /**
     * Performs a cell analysis. The algorithm argument tells the LESizer how to size
     * the netlist generated by LENetlist.
     */
    public static class AnalyzeCell extends Job
    {
        /** progress */                         private String progress;
        /** cell to analyze */                  private Cell cell;
        /** var context */                      private VarContext context;
        /** algorithm type */                   private LESizer.Alg algorithm;
        /** netlist */                          private LENetlister netlister;
        private boolean newAlg;

        public AnalyzeCell(LESizer.Alg algorithm, Cell cell, VarContext context, boolean newAlg) {
            super("Analyze "+cell, tool, Job.Type.EXAMINE, null, cell, Job.Priority.USER);
            progress = null;
            this.algorithm = algorithm;
            this.cell = cell;
            this.context = context;
            this.newAlg = newAlg;
        }
        
        public boolean doIt() throws JobException {
            // delete last job, if any
            if (lastLEJobExecuted != null)
                lastLEJobExecuted.remove();
            lastLEJobExecuted = this;

            setProgress("building equations");
            System.out.print("Building equations...");

            // sleep for testing purposes only, remove later
//            try {
//                boolean donesleeping = false;
//                while(!donesleeping) {
//                    Thread.sleep(1);
//                    donesleeping = true;
//                }
//            } catch (InterruptedException e) {}

            // get sizer and netlister
            if (newAlg)
                netlister = new LENetlister2(this);
            else
                netlister = new LENetlister1(this);
            boolean success = netlister.netlist(cell, context, true);
            if (!success) return false;

            // calculate statistics
            long equationsDone = System.currentTimeMillis();
            String elapsed = TextUtils.getElapsedTime(equationsDone-startTime);
            System.out.println("done ("+elapsed+")");

            // if user aborted, return, and do not run sizer
            if (checkAbort(null)) {
                netlister.done();
                return false;
            }

            System.out.println("Starting iterations: ");
            setProgress("iterating");
            boolean success2 = netlister.size(algorithm);

            // if user aborted, return, and do not update sizes
            if (checkAbort(null)) {
                netlister.done();
                return false;
            }

            if (success2) {
                System.out.println("Sizing finished, updating sizes...");
                netlister.printStatistics();
                List<Float> sizes = new ArrayList<Float>();
                List<String> varNames = new ArrayList<String>();
                List<NodeInst> nodes = new ArrayList<NodeInst>();
                List<VarContext> contexts = new ArrayList<VarContext>();
                netlister.getSizes(sizes, varNames, nodes, contexts);

                // check for small sizes
                for (int i=0; i<sizes.size(); i++) {
                    float f = sizes.get(i).floatValue();
                    NodeInst ni = nodes.get(i);
                    VarContext context = contexts.get(i);

                    if (f < 1.0f) {
                        String msg = "WARNING: Instance "+ni+" has size "+TextUtils.formatDouble(f, 3)+" less than 1";
                        System.out.println(msg);
                        if (ni != null) {
                            netlister.getErrorLogger().logWarning(msg, ni, ni.getParent(), context, 2);
                        }
                    }
                }
                new UpdateSizes(sizes, varNames, nodes, contexts, cell, 0.10);
                netlister.getErrorLogger().termLogging(true);
                //netlister.nullErrorLogger();
            } else {
                System.out.println("Sizing failed, sizes unchanged");
                netlister.done();
            }
			return true;
       }

        /**
         * Check if we are scheduled to abort. If so, print msg if non null
         * and return true.
         * @param msg message to print if we are aborted
         * @return true on abort, false otherwise
         */
        protected boolean checkAbort(String msg) {
            if (getScheduledToAbort()) {
                if (msg != null) System.out.println("LETool aborted: "+msg);
                else System.out.println("LETool aborted: no changes made");
                setAborted();                   // Job has been aborted
                return true;
            }
            return false;
        }

        // add more info to default getInfo
        public String getInfo() {

            StringBuffer buf = new StringBuffer();
            buf.append(super.getInfo());
            if (getScheduledToAbort())
                buf.append("  Job aborted, no changes made\n");
            else {
                buf.append("  Job completed successfully\n");
            }
            return buf.toString();
        }

        public LENetlister getNetlister() { return netlister; }
    }

    private static class UpdateSizes extends Job {

        private List<Float> sizes;
        private List<String> varNames;
        private List<NodeInst> nodes;
        private List<VarContext> contexts;
        private Cell cell;
        private double standardCellErrorTolerancePrint = 0.10;

        private UpdateSizes(List<Float> sizes, List<String> varNames, List<NodeInst> nodes,
                            List<VarContext> contexts, Cell cell, double standardCellErrorTolerancePrint) {
            super("Update LE Sizes", tool, Job.Type.CHANGE, null, cell, Job.Priority.USER);
            this.sizes = sizes;
            this.varNames = varNames;
            this.nodes = nodes;
            this.contexts = contexts;
            this.cell = cell;
            this.standardCellErrorTolerancePrint = standardCellErrorTolerancePrint;
            startJob();
        }

        public boolean doIt() throws JobException {
            // handle changed standard cells

            UniqueNodeMap standardCellNodeMap = new UniqueNodeMap();
            boolean standardCellArrayError = false;

            System.out.println("Standard cell quantization errors over "+
                    (standardCellErrorTolerancePrint*100)+"%:");
            StringBuffer errBuf = new StringBuffer();
            for (int i=0; i<sizes.size(); i++) {
                NodeInst ni = nodes.get(i);
                VarContext context = contexts.get(i);
                Float f = sizes.get(i);
                Cell standardCell = getStandardCell(ni, context, f.doubleValue());
                if (standardCell != null) {
                    VarContext nicontext = context.push(ni);
                    Cell previousStandardCell = standardCellNodeMap.get(nicontext);
                    if (previousStandardCell != null && previousStandardCell != standardCell.iconView()) {
                        // we have an arrayed NodeInst that has been assigned different sizes,
                        // this is not allowed for standard cells
                        errBuf.append("ERROR: Arrayed Standard Cell " +context.getInstPath(".")+"."+ni.getName()+"\n");
                        errBuf.append("       cannot yield separate sizes for nodes in array: "+previousStandardCell.getName()+" vs "+standardCell.getName()+"\n");
                        standardCellArrayError = true;
                    }
                    VarContext nocontext = context.push(Netlist.getNodableFor(ni, 0));
                    standardCellNodeMap.put(nocontext, standardCell.iconView());
                } else {
                    // this is not a standard cell, must be purple gate - apply size
                    // as top level cell parameter
                    String varName = varNames.get(i);
                    cell.newVar(varName, f);
                }
            }

            // if there are standard cells, replace them in the hierarchy
            if (standardCellNodeMap.size() > 0) {
                Uniquifier uniquifier = new Uniquifier(standardCellNodeMap);
                uniquifier.registerCellsToUniquify(cell, VarContext.globalContext);
                // if hierarchy errors, do not replace new cells or set new sizes
                if (uniquifier.isHierarchyError() || standardCellArrayError) {
                    System.out.println(errBuf);
                    System.out.println("Error with hierarchy, please fix first: Standard Cell Sizes Not Updated");
                    return false;
                } else {
                    uniquifier.uniquify(cell, VarContext.globalContext);
                }
            }

            System.out.println("Sizes updated. Sizing Finished.");
            fieldVariableChanged("cell");
            return true;
        }

        /**
         * Get the standard cell for the given size. The type is based on the current
         * node type.
         * @param ni the current standard cell node
         * @param context the context
         * @param targetsize the target size
         * @return the standard cell (schematic view)
         */
        private Cell getStandardCell(NodeInst ni, VarContext context, double targetsize) {
            Cell np = null;
            if (ni.isCellInstance()) {
                np = (Cell)ni.getProto();
            }
            if (np != null && np.getCellGroup().getMainSchematics().getVar(SCLibraryGen.STANDARDCELL) != null) {
                // this is a standard cell from a standard cell library
                int tail = np.getName().indexOf("_X");
                String type = np.getName().substring(0, tail);
                double diff = Double.MAX_VALUE;
                double chosensize = 0;
                Cell chosencell = null;
                for (Iterator<Cell> it = np.getLibrary().getCells(); it.hasNext(); ) {
                    Cell c = it.next();
                    if (c.getView() != View.SCHEMATIC) continue;
                    String cname = c.getName();
                    if (!cname.startsWith(type+"_X")) continue;
                    double size = 0;
                    try {
                        size = Double.parseDouble(cname.substring(type.length()+2));
                    } catch (NumberFormatException e) {
                        continue;
                    }
                    double tempdiff = targetsize - size;
                    if (Math.abs(tempdiff) < Math.abs(diff)) {
                        diff = tempdiff;
                        chosensize = size;
                        chosencell = c;
                    }
                }
                if (chosencell == null) {
                    System.out.println("Unable to find standard cell for "+ni.describe(false));
                    return null;
                }
                double percentdiff = diff/targetsize;
                if (Math.abs(percentdiff) > standardCellErrorTolerancePrint) {
                    String p = TextUtils.formatDouble(100*percentdiff, 1);
                    String ideal = TextUtils.formatDouble(targetsize, 2);
                    String used = TextUtils.formatDouble(chosensize, 2);
                    //String strcontext = (context == VarContext.globalContext ? "topcell" : context.getInstPath("."));
                    String strcontext = context.push(ni).getInstPath(".");
                    System.out.println("  "+p+"%: "+ideal+" (ideal) vs "+used+" (used); for "+strcontext);
                }
                return chosencell;
            }
            return null;
        }

        public void terminateOK() {
            EditWindow wnd = EditWindow.findWindow(cell);
            if (wnd != null) wnd.repaintContents(null, false);
        }
    }

    /**
     * What I really want is to overload VarContext.hashCode to be
     * based on the string path, but that is really only valid for this
     * particular application, so instead I made a map that uses
     * the string path as the key.
     */
    public static class UniqueNodeMap {
        private Map<String,Cell> nodeMap;
        public UniqueNodeMap() {
            nodeMap = new HashMap<String,Cell>();
        }
        public void put(VarContext context, Cell cell) {
            nodeMap.put(getKey(context), cell);
        }
        public Cell get(VarContext context) {
            return nodeMap.get(getKey(context));
        }
        public int size() {
            return nodeMap.size();
        }
        private static String getKey(VarContext context) {
            return context.getInstPath(".");
        }
    }

    /**
     * The Uniquifier does two passes through the hierarchy.
     * The first pass is bottom-up to determine which cells need to
     * be duplicated/replaced. This depends on which nodes are standard cells
     * and what sizes they are.
     * The second pass goes top down doing all the actual cell replacements.
     * It must be top down to get the replacements correct, as lower level
     * cells must be replaced in their properly replaced upper level cells.
     */
    private static class Uniquifier {

        // key: varcontext, value: icon cell for last nodeinst on context
        // note that this contains standard cells for replacement, as well as uniquified cells
        private UniqueNodeMap uniqueNodeMap;
        // Map for a cell in the original hierarchy to it's replacement (unique) cells
        private Map<Cell,UniqueCell> uniqueCellMap;
        private boolean hierarchyError = false;
        private boolean verbose = false;

        public Uniquifier(UniqueNodeMap leafCellNodeMap) {
            this.uniqueNodeMap = leafCellNodeMap;
            uniqueCellMap = new HashMap<Cell,UniqueCell>();
        }

        /**
         * First pass: bottom up check of standard cells in the hierarchy.
         * Any cell that contains standard cells, or contains cells that contain
         * standard cells, is registered, and checked if it must be made unique.
         * A cell instantiated more than once in the design, that contains standard
         * cells with different sizes, must be made unique.  This method is
         * recursive.
         * @param cell current cell
         * @param context current context
         */
        public void registerCellsToUniquify(Cell cell, VarContext context) {
            Netlist netlist = cell.getUserNetlist();

            for (Iterator<Nodable> it = netlist.getNodables(); it.hasNext(); ) {
                Nodable no = it.next();
                if (!no.isCellInstance()) continue;
                Cell subproto = (Cell)no.getProto();
                if (subproto.isIconOf(cell)) continue;
                Cell schcell = subproto.getCellGroup().getMainSchematics();
                // recurse, bottom up
                registerCellsToUniquify(schcell, context.push(no));
            }

            // want bottom up traversal, so do action now after recurse
            Map<String,Cell> relevantNodes = new HashMap<String,Cell>();
            Map<NodeInst,Cell> arrayedNodeConflicts = new HashMap<NodeInst,Cell>();

            for (Iterator<Nodable> it = netlist.getNodables(); it.hasNext(); ) {
                Nodable no = it.next();
                if (!no.isCellInstance()) continue;

                VarContext nocontext = context.push(no);
                Cell iconCell = uniqueNodeMap.get(nocontext);
                if (iconCell == null) continue;

                // add to relevant nodes
                prMsg("Found Cell "+iconCell.describe(false)+" for context "+nocontext.getInstPath("."));
                relevantNodes.put(no.getName(), iconCell);

                // check for arrayed node conflicts (all nodes in array must map to same cell)
                Cell mappedIconCell = arrayedNodeConflicts.get(no.getNodeInst());
                if (mappedIconCell == null) {
                    arrayedNodeConflicts.put(no.getNodeInst(), iconCell);
                } else if (mappedIconCell != iconCell) {
                    System.out.println("ERROR: Arrayed node: "+nocontext.getInstPath("."));
                    System.out.println("       Has different sizes or different sizes in sub nodes; array must be flattened");
                    hierarchyError = true;
                }
            }

            if (context == VarContext.globalContext) return;

            if (relevantNodes.size() > 0) {
                // we may need to make this cell unique
                UniqueCell uniqueCell = uniqueCellMap.get(cell);
                if (uniqueCell == null) {
                    // first usage of this cell, do not need to duplicate
                    uniqueCell = new UniqueCell(cell, relevantNodes);
                    uniqueCellMap.put(cell, uniqueCell);
                }
                Cell newcell = uniqueCell.getUniqueCell(relevantNodes);
                uniqueNodeMap.put(context, newcell.iconView());
            }
        }

        /**
         * Second pass: top down replacement of both uniquified cells and
         * standard cells.  The replacement must be top down as sub-cells must
         * be replaced in their properly replaced parent cells. This method is
         * recursive.
         * @param cell current cell
         * @param context current context
         */
        public void uniquify(Cell cell, VarContext context) {
            Netlist netlist = cell.getUserNetlist();

            // top down traversal
            for (Iterator<Nodable> it = netlist.getNodables(); it.hasNext(); ) {
                Nodable no = it.next();
                if (!no.isCellInstance()) continue;

                NodeInst ni = no.getNodeInst();
                Cell subcell = (Cell)no.getProto();
                if (subcell.isIconOf(cell)) continue;

                VarContext nocontext = context.push(no);
                Cell schsubcell = subcell.getCellGroup().getMainSchematics();

                Cell newIcon = uniqueNodeMap.get(nocontext);
                if (newIcon != null && newIcon != ni.getProto()) {
                    if (ni.isLinked()) {
                        // if arrayed node, may have already replaced, so only replace if linked
                        ni.replace(newIcon, true, true);
                        prMsg("Replaced "+nocontext.getInstPath(".")+" with "+newIcon.describe(false));
                    }
                    schsubcell = newIcon.getCellGroup().getMainSchematics();
                    // find new nodable
                    boolean found = false;
                    for (Iterator<Nodable> it2 = netlist.getNodables(); it2.hasNext(); ) {
                        Nodable no2 = it2.next();
                        if (no.getNameKey() == no2.getNameKey()) {
                            nocontext = context.push(no2);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        System.out.println("ERROR: Could not find new nodable for "+context.getInstPath(".")+"."+no.getName());
                    }
                }
                // recurse, top down
                uniquify(schsubcell, nocontext);
            }
        }

        public boolean isHierarchyError() { return hierarchyError; }

        public void prMsg(String msg) {
            if (verbose) System.out.println(msg);
        }
    }

    /**
     * Register a cell containing standard cells, or containing
     * cells that contain standard cells - hereafter called the "relevant cells".
     * A unique key is made based on the relevant cells. Instances
     * with the same key share the same unique cell. The first key is assigned
     * to the original cell; new keys are assigned to duplicated cells.
     */
    private static class UniqueCell {
        private Set<String> relevantNodeNames;
        private Map<String,Cell> uniqueCells;
        private Cell originalCell;
        private boolean verbose = false;

        private UniqueCell(Cell cell, Map<String,Cell> relevantNodes) {
            originalCell = cell;
            this.relevantNodeNames = relevantNodes.keySet();
            uniqueCells = new HashMap<String,Cell>();
            // register existing cell as associated with current set of nodes
            String key = getKey(relevantNodes);
            uniqueCells.put(key, cell);
            prMsg("Registering original cell "+originalCell.describe(false)+" under key "+key);
        }

        public String getKey(Map<String,Cell> relevantNodes) {
            StringBuffer buf = new StringBuffer();
            for (String s : relevantNodeNames) {
                Cell c = relevantNodes.get(s);
                if (c != null) {
                    buf.append(c.getLibrary().getName());
                    buf.append(c.getName());
                }
            }
            return buf.toString();
        }

        /**
         * Retrieve a unique cell given the relevant nodes at some point in the
         * hierarchy. The relevant nodes are mapped to a key. If a cell is already
         * associated with the key, it is returned. Otherwise, a cell is duplicated
         * from the original cell, associated with the key, and returned.
         * @param relevantNodes the nodes that should be used at that point in the hierarchy.
         * May be standard cells or unique cells
         * @return the unique cells.
         */
        public Cell getUniqueCell(Map<String,Cell> relevantNodes) {
            String key = getKey(relevantNodes);
            prMsg("Checking against original cell "+originalCell.describe(false)+" with new key "+key);
            Cell c = uniqueCells.get(key);
            if (c == null) {
                // need to create a new duplicate of cell
                c = duplicate(originalCell);
                prMsg("Duplicating "+originalCell.describe(false));
                uniqueCells.put(key, c);
            }
            return c;
        }

        public static Cell duplicate(Cell cell) {
            assert(cell.getView() == View.SCHEMATIC);

            Library lib = cell.getLibrary();
            String pname = cell.getName().replaceAll("_[0-9]+$", "");
            String newpname = pname;
            int i=1;
            for (i=1; i<100; i++) {
                String temp = pname+"_"+i;
                if (lib.findNodeProto(temp) == null) {
                    newpname = temp;
                    break;
                }
            }
            if (i == 100) {
                System.out.println("Error: Unable to uniquify cell "+cell.describe(false)+", too many versions already");
                return cell;
            }
            // create new version
            Cell icon = cell.iconView();
            Cell newicon = null;
            if (icon != null) {
                newicon = Cell.copyNodeProto(icon, lib, newpname, true);
                if (newicon == null) {
                    System.out.println("Error: Unable to copy "+icon.describe(false)+" to "+newpname);
                    return cell;
                }
            }
            Cell newcell = Cell.copyNodeProto(cell, lib, newpname, true);
            if (newcell == null) {
                System.out.println("Error: Unable to copy "+cell.describe(false)+" to "+newpname);
                return cell;
            }
            // change master icon
            if (icon != null && newicon != null) {
                for (Iterator<NodeInst> it = newcell.getNodes(); it.hasNext(); ) {
                    NodeInst nni = it.next();
                    if (nni.getProto() == icon) {
                        nni.replace(newicon, true, true);
                    }
                }
            }
            return newcell;
        }

        public void prMsg(String msg) {
            if (verbose) System.out.println(msg);
        }
    }

    /**
     * Prints results of a sizing job for a Nodable.
     * @param no the Nodable to print info for.
     */
    public static void printResults(Nodable no, VarContext context)
    {
        // Doesn't iterate anymore over allJobs which might be AnalyzeCell because
        // it remembers only 1
        if (lastLEJobExecuted != null)
        {
            LENetlister netlister = lastLEJobExecuted.getNetlister();
            if (netlister.printResults(no, context)) return;
        }
        // no info found
        System.out.println("No existing completed sizing jobs contain info about "+no.getName());
    }

    public static void clearStoredSizesJob(Nodable no) {
        ClearStoredSizes job = new ClearStoredSizes(no);
    }

    public static void clearStoredSizesJob(Library lib) {
        ClearStoredSizesLibrary job = new ClearStoredSizesLibrary(lib);
    }

    /**
     * Clears stored "LEDRIVE_" sizes on a Nodable.
     */
    public static class ClearStoredSizes extends Job {
        private Nodable no;

		public ClearStoredSizes(Nodable no) {
            super("Clear LE Sizes", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.no = no;
            startJob();
        }

        public boolean doIt() throws JobException {
            clearStoredSizes(no);
            return true;
        }
    }

    public static class ClearStoredSizesLibrary extends Job {
        private Library lib;

		public ClearStoredSizesLibrary(Library lib) {
            super("Clear LE Sizes", tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.lib = lib;
            startJob();
        }

        public boolean doIt() throws JobException {
            clearStoredSizes(lib);
            return true;
        }
    }

    /**
     * Clears stored "LEDRIVE_" sizes on all nodes in a Cell.
     */
    private static void clearStoredSizes(Cell cell) {
        for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); ) {
            clearStoredSizes((NodeInst)it.next());
        }
        for (Iterator<Variable> it = cell.getVariables(); it.hasNext(); ) {
            Variable var = (Variable)it.next();
            String name = var.getKey().getName();
            if (name.startsWith("LEDRIVE_")) {
                cell.delVar(var.getKey());
            }
        }
    }

    /**
     * Clears stored "LEDRIVE_" sizes for all cells in a Library.
     */
    private static void clearStoredSizes(Library lib) {
        for (Iterator<Cell> it = lib.getCells(); it.hasNext(); ) {
            clearStoredSizes((Cell)it.next());
        }
    }

    // delete all vars that start with "LEDRIVE_"
    private static void clearStoredSizes(Nodable no) {
        for (Iterator<Variable> it = no.getVariables(); it.hasNext(); ) {
            Variable var = (Variable)it.next();
            String name = var.getKey().getName();
            if (name.startsWith("LEDRIVE_")) {
                no.delVar(var.getKey());
            }
        }
    }

/*
    protected static void addSettings(NetlisterConstants constants, Cell cell) {
        // find LESETTINGS cell
        Cell settings = null;
        for (Iterator libIt = Library.getLibraries(); libIt.hasNext(); ) {
            Library lib = (Library)libIt.next();
            for (Iterator it = lib.getCells(); it.hasNext(); ) {
                Cell c = (Cell)it.next();
                if (c.getVar("ATTR_LESETTINGS") != null) {
                    settings = c;
                    break;
                }
            }
        }
        if (settings == null) {
            System.out.println("Did not find LESETTINGS Cell, not saving settings to "+cell.describe());
            return;
        }
        Rectangle2D bounds = cell.getBounds();
        int x = (int)bounds.getMaxX();
        int y = (int)bounds.getMinY();
        NodeInst ni = NodeInst.makeInstance(settings, new Point2D.Double(x,y), settings.getDefWidth(),
                settings.getDefHeight(), cell);
        if (ni == null) {
            System.out.println("Could not create LESETTINGS instance, not saving settings to "+cell.describe());
            return;
        }
        Variable var;
        if ((var = ni.getVar("ATTR_su")) != null) ni.updateVar(var.getKey(), new Float(constants.su));
        if ((var = ni.getVar("ATTR_wire_ratio")) != null) ni.updateVar(var.getKey(), new Float(constants.wireRatio));
        if ((var = ni.getVar("ATTR_epsilon")) != null) ni.updateVar(var.getKey(), new Float(constants.epsilon));
        if ((var = ni.getVar("ATTR_max_iter")) != null) ni.updateVar(var.getKey(), new Integer(constants.maxIterations));
        if ((var = ni.getVar("ATTR_gate_cap")) != null) ni.updateVar(var.getKey(), new Float(constants.gateCap));
        if ((var = ni.getVar("ATTR_alpha")) != null) ni.updateVar(var.getKey(), new Float(constants.alpha));
        if ((var = ni.getVar("ATTR_keeper_ratio")) != null) ni.updateVar(var.getKey(), new Float(constants.keeperRatio));
    }
*/

	/************************** PROJECT SETTINGS *************************/

//	// preferences; tech-dependent values handled by technology and
    // project settings
    private static double DEFAULT_GLOBALFANOUT = 4.7;
    private static double DEFAULT_EPSILON      = 0.001;
    private static int    DEFAULT_MAXITER      = 30;
//    private static double DEFAULT_GATECAP      = 0.4;
//    private static double DEFAULT_WIRERATIO    = 0.16;
//    private static double DEFAULT_DIFFALPHA    = 0.7;
    private static double DEFAULT_KEEPERRATIO  = 0.1;

	/**
	 * Method to tell whether to use local settings for Logical Effort.
	 * The default is true.
	 * @return true to use local settings for Logical Effort
	 */
	public static boolean isUseLocalSettings() { return tool.cacheUseLocalSettings.getBoolean(); }
	/**
	 * Method to set whether to use local settings for Logical Effort
	 * @param on whether to use local settings for Logical Effort
	 */
	public static void setUseLocalSettings(boolean on) { tool.cacheUseLocalSettings.setBoolean(on); }
	/**
	 * Returns project Setting to tell whether to use local settings for Logical Effort
	 * @return project Setting to tell whether to use local settings for Logical Effort
	 */
	public static Setting getUseLocalSettingsSetting() { return tool.cacheUseLocalSettings; }

//	private static Pref cacheHighlightComponents = Pref.makeBooleanPref("HighlightComponents", LETool.tool.prefs, false);
//	/**
//	 * Method to tell whether to highlight components in Logical Effort.
//	 * The default is false.
//	 * @return true to highlight components in Logical Effort
//	 */
//	public static boolean isHighlightComponents() { return cacheHighlightComponents.getBoolean(); }
//	/**
//	 * Method to set whether to highlight components in Logical Effort
//	 * @param on whether to highlight components in Logical Effort
//	 */
//	public static void setHighlightComponents(boolean on) { cacheHighlightComponents.setBoolean(on); }

//	private static Pref cacheShowIntermediateCapacitances = Pref.makeBooleanPref("ShowIntermediateCapacitances", LETool.tool.prefs, false);
//	/**
//	 * Method to tell whether to highlight intermediate capacitances in Logical Effort.
//	 * The default is false.
//	 * @return true to highlight intermediate capacitances in Logical Effort
//	 */
//	public static boolean isShowIntermediateCapacitances() { return cacheShowIntermediateCapacitances.getBoolean(); }
//	/**
//	 * Method to set whether to highlight intermediate capacitances in Logical Effort
//	 * @param on whether to highlight intermediate capacitances in Logical Effort
//	 */
//	public static void setShowIntermediateCapacitances(boolean on) { cacheShowIntermediateCapacitances.setBoolean(on); }

	/**
	 * Method to get the Global Fanout for Logical Effort.
	 * The default is DEFAULT_GLOBALFANOUT.
	 * @return the Global Fanout for Logical Effort.
	 */
	public static double getGlobalFanout() { return tool.cacheGlobalFanout.getDouble(); }
	/**
     * Method to set the Global Fanout for Logical Effort.
	 * @param fo the Global Fanout for Logical Effort.
	 */
	public static void setGlobalFanout(double fo) { tool.cacheGlobalFanout.setDouble(fo); }
	/**
     * Returns project Setting to tell the Global Fanout for Logical Effort.
     * @return project Setting to tell the Global Fanout for Logical Effort.
	 */
	public static Setting getGlobalFanoutSetting() { return tool.cacheGlobalFanout; }

	/**
	 * Method to get the Convergence Epsilon value for Logical Effort.
	 * The default is DEFAULT_EPSILON.
	 * @return the Convergence Epsilon value for Logical Effort.
	 */
	public static double getConvergenceEpsilon() { return tool.cacheConvergenceEpsilon.getDouble(); }
	/**
	 * Method to set the Convergence Epsilon value for Logical Effort.
	 * @param ep the Convergence Epsilon value for Logical Effort.
	 */
	public static void setConvergenceEpsilon(double ep) { tool.cacheConvergenceEpsilon.setDouble(ep); }
	/**
	 * Returns project Setting to tell the Convergence Epsilon value for Logical Effort.
	 * @return project Setting to tell the Convergence Epsilon value for Logical Effort.
	 */
	public static Setting getConvergenceEpsilonSetting() { return tool.cacheConvergenceEpsilon; }

    /**
	 * Method to get the maximum number of iterations for Logical Effort.
	 * The default is DEFAULT_MAXITER.
	 * @return the maximum number of iterations for Logical Effort.
	 */
	public static int getMaxIterations() { return tool.cacheMaxIterations.getInt(); }
	/**
	 * Method to set the maximum number of iterations for Logical Effort.
	 * @param it the maximum number of iterations for Logical Effort.
	 */
	public static void setMaxIterations(int it) { tool.cacheMaxIterations.setInt(it); }
	/**
	 * Returns project Setting to tell the maximum number of iterations for Logical Effort.
	 * @return project Setting to tell the maximum number of iterations for Logical Effort.
	 */
	public static Setting getMaxIterationsSetting() { return tool.cacheMaxIterations; }

//	private static Pref cacheGateCapacitance = Pref.makeDoublePref("GateCapfFPerLambda", LETool.tool.prefs, DEFAULT_GATECAP);
//	/**
//	 * Method to get the Gate Capacitance for Logical Effort.
//	 * The default is DEFAULT_GATECAP.
//	 * @return the Gate Capacitance for Logical Effort.
//	 */
//	public static double getGateCapacitance() { return cacheGateCapacitance.getDouble(); }
//	/**
//	 * Method to set the Gate Capacitance for Logical Effort.
//	 * @param gc the Gate Capacitance for Logical Effort.
//	 */
//	public static void setGateCapacitance(double gc) { cacheGateCapacitance.setDouble(gc); }

//	private static Pref cacheWireRatio = Pref.makeDoublePref("WireRatio", LETool.tool.prefs, DEFAULT_WIRERATIO);
//	/**
//	 * Method to get the wire capacitance ratio for Logical Effort.
//	 * The default is DEFAULT_WIRERATIO.
//	 * @return the wire capacitance ratio for Logical Effort.
//	 */
//	public static double getWireRatio() { return cacheWireRatio.getDouble(); }
//	/**
//	 * Method to set the wire capacitance ratio for Logical Effort.
//	 * @param wr the wire capacitance ratio for Logical Effort.
//	 */
//	public static void setWireRatio(double wr) { cacheWireRatio.setDouble(wr); }

//	private static Pref cacheDiffAlpha = Pref.makeDoublePref("DiffusionAlpha", LETool.tool.prefs, DEFAULT_DIFFALPHA);
//	/**
//	 * Method to get the diffusion to gate capacitance ratio for Logical Effort.
//	 * The default is DEFAULT_DIFFALPHA.
//	 * @return the diffusion to gate capacitance ratio for Logical Effort.
//	 */
//	public static double getDiffAlpha() { return cacheDiffAlpha.getDouble(); }
//	/**
//	 * Method to set the diffusion to gate capacitance ratio for Logical Effort.
//	 * @param da the diffusion to gate capacitance ratio for Logical Effort.
//	 */
//	public static void setDiffAlpha(double da) { cacheDiffAlpha.setDouble(da); }

	/**
	 * Method to get the keeper size ratio for Logical Effort.
	 * The default is DEFAULT_KEEPERRATIO.
	 * @return the keeper size ratio for Logical Effort.
	 */
	public static double getKeeperRatio() { return tool.cacheKeeperRatio.getDouble(); }
	/**
	 * Method to set the keeper size ratio for Logical Effort.
	 * @param kr the keeper size ratio for Logical Effort.
	 */
	public static void setKeeperRatio(double kr) { tool.cacheKeeperRatio.setDouble(kr); }
	/**
	 * Returns project Setting to tell the keeper size ratio for Logical Effort.
	 * @eturn project Setting to tell the keeper size ratio for Logical Effort.
	 */
	public static Setting getKeeperRatioSetting() { return tool.cacheKeeperRatio; }

	private Setting cacheUseLocalSettings;
    private Setting cacheGlobalFanout;
    private Setting cacheConvergenceEpsilon;
    private Setting cacheMaxIterations;
    private Setting cacheKeeperRatio;

    @Override
    protected void initProjectSettings() {
        makeBooleanSetting("UseLocalSettings", "Logical Effort Tab", "Use Local Settings from Cell", true);
        makeDoubleSetting("GlobalFanout", "Logical Effort Tab", "Global Fanout", DEFAULT_GLOBALFANOUT);
        makeDoubleSetting("ConvergenceEpsilon", "Logical Effort Tab", "Convergence Epsilon", DEFAULT_EPSILON);
        makeIntSetting("MaxIterations", "Logical Effort Tab", "Maximum Iterations", DEFAULT_MAXITER);
        makeDoubleSetting("KeeperRatio", "Logical Effort Tab", "Keeper Ratio", DEFAULT_KEEPERRATIO);
    }
    
}
