package com.sun.electric.tool.generator.layout.fill;

/**
 * Created by IntelliJ IDEA.
 * User: gg151869
 * Date: Feb 23, 2007
 * Time: 4:25:45 PM
 * To change this template use File | Settings | File Templates.
 */

// ---------------------------------- FloorPlan -------------------------------
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

//---------------------------------- CapFloorPlan -----------------------------
public class CapFloorplan extends Floorplan {
	public CapFloorplan(double width, double height, boolean horiz) {
		super(width, height, horiz);
	}
}
