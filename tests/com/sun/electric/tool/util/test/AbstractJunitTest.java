/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AbstractJunitTest.java
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
package com.sun.electric.tool.util.test;

import org.junit.Assert;
import org.junit.Test;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;

/**
 * @author Felix Schmidt
 * 
 */
public class AbstractJunitTest extends AbstractJunitBaseClass {

	@Test
	public void testLoadLibrary() throws Exception {
		Library lib = this.loadLibrary("testLib", "/com/sun/electric/tool/util/test/testData/testLib.jelib");

		Assert.assertNotNull(lib);
	}

	@Test
	public void testLoadCellLayout() throws Exception {
		Cell cell = this
				.loadCell("testLib", "testCell", "/com/sun/electric/tool/util/test/testData/testLib.jelib");
		Assert.assertNotNull(cell);
		Assert.assertTrue(cell.getView().equals(View.LAYOUT));
	}

	@Test
	public void testLoadCellSchematic() throws Exception {
		Cell cell = this.loadCell("testLib", "testSch", "/com/sun/electric/tool/util/test/testData/testLib.jelib");
		Assert.assertNotNull(cell);
		Assert.assertTrue(cell.getView().equals(View.SCHEMATIC));
	}

}
