/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: EDIF.java
 * Input/output tool: EDIF netlist generator
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
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.network.Global;
import com.sun.electric.database.network.JNetwork;
import com.sun.electric.database.network.Netlist;
import com.sun.electric.database.prototype.NodeProto;
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
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.user.User;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

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
	private static final EGraphic EGTEXT = new EGraphic("TEXT");
	private static final EGraphic EGWIRE = new EGraphic("WIRE");
	private static final EGraphic EGBUS = new EGraphic("BUS");

	private EGraphic egraphic = EGUNKNOWN;
	private EGraphic egraphic_override = EGUNKNOWN;

	/**
	 * The main entry point for EDIF deck writing.
	 * @param cell the top-level cell to write.
	 * @param filePath the disk file to create with EDIF.
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
	}

	protected void start()
	{
		// See if schematic view is requested
		double meters_to_lambda = TextUtils.convertDistance(1, Technology.getCurrent(), TextUtils.UnitScale.NONE);

		String name = io_ediftoken(topCell.getName());

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
			System.out.println("Writing footprint for cell " + io_ediftoken(topCell.getName()));
			printWriter.println("(footprint " + TextUtils.formatDouble(route) + "e-06");
			printWriter.println(" (unknownLayoutRep");

			// get standard cell dimensions
			Rectangle2D cellBounds = topCell.getBounds();
			double width = cellBounds.getWidth() / rgrid;
			double height = cellBounds.getHeight() / rgrid;
			printWriter.println("  (" + io_ediftoken(topCell.getName()) + " standard (" +
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
		EO_put_header(topCell, header, "EDIF Writer", topCell.getLibrary().getName());

		// determine the primitives being used
		HashMap useCount = new HashMap();
		if (io_edifsearch(topCell, useCount) != 0)
		{
			// write out all primitives used in the library
			EO_open_block("library");
			EO_put_identifier("lib0");
			EO_put_block("edifLevel", "0");
			EO_open_block("technology");
			EO_open_block("numberDefinition");
			if (IOTool.isEDIFUseSchematicView())
			{
				EO_open_block("scale");
				EO_put_integer(1);
				EO_put_float(meters_to_lambda);
				EO_put_block("unit", "DISTANCE");
				EO_close_block("scale");
			}
			EO_close_block("technology");
			for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
			{
				Technology tech = (Technology)it.next();
				for(Iterator pIt = tech.getNodes(); pIt.hasNext(); )
				{
					NodeProto np = (NodeProto)pIt.next();
					GenMath.MutableInteger mi = (GenMath.MutableInteger)useCount.get(np);
					if (mi == null) continue;

					NodeProto.Function fun = np.getFunction();
					if (fun == NodeProto.Function.UNKNOWN || fun == NodeProto.Function.PIN ||
						fun == NodeProto.Function.CONTACT || fun == NodeProto.Function.NODE ||
						fun == NodeProto.Function.CONNECT || fun == NodeProto.Function.ART) continue;
					for (int i = 0; i < mi.intValue(); i++)
						io_edifwriteprim(np, i, fun);
				}
			}
			EO_close_block("library");
		}

		// now recursively write the cells expanded within the library
		EO_open_block("library");
		EO_put_identifier(io_ediftoken(topCell.getLibrary().getName()));
		EO_put_block("edifLevel", "0");
		EO_open_block("technology");
		EO_open_block("numberDefinition");
		if (IOTool.isEDIFUseSchematicView())
		{
			EO_open_block("scale");
			EO_put_integer(1);
			EO_put_float(meters_to_lambda);
			EO_put_block("unit", "DISTANCE");
			EO_close_block("scale");
		}
		EO_close_block("technology");
	}

	protected void done()
	{
		EO_close_block("library");

		// post-identify the design and library
		EO_open_block("design");
		EO_put_identifier(io_ediftoken(topCell.getName()));
		EO_open_block("cellRef");
		EO_put_identifier(io_ediftoken(topCell.getName()));
		EO_put_block("libraryRef", io_ediftoken(topCell.getLibrary().getName()));

		// clean up
		EO_close_stream();
	}

	/**
	 * Method to write cellGeom
	 */
	protected void writeCellTopology(Cell cell, CellNetInfo cni, VarContext context)
	{
		// write out the cell header information
		EO_open_block("cell");
		EO_put_identifier(io_ediftoken(cell.getName()));
		EO_put_block("cellType", "generic");
		EO_open_block("view");
		EO_put_identifier("cell");
		EO_put_block("viewType", IOTool.isEDIFUseSchematicView() ? "SCHEMATIC" : "NETLIST");

		// write out the interface description
		EO_open_block("interface");

		// write ports and directions
		for(Iterator it = cni.getCellSignals(); it.hasNext(); )
		{
			CellSignal cs = (CellSignal)it.next();
			if (cs.isExported())
			{
				Export e = cs.getExport();
				String direction = "INPUT";
				if (e.getCharacteristic() == PortProto.Characteristic.OUT ||
					e.getCharacteristic() == PortProto.Characteristic.REFOUT) direction = "OUTPUT";
				if (e.getCharacteristic() == PortProto.Characteristic.BIDIR) direction = "INOUT";
				EO_open_block("port");
				EO_put_identifier(io_ediftoken(cs.getName()));
				EO_put_block("direction", direction);
				EO_close_block("port");
			}
		}
		if (IOTool.isEDIFUseSchematicView() && cell.getView() == View.ICON)
		{
			// output the icon
			io_edifsymbol(cell);
		}

		EO_close_block("interface");

		// write cell contents
		Netlist netList = cni.getNetList();
		for(Iterator nIt = netList.getNodables(); nIt.hasNext(); )
		{
			Nodable no = (Nodable)nIt.next();
			if (no.getProto() instanceof PrimitiveNode)
			{
				NodeProto.Function fun = ((NodeInst)no).getFunction();
				if (fun == NodeProto.Function.UNKNOWN || fun == NodeProto.Function.PIN ||
					fun == NodeProto.Function.CONTACT || fun == NodeProto.Function.NODE ||
					fun == NodeProto.Function.CONNECT || fun == NodeProto.Function.ART) continue;
			}

			EO_open_block("instance");
			String iname = io_edifwritecompname(no);
			String oname = no.getName();
			if (!oname.equalsIgnoreCase(iname))
			{
				EO_open_block("rename");
				EO_put_identifier(iname);
				EO_put_string(oname);
				EO_close_block("rename");
			} else EO_put_identifier(iname);

			if (no.getProto() instanceof PrimitiveNode)
			{
				NodeInst ni = (NodeInst)no;
				NodeProto.Function fun = ni.getFunction();
				EO_open_block("viewRef");
				EO_put_identifier("cell");
				EO_open_block("cellRef");
				if (fun == NodeProto.Function.GATEAND || fun == NodeProto.Function.GATEOR || fun == NodeProto.Function.GATEXOR)
				{
					// count the number of inputs
					int i = 0;
					for(Iterator pIt = ni.getConnections(); pIt.hasNext(); )
					{
						Connection con = (Connection)pIt.next();
						if (con.getPortInst().getPortProto().getName().equals("a")) i++;
					}
					String name = io_ediftoken(ni.getProto().getName()) + i;
					EO_put_identifier(name);
				} else EO_put_identifier(io_edifdescribepriminst(ni, fun));
				EO_put_block("libraryRef", "lib0");
				EO_close_block("viewRef");
			} else
			{
//				if (((Cell)ni.getProto()).getView() == View.ICON &&
//						((Cell)ni.getProto()).contentsView() == null)
//				{
//					// this node came from an external schematic library
//					EO_open_block("viewRef");
//					EO_put_identifier("cell");
//					EO_open_block("cellRef");
//					EO_put_identifier(io_ediftoken(ni.getProto().getName()));
//					String name = "schem_lib_" + ni.getProto().getName();
//					EO_put_block("libraryRef", name);
//					EO_close_block("viewRef");
//				} else
				{
					// this node came from this library
					EO_open_block("viewRef");
					EO_put_identifier("cell");
					EO_open_block("cellRef");
					EO_put_identifier(io_ediftoken(no.getProto().getName()));
					EO_put_block("libraryRef", cell.getLibrary().getName());
					EO_close_block("viewRef");
				}
			}

			// now graphical information
			if (IOTool.isEDIFUseSchematicView())
			{
				if (no instanceof NodeInst)
				{
					NodeInst ni = (NodeInst)no;
					EO_open_block("transform");

					// get the orientation (note only support orthogonal)
					EO_put_block("orientation", io_edif_orientation(ni));

					// now the origin
					EO_open_block("origin");
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
					io_edif_pt(pt.getX(), pt.getY());
					EO_close_block("transform");
				}
			}

			// check for variables to write as properties
			if (IOTool.isEDIFUseSchematicView())
			{
				// do all display variables first
				if (no instanceof NodeInst)
				{
					NodeInst ni = (NodeInst)no;
					int num = ni.numDisplayableVariables(false);
					Poly [] varPolys = new Poly[num];
					ni.addDisplayableVariables(ni.getBounds(), varPolys, 0, null, false);
					for(int i=0; i<num; i++)
					{
						Poly varPoly = varPolys[i];
						String name = null;
						Variable var = varPoly.getVariable();
						if (var != null) name = var.getKey().getName(); else
							if (varPoly.getName() != null) name = "NODE_name";
						if (name == null) continue;
						AffineTransform trans = ni.rotateOut();
						varPoly.transform(trans);
						EO_open_block("property");
						EO_put_identifier(name);
						EO_open_block("string");
						io_edifsymbol_showpoly(varPoly);
						EO_close_block("property");
					}
				}
			}
			EO_close_block("instance");
		}

		// if there is anything to connect, write the networks in the cell
		for(Iterator it = cni.getCellSignals(); it.hasNext(); )
		{
			CellSignal cs = (CellSignal)it.next();

			// establish if this is a global net
			boolean globalport = false;
//			if ((pp = (PORTPROTO *) net->temp2) != NOPORTPROTO)
//				globalport = io_edifisglobal(pp);

			EO_open_block("net");
			String netName = cs.getName();
			if (globalport)
			{
				EO_open_block("rename");
				EO_put_identifier(io_ediftoken(netName));
				String name = io_ediftoken(netName) + "!";
				EO_put_identifier(name);
				EO_close_block("rename");
				EO_put_block("property", "GLOBAL");
			} else
			{
				String oname = io_ediftoken(netName);
				if (!oname.equals(netName))
				{
					// different names
					EO_open_block("rename");
					EO_put_identifier(oname);
					EO_put_string(netName);
					EO_close_block("rename");
				} else EO_put_identifier(oname);
			}

			// write net connections
			EO_open_block("joined");

			// include exported ports
			if (cs.isExported())
			{
				Export e = cs.getExport();
				String pt = e.getName();
				EO_put_block("portRef", io_ediftoken(pt));
			}

			JNetwork net = cs.getNetwork();
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
						JNetwork subNet = netList.getNetwork(no, subPp, cs.getExportIndex());
						if (cs != cni.getCellSignal(subNet)) continue;
						EO_open_block("portRef");
						EO_put_identifier(io_ediftoken(subCs.getName()));
						EO_put_block("instanceRef", io_edifwritecompname(no));
						EO_close_block("portRef");
					}
				} else
				{
					NodeInst ni = (NodeInst)no;
					NodeProto.Function fun = ni.getFunction();
					if (fun == NodeProto.Function.UNKNOWN || fun == NodeProto.Function.PIN ||
						fun == NodeProto.Function.CONTACT || fun == NodeProto.Function.NODE ||
						fun == NodeProto.Function.CONNECT || fun == NodeProto.Function.ART) continue;
					for(Iterator cIt = ni.getConnections(); cIt.hasNext(); )
					{
						Connection con = (Connection)cIt.next();
						ArcInst ai = con.getArc();
						JNetwork aNet = netList.getNetwork(ai, 0);
						if (aNet != net) continue;
						String pt = con.getPortInst().getPortProto().getName();
						EO_open_block("portRef");
						EO_put_identifier(io_ediftoken(pt));
						EO_put_block("instanceRef", io_edifwritecompname(no));
						EO_close_block("portRef");
					}
				}
			}
			EO_close_block("joined");

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
					JNetwork aNet = netList.getNetwork(ai, 0);
					if (aNet == net) io_edifsymbol_arcinst(ai, GenMath.MATID);
				}
				io_edifsetgraphic(EGUNKNOWN);
				egraphic_override = EGUNKNOWN;
			}

			if (globalport)
				EO_put_block("userData", "global");
			EO_close_block("net");
		}

		// write busses
		for(Iterator it = cni.getCellAggregateSignals(); it.hasNext(); )
		{
			CellAggregateSignal cas = (CellAggregateSignal)it.next();

			// ignore single signals
			if (cas.getLowIndex() > cas.getHighIndex()) continue;

			EO_open_block("netBundle");
			String busName = cas.getNameWithIndices();
			String oname = io_ediftoken(busName);
			if (!oname.equals(busName))
			{
				// different names
				EO_open_block("rename");
				EO_put_identifier(oname);
				EO_put_string(busName);
				EO_close_block("rename");
			} else EO_put_identifier(oname);
			EO_open_block("listOfNets");

			// now each sub-net name
			int numSignals = cas.getHighIndex() - cas.getLowIndex() + 1;
			for (int k=0; k<numSignals; k++)
			{
				EO_open_block("net");

				// now output this name
				CellSignal cs = cas.getSignal(k);
				String pt = cs.getName();
				oname = io_ediftoken(pt);
				if (!oname.equals(pt))
				{
					// different names
					EO_open_block("rename");
					EO_put_identifier(oname);
					EO_put_string(pt);
					EO_close_block("rename");
				} else EO_put_identifier(oname);
				EO_close_block("net");
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
					if (arcBusName.equals(busName)) io_edifsymbol_arcinst(ai, GenMath.MATID);
				}
				io_edifsetgraphic(EGUNKNOWN);
				egraphic_override = EGUNKNOWN;
			}

			EO_close_block("netBundle");
			continue;
		}

		// matches "(cell "
		EO_close_block("cell");
	}

	/****************************** MIDDLE-LEVEL HELPER METHODS ******************************/

	private void EO_put_header(Cell cell, String program, String comment, String origin)
	{
		// output the standard EDIF 2 0 0 header
		EO_open_block("edif");
		EO_put_identifier(cell.getName());
		EO_put_block("edifVersion", "2 0 0");
		EO_put_block("edifLevel", "0");
		EO_open_block("keywordMap");
		EO_put_block("keywordLevel", "0");		// was "1"
		EO_close_block("keywordMap");
		EO_open_block("status");
		EO_open_block("written");
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy MM dd HH mm ss");
		EO_put_block("timeStamp", simpleDateFormat.format(new Date()));
		if (program != null) EO_put_block("program", "\"" + program + "\"");
		if (comment != null) EO_put_block("comment", "\"" + comment + "\"");
		if (origin != null) EO_put_block("dataOrigin", "\"" + origin + "\"");
		EO_close_block("status");
	}

	/**
	 * Method to dump the description of primitive "np" to the EDIF file
	 * If the primitive is a schematic gate, use "i" as the number of inputs
	 */
	private void io_edifwriteprim(NodeProto np, int i, NodeProto.Function fun)
	{
		// write primitive name
		if (fun == NodeProto.Function.GATEAND || fun == NodeProto.Function.GATEOR || fun == NodeProto.Function.GATEXOR)
		{
			EO_open_block("cell");
			String name = io_ediftoken(np.getName()) + i;
			EO_put_identifier(name);
		} else
		{
			EO_open_block("cell");
			EO_put_identifier(io_ediftoken(np.getName()));
		}

		// write primitive connections
		EO_put_block("cellType", "GENERIC");
		EO_open_block("view");
		EO_put_identifier("cell");
		EO_put_block("viewType", IOTool.isEDIFUseSchematicView() ? "SCHEMATIC" : "NETLIST");
		EO_open_block("interface");

		int firstPortIndex = 0;
		if (fun == NodeProto.Function.GATEAND || fun == NodeProto.Function.GATEOR || fun == NodeProto.Function.GATEXOR)
		{
			for (int j = 0; j < i; j++)
			{
				EO_open_block("port");
				String name = "IN" + (j + 1);
				EO_put_identifier(name);
				EO_put_block("direction", "INPUT");
				EO_close_block("port");
			}
			firstPortIndex = 1;
		}
		for(int k=firstPortIndex; k<np.getNumPorts(); k++)
		{
			PortProto pp = np.getPort(k);
			String direction = "input";
			if (pp.getCharacteristic() == PortProto.Characteristic.OUT) direction = "output"; else
				if (pp.getCharacteristic() == PortProto.Characteristic.BIDIR) direction = "inout";
			EO_open_block("port");
			EO_put_identifier(io_ediftoken(pp.getName()));
			EO_put_block("direction", direction);
			EO_close_block("port");
		}
		EO_close_block("cell");
	}

	/**
	 * Method to count the usage of primitives hierarchically below cell "np"
	 */
	private int io_edifsearch(Cell cell, HashMap useMap)
	{
		// do not search this cell if it is an icon
		if (cell.getView() == View.ICON) return 0;

		// keep a count of the total number of primitives encountered
		int primcount = 0;

		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			NodeProto np = ni.getProto();
			if (np instanceof PrimitiveNode)
			{
				NodeProto.Function fun = ni.getFunction();
				int i = 1;
				if (fun == NodeProto.Function.GATEAND || fun == NodeProto.Function.GATEOR || fun == NodeProto.Function.GATEXOR)
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
	
				if (fun != NodeProto.Function.UNKNOWN && fun != NodeProto.Function.PIN && fun != NodeProto.Function.CONTACT &&
					fun != NodeProto.Function.NODE && fun != NodeProto.Function.CONNECT && fun != NodeProto.Function.METER &&
						fun != NodeProto.Function.CONPOWER && fun != NodeProto.Function.CONGROUND && fun != NodeProto.Function.SOURCE &&
							fun != NodeProto.Function.SUBSTRATE && fun != NodeProto.Function.WELL && fun != NodeProto.Function.ART)
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
			if (count == null) primcount += io_edifsearch(oNp, useMap);
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

	/**
	 * Method to generate a pt symbol (pt x y)
	 */
	private void io_edif_pt(double x, double y)
	{
		EO_open_block("pt");
		EO_put_integer(io_edif_scale(x));
		EO_put_integer(io_edif_scale(y));
		EO_close_block("pt");
	}

	/**
	 * Method to map Electric orientations to EDIF orientations
	 */
	private String io_edif_orientation(NodeInst ni)
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
	private double io_edif_scale(double val)
	{
		return val;
	}

	/**
	 * Establish whether port 'e' is a global port or not
	 */
	private boolean io_edifisglobal(Export e)
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
	private String io_edifdescribepriminst(NodeInst ni, NodeProto.Function fun)
	{
		if (fun == NodeProto.Function.RESIST) return "Resistor";
		if (fun == NodeProto.Function.TRANPN) return "npn";
		if (fun == NodeProto.Function.TRAPNP) return "pnp";
		if (fun == NodeProto.Function.SUBSTRATE) return "gtap";
		return io_ediftoken(ni.getProto().getName());
	}

	/**
	 * Helper name builder
	 */
	private String io_edifwritecompname(Nodable no)
	{
		String okname = io_edifvalidname(no.getName());
		if (okname.length() > 0)
		{
			char chr = okname.charAt(0);
			if (Character.isDigit(chr) || chr == '_')
				okname = "&" + okname;
		}
		return okname;
	}

	/**
	 * Method to return null if there is no valid name in "var", corrected name if valid.
	 */
	private String io_edifvalidname(String name)
	{
		StringBuffer iptr = new StringBuffer(name);

		// allow '&' for the first character (this must be fixed later if digit or '_')
		int i = 0;
		if (iptr.charAt(i) == '&') i++;
	
		// allow "_" and alphanumeric for others
		for(; i<iptr.length(); i++)
		{
			if (Character.isLetterOrDigit(iptr.charAt(i))) continue;
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
	private String io_ediftoken(String str)
	{
		if (str.length() == 0) return str;
		StringBuffer sb = new StringBuffer();
		if (Character.isDigit(str.charAt(0))) sb.append('X');
		for(int i=0; i<str.length(); i++)
		{
			char chr = str.charAt(i);
			if (Character.isWhitespace(chr)) break;
			if (chr == '[') chr = '_';
			if (Character.isLetterOrDigit(chr) || chr == '&' || chr == '_')
				sb.append(chr);
		}
		return sb.toString();
	}

	/****************************** GRAPHIC OUTPUT METHODS ******************************/

	/**
	 * Method to output all graphic objects of a symbol.
	 */
	private void io_edifsymbol(Cell cell)
	{
		EO_open_block("symbol");
		egraphic_override = EGWIRE;
		egraphic = EGUNKNOWN;
		for(Iterator it = cell.getPorts(); it.hasNext(); )
		{
			Export e = (Export)it.next();
			EO_open_block("portImplementation");
			EO_put_identifier(io_ediftoken(e.getName()));
			EO_open_block("connectLocation");
			Poly portPoly = e.getOriginalPort().getPoly();
			io_edifsymbol_showpoly(portPoly);
	
			// close figure
			io_edifsetgraphic(EGUNKNOWN);
			EO_close_block("portImplementation");
		}
		egraphic_override = EGUNKNOWN;

		for(Iterator it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			io_edifsymbol_cell(ni, GenMath.MATID);
		}
		for(Iterator it = cell.getArcs(); it.hasNext(); )
		{
			ArcInst ai = (ArcInst)it.next();
			io_edifsymbol_arcinst(ai, GenMath.MATID);
		}

		// close figure
		io_edifsetgraphic(EGUNKNOWN);
		EO_close_block("symbol");
	}

	/**
	 * Method to output a specific symbol cell
	 */
	private void io_edifsymbol_cell(NodeInst ni, AffineTransform prevtrans)
	{
		// make transformation matrix within the current nodeinst
		if (ni.getAngle() == 0 && !ni.isMirroredAboutXAxis() && !ni.isMirroredAboutYAxis())
		{
			io_edifsymbol_nodeinst(ni, prevtrans);
		} else
		{
			AffineTransform localtran = ni.rotateOut(prevtrans);
			io_edifsymbol_nodeinst(ni, localtran);
		}
	}

	/**
	 * Method to symbol "ni" when transformed through "prevtrans".
	 */
	private void io_edifsymbol_nodeinst(NodeInst ni, AffineTransform prevtrans)
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
					io_edifsetgraphic(EGUNKNOWN);
					EO_open_block("annotate");
				}

				io_edifsymbol_showpoly(poly);
				if (istext) EO_close_block("annotate");
			}
		} else
		{
			// transform into the nodeinst for display of its guts
			Cell subCell = (Cell)np;
			AffineTransform subrot = ni.translateOut(prevtrans);

			// see if there are displayable variables on the cell
			int num = ni.numDisplayableVariables(false);
			if (num != 0)
				io_edifsetgraphic(EGUNKNOWN);
			Poly [] varPolys = new Poly[num];
			ni.addDisplayableVariables(ni.getBounds(), varPolys, 0, null, false);
			for(int i=0; i<num; i++)
			{
				Poly varPoly = varPolys[i];
				String name = null;
				Variable var = varPoly.getVariable();
				if (var != null) name = var.getKey().getName(); else
					if (varPoly.getName() != null) name = "NODE_name";
				if (name == null) continue;
				AffineTransform trans = ni.rotateOut();
				varPoly.transform(prevtrans);
				EO_open_block("property");
				EO_put_identifier(name);
				EO_open_block("string");
				io_edifsymbol_showpoly(varPoly);
				EO_close_block("property");
			}

			// search through cell
			for(Iterator it = subCell.getNodes(); it.hasNext(); )
			{
				NodeInst sNi = (NodeInst)it.next();
				io_edifsymbol_cell(sNi, subrot);
			}
			for(Iterator it = subCell.getArcs(); it.hasNext(); )
			{
				ArcInst sAi = (ArcInst)it.next();
				io_edifsymbol_arcinst(sAi, subrot);
			}
		}
	}

	/**
	 * Method to draw an arcinst.  Returns indicator of what else needs to
	 * be drawn.  Returns negative if display interrupted
	 */
	private void io_edifsymbol_arcinst(ArcInst ai, AffineTransform trans)
	{
		Technology tech = ai.getProto().getTechnology();
		Poly [] polys = tech.getShapeOfArc(ai);

		// get the endpoints of the arcinst
		Point2D [] points = new Point2D[2];
		points[0] = ai.getTail().getLocation();
		points[1] = ai.getHead().getLocation();
		Poly poly = new Poly(points);
		poly.setStyle(Poly.Type.OPENED);
		poly.transform(trans);
		io_edifsymbol_showpoly(poly);

		// now get the variables
		int num = ai.numDisplayableVariables(false);
		if (num != 0)
			io_edifsetgraphic(EGUNKNOWN);
		Poly [] varPolys = new Poly[num];
		ai.addDisplayableVariables(ai.getBounds(), varPolys, 0, null, false);
		for(int i=0; i<num; i++)
		{
			Poly varPoly = varPolys[i];
			String name = null;
			Variable var = varPoly.getVariable();
			if (var != null) name = var.getKey().getName(); else
				if (varPoly.getName() != null) name = "ARC_name";
			if (name == null) continue;
			poly.transform(trans);
			EO_open_block("property");
			EO_put_identifier(name);
			EO_open_block("string");
			io_edifsymbol_showpoly(varPoly);
			EO_close_block("property");
		}
	}

	private void io_edifsetgraphic(EGraphic type)
	{
		if (type == EGUNKNOWN)
		{
			// terminate the figure
			if (egraphic != EGUNKNOWN) EO_close_block("figure");
			egraphic = EGUNKNOWN;
		} else if (egraphic_override == EGUNKNOWN)
		{
			// normal case
			if (type != egraphic)
			{
				// new egraphic type
				if (egraphic != EGUNKNOWN) EO_close_block("figure");
				egraphic = type;
				EO_open_block("figure");
				EO_put_identifier(egraphic.getText());
			}
		} else if (egraphic != egraphic_override)
		{
			// override figure
			if (egraphic != EGUNKNOWN) EO_close_block("figure");
			egraphic = egraphic_override;
			EO_open_block("figure");
			EO_put_identifier(egraphic.getText());
		}
	}

	/**
	 * Method to write polys into EDIF syntax
	 */
	private void io_edifsymbol_showpoly(Poly obj)
	{
		// now draw the polygon
		Poly.Type type = obj.getStyle();
		Point2D [] points = obj.getPoints();
		if (type == Poly.Type.CIRCLE || type == Poly.Type.DISC)
		{
			io_edifsetgraphic(EGART);
			double i = points[0].distance(points[1]);
			EO_open_block("circle");
			io_edif_pt(points[0].getX() - i, points[0].getY());
			io_edif_pt(points[0].getX() + i, points[0].getY());
			EO_close_block("circle");
			return;
		}

		if (type == Poly.Type.CIRCLEARC)
		{
			io_edifsetgraphic(EGART);

			// arcs at [i] points [1+i] [2+i] clockwise
			if (points.length == 0) return;
			if ((points.length % 3) != 0) return;
			for (int i = 0; i < points.length; i += 3)
			{
				EO_open_block("openShape");
				EO_open_block("curve");
				EO_open_block("arc");
				io_edif_pt(points[i + 1].getX(), points[i + 1].getY());

				// calculate a point between the first and second point
				Point2D si = io_compute_center(points[i], points[i + 1], points[i + 2]);
				io_edif_pt(si.getX(), si.getY());
				io_edif_pt(points[i + 2].getX(), points[i + 2].getY());
				EO_close_block("openShape");
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
					io_edifsetgraphic(EGART);
					EO_open_block("dot");
					io_edif_pt(bounds.getCenterX(), bounds.getCenterY());
					EO_close_block("dot");
				} else
				{
					io_edifsetgraphic(EGART);
					EO_open_block("rectangle");
					io_edif_pt(bounds.getMinX(), bounds.getMinY());
					io_edif_pt(bounds.getMaxY(), bounds.getMaxY());
					EO_close_block("rectangle");
				}
			} else
			{
				io_edifsetgraphic(EGART);
				EO_open_block("path");
				EO_open_block("pointList");
				for (int i = 0; i < points.length; i++)
					io_edif_pt(points[i].getX(), points[i].getY());
				if (points.length > 2) io_edif_pt(points[0].getX(), points[0].getY());
				EO_close_block("path");
			}
			return;
		}
		if (type.isText())
		{	
			Rectangle2D bounds = obj.getBounds2D();
			io_edifsetgraphic(EGUNKNOWN);
			EO_open_block("stringDisplay");
			EO_put_string(obj.getString());
			EO_open_block("display");
			TextDescriptor td = obj.getTextDescriptor();
			if (td.getSize().isAbsolute())
			{
				EO_open_block("figureGroupOverride");
				EO_put_identifier(EGART.getText());

				// output the text height
				EO_open_block("textHeight");

				// 2 pixels = 0.0278 in or 36 double pixels per inch
				double height = td.getSize().getSize() * 10 / 36;
				EO_put_integer(io_edif_scale(height));
				EO_close_block("figureGroupOverride");
			} else EO_put_identifier(EGART.getText());
			if (type == Poly.Type.TEXTCENT) EO_put_block("justify", "CENTERCENTER"); else
			if (type == Poly.Type.TEXTTOP) EO_put_block("justify", "LOWERCENTER"); else
			if (type == Poly.Type.TEXTBOT) EO_put_block("justify", "UPPERCENTER"); else
			if (type == Poly.Type.TEXTLEFT) EO_put_block("justify", "CENTERRIGHT"); else
			if (type == Poly.Type.TEXTRIGHT) EO_put_block("justify", "CENTERLEFT"); else
			if (type == Poly.Type.TEXTTOPLEFT) EO_put_block("justify", "LOWERRIGHT"); else
			if (type == Poly.Type.TEXTBOTLEFT) EO_put_block("justify", "UPPERRIGHT"); else
			if (type == Poly.Type.TEXTTOPRIGHT) EO_put_block("justify", "LOWERLEFT"); else
			if (type == Poly.Type.TEXTBOTRIGHT) EO_put_block("justify", "UPPERLEFT");
			EO_put_block("orientation", "R0");
			EO_open_block("origin");
			io_edif_pt(bounds.getMinX(), bounds.getMinY());
			EO_close_block("stringDisplay");
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
						io_edifsetgraphic(EGART);
						EO_open_block("dot");
						io_edif_pt(bounds.getCenterX(), bounds.getCenterY());
						EO_close_block("dot");
					} else
					{
						io_edifsetgraphic(EGART);
						EO_open_block("rectangle");
						io_edif_pt(bounds.getMinX(), bounds.getMinY());
						io_edif_pt(bounds.getMaxX(), bounds.getMaxY());
						EO_close_block("rectangle");
					}
					return;
				}
			}
			io_edifsetgraphic(EGART);
			EO_open_block("path");
			EO_open_block("pointList");
			for (int i = 0; i < points.length; i++)
				io_edif_pt(points[i].getX(), points[i].getY());
			EO_close_block("path");
			return;
		}
		if (type == Poly.Type.VECTORS)
		{
			io_edifsetgraphic(EGART);
			for (int i = 0; i < points.length; i += 2)
			{
				EO_open_block("path");
				EO_open_block("pointList");
				io_edif_pt(points[i].getX(), points[i].getY());
				io_edif_pt(points[i + 1].getX(), points[i + 1].getY());
				EO_close_block("path");
			}
			return;
		}
	}

	/**
	 * Method used by "ioedifo.c" and "routmaze.c".
	 */
	private Point2D io_compute_center(Point2D c, Point2D p1, Point2D p2)
	{
		// reconstruct angles to p1 and p2
		double radius = p1.distance(c);
		double a1 = io_calc_angle(radius, p1.getX() - c.getX(), p1.getY() - c.getY());
		double a2 = io_calc_angle(radius, p2.getX() - c.getX(), p1.getY() - c.getY());
		if (a1 < a2) a1 += 3600;
		double a = (a1 + a2) / 2;
		double theta = a * Math.PI / 1800.0;	/* in radians */
		return new Point2D.Double(c.getX() + radius * Math.cos(theta), c.getY() + radius * Math.sin(theta));
	}

	private double io_calc_angle(double r, double dx, double dy)
	{
		double ratio, a1, a2;

		ratio = 1800.0 / Math.PI;
		a1 = Math.acos(dx/r) * ratio;
		a2 = Math.asin(dy/r) * ratio;
		if (a2 < 0.0) return 3600.0 - a1;
		return a1;
	}

	/****************************** LOW-LEVEL BLOCK OUTPUT METHODS ******************************/

	/**
	 * Will open a new keyword block, will indent the new block
	 * depending on depth of the keyword
	 */
	private void EO_open_block(String keyword)
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
	private void EO_put_block(String keyword, String identifier)
	{
		// output the new block
		if (blkstack_ptr != 0) printWriter.print("\n");

		// output the keyword
		printWriter.print(getBlanks(blkstack_ptr) + "( " + keyword + " " + identifier + " )");
	}

	/**
	 * Will output a string identifier to the file
	 */
	private void EO_put_identifier(String str)
	{
		printWriter.print(" " + str);
	}

	/**
	 * Will output a quoted string to the file
	 */
	private void EO_put_string(String str)
	{
		printWriter.print(" \"" + str + "\"");
	}

	/**
	 * Will output an integer to the edif file
	 */
	private void EO_put_integer(double val)
	{
		printWriter.print(" " + TextUtils.formatDouble(val));
	}

	/**
	 * Will output a floating value to the edif file
	 */
	private static DecimalFormat decimalFormatScientific = null;
	private void EO_put_float(double val)
	{
		if (decimalFormatScientific == null)
		{
			decimalFormatScientific = new DecimalFormat("0.#####E0");
			decimalFormatScientific.setGroupingUsed(false);
		}
		String ret = decimalFormatScientific.format(val).replace('E', ' ');
		printWriter.print(" ( e " + ret + " )");
	}

	private void EO_close_block(String keyword)
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
	private void EO_close_stream()
	{
		if (blkstack_ptr > 0)
		{
			EO_close_block(blkstack[0]);
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
	protected String getPowerName() { return "VDD"; }

	/** Method to return the proper name of Ground */
	protected String getGroundName() { return "GND"; }

	/** Method to return the proper name of a Global signal */
	protected String getGlobalName(Global glob) { return glob.getName(); }

	/** Method to report that export names DO take precedence over
	 * arc names when determining the name of the network. */
	protected boolean isNetworksUseExportedNames() { return true; }

	/** Method to report that library names ARE always prepended to cell names. */
	protected boolean isLibraryNameAlwaysAddedToCellName() { return false; }

	/** Method to report that aggregate names (busses) ARE used. */
	protected boolean isAggregateNamesSupported() { return true; }

	/**
	 * Method to adjust a network name to be safe for EDIF output.
	 */
	protected String getSafeNetName(String name) { return name; }

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
	protected boolean canParameterizeNames() { return true; }
}
