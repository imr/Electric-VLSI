/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemCircuitHolderMap.java
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

package com.sun.electric.tool.ncc.lists;
import com.sun.electric.tool.ncc.trees.JemCircuit;
import com.sun.electric.tool.ncc.basicA.Messenger;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;

public class JemCircuitHolderMap extends JemMap {
		
    private JemCircuitHolderMap(int i){
		super(i);
		return;
    } //end of JemCircuitHolderMap constructor

	public static JemCircuitHolderMap mapPlease(int i){
		JemCircuitHolderMap mm= new JemCircuitHolderMap(i);
		return mm;
	} //end of mapPlease

	protected boolean classOK(Object x){
        return (x instanceof JemCircuitHolder);
    } //end of classOK
	
	/**
	 * This factory method does the "unwind" function to turn a list
	 * of columns into a column of lists.  given a JemCircuitMapHolder
	 * that holds a few, usually two, JemCircuitSpreads, each of which
	 * is a List or Map of several JemCircuits, it produces a
	 * JemCircuitHolderMap, a List of many JemCircuitHolders each with
	 * a few, ususally two, Jemircuits.
	 */
	public static JemCircuitHolderMap please(JemCircuitMapHolder in){
		//pass one, find out how many entries there are for pass 2
		int c= 0;
		Set s= new HashSet();
		Iterator it= in.iterator();
        while(it.hasNext()){
            Object oo= it.next(); //the next spread of circuits
			c= passOne(oo, c, s); //fill set s or increase c
		} //end of while

		//pass two, do the work
		// get a JemCircuitHolderMap filled with JemCircuitHolders
		JemCircuitHolderMap out= JemCircuitHolderMap.mapPlease(c);
		it= in.iterator();
        //row loop
        while(it.hasNext()){
            Object oo= it.next(); //the next spread of circuits
	//		if(oo instanceof JemCircuitMap)out= oneList(oo, out, c);
			if(oo instanceof JemCircuitMap)out= oneMap(oo, out, s);
        } //end of row loop
        int ii= out.size(); //for debug
        return out;
	} //end of please

	private static int passOne(Object oo, int c, Set s){
/*		if(oo instanceof JemCircuitMap){
			JemCircuitMap cs= (JemCircuitMap)oo;
			if(c < cs.size())c= cs.size();
			return c;
		} //end of if
*/
//		if(oo instanceof JemCircuitMap){
		JemCircuitMap m= (JemCircuitMap)oo;
		Set k= m.keySet();
		s.addAll(k);
		return s.size();
		//		} //end of if
//		return 0;
	} //end of passOne
/*
	private static JemCircuitHolderMap oneList(Object oo,
	JemCircuitHolderMap chm, int c){
        JemCircuitMap cs= (JemCircuitMap)oo;
        //column loop
        for(int i= 0; i < c; i++){
            //does chm have an entry?
			Integer ii= new Integer(i);
            JemCircuitHolder ch=(JemCircuitHolder)chm.get(ii);
            if(ch == null){
                ch= new JemCircuitHolder(2);
                chm.set(ii, ch);
            } //end of if
			//does ch have an entry? or make an empty one
            JemCircuit jc= (JemCircuit)cs.get(i);
            if(jc == null)jc= JemCircuit.please(1);
            ch.add(jc);
        } //end of column loop
        return chm;
    } //end of oneList
*/
	private static JemCircuitHolderMap oneMap(Object oo, JemCircuitHolderMap chm,
											  Set s){
		JemCircuitMap mm= (JemCircuitMap)oo;
		Iterator it= s.iterator();
		while(it.hasNext()){
			//does chm have this entry?
			Integer ii= (Integer)it.next();
			JemCircuitHolder ch=(JemCircuitHolder)chm.get(ii);
            if(ch == null){
                ch= new JemCircuitHolder();
                chm.put(ii, ch);
            } //end of if
			JemCircuit jc= (JemCircuit)mm.get(ii);
            if(jc == null)jc= JemCircuit.please();
            ch.add(jc);
        } //end of column loop
        return chm;
	} //end of oneMap
	
} //end of class JemCircuitHolderMap
