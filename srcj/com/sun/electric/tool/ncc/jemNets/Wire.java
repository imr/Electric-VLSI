/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Wire.java
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
package com.sun.electric.tool.ncc.jemNets;
import com.sun.electric.tool.ncc.basicA.Name;
//import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.trees.NetObject;
//import com.sun.electric.tool.ncc.trees.JemTreeGroup;
import com.sun.electric.tool.ncc.jemNets.Transistor;
import com.sun.electric.tool.ncc.trees.JemCircuit;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Hashtable;

public class Wire extends NetObject{
    // ---------- private data -------------
    //    private List myParts;
	
	//portMap maps each Wire to a List of Ports
	private static Map portMap= new HashMap(20);

	//associates Names with wires
    private static Hashtable wireHashtable= new Hashtable();
    private static int wireCount= 0;

    // ---------- private methods ----------
    private Wire(Name n){
        super(n, 2); //sets the name and content size
      //  content= new ArrayList(3);
        wireCount++;
    } //end of constructor

    // ---------- public methods ----------

    /** 
	 * disconnect the indicated Part from this Wire
	 * @param the Part to remove
	 * @return true if it was properly removed.
	 */
    public boolean disconnect(Part p){return content.remove(p);}

    /** 
	 * add the indicated Part to the Collection on this Wire
	 * @param the NetObject to add
	 * complain if the NetObject to add isn't a Part or a Port
	 * @return true if properly added, false otherwise
	 */
    public boolean add(NetObject p){
        if(p == null){
            getMessenger().error("Wires can't add null Objects");
            return false;
        } else if(p instanceof Part) {
            return content.add(p);
		} else if(p instanceof Port){
			//portMap contains the pointers from Wire to Port
			List ports;
			if(portMap.containsKey(this)){
				ports= (List)portMap.get(this);
			} else {
				ports= new ArrayList(1);
				portMap.put(this, ports);
			} //end of else
			ports.add(p);
			return true;
        } else {
            getMessenger().error("Wires can add only Parts or Ports");
            return false;
        }//end of else
    } //end of add
	
	//return the list of Ports on this Wire
	public List getPortList(){
		if( ! portMap.containsKey(this))return new ArrayList(1);
		List out= (List)portMap.get(this);
		return out;
	} //end of getPortList
	
	
    /** 
	 * Factory - return a Wire with the given Name
	 * @param the Name to give the wire
	 * @return the Wire with that name
	 */
    public static Wire please(Name n){
        if(wireHashtable.containsKey(n)){
            //wire is known
            return (Wire)wireHashtable.get(n);
        } //end of if
          //no known wire by this name
        Wire w= new Wire(n); //make one
        wireHashtable.put(w.getTheName(),w);
        return w;
    } //end of please

    /**
	 * Factory - return a Wire with the given String as its name
	 * adopted by the JemCircuit cc
	 * @param the JemCircuit to contain this Wire
	 * @param the String name to give the wire
	 * @return the Wire with that name
	 */
    public static Wire please(JemCircuit cc, Name nn){
        Wire w=  Wire.please(nn);
        if(w.getParent()!=null)return w;
        cc.adopt(w);
        return w;
    } //end of please

    /**
	 * @return the number of Wires that ever were constructed
	 */
    public static int getWireCount (){return wireCount;}

    /** 
	 * Put this Wire into standard form, removing duplicate references to Parts.
	 * @return the number of references removed: 0 if none.
	 */
    public int cleanMe(){
        int a= size();
        if(a<2)return 0;
        Set s= new HashSet();
        s.addAll(content);
        content.clear();
        content.addAll(s);
        int b= size();
        int diff= a-b;
        if(diff == 0) return diff; //no change
        getMessenger().line("Removed " + diff +
                            " duplicate connections on " + nameString());
        return diff;
    } //end of cleanMe

	public int sortMe(){
		Set with= new HashSet();
		Set without= new HashSet();
		Iterator it= content.iterator();
		while(it.hasNext()){
			Part p= (Part)it.next();
			if(p.touchesAtGate(this))with.add(p);
			else without.add(p);
		} //end of while
		content.clear();
		content.addAll(with);
		content.addAll(without);
		return with.size();
	} //end of sortMe

	//this computes the number of non port connections on the wire
	public int popularity(){
		Iterator it= content.iterator();
		int ports= 0;
		while(it.hasNext()){
			Object oo= it.next();
			if(oo instanceof Port)ports++;
			else break; //first non-port is past the ports
		} //end of while
		int out= size()-ports;
		return out;
	} // end of popularity
	
	//calculates N gate - N diffusion
	public int stepUp(){
		Iterator it= content.iterator();
		int gates= 0;
		int diffusion= 0;
		while(it.hasNext()){
			Object oo= it.next();
			if(oo instanceof Transistor){
				Transistor t= (Transistor)oo;
				if(t.touchesAtGate(this))gates++;
				if(t.touchesAtDiffusion(this))diffusion++;
			} //end of while
		} //end of while
		int out= gates - diffusion;
		return out;
	} // end of stepUp
	
    /** 
	 * check that this Wire is properly structured.  check each
	 * connection to see if it points back
	 * @param the Messenger to report errors
	 * @return true if all was OK, false if problems
	 */
    public boolean checkMe(){
        boolean good= true;
        Iterator it= iterator();
        while(it.hasNext()){
            NetObject nn=(NetObject)it.next();
            if(nn instanceof Part){
                //its a Part
                Part pp=(Part)nn;
                if(pp.touches(this)==false){
                    //its not connected back
                    getMessenger().error("Part " + pp.getStringName() +
                                         " not connected to Wire " +
										 getStringName());
                    good= false;
                } //end of if
            }else{
                //its not a Part
                getMessenger().error("Wire " + getStringName() +
                                     "connected to Wire " +
									 nn.getStringName());
                good= false;
            } //end of if
        } //end of loop
        return good;
    } //end of checkMe

    /** 
	 * Does this Wire touch the given Part?
	 * @param the Part to test
	 * @return true if it touches, false if not
	 */
    public boolean touches(Part p){return content.contains(p);}
	
	public boolean touches(Port pp){
		if( ! portMap.containsKey(this))return false;
		List pl= (List)portMap.get(this);
		Iterator it= pl.iterator();
		while(it.hasNext()){
			Port xx= (Port)it.next();
			if(pp == xx)return true;
		} //end of loop
		return false;
	}// end of touches(Port)
	
    //returns true if this wire touches
    //two N-type or two P-type gates.
    public boolean hasTwoGates(){
        int pgates= 0;
        int ngates= 0;
        Iterator it= iterator();
        while(it.hasNext()){
            Part p= (Part)it.next();
            if( ! p.touchesAtGate(this))
                continue;
            Transistor t= (Transistor)p;
            if(t.isNtype())ngates++;
            if(t.isPtype())pgates++;
            if((ngates >1)||(pgates>1))
                return true;
        } //end of while
        return false;
    } //end of hasTwoGates

    /** 
	 * Compute a separation code for this Wire.
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

    /**
	 * compute the name code for this object
     * as the sum of the Object hashCode()'s of its wires
	 */
    private Integer computeNameCode(){
        int sum= 0;
        Iterator it= iterator();
        while(it.hasNext()){
            Part pp= (Part)it.next();
            if(pp != null){
                int	hash= pp.hashCode();
                sum= sum + hash;
            } //end of if
        } //end of while
        return new Integer(sum);
    } //end of computeNameCode

    /** Gate touches are supposed to come last in the list of Parts */
    private Integer computeWithoutGate(){
        int sum= 0;
        int hash= 0;
        Iterator it= iterator();
        while(it.hasNext()){
            Object oo= it.next();
            if(oo == null)continue;
            Part pp= (Part)oo;
            if(pp.touchesAtGate(this)) return new Integer(sum);
            hash= pp.getHashFor(this);
            sum= sum+hash;
        } //end of while
        return new Integer(sum);
    } //end of computeWithoutGate

    private Integer computeGateOnly(){
        int sum= 0;
        int hash= 0;
        Iterator it= iterator();
        while(it.hasNext()){
            Object oo= it.next();
            if(oo == null)continue;
            Part pp= (Part)oo;
            if( ! pp.touchesAtGate(this))continue;
            hash= pp.getHashFor(this);
            sum= sum+hash;
        } //end of while
        return new Integer(sum);
    } //end of computeGateOnly

    private Integer computeHashCode(){
        int sum= 0;
        int hash= 0;
        Iterator it= iterator();
        while(it.hasNext()){
            Object oo= it.next();
            if(oo == null)continue;
            Part pp= (Part)oo;
            hash= pp.getHashFor(this);
            sum= sum+hash;
        } //end of while
        return new Integer(sum);
    } //end of computeHashCode

    /** 
	 * report the number of connections on this wire.
	 * @return an int with the number of connections
	 */
    public int size(){return content.size();}

    /** 
	 * Get an identifying String for this NewObject.
	 * @return an identifying String.
	 */
    public String nameString(){
        return ("Wire " + getStringName());
    } //end of nameString

    /** 
	 * Get a String indicating up to N connections for this NetObject.
	 * @param the maximum number of connections to list
	 * @return a String of connections.
	 */
    public String connectionString(int n){
        if (content.size()==0)return ("is unconnected");
        if (content.size()>n)return ("has " + content.size() + " pins");
        Iterator it= iterator();
        String s= "";
        while(it.hasNext()){
            Part pp= (Part)it.next();
            String cc= pp.getStringName();
            s= s + " " + cc;
        } //end of loop
        return s;
    } //end of connectionString

    public void printMe(int x){
        int maxPins= 3;
        getMessenger().say(nameString());
        String s= "";
        if(size()>maxPins){
            int count= 0;
            s= " has " + size() + " pins starting:";
            Iterator it= iterator();
            while(it.hasNext()&&(count<maxPins)){
                Part pp= (Part)it.next();
                String cc= pp.getStringName();
                s= s + " " + cc;
                count++;
            } //end of while
        }else{
            s= connectionString(maxPins);
        } //end of else
        getMessenger().line(s);
        return;
    } //end of doFor

} //end of class Wire
