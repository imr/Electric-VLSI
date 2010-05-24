/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DRCCheck.java
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

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.RTNode;
import com.sun.electric.technology.DRCTemplate;
import com.sun.electric.technology.Layer;
import com.sun.electric.tool.drc.DRC;
import com.sun.electric.tool.erc.ERCWellCheck.StrategyParameter;
import com.sun.electric.tool.erc.ERCWellCheck.WellBound;
import com.sun.electric.tool.erc.ERCWellCheck.WellCheckPreferences;
import com.sun.electric.tool.user.ErrorLogger;

/**
 * @author fschmidt
 * 
 */
public class DRCCheck implements WellCheckAnalysisStrategy {

	private Layer pWellLayer;
	private Layer nWellLayer;
	private RTNode pWellRoot;
	private RTNode nWellRoot;
	private StrategyParameter parameters;

	/**
	 * @param preferences
	 * @param pWellLayer
	 * @param nWellLayer
	 * @param pWellRoot
	 * @param nWellRoot
	 * @param errorLogger
	 * @param cell
	 */
	public DRCCheck(StrategyParameter parameters, Layer pWellLayer, Layer nWellLayer,
			RTNode pWellRoot, RTNode nWellRoot) {
		super();
		this.parameters = parameters;
		this.pWellLayer = pWellLayer;
		this.nWellLayer = nWellLayer;
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
		if (parameters.getWellPrefs().drcCheck) {
			long startTime = System.currentTimeMillis();
			DRCTemplate pRule = DRC.getSpacingRule(pWellLayer, null, pWellLayer, null, false, -1,
					0, 0);
			DRCTemplate nRule = DRC.getSpacingRule(nWellLayer, null, nWellLayer, null, false, -1,
					0, 0);
			if (pRule != null)
				findDRCViolations(pWellRoot, pRule.getValue(0));
			if (nRule != null)
				findDRCViolations(nWellRoot, nRule.getValue(0));

			long endTime = System.currentTimeMillis();
			System.out.println("   Design rule check took "
					+ TextUtils.getElapsedTime(endTime - startTime));
			startTime = endTime;
		}

	}

	private void findDRCViolations(RTNode rtree, double minDist) {
		for (int j = 0; j < rtree.getTotal(); j++) {
			if (rtree.getFlag()) {
				WellBound child = (WellBound) rtree.getChild(j);
				if (child.getNetID() == null)
					continue;

				// look all around this geometry for others in the well area
				Rectangle2D searchArea = new Rectangle2D.Double(child.getBounds().getMinX()
						- minDist, child.getBounds().getMinY() - minDist, child.getBounds()
						.getWidth()
						+ minDist * 2, child.getBounds().getHeight() + minDist * 2);
				for (RTNode.Search sea = new RTNode.Search(searchArea, rtree, true); sea.hasNext();) {
					WellBound other = (WellBound) sea.next();
					if (other.getNetID().getIndex() <= child.getNetID().getIndex())
						continue;
					if (child.getBounds().getMinX() > other.getBounds().getMaxX() + minDist
							|| other.getBounds().getMinX() > child.getBounds().getMaxX() + minDist
							|| child.getBounds().getMinY() > other.getBounds().getMaxY() + minDist
							|| other.getBounds().getMinY() > child.getBounds().getMaxY() + minDist)
						continue;

					PolyBase pb = new PolyBase(child.getBounds());
					double trueDist = pb.polyDistance(other.getBounds());
					if (trueDist < minDist) {
						List<PolyBase> polyList = new ArrayList<PolyBase>();
						polyList.add(new PolyBase(child.getBounds()));
						polyList.add(new PolyBase(other.getBounds()));
						parameters.getErrorLogger().logMessage(
								"Well areas too close (are " + TextUtils.formatDistance(trueDist)
										+ " but should be " + TextUtils.formatDistance(minDist)
										+ " apart)", polyList, parameters.getCell(), 0, true);
					}
				}
			} else {
				RTNode child = (RTNode) rtree.getChild(j);
				findDRCViolations(child, minDist);
			}
		}
	}

}
