/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CollectionFactory_T.java
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
package com.sun.electric.tool.util;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.sun.electric.tool.util.datastructures.ImmutableList;

/**
 * @author Felix Schmidt
 * 
 */
public class CollectionFactoryTest {

	@Test
	public void testCopyListToImmutableList() {
		List<Integer> list = CollectionFactory.createArrayList();
		for (int i = 0; i < 10; i++) {
			list.add(i);
		}

		ImmutableList<Integer> imList = CollectionFactory.copyListToImmutableList(list);

		Iterator<Integer> imIt = imList.iterator();
		int i = 9;
		while (imIt.hasNext()) {
			Integer current = imIt.next();
			Integer expected = list.get(i);

			Assert.assertEquals(expected, current);
			i--;
		}
	}

	@Test
	public void testCopySetToList() {
		Random rand = new Random(System.currentTimeMillis());
		Set<Integer> set = CollectionFactory.createHashSet();

		for (int i = 0; i < 10; i++) {
			set.add(rand.nextInt(100));
		}

		List<Integer> list = CollectionFactory.copySetToList(set);

		for (int i = 0; i < 10; i++) {
			set.remove(list.get(i));
		}

		Assert.assertEquals(0, set.size());
	}

}
