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
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.trees.JemCircuit;
import com.sun.electric.tool.ncc.trees.NetObject;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashSet;

/** An NCC Port holds all the Export names associated with a single NCC
 * Wire. */ 
public class Port extends NetObject {
    // ---------- private data -------------
	private Wire wire;    
	private HashSet names = new HashSet();
    
    // ---------- public methods ----------
	public Port(String name, Wire w) {
		super("");
		wire = w;
		names.add(name);
	}
	public Type getNetObjType() {return Type.PORT;}
	public Iterator getConnected() {return (new ArrayList()).iterator();}
	
	public void addExportName(String nm) {names.add(nm);}

	public Wire getWire(){return wire;}

    public String connectionString(int n){return "is on Wire: "+wire.getName();}

	public void checkMe(JemCircuit parent){
		error(parent!=getParent(), "wrong parent");
		error(wire==null, nameString() + " has null connection");
		error(!wire.touches(this),
			  nameString()+" has inconsistant connection to " + 
			  wire.nameString());
	}
	public String exportNamesString() {
		String s= "";
		for (Iterator it=names.iterator(); it.hasNext();) {
			if (s.length()!=0)  s+=" ";
			s += (String) it.next();
		}
		return s;
	}
	public String nameString() {return "Port " + exportNamesString();}
	public Iterator getExportNames() {return names.iterator();}

	public void printMe(int maxCon, Messenger messenger){
		messenger.print("Port on wire: " + wire.getName() +
		                "has Export names: ");
		for (Iterator it=names.iterator(); it.hasNext();) {
			String nm = (String) it.next();
			messenger.print(" "+nm);
		}
		messenger.println();
	}
}

