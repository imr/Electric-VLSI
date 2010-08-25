/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SeaOfGatesBase.java
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
package com.sun.electric.tool.routing.seaOfGates;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.routing.AbstractRoutingBaseClass;
import com.sun.electric.tool.routing.RoutingFrame;
import com.sun.electric.tool.routing.SeaOfGates;
import com.sun.electric.tool.routing.SeaOfGates.SeaOfGatesOptions;
import com.sun.electric.tool.routing.seaOfGates.SeaOfGatesEngineFactory.SeaOfGatesEngineType;

/**
 * @author Felix Schmidt
 *
 */
public abstract class SeaOfGatesBase extends AbstractRoutingBaseClass {

	/* (non-Javadoc)
	 * @see com.sun.electric.tool.routing.AbstractRoutingBaseClass#getRoutingFrame()
	 */
	@Override
	protected RoutingFrame getRoutingFrame() {
		// TODO Auto-generated method stub
		return null;
	}
	
	protected abstract SeaOfGatesEngineType getType();
	
	/* (non-Javadoc)
	 * @see com.sun.electric.tool.routing.AbstractRoutingBaseClass#testRouter()
	 */
	@Override
	public void testRouter() throws Exception {
		Cell cell = this.loadCell("PlacementTest", "PlacementTest4",
				"W:/workspace/regression/tools/Placement/data/libs/placementTests.jelib", LoadLibraryType.fileSystem);
		
		SeaOfGatesOptions options = new SeaOfGatesOptions();
		options.getOptionsFromPreferences();
		
		SeaOfGates.seaOfGatesRoute(cell, options, SeaOfGatesEngineType.oldThreads);
	}

}
