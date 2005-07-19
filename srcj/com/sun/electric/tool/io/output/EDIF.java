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
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.hierarchy.*;
import com.sun.electric.database.network.Global;
import com.sun.electric.database.network.Network;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * This is the netlister for EDIF.
 */
public class EDIF extends Topology
{
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

    private int scale = 20;
    private static final boolean CadenceProperties = true;

    private final HashMap libsToWrite; // key is Library, Value is LibToWrite
    private final List libsToWriteOrder; // list of libraries to write, in order

    private static class LibToWrite {
        private final Library lib;
        private final List cellsToWrite;
        private LibToWrite(Library l) {
            lib = l;
            cellsToWrite = new ArrayList();
        }
        private void add(CellToWrite c) { cellsToWrite.add(c); }
        private Iterator getCells() { return cellsToWrite.iterator(); }
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
	 * @param cellJob contains following information
     * cell: the top-level cell to write.
     * context: the hierarchical context to the cell.
	 * filePath: the disk file to create with EDIF.
	 */
	public static void writeEDIFFile(OutputCellInfo cellJob)
	{
		EDIF out = new EDIF();
		if (out.openTextOutputStream(cellJob.filePath)) return;
		if (out.writeCell(cellJob.cell, cellJob.context)) return;
		if (out.closeTextOutputStream()) return;
		System.out.println(cellJob.filePath + " written");
	}

	/**
	 * Creates a new instance of the EDIF netlister.
	 */
	EDIF()
	{
        libsToWrite = new HashMap();
        libsToWriteOrder = new ArrayList();
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

		// determine the primitives being used
		HashMap useCount = new HashMap();
		if (searchHierarchy(topCell, useCount) != 0)
		{
			// write out all primitives used in the library
			blockOpen("library");
			blockPutIdentifier("lib0");
			blockPut("edifLevel", "0");
			blockOpen("technology");
			blockOpen("numberDefinition");
			if (IOTool.isEDIFUseSchematicView())
			{
                writeScale(Technology.getCurrent());
			}
			blockClose("technology");
			for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
			{
				Technology tech = (Technology)it.next();
				for(Iterator pIt = tech.getNodes(); pIt.hasNext(); )
				{
					NodeProto np = (NodeProto)pIt.next();
					GenMath.MutableInteger mi = (GenMath.MutableInteger)useCount.get(np);
					if (mi == null) continue;

					PrimitiveNode.Function fun = np.getFunction();
					if (fun == PrimitiveNode.Function.UNKNOWN || fun == PrimitiveNode.Function.PIN ||
						fun == PrimitiveNode.Function.CONTACT || fun == PrimitiveNode.Function.NODE ||
						fun == PrimitiveNode.Function.CONNECT || fun == PrimitiveNode.Function.ART) continue;
					for (int i = 0; i < mi.intValue(); i++)
						writePrimitive(np, i, fun);
				}
			}
			blockClose("library");

            // external lib
/*
            blockOpen("external");
            blockPutIdentifier("sample");
            blockPut("edifLevel", "0");
            blockOpen("technology");
            blockOpen("numberDefinition");
            if (IOTool.isEDIFUseSchematicView())
            {
                writeScale(Technology.getCurrent());
            }
            blockClose("technology");
*/

		}
	}

    /**
     * Build up lists of cells that need to be written, organized by library
     */
    protected void writeCellTopology(Cell cell, CellNetInfo cni, VarContext context)
    {
        Library lib = cell.getLibrary();
        LibToWrite l = (LibToWrite)libsToWrite.get(lib);
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
        for (Iterator it = libsToWriteOrder.iterator(); it.hasNext(); ) {
            Library lib = (Library)it.next();
            LibToWrite l = (LibToWrite)libsToWrite.get(lib);
            // here is where we write everything out, organized by library
            // write library header
            blockOpen("library");
            blockPutIdentifier(makeToken(lib.getName()));
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
            for (Iterator it2 = l.getCells(); it2.hasNext(); ) {
                CellToWrite c = (CellToWrite)it2.next();
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
    private void writeCellEdif(Cell cell, CellNetInfo cni, VarContext context) {

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
		for(Iterator it = cni.getCellSignals(); it.hasNext(); )
		{
			CellSignal cs = (CellSignal)it.next();
			if (cs.isExported())
			{
				Export e = cs.getExport();
				String direction = "INPUT";
				if (e.getCharacteristic() == PortCharacteristic.OUT ||
					e.getCharacteristic() == PortCharacteristic.REFOUT) direction = "OUTPUT";
				if (e.getCharacteristic() == PortCharacteristic.BIDIR) direction = "INOUT";
				blockOpen("port");
				blockPutIdentifier(makeToken(cs.getName()));
				blockPut("direction", direction);
				blockClose("port");
			}
		}
		if (IOTool.isEDIFUseSchematicView())
		{
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
		Netlist netList = cni.getNetList();
		for(Iterator nIt = cell.getNodes(); nIt.hasNext(); )
		{
			NodeInst no = (NodeInst)nIt.next();
			if (no.getProto() instanceof PrimitiveNode)
			{
				PrimitiveNode.Function fun = ((NodeInst)no).getFunction();
                Variable var = no.getVar("ART_message");
                if (var != null) {
                    // this is cell annotation text
                    blockOpen("commentGraphics");
                    blockOpen("annotate");
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
                    writeSymbolPoly(p);
                    blockClose("commentGraphics");
                    continue;
                }
				if (fun == PrimitiveNode.Function.UNKNOWN || fun == PrimitiveNode.Function.PIN ||
					fun == PrimitiveNode.Function.CONTACT || fun == PrimitiveNode.Function.NODE ||
					fun == PrimitiveNode.Function.CONNECT || fun == PrimitiveNode.Function.ART) continue;
			}

			blockOpen("instance");
			String iname = makeComponentName(no);
			String oname = no.getName();
			if (!oname.equalsIgnoreCase(iname))
			{
				blockOpen("rename");
				blockPutIdentifier(iname);
				blockPutString(oname);
				blockClose("rename");
			} else blockPutIdentifier(iname);

			if (no.getProto() instanceof PrimitiveNode)
			{
				NodeInst ni = (NodeInst)no;
				PrimitiveNode.Function fun = ni.getFunction();
				blockOpen("viewRef");
				blockPutIdentifier("symbol");
				blockOpen("cellRef");
                String lib = "lib0";
				if (fun == PrimitiveNode.Function.GATEAND || fun == PrimitiveNode.Function.GATEOR || fun == PrimitiveNode.Function.GATEXOR)
				{
					// count the number of inputs
					int i = 0;
					for(Iterator pIt = ni.getConnections(); pIt.hasNext(); )
					{
						Connection con = (Connection)pIt.next();
						if (con.getPortInst().getPortProto().getName().equals("a")) i++;
					}
					String name = makeToken(ni.getProto().getName()) + i;
					blockPutIdentifier(name);

				} else if (fun == PrimitiveNode.Function.TRA4NMOS || fun == PrimitiveNode.Function.TRA4PMOS) {
                    // NMOS (Complementary) 4-port transistor
                    blockPutIdentifier(describePrimitive(ni, fun));
                    lib = "sample";
                }
                else {
                    blockPutIdentifier(describePrimitive(ni, fun));
                }
				blockPut("libraryRef", lib);
				blockClose("viewRef");
			} else
			{
//				if (((Cell)ni.getProto()).isIcon() &&
//					((Cell)ni.getProto()).contentsView() == null)
//				{
//					// this node came from an external schematic library
//					blockOpen("viewRef");
//					blockPutIdentifier("cell");
//					blockOpen("cellRef");
//					blockPutIdentifier(makeToken(ni.getProto().getName()));
//					String name = "schem_lib_" + ni.getProto().getName();
//					blockPut("libraryRef", name);
//					blockClose("viewRef");
//				} else
				{
					// this node came from some library
					blockOpen("viewRef");
					blockPutIdentifier("symbol");
					blockOpen("cellRef");
                    Cell np = (Cell)no.getProto();
					blockPutIdentifier(makeToken(no.getProto().getName()));
					blockPut("libraryRef", np.getLibrary().getName());
					blockClose("viewRef");
				}
			}

			// now graphical information
			if (IOTool.isEDIFUseSchematicView())
			{
                NodeInst ni = (NodeInst)no;
                blockOpen("transform");

                // get the orientation (note only support orthogonal)
                blockPut("orientation", getOrientation(ni));

                // now the origin
                blockOpen("origin");
                double cX = ni.getAnchorCenterX(), cY = ni.getAnchorCenterY();
                if (no.getProto() instanceof Cell)
                {
                    Rectangle2D cellBounds = ((Cell)no.getProto()).getBounds();
                    cX = ni.getTrueCenterX() - cellBounds.getCenterX();
                    cY = ni.getTrueCenterY() - cellBounds.getCenterY();
                }
                Point2D pt = new Point2D.Double(cX, cY);
                AffineTransform trans = ni.rotateOut();
                trans.transform(pt, pt);
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
                writeDisplayableVariables(varPolys, "NODE_name", ni.rotateOut());
			}
			blockClose("instance");
		}

		// if there is anything to connect, write the networks in the cell
		for(Iterator it = cni.getCellSignals(); it.hasNext(); )
		{
			CellSignal cs = (CellSignal)it.next();

			// ignore unconnected (unnamed) nets
			String netName = cs.getNetwork().describe(false);
			if (netName.length() == 0) continue;

			// establish if this is a global net
			boolean globalport = false;
//			if ((pp = (PORTPROTO *) net->temp2) != NOPORTPROTO)
//				globalport = isGlobalExport(pp);

			blockOpen("net");
			netName = cs.getName();
			if (globalport)
			{
				blockOpen("rename");
				blockPutIdentifier(makeToken(netName));
				String name = makeToken(netName) + "!";
				blockPutIdentifier(name);
				blockClose("rename");
				blockPut("property", "GLOBAL");
			} else
			{
				String oname = makeToken(netName);
				if (!oname.equals(netName))
				{
					// different names
					blockOpen("rename");
					blockPutIdentifier(oname);
					blockPutString(netName);
					blockClose("rename");
				} else blockPutIdentifier(oname);
			}

			// write net connections
			blockOpen("joined");

			// include exported ports
			if (cs.isExported())
			{
				Export e = cs.getExport();
				String pt = e.getName();
				blockPut("portRef", makeToken(pt));
			}

			Network net = cs.getNetwork();
			for(Iterator nIt = netList.getNodables(); nIt.hasNext(); )
			{
				Nodable no = (Nodable)nIt.next();
				NodeProto niProto = no.getProto();
				if (niProto instanceof Cell)
				{
					String nodeName = parameterizedName(no, context);
					CellNetInfo subCni = getCellNetInfo(nodeName);
					for(Iterator sIt = subCni.getCellSignals(); sIt.hasNext(); )
					{
						CellSignal subCs = (CellSignal)sIt.next();

						// ignore networks that aren't exported
						PortProto subPp = subCs.getExport();
						if (subPp == null) continue;

						// single signal
						Network subNet = netList.getNetwork(no, subPp, subCs.getExportIndex());
						if (cs != cni.getCellSignal(subNet)) continue;
						blockOpen("portRef");
						blockPutIdentifier(makeToken(subCs.getName()));
						blockPut("instanceRef", makeComponentName(no));
						blockClose("portRef");
					}
				} else
				{
					NodeInst ni = (NodeInst)no;
					PrimitiveNode.Function fun = ni.getFunction();
					if (fun == PrimitiveNode.Function.UNKNOWN || fun == PrimitiveNode.Function.PIN ||
						fun == PrimitiveNode.Function.CONTACT || fun == PrimitiveNode.Function.NODE ||
						fun == PrimitiveNode.Function.CONNECT || fun == PrimitiveNode.Function.ART) continue;
					for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
					{
						Connection con = (Connection)cIt.next();
						ArcInst ai = con.getArc();
						Network aNet = netList.getNetwork(ai, 0);
						if (aNet != net) continue;
						String pt = con.getPortInst().getPortProto().getName();
						blockOpen("portRef");
						blockPutIdentifier(makeToken(pt));
						blockPut("instanceRef", makeComponentName(no));
						blockClose("portRef");
					}
				}
			}
			blockClose("joined");

			if (IOTool.isEDIFUseSchematicView())
			{
				// output net graphic information for all arc instances connected to this net
				egraphic = EGUNKNOWN;
				egraphic_override = EGWIRE;
				for(Iterator aIt = cell.getArcs(); aIt.hasNext(); )
				{
					ArcInst ai = (ArcInst)aIt.next();
					int aWidth = netList.getBusWidth(ai);
					if (aWidth > 1) continue;
					Network aNet = netList.getNetwork(ai, 0);
					if (aNet == net) writeSymbolArcInst(ai, GenMath.MATID);
				}
				setGraphic(EGUNKNOWN);
				egraphic_override = EGUNKNOWN;
			}

			if (globalport)
				blockPut("userData", "global");
			blockClose("net");
		}

		// write busses
		for(Iterator it = cni.getCellAggregateSignals(); it.hasNext(); )
		{
			CellAggregateSignal cas = (CellAggregateSignal)it.next();

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
				for(Iterator aIt = cell.getArcs(); aIt.hasNext(); )
				{
					ArcInst ai = (ArcInst)aIt.next();
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

        // write text

        Poly [] text = cell.getAllText(true, null);
        for (int i=0; i<text.length; i++) {
            Poly p = text[i];
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
	private void writePrimitive(NodeProto np, int i, PrimitiveNode.Function fun)
	{
		// write primitive name
		if (fun == PrimitiveNode.Function.GATEAND || fun == PrimitiveNode.Function.GATEOR || fun == PrimitiveNode.Function.GATEXOR)
		{
			blockOpen("cell");
			String name = makeToken(np.getName()) + i;
			blockPutIdentifier(name);
		} else
		{
			blockOpen("cell");
			blockPutIdentifier(makeToken(np.getName()));
		}

		// write primitive connections
		blockPut("cellType", "generic");
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
		for(int k=firstPortIndex; k<np.getNumPorts(); k++)
		{
			PortProto pp = np.getPort(k);
			String direction = "input";
			if (pp.getCharacteristic() == PortCharacteristic.OUT) direction = "output"; else
				if (pp.getCharacteristic() == PortCharacteristic.BIDIR) direction = "inout";
			blockOpen("port");
			blockPutIdentifier(makeToken(pp.getName()));
			blockPut("direction", direction);
			blockClose("port");
		}
		blockClose("cell");
	}

	/**
	 * Method to count the usage of primitives hierarchically below cell "np"
	 */
	private int searchHierarchy(Cell cell, HashMap useMap)
	{
		// do not search this cell if it is an icon
		if (cell.isIcon()) return 0;

		// keep a count of the total number of primitives encountered
		int primcount = 0;

		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			NodeProto np = ni.getProto();
			if (np instanceof PrimitiveNode)
			{
				PrimitiveNode.Function fun = ni.getFunction();
				int i = 1;
				if (fun == PrimitiveNode.Function.GATEAND || fun == PrimitiveNode.Function.GATEOR || fun == PrimitiveNode.Function.GATEXOR)
				{
					// count the number of inputs
					for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
					{
						Connection con = (Connection)cIt.next();
						if (con.getPortInst().getPortProto().getName().equals("a")) i++;
					}
				}
				GenMath.MutableInteger count = (GenMath.MutableInteger)useMap.get(np);
				if (count == null)
				{
					count = new GenMath.MutableInteger(0);
					useMap.put(np, count);
				}
				count.setValue(Math.max(count.intValue(), i));
	
				if (fun != PrimitiveNode.Function.UNKNOWN && fun != PrimitiveNode.Function.PIN && fun != PrimitiveNode.Function.CONTACT &&
					fun != PrimitiveNode.Function.NODE && fun != PrimitiveNode.Function.CONNECT && fun != PrimitiveNode.Function.METER &&
						fun != PrimitiveNode.Function.CONPOWER && fun != PrimitiveNode.Function.CONGROUND && fun != PrimitiveNode.Function.SOURCE &&
							fun != PrimitiveNode.Function.SUBSTRATE && fun != PrimitiveNode.Function.WELL && fun != PrimitiveNode.Function.ART)
								primcount++;
				continue;
			}

			// ignore recursive references (showing icon in contents)
			if (ni.isIconOfParent()) continue;

			// get actual subcell (including contents/body distinction)
			Cell oNp = ((Cell)np).contentsView();
			if (oNp == null) oNp = (Cell)np;

			// search the subcell
			GenMath.MutableInteger count = (GenMath.MutableInteger)useMap.get(oNp);
			if (count == null) primcount += searchHierarchy(oNp, useMap);
		}
		GenMath.MutableInteger count = (GenMath.MutableInteger)useMap.get(cell);
		if (count == null)
		{
			count = new GenMath.MutableInteger(0);
			useMap.put(cell, count);
		}
		count.increment();
		return primcount;
	}

    private void writeInstanceRef(NodeInst ni, Library lib) {

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
	 * Method to map Electric orientations to EDIF orientations
	 */
	private String getOrientation(NodeInst ni)
	{
		String orientation = "ERROR";
		switch (ni.getAngle())
		{
			case 0:    orientation = "";       break;
			case 900:  orientation = "R90";    break;
			case 1800: orientation = "R180";   break;
			case 2700: orientation = "R270";   break;
		}
		if (ni.isMirroredAboutXAxis()) orientation = "MX" + orientation;
		if (ni.isMirroredAboutYAxis()) orientation = "MY" + orientation;
		if (orientation.length() == 0) orientation = "R0";
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
	private String makeValidName(String name)
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
			if (chr == '[') chr = '_';
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
		blockOpen("symbol");
		egraphic_override = EGWIRE;
		egraphic = EGUNKNOWN;
		for(Iterator it = cell.getPorts(); it.hasNext(); )
		{
			Export e = (Export)it.next();
			blockOpen("portImplementation");
            blockOpen("name");
			blockPutIdentifier(makeToken(e.getName()));
            blockOpen("display");
            blockOpen("figureGroupOverride");
            blockPutIdentifier(EGWIRE.getText());
            blockOpen("textHeight");
            blockPutInteger(getTextHeight(e.getTextDescriptor(Export.EXPORT_NAME_TD)));
            blockClose("figureGroupOverride");
            blockOpen("origin");
            Poly portPoly = e.getOriginalPort().getPoly();
            writePoint(portPoly.getCenterX(), portPoly.getCenterY());
            blockClose("name");
			blockOpen("connectLocation");
			writeSymbolPoly(portPoly);
	
			// close figure
			setGraphic(EGUNKNOWN);
			blockClose("portImplementation");
		}
		egraphic_override = EGUNKNOWN;

		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			writeSymbolCell(ni, GenMath.MATID);
		}
		for(Iterator it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			writeSymbolArcInst(ai, GenMath.MATID);
		}

		// close figure
		setGraphic(EGUNKNOWN);
		blockClose("symbol");
	}

	/**
	 * Method to output a specific symbol cell
	 */
	private void writeSymbolCell(NodeInst ni, AffineTransform prevtrans)
	{
		// make transformation matrix within the current nodeinst
		if (ni.getAngle() == 0 && !ni.isMirroredAboutXAxis() && !ni.isMirroredAboutYAxis())
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
		if (np instanceof PrimitiveNode)
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

				writeSymbolPoly(poly);
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
            writeDisplayableVariables(varPolys, "NODE_name", prevtrans);

			// search through cell
			for(Iterator it = subCell.getNodes(); it.hasNext(); )
			{
				NodeInst sNi = (NodeInst)it.next();
				writeSymbolCell(sNi, subrot);
			}
			for(Iterator it = subCell.getArcs(); it.hasNext(); )
			{
				ArcInst sAi = (ArcInst)it.next();
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
		Poly poly = new Poly(points);
		poly.setStyle(Poly.Type.OPENED);
		poly.transform(trans);
		writeSymbolPoly(poly);

		// now get the variables
		int num = ai.numDisplayableVariables(false);
		if (num != 0)
			setGraphic(EGUNKNOWN);
		Poly [] varPolys = new Poly[num];
		ai.addDisplayableVariables(ai.getBounds(), varPolys, 0, null, false);
        writeDisplayableVariables(varPolys, "ARC_name", trans);
	}

    private void writeDisplayableVariables(Poly [] varPolys, String defaultVarName, AffineTransform prevtrans) {
        for(int i=0; i<varPolys.length; i++)
        {
            Poly varPoly = varPolys[i];
            String name = null;
            Variable var = varPoly.getVariable();
            if (var != null)
                name = var.getTrueName();
            else {
                if (varPoly.getName() != null)
                    name = defaultVarName;
            }
            if (name == null) continue;
            if (prevtrans != null) varPoly.transform(prevtrans);
            // make sure poly type is some kind of text
            if (!varPoly.getStyle().isText()) {
                TextDescriptor td = var.getTextDescriptor();
                if (td != null) {
                    Poly.Type style = td.getPos().getPolyType();
                    varPoly.setStyle(style);
                }
            }
            if (varPoly.getString() == null)
                varPoly.setString(var.getObject().toString());
            blockOpen("property");
            blockPutIdentifier(name);
            blockOpen("string");
            writeSymbolPoly(varPoly);
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
				blockPutIdentifier(egraphic.getText());
			}
		} else if (egraphic != egraphic_override)
		{
			// override figure
			if (egraphic != EGUNKNOWN) blockClose("figure");
			egraphic = egraphic_override;
			blockOpen("figure");
			blockPutIdentifier(egraphic.getText());
		}
	}

	/**
	 * Method to write polys into EDIF syntax
	 */
	private void writeSymbolPoly(Poly obj)
	{
		// now draw the polygon
		Poly.Type type = obj.getStyle();
		Point2D [] points = obj.getPoints();
		if (type == Poly.Type.CIRCLE || type == Poly.Type.DISC)
		{
			setGraphic(EGART);
			double i = points[0].distance(points[1]);
			blockOpen("circle");
			writePoint(points[0].getX() - i, points[0].getY());
			writePoint(points[0].getX() + i, points[0].getY());
			blockClose("circle");
			return;
		}

		if (type == Poly.Type.CIRCLEARC)
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
            if (CadenceProperties && obj.getVariable() != null) {
                // Properties in Cadence do not have position info, Cadence
                // determines position automatically and cannot be altered by user.
                // There also does not seem to be any way to set the 'display' so
                // that it shows up on the instance
                blockPutString(obj.getString());
                return;
            }

			Rectangle2D bounds = obj.getBounds2D();
			setGraphic(EGUNKNOWN);
			blockOpen("stringDisplay");
			blockPutString(obj.getString());
			blockOpen("display");
			TextDescriptor td = obj.getTextDescriptor();
			if (td != null && td.getSize().isAbsolute())
			{
				blockOpen("figureGroupOverride");
				blockPutIdentifier(EGART.getText());

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

    private void writeFigureGroup(EGraphic graphic) {
        blockOpen("figureGroup");
        blockPutIdentifier(graphic.getText());
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
        // 2 pixels = 0.0278 in or 36 double pixels per inch
        double height = td.getSize().getSize() * 10 / 36;
        return scaleValue(height);
    }


	/****************************** LOW-LEVEL BLOCK OUTPUT METHODS ******************************/

	/**
	 * Will open a new keyword block, will indent the new block
	 * depending on depth of the keyword
	 */
	private void blockOpen(String keyword)
	{
		// output the new block
		if (blkstack_ptr > 0) printWriter.print("\n");
		blkstack[blkstack_ptr++] = keyword;

		// output the keyword
		printWriter.print(getBlanks(blkstack_ptr-1) + "( " + keyword);
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

	/** Method to report that aggregate names (busses) ARE used. */
	protected boolean isAggregateNamesSupported() { return true; }

	/** Method to report whether input and output names are separated. */
	protected boolean isSeparateInputAndOutput() { return true; }
	
	/**
	 * Method to adjust a network name to be safe for EDIF output.
	 */
	protected String getSafeNetName(String name, boolean bus) { return name; }

	/**
	 * Method to obtain Netlist information for a cell.
	 * This is pushed to the writer because each writer may have different requirements for resistor inclusion.
	 * EDIF includes resistors.
	 */
	protected Netlist getNetlistForCell(Cell cell)
	{
		// get network information about this cell
		boolean shortResistors = false;
		Netlist netList = cell.getNetlist(shortResistors);
		return netList;
	}

	/**
	 * Method to tell whether the topological analysis should mangle cell names that are parameterized.
	 */
	protected boolean canParameterizeNames() { return false; }
}
