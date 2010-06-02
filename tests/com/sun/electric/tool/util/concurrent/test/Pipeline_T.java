/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Pipeline_t.java
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
package com.sun.electric.tool.util.concurrent.test;

import org.junit.Test;

import com.sun.electric.tool.util.CollectionFactory;
import com.sun.electric.tool.util.IStructure;
import com.sun.electric.tool.util.concurrent.runtime.Pipeline;
import com.sun.electric.tool.util.concurrent.runtime.Pipeline.Filter;

/**
 * @author fs239085
 * 
 */
public class Pipeline_T {

	@Test
	public void testPipeline() {
		IStructure<Integer> testData = CollectionFactory.createLockFreeQueue();

		testData.add(1);
		testData.add(2);
		testData.add(3);

		Pipeline pipe = new Pipeline();
		pipe.addFilter(new Stage1(), 2);
		pipe.addFilter(new Stage2(), 2);
		
		pipe.start(testData);

	}

	public class Stage1 extends Filter<Integer, Integer> {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.sun.electric.tool.util.concurrent.runtime.Pipeline.Filter#execute
		 * (java.lang.Object)
		 */
		@Override
		public void execute(Integer element) {

			System.out.println("Stage 1: " + element);
			this.sendToOutput(element + 2);

		}

	}

	public class Stage2 extends Filter<Integer, Integer> {

		/*
		 * (non-Javadoc)
		 * 
		 * @see
		 * com.sun.electric.tool.util.concurrent.runtime.Pipeline.Filter#execute
		 * (java.lang.Object)
		 */
		@Override
		public void execute(Integer element) {

			System.out.println("Stage 2: " + element);

		}

	}

}
