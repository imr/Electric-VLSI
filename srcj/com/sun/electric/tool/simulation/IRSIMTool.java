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
 * IRSIMTool.java
 *
 * Created on December 2, 2003, 2:50 PM
 */

package com.sun.electric.tool.simulation;

import com.sun.electric.tool.Tool;
import com.sun.electric.tool.Job;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.user.ui.EditWindow;
//import com.sun.electric.technology.technologies.;

import java.util.ArrayList;
import java.io.File;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.awt.Dimension;

/**
 *
 * @author  gainsley
 */
public class IRSIMTool extends Tool {
    
//    /** File to write to */                 private File file;
//    /** Output */                           private PrintWriter out;
//    /** Cell to write out */                private Cell topCell;
    /** IRSIM Tool */                       public static IRSIMTool tool = new IRSIMTool();
    
    
    /** Creates a new instance of IRSIMNetlister */
    private IRSIMTool() {
        super("IRSIM");
        // try to open file in current directory for cell
//        file = new File(cell.noLibDescribe()+".sim");
//       try {
//            out = new PrintWriter(new FileWriter(file));
//        } catch (IOException e) {
//            System.out.println("Error opening "+file+": "+e.getMessage());
//            return;
//        }
    }

    //----------------------------IRSIM Commands-------------------------------

    public void netlistCell(Cell cell, VarContext context, EditWindow wnd)
    {
        NetlistCell ncjob = new NetlistCell(cell, context, wnd);
    }
    
    
    //-----------------------------IRSIM Jobs-----------------------------------

    public class NetlistCell extends Job
    {
        /** progress */                         private String progress;
        /** cell to analyze */                  private Cell cell;
        /** var context */                      private VarContext context;
        /** EditWindow */                       private EditWindow wnd;
        /** netlister */                        private IRSIMNetlister netlister;
       
        protected NetlistCell(Cell cell, VarContext context, EditWindow wnd) {
            super("IRSIM Netlist "+cell.describe(), tool, Job.Type.CHANGE, null, cell, Job.Priority.USER);
            progress = null;
            this.cell = cell;
            this.context = context;
            this.wnd = wnd;
        }            
        
        public void doIt() {
            netlister = new IRSIMNetlister();
            cell.rebuildNetworks(null, true);
            HierarchyEnumerator.enumerateCell(cell, context, netlister);
        }        
    }        
    
    //------------------------------IRSIM NETLISTER----------------------------

    /** IRSIM Netlister */
    protected class IRSIMNetlister extends HierarchyEnumerator.Visitor {
        
        public HierarchyEnumerator.CellInfo newCellInfo() { return new IRSIMCellInfo(); }

        public boolean enterCell(HierarchyEnumerator.CellInfo info) {
            ((IRSIMCellInfo)info).extInit();
            return true;            
        }        
        
        public void exitCell(HierarchyEnumerator.CellInfo info) {
        }        
        
        public boolean visitNodeInst(NodeInst ni, HierarchyEnumerator.CellInfo info) {

            IRSIMCellInfo iinfo = (IRSIMCellInfo)info;
            
            NodeProto np = ni.getProto();               // check if prototype is Primitive transistor
            if (!(np instanceof PrimitiveNode)) return true;  // descend and enumerate
            PrimitiveNode pn = (PrimitiveNode)np;
            if (!(ni.isPrimitiveTransistor())) return false;   // not transistor, ignore
            boolean isNMOS = false;
            if (ni.getFunction() == NodeProto.Function.TRANMOS ||
                ni.getFunction() == NodeProto.Function.TRA4NMOS)
                isNMOS = true;
            
            PortInst g = ni.getTransistorGatePort();
            PortInst d = ni.getTransistorDrainPort();
            PortInst s = ni.getTransistorSourcePort();
            if (g == null || d == null || s == null) {
                System.out.println("  PortInst for "+ni+" null!");
                return false;
            }
            JNetwork gnet = g.getNetwork();
            JNetwork dnet = d.getNetwork();
            JNetwork snet = s.getNetwork();
            if (gnet == null || dnet == null || snet == null) {
                System.out.println("  Warning, ignoring unconnected transistor "+ni+" in cell "+iinfo.getCell());
                return false;
            }
            // print out transistor
            if (isNMOS)
                System.out.print("n ");
            else
                System.out.print("p ");
            System.out.print(iinfo.getUniqueNetName(gnet, "/")+" ");
            System.out.print(iinfo.getUniqueNetName(snet, "/")+" ");
            System.out.print(iinfo.getUniqueNetName(dnet, "/")+" ");
            Dimension dim = ni.getTransistorSize(iinfo.getContext());
            float m = iinfo.getMFactor();
            System.out.print(dim.getHeight() + " ");                // length
            System.out.print((double)m * dim.getWidth() + " ");     // width
            System.out.print(ni.getCenterX()+" ");                  // xpos
            System.out.print(ni.getCenterY()+" ");                  // ypos
            if (isNMOS)
                System.out.print("g=S_gnd ");
            else 
                System.out.print("g=S_vdd ");
            double sourceArea;
            double sourcePerim;
            double drainArea;
            double drainPerim;
            sourceArea = sourcePerim = drainArea = drainPerim = 0;
            System.out.print("s=A_"+sourceArea+",P_"+sourcePerim);
            System.out.print("d=A_"+drainArea+",P_"+drainPerim);
            System.out.println();
            return false;
        }        
    }

    //----------------------------IRSIM Cell Info for HierarchyEnumerator--------------------
    
    /** IRSIM Cell Info class */
    private class IRSIMCellInfo extends HierarchyEnumerator.CellInfo {

        /** M-factor to be applied to size */       private float mFactor;

        /** initialize LECellInfo: assumes CellInfo.init() has been called */
        protected void extInit() {
            HierarchyEnumerator.CellInfo parent = getParentInfo();
            if (parent == null) mFactor = 1f;
            else mFactor = ((IRSIMCellInfo)parent).getMFactor();
            // get mfactor from instance we pushed into
            NodeInst ni = getContext().getNodeInst();
            if (ni == null) return;
            Variable mvar = ni.getVar("ATTR_M");
            if (mvar == null) return;
            Object mval = getContext().evalVar(mvar, null);
            if (mval == null) return;
            mFactor = mFactor * VarContext.objectToFloat(mval, 1f);
        }
        
        /** get mFactor */
        protected float getMFactor() { return mFactor; }
    }
        
}
