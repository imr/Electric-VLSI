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
	/** indices of Gnd pins and vias */
	public static final int GND_TL = 0;
	public static final int GND_TR = 1;
	public static final int GND_BL = 2;
	public static final int GND_BR = 3;
	/** indices of Vdd pins and vias */
	public static final int VDD_TL = 4;
	public static final int VDD_TR = 5;
	public static final int VDD_BL = 6;
	public static final int VDD_BR = 7;

	/** number of pins and vias */     		
	public static final int NUM_VIAS=8;

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

// ------------------------------ Floorplan -----------------------------------
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
class Floorplan {
										public final double cellWidth;
										public final double cellHeight;
										public final boolean horizontal;
	/** width Vdd wires */				public final double vddWidth;
	/** width Gnd wires */  			public final double gndWidth;
	/** no gap between Vdd wires */		public final boolean mergedVdd;
	/** if horizontal then y coordinate of top Vdd wire
	 *  if vertical then x coordinate of right Vdd wire */
	public final double vddCenter;
	/** if horizontal then y coordinate of top Gnd wire 
	 *  if vertical then x coordinate of right Gnd wire */ 
	public final double gndCenter;

	Floorplan(double cellWidth, double cellHeight,
			  double vddReserve, double gndReserve, 
			  double space, boolean horiz) {
		this.cellWidth = cellWidth;
		this.cellHeight = cellHeight;
		this.horizontal = horiz;
		mergedVdd = vddReserve==0;
		double cellSpace = horiz ? cellHeight : cellWidth;
		vddWidth = (cellSpace/2 - space - vddReserve) / 2;
		gndWidth = (cellSpace/2 - space - gndReserve) / 2;
		vddCenter = vddReserve/2 + vddWidth/2;
		gndCenter = vddReserve/2 + vddWidth + space + gndWidth/2;			
	}
}

// ------------------------------- FillLayerMetal -----------------------------
class FillLayerMetal {
	private static final PrimitiveArc[] METALS = 
		{null, Tech.m1, Tech.m2, Tech.m3, Tech.m4, Tech.m5, Tech.m6};
	private static final PrimitiveNode[] PINS = 
		{null, Tech.m1pin, Tech.m2pin, Tech.m3pin, Tech.m4pin, Tech.m5pin, 
		 Tech.m6pin};
	private final Floorplan plan;
	private final int layerNum;
	private final PrimitiveNode pin;
	private final PrimitiveArc metal;
	private final PortInst pins[] = new PortInst[G.NUM_VIAS];
	
	private void buildGnd(Floorplan plan, Cell cell) {
		double pinX, pinY;
		if (plan.horizontal) {
			pinX = plan.cellWidth/2 - plan.gndWidth/2;
			pinY = plan.gndCenter;				
		} else {
			pinX = plan.gndCenter;
			pinY = plan.cellHeight/2 - plan.gndWidth/2;
		}
		pins[G.GND_TL] = LayoutLib.newNodeInst(pin, -pinX, pinY, G.DEF_SIZE, 
											   G.DEF_SIZE, 0, cell
											   ).getOnlyPortInst();
		pins[G.GND_TR] = LayoutLib.newNodeInst(pin, pinX, pinY, G.DEF_SIZE, 
										       G.DEF_SIZE, 0, cell
										       ).getOnlyPortInst();
		pins[G.GND_BL] = LayoutLib.newNodeInst(pin, -pinX, -pinY, G.DEF_SIZE,
										       G.DEF_SIZE, 0, cell
										       ).getOnlyPortInst();
		pins[G.GND_BR] = LayoutLib.newNodeInst(pin, pinX, -pinY, G.DEF_SIZE, 
										       G.DEF_SIZE, 0, cell
										       ).getOnlyPortInst();
		if (plan.horizontal) {
			G.newArc(metal, plan.gndWidth, pins[G.GND_TL], pins[G.GND_TR]);
			G.newArc(metal, plan.gndWidth, pins[G.GND_BL], pins[G.GND_BR]);
		} else {
			G.newArc(metal, plan.gndWidth, pins[G.GND_BL], pins[G.GND_TL]);
			G.newArc(metal, plan.gndWidth, pins[G.GND_BR], pins[G.GND_TR]);
		}
	}
	
	private void buildVdd(Floorplan plan, Cell cell) {
		double pinX, pinY;
		if (plan.horizontal) {
			pinX = plan.cellWidth/2 - plan.vddWidth/2;
			pinY = plan.vddCenter;
		} else {
			pinX = plan.vddCenter;
			pinY = plan.cellHeight/2 - plan.vddWidth/2;
		}
		pins[G.VDD_TL] = LayoutLib.newNodeInst(pin, -pinX, pinY, G.DEF_SIZE, 
											   G.DEF_SIZE, 0, cell
											   ).getOnlyPortInst();
		pins[G.VDD_TR] = LayoutLib.newNodeInst(pin, pinX, pinY, G.DEF_SIZE, 
											   G.DEF_SIZE, 0, cell
											   ).getOnlyPortInst();
		pins[G.VDD_BL] = LayoutLib.newNodeInst(pin, -pinX, -pinY, G.DEF_SIZE,
											   G.DEF_SIZE, 0, cell
											   ).getOnlyPortInst();
		pins[G.VDD_BR] = LayoutLib.newNodeInst(pin, pinX, -pinY, G.DEF_SIZE, 
											   G.DEF_SIZE, 0, cell
											   ).getOnlyPortInst();
		if (plan.horizontal) {
			G.newArc(metal, plan.vddWidth, pins[G.VDD_TL], pins[G.VDD_TR]);
			G.newArc(metal, plan.vddWidth, pins[G.VDD_BL], pins[G.VDD_BR]);
		} else {
			G.newArc(metal, plan.vddWidth, pins[G.VDD_BL], pins[G.VDD_TL]);
			G.newArc(metal, plan.vddWidth, pins[G.VDD_BR], pins[G.VDD_TR]);
		}
		// If there is no space between the Vdd wires then connect them 
		// together so it will pass DRC.
		if (plan.mergedVdd) {
			G.newArc(metal, G.DEF_SIZE, pins[G.VDD_TL], pins[G.VDD_BR]);
		}
	}
	
	public FillLayerMetal(int layerNum, Floorplan plan, Cell cell) {
		this.plan = plan;
		this.layerNum = layerNum;
		metal = METALS[layerNum];
		pin = PINS[layerNum]; 
		buildGnd(plan, cell);
		buildVdd(plan, cell);
	}
	
	public PortInst getPin(int pinNdx) {return pins[pinNdx];}
	public PrimitiveArc getMetalType() {return metal;}
	public Floorplan getFloorplan() {return plan;}
	public int getLayerNumber() {return layerNum;}
	public void exportPerimeter(Cell cell) {
		double pinX, pinY;
		if (plan.horizontal) {
			pinX = plan.cellWidth/2;
			pinY = plan.gndCenter;
		} else {
			pinX = plan.gndCenter;
			pinY = plan.cellHeight/2;
		}
		String n = "gnd_"+layerNum;
		export(-pinX, pinY, pins[G.GND_TL], plan.gndWidth, n+"tl", true, cell);
		export(pinX, pinY, pins[G.GND_TR], plan.gndWidth, n+"tr", true, cell);
		export(-pinX, -pinY, pins[G.GND_BL], plan.gndWidth, n+"bl", true, cell);
		export(pinX, -pinY, pins[G.GND_BR], plan.gndWidth, n+"br", true, cell);

		if (plan.horizontal) {
			pinY = plan.vddCenter;
		} else {
			pinX = plan.vddCenter; 	 
		}
		n = "vdd_"+layerNum;
		export(-pinX, pinY, pins[G.VDD_TL], plan.vddWidth, n+"tl", false, cell);
		export(pinX, pinY, pins[G.VDD_TR], plan.vddWidth, n+"tr", false, cell);
		export(-pinX, -pinY, pins[G.VDD_BL], plan.vddWidth, n+"bl", false, cell);
		export(pinX, -pinY, pins[G.VDD_BR], plan.vddWidth, n+"br", false, cell);
	}
	private void export(double x, double y, PortInst conn, double w, 
						 String name, boolean gnd, Cell cell) {
		PortInst pi = LayoutLib.newNodeInst(pin, x, y, G.DEF_SIZE, G.DEF_SIZE, 
											0, cell).getOnlyPortInst();							 	
		G.noExtendArc(metal, w, conn, pi);
		Export e = Export.newInstance(cell, pi, name);
		e.setCharacteristic(gnd ? PortProto.Characteristic.GND : 
								  PortProto.Characteristic.PWR);
	}
	public void exportWiring(Cell cell) {
		// horizontal
		double pinX, pinY;
		if (plan.horizontal) {
			pinX = 0;
			pinY = plan.gndCenter;
		} else {
			pinX = plan.gndCenter;
			pinY = 0;
		}
		String n = "gnd_"+layerNum;
		export(pinX, pinY, pins[G.GND_TR], plan.gndWidth, n+"w1",true,cell);
		export(-pinX, -pinY, pins[G.GND_BL], plan.gndWidth, n+"w0",true,cell);
		if (plan.horizontal) {
			pinY = plan.vddCenter;
		} else {
			pinX = plan.vddCenter;
		}
		n = "vdd_"+layerNum;
		export(pinX, pinY, pins[G.VDD_TR], plan.vddWidth, n+"w1", true, cell);
		export(-pinX, -pinY, pins[G.VDD_BL], plan.vddWidth, n+"w0", true, cell);
	}
}

// ---------------------------------- FillLayerVias ---------------------------	
class FillLayerVias {
	// We really only need an inset of .5. However, .5 leaves via center 
	// .25 lambda aligned. This will cause CIF alignment errors for arcs
	// that connect to the center of via. 
	private static final double VIA_INSET = 1;
	private static final PrimitiveNode VIAS[] = 
		{null, Tech.m1m2, Tech.m2m3, Tech.m3m4, Tech.m4m5, Tech.m5m6};
	private final PortInst vias[] = new PortInst[G.NUM_VIAS];
		
	private int getLowLayerNumber(FillLayerMetal lay1, FillLayerMetal lay2) {
		int l1 = lay1.getLayerNumber();
		int l2 = lay2.getLayerNumber();
		LayoutLib.error(l1+1!=l2 && l2+1!=l1, "FillLayerVias: Layers not adjacent");
		return Math.min(l1, l2);
	}
	private Floorplan[] findHoriVert(FillLayerMetal lay1, FillLayerMetal lay2) {
		Floorplan floor1 = lay1.getFloorplan();
		Floorplan floor2 = lay2.getFloorplan();
		if (floor1.horizontal) {
			LayoutLib.error(floor2.horizontal, "adjacent layers both horizontal");
			return new Floorplan[] {floor1, floor2};
		} else {
			LayoutLib.error(!floor2.horizontal, "adjacent layers both vertical");
			return new Floorplan[] {floor2, floor1};
		}
	}

	public FillLayerVias(FillLayerMetal lay1, FillLayerMetal lay2, Cell cell) {
		PrimitiveNode via = VIAS[getLowLayerNumber(lay1, lay2)];
		Floorplan[] floorHV = findHoriVert(lay1, lay2);
		Floorplan floorH = floorHV[0];
		Floorplan floorV = floorHV[1];

		// create Gnd vias
		double viaW = floorV.gndWidth - VIA_INSET;
		double viaH = floorH.gndWidth - VIA_INSET;
		double viaX = floorV.gndCenter - VIA_INSET/2;
		double viaY = floorH.gndCenter - VIA_INSET/2;
		vias[G.GND_TL] = LayoutLib.newNodeInst(via, -viaX, viaY, viaW, viaH, 
											 0, cell).getOnlyPortInst();
		vias[G.GND_TR] = LayoutLib.newNodeInst(via, viaX, viaY, viaW, viaH, 
											 0, cell).getOnlyPortInst();
		vias[G.GND_BL] = LayoutLib.newNodeInst(via, -viaX, -viaY, viaW, viaH, 
											 0, cell).getOnlyPortInst();
		vias[G.GND_BR] = LayoutLib.newNodeInst(via, viaX, -viaY, viaW, viaH, 
											 0, cell).getOnlyPortInst();
		// create Vdd vias
		viaW = floorV.vddWidth - VIA_INSET;
		viaH = floorH.vddWidth - VIA_INSET;
		viaX = floorV.vddCenter + VIA_INSET/2;
		viaY = floorH.vddCenter + VIA_INSET/2;
		vias[G.VDD_TL] = LayoutLib.newNodeInst(via, -viaX, viaY, viaW, viaH, 
											 0, cell).getOnlyPortInst();
		vias[G.VDD_TR] = LayoutLib.newNodeInst(via, viaX, viaY, viaW, viaH, 
											 0, cell).getOnlyPortInst();
		vias[G.VDD_BL] = LayoutLib.newNodeInst(via, -viaX, -viaY, viaW, viaH, 
											 0, cell).getOnlyPortInst();
		vias[G.VDD_BR] = LayoutLib.newNodeInst(via, viaX, -viaY, viaW, viaH, 
											 0, cell).getOnlyPortInst();
	}
	public PortInst getVia(int viaNdx) {return vias[viaNdx];}
}

/**
 * Generate fill cells.
 * Create a library called fillCells. 
 */
public class FillCell extends Job {
	private String fillName(int lo, int hi, boolean wireLowest) {
		StringBuffer buf = new StringBuffer();
		buf.append("fill");
		for (int i=hi; i>=lo; i--)  buf.append(i);

		if (wireLowest)  buf.append("w");
		buf.append("{lay}");
		return buf.toString();
	}
	private void genFillCell(Library lib, Floorplan[] plans, int botLayer, 
							 int topLayer, boolean wireLowest) {
		String name = fillName(botLayer, topLayer, wireLowest);
		Cell cell = Cell.newInstance(lib, name);
		FillLayerMetal[] metals = new FillLayerMetal[7];
		FillLayerVias[] vias = new FillLayerVias[7];
		for (int i=topLayer; i>=botLayer; i--) {
			metals[i] = new FillLayerMetal(i, plans[i], cell);
			if (i!=topLayer) {
				// connect to upper level
				vias[i] = new FillLayerVias(metals[i], metals[i+1], cell);
				PrimitiveArc loMetal = metals[i].getMetalType();
				PrimitiveArc hiMetal = metals[i+1].getMetalType();
				for (int v=0; v<G.NUM_VIAS; v++) {
					G.newArc(loMetal, G.DEF_SIZE, 
							 metals[i].getPin(v), vias[i].getVia(v));
					G.newArc(hiMetal, G.DEF_SIZE, 
							 metals[i+1].getPin(v), vias[i].getVia(v));
				}
			}
		}
		if (metals[6]!=null) metals[6].exportPerimeter(cell);
		if (metals[5]!=null) metals[5].exportPerimeter(cell);
		if (wireLowest && metals[botLayer]!=null) {
			metals[botLayer].exportWiring(cell);
		}
	}
	private void genFillCells() {
		Library lib = 
			LayoutLib.openLibForWrite("fillLib", "fillLib.elib");
		double width = 60;
		double height = 80; 
		boolean topHori = false;
									 	
		Floorplan[] plans = {
			null,
			new Floorplan(width, height, 6, 6, 6, !topHori),	// metal 1
			new Floorplan(width, height, 6, 6, 6,  topHori),	// metal 2
			new Floorplan(width, height, 6, 6, 6, !topHori),	// metal 3
			new Floorplan(width, height, 6, 6, 6,  topHori),	// metal 4
			new Floorplan(width, height, 6, 6, 6, !topHori),	// metal 5
			new Floorplan(width, height, 8, 8, 8,  topHori)		// metal 6
		};
		//genFillCell(lib, plans, 5, 5, true);
		for (int i=1; i<=6; i++) {
			for (int w=0; w<2; w++) {
				boolean wireLowest = w==1;
				genFillCell(lib, plans, i, 6, wireLowest);	
			}
		}
		Cell gallery = Gallery.makeGallery(lib);
	}
	
	public void doIt() {
		System.out.println("Begin FillCell");
		genFillCells();
		System.out.println("Done FillCell");
	}
	
	public FillCell() {
		super("Generate Fill Cell Library", User.tool, Job.Type.CHANGE, 
			  null, null, Job.Priority.ANALYSIS);
		startJob();
	}
}
