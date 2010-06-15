/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: MultiThreadedRandomizer.java
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
package com.sun.electric.tool.util.concurrent.runtime;

import java.util.Random;

/**
 * @author fs239085
 * 
 */
public class MultiThreadedRandomizer {

	private int numOfCores;
	private Random[] randomizers;

	public MultiThreadedRandomizer(int numOfCores) {
		this.numOfCores = numOfCores;
		
		randomizers = new Random[this.numOfCores];

		Random rand = new Random(System.currentTimeMillis());
		for (int i = 0; i < this.numOfCores; i++) {
			randomizers[i] = new Random(rand.nextLong());
		}
	}

	public Random getRandomizer() {
		int threadId = ThreadID.get();
		if (threadId < 0 || threadId <= numOfCores) return new Random(System.currentTimeMillis());
		return randomizers[ThreadID.get()];
	}

	public Random getRandomizer(int n) {
		return randomizers[n];
	}

}
