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
import java.util.ArrayList;
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
	/** the name */				private String ns;
	/** the lowercase name */	private Name lowerCase;
	/** list of subnames */		private List subnames;
	/** Map String -> Name */	private static Map allNames;
	/** the flags */			private int flags;
	

	/**
	 * Routine to return the name object for this string.
	 * @param ns given string
	 * @return the name object for the string.
	 */
	public static Name findName(String ns) { return findTrimmedName(trim(ns)); }

	/**
	 * Routine to check whether or not string is a valid name.
	 * @param ns given string
	 * @return the error description or null if string is correct name.
	 */
	public static String checkName(String ns)
	{
		try
		{
			checkNameThrow(ns);
			return null;
		} catch (NumberFormatException e)
		{
			return e.toString();
		}
	}

	/**
	 * Returns a printable version of this Name.
	 * @return a printable version of this Name.
	 */
	public String toString() { return ns; }

	/**
	 * Returns lowerCase equivalent of this Name.
	 * @return lowerCase equivalent of this Name.
	 */
	public Name lowerCase() { return lowerCase; }

	/**
     * Compares this <code>Name</code> to another <code>Name</code>,
     * ignoring case considerations.  Two strings are considered equal
     * ignoring case if they are of the same length, and corresponding
     * characters in the two strings are equal ignoring case.
     * @return  <code>true</code> if names are equal,
     *          ignoring case; <code>false</code> otherwise.
	 */
	public boolean equalsIgnoreCase(Object anObject)
	{
		if (this == anObject) return false;
		if (anObject instanceof Name)
		{
			Name anotherName = (Name)anObject;
			return lowerCase == anotherName.lowerCase;
		}
		return false;
	}

	/**
	 * Tells whether or not this Name is a valid bus or signal name.
	 * @return true if Name is a valid name.
	 */
	public boolean isValid() { return (flags & ERROR) == 0; }

	/**
	 * Tells whether or not this Name is a list of names separated by comma.
	 * @return true if Name is a list of names separated by comma.
	 */
	public boolean isList() { return (flags & LIST) != 0; }

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
	public Name subname(int i) { return subnames == null ? this : (Name)subnames.get(i); }

	/**
	 * Returns number of subnames of a bus.
	 * @return the number of subnames of a bus.
	 */
	public int busWidth() { return subnames == null ? 1 : subnames.size(); }


	// ------------------ protected and private methods -----------------------

	public static final int ERROR    = 0x1;
	public static final int LIST     = 0x2;
	public static final int BUS      = 0x4;

	/**
	 * Returns the name object for this string, assuming that is is trimmed.
	 * @param ns given trimmed string
	 * @return the name object for the string.
	 */
	private static Name findTrimmedName(String ns)
	{
		Name name = (Name)allNames.get(ns);
		if (name == null)
		{
			name = new Name(ns);
			allNames.put(name.toString(), name);
		}
		return name;
	}

	/**
	 * Returns the trimmed string for given string.
	 * @param ns given string
	 * @return trimmed string.
	 */
	private static String trim(String ns)
	{
		int len = ns.length();
		int newLen = 0;
		for (int i = 0; i < len; i++)
		{
			if (ns.charAt(i) > ' ') newLen++;
		}
		if (newLen == len) return ns;

		StringBuffer buf = new StringBuffer(newLen);
		int l = 0;
		for (int i = 0; i < len; i++)
		{
			if (ns.charAt(i) > ' ') buf.setCharAt(l++, ns.charAt(i));
		}
		return buf.toString();
	}

	/**
	 * Constructs a <CODE>Name</CODE> (cannot be called).
	 */
	private Name(String ns)
	{
		this.ns = ns;
		String lower = ns.toLowerCase();
		this.lowerCase = (ns.equals(lower) ? this : findTrimmedName(lower));
		try
		{
			flags = checkNameThrow(ns);
		} catch (NumberFormatException e)
		{
			flags = ERROR;
			return;
		}
		if ((flags & BUS) == 0) return;

		/* Make subnames */
		if (isList())
		{
			makeListSubNames();
			return;
		}
		int split = ns.indexOf('[');
		if (split == 0) split = ns.lastIndexOf('[');
		if (split == 0)
			makeBracketSubNames();
		else
			makeSplitSubNames(split);
	}

	/**
	 * Makes subnames of a bus whose name is a list of names separated by commas.
	 */
	private void makeListSubNames()
	{
		subnames = new ArrayList();
		for (int beg = 0; beg <= ns.length(); )
		{
			int end = beg;
			while (end < ns.length() && ns.charAt(end) != ',')
			{
				if (ns.charAt(end) == '[')
				{
					while (ns.charAt(end) != ']') end++;
				}
				end++;
			}
			Name nm = findTrimmedName(ns.substring(beg,end));
			for (int j = 0; j < nm.busWidth(); j++)
				subnames.add(nm.subname(j));
			beg = end + 1;
		}
	}

	/**
	 * Makes subnames of a bus whose name is indices list in brackets.
	 */
	private void makeBracketSubNames()
	{
		subnames = new ArrayList();
		for (int beg = 1; beg < ns.length(); )
		{
			int end = ns.indexOf(',', beg);
			if (end < 0) end = ns.length() - 1; /* index of ']' */
			int colon = ns.indexOf(':', beg);
			if (colon < 0)
			{
				Name nm = findTrimmedName("["+ns.substring(beg,end)+"]");
				subnames.add(nm);
			} else
			{
				int ind1 = Integer.parseInt(ns.substring(beg, colon));
				int ind2 = Integer.parseInt(ns.substring(colon+1, end));
				if (ind1 < ind2)
				{
					for (int i = ind1; i <= ind2; i++)
						subnames.add(findTrimmedName("["+i+"]"));
				} else
				{
					for (int i = ind1; i >= ind2; i--)
						subnames.add(findTrimmedName("["+i+"]"));
				}
			}
			beg = end+1;
		}
	}

	/**
	 * Makes subnames of a bus whose name consists of simpler names.
	 * @param split index dividing name into simpler names.
	 */
	private void makeSplitSubNames(int split)
	{
		Name baseName = findTrimmedName(ns.substring(0,split));
		Name indexList = findTrimmedName(ns.substring(split));
		subnames = new ArrayList(baseName.busWidth()*indexList.busWidth());
		for (int i = 0; i < baseName.busWidth(); i++)
		{
			for (int j = 0; j < indexList.busWidth(); j++)
			{
				subnames.set(i*indexList.busWidth()+j,
					baseName.subname(i).toString()+indexList.subname(j).toString());
			}

		}
	}

	/**
	 * Routine to check whether or not string is a valid name.
	 * Throws exception on invaliod string
	 * @param ns given string
	 * @return flags describing the string.
	 */
	static int checkNameThrow(String ns) throws NumberFormatException
	{
		int flags = 0;
		
		int bracket = -1;
		int colon = -1;
		for (int i = 0; i < ns.length(); i++)
		{
			char c = ns.charAt(i);
			if (bracket < 0)
			{
				colon = -1;
				if (c == '[') bracket = i;
				if (c == ']') throw new NumberFormatException("unmatched ']' in name");
				if (c == ':') throw new NumberFormatException("':' out of brackets");
				if (c == ',') flags |= (LIST|BUS);
				continue;
			}
			if (c == '[') throw new NumberFormatException("nested bracket '[' in name");
			if (c == ':')
			{
				if (colon >= 0) throw new NumberFormatException("too many ':' inside brackets");
				if (i == bracket + 1) throw new NumberFormatException("has missing start of index range");
				if (ns.charAt(bracket+1) == '-') throw new NumberFormatException("has negative start of index range");
				for (int j = bracket + 1; j < i; j++)
				{
					if (!Character.isDigit(ns.charAt(j)))
						 throw new NumberFormatException("has nonnumeric start of index range");
				}
				colon = i;
				flags |= BUS;
			}
			if (colon >= 0 && (c == ']' || c == ','))
			{
				if (i == colon + 1) throw new NumberFormatException("has missing end of index range");
				if (ns.charAt(colon+1) == '-') throw new NumberFormatException("has negative end of index range");
				for (int j = colon + 1; j < i; j++)
				{
					if (!Character.isDigit(ns.charAt(j)))
						 throw new NumberFormatException("has nonnumeric end of index range");
				}
				if (Integer.parseInt(ns.substring(bracket+1,colon)) == Integer.parseInt(ns.substring(colon+1,i)))
					throw new NumberFormatException("has equal start and end indices");
				colon = -1;
			}
			if (c == ']') bracket = -1;
			if (c == ',')
			{
				bracket = i;
				flags |= BUS;
			}
		}
		return flags;
	}
}
