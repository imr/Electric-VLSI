/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ELIBConstants.java
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
package com.sun.electric.tool.io;

import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.tool.Tool;
import com.sun.electric.technology.Technology;

import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * This class has constants for reading and writing binary (.elib) files.
 */
public class ELIBConstants
{
	// ".elib" file version numbers
	/** current magic number: version 13 */		public static final int MAGIC13 = -1597;
	/** older magic number: version 12 */		public static final int MAGIC12 = -1595;
	/** older magic number: version 11 */		public static final int MAGIC11 = -1593;
	/** older magic number: version 10 */		public static final int MAGIC10 = -1591;
	/** older magic number: version 9 */		public static final int MAGIC9 =  -1589;
	/** older magic number: version 8 */		public static final int MAGIC8 =  -1587;
	/** older magic number: version 7 */		public static final int MAGIC7 =  -1585;
	/** older magic number: version 6 */		public static final int MAGIC6 =  -1583;
	/** older magic number: version 5 */		public static final int MAGIC5 =  -1581;
	/** older magic number: version 4 */		public static final int MAGIC4 =  -1579;
	/** older magic number: version 3 */		public static final int MAGIC3 =  -1577;
	/** older magic number: version 2 */		public static final int MAGIC2 =  -1575;
	/** oldest magic number: version 1 */		public static final int MAGIC1 =  -1573;

	// bits found in the "type" field of a Variable:
	/** Defines an undefined variable. */										public static final int VUNKNOWN =                  0;
	/** Defines a 32-bit integer variable. */									public static final int VINTEGER =                 01;
	/** Defines an unsigned address. */											public static final int VADDRESS =                 02;
	/** Defines a character variable. */										public static final int VCHAR =                    03;
	/** Defines a string variable. */											public static final int VSTRING =                  04;
	/** Defines a floating point variable. */									public static final int VFLOAT =                   05;
	/** Defines a double-precision floating point. */							public static final int VDOUBLE =                  06;
	/** Defines a nodeinst pointer. */											public static final int VNODEINST =                07;
	/** Defines a nodeproto pointer. */											public static final int VNODEPROTO =              010;
	/** Defines a portarcinst pointer. */										public static final int VPORTARCINST =            011;
	/** Defines a portexpinst pointer. */										public static final int VPORTEXPINST =            012;
	/** Defines a portproto pointer. */											public static final int VPORTPROTO =              013;
	/** Defines an arcinst pointer. */											public static final int VARCINST =                014;
	/** Defines an arcproto pointer. */											public static final int VARCPROTO =               015;
	/** Defines a geometry pointer. */											public static final int VGEOM =                   016;
	/** Defines a library pointer. */											public static final int VLIBRARY =                017;
	/** Defines a technology pointer. */										public static final int VTECHNOLOGY =             020;
	/** Defines a tool pointer. */												public static final int VTOOL =                   021;
	/** Defines an R-tree pointer. */											public static final int VRTNODE =                 022;
	/** Defines a fractional integer (scaled by WHOLE). */						public static final int VFRACT =                  023;
	/** Defines a network pointer. */											public static final int VNETWORK =                024;
	/** Defines a view pointer. */												public static final int VVIEW =                   026;
	/** Defines a window partition pointer. */									public static final int VWINDOWPART =             027;
	/** Defines a graphics object pointer. */									public static final int VGRAPHICS =               030;
	/** Defines a 16-bit integer. */											public static final int VSHORT =                  031;
	/** Defines a constraint solver. */											public static final int VCONSTRAINT =             032;
	/** Defines a general address/type pairs (only in fixed-length arrays). */	public static final int VGENERAL =                033;
	/** Defines a window frame pointer. */										public static final int VWINDOWFRAME =            034;
	/** Defines a polygon pointer. */											public static final int VPOLYGON =                035;
	/** Defines a boolean variable. */											public static final int VBOOLEAN =                036;
	/** Defines all above type fields. */										public static final int VTYPE =                   037;
	/** Defines whether the variable is interpreted code (with VCODE2). */		public static final int VCODE1 =                  040;
	/** Set if the variable is displayable (uses textdescript field). */		public static final int VDISPLAY =               0100;
	/** Set if variable is an array of objects. */								public static final int VISARRAY =               0200;
	/** Defines the array length (0: array is -1 terminated). */				public static final int VLENGTH =         03777777000;
	/** Defines the right shift for VLENGTH. */									public static final int VLENGTHSH =                 9;
	/** Defines whether the variable is interpreted code (with VCODE1). */		public static final int VCODE2 =          04000000000;

	// some bits of TextDescriptor of a Variable:
	/** Semantic bits in descriptor 0 (VTISPARAMETER|VTINTERIOR|VTINHERIT). */	public static final int VTSEMANTIC0 =           07000;
	/** Semantic bits in descriptor 1 (VTUNITS). */								public static final int VTSEMANTIC1 =    034000000000;
	/** Face of text bits in descriptor 1. */									public static final int VTFACE =            017700000;
	/** Right shift of VTFACE. */												public static final int VTFACESH =                 15;

	// Cell userbits
	/** set if instances should be expanded */						private static final int WANTNEXPAND   =           02;
	/** set if everything in cell is locked */						private static final int NPLOCKED      =     04000000;
	/** set if instances in cell are locked */						private static final int NPILOCKED     =    010000000;
	/** set if cell is part of a "cell library" */					private static final int INCELLLIBRARY =    020000000;
	/** set if cell is from a technology-library */					private static final int TECEDITCELL   =    040000000;
	/** set if cell is a multi-page schematic */					private static final int MULTIPAGE     = 017600000000;
	public static final int CELL_BITS = WANTNEXPAND | NPLOCKED | NPILOCKED | INCELLLIBRARY | TECEDITCELL /* | MULTIPAGE*/;

	/** set if this port should always be drawn */			private static final int PORTDRAWN =         0400000000;
	/** set to exclude this port from the icon */			private static final int BODYONLY =         01000000000;
	/** input/output/power/ground/clock state */			private static final int STATEBITS =       036000000000;
	/** input/output/power/ground/clock state */			private static final int STATEBITSSHIFTED =         036;
	/** input/output/power/ground/clock state */			private static final int STATEBITSSH =               27;
	public static final int EXPORT_BITS = PORTDRAWN | BODYONLY | STATEBITS;

	/** fixed-length arc */								private static final int FIXED =                     01;
	/** fixed-angle arc */								private static final int FIXANG =                    02;
	/** angle of arc from end 0 to end 1 */				private static final int AANGLE =                037740;
	/** bits of right shift for AANGLE field */			private static final int AANGLESH =                   5;
	/** set if head end of ArcInst is negated */		private static final int ISHEADNEGATED =        0200000;
	/** set if ends do not extend by half width */		private static final int NOEXTEND =             0400000;
	/** set if tail end of ArcInst is negated */		private static final int ISNEGATED =           01000000;
	/** set if arc aims from end 0 to end 1 */			private static final int ISDIRECTIONAL =       02000000;
	/** no extension/negation/arrows on end 0 */		private static final int NOTEND0 =             04000000;
	/** no extension/negation/arrows on end 1 */		private static final int NOTEND1 =            010000000;
	/** reverse extension/negation/arrow ends */		private static final int REVERSEEND =         020000000;
	/** set if arc can't slide around in ports */		private static final int CANTSLIDE =          040000000;
	/** set if afixed arc was changed */				private static final int FIXEDMOD =          0100000000;
	/** set if hard to select */						private static final int HARDSELECTA =     020000000000;

	public static final int ARC_BITS = FIXED | FIXANG | AANGLE | CANTSLIDE /* | FIXEDMOD*/ | HARDSELECTA;

	/** if on, draw node expanded */						private static final int NEXPAND =                   04;
	/** set if node not drawn due to wiping arcs */			private static final int WIPED =                    010;
	/** set if node is to be drawn shortened */				private static final int NSHORT =                   020;
	/** set if hard to select */							private static final int HARDSELECTN =          0100000;
	/** set if node only visible inside cell */				private static final int NVISIBLEINSIDE =     040000000;
	/** technology-specific bits for primitives */			private static final int NTECHBITS =          037400000;
	/** right-shift of NTECHBITS */							private static final int NTECHBITSSH =               17;
	/** set if node is locked (can't be changed) */			private static final int NILOCKED =          0100000000;

	public static final int NODE_BITS = NEXPAND | WIPED | NSHORT | HARDSELECTN | NVISIBLEINSIDE | NTECHBITS | NILOCKED;

	// bits found in the "userbits" field of an ArcInst:
// 	/** set if head end of ArcInst is negated */								public static final int ISHEADNEGATED =       0200000;
// 	/** set if tail end of ArcInst is negated */								public static final int ISNEGATED =          01000000;
// 	/** reverse extension/negation/arrow ends */								public static final int REVERSEEND =        020000000;

	/**
	 * Method to convert an integer date read from disk to a Java Date object.
	 * The Electric library disk format for dates is in seconds since the epoch.
	 * @param secondsSinceEpoch the number of seconds since the epoch (Jan 1, 1970).
	 * @return a Java Date object.
	 */
	public static Date secondsToDate(int secondsSinceEpoch)
	{
		GregorianCalendar creation = new GregorianCalendar();
		creation.setTimeInMillis(0);
		creation.setLenient(true);
		creation.add(Calendar.SECOND, secondsSinceEpoch);
		return creation.getTime();
	}

	/**
	 * Method to convert a Java Date object to an integer (seconds since the epoch).
	 * The Electric library disk format for dates is in seconds since the epoch.
	 * @param date a Java Date object.
	 * @return the number of seconds since the epoch (Jan 1, 1970);
	 */
	public static long dateToSeconds(Date date)
	{
//	the easy way?	return date.getTime();
		GregorianCalendar creation = new GregorianCalendar();
		creation.setTime(date);
		return creation.getTimeInMillis() / 1000;
	}

	/**
	 * Method to compute the "userbits" to use for a given ArcInst.
	 * The "userbits" are a set of bits that describes constraints and other properties,
	 * and are stored in ELIB files.
	 * The negation, directionality, and end-extension must be converted.
	 * @param ai the ArcInst to analyze.
	 * @return the "userbits" for that ArcInst.
	 */
	public static int makeELIBArcBits(ArcInst ai)
	{
		int userBits = ai.lowLevelGetUserbits() & ARC_BITS;
	
		// adjust bits for negation
		userBits &= ~(ISNEGATED | ISHEADNEGATED);
		if (ai.isTailNegated()) userBits |= ISNEGATED;
		if (ai.isHeadNegated()) userBits |= ISHEADNEGATED;
	
		// adjust bits for extension
		if (!ai.isHeadExtended() || !ai.isTailExtended())
		{
			userBits |= NOEXTEND;
			if (ai.isHeadExtended() != ai.isTailExtended())
			{
				if (ai.isTailExtended()) userBits |= NOTEND0;
				if (ai.isHeadExtended()) userBits |= NOTEND1;
			}
		}
	
		// adjust bits for directionality
		if (ai.isHeadArrowed() || ai.isTailArrowed() || ai.isBodyArrowed())
		{
			userBits |= ISDIRECTIONAL;
			if (ai.isTailArrowed()) userBits |= REVERSEEND;
			if (!ai.isHeadArrowed() && !ai.isTailArrowed()) userBits |= NOTEND1;
		}
		return userBits;
	}

	public static int getArcAngleFromBits(int bits) { return (bits & AANGLE) >> AANGLESH; }

	/**
	 * Method to apply a set of "userbits" to an arc.
	 * The "userbits" are a set of bits that describes constraints and other properties,
	 * and are stored in ELIB files.
	 * The negation, directionality, and end-extension must be converted.
	 * @param ai the ArcInst to modify.
	 * @param bits the userbits to apply to the arc.
	 */
	public static void applyELIBArcBits(ArcInst ai, int bits)
	{
		ai.lowLevelSetUserbits(bits & ARC_BITS);
		if ((bits&ELIBConstants.ISNEGATED) != 0)
		{
			if ((bits&ELIBConstants.REVERSEEND) != 0) ai.setHeadNegated(true); else
				ai.setTailNegated(true);
		}
		if ((bits&ELIBConstants.ISHEADNEGATED) != 0)
		{
			if ((bits&ELIBConstants.REVERSEEND) != 0) ai.setTailNegated(true); else
				ai.setHeadNegated(true);
		}

		if ((bits&ELIBConstants.NOEXTEND) != 0)
		{
			ai.setHeadExtended(false);
			ai.setTailExtended(false);
			if ((bits&ELIBConstants.NOTEND0) != 0) ai.setTailExtended(true);
			if ((bits&ELIBConstants.NOTEND1) != 0) ai.setHeadExtended(true);
		} else
		{
			ai.setHeadExtended(true);
			ai.setTailExtended(true);
		}

		ai.setBodyArrowed(false);
		ai.setTailArrowed(false);
		ai.setHeadArrowed(false);
		if ((bits&ELIBConstants.ISDIRECTIONAL) != 0)
		{
			ai.setBodyArrowed(true);
			if ((bits&ELIBConstants.REVERSEEND) != 0)
			{
				if ((bits&ELIBConstants.NOTEND0) == 0) ai.setTailArrowed(true);
			} else
			{
				if ((bits&ELIBConstants.NOTEND1) == 0) ai.setHeadArrowed(true);
			}
		}
	}

	/**
	 * Method to convert from Java types to "elib" types.
	 * The "elib" types are used when saving libraries to disk.
	 * They were the constants used in the C Electric's object implementation,
	 * and are still used in both the "elib" and Readable Dump files.
	 * @param obj an Object of any class in the Electric world.
	 * @return the "elib" integer type number for that class of object.
	 */
	public static int getVarType(Object obj)
	{
		if (obj instanceof Integer) return ELIBConstants.VINTEGER;
		if (obj instanceof Short) return ELIBConstants.VSHORT;
		if (obj instanceof Byte) return ELIBConstants.VCHAR;
		if (obj instanceof String) return ELIBConstants.VSTRING;
		if (obj instanceof Float) return ELIBConstants.VFLOAT;
		if (obj instanceof Double) return ELIBConstants.VDOUBLE;
		if (obj instanceof Technology) return ELIBConstants.VTECHNOLOGY;
		if (obj instanceof Library) return ELIBConstants.VLIBRARY;
		if (obj instanceof Tool) return ELIBConstants.VTOOL;
		if (obj instanceof NodeInst) return ELIBConstants.VNODEINST;
		if (obj instanceof ArcInst) return ELIBConstants.VARCINST;
		if (obj instanceof NodeProto) return ELIBConstants.VNODEPROTO;
		if (obj instanceof ArcProto) return ELIBConstants.VARCPROTO;
		if (obj instanceof PortProto) return ELIBConstants.VPORTPROTO;
		return ELIBConstants.VUNKNOWN;
	}
}
