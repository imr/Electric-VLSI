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

/**
 * An EdgeH is a scalable X coordinate that converts a NodeInst bounds to a location inside of that NodeInst.
 * It consists of two numbers: a <I>multiplier</I> and an <I>adder</I>.
 * The resulting location starts at the center of the NodeInst,
 * adds the NodeInst width times the multiplier,
 * adds the adder.
 * <P>
 * For example, the center of the NodeInst simply has multiplier = 0 and adder = 0.
 * The left edge of the NodeInst has multiplier = -0.5 and adder = 0.
 * The point that is 2 left of the right edge has multiplier = 0.5 and adder = -2.
 * The point that is 3 right of the center has multiplier = 0 and adder = 3.
 */
public class EdgeH
{
	/** The multiplier (scales the width by this amount). */	private double multiplier;
	/** The adder (adds this amount to the scaled width). */	private double adder;

	/**
	 * Constructs an <CODE>EdgeH</CODE> with the specified values.
	 * @param multiplier is the multiplier to store in the EdgeV.
	 * @param adder is the adder to store in the EdgeV.
	 */
	public EdgeH(double multiplier, double adder)
	{
		this.multiplier = multiplier;
		this.adder = adder;
	}

	/**
	 * Returns the multiplier.
	 * This is the amount to scale a NodeInst width.
	 * @return the multiplier.
	 */
	public double getMultiplier() { return multiplier; }

	/**
	 * Sets the multiplier.
	 * This is the amount to scale a NodeInst width.
	 * @param multiplier the new multiplier.
	 */
	public void setMultiplier(double multiplier) { this.multiplier = multiplier; }

	/**
	 * Returns the adder.
	 * This is the amount to add to a NodeInst width.
	 * @return the adder.
	 */
	public double getAdder() { return adder; }

	/**
	 * Sets the adder.
	 * This is the amount to add to a NodeInst width.
	 * @param adder the new adder.
	 * @return true if original value was modified
	 */
	public boolean setAdder(double adder)
	{
		if (this.adder != adder)
		{
			this.adder = adder;
			return true;
		}
		return false;
	}

	/**
	 * Describes a position that is in from the left by a specified amount.
	 * @param amt the amount to inset from the left of a NodeInst.
	 */
	public static EdgeH fromLeft(double amt)
	{
		return new EdgeH(-0.5, amt);
	}

	/**
	 * Describes a position that is in from the right by a specified amount.
	 * @param amt the amount to inset from the right of a NodeInst.
	 */
	public static EdgeH fromRight(double amt)
	{
		return new EdgeH(0.5, -amt);
	}

	/**
	 * Describes a position that is away from the center by a specified amount.
	 * @param amt the amount to move away from the center of the NodeInst.
	 */
	public static EdgeH fromCenter(double amt)
	{
		return new EdgeH(0.0, amt);
	}

	/**
	 * Creates a position that describes the left edge of the NodeInst.
	 * @return a position that describes the left edge of the NodeInst.
	 */
	public static EdgeH makeLeftEdge() { return fromLeft(0); }

	/**
	 * Creates a position that describes the right edge of the NodeInst.
	 * @return a position that describes the right edge of the NodeInst.
	 */
	public static EdgeH makeRightEdge() { return fromRight(0); }

	/**
	 * Creates a position that describes the center of the NodeInst.
	 * @return a position that describes the center of the NodeInst.
	 */
	public static EdgeH makeCenter() { return fromCenter(0); }

	/**
	 * Returns a printable version of this EdgeH.
	 * @return a printable version of this EdgeH.
	 */
	public String toString()
	{
		return "EdgeH("+multiplier+","+adder+")";
	}
}
