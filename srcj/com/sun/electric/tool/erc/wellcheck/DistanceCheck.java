/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DistanceCheck.java
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

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.RTNode;
import com.sun.electric.tool.erc.ERCWellCheck.StrategyParameter;
import com.sun.electric.tool.erc.ERCWellCheck.WellBound;
import com.sun.electric.tool.erc.ERCWellCheck.WellNet;

/**
 * @author Felix Schmidt
 * 
 */
public class DistanceCheck implements WellCheckAnalysisStrategy {

	private StrategyParameter parameter;
	private double worstPWellDist;
	
	@SuppressWarnings("unused")
	private Point2D worstPWellCon;
	
	@SuppressWarnings("unused")
	private Point2D worstPWellEdge;
	private double worstNWellDist;
	
	@SuppressWarnings("unused")
	private Point2D worstNWellCon;
	
	@SuppressWarnings("unused")
	private Point2D worstNWellEdge;
	private RTNode pWellRoot;
	private RTNode nWellRoot;

	/**
	 * @param preferences
	 * @param worstPWellDist
	 * @param worstPWellCon
	 * @param worstPWellEdge
	 * @param worstNWellDist
	 * @param worstNWellCon
	 * @param worstNWellEdge
	 * @param wellCons
	 * @param pWellRoot
	 * @param nWellRoot
	 */
	public DistanceCheck(StrategyParameter parameter, double worstPWellDist, Point2D worstPWellCon,
			Point2D worstPWellEdge, double worstNWellDist, Point2D worstNWellCon, Point2D worstNWellEdge,
			RTNode pWellRoot, RTNode nWellRoot) {
		super();
		this.parameter = parameter;
		this.worstPWellDist = worstPWellDist;
		this.worstPWellCon = worstPWellCon;
		this.worstPWellEdge = worstPWellEdge;
		this.worstNWellDist = worstNWellDist;
		this.worstNWellCon = worstNWellCon;
		this.worstNWellEdge = worstNWellEdge;
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
		if (parameter.getWellPrefs().findWorstCaseWell) {
			long startTime = System.currentTimeMillis();
			worstPWellDist = 0;
			worstPWellCon = null;
			worstPWellEdge = null;
			worstNWellDist = 0;
			worstNWellCon = null;
			worstNWellEdge = null;

			Map<Integer, WellNet> wellNets = new HashMap<Integer, WellNet>();
			for (WellCon wc : parameter.getWellCons()) {
				if (wc.getWellNum() == null)
					continue;
				Integer netNUM = new Integer(wc.getWellNum().getIndex());
				WellNet wn = wellNets.get(netNUM);
				if (wn == null) {
					wn = new WellNet(new ArrayList<Point2D>(), new ArrayList<WellCon>(), wc.getFun());
					wellNets.put(netNUM, wn);
				}
				wn.getContactsOnNet().add(wc);
			}

			findWellNetPoints(pWellRoot, wellNets);
			findWellNetPoints(nWellRoot, wellNets);

			for (Integer netNUM : wellNets.keySet()) {
				WellNet wn = wellNets.get(netNUM);
				for (Point2D pt : wn.getPointsOnNet()) {
					// find contact closest to this point
					double closestDist = Double.MAX_VALUE;
					Point2D closestCon = null;
					for (WellCon wc : wn.getContactsOnNet()) {
						double dist = wc.getCtr().distance(pt);
						if (dist < closestDist) {
							closestDist = dist;
							closestCon = wc.getCtr();
						}
					}

					// see if this distance is worst for the well type
					if (Utils.canBeSubstrateTap(wn.getFun())) {
						// pWell
						if (closestDist > worstPWellDist) {
							worstPWellDist = closestDist;
							worstPWellCon = closestCon;
							worstPWellEdge = pt;
						}
					} else {
						// nWell
						if (closestDist > worstNWellDist) {
							worstNWellDist = closestDist;
							worstNWellCon = closestCon;
							worstNWellEdge = pt;
						}
					}
				}
			}
			long endTime = System.currentTimeMillis();
			System.out.println("   Worst-case distance analysis took "
					+ TextUtils.getElapsedTime(endTime - startTime));
			startTime = endTime;
		}

	}

	private void findWellNetPoints(RTNode rtree, Map<Integer, WellNet> wellNets) {
		for (int j = 0; j < rtree.getTotal(); j++) {
			if (rtree.getFlag()) {
				WellBound child = (WellBound) rtree.getChild(j);
				if (child.getNetID() == null)
					continue;
				Integer netNUM = new Integer(child.getNetID().getIndex());
				WellNet wn = wellNets.get(netNUM);
				if (wn == null)
					continue;
				wn.getPointsOnNet().add(
						new Point2D.Double(child.getBounds().getMinX(), child.getBounds().getMinY()));
				wn.getPointsOnNet().add(
						new Point2D.Double(child.getBounds().getMaxX(), child.getBounds().getMinY()));
				wn.getPointsOnNet().add(
						new Point2D.Double(child.getBounds().getMaxX(), child.getBounds().getMaxY()));
				wn.getPointsOnNet().add(
						new Point2D.Double(child.getBounds().getMinX(), child.getBounds().getMaxY()));
			} else {
				RTNode child = (RTNode) rtree.getChild(j);
				findWellNetPoints(child, wellNets);
			}
		}
	}

}
