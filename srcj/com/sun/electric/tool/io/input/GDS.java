/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GDS.java
 * Input/output tool: GDS input
 * Original C code written by Glen Lawson, S-MOS Systems, Inc.
 * Translated into Java by Steven M. Rubin, Sun Microsystems.
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
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
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
 * This class reads files in GDS files.
 * <BR>
 * Notes:
 *	Unsupported features and assumptions:
 *	Map layers with the IO_gds_layer_numbers variable on the current technology.
 *	The pure-layer node must have the HOLDSTRACE option, if not will CRASH
 *		for boundary types.  Will map to the generic:DRC-node if not found.
 *	Case sensitive.
 *	SUBLAYERS or XXXTYPE fields are not supported, perhaps a variable
 *		could be attached to the object with this value.
 *	NODEs and TEXTNODEs - don't have an example
 *	PROPERTIES - could add as a variable to the structure.
 *	PATHTYPE 1 - rounded ends on paths, not supported
 *	Path dogears - no cleanup yet, simpler to map paths into arcs
 *	Absolute angle - ???
 *	REFLIBS - not supported.
 *	I have no BOX type examples.
 *
 * Nice to have:
 *	PATH-ARC mapping - should be done, problem is that any layer can
 *		be a path, only connection layers in electric are arcs.
 *	Someone could make a GDS mapping technology for this purpose,
 *		defaults could be taken from this technology.
 *	Misc. fields mapped to variables - should be done.
 *	AREFs - could build into a separate NODEPROTO, and instance, thus
 *		preserving the original intent and space.  With a timestamp,
 *		can determine if the structure has been disturbed for output.
 *	MAG - no scaling is possible, must create a separate object for each
 *		value, don't scale.  (TEXT does scale)
 */
public class GDS extends Input
{
	// data declarations
	private static final int MAXPOINTS     = 4096;
	private static final int MAXLAYERS     =  256;
	private static final int IDSTRING      =  128;
	private static final int TEXTLINE      =  512;
	private static final int MINFONTWIDTH  =  130;
	private static final int MINFONTHEIGHT =  190;

//	typedef enum
//	{
//		GDS_POLY,
//		GDS_RECTANGLE,
//		GDS_OBLIQUE,
//		GDS_LINE,
//		GDS_CLOSED
//	} shape_type;
//	
//	typedef struct
//	{
//		CHAR a11[7];
//		CHAR a12[7];
//		CHAR a21[7];
//		CHAR a22[7];
//	} orientation_type;
//	
//	typedef struct
//	{
//		INTBIG	px;
//		INTBIG	py;
//	} point_type;
//	
	static class datatype_symbol {}
	private static final datatype_symbol GDS_ERR = new datatype_symbol();
	private static final datatype_symbol GDS_NONE = new datatype_symbol();
	private static final datatype_symbol GDS_FLAGS = new datatype_symbol();
	private static final datatype_symbol GDS_SHORT_WORD = new datatype_symbol();
	private static final datatype_symbol GDS_LONG_WORD = new datatype_symbol();
	private static final datatype_symbol GDS_SHORT_REAL = new datatype_symbol();
	private static final datatype_symbol GDS_LONG_REAL = new datatype_symbol();
	private static final datatype_symbol GDS_ASCII_STRING = new datatype_symbol();

	private final static double twoto32 = io_gdsIpower(2, 32);
	private final static double twotoneg56 = 1.0 / io_gdsIpower (2, 56);

	static class gsymbol
	{
		int value;
		gsymbol(int value) { this.value = value; }
	}
	private static final gsymbol GDS_NULLSYM = new gsymbol(0);
	private static final gsymbol GDS_HEADER = new gsymbol(1);
	private static final gsymbol GDS_BGNLIB = new gsymbol(2);
	private static final gsymbol GDS_LIBNAME = new gsymbol(3);
	private static final gsymbol GDS_UNITS = new gsymbol(4);
	private static final gsymbol GDS_ENDLIB = new gsymbol(5);
	private static final gsymbol GDS_BGNSTR = new gsymbol(6);
	private static final gsymbol GDS_STRNAME = new gsymbol(7);
	private static final gsymbol GDS_ENDSTR = new gsymbol(8);
	private static final gsymbol GDS_BOUNDARY = new gsymbol(9);
	private static final gsymbol GDS_PATH = new gsymbol(10);
	private static final gsymbol GDS_SREF = new gsymbol(11);
	private static final gsymbol GDS_AREF = new gsymbol(12);
	private static final gsymbol GDS_TEXTSYM = new gsymbol(13);
	private static final gsymbol GDS_LAYER = new gsymbol(14);
	private static final gsymbol GDS_DATATYPSYM = new gsymbol(15);
	private static final gsymbol GDS_WIDTH = new gsymbol(16);
	private static final gsymbol GDS_XY = new gsymbol(17);
	private static final gsymbol GDS_ENDEL = new gsymbol(18);
	private static final gsymbol GDS_SNAME = new gsymbol(19);
	private static final gsymbol GDS_COLROW = new gsymbol(20);
	private static final gsymbol GDS_TEXTNODE = new gsymbol(21);
	private static final gsymbol GDS_NODE = new gsymbol(22);
	private static final gsymbol GDS_TEXTTYPE = new gsymbol(23);
	private static final gsymbol GDS_PRESENTATION = new gsymbol(24);
	private static final gsymbol GDS_SPACING = new gsymbol(25);
	private static final gsymbol GDS_STRING = new gsymbol(26);
	private static final gsymbol GDS_STRANS = new gsymbol(27);
	private static final gsymbol GDS_MAG = new gsymbol(28);
	private static final gsymbol GDS_ANGLE = new gsymbol(29);
	private static final gsymbol GDS_UINTEGER = new gsymbol(30);
	private static final gsymbol GDS_USTRING = new gsymbol(31);
	private static final gsymbol GDS_REFLIBS = new gsymbol(32);
	private static final gsymbol GDS_FONTS = new gsymbol(33);
	private static final gsymbol GDS_PATHTYPE = new gsymbol(34);
	private static final gsymbol GDS_GENERATIONS = new gsymbol(35);
	private static final gsymbol GDS_ATTRTABLE = new gsymbol(36);
	private static final gsymbol GDS_STYPTABLE = new gsymbol(37);
	private static final gsymbol GDS_STRTYPE = new gsymbol(38);
	private static final gsymbol GDS_ELFLAGS = new gsymbol(39);
	private static final gsymbol GDS_ELKEY = new gsymbol(40);
	private static final gsymbol GDS_LINKTYPE = new gsymbol(41);
	private static final gsymbol GDS_LINKKEYS = new gsymbol(42);
	private static final gsymbol GDS_NODETYPE = new gsymbol(43);
	private static final gsymbol GDS_PROPATTR = new gsymbol(44);
	private static final gsymbol GDS_PROPVALUE = new gsymbol(45);
	private static final gsymbol GDS_BOX = new gsymbol(46);
	private static final gsymbol GDS_BOXTYPE = new gsymbol(47);
	private static final gsymbol GDS_PLEX = new gsymbol(48);
	private static final gsymbol GDS_BGNEXTN = new gsymbol(49);
	private static final gsymbol GDS_ENDEXTN = new gsymbol(50);
	private static final gsymbol GDS_TAPENUM = new gsymbol(51);
	private static final gsymbol GDS_TAPECODE = new gsymbol(52);
	private static final gsymbol GDS_STRCLASS = new gsymbol(53);
	private static final gsymbol GDS_NUMTYPES = new gsymbol(54);
	private static final gsymbol GDS_IDENT = new gsymbol(55);
	private static final gsymbol GDS_REALNUM = new gsymbol(56);
	private static final gsymbol GDS_SHORT_NUMBER = new gsymbol(57);
	private static final gsymbol GDS_NUMBER = new gsymbol(58);
	private static final gsymbol GDS_FLAGSYM = new gsymbol(59);
	private static final gsymbol GDS_FORMAT = new gsymbol(60);
	private static final gsymbol GDS_MASK = new gsymbol(61);
	private static final gsymbol GDS_ENDMASKS = new gsymbol(62);
	private static final gsymbol GDS_BADFIELD = new gsymbol(63);

	private static gsymbol [] io_gdsIoption_set = {GDS_ATTRTABLE, GDS_REFLIBS, GDS_FONTS, GDS_GENERATIONS};
	private static gsymbol [] io_gdsIshape_set = {GDS_AREF, GDS_SREF, GDS_BOUNDARY, GDS_PATH, GDS_NODE, GDS_TEXTSYM, GDS_BOX};
	private static gsymbol [] io_gdsIgood_op_set = {GDS_HEADER, GDS_BGNLIB, GDS_LIBNAME, GDS_UNITS, GDS_ENDLIB, GDS_BGNSTR, GDS_STRNAME,
			GDS_ENDSTR, GDS_BOUNDARY, GDS_PATH, GDS_SREF, GDS_AREF, GDS_TEXTSYM, GDS_LAYER, GDS_DATATYPSYM, GDS_WIDTH, GDS_XY, GDS_ENDEL,
			GDS_SNAME, GDS_COLROW, GDS_TEXTNODE, GDS_NODE, GDS_TEXTTYPE, GDS_PRESENTATION, GDS_STRING, GDS_STRANS, GDS_MAG, GDS_ANGLE,
			GDS_REFLIBS, GDS_FONTS, GDS_PATHTYPE, GDS_GENERATIONS, GDS_ATTRTABLE, GDS_NODETYPE, GDS_PROPATTR, GDS_PROPVALUE, GDS_BOX,
			GDS_BOXTYPE, GDS_FORMAT, GDS_MASK, GDS_ENDMASKS};
	private static gsymbol [] io_gdsImask_set = {GDS_DATATYPSYM, GDS_TEXTTYPE, GDS_BOXTYPE, GDS_NODETYPE};
	private static gsymbol [] io_gdsIunsupported_set = {GDS_ELFLAGS, GDS_PLEX};

	// electric specific globals
	private Library io_gdslibrary;
//	static jmp_buf         io_gdsenv;
//	static NODEPROTO      *io_gdsstructure = NONODEPROTO;
//	static NODEPROTO      *io_gdssref = NONODEPROTO;
//	static NODEINST       *io_gdsinstance = NONODEINST;
//	static NODEPROTO      *io_gdslayernode = NONODEPROTO;
//	static NODEPROTO      *io_gdslayernodes[MAXLAYERS];
//	static INTBIG          io_gdslayertext[MAXLAYERS];
//	static INTBIG          io_gdslayershape[MAXLAYERS];
//	static INTBIG          io_gdslayerrect[MAXLAYERS];
//	static INTBIG          io_gdslayerpath[MAXLAYERS];
//	static INTBIG          io_gdslayerpoly[MAXLAYERS];
//	static BOOLEAN         io_gdslayervisible[MAXLAYERS];
//	static BOOLEAN         io_gdslayerused;
//	static INTBIG          io_gdslayerwhich;
	private int          io_gdssrefcount;
//	static INTBIG          io_gdsrecomputed;
//	static INTBIG         *io_gdscurstate;
//	
//	// globals
	private int          io_gdsrecordcount;
	private gsymbol         io_gdstoken;
	private datatype_symbol valuetype;
	private int         io_gdstokenflags;
	private int          io_gdstokenvalue16;
	private int          io_gdstokenvalue32;
	private float           io_gdsrealtokensp;
	private double          io_gdsrealtokendp;
	private String            io_gdstokenstring;
//	static point_type      io_gdsvertex[MAXPOINTS];
//	static point_type      io_gdsboundarydelta[MAXPOINTS];
//	static INTBIG          io_gdscurlayer, io_gdscursublayer;
//	static double          io_gdsscale;
//	static INTBIG         *io_gdsIlayerlist;
//	static INTBIG          io_gdsIlayerlisttotal = 0;
//	
//	// these are not really used
//	static CHAR            io_gdslibraryname[IDSTRING+1];
	
	/**
	 * Method to import a library from disk.
	 * @param lib the library to fill
	 * @return true on error.
	 */
	protected boolean importALibrary(Library lib)
	{
		io_gdslibrary = lib;
	
//		// get current state of I/O tool
//		io_gdscurstate = io_getstatebits();
	
		// set default objects
//		io_gdslayernode = gen_drcprim;
		io_gdsIOnceinit();
		io_gdssrefcount = 0;

		try
		{
			io_gdsIload();
		} catch (IOException e)
		{
		}
	
//		// determine which cells need to be recomputed
//		count = io_gdssrefcount;
//		for (i = 0; i < MAXLAYERS; i++)
//			count += io_gdslayertext[i] + io_gdslayerpoly[i];
//		for(np = io_gdslibrary->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//			np->temp1 = 0;
//		io_gdsrecomputed = 0;
//		for(np = io_gdslibrary->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//			if (np->temp1 == 0)
//				io_gdsIrecompute(np, count);
	
		return false;
	}

	private HashMap gdsLayerNames;

	/**
	 * This routine rewritten by Steven Rubin to be more robust in finding
	 * pure-layer nodes associated with the GDS layer numbers.
	 */
	private void io_gdsIOnceinit()
	{
		// get the array of GDS names
		gdsLayerNames = new HashMap();
		boolean valid = false;
		Technology curTech = Technology.getCurrent();
		for(Iterator it = curTech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			String gdsName = layer.getGDSLayer();
			if (gdsName != null && gdsName.length() > 0)
			{
				List allGDSNumbers = io_gdsIparselayernumbers(gdsName);
				for(Iterator nIt = allGDSNumbers.iterator(); nIt.hasNext(); )
				{
					Integer lay = (Integer)nIt.next();
					gdsLayerNames.put(lay, layer);
				}
				valid = true;
			}
		}
		if (!valid)
		{
			System.out.println("There are no GDS layer names assigned in the " + curTech.getTechName() + " technology");
		}
	}
	
	/**
	 * Method to parse a string of layer numbers in "layernumbers" and return those layers
	 * in the array "layers", with the size of the array in "total".
	 */
	private List io_gdsIparselayernumbers(String layerNumbers)
	{
		String [] numberStrings = layerNumbers.split(",");
		List numbers = new ArrayList();
		for(int i=0; i<numberStrings.length; i++)
		{
			String numberString = numberStrings[i].trim();
			if (TextUtils.isANumber(numberString))
				numbers.add(new Integer(TextUtils.atoi(numberString)));
		}
		return numbers;
	}
	
	private void io_gdsIload()
		throws IOException
	{
		io_gdsIinit();
		io_gdsIgettoken();
		io_gdsIheader();
		io_gdsIgettoken();
//		io_gdsIlibrary();
		io_gdsIgettoken();
//		while (io_gdsImember(io_gdstoken, io_gdsIoption_set))
//			switch (io_gdstoken)
//		{
//			case GDS_REFLIBS:
//				io_gdsIreflibs();     break;
//			case GDS_FONTS:
//				io_gdsIfonts();       break;
//			case GDS_ATTRTABLE:
//				io_gdsIattrtable();   break;
//			case GDS_GENERATIONS:
//				io_gdsIbackup();      break;
//			default:                  break;
//		}
//		while (io_gdstoken != GDS_UNITS)
//			io_gdsIgettoken();
//		io_gdsIunits();
//		io_gdsIgettoken();
//		while (io_gdstoken != GDS_ENDLIB)
//		{
//			io_gdsIstructure();
//			io_gdsIgettoken();
//		}
	}
	
	private void io_gdsIheader()
		throws IOException
	{
		if (io_gdstoken == GDS_HEADER)
		{
			io_gdsIgettoken();
			if (io_gdstoken == GDS_SHORT_NUMBER)
			{
				// version "io_gdstokenvalue16"
			} else
			{
				io_gdsIerror(2);
			}
		} else
		{
			io_gdsIerror(3);
		}
	}
	
	private void io_gdsIbackup()
		throws IOException
	{
		io_gdsIgettoken();
		if (io_gdstoken == GDS_SHORT_NUMBER)
		{
		} else
		{
			io_gdsIerror(8);
		}
		io_gdsIgettoken();
	}
	
	private boolean io_gdsImember(gsymbol tok, gsymbol [] set)
	{
		for(int i=0; i<set.length; i++)
			if (set[i] == tok) return true;
		return false;
	}

	private void io_gdsIgettoken()
		throws IOException
	{
		if (io_gdsrecordcount == 0)
		{
			valuetype = io_gdsIgetrecord();
		} else
		{
			if (valuetype == GDS_FLAGS)
			{
				io_gdstokenflags = io_gdsIgetword();
				io_gdstoken = GDS_FLAGSYM;
				return;
			}
			if (valuetype == GDS_SHORT_WORD)
			{
				io_gdstokenvalue16 = io_gdsIgetword();
				io_gdstoken = GDS_SHORT_NUMBER;
				return;
			}
			if (valuetype == GDS_LONG_WORD)
			{
				io_gdstokenvalue32 = io_gdsIgetinteger();
				io_gdstoken = GDS_NUMBER;
				return;
			}
			if (valuetype == GDS_SHORT_REAL)
			{
				io_gdsrealtokensp = io_gdsIgetfloat();
				io_gdstoken = GDS_REALNUM;
				return;
			}
			if (valuetype == GDS_LONG_REAL)
			{
				io_gdsrealtokendp = io_gdsIgetdouble();
				io_gdstoken = GDS_REALNUM;
				return;
			}
			if (valuetype == GDS_ASCII_STRING)
			{
				io_gdstokenstring = io_gdsIgetstring();
				io_gdstoken = GDS_IDENT;
				return;
			}
			if (valuetype == GDS_ERR)
			{
				io_gdsIerror(1);
				return;
			}
		}
	}

	private datatype_symbol io_gdsIgetrecord()
		throws IOException
	{
		int dataword = io_gdsIgetword();
		io_gdsrecordcount = dataword - 2;
		int recordtype = io_gdsIgetrawbyte() & 0xFF;
		io_gdstoken = GDS_BADFIELD;
		switch (recordtype)
		{
			case  0: io_gdstoken = GDS_HEADER;        break;
			case  1: io_gdstoken = GDS_BGNLIB;        break;
			case  2: io_gdstoken = GDS_LIBNAME;       break;
			case  3: io_gdstoken = GDS_UNITS;         break;
			case  4: io_gdstoken = GDS_ENDLIB;        break;
			case  5: io_gdstoken = GDS_BGNSTR;        break;
			case  6: io_gdstoken = GDS_STRNAME;       break;
			case  7: io_gdstoken = GDS_ENDSTR;        break;
			case  8: io_gdstoken = GDS_BOUNDARY;      break;
			case  9: io_gdstoken = GDS_PATH;          break;
			case 10: io_gdstoken = GDS_SREF;          break;
			case 11: io_gdstoken = GDS_AREF;          break;
			case 12: io_gdstoken = GDS_TEXTSYM;       break;
			case 13: io_gdstoken = GDS_LAYER;         break;
			case 14: io_gdstoken = GDS_DATATYPSYM;    break;
			case 15: io_gdstoken = GDS_WIDTH;         break;
			case 16: io_gdstoken = GDS_XY;            break;
			case 17: io_gdstoken = GDS_ENDEL;         break;
			case 18: io_gdstoken = GDS_SNAME;         break;
			case 19: io_gdstoken = GDS_COLROW;        break;
			case 20: io_gdstoken = GDS_TEXTNODE;      break;
			case 21: io_gdstoken = GDS_NODE;          break;
			case 22: io_gdstoken = GDS_TEXTTYPE;      break;
			case 23: io_gdstoken = GDS_PRESENTATION;  break;
			case 24: io_gdstoken = GDS_SPACING;       break;
			case 25: io_gdstoken = GDS_STRING;        break;
			case 26: io_gdstoken = GDS_STRANS;        break;
			case 27: io_gdstoken = GDS_MAG;           break;
			case 28: io_gdstoken = GDS_ANGLE;         break;
			case 29: io_gdstoken = GDS_UINTEGER;      break;
			case 30: io_gdstoken = GDS_USTRING;       break;
			case 31: io_gdstoken = GDS_REFLIBS;       break;
			case 32: io_gdstoken = GDS_FONTS;         break;
			case 33: io_gdstoken = GDS_PATHTYPE;      break;
			case 34: io_gdstoken = GDS_GENERATIONS;   break;
			case 35: io_gdstoken = GDS_ATTRTABLE;     break;
			case 36: io_gdstoken = GDS_STYPTABLE;     break;
			case 37: io_gdstoken = GDS_STRTYPE;       break;
			case 38: io_gdstoken = GDS_ELFLAGS;       break;
			case 39: io_gdstoken = GDS_ELKEY;         break;
			case 40: io_gdstoken = GDS_LINKTYPE;      break;
			case 41: io_gdstoken = GDS_LINKKEYS;      break;
			case 42: io_gdstoken = GDS_NODETYPE;      break;
			case 43: io_gdstoken = GDS_PROPATTR;      break;
			case 44: io_gdstoken = GDS_PROPVALUE;     break;
			case 45: io_gdstoken = GDS_BOX;           break;
			case 46: io_gdstoken = GDS_BOXTYPE;       break;
			case 47: io_gdstoken = GDS_PLEX;          break;
			case 48: io_gdstoken = GDS_BGNEXTN;       break;
			case 49: io_gdstoken = GDS_ENDEXTN;       break;
			case 50: io_gdstoken = GDS_TAPENUM;       break;
			case 51: io_gdstoken = GDS_TAPECODE;      break;
			case 52: io_gdstoken = GDS_STRCLASS;      break;
			case 53: io_gdstoken = GDS_NUMTYPES;      break;
			case 54: io_gdstoken = GDS_FORMAT;        break;
			case 55: io_gdstoken = GDS_MASK;          break;
			case 56: io_gdstoken = GDS_ENDMASKS;      break;
		}

		datatype_symbol datatypeSymbol = GDS_ERR;
		switch (io_gdsIgetrawbyte())
		{
			case 0:  datatypeSymbol = GDS_NONE;
			case 1:  datatypeSymbol = GDS_FLAGS;
			case 2:  datatypeSymbol = GDS_SHORT_WORD;
			case 3:  datatypeSymbol = GDS_LONG_WORD;
			case 4:  datatypeSymbol = GDS_SHORT_REAL;
			case 5:  datatypeSymbol = GDS_LONG_REAL;
			case 6:  datatypeSymbol = GDS_ASCII_STRING;
		}
		return datatypeSymbol;
	}

	/**
	 * Method to initialize the input of a GDS file
	 */
	private void io_gdsIinit()
	{
//		io_gdsrecordcount = 0;
//		io_gdscurlayer = io_gdscursublayer = 0;
	}
	
//	static CHAR *io_gdsIerror_string[] =
//	{
//		N_("success"),											/* 0 */
//		N_("Invalid GDS II datatype"),							/* 1 */
//		N_("GDS II version number is not decipherable"),		/* 2 */
//		N_("GDS II header statement is missing"),				/* 3 */
//		N_("Library name is missing"),							/* 4 */
//		N_("Begin library statement is missing"),				/* 5 */
//		N_("Date value is not a valid number"),					/* 6 */
//		N_("Libname statement is missing"),						/* 7 */
//		N_("Generations value is invalid"),						/* 8 */
//		N_("Units statement has invalid number format"),		/* 9 */
//		N_("Units statement is missing"),						/* 10 */
//		N_("Warning this structure is empty"),					/* 11 */
//		N_("Structure name is missing"),						/* 12 */
//		N_("Strname statement is missing"),						/* 13 */
//		N_("Begin structure statement is missing"),				/* 14 */
//		N_("Element end statement is missing"),					/* 15 */
//		N_("Structure reference name is missing"),				/* 16 */
//		N_("Structure reference has no translation value"),		/* 17 */
//		N_("Boundary has no points"),							/* 18 */
//		N_("Structure reference is missing its flags field"),	/* 19 */
//		N_("Not enough points in the shape"),					/* 20 */
//		N_("Too many points in the shape"),						/* 21 */
//		N_("Invalid layer number"),								/* 22 */
//		N_("Layer statement is missing"),						/* 23 */
//		N_("No datatype field"),								/* 24 */
//		N_("Path element has no points"),						/* 25 */
//		N_("Text element has no reference point"),				/* 26 */
//		N_("Array reference has no parameters"),				/* 27 */
//		N_("Array reference name is missing"),					/* 28 */
//		N_("Property has no value"),							/* 29 */
//		N_("Array orientation oblique"),						/* 30 */
//		N_("Failed to create AREF"),							/* 31 */
//		N_("Failed to create RECTANGLE"),						/* 32 */
//		N_("Failed to create POLYGON"),							/* 33 */
//		N_("No memory"),										/* 34 */
//		N_("Failed to create POLYGON points"),					/* 35 */
//		N_("Could not create text marker"),						/* 36 */
//		N_("Could not create text string"),						/* 37 */
//		N_("Failed to create structure"),						/* 38 */
//		N_("Failed to create cell center"),						/* 39 */
//		N_("Failed to create NODE"),							/* 40 */
//		N_("Failed to create box"),								/* 41 */
//		N_("Failed to create outline"),							/* 42 */
//		N_("Failed to create SREF proto"),						/* 43 */
//		N_("Failed to create SREF")								/* 44 */
//	};
	private void io_gdsIerror(int n)
	{
//		System.out.println("Error: " + io_gdsIerror_string[n] + " at byte " + byteCount);
//		longjmp(io_gdsenv, 1);
	}
	
	private float io_gdsIgetfloat()
		throws IOException
	{
		int reg = io_gdsIgetrawbyte() & 0xFF;
		int sign = 1;
		if ((reg & 0x00000080) != 0) sign = -1;
		reg = reg & 0x0000007F;
	
		// generate the exponent, currently in Excess-64 representation
		int binary_exponent = (reg - 64) << 2;
		reg = (io_gdsIgetrawbyte() & 0xFF) << 16;
		int dataword = io_gdsIgetword();
		reg = reg + dataword;
		int shift_count = 0;
	
		// normalize the matissa
		while ((reg & 0x00800000) == 0)
		{
			reg = reg << 1;
			shift_count++;
		}
	
		// this is the exponent + normalize shift - precision of matissa
		// binary_exponent = binary_exponent + shift_count - 24;
		binary_exponent = binary_exponent - shift_count - 24;
	
		if (binary_exponent > 0)
		{
			return (float)(sign * reg * io_gdsIpower(2, binary_exponent));
		}
		if (binary_exponent < 0)
			return (float)(sign * reg / io_gdsIpower(2, -binary_exponent));
		return (float)(sign * reg);
	}
	
	private static double io_gdsIpower(int val, int power)
	{
		double result = 1.0;
		for(int count=0; count<power; count++)
			result *= val;
		return result;
	}

	private double io_gdsIgetdouble()
		throws IOException
	{
		// first byte is the exponent field (hex)
		int register1 = io_gdsIgetrawbyte() & 0xFF;
	
		// plus sign bit
		double realValue = 1;
		if ((register1 & 0x00000080) != 0) realValue = -1.0;
	
		// the hex exponent is in excess 64 format
		register1 = register1 & 0x0000007F;
		int exponent = register1 - 64;
	
		// bytes 2-4 are the high ordered bits of the mantissa
		register1 = (io_gdsIgetrawbyte() & 0xFF) << 16;
		int dataword = io_gdsIgetword();
		register1 = register1 + dataword;
	
		// next word completes the matissa (1/16 to 1)
		int long_integer = io_gdsIgetinteger();
		int register2 = long_integer;
	
		// now normalize the value 
		if (register1 != 0 || register2 != 0)
		{
			// while ((register1 & 0x00800000) == 0)
			// check for 0 in the high-order nibble
			while ((register1 & 0x00F00000) == 0)
			{
				// register1 = register1 << 1;
				// shift the 2 registers by 4 bits
				register1 = (register1 << 4) + (register2>>28);
				register2 = register2 << 4;
				exponent--;
			}
		} else
		{
			// true 0
			return 0;
		}
	
		// now create the matissa (fraction between 1/16 to 1)
		realValue *= (register1 * twoto32 + register2) * twotoneg56;
		if (exponent > 0)
		{
			realValue = realValue * io_gdsIpower(16, exponent);
		} else
		{
			if (exponent < 0)
				realValue = realValue / io_gdsIpower(16, -exponent);
		}
		return realValue;
	}
	
	private String io_gdsIgetstring()
		throws IOException
	{
		StringBuffer sb = new StringBuffer();
		while (io_gdsrecordcount != 0)
		{
			char letter = (char)io_gdsIgetrawbyte();
			sb.append(letter);
		}
		return sb.toString();
	}

	private int io_gdsIgetinteger()
		throws IOException
	{
		int highWord = io_gdsIgetword();
		int lowWord = io_gdsIgetword();
		return (highWord << 16) | lowWord;
	}
	
	private int io_gdsIgetword()
		throws IOException
	{
		int highByte = io_gdsIgetrawbyte() & 0xFF;
		int lowByte = io_gdsIgetrawbyte() & 0xFF;
		return (highByte << 8) | lowByte;
	}

	private byte io_gdsIgetrawbyte()
		throws IOException
	{
		byte b = dataInputStream.readByte();
		updateProgressDialog(1);
		return b;
	}
}

//
//	
//	
//	void io_gdsItranslate(INTBIG *ang, INTBIG *trans, double angle, BOOLEAN reflected)
//	{
//		*ang = rounddouble(angle) % 3600;
//		*trans = reflected ? 1 : 0;
//		if (*trans)
//			*ang = (2700 - *ang) % 3600;
//	
//		// should not happen...*/
//		if (*ang < 0) *ang = *ang + 3600;
//	}
//	
//	void io_gdsItransform(point_type delta, INTBIG angle, INTBIG trans)
//	{
//		XARRAY matrix;
//	
//		makeangle(angle, trans, matrix);
//		xform(delta.px, delta.py, &delta.px, &delta.py, matrix);
//	}
//	
//	void io_gdsIstep(INTBIG nc, INTBIG nr, INTBIG angle, INTBIG trans)
//	{
//		point_type rowinterval, colinterval, ptc, pt;
//		INTBIG ic, ir;
//		INTBIG cx, cy, ox, oy;
//		XARRAY transform;
//	
//		if (nc != 1)
//		{
//			colinterval.px = (io_gdsvertex[1].px - io_gdsvertex[0].px) / nc;
//			colinterval.py = (io_gdsvertex[1].py - io_gdsvertex[0].py) / nc;
//			io_gdsItransform(colinterval, angle, trans);
//		}
//		if (nr != 1)
//		{
//			rowinterval.px = (io_gdsvertex[2].px - io_gdsvertex[0].px) / nr;
//			rowinterval.py = (io_gdsvertex[2].py - io_gdsvertex[0].py) / nr;
//			io_gdsItransform(rowinterval, angle, trans);
//		}
//	
//		// create the node
//		makeangle(angle, trans, transform);
//	
//		// now transform the center
//		cx = (io_gdssref->highx + io_gdssref->lowx) / 2;
//		cy = (io_gdssref->highy + io_gdssref->lowy) / 2;
//		xform(cx, cy, &ox, &oy, transform);
//	
//		// calculate the offset from the original center
//		ox -= cx;   oy -= cy;
//	
//		// now generate the array
//		ptc.px = io_gdsvertex[0].px;   ptc.py = io_gdsvertex[0].py;
//		for (ic = 0; ic < nc; ic++)
//		{
//			pt = ptc;
//			for (ir = 0; ir < nr; ir++)
//			{
//				// create the node
//				if ((io_gdscurstate[0]&GDSINARRAYS) != 0 ||
//					(ir == 0 && ic == 0) ||
//						(ir == (nr-1) && ic == (nc-1)))
//				{
//					if ((io_gdsinstance = newnodeinst(io_gdssref, pt.px + io_gdssref->lowx + ox,
//						pt.px + io_gdssref->highx + ox, pt.py + io_gdssref->lowy + oy,
//							pt.py + io_gdssref->highy + oy, trans, angle,
//								io_gdsstructure)) == NONODEINST)
//									io_gdsIerror(31);
//					if ((io_gdscurstate[0] & GDSINEXPAND) != 0)
//						io_gdsinstance->userbits |= NEXPAND;
//					// annotate the array info to this element
//				}
//	
//				// add the row displacement
//				pt.px += rowinterval.px;   pt.py += rowinterval.py;
//			}
//	
//			// add displacement
//			ptc.px += colinterval.px;   ptc.py += colinterval.py;
//		}
//	}
//	
//	void io_gdsIdetermine_boundary(INTBIG npts, shape_type *perimeter, shape_type *oclass)
//	{
//		BOOLEAN is90, is45;
//		REGISTER INTBIG i;
//	
//		is90 = TRUE;
//		is45 = TRUE;
//		if (io_gdsvertex[0].px == io_gdsvertex[npts-1].px &&
//			io_gdsvertex[0].py == io_gdsvertex[npts-1].py)
//				(*perimeter) = GDS_CLOSED; else
//					(*perimeter) = GDS_LINE;
//	
//		for (i=0; i<npts-1 && i<MAXPOINTS-1; i++)
//		{
//			io_gdsboundarydelta[i].px = io_gdsvertex[i+1].px - io_gdsvertex[i].px;
//			io_gdsboundarydelta[i].py = io_gdsvertex[i+1].py - io_gdsvertex[i].py;
//		}
//		for (i=0; i<npts-1 && i<MAXPOINTS-1; i++)
//		{
//			if (io_gdsboundarydelta[i].px != 0 && io_gdsboundarydelta[i].py != 0)
//			{
//				is90 = FALSE;
//				if (abs(io_gdsboundarydelta[i].px) != abs(io_gdsboundarydelta[i].py))
//					is45 = FALSE;
//			}
//		}
//		if ((*perimeter) == GDS_CLOSED && (is90 || is45))
//			(*oclass) = GDS_POLY; else
//				(*oclass) = GDS_OBLIQUE;
//		if (npts == 5 && is90 && (*perimeter) == GDS_CLOSED)
//			(*oclass) = GDS_RECTANGLE;
//	}
//	
//	void io_gdsIbox(INTBIG *npts)
//	{
//		REGISTER INTBIG i, pxm, pym, pxs, pys;
//	
//		pxm = io_gdsvertex[4].px;
//		pxs = io_gdsvertex[4].px;
//		pym = io_gdsvertex[4].py;
//		pys = io_gdsvertex[4].py;
//		for (i = 0; i<4; i++)
//		{
//			if (io_gdsvertex[i].px > pxm) pxm = io_gdsvertex[i].px;
//			if (io_gdsvertex[i].px < pxs) pxs = io_gdsvertex[i].px;
//			if (io_gdsvertex[i].py > pym) pym = io_gdsvertex[i].py;
//			if (io_gdsvertex[i].py < pys) pys = io_gdsvertex[i].py;
//		}
//		io_gdsvertex[0].px = pxs;
//		io_gdsvertex[0].py = pys;
//		io_gdsvertex[1].px = pxm;
//		io_gdsvertex[1].py = pym;
//		(*npts) = 2;
//	}
//	
//	void io_gdsIshape(INTBIG npts, shape_type perimeter, shape_type oclass)
//	{
//		INTBIG lx, ly, hx, hy, cx, cy, i;
//		INTBIG *trace, *pt;
//		NODEINST *ni;
//	
//		switch (oclass)
//		{
//			case GDS_RECTANGLE:
//				io_gdsIbox(&npts);
//	
//				// create the rectangle
//				if (io_gdslayerused)
//				{
//					io_gdslayerrect[io_gdslayerwhich]++;
//					io_gdslayerpoly[io_gdslayerwhich]++;
//					if (newnodeinst(io_gdslayernode, io_gdsvertex[0].px,
//						io_gdsvertex[1].px, io_gdsvertex[0].py, io_gdsvertex[1].py, 0, 0,
//							io_gdsstructure) == NONODEINST)
//								io_gdsIerror(32);
//				}
//				break;
//	
//			case GDS_OBLIQUE:
//			case GDS_POLY:
//				if (!io_gdslayerused) break;
//				io_gdslayershape[io_gdslayerwhich]++;
//				io_gdslayerpoly[io_gdslayerwhich]++;
//	
//				// determine the bounds of the polygon
//				lx = hx = io_gdsvertex[0].px;
//				ly = hy = io_gdsvertex[0].py;
//				for (i=1; i<npts;i++)
//				{
//					if (lx > io_gdsvertex[i].px) lx = io_gdsvertex[i].px;
//					if (hx < io_gdsvertex[i].px) hx = io_gdsvertex[i].px;
//					if (ly > io_gdsvertex[i].py) ly = io_gdsvertex[i].py;
//					if (hy < io_gdsvertex[i].py) hy = io_gdsvertex[i].py;
//				}
//	
//				// now create the node
//				if ((ni = newnodeinst(io_gdslayernode, lx, hx, ly, hy, 0, 0,
//					io_gdsstructure)) == NONODEINST)
//						io_gdsIerror(33);
//	
//				// now allocate the trace
//				pt = trace = emalloc((npts*2*SIZEOFINTBIG), el_tempcluster);
//				if (trace == 0) io_gdsIerror(34);
//				cx = (hx + lx) / 2;   cy = (hy + ly) / 2;
//				for (i=0; i<npts; i++)
//				{
//					*pt++ = io_gdsvertex[i].px - cx;
//					*pt++ = io_gdsvertex[i].py - cy;
//				}
//	
//				// store the trace information
//				if (setvalkey((INTBIG)ni, VNODEINST, el_trace_key, (INTBIG)trace,
//					VINTEGER|VISARRAY|((npts*2)<<VLENGTHSH)) == NOVARIABLE)
//						io_gdsIerror(35);
//	
//				// free the polygon memory
//				efree((CHAR *)trace);
//				break;
//			default:
//				break;
//		}
//	}
//	
//	void io_gdsItext(CHAR *charstring, INTBIG vjust, INTBIG hjust, INTBIG angle,
//		INTBIG trans, double scale)
//	{
//		NODEINST *ni;
//		VARIABLE *var;
//		INTBIG size;
//	
//		// no text
//		if (!io_gdslayerused || !(io_gdscurstate[0]&GDSINTEXT)) return;
//		io_gdslayertext[io_gdslayerwhich]++;
//	
//		io_gdsvertex[1].px = io_gdsvertex[0].px + MINFONTWIDTH * estrlen(charstring);
//		io_gdsvertex[1].py = io_gdsvertex[0].py + MINFONTHEIGHT;
//	
//		// create a holding node
//		ni = newnodeinst(io_gdslayernode, io_gdsvertex[0].px, io_gdsvertex[0].px,
//			io_gdsvertex[0].py, io_gdsvertex[0].py, trans, angle, io_gdsstructure);
//		if (ni == NONODEINST) io_gdsIerror(36);
//	
//		// now add the text
//		var = setvalkey((INTBIG)ni, VNODEINST, el_node_name_key, (INTBIG)charstring,
//			VSTRING|VDISPLAY);
//		if (var == NOVARIABLE) io_gdsIerror(37);
//	
//		// set the text size and orientation
//		size = rounddouble(scale);
//		if (size <= 0) size = 8;
//		if (size > TXTMAXPOINTS) size = TXTMAXPOINTS;
//		TDSETSIZE(var->textdescript, size);
//	
//		// determine the presentation
//		TDSETPOS(var->textdescript, VTPOSCENT);
//		switch (vjust)
//		{
//			case 1:		// top
//				switch (hjust)
//				{
//					case 1:  TDSETPOS(var->textdescript, VTPOSUPRIGHT); break;
//					case 2:  TDSETPOS(var->textdescript, VTPOSUPLEFT);  break;
//					default: TDSETPOS(var->textdescript, VTPOSUP);      break;
//				}
//				break;
//	
//			case 2:		// bottom
//				switch (hjust)
//				{
//					case 1:  TDSETPOS(var->textdescript, VTPOSDOWNRIGHT); break;
//					case 2:  TDSETPOS(var->textdescript, VTPOSDOWNLEFT);  break;
//					default: TDSETPOS(var->textdescript, VTPOSDOWN);      break;
//				}
//				break;
//	
//			default:	// centered
//				switch (hjust)
//				{
//					case 1:  TDSETPOS(var->textdescript, VTPOSRIGHT); break;
//					case 2:  TDSETPOS(var->textdescript, VTPOSLEFT);  break;
//					default: TDSETPOS(var->textdescript, VTPOSCENT);  break;
//				}
//		}
//	}
//	
//	void io_gdsIdetermine_time(CHAR *time_string)
//	{
//		REGISTER INTBIG i;
//		CHAR time_array[7][IDSTRING+1];
//	
//		for (i = 0; i < 6; i++)
//		{
//			if (io_gdstoken == GDS_SHORT_NUMBER)
//			{
//				if (i == 0 && io_gdstokenvalue16 < 1900)
//				{
//					// handle Y2K date issues
//					if (io_gdstokenvalue16 > 60) io_gdstokenvalue16 += 1900; else
//						io_gdstokenvalue16 += 2000;
//				}
//				(void)esnprintf(time_array[i], IDSTRING+1, x_("%02d"), io_gdstokenvalue16);
//				io_gdsIgettoken();
//			} else
//			{
//				io_gdsIerror(6);
//			}
//		}
//		(void)esnprintf(time_string, IDSTRING+1, x_("%s-%s-%s at %s:%s:%s"), time_array[1], time_array[2],
//			time_array[0], time_array[3], time_array[4], time_array[5]);
//	}
//	
//	void io_gdsIlibrary(void)
//	{
//		CHAR mod_time[IDSTRING+1], create_time[IDSTRING+1];
//	
//		if (io_gdstoken == GDS_BGNLIB)
//		{
//			io_gdsIgettoken();
//			io_gdsIdetermine_time(create_time);
//			io_gdsIdetermine_time(mod_time);
//			if (io_gdstoken == GDS_LIBNAME)
//			{
//				io_gdsIgettoken();
//				if (io_gdstoken == GDS_IDENT)
//				{
//					(void)estrcpy(io_gdslibraryname, io_gdstokenstring);
//				} else
//				{
//					io_gdsIerror(4);
//				}
//			}
//		} else
//		{
//			io_gdsIerror(5);
//		}
//	}
//	
//	void io_gdsIreflibs(void)
//	{
//		io_gdsIgettoken();
//		io_gdsIgettoken();
//	}
//	
//	void io_gdsIfonts(void)
//	{
//		io_gdsIgettoken();
//		io_gdsIgettoken();
//	}
//	
//	void io_gdsIattrtable(void)
//	{
//		io_gdsIgettoken();
//		if (io_gdstoken == GDS_IDENT)
//		{
//			io_gdsIgettoken();
//		}
//	}
//	
//	void io_gdsIunits(void)
//	{
//		double meter_unit, db_unit;
//		double microns_per_user_unit;
//		CHAR realstring[IDSTRING+1];
//	
//		if (io_gdstoken == GDS_UNITS)
//		{
//			io_gdsIgettoken();
//			if (io_gdstoken == GDS_REALNUM)
//			{
//				db_unit = io_gdsrealtokendp;
//				// io_gdstechnologyscalefactor = rounddouble(1.0 / io_gdsrealtokendp);
//				io_gdsIgettoken();
//				meter_unit = io_gdsrealtokendp;
//				microns_per_user_unit = (meter_unit / db_unit) * 1.0e6;
//				(void)esnprintf(realstring, IDSTRING+1, x_("%6.3f"), microns_per_user_unit);
//	
//				// don't change the cast in this equation - roundoff error!
//				io_gdsscale = meter_unit  * (double)(1000000 * scalefromdispunit(1.0, DISPUNITMIC));
//			} else
//			{
//				io_gdsIerror(9);
//			}
//		} else
//		{
//			io_gdsIerror(10);
//		}
//	}
//	
//	void io_gdsIunsupported(gsymbol bad_op_set[])
//	{
//		if (io_gdsImember(io_gdstoken, bad_op_set))
//		do
//		{
//			io_gdsIgettoken();
//		} while (!io_gdsImember(io_gdstoken, io_gdsIgood_op_set));
//	}
//	
//	void io_gdsIdetermine_points(INTBIG min_points, INTBIG max_points, INTBIG *point_counter)
//	{
//		(*point_counter) = 0;
//		while (io_gdstoken == GDS_NUMBER)
//		{
//			io_gdsvertex[*point_counter].px = rounddouble((double)io_gdstokenvalue32 * io_gdsscale);
//			io_gdsIgettoken();
//			io_gdsvertex[*point_counter].py = rounddouble((double)io_gdstokenvalue32 * io_gdsscale);
//			(*point_counter)++;
//			if (*point_counter > max_points)
//			{
//				ttyputmsg(_("Found %ld points"), *point_counter);
//				io_gdsIerror(21);
//			}
//			io_gdsIgettoken();
//		}
//		if (*point_counter < min_points)
//		{
//			ttyputmsg(_("Found %ld points"), *point_counter);
//			io_gdsIerror(20);
//		}
//	}
//	
//	void io_gdsIdetermine_orientation(INTBIG *angle, INTBIG *trans, double *scale)
//	{
//		BOOLEAN mirror_x;
//		double anglevalue;
//	
//		anglevalue = 0.0;
//		*scale = 1.0;
//		mirror_x = FALSE;
//		io_gdsIgettoken();
//		if (io_gdstoken == GDS_FLAGSYM)
//		{
//			if ((io_gdstokenflags&0100000) != 0) mirror_x = TRUE;
//			io_gdsIgettoken();
//		} else
//		{
//			io_gdsIerror(19);
//		}
//		if (io_gdstoken == GDS_MAG)
//		{
//			io_gdsIgettoken();
//			*scale = io_gdsrealtokendp;
//			io_gdsIgettoken();
//		}
//		if (io_gdstoken == GDS_ANGLE)
//		{
//			io_gdsIgettoken();
//			anglevalue = io_gdsrealtokendp * 10;
//			io_gdsIgettoken();
//		}
//		io_gdsItranslate(angle, trans, anglevalue, mirror_x);
//	}
//	
//	void io_gdsIdetermine_layer(INTBIG *layer, INTBIG *sublayer)
//	{
//		*layer = io_gdscurlayer;
//		*sublayer = io_gdscursublayer;
//		if (io_gdstoken == GDS_LAYER)
//		{
//			io_gdsIgettoken();
//			if (io_gdstoken == GDS_SHORT_NUMBER)
//			{
//				io_gdscurlayer = *layer = io_gdstokenvalue16;
//				if (io_gdscurlayer >= MAXLAYERS)
//				{
//					ttyputmsg(_("GDS layer %ld is too high (limit is %d)"),
//						io_gdscurlayer, MAXLAYERS-1);
//					io_gdscurlayer = MAXLAYERS-1;
//				}
//				if (io_gdslayernodes[io_gdscurlayer] == NONODEPROTO)
//				{
//					if ((io_gdscurstate[0]&GDSINIGNOREUKN) == 0)
//					{
//						ttyputmsg(_("GDS layer %ld unknown, using Generic:DRC"),
//							io_gdscurlayer);
//					} else
//					{
//						ttyputmsg(_("GDS layer %ld unknown, ignoring it"),
//							io_gdscurlayer);
//					}
//					io_gdslayernodes[io_gdscurlayer] = gen_drcprim;
//				}
//				io_gdslayernode = io_gdslayernodes[io_gdscurlayer];
//				if (io_gdslayernode == gen_drcprim &&
//					(io_gdscurstate[0]&GDSINIGNOREUKN) != 0)
//				{
//					io_gdslayerused = FALSE;
//				} else
//				{
//					io_gdslayerused = io_gdslayervisible[io_gdscurlayer];
//				}
//				io_gdslayerwhich = io_gdscurlayer;
//	
//				io_gdsIgettoken();
//				if (io_gdsImember(io_gdstoken, io_gdsImask_set))
//				{
//					io_gdsIgettoken();
//					if (io_gdstokenvalue16 != 0)
//					{
//						io_gdscursublayer = *sublayer = io_gdstokenvalue16;
//					}
//				} else
//				{
//					io_gdsIerror(24);
//				}
//			} else
//			{
//				io_gdsIerror(22);
//			}
//		} else
//		{
//			io_gdsIerror(23);
//		}
//	}
//	
//	NODEPROTO *io_gdsIfindproto(CHAR *name)
//	{
//		REGISTER NODEPROTO *np;
//	
//		for(np = io_gdslibrary->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//			if (namesame(name, np->protoname) == 0) return(np);
//		return(NONODEPROTO);
//	}
//	
//	void io_gdsIbeginstructure(void)
//	{
//		CHAR create_time[IDSTRING+1], mod_time[IDSTRING+1];
//		REGISTER void *infstr;
//	
//		if (io_gdstoken == GDS_BGNSTR)
//		{
//			io_gdsIgettoken();
//			io_gdsIdetermine_time(create_time);
//			io_gdsIdetermine_time(mod_time);
//			if (io_gdstoken == GDS_STRNAME)
//			{
//				io_gdsIgettoken();
//				if (io_gdstoken == GDS_IDENT)
//				{
//					// look for this nodeproto
//					io_gdsstructure = io_gdsIfindproto(io_gdstokenstring);
//					if (io_gdsstructure == NONODEPROTO)
//					{
//						// create the proto
//						io_gdsstructure = us_newnodeproto(io_gdstokenstring, io_gdslibrary);
//						if (io_gdsstructure == NONODEPROTO)
//							io_gdsIerror(38);
//						ttyputmsg(_("Reading %s"), io_gdstokenstring);
//						if (io_gdslibrary->curnodeproto == NONODEPROTO)
//							io_gdslibrary->curnodeproto = io_gdsstructure;
//					}
//				} else
//				{
//					io_gdsIerror(12);
//				}
//			} else
//			{
//				io_gdsIerror(13);
//			}
//		} else
//		{
//			io_gdsIerror(14);
//		}
//	}
//	
//	void io_gdsIdetermine_property(void)
//	{
//		CHAR propvalue[IDSTRING+1];
//	
//		io_gdsIgettoken();
//		io_gdsIgettoken();
//		if (io_gdstoken == GDS_PROPVALUE)
//		{
//			io_gdsIgettoken();
//			(void)estrcpy(propvalue, io_gdstokenstring);
//	
//			// add to the current structure as a variable?
//			io_gdsIgettoken();
//		} else
//		{
//			io_gdsIerror(29);
//		}
//		// close Attribute
//	}
//	
//	// need more info ... */
//	void io_gdsIdetermine_node(void)
//	{
//		INTBIG n;
//	
//		io_gdsIgettoken();
//		io_gdsIunsupported(io_gdsIunsupported_set);
//		if (io_gdstoken == GDS_LAYER)
//		{
//			io_gdsIgettoken();
//			if (io_gdstoken == GDS_SHORT_NUMBER)
//			{
//				io_gdscurlayer = io_gdstokenvalue16;
//				if (io_gdscurlayer >= MAXLAYERS)
//				{
//					ttyputmsg(_("GDS layer %ld is too high (limit is %d)"),
//						io_gdscurlayer, MAXLAYERS-1);
//					io_gdscurlayer = MAXLAYERS-1;
//				}
//				if (io_gdslayernodes[io_gdscurlayer] == NONODEPROTO)
//				{
//					ttyputmsg(_("GDS layer %ld unknown, using Generic:DRC"),
//						io_gdscurlayer);
//					io_gdslayernodes[io_gdscurlayer] = gen_drcprim;
//				}
//				io_gdslayernode = io_gdslayernodes[io_gdscurlayer];
//	
//				io_gdsIgettoken();
//			}
//		} else
//		{
//			io_gdsIerror(18);
//		}
//	
//		// should do something with node type???
//		if (io_gdstoken == GDS_NODETYPE)
//		{
//			io_gdsIgettoken();
//			io_gdsIgettoken();
//		}
//	
//		// make a dot
//		if (io_gdstoken == GDS_XY)
//		{
//			io_gdsIgettoken();
//			io_gdsIdetermine_points(1, 1, &n);
//	
//			// create the node
//			if (newnodeinst(io_gdslayernode, io_gdsvertex[0].px,
//				io_gdsvertex[0].px, io_gdsvertex[0].py, io_gdsvertex[0].py, 0, 0,
//					io_gdsstructure) == NONODEINST)
//						io_gdsIerror(40);
//		} else
//		{
//			io_gdsIerror(18);
//		}
//	}
//	
//	/* untested feature, I don't have a box type */
//	void io_gdsIdetermine_box(void)
//	{
//		INTBIG n, layer, sublayer;
//	
//		io_gdsIgettoken();
//		io_gdsIunsupported(io_gdsIunsupported_set);
//		io_gdsIdetermine_layer(&layer, &sublayer);
//		if (io_gdstoken == GDS_XY)
//		{
//			io_gdsIgettoken();
//			io_gdsIdetermine_points(2, MAXPOINTS, &n);
//			if (io_gdslayerused)
//			{
//				// create the box
//				io_gdslayerrect[io_gdslayerwhich]++;
//			    io_gdslayerpoly[io_gdslayerwhich]++;
//				if (newnodeinst(io_gdslayernode, io_gdsvertex[0].px, io_gdsvertex[1].px,
//					io_gdsvertex[0].py, io_gdsvertex[1].py , 0, 0,
//						io_gdsstructure) == NONODEINST)
//							io_gdsIerror(41);
//			}
//		} else
//		{
//			io_gdsIerror(18);
//		}
//	}
//	
//	void io_gdsIdetermine_shape(void)
//	{
//		INTBIG n, layer, sublayer;
//		shape_type perimeter, oclass;
//	
//		io_gdsIgettoken();
//		io_gdsIunsupported(io_gdsIunsupported_set);
//		io_gdsIdetermine_layer(&layer, &sublayer);
//		io_gdsIgettoken();
//		if (io_gdstoken == GDS_XY)
//		{
//			io_gdsIgettoken();
//			io_gdsIdetermine_points(3, MAXPOINTS, &n);
//			io_gdsIdetermine_boundary(n, &perimeter, &oclass);
//			io_gdsIshape(n, perimeter, oclass);
//		} else
//		{
//			io_gdsIerror(18);
//		}
//	}
//	
//	void io_gdsIdetermine_path(void)
//	{
//		INTBIG endcode, n, layer, sublayer, off[8], thisAngle, lastAngle, nextAngle,
//			bgnextend, endextend, fextend, textend, ang;
//		INTBIG width, length, lx, ly, hx, hy, i, j, fx, fy, tx, ty, cx, cy;
//		REGISTER NODEINST *ni;
//		static POLYGON *poly = NOPOLYGON;
//	
//		endcode = 0;
//		io_gdsIgettoken();
//		io_gdsIunsupported(io_gdsIunsupported_set);
//		io_gdsIdetermine_layer(&layer, &sublayer);
//		io_gdsIgettoken();
//		if (io_gdstoken == GDS_PATHTYPE)
//		{
//			io_gdsIgettoken();
//			endcode = io_gdstokenvalue16;
//			io_gdsIgettoken();
//		}
//		if (io_gdstoken == GDS_WIDTH)
//		{
//			io_gdsIgettoken();
//			width = rounddouble((double)io_gdstokenvalue32 * io_gdsscale);
//			io_gdsIgettoken();
//		} else
//		{
//			width = 0;
//		}
//		bgnextend = endextend = (endcode == 0 || endcode == 4 ? 0 : width/2);
//		if (io_gdstoken == GDS_BGNEXTN)
//		{
//			io_gdsIgettoken();
//			if (endcode == 4)
//				bgnextend = rounddouble((double)io_gdstokenvalue32 * io_gdsscale);
//			io_gdsIgettoken();
//		}
//		if (io_gdstoken == GDS_ENDEXTN)
//		{
//			io_gdsIgettoken();
//			if (endcode == 4)
//				endextend = rounddouble((double)io_gdstokenvalue32 * io_gdsscale);
//			io_gdsIgettoken();
//		}
//		if (io_gdstoken == GDS_XY)
//		{
//			io_gdsIgettoken();
//			io_gdsIdetermine_points(2, MAXPOINTS, &n);
//			if (io_gdslayerused)
//				io_gdslayerpath[io_gdslayerwhich]++;
//	
//			// construct the path
//			for (i=0; i < n-1; i++)
//			{
//				fx = io_gdsvertex[i].px;    fy = io_gdsvertex[i].py;
//				tx = io_gdsvertex[i+1].px;  ty = io_gdsvertex[i+1].py;
//	
//				// determine whether either end needs to be shrunk
//				fextend = textend = width / 2;
//				thisAngle = figureangle(fx, fy, tx, ty);
//				if (i > 0)
//				{
//					lastAngle = figureangle(io_gdsvertex[i-1].px, io_gdsvertex[i-1].py,
//						io_gdsvertex[i].px, io_gdsvertex[i].py);
//					if (abs(thisAngle-lastAngle) % 900 != 0)
//					{
//						ang = abs(thisAngle-lastAngle) / 10;
//						if (ang > 180) ang = 360 - ang;
//						if (ang > 90) ang = 180 - ang;
//						fextend = tech_getextendfactor(width, ang);
//					}
//				} else
//				{
//					fextend = bgnextend;
//				}
//				if (i+1 < n-1)
//				{
//					nextAngle = figureangle(io_gdsvertex[i+1].px, io_gdsvertex[i+1].py,
//						io_gdsvertex[i+2].px, io_gdsvertex[i+2].py);
//					if (abs(thisAngle-nextAngle) % 900 != 0)
//					{
//						ang = abs(thisAngle-nextAngle) / 10;
//						if (ang > 180) ang = 360 - ang;
//						if (ang > 90) ang = 180 - ang;
//						textend = tech_getextendfactor(width, ang);
//					}
//				} else
//				{
//					textend = endextend;
//				}
//	
//				// handle arbitrary angle path segment
//				if (io_gdslayerused)
//				{
//					io_gdslayerpoly[io_gdslayerwhich]++;
//	
//					// get polygon
//					(void)needstaticpolygon(&poly, 4, io_tool->cluster);
//	
//					// determine shape of segment
//					length = computedistance(fx, fy, tx, ty);
//					j = figureangle(fx, fy, tx, ty);
//					tech_makeendpointpoly(length, width, j, fx, fy, fextend,
//						tx, ty, textend, poly);
//	
//					// make the node for this segment
//					if (isbox(poly, &lx, &hx, &ly, &hy))
//					{
//						if (newnodeinst(io_gdslayernode, lx, hx, ly, hy, 0, 0,
//							io_gdsstructure) == NONODEINST)
//								io_gdsIerror(42);
//					} else
//					{
//						getbbox(poly, &lx, &hx, &ly, &hy);
//						ni = newnodeinst(io_gdslayernode, lx, hx, ly, hy, 0, 0, io_gdsstructure);
//						if (ni == NONODEINST)
//							io_gdsIerror(42);
//	
//						// set the shape into the node
//						cx = (lx + hx) / 2;
//						cy = (ly + hy) / 2;
//						for(j=0; j<4; j++)
//						{
//							off[j*2] = poly->xv[j] - cx;
//							off[j*2+1] = poly->yv[j] - cy;
//						}
//						(void)setvalkey((INTBIG)ni, VNODEINST, el_trace_key, (INTBIG)off,
//							VINTEGER|VISARRAY|(8<<VLENGTHSH));
//					}
//				}
//			}
//		} else
//		{
//			io_gdsIerror(25);
//		}
//	}
//	
//	void io_gdsIgetproto(CHAR *name)
//	{
//		NODEPROTO *np;
//		REGISTER void *infstr;
//	
//		// scan for this proto
//		np = io_gdsIfindproto(name);
//		if (np == NONODEPROTO)
//		{
//			// FILO order, create this nodeproto
//			if ((np = us_newnodeproto(io_gdstokenstring, io_gdslibrary)) == NONODEPROTO)
//				io_gdsIerror(43);
//			infstr = initinfstr();
//			formatinfstr(infstr, _("Reading %s"), io_gdstokenstring);
//			DiaSetTextProgress(io_gdsprogressdialog, returninfstr(infstr));
//		}
//	
//		// set the reference node proto
//		io_gdssref = np;
//	}
//	
//	void io_gdsIdetermine_sref(void)
//	{
//		INTBIG n, angle, trans, cx, cy, ox, oy;
//		XARRAY transform;
//		double scale;
//	
//		io_gdsIgettoken();
//		io_gdsIunsupported(io_gdsIunsupported_set);
//		if (io_gdstoken == GDS_SNAME)
//		{
//			io_gdsIgettoken();
//			io_gdsIgetproto(io_gdstokenstring);
//			io_gdsIgettoken();
//			if (io_gdstoken == GDS_STRANS)
//				io_gdsIdetermine_orientation(&angle, &trans, &scale);
//			else
//			{
//				angle = 0; trans = 0;
//				scale = 1.0;
//			}
//			if (io_gdstoken == GDS_XY)
//			{
//				io_gdsIgettoken();
//				io_gdsIdetermine_points(1, 1, &n);
//				// close Translate
//			} else
//			{
//				io_gdsIerror(17);
//			}
//	
//			// create the node
//			makeangle(angle, trans, transform);
//	
//			// now transform the center
//			cx = (io_gdssref->highx + io_gdssref->lowx) / 2;
//			cy = (io_gdssref->highy + io_gdssref->lowy) / 2;
//			xform(cx, cy, &ox, &oy, transform);
//	
//			// calculate the offset from the original center
//			ox -= cx;   oy -= cy;
//	
//			if ((io_gdsinstance = newnodeinst(io_gdssref,
//				io_gdsvertex[0].px + io_gdssref->lowx + ox,
//				io_gdsvertex[0].px + io_gdssref->highx + ox,
//				io_gdsvertex[0].py + io_gdssref->lowy + oy,
//				io_gdsvertex[0].py + io_gdssref->highy + oy,
//				trans, angle, io_gdsstructure)) == NONODEINST)
//					io_gdsIerror(44);
//			if (io_gdscurstate[0]&GDSINEXPAND)
//				io_gdsinstance->userbits |= NEXPAND;
//			io_gdssrefcount++;
//		} else
//		{
//			io_gdsIerror(16);
//		}
//	}
//	
//	void io_gdsIdetermine_aref(void)
//	{
//		INTBIG n, ncols, nrows, angle, trans;
//		double scale;
//	
//		io_gdsIgettoken();
//		io_gdsIunsupported(io_gdsIunsupported_set);
//		if (io_gdstoken == GDS_SNAME)
//		{
//			io_gdsIgettoken();
//	
//			// get this nodeproto
//			io_gdsIgetproto(io_gdstokenstring);
//			io_gdsIgettoken();
//			if (io_gdstoken == GDS_STRANS)
//				io_gdsIdetermine_orientation(&angle, &trans, &scale);
//			else
//			{
//				angle = trans = 0;
//				scale = 1.0;
//			}
//			if (io_gdstoken == GDS_COLROW)
//			{
//				io_gdsIgettoken();
//				ncols = io_gdstokenvalue16;
//				io_gdsIgettoken();
//				nrows = io_gdstokenvalue16;
//				io_gdsIgettoken();
//			}
//			if (io_gdstoken == GDS_XY)
//			{
//				io_gdsIgettoken();
//				io_gdsIdetermine_points(3, 3, &n);
//				io_gdsIstep(ncols, nrows, angle, trans);
//			} else
//			{
//				io_gdsIerror(27);
//			}
//		} else
//		{
//			io_gdsIerror(28);
//		}
//	}
//	
//	void io_gdsIdetermine_justification(INTBIG *vert_just, INTBIG *horiz_just)
//	{
//		INTBIG font_libno;
//	
//		io_gdsIgettoken();
//		if (io_gdstoken == GDS_FLAGSYM)
//		{
//			font_libno = io_gdstokenflags & 0x0030;
//			font_libno = font_libno >> 4;
//			(*vert_just) = io_gdstokenflags & 0x000C;
//			(*vert_just) = (*vert_just) >> 2;
//			(*horiz_just) = io_gdstokenflags & 0x0003;
//			io_gdsIgettoken();
//		} else
//		{
//			io_gdsIerror(27);
//		}
//	}
//	
//	void io_gdsIdetermine_text(void)
//	{
//		INTBIG vert_just, horiz_just, layer, sublayer;
//		CHAR textstring[TEXTLINE+1];
//		INTBIG n, angle, trans;
//		double scale;
//	
//		io_gdsIgettoken();
//		io_gdsIunsupported(io_gdsIunsupported_set);
//		io_gdsIdetermine_layer(&layer, &sublayer);
//		io_gdsIgettoken();
//		vert_just = -1;
//		horiz_just = -1;
//		if (io_gdstoken == GDS_PRESENTATION)
//			io_gdsIdetermine_justification(&vert_just, &horiz_just);
//		if (io_gdstoken == GDS_PATHTYPE)
//		{
//			io_gdsIgettoken();
//			// pathcode = io_gdstokenvalue16;
//			io_gdsIgettoken();
//		}
//		if (io_gdstoken == GDS_WIDTH)
//		{
//			io_gdsIgettoken();
//			// pathwidth = rounddouble((double)io_gdstokenvalue32 * io_gdsscale);
//			io_gdsIgettoken();
//		}
//		angle = trans = 0;
//		scale = 1.0;
//		for(;;)
//		{
//			if (io_gdstoken == GDS_STRANS)
//			{
//				io_gdsIdetermine_orientation(&angle, &trans, &scale);
//				continue;
//			}
//			if (io_gdstoken == GDS_XY)
//			{
//				io_gdsIgettoken();
//				io_gdsIdetermine_points(1, 1, &n);
//				continue;
//			}
//			if (io_gdstoken == GDS_ANGLE)
//			{
//				io_gdsIgettoken();
//				angle = (INTBIG)(io_gdsrealtokendp * 10.0);
//				io_gdsIgettoken();
//				continue;
//			}
//			if (io_gdstoken == GDS_STRING)
//			{
//				if (io_gdsrecordcount == 0) textstring[0] = '\0'; else
//				{
//					io_gdsIgettoken();
//					(void)estrcpy(textstring, io_gdstokenstring);
//				}
//				io_gdsIgettoken();
//				break;
//			}
//			if (io_gdstoken == GDS_MAG)
//			{
//				io_gdsIgettoken();
//				io_gdsIgettoken();
//				continue;
//			}
//			io_gdsIerror(26);
//			break;
//		}
//		io_gdsItext(textstring, vert_just, horiz_just, angle, trans, scale);
//	}
//	
//	void io_gdsIelement(BOOLEAN *empty_structure)
//	{
//		while (io_gdsImember(io_gdstoken, io_gdsIshape_set))
//		{
//			switch (io_gdstoken)
//			{
//				case GDS_AREF:     io_gdsIdetermine_aref();    break;
//				case GDS_SREF:     io_gdsIdetermine_sref();    break;
//				case GDS_BOUNDARY: io_gdsIdetermine_shape();   break;
//				case GDS_PATH:     io_gdsIdetermine_path();    break;
//				case GDS_NODE:     io_gdsIdetermine_node();    break;
//				case GDS_TEXTSYM:  io_gdsIdetermine_text();    break;
//				case GDS_BOX:      io_gdsIdetermine_box();     break;
//				default:                                       break;
//			}
//		}
//	
//		while (io_gdstoken == GDS_PROPATTR)
//			io_gdsIdetermine_property();
//		if (io_gdstoken != GDS_ENDEL)
//		{
//			io_gdsIerror(15);
//		}
//		(*empty_structure) = FALSE;
//	}
//	
//	void io_gdsIstructure(void)
//	{
//		BOOLEAN empty_structure;
//	
//		io_gdsIbeginstructure();
//		empty_structure = TRUE;
//		io_gdsIgettoken();
//		while (io_gdstoken != GDS_ENDSTR)
//		{
//			io_gdsIelement(&empty_structure);
//			io_gdsIgettoken();
//		}
//		if (empty_structure)
//		{
//			ttyputmsg("Warning this structure is empty");
//			ttyputerr(_("Error at byte number %d"), byteCount);
//			return;
//		}
//	
//		db_boundcell(io_gdsstructure, &io_gdsstructure->lowx, &io_gdsstructure->highx,
//			&io_gdsstructure->lowy, &io_gdsstructure->highy);
//	}
//	
//	void io_gdsIrecompute(NODEPROTO *np, INTBIG count)
//	{
//		REGISTER INTBIG cx, cy;
//		CHAR numsofar[50];
//		INTBIG ox, oy;
//		XARRAY trans;
//		REGISTER NODEINST *ni;
//	
//		// rebuild the geometry database, note that this is faster then just updating the SREF info
//		// first clear the geometry structure
//		db_freertree(np->rtree);
//		(void)geomstructure(np);
//		for(ni = np->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//		{
//			if (ni->proto->primindex == 0)
//			{
//				if (ni->proto->temp1 == 0)
//					io_gdsIrecompute(ni->proto, count);
//				if ((ni->highx-ni->lowx) != (ni->proto->highx-ni->proto->lowx) ||
//					(ni->highy-ni->lowy) != (ni->proto->highy-ni->proto->lowy))
//				{
//					makeangle(ni->rotation, ni->transpose, trans);
//	
//					// now transform the center
//					cx = (ni->proto->highx + ni->proto->lowx) / 2;
//					cy = (ni->proto->highy + ni->proto->lowy) / 2;
//					xform(cx, cy, &ox, &oy, trans);
//	
//					// calculate the offset from the original center
//					ox -= cx;   oy -= cy;
//					ni->lowx += (ni->proto->lowx + ox);
//					ni->highx += (ni->proto->highx + ox);
//					ni->lowy += (ni->proto->lowy + oy);
//					ni->highy += (ni->proto->highy + oy);
//					boundobj(ni->geom, &(ni->geom->lowx), &(ni->geom->highx),
//						&(ni->geom->lowy), &(ni->geom->highy));
//				}
//			}
//	
//			// now link into the geometry list
//			linkgeom(ni->geom, np);
//	
//			io_gdsrecomputed++;
//			DiaSetTextProgress(io_gdsprogressdialog, numsofar);
//		}
//	
//		db_boundcell(np, &np->lowx, &np->highx, &np->lowy, &np->highy);
//		np->temp1 = 1;
//	}
