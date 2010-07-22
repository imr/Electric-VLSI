/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GlobalRouterV3.java
 * Written by: Alexander Herzog (Team 4)
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

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CyclicBarrier;

import com.sun.electric.tool.routing.RoutingFrame.RoutingSegment;
import com.sun.electric.tool.routing.experimentalLeeMoore2.RoutingFrameLeeMoore.Coordinate;

public class GlobalRouterV3 {
	
	/** Threads */
	int num_threads;
	
	/** barrier to synchronize stages */
	CyclicBarrier barrier;
	
	/** minimum tile size of detailed router */
	double tileSize;

	/** maximum detour */
	int max_detour = 6;
	
	/** cost for crossing a tile from horizontal to vertical or vice versa */
	double via_cost = 0.2;
	
	/** Global routing output. This represents a view on a whole route.  */
	public class RouteToStitch{
		public int id = -1;
		public List<SegPart> coarse_route = null;
		public ArrayList<Vector2i> region_positions;
		
		public RoutingSegment seg_head_tail = null;
		public int start_layer = Integer.MIN_VALUE;
		public int finish_layer = Integer.MIN_VALUE;
		
		public RouteToStitch(RoutingSegment routing_seg){
			seg_head_tail = routing_seg;
			
			start_layer = routing_seg.getStartLayers().get(0).getMetalNumber();
			finish_layer = routing_seg.getFinishLayers().get(0).getMetalNumber();
		}
		
		public ArrayList<RegionToRoute> SelectPassedRegionsFrom(RegionToRoute[] all_regions){
			ArrayList<RegionToRoute> ret = new ArrayList<RegionToRoute>();
			
			Iterator<Vector2i> it = region_positions.iterator();
			while(it.hasNext()){
				Vector2i pos = it.next();
				ret.add(all_regions[pos.y * regions_x + pos.x]);
			}
			
			return ret;
		}
	}
	public HashMap<Integer, RouteToStitch> output_coarse_routes;

	/** Adds a whole route to output data */
	public synchronized void OfferCoarseRoute(List<SegPart> route, int seg_id, ArrayList<Vector2i> passed_regions){

		RouteToStitch rts = new RouteToStitch(electric_segments.get(seg_id));
		rts.coarse_route = route;
		rts.id = seg_id;
		rts.region_positions = passed_regions;
		
		output_coarse_routes.put(new Integer(rts.id), rts);
	}
	
	/** Output data for further processing in a detailed router. */
	public class RegionToRoute{
		Rectangle2D bounds;
		List<SegPart> segments_to_route;
		
		public RegionToRoute(Rectangle2D b){
			bounds = b;
			segments_to_route = new ArrayList<SegPart>();
		}
	}
	public RegionToRoute[] output_regions;
	
	/** Adds a route patch to a output region representation */
	public synchronized void AddRouteToOutReg(Vector2i reg_pos, SegPart segment){
		RegionToRoute region = OutputRegionAt(reg_pos.x, reg_pos.y);
		region.segments_to_route.add(segment);
	}
	
	/** Access to output */
	public RegionToRoute OutputRegionAt(int x, int y){
		return output_regions[y * regions_x + x];
	}
	
	public RegionToRoute OutputRegionAt(int x, int y, RegionDirection dir){
		
		switch(dir){
		case rd_left:
			--x;
			break;
		case rd_right:
			++x;
			break;
		case rd_down:
			--y;
			break;
		case rd_up:
			++y;
			break;
		case rd_undefined:
			break;
		}
		
		if(IsCoordinateValid(x, y)){
			return OutputRegionAt(x, y);
		}else{
			return null;
		}
	}
	
	/** Information for a GlobalRouter worker to do his job */
	class SegmentInfo{
		public boolean is_initialized = false;
		public boolean was_part_of_bt = false;
		public boolean is_on_min_path = false;
		
		/** for each direction you can reach the source with several
		 * length-minimum pairs 
		 */
		private ArrayList<WeightLengthPair> left_back_scores = 
			new ArrayList<WeightLengthPair>();
		private ArrayList<WeightLengthPair> right_back_scores = 
			new ArrayList<WeightLengthPair>();
		private ArrayList<WeightLengthPair> up_back_scores = 
			new ArrayList<WeightLengthPair>();
		private ArrayList<WeightLengthPair> down_back_scores = 
			new ArrayList<WeightLengthPair>();
		
		public ArrayList<WeightLengthPair> GetScores(RegionDirection dir){
			ArrayList<WeightLengthPair> ret = null;
			switch(dir){
			case rd_left:
				ret = left_back_scores;
				break;
			case rd_right:
				ret = right_back_scores;
				break;
			case rd_up:
				ret = up_back_scores;
				break;
			case rd_down:
				ret = down_back_scores;
				break;
			case rd_undefined:
				ret = null;
				break;
			}
			
			return ret;
		}
		
		/** checks if segment is possibly part of the best backtrace and refreshes data */
		public boolean RefreshSegment(int length, double weight, RegionDirection min_dir){
			
			// first check if detour limit is reached
			if(max_detour < length){
				return false;
			}else if(!is_initialized){
				is_initialized = true;
			}
			
			if(min_dir != RegionDirection.rd_undefined){
				// drop this path if there is another path with lower cost AND lower length
					ArrayList<WeightLengthPair> scores = GetScores(min_dir);
					for(WeightLengthPair wlp : scores){
						if(wlp.length <= length && wlp.weight <= weight){
							return false;
						}
					}
				// if not returned false, insert weight-length pair
				GetScores(min_dir).add(new WeightLengthPair(weight, length));
			}else{
				// broadcast weight-length pair to all directions (needed for source segment)
				for(RegionDirection dir : RegionDirection.values()){
					if(dir == RegionDirection.rd_undefined){
						continue;
					}
					ArrayList<WeightLengthPair> scores = GetScores(dir);
					scores.add(new WeightLengthPair(weight, length));
				}
			}
			
			return true;		
		}
		
		/** Information for backtracking to find its way back to the start point */
		public BacktraceState GetMin(int max_path_length){
			if(!is_initialized){
				return null;
			}

			BacktraceState min = null;
			
				for(RegionDirection dir : RegionDirection.values()){
					if(dir == RegionDirection.rd_undefined){
						continue;
					}
					
					ArrayList<WeightLengthPair> scores = GetScores(dir);
					for(WeightLengthPair cur_wlp : scores){
						if(cur_wlp.length > max_path_length){
							continue;
						}
						if(min == null){
							min = new BacktraceState(cur_wlp.weight, dir, cur_wlp.length);
						}else if(cur_wlp.weight < min.weight){
							min.weight = cur_wlp.weight;
							min.dir = dir;
							min.path_length = cur_wlp.length;
						}
					}
				}
				
			return min;
		}
	}
	
	/** This represents a sub-region, passed by routes */
	class RegionRepresentation{
		public double weight;
		public SegmentInfo[] segment_infos;

		public RegionBorder left_border;
		public RegionBorder right_border;
		public RegionBorder lower_border;
		public RegionBorder upper_border;
		
		public RegionRepresentation(double weight, int num_segs){
			this.weight = weight;
			segment_infos = new SegmentInfo[num_segs];
		}
		
		public double getCongestionWeight(){
			double congestion = 0;

			if(left_border != null && left_border.GetWeight() != Double.MAX_VALUE)
				congestion += left_border.GetWeight();	
			else
				congestion += 10;
			
			if(right_border != null && right_border.GetWeight() != Double.MAX_VALUE)
				congestion += right_border.GetWeight();
			else
				congestion += 10;
			
			if(lower_border != null && lower_border.GetWeight() != Double.MAX_VALUE)
				congestion += lower_border.GetWeight();
			else
				congestion += 10;
			
			if(upper_border != null && upper_border.GetWeight() != Double.MAX_VALUE)
				congestion += upper_border.GetWeight();
			else
				congestion += 10;
			
			return congestion;
		}
		
		public RegionBorder GetRegionBorder(RegionDirection dir){
			switch(dir){
			case rd_left:
				return left_border;
			case rd_right:
				return right_border;
			case rd_down:
				return lower_border;
			case rd_up:
				return upper_border;
			default:
				return null;
			}
		}
	}
	RegionRepresentation[] regions;
	int regions_x, regions_y;
	double region_width, region_height;
	double offset_x, offset_y;
	
	/** Access to regions */
	public RegionRepresentation RegionAt(int x, int y){
		return regions[y * regions_x + x];
	}
	
	public RegionRepresentation RegionAt(int x, int y, RegionDirection dir){
		
		switch(dir){
		case rd_left:
			--x;
			break;
		case rd_right:
			++x;
			break;
		case rd_down:
			--y;
			break;
		case rd_up:
			++y;
			break;
		case rd_undefined:
			break;
		}
		
		if(IsCoordinateValid(x, y)){
			return RegionAt(x, y);
		}else{
			return null;
		}
	}
	
	/** Representation of a border between two regions */
	class RegionBorder{
		private int demand;
		private int supply;
		private int min_supply = 4;
		
		private Vector<Integer> priv_soft_passes;
		private ArrayList<Integer> soft_passes = new ArrayList<Integer>();
		private Vector<Integer> hard_passes = new Vector<Integer>();
		
		public RegionBorder(int demand, int supply, int passes){
			this.demand = demand;
			this.supply = supply;
			
			priv_soft_passes = new Vector<Integer>();
			for(int i = 0; i < passes; ++i){
				priv_soft_passes.add(null);
				hard_passes.add(null);
			}
			
		}
		
		boolean IsBlocked(){
			synchronized(this){
				return supply <= this.min_supply;
			}
		}
		
		public int GetDemand(){
			return demand;
		}
		
		public void IncDemand(){
			++ demand;
		}
		
		public boolean DecDemand(){
			if(demand > 0){
				--demand;
				return true;
			}else{
				return false;
			}
		}
		
		public int GetSupply(){
			return supply;
		}
		
		public int GetHardPos(int seg_id){
			return hard_passes.indexOf(new Integer(seg_id));
		}
		
		/** see: "Global Wiring on a Wire Routing Machine" Ravi Nair et al. */
		public double GetWeight(){
			double estd = (double)(demand + 1) / 16.0d;
			double weight = Double.MAX_VALUE;
			if(estd <= supply - min_supply){
				weight = estd * 8 / Math.pow(2, 
						((double)supply - min_supply - estd)/2);
			}else if(estd > supply - min_supply 
					&& supply - min_supply >= 3){
				weight = estd * 8 * 2 / Math.pow(2, 
						(double)supply - min_supply / 2);
			}else if(estd > supply - min_supply 
					&& supply - min_supply == 2){
				weight = estd * 8 * 3;
			}else if(estd > supply - min_supply 
					&& supply - min_supply == 1){
				weight = estd * 8 * 9;
			}else if(supply - min_supply == 0){
				weight = Double.MAX_VALUE;
			}
			
			return weight;
		}
		
		/** Route passes border temporarily */
		public boolean SoftPassBorder(int seg_id){
			synchronized(this){
				if(supply > this.min_supply){
					--supply;
					soft_passes.add(seg_id);
					return true;
				}else{
					return false;
				}
			}
		}
		
		/** Set a privileged position in this border */
		public boolean FromSoftToPriv(int seg_id, int priv_pos){
			synchronized(this){
				if(soft_passes.contains(seg_id)){
					soft_passes.remove(soft_passes.indexOf(seg_id));
					
					int delta = 1;
					int length = priv_soft_passes.size();
					int lower = priv_pos - delta;
					int higher = priv_pos + delta;
					
					while(((lower >= 0) && (lower < length))
						|| ((higher >= 0) && (higher < length))){
						
						if(((lower >= 0) && (lower < length)) && priv_soft_passes.get(lower) == null){
							priv_soft_passes.set(lower, seg_id);
							return true;
						}else if(((higher >= 0) && (higher < length)) && priv_soft_passes.get(higher) == null){
							priv_soft_passes.set(higher, seg_id);
							return true;
						}
						
						++delta;
						lower = priv_pos - delta;
						higher = priv_pos + delta;
					}
					
					// nothing free (should not happen)
					soft_passes.add(seg_id);
					return false;
				}else{
					return false;
				}
			}
		}
		
		
		public boolean SoftPassBorder(int seg_id, int priv_pos){
			synchronized(this){
				if(supply > this.min_supply){
					--supply;
					
					if(priv_soft_passes.get(priv_pos) == null){
						priv_soft_passes.set(priv_pos, seg_id);
						return true;
					}else{
						int delta = 1;
						int length = priv_soft_passes.size();
						int lower = priv_pos - delta;
						int higher = priv_pos + delta;
						
						while(((lower >= 0) && (lower < length))
							|| ((higher >= 0) && (higher < length))){
							
							if(((lower >= 0) && (lower < length)) && priv_soft_passes.get(lower) == null){
								priv_soft_passes.set(lower, seg_id);
								return true;
							}else if(((higher >= 0) && (higher < length)) && priv_soft_passes.get(higher) == null){
								priv_soft_passes.set(higher, seg_id);
								return true;
							}
							
							++delta;
							lower = priv_pos - delta;
							higher = priv_pos + delta;
						}
						
						// nothing free (should not happen)
						soft_passes.add(seg_id);
						return true;
					}
				}else{
					return false;
				}
			}
		}
		
		/** Remove a route, which passed this border earlier */
		public boolean RevertSoftPass(int seg_id){
			synchronized(this){
				if(soft_passes.contains(seg_id)){
					++supply;
					soft_passes.remove(soft_passes.indexOf(seg_id));
					return true;
				}else if(priv_soft_passes.contains(seg_id)){
					++supply;
					int index = priv_soft_passes.indexOf(seg_id);
					priv_soft_passes.set(index, null);
					
					return true;
				}else{
					return false;
				}
			}
		}
		
		/** Pay attention using these two functions because of concurrency.
		 *  Reverting a pass, set in this function will only be done, if 
		 *  detailed will not be able to route this wire.
		 */
		public Vector<Integer> HardPassBorderV2(){
			HardPassBorder();
			return new Vector<Integer>(hard_passes);
		}
		
		public Vector<Integer> HardPassBorderV2(Vector<Integer> passes){
			for(int i = 0; i < passes.size(); ++i){
				Integer id = passes.get(i);
				if(id != null && soft_passes.contains(id)){
					FromSoftToPriv(id, i);
				}
			}
			
			HardPassBorder();
			return new Vector<Integer>(hard_passes);
		}
		
		public void HardPassBorder(){
			synchronized(this){
				/** first sort passes */
				Collections.sort(soft_passes);
				
				/** insert previliged passes */
				for(int priv_i = 0; priv_i < priv_soft_passes.size(); ++priv_i){
	
					if(priv_soft_passes.get(priv_i) == null){
						continue;
					}
					
					int id = priv_soft_passes.get(priv_i);
					
					if(hard_passes.get(priv_i) == null){
						hard_passes.set(priv_i, id);
					}else{
						int delta = 1;
						int length = hard_passes.size();
						int lower = priv_i - delta;
						int higher = priv_i + delta;
						
						while(((lower >= 0) && (lower < length))
							|| ((higher >= 0) && (higher < length))){
							
							if(((lower >= 0) && (lower < length)) && hard_passes.get(lower) == null){
								hard_passes.set(lower, id);
								break;
							}else if(((higher >= 0) && (higher < length)) && hard_passes.get(higher) == null){
								hard_passes.set(higher, id);
								break;
							}
							
							++delta;
							lower = priv_i - delta;
							higher = priv_i + delta;
						}
					}
				}
				
				/** then insert soft passes to hard passes */
				int hi = this.min_supply / 2;
				for(int si = 0; si < soft_passes.size(); ++ si){
					for(; hi < hard_passes.size() && hard_passes.get(hi) != null && hard_passes.get(hi) >= 0; ++hi){
					};
					if(hi < hard_passes.size()){
						hard_passes.set(hi, soft_passes.get(si));
					}else{
						hard_passes.add(soft_passes.get(si));
					}
	
					hi++;
				}
					
				/** and at the end reset soft_passes */
				soft_passes = new ArrayList<Integer>();
				int passes = priv_soft_passes.size();
				priv_soft_passes = new Vector<Integer>();
				for(int i = 0; i < passes; ++i){
					priv_soft_passes.add(null);
				}
			}
		}
			
		/** Reverts a pass through this border, if the detailed router could 
		 * not route the segment.
		 */
		public boolean RevertHardPass(int seg_id){
			synchronized(this){
				int index = hard_passes.indexOf(new Integer(seg_id));
				if(index >= 0){
					++supply;
					hard_passes.set(index, null);
					return true;
				}else{
					return false;
				}
			}			
		}
		
		/** Removes segments failed to route in the global router. */
		public void CleanBorder(){
			synchronized(this){
				for(Integer i : not_routed){
					if(soft_passes.contains(i)){
						soft_passes.remove(soft_passes.indexOf(i));
					}
					if(priv_soft_passes.contains(i)){
						priv_soft_passes.set(priv_soft_passes.indexOf(i), null);
					}
				}
			}
		}
	}
	int max_supply_vertical, max_supply_horizontal;
	
	/** Checks and s to assure a correct coordinate access */
	public boolean IsCoordinateValid(int x, int y){
		return 0 <= x && x < regions_x && 0 <= y && y < regions_y;
	}

	public int ConvertX(double x){
		return (int)(x / region_width);
	}
	
	public int ConvertY(double y){
		return (int)(y / region_height);
	}
	
	
	public RegionDirection DirFromTo(Vector2i from, Vector2i to){
		int x = to.x - from.x;
		int y = to.y - from.y;
		
		RegionDirection dir;
		if(x == 1 && y == 0){
			dir = RegionDirection.rd_right;
		}else if(x == -1 && y == 0){
			dir = RegionDirection.rd_left;
		}else if(x == 0 && y == 1){
			dir = RegionDirection.rd_up;
		}else if(x == 0 && y == -1){
			dir = RegionDirection.rd_down;
		}else{
			dir = RegionDirection.rd_undefined;
		}
		
		return dir;
	}
	
	public int GetNeighborX(int x, RegionDirection dir){
		switch(dir){
		case rd_left:
			--x;
			break;
		case rd_right:
			++x;
			break;
		}
		
		return x;
	}
	
	public int GetNeighborY(int y, RegionDirection dir){
		switch(dir){
		case rd_down:
			--y;
			break;
		case rd_up:
			++y;
			break;
		}
		
		return y;
	}
	
	public Vector2i GetNeighborPos(int x, int y, RegionDirection dir){

		switch(dir){
		case rd_left:
			--x;
			break;
		case rd_right:
			++x;
			break;
		case rd_down:
			--y;
			break;
		case rd_up:
			++y;
			break;
		case rd_undefined:
			break;
		}
		
		return new Vector2i(x, y);
	}
	
	public Vector2i GetNeighborPos(Vector2i curr_pos, RegionDirection dir){
		return GetNeighborPos(curr_pos.x, curr_pos.y, dir);
	}
	
	static public RegionDirection GetOppositeDir(RegionDirection dir){
		
		switch(dir){
		case rd_left:
			return RegionDirection.rd_right;
		case rd_right:
			return RegionDirection.rd_left;
		case rd_down:
			return RegionDirection.rd_up;
		case rd_up:
			return RegionDirection.rd_down;
		default:
			return RegionDirection.rd_undefined;
		}
	}

	/** Structure to implement job-stealing. This queue offers not yet routed segments. */
	private ArrayList<JobMessage> segment_jobs;
	
	public synchronized JobMessage PollSegmentJob(){
			if(segment_jobs.isEmpty()){
				return null;
			}else{
				JobMessage job = segment_jobs.get(0);
				segment_jobs.remove(0);
				return job;
			}
	}
	
	/** Interface between data from electric and global router */
	class SegmentRepresentation{
		public Vector2i start;
		public Vector2i end;
		public double d_start_x;
		public double d_start_y;
		public double d_end_x;
		public double d_end_y;
		SegmentRepresentation(Vector2i start, Vector2i end){
			this.start = new Vector2i(start.x, start.y);
			this.end = new Vector2i(end.x, end.y);
		}
	}
	SegmentRepresentation[] segments;
	ArrayList<RoutingSegment> electric_segments;
	
	/** job stealing for demand estimation */
	Integer demand_seg_index;
	
	public synchronized SegmentRepresentation PollDemandEstimationJob(){
		SegmentRepresentation ret = null;
			if(to_reroute.isEmpty()){
				if(demand_seg_index < segments.length){
					ret = segments[demand_seg_index];
					++demand_seg_index;
				}
			}else{
				if(demand_seg_index < to_reroute.size()){
					ret = segments[to_reroute.get(demand_seg_index)];
					++demand_seg_index;
				}
			}
		
		return ret;
	}
	
	/** job stealing for removing failed segments */
	Integer remove_failed_index;
	
	public synchronized int PollFailedId(){
			if(remove_failed_index < to_reroute.size()){
				int ret = to_reroute.get(remove_failed_index);
				++remove_failed_index;
				return ret;
			}else{
				return -1;
			}
		
	}
	
	public synchronized void ResetOutputRoutes(){
			this.output_coarse_routes = new HashMap<Integer, RouteToStitch>();
	}
	
	/** backtraces from global router workers */
	class SegBtPair{
		public int seg_id;
		public Vector<RegionDirection> backtrace;
	}
	ArrayList<SegBtPair> seg_backtraces;
	
	public synchronized void OfferBacktrace(Vector<RegionDirection> bt, int seg_id){
		SegBtPair pair = new SegBtPair();
		pair.backtrace = bt;
		pair.seg_id = seg_id;

		seg_backtraces.add(pair);
	}
	
	public synchronized SegBtPair PollBacktrace(){
		
		SegBtPair ret;

		if(seg_backtraces.isEmpty()){
			ret = new SegBtPair();
			ret.seg_id = -1;
			ret.backtrace = null;
		}else{
			ret = seg_backtraces.get(0);
			seg_backtraces.remove(0);
		}

		return ret;
	}
	
	/** routes which could not be routed */
	public ArrayList<Integer> not_routed;
	
	public synchronized void OfferUnrouted(ArrayList<Integer> list){
			not_routed.addAll(list);
	}
	
	/** synchronization of second phase */
	Integer regions_in_work;
	public synchronized RegionRepresentation PollNextRegionToSort(){
		RegionRepresentation ret = null;

		if(regions_in_work < regions_x * regions_y){
			ret =  regions[regions_in_work];
			++regions_in_work;
		}
		
		return ret;
	}
	
	/** list with id's of segments which need to be rerouted (feedback from detailed)*/
	ArrayList<Integer> to_reroute;

	/** Transforms Coordinates back after the whole routing process is finished. */
	public void Retransform(List<Coordinate> list){
		Coordinate co;
		for (Iterator<Coordinate> i = list.iterator(); i.hasNext();) {
			co = i.next();
			co.x += offset_x;
			co.y += offset_y;
		}
	}
	
	public GlobalRouterV3(Rectangle2D rect, int num_regions, List<RoutingSegment> segmentsToRoute, int num_threads, double tileSize){
		
		this.tileSize = tileSize;
		this.num_threads = num_threads;
		barrier = new CyclicBarrier(num_threads);
		
		/* calculate region width and height */
		double w = rect.getWidth() / num_regions;
		double h = rect.getHeight() / num_regions;
		int num_tiles_w_in_region = (int)Math.ceil(w / this.tileSize);
		int num_tiles_h_in_region = (int)Math.ceil(h / this.tileSize);
		
		this.region_width = num_tiles_w_in_region * this.tileSize;
		this.region_height = num_tiles_h_in_region * this.tileSize;
		this.regions_x = num_regions;
		this.regions_y = num_regions;
		this.offset_x = rect.getMinX() - Math.signum(rect.getMinX()) * Math.abs(rect.getMinX()) % 0.01;
		this.offset_y = rect.getMinY() - Math.signum(rect.getMinY()) * Math.abs(rect.getMinY()) % 0.01;
		
		regions = new RegionRepresentation[regions_x * regions_y];
		electric_segments = new ArrayList<RoutingSegment>(segmentsToRoute);
		this.segments = new SegmentRepresentation[segmentsToRoute.size()];
		
		Iterator<RoutingSegment> it = segmentsToRoute.iterator();
		for(int i = 0; i < segments.length; ++i){
			RoutingSegment r_seg = it.next();
			double start_x = r_seg.getStartEnd().getLocation().getX() - offset_x;
			double start_y = r_seg.getStartEnd().getLocation().getY() - offset_y;
			double end_x = r_seg.getFinishEnd().getLocation().getX() - offset_x;
			double end_y = r_seg.getFinishEnd().getLocation().getY() - offset_y;
			Vector2i i_start = new Vector2i((int)(start_x / this.region_width),
					(int)(start_y / this.region_height));
			Vector2i i_end = new Vector2i((int)(end_x / this.region_width), 
					(int)(end_y / this.region_height));
			if(i_start.x < 0){
				i_start.x = 0;
			}
			if(i_start.x >= regions_x){
				i_start.x = regions_x - 1;
			}
			if(i_start.y < 0){
				i_start.y = 0;
			}
			if(i_start.y >= regions_y){
				i_start.y = regions_y - 1;
			}
			if(i_end.x < 0){
				i_end.x = 0;
			}
			if(i_end.x >= regions_x){
				i_end.x = regions_x - 1;
			}
			if(i_end.y < 0){
				i_end.y = 0;
			}
			if(i_end.y >= regions_y){
				i_end.y = regions_y - 1;
			}
			this.segments[i] = new SegmentRepresentation(i_start, i_end);
			this.segments[i].d_start_x = start_x;
			this.segments[i].d_start_y = start_y;
			this.segments[i].d_end_x = end_x;
			this.segments[i].d_end_y = end_y;
		}
		
		/** setup RegionRepresentations  */
		for(int i = 0; i < this.regions_x * this.regions_y; ++i){
			regions[i] = new RegionRepresentation(0d, segments.length);
		}
		
		/** create borders */
		this.max_supply_horizontal = (int)(region_width / tileSize) - 1;
		int supply_x = this.max_supply_horizontal;
		this.max_supply_vertical = (int)(region_height / tileSize) - 1;
		int supply_y = this.max_supply_vertical;
		for(int x = 0; x < this.regions_x; ++x){
			for(int y = 0; y < this.regions_y; ++y){
				if(x > 0){
					RegionAt(x, y).left_border = new RegionBorder(0, supply_y, max_supply_vertical);
				}
				if(y > 0){
					RegionAt(x, y).lower_border = new RegionBorder(0, supply_x, max_supply_horizontal);
				}
			}
		}
		/** set upper and right border */
		for(int x = 0; x < this.regions_x; ++x){
			for(int y = 0; y < this.regions_y; ++y){
				if(x < this.regions_x - 1){
					RegionAt(x, y).right_border = RegionAt(x, y, RegionDirection.rd_right).left_border;
				}
				if(y < this.regions_y - 1){
					RegionAt(x, y).upper_border = RegionAt(x, y, RegionDirection.rd_up).lower_border;
				}
			}
		}
		
		/** initialize for first run of global router */
		to_reroute = new ArrayList<Integer>();
		segment_jobs = new ArrayList<JobMessage>();
		demand_seg_index = 0;
		regions_in_work = 0;
		not_routed = new ArrayList<Integer>();
		remove_failed_index = 0;
		this.seg_backtraces = new ArrayList<SegBtPair>();

		for(int i = 0; i < segments.length; ++i){
			JobMessage job = new JobMessage(i, segments[i].start, 0, 0.0d, RegionDirection.rd_undefined);
			segment_jobs.add(job);
		}

		this.output_coarse_routes = new HashMap<Integer, RouteToStitch>();
		this.output_regions = new RegionToRoute[regions_x * regions_y];
		for(int x = 0; x < regions_x; ++x){
			for(int y = 0; y < regions_y; ++y){
				Rectangle2D rec = new Rectangle2D.Double();
				rec.setFrameFromDiagonal(x * this.region_width, y * this.region_height,
						(x + 1) * this.region_width, (y + 1) * this.region_height);
				output_regions[y * regions_x + x] = new RegionToRoute(rec);
			}	
		}
	}
	
	/** List of routes, which could be completed by the detailed router */
	List<Integer> wiried_routes = new LinkedList<Integer>();
	
	public void Reinitialize(List<Integer> failed_to_route){
		to_reroute = new ArrayList<Integer>(failed_to_route);
		if(max_detour < Math.max(regions_x, regions_y))
		{
			max_detour += 2;
		}	
		
		//mark rest of output_coarse_routes that are not in failed_to_route as wiried
		for(RouteToStitch rts : output_coarse_routes.values()){
			if(!failed_to_route.contains(rts.id)){
				wiried_routes.add(rts.id);
			}
		}
		
		to_reroute.addAll(not_routed);
		not_routed = new ArrayList<Integer>();
		segment_jobs = new ArrayList<JobMessage>();
		demand_seg_index = 0;
		regions_in_work = 0;
		remove_failed_index = 0;
		this.seg_backtraces = new ArrayList<SegBtPair>();

		for(int i = 0; i < to_reroute.size(); ++i){
			int seg_id = to_reroute.get(i);
			JobMessage job = new JobMessage(seg_id, segments[seg_id].start, 0, 0.0d, RegionDirection.rd_undefined);
			segment_jobs.add(job);
		}

		this.output_regions = new RegionToRoute[regions_x * regions_y];
		for(int x = 0; x < regions_x; ++x){
			for(int y = 0; y < regions_y; ++y){
				Rectangle2D rec = new Rectangle2D.Double();
				rec.setFrameFromDiagonal(x * this.region_width, y * this.region_height,
						(x + 1) * this.region_width, (y + 1) * this.region_height);
				output_regions[y * regions_x + x] = new RegionToRoute(rec);
			}	
		}
		
		/** reset demand estimate */
		for(int x = 0; x < this.regions_x; ++x){
			for(int y = 0; y < this.regions_y; ++y){
				if(x > 0){
					RegionAt(x, y).left_border.demand = 0;
				}
				if(y > 0){
					RegionAt(x, y).lower_border.demand = 0;
				}
			}
		}
	}
	
	public void StartGlobalRouting(){
		Thread[] slave_threads;
		GlobalRouterThreadV3[] slaves;
		slave_threads = new Thread[num_threads - 1];
		slaves = new GlobalRouterThreadV3[num_threads];
		
		/** start parallel global routing */
		for(int i = 0; i < num_threads - 1; ++i){
			slaves[i] = new GlobalRouterThreadV3(this, i);
			slave_threads[i] = new Thread(slaves[i]);
			slave_threads[i].setName("GlobalRouterSlave_" + i);
			slave_threads[i].start();
		}
		slaves[num_threads - 1] = new GlobalRouterThreadV3(this, 
				num_threads - 1);
		slaves[num_threads - 1].run();
	}
}


/** routing segments representation declarations */
/** Position in coordinate system of global router */
class Vector2i{
	public int x;
	public int y;
	
	public Vector2i(int x, int y){
		this.x = x;
		this.y = y;
	}
	public Vector2i(Vector2i p){
		this.x = p.x;
		this.y = p.y;
	}
	 public boolean equals(Vector2i v){
		 return (v.x == x && v.y == y);
	 }
}

/** region declaration */
enum RegionDirection{
	rd_left, rd_right, rd_up, rd_down, rd_undefined
}

/** job queues declaration */
class JobMessage{
	public int seg_id = Integer.MIN_VALUE;
	public Vector2i position = null;
	public int step_num = Integer.MIN_VALUE;
	public double weight = Double.MIN_VALUE;
	public RegionDirection min_dir = RegionDirection.rd_undefined;
	
	public JobMessage(int seg_id, Vector2i pos, int min_length, double weight, RegionDirection min_dir){
		position = new Vector2i(pos.x, pos.y);
		this.seg_id = seg_id;
		this.step_num = min_length;
		this.weight = weight;
		this.min_dir = min_dir;
	}
	
	public JobMessage(int seg_id, Vector2i pos, int step_num){
		position = new Vector2i(pos.x, pos.y);
		this.seg_id = seg_id;
		this.step_num = step_num;
	}
	
	public JobMessage(int seg_id, Vector2i pos, double weight, int step_num){
		position = new Vector2i(pos.x, pos.y);
		this.seg_id = seg_id;
		this.weight = weight;
		this.step_num = step_num;
	}
}

/** SegmentInfoHelper classes */
class WeightLengthPair{
	public double weight;
	public int length;
	public WeightLengthPair(double w, int l){
		weight = w;
		length = l;
	}
}

/** Structure holding information for backtracing procedure */
class BacktraceState{
	public double weight;
	public RegionDirection dir;
	public int path_length;
	public BacktraceState(double w, RegionDirection d, int l){
		weight = w;
		dir = d;
		path_length = l;
	}
}

/** A segment patch, inside of a region. */
class SegPart{
	public List<Coordinate> segment_part;
	public int id;
}
