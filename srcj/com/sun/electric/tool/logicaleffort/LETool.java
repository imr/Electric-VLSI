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
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.Job;

import java.util.Iterator;
import java.lang.InterruptedException;

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
    
    /** Analyze Cell called from menu */
    public void analyzeCell(Cell cell, VarContext context) {
        AnalyzeCell acjob = new AnalyzeCell(cell, context);
        acjob.start();
    }
    
    /** Actual Analyze cell job */
    protected class AnalyzeCell extends Job
    {
        /** progress */                         private String progress;
        /** cell to analyze */                  private Cell cell;
        /** var context */                      private VarContext context;
        /** lesizer */                          private LESizer lesizer;
        
        protected AnalyzeCell(Cell cell, VarContext context) {
            super("Analyze Cell "+cell.describe());
            progress = null;
            this.cell = cell;
            this.context = context;
        }
        
        public void doIt() {
            setProgress("building equations");
            try {
                boolean donesleeping = false;
                while(!donesleeping) {
                    sleep(5000);
                    donesleeping = true;
                }
            } catch (InterruptedException e) {}
            lesizer = new LESizer(ostream);
            LENetlister netlister = new LENetlister(lesizer, ostream);
            netlister.netlist(cell, context);
            if (getScheduledToAbort()) { setAborted(); return; }                  // abort job
            setProgress("iterating");
            netlister.size();
            try {
                boolean donesleeping = false;
                while(!donesleeping) {
                    sleep(5000);
                    donesleeping = true;
                }
            } catch (InterruptedException e) {}
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
