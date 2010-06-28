/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Pipeline.java
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
package com.sun.electric.tool.util.concurrent.runtime.pipeline;

import java.util.List;

import com.sun.electric.tool.Job;
import com.sun.electric.tool.util.CollectionFactory;
import com.sun.electric.tool.util.IStructure;

// TODO termination? Jobs?
// TODO easy interface
// TODO type checks

/**
 * @author Felix Schmidt
 * 
 */
public class PipelineRuntime<PipeIn, PipeOut> {

	private List<Thread> threads = CollectionFactory.createLinkedList();

	public static enum PipelineWorkerStrategyType {
		simple;
	}

	private List<Stage<?, ?>> stages;

	public PipelineRuntime() {
		stages = CollectionFactory.createLinkedList();
	}

	public void input(PipeIn input) {
		stages.get(0).send(input);
	}

	// TODO handle different types, checks at runtime
	@SuppressWarnings("unchecked")
	public <Input, Output> void addStage(StageImpl<Input, Output> impl, int numOfWorkers) {
		Stage<Input, Output> stage = new Stage<Input, Output>(numOfWorkers);
		for (int i = 0; i < numOfWorkers; i++) {
			try {
				PipelineWorkerStrategy strategy = createPipelineWorker(
						PipelineWorkerStrategyType.simple, stage, (StageImpl<Input, Output>) impl
								.clone());
				Thread thread = new Thread(strategy, "StageID" + stages.size() + "_" +i);
				thread.start();
				threads.add(thread);
				stage.getWorkers().add(strategy);
			} catch (CloneNotSupportedException e) {
				e.printStackTrace();
			}
		}
		stages.add(stage);
		if (stages.size() > 1) {
			stages.get(stages.size() - 2).next = (Stage<?, ?>) stage;
		}
		
		if(Job.getDebug()) {
			System.out.println("Stage added: " + (stages.size() - 1) + "/" + numOfWorkers);
		}
	}

	public void shutdown() throws InterruptedException {
		for (Stage<?, ?> stage : stages) {
			stage.shutdown();
		}

		for (Thread thread : threads) {
			thread.join();
		}

		if (Job.getDebug()) {
			System.out.println("Pipeline shutdown");
		}
	}

	public static class Stage<Input, Output> {

		private IStructure<Input> inputQueue;
		private List<PipelineWorkerStrategy> workers;
		private Stage<?, ?> next;

		public Stage(int numOfWorkers) {
			this.inputQueue = CollectionFactory.createLockFreeQueue();
			this.workers = CollectionFactory.createLinkedList();
		}

		@SuppressWarnings("unchecked")
		public void send(Object item) {
			inputQueue.add((Input) item);
		}

		public void forward(Object item) {
			if (next != null) {
				next.send(item);
			}
		}

		public Input recv() {
			return this.inputQueue.remove();
		}

		public void setWorkers(List<PipelineWorkerStrategy> workers) {
			this.workers = workers;
		}

		public List<PipelineWorkerStrategy> getWorkers() {
			return workers;
		}

		public void shutdown() {
			for (PipelineWorkerStrategy strategy : workers) {
				strategy.shutdown();
			}
		}

		public void setNext(Stage<?, ?> next) {
			this.next = next;
		}

		public Stage<?, ?> getNext() {
			return next;
		}
	}

	public static abstract class StageImpl<Input, Output> implements Cloneable {
		public abstract Output execute(Input item);

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#clone()
		 */
		@Override
		public Object clone() throws CloneNotSupportedException {
			return super.clone();
		}
	}

	public static <Input, Output> PipelineWorkerStrategy createPipelineWorker(
			PipelineWorkerStrategyType type, Stage<Input, Output> stage,
			StageImpl<Input, Output> impl) {
		if (type == PipelineWorkerStrategyType.simple) {
			return new SimplePipelineWorker<Input, Output>(stage, impl);
		}
		return null;
	}
}
