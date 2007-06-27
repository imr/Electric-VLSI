package com.sun.electric.tool.generator.infinity;

import java.util.ArrayList;
import java.util.List;


/** rectangular region for routing */
public class Channel implements Comparable<Channel> {
	// A channel may be adjacent to a wide power or ground strap.
	// More spacing is required for wide metal. 
	private static final double METAL_WIDE_SPACE = 4;
	private static final double METAL_SPACE = 3;
	private static final double METAL_WIDTH = 3;
	private boolean horizontal;
    private List<Track> tracks = new ArrayList<Track>();
    private double minL, maxL,   // dimensions along channel length    
                   minW, maxW;   // dimensions along channel width

    public Channel(boolean horizontal, double l1, double l2, 
    		       double w1, double w2) {
    	this.horizontal = horizontal;
    	this.minL = Math.min(l1, l2);
    	this.maxL = Math.max(l1, l2);
    	this.minW = Math.min(w1, w2);
    	this.maxW = Math.max(w1, w2);
    	double trackCent=minW+METAL_WIDE_SPACE+METAL_WIDTH/2;
    	while (trackCent+METAL_WIDTH/2 + METAL_WIDE_SPACE <=maxW) {
    		int ndx = tracks.size();
    		tracks.add(new Track(trackCent, this, ndx));
    		trackCent += METAL_SPACE + METAL_WIDTH;
    	}
    }
    
    public int compareTo(Channel c) {
    	double delta = minW - c.minW;
    	return (int) Math.signum(delta);
    }
   
    public Segment allocate(double xy1, double xy2) {
    	double min = Math.min(xy1, xy2);
    	double max = Math.max(xy1, xy2);
    	
    	// Check if span is outside this Channel
    	if (max>this.maxL || min<this.minL) return null;
    	
    	// Try a greedy track allocation
    	for (Track t : tracks) {
    		if (t.isAvailable(min, max)) {
    			return t.allocate(min, max);
    		}
    	}
    	return null;
    }
    public boolean isHorizontal() {return horizontal;}
    public boolean hasTracks() {return tracks.size()!=0;}
    public String toString() {
    	double xMin, xMax, yMin, yMax;
    	if (horizontal) {
    		xMin=minL; yMin=minW;
    		xMax=maxL; yMax =maxW;
    	} else {
    		xMin=minW; yMin=minL;
    		xMax=maxW; yMax=maxL;
    	}
    	
    	StringBuffer sb = new StringBuffer();
    	sb.append(horizontal ? "  Horizontal" : "  Vertical");
    	sb.append(" channel bounds: (");
    	sb.append(xMin+", "+yMin+") (");
    	sb.append(xMax+", "+yMax+") ");
    	sb.append(" Tracks:\n");
    	for (Track t : tracks)  sb.append(t.toString());

    	return sb.toString();
    }
    public double getMinX() {return isHorizontal() ? minL : minW;}
    public double getMaxX() {return isHorizontal() ? maxL : maxW;}
    public double getMinY() {return isHorizontal() ? minW : minL;}
    public double getMaxY() {return isHorizontal() ? maxW : maxL;}
}
