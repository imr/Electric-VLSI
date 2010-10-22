/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LayerChannels.java
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

import java.util.Collection;
import java.util.TreeSet;

import com.sun.electric.tool.generator.flag.Utils;
import com.sun.electric.tool.Job;

/** All the channels for one layer */
public class LayerChannels {
	private TreeSet<Channel> channels = new TreeSet<Channel>();
	
	private static void prln(String msg) {Utils.prln(msg);}
	private boolean isHorizontal() {
		Job.error(channels.size()==0,
				        "can't tell direction because no channels");
		return channels.first().isHorizontal();
	}
	
	public LayerChannels() { }
	public void add(Channel ch) {
		if (ch.hasTracks()) channels.add(ch);
	}
	public Collection<Channel> getChannels() {return channels;}
	
	/** For a horizontal layer, find a channel that covers a vertical pin
	 * at x from y1 to y2. */
	public Channel findChanOverVertInterval(double x, double y1, double y2) {
		double yMin = Math.min(y1, y2);
		double yMax = Math.max(y1, y2);
		if (channels.size()==0) return null;
		Job.error(!isHorizontal(), "not sure what this means yet");
		for (Channel c : channels) {
			Job.error(x<c.getMinTrackEnd() || x > c.getMaxTrackEnd(),
					        "channels can't cover X");
			if (c.getMaxTrackCenter()<yMin) continue;
			if (c.getMinTrackCenter()>yMax) break;;
			return c;
		}
		return null;
	}
	/** For a vertical layer, find a vertical channel between x1 and x2
	 * that can connect the two horizontal channels: horChan1 and horChan2. */
	public Channel findVertBridge(Channel horChan1, Channel horChan2, 
			                      double x1, double x2) {
		if (channels.size()==0) return null;
		Job.error(isHorizontal(), "layer must be vertical");
		double yMin = Math.min(horChan1.getMinTrackCenter(), 
				               horChan2.getMinTrackCenter());
		double yMax = Math.max(horChan1.getMaxTrackCenter(), 
				               horChan2.getMaxTrackCenter());
		double xMin = Math.min(x1, x2);
		double xMax = Math.max(x1, x2);
		
		double maxOverlap = -Double.MAX_VALUE;
		Channel bestChan = null;
		for (Channel c : channels) {
			if (yMax>c.getMaxTrackEnd() || yMin<c.getMinTrackEnd()) {
				prln("channels can't cover Y");
			}
			// calculate how much of the vertical channel overlaps with
			// [x1, x2]. If channel doesn't overlap with [x1, x2] then 
			// measure the distance between the channel and [x1, x2]
			double startX = Math.max(xMin, c.getMinTrackCenter());
			double endX = Math.min(xMax, c.getMaxTrackCenter());
			double overlap = endX - startX;
			if (overlap>maxOverlap) {
				maxOverlap = overlap;
				bestChan = c;
			}
			// ideally I should check to see if channel has enough
			// capacity
		}
		
		return bestChan;
	}
	public String toString() {
		StringBuffer sb = new StringBuffer();
//		sb.append(isHorizontal() ? "  Horizontal " : "  Vertical ");
//		sb.append("channel\n");
		for (Channel c : channels)  sb.append(c.toString());
		
		return sb.toString();
	}
	public int numChannels() {return channels.size();}
	
}
