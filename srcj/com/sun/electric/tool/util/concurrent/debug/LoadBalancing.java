/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LoadBalancing.java
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
package com.sun.electric.tool.util.concurrent.debug;

import java.util.Iterator;
import java.util.Set;

import com.sun.electric.database.text.TextUtils;
import com.sun.electric.tool.util.CollectionFactory;
import com.sun.electric.tool.util.concurrent.runtime.PoolWorkerStrategy;

/**
 * @author fschmidt
 * 
 */
public class LoadBalancing implements IDebug {

	private static LoadBalancing instance = new LoadBalancing();
	private Set<PoolWorkerStrategy> workers;

	private LoadBalancing() {

		workers = CollectionFactory.createConcurrentHashSet();

	}

	public static LoadBalancing getInstance() {
		return instance;
	}

	public void registerWorker(PoolWorkerStrategy worker) {
		workers.add(worker);
	}

	public void reset() {
		workers.clear();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sun.electric.tool.util.concurrent.debug.IDebug#printStatistics()
	 */
	public void printStatistics() {
		Set<PoolWorkerStrategy> workersLocalCopy = CollectionFactory.copySetToConcurrent(workers);

		int sum = 0;
		for (Iterator<PoolWorkerStrategy> it = workersLocalCopy.iterator(); it.hasNext();) {
			sum += it.next().getExecutedCounter();
		}

		double mean = (double) sum / (double) workersLocalCopy.size();
		double var = 0.0;

		for (Iterator<PoolWorkerStrategy> it = workersLocalCopy.iterator(); it.hasNext();) {
			PoolWorkerStrategy worker = it.next();
			System.out.print(worker);
			System.out.print(" - ");
			System.out.println(TextUtils.getPercentageString((double) worker.getExecutedCounter() / (double) sum));
			var += Math.pow((double) worker.getExecutedCounter() - mean, 2);
		}
		
		var /= (double)workersLocalCopy.size();

		System.out.print("mean value");
		System.out.print(" - ");
		System.out.println(TextUtils.getPercentageString(mean / (double) sum));

		System.out.print("variance");
		System.out.print(" - ");
		System.out.println(var);
	}
}