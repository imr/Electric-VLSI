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

package com.sun.electric.tool.ncc.netlist;
import java.util.Iterator;

import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.result.NetObjReport.NetObjReportable;
import com.sun.electric.tool.ncc.trees.Circuit;

/**  
 * NetObject is the parent class for Parts, Wires, and Ports.  There
 * is only one class of Wire, but many sub-classes of Part including:
 * Transistor, Resistor.  The hash code calculations are in Wire and
 * Part.  Wires can connect only to Parts and vice versa.
 */
public abstract class NetObject implements NetObjReportable {
	public static class Type {
		int ordinal;
		private Type(int ord) {ordinal=ord;}
		public int ordinal() {return ordinal;}
		
		public static final Type PART = new Type(0);
		public static final Type WIRE = new Type(1);
		public static final Type PORT = new Type(2);
	}

    // ---------- private data -------------
    private Circuit myParent;
    private static final int MAX_CONN = 100;
	
    /** Distinguish Parts, Wires, and Ports.
     * @return PART or WIRE or PORT */
    public abstract Type getNetObjType();

    public abstract Iterator<NetObject> getConnected();

    // ---------- protected methods ----------
    protected static void error(boolean pred, String msg) {
    	LayoutLib.error(pred, msg);
    }

    /** Make sure this object is OK. */
    public abstract void checkMe(Circuit parent);
	
    // ---------- public methods ----------


    /** Return an integer hash code for this NetObject.
	 * @return the integer hash code from this NetObjec's EquivRecord. */
    public int getCode(){return myParent.getCode();} //get my group code

    /**	 @return the Circuit containing this NetObject */
    public Circuit getParent(){return myParent;}

	public void setParent(Circuit x){myParent=x;}

	public abstract boolean isDeleted(); 
	/** instance name qualified by path prefix */
    public abstract String getName();
	/** human readable identification of instance */
	public abstract String instanceDescription();
	/** human readable enumeration of sizes and other values */
    public abstract String valueDescription();
    /** human readable description of things connected this NetObject */
    public abstract String connectionDescription(int maxConn);
    public String toString() {
    	LayoutLib.error(true, "Please call fullDescription() instead");
    	return "";
    }
	public String fullDescription() {
		StringBuffer sb = new StringBuffer();
		sb.append(instanceDescription());
		String v = valueDescription();
		if (!v.equals("")) sb.append(" "+v);
		String c = connectionDescription(MAX_CONN);
		if (!c.equals(" ")) sb.append(" "+c);
		return sb.toString();
	}
}

