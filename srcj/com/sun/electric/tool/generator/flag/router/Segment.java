/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Segment.java
 *
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.electric.tool.generator.flag.router;




/** A line segment that makes up part of a route */
public class Segment implements Comparable {
	public final Track track;
	public final int trackNdx;
	public double min, max;
	public Segment(double xy1, double xy2, Track track, int trackNdx) {
		this.min = Math.min(xy1, xy2);
		this.max = Math.max(xy1, xy2);
		this.track = track;
		this.trackNdx = trackNdx;
	}
	public int compareTo(Object o) {
		double delta = min - ((Segment)o).min;
		return (int) Math.signum(delta);
	}	
	public boolean isHorizontal() {return track.isHorizontal();}
	public double getTrackCenter() {return track.getCenter();}
	public double getSegmentMin() {return min;}
	public double getSegmentMax() {return max;}
	public void trim(double xy1, double xy2) {
		double xyMin = Math.min(xy1, xy2);
		double xyMax = Math.max(xy1, xy2);
		//LayoutLib.error(xyMin<min || xyMax>max, "trim may not extend segment");
		min = Math.max(min, xyMin);
		max = Math.min(max, xyMax);
		track.resort(this);
	}
	public String toString() {
		return "center="+getTrackCenter()+" ["+min+", "+max+"]";
	}
}
