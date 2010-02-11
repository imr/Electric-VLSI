/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: OverallAreaMetric.java
 * Written by Team 7: Felix Schmidt, Daniel Lechner
 * 
 * This code has been developed at the Karlsruhe Institute of Technology (KIT), Germany, 
 * as part of the course "Multicore Programming in Practice: Tools, Models, and Languages".
 * Contact instructor: Dr. Victor Pankratius (pankratius@ipd.uka.de)
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.placement.forceDirected2.metrics;

import com.sun.electric.tool.placement.PlacementFrame.PlacementNetwork;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNode;
import com.sun.electric.tool.placement.forceDirected2.forceDirected.util.CheckboardingField;
import com.sun.electric.tool.placement.forceDirected2.forceDirected.util.CheckboardingPattern;

import java.math.BigDecimal;
import java.util.List;

/**
 * Parallel Placement
 * 
 * Overall area metric
 */
public class OverallAreaMetric extends AbstractMetric {

	private static final String metricName = "Overall Area Metric";
	private CheckboardingPattern checkPattern;

	public OverallAreaMetric(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks, CheckboardingPattern pattern) {
		super(nodesToPlace, allNetworks);
		this.checkPattern = pattern;
	}

	@Override
	public Double compute() {
		return new Double(this.computeAsBigDecimal().doubleValue());
	}

	public BigDecimal computeAsBigDecimal() {

		if (this.checkPattern == null) {
			return new BigDecimal(-1.0);
		}

		BigDecimal minX = null;
		BigDecimal maxX = null;
		BigDecimal minY = new BigDecimal(-1);
		BigDecimal maxY = new BigDecimal(-1);

		for (int i = 0; i < this.checkPattern.getAll().length; i++) {
			for (int j = 0; j < this.checkPattern.getAll()[i].length; j++) {

				CheckboardingField field = this.checkPattern.getField(j, i);
				if (field.getNode() != null) {
					if (minX == null) {
						minX = new BigDecimal(field.getLocation().getX() - field.getNode().getWidth() / 2);
						maxX = new BigDecimal(field.getLocation().getX() - field.getNode().getWidth() / 2);
					}

					if (minY == null) {
						minY = new BigDecimal(field.getLocation().getY() - field.getNode().getHeight() / 2);
						maxY = new BigDecimal(field.getLocation().getY() - field.getNode().getHeight() / 2);
					}

					BigDecimal tmpX = new BigDecimal(field.getLocation().getX() + field.getNode().getWidth() / 2);
					maxX = maxX.max(tmpX);

					BigDecimal tmpY = new BigDecimal(field.getLocation().getY() + field.getNode().getHeight() / 2);
					maxY = maxY.max(tmpY);
				}
			}
		}

		BigDecimal x = maxX.subtract(minX);
		BigDecimal y = maxY.subtract(minY);

		return x.multiply(y);

	}

	public CheckboardingPattern getCheckPattern() {
		return this.checkPattern;
	}

	@Override
	public String getMetricName() {
		return OverallAreaMetric.metricName;
	}

	public void setCheckPattern(CheckboardingPattern checkPattern) {
		this.checkPattern = checkPattern;
	}

}
