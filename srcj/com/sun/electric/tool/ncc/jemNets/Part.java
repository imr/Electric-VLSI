/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Part.java
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
//	Updated 16 October 2003 to use JemTree
//  Annotated on 29 January 2004 by Ivan Sutherland

/**
 * Part is an intermediate abstract sub-class of NetObject.
 * sub-classes of Part include Transistor, Resistor, (Capacitor), but
 * NOT Port.  Parts use the "content" List provided by NetObject to
 * hold pointers to the Wires attached to them.  Each class of Part
 * has a fixed number of connections, e.g. TransistorOne has three.
*/

package com.sun.electric.tool.ncc.jemNets;
import com.sun.electric.tool.ncc.basicA.Name;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.trees.NetObject;
import com.sun.electric.tool.ncc.trees.JemCircuit;
import com.sun.electric.tool.ncc.strategy.JemStrat;

import java.util.Collection;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

public abstract class Part extends NetObject{
    // ---------- private data -------------

    /** the count of all Parts ever made */	private static int partCount= 0;
    
    // ---------- private methods ----------

	/** Here is the constructor for Parts
	* @param the Name for this Parts
	* @param an int saying how many terminals this Parts has
	* @return the fresh Parts
	*/
    protected Part(Name n, int numPins){
		super(n, numPins); //sets the name and content size
	//	content= new ArrayList(numPins); //sets the capacity
		for(int i= 0; i<numPins; i++)content.add(null); //clears the array
		partCount++;
        return;
    } //end of constructor

    // ---------- public methods ----------

	/** 
	 * getMyWires is the accessor Method for the Wires array.
	 * @return the List of all Wires touched by this part.
	 */
	public List getMyWires(){return content;}

	/**
	 *  valueString reports the numeric values of this Part,
	 *  e.g. width, length, resistance.
	 * @return a String describing the Part's numeric values.
	 */
	public abstract String valueString();

	/**
	 * The clear method disconnects this part from its Wires
	 */
	public void clear(){content.clear();}

	/** 
	 * Here is the accessor for the part count
	 * @return an int indicating how many parts have ever been made
	 */
	public static int getPartCount(){return partCount;}

	/** 
	 * Here is the accessor for the number of terminals on this Part
	 * @return the number of terminals on this Part, usually a small number.
	 */
	public abstract int size(); //get the number of connections

	/** 
	 * Here is an accessor method for the coefficiant array for this
	 * Part.  The terminal coefficients are used to compute new hash
	 * codes.
	 * @return the array of terminal coefficients for this Part
	 */
	public abstract int[] getTermCoefs(); //the terminal coeficients

	/** 
	 * This method attempts to merge this Part in parallel with another Part
	 * @param the other Part with which to merge
	 * @return true if merge was successful, false otherwise
	 */
	public abstract boolean parallelMerge(Part p);

    /** 
	 * parallelMerge attempts to parallel merge the Parts in
	 * parts. Because there is a possibility of hash code collisions,
	 * I examine all n^2 combinations to guarantee that all possible
	 * parallel merges are performed.
	 * @param j Collection of Parts to merge in parallel
	 * @return the count of Parts actually merged
	 */
    public static int parallelMerge(Collection parts){
		int merged= 0;
		while (parts.size()>=2) {
			Iterator it= parts.iterator();
			Part first= (Part)it.next();
			it.remove();			
			while(it.hasNext()){
				Part p= (Part)it.next();
				if(first.parallelMerge(p)) {
					it.remove();
					merged++;
				} 
			}
		}
		return merged;
    }
		
	/** 
	 * A method to disconnect a Wire from this Part
	 * @param the Wire to disconnect
	 * @return true if the Wire was disconnected, false if not
	 * found on this Part
	 */
	public boolean disconnect(Wire w){
		boolean found= false;
		for(int i= 0;i<size(); i++){
			Wire ww= (Wire)content.get(i);
			if(ww == w){
				content.set(i,null);
				found= true;
			}content.set(i,null);
		} //end of loop
		return found;
    } //end of disconnect
	
	/** 
	 * deleteMe disconnects this Part from its wires and removes it
	 * from its circuit.  The Part is garbage and gone from any
	 * further consideration.
	 */
    public void deleteMe(){
		Iterator it= iterator();
		while(it.hasNext()){
			Wire w= (Wire)it.next();
			if(w!=null)	w.disconnect(this);
		} //end of loop
		JemCircuit parent= (JemCircuit)getParent();
		parent.remove(this);
		return;
    } //end of deleteMe
	
	/** 
	 * cleanMe checks that this Part in standard form and complains if not
	 * @return the number of problems found, 0 if all is well.
	 */
	public int cleanMe(){
        int badCount= 0;
        int n= size();
        for(int i= 0; i<n; i++){
            if(content.get(i)==null){
                getMessenger().error(getStringName() +
                    " has null on terminal " + i);
                badCount++;
            } //end of if
        } //end of for
        return badCount;
    } //end of cleanMe

	/** 
	 * A method to test if a Part touches a Wire.
	 * @param the Wire to test
	 * @return true if this Part touches the Wire, false otherwise
	 */
	public boolean touches(Wire w){
	Iterator it= iterator();
	while(it.hasNext()){
	    Wire x= (Wire)it.next();
	    if(x == w) return true;
	} //end of while
        return false;
    } //end of holds

	/** 
	 * A method to test if this Part touches a Wire with a gate connection.
	 * Transistors that have gates must provide touchesAtGate.
	 * @param the Wire to test
	 * @return true if a gate terminal of this Part touches the Wire
	 */
    public abstract boolean touchesAtGate(Wire w);

/*	public int compareOnWire(Part p, Wire w){
		int hisNum= p.touchesAtGate(w)? 1 : 0;
		int myNum= touchesAtGate(w)? 1 : 0;
		return myNum - hisNum;
	} //end of compareOnWire
*/
	/** 
	 * isThisGate returns true if the terminal number is a gate.
	 * @param the terminal number to test
	 * @return true if the terminal is a gate, false otherwise
	 */
	protected abstract boolean isThisGate(int x);

	/** 
	 * A method to compute the hash code for this NetObject.
	 * @param type: 0 by part name, 1 omitting gate connections
	 * 2 by gate connections only, 3 using all connections.
	 * @return an Integer code for distinguishing the objects.
	 */
	public Integer computeCode(int type){
		if(type == 0) return computeNameCode();
		if(type == 1) return computeWithoutGate();
		if(type == 2) return computeGateOnly();
		if(type == 3) return computeHashCode();
		return null;
	} //end of computeCode

    //compute the name code for this object
    //as the sum of the Object hashCode()'s of its wires
    private Integer computeNameCode(){
        int sum= 0;
		int hash= 0;
		int codes[]= getTermCoefs();
		for(int i=0; i<size(); i++){
			int code= 0;
			Wire w= (Wire)content.get(i);
			if(w!=null)hash= w.hashCode(); //use the Object function!
			sum= sum+hash*codes[i];
        } //end of loop
        return new Integer(sum);
    } //end of computeNameCode

	private Integer computeWithoutGate(){
        int sum= 0;
		int hash= 0;
        int codes[]= getTermCoefs();
		for(int i=0; i<size(); i++){
			if(isThisGate(i))continue;
			hash= codes[i];
			Wire w= (Wire)content.get(i);
			if(w!=null)hash= w.getCode();
            sum= sum+hash*codes[i];
        } //end of loop
        return new Integer(sum);
    } //end of computeWithoutGate

	private Integer computeGateOnly(){
        int sum= 0;
        int codes[]= getTermCoefs();
		for(int i=0; i<size(); i++){
			if( ! isThisGate(i))continue;
			int hash= codes[i];
			Wire w= (Wire)content.get(i);
			if(w!=null)hash= w.getCode();
            sum= sum+hash*codes[i];
        } //end of loop
        return new Integer(sum);
    } //end of computeHashCode

	protected Integer computeHashCode(){
        int sum= 0;
        int codes[]= getTermCoefs();
		for(int i=0; i<size(); i++){
			int hash= codes[i];
			Wire w= (Wire)content.get(i);
			if(w!=null)hash= w.getCode();
            sum= sum+hash*codes[i];
        } //end of loop
        return new Integer(sum);
    } //end of computeHashCode

	/** 
	 * The Part must compute a hash code contribution for a Wire to
	 * use.  because the Wire doesn't know how it's connected to this
	 * Part and multiple connections are allowed.
	 * @param the Wire for which a hash code is needed
	 * @return an int with the code contribution.
	 */
    public int getHashFor(Wire w){
        int sum= 0;
        int codes[]= getTermCoefs();
		for(int i=0; i<size(); i++){
			Wire x= (Wire)content.get(i);
			if(x != null && x == w)sum= sum+(codes[i] * getCode());
        } //end of loop
        return sum;
    } //end of getHash
	
	/** 
	 * check that this Part is in proper form
	 * complain if it's wrong
	 * @param the messenger to use for a report
	 * @return true if all OK, false if there's a problem
	 */
	public boolean checkMe(){
	boolean good= true;
	for(int i=0; i<size(); i++){
	    Wire w= (Wire)content.get(i);
	    if(w==null){
			getMessenger().error(getStringName() +
								 " has null on terminal " + i);
			good= false;
		}else{
			if(w.touches(this)==false){
				getMessenger().error("Wire " + w.getStringName() +
									 " not connected to Part " + getStringName()
									 + " at terminal " + i);
				good= false;
			} //end of if
		} //end of else
	} //end of for
	return good;
    } //end of checkMe
	
	/** 
	 * printMe prints out this NetObject
	 * @param an integer length limit to the printout
	 */
	public void printMe(int maxCon){
		String n= nameString();
		String s= valueString();
		String c= connectionString(maxCon);
		getMessenger().line(n + " " + s + " " + c);
		return;
	} //end of printMe
	
} //end of Part

