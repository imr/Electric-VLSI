/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Bipolar.java
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
import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.basic.Primes;
import com.sun.electric.tool.ncc.netlist.NccNameProxy.PartNameProxy;
import com.sun.electric.tool.Job;

/** A Bipolar transistor */
public class Bipolar extends Part {
	private static class BipolarPinType implements PinType {
		private final Function np;
		private final int pinIndex;
		private static final String[] PIN_NAMES = {"emitter","base","collector"}; 

		public String description() {
			return np.getShortName()+" "+PIN_NAMES[pinIndex];
		}
		public BipolarPinType(Function np, int pinIndex) {
			Job.error(np==null, "null type?");
			this.np = np;
			this.pinIndex = pinIndex;
		}
	}
	
	private static class BipolarPinTypeCache {
		private Map<Function,BipolarPinType[]> typeToPinTypeArray = 
			                          new HashMap<Function,BipolarPinType[]>();
	    synchronized BipolarPinType[] get(Function f) {
	    	BipolarPinType[] bpt = typeToPinTypeArray.get(f);
	    	if (bpt==null) {
	    		bpt = new BipolarPinType[3];
	    		for (int p=0; p<3; p++) {
	    			bpt[p] = new BipolarPinType(f,p);
	    		}
	    		typeToPinTypeArray.put(f, bpt);
	    	}
	    	return bpt;
	    }
	}

	private static final BipolarPinTypeCache TYPE_TO_PINTYPE_ARRAY = 
													 new BipolarPinTypeCache();

	@Override
	public PinType getPinTypeOfNthPin(int n) {
		PinType[] pinTypeArray = TYPE_TO_PINTYPE_ARRAY.get(type());
		return pinTypeArray[n];
	}
	
    // ---------- private data -------------
    private double area;
    private static final int PIN_COEFFS[] = 
								{Primes.get(1), Primes.get(2), Primes.get(3)}; 
    
    // ---------- private methods ----------
	private Bipolar(Function np, PartNameProxy name, double area, Wire[] pins) {
		super(name, np, pins);
		Job.error(np==null, "null type?");
		this.area = area;
	}

	private boolean samePinsAs(Bipolar b) {
		Job.error(b.pins.length!=pins.length, "different # pins?");
		for (int i=0; i<pins.length; i++) {
			if (pins[i]!=b.pins[i]) return false;
		}
		return true;
	}

    // ---------- public methods ----------

	public Bipolar(Function np, PartNameProxy name, double area,
				   Wire emit, Wire base, Wire coll) {
		this(np, name, area, new Wire[] {emit, base, coll});
	}
    public double getArea() {return area;}
    @Override
	public int[] getPinCoeffs() {return PIN_COEFFS;}
    @Override
	public Integer hashCodeForParallelMerge() {
		// include the class
		int hc = getClass().hashCode();
		// include what's connected
		for (int i=0; i<pins.length; i++)  
			hc += pins[i].hashCode() * PIN_COEFFS[i];
		// include whether its NPN or PNP
		hc += type().hashCode();
		return new Integer(hc);
	}

	// merge into this transistor
    @Override
	public boolean parallelMerge(Part p, NccOptions nccOpt){
		if(!(p instanceof Bipolar)) return false;
		Bipolar b = (Bipolar) p;
		if (this==b) return false; //same transistor

		if (type()!=b.type()) return false; //different type

		if (!samePinsAs(b)) return false; // same connections
		
		area += b.area;
		b.setDeleted();
		return true;		    	
	}
    @Override
	public int typeCode() {return type().ordinal();}

	// ---------- printing methods ----------
    @Override
	public String typeString() {return type().getShortName();}
    @Override

	public String valueDescription(){
		return "A=" + NccUtils.round(area,2);
	}
    @Override
	public String connectionDescription(int n) {
		return "E="+pins[0].getName()+
		      " B="+pins[1].getName()+
			  " C="+pins[2].getName();
	}
    @Override
	public String connectionDescription(Wire w) {
		String s = "";
		for (int i=0; i<pins.length; i++) {
			if (pins[i]!=w)  continue;
			if (s.length()!=0) s+=",";
			if (i==0) {
				s += "E";
			} else if (i==1) {
				s += "B"; 
			} else {
				s += "C"; 
			}
		}
		return s;
	}
//    @Override
//	public Integer computeHashCode() {
//		int sum=0;
//		for (int i=0; i<pins.length; i++){
//			sum += pins[i].getCode() * PIN_COEFFS[i];
//		}
//		return new Integer(sum);
//	}

}
