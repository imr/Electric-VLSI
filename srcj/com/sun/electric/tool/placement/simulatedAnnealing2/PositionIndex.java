/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: PositionIndex.java
 * Written by Team 6: Sebastian Roether, Jochen Lutz
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
package com.sun.electric.tool.placement.simulatedAnnealing2;

import com.sun.electric.database.geometry.Orientation;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * To speed up the node overlap calculation, this class provides a method
 * that returns only the nodes within the proximity of a given node. For this,
 * the area is partitioned into quadrants (buckets) into which the nodes are sorted.
 * A Node is sorted into a bucket when a least one of its corners is within the
 * boundaries of that quadrant (bucket)
 */
public class PositionIndex {
	
	/**
	 * This class holds all information that is needed to evaluate the area effects
	 * of a perturbation. it will not be altered so the calculation is not affected
	 * by other threads
	 * 
	 * @author Basti
	 *
	 */
	public static class AreaSnapshot {
		public double maxX = -Double.MAX_VALUE;
		public double minX = Double.MAX_VALUE;
		public double maxY = -Double.MAX_VALUE;
		public double minY = Double.MAX_VALUE;
		
		public double sndMaxX = maxX;
		public double sndMinX = minX;
		public double sndMaxY = maxY;
		public double sndMinY = minY;
		
		// NOTE: there may be more than one node that has maximum coordinates. this causes the boundaries to be recalculated more often than needed
		public ProxyNode node_maxX = null;
		public ProxyNode node_maxY = null;
		public ProxyNode node_minY = null;
		public ProxyNode node_minX = null;
		
		public ProxyNode node_sndMaxX = null;
		public ProxyNode node_sndMaxY = null;
		public ProxyNode node_sndMinY = null;
		public ProxyNode node_sndMinX = null;
		
		
		protected AreaSnapshot clone() {
			AreaSnapshot clone = new AreaSnapshot();
			
			clone.maxX = maxX;
			clone.minX = minX;
			clone.maxY = maxY;
			clone.minY = minY;
			
			clone.sndMaxX = sndMaxX;
			clone.sndMaxY = sndMaxY;
			clone.sndMinX = sndMinX;
			clone.sndMinY = sndMinY;
			
			clone.node_maxX = node_maxX;
			clone.node_maxY = node_maxY;
			clone.node_minX = node_minX;
			clone.node_minY = node_minY;
			
			clone.node_sndMaxX = node_sndMaxX;
			clone.node_sndMaxY = node_sndMaxY;
			clone.node_sndMinX = node_sndMinX;
			clone.node_sndMinY = node_sndMinY;

			return clone;
		}
		
		public double getArea()
		{
			return (maxX - minX) * (maxY - minY);
		}
		
		/**
		 * Method that calculates the total chip are after the node <node> has been removed
		 * and the node <dummy> has been placed
		 * @param node The actual node that is replaced by <dummy> and should be ignored
		 * @param dummy The dummy that replaces <node>
		 * @return The area after <node> has been replaced by <dummy>
		 */
		public double areaForDummy(ProxyNode node, ProxyNode dummy)
		{
			double lminX = minX;
			double lmaxX = maxX;
			double lminY = minY;
			double lmaxY = maxY;
			
			double ld = dummy.getPlacementX() - dummy.width/2;
			double rd = dummy.getPlacementX() + dummy.width/2;
			double td = dummy.getPlacementY() - dummy.height/2;
			double bd = dummy.getPlacementY() + dummy.height/2;
			
			// Remove node
			// Get new boundaries for when the node to be moved was an outmost node
			if(node == node_maxX) lmaxX = sndMaxX;
			if(node == node_minX) lminX = sndMinX;
			if(node == node_minY) lminY = sndMinY;
			if(node == node_maxY) lmaxY = sndMaxY;
			
			// Add dummy node
			if(ld < lminX) { lminX = ld; }
			if(rd > lmaxX) { lmaxX = rd; }
			if(td < lminY) { lminY = td; }
			if(bd > lmaxY) { lmaxY = bd; }
			
			return (lmaxX-lminX) * (lmaxY-lminY);		
		}
		
		/**
		 * Method that calculates the total chip are after two nodes have been removed
		 * and two nodes have been placed
		 * @return The area after <node1>, <node2> have been replaced by <dummy1>, <dummy2>
		 */
		public double areaForDummy(ProxyNode node1, ProxyNode node2, ProxyNode dummy1, ProxyNode dummy2 )
		{
			double lminX = minX;
			double lmaxX = maxX;
			double lminY = minY;
			double lmaxY = maxY;
			
			double ld1 = dummy1.getPlacementX() - dummy1.width/2;
			double rd1 = dummy1.getPlacementX() + dummy1.width/2;
			double td1 = dummy1.getPlacementY() - dummy1.height/2;
			double bd1 = dummy1.getPlacementY() + dummy1.height/2;
			
			double ld2 = dummy2.getPlacementX() - dummy2.width/2;
			double rd2 = dummy2.getPlacementX() + dummy2.width/2;
			double td2 = dummy2.getPlacementY() - dummy2.height/2;
			double bd2 = dummy2.getPlacementY() + dummy2.height/2;
			
			// Remove nodes
			// Get new boundaries for when one of the nodes to be moved was an outmost node
			if(node1 == node_maxX || node2 == node_maxX) lmaxX = sndMaxX;
			if(node1 == node_minX || node2 == node_minX) lminX = sndMinX;
			if(node1 == node_minY || node2 == node_minY) lminY = sndMinY;
			if(node1 == node_maxY || node2 == node_maxY) lmaxY = sndMaxY;
			
			// Add dummy nodes
			if(ld1 < lminX) { lminX = ld1; }
			if(rd1 > lmaxX) { lmaxX = rd1; }
			if(td1 < lminY) { lminY = td1; }
			if(bd1 > lmaxY) { lmaxY = bd1; }
			
			if(ld2 < lminX) { lminX = ld2; }
			if(rd2 > lmaxX) { lmaxX = rd2; }
			if(td2 < lminY) { lminY = td2; }
			if(bd2 > lmaxY) { lmaxY = bd2; }
			
			return (lmaxX-lminX) * (lmaxY-lminY);	
		}
		
		/**
		 * Method that stretches the boundaries so that a new node falls within the covered area
		 * @param node 
		 * @param l x-coordinate of the left edge of the node 
		 * @param r right
		 * @param t top
		 * @param b bottom
		 */
		public void add(ProxyNode node, double l, double r, double t, double b)
		{
			// Left
			if ( node == node_minX || node == node_minY || node == node_maxX || node == node_maxY )
				return;

			if(l < sndMinX) {
				sndMinX = l;
				node_sndMinX = node;
				if(l < minX) {
					sndMinX = minX;
					minX = l;
					node_sndMinX = node_minX;
					node_minX = node;
				}
			}	
			
			// Right
			if(r > sndMaxX) {
				sndMaxX = r;
				node_sndMaxX = node;
				if(r > maxX) {
					sndMaxX = maxX;
					maxX = r;
					node_sndMaxX = node_maxX;
					node_maxX = node;
				}
			}	
			
			// Top
			if(t < sndMinY) {
				sndMinY = t;
				node_sndMinY = node;
				if(t < minY) {
					sndMinY = minY;
					minY = t;
					node_sndMinY = node_minY;
					node_minY = node;
				}
			}	
			
			// Bottom
			if(b > sndMaxY) {
				sndMaxY = b;
				node_sndMaxY = node;
				if(b > maxY) {
					sndMaxY = maxY;
					maxY = b;
					node_sndMaxY = node_maxY;
					node_maxY = node;
				}
			}	
		}
	}
	
	public AreaSnapshot area = new AreaSnapshot();
	private AreaSnapshot area_buffer = new AreaSnapshot();
	
	private ArrayList<ProxyNode>[][] buckets;
	private double bucketSize;
	private int bucketCount;

	private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
	private final Lock read = rwl.readLock();
	private final Lock write = rwl.writeLock();
	
	/**
	 * 
	 * @param chipLength the maximum chip length
	 * @param nodesToPlace the nodes to be sorted into the buckets
	 */
	PositionIndex( double chipLength, ArrayList<ProxyNode> nodesToPlace )
	{
		// the bucket size is twice the size of the biggest node
		// (so the area is four times the area of the biggest single node)
		double totalWidth = 0, totalHeight = 0;
		
		for(ProxyNode p : nodesToPlace) {
			//if(p.width > bucketSize) bucketSize = p.width;
			//if(p.height > bucketSize) bucketSize = p.width;
			totalWidth += p.width;
			totalHeight += p.height;
		}
		//bucketSize *= 2;
		bucketSize = (totalWidth + totalHeight) / (2 * nodesToPlace.size());
		
		// calculate how many buckets are needed and sort nodes into buckets
		bucketCount = (int) Math.ceil(chipLength / bucketSize);
		buckets = new ArrayList[bucketCount][bucketCount];
		for ( int i = 0; i < bucketCount; i++ )
			for ( int j = 0; j < bucketCount; j++ )
				buckets[i][j] = new ArrayList<ProxyNode>();

		for ( ProxyNode p : nodesToPlace ) {
			put( p, p.getPlacementX(), p.getPlacementY() );
		}
		
		swapAreaBuffer();
	}

	/**
	 * Adds a node to all buckets that it currently overlaps
	 * @param node the node to add to the bucket
	 */
	private void put( ProxyNode node ) {
		put( node, node.getPlacementX(), node.getPlacementY());
	}
	
	/**
	 * Adds a node to all buckets that it overlaps when placed at the given coordinates
	 * @param node the node to add to the bucket
	 * @param posX
	 * @param posY
	 */
	private void put( ProxyNode node, double posX, double posY )
	{
		double l = posX - node.width/2;
		double r = posX + node.width/2;
		double t = posY - node.height/2;
		double b = posY + node.height/2;
				
		int minX = bucketNum( l );
		int minY = bucketNum( t );
		int maxX = bucketNum( r );
		int maxY = bucketNum( b );

		area_buffer.add(node, l, r, t, b);
		
		for ( int x = minX; x <= maxX; x++ ) {
			for ( int y = minY; y <= maxY; y++ ) {
				buckets[x][y].add( node );
			}
		}
	}

	/**
	 * Removes a node from all buckets
	 * @param node the node to remove
	 */
	private void remove( ProxyNode node )
	{
		double l = node.getPlacementX() - node.width/2;
		double r = node.getPlacementX() + node.width/2;
		double t = node.getPlacementY() - node.height/2;
		double b = node.getPlacementY() + node.height/2;
				
		int minX = bucketNum( l );
		int minY = bucketNum( t );
		int maxX = bucketNum( r );
		int maxY = bucketNum( b );
		
		for ( int x = minX; x <= maxX; x++ ) {
			for ( int y = minY; y <= maxY; y++ ) {
				buckets[x][y].remove( node );
			}
		}
		if(node == area_buffer.node_minX || 
		   node == area_buffer.node_maxX ||
		   node == area_buffer.node_minY ||
		   node == area_buffer.node_maxY ||
		   node == area_buffer.node_sndMaxX ||
		   node == area_buffer.node_sndMinX ||
		   node == area_buffer.node_sndMaxY ||
		   node == area_buffer.node_sndMinY
		) {
			// TODO: this is not always necessary because that node might be moved
			// outwards an will therefore be added later
			calcBoundaries();
		}
	}
	
	/**
	 * Method that finds the outmost nodes and creates a new <AreaSnapshot>
	 */
	private void calcBoundaries() 
	{
		// only call this while holding the write lock
		
		area_buffer = new AreaSnapshot();
		
		// TODO PERFORMANCE to find the boundaries we could easily search from the outer buckets
		// and stop after checking all nodes in the same row/column
		for ( int i = 0; i < bucketCount; i++ )
			for ( int j = 0; j < bucketCount; j++ )
				for(ProxyNode node: buckets[i][j]) {
					double l = node.getPlacementX() - node.width/2;
					double r = node.getPlacementX() + node.width/2;
					double t = node.getPlacementY() - node.height/2;
					double b = node.getPlacementY() + node.height/2;

					area_buffer.add(node, l, r, t, b);
				}
	}
	
	/**
	 * Swaps two nodes
	 * @param node1
	 * @param node2
	 */
	public void swap( ProxyNode node1, ProxyNode node2)
	{
		double x = node1.getPlacementX();
		double y = node1.getPlacementY();
		
		write.lock();
		remove(node1);
		remove(node2);
		
		node1.setPlacement(node2.getPlacementX(), node2.getPlacementY());
		node2.setPlacement(x, y);
		
		put(node1);
		put(node2);

		swapAreaBuffer();
		write.unlock();
	}
	
	/**
	 * Rotates a node
	 * @param node
	 * @param o the new orientation of that node
	 */
	public void rotate (ProxyNode node, Orientation o)
	{
		write.lock();
		remove(node);
		node.setPlacementOrientation(o, false); 
		put(node);
		swapAreaBuffer();
		write.unlock();
	}
	
	/**
	 * Moves a node to another position 
	 * @param node
	 * @param newX
	 * @param newY
	 */
	public void move( ProxyNode node, double newX, double newY )
	{
		write.lock();
		remove(node);
		node.setPlacement(newX, newY);
		put(node, newX, newY);
		swapAreaBuffer();
		write.unlock();
	}
	
	/**
	 * Swaps the current area buffer with the one that was created by the last perturbation
	 * This is for making the perturbation atomic without using a lock (but the whole idea is...)
	 */
	private void swapAreaBuffer()
	{
		area = area_buffer;
		area_buffer = area.clone();
	}

	/**
	 * Returns a list with nodes that possibly overlap with a given node
	 * @param node
	 * @return List of all nodes which occupy the same buckets as /node/. Do not necessarily overlap.
	 */
	public List<ProxyNode> getPossibleOverlaps( ProxyNode node )
	{
		List<ProxyNode> result = new ArrayList<ProxyNode>();

		int minX, minY, maxX, maxY;

		minX = bucketNum( node.getPlacementX() - node.width/2 );
		minY = bucketNum( node.getPlacementY() - node.height/2 );
		maxX = bucketNum( node.getPlacementX() + node.width/2 );
		maxY = bucketNum( node.getPlacementY() + node.height/2 );

		read.lock();
		for ( int x = minX; x <= maxX; x++ ) {
			for ( int y = minY; y <= maxY; y++ ) {
			result.addAll( buckets[x][y] );
			
			// TODO Check for duplicates
			/*
				for ( ProxyNode p : buckets[x][y] ) {
					if ( ! result.contains( p ) )
						result.add( p );
				}
				*/
			}
		}
		read.unlock();

		return result;
	}

	/**
	 * Calculates into which bucket a coordinate falls
	 * @param coordinate along an axis
	 * @return the bucketIndex for the given coordinate
	 */
	private final int bucketNum( double pos )
	{
		int bucket = (int) Math.floor( ( pos + bucketSize * bucketCount/2 ) / bucketSize );

		if ( bucket < 0 )
			bucket = 0;
		if ( bucket >= bucketCount )
			bucket = bucketCount - 1;

		return bucket;
	}	
}
