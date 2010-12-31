/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: WireQualityMetric.java
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.tool.routing.metrics.WireQualityMetric.QualityResults;

/**
 * @author Felix Schmidt
 * 
 */
public class WireQualityMetric extends RoutingMetric<QualityResults> {

	private static Logger logger = LoggerFactory.getLogger(WireQualityMetric.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sun.electric.tool.routing.metrics.RoutingMetric#calculate(com.sun
	 * .electric.database.hierarchy.Cell)
	 */
	@Override
	public QualityResults calculate(Cell cell) {
		QualityResults result = new QualityResults();

		logger.trace("calculate via amount metric...");
		result.vias = new ViaAmountMetric().calculate(cell);
		logger.debug("via amount metric: " + result.vias);

		logger.trace("calculate stacked via amount metric...");
		result.stackedVias = new StackedViasAmountMetric().calculate(cell);
		logger.debug("stacked via amount metric: " + result.stackedVias);

		logger.trace("calculate detouring amount metric...");
		result.detourings = new DetouringAmountMetric().calculate(cell);
		logger.debug("detouring amount metric: " + result.detourings);

		logger.trace("calculate evenness metric...");
		result.evenness = new EvennessMetric().calculate(cell);
		logger.debug("evenness metric: " + result.evenness);

		return result;
	}

	public static class QualityResults {
		public Integer vias;
		public Integer stackedVias;
		public Integer detourings;
		public Double evenness;
	}

	/* (non-Javadoc)
	 * @see com.sun.electric.tool.routing.metrics.RoutingMetric#reduce(java.lang.Object, com.sun.electric.database.topology.ArcInst)
	 */
	@Override
	protected QualityResults reduce(QualityResults result, ArcInst instance, Network net) {
		// [fschmidt] method not required here
		throw new UnsupportedOperationException();
	}

}
