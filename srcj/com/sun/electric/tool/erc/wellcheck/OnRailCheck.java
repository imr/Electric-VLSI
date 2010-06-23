/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: OnRailCheck.java
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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.tool.erc.ERCWellCheck.StrategyParameter;
import com.sun.electric.tool.erc.ERCWellCheck.Transistor;
import com.sun.electric.tool.util.CollectionFactory;

/**
 * @author Felix Schmidt
 * 
 */
public class OnRailCheck implements WellCheckAnalysisStrategy {

	private Set<Integer> networkExportAvailable;
	private Set<Integer> networkWithExportCache;
	private Map<Integer, List<Transistor>> transistors;
	private List<Transistor> alreadyHit;
	private StrategyParameter parameter;

	/**
	 * @param preferences
	 * @param wellCons
	 * @param networkExportAvailable
	 * @param transistors
	 * @param errorLogger
	 * @param cell
	 */
	public OnRailCheck(StrategyParameter parameter, Set<Integer> networkExportAvailable,
			Map<Integer, List<Transistor>> transistors) {
		super();
		this.networkExportAvailable = networkExportAvailable;
		this.transistors = transistors;
		this.networkWithExportCache = CollectionFactory.createHashSet();
		this.parameter = parameter;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sun.electric.tool.erc.wellcheck.WellCheckAnalysisStrategy#execute()
	 */
	public void execute() {
		int cacheHits = 0;
		// System.out.println(wellCons.size());
		for (WellCon wc : parameter.getWellCons()) {

			if (!wc.isOnRail()) {
				if (networkExportAvailable.contains(wc.getNetNum()))
					wc.setOnRail(true);
				else if (networkWithExportCache.contains(wc.getNetNum())) {
					wc.setOnRail(true);
					cacheHits++;
				} else {
					Set<Integer> startPath = new HashSet<Integer>();
					List<Transistor> startTrans = transistors.get(wc.getNetNum());
					if (startTrans != null) {
						if (createTransistorChain(startPath, startTrans.get(0), false)) {
							wc.setOnRail(true);
							networkWithExportCache.addAll(startPath);
						}
					}
				}
			}

			if (!(wc.isOnRail() || wc.isOnProperRail())) {
				if (Utils.canBeSubstrateTap(wc.getFun())) {
					if (parameter.getWellPrefs().mustConnectPWellToGround) {
						parameter.getErrorLogger().logError(
								"P-Well contact '" + wc.getNi().getName() + "' not connected to ground",
								new EPoint(wc.getCtr().getX(), wc.getCtr().getY()), parameter.getCell(), 0);
					}
				} else {
					if (parameter.getWellPrefs().mustConnectNWellToPower) {
						parameter.getErrorLogger().logError(
								"N-Well contact '" + wc.getNi().getName() + "' not connected to power",
								new EPoint(wc.getCtr().getX(), wc.getCtr().getY()), parameter.getCell(), 0);
					}
				}
			}
		}
	}

	private boolean createTransistorChain(Set<Integer> path, Transistor node, boolean result) {
		alreadyHit = new LinkedList<Transistor>();

		result |= createTransistorRec(path, node, result, node.getDrainNet().get());
		if (!result)
			result |= createTransistorRec(path, node, result, node.getSourceNet().get());

		return result;

	}

	private boolean createTransistorRec(Set<Integer> path, Transistor node, boolean result, int num) {

		List<Transistor> transis = new LinkedList<Transistor>();
		transis.add(node);
		alreadyHit.add(node);

		while (transis.size() > 0) {
			Transistor transistor = transis.get(0);

			transis.remove(transistor);

			Integer neighbor = null;

			if (transistor.getDrainNet().get() == num) {
				neighbor = transistor.getSourceNet().get();
			} else {
				neighbor = transistor.getDrainNet().get();
			}

			path.add(neighbor);
			num = neighbor;

			result |= networkExportAvailable.contains(neighbor);

			if (!result) {
				result |= networkWithExportCache.contains(neighbor);
			}

			// cut the line
			if (result)
				return result;

			for (Transistor trans : transistors.get(neighbor)) {
				if (!alreadyHit.contains(trans)) {
					transis.add(trans);
					alreadyHit.add(trans);
				}
			}

		}

		return result;
	}

}
