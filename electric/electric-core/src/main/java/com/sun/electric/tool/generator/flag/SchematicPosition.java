/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SchematicPosition.java
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
package com.sun.electric.tool.generator.flag;

import java.util.ArrayList;
import java.util.List;

/** The position of a NodeInst deep in the hierarchy. For example "123/456/789"
 * means 789 is coordinate of NodeInst, 456 is coordinate of NodeInst's
 * parent, and 123 is coordinate of NodeInst's grand parent. */
public class SchematicPosition implements Comparable<SchematicPosition> {
	private List<Double> coords = new ArrayList<Double>();
	public int depth() {return coords.size();}
	public int compareTo(SchematicPosition p2) {
		int shared = Math.min(depth(), p2.depth());
		for (int i=0; i<shared; i++) {
			double x1 = coords.get(i);
			double x2 = p2.coords.get(i);
			if (x1==x2) continue;
			return (int) Math.signum(x1-x2);
		}
		// position matches up to shared depth
		return depth()-p2.depth();
	}
	public void push(double d) {
		coords.add(d);
	}
	public void pop() {
		coords.remove(coords.size()-1);
	}
	public SchematicPosition copy() {
		SchematicPosition c = new SchematicPosition();
		for (int i=0; i<depth(); i++) {
			c.push(coords.get(i));
		}
		return c;
	}
}
