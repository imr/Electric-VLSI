/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: FillGenerator.java
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

import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Area;
import java.util.*;
import java.io.Serializable;

import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.geometry.*;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.*;
import com.sun.electric.technology.technologies.MoCMOS;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.extract.LayerCoverageTool;
import com.sun.electric.tool.routing.*;
import com.sun.electric.tool.user.ErrorLogger;

// ---------------------------- Fill Cell Globals -----------------------------
class G {
	public static double DEF_SIZE = LayoutLib.DEF_SIZE;
	public static ArcInst noExtendArc(ArcProto pa, double w, 
									   PortInst p1, PortInst p2) {
		ArcInst ai = LayoutLib.newArcInst(pa, w, p1, p2);
		ai.setHeadExtended(false);
		ai.setTailExtended(false);
		return ai;		
	}
	public static ArcInst newArc(ArcProto pa, double w, 
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
			         double vddReserve, double gndReserve,
			        double space, boolean horiz)
    {
        super(cellWidth, cellHeight, horiz);
        this.vddWidth = 27;
        this.gndWidth = 20;
        this.space = space;
        this.vddReserve = vddReserve;
        this.gndReserve = gndReserve;
        minWidth = vddReserve + gndReserve + 2*space + 2*gndWidth + 2*vddWidth;
//        int divider = 1;

//        if (horizontal)
//        {
//            divider = (int)Math.floor(cellHeight/minWidth);
//            if (divider > 1) cellHeight /= divider;
//        }
//        else
//        {
//            divider = (int)Math.floor(cellWidth/minWidth);
//            if (divider > 1) cellWidth /= divider;
//        }
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
//		boolean mergedGnd = gndReserve==0;
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
//		double vddEdge = vddCenter + vddWidth/2;
		
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

/** Give access to the metal straps inside a MetalLayer or CapLayer */
interface VddGndStraps {
    static final ArcProto[] METALS =
		{null, Tech.m1, Tech.m2, Tech.m3, Tech.m4, Tech.m5, Tech.m6};
	static final PrimitiveNode[] PINS =
		{null, Tech.m1pin, Tech.m2pin, Tech.m3pin, Tech.m4pin, Tech.m5pin,
		 Tech.m6pin};
    // Defined here so sequence with PINS is kept aligned
    static final PrimitiveNode[] fillContacts = {null, null, Tech.m2m3, Tech.m3m4, Tech.m4m5};
	/** are metal straps horizontal? */		boolean isHorizontal();

	/** how many Vdd straps? */				int numVdd();
	/** get nth Vdd strap */				PortInst getVdd(int n, int pos);
	/** if horizontal get Y else get X */	double getVddCenter(int n);
	/** how wide is nth Vdd metal strap */	double getVddWidth(int n);

	/** how many Gnd straps? */ 			int numGnd();
	/** get nth Gnd strap */				PortInst getGnd(int n, int pos);
	/** if horizontal get Y else X */ 		double getGndCenter(int n);
	/** how wide is nth Gnd strap? */ 		double getGndWidth(int n);
	
	PrimitiveNode getPinType();
	ArcProto getMetalType();
	double getCellWidth();
	double getCellHeight();
    boolean addExtraArc(); /** To create an export on new pin connected with a zero length arc */
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
        center = new Double(c);
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
		return (vddBars.get(n).center.doubleValue());
	}
    public PortInst getVdd(int n, int pos)
    {return vddBars.get(n).ports[pos];}
    public double getVddWidth(int n) {return plan.vddWidth;}
    public int numGnd() {return gndBars.size();}
    public double getGndCenter(int n) {
		return (gndBars.get(n).center.doubleValue());
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
		double x = LayoutLib.roundCenterX(p1);
		double y1 = roundToHalfLambda(LayoutLib.roundCenterY(p1));
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
		PortInst pi = getVdd(n, 0);
		return plan.horizontal ? LayoutLib.roundCenterY(pi) : 
			                     LayoutLib.roundCenterX(pi);
	}
	public double getVddWidth(int n) {return capCell.getVddWidth();}
	public int numGnd() {return capCell.numGnd();}
	public PortInst getGnd(int n, int pos) {
		return capCellInst.findPortInst(gndName+"_"+n);
	}
	public double getGndCenter(int n) {
		PortInst pi = getGnd(n, 0);
		return plan.horizontal ? LayoutLib.roundCenterY(pi) :
			                     LayoutLib.roundCenterX(pi);
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
	
	private Cell makeFillCell1(Library lib, Floorplan[] plans, int botLayer,
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
	private FillCell(StdCellParams stdCell) {
		gndNm = stdCell.getGndExportName();
		vddNm = stdCell.getVddExportName();
	}
	public static Cell makeFillCell(Library lib, Floorplan[] plans,
                                    int botLayer, int topLayer, CapCell capCell,
                                    boolean wireLowest, StdCellParams stdCell, boolean metalFlex, boolean hierFlex) {
		FillCell fc = new FillCell(stdCell);

		return fc.makeFillCell1(lib, plans, botLayer, topLayer, capCell, 
		                        wireLowest, stdCell, metalFlex, hierFlex);
	}
}

class FillRouter {
	private HashMap<String,List<PortInst>> portMap = new HashMap<String,List<PortInst>>();
	private String makeKey(PortInst pi) {
		String x = ""+LayoutLib.roundCenterX(pi);
		String y = ""+LayoutLib.roundCenterY(pi);
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
class TiledCell {
	private static final int VERT_EXTERIOR = 0;
	private static final int HORI_EXTERIOR = 1;
	private static final int INTERIOR = 2;

	private int vddNum, gndNum;
	private final String vddNm, gndNm;
    private Cell tileCell;
    private double drcSpacing; // required by qTree method

	private String vddName() {
		int n = vddNum++;
		return n==0 ? vddNm : vddNm+"_"+n;
	}
	private String gndName() {
		int n = gndNum++;
		return n==0 ? gndNm : gndNm+"_"+n;
	}

    /**
     * Constructor used by working with qTree fill.
     * @param stdCell
     */
    public TiledCell(StdCellParams stdCell, double spacing)
    {
	    vddNm = stdCell.getVddExportName();
	    gndNm = stdCell.getGndExportName();
        drcSpacing = spacing;
    }

    /**
     * Method to create the master tiledCell. Note that targetHeight and targetHeight do not necessarily match
     * with cellW and cellH if algorithm is used to create a flexible number of tiled cells
     @param numX
     * @param numY
     * @param cell
     * @param plans
     * @param lib
     * @param stdCell
     */
	private TiledCell(int numX, int numY, Cell cell, Floorplan[] plans,
                      Library lib, StdCellParams stdCell)
    {
	    vddNm = stdCell.getVddExportName();
	    gndNm = stdCell.getGndExportName();

		String tiledName = "t"+cell.getName()+"_"+numX+"x"+numY+"{lay}";
		tileCell = Cell.newInstance(lib, tiledName);

		Rectangle2D bounds = cell.findEssentialBounds();
        ERectangle r = cell.getBounds();
		LayoutLib.error(bounds==null, "missing Essential Bounds");
		double cellW = bounds.getWidth();
		double cellH = bounds.getHeight();

		// put bottom left cell at (0, 0)
		double y = 0;

		NodeInst[][] rows = newRows(numX, numY);
		for (int row=0; row<numY; row++) {
			double x = 0;
			for (int col=0; col<numX; col++) {
				rows[row][col] = LayoutLib.newNodeInst(cell, x, y, G.DEF_SIZE,
													   G.DEF_SIZE, 0, tileCell);
				x += cellW;
			}
			y += cellH;
		}
		ArrayList<PortInst> portInsts = getAllPortInsts(tileCell);
		FillRouter.connectCoincident(portInsts);
		exportUnconnectedPortInsts(rows, plans[plans.length-1].horizontal, tileCell, stdCell);
//		addEssentialBounds(cellW, cellH, numX, numY, tileCell);
        addEssentialBounds1(r.getX(), r.getY(), cellW, cellH, numX, numY, tileCell);
	}

    /**
     *  Method to set up conditions for qTree refinement
     */
    public Cell makeQTreeCell(Cell master, Cell empty, Library autoLib, int tileOnX, int tileOnY, double minTileSizeX,
                              double minTileSizeY, boolean isPlanHorizontal, StdCellParams stdCellParam, Cell topCell, boolean binary,
                              List<Rectangle2D> topBoxList, Area area)
    {
        double fillWidth = tileOnX * minTileSizeX;
        double fillHeight = tileOnY * minTileSizeY;
        // in case of binary division, make sure the division is 2^N
        if (binary)
        {
            int powerX = getFillDimension(tileOnX, true);
            int powerY = getFillDimension(tileOnY, true);
            fillWidth = powerX*minTileSizeX;
            fillHeight = powerY*minTileSizeY;
        }

        // topBox its width/height must be multiply of master.getWidth()/Height()
        Rectangle2D topBox = new Rectangle2D.Double(topCell.getBounds().getCenterX()-fillWidth/2,
                topCell.getBounds().getCenterY()-fillHeight/2, fillWidth, fillHeight);
        Variable pitchVar = topCell.getVar("FILL_PITCH_Y");
        if (pitchVar != null)
        {
            Object pitchValue = pitchVar.getObject();
            double value = (pitchValue instanceof Integer) ?
                    ((Integer)pitchValue).intValue() :
                    ((Double)pitchValue).doubleValue();
            double deltaYFromBottom = 0;
            // getting master Y pitch for GDN
            for (Iterator<Export> expIt = master.getExports(); expIt.hasNext(); )
            {
                Export exp = expIt.next();
                if (exp.getName().equals("gnd")) // lazy way to detect left bottom gnd bar
                {
                    deltaYFromBottom = master.getBounds().getMinY() - exp.getOriginalPort().getNodeInst().getBounds().getCenterY();
                    break;
                }
            }
            double newPitch = value + deltaYFromBottom;
            assert((newPitch-topBox.getMinY()) < minTileSizeY);
            topBox = new Rectangle2D.Double(topBox.getMinX(), newPitch-minTileSizeY, topBox.getWidth(), fillHeight+minTileSizeY);
        }
        topBoxList.clear(); // remove original value
        topBoxList.add(topBox); // I need information for setting

        // refine recursively
        List<Cell> newElems = new ArrayList<Cell>();
        refine(master, empty, autoLib, topBox, isPlanHorizontal, stdCellParam, newElems, topCell, binary, area);
        assert(newElems.size()==1);
        return newElems.iterator().next();
    }

    private static class OrderPortInstsByName implements Comparator<PortInst> {
		private String base(String s) {
			int under = s.indexOf("_");
			if (under==-1) return s;
			return s.substring(0, under);
		}
		private int subscript(String s) {
			int under = s.indexOf("_");
			if (under==-1) return 0;
			String num = s.substring(under+1, s.length());
			return Integer.parseInt(num);
		}
		public int compare(PortInst p1, PortInst p2) {
			String n1 = p1.getPortProto().getName();
			String n2 = p2.getPortProto().getName();
			String base1 = base(n1);
			String base2 = base(n2);			
			if (!base1.equals(base2)) {
				return n1.compareTo(n2);
			} else {
				int sub1 = subscript(n1);
				int sub2 = subscript(n2);
				return sub1-sub2;
			}
		}
	}
	private static ArrayList<PortInst> getAllPortInsts(Cell cell) {
		// get all the ports
		ArrayList<PortInst> ports = new ArrayList<PortInst>();
		for (Iterator<NodeInst> it=cell.getNodes(); it.hasNext();) {
			NodeInst ni = it.next();
			for (Iterator<PortInst> pIt=ni.getPortInsts(); pIt.hasNext();) {
				PortInst pi = pIt.next();
				ports.add(pi);
			}
		}
		return ports;
	}
	private static int orientation(Rectangle2D bounds, PortInst pi) {
		double portX = LayoutLib.roundCenterX(pi);
		double portY = LayoutLib.roundCenterY(pi);
		double minX = bounds.getMinX();
		double maxX = bounds.getMaxX();
		double minY = bounds.getMinY();
		double maxY = bounds.getMaxY();
		if (DBMath.areEquals(portX,minX) || DBMath.areEquals(portX,maxX)) return VERT_EXTERIOR;
		if (DBMath.areEquals(portY,minY) || DBMath.areEquals(portY,maxY)) return HORI_EXTERIOR;
		return INTERIOR;
	}
	/** return a list of all PortInsts of ni that aren't connected to 
	 * something. */
	private static ArrayList<PortInst> getUnconnectedPortInsts(int orientation, NodeInst ni) {
		Rectangle2D bounds = ni.findEssentialBounds();
        if (bounds == null)
            bounds = ni.getBounds();
		ArrayList<PortInst> ports = new ArrayList<PortInst>();
		for (Iterator<PortInst> it=ni.getPortInsts(); it.hasNext();) {
			PortInst pi = it.next();
			Iterator conns = pi.getConnections();
			if (!conns.hasNext() && orientation(bounds,pi)==orientation) {
				ports.add(pi);
			}
		}
		return ports;
	}
	private void exportPortInsts(List<PortInst> ports, Cell tiled,
								 StdCellParams stdCell) {
		Collections.sort(ports, new OrderPortInstsByName());
		for (PortInst pi : ports) {
			PortProto pp = (PortProto) pi.getPortProto();
			PortCharacteristic role = pp.getCharacteristic(); 
			if (role==stdCell.getVddExportRole()) {
				//System.out.println(pp.getName());
				Export e = Export.newInstance(tiled, pi, vddName());
				e.setCharacteristic(role);
			} else if (role==stdCell.getGndExportRole()) {
				//System.out.println(pp.getName());
				Export e = Export.newInstance(tiled, pi, gndName());
				e.setCharacteristic(role);
			} else {
				LayoutLib.error(true, "unrecognized Characteristic");
			}
		}
	}
	/** export all PortInsts of all NodeInsts in insts that aren't connected
	 * to something */
	private void exportUnconnectedPortInsts(NodeInst[][] rows, boolean isPlanHorizontal,
                                            Cell tiled, StdCellParams stdCell) {
		// Subtle!  If top layer is horizontal then begin numbering exports on 
		// vertical edges of boundary first. This ensures that fill6_2x2 and 
		// fill56_2x2 have matching port names on the vertical edges.
		// Always number interior exports last so they never interfere with
		// perimeter exports.
		int[] orientations;
        if (isPlanHorizontal) {
			orientations = new int[] {
				VERT_EXTERIOR,
				HORI_EXTERIOR,
				INTERIOR
			};
		} else {
			orientations = new int[] {
				HORI_EXTERIOR,
				VERT_EXTERIOR,
				INTERIOR
			};
		}
		for (int o=0; o<3; o++) {
			int orientation = orientations[o];
			for (int row=0; row<rows.length; row++) {
				for (int col=0; col<rows[row].length; col++) {
					if (orientation!=INTERIOR || row==col) {
						List<PortInst> ports = 
							getUnconnectedPortInsts(orientation, rows[row][col]);
						exportPortInsts(ports, tiled, stdCell);
					} 
				}
			}
		}
	}
	private NodeInst[][] newRows(int numX, int numY) {
		NodeInst[][] rows = new NodeInst[numY][];
		for (int row=0; row<numY; row++) {
			rows[row] = new NodeInst[numX];
		}
		return rows;
	}
	/** Geometric center of bottom left cell is at (0, 0). */
	private void addEssentialBounds(double cellW, double cellH, 
	                                int numX, int numY, Cell tiled) {
		double blX = -cellW/2;
		double blY = -cellH/2;
		double tlX = cellW/2 + (numX-1)*cellW;
		double tlY = cellH/2 + (numY-1)*cellH;
		LayoutLib.newNodeInst(Tech.essentialBounds, blX, blY,
							  G.DEF_SIZE, G.DEF_SIZE, 180, tiled);
		LayoutLib.newNodeInst(Tech.essentialBounds, tlX, tlY,
							  G.DEF_SIZE, G.DEF_SIZE, 0, tiled);
	}
    private void addEssentialBounds1(double x, double y, double cellW, double cellH, int numX, int numY, Cell tiled)
    {
        double x2 = x + numX*cellW;
        double y2 = y + numY*cellH;
        LayoutLib.newNodeInst(Tech.essentialBounds, x, y,
							  G.DEF_SIZE, G.DEF_SIZE, 180, tiled);
		LayoutLib.newNodeInst(Tech.essentialBounds, x2, y2,
							  G.DEF_SIZE, G.DEF_SIZE, 0, tiled);
    }

    enum CutType {NONE,X,Y,XY};

    /**
     * ----------------------------
     * |     2      |      3      |
     * ----------------------------
     * |     0      |      1      |
     * ----------------------------
     * @param master
     * @param empty
     * @param autoLib
     * @param box  bounding box representing region to refine. (0,0) is the top left corner
     * @param topCell
     * @param binary
     * @param area
     * @return true if refined elements form std cell
     */
    private boolean refine(Cell master, Cell empty, Library autoLib, Rectangle2D box, boolean isPlanHorizontal, StdCellParams stdCellParam,
                           List<Cell> newElems, Cell topCell, boolean binary, Area area)
    {
        Rectangle2D r = master.findEssentialBounds();
        double masterW = r.getWidth();
        double masterH = r.getHeight();
        double w = box.getWidth(), wHalf = w/2;
        double h = box.getHeight(), hHalf = h/2;
        double x = box.getX();
        double y = box.getY();
        double refineOnX = w/masterW;
        double refineOnY = h/masterH;
        // refineOnX and refineOnY must be multiple of masterW/masterH
        if (DBMath.hasRemainder(w, masterW) || DBMath.hasRemainder(h, masterH))
            System.out.println("not good refinement");

        int cutX = (int)Math.ceil(refineOnX);
        int cutY = (int)Math.ceil(refineOnY);
        assert(cutX > 0 && cutY > 0);
        int cutXOnLeft = getFillDimension((int)(refineOnX/2), false);
        int cutXOnRight = cutX - cutXOnLeft; // get exact number of cuts
        int cutYOnBottom = getFillDimension((int)(refineOnY/2), false);
        int cutYOnTop = cutY - cutYOnBottom; // get exact number of cuts

        // Each cut will have a copy of the cell to use
        List<Cell> childrenList = new ArrayList<Cell>();
        boolean stdCell = true;
        List<Rectangle2D> boxes = new ArrayList<Rectangle2D>();
        CutType cut = CutType.NONE;
        Rectangle2D bb = null;

        if (cutX > 1 && cutY > 1) // refine on XY
        {
            cut = CutType.XY;
            double[] widths = null, heights = null;
            double[] centerX = null, centerY = null;

            if (binary)
            {
                widths = new double[]{wHalf, wHalf, wHalf, wHalf};
                heights = new double[]{hHalf, hHalf, hHalf, hHalf};
                centerX = new double[]{box.getCenterX(), box.getCenterX(), box.getCenterX(), box.getCenterX()};
                centerY = new double[]{box.getCenterY(), box.getCenterY(), box.getCenterY(), box.getCenterY()};
            }
            else
            {
                double tmpXLeft = cutXOnLeft*masterW, tmpYOnBottom = cutYOnBottom*masterH;
                widths = new double[]{tmpXLeft, cutXOnRight*masterW};
                heights = new double[]{tmpYOnBottom, cutYOnTop*masterH};
                centerX = new double[]{x, x+tmpXLeft};
                centerY = new double[]{y, y+tmpYOnBottom};
            }

            for (int i = 0; i < 4; i++)
            {
                int xPos = i%2;
                int yPos = i/2;
                bb = GenMath.getQTreeBox(x, y, widths[xPos], heights[yPos], centerX[xPos], centerY[yPos], i);
                boxes.add(bb);
                if (!refine(master, empty, autoLib, bb, isPlanHorizontal, stdCellParam, childrenList, topCell, binary, area))
                    stdCell = false;
            }
        }
        else if (cutX > 1) // only on X
        {
            cut = CutType.X;
            double[] widths = null, centerX = null;

            if (binary)
            {
                widths = new double[]{wHalf, wHalf};
                centerX = new double[]{box.getCenterX(), box.getCenterX()};
            }
            else
            {
                double tmpXLeft = cutXOnLeft*masterW;
                widths = new double[]{tmpXLeft, cutXOnRight*masterW};
                centerX = new double[]{x, x+tmpXLeft};
            }

            for (int i = 0; i < 2; i++)
            {
                bb = GenMath.getQTreeBox(x, y, widths[i], h, centerX[i], box.getCenterY(), i);
                boxes.add(bb);
                if (!refine(master, empty, autoLib, bb, isPlanHorizontal, stdCellParam, childrenList, topCell, binary, area))
                    stdCell = false;
            }
        }
        else if (cutY > 1) // only on Y
        {
            cut = CutType.Y;
            double[] heights = null, centerY = null;

            if (binary)
            {
                heights = new double[]{hHalf, hHalf};
                centerY = new double[]{box.getCenterY(), box.getCenterY()};
            }
            else
            {
                double tmpYOnBottom = cutYOnBottom*masterH;
                heights = new double[]{tmpYOnBottom, cutYOnTop*masterH};
                centerY = new double[]{y, y+tmpYOnBottom};
            }
            for (int i = 0; i < 2; i++)
            {
                bb = GenMath.getQTreeBox(x, y, w, heights[i], box.getCenterX(), centerY[i], i*2);
                boxes.add(bb);
                if (!refine(master, empty, autoLib, bb, isPlanHorizontal, stdCellParam, childrenList, topCell, binary, area))
                    stdCell = false;
            }
        } else {
            // nothing refined, qTree leave
            HashSet<NodeInst> nodesToRemove = new HashSet<NodeInst>();
            HashSet<ArcInst> arcsToRemove = new HashSet<ArcInst>();
            // Translation from master cell center to actual location in the qTree
            /*		[   1    0    tx  ]
             *		[   0    1    ty  ]
             *		[   0    0    1   ]
             */
            AffineTransform fillTransUp = new AffineTransform(1.0, 0.0, 0.0, 1.0,
                    box.getCenterX() - master.getBounds().getCenterX(),
                    box.getCenterY() - master.getBounds().getCenterY());
            boolean isExcluded = area.intersects(box);
            stdCell = true;

            if (isExcluded || FillGenerator.detectOverlappingBars(master, fillTransUp, nodesToRemove, arcsToRemove,
                    topCell, new NodeInst[] {}, drcSpacing))
            {
                boolean emptyCell = isExcluded || arcsToRemove.size() == master.getNumArcs();
                Cell dummyCell = null;

                if (emptyCell)
                    dummyCell = empty;
                else
                {
                    // Replace by dummy cell for now
                    String dummyName = "dummy";
                    // Collect names from nodes and arcs to remove if the cell is not empty
                    List<String> namesList = new ArrayList<String>();
                    for (NodeInst ni : nodesToRemove)
                    {
                        // Not deleting the pins otherwise cells can be connected.
                        // List should not contain pins
                        if (ni.getProto().getFunction() != PrimitiveNode.Function.PIN)
                            namesList.add(ni.getName());
                    }
//                    for (ArcInst ai : arcsToRemove)
//                    {
//                        namesList.add(ai.getName());
//                    }
                    Collections.sort(namesList);
                    int code = namesList.toString().hashCode();

                    dummyName +=code+"{lay}";
                    dummyCell = master.getLibrary().findNodeProto(dummyName);

                    if (dummyCell == null)
                    {
                        // Creating empty/dummy Master cell or look for an existing one
                        dummyCell = Cell.copyNodeProto(master, master.getLibrary(), dummyName, true);
//                        LayoutLib.newNodeInst(Tech.essentialBounds, -masterW/2, -masterH/2,
//                                              G.DEF_SIZE, G.DEF_SIZE, 180, dummyCell);
//                        LayoutLib.newNodeInst(Tech.essentialBounds, masterW/2, masterW/2,
//                                              G.DEF_SIZE, G.DEF_SIZE, 0, dummyCell);

                        // Time to delete the elements overlapping with the rest of the top cell
                        for (NodeInst ni : nodesToRemove)
                        {
                            NodeInst d = dummyCell.findNode(ni.getName());
                            d.kill();
                        }

                        for (ArcInst ai : arcsToRemove)
                        {
                            ArcInst d = dummyCell.findArc(ai.getName());
                            // d is null if a NodeInst connected was killed.
                            if (d != null)
                                d.kill();
                        }
                    }
                }
                newElems.add(dummyCell);
                stdCell = emptyCell;
            }
            else
                newElems.add(master);
            return stdCell;
        }

        Cell tiledCell = null;
        String tileName = null;
        boolean stdC = true;
        assert(childrenList.size() > 1);
        Cell template = childrenList.get(0);
        for (int i = 1; i < childrenList.size(); i++)
        {
            if (template != childrenList.get(i))
            {
                stdC = false;
                break;
            }
        }

        // Search by names
        if (!stdCell)
        {
            stdCell = stdC;
        }

        if (stdCell)
        {
            String rootName = childrenList.get(0).getName();
            if (childrenList.get(0) == master)
                rootName = master.getName();
            else if (childrenList.get(0) == empty)
                rootName = empty.getName();
            tileName = rootName+"_"+w+"x"+h+"{lay}";
            tiledCell = master.getLibrary().findNodeProto(tileName);
        }
        else
            tileName = "dummy"+master.getName()+"_"+w+"x"+h+"("+x+","+y+"){lay}";

        if (tiledCell == null || !stdCell)
        {
            tiledCell = Cell.newInstance(master.getLibrary(), tileName);
            LayoutLib.newNodeInst(Tech.essentialBounds, -w/2, -h/2,
                                  G.DEF_SIZE, G.DEF_SIZE, 180, tiledCell);
            LayoutLib.newNodeInst(Tech.essentialBounds, w/2, h/2,
                                  G.DEF_SIZE, G.DEF_SIZE, 0, tiledCell);

            NodeInst[][] rows = null;

            // if it could use previous std fill cells
            switch (cut)
            {
                case X:
                    rows = new NodeInst[1][2];
                    break;
                case Y:
                    rows = new NodeInst[2][1];
                    break;
                case XY:
                    rows = new NodeInst[2][2];
                    break;
            }
            assert(boxes.size() == childrenList.size());

            for (int i = 0; i < boxes.size(); i++)
            {
                Rectangle2D b = boxes.get(i);
                double cenX = b.getCenterX() - box.getCenterX();
                double cenY = b.getCenterY() - box.getCenterY();
                NodeInst node = LayoutLib.newNodeInst(childrenList.get(i), cenX, cenY,
                        b.getWidth(), b.getHeight(), 0, tiledCell);
                switch (cut)
                {
                    case X:
                        rows[0][i] = node;
                        break;
                    case Y:
                        rows[i][0] = node;
                        break;
                    case XY:
                        rows[(int)i/2][i%2] = node;
                }
            }

            ArrayList<PortInst> portInsts = TiledCell.getAllPortInsts(tiledCell);
            FillRouter.connectCoincident(portInsts);
            exportUnconnectedPortInsts(rows, isPlanHorizontal, tiledCell, stdCellParam);
        }

        newElems.add(tiledCell);
        return stdCell;
    }

    /**
     * Method to calculate number of master sizes to cover using binary division or the intersection approach
     * @param size
     * @param binary
     * @return
     */
    private static int getFillDimension(int size, boolean binary)
    {
        if (size <=2) return size;
        if (binary)
        {
            // calculate the closest upper 2^n so the qTree will be perfectely balanced.
            double val = Math.sqrt(size);
            val = Math.ceil(val);
            return (int)Math.pow(2,val);
        }
        // otherwise just the closest even number
        if (size%2 != 0) //odd number
        {
            size -= 1;

        }
        return size;
    }

	public static Cell makeTiledCell(int numX, int numY, Cell cell,
                                     Floorplan[] plans, Library lib,
                                     StdCellParams stdCell) {
		TiledCell tile = new TiledCell(numX, numY, cell, plans, lib, stdCell);
        return tile.tileCell;
	}
}

/**
 * Object for building fill libraries
 */
public class FillGenerator implements Serializable {
    
    public static boolean detectOverlappingBars(Cell cell, AffineTransform fillTransUp,
                                                HashSet<NodeInst> nodesToRemove, HashSet<ArcInst> arcsToRemove,
                                                Cell topCell, NodeInst[] ignore, double drcSpacing)
    {
        List<Layer.Function> tmp = new ArrayList<Layer.Function>();

        // Check if any metalXY must be removed
        for (Iterator<NodeInst> itNode = cell.getNodes(); itNode.hasNext(); )
        {
            NodeInst ni = itNode.next();

            if (NodeInst.isSpecialNode(ni)) continue;

            tmp.clear();
            NodeProto np = ni.getProto();
            if (ni.isCellInstance())
            {
                throw new Error("This should not happen");
            }
            PrimitiveNode pn = (PrimitiveNode)np;
            if (pn.getFunction() == PrimitiveNode.Function.PIN) continue; // pins have pseudo layers

            for (Technology.NodeLayer tlayer : pn.getLayers())
            {
                tmp.add(tlayer.getLayer().getFunction());
            }
            Rectangle2D rect = getSearchRectangle(ni.getBounds(), fillTransUp, drcSpacing);
            if (searchCollision(topCell, rect, tmp, null, ignore))
            {
                // Direct on last top fill cell
                nodesToRemove.add(ni);
                for (Iterator<Connection> itC = ni.getConnections(); itC.hasNext();)
                {
                    Connection c = itC.next();
                    arcsToRemove.add(c.getArc());
                }
            }
        }

        // Checking if any arc in FillCell collides with rest of the cells
        for (Iterator<ArcInst> itArc = cell.getArcs(); itArc.hasNext(); )
        {
            ArcInst ai = itArc.next();
            tmp.clear();
            tmp.add(ai.getProto().getLayers()[0].getLayer().getNonPseudoLayer().getFunction());
            // Searching box must reflect DRC constrains
            Rectangle2D rect = getSearchRectangle(ai.getBounds(), fillTransUp, drcSpacing);
            if (searchCollision(topCell, rect, tmp, null, ignore))
            {
                arcsToRemove.add(ai);
                // Remove exports and pins as well
//                nodesToRemove.add(ai.getTail().getPortInst().getNodeInst());
//                nodesToRemove.add(ai.getHead().getPortInst().getNodeInst());
            }
        }

        Set<ArcProto> names = new HashSet<ArcProto>();
        // Check if there are not contacts or ping that don't have at least one or zero arc per metal
        // If they don't have, then they are floating
        for (Iterator<NodeInst> itNode = cell.getNodes(); itNode.hasNext(); )
        {
            NodeInst ni = itNode.next();

            if (Generic.isSpecialGenericNode(ni)) continue; // Can't skip pins

            // For removal
            if (nodesToRemove.contains(ni))
                continue;

            int minNum = (ni.getProto().getFunction() == PrimitiveNode.Function.PIN)?0:1;
            // At least should have connections to both layers
            names.clear();
            boolean found = false;
            for (Iterator<Connection> itC = ni.getConnections(); itC.hasNext();)
            {
                Connection c = itC.next();
                ArcInst ai = c.getArc();
                if (arcsToRemove.contains(ai)) continue; // marked for deletion
                names.add(ai.getProto());
                if (names.size() > minNum) // found at least two different arcs
                {
                    found = true;
                    break;
                }
            }
            if (!found) // element could be deleted
                nodesToRemove.add(ni);
        }

        return nodesToRemove.size() > 0 || arcsToRemove.size() > 0;
    }
    
    /**
     * Method to determine if new contact will overlap with other metals in the configuration
     * @param parent
     * @param nodeBounds
     * @param p
     * @param ignores NodeInst instances to ignore
     * @return
     */
    protected static boolean searchCollision(Cell parent, Rectangle2D nodeBounds, List<Layer.Function> theseLayers,
                                             FillGenJob.PortConfig p, NodeInst[] ignores)
    {
        // Not checking if they belong to the same net!. If yes, ignore the collision
        Rectangle2D subBound = new Rectangle2D.Double();
        Netlist netlist = parent.acquireUserNetlist();

        for(Iterator<Geometric> it = parent.searchIterator(nodeBounds); it.hasNext(); )
        {
            Geometric geom = it.next();

            if (p != null && geom == p.p.getNodeInst())
                continue; // port belongs to this node

            boolean ignoreThis = false;
            for (int i = 0; i < ignores.length; i++)
            {
                if (geom == ignores[i])
                {
                    ignoreThis = true;
                    break;
                }
            }
            if (ignoreThis) continue; // ignore the cell. E.g. fillNi, connectionNi

            if (geom instanceof NodeInst)
            {
                NodeInst ni = (NodeInst)geom;

                if (NodeInst.isSpecialNode(ni)) continue;

                // ignore nodes that are not primitive
                if (ni.isCellInstance())
                {
                    // instance found: look inside it for offending geometry
                    AffineTransform rTransI = ni.rotateIn();
                    AffineTransform tTransI = ni.translateIn();
                    rTransI.preConcatenate(tTransI);
                    subBound.setRect(nodeBounds);
                    DBMath.transformRect(subBound, rTransI);

                    if (searchCollision((Cell)ni.getProto(), subBound, theseLayers, p, ignores))
                        return true;
                } else
                {
                    if (p != null)
                    {
                        boolean found = false;
                        for (Iterator<PortInst> itP = ni.getPortInsts(); itP.hasNext(); )
                        {
                            PortInst port = itP.next();
                            Network net = netlist.getNetwork(port);
                            // They export the same, power or gnd so no worries about overlapping
                            if (p != null && net.findExportWithSameCharacteristic(p.e) != null)
                            {
                                found = true;
                                break;
                            }
                        }
                        if (found)
                            continue; // no match in network type
                    }

                    Poly [] subPolyList = parent.getTechnology().getShapeOfNode(ni, null, null, true, true,
                            theseLayers);
                    // Overlap found
                    if (subPolyList.length > 0)
                        return true;
                }
            }
            else
            {
                ArcInst ai = (ArcInst)geom;
                Network net = netlist.getNetwork(ai, 0);

                // They export the same, power or gnd so no worries about overlapping
                if (p != null && net.findExportWithSameCharacteristic(p.e) != null)
                    continue; // no match in network type

                Poly [] subPolyList = parent.getTechnology().getShapeOfArc(ai, null, null, theseLayers);

                // Something overlaps
                if (subPolyList.length > 0)
                    return true;
            }
        }
        return false;
    }

    private static Rectangle2D getSearchRectangle(Rectangle2D bnd, AffineTransform fillTransUp, double drcSpacing)
    {
        Rectangle2D rect = new Rectangle2D.Double(bnd.getX() - drcSpacing,
                    bnd.getY() - drcSpacing,
                    bnd.getWidth() + 2* drcSpacing,
                    bnd.getHeight() + 2* drcSpacing);
        if (fillTransUp != null)
            DBMath.transformRect(rect, fillTransUp);
        return rect;
    }

    public enum Units {LAMBDA, TRACKS}
	public enum PowerType {POWER, VDD}
	public enum ExportConfig {PERIMETER, PERIMETER_AND_INTERNAL}

	private static final double m1via = 4;
	private static final double m1sp = 3;
	private static final double m1SP = 6;
	private static final double m6via = 5;
	private static final double m6sp = 4;
	private static final double m6SP = 8;

	private double width=Double.NaN, height=Double.NaN;
    private double targetWidth=Double.NaN, targetHeight=Double.NaN, minTileSizeX=Double.NaN, minTileSizeY=Double.NaN;
	private String libName;
	private Library lib;
	private boolean libInitialized;
	private boolean evenLayersHorizontal;
    private boolean binary; // for qTree approach
	private double[] vddReserved = {0, 0, 0, 0, 0, 0, 0};
	private double[] gndReserved = {0, 0, 0, 0, 0, 0, 0}; 
	private StdCellParams stdCell, stdCellP;
	private CapCell capCell, capCellP;
	private Floorplan[] plans;
    protected double drcSpacing; // to store min distance between wires
    private PrimitiveNode[] metalPins = null;

	private double reservedToLambda(int layer, double reserved, Units units) {
		if (units==LAMBDA) return reserved;
		double nbTracks = reserved;
		if (nbTracks==0) return 0;
		if (layer!=6) return 2*m1SP - m1sp + nbTracks*(m1via+m1sp);
		return 2*m6SP - m6sp + nbTracks*(m6via+m6sp);
	}

    private Floorplan[] makeFloorplans(boolean metalFlex, boolean hierFlex) {
		LayoutLib.error(width==Double.NaN,
						"width hasn't been specified. use setWidth()");
		LayoutLib.error(height==Double.NaN,
						"height hasn't been specified. use setHeight()");
		double w = width;
		double h = height;
		double[] vddRes = vddReserved;
		double[] gndRes = gndReserved;
		boolean evenHor = evenLayersHorizontal;
        if (metalFlex)
        {
            if (!hierFlex)
            {
                return new Floorplan[] {
                null,
                new CapFloorplan(w, h, 			 	                 !evenHor),
                new MetalFloorplanFlex(w, h, vddRes[2], gndRes[2], m1SP,  evenHor),
                new MetalFloorplanFlex(w, h, vddRes[3], gndRes[3], m1SP, !evenHor),
                new MetalFloorplanFlex(w, h, vddRes[4], gndRes[4], m1SP,  evenHor),
                new MetalFloorplanFlex(w, h, vddRes[5], gndRes[5], m1SP, !evenHor),
                new MetalFloorplanFlex(w, h, vddRes[6], gndRes[6], m6SP,  evenHor)
                };
            }
            w = width = minTileSizeX;
            h = height = minTileSizeY;
        }
		return new Floorplan[] {
			null,
			new CapFloorplan(w, h, 			 	                 !evenHor),
			new MetalFloorplan(w, h, vddRes[2], gndRes[2], m1SP,  evenHor),
			new MetalFloorplan(w, h, vddRes[3], gndRes[3], m1SP, !evenHor),
			new MetalFloorplan(w, h, vddRes[4], gndRes[4], m1SP,  evenHor),
			new MetalFloorplan(w, h, vddRes[5], gndRes[5], m1SP, !evenHor),
			new MetalFloorplan(w, h, vddRes[6], gndRes[6], m6SP,  evenHor)
		};
	}

	private void printCoverage(Floorplan[] plans) {
		for (int i=2; i<plans.length; i++) {
			System.out.println("metal-"+i+" coverage: "+
							   ((MetalFloorplan)plans[i]).coverage);
		}
	}

    private void initFlatFillParameters(boolean metalFlex) {
		if (libInitialized) return;

		LayoutLib.error(libName==null, "no library specified. Use setFillLibrary()");

		plans = makeFloorplans(metalFlex, false);
		if (!metalFlex) printCoverage(plans);

		lib = LayoutLib.openLibForWrite(libName);
		stdCell = new StdCellParams(null, Tech.getTechnology());
		stdCellP = new StdCellParams(null, Tech.getTechnology());
		stdCellP.setVddExportName("power");
		stdCellP.setVddExportRole(PortCharacteristic.IN);
		capCell = new CapCell(lib, (CapFloorplan) plans[1], stdCell);
		capCellP = new CapCell(lib, (CapFloorplan) plans[1], stdCellP);

		libInitialized = true;
	}

	private void initHierFillParameters(boolean metalFlex) {
		if (libInitialized) return;
		
		LayoutLib.error(libName==null, "no library specified. Use setFillLibrary()");

		plans = makeFloorplans(metalFlex, true);
		if (!metalFlex) printCoverage(plans);
		
		lib = LayoutLib.openLibForWrite(libName);
		stdCell = new StdCellParams(null, Tech.getTechnology());
		stdCellP = new StdCellParams(null, Tech.getTechnology());
		stdCellP.setVddExportName("power");
		stdCellP.setVddExportRole(PortCharacteristic.IN);
		capCell = new CapCell(lib, (CapFloorplan) plans[1], stdCell);
		capCellP = new CapCell(lib, (CapFloorplan) plans[1], stdCellP);
		
		libInitialized = true; 
	}
	private void changeWarning() {
		LayoutLib.error(libInitialized,
						"fill cells with different widths, heights, orientations, "+
						"or space reservations must be placed in a different library.\n"+
						"change the library first before changing any of these fill cell "+
						"characteristics.");
	}

	private void makeTiledCells(Cell cell, Floorplan[] plans, Library lib,
								int[] tiledSizes, StdCellParams stdCell) {
		if (tiledSizes==null) return;
		for (int i=0; i<tiledSizes.length; i++) {
			int num = tiledSizes[i];
			TiledCell.makeTiledCell(num, num, cell, plans, lib, stdCell);
		}
	}

    /**
     * Method to create standard set of tiled cells.
     */
    private Cell standardMakeAndTileCell(Library lib, Floorplan[] plans, int lowLay,
                                 int hiLay, CapCell capCell, boolean wireLowest,
                                 int[] tiledSizes, StdCellParams stdCell,
                                 boolean metalFlex)
    {
		Cell c = FillCell.makeFillCell(lib, plans, lowLay, hiLay, capCell,
									   wireLowest, stdCell, metalFlex, false);
        makeTiledCells(c, plans, lib, tiledSizes, stdCell);
        return c;
    }

	private Cell treeMakeAndTileCell(Cell master, boolean isPlanHorizontal, Cell topCell,
                                     List<Rectangle2D> topBoxList, Area area)
    {
        // Create an empty cell for cases where all nodes/arcs are moved due to collision
        Cell empty = Cell.newInstance(lib, "emtpy"+master.getName()+"{lay}");
        double cellWidth = master.getBounds().getWidth();
        double cellHeight = master.getBounds().getHeight();
		LayoutLib.newNodeInst(Tech.essentialBounds,
							  -cellWidth/2, -cellHeight/2,
							  G.DEF_SIZE, G.DEF_SIZE, 180, empty);
		LayoutLib.newNodeInst(Tech.essentialBounds,
							  cellWidth/2, cellHeight/2,
							  G.DEF_SIZE, G.DEF_SIZE, 0, empty);

        int tileOnX = (int)Math.ceil(targetWidth/minTileSizeX);
        int tileOnY = (int)Math.ceil(targetHeight/minTileSizeY);

        TiledCell t = new TiledCell(stdCell, 6);
        master = t.makeQTreeCell(master, empty, lib, tileOnX, tileOnY, minTileSizeX, minTileSizeY, isPlanHorizontal, stdCell,
                topCell, binary, topBoxList, area);
        return master;
	}

	public static final Units LAMBDA = Units.LAMBDA;
	public static final Units TRACKS = Units.TRACKS;
	public static final PowerType POWER = PowerType.POWER;
	public static final PowerType VDD = PowerType.VDD;
	public static final ExportConfig PERIMETER = ExportConfig.PERIMETER;
	public static final ExportConfig PERIMETER_AND_INTERNAL = ExportConfig.PERIMETER_AND_INTERNAL;
	
	// Deprecated: Keep this for backwards compatibility
	public FillGenerator() {
		Tech.setTechnology(Tech.Type.MOCMOS);
	}
	
	public FillGenerator(Technology tech) {
        Tech.Type techNm = Tech.Type.INVALID;

        if (tech == MoCMOS.tech)
        {
            techNm = (tech.getSelectedFoundry().getType() == Foundry.Type.TSMC) ?
                    Tech.Type.TSMC180 :
                    Tech.Type.MOCMOS;
        }
        else
            techNm = Tech.Type.TSMC90;

//		LayoutLib.error((techNm != Tech.Type.MOCMOS && techNm != Tech.Type.TSMC180),
//						"FillGenerator only recognizes the technologies: "+
//						Tech.Type.MOCMOS+" and "+Tech.Type.TSMC180+".\n"+
//						"For 90nm use FillGenerator90");
		Tech.setTechnology(techNm);
	}

	/** Specify the library into which fill cells should be placed */
	public void setFillLibrary(String libName) {		
		this.libName = libName;
		libInitialized = false;
	}
	/** Get the Library. This is useful for generating a Gallery */
	public Library getFillLibrary() {return lib;}
	/** Set the width of a single fill cell. 
	 * @param w width in lambda */	
	public void setFillCellWidth(double w) {
		changeWarning();
		width=w;
	}
	/** Set the height of a single fill cell. 
	 * @param h height in lambda */
	public void setFillCellHeight(double h) {
		changeWarning();
		height=h;
	}
    /**
     * Set the min distance between wires to avoid DRC violation
     * @param spacing
     */
    public void setDRCSpacing(double spacing) {
        this.drcSpacing = spacing;
    }
    /**
     * Set the min distance between wires to avoid DRC violation
     * @param b
     */
    public void setBinaryMode(boolean b) {
        this.binary = b;
    }

    /** Set target values: the minimum size of title based on user input and DRC rules
     * @param targetW
     * @param targetH
     * @param sx width in lambda
     * @param sy height in lambda
     */
    public void setTargetValues(double targetW, double targetH, double sx, double sy) {
		changeWarning();
        targetWidth = targetW;
        targetHeight = targetH;
        minTileSizeX = sx;
        minTileSizeY = sy;
    }
	/** Make even layers horizontal or vertical. Odd layers are orthogonal to
	 * even layers.
	 * @param b true if even layers should be horizontal.
	 */
	public void makeEvenLayersHorizontal(boolean b) {
		changeWarning();
		evenLayersHorizontal = b;
	}
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
	public void reserveSpaceOnLayer(int layer, 
									double vddReserved, Units vddUnits, 
							   		double gndReserved, Units gndUnits) {
		LayoutLib.error(layer<2 || layer>6, 
						"Bad layer. Layers must be between 2 and 6 inclusive: "+
						layer);
		this.vddReserved[layer] = reservedToLambda(layer, vddReserved, vddUnits);
		this.gndReserved[layer] = reservedToLambda(layer, gndReserved, gndUnits);
	}

	/** This version of makeFillCell is deprecated. We should no longer need
	 * to create fill cells with export type "POWER". Please use the version
	 * of makeFillCell that has no PowerType argument. */
	private Cell standardMakeFillCell(int loLayer, int hiLayer, ExportConfig exportConfig, PowerType powerType,
                                      int[] tiledSizes, boolean metalFlex) {
		initFlatFillParameters(metalFlex);
		
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

    /** Similar to standardMakeFillCell but it generates hierarchical fills with a qTree
     * @return
     */
    private Cell treeMakeFillCell(int loLayer, int hiLayer, ExportConfig exportConfig, Cell topCell,
                                  Cell givenMaster, List<Rectangle2D> topBoxList, Area area)
    {
		boolean metalFlex = true;
        initHierFillParameters(metalFlex);

		LayoutLib.error(loLayer<1, "loLayer must be >=1");
		LayoutLib.error(hiLayer>6, "hiLayer must be <=6");
		LayoutLib.error(loLayer>hiLayer, "loLayer must be <= hiLayer");
		boolean wireLowest = exportConfig==PERIMETER_AND_INTERNAL;

        Cell master = givenMaster;

        if (master == null)
            master = FillCell.makeFillCell(lib, plans, loLayer, hiLayer, capCell, wireLowest, stdCell, metalFlex, true);
        else
        {
            // must adjust minSize
            Rectangle2D r = master.findEssentialBounds(); // must use essential elements to match the edges
            minTileSizeX = r.getWidth();
            minTileSizeY = r.getHeight();
        }
        Cell cell = treeMakeAndTileCell(master, plans[plans.length-1].horizontal, topCell, topBoxList, area);

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

    public static class FillGenJob extends Job
    {
        private FillGenerator fillGen = null;
        private ExportConfig  perimeter;
        private int firstMetal, lastMetal;
        private int[] cellsList;
        private Cell topCell;
        private ErrorLogger log;
        private boolean hierarchy;
        private boolean doItNow;
        private double minOverlap;

		public FillGenJob(Cell cell, FillGenerator gen, ExportConfig perim, int first, int last, int[] cells,
                          boolean hierarchy, boolean doItNow, double minO)
		{
			super("Fill generator job", null, Type.CHANGE, null, null, Priority.USER);
            this.perimeter = perim;
            this.fillGen = gen;
            this.firstMetal = first;
            this.lastMetal = last;
            this.cellsList = cells;
            this.topCell = cell; // Only if 1 cell is generated.
            this.hierarchy = hierarchy;
            this.doItNow = doItNow;
            this.minOverlap = minO;

            if (doItNow) // call from regressions
            {
                try
                {
                    if (doIt())
                        terminateOK();

                } catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
            else
			    startJob(); // queue the job
		}

        public ErrorLogger getLogger() { return log; }

        public Library getAutoFilLibrary() { return fillGen.lib; }

        /**
         * Method to obtain the PrimitiveNode layer holding this export. It travels along hierarchy
         * until reaches the PrimitiveNode leaf containing the export. Only metal layers are selected.
         * @param ex
         * @return Non pseudo layer for the given export
         */
        public static Layer getMetalLayerFromExport(PortProto ex)
        {
            PortProto po = ex;

            if (ex instanceof Export)
            {
                PortInst pi = ((Export)ex).getOriginalPort();
                po = pi.getPortProto();
            }
            if (po instanceof Export)
                return getMetalLayerFromExport((Export)po);
            if (po instanceof PrimitivePort)
            {
                PrimitivePort pp = (PrimitivePort)po;
                PrimitiveNode node = pp.getParent();
                // Search for at least m2
                for (Iterator<Layer> it = node.layerIterator(); it.hasNext();)
                {
                    Layer layer = it.next();
                    Layer.Function func = layer.getFunction();
                    // Exclude metal1
                    if (func.isMetal() && func != Layer.Function.METAL1)
                        return layer.getNonPseudoLayer();
                }
            }
            return null;
        }

        private List<PortConfig> searchPortList()
        {
           // Searching common power/gnd connections and skip the ones are in the same network
            // Don't change List by Set otherwise the sequence given by Set is not deterministic and hard to debug
            List<PortInst> portList = new ArrayList<PortInst>();
            Netlist topCellNetlist = topCell.acquireUserNetlist();
            List<Export> exportList = new ArrayList<Export>();

            for (Iterator<NodeInst> it = topCell.getNodes(); it.hasNext(); )
            {
                NodeInst ni = it.next();

                if (!ni.isCellInstance())
                {
//                    for (Iterator<PortInst> itP = ni.getPortInsts(); itP.hasNext(); )
//                    {
//                        PortInst p = itP.next();
//
//                        if (!p.getPortProto().isGround() && !p.getPortProto().isPower())
//                            continue;
//                        // Simple case
//                        portList.add(p);
//                    }
                    for (Iterator<Export> itE = ni.getExports(); itE.hasNext(); )
                    {
                        Export e = itE.next();
                        if (!e.isGround() && !e.isPower())
                            continue;
                        portList.add(e.getOriginalPort());
                        exportList.add(e);
                    }
                }
                else
                {
                    Cell cell = (Cell)ni.getProto();
                    Netlist netlist = cell.acquireUserNetlist();
                    List<PortInst> list = new ArrayList<PortInst>();
                    List<Network> nets = new ArrayList<Network>();
                    List<Export> eList = new ArrayList<Export>();

                    for (Iterator<PortInst> itP = ni.getPortInsts(); itP.hasNext(); )
                    {
                        PortInst p = itP.next();

                        if (!p.getPortProto().isGround() && !p.getPortProto().isPower())
                            continue;
                        // If subcell has two exports on the same network, it assumes they are connected inside
                        // and therefore only one of them is checked
                        assert(p.getPortProto() instanceof Export);
                        Export ex = (Export)p.getPortProto();
                        Network net = netlist.getNetwork(ex.getOriginalPort());
                        Network topNet = topCellNetlist.getNetwork(p);
                        Cell fillCell = null;

                        // search for possible existing fill already defined
                        for (Iterator<Nodable> itN = topNet.getNetlist().getNodables(); itN.hasNext(); )
                        {
                            Nodable no = itN.next();
                            if (ni == no) continue; // skip itself
                            if (!no.isCellInstance()) continue; // skip any flat PrimitiveNode?
                            Cell c = (Cell)no.getProto();
                            if (c == p.getNodeInst().getProto()) // skip port parent
                                continue;
                            if (c.getName().indexOf("fill") == -1) continue; // not a fill cell
                            fillCell = c;
                            break;
                        }
                        // if fillCell is not null -> cover by a fill cell
                        if (fillCell == null && !nets.contains(net))
                        {
                            list.add(p);
                            nets.add(net);
                            eList.add(ex);
                        }
                        else
                            System.out.println("Skipping export " + p + " in " + ni);
                    }
                    portList.addAll(list);
                    exportList.addAll(eList);
                }
            }

            // searching for exclusion regions. If port is inside these regions, then it will be removed.
            // Search them in a chunk of ports. It should be faster
//            ObjectQTree tree = new ObjectQTree(topCell.getBounds());
//            List<Rectangle2D> searchBoxes = new ArrayList<Rectangle2D>(); // list of AFG boxes to use.
//
//            for (Iterator<NodeInst> it = topCell.getNodes(); it.hasNext(); )
//            {
//                NodeInst ni = it.next();
//                NodeProto np = ni.getProto();
//                if (np == Generic.tech.afgNode)
//                    searchBoxes.add(ni.getBounds());
//            }
//            if (searchBoxes.size() > 0)
//            {
//                for (PortInst p : portList)
//                {
//                    tree.add(p, p.getBounds());
//                }
//                for (Rectangle2D rect : searchBoxes)
//                {
//                    Set set = tree.find(rect);
//                    portList.removeAll(set);
//                }
//            }
            List<PortConfig> plList = new ArrayList<PortConfig>();

            assert(portList.size() == exportList.size());

            for (int i = 0; i < exportList.size(); i++)
            {
                PortInst p = portList.get(i);

                Layer l = FillGenJob.getMetalLayerFromExport(p.getPortProto());
                if (l != null)
                    plList.add(new PortConfig(p, exportList.get(i), l));
                else
                    System.out.println("F ");
            }
//            for (PortInst p : portList)
//            {
//                plList.add(new PortConfig(p, getMetalLayerFromExport(p.getPortProto())));
//            }
            return plList;
        }

        private Cell searchPossibleMaster()
        {
            for (Iterator<Library> it = Library.getLibraries(); it.hasNext();)
            {
                Library lib = it.next();
                for (Iterator<Cell> itC = lib.getCells(); itC.hasNext();)
                {
                    Cell c = itC.next();
                    if (c.getVar("FILL_MASTER") != null)
                        return c;
                }
            }

            return null;
        }

        /**
         * Auxiliar class to hold port and its corresponding layer
         * so no extra calculation has to be done later.
         */
        private static class PortConfig
        {
            PortInst p;
            Export e;
            Layer l;
            Poly pPoly; // to speed up the progress because PortInst.getPoly() calls getShape()

            PortConfig(PortInst p, Export e, Layer l)
            {
                this.e = e;
                this.p = p;
                this.pPoly = p.getPoly();
                this.l = l;
            }
        }

		public boolean doIt() throws JobException
		{
            // logger must be created in server otherwise it won't return the elements.
            log = ErrorLogger.newInstance("Fill");
            if (!doItNow)
                fieldVariableChanged("log");

            // Searching for possible master
            Cell master = searchPossibleMaster();

            // Creating fills only for layers found in exports
            firstMetal = Integer.MAX_VALUE;
//            lastMetal = Integer.MIN_VALUE;
            List<PortConfig> portList = searchPortList();

            for (PortConfig p : portList)
            {
                int index = p.l.getIndex() + 1;
                if (index < firstMetal) firstMetal = index;
//                if (lastMetal < index) lastMetal = index;
            }
            if (firstMetal <= 2) firstMetal = 3;
            if (lastMetal < firstMetal) lastMetal = firstMetal;
            
            // otherwise pins at edges increase cell sizes and FillRouter.connectCoincident(portInsts)
            // does work
//            G.DEF_SIZE = 0;
            List<Rectangle2D> topBoxList = new ArrayList<Rectangle2D>();
//            Rectangle2D bndd = topCell.getBounds();
            topBoxList.add(topCell.getBounds()); // topBox might change if predefined pitch is included in master cell

            Area area = new Area();
            for (Iterator<NodeInst> it = topCell.getNodes(); it.hasNext(); )
            {
                NodeInst ni = it.next();
                NodeProto np = ni.getProto();
                if (np == Generic.tech.afgNode)
                    area.add(new Area(ni.getBounds()));
            }

            Cell fillCell = (hierarchy) ?
                    fillGen.treeMakeFillCell(firstMetal, lastMetal, perimeter, topCell, master, topBoxList, area) :
                    fillGen.standardMakeFillCell(firstMetal, lastMetal, perimeter, cellsList, true);
//            fillGen.makeGallery();

            if (topCell == null || portList == null || portList.size() == 0) return true;

            Cell connectionCell = Cell.newInstance(topCell.getLibrary(), topCell.getName()+"fill{lay}");

            Rectangle2D fillBnd = fillCell.getBounds();
            double essentialX = fillBnd.getWidth()/2;
            double essentialY = fillBnd.getHeight()/2;
            LayoutLib.newNodeInst(Tech.essentialBounds,
                      -essentialX, -essentialY,
                      G.DEF_SIZE, G.DEF_SIZE, 180, connectionCell);
		    LayoutLib.newNodeInst(Tech.essentialBounds,
                      essentialX, essentialY,
                      G.DEF_SIZE, G.DEF_SIZE, 0, connectionCell);

            // Adding the connection cell into topCell
            assert(topBoxList.size() == 1);
            Rectangle2D bnd = topBoxList.get(0);
            NodeInst conNi = LayoutLib.newNodeInst(connectionCell, bnd.getCenterX(), bnd.getCenterY(),
                    fillBnd.getWidth(), fillBnd.getHeight(), 0, topCell);

            // Adding the fill cell into connectionCell
            Rectangle2D conBnd = connectionCell.getBounds();
            NodeInst fillNi = LayoutLib.newNodeInst(fillCell, conBnd.getCenterX() - fillBnd.getWidth()/2 - fillBnd.getX(),
                    conBnd.getCenterY() - fillBnd.getHeight()/2 - fillBnd.getY(),
                    fillBnd.getWidth(), fillBnd.getHeight(), 0, connectionCell);

            AffineTransform conTransOut = conNi.transformOut();
            AffineTransform fillTransOutToCon = fillNi.transformOut(); // Don't want to calculate transformation to top
            AffineTransform fillTransIn = fillNi.transformIn(conNi.transformIn());

            InteractiveRouter router  = new SimpleWirer();
//            List<PortInst> portNotReadList = new ArrayList<PortInst>();
//            List<Rectangle2D> bndNotReadList = new ArrayList<Rectangle2D>();
            FillGenJobContainer container = new FillGenJobContainer(router, fillCell, fillNi, connectionCell, conNi);

            // Checking if any arc in FillCell collides with rest of the cells
            if (!hierarchy)
            {
                AffineTransform fillTransOut = fillNi.transformOut(conTransOut);
                removeOverlappingBars(container, fillTransOut);
            }

            // Export all fillCell exports in connectCell before extra exports are added into fillCell
            for (Iterator<Export> it = container.fillCell.getExports(); it.hasNext();)
            {
                Export export = it.next();
                PortInst p = container.fillNi.findPortInstFromProto(export);
                Export e = Export.newInstance(container.connectionCell, p, p.getPortProto().getName());
		        e.setCharacteristic(p.getPortProto().getCharacteristic());
            }

            int count = 0;

            // First attempt if ports are below a power/ground bars
            for (PortConfig p : portList)
            {
                count++;
                Rectangle2D rect = null;
                // Transformation of the cell instance containing this port
                AffineTransform trans = null; // null if the port is on the top cell

                if (p.p.getPortProto() instanceof Export)
                {
                    Export ex = (Export)p.p.getPortProto();
                    assert(ex == p.e);
                    Cell exportCell = (Cell)p.p.getNodeInst().getProto();
                    // Supposed to work only with metal layers
                    rect = LayerCoverageTool.getGeometryOnNetwork(exportCell, ex.getOriginalPort(),
                            p.l.getNonPseudoLayer());
                    trans = p.p.getNodeInst().transformOut();
                }
                else // port on pins
                    rect = (Rectangle2D)p.p.getNodeInst().getBounds().clone(); // just to be cloned due to changes inside function

                // Looking to detect any possible contact based on overlap between this geometry and fill
                NodeInst added = addAllPossibleContacts(container, p, rect, trans, fillTransIn, fillTransOutToCon, conTransOut);
                List<PolyBase> polyList = new ArrayList<PolyBase>();
                List<Geometric> gList = new ArrayList<Geometric>();
                polyList.add(p.pPoly);
                gList.add(p.p.getNodeInst());
                if (added != null)
                {
                    log.logWarning(p.p.describe(false) + " connected", gList, null, null, null, polyList, topCell, 0);
                    continue;
                }
                else
                {
                    log.logError(p.p.describe(false) + " not connected", gList, null, null, null, polyList, topCell, 0);
                }

//                // Transformation of the cell instance containing this port
//                nodeBounds.setRect(ni.getBounds());
//                DBMath.transformRect(nodeBounds, trans);
//                DBMath.transformRect(nodeBounds, fillTransIn);
//                // Try to find closest arc. If not possible, then add to to-do list
//                Geometric geom = routeToClosestArc(container, p, nodeBounds, rect.getHeight(), fillTransOut);
//                if (geom == null)
//                {
//                    System.out.println("Check this case");
//                    portNotReadList.add(p);
//                    bndNotReadList.add(nodeBounds);
//                }
//                else
//                {
//                    ErrorLogger.MessageLog l = log.logWarning(p.describe(false) + " connected", topCell, 0);
//                    l.addPoly(p.getPoly(), true, topCell);
//                    if (p.getPortProto() instanceof Export)
//                        l.addExport((Export)p.getPortProto(), true, topCell, null);
//                    l.addGeom(geom, true, fillCell, null);
//                    globalWidth = geom.getBounds().getWidth(); // assuming all contacts have the same width;
//                }
            }

            // Checking if ports not falling over power/gnd bars can be connected using existing contacts
            // along same X axis
//            PortInst[] ports = new PortInst[portNotReadList.size()];
//            portNotReadList.toArray(ports);
//            portNotReadList.clear();
//            Rectangle2D[] rects = new Rectangle2D[ports.length];
//            bndNotReadList.toArray(rects);
//            bndNotReadList.clear();
//
//            for (int i = 0; i < ports.length; i++)
//            {
//                PortInst p = ports[i];
//                Rectangle2D portBnd = rects[i];
//                NodeInst minNi = connectToExistingContacts(p, portBnd, fillContactList, fillPortInstList);
//
//                if (minNi != null)
//                {
//                    int index = fillContactList.indexOf(minNi);
//                    PortInst fillNiPort = fillPortInstList.get(index);
//                    // Connecting the export in the top cell
//                    Route exportRoute = router.planRoute(topCell, p, fillNiPort,
//                            new Point2D.Double(p.getBounds().getCenterX(), p.getBounds().getCenterY()), null, false);
//                    Router.createRouteNoJob(exportRoute, topCell, true, false, null);
//                }
//                else
//                {
//                    portNotReadList.add(p);
//                    bndNotReadList.add(rects[i]);
//                }
//            }
//
//            // If nothing works, try to insert contacts in location with same Y
//            // Cleaning fillContacts so it doesn't try again with the same sets
//            fillPortInstList.clear();
//            fillContactList.clear();
//            for (int i = 0; i < portNotReadList.size(); i++)
//            {
//                PortInst p = portNotReadList.get(i);
//                Rectangle2D r = bndNotReadList.get(i);
//                double newWid = r.getWidth()+globalWidth;
//                Rectangle2D rect = new Rectangle2D.Double(r.getX()-newWid, r.getY(),
//                        2*newWid, r.getHeight()); // copy the rectangle to add extra width
//
//                // Check possible new contacts added
//                NodeInst minNi = connectToExistingContacts(p, rect, fillContactList, fillPortInstList);
//                if (minNi != null)
//                {
//                    int index = fillContactList.indexOf(minNi);
//                    PortInst fillNiPort = fillPortInstList.get(index);
//                    // Connecting the export in the top cell
//                    Route exportRoute = router.planRoute(topCell, p, fillNiPort,
//                            new Point2D.Double(p.getBounds().getCenterX(), p.getBounds().getCenterY()), null, false);
//                    Router.createRouteNoJob(exportRoute, topCell, true, false, null);
//                }
//                else
//                {
//                    // Searching arcs again
//                    Geometric geom = routeToClosestArc(container, p, rect, 10, fillTransOut);
//                    if (geom == null)
//                    {
//                        ErrorLogger.MessageLog l = log.logError(p.describe(false) + " not connected", topCell, 0);
//                        l.addPoly(p.getPoly(), true, topCell);
//                        if (p.getPortProto() instanceof Export)
//                            l.addExport((Export)p.getPortProto(), true, topCell, null);
//                        l.addGeom(p.getNodeInst(), true, fillCell, null);
//                    }
//                }
//            }

            return true;
        }

        public void terminateOK()
        {
            log.termLogging(false);
            long endTime = System.currentTimeMillis();
            int errorCount = log.getNumErrors();
            int warnCount = log.getNumWarnings();
            System.out.println(errorCount + " errors and " + warnCount + " warnings found (took " + TextUtils.getElapsedTime(endTime - startTime) + ")");
        }

        /**
         * Method to detect which fill nodes are overlapping in the top cell.
         * @param cell
         * @param fillTransUp matrix
         */
        private boolean detectOverlappingBars(Cell cell, AffineTransform fillTransUp, HashSet<Geometric> nodesToRemove,
                                              FillGenJobContainer container)
        {
            List<Layer.Function> tmp = new ArrayList<Layer.Function>();

            // Check if any metalXY must be removed
            for (Iterator<NodeInst> itNode = cell.getNodes(); itNode.hasNext(); )
            {
                NodeInst ni = itNode.next();

                if (NodeInst.isSpecialNode(ni)) continue;

                tmp.clear();
                NodeProto np = ni.getProto();
                if (ni.isCellInstance())
                {
                    Cell subCell = (Cell)ni.getProto();
                    AffineTransform subTransUp = ni.transformOut(fillTransUp);
                    // No need of checking the rest of the elements if first one is detected.
                    if (detectOverlappingBars(subCell, subTransUp, nodesToRemove, container))
                    {
                        if (cell == container.fillCell)
                            nodesToRemove.add(ni);
                        else
                            return true;
                    }
                    continue;
                }
                PrimitiveNode pn = (PrimitiveNode)np;
                if (pn.getFunction() == PrimitiveNode.Function.PIN) continue; // pins have pseudo layers

                for (Technology.NodeLayer tlayer : pn.getLayers())
                {
                    tmp.add(tlayer.getLayer().getFunction());
                }
                Rectangle2D rect = getSearchRectangle(ni.getBounds(), fillTransUp, fillGen.drcSpacing);
                if (searchCollision(topCell, rect, tmp, null,
                        new NodeInst[] {container.fillNi, container.connectionNi}))
                {
                    // Direct on last top fill cell
                    if (cell == container.fillCell)
                        nodesToRemove.add(ni);
                    else
                        return true; // time to delete parent NodeInst
                }
            }

            // Checking if any arc in FillCell collides with rest of the cells
            for (Iterator<ArcInst> itArc = cell.getArcs(); itArc.hasNext(); )
            {
                ArcInst ai = itArc.next();
                tmp.clear();
                tmp.add(ai.getProto().getLayers()[0].getLayer().getNonPseudoLayer().getFunction());
                // Searching box must reflect DRC constrains
                Rectangle2D rect = getSearchRectangle(ai.getBounds(), fillTransUp, fillGen.drcSpacing);
                if (searchCollision(topCell, rect, tmp, null,
                        new NodeInst[] {container.fillNi, container.connectionNi}))
                {
                    if (cell == container.fillCell)
                    {
                        nodesToRemove.add(ai);
                        // Remove exports and pins as well
                        nodesToRemove.add(ai.getTail().getPortInst().getNodeInst());
                        nodesToRemove.add(ai.getHead().getPortInst().getNodeInst());
                    }
                    else
                        return true; // time to delete parent NodeInst.
                }
            }
            return false;
        }

        private void removeOverlappingBars(FillGenJobContainer container, AffineTransform fillTransOut)
        {
            // Check if any metalXY must be removed
            HashSet<Geometric> nodesToRemove = new HashSet<Geometric>();

            // This function should replace NodeInsts for temporary cells that don't have elements overlapping
            // the standard fill cells.
            // DRC conditions to detect overlap otherwise too many elements/cells might be discarded.
            detectOverlappingBars(container.fillCell, fillTransOut, nodesToRemove, container);

            for (Geometric geo : nodesToRemove)
            {
                System.out.println("Removing " + geo);
                if (geo instanceof NodeInst)
                    ((NodeInst)geo).kill();
                else
                    ((ArcInst)geo).kill();
            }
        }

//        private NodeInst connectToExistingContacts(PortInst p, Rectangle2D portBnd,
//                                               List<NodeInst> fillContactList, List<PortInst> fillPortInstList)
//        {
//            double minDist = Double.POSITIVE_INFINITY;
//            NodeInst minNi = null;
//
//            for (int j = 0; j < fillContactList.size(); j++)
//            {
//                NodeInst ni = fillContactList.get(j);
//                PortInst fillNiPort = fillPortInstList.get(j);
//                // Checking only the X distance between a placed contact and the port
//                Rectangle2D contBox = ni.getBounds();
//
//                // check if contact is connected to the same grid
//                if (fillNiPort.getPortProto().getCharacteristic() != p.getPortProto().getCharacteristic())
//                    continue; // no match in network type
//
//                // If they are not aligned on Y, discard
//                if (!DBMath.areEquals(contBox.getCenterY(), portBnd.getCenterY())) continue;
//                double pdx = Math.abs(Math.max(contBox.getMinX()-portBnd.getMaxX(), portBnd.getMinX()-contBox.getMaxX()));
//                if (pdx < minDist)
//                {
//                    minNi = ni;
//                    minDist = pdx;
//                }
//            }
//            return minNi;
//        }

        private class FillGenJobContainer
        {
            InteractiveRouter router;
            Cell fillCell, connectionCell;
            NodeInst fillNi, connectionNi;
            List<PortInst> fillPortInstList;
            List<NodeInst> fillContactList;

            FillGenJobContainer(InteractiveRouter r, Cell fC, NodeInst fNi, Cell cC, NodeInst cNi)
            {
                this.router = r;
                this.fillCell = fC;
                this.fillNi = fNi;
                this.connectionCell = cC;
                this.connectionNi = cNi;
                this.fillPortInstList = new ArrayList<PortInst>();
                this.fillContactList = new ArrayList<NodeInst>();
            }
        }

        /**
         * THIS METHOD ASSUMES contactAreaOrig is horizontal!
         */
        private void searchOverlapHierarchically(Cell searchCell, GeometryHandler handler, PortConfig p,
                                                 Rectangle2D contactAreaOrig, AffineTransform downTrans, AffineTransform upTrans)
        {
            Rectangle2D contactArea = (Rectangle2D)contactAreaOrig.clone();
            double contactAreaHeight = contactArea.getHeight();
            DBMath.transformRect(contactArea, downTrans);

            Netlist fillNetlist = searchCell.acquireUserNetlist();

            // Give high priority to lower arcs
            HashMap<Layer, List<ArcInst>> protoMap = new HashMap<Layer, List<ArcInst>>();

            for (Iterator<Geometric> it = searchCell.searchIterator(contactArea); it.hasNext(); )
            {
                // Check if there is a contact on that place already!
                Geometric geom = it.next();

                if (geom instanceof NodeInst)
                {
                    NodeInst ni = (NodeInst)geom;
                    if (!ni.isCellInstance()) continue;

                    AffineTransform fillIn = ni.transformIn();
                    AffineTransform fillUp = ni.transformOut(upTrans);
                    // In case of being a cell
                    searchOverlapHierarchically((Cell)ni.getProto(), handler, p, contactArea, fillIn, fillUp);
                    continue;
                }

                ArcInst ai = (ArcInst)geom;
                ArcProto ap = ai.getProto();
                Network arcNet = fillNetlist.getNetwork(ai, 0);

                // No export with the same characteristic found in this netlist
                if (arcNet.findExportWithSameCharacteristic(p.e) == null)
                    continue; // no match in network type

//                if (ap != Tech.m3)
//                {
//                    System.out.println("picking  metal");
//                    continue; // Only metal 3 arcs
//                }

                // Adding now
                Layer layer = ap.layerIterator().next();
                List<ArcInst> list = protoMap.get(layer);
                if (list == null)
                {
                    list = new ArrayList<ArcInst>();
                    protoMap.put(layer, list);
                }
                list.add(ai);
            }

            // Assign priority to lower metal bars. eg m3 instead of m4
            Set<Layer> results = protoMap.keySet();
            List<Layer> listOfLayers = new ArrayList<Layer>(results.size());
            listOfLayers.addAll(results);
            Collections.sort(listOfLayers, Layer.layerSort);

            // Now select possible pins
            for (Layer layer : listOfLayers)
            {
                ArcProto ap = findArcProtoFromLayer(layer);
                boolean horizontalBar = (ap == Tech.m4 || ap == Tech.m6);
                PrimitiveNode defaultContact = null;

                if (horizontalBar)
                {
                    if (ap == Tech.m4)
                        defaultContact = Tech.m3m4;
                    else if (ap == Tech.m6)
                        defaultContact = Tech.m4m5;
                    else
                        assert(false);
                }

                boolean found = false;

                for (ArcInst ai : protoMap.get(layer))
                {
                    Rectangle2D geomBnd = ai.getBounds();

                    // Add only the piece that overlap. If more than 1 arc covers the same area -> only 1 contact
                    // will be added.
                    Rectangle2D newElem = null;
                    double usefulBar, newElemMin, newElemMax, areaMin, areaMax, geoMin, geoMax;

                    if (horizontalBar)
                    {
                        usefulBar = geomBnd.getHeight();
                        // search for the contacts m3m4 at least
                        Network net = fillNetlist.getNetwork(ai, 0);
                        List<Rectangle2D> nodes = new ArrayList<Rectangle2D>();

                        // get contact nodes in the same network
                        for (Iterator<NodeInst> it = searchCell.getNodes(); it.hasNext(); )
                        {
                            NodeInst ni = it.next();
                            Rectangle2D r = ni.getBounds();
                            // only contacts
                            if (ni.getProto().getFunction() != PrimitiveNode.Function.CONTACT) continue;
                            // Only those who overlap
                            if (!r.intersects(geomBnd)) continue;
                            for (Iterator<PortInst> pit = ni.getPortInsts(); pit.hasNext(); ) {
                                PortInst pi = pit.next();
                                // Only those
                                if (fillNetlist.getNetwork(pi) == net)
                                {
                                    nodes.add(r);
                                    break; // stop the loop here
                                }
                            }
                        }
                        // No contact on that bar
                        if (nodes.size() == 0)
                            newElem = new Rectangle2D.Double(contactArea.getX(), geomBnd.getY(), defaultContact.getDefWidth(), usefulBar);
                        else
                        {
                            // search for closest distance or I could add all!!
                            // Taking the first element for now
                            geomBnd = (Rectangle2D)nodes.get(0).getBounds().clone();
                            newElem = new Rectangle2D.Double(geomBnd.getX(), geomBnd.getY(), geomBnd.getWidth(), geomBnd.getHeight());
                        }
                        newElemMin = newElem.getMinY();
                        newElemMax = newElem.getMaxY();
                        areaMin = contactArea.getMinY();
                        areaMax = contactArea.getMaxY();
                        geoMin = geomBnd.getMinY();
                        geoMax = geomBnd.getMaxY();
                    }
                    else
                    {
                        usefulBar = geomBnd.getWidth();
                        newElem = new Rectangle2D.Double(geomBnd.getX(), contactArea.getY(), usefulBar, contactAreaHeight);
                        newElemMin = newElem.getMinX();
                        newElemMax = newElem.getMaxX();
                        areaMin = contactArea.getMinX();
                        areaMax = contactArea.getMaxX();
                        geoMin = geomBnd.getMinX();
                        geoMax = geomBnd.getMaxX();
                    }

                    // Don't consider no overlapping areas
                    if (newElemMax < areaMin || areaMax < newElemMin)
                        continue;
                    boolean containMin = newElemMin <= areaMin && areaMin <= newElemMax;
                    boolean containMax = newElemMin <= areaMax && areaMax <= newElemMax;

                    // Either end is not contained otherwise the contact is fully contained by the arc
                    if (!containMin || !containMax)
                    {
                        // Getting the intersection along X/Y axis. Along YX it should cover completely
                        assert(geoMin == newElemMin);
                        assert(geoMax == newElemMax);

                        double min = Math.max(geoMin, areaMin);
                        double max = Math.min(geoMax, areaMax);
                        double overlap = (max-min)/usefulBar;
                        // Checking if new element is completely inside the contactArea otherwise routeToClosestArc could add
                        // the missing contact
                        if (overlap < minOverlap)
                        {
                            System.out.println("Not enough overlap (" + overlap + ") in " + ai + " to cover " + p);
                            continue;
                        }
                    }
                    // Transforming geometry up to fillCell coordinates
                    DBMath.transformRect(newElem, upTrans);
                    // Adding element
                    handler.add(ai.getProto().layerIterator().next(), newElem);
                    found = true;
                }
                if (found)
                    return; // only one set for now if something overlapping was found
            }
        }

        /**
         * Method to find corresponding metal pin associated to the given layer
         * @param layer
         * @return
         */
        private static PrimitiveNode findPrimitiveNodeFromLayer(Layer layer)
        {
            for (PrimitiveNode pin : VddGndStraps.PINS)
            {
                if (pin != null && layer == pin.layerIterator().next().getNonPseudoLayer())
                {
                    return pin; // found
                }
            }
            return null;
        }

        /**
         * Method to find corresponding metal arc associated to the given layer
         * @param layer
         * @return
         */
        private static ArcProto findArcProtoFromLayer(Layer layer)
        {
            for (ArcProto arc : VddGndStraps.METALS)
            {
                if (arc != null && layer == arc.layerIterator().next().getNonPseudoLayer())
                {
                    return arc; // found
                }
            }
            return null;
        }

        /**
         * Method to add all possible contacts in connection cell based on the overlap of a given metal2 area
         * and fill cell.
         * THIS ONLY WORK if first fill bar is vertical
         * @param container
         * @param p
         * @param contactArea
         * @param nodeTransOut null if the port is on the top cell
         * @param fillTransOutToCon
         * @return
         */
        private NodeInst addAllPossibleContacts(FillGenJobContainer container, PortConfig p, Rectangle2D contactArea,
                                                AffineTransform nodeTransOut, AffineTransform fillTransIn, AffineTransform fillTransOutToCon,
                                                AffineTransform conTransOut)
        {
            // Until this point, contactArea is at the fillCell level
            // Contact area will contain the remaining are to check
            double contactAreaHeight = contactArea.getHeight();
            double contactAreaWidth = contactArea.getWidth();

            NodeInst added = null;
            // Transforming rectangle with gnd/power metal into the connection cell
            if (nodeTransOut != null)
                DBMath.transformRect(contactArea, nodeTransOut);
            GeometryHandler handler = GeometryHandler.createGeometryHandler(GeometryHandler.GHMode.ALGO_SWEEP, 1);

            searchOverlapHierarchically(container.fillCell, handler, p, contactArea, fillTransIn, new AffineTransform());
            handler.postProcess(false);

            Set<Layer> results = handler.getKeySet();
            int size = results.size();

//            assert(size <= 1); // Must contain only m3

            if (size == 0) return null;

            Rectangle2D portInConFill = new Rectangle2D.Double();
            portInConFill.setRect(p.pPoly.getBounds2D());
            DBMath.transformRect(portInConFill, fillTransIn);

            // Creating the corresponding export in connectionNi (projection pin)
            // This should be done only once!
            PrimitiveNode thePin = findPrimitiveNodeFromLayer(p.l);
            assert(thePin != null);
            assert(thePin != Tech.m1pin); // should start from m2
            NodeInst pinNode = null;
            PortInst pin = null;
//            LayoutLib.newNodeInst(thePin, portInConFill.getCenterX(), portInConFill.getCenterY(),
//                    thePin.getDefWidth(), contactAreaHeight, 0, container.connectionCell);
//            PortInst pin = pinNode.getOnlyPortInst();

            // Loop along all possible connections (different layers)
            List<Layer> listOfLayers = new ArrayList<Layer>(size);
            listOfLayers.addAll(results);
            Collections.sort(listOfLayers, Layer.layerSort);

            for (Layer layer : listOfLayers)
            {
                Collection set = handler.getObjects(layer, false, true);

                // Get connecting metal contact (PrimitiveNode) starting from techPin up to the power/vdd bar found
                List<Layer.Function> fillLayers = new ArrayList<Layer.Function>();
                PrimitiveNode topPin = findPrimitiveNodeFromLayer(layer);
                PrimitiveNode topContact = null;
                int start = -1;
                int end = -1;
                for (int i = 0; i < VddGndStraps.PINS.length; i++)
                {
                    if (start == -1 && VddGndStraps.PINS[i] == thePin)
                        start = i;
                    if (end == -1 && VddGndStraps.PINS[i] == topPin)
                        end = i;
                }
                for (int i = start; i <= end; i++)
                {
                    fillLayers.add(VddGndStraps.PINS[i].layerIterator().next().getFunction());
                    if (i < end)
                        topContact = VddGndStraps.fillContacts[i];
                }

//                if (topContact == null)
//                    System.out.println("Here ");
                assert(topContact != null);
                boolean horizontalBar = (topPin == Tech.m4pin || topPin == Tech.m6pin);

                for (Iterator it = set.iterator(); it.hasNext(); )
                {
                    // ALGO_SWEEP retrieves only PolyBase
                    PolyBase poly = (PolyBase)it.next();
                    Rectangle2D newElemFill = poly.getBounds2D();
                    double newElemFillWidth = newElemFill.getWidth();
                    double newElemFillHeight = newElemFill.getHeight();

                    // Location of new element in fillCell
                    Rectangle2D newElemConnect = (Rectangle2D)newElemFill.clone();
                    DBMath.transformRect(newElemConnect, fillTransOutToCon);

                    // Location of new contact from top cell
                    Rectangle2D newElemTop = (Rectangle2D)newElemConnect.clone();
                    DBMath.transformRect(newElemTop, conTransOut);

                    // Get connecting metal contact (PrimitiveNode) starting from techPin up to the power/vdd bar found
                    // Search if there is a collision with existing nodes/arcs
                    if (searchCollision(topCell, newElemTop, fillLayers, p,
                            new NodeInst[] {container.fillNi, container.connectionNi}))
                        continue;

                    // The first time but only after at least one element can be placed
                    if (pinNode == null)
                    {
                        pinNode = LayoutLib.newNodeInst(thePin, portInConFill.getCenterX(), portInConFill.getCenterY(),
                                thePin.getDefWidth(), contactAreaHeight, 0, container.connectionCell);
                        pin = pinNode.getOnlyPortInst();
                    }

                    if (topContact != null)
                    {
                        // adding contact
                        added = horizontalBar ?
                                LayoutLib.newNodeInst(topContact, newElemConnect.getCenterX(), newElemConnect.getCenterY(),
                                newElemFillWidth, newElemFillHeight, 0, container.connectionCell) :
                                LayoutLib.newNodeInst(topContact, newElemConnect.getCenterX(), newElemConnect.getCenterY(),
                                newElemFillWidth, contactAreaHeight, 0, container.connectionCell);
                    }
                    else // on the same layer as thePin
                       added = pinNode;

                    container.fillContactList.add(added);
    //                 // Creating the export above the contact in the connection cell
    //                Export conM2Export = Export.newInstance(container.connectionCell, added.getOnlyPortInst(),
    //                        p.getPortProto().getName());
    //                conM2Export.setCharacteristic(p.getPortProto().getCharacteristic());

                    // Connecting the contact export in the top cell
    //                PortInst conNiPort = container.connectionNi.findPortInstFromProto(conM2Export);
    //                container.fillPortInstList.add(conNiPort);
    //                Route conTopExportRoute = container.router.planRoute(topCell, p, conNiPort,
    //                        new Point2D.Double(newElemTop.getCenterX(), newElemTop.getCenterY()), null, false);
    //                Router.createRouteNoJob(conTopExportRoute, topCell, true, false);

                    // route new pin instance in connectioNi with new contact
                    Route pinExportRoute = container.router.planRoute(container.connectionCell, pin, added.getOnlyPortInst(),
                            new Point2D.Double(portInConFill.getCenterX(), portInConFill.getCenterY()), null, false);
                    Router.createRouteNoJob(pinExportRoute, container.connectionCell, true, false);

                    // Adding the connection to the fill via the exports.
                    // Looking for closest export in fillCell.
                    PortInst fillNiPort = null;
                    double minDistance = Double.POSITIVE_INFINITY;

                    for (Iterator<Export> e = container.fillNi.getExports(); e.hasNext();)
                    {
                        Export exp = e.next();
                        PortInst port = exp.getOriginalPort();

                        // The port characteristics must be identical
                        boolean newC = port.getPortProto().getCharacteristic() != p.e.getCharacteristic();
                        boolean oldC = port.getPortProto().getCharacteristic() != p.p.getPortProto().getCharacteristic();
                        assert(newC == oldC);
                        if (newC != oldC)
                            System.out.println("Check this");
                        if (port.getPortProto().getCharacteristic() != p.p.getPortProto().getCharacteristic())
                            continue;

                        Rectangle2D geo = port.getPoly().getBounds2D();
                        assert(fillGen.evenLayersHorizontal);
                        boolean condition = (horizontalBar) ?
                                DBMath.areEquals(geo.getCenterY(), newElemConnect.getCenterY()) :
                                DBMath.areEquals(geo.getCenterX(), newElemConnect.getCenterX());
                        if (!condition)
                            continue; // only align with this so it could guarantee correct arc (M3)
                        double deltaX = geo.getCenterX() - newElemConnect.getCenterX();
                        double deltaY = geo.getCenterY() - newElemConnect.getCenterY();
                        double dist = Math.sqrt(deltaX*deltaX + deltaY*deltaY);
                        if (DBMath.isGreaterThan(minDistance, dist))
                        {
                            minDistance = dist;
                            fillNiPort = port;
                        }
                    }
                    if (fillNiPort != null)
                    {
                        Rectangle2D r = fillNiPort.getBounds();
                        Route exportRoute = container.router.planRoute(container.connectionCell, added.getOnlyPortInst(), fillNiPort,
                            new Point2D.Double(r.getCenterX(), r.getCenterY()), null, false);
                        Router.createRouteNoJob(exportRoute, container.connectionCell, true, false);
                    }
                }

                // Done at the end so extra connections would not produce collisions.
                // Routing the new contact to topCell in connectNi instead of top cell
                // Export connect projected pin in ConnectionCell
                if (pinNode != null) // at least done for one
                {
                    Export pinExport = Export.newInstance(container.connectionCell, pin, "proj-"+p.e.getName());
                    if (pinExport != null)
                    {
                        pinExport.setCharacteristic(p.e.getCharacteristic());
                        // Connect projected pin in ConnectionCell with real port
                        PortInst pinPort = container.connectionNi.findPortInstFromProto(pinExport);
                        Route conTopExportRoute = container.router.planRoute(topCell, p.p, pinPort,
                                new Point2D.Double(p.pPoly.getBounds2D().getCenterX(), p.pPoly.getBounds2D().getCenterY()), null, false);
                        Router.createRouteNoJob(conTopExportRoute, topCell, true, false);
                    }
                    else
                        System.out.println("DD");

                    return added;
                }
            }
            return null;
        }

        /**
         * Method to wire an export with the closest fill bar available
         * @param container
         * @param p
         * @param nodeBounds
         * @param height
         * @param fillTransOut
         * @return
         */
//        private Geometric routeToClosestArc(FillGenJobContainer container, PortInst p, Rectangle2D nodeBounds,
//                                            double height, AffineTransform fillTransOut)
//        {
//            Netlist fillNetlist = container.fillCell.acquireUserNetlist();
//            for(Iterator<Geometric> it = container.fillCell.searchIterator(nodeBounds); it.hasNext(); )
//            {
//                // Check if there is a contact on that place already!
//                Geometric geom = it.next();
//                if (!(geom instanceof ArcInst)) continue;
//                ArcInst ai = (ArcInst)geom;
//                if (ai.getProto() != Tech.m3) continue; // Only metal 3 arcs
//                Network arcNet = fillNetlist.getNetwork(ai, 0);
//
//                // No export with the same characteristic found in this netlist
//                if (arcNet.findExportWithSameCharacteristic(p) == null)
//                    continue; // no match in network type
//
//                Rectangle2D geomBnd = geom.getBounds();
//                double width = geomBnd.getWidth();
//
//                // Transform location of the new contact and make sure nothing else overlap
//                Rectangle2D newElem = new Rectangle2D.Double(geomBnd.getX(), nodeBounds.getY()-height/2, width, height);
//                DBMath.transformRect(newElem, fillTransOut);
//
//                // Search if there is a collision with existing nodes/arcs
//                if (searchCollision(topCell, newElem, fillLayers, p, container.fillNi, container.connectionNi, null))
//                    continue;
//
//                NodeInst added = LayoutLib.newNodeInst(Tech.m2m3, geomBnd.getCenterX(), nodeBounds.getCenterY(),
//                        width, height, 0, container.connectionCell);
//                container.fillContactList.add(added);
//                 // Creating the export above the contact in the connection cell
//                Export conM2Export = LayoutLib.newExport(container.connectionCell, p.getPortProto().getName(), p.getPortProto().getCharacteristic(), Tech.m2,
//                        height, nodeBounds.getCenterX(), nodeBounds.getCenterY());
//                Route conExportRoute = container.router.planRoute(container.connectionCell, added.getOnlyPortInst(), conM2Export.getOriginalPort(),
//                        new Point2D.Double(nodeBounds.getCenterX(), nodeBounds.getCenterY()), null, false);
//                Router.createRouteNoJob(conExportRoute, container.connectionCell, true, false);
//                // Connecting the contact export in the top cell
//                PortInst conNiPort = container.connectionNi.findPortInstFromProto(conM2Export);
//                container.fillPortInstList.add(conNiPort);
//                Route conTopExportRoute = container.router.planRoute(topCell, p, conNiPort,
//                        new Point2D.Double(p.getBounds().getCenterX(), p.getBounds().getCenterY()), null, false);
//                Router.createRouteNoJob(conTopExportRoute, topCell, true, false);
//
//                // Creating the export above the pin in the fill cell
//                Export pinExport = LayoutLib.newExport(container.fillCell, p.getPortProto().getName(), p.getPortProto().getCharacteristic(), ai.getProto(),
//                        height, geomBnd.getCenterX(), nodeBounds.getCenterY());
//                Route pinExportRoute = container.router.planRoute(container.fillCell, ai, pinExport.getOriginalPort(),
//                        new Point2D.Double(geomBnd.getCenterX(), nodeBounds.getCenterY()), null, false);
//                Router.createRouteNoJob(pinExportRoute, container.fillCell, true, false);
//                // Connecting the export in the top cell
//                PortInst fillNiPort = container.fillNi.findPortInstFromProto(pinExport);
//                Route exportRoute = container.router.planRoute(container.connectionCell, added.getOnlyPortInst(), fillNiPort,
//                        new Point2D.Double(fillNiPort.getBounds().getCenterX(), fillNiPort.getBounds().getCenterY()), null, false);
//                Router.createRouteNoJob(exportRoute, container.connectionCell, true, false);
//
//                return geom;
//            }
//            return null;
//        }
    }

    public static FillGenJob generateAutoFill(Cell cell, boolean hierarchy, boolean binary, boolean doItNow)
    {
        // Creating fill template
        FillGenerator fg = new FillGenerator(cell.getTechnology());
        fg.setFillLibrary("autoFillLib");
        Rectangle2D bnd = cell.getBounds();

        double drcSpacingRule = 6;
        double vddReserve = drcSpacingRule*2;
        double gndReserve = drcSpacingRule*3;
        double minSize = vddReserve + gndReserve + 2*drcSpacingRule + 2*gndReserve + 2*vddReserve;
        fg.setTargetValues(bnd.getWidth(), bnd.getHeight(), minSize, minSize);
        fg.setFillCellWidth(bnd.getWidth());
        fg.setFillCellHeight(bnd.getHeight());
        fg.setDRCSpacing(drcSpacingRule);
        fg.setBinaryMode(binary);
        fg.makeEvenLayersHorizontal(true);
        fg.reserveSpaceOnLayer(3, vddReserve, FillGenerator.LAMBDA, gndReserve, FillGenerator.LAMBDA);
        fg.reserveSpaceOnLayer(4, vddReserve, FillGenerator.LAMBDA, gndReserve, FillGenerator.LAMBDA);
        FillGenJob job = new FillGenJob(cell, fg, FillGenerator.PERIMETER, 3, 4, null, hierarchy, doItNow, 0.1);
        return job;
    }
}

