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

import java.io.OutputStream;

import com.sun.electric.tool.ncc.basic.Messenger;

public class NccOptions {
	/** enable size checking */
	public boolean checkSizes = false;
	public double absoluteSizeTolerance;
	public double relativeSizeTolerance;

	/** merge parallel Cells into one. For Russell Kao use only! */
	public boolean mergeParallelCells = false;

	/** print lots of progress messages. For Russell Kao use only! */
	public boolean verbose = false;
	
	/** for hierarchical comparisons try to continue comparing
	 * higher up in the hierarchy even if this Cell doesn't match */
	public boolean haltAfterFirstMismatch = true;
	
	/** If hash code partitioning detects a mismatch, how many mismatched 
	 * Part or Wire Equivalence Classes should I print? */
	public int numMismatchedEquivClassesToPrint = 1000;
	
	/** If hash code partitioning detects a mismatch, how many matched
	 * Part or Wire Equivalence Classes should I print? */
	public int numMatchedEquivClassesToPrint = 1000;
}
