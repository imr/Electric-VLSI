/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NccPreferences.java
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
import com.sun.electric.database.text.Pref;

/**
 * Contains NCC preferences
 */
public class NccPreferences {
	public static NccPreferences tool = new NccPreferences();
	// per-package namespace for preferences
	private Pref.Group prefs = Pref.groupForPackage(this.getClass());

	private static Pref checkSizes = 
		Pref.makeBooleanPref("CheckSizes", NccPreferences.tool.prefs, false);
	public static boolean getCheckSizes() {
		return checkSizes.getBoolean(); 
	}
	public static void setCheckSizes(boolean on) { 
		checkSizes.setBoolean(on); 
	}

	private static Pref relativeSizeTolerance = 
		Pref.makeDoublePref("RelativeSizeTolerance", NccPreferences.tool.prefs, 0.0);
	public static double getRelativeSizeTolerance() {
		return relativeSizeTolerance.getDouble(); 
	}
	public static void setRelativeSizeTolerance(double d) { 
		relativeSizeTolerance.setDouble(d); 
	}

	private static Pref absoluteSizeTolerance = 
		Pref.makeDoublePref("AbsoluteSizeTolerance", NccPreferences.tool.prefs, 0.0);
	public static double getAbsoluteSizeTolerance() {
		return absoluteSizeTolerance.getDouble(); 
	}
	public static void setAbsoluteSizeTolerance(double d) { 
		absoluteSizeTolerance.setDouble(d); 
	}

	private static Pref haltAfterFirstMismatch = 
		Pref.makeBooleanPref("HaltAfterFirstMismatch", NccPreferences.tool.prefs, true);
	public static boolean getHaltAfterFirstMismatch() {
		return haltAfterFirstMismatch.getBoolean(); 
	}
	public static void setHaltAfterFirstMismatch(boolean on) { 
		haltAfterFirstMismatch.setBoolean(on); 
	}
    
	private static Pref skipPassed = 
		Pref.makeBooleanPref("SkipPassed", NccPreferences.tool.prefs, false);
	public static boolean getSkipPassed() {
		return skipPassed.getBoolean(); 
	}
	public static void setSkipPassed(boolean on) { 
		skipPassed.setBoolean(on); 
	}
	
	private static Pref maxMatchedClasses =
		Pref.makeIntPref("MaxMatchedClasses", NccPreferences.tool.prefs, 10);
	public static int getMaxMatchedClasses() {
		return maxMatchedClasses.getInt();
	}
	public static void setMaxMatchedClasses(int i) {
		maxMatchedClasses.setInt(i);
	}

	private static Pref maxMismatchedClasses =
		Pref.makeIntPref("MaxMismatchedClasses", NccPreferences.tool.prefs, 10);
	public static int getMaxMismatchedClasses() {
		return maxMismatchedClasses.getInt();
	}
	public static void setMaxMismatchedClasses(int i) {
		maxMismatchedClasses.setInt(i);
	}

	private static Pref maxClassMembers =
		Pref.makeIntPref("MaxClassMembers", NccPreferences.tool.prefs, 10);
	public static int getMaxClassMembers() {
		return maxClassMembers.getInt();
	}
	public static void setMaxClassMembers(int i) {
		maxClassMembers.setInt(i);
	}

	private static Pref operation =
		Pref.makeIntPref("Operation", NccPreferences.tool.prefs, NccOptions.HIER_EACH_CELL);
	public static int getOperation() {
		int op = operation.getInt();
		// guard against corrupted preferences
		if (op<NccOptions.HIER_EACH_CELL || op>NccOptions.LIST_ANNOTATIONS) return NccOptions.HIER_EACH_CELL; 
		return op;
	}
	public static void setOperation(int i) {
		operation.setInt(i);
	}

	private static int boundStatus(int s) {		
		s = Math.max(s, 0);
		s = Math.min(s, 3);
		return s;
	}

	private static Pref howMuchStatus =
		Pref.makeIntPref("HowMuchStatus", NccPreferences.tool.prefs, 0);
	public static int getHowMuchStatus() {
		return boundStatus(howMuchStatus.getInt());
	}
	public static void setHowMuchStatus(int i) {
		howMuchStatus.setInt(boundStatus(i));
	}
}
