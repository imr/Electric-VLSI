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

import com.sun.electric.database.geometry.GenMath;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;


/**
 * A Name is a text-parsing object for port, node and arc names.
 * These names can use bus notation:<BR>
 * <CENTER>name = username | temname</CENTER>
 * <CENTER>username = itemname { ',' itemname }</CENTER>
 * <CENTER>itemname = simplename { '[' index ']' }</CENTER>
 * <CENTER>index = indexitem { ',' indexitem ']' }</CENTER>
 * <CENTER>indexitem = simplename | number [':' number]</CENTER><BR>
 * <CENTER>tempname = simplename '@' number </CENTER><BR>
 * <CENTER>simplename = string</CENTER><BR>
 * string doesn't contain '[', ']', ',', ':'.
 * Bus names are expanded into a list of subnames.
 */
public class Name implements Comparable<Name>
{
    /** True to keep strings in PermGen heap */     private static final boolean INTERN = true;

    /** the original name */	private final String ns;
	/** the canonic name */     private final String canonicString;
	/** list of subnames */		private Name[] subnames;
	/** basename */				private final Name basename;
	/** numerical suffix */     private final int numSuffix;
	/** the flags */			private int flags;
	
	/** Hash of Names */        private static volatile Name[] allNames = new Name[1];
    /** count of allocated Names */private static int allNamesCount = 0;
    /** Hash of canonic names. */private static final HashMap<String,Name> canonicNames = new HashMap<String,Name>();

	/**
	 * Method to return the name object for this string.
	 * @param ns given string
	 * @return the name object for the string.
	 */
	public static final Name findName(String ns) {
        if (ns == null) return null;
        String ts = trim(ns);
        return newTrimmedName(ts, ts == ns); }

	/**
	 * Method to check whether or not string is a valid name.
	 * @param ns given string
	 * @return the error description or null if string is correct name.
	 */
	public static String checkName(String ns)
	{
		try
		{
			int flags = checkNameThrow(ns);
            if ((flags & HAS_EMPTIES) != 0) return "has empty subnames";
			return null;
		} catch (NumberFormatException e)
		{
			return e.getMessage();
		}
	}

    /**
     * Print statistics about Names.
     */
    public static void printStatistics() {
        int validNames = 0;
        int userNames = 0;
        int busCount = 0;
        int busWidth = 0;
        int lowerCase = 0;
        long length = 0;
        HashSet<String> canonic = new HashSet<String>();
        for (Name n: allNames) {
            if (n == null) continue;
            length += n.toString().length();
            if (n.isValid())
                validNames++;
            if (!n.isTempname())
                userNames++;
            if (n.subnames != null) {
                busCount++;
                busWidth += n.subnames.length;
            }
            if (n.toString() == n.canonicString())
                lowerCase++;
            else
                canonic.add(n.canonicString());
        }
        for (Name n: allNames) {
            if (n == null) continue;
            canonic.remove(n.toString());
        }
        long canonicLength = 0;
        for (String s: canonic)
            canonicLength += s.length();
        System.out.println(allNamesCount + " Names " + length + " chars. " + validNames + " valid " + userNames + " usernames " +
                busCount + " buses with " + busWidth + " elements. " +
                lowerCase + " lowercase " + canonic.size() + " canonic strings with " + canonicLength + " chars.");
    }
    
	/**
	 * Returns a printable version of this Name.
	 * @return a printable version of this Name.
	 */
	public final String toString() { return ns; }

	/**
	 * Returns canonic equivalent String of this Name.
	 * @return canonic equivalent String of this Name.
	 */
	public final String canonicString() { return canonicString; }

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
		if (canonicString == name.canonicString) return 0;
		return canonicString.compareTo(name.canonicString);
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
//4*/public int compareTo(Object o) { return compareTo((Name)o); }

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
			return canonicString == anotherName.canonicString;
		}
		return false;
	}

    /**
     * Returns a hash code for this TextDescriptor. The hash code for a
     * <code>TextDescriptor</code> object is computed as sum of its fields.
     * @return  a hash code value for this object.
     */
    public int hashCode() { return canonicString.hashCode(); }

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
	 * Returns basename of temporary Name.
	 * Returns null if not temporary Name.
	 * @return base of name.
	 */
	public final Name getBasename() { return basename; }

	/**
	 * Returns numerical suffix of temporary Name.
	 * Returns -1 if not temporary name.
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
        String basenameString = basename.ns.substring(0, basename.ns.length() - 1);
		return findName(basenameString + i);
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
     * @param clone true to clone on reallocation
	 * @return the name object for the string.
	 */
	private static Name newTrimmedName(String ns, boolean clone) {
        return findTrimmedName(ns, true, clone);
    }
    
	/**
	 * Returns the name object for this string, assuming that is is trimmed.
	 * @param ns given trimmed string
     * @param create true to allocate new name if not found
     * @param clone true to clone on reallocation
	 * @return the name object for the string.
	 */
	private static Name findTrimmedName(String ns, boolean create, boolean clone)
	{
        // The allNames array is created in "rehash" method inside synchronized block.
        // "rehash" fills some entris leaving null in others.
        // All entries filled in rehash() are final.
        // However other threads may change initially null entries to non-null value.
        // This non-null value is final.
        // First we scan a sequence of non-null entries out of synchronized block.
        // It is guaranteed that we see the correct values of non-null entries.

        // Get poiner to hash array locally once to avoid many reads of volatile variable.
        Name[] hash = allNames;
        
        // We shall try to search a sequence of non-null entries for CellUsage with our protoId.
        int i = ns.hashCode() & 0x7FFFFFFF;
        i %= hash.length;
        for (int j = 1; hash[i] != null; j += 2) {
            Name n = hash[i];
            
            // We scanned a seqence of non-null entries and found the result.
            // It is correct to return it without synchronization.
            if (n.ns.equals(ns)) return n;
            
            i += j;
            if (i >= hash.length) i -= hash.length;
        }
        
        // Need to enter into the synchronized mode.
        synchronized (Name.class) {
            
            if (hash == allNames && allNames[i] == null) {
                // There we no rehash during our search and the last null entry is really null.
                // So we can safely use results of unsynchronized search.
                if (!create) return null;
                
                if (allNamesCount*2 <= hash.length - 3) {
                    // create a new CellUsage, if enough space in the hash
                    if (!INTERN && clone) {
                        ns = new String(ns);
                        clone = false;
                    }
                    Name n = new Name(ns);
                    if (hash != allNames || hash[i] != null)
                        return newTrimmedName(ns, false);
                    hash[i] = n;
                    allNamesCount++;
                    return n;
                }
                // enlarge hash if not 
                rehash();
            }
            // retry in synchronized mode.
            return findTrimmedName(ns, create, clone);
        }
    }
    
    /**
     * Rehash the allNames hash.
     * @throws IndexOutOfBoundsException on hash overflow.
     * This method may be called only inside synchronized block.
     */
    private static void rehash() {
        Name[] oldHash = allNames;
        int newSize = oldHash.length*2 + 3;
        if (newSize < 0) throw new IndexOutOfBoundsException();
        Name[] newHash = new Name[GenMath.primeSince(newSize)];
        for (Name n: oldHash) {
            if (n == null) continue;
            int i = n.ns.hashCode() & 0x7FFFFFFF;
            i %= newHash.length;
            for (int j = 1; newHash[i] != null; j += 2) {
                i += j;
                if (i >= newHash.length) i -= newHash.length;
            }
            newHash[i] = n;
        }
        allNames = newHash;
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
	 * Constructs a <CODE>Name</CODE> (cannot be called).
	 */
	private Name(String s)
	{
        String canonic;
        if (INTERN) {
            s = s.intern();
            canonic = TextUtils.canonicString(s);
            if (canonic != s)
                canonic = canonic.intern();
        } else {
            canonic = TextUtils.canonicString(s);
            if (canonic == s) {
                Name canonicName = canonicNames.get(s);
                if (canonicName != null) {
                    canonic = s = canonicName.canonicString;
                    canonicNames.remove(canonic);
                }
            } else {
                Name canonicName = findTrimmedName(canonic, false, false);
                if (canonicName == null)
                    canonicName = canonicNames.get(s);
                if (canonicName != null)
                    canonic = canonicName.canonicString;
                else
                    canonicNames.put(canonic, this);
            }
        }
        ns = s;
        canonicString = canonic;
        int suffix = -1;
        Name base = null;
		try
		{
			flags = checkNameThrow(ns);
		} catch (NumberFormatException e)
		{
			flags = ERROR;
		}
		if ((flags & ERROR) == 0 && (flags & TEMP) != 0)
		{
			int l = ns.length();
			while (l > 0 && TextUtils.isDigit(ns.charAt(l-1))) l--;
			if (l == ns.length()-1 && ns.charAt(ns.length() - 1) == '0')
			{
                base = this;
                suffix = 0;
			} else {
				base = newTrimmedName(ns.substring(0,l)+'0', false);
				suffix = Integer.parseInt(ns.substring(l));
			}
		}
        this.numSuffix = suffix;
        this.basename = base;
        if (flags == ERROR) return;
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
		List<Name> subs = new ArrayList<Name>();
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
			Name nm = newTrimmedName(ns.substring(beg,end), true);
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
		List<Name> subs = new ArrayList<Name>();
		for (int beg = 1; beg < ns.length(); )
		{
			int end = ns.indexOf(',', beg);
			if (end < 0) end = ns.length() - 1; /* index of ']' */
			int colon = ns.indexOf(':', beg);
			if (colon < 0 || colon >= end)
			{
				Name nm = newTrimmedName("["+ns.substring(beg,end)+"]", false);
				subs.add(nm);
			} else
			{
				int ind1 = Integer.parseInt(ns.substring(beg, colon));
				int ind2 = Integer.parseInt(ns.substring(colon+1, end));
				if (ind1 < ind2)
				{
					for (int i = ind1; i <= ind2; i++)
						subs.add(newTrimmedName("["+i+"]", false));
				} else
				{
					for (int i = ind1; i >= ind2; i--)
						subs.add(newTrimmedName("["+i+"]", false));
				}
			}
			beg = end+1;
		}
		setSubnames(subs);
	}

	private void setSubnames(List<Name> subs)
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
		Name baseName = newTrimmedName(ns.substring(0,split), true);
		Name indexList = newTrimmedName(ns.substring(split),true);
		subnames = new Name[baseName.busWidth()*indexList.busWidth()];
		for (int i = 0; i < baseName.busWidth(); i++)
		{
			String bs = baseName.subname(i).toString();
			for (int j = 0; j < indexList.busWidth(); j++)
			{
				String is = indexList.subname(j).toString();
				subnames[i*indexList.busWidth()+j] = newTrimmedName(bs+is, false);
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
				if (c == '@') {
                    for (int j = i + 1; j < ns.length(); j++) {
                        char cj = ns.charAt(j);
                        if (cj < '0' || cj > '9')
                            throw new NumberFormatException("Wrong number suffix in temporary name");
                    }
                    if (i == ns.length() - 1 || ns.charAt(i + 1) == '0' && i != ns.length() - 2)
                        throw new NumberFormatException("Wrong temporary name");
                    if ((flags & SIMPLE) == 0) throw new NumberFormatException("list of temporary names");
                    Integer.parseInt(ns.substring(i + 1)); // throws exception on bad number
                    assert flags == SIMPLE;
                    return SIMPLE|TEMP;
                }
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
					if (!TextUtils.isDigit(ns.charAt(j)))
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
					if (!TextUtils.isDigit(ns.charAt(j)))
						 throw new NumberFormatException("has nonnumeric end of index range");
				}
				if (Integer.parseInt(ns.substring(bracket+1,colon)) == Integer.parseInt(ns.substring(colon+1,i)))
					throw new NumberFormatException("has equal start and end indices");
				colon = -1;
			}
			if (c == ']') {
                if (i == bracket + 1) flags |= HAS_EMPTIES;
                bracket = -1;
            }
			if (c == ',')
			{
                if (i == bracket + 1) flags += HAS_EMPTIES;
				bracket = i;
				flags |= BUS;
			}
			if (c == '@') throw new NumberFormatException("'@' in brackets");
		}
		if (bracket != -1) throw new NumberFormatException("Unclosed bracket");
		return flags;
	}
}
