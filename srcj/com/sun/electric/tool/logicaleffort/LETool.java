/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ToolOptions.java
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
   
    // preferences; default values are for the TSMC 180nm technology
    public static String        OPTION_USELOCALSETTINGS = "UseLocalSettings";
    public static boolean       DEFAULT_USELOCALSETTINGS = true;
    public static String        OPTION_GLOBALFANOUT = "GlobalFanout";
    public static float         DEFAULT_GLOBALFANOUT = 4.7f;
    public static String        OPTION_WIRERATIO = "WireRatio";
    public static float         DEFAULT_WIRERATIO = 0.16f;
    public static String        OPTION_EPSILON = "Epsilon";
    public static float         DEFAULT_EPSILON = 0.001f;
    public static String        OPTION_MAXITER = "MaxIterations";
    public static int           DEFAULT_MAXITER = 30;
    public static String        OPTION_GATECAP = "GateCapfFPerLambda";
    public static float         DEFAULT_GATECAP = 0.4f;
    public static String        OPTION_DIFFALPHA = "DiffusionAlpha";
    public static float         DEFAULT_DIFFALPHA = 0.7f;
    public static String        OPTION_KEEPERRATIO = "KeeperRatio";
    public static float         DEFAULT_KEEPERRATIO = 0.1f;
    
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
        if (!(info instanceof NodeInst)) return "?";
        VarContext context = EvalJavaBsh.tool.getCurrentContext();
        NodeInst ni = (NodeInst)info;
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
