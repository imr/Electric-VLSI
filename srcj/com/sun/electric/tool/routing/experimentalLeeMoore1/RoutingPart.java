/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: RoutingPart.java
 * Written by: Andreas Uebelhoer, Alexander Bieles, Emre Selegin (Team 6)
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
package com.sun.electric.tool.routing.experimentalLeeMoore1;

import com.sun.electric.tool.routing.RoutingFrame.RoutePoint;
import com.sun.electric.tool.routing.RoutingFrame.RoutingContact;
import com.sun.electric.tool.routing.RoutingFrame.RoutingSegment;
import com.sun.electric.tool.routing.experimentalLeeMoore1.LeeMoore.Tupel;

/**
 * This class encapsulates a part of a routing segment as we divide it into pieces
 */
public class RoutingPart {
	public RoutingSegment rs;				//RoutingSegment this RoutingPart belongs to
	public Tupel start, end;				//start and end of this RoutingPart
	private RoutePoint rp_start,rp_end;		//RoutePoint of start and end (they are already created)
	
	RoutingPart(RoutingSegment rs, Tupel start, Tupel end, RoutePoint rp_start, RoutePoint rp_end){
		this.rs = rs;
		this.start = start;
		this.end = end;
		this.rp_start=rp_start;
		this.rp_end=rp_end;
	}
	
	public RoutingPart(RoutingSegment rs){
		this.rs = rs;
		this.start = new Tupel(rs.getStartEnd().getLocation(),rs.getStartLayers().get(0).getMetalNumber()-1);
		rp_start=new RoutePoint(RoutingContact.STARTPOINT, rs.getStartEnd().getLocation(), 0);
		this.end = new Tupel(rs.getFinishEnd().getLocation(),rs.getFinishLayers().get(0).getMetalNumber()-1);
		rp_end=new RoutePoint(RoutingContact.FINISHPOINT, rs.getFinishEnd().getLocation(), 0);
	}
	
	/**
	 * Split the routing part at the given middle point and return the suffix part.
	 * @param middlePoint new start
	 * @param rp RoutePoint of the new start
	 * @return RoutingPart from middlePoint to end
	 */
	public RoutingPart getSuffixPart(Tupel middlePoint,RoutePoint rp){
		return new RoutingPart(this.rs, new Tupel(middlePoint.getX_InsideElectric(),middlePoint.getY_InsideElectric(),middlePoint.getLayer(),true), this.end,rp,rp_end);
	}
	
	/**
	 * Split the routing part at the given middle point and return the prefix part.
	 * @param middlePoint new start
	 * @param rp RoutePoint of the new end
	 * @return RoutingPart from start to middlePoint
	 */
	public RoutingPart getPrefixPart(Tupel middlePoint,RoutePoint rp){
		return new RoutingPart(this.rs, this.start, new Tupel(middlePoint.getX_InsideElectric(),middlePoint.getY_InsideElectric(),middlePoint.getLayer(),true),rp_start,rp);
	}
	
	/**
	 * Get RoutePoint of start
	 * @return RotePoint
	 */
	public RoutePoint getStartRoutePoint() {
		return rp_start;
	}
	
	/**
	 * Set RoutePoint of start
	 * @param rpStart RoutePoint
	 */
	public void setStartRoutePoint(RoutePoint rpStart) {
		rp_start = rpStart;
	}
	
	/**
	 * Get RoutePoint of end
	 * @return RotePoint
	 */
	public RoutePoint getEndRoutePoint() {
		return rp_end;
	}

	/**
	 * Set RoutePoint of end
	 * @param rpEnd RoutePoint
	 */
	public void setEndRoutePoint(RoutePoint rpEnd) {
		rp_end = rpEnd;
	}
	
	public String toString(){
		return start.toString()+"->"+end.toString();
	}
}
