/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Part.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
*/
package com.sun.electric.tool.ncc.netlist;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.sun.electric.technology.PrimitiveNode.Function;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.netlist.NccNameProxy.PartNameProxy;
import com.sun.electric.tool.ncc.result.PartReport.PartReportable;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.Job;

/** Part is an intermediate abstract sub-class of NetObject.
 * sub-classes of Part include Transistor, Resistor, (Capacitor), but
 * NOT Port. */
public abstract class Part extends NetObject implements PartReportable {
	// ------------------- constants ------------------
	private static final Wire[] DELETED = null;
	private static final int NUM_FUNCS = Function.values().length;
	protected static final int TYPE_FIELD_WIDTH = 
		                    (int) Math.ceil( Math.log(NUM_FUNCS)/Math.log(2) );  

    // ---------- private data -------------
	private PartNameProxy nameProxy;
	private final Function type;
    protected Wire[] pins;
    
    // ---------- private methods ----------
	/** @param name the name of Part
	 * @param type the type of Parts
	 * @param pins Wires attached to pins of Part */
    protected Part(PartNameProxy name, Function type, Wire[] pins){
    	nameProxy = name;
    	this.type = type;
		this.pins = pins;
		for (int i=0; i<pins.length; i++)  pins[i].add(this);
    }

    // ---------- public methods ----------
    /** Return the type of part. The type distinguishes between MOS 
     * transistors, bipolar transistors, resistors, and subcircuits. 
     * 
     * The type also distinguishes variations. For MOS
     * transistors it distinguishes between different threshold 
     * devices, different voltage devices, whether the MOS is a depletion
     * transistor, floating gate transistor, etc.
     * 
     * For Resistors the type distinguishes between p-poly vs n-poly vs
     * p-well vs n-well resistors.
     * 
     * The type does NOT distinguish between the MOS transistors that
     * are stacked in series. The type does NOT distinguish between
     * MOS transistors that are wired as capacitors. These distinctions
     * are created dynamically.
     * 
     * The type does NOT distinguish between 3 vs 4 pin MOS transistors.
     * This distinction isn't important for NCC.
     * 
     * As Electric evolves I expect it will accumulate additional
     * variations. I'm trying to write NCC so that it automatically
     * accommodates those variations without substantial, if any 
     * change */
    public Function type() {return type;} 

	@Override public String getName() {return nameProxy.getName();}
	@Override public Iterator getConnected() {return Arrays.asList(pins).iterator();}
	public PartNameProxy getNameProxy() {return nameProxy;}
    @Override public Type getNetObjType() {return Type.PART;}

	/** Here is the accessor for the number of terminals on this Part
	 * @return the number of terminals on this Part, usually a small number. */
	public int numPins() {return pins.length;}

	/**  Here is an accessor method for the coefficient array for this
	 * Part.  The terminal coefficients are used to compute new hash
	 * codes.
	 * @return the array of terminal coefficients for this Part*/
	public abstract int[] getPinCoeffs(); 

	/** This method attempts to merge this Part in parallel with another Part
	 * @param p the other Part with which to merge
	 * @param nccOpt NccOptions. Used for size tolerance specification.
	 * @return true if merge was successful, false otherwise */
	public abstract boolean parallelMerge(Part p, NccOptions nccOpt);
	/** Compute a hash code for this part for the purpose of performing
	 * parallel merge. If two parallel Parts should be merged into one then
	 * hashCodeForParallelMerge() must return the same value for both
	 * Parts. */
	public abstract Integer hashCodeForParallelMerge();
	
    /** Mark this Part deleted and release all storage */
    public void setDeleted() {pins=DELETED;}
    @Override public boolean isDeleted() {return pins==DELETED;}
    
	/**  Get the number of distinct Wires this part is connected to.
	 * For example, if all pins are connected to the same Wire then return 1.
	 * This method is only used for sanity checking by StratCount.
	 * @return the number of distinct Wires to which this Part is connected */
    public int numDistinctWires() {
    	Set<Wire> wires = new HashSet<Wire>();
    	for (int i=0; i<pins.length; i++)  wires.add(pins[i]);
    	return wires.size();
    }
    
	/** returns String describing Part's type */
    public abstract String typeString();
    
	/** returns a unique int value for each distinct Part type */
    public abstract int typeCode();
    	
	/** How many pins of this Part are connected to Wire.
	 * @param w the Wire to test
	 * @return number of pins connected to Wire */
	public int numPinsConnected(Wire w) {
		int numConnected = 0;
		for (int i=0; i<pins.length; i++) {
		    if (pins[i]==w) numConnected++;
		}
        return numConnected;
    }

	public Integer computeHashCode(){
        int sum= 0;
        int codes[]= getPinCoeffs();
		for(int i=0; i<pins.length; i++) {
			Wire w = pins[i];
            sum += w.getCode() * codes[i];
        }
        return new Integer(sum);
    }

	/**  The Part must compute a hash code contribution for a Wire to
	 * use.  because the Wire doesn't know how it's connected to this
	 * Part and multiple connections are allowed.
	 * @param w the Wire for which a hash code is needed
	 * @return an int with the code contribution. */
    public int getHashFor(Wire w){
        int sum= 0;
        int codes[]= getPinCoeffs();
		for(int i=0; i<pins.length; i++){
			Wire x= pins[i];
			error(x==null, "null wire?");
			if(x==w) sum += codes[i] * getCode();
        }
        return sum;
    }
	
	/** check that this Part is in proper form
	 * complain if it's wrong */
	@Override public void checkMe(Circuit parent){
		error(parent!=getParent(), "wrong parent");
		for(int i=0; i<pins.length; i++){
		    Wire w= pins[i];
		    error(w==null, "Wire is null");
			error(!w.touches(this), "Wire not connected to Part");
		}
    }
	
	/** @return the PinType for the nth pin */
	public abstract PinType getPinTypeOfNthPin(int n);
	
    /** @return a String containing the part type, the Cell containing the part, 
     * and the instance name */
    @Override public String instanceDescription() {
    	// Don't print "Cell instance:" in root Cell where there is no path.
    	String inst = nameProxy.cellInstPath();
    	String instMsg = inst.equals("") ? "" : (" Cell instance: "+inst); 
    	return typeString()+" "+nameProxy.leafName()+" in Cell: "+
		       nameProxy.leafCell().libDescribe()+instMsg;
    }
    
	/** Report the numeric values of this Part,
	 * for example: width, length, resistance.
	 * @return a String describing the Part's numeric values.*/
	@Override public abstract String valueDescription();

	/** comma separated list of pins connected to w */
	public abstract String connectionDescription(Wire w);
	
	public boolean isMos() {return this instanceof Mos;}
	public boolean isResistor() {return this instanceof Resistor;}
	public double getWidth() {
        Job.error(true, "Part has no width"); return 0;}
	public double getLength() {
        Job.error(true, "Part has no length"); return 0;}
}

