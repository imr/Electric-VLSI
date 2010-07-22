/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Rating.java
 * Written by: Dennis Appelt, Sven Janko (Team 2)
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
package com.sun.electric.tool.routing.experimentalLeeMoore3;

public class Rating implements Comparable<Rating> {

	private int rating;

	//private int shiftsInDirection;
	
	// rates a grid point regarding its distance from the start point
	private int distance;
	
	// rates a grid point regarding crossed blockages
	//private int crossings;
	
	// rates a grid point regarding the direction from start point to finish
	// point
	private int direction;
	
	private int outOfBounds;

	public int getOutOfBounds() {
		return outOfBounds;
	}

	public void setOutOfBounds(int outOfBounds) {
		this.outOfBounds = outOfBounds;
	}

	public Rating() {
	}

	public void setDistance(int dist) {
		this.distance = dist;
	}

	public int getDistance() {
		return distance;
	}

//	public void setCrossings(int crossings) {
//		this.crossings = crossings;
//	}

//	public int getCrossings() {
//		return crossings;
//	}

	public void setDirection(int direction) {
		this.direction = direction;
	}

	public int getDirection() {
		return direction;
	}

//	public int getShiftsInDirection() {
//		return shiftsInDirection;
//	}

//	public void setShiftsInDirection(int shiftsInDirection) {
//		this.shiftsInDirection = shiftsInDirection;
//	}

	public final static int distanceMalus = 1;
//	public final static int crossingMalus = 5;
	public final static int directionMalus = 4;
//	public final static int shiftInDirectionMalus = 1;
	public final static int outOfBoundsMalus = RoutingFrameLeeMoore.GLOBALDETAILEDROUTING ? 10000 : 20;
	public void calcRating() {
		this.rating = distance * distanceMalus 
//				+ crossings * crossingMalus
				+ direction * directionMalus 
//				+ shiftsInDirection	* shiftInDirectionMalus 
				+ outOfBounds * outOfBoundsMalus;
	}

	public int getRating() {
		return this.rating;
	}

	public int compareTo(Rating o) {
		int otherRating = o.getRating();

		return (this.rating < otherRating) ? -1
				: (this.rating == otherRating) ? 0 : 1;
	}
}

