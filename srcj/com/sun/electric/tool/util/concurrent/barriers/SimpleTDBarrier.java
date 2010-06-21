/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SimpleTDBarrier.java
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
 * @author Felix Schmidt
 * 
 */
public class SimpleTDBarrier implements TDBarrier {

	private AtomicInteger count;

	public SimpleTDBarrier(int n) {
		count = new AtomicInteger(n);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sun.electric.tool.util.concurrent.barriers.TDBarrier#isTerminated()
	 */
	public boolean isTerminated() {
		return count.get() == 0;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.sun.electric.tool.util.concurrent.barriers.TDBarrier#setActive(boolean
	 * )
	 */
	public void setActive(boolean state) {
		if (state) {
			count.getAndDecrement();
		} else {
			count.getAndIncrement();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.sun.electric.tool.util.concurrent.barriers.Barrier#await()
	 */
	@Deprecated
	public void await() {
		throw new UnsupportedOperationException();

	}

}
