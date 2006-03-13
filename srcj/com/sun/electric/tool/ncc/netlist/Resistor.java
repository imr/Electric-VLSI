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
*/
package com.sun.electric.tool.ncc.netlist;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.sun.electric.tool.ncc.basic.Primes;
import com.sun.electric.tool.ncc.netlist.NccNameProxy.PartNameProxy;

public class Resistor extends Part {
	public static final PartTypeTable TYPES = 
		new PartTypeTable(new String[][] {
			{"N-Poly-RPO-Resistor", "N-Poly-RPO-Resistor"},
			{"P-Poly-RPO-Resistor", "P-Poly-RPO-Resistor"}
		});
		
	private static class ResistorPinType implements PinType {
		private PartType type;
		public ResistorPinType(PartType t) {type=t;}
		public String description() {return type.getName();}
	}
	private static final Map<PartType,ResistorPinType> TYPE_TO_PINTYPE = new HashMap<PartType,ResistorPinType>();
	static {
		for (Iterator<PartType> it=TYPES.iterator(); it.hasNext();) {
			PartType t = it.next();
			TYPE_TO_PINTYPE.put(t, new ResistorPinType(t));
		}
	}

    // ---------- private data -------------
    private static final int PIN_COEFFS[] = 
    	{Primes.get(1), Primes.get(1)}; //resistors are symmetric
    private final PartType type;
    private final double width, length;

    // ---------- public methods ----------
	public Resistor(PartType type, PartNameProxy name, 
			        double width, double length, Wire w1, Wire w2) {
		super(name, new Wire[]{w1, w2});
		this.type = type;
		this.width = width;
		this.length = length;
	}

    // ---------- abstract commitment ----------

    public int[] getPinCoeffs(){return PIN_COEFFS;}
	public String valueDescription(){
		String sz= "W= "+width+" L="+length;
		return sz;
	}
	public Integer hashCodeForParallelMerge() {
		int hc = pins[0].hashCode() + pins[1].hashCode() +
				 getClass().hashCode();
		return new Integer(hc);
	}
	
    // ---------- public methods ----------

    public double getWidth() {return width;}
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
    public boolean parallelMerge(Part p) {return false;}

	public int typeCode() {
		final int tw = Part.TYPE_FIELD_WIDTH;
		return Part.RESISTOR + (type.getOrdinal() << tw);
	}
	
	// Both pins of resistor have same PinType
	public PinType getPinTypeOfNthPin(int n) {
		return TYPE_TO_PINTYPE.get(type);
	}

    // ---------- printing methods ----------

    public String typeString() {return type.getName();}

    public String connectionDescription(int n){
		String s = pins[0].getName();
		String e = pins[1].getName();
		return ("S= " + s + " E= " + e);
    }
    
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

