/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: SA_Test.java
 * Written by Team 2: Jan Barth, Iskandar Abudiab
 * 
 * This code has been developed at the Karlsruhe Institute of Technology (KIT), Germany, 
 * as part of the course "Multicore Programming in Practice: Tools, Models, and Languages".
 * Contact instructor: Dr. Victor Pankratius (pankratius@ipd.uka.de)
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
package com.sun.electric.tool.placement.simulatedAnnealing1;

import com.sun.electric.tool.placement.PlacementFrame;
import com.sun.electric.tool.placement.simulatedAnnealing1.metrics.AOMetric;
import com.sun.electric.tool.placement.simulatedAnnealing1.metrics.BBMetric;

import java.util.List;
import java.util.Random;

/** Parallel Placement
 **/
public class SA_Test extends PlacementFrame {

	private double INITIAL_TEMP = 10000000;
//	private int ITERATIONS = 100000;
	private int INNER_ITERATIONS = 200;
	private Random rand = new Random();			
	
	@Override	
	public String getAlgorithmName() {		
		return "SA_Test";
	}

	/**
	 * @see PlacementFrame#runPlacement(List, List, String);
	 */
	@Override
	protected void runPlacement(List<PlacementNode> nodesToPlace,
			List<PlacementNetwork> allNetworks, String cellName) {

		//variable declarations
		double temp = INITIAL_TEMP;
		double ratio = 0.99; //temp decline ratio
		double maxX = 0, maxY = 0;		
		int accepts = 1;
		int declines = 1;
//		int acceptRate = 1; //integer is intentional
//		int stuckRate = 0;
//		int minSteps= 100;
		BBMetric bbMetric = new BBMetric( allNetworks );
				
		PlacementNode[] nodes = new PlacementNode[nodesToPlace.size()];
		
		for( int i = 0; i < nodesToPlace.size(); i++ ){
			nodes[i] = nodesToPlace.get( i );
		}

		AOMetric aoMetric = new AOMetric(nodes );
		
		//random grid placement
		for (int i = 0; i <  nodesToPlace.size(); i++ ) {
			PlacementNode n = nodes[i];
			//TODO Change 200 to a variable number depending on the size of the nodes
			n.setPlacement(i * 1.0 % Math.sqrt(nodes.length) * 200, Math.round(i * 1.0 / Math.sqrt(nodes.length))* 200);
		}		

		double w = 0;
		double h = 0;
		
		for(int i = 0; i < nodesToPlace.size(); i++){
			PlacementNode n = nodes[i];
			if( n.getWidth() > w ) w = n.getWidth();
			if( n.getHeight() > h ) h = n.getHeight();
		}
		
//		int numRows = (int)Math.round( Math.sqrt( nodesToPlace.size() ) );
//		double xPos = 0, yPos = 0;
//		for(int i = 0; i < nodesToPlace.size(); i++){
//			PlacementNode plNode = nodes[i];
//			plNode.setPlacement( xPos, yPos );
//			xPos += w;
//			if ( ( i%numRows ) == numRows-1 )
//			{
//				xPos = 0;
//				yPos += h;
//			}
//		}
		
		//sandbox bounds	
		for(PlacementNode node : nodes){
			if( node.getPlacementX() > maxX ) maxX = node.getPlacementX();
			if( node.getPlacementY() > maxY ) maxY = node.getPlacementY();
		}				

		//Step 1: GridPlacement
		
		double beforeScore = bbMetric.getScore();
		
		System.out.println( "START: " + bbMetric.getScore() );		
//		while ( (accepts+declines < minSteps || acceptRate > 0)){
		while ( temp > 0.01 ){
			for( int i = 0; i < INNER_ITERATIONS; i++ ){				

				int index = (int) Math.round( rand.nextDouble()*( nodes.length - 1 ) );				
				int index2 = (int) Math.round( rand.nextDouble()*( nodes.length - 1 ) );

				PlacementNode node1 = nodes[index];
				PlacementNode node2 = nodes[index2];

				double x1 = node1.getPlacementX();
				double y1 = node1.getPlacementY();

				double x2 = node2.getPlacementX();
				double y2 = node2.getPlacementY();


				//				double newX = rand.nextDouble() * maxX; 
				//				double newY = rand.nextDouble() * maxY;

				node1.setPlacement( x2, y2 );
				node2.setPlacement( x1, y1 );

				double afterScore = bbMetric.getScore();

				double delta = afterScore - beforeScore;

				if( !( delta < 0 ) && ! ( rand.nextDouble() < Math.exp( -delta / temp ) ) ) {

					node1.setPlacement( x1, y1 );
					node2.setPlacement( x2, y2 );
					declines += 1;

//					node1.setOrientation(node1.getPlacementOrientation().concatenate(Orientation.R));
//					
//					double afterRotationScore = bbMetric.getScore();
//					delta = afterRotationScore - beforeScore;
//					if( !( delta < 0 ) && ! ( rand.nextDouble() < Math.exp( -delta / temp ) ) ) {
//						node1.setOrientation(node1.getPlacementOrientation().concatenate(Orientation.X));						
//					} else{
//						 beforeScore = afterRotationScore;
//					}
				} else {
					accepts += 1;
					beforeScore = afterScore;
				}
			}					
			temp *= ratio;			
			
//			acceptRate = accepts / declines;
			
			System.out.println(temp);			
			System.out.println( "SCORE: " + beforeScore );
			System.out.println("Accepts=" + accepts + ", declines=" + declines + ", ratio=" + accepts / declines);
		
		}

		//Step 2: Run Moves and Rotations
		temp = INITIAL_TEMP;
		accepts = 1;
		declines = 1;
		beforeScore =  aoMetric.getScore();
		while (temp > 0.01) {
			
			for( int i = 0; i < INNER_ITERATIONS; i++ ){				

				int index = (int) Math.round( rand.nextDouble()*( nodes.length - 1 ) );				
				//int index2 = (int) Math.round( rand.nextDouble()*( nodes.length - 1 ) );

				PlacementNode node1 = nodes[index];
				//PlacementNode node2 = nodes[index2];

				double x1 = node1.getPlacementX();
				double y1 = node1.getPlacementY();

//				double cx = maxX - x1;
//				double cy = maxY - y1;
				
				double newX = 0; 
				double newY = 0;
				
				boolean sx = true;
				boolean sy = true;
				
				if ( x1 < maxX/2 ) { 
					newX =  x1 + rand.nextDouble() * ( node1.getWidth()/20 );
				} else {
					newX = x1 - rand.nextDouble() * ( node1.getWidth()/20 );
					sx = false;
				}
				
				if ( y1 < maxY/2 ){
					newY = y1 + rand.nextDouble() * ( node1.getHeight()/20 );
				} else {
					newY = y1 - rand.nextDouble() * ( node1.getHeight()/20 );
					sy = false;
				}
				
//				double newX = rand.nextDouble() * maxX; 
//				double newY = rand.nextDouble() * maxY;

				//Calculate new location
				// 1) select connection
				
//				int portIndex = (int) ((int) (node1.getPorts().size() - 1) * rand.nextDouble());
//				PlacementPort p = node1.getPorts().get(portIndex);
//				
//				PlacementNetwork net = p.getPlacementNetwork();
//				double x2;
//				double y2;
//
//				if (net == null) {
//					continue;
//				}
//				
//				List<PlacementPort> ports =  net.getPortsOnNet();
//
//				if (ports != null) {
//					int partnerPortIndex = (int)((ports.size()-1)* rand.nextDouble());
//					PlacementPort partnerPort = net.getPortsOnNet().get(partnerPortIndex); 
//					
//					x2 = partnerPort.getPlacementNode().getPlacementX() + partnerPort.getRotatedOffX();
//					y2 = partnerPort.getPlacementNode().getPlacementY()+ partnerPort.getRotatedOffY();
//	
//					double xDirection = (x1-x2) * (rand.nextDouble() / 0.6 + 0.2);
//					double yDirection = (y1-y2) * (rand.nextDouble() / 0.6 + 0.2);
//					double distance = Math.sqrt(xDirection * xDirection + yDirection * yDirection);
//	
//					x2 += distance / 8 - (distance / 4) * rand.nextDouble();  
//					y2 += distance / 8 - (distance / 4) * rand.nextDouble();
//
//				} else {					
//					x2 = x1 - maxX + maxX * rand.nextDouble();
//					y2 = y1 - maxY + maxY * rand.nextDouble();					
//				}
//
//					node1.setPlacement( x2, y2 );
				node1.setPlacement( newX, newY );

				double afterScore =  aoMetric.getScore();
				
				double delta = afterScore - beforeScore;

				if( !( delta <= 0 ) ) {

					node1.setPlacement( x1, y1 );				
					declines += 1;
					
					if( rand.nextBoolean() ){
						if ( sx ) {
							newX = x1 + rand.nextDouble() * ( node1.getWidth()/20 );
							newY = y1;
						} else {
							newX = x1 - rand.nextDouble() * ( node1.getWidth()/20 );
							newY = y1;
						}
						
					} else {
						if( sy ){
							newX = x1;
							newY = y1 + rand.nextDouble() * ( node1.getHeight()/20 );
						} else {
							newX = x1;
							newY = y1 - rand.nextDouble() * ( node1.getHeight()/20 );
						}
					}
					
					node1.setPlacement( newX , newY );
					
//					node1.setOrientation(node1.getPlacementOrientation().concatenate(Orientation.R));
//
					afterScore = aoMetric.getScore();
					
					delta = afterScore - beforeScore;
					if( !( delta <= 0 ) ) {					
						node1.setPlacement( x1 , y1 );
					} else{
						beforeScore = afterScore;
						accepts += 1;
					}
				} else {
					accepts += 1;
					beforeScore = afterScore;
				}
			}					

			temp *= ratio;
			
			
			System.out.println(temp);			
			System.out.println( "SCORE: " + beforeScore );
			System.out.println("Accepts=" + accepts + ", declines=" + declines + ", ratio=" + accepts / declines);
			
		}
		
		//last step: transform placement back to the non-array
		for (int i = 0; i < nodes.length; i++) {
			nodesToPlace.set(i, nodes[i]);
		}
		
		System.out.println("Final Bounding Box: " + bbMetric.getScore());
		
	}
}
