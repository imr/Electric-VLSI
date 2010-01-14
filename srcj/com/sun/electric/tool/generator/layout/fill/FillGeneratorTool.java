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
 * the Free Software Foundation; either version 3 of the License, or
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

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.JobException;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.generator.layout.Gallery;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.generator.layout.TechType;

import java.lang.reflect.Constructor;
import java.util.*;

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

class MetalLayer extends VddGndStraps {
	protected MetalFloorplanBase plan;
	protected int layerNum;
	protected PrimitiveNode pin;
	protected ArcProto metal;
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

	public MetalLayer(TechType t, int layerNum, Floorplan plan, Cell cell)
    {
        super(t);
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

    public MetalLayerFlex(TechType t, int layerNum, Floorplan plan, Cell cell) {
        super(t, layerNum, plan, cell);
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

//---------------------------------- CapLayer ---------------------------------
class CapLayer extends VddGndStraps {
	private CapCell capCell;
	private NodeInst capCellInst;
	private CapFloorplan plan;

    public boolean addExtraArc() { return true; }

	public CapLayer(TechType t, CapFloorplan plan, CapCell capCell, Cell cell)
    {
        super(t);
        this.plan = plan;
		this.capCell = capCell;

		double angle = plan.horizontal ? 0 : 90;
        if (capCell != null)
            capCellInst = LayoutLib.newNodeInst(capCell.getCell(), 0, 0, G.DEF_SIZE,
									 		G.DEF_SIZE, angle, cell);
	}

	public boolean isHorizontal() {return plan.horizontal;}
	public int numVdd() {return (capCell != null) ? capCell.numVdd() : 0;}
	public PortInst getVdd(int n, int pos) {
		return capCellInst.findPortInst(FillCell.VDD_NAME+"_"+n);
	}
	public double getVddCenter(int n) {
        EPoint center = getVdd(n, 0).getCenter();
		return plan.horizontal ? center.getY() : center.getX();
	}
	public double getVddWidth(int n) {return capCell.getVddWidth();}
	public int numGnd() {return (capCell != null) ? capCell.numGnd() : 0;}
	public PortInst getGnd(int n, int pos) {
		return capCellInst.findPortInst(FillCell.GND_NAME+"_"+n);
	}
	public double getGndCenter(int n) {
        EPoint center = getGnd(n, 0).getCenter();
		return plan.horizontal ? center.getY() : center.getX();
	}
	public double getGndWidth(int n) {return capCell.getGndWidth();}

	public PrimitiveNode getPinType() {return tech.m1pin();}
	public ArcProto getMetalType() {return tech.m1();}
	public double getCellWidth() {return plan.cellWidth;}
	public double getCellHeight() {return plan.cellHeight;}
	public int getLayerNumber() {return 1;}
}


class FillRouter {
	private HashMap<String,List<PortInst>> portMap = new HashMap<String,List<PortInst>>();
    private TechType tech;

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
		ArcProto[] metals = {tech.m6(), tech.m5(), tech.m4(), tech.m3(), tech.m2(), tech.m1()};
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
	private FillRouter(TechType t, ArrayList<PortInst> ports)
    {
        tech = t;
        for (PortInst pi : ports) {
			String key = makeKey(pi);
			List<PortInst> l = portMap.get(key);
			if (l==null) {
				l = new LinkedList<PortInst>();
				portMap.put(key, l);
			}
			l.add(pi);
		}
        // to guarantee deterministic results
        List<String> keys = new ArrayList<String>();
        keys.addAll(portMap.keySet());
        Collections.sort(keys);
        for (String str : keys) {
			connectPorts(portMap.get(str));
		}
	}
	public static void connectCoincident(TechType t, ArrayList<PortInst> ports) {
		new FillRouter(t, ports);
	}
}

/**
 * Object for building fill libraries
 */
public class FillGeneratorTool extends Tool {
    public FillGenConfig config;
    protected Library lib;
    private boolean libInitialized;
    public List<Cell> masters;
    protected CapCell capCell;
    protected Floorplan[] plans;
    
    /** the fill generator tool. */								private static FillGeneratorTool tool = getTool();
     // Depending on generator plugin available
    public static FillGeneratorTool getTool()
    {
        if (tool != null) return tool;

        FillGeneratorTool tool;
        try
        {
            Class<?> extraClass = Class.forName("com.sun.electric.plugins.generator.FillCellTool");
            Constructor instance = extraClass.getDeclaredConstructor(); // varags
            Object obj = instance.newInstance();  // varargs;
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
    }

    public void setConfig(FillGenConfig config)
    {
        this.config = config;
        this.libInitialized = false; 
    }

    public enum Units {NONE, LAMBDA, TRACKS}
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
        return config.getTechType().reservedToLambda(layer, nbTracks);
    }

    private Floorplan[] makeFloorplans(boolean metalFlex, boolean hierFlex) {
        Job.error(config.width==Double.NaN,
                        "width hasn't been specified. use setWidth()");
        Job.error(config.height==Double.NaN,
                        "height hasn't been specified. use setHeight()");
        double w = config.width;
        double h = config.height;
        int numLayers = config.getTechType().getNumMetals() + 1; // one extra for the cap
        double[] vddRes = new double[numLayers]; //{0,0,0,0,0,0,0};
        double[] gndRes = new double[numLayers]; //{0,0,0,0,0,0,0};
        double[] vddW = new double[numLayers]; //{0,0,0,0,0,0,0};
        double[] gndW = new double[numLayers]; //{0,0,0,0,0,0,0};

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
        double[] spacing = new double[numLayers];
        for (int i = 0; i < numLayers; i++) spacing[i] = config.drcSpacingRule;
//                {config.drcSpacingRule,config.drcSpacingRule,
//                config.drcSpacingRule,config.drcSpacingRule,
//                config.drcSpacingRule,config.drcSpacingRule,config.drcSpacingRule};

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

        Floorplan[] thePlans = new Floorplan[numLayers];
        // 0 is always null
        thePlans[1] = new CapFloorplan(w, h, !evenHor);
        if (metalFlex)
        {
            if (!hierFlex)
            {
                for (int i = 2; i < numLayers; i++)
                {
                    boolean horiz = (i%2==0);
                    thePlans[i] = new MetalFloorplanFlex(w, h, vddRes[i], gndRes[i], spacing[i], vddW[i], gndW[i], horiz);
                }
                return thePlans;
            }
            w = config.width = config.minTileSizeX;
            h = config.height = config.minTileSizeY;
        }

        for (int i = 2; i < numLayers; i++)
        {
            boolean horiz = (i%2==0);
            thePlans[i] = new MetalFloorplan(w, h, vddRes[i], gndRes[i], spacing[i],  horiz);
        }
        return thePlans;
    }

    private void printCoverage(Floorplan[] plans) {
        for (int i=2; i<plans.length; i++) {
            System.out.println("metal-"+i+" coverage: "+
                               ((MetalFloorplan)plans[i]).coverage);
        }
    }

    private static CapCell getCMOS90CapCell(Library lib, CapFloorplan plan)
    {
        CapCell c = null;
        try
		{
			Class<?> cmos90Class = Class.forName("com.sun.electric.plugins.tsmc.fill90nm.CapCellCMOS90");
            Constructor capCellC = cmos90Class.getDeclaredConstructor(Library.class, CapFloorplan.class);   // varargs
            Object cell = capCellC.newInstance(lib, plan);
            c = (CapCell)cell;
         } catch (Exception e)
        {
            assert(false); // runtime error
        }
 		return c;
    }

    protected void initFillParameters(boolean metalFlex, boolean hierFlex) {
        if (libInitialized) return;

        Job.error(config.fillLibName==null, "no library specified. Use setFillLibrary()");
        Job.error((config.width==Double.NaN || config.width<=0), "no width specified. Use setFillCellWidth()");
        Job.error((config.height==Double.NaN || config.height<=0), "no height specified. Use setFillCellHeight()");

        plans = makeFloorplans(metalFlex, hierFlex);
        if (!metalFlex) printCoverage(plans);

        lib = LayoutLib.openLibForWrite(config.fillLibName);
        if (!metalFlex) // don't do transistors
        {
            if (config.is180Tech())
            {
                capCell = new CapCellMosis(lib, (CapFloorplan) plans[1], config.getTechType());
            }
            else
            {
                capCell = getCMOS90CapCell(lib, (CapFloorplan) plans[1]);
            }
        }
        libInitialized = true;
    }

    private void makeTiledCells(Cell cell, Floorplan[] plans, Library lib,
                                int[] tiledSizes) {
        if (tiledSizes==null) return;
        for (int num : tiledSizes)
        {
            TiledCell.makeTiledCell(num, num, cell, plans, lib);
        }
    }

	public static Cell makeFillCell(Library lib, Floorplan[] plans,
                                    int botLayer, int topLayer, CapCell capCell,
                                    TechType tech,
                                    ExportConfig expCfg, boolean metalFlex, boolean hierFlex) {
		FillCell fc = new FillCell(tech);

		return fc.makeFillCell1(lib, plans, botLayer, topLayer, capCell,
		                        expCfg, metalFlex, hierFlex);
	}

    /**
     * Method to create standard set of tiled cells.
     */
    private Cell standardMakeAndTileCell(Library lib, Floorplan[] plans, int lowLay,
                                         int hiLay, CapCell capCell,
                                         TechType tech,
                                         ExportConfig expCfg,
                                         int[] tiledSizes, boolean metalFlex)
    {
        Cell master = makeFillCell(lib, plans, lowLay, hiLay, capCell,
                                   tech, expCfg, metalFlex, false);
        masters = new ArrayList<Cell>();
        masters.add(master);
        makeTiledCells(master, plans, lib, tiledSizes);
        return master;
    }

    public static final Units LAMBDA = Units.LAMBDA;
    public static final Units TRACKS = Units.TRACKS;
    //public static final PowerType POWER = PowerType.POWER;
    //public static final PowerType VDD = PowerType.VDD;
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
    public Cell standardMakeFillCell(int loLayer, int hiLayer, 
    		                         TechType tech,
    		                         ExportConfig exportConfig, 
                                     int[] tiledSizes, boolean metalFlex) {
        initFillParameters(metalFlex, false);

        Job.error(loLayer<1, "loLayer must be >=1");
        int maxNumMetals = config.getTechType().getNumMetals();
        Job.error(hiLayer>maxNumMetals, "hiLayer must be <=" + maxNumMetals);
        Job.error(loLayer>hiLayer, "loLayer must be <= hiLayer");
        Cell cell = null;
            cell = standardMakeAndTileCell(lib, plans, loLayer, hiLayer, capCell, 
            		                       tech, exportConfig,
            		                       tiledSizes, metalFlex);
        return cell;
    }

    public void makeGallery() {
        Gallery.makeGallery(lib);
    }

    public void writeLibrary(int backupScheme) throws JobException {
        LayoutLib.writeLibrary(lib, backupScheme);
    }

    public enum FillTypeEnum {INVALID,TEMPLATE,CELL}
}
