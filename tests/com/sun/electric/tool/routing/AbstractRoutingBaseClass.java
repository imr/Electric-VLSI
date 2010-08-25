/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AbstractRoutingBaseClass.java
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
package com.sun.electric.tool.routing;

import org.junit.Test;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.util.test.AbstractJunitBaseClass;

/**
 * @author Felix Schmidt
 * 
 */
public abstract class AbstractRoutingBaseClass extends AbstractJunitBaseClass {

	protected int testRoutingAlgorithm(RoutingFrame router, String libName, String cellName, String fileName)
			throws Exception {
		Cell cell = this.loadCell(libName, cellName, fileName);
		return testRoutingAlgorithm(router, cell);
	}

	protected int testRoutingAlgorithm(RoutingFrame router, Cell cell) {
		return router.doRouting(cell);
	}

	protected abstract RoutingFrame getRoutingFrame();

	@Test
	public void testRouter() throws Exception {
		Cell cell = this.loadCell("PlacementTest", "PlacementTest4",
				"W:/workspace/regression/tools/Placement/data/libs/placementTests.jelib", LoadLibraryType.fileSystem);
		this.testRoutingAlgorithm(getRoutingFrame(), cell);
	}

}
