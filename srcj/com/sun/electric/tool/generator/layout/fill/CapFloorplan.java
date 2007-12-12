package com.sun.electric.tool.generator.layout.fill;

/**
 * FloorPlan.
 * User: gg151869
 * Date: Feb 23, 2007
 */
class Floorplan {
	public final double cellWidth;
	public final double cellHeight;
	public final boolean horizontal;
	public Floorplan(double width, double height, boolean horiz) {
		cellWidth = width;
		cellHeight = height;
		horizontal = horiz;
	}
}

/**
 * CapFloorPlan.
 */
public class CapFloorplan extends Floorplan {
	public CapFloorplan(double width, double height, boolean horiz) {
		super(width, height, horiz);
	}
}
