/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LENetlister.java
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
 * LENetlister.java
 *
 * Created on November 11, 2003, 3:56 PM
 */

package com.sun.electric.tool.logicaleffort;

import com.sun.electric.tool.logicaleffort.*;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.topology.*;
import com.sun.electric.database.prototype.*;
import com.sun.electric.database.variable.*;
import com.sun.electric.tool.Tool;

import java.util.prefs.*;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;
import java.awt.geom.AffineTransform;

/**
 * Creates a logical effort netlist to be sized by LESizer.
 * This is so the LESizer is independent of Electric's Database,
 * and can match George Chen's C++ version being developed for 
 * PNP.
 *
 * @author  gainsley
 */
public class LENetlister extends HierarchyEnumerator.Visitor {
    
    // ALL GATES SAME DELAY
    /** global step-up */                       private float su;
    /** wire to gate cap ratio */               private float wireRatio;
    /** convergence criteron */                 private float epsilon;
    /** max number of iterations */             private int maxIterations;
    /** gate cap, in fF/lambda */               private float gateCap;
    /** ratio of diffusion to gate cap */       private float alpha;
    /** ratio of keeper to driver size */       private float keeperRatio;
    
    /** LESizer to do sizing */                 private LESizer lesizer;
    /** Where to direct output */               private PrintStream out;
    /** Mapping between NodeInst and Instance */private HashMap instancesMap;
    
    /** Creates a new instance of LENetlister */
    public LENetlister(LESizer lesizer, OutputStream ostream) {
        // get preferences for this package
        Tool leTool = Tool.findTool("logical effort");
        su = leTool.getPrefs().getFloat(LETool.OPTION_GLOBALFANOUT, LETool.DEFAULT_GLOBALFANOUT);
		epsilon = leTool.getPrefs().getFloat(LETool.OPTION_EPSILON, LETool.DEFAULT_EPSILON);
		maxIterations = leTool.getPrefs().getInt(LETool.OPTION_MAXITER, LETool.DEFAULT_MAXITER);
		gateCap = leTool.getPrefs().getFloat(LETool.OPTION_GATECAP, LETool.DEFAULT_GATECAP);
		wireRatio = leTool.getPrefs().getFloat(LETool.OPTION_WIRERATIO, LETool.DEFAULT_WIRERATIO);
		alpha = leTool.getPrefs().getFloat(LETool.OPTION_DIFFALPHA, LETool.DEFAULT_DIFFALPHA);
		keeperRatio = leTool.getPrefs().getFloat(LETool.OPTION_KEEPERRATIO, LETool.DEFAULT_KEEPERRATIO);
        
        this.lesizer = lesizer;
        this.instancesMap = new HashMap();
        this.out = new PrintStream(ostream);
    }        
    
    /** NodeInst should be an LESettings instance */
    protected void useLESettings(NodeInst ni, VarContext context) {
        Variable var;
        if ((var = ni.getVar("ATTR_su")) != null) su = VarContext.objectToFloat(context.evalVar(var), su);
        if ((var = ni.getVar("ATTR_wire_ratio")) != null) wireRatio = VarContext.objectToFloat(context.evalVar(var), wireRatio);
        if ((var = ni.getVar("ATTR_epsilon")) != null) epsilon = VarContext.objectToFloat(context.evalVar(var), epsilon);
        if ((var = ni.getVar("ATTR_max_iter")) != null) maxIterations = VarContext.objectToInt(context.evalVar(var), maxIterations);
        if ((var = ni.getVar("ATTR_gate_cap")) != null) gateCap = VarContext.objectToFloat(context.evalVar(var), gateCap);
        if ((var = ni.getVar("ATTR_alpha")) != null) alpha = VarContext.objectToFloat(context.evalVar(var), alpha);
        if ((var = ni.getVar("ATTR_keeper_ratio")) != null) keeperRatio = VarContext.objectToFloat(context.evalVar(var), keeperRatio);
    }
        
    protected void netlist(Cell cell, VarContext context) {

        //ArrayList connectedPorts = new ArrayList();
        //connectedPorts.add(Schematics.tech.resistorNode.getPortsList());
        Netlist netlist = cell.getNetlist(true);
        
        // read schematic-specific sizing options
        for (Iterator instIt = cell.getNodes(); instIt.hasNext();) {
            NodeInst ni = (NodeInst)instIt.next();
            if (ni.getVar("ATTR_LESETTINGS") != null) {
                useLESettings(ni, context);            // get settings from object
                break;
            }
        }

        HierarchyEnumerator.enumerateCell(cell, context, netlist, this);
    }
    
    public void size() {
        //lesizer.printDesign();
        boolean verbose = true;
        lesizer.optimizeLoops((float)0.01, maxIterations, verbose, alpha, keeperRatio);
        //out.println("---------After optimization:------------");
        //lesizer.printDesign();
    }
    
    public void updateSizes() {
        Set allEntries = instancesMap.entrySet();
        for (Iterator it = allEntries.iterator(); it.hasNext();) {
            Map.Entry entry = (Map.Entry)it.next();
            Instance inst = (Instance)entry.getKey();
            NodeInst ni = (NodeInst)entry.getValue();
            String varName = "LEDRIVE_" + inst.getName();
            ni.newVar(varName, new Float(inst.getLeX()));
        }
    }
        
    public HashMap getInstancesMap() { return instancesMap; }

    public HierarchyEnumerator.CellInfo newCellInfo() { return new LECellInfo(); }

    public boolean enterCell(HierarchyEnumerator.CellInfo info) {
        ((LECellInfo)info).leInit();
        return true;
    }
    
    public void exitCell(HierarchyEnumerator.CellInfo info) {
    }
    
    public boolean visitNodeInst(Nodable ni, HierarchyEnumerator.CellInfo info) {
        float leX = (float)0.0;

        // check if leGate
        Instance.Type type = Instance.Type.NOTSIZEABLE;        
        if (ni.getVar("ATTR_LEGATE") != null) type = Instance.Type.LEGATE;
        else if (ni.getVar("ATTR_LEWIRE") != null) {
            // Note that if inst is an LEWIRE, it will have no 'le' attributes,
            // and those pins will have default 'le' values of one.
            // This creates an instance which has Type LEWIRE, but has
            // boolean leGate set to false; it will not be sized
            Variable var = ni.getVar("ATTR_L");
            float len = VarContext.objectToFloat(info.getContext().evalVar(var), (float)0.0);
            var = ni.getVar("ATTR_width");
            float width = VarContext.objectToFloat(info.getContext().evalVar(var), (float)3.0f);
            leX = (float)(0.95f*len + 0.05f*len*(width/3.0f))*wireRatio;  // equivalent lambda of gate
            leX = leX/9.0f;                         // drive strength X=1 is 9 lambda of gate
        }
        else if (ni.getVar("ATTR_LEKEEPER") != null) type = Instance.Type.LEKEEPER;
        else if (ni.getVar("ATTR_LESETTINGS") != null) return false;
        else return true;                           // descend into and process
        
        // build leGate instance
        //out.println("------------------------------------");
        ArrayList pins = new ArrayList();
        //Cell schCell = ni.getProtoEquivalent();
		Netlist netlist = info.getNetlist();
		for (Iterator ppIt = ni.getParent().getPorts(); ppIt.hasNext();) {
			PortProto pp = (PortProto)ppIt.next();
//      for (Iterator piIt = ni.getPortInsts(); piIt.hasNext();) {
//          PortInst pi = (PortInst)piIt.next();
//          PortProto pp = pi.getProtoEquivalent();
            Variable var = pp.getVar("ATTR_le");
            // Note default 'le' value should be one
            float le = VarContext.objectToFloat(info.getContext().evalVar(var), (float)1.0);
            String netName = info.getUniqueNetName(netlist.getNetwork(ni,pp,0), ".");
//            String netName = info.getUniqueNetName(pi.getNetwork(), ".");
            Pin.Dir dir = Pin.Dir.INPUT;
            //if (pp.getCharacteristic() == PortProto.Characteristic.IN) dir = Pin.Dir.INPUT;
            // if it's not an output, it doesn't really matter what it is.
            if (pp.getCharacteristic() == PortProto.Characteristic.OUT) dir = Pin.Dir.OUTPUT;
            pins.add(new Pin(pp.getProtoName(), dir, le, netName));
            //out.println("    Added "+dir+" pin "+pp.getProtoName()+", le: "+le+", netName: "+netName);
            if (type == Instance.Type.NOTSIZEABLE) break;    // this is LEWIRE, only add one pin of it
        }
        // create new leGate instance
        VarContext vc = info.getContext().push(ni);                   // to create unique flat name
        Instance inst = lesizer.addInstance(vc.getInstPath("."), type, su, leX, pins);
        //out.println("  Added instance "+vc.getInstPath(".")+" of type "+type);
        instancesMap.put(inst, ni);
        return false;
    }
            
    /** Logical Effort Cell Info class */
    public class LECellInfo extends HierarchyEnumerator.CellInfo {

        /** M-factor to be applied to size */       private float mFactor;

        /** initialize LECellInfo: assumes CellInfo.init() has been called */
        protected void leInit() {
            HierarchyEnumerator.CellInfo parent = getParentInfo();
            if (parent == null) mFactor = 1f;
            else mFactor = ((LECellInfo)parent).getMFactor();
            // get mfactor from instance we pushed into
            Nodable ni = getContext().getNodable();
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
    
    
    // ---- TEST STUFF -----  REMOVE LATER ----
    public static void test1() {
        LESizer.test1();
    }
    
}
