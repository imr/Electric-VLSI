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
import com.sun.electric.tool.ncc.trees.NetObject;
//import com.sun.electric.tool.ncc.trees.JemTreeGroup;
import com.sun.electric.tool.ncc.jemNets.Transistor;
import com.sun.electric.tool.ncc.trees.JemCircuit;
import com.sun.electric.tool.ncc.NccGlobals;

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
    private Set content = new HashSet();
	private Port port;  	  // usually null because most Wires have no Port
	private boolean isGlobal; // in Electric this is a Global wire 	

    // ---------- private methods ----------

    // ---------- public methods ----------

	public Wire(String name, boolean isGlobal){
		super(name);
		this.isGlobal = isGlobal;
	}
	public Iterator getParts() {return content.iterator();}
	public Iterator getConnected() {return getParts();}
	public boolean isGlobal() {return isGlobal;}

    /** 
	 * disconnect the indicated Part from this Wire
	 * @param p the Part to remove
	 * @return true if it was properly removed.
	 */
    public boolean disconnect(Part p){return content.remove(p);}

    /** 
	 * add a Part to this Wire
	 * @param p the Part to add
	 */
    public void add(Part p){
    	error(p==null, "Wires can't add null Part");
		content.add(p);
    }
    
	/** 
	 * add a Port to this Wire
	 * @param p the Port to add
	 */
    public Port addExportName(String portName) {
		if (port==null)  port = new Port(portName, this);
		else port.addExportName(portName); 
		return port;
    }
	
	//return the Ports on this Wire
	public Port getPort() {return port;}
	
    public Type getNetObjType() {return Type.WIRE;}

    /**
     * remove Wire from its parent's list. This Wire must not be connected to 
     * any Parts. 
     */
    public void killMe() {
    	error(content.size()!=0, "Can't kill: wire connected to a Part");
    	error(port!=null, "Can't kill: wire connected to a Port");
    	error(isGlobal(), "Can't kill: wire declared GLOBAL in Electric");
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
	 * @param parent the wire's parent
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
	 * @param p the Part to test
	 * @return true if it touches, false if not
	 */
    public boolean touches(Part p){return content.contains(p);}
    public boolean touches(Port p) {return port==p;}
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
        return ("Wire " + getName());
    }

    /** 
	 * Get a String indicating up to N connections for this NetObject.
	 * @param n the maximum number of connections to list
	 * @return a String of connections.
	 */
    public String connectionString(int n){
        if (content.size()==0)return ("is unconnected");
        if (content.size()>n)return ("has " + content.size() + " pins");
        Iterator it= getParts();
        String s= "";
        while(it.hasNext()){
            Part pp= (Part)it.next();
            String cc= pp.getName();
            s= s + " " + cc;
        }
        return s;
    }

    public void printMe(int x, Messenger messenger){
        int maxPins= 3;
        messenger.print(nameString());
        String s= "";
        if(numParts()>maxPins){
            int count= 0;
            s= " has " + numParts() + " pins starting:";
            Iterator it= getParts();
            while(it.hasNext()&&(count<maxPins)){
                Part pp= (Part)it.next();
                String cc= pp.getName();
                s= s + " " + cc;
                count++;
            }
        }else{
            s= connectionString(maxPins);
        }
        messenger.println(s);
    }

}
