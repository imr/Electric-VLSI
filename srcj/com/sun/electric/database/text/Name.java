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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * A Name is a text-parsing object for port, node and arc names.
 * These names can use bus notation:<BR>
 * <CENTER>name = itemname { ',' itemname }</CENTER>
 * <CENTER>itemname = simplename { '[' index ']' }</CENTER>
 * <CENTER>index = indexitem { ',' indexitem ']' }</CENTER>
 * <CENTER>indexitem = simplename | number ':' number</CENTER><BR>
 * <CENTER>simplename = basename [ numericalSuffix ]</CENTER><BR>
 * <CENTER>basename = string</CENTER><BR>
 * <CENTER>numericalSuffix = number</CENTER><BR>
 * string doesn't contain '[', ']', ',', ':'.
 * Bus names are expanded into a list of subnames.
 */
public class Name implements Comparable
{
	/** the original name */	private String ons;
	/** the canonical name */	private String ns;
	/** the lowercase name */	private Name lowerCase;
	/** list of subnames */		private Name[] subnames;
	/** basename */				private Name basename;
	/** numerical suffix */     private int numSuffix;
	/** the flags */			private int flags;
	
	/** Map String -> Name */	private static Map allNames = new HashMap();

	/**
	 * Method to return the name object for this string.
	 * @param ns given string
	 * @return the name object for the string.
	 */
	public static synchronized final Name findName(String ns) { return findTrimmedName(trim(ns)); }

	/**
	 * Method to check whether or not string is a valid name.
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
	public final String toString() { return ons; }

	/**
	 * Returns lowerCase equivalent of this Name.
	 * @return lowerCase equivalent of this Name.
	 */
	public final Name lowerCase() { return lowerCase; }

    /**
     * Compares this Name with the specified Name for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.<p>
     * @param   name the Name to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *		is less than, equal to, or greater than the specified object.
     */
    public int compareTo(Name name)
	{
		if (lowerCase == name.lowerCase) return 0;
		return lowerCase.ns.compareTo(name.lowerCase.ns);
	}

    /**
     * Compares this Name with the specified object for order.  Returns a
     * negative integer, zero, or a positive integer as this object is less
     * than, equal to, or greater than the specified object.<p>
     * @param   o the Object to be compared.
     * @return  a negative integer, zero, or a positive integer as this object
     *		is less than, equal to, or greater than the specified object.
     * 
     * @throws ClassCastException if the specified object's type prevents it
     *         from being compared to this Object.
     */
    public int compareTo(Object o)
	{
		return compareTo((Name)o);
	}

	/**
     * Compares this <code>Name</code> to another <code>Name</code>,
     * ignoring case considerations.  Two strings are considered equal
     * ignoring case if they are of the same length, and corresponding
     * characters in the two strings are equal ignoring case.
     * @return  <code>true</code> if names are equal,
     *          ignoring case; <code>false</code> otherwise.
	 */
	public boolean equals(Object anObject)
	{
		if (this == anObject) return true;
		if (anObject instanceof Name)
		{
			Name anotherName = (Name)anObject;
			return lowerCase == anotherName.lowerCase;
		}
		return false;
	}

    /**
     * Returns a hash code for this TextDescriptor. The hash code for a
     * <code>TextDescriptor</code> object is computed as sum of its fields.
     * @return  a hash code value for this object.
     */
    public int hashCode() { return lowerCase == this ? super.hashCode() : lowerCase.hashCode(); }

	/**
	 * Tells whether or not this Name is a valid bus or signal name.
	 * @return true if Name is a valid name.
	 */
	public final boolean isValid() { return (flags & ERROR) == 0; }

	/**
	 * Tells whether or not this Name is a temporary name
	 * @return true if Name is a temporary name.
	 */
	public final boolean isTempname() { return (flags & TEMP) != 0; }

	/**
	 * Tells whether Name has duplicate subnames.
	 * @return true if Name has duplicate subnames.
	 */
	public final boolean hasDuplicates() { return (flags & DUPLICATES) != 0; }

	/**
	 * Tells whether Name has duplicate subnames.
	 * @return true if Name has duplicate subnames.
	 */
	public final boolean hasEmptySubnames() { return (flags & HAS_EMPTIES) != 0; }

	/**
	 * Tells whether or not this Name is a list of names separated by comma.
	 * @return true if Name is a list of names separated by comma.
	 */
	public final boolean isList() { return (flags & LIST) != 0; }

	/**
	 * Tells whether or not this Name is a bus name.
	 * @return true if name is a bus name.
	 */
	public final boolean isBus() { return subnames != null; }

	/**
	 * Returns subname of a bus name.
	 * @param i an index of subname.
	 * @return the view part of a parsed Cell name.
	 */
	public final Name subname(int i) { return subnames == null ? this : subnames[i]; }

	/**
	 * Returns number of subnames of a bus.
	 * @return the number of subnames of a bus.
	 */
	public final int busWidth() { return subnames == null ? 1 : subnames.length; }

	/**
	 * Returns basename of simple Name.
	 * Returns null if not simple Name.
	 * @return base of name.
	 */
	public final Name getBasename() { return basename; }

	/**
	 * Returns numerical suffix of simple Name.
	 * Returns zero if numerical suffix is absent or name is not simple.
	 * @return numerical suffix.
	 */
	public final int getNumSuffix() { return numSuffix; }

	/**
	 * Returns the name obtained from base of this simple name by adding numerical suffix.
	 * Returns null if name is not simple or if i is negative.
	 * @param i numerical suffix
	 * @return suffixed name.
	 */
	public final Name findSuffixed(int i)
	{
		if (i < 0 || basename == null) return null;
		return findName(basename.toString()+i);
	}

	// ------------------ protected and private methods -----------------------

	private static final int ERROR			= 0x01;
	private static final int LIST			= 0x02;
	private static final int BUS			= 0x04;
	private static final int SIMPLE			= 0x08;
	private static final int TEMP			= 0x10;
	private static final int DUPLICATES		= 0x20;
	private static final int HAS_EMPTIES	= 0x40;

	/**
	 * Returns the name object for this string, assuming that is is trimmed.
	 * @param ns given trimmed string
	 * @return the name object for the string.
	 */
	private static Name findTrimmedName(String ns)
	{
		Name name = (Name)allNames.get(ns);
		if (name == null && ns != null)
		{
			name = new Name(ns);
			allNames.put(name.ons, name);
		}
		return name;
	}

	/**
	 * Returns the trimmed string for given string.
	 * @param ns given string
	 * @return trimmed string, or null if argument is null
	 */
	private static String trim(String ns)
	{
        if (ns == null) return null;
		int len = ns.length();
		int newLen = 0;
		for (int i = 0; i < len; i++)
		{
			if (ns.charAt(i) > ' ') newLen++;
		}
		if (newLen == len) return ns;

		StringBuffer buf = new StringBuffer(newLen);
		for (int i = 0; i < len; i++)
		{
			if (ns.charAt(i) > ' ') buf.append(ns.charAt(i));
		}
		return buf.toString();
	}

	/**
	 * Returns the trimmed string for given string.
	 * @param ns given string
	 * @return trimmed string.
	 */
	private static String trimPlusMinus(String ns)
	{
		int len = ns.length();
		int newLen = 0;
		for (int i = 0; i < len; i++)
		{
			char ch = ns.charAt(i);
			if (ch != '+' && ch != '-') newLen++;
		}
		if (newLen == len) return ns;

		StringBuffer buf = new StringBuffer(newLen);
		for (int i = 0; i < len; i++)
		{
			char ch = ns.charAt(i);
			if (ch != '+' && ch != '-') buf.append(ns.charAt(i));
		}
		return buf.toString();
	}

	/**
	 * Constructs a <CODE>Name</CODE> (cannot be called).
	 */
	private Name(String ons)
	{
		this.ons = ons;
		this.ns = trimPlusMinus(ons);
		this.numSuffix = 0;
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
		if ((flags & SIMPLE) != 0)
		{
			int l = ns.length();
			while (l > 0 && Character.isDigit(ns.charAt(l-1))) l--;
			if (l == ns.length())
			{
				basename = this;
			} else {
				basename = findTrimmedName(ns.substring(0,l));
				numSuffix = Integer.parseInt(ns.substring(l));
			}
		}
		if ((flags & BUS) == 0) return;

		// Make subnames
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
		List subs = new ArrayList();
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
				subs.add(nm.subname(j));
			beg = end + 1;
		}
		setSubnames(subs);
	}

	/**
	 * Makes subnames of a bus whose name is indices list in brackets.
	 */
	private void makeBracketSubNames()
	{
		List subs = new ArrayList();
		for (int beg = 1; beg < ns.length(); )
		{
			int end = ns.indexOf(',', beg);
			if (end < 0) end = ns.length() - 1; /* index of ']' */
			int colon = ns.indexOf(':', beg);
			if (colon < 0 || colon >= end)
			{
				Name nm = findTrimmedName("["+ns.substring(beg,end)+"]");
				subs.add(nm);
			} else
			{
				int ind1 = Integer.parseInt(ns.substring(beg, colon));
				int ind2 = Integer.parseInt(ns.substring(colon+1, end));
				if (ind1 < ind2)
				{
					for (int i = ind1; i <= ind2; i++)
						subs.add(findTrimmedName("["+i+"]"));
				} else
				{
					for (int i = ind1; i >= ind2; i--)
						subs.add(findTrimmedName("["+i+"]"));
				}
			}
			beg = end+1;
		}
		setSubnames(subs);
	}

	private void setSubnames(List subs)
	{
		subnames = new Name[subs.size()];
		subs.toArray(subnames);

		// check duplicates
		Name[] sorted = new Name[subs.size()];
		subs.toArray(sorted);
		Arrays.sort(sorted);
		for (int i = 1; i < sorted.length; i++)
		{
			if (sorted[i].equals(sorted[i-1]))
			{
				flags |= DUPLICATES;
				break;
			}
		}
	}

	/**
	 * Makes subnames of a bus whose name consists of simpler names.
	 * @param split index dividing name into simpler names.
	 */
	private void makeSplitSubNames(int split)
	{
		// if (ns.length() == 0) return;
		if (split < 0 || split >= ns.length())
		{
			System.out.println("HEY! string is '"+ns+"' but want index "+split);
			return;
		}
		Name baseName = findTrimmedName(ns.substring(0,split));
		Name indexList = findTrimmedName(ns.substring(split));
		subnames = new Name[baseName.busWidth()*indexList.busWidth()];
		for (int i = 0; i < baseName.busWidth(); i++)
		{
			String bs = baseName.subname(i).toString();
			for (int j = 0; j < indexList.busWidth(); j++)
			{
				String is = indexList.subname(j).toString();
				subnames[i*indexList.busWidth()+j] = findTrimmedName(bs+is);
			}
		}
		if (baseName.hasDuplicates() || indexList.hasDuplicates())
			flags |= DUPLICATES;
	}

	/**
	 * Method to check whether or not string is a valid name.
	 * Throws exception on invaliod string
	 * @param ns given string
	 * @return flags describing the string.
	 */
	private static int checkNameThrow(String ns) throws NumberFormatException
	{
		int flags = SIMPLE;
		
		int bracket = -1;
		boolean wasBrackets = false;
		int colon = -1;
		if (ns.length() == 0) flags |= HAS_EMPTIES;
		for (int i = 0; i < ns.length(); i++)
		{
			char c = ns.charAt(i);
			if (bracket < 0)
			{
				colon = -1;
				if (c == ']') throw new NumberFormatException("unmatched ']' in name");
				if (c == ':') throw new NumberFormatException("':' out of brackets");
				if (c == '[')
				{
					bracket = i;
					flags &= ~SIMPLE;
					if (i == 0 || ns.charAt(i-1) == ',') flags |= HAS_EMPTIES;
					wasBrackets = true;
				} else if (c == ',')
				{
					flags |= (LIST|BUS);
					flags &= ~SIMPLE;
					if (i == 0 || ns.charAt(i-1) == ',') flags |= HAS_EMPTIES;
					wasBrackets = false;
				} else if (wasBrackets) throw new NumberFormatException("Wrong character after brackets");
				if (c == '@') flags |= TEMP;
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
			if (c == '@') throw new NumberFormatException("'@' in brackets");
		}
		if ((flags & TEMP) != 0 && (flags & LIST) != 0) throw new NumberFormatException("list of temporary names");
		return flags;
	}
}
