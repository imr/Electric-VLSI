/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NetObject.java
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
//	Updated 10 October 2003
//revised for jemTree interface 16 October 03

package com.sun.electric.tool.ncc.trees;
import com.sun.electric.tool.ncc.basicA.Name;
import com.sun.electric.tool.ncc.basicA.Messenger;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**  
 * NetObject is the parent class for Parts and Wires.
 * There is only one class of Wire, but many sub-classes of Part
 * including: Transistor, Resistor, and Port.
 * The hash code calculations are in Wire and Part.
 * Wires can connect only to Parts and vice versa.
 */
public abstract class NetObject implements JemChild{

    // ---------- private data -------------
    private Name myName= null;
    private JemCircuit myParent= null;  // the parent pointer
    protected List content;
    private static Messenger myMessenger;
	
    /** 
	 * Get an identifying String for this NewObject.
	 * @return an identifying String.
	 */
    public abstract String nameString();	//type and name

    /** 
	 * Get a String listing the connections for this NetObject.
	 * @param the maximum number of connections to list
	 * @return a String of connections.
	 */
    public abstract String connectionString(int n);

    /** 
	 * Put this NetObject into standard form, complain if errors found.
	 * @return the number of errors found: 0 if good.
	 */
    public abstract int cleanMe();

    /** 
	 * Compute a separation code for this NetObject.
	 * @param type: 0 by part name, 1 omitting gate connections
	 * 2 by gate connections only, 3 using all connections.
	 * @return an Integer code for distinguishing the objects.
	 */
    public abstract Integer computeCode(int type);

	public boolean checkParent(JemParent p){
		if(p instanceof JemCircuit)return true;
		else {
			getMessenger().error("wrong class parent in " + nameString());
			return false;
		} //end of if
	} //end of checkParent

    // ---------- protected methods ----------

    //the constructor - if s==null gives next sequential name
    protected NetObject(Name n, int i){
		myMessenger= Messenger.toTestPlease("NetObject");
		myName= n; //gets the Name
        myParent= null;
        content= new ArrayList(i);
        return;
    } //end of constructor

    protected static Messenger getMessenger(){return myMessenger;}

    /** 
	 * Fix this object to be in standard form.
	 * @return true if all OK, false otherwise
	 */
    public abstract boolean checkMe();
	
    // ---------- public methods ----------

    public Name getTheName(){return myName;} //name alone
    public String getStringName(){return myName.toString();} //name alone

    /** 
	 * getCode returns an integer hash code for this NetObject.
	 * @return the integer hash code from this NetObjec's JemEquivRecord.
	 */
    public int getCode(){return myParent.getCode();} //get my group code

    /** 
	 * getParent fetches the next JemTree towards the root.
	 * @return the JemTree parent of this instance, if any.
	 */
    public JemParent getParent(){return myParent;}

	/** 
	 * setParent checks the proposed parent's class before writing it.
	 * @param x the JemParent proposed
	 * @return true if parent was accepted, false otherwise
	 */
	public boolean setParent(JemParent x){
		if(checkParent(x)){
			myParent= (JemCircuit)x;
			return true;
		} else {
			getMessenger().error("wrong class parent in " + nameString());
			return false;
		} //end of else
	}

    /** 
	 * toString returns the name and connections of this NetObject as
	 * a String.
	 * @return a String with name and connections
	 */
    public String toString(){
        return (nameString() + ": " + connectionString(6));
    }

    public abstract void printMe(int i); //i is the size limit

    public Iterator iterator(){return content.iterator();}

}

