/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: QuadTree.java
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
 * Recursively sub-divides the grid into squares until reaching the cutOffSize 
 * to allow for querying the size of the free square around a position
 * 
 * @author Jonas Thedering
 * @author Christian Jülg
 */
public class QuadTree {
	private static class QuadNode {
		public QuadNode[] children = new QuadNode[4];
	}
	
	private static final int cutOffSize = 4;
	
	private int size;
	
	private QuadNode root = new QuadNode();
	
	// Special node to avoid allocations for smallest nodes
	private QuadNode terminator;
	
	public QuadTree(int width, int height) {
		size = roundUpToPowerOf2(Math.max(width, height));
		
		terminator = new QuadNode();
		for(int i = 0; i < terminator.children.length; ++i)
			terminator.children[i] = terminator;
	}
	
	private int roundUpToPowerOf2(int v) {
		// Taken from http://graphics.stanford.edu/~seander/bithacks.html#RoundUpPowerOf2
		v--;
		v |= v >> 1;
		v |= v >> 2;
		v |= v >> 4;
		v |= v >> 8;
		v |= v >> 16;
		v++;
		
		return v;
	}
	
	public void add(int x, int y) {
		QuadNode current = root;
		int cellSize = size;
		
		while(cellSize > cutOffSize) {
			cellSize /= 2;
			int xIndex = x / cellSize;
			int yIndex = y / cellSize;
			QuadNode next = current.children[xIndex + 2 * yIndex];
			
			if(next == null) {
				if(cellSize <= cutOffSize)
					next = terminator;
				else
					next = new QuadNode();
				
				current.children[xIndex + 2 * yIndex] = next;
			}
			
			x -= cellSize * xIndex;
			y -= cellSize * yIndex;
			
			current = next;
		}
	}
	
	/** @return the size of the free square around the given position or 0 if the position is occupied or the cutOffSize has been reached */
	public int spaceAt(int x, int y) {
		QuadNode current = root;
		int cellSize = size;
		
		while(cellSize > cutOffSize) {
			cellSize /= 2;
			int xIndex = x / cellSize;
			int yIndex = y / cellSize;
			QuadNode next = current.children[xIndex + 2 * yIndex];
			
			if(next == null) {
				return cellSize;
			}
			
			x -= cellSize * xIndex;
			y -= cellSize * yIndex;
			
			current = next;
		}
		
		return 0;
	}
	
	// "Unit test"
	public static void main(String[] args) {
		int width = 15, height = 17;
		QuadTree q = new QuadTree(width, height);
		
		q.add(0, 0);
		
		for(int y=0; y < height; ++y) {
			for(int x=0; x < width; ++x) {
				System.out.printf("%02d ", q.spaceAt(x, y));
			}
			System.out.println();
		}
	}
}
