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
import com.sun.electric.tool.ncc.basicA.Messenger;

import java.util.Iterator;
import java.util.Collections;
import java.util.Comparator;

public class JemEquivList extends JemRecordList {

	private static class SizeCompare implements Comparator {
		public int compare(Object o1, Object o2){
			JemEquivRecord s1 = (JemEquivRecord)o1;
			JemEquivRecord s2 = (JemEquivRecord)o2;
			return s1.maxSize() - s2.maxSize();
		}
	}

	public JemEquivList(){	}

	protected boolean classOK(Object x){
		if (x instanceof JemEquivRecord){
			JemEquivRecord er= (JemEquivRecord)x;
			if(er.getCode() == -597136633){
				Messenger.error("killing " + er.nameString());
			} //end of if
			return true;
		} //end of if
		return false;
	}
	
	public void sortByIncreasingSize() {
		Collections.sort(getContent(), new SizeCompare());
	}

	protected void reportClassError(){
		Messenger.error("JemEquivList can add only JemEquivRecords");
		return;
	}

	public String sizeInfoString(){
        String max= " Of max sizes";
        String diff= " and size differences";
        boolean matchOK= true;
        for (Iterator it= iterator(); it.hasNext();) {
            JemEquivRecord g= (JemEquivRecord)it.next();
            max= max + " " + g.maxSize();
            diff= diff + " " + g.maxSizeDiff();
            if(g.maxSizeDiff() > 0) matchOK= false;
        }
        if(matchOK)return (max);
        else return (max + diff + " WARNING: Mismatched sizes");
    }

	/** 
	 * selectActive selects JemEquivRecords that aren't retired or mismatched
	 * @return a JemEquivList, possibly empty, of those that do retire.
	 */
	public JemEquivList selectActive(){
		int numRetired= 0;
	    JemEquivList out = new JemEquivList();
	    for (Iterator it=iterator(); it.hasNext();) {
			JemEquivRecord er= (JemEquivRecord) it.next();
	        if(er.isActive()) out.add(er);
	    }
		Messenger.line("selectActive retired " +numRetired+" JemEquivRecords");
	    return out;
	}

	/** 
	 * selectRetired selects JemEquivRecords that are retired
	 * @return a JemEquivList, possibly empty, of those that do retire.
	 */
	public JemEquivList selectRetired(){
		JemEquivList out= new JemEquivList();
	    for (Iterator it=iterator(); it.hasNext();) {
			JemEquivRecord er = (JemEquivRecord) it.next();
	        if(er.isRetired())  out.add(er);
	    }
		Messenger.line("findNewlyRetired found " +
					   out.size() + " retired JemEquivRecords");
		return out;
	}
	

}
