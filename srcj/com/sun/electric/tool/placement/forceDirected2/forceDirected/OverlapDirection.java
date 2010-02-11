/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: OverlapDirection.java
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
package com.sun.electric.tool.placement.forceDirected2.forceDirected;

/**
 * Parallel Placement
 * 
 * The direction of an overlapping
 */
public enum OverlapDirection {

	north, northeast, east, southeast, south, southwest, west, northwest, none;

	public static boolean isEast(OverlapDirection direction) {
		boolean result = false;

		if ((direction == OverlapDirection.east) || (direction == OverlapDirection.northeast) || (direction == OverlapDirection.southeast)) {
			result = true;
		}

		return result;
	}

	public static boolean isNorth(OverlapDirection direction) {
		boolean result = false;

		if ((direction == OverlapDirection.north) || (direction == OverlapDirection.northeast) || (direction == OverlapDirection.northwest)) {
			result = true;
		}

		return result;
	}

	public static boolean isSouth(OverlapDirection direction) {
		boolean result = false;

		if ((direction == OverlapDirection.south) || (direction == OverlapDirection.southeast) || (direction == OverlapDirection.southwest)) {
			result = true;
		}

		return result;
	}

	public static boolean isWest(OverlapDirection direction) {
		boolean result = false;

		if ((direction == OverlapDirection.west) || (direction == OverlapDirection.northwest) || (direction == OverlapDirection.southwest)) {
			result = true;
		}

		return result;
	}

	/**
	 * mix directions (e.g. north and east --> northeast, south and west -->
	 * southwest)
	 * 
	 * @param dir1
	 * @param dir2
	 */
	public static OverlapDirection mixDirections(OverlapDirection dir1, OverlapDirection dir2) {

		if (dir1 == dir2) {
			return dir1;
		}

		if (dir1 == none) {
			return dir2;
		}

		if (dir2 == none) {
			return dir1;
		}

		if ((dir1 == north) || (dir2 == north)) {
			if ((dir2 == west) || (dir1 == west)) {
				return northwest;
			} else {
				return northeast;
			}
		} else {
			if ((dir2 == west) || (dir1 == west)) {
				return southwest;
			} else {
				return southeast;
			}
		}
	}

}
