/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DemandTemplateHandler.java
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


public class DemandTemplateHandler {
	
	GlobalRouterThreadV3 grt;
	
	public DemandTemplateHandler(GlobalRouterThreadV3 gr_thread){
		grt = gr_thread;
	}
	
	int max_demand_estimate = 8;
	class DemandEstimationJob{
		public Vector2i pos;
		public RegionDirection dir;
		public int demand_increment;
		
		public DemandEstimationJob(Vector2i pos, RegionDirection dir, int demand_inc){
			this.pos  = pos;
			this.dir = dir;
			this.demand_increment = demand_inc;
		}
	}
	
	public void AddDemandEstimate(Vector2i start, Vector2i end){
		ArrayList<DemandEstimationJob> template = CalcDemandTemplate(start, end);
		ApplyDemandTemplate(template, true);
	}
	public void DecrementDemandEstimate(Vector2i start, Vector2i end){
		ArrayList<DemandEstimationJob> template = CalcDemandTemplate(start, end);
		ApplyDemandTemplate(template, false);
	}
	
	private ArrayList<DemandEstimationJob> CalcDemandTemplate(Vector2i start, Vector2i end){
		ArrayList<DemandEstimationJob> demand_job_queue = new ArrayList<DemandEstimationJob>();
		
		/** go to end point in horizontal direction */
		int hor_dir = end.x - start.x >= 0 ? 1 : -1;
		int ver_dir = end.y - start.y >= 0 ? 1 : -1;
		RegionDirection curr_dir = (hor_dir == 1 ? RegionDirection.rd_right : RegionDirection.rd_left);
		
		Vector2i it = new Vector2i(start.x, start.y);
		for(;it.x != end.x; it.x += hor_dir){
			int s_dist = max_demand_estimate - Math.abs(it.x - start.x) + 1;
			s_dist = s_dist >= 0 ? s_dist : 0;
			int e_dist = max_demand_estimate - Math.abs(it.x - end.x) + 1;
			e_dist = e_dist >= 0 ? e_dist : 0;
			int dist = e_dist + s_dist;
			
			Vector2i pos = new Vector2i(it);
			RegionDirection dir = curr_dir;
			demand_job_queue.add(new DemandEstimationJob(pos, dir, dist));
		}
		
		/** go to end point in vertical direction */
		curr_dir = (ver_dir == 1 ? RegionDirection.rd_up : RegionDirection.rd_down);
		
		for(; it.y != end.y; it.y += ver_dir){
			int s_dist = max_demand_estimate - Math.abs(it.y - start.y) + 1;
			s_dist = s_dist >= 0 ? s_dist : 0;
			int e_dist = max_demand_estimate - Math.abs(it.y - end.y) + 1;
			e_dist = e_dist >= 0 ? e_dist : 0;
			int dist = e_dist + s_dist;

			Vector2i pos = new Vector2i(it);
			RegionDirection dir = curr_dir;
			demand_job_queue.add(new DemandEstimationJob(pos, dir, dist));
		}
		
		/** flip direction and go back to start in horizontal direction */
		hor_dir -= 2 * hor_dir;
		ver_dir -= 2 * ver_dir;
		curr_dir = (hor_dir == 1 ? RegionDirection.rd_right : RegionDirection.rd_left);
		
		for(;it.x != start.x; it.x += hor_dir){
			int s_dist = max_demand_estimate - Math.abs(it.x - start.x) + 1;
			s_dist = s_dist >= 0 ? s_dist : 0;
			int e_dist = max_demand_estimate - Math.abs(it.x - end.x) + 1;
			e_dist = e_dist >= 0 ? e_dist : 0;
			int dist = e_dist + s_dist;
			
			Vector2i pos = new Vector2i(it);
			RegionDirection dir = curr_dir;
			demand_job_queue.add(new DemandEstimationJob(pos, dir, dist));
		}
		
		/** go to start in vertical direction */
		curr_dir = (ver_dir == 1 ? RegionDirection.rd_up : RegionDirection.rd_down);
		
		for(; it.y != start.y; it.y += ver_dir){
			int s_dist = max_demand_estimate - Math.abs(it.y - start.y) + 1;
			s_dist = s_dist >= 0 ? s_dist : 0;
			int e_dist = max_demand_estimate - Math.abs(it.y - end.y) + 1;
			e_dist = e_dist >= 0 ? e_dist : 0;
			int dist = e_dist + s_dist;

			Vector2i pos = new Vector2i(it);
			RegionDirection dir = curr_dir;
			demand_job_queue.add(new DemandEstimationJob(pos, dir, dist));
		}
		
		return demand_job_queue;
	}
	
	private void ApplyDemandTemplate(ArrayList<DemandEstimationJob> template, boolean is_inc){
		
		Iterator<DemandEstimationJob> it = template.iterator();
		while(it.hasNext()){
			DemandEstimationJob job = it.next();
			if(is_inc){
				grt.rm.RegionAt(job.pos.x, job.pos.y).GetRegionBorder(job.dir).IncDemand();
			}else{
				grt.rm.RegionAt(job.pos.x, job.pos.y).GetRegionBorder(job.dir).DecDemand();
			}
			
		}
	}
}
