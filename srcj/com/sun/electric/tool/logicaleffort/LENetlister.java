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
 * Created on November 11, 2003, 3:56 PM
 */

package com.sun.electric.tool.logicaleffort;

import com.sun.electric.tool.logicaleffort.*;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.topology.*;
import com.sun.electric.database.prototype.*;
import com.sun.electric.database.variable.*;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ui.MessagesWindow;
import com.sun.electric.tool.user.ui.TopLevel;
import com.sun.electric.technology.PrimitiveNode;

import java.awt.geom.AffineTransform;
import java.awt.*;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;

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

    /** all networks */                         private HashMap allNets;
    /** all instances (LEGATES, not loads) */   private HashMap allInstances;

    /** The algorithm to run */                 private LESizer.Alg algorithm;
    /** Sizer */                                private LESizer sizer;
    /** Job we are part of */                   private Job job;
    /** Where to direct output */               private PrintStream out;
    /** Mapping between NodeInst and Instance */private HashMap instancesMap;

    /** True if we got aborted */               private boolean aborted;

    private static final boolean DEBUG = false;

    /** Creates a new instance of LENetlister */
    public LENetlister(LESizer.Alg algorithm, Job job) {
        // get preferences for this package
        Tool leTool = Tool.findTool("logical effort");
        su = (float)LETool.getGlobalFanout();
        epsilon = (float)LETool.getConvergenceEpsilon();
        maxIterations = LETool.getMaxIterations();
        gateCap = (float)LETool.getGateCapacitance();
        wireRatio = (float)LETool.getWireRatio();
        alpha = (float)LETool.getDiffAlpha();
        keeperRatio = (float)LETool.getKeeperRatio();
        
        allNets = new HashMap();
        allInstances = new HashMap();

        this.algorithm = algorithm;
        this.job = job;
        this.instancesMap = new HashMap();
        this.out = new PrintStream((OutputStream)System.out);

        aborted = false;
    }
    
    // Entry point: This netlists the cell
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

    /**
     * Size the netlist.
     * @return true on success, false otherwise.
     */
    public boolean size() {
        //lesizer.printDesign();
        boolean verbose = false;
        // create a new sizer
        sizer = new LESizer(algorithm, this, job);
        boolean success = sizer.optimizeLoops(epsilon, maxIterations, verbose, alpha, keeperRatio);
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
        Set allEntries = allInstances.entrySet();
        for (Iterator it = allEntries.iterator(); it.hasNext();) {

            Map.Entry entry = (Map.Entry)it.next();
            Instance inst = (Instance)entry.getValue();
            Nodable no = inst.getNodable();

            String varName = "LEDRIVE_" + inst.getName();
            no.newVar(varName, new Float(inst.getLeX()));
            /*
            if (no instanceof NodeInst) {
                ((NodeInst)no).newVar(varName, new Float(inst.getLeX()));
            } else {
                System.out.println("Can't handle NodeInst Proxies on update sizes yet");
            }*/
        }
    }

    /** NodeInst should be an LESettings instance */
    private void useLESettings(NodeInst ni, VarContext context) {
        Variable var;
        if ((var = ni.getVar("ATTR_su")) != null) su = VarContext.objectToFloat(context.evalVar(var), su);
        if ((var = ni.getVar("ATTR_wire_ratio")) != null) wireRatio = VarContext.objectToFloat(context.evalVar(var), wireRatio);
        if ((var = ni.getVar("ATTR_epsilon")) != null) epsilon = VarContext.objectToFloat(context.evalVar(var), epsilon);
        if ((var = ni.getVar("ATTR_max_iter")) != null) maxIterations = VarContext.objectToInt(context.evalVar(var), maxIterations);
        if ((var = ni.getVar("ATTR_gate_cap")) != null) gateCap = VarContext.objectToFloat(context.evalVar(var), gateCap);
        if ((var = ni.getVar("ATTR_alpha")) != null) alpha = VarContext.objectToFloat(context.evalVar(var), alpha);
        if ((var = ni.getVar("ATTR_keeper_ratio")) != null) keeperRatio = VarContext.objectToFloat(context.evalVar(var), keeperRatio);
    }

    /**
	 * Add new instance to design
	 * @param name name of the instance
	 * param leGate true if this is an LEGate
	 * @param leX size
	 * @param pins list of pins on instance
	 *
	 * @return the new instance added, null if error
	 */
	protected Instance addInstance(String name, Instance.Type type, float leSU,
		float leX, ArrayList pins, Nodable no)
	{
		if (allInstances.containsKey(name)) {
			out.println("Error: Instance "+name+" already exists.");
			return null;
		}
		// create instance
		Instance instance = new Instance(name, type, leSU, leX, no);

		// create each net if necessary, from pin.
		Iterator iter = pins.iterator();
		while (iter.hasNext()) {
			Pin pin = (Pin)iter.next();
			String netname = pin.getNetName();

			// check to see if net had already been added to the design
			Net net = (Net)allNets.get(netname);
			if (net != null) {
				pin.setNet(net);
				pin.setInstance(instance);
				net.addPin(pin);
			} else {
				// create new net
				net = new Net(netname);
				allNets.put(netname, net);
				pin.setNet(net);
				pin.setInstance(instance);
				net.addPin(pin);
			}
		}
		instance.setPins(pins);

		allInstances.put(name, instance);
		return instance;
	}

    //public HashMap getInstancesMap() { return instancesMap; }
    protected HashMap getAllInstances() { return allInstances; }

    protected HashMap getAllNets() { return allNets; }

    /** return number of gates sized */
    protected int getNumGates() { return allInstances.size(); }

    protected LESizer getSizer() { return sizer; }

    protected float getKeeperRatio() { return keeperRatio; }

    // ======================= Hierarchy Enumerator ==============================

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

        ((LECellInfo)info).leInit();
        return true;
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
        float leX = (float)0.0;
        boolean wire = false;
        boolean primitiveTransistor = false;

        // Check if this NodeInst is tagged as a logical effort node
        Instance.Type type = null;
        Variable var = null;
        if ((var = ni.getVar("ATTR_LEGATE")) != null) {
            // assume it is LEGATE if can't resolve value
            int gate = VarContext.objectToInt(info.getContext().evalVar(var), 1);
            if (gate == 1)
                type = Instance.Type.LEGATE;
            else
                type = Instance.Type.STATICGATE;
        }
        else if ((var = ni.getVar("ATTR_LEKEEPER")) != null) {
            // assume it is LEKEEPER if can't resolve value
            int gate = VarContext.objectToInt(info.getContext().evalVar(var), 1);
            if (gate == 1)
                type = Instance.Type.LEKEEPER;
            else
                type = Instance.Type.STATICGATE;
        }
        else if (ni.getVar("ATTR_LEWIRE") != null) {
            type = Instance.Type.WIRE;
            // Note that if inst is an LEWIRE, it will have no 'le' attributes.
            // we therefore assign pins to have default 'le' values of one.
            // This creates an instance which has Type LEWIRE, but has
            // boolean leGate set to false; it will not be sized
            var = ni.getVar("ATTR_L");
            float len = VarContext.objectToFloat(info.getContext().evalVar(var), 0.0f);
            var = ni.getVar("ATTR_width");
            float width = VarContext.objectToFloat(info.getContext().evalVar(var), 3.0f);
            leX = (float)(0.95f*len + 0.05f*len*(width/3.0f))*wireRatio;  // equivalent lambda of gate
            leX = leX/9.0f;                         // drive strength X=1 is 9 lambda of gate
            wire = true;
        }
        else if ((ni.getProto() != null) && (ni.getProto().getFunction().isTransistor())) {
            // handle transistor loads
            type = Instance.Type.STATICGATE;
            var = ni.getVar("ATTR_width");
            if (var == null) {
                System.out.println("Error: transistor "+ni+" has no width in Cell "+info.getCell());
                return false;
            }
            float width = VarContext.objectToFloat(info.getContext().evalVar(var), (float)3.0);
            var = ni.getVar("ATTR_length");
            if (var == null) {
                System.out.println("Error: transistor "+ni+" has no length in Cell "+info.getCell());
                return false;
            }
            float length = VarContext.objectToFloat(info.getContext().evalVar(var), (float)2.0);
            // not exactly correct because assumes all cap is area cap, which it isn't
            leX = (float)(width*length/2.0f);
            leX = leX/9.0f;
            primitiveTransistor = true;
        }
        else if (ni.getVar("ATTR_LESETTINGS") != null)
            return false;


        if (type == null) return true;              // descend into and process

        if (DEBUG) System.out.println("------------------------------------");

        // If got to this point, this is either an LEGATE or an LEWIRE
        // Both require us to build an instance.
        ArrayList pins = new ArrayList();
		Netlist netlist = info.getNetlist();
		for (Iterator ppIt = ni.getProto().getPorts(); ppIt.hasNext();) {
			PortProto pp = (PortProto)ppIt.next();
            var = pp.getVar("ATTR_le");
            // Note default 'le' value should be one
            float le = VarContext.objectToFloat(info.getContext().evalVar(var), (float)1.0);
            String netName = info.getUniqueNetName(info.getNetID(netlist.getNetwork(ni,pp,0)), ".");
            Pin.Dir dir = Pin.Dir.INPUT;
            // if it's not an output, it doesn't really matter what it is.
            if (pp.getCharacteristic() == PortProto.Characteristic.OUT) dir = Pin.Dir.OUTPUT;
            if (primitiveTransistor) {
                // primitive Electric Transistors have their source and drain set to BIDIR, we
                // want them set to OUTPUT so that they count as diffusion capacitance
                if (pp.getCharacteristic() == PortProto.Characteristic.BIDIR) dir = Pin.Dir.OUTPUT;
            }
            pins.add(new Pin(pp.getName(), dir, le, netName));
            if (DEBUG) System.out.println("    Added "+dir+" pin "+pp.getName()+", le: "+le+", netName: "+netName+", JNetwork: "+netlist.getNetwork(ni,pp,0));
            if (type == Instance.Type.WIRE) break;    // this is LEWIRE, only add one pin of it
        }

        // see if passed-down step-up exists
        float localsu = su;
        if (((LECellInfo)info).getSU() != -1f)
            localsu = ((LECellInfo)info).getSU();
        // check for step-up on gate
        var = ni.getVar("ATTR_su");
        if (var != null) {
            float nisu = VarContext.objectToFloat(info.getContext().evalVar(var), -1f);
            if (nisu != -1f)
                localsu = nisu;
        }

        // create new leGate instance
        VarContext vc = info.getContext().push(ni);                   // to create unique flat name
        Instance inst = addInstance(vc.getInstPath("."), type, localsu, leX, pins, ni);

        // set instance parameters for sizeable gates
        if (type == Instance.Type.LEGATE) {
            var = ni.getVar("ATTR_LEPARALLGRP");
            if (var != null) {
                // set parallel group number
                int g = VarContext.objectToInt(info.getContext().evalVar(var), 0);
                inst.setParallelGroup(g);
            }
        }
        // set mfactor
        var = ni.getVar("ATTR_M");
        if (var != null) {
            // set mfactor
            int m = VarContext.objectToInt(info.getContext().evalVar(var), 1);
            inst.setMfactor(m * ((LECellInfo)info).getMFactor());
        }

        if (DEBUG) {
            if (wire) System.out.println("  Added LEWire "+vc.getInstPath(".")+", X="+leX);
            else System.out.println("  Added instance "+vc.getInstPath(".")+" of type "+type);
        }
        instancesMap.put(ni, inst);
        return false;
    }
            
    /**
     * Nothing to do for exitCell
     */
    public void exitCell(HierarchyEnumerator.CellInfo info) {
    }

    /**
     * Logical Effort Cell Info class.  Keeps track of:
     * <p>- M factors
     */
    public class LECellInfo extends HierarchyEnumerator.CellInfo {

        /** M-factor to be applied to size */       private float mFactor;
        /** SU to be applied to gates in cell */    private float cellsu;

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
            Variable mvar = ni.getVar("ATTR_M");
            if (mvar != null) {
                Object mval = getContext().evalVar(mvar, null);
                if (mval != null)
                    mFactor = mFactor * VarContext.objectToFloat(mval, 1f);
            }

            // get su from instance we pushed into
            Variable suvar = ni.getVar("ATTR_su");
            if (suvar != null) {
                float su = VarContext.objectToFloat(getContext().evalVar(suvar, null), -1f);
                if (su != -1f) cellsu = su;
            }
        }
        
        /** get mFactor */
        protected float getMFactor() { return mFactor; }

        protected float getSU() { return cellsu; }
    }



    // =============================== Statistics ==================================

    public void printStatistics() {
        System.out.println("Total size of all instances (sized and loads): "+ getTotalSize());
    }

    /**
     * return total size of all sized gates
     */
    protected float getTotalSize() {
        Collection instances = getAllInstances().values();
        float totalsize = 0f;
        for (Iterator it = instances.iterator(); it.hasNext();) {
            Instance inst = (Instance)it.next();
            totalsize += inst.getLeX();
        }
        return totalsize;
    }

    public boolean printResults(Nodable no) {
        // if this is a NodeInst, convert to Nodable
        if (no instanceof NodeInst) {
            no = Netlist.getNodableFor((NodeInst)no, 0);
        }
        Instance inst = (Instance)instancesMap.get(no);
        if (inst == null) return false;                 // failed

        MessagesWindow msgs = TopLevel.getMessagesWindow();
        //Font oldFont = msgs.getFont();
        //msgs.setFont(new Font("Courier", Font.BOLD, oldFont.getSize()));

        // print netlister info
        System.out.println("Netlister: Gate Cap="+gateCap+", Alpha="+alpha);

        // print instance info
        inst.print();

        // collect info about what is driven
        Pin out = (Pin)inst.getOutputPins().get(0);
        Net net = out.getNet();

        ArrayList gatesDrivenPins = new ArrayList();
        ArrayList loadsDrivenPins = new ArrayList();
        ArrayList wiresDrivenPins = new ArrayList();
        ArrayList gatesFightingPins = new ArrayList();

        for (Iterator it = net.getAllPins().iterator(); it.hasNext(); ) {
            Pin pin = (Pin)it.next();
            Instance in = pin.getInstance();
            if (pin.getDir() == Pin.Dir.INPUT) {
                if (in.isGate()) gatesDrivenPins.add(pin);
                //if (in.getType() == Instance.Type.STATICGATE) staticGatesDriven.add(in);
                if (in.getType() == Instance.Type.LOAD) loadsDrivenPins.add(pin);
                if (in.getType() == Instance.Type.WIRE) wiresDrivenPins.add(pin);
            }
            if (pin.getDir() == Pin.Dir.OUTPUT) {
                if (in.isGate()) gatesFightingPins.add(pin);
            }
        }
        System.out.println("Note: Load = Size * LE * M");
        System.out.println("Note: Load = Size * LE * M * Alpha, for Gates Fighting");

        float totalLoad = 0f;
        System.out.println("  -------------------- Gates Driven ("+gatesDrivenPins.size()+") --------------------");
        for (Iterator it = gatesDrivenPins.iterator(); it.hasNext(); ) {
            Pin pin = (Pin)it.next(); totalLoad += pin.getInstance().printLoadInfo(pin, alpha);
        }
        System.out.println("  -------------------- Loads Driven ("+loadsDrivenPins.size()+") --------------------");
        for (Iterator it = loadsDrivenPins.iterator(); it.hasNext(); ) {
            Pin pin = (Pin)it.next(); totalLoad += pin.getInstance().printLoadInfo(pin, alpha);
        }
        System.out.println("  -------------------- Wires Driven ("+wiresDrivenPins.size()+") --------------------");
        for (Iterator it = wiresDrivenPins.iterator(); it.hasNext(); ) {
            Pin pin = (Pin)it.next(); totalLoad += pin.getInstance().printLoadInfo(pin, alpha);
        }
        System.out.println("  -------------------- Gates Fighting ("+gatesFightingPins.size()+") --------------------");
        for (Iterator it = gatesFightingPins.iterator(); it.hasNext(); ) {
            Pin pin = (Pin)it.next(); totalLoad += pin.getInstance().printLoadInfo(pin, alpha);
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
