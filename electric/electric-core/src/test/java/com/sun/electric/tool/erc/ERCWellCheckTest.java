/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ERCWellCheckTest.java
 *
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.tool.erc;

import org.junit.Test;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.erc.ERCWellCheck.WellCheckPreferences;
import com.sun.electric.tool.util.test.AbstractJunitBaseClass;
import com.sun.electric.util.config.Configuration;

/**
 * @author Felix Schmidt
 * 
 */
public class ERCWellCheckTest extends AbstractJunitBaseClass {

	@Test
	public void testERCWellCheck() throws Exception {
		Cell cell = this.loadCell("placementTests", "PlacementTest4");
		WellCheckPreferences wellPrefs = new WellCheckPreferences(false);
		wellPrefs.drcCheck = false;
		wellPrefs.pWellCheck = 1;
		wellPrefs.nWellCheck = 1;
		wellPrefs.maxProc = 1;
//		wellPrefs.maxProc = 2;

		ERCWellCheck.checkERCWell(cell, wellPrefs);
	}

}
