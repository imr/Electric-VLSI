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
    private static int numCon= 3;
    private static int termCoefs[] = {47,13,47};

    // ---------- private methods ----------
    protected TransistorOne(Name n){
		super(n, numCon);
    } //end of constructor

    public void flip(){
		List c= getMyWires();
        Wire w= (Wire)c.get(0);
		c.set(0, c.get(2));
        c.set(2, w);
    } //end of flip

    // ---------- public methods ----------

    public static TransistorOne please(){
		TransistorOne t= new TransistorOne(null); //nameless
		return t;
    } //end of please

	/*    public static TransistorOne please(String n){
		TransistorOne t= new TransistorOne(n);
	return t;
    } //end of please
*/
    public static TransistorOne please(JemCircuit cc, Name n){
		TransistorOne t= new TransistorOne(n);
		cc.adopt(t);
		return t;
    } //end of please

    public static TransistorOne nPlease(JemCircuit cc, Name n){
		TransistorOne t= new TransistorOne(n);
		cc.adopt(t);
		t.myType= Transistor.Ntype;
		return t;
    } //end of nPlease

    public static TransistorOne pPlease(JemCircuit cc, Name n){
		TransistorOne t= new TransistorOne(n);
		cc.adopt(t);
		t.myType= Transistor.Ptype;
		return t;
    } //end of pPlease

    // ---------- abstract commitment ----------

	public boolean isThisGate(int x){return (x == 1);}
	public int size(){return numCon;}
    public int getNumCon(){return numCon;}
    public int[] getTermCoefs(){return termCoefs;} //the terminal coeficients

    // ---------- public methods ----------

    public void connect(Wire source, Wire gate, Wire drain){
		List c= (ArrayList)getMyWires();
        c.set(0, source);
        c.set(1, gate);
        c.set(2, drain);
        if(source!=null)source.add(this);
		if(gate!=null)gate.add(this);
		if(drain!=null)drain.add(this);
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
		Wire y= (Wire)c.get(2);
        if(w == x)return true;
        if(w == y)return true;
        return false;
    } //end of touchesAtDiffusion

    public boolean isCapacitor(){
		List ww= getMyWires();
		Wire x= (Wire)ww.get(0);
		Wire y= (Wire)ww.get(2);
		return(x == y);
    } //end of isAcapacitor

    //merge into this transistor
    public boolean parallelMerge(Part p){
        if(!(p instanceof TransistorOne)) return false;
        TransistorOne t= (TransistorOne)p;
        if(this == t)return false; //same transistor
		if( ! this.isLike(t))return false; //different type
        // a different individual same type and length

		// myMessenger.line("Comparing " + nameString() +
		//    " to " + t.nameString());

		List ca= getMyWires();
		List cb= t.getMyWires();
        if(ca.get(0) != cb.get(0))flip();
        if(ca.get(0) != cb.get(0))t.flip();
        if(ca.get(0) != cb.get(0))flip();
        //first connection must match
        if(ca.get(1) != cb.get(1))return false;
        if(ca.get(2) != cb.get(2))return false;
        //OK to merge topologically
		//myMessenger.line("Merging " + nameString() +
		//	  " and " + t.nameString());
        myWidth= myWidth + t.myWidth;
        t.deleteMe();
        return true; //return true if merged
    } //end of parallelMerge

    // ---------- printing methods ----------

    public String nameString(){
        String s= super.nameString();
        return (s + "TransOne " + getStringName());
    } //end of name string

    public String connectionString(int n){
        ArrayList c= (ArrayList)getMyWires();
        Wire w= null;
        String s= "unconnected";
        String g= "unconnected";
        String d= "unconnected";
        w= (Wire)c.get(0);
        if(w!=null)s= w.getStringName();
        w= (Wire)c.get(1);
        if(w!=null)g= w.getStringName();
        w= (Wire)c.get(2);
        if(w!=null)d= w.getStringName();
        return ("S=" + s + " G=" + g + " D=" + d);
    } //end of connectionString

} //end of Part

