/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: BinaryConstants.java
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

import java.util.Date;
import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * This class has constants for reading and writing binary (.elib) files.
 */
public class BinaryConstants
{
	/** undefined variable */										public static final int VUNKNOWN =                  0;
	/** 32-bit integer variable */									public static final int VINTEGER =                 01;
	/** unsigned address */											public static final int VADDRESS =                 02;
	/** character variable */										public static final int VCHAR =                    03;
	/** string variable */											public static final int VSTRING =                  04;
	/** floating point variable */									public static final int VFLOAT =                   05;
	/** double-precision floating point */							public static final int VDOUBLE =                  06;
	/** nodeinst pointer */											public static final int VNODEINST =                07;
	/** nodeproto pointer */										public static final int VNODEPROTO =              010;
	/** portarcinst pointer */										public static final int VPORTARCINST =            011;
	/** portexpinst pointer */										public static final int VPORTEXPINST =            012;
	/** portproto pointer */										public static final int VPORTPROTO =              013;
	/** arcinst pointer */											public static final int VARCINST =                014;
	/** arcproto pointer */											public static final int VARCPROTO =               015;
	/** geometry pointer */											public static final int VGEOM =                   016;
	/** library pointer */											public static final int VLIBRARY =                017;
	/** technology pointer */										public static final int VTECHNOLOGY =             020;
	/** tool pointer */												public static final int VTOOL =                   021;
	/** R-tree pointer */											public static final int VRTNODE =                 022;
	/** fractional integer (scaled by WHOLE) */						public static final int VFRACT =                  023;
	/** network pointer */											public static final int VNETWORK =                024;
	/** view pointer */												public static final int VVIEW =                   026;
	/** window partition pointer */									public static final int VWINDOWPART =             027;
	/** graphics object pointer */									public static final int VGRAPHICS =               030;
	/** 16-bit integer */											public static final int VSHORT =                  031;
	/** constraint solver */										public static final int VCONSTRAINT =             032;
	/** general address/type pairs (only in fixed-length arrays) */	public static final int VGENERAL =                033;
	/** window frame pointer */										public static final int VWINDOWFRAME =            034;
	/** polygon pointer */											public static final int VPOLYGON =                035;
	/** boolean variable */											public static final int VBOOLEAN =                036;
	/** all above type fields */									public static final int VTYPE =                   037;
	/** variable is interpreted code (with VCODE2) */				public static final int VCODE1 =                  040;
	/** display variable (uses textdescript field) */				public static final int VDISPLAY =               0100;
	/** set if variable is array of above objects */				public static final int VISARRAY =               0200;
	/** array length (0: array is -1 terminated) */					public static final int VLENGTH =         03777777000;
	/** right shift for VLENGTH */									public static final int VLENGTHSH =                 9;
	/** variable is interpreted code (with VCODE1) */				public static final int VCODE2 =          04000000000;
	
	/**
	 * Routine to convert the C-Electric-format date to a Java Data object.
	 * @param secondsSinceEpoch the number of seconds since the epoch (Jan 1, 1970).
	 * @return a Java Date object.
	 */
	public static Date fromElectricDate(int secondsSinceEpoch)
	{
		GregorianCalendar creation = new GregorianCalendar();
		creation.setTimeInMillis(0);
		creation.setLenient(true);
		creation.add(Calendar.SECOND, secondsSinceEpoch);
		return creation.getTime();
	}

	/**
	 * Routine to convert the Java Date object to a C-Electric-format date.
	 * @param date a Java Date object.
	 * @return the number of seconds since the epoch (Jan 1, 1970);
	 */
	public static long toElectricDate(Date date)
	{
		GregorianCalendar creation = new GregorianCalendar();
		creation.setTime(date);
		return creation.getTimeInMillis() / 1000;
	}
}
