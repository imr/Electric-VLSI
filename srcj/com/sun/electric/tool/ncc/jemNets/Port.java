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
import com.sun.electric.tool.ncc.basicA.Name;
import com.sun.electric.tool.ncc.trees.JemEquivRecord;
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
    private static final int NUM_CON= 1;
    private static final int termCoefs[] = {47};
    private static int portCount= 0; //the count of all Ports made
	private Wire wire;    
    
    // ---------- private methods ----------
    private Port(Name n){
		super(n);
		portCount++;
    }
    
    // ---------- public methods ----------
    
    public static Port please(Name n){
        return new Port(n);
    }

    public static Port please(JemCircuit cc, Name n){
        Port p= new Port(n);
	 	cc.adopt(p);
        return p;
    }
    
    public Type getNetObjType() {return Type.PORT;}

	public Iterator getConnected() {
		ArrayList l = new ArrayList();
		return l.iterator();
	}

	public boolean isThisGate(int x){return false;}
	
    // ---------- abstract commitment ----------
    public int size(){return 1;}
    public int getNumCon(){return NUM_CON;}
    public int[] getTermCoefs(){return termCoefs;} //the terminal coeficients

    public void connect(Wire w){
        wire = w;
		w.add(this);
    }

	public Wire getMyWire(){return wire;}

	/** 
	 * Compute a hash code for Wire w to use.  special because a Ports
	 * must not have an impact on a Wire unless retired
	 * @param the Wire for which hash code is needed
	 * @return the hash code for the wire to use
	 */
    public int getHashFor(Wire w){
		JemCircuit circuit= (JemCircuit)getParent();
		JemEquivRecord g= (JemEquivRecord)circuit.getParent();
		if(g.isRetired())return getCode();
		else return 0;
	}
	
	public Integer computeCode(int i){
		Wire w= getMyWire();
		int ii= getHashFor(w);
		return new Integer(ii);
	}
	
    public String nameString(){
        return ("Port " + getStringName());
    }

    public String connectionString(int n){
        String s= "is unconnected";
        if(wire!=null)  s = wire.getStringName();
        return ("is on " + s);
    }

	public void checkMe(JemCircuit parent){
		error(parent!=getParent(), "wrong parent");
		error(wire==null, nameString() + " has null connection");
		error(!wire.touches(this),
			  nameString()+" has inconsistant connection to " + 
			  wire.nameString());
	}

	public void printMe(int maxCon){
		String n= nameString();
		String c= connectionString(maxCon);
		Messenger.line(n + " " + c);
	}
	
}

