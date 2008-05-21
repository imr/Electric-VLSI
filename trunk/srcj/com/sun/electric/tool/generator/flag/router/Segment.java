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
