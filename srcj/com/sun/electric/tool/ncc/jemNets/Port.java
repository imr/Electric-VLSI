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
package com.sun.electric.tool.ncc.jemNets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.tool.ncc.basic.Messenger;
import com.sun.electric.tool.ncc.trees.Circuit;

/** An NCC Port holds all the Export names associated with a single NCC
 * Wire. */ 
public class Port extends NetObject {
    // ---------- private data -------------
	private Wire wire;
	/** name of each Export attached to Wire */    
	private List names = new ArrayList();
	/** type of each Export attached to Wire */
	private List types = new ArrayList();
	/** true if user indicates he wants NCC to choose the correct name based 
	 * upon topological equivalence */
	private boolean toBeRenamed = false;
    
    // ---------- public methods ----------
	public Port(String name, Object type, Wire w) {
		super(null);
		wire = w;
		names.add(name);
		types.add(type);
	}
	public Type getNetObjType() {return Type.PORT;}
	public Iterator getConnected() {return (new ArrayList()).iterator();}
	
	public void addExport(String nm, PortProto.Characteristic type) {
		names.add(nm);
		types.add(type);
	}
	/** @return the type of Export. If a Wire has multiple Exports of
	 * different types then return the most common type. */
	public PortProto.Characteristic getType() {
		Map typeToCount = new HashMap();
		for (Iterator it=types.iterator(); it.hasNext();) {
			PortProto.Characteristic t = (PortProto.Characteristic) it.next();
			Integer count = (Integer) typeToCount.get(t);
			int c = count!=null ? count.intValue() : 0;
			typeToCount.put(t, new Integer(c+1));
		}
		int popularCount = 0;
		PortProto.Characteristic popularType = null;
		for (Iterator it=typeToCount.keySet().iterator(); it.hasNext();) {
			PortProto.Characteristic t = (PortProto.Characteristic) it.next();
			int count = ((Integer) typeToCount.get(t)).intValue();
			if (count>popularCount ||
			    (count==popularCount && t!=PortProto.Characteristic.UNKNOWN)) {
				popularCount = count;
				popularType = t;
			}
		}
		return popularType;
	}
	
	/** Warn user if all Exports on a single wire don't have the same type */
	public void warnIfExportTypesDiffer(String cellName) {
		PortProto.Characteristic popularType = getType();
		for (int i=0; i<names.size(); i++) {
			String nm = (String) names.get(i);
			PortProto.Characteristic t = (PortProto.Characteristic)types.get(i);
			if (t!=popularType) {
				System.out.println("In Cell: "+cellName+
                                   " the export: "+nm+
                                   " has Characteristic: "+t.toString()+
                                   " which differs from the most common Characteristic: "+
                                   popularType.toString()+
                                   " of the exports: "+
                                   exportNamesString());
			}
		}
	}

	public Wire getWire(){return wire;}

    public String connectionString(int n){return "is on Wire: "+wire.getName();}

	public void checkMe(Circuit parent){
		error(parent!=getParent(), "wrong parent");
		error(wire==null, nameString() + " has null connection");
		error(!wire.touches(this),
			  nameString()+" has inconsistant connection to " + 
			  wire.nameString());
	}
	public String exportNamesString() {
		StringBuffer sb = new StringBuffer();
		sb.append("{ ");
		// Sort the names
		for (Iterator it=(new TreeSet(names)).iterator(); it.hasNext();) {
			if (sb.length()>2)  sb.append(", ");
			sb.append((String) it.next());
		}
		sb.append(" }");
		return sb.toString();
	}
	public String nameString() {return "Port " + exportNamesString();}
	public Iterator getExportNames() {return names.iterator();}
	public boolean isDeleted() {return false;}
	public void setToBeRenamed() {toBeRenamed = true;}
	public boolean getToBeRenamed() {return toBeRenamed;}

	public void printMe(int maxCon, Messenger messenger){
		messenger.print("Port on wire: " + wire.getName() +
		                " has Export names:");
		for (Iterator it=names.iterator(); it.hasNext();) {
			String nm = (String) it.next();
			messenger.print(" "+nm);
		}
		messenger.println();
	}
}

