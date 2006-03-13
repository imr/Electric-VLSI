/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Mos.java
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.basic.Primes;
import com.sun.electric.tool.ncc.netlist.NccNameProxy.PartNameProxy;
import com.sun.electric.tool.ncc.trees.Circuit;

/** One or more MOS transistors in series. All gates have the same width
 * and length. */
public class Mos extends Part {
	public static final PartTypeTable TYPES = 
		new PartTypeTable(new String[][] {
			{"NMOS",         "N-Transistor"},
			{"NMOS",         "N-Transistor-Scalable"},
			{"NMOS-VTH",     "VTH-N-Transistor"},
			{"NMOS-VTL",     "VTL-N-Transistor"},
			{"NMOS-OD18",    "OD18-N-Transistor"},
			{"NMOS-OD25",    "OD25-N-Transistor"},
			{"NMOS-OD33",    "OD33-N-Transistor"},
			{"NMOS-NT",      "NT-N-Transistor"},
			{"NMOS-NT-OD18", "NT-OD18-N-Transistor"},
			{"NMOS-NT-OD25", "NT-OD25-N-Transistor"},
			{"NMOS-NT-OD33", "NT-OD33-N-Transistor"},
			{"PMOS",         "P-Transistor"},
			{"PMOS",         "P-Transistor-Scalable"},
			{"PMOS-VTH",     "VTH-P-Transistor"},
			{"PMOS-VTL",     "VTL-P-Transistor"},
			{"PMOS-OD18",    "OD18-P-Transistor"},
			{"PMOS-OD25",    "OD25-P-Transistor"},
			{"PMOS-OD33",    "OD33-P-Transistor"},
		});

	private static class GateType implements PinType {
		private final int numSeries;
		private final PartType np;
		private final int gateHeight;
		private final boolean cap;

		public String description() {
			String t = np.getName();
			String c = cap ? "_CAP" : "";
			String h = numSeries==1 ? "" : ("_"+numSeries+"stack");
			int hiGate = numSeries+1 - gateHeight;
			String g = "";
			if (numSeries>2) {
				g = gateHeight + (gateHeight==hiGate ? "" : ("/"+hiGate)); 
			} 
			return t+c+h+" gate"+g;
		}
		public GateType(PartType np, int numSeries, int gateHeight, boolean cap) {
			LayoutLib.error(np==null, "null type?");
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
		private final PartType np;
		private final boolean cap;

		public String description() {
			String t = np.getName();
			String c = cap ? "_CAP" : "";
			String h = numSeries==1 ? "" : ("_"+numSeries+"stack");
			return t+c+h+" diffusion";
		}
		public DiffType(PartType np, int numSeries, boolean cap) {
			LayoutLib.error(np==null, "null type?");
			LayoutLib.error(numSeries<1, "bad numSeries");
			this.np = np;
			this.numSeries = numSeries;
			this.cap = cap;
		}
	}
	/** Set of all the pins for a particular Transistor */ 
	private static class PinTypeSetKey {
		private PartType type;
		private boolean isCapacitor;
		private int numSeries;
		public PinTypeSetKey(PartType type, boolean isCapacitor, int numSeries) {
			this.type = type;
			this.isCapacitor = isCapacitor;
			this.numSeries = numSeries;
		}
		public boolean equals(Object o) {
			if (!(o instanceof PinTypeSetKey)) return false;
			PinTypeSetKey p = (PinTypeSetKey) o;
			return type==p.type && isCapacitor==p.isCapacitor && numSeries==p.numSeries;
		}
		public int hashCode() {
			return type.hashCode() + (isCapacitor?1:0) + (numSeries<<1);
		}
	}
	private static final Map<PinTypeSetKey,PinType[]> TYPE_TO_PINTYPE_ARRAY = new HashMap<PinTypeSetKey,PinType[]>();
	
	public synchronized PinType[] getPinTypeArray() {
		PinTypeSetKey key = new PinTypeSetKey(type, isCapacitor(), numSeries());
		PinType[] pinTypeArray = TYPE_TO_PINTYPE_ARRAY.get(key);
		if (pinTypeArray==null) {
			pinTypeArray = new PinType[pins.length];
			TYPE_TO_PINTYPE_ARRAY.put(key, pinTypeArray);
			
			pinTypeArray[0] = pinTypeArray[pinTypeArray.length-1] =
				new DiffType(type, numSeries(), isCapacitor());

			int maxHeight = (numSeries()+1) / 2;
			for (int gateHeight=1; gateHeight<=maxHeight; gateHeight++) {
				pinTypeArray[gateHeight] = 
					pinTypeArray[pinTypeArray.length-1-gateHeight] = 
					new GateType(type, numSeries(), gateHeight, isCapacitor());
			}
		}
		return pinTypeArray;
	}
	public synchronized PinType getPinTypeOfNthPin(int n) {
		return getPinTypeArray()[n];
	}
	
	/** Generate arrays of pin coefficients on demand. Share these arrays
	 * between identically sized Transistors */
	private static class CoeffGen {
		private static ArrayList<int[]> coeffArrays = new ArrayList<int[]>();
		private static void ensureListEntry(int numPins) {
			while (coeffArrays.size()-1<numPins)  coeffArrays.add(null);
		}
		public static int[] getCoeffArray(int numPins) {
			ensureListEntry(numPins);
			int[] coeffArray = coeffArrays.get(numPins);
			if (coeffArray==null) {
				coeffArray = new int[numPins];
				for (int i=0; i<(numPins+1)/2; i++) {
					int j = numPins-1-i;
					int nthPrime = 30 + i + numPins;
					coeffArray[i] = coeffArray[j] = Primes.get(nthPrime);
				}
				coeffArrays.set(numPins, coeffArray);
			}
			return coeffArray;
		}
	}

    // ---------- private data -------------
    private final int[] pin_coeffs;
    private double width;
    private final double length;
    private final PartType type;
    
    // ---------- private methods ----------
	/** Stack of series transistors */
	private Mos(PartType np, PartNameProxy name, double width, double length,
				Wire[] pins) {
		super(name, pins);
		type = np;
		this.width = width;
		this.length = length;
		LayoutLib.error(type==null, "null type?");
		
		pin_coeffs = CoeffGen.getCoeffArray(pins.length);
	}

	private boolean matchForward(Mos t) {
		for (int i=0; i<pins.length; i++) {
			if (pins[i]!=t.pins[i]) return false;
		}
		return true;
	}
	private boolean matchReverse(Mos t) {
		for (int i=0; i<pins.length; i++) {
			int j = pins.length-1-i;
			if (pins[i]!=t.pins[j]) return false;
		}
		return true;
	}
	private boolean samePinsAs(Mos t) {
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
	public Mos(PartType np, PartNameProxy name, double width, double length,
			   Wire src, Wire gate, Wire drn) {
		this(np, name, width, length, new Wire[] {src, gate, drn});
	}

    public PartType getType() {return type;}
    public double getLength() {return length;}
    public double getWidth() {return width;}
	public int numSeries() {return pins.length-2;}
	public int[] getPinCoeffs() {return pin_coeffs;}

	private boolean touchesSomeGate(Wire w){
		for (int i=1; i<pins.length-1; i++)  if (w==pins[i]) return true;
		return false;
	}

	public boolean touchesOneDiffPinAndNoOtherPins(Wire w) {
		return (w==pins[0] ^ w==pins[pins.length-1]) &&
			   !touchesSomeGate(w);
	}

	public boolean isCapacitor() {return pins[0]==pins[pins.length-1];}

	public Integer hashCodeForParallelMerge() {
		// include how many Wires may be connected
		int hc = pins.length;
		// include what's connected
		for (int i=0; i<pins.length; i++)  
			hc += pins[i].hashCode() * pin_coeffs[i];
		// include the class
		hc += getClass().hashCode();
		// include whether its NMOS or PMOS
		hc += type.hashCode();
		return new Integer(hc);
	}

	// merge into this transistor
	public boolean parallelMerge(Part p){
		if(!(p instanceof Mos)) return false;
		Mos t= (Mos) p;
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
			   (type.getOrdinal() << tw+1) +
			   (numSeries() << tw+1+TYPES.log2NumTypes());
	}

	// ---------- printing methods ----------

	public String typeString() {
		String t = type.getName();
		String c = isCapacitor() ? "_CAP" : "";
		String h = pins.length==3 ? "" : ("_"+(pins.length-2)+"stack");
		return t+c+h;
	}

	public String valueDescription(){
		return "W=" + NccUtils.round(width,2) + " L=" + NccUtils.round(length, 2);
	}
	public String connectionDescription(int n) {
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

			// RKao debug
			//Wire w = pins[i];
			//msg += "(hash="+w.getCode()+")";

		}
		return msg;
	}
	
	public String connectionDescription(Wire w) {
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

	/** Compare the type (N vs P) and the gate length
	 * @param t Transistor to compare to
	 * @return true if type and gate length match */
	public boolean isLike(Mos t){
		return type==t.type && length==t.length;
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
		Set<Mos> trans = new HashSet<Mos>();
		for (Iterator<Part> it=w.getParts(); it.hasNext();) {
			Part p = it.next();
			if (p.isDeleted()) continue;
			if (!(p instanceof Mos)) return false;
			Mos t = (Mos) p;
			if (!t.touchesOneDiffPinAndNoOtherPins(w)) return false;
			trans.add(t);
			if (trans.size()>2) return false;
		}
		if (trans.size()!=2) return false;

		Iterator<Mos> it = trans.iterator();
		Mos ta = it.next();
		Mos tb = it.next();
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
		Mos stack = new Mos(ta.getType(), ta.getNameProxy(),  
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
			sumLo += pins[i].getCode() * pin_coeffs[i];
			int j = pins.length-1-i;
			sumHi += pins[j].getCode() * pin_coeffs[j];
		}
		return new Integer(sumLo * sumHi);
	}

}
