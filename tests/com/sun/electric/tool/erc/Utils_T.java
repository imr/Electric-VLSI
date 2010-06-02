/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Utils_T.java
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

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import junit.framework.Assert;

import org.junit.Test;

import com.sun.electric.tool.erc.wellcheck.Utils;

/**
 * @author fs239085
 * 
 */
public class Utils_T {

	@Test
	public void testSafeListRemove() {

		Random rand = new Random(System.currentTimeMillis());
		List<Integer> list = new LinkedList<Integer>();

		for (int i = 0; i < 10; i++) {
			list.add(rand.nextInt());
		}
		
		Assert.assertNotNull(Utils.safeListRemove(list, 0));
	}

}
