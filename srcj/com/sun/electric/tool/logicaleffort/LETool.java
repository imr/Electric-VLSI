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
 * LETool.java
 *
 * Created on November 17, 2003, 10:16 AM
 */

package com.sun.electric.tool.logicaleffort;

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.EvalJavaBsh;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.EditWindow;

import bsh.Interpreter;
import bsh.InterpreterError;

import java.io.OutputStream;
import java.lang.InterruptedException;
import java.util.Iterator;

/**
 * This is the Logical Effort Tool.  It doesn't actually do
 * any work itself, but acts as a public API for all of the
 * logical effort tool functionality.
 *
 * @author  gainsley
 */
public class LETool extends Tool {
    
    /** The Logical Effort tool */              public static LETool tool = new LETool();
    /** The Logical Effort tool Thread */       private Thread toolThread = null;
    /** LESizer object */                       private LESizer lesizer = null;
    /** if the tool has been paused */          private boolean paused = false;

    /** Creates a new instance of LETool */
    private LETool() {
        super("logical effort");
    }

    /** get LETool */
    public static LETool getLETool() {
        return tool;
    }
    
    /** Initialize tool - add calls to Bean Shell Evaluator */
    public void init() {
        Interpreter env = EvalJavaBsh.tool.getInterpreter();
        try {            
            env.set("LE", tool);
        } catch (bsh.EvalError e) {
            System.out.println("  LETool init() bean shell error: "+e.getMessage());
        }
   }

    public Object getdrive() {
        Object info = EvalJavaBsh.tool.getCurrentInfo();
        if (!(info instanceof Nodable)) return "?";
        VarContext context = EvalJavaBsh.tool.getCurrentContext();
        Nodable ni = (Nodable)info;
        String ledrive = LETool.makeDriveStr(context.push(ni));
        if (ledrive == null) return "?";
        Variable var = ni.getVar(ledrive);
        if (var == null) return "?";
        Object val = var.getObject();
        return val;
    }
        
    private static String makeDriveStr(VarContext context) {
        return "LEDRIVE_"+context.getInstPath(".");
    }
    
    /** Analyze Cell called from menu */
    public void analyzeCell(Cell cell, VarContext context, EditWindow wnd) {
        AnalyzeCell acjob = new AnalyzeCell(cell, context, wnd);
//        acjob.start();
    }
    
    /** Actual Analyze cell job */
    protected class AnalyzeCell extends Job
    {
        /** progress */                         private String progress;
        /** cell to analyze */                  private Cell cell;
        /** var context */                      private VarContext context;
        /** lesizer */                          private LESizer lesizer;
        /** EditWindow */                       private EditWindow wnd;
        
        protected AnalyzeCell(Cell cell, VarContext context, EditWindow wnd) {
            super("Analyze Cell "+cell.describe(), tool, Job.Type.CHANGE, null, cell, Job.Priority.USER);
            progress = null;
            this.cell = cell;
            this.context = context;
            this.wnd = wnd;
			this.startJob();
        }
        
        public void doIt() {
            setProgress("building equations");
            System.out.print("Building equations...");
            try {
                boolean donesleeping = false;
                while(!donesleeping) {
                    Thread.sleep(1);
                    donesleeping = true;
                }
            } catch (InterruptedException e) {}
            lesizer = new LESizer((OutputStream)System.out);
            LENetlister netlister = new LENetlister(lesizer, (OutputStream)System.out);
            netlister.netlist(cell, context);
            long equationsDone = System.currentTimeMillis();
            String elapsed = Job.getElapsedTime(equationsDone-startTime);
            System.out.println("done ("+elapsed+")");
            if (getScheduledToAbort()) { setAborted(); return; }                  // abort job
            System.out.println("Starting iterations: ");
            setProgress("iterating");
            netlister.size();
            netlister.updateSizes();
            try {
                boolean donesleeping = false;
                while(!donesleeping) {
                    Thread.sleep(1);
                    donesleeping = true;
                }
            } catch (InterruptedException e) {}
            wnd.repaintContents();
        }
        
        // add more info to default getInfo
        public String getInfo() {
            StringBuffer buf = new StringBuffer();
            buf.append(super.getInfo());
            buf.append("  Gates sized: "+lesizer.getNumGates()+"\n");
            buf.append("  Total Drive Strength: "+lesizer.getTotalSize()+"\n");
            return buf.toString();
        }
    }

	/****************************** OPTIONS ******************************/

	// preferences; default values are for the TSMC 180nm technology
    private static double DEFAULT_GLOBALFANOUT = 4.7;
    private static double DEFAULT_EPSILON      = 0.001;
    private static int    DEFAULT_MAXITER      = 30;
    private static double DEFAULT_GATECAP      = 0.4;
    private static double DEFAULT_WIRERATIO    = 0.16;
    private static double DEFAULT_DIFFALPHA    = 0.7;
    private static double DEFAULT_KEEPERRATIO  = 0.1;

	private static Tool.Pref cacheUseLocalSettings = LETool.tool.makeBooleanPref("UseLocalSettings", true);
	/**
	 * Method to tell whether to use local settings for Logical Effort.
	 * The default is true.
	 * @return true to use local settings for Logical Effort
	 */
	public static boolean isUseLocalSettings() { return cacheUseLocalSettings.getBoolean(); }
	/**
	 * Method to set whether to use local settings for Logical Effort
	 * @param on whether to use local settings for Logical Effort
	 */
	public static void setUseLocalSettings(boolean on) { cacheUseLocalSettings.setBoolean(on); }

	private static Tool.Pref cacheHighlightComponents = LETool.tool.makeBooleanPref("HighlightComponents", false);
	/**
	 * Method to tell whether to highlight components in Logical Effort.
	 * The default is false.
	 * @return true to highlight components in Logical Effort
	 */
	public static boolean isHighlightComponents() { return cacheHighlightComponents.getBoolean(); }
	/**
	 * Method to set whether to highlight components in Logical Effort
	 * @param on whether to highlight components in Logical Effort
	 */
	public static void setHighlightComponents(boolean on) { cacheHighlightComponents.setBoolean(on); }

	private static Tool.Pref cacheShowIntermediateCapacitances = LETool.tool.makeBooleanPref("ShowIntermediateCapacitances", false);
	/**
	 * Method to tell whether to highlight intermediate capacitances in Logical Effort.
	 * The default is false.
	 * @return true to highlight intermediate capacitances in Logical Effort
	 */
	public static boolean isShowIntermediateCapacitances() { return cacheShowIntermediateCapacitances.getBoolean(); }
	/**
	 * Method to set whether to highlight intermediate capacitances in Logical Effort
	 * @param on whether to highlight intermediate capacitances in Logical Effort
	 */
	public static void setShowIntermediateCapacitances(boolean on) { cacheShowIntermediateCapacitances.setBoolean(on); }

	private static Tool.Pref cacheGlobalFanout = LETool.tool.makeDoublePref("GlobalFanout", DEFAULT_GLOBALFANOUT);
	/**
	 * Method to get the Global Fanout for Logical Effort.
	 * The default is DEFAULT_GLOBALFANOUT.
	 * @return the Global Fanout for Logical Effort.
	 */
	public static double getGlobalFanout() { return cacheGlobalFanout.getDouble(); }
	/**
	 * Method to set the Global Fanout for Logical Effort.
	 * @param fo the Global Fanout for Logical Effort.
	 */
	public static void setGlobalFanout(double fo) { cacheGlobalFanout.setDouble(fo); }

	private static Tool.Pref cacheConvergenceEpsilon = LETool.tool.makeDoublePref("Epsilon", DEFAULT_EPSILON);
	/**
	 * Method to get the Convergence Epsilon value for Logical Effort.
	 * The default is DEFAULT_EPSILON.
	 * @return the Convergence Epsilon value for Logical Effort.
	 */
	public static double getConvergenceEpsilon() { return cacheConvergenceEpsilon.getDouble(); }
	/**
	 * Method to set the Convergence Epsilon value for Logical Effort.
	 * @param ep the Convergence Epsilon value for Logical Effort.
	 */
	public static void setConvergenceEpsilon(double ep) { cacheConvergenceEpsilon.setDouble(ep); }

	private static Tool.Pref cacheMaxIterations = LETool.tool.makeIntPref("MaxIterations", DEFAULT_MAXITER);
	/**
	 * Method to get the maximum number of iterations for Logical Effort.
	 * The default is DEFAULT_MAXITER.
	 * @return the maximum number of iterations for Logical Effort.
	 */
	public static int getMaxIterations() { return cacheMaxIterations.getInt(); }
	/**
	 * Method to set the maximum number of iterations for Logical Effort.
	 * @param it the maximum number of iterations for Logical Effort.
	 */
	public static void setMaxIterations(int it) { cacheMaxIterations.setInt(it); }

	private static Tool.Pref cacheGateCapacitance = LETool.tool.makeDoublePref("GateCapfFPerLambda", DEFAULT_GATECAP);
	/**
	 * Method to get the Gate Capacitance for Logical Effort.
	 * The default is DEFAULT_GATECAP.
	 * @return the Gate Capacitance for Logical Effort.
	 */
	public static double getGateCapacitance() { return cacheGateCapacitance.getDouble(); }
	/**
	 * Method to set the Gate Capacitance for Logical Effort.
	 * @param gc the Gate Capacitance for Logical Effort.
	 */
	public static void setGateCapacitance(double gc) { cacheGateCapacitance.setDouble(gc); }

	private static Tool.Pref cacheWireRatio = LETool.tool.makeDoublePref("WireRatio", DEFAULT_WIRERATIO);
	/**
	 * Method to get the wire capacitance ratio for Logical Effort.
	 * The default is DEFAULT_WIRERATIO.
	 * @return the wire capacitance ratio for Logical Effort.
	 */
	public static double getWireRatio() { return cacheWireRatio.getDouble(); }
	/**
	 * Method to set the wire capacitance ratio for Logical Effort.
	 * @param wr the wire capacitance ratio for Logical Effort.
	 */
	public static void setWireRatio(double wr) { cacheWireRatio.setDouble(wr); }

	private static Tool.Pref cacheDiffAlpha = LETool.tool.makeDoublePref("DiffusionAlpha", DEFAULT_DIFFALPHA);
	/**
	 * Method to get the diffusion to gate capacitance ratio for Logical Effort.
	 * The default is DEFAULT_DIFFALPHA.
	 * @return the diffusion to gate capacitance ratio for Logical Effort.
	 */
	public static double getDiffAlpha() { return cacheDiffAlpha.getDouble(); }
	/**
	 * Method to set the diffusion to gate capacitance ratio for Logical Effort.
	 * @param da the diffusion to gate capacitance ratio for Logical Effort.
	 */
	public static void setDiffAlpha(double da) { cacheDiffAlpha.setDouble(da); }

	private static Tool.Pref cacheKeeperRatio = LETool.tool.makeDoublePref("KeeperRatio", DEFAULT_KEEPERRATIO);
	/**
	 * Method to get the keeper size ratio for Logical Effort.
	 * The default is DEFAULT_KEEPERRATIO.
	 * @return the keeper size ratio for Logical Effort.
	 */
	public static double getKeeperRatio() { return cacheKeeperRatio.getDouble(); }
	/**
	 * Method to set the keeper size ratio for Logical Effort.
	 * @param kr the keeper size ratio for Logical Effort.
	 */
	public static void setKeeperRatio(double kr) { cacheKeeperRatio.setDouble(kr); }

}
