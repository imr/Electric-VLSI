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
    /** Find an unused part of a track that can connect xy1 with xy2.
     * Any track whose center lies outside the bounds boundXY1 and boundXY2
     * increases the manhatten distance of the route.  
     * If we need to select from two tracks that both minimize the 
     * manhatten distance, then choose the track closest to the center
     * of the channel.  This will tend to pack the segments into the 
     * smallest number of channels.  */
    public Segment allocate(double xy1, double xy2,
    		                double boundXY1, double boundXY2) {
    	double min = Math.min(xy1, xy2);
    	double max = Math.max(xy1, xy2);
    	double minBound = Math.min(boundXY1, boundXY2);
    	double maxBound = Math.max(boundXY1, boundXY2);
    	
    	// Check if span is outside this Channel
    	if (max>this.maxL || min<this.minL) return null;
    	
    	// Try a greedy track allocation
    	double minCost = Double.MAX_VALUE;
    	Track bestTrack = null;
    	int nbTracks = tracks.size();
    	int midNdx = (nbTracks-1) / 2;
    	for (Track t : tracks) {
    		if (t.isAvailable(min, max)) {
    			double center = t.getCenter();
    			double cost = Math.abs(t.getIndex()-midNdx);
    			double c = center - maxBound;
    			if (c>0) cost+=c*1000;
    			c = minBound-center;
    			if (c>0) cost+=c*1000;
    			if (cost<minCost) {
    				minCost = cost;
    				bestTrack = t;
    			}
    		}
    	}
    	if (bestTrack==null) return null;
    	return bestTrack.allocate(min, max);
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
