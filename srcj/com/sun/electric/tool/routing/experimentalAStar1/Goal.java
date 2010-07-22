/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Goal.java
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

/**
 * Gives the A* movement information to AStarWorker to direct the routing
 * 
 * @author Jonas Thedering
 * @author Christian Jülg
 */
public class Goal {
	private boolean DEBUG = false;
	int[] startX, startY;
	int[] finishX, finishY;
	int currentStartZ = -1, currentFinishZ = -1;
	int index = -1;
	
	private Net net;
	private Map map;
	private int scalingFactor;
	
	// These values are for the case left/below, so right and above cases have to shift the values
	private static final int[] xValuesArea = new int[]{-1, 0, -1, 0};
	private static final int[] yValuesArea = new int[]{-1, -1, 0, 0};
	private static final int[] xValuesSurrounding = new int[]{-2, -1, 0, 1, -2, 1, -2, 1, -2, -1, 0, 1};
	private static final int[] yValuesSurrounding = new int[]{-2, -2, -2, -2, -1, -1, 0, 0, 1, 1, 1, 1};
	
	//TODO: optimize this value to reflect actual cost during manufacturing
	public static final int LAYER_TRAVERSL_COST = 20;

	public Goal(Net net, Map map) {
		List<Path> paths = net.getPaths();
		int n = paths.size();
		startX = new int[n];
		startY = new int[n];
		finishX = new int[n];
		finishY = new int[n];
		
		this.net = net;
		this.map = map;
		//currently scalingFactors are assumed to be integer-aligned
		this.scalingFactor = (int) map.getScalingFactor();

		for (int i = 0; i < n; ++i) {
			startX[i] = paths.get(i).startX;
			startY[i] = paths.get(i).startY;

			finishX[i] = paths.get(i).finishX;
			finishY[i] = paths.get(i).finishY;
		}

		DEBUG &= AStarRoutingFrame.getInstance().isOutputEnabled(); 
		
		if (DEBUG) {
			// System.out.printf("Goal: finishLoc is (%2.1f : %2.1f)\n",
			// finishLoc.getX(), finishLoc.getY());

			for (int i = 0; i < paths.size(); i++) {
				System.out.printf("Goal: finishX/Y/Z is (%d : %d), dispX:%d, dispY:%d\n", finishX[i], finishY[i], map.getDispX(), map.getDispY());
			}
		}
	}
	
	/** @return true if the given position is inside the area of the start or finish point of segment i */
	private boolean insideStartOrFinishArea(int x, int y, int z, int i) {
		Path path = net.getPaths().get(i);
		int startDiffX = path.startRight ? 1 : 0;
		int startDiffY = path.startAbove ? 1 : 0;
		int finishDiffX = path.finishRight ? 1 : 0;
		int finishDiffY = path.finishAbove ? 1 : 0;
		
		return (x >= startX[i]-1+startDiffX && x <= startX[i]+startDiffX && y >= startY[i]-1+startDiffY && y <= startY[i]+startDiffY)
			|| (x >= finishX[i]-1+finishDiffX && x <= finishX[i]+finishDiffX && y >= finishY[i]-1+finishDiffY && y <= finishY[i]+finishDiffY);
	}
	
	/** @return true if the given move is allowed */ 
	public boolean isTileOK(int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
		int netID = net.getNetID();
		int fromStatus = map.getStatus(fromX, fromY, fromZ);
		int toStatus = map.getStatus(toX, toY, toZ);
		
		if(fromStatus == Map.CLEAR && toStatus == Map.CLEAR)
			return true;
		else if((fromStatus == netID && (toStatus == Map.CLEAR || toStatus == netID))
			|| (toStatus == netID && (fromStatus == Map.CLEAR || fromStatus == netID))) { // ensure netIDs >0 for this to be reliable!
			// Are we in our own end area?
			if(insideStartOrFinishArea(toX, toY, toZ, index)
				|| insideStartOrFinishArea(fromX, fromY, fromZ, index))
				return toZ == fromZ;
			
			for(int i = 0; i < net.getPaths().size(); ++i) {
				// Are we inside the area of an end we don't possess?
				if(i == index)
					continue;
				if(insideStartOrFinishArea(toX, toY, toZ, i)
					|| insideStartOrFinishArea(fromX, fromY, fromZ, i))
					return false;
			}
			
			// We are outside of end areas
			return true;
		}
		else
			return false;
	}

	/** @return the cost for the given move */
	public int getTileCost(int fromX, int fromY, int fromZ, int toX, int toY, int toZ) {
		return scalingFactor * ( Math.abs(toX - fromX) + Math.abs(toY - fromY) )
			+ LAYER_TRAVERSL_COST * Math.abs(toZ - fromZ);
	}

	/** @return an estimate for the cost to reach the finish from the given position */
	public int distanceToGoal(int x, int y, int z) {
		return scalingFactor * (Math.abs(finishX[index] - x) + Math.abs(finishY[index] - y)) + LAYER_TRAVERSL_COST*Math.abs(currentFinishZ - z);
	}

	/** @return true if the given position is the finish position */
	public boolean isFinishPosition(int x, int y, int z) {
		return (x == finishX[index]) && (y == finishY[index]) && (z == currentFinishZ);
	}

	/**
	 * used by worker for debug, to find out if finish has been overwritten
	 */
	public int getFinishPositionStatus() {
		return map.getStatus(finishX[index], finishY[index], currentFinishZ);
	}

	/** Switches to the next segment to route and returns the corresponding start coordinates */
	public int[] getNextStart() {
		String debug = "";
		do {
			debug += index+" ";
			assert index < (startX.length - 1) : debug+"/"+startX.length;
			++index;
		} while (net.pathDone[index]);
		
		Path path = net.getPaths().get(index);
		
		int maxStartOpenness = -1;
		currentStartZ = -1;
		for(int i=0; i < path.startZ.length; ++i) {
			int openness = getSurroundingOpenness(path.startX, path.startY, path.startZ[i], path.startRight, path.startAbove);
			if(openness > maxStartOpenness) {
				currentStartZ = path.startZ[i];
				maxStartOpenness = openness;
			}
		}
		
		int maxFinishOpenness = -1;
		currentFinishZ = -1;
		for(int i=0; i < path.finishZ.length; ++i) {
			int openness = getSurroundingOpenness(path.finishX, path.finishY, path.finishZ[i], path.finishRight, path.finishAbove);
			if(path.finishZ[i] == currentStartZ
				&& Math.abs(path.finishX - path.startX) <= 1
				&& Math.abs(path.finishY - path.startY) <= 1) {
				// Start and finish are too close together, only use this layer if we must
				openness = 0;
			}
			
			if(openness > maxFinishOpenness) {
				currentFinishZ = path.finishZ[i];
				maxFinishOpenness = openness;
			}
		}
		
		return new int[] { startX[index], startY[index], currentStartZ};
		
	}

	/** @return true if all segments have been routed */
	public boolean isRoutingComplete() {
		for (int i = index+1; i < net.pathDone.length; i++) {
			if (!net.pathDone[i])
				return false;
		}
		return true;
	}
	
	/** Counts the free points around the given endpoint and checks for dangerous overlaps */
	private int getSurroundingOpenness(int x, int y, int z, boolean right, boolean above) {
		int diffX = right ? 1 : 0;
		int diffY = above ? 1 : 0;
		
		for(int i=0; i < 4; ++i) {
			int testX = x + xValuesArea[i] + diffX;
			int testY = y + yValuesArea[i] + diffY;
			
			for(int j = 0; j < net.getPaths().size(); ++j) {
				// Are we inside the area of an end we don't possess?
				if(j == index)
					continue;
				if(insideStartOrFinishArea(testX, testY, z, j)) {
					Path otherPath = net.getPaths().get(j);
					if(!(otherPath.startX == startX[index] && otherPath.startY == startY[index])
						&& !(otherPath.finishX == startX[index] && otherPath.finishY == startY[index])
						&& !(otherPath.startX == finishX[index] && otherPath.startY == finishY[index])
						&& !(otherPath.finishX == finishX[index] && otherPath.finishY == finishY[index])) {
						// We would overlap with another endpoint's area, avoid this!
						return 0;
					}
				}
			}
		}
		
		int sum = 0;
		for(int i=0; i < 12; ++i) {
			int status = map.getStatus(x + xValuesSurrounding[i] + diffX, y + yValuesSurrounding[i] + diffY, z);
			if(status == Map.CLEAR || status == net.getNetID())
				++sum;
		}
		
		return sum;
	}
}
