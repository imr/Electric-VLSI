/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ERCWellCheck_T.java
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
package com.sun.electric.tool.erc;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import com.sun.electric.tool.erc.wellcheck.Utils;
import com.sun.electric.tool.erc.wellcheck.WellCon;
import com.sun.electric.tool.util.CollectionFactory;

/**
 * @author fs239085
 * 
 */
public class ERCWellCheck_T  {

	@Test
	public void testGetStarters() {

		Assert.assertTrue(checkStarters(Utils.getStarters(1, createConsList(10)), 1));
		Assert.assertTrue(checkStarters(Utils.getStarters(2, createConsList(10)), 2));
		Assert.assertTrue(checkStarters(Utils.getStarters(10, createConsList(10)), 10));
		Assert.assertTrue(checkStarters(Utils.getStarters(100, createConsList(100000)), 100));

	}

	@Test(expected = IndexOutOfBoundsException.class)
	public void testGetStartersErrorCase() {
		Assert.assertTrue(checkStarters(Utils.getStarters(101, createConsList(10)), 10));
	}

	private boolean checkStarters(WellCon[] starters, int count) {
		Set<WellCon> used = new HashSet<WellCon>();
		for (WellCon starter : starters) {
			if (used.contains(starter))
				return false;
		}
		return starters.length == count;
	}

	public void testWellConSorting() {
		List<WellCon> wellCons = new LinkedList<WellCon>();
	}

	private List<WellCon> createConsList(int amount) {
		List<WellCon> cons = CollectionFactory.createArrayList();

		for (int i = 0; i < amount; i++) {
			cons.add(new WellCon(null, i, null, false, false, null, null));
		}

		return cons;

	}

}
