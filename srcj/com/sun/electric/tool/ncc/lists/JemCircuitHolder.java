/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemCircuitHolder.java
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

import java.util.Iterator;

public class JemCircuitHolder extends JemHolder {

    public JemCircuitHolder(){}

	public static JemCircuitHolder please(int i){
		JemCircuitHolder h= new JemCircuitHolder();
		return h;
	} //end of please

    protected boolean classOK(Object x){
        return (x instanceof JemCircuit);
    } //end of classOK

    protected void reportClassError(){
        getMessenger().error("JemCircuitHolder can add only JemCircuit");
        return;
    }// end of reportClassError

    public int maxSize(){
        int out= 0;
        Iterator it= iterator();
        while(it.hasNext()){
            Object oo= it.next();
            JemCircuit j= (JemCircuit)oo;
            if(j.size() > out)out= j.size();
        } //end of while
        return out;
    } //end of maxSize

    public String sizeString(){
        if(size() == 0) return "0";
        Iterator it= iterator();
        String s= "";
        while(it.hasNext()){
            Object oo= it.next();
            JemCircuit jc= (JemCircuit)oo;
            s= s + " " + jc.size() ;
        } //end of while
        return s;
    } //end of sizeString

    public int maxSizeDiff(){
        int out= 0;
        int max= maxSize();
        Iterator it= iterator();
        while(it.hasNext()){
            Object oo= it.next();
            JemCircuit j= (JemCircuit)oo;
            int diff= max-j.size();
            if(diff > out)out= diff;
        } //end of loop
        return out;
    } // end of maxSizeDiff
    
} //end of JemCircuitHolder
