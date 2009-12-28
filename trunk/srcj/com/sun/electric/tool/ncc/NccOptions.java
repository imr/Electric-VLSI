/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NccOptions.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
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

import java.io.Serializable;
/** NccOptions allows the user to control exactly how NCC performs
 * its comparison. When the NCC command is launched, NCC
 * examines the NCC user preferences to create an NccOptions
 * object. Thereafter, NCC only looks at the NccOptions in order
 * to determine what to do.
 * 
 * The only place where NCC examines the NCC user preferences
 * is the getOptionsFromNccPreferences() method below. Everywhere else
 * NCC uses NccOptions.
 * 
 * My use of NccOptions to control NCC allows NCC to be thread safe.
 * Multiple threads can run NCC at the same time. Each thread 
 * calls NCC with a different NccOptions object and each NccOptions
 * object can specify different options.
 */
public class NccOptions implements Serializable {
	static final long serialVersionUID = 0;
	
	/** what NCC operation to perform */
	public static final int HIER_EACH_CELL = 0;
	public static final int FLAT_EACH_CELL = 1;		// for regression testing
	public static final int FLAT_TOP_CELL = 2;
	public static final int LIST_ANNOTATIONS = 3;
	public int operation = HIER_EACH_CELL;
	
	/** enable size checking */
	public boolean checkSizes = false;
	public double absoluteSizeTolerance;
	public double relativeSizeTolerance;

	/** Don't recheck Cells that have passed NCC in the current run of 
	 * Electric. This is a hack because it doesn't re-check a Cell if
	 * it has been modified since the last comparison. */
	public boolean skipPassed = false;

	/** How many progress messages to print (0 means minimal, 10 means maximum). */
	public int howMuchStatus = 0;
	
	/** for hierarchical comparisons try to continue comparing
	 * higher up in the hierarchy even if this Cell doesn't match */
	public boolean haltAfterFirstMismatch = true;
	
	/** If hash code partitioning detects a mismatch, how many mismatched 
	 * Part or Wire Equivalence Classes should I print? */
	public int maxMismatchedEquivRecsToPrint = 10;
	
	/** If hash code partitioning detects a mismatch, how many matched
	 * Part or Wire Equivalence Classes should I print? */
	public int maxMatchedEquivRecsToPrint = 10;
	
	/** For all diagnostic messages, how many members of an equivalence
	 * class should I print */ 
	public int maxEquivRecMembersToPrint = 10;

	/** This is false only for old regressions */
	public boolean oneNamePerPort = true;
	
	/** Check the body connections of MOS transistors. */
	public boolean checkBody = false;
	
	/** Construct an NccOptions with the default options */
	public NccOptions() {}
	
	/** Construct an NccOptions that is a copy of o 
	 * @param o the NccOptions to copy
	 */
	public NccOptions(NccOptions o) {
		operation = o.operation;
		checkSizes = o.checkSizes;
		absoluteSizeTolerance = o.absoluteSizeTolerance;
		relativeSizeTolerance = o.relativeSizeTolerance;
		skipPassed = o.skipPassed;
		howMuchStatus = o.howMuchStatus;
		haltAfterFirstMismatch = o.haltAfterFirstMismatch;
		maxMismatchedEquivRecsToPrint = o.maxMismatchedEquivRecsToPrint;
		maxMatchedEquivRecsToPrint = o.maxMatchedEquivRecsToPrint;
		maxEquivRecMembersToPrint = o.maxEquivRecMembersToPrint;
		oneNamePerPort = o.oneNamePerPort;
		checkBody = o.checkBody;
	}
	
	/** Look at the NCC user preferences and construct an NccOptions object
	 * that reflects those preferences. This is the only place in all of NCC
	 * where the NCC user preferences are examined.
	 */
	public static NccOptions getOptionsFromNccPreferences() {
		NccOptions options = new NccOptions();
		options.operation = NccPreferences.getOperation();

		options.checkSizes = NccPreferences.getCheckSizes();
		// convert percent to fraction
		options.relativeSizeTolerance = NccPreferences.getRelativeSizeTolerance()/100;
		options.absoluteSizeTolerance = NccPreferences.getAbsoluteSizeTolerance();
		
		options.checkBody = NccPreferences.getCheckBody();

		options.skipPassed = NccPreferences.getSkipPassed();
		options.howMuchStatus = NccPreferences.getHowMuchStatus();
		options.haltAfterFirstMismatch = NccPreferences.getHaltAfterFirstMismatch();
		options.maxMismatchedEquivRecsToPrint = NccPreferences.getMaxMismatchedClasses();
		options.maxMatchedEquivRecsToPrint = NccPreferences.getMaxMatchedClasses();
		options.maxEquivRecMembersToPrint = NccPreferences.getMaxClassMembers();
		
		/** Subtle!!!!!
		 * NCC used to have a more flexible Export matching algorithm. If
		 * the schematic had a wire with Exports A, B, and C, and the 
		 * equivalent wire in the layout had Exports B, C, and D, then NCC
		 * used to say the Export names matched. Later, I made NCC's 
		 * Export matcher more restrictive in order to match the behavior
		 * of commercial tools. Now NCC complains that the lexigraphically
		 * first schematic Export A doesn't match the lexigraphically first
		 * layout Export B. 
		 * 
		 *  However, most of NCC's regressions were created for the old 
		 *  Export checker. Therefore the regression scripts enable the 
		 *  old Export checker. However, if you invoke NCC from the GUI 
		 *  to compare designs in the regression database they will fail.
		 *  
		 *  The following comment enables the old Export checker when
		 *  NCC is invoked from the GUI. I uncomment if I want to
		 *  run NCC on old regression data.
		 */
		// for testing old regressions only!
		//options.oneNamePerPort = false;
		
		return options;
	}

}
