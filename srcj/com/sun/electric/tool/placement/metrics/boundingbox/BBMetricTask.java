/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BBMetric.java
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
package com.sun.electric.tool.placement.metrics.boundingbox;

import com.sun.electric.tool.placement.PlacementFrame.PlacementNetwork;
import com.sun.electric.tool.placement.PlacementFrame.PlacementNode;
import com.sun.electric.tool.placement.PlacementFrame.PlacementPort;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange;
import com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange1D;
import com.sun.electric.tool.util.concurrent.patterns.PReduceJob.PReduceTask;

import java.awt.geom.Point2D;
import java.util.List;

/**
 * Parallel Placement
 * 
 * Estimate wirelength using the bounding box metric
 */
public class BBMetricTask extends PReduceTask<Double> {

	List<PlacementNode> nodesToPlace;
	List<PlacementNetwork> allNetworks;
	private Double sum = 0.0;

	public BBMetricTask(List<PlacementNode> nodesToPlace, List<PlacementNetwork> allNetworks) {
		this.nodesToPlace = nodesToPlace;
		this.allNetworks = allNetworks;
	}

	private double compute(PlacementNetwork net) {
		List<PlacementPort> portsOnNet = net.getPortsOnNet();

		double leftmost = Double.MAX_VALUE;
		double rightmost = -Double.MAX_VALUE;
		double uppermost = -Double.MAX_VALUE;
		double undermost = Double.MAX_VALUE;

		for (PlacementPort port : portsOnNet) {
			Point2D.Double position = this.getPortPosition(port);
			if (position.getX() < leftmost) {
				leftmost = position.getX();
			}
			if (position.getX() > rightmost) {
				rightmost = position.getX();
			}
			if (position.getY() > uppermost) {
				uppermost = position.getY();
			}
			if (position.getY() < undermost) {
				undermost = position.getY();
			}
		}

		return (rightmost - leftmost) + (uppermost - undermost);

	}

	private Point2D.Double getPortPosition(PlacementPort port) {
		double x = port.getRotatedOffX() + port.getPlacementNode().getPlacementX();
		double y = port.getRotatedOffY() + port.getPlacementNode().getPlacementY();

		return new Point2D.Double(x, y);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sun.electric.tool.util.concurrent.patterns.PReduceJob.PReduceTask
	 * #reduce
	 * (com.sun.electric.tool.util.concurrent.patterns.PReduceJob.PReduceTask)
	 */
	@Override
	public synchronized Double reduce(PReduceTask<Double> other) {
		BBMetricTask bbOther = (BBMetricTask) other;

		if (!this.equals(other)) {
			this.sum += bbOther.sum;
		}

		return this.sum;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sun.electric.tool.util.concurrent.patterns.PForJob.PForTask#execute
	 * (com.sun.electric.tool.util.concurrent.patterns.PForJob.BlockedRange)
	 */
	@Override
	public void execute(BlockedRange range) {
		BlockedRange1D tmpRange = (BlockedRange1D) range;

		for (int i = tmpRange.start(); i < tmpRange.end(); i++) {
			sum = sum + this.compute(allNetworks.get(i));
		}
	}
}
