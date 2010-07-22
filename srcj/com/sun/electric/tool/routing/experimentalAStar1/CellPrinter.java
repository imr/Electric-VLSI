/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CellPrinter.java
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

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.routing.RoutingFrame.RoutingContact;
import com.sun.electric.tool.routing.RoutingFrame.RoutingGeometry;
import com.sun.electric.tool.routing.RoutingFrame.RoutingLayer;
import com.sun.electric.tool.routing.RoutingFrame.RoutingSegment;

/* Prints debug information about the cell to be routed */
public class CellPrinter {

	public static void printLayers(List<RoutingLayer> allLayers) {
		for (RoutingLayer layer : allLayers) {
			System.out.printf("Layer: %s, metal:%b\n", layer.getName(), layer
					.isMetal());
			RoutingContact pin = layer.getPin();
			if (pin != null) {
			System.out.printf("Pin: %s, firstLayer:%s, secondLayer:%s\n", pin
					.getName(), pin.getFirstLayer().getName(), pin
					.getSecondLayer().getName());
			}
		}
	}

	public static void printContacts(List<RoutingContact> allContacts) {
		for (RoutingContact contact : allContacts) {

			double viaSpacing = contact.getViaSpacing();

			System.out.printf("Contact: %s, firstLayer:%s, secondLayer:%s, viaSpacing=%f\n",
					contact.getName(), contact.getFirstLayer().getName(),
					contact.getSecondLayer().getName(), viaSpacing);
		}
	}

	public static void printChipStatistics(Cell cell, List<RoutingSegment> segmentsToRoute, List<RoutingLayer> allLayers,
			List<RoutingContact> allContacts, List<RoutingGeometry> blockages) {
		
		int metalLayers = 0;
		
		for(RoutingLayer rl : allLayers) {
			if(rl.isMetal()) {
				metalLayers++;
			}
		}
		
		int[] layerDistance = new int[metalLayers];
		int[] layerMembership = new int[metalLayers];
		int[][] crossLayerMembership = new int[metalLayers][metalLayers]; 
		int singleLayer = 0;
		int crossLayer = 0;
		
		int maxNetID = Integer.MIN_VALUE;
		
		for(RoutingSegment rs : segmentsToRoute) {
			maxNetID = Math.max(maxNetID, rs.getNetID());
			
			int minStart = Integer.MAX_VALUE;
			int maxStart = Integer.MIN_VALUE;
			for(RoutingLayer rl : rs.getStartLayers()) {
				minStart = Math.min(minStart, rl.getMetalNumber());
				maxStart = Math.max(maxStart, rl.getMetalNumber());
			}
			int minFinish = Integer.MAX_VALUE;
			int maxFinish = Integer.MIN_VALUE;
			for(RoutingLayer rl : rs.getFinishLayers()) {
				minFinish = Math.min(minFinish, rl.getMetalNumber());
				maxFinish = Math.max(maxFinish, rl.getMetalNumber());
			}
			
			if(minStart==maxStart && minFinish==maxFinish) {
				int distance = Math.abs(minStart-minFinish);
				layerDistance[distance]++;
				if(minStart == minFinish) {
					singleLayer++;
					layerMembership[minStart-1]++;
				}
				else {
					// XXX: appears not to work as expected
					int min = Math.min(minStart, minFinish);
					int max = Math.max(minStart, minFinish);
					crossLayerMembership[min-1][max-1]++;
					crossLayer++;
				}
			}
		}
		
		int[] nets = new int[maxNetID+1];
		
		for(RoutingSegment rs : segmentsToRoute) {
			nets[rs.getNetID()]++;
		}
		
		int numberOfNets = 0;
		int maxSegments = Integer.MIN_VALUE;
		
		for(int i=0; i<nets.length; i++) {
			if(nets[i] != 0) {
				numberOfNets++;
			}
			maxSegments = Math.max(maxSegments, nets[i]);
		}
		
		int[] numberOfSegments = new int[maxSegments];
		for(int i=0; i<nets.length; i++) {
			if(nets[i] != 0) {
				numberOfSegments[nets[i]-1]++;
			}
		}
		
		int[] blockagesOnLayer = new int[metalLayers];
		
		for(RoutingGeometry rg : blockages) {
			blockagesOnLayer[rg.getLayer().getMetalNumber()-1]++;
		}
		
		System.out.println();
		System.out.println();
		System.out.print("#Metal-Layers: " + metalLayers + " { ");
		for(RoutingLayer rl : allLayers) {
			if(rl.isMetal()) {
				System.out.print(rl.getMetalNumber() + ", ");
			}
		}
		System.out.println("}\n");
		
		System.out.println("#Segments: " + segmentsToRoute.size());
		System.out.println("\t on one layer:  " + singleLayer);
		for(int i=0; i<layerMembership.length; i++) {
			if(layerMembership[i] != 0) {
				System.out.println("\t\t Metal-" + (i+1) + ": " + layerMembership[i]);
			}
		}
		System.out.println("\t on two layers: " + crossLayer);
		for(int i=0; i<crossLayerMembership.length; i++) {
			for(int j=0; j<crossLayerMembership[0].length; j++) {
				if(crossLayerMembership[i][j] != 0) {
					System.out.println("\t\t Metal-" +(i+1)+ " - Metal-" + (j+1) + ": " + crossLayerMembership[i][j]);
				}
			}
		}
		System.out.println();
		
		System.out.println("layer distance distribution");
		for(int i=0; i<layerDistance.length; i++) {
			if(layerDistance[i] != 0) {
				System.out.println("\t distance " + i + ": " + layerDistance[i]);
			}
		}
		System.out.println();

		System.out.println("#Blockages: " + blockages.size());
		for(int i=0; i<blockagesOnLayer.length; i++) {
			if(blockagesOnLayer[i] != 0) {
				System.out.println("\t on Metal-" + (i+1) + ": " + blockagesOnLayer[i]);
			}
		}
		System.out.println();
		
		int countNumSegments = 0;
		
		System.out.println("#Nets: " + numberOfNets);
		for(int i=0; i<numberOfSegments.length; i++) {
			if(numberOfSegments[i] != 0) {
				countNumSegments += numberOfSegments[i] * (i+1); 
				System.out.print("\t " +(i+1) + " segments: " + numberOfSegments[i] + "{ ");
				for(int j=0; j<nets.length; j++) {
					if(nets[j] == (i+1)) {
						System.out.print(j + ", ");
					}
				}
				System.out.println("}");
			}
		}
		System.out.println("\tsum: " + countNumSegments + " segments");
		

	}
}
