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
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.io.IOTool;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.HashMap;
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
	
//	static INTBIG            io_dxfreadalllayers;
//	static INTBIG            io_dxfflatteninput;
//	static INTBIG            io_dxfcurline, io_dxflength;
//	static INTBIG            io_dxfentityhandle;
	private int            io_dxfgroupid;
	private int io_dxfignoredpoints, io_dxfignoredattribdefs, io_dxfignoredattributes;
	private int     io_dxfreadpolylines, io_dxfreadlines, io_dxfreadcircles, io_dxfreadsolids,
						 io_dxfread3dfaces, io_dxfreadarcs, io_dxfreadinserts, io_dxfreadtexts;
	private String              io_dxfline;
	private DXFLAYER         io_dxffirstlayer;
	private FORWARDREF       io_dxffirstforwardref;
	private Cell        io_dxfmaincell;
	private Cell        io_dxfcurcell;
	private boolean io_dxfpairvalid;
	private List           io_dxfheadertext;
	private List           io_dxfheaderid;
	private int            io_dxfinputmode;			/* 0: pairs not read, 1: normal pairs, 2: blank lines */
//	static INTBIG            io_dxfvalidlayercount = 0;
//	static CHAR            **io_dxfvalidlayernames;
//	static INTBIG           *io_dxfvalidlayernumbers;
//	static void             *io_dxfignoredlayerstringarray = 0;
//	static INTBIG            io_dxflayerkey = 0;
	private TextUtils.UnitScale            io_dxfdispunit;

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
//		FILE *io;
//		CHAR *filename, *text, warning[256], countStr[50], **list;
//		INTBIG groupID;
//		BOOLEAN err;
//		INTBIG len, i, *curstate;
//		NODEPROTO *np;
//		FORWARDREF *fr, *nextfr;
//		NODEINST *ni;
//		REGISTER void *infstr;
	
		// parameters for reading files
//		if (io_dxflayerkey == 0) io_dxflayerkey = makekey(x_("IO_dxf_layer"));
//		curstate = io_getstatebits();
//		io_dxfreadalllayers = curstate[0] & DXFALLLAYERS;
//		io_dxfflatteninput = curstate[0] & DXFFLATTENINPUT;
	
		// set the scale
		io_dxfsetcurunits();
	
		// examine technology for acceptable DXF layer names
//		if (io_dxfgetacceptablelayers() != 0) return true;
	
		// make the only cell in this library
		io_dxfmaincell = Cell.makeInstance(lib, lib.getName());
		if (io_dxfmaincell == null) return true;
		lib.setCurCell(io_dxfmaincell);
		io_dxfcurcell = io_dxfmaincell;
		io_dxfheaderid = new ArrayList();
		io_dxfheadertext = new ArrayList();
//		if (io_dxfignoredlayerstringarray == 0)
//		{
//			io_dxfignoredlayerstringarray = newstringarray(io_tool->cluster);
//			if (io_dxfignoredlayerstringarray == 0) return true;
//		}
//		clearstrings(io_dxfignoredlayerstringarray);
	
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
//					if (err = io_dxfreadentities(lib)) break;
					continue;
				}
				if (text.equals("ENTITIES"))
				{
//					if (err = io_dxfreadentities(lib)) break;
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
//		for(fr = io_dxffirstforwardref; fr != 0; fr = nextfr)
//		{
//			nextfr = fr->nextforwardref;
//	
//			// have to search by hand because of weird prototype names
//			for(np = lib->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//				if (estrcmp(np->protoname, fr->refname) == 0) break;
//			if (np == NONODEPROTO)
//			{
//				ttyputmsg(_("Cannot find block '%s'"), fr->refname);
//			} else
//			{
//				if (io_dxfflatteninput != 0)
//				{
//					if (io_dxfextractinsert(np, fr->x, fr->y, fr->xsca, fr->ysca, fr->rot, fr->parent) != 0) return true;
//				} else
//				{
//					if (fr->xsca != 1.0 || fr->ysca != 1.0)
//					{
//						np = io_dxfgetscaledcell(np, fr->xsca, fr->ysca);
//						if (np == NONODEPROTO) return true;
//					}
//					ni = newnodeinst(np, fr->x+np->lowx, fr->x+np->highx, fr->y+np->lowy, fr->y+np->highy,
//						0, fr->rot*10, fr->parent);
//					if (ni == NONODEINST) return true;
//					ni->userbits |= NEXPAND;
//				}
//			}
//			efree(fr->refname);
//			efree((CHAR *)fr);
//		}
	
		// save header with library
		if (io_dxfheaderid.size() > 0)
		{
//			setval((INTBIG)lib, VLIBRARY, x_("IO_dxf_header_text"), (INTBIG)io_dxfheadertext,
//				VSTRING|VISARRAY|(io_dxfheadercount<<VLENGTHSH));
//			setval((INTBIG)lib, VLIBRARY, x_("IO_dxf_header_ID"), (INTBIG)io_dxfheaderid,
//				VSHORT|VISARRAY|(io_dxfheadercount<<VLENGTHSH));
		}
	
		// recompute bounds
//		(*el_curconstraint->solve)(NONODEPROTO);
	
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
//		list = getstringarray(io_dxfignoredlayerstringarray, &len);
//		if (len > 0)
//		{
//			infstr = initinfstr();
//			addstringtoinfstr(infstr, _("Ignored layers "));
//			for(i=0; i<len; i++)
//			{
//				if (i > 0) addstringtoinfstr(infstr, x_(", "));
//				formatinfstr(infstr, x_("'%s'"), list[i]);
//			}
//			ttyputmsg(x_("%s"), returninfstr(infstr));
//		}
	
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
//		CHAR *text, *msg, *pt;
//		float xsca, ysca;
//		INTBIG groupID, count, i, closed, xrep, yrep, rot, lineType, start, last;
//		INTBIG x1,y1,z1, x2,y2,z2, x3,y3,z3, x4,y4,z4, rad, *xp,*yp,*zp, coord[8], *coords, lx, hx, ly, hy,
//			xc, yc, xspa, yspa, iangle;
//		UINTBIG descript[TEXTDESCRIPTSIZE];
//		double sangle, eangle, startoffset, bulgesign;
//		double *bulge;
//		DXFLAYER *layer;
//		NODEINST *ni;
//		NODEPROTO *np;
//		VARIABLE *var;
//		FORWARDREF *fr;
	
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
//				if (io_dxfreadinsertentity(io, &x1,&y1,&z1, &msg, &rot, &xsca, &ysca, &xrep, &yrep,
//					&xspa, &yspa, &layer) != 0) return true;
//				pt = io_dxfblockname(msg);
//				efree(msg);
//				if (pt != 0)
//				{
//					if (xrep != 1 || yrep != 1)
//					{
//						ttyputmsg(_("Cannot insert block '%s' repeated %ldx%ld times"), pt, xrep, yrep);
//						continue;
//					}
//	
//					// have to search by hand because of weird prototype names
//					for(np = lib->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//						if (estrcmp(np->protoname, pt) == 0) break;
//					if (np == NONODEPROTO)
//					{
//						fr = (FORWARDREF *)emalloc(sizeof (FORWARDREF), io_tool->cluster);
//						if (fr == 0) return true;
//						(void)allocstring(&fr->refname, pt, io_tool->cluster);
//						fr->parent = io_dxfcurcell;
//						fr->x = x1;		fr->y = y1;
//						fr->rot = rot;
//						fr->xrep = xrep;	fr->yrep = yrep;
//						fr->xspa = xspa;	fr->yspa = yspa;
//						fr->xsca = xsca;	fr->ysca = ysca;
//						fr->nextforwardref = io_dxffirstforwardref;
//						io_dxffirstforwardref = fr;
//						continue;
//					}
//	
//					if (io_dxfflatteninput != 0)
//					{
//						if (io_dxfextractinsert(np, x1, y1, xsca, ysca, rot, io_dxfcurcell) != 0) return true;
//					} else
//					{
//						if (xsca != 1.0 || ysca != 1.0)
//						{
//							np = io_dxfgetscaledcell(np, xsca, ysca);
//							if (np == NONODEPROTO) return true;
//						}
//						ni = newnodeinst(np, x1+np->lowx, x1+np->highx, y1+np->lowy, y1+np->highy, 0, rot*10, io_dxfcurcell);
//						if (ni == NONODEINST) return true;
//						ni->userbits |= NEXPAND;
//					}
//				}
				io_dxfreadinserts++;
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
//				if (io_dxfreadpolylineentity(io, &xp, &yp, &zp, &bulge, &count, &closed, &lineType, &layer) != 0) return true;
//				if (io_dxfacceptablelayer(layer) != 0 && count >= 3)
//				{
//					// see if there is bulge information
//					for(i=0; i<count; i++) if (bulge[i] != 0.0) break;
//					if (i < count)
//					{
//						// handle bulges
//						if (closed != 0) start = 0; else start = 1;
//						for(i=start; i<count; i++)
//						{
//							if (i == 0) last = count-1; else last = i-1;
//							x1 = xp[last];   y1 = yp[last];
//							x2 = xp[i];      y2 = yp[i];
//							if (bulge[last] != 0.0)
//							{
//								// this segment has a bulge
//								double dist, dx, dy, arcrad, x01, y01, x02, y02, r2, delta_1, delta_12, delta_2,
//									xcf, ycf, sa, ea, incangle;
//	
//								// special case the semicircle bulges
//								if (fabs(bulge[last]) == 1.0)
//								{
//									xc = (x1 + x2) / 2;
//									yc = (y1 + y2) / 2;
//									if ((y1 == yc && x1 == xc) || (y2 == yc && x2 == xc))
//									{
//										ttyputerr(_("Domain error in polyline bulge computation"));
//										continue;
//									}
//									sa = atan2((double)(y1-yc), (double)(x1-xc));
//									ea = atan2((double)(y2-yc), (double)(x2-xc));
//									if (bulge[last] < 0.0)
//									{
//										r2 = sa;   sa = ea;   ea = r2;
//									}
//									if (sa < 0.0) sa += 2.0 * EPI;
//									sa = sa * 1800.0 / EPI;
//									iangle = rounddouble(sa);
//									rad = computedistance(xc, yc, x1, y1);
//									ni = newnodeinst(art_circleprim, xc-rad, xc+rad, yc-rad, yc+rad, 0,
//										iangle, io_dxfcurcell);
//									if (ni == NONODEINST) return true;
//									startoffset = sa;
//									startoffset -= (double)iangle;
//									setarcdegrees(ni, startoffset * EPI / 1800.0, EPI);
//									setvalkey((INTBIG)ni, VNODEINST, io_dxflayerkey, (INTBIG)layer->layerName, VSTRING);
//									continue;
//								}
//	
//								// compute half distance between the points
//								x01 = x1;   y01 = y1;
//								x02 = x2;   y02 = y2;
//								dx = x02 - x01;   dy = y02 - y01;
//								dist = sqrt(dx*dx + dy*dy);
//	
//								// compute radius of arc (bulge is tangent of 1/4 of included arc angle)
//								incangle = atan(bulge[last]) * 4.0;
//								arcrad = fabs((dist / 2.0) / sin(incangle / 2.0));
//								rad = rounddouble(arcrad);
//	
//								// prepare to compute the two circle centers
//								r2 = arcrad*arcrad;
//								delta_1 = -dist / 2.0;
//								delta_12 = delta_1 * delta_1;
//								delta_2 = sqrt(r2 - delta_12);
//	
//								// pick one center, according to bulge sign
//								bulgesign = bulge[last];
//								if (fabs(bulgesign) > 1.0) bulgesign = -bulgesign;
//								if (bulgesign > 0.0)
//								{
//									xcf = x02 + ((delta_1 * (x02-x01)) + (delta_2 * (y01-y02))) / dist;
//									ycf = y02 + ((delta_1 * (y02-y01)) + (delta_2 * (x02-x01))) / dist;
//								} else
//								{
//									xcf = x02 + ((delta_1 * (x02-x01)) + (delta_2 * (y02-y01))) / dist;
//									ycf = y02 + ((delta_1 * (y02-y01)) + (delta_2 * (x01-x02))) / dist;
//								}
//								x1 = rounddouble(xcf);   y1 = rounddouble(ycf);
//	
//								// compute angles to the arc endpoints
//								if ((y01 == ycf && x01 == xcf) || (y02 == ycf && x02 == xcf))
//								{
//									ttyputerr(_("Domain error in polyline computation"));
//									continue;
//								}
//								sa = atan2(y01-ycf, x01-xcf);
//								ea = atan2(y02-ycf, x02-xcf);
//								if (bulge[last] < 0.0)
//								{
//									r2 = sa;   sa = ea;   ea = r2;
//								}
//								if (sa < 0.0) sa += 2.0 * EPI;
//								if (ea < 0.0) ea += 2.0 * EPI;
//								sa = sa * 1800.0 / EPI;
//								ea = ea * 1800.0 / EPI;
//	
//								// create the arc node
//								iangle = rounddouble(sa);
//								ni = newnodeinst(art_circleprim, x1-rad, x1+rad, y1-rad, y1+rad, 0,
//									iangle%3600, io_dxfcurcell);
//								if (ni == NONODEINST) return true;
//								if (sa > ea) ea += 3600.0;
//								startoffset = sa;
//								startoffset -= (double)iangle;
//								setarcdegrees(ni, startoffset * EPI / 1800.0, (ea-sa) * EPI / 1800.0);
//								setvalkey((INTBIG)ni, VNODEINST, io_dxflayerkey, (INTBIG)layer->layerName, VSTRING);
//								continue;
//							}
//	
//							// this segment has no bulge
//							lx = mini(x1, x2);		hx = maxi(x1, x2);
//							ly = mini(y1, y2);		hy = maxi(y1, y2);
//							xc  = (lx + hx) / 2;	yc = (ly + hy) / 2;
//							if (lineType == 0) np = art_openedpolygonprim; else
//								np = art_openeddashedpolygonprim;
//							ni = newnodeinst(np, lx, hx, ly, hy, 0, 0, io_dxfcurcell);
//							if (ni == NONODEINST) return true;
//							coord[0] = x1 - xc;		coord[1] = y1 - yc;
//							coord[2] = x2 - xc;		coord[3] = y2 - yc;
//							(void)setvalkey((INTBIG)ni, VNODEINST, el_trace_key, (INTBIG)coord,
//								VINTEGER|VISARRAY|(4<<VLENGTHSH));
//							setvalkey((INTBIG)ni, VNODEINST, io_dxflayerkey, (INTBIG)layer->layerName,
//								VSTRING);
//						}
//					} else
//					{
//						// no bulges: do simple polygon
//						lx = hx = xp[0];
//						ly = hy = yp[0];
//						for(i=1; i<count; i++)
//						{
//							if (xp[i] < lx) lx = xp[i];
//							if (xp[i] > hx) hx = xp[i];
//							if (yp[i] < ly) ly = yp[i];
//							if (yp[i] > hy) hy = yp[i];
//						}
//						xc  = (lx + hx) / 2;	yc = (ly + hy) / 2;
//						if (closed != 0)
//						{
//							np = art_closedpolygonprim;
//						} else
//						{
//							if (lineType == 0) np = art_openedpolygonprim; else
//								np = art_openeddashedpolygonprim;
//						}
//						ni = newnodeinst(np, lx, hx, ly, hy, 0, 0, io_dxfcurcell);
//						if (ni == NONODEINST) return true;
//						coords = (INTBIG *)emalloc(count*2 * SIZEOFINTBIG, el_tempcluster);
//						if (coords == 0) return true;
//						for(i=0; i<count; i++)
//						{
//							coords[i*2] = xp[i] - xc;
//							coords[i*2+1] = yp[i] - yc;
//						}
//						(void)setvalkey((INTBIG)ni, VNODEINST, el_trace_key, (INTBIG)coords,
//							VINTEGER|VISARRAY|((count*2)<<VLENGTHSH));
//						efree((CHAR *)coords);
//						setvalkey((INTBIG)ni, VNODEINST, io_dxflayerkey, (INTBIG)layer->layerName, VSTRING);
//					}
//				}
//				efree((CHAR *)xp);    efree((CHAR *)yp);    efree((CHAR *)zp);    efree((CHAR *)bulge);
				io_dxfreadpolylines++;
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
	
//	INTBIG io_dxfreadinsertentity(FILE *io, INTBIG *x, INTBIG *y, INTBIG *z, CHAR **name, INTBIG *rot,
//		float *xsca, float *ysca, INTBIG *xrep, INTBIG *yrep, INTBIG *xspa, INTBIG *yspa, DXFLAYER **retLayer)
//	{
//		CHAR *text, *saveMsg;
//		INTBIG groupID;
//	
//		*retLayer = 0;
//		*rot = 0;
//		saveMsg = 0;
//		*xrep = *yrep = 1;
//		*xspa = *yspa = 0;
//		*xsca = *ysca = 1.0;
//		for(;;)
//		{
//			if (stopping(STOPREASONDXF)) return(1);
//			if (io_dxfgetnextpair(io, &groupID, &text) != 0) return(1);
//			switch (groupID)
//			{
//				case 8:  *retLayer = io_dxfgetlayer(text);                         break;
//				case 10: *x = scalefromdispunit((float)eatof(text), io_dxfdispunit);    break;
//				case 20: *y = scalefromdispunit((float)eatof(text), io_dxfdispunit);    break;
//				case 30: *z = scalefromdispunit((float)eatof(text), io_dxfdispunit);    break;
//				case 50: *rot = eatoi(text);                                        break;
//				case 41: *xsca = (float)eatof(text);                                break;
//				case 42: *ysca = (float)eatof(text);                                break;
//				case 70: *xrep = eatoi(text);                                       break;
//				case 71: *yrep = eatoi(text);                                       break;
//				case 44: *xspa = scalefromdispunit((float)eatof(text), io_dxfdispunit); break;
//				case 45: *yspa = scalefromdispunit((float)eatof(text), io_dxfdispunit); break;
//				case 2:
//					if (saveMsg != 0) (void)reallocstring(&saveMsg, text, el_tempcluster); else
//						(void)allocstring(&saveMsg, text, el_tempcluster);
//					break;
//			}
//			if (groupID == 0)
//			{
//				io_dxfpushpair(groupID, text);
//				break;
//			}
//		}
//		*name = saveMsg;
//		return(0);
//	}
	
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
//	
//	INTBIG io_dxfreadpolylineentity(FILE *io, INTBIG **xp, INTBIG **yp, INTBIG **zp, double **bulgep,
//		INTBIG *count, INTBIG *closed, INTBIG *lineType, DXFLAYER **retLayer)
//	{
//		CHAR *text;
//		INTBIG groupID, vertCount, inEnd, vertLimit, newVertLimit, i;
//		INTBIG *x, *y, *z, *newx, *newy, *newz;
//		double *bulge, *newbulge;
//	
//		*closed = 0;
//		*retLayer = 0;
//		*lineType = 0;
//		inEnd = 0;
//		vertCount = vertLimit = 0;
//		for(;;)
//		{
//			if (stopping(STOPREASONDXF)) return(1);
//			if (io_dxfgetnextpair(io, &groupID, &text) != 0) break;
//			switch(groupID)
//			{
//				case 8:
//					*retLayer = io_dxfgetlayer(text);
//					break;
//	
//				case 0:
//					if (inEnd != 0)
//					{
//						io_dxfpushpair(groupID, text);
//	
//						// LINTED "x" used in proper order
//						*xp = x;
//	
//						// LINTED "y" used in proper order
//						*yp = y;
//	
//						// LINTED "z" used in proper order
//						*zp = z;
//	
//						// LINTED "bulge" used in proper order
//						*bulgep = bulge;
//						*count = vertCount;
//						return(0);
//					}
//					if (estrcmp(text, x_("SEQEND")) == 0)
//					{
//						inEnd = 1;
//						continue;
//					}
//					if (estrcmp(text, x_("VERTEX")) == 0)
//					{
//						if (vertCount >= vertLimit)
//						{
//							newVertLimit = vertCount + 10;
//							newx = (INTBIG *)emalloc(newVertLimit * SIZEOFINTBIG, el_tempcluster);
//							if (newx == 0) return(1);
//							newy = (INTBIG *)emalloc(newVertLimit * SIZEOFINTBIG, el_tempcluster);
//							if (newy == 0) return(1);
//							newz = (INTBIG *)emalloc(newVertLimit * SIZEOFINTBIG, el_tempcluster);
//							if (newz == 0) return(1);
//							newbulge = (double *)emalloc(newVertLimit * (sizeof (double)), el_tempcluster);
//							if (newbulge == 0) return(1);
//							for(i=0; i<vertCount; i++)
//							{
//								newx[i] = x[i];
//								newy[i] = y[i];
//								newz[i] = z[i];
//								newbulge[i] = bulge[i];
//							}
//							if (vertLimit > 0)
//							{
//								efree((CHAR *)x);
//								efree((CHAR *)y);
//								efree((CHAR *)z);
//								efree((CHAR *)bulge);
//							}
//							x = newx;    y = newy;    z = newz;    bulge = newbulge;
//							vertLimit = newVertLimit;
//						}
//						bulge[vertCount] = 0.0;
//						vertCount++;
//					}
//					break;
//	
//				case 10:
//					if (vertCount > 0) x[vertCount-1] = scalefromdispunit((float)eatof(text), io_dxfdispunit);
//					break;
//				case 20:
//					if (vertCount > 0) y[vertCount-1] = scalefromdispunit((float)eatof(text), io_dxfdispunit);
//					break;
//				case 30:
//					if (vertCount > 0) z[vertCount-1] = scalefromdispunit((float)eatof(text), io_dxfdispunit);
//					break;
//				case 42:
//					if (vertCount > 0) bulge[vertCount-1] = eatof(text);
//					break;
//				case 70:
//					i = eatoi(text);
//					if ((i&1) != 0) *closed = 1;
//					break;
//			}
//		}
//		return(1);
//	}
	
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
//		REGISTER INTBIG i;
//		INTBIG len;
//		REGISTER CHAR **list;
	
		if (IOTool.isDXFInputReadsAllLayers()) return true;
		if (layer == null) return false;
//		for(int i=0; i<io_dxfvalidlayercount; i++)
//			if (estrcmp(layer->layerName, io_dxfvalidlayernames[i]) == 0) return(1);
	
		// add this to the list of layer names that were ignored
//		list = getstringarray(io_dxfignoredlayerstringarray, &len);
//		for(i=0; i<len; i++)
//			if (estrcmp(layer->layerName, list[i]) == 0) break;
//		if (i >= len)
//			addtostringarray(io_dxfignoredlayerstringarray, layer->layerName);
		return false;
	}
	
//	INTBIG io_dxfextractinsert(NODEPROTO *onp, INTBIG x, INTBIG y, float xsca, float ysca, INTBIG rot, NODEPROTO *np)
//	{
//		INTBIG *newtrace, tx, ty;
//		REGISTER INTBIG sx, sy, cx, cy;
//		INTBIG i, len;
//		NODEINST *ni, *nni;
//		double startoffset, endangle;
//		REGISTER VARIABLE *var;
//		XARRAY trans;
//	
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
//		return(0);
//	}
//	
//	NODEPROTO *io_dxfgetscaledcell(NODEPROTO *onp, float xsca, float ysca)
//	{
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
//	}
	
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
	
//	/*
//	 * routine to examine the variable "IO_dxf_layer_names" on the artwork technology and obtain
//	 * a list of acceptable layer names and numbers (in "io_dxfvalidlayernames" and "io_dxfvalidlayernumbers"
//	 * that is "io_dxfvalidlayercount" long.  Returns nonzero on error.
//	 */
//	INTBIG io_dxfgetacceptablelayers(void)
//	{
//		REGISTER VARIABLE *var;
//		INTBIG i, j, k, save;
//		CHAR **list, *pt, *start;
//	
//		// get the acceptable DXF layer names
//		var = getval((INTBIG)art_tech, VTECHNOLOGY, VSTRING|VISARRAY, x_("IO_dxf_layer_names"));
//		if (var == NOVARIABLE)
//		{
//			ttyputerr(_("There are no DXF layer names in the %s technology"), art_tech->techname);
//			return(1);
//		}
//		list = (CHAR **)var->addr;
//	
//		// determine number of DXF layers
//		j = getlength(var);
//		io_dxfclearacceptablelayers();
//		for(i=0; i<j; i++) if (list[i] != 0)
//		{
//			for(pt = list[i]; *pt != 0; pt++)
//				if (*pt == ',') io_dxfvalidlayercount++;
//			io_dxfvalidlayercount++;
//		}
//	
//		// make space for these values
//		io_dxfvalidlayernames = (CHAR **)emalloc((io_dxfvalidlayercount * sizeof(CHAR *)),
//			io_tool->cluster);
//		if (io_dxfvalidlayernames == 0) return(1);
//		io_dxfvalidlayernumbers = (INTBIG *)emalloc((io_dxfvalidlayercount * SIZEOFINTBIG),
//			io_tool->cluster);
//		if (io_dxfvalidlayernumbers == 0) return(1);
//	
//		// set DXF layer names
//		k = 0;
//		for(i=0; i<j; i++) if (list[i] != 0)
//		{
//			pt = list[i];
//			for(;;)
//			{
//				start = pt;
//				while(*pt != 0 && *pt != ',') pt++;
//				save = *pt;
//				*pt = 0;
//				if (allocstring(&io_dxfvalidlayernames[k], start, io_tool->cluster))
//					return(1);
//				io_dxfvalidlayernumbers[k] = i;
//				k++;
//				if (save == 0) break;
//				*pt++ = (CHAR)save;
//			}
//		}
//		return(0);
//	}
//	
//	void io_dxfclearacceptablelayers(void)
//	{
//		REGISTER INTBIG i;
//	
//		for(i=0; i<io_dxfvalidlayercount; i++)
//			efree((CHAR *)io_dxfvalidlayernames[i]);
//		if (io_dxfvalidlayercount > 0)
//		{
//			efree((CHAR *)io_dxfvalidlayernames);
//			efree((CHAR *)io_dxfvalidlayernumbers);
//		}
//		io_dxfvalidlayercount = 0;
//	}
	
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
