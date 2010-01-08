/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Subcircuit.java
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
import com.sun.electric.tool.ncc.netlist.NccNameProxy.PartNameProxy;
import com.sun.electric.tool.ncc.processing.SubcircuitInfo;
import com.sun.electric.tool.Job;

/** A Cell instance that is being treated as a primitive circuit component
 * during a hierarchical netlist comparison */
public class Subcircuit extends Part {
	/** presume that no ports are interchangeable */
	public static class SubcircuitPinType implements PinType {
		private int typeCode;
		private int portIndex;
		private String description;
		public SubcircuitPinType(int typeCode, int portIndex, String description) {
			this.typeCode = typeCode;
			this.portIndex = portIndex;
			this.description = description;
		}
		public String description() {return description;}
	}

	private final int[] pinCoeffs;
	// For each wire, store the sum of the coefficients of the pins attached to
	// that wire
	private final Map<Wire,Integer> wireToCoeffSum;
	private final SubcircuitInfo subcircuitInfo;
	
	private String getPortName(int i) {return subcircuitInfo.getPortName(i);}
	
	private Map<Wire,Integer> computeWireToCoeffSum(Wire[] pins, int[] coeffs) {
		Map<Wire,Integer> wireToCoeffs = new HashMap<Wire,Integer>();
		for (int i=0; i<pins.length; i++) {
			Wire w = pins[i];
			int coeff = pinCoeffs[i];
			Integer coeffSum = wireToCoeffs.get(w);
			if (coeffSum==null)  coeffSum = new Integer(0);
			coeffSum += coeff;
			wireToCoeffs.put(w, coeffSum);
		}
		return wireToCoeffs;
	}

	@Override
	public String valueDescription() {return "";}
	@Override
	public int[] getPinCoeffs() {return pinCoeffs;}
	@Override
	public boolean parallelMerge(Part p, NccOptions nccOpt) {
		Job.error(true, "we never parallel merge subcircuits so don't call this method");
		return false;
	}
	
	
	/** I never parallel merge subcircuits so this really doesn't matter. 
	 * This method is too slow for subcircuits with tens of thousands of
	 * pins. It's better to avoid calling this method at a higher level.*/
	@Override
	public Integer hashCodeForParallelMerge() {
		Job.error(true, "we never parallel merge subcircuits so don't call this method");
		
		// include how many Wires may be connected
		int hc = pins.length;
		// include what's connected
		for (int i=0; i<pins.length; i++)  
			hc += pins[i].hashCode() * pinCoeffs[i];
		// include the class
		hc += getClass().hashCode();
		// include subcircuit ID
		hc += subcircuitInfo.getID();
		return new Integer(hc);
	}
	@Override
	public String typeString() {return subcircuitInfo.getName();}
	
	@Override
	public int typeCode() {
		return type().ordinal() + 
		       (subcircuitInfo.getID() << Part.TYPE_FIELD_WIDTH);
	}
	@Override
	public PinType getPinTypeOfNthPin(int n) {
		PinType[] pinTypes = subcircuitInfo.getPinTypes();
		return pinTypes[n];
	}
	@Override
	public String connectionDescription(Wire w) {
		String msg = "";
		for (int i=0; i<pins.length; i++) {
			if (pins[i]==w) {
				if (msg.length()!=0) msg += ',';
				msg += getPortName(i);
			}
		}
		return msg; 
	}
	@Override
	public String connectionDescription(int maxCon) {
		String msg = "";
		for (int i=0; i<maxCon && i<pins.length; i++) {
			if (msg.length()!=0) msg += " ";
			msg += getPortName(i)+"="+pins[i].getName();
		}
		return msg;
	}
	
	/**  Subcircuits can have tens of thousands of pins. Part.getHashFor(Wire)
	 * is O(n) in the number of pins. Since it gets called n times we have
	 * O(n^2) execution time. Subcircuit.getHashFor(Wire) takes more 
	 * storage but executes in constant time.
	 * @param w the Wire for which a hash code is needed
	 * @return an int with the code contribution. */
	@Override
    public int getHashFor(Wire w) {
		Integer coeffSum = wireToCoeffSum.get(w);
		Job.error(coeffSum==null, "Wire not found");
		return coeffSum*getCode();
    }
	
	public Subcircuit(PartNameProxy instName, SubcircuitInfo subcircuitInfo,
	                  Wire[] pins) {
		super(instName, Function.UNKNOWN, pins);
		this.subcircuitInfo = subcircuitInfo;
		pinCoeffs = subcircuitInfo.getPortCoeffs();
		
		wireToCoeffSum = computeWireToCoeffSum(pins, pinCoeffs);
	}
}
