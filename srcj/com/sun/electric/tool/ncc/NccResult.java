/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: NccResult.java
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

/** The result of running a netlist comparison */
public class NccResult {
	private boolean exportMatch;
	private boolean topologyMatch;
	private boolean sizeMatch;
	private NccGlobals globalData;

	public NccResult(boolean exportNameMatch, boolean topologyMatch, 
			         boolean sizeMatch, NccGlobals globalData) {
		this.exportMatch = exportNameMatch;
		this.topologyMatch = topologyMatch;
		this.sizeMatch = sizeMatch;
		this.globalData = globalData;
	}
	/** aggregate the result of multiple comparisons */
	public void and(NccResult result) {
		exportMatch &= result.exportMatch;
		topologyMatch &= result.topologyMatch;
		sizeMatch &= result.sizeMatch;
		globalData = result.globalData;
	}
	/** No problem was found with Exports */ 
	public boolean exportMatch() {return exportMatch;}
	
	/** No problem was found with the network topology */
	public boolean topologyMatch() {return topologyMatch;}

	/** No problem was found with transistor sizes */
	public boolean sizeMatch() {return sizeMatch;}
	
	/** No problem was found */
	public boolean match() {return exportMatch && topologyMatch && sizeMatch;}
	
	NetEquivalence getNetEquivalence() {
		return new NetEquivalence(globalData.getEquivalentNets());
	}
	
	public String summary(boolean checkSizes) {
		String s;
		if (exportMatch) {
			s = "exports match, ";
		} else {
			s = "exports mismatch, ";
		}
		if (topologyMatch) {
			s += "topologies match, ";
		} else {
			s += "topologies mismatch, ";
		}
		if (!checkSizes) {
			s += "sizes not checked";
		} else if (sizeMatch) {
			s += "sizes match";
		} else {
			s += "sizes mismatch";
		}
		return s;
	}
}
