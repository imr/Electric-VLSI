/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: TrackRouter.java
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
 *
 * Electric(tm) is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
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
package com.sun.electric.tool.generator.layout;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.Properties;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import com.sun.electric.tool.Job;
import com.sun.electric.tool.user.User;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;

// ---------------------------- Fill Cell Globals -----------------------------
class G {
	public static final double DEF_SIZE = LayoutLib.DEF_SIZE;
	public static ArcInst noExtendArc(PrimitiveArc pa, double w, 
									   PortInst p1, PortInst p2) {
		ArcInst ai = LayoutLib.newArcInst(pa, w, p1, p2);
		ai.clearExtended();
		return ai;		
	}
	public static ArcInst newArc(PrimitiveArc pa, double w, 
								  PortInst p1, PortInst p2) {
		return LayoutLib.newArcInst(pa, w, p1, p2);
	}
	private G(){}
}
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

// ------------------------------ MetalFloorplan ------------------------------
// Floor plan:
//
//  half of Gnd reserved
//  gggggggggggggggggggg
//  wide space
//  vvvvvvvvvvvvvvvvvvvv
//	Vdd reserved
//  vvvvvvvvvvvvvvvvvvvv
//  wide space
//  gggggggggggggggggggg
//	half of Gnd reserved 
class MetalFloorplan extends Floorplan {
	/** width Vdd wires */				public final double vddWidth;
	/** width Gnd wires */  			public final double gndWidth;
	/** no gap between Vdd wires */		public final boolean mergedVdd;
	/** if horizontal then y coordinate of top Vdd wire
	 *  if vertical then x coordinate of right Vdd wire */
	public final double vddCenter;
	/** if horizontal then y coordinate of top Gnd wire 
	 *  if vertical then x coordinate of right Gnd wire */ 
	public final double gndCenter;

	MetalFloorplan(double cellWidth, double cellHeight,
			  double vddReserve, double gndReserve, 
			  double space, boolean horiz) {
		super(cellWidth, cellHeight, horiz);
		mergedVdd = vddReserve==0;
		double cellSpace = horiz ? cellHeight : cellWidth;
		gndWidth = (cellSpace/2 - space - gndReserve) / 2;
		gndCenter = cellSpace/4 + space/2 + gndWidth/2;
		if (mergedVdd) {		
			vddWidth = cellSpace/2 - space - vddReserve;
			vddCenter = 0;
		} else {
			vddWidth = (cellSpace/2 - space - vddReserve) / 2;
			vddCenter = vddReserve/2 + vddWidth/2;
		}
	}
}

/** Give access to the metal straps inside a MetalLayer or CapLayer */
interface VddGndStraps {
	/** are metal straps horizontal? */		boolean isHorizontal();

	/** how many Vdd straps? */				int numVdd();
	/** get nth Vdd strap */				PortInst getVdd(int n);
	/** if horizontal get Y else get X */	double getVddCenter(int n);
	/** how wide is nth Vdd metal strap */	double getVddWidth(int n);

	/** how many Gnd straps? */ 			int numGnd();
	/** get nth Gnd strap */				PortInst getGnd(int n);
	/** if horizontal get Y else X */ 		double getGndCenter(int n);
	/** how wide is nth Gnd strap? */ 		double getGndWidth(int n);
	
	PrimitiveNode getPinType();
	PrimitiveArc getMetalType();
	double getCellWidth();
	double getCellHeight();
}

// ------------------------------- FillLayerMetal -----------------------------
class MetalLayer implements VddGndStraps {
	private static final PrimitiveArc[] METALS = 
		{null, Tech.m1, Tech.m2, Tech.m3, Tech.m4, Tech.m5, Tech.m6};
	private static final PrimitiveNode[] PINS = 
		{null, Tech.m1pin, Tech.m2pin, Tech.m3pin, Tech.m4pin, Tech.m5pin, 
		 Tech.m6pin};
	private final MetalFloorplan plan;
	private final int layerNum;
	private final PrimitiveNode pin;
	private final PrimitiveArc metal;
	private ArrayList vddPorts = new ArrayList();
	private ArrayList gndPorts = new ArrayList();
	private ArrayList vddCenters = new ArrayList();
	private ArrayList gndCenters = new ArrayList();

	private void buildGnd(MetalFloorplan plan, Cell cell) {
		double pinX, pinY;
		if (plan.horizontal) {
			pinX = plan.cellWidth/2 - plan.gndWidth/2;
			pinY = plan.gndCenter;				
		} else {
			pinX = plan.gndCenter;
			pinY = plan.cellHeight/2 - plan.gndWidth/2;
		}
		PortInst tl = LayoutLib.newNodeInst(pin, -pinX, pinY, G.DEF_SIZE, 
										    G.DEF_SIZE, 0, cell
										    ).getOnlyPortInst();
		PortInst tr = LayoutLib.newNodeInst(pin, pinX, pinY, G.DEF_SIZE, 
										    G.DEF_SIZE, 0, cell
										    ).getOnlyPortInst();
		PortInst bl = LayoutLib.newNodeInst(pin, -pinX, -pinY, G.DEF_SIZE,
										    G.DEF_SIZE, 0, cell
										    ).getOnlyPortInst();
		PortInst br = LayoutLib.newNodeInst(pin, pinX, -pinY, G.DEF_SIZE, 
										    G.DEF_SIZE, 0, cell
										    ).getOnlyPortInst();
		if (plan.horizontal) {
			G.newArc(metal, plan.gndWidth, tl, tr);
			G.newArc(metal, plan.gndWidth, bl, br);
		} else {
			G.newArc(metal, plan.gndWidth, bl, tl);
			G.newArc(metal, plan.gndWidth, br, tr);
		}
		gndPorts.add(bl);
		gndPorts.add(tr);
		gndCenters.add(new Double(-plan.gndCenter));
		gndCenters.add(new Double(plan.gndCenter));
	}
	
	private void buildVdd(MetalFloorplan plan, Cell cell) {
		double pinX, pinY;
		if (plan.horizontal) {
			pinX = plan.cellWidth/2 - plan.vddWidth/2;
			pinY = plan.vddCenter;
		} else {
			pinX = plan.vddCenter;
			pinY = plan.cellHeight/2 - plan.vddWidth/2;
		}
		if (plan.mergedVdd) {
			PortInst tr = LayoutLib.newNodeInst(pin, pinX, pinY, G.DEF_SIZE, 
												G.DEF_SIZE, 0, cell
												).getOnlyPortInst();
			PortInst bl = LayoutLib.newNodeInst(pin, -pinX, -pinY, G.DEF_SIZE,
												G.DEF_SIZE, 0, cell
												).getOnlyPortInst();
			G.newArc(metal, plan.vddWidth, bl, tr);
			vddPorts.add(bl);
			vddCenters.add(new Double(plan.vddCenter));
		} else {
			PortInst tl = LayoutLib.newNodeInst(pin, -pinX, pinY, G.DEF_SIZE, 
												G.DEF_SIZE, 0, cell
												).getOnlyPortInst();
			PortInst tr = LayoutLib.newNodeInst(pin, pinX, pinY, G.DEF_SIZE, 
												G.DEF_SIZE, 0, cell
												).getOnlyPortInst();
			PortInst bl = LayoutLib.newNodeInst(pin, -pinX, -pinY, G.DEF_SIZE,
												G.DEF_SIZE, 0, cell
												).getOnlyPortInst();
			PortInst br = LayoutLib.newNodeInst(pin, pinX, -pinY, G.DEF_SIZE, 
												G.DEF_SIZE, 0, cell
												).getOnlyPortInst();
			if (plan.horizontal) {
				G.newArc(metal, plan.vddWidth, tl, tr);
				G.newArc(metal, plan.vddWidth, bl, br);
			} else {
				G.newArc(metal, plan.vddWidth, bl, tl);
				G.newArc(metal, plan.vddWidth, br, tr);
			}
			vddPorts.add(bl);
			vddPorts.add(tr);
			vddCenters.add(new Double(-plan.vddCenter));
			vddCenters.add(new Double(plan.vddCenter));
		}
	}
	
	public MetalLayer(int layerNum, MetalFloorplan plan, Cell cell) {
		this.plan = plan;
		this.layerNum = layerNum;
		metal = METALS[layerNum];
		pin = PINS[layerNum]; 
		buildGnd(plan, cell);
		buildVdd(plan, cell);
	}
	
	public boolean isHorizontal() {return plan.horizontal;}
	public int numVdd() {return vddPorts.size();}
	public PortInst getVdd(int n) {return (PortInst) vddPorts.get(n);}
	public double getVddCenter(int n) {
		return ((Double)vddCenters.get(n)).doubleValue();
	}
	public double getVddWidth(int n) {return plan.vddWidth;}
	public int numGnd() {return gndPorts.size();}
	public PortInst getGnd(int n) {return (PortInst) gndPorts.get(n);}
	public double getGndCenter(int n) {
		return ((Double)gndCenters.get(n)).doubleValue();
	}
	public double getGndWidth(int n) {return plan.gndWidth;}

	public PrimitiveNode getPinType() {return pin;}
	public PrimitiveArc getMetalType() {return metal;}
	public double getCellWidth() {return plan.cellWidth;}
	public double getCellHeight() {return plan.cellHeight;}
	public int getLayerNumber() {return layerNum;}
}

//---------------------------------- CapFloorPlan -----------------------------
class CapFloorplan extends Floorplan {
	public CapFloorplan(double width, double height, boolean horiz) {
		super(width, height, horiz);
	}
}

// ------------------------------------ CapCell -------------------------------
/** CapCell is built assuming horizontal metal 1 straps. I deal with the 
 *  possible 90 degree rotation by creating a NodeInst of this Cell rotated  
 *  by -90 degrees. */
class CapCell {
	/** All the fields in InternalPlan assume that metal1 runs horizontally
	 *  since that is how we build CapCell */
	private static class ProtoPlan {
		private static final double MAX_MOS_WIDTH = 40;
		private static final double SEL_TO_CELL_EDGE = 2;// set by poly cont space
		private static final double SEL_WIDTH = 9;
		private static final double SEL_TO_MOS = 2;
		
		public final double protoWidth, protoHeight;
	
		public final double vddWidth = 9;
		public final double gndWidth = 4;
		public final double vddGndSpace = 3;

		public final double gateWidth;
		public final int numMosX;
		public final double mosPitchX;
		public final double leftWellContX;

		public final double gateLength;
		public final int numMosY;
		public final double mosPitchY;
		public final double botWellContY;
	
		public ProtoPlan(CapFloorplan instPlan) {
			protoWidth = 
				instPlan.horizontal ? instPlan.cellWidth : instPlan.cellHeight;
			protoHeight = 
				instPlan.horizontal ? instPlan.cellHeight : instPlan.cellWidth;
			// compute number of MOS's left to right
			double availForCap = protoWidth - 2*(SEL_TO_CELL_EDGE + SEL_WIDTH/2);
			double numMosD = availForCap / 
							 (MAX_MOS_WIDTH + SEL_WIDTH + 2*SEL_TO_MOS);
			numMosX = (int) Math.ceil(numMosD);
			double mosWidth1 = availForCap/numMosX - SEL_WIDTH - 2*SEL_TO_MOS;
			// round down mos Width to integral number of lambdas
			gateWidth = Math.floor(mosWidth1);
			mosPitchX = gateWidth + SEL_WIDTH + 2*SEL_TO_MOS;
			leftWellContX = - numMosX * mosPitchX / 2;

			// compute number of MOS's bottom to top
			mosPitchY = gndWidth + 2*vddGndSpace + vddWidth;
			gateLength = mosPitchY - gndWidth - 2; 
			numMosY = (int) Math.floor(protoHeight/mosPitchY) - 1;
			botWellContY = - numMosY * mosPitchY / 2; 
		}
	}
	
	private final double POLY_CONT_WIDTH = 10;
	private final String TOP_DIFF = "n-trans-diff-top";
	private final String BOT_DIFF = "n-trans-diff-bottom";
	private final String LEFT_POLY = "n-trans-poly-left";
	private final String RIGHT_POLY = "n-trans-poly-right";
	private final ProtoPlan plan;
	private int gndNum, vddNum; 
	private Cell cell;

	/** Interleave well contacts with diffusion contacts left to right. Begin 
	 *  and end with well contacts */
	private PortInst[] diffCont(double y, ProtoPlan plan, Cell cell) {
		PortInst[] conts = new PortInst[plan.numMosX];
		double x = - plan.numMosX * plan.mosPitchX / 2;
		PortInst wellCont = LayoutLib.newNodeInst(Tech.pwm1, x, y, G.DEF_SIZE,  
										 		  G.DEF_SIZE, 0, cell
										 		  ).getOnlyPortInst();
		Export e = Export.newInstance(cell, wellCont, "gnd_"+gndNum++);
		e.setCharacteristic(PortProto.Characteristic.PWR);

		for (int i=0; i<plan.numMosX; i++) {
			x += plan.mosPitchX/2;
			conts[i] = LayoutLib.newNodeInst(Tech.ndm1, x, y, plan.gateWidth, 5, 
											 0, cell).getOnlyPortInst();
			LayoutLib.newArcInst(Tech.m1, plan.gndWidth, wellCont, conts[i]);
			x += plan.mosPitchX/2;
			wellCont = LayoutLib.newNodeInst(Tech.pwm1, x, y, G.DEF_SIZE,  
											 G.DEF_SIZE, 0, cell
											 ).getOnlyPortInst();
			LayoutLib.newArcInst(Tech.m1, plan.gndWidth, conts[i], wellCont);
		}
		
		// bring metal to cell left and right edges to prevent notches
		x = -plan.protoWidth/2 + plan.gndWidth/2;
		PortInst pi;
		pi = LayoutLib.newNodeInst(Tech.m1pin, x, y, G.DEF_SIZE, G.DEF_SIZE, 0, 
		                           cell).getOnlyPortInst();
		LayoutLib.newArcInst(Tech.m1, plan.gndWidth, pi, conts[0]);
		
		x = plan.protoWidth/2 - plan.gndWidth/2;
		pi = LayoutLib.newNodeInst(Tech.m1pin, x, y, G.DEF_SIZE, G.DEF_SIZE, 0,
		                           cell).getOnlyPortInst();
		LayoutLib.newArcInst(Tech.m1, plan.gndWidth, pi, conts[conts.length-1]);

		return conts;
	}

	/** Interleave gate contacts and MOS transistors left to right. Begin 
	 *  and end with gate contacts. */ 
	private void mos(PortInst[] botDiffs, PortInst[] topDiffs, double y, 
					 ProtoPlan plan, Cell cell) {
		final double POLY_CONT_HEIGHT = plan.vddWidth + 1;
		double x = plan.leftWellContX;
		PortInst poly = LayoutLib.newNodeInst(Tech.p1m1, x, y, POLY_CONT_WIDTH, 
											  POLY_CONT_HEIGHT, 0, cell
											  ).getOnlyPortInst();	
		PortInst leftCont = poly;
		Export e = Export.newInstance(cell, poly, "vdd_"+vddNum++);
		e.setCharacteristic(PortProto.Characteristic.PWR);
		
		for (int i=0; i<plan.numMosX; i++) {
			x += plan.mosPitchX/2;
			NodeInst mos = LayoutLib.newNodeInst(Tech.nmos, x, y, plan.gateWidth, 
												 plan.gateLength, 0, cell);	
			G.noExtendArc(Tech.p1, POLY_CONT_HEIGHT, poly,
						  mos.findPortInst(LEFT_POLY));
			x += plan.mosPitchX/2;
			PortInst polyR = LayoutLib.newNodeInst(Tech.p1m1, x, y, 
												   POLY_CONT_WIDTH, 
										 		   POLY_CONT_HEIGHT, 0, cell
										 		   ).getOnlyPortInst();	
			G.noExtendArc(Tech.m1, plan.vddWidth, poly, polyR);
			poly = polyR;
			G.noExtendArc(Tech.p1, POLY_CONT_HEIGHT, poly,
						  mos.findPortInst(RIGHT_POLY));
			botDiffs[i] = mos.findPortInst(BOT_DIFF);
			topDiffs[i] = mos.findPortInst(TOP_DIFF);
		}
		PortInst rightCont = poly;

		// bring metal to cell left and right edges to prevent notches
		x = -plan.protoWidth/2 + plan.vddWidth/2;
		PortInst pi;
		pi = LayoutLib.newNodeInst(Tech.m1pin, x, y, G.DEF_SIZE, G.DEF_SIZE, 0, 
								   cell).getOnlyPortInst();
		LayoutLib.newArcInst(Tech.m1, plan.vddWidth, pi, leftCont);
		
		x = plan.protoWidth/2 - plan.vddWidth/2;
		pi = LayoutLib.newNodeInst(Tech.m1pin, x, y, G.DEF_SIZE, G.DEF_SIZE, 0,
								   cell).getOnlyPortInst();
		LayoutLib.newArcInst(Tech.m1, plan.vddWidth, pi, rightCont);

	}
	
	private void connectDiffs(PortInst[] a, PortInst[] b) {
		for (int i=0; i<a.length; i++) {
			LayoutLib.newArcInst(Tech.ndiff, G.DEF_SIZE, a[i], b[i]);
		}
	}
		
	public CapCell(Library lib, CapFloorplan instPlan) {
		this.plan = new ProtoPlan(instPlan);
		PortInst[] botDiffs = new PortInst[plan.numMosX]; 
		PortInst[] topDiffs = new PortInst[plan.numMosX]; 

		cell = Cell.newInstance(lib, "fillCap{lay}");
		double y = plan.botWellContY;
	
		PortInst[] lastCont = diffCont(y, plan, cell);
		for (int i=0; i<plan.numMosY; i++) {
			y += plan.mosPitchY/2;
			mos(botDiffs, topDiffs, y, plan, cell);
			connectDiffs(lastCont, botDiffs);
			y += plan.mosPitchY/2;
			lastCont = diffCont(y, plan, cell);
			connectDiffs(topDiffs, lastCont);
		}
		// Cover the sucker with well to eliminate notch errors
		LayoutLib.newNodeInst(Tech.pwell, 0, 0, plan.protoWidth, plan.protoHeight, 0, cell);
	}
	public int numVdd() {return plan.numMosY;}
	public int numGnd() {return plan.numMosY+1;}
	public double getVddWidth() {return plan.vddWidth;}
	public double getGndWidth() {return plan.gndWidth;}
	public Cell getCell() {return cell;}
}

//---------------------------------- CapLayer ---------------------------------
class CapLayer implements VddGndStraps {
	private CapCell capCell;
	private NodeInst capCellInst;
	private CapFloorplan plan; 

	public CapLayer(Library lib, CapFloorplan plan, Cell cell) {
		this.plan = plan;
		capCell = new CapCell(lib, plan); 

		double angle = plan.horizontal ? 0 : 90;
		capCellInst = LayoutLib.newNodeInst(capCell.getCell(), 0, 0, G.DEF_SIZE,
									 		G.DEF_SIZE, angle, cell);
	}

	public boolean isHorizontal() {return plan.horizontal;}
	public int numVdd() {return capCell.numVdd();}
	public PortInst getVdd(int n) {return capCellInst.findPortInst("vdd_"+n);}
	public double getVddCenter(int n) {
		Rectangle2D bounds = getVdd(n).getBounds();
		return plan.horizontal ? bounds.getCenterY() : bounds.getCenterX();
	}
	public double getVddWidth(int n) {return capCell.getVddWidth();}
	public int numGnd() {return capCell.numGnd();}
	public PortInst getGnd(int n) {return capCellInst.findPortInst("gnd_"+n);}
	public double getGndCenter(int n) {
		Rectangle2D bounds = getGnd(n).getBounds();
		return plan.horizontal ? bounds.getCenterY() : bounds.getCenterX();
	}
	public double getGndWidth(int n) {return capCell.getGndWidth();}

	public PrimitiveNode getPinType() {return Tech.m1pin;}
	public PrimitiveArc getMetalType() {return Tech.m1;}
	public double getCellWidth() {return plan.cellWidth;}
	public double getCellHeight() {return plan.cellHeight;}
	public int getLayerNumber() {return 1;}
}

// ---------------------------------- FillCell --------------------------------
class FillCell {
	private int vddNum, gndNum;
	
	private String vddName() {
		int n = vddNum++; 
		return "vdd" + (n==0 ? "" : ("_"+n)); 
	}
	private String gndName() {
		int n = gndNum++; 
		return "gnd" + (n==0 ? "" : ("_"+n)); 
	}
	
	public void exportPerimeter(VddGndStraps lay, Cell cell) {
		for (int i=0; i<lay.numGnd(); i++) {
			exportStripeEnds(i, lay, true, cell);				 
		}
		for (int i=0; i<lay.numVdd(); i++) {
			exportStripeEnds(i, lay, false, cell);				 
		}
	}
	private void exportStripeEnds(int n, VddGndStraps lay, boolean gnd, Cell cell) {
		PrimitiveNode pin = lay.getPinType();
		PrimitiveArc metal = lay.getMetalType();
		double edge = (lay.isHorizontal() ? lay.getCellWidth() : lay.getCellHeight())/2;
		double center = gnd ? lay.getGndCenter(n) : lay.getVddCenter(n);
		double width = gnd ? lay.getGndWidth(n) : lay.getVddWidth(n);
		PortInst pi = gnd ? lay.getGnd(n) : lay.getVdd(n);
		if (lay.isHorizontal()) {
			export(-edge, center, pin, metal, pi, width, 
				   gnd ? gndName() : vddName(), gnd, cell);	
			export(edge, center, pin, metal, pi, width, 
				   gnd ? gndName() : vddName(), gnd, cell);	
		} else {
			export(center, -edge, pin, metal, pi, width, 
				   gnd ? gndName() : vddName(), gnd, cell);	
			export(center, edge, pin, metal, pi, width, 
				   gnd ? gndName() : vddName(), gnd, cell);	
		}
	}
	private void export(double x, double y, PrimitiveNode pin, 
						PrimitiveArc metal, PortInst conn, double w, 
						String name, boolean gnd, Cell cell) {
		PortInst pi = LayoutLib.newNodeInst(pin, x, y, G.DEF_SIZE, G.DEF_SIZE, 
											0, cell).getOnlyPortInst();							 	
		G.noExtendArc(metal, w, conn, pi);
		Export e = Export.newInstance(cell, pi, name);
		e.setCharacteristic(gnd ? PortProto.Characteristic.GND : 
								  PortProto.Characteristic.PWR);
	}
	public void exportWiring(VddGndStraps lay, Cell cell) {
		for (int i=0; i<lay.numGnd(); i++) {
			exportStripeCenter(i, lay, true, cell);				 
		}
		for (int i=0; i<lay.numVdd(); i++) {
			exportStripeCenter(i, lay, false, cell);				 
		}
	}
	private void exportStripeCenter(int n, VddGndStraps lay, boolean gnd, Cell cell) {
		PrimitiveNode pin = lay.getPinType();
		PrimitiveArc metal = lay.getMetalType();
		double center = gnd ? lay.getGndCenter(n) : lay.getVddCenter(n);
		double width = gnd ? lay.getGndWidth(n) : lay.getVddWidth(n);
		PortInst pi = gnd ? lay.getGnd(n) : lay.getVdd(n);
		if (lay.isHorizontal()) {
			export(0, center, pin, metal, pi, width, 
				   gnd ? gndName() : vddName(), gnd, cell);	
		} else {
			export(center, 0, pin, metal, pi, width, 
				   gnd ? gndName() : vddName(), gnd, cell);	
		}
	}

	private String fillName(int lo, int hi, boolean wireLowest) {
		StringBuffer buf = new StringBuffer();
		buf.append("fill");
		if (lo!=1 || hi!=6) {
			for (int i=lo; i<=hi; i++)  buf.append(i);
		}

		if (wireLowest)  buf.append("w");
		buf.append("{lay}");
		return buf.toString();
	}
	
	private VddGndStraps[] findHoriVert(VddGndStraps lay1, VddGndStraps lay2) {
		if (lay1.isHorizontal()) {
			LayoutLib.error(lay2.isHorizontal(), "adjacent layers both horizontal");
			return new VddGndStraps[] {lay1, lay2};
		} else {
			LayoutLib.error(!lay2.isHorizontal(), "adjacent layers both vertical");
			return new VddGndStraps[] {lay2, lay1};
		}
	}	
	
	/** Move via's edge inside by 1 lambda if via's edge is on cell's edge */
	private static class ViaDim {
		public final double x, y, w, h;
		public ViaDim(VddGndStraps lay, double x, double y, double w, double h) {
			if (x+w/2 == lay.getCellWidth()/2) {
				w -= 1;
				x -= .5;
			} else if (x-w/2 == -lay.getCellWidth()/2) {
				w -= 1;
				x += .5;
			}
			if (y+h/2 == lay.getCellHeight()/2) {
				h -= 1;
				y -= .5;
			} else if (y-h/2 == -lay.getCellHeight()/2) {
				h -= 1;
				y += .5;
			}
			this.x = x;
			this.y = y;
			this.w = w;
			this.h = h;
		}
	}
	
	private void connectVddStraps(VddGndStraps horLay, int horNdx,
								  VddGndStraps verLay, int verNdx, Cell cell) {
		double w = verLay.getVddWidth(verNdx);
		double x = verLay.getVddCenter(verNdx);
		PrimitiveArc verMetal = verLay.getMetalType();
		PortInst verPort = verLay.getVdd(verNdx);
		double h = horLay.getVddWidth(horNdx);
		double y = horLay.getVddCenter(horNdx);
		PrimitiveArc horMetal = horLay.getMetalType();
		PrimitiveNode viaType = Tech.getViaFor(verMetal, horMetal);
		PortInst horPort = horLay.getVdd(horNdx);
		LayoutLib.error(viaType==null, "can't find via for metal layers");

		ViaDim d = new ViaDim(horLay, x, y, w, h);

		PortInst via = LayoutLib.newNodeInst(viaType, d.x, d.y, d.w, d.h, 0, 
		                                     cell).getOnlyPortInst();

		LayoutLib.newArcInst(horMetal, G.DEF_SIZE, horPort, via);
		LayoutLib.newArcInst(verMetal, G.DEF_SIZE, via, verPort);
	}

	private void connectGndStraps(VddGndStraps horLay, int horNdx,
								  VddGndStraps verLay, int verNdx, Cell cell) {
		double w = verLay.getGndWidth(verNdx);
		double x = verLay.getGndCenter(verNdx);
		PrimitiveArc verMetal = verLay.getMetalType();
		PortInst verPort = verLay.getGnd(verNdx);
		double h = horLay.getGndWidth(horNdx);
		double y = horLay.getGndCenter(horNdx);
		PrimitiveArc horMetal = horLay.getMetalType();
		PrimitiveNode viaType = Tech.getViaFor(verMetal, horMetal);
		PortInst horPort = horLay.getGnd(horNdx);
		LayoutLib.error(viaType==null, "can't find via for metal layers");

		ViaDim d = new ViaDim(horLay, x, y, w, h);

		PortInst via = LayoutLib.newNodeInst(viaType, d.x, d.y, d.w, d.h, 0, 
		 									 cell).getOnlyPortInst();

		LayoutLib.newArcInst(horMetal, G.DEF_SIZE, horPort, via);
		LayoutLib.newArcInst(verMetal, G.DEF_SIZE, via, verPort);
	}
	
	private void connectLayers(VddGndStraps loLayer, VddGndStraps hiLayer,
							   Cell cell) {
		VddGndStraps layers[] = findHoriVert(loLayer, hiLayer);
		VddGndStraps horLay = layers[0];
		VddGndStraps verLay = layers[1];
		for (int h=0; h<horLay.numVdd(); h++) {
			for (int v=0; v<verLay.numVdd(); v++) {
				connectVddStraps(horLay, h, verLay, v, cell);				
			}
		}
		for (int h=0; h<horLay.numGnd(); h++) {
			for (int v=0; v<verLay.numGnd(); v++) {
				connectGndStraps(horLay, h, verLay, v, cell);				
			}
		}
   	}
	
	private FillCell(Library lib, Floorplan[] plans, int botLayer, 
					 int topLayer, boolean wireLowest) {
		String name = fillName(botLayer, topLayer, wireLowest);
		Cell cell = Cell.newInstance(lib, name);
		VddGndStraps[] layers = new VddGndStraps[7];
		for (int i=topLayer; i>=botLayer; i--) {
			if (i==1) {
				layers[i] = new CapLayer(lib, (CapFloorplan) plans[i], cell); 
			} else {
				layers[i] = new MetalLayer(i, (MetalFloorplan)plans[i], cell);
			} 
			if (i!=topLayer) {
				// connect to upper level
				connectLayers(layers[i], layers[i+1], cell);
			}
		}
		if (layers[6]!=null) exportPerimeter(layers[6], cell);
		if (layers[5]!=null) exportPerimeter(layers[5], cell);
		if (wireLowest)  exportWiring(layers[botLayer], cell);
		
		double cellWidth = plans[topLayer].cellWidth;
		double cellHeight = plans[topLayer].cellHeight;
		LayoutLib.newNodeInst(Tech.essentialBounds,
							  -cellWidth/2, -cellHeight/2,
							  G.DEF_SIZE, G.DEF_SIZE, 180, cell);
		LayoutLib.newNodeInst(Tech.essentialBounds,
							  cellWidth/2, cellHeight/2,
							  G.DEF_SIZE, G.DEF_SIZE, 0, cell);
	}
	public static void newFillCell(Library lib, Floorplan[] plans, 
								   int botLayer, int topLayer, 
								   boolean wireLowest) {
		new FillCell(lib, plans, botLayer, topLayer, wireLowest);
	}
}

/**
 * Generate fill cells.
 * Create a library called fillCells. 
 */
public class FillLibGen extends Job {

	private void genFillCells() {
		Library lib = 
			LayoutLib.openLibForWrite("fillLib", "fillLib.elib");
		double width = 245; //245 80
		double height = 175; //175 100 
		boolean topHori = false;
		// m1 via = 4x4, m1 space = 6
		// m6 via = 5x5, m6 space = 8
		Floorplan[] plans = {
			null,
			new CapFloorplan(width, height, 			 !topHori),	// metal 1

//			new MetalFloorplan(width, height, 16, 16, 6,  topHori),	// metal 2
//			new MetalFloorplan(width, height, 16, 16, 6, !topHori),	// metal 3
//			new MetalFloorplan(width, height, 16, 16, 6,  topHori),	// metal 4
//			new MetalFloorplan(width, height, 16, 16, 6, !topHori),	// metal 5
//			new MetalFloorplan(width, height, 21, 21, 8,  topHori)	// metal 6

			new MetalFloorplan(width, height, 0, 0, 6,  topHori),	// metal 2
			new MetalFloorplan(width, height, 0, 0, 6, !topHori),	// metal 3
			new MetalFloorplan(width, height, 0, 0, 6,  topHori),	// metal 4
			new MetalFloorplan(width, height, 0, 0, 6, !topHori),	// metal 5
			new MetalFloorplan(width, height, 0, 0, 8,  topHori)	// metal 6
		};
		FillCell.newFillCell(lib, plans, 1, 2, true);
//		for (int i=1; i<=6; i++) {
//			for (int w=0; w<2; w++) {
//				boolean wireLowest = w==1;
//				if (!(wireLowest && i==1)) {
//					FillCell.newFillCell(lib, plans, i, 6, wireLowest);	
//				}
//			}
//		}
		Cell gallery = Gallery.makeGallery(lib);
	}
	
	public void doIt() {
		System.out.println("Begin FillCell");
		genFillCells();
		System.out.println("Done FillCell");
	}
	
	public FillLibGen() {
		super("Generate Fill Cell Library", User.tool, Job.Type.CHANGE, 
			  null, null, Job.Priority.ANALYSIS);
		startJob();
	}
}
