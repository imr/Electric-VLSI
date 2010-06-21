/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: ThreadID.java
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

/**
 * This class provides unique thread IDs for each thread. A thread gets a thread
 * ID assigned when get or set is called within the thread.
 * 
 * @author Felix Schmidt
 * 
 */
public class ThreadID {

	private static volatile int nextID = 0;

	private static class ThreadLocalID extends ThreadLocal<Integer> {
		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.ThreadLocal#initialValue()
		 */
		@Override
		protected synchronized Integer initialValue() {
			return nextID++;
		}
	}

	private static ThreadLocalID threadID = new ThreadLocalID();

	/**
	 * Get the thread ID of the current thread. The thead-id matching is done by
	 * a thread local variable.
	 * 
	 * @return
	 */
	public static int get() {
		return threadID.get();
	}

	/**
	 * Set the thread ID to a given value
	 * 
	 * @param index
	 */
	public static void set(int index) {
		threadID.set(index);
	}

	/**
	 * reset the all thread IDs. Call this before you start a new thread pool to
	 * make sure, that the thread IDs starts at 0
	 */
	public static void reset() {
		ThreadID.threadID = new ThreadLocalID();
		nextID = 0;
	}

}
