/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: FlagSet.java
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
package com.sun.electric.database.variable;

/**
 * The FlagSet class is used to allocate bits in various other objects for marking.
 * Many Electric objects have a "flag bits" field that can be set with bits.
 * To avoid conflicts of use of these bits, code that wants to use a bit must allocate
 * that bit using a FlagSet object.  When done using the bit, it must be freed.
 */
public class FlagSet
{
	/** the bits covered by this flagSet */			private int mask;
	/** the bits not covered by this flagSet */		private int unmask;
	/** the shift of the bits in this flagSet */	private int shift;

	/**
	 * The Generator class has a bit mask of those flag bits that are in use.
	 */
	public static class Generator
	{
		/** used to request flag bit sets */		private int flagBitsUsed = 0;
	}

	private FlagSet() {}

	/**
	 * Routine to create a FlagSet object which can be used to mark flag bits.
	 * @param fg the generator object for the class on which bits are desired.
	 * @param numBits the number of bits needed for marking.
	 * @return a FlagSet object that can be used to set bits on that object.
	 */
	public static FlagSet getFlagSet(Generator fg, int numBits)
	{
		// construct a mask of the appropriate width
		int mask = 0;
		for(int i=0; i<numBits; i++) mask |= 1 << i;

		// see if these bits fit in the flag word
		int shift;
		for(shift=0; shift<=32-numBits; shift++)
			if ((fg.flagBitsUsed & (mask << shift)) == 0) break;
		if (shift > 32-numBits)
		{
			System.out.println("Error: ran out of flag bits");
			return null;
		}

		FlagSet fs = new FlagSet();
		fs.shift = shift;
		fs.mask = mask << shift;
		fs.unmask = ~mask;
		fg.flagBitsUsed |= fs.mask;
		return fs;
	}

	/**
	 * Routine to return the mask bits for this FlagSet.
	 * The mask bits show which bits in the flag word are being used.
	 * @return the mask bits for this FlagSet.
	 */
	public int getMask()   { return mask; }

	/**
	 * Routine to return the unmask bits for this FlagSet.
	 * The mask bits show which bits in the flag word are not being used.
	 * @return the unmask bits for this FlagSet.
	 */
	public int getUnmask() { return unmask; }

	/**
	 * Routine to return the shift amount for this FlagSet.
	 * The shift amount is the amount to right-shift the mask bits to make an integer of them.
	 * @return the shift amount for this FlagSet.
	 */
	public int getShift()  { return shift; }

	/**
	 * Routine to free a FlagSet object and release the marking bits.
	 * @param fg the generator object for the class on which bits are desired.
	 */
	public void freeFlagSet(Generator fg)
	{
		fg.flagBitsUsed &= unmask;
	}
}
