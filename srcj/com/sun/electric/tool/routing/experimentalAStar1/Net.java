/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Net.java
 * Written by: Christian Julg, Jonas Thedering (Team 1)
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
package com.sun.electric.tool.routing.experimentalAStar1;

import java.util.List;
import java.util.Vector;

/** 
 * Contains all segments belonging to the same netID
 * 
 * @author Christian Jülg
 * @author Jonas Thedering
 */
public class Net {

	// used to ensure getNetID() yields positive values
	private static final int NET_ID_OFFSET = 10000;

	private int netID;
	private List<Path> paths = new Vector<Path>();

	private int length = -1;

	// if true the path is already routed, or unroutable
	public boolean[] pathDone;

	public Net(int netID) {
		this.netID = netID;

		// offset ensures proper working of start/finish marking with getNetID values
		assert netID + NET_ID_OFFSET > 0;
	}

	public void initialize() {
		int n = paths.size();
		pathDone = new boolean[n];
	}

	/**
	 * offset used to ensure getNetID() yields positive values
	 */
	public int getNetID() {
		return netID + Net.NET_ID_OFFSET;
	}

	/**
	 * returns netID as given by Electric
	 */
	public int getElectricNetID() {
		return netID;
	}

	public List<Path> getPaths() {
		return paths;
	}

	/**
	 * total length for all paths of this net
	 * 
	 * used in comparisons
	 */
	public int getLengthEstimate() {
		if (length == -1) {
			int len = 0;
			for (Path p : paths){
				len += p.getLengthEstimate();
			}
			this.length = len;
		}
		return length ;
	}

	/**
	 * called only after all paths are either routed or marked unroutable
	 * 
	 * @return length estimate for sum of minimum length of all routable paths
	 */
	public int getRoutableLengthEstimate() {
		int len = 0;
		for (Path p : paths) {
			if (p.pathDone && !p.pathUnroutable) {
				len += p.getLengthEstimate();
			}
		}

		return len;
	}
	
	/** @return the sum of the overlap between this and the other net */
	public int getOverlapSum(Net other) {
		int sum = 0;
		for(Path path : paths)
			for(Path otherPath : other.paths) {
				sum += path.getOverlapAmount(otherPath);
			}
		
		return sum;
	}
}
