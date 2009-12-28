/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LENetlister.java
 * Written by Jonathan Gainsley, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.logicaleffort;

import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.user.ErrorLogger;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.List;
import java.io.Serializable;


public abstract class LENetlister extends HierarchyEnumerator.Visitor {
    static final Netlist.ShortResistors SHORT_RESISTORS = Netlist.ShortResistors.ALL;

    public static final Variable.Key ATTR_su = Variable.newKey("ATTR_su");
    public static final Variable.Key ATTR_le = Variable.newKey("ATTR_le");
    public static final Variable.Key ATTR_wire_ratio = Variable.newKey("ATTR_wire_ratio");
    public static final Variable.Key ATTR_epsilon = Variable.newKey("ATTR_epsilon");
    public static final Variable.Key ATTR_max_iter = Variable.newKey("ATTR_max_iter");
    public static final Variable.Key ATTR_gate_cap = Variable.newKey("ATTR_gate_cap");
    public static final Variable.Key ATTR_alpha = Variable.newKey("ATTR_alpha");
    public static final Variable.Key ATTR_diffn = Variable.newKey("ATTR_diffn");
    public static final Variable.Key ATTR_diffp = Variable.newKey("ATTR_diffp");
    public static final Variable.Key ATTR_keeper_ratio = Variable.newKey("ATTR_keeper_ratio");
    public static final Variable.Key ATTR_x1inverter_nwidth = Variable.newKey("ATTR_x1inverter_nwidth");
    public static final Variable.Key ATTR_x1inverter_pwidth = Variable.newKey("ATTR_x1inverter_pwidth");
    public static final Variable.Key ATTR_x1inverter_length = Variable.newKey("ATTR_x1inverter_length");
    public static final Variable.Key ATTR_LEGATE = Variable.newKey("ATTR_LEGATE");
    public static final Variable.Key ATTR_LEKEEPER = Variable.newKey("ATTR_LEKEEPER");
    public static final Variable.Key ATTR_LEWIRE = Variable.newKey("ATTR_LEWIRE");
    public static final Variable.Key ATTR_LEIGNORE = Variable.newKey("ATTR_LEIGNORE");
    public static final Variable.Key ATTR_LESETTINGS = Variable.newKey("ATTR_LESETTINGS");
    public static final Variable.Key ATTR_LEPARALLGRP = Variable.newKey("ATTR_LEPARALLGRP");
    public static final Variable.Key ATTR_L = Variable.newKey("ATTR_L");
    public static final Variable.Key ATTR_LEWIRECAP = Variable.newKey("ATTR_LEWIRECAP");

    public static class NetlisterConstants implements Serializable {
        /** global step-up */                       public final float su;
        /** wire to gate cap ratio */               public final float wireRatio;
        /** convergence criteron */                 public final float epsilon;
        /** max number of iterations */             public final int maxIterations;
        /** gate cap, in fF/lambda */               public final float gateCap;
        /** ratio of diffusion to gate cap */       public final float alpha;
        /** ratio of keeper to driver size */       public final float keeperRatio;
        /** width in lambda of x=1 inverter nmos */ public final float x1inverter_nwidth;
        /** width in lambda of x=1 inverter pmos */ public final float x1inverter_pwidth;
        /** length in lambda of x=1 mosfets */      public final float x1inverter_length;

        public NetlisterConstants(float su, float wireRatio, float epsilon,
                                  int maxIterations, float gateCap, float alpha, float keeperRatio,
                                  float x1inverter_nwidth, float x1inverter_pwidth, float x1inverter_length) {
            this.su = su;
            this.wireRatio = wireRatio;
            this.epsilon = epsilon;
            this.maxIterations = maxIterations;
            this.gateCap = gateCap;
            this.alpha = alpha;
            this.keeperRatio = keeperRatio;
            this.x1inverter_nwidth = x1inverter_nwidth;
            this.x1inverter_pwidth = x1inverter_pwidth;
            this.x1inverter_length = x1inverter_length;
        }

        /** Create a new set of constants from the user's settings */
        public NetlisterConstants(Technology layoutTech) {
            su = (float)LETool.getGlobalFanout();
            epsilon = (float)LETool.getConvergenceEpsilon();
            maxIterations = LETool.getMaxIterations();
            gateCap = (float)layoutTech.getGateCapacitance();
            wireRatio = (float)layoutTech.getWireRatio();
            alpha = (float)layoutTech.getDiffAlpha();
            keeperRatio = (float)LETool.getKeeperRatio();
            x1inverter_nwidth = (float)LETool.getX1InverterNWidth();
            x1inverter_pwidth = (float)LETool.getX1InverterPWidth();
            x1inverter_length = (float)LETool.getX1InverterLength();
        }

        /** Returns true if the two NetlisterConstants have the same values for all fields */
        public boolean equals(NetlisterConstants other) {
            if (su != other.su) return false;
            if (wireRatio != other.wireRatio) return false;
            if (epsilon != other.epsilon) return false;
            if (maxIterations != other.maxIterations) return false;
            if (gateCap != other.gateCap) return false;
            if (alpha != other.alpha) return false;
            if (keeperRatio != other.keeperRatio) return false;
            if (x1inverter_nwidth != other.x1inverter_nwidth) return false;
            if (x1inverter_pwidth != other.x1inverter_pwidth) return false;
            if (x1inverter_length != other.x1inverter_length) return false;
            return true;
        }
    }
    
    Technology layoutTech;
    
    LENetlister(Technology layoutTech) {
        this.layoutTech = layoutTech;
    }

    /** Call to start netlisting. Returns false if failed */
    public abstract boolean netlist(Cell cell, VarContext context, boolean useCaching);

    /** Call to stop or interrupt netlisting */
    public abstract void done();

    /**
     * Call to size netlist with the specified algorithm
     * @return true if successful, false otherwise
     */
    public abstract boolean size(LESizer.Alg algorithm);

    /** Get the error logger */
    public abstract ErrorLogger getErrorLogger();
    /** Destroy the error logger */
    public abstract void nullErrorLogger();

    /** Get the settings used for sizing */
    public abstract NetlisterConstants getConstants();

    /** Get the sizes and associated variable names to store on the top level cell */
    public abstract void getSizes(List<Float> sizes, List<String> varNames,
                                  List<NodeInst> nodes, List<VarContext> contexts);

    public abstract void printStatistics();

    // ---------------------------- statistics ---------------------------------

    /** print the results for the Nodable
     * @return true if successful, false otherwise */
    public abstract boolean printResults(Nodable no, VarContext context);

    /** Get the total size of all gates sized using Logical Effort */
    public abstract float getTotalLESize();

    // ------------------------- Utility ---------------------------------------

    /**
     * Get any Logical Effort settings saved on the specified cell
     * @param cell the cell in question
     * @return the netlister constants settings, or null if none found
     */
    protected NetlisterConstants getSettings(Cell cell) {
        if (!LETool.isUseLocalSettings()) return null;
        for (Iterator<NodeInst> instIt = cell.getNodes(); instIt.hasNext();) {
            NodeInst ni = (NodeInst)instIt.next();
            if (ni.isIconOfParent()) continue;
            if (!ni.isCellInstance()) continue;
            if (ni.getParameterOrVariable(ATTR_LESETTINGS) != null) {
                Technology tech = cell.getTechnology();
                if (cell.isSchematic())
                    tech = layoutTech;
                float su = (float)LETool.getGlobalFanout();
                float epsilon = (float)LETool.getConvergenceEpsilon();
                int maxIterations = LETool.getMaxIterations();
                float gateCap = (float)tech.getGateCapacitance();
                float wireRatio = (float)tech.getWireRatio();
                float alpha = (float)tech.getDiffAlpha();
                float keeperRatio = (float)LETool.getKeeperRatio();
                float x1inverter_nwidth = (float)LETool.getX1InverterNWidth();
                float x1inverter_pwidth = (float)LETool.getX1InverterPWidth();
                float x1inverter_length = (float)LETool.getX1InverterLength();
                Variable var;
                VarContext context = VarContext.globalContext;
                if ((var = ni.getParameterOrVariable(ATTR_su)) != null) su = VarContext.objectToFloat(context.evalVar(var), su);
                if ((var = ni.getParameterOrVariable(ATTR_wire_ratio)) != null) wireRatio = VarContext.objectToFloat(context.evalVar(var), wireRatio);
                if ((var = ni.getParameterOrVariable(ATTR_epsilon)) != null) epsilon = VarContext.objectToFloat(context.evalVar(var), epsilon);
                if ((var = ni.getParameterOrVariable(ATTR_max_iter)) != null) maxIterations = VarContext.objectToInt(context.evalVar(var), maxIterations);
                if ((var = ni.getParameterOrVariable(ATTR_gate_cap)) != null) gateCap = VarContext.objectToFloat(context.evalVar(var), gateCap);
                if ((var = ni.getParameterOrVariable(ATTR_alpha)) != null) alpha = VarContext.objectToFloat(context.evalVar(var), alpha);
                if ((var = ni.getParameterOrVariable(ATTR_keeper_ratio)) != null) keeperRatio = VarContext.objectToFloat(context.evalVar(var), keeperRatio);
                if ((var = ni.getParameterOrVariable(ATTR_x1inverter_nwidth)) != null) x1inverter_nwidth = VarContext.objectToFloat(context.evalVar(var), x1inverter_nwidth);
                if ((var = ni.getParameterOrVariable(ATTR_x1inverter_pwidth)) != null) x1inverter_pwidth = VarContext.objectToFloat(context.evalVar(var), x1inverter_pwidth);
                if ((var = ni.getParameterOrVariable(ATTR_x1inverter_length)) != null) x1inverter_length = VarContext.objectToFloat(context.evalVar(var), x1inverter_length);
                return new NetlisterConstants(su, wireRatio, epsilon, maxIterations, gateCap, alpha, keeperRatio, x1inverter_nwidth, x1inverter_pwidth, x1inverter_length);
            }
        }
        return null;
    }

    /**
     * This checks for LE settings in the cell, and returns true if they conflict.
     * It also warns the user that there are conflicting settings from the subcell.
     * @param current the current settings (from the top level cell, or global options)
     * @return true if there was a conflict, false otherwise
     */
    protected boolean isSettingsConflict(NetlisterConstants current, Cell topLevelCell, VarContext context, Cell localCell) {
        assert(current != null);
        NetlisterConstants local = getSettings(localCell);
        if (local == null) return false;
        if (!current.equals(local)) {
            System.out.println("Error: Global settings from "+topLevelCell+" do not match global settings from \""+context.getInstPath("/")
                    +": "+localCell.noLibDescribe()+"\" in (" + localCell.getLibrary().getName()+")");
            System.out.println("       Global settings are by definition global, and differences may indicate an inconsistency in your design.");
            System.out.println("       Note that step-up, \"su\", can be made local by defining a \"su\" parameter on an instance.");
            System.out.println("\tglobal/parent vs local:");
            if (current.su != local.su) System.out.println("su:\t"+current.su+" vs "+local.su);
            if (current.wireRatio != local.wireRatio) System.out.println("wireRatio:\t"+current.wireRatio+" vs "+local.wireRatio);
            if (current.epsilon != local.epsilon) System.out.println("epsilon:\t"+current.epsilon+" vs "+local.epsilon);
            if (current.maxIterations != local.maxIterations) System.out.println("maxIterations:\t"+current.maxIterations+" vs "+local.maxIterations);
            if (current.gateCap != local.gateCap) System.out.println("gateCap:\t"+current.gateCap+" vs "+local.gateCap);
            if (current.alpha != local.alpha) System.out.println("alpha:\t"+current.alpha+" vs "+local.alpha);
            if (current.keeperRatio != local.keeperRatio) System.out.println("keeperRatio:\t"+current.keeperRatio+" vs "+local.keeperRatio);
            if (current.x1inverter_nwidth != local.x1inverter_nwidth) System.out.println("x=1 inverter nwidth:\t"+current.x1inverter_nwidth+" vs "+local.x1inverter_nwidth);
            if (current.x1inverter_pwidth != local.x1inverter_pwidth) System.out.println("x=1 inverter pwidth:\t"+current.x1inverter_pwidth+" vs "+local.x1inverter_pwidth);
            if (current.x1inverter_length != local.x1inverter_length) System.out.println("x=1 inverter length:\t"+current.x1inverter_length+" vs "+local.x1inverter_length);
            //SwingUtilities.invokeLater(new Runnable() {
            //    public void run() {
                	Job.getUserInterface().showErrorMessage("Conflicting global parameter settings were found, " +
                            "please see message window for details", "Settings Conflict Found!!");
            //    }
            //});
            return true;
        }
        return false;
    }

    /**
     * Saves the Global settings to the cell. Note that this does not overwrite
     * settings already there.  If any settings are found, it does nothing and returns false.
     * @return true if settings saved, false otherwise
     */
    protected boolean saveSettings(NetlisterConstants constants, Cell cell) {
        // make sure no settings already on cell
        if (getSettings(cell) != null) return false;

        // first we need to find the LESettings Cell
        Cell settings = null;
        for (Iterator<Library> it = Library.getLibraries(); it.hasNext(); ) {
            Library lib = (Library)it.next();
            for (Iterator<Cell> it2 = lib.getCells(); it2.hasNext(); ) {
                Cell c = (Cell)it2.next();
                if (c.getParameterOrVariable(ATTR_LESETTINGS) != null) {
                    settings = c;
                    break;
                }
            }
            if (settings != null) break;
        }
        if (settings == null) {
            System.out.println("Could not find LESETTINGS cell in order to save settings to "+cell);
            return false;
        }
        System.out.println("Creating new LESETTINGS box on "+cell+" from User Preferences because none found. Logical effort requires this box");
        SaveSettings job = new SaveSettings(cell, settings, constants);
        return true;
    }

    private static class SaveSettings extends Job {
        private Cell cell;
        private Cell settings;
        private NetlisterConstants constants;

		public SaveSettings(Cell cell, Cell settings, NetlisterConstants constants) {
            super("Clear LE Sizes", LETool.getLETool(), Job.Type.CHANGE, null, null, Job.Priority.USER);
            this.cell = cell;
            this.settings = settings;
            this.constants = constants;
            startJob();
        }

        public boolean doIt() throws JobException {
            Rectangle2D bounds = cell.getBounds();
            int x = (int)bounds.getMaxX();
            int y = (int)bounds.getMinY();
            Cell temp = settings.iconView();
            if (temp != null) settings = temp;
            NodeInst ni = NodeInst.makeInstance(settings, new Point2D.Double(x, y), settings.getDefWidth(),
                    settings.getDefHeight(), cell);
            if (ni == null) {
                System.out.println("Could not make instance of LESETTINGS in "+cell+" to save settings.");
                return false;
            }
            ni.updateVar(ATTR_su, new Float(constants.su));
            ni.updateVar(ATTR_wire_ratio, new Float(constants.wireRatio));
            ni.updateVar(ATTR_epsilon, new Float(constants.epsilon));
            ni.updateVar(ATTR_max_iter, new Integer(constants.maxIterations));
            ni.updateVar(ATTR_gate_cap, new Float(constants.gateCap));
            ni.updateVar(ATTR_alpha, new Float(constants.alpha));
            ni.updateVar(ATTR_keeper_ratio, new Float(constants.keeperRatio));
            ni.updateVar(ATTR_x1inverter_nwidth, new Float(constants.x1inverter_nwidth));
            ni.updateVar(ATTR_x1inverter_pwidth, new Float(constants.x1inverter_pwidth));
            ni.updateVar(ATTR_x1inverter_length, new Float(constants.x1inverter_length));

            return true;
        }

    }

    protected static class LECellInfo extends HierarchyEnumerator.CellInfo {

        /** M-factor to be applied to size */       private float mFactor;
        /** SU to be applied to gates in cell */    private float cellsu;
        /** local settings */                       private NetlisterConstants localSettings;

        protected void leInit(NetlisterConstants constants) {

            HierarchyEnumerator.CellInfo parent = getParentInfo();

            // check for M-Factor from parent
            if (parent == null) mFactor = 1f;
            else mFactor = ((LECellInfo)parent).getMFactor();

            // check for su from parent
            if (parent == null) cellsu = constants.su;
            else cellsu = ((LECellInfo)parent).getSU();

            // get info from node we pushed into
            Nodable ni = getContext().getNodable();
            if (ni != null) {
                // get mfactor from instance we pushed into
                Variable mvar = LETool.getMFactor(ni);
                if (mvar != null) {
                    Object mval = getContext().evalVar(mvar, null);
                    if (mval != null)
                        mFactor = mFactor * VarContext.objectToFloat(mval, 1f);
                }

                // get su from instance we pushed into
                Variable suvar = ni.getParameterOrVariable(ATTR_su);
                if (suvar != null) {
                    float su = VarContext.objectToFloat(getContext().evalVar(suvar, null), -1f);
                    if (su != -1f) cellsu = su;
                }
            }

            localSettings = new NetlisterConstants(
                    cellsu,
                    constants.wireRatio,
                    constants.epsilon,
                    constants.maxIterations,
                    constants.gateCap,
                    constants.alpha,
                    constants.keeperRatio,
                    constants.x1inverter_nwidth,
                    constants.x1inverter_pwidth,
                    constants.x1inverter_length
            );
        }

        /** get mFactor */
        protected float getMFactor() { return mFactor; }

        protected float getSU() { return cellsu; }

        protected NetlisterConstants getSettings() {
            return localSettings;
        }
    }

}
