/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Stage.java
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
package com.sun.electric.tool.placement.forceDirected2.utils.concurrent;

import com.sun.electric.tool.placement.forceDirected2.forceDirected.staged.PlacementDTO;
import com.sun.electric.tool.placement.forceDirected2.utils.GlobalVars;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Parallel Placement
 * 
 * Base class for pipeline stages
 */
public class Stage {

	private List<StageWorker> layers;
	private IStructure<PlacementDTO> input;
	private IStructure<PlacementDTO> altInput;
	private List<Stage> nextStages;
	private List<Thread> threads;
	private int objectCounter = 0;
	private Map<StageWorker, Integer> balancingCounter = new HashMap<StageWorker, Integer>();

	public Stage(List<StageWorker> layer) {
		this.nextStages = new ArrayList<Stage>();
		this.layers = layer;
		this.input = new LockFreeQueue<PlacementDTO>();
		for (StageWorker worker : this.layers) {
			worker.setStage(this);
		}
	}

	public IStructure<PlacementDTO> getAltInput() {
		return this.altInput;
	}

	public IStructure<PlacementDTO> getInput(StageWorker worker) {
		if (GlobalVars.showBalancing && worker != null) {
			if (!this.balancingCounter.containsKey(worker)) {
				this.balancingCounter.put(worker, Integer.valueOf(1));
			} else {
				int i = this.balancingCounter.get(worker).intValue();
				i++;
				this.balancingCounter.put(worker, new Integer(i));
			}
		}
		return this.input;
	}

	public List<Stage> getNextStages() {
		return this.nextStages;
	}

	public synchronized int getObjectCounter() {
		return this.objectCounter;
	}

	protected List<Thread> getThreads() {
		return this.threads;
	}

	public synchronized void incObjectCounter() {
		this.objectCounter++;
	}

	public void join() throws InterruptedException {
		List<Thread> threads = this.getThreads();
		for (Thread t : threads) {
			t.join();
		}
	}

	public void sendToNextStage(PlacementDTO data) {
		for (Stage stage : this.nextStages) {
			stage.input.add(data);
		}
	}

	public void setAltInput(IStructure<PlacementDTO> altInput) {
		this.altInput = altInput;
	}

	public void start() {
		this.threads = new ArrayList<Thread>();
		for (StageWorker worker : this.layers) {
			Thread t = new Thread(worker);
			this.threads.add(t);
			t.start();
		}
	}

	public void stop() {
		int sum = 0;
		for (StageWorker worker : this.layers) {
			worker.shutdown();
			if (this.balancingCounter.containsKey(worker)) {
				sum += this.balancingCounter.get(worker).intValue();
			}
		}

		if (GlobalVars.showBalancing) {
			for (StageWorker worker : this.layers) {
				if (this.balancingCounter.containsKey(worker)) {
					double value = (double) this.balancingCounter.get(worker).intValue() / (double) sum;
					System.out.println(worker.toString() + ": " + value);
				} else {
					System.out.println(worker.toString() + ": 0");
				}
			}
		}

	}
}
