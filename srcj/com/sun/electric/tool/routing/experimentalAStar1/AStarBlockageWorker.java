/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AStarBlockageWorker.java
 * Written by: Christian Julg, Jonas Thedering (Team 1)
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
package com.sun.electric.tool.routing.experimentalAStar1;

import java.awt.geom.Rectangle2D;
import java.util.concurrent.CountDownLatch;

import com.sun.electric.tool.routing.RoutingFrame.RoutingGeometry;

/** 
 * Sets the X marks on the map for the given blockage.
 * 
 * @author Christian Jülg
 * @author Jonas Thedering
 */
public class AStarBlockageWorker implements Runnable {
	
	private boolean DEBUG = false;

	private CountDownLatch latch;
	private RoutingGeometry blockage;
	private Map map;

	public AStarBlockageWorker(Map map, RoutingGeometry blockage,
			CountDownLatch latch) {
		this.blockage = blockage;
		this.latch = latch;
		this.map = map;
		
		DEBUG &= AStarRoutingFrame.getInstance().isOutputEnabled(); 
	}

	public void run() {

		Rectangle2D rec = blockage.getBounds();
		double scalingFactor = map.getScalingFactor();

		int minX, maxX, minY, maxY;
		minX = (int) Math.floor((rec.getMinX() + map.getDispX()) / scalingFactor + 0.5);
		maxX = (int) Math.ceil((rec.getMaxX() + map.getDispX()) / scalingFactor - 0.5);
		minY = (int) Math.floor((rec.getMinY() + map.getDispY()) / scalingFactor + 0.5);
		maxY = (int) Math.ceil((rec.getMaxY() + map.getDispY()) / scalingFactor - 0.5);

		if (DEBUG) {
			System.out.printf("AStarBlockageWorker: block: minx: %f, miny: %f, maxx: %f, maxy: %f\n",
						minX, minY, maxX, maxY);
		}

		int layer = blockage.getLayer().getMetalNumber() - 1;
		
		for (int x = minX; x < maxX; x++) {
			for (int y = minY; y < maxY; y++) {
				map.setStatus(x, y, layer, Map.X);	
			}
		}

		latch.countDown();
	}
}
