/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Name.java
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
package com.sun.electric.database.text;

import java.util.List;
import java.util.Map;

/**
 * A Name is a text-parsing object for port, node and arc names.
 * These names can use bus notation:<BR>
 * <CENTER>name = itemname { ',' itemname }</CENTER>
 * <CENTER>itemname = string { '[' index ']' }</CENTER>
 * <CENTER>index = indexitem { ',' indexitem ']' }</CENTER>
 * <CENTER>indexitem = string | number ':' number</CENTER><BR>
 * Bus names are expanded into a list of subnames.
 */
public class Name
{
	/** the name */				private String name;
	/** list of subnames */		private List subnames;
	/** Map String -> Name */	private static Map allNames;

	/**
	 * Constructs a <CODE>Name</CODE> (cannot be called).
	 */
	private Name(String ns) {}

	/**
	 * Routine to return the name object for this string.
	 * @param ns given string
	 * @return the name object for string or null if string is not correct name.
	 */
	public static Name findName(String ns) { return null; }

	/**
	 * Tells whether or not this Name is a bus name.
	 * @return true if name is a bus name.
	 */
	public boolean isBus() { return subnames != null; }

	/**
	 * Returns subname of a bus name.
	 * @param i an index of subname.
	 * @return the view part of a parsed Cell name.
	 */
	public Name subname(int i) { return null; }

	/**
	 * Returns number of subnames of a bus.
	 * @return the number of subnames of a bus.
	 */
	public int busWidth() { return subnames == null ? 1 : subnames.size(); }
}
