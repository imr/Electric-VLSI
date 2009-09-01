/**
 * 
 */
package com.sun.electric.tool.generator.flag.router;

public class Interval {
	private double min, max;
	Interval(double min, double max) {this.min=min; this.max=max;}
	/** Merge two overlapping blockages into one */
	public void merge(double min, double max) {
		this.min = Math.min(this.min, min);
		this.max = Math.max(this.max, max); 
	}
	public double getMin() {return min;}
	public double getMax() {return max;}
	public String toString() {
		return "["+min+", "+max+"]";
	}
}
