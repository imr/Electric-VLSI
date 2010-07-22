/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Path.java
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

import java.awt.geom.Point2D;

import com.sun.electric.tool.routing.RoutingFrame.RoutingSegment;

/** 
 * Contains information to route the contained segment and later contains the routing points
 * 
 * @author Christian Jülg
 * @author Jonas Thedering
 */
public class Path {

	// will be required to actually use the computed path later
	RoutingSegment segment;
	
	// Bounding box used to calculate overlap
	int minX, maxX, minY, maxY;
	
	int startX, finishX, startY, finishY;
	int[] startZ = null, finishZ = null;
	boolean startRight, startAbove, finishRight, finishAbove;

	// the result of the routing
	int[] nodesX, nodesY, nodesZ;
	int totalCost;
	
	// set by Master, if true Worker should ignore this Path
	boolean pathDone = false;
	
	// set by EndpointMarker or Master
	boolean pathUnroutable = false;

	public Path(RoutingSegment rs, double dispX, double dispY, double scalingFactor) {
		this.segment = rs;
		
		Point2D startLoc = segment.getStartEnd().getLocation();
		startX = (int) Math.floor((startLoc.getX() + dispX) / scalingFactor);
		startY = (int) Math.floor((startLoc.getY() + dispY) / scalingFactor);

		Point2D finishLoc = segment.getFinishEnd().getLocation();
		finishX = (int) Math.floor((finishLoc.getX() + dispX) / scalingFactor);
		finishY = (int) Math.floor((finishLoc.getY() + dispY) / scalingFactor);
		
		startRight = ((startLoc.getX() + dispX) % scalingFactor) >= scalingFactor / 2;
		startAbove = ((startLoc.getY() + dispY) % scalingFactor) >= scalingFactor / 2;
		finishRight = ((finishLoc.getX() + dispX) % scalingFactor) >= scalingFactor / 2;
		finishAbove = ((finishLoc.getY() + dispY) % scalingFactor) >= scalingFactor / 2;
	}

	/**
	 * should be called by worker
	 */
	public void initialize(){
		nodesX = null;
		nodesY = null;
		nodesZ = null;
		totalCost = -1;
	}
	
	/** Updates the bounding box according to the current start/finish position */
	public void updateBoundingBox() {
		minX = Math.min(startX, finishX);
		maxX = Math.max(startX, finishX);
		minY = Math.min(startY, finishY);
		maxY = Math.max(startY, finishY);
	}
	
	/** @return if this path's bounding box overlaps the other path's */
	public boolean overlaps(Path other) {
		return ((minX >= other.minX && minX <= other.maxX) || (other.minX >= minX && other.minX <= maxX))
			&& ((minY >= other.minY && minY <= other.maxY) || (other.minY >= minY && other.minY <= maxY));
	}
	
	/** @return how much area of this path's bounding box overlaps the other path's */
	public int getOverlapAmount(Path other) {
		int ox = 0;
		if(minX >= other.minX && minX <= other.maxX)
			ox = Math.min(maxX, other.maxX) - minX;
		else if(other.minX >= minX && other.minX <= maxX)
			ox = Math.min(maxX, other.maxX) - other.minX;
		else
			return 0;
		
		int oy = 0;
		if(minY >= other.minY && minY <= other.maxY)
			oy = Math.min(maxY, other.maxY) - minY;
		else if(other.minY >= minY && other.minY <= maxY)
			oy = Math.min(maxY, other.maxY) - other.minY;
		else
			return 0;
		
		return ox * oy;
	}
	
	/** @return an estimate for the length of the final routing */
	public int getLengthEstimate() {
		Point2D start = segment.getStartEnd().getLocation();
		Point2D finish = segment.getFinishEnd().getLocation();
		int xDiff = (int) Math.abs(start.getX()-finish.getX());
		int yDiff = (int) Math.abs(start.getY()-finish.getY());
		return xDiff + yDiff;
	}
}
