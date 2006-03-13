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
import java.util.Map;

import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.basic.Primes;
import com.sun.electric.tool.ncc.netlist.NccNameProxy.PartNameProxy;

/** One or more MOS transistors in series. All gates have the same width
 * and length. */
public class Bipolar extends Part {
	public static class Type {
		public static final Type NPN = new Type(0, "NPN");
		public static final Type PNP = new Type(1, "PNP");

		private final String name;
		private final int ordinal;
		private final int[] pin_coeffs = new int[3];
		private Type(int ord, String s) {
			ordinal=ord; 
			name=s;
			for (int p=0; p<3; p++) {
				pin_coeffs[p] = Primes.get(ord*3 + p);
			}
		}
		
		public String getName() {return name;}
		public int getOrdinal() {return ordinal;}
		public int[] getPinCoeffs() {return pin_coeffs;}
	}
	
	private static class BipolarPinType implements PinType {
		private final Type np;
		private final int pinIndex;
		private static final String[] PIN_NAMES = {"emitter","base","collector"}; 

		public String description() {
			return np.getName()+" "+PIN_NAMES[pinIndex];
		}
		public BipolarPinType(Type np, int pinIndex) {
			LayoutLib.error(np==null, "null type?");
			this.np = np;
			this.pinIndex = pinIndex;
		}
	}

	private static final Map<Type,PinType[]> TYPE_TO_PINTYPE_ARRAY = new HashMap<Type,PinType[]>();
	static {
		for (int t=0; t<2; t++) {
			Type type = t==0 ? Type.NPN : Type.PNP;
			PinType[] pinTypeArray = new PinType[3];
			TYPE_TO_PINTYPE_ARRAY.put(type, pinTypeArray);
			for (int p=0; p<3; p++) {
				pinTypeArray[p] = new BipolarPinType(type, p);
			}
		}
	}

	public PinType getPinTypeOfNthPin(int n) {
		PinType[] pinTypeArray = TYPE_TO_PINTYPE_ARRAY.get(type);
		return pinTypeArray[n];
	}
	
    // ---------- private data -------------
    private double area;
    private final Type type;
    
    // ---------- private methods ----------
	/** Stack of series transistors */
	private Bipolar(Type np, PartNameProxy name, double area, Wire[] pins) {
		super(name, pins);
		LayoutLib.error(np==null, "null type?");
		type = np;
		this.area = area;
	}

	private boolean samePinsAs(Bipolar b) {
		LayoutLib.error(b.pins.length!=pins.length, "different # pins?");
		for (int i=0; i<pins.length; i++) {
			if (pins[i]!=b.pins[i]) return false;
		}
		return true;
	}

    // ---------- public methods ----------

	public Bipolar(Type np, PartNameProxy name, double area,
				   Wire emit, Wire base, Wire coll) {
		this(np, name, area, new Wire[] {emit, base, coll});
	}

    public Type getType() {return type;}
    public double getArea() {return area;}
	public int[] getPinCoeffs() {return type.getPinCoeffs();}

	public Integer hashCodeForParallelMerge() {
		// include the class
		int hc = getClass().hashCode();
		// include what's connected
		for (int i=0; i<pins.length; i++)  
			hc += pins[i].hashCode() * type.getPinCoeffs()[i];
		// include whether its NPN or PNP
		hc += type.hashCode();
		return new Integer(hc);
	}

	// merge into this transistor
	public boolean parallelMerge(Part p){
		if(!(p instanceof Bipolar)) return false;
		Bipolar b = (Bipolar) p;
		if (this==b) return false; //same transistor

		if (type!=b.type) return false; //different type

		if (!samePinsAs(b)) return false; // same connections
		
		area += b.area;
		b.setDeleted();
		return true;		    	
	}

	public int typeCode() {
		final int tw = Part.TYPE_FIELD_WIDTH;
		return Part.TRANSISTOR + (type.getOrdinal() << tw);
	}

	// ---------- printing methods ----------

	public String typeString() {return type.name;}

	public String valueDescription(){
		return "A=" + NccUtils.round(area,2);
	}
	public String connectionDescription(int n) {
		return "E="+pins[0].getName()+
		      " B="+pins[1].getName()+
			  " C="+pins[2].getName();
	}
	
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

	public Integer computeHashCode() {
		int sum=0;
		for (int i=0; i<pins.length; i++){
			sum += pins[i].getCode() * type.getPinCoeffs()[i];
		}
		return new Integer(sum);
	}

}
