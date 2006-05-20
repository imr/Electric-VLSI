/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellName.java
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

import com.sun.electric.database.hierarchy.View;

/**
 * A CellName is a text-parsing object for Cell names.
 * Cell names have the form:<BR>
 * <CENTER>Name;Version{View}</CENTER><BR>
 * where the <I>Name</I> names the cell,
 * the <I>Version</I> is the version number,
 * and the <I>View</I> is the view of the cell (layout, schematics, icon, etc.)
 * <P>
 * Only the name is necessary.
 * If the ";Version" is omitted, then the most recent version is assumed.
 * If the "{View}" is omitted, then the "unknown" view is assumed.
 */
public class CellName implements Comparable<CellName>
{
	/** the name */		private final String name;
	/** the view */		private final View   view;
	/** the version */	private final int    version;
	/** hash of this */ private final int    hash;
	/**
	 * Constructs a <CODE>CellName</CODE> (cannot be called).
	 */
	private CellName(String name, View view, int version) {
        this.name = name;
        this.view = view;
        this.version = version;
		int h = 0;
		for (int i = 0; i < name.length(); i++) {
			h = 31*h + TextUtils.canonicChar(name.charAt(i));
		}
		h = h * 37 + view.getOrder();
		h = h * 17 + version;
		hash = h;
    }

	/**
	 * Method to return the name part of a parsed Cell name.
	 * @return the name part of a parsed Cell name.
	 */
	public String getName() { return name; }

	/**
	 * Method to return the view part of a parsed Cell name.
	 * @return the view part of a parsed Cell name.
	 */
	public View getView() { return view; }

	/**
	 * Method to return the version part of a parsed Cell name.
	 * @return the version part of a parsed Cell name.
	 */
	public int getVersion() { return version; }

    /**
     * Returns a hash code for this <code>Version</code>.
     * @return  a hash code value for this Version.
	 */
    public int hashCode() {
		return hash;
    }

    /**
     * Compares this CellName object to the specified object.  The result is
     * <code>true</code> if and only if the argument is not
     * <code>null</code> and is an <code>CellName</code> object that
     * contains the same <code>version</code>, <code>view</code> and case-insensitive <code>name</code> as this CellName.
     *
     * @param   obj   the object to compare with.
     * @return  <code>true</code> if the objects are the same;
     *          <code>false</code> otherwise.
     */
    public boolean equals(Object obj) {
		if (obj instanceof CellName) {
			CellName n = (CellName)obj;
			return hash == n.hash && name.equals(n.name) && view == n.view && version == n.version;
		}
		return false;
    }

    /**
     * Compares this CellName object to the specified object.  The result is
     * <code>true</code> if and only if the argument is not
     * <code>null</code> and is an <code>CellName</code> object that
     * contains the same <code>view</code> and case-insensitive <code>name</code> as this CellName.
     *
     * @param   obj   the object to compare with.
     * @return  <code>true</code> if the objects are the same;
     *          <code>false</code> otherwise.
     */
    public boolean equalsIgnoreVersion(Object obj) {
		if (obj instanceof CellName) {
			CellName n = (CellName)obj;
			return name.equals(n.name) && view == n.view;
		}
		return false;
    }

    /**
     * Compares two <code>CellName</code> objects.
     * @param that the CellName to be compared.
     * @return the result of comparison.
     */
	public int compareTo(CellName that) {
		int cmp = TextUtils.STRING_NUMBER_ORDER.compare(name, that.name);
		if (cmp != 0) return cmp;

		cmp = view.compareTo(that.view);
		if (cmp != 0) return cmp;

		if (version > 0)
			return that.version > 0 ? that.version - version : 1;
		else
			return that.version > 0 ? -1 : 0;
    }

	/**
	 * Method to build the full cell name.
	 * @return the full cell name.
	 */
	public String toString()
	{
		if (version > 0)
			return name + ";" + version + "{" + view.getAbbreviation() + "}";
		else
			return name + "{" + view.getAbbreviation() + "}";
	}

	/**
	 * Method to parse the specified Cell name and return a CellName object.
	 * @param name the name of the Cell.
	 * @return a CellName object with the fields parsed.
	 */
	public static CellName parseName(String name)
	{
		if (name == null) return null;

		// figure out the view and version of the cell
		View view = View.UNKNOWN;
		int openCurly = name.indexOf('{');
		int closeCurly = name.lastIndexOf('}');
		if (openCurly != -1 && closeCurly != -1)
		{
			String viewName = name.substring(openCurly+1, closeCurly);
			view = View.findView(viewName);
			if (view == null)
			{
				System.out.println("Unknown view: " + viewName);
				return null;
			}
		}

		// figure out the version
		int version = 0;
		int semiColon = name.indexOf(';');
		if (semiColon != -1)
		{
			String versionString;
			if (openCurly > semiColon) versionString = name.substring(semiColon+1, openCurly); else
				versionString = name.substring(semiColon+1);
			try
			{
				version = Integer.parseInt(versionString);
			}
			catch (NumberFormatException e)
			{
				System.out.println(versionString + "is not a valid cell version number");
				return null;
			}
			if (version <= 0)
			{
				System.out.println("Cell versions must be positive, this is " + version);
				return null;
			}
		}

		// get the pure cell name
		if (semiColon == -1) semiColon = name.length();
		if (openCurly == -1) openCurly = name.length();
		int nameEnd = Math.min(semiColon, openCurly);
		name = name.substring(0, nameEnd);
		if (name.length() == 0)
		{
			System.out.println("Cell name can't be empty");
			return null;
		}
		for (int i = 0; i < name.length(); i++)
		{
			char ch = name.charAt(i);
			if (ch == '\n' || ch == '{' || ch == '}' || ch == ';' || ch == '|' || ch == ':')
			{
				System.out.println("Cell name " + name + " has invalid char '" + ch + "'");
				return null;
			}
		}
        CellName n = new CellName(name, view, version);
		return n;
	}

    /**
     * Create a CellName from the given name, view, and version.
     * Any view or version information that is part of the name
     * is stripped away and ignored, and the arguments for view and version
     * are used instead.
     * @param name the name of the cell
     * @param view the view
     * @param version the version
     * @return a CellName object for the given name, view, and version
     */
    public static CellName newName(String name, View view, int version) {
		if (view == null) view = View.UNKNOWN;
		if (version < 0) version = 0;
        CellName n = parseName(name);
        CellName nn = new CellName(n.getName(), view, version);
        return nn;
    }
    
    /**
	 * Checks invariant of this CellName.
	 * @throws AssertionError if invariant is broken.
	 */
    public void check() {
		assert name.length() > 0;
		for (int i = 0; i < name.length(); i++) {
			char ch = name.charAt(i);
			assert ch != '\n' && ch != '{' && ch != '}' && ch != ';' && ch != '|' && ch != ':';
		}
        assert view != null;
        assert version >= 0;
    }
}
