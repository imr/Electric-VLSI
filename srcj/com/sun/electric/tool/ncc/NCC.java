/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DRC.java
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
package com.sun.electric.tool.ncc;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.Listener;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.ErrorLogger;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.util.Iterator;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Date;
import java.util.prefs.Preferences;

/**
 * Contains NCC preferences
 */
public class NCC {
	public static NCC tool = new NCC();
	// per-package namespace for preferences
	private Preferences prefs = Preferences.userNodeForPackage(this.getClass());

	private static Pref checkSizes = 
		Pref.makeBooleanPref("CheckSizes", NCC.tool.prefs, false);
	public static boolean getCheckSizes() {
		//return checkSizes.getBoolean(); 
		return false;
	}
	public static void setCheckSizes(boolean on) { 
		//checkSizes.setBoolean(on); 
		checkSizes.setBoolean(false);
	}

	private static Pref relativeSizeTolerance = 
		Pref.makeDoublePref("RelativeSizeTolerance", NCC.tool.prefs, 0.0);
	public static double getRelativeSizeTolerance() {
		return relativeSizeTolerance.getDouble(); 
	}
	public static void setRelativeSizeTolerance(double d) { 
		relativeSizeTolerance.setDouble(d); 
	}

	private static Pref absoluteSizeTolerance = 
		Pref.makeDoublePref("AbsoluteSizeTolerance", NCC.tool.prefs, 0.0);
	public static double getAbsoluteSizeTolerance() {
		return absoluteSizeTolerance.getDouble(); 
	}
	public static void setAbsoluteSizeTolerance(double d) { 
		absoluteSizeTolerance.setDouble(d); 
	}

	private static Pref haltAfterFirstMismatch = 
		Pref.makeBooleanPref("HaltAfterFirstMismatch", NCC.tool.prefs, true);
	public static boolean getHaltAfterFirstMismatch() {
		return haltAfterFirstMismatch.getBoolean(); 
	}
	public static void setHaltAfterFirstMismatch(boolean on) { 
		haltAfterFirstMismatch.setBoolean(on); 
	}
}
