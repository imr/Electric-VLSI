/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LENetlister1.java
 * Written by Jonathan Gainsley, Sun Microsystems.
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.user.ErrorLogger;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Creates a logical effort netlist to be sized by LESizer.
 * This is so the LESizer is independent of Electric's Database,
 * and can match George Chen's C++ version being developed for 
 * PNP.
 *
 * @author  gainsley
 */
public class LENetlister1 extends LENetlister {
    
    // ALL GATES SAME DELAY
    /** Netlister constants */                  protected NetlisterConstants constants;

    /** all networks */                         private HashMap<String,Net> allNets;
    /** all instances (LEGATES, not loads) */   private HashMap<String,Instance> allInstances;

    /** Sizer */                                private LESizer sizer;
    /** Job we are part of */                   private Job job;
    /** Where to direct output */               private PrintStream out;
    /** Mapping between NodeInst and Instance */private List<Instance> instancesMap;

    /** True if we got aborted */               private boolean aborted;
    /** for logging errors */                   private ErrorLogger errorLogger;
    /** record definition errors so no multiple warnings */ private HashMap<Export,Export> lePortError;
    /** The top level cell netlisted */         private Cell topLevelCell;

    private static final boolean DEBUG = false;

    /** Creates a new instance of LENetlister */
    public LENetlister1(Job job) {
        // get preferences for this package
        Tool leTool = Tool.findTool("logical effort");
        constants = null;
        topLevelCell = null;

        allNets = new HashMap<String,Net>();
        allInstances = new HashMap<String,Instance>();

        this.job = job;
        this.instancesMap = new ArrayList<Instance>();
        this.lePortError = new HashMap<Export,Export>();
        this.out = new PrintStream((OutputStream)System.out);

        errorLogger = null;
        aborted = false;
    }
    
    // Entry point: This netlists the cell
    public boolean netlist(Cell cell, VarContext context, boolean useCaching) {

        //ArrayList connectedPorts = new ArrayList();
        //connectedPorts.add(Schematics.tech.resistorNode.getPortsList());
        assert errorLogger == null;
//        if (errorLogger != null) errorLogger.delete();
        errorLogger = ErrorLogger.newInstance("LE Netlister");

//        Netlist netlist = cell.getNetlist(true);

        // read schematic-specific sizing options
        constants = getSettings(cell);
        if (constants == null) {
            constants = new NetlisterConstants(cell.getTechnology());
            if (!saveSettings(constants, cell)) {
                // couldn't save settings to cell, abort
                return false;
            }
        }

        topLevelCell = cell;
        HierarchyEnumerator.enumerateCell(cell, context, this, true);
//        HierarchyEnumerator.enumerateCell(cell, context, netlist, this);
        if (aborted) return false;
        return true;
    }

    /**
     * Size the netlist.
     * @return true on success, false otherwise.
     */
    public boolean size(LESizer.Alg algorithm) {
        //lesizer.printDesign();
        boolean verbose = false;
        // create a new sizer
        sizer = new LESizer(algorithm, this, job, errorLogger);
        boolean success = sizer.optimizeLoops(constants.epsilon, constants.maxIterations, verbose,
                constants.alpha, constants.keeperRatio);
        //out.println("---------After optimization:------------");
        //lesizer.printDesign();
        // get rid of the sizer
        sizer = null;
        return success;
    }

    public void getSizes(List<Float> sizes, List<String> varNames, List<NodeInst> nodes, List<VarContext> contexts) {
        // iterator over all LEGATEs
        Set<Map.Entry<String,Instance>> allEntries = allInstances.entrySet();
        for (Map.Entry<String,Instance> entry : allEntries) {

            Instance inst = (Instance)entry.getValue();
            Nodable no = inst.getNodable();
            NodeInst ni = no.getNodeInst();
            if (ni != null) no = ni;

            if (!inst.isLeGate()) continue;

            String varName = "LEDRIVE_" + inst.getName();
            //no.newVar(varName, new Float(inst.getLeX()));
            //topLevelCell.newVar(varName, new Float(inst.getLeX()));
            sizes.add(new Float(inst.getLeX()));
            varNames.add(varName);
            nodes.add(ni);
            contexts.add(inst.getContext());
        }
    }

    public void done() {
        errorLogger.termLogging(true);
        //errorLogger = null;
    }

    public ErrorLogger getErrorLogger() { return errorLogger; }

    public void nullErrorLogger() { errorLogger = null; }

    public NetlisterConstants getConstants() { return constants; }

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
		float leX, ArrayList<Pin> pins, Nodable no)
	{
		if (allInstances.containsKey(name)) {
			out.println("Error: Instance "+name+" already exists.");
			return null;
		}
		// create instance
		Instance instance = new Instance(name, type, leSU, leX, no);

		// create each net if necessary, from pin.
		Iterator<Pin> iter = pins.iterator();
		while (iter.hasNext()) {
			Pin pin = iter.next();
			String netname = pin.getNetName();

			// check to see if net had already been added to the design
			Net net = allNets.get(netname);
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
    protected HashMap<String,Instance> getAllInstances() { return allInstances; }

    protected HashMap<String,Net> getAllNets() { return allNets; }

    /** return number of gates sized */
    protected int getNumGates() { return allInstances.size(); }

    protected LESizer getSizer() { return sizer; }

    protected float getKeeperRatio() { return constants.keeperRatio; }

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

        LECellInfo leinfo = (LECellInfo)info;
        leinfo.leInit(constants);

        // check if conflicting settings
        if (topLevelCell != info.getCell()) {
            if (isSettingsConflict(leinfo.getSettings(), topLevelCell, info.getContext(), info.getCell())) {
                aborted = true;
                return false;
            }
        }

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
        if ((var = getVar(ni, ATTR_LEGATE)) != null) {
            // assume it is LEGATE if can't resolve value
            int gate = VarContext.objectToInt(info.getContext().evalVar(var), 1);
            if (gate == 1)
                type = Instance.Type.LEGATE;
            else
                return true;
        }
        else if ((var = getVar(ni, ATTR_LEKEEPER)) != null) {
            // assume it is LEKEEPER if can't resolve value
            int gate = VarContext.objectToInt(info.getContext().evalVar(var), 1);
            if (gate == 1)
                type = Instance.Type.LEKEEPER;
            else
                return true;
        }
        else if (getVar(ni, ATTR_LEWIRE) != null) {
            type = Instance.Type.WIRE;
            // Note that if inst is an LEWIRE, it will have no 'le' attributes.
            // we therefore assign pins to have default 'le' values of one.
            // This creates an instance which has Type LEWIRE, but has
            // boolean leGate set to false; it will not be sized
            // NEW: If we find ATTR_LEWIRECAP, that is the capacitance to use,
            // and we will not calculate the cap from L and W.
            var = ni.getVar(ATTR_LEWIRECAP);
            float cap = 0;
            if (var != null) {
                cap = VarContext.objectToFloat(info.getContext().evalVar(var), 0.0f);
            } else {
                var = ni.getVar(ATTR_L);
                if (var == null) {
                    System.out.println("Error, no L attribute found on LEWIRE "+info.getContext().push(ni).getInstPath("."));
                }
                float len = VarContext.objectToFloat(info.getContext().evalVar(var), 0.0f);
                var = ni.getVar(Schematics.ATTR_WIDTH);
                if (var == null) {
                    System.out.println("Warning, no width attribute found on LEWIRE "+info.getContext().push(ni).getInstPath("."));
                }
                float width = VarContext.objectToFloat(info.getContext().evalVar(var), 3.0f);
                cap = (float)(0.95f*len + 0.05f*len*(width/3.0f));      // capacitance
            }
            leX = cap*constants.wireRatio/9.0f;     // drive strength X=1 is 9 lambda of gate
            wire = true;
        }
        else if ((ni.getProto() != null) && (ni.getProto().getFunction().isTransistor())) {
            // handle transistor loads
            type = Instance.Type.STATICGATE;
            var = ni.getVar(Schematics.ATTR_WIDTH);
            if (var == null) {
                System.out.println("Error: transistor "+ni+" has no width in Cell "+info.getCell());
                errorLogger.logError("Error: transistor "+ni+" has no width in Cell "+info.getCell(),
                	ni.getNodeInst(), info.getCell(), info.getContext(), 0);
                return false;
            }
            float width = VarContext.objectToFloat(info.getContext().evalVar(var), (float)3.0);

            // note that LE will handle any gate load bloat due to increased gate length
            leX = (float)(width/9.0f);
            primitiveTransistor = true;
        }
        else if ((ni.getProto() != null) && (ni.getProto().getFunction() == PrimitiveNode.Function.CAPAC)) {
            type = Instance.Type.CAPACITOR;
            var = ni.getVar(Schematics.SCHEM_CAPACITANCE);
            if (var == null) {
                System.out.println("Error: capacitor "+ni+" has no capacitance in Cell "+ni.getParent());
                //errorLogger.logError("Error: capacitor "+no+" has no capacitance in Cell "+info.getCell(), ni.getNodeInst(), true, no.getParent(), context, 0);
                return false;
            }
            float cap = VarContext.objectToFloat(info.getContext().evalVar(var), (float)0.0);
            leX = (float)(cap/constants.gateCap/1e-15/9.0f);
        }
        else if (ni.getVar(ATTR_LESETTINGS) != null)
            return false;
        else if (ni.getVar(ATTR_LEIGNORE) != null)
            return false;


        if (type == null) return true;              // descend into and process

        if (DEBUG) System.out.println("------------------------------------");

        // If got to this point, this is either an LEGATE or an LEWIRE
        // Both require us to build an instance.
        ArrayList<Pin> pins = new ArrayList<Pin>();
		Netlist netlist = info.getNetlist();
		for (Iterator<PortProto> ppIt = ni.getProto().getPorts(); ppIt.hasNext();) {
			PortProto pp = ppIt.next();
            // Note default 'le' value should be one
            float le = getLE(ni, type, pp, info);
            String netName = info.getUniqueNetName(info.getNetID(netlist.getNetwork(ni,pp,0)), ".");
            Pin.Dir dir = Pin.Dir.INPUT;
            // if it's not an output, it doesn't really matter what it is.
            if (pp.getCharacteristic() == PortCharacteristic.OUT) dir = Pin.Dir.OUTPUT;
            if (primitiveTransistor) {
                // primitive Electric Transistors have their source and drain set to BIDIR, we
                // want them set to OUTPUT so that they count as diffusion capacitance
                if (pp.getCharacteristic() == PortCharacteristic.BIDIR) dir = Pin.Dir.OUTPUT;
            }
            if (dir == Pin.Dir.INPUT && type == Instance.Type.STATICGATE) {
                // gate load: check if length > 2, if so, increase LE to account for added capacitance
                var = ni.getVar(Schematics.ATTR_LENGTH);
                if (var == null) {
                    System.out.println("Error: transistor "+ni+" has no length in Cell "+info.getCell());
                    errorLogger.logError("Error: transistor "+ni+" has no length in Cell "+info.getCell(),
                    	ni.getNodeInst(), info.getCell(), info.getContext(), 0);
                }
                float length = VarContext.objectToFloat(info.getContext().evalVar(var), (float)2.0);
                // not exactly correct because assumes all cap is area cap, which it isn't
                if (length != 2.0f)
                    le = le * length / 2.0f;
            }
            pins.add(new Pin(pp.getName(), dir, le, netName));
            if (DEBUG) System.out.println("    Added "+dir+" pin "+pp.getName()+", le: "+le+", netName: "+netName+", Network: "+netlist.getNetwork(ni,pp,0));
            if (type == Instance.Type.WIRE) break;    // this is LEWIRE, only add one pin of it
        }

        // see if passed-down step-up exists
        float localsu = constants.su;
        if (((LECellInfo)info).getSU() != -1f)
            localsu = ((LECellInfo)info).getSU();
        // check for step-up on gate
        var = ni.getVar(ATTR_su);
        if (var != null) {
            float nisu = VarContext.objectToFloat(info.getContext().evalVar(var), -1f);
            if (nisu != -1f)
                localsu = nisu;
        }

        // create new leGate instance
        VarContext vc = info.getContext().push(ni);                   // to create unique flat name
        Instance inst = addInstance(vc.getInstPath("."), type, localsu, leX, pins, ni);
        inst.setContext(info.getContext());

        // set instance parameters for sizeable gates
        if (type == Instance.Type.LEGATE || type == Instance.Type.LEKEEPER) {
            var = ni.getVar(ATTR_LEPARALLGRP);
            if (var != null) {
                // set parallel group number
                int g = VarContext.objectToInt(info.getContext().evalVar(var), 0);
                inst.setParallelGroup(g);
            }
        }
        // set mfactor
        float parentM = ((LECellInfo)info).getMFactor();
        inst.setMfactor(parentM);
        var = LETool.getMFactor(ni);
        if (var != null) {
            // set mfactor
            float m = VarContext.objectToFloat(info.getContext().evalVar(var), 1.0f);
            m = m * parentM;
            inst.setMfactor(m);
        }

        if (DEBUG) {
            if (wire) System.out.println("  Added LEWire "+vc.getInstPath(".")+", X="+leX);
            else System.out.println("  Added instance "+vc.getInstPath(".")+" of type "+type+", X="+leX);
        }
        instancesMap.add(inst);
        return false;
    }

    private float getLE(Nodable ni, Instance.Type type, PortProto pp, HierarchyEnumerator.CellInfo info) {
        boolean leFound = false;
        // Note default 'le' value should be one
        float le = 1.0f;
		if (!(pp instanceof Export))
			return le;
        Variable var = ((Export)pp).getVar(ATTR_le);
        if (var != null) {
            leFound = true;
            le = VarContext.objectToFloat(info.getContext().evalVar(var), 1.0f);
        } else if ((pp.getCharacteristic() == PortCharacteristic.OUT) &&
                (type == Instance.Type.LEGATE || type == Instance.Type.LEKEEPER)) {
            // if this is an Sizeable gate's output, look for diffn and diffp
            float diff = 0;
            var = ((Export)pp).getVar(ATTR_diffn);
            if (var != null) {
                diff += VarContext.objectToFloat(info.getContext().evalVar(var), 0);
                leFound = true;
            }
            var = ((Export)pp).getVar(ATTR_diffp);
            if (var != null) {
                diff += VarContext.objectToFloat(info.getContext().evalVar(var), 0);
                leFound = true;
            }
            le = diff/3.0f;
        }
        if (!leFound && (type == Instance.Type.LEGATE || type == Instance.Type.LEKEEPER)) {
            Cell cell = (Cell)ni.getProto();
            Export exp = cell.findExport(pp.getName());
            if (exp != null && lePortError.get(exp) == null) {
                String msg = "Warning: Sizeable gate has no logical effort specified for port "+pp.getName()+" in "+cell;
                System.out.println(msg);
                errorLogger.logWarning(msg, exp, cell, info.getContext().push(ni), 0);
                lePortError.put(exp, exp);
            }
        }
        return le;
    }

    private Variable getVar(Nodable no, Variable.Key key) {
        Variable var = no.getParameter(key);
        //if (var == null) var = no.getVarDefaultOwner().getVar(key);
        return var;
    }

    public void doneVisitNodeInst(Nodable ni, HierarchyEnumerator.CellInfo info) {}

    /**
     * Nothing to do for exitCell
     */
    public void exitCell(HierarchyEnumerator.CellInfo info) {
    }

    /**
     * Logical Effort Cell Info class.  Keeps track of:
     * <p>- M factors
     */
    public class LECellInfo extends LENetlister.LECellInfo {

    }



    // =============================== Statistics ==================================


    public void printStatistics() {
        Collection<Instance> instances = getAllInstances().values();
        float totalsize = 0f;
        float instsize = 0f;
        int numLEGates = 0;
        int numLEWires = 0;
        for (Instance inst : instances) {
            totalsize += inst.getLeX();
            if (inst.getType() == Instance.Type.LEGATE || inst.getType() == Instance.Type.LEKEEPER) {
                numLEGates++;
                instsize += inst.getLeX();
            }
            if (inst.getType() == Instance.Type.WIRE)
                numLEWires++;
        }
        System.out.println("Number of LEGATEs: "+numLEGates);
        System.out.println("Number of Wires: "+numLEWires);
        System.out.println("Total size of all LEGATEs: "+instsize);
        System.out.println("Total size of all instances (sized and loads): "+totalsize);
    }

    public float getTotalLESize() {
        return getTotalSize(Instance.Type.LEGATE) + getTotalSize(Instance.Type.LEKEEPER);
    }

    /**
     * return total size of all instances of the specified type
     * if type is null, uses all types
     */
    public float getTotalSize(Instance.Type type) {
        Collection<Instance> instances = getAllInstances().values();
        float totalsize = 0f;
        for (Instance inst : instances) {
            if (type == null)
                totalsize += inst.getLeX();
            else if (inst.getType() == type)
                totalsize += inst.getLeX();
        }
        return totalsize;
    }

    public boolean printResults(Nodable no, VarContext context) {
        // if this is a NodeInst, convert to Nodable
        if (no instanceof NodeInst) {
            no = Netlist.getNodableFor((NodeInst)no, 0);
        }
        Instance inst = null;
        for (Instance instance : instancesMap) {
            if (instance.getNodable() == no) {
                if (instance.getContext().getInstPath(".").equals(context.getInstPath("."))) {
                    inst = instance;
                    break;
                }
            }
        }
        if (inst == null) return false;                 // failed

//        MessagesWindow msgs = TopLevel.getMessagesWindow();
        //Font oldFont = msgs.getFont();
        //msgs.setFont(new Font("Courier", Font.BOLD, oldFont.getSize()));

        // print netlister info
        System.out.println("Netlister: Gate Cap="+constants.gateCap+", Alpha="+constants.alpha);

        // print instance info
        inst.print();

        // collect info about what is driven
        Pin out = inst.getOutputPins().get(0);
        Net net = out.getNet();

        ArrayList<Pin> gatesDrivenPins = new ArrayList<Pin>();
        ArrayList<Pin> loadsDrivenPins = new ArrayList<Pin>();
        ArrayList<Pin> wiresDrivenPins = new ArrayList<Pin>();
        ArrayList<Pin> gatesFightingPins = new ArrayList<Pin>();

        for (Pin pin : net.getAllPins()) {
            Instance in = pin.getInstance();
            if (pin.getDir() == Pin.Dir.INPUT) {
                if (in.isGate()) gatesDrivenPins.add(pin);
                //if (in.getType() == Instance.Type.STATICGATE) staticGatesDriven.add(in);
                if (in.getType() == Instance.Type.LOAD) loadsDrivenPins.add(pin);
                if (in.getType() == Instance.Type.CAPACITOR) loadsDrivenPins.add(pin);
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
        for (Pin pin : gatesDrivenPins) {
            totalLoad += pin.getInstance().printLoadInfo(pin, constants.alpha);
        }
        System.out.println("  -------------------- Loads Driven ("+loadsDrivenPins.size()+") --------------------");
        for (Pin pin : loadsDrivenPins) {
            totalLoad += pin.getInstance().printLoadInfo(pin, constants.alpha);
        }
        System.out.println("  -------------------- Wires Driven ("+wiresDrivenPins.size()+") --------------------");
        for (Pin pin : wiresDrivenPins) {
            totalLoad += pin.getInstance().printLoadInfo(pin, constants.alpha);
        }
        System.out.println("  -------------------- Gates Fighting ("+gatesFightingPins.size()+") --------------------");
        for (Pin pin : gatesFightingPins) {
            totalLoad += pin.getInstance().printLoadInfo(pin, constants.alpha);
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
