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

import com.sun.electric.database.change.Undo;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;

import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.prefs.Preferences;

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
	private static final int VTDISPLAYNAMEVALUE =         2;		/* 0:   display name and value */
	private static final int VTDISPLAYNAMEVALINH =        1;		/* 0:   display name, value, 1-level inherit */
	private static final int VTDISPLAYNAMEVALINHALL =     3;		/* 0:   display name, value, any inherit */
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
	private static final int VTUNITSHMASK =              07;		/* 1: mask of this value after being shifted down */
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
	private static final int TXTMAXPOINTS =       63;
	private static final int TXTQGRIDSH =          6;		
	private static final double TXTMAXQGRID =    127.75;

	/**
	 * Position is a typesafe enum class that describes the text position of a Variable.
	 * The Position describes the "anchor point" of the text,
	 * which is the point on the text that is attached to the object and does not move.
	 * For example, when the Position is CENT, then the center of the text is fixed,
	 * and as the text grows, it expands uniformly about the center.
	 * When the Position is UP, the text is centered above the anchor point, and as the text grows
	 * it expands upward from the bottom-center point.
	 */
	public static class Position
	{
		private final String name;
		private final int index;
		private final Poly.Type pt;
        private static List positions = new ArrayList();

		private Position(String name, int index, Poly.Type pt)
		{
			this.name = name;
			this.index = index;
			this.pt = pt;
            positions.add(index, this);
		}

		/**
		 * Method to return the integer equivalent of this Position.
		 * @return the integer equivalent of this Position.
		 */
		private int getIndex() { return index; }

		/**
		 * Method to return the Poly.Type to use for this Position.
		 * The Poly.Type will vary through the 9 "anchor point" locations:
		 * center, left, right, up, down, up-left, up-right, down-left, down-right.
		 * @return the Poly.Type to use for this Position.
		 */
		public Poly.Type getPolyType() { return pt; }

		/**
		 * Method to return the number Positions.
		 * @return the number of Positions.
		 */
		public static int getNumPositions() { return positions.size(); }

		/**
		 * Method to return the Position at a given index.
		 * @param index the Position number desired.
		 * @return the Position at a given index.
		 */
		public static Position getPositionAt(int index) { return (Position)positions.get(index); }

        /**
         * Get an iterator over all Positions
         */
        public static Iterator getPositions() { return positions.iterator(); }

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
		public static final Position UP = new Position("bottom", VTPOSUP, Poly.Type.TEXTBOT);

		/**
		 * Describes text centered below a point.
		 */
		public static final Position DOWN = new Position("top", VTPOSDOWN, Poly.Type.TEXTTOP);

		/**
		 * Describes text centered to left of a point.
		 */
		public static final Position LEFT = new Position("right", VTPOSLEFT, Poly.Type.TEXTRIGHT);

		/**
		 * Describes text centered to right of a point.
		 */
		public static final Position RIGHT = new Position("left", VTPOSRIGHT, Poly.Type.TEXTLEFT);

		/**
		 * Describes text centered to upper-left of a point.
		 */
		public static final Position UPLEFT = new Position("lower-right", VTPOSUPLEFT, Poly.Type.TEXTBOTRIGHT);

		/**
		 * Describes text centered to upper-right of a point.
		 */
		public static final Position UPRIGHT = new Position("lower-left", VTPOSUPRIGHT, Poly.Type.TEXTBOTLEFT);

		/**
		 * Describes text centered to lower-left of a point.
		 */
		public static final Position DOWNLEFT = new Position("upper-right", VTPOSDOWNLEFT, Poly.Type.TEXTTOPRIGHT);

		/**
		 * Describes text centered to lower-right of a point.
		 */
		public static final Position DOWNRIGHT = new Position("upper-left", VTPOSDOWNRIGHT, Poly.Type.TEXTTOPLEFT);

		/**
		 * Describes text centered and limited to the object size.
		 * This means that the text may shrink in size or clip letters if necessary.
		 */
		public static final Position BOXED = new Position("boxed", VTPOSBOXED, Poly.Type.TEXTBOX);
	}


	/**
	 * DispPos is a typesafe enum class that describes text's display position on a Variable.
	 */
	public static class DispPos
	{
		private final String name;
		private final int index;
		private static List positions = new ArrayList();

		private DispPos(String name, int index)
		{
			this.name = name;
			this.index = index;
			positions.add(this);
		}

		/**
		 * Method to return the integer equivalent of this DispPos.
		 * @return the integer equivalent of this DispPos.
		 */
		public int getIndex() { return index; }

		/**
		 * Method to return the name of this DispPos.
		 * It is used in popup menus.
		 * @return the name of this DispPos.
		 */
		public String getName() { return name; }

		/**
		 * Method to return the number DispPos.
		 * @return the number DispPos.
		 */
		public static int getNumShowStyles() { return positions.size(); }

		/**
		 * Method to return the DispPos at a given index.
		 * @param index the DispPos number desired.
		 * @return the DispPos at a given index.
		 */
		public static DispPos getShowStylesAt(int index) {
            for (Iterator it = positions.iterator(); it.hasNext(); ) {
                DispPos d = (DispPos)it.next();
                if (d.index == index) return d;
            }
            return NAMEVALUE;
        }

        /**
         * Get an iterator over all show styles.
         * @return an iterator over the list of show styles
         */
        public static Iterator getShowStyles() { return positions.iterator(); }

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
		 * Describes a Variable that displays its name, value, 1-level inherit.
		 * The form of the display is "ATTR=VALUE;def=DEFAULT";
		 */
		//public static final DispPos NAMEVALINH = new DispPos("name=inherit;def=value", VTDISPLAYNAMEVALINH);

		/**
		 * Describes a Variable that displays its name and value.
		 * The form of the display is "ATTR=VALUE";
		 */
		public static final DispPos NAMEVALUE = new DispPos("name=value", VTDISPLAYNAMEVALUE);

		/**
		 * Describes a Variable that displays its name, value, any inherit.
		 * The form of the display is "ATTR=VALUE;def=DEFAULT";
		 */
		//public static final DispPos NAMEVALINHALL = new DispPos("name=inheritAll;def=value", VTDISPLAYNAMEVALINHALL);
	}


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
		 * Method to return bits associated with this text Size.
		 * @return bits associated with this text Size.
		 */
		private int getBits() { return bits; }

		/**
		 * Method to return a Size object that describes a relative text size (in grid units).
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
		 * Method to return a Size object that describes an absolute point text size.
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
		 * Method to return text Size value (in points or units).
		 * @return the text Size value (in points or units).
		 */
		public double getSize() { return size; }

		/**
		 * Method to tell whether this text Size is absolute or relative.
		 * @return true if this text size is absolute
		 */
		public boolean isAbsolute() { return absolute; }

		/**
		 * Method to tell whether this Size is the same as another.
		 * @return true if they are equal.
		 */
		public boolean equals(Size other)
		{
			if (this.absolute != other.absolute) return false;
			if (!DBMath.doublesEqual(this.size, other.size)) return false;
			return true;
		}

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
		private final int index;
		private final String name;
		private static List rotations = new ArrayList();

		private Rotation(int angle, int index, String name)
		{
			this.angle = angle;
			this.index = index;
			this.name = name;
			rotations.add(index, this);
		}

		/**
		 * Method to return the integer equivalent of this DispPos.
		 * This is zero-based.
		 * @return the integer equivalent of this DispPos.
		 */
		public int getIndex() { return index; }

		/**
		 * Method to return the description of this DispPos.
		 * It appears in popup menus.
		 * @return the description of this DispPos.
		 */
		public String getDescription() { return name; }

        /**
         * Get the angle of this rotation.
         * @return the angle of this rotation.
         */
        public int getAngle() { return angle; }

        /**
         * Get the Rotation for the given angle.
         * @param angle the angle.
         * @return a Rotation for the given angle, or null if non exists.
         */
        public static Rotation getRotation(int angle) {
            for (Iterator it = rotations.iterator(); it.hasNext(); ) {
                Rotation rot = (Rotation)it.next();
                if (rot.getAngle() == angle) return rot;
            }
            return null;
        }

		/**
		 * Method to return the number Rotations.
		 * @return the number Rotations.
		 */
		public static int getNumRotations() { return rotations.size(); }

		/**
		 * Method to return the Rotation at a given index.
		 * @param index the Rotation number desired.
		 * @return the Rotation at a given index.
		 */
		public static Rotation getRotationAt(int index) { return (Rotation)rotations.get(index); }

        /**
         * Get an iterator over all rotations
         * @return an iterator over all rotations
         */
        public static Iterator getRotations() { return rotations.iterator(); }

		/**
		 * Returns a printable version of this Rotation.
		 * @return a printable version of this Rotation.
		 */
		public String toString() { return "Text Rotation "+angle; }

		/** Describes a Rotation of 0 degrees. */	public static final Rotation ROT0 =
														new Rotation(0, 0, "None");
		/** Describes a Rotation of 90 degrees. */	public static final Rotation ROT90 =
														new Rotation(90, 1, "90 degrees counterclockwise");
		/** Describes a Rotation of 180 degrees. */	public static final Rotation ROT180 =
														new Rotation(180, 2, "180 degrees");
		/** Describes a Rotation of 270 degrees. */	public static final Rotation ROT270 =
														new Rotation(270, 3, "90 degrees clockwise");
	}


	/**
	 * Unit is a typesafe enum class that describes text's units on a Variable.
	 */
	public static class Unit
	{
		private final String name;
		private final int index;
		private static List units = new ArrayList();

		private Unit(String name, int index)
		{
			this.name = name;
			this.index = index;
			units.add(index, this);
		}

		/**
		 * Method to return the integer equivalent of this Unit.
		 * This is zero-based.
		 * @return the integer equivalent of this Unit.
		 */
		public int getIndex() { return index; }

		/**
		 * Method to return the description of this Unit.
		 * It appears in popup menus.
		 * @return the description of this Unit.
		 */
		public String getDescription() { return name; }

		/**
		 * Method to return the number Units.
		 * @return the number Units.
		 */
        public static int getNumUnits() { return units.size(); }

		/**
		 * Method to return the Unit at a given index.
		 * @param index the Unit number desired.
		 * @return the Unit at a given index.
		 */
        public static Unit getUnitAt(int index) { return (Unit)units.get(index); }

        /**
         * Get an iterator over all units.
         * @return an iterator over the list of unit types.
         */
        public static Iterator getUnits() { return units.iterator(); }

		/**
		 * Returns a printable version of this Unit.
		 * @return a printable version of this Unit.
		 */
		public String toString() { return name; }

		/** Describes no units. */				public static final Unit NONE =        new Unit("none", VTUNITSNONE);
		/** Describes resistance units. */		public static final Unit RESISTANCE =  new Unit("resistance", VTUNITSRES);
		/** Describes capacitance units. */		public static final Unit CAPACITANCE = new Unit("capacitance", VTUNITSCAP);
		/** Describes inductance units. */		public static final Unit INDUCTANCE =  new Unit("inductance", VTUNITSIND);
		/** Describes current units. */			public static final Unit CURRENT =     new Unit("current", VTUNITSCUR);
		/** Describes voltage units. */			public static final Unit VOLTAGE =     new Unit("voltage", VTUNITSVOLT);
		/** Describes distance units. */		public static final Unit DISTANCE =    new Unit("distance", VTUNITSDIST);
		/** Describes time units. */			public static final Unit TIME =        new Unit("time", VTUNITSTIME);
	}

	/**
	 * ActiveFont is a class that describes fonts currently in use.
	 */
	public static class ActiveFont
	{
		private String fontName;
		private int index;
		private static int indexCount = 0;
		private static HashMap fontMap = new HashMap();
		private static List fontList = new ArrayList();

		private ActiveFont(String fontName)
		{
			indexCount++;
			this.index = indexCount;
			this.fontName = fontName;
			fontMap.put(fontName, this);
			fontList.add(this);
		}

		/**
		 * Method to return the maximum index value for ActiveFonts.
		 * @return the maximum index value.
		 * ActiveFonts will have indices ranging from 1 to this value.
		 */
		public static int getMaxIndex() { return indexCount; }

		/**
		 * Method to return the index for this ActiveFont.
		 * @return the index of this ActiveFont.
		 * The index value is 1-based, because font 0 is the "default font".
		 */
		public int getIndex() { return index; }

		/**
		 * Method to return the font name associated with this ActiveFont.
		 * @return the font name associated with this ActiveFont.
		 */
		public String getName() { return fontName; }

		/**
		 * Method to return the ActiveFont with a given name.
		 * @param fontName the name of the font.
		 * @return an ActiveFont object.  If there is no ActiveFont
		 * associated with this fontname, one is created.
		 */
		public static ActiveFont findActiveFont(String fontName)
		{
			ActiveFont af = (ActiveFont)fontMap.get(fontName);
			if (af == null)
				af = new ActiveFont(fontName);
			return af;
		}

		/**
		 * Method to return the ActiveFont with a given index.
		 * @param index the index number (1-based) of the ActiveFont.
		 * @return the ActiveFont with this index.  Returns null if there is none.
		 */
		public static ActiveFont findActiveFont(int index)
		{
			if (index <= 0) return null;
			if (index > fontList.size()) return null;
			ActiveFont af = (ActiveFont)fontList.get(index-1);
			return af;
		}

		/**
		 * Returns a printable version of this ActiveFont.
		 * @return a printable version of this ActiveFont.
		 */
		public String toString() { return fontName; }
	}


	/** the words of the text descriptor */		private int descriptor0, descriptor1;
	/** the color of the text descriptor */		private int colorIndex;
	/** the owner of the text descriptor */		ElectricObject owner;

	/** preferences for all descriptors */	private static Preferences prefs = Preferences.userNodeForPackage(TextDescriptor.class);

	/**
	 * The constructor simply creates a TextDescriptor with zero values filled-in.
	 * @param owner owner of this TextDescriptor.
	 */
	public TextDescriptor(ElectricObject owner)
	{
		this.owner = owner;
		this.descriptor0 = this.descriptor1 = 0;
		this.colorIndex = 0;
	}

	/**
	 * The constructor creates copy of anotherTextDescriptor.
	 * @param owner owner of this TextDescriptor.
	 * @param descriptor another descriptor.
	 */
	public TextDescriptor(ElectricObject owner, TextDescriptor descriptor)
	{
		this.owner = owner;
		this.descriptor0 = descriptor.descriptor0;
		this.descriptor1 = descriptor.descriptor1;
		this.colorIndex = descriptor.colorIndex;
	}

	/**
	 * The constructor simply creates a TextDescriptor with specified values.
	 * @param owner owner of this TextDescriptor.
	 * @param descriptor0 lower word of the text descriptor.
	 * @param descriptor1 higher word of the text descriptor.
	 */
	public TextDescriptor(ElectricObject owner, int descriptor0, int descriptor1, int colorIndex)
	{
		this.owner = owner;
		this.descriptor0 = descriptor0;
		this.descriptor1 = descriptor1;
		this.colorIndex = colorIndex;
	}

	/**
	 * The constructor simply creates a TextDescriptor with specified values.
	 * @param owner owner of this TextDescriptor.
	 * @param descriptor the bits of the text descriptor.
	 */
	public TextDescriptor(ElectricObject owner, long descriptor, int colorIndex)
	{
		this.owner = owner;
		this.descriptor0 = (int)(descriptor >> 32);
		this.descriptor1 = (int)(descriptor & 0xFFFFFFFF);
		this.colorIndex = colorIndex;
	}

	/**
	 * Default TextDescriptor for NodeInsts is 1 unit tall.
	 */
	private static Pref cacheNodeDescriptor = Pref.makeLongPref("TextDescriptorForNode", prefs, (4 << TXTQGRIDSH) << VTSIZESH);
	/**
	 * Method to return a TextDescriptor that is a default for Variables on NodeInsts.
	 * @param owner owner of this TextDescriptor.
	 * @return a new TextDescriptor that can be stored in a Variable on a NodeInsts.
	 */
	public static TextDescriptor getNodeTextDescriptor(ElectricObject owner)
	{
		TextDescriptor td = new TextDescriptor(owner, cacheNodeDescriptor.getLong(), 0);
		String fontName = getNodeTextDescriptorFont();
		if (fontName.length() > 0) td.setFace(ActiveFont.findActiveFont(fontName).getIndex());
		return td;
	}
	/**
	 * Method to set a TextDescriptor that is a default for Variables on NodeInsts.
	 * @param td the default TextDescriptor for Variables on NodeInsts.
	 */
	public static void setNodeTextDescriptor(TextDescriptor td) { cacheNodeDescriptor.setLong(td.lowLevelGet()); }

	private static Pref cacheNodeFont = Pref.makeStringPref("TextDescriptorFontForNode", prefs, "");
	/**
	 * Method to return the name of the default font for Variables on NodeInsts.
	 * @return the name of the default font for Variables on NodeInsts.
	 */
	public static String getNodeTextDescriptorFont() { return cacheNodeFont.getString(); }
	/**
	 * Method to set the name of the default font for Variables on NodeInsts.
	 * @param font the name of the default font for Variables on NodeInsts.
	 */
	public static void setNodeTextDescriptorFont(String font) { cacheNodeFont.setString(font); }

	/**
	 * Default TextDescriptor for ArcInsts is 1 unit tall.
	 */
	private static Pref cacheArcDescriptor = Pref.makeLongPref("TextDescriptorForArc", prefs, (4 << TXTQGRIDSH) << VTSIZESH);
	/**
	 * Method to return a TextDescriptor that is a default for Variables on ArcInsts.
	 * @param owner owner of this TextDescriptor.
	 * @return a new TextDescriptor that can be stored in a Variable on a ArcInsts.
	 */
	public static TextDescriptor getArcTextDescriptor(ElectricObject owner)
	{
		TextDescriptor td = new TextDescriptor(owner, cacheArcDescriptor.getLong(), 0);
		String fontName = getArcTextDescriptorFont();
		if (fontName.length() > 0) td.setFace(ActiveFont.findActiveFont(fontName).getIndex());
		return td;
	}
	/**
	 * Method to set a TextDescriptor that is a default for Variables on ArcInsts.
	 * @param td the default TextDescriptor for Variables on ArcInsts.
	 */
	public static void setArcTextDescriptor(TextDescriptor td) { cacheArcDescriptor.setLong(td.lowLevelGet()); }

	private static Pref cacheArcFont = Pref.makeStringPref("TextDescriptorFontForArc", prefs, "");
	/**
	 * Method to return the name of the default font for Variables on ArcInsts.
	 * @return the name of the default font for Variables on ArcInsts.
	 */
	public static String getArcTextDescriptorFont() { return cacheArcFont.getString(); }
	/**
	 * Method to set the name of the default font for Variables on ArcInsts.
	 * @param font the name of the default font for Variables on ArcInsts.
	 */
	public static void setArcTextDescriptorFont(String font) { cacheArcFont.setString(font); }

	/**
	 * Default TextDescriptor for Exports and Ports is 2 units tall.
	 */
	private static Pref cacheExportDescriptor = Pref.makeLongPref("TextDescriptorForExport", prefs, (8 << TXTQGRIDSH) << VTSIZESH);
	/**
	 * Method to return a TextDescriptor that is a default for Variables on Exports.
	 * @param owner owner of this TextDescriptor.
	 * @return a new TextDescriptor that can be stored in a Variable on a Exports.
	 */
	public static TextDescriptor getExportTextDescriptor(ElectricObject owner)
	{
		TextDescriptor td = new TextDescriptor(owner, cacheExportDescriptor.getLong(), 0);
		String fontName = getExportTextDescriptorFont();
		if (fontName.length() > 0) td.setFace(ActiveFont.findActiveFont(fontName).getIndex());
		return td;
	}
	/**
	 * Method to set a TextDescriptor that is a default for Variables on Exports.
	 * @param td the default TextDescriptor for Variables on Exports.
	 */
	public static void setExportTextDescriptor(TextDescriptor td) { cacheExportDescriptor.setLong(td.lowLevelGet()); }

	private static Pref cacheExportFont = Pref.makeStringPref("TextDescriptorFontForExport", prefs, "");
	/**
	 * Method to return the name of the default font for Variables on Exports.
	 * @return the name of the default font for Variables on Exports.
	 */
	public static String getExportTextDescriptorFont() { return cacheExportFont.getString(); }
	/**
	 * Method to set the name of the default font for Variables on Exports.
	 * @param font the name of the default font for Variables on Exports.
	 */
	public static void setExportTextDescriptorFont(String font) { cacheExportFont.setString(font); }

	/**
	 * Default TextDescriptor for Annotations is 1 unit tall.
	 */
	private static Pref cacheAnnotationDescriptor = Pref.makeLongPref("TextDescriptorForAnnotation", prefs, (4 << TXTQGRIDSH) << VTSIZESH);
	/**
	 * Method to return a TextDescriptor that is a default for Variables on Annotations.
	 * @param owner owner of this TextDescriptor.
	 * @return a new TextDescriptor that can be stored in a Variable on a Annotations.
	 */
	public static TextDescriptor getAnnotationTextDescriptor(ElectricObject owner)
	{
		TextDescriptor td = new TextDescriptor(owner, cacheAnnotationDescriptor.getLong(), 0);
		String fontName = getAnnotationTextDescriptorFont();
		if (fontName.length() > 0) td.setFace(ActiveFont.findActiveFont(fontName).getIndex());
		return td;
	}
	/**
	 * Method to set a TextDescriptor that is a default for Variables on Annotations.
	 * @param td the default TextDescriptor for Variables on Annotations.
	 */
	public static void setAnnotationTextDescriptor(TextDescriptor td) { cacheAnnotationDescriptor.setLong(td.lowLevelGet()); }

	private static Pref cacheAnnotationFont = Pref.makeStringPref("TextDescriptorFontForAnnotation", prefs, "");
	/**
	 * Method to return the name of the default font for Variables on Annotations.
	 * @return the name of the default font for Variables on Annotations.
	 */
	public static String getAnnotationTextDescriptorFont() { return cacheAnnotationFont.getString(); }
	/**
	 * Method to set the name of the default font for Variables on Annotations.
	 * @param font the name of the default font for Variables on Annotations.
	 */
	public static void setAnnotationTextDescriptorFont(String font) { cacheAnnotationFont.setString(font); }

	/**
	 * Default TextDescriptor for Cell Instance Names is 4 units tall.
	 */
	private static Pref cacheInstanceDescriptor = Pref.makeLongPref("TextDescriptorForInstance", prefs, (16 << TXTQGRIDSH) << VTSIZESH);
	/**
	 * Method to return a TextDescriptor that is a default for Variables on Cell Instance Names.
	 * @param owner owner of this TextDescriptor.
	 * @return a new TextDescriptor that can be stored in a Variable on a Cell Instance Names.
	 */
	public static TextDescriptor getInstanceTextDescriptor(ElectricObject owner)
	{
		TextDescriptor td = new TextDescriptor(owner, cacheInstanceDescriptor.getLong(), 0);
		String fontName = getInstanceTextDescriptorFont();
		if (fontName.length() > 0) td.setFace(ActiveFont.findActiveFont(fontName).getIndex());
		return td;
	}
	/**
	 * Method to set a TextDescriptor that is a default for Variables on Cell Instance Names.
	 * @param td the default TextDescriptor for Variables on Cell Instance Names.
	 */
	public static void setInstanceTextDescriptor(TextDescriptor td) { cacheInstanceDescriptor.setLong(td.lowLevelGet()); }

	private static Pref cacheInstanceFont = Pref.makeStringPref("TextDescriptorFontForInstance", prefs, "");
	/**
	 * Method to return the name of the default font for Variables on Cell Instance Names.
	 * @return the name of the default font for Variables on Cell Instance Names.
	 */
	public static String getInstanceTextDescriptorFont() { return cacheInstanceFont.getString(); }
	/**
	 * Method to set the name of the default font for Variables on Cell Instance Names.
	 * @param font the name of the default font for Variables on Cell Instance Names.
	 */
	public static void setInstanceTextDescriptorFont(String font) { cacheInstanceFont.setString(font); }

	/**
	 * Default TextDescriptor for Cell Variables is 1 unit tall.
	 */
	private static Pref cacheCellDescriptor = Pref.makeLongPref("TextDescriptorForCell", prefs, (4 << TXTQGRIDSH) << VTSIZESH);
	/**
	 * Method to return a TextDescriptor that is a default for Variables on Cell Variables.
	 * @param owner owner of this TextDescriptor.
	 * @return a new TextDescriptor that can be stored in a Variable on a Cell Variables.
	 */
	public static TextDescriptor getCellTextDescriptor(ElectricObject owner)
	{
		TextDescriptor td = new TextDescriptor(owner, cacheCellDescriptor.getLong(), 0);
		String fontName = getCellTextDescriptorFont();
		if (fontName.length() > 0) td.setFace(ActiveFont.findActiveFont(fontName).getIndex());
		return td;
	}
	/**
	 * Method to set a TextDescriptor that is a default for Variables on Cell Variables.
	 * @param td the default TextDescriptor for Variables on Cell Variables.
	 */
	public static void setCellTextDescriptor(TextDescriptor td) { cacheCellDescriptor.setLong(td.lowLevelGet()); }

	private static Pref cacheCellFont = Pref.makeStringPref("TextDescriptorFontForCell", prefs, "");
	/**
	 * Method to return the name of the default font for Variables on Cell Variables.
	 * @return the name of the default font for Variables on Cell Variables.
	 */
	public static String getCellTextDescriptorFont() { return cacheCellFont.getString(); }
	/**
	 * Method to set the name of the default font for Variables on Cell Variables.
	 * @param font the name of the default font for Variables on Cell Variables.
	 */
	public static void setCellTextDescriptorFont(String font) { cacheCellFont.setString(font); }

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
			return descriptor0 == td.descriptor0 && descriptor1 == td.descriptor1 &&
			colorIndex == td.colorIndex;
		}
		return false;
    }

	/**
	 * Method checks if text descriptor can be changed.
	 */
	private void checkChanging()
	{
		if (owner == null || owner.isDummyObject()) return;
		owner.checkChanging();

		// handle change control, constraint, and broadcast
		Undo.modifyTextDescript(owner, this, descriptor0, descriptor1, colorIndex);
	}

	/**
	 * Method to set "smart text placement" on this TextDescriptor.
	 * Smart text placement puts the anchor point in a sensible location
	 * for NodeInsts and ArcInsts.
	 */
	public void setSmartPlacement()
	{
		if (!(owner instanceof Export)) return;

		// handle smart text placement relative to attached object
		int smartVertical = User.getSmartVerticalPlacement();
		int smartHorizontal = User.getSmartHorizontalPlacement();
		if (smartVertical == 0 && smartHorizontal == 0) return;

		// figure out location of object relative to environment
		double dx = 0, dy = 0;
		Export pp = (Export)owner;
		PortInst pi = pp.getOriginalPort();
		NodeInst ni = pi.getNodeInst();
		Rectangle2D nodeBounds = ni.getBounds();
		for(Iterator it = ni.getConnections(); it.hasNext(); )
		{
			Connection con = (Connection)it.next();
			if (con.getPortInst() == pi)
			{
				ArcInst ai = con.getArc();
				Rectangle2D arcBounds = ai.getBounds();
				dx = arcBounds.getCenterX() - nodeBounds.getCenterX();
				dy = arcBounds.getCenterY() - nodeBounds.getCenterY();
			}
		}

		// first move placement horizontally
		boolean goleft = false, goright = false, goup = false, godown = false;
		if (smartHorizontal == 1)
		{
			// place label inside (towards center)
			if (dx > 0) goright = true; else
				if (dx < 0) goleft = true;
		} else if (smartHorizontal == 2)
		{
			// place label outside (away from center)
			if (dx > 0) goleft = true; else
				if (dx < 0) goright = true;
		}

		// next move placement vertically
		if (smartVertical == 1)
		{
			// place label inside (towards center)
			if (dy > 0) goup = true; else
				if (dy < 0) godown = true;
		} else if (smartVertical == 2)
		{
			// place label outside (away from center)
			if (dy > 0) godown = true; else
				if (dy < 0) goup = true;
		}
		if (goleft)
		{
			if (getPos() == Position.CENT || getPos() == Position.RIGHT || getPos() == Position.LEFT)
			{
				setPos(Position.LEFT);
			} else if (getPos() == Position.UP || getPos() == Position.UPRIGHT || getPos() == Position.UPLEFT)
			{
				setPos(Position.UPLEFT);
			} else if (getPos() == Position.DOWN || getPos() == Position.DOWNRIGHT || getPos() == Position.DOWNLEFT)
			{
				setPos(Position.DOWNLEFT);
			}
		}
		if (goright)
		{
			if (getPos() == Position.CENT || getPos() == Position.RIGHT || getPos() == Position.LEFT)
			{
				setPos(Position.RIGHT);
			} else if (getPos() == Position.UP || getPos() == Position.UPRIGHT || getPos() == Position.UPLEFT)
			{
				setPos(Position.UPRIGHT);
			} else if (getPos() == Position.DOWN || getPos() == Position.DOWNRIGHT || getPos() == Position.DOWNLEFT)
			{
				setPos(Position.DOWNRIGHT);
			}
		}
		if (goup)
		{
			if (getPos() == Position.CENT || getPos() == Position.UP || getPos() == Position.DOWN)
			{
				setPos(Position.UP);
			} else if (getPos() == Position.RIGHT || getPos() == Position.UPRIGHT || getPos() == Position.DOWNRIGHT)
			{
				setPos(Position.UPRIGHT);
			} else if (getPos() == Position.LEFT || getPos() == Position.UPLEFT || getPos() == Position.DOWNLEFT)
			{
				setPos(Position.UPLEFT);
			}
		}
		if (godown)
		{
			if (getPos() == Position.CENT || getPos() == Position.UP || getPos() == Position.DOWN)
			{
				setPos(Position.DOWN);
			} else if (getPos() == Position.RIGHT || getPos() == Position.UPRIGHT || getPos() == Position.DOWNRIGHT)
			{
				setPos(Position.DOWNRIGHT);
			} else if (getPos() == Position.LEFT || getPos() == Position.UPLEFT || getPos() == Position.DOWNLEFT)
			{
				setPos(Position.DOWNLEFT);
			}
		}
	}

	/**
	 * Low-level method to set the bits in the TextDescriptor.
	 * These bits are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the ELIB
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @param descriptor0 the first word of the new TextDescriptor.
	 * @param descriptor1 the second word of the new TextDescriptor.
	 */
	public void lowLevelSet(int descriptor0, int descriptor1)
	{
		this.descriptor0 = descriptor0;
		this.descriptor1 = descriptor1;
	}

	/**
	 * Low-level method to set the bits in the TextDescriptor.
	 * These bits are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the ELIB
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @param descriptor1 the bits of the new TextDescriptor.
	 */
	public void lowLevelSet(long descriptor)
	{
		this.descriptor0 = (int)(descriptor >> 32);
		this.descriptor1 = (int)(descriptor & 0xFFFFFFFF);
	}

	/**
	 * Method to copy another TextDescriptor to this TextDescriptor.
	 * These bits are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the ELIB
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @param descriptor other TextDescriptor.
	 */
	public void copy(TextDescriptor descriptor)
	{
		checkChanging();
		this.descriptor0 = descriptor.descriptor0;
		this.descriptor1 = descriptor.descriptor1;
		this.colorIndex = descriptor.colorIndex;
	}

	/**
	 * Method to compare another TextDescriptor to this TextDescriptor.
	 * @param descriptor the other TextDescriptor.
	 * @return true if they are the same; false if they differ.
	 */
	public boolean compare(TextDescriptor descriptor)
	{
		if (this.descriptor0 == descriptor.descriptor0 &&
			this.descriptor1 == descriptor.descriptor1 &&
			this.colorIndex == descriptor.colorIndex) return true;
		return false;
	}

	/**
	 * Low-level method to get the first word of the bits in the TextDescriptor.
	 * These bits are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the ELIB
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @return the first word of the bits in the TextDescriptor.
	 */
	public int lowLevelGet0() { return descriptor0; }

	/**
	 * Low-level method to get the second word of the bits in the TextDescriptor.
	 * These bits are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the ELIB
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @return the second word of the bits in the TextDescriptor.
	 */
	public int lowLevelGet1() { return descriptor1; }

	/**
	 * Low-level method to get the bits in the TextDescriptor.
	 * These bits are a collection of flags that are more sensibly accessed
	 * through special methods.
	 * This general access to the bits is required because the ELIB
	 * file format stores it as a full integer.
	 * This should not normally be called by any other part of the system.
	 * @return the bits in the TextDescriptor.
	 */
	public long lowLevelGet() { return ((long)descriptor0 << 32) | descriptor1; }

	/**
	 * Method to return the color index of the TextDescriptor.
	 * Color indices are more general than colors, because they can handle
	 * transparent layers, C-Electric-style opaque layers, and full color values.
	 * Methods in "EGraphics" manipulate color indices.
	 * @return the color index of the TextDescriptor.
	 */
	public int getColorIndex() { return colorIndex; }

	/**
	 * Method to set the color index of the TextDescriptor.
	 * Color indices are more general than colors, because they can handle
	 * transparent layers, C-Electric-style opaque layers, and full color values.
	 * Methods in "EGraphics" manipulate color indices.
	 * @param colorIndex the color index of the TextDescriptor.
	 */
	public void setColorIndex(int colorIndex)
	{
		checkChanging();
		this.colorIndex = colorIndex;
	}

	/**
	 * Method to return the text position of the TextDescriptor.
	 * The text position describes the "anchor point" of the text,
	 * which is the point on the text that is attached to the object and does not move.
	 * @return the text position of the TextDescriptor.
	 */
	public Position getPos()
	{
		int pos = descriptor0 & VTPOSITION;
		if (pos >= Position.getNumPositions()) pos = 0;
		return Position.getPositionAt(pos);
	}

	/**
	 * Method to set the text position of the TextDescriptor.
	 * The text position describes the "anchor point" of the text,
	 * which is the point on the text that is attached to the object and does not move.
	 * @param p the text position of the TextDescriptor.
	 */
	public void setPos(Position p)
	{
		checkChanging();
		descriptor0 = (descriptor0 & ~VTPOSITION) | p.getIndex();
	}

	/**
	 * Method to return the text size of the text in this TextDescriptor.
	 * This is a Size object that can describe either absolute text (in points)
	 * or relative text (in quarter units).
	 * @return the text size of the text in this TextDescriptor.
	 */
	public Size getSize()
	{
		int textSize = (descriptor1 & VTSIZE) >> VTSIZESH;
		if (textSize == 0) return Size.newRelSize(1);
		if (textSize <= TXTMAXPOINTS) return Size.newAbsSize(textSize);
		int sizeValue = textSize>>TXTQGRIDSH;
		double size = sizeValue / 4.0;
		return Size.newRelSize(size);
	}

	/**
	 * Method to find the true size in points for this TextDescriptor in a given EditWindow.
	 * If the TextDescriptor is already Absolute (in points) nothing needs to be done.
	 * Otherwise, the scale of the EditWindow is used to determine the acutal point size.
	 * @param wnd the EditWindow in which drawing will occur.
	 * @return the point size of the text described by this TextDescriptor.
	 */
	public int getTrueSize(EditWindow wnd)
	{
		Size s = getSize();
		if (s == null) return 14;

		// absolute font sizes are easy
		if (s.isAbsolute()) return (int)s.getSize();

		// relative font: get size in grid units
		double height = s.getSize();

		// convert to screen units
		return wnd.getTextPointSize(height);
	}

	/**
	 * Method to set the text size of this TextDescriptor to an absolute size (in points).
	 * The size must be between 1 and 63 points.
	 * @param s the point size of this TextDescriptor.
	 */
	public void setAbsSize(int s)
	{
		Size size = Size.newAbsSize(s);
		if (size == null) return;
		if (owner != null) checkChanging();
		descriptor1 = (descriptor1 & ~VTSIZE) | (size.getBits() << VTSIZESH);
	}

	/**
	 * Method to set the text size of this TextDescriptor to a relative size (in units).
	 * The size must be between 0.25 and 127.75 grid units (in .25 increments).
	 * @param s the unit size of this TextDescriptor.
	 */
	public void setRelSize(double s)
	{
		Size size = Size.newRelSize(s);
		if (size == null) return;
		if (owner != null) checkChanging();
		descriptor1 = (descriptor1 & ~VTSIZE) | (size.getBits() << VTSIZESH);
	}

	/**
	 * Method to return the text font of the TextDescriptor.
	 * @return the text font of the TextDescriptor.
	 */
	public int getFace() { return (descriptor1 & VTFACE) >> VTFACESH; }

	/**
	 * Method to set the text font of the TextDescriptor.
	 * @param f the text font of the TextDescriptor.
	 */
	public void setFace(int f)
	{
		checkChanging();
		descriptor1 = (descriptor1 & ~VTFACE) | (f << VTFACESH);
	}

	/**
	 * Method to return the text rotation of the TextDescriptor.
	 * There are only 4 rotations: 0, 90 degrees, 180 degrees, and 270 degrees.
	 * @return the text rotation of the TextDescriptor.
	 */
	public Rotation getRotation() { return Rotation.getRotationAt((descriptor1 & VTROTATION) >> VTROTATIONSH); }

	/**
	 * Method to set the text rotation of the TextDescriptor.
	 * There are only 4 rotations: 0, 90 degrees, 180 degrees, and 270 degrees.
	 * @param r the text rotation of the TextDescriptor.
	 */
	public void setRotation(Rotation r)
	{
		checkChanging();
		descriptor1 = (descriptor1 & ~VTROTATION) | (r.getIndex() << VTROTATIONSH);
	}

	/**
	 * Method to return the text display part of the TextDescriptor.
	 * @return the text display part of the TextDescriptor.
	 */
	public DispPos getDispPart() { return DispPos.getShowStylesAt((descriptor0 & VTDISPLAYPART) >> VTDISPLAYPARTSH); }

	/**
	 * Method to set the text display part of the TextDescriptor.
	 * @param d the text display part of the TextDescriptor.
	 */
	public void setDispPart(DispPos d)
	{
		checkChanging();
		descriptor0 = (descriptor0 & ~VTDISPLAYPART) | (d.getIndex() << VTDISPLAYPARTSH);
	}

	/**
	 * Method to return true if the text in the TextDescriptor is italic.
	 * @return true if the text in the TextDescriptor is italic.
	 */
	public boolean isItalic() { return (descriptor0 & VTITALIC) != 0; }

	/**
	 * Method to set the text in the TextDescriptor to be italic.
	 */
	public void setItalic(boolean state)
	{
		checkChanging();
        if (state)
		    descriptor0 |= VTITALIC;
        else
            descriptor0 &= ~VTITALIC;
	}

	/**
	 * Method to return true if the text in the TextDescriptor is bold.
	 * @return true if the text in the TextDescriptor is bold.
	 */
	public boolean isBold() { return (descriptor0 & VTBOLD) != 0; }

	/**
	 * Method to set the text in the TextDescriptor to be bold.
	 */
	public void setBold(boolean state)
	{
		checkChanging();
        if (state)
		    descriptor0 |= VTBOLD;
        else
            descriptor0 &= ~VTBOLD;
	}

	/**
	 * Method to return true if the text in the TextDescriptor is underlined.
	 * @return true if the text in the TextDescriptor is underlined.
	 */
	public boolean isUnderline() { return (descriptor0 & VTUNDERLINE) != 0; }

	/**
	 * Method to set the text in the TextDescriptor to be underlined.
	 */
	public void setUnderline(boolean state)
	{
		checkChanging();
		if (state)
            descriptor0 |= VTUNDERLINE;
        else
            descriptor0 &= ~VTUNDERLINE;
	}

	/**
	 * Method to return true if the text in the TextDescriptor is interior.
	 * Interior text is not seen at higher levels of the hierarchy.
	 * @return true if the text in the TextDescriptor is interior.
	 */
	public boolean isInterior() { return (descriptor0 & VTINTERIOR) != 0; }

	/**
	 * Method to set the text in the TextDescriptor to be interior.
	 * Interior text is not seen at higher levels of the hierarchy.
	 */
	public void setInterior(boolean state)
	{
		checkChanging();
		if (state)
            descriptor0 |= VTINTERIOR;
        else
            descriptor0 &= ~VTINTERIOR;
	}

	/**
	 * Method to return true if the text in the TextDescriptor is inheritable.
	 * Inheritable variables copy their contents from prototype to instance.
	 * Only Variables on NodeProto and PortProto objects can be inheritable.
	 * When a NodeInst is created, any inheritable Variables on its NodeProto are automatically
	 * created on that NodeInst.
	 * @return true if the text in the TextDescriptor is inheritable.
	 */
	public boolean isInherit() { return (descriptor0 & VTINHERIT) != 0; }

	/**
	 * Method to set the text in the TextDescriptor to be inheritable.
	 * Inheritable variables copy their contents from prototype to instance.
	 * Only Variables on NodeProto and PortProto objects can be inheritable.
	 * When a NodeInst is created, any inheritable Variables on its NodeProto are automatically
	 * created on that NodeInst.
	 */
	public void setInherit(boolean state)
	{
		checkChanging();
		if (state)
            descriptor0 |= VTINHERIT;
        else
            descriptor0 &= ~VTINHERIT;
	}

	/**
	 * Method to return true if the text in the TextDescriptor is a parameter.
	 * Parameters are those Variables that have values on instances which are
	 * passed down the hierarchy into the contents.
	 * Parameters can only exist on NodeInst objects.
	 * @return true if the text in the TextDescriptor is a parameter.
	 */
	public boolean isParam() { return (descriptor0 & VTISPARAMETER) != 0; }

	/**
	 * Method to set the text in the TextDescriptor to be a parameter.
	 * Parameters are those Variables that have values on instances which are
	 * passed down the hierarchy into the contents.
	 * Parameters can only exist on NodeInst objects.
	 */
	public void setParam(boolean state)
	{
		checkChanging();
		if (state)
            descriptor0 |= VTISPARAMETER;
        else
            descriptor0 &= ~VTISPARAMETER;
	}

	/**
	 * Method to return the X offset of the text in the TextDescriptor.
	 * @return the X offset of the text in the TextDescriptor.
	 */
	public double getXOff()
	{
		int offset = (descriptor0 & VTXOFF) >> VTXOFFSH;
		if ((descriptor0&VTXOFFNEG) != 0) offset = -offset;
		int scale = getOffScale() + 1;
		return((double)offset * scale / 4);
	}

	/**
	 * Method to return the Y offset of the text in the TextDescriptor.
	 * @return the Y offset of the text in the TextDescriptor.
	 */
	public double getYOff()
	{
		int offset = (descriptor0 & VTYOFF) >> VTYOFFSH;
		if ((descriptor0&VTYOFFNEG) != 0) offset = -offset;
		int scale = getOffScale() + 1;
		return((double)offset * scale / 4);
	}

	/**
	 * Method to set the X and Y offsets of the text in the TextDescriptor.
	 * The values are scaled by 4, so a value of 3 indicates a shift of 0.75 and a value of 4 shifts by 1.
	 * @param xd the X offset of the text in the TextDescriptor.
	 * @param yd the Y offset of the text in the TextDescriptor.
	 */
	public void setOff(double xd, double yd)
	{
		checkChanging();
		int x = (int)(xd * 4);
		int y = (int)(yd * 4);
		descriptor0 &= ~(VTXOFF|VTYOFF|VTXOFFNEG|VTYOFFNEG);
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
		descriptor0 |= (x << VTXOFFSH) & VTXOFF;
		descriptor0 |= (y << VTYOFFSH) & VTYOFF;
		//setOffScale(scale);
		descriptor1 = (descriptor1 & ~VTOFFSCALE) | ((scale << VTOFFSCALESH) & VTOFFSCALE);
	}

	/** Method to return the offset scale of the text in the text descriptor. */
	private int getOffScale() { return (descriptor1 & VTOFFSCALE) >> VTOFFSCALESH; }

	/**
	 * Method to return the Unit of the TextDescriptor.
	 * Unit describes the type of real-world unit to apply to the value.
	 * For example, if this value is in volts, the Unit tells whether the value
	 * is volts, millivolts, microvolts, etc.
	 * @return the Unit of the TextDescriptor.
	 */
	public Unit getUnit() { return Unit.getUnitAt(((descriptor1 & VTUNITS) >> VTUNITSSH) & VTUNITSHMASK); }

	/**
	 * Method to set the Unit of the TextDescriptor.
	 * Unit describe the type of real-world unit to apply to the value.
	 * For example, if this value is in volts, the Unit tells whether the value
	 * is volts, millivolts, microvolts, etc.
	 * @param u the Unit of the TextDescriptor.
	 */
	public void setUnit(Unit u)
	{
		checkChanging();
		descriptor1 = (descriptor1 & ~VTUNITS) | (u.getIndex() << VTUNITSSH);
	}
}
