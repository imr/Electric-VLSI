/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemHistogram.java
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

// JemHistogram forms a map relating Integers to counts.  

package com.sun.electric.tool.ncc.basicA;

import java.util.Iterator;
import java.util.Set;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Collections;
import java.util.ArrayList;

public class JemHistogram {

	private Map myMap;
	
	public JemHistogram(int i){
		myMap= new HashMap(i);
	} //end of constructor
	
	public void clear(){myMap.clear();}
	
	//this adds one to the entry "key" and returns the new value
	public int incrementEntry(int key){
		Integer k= new Integer(key);
		Integer value= (Integer)myMap.get(k);
		if(value == null)value= new Integer(0);
		int x= value.intValue();
		x++;
		value= new Integer(x);
		myMap.put(k, value);
		return x;
	} //end of entry
	
	public void printMe(Messenger mm){
		Set theKeys= myMap.keySet();
		List keys= new ArrayList(theKeys);
		Collections.sort(keys);
		Collections.sort(keys, Collections.reverseOrder());
		Iterator it= keys.iterator();
		mm.line("key   count");
		while(it.hasNext()){
			Integer key= (Integer)it.next();
			Integer value= (Integer)myMap.get(key);
			mm.line(key.toString() + "      " + value.toString());
		} //end of loop
		return;
	} //end of printMe
	
	public static void testMe(Messenger mm){
		JemHistogram xx= new JemHistogram(7);
		xx.incrementEntry(5);
		xx.incrementEntry(5);
		xx.incrementEntry(4);
		xx.incrementEntry(2);
		xx.incrementEntry(7);
		xx.printMe(mm);
		return;
	} //end of testMe
	
	
} //end of class JemHistogram
