/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LeafList.java
 *
 * Copyright (c) 2003 Sun Microsystems and Free Software
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

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

import com.sun.electric.tool.ncc.NccGlobals;
import com.sun.electric.tool.ncc.trees.EquivRecord;
public class LeafList extends RecordList {
	private static class SizeCompare implements Comparator<EquivRecord> {
		public int compare(EquivRecord s1, EquivRecord s2){
			return s1.maxSize() - s2.maxSize();
		}
	}
	public void add(EquivRecord r) {
		error(!r.isLeaf(), "EquivList only allows leaves");
		super.add(r);
	}

	public void sortByIncreasingSize() {
		Collections.sort(content, new SizeCompare());
	}

	public String sizeInfoString() {
		StringBuffer max = new StringBuffer(" offspring max sizes:");
		StringBuffer diff = new StringBuffer(" offspring size differences: ");

		boolean matchOK= true;
		for (Iterator<EquivRecord> it= iterator(); it.hasNext();) {
			EquivRecord g= (EquivRecord)it.next();
			max.append(" " + g.maxSize());
			diff.append(" " + g.maxSizeDiff());
			if(g.maxSizeDiff() > 0) matchOK= false;
		}
		if (matchOK) return (max.toString());
		else return (max +"\n"+ diff + "\n WARNING: Mismatched sizes");
	}
	/** @return a LeafList, possibly empty, of EquivRecords that aren't
	 * matched nor mismatched. */
	public LeafList selectActive(NccGlobals globals) {
		LeafList out = new LeafList();
		for (Iterator<EquivRecord> it=iterator(); it.hasNext();) {
			EquivRecord er= (EquivRecord) it.next();
			if(er.isActive()) out.add(er);
		}
		globals.status2(" selectActive found "+out.size()+
  					    " active leaf records");
		return out;
	}

	/** @return a LeafList, possibly empty, of those that are matched. */
	public LeafList selectMatched(NccGlobals globals) {
		LeafList out= new LeafList();
		for (Iterator<EquivRecord> it=iterator(); it.hasNext();) {
			EquivRecord er = (EquivRecord) it.next();
			if(er.isMatched())  out.add(er);
		}
		globals.status2(" selectMatched found "+out.size()+
					    " matched leaf records");
		return out;
	}
}

