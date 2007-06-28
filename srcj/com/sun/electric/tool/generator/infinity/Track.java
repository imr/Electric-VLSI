package com.sun.electric.tool.generator.infinity;

import java.util.TreeSet;

import com.sun.electric.tool.generator.layout.LayoutLib;

public class Track {
	/** X or Y coordinate of center of track */
	private final double center;
	private final int trackNdx;
	private final Channel channel;
	private TreeSet<Segment> segments = new TreeSet<Segment>();
	
	public Track(double coord, Channel channel, int trackNdx) {
		this.center = coord;
		this.trackNdx=trackNdx; 
		this.channel=channel;
	}
	
	// does a blockage overlap the interval?
	public boolean isAvailable(double min, double max) {
		for (Segment s : segments) {
			if (s.min>max) return true;
			if (s.max<min) continue;
			return false;
		}
		return true;
	}
	
	private void sanityCheck() {
		Segment prev = null;
		for (Segment s : segments) {
			if (prev!=null) {
				LayoutLib.error(prev.min>=s.min, "illegally ordered segments");
				LayoutLib.error(prev.max>s.min, "overlapping segments");
			}
			prev = s;
		}
	}
	
	public Segment allocate(double min, double max) {
		// sanity check
		LayoutLib.error(!isAvailable(min, max), "overlapping blockages");
		Segment seg = new Segment(min, max, this, trackNdx);
		segments.add(seg);
		sanityCheck();
		return seg;
	}
	public boolean isHorizontal() {return channel.isHorizontal();}
	public double getCenter() {return center;}
	public int getIndex() {return trackNdx;}
	public void resort(Segment s) {
		segments.remove(s);
		segments.add(s);
	}
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("    Track center: "+center+"\n");
		if (segments.size()!=0) {
			sb.append("      ");
			for (Segment s : segments)  sb.append(s.toString()+"  ");
			sb.append("\n");
		}
		return sb.toString();
	}
}
