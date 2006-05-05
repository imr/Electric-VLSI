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

import java.io.Serializable;

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
	
	/** Perform a regression test of the net equivalence map. */
	//public boolean checkNetEquivalenceMap = false;
	
	/** This is false only for old regressions */
	public boolean oneNamePerPort = true;
}
