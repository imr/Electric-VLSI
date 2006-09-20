/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: FillGeneratorTool.java
 *
 * Copyright (c) 2006 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.generator.layout.fill;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.geometry.*;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.tool.generator.layout.*;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.Job;

import java.util.*;

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

abstract class MetalFloorplanBase extends Floorplan
{
	/** width Vdd wires */				public double vddWidth;
	/** width Gnd wires */  			public double gndWidth;

    MetalFloorplanBase(double cellWidth, double cellHeight, boolean horiz)
    {
        super(cellWidth, cellHeight, horiz);
        vddWidth = gndWidth = 0;
    }
}

// ------------------------------ MetalFloorplanFlex ------------------------------
// Similar to Metalfloor but number of power/gnd lines is determined by cell size
class MetalFloorplanFlex extends MetalFloorplanBase {

    public final double minWidth, space, vddReserve, gndReserve;

    MetalFloorplanFlex(double cellWidth, double cellHeight,
                       double vddReserve, double gndReserve, double space,
                       double vddW, double gndW,
                       boolean horiz)
    {
        super(cellWidth, cellHeight, horiz);
        this.vddWidth = vddW; //27;
        this.gndWidth = gndW; //20;
        this.space = space;
        this.vddReserve = vddReserve;
        this.gndReserve = gndReserve;
        minWidth = vddReserve + gndReserve + 2*space + 2*gndWidth + 2*vddWidth;
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
class MetalFloorplan extends MetalFloorplanBase {
	/** no gap between Vdd wires */		public final boolean mergedVdd;
	/** if horizontal then y coordinate of top Vdd wire
	 *  if vertical then x coordinate of right Vdd wire */
	public final double vddCenter;
	/** if horizontal then y coordinate of top Gnd wire
	 *  if vertical then x coordinate of right Gnd wire */
	public final double gndCenter;

	public final double coverage;

	private double roundDownOneLambda(double x) {
		return Math.floor(x);
	}
	// Round metal widths down to multiples of 1 lambda resolution.
	// Then metal center can be on 1/2 lambda grid without problems.
	MetalFloorplan(double cellWidth, double cellHeight,
			       double vddReserve, double gndReserve,
			       double space, boolean horiz) {
		super(cellWidth, cellHeight, horiz);
		mergedVdd = vddReserve==0;
		double cellSpace = horiz ? cellHeight : cellWidth;
		double metalSpace = cellSpace - 2*space - vddReserve - gndReserve;

		// gnd is always in two pieces
		gndWidth = roundDownOneLambda(metalSpace / 4);
		gndCenter = cellSpace/2 - gndReserve/2 - gndWidth/2;

		// vdd may be one or two pieces
		if (mergedVdd) {
			vddWidth =  gndWidth*2;
			vddCenter = 0;
		} else {
			vddWidth = gndWidth;
			vddCenter = vddReserve/2 + vddWidth/2;
		}

		// compute coverage statistics
		double cellArea = cellWidth * cellHeight;
		double strapLength = horiz ? cellWidth : cellHeight;
		double vddArea = (mergedVdd ? 1 : 2) * vddWidth * strapLength;
		double gndArea = 2 * gndWidth * strapLength;
		coverage = (vddArea + gndArea)/cellArea;
	}

// Save this code in case I need to replicate LoCo FillCell exactly
//	MetalFloorplan(double cellWidth, double cellHeight,
//				   double vddReserve, double gndReserve,
//				   double space, boolean horiz) {
//		super(cellWidth, cellHeight, horiz);
//		mergedVdd = vddReserve==0;
//		double cellSpace = horiz ? cellHeight : cellWidth;
//		if (mergedVdd) {
//			double w = cellSpace/2 - space - vddReserve;
//			vddWidth =  roundDownOneLambda(w);
//			vddCenter = 0;
//		} else {
//			double w = (cellSpace/2 - space - vddReserve) / 2;
//			vddWidth = roundDownOneLambda(w);
//			vddCenter = vddReserve/2 + vddWidth/2;
//		}
//		double vddEdge = vddCenter + vddWidth/2;
//		double w = cellSpace/2 - vddEdge - space - gndReserve/2;
//		gndWidth = roundDownOneLambda(w);
//		gndCenter = vddEdge + space + gndWidth/2;
//
//		// compute coverage statistics
//		double cellArea = cellWidth * cellHeight;
//		double strapLength = horiz ? cellWidth : cellHeight;
//		double vddArea = (mergedVdd ? 1 : 2) * vddWidth * strapLength;
//		double gndArea = 2 * gndWidth * strapLength;
//		coverage = (vddArea + gndArea)/cellArea;
//	}
}

// ------------------------------- ExportBars ---------------------------------
class ExportBar
{
    PortInst[] ports = null;
    Double center = null;

    ExportBar(PortInst p1, PortInst p2, double c)
    {
        ports = new PortInst[2];
        ports[0] = p1;
        ports[1] = p2;
        center = (c);  // autoboxing
    }
}

class MetalLayer implements VddGndStraps {
	protected final MetalFloorplanBase plan;
	protected final int layerNum;
	protected final PrimitiveNode pin;
	protected final ArcProto metal;
    protected ArrayList<ExportBar> vddBars = new ArrayList<ExportBar>();
    protected ArrayList<ExportBar> gndBars = new ArrayList<ExportBar>();

    public boolean addExtraArc() { return true; }

	private void buildGnd(Cell cell) {
		double pinX, pinY;
        MetalFloorplan plan = (MetalFloorplan)this.plan;

		if (plan.horizontal) {
			pinX = plan.cellWidth/2; // - plan.gndWidth/2;
			pinY = plan.gndCenter;
		} else {
			pinX = plan.gndCenter;
			pinY = plan.cellHeight/2; // - plan.gndWidth/2;
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
			G.noExtendArc(metal, plan.gndWidth, tl, tr);
			G.noExtendArc(metal, plan.gndWidth, bl, br);
            gndBars.add(new ExportBar(bl, br, -plan.gndCenter));
            gndBars.add(new ExportBar(tl, tr, plan.gndCenter));
		} else {
			G.noExtendArc(metal, plan.gndWidth, bl, tl);
			G.noExtendArc(metal, plan.gndWidth, br, tr);
            gndBars.add(new ExportBar(bl, tl, -plan.gndCenter));
            gndBars.add(new ExportBar(br, tr, plan.gndCenter));
		}
	}

	private void buildVdd(Cell cell) {
		double pinX, pinY;
        MetalFloorplan plan = (MetalFloorplan)this.plan;

		if (plan.horizontal) {
			pinX = plan.cellWidth/2; // - plan.vddWidth/2;
			pinY = plan.vddCenter;
		} else {
			pinX = plan.vddCenter;
			pinY = plan.cellHeight/2; // - plan.vddWidth/2;
		}
		if (plan.mergedVdd) {
			PortInst tr = LayoutLib.newNodeInst(pin, pinX, pinY, G.DEF_SIZE,
												G.DEF_SIZE, 0, cell
												).getOnlyPortInst();
			PortInst bl = LayoutLib.newNodeInst(pin, -pinX, -pinY, G.DEF_SIZE,
												G.DEF_SIZE, 0, cell
												).getOnlyPortInst();
			G.noExtendArc(metal, plan.vddWidth, bl, tr);
            vddBars.add(new ExportBar(bl, tr, plan.vddCenter));
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
				G.noExtendArc(metal, plan.vddWidth, tl, tr);
				G.noExtendArc(metal, plan.vddWidth, bl, br);
                vddBars.add(new ExportBar(bl, br, -plan.vddCenter));
                vddBars.add(new ExportBar(tl, tr, plan.vddCenter));
			} else {
				G.noExtendArc(metal, plan.vddWidth, bl, tl);
				G.noExtendArc(metal, plan.vddWidth, br, tr);
                vddBars.add(new ExportBar(bl, tl, -plan.vddCenter));
                vddBars.add(new ExportBar(br, tr, plan.vddCenter));
			}
		}
	}

    /** It has to be protected to be overwritten by sub classes */
    protected void buildGndAndVdd(Cell cell)
    {
		buildGnd(cell);
		buildVdd(cell);
    }

	public MetalLayer(int layerNum, Floorplan plan, Cell cell) {
		this.plan = (MetalFloorplanBase)plan;
		this.layerNum = layerNum;
		metal = METALS[layerNum];
		pin = PINS[layerNum];
        buildGndAndVdd(cell);
	}

    public boolean isHorizontal() {return plan.horizontal;}
    public int numVdd() {return vddBars.size();}
    public double getVddCenter(int n) {
		return (vddBars.get(n).center); // autoboxing
	}
    public PortInst getVdd(int n, int pos)
    {return vddBars.get(n).ports[pos];}
    public double getVddWidth(int n) {return plan.vddWidth;}
    public int numGnd() {return gndBars.size();}
    public double getGndCenter(int n) {
		return (gndBars.get(n).center); // autoboxing
	}
    public PortInst getGnd(int n, int pos) {return gndBars.get(n).ports[pos];}
    public double getGndWidth(int n) {return (plan).gndWidth;}

    public PrimitiveNode getPinType() {return pin;}
	public ArcProto getMetalType() {return metal;}
	public double getCellWidth() {return plan.cellWidth;}
	public double getCellHeight() {return plan.cellHeight;}
	public int getLayerNumber() {return layerNum;}
}

// ------------------------------- MetalLayerFlex -----------------------------

class MetalLayerFlex extends MetalLayer {

    public MetalLayerFlex(int layerNum, Floorplan plan, Cell cell) {
        super(layerNum, plan, cell);
	}

    public boolean addExtraArc() { return false; } // For automatic fill generator no extra arcs are wanted.
    protected void buildGndAndVdd(Cell cell) {
		double pinX, pinY;
        double limit = 0;
        MetalFloorplanFlex plan = (MetalFloorplanFlex)this.plan;

        if (plan.horizontal)
        {
            limit = plan.cellHeight/2;
        }
        else
        {
            limit = plan.cellWidth/2;
        }

        double position = 0;
        int i = 0;

        while (position < limit)
        {
            boolean even = (i%2==0);
            double maxDelta = 0, pos = 0;

            if (even)
            {
                maxDelta = plan.vddReserve/2 + plan.vddWidth;
                pos = plan.vddReserve/2 + plan.vddWidth/2 + position;
            }
            else
            {
                maxDelta = plan.gndReserve/2 + plan.gndWidth;
                pos = plan.gndReserve/2 + plan.gndWidth/2 + position;
            }

            if (position + maxDelta > limit) return; // border was reached

            if (plan.horizontal)
            {
                pinY =  pos;
                pinX = plan.cellWidth/2;
            }
            else
            {
                pinX = pos;
                pinY = plan.cellHeight/2;
            }

            // Vdd if even, gnd if odd
            if (!even)
                addBars(cell, pinX, pinY, plan.gndWidth, gndBars);
            else
                addBars(cell, pinX, pinY, plan.vddWidth, vddBars);

            if (even)
            {
                maxDelta = plan.vddReserve/2 + plan.vddWidth + plan.space + plan.gndWidth;
                pos = plan.vddReserve/2 + plan.vddWidth + plan.space + plan.gndWidth/2 + position;
            }
            else
            {
                maxDelta = plan.gndReserve/2 + plan.gndWidth + plan.space + plan.vddWidth;
                pos = plan.gndReserve/2 + plan.gndWidth + plan.space + plan.vddWidth/2 + position;
            }

            if (position + maxDelta > limit) return; // border was reached

            if (plan.horizontal)
                pinY = pos;
            else
                pinX = pos;

            // Gnd if even, vdd if odd
            if (!even)
            {
                addBars(cell, pinX, pinY, plan.vddWidth, vddBars);
                position = ((plan.horizontal)?pinY:pinX) + plan.vddWidth/2 + plan.vddReserve/2;
            }
            else
            {
                addBars(cell, pinX, pinY, plan.gndWidth, gndBars);
                position = ((plan.horizontal)?pinY:pinX) + plan.gndWidth/2 + plan.gndReserve/2;
            }
            i++;
        }
	}

    private void addBars(Cell cell, double pinX, double pinY, double width, ArrayList<ExportBar> bars)
    {

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

        double center = 0;

        if (plan.horizontal) {
            G.noExtendArc(metal, width, tl, tr);
            G.noExtendArc(metal, width, bl, br);
            center = pinY;
            bars.add(new ExportBar(bl, br, -center));
            bars.add(new ExportBar(tl, tr, center));
        } else {
            G.noExtendArc(metal, width, bl, tl);
            G.noExtendArc(metal, width, br, tr);
            center = pinX;
            bars.add(new ExportBar(bl, tl, -center));
            bars.add(new ExportBar(br, tr, center));
        }
    }
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
	/** All the fields in ProtoPlan assume that metal1 runs horizontally
	 *  since that is how we build CapCell */
	private static class ProtoPlan {
		private final double MAX_MOS_WIDTH = 40;
		private final double SEL_WIDTH_OF_NDM1 =
			Tech.getDiffContWidth() +
		    Tech.selectSurroundDiffInActiveContact()*2;
		private final double SEL_TO_MOS =
			Tech.selectSurroundDiffAlongGateInTrans();

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

			// compute number of MOS's bottom to top
			mosPitchY = gndWidth + 2*vddGndSpace + vddWidth;
			gateLength = mosPitchY - gndWidth - 2;
			numMosY = (int) Math.floor((protoHeight-Tech.getWellWidth())/mosPitchY);
			botWellContY = - numMosY * mosPitchY / 2;

			// min distance from left Cell edge to center of leftmost diffusion
			// contact.
			double cellEdgeToDiffContCenter =
				Tech.getWellSurroundDiff() + Tech.getDiffContWidth()/2;
			// min distance from left Cell Edge to center of leftmost poly
			// contact.
			double polyContWidth = Math.floor(gateLength / Tech.getP1M1Width()) *
			                       Tech.getP1M1Width();
			double cellEdgeToPolyContCenter =
				Tech.getP1ToP1Space()/2 + polyContWidth/2;
			// diffusion and poly contact centers line up
			double cellEdgeToContCenter = Math.max(cellEdgeToDiffContCenter,
					                               cellEdgeToPolyContCenter);

			// compute number of MOS's left to right
			//double availForCap = protoWidth - 2*(SEL_TO_CELL_EDGE + SEL_WIDTH_OF_NDM1/2);
			double availForCap = protoWidth - 2*cellEdgeToContCenter;
			double numMosD = availForCap /
							 (MAX_MOS_WIDTH + SEL_WIDTH_OF_NDM1 + 2*SEL_TO_MOS);
			numMosX = (int) Math.ceil(numMosD);
			double mosWidth1 = availForCap/numMosX - SEL_WIDTH_OF_NDM1 - 2*SEL_TO_MOS;
			// round down mos Width to integral number of lambdas
			gateWidth = Math.floor(mosWidth1);
			mosPitchX = gateWidth + SEL_WIDTH_OF_NDM1 + 2*SEL_TO_MOS;
			leftWellContX = - numMosX * mosPitchX / 2;

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
	private PortInst[] diffCont(double y, ProtoPlan plan, Cell cell,
	                            StdCellParams stdCell) {
		PortInst[] conts = new PortInst[plan.numMosX];
		double x = - plan.numMosX * plan.mosPitchX / 2;
		PortInst wellCont = LayoutLib.newNodeInst(Tech.pwm1, x, y, G.DEF_SIZE,
										 		  G.DEF_SIZE, 0, cell
										 		  ).getOnlyPortInst();
		Export e = Export.newInstance(cell, wellCont,
		                              stdCell.getGndExportName()+"_"+gndNum++);
		e.setCharacteristic(stdCell.getGndExportRole());

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
					 ProtoPlan plan, Cell cell, StdCellParams stdCell) {
		final double POLY_CONT_HEIGHT = plan.vddWidth + 1;
		double x = plan.leftWellContX;
		PortInst poly = LayoutLib.newNodeInst(Tech.p1m1, x, y, POLY_CONT_WIDTH,
											  POLY_CONT_HEIGHT, 0, cell
											  ).getOnlyPortInst();
		PortInst leftCont = poly;
		Export e = Export.newInstance(cell, poly,
		                              stdCell.getVddExportName()+"_"+vddNum++);
		e.setCharacteristic(stdCell.getVddExportRole());

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

	double roundToHalfLambda(double x) {
		return Math.rint(x * 2) / 2;
	}

	// The height of a MOS diff contact is 1/2 lambda. Therefore, using the
	// center for diffusion arcs always generates CIF resolution errors
	private void newDiffArc(PortInst p1, PortInst p2) {
        EPoint p1P = p1.getCenter();
		double x = p1P.getX(); // LayoutLib.roundCenterX(p1);
		double y1 = roundToHalfLambda(p1P.getY()); // LayoutLib.roundCenterY(p1));
		double y2 = roundToHalfLambda(LayoutLib.roundCenterY(p2));

		LayoutLib.newArcInst(Tech.ndiff, LayoutLib.DEF_SIZE, p1, x, y1, p2, x, y2);
	}

	private void connectDiffs(PortInst[] a, PortInst[] b) {
		for (int i=0; i<a.length; i++) {
			//LayoutLib.newArcInst(Tech.ndiff, G.DEF_SIZE, a[i], b[i]);
			newDiffArc(a[i], b[i]);
		}
	}

	public CapCell(Library lib, CapFloorplan instPlan, StdCellParams stdCell) {
		this.plan = new ProtoPlan(instPlan);
		PortInst[] botDiffs = new PortInst[plan.numMosX];
		PortInst[] topDiffs = new PortInst[plan.numMosX];

		String nameExt = stdCell.getVddExportName().equals("vdd") ? "" : "_pwr";
		cell = Cell.newInstance(lib, "fillCap"+nameExt+"{lay}");
		double y = plan.botWellContY;

		PortInst[] lastCont = diffCont(y, plan, cell, stdCell);
		for (int i=0; i<plan.numMosY; i++) {
			y += plan.mosPitchY/2;
			mos(botDiffs, topDiffs, y, plan, cell, stdCell);
			connectDiffs(lastCont, botDiffs);
			y += plan.mosPitchY/2;
			lastCont = diffCont(y, plan, cell, stdCell);
			connectDiffs(topDiffs, lastCont);
		}
		// Cover the sucker with well to eliminate notch errors
		LayoutLib.newNodeInst(Tech.pwell, 0, 0, plan.protoWidth,
		                      plan.protoHeight, 0, cell);
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
	private String vddName, gndName;

    public boolean addExtraArc() { return true; }

	public CapLayer(CapFloorplan plan, CapCell capCell, Cell cell,
                    StdCellParams stdCell) {
		this.plan = plan;
		this.capCell = capCell;
		vddName = stdCell.getVddExportName();
		gndName = stdCell.getGndExportName();

		double angle = plan.horizontal ? 0 : 90;
		capCellInst = LayoutLib.newNodeInst(capCell.getCell(), 0, 0, G.DEF_SIZE,
									 		G.DEF_SIZE, angle, cell);
	}

	public boolean isHorizontal() {return plan.horizontal;}
	public int numVdd() {return capCell.numVdd();}
	public PortInst getVdd(int n, int pos) {
		return capCellInst.findPortInst(vddName+"_"+n);
	}
	public double getVddCenter(int n) {
        EPoint center = getVdd(n, 0).getCenter();
		return plan.horizontal ? center.getY() : center.getX();
	}
	public double getVddWidth(int n) {return capCell.getVddWidth();}
	public int numGnd() {return capCell.numGnd();}
	public PortInst getGnd(int n, int pos) {
		return capCellInst.findPortInst(gndName+"_"+n);
	}
	public double getGndCenter(int n) {
        EPoint center = getGnd(n, 0).getCenter();
		return plan.horizontal ? center.getY() : center.getX();
	}
	public double getGndWidth(int n) {return capCell.getGndWidth();}

	public PrimitiveNode getPinType() {return Tech.m1pin;}
	public ArcProto getMetalType() {return Tech.m1;}
	public double getCellWidth() {return plan.cellWidth;}
	public double getCellHeight() {return plan.cellHeight;}
	public int getLayerNumber() {return 1;}
}

// ---------------------------------- FillCell --------------------------------
class FillCell {
	private int vddNum, gndNum;
	private String vddNm, gndNm;

	private String vddName() {
		int n = vddNum++;
		return vddNm + (n==0 ? "" : ("_"+n));
	}
	private String gndName() {
		int n = gndNum++;
		return gndNm + (n==0 ? "" : ("_"+n));
	}

	public void exportPerimeter(VddGndStraps lay, Cell cell,
	StdCellParams stdCell) {
		for (int i=0; i<lay.numGnd(); i++) {
			exportStripeEnds(i, lay, true, cell, stdCell);
		}
		for (int i=0; i<lay.numVdd(); i++) {
			exportStripeEnds(i, lay, false, cell, stdCell);
		}
	}
	private void exportStripeEnds(int n, VddGndStraps lay, boolean gnd, Cell cell,
	StdCellParams stdCell) {
		PrimitiveNode pin = lay.getPinType();
		ArcProto metal = lay.getMetalType();
		double edge = (lay.isHorizontal() ? lay.getCellWidth() : lay.getCellHeight())/2;
		double center = gnd ? lay.getGndCenter(n) : lay.getVddCenter(n);
		double width = gnd ? lay.getGndWidth(n) : lay.getVddWidth(n);
		PortInst piLeft = gnd ? lay.getGnd(n, 0) : lay.getVdd(n, 0);
        PortInst piRight = gnd ? lay.getGnd(n, 1) : lay.getVdd(n, 1);
		if (lay.isHorizontal()) {
			export(-edge, center, pin, metal, piLeft, width,
				   gnd ? gndName() : vddName(), gnd, cell, stdCell, lay.addExtraArc());
			export(edge, center, pin, metal, piRight, width,
				   gnd ? gndName() : vddName(), gnd, cell, stdCell, lay.addExtraArc());
		} else {
			export(center, -edge, pin, metal, piLeft, width,
				   gnd ? gndName() : vddName(), gnd, cell, stdCell, lay.addExtraArc());
			export(center, edge, pin, metal, piRight, width,
				   gnd ? gndName() : vddName(), gnd, cell, stdCell, lay.addExtraArc());
		}
	}
	private void export(double x, double y, PrimitiveNode pin,
						ArcProto metal, PortInst conn, double w,
						String name, boolean gnd, Cell cell,
						StdCellParams stdCell, boolean withExtraArc)
    {
        Export e = null;
        if (false) // withExtraArc)
        {
            PortInst pi = LayoutLib.newNodeInst(pin, x, y, G.DEF_SIZE, G.DEF_SIZE,
                                                0, cell).getOnlyPortInst();
            G.noExtendArc(metal, w, conn, pi);
            e = Export.newInstance(cell, pi, name);
        }
        else
            e = Export.newInstance(cell, conn, name);
		e.setCharacteristic(gnd ? stdCell.getGndExportRole() :
								  stdCell.getVddExportRole());
	}
	public void exportWiring(VddGndStraps lay, Cell cell, StdCellParams stdCell) {
		for (int i=0; i<lay.numGnd(); i++) {
			exportStripeCenter(i, lay, true, cell, stdCell);
		}
		for (int i=0; i<lay.numVdd(); i++) {
			exportStripeCenter(i, lay, false, cell, stdCell);
		}
	}
	private void exportStripeCenter(int n, VddGndStraps lay, boolean gnd, Cell cell,
	StdCellParams stdCell) {
		PrimitiveNode pin = lay.getPinType();
		ArcProto metal = lay.getMetalType();
		double center = gnd ? lay.getGndCenter(n) : lay.getVddCenter(n);
		double width = gnd ? lay.getGndWidth(n) : lay.getVddWidth(n);
		PortInst pi = gnd ? lay.getGnd(n, 0) : lay.getVdd(n, 0); // Doesn't matter which bar end is taken
		if (lay.isHorizontal()) {
			export(0, center, pin, metal, pi, width,
				   gnd ? gndName() : vddName(), gnd, cell, stdCell, lay.addExtraArc());
		} else {
			export(center, 0, pin, metal, pi, width,
				   gnd ? gndName() : vddName(), gnd, cell, stdCell, lay.addExtraArc());
		}
	}

	private String fillName(int lo, int hi, boolean wireLowest,
						    StdCellParams stdCell) {
		StringBuffer buf = new StringBuffer();
		buf.append("fill");
		if (lo!=1 || hi!=6) {
			for (int i=lo; i<=hi; i++)  buf.append(i);
		}

		if (wireLowest)  buf.append("w");
		if (!stdCell.getVddExportName().equals("vdd")) buf.append("_pwr");
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
		ArcProto verMetal = verLay.getMetalType();
        // Try to select the closest pin in the other layer , even to the left, odd to the right
		PortInst verPort = (horNdx%2==0) ? verLay.getVdd(verNdx, 0) : verLay.getVdd(verNdx, 1);
		double h = horLay.getVddWidth(horNdx);
		double y = horLay.getVddCenter(horNdx);
		ArcProto horMetal = horLay.getMetalType();
		PrimitiveNode viaType = Tech.getViaFor(verMetal, horMetal);
		PortInst horPort = (verNdx%2==0) ? horLay.getVdd(horNdx, 0) : horLay.getVdd(horNdx, 1);
		LayoutLib.error(viaType==null, "can't find via for metal layers");

		ViaDim d = new ViaDim(horLay, x, y, w, h);

		PortInst via = LayoutLib.newNodeInst(viaType, d.x, d.y, d.w, d.h, 0,
		                                     cell).getOnlyPortInst();

        G.noExtendArc(horMetal, h, horPort, via);
        G.noExtendArc(verMetal, w, via, verPort);
//		LayoutLib.newArcInst(horMetal, G.DEF_SIZE, horPort, via);
//		LayoutLib.newArcInst(verMetal, G.DEF_SIZE, via, verPort);
	}

	private void connectGndStraps(VddGndStraps horLay, int horNdx,
								  VddGndStraps verLay, int verNdx, Cell cell) {
		double w = verLay.getGndWidth(verNdx);
		double x = verLay.getGndCenter(verNdx);
		ArcProto verMetal = verLay.getMetalType();
        // Try to select the closest pin in the other layer , even to the left, odd to the right
		PortInst verPort = (horNdx%2==0) ? verLay.getGnd(verNdx, 0) : verLay.getGnd(verNdx, 1);
		double h = horLay.getGndWidth(horNdx);
		double y = horLay.getGndCenter(horNdx);
		ArcProto horMetal = horLay.getMetalType();
		PrimitiveNode viaType = Tech.getViaFor(verMetal, horMetal);
		PortInst horPort = (verNdx%2==0) ? horLay.getGnd(horNdx, 0) : horLay.getGnd(horNdx, 1);
		LayoutLib.error(viaType==null, "can't find via for metal layers");

		ViaDim d = new ViaDim(horLay, x, y, w, h);

		PortInst via = LayoutLib.newNodeInst(viaType, d.x, d.y, d.w, d.h, 0,
		 									 cell).getOnlyPortInst();

        G.noExtendArc(horMetal, h, horPort, via);
        G.noExtendArc(verMetal, w, via, verPort);
//		LayoutLib.newArcInst(horMetal, G.DEF_SIZE, horPort, via);
//		LayoutLib.newArcInst(verMetal, G.DEF_SIZE, via, verPort);
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

	protected Cell makeFillCell1(Library lib, Floorplan[] plans, int botLayer,
                               int topLayer, CapCell capCell, boolean wireLowest,
                               StdCellParams stdCell, boolean metalFlex, boolean hierFlex) {
		String name = fillName(botLayer, topLayer, wireLowest, stdCell);
		Cell cell = Cell.newInstance(lib, name);
		VddGndStraps[] layers = new VddGndStraps[7];
		for (int i=topLayer; i>=botLayer; i--) {
			if (i==1) {
				layers[i] = new CapLayer((CapFloorplan) plans[i], capCell,
				                         cell, stdCell);
			} else {
                if (metalFlex && !hierFlex)
				    layers[i] = new MetalLayerFlex(i, plans[i], cell);
                else
                    layers[i] = new MetalLayer(i, plans[i], cell);
			}
			if (i!=topLayer) {
				// connect to upper level
				connectLayers(layers[i], layers[i+1], cell);
			}
		}
		if (layers[topLayer]!=null) exportPerimeter(layers[topLayer], cell, stdCell);
		if (layers[topLayer-1]!=null) exportPerimeter(layers[topLayer-1], cell, stdCell);
		if (wireLowest)  exportWiring(layers[botLayer], cell, stdCell);

		double cellWidth = plans[topLayer].cellWidth;
		double cellHeight = plans[topLayer].cellHeight;
		LayoutLib.newNodeInst(Tech.essentialBounds,
							  -cellWidth/2, -cellHeight/2,
							  G.DEF_SIZE, G.DEF_SIZE, 180, cell);
		LayoutLib.newNodeInst(Tech.essentialBounds,
							  cellWidth/2, cellHeight/2,
							  G.DEF_SIZE, G.DEF_SIZE, 0, cell);
		return cell;
	}
	protected FillCell(StdCellParams stdCell) {
		gndNm = stdCell.getGndExportName();
		vddNm = stdCell.getVddExportName();
	}
}

class FillRouter {
	private HashMap<String,List<PortInst>> portMap = new HashMap<String,List<PortInst>>();
	private String makeKey(PortInst pi) {
        EPoint center = pi.getCenter();
		String x = ""+center.getX(); // LayoutLib.roundCenterX(pi);
		String y = ""+center.getY(); // LayoutLib.roundCenterY(pi);
		return x+"x"+y;
	}
//	private boolean bothConnect(ArcProto a, PortProto pp1, PortProto pp2) {
//		return pp1.connectsTo(a) && pp2.connectsTo(a);
//	}
	private ArcProto findCommonArc(PortInst p1, PortInst p2) {
		ArcProto[] metals = {Tech.m6, Tech.m5, Tech.m4, Tech.m3, Tech.m2, Tech.m1};
		PortProto pp1 = p1.getPortProto();
		PortProto pp2 = p2.getPortProto();
		for (int i=0; i<metals.length; i++) {
			if (pp1.connectsTo(metals[i]) && pp2.connectsTo(metals[i])) {
				return metals[i];
			}
		}
		return null;
	}
	private void connectPorts(List<PortInst> ports) {
		for (Iterator<PortInst> it=ports.iterator(); it.hasNext(); ) {
			PortInst first = it.next();
			double width = LayoutLib.widestWireWidth(first);
			it.remove();
			for (PortInst pi : ports) {
				ArcProto a = findCommonArc(first, pi);
				if (a!=null)  LayoutLib.newArcInst(a, width, first, pi);
			}
		}
	}
	private FillRouter(ArrayList<PortInst> ports) {
		for (PortInst pi : ports) {
			String key = makeKey(pi);
			List<PortInst> l = portMap.get(key);
			if (l==null) {
				l = new LinkedList<PortInst>();
				portMap.put(key, l);
			}
			l.add(pi);
		}
		for (String str : portMap.keySet()) {
			connectPorts(portMap.get(str));
		}
	}
	public static void connectCoincident(ArrayList<PortInst> ports) {
		new FillRouter(ports);
	}
}


/**
 * Object for building fill libraries
 */
public class FillGeneratorTool extends Tool {

    /** the fill generator tool. */								private static FillGeneratorTool tool = getTool();
     // Depending on generator plugin available
    public static FillGeneratorTool getTool()
    {
        if (tool != null) return tool;

        FillGeneratorTool tool;
        try
        {
            Class extraClass = Class.forName("com.sun.electric.plugins.generator.FillCellTool");
            Object obj = extraClass.getDeclaredConstructor().newInstance();
            tool = (FillGeneratorTool)obj;
        } catch (Exception e)
        {
            if (Job.getDebug())
                System.out.println("GNU Release can't find Fill Cell Generator plugin");
            tool = new FillGeneratorTool();
        }
        return tool;
    }

    public FillGeneratorTool() {
        super("Fill Generator");
        Tech.setTechnology(Tech.Type.MOCMOS);
    }

    public void setConfig(FillGenConfig config)
    {
        this.config = config;
        this.libInitialized = false;
        /** Set technology */
        Tech.setTechnology(config.techNm);
    }

    public enum Units {NONE, LAMBDA, TRACKS}
    public enum PowerType {POWER, VDD}
    public enum ExportConfig {PERIMETER, PERIMETER_AND_INTERNAL}

    private static final double m1via = 4;
    private static final double m1sp = 3;
    private static final double m1SP = 6;
    private static final double m6via = 5;
    private static final double m6sp = 4;
    private static final double m6SP = 8;

    public FillGenConfig config;
    protected Library lib;
    private boolean libInitialized;
    public List<Cell> masters;
    protected StdCellParams stdCell, stdCellP;
    protected CapCell capCell, capCellP;
    protected Floorplan[] plans;

    protected boolean getOrientation() {return plans[plans.length-1].horizontal;}

    /** Reserve space in the middle of the Vdd and ground straps for signals.
     * @param layer the layer number. This may be 2, 3, 4, 5, or 6. The layer
     * number 1 is reserved to mean "capacitor between Vdd and ground".
     * @param reserved space to reserve in the middle of the central
     * strap in case of Vdd. The value 0 makes the Vdd strap one large strap instead of two smaller
     * adjacent straps.
     * Space to reserve between the ground strap of this
     * cell and the ground strap of the adjacent fill cell. The value 0 means
     * that these two ground straps should abut to form a single large strap
     * instead of two smaller adjacent straps.
     * */
    private double reservedToLambda(int layer, double reserved, Units units) {
        if (units==LAMBDA) return reserved;
        double nbTracks = reserved;
        if (nbTracks==0) return 0;
        if (layer!=6) return 2*m1SP - m1sp + nbTracks*(m1via+m1sp);
        return 2*m6SP - m6sp + nbTracks*(m6via+m6sp);
    }

    private Floorplan[] makeFloorplans(boolean metalFlex, boolean hierFlex) {
        LayoutLib.error(config.width==Double.NaN,
                        "width hasn't been specified. use setWidth()");
        LayoutLib.error(config.height==Double.NaN,
                        "height hasn't been specified. use setHeight()");
        double w = config.width;
        double h = config.height;
        double[] vddRes = {0,0,0,0,0,0,0};
        double[] gndRes = {0,0,0,0,0,0,0};
        double[] vddW = {0,0,0,0,0,0,0};
        double[] gndW = {0,0,0,0,0,0,0};

        // set given values
        for (FillGenConfig.ReserveConfig c : config.reserves)
        {
            vddRes[c.layer] = reservedToLambda(c.layer, c.vddReserved, c.vddUnits);
            gndRes[c.layer] = reservedToLambda(c.layer, c.gndReserved, c.gndUnits);
            if (c.vddWUnits != Units.NONE)
               vddW[c.layer] = reservedToLambda(c.layer, c.vddWidth, c.vddWUnits);
            if (c.gndWUnits != Units.NONE)
               gndW[c.layer] = reservedToLambda(c.layer, c.gndWidth, c.gndWUnits);
        }
        boolean evenHor = config.evenLayersHorizontal;
        boolean alignedMetals = true;
//        double[] spacing1 = drcRules;
        double[] spacing = {config.drcSpacingRule,config.drcSpacingRule,
                config.drcSpacingRule,config.drcSpacingRule,
                config.drcSpacingRule,config.drcSpacingRule,config.drcSpacingRule};

        if (alignedMetals)
        {
            double maxVddRes = 0, maxGndRes = 0, maxSpacing = 0, maxVddW = 0, maxGndW = 0;
            for (int i = 0; i < vddRes.length; i++)
            {
                boolean vddOK = false, gndOK = false;
                if (vddRes[i] > 0)
                {
                    vddOK = true;
                    if (maxVddRes < vddRes[i]) maxVddRes = vddRes[i];
                }
                if (gndRes[i] > 0)
                {
                    gndOK = true;
                    if (maxGndRes < gndRes[i]) maxGndRes = gndRes[i];
                }
                if (gndOK || vddOK) // checking max spacing rule
                {
                    if (maxSpacing < config.drcSpacingRule) maxSpacing = config.drcSpacingRule; //drcRules[i];
                }
                if (maxVddW < vddW[i])
                    maxVddW = vddW[i];
                if (maxGndW < gndW[i])
                    maxGndW = gndW[i];
            }
            // correct the values
            for (int i = 0; i < vddRes.length; i++)
            {
                vddRes[i] = maxVddRes;
                gndRes[i] = maxGndRes;
                spacing[i] = maxSpacing;
                vddW[i] = maxVddW;
                gndW[i] = maxGndW;
            }
        }

        if (metalFlex)
        {
            if (!hierFlex)
            {
                return new Floorplan[] {
                null,
                new CapFloorplan(w, h, 			 	                 !evenHor),
                new MetalFloorplanFlex(w, h, vddRes[2], gndRes[2], spacing[2],  vddW[2], gndW[2], evenHor),
                new MetalFloorplanFlex(w, h, vddRes[3], gndRes[3], spacing[3],  vddW[3], gndW[3],!evenHor),
                new MetalFloorplanFlex(w, h, vddRes[4], gndRes[4], spacing[4],  vddW[4], gndW[4], evenHor),
                new MetalFloorplanFlex(w, h, vddRes[5], gndRes[5], spacing[5],  vddW[5], gndW[5],!evenHor),
                new MetalFloorplanFlex(w, h, vddRes[6], gndRes[6], spacing[6],  vddW[6], gndW[6], evenHor)
                };
            }
            w = config.width = config.minTileSizeX;
            h = config.height = config.minTileSizeY;
        }
        return new Floorplan[] {
            null,
            new CapFloorplan(w, h, 			 	                 !evenHor),
            new MetalFloorplan(w, h, vddRes[2], gndRes[2], spacing[2],  evenHor),
            new MetalFloorplan(w, h, vddRes[3], gndRes[3], spacing[3], !evenHor),
            new MetalFloorplan(w, h, vddRes[4], gndRes[4], spacing[4],  evenHor),
            new MetalFloorplan(w, h, vddRes[5], gndRes[5], spacing[5], !evenHor),
            new MetalFloorplan(w, h, vddRes[6], gndRes[6], spacing[6],  evenHor)
        };
    }

    private void printCoverage(Floorplan[] plans) {
        for (int i=2; i<plans.length; i++) {
            System.out.println("metal-"+i+" coverage: "+
                               ((MetalFloorplan)plans[i]).coverage);
        }
    }

    protected void initFillParameters(boolean metalFlex, boolean hierFlex) {
        if (libInitialized) return;

        LayoutLib.error(config.fillLibName==null, "no library specified. Use setFillLibrary()");
        LayoutLib.error((config.width==Double.NaN || config.width<=0), "no width specified. Use setFillCellWidth()");
        LayoutLib.error((config.height==Double.NaN || config.height<=0), "no height specified. Use setFillCellHeight()");

        plans = makeFloorplans(metalFlex, hierFlex);
        if (!metalFlex) printCoverage(plans);

        lib = LayoutLib.openLibForWrite(config.fillLibName);
        stdCell = new StdCellParams(null, Tech.getTechnology());
        stdCellP = new StdCellParams(null, Tech.getTechnology());
        stdCellP.setVddExportName("power");
        stdCellP.setVddExportRole(PortCharacteristic.IN);
        if (!metalFlex) // don't do transistors
        {
            capCell = new CapCell(lib, (CapFloorplan) plans[1], stdCell);
            capCellP = new CapCell(lib, (CapFloorplan) plans[1], stdCellP);
        }
        libInitialized = true;
    }

//    private void changeWarning() {
//        LayoutLib.error(libInitialized,
//                        "fill cells with different widths, heights, orientations, "+
//                        "or space reservations must be placed in a different library.\n"+
//                        "change the library first before changing any of these fill cell "+
//                        "characteristics.");
//    }

    private void makeTiledCells(Cell cell, Floorplan[] plans, Library lib,
                                int[] tiledSizes, StdCellParams stdCell) {
        if (tiledSizes==null) return;
        for (int num : tiledSizes)
        {
            TiledCell.makeTiledCell(num, num, cell, plans, lib, stdCell);
        }
    }

	public static Cell makeFillCell(Library lib, Floorplan[] plans,
                                    int botLayer, int topLayer, CapCell capCell,
                                    boolean wireLowest, StdCellParams stdCell, boolean metalFlex, boolean hierFlex) {
		FillCell fc = new FillCell(stdCell);

		return fc.makeFillCell1(lib, plans, botLayer, topLayer, capCell,
		                        wireLowest, stdCell, metalFlex, hierFlex);
	}

    /**
     * Method to create standard set of tiled cells.
     */
    private Cell standardMakeAndTileCell(Library lib, Floorplan[] plans, int lowLay,
                                         int hiLay, CapCell capCell, boolean wireLowest,
                                         int[] tiledSizes, StdCellParams stdCell,
                                         boolean metalFlex)
    {
        Cell master = makeFillCell(lib, plans, lowLay, hiLay, capCell,
                wireLowest, stdCell, metalFlex, false);
        masters = new ArrayList<Cell>();
        masters.add(master);
        makeTiledCells(master, plans, lib, tiledSizes, stdCell);
        return master;
    }

    public static final Units LAMBDA = Units.LAMBDA;
    public static final Units TRACKS = Units.TRACKS;
    public static final PowerType POWER = PowerType.POWER;
    public static final PowerType VDD = PowerType.VDD;
    public static final ExportConfig PERIMETER = ExportConfig.PERIMETER;
    public static final ExportConfig PERIMETER_AND_INTERNAL = ExportConfig.PERIMETER_AND_INTERNAL;

    /** Reserve space in the middle of the Vdd and ground straps for signals.
     * @param layer the layer number. This may be 2, 3, 4, 5, or 6. The layer
     * number 1 is reserved to mean "capacitor between Vdd and ground".
     * @param vddReserved space to reserve in the middle of the central Vdd
     * strap.
     * The value 0 makes the Vdd strap one large strap instead of two smaller
     * adjacent straps.
     * @param vddUnits LAMBDA or TRACKS
     * @param gndReserved space to reserve between the ground strap of this
     * cell and the ground strap of the adjacent fill cell. The value 0 means
     * that these two ground straps should abut to form a single large strap
     * instead of two smaller adjacent straps.
     * @param gndUnits LAMBDA or TRACKS
     * param tiledSizes an array of sizes. The default value is null.  The
     * value null means don't generate anything. */
//	public void reserveSpaceOnLayer(int layer,
//									double vddReserved, Units vddUnits,
//							   		double gndReserved, Units gndUnits) {
//		LayoutLib.error(layer<2 || layer>6,
//						"Bad layer. Layers must be between 2 and 6 inclusive: "+
//						layer);
//		this.vddReserved[layer] = reservedToLambda(layer, vddReserved, vddUnits);
//		this.gndReserved[layer] = reservedToLambda(layer, gndReserved, gndUnits);
//	}

    /** This version of makeFillCell is deprecated. We should no longer need
     * to create fill cells with export type "POWER". Please use the version
     * of makeFillCell that has no PowerType argument. */
    private Cell standardMakeFillCell(int loLayer, int hiLayer, ExportConfig exportConfig, PowerType powerType,
                                      int[] tiledSizes, boolean metalFlex) {
        initFillParameters(metalFlex, false);

        LayoutLib.error(loLayer<1, "loLayer must be >=1");
        LayoutLib.error(hiLayer>6, "hiLayer must be <=6");
        LayoutLib.error(loLayer>hiLayer, "loLayer must be <= hiLayer");
        boolean wireLowest = exportConfig==PERIMETER_AND_INTERNAL;
        Cell cell = null;
        if (powerType==VDD) {
            cell = standardMakeAndTileCell(lib, plans, loLayer, hiLayer, capCell, wireLowest,
                            tiledSizes, stdCell, metalFlex);
        } else {
            cell = standardMakeAndTileCell(lib, plans, loLayer, hiLayer, capCellP, wireLowest,
                            tiledSizes, stdCellP, metalFlex);
        }
        return cell;
    }

    /** Create a fill cell using the current library, fill cell width, fill cell
     * height, layer orientation, and reserved spaces for each layer. Then
     * generate larger fill cells by tiling that fill cell according to the
     * current tiled cell sizes.
     * @param loLayer the lower layer. This may be 1 through 6. Layer 1 means
     * build a capacitor using MOS transistors between Vdd and ground.
     * @param hiLayer the upper layer. This may be 2 through 6. Note that hiLayer
     * must be >= loLayer.
     * @param exportConfig may be PERIMETER in which case exports are
     * placed along the perimeter of the cell for the top two layers. Otherwise
     * exportConfig must be PERIMETER_AND_INTERNAL in which case exports are
     * placed inside the perimeter of the cell for the bottom layer.
     * @param tiledSizes Array specifying composite Cells we should build by
     * concatonating fill cells. For example int[] {2, 4, 7} means we should
     * */
    public Cell standardMakeFillCell(int loLayer, int hiLayer, ExportConfig exportConfig,
                                     int[] tiledSizes, boolean metalFlex) {
        return standardMakeFillCell(loLayer, hiLayer, exportConfig, VDD, tiledSizes, metalFlex);
    }

    public void makeGallery() {
        Gallery.makeGallery(lib);
    }

    public void writeLibrary() {
        LayoutLib.writeLibrary(lib);
    }

    public enum FillTypeEnum {INVALID,TEMPLATE,CELL}
}
