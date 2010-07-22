/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GlobalRouterThreadV3.java
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.BrokenBarrierException;

import com.sun.electric.tool.routing.experimentalLeeMoore2.GlobalRouterV3.*;
import com.sun.electric.tool.routing.experimentalLeeMoore2.RoutingFrameLeeMoore.Coordinate;
import com.sun.electric.tool.routing.experimentalLeeMoore2.RoutingFrameLeeMoore.ManhattenAlignment;

public class GlobalRouterThreadV3 implements Runnable {

	ArrayList<Integer> non_routable = new ArrayList<Integer>();
	
	/** Id of this worker */
	int slave_id;
	
	/** internal job queues */
	ArrayList<JobMessage> internal_fwd_jobs = new ArrayList<JobMessage>();
	ArrayList<JobMessage> internal_bckwd_jobs = new ArrayList<JobMessage>();
	
	/** temporary result of backtracing */
	Vector<RegionDirection> backtrace;
	
	/** master */
	GlobalRouterV3 rm;
	
	public GlobalRouterThreadV3(GlobalRouterV3 gr, int slave_id){
		this.rm = gr;
		this.slave_id = slave_id;
	}

	/** if sink was found, offers backward job to queue and returns true */
	private JobMessage DoForwardJob(JobMessage entry){
			
		/** do job */
		RegionRepresentation region = rm.RegionAt(entry.position.x,
				entry.position.y); 
		SegmentInfo seg_info = region.segment_infos[entry.seg_id];

		JobMessage ret = null;
		// check if region has to be considered and refresh data
		if(seg_info.RefreshSegment(entry.step_num, entry.weight, entry.min_dir)){
		
			/** check if sink was reached */
			Vector2i end_point = rm.segments[entry.seg_id].end;
			if(entry.position.x == end_point.x && entry.position.y == end_point.y){
				JobMessage bwd_job = new JobMessage(entry.seg_id, entry.position, entry.weight, rm.max_detour);
				ret = bwd_job;
			}
			
			/** offer neighbor jobs to queue */
			for(RegionDirection dir : RegionDirection.values()){
				if(dir == RegionDirection.rd_undefined){
					continue;
				}
				if(!rm.IsCoordinateValid(rm.GetNeighborX(
						entry.position.x, dir), rm.GetNeighborY(
						entry.position.y, dir))){
					continue;
				}
				
				/** create job */
				double weight = entry.weight;
				
				boolean from_hor = entry.min_dir == RegionDirection.rd_left || entry.min_dir == RegionDirection.rd_right;
				boolean from_vert = entry.min_dir == RegionDirection.rd_up || entry.min_dir == RegionDirection.rd_down;
				boolean to_hor = dir == RegionDirection.rd_left || dir == RegionDirection.rd_right;
				boolean to_vert = dir == RegionDirection.rd_up || dir == RegionDirection.rd_down;
				if((from_hor && to_vert) || (from_vert && to_hor)){
					weight += rm.via_cost;
				}
				RegionBorder border = region.GetRegionBorder(dir);
//				double bw = border.GetWeight();
				
				double congestion_w = rm.RegionAt(rm.GetNeighborX(
						entry.position.x, dir), rm.GetNeighborY(
								entry.position.y, dir)).getCongestionWeight();
				
				// if weight is equal to Double.Max there is a blockage
				if(border.IsBlocked()){
					continue;
				}
//				weight += bw;
//				weight = bw;
				weight = congestion_w;
				weight = Math.max(entry.weight, weight);
				SegmentInfo neighbor_info = rm.RegionAt(entry.position.x, 
						entry.position.y, dir).segment_infos[entry.seg_id];
				int detour_inc = seg_info.is_on_min_path 
						&& neighbor_info.is_on_min_path ? 0 : 1;
				
				JobMessage job_to_send = new JobMessage(entry.seg_id,
						rm.GetNeighborPos(entry.position, dir),
						entry.step_num + detour_inc, weight, GlobalRouterV3.GetOppositeDir(dir));
				
				/** send job to region */
				internal_fwd_jobs.add(job_to_send);
			}
		}
		
		return ret;
	}
	
	/** returns false, if could not continue because of blockage */
	private boolean DoBackwardJob(JobMessage entry){
		
		RegionRepresentation region = rm.RegionAt(entry.position.x,
				entry.position.y); 
		SegmentInfo seg_info = region.segment_infos[entry.seg_id];
		
		/** mark region as part of backtrace */
		seg_info.was_part_of_bt = true;
		
		/** check if source was reached */
		Vector2i start_point = rm.segments[entry.seg_id].start;
		if(entry.position.x == start_point.x && entry.position.y == start_point.y){
			/** source reached */
			return true;
		}else{
			/** search for minimum */
			BacktraceState min = seg_info.GetMin(entry.step_num);
			
			/** try to pass border, but if blockage occurred return false */
			RegionBorder passed_border = rm.RegionAt(entry.position.x,
					entry.position.y).GetRegionBorder(min.dir);
			if(!passed_border.SoftPassBorder(entry.seg_id)){
				return false;
			}
			
			/** set backtrace */
			RegionDirection dir_to_get_here = GlobalRouterV3.GetOppositeDir(min.dir);
			backtrace.add(0, dir_to_get_here);
			
			/** delegate new job to neighbor */
			JobMessage job_to_send = new JobMessage(entry.seg_id,
					rm.GetNeighborPos(entry.position, min.dir), min.path_length);
			
			/** send job to region */
			internal_bckwd_jobs.add(job_to_send);
			
			return true;
		}
	}
	
	/** if backward propagation had to be canceled, last position visited is returned */
	private Vector2i BackwardPropagation(){
		while(!internal_bckwd_jobs.isEmpty()){
			JobMessage internal_bwd_job = internal_bckwd_jobs.get(0);
			internal_bckwd_jobs.remove(0);
			if(!DoBackwardJob(internal_bwd_job)){
				return internal_bwd_job.position;
			}
		}
		
		return null;
	}
	
	/** Try to put shortest path and create forward jobs. */
	private JobMessage PutShortestPath(List<RegionDirection> dirs, Vector2i start, int seg_id){
		Vector2i pos = new Vector2i(start);
		Iterator<RegionDirection> it = dirs.iterator();
		double w = 0.0d;
		
		// set start point
		RegionRepresentation region = rm.RegionAt(pos.x, pos.y); 
		SegmentInfo seg_info = region.segment_infos[seg_id];
		seg_info.is_on_min_path = true;
		if(!it.hasNext()){
			// send jobs to neighbors
			for(RegionDirection rd : RegionDirection.values()){
				if(rd == RegionDirection.rd_undefined){
					continue;
				}
				if(!rm.IsCoordinateValid(rm.GetNeighborX(
						pos.x, rd), rm.GetNeighborY(pos.y, rd))){
					continue;
				}
				RegionBorder b = region.GetRegionBorder(rd);
				double next_w = b.GetWeight();
				if(b.IsBlocked()){	//skip this direction
					continue;
				}else{	//send job to this direction
					next_w += w;
					
	
					JobMessage job_to_send = new JobMessage(seg_id,
							rm.GetNeighborPos(pos, rd), 1,
							next_w, GlobalRouterV3.GetOppositeDir(rd));
					
					/** send job to region */
					internal_fwd_jobs.add(job_to_send);
				}
				
			}
		}
		
		while(it.hasNext()){
			// send jobs to neighbors
			for(RegionDirection rd : RegionDirection.values()){
				if(rd == RegionDirection.rd_undefined){
					continue;
				}
				if(!rm.IsCoordinateValid(rm.GetNeighborX(
						pos.x, rd), rm.GetNeighborY(pos.y, rd))){
					continue;
				}
				RegionBorder b = region.GetRegionBorder(rd);
				double next_w = b.GetWeight();
				if(b.IsBlocked()){	//skip this direction
					continue;
				}else{	//send job to this direction
					next_w += w;
					
	
					JobMessage job_to_send = new JobMessage(seg_id,
							rm.GetNeighborPos(pos, rd), 1,
							next_w, GlobalRouterV3.GetOppositeDir(rd));
					
					/** send job to region */
					internal_fwd_jobs.add(job_to_send);
				}
			}

			//get direction and border to next region
			RegionDirection cur_dir = it.next();
			
			RegionBorder border = region.GetRegionBorder(cur_dir);
			
			//calculate new weight
			double bw = border.GetWeight();
			// blockages could have been occurred meanwhile
			if(border.IsBlocked()){	//cancel computation, because of blockage
				return null;
			}
			
			w += bw;
			
			// fetch next region and seg_info
			pos = rm.GetNeighborPos(pos, cur_dir);
			region = rm.RegionAt(pos.x, pos.y);
			seg_info = region.segment_infos[seg_id];
			
			// set new weight
			seg_info.is_on_min_path = true;
			
		}
		
		return new JobMessage(seg_id, pos, w, rm.max_detour);
	}
	
	/** Polls jobs from master and tries to route them */
	private void FindRoutes(){
		
		/** put source to internal job queue */
		JobMessage job = rm.PollSegmentJob();
		while(job != null){
			
			/** first re-initialize segment_infos */
			for(int i = 0; i < rm.regions_x * rm.regions_y; ++i){
				rm.regions[i].segment_infos[job.seg_id] = rm.new SegmentInfo();
			}
			
			Vector2i source = new Vector2i(rm.segments[job.seg_id].start);
			Vector2i sink = new Vector2i(rm.segments[job.seg_id].end);
			
			/** create shortest path */
			GlobalRouterPathFinder path_finder = new GlobalRouterPathFinder(rm);
			List<RegionDirection> rd_path = path_finder.FindShortestPath(job.seg_id);
			JobMessage best_job = null;
			if(rd_path != null){	//could find shortest path
				best_job = PutShortestPath(rd_path, source, job.seg_id);
			}
			
			if(rd_path == null || best_job == null){	
				// could not find shortest path so drop this segment
				
				/** remove demand estimate */
				DemandTemplateHandler dth = new DemandTemplateHandler(this);
				dth.DecrementDemandEstimate(source, sink);
				
				/** put segment id to non_routable */
				non_routable.add(job.seg_id);
				
				/** continue with next segment */
			}else{	//min_path found and set
				/** do internal jobs */
				while(!internal_fwd_jobs.isEmpty()){
					JobMessage internal_job = internal_fwd_jobs.get(0);
					internal_fwd_jobs.remove(0);
					int seg_id = internal_job.seg_id;
					
					
					JobMessage bwd_job = DoForwardJob(internal_job);
					
					if(bwd_job != null){
						
						if(bwd_job.weight < best_job.weight){
							best_job = bwd_job;
						}
					}
					if(internal_job.step_num > rm.max_detour || internal_fwd_jobs.isEmpty()){
						
						internal_bckwd_jobs.add(best_job);
						backtrace = new Vector<RegionDirection>();
						Vector2i error_pos = BackwardPropagation();
						if(error_pos != null){
							// could not complete backtrace, because blockage occurred meanwhile
							// need to revert changes
							Vector2i it = new Vector2i(error_pos);
							for(int i = 0; i < backtrace.size(); ++i){
								RegionDirection curr_dir = backtrace.get(i);
								
								it = rm.GetNeighborPos(it, curr_dir);
							}
							
							// add id to non_routables
							non_routable.add(job.seg_id);
						}else{
							
							/** backtrace was valid, so offer it to master */
							
							//but first reset start and end passes
							if(backtrace.size() > 0){
								RegionDirection start_dir = backtrace.get(0);
								Vector2i start = rm.segments[seg_id].start;
								if(start_dir == RegionDirection.rd_left || start_dir == RegionDirection.rd_right){
									double y = rm.segments[seg_id].d_start_y % rm.region_height;
									int sy_pos = (int)Math.floor((y - (rm.tileSize / 2d)) / rm.tileSize);
									rm.RegionAt(start.x, start.y).GetRegionBorder(start_dir).FromSoftToPriv(seg_id, sy_pos);
									
								}else if(start_dir == RegionDirection.rd_up || start_dir == RegionDirection.rd_down){
									double x = rm.segments[seg_id].d_start_x % rm.region_width;
									int sx_pos = (int)Math.floor((x - (rm.tileSize / 2d)) / rm.tileSize);
									rm.RegionAt(start.x, start.y).GetRegionBorder(start_dir).FromSoftToPriv(seg_id, sx_pos);
								}
								start_dir = null;
								start = null;
								
								RegionDirection end_dir = GlobalRouterV3.GetOppositeDir(backtrace.get(backtrace.size() - 1));
								Vector2i end = rm.segments[seg_id].end;
								if(end_dir == RegionDirection.rd_left || end_dir == RegionDirection.rd_right){
									double y = rm.segments[seg_id].d_end_y % rm.region_height;
									int ey_pos = (int)Math.floor((y - (rm.tileSize / 2d)) / rm.tileSize);
									rm.RegionAt(end.x, end.y).GetRegionBorder(end_dir).FromSoftToPriv(seg_id, ey_pos);
									
								}else if(end_dir == RegionDirection.rd_up || end_dir == RegionDirection.rd_down){
									double x = rm.segments[seg_id].d_end_x % rm.region_width;
									int ex_pos = (int)Math.floor((x - (rm.tileSize / 2d)) / rm.tileSize);
									rm.RegionAt(end.x, end.y).GetRegionBorder(end_dir).FromSoftToPriv(seg_id, ex_pos);
								}
							}
							
							// finally offer backtrace to master
							rm.OfferBacktrace(backtrace, seg_id);
						}
						
						/** in any case remove estimated demand and continue with next job */
						DemandTemplateHandler dth = new DemandTemplateHandler(this);
						dth.DecrementDemandEstimate(source, sink);
						
						break;
					}
				}
			}
			internal_fwd_jobs.clear();
			
			job = rm.PollSegmentJob();
		}
	}
	
	private void SortPassesV2(){
		for(int y = slave_id; y < rm.regions_y; y += rm.num_threads){
			Vector<Integer> passes = rm.RegionAt(0, y).GetRegionBorder(RegionDirection.rd_right).HardPassBorderV2();
			for(int x = 1; x < rm.regions_x - 1; ++x){
				passes = rm.RegionAt(x, y).GetRegionBorder(RegionDirection.rd_right).HardPassBorderV2(passes);
			}
		}
		
		for(int x = slave_id; x < rm.regions_x; x += rm.num_threads){
			Vector<Integer> passes = rm.RegionAt(x, 0).GetRegionBorder(RegionDirection.rd_up).HardPassBorderV2();
			for(int y = 1; y < rm.regions_y - 1; ++y){
				passes = rm.RegionAt(x, y).GetRegionBorder(RegionDirection.rd_up).HardPassBorderV2(passes);
			}
		}
	}
	
	int debug_bt_skips = 0;
	private void CreateOutput(){
		
		/** poll backtraces */
		SegBtPair sbtp = rm.PollBacktrace();
		Vector<RegionDirection> bt = sbtp.backtrace;
		int seg_id = sbtp.seg_id;
		
		while(bt != null){
			if(!rm.not_routed.contains(new Integer(sbtp.seg_id))){

				Vector2i current_region_pos = new Vector2i(rm.segments[seg_id].start);
				Vector2i next_region_pos;

				int start_layer = rm.electric_segments.get(seg_id).getStartLayers().get(0).getMetalNumber();
				int finish_layer = rm.electric_segments.get(seg_id).getFinishLayers().get(0).getMetalNumber();
				List<SegPart> coarse_route = new ArrayList<SegPart>();
				ArrayList<Vector2i> visited_routes = new ArrayList<Vector2i>();
				visited_routes.add(new Vector2i(rm.segments[seg_id].start));
				
				/** set startpoint */
				Coordinate last_coordinate = new Coordinate(rm.segments[seg_id].d_start_x, 
						rm.segments[seg_id].d_start_y, start_layer);
				Coordinate cur_coordinate;
				
				/** iterate through trace */
				Iterator<RegionDirection> it = bt.iterator();
				while(it.hasNext()){
					RegionDirection direction = it.next();
					
					next_region_pos = rm.GetNeighborPos(current_region_pos, direction);
					visited_routes.add(new Vector2i(next_region_pos));
					RegionBorder border = rm.RegionAt(current_region_pos.x,
							current_region_pos.y).GetRegionBorder(direction);
					
					/** calculate position in border */
					int pos_in_list = border.GetHardPos(seg_id);
					int pos_in_vertical_border = pos_in_list;
					int pos_in_horizontal_border = pos_in_list;
					
					double offset_hor = (double) pos_in_horizontal_border * rm.tileSize + rm.tileSize;
					double offset_vert = (double) pos_in_vertical_border * rm.tileSize + rm.tileSize;
					double cur_seg_pos_x = current_region_pos.x * rm.region_width;
					double cur_seg_pos_y = current_region_pos.y * rm.region_height;
					ManhattenAlignment cur_seg_pos_align = ManhattenAlignment.ma_undefined;
					
					switch(direction){
					case rd_left:
						cur_seg_pos_x += 0.0d;
						cur_seg_pos_y += offset_vert;
						cur_seg_pos_align = ManhattenAlignment.ma_horizontal;
						break;
					case rd_right:
						cur_seg_pos_x += rm.region_width;
						cur_seg_pos_y += offset_vert;
						cur_seg_pos_align = ManhattenAlignment.ma_horizontal;
						break;
					case rd_up:
						cur_seg_pos_x += offset_hor;
						cur_seg_pos_y += rm.region_height;
						cur_seg_pos_align = ManhattenAlignment.ma_vertical;
						break;
					case rd_down:
						cur_seg_pos_x += offset_hor;
						cur_seg_pos_y += 0.0d;
						cur_seg_pos_align = ManhattenAlignment.ma_vertical;
						break;
					}
					
					/** enlarge route */
					cur_coordinate = new Coordinate(cur_seg_pos_x, cur_seg_pos_y, start_layer, cur_seg_pos_align);
					SegPart sub_r_wrapper = new SegPart();
					ArrayList<Coordinate> sub_r = new ArrayList<Coordinate>();
					sub_r.add(last_coordinate);
					sub_r.add(cur_coordinate);
					sub_r_wrapper.segment_part = sub_r;
					sub_r_wrapper.id = seg_id;
					coarse_route.add(sub_r_wrapper);
					
					/** set routing segment to region */
					rm.AddRouteToOutReg(current_region_pos, sub_r_wrapper);
					
					/** refresh variables for next iteration */
					current_region_pos = next_region_pos;
					last_coordinate = cur_coordinate;//new Coordinate(cur_coordinate.x, cur_coordinate.y, cur_coordinate.layer);
				}
				
				/** set endpoint */
				cur_coordinate = new Coordinate(rm.segments[seg_id].d_end_x, rm.segments[seg_id].d_end_y, finish_layer);
				SegPart sub_r_wrapper = new SegPart();
				ArrayList<Coordinate> sub_r = new ArrayList<Coordinate>();
				sub_r.add(last_coordinate);
				sub_r.add(cur_coordinate);
				sub_r_wrapper.segment_part = sub_r;
				sub_r_wrapper.id = seg_id;
				coarse_route.add(sub_r_wrapper);
				
				/** set routing segment to region */
				rm.AddRouteToOutReg(current_region_pos, sub_r_wrapper);
				
				/** offer calculated route to rm */
				rm.OfferCoarseRoute(coarse_route, seg_id, visited_routes);
			
			}else{
				++debug_bt_skips;
			}
			
			/** poll next backtrace */
			sbtp = rm.PollBacktrace();
			bt = sbtp.backtrace;
			seg_id = sbtp.seg_id;
		}
	}
	
	
	
	private void EstimateDemand(){
		SegmentRepresentation seg_rep = rm.PollDemandEstimationJob();
		while(seg_rep != null){

			/** Create Demand Template and add it to RegionBorders */
			DemandTemplateHandler dth = new DemandTemplateHandler(this);
			dth.AddDemandEstimate(seg_rep.start, seg_rep.end);
			
			/** poll new job */
			seg_rep = rm.PollDemandEstimationJob();
		}
	}
	
	private void RemoveFailedSegments(){
		int seg_id = rm.PollFailedId();
		while(seg_id >= 0){
			
			/** get route */
			RouteToStitch rts = null;
			rts = rm.output_coarse_routes.get(seg_id);
			if(rts == null){
				seg_id = rm.PollFailedId();
				continue;
			}
			
			ArrayList<Vector2i> route = rts.region_positions;
			Iterator<Vector2i> it = route.iterator();
			if(it.hasNext()){
				
				Vector2i cur = it.next();
				Vector2i next = it.hasNext() ? it.next() : null;
				while(next != null){
					RegionDirection dir = rm.DirFromTo(cur, next);
					
					if(dir != RegionDirection.rd_undefined){
					
						RegionBorder b = rm.RegionAt(cur.x, cur.y).GetRegionBorder(dir);
						b.RevertHardPass(seg_id);
					}	
					cur = next;
					next = it.hasNext() ? it.next() : null;
				}
			}
			
			seg_id = rm.PollFailedId();
		};
	}
	
	public void run(){
		
		/** Remove Failed Segments from last run */
		RemoveFailedSegments();
		
		/** join threads */
		try {
		  rm.barrier.await();
		} catch (InterruptedException ex) {
		   return;
		} catch (BrokenBarrierException ex) {
		  return;
		}
		
		/** reset output_routes */
		if(slave_id == 0){
			rm.ResetOutputRoutes();
		}
		
		/** calculate demand estimate */
		EstimateDemand();
		
		/** join threads */
		try {
		  rm.barrier.await();
		} catch (InterruptedException ex) {
		   return;
		} catch (BrokenBarrierException ex) {
		  return;
		}
		
		/** do frontwave and backtraces for all segments */
		FindRoutes();
		rm.OfferUnrouted(non_routable);
		
		/** join threads */
		try {
		  rm.barrier.await();
		} catch (InterruptedException ex) {
		   return;
		} catch (BrokenBarrierException ex) {
		  return;
		}

		SortPassesV2();
		
		/** join threads */
		try {
		  rm.barrier.await();
		} catch (InterruptedException ex) {
		   return;
		} catch (BrokenBarrierException ex) {
		  return;
		}
		
		CreateOutput();
		
		/** join threads */
		try {
		  rm.barrier.await();
		} catch (InterruptedException ex) {
		   return;
		} catch (BrokenBarrierException ex) {
		  return;
		}
	}
			
}
