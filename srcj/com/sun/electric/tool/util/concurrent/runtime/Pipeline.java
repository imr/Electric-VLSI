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
package com.sun.electric.tool.util.concurrent.runtime;

import java.util.List;

import com.sun.electric.tool.util.CollectionFactory;
import com.sun.electric.tool.util.IStructure;

/**
 * @author fs239085
 * 
 */
public class Pipeline {

	private List<Stage<?, ?>> stages;

	/**
	 * 
	 */
	public Pipeline() {
		stages = CollectionFactory.createConcurrentLinkedList();
	}

	@SuppressWarnings("unchecked")
	public <T, K> void addFilter(Filter<T, K> filter, int n) {
		Stage<T, K> stage = new Stage<T, K>(n, filter);
		filter.setStage(stage);

		if (stages.size() != 0)
			((Stage<T, K>) stages.get(stages.size() - 1)).setOutputStage(stage);

		stages.add(stage);
	}

	public void start(IStructure<?> input) {

		for (Stage<?, ?> stage : stages) {
			stage.start();
		}

	}

	private static class Stage<T, K> {
		private IStructure<T> input;
		private int stageSize;
		private Stage<?, ?> outputStage;
		private Filter<T, K> filter;

		public Stage(int n, Filter<T, K> filter) {
			input = CollectionFactory.createLockFreeQueue();
			stageSize = n;
			this.filter = filter;
		}

		@SuppressWarnings("hiding")
		public <K, R> void setOutputStage(Stage<K, R> stage) {
			this.outputStage = stage;
		}

		@SuppressWarnings("unchecked")
		public void sendToOutput(K output) {
			Stage<K, ?> os = (Stage<K, ?>) this.outputStage;
			if (os != null)
				os.input.add(output);
		}

		@SuppressWarnings("unchecked")
		public void start() {
			for (int i = 0; i < stageSize; i++) {
				try {
					new Thread((Filter<T, K>) filter.clone()).start();
				} catch (CloneNotSupportedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * 
	 * @author fs239085
	 * 
	 * @param <T>
	 *            input type
	 * @param <K>
	 *            output type
	 */
	public abstract static class Filter<T, K> implements Runnable, Cloneable {

		private Stage<T, K> stage;

		public void setStage(Stage<T, K> stage) {
			this.stage = stage;
		}

		protected void sendToOutput(K output) {
			this.stage.sendToOutput(output);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Object#clone()
		 */
		@Override
		public Object clone() throws CloneNotSupportedException {
			return super.clone();
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.lang.Runnable#run()
		 */
		public void run() {

			while (true) {
				this.execute(this.stage.input.remove());
			}

		}

		public abstract void execute(T element);
	}

}
