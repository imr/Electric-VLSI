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
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.strategy.Strategy;
import com.sun.electric.tool.ncc.jemNets.Wire;
import com.sun.electric.tool.ncc.jemNets.Port;
import com.sun.electric.tool.ncc.jemNets.NetObject;
import com.sun.electric.tool.ncc.basic.Messenger;
import java.util.Iterator;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Circuit {
    private EquivRecord myParent;
    private ArrayList netObjs = new ArrayList();

    private Circuit(){}

	private static void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}

	// ---------------------- public methods ------------------
	public static Circuit please(List netObjs){
		Circuit ckt = new Circuit();
		for (Iterator it=netObjs.iterator(); it.hasNext();) {
			ckt.adopt((NetObject)it.next());
		}
		return ckt;
	}
	/** Remove deleted NetObjects. Minimize storage used. */
	public void putInFinalForm() {
		Set goodObjs = new HashSet();
		for (Iterator it=netObjs.iterator(); it.hasNext();) {
			NetObject n = (NetObject) it.next();
			if (n.isDeleted()) continue;
			error(goodObjs.contains(n), "duplicate NetObj in Circuit!???");
			goodObjs.add(n);
			if (n instanceof Wire)  ((Wire)n).putInFinalForm();
		}
		netObjs = new ArrayList();
		netObjs.addAll(goodObjs);
		netObjs.trimToSize();
	}
    
	public Iterator getNetObjs() {return netObjs.iterator();}
	public int numNetObjs() {return netObjs.size();}
	public int numUndeletedNetObjs() {
		int count = 0;
		for (Iterator it=getNetObjs(); it.hasNext();) {
			NetObject n = (NetObject) it.next();
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
	
	public HashMap apply(Strategy js){
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
}
