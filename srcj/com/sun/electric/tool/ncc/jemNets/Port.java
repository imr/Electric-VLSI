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


public class Port extends NetObject implements Comparable{
    // ---------- private data -------------
    private static int numCon= 1;
    private static int termCoefs[] = {47};
    private static int portCount= 0; //the count of all Ports made
    
    //	private Name myOwnName;
    
    //override the methods in NetObject
    
    //  public String getStringName(){return getTheName().toString();} //name alone
    
    // ---------- private methods ----------
    protected Port(Name n){
	super(n, numCon);
	for(int i= 0; i<numCon; i++)content.add((Object)null); //clears the array
	portCount++;
    } //end of constructor
    
    // ---------- public methods ----------
    
    public static Port please(Name n){
        Port p= new Port(n);
        return p;
    } //end of please

    public static Port please(JemCircuit cc, Name n){
        Port p= new Port(n);
	 	cc.adopt(p);
        return p;
    } //end of please

	public boolean isThisGate(int x){return false;}
	
	public int compareTo(Object x){
		if(x instanceof Port){
			Port p= (Port)x;
			return getTheName().compareTo(p.getTheName());
		} //end of if
		return 0;
	} //end of compareTo
	/*	

    public static int parallelMerge(Collection j){
		int s= j.size();
		if(s<2)return 0; //too small
		List x= new ArrayList(j);
		Collections.sort(x);
		Iterator it= x.iterator();
		Part a= (Part)it.next(); //the first one
		int merged= 0;
		while(it.hasNext()){
			Part p= (Part)it.next();
			//getMessenger().line("merge #" + merged +
			//		   " is " + a.getStringName() + " to " + p.getStringName());
			if(a.parallelMerge(p)){
				merged++;
			} //end of if
		} //end of loop
		return merged;
    } //end of parallelMerge

    //merge with this Port, this one survives 
    public boolean parallelMerge(Part p){
		if(p == null)return false;
        if(p == this)return false;
        if( ! (p instanceof Port)) return false;
        //its the same class but a different one
        Port pp= (Port)p;
		Wire ww= pp.getMyWire();
		if(ww != getMyWire())return false;
		JemCircuit cc= (JemCircuit)pp.getParent();
		if(cc != getParent())return false;
		Name x= getTheName().parallelMerge(pp.getTheName());
		myOwnName= x;
		pp.deleteMe();
        return true;
    } //end of parallelMerge
*/
    // ---------- abstract commitment ----------
    public int size(){return 1;}
    public int getNumCon(){return numCon;}
    public int[] getTermCoefs(){return termCoefs;} //the terminal coeficients

    public void connect(Wire w){
        content.set(0, w); //Port points to Wire
		w.add(this);
		return;
    } //end of connect

	public Wire getMyWire(){return (Wire)content.get(0);}

	/** 
	 * Compute a hash code for Wire w to use.  special because a Ports
	 * must not have an impact on a Wire unless retired
	 * @param the Wire for which hash code is needed
	 * @return the hash code for the wire to use
	 */
    public int getHashFor(Wire w){
		JemCircuit circuit= (JemCircuit)getParent();
		JemEquivRecord g= (JemEquivRecord)circuit.getParent();
		if(g.canRetire())return getCode();
		else return 0;
	} //end of getHashFor
	
	public Integer computeCode(int i){
		Wire w= getMyWire();
		int ii= getHashFor(w);
		return new Integer(ii);
	} //end of computeCode
	
    public String nameString(){
        return ("Port " + getStringName());
    } //end of nameString

    public String connectionString(int n){
        String s= "is unconnected";
        Wire w= (Wire)content.get(0);
        if(w !=null)s= w.getStringName();
        return ("is on " + s);
    } //end of connectionString

	public boolean checkMe(){
		String s= null;
		if(content.size() == 0){
			s= " is unconnected";
			getMessenger().line(nameString() + s);
			return false;
		} //end of if
		Wire w=	getMyWire();
		if(w == null){
			s= " has null connection";
			getMessenger().line(nameString() + s);
			return false;
		} //end of if
		if(w.touches(this)){
			return true;
		} else {
			s= " has inconsitant connection to ";
			getMessenger().line(nameString() + s + w.nameString());
			return false;
		} //end of else

	} //end of checkMe

	public int cleanMe(){
		if(checkMe())return 0;
		else return 1;
	}// end of cleanMe
	
	public void printMe(int maxCon){
		String n= nameString();
		String c= connectionString(maxCon);
		getMessenger().line(n + " " + c);
		return;
	} //end of printMe
	
} //end of Port

