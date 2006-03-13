/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: RecordList.java
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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.ncc.trees.EquivRecord;

public class RecordList {
	protected List<EquivRecord> content = new ArrayList<EquivRecord>(); 
	protected void error(boolean pred, String msg) {
		LayoutLib.error(pred, msg);
	}

	public RecordList() {}
	public EquivRecord get(int ndx) {return content.get(ndx);}
	public void add(EquivRecord x) {content.add(x);}
	public void addAll(RecordList x) {content.addAll(x.content);}
	public Iterator<EquivRecord> iterator(){return content.iterator();}
	public void clear(){content.clear();}
	public int size(){return content.size();}


}