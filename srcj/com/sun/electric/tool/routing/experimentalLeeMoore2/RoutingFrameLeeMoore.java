/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: RoutingFrameLeeMoore.java
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

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.tool.routing.experimentalLeeMoore2.GlobalRouterV3.RegionToRoute;
import com.sun.electric.tool.routing.experimentalLeeMoore2.GlobalRouterV3.RouteToStitch;

/**
 * Acts as interface betweed electric and our routing algorithm. Here our
 * algorithm specific classes are defined and conversions are done.
 * 
 * @author Alexander Herzog, Martin Fietz
 * 
 */
public class RoutingFrameLeeMoore extends BenchmarkRouter {

	final static boolean IS_DEBUG = false;

	public String getAlgorithmName() {
		return "Lee/Moore - 2";
	}

	/** number of threads */

	private RoutingLayer[] metalLayers;

	List<RoutingContact> allContacts;

	public static class Coordinate {
		double x;
		double y;
		int layer;
		ManhattenAlignment alignment = ManhattenAlignment.ma_undefined;

		public Coordinate(double x, double y, int z,
				ManhattenAlignment alignment) {
			this.x = x;
			this.y = y;
			this.layer = z;
			this.alignment = alignment;
		}

		public Coordinate(double x, double y, int z) {
			this.x = x;
			this.y = y;
			this.layer = z;
		}

		public Point2D getLocation() {
			return new Point2D.Double(x, y);
		}

		public int getLayer() {
			return layer;
		}
	}

	enum ManhattenAlignment {
		ma_horizontal, ma_vertical, ma_undefined
	}
	
	class RoutingIterationAnalysis{
		int iteration;
		long global_ns;
		long detailed_ns;
		int wires_routed;
	}

	/** converts data, starts routing and then reconverts data */
	protected void runRouting(Cell cell, List<RoutingSegment> segmentsToRoute,
			List<RoutingLayer> allLayers, List<RoutingContact> allContacts,
			List<RoutingGeometry> blockages) {
		this.allContacts = allContacts;

		long startTime = System.currentTimeMillis();
		int numMetalLayers = 0;
		for (RoutingLayer rl : allLayers)
			if (rl.isMetal())
				numMetalLayers++;
		metalLayers = new RoutingLayer[numMetalLayers + 1];
		for (RoutingLayer rl : allLayers)
			if (rl.isMetal())
				metalLayers[rl.getMetalNumber()] = rl;

		/** global router */
		double minWidth = Double.MIN_VALUE;
		for (RoutingLayer lay : metalLayers) {
			if (lay != null) {
				minWidth = Math.max(lay.getMinWidth(), minWidth);
			}
		}
		double wireSpacing = Double.MIN_VALUE;
		for (RoutingLayer lay : metalLayers) {
			if (null == lay) {
				continue;
			}
			wireSpacing = Math.max(lay.getMinSpacing(lay), wireSpacing);
		}
		
		double maxSurr = Double.MIN_VALUE;
		for(RoutingLayer lay : metalLayers){
			if(lay != null){
				maxSurr = Math.max(lay.getMaxSurround(), maxSurr);
			}
		}

		double tileSize = maxSurr + minWidth;
		
		List<Integer> unrouted = null;
		Collection<RouteToStitch> routes = new ArrayList<RouteToStitch>();
		GlobalRouterV3 global_router = null;
		DetailedRouter dr = null;
		long timeLeft = 0;
		int lastUnroutedSize = segmentsToRoute.size();
		int noProgress = 0;
		
		do {
			
			if (global_router == null) {
				int num_regions = (int)Math.ceil(Math.sqrt(numThreads.getIntValue()));
				num_regions = num_regions <= 1 ? 2 : num_regions;
				global_router = new GlobalRouterV3(cell.getBounds(), num_regions,
						segmentsToRoute, numThreads.getIntValue(), tileSize);
			} else {
				global_router.Reinitialize(unrouted);
			}
			global_router.StartGlobalRouting();
			Collection<RouteToStitch> cur_routes = global_router.output_coarse_routes
					.values();
			RegionToRoute[] regions = global_router.output_regions;
//			debug("GlobalRouter created " + cur_routes.size() + " routes");

//			adjustLayers(cur_routes, tileSize);

			/* detailed router */
			if (dr == null) {
				dr = new DetailedRouter(numThreads.getIntValue(), metalLayers,
						regions, tileSize, enableOutput.getBooleanValue());
			}
			dr.setRegions(regions);
			timeLeft = maxRuntime.getIntValue()
					- (System.currentTimeMillis() - startTime) / 1000;
			dr.setTimeout(timeLeft);
			dr.start();
			dr.writeSolution();
			unrouted = dr.getUnroutables();
			/* remove unrouted routes and add them to the final routes */
			Iterator<RouteToStitch> it = cur_routes.iterator();
			while (it.hasNext()) {
				RouteToStitch single_route = it.next();
				if (!unrouted.contains(single_route.id)) {
					/* assert that routes does not contain this route already */
//					for (RouteToStitch rts : routes) {
//						assert (rts.id != single_route.id);
//					}

					/* add to route */
					routes.add(single_route);
				}
			}
			timeLeft = maxRuntime.getIntValue()
					- (System.currentTimeMillis() - startTime) / 1000;
			if( global_router.wiried_routes.size() == lastUnroutedSize ) {
				noProgress +=1;
			}
			else {
				noProgress = 0;
			}
			lastUnroutedSize = global_router.wiried_routes.size();
		} while (  unrouted.size() > 0 && timeLeft > 0 && noProgress <= 10 );

//		writeAnalysis(process_analysis);
		/** stitch lists together */
//		debug("Stitching Routes...");
		Iterator<RouteToStitch> routes_it = routes.iterator();
		while (routes_it.hasNext()) {
			RouteToStitch to_stitch = routes_it.next();

			
			Iterator<SegPart> ts_it = to_stitch.coarse_route.iterator();
			List<Coordinate> stitched_list = new ArrayList<Coordinate>();
			while (ts_it.hasNext()) {
				SegPart cur_patch = ts_it.next();
//				if(!(skipped == null || skipped == cur_patch.segment_part.get(0))){
//					/* if exception occurs, the last point of a segPart is not 
//						the same as the first point of its successor */
//					assert(skipped == null || skipped == cur_patch.segment_part.get(0));
//				}
				Coordinate co;
				for (Iterator<Coordinate> i = cur_patch.segment_part.iterator(); i
						.hasNext();) {
					co = i.next();
					if (i.hasNext() || !ts_it.hasNext()) {
						stitched_list.add(co);
					}
				}
			}
			global_router.Retransform(stitched_list);
			stitched_list = minimizeCoordinateRoute(stitched_list);
			correctStartEnd(stitched_list);
			
			//debugPrintCoordinateList(stitched_list);
			doWiring(to_stitch.seg_head_tail, stitched_list);
		}
	}
	
	private void correctStartEnd(List<Coordinate> route){
		/** begin with start */
		Coordinate start = route.get(0);
		Coordinate start_succ = route.get(1);
		Coordinate start_link = addCornerPoint(start, start_succ);
		route.add(1, start_link);
		
		/** now the same with end */
		if(route.size() > 3){
			Coordinate end = route.get(route.size() - 1);
			Coordinate end_prev = route.get(route.size() - 2);
			Coordinate end_link = addCornerPoint(end, end_prev);
			route.add(route.size() - 1, end_link);
		}
	}
	
	private Coordinate addCornerPoint(Coordinate precise, Coordinate non_precise){
//		assert(precise.layer == non_precise.layer);
		Coordinate ret = new Coordinate(-1, -1, precise.layer);
		if(precise.layer % 2 == 0){	//even layer => vertical
			ret.x = non_precise.x;
			ret.y = precise.y;
		}else{	//odd layer => horizontal
			ret.x = precise.x;
			ret.y = non_precise.y;
		}
		
		return ret;
	}
	
	public void adjustLayers( Collection<RouteToStitch> routes, double tileSize ) {
		for( RouteToStitch rts : routes ) {
			int lastLayer = rts.coarse_route.get(0).segment_part.get(0).getLayer();
			int finishLayer = rts.coarse_route.get(rts.coarse_route.size()-1).segment_part.get(1).getLayer();
			Coordinate c;
			/* the finish coordinate and start coordinate of two consecutive segparts are the same reference, 
			 * so we only have to process them once
			 */
			for( int i=0; i<rts.coarse_route.size()-1; i++ ) { // don't adjust the finish coordinate of the last coarse_route
				SegPart sp = rts.coarse_route.get(i);
				assert(sp.segment_part.size() == 2);
				c = sp.segment_part.get(1);
				assert(rts.coarse_route.get(i + 1).segment_part.get(0).alignment == c.alignment);
				assert(rts.coarse_route.get(i + 1).segment_part.get(0) == c);
				assert(c.alignment != ManhattenAlignment.ma_undefined);
				if( ( c.alignment == ManhattenAlignment.ma_horizontal && c.layer%2 != 1) // horizontal is even, should be odd
						|| ( c.alignment == ManhattenAlignment.ma_vertical  && c.layer%2 != 0) ) { // vertical is odd, should be even
						incOrDecLayer( c, lastLayer, finishLayer );
						lastLayer = c.getLayer();
				}		
			}
		}
	}

	/* increment or decrement coordinate layer randomly */
	public void incOrDecLayer(Coordinate c, int lastLayer, int finishLayer) {
		int dz = (int) Math.signum(finishLayer - lastLayer);
		if (dz == 0) {
			dz = -1;
		}
		if (0 < c.layer+dz && c.layer+dz < metalLayers.length) {
			c.layer += dz;
		} else if (0 < c.layer-dz && c.layer-dz < metalLayers.length) {
			c.layer -= dz;
		}
	}

//	public static void printCoordinateList(List<Coordinate> cs) {
//		for (Coordinate c : cs)
//			printCoordinate(c);
//		System.out.println(".");
//	}
//
//	public static void printCoordinate(Coordinate c) {
//		System.out.print("(" + c.x + "," + c.y + "," + c.layer + ") ");
//	}

	private void doWiring(RoutingSegment rs, List<Coordinate> coords) {
		RoutePoint rpStart = new RoutePoint(RoutingContact.STARTPOINT, coords
				.get(0).getLocation(), 0);
		rs.addWireEnd(rpStart);
		RoutePoint fromRP = rpStart;
		RoutingLayer fromLayer = metalLayers[coords.get(0).layer];
		for (int i = 1; i < coords.size()-1; i++) {
			RoutingLayer toLayer = metalLayers[coords.get(i).getLayer()];
			RoutingContact rc = getContact(coords, i);
//			assert(rc != null);
			RoutePoint toRP = new RoutePoint(rc, coords.get(i).getLocation(), 0);
			RouteWire rw = new RouteWire(toLayer, toRP, fromRP, toLayer.getMinWidth() );
			if(fromLayer == toLayer || isVia( coords, i)) {
				rs.addWireEnd(toRP);
				rs.addWire( rw );
				fromRP = toRP;
			}
			fromLayer = toLayer;
		}
		RoutePoint rpFinish = new RoutePoint(RoutingContact.FINISHPOINT, coords
				.get(coords.size()-1).getLocation(), 0);
		rs.addWireEnd(rpFinish);
		RoutingLayer layer = metalLayers[coords.get(coords.size() - 1)
				.getLayer()];
		RouteWire rw = new RouteWire(layer, rpFinish, fromRP, layer
				.getMinWidth());
		rs.addWire(rw);
	}

	private List<Coordinate> minimizeCoordinateRoute(final List<Coordinate> lc) {
		List<Coordinate> result = new ArrayList<Coordinate>();
		result.add(lc.get(0));
		for (int i = 1; i < lc.size() - 1; i++) {
			Coordinate pre = lc.get(i - 1);
			Coordinate c = lc.get(i);
			Coordinate succ = lc.get(i + 1);
			// not same layer?
			if ((pre.layer != c.layer) || (c.layer != succ.layer)) {
				result.add(c);
			} else {
				// not on a straight line?
				if ((Math.abs(pre.x - c.x) < 0.001 && Math.abs(c.x - succ.x) < 0.001) == false
						&& (Math.abs(pre.y - c.y) < 0.001 && Math.abs(c.y
								- succ.y) < 0.001) == false)
					result.add(c);
			}
		}
		result.add(lc.get(lc.size() - 1));
		return result;
	}

	public RoutingContact getVia(RoutingLayer l1, RoutingLayer l2) {
//		assert (l1 != null && l2 != null);
		for (RoutingContact rc : allContacts) {
			if ((rc.getFirstLayer().equals(l1) && rc.getSecondLayer()
					.equals(l2))
					|| (rc.getFirstLayer().equals(l2) && rc.getSecondLayer()
							.equals(l1)))
				return rc;
		}
//		debug("no via: " + l1.getMetalNumber() + " -> " + l2.getMetalNumber());
		return null;
	}

	public RoutingContact getContact(List<Coordinate> coords, int i) {
		if( isVia( coords, i) ) {
			return getVia(metalLayers[coords.get(i).getLayer()],
					metalLayers[coords.get(i + 1).getLayer()]);
		}
		else {
		return metalLayers[coords.get(i).getLayer()].getPin();
		}
	}
	
	public boolean isVia(List<Coordinate> coords, int i) {
		if (i < coords.size()-1
				&& (coords.get(i).getLayer() != coords.get(i + 1).getLayer()))
		{
			return true;
		}
		else {
			return false;
		}
	}
	
//	private void debug(String s) {
//		if (enableOutput.getBooleanValue()) {
//			System.out.print(s);
//		}
//	}

//	public void debugPrintCoordinateList(List<Coordinate> cs) {
//		for (Coordinate c : cs)
//			debugPrintCoordinate(c);
//		debug(".\n");
//	}
//
//	public void debugPrintCoordinate(Coordinate c) {
//		debug("(" + c.x + "," + c.y + "," + c.layer + ") ");
//	}

	// Diese Methode muss ueberschrieben werden um eine NullPointerException zu
	// verhindern
	// public List<RoutingParameter> getParameters() {
	// List<RoutingParameter> allParams = new ArrayList<RoutingParameter>();
	// return allParams;
	// }
}
