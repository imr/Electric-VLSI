/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Resistor.java
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
import java.util.HashMap;
import java.util.Map;

import com.sun.electric.technology.PrimitiveNode.Function;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.basic.Primes;
import com.sun.electric.tool.ncc.netlist.NccNameProxy.PartNameProxy;

public class Resistor extends Part {
	private static class ResistorPinType implements PinType {
		private Function type;
		public ResistorPinType(Function t) {type=t;}
		public String description() {return type.getShortName();}
	}
	
	private static class ResistorPinTypeCache {
		private final Map<Function,ResistorPinType> typeToPinType = 
	                                   new HashMap<Function,ResistorPinType>();
		synchronized ResistorPinType get(Function f) {
			ResistorPinType t = typeToPinType.get(f);
			if (t==null) {
				t = new ResistorPinType(f);
				typeToPinType.put(f, t);
			}
			return t;
		}
	}

    // ---------- private data -------------
	private static final ResistorPinTypeCache TYPE_TO_PINTYPE = 
                                                    new ResistorPinTypeCache();
    private static final int PIN_COEFFS[] = 
    	{Primes.get(1), Primes.get(1)}; //resistors are symmetric
    private final double width, length;

    // ---------- public methods ----------
	public Resistor(Function type, PartNameProxy name, 
			        double width, double length, Wire w1, Wire w2) {
		super(name, type, new Wire[]{w1, w2});
		this.width = width;
		this.length = length;
	}

    // ---------- abstract commitment ----------
	@Override
    public int[] getPinCoeffs(){return PIN_COEFFS;}
	@Override
	public String valueDescription(){
		String sz= "W= "+width+" L="+length;
		return sz;
	}
	@Override
	public Integer hashCodeForParallelMerge() {
		int hc = pins[0].hashCode() + pins[1].hashCode() +
				 getClass().hashCode();
		return new Integer(hc);
	}

    // ---------- public methods ----------
	@Override
    public double getWidth() {return width;}
	@Override
    public double getLength() {return length;}

    public void connect(Wire ss, Wire ee){
        pins[0] = ss;
        pins[1] = ee;
		ss.add(this);
		ee.add(this);
    }
	
    //merge with this resistor
//    public boolean parallelMerge(Part p){
//        if(p.getClass() != getClass()) return false;
//        if(this == p)return false;
//        //its the same class but a different one
//        Resistor r= (Resistor)p;
//        if(pins[0]!=r.pins[0])  r.flip();
//		if(pins[0]!=r.pins[0] || pins[1]!=r.pins[1])  return false;
//
//        //OK to merge
//        float ff= 0;
//        float pp= r.resistance();
//        float mm= resistance();
//        if(pp != 0 && mm != 0)ff= (ff * mm)/(ff + mm);
//        resistance= ff;
//        r.setDeleted();
//        return true; //return true if merged
//    }
    /** Never perform series/parallel combination of resistors. For layout
     * resistors, resistance is 
     * a vendor dependent function of resistor width and length. We'll simply
     * annotate resistor width and length and compare these between schematic
     * and layout. See Jon Lexau for rationale. */
	@Override
    public boolean parallelMerge(Part p, NccOptions nccOpt) {return false;}

	@Override
	public int typeCode() {return type().ordinal();}
	
	// Both pins of resistor have same PinType
	@Override
	public PinType getPinTypeOfNthPin(int n) {
		return TYPE_TO_PINTYPE.get(type());
	}

    // ---------- printing methods ----------

	@Override
    public String typeString() {return type().getShortName();}

	@Override
    public String connectionDescription(int n){
		String s = pins[0].getName();
		String e = pins[1].getName();
		return ("S= " + s + " E= " + e);
    }
    
	@Override
    public String connectionDescription(Wire w) {
    	String s = "";
    	for (int i=0; i<pins.length; i++) {
    		if (pins[i]!=w) continue;
    		if (s.length()!=0) s+=",";
    		s += i==0 ? "S" : "E";
    	}
    	return s;
    }
}

