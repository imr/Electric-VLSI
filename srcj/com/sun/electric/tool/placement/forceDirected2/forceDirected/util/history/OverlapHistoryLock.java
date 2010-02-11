/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: OverlapHistoryLock.java
 * Written by Team 7: Felix Schmidt, Daniel Lechner
 * 
 * This code has been developed at the Karlsruhe Institute of Technology (KIT), Germany, 
 * as part of the course "Multicore Programming in Practice: Tools, Models, and Languages".
 * Contact instructor: Dr. Victor Pankratius (pankratius@ipd.uka.de)
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
package com.sun.electric.tool.placement.forceDirected2.forceDirected.util.history;

import com.sun.electric.tool.placement.PlacementFrame.PlacementNode;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Parallel Placement
 */
public class OverlapHistoryLock extends OverlapHistory<PlacementNode> {

	private static OverlapHistoryLock instance = new OverlapHistoryLock();
	private static ReentrantLock lock;

	public static OverlapHistoryLock getInstance() {
		return instance;
	}

	private OverlapHistoryLock() {
		lock = new ReentrantLock(true);
	}

	@Override
	public boolean isMovementInHistory(PlacementNode node1, PlacementNode node2) {

		boolean result = false;

		lock.lock();

		result = super.isMovementInHistory(node1, node2);

		lock.unlock();

		return result;
	}

	@Override
	public void removeHistory(PlacementNode node1, PlacementNode node2) {

		lock.lock();

		super.removeHistory(node1, node2);

		lock.unlock();
	}

	@Override
	public void saveHistory(PlacementNode node1, PlacementNode node2) {

		lock.lock();

		super.saveHistory(node1, node2);

		lock.unlock();
	}
}
