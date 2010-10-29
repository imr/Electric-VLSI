/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Blockage1D.java
 *
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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
package com.sun.electric.tool.generator.flag.router;

import java.util.LinkedList;
import java.util.List;

import com.sun.electric.tool.generator.flag.Utils;

/** Keep track of blockages in one dimension */
public class Blockage1D {
	// sorted by increasing min values
	private List<Interval> blockages = new LinkedList<Interval>();
	
	private void prln(String msg) {Utils.prln(msg);}
	
	public Blockage1D() {}
	
	public void block(double min, double max) {
		//prln("Blocking "+min+" to "+max);
		int i=0;
		for (; i<blockages.size(); i++) {
			Interval in = blockages.get(i);
			if (in.getMax()<min) continue;
			if (in.getMin()>max) break;
			// overlap detected
			in.merge(min, max);
			return;
		}
		// No overlap detected.  Add blockage to list. 
		blockages.add(i, new Interval(min,max));
	}
	public List<Interval> getBlockages() {
		return blockages;
	}

}
