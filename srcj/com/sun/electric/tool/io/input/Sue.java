/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: Sue.java
* Input/output tool: Sue input
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
package com.sun.electric.tool.io.input;

import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.io.IOTool;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * This class reads files in Sue files.
 */
public class Sue extends Input
{
	/*************** SUE EQUIVALENCES ***************/
	
	private static class SueExtraWire
	{
		String   portname;
		double  xoffset;
		double  yoffset;

		private SueExtraWire(String portname, double xoffset, double yoffset)
		{
			this.portname = portname;
			this.xoffset = xoffset;
			this.yoffset = yoffset;
		}
	};
	
	private SueExtraWire [] io_suetransistorwires =
	{
		new SueExtraWire("d",  3, 0),
		new SueExtraWire("s", -3, 0),
		new SueExtraWire("g",  0, 4.5)
	};
	
	private SueExtraWire [] io_suetransistor4wires =
	{
		new SueExtraWire("d",  3,     0),
		new SueExtraWire("s", -3,     0),
		new SueExtraWire("b", -0.25, -2.5),
		new SueExtraWire("g",  0,     4.5)
	};
	
	private SueExtraWire [] io_sueresistorwires =
	{
		new SueExtraWire("a", -3, 0),
		new SueExtraWire("b",  3, 0)
	};
	
	private SueExtraWire [] io_suecapacitorwires =
	{
		new SueExtraWire("a",  0,  1.75),
		new SueExtraWire("b",  0, -1.75)
	};
	
	private SueExtraWire [] io_suesourcewires =
	{
		new SueExtraWire("minus", 0, -1.25),
		new SueExtraWire("plus",  0,  1.5)
	};
	
	private SueExtraWire [] io_suetwoportwires =
	{
		new SueExtraWire("a", -11.25,  3.625),
		new SueExtraWire("b", -11.25, -3.625),
		new SueExtraWire("x",  11.25,  3.625),
		new SueExtraWire("y",  11.25, -3.625)
	};
	
	private static class SueEquiv
	{
		String         suename;
		NodeProto   intproto;
		boolean        netateoutput;
		int        rotation;
		boolean        transpose;
		double        xoffset;
		double        yoffset;
		PrimitiveNode.Function        detailbits;
		SueExtraWire [] extrawires;

		private SueEquiv(String suename, NodeProto intproto, boolean netateoutput, int rotation, boolean transpose,
			double xoffset, double yoffset, PrimitiveNode.Function detailbits, SueExtraWire [] extrawires)
		{
			this.suename = suename;
			this.intproto = intproto;
			this.netateoutput = netateoutput;
			this.rotation = rotation;
			this.transpose = transpose;
			this.xoffset = xoffset;
			this.yoffset = yoffset;
			this.detailbits = detailbits;
			this.extrawires = extrawires;
		}
	};

	SueEquiv [] io_sueequivs =
	{
		//            name         primitive                        NEG     ANG       X     Y      FUNCTION                        EXTRA-WIRES
		new SueEquiv("pmos10",     Schematics.tech.transistorNode, false, 900,false, -2,    0,     PrimitiveNode.Function.TRAPMOS, io_suetransistorwires),
		new SueEquiv("nmos10",     Schematics.tech.transistorNode, false, 900,false, -2,    0,     PrimitiveNode.Function.TRANMOS, io_suetransistorwires),
		new SueEquiv("pmos4",      Schematics.tech.transistorNode, false, 900,false, -2,    0,     PrimitiveNode.Function.TRAPMOS, io_suetransistorwires),
		new SueEquiv("nmos4",      Schematics.tech.transistorNode, false, 900,false, -2,    0,     PrimitiveNode.Function.TRANMOS, io_suetransistorwires),
		new SueEquiv("pmos",       Schematics.tech.transistorNode, false, 900,false, -2,    0,     PrimitiveNode.Function.TRAPMOS, io_suetransistorwires),
		new SueEquiv("nmos",       Schematics.tech.transistorNode, false, 900,false, -2,    0,     PrimitiveNode.Function.TRANMOS, io_suetransistorwires),
		new SueEquiv("capacitor",  Schematics.tech.capacitorNode,  false,   0,false,  0,    0,     null,                           io_suecapacitorwires),
		new SueEquiv("resistor",   Schematics.tech.resistorNode,   false, 900,false,  0,    0,     null,                           io_sueresistorwires),
		new SueEquiv("inductor",   Schematics.tech.inductorNode,   false,   0,false,  0,    0,     null,                           null),
		new SueEquiv("cccs",       Schematics.tech.twoportNode,    false,   0,false,  1.25,-6.875, PrimitiveNode.Function.CCCS,    io_suetwoportwires),
		new SueEquiv("ccvs",       Schematics.tech.twoportNode,    false,   0,false,  1.25,-6.875, PrimitiveNode.Function.CCVS,    io_suetwoportwires),
		new SueEquiv("vcvs",       Schematics.tech.twoportNode,    false,   0,false,  1.25,-6.875, PrimitiveNode.Function.VCVS,    io_suetwoportwires),
		new SueEquiv("vccs",       Schematics.tech.twoportNode,    false,   0,false, -1.875,-5,    PrimitiveNode.Function.VCCS,    null)
	};

	SueEquiv [] io_sueequivs4 =
	{
		//            name         primitive                         NEG     ANG       X     Y      FUNCTION                        EXTRA-WIRES
		new SueEquiv("pmos10",     Schematics.tech.transistor4Node, false,   0,true,  -2,    0,     PrimitiveNode.Function.TRAPMOS, io_suetransistor4wires),
		new SueEquiv("nmos10",     Schematics.tech.transistor4Node, false, 900,false, -2,    0,     PrimitiveNode.Function.TRANMOS, io_suetransistor4wires),
		new SueEquiv("pmos4",      Schematics.tech.transistor4Node, false,   0,true,  -2,    0,     PrimitiveNode.Function.TRAPMOS, io_suetransistor4wires),
		new SueEquiv("nmos4",      Schematics.tech.transistor4Node, false, 900,false, -2,    0,     PrimitiveNode.Function.TRANMOS, io_suetransistor4wires),
		new SueEquiv("pmos",       Schematics.tech.transistor4Node, false,   0,true,  -2,    0,     PrimitiveNode.Function.TRAPMOS, io_suetransistor4wires),
		new SueEquiv("nmos",       Schematics.tech.transistor4Node, false, 900,false, -2,    0,     PrimitiveNode.Function.TRANMOS, io_suetransistor4wires),
		new SueEquiv("capacitor",  Schematics.tech.capacitorNode,   false,   0,false,  0,    0,     null,                           io_suecapacitorwires),
		new SueEquiv("resistor",   Schematics.tech.resistorNode,    false, 900,false,  0,    0,     null,                           io_sueresistorwires),
		new SueEquiv("inductor",   Schematics.tech.inductorNode,    false,   0,false,  0,    0,     null,                           null),
		new SueEquiv("cccs",       Schematics.tech.twoportNode,     false,   0,false,  1.25,-6.875, PrimitiveNode.Function.CCCS,    io_suetwoportwires),
		new SueEquiv("ccvs",       Schematics.tech.twoportNode,     false,   0,false,  1.25,-6.875, PrimitiveNode.Function.CCVS,    io_suetwoportwires),
		new SueEquiv("vcvs",       Schematics.tech.twoportNode,     false,   0,false,  1.25,-6.875, PrimitiveNode.Function.VCVS,    io_suetwoportwires),
		new SueEquiv("vccs",       Schematics.tech.twoportNode,     false,   0,false, -1.875,-5,    PrimitiveNode.Function.VCCS,    null)
	};

	/*************** SUE WIRES ***************/
	
	private static class SueWire
	{
		Point2D  [] pt;
		PortInst [] pi;
		ArcProto    proto;

		private SueWire()
		{
			pt = new Point2D[2];
			pi = new PortInst[2];
		}
	};
	
	/*************** SUE NETWORKS ***************/
	
	private static class SueNet
	{
		Point2D  pt;
		String   label;
	};

//	/*************** MISCELLANEOUS ***************/
//	
//	#define MAXLINE       300			/* maximum characters on an input line */

	private String   io_suelastline;
	private String   io_sueorigline;
	private String   io_suecurline;
	private List io_suedirectories;

	/**
	 * Method to import a library from disk.
	 * @param lib the library to fill
	 * @return true on error.
	 */
	protected boolean importALibrary(Library lib)
	{
		// determine the cell name
		String cellName = lib.getName();
	
		// initialize the number of directories that need to be searched
		io_suedirectories = new ArrayList();
	
		// determine the current directory
		String topdirname = TextUtils.getFilePath(lib.getLibFile());
		io_suedirectories.add(topdirname);
	
//		// find all subdirectories that start with "suelib_" and include them in the search
//		filecount = filesindirectory(topdirname, &filelist);
//		for(i=0; i<filecount; i++)
//		{
//			if (namesamen(filelist[i], x_("suelib_"), 7) != 0) continue;
//			estrcpy(dirname, topdirname);
//			estrcat(dirname, filelist[i]);
//			if (fileexistence(dirname) != 2) continue;
//			estrcat(dirname, DIRSEPSTR);
//			io_suedirectories.add(dirname);
//		}
//	
//		// see if the current directory is inside of a SUELIB
//		len = estrlen(topdirname);
//		for(i = len-2; i>0; i--)
//			if (topdirname[i] == DIRSEP) break;
//		i++;
//		if (namesamen(&topdirname[i], x_("suelib_"), 7) == 0)
//		{
//			topdirname[i] = 0;
//			filecount = filesindirectory(topdirname, &filelist);
//			for(i=0; i<filecount; i++)
//			{
//				if (namesamen(filelist[i], x_("suelib_"), 7) != 0) continue;
//				estrcpy(dirname, topdirname);
//				estrcat(dirname, filelist[i]);
//				if (fileexistence(dirname) != 2) continue;
//				estrcat(dirname, DIRSEPSTR);
//				io_suedirectories.add(dirname);
//			}
//		}
	
		// read the file
		try
		{
			Cell topCell = io_suereadfile(lib, cellName);
			if (topCell != null)
				lib.setCurCell(topCell);
		} catch (IOException e)
		{
			System.out.println("ERROR reading Sue libraries");
		}
	
		return false;
	}
	
	/**
	 * Method to read the SUE file in "f"
	 */
	private Cell io_suereadfile(Library lib, String cellName)
		throws IOException
	{
		boolean placeIcon = false;
		List sueWires = new ArrayList();
		List sueNets = new ArrayList();
		Cell cell = null;
		Cell schemCell = null;
		Cell iconCell = null;
		io_suelastline = null;
		Point2D iconPt = null;
//		numargs = 0;
		for(;;)
		{
			// get the next line of text
			List keywords = io_suegetnextline(0);
			if (keywords == null) break;
			int count = keywords.size();
			if (count == 0) continue;
			String keyword0 = (String)keywords.get(0);

			// handle "proc" for defining views
			if (keyword0.equalsIgnoreCase("proc"))
			{
				// write any wires from the last proc
				if (cell != null)
				{
					io_sueplacewires(sueWires, sueNets, cell);
					io_sueplacenets(sueNets, cell);
					sueWires = new ArrayList();
					sueNets = new ArrayList();
				}
	
				if (count < 2)
				{
					System.out.println("Cell " + cellName + ", line " + lineReader.getLineNumber() +
						": 'proc' is missing arguments: " + io_sueorigline);
					continue;
				}

				String keyword1 = (String)keywords.get(1);
				if (keyword1.startsWith("SCHEMATIC_"))
				{
					// create the schematic cell
					String subCellName = keyword1.substring(10);
					if (subCellName.equalsIgnoreCase("[get_file_name]"))
						subCellName = cellName;
					subCellName += "{sch}";
					schemCell = cell = Cell.makeInstance(lib, subCellName);
					placeIcon = false;
				} else if (keyword1.startsWith("ICON_"))
				{
					// create the icon cell
					String subCellName = keyword1.substring(5);
					if (subCellName.equalsIgnoreCase("[get_file_name]"))
						subCellName = cellName;
					subCellName += "{ic}";
					iconCell = cell = Cell.makeInstance(lib, subCellName);
				} else
				{
					System.out.println("Cell " + cellName + ", line " + lineReader.getLineNumber() +
						": unknown 'proc' statement: " + io_sueorigline);
				}
				continue;
			}
	
			// handle "make" for defining components
			if (keyword0.equalsIgnoreCase("make"))
			{
				if (count < 2)
				{
					System.out.println("Cell " + cellName + ", line " + lineReader.getLineNumber() +
						": 'make' is missing arguments: " + io_sueorigline);
					continue;
				}

				// extract parameters
				ParseParameters parP = new ParseParameters(keywords, 2);

				// save the name string
				String theName = parP.theName;
	
				// ignore self-references
				String keyword1 = (String)keywords.get(1);
				if (keyword1.equalsIgnoreCase(cellName))
				{
					if (parP.pt.getX() != 0 || parP.pt.getY() != 0)
					{
						// queue icon placement
						iconPt = parP.pt;
						placeIcon = true;
					}
					continue;
				}
	
				// special case for network names: queue them
				if (keyword1.equalsIgnoreCase("name_net_m") ||
					keyword1.equalsIgnoreCase("name_net_s") ||
					keyword1.equalsIgnoreCase("name_net"))
				{
					SueNet sn = new SueNet();
					sn.pt = parP.pt;
					sn.label = parP.theName;
					sueNets.add(sn);
					continue;
				}
	
				// first check for special names
				NodeProto proto = null;
				double xoff = 0, yoff = 0;
				PortCharacteristic type = PortCharacteristic.UNKNOWN;
				double xshrink = 0, yshrink = 0;
				boolean invertoutput = false;
				int rotation = 0;
				boolean transpose = false;
				PrimitiveNode.Function detailbits = null;
				SueExtraWire [] extrawires = null;
				if (keyword1.equalsIgnoreCase("inout"))
				{
					proto = Schematics.tech.offpageNode;
					AffineTransform trans = NodeInst.pureRotate(parP.rot, parP.trn);
					Point2D offPt = new Point2D.Double(2, 0);
					trans.transform(offPt, offPt);
					xoff = offPt.getX();   yoff = offPt.getY();
					type = PortCharacteristic.BIDIR;
				} else if (keyword1.equalsIgnoreCase("input"))
				{
					proto = Schematics.tech.offpageNode;
					AffineTransform trans = NodeInst.pureRotate(parP.rot, parP.trn);
					Point2D offPt = new Point2D.Double(-2, 0);
					trans.transform(offPt, offPt);
					xoff = offPt.getX();   yoff = offPt.getY();
					type = PortCharacteristic.IN;
				} else if (keyword1.equalsIgnoreCase("output"))
				{
					proto = Schematics.tech.offpageNode;
					AffineTransform trans = NodeInst.pureRotate(parP.rot, parP.trn);
					Point2D offPt = new Point2D.Double(2, 0);
					trans.transform(offPt, offPt);
					xoff = offPt.getX();   yoff = offPt.getY();
					type = PortCharacteristic.OUT;
				} else if (keyword1.equalsIgnoreCase("rename_net"))
				{
					proto = Schematics.tech.wirePinNode;
				} else if (keyword1.equalsIgnoreCase("global"))
				{
					Name busName = Name.findName(parP.theName);
					int busWidth = busName.busWidth();
					if (busWidth > 1) proto = Schematics.tech.busPinNode; else
					{
						proto = Schematics.tech.wirePinNode;
						if (parP.theName.equalsIgnoreCase("gnd"))
						{
							AffineTransform trans = NodeInst.pureRotate(parP.rot, parP.trn);
							Point2D offPt = new Point2D.Double(-2, 0);
							trans.transform(offPt, offPt);
							xoff = offPt.getX();   yoff = offPt.getY();
							proto = Schematics.tech.groundNode;
							type = PortCharacteristic.GND;
						}
						if (parP.theName.equalsIgnoreCase("vdd"))
						{
							proto = Schematics.tech.powerNode;
							type = PortCharacteristic.PWR;
						}
					}
				} else if (keyword1.equalsIgnoreCase("join_net"))
				{
					proto = Schematics.tech.wireConNode;
					xshrink = -2;
					AffineTransform trans = NodeInst.pureRotate(parP.rot, parP.trn);
					Point2D offPt = new Point2D.Double(1.25, 0);
					trans.transform(offPt, offPt);
					xoff = offPt.getX();   yoff = offPt.getY();
				}
	
				// now check for internal associations to known primitives
				if (proto == null)
				{
					SueEquiv [] curequivs = io_sueequivs;
					if (IOTool.isSueUses4PortTransistors()) curequivs = io_sueequivs4;
					int i = 0;
					for( ; i < curequivs.length; i++)
						if (keyword1.equalsIgnoreCase(curequivs[i].suename)) break;
					if (i < curequivs.length)
					{
						proto = curequivs[i].intproto;
						invertoutput = curequivs[i].netateoutput;
						rotation = curequivs[i].rotation;
						transpose = curequivs[i].transpose;
						AffineTransform trans = NodeInst.pureRotate(parP.rot, parP.trn);
						Point2D offPt = new Point2D.Double(curequivs[i].xoffset, curequivs[i].yoffset);
						trans.transform(offPt, offPt);
						xoff = offPt.getX();   yoff = offPt.getY();
						
						if (transpose)
						{
							parP.trn = !parP.trn;
							parP.rot = rotation - parP.rot;
							if (parP.rot < 0) parP.rot += 3600;
						} else
						{
							parP.rot += rotation;
							if (parP.rot >= 3600) parP.rot -= 3600;
						}
						detailbits = curequivs[i].detailbits;
						extrawires = curequivs[i].extrawires;
					}
				}
	
				// now check for references to cells
				if (proto == null)
				{
					// find node or read it from disk
					proto = io_suegetnodeproto(lib, keyword1);
					if (proto == null)
						proto = io_suereadfromdisk(lib, keyword1);
	
					// set proper offsets for the cell
					if (proto != null)
					{
						Cell np = ((Cell)proto).iconView();
						if (np != null) proto = np;
						Rectangle2D bounds = ((Cell)proto).getBounds();
						AffineTransform trans = NodeInst.pureRotate(parP.rot, parP.trn);
						Point2D offPt = new Point2D.Double(bounds.getCenterX(), bounds.getCenterY());
						trans.transform(offPt, offPt);
						xoff = offPt.getX();   yoff = offPt.getY();
					}
				}
	
				// ignore "title" specifications
				if (keyword1.startsWith("title_")) continue;
	
				// stop now if SUE node is unknown
				if (proto == null)
				{
					System.out.println("Cell " + cellName + ", line " + lineReader.getLineNumber() +
						", cannot create instance of " + keyword1);
					continue;
				}
	
				// create the instance
				double wid = proto.getDefWidth();
				double hei = proto.getDefHeight();
				if (proto instanceof Cell)
				{
					Rectangle2D bounds = ((Cell)proto).getBounds();
					wid = bounds.getWidth();
					hei = bounds.getHeight();
				}
				wid -= xshrink;
				hei -= yshrink;
				if (parP.trn)
				{
					parP.rot = (parP.rot + 900) % 3600;
					hei = -hei;
				}
				NodeInst ni = NodeInst.makeInstance(proto, new Point2D.Double(parP.pt.getX() + xoff, parP.pt.getY() + yoff), wid, hei, cell,
					parP.rot, null, Schematics.getPrimitiveFunctionBits(detailbits));
				if (ni == null) continue;
//				ni->temp1 = invertoutput;
				if (proto instanceof Cell && ((Cell)proto).isIcon())
					ni.setExpanded();
	
				// add any extra wires to the node
				if (extrawires != null)
				{
					for(int i=0; i<extrawires.length; i++)
					{
						PortProto pp = proto.findPortProto(extrawires[i].portname);
						if (pp == null) continue;
						PortInst pi = ni.findPortInstFromProto(pp);
						Poly portPoly = pi.getPoly();
						double x = portPoly.getCenterX();
						double y = portPoly.getCenterY();
						AffineTransform trans = NodeInst.pureRotate(ni.getAngle(), ni.isMirroredAboutXAxis(), ni.isMirroredAboutYAxis());
						double px = extrawires[i].xoffset;
						double py = extrawires[i].yoffset;
						Point2D dPt = new Point2D.Double(px, py);
						trans.transform(dPt, dPt);
						double dx = dPt.getX();
						double dy = dPt.getY();
						PrimitiveNode wirePin = Schematics.tech.wirePinNode;
						px = wirePin.getDefWidth();
						py = wirePin.getDefHeight();
						double pinx = x + dx;
						double piny = y + dy;
						PortInst ppi = io_suefindpinnode(pinx, piny, cell);
						if (ppi == null)
						{
							NodeInst nni = NodeInst.makeInstance(Schematics.tech.wirePinNode, new Point2D.Double(pinx, piny), px, py, cell);
							if (nni == null) continue;
							ppi = nni.getOnlyPortInst();
						}
						ArcInst ai = ArcInst.makeInstance(Schematics.tech.wire_arc, 0, pi, ppi);
						if (ai == null)
						{
							System.out.println("Cell " + cellName + ", line " + lineReader.getLineNumber() +
								", error adding extra wires to node " + keyword1);
							break;
						}
						if (x != pinx && y != piny) ai.setFixedAngle(false);
					}
				}
	
				// handle names assigned to the node
				if (parP.theName != null)
				{
					// export a port if this is an input, output, inout
					if (proto == Schematics.tech.offpageNode && parP.theName != null)
					{
						Iterator it = ni.getPortInsts();
						PortInst pi = (PortInst)it.next();
						if (keyword1.equalsIgnoreCase("output")) pi = (PortInst)it.next();
						Export ppt = io_suenewexport(cell, pi, parP.theName);
						if (ppt == null)
						{
							System.out.println("Cell " + cellName + ", line " + lineReader.getLineNumber() +
								", could not create export " + parP.theName);
						} else
						{
							ppt.setCharacteristic(type);
						}
					} else
					{
						// just name the node
						ni.setName(parP.theName);
					}
				}
	
				// count the variables
				int varcount = 0;
				for(int i=2; i<count; i += 2)
				{
					String keyword = (String)keywords.get(i);
					if (!keyword.startsWith("-")) continue;
					if (keyword.equalsIgnoreCase("-origin") ||
						keyword.equalsIgnoreCase("-orient") ||
						keyword.equalsIgnoreCase("-type") ||
						keyword.equalsIgnoreCase("-name")) continue;
					varcount++;
				}
	
				// add variables
				int varindex = 1;
				double varoffset = ni.getYSize() / (varcount+1);
				for(int i=2; i<count; i += 2)
				{
					String keyword = (String)keywords.get(i);
					if (!keyword.startsWith("-")) continue;
					if (keyword.equalsIgnoreCase("-origin") ||
						keyword.equalsIgnoreCase("-orient") ||
						keyword.equalsIgnoreCase("-type") ||
						keyword.equalsIgnoreCase("-name")) continue;

					boolean varissize = false;
					boolean halvesize = false;
					boolean isparam = false;
					double xpos = 0, ypos = 0;
					String sueVarName = null;
					if (keyword.charAt(1) == 'w')
					{
						sueVarName = "ATTR_width";
						varissize = true;
						xpos = 2;
						ypos = -4;
					} else if (keyword.charAt(1) == 'l')
					{
						sueVarName = "ATTR_length";
						varissize = true;
						xpos = -2;
						ypos = -4;
						halvesize = true;
					} else
					{
						sueVarName = "ATTR_" + keyword.substring(1);
						if (sueVarName.indexOf(' ') >= 0)
						{
							System.out.println("Cell " + cellName + ", line " + lineReader.getLineNumber() +
								", bad variable name: " + sueVarName);
							break;
						}
						xpos = 0;
						ypos = ni.getYSize() / 2 - varindex * varoffset;
						isparam = true;
					}
					Object newaddr = null;
					String pt = (String)keywords.get(i+1);
					if (keyword.charAt(1) == 'W' && keyword.length() > 2)
					{
						newaddr = keyword.substring(2) + ":" + io_sueparseexpression(pt);
					} else
					{
						int len = pt.length() - 1;
						if (Character.toLowerCase(pt.charAt(len)) == 'u')
						{
							pt = pt.substring(0, len-1);
							if (TextUtils.isANumber(pt))
							{
								newaddr = new Double(TextUtils.convertFromDistance(TextUtils.atof(pt), Technology.getCurrent(), TextUtils.UnitScale.MICRO));
							}
							pt += "u";
						}
						if (newaddr == null && TextUtils.isANumber(pt))
						{
							newaddr = new Integer(TextUtils.atoi(pt));
							if (pt.indexOf('.') >= 0 || pt.toLowerCase().indexOf('e') >= 0)
							{
								newaddr = new Double(TextUtils.atof(pt));
							}
						}
						if (newaddr == null)
						{
							newaddr = io_sueparseexpression(pt);
						}
					}

					// see if the string should be Java code
					boolean makeJava = false;
					if (newaddr instanceof String)
					{
						if (((String)newaddr).indexOf('@') >= 0 ||
							((String)newaddr).indexOf("p(") >= 0) makeJava = true;
					}
					Variable var = ni.newVar(sueVarName, newaddr);
					if (var != null)
					{
						var.setDisplay(true);
						if (makeJava) var.setCode(Variable.Code.JAVA);
						varindex++;
						TextDescriptor td = var.getTextDescriptor();
						td.setOff(xpos, ypos);
						if (halvesize)
						{
							if (td.getSize().isAbsolute())
								td.setAbsSize((int)(td.getSize().getSize() / 2)); else
									td.setRelSize(td.getSize().getSize() / 2);
						}
						if (isparam)
						{
							td.setParam(true);
							td.setDispPart(TextDescriptor.DispPos.NAMEVALUE);
	
//							// make sure the parameter exists in the cell definition
//							cnp = contentsview(ni->proto);
//							if (cnp == NONODEPROTO) cnp = ni->proto;
//							var = getval((INTBIG)cnp, VNODEPROTO, -1, sueVarName);
//							if (var == NOVARIABLE)
//							{
//								var = setval((INTBIG)cnp, VNODEPROTO, sueVarName, newaddr,
//									newtype|VDISPLAY);
//								if (var != NOVARIABLE)
//								{
//									TDSETISPARAM(var->textdescript, VTISPARAMETER);
//									TDSETDISPPART(var->textdescript, VTDISPLAYNAMEVALINH);
//								}
//							}
						}
					}
				}
				continue;
			}
	
			// handle "make_wire" for defining arcs
			if (keyword0.equalsIgnoreCase("make_wire"))
			{
				SueWire sw = new SueWire();
				double fx = io_suemakex(TextUtils.atof((String)keywords.get(1)));
				double fy = io_suemakex(TextUtils.atof((String)keywords.get(2)));
				sw.pt[0] = new Point2D.Double(fx, fy);
				double tx = io_suemakex(TextUtils.atof((String)keywords.get(3)));
				double ty = io_suemakex(TextUtils.atof((String)keywords.get(4)));
				sw.pt[1] = new Point2D.Double(tx, ty);
				sueWires.add(sw);
				continue;
			}
	
			// handle "icon_term" for defining ports in icons
			if (keyword0.equalsIgnoreCase("icon_term"))
			{
				ParseParameters parP = new ParseParameters(keywords, 1);
				NodeProto proto = Schematics.tech.busPinNode;
				double px = proto.getDefWidth();
				double py = proto.getDefHeight();
				NodeInst ni = NodeInst.makeInstance(proto, parP.pt, px, py, cell);
				if (ni == null) continue;

				PortInst pi = ni.getOnlyPortInst();
				Export ppt = Export.newInstance(cell, pi, parP.theName);
				if (ppt == null)
				{
					System.out.println("Cell " + cellName + ", line " + lineReader.getLineNumber() +
						", could not create port " + parP.theName);
				} else
				{
					ppt.setCharacteristic(parP.type);
				}
				continue;
			}
	
			// handle "icon_arc" for defining icon curves
			if (keyword0.equalsIgnoreCase("icon_arc"))
			{
				if (count != 9)
				{
					System.out.println("Cell " + cellName + ", line " + lineReader.getLineNumber() +
						": needs 9 arguments, has " + count + ": " + io_sueorigline);
					continue;
				}
				int start = 0;   int extent = 359;
				double p1x = io_suemakex(TextUtils.atof((String)keywords.get(1)));
				double p1y = io_suemakey(TextUtils.atof((String)keywords.get(2)));
				double p2x = io_suemakex(TextUtils.atof((String)keywords.get(3)));
				double p2y = io_suemakey(TextUtils.atof((String)keywords.get(4)));
				if (((String)keywords.get(5)).equals("-start")) start = TextUtils.atoi((String)keywords.get(6));
				if (((String)keywords.get(7)).equals("-extent")) extent = TextUtils.atoi((String)keywords.get(8));

				double sX = Math.abs(p1x - p2x);
				double sY = Math.abs(p1y - p2y);
				Point2D ctr = new Point2D.Double((p1x+p2x)/2, (p1y+p2y)/2);

				NodeInst ni = NodeInst.makeInstance(Artwork.tech.circleNode, ctr, sX, sY, cell);
				if (ni == null) continue;
				if (extent != 359)
				{
					if (extent < 0)
					{
						start += extent;
						extent = -extent;
					}
					double rextent = extent+1;
					rextent = rextent * Math.PI / 180.0;
					double rstart = start * Math.PI / 180.0;
					ni.setArcDegrees(rstart, rextent);
				}
				continue;
			}
	
			// handle "icon_line" for defining icon outlines
			if (keyword0.equalsIgnoreCase("icon_line"))
			{
				List pointList = new ArrayList();
				double x = 0;
				for(int i=1; i<keywords.size(); i++)
				{
					if (((String)keywords.get(i)).equals("-tags")) break;
					if ((i%2) != 0)
					{
						x = io_suemakex(TextUtils.atof((String)keywords.get(i)));
					} else
					{
						double y = io_suemakey(TextUtils.atof((String)keywords.get(i)));
						pointList.add(new Point2D.Double(x, y));
					}
				}
				int keyCount = pointList.size();
				if (keyCount == 0) continue;
	
				// determine bounds of icon
				Point2D firstPt = (Point2D)pointList.get(0);
				double lx = firstPt.getX();
				double hx = lx;
				double ly = firstPt.getY();
				double hy = ly;
				for(int i=1; i<keyCount; i++)
				{
					Point2D nextPt = (Point2D)pointList.get(i);
					if (nextPt.getX() < lx) lx = nextPt.getX();
					if (nextPt.getX() > hx) hx = nextPt.getX();
					if (nextPt.getY() < ly) ly = nextPt.getY();
					if (nextPt.getY() > hy) hy = nextPt.getY();
				}
				double cx = (lx + hx) / 2;
				double cy = (ly + hy) / 2;
				Point2D ctr = new Point2D.Double(cx, cy);
				NodeInst ni = NodeInst.makeInstance(Artwork.tech.openedPolygonNode, ctr, hx-lx, hy-ly, cell);
				if (ni == null) return null;
				Point2D [] points = new Point2D[keyCount];
				for(int i=0; i<keyCount; i++)
				{
					Point2D pt = (Point2D)pointList.get(i);
					points[i] = new Point2D.Double(pt.getX() - cx, pt.getY() - cy);
				}
				ni.newVar(NodeInst.TRACE, points);
				continue;
			}
	
			// handle "icon_setup" for defining variables
			if (keyword0.equalsIgnoreCase("icon_setup"))
			{
//				// extract parameters
//				if (namesame(keywords[1], x_("$args")) != 0)
//				{
//					ttyputerr(_("Cell %s, line %ld: has unrecognized 'icon_setup'"),
//						cellname, io_suelineno);
//					continue;
//				}
//				pt = keywords[2];
//				if (*pt == '{') pt++;
//				for(;;)
//				{
//					while (*pt == ' ') pt++;
//					if (*pt == '}' || *pt == 0) break;
//	
//					// collect up to a space or close curly
//					startkey = pt;
//					curly = 0;
//					for(;;)
//					{
//						if (curly == 0)
//						{
//							if (*pt == 0 || *pt == ' ' || *pt == '}') break;
//						}
//						if (*pt == '{') curly++;
//						if (*pt == '}') curly--;
//						if (*pt == 0) break;
//						pt++;
//					}
//					save = *pt;
//					*pt = 0;
//	
//					// parse the keyword pair in "startkey"
//					i = numargs+1;
//					newargnames = (CHAR **)emalloc(i * (sizeof (CHAR *)), el_tempcluster);
//					newargvalues = (CHAR **)emalloc(i * (sizeof (CHAR *)), el_tempcluster);
//					for(i=0; i<numargs; i++)
//					{
//						// LINTED "argnames" used in proper order
//						newargnames[i] = argnames[i];
//	
//						// LINTED "argvalues" used in proper order
//						newargvalues[i] = argvalues[i];
//					}
//					if (numargs > 0)
//					{
//						efree((CHAR *)argnames);
//						efree((CHAR *)argvalues);
//					}
//					argnames = newargnames;
//					argvalues = newargvalues;
//					startkey++;
//					for(cpt = startkey; *cpt != 0; cpt++) if (*cpt == ' ') break;
//					if (*cpt != 0) *cpt++ = 0;
//					(void)allocstring(&argnames[numargs], startkey, el_tempcluster);
//					while (*cpt == ' ') cpt++;
//					if (*cpt == '{') cpt++;
//					startkey = cpt;
//					for(cpt = startkey; *cpt != 0; cpt++) if (*cpt == '}') break;
//					if (*cpt != 0) *cpt++ = 0;
//					(void)allocstring(&argvalues[numargs], startkey, el_tempcluster);
//					numargs++;
//	
//					*pt = save;
//				}
				continue;
			}
	
			// handle "icon_property" for defining icon strings
			if (keyword0.equalsIgnoreCase("icon_property"))
			{
				// extract parameters
				ParseParameters parP = new ParseParameters(keywords, 1);
				if (parP.theLabel == null) continue;
	
//				// substitute parameters
//				infstr = initinfstr();
//				for(pt = thelabel; *pt != 0; pt++)
//				{
//					if (*pt == '$')
//					{
//						for(i=0; i<numargs; i++)
//							if (namesamen(&pt[1], argnames[i], estrlen(argnames[i])) == 0) break;
//						if (i < numargs)
//						{
//							addstringtoinfstr(infstr, argvalues[i]);
//							pt += estrlen(argnames[i]);
//							continue;
//						}
//					}
//					addtoinfstr(infstr, *pt);
//				}
//				thelabel = returninfstr(infstr);

				NodeInst ni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, parP.pt, 0, 0, cell);
				if (ni == null) continue;
				Variable var = ni.newVar(Artwork.ART_MESSAGE, parP.theLabel);
				continue;
			}
	
			// handle "make_text" for placing strings
			if (keyword0.equalsIgnoreCase("make_text"))
			{
				// extract parameters
				ParseParameters parP = new ParseParameters(keywords, 1);
				if (parP.theText == null) continue;
	
				NodeInst ni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, parP.pt, 0, 0, cell);
				if (ni == null) continue;
				Variable var = ni.newVar(Artwork.ART_MESSAGE, parP.theText);
				continue;
			}
	
			// ignore known keywords
			if (keyword0.equalsIgnoreCase("icon_title") ||
				keyword0.equalsIgnoreCase("make_line") ||
				keyword0.equalsIgnoreCase("}"))
			{
				continue;
			}
	
			System.out.println("Cell " + cellName + ", line " + lineReader.getLineNumber() +
				": unknown keyword (" + keyword0 + "): " + io_sueorigline);
		}
	
		// place an icon instance in the schematic if requested
		if (placeIcon && schemCell != null &&
			iconCell != null)
		{
			Rectangle2D bounds = iconCell.getBounds();
			double wid = bounds.getWidth();
			double hei = bounds.getHeight();
			NodeInst ni = NodeInst.makeInstance(iconCell, iconPt, wid, hei, schemCell);
			if (ni != null) ni.setExpanded();
		}
	
		// cleanup the current cell
		if (cell != null)
		{
			io_sueplacewires(sueWires, sueNets, cell);
			io_sueplacenets(sueNets, cell);
		}
	
//		// make sure cells are the right size
//		if (schemCell != NONODEPROTO) (*el_curconstraint->solve)(schemCell);
//		if (iconCell != NONODEPROTO) (*el_curconstraint->solve)(iconCell);
	
		// return the cell
		if (schemCell != null) return schemCell;
		return iconCell;
	}
	
	/**
	 * Method to create a port called "thename" on port "pp" of node "ni" in cell "cell".
	 * The name is modified if it already exists.
	 */
	private Export io_suenewexport(Cell cell, PortInst pi, String theName)
	{
		String portName = theName;
		for(int i=0; ; i++)
		{
			Export ppt = (Export)cell.findPortProto(portName);
			if (ppt == null)
			{
				return Export.newInstance(cell, pi, portName);
			}
	
			// make space for modified name
			int openPos = theName.indexOf('[');
			if (openPos < 0) portName = theName + "-" + i; else
			{
				portName = theName.substring(0, openPos) + "-" + i + theName.substring(openPos);
			}
		}
	}

	/**
	 * Method to find the pin at (x, y) and return it.
	 */
	private PortInst io_suefindpinnode(double x, double y, Cell cell)
	{
		Rectangle2D searchBounds = new Rectangle2D.Double(x, y, 0, 0);
		for(Geometric.Search sea = new Geometric.Search(searchBounds, cell); sea.hasNext(); )
		{
			Geometric geom = (Geometric)sea.next();
			if (!(geom instanceof NodeInst)) continue;
			NodeInst ni = (NodeInst)geom;
	
			// find closest port
			for(Iterator it = ni.getPortInsts(); it.hasNext(); )
			{
				PortInst pi = (PortInst)it.next();
				Poly poly = pi.getPoly();
				if (poly.getCenterX() == x && poly.getCenterY() == y) return pi;
			}
		}
		return null;
	}

	/**
	 * Method to find the SUE file "name" on disk, and read it into library "lib".
	 * Returns NONODEPROTO if the file is not found or not read properly.
	 */
	private NodeProto io_suereadfromdisk(Library lib, String name)
	{
		// look for another "sue" file that describes this cell
		for(Iterator it = io_suedirectories.iterator(); it.hasNext(); )
		{
			String directory = (String)it.next();
			String subFileName = directory + name + ".sue";
//			f = xopen(subfilename, io_filetypesue, x_(""), &truename);
//			if (f != 0)
//			{
//				for(i=0; i<MAXLINE; i++) savecurline[i] = io_suecurline[i];
//				estrcpy(saveorigline, io_sueorigline);
//				estrcpy(savesuelastline, io_suelastline);
//				estrcpy(lastprogressmsg, DiaGetTextProgress(dia));
//				estrcpy(suevarname, _("Reading "));
//				estrcat(suevarname, name);
//				estrcat(suevarname, x_("..."));
//				DiaSetTextProgress(dia, suevarname);
//	
//				estrcpy(subfilename, name);
//				savelineno = io_suelineno;
//				io_suelineno = 0;
//				(void)io_suereadfile(lib, subfilename, dia);
//				io_suelineno = savelineno;
//				estrcpy(io_suelastline, savesuelastline);
//				estrcpy(io_sueorigline, saveorigline);
//				for(i=0; i<MAXLINE; i++) io_suecurline[i] = savecurline[i];
//	
//				// now try to find the cell in the library
//				proto = io_suegetnodeproto(lib, subfilename);
//				return(proto);
//			}
		}
		return null;
	}

	/**
	 * Method to find cell "protoname" in library "lib".
	 */
	private NodeProto io_suegetnodeproto(Library lib, String protoname)
	{
		return lib.findNodeProto(protoname);
	}

	private static class ParseParameters
	{
		int count;
		Point2D pt;
		int rot;
		boolean trn;
		PortCharacteristic type;
		String theName;
		String theLabel;
		String theText;

		/**
		 * Method to parse the "count" parameters in "keywords" and fill in the values
		 * that are found.  Fills in:
		 * "-origin"  placed into "x" and "y"
		 * "-orient"  placed into "rot" and "trn"
		 * "-type"    placed into "type"
		 * "-name"    placed into "thename".
		 * "-label"   placed into "thelabel".
		 * "-text"    placed into "thetext".
		 */
		private ParseParameters(List keywords, int start)
		{
			double x = 0, y = 0;
			rot = 0;
			trn = false;
			type = PortCharacteristic.UNKNOWN;
			theName = null;
			theLabel = null;
			theText = null;
			for(int i=start; i<keywords.size(); i += 2)
			{
				String keyword = (String)keywords.get(i);
				String param = (String)keywords.get(i+1);
				if (keyword.equalsIgnoreCase("-origin"))
				{
					int j = 0;
					if (param.charAt(j) == '{') j++;
					x = TextUtils.atof(param.substring(j));
					while (j < param.length()-1 && !Character.isWhitespace(param.charAt(j))) j++;
					while (j < param.length()-1 && Character.isWhitespace(param.charAt(j))) j++;
					y = TextUtils.atof(param.substring(j));
				}
				if (keyword.equalsIgnoreCase("-orient"))
				{
					if (param.equalsIgnoreCase("R90"))  { rot = 900;  } else
					if (param.equalsIgnoreCase("R270")) { rot = 2700; } else
					if (param.equalsIgnoreCase("RXY"))  { rot = 1800; } else
					if (param.equalsIgnoreCase("RY"))   { rot = 900;  trn = true; } else
					if (param.equalsIgnoreCase("R90X")) { rot = 0;    trn = true; } else
					if (param.equalsIgnoreCase("R90Y")) { rot = 1800; trn = true; } else
					if (param.equalsIgnoreCase("RX"))   { rot = 2700; trn = true; }
				}
				if (keyword.equalsIgnoreCase("-type"))
				{
					if (param.equalsIgnoreCase("input")) type = PortCharacteristic.IN; else
					if (param.equalsIgnoreCase("output")) type = PortCharacteristic.OUT; else
					if (param.equalsIgnoreCase("inout")) type = PortCharacteristic.BIDIR;
				}
				if (keyword.equalsIgnoreCase("-name") ||
					keyword.equalsIgnoreCase("-label") ||
					keyword.equalsIgnoreCase("-text"))
				{
					String infstr = param;
					if (infstr.startsWith("{") && infstr.endsWith("}"))
					{
						int len = infstr.length();
						infstr = infstr.substring(1, len-1);
					}
					if (keyword.equalsIgnoreCase("-name")) theName = infstr; else
						if (keyword.equalsIgnoreCase("-label")) theLabel = infstr; else
							if (keyword.equalsIgnoreCase("-text")) theText = infstr;
				}
			}
			pt = new Point2D.Double(io_suemakex(x), io_suemakey(y));
			rot = (3600 - rot) % 3600;
		}
	}

	/**
	 * Method to place all SUE wires into the cell.
	 */
	private void io_sueplacewires(List sueWires, List sueNets, Cell cell)
	{
//		SUEWIRE *sw, *osw;
//		SUENET *sn;
//		REGISTER INTBIG i, j, wid, px, py, lx, hx, ly, hy, sea, bits;
//		REGISTER BOOLEAN propagatedbus, isbus;
//		INTBIG xsize, ysize, x, y, ox, oy;
//		REGISTER NODEPROTO *proto;
//		REGISTER GEOM *geom;
//		REGISTER PORTEXPINST *pe;
//		NODEINST *ni;
//		REGISTER ARCPROTO *ap;
//		REGISTER NODEINST *bottomni, *oni;
//		PORTPROTO *pp;
//		REGISTER PORTPROTO *bottompp, *opp;
//		REGISTER ARCINST *ai;
	
		// mark all wire ends as "unassigned", all wire types as unknown
		for(Iterator wIt = sueWires.iterator(); wIt.hasNext(); )
		{
			SueWire sw = (SueWire)wIt.next();
			sw.pi[0] = sw.pi[1] = null;
			sw.proto = null;
		}
	
		// examine all network names and assign wire types appropriately
		for(Iterator nIt = sueNets.iterator(); nIt.hasNext(); )
		{
			SueNet sn = (SueNet)nIt.next();
			for(Iterator wIt = sueWires.iterator(); wIt.hasNext(); )
			{
				SueWire sw = (SueWire)wIt.next();
				for(int i=0; i<2; i++)
				{
					if (sw.pt[i].getX() == sn.pt.getX() && sw.pt[i].getY() == sn.pt.getY())
					{
						Name snName = Name.findName(sn.label);
						if (snName.busWidth() > 1) sw.proto = Schematics.tech.bus_arc; else
							sw.proto = Schematics.tech.wire_arc;
					}
				}
			}
		}
	
		// find connections that are exactly on existing nodes
		for(Iterator wIt = sueWires.iterator(); wIt.hasNext(); )
		{
			SueWire sw = (SueWire)wIt.next();
			for(int i=0; i<2; i++)
			{
				if (sw.pi[i] != null) continue;
				for(Iterator it = cell.getNodes(); it.hasNext(); )
				{
					NodeInst ni = (NodeInst)it.next();
					PortInst pi = io_suewiredport(ni, sw.pt[i], sw.pt[1-i]);
					if (pi == null) continue;
					sw.pi[i] = pi;
	
					// determine whether this port is a bus
					boolean isbus = false;
//					bottomni = ni;   bottompp = pp;
//					while (bottomni->proto->primindex == 0)
//					{
//						bottomni = bottompp->subnodeinst;
//						bottompp = bottompp->subportproto;
//					}
//					if (bottomni->proto == Schematics.tech.wireConNode) continue;
//					if (!isbus && ni->proto == Schematics.tech.offpageNode)
//					{
//						// see if there is a bus port on this primitive
//						for(pe = ni->firstportexpinst; pe != NOPORTEXPINST; pe = pe->nextportexpinst)
//						{
//							if (net_buswidth(pe->exportproto->protoname) > 1) isbus = true;
//						}
//					}
	
					if (isbus)
					{
						sw.proto = Schematics.tech.bus_arc;
					} else
					{
						if (sw.proto == null)
							sw.proto = Schematics.tech.wire_arc;
					}
				}
			}
		}
	
		// now iteratively extend bus wires to connections with others
		boolean propagatedbus = true;
		while (propagatedbus)
		{
			propagatedbus = false;
			for(Iterator wIt = sueWires.iterator(); wIt.hasNext(); )
			{
				SueWire sw = (SueWire)wIt.next();
				if (sw.proto != Schematics.tech.bus_arc) continue;
				for(Iterator oWIt = sueWires.iterator(); oWIt.hasNext(); )
				{
					SueWire oSw = (SueWire)oWIt.next();
					if (oSw.proto != null) continue;
					for(int i=0; i<2; i++)
					{
						for(int j=0; j<2; j++)
						{
							if (sw.pt[i].getX() == oSw.pt[j].getX() && sw.pt[i].getY() == oSw.pt[j].getY())
							{
								// common point found: continue the bus request
								oSw.proto = Schematics.tech.bus_arc;
								propagatedbus = true;
							}
						}
					}
				}
			}
		}

		// now make pins where wires meet
		for(Iterator wIt = sueWires.iterator(); wIt.hasNext(); )
		{
			SueWire sw = (SueWire)wIt.next();
			for(int i=0; i<2; i++)
			{
				if (sw.pi[i] != null) continue;
				NodeProto proto = Schematics.tech.wirePinNode;
				if (sw.proto == Schematics.tech.bus_arc) proto = Schematics.tech.busPinNode;

				// look at all other wires at this point and figure out type of pin to make
				for(Iterator oWIt = sueWires.iterator(); oWIt.hasNext(); )
				{
					SueWire oSw = (SueWire)oWIt.next();
					if (oSw == sw) continue;
					for(int j=0; j<2; j++)
					{
						if (sw.pt[i].getX() != oSw.pt[j].getX() || sw.pt[i].getY() != oSw.pt[j].getY()) continue;
						if (oSw.pi[j] != null)
						{
							sw.pi[i] = oSw.pi[j];
							break;
						}
						if (oSw.proto == Schematics.tech.bus_arc) proto = Schematics.tech.busPinNode;
					}
					if (sw.pi[i] != null) break;
				}
	
				// make the pin if it doesn't exist
				if (sw.pi[i] == null)
				{
					// common point found: make a pin
					double xSize = proto.getDefWidth();
					double ySize = proto.getDefHeight();
					NodeInst ni = NodeInst.makeInstance(proto, sw.pt[i], xSize, ySize, cell);
					sw.pi[i] = ni.getOnlyPortInst();
				}
	
				// put that node in all appropriate locations
				for(Iterator oWIt = sueWires.iterator(); oWIt.hasNext(); )
				{
					SueWire oSw = (SueWire)oWIt.next();
					if (oSw == sw) continue;
					for(int j=0; j<2; j++)
					{
						if (sw.pt[i].getX() != oSw.pt[j].getX() || sw.pt[i].getY() != oSw.pt[j].getY()) continue;
						if (oSw.pi[j] != null) continue;
						oSw.pi[j] = sw.pi[i];
					}
				}
			}
		}
	
		// make pins at all of the remaining wire ends
		for(Iterator wIt = sueWires.iterator(); wIt.hasNext(); )
		{
			SueWire sw = (SueWire)wIt.next();
			for(int i=0; i<2; i++)
			{
				if (sw.pi[i] != null) continue;
				sw.pi[i] = io_suefindnode(sw.pt[i], sw.pt[1-i], cell, sw.pi[1-i]);
				if (sw.pi[i] == null)
				{
					NodeProto proto = Schematics.tech.wirePinNode;
					if (sw.proto == Schematics.tech.bus_arc) proto = Schematics.tech.busPinNode;
					double xsize = proto.getDefWidth();
					double ysize = proto.getDefHeight();
					NodeInst ni = NodeInst.makeInstance(proto, sw.pt[i], xsize, ysize, cell);
					sw.pi[i] = ni.getOnlyPortInst();
				}
			}
		}
	
		// now make the connections
		for(Iterator wIt = sueWires.iterator(); wIt.hasNext(); )
		{
			SueWire sw = (SueWire)wIt.next();
			if (sw.proto == null) sw.proto = Schematics.tech.wire_arc;
			double wid = sw.proto.getDefaultWidth();
	
			// if this is a bus, make sure it can connect */
			if (sw.proto == Schematics.tech.bus_arc)
			{
//				for(int i=0; i<2; i++)
//				{
//					for(j=0; sw->pp[i]->connects[j] != NOARCPROTO; j++)
//						if (sw->pp[i]->connects[j] == sch_busarc) break;
//					if (sw->pp[i]->connects[j] == NOARCPROTO)
//					{
//						// this end cannot connect: fake the connection
//						px = (sw->x[0] + sw->x[1]) / 2;
//						py = (sw->y[0] + sw->y[1]) / 2;
//						defaultnodesize(sch_buspinprim, &xsize , &ysize);
//						lx = px - xsize/2;   hx = lx + xsize;
//						ly = py - ysize/2;   hy = ly + ysize;
//						ni = newnodeinst(sch_buspinprim, lx, hx, ly, hy, 0, 0, cell);
//						if (ni == NONODEINST) break;
//						endobjectchange((INTBIG)ni, VNODEINST);
//						pp = ni->proto->firstportproto;
//						ai = newarcinst(gen_unroutedarc, defaultarcwidth(gen_unroutedarc),
//							us_makearcuserbits(gen_unroutedarc), ni, pp, px, py,
//								sw->ni[i], sw->pp[i], sw->x[i], sw->y[i], cell);
//						if (ai == NOARCINST)
//						{
//							ttyputerr(_("Error making fake connection"));
//							break;
//						}
//						endobjectchange((INTBIG)ai, VARCINST);
//						sw->ni[i] = ni;
//						sw->pp[i] = pp;
//						sw->x[i] = px;
//						sw->y[i] = py;
//					}
//				}
			}

			ArcInst ai = ArcInst.makeInstance(sw.proto, wid, sw.pi[0], sw.pi[1], sw.pt[0], sw.pt[1], null);
			if (ai == null)
			{
//				ttyputerr(_("Could not run a wire from %s to %s in cell %s"),
//					describenodeinst(sw->ni[0]), describenodeinst(sw->ni[1]),
//						describenodeproto(cell));
				continue;
			}
	
//			// negate the wire if requested
//			if (sw->ni[0]->temp1 != 0 &&
//				estrcmp(sw->pp[0]->protoname, x_("y")) == 0)
//			{
//				ai->userbits |= ISNEGATED;
//			} else if (sw->ni[1]->temp1 != 0 &&
//				estrcmp(sw->pp[1]->protoname, x_("y")) == 0)
//			{
//				ai->userbits |= ISNEGATED | REVERSEEND;
//			}
		}
	
//		// now look for implicit connections where "offpage" connectors touch
//		for(ni = cell->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//		{
//			if (ni->proto != sch_offpageprim) continue;
//			if (ni->firstportarcinst != NOPORTARCINST) continue;
//			pp = ni->proto->firstportproto->nextportproto;
//			portposition(ni, pp, &x, &y);
//			sea = initsearch(x, x, y, y, cell);
//			for(;;)
//			{
//				geom = nextobject(sea);
//				if (geom == NOGEOM) break;
//				if (!geom->entryisnode) continue;
//				oni = geom->entryaddr.ni;
//				if (oni == ni) continue;
//				for(opp = oni->proto->firstportproto; opp != NOPORTPROTO; opp = opp->nextportproto)
//				{
//					portposition(oni, opp, &ox, &oy);
//					if (ox != x || oy != y) continue;
//					for(i=0; i<3; i++)
//					{
//						switch (i)
//						{
//							case 0: ap = sch_busarc;      break;
//							case 1: ap = sch_wirearc;     break;
//							case 2: ap = gen_unroutedarc; break;
//						}
//						for(j=0; pp->connects[j] != NOARCPROTO; j++)
//							if (pp->connects[j] == ap) break;
//						if (pp->connects[j] == NOARCPROTO) continue;
//						for(j=0; opp->connects[j] != NOARCPROTO; j++)
//							if (opp->connects[j] == ap) break;
//						if (opp->connects[j] == NOARCPROTO) continue;
//						break;
//					}
//	
//					wid = defaultarcwidth(ap);
//					bits = us_makearcuserbits(ap);
//					ai = newarcinst(ap, wid, bits, ni, pp, x, y, oni, opp, x, y, cell);
//					if (ai != NOARCINST)
//						endobjectchange((INTBIG)ai, VARCINST);
//					break;
//				}
//				if (opp != NOPORTPROTO) break;
//			}
//		}
	}
	
	/**
	 * Method to find the node at (x, y) and return it.
	 */
	private PortInst io_suefindnode(Point2D pt, Point2D oPt, Cell cell, PortInst notThisPort)
	{
		double slop = 10;
		PortInst bestPi = null;
		double bestDist = Double.MAX_VALUE;
		Rectangle2D searchBounds = new Rectangle2D.Double(pt.getX()-slop, pt.getY()-slop, slop*2, slop*2);
		for(Geometric.Search sea = new Geometric.Search(searchBounds, cell); sea.hasNext(); )
		{
			Geometric geom = (Geometric)sea.next();
			if (!(geom instanceof NodeInst)) continue;
			NodeInst ni = (NodeInst)geom;
			if (notThisPort != null && ni == notThisPort.getNodeInst()) continue;
	
			// ignore pins
			if (ni.getProto() == Schematics.tech.wirePinNode) continue;
	
			// find closest port
			for(Iterator it = ni.getPortInsts(); it.hasNext(); )
			{
				PortInst pi = (PortInst)it.next();
				Poly poly = pi.getPoly();
				Rectangle2D bounds = poly.getBounds2D();
	
				// find out if the line crosses the polygon
				double thisX = oPt.getX();
				double thisY = oPt.getY();
				if (pt.getX() == oPt.getX())
				{
					// line is vertical: look for intersection with polygon
					if (oPt.getX() < bounds.getMinX() || oPt.getX() > bounds.getMaxX()) continue;
					thisX = oPt.getX();
					thisY = bounds.getCenterY();
				} else if (pt.getY() == oPt.getY())
				{
					// line is horizontal: look for intersection with polygon
					if (oPt.getY() < bounds.getMinY() || oPt.getY() > bounds.getMaxY()) continue;
					thisX = bounds.getCenterX();
					thisY = oPt.getY();
				} else
				{
					if (!poly.isInside(oPt)) continue;
				}
	
				double dist = oPt.distance(new Point2D.Double(thisX, thisY));
				if (bestPi == null || dist < bestDist)
				{
					bestPi = pi;
					bestDist = dist;
				}
			}
		}
	
		// report the hit
		return bestPi;
	}

	/**
	 * Method to find the port on node "ni" that attaches to the wire from (x,y) to (ox,oy).
	 * Returns NOPORTPROTO if not found.
	 */
	private PortInst io_suewiredport(NodeInst ni, Point2D pt, Point2D oPt)
	{
		for(Iterator it = ni.getPortInsts(); it.hasNext(); )
		{
			PortInst pi = (PortInst)it.next();
			Poly poly = pi.getPoly();
			if (poly.isInside(pt)) return pi;
		}
		if (ni.getTrueCenterX() != pt.getX() ||
			ni.getTrueCenterY() != pt.getY()) return null;
	
		// find port that is closest to OTHER end
		double bestdist = Double.MAX_VALUE;
		PortInst bestpi = null;
		for(Iterator it = ni.getPortInsts(); it.hasNext(); )
		{
			PortInst pi = (PortInst)it.next();
			Poly poly = pi.getPoly();
			Point2D ctr = new Point2D.Double(poly.getCenterX(), poly.getCenterY());
			double dist = ctr.distance(oPt);
			if (dist > bestdist) continue;
			bestdist = dist;
			bestpi = pi;
		}
		Poly poly = bestpi.getPoly();
		pt.setLocation(poly.getCenterX(), poly.getCenterY());
		return bestpi;
	}

	/**
	 * Method to place all SUE nets into the cell (they are in a linked
	 * list headed by "sueNets").
	 */
	private void io_sueplacenets(List sueNets, Cell cell)
	{
//		SUENET *sn;
//		REGISTER INTBIG pass;
//		REGISTER BOOLEAN isbus;
//		REGISTER ARCINST *ai, *bestai;
//		REGISTER INTBIG cx, cy, dist, bestdist, sea;
//		REGISTER GEOM *geom;
//		REGISTER CHAR *netname, *busname;
//		REGISTER void *infstr;
	
		// 3 passes: qualified labels, unqualified busses, unqualified wires
		for(int pass=0; pass<3; pass++)
		{
			for(Iterator it = sueNets.iterator(); it.hasNext(); )
			{
				SueNet sn = (SueNet)it.next();

//				// unqualified labels (starting with "[") happen second
//				if (*sn->label == '[')
//				{
//					// unqualified label: pass 2 or 3 only
//					if (pass == 0) continue;
//				} else
//				{
//					// qualified label: pass 1 only
//					if (pass != 0) continue;
//				}
//	
//				// see if this is a bus
//				if (net_buswidth(sn->label) > 1) isbus = TRUE; else isbus = FALSE;
//	
//				sea = initsearch(sn->x, sn->x, sn->y, sn->y, cell);
//				bestai = NOARCINST;
//				for(;;)
//				{
//					geom = nextobject(sea);
//					if (geom == NOGEOM) break;
//					if (geom->entryisnode) continue;
//					ai = geom->entryaddr.ai;
//					if (isbus)
//					{
//						if (ai->proto != sch_busarc) continue;
//					} else
//					{
//						if (ai->proto == sch_busarc) continue;
//					}
//					cx = (ai->end[0].xpos + ai->end[1].xpos) / 2;
//					cy = (ai->end[0].ypos + ai->end[1].ypos) / 2;
//					dist = computedistance(cx, cy, sn->x, sn->y);
//	
//					// LINTED "bestdist" used in proper order
//					if (bestai == NOARCINST || dist < bestdist)
//					{
//						bestai = ai;
//						bestdist = dist;
//					}
//				}
//				if (bestai != NOARCINST)
//				{
//					if (pass == 1)
//					{
//						// only allow busses
//						if (bestai->proto != sch_busarc) continue;
//					} else if (pass == 2)
//					{
//						// disallow busses
//						if (bestai->proto == sch_busarc) continue;
//					}
//					netname = sn->label;
//					if (*netname == '[')
//					{
//						// find the proper name of the network
//						busname = io_suefindbusname(bestai);
//						if (busname != 0)
//						{
//							infstr = initinfstr();
//							addstringtoinfstr(infstr, busname);
//							addstringtoinfstr(infstr, netname);
//							netname = returninfstr(infstr);
//						}
//					}
//					us_setarcname(bestai, netname);
//				}
			}
		}
	}

	/**
	 * Method to read the next line from file and break
	 * it up into space-separated keywords.  Returns the number
	 * of keywords (-1 on EOF)
	 */
	private List io_suegetnextline(int curlydepth)
		throws IOException
	{
		for(int lineno=0; ; lineno++)
		{
			if (io_suelastline == null)
			{
				io_suelastline = lineReader.readLine();
				if (io_suelastline == null) return null;
			}
			if (lineno == 0)
			{
				// first line: use it
				io_suecurline = io_suelastline;
			} else
			{
				// subsequent line: use it only if a continuation
				if (io_suelastline.length() == 0 || io_suelastline.charAt(0) != '+') break;
				io_suecurline += io_suelastline.substring(1);
			}
			io_suelastline = null;
		}
		io_sueorigline = io_suecurline;

		// parse the line
		boolean inblank = true;
		List keywords = new ArrayList();
		int startIndex = 0;
		int len = io_suecurline.length();
		for(int i=0; i<len; i++)
		{
			char pt = io_suecurline.charAt(i);
			if (pt == '{') curlydepth++;
			if (pt == '}') curlydepth--;
			if ((pt == ' ' || pt == '\t') && curlydepth == 0)
			{
				if (!inblank)
				{
					String keyword = io_suecurline.substring(startIndex, i).trim();
					keywords.add(keyword);
					startIndex = i;
				}
				inblank = true;
			} else
			{
				if (inblank)
				{
					startIndex = i;
				}
				inblank = false;
			}
		}
		String keyword = io_suecurline.substring(startIndex, len).trim();
		if (keyword.length() > 0)
			keywords.add(keyword);
		return keywords;
	}
	
	/**
	 * Method to examine a SUE expression and add "@" in front of variable names.
	 */
	private String io_sueparseexpression(String expression)
	{
//		infstr = initinfstr();
//		while (*expression != 0)
//		{
//			keyword = getkeyword(&expression, x_(" \t,+-*/()"));
//			if (keyword == NOSTRING) break;
//			if (*keyword != 0)
//			{
//				if (isdigit(keyword[0]))
//				{
//					addstringtoinfstr(infstr, keyword);
//				} else
//				{
//					if (*expression != '(')
//						addtoinfstr(infstr, '@');
//					addstringtoinfstr(infstr, keyword);
//				}
//				if (*expression != 0)
//					addtoinfstr(infstr, *expression++);
//			}
//		}
//		return(returninfstr(infstr));
		return expression;
	}

	/**
	 * Method to convert SUE X coordinate "x" to Electric coordinates
	 */
	private static double io_suemakex(double x)
	{
		return x / 8;
	}
	
	/**
	 * Method to convert SUE Y coordinate "y" to Electric coordinates
	 */
	private static double io_suemakey(double y)
	{
		return -y / 8;
	}
}

//	
//	/*
//	 * Routine to start at "ai" and search all wires until it finds a named bus.
//	 * Returns zero if no bus name is found.
//	 */
//	CHAR *io_suefindbusname(ARCINST *ai)
//	{
//		REGISTER ARCINST *oai;
//		REGISTER CHAR *busname, *pt;
//		REGISTER VARIABLE *var;
//		static CHAR pseudobusname[50];
//		REGISTER INTBIG index, len;
//	
//		for(oai = ai->parent->firstarcinst; oai != NOARCINST; oai = oai->nextarcinst)
//			oai->temp1 = 0;
//		busname = io_suesearchbusname(ai);
//		if (busname == 0)
//		{
//			for(index=1; ; index++)
//			{
//				esnprintf(pseudobusname, 50, x_("NET%ld"), index);
//				len = estrlen(pseudobusname);
//				for(oai = ai->parent->firstarcinst; oai != NOARCINST; oai = oai->nextarcinst)
//				{
//					var = getvalkey((INTBIG)oai, VARCINST, VSTRING, el_arc_name_key);
//					if (var == NOVARIABLE) continue;
//					pt = (CHAR *)var->addr;
//					if (namesame(pseudobusname, pt) == 0) break;
//					if (namesamen(pseudobusname, pt, len) == 0 &&
//						pt[len] == '[') break;
//				}
//				if (oai == NOARCINST) break;
//			}
//			busname = pseudobusname;
//		}
//		return(busname);
//	}
//	
//	CHAR *io_suesearchbusname(ARCINST *ai)
//	{
//		REGISTER ARCINST *oai;
//		REGISTER CHAR *busname;
//		REGISTER INTBIG i;
//		REGISTER NODEINST *ni;
//		REGISTER PORTARCINST *pi;
//		REGISTER PORTEXPINST *pe;
//		REGISTER PORTPROTO *pp;
//		REGISTER VARIABLE *var;
//		REGISTER void *infstr;
//	
//		ai->temp1 = 1;
//		if (ai->proto == sch_busarc)
//		{
//			var = getvalkey((INTBIG)ai, VARCINST, VSTRING, el_arc_name_key);
//			if (var != NOVARIABLE)
//			{
//				infstr = initinfstr();
//				for(busname = (CHAR *)var->addr; *busname != 0; busname++)
//				{
//					if (*busname == '[') break;
//					addtoinfstr(infstr, *busname);
//				}
//				return(returninfstr(infstr));
//			}
//		}
//		for(i=0; i<2; i++)
//		{
//			ni = ai->end[i].nodeinst;
//			if (ni->proto != sch_wirepinprim && ni->proto != sch_buspinprim &&
//				ni->proto != sch_offpageprim) continue;
//			if (ni->proto == sch_buspinprim || ni->proto == sch_offpageprim)
//			{
//				// see if there is an arrayed port here
//				for(pe = ni->firstportexpinst; pe != NOPORTEXPINST; pe = pe->nextportexpinst)
//				{
//					pp = pe->exportproto;
//					for(busname = pp->protoname; *busname != 0; busname++)
//						if (*busname == '[') break;
//					if (*busname != 0)
//					{
//						infstr = initinfstr();
//						for(busname = pp->protoname; *busname != 0; busname++)
//						{
//							if (*busname == '[') break;
//							addtoinfstr(infstr, *busname);
//						}
//						return(returninfstr(infstr));
//					}
//				}
//			}
//			for(pi = ni->firstportarcinst; pi != NOPORTARCINST; pi = pi->nextportarcinst)
//			{
//				oai = pi->conarcinst;
//				if (oai->temp1 != 0) continue;
//				busname = io_suesearchbusname(oai);
//				if (busname != 0) return(busname);
//			}
//		}
//		return(0);
//	}
