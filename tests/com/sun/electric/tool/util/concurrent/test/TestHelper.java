/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TestHelper.java
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
package com.sun.electric.tool.util.concurrent.test;

import java.util.Random;

/**
 * @author Felix Schmidt
 * 
 */
public class TestHelper {

	public static int[][] createMatrix(int sizeX, int sizeY, int maximumValue) {
		int[][] result = new int[sizeY][sizeX];

		Random rand = new Random(System.currentTimeMillis());

		for (int i = 0; i < sizeY; i++)
			for (int j = 0; j < sizeX; j++)
				result[i][j] = rand.nextInt(maximumValue);

		return result;
	}
	
	public static Integer[][] createMatrixInteger(int sizeX, int sizeY, int maximumValue) {
		Integer[][] result = new Integer[sizeY][sizeX];

		Random rand = new Random(System.currentTimeMillis());

		for (int i = 0; i < sizeY; i++)
			for (int j = 0; j < sizeX; j++)
				result[i][j] = rand.nextInt(maximumValue);

		return result;
	}
	
	public static float[][] createMatrix(int sizeX, int sizeY) {
		float[][] result = new float[sizeY][sizeX];

		Random rand = new Random(System.currentTimeMillis());

		for (int i = 0; i < sizeY; i++)
			for (int j = 0; j < sizeX; j++)
				result[i][j] = rand.nextFloat();

		return result;
	}
}
