/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CheckboardingField.java
 * Written by Team 7: Felix Schmidt, Daniel Lechner
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
package com.sun.electric.tool.placement.forceDirected2.forceDirected.util;

import java.awt.geom.Point2D;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.electric.tool.placement.PlacementFrame.PlacementNode;
import com.sun.electric.tool.placement.forceDirected2.AdditionalNodeData;
import com.sun.electric.tool.placement.forceDirected2.PlacementForceDirectedStaged;
import com.sun.electric.tool.placement.forceDirected2.forceDirected.OverlapDirection;

/**
 * Parallel Placement
 * 
 * This class represents a checkboarding field. Use this field for creating a
 * checkboarding pattern for placement.
 */
public class CheckboardingField {

	/**
	 * 
	 * @author fschmidt
	 * 
	 */
	private class FieldBoardingDTO {
		double cellMinX, cellMinY, cellMaxX, cellMaxY;
	}

	private Point2D location;
	private double cellX;
	private double cellY;
	private PlacementNode node;
	private int cellId;
	private FieldBoardingDTO fieldBoarding;
	private AtomicInteger counter = new AtomicInteger(0);
	private int coordX;
	private int coordY;

	private int notMovedCounter;

	/**
	 * Constructor, set members
	 * 
	 * @param loc
	 * @param x
	 * @param y
	 * @param cellId
	 */
	public CheckboardingField(Point2D loc, double x, double y, int cellId) {
		this.location = loc;
		this.cellX = x;
		this.cellY = y;
		this.setCellId(cellId);

		this.fieldBoarding = new FieldBoardingDTO();

		this.calculateFieldBoarding();
	}

	public CheckboardingField(Point2D loc, double x, double y, int cellId, int cellIdX, int cellIdY) {
		this.location = loc;
		this.cellX = x;
		this.cellY = y;
		this.setCellId(cellId);

		this.setCoordX(cellIdX);
		this.setCoordY(cellIdY);

		this.fieldBoarding = new FieldBoardingDTO();

		this.calculateFieldBoarding();
	}

	/**
	 * calculate the bounding box of this field
	 */
	private void calculateFieldBoarding() {
		this.fieldBoarding.cellMinX = this.location.getX() - this.cellX / 2;
		this.fieldBoarding.cellMaxX = this.location.getX() + this.cellX / 2;
		this.fieldBoarding.cellMinY = this.location.getY() - this.cellY / 2;
		this.fieldBoarding.cellMaxY = this.location.getY() + this.cellY / 2;
	}

	/**
	 * get cell id
	 */
	public synchronized int getCellId() {
		return this.cellId;
	}

	/**
	 * set width of this field
	 * 
	 * @return width
	 */
	public double getCellX() {
		return this.cellX;
	}

	/**
	 * get height of this field
	 * 
	 * @return height
	 */
	public double getCellY() {
		return this.cellY;
	}

	public int getCoordX() {
		return this.coordX;
	}

	public int getCoordY() {
		return this.coordY;
	}

	public AtomicInteger getCounter() {
		return this.counter;
	}

	/**
	 * @return location
	 */
	public Point2D getLocation() {
		return this.location;
	}

	/**
	 * get placement node of this field
	 */
	public synchronized PlacementNode getNode() {
		return this.node;
	}

	public int getNotMovedCounter() {
		return this.notMovedCounter;
	}

	public OverlapDirection getOverlapDirection() {
		if (this.node == null) {
			return OverlapDirection.none;
		}

		return this.getOverlapDirection(this.node.getPlacementX(), this.node.getPlacementY(), this.node.getWidth(), this.node.getHeight());
	}

	/**
	 * get the direction of overlapping
	 * 
	 * north: negativ overlapping in y direction<br />
	 * east: positiv overlapping in x direction<br />
	 * south: positiv overlapping in y direction<br />
	 * west: negativ overlapping in x direction<br />
	 * mixes (northeast, ...): overlapping in x and y direction<br />
	 * otherwise: none
	 * 
	 * @param xPos
	 * @param yPos
	 * @param x
	 * @param y
	 * @return direction
	 */
	public synchronized OverlapDirection getOverlapDirection(double xPos, double yPos, double x, double y) {
		OverlapDirection result = OverlapDirection.none;

		// create placementNode box
		double minX, maxX, minY, maxY;
		minX = xPos - x / 2;
		maxX = xPos + x / 2;
		minY = yPos - y / 2;
		maxY = yPos + y / 2;

		if (this.fieldBoarding.cellMinX > minX) {
			result = OverlapDirection.west;
		} else if (this.fieldBoarding.cellMaxX < maxX) {
			result = OverlapDirection.east;
		}

		if (this.fieldBoarding.cellMinY > minY) {
			result = OverlapDirection.mixDirections(result, OverlapDirection.south);
		} else if (this.fieldBoarding.cellMaxY < maxY) {
			result = OverlapDirection.mixDirections(result, OverlapDirection.north);
		}

		return result;
	}

	public synchronized double getOverlappingFractionInX() {

		double result;
		if (this.node != null) {
			result = this
					.getOverlappingFractionInX(this.node.getPlacementX(), this.node.getPlacementY(), this.node.getWidth(), this.node.getHeight());
		} else {
			result = 0.0;
		}
		return result;

	}

	/**
	 * get overlapping in x direction
	 * 
	 * @param xPos
	 * @param yPos
	 * @param x
	 * @param y
	 * @return percentage of overlapping
	 */
	public synchronized double getOverlappingFractionInX(double xPos, double yPos, double x, double y) {

		// create placementNode box
		double minX, maxX;
		minX = xPos - x / 2;
		maxX = xPos + x / 2;

		double overlapping = 0.0;

		if (this.fieldBoarding.cellMinX > minX) {
			overlapping = this.fieldBoarding.cellMinX - minX;
		} else if (this.fieldBoarding.cellMaxX < maxX) {
			overlapping = this.fieldBoarding.cellMaxX - maxX;
		}

		return Math.abs(overlapping / x);
	}

	public double getOverlappingFractionInY() {
		if (this.node == null) {
			return 0.0;
		}

		return this.getOverlappingFractionInY(this.node.getPlacementX(), this.node.getPlacementY(), this.node.getWidth(), this.node.getHeight());
	}

	/**
	 * get overlapping in y direction
	 * 
	 * @param xPos
	 * @param yPos
	 * @param x
	 * @param y
	 * @return percentage of overlapping
	 */
	public synchronized double getOverlappingFractionInY(double xPos, double yPos, double x, double y) {

		// create placementNode box
		double minY, maxY;
		minY = yPos - y / 2;
		maxY = yPos + y / 2;

		double overlapping = 0.0;

		if (this.fieldBoarding.cellMinY > minY) {
			overlapping = this.fieldBoarding.cellMinY - minY;
		} else if (this.fieldBoarding.cellMaxY < maxY) {
			overlapping = this.fieldBoarding.cellMaxY - maxY;
		}

		return Math.abs(overlapping / y);
	}

	/**
	 * returns true, if object with location (xPos, yPos) and width x and height
	 * y doesn't fit to the current field, otherwise false
	 * 
	 * @param xPos
	 * @param yPos
	 * @param x
	 * @param y
	 * @return true if cell doesn't fit to this field
	 */
	public boolean isOverlapping(double xPos, double yPos, double x, double y) {

		return (this.getOverlapDirection(xPos, yPos, x, y) != OverlapDirection.none);
	}

	public boolean isOverlappingBiggerThreshold(double threshold) {
		double x = this.getOverlappingFractionInX();
		double y = this.getOverlappingFractionInY();

		return ((x > threshold) || (y > threshold));
	}

	public synchronized void placeCentralized(PlacementNode node) {
		if (node != null) {
			AdditionalNodeData data = PlacementForceDirectedStaged.getNodeData().get(node);
			node.setPlacement(this.location.getX(), this.location.getY());
			PlacementForceDirectedStaged.getNodeData().put(node, data);
		}
		this.node = node;
	}

	/**
	 * set the id of this cell, the id can be used for defining positions in the
	 * checkboarding grid
	 * 
	 * @param cellId
	 */
	public void setCellId(int cellId) {
		this.cellId = cellId;
	}

	/**
	 * get width of this field
	 * 
	 * @param cellX
	 */
	public void setCellX(double cellX) {
		this.cellX = cellX;
		this.calculateFieldBoarding();
	}

	/**
	 * set field height
	 * 
	 * @param cellY
	 */
	public void setCellY(double cellY) {
		this.cellY = cellY;
		this.calculateFieldBoarding();
	}

	public void setCoordX(int coordX) {
		this.coordX = coordX;
	}

	public void setCoordY(int coordY) {
		this.coordY = coordY;
	}

	public void setCounter(AtomicInteger counter) {
		this.counter = counter;
	}

	/**
	 * set location
	 * 
	 * @param location
	 */
	public void setLocation(Point2D location) {
		this.location = location;
		this.calculateFieldBoarding();
	}

	/**
	 * set placement node
	 * 
	 * @param node
	 */
	public void setNode(PlacementNode node) {
		this.node = node;
	}

	public void setNotMovedCounter(int notMovedCounter) {
		this.notMovedCounter = notMovedCounter;
	}

}
