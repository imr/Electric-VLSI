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
//	Updated 9 October 2003

package com.sun.electric.tool.ncc.jemNets;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.trees.JemCircuit;
import com.sun.electric.tool.ncc.trees.NetObject;

//import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;


public class Port extends NetObject {
    // ---------- private data -------------
	private Wire wire;    
    
    // ---------- public methods ----------
	public Port(String name, Wire w) {
		super(name);  
		wire = w;
		wire.add(this);
	}

    public Type getNetObjType() {return Type.PORT;}

	public Iterator getConnected() {return (new ArrayList()).iterator();}

    // ---------- abstract commitment ----------
    public int size(){return 1;}

    public void connect(Wire w){
        wire = w;
		wire.add(this);
    }

	public Wire getMyWire(){return wire;}

    public String nameString(){
        return ("Port " + getName());
    }

    public String connectionString(int n){return "is on Wire: "+wire.getName();}

	public void checkMe(JemCircuit parent){
		error(parent!=getParent(), "wrong parent");
		error(wire==null, nameString() + " has null connection");
		error(!wire.touches(this),
			  nameString()+" has inconsistant connection to " + 
			  wire.nameString());
	}

	public void printMe(int maxCon, Messenger messenger){
		String n= nameString();
		String c= connectionString(maxCon);
		messenger.println(n + " " + c);
	}
}

