/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EDIF.java
 * Input/output tool: EDIF netlist generator
 * Original C Code written by Steven M. Rubin, B G West and Glen M. Lawson
 * Translated to Java by Steven M. Rubin, Sun Microsystems.
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
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.network.Global;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.DisplayedText;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.io.output.Topology.CellAggregateSignal;
import com.sun.electric.tool.io.output.Topology.CellNetInfo;
import com.sun.electric.tool.io.output.Topology.CellSignal;
import com.sun.electric.tool.user.User;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This is the netlister for EDIF.
 */
public class EDIF extends Topology
{
	/** true to add extra "ripping" cells where arcs and busses meet */	private static final boolean ADD_RIPPERS = true;

	private static class EGraphic
	{
		private String text;
		EGraphic(String text) { this.text = text; }
		String getText() { return text; }
	}
	private static final EGraphic EGUNKNOWN = new EGraphic("UNKNOWN");
	private static final EGraphic EGART = new EGraphic("ARTWORK");
//	private static final EGraphic EGTEXT = new EGraphic("TEXT");
	private static final EGraphic EGWIRE = new EGraphic("WIRE");
	private static final EGraphic EGBUS = new EGraphic("BUS");

	private EGraphic egraphic = EGUNKNOWN;
	private EGraphic egraphic_override = EGUNKNOWN;

	// for bus rippers
	private static class BusRipper
	{
		private NodeInst ni;
		private Network net;
		private int busWidth;
		private int busIndex;
		private int splitterIndex;
		private String busName;
		private static HashMap<Cell,List<BusRipper>> rippersPerCell = null;

		private BusRipper(NodeInst ni, Network net, int busWidth, int busIndex, int splitterIndex, String busName)
		{
			this.ni = ni;
			this.net = net;
			this.busWidth = busWidth;
			this.busIndex = busIndex;
			this.splitterIndex = splitterIndex;
			this.busName = busName;
		}

		public int getBusWidth() { return busWidth; }

		public int getBusIndex() { return busIndex; }

		public int getSplitterIndex() { return splitterIndex; }
		
		public static void makeBusRipper(NodeInst ni, Network net, int busWidth, int busIndex, int splitterIndex, String busName)
		{
			BusRipper br = new BusRipper(ni, net, busWidth, busIndex, splitterIndex, busName);

			// add to lists
			Cell cell = ni.getParent();
			if (rippersPerCell == null) rippersPerCell = new HashMap<Cell,List<BusRipper>>();
			List<BusRipper> rippersInCell = rippersPerCell.get(cell);
			if (rippersInCell == null)
			{
				rippersInCell = new ArrayList<BusRipper>();
				rippersPerCell.put(cell, rippersInCell);
			}
			rippersInCell.add(br);
		}

		public static BusRipper findBusRipper(NodeInst ni, Network net)
		{
			if (rippersPerCell == null) return null;
			List<BusRipper> rippersInCell = rippersPerCell.get(ni.getParent());
			if (rippersInCell == null) return null;
			for(BusRipper br : rippersInCell)
			{
				if (br.ni == ni && br.net == net) return br;
			}
			return null;
		}

		public static List<BusRipper> getRippersOnBus(Cell cell, String busName)
		{
			List<BusRipper> ripperList = new ArrayList<BusRipper>();
			if (rippersPerCell == null) return ripperList;
			List<BusRipper> rippersInCell = rippersPerCell.get(cell);
			if (rippersInCell == null) return ripperList;
			for(BusRipper br : rippersInCell)
			{
				if (br.busName.equals(busName)) ripperList.add(br);
			}
			return ripperList;
		}

		public static void done()
		{
			rippersPerCell = null;
		}
	}

	// settings that may later be changed by preferences
    private static final String primitivesLibName = "ELECTRIC_PRIMS";

    private int scale = 20;
    EDIFEquiv equivs;
    private final HashMap<Library,LibToWrite> libsToWrite; // key is Library, Value is LibToWrite
    private final List<Library> libsToWriteOrder; // list of libraries to write, in order

    private static class LibToWrite {
        private final Library lib;
        private final List<CellToWrite> cellsToWrite;
        private LibToWrite(Library l) {
            lib = l;
            cellsToWrite = new ArrayList<CellToWrite>();
        }
        private void add(CellToWrite c) { cellsToWrite.add(c); }
        private Iterator<CellToWrite> getCells() { return cellsToWrite.iterator(); }
    }

    private static class CellToWrite {
        private final Cell cell;
        private final CellNetInfo cni;
        private final VarContext context;
        private CellToWrite(Cell cell, CellNetInfo cni, VarContext context) {
            this.cell = cell;
            this.cni = cni;
            this.context = context;
        }
    }

	/**
	 * The main entry point for EDIF deck writing.
     * @param cell the top-level cell to write.
     * @param context the hierarchical context to the cell.
	 * @param filePath the disk file to create.
	 */
	public static void writeEDIFFile(Cell cell, VarContext context, String filePath)
	{
		EDIF out = new EDIF();
		if (out.openTextOutputStream(filePath)) return;
		if (out.writeCell(cell, context)) return;
		if (out.closeTextOutputStream()) return;
		System.out.println(filePath + " written");
	}

	/**
	 * Creates a new instance of the EDIF netlister.
	 */
	EDIF()
	{
        libsToWrite = new HashMap<Library,LibToWrite>();
        libsToWriteOrder = new ArrayList<Library>();
        equivs = new EDIFEquiv();
	}

	protected void start()
	{
        // find the edit window
		String name = makeToken(topCell.getName());

		// If this is a layout representation, then create the footprint
		if (topCell.getView() == View.LAYOUT)
		{
			// default routing grid is 6.6u = 660 centimicrons
			int rgrid = 660;

			// calculate the actual routing grid in microns
			double route = rgrid / 100;

//			String iname = name + ".foot";
//			io_fileout = xcreate(iname, io_filetypeedif, "EDIF File", &truename);
//			if (io_fileout == NULL)
//			{
//				if (truename != 0) ttyputerr(_("Cannot write %s"), iname);
//				return(TRUE);
//			}

			// write the header
			System.out.println("Writing footprint for cell " + makeToken(topCell.getName()));
			printWriter.println("(footprint " + TextUtils.formatDouble(route) + "e-06");
			printWriter.println(" (unknownLayoutRep");

			// get standard cell dimensions
			Rectangle2D cellBounds = topCell.getBounds();
			double width = cellBounds.getWidth() / rgrid;
			double height = cellBounds.getHeight() / rgrid;
			printWriter.println("  (" + makeToken(topCell.getName()) + " standard (" +
				TextUtils.formatDouble(height) + " " + TextUtils.formatDouble(width) + ")");
			printWriter.println(")))");
			return;
		}

		// write the header
		String header = "Electric VLSI Design System";
		if (User.isIncludeDateAndVersionInOutput())
		{
			header += ", version " + Version.getVersion();
		}
		writeHeader(topCell, header, "EDIF Writer", topCell.getLibrary().getName());

        // write out all primitives used in the library
        blockOpen("library");
        blockPutIdentifier(primitivesLibName);
        blockPut("edifLevel", "0");
        blockOpen("technology");
        blockOpen("numberDefinition");
        if (IOTool.isEDIFUseSchematicView())
        {
            writeScale(Technology.getCurrent());
        }
        blockClose("numberDefinition");
        if (IOTool.isEDIFUseSchematicView())
        {
            writeFigureGroup(EGART);
            writeFigureGroup(EGWIRE);
            writeFigureGroup(EGBUS);
        }
        blockClose("technology");
        HashMap<Object,PrimitiveNode> primsFound = new HashMap<Object,PrimitiveNode>();
        writeAllPrims(topCell, primsFound);
        blockClose("library");

		// TODO (DONE) initialize rippers library
		if (ADD_RIPPERS)
		{
			// figure out how many bus rippers are needed
			HashSet<Integer> rippers = new HashSet<Integer>();
			countRippers(topCell, rippers);
			if (rippers.size() > 0)
			{
		        blockOpen("library");
		        blockPutIdentifier("cdsRipLib");
		        blockPut("edifLevel", "0");
		        blockOpen("technology");
			        blockOpen("numberDefinition");
				        if (IOTool.isEDIFUseSchematicView())
				            writeScale(Technology.getCurrent());
			        blockClose("numberDefinition");
		        blockClose("technology");
			}
			for(Integer width : rippers)
			{
		        blockOpen("cell");
				blockPutIdentifier("ripper_" + width.intValue());
				blockPut("cellType", "RIPPER");
				blockOpen("view");
				blockPutIdentifier("symbol");
				blockPut("viewType", IOTool.isEDIFUseSchematicView() ? "SCHEMATIC" : "NETLIST");
		        blockOpen("interface");

				blockOpen("port");
					blockOpen("array");
						blockPutIdentifier("dst_0");
						blockPutIdentifier(width.toString());
					blockClose("array");
				blockClose("port");

				blockOpen("port");
					blockOpen("array");
						blockPutIdentifier("src");
						blockPutIdentifier(width.toString());
					blockClose("array");
				blockClose("port");

				blockOpen("joined");
					blockPut("portRef", "dst_0");
					blockPut("portRef", "src");
				blockClose("joined");

				blockOpen("symbol");
					blockOpen("figure");
						blockPutIdentifier("wire");
						blockOpen("circle");
							blockOpen("pt");
								blockPutIdentifier("-5");
								blockPutIdentifier("0");
							blockClose("pt");
							blockOpen("pt");
								blockPutIdentifier("5");
								blockPutIdentifier("0");
							blockClose("pt");
						blockClose("circle");
					blockClose("figure");

					blockOpen("portImplementation");
						blockPutIdentifier("dst_0");
						blockOpen("connectLocation");
							blockOpen("figure");
								blockPutIdentifier("pin");
								blockOpen("dot");
									writePoint(0, 0);
								blockClose("dot");
							blockClose("figure");
						blockClose("connectLocation");
					blockClose("portImplementation");

					blockOpen("portImplementation");
						blockPutIdentifier("src");
						blockOpen("connectLocation");
							blockOpen("figure");
								blockPutIdentifier("pin");
								blockOpen("dot");
									writePoint(0, 0);
								blockClose("dot");
							blockClose("figure");
						blockClose("connectLocation");
					blockClose("portImplementation");

				blockClose("symbol");
				blockClose("interface");
				blockClose("view");
				blockClose("cell");
			}
			if (rippers.size() > 0)
			{
				blockClose("library");
			}
		}

        // external libs
        // organize by library
        List<String> libs = new ArrayList<String>();
        for (EDIFEquiv.NodeEquivalence e : equivs.getNodeEquivs()) {
            if (libs.contains(e.externalLib)) continue;
            libs.add(e.externalLib);
        }
        for (String lib : libs) {
            blockOpen("external");
            blockPutIdentifier(lib);
            blockPut("edifLevel", "0");
            blockOpen("technology");
            blockOpen("numberDefinition");
            if (IOTool.isEDIFUseSchematicView())
            {
                writeScale(Technology.getCurrent());
            }
            blockClose("technology");

            for (EDIFEquiv.NodeEquivalence e : equivs.getNodeEquivs()) {
                if (!lib.equals(e.externalLib)) continue;
                String viewType = null;
                if (e.exortedType != null) viewType = "GRAPHIC"; // pins must have GRAPHIC view
                writeExternalDef(e.externalCell, e.externalView, viewType, e.getExtPorts());
            }
            blockClose("external");
        }
	}

    /**
     * Build up lists of cells that need to be written, organized by library
     */
    protected void writeCellTopology(Cell cell, CellNetInfo cni, VarContext context, Topology.MyCellInfo info)
    {
        Library lib = cell.getLibrary();
        LibToWrite l = libsToWrite.get(lib);
        if (l == null) {
            l = new LibToWrite(lib);
            libsToWrite.put(lib, l);
            libsToWriteOrder.add(lib);
        }
        l.add(new CellToWrite(cell, cni, context));
    }

    protected void done() {

        // Note: if there are cross dependencies between libraries, there is no
        // way to write out valid EDIF without changing the cell organization of the libraries
        for (Library lib : libsToWriteOrder) {
            LibToWrite l = libsToWrite.get(lib);
            // here is where we write everything out, organized by library
            // write library header
            blockOpen("library");
            blockPutIdentifier(makeToken(lib.getName()));
            blockPut("edifLevel", "0");
            blockOpen("technology");
            blockOpen("numberDefinition", false);
            if (IOTool.isEDIFUseSchematicView())
            {
                writeScale(Technology.getCurrent());
            }
            blockClose("numberDefinition");
            if (IOTool.isEDIFUseSchematicView())
            {
                writeFigureGroup(EGART);
                writeFigureGroup(EGWIRE);
                writeFigureGroup(EGBUS);
            }
            blockClose("technology");
            for (Iterator<CellToWrite> it2 = l.getCells(); it2.hasNext(); ) {
                CellToWrite c = it2.next();
                writeCellEdif(c.cell, c.cni, c.context);
            }
            blockClose("library");
        }

        // post-identify the design and library
        blockOpen("design");
        blockPutIdentifier(makeToken(topCell.getName()));
        blockOpen("cellRef");
        blockPutIdentifier(makeToken(topCell.getName()));
        blockPut("libraryRef", makeToken(topCell.getLibrary().getName()));

        // clean up
        blockFinish();
	}

    /**
     * Method to write cellGeom
     */
    private void writeCellEdif(Cell cell, CellNetInfo cni, VarContext context)
	{
		// write out the cell header information
		blockOpen("cell");
		blockPutIdentifier(makeToken(cell.getName()));
		blockPut("cellType", "generic");
		blockOpen("view");
		blockPutIdentifier("symbol");
		blockPut("viewType", IOTool.isEDIFUseSchematicView() ? "SCHEMATIC" : "NETLIST");

		// write out the interface description
		blockOpen("interface");

		// write ports and directions
		Netlist netList = cni.getNetList();
		HashMap<Export,String> busExports = new HashMap<Export,String>();
		for(Iterator<CellSignal> it = cni.getCellSignals(); it.hasNext(); )
		{
			CellSignal cs = it.next();
			if (cs.isExported())
			{
				Export e = cs.getExport();
				String direction = "INPUT";
				if (e.getCharacteristic() == PortCharacteristic.OUT ||
					e.getCharacteristic() == PortCharacteristic.REFOUT) direction = "OUTPUT";
				if (e.getCharacteristic() == PortCharacteristic.BIDIR) direction = "INOUT";
				int busWidth = netList.getBusWidth(e);
				if (busWidth > 1)
				{
					// only write bus exports once
					if (busExports.get(e) != null) continue;
					blockOpen("port");
					blockOpen("array");
					String eBusName = convertBusName(e.getName(), netList, e);
					String busName = makeToken(eBusName);
					busExports.put(e, busName);
					blockOpen("rename");
					blockPutIdentifier(busName);
					blockPutString(eBusName);
					blockClose("rename");
					blockPutIdentifier(Integer.toString(busWidth));
					blockClose("array");
					blockPut("direction", direction);
					blockClose("port");
				} else
				{
					blockOpen("port");
					blockPutIdentifier(makeToken(cs.getName()));
					blockPut("direction", direction);
					blockClose("port");
				}
			}
		}
		if (IOTool.isEDIFUseSchematicView())
		{
            for (Iterator<Variable> it = cell.getVariables(); it.hasNext(); ) {
                Variable var = it.next();
                if (var.getTrueName().equals("prototype_center")) continue;
                blockOpen("property");
                String name = var.getTrueName();
                String name2 = makeValidName(name);
                if (!name.equals(name2)) {
                    blockOpen("rename", false);
                    blockPutIdentifier(name2);
                    blockPutString(name);
                    blockClose("rename");
                } else {
                    blockPutIdentifier(name);
                }
                blockOpen("string", false);
                String value = var.getObject().toString();
                value = value.replaceAll("\"", "%34%");
                blockPutString(value);
                blockClose("string");
                if (!var.isAttribute()) {
                    blockOpen("owner", false);
                    blockPutString("Electric");
                }
                blockClose("property");
            }

			// output the icon
            //writeIcon(cell);
			writeSymbol(cell);
		}

		blockClose("interface");

		// write cell contents
        blockOpen("contents");
        if (IOTool.isEDIFUseSchematicView()) {
            blockOpen("page");
            blockPutIdentifier("SH1");
        }

		// TODO (DONE) add ripper instances
		if (ADD_RIPPERS)
		{
			int splitterIndex = 1;
			for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = it.next();
				NodeProto np = ni.getProto();
				if (ni.isCellInstance()) continue;
                if (equivs.getNodeEquivalence(ni) != null) continue;        // will be defined by external reference
                PrimitiveNode pn = (PrimitiveNode)np;
				PrimitiveNode.Function fun = ni.getFunction();
				if (fun != PrimitiveNode.Function.PIN) continue;

				// check all the connections
				ArcInst busFound = null;
				for(Iterator<Connection> cIt = ni.getConnections(); cIt.hasNext(); )
				{
					Connection con = cIt.next();
					ArcInst ai = con.getArc();
					int width = netList.getBusWidth(ai);
					if (width > 1) busFound = ai;
				}
				if (busFound == null) continue;
				int busWidth = netList.getBusWidth(busFound);

				// a bus pin: look for wires that indicate ripping
				for(Iterator<Connection> cIt = ni.getConnections(); cIt.hasNext(); )
				{
					Connection con = cIt.next();
					ArcInst ai = con.getArc();
					int width = netList.getBusWidth(ai);
					if (width < 2)
					{
						// add an instance of a ripper
						Network net = netList.getNetwork(ai, 0);
						int busIndex = 0;
						for(int i=0; i<busWidth; i++)
						{
							Network busNet = netList.getNetwork(busFound, i);
							if (busNet == net)
							{
								busIndex = i;
								break;
							}
						}
						BusRipper.makeBusRipper(ni, net, busWidth, busIndex, splitterIndex,
							netList.getBusName(busFound).toString());
			            blockOpen("instance");
							String splitterName = "splitter_" + splitterIndex;
							splitterIndex++;
				            blockPutIdentifier(splitterName);
				            blockOpen("viewRef");
				            	blockPutIdentifier("symbol");
					            blockOpen("cellRef");
				            		blockPutIdentifier("ripper_" + busWidth);
									blockPut("libraryRef", "cdsRipLib");
								blockClose("cellRef");
							blockClose("viewRef");
							blockOpen("transform");
								blockOpen("origin");
									writePoint(ni.getAnchorCenterX(), ni.getAnchorCenterY());
								blockClose("origin");
							blockClose("transform");
			            blockClose("instance");
					}
				}
			}
		}

		for(Iterator<NodeInst> nIt = cell.getNodes(); nIt.hasNext(); )
		{
			NodeInst no = nIt.next();
            if (no.isCellInstance()) {
                Cell c = (Cell)no.getProto();
                if (cell.iconView() == c) continue;         // can't make instance of icon view
            }

			if (!no.isCellInstance())
			{
				PrimitiveNode.Function fun = ((NodeInst)no).getFunction();
                Variable var = no.getVar(Artwork.ART_MESSAGE);
                if (var != null) {
                    // this is cell annotation text
                    blockOpen("commentGraphics");
                    blockOpen("annotate", false);
                    Point2D point = new Point2D.Double(no.getAnchorCenterX(), no.getAnchorCenterY());
                    Poly p = new Poly(new Point2D [] { point });
                    String s;
                    if (var.getObject() instanceof String []) {
                        String [] lines = (String [])var.getObject();
                        StringBuffer sbuf = new StringBuffer();
                        for (int i=0; i<lines.length; i++) {
                            sbuf.append(lines[i]);
                            if (i != lines.length-1) sbuf.append("%10%");    // newline separator in Cadence
                        }
                        s = sbuf.toString();
                    } else {
                        s = var.getObject().toString();
                    }
                    p.setString(s);
                    p.setTextDescriptor(var.getTextDescriptor());
                    p.setStyle(var.getTextDescriptor().getPos().getPolyType());
                    writeSymbolPoly(p, null, 1);
                    blockClose("commentGraphics");
                    continue;
                }
				if (fun == PrimitiveNode.Function.UNKNOWN || fun == PrimitiveNode.Function.PIN ||
					fun == PrimitiveNode.Function.CONTACT || fun == PrimitiveNode.Function.NODE ||
					//fun == PrimitiveNode.Function.CONNECT ||
                        fun == PrimitiveNode.Function.ART) continue;
			}

			String iname = makeComponentName(no);
			String oname = no.getName();

            // write reference - get lib, cell, view
            String refLib = "?", refCell="?", refView = "symbol";
            int addedRotation = 0;
            boolean openedPortImplementation = false;

            EDIFEquiv.NodeEquivalence ne = equivs.getNodeEquivalence(no);
            if (ne != null) {
                addedRotation = ne.rotation * 10;
                refLib = ne.externalLib;
                refCell = ne.externalCell;
                if (ne.exortedType != null) {
                    // cadence pin: encapsulate instance inside of a portImplementation
                    Iterator<Export> eit = no.getExports();
                    if (eit.hasNext()) {
                        Export e = eit.next();
                        oname = e.getName();
                        writePortImplementation(e, false);
                        openedPortImplementation = true;
                    }
                }
            } else if (!no.isCellInstance())
			{
				NodeInst ni = (NodeInst)no;
				PrimitiveNode.Function fun = ni.getFunction();

                // do default action for primitives
                refLib = primitivesLibName;
                if (fun == PrimitiveNode.Function.GATEAND || fun == PrimitiveNode.Function.GATEOR || fun == PrimitiveNode.Function.GATEXOR) {					// count the number of inputs
                    int i = 0;
                    for(Iterator<Connection> pIt = ni.getConnections(); pIt.hasNext(); )
                    {
                        Connection con = pIt.next();
                        if (con.getPortInst().getPortProto().getName().equals("a")) i++;
                    }
                    refCell = makeToken(ni.getProto().getName()) + i;
                } else {
                    refCell = describePrimitive(ni, fun);
                }
			} else
			{
                // this is a cell
                Cell np = (Cell)no.getProto();
                refLib = np.getLibrary().getName();
                refCell = makeToken(no.getProto().getName());
			}

            // write reference
            blockOpen("instance");
			if (!oname.equalsIgnoreCase(iname))
			{
				blockOpen("rename", false);
				blockPutIdentifier(iname);
				blockPutString(oname);
				blockClose("rename");
			} else blockPutIdentifier(iname);
            blockOpen("viewRef");
            blockPutIdentifier(refView);
            blockOpen("cellRef", false);
            blockPutIdentifier(refCell);
            blockPut("libraryRef", refLib);
            blockClose("viewRef");

			// now graphical information
			if (IOTool.isEDIFUseSchematicView())
			{
                NodeInst ni = (NodeInst)no;
                blockOpen("transform");

                // get the orientation (note only support orthogonal)
                blockPut("orientation", getOrientation(ni, addedRotation));

                // now the origin
                blockOpen("origin");
                double cX = ni.getAnchorCenterX(), cY = ni.getAnchorCenterY();
/*
                if (no.isCellInstance())
                {
                    Rectangle2D cellBounds = ((Cell)no.getProto()).getBounds();
                    cX = ni.getTrueCenterX() - cellBounds.getCenterX();
                    cY = ni.getTrueCenterY() - cellBounds.getCenterY();
                }
*/
                Point2D pt = new Point2D.Double(cX, cY);
/*
                AffineTransform trans = ni.rotateOut();
                trans.transform(pt, pt);
*/
                writePoint(pt.getX(), pt.getY());
                blockClose("transform");
			}

			// check for variables to write as properties
			if (IOTool.isEDIFUseSchematicView())
			{
				// do all display variables first
                NodeInst ni = (NodeInst)no;
                int num = ni.numDisplayableVariables(false);
                Poly [] varPolys = new Poly[num];
                ni.addDisplayableVariables(ni.getBounds(), varPolys, 0, null, false);
                writeDisplayableVariables(varPolys, ni.rotateOut());
			}
			blockClose("instance");
            if (openedPortImplementation) {
                blockClose("portImplementation");
            }
		}

		// if there is anything to connect, write the networks in the cell
		for(Iterator<CellSignal> it = cni.getCellSignals(); it.hasNext(); )
		{
			CellSignal cs = it.next();

			// ignore unconnected (unnamed) nets
			String netName = cs.getNetwork().describe(false);
			if (netName.length() == 0) continue;

			// establish if this is a global net
			boolean globalport = false;
//			if ((pp = (PORTPROTO *) net->temp2) != NOPORTPROTO)
//				globalport = isGlobalExport(pp);

			blockOpen("net");
			netName = cs.getName();
			String eName = makeToken(netName);
			if (globalport)
			{
				blockOpen("rename");
				blockPutIdentifier(eName);
				blockPutString(eName + "!");
				blockClose("rename");
				blockPut("property", "GLOBAL");
			} else
			{
				EDIFEquiv.GlobalEquivalence ge = equivs.getElectricGlobalEquivalence(netName);
				if (ge != null) netName = ge.externGName;
				if (!eName.equals(netName))
				{
					// different names
					blockOpen("rename");
					blockPutIdentifier(eName);
					blockPutString(netName);
					blockClose("rename");
				} else blockPutIdentifier(eName);
			}

			// write net connections
			blockOpen("joined");

			// include exported ports
			if (cs.isExported())
			{
				Export e = cs.getExport();
				if (netList.getBusWidth(e) <= 1)
				{
					String pt = e.getName();
					blockPut("portRef", makeToken(pt));
				}
			}

			Network net = cs.getNetwork();
			for(Iterator<Nodable> nIt = netList.getNodables(); nIt.hasNext(); )
			{
				Nodable no = nIt.next();
				NodeProto niProto = no.getProto();

				// TODO (DONE) mention connectivity to a bus ripper
				if (no instanceof NodeInst)
				{
					BusRipper br = BusRipper.findBusRipper((NodeInst)no, net);
					if (br != null)
					{
						blockOpen("portRef");
							blockOpen("member");
								blockPutIdentifier("dst_0");
								blockPutIdentifier(Integer.toString(br.getBusIndex()));
							blockClose("member");
							blockOpen("instanceRef");
								blockPutIdentifier("splitter_" + br.getSplitterIndex());
							blockClose("instanceRef");
						blockClose("portRef");
					}
				}

				EDIFEquiv.NodeEquivalence ne = equivs.getNodeEquivalence(no.getNodeInst());
				if (niProto instanceof Cell)
				{
					String nodeName = parameterizedName(no, context);
					CellNetInfo subCni = getCellNetInfo(nodeName);
					for(Iterator<CellSignal> sIt = subCni.getCellSignals(); sIt.hasNext(); )
					{
						CellSignal subCs = sIt.next();

						// ignore networks that aren't exported
						PortProto subPp = subCs.getExport();
						if (subPp == null) continue;

						// single signal
						Network subNet = netList.getNetwork(no, subPp, subCs.getExportIndex());
						if (cs != cni.getCellSignal(subNet)) continue;
                        String portName = subCs.getName();
                        if (ne != null) {
                            // get equivalent port name
                            EDIFEquiv.PortEquivalence pe = ne.getPortEquivElec(portName);
                            if (pe == null) {
                                System.out.println("Error: no equivalent port found for '"+portName+"' on node "+niProto.describe(false));
                                System.out.println("     Equivalence class: ");
                                System.out.println(ne.toString());
                            } else {
                                if (!pe.getExtPort().ignorePort) {
                                    blockOpen("portRef");
                                    portName = pe.getExtPort().name;
                                    blockPutIdentifier(makeToken(portName));
                                    blockPut("instanceRef", makeComponentName(no));
                                    blockClose("portRef");
                                }
                            }
                        } else {
                            blockOpen("portRef");
                            blockPutIdentifier(makeToken(portName));
                            blockPut("instanceRef", makeComponentName(no));
                            blockClose("portRef");
                        }
					}
				} else
				{
					NodeInst ni = (NodeInst)no;
					PrimitiveNode.Function fun = ni.getFunction();
					if (fun == PrimitiveNode.Function.UNKNOWN || fun == PrimitiveNode.Function.PIN ||
						fun == PrimitiveNode.Function.CONTACT || fun == PrimitiveNode.Function.NODE ||
						//fun == PrimitiveNode.Function.CONNECT ||
                            fun == PrimitiveNode.Function.ART) continue;
					for(Iterator<Connection> cIt = ni.getConnections(); cIt.hasNext(); )
					{
						Connection con = cIt.next();
						ArcInst ai = con.getArc();
						Network aNet = netList.getNetwork(ai, 0);
						if (aNet != net) continue;
						String portName = con.getPortInst().getPortProto().getName();
                        if (ne != null) {
                            // get equivalent port name
                            EDIFEquiv.PortEquivalence pe = ne.getPortEquivElec(portName);
                            if (pe == null) {
                                System.out.println("Error: no equivalent port found for '"+portName+"' on node "+niProto.describe(false));
                                System.out.println("     Equivalence class: ");
                                System.out.println(ne.toString());
                            } else {
                                if (!pe.getExtPort().ignorePort) {
                                    blockOpen("portRef");
                                    portName = pe.getExtPort().name;
                                    String safeName = makeValidName(portName);
                                    if (!safeName.equals(portName)) {
                                        blockPutIdentifier(makeToken(safeName));
                                    } else {
                                        blockPutIdentifier(makeToken(portName));
                                    }
                                    blockPut("instanceRef", makeComponentName(no));
                                    blockClose("portRef");
                                }
                            }
                        } else {
                            blockOpen("portRef");
                            blockPutIdentifier(makeToken(portName));
                            blockPut("instanceRef", makeComponentName(no));
                            blockClose("portRef");
                        }
					}
				}
			}
			blockClose("joined");

			if (IOTool.isEDIFUseSchematicView())
			{
				// output net graphic information for all arc instances connected to this net
				egraphic = EGUNKNOWN;
				egraphic_override = EGWIRE;
				for(Iterator<ArcInst> aIt = cell.getArcs(); aIt.hasNext(); )
				{
					ArcInst ai = aIt.next();
					int aWidth = netList.getBusWidth(ai);
					if (aWidth > 1) continue;
					Network aNet = netList.getNetwork(ai, 0);
					if (aNet == net) writeSymbolArcInst(ai, GenMath.MATID);
				}
				setGraphic(EGUNKNOWN);
				egraphic_override = EGUNKNOWN;
			}

            if (cs.isExported()) {
                Export e = cs.getExport();
                blockOpen("comment");
                blockPutString("exported as "+e.getName()+", type "+e.getCharacteristic().getName());
                blockClose("comment");
            }

			if (globalport)
				blockPut("userData", "global");
			blockClose("net");
		}

		// write busses
		if (ADD_RIPPERS)
		{
			// TODO (DONE) the new way
			HashSet<ArcInst> bussesSeen = new HashSet<ArcInst>();
			for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
			{
				ArcInst ai = it.next();
				int busWidth = netList.getBusWidth(ai);
				if (busWidth < 2) continue;
				if (bussesSeen.contains(ai)) continue;

				blockOpen("net");
				blockOpen("array");
					String realBusName = netList.getBusName(ai).toString();
					String busName = convertBusName(realBusName, netList, ai);
					String oname = makeToken(busName);
					if (!oname.equals(busName))
					{
						// different names
						blockOpen("rename");
							blockPutIdentifier(oname);
							blockPutString(busName);
						blockClose("rename");
					} else blockPutIdentifier(oname);
					blockPutIdentifier(Integer.toString(busWidth));
				blockClose("array");
	
				// now each sub-net name
				blockOpen("joined");
				List<BusRipper> rippersOnBus = BusRipper.getRippersOnBus(cell, realBusName);
				for(BusRipper br : rippersOnBus)
				{
					blockOpen("portList");
					for(int i=0; i<busWidth; i++)
					{
						blockOpen("portRef");
							blockOpen("member");
								blockPutIdentifier("src");
								blockPutIdentifier(Integer.toString(i));
							blockClose("member");
							blockOpen("instanceRef");
								blockPutIdentifier("splitter_" + br.splitterIndex);
							blockClose("instanceRef");
						blockClose("portRef");
					}
					blockClose("portList");
				}
				blockClose("joined");
	
				// now graphics for the bus
				if (IOTool.isEDIFUseSchematicView())
				{
					// output net graphic information for all arc instances connected to this net
					egraphic = EGUNKNOWN;
					egraphic_override = EGBUS;
					for(Iterator<ArcInst> aIt = cell.getArcs(); aIt.hasNext(); )
					{
						ArcInst oAi = aIt.next();
						if (oAi.getProto() != Schematics.tech.bus_arc) continue;
						String arcBusName = netList.getBusName(oAi).toString();
						if (arcBusName.equals(realBusName))
						{
							writeSymbolArcInst(oAi, GenMath.MATID);
							bussesSeen.add(oAi);
						}
					}
					setGraphic(EGUNKNOWN);
					egraphic_override = EGUNKNOWN;
				}
				blockClose("net");
			}
		} else
		{
			// the old way: no longer done
			for(Iterator<CellAggregateSignal> it = cni.getCellAggregateSignals(); it.hasNext(); )
			{
				CellAggregateSignal cas = it.next();
	
				// ignore single signals
				if (cas.getLowIndex() > cas.getHighIndex()) continue;
	
				blockOpen("netBundle");
				String busName = cas.getNameWithIndices();
				String oname = makeToken(busName);
				if (!oname.equals(busName))
				{
					// different names
					blockOpen("rename");
					blockPutIdentifier(oname);
					blockPutString(busName);
					blockClose("rename");
				} else blockPutIdentifier(oname);
				blockOpen("listOfNets");
	
				// now each sub-net name
				int numSignals = cas.getHighIndex() - cas.getLowIndex() + 1;
				for (int k=0; k<numSignals; k++)
				{
					blockOpen("net");
	
					// now output this name
					CellSignal cs = cas.getSignal(k);
					String pt = cs.getName();
					oname = makeToken(pt);
					if (!oname.equals(pt))
					{
						// different names
						blockOpen("rename");
						blockPutIdentifier(oname);
						blockPutString(pt);
						blockClose("rename");
					} else blockPutIdentifier(oname);
					blockClose("net");
				}
	
				// now graphics for the bus
				if (IOTool.isEDIFUseSchematicView())
				{
					// output net graphic information for all arc instances connected to this net
					egraphic = EGUNKNOWN;
					egraphic_override = EGBUS;
					for(Iterator<ArcInst> aIt = cell.getArcs(); aIt.hasNext(); )
					{
						ArcInst ai = aIt.next();
						if (ai.getProto() != Schematics.tech.bus_arc) continue;
						String arcBusName = netList.getBusName(ai).toString();
						if (arcBusName.equals(busName)) writeSymbolArcInst(ai, GenMath.MATID);
					}
					setGraphic(EGUNKNOWN);
					egraphic_override = EGUNKNOWN;
				}
	
				blockClose("netBundle");
				continue;
			}
		}

        // write text
        Poly [] text = cell.getAllText(true, null);
        if (text != null) {
            for (int i=0; i<text.length; i++) {
                Poly p = text[i];
            }
        }

        if (IOTool.isEDIFUseSchematicView()) {
            blockClose("page");
        }
        blockClose("contents");

		// matches "(cell "
		blockClose("cell");
	}

	/****************************** MIDDLE-LEVEL HELPER METHODS ******************************/

	private void writeHeader(Cell cell, String program, String comment, String origin)
	{
		// output the standard EDIF 2 0 0 header
		blockOpen("edif");
		blockPutIdentifier(cell.getName());
		blockPut("edifVersion", "2 0 0");
		blockPut("edifLevel", "0");
		blockOpen("keywordMap");
		blockPut("keywordLevel", "0");		// was "1"
		blockClose("keywordMap");
		blockOpen("status");
		blockOpen("written");
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy MM dd HH mm ss");
		blockPut("timeStamp", simpleDateFormat.format(new Date()));
		if (program != null) blockPut("program", "\"" + program + "\"");
		if (comment != null) blockPut("comment", "\"" + comment + "\"");
		if (origin != null) blockPut("dataOrigin", "\"" + origin + "\"");
		blockClose("status");
	}

	/**
	 * Method to dump the description of primitive "np" to the EDIF file
	 * If the primitive is a schematic gate, use "i" as the number of inputs
	 */
	private void writePrimitive(PrimitiveNode pn, int i, PrimitiveNode.Function fun)
	{
		// write primitive name
		if (fun == PrimitiveNode.Function.GATEAND || fun == PrimitiveNode.Function.GATEOR || fun == PrimitiveNode.Function.GATEXOR)
		{
			blockOpen("cell");
			String name = makeToken(pn.getName()) + i;
			blockPutIdentifier(name);
		} else
		{
			blockOpen("cell");
            blockPutIdentifier(makeToken(pn.getName()));
		}

		// write primitive connections
		blockPut("cellType", "generic");
        blockOpen("comment");
        Technology tech = pn.getTechnology();
        blockPutString("Tech: "+tech.getTechName()+", Node: "+pn.getName()+", Func: "+fun.getConstantName());
        blockClose("comment");
		blockOpen("view");
		blockPutIdentifier("symbol");
		blockPut("viewType", IOTool.isEDIFUseSchematicView() ? "SCHEMATIC" : "NETLIST");
		blockOpen("interface");

		int firstPortIndex = 0;
		if (fun == PrimitiveNode.Function.GATEAND || fun == PrimitiveNode.Function.GATEOR || fun == PrimitiveNode.Function.GATEXOR)
		{
			for (int j = 0; j < i; j++)
			{
				blockOpen("port");
				String name = "IN" + (j + 1);
				blockPutIdentifier(name);
				blockPut("direction", "INPUT");
				blockClose("port");
			}
			firstPortIndex = 1;
		}
		for(int k=firstPortIndex; k<pn.getNumPorts(); k++)
		{
			PortProto pp = pn.getPort(k);
			String direction = "input";
			if (pp.getCharacteristic() == PortCharacteristic.OUT) direction = "output"; else
				if (pp.getCharacteristic() == PortCharacteristic.BIDIR) direction = "inout";
			blockOpen("port");
			blockPutIdentifier(makeToken(pp.getName()));
			blockPut("direction", direction);
			blockClose("port");
		}

        NodeInst ni = NodeInst.makeDummyInstance(pn);
        writeSymbol(pn, ni);
		blockClose("cell");
	}

    // ports is a list of EDIFEquiv.Port objects
    private void writeExternalDef(String extCell, String extView, String viewType, List<EDIFEquiv.Port> ports) {
        blockOpen("cell");
        blockPutIdentifier(extCell);
        blockPut("cellType", "generic");
        blockOpen("view");
        blockPutIdentifier(extView);
        if (viewType == null) viewType = IOTool.isEDIFUseSchematicView() ? "SCHEMATIC" : "NETLIST";
        blockPut("viewType", viewType);

        // write interface
        blockOpen("interface");
        for (EDIFEquiv.Port port : ports) {
            if (port.ignorePort) continue;
            blockOpen("port");
            String safeName = makeValidName(port.name);
            if (!safeName.equals(port.name)) {
                blockOpen("rename");
                blockPutIdentifier(safeName);
                blockPutString(port.name);
                blockClose("rename");
            } else {
                blockPutIdentifier(port.name);
            }
            blockClose("port");
        }
        blockClose("cell");
    }

	/**
	 * Method to count the usage of primitives hierarchically below cell "np"
	 */
	private void writeAllPrims(Cell cell, HashMap<Object,PrimitiveNode> primsFound)
	{
		// do not search this cell if it is an icon
		if (cell.isIcon()) return;

		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			NodeProto np = ni.getProto();
			if (!ni.isCellInstance())
			{
                if (equivs.getNodeEquivalence(ni) != null) continue;        // will be defined by external reference
                PrimitiveNode pn = (PrimitiveNode)np;
                PrimitiveNode.Function fun = pn.getTechnology().getPrimitiveFunction(pn, ni.getTechSpecific());
				//PrimitiveNode.Function fun = ni.getFunction();
				int i = 1;
				if (fun == PrimitiveNode.Function.GATEAND || fun == PrimitiveNode.Function.GATEOR || fun == PrimitiveNode.Function.GATEXOR)
				{
					// count the number of inputs
					for(Iterator<Connection> cIt = ni.getConnections(); cIt.hasNext(); )
					{
						Connection con = cIt.next();
						if (con.getPortInst().getPortProto().getName().equals("a")) i++;
					}
				}
                if (primsFound.get(getPrimKey(ni, i)) != null) continue;    // already written

/*
				if (fun != PrimitiveNode.Function.UNKNOWN && fun != PrimitiveNode.Function.PIN && fun != PrimitiveNode.Function.CONTACT &&
					fun != PrimitiveNode.Function.NODE && fun != PrimitiveNode.Function.CONNECT && fun != PrimitiveNode.Function.METER &&
						fun != PrimitiveNode.Function.CONPOWER && fun != PrimitiveNode.Function.CONGROUND && fun != PrimitiveNode.Function.SOURCE &&
							fun != PrimitiveNode.Function.SUBSTRATE && fun != PrimitiveNode.Function.WELL && fun != PrimitiveNode.Function.ART) {
*/
                if (fun == PrimitiveNode.Function.UNKNOWN || fun == PrimitiveNode.Function.PIN || fun == PrimitiveNode.Function.ART)
                    continue;
                writePrimitive(pn, i, fun);
                primsFound.put(getPrimKey(ni, i), pn);
				continue;
			}

			// ignore recursive references (showing icon in contents)
			if (ni.isIconOfParent()) continue;

			// get actual subcell (including contents/body distinction)
			Cell oNp = ((Cell)np).contentsView();
			if (oNp == null) oNp = (Cell)np;

			// search the subcell
            writeAllPrims(oNp, primsFound);
		}
	}

	/**
	 * Method to count the usage of primitives hierarchically below cell "np"
	 */
	private void countRippers(Cell cell, HashSet<Integer> rippers)
	{
		// do not search this cell if it is an icon
		if (cell.isIcon()) return;

		Netlist netlist = null;
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			NodeProto np = ni.getProto();
			if (!ni.isCellInstance())
			{
                if (equivs.getNodeEquivalence(ni) != null) continue;        // will be defined by external reference
                PrimitiveNode pn = (PrimitiveNode)np;
				PrimitiveNode.Function fun = ni.getFunction();
				if (fun == PrimitiveNode.Function.PIN)
				{
					// check all the connections
					int busWidthFound = -1;
					boolean wireFound = false;
					for(Iterator<Connection> cIt = ni.getConnections(); cIt.hasNext(); )
					{
						Connection con = cIt.next();
						ArcInst ai = con.getArc();
						if (netlist == null) netlist = cell.acquireUserNetlist();
						int width = netlist.getBusWidth(ai);
						if (width > 1) busWidthFound = width; else
							wireFound = true;
					}
					if (wireFound && busWidthFound > 1)
						rippers.add(new Integer(busWidthFound));
				}
				continue;
			}

			// ignore recursive references (showing icon in contents)
			if (ni.isIconOfParent()) continue;

			// get actual subcell (including contents/body distinction)
			Cell oNp = ((Cell)np).contentsView();
			if (oNp == null) oNp = (Cell)np;

			// search the subcell
			countRippers(oNp, rippers);
		}
	}

    private Object getPrimKey(NodeInst ni, int i) {
        if (ni.isCellInstance()) return null;
        PrimitiveNode pn = (PrimitiveNode)ni.getProto();
        PrimitiveNode.Function func = pn.getTechnology().getPrimitiveFunction(pn, ni.getTechSpecific());
        String key = pn.getTechnology().getTechShortName() + "_" + pn.getName() + "_" + func.getConstantName() + "_" +i;
        return key;
    }

	/**
	 * Method to generate a pt symbol (pt x y)
	 */
	private void writePoint(double x, double y)
	{
		blockOpen("pt");
		blockPutInteger(scaleValue(x));
		blockPutInteger(scaleValue(y));
		blockClose("pt");
	}

	/**
	 * Method to convert a bus name to the proper string for output.
	 * @param busName the bus name in Electric.
	 * @return the bus name for EDIF output.
	 */
	private String convertBusName(String busName, Netlist netlist, ElectricObject eObj)
	{
		if (IOTool.isEDIFCadenceCompatibility())
		{
			// see if the bus is simple
			int firstOpen = busName.indexOf('[');
			if (firstOpen < 0) return busName;
			boolean simple = true;
			if (busName.indexOf('[', firstOpen+1) >= 0) simple = false; else
			{
				int closePos = busName.indexOf(']', firstOpen);
				for(int i=firstOpen+1; i<closePos; i++)
				{
					char ch = busName.charAt(i);
					if (ch != ':' && ch != ',' && !TextUtils.isDigit(ch))
					{
						simple = false;
						break;
					}
				}
			}
			if (simple)
			{
				busName = busName.replaceAll("\\[", "\\<").replaceAll("\\]", "\\>");
			} else
			{
				// complex name: break it into many signals
				busName = "";
				if (eObj instanceof ArcInst)
				{
					ArcInst ai = (ArcInst)eObj;
					int width = netlist.getBusWidth(ai);
					for(int i=0; i<width; i++)
					{
						Network net = netlist.getNetwork(ai, i);
						if (busName.length() > 0) busName += ",";
						Iterator<String> nIt = net.getNames();
						String netName;
						if (nIt.hasNext()) netName = nIt.next(); else
							netName = net.describe(true);
						busName += netName.replaceAll("\\[", "_").replaceAll("\\]", "_");
					}
				} else if (eObj instanceof Export)
				{
					Export e = (Export)eObj;
					int width = netlist.getBusWidth(e);
					for(int i=0; i<width; i++)
					{
						Network net = netlist.getNetwork(e, i);
						if (busName.length() > 0) busName += ",";
						Iterator<String> nIt = net.getNames();
						String netName;
						if (nIt.hasNext()) netName = nIt.next(); else
							netName = net.describe(true);
						busName += netName.replaceAll("\\[", "_").replaceAll("\\]", "_");
					}
				}
			}
		}
		return busName;
	}

	/**
	 * Method to map Electric orientations to EDIF orientations
	 */
	public static String getOrientation(NodeInst ni, int addedRotation)
	{
		String orientation = "ERROR";
        int angle = (ni.getAngle() - addedRotation);
        if (angle < 0) angle = angle + 3600;
        if (angle > 3600) angle = angle % 3600;
		switch (angle)
		{
			case 0:    orientation = "";       break;
			case 900:  orientation = "R90";    break;
			case 1800: orientation = "R180";   break;
			case 2700: orientation = "R270";   break;
		}
		if (ni.isMirroredAboutXAxis()) orientation = "MX" + orientation;
		if (ni.isMirroredAboutYAxis()) orientation = "MY" + orientation;
		if (orientation.length() == 0) orientation = "R0";
        if (orientation.equals("MXR180")) orientation = "MY";
        if (orientation.equals("MYR180")) orientation = "MX";
        if (orientation.equals("MXR270")) orientation = "MYR90";
        if (orientation.equals("MYR270")) orientation = "MXR90";
		return orientation;
	}

	/**
	 * Method to scale the requested integer
	 * returns the scaled value
	 */
	private double scaleValue(double val)
	{
		return (int)(val*scale);
	}

	/**
	 * Establish whether port 'e' is a global port or not
	 */
	private boolean isGlobalExport(Export e)
	{
		// pp is a global port if it is marked global
		if (e.isBodyOnly()) return true;

		// or if it does not exist on the icon
		Cell parent = (Cell)e.getParent();
		Cell inp = parent.iconView();
		if (inp == null) return false;
		if (e.getEquivalent() == null) return true;
		return false;
	}

	/**
	 * Method to properly identify an instance of a primitive node
	 * for ASPECT netlists
	 */
	private String describePrimitive(NodeInst ni, PrimitiveNode.Function fun)
	{
		if (fun.isResistor()) /* == PrimitiveNode.Function.RESIST)*/ return "Resistor";
		if (fun == PrimitiveNode.Function.TRANPN) return "npn";
		if (fun == PrimitiveNode.Function.TRAPNP) return "pnp";
        if (fun == PrimitiveNode.Function.TRA4NMOS) return "nfet";
        if (fun == PrimitiveNode.Function.TRA4PMOS) return "pfet";
		if (fun == PrimitiveNode.Function.SUBSTRATE) return "gtap";
		return makeToken(ni.getProto().getName());
	}

	/**
	 * Helper name builder
	 */
	private String makeComponentName(Nodable no)
	{
		String okname = makeValidName(no.getName());
		if (okname.length() > 0)
		{
			char chr = okname.charAt(0);
			if (TextUtils.isDigit(chr) || chr == '_')
				okname = "&" + okname;
		}
		return okname;
	}

	/**
	 * Method to return null if there is no valid name in "var", corrected name if valid.
	 */
	public static String makeValidName(String name)
	{
		StringBuffer iptr = new StringBuffer(name);

		// allow '&' for the first character (this must be fixed later if digit or '_')
		int i = 0;
		if (iptr.charAt(i) == '&') i++;

		// allow "_" and alphanumeric for others
		for(; i<iptr.length(); i++)
		{
			if (TextUtils.isLetterOrDigit(iptr.charAt(i))) continue;
			if (iptr.charAt(i) == '_') continue;
			iptr.setCharAt(i, '_');
		}
		return iptr.toString();
	}

	/**
	 * convert a string token into a valid EDIF string token (note - NOT re-entrant coding)
	 * In order to use NSC program ce2verilog, we need to suppress the '_' which replaces
	 * ']' in bus definitions.
	 */
	private String makeToken(String str)
	{
		if (str.length() == 0) return str;
		StringBuffer sb = new StringBuffer();
		if (TextUtils.isDigit(str.charAt(0))) sb.append('X');
		for(int i=0; i<str.length(); i++)
		{
			char chr = str.charAt(i);
			if (Character.isWhitespace(chr)) break;
			if (chr == '[' || chr == '<') chr = '_';
			if (TextUtils.isLetterOrDigit(chr) || chr == '&' || chr == '_')
				sb.append(chr);
		}
		return sb.toString();
	}

	/****************************** GRAPHIC OUTPUT METHODS ******************************/

	/**
	 * Method to output all graphic objects of a symbol.
	 */
	private void writeSymbol(Cell cell)
	{
        if (cell == null) return;
        if (cell.getView() != View.ICON)
            cell = cell.iconView();
        if (cell == null) return;
		blockOpen("symbol");
		egraphic_override = EGWIRE;
		egraphic = EGUNKNOWN;
		for(Iterator<PortProto> it = cell.getPorts(); it.hasNext(); )
		{
			Export e = (Export)it.next();
            writePortImplementation(e, true);
		}
		egraphic_override = EGUNKNOWN;

		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			writeSymbolCell(ni, GenMath.MATID);
		}
		for(Iterator<ArcInst> it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = it.next();
			writeSymbolArcInst(ai, GenMath.MATID);
		}

		// close figure
		setGraphic(EGUNKNOWN);
		blockClose("symbol");
	}

    private void writeSymbol(PrimitiveNode pn, NodeInst ni) {
        if (pn == null) return;

        blockOpen("symbol");
        egraphic_override = EGWIRE;
        egraphic = EGUNKNOWN;
        for(Iterator<PortProto> it = pn.getPorts(); it.hasNext(); )
        {
            PortProto e = it.next();
            blockOpen("portImplementation");
            blockOpen("name");
            blockPutIdentifier(makeToken(e.getName()));
            blockOpen("display");
            blockOpen("figureGroupOverride");
            blockPutIdentifier(getFigureGroupName(EGWIRE));
            blockOpen("textHeight");
            blockPutInteger(getTextHeight(null));
            blockClose("figureGroupOverride");
            Poly portPoly = ni.getShapeOfPort(e);
            //blockOpen("origin");
            //writePoint(portPoly.getCenterX(), portPoly.getCenterY());
            blockClose("name");
            blockOpen("connectLocation");
            writeSymbolPoly(portPoly, null, 1);

            // close figure
            setGraphic(EGUNKNOWN);
            blockClose("portImplementation");
        }
        egraphic_override = EGUNKNOWN;

        Poly [] polys = pn.getTechnology().getShapeOfNode(ni);
        for (int i=0; i<polys.length; i++) {
            writeSymbolPoly(polys[i], null, 1);
        }

        // close figure
        setGraphic(EGUNKNOWN);
        blockClose("symbol");
    }

    /**
     * Write a portImplementation node.
     * @param e
     * @param closeBlock true to close block, false to leave portImplementation block open
     */
    private void writePortImplementation(Export e, boolean closeBlock) {
        blockOpen("portImplementation");
        blockOpen("name");
        blockPutIdentifier(makeToken(e.getName()));
        blockOpen("display");
        blockOpen("figureGroupOverride");
        blockPutIdentifier(getFigureGroupName(EGWIRE));
        blockOpen("textHeight");
        blockPutInteger(getTextHeight(e.getTextDescriptor(Export.EXPORT_NAME)));
        blockClose("figureGroupOverride");
        blockOpen("origin");
        Poly namePoly = e.getNamePoly();
        writePoint(namePoly.getCenterX(), namePoly.getCenterY());
        blockClose("name");
        blockOpen("connectLocation");
        Poly portPoly = e.getOriginalPort().getPoly();
        egraphic_override = EGWIRE;
        egraphic = EGUNKNOWN;
        writeSymbolPoly(portPoly, null, 1);
        setGraphic(EGUNKNOWN);
        blockClose("connectLocation");
        if (closeBlock) {
            blockClose("portImplementation");
        }
    }

	/**
	 * Method to output a specific symbol cell
	 */
	private void writeSymbolCell(NodeInst ni, AffineTransform prevtrans)
	{
		// make transformation matrix within the current nodeinst
		if (ni.getOrient().equals(Orientation.IDENT))
		{
			writeSymbolNodeInst(ni, prevtrans);
		} else
		{
			AffineTransform localtran = ni.rotateOut(prevtrans);
			writeSymbolNodeInst(ni, localtran);
		}
	}

	/**
	 * Method to symbol "ni" when transformed through "prevtrans".
	 */
	private void writeSymbolNodeInst(NodeInst ni, AffineTransform prevtrans)
	{
		NodeProto np = ni.getProto();

		// primitive nodeinst: ask the technology how to draw it
		if (!ni.isCellInstance())
		{
			Technology tech = np.getTechnology();
			Poly [] polys = tech.getShapeOfNode(ni);
			int high = polys.length;

			// don't draw invisible pins
			int low = 0;
			if (np == Generic.tech.invisiblePinNode) low = 1;

			for (int j = low; j < high; j++)
			{
				// get description of this layer
				Poly poly = polys[j];

				// draw the nodeinst
				poly.transform(prevtrans);

				// draw the nodeinst and restore the color
				// check for text ...
				boolean istext = false;
				if (poly.getStyle().isText())
				{
					istext = true;
					// close the current figure ...
					setGraphic(EGUNKNOWN);
					blockOpen("annotate");
				}

				writeSymbolPoly(poly, null, 1);
				if (istext) blockClose("annotate");
			}
		} else
		{
			// transform into the nodeinst for display of its guts
			Cell subCell = (Cell)np;
			AffineTransform subrot = ni.translateOut(prevtrans);

			// see if there are displayable variables on the cell
			int num = ni.numDisplayableVariables(false);
			if (num != 0)
				setGraphic(EGUNKNOWN);
			Poly [] varPolys = new Poly[num];
			ni.addDisplayableVariables(ni.getBounds(), varPolys, 0, null, false);
            writeDisplayableVariables(varPolys, prevtrans);

			// search through cell
			for(Iterator<NodeInst> it = subCell.getNodes(); it.hasNext(); )
			{
				NodeInst sNi = it.next();
				writeSymbolCell(sNi, subrot);
			}
			for(Iterator<ArcInst> it = subCell.getArcs(); it.hasNext(); )
			{
				ArcInst sAi = it.next();
				writeSymbolArcInst(sAi, subrot);
			}
		}
	}

	/**
	 * Method to draw an arcinst.  Returns indicator of what else needs to
	 * be drawn.  Returns negative if display interrupted
	 */
	private void writeSymbolArcInst(ArcInst ai, AffineTransform trans)
	{
		Technology tech = ai.getProto().getTechnology();
		Poly [] polys = tech.getShapeOfArc(ai);

		// get the endpoints of the arcinst
		Point2D [] points = new Point2D[2];
		points[0] = new Point2D.Double(ai.getTailLocation().getX(), ai.getTailLocation().getY());
		points[1] = new Point2D.Double(ai.getHeadLocation().getX(), ai.getHeadLocation().getY());
        // translate point if needed
        points[0] = equivs.translatePortConnection(points[0], ai.getTailPortInst());
        points[1] = equivs.translatePortConnection(points[1], ai.getHeadPortInst());

		Poly poly = new Poly(points);
		poly.setStyle(Poly.Type.OPENED);
		poly.transform(trans);
		writeSymbolPoly(poly, null, 1);

		// now get the variables
		int num = ai.numDisplayableVariables(false);
		if (num != 0)
			setGraphic(EGUNKNOWN);
		Poly [] varPolys = new Poly[num];
		ai.addDisplayableVariables(ai.getBounds(), varPolys, 0, null, false);
        writeDisplayableVariables(varPolys, trans);
	}

    private void writeDisplayableVariables(Poly [] varPolys, AffineTransform prevtrans) {
        for(int i=0; i<varPolys.length; i++)
        {
            Poly varPoly = varPolys[i];
            String name = null;
			String append = null;
			double scale = 1;
			DisplayedText dt = varPoly.getDisplayedText();
			Variable var = null;
            if (dt != null) var = dt.getVariable();
            if (var != null)
			{
				// see if there is a translation
                name = var.getTrueName();
				EDIFEquiv.VariableEquivalence ve = equivs.getElectricVariableEquivalence(dt.getVariableKey().getName());
				if (ve != null)
				{
					name = ve.externVarName;
					append = ve.appendElecOutput;
					scale = ve.scale;
				}
            }
            if (name == null) continue;

            if (prevtrans != null) varPoly.transform(prevtrans);
            // make sure poly type is some kind of text
            if (!varPoly.getStyle().isText() && var != null) {
                TextDescriptor td = var.getTextDescriptor();
                if (td != null) {
                    Poly.Type style = td.getPos().getPolyType();
                    varPoly.setStyle(style);
                }
            }
            if (varPoly.getString() == null && var != null)
                varPoly.setString(var.getObject().toString());
            blockOpen("property");
            blockPutIdentifier(name);
            blockOpen("string");
            writeSymbolPoly(varPoly, append, scale);
            blockClose("property");
        }
    }

	private void setGraphic(EGraphic type)
	{
		if (type == EGUNKNOWN)
		{
			// terminate the figure
			if (egraphic != EGUNKNOWN) blockClose("figure");
			egraphic = EGUNKNOWN;
		} else if (egraphic_override == EGUNKNOWN)
		{
			// normal case
			if (type != egraphic)
			{
				// new egraphic type
				if (egraphic != EGUNKNOWN) blockClose("figure");
				egraphic = type;
				blockOpen("figure");
				blockPutIdentifier(getFigureGroupName(egraphic));
			}
		} else if (egraphic != egraphic_override)
		{
			// override figure
			if (egraphic != EGUNKNOWN) blockClose("figure");
			egraphic = egraphic_override;
			blockOpen("figure");
			blockPutIdentifier(getFigureGroupName(egraphic));
		}
	}

	/**
	 * Method to write polys into EDIF syntax
	 */
	private void writeSymbolPoly(Poly obj, String append, double scale)
	{
		// now draw the polygon
		Poly.Type type = obj.getStyle();
		Point2D [] points = obj.getPoints();
		if (type == Poly.Type.CIRCLE || type == Poly.Type.DISC || type == Poly.Type.THICKCIRCLE)
		{
			setGraphic(EGART);
			double i = points[0].distance(points[1]);
			blockOpen("circle");
			writePoint(points[0].getX() - i, points[0].getY());
			writePoint(points[0].getX() + i, points[0].getY());
			blockClose("circle");
			return;
		}

		if (type == Poly.Type.CIRCLEARC || type == Poly.Type.THICKCIRCLEARC)
		{
			setGraphic(EGART);

			// arcs at [i] points [1+i] [2+i] clockwise
			if (points.length == 0) return;
			if ((points.length % 3) != 0) return;
			for (int i = 0; i < points.length; i += 3)
			{
				blockOpen("openShape");
				blockOpen("curve");
				blockOpen("arc");
				writePoint(points[i + 1].getX(), points[i + 1].getY());

				// calculate a point between the first and second point
				Point2D si = GenMath.computeArcCenter(points[i], points[i + 1], points[i + 2]);
				writePoint(si.getX(), si.getY());
				writePoint(points[i + 2].getX(), points[i + 2].getY());
				blockClose("openShape");
			}
			return;
		}

		if (type == Poly.Type.FILLED || type == Poly.Type.CLOSED)
		{
			Rectangle2D bounds = obj.getBox();
			if (bounds != null)
			{
				// simple rectangular box
				if (bounds.getWidth() == 0 && bounds.getHeight() == 0)
				{
					if (egraphic_override == EGUNKNOWN) return;
					setGraphic(EGART);
					blockOpen("dot");
					writePoint(bounds.getCenterX(), bounds.getCenterY());
					blockClose("dot");
				} else
				{
					setGraphic(EGART);
					blockOpen("rectangle");
					writePoint(bounds.getMinX(), bounds.getMinY());
					writePoint(bounds.getMaxY(), bounds.getMaxY());
					blockClose("rectangle");
				}
			} else
			{
				setGraphic(EGART);
				blockOpen("path");
				blockOpen("pointList");
				for (int i = 0; i < points.length; i++)
					writePoint(points[i].getX(), points[i].getY());
				if (points.length > 2) writePoint(points[0].getX(), points[0].getY());
				blockClose("path");
			}
			return;
		}
		if (type.isText())
		{
            if (IOTool.isEDIFCadenceCompatibility() && obj.getDisplayedText() != null) {
                // Properties in Cadence do not have position info, Cadence
                // determines position automatically and cannot be altered by user.
                // There also does not seem to be any way to set the 'display' so
                // that it shows up on the instance
				String value = obj.getDisplayedText().getVariable().getPureValue(-1);
				if (scale != 1)
				{
					double scaled = TextUtils.atof(value);
					value = TextUtils.formatDouble(scaled * scale);
				}
				if (append != null) value += append;
                String str = convertElectricPropToCadence(value);
                str = str.replaceAll("\"", "%34%");
                blockPutString(str);
                return;
            }

			Rectangle2D bounds = obj.getBounds2D();
			setGraphic(EGUNKNOWN);
			blockOpen("stringDisplay");
            String str = obj.getString().replaceAll("\"", "%34%");
			if (append != null) str += append;
			blockPutString(str);
			blockOpen("display");
			TextDescriptor td = obj.getTextDescriptor();
			if (td != null)
			{
				blockOpen("figureGroupOverride");
				blockPutIdentifier(getFigureGroupName(EGART));

				// output the text height
				blockOpen("textHeight");

				// 2 pixels = 0.0278 in or 36 double pixels per inch
				double height = getTextHeight(td);
                blockPutInteger(height);
				blockClose("figureGroupOverride");
			} else {
                blockPutIdentifier(EGART.getText());
            }
			if (type == Poly.Type.TEXTCENT) blockPut("justify", "CENTERCENTER"); else
			if (type == Poly.Type.TEXTTOP) blockPut("justify", "LOWERCENTER"); else
			if (type == Poly.Type.TEXTBOT) blockPut("justify", "UPPERCENTER"); else
			if (type == Poly.Type.TEXTLEFT) blockPut("justify", "CENTERRIGHT"); else
			if (type == Poly.Type.TEXTRIGHT) blockPut("justify", "CENTERLEFT"); else
			if (type == Poly.Type.TEXTTOPLEFT) blockPut("justify", "LOWERRIGHT"); else
			if (type == Poly.Type.TEXTBOTLEFT) blockPut("justify", "UPPERRIGHT"); else
			if (type == Poly.Type.TEXTTOPRIGHT) blockPut("justify", "LOWERLEFT"); else
			if (type == Poly.Type.TEXTBOTRIGHT) blockPut("justify", "UPPERLEFT");
			blockPut("orientation", "R0");
			blockOpen("origin");
			writePoint(bounds.getMinX(), bounds.getMinY());
			blockClose("stringDisplay");
			return;
		}
		if (type == Poly.Type.OPENED || type == Poly.Type.OPENEDT1 ||
			type == Poly.Type.OPENEDT2 || type == Poly.Type.OPENEDT3)
		{
			// check for closed 4 sided figure
			if (points.length == 5 && points[4].getX() == points[0].getX() && points[4].getY() == points[0].getY())
			{
				Rectangle2D bounds = obj.getBox();
				if (bounds != null)
				{
					// simple rectangular box
					if (bounds.getWidth() == 0 && bounds.getHeight() == 0)
					{
						if (egraphic_override == EGUNKNOWN) return;
						setGraphic(EGART);
						blockOpen("dot");
						writePoint(bounds.getCenterX(), bounds.getCenterY());
						blockClose("dot");
					} else
					{
						setGraphic(EGART);
						blockOpen("rectangle");
						writePoint(bounds.getMinX(), bounds.getMinY());
						writePoint(bounds.getMaxX(), bounds.getMaxY());
						blockClose("rectangle");
					}
					return;
				}
			}
			setGraphic(EGART);
			blockOpen("path");
			blockOpen("pointList");
			for (int i = 0; i < points.length; i++)
				writePoint(points[i].getX(), points[i].getY());
			blockClose("path");
			return;
		}
		if (type == Poly.Type.VECTORS)
		{
			setGraphic(EGART);
			for (int i = 0; i < points.length; i += 2)
			{
				blockOpen("path");
				blockOpen("pointList");
				writePoint(points[i].getX(), points[i].getY());
				writePoint(points[i + 1].getX(), points[i + 1].getY());
				blockClose("path");
			}
			return;
		}
	}

	private String getFigureGroupName(EGraphic graphic)
	{
		String name = graphic.getText();
		EDIFEquiv.FigureGroupEquivalence fge = equivs.getElectricFigureGroupEquivalence(name);
		if (fge != null) name = fge.externFGName;
		return name;
	}

	private void writeFigureGroup(EGraphic graphic) {
        blockOpen("figureGroup");
        blockPutIdentifier(getFigureGroupName(graphic));
        blockClose("figureGroup");
    }

    private void writeScale(Technology tech) {
        //double meters_to_lambda = TextUtils.convertDistance(1, tech, TextUtils.UnitScale.NONE);
        blockOpen("scale");
        blockPutInteger(160);
        blockPutDouble(0.0254);
        blockPut("unit", "DISTANCE");
        blockClose("scale");
    }

    private double getTextHeight(TextDescriptor td) {
        double size = 2;        // default
        if (td != null) {
            size = td.getSize().getSize();
            if (!td.getSize().isAbsolute()) {
                size = size * 2;
            }
        }
        // 2 pixels = 0.0278 in or 36 double pixels per inch
        double height = size * 10 / 36;
        return scaleValue(height);
    }



    private static final Pattern atPat = Pattern.compile("@(\\w+)");
    private static final Pattern pPat = Pattern.compile("(P|PAR)\\(\"(\\w+)\"\\)");
    /**
     * Convert a property in Electric, with parameter passing, to
     * Cadence parameter passing syntax
     * @param prop the expression
     * @return an equivalent expression in Cadence
     */
    public static String convertElectricPropToCadence(String prop) {
        StringBuffer sb = new StringBuffer();
        Matcher atMat = atPat.matcher(prop);
        while (atMat.find()) {
            atMat.appendReplacement(sb, "P(\""+atMat.group(1)+"\")");
        }
        atMat.appendTail(sb);

        prop = sb.toString();
        sb = new StringBuffer();
        Matcher pMat = pPat.matcher(prop);
        while (pMat.find()) {
            String c = "+";                   // default
            if (pMat.group(1).equals("PAR"))
                c = "@";
            pMat.appendReplacement(sb, "["+c+pMat.group(2)+"]");
        }
        pMat.appendTail(sb);

        return sb.toString();
    }

    private static final Pattern pparPat = Pattern.compile("(pPar|iPar|atPar|dotPar|_Par)\\(\"(\\w+)\"\\)");
    private static final Pattern bPat = Pattern.compile("\\[([~+.@])(\\w+)\\]");
    /**
     * Convert a property in Cadence, with parameter passing, to
     * Electric parameter passing syntax
     * @param prop the expression
     * @return an equivalent expression in Electric
     */
    public static String convertCadencePropToElectric(String prop) {
        String origProp = prop;
        StringBuffer sb = new StringBuffer();
        Matcher atMat = bPat.matcher(prop);
        while (atMat.find()) {
            String call = "pPar";
            if (atMat.group(1).equals("+")) call = "pPar";
            else if (atMat.group(1).equals("@")) call = "atPar";
            else {
                System.out.println("Warning converting properties: Electric does not support \"["+atMat.group(1)+"param], using [+param] instead, in "+origProp);
            }
            //if (atMat.group(1).equals("~")) call = "iPar";
            //if (atMat.group(1).equals(".")) call = "dotPar";
            atMat.appendReplacement(sb, call+"(\""+atMat.group(2)+"\")");
        }
        atMat.appendTail(sb);

        prop = sb.toString();
        sb = new StringBuffer();
        Matcher pMat = pparPat.matcher(prop);
        while (pMat.find()) {
            String c = "P";                   // default
            if (pMat.group(1).equals("pPar")) c = "P";
            else if (pMat.group(1).equals("atPar")) c = "PAR";
            else {
                System.out.println("Warning converting properties: Electric does not support \"["+pMat.group(1)+"param], using pPar instead, in "+origProp);
            }
            pMat.appendReplacement(sb, c+"(\""+pMat.group(2)+"\")");
        }
        pMat.appendTail(sb);

        return sb.toString();
    }

	/****************************** LOW-LEVEL BLOCK OUTPUT METHODS ******************************/

    /**
     * Will open a new keyword block, will indent the new block
     * depending on depth of the keyword
     */
    private void blockOpen(String keyword) {
        blockOpen(keyword, true);
    }

	/**
	 * Will open a new keyword block, will indent the new block
	 * depending on depth of the keyword
	 */
	private void blockOpen(String keyword, boolean startOnNewLine)
	{
        if (blkstack_ptr > 0 && startOnNewLine) printWriter.print("\n");
		// output the new block
		blkstack[blkstack_ptr++] = keyword;

		// output the keyword
        String blanks = startOnNewLine ? getBlanks(blkstack_ptr-1) : " ";
		printWriter.print(blanks + "( " + keyword);
	}

	/**
	 * Will output a one identifier block
	 */
	private void blockPut(String keyword, String identifier)
	{
		// output the new block
		if (blkstack_ptr != 0) printWriter.print("\n");

		// output the keyword
		printWriter.print(getBlanks(blkstack_ptr) + "( " + keyword + " " + identifier + " )");
	}

	/**
	 * Will output a string identifier to the file
	 */
	private void blockPutIdentifier(String str)
	{
		printWriter.print(" " + str);
	}

	/**
	 * Will output a quoted string to the file
	 */
	private void blockPutString(String str)
	{
		printWriter.print(" \"" + str + "\"");
	}

	/**
	 * Will output an integer to the edif file
	 */
	private void blockPutInteger(double val)
	{
		printWriter.print(" " + TextUtils.formatDouble(val));
	}

	/**
	 * Will output a floating value to the edif file
	 */
	private static DecimalFormat decimalFormatScientific = null;
	private void blockPutDouble(double val)
	{
		if (decimalFormatScientific == null)
		{
			decimalFormatScientific = new DecimalFormat("########E0");
			decimalFormatScientific.setGroupingUsed(false);
		}
		String ret = decimalFormatScientific.format(val).replace('E', ' ');
		ret = ret.replaceAll("\\.[0-9]+", "");
		printWriter.print(" ( e " + ret + " )");
	}

	private void blockClose(String keyword)
	{
		if (blkstack_ptr == 0) return;
		int depth = 1;
		if (keyword != null)
		{
			// scan for this saved keyword
			for (depth = 1; depth <= blkstack_ptr; depth++)
			{
				if (blkstack[blkstack_ptr - depth].equals(keyword)) break;
			}
			if (depth > blkstack_ptr)
			{
				System.out.println("EDIF output: could not match keyword <" + keyword + ">");
				return;
			}
		}

		// now terminate and free keyword list
		do
		{
			blkstack_ptr--;
			printWriter.print(" )");
		} while ((--depth) > 0);
	}

	/**
	 * Method to terminate all currently open blocks.
	 */
	private void blockFinish()
	{
		if (blkstack_ptr > 0)
		{
			blockClose(blkstack[0]);
		}
		printWriter.print("\n");
	}

	private int blkstack_ptr;
	private String [] blkstack = new String[50];

	private String getBlanks(int num)
	{
		StringBuffer sb = new StringBuffer();
		for(int i=0; i<num; i++) sb.append(' ');
		return sb.toString();
	}

	/****************************** SUBCLASSED METHODS FOR THE TOPOLOGY ANALYZER ******************************/

	/**
	 * Method to adjust a cell name to be safe for EDIF output.
	 * @param name the cell name.
	 * @return the name, adjusted for EDIF output.
	 */
	protected String getSafeCellName(String name) { return name; }

	/** Method to return the proper name of Power */
	protected String getPowerName(Network net) { return "VDD"; }

	/** Method to return the proper name of Ground */
	protected String getGroundName(Network net) { return "GND"; }

	/** Method to return the proper name of a Global signal */
	protected String getGlobalName(Global glob) { return glob.getName(); }

	/** Method to report that export names DO take precedence over
	 * arc names when determining the name of the network. */
	protected boolean isNetworksUseExportedNames() { return true; }

	/** Method to report that library names ARE always prepended to cell names. */
	protected boolean isLibraryNameAlwaysAddedToCellName() { return false; }

	/** Method to report that aggregate names (busses) are NOT used (bus information is extracted independently). */
	protected boolean isAggregateNamesSupported() { return false; }

	/** Method to report whether input and output names are separated. */
	protected boolean isSeparateInputAndOutput() { return true; }

	/**
	 * Method to adjust a network name to be safe for EDIF output.
	 */
	protected String getSafeNetName(String name, boolean bus) { return name; }

    /** Tell the Hierarchy enumerator whether or not to short parasitic resistors */
    protected boolean isShortResistors() { return false; }

    /** Tell the Hierarchy enumerator whether or not to short explicit (poly) resistors */
    protected boolean isShortExplicitResistors() { return false; }

	/**
	 * Method to tell whether the topological analysis should mangle cell names that are parameterized.
	 */
	protected boolean canParameterizeNames() { return false; }
}
