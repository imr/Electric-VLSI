/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Circuit.java
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
package com.sun.electric.tool.ncc.trees;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.netlist.NetObject;
import com.sun.electric.tool.ncc.netlist.Wire;
import com.sun.electric.tool.ncc.strategy.Strategy;

public class Circuit {
    private EquivRecord myParent;
    private ArrayList<NetObject> netObjs = new ArrayList<NetObject>();

    private Circuit(){}

	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}

	// ---------------------- public methods ------------------
	public static Circuit please(List<NetObject> netObjs){
		Circuit ckt = new Circuit();
		for (NetObject n : netObjs) {
			ckt.adopt(n);
		}
		return ckt;
	}
	/** Remove deleted NetObjects. Minimize storage used. */
	public void putInFinalForm() {
		Set<NetObject> goodObjs = new HashSet<NetObject>();
		for (NetObject n : netObjs) {
			if (n.isDeleted()) continue;
			error(goodObjs.contains(n), "duplicate NetObj in Circuit!???");
			goodObjs.add(n);
			if (n instanceof Wire)  ((Wire)n).putInFinalForm();
		}
		netObjs = new ArrayList<NetObject>();
		netObjs.addAll(goodObjs);
		netObjs.trimToSize();
	}
    
	public Iterator<NetObject> getNetObjs() {return netObjs.iterator();}
	public int numNetObjs() {return netObjs.size();}
	public int numUndeletedNetObjs() {
		int count = 0;
		for (Iterator<NetObject> it=getNetObjs(); it.hasNext();) {
			NetObject n = it.next();
			if (!n.isDeleted()) count++;
		}
		return count;
	}
    public void adopt(NetObject n) {
    	netObjs.add(n);
    	n.setParent(this);
    }
    //public void remove(NetObject n) {netObjs.remove(n);}

	public void checkMe(EquivRecord parent) {
		error(getParent()!=parent, "wrong parent"); 
	}

    public String nameString(){
    	return "Circuit code=" + getCode() +
			   " size=" + numNetObjs();
    }
	
    public int getCode(){
    	return myParent!=null ? myParent.getCode() : 0;
    }

    public EquivRecord getParent(){return myParent;}
	
	public void setParent(EquivRecord p){
		myParent= (EquivRecord)p;
	}
	
	public HashMap<Integer,List<NetObject>> apply(Strategy js){
		HashMap<Integer,List<NetObject>> codeToNetObjs = new HashMap<Integer,List<NetObject>>();
		for (Iterator<NetObject> it=getNetObjs(); it.hasNext();) {
			NetObject no= it.next();
			Integer code = js.doFor(no);
			error(code==null, "null is no longer a legal code");
			ArrayList<NetObject> ns = (ArrayList<NetObject>) codeToNetObjs.get(code);
			if(ns==null) {
				ns = new ArrayList<NetObject>();
				codeToNetObjs.put(code, ns);
			} 
			ns.add(no);
		}
		return codeToNetObjs;
	}
}
