/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Routing.java
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
package com.sun.electric.tool.routing;

import com.sun.electric.tool.Tool;
import com.sun.electric.tool.Job;

import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;

/**
 * This is the Routing tool.
 */
public class Routing extends Tool
{
	// ---------------------- private and protected methods -----------------

	/** the Routing tool. */		public static Routing tool = new Routing();

	private Preferences prefs = Preferences.userNodeForPackage(getClass());

	/**
	 * The constructor sets up the Routing tool.
	 */
	private Routing()
	{
		super("Routing");
	}

	/**
	 * Method to initialize the Routing tool.
	 */
	public void init()
	{
//		setOn();
	}

	/****************************** OPTIONS ******************************/

	/**
	 * Method to force all Routing Preferences to be saved.
	 */
	private static void flushOptions()
	{
		try
		{
	        tool.prefs.flush();
		} catch (BackingStoreException e)
		{
			System.out.println("Failed to save Routing options");
		}
	}

	/**
	 * Method to tell whether Auto-stitching should be done.
	 * The default is "true".
	 * @return true if Auto-stitching should be done.
	 */
	public static boolean isAutoStitchOn() { return tool.prefs.getBoolean("AutoStitchOn", true); }
	/**
	 * Method to set whether Auto-stitching should be done.
	 * @param on true if Auto-stitching should be done.
	 */
	public static void setAutoStitchOn(boolean on) { tool.prefs.putBoolean("AutoStitchOn", on);   flushOptions(); }

}
