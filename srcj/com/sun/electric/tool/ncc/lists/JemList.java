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

package com.sun.electric.tool.ncc.lists;
import com.sun.electric.tool.ncc.basicA.Messenger;

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public abstract class JemList {

	private List content;

	public JemList(){
		content= new ArrayList();
	}

	public List getContent(){return content;}
	
	protected abstract boolean classOK(Object x);

	protected void reportClassError(){
		Messenger.error("JemList has class type error");
	} //end of reportClassError

	public boolean add(Object x){
		if(classOK(x)){
			return content.add(x);
		} else {
			reportClassError();
			return false;
		} //end of else
	} //end of add
	
	public boolean addAll(JemList x){
		if(x == null)return false;
		return content.addAll(((JemList)x).content);
	} //end of addAll
	
	public Iterator iterator(){return content.iterator();}
	public void clear(){content.clear();}
	public int size(){return content.size();}
	public boolean remove(Object j){return content.remove(j);}

	public Object getFirst(){return content.get(0);}
	public Object get(int i){return content.get(i);}
	public Object set(int i, Object o){return content.set(i, o);}
	//protected List getContent(){return content;}

} //end of JemList
