/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Port.java
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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.result.PortReport.PortReportable;
import com.sun.electric.tool.ncc.trees.Circuit;

/** An NCC Port holds all the Export names associated with a single NCC
 * Wire. */ 
public class Port extends NetObject implements PortReportable {
    // ---------- private data -------------
	private Wire wire;
	/** name of each Export attached to Wire */    
	private List<String> names = new ArrayList<String>();
	/** type of each Export attached to Wire */
	private List<PortCharacteristic> types = new ArrayList<PortCharacteristic>();
	/** true if user indicates he wants NCC to choose the correct name based 
	 * upon topological equivalence */
	private boolean toBeRenamed = false;
    
    // ---------- public methods ----------
	public Port(String name, PortCharacteristic type, Wire w) {
		wire = w;
		names.add(name);
		types.add(type);
	}
	public String getName() {
		LayoutLib.error(names.size()==0, "Port with no name?");
		return names.iterator().next();
	}
	public Type getNetObjType() {return Type.PORT;}
	public Iterator<NetObject> getConnected() {return (new ArrayList<NetObject>()).iterator();}
	
	public void addExport(String nm, PortCharacteristic type, boolean oneNamePerPort) {
		if (oneNamePerPort) {
			LayoutLib.error(names.size()!=1, "expect exactly one Port name");
			if (TextUtils.STRING_NUMBER_ORDER.compare(names.get(0), nm)>0)   names.set(0, nm);
		} else {
			// compatibility mode only for old regressions
			names.add(nm);
		}
		types.add(type);
	}
	/** @return the type of Export. If a Wire has multiple Exports of
	 * different types then return the most common type. */
	public PortCharacteristic getType() {
		Map<PortCharacteristic,Integer> typeToCount = new HashMap<PortCharacteristic,Integer>();
		for (PortCharacteristic t : types) {
			Integer count = typeToCount.get(t);
			int c = count!=null ? count.intValue() : 0;
			typeToCount.put(t, new Integer(c+1));
		}
		int popularCount = 0;
		PortCharacteristic popularType = null;
		for (PortCharacteristic t : typeToCount.keySet()) {
			int count = typeToCount.get(t).intValue();
			if (count>popularCount ||
			    (count==popularCount && t!=PortCharacteristic.UNKNOWN)) {
				popularCount = count;
				popularType = t;
			}
		}
		return popularType;
	}
	
	public Wire getWire(){return wire;}
	public String getWireName() {return wire.getName();}

	public void checkMe(Circuit parent){
		error(parent!=getParent(), "wrong parent");
		error(wire==null, instanceDescription() + " has null connection");
		error(!wire.touches(this),
			  instanceDescription()+" has inconsistant connection to " + 
			  wire.instanceDescription());
	}
	public String exportNamesString() {
		StringBuffer sb = new StringBuffer();
		sb.append("{ ");
		// Sort the names
		for (String s : new TreeSet<String>(names)) {
			if (sb.length()>2)  sb.append(", ");
			sb.append(s);
		}
		sb.append(" }");
		return sb.toString();
	}
	public Iterator<String> getExportNames() {return names.iterator();}
	public boolean isDeleted() {return false;}
	public boolean isImplied() {
        return ! getWire().getNameProxy().getNet().getExports().hasNext();
	}
	public void setToBeRenamed() {toBeRenamed = true;}
	public boolean getToBeRenamed() {return toBeRenamed;}

	public String instanceDescription() {return "Port "+exportNamesString();}
	public String valueDescription() {return "";}
    public String connectionDescription(int n){return "is on Wire: "+wire.getName();}
}

