/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: JemLeafList.java
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



import com.sun.electric.tool.ncc.NccGlobals;

import com.sun.electric.tool.ncc.trees.JemEquivRecord;

import com.sun.electric.tool.ncc.basicA.Messenger;



import java.util.Iterator;

import java.util.Collections;

import java.util.Comparator;



/**

 */

public class JemLeafList extends JemRecordList {

	private static class SizeCompare implements Comparator {

		public int compare(Object o1, Object o2){

			JemEquivRecord s1 = (JemEquivRecord)o1;

			JemEquivRecord s2 = (JemEquivRecord)o2;

			return s1.maxSize() - s2.maxSize();

		}

	}



	public void add(JemEquivRecord r) {

		error(!r.isLeaf(), "JemEquivList only allows leaves");

		super.add(r);

	}

	public void sortByIncreasingSize() {

		Collections.sort(content, new SizeCompare());

	}



	public String sizeInfoString() {

		String max= " offspring max sizes:";

		String diff= " offspring size differences: ";

		boolean matchOK= true;

		for (Iterator it= iterator(); it.hasNext();) {

			JemEquivRecord g= (JemEquivRecord)it.next();

			max= max + " " + g.maxSize();

			diff= diff + " " + g.maxSizeDiff();

			if(g.maxSizeDiff() > 0) matchOK= false;

		}

		if(matchOK)return (max);

		else return (max +"\n"+ diff + "\n WARNING: Mismatched sizes");

	}



	/** 

	 * selectActive selects leaf JemEquivRecords that aren't retired or 

	 * mismatched

	 * @return a JemLeafList, possibly empty, of those that do retire.

	 */

	public JemLeafList selectActive(NccGlobals globals) {

		JemLeafList out = new JemLeafList();

		for (Iterator it=iterator(); it.hasNext();) {

			JemEquivRecord er= (JemEquivRecord) it.next();

			if(er.isActive()) out.add(er);

		}

		globals.println(" selectActive found "+out.size()+

					   " active leaf records");

		return out;

	}



	/** 

	 * selectRetired selects JemEquivRecords that are retired

	 * @return a JemLeafList, possibly empty, of those that do retire.

	 */

	public JemLeafList selectRetired(NccGlobals globals) {

		JemLeafList out= new JemLeafList();

		for (Iterator it=iterator(); it.hasNext();) {

			JemEquivRecord er = (JemEquivRecord) it.next();

			if(er.isRetired())  out.add(er);

		}

		globals.println(" selectRetired found "+out.size()+

					   " retired leaf records");

		return out;

	}



}

