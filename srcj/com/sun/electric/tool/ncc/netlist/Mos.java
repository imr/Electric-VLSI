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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.sun.electric.technology.PrimitiveNode.Function;
import com.sun.electric.tool.ncc.NccOptions;
import com.sun.electric.tool.ncc.basic.NccUtils;
import com.sun.electric.tool.ncc.basic.Primes;
import com.sun.electric.tool.ncc.netlist.NccNameProxy.PartNameProxy;
import com.sun.electric.tool.ncc.trees.Circuit;
import com.sun.electric.tool.Job;

/** One or more MOS transistors in series. All gates have the same width
 * and length. 
 * If MOS has body port then last entry in pins array is body wire */
public class Mos extends Part {
	private static class GateType implements PinType {
		private final int numSeries;
		private final Function np;
		private final int gateHeight;
		private final boolean cap;
		private final boolean hasBody;

		public String description() {
			String t = np.getShortName();
			String c = cap ? "_CAP" : "";
			String h = numSeries==1 ? "" : ("_"+numSeries+"stack");
			String s = hasBody ? "_withBody" : "";
			int hiGate = numSeries+1 - gateHeight;
			String g = "";
			if (numSeries>2) {
				g = gateHeight + (gateHeight==hiGate ? "" : ("/"+hiGate)); 
			} 
			return t+c+h+s+" gate"+g;
		}
		public GateType(Function np, int numSeries, int gateHeight, boolean cap,
				        boolean hasBody) {
			Job.error(np==null, "null type?");
			Job.error(numSeries<1, "bad numSeries");
			int highestGateInLowerHalfOfStack = (numSeries+1)/2;
			Job.error(gateHeight>highestGateInLowerHalfOfStack, "bad gate Height");
			this.np = np;
			this.numSeries = numSeries;
			this.gateHeight = gateHeight;
			this.cap = cap;
			this.hasBody = hasBody;
		}
	}
	private static class DiffType implements PinType {
		private final int numSeries;
		private final Function np;
		private final boolean cap;
		private final boolean hasBody;

		public String description() {
			String t = np.getShortName();
			String c = cap ? "_CAP" : "";
			String h = numSeries==1 ? "" : ("_"+numSeries+"stack");
			String s = hasBody ? "_withBody" : "";
			return t+c+h+s+" diffusion";
		}
		public DiffType(Function np, int numSeries, boolean cap, 
				        boolean hasBody) {
			Job.error(np==null, "null type?");
			Job.error(numSeries<1, "bad numSeries");
			this.np = np;
			this.numSeries = numSeries;
			this.cap = cap;
			this.hasBody = hasBody;
		}
	}
	private static class BodyType implements PinType {
		private final int numSeries;
		private final Function np;
		private final boolean cap;
		public String description() {
			String t = np.getShortName();
			String c = cap ? "_CAP" : "";
			String h = numSeries==1 ? "" : ("_"+numSeries+"stack");
			return t+c+h+"_withBody body";
		}
		public BodyType(Function np, int numSeries, boolean cap) {
			Job.error(np==null, "null type?");
			Job.error(numSeries<1, "bad numSeries");
			this.np = np;
			this.numSeries = numSeries;
			this.cap = cap;
		}
	}
	/** Set of all the pins for a particular Transistor */ 
	private static class PinTypeSetKey {
		private Function type;
		private boolean isCapacitor;
		private int numSeries;
		private boolean hasBody;
		public PinTypeSetKey(Function type, boolean isCapacitor, int numSeries,
				             boolean hasBody) {
			this.type = type;
			this.isCapacitor = isCapacitor;
			this.numSeries = numSeries;
			this.hasBody = hasBody;
		}
	    @Override
		public boolean equals(Object o) {
			if (!(o instanceof PinTypeSetKey)) return false;
			PinTypeSetKey p = (PinTypeSetKey) o;
			return type==p.type && isCapacitor==p.isCapacitor && numSeries==p.numSeries
			       && hasBody==p.hasBody;
		}
	    @Override
		public int hashCode() {
			return type.hashCode() + (isCapacitor?1:0) + (numSeries<<1);
		}
	}
	private static final Map<PinTypeSetKey,PinType[]> TYPE_TO_PINTYPE_ARRAY = new HashMap<PinTypeSetKey,PinType[]>();
	
	private synchronized PinType[] getPinTypeArray() {
		PinTypeSetKey key = new PinTypeSetKey(type(), isCapacitor(), numSeries(),
				                              hasBody);
		PinType[] pinTypeArray = TYPE_TO_PINTYPE_ARRAY.get(key);
		if (pinTypeArray==null) {
			pinTypeArray = new PinType[pins.length];
			TYPE_TO_PINTYPE_ARRAY.put(key, pinTypeArray);
			
			pinTypeArray[0] = pinTypeArray[nbGateDiffPins()-1] =
				new DiffType(type(), numSeries(), isCapacitor(), hasBody);

			int maxHeight = (numSeries()+1) / 2;
			for (int gateHeight=1; gateHeight<=maxHeight; gateHeight++) {
				pinTypeArray[gateHeight] = 
					pinTypeArray[nbGateDiffPins()-1-gateHeight] = 
					new GateType(type(), numSeries(), gateHeight, isCapacitor(), hasBody);
			}
			if (hasBody) {
				pinTypeArray[pinTypeArray.length-1] = new BodyType(type(), numSeries(), isCapacitor());
			}
		}
		return pinTypeArray;
	}
    @Override
	public synchronized PinType getPinTypeOfNthPin(int n) {
		return getPinTypeArray()[n];
	}
	
	/** Generate arrays of pin coefficients on demand. Share these arrays
	 * between identically sized Transistors */
	private static class CoeffGen {
		private static ArrayList<int[]> coeffArraysNoBody = new ArrayList<int[]>();
		private static ArrayList<int[]> coeffArraysBody = new ArrayList<int[]>();
		private static void ensureListEntry(ArrayList<int[]> coeffArrays, int numPins) {
				while (coeffArrays.size()-1<numPins)  coeffArrays.add(null);
		}
		public static int[] getCoeffArray(int nbGateDiff, boolean withBody) {
			ArrayList<int[]> coeffArrays = withBody ? coeffArraysBody : coeffArraysNoBody;
			ensureListEntry(coeffArrays, nbGateDiff);
			int[] coeffArray = coeffArrays.get(nbGateDiff);
			if (coeffArray==null) {
				coeffArray = new int[nbGateDiff+(withBody?1:0)];
				for (int i=0, j=nbGateDiff-1; i<(nbGateDiff+1)/2; i++,j--) {
					int nthPrime = 30 + i + nbGateDiff;
					coeffArray[i] = coeffArray[j] = Primes.get(nthPrime);
				}
				if (withBody) {
					coeffArray[coeffArray.length-1] = Primes.get(30);
				}
				coeffArrays.set(nbGateDiff, coeffArray);
			}
			return coeffArray;
		}
	}

    // ---------- private data -------------
    /** array of pin coefficients */ private final int[] pin_coeffs;
    /** channel width */ private double width;
    /** channel length */ private final double length;
    /** true if transistor has a body connection */ private final boolean hasBody;
    
    // ---------- private methods ----------
	/** Stack of series transistors. If Mos has body connection then 
	 * body is last entry of pins */
	private Mos(Function np, PartNameProxy name, double width, double length,
				boolean hasBody, Wire[] pins) {
		super(name, np, pins);
		this.width = width;
		this.length = length;
		this.hasBody = hasBody;
		Job.error(np==null, "null type?");
		
		int nbGateDiff = pins.length - (hasBody?1:0);
		
		pin_coeffs = CoeffGen.getCoeffArray(nbGateDiff, hasBody);
		Job.error(pins.length!=pin_coeffs.length, "wrong number of pin coeffs");
	}
	
	private int nbGateDiffPins() {
		return hasBody ? pins.length-1 : pins.length;
	}
	
	private int bodyNdx() {return hasBody ? pins.length-1 : -1;}
	
	private boolean bodyMatches(Mos t) {
		if (hasBody!=t.hasBody) return false;
		if (!hasBody) return true;
		if (pins[bodyNdx()]!=t.pins[t.bodyNdx()]) return false;
		
		return true;
	}

	private boolean matchForward(Mos t) {
		for (int i=0; i<nbGateDiffPins(); i++) {
			if (pins[i]!=t.pins[i]) return false;
		}
		return true;
	}
	private boolean matchReverse(Mos t) {
		int nbGateDiff = nbGateDiffPins();
		for (int i=0, j=nbGateDiff-1; i<nbGateDiff; i++, j--) {
			if (pins[i]!=t.pins[j]) return false;
		}
		return true;
	}
	private boolean samePinsAs(Mos t) {
		if (pins.length!=t.pins.length) return false;
		if (hasBody!=t.hasBody) return false;
		if (!bodyMatches(t)) return false;
		return matchForward(t) || matchReverse(t);
	}
	private void flip() {
		int nbGateDiff = nbGateDiffPins();
		for (int i=0, j=nbGateDiff-1; i<nbGateDiff/2; i++,j--) {
			Wire w = pins[i];
			pins[i] = pins[j];
			pins[j] = w;
		}
	}
	private Wire hiDiff() {return pins[nbGateDiffPins()-1];}
	private Wire loDiff() {return pins[0];}

    // ---------- public methods ----------

	/** Transistor without body port. */
	public Mos(Function np, PartNameProxy name, double width, double length,
			   Wire src, Wire gate, Wire drn) {
		this(np, name, width, length, false, new Wire[] {src, gate, drn});
	}
	/** Transistor with body port. */
	public Mos(Function np, PartNameProxy name, double width, double length,
			   Wire src, Wire gate, Wire drn, Wire body) {
		this(np, name, width, length, true, new Wire[] {src, gate, drn, body});
	}
    @Override
    public double getLength() {return length;}
    @Override
    public double getWidth() {return width;}
	public int numSeries() {return nbGateDiffPins()-2;}
    @Override
	public int[] getPinCoeffs() {return pin_coeffs;}

    /** @return true if Wire w is connected to one or more gates */
	private boolean touchesSomeGate(Wire w){
		for (int i=1; i<nbGateDiffPins()-1; i++)  if (w==pins[i]) return true;
		return false;
	}

	/** @return true if Wire w connects to exactly one diffusion and w
	 *  doesn't connect to any gate
	 */
	public boolean touchesOnlyOneDiffAndNoGate(Wire w) {
		return (w==loDiff() ^ w==hiDiff()) &&
			   !touchesSomeGate(w);
	}

	/** @return true if Mos is a capacitor. That is both diffusions are connected */
	public boolean isCapacitor() {return pins[0]==pins[nbGateDiffPins()-1];}

	@Override
	public Integer hashCodeForParallelMerge() {
		// include how many Wires may be connected
		int hc = pins.length;
		// include what's connected
		for (int i=0; i<pins.length; i++)  
			hc += pins[i].hashCode() * pin_coeffs[i];
		// include the class
		hc += getClass().hashCode();
		// include whether its NMOS or PMOS
		hc += type().hashCode();
		return new Integer(hc);
	}

	// merge into this transistor
	@Override
	public boolean parallelMerge(Part p, NccOptions nccOpt){
		if(!(p instanceof Mos)) return false;
		Mos t= (Mos) p;
		if (this==t)return false; //same transistor

		if (!this.isLike(t, nccOpt)) return false; //different type

		// a different individual same type and length
		if (!samePinsAs(t)) return false;
		
		width += t.width;
		t.setDeleted();
		return true;		    	
	}
	@Override
	public int typeCode() {
		final int tw = Part.TYPE_FIELD_WIDTH;
		return type().ordinal() +
			   ((isCapacitor()?1:0) << tw) +
			   ((hasBody?1:0) << tw+1) +
			   (numSeries() << tw+2);
	}

	// ---------- printing methods ----------
	@Override
	public String typeString() {
		String t = type().getShortName();
		String c = isCapacitor() ? "_CAP" : "";
		String h = nbGateDiffPins()==3 ? "" : ("_"+(nbGateDiffPins()-2)+"stack");
		String s = hasBody ? "_withBody" : "";
		return t+c+h+s;
	}
	@Override
	public String valueDescription(){
		return "W=" + NccUtils.round(width,2) + " L=" + NccUtils.round(length, 2);
	}
	@Override
	public String connectionDescription(int n) {
		StringBuffer msg = new StringBuffer();
		for (int i=0; i<pins.length; i++) {
			if (i==0) {
				msg.append("S=");
			} else if (i==nbGateDiffPins()-1) {
				msg.append(" D=");
			} else if (hasBody && i==bodyNdx()) {
				msg.append(" B=");
			} else {
				if (nbGateDiffPins()==3) {
					msg.append(" G="); 
				} else {
					msg.append(" G"+i+"=");
				}
			}
			msg.append(pins[i].getName());
		}
		return msg.toString();
	}
	@Override
	public String connectionDescription(Wire w) {
		StringBuffer s = new StringBuffer();
		for (int i=0; i<pins.length; i++) {
			if (pins[i]!=w)  continue;
			if (s.length()!=0) s.append(",");
			if (i==0) {
				s.append("S");
			} else if (i==nbGateDiffPins()-1) {
				s.append("D"); 
			} else if (hasBody && i==bodyNdx()) {
				s.append("B");
			} else {
				if (nbGateDiffPins()==3) {
					s.append("G"); 
				} else {
					s.append("G"+i);
				}
			}
		}
		return s.toString();
	}

	/** Compare the type (N vs P) and the gate length
	 * @param t Transistor to compare to
	 * @return true if type and gate length match */
	public boolean isLike(Mos t, NccOptions nccOpt){
		return type()==t.type() && NccUtils.sizesMatch(length, t.length, nccOpt);
    }
	
	/** Merge two series Transistors into a single Transistor. 
	 * Tricky: Parts on wire may be deleted or replicated. 
	 * @param w wire joining diffusions of two transistors
	 * @return true if merge has taken place */
	public static boolean joinOnWire(Wire w, NccOptions nccOpt) {
		if (w.isDeleted()) return false;

		// make sure there are no Ports on wire
		if (w.getPort()!=null)  return false;
		
		// Use Set to remove duplicate Parts
		Set<Mos> trans = new HashSet<Mos>();
		for (Iterator<Part> it=w.getParts(); it.hasNext();) {
			Part p = it.next();
			if (p.isDeleted()) continue;
			if (!(p instanceof Mos)) return false;
			Mos t = (Mos) p;
			if (!t.touchesOnlyOneDiffAndNoGate(w)) return false;
			trans.add(t);
			if (trans.size()>2) return false;
		}
		if (trans.size()!=2) return false;

		Iterator<Mos> it = trans.iterator();
		Mos ta = it.next();
		Mos tb = it.next();
		error(ta.getParent()!=tb.getParent(), "mismatched parents?");
		if (!ta.isLike(tb, nccOpt))  return false;
		if (!NccUtils.sizesMatch(ta.width, tb.width, nccOpt))  return false;
		if (!ta.bodyMatches(tb)) return false;
		
		// it's a match - merge them into a stack
		if (ta.hiDiff()!=w) ta.flip();
		if (tb.loDiff()!=w) tb.flip();
		error(ta.hiDiff()!=w || tb.loDiff()!=w, "joinOnWire: diffusion connections corrupted");
		
		boolean hasBody = ta.hasBody;
		Wire[] mergedPins = new Wire[ta.nbGateDiffPins() + tb.nbGateDiffPins() - 2 + (hasBody?1:0)];
		int aNdx = 0;
		for (; aNdx<ta.nbGateDiffPins()-1; aNdx++) {
			mergedPins[aNdx] = ta.pins[aNdx];
		}
		for (int bNdx=1; bNdx<tb.nbGateDiffPins(); bNdx++){
			mergedPins[aNdx++] = tb.pins[bNdx];
		}
		if (hasBody) mergedPins[mergedPins.length-1] = ta.pins[ta.bodyNdx()];
		Mos stack = new Mos(ta.type(), ta.getNameProxy(),  
							ta.getWidth(), ta.getLength(), 
							hasBody, mergedPins);

		Circuit parent = tb.getParent();
		parent.adopt(stack);
		ta.setDeleted();
		tb.setDeleted();
		//error(w.numParts()!=0, "wire not empty?");
		w.setDeleted();
		return true;
	}
    @Override
	public Integer computeHashCode(){
		// the function is symmetric: ABCD = DCBA
    	int nbGateDiff = nbGateDiffPins();
		int sumLo=0, sumHi=0;
		for (int i=0, j=nbGateDiff-1; i<(nbGateDiff+1)/2; i++,j--){
			sumLo += pins[i].getCode() * pin_coeffs[i];
			sumHi += pins[j].getCode() * pin_coeffs[j];
		}
		int sum = sumLo * sumHi;
		if (hasBody) sum += pins[bodyNdx()].getCode() * pin_coeffs[bodyNdx()];
		return new Integer(sum);
	}

}
