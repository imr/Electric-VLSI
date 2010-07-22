/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EndPointMarker.java
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.sun.electric.tool.routing.RoutingFrame.RoutingLayer;

/** 
 * Determines the possible start/finish layers for the paths in the nets 
 * and adds corresponding markings to the map
 * 
 * @author Christian Jülg
 * @author Jonas Thedering
 */
public class EndPointMarker {

	private boolean DEBUG = false;
	
	private static final int CLEAR = 0;
	private static final int CORE_BLOCKED = -1;
	private static final int CORE_FREE_SURROUNDING_BLOCKED = -2;
	
	private static Random random = new Random(8682522807148012L);
	
	Map map;

	public EndPointMarker(Map map) {
		this.map = map;
		
		DEBUG &= AStarRoutingFrame.getInstance().isOutputEnabled(); 
	}

	/**
	 * marks start and finish locations for each segment, 
	 * to prevent other Nets that are routed earlier to run over these endpoints
	 */
	public void markStartAndFinish(List<Net> netList) {
		long start = 0;
		if (DEBUG) {
			start = System.currentTimeMillis();
		}
		
		ArrayList<EndPoint> endPoints = new ArrayList<EndPoint>();

		for (Net net : netList) {
			net.initialize(); // needed before Goal()
			
			List<Path> paths = net.getPaths();
			for (int pathIndex=0; pathIndex<paths.size(); pathIndex++) {
				Path path = paths.get(pathIndex);

				EndPoint startPoint = new EndPoint(path, net, EndPoint.START);
				List<RoutingLayer> layers = path.segment.getStartLayers();
				startPoint.possibleLayers = new int[layers.size()];
				startPoint.fullyMarkedLayers = new int[layers.size()];
				
//				assert startPoint.possibleLayers.length <= map.getLayers() : "Error: There are not enough valid layers configured, or this segment has too many startLayers set";
				
				for (int layerIndex=0; layerIndex<layers.size(); layerIndex++) {
					RoutingLayer rl = layers.get(layerIndex);
					int z = rl.getMetalNumber() - 1;
					startPoint.possibleLayers[layerIndex] = z;
				}
				endPoints.add(startPoint);
				
				EndPoint finishPoint = new EndPoint(path, net, EndPoint.FINISH);
				layers = path.segment.getFinishLayers();
				finishPoint.possibleLayers = new int[layers.size()];
				finishPoint.fullyMarkedLayers = new int[layers.size()];

				for (int layerIndex=0; layerIndex<layers.size(); layerIndex++) {
					RoutingLayer rl = layers.get(layerIndex);
					int z = rl.getMetalNumber() - 1;
					finishPoint.possibleLayers[layerIndex] = z;
				}
				endPoints.add(finishPoint);
				
				findPreferredLayer(startPoint, finishPoint);
			}
		}

		// try to mark all layers for each endPoint, with surrounding
		boolean repeat = true;
		while(repeat) {
			repeat = false;
			for (EndPoint endPoint : endPoints) {
	
				int netID = endPoint.net.getNetID();

				// mark one layer for this endpoint so endpoints take turns in choosing layers
				while(endPoint.markingIndex < endPoint.possibleLayers.length) {
					int z = endPoint.possibleLayers[endPoint.markingIndex];
					
					++endPoint.markingIndex;
	
					int status = isEndpointFree(endPoint, z, netID, true);
					if (status == CLEAR) {
	
						setEndpoint(endPoint, z, netID);
	
						endPoint.fullyMarkedLayers[endPoint.fullyMarkedCount] = z;
						endPoint.fullyMarkedCount++;
						break;
					}
					// ignore else
				}
				
				if(endPoint.markingIndex < endPoint.possibleLayers.length)
					repeat = true;
			}
		}

		for (EndPoint endPoint : endPoints) {
			
			int netID = endPoint.net.getNetID();

			// mark one layer for this endpoint so endpoints take turns in choosing layers
			for(int i=0; i < endPoint.possibleLayers.length; i++) {
				int z = endPoint.possibleLayers[i];

				int status = map.getStatus(endPoint.getX(), endPoint.getY(), z);
				
				if (status == Map.CLEAR) {
					assert isEndpointFree(endPoint, z, netID, true) == CORE_FREE_SURROUNDING_BLOCKED : isEndpointFree(endPoint, z, netID, true);
					map.setStatus(endPoint.getX(), endPoint.getY(), z, netID);
				}
			}
		}
		
		for(EndPoint endPoint : endPoints) {
			// We must not have an empty layer list
			if(endPoint.fullyMarkedCount == 0) {
				endPoint.path.pathUnroutable = true;
				++endPoint.fullyMarkedCount;
				endPoint.fullyMarkedLayers[0] = endPoint.possibleLayers[0];
			}
			endPoint.setZ();
		}

		if (DEBUG) {
			System.out.printf("Master: markStartAndFinish() took %d ms for %d nets\n", System.currentTimeMillis() - start, netList.size());
		}

	}
	
	/** Determines how many blocking markings are at the 4 mark points for the given endPoint and layer 
	 * 
	 * @returns CLEAR, CORE_BLOCKED or CORE_FREE_SURROUNDING_BLOCKED
	 */
	private int isEndpointFree(EndPoint endPoint, int z, int netID, boolean checkSurrounding) {
		int x = endPoint.getX();
		int y = endPoint.getY();
		
		int xDiff = endPoint.isRight() ? 1 : -1;
		int yDiff = endPoint.isAbove() ? 1 : -1;

		int[] xArgs = new int[4];
		int[] yArgs = new int[4];
		
		xArgs[0] = x;
		yArgs[0] = y;
		xArgs[1] = x + xDiff;
		yArgs[1] = y;
		xArgs[2] = x;
		yArgs[2] = y + yDiff;
		xArgs[3] = x + xDiff;
		yArgs[3] = y + yDiff;
		
		int status = map.getStatus(xArgs[0], yArgs[0], z);

//		// TODO: check for != Map.X would mean Endpoints can be in same grid point as blockages,
//		//		 could lead to DRC errors!
//		if (status != Map.CLEAR && status != netID && status != Map.X) {
//			return CORE_BLOCKED;
//		}
		if (status != Map.CLEAR && status != netID) {
			return CORE_BLOCKED;
		}
		
		if (checkSurrounding) {
			for (int i = 1; i < 4; i++) {
				status = map.getStatus(xArgs[i], yArgs[i], z);

//				// TODO: check for != Map.X would mean Endpoints can be in same grid point as blockages,
//				//		 could lead to DRC errors!
//				if (status != Map.CLEAR && status != netID && status != Map.X) {
//					return CORE_FREE_SURROUNDING_BLOCKED;
//				}				
				if (status != Map.CLEAR && status != netID) {
					return CORE_FREE_SURROUNDING_BLOCKED;
				}
			}
		}
		return CLEAR;
	}
	
	/** Adds the 4 markings for the given endPoint and layer */
	private void setEndpoint(EndPoint endPoint, int z, int netID) {
		int x = endPoint.getX();
		int y = endPoint.getY();
		
		int xDiff = endPoint.isRight() ? 1 : -1;
		int yDiff = endPoint.isAbove() ? 1 : -1;

		int[] xArgs = new int[4];
		int[] yArgs = new int[4];
		
		xArgs[0] = x;
		yArgs[0] = y;
		xArgs[1] = x + xDiff;
		yArgs[1] = y;
		xArgs[2] = x;
		yArgs[2] = y + yDiff;
		xArgs[3] = x + xDiff;
		yArgs[3] = y + yDiff;
		
		map.setStatus(xArgs[0], yArgs[0], z, netID);
		map.setStatus(xArgs[1], yArgs[1], z, netID);
		map.setStatus(xArgs[2], yArgs[2], z, netID);
		map.setStatus(xArgs[3], yArgs[3], z, netID);
		
		if(DEBUG)
			for(int i=0; i<4; ++i)
				System.out.printf("EndPointMarker: Marking (%.2f, %.2f)(%d,%d) as #%d for net %d\n", xArgs[i]*map.getScalingFactor()-map.getDispX()+map.getScalingFactor()/2f, yArgs[i]*map.getScalingFactor()-map.getDispY()+map.getScalingFactor()/2f, xArgs[i], yArgs[i], i, endPoint.net.getElectricNetID());
	}

	/**
	 * Selects one layer for the endpoints of the given segment that should be used preferably
	 * 
	 * If both endpoints have common layers, one of these is chosen.
	 */
	public void findPreferredLayer(EndPoint startPoint, EndPoint finishPoint) {
		// there can be multiple start/finish layers

		int[] startLayerNums = startPoint.possibleLayers;
		int[] finishLayerNums = finishPoint.possibleLayers;

		int minS = startLayerNums[0];
		int maxS = startLayerNums[startLayerNums.length - 1];
		int minF = finishLayerNums[0];
		int maxF = finishLayerNums[finishLayerNums.length - 1];

		Arrays.sort(startLayerNums);
		Arrays.sort(finishLayerNums);

		int[] common = new int[startLayerNums.length];
		int commonCount = 0;

		int iFinish = 0;
		for (int iStart = 0; iStart < startLayerNums.length; iStart++) {
			if (iFinish == finishLayerNums.length) {
				break;
			} else if (startLayerNums[iStart] == finishLayerNums[iFinish]) {
				common[commonCount++] = startLayerNums[iStart];
			}
			iFinish++;
		}

		if (DEBUG) {
			System.out.printf("EndPointMarker.initStartFinishZ: minS:%d, maxS:%d, minF:%d, maxF:%d\n", minS, maxS, minF, maxF);
			System.out.printf("there are %d common layers: %s\n", commonCount, Arrays.toString(common));
		}

		// important: only the first commonCount elements of common[] are valid!
		if (commonCount > 0) {
			int idx = random.nextInt(commonCount);
	
			startPoint.preferLayer(common[idx]);
			finishPoint.preferLayer(common[idx]);
		}
	}
}
