package com.sun.electric.tool.generator.infinity;

import java.util.Collection;
import java.util.TreeSet;

import com.sun.electric.tool.generator.layout.LayoutLib;

/** All the channels for one layer */
public class LayerChannels {
	private TreeSet<Channel> channels = new TreeSet<Channel>();
	
	private static void prln(String msg) {System.out.println(msg);}
	private boolean isHorizontal() {
		LayoutLib.error(channels.size()==0, 
				        "can't tell direction because no channels");
		return channels.first().isHorizontal();
	}
	
	public LayerChannels() { }
	public void add(Channel ch) {channels.add(ch);}
	public Collection<Channel> getChannels() {return channels;}
	
	/** For a horizontal layer, find a channel that covers a vertical pin
	 * at x from y1 to y2. */
	public Channel findChanOverVertInterval(double x, double y1, double y2) {
		double yMin = Math.min(y1, y2);
		double yMax = Math.max(y1, y2);
		if (channels.size()==0) return null;
		LayoutLib.error(!isHorizontal(), "not sure what this means yet");
		for (Channel c : channels) {
			LayoutLib.error(x<c.getMinTrackEnd() || x > c.getMaxTrackEnd(),
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
		LayoutLib.error(isHorizontal(), "layer must be vertical");
		double yMin = Math.min(horChan1.getMinTrackCenter(), 
				               horChan2.getMinTrackCenter());
		double yMax = Math.max(horChan1.getMaxTrackCenter(), 
				               horChan2.getMaxTrackCenter());
		double minDist = Double.MAX_VALUE;
		Channel bestChan = null;
		for (Channel c : channels) {
			LayoutLib.error(yMax>c.getMaxTrackEnd() || yMin<c.getMinTrackEnd(),
					        "channels can't cover Y");
			double cCent = (c.getMinTrackCenter()+c.getMaxTrackCenter())/2;
			double dist = Math.abs(cCent-((x1+x2)/2));
			if (dist<minDist) {
				minDist = dist;
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
	
}
