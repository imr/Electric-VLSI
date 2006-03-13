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
import com.sun.electric.tool.ncc.netlist.NccNameProxy.PartNameProxy;
import com.sun.electric.tool.ncc.processing.SubcircuitInfo;

/** A Cell instance that is being treated as a primitive circuit component
 * during a hierarchical netlist comparison */
public class Subcircuit extends Part {
	/** presume that no ports are interchangable */
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
	private final SubcircuitInfo subcircuitInfo;
	
	private String getPortName(int i) {return subcircuitInfo.getPortName(i);}
	
	public String valueDescription() {return "";}
	public int[] getPinCoeffs() {return pinCoeffs;}
	public boolean parallelMerge(Part p) {return false;}
	
	
	/** I never parallel merge subcircuits so this really doesn't matter. */
	public Integer hashCodeForParallelMerge() {
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
	public String typeString() {return subcircuitInfo.getName();}
	
	public int typeCode() {
		return Part.SUBCIRCUIT + 
		       (subcircuitInfo.getID() << Part.TYPE_FIELD_WIDTH);
	}
	public PinType getPinTypeOfNthPin(int n) {
		PinType[] pinTypes = subcircuitInfo.getPinTypes();
		return pinTypes[n];
	}
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
	public String connectionDescription(int maxCon) {
		String msg = "";
		for (int i=0; i<maxCon && i<pins.length; i++) {
			if (msg.length()!=0) msg += " ";
			msg += getPortName(i)+"="+pins[i].getName();
		}
		return msg;
	}
	
	public Subcircuit(PartNameProxy instName, SubcircuitInfo subcircuitInfo,
	                  Wire[] pins) {
		super(instName, pins);
		this.subcircuitInfo = subcircuitInfo;
		pinCoeffs = subcircuitInfo.getPortCoeffs();
	}
}
