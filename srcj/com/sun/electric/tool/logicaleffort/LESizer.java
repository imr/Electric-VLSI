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
 * LESizer.java
 *
 * Created on November 11, 2003, 4:42 PM
 */

package com.sun.electric.tool.logicaleffort;

import com.sun.electric.tool.logicaleffort.*;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.tool.Job;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Collection;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.OutputStream;

/**
 * LESizer sizes an LENetlist. The LENetlist is generated by LENetlister from
 * the Electric database, or perhaps read in from a Spice file(?)
 *
 * NOTE: the only 'Electric' objects used are in LENetlister,
 * any objects referenced in this file are from the logicaleffort
 * package, although their names may imply otherwise.  Their names
 * are as such because their names match PNP's naming scheme.
 *
 * @author  gainsley
 */
public class LESizer {
    
    /** all networks */                             private HashMap allNets;
    /** all instances (LEGATES, not loads) */       private HashMap allInstances;
    /** which algorithm to use */                   private Alg optimizationAlg;
    /** Where to direct output */                   private PrintStream out;
   
    /** Alg is a typesafe enum class that describes the algorithm to be used */
    protected static class Alg {
        private final String name;
        private Alg(String name) { this.name = name; }
        public String toString() { return name; }
        
        /** The simple algorithm, ignores all parasitics */
        protected static final Alg SIMPLE = new Alg("simple");
        /** Algorithm includes capacitive side loads */
        protected static final Alg CAPLOADS = new Alg("caploads");
        /** Algorithm includes capacitive and resistive loads in tree form, but ignores mesh structures */
        protected static final Alg RCTREE = new Alg("rctree");
        /** Algorithm includes capacitive and resistive loads in both tree and mesh topologies */
        protected static final Alg RCMESH = new Alg("rcmesh");
    }
        
    /** Creates a new instance of LESizer */
    protected LESizer(OutputStream ostream) {
        allNets = new HashMap();
        allInstances = new HashMap();
        optimizationAlg = Alg.SIMPLE;
        
        out = new PrintStream(ostream);
    }
   
    /** Set the optimization type */
    public void setAlg(Alg alg) { optimizationAlg = alg; }
    
    /** Get the optimization type */
    public Alg getAlg() { return optimizationAlg; }
    
    /** Clear LE data */
    public void clear() {
        allNets.clear();
        allInstances.clear();
    }
    
    /** 
	 * Add new instance to design
	 * @param name name of the instance
	 * @param leGate true if this is an LEGate
	 * @param leX size
	 * @param pins list of pins on instance
	 *
	 * @return the new instance added, null if error
	 */
	protected Instance addInstance(String name, Instance.Type type, float leSU, 
		float leX, ArrayList pins)
	{
		if (allInstances.containsKey(name)) {
			out.println("Error: Instance "+name+" already exists.");
			return null;
		}
		// create instance
		Instance instance = new Instance(name, type, leSU, leX);
		
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

	/** 
	 * Optimize using loop algorithm;
	 * @param maxDeltaX maximum tolerance allowed in X
	 * @param N maximum number of loops
	 * @param verbose print out size information for each optimization loop
	 *
	 * Optimization will stop when the difference in sizes (X) is 
	 * less than maxDeltaX, or when N iterations have occurred.
	 */
	protected void optimizeLoops(float maxDeltaX, int N, boolean verbose, 
        float alpha, float keeperRatio)
	{
		// iterate through all the instances, updating sizes

		float currentLoopDeltaX = maxDeltaX + 1;	// force at least one iteration
        long startTime;
        
		int loopcount = 0;
		
		while ((currentLoopDeltaX > maxDeltaX) && (loopcount < N)) {
			currentLoopDeltaX = 0;
            startTime = System.currentTimeMillis();
            System.out.print("  Iteration "+loopcount);
            if (verbose) System.out.println(":");
            
			// iterate through each instance
			Iterator instancesIter = allInstances.values().iterator();
			Iterator netsIter = allNets.values().iterator();
			
			while (instancesIter.hasNext()) {
				Instance instance = (Instance)instancesIter.next();
				String instanceName = instance.getName();
				
				// make sure it is a sizeable gate
				if (instance.getLeGate()) {
					
					// iterate through all nets connected to all output pins
					ArrayList outputPins = instance.getOutputPins();
                    if (outputPins.size() != 1) {
                        // error
                        continue;
                    }
                    Pin outputPin = (Pin)outputPins.get(0);                    
                    Net net = outputPin.getNet();
                    
                    // now find all pins connected to this net
                    ArrayList netpins = net.getAllPins();
						
                    // compute total le*X (totalcap)
                    float totalcap = 0;
                    Iterator netpinsIter = netpins.iterator();
                    while (netpinsIter.hasNext()) {
                        Pin netpin = (Pin)netpinsIter.next();
                        Instance netpinInstance = netpin.getInstance();
                        float load = netpinInstance.getLeX() * netpin.getLE();
                        if (netpin.getDir() == Pin.Dir.OUTPUT) load *= alpha;
                        totalcap += load;
                    }
						
                    float newX = totalcap / instance.getLeSU();
                    float currentX = instance.getLeX();
                    if (currentX == 0) currentX = 0.001f;
                    float deltaX = Math.abs( (newX-currentX)/currentX);
                    currentLoopDeltaX = (deltaX > currentLoopDeltaX) ? deltaX : currentLoopDeltaX;
                    if (verbose) {
                        out.println("Optimized "+instanceName+": size:  "+
                            Variable.truncate(new Float(instance.getLeX()))+
                            "x ==> "+Variable.truncate(new Float(newX))+"x");
                    }
                    instance.setLeX(newX);

                } // if (leGate)

			} // while (instancesIter)
            String elapsed = Job.getElapsedTime(System.currentTimeMillis()-startTime);
            System.out.println("  ...done ("+elapsed+"), delta: "+currentLoopDeltaX);            
            if (verbose) System.out.println("-----------------------------------");
			loopcount++;

		} // while (currentLoopDeltaX ... )

	}
	   
    /**
     * Dump the design information for debugging purposes
     */
    protected void printDesign() 
	{
		out.println("Instances in design are:");
		        
		Iterator instancesIter = allInstances.values().iterator();
		while (instancesIter.hasNext()) {
			Instance instance = (Instance)instancesIter.next();
			String instanceName = instance.getName();
            StringBuffer buf = new StringBuffer();
			out.println("\t"+instanceName+" ==> "+Variable.truncate(new Float(instance.getLeX()))+"x");
			ArrayList pins = instance.getAllPins();
			
			// now print out pinname ==> netname
			Iterator pinsIter = pins.iterator();
			while (pinsIter.hasNext()) {
				Pin pin = (Pin)pinsIter.next();
				out.println("\t\t"+pin.getName()+" ==> "+pin.getNetName());
			}
		}
	}

    /**
     * Generate simple size file (for regression purposes)
     * @param filename output filename
     */
    protected int printDesignSizes(String filename)
	{
		// open output file
		try {
			FileWriter fileWriter = new FileWriter(filename); // throws IOException
			
			// iterate through all instances
			Iterator instancesIter = allInstances.values().iterator();
			while (instancesIter.hasNext()) {
				Instance instance = (Instance)instancesIter.next();
				String instanceName = instance.getName();
				float leX = instance.getLeX();
				fileWriter.write(instanceName+" "+leX+"\n"); // throws IOException
				fileWriter.flush(); // throws IOException
			}
			fileWriter.close(); // throws IOException

		} catch (IOException e) {
			out.println("Writing to file "+filename+": "+e.getMessage());
			return 1;
		}
		return 0;
	}

    /**
     * Generate SKILL backannotation file
     * @param filename output filename
     * @param libname  The Opus library name to be annotated
     * @param cellname  The Opus cell to be annotated
     */
    protected int printDesignSkill(String filename, String libname, String cellname)
	{
		// nothing here
		return 0;
	}

    /**
     * Dummy routine to improve test coverage
     */
    protected void testcoverage()
	{
		// nothing here
	}
   
    /** return number of gates sized */
    protected int getNumGates() { return allInstances.size(); }
    
    /** return total size of all sized gates */
    protected float getTotalSize() {
        Collection instances = allInstances.values();
        float totalsize = 0f;
        for (Iterator it = instances.iterator(); it.hasNext();) {
            Instance inst = (Instance)it.next();
            totalsize += inst.getLeX();
        }
        return totalsize;
    }
    
    //---------------------------------------TEST---------------------------------------
    //---------------------------------------TEST---------------------------------------
    
    /** run a contrived test */
    public static void test1()
    {
        System.out.println("Running GASP test circuit");
        System.out.println("=========================");
        
        float su = (float)4.0;
        LESizer lesizer = new LESizer((OutputStream)System.out);
                
        {
        // inv1
        Pin pin_a = new Pin("A", Pin.Dir.INPUT, (float)1.0, "nand1_out");
        Pin pin_y = new Pin("Y", Pin.Dir.OUTPUT, (float)1.0, "inv1_out");
        ArrayList pins = new ArrayList();
        pins.add(pin_a);
        pins.add(pin_y);
        lesizer.addInstance("inv1", Instance.Type.LEGATE, su, (float)1.0, pins);
        }

        {
        // inv2
        Pin pin_a = new Pin("A", Pin.Dir.INPUT, (float)1.0, "pu_out");
        Pin pin_y = new Pin("Y", Pin.Dir.OUTPUT, (float)1.0, "inv2_out");
        ArrayList pins = new ArrayList();
        pins.add(pin_a);
        pins.add(pin_y);
        lesizer.addInstance("inv2", Instance.Type.LEGATE, su, (float)1.0, pins);
        }

        {
        // inv3
        Pin pin_a = new Pin("A", Pin.Dir.INPUT, (float)1.0, "nand1_out");
        Pin pin_y = new Pin("Y", Pin.Dir.OUTPUT, (float)1.0, "inv3_out");
        ArrayList pins = new ArrayList();
        pins.add(pin_a);
        pins.add(pin_y);
        lesizer.addInstance("inv3", Instance.Type.LEGATE, su, (float)1.0, pins);
        }
        
        {
        // nand1
        Pin pin_a = new Pin("A", Pin.Dir.INPUT, (float)1.333, "inv2_out");
        Pin pin_b = new Pin("B", Pin.Dir.INPUT, (float)1.333, "pd_out");
        Pin pin_y = new Pin("Y", Pin.Dir.OUTPUT, (float)2.0, "nand1_out");
        ArrayList pins = new ArrayList();
        pins.add(pin_a);
        pins.add(pin_b);
        pins.add(pin_y);
        lesizer.addInstance("nand1", Instance.Type.LEGATE, su, (float)1.0, pins);
        }

        {
        // pu
        Pin pin_g = new Pin("G", Pin.Dir.INPUT, (float)0.667, "nand1_out");
        Pin pin_d = new Pin("D", Pin.Dir.OUTPUT, (float)0.667, "pu_out");
        ArrayList pins = new ArrayList();
        pins.add(pin_g);
        pins.add(pin_d);
        lesizer.addInstance("pu", Instance.Type.LEGATE, su, (float)1.0, pins);
        }

        {
        // pd
        Pin pin_g = new Pin("G", Pin.Dir.INPUT, (float)0.333, "inv3_out");
        Pin pin_d = new Pin("D", Pin.Dir.OUTPUT, (float)0.333, "pd_out");
        ArrayList pins = new ArrayList();
        pins.add(pin_g);
        pins.add(pin_d);
        lesizer.addInstance("pd", Instance.Type.LEGATE, su, (float)1.0, pins);
        }

        {
        // cap1
        Pin pin_c = new Pin("C", Pin.Dir.INPUT, (float)1.0, "pd_out");
        ArrayList pins = new ArrayList();
        pins.add(pin_c);
        lesizer.addInstance("cap1", Instance.Type.NOTSIZEABLE, su, (float)0.0, pins);
        }

        {
        // cap2
        Pin pin_c = new Pin("C", Pin.Dir.INPUT, (float)1.0, "pu_out");
        ArrayList pins = new ArrayList();
        pins.add(pin_c);
        lesizer.addInstance("cap2", Instance.Type.NOTSIZEABLE, su, (float)0.0, pins);
        }

        {
        // cap3
        Pin pin_c = new Pin("C", Pin.Dir.INPUT, (float)1.0, "inv1_out");
        ArrayList pins = new ArrayList();
        pins.add(pin_c);
        lesizer.addInstance("cap3", Instance.Type.NOTSIZEABLE, su, (float)100.0, pins);
        }

        lesizer.printDesign();
        lesizer.optimizeLoops((float)0.01, 30, true, (float)0.7, (float)0.1);
        System.out.println("After optimization:");
        lesizer.printDesign();
    }
}
