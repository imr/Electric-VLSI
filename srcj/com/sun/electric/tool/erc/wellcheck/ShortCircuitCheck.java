/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ShortCircuitCheck.java
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
package com.sun.electric.tool.erc.wellcheck;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.tool.erc.ERCWellCheck.StrategyParameter;

/**
 * @author Felix Schmidt
 * 
 */
public class ShortCircuitCheck implements WellCheckAnalysisStrategy {

	private StrategyParameter parameter;

	/**
	 * @param wellCons
	 * @param errorLogger
	 * @param cell
	 */
	public ShortCircuitCheck(StrategyParameter parameter) {
		super();
		this.parameter = parameter;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sun.electric.tool.erc.wellcheck.WellCheckAnalysisStrategy#execute()
	 */
	public void execute() {
		Map<Integer, WellCon> wellContacts = new HashMap<Integer, WellCon>();
		Map<Integer, Set<Integer>> wellShorts = new HashMap<Integer, Set<Integer>>();
		
		for (WellCon wc : parameter.getWellCons()) {
			Integer wellIndex = new Integer(wc.getWellNum().getIndex());
			WellCon other = wellContacts.get(wellIndex);
			if (other == null)
				wellContacts.put(wellIndex, wc);
			else {
				if (wc.getNetNum() != other.getNetNum() && wc.getNi() != other.getNi()) {
					Integer wcNetNum = new Integer(wc.getNetNum());
					Set<Integer> shortsInWC = wellShorts.get(wcNetNum);
					if (shortsInWC == null) {
						shortsInWC = new HashSet<Integer>();
						wellShorts.put(wcNetNum, shortsInWC);
					}

					Integer otherNetNum = new Integer(other.getNetNum());
					Set<Integer> shortsInOther = wellShorts.get(otherNetNum);
					if (shortsInOther == null) {
						shortsInOther = new HashSet<Integer>();
						wellShorts.put(otherNetNum, shortsInOther);
					}

					// give error if not seen before
					if (!shortsInWC.contains(otherNetNum)) {
						List<EPoint> pointList = new ArrayList<EPoint>();
						pointList.add(new EPoint(wc.getCtr().getX(), wc.getCtr().getY()));
						pointList.add(new EPoint(other.getCtr().getX(), other.getCtr().getY()));
						parameter.getErrorLogger().logMessage("Short circuit between well contacts",
								pointList, parameter.getCell(), 0, true);
						shortsInWC.add(otherNetNum);
						shortsInOther.add(wcNetNum);
					}
				}
			}
		}

	}

}
