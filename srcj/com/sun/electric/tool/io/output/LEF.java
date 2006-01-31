/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: LEF.java
 * Input/output tool: LEF output
 * Written by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2004 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.io.output;

import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortOriginal;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Technology;
import com.sun.electric.tool.user.User;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;

/**
 * This is the netlister for LEF.
 *
 * Note that this writer was built by examining LEF files and reverse-engineering them.
 * It does not claim to be compliant with the LEF specification, but it also does not
 * claim to define a new specification.  It is merely incomplete.
 */
public class LEF extends Output
{
	private Layer io_lefoutcurlayer;
	private HashSet<NodeInst> nodesSeen;
	private HashSet<ArcInst> arcsSeen;

	/**
	 * The main entry point for LEF deck writing.
     * @param cell the top-level cell to write.
     * @param context the hierarchical context to the cell.
	 * @param filePath the disk file to create.
	 */
	public static void writeLEFFile(Cell cell, VarContext context, String filePath)
	{
		LEF out = new LEF();
		if (out.openTextOutputStream(filePath)) return;

		out.init(cell);
		HierarchyEnumerator.enumerateCell(cell, context, new Visitor(out), true);
//		HierarchyEnumerator.enumerateCell(cell, context, null, new Visitor(out));
		out.term(cell);

		if (out.closeTextOutputStream()) return;
		System.out.println(filePath + " written");
	}

	/**
	 * Creates a new instance of the LEF netlister.
	 */
	LEF()
	{
	}

	private static class Visitor extends HierarchyEnumerator.Visitor
	{
		private LEF generator;

		public Visitor(LEF generator)
		{
			this.generator = generator;
		}

		public boolean enterCell(HierarchyEnumerator.CellInfo info)
		{
 			generator.writeCellContents(info);
			return true;
		}

		public void exitCell(HierarchyEnumerator.CellInfo info) {}

		public boolean visitNodeInst(Nodable no, HierarchyEnumerator.CellInfo info) { return true; }
	}

	private void init(Cell cell)
	{
		// exclude resistors (short them)
		Netlist netList = cell.getNetlist(true);

		// write header information
		if (User.isIncludeDateAndVersionInOutput())
		{
			printWriter.println("# Electric VLSI Design System, version " + Version.getVersion());
			printWriter.println("# " + TextUtils.formatDate(new Date()));
		} else
		{
			printWriter.println("# Electric VLSI Design System");
		}
		emitCopyright("# ", "");
		printWriter.println("");
		printWriter.println("NAMESCASESENSITIVE ON ;");
		printWriter.println("UNITS");
		printWriter.println("  DATABASE MICRONS 1 ;");
		printWriter.println("END UNITS");
		printWriter.println("");

		// write layer information
		for(int i=0; i<8; i++)
		{
			printWriter.println("LAYER METAL" + (i+1));
			printWriter.println("  TYPE ROUTING ;");
			printWriter.println("END METAL" + (i+1));
			printWriter.println("");
		}
		printWriter.println("LAYER CONT");
		printWriter.println("  TYPE CUT ;");
		printWriter.println("END CONT");
		printWriter.println("");
		for(int i=0; i<3; i++)
		{
			printWriter.println("LAYER VIA" + (i+1) + (i+2));
			printWriter.println("  TYPE CUT ;");
			printWriter.println("END VIA: " + (i+1) + (i+2));
			printWriter.println("");
		}
		for(int i=0; i<3; i++)
		{
			printWriter.println("LAYER POLY" + (i+1));
			printWriter.println("  TYPE MASTERSLICE ;");
			printWriter.println("END POLY" + (i+1));
			printWriter.println("");
		}
		printWriter.println("LAYER PDIFF");
		printWriter.println("  TYPE MASTERSLICE ;");
		printWriter.println("END PDIFF");
		printWriter.println("");
		printWriter.println("LAYER NDIFF");
		printWriter.println("  TYPE MASTERSLICE ;");
		printWriter.println("END NDIFF");
		printWriter.println("");

		// write main cell header
		printWriter.println("MACRO " + cell.getName());
		printWriter.println("  FOREIGN " + cell.getName() + " ;");
		Rectangle2D bounds = cell.getBounds();
		double width = TextUtils.convertDistance(bounds.getWidth(), cell.getTechnology(), TextUtils.UnitScale.MICRO);
		double height = TextUtils.convertDistance(bounds.getHeight(), cell.getTechnology(), TextUtils.UnitScale.MICRO);
		printWriter.println("  SIZE " + TextUtils.formatDouble(width) + " BY " + TextUtils.formatDouble(height) + " ;");
		printWriter.println("  SITE " + cell.getName() + " ;");
		
		// write all of the metal geometry and ports
		nodesSeen = new HashSet<NodeInst>();
		arcsSeen = new HashSet<ArcInst>();
		boolean first = true;
		for(Iterator<PortProto> it = cell.getPorts(); it.hasNext(); )
		{
			Export e = (Export)it.next();
			if (first) first = false; else printWriter.println("");
			printWriter.println("  PIN " + e.getName());

			PortOriginal fp = new PortOriginal(e.getOriginalPort());
			NodeInst rni = fp.getBottomNodeInst();
			PrimitivePort rpp = fp.getBottomPortProto();
			AffineTransform trans = fp.getTransformToTop();
			printWriter.println("    PORT");
			io_lefoutcurlayer = null;
			Technology tech = rni.getProto().getTechnology();
			Poly [] polys = tech.getShapeOfNode(rni, null, null, true, false, null);
			if (polys.length == 0)
			{
				PrimitiveNode np = (PrimitiveNode)rni.getProto();
				Technology.NodeLayer [] nls = np.getLayers();
				if (nls.length > 0)
				{
					polys = new Poly[1];
					polys[0] = new Poly(rni.getAnchorCenterX(), rni.getAnchorCenterY(), rni.getXSize(), rni.getYSize());
					polys[0].setLayer(nls[0].getLayer().getNonPseudoLayer());
					polys[0].setPort(rpp);
				}
			}
			for(int i=0; i<polys.length; i++)
			{
				Poly poly = polys[i];
				if (poly.getPort() != rpp) continue;
				io_lefwritepoly(poly, trans, tech);
			}
			Network net = netList.getNetwork(e, 0);
			io_lefoutspread(cell, net, e.getOriginalPort().getNodeInst(), netList);
			printWriter.println("    END");
			if (e.getCharacteristic() == PortCharacteristic.PWR)
				printWriter.println("    USE POWER ;");
			if (e.getCharacteristic() == PortCharacteristic.GND)
				printWriter.println("    USE GROUND ;");
			printWriter.println("  END " + e.getName());
		}
	
		// write the obstructions (all of the metal)
		printWriter.println("");
		printWriter.println("  OBS");
		io_lefoutcurlayer = null;
	}

	private void term(Cell cell)
	{
	 	printWriter.println("  END");
	   	printWriter.println("");

		printWriter.println("END " + cell.getName());
		printWriter.println("");
		printWriter.println("END LIBRARY");
	}

	private void writeCellContents(HierarchyEnumerator.CellInfo info)
	{
		Cell cell = info.getCell();
		AffineTransform trans = info.getTransformToRoot();
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.isCellInstance()) continue;
			if (info.isRootCell() && nodesSeen.contains(ni)) continue;
			AffineTransform rot = ni.rotateOut(trans);
			Technology tech = ni.getProto().getTechnology();
			Poly [] polys = tech.getShapeOfNode(ni);
			for(int i=0; i<polys.length; i++)
			{
				Poly poly = polys[i];
				io_lefwritepoly(poly, rot, tech);
			}
		}
	
		// write metal layers for all arcs
		for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			if (info.isRootCell() && arcsSeen.contains(ai)) continue;
			Technology tech = ai.getProto().getTechnology();
			Poly [] polys = tech.getShapeOfArc(ai);
			for(int i=0; i<polys.length; i++)
			{
				Poly poly = polys[i];
				io_lefwritepoly(poly, trans, tech);
			}
		}
	}

	/**
	 * Method to write all geometry in cell "cell" that is on network "net"
	 * to file "out".  Does not write node "ignore".
	 */
	void io_lefoutspread(Cell cell, Network net, NodeInst ignore, Netlist netList)
	{
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.isCellInstance()) continue;
			if (ni == ignore) continue;
			PrimitiveNode.Function fun = ni.getFunction();
			if (fun != PrimitiveNode.Function.PIN && fun != PrimitiveNode.Function.CONTACT &&
				fun != PrimitiveNode.Function.NODE && fun != PrimitiveNode.Function.CONNECT) continue;
			boolean found = true;
			for(Iterator<PortInst> pIt = ni.getPortInsts(); pIt.hasNext(); )
			{
				PortInst pi = (PortInst)pIt.next();
				Network pNet = netList.getNetwork(pi);
				if (pNet != net) { found = false;   break; }
			}
			if (!found) continue;
	
			// write all layers on this node
			nodesSeen.add(ni);
			AffineTransform trans = ni.rotateOut();
			Technology tech = ni.getProto().getTechnology();
			Poly [] polys = tech.getShapeOfNode(ni);
			for(int i=0; i<polys.length; i++)
			{
				Poly poly = polys[i];
				io_lefwritepoly(poly, trans, tech);
			}
		}
	
		// write metal layers for all arcs
		for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			Network aNet = netList.getNetwork(ai, 0);
			if (aNet != net) continue;
			arcsSeen.add(ai);
			Technology tech = ai.getProto().getTechnology();
			Poly [] polys = tech.getShapeOfArc(ai);
			for(int i=0; i<polys.length; i++)
			{
				Poly poly = polys[i];
				io_lefwritepoly(poly, GenMath.MATID, tech);
			}
		}
	}

	/**
	 * Method to write polygon "poly" from technology "tech", transformed by "trans",
	 * to "out".
	 */
	private void io_lefwritepoly(Poly poly, AffineTransform trans, Technology tech)
	{
		Layer layer = poly.getLayer();
		if (layer == null) return;
		String layername = io_lefoutlayername(layer);
		if (layername.length() == 0) return;
		poly.transform(trans);
		Rectangle2D polyBounds = poly.getBox();
		if (polyBounds == null) return;
		double flx = TextUtils.convertDistance(polyBounds.getMinX(), tech, TextUtils.UnitScale.MICRO);
		double fly = TextUtils.convertDistance(polyBounds.getMinY(), tech, TextUtils.UnitScale.MICRO);
		double fhx = TextUtils.convertDistance(polyBounds.getMaxX(), tech, TextUtils.UnitScale.MICRO);
		double fhy = TextUtils.convertDistance(polyBounds.getMaxY(), tech, TextUtils.UnitScale.MICRO);
		if (layer != io_lefoutcurlayer)
		{
			printWriter.println("    LAYER " + layername + " ;");
			io_lefoutcurlayer = layer;
		}
		printWriter.println("    RECT " + TextUtils.formatDouble(flx) + " " + TextUtils.formatDouble(fly) + " " +
			TextUtils.formatDouble(fhx) + " " + TextUtils.formatDouble(fhy) + " ;");
	}
	
	private String io_lefoutlayername(Layer layer)
	{
		layer = layer.getNonPseudoLayer();
		Layer.Function fun = layer.getFunction();
		if (fun.isMetal()) return "METAL" + fun.getLevel();
		if (fun == Layer.Function.GATE) return "POLY1";
		if (fun.isPoly()) return "POLY" + fun.getLevel();
		if (fun == Layer.Function.CONTACT1) return "CONT";
		if (fun == Layer.Function.CONTACT2) return "VIA12";
		if (fun == Layer.Function.CONTACT3) return "VIA23";
		if (fun == Layer.Function.CONTACT4) return "VIA34";
		if (fun == Layer.Function.CONTACT5) return "VIA45";
		if (fun == Layer.Function.CONTACT6) return "VIA56";
		if (fun == Layer.Function.CONTACT7) return "VIA67";
		if (fun == Layer.Function.CONTACT8) return "VIA78";
		if (fun == Layer.Function.CONTACT9) return "VIA89";
		if (fun == Layer.Function.CONTACT10) return "VIA9";
		if (fun == Layer.Function.CONTACT11) return "VIA10";
		if (fun == Layer.Function.CONTACT12) return "VIA11";
		if (fun == Layer.Function.DIFFN) return "NDIFF";
		if (fun == Layer.Function.DIFFP) return "PDIFF";
		if (fun == Layer.Function.DIFF) return "DIFF";
		return "";
	}
}
