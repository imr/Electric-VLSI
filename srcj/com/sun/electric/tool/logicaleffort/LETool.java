/* -*- tab-width: 4 -*-
 *
 * LETool.java
 *
 * Created on November 17, 2003, 10:16 AM
 */

package com.sun.electric.tool.logicaleffort;

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.*;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.Prefs;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.Job;

import bsh.Interpreter;
import bsh.InterpreterError;

import java.util.Iterator;
import java.lang.InterruptedException;
import java.io.OutputStream;

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
        Interpreter env = EvalJavaBsh.getInterpreter();
        try {            
            env.set("LE", tool);
        } catch (bsh.EvalError e) {
            System.out.println("  LETool init() bean shell error: "+e.getMessage());
        }

		// initialize preferences
 		if (!Prefs.exists("LEUseLocalSettings")) setUseLocalSettings(true);
 		if (!Prefs.exists("LEDisplayIntermediateCaps")) setDisplayIntermediateCaps(true);
 		if (!Prefs.exists("LEGlobalFanOut")) setGlobalFanOut(4.5);
		if (!Prefs.exists("LEConvergence")) setConvergence(0.1);
		if (!Prefs.exists("LEMaxIterations")) setMaxIterations(3);
		if (!Prefs.exists("LEGateCapacitance")) setGateCapacitance(0.4);
		if (!Prefs.exists("LEDefWireCapRatio")) setDefWireCapRatio(0.1);
		if (!Prefs.exists("LEDiffToGateCapRatioNMOS")) setDiffToGateCapRatioNMOS(0.7);
		if (!Prefs.exists("LEDiffToGateCapRatioPMOS")) setDiffToGateCapRatioPMOS(0.7);
		if (!Prefs.exists("LEKeeperSizeRatio")) setKeeperSizeRatio(0.1);
   }

	public static double getGlobalFanOut() { return Prefs.getDoubleOption("LEGlobalFanOut"); }
	public static void setGlobalFanOut(double v) { Prefs.setDoubleOption("LEGlobalFanOut", v); }

	public static double getConvergence() { return Prefs.getDoubleOption("LEConvergence"); }
	public static void setConvergence(double v) { Prefs.setDoubleOption("LEConvergence", v); }

	public static int getMaxIterations() { return Prefs.getIntegerOption("LEMaxIterations"); }
	public static void setMaxIterations(int v) { Prefs.setIntegerOption("LEMaxIterations", v); }

	public static double getGateCapacitance() { return Prefs.getDoubleOption("LEGateCapacitance"); }
	public static void setGateCapacitance(double v) { Prefs.setDoubleOption("LEGateCapacitance", v); }

	public static double getDefWireCapRatio() { return Prefs.getDoubleOption("LEDefWireCapRatio"); }
	public static void setDefWireCapRatio(double v) { Prefs.setDoubleOption("LEDefWireCapRatio", v); }

	public static double getDiffToGateCapRatioNMOS() { return Prefs.getDoubleOption("LEDiffToGateCapRatioNMOS"); }
	public static void setDiffToGateCapRatioNMOS(double v) { Prefs.setDoubleOption("LEDiffToGateCapRatioNMOS", v); }

	public static double getDiffToGateCapRatioPMOS() { return Prefs.getDoubleOption("LEDiffToGateCapRatioPMOS"); }
	public static void setDiffToGateCapRatioPMOS(double v) { Prefs.setDoubleOption("LEDiffToGateCapRatioPMOS", v); }

	public static double getKeeperSizeRatio() { return Prefs.getDoubleOption("LEKeeperSizeRatio"); }
	public static void setKeeperSizeRatio(double v) { Prefs.setDoubleOption("LEKeeperSizeRatio", v); }

	public static double getArcRatio(ArcProto arc) { return Prefs.getDoubleOption("LERatio"+arc.getTechnology().getTechName()+arc.getProtoName()); }
	public static void setArcRatio(ArcProto arc, double v) { Prefs.setDoubleOption("LERatio"+arc.getTechnology().getTechName()+arc.getProtoName(), v); }

	public static boolean isUseLocalSettings() { return Prefs.getBooleanOption("LEUseLocalSettings"); }
	public static void setUseLocalSettings(boolean v) { Prefs.setBooleanOption("LEUseLocalSettings", v); }

	public static boolean isDisplayIntermediateCaps() { return Prefs.getBooleanOption("LEDisplayIntermediateCaps"); }
	public static void setDisplayIntermediateCaps(boolean v) { Prefs.setBooleanOption("LEDisplayIntermediateCaps", v); }

	public static boolean isHighlightComponents() { return Prefs.getBooleanOption("LEHighlightComponents"); }
	public static void setHighlightComponents(boolean v) { Prefs.setBooleanOption("LEHighlightComponents", v); }

    public Object getdrive() {
        Interpreter env = EvalJavaBsh.getInterpreter();
        try {            
            Object info = env.get("info");
            if (!(info instanceof NodeInst)) return "?";
            Object context = env.get("context");
            if (!(context instanceof VarContext)) return "?";
            NodeInst ni = (NodeInst)info;
            VarContext vc = (VarContext)context;
            String ledrive = LETool.makeDriveStr(vc.push(ni));
            Variable var = ni.getVar(ledrive);
            if (var == null) return "?";
            Object val = var.getObject();
            return val;
        } catch (bsh.EvalError e) {
            System.out.println("  LETool getdrive failed: "+e.getMessage());
        }
        return null;
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
        }
        
        public void doIt() {
            setProgress("building equations");
            System.out.print("Building equations...");
            try {
                boolean donesleeping = false;
                while(!donesleeping) {
                    Thread.sleep(5000);
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
                    Thread.sleep(5000);
                    donesleeping = true;
                }
            } catch (InterruptedException e) {}
            wnd.redraw();
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
    
    
}
