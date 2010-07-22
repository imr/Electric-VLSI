/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Gridpoint.java
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

class Gridpoint implements Comparable<Gridpoint> {
	private double x, y; // x, y coordinates at grid
	private int z; // which layer
	private Gridpoint prev;

	private Rating rating;

	Gridpoint(double x, double y, int z, Gridpoint p) {
		this.prev = p;
		this.x = x;
		this.y = y;
		this.z = z;
		this.rating = new Rating();
	}

	/**
	 * Method that is called to sort the Gridpoint objects by their rating.
	 */
	public int compareTo(Gridpoint point) {

		return this.rating.compareTo(point.rating);
		// int thisRating = this.rating.getRating();
		// int otherRating = point.getRating().getRating();
		// if (thisRating < otherRating)
		// return -1;
		// if (thisRating > otherRating)
		// return 1;
		// return 0;
	}

	public double getX() {
		return this.x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return this.y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public int getZ() {
		return this.z;
	}

	public void setZ(int z) {
		this.z = z;
	}

	public Rating getRating() {
		return this.rating;
	}
	public Gridpoint getPrev(){
		return this.prev;
	}
}
