/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PlacementProperties.java
 * Written by Team 7: Felix Schmidt, Daniel Lechner
 * 
 * This code has been developed at the Karlsruhe Institute of Technology (KIT), Germany, 
 * as part of the course "Multicore Programming in Practice: Tools, Models, and Languages".
 * Contact instructor: Dr. Victor Pankratius (pankratius@ipd.uka.de)
 *
 * Copyright (c) 2010 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.placement.forceDirected2.utils;

/**
 * Parallel Placement
 * 
 * Property mapper for placement algorithms. This class implements the Singleton
 * Pattern. For getting an instance call PlacementProperties.getInstance
 */
public class PlacementProperties {

	private static PlacementProperties instance = new PlacementProperties();

	/**
	 * Get the instance of PlacementProperties. This funktion is required for
	 * singleton pattern.
	 * 
	 * @return the instance
	 * @throws Exception
	 */
	public static PlacementProperties getInstance() {
		return PlacementProperties.instance;
	}

	/**
	 * constructor, opens the file
	 * 
	 * @throws Exception
	 */
	private PlacementProperties() {
	}

	/**
	 * This function returns the divergence of the square root solution.
	 * 
	 * @return divergence
	 */
	public int getDivergence() {
		return GlobalVars.divergence.intValue();
	}

	/**
	 */
	public int getIterations() {
		return GlobalVars.iterations.intValue();
	}

	/**
	 */
	public int getNotMovedMax() {
		return GlobalVars.notMovedMax.intValue();
	}

	/**
	 * 
	 * @return number of expected threads
	 */
	public int getNumOfThreads() {
		return GlobalVars.numOfThreads.intValue();
	}

	/**
	 */
	public double getOverlappingThreshold() {
		return GlobalVars.overlappingThreshold.doubleValue();
	}

	/**
	 */
	public int getPadding() {
		return GlobalVars.padding.intValue();
	}

	/**
	 * 
	 * @return timeout for running the placement
	 */
	public int getTimeout() {
		return GlobalVars.timeout.intValue();
	}
}
