/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemList.java
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
 * JemMap is the parent class for the maps that hold Circuits and
 * CircuitHolders. 
 */
package com.sun.electric.tool.ncc.lists;
import com.sun.electric.tool.ncc.basicA.Messenger;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

public abstract class JemMap {

	private Map content;
	private static Messenger myMessenger;

	public JemMap(int i){
		content= new HashMap(i);
		myMessenger= Messenger.toTestPlease("JemMap");
		return;
	} //end of JemMap constructor

	protected static Messenger getMessenger(){return myMessenger;}
	protected abstract boolean classOK(Object x);

	protected void reportClassError(){
		getMessenger().error("JemMap has class type error");
		return;
	} //end of reportClassError

	public boolean put(Integer i, Object j){
        if(classOK(j)){
            content.put(i, j);
            return true;
        } else {
            reportClassError();
            return false;
        } //end of else
    } //end of set

	public Object get(Integer i){return content.get(i);}
	
	public boolean containsKey(Integer i){return content.containsKey(i);}
	
	public Iterator keyIterator(){return content.keySet().iterator();}
	public void clear(){content.clear();}
	public int size(){return content.size();}
	public Object remove(Object j){return content.remove(j);}

	public Set keySet(){return content.keySet();}

} //end of JemMap
