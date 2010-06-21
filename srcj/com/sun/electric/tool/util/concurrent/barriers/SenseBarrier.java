/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SenseBarrier.java
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
package com.sun.electric.tool.util.concurrent.barriers;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author fs239085
 * 
 */
public class SenseBarrier implements Barrier {
	private AtomicInteger count;
	private int size;
	private Boolean sense;
	private ThreadLocal<Boolean> threadSense;

	public SenseBarrier(int n) {
		count = new AtomicInteger(n);
		size = n;
		sense = false;
		threadSense = new ThreadLocal<Boolean>() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see java.lang.ThreadLocal#initialValue()
			 */
			@Override
			protected Boolean initialValue() {
				return !sense;
			}
		};

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sun.electric.tool.util.concurrent.barriers.Barrier#await()
	 */
	public void await() {
		Boolean mySense = threadSense.get();
		int position = count.getAndDecrement();
		if (position == 1) {
			count.set(size);
			sense = mySense;
		} else {
			while (sense != mySense) {}
		}
		threadSense.set(!mySense);
	}

}
