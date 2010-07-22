/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Storage.java
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

/** 
 * Contains the open and closed lists for the A* algorithm and allows for
 * getting the node corresponding to a position, if any
 * 
 * @author Jonas Thedering
 * @author Christian Jülg
 */
public class Storage implements Poolable<Storage> {
	private boolean DEBUG = false;
	private PriorityQueue queue = new PriorityQueue();
	private Storage tail;
	private Node[] nodeMap = null;
	private int width = -1, height = -1, layers = -1;
	private Node nodeListHead = null;
	// Contains the node that was inserted first, needed to avoid list walking in cleanup
	private Node nodeListLast = null;
	
	// Bounding boxes for map cleaning
	private int[] minX, maxX, minY, maxY;
	
	// Only for debugging
	private int nodeCount = 0;
	
	public Storage() {
		DEBUG &= AStarRoutingFrame.getInstance().isOutputEnabled(); 
	}
	
	/** Ensures the storage is set to the initial state */
	public void initialize(int width, int height, int layers) {
		if(nodeMap == null || width != this.width || height != this.height || layers != this.layers) {
			this.width = width;
			this.height = height;
			this.layers = layers;
			nodeMap = new Node[width * height*layers]; // All entries will implicitly be null
			minX = new int[layers];
			maxX = new int[layers];
			minY = new int[layers];
			maxY = new int[layers];
		}
		else {
			long startTime = 0;
			if (DEBUG) {
				startTime= System.nanoTime(); 
			}
			
			for(int z=0; z<layers; ++z) {
				int temp = z * height;
				for(int y=minY[z]; y<=maxY[z]; ++y) {
					int temp2 = (temp + y) * width;
					for(int x=minX[z]; x<=maxX[z]; ++x)
						nodeMap[temp2 + x] = null;
				}
			}
			
			if(DEBUG) {
				float percentage = 0.0f;
				int sweepedCount = 0;
				for(int z=0; z<layers; ++z)
					if(maxX[z] >= minX[z] && maxY[z] >= minY[z])
						sweepedCount += (maxY[z]-minY[z]+1)*(maxX[z]-minX[z]+1);
				if(sweepedCount != 0)
					percentage = (nodeCount * 100.0f) / sweepedCount;
				System.out.printf("Storage: Sweeping nodeMap in %d ms, %d/%d necessary sweeps (%.1f %%)\n", (System.nanoTime() - startTime) / 1000000, nodeCount, sweepedCount, percentage);
			}
			nodeCount = 0;
		}
		for(int z=0; z < layers; ++z) {
			minX[z] = Integer.MAX_VALUE;
			maxX[z] = Integer.MIN_VALUE;
			minY[z] = Integer.MAX_VALUE;
			maxY[z] = Integer.MIN_VALUE;
		}
		
		queue.clear();
		nodeListHead = null;
		nodeListLast = null;
	}
	
	/** Adds the given node to the open list */
	public void addToOpen(Node node) {
		// Mark as open
		node.childCount = -1;
		
		queue.add(node);
		
		int nodeMapIndex = (node.z * height + node.y) * width + node.x;
		int z = node.z;
		minX[z] = Math.min(minX[z], node.x);
		maxX[z] = Math.max(maxX[z], node.x);
		minY[z] = Math.min(minY[z], node.y);
		maxY[z] = Math.max(maxY[z], node.y);
		nodeMap[nodeMapIndex] = node;
		
		++nodeCount;
		
		// Add to node list for later cleanup
		node.setTail(nodeListHead);
		if(nodeListHead == null)
			nodeListLast = node;
		nodeListHead = node;
	}
	
	/** Moves the cheapest node from open list to closed list and returns it */
	public Node shiftCheapestNode() {
		Node node = queue.remove();
		// Mark as closed
		node.childCount = 0;
		
		return node;
	}
	
	public boolean isOpenEmpty() {
		return queue.isEmpty();
	}
	
	/** @return if there's a node corresponding to the given position*/
	public boolean contains(int x, int y, int z) {
		return nodeMap[(z * height + y) * width + x] != null;
	}
	
	/** @return the node corresponding to the given position, if any */
	public Node get(int x, int y, int z) {
		return nodeMap[(z * height + y) * width + x];
	}
	
	public boolean isNodeInOpen(Node node) {
		return node.childCount == -1;
	}
	
	/** Updates the node's cost attribute and notifies the open list of it */
	public void decreaseCost(Node node, int newCost) {
		queue.decreaseKey(node, newCost);
	}
	
	/** Returns the nodes contained to the given pool */
	public void freeNodes(ObjectPool<Node> pool) {
		if(nodeListHead != null) {
			pool.freeAllLinked(nodeListHead, nodeListLast);
			nodeListHead = null;
			nodeListLast = null;
		}
	}

	public Storage getTail() {
		return tail;
	}

	public void setTail(Storage tail) {
		this.tail = tail;
	}
}
