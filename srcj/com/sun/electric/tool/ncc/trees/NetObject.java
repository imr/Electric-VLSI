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
 * NetObject is the parent class for Parts, Wires, and Ports.  There
 * is only one class of Wire, but many sub-classes of Part including:
 * Transistor, Resistor.  The hash code calculations are in Wire and
 * Part.  Wires can connect only to Parts and vice versa.
 */
public abstract class NetObject {
	public static class Type {
		int ordinal;
		private Type(int ord) {ordinal=ord;}
		public int ordinal() {return ordinal;}
		
		public static final Type PART = new Type(0);
		public static final Type WIRE = new Type(1);
		public static final Type PORT = new Type(2);
	}

    // ---------- private data -------------
    private Name myName;
    private JemCircuit myParent;
	
    /** 
	 * Get an identifying String for this NewObject.
	 * @return an identifying String.
	 */
    public abstract String nameString();	//type and name
    
    /**
     * Distinguish Parts, Wires, and Ports.
     * @return
     */
    public abstract Type getNetObjType();

    /** 
	 * Get a String listing the connections for this NetObject.
	 * @param the maximum number of connections to list
	 * @return a String of connections.
	 */
    public abstract String connectionString(int n);

    public abstract Iterator getConnected();

    // ---------- protected methods ----------

    protected NetObject(Name n){myName= n;}

    public static void error(boolean pred, String msg) {
    	if (pred) Messenger.error(msg);
    }

    /** 
	 * Make sure this object is OK.
	 */
    public abstract void checkMe(JemCircuit parent);
	
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
    public JemCircuit getParent(){return myParent;}

	public void setParent(JemCircuit x){myParent=x;}

    /** 
	 * toString returns the name and connections of this NetObject as
	 * a String.
	 * @return a String with name and connections
	 */
    public String toString(){
        return (nameString() + ": " + connectionString(6));
    }

    public abstract void printMe(int i); //i is the size limit

}

