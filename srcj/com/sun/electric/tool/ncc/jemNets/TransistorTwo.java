/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TransistorTwo.java
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

/** 
 * TransistorTwo represents a two-gate transistor with both gates the
 * same width and length.  TransistorTwos can be symmetric or not
 * symmetric.  Symmetric means the two gates are interchangable Not
 * symmetric means the two gates are ordered.  has code computations
 * for non-symmetric TransistorTwo is harder than for symmetric
 * TransistorTwo.
*/
package com.sun.electric.tool.ncc.jemNets;
import com.sun.electric.tool.ncc.basicA.Name;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.jemNets.Part;
import com.sun.electric.tool.ncc.jemNets.Wire;
import com.sun.electric.tool.ncc.trees.JemCircuit;

import java.util.List;
import java.util.Iterator;

public class TransistorTwo extends Transistor{
    // ---------- private data -------------
    private static int numCon= 4;
    //these coeficients apply for symmetric case only
    private static int termCoefs[] = {7, 13, 13, 7};

    private boolean symmetric= false;
    public boolean isSymmetric(){return symmetric;}

    private void flip(){
		List c= getMyWires();
        Wire w= (Wire)c.get(0);
		c.set(0, c.get(3));
        c.set(3, w);
		w= (Wire)c.get(1);
		c.set(1, c.get(2));
        c.set(2, w);
    } //end of flip

    private void flipGates(){
		if(!symmetric)return; //won't flip gates
		List c= getMyWires();
		Wire w= (Wire)c.get(1);
		c.set(1, c.get(2));
		c.set(2, w);
    } //end of flipGates

    // ---------- private methods ----------
    protected TransistorTwo(Name n){
		super(n, numCon);
		symmetric= false;
    } //end of constructor

    // ---------- public methods ----------

    public static TransistorTwo please(){
		TransistorTwo t= new TransistorTwo(null);
		return t;
    } //end of please

    public static TransistorTwo please(Name n){
		TransistorTwo t= new TransistorTwo(n);
		return t;
    } //end of please

    public static TransistorTwo please(JemCircuit parent, Name n){
		if(parent == null) return please(n);
		TransistorTwo t= new TransistorTwo(n);
		parent.adopt(t);
		return t;
    } //end of please

    public static TransistorTwo nPlease(JemCircuit parent, Name n){
		TransistorTwo t= please(parent,n);
		t.myType= Transistor.Ntype;
		return t;
    } //end of nPlease

    public static TransistorTwo pPlease(JemCircuit parent, Name n){
		TransistorTwo t= please(parent,n);
		t.myType= Transistor.Ptype;
		return t;
    } //end of pPlease

    public static TransistorTwo joinOnWire(Wire w){
		TransistorOne t= null;
		TransistorOne ta= null;
		TransistorOne tb= null;
		Iterator it= w.iterator();
		while(it.hasNext()){
			Object oo= it.next();
			if( ! (oo instanceof TransistorOne))continue;
			t= (TransistorOne)oo;
			if(ta == null)ta= t;
			else if(tb == null)tb= t;
			else return null; //too many pins
		} //end of while
		if((ta == null) || (tb == null))return null;
		if(ta == tb) return null;
		if(ta.getParent() != tb.getParent())return null;
		if( ! ta.isLike(tb))return null;
		if(ta.myWidth != tb.myWidth)return null;
		// it's a match - make the new one
		List la= ta.getMyWires();
		List lb= tb.getMyWires();
        if(la.get(2) != w)ta.flip();
        if(lb.get(0) != w)tb.flip();
		if(la.get(2) != w)getMessenger().error("building TransistorTwo");
		if(lb.get(0) != w)getMessenger().error("building TransistorTwo");
		TransistorTwo t2= please((JemCircuit)tb.getParent(), tb.getTheName());
		t2.connect((Wire)la.get(0), (Wire)la.get(1),
			 (Wire)lb.get(1), (Wire)lb.get(2));
		t2.myLength= ta.myLength;
		t2.myWidth= ta.myWidth;
		t2.myType= ta.myType;
		ta.deleteMe();
		tb.deleteMe();
		if(w.size() == 0)return t2;
		return t2; //will get here only if a Port was on the wire
    } //end of joinOnWire

    // ---------- abstract commitment ----------

	public boolean isThisGate(int x){return ((x == 1) || (x == 2));}
    public int size(){return numCon;}

    public int getNumCon(){return numCon;}
    public int[] getTermCoefs(){return termCoefs;} //the terminal coeficients

    // ---------- public methods ----------

    public void connect(Wire source, Wire gateA, Wire gateB, Wire drain){
		List c= getMyWires();
        c.set(0, source);
        c.set(1, gateA);
        c.set(2, gateB);
        c.set(3, drain);
        if(source!=null)source.add(this);
		if(gateA!=null)gateA.add(this);
		if(gateB!=null)gateB.add(this);
		if(drain!=null)drain.add(this);
		return;
    } //end of connect

    public boolean touchesAtGate(Wire w){
		List c= getMyWires();
		Wire x= (Wire)c.get(1);
		Wire y= (Wire)c.get(2);
        if(w == x)return true;
        if(w == y)return true;
        return false;
    } //end of touchesAtGate

    public boolean touchesAtDiffusion(Wire w){
		List c= getMyWires();
		Wire x= (Wire)c.get(0);
		Wire y= (Wire)c.get(3);
        if(w == x)return true;
        if(w == y)return true;
        return false;
    } //end of touchesAtDiffusion

    public boolean isCapacitor(){
		List ww= getMyWires();
		Wire x= (Wire)ww.get(0);
		Wire y= (Wire)ww.get(3);
		if(x != y)return false;
		x= (Wire)ww.get(1);
		y= (Wire)ww.get(2);
		if(x != y)return false;
		return true;
    } //end of isAcapacitor

    //merge into this transistor
    public boolean parallelMerge(Part p){
        if(!(p instanceof TransistorTwo)) return false;
        TransistorTwo t= (TransistorTwo)p;
        if(this == t)return false; //same transistor
		//its the same class but a different individual
		if( ! this.isLike(t))return false; //wrong type transistor

		// myMessenger.line("Comparing " + nameString() +
		//    " to " + t.nameString());

		List ca= getMyWires();
		List cb= t.getMyWires();
		if(ca.get(0) != cb.get(0))flip();
		if(ca.get(0) != cb.get(0))t.flip();
		if(ca.get(0) != cb.get(0))flip();
		//source connection (0) now matches
		if(ca.get(1) != cb.get(1))flipGates();
		//first gate connection now matches if it can
		//symmetric has been handled in flipGates
		if((ca.get(1) == cb.get(2))&&(ca.get(2) == cb.get(1))){
			//swap match of non-symmetric ones
			symmetric= true;
		}else{
			if(ca.get(1) != cb.get(1))return false;
			if(ca.get(2) != cb.get(2))return false;
		} //end of else

		//OK to merge topologically
		//what remains can merge
		//		myMessenger.line("Merging " + nameString() +
		//		" and " + t.nameString());
		myWidth= myWidth + t.myWidth;
		t.deleteMe();
		return true; //return true if merged
    } //end of parallelMerge

    //over ride the hash computation
    protected Integer computeHashCode(){
		if(symmetric)return super.computeHashCode();
		//the function is symmetric: ABCD = DCBA
		//but not symmetric in gates: ABCD != ACBD
		int[] xx= new int[numCon];
		//get the wire values
		for(int i=0; i<size(); i++){
			Wire w= (Wire)getMyWires().get(i);
			if(w!=null)xx[i]= w.getCode();
			xx[i]= xx[i] * termCoefs[i];
		} //end of loop
		int sum= (xx[0] + xx[1]) * (xx[2] + xx[3]);
        return new Integer(sum);
    } //end of computeHashCode

    // ---------- printing methods ----------

    public String nameString(){
		String t= symmetric?"(Sym)":"(Asym)";
		String s= super.nameString();
		return (s + "TransTwo" + t + " " + getStringName());
    } //end of name string

    public String connectionString(int n){
		List c= getMyWires();
		Wire w= null;
        String s= "unconnected";
        String gs= "unconnected";
        String gd= "unconnected";
        String d= "unconnected";
		w= (Wire)c.get(0);
        if(w!=null)s= w.getStringName();
		w= (Wire)c.get(1);
        if(w!=null)gs= w.getStringName();
		w= (Wire)c.get(2);
        if(w!=null)gd= w.getStringName();
		w= (Wire)c.get(3);
        if(w!=null)d= w.getStringName();
		return ("S=" + s + " Gs=" + gs + " Gd=" + gd + " D=" + d);
    } //end of connectionString

} //end of class TransistorTwo
