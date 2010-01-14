package com.sun.electric.tool.generator.layout.fill;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.tool.generator.layout.LayoutLib;
import com.sun.electric.tool.generator.layout.TechType;
import com.sun.electric.tool.Job;

//---------------------------------- FillCell --------------------------------
public class FillCell {
	public static final String VDD_NAME = "vdd";
	public static final String GND_NAME = "gnd";
	public static final PortCharacteristic VDD_CHARACTERISTIC = 
		PortCharacteristic.PWR;
	public static final PortCharacteristic GND_CHARACTERISTIC = 
		PortCharacteristic.GND;
	
	private int vddNum, gndNum;
	private final TechType tech;

	private String vddName() {
		int n = vddNum++;
		return VDD_NAME + (n==0 ? "" : ("_"+n));
	}
	private String gndName() {
		int n = gndNum++;
		return GND_NAME + (n==0 ? "" : ("_"+n));
	}

	public void exportPerimeter(VddGndStraps lay, Cell cell) {
		for (int i=0; i<lay.numGnd(); i++) {
			exportStripeEnds(i, lay, true, cell);
		}
		for (int i=0; i<lay.numVdd(); i++) {
			exportStripeEnds(i, lay, false, cell);
		}
	}
	public void exportPerimeter(VddGndStraps[] lays, int botLay, int topLay,
                                ExportConfig exptConfig, Cell cell) {
		int[] perim = exptConfig.getPerimeterExports(botLay, topLay);
		for (int i=0; i<perim.length; i++) {
			VddGndStraps lay = lays[perim[i]];
			if (lay!=null) exportPerimeter(lay, cell);
		}
	}
	private void exportStripeEnds(int n, VddGndStraps lay, 
			                      boolean gnd, Cell cell) {
		PrimitiveNode pin = lay.getPinType();
		ArcProto metal = lay.getMetalType();
		double edge = (lay.isHorizontal() ? lay.getCellWidth() : lay.getCellHeight())/2;
		double center = gnd ? lay.getGndCenter(n) : lay.getVddCenter(n);
		double width = gnd ? lay.getGndWidth(n) : lay.getVddWidth(n);
		PortInst piLeft = gnd ? lay.getGnd(n, 0) : lay.getVdd(n, 0);
        PortInst piRight = gnd ? lay.getGnd(n, 1) : lay.getVdd(n, 1);
		if (lay.isHorizontal()) {
			export(-edge, center, pin, metal, piLeft, width,
				   gnd ? gndName() : vddName(), gnd, cell, lay.addExtraArc());
			export(edge, center, pin, metal, piRight, width,
				   gnd ? gndName() : vddName(), gnd, cell, lay.addExtraArc());
		} else {
			export(center, -edge, pin, metal, piLeft, width,
				   gnd ? gndName() : vddName(), gnd, cell, lay.addExtraArc());
			export(center, edge, pin, metal, piRight, width,
				   gnd ? gndName() : vddName(), gnd, cell, lay.addExtraArc());
		}
	}
	private void export(double x, double y, PrimitiveNode pin,
						ArcProto metal, PortInst conn, double w,
						String name, boolean gnd, Cell cell,
						boolean withExtraArc)
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
		e.setCharacteristic(gnd ? GND_CHARACTERISTIC :
								  VDD_CHARACTERISTIC);
	}
	public void exportWiring(VddGndStraps lay, Cell cell) {
		for (int i=0; i<lay.numGnd(); i++) {
			exportStripeCenter(i, lay, true, cell);
		}
		for (int i=0; i<lay.numVdd(); i++) {
			exportStripeCenter(i, lay, false, cell);
		}
	}
	public void exportWiring(VddGndStraps[] lays, int botLay, int topLay,
                             ExportConfig exptConfig, Cell cell) {
		int[] intnl = exptConfig.getInternalExports(botLay, topLay);
		for (int i=0; i<intnl.length; i++) {
			VddGndStraps lay = lays[intnl[i]];
			if (lay!=null) exportWiring(lay, cell);
		}
	}
	private void exportStripeCenter(int n, VddGndStraps lay, 
			                        boolean gnd, Cell cell) {
		PrimitiveNode pin = lay.getPinType();
		ArcProto metal = lay.getMetalType();
		double center = gnd ? lay.getGndCenter(n) : lay.getVddCenter(n);
		double width = gnd ? lay.getGndWidth(n) : lay.getVddWidth(n);
		PortInst pi = gnd ? lay.getGnd(n, 0) : lay.getVdd(n, 0); // Doesn't matter which bar end is taken
		if (lay.isHorizontal()) {
			export(0, center, pin, metal, pi, width,
				   gnd ? gndName() : vddName(), gnd, cell, lay.addExtraArc());
		} else {
			export(center, 0, pin, metal, pi, width,
				   gnd ? gndName() : vddName(), gnd, cell, lay.addExtraArc());
		}
	}
	private void appendExportLayerNumbers(StringBuffer buf, 
                                          String type, int[] layerNbs) {
		if (layerNbs.length>0) {
			buf.append(type);
			for (int i=0; i<layerNbs.length; i++) {
				buf.append(layerNbs[i]);
			}
		}
	}
	private String fillName(int lo, int hi, ExportConfig expConfig) {
		StringBuffer buf = new StringBuffer();
		buf.append("fill");
		if (lo!=1 || hi!=tech.getNumMetals()) {
			for (int i=lo; i<=hi; i++)  buf.append(i);
		}
		if (expConfig==ExportConfig.PERIMETER) {
			// append nothing
		} else if (expConfig==ExportConfig.PERIMETER_AND_INTERNAL) {
			buf.append("w");
		} else {
			int[] perim = expConfig.getPerimeterExports(lo, hi);
			appendExportLayerNumbers(buf, "p", perim);
			int[] intnl = expConfig.getInternalExports(lo, hi);
			appendExportLayerNumbers(buf, "w", intnl);
		}
		buf.append("{lay}");
		return buf.toString();
	}

	private VddGndStraps[] findHoriVert(VddGndStraps lay1, VddGndStraps lay2) {
		if (lay1.isHorizontal()) {
			Job.error(lay2.isHorizontal(), "adjacent layers both horizontal");
			return new VddGndStraps[] {lay1, lay2};
		} else {
			Job.error(!lay2.isHorizontal(), "adjacent layers both vertical");
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
		PrimitiveNode viaType = tech.getViaFor(verMetal, horMetal);
		PortInst horPort = (verNdx%2==0) ? horLay.getVdd(horNdx, 0) : horLay.getVdd(horNdx, 1);
        // Line below will detect mixture of technologies.
        Job.error(viaType==null, "can't find via for metal layers " + verMetal + " " + horMetal);

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
		PrimitiveNode viaType = tech.getViaFor(verMetal, horMetal);
		PortInst horPort = (verNdx%2==0) ? horLay.getGnd(horNdx, 0) : horLay.getGnd(horNdx, 1);
		Job.error(viaType==null, "can't find via for metal layers");

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
                               int topLayer, CapCell capCell, 
                               ExportConfig expCfg,
                               boolean metalFlex, boolean hierFlex) {
		String name = fillName(botLayer, topLayer, expCfg);
		Cell cell = Cell.newInstance(lib, name);
		VddGndStraps[] layers = new VddGndStraps[topLayer+1];
		for (int i=topLayer; i>=botLayer; i--) {
			if (i==1) {
				layers[i] = new CapLayer(tech, (CapFloorplan) plans[i], capCell,
						                 cell);
			} else {
                if (metalFlex && !hierFlex)
				    layers[i] = new MetalLayerFlex(tech, i, plans[i], cell);
                else
                    layers[i] = new MetalLayer(tech, i, plans[i], cell);
			}
			if (i!=topLayer) {
				// connect to upper level
				connectLayers(layers[i], layers[i+1], cell);
			}
		}
		exportPerimeter(layers, botLayer, topLayer, expCfg, cell);
		exportWiring(layers, botLayer, topLayer, expCfg, cell);

		double cellWidth = plans[topLayer].cellWidth;
		double cellHeight = plans[topLayer].cellHeight;
		LayoutLib.newNodeInst(tech.essentialBounds(),
							  -cellWidth/2, -cellHeight/2,
							  G.DEF_SIZE, G.DEF_SIZE, 180, cell);
		LayoutLib.newNodeInst(tech.essentialBounds(),
							  cellWidth/2, cellHeight/2,
							  G.DEF_SIZE, G.DEF_SIZE, 0, cell);
//        cell.setCharacteristicSpacing(cellWidth, cellHeight);
        return cell;
	}
	protected FillCell(TechType tech) {
		this.tech = tech;
	}
}
