/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Extract.java
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
package com.sun.electric.tool.extract;

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.DRCRules;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.routing.Routing;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.Highlighter;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.geom.Rectangle2D;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.prefs.Preferences;

/**
 * This is the Extraction tool.
 */
public class Extract extends Listener
{
	/** the Extraction tool. */								protected static Extract tool = new Extract();

	/****************************** TOOL CONTROL ******************************/

	/**
	 * The constructor sets up the DRC tool.
	 */
	private Extract()
	{
		super("extract");
	}

    /**
	 * Method to initialize the Extraction tool.
	 */
	public void init()
	{
	}
	
    /****************************** OPTIONS ******************************/

	private static Pref cacheIncrementalDRCOn = Pref.makeBooleanPref("IncrementalDRCOn", Extract.tool.prefs, true);
	/**
	 * Method to tell whether DRC should be done incrementally.
	 * The default is "true".
	 * @return true if DRC should be done incrementally.
	 */
	public static boolean isIncrementalDRCOn() { return cacheIncrementalDRCOn.getBoolean(); }
	/**
	 * Method to set whether DRC should be done incrementally.
	 * @param on true if DRC should be done incrementally.
	 */
	public static void setIncrementalDRCOn(boolean on) { cacheIncrementalDRCOn.setBoolean(on); }

}
