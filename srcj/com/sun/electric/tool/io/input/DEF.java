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
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.ArcProto.Function;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.io.IOTool;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
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
	private String  io_defline;
	private String  io_deffilename;
	private int     io_deflinepos;
	private double  io_defunits;
	private VIADEF  io_deffirstviadef;

	private static class VIADEF
	{
		private String    vianame;
		private NodeProto via;
		private ArcProto  lay1, lay2;
		private double    sx, sy;
		private VIADEF    nextviadef;
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
		io_defunits = 1000;
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
				if (io_defignoretosemicolon(curkey)) return true;
				continue;
			}
			if (key.equalsIgnoreCase("DEFAULTCAP") || key.equalsIgnoreCase("REGIONS"))
			{
				if (io_defignoreblock(key)) return true;
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
				if (io_defreadunits()) return true;
				continue;
			}
	
			if (key.equalsIgnoreCase("PROPERTYDEFINITIONS"))
			{
				if (io_defreadpropertydefinitions()) return true;
				continue;
			}
	
			if (key.equalsIgnoreCase("VIAS"))
			{
				if (io_defreadvias(cell)) return true;
				continue;
			}
	
			if (key.equalsIgnoreCase("COMPONENTS"))
			{
				if (io_defreadcomponents(cell)) return true;
				continue;
			}
	
			if (key.equalsIgnoreCase("PINS"))
			{
				if (io_defreadpins(cell)) return true;
				continue;
			}
	
			if (key.equalsIgnoreCase("SPECIALNETS"))
			{
				if (io_defreadnets(cell, true)) return true;
				continue;
			}
	
			if (key.equalsIgnoreCase("NETS"))
			{
				if (io_defreadnets(cell, false)) return true;
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
	
	private boolean io_defignoreblock(String command)
		throws IOException
	{
		for(;;)
		{
			// get the next keyword
			String key = io_defmustgetkeyword(command);
			if (key == null) return true;
	
			if (key.equalsIgnoreCase("END"))
			{
				io_defgetkeyword();
				break;
			}
		}
		return false;
	}

	private Point2D io_defreadcoordinate()
		throws IOException
	{
		// get "("
		String key = io_defmustgetkeyword("coordinate");
		if (key == null) return null;
		if (!key.equals("("))
		{
			io_definerror("Expected '(' in coordinate");
			return null;
		}
	
		// get X
		key = io_defmustgetkeyword("coordinate");
		if (key == null) return null;
		double x = TextUtils.atof(key) / io_defunits;
//		*x = scalefromdispunit(v, DISPUNITMIC);
	
		// get Y
		key = io_defmustgetkeyword("coordinate");
		if (key == null) return null;
		double y = TextUtils.atof(key) / io_defunits;
//		*y = scalefromdispunit(v, DISPUNITMIC);
	
		// get ")"
		key = io_defmustgetkeyword("coordinate");
		if (key == null) return null;
		if (!key.equals(")"))
		{
			io_definerror("Expected ')' in coordinate");
			return null;
		}
		return new Point2D.Double(x, y);
	}
	
	private Cell io_defgetnodeproto(String name, Library curlib)
	{
		// first see if this cell is in the current library
		Cell cell = curlib.findNodeProto(name);
		if (cell != null) return cell;
	
		// now look in other libraries
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			if (lib.isHidden()) continue;
			if (lib == curlib) continue;
			cell = lib.findNodeProto(name);
//			if (cell != null)
//			{
//				// must copy the cell
//				Cell newCell = us_copyrecursively(cell, cell->protoname, curlib, cell->cellview,
//					FALSE, FALSE, "", FALSE, FALSE, FALSE);
//				return newCell;
//			}
		}
		return null;
	}
	private class GetOrientation
	{
		int angle;
		boolean mX, mY;

		GetOrientation()
			throws IOException
		{
			String key = io_defmustgetkeyword("orientation");
			if (key == null) return;
			boolean transpose = false;
			if (key.equalsIgnoreCase("N")) { angle = 0; } else
			if (key.equalsIgnoreCase("S")) { angle = 1800; } else
			if (key.equalsIgnoreCase("E")) { angle = 2700; } else
			if (key.equalsIgnoreCase("W")) { angle = 900; } else
			if (key.equalsIgnoreCase("FN")) { angle = 900;  transpose = true; } else
			if (key.equalsIgnoreCase("FS")) { angle = 2700; transpose = true; } else
			if (key.equalsIgnoreCase("FE")) { angle = 1800; transpose = true; } else
			if (key.equalsIgnoreCase("FW")) { angle = 0;    transpose = true; } else
			{
				io_definerror("Unknown orientation (" + key + ")");
				return;
			}
			NodeInst.OldStyleTransform ost = new NodeInst.OldStyleTransform(angle, transpose);
			angle = ost.getJAngle();
			mX = ost.isJMirrorX();
			mY = ost.isJMirrorY();
		}
	}
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

	private static class GetLayerInformation
	{
		NodeProto pin;
		NodeProto pure;
		ArcProto arc;

		GetLayerInformation(String name)
		{
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
					// find the layer on this node that is of type "contact"
					PrimitiveNode pNp = (PrimitiveNode)pin;
					Technology.NodeLayer [] nl = pNp.getLayers();
					Layer viaLayer = null;
					for(int i=0; i<nl.length; i++)
					{
						Technology.NodeLayer nLay = nl[i];
						Layer lay = nLay.getLayer();
						Layer.Function fun = lay.getFunction();
						if (fun.isContact()) { viaLayer = lay;   break; }
					}
					if (viaLayer == null) return;
					pure = viaLayer.getPureLayerNode();
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
				int laynum = TextUtils.atoi(name.substring(j));
				ArcProto.Function afunc = ArcProto.Function.getMetal(laynum);
				Layer.Function lfunc = Layer.Function.getMetal(laynum);
				if (afunc == null || lfunc == null) return;
		
				// find the arc with this function
				for(Iterator it = Technology.getCurrent().getArcs(); it.hasNext(); )
				{
					ArcProto ap = (ArcProto)it.next();
					if (ap.getFunction() == afunc)
					{
						arc = ap;
						pin = ((PrimitiveArc)ap).findPinProto();
						break;
					}
				}
		
				// find the pure layer node with this function
				for(Iterator it = Technology.getCurrent().getLayers(); it.hasNext(); )
				{
					Layer lay = (Layer)it.next();
					if (lay.getFunction() == lfunc)
					{
						pure = lay.getPureLayerNode();
						break;
					}
				}
				return;
			}
		}
	}

	/*************** PINS ***************/
	
	private boolean io_defreadpins(Cell cell)
		throws IOException
	{
		if (io_defignoretosemicolon("PINS")) return true;
		for(;;)
		{
			// get the next keyword
			String key = io_defmustgetkeyword("PINs");
			if (key == null) return true;
			if (key.equals("-"))
			{
				if (io_defreadpin(cell)) return true;
				continue;
			}
	
			if (key.equalsIgnoreCase("END"))
			{
				key = io_defgetkeyword();
				break;
			}
	
			// ignore the keyword
			if (io_defignoretosemicolon(key)) return true;
		}
		return false;
	}
	
	private boolean io_defreadpin(Cell cell)
		throws IOException
	{
		// get the pin name
		String key = io_defmustgetkeyword("PIN");
		if (key == null) return true;
		String pinname = key;
		PortCharacteristic portbits = null;
		int havecoord = 0;
		NodeProto np = null;
		Point2D ll = null, ur = null, xy = null;
		boolean haveCoord = false;
		GetOrientation orient = null;

		for(;;)
		{
			// get the next keyword
			key = io_defmustgetkeyword("PIN");
			if (key == null) return true;
			if (key.equals("+"))
			{
				key = io_defmustgetkeyword("PIN");
				if (key == null) return true;
				if (key.equalsIgnoreCase("NET"))
				{
					key = io_defmustgetkeyword("net name");
					if (key == null) return true;
					continue;
				}
				if (key.equalsIgnoreCase("DIRECTION"))
				{
					key = io_defmustgetkeyword("DIRECTION");
					if (key == null) return true;
					if (key.equalsIgnoreCase("INPUT")) portbits = PortCharacteristic.IN; else
					if (key.equalsIgnoreCase("OUTPUT")) portbits = PortCharacteristic.OUT; else
					if (key.equalsIgnoreCase("INOUT")) portbits = PortCharacteristic.BIDIR; else
					if (key.equalsIgnoreCase("FEEDTHRU")) portbits = PortCharacteristic.BIDIR; else
					{
						io_definerror("Unknown direction (" + key + ")");
						return true;
					}
					continue;
				}
				if (key.equalsIgnoreCase("USE"))
				{
					key = io_defmustgetkeyword("USE");
					if (key == null) return true;
					if (key.equalsIgnoreCase("SIGNAL")) ; else
					if (key.equalsIgnoreCase("POWER")) portbits = PortCharacteristic.PWR; else
					if (key.equalsIgnoreCase("GROUND")) portbits = PortCharacteristic.GND; else
					if (key.equalsIgnoreCase("CLOCK")) portbits = PortCharacteristic.CLK; else
					if (key.equalsIgnoreCase("TIEOFF")) ; else
					if (key.equalsIgnoreCase("ANALOG")) ; else
					{
						io_definerror("Unknown usage (" + key + ")");
						return true;
					}
					continue;
				}
				if (key.equalsIgnoreCase("LAYER"))
				{
					key = io_defmustgetkeyword("LAYER");
					if (key == null) return true;
					GetLayerInformation li = new GetLayerInformation(key);
					if (li.pin == null)
					{
						io_definerror("Unknown layer (" + key + ")");
						return true;
					}
					np = li.pin;
					ll = io_defreadcoordinate();
					if (ll == null) return true;
					ur = io_defreadcoordinate();
					if (ur == null) return true;
					continue;
				}
				if (key.equalsIgnoreCase("PLACED"))
				{
					// get pin location and orientation
					xy = io_defreadcoordinate();
					if (xy == null) return true;
					orient = new GetOrientation();
					haveCoord = true;
					continue;
				}
				continue;
			}
	
			if (key.equals(";"))
				break;
		}
	
		// all factors read, now place the pin
		if (np != null && haveCoord)
		{
			// determine the pin size
			AffineTransform trans = NodeInst.pureRotate(orient.angle, orient.mX, orient.mY);
			trans.transform(ll, ll);
			trans.transform(ur, ur);
			double sX = Math.abs(ll.getX() - ur.getX());
			double sY = Math.abs(ll.getY() - ur.getY());
			double cX = (ll.getX() + ur.getX()) / 2 + xy.getX();
			double cY = (ll.getY() + ur.getY()) / 2 + xy.getY();
	
			// make the pin
			NodeInst ni = NodeInst.makeInstance(np, new Point2D.Double(cX, cY), sX, sY, cell);
			if (ni == null)
			{
				io_definerror("Unable to create pin");
				return true;
			}
			PortInst pi = ni.findPortInstFromProto(np.getPort(0));
			Export e = Export.newInstance(cell, pi, pinname);
			if (e == null)
			{
				io_definerror("Unable to create pin name");
				return true;
			}
			e.setCharacteristic(portbits);
		}
		return false;
	}

	/*************** COMPONENTS ***************/
	
	private boolean io_defreadcomponents(Cell cell)
		throws IOException
	{
		if (io_defignoretosemicolon("COMPONENTS")) return true;
		for(;;)
		{
			// get the next keyword
			String key = io_defmustgetkeyword("COMPONENTs");
			if (key == null) return true;
			if (key.equals("-"))
			{
				if (io_defreadcomponent(cell)) return true;
				continue;
			}
	
			if (key.equalsIgnoreCase("END"))
			{
				key = io_defgetkeyword();
				break;
			}
	
			// ignore the keyword
			if (io_defignoretosemicolon(key)) return true;
		}
		return false;
	}
	
	private boolean io_defreadcomponent(Cell cell)
		throws IOException
	{
		// get the component name and model name
		String key = io_defmustgetkeyword("COMPONENT");
		if (key == null) return true;
		String compname = key;
		key = io_defmustgetkeyword("COMPONENT");
		if (key == null) return true;
		String modelname = key;
	
		// find the named cell
		Cell np = io_defgetnodeproto(modelname, cell.getLibrary());
		if (np == null)
		{
			io_definerror("Unknown cell (" + modelname + ")");
			return true;
		}
	
		for(;;)
		{
			// get the next keyword
			key = io_defmustgetkeyword("COMPONENT");
			if (key == null) return true;
			if (key.equals("+"))
			{
				key = io_defmustgetkeyword("COMPONENT");
				if (key == null) return true;
				if (key.equalsIgnoreCase("PLACED") || key.equalsIgnoreCase("FIXED"))
				{
					// handle placement
					Point2D pt = io_defreadcoordinate();
					if (pt == null) return true;
					GetOrientation or = new GetOrientation();
	
					// place the node
					double sX = np.getDefWidth();
					double sY = np.getDefHeight();
					if (or.mX) sX = -sX;
					if (or.mY) sY = -sY;
					NodeInst ni = NodeInst.makeInstance(np, pt, sX, sY, cell, or.angle, compname, 0);
					if (ni == null)
					{
						io_definerror("Unable to create node");
						return true;
					}
					continue;
				}
				continue;
			}
	
			if (key.equals(";")) break;
		}
		return false;
	}
	
	/*************** NETS ***************/
	
	private boolean io_defreadnets(Cell cell, boolean special)
		throws IOException
	{
		for(;;)
		{
			// get the next keyword
			String key = io_defmustgetkeyword("NETs");
			if (key == null) return true;
			if (key.equals("-"))
			{
				if (io_defreadnet(cell, special)) return true;
				continue;
			}
			if (key.equalsIgnoreCase("END"))
			{
				key = io_defgetkeyword();
				break;
			}
	
			// ignore the keyword
			if (io_defignoretosemicolon(key)) return true;
		}
		return false;
	}
	
	private boolean io_defreadnet(Cell cell, boolean special)
		throws IOException
	{
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
//		if (key == null) return true;
//		estrcpy(netname, key);
//	
//		// get the next keyword
//		key = io_defmustgetkeyword(f, x_("NET"));
//		if (key == null) return true;
//	
//		// scan the "net" statement
//		wantpinpairs = 1;
//		lastx = lasty = 0;
//		pathstart = 1;
//		lastlogni = null;
//		for(;;)
//		{
//			// examine the next keyword
//			if (namesame(key, x_(";")) == 0) break;
//	
//			if (namesame(key, x_("+")) == 0)
//			{
//				wantpinpairs = 0;
//				key = io_defmustgetkeyword(f, x_("NET"));
//				if (key == 0) return true;
//	
//				if (namesame(key, x_("USE")) == 0)
//				{
//					// ignore "USE" keyword
//					key = io_defmustgetkeyword(f, x_("NET"));
//					if (key == null) return true;
//				} else if (namesame(key, x_("ROUTED")) == 0)
//				{
//					// handle "ROUTED" keyword
//					key = io_defmustgetkeyword(f, x_("NET"));
//					if (key == null) return true;
//					io_defgetlayernodes(key, &pin, &pure, &routap);
//					if (pin == NONODEPROTO)
//					{
//						io_definerror(_("Unknown layer (%s)"), key);
//						return true;
//					}
//					pathstart = 1;
//					if (special)
//					{
//						// specialnets have width here
//						key = io_defmustgetkeyword(f, x_("NET"));
//						if (key == null) return true;
//						v = (float)eatof(key) / (float)io_defunits;
//						specialwidth = scalefromdispunit(v, DISPUNITMIC);
//					}
//				} else if (namesame(key, x_("FIXED")) == 0)
//				{
//					// handle "FIXED" keyword
//					key = io_defmustgetkeyword(f, x_("NET"));
//					if (key == null) return true;
//					io_defgetlayernodes(key, &pin, &pure, &routap);
//					if (pin == NONODEPROTO)
//					{
//						io_definerror(_("Unknown layer (%s)"), key);
//						return true;
//					}
//					pathstart = 1;
//				} else if (namesame(key, x_("SHAPE")) == 0)
//				{
//					// handle "SHAPE" keyword
//					key = io_defmustgetkeyword(f, x_("NET"));
//					if (key == null) return true;
//				} else
//				{
//					io_definerror(_("Cannot handle '%s' nets"), key);
//					return true;
//				}
//	
//				// get next keyword
//				key = io_defmustgetkeyword(f, x_("NET"));
//				if (key == null) return true;
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
//					return true;
//				}
//	
//				// get the pin names
//				key = io_defmustgetkeyword(f, x_("NET"));
//				if (key == null) return true;
//				if (namesame(key, x_("PIN")) == 0)
//				{
//					// find the export
//					key = io_defmustgetkeyword(f, x_("NET"));
//					if (key == 0) return true;
//					pp = getportproto(cell, key);
//					if (pp == null)
//					{
//						io_definerror(_("Warning: unknown pin '%s'"), key);
//						if (io_defignoretosemicolon(f, _("NETS"))) return true;
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
//					if (ni == null)
//					{
//						io_definerror(_("Unknown component '%s'"), key);
//						return true;
//					}
//	
//					// get the port name
//					key = io_defmustgetkeyword(f, x_("NET"));
//					if (key == null) return true;
//					pp = getportproto(ni->proto, key);
//					if (pp == null)
//					{
//						io_definerror(_("Unknown port '%s' on component '%s'"),
//							key, (CHAR *)var->addr);
//						return true;
//					}
//				}
//	
//				// get the close parentheses
//				key = io_defmustgetkeyword(f, x_("NET"));
//				if (key == null) return true;
//				if (namesame(key, x_(")")) != 0)
//				{
//					io_definerror(_("Expected ')' of pin pair"));
//					return true;
//				}
//	
//				if (lastlogni != NONODEINST && !IOTool.isDEFLogicalPlacement())
//				{
//					portposition(ni, pp, &fx, &fy);
//	
//					// LINTED "lastlogpp" used in proper order
//					portposition(lastlogni, lastlogpp, &tx, &ty);
//					bits = us_makearcuserbits(gen_unroutedarc);
//					ai = newarcinst(gen_unroutedarc, defaultarcwidth(gen_unroutedarc), bits,
//						ni, pp, fx, fy, lastlogni, lastlogpp, tx, ty, cell);
//					if (ai == null)
//					{
//						io_definerror(_("Could not create unrouted arc"));
//						return true;
//					}
//				}
//				lastlogni = ni;
//				lastlogpp = pp;
//	
//				// get the next keyword and continue parsing
//				key = io_defmustgetkeyword(f, x_("NET"));
//				if (key == null) return true;
//				continue;
//			}
//	
//			// handle "new" start of coordinate trace
//			if (namesame(key, x_("NEW")) == 0)
//			{
//				key = io_defmustgetkeyword(f, x_("NET"));
//				if (key == null) return true;
//				io_defgetlayernodes(key, &pin, &pure, &routap);
//				if (pin == NONODEPROTO)
//				{
//					io_definerror(_("Unknown layer (%s)"), key);
//					return true;
//				}
//				pathstart = 1;
//				key = io_defmustgetkeyword(f, x_("NET"));
//				if (key == null) return true;
//				if (special)
//				{
//					// specialnets have width here
//					v = (float)eatof(key) / (float)io_defunits;
//					specialwidth = scalefromdispunit(v, DISPUNITMIC);
//	
//					// get the next keyword
//					key = io_defmustgetkeyword(f, x_("NET"));
//					if (key == null) return true;
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
//				if (key == null) return true;
//				if (estrcmp(key, x_("*")) == 0) curx = lastx; else
//				{
//					v = (float)eatof(key) / (float)io_defunits;
//					curx = scalefromdispunit(v, DISPUNITMIC);
//				}
//	
//				// get the Y coordinate
//				key = io_defmustgetkeyword(f, x_("NET"));
//				if (key == null) return true;
//				if (estrcmp(key, x_("*")) == 0) cury = lasty; else
//				{
//					v = (float)eatof(key) / (float)io_defunits;
//					cury = scalefromdispunit(v, DISPUNITMIC);
//				}
//	
//				// get the close parentheses
//				key = io_defmustgetkeyword(f, x_("NET"));
//				if (key == null) return true;
//				if (namesame(key, x_(")")) != 0)
//				{
//					io_definerror(_("Expected ')' of coordinate pair"));
//					return true;
//				}
//			}
//	
//			// get the next keyword
//			key = io_defmustgetkeyword(f, x_("NET"));
//			if (key == null) return true;
//	
//			// see if it is a via name
//			for(vd = io_deffirstviadef; vd != NOVIADEF; vd = vd->nextviadef)
//				if (namesame(key, vd->vianame) == 0) break;
//			if (vd == null)
//			{
//				// see if the via name is from the LEF file
//				for(vd = io_leffirstviadef; vd != NOVIADEF; vd = vd->nextviadef)
//					if (namesame(key, vd->vianame) == 0) break;
//			}
//	
//			// stop now if not placing physical nets
//			if (IOTool.isDEFPhysicalPlacement())
//			{
//				// ignore the next keyword if a via name is coming
//				if (vd != null)
//				{
//					key = io_defmustgetkeyword(f, x_("NET"));
//					if (key == null) return true;
//				}
//				continue;
//			}
//	
//			// if a via is mentioned next, use it
//			if (vd != null)
//			{
//				// place the via at this location
//				sx = vd->sx;
//				sy = vd->sy;
//				lx = curx - sx / 2;   hx = lx + sx;
//				ly = cury - sy / 2;   hy = ly + sy;
//				if (vd->via == null)
//				{
//					io_definerror(_("Cannot to create via"));
//					return true;
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
//				if (ni == null)
//				{
//					io_definerror(_("Unable to create via layer"));
//					return true;
//				}
//				pp = ni->proto->firstportproto;
//	
//				// if the path starts with a via, wire it
//				if (pathstart != 0 && lastni != null && foundcoord)
//				{
//					if (special) width = specialwidth; else
//					{
//						var = getval((INTBIG)routap, VARCPROTO, VINTEGER, x_("IO_lef_width"));
//						if (var == null) width = defaultarcwidth(routap); else
//							width = var->addr;
//					}
//					ai = newarcinst(routap, width, us_makearcuserbits(routap),
//						lastni, lastpp, curx, cury, ni, pp, curx, cury, cell);
//					if (ai == null)
//					{
//						io_definerror(_("Unable to create net starting point"));
//						return true;
//					}
//					endobjectchange((INTBIG)ai, VARCINST);
//				}
//	
//				// remember that a via was placed
//				placedvia = 1;
//	
//				// get the next keyword
//				key = io_defmustgetkeyword(f, x_("NET"));
//				if (key == null) return true;
//			} else
//			{
//				// no via mentioned: just make a pin
//				if (io_defgetpin(curx, cury, routap, cell, &ni, &pp)) return true;
//				placedvia = 0;
//			}
//			if (!foundcoord) continue;
//	
//			// run the wire
//			if (pathstart == 0)
//			{
//				// make sure that this arc can connect to the current pin
//				for(i=0; pp->connects[i] != null; i++)
//					if (pp->connects[i] == routap) break;
//				if (pp->connects[i] == null)
//				{
//					np = getpinproto(routap);
//					defaultnodesize(np, &sx, &sy);
//					lx = curx - sx / 2;   hx = lx + sx;
//					ly = cury - sy / 2;   hy = ly + sy;
//					ni = newnodeinst(np, lx, hx, ly, hy, 0, 0, cell);
//					if (ni == null)
//					{
//						io_definerror(_("Unable to create net pin"));
//						return true;
//					}
//					pp = ni->proto->firstportproto;
//				}
//	
//				// run the wire
//				if (special) width = specialwidth; else
//				{
//					var = getval((INTBIG)routap, VARCPROTO, VINTEGER, x_("IO_lef_width"));
//					if (var == null) width = defaultarcwidth(routap); else
//						width = var->addr;
//				}
//				ai = newarcinst(routap, width, us_makearcuserbits(routap),
//					lastni, lastpp, lastx, lasty, ni, pp, curx, cury, cell);
//				if (ai == null)
//				{
//					io_definerror(_("Unable to create net path"));
//					return true;
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
//					nextni = null;
//	
//				// if the path starts with a via, wire it
//				if (nextni != null)
//				{
//					if (special) width = specialwidth; else
//					{
//						var = getval((INTBIG)routap, VARCPROTO, VINTEGER, x_("IO_lef_width"));
//						if (var == null) width = defaultarcwidth(routap); else
//							width = var->addr;
//					}
//					ai = newarcinst(routap, width, us_makearcuserbits(routap),
//						ni, pp, curx, cury, nextni, nextpp, curx, cury, cell);
//					if (ai == null)
//					{
//						io_definerror(_("Unable to create net ending point"));
//						return true;
//					}
//				}
//			}
//		}
		return false;
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
					GetLayerInformation li = new GetLayerInformation(key);
					if (li.pure == null)
					{
						io_definerror("Layer " + key + " not in current technology");
					}
					if (key.startsWith("VIA"))
					{
						if (li.pin == null) li.pin = Generic.tech.universalPinNode;
						vd.via = li.pin;
					}
					if (key.startsWith("METAL"))
					{
						if (li.arc == null) li.arc = Generic.tech.universal_arc;
						if (vd.lay1 == null) vd.lay1 = li.arc; else
							vd.lay2 = li.arc;
					}
					Point2D ll = io_defreadcoordinate();
					if (ll == null) return true;
					Point2D ur = io_defreadcoordinate();
					if (ur == null) return true;
	
					// accumulate largest contact size
					if (ur.getX()-ll.getX() > vd.sx) vd.sx = ur.getX() - ll.getX();
					if (ur.getY()-ll.getY() > vd.sy) vd.sy = ur.getY() - ll.getY();
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
	
	/*************** PROPERTY DEFINITIONS ***************/
	
	private boolean io_defreadpropertydefinitions()
		throws IOException
	{
		for(;;)
		{
			// get the next keyword
			String key = io_defmustgetkeyword("PROPERTYDEFINITION");
			if (key == null) return true;
			if (key.equalsIgnoreCase("END"))
			{
				key = io_defgetkeyword();
				break;
			}
	
			// ignore the keyword
			if (io_defignoretosemicolon(key)) return true;
		}
		return false;
	}

	/*************** UNITS ***************/
	
	private boolean io_defreadunits()
		throws IOException
	{	
		// get the "DISTANCE" keyword
		String key = io_defmustgetkeyword("UNITS");
		if (key == null) return true;
		if (!key.equalsIgnoreCase("DISTANCE"))
		{
			io_definerror("Expected 'DISTANCE' after 'UNITS'");
			return true;
		}
	
		// get the "MICRONS" keyword
		key = io_defmustgetkeyword("UNITS");
		if (key == null) return true;
		if (!key.equalsIgnoreCase("MICRONS"))
		{
			io_definerror("Expected 'MICRONS' after 'UNITS'");
			return true;
		}
	
		// get the amount
		key = io_defmustgetkeyword("UNITS");
		if (key == null) return true;
		io_defunits = TextUtils.atof(key);
	
		// ignore the keyword
		if (io_defignoretosemicolon("UNITS")) return true;
		return false;
	}

}
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
//					return true;
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
//			return true;
//		}
//		endobjectchange((INTBIG)ni, VNODEINST);
//		*theni = ni;
//		*thepp = ni->proto->firstportproto;
//		return false;
//	}
