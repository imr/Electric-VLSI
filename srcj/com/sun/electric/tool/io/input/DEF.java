/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: DEF.java
* Input/output tool: DEF (Design Exchange Format) reader
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
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.ArcProto.Function;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.io.IOTool;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * This class reads files in DEF files.
 * Note that this reader was built by examining DEF files and reverse-engineering them.
 * It does not claim to be compliant with the DEF specification, but it also does not
 * claim to define a new specification.  It is merely incomplete.
 */
public class DEF extends Input
{
	private String    io_defline;
	private String   io_deffilename;
	private int  io_deflinepos;
//	static INTBIG  io_defunits;
	private VIADEF io_deffirstviadef;

	private static class VIADEF
	{
		String       vianame;
		NodeProto  via;
		ArcProto   lay1, lay2;
		double      sx, sy;
		VIADEF nextviadef;
	};

	/**
	 * Method to import a library from disk.
	 * @param lib the library to fill
	 * @return true on error.
	 */
	protected boolean importALibrary(Library lib)
	{
		io_deffilename = lib.getLibFile().toString();
		io_deflinepos = 0;
		io_defline = "";
//		io_defunits = 1000;
		io_deffirstviadef = null;
	
		// read the file
		try
		{
			boolean ret = io_defreadfile(lib);
		} catch (IOException e)
		{
			System.out.println("ERROR reading DEF libraries");
		}
		return false;
	}

	private String io_defgetkeyword()
		throws IOException
	{
		// keep reading from file until something is found on a line
		for(;;)
		{
			if (io_defline.length() >= io_deflinepos)
			{
				io_defline = lineReader.readLine();
		
				// look for the first text on the line
				io_deflinepos = 0;
				continue;
			}

			while (io_deflinepos < io_defline.length())
			{
				char chr = io_defline.charAt(io_deflinepos);
				if (chr != ' ' && chr != '\t') break;
				io_deflinepos++;
			}
			if (io_deflinepos >= io_defline.length()) continue;

			// remember where the keyword begins
			int start = io_deflinepos;
		
			// scan to the end of the keyword
			while (io_deflinepos < io_defline.length())
			{
				char chr = io_defline.charAt(io_deflinepos);
				if (chr == ' ' || chr == '\t') break;
				io_deflinepos++;
			}
		
			// advance to the start of the next keyword
			return io_defline.substring(start, io_deflinepos);
		}
	}
	
	private boolean io_defignoretosemicolon(String command)
		throws IOException
	{
		// ignore up to the next semicolon
		for(;;)
		{
			String key = io_defmustgetkeyword(command);
			if (key == null) return true;
			if (key.equals(";")) break;
		}
		return false;
	}
	
	private String io_defmustgetkeyword(String where)
		throws IOException
	{
		String key = io_defgetkeyword();
		if (key == null) io_definerror("EOF parsing " + where);
		return key;
	}
	
	private void io_definerror(String command)
	{
		System.out.println("File " + io_deffilename + ", line " + lineReader.getLineNumber() + ": " + command);
	}

	/**
	 * Method to read the DEF file.
	 */
	private boolean io_defreadfile(Library lib)
		throws IOException
	{
//		REGISTER CHAR *key, *cellname;
//		REGISTER INTBIG *options;
//		CHAR curkey[200];
//		REGISTER NODEPROTO *cell;
	
//		options = io_getstatebits();

		Cell cell = null;
		for(;;)
		{
			// get the next keyword
			String key = io_defgetkeyword();
			if (key == null) break;
			if (key.equalsIgnoreCase("VERSION") || key.equalsIgnoreCase("NAMESCASESENSITIVE") ||
				key.equalsIgnoreCase("DIVIDERCHAR") || key.equalsIgnoreCase("BUSBITCHARS") ||
				key.equalsIgnoreCase("DIEAREA") || key.equalsIgnoreCase("ROW") ||
				key.equalsIgnoreCase("TRACKS") || key.equalsIgnoreCase("GCELLGRID") ||
				key.equalsIgnoreCase("HISTORY") || key.equalsIgnoreCase("TECHNOLOGY"))
			{
				String curkey = key;
//				if (io_defignoretosemicolon(curkey)) return true;
				continue;
			}
			if (key.equalsIgnoreCase("DEFAULTCAP") || key.equalsIgnoreCase("REGIONS"))
			{
//				if (io_defignoreblock(key)) return true;
				continue;
			}
			if (key.equalsIgnoreCase("DESIGN"))
			{
				String cellname = io_defmustgetkeyword("DESIGN");
				if (cellname == null) return true;
				cell = Cell.makeInstance(lib, cellname);
				if (cell == null)
				{
					io_definerror("Cannot create cell '" + cellname + "'");
					return true;
				}
				if (io_defignoretosemicolon("DESIGN")) return true;
				continue;
			}
	
			if (key.equalsIgnoreCase("UNITS"))
			{
//				if (io_defreadunits()) return true;
				continue;
			}
	
			if (key.equalsIgnoreCase("PROPERTYDEFINITIONS"))
			{
//				if (io_defreadpropertydefinitions()) return true;
				continue;
			}
	
			if (key.equalsIgnoreCase("VIAS"))
			{
				if (io_defreadvias(cell)) return true;
				continue;
			}
	
			if (key.equalsIgnoreCase("COMPONENTS"))
			{
//				if (io_defreadcomponents(cell)) return true;
				continue;
			}
	
			if (key.equalsIgnoreCase("PINS"))
			{
//				if (io_defreadpins(cell)) return true;
				continue;
			}
	
			if (key.equalsIgnoreCase("SPECIALNETS"))
			{
//				if (io_defreadnets(cell, true, options[0])) return true;
				continue;
			}
	
			if (key.equalsIgnoreCase("NETS"))
			{
//				if (io_defreadnets(cell, false, options[0])) return true;
				continue;
			}
	
			if (key.equalsIgnoreCase("END"))
			{
				key = io_defgetkeyword();
				break;
			}
		}
		return false;
	}

	private static class GetLayerInformation
	{
		NodeProto pin;
		NodeProto pure;
		ArcProto arc;

		GetLayerInformation(String name)
		{
//		/* returns nonzero on error */
//		void io_defgetlayernodes(CHAR *name, NODEPROTO **pin, NODEPROTO **pure, ARCPROTO **arc)
//		{
//			REGISTER INTBIG laynum, i, j, lfunc;
//			REGISTER INTBIG afunc, afunc1, afunc2, ap1found, ap2found, vialayer;
//			REGISTER ARCPROTO *ap, *ap1, *ap2;
//			REGISTER NODEPROTO *np;
//			REGISTER NODEINST *ni;
//			NODEINST node;
//			REGISTER PORTPROTO *pp;
//			static POLYGON *poly = NOPOLYGON;
		
			// initialize
			pin = null;
			pure = null;
			arc = null;
			ArcProto.Function aFunc1 = ArcProto.Function.UNKNOWN;
			ArcProto.Function aFunc2 = ArcProto.Function.UNKNOWN;
		
			// handle via layers
			int j = 0;
			if (name.startsWith("VIA")) j = 3; else
				if (name.startsWith("V")) j = 1;
			if (j != 0)
			{
				// find the two layer functions
				if (j >= name.length())
				{
					aFunc1 = ArcProto.Function.METAL1;
					aFunc2 = ArcProto.Function.METAL2;
				} else if (j+1 >= name.length())
				{
					int level = name.charAt(j) - '1';
					aFunc1 = ArcProto.Function.getMetal(level);
					aFunc2 = ArcProto.Function.getMetal(level + 1);
				} else
				{
					int level1 = name.charAt(j) - '1';
					aFunc1 = ArcProto.Function.getMetal(level1);
					int level2 = name.charAt(j+1) - '1';
					aFunc2 = ArcProto.Function.getMetal(level2);
				}
		
				// find the arcprotos that embody these layers
				ArcProto ap1 = null, ap2 = null;
				for(Iterator it = Technology.getCurrent().getArcs(); it.hasNext(); )
				{
					ArcProto apTry = (ArcProto)it.next();
					if (apTry.getFunction() == aFunc1) ap1 = apTry;
					if (apTry.getFunction() == aFunc2) ap2 = apTry;
				}
				if (ap1 == null || ap2 == null) return;
		
				// find the via that connects these two arcs
				for(Iterator it = Technology.getCurrent().getNodes(); it.hasNext(); )
				{
					NodeProto np = (NodeProto)it.next();
					// must have just one port
					if (np.getNumPorts() != 1) continue;
		
					// port must connect to both arcs
					PortProto pp = np.getPort(0);
					boolean ap1Found = pp.connectsTo(ap1);
					boolean ap2Found = pp.connectsTo(ap2);
					if (ap1Found && ap2Found) { pin = np;   break; }
				}
		
				// find the pure layer node that is the via contact
				if (pin != null)
				{
//					// find the layer on this node that is of type "contact"
//					ni = &node;   initdummynode(ni);
//					ni->proto = np;
//					ni->lowx = 0;   ni->highx = 1000;
//					ni->lowy = 0;   ni->highy = 1000;
//					j = nodepolys(ni, 0, NOWINDOWPART);
//					for(i=0; i<j; i++)
//					{
//						shapenodepoly(ni, i, poly);
//						vialayer = poly->layer;
//						lfunc = layerfunction(el_curtech, vialayer) & LFTYPE;
//						if (layeriscontact(lfunc)) break;
//					}
//					if (i >= j) return;
//		
//					// now find the pure layer node that has this layer
//					np = getpurelayernode(el_curtech, vialayer, 0);
//					*pure = np;
				}
				return;
			}
		
			// handle metal layers
			j = 0;
			if (name.startsWith("METAL")) j = 5; else
				if (name.startsWith("MET")) j = 3; else
					if (name.startsWith("M")) j = 1;
			if (j != 0)
			{
//				int laynum = TextUtils.atoi(name.substring(j));
//				ArcProto.Function afunc = ArcProto.Function.UNKNOWN;
//				Layer.Function lfunc = Layer.Function.UNKNOWN;
//				afunc = ArcProto.Function.getMetal(laynum);
//				
//				switch (laynum)
//				{
//					case 1:  afunc = APMETAL1;   lfunc = LFMETAL1;   break;
//					case 2:  afunc = APMETAL2;   lfunc = LFMETAL2;   break;
//					case 3:  afunc = APMETAL3;   lfunc = LFMETAL3;   break;
//					case 4:  afunc = APMETAL4;   lfunc = LFMETAL4;   break;
//					case 5:  afunc = APMETAL5;   lfunc = LFMETAL5;   break;
//					case 6:  afunc = APMETAL6;   lfunc = LFMETAL6;   break;
//					case 7:  afunc = APMETAL7;   lfunc = LFMETAL7;   break;
//					case 8:  afunc = APMETAL8;   lfunc = LFMETAL8;   break;
//					case 9:  afunc = APMETAL9;   lfunc = LFMETAL9;   break;
//					case 10: afunc = APMETAL10;  lfunc = LFMETAL10;  break;
//					case 11: afunc = APMETAL11;  lfunc = LFMETAL11;  break;
//					case 12: afunc = APMETAL12;  lfunc = LFMETAL12;  break;
//				}
//				if (afunc == APUNKNOWN || lfunc == LFUNKNOWN) return;
//		
//				// find the arc with this function
//				for(ap = el_curtech->firstarcproto; ap != NOARCPROTO; ap = ap->nextarcproto)
//					if ((INTBIG)((ap->userbits&AFUNCTION)>>AFUNCTIONSH) == afunc) break;
//				if (ap != NOARCPROTO)
//				{
//					*arc = ap;
//					*pin = getpinproto(ap);
//				}
//		
//				// find the pure layer node with this function
//				np = getpurelayernode(el_curtech, -1, lfunc);
//				*pure = np;
//				return;
			}
		}
	}

	/*************** VIAS ***************/
	
	private boolean io_defreadvias(Cell cell)
		throws IOException
	{
		if (io_defignoretosemicolon("VIAS")) return true;
		for(;;)
		{
			// get the next keyword
			String key = io_defmustgetkeyword("VIAs");
			if (key == null) return true;
			if (key.equals("-"))
			{
				if (io_defreadvia()) return true;
				continue;
			}
	
			if (key.equalsIgnoreCase("END"))
			{
				key = io_defgetkeyword();
				break;
			}
	
			// ignore the keyword
			String curkey = key;
			if (io_defignoretosemicolon(curkey)) return true;
		}
		return false;
	}
	
	private boolean io_defreadvia()
		throws IOException
	{
//		REGISTER CHAR *key;
//		INTBIG lx, hx, ly, hy;
//		NODEPROTO *pin, *pure;
//		ARCPROTO *ap;
//		REGISTER VIADEF *vd;
	
		// get the via name
		String key = io_defmustgetkeyword("VIA");
		if (key == null) return true;
	
		// create a new via definition
		VIADEF vd = new VIADEF();
		vd.vianame = key;
		vd.sx = vd.sy = 0;
		vd.via = null;
		vd.lay1 = vd.lay2 = null;
		vd.nextviadef = io_deffirstviadef;
		io_deffirstviadef = vd;
	
		for(;;)
		{
			// get the next keyword
			key = io_defmustgetkeyword("VIA");
			if (key == null) return true;
			if (key.equals("+"))
			{
				key = io_defmustgetkeyword("VIA");
				if (key == null) return true;
				if (key.equalsIgnoreCase("RECT"))
				{
					// handle definition of a via rectangle
					key = io_defmustgetkeyword("VIA");
					if (key == null) return true;
//					io_defgetlayernodes(key, &pin, &pure, &ap);
//					if (pure == null)
//					{
//						io_definerror("Layer " + key + " not in current technology");
//						pure = NONODEPROTO;
//					}
//					if (key.startsWith("VIA"))
//					{
//						if (pin == null) pin = gen_univpinprim;
//						vd.via = pin;
//					}
//					if (key.startsWith("METAL"))
//					{
//						if (ap == null) ap = Generic.tech.universal_arc;
//						if (vd.lay1 == null) vd.lay1 = ap; else
//							vd.lay2 = ap;
//					}
//					if (io_defreadcoordinate(&lx, &ly)) return true;
//					if (io_defreadcoordinate(&hx, &hy)) return true;
//	
//					// accumulate largest contact size
//					if (hx-lx > vd.sx) vd.sx = hx - lx;
//					if (hy-ly > vd.sy) vd.sy = hy - ly;
					continue;
				}
				continue;
			}
	
			if (key.equals(";")) break;
		}
		if (vd.via != null)
		{
			if (vd.sx == 0) vd.sx = vd.via.getDefWidth();
			if (vd.sy == 0) vd.sy = vd.via.getDefHeight();
		}
		return false;
	}
	
}
//	/*************** COMPONENTS ***************/
//	
//	BOOLEAN io_defreadcomponents(FILE *f, NODEPROTO *cell)
//	{
//		REGISTER CHAR *key;
//		CHAR curkey[200];
//	
//		if (io_defignoretosemicolon(f, _("COMPONENTS"))) return(1);
//		for(;;)
//		{
//			// get the next keyword
//			key = io_defmustgetkeyword(f, x_("COMPONENTs"));
//			if (key == 0) return true;
//			if (namesame(key, x_("-")) == 0)
//			{
//				if (io_defreadcomponent(f, cell)) return true;
//				continue;
//			}
//	
//			if (namesame(key, x_("END")) == 0)
//			{
//				key = io_defgetkeyword(f);
//				break;
//			}
//	
//			// ignore the keyword
//			estrcpy(curkey, key);
//			if (io_defignoretosemicolon(f, curkey)) return true;
//		}
//		return false;
//	}
//	
//	BOOLEAN io_defreadcomponent(FILE *f, NODEPROTO *cell)
//	{
//		REGISTER CHAR *key;
//		INTBIG x, y, cx, cy;
//		INTBIG sx, sy;
//		REGISTER INTBIG lx, hx, ly, hy;
//		REGISTER NODEINST *ni;
//		REGISTER NODEPROTO *np;
//		REGISTER VARIABLE *var;
//		INTSML rot, trans;
//		CHAR compname[200], modelname[200];
//	
//		// get the component name and model name
//		key = io_defmustgetkeyword(f, x_("COMPONENT"));
//		if (key == 0) return true;
//		estrcpy(compname, key);
//		key = io_defmustgetkeyword(f, x_("COMPONENT"));
//		if (key == 0) return true;
//		estrcpy(modelname, key);
//	
//		// find the named cell
//		np = io_defgetnodeproto(modelname, cell->lib);
//		if (np == NONODEPROTO)
//		{
//			io_definerror(_("Unknown cell (%s)"), modelname);
//			return true;
//		}
//	
//		for(;;)
//		{
//			// get the next keyword
//			key = io_defmustgetkeyword(f, x_("COMPONENT"));
//			if (key == 0) return true;
//			if (namesame(key, x_("+")) == 0)
//			{
//				key = io_defmustgetkeyword(f, x_("COMPONENT"));
//				if (key == 0) return true;
//				if (namesame(key, x_("PLACED")) == 0 || namesame(key, x_("FIXED")) == 0)
//				{
//					// handle placement
//					if (io_defreadcoordinate(f, &x, &y)) return true;
//					if (io_defreadorientation(f, &rot, &trans)) return true;
//	
//					// place the node
//					defaultnodesize(np, &sx, &sy);
//					corneroffset(NONODEINST, np, 0, 0, &cx, &cy, FALSE);
//					lx = x - cx;   hx = lx + sx;
//					ly = y - cy;   hy = ly + sy;
//					ni = newnodeinst(np, lx, hx, ly, hy, trans, rot, cell);
//					if (ni == NONODEINST)
//					{
//						io_definerror(_("Unable to create node"));
//						return true;
//					}
//					endobjectchange((INTBIG)ni, VNODEINST);
//					var = setvalkey((INTBIG)ni, VNODEINST, el_node_name_key,
//						(INTBIG)compname, VSTRING|VDISPLAY);
//					if (var != NOVARIABLE)
//						defaulttextsize(3, var->textdescript);
//					continue;
//				}
//				continue;
//			}
//	
//			if (namesame(key, x_(";")) == 0)
//				break;
//		}
//		return false;
//	}
//	
//	/*************** PINS ***************/
//	
//	BOOLEAN io_defreadpins(FILE *f, NODEPROTO *cell)
//	{
//		REGISTER CHAR *key;
//		CHAR curkey[200];
//	
//		if (io_defignoretosemicolon(f, _("PINS"))) return true;
//		for(;;)
//		{
//			// get the next keyword
//			key = io_defmustgetkeyword(f, x_("PINs"));
//			if (key == 0) return true;
//			if (namesame(key, x_("-")) == 0)
//			{
//				if (io_defreadpin(f, cell)) return true;
//				continue;
//			}
//	
//			if (namesame(key, x_("END")) == 0)
//			{
//				key = io_defgetkeyword(f);
//				break;
//			}
//	
//			// ignore the keyword
//			estrcpy(curkey, key);
//			if (io_defignoretosemicolon(f, curkey)) return true;
//		}
//		return false;
//	}
//	
//	BOOLEAN io_defreadpin(FILE *f, NODEPROTO *cell)
//	{
//		REGISTER CHAR *key;
//		INTBIG lx, hx, ly, hy, x, y;
//		REGISTER INTBIG portbits, i, havecoord;
//		REGISTER NODEINST *ni;
//		NODEPROTO *np, *pure;
//		ARCPROTO *ap;
//		REGISTER PORTPROTO *pp;
//		INTSML rot, trn;
//		CHAR pinname[200];
//		XARRAY trans;
//	
//		// get the pin name
//		key = io_defmustgetkeyword(f, x_("PIN"));
//		if (key == 0) return true;
//		estrcpy(pinname, key);
//		portbits = havecoord = 0;
//		np = NONODEPROTO;
//	
//		for(;;)
//		{
//			// get the next keyword
//			key = io_defmustgetkeyword(f, x_("PIN"));
//			if (key == 0) return true;
//			if (namesame(key, x_("+")) == 0)
//			{
//				key = io_defmustgetkeyword(f, x_("PIN"));
//				if (key == 0) return true;
//				if (namesame(key, x_("NET")) == 0)
//				{
//					key = io_defmustgetkeyword(f, _("net name"));
//					if (key == 0) return(TRUE);
//					continue;
//				}
//				if (namesame(key, x_("DIRECTION")) == 0)
//				{
//					key = io_defmustgetkeyword(f, x_("DIRECTION"));
//					if (key == 0) return(TRUE);
//					if (namesame(key, x_("INPUT")) == 0) portbits = INPORT; else
//					if (namesame(key, x_("OUTPUT")) == 0) portbits = OUTPORT; else
//					if (namesame(key, x_("INOUT")) == 0) portbits = BIDIRPORT; else
//					if (namesame(key, x_("FEEDTHRU")) == 0) portbits = BIDIRPORT; else
//					{
//						io_definerror(_("Unknown direction (%s)"), key);
//						return(TRUE);
//					}
//					continue;
//				}
//				if (namesame(key, x_("USE")) == 0)
//				{
//					key = io_defmustgetkeyword(f, x_("USE"));
//					if (key == 0) return(TRUE);
//					if (namesame(key, x_("SIGNAL")) == 0) portbits = portbits; else
//					if (namesame(key, x_("POWER")) == 0) portbits = PWRPORT; else
//					if (namesame(key, x_("GROUND")) == 0) portbits = GNDPORT; else
//					if (namesame(key, x_("CLOCK")) == 0) portbits = CLKPORT; else
//					if (namesame(key, x_("TIEOFF")) == 0) portbits = portbits; else
//					if (namesame(key, x_("ANALOG")) == 0) portbits = portbits; else
//					{
//						io_definerror(_("Unknown usage (%s)"), key);
//						return(TRUE);
//					}
//					continue;
//				}
//				if (namesame(key, x_("LAYER")) == 0)
//				{
//					key = io_defmustgetkeyword(f, x_("LAYER"));
//					if (key == 0) return(TRUE);
//					io_defgetlayernodes(key, &np, &pure, &ap);
//					if (np == NONODEPROTO)
//					{
//						io_definerror(_("Unknown layer (%s)"), key);
//						return(TRUE);
//					}
//					if (io_defreadcoordinate(f, &lx, &ly)) return(TRUE);
//					if (io_defreadcoordinate(f, &hx, &hy)) return(TRUE);
//					continue;
//				}
//				if (namesame(key, x_("PLACED")) == 0)
//				{
//					// get pin location and orientation
//					if (io_defreadcoordinate(f, &x, &y)) return(TRUE);
//					if (io_defreadorientation(f, &rot, &trn)) return(TRUE);
//					havecoord = 1;
//					continue;
//				}
//				continue;
//			}
//	
//			if (namesame(key, x_(";")) == 0)
//				break;
//		}
//	
//		// all factors read, now place the pin
//		if (np != NONODEPROTO && havecoord != 0)
//		{
//			// determine the pin size
//			makeangle(rot, trn, trans);
//			rot = trn = 0;
//			xform(lx, ly, &lx, &ly, trans);
//			xform(hx, hy, &hx, &hy, trans);
//			if (lx > hx) { i = lx;   lx = hx;   hx = i; }
//			if (ly > hy) { i = ly;   ly = hy;   hy = i; }
//			lx += x;   hx += x;
//			ly += y;   hy += y;
//	
//			// make the pin
//			ni = newnodeinst(np, lx, hx, ly, hy, trn, rot, cell);
//			if (ni == NONODEINST)
//			{
//				io_definerror(_("Unable to create pin"));
//				return(TRUE);
//			}
//			endobjectchange((INTBIG)ni, VNODEINST);
//			pp = newportproto(cell, ni, np->firstportproto, pinname);
//			if (pp == NOPORTPROTO)
//			{
//				io_definerror(_("Unable to create pin name"));
//				return(TRUE);
//			}
//			pp->userbits = (pp->userbits & ~STATEBITS) | portbits;
//		}
//		return false;
//	}
//	
//	/*************** NETS ***************/
//	
//	BOOLEAN io_defreadnets(FILE *f, NODEPROTO *cell, BOOLEAN special, INTBIG options)
//	{
//		REGISTER CHAR *key;
//		CHAR curkey[200];
//	
//		for(;;)
//		{
//			// get the next keyword
//			key = io_defmustgetkeyword(f, x_("NETs"));
//			if (key == 0) return(TRUE);
//			if (namesame(key, x_("-")) == 0)
//			{
//				if (io_defreadnet(f, cell, special, options)) return(TRUE);
//				continue;
//			}
//			if (namesame(key, x_("END")) == 0)
//			{
//				key = io_defgetkeyword(f);
//				break;
//			}
//	
//			// ignore the keyword
//			estrcpy(curkey, key);
//			if (io_defignoretosemicolon(f, curkey)) return(TRUE);
//		}
//		return false;
//	}
//	
//	BOOLEAN io_defreadnet(FILE *f, NODEPROTO *cell, BOOLEAN special, INTBIG options)
//	{
//		REGISTER CHAR *key;
//		INTBIG lx, hx, ly, hy, plx, ply, phx, phy, sx, sy, fx, fy, tx, ty;
//		REGISTER INTBIG i, curx, cury, lastx, lasty, pathstart, placedvia, width,
//			wantpinpairs, specialwidth, bits;
//		NODEINST *ni, *lastni, *nextni;
//		REGISTER NODEINST *lastlogni;
//		REGISTER ARCINST *ai;
//		REGISTER BOOLEAN foundcoord;
//		PORTPROTO *pp, *lastpp, *nextpp;
//		REGISTER PORTPROTO *lastlogpp;
//		REGISTER NODEPROTO *np;
//		NODEPROTO *pin, *pure;
//		ARCPROTO *routap;
//		REGISTER VARIABLE *var;
//		float v;
//		CHAR netname[200];
//		REGISTER VIADEF *vd;
//	
//		// get the net name
//		key = io_defmustgetkeyword(f, x_("NET"));
//		if (key == 0) return(TRUE);
//		estrcpy(netname, key);
//	
//		// get the next keyword
//		key = io_defmustgetkeyword(f, x_("NET"));
//		if (key == 0) return(TRUE);
//	
//		// scan the "net" statement
//		wantpinpairs = 1;
//		lastx = lasty = 0;
//		pathstart = 1;
//		lastlogni = NONODEINST;
//		for(;;)
//		{
//			// examine the next keyword
//			if (namesame(key, x_(";")) == 0) break;
//	
//			if (namesame(key, x_("+")) == 0)
//			{
//				wantpinpairs = 0;
//				key = io_defmustgetkeyword(f, x_("NET"));
//				if (key == 0) return(TRUE);
//	
//				if (namesame(key, x_("USE")) == 0)
//				{
//					// ignore "USE" keyword
//					key = io_defmustgetkeyword(f, x_("NET"));
//					if (key == 0) return(TRUE);
//				} else if (namesame(key, x_("ROUTED")) == 0)
//				{
//					// handle "ROUTED" keyword
//					key = io_defmustgetkeyword(f, x_("NET"));
//					if (key == 0) return(TRUE);
//					io_defgetlayernodes(key, &pin, &pure, &routap);
//					if (pin == NONODEPROTO)
//					{
//						io_definerror(_("Unknown layer (%s)"), key);
//						return(TRUE);
//					}
//					pathstart = 1;
//					if (special)
//					{
//						// specialnets have width here
//						key = io_defmustgetkeyword(f, x_("NET"));
//						if (key == 0) return(TRUE);
//						v = (float)eatof(key) / (float)io_defunits;
//						specialwidth = scalefromdispunit(v, DISPUNITMIC);
//					}
//				} else if (namesame(key, x_("FIXED")) == 0)
//				{
//					// handle "FIXED" keyword
//					key = io_defmustgetkeyword(f, x_("NET"));
//					if (key == 0) return(TRUE);
//					io_defgetlayernodes(key, &pin, &pure, &routap);
//					if (pin == NONODEPROTO)
//					{
//						io_definerror(_("Unknown layer (%s)"), key);
//						return(TRUE);
//					}
//					pathstart = 1;
//				} else if (namesame(key, x_("SHAPE")) == 0)
//				{
//					// handle "SHAPE" keyword
//					key = io_defmustgetkeyword(f, x_("NET"));
//					if (key == 0) return(TRUE);
//				} else
//				{
//					io_definerror(_("Cannot handle '%s' nets"), key);
//					return(TRUE);
//				}
//	
//				// get next keyword
//				key = io_defmustgetkeyword(f, x_("NET"));
//				if (key == 0) return(TRUE);
//				continue;
//			}
//	
//			// if still parsing initial pin pairs, do so
//			if (wantpinpairs != 0)
//			{
//				// it must be the "(" of a pin pair
//				if (namesame(key, x_("(")) != 0)
//				{
//					io_definerror(_("Expected '(' of pin pair"));
//					return(TRUE);
//				}
//	
//				// get the pin names
//				key = io_defmustgetkeyword(f, x_("NET"));
//				if (key == 0) return(TRUE);
//				if (namesame(key, x_("PIN")) == 0)
//				{
//					// find the export
//					key = io_defmustgetkeyword(f, x_("NET"));
//					if (key == 0) return(TRUE);
//					pp = getportproto(cell, key);
//					if (pp == NOPORTPROTO)
//					{
//						io_definerror(_("Warning: unknown pin '%s'"), key);
//						if (io_defignoretosemicolon(f, _("NETS"))) return(TRUE);
//						return false;
//					}
//					ni = pp->subnodeinst;
//					pp = pp->subportproto;
//				} else
//				{
//					for(ni = cell->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//					{
//						var = getvalkey((INTBIG)ni, VNODEINST, VSTRING, el_node_name_key);
//						if (var == NOVARIABLE) continue;
//						if (namesame((CHAR *)var->addr, key) == 0) break;
//					}
//					if (ni == NONODEINST)
//					{
//						io_definerror(_("Unknown component '%s'"), key);
//						return(TRUE);
//					}
//	
//					// get the port name
//					key = io_defmustgetkeyword(f, x_("NET"));
//					if (key == 0) return(TRUE);
//					pp = getportproto(ni->proto, key);
//					if (pp == NOPORTPROTO)
//					{
//						io_definerror(_("Unknown port '%s' on component '%s'"),
//							key, (CHAR *)var->addr);
//						return(TRUE);
//					}
//				}
//	
//				// get the close parentheses
//				key = io_defmustgetkeyword(f, x_("NET"));
//				if (key == 0) return(TRUE);
//				if (namesame(key, x_(")")) != 0)
//				{
//					io_definerror(_("Expected ')' of pin pair"));
//					return(TRUE);
//				}
//	
//				if (lastlogni != NONODEINST && (options&DEFNOLOGICAL) == 0)
//				{
//					portposition(ni, pp, &fx, &fy);
//	
//					// LINTED "lastlogpp" used in proper order
//					portposition(lastlogni, lastlogpp, &tx, &ty);
//					bits = us_makearcuserbits(gen_unroutedarc);
//					ai = newarcinst(gen_unroutedarc, defaultarcwidth(gen_unroutedarc), bits,
//						ni, pp, fx, fy, lastlogni, lastlogpp, tx, ty, cell);
//					if (ai == NOARCINST)
//					{
//						io_definerror(_("Could not create unrouted arc"));
//						return(TRUE);
//					}
//					endobjectchange((INTBIG)ai, VARCINST);
//				}
//				lastlogni = ni;
//				lastlogpp = pp;
//	
//				// get the next keyword and continue parsing
//				key = io_defmustgetkeyword(f, x_("NET"));
//				if (key == 0) return(TRUE);
//				continue;
//			}
//	
//			// handle "new" start of coordinate trace
//			if (namesame(key, x_("NEW")) == 0)
//			{
//				key = io_defmustgetkeyword(f, x_("NET"));
//				if (key == 0) return(TRUE);
//				io_defgetlayernodes(key, &pin, &pure, &routap);
//				if (pin == NONODEPROTO)
//				{
//					io_definerror(_("Unknown layer (%s)"), key);
//					return(TRUE);
//				}
//				pathstart = 1;
//				key = io_defmustgetkeyword(f, x_("NET"));
//				if (key == 0) return(TRUE);
//				if (special)
//				{
//					// specialnets have width here
//					v = (float)eatof(key) / (float)io_defunits;
//					specialwidth = scalefromdispunit(v, DISPUNITMIC);
//	
//					// get the next keyword
//					key = io_defmustgetkeyword(f, x_("NET"));
//					if (key == 0) return(TRUE);
//				}
//				continue;
//			}
//	
//			foundcoord = FALSE;
//			if (namesame(key, x_("(")) == 0)
//			{
//				// get the X coordinate
//				foundcoord = TRUE;
//				key = io_defmustgetkeyword(f, x_("NET"));
//				if (key == 0) return(TRUE);
//				if (estrcmp(key, x_("*")) == 0) curx = lastx; else
//				{
//					v = (float)eatof(key) / (float)io_defunits;
//					curx = scalefromdispunit(v, DISPUNITMIC);
//				}
//	
//				// get the Y coordinate
//				key = io_defmustgetkeyword(f, x_("NET"));
//				if (key == 0) return(TRUE);
//				if (estrcmp(key, x_("*")) == 0) cury = lasty; else
//				{
//					v = (float)eatof(key) / (float)io_defunits;
//					cury = scalefromdispunit(v, DISPUNITMIC);
//				}
//	
//				// get the close parentheses
//				key = io_defmustgetkeyword(f, x_("NET"));
//				if (key == 0) return(TRUE);
//				if (namesame(key, x_(")")) != 0)
//				{
//					io_definerror(_("Expected ')' of coordinate pair"));
//					return(TRUE);
//				}
//			}
//	
//			// get the next keyword
//			key = io_defmustgetkeyword(f, x_("NET"));
//			if (key == 0) return(TRUE);
//	
//			// see if it is a via name
//			for(vd = io_deffirstviadef; vd != NOVIADEF; vd = vd->nextviadef)
//				if (namesame(key, vd->vianame) == 0) break;
//			if (vd == NOVIADEF)
//			{
//				// see if the via name is from the LEF file
//				for(vd = io_leffirstviadef; vd != NOVIADEF; vd = vd->nextviadef)
//					if (namesame(key, vd->vianame) == 0) break;
//			}
//	
//			// stop now if not placing physical nets
//			if ((options&DEFNOPHYSICAL) != 0)
//			{
//				// ignore the next keyword if a via name is coming
//				if (vd != NOVIADEF)
//				{
//					key = io_defmustgetkeyword(f, x_("NET"));
//					if (key == 0) return(TRUE);
//				}
//				continue;
//			}
//	
//			// if a via is mentioned next, use it
//			if (vd != NOVIADEF)
//			{
//				// place the via at this location
//				sx = vd->sx;
//				sy = vd->sy;
//				lx = curx - sx / 2;   hx = lx + sx;
//				ly = cury - sy / 2;   hy = ly + sy;
//				if (vd->via == NONODEPROTO)
//				{
//					io_definerror(_("Cannot to create via"));
//					return(TRUE);
//				}
//	
//				// see if there is a connection point here when starting a path
//				if (pathstart != 0)
//				{
//					if (!io_deffindconnection(curx, cury, routap, cell, NONODEINST,
//						&lastni, &lastpp)) lastni = NONODEINST;
//				}
//	
//				// create the via
//				nodeprotosizeoffset(vd->via, &plx, &ply, &phx, &phy, cell);
//				ni = newnodeinst(vd->via, lx-plx, hx+phx, ly-ply, hy+phy, 0, 0, cell);
//				if (ni == NONODEINST)
//				{
//					io_definerror(_("Unable to create via layer"));
//					return(TRUE);
//				}
//				endobjectchange((INTBIG)ni, VNODEINST);
//				pp = ni->proto->firstportproto;
//	
//				// if the path starts with a via, wire it
//				if (pathstart != 0 && lastni != NONODEINST && foundcoord)
//				{
//					if (special) width = specialwidth; else
//					{
//						var = getval((INTBIG)routap, VARCPROTO, VINTEGER, x_("IO_lef_width"));
//						if (var == NOVARIABLE) width = defaultarcwidth(routap); else
//							width = var->addr;
//					}
//					ai = newarcinst(routap, width, us_makearcuserbits(routap),
//						lastni, lastpp, curx, cury, ni, pp, curx, cury, cell);
//					if (ai == NOARCINST)
//					{
//						io_definerror(_("Unable to create net starting point"));
//						return(TRUE);
//					}
//					endobjectchange((INTBIG)ai, VARCINST);
//				}
//	
//				// remember that a via was placed
//				placedvia = 1;
//	
//				// get the next keyword
//				key = io_defmustgetkeyword(f, x_("NET"));
//				if (key == 0) return(TRUE);
//			} else
//			{
//				// no via mentioned: just make a pin
//				if (io_defgetpin(curx, cury, routap, cell, &ni, &pp)) return(TRUE);
//				placedvia = 0;
//			}
//			if (!foundcoord) continue;
//	
//			// run the wire
//			if (pathstart == 0)
//			{
//				// make sure that this arc can connect to the current pin
//				for(i=0; pp->connects[i] != NOARCPROTO; i++)
//					if (pp->connects[i] == routap) break;
//				if (pp->connects[i] == NOARCPROTO)
//				{
//					np = getpinproto(routap);
//					defaultnodesize(np, &sx, &sy);
//					lx = curx - sx / 2;   hx = lx + sx;
//					ly = cury - sy / 2;   hy = ly + sy;
//					ni = newnodeinst(np, lx, hx, ly, hy, 0, 0, cell);
//					if (ni == NONODEINST)
//					{
//						io_definerror(_("Unable to create net pin"));
//						return(TRUE);
//					}
//					endobjectchange((INTBIG)ni, VNODEINST);
//					pp = ni->proto->firstportproto;
//				}
//	
//				// run the wire
//				if (special) width = specialwidth; else
//				{
//					var = getval((INTBIG)routap, VARCPROTO, VINTEGER, x_("IO_lef_width"));
//					if (var == NOVARIABLE) width = defaultarcwidth(routap); else
//						width = var->addr;
//				}
//				ai = newarcinst(routap, width, us_makearcuserbits(routap),
//					lastni, lastpp, lastx, lasty, ni, pp, curx, cury, cell);
//				if (ai == NOARCINST)
//				{
//					io_definerror(_("Unable to create net path"));
//					return(TRUE);
//				}
//				endobjectchange((INTBIG)ai, VARCINST);
//			}
//			lastx = curx;   lasty = cury;
//			pathstart = 0;
//			lastni = ni;
//			lastpp = pp;
//	
//			// switch layers to the other one supported by the via
//			if (placedvia != 0)
//			{
//				if (routap == vd->lay1)
//				{
//					routap = vd->lay2;
//				} else if (routap == vd->lay2)
//				{
//					routap = vd->lay1;
//				}
//				pin = getpinproto(routap);
//			}
//	
//			// if the path ends here, connect it
//			if (namesame(key, x_("NEW")) == 0 || namesame(key, x_(";")) == 0)
//			{
//				// see if there is a connection point here when starting a path
//				if (!io_deffindconnection(curx, cury, routap, cell, ni, &nextni, &nextpp))
//					nextni = NONODEINST;
//	
//				// if the path starts with a via, wire it
//				if (nextni != NONODEINST)
//				{
//					if (special) width = specialwidth; else
//					{
//						var = getval((INTBIG)routap, VARCPROTO, VINTEGER, x_("IO_lef_width"));
//						if (var == NOVARIABLE) width = defaultarcwidth(routap); else
//							width = var->addr;
//					}
//					ai = newarcinst(routap, width, us_makearcuserbits(routap),
//						ni, pp, curx, cury, nextni, nextpp, curx, cury, cell);
//					if (ai == NOARCINST)
//					{
//						io_definerror(_("Unable to create net ending point"));
//						return(TRUE);
//					}
//					endobjectchange((INTBIG)ai, VARCINST);
//				}
//			}
//		}
//		return false;
//	}
//	
//	/*
//	 * Routine to look for a connection to arcs of type "ap" in cell "cell"
//	 * at (x, y).  The connection can not be on "not" (if it is not NONODEINST).
//	 * If found, return true and places the node and port in "theni" and "thepp".
//	 */
//	BOOLEAN io_deffindconnection(INTBIG x, INTBIG y, ARCPROTO *ap, NODEPROTO *cell, NODEINST *noti,
//		NODEINST **theni, PORTPROTO **thepp)
//	{
//		REGISTER INTBIG sea, i;
//		REGISTER NODEINST *ni;
//		REGISTER PORTPROTO *pp;
//		REGISTER GEOM *geom;
//		static POLYGON *poly = NOPOLYGON;
//	
//		// get polygons
//		(void)needstaticpolygon(&poly, 4, us_tool->cluster);
//	
//		sea = initsearch(x, x, y, y, cell);
//		for(;;)
//		{
//			geom = nextobject(sea);
//			if (geom == NOGEOM) break;
//			if (!geom->entryisnode) continue;
//			ni = geom->entryaddr.ni;
//			if (ni == noti) continue;
//			for(pp = ni->proto->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//			{
//				for(i=0; pp->connects[i] != NOARCPROTO; i++)
//					if (pp->connects[i] == ap) break;
//				if (pp->connects[i] == NOARCPROTO) continue;
//				shapeportpoly(ni, pp, poly, FALSE);
//				if (isinside(x, y, poly))
//				{
//					termsearch(sea);
//					*theni = ni;
//					*thepp = pp;
//					return(TRUE);
//				}
//			}
//		}
//		return false;
//	}
//	
//	/*
//	 * Routine to look for a connection to arcs of type "ap" in cell "cell"
//	 * at (x, y).  If nothing is found, create a pin.  In any case, return
//	 * the node and port in "theni" and "thepp".  Returns true on error.
//	 */
//	BOOLEAN io_defgetpin(INTBIG x, INTBIG y, ARCPROTO *ap, NODEPROTO *cell,
//		NODEINST **theni, PORTPROTO **thepp)
//	{
//		INTBIG sx, sy;
//		REGISTER INTBIG lx, hx, ly, hy;
//		REGISTER NODEINST *ni;
//		REGISTER NODEPROTO *pin;
//	
//		// if there is an existing connection, return it
//		if (io_deffindconnection(x, y, ap, cell, NONODEINST, theni, thepp)) return false;
//	
//		// nothing found at this location: create a pin
//		pin = getpinproto(ap);
//		defaultnodesize(pin, &sx, &sy);
//		lx = x - sx / 2;   hx = lx + sx;
//		ly = y - sy / 2;   hy = ly + sy;
//		ni = newnodeinst(pin, lx, hx, ly, hy, 0, 0, cell);
//		if (ni == NONODEINST)
//		{
//			io_definerror(_("Unable to create net pin"));
//			return(TRUE);
//		}
//		endobjectchange((INTBIG)ni, VNODEINST);
//		*theni = ni;
//		*thepp = ni->proto->firstportproto;
//		return false;
//	}
//	
//	/*************** UNITS ***************/
//	
//	BOOLEAN io_defreadunits(FILE *f)
//	{
//		REGISTER CHAR *key;
//	
//		// get the "DISTANCE" keyword
//		key = io_defmustgetkeyword(f, x_("UNITS"));
//		if (key == 0) return(TRUE);
//		if (namesame(key, x_("DISTANCE")) != 0)
//		{
//			io_definerror(_("Expected 'DISTANCE' after 'UNITS'"));
//			return(TRUE);
//		}
//	
//		// get the "MICRONS" keyword
//		key = io_defmustgetkeyword(f, x_("UNITS"));
//		if (key == 0) return(TRUE);
//		if (namesame(key, x_("MICRONS")) != 0)
//		{
//			io_definerror(_("Expected 'MICRONS' after 'UNITS'"));
//			return(TRUE);
//		}
//	
//		// get the amount
//		key = io_defmustgetkeyword(f, x_("UNITS"));
//		if (key == 0) return(TRUE);
//		io_defunits = eatoi(key);
//	
//		// ignore the keyword
//		if (io_defignoretosemicolon(f, _("UNITS"))) return(TRUE);
//		return false;
//	}
//	
//	/*************** PROPERTY DEFINITIONS ***************/
//	
//	BOOLEAN io_defreadpropertydefinitions(FILE *f)
//	{
//		REGISTER CHAR *key;
//		CHAR curkey[200];
//	
//		for(;;)
//		{
//			// get the next keyword
//			key = io_defmustgetkeyword(f, x_("PROPERTYDEFINITION"));
//			if (key == 0) return(TRUE);
//			if (namesame(key, x_("END")) == 0)
//			{
//				key = io_defgetkeyword(f);
//				break;
//			}
//	
//			// ignore the keyword
//			estrcpy(curkey, key);
//			if (io_defignoretosemicolon(f, curkey)) return(TRUE);
//		}
//		return false;
//	}
//	
//	/*************** DATABASE SUPPORT ***************/
//	
//	NODEPROTO *io_defgetnodeproto(CHAR *name, LIBRARY *curlib)
//	{
//		REGISTER NODEPROTO *np, *newnp;
//		REGISTER LIBRARY *lib;
//	
//		// first see if this cell is in the current library
//		for(np = curlib->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//			if (namesame(name, np->protoname) == 0) return(np);
//	
//		// now look in other libraries
//		for(lib = el_curlib; lib != NOLIBRARY; lib = lib->nextlibrary)
//		{
//			if (lib == curlib) continue;
//			for(np = lib->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//				if (namesame(name, np->protoname) == 0) break;
//			if (np != NONODEPROTO)
//			{
//				// must copy the cell
//				newnp = us_copyrecursively(np, np->protoname, curlib, np->cellview,
//					FALSE, FALSE, x_(""), FALSE, FALSE, FALSE);
//				return(newnp);
//			}
//		}
//		return(NONODEPROTO);
//	}
//	
//	
//	/*************** FILE SUPPORT ***************/
//	
//	BOOLEAN io_defreadcoordinate(FILE *f, INTBIG *x, INTBIG *y)
//	{
//		CHAR *key;
//		float v;
//	
//		// get "("
//		key = io_defmustgetkeyword(f, _("coordinate"));
//		if (key == 0) return(TRUE);
//		if (estrcmp(key, x_("(")) != 0)
//		{
//			io_definerror(_("Expected '(' in coordinate"));
//			return(TRUE);
//		}
//	
//		// get X
//		key = io_defmustgetkeyword(f, _("coordinate"));
//		if (key == 0) return(TRUE);
//		v = (float)eatof(key) / (float)io_defunits;
//		*x = scalefromdispunit(v, DISPUNITMIC);
//	
//		// get Y
//		key = io_defmustgetkeyword(f, _("coordinate"));
//		if (key == 0) return(TRUE);
//		v = (float)eatof(key) / (float)io_defunits;
//		*y = scalefromdispunit(v, DISPUNITMIC);
//	
//		// get ")"
//		key = io_defmustgetkeyword(f, _("coordinate"));
//		if (key == 0) return(TRUE);
//		if (estrcmp(key, x_(")")) != 0)
//		{
//			io_definerror(_("Expected ')' in coordinate"));
//			return(TRUE);
//		}
//		return false;
//	}
//	
//	BOOLEAN io_defreadorientation(FILE *f, INTSML *rot, INTSML *trans)
//	{
//		REGISTER CHAR *key;
//	
//		key = io_defmustgetkeyword(f, _("orientation"));
//		if (key == 0) return(TRUE);
//		if (namesame(key, x_("N"))  == 0) { *rot = 0;    *trans = 0; } else
//		if (namesame(key, x_("S"))  == 0) { *rot = 1800; *trans = 0; } else
//		if (namesame(key, x_("E"))  == 0) { *rot = 2700; *trans = 0; } else
//		if (namesame(key, x_("W"))  == 0) { *rot = 900;  *trans = 0; } else
//		if (namesame(key, x_("FN")) == 0) { *rot = 900;  *trans = 1; } else
//		if (namesame(key, x_("FS")) == 0) { *rot = 2700; *trans = 1; } else
//		if (namesame(key, x_("FE")) == 0) { *rot = 1800; *trans = 1; } else
//		if (namesame(key, x_("FW")) == 0) { *rot = 0;    *trans = 1; } else
//		{
//			io_definerror(_("Unknown orientation (%s)"), key);
//			return(TRUE);
//		}
//		return false;
//	}
//	
//	BOOLEAN io_defignoreblock(FILE *f, CHAR *command)
//	{
//		REGISTER CHAR *key;
//	
//		for(;;)
//		{
//			// get the next keyword
//			key = io_defmustgetkeyword(f, command);
//			if (key == 0) return(TRUE);
//	
//			if (namesame(key, x_("END")) == 0)
//			{
//				(void)io_defgetkeyword(f);
//				break;
//			}
//		}
//		return false;
//	}
