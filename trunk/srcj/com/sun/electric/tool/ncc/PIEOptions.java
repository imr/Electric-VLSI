/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PIEOptions.java
 *
 * Copyright (c) 2006 Stephen Friedman
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

public class PIEOptions implements Serializable {
	static final long serialVersionUID = 1;
	
	/** Whether or not to factor the subcircuits */
	public boolean factorSubcircuit = false;

	/** Whether or not to allow port interchange */
	public boolean allowInterchange = false;
	
	/** Whether or not to allow serial/parallel transistor merging */
	public boolean serialParallelMerge = true;
	
	/** Whether or not to allow serial/parallel transistor merging */
	public boolean enableBacktracking = false;

	/** Whether or not to allow serial/parallel transistor merging */
	public int maxBacktrackingGuesses = -1;

	/** Whether or not to allow serial/parallel transistor merging */
	public int maxBacktrackingSpace = -1;

	/** Whether or not to test possible swap orders 
	 * during the run.  This is turned off initally
	 * to let ambiguity settle out, then it is turned
	 * on to complete the run.
	 */
	public boolean runSwapTests = false;
	
	public PIEOptions(){};
	
	public PIEOptions(PIEOptions other){
		this.factorSubcircuit = other.factorSubcircuit;
		this.allowInterchange = other.allowInterchange;
		this.runSwapTests = other.runSwapTests;
		this.serialParallelMerge = other.serialParallelMerge;
		this.enableBacktracking = other.enableBacktracking;
		this.maxBacktrackingGuesses = other.maxBacktrackingGuesses;
		this.maxBacktrackingSpace = other.maxBacktrackingSpace;
	}
}
