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
import com.sun.electric.tool.ncc.trees.JemEquivRecord;
import com.sun.electric.tool.ncc.strategy.JemStrat;
import com.sun.electric.tool.ncc.basicA.Messenger;
import com.sun.electric.tool.generator.layout.LayoutLib;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

public class JemRecordList {
	protected List content = new ArrayList(); 
	protected void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}

	public JemRecordList() {}
	public JemEquivRecord get(int ndx) {return (JemEquivRecord) content.get(ndx);}
	public void add(Object x) {content.add(x);}
	public void addAll(JemRecordList x) {content.addAll(x.content);}
	public Iterator iterator(){return content.iterator();}
	public void clear(){content.clear();}
	public int size(){return content.size();}

	public JemLeafList apply(JemStrat s){
		JemLeafList out= new JemLeafList();
		for (Iterator it=content.iterator(); it.hasNext();){
			JemEquivRecord jr= (JemEquivRecord)it.next();
			JemLeafList xx= s.doFor(jr);
			out.addAll(xx);
		}
		return out;
	}

}