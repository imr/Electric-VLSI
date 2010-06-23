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

import com.sun.electric.tool.util.CollectionFactory;
import com.sun.electric.tool.util.IStructure;

/**
 * @author Felix Schmidt
 * 
 */
public class PipelineRuntime {

	public static class Stage<Input, Output> {

		private IStructure<Input> inputQueue;
		private int numOfWorkers;

		public Stage(int numOfWorkers) {
			this.inputQueue = CollectionFactory.createLockFreeQueue();
			this.numOfWorkers = numOfWorkers;
		}

		public void send(Input item) {
			inputQueue.add(item);
		}

		public Input recv() {
			return this.inputQueue.remove();
		}
	}

	public static abstract class StageImpl<Input, Output> {
		public abstract Output execute(Input item);
	}
}
