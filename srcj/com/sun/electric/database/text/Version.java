/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Version.java
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

import com.sun.electric.tool.user.ActivityLogger;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.StringTokenizer;

/**
 * A Version is a text-parsing object for Electric's version number.
 * Electric's current version has the form:<BR>
 * <CENTER>Major.Minor[Details]</CENTER><BR>
 * where <I>Major</I> is the major release number,
 * <I>Minor</I> is the minor release number within the major release,
 * and <I>Details</I> are sub-release values which can be letters or numbers.
 * <P>
 * If Details are omitted, then this is a full release of Electric.
 * If Details are present, then this is an interim release.
 * The Details can take two forms: letters or numbers with a dot in front.
 * <P>
 * Examples:
 *  6.03a
 *  7.04.1
 *  8.00
 */
public class Version implements Comparable<Version>
{
	/**
	 * This is the current version of Electric
	 */
	private static final String CURRENT = "8.04";
	private static final String ROOTARNAME = "electric";

	private final String version;
	private final int major;
	private final int minor;
	private final int details;

	private static final Version current = new Version(CURRENT);

	/**
	 * Constructs a <CODE>Version</CODE> (cannot be called).
	 * Routine to parse the version of Electric in "version" into three fields:
	 * the major version number, minor version, and a detail version number.
	 * The detail version number can be letters.  If it is omitted, it is
	 * assumed to be 999.  If it is a number, it is beyond 1000.  For example:
	 *    "8"         major=8, minor=0, detail=0       (boundary between 7 and 8)
	 *    "8.00"      major=8, minor=0, detail=999     (a Release)
	 *    "8.01a"     major=8, minor=1, detail=1       (a Prerelease)
	 *    "8.01z"     major=8, minor=1, detail=26      (a Prerelease)
	 *    "8.01aa"    major=8, minor=1, detail=27      (a Prerelease)
	 *    "8.01az"    major=8, minor=1, detail=52      (a Prerelease)
	 *    "8.01ba"    major=8, minor=1, detail=53      (a Prerelease)
	 *    "8.01"      major=8, minor=1, detail=999     (a Release)
	 *    "8.01.1"    major=8, minor=1, detail=1001    (a PostRelease, update)
	 */
	private Version(String version)
	{
		int major = 0;
		int minor = 0;
		int details = 0;

		/* parse the version fields */
		int dot = version.indexOf('.');
		if (dot == -1)
		{
			// no "." in the string: version should be a pure number
			major = Integer.parseInt(version);
		} else
		{
			// found the "." so parse the major version number (to the left of the ".")
			String majorString = version.substring(0, dot);
			String restOfString = version.substring(dot+1);
			major = Integer.parseInt(majorString);

			int letters;
			for(letters = 0; letters < restOfString.length(); letters++)
				if (!TextUtils.isDigit(restOfString.charAt(letters))) break;
			String minorString = restOfString.substring(0, letters);
			minor = Integer.parseInt(minorString);
			restOfString = restOfString.substring(letters);
		
			// see what else follows the minor version number
			if (restOfString.length() == 0)
			{
				details = 999;
			} else if (restOfString.charAt(0) == '.')
			{
				details = 1000 + Integer.parseInt(restOfString.substring(1));
			} else
			{
				while (restOfString.length() > 0 &&
					   Character.isLetter(restOfString.charAt(0)))
				{
					details = (details * 26) + Character.toLowerCase(restOfString.charAt(0)) - 'a' + 1;
					restOfString = restOfString.substring(1);
				}
				if (restOfString.length() > 0)
					System.out.println("Invalid version string " + version);
			}
		}

		this.version = version;
		this.major = major;
		this.minor = minor;
		this.details = details;
	}

	/**
	 * Method to return author information
	 * @return Steve M. Rubin
	 */
	public static String getAuthorInformation() { return "Written by Steven M. Rubin"; }

	/**
	 * Method to return official name of Electric
	 * @return the official name
	 */
	public static String getApplicationInformation() { return "The Electric VLSI Design System"; }

	/**
	 * Method to return copyright information
	 * @return  Sun Microsystems and Static Free Software
	 */
	public static String getCopyrightInformation() { return "Copyright (c) 2004 Sun Microsystems and Static Free Software"; }

	/**
	 * Method to return a short description of warranty
	 * @return short description of warranty
	 */
	public static String getWarrantyInformation() { return "Electric comes with ABSOLUTELY NO WARRANTY"; }

	/**
	 * Method to return version and compilation date if available
	 * @return string containing version number and date of jar file
	 */
	public static String getVersionInformation()
	{
		String versionText =  "Version " + getVersion();
        String buildText = getBuildDate();
        if (buildText != null)
           versionText += " (built on " + buildText + ")";
		return (versionText);
	}

	/**
	 * Method to return the current Electric version.
	 * @return the current Electric version.
	 */
	public static Version getVersion() { return current; }

    /**
     * Method to return build date of main jar file
     * @return string containing the date in short format
     */
    public static String getBuildDate()
    {
        // Might need to adjust token delim depending on operating system
        StringTokenizer parse = new StringTokenizer(System.getProperty("java.class.path"));
        String delim = System.getProperty("path.separator");
        try
        {
            while (parse.hasMoreElements())
            {
                String val = parse.nextToken(delim);
                // Find path for main jar
	            String filename = ROOTARNAME+".jar";
                if (val.lastIndexOf(filename) != -1)
                {
                    File electricJar = new File(val);
                    long date = electricJar.lastModified();
                    Date d = new Date(date);
                    return (DateFormat.getDateInstance().format(d));
                }
            }
        }
        catch (Exception e)
        {
            ActivityLogger.logException(e);
        }
        return (null);
    }

	/**
	 * Method to return the major part of a parsed Version number.
	 * @return the major part of a parsed Version number.
	 */
	public int getMajor() { return major; }

	/**
	 * Method to return the minor part of a parsed Version number.
	 * @return the minor part of a parsed Version number.
	 */
	public int getMinor() { return minor; }

	/**
	 * Method to return the details part of a parsed Version number.
	 * @return the details part of a parsed Version number.
	 */
	public int getDetail() { return details; }

    /**
     * Returns a hash code for this <code>Version</code>.
     * @return  a hash code value for this Version.
	 */
    public int hashCode() {
		return major*1000000 + minor*10000 + details;
    }

    /**
     * Compares this Version object to the specified object.  The result is
     * <code>true</code> if and only if the argument is not
     * <code>null</code> and is an <code>Version</code> object that
     * contains the same <code>major</code>, <code>minor</code> and <code>details</code> as this Version.
     *
     * @param   obj   the object to compare with.
     * @return  <code>true</code> if the objects are the same;
     *          <code>false</code> otherwise.
     */
    public boolean equals(Object obj) {
		if (obj instanceof Version) {
			Version v = (Version)obj;
			return major == v.major && minor == v.minor && details == v.details;
		}
		return false;
    }

    /**
     * Compares two <code>Version</code> objects numerically.
     *
     * @param v the object to be compared.
     * @return the result of comparison.
     */
	public int compareTo(Version v) {

		if (major < v.major) return -1;
		if (major > v.major) return 1;

		if (minor < v.minor) return -1;
		if (minor > v.minor) return 1;

		if (details < v.details) return -1;
		if (details > v.details) return 1;
		
		return 0;
    }

    /**
     * Returns a <code>String</code> object representing this Version.
     * @return  a string representation of this Version
     */
    public String toString() {
		return version;
    }

	/**
	 * Method to parse the specified Version number and return a Version object.
	 * @param version the version of Electric.
	 * @return a Version object with the fields parsed.
	 */
	public static Version parseVersion(String version)
	{
		return new Version(version);
	}
}
