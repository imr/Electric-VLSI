/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ShiftInDirectionRatingFunction.java
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

import java.awt.geom.Rectangle2D;

import com.sun.electric.database.topology.RTNode;

public class ShiftInDirectionRatingFunction implements RatingFunction {

	public void doRating(Gridpoint curPoint, Gridpoint prevPoint,
			double xFinish, double yFinish, RTNode blockings, Rectangle2D cellBounds) {
		
//		Gridpoint prevPrevPoint = prevPoint.getPrev();
//		if (prevPrevPoint == null)
//			return;
//		
//		if (curPoint.getX() != prevPrevPoint.getX()
//				&& curPoint.getY() != prevPrevPoint.getY()) {
//			curPoint.getRating().setShiftsInDirection(
//					prevPoint.getRating().getShiftsInDirection() + 1);
//		} else {
//			curPoint.getRating().setShiftsInDirection(
//					prevPoint.getRating().getShiftsInDirection());
//		}
	}

}