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
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.io.IOTool;

import java.awt.Point;
import java.awt.Rectangle;
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
		SueExtraWire(String portname, double xoffset, double yoffset)
		{
			this.portname = portname;
			this.xoffset = xoffset;
			this.yoffset = yoffset;
		}
	};
	
	SueExtraWire [] io_suetransistorwires =
	{
		new SueExtraWire("d",  3, 0),
		new SueExtraWire("s", -3, 0),
		new SueExtraWire("g",  0, 4.5)
	};
	
	SueExtraWire [] io_suetransistor4wires =
	{
		new SueExtraWire("d",  3,     0),
		new SueExtraWire("s", -3,     0),
		new SueExtraWire("b", -0.25, -2.5),
		new SueExtraWire("g",  0,     4.5)
	};
	
	SueExtraWire [] io_sueresistorwires =
	{
		new SueExtraWire("a", -3, 0),
		new SueExtraWire("b",  3, 0)
	};
	
	SueExtraWire [] io_suecapacitorwires =
	{
		new SueExtraWire("a",  0,  1.75),
		new SueExtraWire("b",  0, -1.75)
	};
	
	SueExtraWire [] io_suesourcewires =
	{
		new SueExtraWire("minus", 0, -1.25),
		new SueExtraWire("plus",  0,  1.5)
	};
	
	SueExtraWire [] io_suetwoportwires =
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
//		int        numextrawires;
		SueExtraWire [] extrawires;

		SueEquiv(String suename, NodeProto intproto, boolean netateoutput, int rotation, boolean transpose,
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
	
	static class SueWire
	{
		Point2D         [] pt;
		NodeInst [] ni;
		PortProto [] pp;
		ArcProto        proto;

		SueWire()
		{
			pt = new Point2D[2];
			ni = new NodeInst[2];
			pp = new PortProto[2];
		}
	};
	
	/*************** SUE NETWORKS ***************/
	
	private static class SueNet
	{
		Point2D         pt;
		String           label;
	};

//	/*************** MISCELLANEOUS ***************/
//	
//	#define MAXLINE       300			/* maximum characters on an input line */
//	#define MAXKEYWORDS    50			/* maximum keywords on an input line */
//	#define MAXICONPOINTS  25			/* maximum coordinates on an "icon_line" line */

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
//		(void)asktool(net_tool, x_("total-re-number"));
	
		return false;
	}
	
	/**
	 * Method to read the SUE file in "f"/
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
		String sueVarName = null;
//		numargs = 0;
//		namestrlen = 0;
//		lambda = lib->lambda[sch_tech->techindex];
//		inischemtech = defschematictechnology(el_curtech);
//		numericlambda = lib->lambda[inischemtech->techindex];
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
				ParseParameters pp = new ParseParameters(keywords, 2);
				if (pp.theLabel == null) continue;

//				// extract parameters
//				io_sueparseparameters(&keywords[2], count-2, &x, &y, lambda, &rot, &trn,
//					&type, &thename, &thelabel, &thetext, dia);
	
				// save the name string
				String theName = pp.theName;
	
				// ignore self-references
				if (!((String)keywords.get(1)).equalsIgnoreCase(cellName))
				{
					if (pp.pt.getX() != 0 || pp.pt.getY() != 0)
					{
//						// queue icon placement
//						iconx = x;   icony = y;
//						placeIcon = true;
					}
					continue;
				}
	
//				// special case for network names: queue them
//				if (namesame(keywords[1], x_("name_net_m")) == 0 ||
//					namesame(keywords[1], x_("name_net_s")) == 0 ||
//					namesame(keywords[1], x_("name_net")) == 0)
//				{
//					SueNet sn = new SueNet();
//					sn->x = x;
//					sn->y = y;
//					sn.label = thename;
//					sueNets.add(sn);
//					continue;
//				}
//	
//				// first check for special names
//				proto = NONODEPROTO;
//				invertoutput = 0;
//				rotation = transpose = 0;
//				xoff = yoff = 0;
//				xshrink = yshrink = 0;
//				detailbits = 0;
//				numextrawires = 0;
//				extrawires = 0;
//				type = 0;
//				if (namesame(keywords[1], x_("inout")) == 0)
//				{
//					proto = sch_offpageprim;
//					makeangle(rot, trn, trans);
//					xform(K2, 0, &xoff, &yoff, trans);
//					xoff = muldiv(xoff, lambda, WHOLE);
//					yoff = muldiv(yoff, lambda, WHOLE);
//					type = BIDIRPORT;
//				} else if (namesame(keywords[1], x_("input")) == 0)
//				{
//					proto = sch_offpageprim;
//					makeangle(rot, trn, trans);
//					xform(-K2, 0, &xoff, &yoff, trans);
//					xoff = muldiv(xoff, lambda, WHOLE);
//					yoff = muldiv(yoff, lambda, WHOLE);
//					type = INPORT;
//				} else if (namesame(keywords[1], x_("output")) == 0)
//				{
//					proto = sch_offpageprim;
//					makeangle(rot, trn, trans);
//					xform(K2, 0, &xoff, &yoff, trans);
//					xoff = muldiv(xoff, lambda, WHOLE);
//					yoff = muldiv(yoff, lambda, WHOLE);
//					type = OUTPORT;
//				} else if (namesame(keywords[1], x_("rename_net")) == 0)
//				{
//					proto = sch_wirepinprim;
//				} else if (namesame(keywords[1], x_("global")) == 0)
//				{
//					if (net_buswidth(thename) > 1) proto = sch_buspinprim; else
//					{
//						proto = sch_wirepinprim;
//						if (namesame(thename, x_("gnd")) == 0)
//						{
//							makeangle(rot, trn, trans);
//							xform(0, -K2, &xoff, &yoff, trans);
//							xoff = muldiv(xoff, lambda, WHOLE);
//							yoff = muldiv(yoff, lambda, WHOLE);
//							proto = sch_gndprim;
//							type = GNDPORT;
//						}
//						if (namesame(thename, x_("vdd")) == 0)
//						{
//							proto = sch_pwrprim;
//							type = PWRPORT;
//						}
//					}
//				} else if (namesame(keywords[1], x_("join_net")) == 0)
//				{
//					proto = sch_wireconprim;
//					xshrink = -K2;
//					makeangle(rot, trn, trans);
//					xform(Q1, 0, &xoff, &yoff, trans);
//					xoff = muldiv(xoff, lambda, WHOLE);
//					yoff = muldiv(yoff, lambda, WHOLE);
//				}
//	
//				// now check for internal associations to known primitives
//				if (proto == NONODEPROTO)
//				{
//					curstate = io_getstatebits();
//					if ((curstate[1]&SUEUSE4PORTTRANS) != 0) curequivs = io_sueequivs4; else
//						curequivs = io_sueequivs;
//					for(i=0; curequivs[i].suename != 0; i++)
//						if (namesame(keywords[1], curequivs[i].suename) == 0) break;
//					if (curequivs[i].suename != 0)
//					{
//						proto = *curequivs[i].intproto;
//						invertoutput = curequivs[i].netateoutput;
//						rotation = curequivs[i].rotation;
//						transpose = curequivs[i].transpose;
//						makeangle(rot, trn, trans);
//						xform(curequivs[i].xoffset, curequivs[i].yoffset, &xoff, &yoff, trans);
//						xoff = muldiv(xoff, lambda, WHOLE);
//						yoff = muldiv(yoff, lambda, WHOLE);
//						
//						if (transpose != 0)
//						{
//							trn = 1 - trn;
//							rot = rotation - rot;
//							if (rot < 0) rot += 3600;
//						} else
//						{
//							rot += rotation;
//							if (rot >= 3600) rot -= 3600;
//						}
//						detailbits = curequivs[i].detailbits;
//						numextrawires = curequivs[i].numextrawires;
//						extrawires = curequivs[i].extrawires;
//					}
//				}
//	
//				// now check for references to cells
//				if (proto == NONODEPROTO)
//				{
//					// find node or read it from disk
//					proto = io_suegetnodeproto(lib, keywords[1]);
//					if (proto == NONODEPROTO)
//						proto = io_suereadfromdisk(lib, keywords[1], dia);
//	
//					// set proper offsets for the cell
//					if (proto != NONODEPROTO)
//					{
//						np = iconview(proto);
//						if (np != NONODEPROTO) proto = np;
//						xoff = (proto->lowx + proto->highx) / 2;
//						yoff = (proto->lowy + proto->highy) / 2;
//						makeangle(rot, trn, trans);
//						xform(xoff, yoff, &xoff, &yoff, trans);
//					}
//				}
//	
//				// ignore "title" specifications
//				if (namesamen(keywords[1], x_("title_"), 6) == 0) continue;
//	
//				// stop now if SUE node is unknown
//				if (proto == NONODEPROTO)
//				{
//					ttyputmsg(_("Cannot make '%s' in cell %s"), keywords[1], describenodeproto(cell));
//					continue;
//				}
//	
//				// create the instance
//				defaultnodesize(proto, &px, &py);
//				px -= muldiv(xshrink, lambda, WHOLE);
//				py -= muldiv(yshrink, lambda, WHOLE);
//				lx = x - px/2 + xoff;   hx = lx + px;
//				ly = y - py/2 + yoff;   hy = ly + py;
//				ni = newnodeinst(proto, lx, hx, ly, hy, trn, rot, cell);
//				if (ni == NONODEINST) continue;
//				ni->userbits |= detailbits;
//				ni->temp1 = invertoutput;
//				if (proto->primindex == 0 && proto->cellview == el_iconview)
//					ni->userbits |= NEXPAND;
//				endobjectchange((INTBIG)ni, VNODEINST);
//				if (cell->tech == gen_tech)
//					cell->tech = whattech(cell);
//	
//				// add any extra wires to the node
//				for(i=0; i<numextrawires; i++)
//				{
//					pp = getportproto(proto, extrawires[i].portname);
//					if (pp == NOPORTPROTO) continue;
//					portposition(ni, pp, &x, &y);
//					makeangle(ni->rotation, ni->transpose, trans);
//					px = muldiv(extrawires[i].xoffset, lambda, WHOLE);
//					py = muldiv(extrawires[i].yoffset, lambda, WHOLE);
//					xform(px, py, &dx, &dy, trans);
//					defaultnodesize(sch_wirepinprim, &px, &py);
//					pinx = x + dx;   piny = y + dy;
//					nni = io_suefindpinnode(pinx, piny, cell, &npp);
//					if (nni == NONODEINST)
//					{
//						lx = pinx - px/2;   hx = lx + px;
//						ly = piny - py/2;   hy = ly + py;
//						nni = newnodeinst(sch_wirepinprim, lx, hx, ly, hy, 0, 0, cell);
//						if (nni == NONODEINST) continue;
//						npp = nni->proto->firstportproto;
//					}
//					bits = us_makearcuserbits(sch_wirearc);
//					if (x != pinx && y != piny) bits &= ~FIXANG;
//					ai = newarcinst(sch_wirearc, 0, bits, ni, pp, x, y, nni, npp, pinx, piny, cell);
//					if (ai == NOARCINST)
//					{
//						ttyputerr(_("Error adding extra wires to node %s"), keywords[1]);
//						break;
//					}
//				}
//	
//				// handle names assigned to the node
//				if (thename != 0)
//				{
//					// export a port if this is an input, output, inout
//					if (proto == sch_offpageprim && thename != 0)
//					{
//						pp = proto->firstportproto;
//						if (namesame(keywords[1], x_("output")) == 0) pp = pp->nextportproto;
//						ppt = io_suenewexport(cell, ni, pp, thename);
//						if (ppt == NOPORTPROTO)
//						{
//							ttyputmsg(_("Cell %s, line %ld, could not create port %s"), 
//								cellname, io_suelineno, thename);
//						} else
//						{
//							defaulttextdescript(ppt->textdescript, NOGEOM);
//							defaulttextsize(1, ppt->textdescript);
//							ppt->userbits = (ppt->userbits & ~STATEBITS) | type;
//							endobjectchange((INTBIG)ppt, VPORTPROTO);
//						}
//					} else
//					{
//						// just name the node
//						var = setvalkey((INTBIG)ni, VNODEINST, el_node_name_key, (INTBIG)thename,
//							VSTRING|VDISPLAY);
//						if (var != NOVARIABLE)
//							defaulttextsize(3, var->textdescript);
//						net_setnodewidth(ni);
//					}
//				}
//	
//				// count the variables
//				varcount = 0;
//				for(i=2; i<count; i += 2)
//				{
//					if (keywords[i][0] != '-') continue;
//					if (namesame(keywords[i], x_("-origin")) == 0 ||
//						namesame(keywords[i], x_("-orient")) == 0 ||
//						namesame(keywords[i], x_("-type")) == 0 ||
//						namesame(keywords[i], x_("-name")) == 0) continue;
//					varcount++;
//				}
//	
//				// add variables
//				varindex = 1;
//				varoffset = (ni->highy - ni->lowy) / (varcount+1);
//				for(i=2; i<count; i += 2)
//				{
//					if (keywords[i][0] != '-') continue;
//					if (namesame(keywords[i], x_("-origin")) == 0 ||
//						namesame(keywords[i], x_("-orient")) == 0 ||
//						namesame(keywords[i], x_("-type")) == 0 ||
//						namesame(keywords[i], x_("-name")) == 0) continue;
//					varissize = FALSE;
//					halvesize = FALSE;
//					isparam = FALSE;
//					if (namesame(&keywords[i][1], x_("w")) == 0)
//					{
//						estrcpy(suevarname, x_("ATTR_width"));
//						varissize = TRUE;
//						xpos = 2;
//						ypos = -4;
//					} else if (namesame(&keywords[i][1], x_("l")) == 0)
//					{
//						estrcpy(suevarname, x_("ATTR_length"));
//						varissize = TRUE;
//						xpos = -2;
//						ypos = -4;
//						halvesize = TRUE;
//					} else
//					{
//						esnprintf(suevarname, 200, x_("ATTR_%s"), &keywords[i][1]);
//						for(pt = suevarname; *pt != 0; pt++) if (*pt == ' ')
//						{
//							ttyputmsg(_("Cell %s, line %ld, bad variable name (%s)"), 
//								cellname, io_suelineno, suevarname);
//							break;
//						}
//						xpos = 0;
//						pos = (ni->highy - ni->lowy) / 2 - varindex * varoffset;
//						ypos = pos * 4 / lambda;
//						isparam = TRUE;
//					}
//					newtype = 0;
//					if (keywords[i][1] == 'W' && keywords[i][2] != 0)
//					{
//						infstr = initinfstr();
//						addstringtoinfstr(infstr, &keywords[i][2]);
//						addstringtoinfstr(infstr, x_(":"));
//						addstringtoinfstr(infstr, io_sueparseexpression(keywords[i+1]));
//						newaddr = (INTBIG)returninfstr(infstr);
//						newtype = VSTRING;
//					} else
//					{
//						pt = keywords[i+1];
//						len = estrlen(pt) - 1;
//						if (tolower(pt[len]) == 'u')
//						{
//							pt[len] = 0;
//							if (isanumber(pt))
//							{
//								newaddr = scalefromdispunit((float)eatof(pt), DISPUNITMIC) * WHOLE / numericlambda;
//								newtype = VFRACT;
//							}
//							pt[len] = 'u';
//						}
//						if (newtype == 0 && isanumber(pt))
//						{
//							newtype = VINTEGER;
//							newaddr = eatoi(pt);
//							for(cpt = pt; *cpt != 0; cpt++) if (*cpt == '.' || *cpt == 'e' || *cpt == 'E')
//							{
//								f = (float)eatof(pt);
//								j = (INTBIG)(f * WHOLE);
//								if (j / WHOLE == f)
//								{
//									newtype = VFRACT;
//									newaddr = j;
//								} else
//								{
//									newtype = VFLOAT;
//									newaddr = castint(f);
//								}
//								break;
//							}
//						}
//						if (newtype == 0)
//						{
//							newaddr = (INTBIG)io_sueparseexpression(pt);
//							newtype = VSTRING;
//						}
//					} 
//					// see if the string should be Java code
//					if (newtype == VSTRING)
//					{
//						for(cpt = (CHAR *)newaddr; *cpt != 0; cpt++)
//						{
//							if (*cpt == '@') break;
//							if (tolower(*cpt) == 'p' && cpt[1] == '(') break;
//						}
//						if (*cpt != 0)
//							newtype = VFLOAT|VJAVA;
//					}
//					var = setval((INTBIG)ni, VNODEINST, suevarname, newaddr,
//						newtype|VDISPLAY);
//					if (var != NOVARIABLE)
//					{
//						defaulttextdescript(var->textdescript, ni->geom);
//						varindex++;
//						TDSETOFF(var->textdescript, xpos, ypos);
//						if (halvesize)
//							TDSETSIZE(var->textdescript, TDGETSIZE(var->textdescript)/2);
//						if (isparam)
//						{
//							TDSETISPARAM(var->textdescript, VTISPARAMETER);
//							TDSETDISPPART(var->textdescript, VTDISPLAYNAMEVALUE);
//	
//							// make sure the parameter exists in the cell definition
//							cnp = contentsview(ni->proto);
//							if (cnp == NONODEPROTO) cnp = ni->proto;
//							var = getval((INTBIG)cnp, VNODEPROTO, -1, suevarname);
//							if (var == NOVARIABLE)
//							{
//								var = setval((INTBIG)cnp, VNODEPROTO, suevarname, newaddr,
//									newtype|VDISPLAY);
//								if (var != NOVARIABLE)
//								{
//									TDSETISPARAM(var->textdescript, VTISPARAMETER);
//									TDSETDISPPART(var->textdescript, VTDISPLAYNAMEVALINH);
//								}
//							}
//						}
//					}
//				}
				continue;
			}
	
			// handle "make_wire" for defining arcs
			if (keyword0.equalsIgnoreCase("make_wire"))
			{
				SueWire sw = new SueWire();
				double x = io_suemakex(TextUtils.atof((String)keywords.get(1)));
				double y = io_suemakex(TextUtils.atof((String)keywords.get(2)));
				sw.pt[0] = new Point2D.Double(x, y);
				x = io_suemakex(TextUtils.atof((String)keywords.get(3)));
				y = io_suemakex(TextUtils.atof((String)keywords.get(4)));
				sw.pt[1] = new Point2D.Double(x, y);
				sueWires.add(sw);
				continue;
			}
	
			// handle "icon_term" for defining ports in icons
			if (keyword0.equalsIgnoreCase("icon_term"))
			{
				ParseParameters pp = new ParseParameters(keywords, 1);
				NodeProto proto = Schematics.tech.busPinNode;
				double px = proto.getDefWidth();
				double py = proto.getDefHeight();
				NodeInst ni = NodeInst.makeInstance(proto, pp.pt, px, py, cell);
				if (ni == null) continue;

				PortInst pi = ni.getOnlyPortInst();
				Export ppt = Export.newInstance(cell, pi, pp.theName);
				if (ppt == null)
				{
					System.out.println("Cell " + cellName + ", line " + lineReader.getLineNumber() +
						", could not create port " + pp.theName);
				} else
				{
					ppt.setCharacteristic(pp.type);
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
				ParseParameters pp = new ParseParameters(keywords, 1);
				if (pp.theLabel == null) continue;
	
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

				NodeInst ni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, pp.pt, 0, 0, cell);
				if (ni == null) continue;
				Variable var = ni.newVar(Artwork.ART_MESSAGE, pp.theLabel);
				continue;
			}
	
			// handle "make_text" for placing strings
			if (keyword0.equalsIgnoreCase("make_text"))
			{
				// extract parameters
				ParseParameters pp = new ParseParameters(keywords, 1);
				if (pp.theText == null) continue;
	
				NodeInst ni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, pp.pt, 0, 0, cell);
				if (ni == null) continue;
				Variable var = ni.newVar(Artwork.ART_MESSAGE, pp.theText);
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
	
//		// place an icon instance in the schematic if requested
//		if (placeIcon && schemCell != NONODEPROTO &&
//			iconCell != NONODEPROTO)
//		{
//			px = iconCell->highx - iconCell->lowx;
//			py = iconCell->highy - iconCell->lowy;
//			lx = iconx - px/2;   hx = lx + px;
//			ly = icony - py/2;   hy = ly + py;
//			ni = newnodeinst(iconCell, lx, hx, ly, hy, 0, 0, schemCell);
//			if (ni != NONODEINST)
//			{
//				ni->userbits |= NEXPAND;
//				endobjectchange((INTBIG)ni, VNODEINST);
//			}
//		}
	
//		// cleanup the current cell
//		if (cell != NONODEPROTO)
//		{
//			io_sueplacewires(sueWires, sueNets, cell);
//			io_sueplacenets(sueNets, cell);
//		}
	
//		// make sure cells are the right size
//		if (schemCell != NONODEPROTO) (*el_curconstraint->solve)(schemCell);
//		if (iconCell != NONODEPROTO) (*el_curconstraint->solve)(iconCell);
	
		// return the cell
		if (schemCell != null) return schemCell;
		return iconCell;
	
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
		ParseParameters(List keywords, int start)
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
//			sw->ni[0] = sw->ni[1] = NONODEINST;
//			sw->proto = NOARCPROTO;
		}
	
		// examine all network names and assign wire types appropriately
		for(Iterator nIt = sueNets.iterator(); nIt.hasNext(); )
		{
			SueNet sn = (SueNet)nIt.next();
			for(Iterator wIt = sueWires.iterator(); wIt.hasNext(); )
			{
				SueWire sw = (SueWire)wIt.next();
//				for(i=0; i<2; i++)
//				{
//					if (sw->x[i] == sn->x && sw->y[i] == sn->y)
//					{
//						if (net_buswidth(sn->label) > 1) sw->proto = sch_busarc; else
//							sw->proto = sch_wirearc;
//					}
//				}
			}
		}
	
		// find connections that are exactly on existing nodes
		for(Iterator wIt = sueWires.iterator(); wIt.hasNext(); )
		{
			SueWire sw = (SueWire)wIt.next();
//			for(i=0; i<2; i++)
//			{
//				if (sw->ni[i] != NONODEINST) continue;
//				for(ni = cell->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//				{
//					pp = io_suewiredport(ni, &sw->x[i], &sw->y[i], sw->x[1-i], sw->y[1-i]);
//					if (pp == NOPORTPROTO) continue;
//					sw->ni[i] = ni;
//					sw->pp[i] = pp;
//	
//					// determine whether this port is a bus
//					isbus = FALSE;
//					bottomni = ni;   bottompp = pp;
//					while (bottomni->proto->primindex == 0)
//					{
//						bottomni = bottompp->subnodeinst;
//						bottompp = bottompp->subportproto;
//					}
//					if (bottomni->proto == sch_wireconprim) continue;
//					if (!isbus && ni->proto == sch_offpageprim)
//					{
//						// see if there is a bus port on this primitive
//						for(pe = ni->firstportexpinst; pe != NOPORTEXPINST; pe = pe->nextportexpinst)
//						{
//							if (net_buswidth(pe->exportproto->protoname) > 1) isbus = TRUE;
//						}
//					}
//	
//					if (isbus)
//					{
//						sw->proto = sch_busarc;
//					} else
//					{
//						if (sw->proto == NOARCPROTO)
//							sw->proto = sch_wirearc;
//					}
//				}
//			}
		}
	
		// now iteratively extend bus wires to connections with others
		boolean propagatedbus = true;
		while (propagatedbus)
		{
			propagatedbus = false;
			for(Iterator wIt = sueWires.iterator(); wIt.hasNext(); )
			{
				SueWire sw = (SueWire)wIt.next();
//				if (sw->proto != sch_busarc) continue;
				for(Iterator oWIt = sueWires.iterator(); oWIt.hasNext(); )
				{
					SueWire oSw = (SueWire)oWIt.next();
//					if (osw->proto != NOARCPROTO) continue;
//					for(i=0; i<2; i++)
//					{
//						for(j=0; j<2; j++)
//						{
//							if (sw->x[i] == osw->x[j] && sw->y[i] == osw->y[j])
//							{
//								// common point found: continue the bus request
//								osw->proto = sch_busarc;
//								propagatedbus = TRUE;
//							}
//						}
//					}
				}
			}
		}
	
		// now make pins where wires meet
		for(Iterator wIt = sueWires.iterator(); wIt.hasNext(); )
		{
			SueWire sw = (SueWire)wIt.next();
			for(int i=0; i<2; i++)
			{
//				if (sw->ni[i] != NONODEINST) continue;
//				if (sw->proto == sch_busarc) proto = sch_buspinprim; else
//					proto = sch_wirepinprim;
	
				// look at all other wires at this point and figure out type of pin to make
				for(Iterator oWIt = sueWires.iterator(); oWIt.hasNext(); )
				{
					SueWire oSw = (SueWire)oWIt.next();
					if (oSw == sw) continue;
//					for(j=0; j<2; j++)
//					{
//						if (sw->x[i] != osw->x[j] || sw->y[i] != osw->y[j]) continue;
//						if (osw->ni[j] != NONODEINST)
//						{
//							sw->ni[i] = osw->ni[j];
//							sw->pp[i] = osw->pp[j];
//							break;
//						}
//						if (osw->proto == sch_busarc) proto = sch_buspinprim;
//					}
//					if (sw->ni[i] != NONODEINST) break;
				}
	
//				// make the pin if it doesn't exist
//				if (sw->ni[i] == NONODEINST)
//				{
//					// common point found: make a pin
//					defaultnodesize(proto, &xsize , &ysize);
//					sw->ni[i] = newnodeinst(proto, sw->x[i] - xsize/2,
//						sw->x[i] + xsize/2, sw->y[i] - ysize/2,
//						sw->y[i] + ysize/2, 0, 0, cell);
//					endobjectchange((INTBIG)sw->ni[i], VNODEINST);
//					sw->pp[i] = proto->firstportproto;
//				}
	
				// put that node in all appropriate locations
				for(Iterator oWIt = sueWires.iterator(); oWIt.hasNext(); )
				{
					SueWire oSw = (SueWire)oWIt.next();
					if (oSw == sw) continue;
//					for(j=0; j<2; j++)
//					{
//						if (sw->x[i] != osw->x[j] || sw->y[i] != osw->y[j]) continue;
//						if (osw->ni[j] != NONODEINST) continue;
//						osw->ni[j] = sw->ni[i];
//						osw->pp[j] = sw->pp[i];
//					}
				}
			}
		}
	
		// make pins at all of the remaining wire ends
		for(Iterator wIt = sueWires.iterator(); wIt.hasNext(); )
		{
			SueWire sw = (SueWire)wIt.next();
//			for(i=0; i<2; i++)
//			{
//				if (sw->ni[i] != NONODEINST) continue;
//				if (!io_suefindnode(&sw->x[i], &sw->y[i], sw->x[1-i], sw->y[1-i], cell,
//					&sw->ni[i], &sw->pp[i], sw->ni[1-i], lambda))
//				{
//					if (sw->proto == sch_busarc) proto = sch_buspinprim; else
//						proto = sch_wirepinprim;
//					defaultnodesize(proto, &xsize , &ysize);
//					sw->ni[i] = newnodeinst(proto, sw->x[i] - xsize/2,
//						sw->x[i] + xsize/2, sw->y[i] - ysize/2, sw->y[i] + ysize/2,
//						0, 0, cell);
//					endobjectchange((INTBIG)sw->ni[i], VNODEINST);
//					sw->pp[i] = sw->ni[i]->proto->firstportproto;
//				}
//			}
		}
	
		// now make the connections
		for(Iterator wIt = sueWires.iterator(); wIt.hasNext(); )
		{
			SueWire sw = (SueWire)wIt.next();
//			if (sw->proto == NOARCPROTO) sw->proto = sch_wirearc;
//			wid = defaultarcwidth(sw->proto);
//	
//			// if this is a bus, make sure it can connect */
//			if (sw->proto == sch_busarc)
//			{
//				for(i=0; i<2; i++)
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
//			}
//	
//			ai = newarcinst(sw->proto, wid, us_makearcuserbits(sw->proto),
//				sw->ni[0], sw->pp[0], sw->x[0], sw->y[0],
//				sw->ni[1], sw->pp[1], sw->x[1], sw->y[1], cell);
//			if (ai == NOARCINST)
//			{
//				ttyputerr(_("Could not run a wire from %s to %s in cell %s"),
//					describenodeinst(sw->ni[0]), describenodeinst(sw->ni[1]),
//						describenodeproto(cell));
//				continue;
//			}
//	
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
//			endobjectchange((INTBIG)ai, VARCINST);
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
					String keyword = io_suecurline.substring(startIndex, i);
					keywords.add(keyword);
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
		String keyword = io_suecurline.substring(startIndex, len);
		keywords.add(keyword);
		return keywords;
	}
	
	/**
	 * Method to convert SUE X coordinate "x" to Electric coordinates
	 */
	private static double io_suemakex(double x)
	{
		return x;
	}
	
	/**
	 * Method to convert SUE Y coordinate "y" to Electric coordinates
	 */
	private static double io_suemakey(double y)
	{
		return -y;
	}
}

//	
//	/*
//	 * Routine to examine a SUE expression and add "@" in front of variable names.
//	 */
//	CHAR *io_sueparseexpression(CHAR *expression)
//	{
//		REGISTER void *infstr;
//		REGISTER CHAR *keyword;
//	
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
//	}
//	
//	/*
//	 * Routine to find cell "protoname" in library "lib".
//	 */
//	NODEPROTO *io_suegetnodeproto(LIBRARY *lib, CHAR *protoname)
//	{
//		REGISTER NODEPROTO *np;
//	
//		for(np = lib->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//			if (namesame(protoname, np->protoname) == 0) return(np);
//		return(NONODEPROTO);
//	}
//	
//	/*
//	 * Routine to create a port called "thename" on port "pp" of node "ni" in cell "cell".
//	 * The name is modified if it already exists.
//	 */
//	PORTPROTO *io_suenewexport(NODEPROTO *cell, NODEINST *ni, PORTPROTO *pp, CHAR *thename)
//	{
//		REGISTER PORTPROTO *ppt;
//		REGISTER INTBIG i;
//		REGISTER CHAR *portname, *pt;
//		CHAR numbuf[20];
//		REGISTER void *infstr;
//	
//		portname = thename;
//		for(i=0; ; i++)
//		{
//			ppt = getportproto(cell, portname);
//			if (ppt == NOPORTPROTO)
//			{
//				ppt = newportproto(cell, ni, pp, portname);
//				break;
//			}
//	
//			// make space for modified name
//			infstr = initinfstr();
//			for(pt = thename; *pt != 0 && *pt != '['; pt++)
//				addtoinfstr(infstr, *pt);
//			esnprintf(numbuf, 20, x_("-%ld"), i);
//			addstringtoinfstr(infstr, numbuf);
//			for( ; *pt != 0; pt++)
//				addtoinfstr(infstr, *pt);
//			portname = returninfstr(infstr);
//		}
//		return(ppt);
//	}
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
//	
//	/*
//	 * Routine to find the port on node "ni" that attaches to the wire from (x,y) to (ox,oy).
//	 * Returns NOPORTPROTO if not found.
//	 */
//	PORTPROTO *io_suewiredport(NODEINST *ni, INTBIG *x, INTBIG *y, INTBIG ox, INTBIG oy)
//	{
//		REGISTER PORTPROTO *pp, *bestpp;
//		REGISTER INTBIG dist, bestdist;
//		INTBIG px, py;
//		static POLYGON *poly = NOPOLYGON;
//	
//		// make sure there is a polygon
//		(void)needstaticpolygon(&poly, 4, io_tool->cluster);
//		for(pp = ni->proto->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//		{
//			shapeportpoly(ni, pp, poly, FALSE);
//			if (isinside(*x, *y, poly)) return(pp);
//		}
//		if ((ni->lowx+ni->highx) / 2 != *x ||
//			(ni->lowy+ni->highy) / 2 != *y) return(NOPORTPROTO);
//	
//		// find port that is closest to OTHER end
//		bestdist = MAXINTBIG;
//		bestpp = NOPORTPROTO;
//		for(pp = ni->proto->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//		{
//			portposition(ni, pp, &px, &py);
//			dist = computedistance(px, py, ox, oy);
//			if (dist > bestdist) continue;
//			bestdist = dist;
//			bestpp = pp;
//		}
//		portposition(ni, bestpp, x, y);
//		return(bestpp);
//	}
//	
//	/*
//	 * Routine to find the pin at (x, y) and return it.
//	 */
//	NODEINST *io_suefindpinnode(INTBIG x, INTBIG y, NODEPROTO *np, PORTPROTO **thepp)
//	{
//		REGISTER GEOM *geom;
//		REGISTER INTBIG sea;
//		REGISTER NODEINST *ni;
//		REGISTER PORTPROTO *pp;
//		INTBIG px, py;
//	
//		*thepp = NOPORTPROTO;
//		sea = initsearch(x, x, y, y, np);
//		for(;;)
//		{
//			geom = nextobject(sea);
//			if (geom == NOGEOM) break;
//			if (!geom->entryisnode) continue;
//			ni = geom->entryaddr.ni;
//	
//			// find closest port
//			for(pp = ni->proto->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//			{
//				// make sure there is a polygon
//				portposition(ni, pp, &px, &py);
//				if (px == x && py == y)
//				{
//					*thepp = pp;
//					termsearch(sea);
//					return(ni);
//				}
//			}
//		}
//		return(NONODEINST);
//	}
//	
//	/*
//	 * Routine to find the node at (x, y) and return it.
//	 */
//	BOOLEAN io_suefindnode(INTBIG *x, INTBIG *y, INTBIG ox, INTBIG oy,
//		NODEPROTO *np, NODEINST **rni, PORTPROTO **rpp, NODEINST *notthisnode, INTBIG lambda)
//	{
//		REGISTER GEOM *geom;
//		REGISTER INTBIG sea;
//		REGISTER NODEINST *ni, *bestni;
//		INTBIG plx, phx, ply, phy;
//		REGISTER INTBIG dist, bestdist, thisx, thisy, bestx, besty, slop;
//		REGISTER PORTPROTO *pp, *bestpp;
//		static POLYGON *poly = NOPOLYGON;
//	
//		slop = lambda * 10;
//		sea = initsearch(*x-slop, *x+slop, *y-slop, *y+slop, np);
//		bestpp = NOPORTPROTO;
//		for(;;)
//		{
//			geom = nextobject(sea);
//			if (geom == NOGEOM) break;
//			if (!geom->entryisnode) continue;
//			ni = geom->entryaddr.ni;
//			if (ni == notthisnode) continue;
//	
//			// ignore pins
//			if (ni->proto == sch_wirepinprim) continue;
//	
//			// find closest port
//			for(pp = ni->proto->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//			{
//				// make sure there is a polygon
//				(void)needstaticpolygon(&poly, 4, io_tool->cluster);
//	
//				// get the polygon describing the port
//				shapeportpoly(ni, pp, poly, FALSE);
//				getbbox(poly, &plx, &phx, &ply, &phy);
//	
//				// find out if the line crosses the polygon
//				if (*x == ox)
//				{
//					// line is vertical: look for intersection with polygon
//					if (ox < plx || ox > phx) continue;
//					thisx = ox;
//					thisy = (ply+phy)/2;
//				} else if (*y == oy)
//				{
//					// line is horizontal: look for intersection with polygon
//					if (oy < ply || oy > phy) continue;
//					thisx = (plx+phx)/2;
//					thisy = oy;
//				} else
//				{
//					if (!isinside(ox, oy, poly)) continue;
//					thisx = ox;
//					thisy = oy;
//				}
//	
//				dist = computedistance(ox, oy, thisx, thisy);
//	
//				// LINTED "bestdist" used in proper order
//				if (bestpp == NOPORTPROTO || dist < bestdist)
//				{
//					bestpp = pp;
//					bestni = ni;
//					bestdist = dist;
//					bestx = thisx;
//					besty = thisy;
//				}
//			}
//		}
//	
//		// report the hit
//		if (bestpp == NOPORTPROTO) return(FALSE);
//		*rni = bestni;   *rpp = bestpp;
//		*x = bestx;      *y = besty;
//		return(TRUE);
//	}
//	
//	/*
//	 * Routine to find the SUE file "name" on disk, and read it into library "lib".
//	 * Returns NONODEPROTO if the file is not found or not read properly.
//	 */
//	NODEPROTO *io_suereadfromdisk(LIBRARY *lib, CHAR *name, void *dia)
//	{
//		REGISTER INTBIG i, savelineno, filepos, savefilesize;
//		CHAR suevarname[200], subfilename[300], savesuelastline[MAXLINE], saveorigline[MAXLINE],
//			lastprogressmsg[MAXLINE], savecurline[MAXLINE], *truename;
//		REGISTER FILE *f, *savefp;
//		REGISTER NODEPROTO *proto;
//	
//		// look for another "sue" file that describes this cell
//		for(Iterator it = io_suedirectories.iterator(); it.hasNext(); )
//		{
//			String directory = (String)it.next();
//			estrcpy(subfilename, directory);
//			estrcat(subfilename, name);
//			estrcat(subfilename, x_(".sue"));
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
//		}
//		return(NONODEPROTO);
//	}
