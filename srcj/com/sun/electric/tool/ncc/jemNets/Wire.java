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
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.basicA.Name;
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
    Set content = new HashSet(); 
	
	//portMap maps each Wire to a List of Ports
	private static Map portMap= new HashMap(20);

	//associates Names with wires
    private static Hashtable wireHashtable= new Hashtable();
    private static int wireCount= 0;

    // ---------- private methods ----------
    private Wire(Name n){
        super(n); //sets the name and content size
        wireCount++;
    }

    // ---------- public methods ----------

	public Iterator getParts() {return content.iterator();}
	public Iterator getConnected() {return getParts();}

    /** 
	 * disconnect the indicated Part from this Wire
	 * @param the Part to remove
	 * @return true if it was properly removed.
	 */
    public boolean disconnect(Part p){return content.remove(p);}

    /** 
	 * add the indicated Part or Port to this Wire
	 * @param the NetObject to add
	 * complain if the NetObject to add isn't a Part or a Port
	 * @return true if properly added, false otherwise
	 */
    public void add(NetObject p){
    	error(p==null, "Wires can't add null Objects");
        if(p instanceof Part) {
            error(!content.add(p), "add failed?");
		} else {
			error(!(p instanceof Port), "Wires can add only Parts or Ports"); 
			//portMap contains the pointers from Wire to Port
			List ports = (List)portMap.get(this);
			if(ports==null){
				ports = new ArrayList();
				portMap.put(this, ports);
			}
			error(!ports.add(p), "add failed?");
        }
    }
	
	//return the list of Ports on this Wire
	public List getPortList(){
		List out= (List)portMap.get(this);
		return out!=null ? out : new ArrayList();
	}
	
	
    /** 
	 * Factory - return a Wire with the given Name
	 * @param the Name to give the wire
	 * @return the Wire with that name
	 */
    public static Wire please(Name n){
        if(wireHashtable.containsKey(n)){
            //wire is known
            return (Wire)wireHashtable.get(n);
        }
        //no known wire by this name
        Wire w= new Wire(n); //make one
        wireHashtable.put(w.getTheName(),w);
        return w;
    }

    /**
	 * Factory - return a Wire with the given String as its name
	 * adopted by the JemCircuit cc
	 * @param the JemCircuit to contain this Wire
	 * @param the String name to give the wire
	 * @return the Wire with that name
	 */
    public static Wire please(JemCircuit cc, Name nn){
        Wire w=  Wire.please(nn);
        if(w.getParent()==null)  cc.adopt(w);
        return w;
    }
    
    public Type getNetObjType() {return Type.WIRE;}

    /**
	 * @return the number of Wires that ever were constructed
	 */
    public static int getWireCount (){return wireCount;}
    
    /**
     * remove Wire from its parent's list. This Wire must not be connected to 
     * any Parts. 
     *
     */
    public void killMe() {
    	error(content.size()!=0, "Wire still connected to Parts");
    	getParent().remove(this);
    }

	/**
	 * @return the number of Parts with Gates attached
	 */
	public int numPartsWithGateAttached(){
		int with = 0;
		for (Iterator it=content.iterator(); it.hasNext();) {
			Part p= (Part)it.next();
			if(p.touchesAtGate(this)) with++;
		}
		return with;
	}

	//calculates #gates - #diffusions
	public int stepUp(){
		int gates= 0;
		int diffusion= 0;
		for (Iterator it=content.iterator(); it.hasNext();) {
			Object oo= it.next();
			if(oo instanceof Transistor){
				Transistor t= (Transistor)oo;
				if(t.touchesAtGate(this))gates++;
				if(t.touchesAtDiffusion(this))diffusion++;
			}
		}
		return gates - diffusion;
	}
	
    /** 
	 * check that this Wire is properly structured.  check each
	 * connection to see if it points back
	 * @param the Messenger to report errors
	 * @return true if all was OK, false if problems
	 */
    public void checkMe(JemCircuit parent){
    	error(getParent()!=parent, "wrong parent");
        for (Iterator it=getParts(); it.hasNext();) {
            NetObject nn=(NetObject)it.next();
            error(!(nn instanceof Part), "expecting only parts");
            Part pp=(Part)nn;
            error(!pp.touches(this), "Part not connected back to wire"); 
        }
    }

    /** 
	 * Does this Wire touch the given Part?
	 * @param the Part to test
	 * @return true if it touches, false if not
	 */
    public boolean touches(Part p){return content.contains(p);}
	
	public boolean touches(Port pp){
		if( ! portMap.containsKey(this))return false;
		List pl= (List)portMap.get(this);
		
		for (Iterator it=pl.iterator(); it.hasNext();) {
			Port xx= (Port)it.next();
			if(pp == xx)return true;
		}
		return false;
	}
	
    /** 
	 * Compute a separation code for this Wire.
	 * @param type: 0 by part name, 1 omitting gate connections
	 * 2 by gate connections only, 3 using all connections.
	 * @return an Integer code for distinguishing the objects.
	 */
//    public Integer computeCode(int type){
//        if(type == 0) return computeNameCode();
//        if(type == 1) return computeWithoutGate();
//        if(type == 2) return computeGateOnly();
//        if(type == 3) return computeHashCode();
//        return null;
//    }

    /**
	 * compute the name code for this object
     * as the sum of the Object hashCode()'s of its wires
	 */
//    private Integer computeNameCode(){
//        int sum= 0;
//        for (Iterator it=iterator(); it.hasNext();) {
//            Part pp= (Part)it.next();
//            sum += pp.hashCode();
//        }
//        return new Integer(sum);
//    }

    /** Gate touches are supposed to come last in the list of Parts */
//    private Integer computeWithoutGate(){
//        int sum= 0;
//        int hash= 0;
//        for (Iterator it=iterator(); it.hasNext();) {
//            Object oo= it.next();
//            if(oo == null)continue;
//            Part pp= (Part)oo;
//            if(pp.touchesAtGate(this)) return new Integer(sum);
//            hash= pp.getHashFor(this);
//            sum= sum+hash;
//        }
//        return new Integer(sum);
//    }

//    private Integer computeGateOnly(){
//        int sum= 0;
//        int hash= 0;
//        Iterator it= iterator();
//        while(it.hasNext()){
//            Object oo= it.next();
//            if(oo == null)continue;
//            Part pp= (Part)oo;
//            if( ! pp.touchesAtGate(this))continue;
//            hash= pp.getHashFor(this);
//            sum= sum+hash;
//        } //end of while
//        return new Integer(sum);
//    } //end of computeGateOnly

    public Integer computeHashCode(){
        int sum= 0;
        for (Iterator it=getParts(); it.hasNext();) {
            Part pp= (Part) it.next();
            sum += pp.getHashFor(this);
        }
        return new Integer(sum);
    }

    /** 
	 * report the number of Parts connected to this wire.
	 * @return an int with the number of connections
	 */
    public int numParts(){return content.size();}

    /** 
	 * Get an identifying String for this NewObject.
	 * @return an identifying String.
	 */
    public String nameString(){
        return ("Wire " + getStringName());
    }

    /** 
	 * Get a String indicating up to N connections for this NetObject.
	 * @param the maximum number of connections to list
	 * @return a String of connections.
	 */
    public String connectionString(int n){
        if (content.size()==0)return ("is unconnected");
        if (content.size()>n)return ("has " + content.size() + " pins");
        Iterator it= getParts();
        String s= "";
        while(it.hasNext()){
            Part pp= (Part)it.next();
            String cc= pp.getStringName();
            s= s + " " + cc;
        }
        return s;
    }

    public void printMe(int x){
        int maxPins= 3;
        Messenger.say(nameString());
        String s= "";
        if(numParts()>maxPins){
            int count= 0;
            s= " has " + numParts() + " pins starting:";
            Iterator it= getParts();
            while(it.hasNext()&&(count<maxPins)){
                Part pp= (Part)it.next();
                String cc= pp.getStringName();
                s= s + " " + cc;
                count++;
            }
        }else{
            s= connectionString(maxPins);
        }
        Messenger.line(s);
    }

}
