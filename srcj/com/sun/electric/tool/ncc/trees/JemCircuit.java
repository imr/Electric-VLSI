/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemCircuit.java
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
//  Updated 28 October to add the simplify the hierarchy and
//    include the flag.
// Updated 2 November to eliminate JemCircuitPlain and JemCircuitMap

package com.sun.electric.tool.ncc.trees;
import com.sun.electric.tool.ncc.strategy.JemStrat;
import com.sun.electric.tool.ncc.lists.JemCircuitMap;
import com.sun.electric.tool.ncc.jemNets.Wire;
import com.sun.electric.tool.ncc.jemNets.Port;
import com.sun.electric.tool.ncc.basicA.Messenger;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

//JemCircuit will add only NetObjects

public class JemCircuit extends JemParent implements JemChild{

    private JemEquivRecord myParent; //the upwards pointer

    private JemCircuit(){myParent= null;}

    public static JemCircuit please(){
        return new JemCircuit();
    } //end of please

	/** getExportMap produces a map of String export names to Wires including only Wires with export names unused by any other Wire in this circuit.
		* @return a map of Strings to Wires
		*/
	public Map getExportMap(){
		Map out= new HashMap(5);
		Iterator it= iterator();
		while(it.hasNext()){
			NetObject n= (NetObject)it.next();
			if( ! (n instanceof Wire))return out;
			Wire w= (Wire)n;
			List ports= w.getPortList();
			Iterator pi= ports.iterator();
			while(pi.hasNext()){
				Port pp= (Port)pi.next();
				String ss= pp.getStringName();
				if(out.containsKey(ss))out.put(ss, (Wire)null);
				else out.put(ss, w);
			} //end of pi loop
		} //end of while
	//	printTheMap(out);
		return out;
	} //end of getExportMap
	
    public String nameString(){return "JemCircuit";}
	
    public int getCode(){
        if(myParent != null)return myParent.getCode();
        else return 0;
    } //end of getCode

    public JemParent getParent(){return myParent;}
	
	/** setParent checks the proposed parent's class before writing it.
		* @param the JemParent proposed
		* @return true if parent was accepted, false otherwise
		*/
	public boolean setParent(JemParent x){
		if(x instanceof JemEquivRecord){
			myParent= (JemEquivRecord)x;
			return true;
		} else {
			getMessenger().error("The parent of a JemCircuit must be a JemEquivRecord");
			return false;
		} //end of else
	} //end of setParent
	
	public boolean checkParent(JemParent p){
		if(p instanceof JemEquivRecord)return true;
		else{
			getMessenger().error("bad parent class in " + nameString());
			return false;
		} //end of else
	} //end of checkParent

	public boolean checkChild(JemChild c){
		if(c instanceof NetObject)return true;
		else {
			getMessenger().error("bad child class in " + nameString());
			return false;
		} //end of else
	} //end of checkChild
	
	public JemCircuitMap apply(JemStrat js){
		JemCircuitMap out= JemCircuitMap.mapPlease(3);
		Iterator it= iterator();
		while(it.hasNext()){
			Object oo= it.next();
			NetObject no= (NetObject)oo;
			Integer x= js.doFor(no);
			if(x == null)continue;
			JemCircuit cc= (JemCircuit)out.get(x);
			if(cc == null)cc= JemCircuit.please();
			//remove(no);
			cc.adopt(no);
			out.put(x, cc);
		} //end of loop
		return out;
	} //end of apply
	
	public static void printTheMap(Map m){
		Messenger mm= getMessenger();
		mm.line("printing a Circuit map of size= " + m.size());
		if(m.size() == 0)return;
		Iterator it= m.keySet().iterator();
		while(it.hasNext()){
			String s= (String)it.next();
			Object oo= m.get(s);
			if(oo == null){
				mm.line(s + " maps to null");
			} else {
				Wire w= (Wire)oo;
				mm.line(s + " maps to " + w.nameString());
			} //end of else
		} //end of while
		return;
	} //end of printTheMap
	
	public void printMe(Messenger mm){
		mm.line(nameString() + 		
				" and code " + getCode() +
				" size= " + size());
		return;
	} //end of printMe
	
} //end of JemCircuit
