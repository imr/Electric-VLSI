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
package com.sun.electric.tool.ncc.jemNets;
import java.util.HashSet;
import java.util.Set;

import com.sun.electric.tool.ncc.basic.Primes;

public class Resistor extends Part {
	private static class ResistorPinType implements PinType {
		public int numConnectionsToPinOfThisType(Part p, Wire w) {
			int numConns = 0;
			for (int i=0; i<p.pins.length; i++)   if (p.pins[i]==w) numConns++;
			return numConns;
		}
		public String description() {return "Resistor pin";}
	}
	private static final Set PIN_TYPES = new HashSet();
	static {
		PIN_TYPES.add(new ResistorPinType());
	}

    // ---------- private data -------------
    private static final int TERM_COEFFS[] = 
    	{Primes.get(1), Primes.get(1)}; //resistors are symmetric
    private float resistance;

    // ---------- private methods ----------

    private void flip(){
        Wire w = pins[0];
        pins[0] = pins[1];
        pins[1] = w;
    }

    // ---------- public methods ----------
	public Resistor(NccNameProxy name, double resist, Wire w1, Wire w2) {
		super(name, new Wire[]{w1, w2});
		resistance = (float) resist;
	}

    // ---------- abstract commitment ----------

	public boolean isThisGate(int x){return false;}
    public int[] getTermCoefs(){return TERM_COEFFS;}
	public String valueString(){
		String sz= "R= " + resistance;
		return sz;
	}
	public Integer hashCodeForParallelMerge() {
		int hc = pins[0].hashCode() + pins[1].hashCode() +
				 getClass().hashCode();
		return new Integer(hc);
	}
	
    // ---------- public methods ----------

    public float resistance(){return resistance;}

	/** A method to test if this Part touches a Wire with a gate connection.
	 * @param w the Wire to test
	 * @return false because Resistors don't have gates.
	 */
    public boolean touchesAtGate(Wire w){return false;}
	
    public void connect(Wire ss, Wire ee){
        pins[0] = ss;
        pins[1] = ee;
		ss.add(this);
		ee.add(this);
    }
	
    //merge with this resistor
    public boolean parallelMerge(Part p){
        if(p.getClass() != getClass()) return false;
        if(this == p)return false;
        //its the same class but a different one
        Resistor r= (Resistor)p;
        if(pins[0]!=r.pins[0])  r.flip();
		if(pins[0]!=r.pins[0] || pins[1]!=r.pins[1])  return false;

        //OK to merge
        float ff= 0;
        float pp= r.resistance();
        float mm= resistance();
        if(pp != 0 && mm != 0)ff= (ff * mm)/(ff + mm);
        resistance= ff;
        r.setDeleted();
        return true; //return true if merged
    }

	public int typeCode() {return Part.RESISTOR;}
	
	public Set getPinTypes() {return PIN_TYPES;}

    // ---------- printing methods ----------

    public String typeString() {return "Resistor";}

    public String connectionString(int n){
		String s = pins[0].getName();
		String e = pins[1].getName();
		return ("S= " + s + " E= " + e);
    }
    
    public String connectionString(Wire w) {
    	String s = "";
    	for (int i=0; i<pins.length; i++) {
    		if (pins[i]!=w) continue;
    		if (s.length()!=0) s+=",";
    		s += i==0 ? "S" : "E";
    	}
    	return s;
    }
}

