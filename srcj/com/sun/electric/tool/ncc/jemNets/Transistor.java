/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Transistor.java
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
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.jemNets.Wire;
import com.sun.electric.tool.ncc.trees.JemCircuit;
import com.sun.electric.tool.generator.layout.LayoutLib;

import java.util.ArrayList;
import java.util.List;
import java.util.Iterator;

/** One or more MOS transistors in series. All gates have the same width
 * and length. */
public class Transistor extends Part {
	public static class Type {
		private String name;
		private Type(String s){name=s;}
	}
	/** Generate arrays of pin coefficients on demand. Share these arrays
	 * between identically sized Transistors */
	private static class CoeffGen {
		private static ArrayList coeffArrays = new ArrayList();
		private static void ensureListEntry(int numPins) {
			while (coeffArrays.size()-1<numPins)  coeffArrays.add(null);
		}
		public static int[] getCoeffArray(int numPins) {
			ensureListEntry(numPins);
			int[] coeffArray = (int[]) coeffArrays.get(numPins);
			if (coeffArray==null) {
				coeffArray = new int[numPins];
				for (int i=0; i<(numPins+1)/2; i++) {
					int j = numPins-1-i;
					int nthPrime = 30 + i + numPins;
					coeffArray[i] = coeffArray[j] = Primes.get(nthPrime);
				}
				coeffArrays.set(numPins, coeffArray);
				// debug
				//System.out.print("New coeff array:");
				//for (int i=0; i<numPins; i++) 
				//	System.out.print(" "+coeffArray[i]);
				//System.out.println();
			}
			return coeffArray;
		}
	}

    // ---------- private data -------------
    private final int[] term_coeffs;
    private double width;
    private final double length;
    private final Type type;
    
    // ---------- private methods ----------
	/** Stack of series transistors */
	private Transistor(Type np, String name, double width, double length,
					   Wire[] pins) {
		super(name, pins);
		type = np;
		this.width = width;
		this.length = length;
		
		term_coeffs = CoeffGen.getCoeffArray(pins.length);
	}

	private boolean matchForward(Transistor t) {
		for (int i=0; i<pins.length; i++) {
			if (pins[i]!=t.pins[i]) return false;
		}
		return true;
	}
	private boolean matchReverse(Transistor t) {
		for (int i=0; i<pins.length; i++) {
			int j = pins.length-1-i;
			if (pins[i]!=t.pins[j]) return false;
		}
		return true;
	}
	private boolean samePinsAs(Transistor t) {
		if (pins.length!=t.pins.length) return false;
		return matchForward(t) || matchReverse(t);
	}
	private void flip() {
		for (int i=0; i<pins.length/2; i++) {
			int j = pins.length-1-i;
			Wire w = pins[i];
			pins[i] = pins[j];
			pins[j] = w;
		}
	}
	private Wire hiDiff() {return pins[pins.length-1];}
	private Wire loDiff() {return pins[0];}

    // ---------- public methods ----------
	public static final Type NTYPE= new Type("N-");
	public static final Type PTYPE= new Type("P-");

	/** Standard 3 terminal Transistor. */
	public Transistor(Type np, String name, double width, double length,
					  Wire src, Wire gate, Wire drn) {
		this(np, name, width, length, new Wire[] {src, gate, drn});
	}

    public Type getType() {return type;}
    public double getLength() {return length;}
    public double getWidth() {return width;}
    public boolean isNtype(){return (type== NTYPE);}
    public boolean isPtype(){return (type== PTYPE);}
	public boolean isThisGate(int x){return x!=0 && x!=pins.length-1;}
	public int[] getTermCoefs() {return term_coeffs;}

	public boolean touchesAtGate(Wire w){
		for (int i=1; i<pins.length-1; i++)  if (w==pins[i]) return true;
		return false;
	}

	public boolean touchesAtDiffusion(Wire w){
		return w==pins[0] || w==pins[pins.length-1];
	}

	public boolean isCapacitor() {return pins[0]==pins[pins.length-1];}

	public Integer hashCodeForParallelMerge() {
		// include how many Wires may be connected
		int hc = pins.length;
		// include what's connected
		for (int i=0; i<pins.length; i++)  
			hc += pins[i].hashCode() * term_coeffs[i];
		// include the class
		hc += getClass().hashCode();
		// include whether its NMOS or PMOS
		hc += type.hashCode();
		return new Integer(hc);
	}

	// merge into this transistor
	public boolean parallelMerge(Part p){
		if(!(p instanceof Transistor)) return false;
		Transistor t= (Transistor) p;
		if(this == t)return false; //same transistor

		if(!this.isLike(t))return false; //different type

		// a different individual same type and length
		if (!samePinsAs(t)) return false;
		
		width += t.width;
		t.deleteMe();
		return true;		    	
	}

	// ---------- printing methods ----------

	public String nameString(){
		return (type.name + "Trans" + (pins.length-2) +" "+ getName());
	}

	public String connectionString(int n) {
		String msg="";
		for (int i=0; i<pins.length; i++) {
			if (i==0) {
				msg += "S=";
			} else if (i==pins.length-1) {
				msg += " D="; 
			} else {
				if (pins.length==3) {
					msg += " G="; 
				} else {
					msg += " G"+i+"=";
				}
			}
			msg += pins[i].getName();
		}
		return msg;
	}

	/**
	 * Compare the type (N vs P) and the gate length
	 * @param t Transistor to compare to
	 * @return true if type and gate length match
	 */
	public boolean isLike(Transistor t){
		return type==t.type && length==t.length;
    }
	
	public String valueString(){
		return "W= " + width + " L= " + length;
	}

	/** 
	 * Merge two series Transistors into a single Transistor.  
	 * @param w wire joining diffusions of two transistors
	 * @return true if merge has taken place
	 */
	public static boolean joinOnWire(Wire w){
		Transistor ta= null;
		Transistor tb= null;
		error(w.numParts()!=2, "wire may only connect two Transistor diffusions");
		for (Iterator it=w.getParts(); it.hasNext();) {
			Object oo= it.next();
			if(oo instanceof Transistor) {
				Transistor t= (Transistor)oo;
				if (ta==null)  ta= t;
				else  tb= t;
			}
		}
		if (ta==null || tb==null)  return false;
		error(ta==tb, "part should only occur once on wire");
		error(ta.getParent()!=tb.getParent(), "mismatched parents?");
		if (!ta.isLike(tb))  return false;
		if (ta.width!=tb.width)  return false;
		
		// it's a match - merge tb into ta
		if (ta.hiDiff()!=w) ta.flip();
		if (tb.loDiff()!=w) tb.flip();
		
		error(ta.hiDiff()!=w || tb.loDiff()!=w, "joinOnWire: diffusion connections corrupted");
		Wire[] mergedPins = new Wire[ta.pins.length + tb.pins.length - 2];
		int aNdx = 0;
		for (; aNdx<ta.pins.length-1; aNdx++) {
			mergedPins[aNdx] = ta.pins[aNdx];
		}
		for (int bNdx=1; bNdx<tb.pins.length; bNdx++){
			mergedPins[aNdx++] = tb.pins[bNdx];
		}
		Transistor stack = new Transistor(ta.getType(), ta.getName(),  
										  ta.getWidth(), ta.getLength(), 
										  mergedPins);

		JemCircuit parent = (JemCircuit) tb.getParent();
		parent.adopt(stack);						  
		ta.deleteMe();
		tb.deleteMe();
		error(w.numParts()!=0, "wire not empty?");
		return true;
	}
	// over ride the hash computation
	public Integer computeHashCode(){
		// the function is symmetric: ABCD = DCBA
		int sumLo=0, sumHi=0;
		for (int i=0; i<(pins.length+1)/2; i++){
			sumLo += pins[i].getCode() * term_coeffs[i];
			int j = pins.length-1-i;
			sumHi += pins[j].getCode() * term_coeffs[j];
		}
		return new Integer(sumLo * sumHi);
	}

}
