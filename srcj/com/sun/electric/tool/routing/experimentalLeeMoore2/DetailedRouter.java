/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DetailedRouter.java
 * Written by: Alexander Herzog, Martin Fietz (Team 4)
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
package com.sun.electric.tool.routing.experimentalLeeMoore2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.sun.electric.tool.routing.RoutingFrame.RoutingLayer;
import com.sun.electric.tool.routing.experimentalLeeMoore2.GlobalRouterV3.RegionToRoute;
import com.sun.electric.tool.routing.experimentalLeeMoore2.RoutingFrameLeeMoore.Coordinate;

public final class DetailedRouter {

	private final int numThreads;
	private long timeout;

	private RegionToRoute[] regions;

	private DetailedRoutingSolution solutions;
	private List<Integer> unrouted;

	private List<DetailedRouterWorker> workers;

	public DetailedRouter(int numThreads, RoutingLayer[] metalLayers,
			RegionToRoute[] regions, double tileSize, boolean debug ) {
		this.numThreads = numThreads;
		unrouted = new ArrayList<Integer>();
		workers = new ArrayList<DetailedRouterWorker>(regions.length);
		solutions = new DetailedRoutingSolution();
		for (int i = 0; i < regions.length; i++) {
			DetailedRouterWorker d = new DetailedRouterWorker(regions[i], metalLayers, tileSize);
			if( debug ) {
				d.enableOutput();
			}
			workers.add(d);
		}
	}

	public void start() {
		unrouted.clear();
		for (int i = 0; i < regions.length; i++) {
			workers.get(i).setRegion(regions[i]);
		}
//		debug("\n");
		ExecutorService es = Executors.newFixedThreadPool(numThreads);
		for (DetailedRouterWorker w : workers) {
			es.submit(w);
			//w.run();
		}
		es.shutdown();
		try {
			es.awaitTermination(timeout, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//get all routed patches
		for( DetailedRouterWorker w : workers ) {
			if( w.isDone() ) {
//				for(SegPart sp : w.getSolution().keySet()){
//					assert(!solutions.containsKey(sp));
//				}
				solutions.putAll( w.getSolution() );
			}
		}
//		debug("\n");
		//recognize each routes not completed
		for (RegionToRoute region : regions) {
			for (SegPart segment : region.segments_to_route) {
//				assert(segment.segment_part.size() == 2);
				if (!solutions.containsKey(segment)
						&& !unrouted.contains(segment.id)) {
					unrouted.add(segment.id);
				}
			}
		}

		// propagate findings to all workers and clean them
		for (DetailedRouterWorker w : workers) {
			w.removeSolutions( unrouted );
		}
		// clean solutions
		for( Iterator<Map.Entry<SegPart, List<Coordinate>>> it = solutions.entrySet().iterator(); it.hasNext(); ) {
			Map.Entry<SegPart,List<Coordinate>> s = it.next();
			if( unrouted.contains( s.getKey().id ) ) {
				it.remove();
			}
		}
		
		// clean region
		for (RegionToRoute region : regions) {
			for (Iterator<SegPart> itr = region.segments_to_route.iterator(); itr.hasNext(); ) {
				SegPart segment = itr.next();
				if (unrouted.contains(segment.id)) {
					itr.remove();
				}
			}
		}
	}

	public void setTimeout(long seconds) {
		this.timeout = seconds;
	}

	public void setRegions(RegionToRoute[] regions) {
		this.regions = regions;
	}

	public List<Integer> getUnroutables() {
		return unrouted;
	}

	public void writeSolution() {
		for (Map.Entry<SegPart, List<Coordinate>> s : solutions.entrySet()) {
			SegPart toRoute = s.getKey();
			List<Coordinate> routed = s.getValue();
			if (routed.size() > 2 && toRoute.segment_part.size() <= 2) {	//pay attention not to refill an earlier routed patch
//				assert(toRoute.segment_part.size() == 2);
				toRoute.segment_part.addAll( 1, routed );
			}
		}
		solutions.clear();
	}
	
//	public void debugPrintCoordinateList(List<Coordinate> cs) {
//		for (Coordinate c : cs)
//			debugPrintCoordinate(c);
//		debug(".\n");
//	}
//
//	public void debugPrintCoordinate(Coordinate c) {
//		debug("(" + c.x + "," + c.y + "," + c.layer + ") ");
//	}
//	
//	private void debug( String s ) {
//		if( this.enableOutput ) {
//			System.out.print( s );
//		}
//	}

	public static final class DetailedRoutingSolution extends
			HashMap<SegPart, List<Coordinate>> {
		/**
		 * 
		 */
		private static final long serialVersionUID = -3806453821768987428L;

		@Override
		public DetailedRoutingSolution clone() {
			return (DetailedRoutingSolution) super.clone();
		}
	}
}

//class FailReport{
//	Rectangle2D bounds;
//	int seg_id;
//	
//	public FailReport(int seg_id, Rectangle2D bounds){
//		this.bounds = bounds;
//		this.seg_id = seg_id;
//	}
//}