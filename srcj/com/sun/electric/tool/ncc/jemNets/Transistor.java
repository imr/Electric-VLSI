/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Transistor.java
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
//	Updated 16 October 2003 to use JemTree interface

package com.sun.electric.tool.ncc.jemNets;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.jemNets.Part;
import com.sun.electric.tool.ncc.jemNets.Wire;
// import com.sun.electric.tool.ncc.trees.JemTree;

import java.util.ArrayList;
import java.util.List;

public abstract class Transistor extends Part {
    // ---------- private data -------------
    protected double width;
    private final double length;
    private final Type type;
    
    // ---------- private methods ----------
    protected Transistor(Type np, String name, double width, double length,
					     Wire[] pins) {
		super(name, pins);
		type = np;
		this.width = width;
		this.length = length;
    }

    // ---------- public methods ----------
    Type getType() {return type;}
    double getLength() {return length;}
    double getWidth() {return width;}
    
    public boolean isNtype(){return (type== NTYPE);}
    public boolean isPtype(){return (type== PTYPE);}

    // ---------- abstract commitment ----------

//    public boolean remove(NetObject j){return super.remove(j);}
    public abstract int[] getTermCoefs();

    // ---------- public methods ----------

    //merge into this transistor
    public abstract boolean parallelMerge(Part p);
    public abstract boolean touchesAtGate(Wire w);
    public abstract boolean touchesAtDiffusion(Wire w);
		
	/**
	 * Compare the type (N vs P) and the gate length
	 * @param t Transistor to compare to
	 * @return true if type and gate length match
	 */
	public boolean isLike(Transistor t){
		return type==t.type && length==t.length;
    }
	
    // ---------- printing methods ----------

	public String nameString(){
		return type!=null ? type.name : "";
    }
	
	public String valueString(){
		return "W= " + width + " L= " + length;
	}

    public abstract String connectionString(int n);

	protected static class Type {
		private String name;
		private Type(String s){name=s;}
	}
	
	protected static final Type NTYPE= new Type("N-");
	protected static final Type PTYPE= new Type("P-");
}
