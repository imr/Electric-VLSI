/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EdgeH.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
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
package com.sun.electric.technology;

public class EdgeH
{
	/** The multiplier (scales the width by this amount). */	private double multiplier;
	/** The adder (adds this amount to the scaled width). */	private double adder;

	public EdgeH(double multiplier, double adder)
	{
		this.multiplier = multiplier;
		this.adder = adder;
	}

	/** Defines the left edge of a primitive. */
	public static final EdgeH LEFTEDGE = new EdgeH(-0.5, 0.0);
	/** Defines the right edge of a primitive. */
	public static final EdgeH RIGHTEDGE = new EdgeH(0.5, 0.0);
	/** Defines the X center of a primitive. */
	public static final EdgeH CENTER = new EdgeH(0.0, 0.0);

	/** Returns the multiplier (scales the width by this amount). */
	public double getMultiplier() { return multiplier; }
	/** Returns the adder (adds this amount to the scaled width). */
	public double getAdder() { return adder; }

	/** In from the left edge by this amount. */
	public static EdgeH fromLeft(double amt)
	{
		return new EdgeH(-0.5, amt);
	}

	/** In from the right edge by this amount. */
	public static EdgeH fromRight(double amt)
	{
		return new EdgeH(0.5, -amt);
	}

	/** This amount from the center. */
	public static EdgeH fromCenter(double amt)
	{
		return new EdgeH(0.0, amt);
	}
}
