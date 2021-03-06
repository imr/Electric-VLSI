/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DetoruingAmountMetric.java
 *
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.tool.routing.metrics;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.topology.ArcInst;

/**
 * @author Felix Schmidt
 * 
 * This metric is part of the routing quality metric
 * 
 */
public class DetouringAmountMetric extends RoutingMetric<Integer> {

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sun.electric.tool.routing.metrics.RoutingMetric#calculate(com.sun
	 * .electric.database.hierarchy.Cell)
	 */
	public Integer calculate(Cell cell) {
		Integer result = 0;

		return result;
	}

	/* (non-Javadoc)
	 * @see com.sun.electric.tool.routing.metrics.RoutingMetric#reduce(java.lang.Object, com.sun.electric.database.topology.ArcInst)
	 */
	@Override
	protected Integer reduce(Integer result, ArcInst instance, Network net) {
		// TODO Auto-generated method stub
		return null;
	}

}
