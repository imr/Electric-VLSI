/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TransistorTwo.java
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

/** 
 * TransistorTwo represents a two-gate transistor with both gates the
 * same width and length.  TransistorTwos can be symmetric or not
 * symmetric.  Symmetric means the two gates are interchangable Not
 * symmetric means the two gates are ordered.  has code computations
 * for non-symmetric TransistorTwo is harder than for symmetric
 * TransistorTwo.
*/
package com.sun.electric.tool.ncc.jemNets;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.jemNets.Part;
import com.sun.electric.tool.ncc.jemNets.Wire;
import com.sun.electric.tool.ncc.trees.JemCircuit;

import java.util.List;
import java.util.Iterator;

public class TransistorTwo extends Transistor{
    // ---------- private data -------------
    //these coeficients apply for symmetric case only
    private static final int TERM_COEFS[] = {7, 13, 13, 7};

	/** width of transistors parallel merged into me after flipping gates */
	private double flipWidth;
	private boolean symmetric; 

    private void flip(){
        Wire w= pins[0];
		pins[0] = pins[3];
        pins[3] = w;
		w = pins[1];
		pins[1] = pins[2];
        pins[2] = w;
    }

    // ---------- private methods ----------
    private TransistorTwo(String name, 
    					  Type type, double width, double length,
    					  Wire src, Wire gateA, Wire gateB, Wire drn){
		super(type, name, width, length, 
		      new Wire[] {src, gateA, gateB, drn});
    }

    // ---------- public methods ----------

	public boolean isSymmetric() {return symmetric;}

	/** 
	 * Merge two TransistorOnes into a TransistorTwo.  
	 * @param w wire joining diffusions of two transistors
	 * @return true if merge has taken place
	 */
    public static boolean joinOnWire(Wire w){
		TransistorOne ta= null;
		TransistorOne tb= null;
		error(w.numParts()!=2, "wire may only connect two Transistor diffusions");
		for (Iterator it=w.getParts(); it.hasNext();){
			Object oo= it.next();
			if(oo instanceof TransistorOne) {
				TransistorOne t= (TransistorOne)oo;
				if (ta==null)  ta= t;
				else  tb= t;
			}
		}
		if(ta==null || tb==null)  return false;
		error(ta==tb, "part should only occur once on wire");
		error(ta.getParent()!=tb.getParent(), "mismatched parents?");
		if( ! ta.isLike(tb))  return false;
		if(ta.width!=tb.width)  return false;
		
		// it's a match - make the new one
        if (ta.pins[2]!=w)  ta.flip();
        if (tb.pins[0]!=w)  tb.flip();
		error(ta.pins[2]!=w || tb.pins[0]!=w, "building TransistorTwo");
		TransistorTwo t2 = new TransistorTwo(tb.getName(), ta.getType(), 
											 ta.getWidth(), ta.getLength(), 
						  					 ta.pins[0], ta.pins[1], 
						  					 tb.pins[1], tb.pins[2]);
		JemCircuit parent = (JemCircuit) tb.getParent();
		parent.adopt(t2);						  
		ta.deleteMe();
		tb.deleteMe();
		error(w.numParts()!=0, "wire not empty?");
		return true;
    }

    // ---------- abstract commitment ----------

	public boolean isThisGate(int x){return ((x == 1) || (x == 2));}

    public int[] getTermCoefs(){return TERM_COEFS;} //the terminal coeficients

    // ---------- public methods ----------

    public boolean touchesAtGate(Wire w){
    	return w==pins[1] || w==pins[2];
    }

    public boolean touchesAtDiffusion(Wire w){
    	return w==pins[0] || w==pins[3];
    }

    public boolean isCapacitor(){
    	return pins[0]==pins[3] && pins[1]==pins[2];
    }

    /**
     * Attempt to merge this part with p. If merge has been done then destroy
     * p.
     * @return true if merged
     */
    public boolean parallelMerge(Part p){
        if(!(p instanceof TransistorTwo)) return false;
        TransistorTwo t= (TransistorTwo)p;
        error(this==t, "Transistor2 merging with itself");

		if (!isLike(t)) return false; //wrong type transistor

		error(t.isSymmetric(), "this algorithm can't merge two symmetrics");
		if (pins[0]!=t.pins[0])  t.flip();
		if (pins[0]!=t.pins[0] || pins[3]!=t.pins[3]) return false;

		// check gates
		if(pins[1]==t.pins[1] && pins[2]==t.pins[2]){
			// if gates match without swapping then don't change symmetric
			width += t.width;
		} else if(pins[1]==t.pins[2] && pins[2]==t.pins[1]){
			// if gates match after swapping then set symmetric
			symmetric = true;
			flipWidth += t.width;
		} else {
			return false;
		}

		//OK to merge topologically
		t.deleteMe();
		return true; //return true if merged
    }

    public void checkMe(JemCircuit parent) {
    	if (isSymmetric() && (flipWidth!=width)) {
			// Aborting is too severe. However, leave this for now.
    		error(true,
				  "symmetric TransistorTwo: two parts don't have same width: "+
				  flipWidth+" "+width);
    	}
    	super.checkMe(parent);
    }

    //over ride the hash computation
    public Integer computeHashCode(){
		if (isSymmetric()) return super.computeHashCode();
		//the function is symmetric: ABCD = DCBA
		//but not symmetric in gates: ABCD != ACBD
		int[] xx= new int[pins.length];
		//get the wire values
		for (int i=0; i<pins.length; i++){
			xx[i]= pins[i].getCode() * TERM_COEFS[i];
		}
		int sum= (xx[0] + xx[1]) * (xx[2] + xx[3]);
        return new Integer(sum);
    }

    // ---------- printing methods ----------

    public String nameString(){
		String t= isSymmetric() ? "(Sym)" : "(Asym)";
		String s= super.nameString();
		return s + "TransTwo" + t + " " + getName();
    }

    public String connectionString(int n){
        String s = pins[0].getName();
        String gs= pins[1].getName();
        String gd= pins[2].getName();
        String d = pins[3].getName();
		return ("S=" + s + " Gs=" + gs + " Gd=" + gd + " D=" + d);
    }

}
