/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GlobalRouterAnalysis.java
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

import java.util.List;

import com.sun.electric.tool.routing.experimentalLeeMoore2.GlobalRouterV3.RegionToRoute;
import com.sun.electric.tool.routing.experimentalLeeMoore2.GlobalRouterV3.RouteToStitch;
import com.sun.electric.tool.routing.experimentalLeeMoore2.RoutingFrameLeeMoore.ManhattenAlignment;

public class GlobalRouterAnalysis {
	
class AnalysisRegionResult{
	int num_segments;
	double wire_length;
}
AnalysisRegionResult[][] regionResult;

class AnalysisRouteResult{
	int num_corners;
	double route_length;
}
AnalysisRouteResult[] routeResult;

public GlobalRouterAnalysis(int num_hor, int num_vert, int num_segments){
	regionResult = new AnalysisRegionResult[num_hor][num_vert];
	for(int x = 0; x < regionResult.length; ++x){
		for(int y = 0; y < regionResult[x].length; ++y){
			regionResult[x][y] = new AnalysisRegionResult();
		}
	}
	
	routeResult = new AnalysisRouteResult[num_segments];
	for(int i = 0; i < routeResult.length; ++i){
		routeResult[i] = new AnalysisRouteResult();
	}
}

public void analyseGRresult(RegionToRoute[] regions, List<RouteToStitch> routes){
	
	/** segment patches and congestion per region */
	for(int i = 0; i < regions.length; ++i){
		int x = i % regionResult.length;
		int y = (int)Math.floor(i / regionResult.length);
		
		regionResult[x][y].num_segments = regions[y * regionResult.length 
		                                          + x].segments_to_route.size();
		
		for(SegPart sp : regions[y * regionResult.length + x].segments_to_route){
			double dx = Math.abs(sp.segment_part.get(sp.segment_part.size() - 1).x 
					- sp.segment_part.get(0).x);
			double dy = Math.abs(sp.segment_part.get(sp.segment_part.size() - 1).y 
					- sp.segment_part.get(0).y);
			regionResult[x][y].wire_length += dx + dy;
		}
	}
	
	/** length and corners of routes */
	for(RouteToStitch rts : routes){
		routeResult[rts.id].num_corners = 0;
		
		int part_size = rts.coarse_route.get(0).segment_part.size();
		ManhattenAlignment last_dir = rts.coarse_route.get(0).segment_part.get(part_size - 1).alignment;
		for(int i = 1; i < rts.coarse_route.size() - 1; ++i){
			part_size = rts.coarse_route.get(i).segment_part.size();
			ManhattenAlignment cur_dir = rts.coarse_route.get(i).segment_part.get(part_size - 1).alignment;
			
			if(cur_dir != last_dir){
				++routeResult[rts.id].num_corners;
			}
			
			last_dir = cur_dir;
		}
		
		for(SegPart sp : rts.coarse_route){
			double dx = Math.abs(sp.segment_part.get(sp.segment_part.size() - 1).x 
					- sp.segment_part.get(0).x);
			double dy = Math.abs(sp.segment_part.get(sp.segment_part.size() - 1).y 
					- sp.segment_part.get(0).y);
			routeResult[rts.id].route_length += dx + dy;
		}
	}
}

}
