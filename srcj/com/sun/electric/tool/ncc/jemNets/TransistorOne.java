/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TransistorOne.java
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
//	Updated 16 October 2003 to use JemTree interface

package com.sun.electric.tool.ncc.jemNets;
import com.sun.electric.tool.ncc.basicA.Name;
import com.sun.electric.tool.ncc.basicA.Messenger;
// import com.sun.electric.tool.ncc.jemNets.Part;
// import com.sun.electric.tool.ncc.jemNets.Wire;
import com.sun.electric.tool.ncc.trees.JemCircuit;

import java.util.ArrayList;
import java.util.List;

public class TransistorOne extends Transistor {

    // ---------- private data -------------
    private static final int NUM_CON= 3;
    private static final int TERM_COEFFS[] = {47,13,47};

    // ---------- private methods ----------
    protected TransistorOne(Name n){super(n, NUM_CON);}

    public void flip(){
        Wire w= pins[0];
		pins[0] = pins[2];
        pins[2] = w;
    }

    // ---------- public methods ----------

    public static TransistorOne nPlease(JemCircuit cc, Name n){
		TransistorOne t= new TransistorOne(n);
		cc.adopt(t);
		t.myType= Transistor.Ntype;
		return t;
    }

    public static TransistorOne pPlease(JemCircuit cc, Name n){
		TransistorOne t= new TransistorOne(n);
		cc.adopt(t);
		t.myType= Transistor.Ptype;
		return t;
    }

    // ---------- abstract commitment ----------

	public boolean isThisGate(int x){return (x == 1);}
    public int[] getTermCoefs(){return TERM_COEFFS;} //the terminal coeficients

    // ---------- public methods ----------

    public void connect(Wire source, Wire gate, Wire drain){
        pins[0] = source;
        pins[1] = gate;
        pins[2] = drain;
        source.add(this);
		gate.add(this);
		drain.add(this);
    }

    public boolean touchesAtGate(Wire w){return w==pins[1];}

    public boolean touchesAtDiffusion(Wire w){
        return w==pins[0] || w==pins[2];
    }

    public boolean isCapacitor() {return(pins[0]==pins[2]);}

    //merge into this transistor
    public boolean parallelMerge(Part p){
        if(!(p instanceof TransistorOne)) return false;
        TransistorOne t= (TransistorOne)p;
        if(this == t)return false; //same transistor
		if( ! this.isLike(t))return false; //different type
        // a different individual same type and length

		// myMessenger.line("Comparing " + nameString() +
		//    " to " + t.nameString());

		if(pins[0]!=t.pins[0])  t.flip();
		if(pins[0]!=t.pins[0] || pins[2]!=t.pins[2])return false;
		
		myWidth += t.myWidth;
		t.deleteMe();
		return true;		    	
    }

    // ---------- printing methods ----------

    public String nameString(){
        String s= super.nameString();
        return (s + "TransOne " + getStringName());
    }

    public String connectionString(int n){
        String s= pins[0].getStringName();
        String g= pins[1].getStringName();
        String d= pins[2].getStringName();
        return ("S=" + s + " G=" + g + " D=" + d);
    }

}

