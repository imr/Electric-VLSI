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

package com.sun.electric.tool.ncc.jemNets;
import java.util.Iterator;

import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.trees.Circuit;

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
    private NccNameProxy myName;	// null means name is empty string
    private Circuit myParent;
    private static final int MAX_CONN = 100;
	
    /** Distinguish Parts, Wires, and Ports.
     * @return PART or WIRE or PORT */
    public abstract Type getNetObjType();


    public abstract Iterator getConnected();

    // ---------- protected methods ----------
	/** @param name NameProxy that can be called to obtain the instance name.
	 * null means the name is the empty string. */
    protected NetObject(NccNameProxy name){myName=name;}

    protected static void error(boolean pred, String msg) {
    	LayoutLib.error(pred, msg);
    }

    /** Make sure this object is OK. */
    public abstract void checkMe(Circuit parent);
	
    // ---------- public methods ----------

    public String getName(){
    	return myName!=null ? myName.toString() : "";
    }
    public NccNameProxy getNameProxy() {return myName;}

    /** Return an integer hash code for this NetObject.
	 * @return the integer hash code from this NetObjec's EquivRecord. */
    public int getCode(){return myParent.getCode();} //get my group code

    /**	 @return the Circuit containing this NetObject */
    public Circuit getParent(){return myParent;}

	public void setParent(Circuit x){myParent=x;}

	public abstract boolean isDeleted(); 

    public abstract String nameString();
    public abstract String valueString();
    public abstract String connectionString(int maxConn);

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(nameString());
		String v = valueString();
		if (!v.equals("")) sb.append(" "+v);
		String c = connectionString(MAX_CONN);
		if (!c.equals(" ")) sb.append(" "+c);
		return sb.toString();
	}
}

