/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemEquivList.java
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
import com.sun.electric.tool.ncc.trees.JemEquivRecord;

import java.util.Iterator;
import java.util.Collections;

public class JemEquivList extends JemRecordList {

	public JemEquivList(){	}

	protected boolean classOK(Object x){
		if (x instanceof JemEquivRecord){
			JemEquivRecord er= (JemEquivRecord)x;
			if(er.getCode() == -597136633){
				getMessenger().error("killing " + er.nameString());
			} //end of if
			return true;
		} //end of if
		return false;
	} //end of classOK
	
	public void sort(){Collections.sort(getContent());}

	protected void reportClassError(){
		getMessenger().error("JemEquivList can add only JemEquivRecords");
		return;
	}// end of reportClassError

	public String sizeInfoString(){
        String max= " Of max sizes";
        String diff= " and size differences";
        boolean matchOK= true;
        Iterator it= iterator();
        while(it.hasNext()){
            JemEquivRecord g= (JemEquivRecord)it.next();
            max= max + " " + g.maxSize();
            diff= diff + " " + g.maxSizeDiff();
            if(g.maxSizeDiff() > 0) matchOK= false;
        } //end of loop
        if(matchOK)return (max);
        else return (max + diff + " WARNING: Mismatched sizes");
    } //end of sizeInfoString
	
	//public JemRecord getFirstRecord(){return (JemRecord)getFirst();}

} //end of JemEquivList
