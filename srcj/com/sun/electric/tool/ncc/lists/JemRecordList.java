/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemRecordList.java
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
import com.sun.electric.tool.ncc.trees.JemRecord;
import com.sun.electric.tool.ncc.strategy.JemStrat;

import java.util.Iterator;

public class JemRecordList extends JemList {

	public JemRecordList(){}

	protected boolean classOK(Object x){
		return (x instanceof JemRecord);
	} //end of classOK

	protected void reportClassError(){
		getMessenger().error("JemRecordList can add only JemRecords");
	}// end of reportClassError
	
	public JemEquivList apply(JemStrat s){
		JemEquivList out= new JemEquivList();
		Iterator it= iterator();
		while(it.hasNext()){
			Object oo= it.next();
			JemRecord jr= (JemRecord)oo;
			JemEquivList xx= s.doFor(jr);
			out.addAll(xx);
		} //end of while
		return out;
	} //end of apply

	//public JemRecord getFirstRecord(){return (JemRecord)getFirst();}

} //end of JemRecordList
