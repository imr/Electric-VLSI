/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DataGrid.java
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

import java.util.HashMap;
import java.util.Map;

/**
 * This class saves the rating for each coordinate. These ratings are the
 * foundation for applying the Lee-Moore algorithm.
 */
public class DataGrid extends HashMap<Double, Map<Double, Rating>> {

	private static final long serialVersionUID = -3003345240541850936L;

	public DataGrid() {
	}

	/**
	 * Method to see if a particular point has been visited.
	 * 
	 * @param x
	 *            The x coordinate.
	 * @param y
	 *            The y coordinate.
	 * @param z
	 *            The metal layer.
	 * @return true, if point has been visited, false otherwise.
	 */
	public boolean isPointVisited(double x, double y) {
		Map<Double, Rating> row = this.get(new Double(y));
		if (row == null)
			return false;
		Rating r = row.get(new Double(x));
		if (r == null)
			return false;
		return true;
	}

	/**
	 * Method to add a point to the map of visited points.
	 * 
	 * @param x
	 *            The x coordinate.
	 * @param y
	 *            The y coordinate.
	 * @param z
	 *            The metal layer.
	 * @param r
	 *            The rating of the point.
	 */
	public void visit(double x, double y, Rating r) {
		Double Y = new Double(y);
		Map<Double, Rating> row = this.get(Y);
		if (row == null) {
			row = new HashMap<Double, Rating>();
			this.put(Y, row);
		}
		row.put(new Double(x), r);
	}

	/**
	 * Method to get the rating of a specific grid point.
	 * 
	 * @param x
	 *            The x coordinate.
	 * @param y
	 *            The y coordinate.
	 * @return The rating of the point.
	 */
	public Rating getRating(double x, double y) {
		Map<Double, Rating> row = this.get(new Double(y));
		if (row == null)
			return null;
		return row.get(new Double(x));
	}
}