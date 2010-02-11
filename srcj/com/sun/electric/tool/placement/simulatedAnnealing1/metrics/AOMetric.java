/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: AOMetric.java
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
package com.sun.electric.tool.placement.simulatedAnnealing1.metrics;


import com.sun.electric.tool.placement.PlacementFrame.PlacementNode;

/** Parallel Placement
 **/
public class AOMetric {	
	
	private PlacementNode[] allNodes;
	private double currentScore;

	/**
	 * Method to create a AreaOverlapMetric object.
	 * @param allNodes a list containing all <code>PlacementNode</code> objects.
	 */
	public AOMetric(PlacementNode[] allNodes){
		this.allNodes = allNodes;
	}

	public double getScore() {
		currentScore = 0;		
		for (PlacementNode node: allNodes) {
			currentScore += computeOverlapForNode(node);
		}		
		return currentScore;		
	}
	

	/**
	 * Method that computes all overlapping areas caused by the given <code>PlacementNode</code>.
	 * @param theOne <code>PlacementNode</code> object for which the overlapping is to be computed.
	 * @return sum of all overlapping areas caused by this node.
	 */
	public double computeOverlapForNode(PlacementNode theOne) {
		double areaOverlap= 0.0;

		for (PlacementNode notTheOne: allNodes) {
			if (theOne == notTheOne) continue;
			double x1 = theOne.getPlacementX();
			double y1 = theOne.getPlacementY();
			double w1 = theOne.getWidth();
			double h1 = theOne.getHeight();
			int angle1 = theOne.getPlacementOrientation().getAngle();
			
			double x2 = notTheOne.getPlacementX();
			double y2 = notTheOne.getPlacementY();
			double w2 = theOne.getWidth();
			double h2 = theOne.getHeight();
			int angle2 = notTheOne.getPlacementOrientation().getAngle();
			
			areaOverlap += getIntersection(x1, y1, h1, w1, angle1 , x2, y2, h2, w2, angle2 );
		}
		
		return areaOverlap;
	}

	private double getIntersection( double x1, double y1, double h1, double w1, double a1,
			double x2, double y2, double h2, double w2, double a2 ){
		
		double minX1 = 0, minY1 = 0, minX2 = 0, minY2 = 0;
		double maxX1 = 0, maxY1 = 0, maxX2 = 0, maxY2 = 0;
		
		switch((int)a1){
			case 0:	 	{
				minX1 = x1 - w1/2;
				minY1 = y1 - h1/2;
				maxX1 = x1 + w1/2;
				maxY1 = y1 + h1/2;
			}
			case 90: 	{
				minX1 = x1 - h1/2;
				minY1 = y1 - w1/2;
				maxX1 = x1 + h1/2;
				maxY1 = y1 + w1/2;
			}
			case 180:	{
				minX1 = x1 - w1/2;
				minY1 = y1 - h1/2;
				maxX1 = x1 + w1/2;
				maxY1 = y1 + h1/2;
			}
			case 270:	{
				minX1 = x1 - h1/2;
				minY1 = y1 - w1/2;
				maxX1 = x1 + h1/2;
				maxY1 = y1 + w1/2;
			}
			default: 	{
				minX1 = x1 - w1/2;
				minY1 = y1 - h1/2;
				maxX1 = x1 + w1/2;
				maxY1 = y1 + h1/2;
			}
		}
		
		switch((int)a2){
			case 0:	 	{
				minX2 = x2 - w2/2;
				minY2 = y2 - h2/2;
				maxX2 = x2 + w2/2;
				maxY2 = y2 + h2/2;
			}
			case 90: 	{
				minX2 = x2 - h2/2;
				minY2 = y2 - w2/2;
				maxX2 = x2 + h2/2;
				maxY2 = y2 + w2/2;
			}
			case 180:	{
				minX2 = x2 - w2/2;
				minY2 = y2 - h2/2;
				maxX2 = x2 + w2/2;
				maxY2 = y2 + h2/2;
			}
			case 270:	{
				minX2 = x2 - h2/2;
				minY2 = y2 - w2/2;
				maxX2 = x2 + h2/2;
				maxY2 = y2 + w2/2;
			}
			default: 	{
				minX2 = x2 - w2/2;
				minY2 = y2 - h2/2;
				maxX2 = x2 + w2/2;
				maxY2 = y2 + h2/2;
			}
		}
		
		double nx1 = Math.max( minX1, minX2 );
		double ny1 = Math.max( minY1, minY2 );
		double nx2 = Math.min( maxX1, maxX2 );
		double ny2 = Math.min( maxY1, maxY2 );
		
		if( nx2 - nx1 < 0 || ny2 - ny1 < 0 ) return 0;
		return ( nx2 - nx1 ) * ( ny2 - ny1 );
	}
	
//	/**
//	 * Method that creates a bounding <code>Rectangle2D.Double</code> given the nodes placement.
//	 * @param x centre's X coordinate.
//	 * @param y centre's Y coordinate.
//	 * @param w width of the node
//	 * @param h height of the node.
//	 * @param angle the rotation angle of the node.
//	 * @return <code>Rectangle2D.Double</code> object bounding the node.
//	 */
//	private Rectangle2D.Double getRectangleForNode(double x, double y, double w, double h, int angle){
//		//TODO: Check the correctness of the generated rectangles.
//		switch(angle){
//			case 0:	 	return new Rectangle2D.Double(x - (w/2), y - (h/2), w, h); 
//			case 90: 	return new Rectangle2D.Double(x - (h/2), y - (w/2), h, w); 
//			case 180:	return new Rectangle2D.Double(x - (w/2), y - (h/2), w, h); 
//			case 270:	return new Rectangle2D.Double(x - (h/2), y - (w/2), h, w); 
//			default: 	return new Rectangle2D.Double(x - (w/2), y - (h/2), w, h);
//		}
//	}
//	
//	/**
//	 * Method to compute the area overlapped by the two given <code>Rectangle2D.Double</code> objects.
//	 * @param r1 the first <code>Rectangle2D.Double</code> object.
//	 * @param r2 the second <code>Rectangle2D.Double</code> object
//	 * @return returns the area being overlapped by the given rectangles.
//	 */
//	private double getIntersectionArea(Rectangle2D.Double r1, Rectangle2D.Double r2){
//		double area = 0.0;
//		Rectangle2D overlap = r1.createIntersection(r2);
//		if(!overlap.isEmpty()) area = overlap.getWidth()*overlap.getHeight();
//		return area;
//	}
}
