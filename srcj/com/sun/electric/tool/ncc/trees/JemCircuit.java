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
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.strategy.JemStrat;
import com.sun.electric.tool.ncc.jemNets.Wire;
import com.sun.electric.tool.ncc.jemNets.Port;
import com.sun.electric.tool.ncc.basicA.Messenger;

import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;


//JemCircuit will add only NetObjects

public class JemCircuit {

    private JemEquivRecord myParent;
    // Use HashSet for content in order to make remove() operation
    // constant time. Otherwise we spend all our time parallel
    // merge which removes Parts from huge globals.parts 
    private Set content = new HashSet();

    private JemCircuit(){}

	public static JemCircuit please(List netObjs){
		JemCircuit ckt = new JemCircuit();
		for (Iterator it=netObjs.iterator(); it.hasNext();) {
			ckt.adopt((NetObject)it.next());
		}
		return ckt;
	}
    
	public Iterator getNetObjs() {return content.iterator();}
	public int numNetObjs() {return content.size();}
    public void adopt(NetObject n) {
    	content.add(n);
    	n.setParent(this);
    }
    public void remove(NetObject n) {content.remove(n);}

	public static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}
	public void checkMe(JemEquivRecord parent) {
		error(getParent()!=parent, "wrong parent"); 
	}

	/** 
	 * getExportMap produces a map of String export names to Wires.
	 * @return a map of Strings to Wires
	 */
	public Map getExportMap(){
		Map out = new HashMap();
		for (Iterator it=getNetObjs(); it.hasNext();) {
			NetObject n= (NetObject)it.next();
			error(!(n instanceof Wire), "getExportMap expects only Wires");
			Wire w= (Wire)n;
			Port p = w.getPort();
			if (p!=null) {
				for (Iterator ni=p.getExportNames(); ni.hasNext();) {
					String exportNm = (String) ni.next();
					error(out.containsKey(exportNm),
						  "different wires have the same export name?");
					out.put(exportNm, w);
				}
			}
		}
//		printTheMap(out);
		return out;
	}
	
    public String nameString(){return "JemCircuit";}
	
    public int getCode(){
    	return myParent!=null ? myParent.getCode() : 0;
    }

    public JemEquivRecord getParent(){return myParent;}
	
	public void setParent(JemEquivRecord p){
		myParent= (JemEquivRecord)p;
	}
	
	public HashMap apply(JemStrat js){
		HashMap codeToNetObjs = new HashMap();
		for (Iterator it=getNetObjs(); it.hasNext();) {
			NetObject no= (NetObject)it.next();
			Integer code = js.doFor(no);
			error(code==null, "null is no longer a legal code");
			ArrayList ns = (ArrayList) codeToNetObjs.get(code);
			if(ns==null) {
				ns = new ArrayList();
				codeToNetObjs.put(code, ns);
			} 
			ns.add(no);
		}
		return codeToNetObjs;
	}
	
	public static void printTheMap(Map m, NccGlobals globals){
		globals.println("printing a Circuit map of size= " + m.size());
		if(m.size() == 0)return;
		Iterator it= m.keySet().iterator();
		while(it.hasNext()){
			String s= (String)it.next();
			Object oo= m.get(s);
			if(oo == null){
				globals.println(s + " maps to null");
			} else {
				Wire w= (Wire)oo;
				globals.println(s + " maps to " + w.nameString());
			}
		}
	}
	
	public void printMe(NccGlobals globals){
		globals.println(nameString() + 		
				" and code " + getCode() +
				" size= " + numNetObjs());
	}
	
}
