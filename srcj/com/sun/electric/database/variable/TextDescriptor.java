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

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.tool.user.ui.EditWindow;

import java.util.HashMap;
import java.awt.Point;

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
	private static final int VTDISPLAYPARTSH =            4;		/* 0: right shift of VTDISPLAYPART */
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

	// text size information
	private static final int TXTMAXPOINTS =    63;
	private static final int TXTQGRIDSH =       6;		
	private static final double TXTMAXQGRID =    127.75;

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
		private final Poly.Type pt;

		private Position(String name, int index, Poly.Type pt)
		{
			this.name = name;
			this.index = index;
			this.pt = pt;
		}

		/**
		 * Routine to return the integer equivalent of this Position.
		 * @return the integer equivalent of this Position.
		 */
		private int getIndex() { return index; }

		/**
		 * Routine to return the Poly.Type to use for this Position.
		 * The Poly.Type will vary through the 9 "grab point" locations:
		 * center, left, right, up, down, up-left, up-right, down-left, down-right.
		 * @return the Poly.Type to use for this Position.
		 */
		public Poly.Type getPolyType() { return pt; }

		/**
		 * Returns a printable version of this Position.
		 * @return a printable version of this Position.
		 */
		public String toString() { return name; }

		/**
		 * Describes text centered at a point.
		 */
		public static final Position CENT = new Position("centered", VTPOSCENT, Poly.Type.TEXTCENT);

		/**
		 * Describes text centered above a point.
		 */
		public static final Position UP = new Position("up", VTPOSUP, Poly.Type.TEXTBOT);

		/**
		 * Describes text centered below a point.
		 */
		public static final Position DOWN = new Position("down", VTPOSDOWN, Poly.Type.TEXTTOP);

		/**
		 * Describes text centered to left of a point.
		 */
		public static final Position LEFT = new Position("left", VTPOSLEFT, Poly.Type.TEXTRIGHT);

		/**
		 * Describes text centered to right of a point.
		 */
		public static final Position RIGHT = new Position("right", VTPOSRIGHT, Poly.Type.TEXTLEFT);

		/**
		 * Describes text centered to upper-left of a point.
		 */
		public static final Position UPLEFT = new Position("up-left", VTPOSUPLEFT, Poly.Type.TEXTBOTRIGHT);

		/**
		 * Describes text centered to upper-right of a point.
		 */
		public static final Position UPRIGHT = new Position("up-right", VTPOSUPRIGHT, Poly.Type.TEXTBOTLEFT);

		/**
		 * Describes text centered to lower-left of a point.
		 */
		public static final Position DOWNLEFT = new Position("down-left", VTPOSDOWNLEFT, Poly.Type.TEXTTOPRIGHT);

		/**
		 * Describes text centered to lower-right of a point.
		 */
		public static final Position DOWNRIGHT = new Position("down-right", VTPOSDOWNRIGHT, Poly.Type.TEXTTOPLEFT);

		/**
		 * Describes text centered and limited to the object size.
		 * This means that the text may shrink in size or clip letters if necessary.
		 */
		public static final Position BOXED = new Position("boxed", VTPOSBOXED, Poly.Type.TEXTCENT);
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

		/**
		 * Describes a Variable that displays its value.
		 */
		public static final DispPos VALUE = new DispPos("value", VTDISPLAYVALUE);

		/**
		 * Describes a Variable that displays its name and value.
		 * The form of the display is “ATTR=VALUE”;
		 */
		public static final DispPos NAMEVALUE = new DispPos("name/value", VTDISPLAYNAMEVALUE);

		/**
		 * Describes a Variable that displays its name, value, 1-level inherit.
		 * The form of the display is “ATTR=VALUE;def=DEFAULT”;
		 */
		public static final DispPos NAMEVALINH = new DispPos("name/value/inherit-1", VTDISPLAYNAMEVALINH);

		/**
		 * Describes a Variable that displays its name, value, any inherit.
		 * The form of the display is “ATTR=VALUE;def=DEFAULT”;
		 */
		public static final DispPos NAMEVALINHALL = new DispPos("name/value/inherit-all", VTDISPLAYNAMEVALINHALL);
	}
	private static final DispPos [] theDispPos = new DispPos[] {DispPos.VALUE, DispPos.NAMEVALUE, DispPos.NAMEVALINH,
		DispPos.NAMEVALINHALL};


	/**
	 * Size is a class that describes text's size on a Variable.
	 * Text size can be absolute (in points) or relative (in quarter grid units).
	 */
	public static class Size
	{
		private final boolean absolute;
		private final double size;
		private final int bits;

		private Size(double size, boolean absolute)
		{
			this.size = size;
			this.absolute = absolute;
			if (absolute)
			{
				this.bits = (int)size;
			} else
			{
				this.bits = ((int)(size*4.0)) << TXTQGRIDSH;
			}
		}

		/**
		 * Routine to return bits associated with this text Size.
		 * @return bits associated with this text Size.
		 */
		private int getBits() { return bits; }

		/**
		 * Routine to return a Size object that describes a relative text size (in grid units).
		 * The size must be between 1 and 63 points.
		 * @param size the size in units.
		 * Returns null if the size is invalid.
		 */
		public static Size newAbsSize(int size)
		{
			if (size <= 0 || size > TXTMAXPOINTS) return null;
			return new Size(size, true);
		}

		/**
		 * Routine to return a Size object that describes an absolute point text size.
		 * The size must be between 0.25 and 127.75 grid units (in .25 increments).
		 * @param size the size in points.
		 * Returns null if the size is invalid.
		 */
		public static Size newRelSize(double size)
		{
			if (size <= 0 || size > TXTMAXQGRID) return null;
			return new Size(size, false);
		}

		/**
		 * Routine to return text Size value (in points or units).
		 * @return the text Size value (in points or units).
		 */
		public double getSize() { return size; }

		/**
		 * Routine to tell whether this text Size is absolute or relative.
		 * @return true if this text size is absolute
		 */
		public boolean isAbsolute() { return absolute; }

		/**
		 * Returns a printable version of this Size.
		 * @return a printable version of this Size.
		 */
		public String toString() { return "Text Size"; }
	}

	/**
	 * Rotation is a typesafe enum class that describes text's rotation in a Variable.
	 */
	public static class Rotation
	{
		private final int angle;
		private final int bits;

		private Rotation(int angle, int bits)
		{
			this.angle = angle;
			this.bits = bits;
		}

		/**
		 * Routine to return the integer equivalent of this DispPos.
		 * @return the integer equivalent of this DispPos.
		 */
		private int getBits() { return bits; }

		/**
		 * Returns a printable version of this Rotation.
		 * @return a printable version of this Rotation.
		 */
		public String toString() { return "Rotation "+angle; }

		/** Describes a Rotation of 0 degrees. */	public static final Rotation ROT0 = new Rotation(0, 0);
		/** Describes a Rotation of 90 degrees. */	public static final Rotation ROT90 = new Rotation(90, 1);
		/** Describes a Rotation of 180 degrees. */	public static final Rotation ROT180 = new Rotation(180, 2);
		/** Describes a Rotation of 270 degrees. */	public static final Rotation ROT270 = new Rotation(270, 3);
	}
	private static final Rotation [] theRotations = new Rotation[] {Rotation.ROT0, Rotation.ROT90, Rotation.ROT180,
		Rotation.ROT270};


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


	/** the words of the text descriptor */		private int descriptor0, descriptor1;
	/** set of all descriptors */				private static HashMap allDescriptors = new HashMap();
	/** descriptor for search */				private static TextDescriptor searchDescriptor = new TextDescriptor();
	/** blank descriptor */						private static TextDescriptor blankDescriptor = newDescriptor(0,0);
	/** nodeArc descriptor */					private static TextDescriptor nodeArcDescriptor = blankDescriptor.setRelSize(1);
	/** export descriptor */					private static TextDescriptor exportDescriptor = blankDescriptor.setRelSize(2);
	/** nonLayout descriptor */					private static TextDescriptor nonLayoutDescriptor = blankDescriptor.setRelSize(1);
	/** instance descriptor */					private static TextDescriptor instanceDescriptor = blankDescriptor.setRelSize(4);
	/** cell descriptor */						private static TextDescriptor cellDescriptor = blankDescriptor.setRelSize(1);

	/**
	 * The constructor simply creates a TextDescriptor with no values filled-in.
	 */
	private TextDescriptor()
	{
	}

	/**
	 * The constructor simply creates a TextDescriptor with specified values.
	 */
	private TextDescriptor(int descriptor0, int descriptor1)
	{
		this.descriptor0 = descriptor0;
		this.descriptor1 = descriptor1;
	}

	/**
	 * This routine finds descriptor with specified values.
	 */
	public static TextDescriptor newDescriptor(int descriptor0, int descriptor1)
	{
		searchDescriptor.descriptor0 = descriptor0;
		searchDescriptor.descriptor1 = descriptor1;
		TextDescriptor descriptor = (TextDescriptor) allDescriptors.get(searchDescriptor);
		if (descriptor != null) return descriptor;
		descriptor = new TextDescriptor(descriptor0, descriptor1);
		allDescriptors.put(descriptor, descriptor);
		return descriptor;
	}

	/**
	 * Routine to return a TextDescriptor that is blank.
	 * @return a TextDescriptor that is blank.
	 */
	public static TextDescriptor newBlankDescriptor() { return blankDescriptor; }

	/**
	 * Routine to return a TextDescriptor that is a default for Variables on NodeInsts.
	 * @return a TextDescriptor with to be used on a Variable on a NodeInsts.
	 */
	public static TextDescriptor newNodeArcDescriptor() { return nodeArcDescriptor; }

	/**
	 * Routine to return a TextDescriptor that is a default for Exports.
	 * @return a TextDescriptor with to be used on an Export.
	 */
	public static TextDescriptor newExportDescriptor() { return exportDescriptor; }

	/**
	 * Routine to return a TextDescriptor that is a default for Nonlayout text (on invisible pins).
	 * @return a TextDescriptor with to be used on Nonlayout text (on invisible pins)..
	 */
	public static TextDescriptor newNonLayoutDescriptor() { return nonLayoutDescriptor; }

	/**
	 * Routine to return a TextDescriptor that is a default for Cell instance names.
	 * This text appears on unexpanded instances of Cells.
	 * @return a TextDescriptor with to be used on a Cell instance name.
	 */
	public static TextDescriptor newInstanceDescriptor() { return instanceDescriptor; }

	/**
	 * Routine to return a TextDescriptor that is a default for Variables on Cells.
	 * These variables are use for parameter declarations.
	 * @return a TextDescriptor with to be used on a Variable on a Cells.
	 */
	public static TextDescriptor newCellDescriptor() { return cellDescriptor; }

    /**
     * Compares this text descriptor to the specified object.
     * The result is <code>true</code> if and only if the argument is not
     * <code>null</code> and is a <code>TextDescriptor</code> object with
     * the same fields.
     *
     * @param   anObject   the object to compare this <code>TextDescriptor</code>
     *                     against.
     * @return  <code>true</code> if the <code>TextDescriptor</code> are equal;
     *          <code>false</code> otherwise.
     */
    public boolean equals(Object anObject) {
		if (this == anObject) {
			return true;
		}
		if (anObject instanceof TextDescriptor) {
			TextDescriptor td = (TextDescriptor)anObject;
			return descriptor0 == td.descriptor0 && descriptor1 == td.descriptor1;
		}
		return false;
    }

    /**
     * Returns a hash code for this TextDescriptor. The hash code for a
     * <code>TextDescriptor</code> object is computed as sum of its fields.
     * @return  a hash code value for this object.
     */
    public int hashCode() {
		return descriptor0+descriptor1;
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
	//public void lowLevelSet(int descriptor0, int descriptor1) { this.descriptor0 = descriptor0;   this.descriptor1 = descriptor1; }

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
	//public void clear() { this.descriptor0 = this.descriptor1 = 0; }

	/**
	 * Routine to copy this TextDescriptor to a specified destination.
	 * @param dest the specified destination TextDescriptor.
	 */
	//public void copy(TextDescriptor dest) { dest.lowLevelSet(descriptor0, descriptor1); }

	/**
	 * Routine to return true if this TextDescriptor is different than the specified one.
	 * @return true if this TextDescriptor is different than the specified one.
	 */
	public boolean diff(TextDescriptor td) { return td != this; }

	/**
	 * Routine to return the text position of the TextDescriptor.
	 * The text position describes the "grab point" of the text,
	 * which is the point on the text that is attached to the object and does not move.
	 * @return the text position of the TextDescriptor.
	 */
	public Position getPos()
	{
		int pos = descriptor0 & VTPOSITION;
		if (pos >= thePositions.length) pos = 0;
		return thePositions[pos];
	}

	/**
	 * Routine to set the text position of the TextDescriptor.
	 * The text position describes the "grab point" of the text,
	 * which is the point on the text that is attached to the object and does not move.
	 * @param p the text position of the TextDescriptor.
	 */
	public TextDescriptor setPos(Position p) { return newDescriptor((descriptor0 & ~VTPOSITION) | p.getIndex(), descriptor1); }

	/**
	 * Routine to return the text size of the text in this TextDescriptor.
	 * This is a Size object that can describe either absolute text (in points)
	 * or relative text (in quarter units).
	 * @return the text size of the text in this TextDescriptor.
	 */
	public Size getSize()
	{
		int textSize = (descriptor1 & VTSIZE) >> VTSIZESH;
		if (textSize <= TXTMAXPOINTS) return Size.newAbsSize(textSize);
		int sizeValue = textSize>>TXTQGRIDSH;
		double size = sizeValue / 4.0;
		return Size.newRelSize(size);
	}

	public int getTrueSize(EditWindow wnd)
	{
		Size s = getSize();
		if (s == null) return 14;

		// absolute font sizes are easy
		if (s.isAbsolute()) return (int)s.getSize();

		// relative font: get size in grid units
		double height = s.getSize();

		// convert to screen units
		Point p = wnd.deltaDatabaseToScreen(height, height);
		return p.x;
	}

	/**
	 * Routine to set the text size of this TextDescriptor to an absolute size (in points).
	 * The size must be between 1 and 63 points.
	 * @param s the point size of this TextDescriptor.
	 */
	public TextDescriptor setAbsSize(int s)
	{
		Size size = Size.newAbsSize(s);
		if (size == null) return this;
		return newDescriptor(descriptor0, (descriptor1 & ~VTSIZE) | (size.getBits() << VTSIZESH));
	}

	/**
	 * Routine to set the text size of this TextDescriptor to a relative size (in units).
	 * The size must be between 0.25 and 127.75 grid units (in .25 increments).
	 * @param s the unit size of this TextDescriptor.
	 */
	public TextDescriptor setRelSize(double s)
	{
		Size size = Size.newRelSize(s);
		if (size == null) return this;
		return newDescriptor(descriptor0, (descriptor1 & ~VTSIZE) | (size.getBits() << VTSIZESH));
	}

	/**
	 * Routine to return the text font of the TextDescriptor.
	 * @return the text font of the TextDescriptor.
	 */
	public int getFace() { return (descriptor1 & VTFACE) >> VTFACESH; }

	/**
	 * Routine to set the text font of the TextDescriptor.
	 * @param f the text font of the TextDescriptor.
	 */
	public TextDescriptor setFace(int f) { return newDescriptor(descriptor0, (descriptor1 & ~VTFACE) | (f << VTFACESH)); }

	/**
	 * Routine to return the text rotation of the TextDescriptor.
	 * There are only 4 rotations: 0, 90 degrees, 180 degrees, and 270 degrees.
	 * @return the text rotation of the TextDescriptor.
	 */
	public Rotation getRotation() { return theRotations[(descriptor1 & VTROTATION) >> VTROTATIONSH]; }

	/**
	 * Routine to set the text rotation of the TextDescriptor.
	 * There are only 4 rotations: 0, 90 degrees, 180 degrees, and 270 degrees.
	 * @param r the text rotation of the TextDescriptor.
	 */
	public TextDescriptor setRotation(Rotation r) { return newDescriptor(descriptor0, (descriptor1 & ~VTROTATION) | (r.getBits() << VTROTATIONSH)); }

	/**
	 * Routine to return the text display part of the TextDescriptor.
	 * @return the text display part of the TextDescriptor.
	 */
	public DispPos getDispPart() { return theDispPos[(descriptor0 & VTDISPLAYPART) >> VTDISPLAYPARTSH]; }

	/**
	 * Routine to set the text display part of the TextDescriptor.
	 * @param d the text display part of the TextDescriptor.
	 */
	public TextDescriptor setDispPart(DispPos d) { return newDescriptor((descriptor0 & ~VTDISPLAYPART) | d.getIndex(), descriptor1); }

	/**
	 * Routine to return true if the text in the TextDescriptor is italic.
	 * @return true if the text in the TextDescriptor is italic.
	 */
	public boolean isItalic() { return (descriptor0 & VTITALIC) != 0; }

	/**
	 * Routine to set the text in the TextDescriptor to be italic.
	 */
	public TextDescriptor setItalic() { return newDescriptor(descriptor0 | VTITALIC, descriptor1); }

	/**
	 * Routine to set the text in the TextDescriptor to be not italic.
	 */
	public TextDescriptor clearItalic() { return newDescriptor(descriptor0 & ~VTITALIC, descriptor1); }

	/**
	 * Routine to return true if the text in the TextDescriptor is bold.
	 * @return true if the text in the TextDescriptor is bold.
	 */
	public boolean isBold() { return (descriptor0 & VTBOLD) != 0; }

	/**
	 * Routine to set the text in the TextDescriptor to be bold.
	 */
	public TextDescriptor setBold() { return newDescriptor(descriptor0 | VTBOLD, descriptor1); }

	/**
	 * Routine to set the text in the TextDescriptor to be not bold.
	 */
	public TextDescriptor clearBold() { return newDescriptor(descriptor0 & ~VTBOLD, descriptor1); }

	/**
	 * Routine to return true if the text in the TextDescriptor is underlined.
	 * @return true if the text in the TextDescriptor is underlined.
	 */
	public boolean isUnderline() { return (descriptor0 & VTUNDERLINE) != 0; }

	/**
	 * Routine to set the text in the TextDescriptor to be underlined.
	 */
	public TextDescriptor setUnderline() { return newDescriptor(descriptor0 | VTUNDERLINE, descriptor1); }

	/**
	 * Routine to set the text in the TextDescriptor to be not underlined.
	 */
	public TextDescriptor clearUnderline() { return newDescriptor(descriptor0 & ~VTUNDERLINE, descriptor1); }

	/**
	 * Routine to return true if the text in the TextDescriptor is interior.
	 * Interior text is not seen at higher levels of the hierarchy.
	 * @return true if the text in the TextDescriptor is interior.
	 */
	public boolean isInterior() { return (descriptor0 & VTINTERIOR) != 0; }

	/**
	 * Routine to set the text in the TextDescriptor to be interior.
	 * Interior text is not seen at higher levels of the hierarchy.
	 */
	public TextDescriptor setInterior() { return newDescriptor(descriptor0 | VTINTERIOR, descriptor1); }

	/**
	 * Routine to set the text in the TextDescriptor to be not interior.
	 * Interior text is not seen at higher levels of the hierarchy.
	 */
	public TextDescriptor clearInterior() { return newDescriptor(descriptor0 & ~VTINTERIOR, descriptor1); }

	/**
	 * Routine to return true if the text in the TextDescriptor is inheritable.
	 * Inheritable variables copy their contents from prototype to instance.
	 * Only Variables on NodeProto and PortProto objects can be inheritable.
	 * When a NodeInst is created, any inheritable Variables on its NodeProto are automatically
	 * created on that NodeInst.
	 * @return true if the text in the TextDescriptor is inheritable.
	 */
	public boolean isInherit() { return (descriptor0 & VTINHERIT) != 0; }

	/**
	 * Routine to set the text in the TextDescriptor to be inheritable.
	 * Inheritable variables copy their contents from prototype to instance.
	 * Only Variables on NodeProto and PortProto objects can be inheritable.
	 * When a NodeInst is created, any inheritable Variables on its NodeProto are automatically
	 * created on that NodeInst.
	 */
	public TextDescriptor setInherit() { return newDescriptor(descriptor0 | VTINHERIT, descriptor1); }

	/**
	 * Routine to set the text in the TextDescriptor to be not inheritable.
	 * Inheritable variables copy their contents from prototype to instance.
	 * Only Variables on NodeProto and PortProto objects can be inheritable.
	 * When a NodeInst is created, any inheritable Variables on its NodeProto are automatically
	 * created on that NodeInst.
	 */
	public TextDescriptor clearInherit() { return newDescriptor(descriptor0 & ~VTINHERIT, descriptor1); }

	/**
	 * Routine to return true if the text in the TextDescriptor is a parameter.
	 * Parameters are those Variables that have values on instances which are
	 * passed down the hierarchy into the contents.
	 * Parameters can only exist on NodeInst objects.
	 * @return true if the text in the TextDescriptor is a parameter.
	 */
	public boolean isParam() { return (descriptor0 & VTISPARAMETER) != 0; }

	/**
	 * Routine to set the text in the TextDescriptor to be a parameter.
	 * Parameters are those Variables that have values on instances which are
	 * passed down the hierarchy into the contents.
	 * Parameters can only exist on NodeInst objects.
	 */
	public TextDescriptor setParam() { return newDescriptor(descriptor0 | VTISPARAMETER, descriptor1); }

	/**
	 * Routine to set the text in the TextDescriptor to be not a parameter.
	 * Parameters are those Variables that have values on instances which are
	 * passed down the hierarchy into the contents.
	 * Parameters can only exist on NodeInst objects.
	 */
	public TextDescriptor clearParam() { return newDescriptor(descriptor0 & ~VTISPARAMETER, descriptor1); }

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
	public TextDescriptor setOff(int x, int y)
	{
		int desc0 = descriptor0 & ~(VTXOFF|VTYOFF|VTXOFFNEG|VTYOFFNEG);
		if (x < 0)
		{
			x = -x;
			desc0 |= VTXOFFNEG;
		}
		if (y < 0)
		{
			y = -y;
			desc0 |= VTYOFFNEG;
		}
		int scale = Math.max(x,y) >> VTOFFMASKWID;
		x /= (scale + 1);
		y /= (scale + 1);
		desc0 |= (x << VTXOFFSH) & VTXOFF;
		desc0 |= (y << VTYOFFSH) & VTYOFF;
		//setOffScale(scale);
		return newDescriptor(desc0, (descriptor1 & ~VTOFFSCALE) | ((scale << VTOFFSCALESH) & VTOFFSCALE));
	}

	/** routine to return the offset scale of the text in the text descriptor. */
	private int getOffScale() { return (descriptor1 & VTOFFSCALE) >> VTOFFSCALE; }

	/** routine to set the offset scale of the text in the text descriptor. */
	//private TextDescriptor setOffScale(int v) { return descriptor1 = (descriptor1 & ~VTOFFSCALE) | (v << VTOFFSCALESH); }

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
	public TextDescriptor setUnits(Units u) { return newDescriptor(descriptor0, (descriptor1 & ~VTUNITS) | (u.getIndex() << VTUNITSSH)); }
}
