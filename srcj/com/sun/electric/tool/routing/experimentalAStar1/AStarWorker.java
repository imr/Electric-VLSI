/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AStarWorker.java
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

import java.util.List;
import java.util.concurrent.Callable;

/** 
 * Does the actual A* routing for the given net
 * 
 * @author Jonas Thedering
 * @author Christian Jülg
 */
public class AStarWorker implements Callable<Net> {

	private boolean DEBUG = false;

	private Storage storage;

	// only used by run(), use nodePool instead
	private List<ObjectPool<Node>> nodePools;
	private List<ObjectPool<Storage>> storagePools;
	private ObjectPool<Node> nodePool;
	private ObjectPool<Storage> storagePool;
	
	private Net net; // contains paths

	private final Map map;
	private Goal goal;

	private long shutdownTime;

	public AStarWorker(Net net, List<ObjectPool<Node>> nodePools, 
		List<ObjectPool<Storage>> storagePools, Map map, Goal goal, long shutdownTime) {
		this.nodePools = nodePools;
		this.storagePools = storagePools;
		this.net = net;
		this.map = map;
		this.goal = goal;
		this.shutdownTime = shutdownTime;
		
		DEBUG &= AStarRoutingFrame.getInstance().isOutputEnabled();
	}

	/** Connects a child for the given position to the parent, if passable */
	private void linkChild(int x, int y, int z, Node parent) {
		if (x < 0 || x >= map.getWidth() || y < 0 || y >= map.getHeight() || z < 0 || z >= map.getLayers())
			return;
		
		if (goal.isTileOK(parent.x, parent.y, parent.z, x, y, z)) {
			int g = parent.g + goal.getTileCost(parent.x, parent.y, parent.z, x, y, z);

			Node child = storage.get(x, y, z);
			if (child != null) {
				parent.children[parent.childCount++] = child;

				if (g < child.g) {
					child.parent = parent;
					child.g = g;
					if(storage.isNodeInOpen(child))
						storage.decreaseCost(child, g + child.h);
					else
						updateChildren(child);
				}
				return;
			}

			// No node existing for that position, so create new one
			child = nodePool.alloc();
			child.initialize(x, y, z);
			child.parent = parent;
			child.g = g;
			child.h = goal.distanceToGoal(x, y, z);
			child.f = (g + child.h);
			parent.children[parent.childCount++] = child;
			storage.addToOpen(child);
		}
	}

	/** Walks through the children of the given node to update their cost */
	private void updateChildren(Node parent) {
		for (int i = 0; i < parent.childCount; ++i) {
			Node child = parent.children[i];

			int g = parent.g
					+ goal.getTileCost(parent.x, parent.y, parent.z, child.x, child.y, child.z);
			if (g < child.g) {
				child.g = g;
				child.parent = parent;

				if(storage.isNodeInOpen(child))
					storage.decreaseCost(child, g + child.h);
				else
					updateChildren(child);
			}
		}
	}

	public Net call() throws Exception {
		
		//sync on nodepools, is unsynced list
		synchronized (nodePools) {
			storagePool = storagePools.remove(0);

			assert nodePools.size() > 0;
			this.nodePool = nodePools.remove(0);
		}
		storage = storagePool.alloc();
		
		int index;
		// Skip paths already routed
		for (index=0; index<net.pathDone.length; index++){
			if (!net.pathDone[index]){
				break;
			}
		}
		
		boolean shutdown = System.currentTimeMillis() > shutdownTime;
		
		// Iterates over the not yet routed paths of the net
		while(!goal.isRoutingComplete() && !shutdown) {
			storage.initialize(map.getWidth(), map.getHeight(), map.getLayers());

			Node startNode = nodePool.alloc();
			int[] startLoc = goal.getNextStart(); // returns an int[] {x,y,z}
			startNode.initialize(startLoc[0], startLoc[1], startLoc[2]);

			startNode.g = 0;
			startNode.h = goal.distanceToGoal(startNode.x, startNode.y, startNode.z);
			startNode.f = startNode.h;
			// If the start node is invalid, don't start the algorithm
			int status = map.getStatus(startNode.x, startNode.y, startNode.z);
			assert status != Map.CLEAR : "Start position not marked!";
			if (status == net.getNetID())
				storage.addToOpen(startNode);

			Node finishNode = null;
			int count = 0;
			long startTime=0;
			if(DEBUG)
				startTime = System.currentTimeMillis();

			// run A* until finish is found or all nodes inspected
			while (!storage.isOpenEmpty()) {
				++count;
				Node current = storage.shiftCheapestNode();
				
				int x = current.x;
				int y = current.y;
				int z = current.z;

				if (goal.isFinishPosition(x, y, z)) {
					finishNode = current;
					break;
				}
				
				linkChild(x, y, z - 1, current);
				linkChild(x, y, z + 1, current);
				
				int spaceAround = map.getSpaceAround(x, y, z);
				
				if(spaceAround == 0) {
					// We are in unknown territory, move conservatively
					linkChild(x - 1, y, z, current);
					linkChild(x + 1, y, z, current);
					linkChild(x, y - 1, z, current);
					linkChild(x, y + 1, z, current);
				}
				else {
					int spaceDiff = spaceAround-1;
					int offX = x % spaceAround;
					int offY = y % spaceAround;
					
					boolean xInside = offX > 0 && offX < spaceDiff;
					boolean yInside = offY > 0 && offY < spaceDiff;
					
					if(xInside && yInside) {
						// We are in the middle of clear space, move to the margins
						linkChild(x - offX, y, z, current);
						linkChild(x - offX + spaceDiff, y, z, current);
						linkChild(x, y - offY, z, current);
						linkChild(x, y - offY + spaceDiff, z, current);
					}
					else {
						// We are at a margin, move by 1 along the margin and to the outside,
						// jump to the margin on the other side 
						if(offX == spaceDiff && yInside)
							linkChild(x - spaceDiff, y, z, current);
						else
							linkChild(x - 1, y, z, current);
						
						if(offX == 0 && yInside)
							linkChild(x + spaceDiff, y, z, current);
						else
							linkChild(x + 1, y, z, current);
						
						if(offY == spaceDiff && xInside)
							linkChild(x, y - spaceDiff, z, current);
						else
							linkChild(x, y - 1, z, current);
						
						if(offY == 0 && xInside)
							linkChild(x, y + spaceDiff, z, current);
						else
							linkChild(x, y + 1, z, current);
					}
				}
			}

			// get Path to enter routing result
			Path path = net.getPaths().get(index);
			path.initialize(); // only segment set before initialize
			if(DEBUG) {
				long time = System.currentTimeMillis()-startTime;
				long speed = -2;
				if(time > 0){
					speed = count / time;
				} else{
					speed = (count>0)? 0 : -1;
				}
				System.out.printf("AStarWorker: %8d iterations in %4d ms (%4d it/ms) for path in \"%s\"\n", count, time, speed, path.segment.getNetName());
			}
			
			if (finishNode != null) {
				// Path was routed successfully, create the nodesX/Y/Z arrays by backtracking from the finish node
				
				int pathLength = 1;
				{
					Node current = finishNode;
					while (current != startNode) {
						pathLength += Math.abs(current.x - current.parent.x)
							+ Math.abs(current.y - current.parent.y)
							+ Math.abs(current.z - current.parent.z);
						current = current.parent;
					}
				}
				
				path.totalCost = finishNode.g;

				path.nodesX = new int[pathLength];
				path.nodesY = new int[pathLength];
				path.nodesZ = new int[pathLength];

				Node current = finishNode;
				int i = pathLength - 1;
				while (current != startNode) {
					if(current.x < current.parent.x)
						for(int x = current.x; x < current.parent.x; ++x) {
							path.nodesX[i] = x;
							path.nodesY[i] = current.y;
							path.nodesZ[i] = current.z;
							--i;
						}
					else if(current.x > current.parent.x)
						for(int x = current.x; x > current.parent.x; --x) {
							path.nodesX[i] = x;
							path.nodesY[i] = current.y;
							path.nodesZ[i] = current.z;
							--i;
						}
					else if(current.y < current.parent.y)
						for(int y = current.y; y < current.parent.y; ++y) {
							path.nodesX[i] = current.x;
							path.nodesY[i] = y;
							path.nodesZ[i] = current.z;
							--i;
						}
					else if(current.y > current.parent.y)
						for(int y = current.y; y > current.parent.y; --y) {
							path.nodesX[i] = current.x;
							path.nodesY[i] = y;
							path.nodesZ[i] = current.z;
							--i;
						}
					else if(current.z < current.parent.z)
						for(int z = current.z; z < current.parent.z; ++z) {
							path.nodesX[i] = current.x;
							path.nodesY[i] = current.y;
							path.nodesZ[i] = z;
							--i;
						}
					else if(current.z > current.parent.z)
						for(int z = current.z; z > current.parent.z; --z) {
							path.nodesX[i] = current.x;
							path.nodesY[i] = current.y;
							path.nodesZ[i] = z;
							--i;
						}
					else
						assert false;
					
					// nodesX/Y will be translated to cellGrid in runRouting
					// translating here confuses AStarMaster
					current = current.parent;
				}
				
				path.nodesX[0] = startNode.x;
				path.nodesY[0] = startNode.y;
				path.nodesZ[0] = startNode.z;
			} else {
				// path was unroutable, set path.totalCost to improve error messages in Master
				
				path.totalCost = count;
				int finishStatus = goal.getFinishPositionStatus();
				if (finishStatus != net.getNetID()){
					//finish is marked incorrectly
					path.totalCost = -2;
				}
			}
			
			// Return "borrowed" node objects
			storage.freeNodes(nodePool);
			
			// Skip paths already routed
			for (index=index+1; index<net.getPaths().size(); index++){
				if (!net.pathDone[index]){
					break;
				}
			}
			
			shutdown = System.currentTimeMillis() > shutdownTime;
		}
		
		if (DEBUG && shutdown) {
			System.out.printf("AStarWorker: shutdown now\n");
		}

		storagePool.free(storage);
		
		// sync on nodepools, is unsynced list
		synchronized (nodePools) {
			nodePools.add(nodePool);
			storagePools.add(storagePool);
		}

		return net;
	}
}
