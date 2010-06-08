/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CollectionTests.java
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

import org.junit.Assert;
import org.junit.Test;

import com.sun.electric.tool.util.IDEStructure;
import com.sun.electric.tool.util.IStructure;
import com.sun.electric.tool.util.concurrent.datastructures.BDEQueue;
import com.sun.electric.tool.util.concurrent.datastructures.CircularArray;
import com.sun.electric.tool.util.concurrent.datastructures.LockFreeQueue;
import com.sun.electric.tool.util.concurrent.datastructures.LockFreeStack;
import com.sun.electric.tool.util.concurrent.datastructures.UnboundedDEQueue;
import com.sun.electric.tool.util.concurrent.datastructures.WorkStealingStructure;

/**
 * @author fs239085
 * 
 */
public class CollectionTests_T {

	@Test
	public void testLockFreeQueue() {
		testIStructure(new LockFreeQueue<Integer>());
	}

	@Test
	public void testLockFreeStack() {
		testIStructure(new LockFreeStack<Integer>());
	}

	@Test
	public void testBDEQueue() {
		testIStructure(new BDEQueue<Integer>(10));
	}

	@Test
	public void testCircularArray() {
		testIStructureCircular(new CircularArray<Integer>(Integer.class, 4));
	}

	@Test
	public void testUnboundedDEQueue() {
		testIDEStructure(new UnboundedDEQueue<Integer>(Integer.class, 4));
	}

	@Test
	public void testLockFreeQueueMore() {
		int[] testData = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
		LockFreeQueue<Integer> intQueue = new LockFreeQueue<Integer>();
		for (int i = 0; i < testData.length; i++)
			intQueue.add(testData[i]);

		// first in - first out
		for (int i = 0; i < testData.length; i++)
			Assert.assertEquals(new Integer(testData[i]), intQueue.remove());
	}

	@Test
	public void testCircularArrayMore() {
		int[] testData = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
		CircularArray<Integer> intQueue = new CircularArray<Integer>(Integer.class, 4);
		for (int i = 0; i < testData.length; i++)
			intQueue.add(testData[i], i);

		for (int i = 0; i < testData.length; i++)
			Assert.assertEquals(new Integer(testData[i]), intQueue.get(i));
	}

	@Test
	public void testBDEQueueMore() {
		int[] testData = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
		BDEQueue<Integer> intQueue = new BDEQueue<Integer>(10);
		for (int i = 0; i < testData.length; i++)
			Assert.assertTrue(intQueue.tryAdd(testData[i]));

		// test both direction
		for (int i = 0; i < (testData.length / 2); i++) {
			Assert.assertEquals(new Integer(testData[i]), intQueue.getFromTop());
			Assert.assertEquals(new Integer(testData[testData.length - i - 1]), intQueue.remove());
		}
	}

	@Test
	public void testBDEQueueTooMuch() {
		int[] testData = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11 };
		BDEQueue<Integer> intQueue = new BDEQueue<Integer>(10);
		for (int i = 0; i < testData.length - 1; i++)
			Assert.assertTrue(intQueue.tryAdd(testData[i]));

		// structure is full
		Assert.assertFalse(intQueue.tryAdd(testData[testData.length - 1]));

		intQueue.remove();

		// one place is free - fill it
		Assert.assertTrue(intQueue.tryAdd(testData[testData.length - 1]));
	}

	@Test
	public void testLockFreeStackMore() {
		int[] testData = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
		LockFreeStack<Integer> intStack = new LockFreeStack<Integer>();
		for (int i = 0; i < testData.length; i++)
			intStack.add(testData[i]);

		// last in - first out
		for (int i = testData.length - 1; i >= 0; i--)
			Assert.assertEquals(new Integer(testData[i]), intStack.remove());
	}

	@Test
	public void testUnboundedDEQueueMore() {
		int[] testData = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
		UnboundedDEQueue<Integer> intStack = new UnboundedDEQueue<Integer>(Integer.class, 4);
		for (int i = 0; i < testData.length; i++)
			intStack.add(testData[i]);

		// last in - first out
		for (int i = testData.length - 1; i >= 0; i--)
			Assert.assertEquals(new Integer(testData[i]), intStack.remove());

		UnboundedDEQueue<Integer> intQueue = new UnboundedDEQueue<Integer>(Integer.class, 4);
		for (int i = 0; i < testData.length; i++)
			intQueue.add(testData[i], i);

		// first in - first out
		for (int i = 0; i < testData.length; i++)
			Assert.assertEquals(new Integer(testData[i]), intQueue.getFromTop());

		BDEQueue<Integer> dequeue = new BDEQueue<Integer>(10);
		for (int i = 0; i < testData.length; i++)
			dequeue.add(testData[i]);

		// test both direction
		for (int i = 0; i < (testData.length / 2); i++) {
			Assert.assertEquals(new Integer(testData[i]), dequeue.getFromTop());
			Assert.assertEquals(new Integer(testData[testData.length - i - 1]), dequeue.remove());
		}

	}

	@Test
	public void testUnboundedQueueWithResize() {
		int[] testData = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
		UnboundedDEQueue<Integer> intStack = new UnboundedDEQueue<Integer>(Integer.class, 4);
		for (int j = 0; j < 10; j++) {
			for (int i = 0; i < testData.length; i++)
				intStack.add(testData[i]);
		}
	}

	@Test
	public void testWorkStealingStructure() {
		WorkStealingStructure<Integer> wsSt = new WorkStealingStructure<Integer>(1, Integer.class);
		wsSt.registerThread();

		testIStructure(wsSt);
	}

	@Test
	public void testWorkStealingStructureMore() {
		int[] testData = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };
		WorkStealingStructure<Integer> wsSt = new WorkStealingStructure<Integer>(1, Integer.class);
		wsSt.registerThread();
		for (int i = 0; i < testData.length; i++)
			wsSt.add(testData[i]);

		// last in - first out
		for (int i = testData.length - 1; i >= 0; i--)
			Assert.assertEquals(new Integer(testData[i]), wsSt.remove());
	}

	@Test
	public void testIsEmptyAllStructures() {
		testIsEmpty(new LockFreeQueue<Integer>());
		testIsEmpty(new LockFreeStack<Integer>());
		testIsEmpty(new BDEQueue<Integer>(1));

//		WorkStealingStructure<Integer> wsSt = new WorkStealingStructure<Integer>(1, Integer.class);
//		wsSt.registerThread();
//		testIsEmpty(wsSt);
	}

	private void testIStructure(IStructure<Integer> structure) {
		structure.add(10);
		Assert.assertEquals(new Integer(10), structure.remove());
		Assert.assertNull(structure.remove());
	}

	private void testIDEStructure(IDEStructure<Integer> structure) {
		structure.add(10);
		Assert.assertEquals(new Integer(10), structure.remove());
		Assert.assertNull(structure.remove());
	}

	private void testIStructureCircular(IStructure<Integer> structure) {
		structure.add(10, 0);
		Assert.assertEquals(new Integer(10), structure.get(0));
	}

	private void testIsEmpty(IStructure<Integer> structure) {
		Assert.assertTrue(structure.isEmpty());
		structure.add(10);
		Assert.assertFalse(structure.isEmpty());
		structure.remove();
		Assert.assertTrue(structure.isEmpty());
	}
}
