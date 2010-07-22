/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PriorityQueue.java
 * Written by: Christian Julg, Jonas Thedering (Team 1)
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
package com.sun.electric.tool.routing.experimentalAStar1;

import java.util.Random;

/** 
 * This class implements a priority queue by using a splay tree and forming a
 * doubly linked list for entries with the same key
 * 
 * this list is ordered (numbers represent add order:
 * (0, n-1, n-2, ..., 1)
 * remove order will happen reverse to addition, 
 * i.e. the double linked list works as a LIFO / stack data structure 
 * 
 * Node.children[] is reused:
 * 0: prev, 1: next
 * 
 * @author Jonas Thedering
 * @author Christian Jülg
 */
public class PriorityQueue {
	SplayTree tree = new SplayTree();
	
	public void clear() {
		tree.clear();
	}
	
	public void add(Node node) {
		Node equalNode = tree.find(node);
		if(equalNode == null) {
			node.children[0] = null;
			node.children[1] = null;
			tree.insert(node);
		}
		else {
			node.children[1] = equalNode.children[1];
			if(equalNode.children[1] != null)
				equalNode.children[1].children[0] = node;
			equalNode.children[1] = node;
			node.children[0] = equalNode;
		}
	}
	
	public Node remove() {
		Node firstNode = tree.findMin();
		if(firstNode == null)
			return null;
		else if(firstNode.children[1] == null) {
			tree.remove(firstNode);
			return firstNode;
		}
		else {
			Node result = firstNode.children[1];
			firstNode.children[1] = result.children[1];
			if(result.children[1] != null)
				result.children[1].children[0] = firstNode;
			
			return result;
		}
	}
	
	public boolean isEmpty() {
		return tree.isEmpty();
	}
	
	/** Updates the f value for the given node and updates its position in the queue */
	public void decreaseKey(Node node, int newKey) {
		// Unfortunately, there is no decreaseKey method in the splay tree
		if(node.children[0] != null) {
			node.children[0].children[1] = node.children[1];
			if(node.children[1] != null)
				node.children[1].children[0] = node.children[0];
		}
		else {
			tree.remove(node);
			
			Node next = node.children[1];
			if(next != null) {
				next.children[0] = null;
				tree.insert(next);
			}
		}
		node.f = newKey;
		add(node);
	}

	
	// "Unit test"
	public static void main(String[] args) throws Exception {
		PriorityQueue queue = new PriorityQueue();
		long seed = System.currentTimeMillis();
		//seed= 1275391773870l;
		System.out.printf("Seed: %d\n", seed);
		Random r = new Random(seed);
		int[] testValues = new int[50000];
		Node[] testNodes = new Node[testValues.length];
		for(int i = 0; i < testValues.length; ++i)
			testValues[i] = r.nextInt(1000000);
		
		for(int i = 0; i < testValues.length; ++i) {
			Node node = new Node();
			node.initialize(1, 2, 3);
			node.childCount = -1;
			node.children[0] = node;
			node.children[1] = node;
			node.f = testValues[i];
			queue.add(node);
			
			testNodes[i] = node;
		}
		
		for(int i = 100; i < testValues.length; i += 19) {
			queue.decreaseKey(testNodes[i], testNodes[i].f - 1);
			
			queue.decreaseKey(testNodes[i-1], testNodes[i-1].f - 12345);
		}
		
		int previousValue = Integer.MIN_VALUE;
		int count = 0;
		while(!queue.isEmpty()) {
			++count;
			Node node = queue.remove();
			//System.out.printf("%d, ", node.f);
			if(node.f < previousValue) throw new Exception("Wrong order");
			previousValue = node.f;
		}
		
		if(count != testValues.length)
			throw new Exception("Wrong value count: " + count);
		
		System.out.println("ok");
	}	
}
