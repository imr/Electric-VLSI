/* -*- tab-width: 4 -*-
 *
 * LETool.java
 *
 * Created on November 17, 2003, 10:16 AM
 */

package com.sun.electric.tool.logicaleffort;

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.variable.*;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.Tool;
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
    }
    
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
                    sleep(5000);
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
                    sleep(5000);
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
