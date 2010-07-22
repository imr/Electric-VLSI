/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GlobalRouterPathFinder.java
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class GlobalRouterPathFinder {

	GlobalRouterV3 glr;
	int[] fields;
	
	public GlobalRouterPathFinder(GlobalRouterV3 router){
		glr = router;
		
		fields = new int[glr.regions_x * glr.regions_y];
		for(int i = 0; i < fields.length; ++i){
			fields[i] = Integer.MAX_VALUE;
		}
	}
	private int GetFieldIndex(int x, int y){
		return y * glr.regions_x + x;
	}
	private void SetField(int x, int y, int value){
		fields[GetFieldIndex(x, y)] = value;
	}
	private int GetField(int x, int y){
		return fields[GetFieldIndex(x, y)];
	}
	
	public ArrayList<Vector2i> ConvertToPath(ArrayList<RegionDirection> dirs, Vector2i start){
		ArrayList<Vector2i> ret = new ArrayList<Vector2i>();
		Vector2i pos = new Vector2i(start);
		
		//add start
		ret.add(pos);
		
		Iterator<RegionDirection> it = dirs.iterator();
		while(it.hasNext()){
			RegionDirection rd = it.next();
			Vector2i nb = glr.GetNeighborPos(pos, rd);
			assert(glr.IsCoordinateValid(nb.x, nb.y));
			ret.add(nb);
		}
		
		return ret;
	}
	
	private class LeeMooreJob{
		Vector2i pos;
		int length;
		
		private LeeMooreJob(Vector2i p, int l){
			pos = p;
			length = l;
		}
	}
	private ArrayList<RegionDirection> FindShortestPathLeeMoore(int seg_id){
		Vector2i start = glr.segments[seg_id].start;
		Vector2i end = glr.segments[seg_id].end;
		
		LinkedList<LeeMooreJob> jobs = new LinkedList<LeeMooreJob>();
		LeeMooreJob cj = new LeeMooreJob(new Vector2i(start), 0);
		SetField(start.x, start.y, 0);
		
		while(cj != null && !cj.pos.equals(end)){
			/** consider neighbors */
			for(RegionDirection dir : RegionDirection.values()){
				if(dir == RegionDirection.rd_undefined){
					continue;
				}
				if(!glr.IsCoordinateValid(glr.GetNeighborX(cj.pos.x, dir), 
						glr.GetNeighborY(cj.pos.y, dir))){
					continue;
				}

				// check if neighbor has shorter backpath
				Vector2i neighbor = glr.GetNeighborPos(cj.pos, dir);
				int n = GetField(neighbor.x, neighbor.y);
				if(n >= 0 && n < Integer.MAX_VALUE && n <= cj.length + 1){
					continue;
				}
				
				// check if border is blocked
				if(glr.RegionAt(cj.pos.x, cj.pos.y).GetRegionBorder(dir).IsBlocked()){
					continue;
				}

				// else pass border and offer job
				SetField(neighbor.x, neighbor.y, cj.length + 1);
				jobs.offer(new LeeMooreJob(neighbor, cj.length + 1));
			}
			

			/** poll next job */
			cj = jobs.poll();
		}
			
		if(cj == null || !cj.pos.equals(end)){
			//could not get to end
			return null;
		}
		int l = cj.length;
		/** backtrace */
		ArrayList<RegionDirection> ret_list = new ArrayList<RegionDirection>();
		
		Vector2i it = new Vector2i(end);
		while(!it.equals(start)){
			int min = Integer.MAX_VALUE;
			RegionDirection min_dir = RegionDirection.rd_undefined;
			
			RegionDirection[] directions = RegionDirection.values();
			List<RegionDirection> dir_list = Arrays.asList(directions);
			Collections.shuffle(dir_list);
			for(int i= 0; i < dir_list.size(); ++i){
				directions[i] = dir_list.get(i);
			}
			for(RegionDirection dir : directions){
				if(dir == RegionDirection.rd_undefined){
					continue;
				}
				if(!glr.IsCoordinateValid(glr.GetNeighborX(it.x, dir), 
						glr.GetNeighborY(it.y, dir))){
					continue;
				}
				
				int w = GetField(glr.GetNeighborX(it.x, dir), glr.GetNeighborY(it.y, dir));
				if(w >= 0 && w < min){
					min = w;
					min_dir = dir;
				}
			}
			if(min_dir == RegionDirection.rd_undefined){
				// should never happen
				assert(min_dir != RegionDirection.rd_undefined);
				return null;
			}
			ret_list.add(0, GlobalRouterV3.GetOppositeDir(min_dir));
			assert(ret_list.size() <= l);
			it = glr.GetNeighborPos(it, min_dir);
		}
		
		return ret_list;
	}
	
	public List<RegionDirection> FindShortestPath(int seg_id){
		return FindShortestPathLeeMoore(seg_id);
	}

}
