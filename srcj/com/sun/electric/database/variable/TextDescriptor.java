/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TextDescriptor.java
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
 * This class describes how variable text appears.
 */
public class TextDescriptor
{
	private static final int VTPOSITION =               017;		/* 0: position of text relative to point */
	private static final int VTPOSCENT =                  0;		/* 0:   text centered about point */
	private static final int VTPOSUP =                    1;		/* 0:   text centered above point */
	private static final int VTPOSDOWN =                  2;		/* 0:   text centered below point */
	private static final int VTPOSLEFT =                  3;		/* 0:   text centered to left of point */
	private static final int VTPOSRIGHT =                 4;		/* 0:   text centered to right of point */
	private static final int VTPOSUPLEFT =                5;		/* 0:   text centered to upper-left of point */
	private static final int VTPOSUPRIGHT =               6;		/* 0:   text centered to upper-right of point */
	private static final int VTPOSDOWNLEFT =              7;		/* 0:   text centered to lower-left of point */
	private static final int VTPOSDOWNRIGHT =             8;		/* 0:   text centered to lower-right of point */
	private static final int VTPOSBOXED =                 9;		/* 0:   text centered and limited to object size */
	private static final int VTDISPLAYPART =            060;		/* 0: bits telling what to display */
	private static final int VTDISPLAYVALUE =             0;		/* 0:   display value */
	private static final int VTDISPLAYNAMEVALUE =       040;		/* 0:   display name and value */
	private static final int VTDISPLAYNAMEVALINH =      020;		/* 0:   display name, value, 1-level inherit */
	private static final int VTDISPLAYNAMEVALINHALL =   060;		/* 0:   display name, value, any inherit */
	private static final int VTITALIC =                0100;		/* 0: set for italic text */
	private static final int VTBOLD =                  0200;		/* 0: set for bold text */
	private static final int VTUNDERLINE =             0400;		/* 0: set for underline text */
	private static final int VTISPARAMETER =          01000;		/* 0: attribute is parameter (nodeinst only) */
	private static final int VTINTERIOR =             02000;		/* 0: text only appears inside cell */
	private static final int VTINHERIT =              04000;		/* 0: set to inherit value from proto to inst */
	private static final int VTXOFF =              07770000;		/* 0: X offset of text */
	private static final int VTXOFFSH =                  12;		/* 0: right shift of VTXOFF */
	private static final int VTXOFFNEG =          010000000;		/* 0: set if X offset is negative */
	private static final int VTYOFF =          017760000000;		/* 0: Y offset of text */
	private static final int VTYOFFSH =                  22;		/* 0: right shift of VTYOFF */
	private static final int VTYOFFNEG =       020000000000;		/* 0: set if Y offset is negative */
	private static final int VTOFFMASKWID =               9;		/* 0: Width of VTXOFF and VTYOFF */
	private static final int VTSIZE =                077777;		/* 1: size of text */
	private static final int VTSIZESH =                   0;		/* 1: right shift of VTSIZE */
	private static final int VTFACE =             017700000;		/* 1: face of text */
	private static final int VTFACESH =                  15;		/* 1: right shift of VTFACE */
	private static final int VTROTATION =         060000000;		/* 1: rotation of text */
	private static final int VTROTATIONSH =              22;		/* 1: right shift of VTROTATION */
	private static final int VTMAXFACE =                128;		/* 1: maximum value of VTFACE field */
	private static final int VTOFFSCALE =       03700000000;		/* 1: scale of text offset */
	private static final int VTOFFSCALESH =              24;		/* 1: right shift of VTOFFSCALE */
	private static final int VTUNITS =         034000000000;		/* 1: units of text */
	private static final int VTUNITSSH =                 29;		/* 1: right shift of VTUNITS */
	private static final int VTUNITSNONE =                0;		/* 1:   units: none */
	private static final int VTUNITSRES =                 1;		/* 1:   units: resistance */
	private static final int VTUNITSCAP =                 2;		/* 1:   units: capacitance */
	private static final int VTUNITSIND =                 3;		/* 1:   units: inductance */
	private static final int VTUNITSCUR =                 4;		/* 1:   units: current */
	private static final int VTUNITSVOLT =                5;		/* 1:   units: voltage */
	private static final int VTUNITSDIST =                6;		/* 1:   units: distance */
	private static final int VTUNITSTIME =                7;		/* 1:   units: time */


	/**
	 * Position is a typesafe enum class that describes the text position of a variable.
	 */
	public static class Position
	{
		private final String name;
		private final int index;

		private Position(String name, int index)
		{
			this.name = name;
			this.index = index;
		}

		public int getIndex() { return index; }
		public String toString() { return name; }

		/** text centered at point */					public static final Position CENT =      new Position("centered", VTPOSCENT);
		/** text centered above point */				public static final Position UP =        new Position("up", VTPOSUP);
		/** text centered below point */				public static final Position DOWN =      new Position("down", VTPOSDOWN);
		/** text centered to left of point */			public static final Position LEFT =      new Position("left", VTPOSLEFT);
		/** text centered to right of point */			public static final Position RIGHT =     new Position("right", VTPOSRIGHT);
		/** text centered to upper-left of point */		public static final Position UPLEFT =    new Position("up-left", VTPOSUPLEFT);
		/** text centered to upper-right of point */	public static final Position UPRIGHT =   new Position("up-right", VTPOSUPRIGHT);
		/** text centered to lower-left of point */		public static final Position DOWNLEFT =  new Position("down-left", VTPOSDOWNLEFT);
		/** text centered to lower-right of point */	public static final Position DOWNRIGHT = new Position("down-right", VTPOSDOWNRIGHT);
		/** text centered and limited to object size */	public static final Position BOXED =     new Position("boxed", VTPOSBOXED);
	}
	private static final Position [] thePositions = new Position[] {Position.CENT, Position.UP, Position.DOWN,
		Position.LEFT, Position.RIGHT, Position.UPLEFT, Position.UPRIGHT, Position.DOWNLEFT, Position.DOWNRIGHT, Position.BOXED};


	/**
	 * DispPos is a typesafe enum class that describes text's display position on a variable.
	 */
	public static class DispPos
	{
		private final String name;
		private final int index;

		private DispPos(String name, int index)
		{
			this.name = name;
			this.index = index;
		}

		public int getIndex() { return index; }
		public String toString() { return name; }

		/** display value */							public static final DispPos VALUE =         new DispPos("value", VTDISPLAYVALUE);
		/** display name and value */					public static final DispPos NAMEVALUE =     new DispPos("name/value", VTDISPLAYNAMEVALUE);
		/** display name, value, 1-level inherit */		public static final DispPos NAMEVALINH =    new DispPos("name/value/inherit-1", VTDISPLAYNAMEVALINH);
		/** display name, value, any inherit */			public static final DispPos NAMEVALINHALL = new DispPos("name/value/inherit-all", VTDISPLAYNAMEVALINHALL);
	}
	private static final DispPos [] theDispPos = new DispPos[] {DispPos.VALUE, DispPos.NAMEVALUE, DispPos.NAMEVALINH,
		DispPos.NAMEVALINHALL};


	/**
	 * Units is a typesafe enum class that describes text's units on a variable.
	 */
	public static class Units
	{
		private final String name;
		private final int index;

		private Units(String name, int index)
		{
			this.name = name;
			this.index = index;
		}

		public int getIndex() { return index; }
		public String toString() { return name; }

		/** no units */					public static final Units NONE =        new Units("none", VTUNITSNONE);
		/** resistance units */			public static final Units RESISTANCE =  new Units("resistance", VTUNITSRES);
		/** capacitance units */		public static final Units CAPACITANCE = new Units("capacitance", VTUNITSCAP);
		/** inductance units */			public static final Units INDUCTANCE =  new Units("inductance", VTUNITSIND);
		/** current units */			public static final Units CURRENT =     new Units("current", VTUNITSCUR);
		/** voltage units */			public static final Units VOLTAGE =     new Units("voltage", VTUNITSVOLT);
		/** distance units */			public static final Units DISTANCE =    new Units("distance", VTUNITSDIST);
		/** time units */				public static final Units TIME =        new Units("time", VTUNITSTIME);
	}
	private static final Units [] theUnits = new Units[] {Units.NONE, Units.RESISTANCE, Units.CAPACITANCE,
		Units.INDUCTANCE, Units.CURRENT, Units.VOLTAGE, Units.DISTANCE, Units.TIME};


	/** the first word of the text descriptor */		private int descriptor0;
	/** the second word of the text descriptor */		private int descriptor1;

	public TextDescriptor()
	{
	}

	/** routine to set the bits in the text descriptor directly. */
	public void lowLevelSet(int descriptor0, int descriptor1) { this.descriptor0 = descriptor0;   this.descriptor1 = descriptor1; }
	public int lowLevelGet0() { return descriptor0; }
	public int lowLevelGet1() { return descriptor1; }

	/** routine to clear the bits in the text descriptor. */
	public void TDClear() { this.descriptor0 = this.descriptor1 = 0; }

	/** routine to copy this text descriptor to "dest". */
	public void TDCopy(TextDescriptor dest) { dest.lowLevelSet(descriptor0, descriptor1); }

	/** routine to return true if the two text descriptors are different. */
	public boolean TDDiff(TextDescriptor td)
	{
		if (this.descriptor0 != td.descriptor0) return true;
		if (this.descriptor1 != td.descriptor1) return true;
		return false;
	}

	/** routine to return the text position of the text descriptor. */
	public Position TDGetPos() { return thePositions[descriptor0 & VTPOSITION]; }
	/** routine to set the text position of the text descriptor. */
	public void TDSetPos(Position p) {descriptor0 = (descriptor0 & ~VTPOSITION) | p.getIndex(); }

	/** routine to return the text size of the text descriptor. */
	public int TDGetSize() { return (descriptor1 & VTSIZE) >> VTSIZESH; }
	/** routine to set the text size of the text descriptor. */
	public void TDSetSize(int v) {descriptor1 = (descriptor1 & ~VTSIZE) | (v << VTSIZESH); }

	/** routine to return the text font of the text descriptor. */
	public int TDGetFace() { return (descriptor1 & VTFACE) >> VTFACESH; }
	/** routine to set the text font of the text descriptor. */
	public void TDSetFace(int v) {descriptor1 = (descriptor1 & ~VTFACE) | (v << VTFACESH); }

	/** routine to return the text rotation of the text descriptor. */
	public int TDGetRotation() { return (descriptor1 & VTROTATION) >> VTROTATIONSH; }
	/** routine to set the text rotation of the text descriptor. */
	public void TDSetRotation(int v) {descriptor1 = (descriptor1 & ~VTROTATION) | (v << VTROTATIONSH); }

	/** routine to return the text display part of the text descriptor. */
	public DispPos TDGetDispPart() { return theDispPos[descriptor0 & VTDISPLAYPART]; }
	/** routine to set the text display part of the text descriptor. */
	public void TDSetDispPart(DispPos v) {descriptor0 = (descriptor0 & ~VTDISPLAYPART) | v.getIndex(); }

	/** routine to return true if the text descriptor indicates italic text. */
	public boolean TDGetItalic() { return (descriptor0 & VTITALIC) != 0; }
	/** routine to set the text descriptor's italic text factor. */
	public void TDSetItalic(boolean i) {descriptor0 = (descriptor0 & ~VTITALIC) | (i ? VTITALIC : 0); }

	/** routine to return true if the text descriptor indicates bold text. */
	public boolean TDGetBold() { return (descriptor0 & VTBOLD) != 0; }
	/** routine to set the text descriptor's bold text factor. */
	public void TDSetBold(boolean b) {descriptor0 = (descriptor0 & ~VTBOLD) | (b ? VTBOLD : 0); }

	/** routine to return true if the text descriptor indicates underlined text. */
	public boolean TDGetUnderline() { return (descriptor0 & VTUNDERLINE) != 0; }
	/** routine to set the text descriptor's underlined text factor. */
	public void TDSetUnderline(boolean u) {descriptor0 = (descriptor0 & ~VTUNDERLINE) | (u ? VTUNDERLINE : 0); }

	/** routine to return true if the text descriptor indicates interior text (not see at higher-levels of hierarchy). */
	public boolean TDGetInterior() { return (descriptor0 & VTINTERIOR) != 0; }
	/** routine to set the text descriptor's interior text factor (whether seen at higher-levels of hierarchy). */
	public void TDSetInterior(boolean i) {descriptor0 = (descriptor0 & ~VTINTERIOR) | (i ? VTINTERIOR : 0); }

	/** routine to return true if the text descriptor indicates inheritable text (instances copy from prototypes). */
	public boolean TDGetInherit() { return (descriptor0 & VTINHERIT) != 0; }
	/** routine to set the text descriptor's inheritable text factor (whether instances copy from prototypes). */
	public void TDSetInherit(boolean i) {descriptor0 = (descriptor0 & ~VTINHERIT) | (i ? VTINHERIT : 0); }

	/** routine to return true if the text descriptor indicates that this variable is a cell parameter. */
	public boolean TDGetIsParam() { return (descriptor0 & VTISPARAMETER) != 0; }
	/** routine to set the text descriptor's parameter factor. */
	public void TDSetIsParam(boolean i) {descriptor0 = (descriptor0 & ~VTISPARAMETER) | (i ? VTISPARAMETER : 0); }

	/** routine to return the X offset of the text in the text descriptor. */
	public int TDGetXOff()
	{
		int offset = (descriptor0 & VTXOFF) >> VTXOFFSH;
		if ((descriptor0&VTXOFFNEG) != 0) offset = -offset;
		int scale = TDGetOffScale() + 1;
		return(offset * scale);
	}
	/** routine to return the X offset of the text in the text descriptor. */
	public int TDGetYOff()
	{
		int offset = (descriptor0 & VTYOFF) >> VTYOFFSH;
		if ((descriptor0&VTYOFFNEG) != 0) offset = -offset;
		int scale = TDGetOffScale() + 1;
		return(offset * scale);
	}
	/** routine to set the X and Y offset of the text in the text descriptor. */
	public void TDSetOff(int x, int y)
	{
		descriptor0 = descriptor0 & ~(VTXOFF|VTYOFF|VTXOFFNEG|VTYOFFNEG);
		if (x < 0)
		{
			x = -x;
			descriptor0 |= VTXOFFNEG;
		}
		if (y < 0)
		{
			y = -y;
			descriptor0 |= VTYOFFNEG;
		}
		int scale = Math.max(x,y) >> VTOFFMASKWID;
		x /= (scale + 1);
		y /= (scale + 1);
		descriptor0 |= (x << VTXOFFSH);
		descriptor0 |= (y << VTYOFFSH);
		TDSetOffScale(scale);
	}

	/** routine to return the offset scale of the text in the text descriptor. */
	public int TDGetOffScale() { return (descriptor1 & VTOFFSCALE) >> VTOFFSCALE; }
	/** routine to set the offset scale of the text in the text descriptor. */
	public void TDSetOffScale(int v) {descriptor1 = (descriptor1 & ~VTOFFSCALE) | (v << VTOFFSCALESH); }

	/** routine to return the units of the text in the text descriptor. */
	public Units TDGetUnits() { return theUnits[(descriptor1 & VTUNITS) >> VTUNITSSH]; }
	/** routine to set the units of the text in the text descriptor. */
	public void TDSetUnits(Units v) {descriptor1 = (descriptor1 & ~VTUNITS) | (v.getIndex() << VTUNITSSH); }
}
