/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ConnectionCheck.java
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

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.topology.RTNode;
import com.sun.electric.tool.erc.ERCWellCheck.StrategyParameter;
import com.sun.electric.tool.erc.ERCWellCheck.WellBound;
import com.sun.electric.tool.erc.ERCWellCheck.WellType;

/**
 * @author fschmidt
 * 
 */
public class ConnectionCheck implements WellCheckAnalysisStrategy {

	private StrategyParameter parameter;
	private boolean hasPCon;
	private boolean hasNCon;
	private RTNode pWellRoot;
	private RTNode nWellRoot;

	/**
	 * @param errorLogger
	 * @param preferences
	 * @param hasPCon
	 * @param hasNCon
	 * @param cell
	 * @param pWellRoot
	 * @param nWellRoot
	 */
	public ConnectionCheck(StrategyParameter parameter, boolean hasPCon, boolean hasNCon,
			RTNode pWellRoot, RTNode nWellRoot) {
		super();
		this.parameter = parameter;
		this.hasPCon = hasPCon;
		this.hasNCon = hasNCon;
		this.pWellRoot = pWellRoot;
		this.nWellRoot = nWellRoot;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sun.electric.tool.erc.wellcheck.WellCheckAnalysisStrategy#execute()
	 */
	public void execute() {
		if (parameter.getWellPrefs().pWellCheck != 2)
			findUnconnected(pWellRoot, pWellRoot, WellType.pwell);
		if (parameter.getWellPrefs().nWellCheck != 2)
			findUnconnected(nWellRoot, nWellRoot, WellType.nwell);
		if (parameter.getWellPrefs().pWellCheck == 1 && !hasPCon) {
			parameter.getErrorLogger().logError("No P-Well contact found in this cell",
					parameter.getCell(), 0);
		}
		if (parameter.getWellPrefs().nWellCheck == 1 && !hasNCon) {
			parameter.getErrorLogger().logError("No N-Well contact found in this cell",
					parameter.getCell(), 0);
		}
	}

	private void findUnconnected(RTNode rtree, RTNode current, WellType type) {
		for (int j = 0; j < current.getTotal(); j++) {
			if (current.getFlag()) {
				WellBound child = (WellBound) current.getChild(j);
				if (child.getNetID() == null) {
					Utils.spreadWellSeed(child.getBounds().getCenterX(), child.getBounds()
							.getCenterY(), new NetValues(), rtree, 0);
					parameter.getErrorLogger().logError(
							"No " + type + "-Well contact in this area",
							new EPoint(child.getBounds().getCenterX(), child.getBounds()
									.getCenterY()), parameter.getCell(), 0);
				}
			} else {
				RTNode child = (RTNode) current.getChild(j);
				findUnconnected(rtree, child, type);
			}
		}
	}

}
