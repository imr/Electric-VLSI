/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BdRouter.java
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.tool.generator.layout.TechType;

/** Brain Dead router. Only do simple interconnections */
public class BdRoute {
	private TechType tech;
	private List<Channel> m2channels, m3channels;
	private BdRoute(TechType tech) {this.tech=tech;}
	
	/** List sorted in increasing Y */
	private List<NodeInst> getStages(Cell layCell) {
		List<NodeInst> stages = new ArrayList<NodeInst>();
		for (Iterator nIt=layCell.getNodes(); nIt.hasNext();) {
			NodeInst ni = (NodeInst) nIt.next();
			if (ni.getProto() instanceof Cell) stages.add(ni);
		}
		// Sort in increasing Y
		Collections.sort(stages, new Comparator<NodeInst>() {
			public int compare(NodeInst n1, NodeInst n2) {
				double delta = n1.getAnchorCenterY() -
				               n2.getAnchorCenterY();
				return (int) Math.signum(delta);
			}
		});
		return stages;
	}
	
	
	private void setUpChannels(Cell layCell) {
		List<NodeInst> stages = getStages(layCell);
		
	}
	
	private void route1(Cell layCell, List<ToConnect> work) {
		
	}
	public static void route(Cell layCell, List<ToConnect> work, TechType tech) {
		BdRoute br = new BdRoute(tech);
		br.route1(layCell, work);
	}
}
