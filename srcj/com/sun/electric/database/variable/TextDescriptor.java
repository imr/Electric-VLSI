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


	/* text font sizes (in VARIABLE, NODEINST, PORTPROTO, and POLYGON->textdescription) */
	/** points from 1 to TXTMAXPOINTS */					public static final int TXTPOINTS =        077;
	/** right-shift of TXTPOINTS */							public static final int TXTPOINTSSH =        0;
	public static final int TXTMAXPOINTS =      63;
//	public static final int TXTSETPOINTS(p)   ((p)<<TXTPOINTSSH)
//	public static final int TXTGETPOINTS(p)   (((p)&TXTPOINTS)>>TXTPOINTSSH)

	public static final int TXTQGRID =    077700;		
	public static final int TXTQGRIDSH =       6;		
	public static final int TXTMAXQGRID =    511;
//	public static final int TXTSETQGRID(ql) ((ql)<<TXTQGRIDSH)
//	public static final int TXTGETQGRIDql) (((ql)&TXTQGRID)>>TXTQGRIDSH)
	/** fixed-width text for text editing */			public static final int TXTEDITOR =     077770;
	/** text for menu selection */						public static final int TXTMENU =       077771;

	/**
	 * Position is a typesafe enum class that describes the text position of a Variable.
	 * The Position describes the "grab point" of the text,
	 * which is the point on the text that is attached to the object and does not move.
	 * For example, when the Position is CENT, then the center of the text is fixed,
	 * and as the text grows, it expands uniformly about the center.
	 * When the Position is UP, the text is centered above the grab point, and as the text grows
	 * it expands upward from the bottom-center point.
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

		/**
		 * Routine to return the integer equivalent of this Position.
		 * @return the integer equivalent of this Position.
		 */
		private int getIndex() { return index; }

		/**
		 * Returns a printable version of this Position.
		 * @return a printable version of this Position.
		 */
		public String toString() { return name; }

		/** Describes text centered at a point. */					public static final Position CENT =      new Position("centered", VTPOSCENT);
		/** Describes text centered above a point. */				public static final Position UP =        new Position("up", VTPOSUP);
		/** Describes text centered below a point. */				public static final Position DOWN =      new Position("down", VTPOSDOWN);
		/** Describes text centered to left of a point. */			public static final Position LEFT =      new Position("left", VTPOSLEFT);
		/** Describes text centered to right of a point. */			public static final Position RIGHT =     new Position("right", VTPOSRIGHT);
		/** Describes text centered to upper-left of a point. */	public static final Position UPLEFT =    new Position("up-left", VTPOSUPLEFT);
		/** Describes text centered to upper-right of a point. */	public static final Position UPRIGHT =   new Position("up-right", VTPOSUPRIGHT);
		/** Describes text centered to lower-left of a point. */	public static final Position DOWNLEFT =  new Position("down-left", VTPOSDOWNLEFT);
		/** Describes text centered to lower-right of a point. */	public static final Position DOWNRIGHT = new Position("down-right", VTPOSDOWNRIGHT);
		/** Describes text centered and limited to object size. */	public static final Position BOXED =     new Position("boxed", VTPOSBOXED);
	}
	private static final Position [] thePositions = new Position[] {Position.CENT, Position.UP, Position.DOWN,
		Position.LEFT, Position.RIGHT, Position.UPLEFT, Position.UPRIGHT, Position.DOWNLEFT, Position.DOWNRIGHT, Position.BOXED};


	/**
	 * DispPos is a typesafe enum class that describes text's display position on a Variable.
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

		/**
		 * Routine to return the integer equivalent of this DispPos.
		 * @return the integer equivalent of this DispPos.
		 */
		private int getIndex() { return index; }

		/**
		 * Returns a printable version of this DispPos.
		 * @return a printable version of this DispPos.
		 */
		public String toString() { return name; }

		/** Describes a Variable that displays its value. */
			public static final DispPos VALUE =         new DispPos("value", VTDISPLAYVALUE);
		/** Describes a Variable that displays its name and value. */
			public static final DispPos NAMEVALUE =     new DispPos("name/value", VTDISPLAYNAMEVALUE);
		/** Describes a Variable that displays its name, value, 1-level inherit. */
			public static final DispPos NAMEVALINH =    new DispPos("name/value/inherit-1", VTDISPLAYNAMEVALINH);
		/** Describes a Variable that displays its name, value, any inherit. */
			public static final DispPos NAMEVALINHALL = new DispPos("name/value/inherit-all", VTDISPLAYNAMEVALINHALL);
	}
	private static final DispPos [] theDispPos = new DispPos[] {DispPos.VALUE, DispPos.NAMEVALUE, DispPos.NAMEVALINH,
		DispPos.NAMEVALINHALL};


	/**
	 * Units is a typesafe enum class that describes text's units on a Variable.
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

		/**
		 * Routine to return the integer equivalent of this Units.
		 * @return the integer equivalent of this Units.
		 */
		private int getIndex() { return index; }

		/**
		 * Returns a printable version of this Units.
		 * @return a printable version of this Units.
		 */
		public String toString() { return name; }

		/** Describes no units. */				public static final Units NONE =        new Units("none", VTUNITSNONE);
		/** Describes resistance units. */		public static final Units RESISTANCE =  new Units("resistance", VTUNITSRES);
		/** Describes capacitance units. */		public static final Units CAPACITANCE = new Units("capacitance", VTUNITSCAP);
		/** Describes inductance units. */		public static final Units INDUCTANCE =  new Units("inductance", VTUNITSIND);
		/** Describes current units. */			public static final Units CURRENT =     new Units("current", VTUNITSCUR);
		/** Describes voltage units. */			public static final Units VOLTAGE =     new Units("voltage", VTUNITSVOLT);
		/** Describes distance units. */		public static final Units DISTANCE =    new Units("distance", VTUNITSDIST);
		/** Describes time units. */			public static final Units TIME =        new Units("time", VTUNITSTIME);
	}
	private static final Units [] theUnits = new Units[] {Units.NONE, Units.RESISTANCE, Units.CAPACITANCE,
		Units.INDUCTANCE, Units.CURRENT, Units.VOLTAGE, Units.DISTANCE, Units.TIME};


	/** the first word of the text descriptor */		private int descriptor0;
	/** the second word of the text descriptor */		private int descriptor1;

	/**
	 * The constructor simply creates a TextDescriptor with no values filled-in.
	 */
	public TextDescriptor()
	{
	}

	/**
	 * Low-level routine to set the bits in the TextDescriptor.
	 * These bits are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the binary ".elib"
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @param descriptor0 the first word of the new TextDescriptor.
	 * @param descriptor1 the second word of the new TextDescriptor.
	 */
	public void lowLevelSet(int descriptor0, int descriptor1) { this.descriptor0 = descriptor0;   this.descriptor1 = descriptor1; }

	/**
	 * Low-level routine to get the first word of the bits in the TextDescriptor.
	 * These bits are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the binary ".elib"
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @return the first word of the bits in the TextDescriptor.
	 */
	public int lowLevelGet0() { return descriptor0; }

	/**
	 * Low-level routine to get the second word of the bits in the TextDescriptor.
	 * These bits are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the binary ".elib"
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @return the second word of the bits in the TextDescriptor.
	 */
	public int lowLevelGet1() { return descriptor1; }

	/**
	 * Routine to clear the the TextDescriptor.
	 */
	public void clear() { this.descriptor0 = this.descriptor1 = 0; }

	/**
	 * Routine to copy this TextDescriptor to a specified destination.
	 * @param dest the specified destination TextDescriptor.
	 */
	public void copy(TextDescriptor dest) { dest.lowLevelSet(descriptor0, descriptor1); }

	/**
	 * Routine to return true if this TextDescriptor is different than the specified one.
	 * @return true if this TextDescriptor is different than the specified one.
	 */
	public boolean diff(TextDescriptor td)
	{
		if (this.descriptor0 != td.descriptor0) return true;
		if (this.descriptor1 != td.descriptor1) return true;
		return false;
	}

	/**
	 * Routine to return the text position of the TextDescriptor.
	 * The text position describes the "grab point" of the text,
	 * which is the point on the text that is attached to the object and does not move.
	 * @return the text position of the TextDescriptor.
	 */
	public Position getPos() { return thePositions[descriptor0 & VTPOSITION]; }

	/**
	 * Routine to set the text position of the TextDescriptor.
	 * The text position describes the "grab point" of the text,
	 * which is the point on the text that is attached to the object and does not move.
	 * @param p the text position of the TextDescriptor.
	 */
	public void setPos(Position p) {descriptor0 = (descriptor0 & ~VTPOSITION) | p.getIndex(); }

	/**
	 * Routine to return the text size of the TextDescriptor.
	 * @return the text size of the TextDescriptor.
	 */
	public int getSize() { return (descriptor1 & VTSIZE) >> VTSIZESH; }

	/**
	 * Routine to set the text size of the TextDescriptor.
	 * @param s the text size of the TextDescriptor.
	 */
	public void setSize(int s) {descriptor1 = (descriptor1 & ~VTSIZE) | (s << VTSIZESH); }

	/**
	 * Routine to return the text font of the TextDescriptor.
	 * @return the text font of the TextDescriptor.
	 */
	public int getFace() { return (descriptor1 & VTFACE) >> VTFACESH; }

	/**
	 * Routine to set the text font of the TextDescriptor.
	 * @param f the text font of the TextDescriptor.
	 */
	public void setFace(int f) {descriptor1 = (descriptor1 & ~VTFACE) | (f << VTFACESH); }

	/**
	 * Routine to return the text rotation of the TextDescriptor.
	 * @return the text rotation of the TextDescriptor.
	 */
	public int getRotation() { return (descriptor1 & VTROTATION) >> VTROTATIONSH; }

	/**
	 * Routine to set the text rotation of the TextDescriptor.
	 * @param f the text rotation of the TextDescriptor.
	 */
	public void setRotation(int r) {descriptor1 = (descriptor1 & ~VTROTATION) | (r << VTROTATIONSH); }

	/**
	 * Routine to return the text display part of the TextDescriptor.
	 * @return the text display part of the TextDescriptor.
	 */
	public DispPos getDispPart() { return theDispPos[descriptor0 & VTDISPLAYPART]; }

	/**
	 * Routine to set the text display part of the TextDescriptor.
	 * @param f the text display part of the TextDescriptor.
	 */
	public void setDispPart(DispPos d) {descriptor0 = (descriptor0 & ~VTDISPLAYPART) | d.getIndex(); }

	/**
	 * Routine to return true if the text in the TextDescriptor is italic.
	 * @return true if the text in the TextDescriptor is italic.
	 */
	public boolean getItalic() { return (descriptor0 & VTITALIC) != 0; }

	/**
	 * Routine to set the text in the TextDescriptor to be italic.
	 */
	public void setItalic() { descriptor0 |= VTITALIC; }

	/**
	 * Routine to set the text in the TextDescriptor to be not italic.
	 */
	public void clearItalic() { descriptor0 &= ~VTITALIC; }

	/**
	 * Routine to return true if the text in the TextDescriptor is bold.
	 * @return true if the text in the TextDescriptor is bold.
	 */
	public boolean getBold() { return (descriptor0 & VTBOLD) != 0; }

	/**
	 * Routine to set the text in the TextDescriptor to be bold.
	 */
	public void setBold() { descriptor0 |= VTBOLD; }

	/**
	 * Routine to set the text in the TextDescriptor to be not bold.
	 */
	public void clearBold() { descriptor0 &= ~VTBOLD; }

	/**
	 * Routine to return true if the text in the TextDescriptor is underlined.
	 * @return true if the text in the TextDescriptor is underlined.
	 */
	public boolean getUnderline() { return (descriptor0 & VTUNDERLINE) != 0; }

	/**
	 * Routine to set the text in the TextDescriptor to be underlined.
	 */
	public void setUnderline() { descriptor0 |= VTUNDERLINE; }

	/**
	 * Routine to set the text in the TextDescriptor to be not underlined.
	 */
	public void clearUnderline() { descriptor0 &= ~VTUNDERLINE; }

	/**
	 * Routine to return true if the text in the TextDescriptor is interior.
	 * Interior text is not seen at higher levels of the hierarchy.
	 * @return true if the text in the TextDescriptor is interior.
	 */
	public boolean getInterior() { return (descriptor0 & VTINTERIOR) != 0; }

	/**
	 * Routine to set the text in the TextDescriptor to be interior.
	 * Interior text is not seen at higher levels of the hierarchy.
	 */
	public void setInterior() { descriptor0 |= VTINTERIOR; }

	/**
	 * Routine to set the text in the TextDescriptor to be not interior.
	 * Interior text is not seen at higher levels of the hierarchy.
	 */
	public void clearInterior() { descriptor0 &= ~VTINTERIOR; }

	/**
	 * Routine to return true if the text in the TextDescriptor is inheritable.
	 * Inheritable variables copy their contents from prototype to instance.
	 * Only Variables on NodeProto and PortProto objects can be inheritable.
	 * When a NodeInst is created, any inheritable Variables on its NodeProto are automatically
	 * created on that NodeInst.
	 * @return true if the text in the TextDescriptor is inheritable.
	 */
	public boolean getInherit() { return (descriptor0 & VTINHERIT) != 0; }

	/**
	 * Routine to set the text in the TextDescriptor to be inheritable.
	 * Inheritable variables copy their contents from prototype to instance.
	 * Only Variables on NodeProto and PortProto objects can be inheritable.
	 * When a NodeInst is created, any inheritable Variables on its NodeProto are automatically
	 * created on that NodeInst.
	 */
	public void setInherit() { descriptor0 |= VTINHERIT; }

	/**
	 * Routine to set the text in the TextDescriptor to be not inheritable.
	 * Inheritable variables copy their contents from prototype to instance.
	 * Only Variables on NodeProto and PortProto objects can be inheritable.
	 * When a NodeInst is created, any inheritable Variables on its NodeProto are automatically
	 * created on that NodeInst.
	 */
	public void clearInherit() { descriptor0 &= ~VTINHERIT; }

	/**
	 * Routine to return true if the text in the TextDescriptor is a parameter.
	 * Parameters are those Variables that have values on instances which are
	 * passed down the hierarchy into the contents.
	 * Parameters can only exist on NodeInst objects.
	 * @return true if the text in the TextDescriptor is a parameter.
	 */
	public boolean getIsParam() { return (descriptor0 & VTISPARAMETER) != 0; }

	/**
	 * Routine to set the text in the TextDescriptor to be a parameter.
	 * Parameters are those Variables that have values on instances which are
	 * passed down the hierarchy into the contents.
	 * Parameters can only exist on NodeInst objects.
	 */
	public void setIsParam() { descriptor0 |= VTISPARAMETER; }

	/**
	 * Routine to set the text in the TextDescriptor to be not a parameter.
	 * Parameters are those Variables that have values on instances which are
	 * passed down the hierarchy into the contents.
	 * Parameters can only exist on NodeInst objects.
	 */
	public void clearIsParam() { descriptor0 &= ~VTISPARAMETER; }

	/**
	 * Routine to return the X offset of the text in the TextDescriptor.
	 * The value is scaled by 4, so a value of 3 indicates a shift of 0.75 and a value of 4 shifts by 1.
	 * @return the X offset of the text in the TextDescriptor.
	 */
	public int getXOff()
	{
		int offset = (descriptor0 & VTXOFF) >> VTXOFFSH;
		if ((descriptor0&VTXOFFNEG) != 0) offset = -offset;
		int scale = getOffScale() + 1;
		return(offset * scale);
	}

	/**
	 * Routine to return the Y offset of the text in the TextDescriptor.
	 * The value is scaled by 4, so a value of 3 indicates a shift of 0.75 and a value of 4 shifts by 1.
	 * @return the Y offset of the text in the TextDescriptor.
	 */
	public int getYOff()
	{
		int offset = (descriptor0 & VTYOFF) >> VTYOFFSH;
		if ((descriptor0&VTYOFFNEG) != 0) offset = -offset;
		int scale = getOffScale() + 1;
		return(offset * scale);
	}

	/**
	 * Routine to set the X and Y offsets of the text in the TextDescriptor.
	 * The values are scaled by 4, so a value of 3 indicates a shift of 0.75 and a value of 4 shifts by 1.
	 * @param x the X offset of the text in the TextDescriptor.
	 * @param y the Y offset of the text in the TextDescriptor.
	 */
	public void setOff(int x, int y)
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
		setOffScale(scale);
	}

	/** routine to return the offset scale of the text in the text descriptor. */
	private int getOffScale() { return (descriptor1 & VTOFFSCALE) >> VTOFFSCALE; }

	/** routine to set the offset scale of the text in the text descriptor. */
	private void setOffScale(int v) {descriptor1 = (descriptor1 & ~VTOFFSCALE) | (v << VTOFFSCALESH); }

	/**
	 * Routine to return the units of the TextDescriptor.
	 * Units describe the type of real-world unit to apply to the value.
	 * For example, if this value is in volts, the Units tells whether the value
	 * is volts, millivolts, microvolts, etc.
	 * @return the units of the TextDescriptor.
	 */
	public Units getUnits() { return theUnits[(descriptor1 & VTUNITS) >> VTUNITSSH]; }

	/**
	 * Routine to set the units of the TextDescriptor.
	 * Units describe the type of real-world unit to apply to the value.
	 * For example, if this value is in volts, the Units tells whether the value
	 * is volts, millivolts, microvolts, etc.
	 * @param u the units of the TextDescriptor.
	 */
	public void setUnits(Units u) {descriptor1 = (descriptor1 & ~VTUNITS) | (u.getIndex() << VTUNITSSH); }
}
