/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DXF.java
 * Input/output tool: DXF input
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

import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.TextUtils.UnitScale;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.io.IOTool;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * This class reads files in DEF files.
 */
public class DXF extends Input
{
	private static class DXFLAYER
	{
		String   layerName;
		int		 layerColor;
		double   layerRed, layerGreen, layerBlue;
		DXFLAYER next;
	};

	private static class FORWARDREF
	{
		String     refname;
		Cell       parent;
		double     x, y;
		int        rot;
		int        xrep, yrep;
		double     xspa, yspa;
		double     xsca, ysca;
		FORWARDREF nextforwardref;
	};

	private int                 io_dxfgroupid;
	private int                 io_dxfignoredpoints, io_dxfignoredattribdefs, io_dxfignoredattributes;
	private int                 io_dxfreadpolylines, io_dxfreadlines, io_dxfreadcircles, io_dxfreadsolids,
						        io_dxfread3dfaces, io_dxfreadarcs, io_dxfreadinserts, io_dxfreadtexts;
	private String              io_dxfline;
	private DXFLAYER            io_dxffirstlayer;
	private FORWARDREF          io_dxffirstforwardref;
	private Cell                io_dxfmaincell;
	private Cell                io_dxfcurcell;
	private boolean             io_dxfpairvalid;
	private List                io_dxfheadertext;
	private List                io_dxfheaderid;
	private int                 io_dxfinputmode;			/* 0: pairs not read, 1: normal pairs, 2: blank lines */
	private HashSet             validLayerNames;
	private HashSet             ignoredLayerNames;
//	static INTBIG               io_dxflayerkey = 0;
	private TextUtils.UnitScale io_dxfdispunit;

	/**
	 * Method to import a library from disk.
	 * @param lib the library to fill
	 * @return true on error.
	 */
	protected boolean importALibrary(Library lib)
	{
		boolean ret = false;
		try
		{
			ret = io_readdxflibrary(lib);
		} catch (IOException e)
		{
		}
		return ret;
	}
	
	/**
	 * Method to read the DXF file into library "lib".  Returns true on error.
	 */
	private boolean io_readdxflibrary(Library lib)
		throws IOException
	{
		// parameters for reading files
//		if (io_dxflayerkey == 0) io_dxflayerkey = makekey(x_("IO_dxf_layer"));
	
		// set the scale
		io_dxfsetcurunits();
	
		// examine technology for acceptable DXF layer names
		io_dxfgetacceptablelayers();
	
		// make the only cell in this library
		io_dxfmaincell = Cell.makeInstance(lib, lib.getName());
		if (io_dxfmaincell == null) return true;
		lib.setCurCell(io_dxfmaincell);
		io_dxfcurcell = io_dxfmaincell;
		io_dxfheaderid = new ArrayList();
		io_dxfheadertext = new ArrayList();
		ignoredLayerNames = new HashSet();
	
		// read the file
		io_dxfpairvalid = false;
		boolean err = false;
		io_dxfclearlayers();
		io_dxffirstforwardref = null;
		io_dxfignoredpoints = io_dxfignoredattributes = io_dxfignoredattribdefs = 0;
		io_dxfreadpolylines = io_dxfreadlines = io_dxfreadcircles = io_dxfreadsolids = 0;
		io_dxfread3dfaces = io_dxfreadarcs = io_dxfreadinserts = io_dxfreadtexts = 0;
		io_dxfinputmode = 0;
		for(;;)
		{
			if (io_dxfgetnextpair()) break;
	
			// must have section change here
			if (groupID != 0)
			{
				System.out.println("Expected group 0 (start section) at line " + lineReader.getLineNumber());
				break;
			}
	
			if (text.equals("EOF")) break;
	
			if (text.equals("SECTION"))
			{
				// see what section is coming
				if (io_dxfgetnextpair()) break;
				if (groupID != 2)
				{
					System.out.println("Expected group 2 (name) at line " + lineReader.getLineNumber());
					err = true;
					break;
				}
				if (text.equals("HEADER"))
				{
					if (err = io_dxfreadheadersection()) break;
					continue;
				}
				if (text.equals("TABLES"))
				{
					if (err = io_dxfreadtablessection()) break;
					continue;
				}
				if (text.equals("BLOCKS"))
				{
					if (err = io_dxfreadentities(lib)) break;
					continue;
				}
				if (text.equals("ENTITIES"))
				{
					if (err = io_dxfreadentities(lib)) break;
					continue;
				}
				if (text.equals("CLASSES"))
				{
					if (err = io_dxfignoresection()) break;
					continue;
				}
				if (text.equals("OBJECTS"))
				{
					if (err = io_dxfignoresection()) break;
					continue;
				}
			}
			System.out.println("Unknown section name (" + text + ") at line " + lineReader.getLineNumber());
			err = true;
			break;
		}
	
		// insert forward references
		for(FORWARDREF fr = io_dxffirstforwardref; fr != null; fr = fr.nextforwardref)
		{
			// have to search by hand because of weird prototype names
			Cell found = null;
			for(Iterator it = lib.getCells(); it.hasNext(); )
			{
				Cell cell = (Cell)it.next();
				if (cell.getName().equals(fr.refname)) { found = cell;   break; }
			}
			if (found == null)
			{
				System.out.println("Cannot find block '" + fr.refname + "'");
				continue;
			}
			if (IOTool.isDXFInputFlattensHierarchy())
			{
//				if (io_dxfextractinsert(found, fr->x, fr->y, fr->xsca, fr->ysca, fr->rot, fr->parent) != 0) return true;
			} else
			{
				if (fr.xsca != 1.0 || fr.ysca != 1.0)
				{
					found = io_dxfgetscaledcell(found, fr.xsca, fr.ysca);
					if (found == null) return true;
				}
				Rectangle2D bounds = found.getBounds();
				NodeInst ni = NodeInst.makeInstance(found, new Point2D.Double(fr.x, fr.y), bounds.getWidth(), bounds.getHeight(), fr.parent, fr.rot*10, null, 0);
				if (ni == null) return true;
				ni.setExpanded();
			}
		}
	
		// save header with library
		if (io_dxfheaderid.size() > 0)
		{
//			setval((INTBIG)lib, VLIBRARY, x_("IO_dxf_header_text"), (INTBIG)io_dxfheadertext,
//				VSTRING|VISARRAY|(io_dxfheadercount<<VLENGTHSH));
//			setval((INTBIG)lib, VLIBRARY, x_("IO_dxf_header_ID"), (INTBIG)io_dxfheaderid,
//				VSHORT|VISARRAY|(io_dxfheadercount<<VLENGTHSH));
		}
	
		if (io_dxfreadpolylines > 0 || io_dxfreadlines > 0 || io_dxfreadcircles > 0 ||
			io_dxfreadsolids > 0 || io_dxfread3dfaces > 0 || io_dxfreadarcs > 0 ||
			io_dxfreadtexts > 0 || io_dxfreadinserts > 0)
		{
			String warning = "Read";
			boolean first = true;
			if (io_dxfreadpolylines > 0)
			{
				if (first) warning += ",";	first = false;
				warning += " " + io_dxfreadpolylines + " polylines";
			}
			if (io_dxfreadlines > 0)
			{
				if (first) warning += ",";	first = false;
				warning += " " + io_dxfreadlines + " lines";
			}
			if (io_dxfreadcircles > 0)
			{
				if (first) warning += ",";	first = false;
				warning += " " + io_dxfreadcircles + " circles";
			}
			if (io_dxfreadsolids > 0)
			{
				if (first) warning += ",";	first = false;
				warning += " " + io_dxfreadsolids + " solids";
			}
			if (io_dxfread3dfaces > 0)
			{
				if (first) warning += ",";	first = false;
				warning += " " + io_dxfread3dfaces + " 3d faces";
			}
			if (io_dxfreadarcs > 0)
			{
				if (first) warning += ",";	first = false;
				warning += " " + io_dxfreadarcs + " arcs";
			}
			if (io_dxfreadtexts > 0)
			{
				if (first) warning += ",";	first = false;
				warning += " " + io_dxfreadtexts + " texts";
			}
			if (io_dxfreadinserts > 0)
			{
				if (first) warning += ",";	first = false;
				warning += " " + io_dxfreadinserts + " inserts";
			}
			System.out.println(warning);
		}
	
		if (io_dxfignoredpoints > 0 || io_dxfignoredattributes > 0 || io_dxfignoredattribdefs > 0)
		{
			String warning = "Ignored";
			boolean first = true;
			if (io_dxfignoredpoints > 0)
			{
				if (first) warning += ",";	first = false;
				warning += " " + io_dxfignoredpoints + " points";
			}
			if (io_dxfignoredattributes > 0)
			{
				if (first) warning += ",";	first = false;
				warning += " " + io_dxfignoredattributes + " attributes";
			}
			if (io_dxfignoredattribdefs > 0)
			{
				if (first) warning += ",";	first = false;
				warning += " " + io_dxfignoredattribdefs + " attribute definitions";
			}
			System.out.println(warning);
		}
	
		// say which layers were ignored
		if (ignoredLayerNames.size() > 0)
		{
			String warning = "Ignored layers ";
			boolean first = true;
			for(Iterator it = ignoredLayerNames.iterator(); it.hasNext(); )
			{
				String name = (String)it.next();
				if (!first) warning += ", ";
				first = false;
				warning += name;
			}
			System.out.println(warning);
		}
	
		return err;
	}

	private int groupID;
	private String text;

	/**
	 * Method to read the next group ID and content pair from the file.
	 * Returns true on end-of-file.
	 */
	private boolean io_dxfgetnextpair()
		throws IOException
	{
		if (io_dxfpairvalid)
		{
			text = io_dxfline;
			groupID = io_dxfgroupid;
			io_dxfpairvalid = false;
			return false;
		}
	
		for(;;)
		{
			// read a line and get the group ID
			io_dxfline = io_dxfgetnextline(false);
			if (io_dxfline == null)
			{
				System.out.println("Unexpected end-of-file at line " + lineReader.getLineNumber());
				return true;
			}
			String groupLine = io_dxfline.trim();
			if (!TextUtils.isANumber(groupLine))
			{
				System.out.println("Invalid group ID on line " + lineReader.getLineNumber() + " (" + io_dxfline + ")");
				return true;
			}
			groupID = TextUtils.atoi(groupLine);
	
			// ignore blank line if file is double-spaced
			if (io_dxfinputmode == 2) io_dxfgetnextline(true);
	
			// read a line and get the text
			io_dxfline = io_dxfgetnextline(true);
			if (io_dxfline == null)
			{
				System.out.println("Unexpected end-of-file at line " + lineReader.getLineNumber());
				return true;
			}
			text = io_dxfline;
	
			// ignore blank line if file is double-spaced
			if (io_dxfinputmode == 2) io_dxfgetnextline(true);
	
			if (io_dxfinputmode == 0)
			{
				// see if file is single or double spaced
				if (io_dxfline.length() != 0) io_dxfinputmode = 1; else
				{
					io_dxfinputmode = 2;
					io_dxfline = io_dxfgetnextline(true);
					if (io_dxfline == null)
					{
						System.out.println("Unexpected end-of-file at line " + lineReader.getLineNumber());
						return true;
					}
					text = io_dxfline;
					io_dxfgetnextline(true);
				}
			}
	
			// continue reading if a comment, otherwise quit
			if (groupID != 999) break;
		}
	
		return false;
	}
	
	private String io_dxfgetnextline(boolean canBeBlank)
		throws IOException
	{
		for(;;)
		{
			String text = lineReader.readLine();
			if (canBeBlank || text.length() != 0) return text;
		}
	}
	
	/****************************************** READING SECTIONS ******************************************/
	
	private boolean io_dxfreadheadersection()
		throws IOException
	{
		// just save everything until the end-of-section
		for(int line=0; ; line++)
		{
			if (io_dxfgetnextpair()) return true;
			if (groupID == 0 && text.equals("ENDSEC")) break;

			// save it
			io_dxfheaderid.add(new Integer(groupID));
			io_dxfheadertext.add(text);
		}
		return false;
	}
	
	private boolean io_dxfreadtablessection()
		throws IOException
	{
		// just ignore everything until the end-of-section
		for(;;)
		{
			if (io_dxfgetnextpair()) return true;
	
			// quit now if at the end of the table section
			if (groupID == 0 && text.equals("ENDSEC")) break;
	
			// must be a 'TABLE' declaration
			if (groupID != 0 || !text.equals("TABLE")) continue;
	
			// a table: see what kind it is
			if (io_dxfgetnextpair()) return true;
			if (groupID != 2 || !text.equals("LAYER")) continue;
	
			// a layer table: ignore the size information
			if (io_dxfgetnextpair()) return true;
			if (groupID != 70) continue;
	
			// read the layers
			DXFLAYER layer = null;
			for(;;)
			{
				if (io_dxfgetnextpair()) return true;
				if (groupID == 0 && text.equals("ENDTAB")) break;
				if (groupID == 0 && text.equals("LAYER"))
				{
					// make a new layer
					layer = new DXFLAYER();
					layer.layerName = null;
					layer.layerColor = -1;
					layer.layerRed = 1.0;
					layer.layerGreen = 1.0;
					layer.layerBlue = 1.0;
					layer.next = io_dxffirstlayer;
					io_dxffirstlayer = layer;
				}
				if (groupID == 2 && layer != null)
				{
					layer.layerName = text;
				}
				if (groupID == 62 && layer != null)
				{
					layer.layerColor = TextUtils.atoi(text);
					DXFLAYER found = null;
					for(DXFLAYER l = io_dxffirstlayer; l != null; l = l.next)
					{
						if (l == layer) continue;
						if (l.layerColor == layer.layerColor) { found = l;   break; }
					}
					if (found != null)
					{
						layer.layerRed = found.layerRed;
						layer.layerGreen = found.layerGreen;
						layer.layerBlue = found.layerBlue;
					} else
					{
						switch (layer.layerColor)
						{
							case 1:			    // red
								layer.layerRed = 1.0;    layer.layerGreen = 0.0;    layer.layerBlue = 0.0;
								break;
							case 2:			    // yellow
								layer.layerRed = 1.0;    layer.layerGreen = 1.0;    layer.layerBlue = 0.0;
								break;
							case 3:			    // green
								layer.layerRed = 0.0;    layer.layerGreen = 1.0;    layer.layerBlue = 0.0;
								break;
							case 4:			    // cyan
								layer.layerRed = 0.0;    layer.layerGreen = 1.0;    layer.layerBlue = 1.0;
								break;
							case 5:			    // blue
								layer.layerRed = 0.0;    layer.layerGreen = 0.0;    layer.layerBlue = 1.0;
								break;
							case 6:			    // magenta
								layer.layerRed = 1.0;    layer.layerGreen = 0.0;    layer.layerBlue = 1.0;
								break;
							case 7:			    // white (well, gray)
								layer.layerRed = 0.75;    layer.layerGreen = 0.75;    layer.layerBlue = 0.75;
								break;
							default:			// unknown layer
								layer.layerRed = Math.random();
								layer.layerGreen = Math.random();
								layer.layerBlue = Math.random();
								break;
						}
					}
				}
			}
		}
		return false;
	}
	
	private boolean io_dxfignoresection()
		throws IOException
	{
		// just ignore everything until the end-of-section
		for(;;)
		{
			if (io_dxfgetnextpair()) return true;
			if (groupID == 0 && text.equals("ENDSEC")) break;
		}
		return false;
	}
	
	/****************************************** READING ENTITIES ******************************************/
	
	private boolean io_dxfreadentities(Library lib)
		throws IOException
	{
		// read the blocks/entities section
		for(;;)
		{
			if (io_dxfgetnextpair()) return true;
			if (groupID != 0)
			{
				System.out.println("Unknown group code (" + groupID + ") at line " + lineReader.getLineNumber());
				return true;
			}
	
			if (text.equals("ARC"))
			{
				if (io_dxfreadarcentity()) return true;
				continue;
			}
			if (text.equals("ATTDEF"))
			{
				io_dxfignoreentity();
				io_dxfignoredattribdefs++;
				continue;
			}
			if (text.equals("ATTRIB"))
			{
				io_dxfignoreentity();
				io_dxfignoredattributes++;
				continue;
			}
			if (text.equals("BLOCK"))
			{
				String msg = io_dxfreadblock();
				if (msg == null) return true;
				io_dxfcurcell = Cell.makeInstance(lib, io_dxfblockname(msg));
				if (io_dxfcurcell == null) return true;
				continue;
			}
			if (text.equals("CIRCLE"))
			{
				if (io_dxfreadcircleentity()) return true;
				continue;
			}
			if (text.equals("ENDBLK"))
			{
				io_dxfignoreentity();
				io_dxfcurcell = io_dxfmaincell;
				continue;
			}
			if (text.equals("ENDSEC"))
			{
				break;
			}
			if (text.equals("INSERT"))
			{
				if (io_dxfreadinsertentity()) return true;
				continue;
			}
			if (text.equals("LINE"))
			{
				if (io_dxfreadlineentity()) return true;
				continue;
			}
			if (text.equals("POINT"))
			{
				io_dxfignoreentity();
				io_dxfignoredpoints++;
				continue;
			}
			if (text.equals("POLYLINE"))
			{
				if (io_dxfreadpolylineentity()) return true;
				continue;
			}
			if (text.equals("SEQEND"))
			{
				io_dxfignoreentity();
				continue;
			}
			if (text.equals("SOLID"))
			{
				if (io_dxfreadsolidentity()) return true;
				continue;
			}
			if (text.equals("TEXT"))
			{
				if (io_dxfreadtextentity()) return true;
				continue;
			}
			if (text.equals("VIEWPORT"))
			{
				io_dxfignoreentity();
				continue;
			}
			if (text.equals("3DFACE"))
			{
				if (io_dxfread3dfaceentity()) return true;
				continue;
			}
			System.out.println("Unknown entity type (" + text + ") at line " + lineReader.getLineNumber());
			return true;
		}
		return false;
	}

	private double scaleString(String text)
	{
		double v = TextUtils.atof(text);
		return TextUtils.convertFromDistance(v, Artwork.tech, io_dxfdispunit);
	}

	private boolean io_dxfreadarcentity()
		throws IOException
	{
		DXFLAYER layer = null;
		double x = 0, y = 0, z = 0;
		double rad = 0;
		double sAngle = 0, eAngle = 0;
		for(;;)
		{
			if (io_dxfgetnextpair()) return true;
			switch (groupID)
			{
				case 8:  layer = io_dxfgetlayer(text);    break;
				case 10: x = scaleString(text);           break;
				case 20: y = scaleString(text);           break;
				case 30: z = scaleString(text);           break;
				case 40: rad = scaleString(text);         break;
				case 50: sAngle = TextUtils.atof(text);   break;
				case 51: eAngle = TextUtils.atof(text);   break;
			}
			if (groupID == 0)
			{
				io_dxfpushpair(groupID, text);
				break;
			}
		}
		if (!io_dxfacceptablelayer(layer)) return false;
		if (sAngle >= 360.0) sAngle -= 360.0;
		int iangle = (int)(sAngle * 10.0);
		NodeInst ni = NodeInst.makeInstance(Artwork.tech.circleNode, new Point2D.Double(x, y), rad*2, rad*2, io_dxfcurcell, iangle%3600, null, 0);
		if (ni == null) return true;
		if (sAngle > eAngle) eAngle += 360.0;
		double startoffset = sAngle;
		startoffset -= (double)iangle / 10.0;
//		setarcdegrees(ni, startoffset * EPI / 180.0, (eAngle-sAngle) * EPI / 180.0);
//		setvalkey((INTBIG)ni, VNODEINST, io_dxflayerkey, (INTBIG)layer.layerName, VSTRING);
		io_dxfreadarcs++;
		return false;
	}
	
	private String io_dxfreadblock()
		throws IOException
	{
		String saveMsg = null;
		for(;;)
		{
			if (io_dxfgetnextpair()) return null;
			if (groupID == 2) saveMsg = text; else
			if (groupID == 0)
			{
				io_dxfpushpair(groupID, text);
				break;
			}
		}
		return saveMsg;
	}
	
	private boolean io_dxfreadcircleentity()
		throws IOException
	{
		DXFLAYER layer = null;
		double x = 0, y = 0, z = 0;
		double rad = 0;
		for(;;)
		{
			if (io_dxfgetnextpair()) return true;
			switch (groupID)
			{
				case 8:  layer = io_dxfgetlayer(text);    break;
				case 10: x = scaleString(text);           break;
				case 20: y = scaleString(text);           break;
				case 30: z = scaleString(text);           break;
				case 40: rad = scaleString(text);         break;
			}
			if (groupID == 0)
			{
				io_dxfpushpair(groupID, text);
				break;
			}
		}
		if (!io_dxfacceptablelayer(layer)) return false;
		NodeInst ni = NodeInst.makeInstance(Artwork.tech.circleNode, new Point2D.Double(x, y), rad*2, rad*2, io_dxfcurcell);
		if (ni == null) return true;
//		setvalkey((INTBIG)ni, VNODEINST, io_dxflayerkey, (INTBIG)layer->layerName, VSTRING);
		io_dxfreadcircles++;
		return false;
	}
	
	private boolean io_dxfreadinsertentity()
		throws IOException
	{
		DXFLAYER layer = null;
		int rot = 0;
		String name = null;
		int xrep = 1, yrep = 1;
		double x = 0, y = 0, z = 0;
		double xspa = 0, yspa = 0;
		double xsca = 1, ysca = 1;
		for(;;)
		{
			if (io_dxfgetnextpair()) return true;
			switch (groupID)
			{
				case 8:  layer = io_dxfgetlayer(text);    break;
				case 10: x = scaleString(text);           break;
				case 20: y = scaleString(text);           break;
				case 30: z = scaleString(text);           break;
				case 50: rot = TextUtils.atoi(text);      break;
				case 41: xsca = TextUtils.atof(text);     break;
				case 42: ysca = TextUtils.atof(text);     break;
				case 70: xrep = TextUtils.atoi(text);     break;
				case 71: yrep = TextUtils.atoi(text);     break;
				case 44: xspa = scaleString(text);        break;
				case 45: yspa = scaleString(text);        break;
				case 2:  name = text;                     break;
			}
			if (groupID == 0)
			{
				io_dxfpushpair(groupID, text);
				break;
			}
		}

		
		String pt = io_dxfblockname(name);
		if (pt != null)
		{
			if (xrep != 1 || yrep != 1)
			{
				System.out.println("Cannot insert block '" + pt + "' repeated " + xrep + "x" + yrep + " times");
				return false;
			}
	
			// have to search by hand because of weird prototype names
			Cell found = null;
			for(Iterator it = lib.getCells(); it.hasNext(); )
			{
				Cell np = (Cell)it.next();
				if (np.getName().equals(pt)) { found = np;   break; }
			}
			if (found == null)
			{
				FORWARDREF fr = new FORWARDREF();
				fr.refname = pt;
				fr.parent = io_dxfcurcell;
				fr.x = x;		fr.y = y;
				fr.rot = rot;
				fr.xrep = xrep;	fr.yrep = yrep;
				fr.xspa = xspa;	fr.yspa = yspa;
				fr.xsca = xsca;	fr.ysca = ysca;
				fr.nextforwardref = io_dxffirstforwardref;
				io_dxffirstforwardref = fr;
				return false;
			}
	
			if (IOTool.isDXFInputFlattensHierarchy())
			{
				if (io_dxfextractinsert(found, x, y, xsca, ysca, rot, io_dxfcurcell)) return true;
			} else
			{
				if (xsca != 1.0 || ysca != 1.0)
				{
					found = io_dxfgetscaledcell(found, xsca, ysca);
					if (found == null) return true;
				}
				double sX = found.getDefWidth();
				double sY = found.getDefHeight();
				NodeInst ni = NodeInst.makeInstance(found, new Point2D.Double(x, y), sX, sY, io_dxfcurcell, rot*10, null, 0);
				if (ni == null) return true;
				ni.setExpanded();
			}
		}
		io_dxfreadinserts++;
		return false;
	}
	
	private boolean io_dxfreadlineentity()
		throws IOException
	{
		DXFLAYER layer = null;
		int lineType = 0;
		double x1 = 0, y1 = 0, z1 = 0;
		double x2 = 0, y2 = 0, z2 = 0;
		for(;;)
		{
			if (io_dxfgetnextpair()) return true;
			switch (groupID)
			{
				case 8:  layer = io_dxfgetlayer(text);     break;
				case 10: x1 = scaleString(text);           break;
				case 20: y1 = scaleString(text);           break;
				case 30: z1 = scaleString(text);           break;
				case 11: x2 = scaleString(text);           break;
				case 21: y2 = scaleString(text);           break;
				case 31: z2 = scaleString(text);           break;
			}
			if (groupID == 0)
			{
				io_dxfpushpair(groupID, text);
				break;
			}
		}
		if (!io_dxfacceptablelayer(layer)) return false;
		double cX = (x1 + x2) / 2;
		double cY = (y1 + y2) / 2;
		double sX = Math.abs(x1 - x2);
		double sY = Math.abs(y1 - y2);
		NodeProto np = Artwork.tech.openedDashedPolygonNode;
		if (lineType == 0) np = Artwork.tech.openedPolygonNode;
		NodeInst ni = NodeInst.makeInstance(np, new Point2D.Double(cX, cY), sX, sY, io_dxfcurcell);
		if (ni == null) return true;
		Point2D [] points = new Point2D[2];
		points[0] = new Point2D.Double(x1 - cX, y1 - cY);
		points[1] = new Point2D.Double(x2 - cX, y2 - cY);
		ni.newVar(NodeInst.TRACE, points);
//		setvalkey((INTBIG)ni, VNODEINST, io_dxflayerkey, (INTBIG)layer->layerName, VSTRING);
		io_dxfreadlines++;
		return false;
	}

	private static class PolyPoint
	{
		double x, y, z;
		double bulge;
	}

	private boolean io_dxfreadpolylineentity()
		throws IOException
	{
		boolean closed = false;
		DXFLAYER layer = null;
		int lineType = 0;
		boolean inEnd = false;
		List polyPoints = new ArrayList();
		PolyPoint curPP = null;
		boolean hasBulgeInfo = false;
		for(;;)
		{
			if (io_dxfgetnextpair()) return true;
			if (groupID == 8)
			{
				layer = io_dxfgetlayer(text);
				continue;
			}
			
			if (groupID == 10)
			{
				if (curPP != null) curPP.x = scaleString(text);
				continue;
			}
			if (groupID == 20)
			{
				if (curPP != null) curPP.y = scaleString(text);
				continue;
			}
			if (groupID == 30)
			{
				if (curPP != null) curPP.z = scaleString(text);
				continue;
			}
			if (groupID == 42)
			{
				if (curPP != null)
				{
					curPP.bulge = TextUtils.atof(text);
					if (curPP.bulge != 0) hasBulgeInfo = true;
				}
				continue;
			}
			if (groupID == 70)
			{
				int i = TextUtils.atoi(text);
				if ((i&1) != 0) closed = true;
				continue;
			}

			if (groupID == 0)
			{
				if (inEnd)
				{
					io_dxfpushpair(groupID, text);
					break;
				}
				if (text.equals("SEQEND"))
				{
					inEnd = true;
					continue;
				}
				if (text.equals("VERTEX"))
				{
					curPP = new PolyPoint();
					curPP.bulge = 0;
					polyPoints.add(curPP);
				}
				continue;
			}
		}

		int count = polyPoints.size();
		if (io_dxfacceptablelayer(layer) && count >= 3)
		{
			// see if there is bulge information
			if (hasBulgeInfo)
			{
				// handle bulges
				int start = 1;
				if (closed) start = 0;
				for(int i=start; i<count; i++)
				{
					int last = i - 1;
					if (i == 0) last = count-1;
					PolyPoint pp = (PolyPoint)polyPoints.get(i);
					PolyPoint lastPp = (PolyPoint)polyPoints.get(last);
					double x1 = lastPp.x;   double y1 = lastPp.y;
					double x2 = pp.x;       double y2 = pp.y;
					if (lastPp.bulge != 0.0)
					{
						// special case the semicircle bulges
						if (Math.abs(lastPp.bulge) == 1.0)
						{
							double xc = (x1 + x2) / 2;
							double yc = (y1 + y2) / 2;
							if ((y1 == yc && x1 == xc) || (y2 == yc && x2 == xc))
							{
								System.out.println("Domain error in polyline bulge computation");
								continue;
							}
							double sa = Math.atan2(y1-yc, x1-xc);
							double ea = Math.atan2(y2-yc, x2-xc);
							if (lastPp.bulge < 0.0)
							{
								double r2 = sa;   sa = ea;   ea = r2;
							}
							if (sa < 0.0) sa += 2.0 * Math.PI;
							sa = sa * 1800.0 / Math.PI;
							int iangle = (int)sa;
							double rad = new Point2D.Double(xc, yc).distance(new Point2D.Double(x1, y1));
							NodeInst ni = NodeInst.makeInstance(Artwork.tech.circleNode, new Point2D.Double(xc, yc), rad*2, rad*2, io_dxfcurcell, iangle, null, 0);
							if (ni == null) return true;
							double startoffset = sa;
							startoffset -= iangle;
//							setarcdegrees(ni, startoffset * EPI / 1800.0, EPI);
//							setvalkey((INTBIG)ni, VNODEINST, io_dxflayerkey, (INTBIG)layer->layerName, VSTRING);
							continue;
						}

						// compute half distance between the points
						double x01 = x1;   double y01 = y1;
						double x02 = x2;   double y02 = y2;
						double dx = x02 - x01;   double dy = y02 - y01;
						double dist = Math.sqrt(dx*dx + dy*dy);

						// compute radius of arc (bulge is tangent of 1/4 of included arc angle)
						double incangle = Math.atan(lastPp.bulge) * 4.0;
						double arcrad = Math.abs((dist / 2.0) / Math.sin(incangle / 2.0));
						int rad = (int)arcrad;

						// prepare to compute the two circle centers
						double r2 = arcrad*arcrad;
						double delta_1 = -dist / 2.0;
						double delta_12 = delta_1 * delta_1;
						double delta_2 = Math.sqrt(r2 - delta_12);

						// pick one center, according to bulge sign
						double bulgesign = lastPp.bulge;
						if (Math.abs(bulgesign) > 1.0) bulgesign = -bulgesign;
						double xcf = 0, ycf = 0;
						if (bulgesign > 0.0)
						{
							xcf = x02 + ((delta_1 * (x02-x01)) + (delta_2 * (y01-y02))) / dist;
							ycf = y02 + ((delta_1 * (y02-y01)) + (delta_2 * (x02-x01))) / dist;
						} else
						{
							xcf = x02 + ((delta_1 * (x02-x01)) + (delta_2 * (y02-y01))) / dist;
							ycf = y02 + ((delta_1 * (y02-y01)) + (delta_2 * (x01-x02))) / dist;
						}
						x1 = xcf;   y1 = ycf;

						// compute angles to the arc endpoints
						if ((y01 == ycf && x01 == xcf) || (y02 == ycf && x02 == xcf))
						{
							System.out.println("Domain error in polyline computation");
							continue;
						}
						double sa = Math.atan2(y01-ycf, x01-xcf);
						double ea = Math.atan2(y02-ycf, x02-xcf);
						if (lastPp.bulge < 0.0)
						{
							r2 = sa;   sa = ea;   ea = r2;
						}
						if (sa < 0.0) sa += 2.0 * Math.PI;
						if (ea < 0.0) ea += 2.0 * Math.PI;
						sa = sa * 1800.0 / Math.PI;
						ea = ea * 1800.0 / Math.PI;

						// create the arc node
						int iangle = (int)sa;
						NodeInst ni = NodeInst.makeInstance(Artwork.tech.circleNode, new Point2D.Double(x1, y1), rad*2, rad*2, io_dxfcurcell, iangle%3600, null, 0);
						if (ni == null) return true;
						if (sa > ea) ea += 3600.0;
						double startoffset = sa;
						startoffset -= (double)iangle;
//						setarcdegrees(ni, startoffset * EPI / 1800.0, (ea-sa) * EPI / 1800.0);
//						setvalkey((INTBIG)ni, VNODEINST, io_dxflayerkey, (INTBIG)layer->layerName, VSTRING);
						continue;
					}

					// this segment has no bulge
					double cX = (x1 + x2) / 2;
					double cY = (y1 + y2) / 2;
					NodeProto np = Artwork.tech.openedDashedPolygonNode;
					if (lineType == 0) np = Artwork.tech.openedPolygonNode;
					NodeInst ni = NodeInst.makeInstance(np, new Point2D.Double(cX, cY), Math.abs(x1 - x2), Math.abs(y1 - y2), io_dxfcurcell);
					if (ni == null) return true;
					Point2D [] points = new Point2D[2];
					points[0] = new Point2D.Double(x1 - cX, y1 - cY);
					points[1] = new Point2D.Double(x2 - cX, y2 - cY);
					ni.newVar(NodeInst.TRACE, points);
//					setvalkey((INTBIG)ni, VNODEINST, io_dxflayerkey, (INTBIG)layer->layerName, VSTRING);
				}
			} else
			{
				// no bulges: do simple polygon
				double lX = 0, hX = 0;
				double lY = 0, hY = 0;
				for(int i=0; i<count; i++)
				{
					PolyPoint pp = (PolyPoint)polyPoints.get(i);
					if (i == 0)
					{
						lX = hX = pp.x;
						lY = hY = pp.y;
					} else
					{
						if (pp.x < lX) lX = pp.x;
						if (pp.x > hX) hX = pp.x;
						if (pp.y < lY) lY = pp.y;
						if (pp.y > hY) hY = pp.y;
					}
				}
				double cX = (lX + hX) / 2;
				double cY = (lY + hY) / 2;
				NodeProto np = Artwork.tech.closedPolygonNode;
				if (!closed)
				{
					if (lineType == 0) np = Artwork.tech.openedPolygonNode; else
						np = Artwork.tech.openedDashedPolygonNode;
				}
				NodeInst ni = NodeInst.makeInstance(np, new Point2D.Double(cX, cY), hX-lX, hY-lY, io_dxfcurcell);
				if (ni == null) return true;
				Point2D [] points = new Point2D[count];
				for(int i=0; i<count; i++)
				{
					PolyPoint pp = (PolyPoint)polyPoints.get(i);
					points[i] = new Point2D.Double(pp.x - cX, pp.y - cY);
				}
				ni.newVar(NodeInst.TRACE, points);
//				setvalkey((INTBIG)ni, VNODEINST, io_dxflayerkey, (INTBIG)layer->layerName, VSTRING);
			}
		}
		io_dxfreadpolylines++;
		return true;
	}
	
	private boolean io_dxfreadsolidentity()
		throws IOException
	{
		DXFLAYER layer = null;
		double factor = 1.0;
		double x1 = 0, y1 = 0, z1 = 0;
		double x2 = 0, y2 = 0, z2 = 0;
		double x3 = 0, y3 = 0, z3 = 0;
		double x4 = 0, y4 = 0, z4 = 0;
		for(;;)
		{
			if (io_dxfgetnextpair()) return true;
			switch (groupID)
			{
				case 8:  layer = io_dxfgetlayer(text);     break;
				case 10: x1 = scaleString(text);           break;
				case 20: y1 = scaleString(text);           break;
				case 30: z1 = scaleString(text);           break;
				case 11: x2 = scaleString(text);           break;
				case 21: y2 = scaleString(text);           break;
				case 31: z2 = scaleString(text);           break;
				case 12: x3 = scaleString(text);           break;
				case 22: y3 = scaleString(text);           break;
				case 32: z3 = scaleString(text);           break;
				case 13: x4 = scaleString(text);           break;
				case 23: y4 = scaleString(text);           break;
				case 33: z4 = scaleString(text);           break;
				case 230:
					factor = TextUtils.atof(text);
					break;
			}
			if (groupID == 0)
			{
				io_dxfpushpair(groupID, text);
				break;
			}
		}
		x1 = x1 * factor;
		x2 = x2 * factor;
		x3 = x3 * factor;
		x4 = x4 * factor;
		if (!io_dxfacceptablelayer(layer)) return true;
		double lx = Math.min(Math.min(x1, x2), Math.min(x3, x4));
		double hx = Math.max(Math.max(x1, x2), Math.max(x3, x4));
		double ly = Math.min(Math.min(y1, y2), Math.min(y3, y4));
		double hy = Math.max(Math.max(y1, y2), Math.max(y3, y4));
		double cX = (lx + hx) / 2;
		double cY = (ly + hy) / 2;
		NodeInst ni = NodeInst.makeInstance(Artwork.tech.filledPolygonNode, new Point2D.Double(cX, cY), hx-lx, hy-ly, io_dxfcurcell);
		if (ni == null) return true;
		Point2D [] points = new Point2D[4];
		points[0] = new Point2D.Double(x1 - cX, y1 - cY);
		points[1] = new Point2D.Double(x2 - cX, y2 - cY);
		points[2] = new Point2D.Double(x3 - cX, y3 - cY);
		points[3] = new Point2D.Double(x4 - cX, y4 - cY);
		ni.newVar(NodeInst.TRACE, points);
//		setvalkey((INTBIG)ni, VNODEINST, io_dxflayerkey, (INTBIG)layer->layerName, VSTRING);
		io_dxfreadsolids++;
		return false;
	}
	
	private boolean io_dxfreadtextentity()
		throws IOException
	{
		DXFLAYER layer = null;
		String msg = null;
		double x = 0, y = 0;
		double height = 0, xAlign = 0;
		boolean gotxa = false;
		for(;;)
		{
			if (io_dxfgetnextpair()) return true;
			switch (groupID)
			{
				case 8:  layer = io_dxfgetlayer(text);                 break;
				case 10: x = scaleString(text);                        break;
				case 20: y = scaleString(text);                        break;
				case 40: height = scaleString(text);                   break;
				case 11: xAlign = scaleString(text);   gotxa = true;   break;
				case 1:  msg = text.trim();                            break;
			}
			if (groupID == 0)
			{
				io_dxfpushpair(groupID, text);
				break;
			}
		}
		double lx = x, hx = x;
		double ly = y, hy = y;
		if (gotxa)
		{
			lx = Math.min(x, xAlign);
			hx = lx + Math.abs(xAlign-x) * 2;
			ly = y;
			hy = y + height;
		} else
		{
			if (msg != null)
			{
//				screengettextsize(el_curwindowpart, msg, &px, &py);
//				lx = x;	hx = x + height*px/py;
//				ly = y;	hy = y + height;
			}
		}
		if (!io_dxfacceptablelayer(layer)) return true;
		if (msg != null)
		{
			NodeInst ni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, new Point2D.Double((lx+hx)/2, (ly+hy)/2), hx-lx, hy-ly, io_dxfcurcell);
			if (ni == null) return true;
			Variable var = ni.newVar(Artwork.ART_MESSAGE, msg);
			if (var != null)
			{
				var.setDisplay(true);
				TextDescriptor td = var.getTextDescriptor();
				td.setPos(TextDescriptor.Position.BOXED);
				td.setAbsSize(TextDescriptor.Size.TXTMAXPOINTS);
			}
//			setvalkey((INTBIG)ni, VNODEINST, io_dxflayerkey, (INTBIG)layer->layerName, VSTRING);
			io_dxfreadtexts++;
		}
		return false;
	}
	
	private boolean io_dxfread3dfaceentity()
		throws IOException
	{
		DXFLAYER layer = null;
		double x1 = 0, y1 = 0, z1 = 0;
		double x2 = 0, y2 = 0, z2 = 0;
		double x3 = 0, y3 = 0, z3 = 0;
		double x4 = 0, y4 = 0, z4 = 0;
		for(;;)
		{
			if (io_dxfgetnextpair()) return true;
			switch (groupID)
			{
				case 8:  layer = io_dxfgetlayer(text);   break;
				case 10: x1 = scaleString(text);         break;
				case 20: y1 = scaleString(text);         break;
				case 30: z1 = scaleString(text);         break;
	
				case 11: x2 = scaleString(text);         break;
				case 21: y2 = scaleString(text);         break;
				case 31: z2 = scaleString(text);         break;
	
				case 12: x3 = scaleString(text);         break;
				case 22: y3 = scaleString(text);         break;
				case 32: z3 = scaleString(text);         break;
	
				case 13: x4 = scaleString(text);         break;
				case 23: y4 = scaleString(text);         break;
				case 33: z4 = scaleString(text);         break;
			}
			if (groupID == 0)
			{
				io_dxfpushpair(groupID, text);
				break;
			}
		}
		if (!io_dxfacceptablelayer(layer)) return false;
		double lx = Math.min(Math.min(x1, x2), Math.min(x3, x4));
		double hx = Math.max(Math.max(x1, x2), Math.max(x3, x4));
		double ly = Math.min(Math.min(y1, y2), Math.min(y3, y4));
		double hy = Math.max(Math.max(y1, y2), Math.max(y3, y4));
		double cX = (lx + hx) / 2;
		double cY = (ly + hy) / 2;
		NodeInst ni = NodeInst.makeInstance(Artwork.tech.closedPolygonNode, new Point2D.Double(cX, cY), hx-lx, hy-ly, io_dxfcurcell);
		if (ni == null) return true;
		Point2D [] points = new Point2D[4];
		points[0] = new Point2D.Double(x1 - cX, y1 - cY);
		points[1] = new Point2D.Double(x2 - cX, y2 - cY);
		points[2] = new Point2D.Double(x3 - cX, y3 - cY);
		points[3] = new Point2D.Double(x4 - cX, y4 - cY);
		ni.newVar(NodeInst.TRACE, points);
//		setvalkey((INTBIG)ni, VNODEINST, io_dxflayerkey, (INTBIG)layer->layerName, VSTRING);
		io_dxfread3dfaces++;
		return false;
	}
	
	private void io_dxfignoreentity()
		throws IOException
	{
		for(;;)
		{
			if (io_dxfgetnextpair()) break;
			if (groupID == 0) break;
		}
		io_dxfpushpair(groupID, text);
	}
	
	/****************************************** READING SUPPORT ******************************************/
	
	private boolean io_dxfacceptablelayer(DXFLAYER layer)
	{
		if (IOTool.isDXFInputReadsAllLayers()) return true;
		if (layer == null) return false;
		if (validLayerNames.contains(layer.layerName)) return true;
	
		// add this to the list of layer names that were ignored
		ignoredLayerNames.add(layer.layerName);
		return false;
	}
	
	private boolean io_dxfextractinsert(Cell onp, double x, double y, double xsca, double ysca, int rot, Cell np)
	{
//		INTBIG *newtrace, tx, ty;
//		REGISTER INTBIG sx, sy, cx, cy;
//		INTBIG i, len;
//		NODEINST *ni, *nni;
//		double startoffset, endangle;
//		REGISTER VARIABLE *var;
//		XARRAY trans;
	
//		// rotate "rot*10" about point [(onp->lowx+onp->highx)/2+x, (onp->lowy+onp->highy)/2+y]
//		makeangle(rot*10, 0, trans);
//		trans[2][0] = (onp->lowx+onp->highx)/2+x;
//		trans[2][1] = (onp->lowy+onp->highy)/2+y;
//		xform(-trans[2][0], -trans[2][1], &trans[2][0], &trans[2][1], trans);
//	
//		for(ni = onp->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//		{
//			if (ni->proto->primindex == 0)
//			{
//				ttyputmsg(_("Cannot insert block '%s'...it has inserts in it"),
//					onp->protoname);
//				return(1);
//			}
//			if (ni->proto == gen_cellcenterprim) continue;
//			sx = roundfloat((float)(ni->highx-ni->lowx) * xsca);
//			sy = roundfloat((float)(ni->highy-ni->lowy) * ysca);
//			cx = x + roundfloat((float)(ni->highx+ni->lowx) * xsca / 2.0f);
//			cy = y + roundfloat((float)(ni->highy+ni->lowy) * ysca / 2.0f);
//			xform(cx, cy, &tx, &ty, trans);
//			tx -= sx/2;   ty -= sy/2;
//			nni = newnodeinst(ni->proto, tx, tx+sx, ty, ty+sy, ni->transpose, (ni->rotation+rot*10)%3600, np);
//			if (nni == NONODEINST) return(1);
//			if (ni->proto == art_closedpolygonprim || ni->proto == art_filledpolygonprim ||
//				ni->proto == art_openedpolygonprim || ni->proto == art_openeddashedpolygonprim)
//			{
//				// copy trace information
//				var = gettrace(ni);
//				if (var != NOVARIABLE)
//				{
//					len = (var->type&VLENGTH) >> VLENGTHSH;
//					newtrace = (INTBIG *)emalloc(len * SIZEOFINTBIG, el_tempcluster);
//					if (newtrace == 0) return(1);
//					for(i=0; i<len; i++)
//					{
//						newtrace[i] = (INTBIG)(((INTBIG *)var->addr)[i] * ((i&1) == 0 ? xsca : ysca));
//					}
//					(void)setvalkey((INTBIG)nni, VNODEINST, el_trace_key, (INTBIG)newtrace, var->type);
//					efree((CHAR *)newtrace);
//				}
//			} else if (ni->proto == gen_invispinprim)
//			{
//				// copy text information
//				var = getvalkey((INTBIG)ni, VNODEINST, VSTRING, art_messagekey);
//				if (var != NOVARIABLE)
//					var = setvalkey((INTBIG)nni, VNODEINST, art_messagekey, var->addr, var->type);
//			} else if (ni->proto == art_circleprim || ni->proto == art_thickcircleprim)
//			{
//				// copy arc information
//				getarcdegrees(ni, &startoffset, &endangle);
//				setarcdegrees(nni, startoffset, endangle);
//			}
//	
//			// copy other information
//			var = getvalkey((INTBIG)ni, VNODEINST, VSTRING, io_dxflayerkey);
//			if (var != NOVARIABLE)
//				setvalkey((INTBIG)nni, VNODEINST, io_dxflayerkey, var->addr, var->type);
//			var = getvalkey((INTBIG)ni, VNODEINST, VINTEGER, art_colorkey);
//			if (var != NOVARIABLE)
//				setvalkey((INTBIG)nni, VNODEINST, art_colorkey, var->addr, var->type);
//		}
		return false;
	}
	
	private Cell io_dxfgetscaledcell(Cell onp, double xsca, double ysca)
	{
//		CHAR sviewname[100], fviewname[100], cellname[200];
//		INTBIG *newtrace;
//		INTBIG i, len;
//		VIEW *view;
//		NODEINST *ni, *nni;
//		NODEPROTO *np;
//		double startoffset, endangle;
//		REGISTER VARIABLE *var;
//	
//		esnprintf(fviewname, 100, x_("scaled%gx%g"), xsca, ysca);
//		esnprintf(sviewname, 100, x_("s%gx%g"), xsca, ysca);
//		view = getview(fviewname);
//		if (view == NOVIEW)
//		{
//			view = newview(fviewname, sviewname);
//			if (view == NOVIEW) return(NONODEPROTO);
//		}
//	
//		// find the view of this cell
//		FOR_CELLGROUP(np, onp)
//			if (np->cellview == view) return(np);
//	
//		// not found: create it
//		esnprintf(cellname, 200, x_("%s{%s}"), onp->protoname, sviewname);
//		np = us_newnodeproto(cellname, onp->lib);
//		if (np == NONODEPROTO) return(NONODEPROTO);
//	
//		for(ni = onp->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//		{
//			if (ni->proto->primindex == 0)
//			{
//				ttyputmsg(_("Cannot scale insert of block '%s'...it has inserts in it"),
//					onp->protoname);
//				return(NONODEPROTO);
//			}
//			nni = newnodeinst(ni->proto, (INTBIG)(ni->lowx*xsca), (INTBIG)(ni->highx*xsca),
//				(INTBIG)(ni->lowy*ysca), (INTBIG)(ni->highy*ysca), ni->transpose, ni->rotation, np);
//			if (nni == NONODEINST) return(NONODEPROTO);
//			if (ni->proto == art_closedpolygonprim || ni->proto == art_filledpolygonprim ||
//				ni->proto == art_openedpolygonprim || ni->proto == art_openeddashedpolygonprim)
//			{
//				// copy trace information
//				var = gettrace(ni);
//				if (var != NOVARIABLE)
//				{
//					len = (var->type&VLENGTH) >> VLENGTHSH;
//					newtrace = (INTBIG *)emalloc(len * SIZEOFINTBIG, el_tempcluster);
//					if (newtrace == 0) return(NONODEPROTO);
//					for(i=0; i<len; i++)
//						newtrace[i] = (INTBIG)(((INTBIG *)var->addr)[i] * ((i&1) == 0 ? xsca : ysca));
//					(void)setvalkey((INTBIG)nni, VNODEINST, el_trace_key, (INTBIG)newtrace, var->type);
//					efree((CHAR *)newtrace);
//				}
//			} else if (ni->proto == gen_invispinprim)
//			{
//				// copy text information
//				var = getvalkey((INTBIG)ni, VNODEINST, VSTRING, art_messagekey);
//				if (var != NOVARIABLE)
//					var = setvalkey((INTBIG)nni, VNODEINST, art_messagekey, var->addr, var->type);
//			} else if (ni->proto == art_circleprim || ni->proto == art_thickcircleprim)
//			{
//				// copy arc information
//				getarcdegrees(ni, &startoffset, &endangle);
//				setarcdegrees(nni, startoffset, endangle);
//			}
//	
//			// copy layer information
//			var = getvalkey((INTBIG)ni, VNODEINST, VSTRING, io_dxflayerkey);
//			if (var != NOVARIABLE)
//				setvalkey((INTBIG)nni, VNODEINST, io_dxflayerkey, var->addr, var->type);
//		}
//		return(np);
		return null;
	}
	
	private DXFLAYER io_dxfgetlayer(String name)
	{
		for(DXFLAYER layer = io_dxffirstlayer; layer != null; layer = layer.next)
			if (name.equals(layer.layerName)) return layer;
	
		// create a new one
		DXFLAYER layer = new DXFLAYER();
		layer.layerName = name;
		layer.layerColor = -1;
		layer.layerRed = 1.0;
		layer.layerGreen = 1.0;
		layer.layerBlue = 1.0;
		layer.next = io_dxffirstlayer;
		io_dxffirstlayer = layer;
		return layer;
	}
	
	private void io_dxfclearlayers()
	{
		io_dxffirstlayer = null;
	}
	
	private void io_dxfpushpair(int groupID, String text)
	{
		io_dxfgroupid = groupID;
		io_dxfline = text;
		io_dxfpairvalid = true;
	}
	
	/**
	 * Method to examine the variable "IO_dxf_layer_names" on the artwork technology and obtain
	 * a list of acceptable layer names and numbers.
	 */
	private void io_dxfgetacceptablelayers()
	{
		validLayerNames = new HashSet();
		for(Iterator it = Artwork.tech.getLayers(); it.hasNext(); )
		{
			Layer lay = (Layer)it.next();
			String layNames = lay.getDXFLayer();
			if (layNames == null) continue;
			while (layNames.length() > 0)
			{
				int commaPos = layNames.indexOf(',');
				if (commaPos < 0) commaPos = layNames.length();
				String oneName = layNames.substring(0, commaPos);
				validLayerNames.add(oneName);
				layNames = layNames.substring(oneName.length());
			}
		}
	}
	
	/**
	 * Method to convert a block name "name" into a valid Electric cell name (converts
	 * bad characters).
	 */
	private String io_dxfblockname(String name)
	{
		StringBuffer infstr = new StringBuffer();
		for(int i=0; i<name.length(); i++)
		{
			char chr = name.charAt(i);
			if (chr == '$' || chr == '{' || chr == '}' || chr == ':') chr = '_';
			infstr.append(chr);
		}
		return infstr.toString();
	}
	
	/**
	 * Method to set the conversion units between DXF files and real distance.
	 * The value is stored in the global "io_dxfdispunit".
	 */
	void io_dxfsetcurunits()
	{
		int units = IOTool.getDXFScale();
		switch (units)
		{
			case -3: io_dxfdispunit = TextUtils.UnitScale.GIGA;   break;
			case -2: io_dxfdispunit = TextUtils.UnitScale.MEGA;   break;
			case -1: io_dxfdispunit = TextUtils.UnitScale.KILO;   break;
			case  0: io_dxfdispunit = TextUtils.UnitScale.NONE;   break;
			case  1: io_dxfdispunit = TextUtils.UnitScale.MILLI;  break;
			case  2: io_dxfdispunit = TextUtils.UnitScale.MICRO;  break;
			case  3: io_dxfdispunit = TextUtils.UnitScale.NANO;   break;
			case  4: io_dxfdispunit = TextUtils.UnitScale.PICO;   break;
			case  5: io_dxfdispunit = TextUtils.UnitScale.FEMTO;  break;
		}
	}
}
