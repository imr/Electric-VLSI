/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Map.java
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
 * Contains the status of the grid points
 * 
 * @author Christian Jülg
 * @author Jonas Thedering
 */
public class Map {

	//important: map[layer][x][y]!
	private int[][][] map;
	private QuadTree[] quadTrees;

	// Constants for representing special fields
	static final int X = -3;
	static final int CLEAR = 0;
	
	// Displacement to map the given coordinates to the field array
	private int dispX, dispY;
	
	// this is the factor by which the Map is shrunk
	private double scalingFactor;
	private int width;
	private int height;

	public Map(double minWidth, int widthUnscaled, int heightUnscaled, int layers, int dispX, int dispY) {
		this.scalingFactor = minWidth;
		assert scalingFactor >= 1d : scalingFactor;
		
		this.width = (int) Math.ceil(widthUnscaled / scalingFactor);
		this.height = (int) Math.ceil(heightUnscaled / scalingFactor);
		
		map = new int[layers][width][height];
		
		quadTrees = new QuadTree[layers];
		for(int i=0; i < layers; ++i)
			quadTrees[i] = new QuadTree(width, height);
		
		this.dispX = dispX;
		this.dispY = dispY;
	}
	
	public int getDispX(){
		return dispX;
	}
	
	public int getDispY(){
		return dispY;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public double getScalingFactor() {
		return scalingFactor;
	}

	public int getLayers() {
		return map.length;
	}

	public int getStatus(int x, int y, int z) {
		return map[z][x][y];
	}
	
	public int getSpaceAround(int x, int y, int z) {
		return quadTrees[z].spaceAt(x, y);
	}

	public void setStatus(int x, int y, int z, int status) {
		map[z][x][y] = status;
		// QuadTree can't handle clearing
		assert status != CLEAR;
		quadTrees[z].add(x, y);
	}

	public void setStatus(int[] x, int[] y, int[] z, int status) {
		// QuadTree can't handle clearing
		assert status != CLEAR;
		for (int i = 0; i < x.length; ++i) {
			map[z[i]][x[i]][y[i]] = status;
			quadTrees[z[i]].add(x[i], y[i]);
		}
	}
}
