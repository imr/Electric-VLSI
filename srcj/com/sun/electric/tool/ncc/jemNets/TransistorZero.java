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
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.jemNets.Part;
import com.sun.electric.tool.ncc.jemNets.Wire;
import com.sun.electric.tool.ncc.trees.JemCircuit;

import java.util.ArrayList;
import java.util.List;

public class TransistorZero extends Transistor {
    private static final int[] termCoefs = {23,51};

    // ---------- private methods ----------
    private TransistorZero(Type type, String name, double width, double length,
    					   Wire src, Wire gate) {
    	super(type, name, width, length, new Wire[] {src, gate});
    }

    // ---------- public methods ----------

    // given a TransistorOne this returns a TransistorZero
    public static TransistorZero please(TransistorOne t1){
        //check to see that source and drain are the same
        Wire source= t1.pins[0];
        Wire gate= t1.pins[1];
        Wire drain= t1.pins[2];
        error(source!=drain, "not convertible to TransistorZero");

		//get a new zero with the name and no parent
		TransistorZero t0 = new TransistorZero(t1.getType(), t1.getName(),
											   t1.getWidth(), t1.getLength(),
											   source, gate);
		JemCircuit parent = (JemCircuit)t1.getParent();
		parent.adopt(t0);
		t1.deleteMe();
		return t0;
    }
	
    // ---------- abstract commitment ----------

	public boolean isThisGate(int x){return (x == 1);}
    public int[] getTermCoefs(){return termCoefs;}

    // ---------- public methods ----------

    public boolean touchesAtGate(Wire w) {return w==pins[1];}

    public boolean touchesAtDiffusion(Wire w){return w==pins[0];}

    //merge into this transistor
    public boolean parallelMerge(Part p){
        if(!(p instanceof TransistorZero)) return false;
        TransistorZero t= (TransistorZero)p;
        error(this==t, "parallel merge with self?");

		if(pins[1]!=t.pins[1] || pins[0]!=t.pins[0])  return false;

		if(getLength()!=t.getLength()) return false;
		width += t.width;
		t.deleteMe();
		return true;
    }
	
    // ---------- printing methods ----------

    public String nameString(){
		return super.nameString() + "TransCap " + getName();
    }
    
    public String connectionString(int n){
        String s= pins[0].getName();
        String g= pins[1].getName();
		return ("S=" + s + " G=" + g);
    }
    
}
