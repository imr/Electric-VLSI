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

public class FlagSet
{
	/** the bits covered by this flagSet */			private int mask;
	/** the bits not covered by this flagSet */		private int unmask;
	/** the shift of the bits in this flagSet */	private int shift;

	public static class Generator
	{
		/** used to request flag bit sets */		private static int flagBitsUsed = 0;
	}

	private FlagSet() {}

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
	public int getMask()   { return mask; }
	public int getUnmask() { return unmask; }
	public int getShift()  { return shift; }

	public void freeFlagSet(Generator fg)
	{
		fg.flagBitsUsed &= unmask;
	}
}
