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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.basic.Primes;
import com.sun.electric.tool.ncc.trees.Circuit;

/** One or more MOS transistors in series. All gates have the same width
 * and length. */
public class Transistor extends Part {
	public static class Type {
		private String name;
		private Type(String s) {name=s;}
	}
	public static final Type NTYPE= new Type("NMOS");
	public static final Type PTYPE= new Type("PMOS");

	private static class GateType implements PinType {
		private final int numSeries;
		private final Type np;
		private final int gateHeight;
		private final boolean cap;

		public int numConnectionsToPinOfThisType(Part p, Wire w) {
			if (!(p instanceof Transistor)) return 0;
			Transistor t = (Transistor) p;
			if (t.getType()!=np) return 0;
			if (t.numSeries()!=numSeries) return 0;
			if (cap!=t.isCapacitor()) return 0;
			
			int loGate = gateHeight;
			int hiGate = numSeries+1 - gateHeight;

			int numPins = numSeries+2;
			int count = 0;
			if (t.pins[loGate]==w) count++;
			if (loGate!=hiGate && t.pins[hiGate]==w) count++;
			return count;
		}
		public String description() {
			String t = np==NTYPE ? "NMOS" : "PMOS";
			String c = cap ? "_CAP" : "";
			String h = numSeries==1 ? "" : ("_"+numSeries+"stack");
			int hiGate = numSeries+1 - gateHeight;
			String g = "";
			if (numSeries>2) {
				g = gateHeight + (gateHeight==hiGate ? "" : ("/"+hiGate)); 
			} 
			return t+c+h+" gate"+g;
		}
		public GateType(Type np, int numSeries, int gateHeight, boolean cap) {
			LayoutLib.error(np!=NTYPE && np!=PTYPE, "bad type");
			LayoutLib.error(numSeries<1, "bad numSeries");
			int highestGateInLowerHalfOfStack = (numSeries+1)/2;
			LayoutLib.error(gateHeight>highestGateInLowerHalfOfStack, "bad gate Height");
			this.np = np;
			this.numSeries = numSeries;
			this.gateHeight = gateHeight;
			this.cap = cap;
		}
	}
	private static class DiffType implements PinType {
		private final int numSeries;
		private final Type np;
		private final boolean cap;

		public int numConnectionsToPinOfThisType(Part p, Wire w) {
			if (!(p instanceof Transistor)) return 0;
			Transistor t = (Transistor) p;
			if (t.getType()!=np) return 0;
			if (t.numSeries()!=numSeries) return 0;
			if (cap!=t.isCapacitor()) return 0;
			
			int count = 0;
			if (t.pins[0]==w) count++;
			if (t.pins[numSeries+1]==w) count++;
			return count;
		}
		public String description() {
			String t = np==NTYPE ? "NMOS" : "PMOS";
			String c = cap ? "_CAP" : "";
			String h = numSeries==1 ? "" : ("_"+numSeries+"stack");
			return t+c+h+" diffusion";
		}
		public DiffType(Type np, int numSeries, boolean cap) {
			LayoutLib.error(np!=NTYPE && np!=PTYPE, "bad type");
			LayoutLib.error(numSeries<1, "bad numSeries");
			int highestGateInLowerHalfOfStack = (numSeries+1)/2;
			this.np = np;
			this.numSeries = numSeries;
			this.cap = cap;
		}
	}

//	public static final Collection PIN_TYPES = new ArrayList();
//	static {
//		Type[] types = {NTYPE, PTYPE};
//		for (int i=0; i<2; i++) {
//			Type type = types[i]; 
//			for (int j=0; j<2; j++) {
//				boolean cap = j==0 ? false : true;
//				for (int numSeries=1; numSeries<=5; numSeries++) {
//					PIN_TYPES.add(new DiffType(type, numSeries, cap));
//
//					int maxHeight = (numSeries+1) / 2;
//					for (int gateHeight=1; gateHeight<=maxHeight; gateHeight++) {
//						PIN_TYPES.add(new GateType(type, numSeries, gateHeight, cap));
//					}
//				}
//			}
//		}
//	}

	private static final List PIN_TYPES = new ArrayList();
	static {
		Type[] types = {NTYPE, PTYPE};
		for (int i=0; i<2; i++) {
			List tList = new ArrayList();
			PIN_TYPES.add(tList);
			Type type = types[i]; 
			for (int j=0; j<2; j++) {
				List cList = new ArrayList();
				tList.add(cList);
				boolean cap = j==0 ? false : true;
				for (int numSeries=1; numSeries<=5; numSeries++) {
					Set pinTypes = new HashSet();
					cList.add(pinTypes);
					
					pinTypes.add(new DiffType(type, numSeries, cap));

					int maxHeight = (numSeries+1) / 2;
					for (int gateHeight=1; gateHeight<=maxHeight; gateHeight++) {
						pinTypes.add(new GateType(type, numSeries, gateHeight, cap));
					}
				}
			}
		}
	}
	public Set getPinTypes() {
		int t = type==NTYPE ? 0 : 1;
		ArrayList l1 = (ArrayList) PIN_TYPES.get(t);
		int c = isCapacitor() ? 1 : 0;
		ArrayList l2 = (ArrayList) l1.get(c);
		int s = numSeries()-1;
		Set pinTypes = (Set) l2.get(s); 
		return pinTypes;
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
	private Transistor(Type np, NccNameProxy name, double width, double length,
					   Wire[] pins) {
		super(name, pins);
		type = np;
		this.width = width;
		this.length = length;
		LayoutLib.error(type!=NTYPE && type!=PTYPE, "bad type");
		
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

	/** The standard 3 terminal Transistor. */
	public Transistor(Type np, NccNameProxy name, double width, double length,
					  Wire src, Wire gate, Wire drn) {
		this(np, name, width, length, new Wire[] {src, gate, drn});
	}

    public Type getType() {return type;}
    public double getLength() {return length;}
    public double getWidth() {return width;}
    public boolean isNtype(){return (type== NTYPE);}
    public boolean isPtype(){return (type== PTYPE);}
	public boolean isThisGate(int x){return x!=0 && x!=pins.length-1;}
	public int numSeries() {return pins.length-2;}
	public int[] getTermCoefs() {return term_coeffs;}

	public boolean touchesAtGate(Wire w){
		for (int i=1; i<pins.length-1; i++)  if (w==pins[i]) return true;
		return false;
	}

//	public boolean touchesAtDiffusion(Wire w){
//		return w==pins[0] || w==pins[pins.length-1];
//	}
	public boolean touchesOneDiffPinAndNoOtherPins(Wire w) {
		return (w==pins[0] ^ w==pins[pins.length-1]) &&
			   !touchesAtGate(w);
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
		t.setDeleted();
		return true;		    	
	}

	public int typeCode() {
		final int tw = Part.TYPE_FIELD_WIDTH;
		return Part.TRANSISTOR +
			   ((isCapacitor()?1:0) << tw) +
			   ((isNtype()?1:0) << tw+1) +
			   (numSeries() << tw+2);
	}

	// ---------- printing methods ----------

	public String typeString() {
		String t = type.name;
		String c = isCapacitor() ? "_CAP" : "";
		String h = pins.length==3 ? "" : ("_"+(pins.length-2)+"stack");
		return t+c+h;
	}

	public String nameString() {
		return typeString()+" "+getName();
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
	
	public String connectionString(Wire w) {
		String s = "";
		for (int i=0; i<pins.length; i++) {
			if (pins[i]!=w)  continue;
			if (s.length()!=0) s+=",";
			if (i==0) {
				s += "S";
			} else if (i==pins.length-1) {
				s += "D"; 
			} else {
				if (pins.length==3) {
					s += "G"; 
				} else {
					s += "G"+i;
				}
			}
		}
		return s;
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

	/** Merge two series Transistors into a single Transistor. 
	 * Tricky: Parts on wire may be deleted or replicated. 
	 * @param w wire joining diffusions of two transistors
	 * @return true if merge has taken place */
	public static boolean joinOnWire(Wire w) {
		if (w.isDeleted()) return false;

		// make sure there are no Ports on wire
		if (w.getPort()!=null)  return false;
		
		// wires declared GLOBAL in Electric can't be internal nodes 
		// of MOS stacks
		//if (w.isGlobal()) return false;
		
		// Use Set to remove duplicate Parts
		Set trans = new HashSet();
		for (Iterator it=w.getParts(); it.hasNext();) {
			Part p = (Part) it.next();
			if (p.isDeleted()) continue;
			if (!(p instanceof Transistor)) return false;
			Transistor t = (Transistor) p;
			if (!t.touchesOneDiffPinAndNoOtherPins(w)) return false;
			trans.add(t);
			if (trans.size()>2) return false;
		}
		if (trans.size()!=2) return false;

		Iterator it = trans.iterator();
		Transistor ta = (Transistor) it.next();
		Transistor tb = (Transistor) it.next();
		error(ta.getParent()!=tb.getParent(), "mismatched parents?");
		if (!ta.isLike(tb))  return false;
		if (ta.width!=tb.width)  return false;
		
		// it's a match - merge them into a stack
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
		Transistor stack = new Transistor(ta.getType(), ta.getNameProxy(),  
										  ta.getWidth(), ta.getLength(), 
										  mergedPins);

		Circuit parent = (Circuit) tb.getParent();
		parent.adopt(stack);
		ta.setDeleted();
		tb.setDeleted();
		//error(w.numParts()!=0, "wire not empty?");
		w.setDeleted();
		return true;
	}
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
