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
    private static int numCon= 2;
    private static int termCoefs[] = {23,51};
    private float myWidth= 0;
    private float myLength= 0;

    // ---------- private methods ----------
    protected TransistorZero(Name n){
		super(n, numCon);
		return;
    } //end of constructor

    // ---------- public methods ----------

    public static TransistorZero please(){
		TransistorZero t= new TransistorZero(null);
		return t;
    } //end of please

    public static TransistorZero please(Name n){
		TransistorZero t= new TransistorZero(n);
		return t;
    } //end of please

    public static TransistorZero please(JemCircuit parent, Name n){
		if(parent == null) return please(n);
		TransistorZero t= new TransistorZero(n);
		parent.adopt(t);
		return t;
    } //end of please
	
    //given a TransistorOne this returns a TransistorZero or null
    public static TransistorZero please(TransistorOne t1){
        //check to see that source and drain are the same
        List ww= t1.getMyWires();
        Wire source= (Wire)ww.get(0);
        Wire gate= (Wire)ww.get(1);
        Wire drain= (Wire)ww.get(2);
        if(source != drain)return null; //not convertable
	//get a new zero with the name and no parent
		TransistorZero t0= please((JemCircuit)t1.getParent(), t1.getTheName());
		t0.connect(source, gate);
		t0.myType= t1.myType;
		t1.deleteMe();
		return t0;
    } //end of constructor
	
    // ---------- abstract commitment ----------

	public boolean isThisGate(int x){return (x == 1);}
    public int size(){return numCon;}
    //    public boolean remove(NetObject j){return super.remove(j);}
    public int getNumCon(){return numCon;}
    public int[] getTermCoefs(){return termCoefs;} //the terminal coeficients

    // ---------- public methods ----------

    public void connect(Wire source, Wire gate){
	List c= (ArrayList)getMyWires();
        c.set(0, source);
        c.set(1, gate);
        if(source!=null)source.add(this);
	if(gate!=null)gate.add(this);
	return;
    } //end of connect

    public boolean touchesAtGate(Wire w){
	ArrayList c=(ArrayList)getMyWires();
	Wire x= (Wire)c.get(1);
        if(w == x)return true;
        return false;
    } //end of touchesAtGate

    public boolean touchesAtDiffusion(Wire w){
	ArrayList c=(ArrayList)getMyWires();
	Wire x= (Wire)c.get(0);
        if(w == x)return true;
        return false;
    } //end of touchesAtDiffusion

    //merge into this transistor
    public boolean parallelMerge(Part p){
        if(!(p instanceof TransistorZero)) return false;
        TransistorZero t= (TransistorZero)p;
        if(this == t)return false; //same transistor
		if(t.getClass()!=getClass())return false; //wrong type
		//its the same class but a different individual
		// myMessenger.line("Comparing " + nameString() +
        //" to " + t.nameString());
		List ca= (ArrayList)getMyWires();
		List cb= (ArrayList)t.getMyWires();
		if(ca.get(1) != cb.get(1))return false;
		if(ca.get(0) != cb.get(0))return false;
		//OK to merge topologically
		if(myLength != t.myLength)return false; //can't merge
	    //myMessenger.line("Merging " + nameString() +
	    //	  " and " + t.nameString());
		myWidth= myWidth + t.myWidth;
		t.deleteMe();
		return true; //return true if merged
    } //end of parallelMerge
	
    // ---------- printing methods ----------

    public String nameString(){
		String s= super.nameString();
		return (s + "TransCap " + getStringName());
    } //end of name string
    
    public String connectionString(int n){
		ArrayList c= (ArrayList)getMyWires();
		Wire w= null;
        String s= "unconnected";
        String g= "unconnected";
		w= (Wire)c.get(0);
        if(w!=null)s= w.getStringName();
		w= (Wire)c.get(1);
        if(w!=null)g= w.getStringName();
		return ("S=" + s + " G=" + g);
    } //end of connectionString
    
} //end of TransistorZero
