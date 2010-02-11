/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Force2D.java
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
package com.sun.electric.tool.placement.forceDirected2.forceDirected.util;

/**
 * Parallel Placement
 * 
 * Two dimensional force
 */
public class Force2D {

	private double x;
	private double y;

	/**
	 * Default constructor
	 */
	public Force2D() {
		this.x = 0;
		this.y = 0;
	}

	/**
	 * Constructor
	 * 
	 * @param x
	 *            : force in x direction
	 * @param y
	 *            : force in y direction
	 */
	public Force2D(double x, double y) {
		this.x = x;
		this.y = y;
	}

	/**
	 * 
	 * @param force
	 */
	public Force2D add(Force2D force) {
		Force2D tmp = new Force2D();
		tmp.x = force.getX() + this.x;
		tmp.y = force.getY() + this.y;

		return tmp;
	}

	/**
	 * 
	 * @return sqrt(x^2 + y^2) length of a vector
	 */
	public double getLength() {
		// sqrt(x^2 + y^2) length of a vector
		return Math.sqrt(this.x * this.x + this.y * this.y);
	}

	/**
	 */
	public double getX() {
		return this.x;
	}

	/**
	 */
	public double getY() {
		return this.y;
	}

	public Force2D mult(double weight) {
		this.x *= weight;
		this.y *= weight;
		return this;
	}

	/**
	 * 
	 * @param x
	 */
	public void setX(double x) {
		this.x = x;
	}

	/**
	 * 
	 * @param y
	 */
	public void setY(double y) {
		this.y = y;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Force = (");
		builder.append(this.x);
		builder.append(", ");
		builder.append(this.y);
		builder.append(")");
		return builder.toString();
	}

}
