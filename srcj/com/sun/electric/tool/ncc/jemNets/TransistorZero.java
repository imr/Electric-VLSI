/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TransistorZero.java
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

//TransistorZero is a transistor with source and drain tied together
package com.sun.electric.tool.ncc.jemNets;
import com.sun.electric.tool.ncc.basicA.Name;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.jemNets.Part;
import com.sun.electric.tool.ncc.jemNets.Wire;
import com.sun.electric.tool.ncc.trees.JemCircuit;

import java.util.ArrayList;
import java.util.List;

public class TransistorZero extends Transistor {
    private static final int NUM_CON= 2;
    private static final int[] termCoefs = {23,51};
    private float myWidth= 0;
    private float myLength= 0;

    // ---------- private methods ----------
    protected TransistorZero(Name n) {super(n, NUM_CON);}

    // ---------- public methods ----------

    public static TransistorZero please(){
		return new TransistorZero(null);
    }

    public static TransistorZero please(Name n){
		return new TransistorZero(n);
    }

    public static TransistorZero please(JemCircuit parent, Name n){
		if(parent == null) return please(n);
		TransistorZero t= new TransistorZero(n);
		parent.adopt(t);
		return t;
    }
	
    //given a TransistorOne this returns a TransistorZero or null
    public static TransistorZero please(TransistorOne t1){
        //check to see that source and drain are the same
        Wire source= t1.pins[0];
        Wire gate= t1.pins[1];
        Wire drain= t1.pins[2];
        error(source!=drain, "not convertible to TransistorZero");

		//get a new zero with the name and no parent
		TransistorZero t0= please((JemCircuit)t1.getParent(), t1.getTheName());
		t0.connect(source, gate);
		t0.myType= t1.myType;
		t1.deleteMe();
		return t0;
    }
	
    // ---------- abstract commitment ----------

	public boolean isThisGate(int x){return (x == 1);}
    //    public boolean remove(NetObject j){return super.remove(j);}
    public int[] getTermCoefs(){return termCoefs;} //the terminal coeficients

    // ---------- public methods ----------

    public void connect(Wire source, Wire gate){
        pins[0] = source;
        pins[1] = gate;
        source.add(this);
		gate.add(this);
    }

    public boolean touchesAtGate(Wire w) {return w==pins[1];}

    public boolean touchesAtDiffusion(Wire w){return w==pins[0];}

    //merge into this transistor
    public boolean parallelMerge(Part p){
        if(!(p instanceof TransistorZero)) return false;
        TransistorZero t= (TransistorZero)p;
        if(this == t)return false; //same transistor
		if(t.getClass()!=getClass())return false; //wrong type
		//its the same class but a different individual
		// myMessenger.line("Comparing " + nameString() +
        //" to " + t.nameString());
		if(pins[1]!=t.pins[1]  || pins[0]!=t.pins[0])  return false;
		//OK to merge topologically
		if(myLength != t.myLength)return false; //can't merge
	    //myMessenger.line("Merging " + nameString() +
	    //	  " and " + t.nameString());
		myWidth= myWidth + t.myWidth;
		t.deleteMe();
		return true;
    }
	
    // ---------- printing methods ----------

    public String nameString(){
		return super.nameString() + "TransCap " + getStringName();
    }
    
    public String connectionString(int n){
        String s= pins[0].getStringName();
        String g= pins[1].getStringName();
		return ("S=" + s + " G=" + g);
    }
    
}
