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
import com.sun.electric.tool.ncc.basicA.Name;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.ncc.jemNets.Part;
import com.sun.electric.tool.ncc.jemNets.Wire;
import com.sun.electric.tool.ncc.trees.JemCircuit;

import java.util.List;
import java.util.Iterator;

public class TransistorTwo extends Transistor{
    // ---------- private data -------------
    private static final int NUM_CON= 4;
    //these coeficients apply for symmetric case only
    private static final int TERM_COEFS[] = {7, 13, 13, 7};

    private boolean symmetric= false;
    /** width of transistors parallel merged into me without flipping gates */ 
    private double unflipWidth;  
	/** width of transistors parallel merged into me after flipping gates */
	private double flipWidth; 
    public boolean isSymmetric(){return symmetric;}

    private void flip(){
        Wire w= pins[0];
		pins[0] = pins[3];
        pins[3] = w;
		w = pins[1];
		pins[1] = pins[2];
        pins[2] = w;
    }

    // ---------- private methods ----------
    protected TransistorTwo(JemCircuit parent, Name n, 
    						Transistor.Type type, double length, double width,
    						Wire source, Wire gateA, Wire gateB, Wire drain){
		super(n, NUM_CON);
		error(parent==null, "no parent?");
		parent.adopt(this);

		pins[0] = source;
		pins[1] = gateA;
		pins[2] = gateB;
		pins[3] = drain;
		source.add(this);
		gateA.add(this);
		gateB.add(this);
		drain.add(this);
		
		myType = type;
		myWidth = width;
		unflipWidth = width;
    }

    // ---------- public methods ----------

	/** 
	 * Merge two TransistorOnes into a TransistorTwo.  
	 * @param w wire joining diffusions of two transistors
	 * @return
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
		if(ta.myWidth!=tb.myWidth)  return false;
		
		// it's a match - make the new one
        if (ta.pins[2]!=w)  ta.flip();
        if (tb.pins[0]!=w)  tb.flip();
		error(ta.pins[2]!=w || tb.pins[0]!=w, "building TransistorTwo");
		new TransistorTwo((JemCircuit)tb.getParent(), tb.getTheName(),
		 				  ta.myType, ta.myLength, ta.myWidth, 
						  ta.pins[0], ta.pins[1], tb.pins[1], tb.pins[2]);
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

    //merge into this transistor
    public boolean parallelMerge(Part p){
        if(!(p instanceof TransistorTwo)) return false;
        TransistorTwo t= (TransistorTwo)p;
        if(this == t)return false; //same transistor
		//its the same class but a different individual
		if( ! this.isLike(t))return false; //wrong type transistor

		// myMessenger.line("Comparing " + nameString() +
		//    " to " + t.nameString());

		if (pins[0]!=t.pins[0])  t.flip();
		if (pins[0]!=t.pins[0] || pins[3]!=t.pins[3]) return false;

		// check gates
		if(pins[1]==t.pins[1] && pins[2]==t.pins[2]){
			// if gates match without swapping then don't change symmetric
			unflipWidth += t.myWidth;
		} else if(pins[1]==t.pins[2] && pins[2]==t.pins[1]){
			// if gates match after swapping then set symmetric
			flipWidth += t.myWidth;
			symmetric= true;
		}else{
			return false;
		}

		//OK to merge topologically
		//what remains can merge
		//		myMessenger.line("Merging " + nameString() +
		//		" and " + t.nameString());
		myWidth += t.myWidth;
		t.deleteMe();
		return true; //return true if merged
    }

    public void checkMe(JemCircuit parent) {
    	if (symmetric && (flipWidth!=unflipWidth)) {
			// Aborting is too severe. However, leave this for now.
    		error(true,
				  "symmetric TransistorTwo: two parts don't have same width: "+
				  flipWidth+" "+unflipWidth);
    	}
    	super.checkMe(parent);
    }

    //over ride the hash computation
    public Integer computeHashCode(){
		if(symmetric)return super.computeHashCode();
		//the function is symmetric: ABCD = DCBA
		//but not symmetric in gates: ABCD != ACBD
		int[] xx= new int[NUM_CON];
		//get the wire values
		for(int i=0; i<pins.length; i++){
			xx[i]= pins[i].getCode() * TERM_COEFS[i];
		}
		int sum= (xx[0] + xx[1]) * (xx[2] + xx[3]);
        return new Integer(sum);
    }

    // ---------- printing methods ----------

    public String nameString(){
		String t= symmetric?"(Sym)":"(Asym)";
		String s= super.nameString();
		return (s + "TransTwo" + t + " " + getStringName());
    }

    public String connectionString(int n){
        String s = pins[0].getStringName();
        String gs= pins[1].getStringName();
        String gd= pins[2].getStringName();
        String d = pins[3].getStringName();
		return ("S=" + s + " Gs=" + gs + " Gd=" + gd + " D=" + d);
    }

}
