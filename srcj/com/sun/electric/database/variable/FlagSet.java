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
 * The FlagSet class is used to allocate "marking" bits in other objects.
 * <P>
 * When examining circuitry, it is often desirable to "mark" an object (Cell, NodeInst, etc.)
 * These Electric objects have a "flag bits" field that can be set, cleared, and tested.
 * The problem is that you never know when another class may also need to do marking,
 * and those marks may conflict with each other.
 * To avoid these conflicts, any code that wants to use flag-bits must allocate
 * those bits using a FlagSet object.  When done using bits, they must be freed.
 * <P>
 * These steps must occur:
 * <UL>
 * <LI>Any class that wants to be marked must have a "private int flagBits" declaration.
 * <LI>The class must also have a "private static FlagSet.Generator flagGenerator = new FlagSet.Generator()"
 * This is static (only one for the entire class) and it remembers which bits are allocated.
 * <LI>To allocate bits, call "FlagSet myFlagSet = FlagSet.getFlagSet(flagGenerator, numBits);" which takes the
 * static "flagGenerator" for the class and the number of bits desired.  The returned "FlagSet" object
 * can be used to do marking and testing as follows:
 * <UL>
 * <LI>To set the bits, do "flagBits = flagBits | myFlagSet.getMask();"
 * <LI>To clear the bits, do "flagBits = flagBits & myFlagSet.getUnMask();"
 * <LI>To test the bits, test "flagBits & myFlagSet.getMask()"
 * <LI>To be able to shift these bits to the proper position, use "myFlagSet.getShift()"
 * </UL>
 * <LI>When done, free the allocated bits with a "myFlagSet.freeFlagSet()"
 * </UL>
 */
public class FlagSet
{
	/** the bits covered by this flagSet */			private int mask;
	/** the bits not covered by this flagSet */		private int unmask;
	/** the shift of the bits in this flagSet */	private int shift;
	private Generator generator;

	/**
	 * The Generator class has a bit mask of those flag bits that are in use.
	 */
	public static class Generator
	{
		/** the name of the object that this generates for. */	private String objectName;
		/** used to request flag bit sets */					private int flagBitsUsed = 0;

		public Generator(String objectName) { this.objectName = objectName; }
		public String getObjectName() { return objectName; }
	}

	private FlagSet() {}

	/**
	 * Method to create a FlagSet object which can be used to mark flag bits.
	 * @param generator the generator object for the class on which bits are desired.
	 * @param numBits the number of bits needed for marking.
	 * @return a FlagSet object that can be used to set bits on that object.
	 */
	public static FlagSet getFlagSet(Generator generator, int numBits)
	{
		// construct a mask of the appropriate width
		int mask = 0;
		for(int i=0; i<numBits; i++) mask |= 1 << i;

		// see if these bits fit in the flag word
		int shift;
		for(shift=0; shift<=32-numBits; shift++)
			if ((generator.flagBitsUsed & (mask << shift)) == 0) break;
		if (shift > 32-numBits)
		{
			System.out.println("Error: ran out of flag bits in " + generator.getObjectName());
			return null;
		}

		FlagSet fs = new FlagSet();
		fs.shift = shift;
		fs.mask = mask << shift;
		fs.unmask = ~fs.mask;
		generator.flagBitsUsed |= fs.mask;
		fs.generator = generator;
		return fs;
	}

	/**
	 * Method to return the mask bits for this FlagSet.
	 * The mask bits show which bits in the flag word are being used.
	 * @return the mask bits for this FlagSet.
	 */
	public int getMask()   { return mask; }

	/**
	 * Method to return the unmask bits for this FlagSet.
	 * The mask bits show which bits in the flag word are not being used.
	 * @return the unmask bits for this FlagSet.
	 */
	public int getUnmask() { return unmask; }

	/**
	 * Method to return the shift amount for this FlagSet.
	 * The shift amount is the amount to right-shift the mask bits to make an integer of them.
	 * @return the shift amount for this FlagSet.
	 */
	public int getShift()  { return shift; }

	/**
	 * Method to free the marking bits associated with this FlagSet object.
	 */
	public void freeFlagSet()
	{
		generator.flagBitsUsed &= unmask;
	}
}
