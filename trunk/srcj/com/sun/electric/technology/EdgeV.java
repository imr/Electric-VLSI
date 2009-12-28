/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EdgeV.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.technology;

import com.sun.electric.database.geometry.DBMath;

import java.io.Serializable;

/**
 * An EdgeV is a scalable Y coordinate that converts a NodeInst bounds to a location inside of that NodeInst.
 * It consists of two numbers: a <I>multiplier</I> and an <I>adder</I>.
 * The resulting location starts at the center of the NodeInst,
 * adds the NodeInst height times the multiplier,
 * adds the adder.
 * <P>
 * For example, the center of the NodeInst simply has multiplier = 0 and adder = 0.
 * The bottom of the NodeInst has multiplier = -0.5 and adder = 0.
 * The point that is 2 below the top has multiplier = 0.5 and adder = -2.
 * The point that is 3 above the center has multiplier = 0 and adder = 3.
 */
public class EdgeV implements Serializable
{
	/** The multiplier (scales the height by this amount). */	private final double multiplier;
	/** The adder (adds this amount to the scaled height). */	private final double adder;
	/** The adder (adds this amount to the scaled width) in grid units. */	private final long gridAdder;

	/**
	 * Constructs an <CODE>EdgeV</CODE> with the specified values.
	 * @param multiplier is the multiplier to store in the EdgeV.
	 * @param adder is the adder to store in the EdgeV.
	 */
	public EdgeV(double multiplier, double adder)
	{
        this.multiplier = multiplier;
        gridAdder = DBMath.lambdaToGrid(adder);
        this.adder = DBMath.gridToLambda(gridAdder);
	}

    /**
     * Compare to another EdgeV
     * @param other the other EdgeV to compare.
     * @return true if the two have the same values.
     */
    public boolean equals(Object other)
    {
    	if (!(other instanceof EdgeV)) return false;
    	EdgeV otherE = (EdgeV)other;
    	return multiplier == otherE.multiplier && adder == otherE.adder;
    }

    /**
	 * Returns the multiplier.
	 * This is the amount to scale a NodeInst height.
	 * @return the multiplier.
	 */
	public double getMultiplier() { return multiplier; }

	/**
	 * Returns the adder.
	 * This is the amount to add to a NodeInst height.
	 * @return the adder.
	 */
	public double getAdder() { return adder; }

	/**
	 * Returns the adder in grid units.
	 * This is the amount to add to a NodeInst height.
	 * @return the adder.
	 */
	public long getGridAdder() { return gridAdder; }

	/**
	 * Returns EdgeV with the new adder.
	 * @param adder the new adder.
	 * @return EdgeV with the new adder
	 */
	public EdgeV withAdder(double adder)
	{
		if (this.adder == adder) return this;
        return new EdgeV(this.multiplier, adder);
	}

	/**
	 * Describes a position that is in from the top by a specified amount.
	 * @param amt the amount to inset from the top of a NodeInst.
	 */
	public static EdgeV fromTop(double amt)
	{
		return new EdgeV(0.5, -amt);
	}

	/**
	 * Describes a position that is in from the bottom by a specified amount.
	 * @param amt the amount to inset from the bottom of a NodeInst.
	 */
	public static EdgeV fromBottom(double amt)
	{
		return new EdgeV(-0.5, amt);
	}

	/**
	 * Describes a position that is away from the center by a specified amount.
	 * @param amt the amount to move away from the center of the NodeInst.
	 */
	public static EdgeV fromCenter(double amt)
	{
		return new EdgeV(0.0, amt);
	}

	/**
	 * Creates a position that describes the bottom edge of the NodeInst.
	 * @return a position that describes the bottom edge of the NodeInst.
	 */
	public static EdgeV makeBottomEdge() { return fromBottom(0); }

	/**
	 * Creates a position that describes the top edge of the NodeInst.
	 * @return a position that describes the top edge of the NodeInst.
	 */
	public static EdgeV makeTopEdge() { return fromTop(0); }

	/**
	 * Creates a position that describes the center of the NodeInst.
	 * @return a position that describes the center of the NodeInst.
	 */
	public static EdgeV makeCenter() { return fromCenter(0); }

	/**
	 * Returns a printable version of this EdgeV.
	 * @return a printable version of this EdgeV.
	 */
	public String toString()
	{
		return "EdgeV("+multiplier+","+adder+")";
	}
}
