/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: Generate.java
 * User tool: Technology Editor, creation
 * Written by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2005 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.user.tecEdit;

import com.sun.electric.database.geometry.EGraphics;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.output.Output;
import com.sun.electric.tool.user.User;
import com.sun.electric.tool.user.menus.CellMenu.SetMultiPageJob;
import com.sun.electric.tool.user.ui.EditWindow;
import com.sun.electric.tool.user.ui.WindowFrame;
import com.sun.electric.tool.user.ui.TopLevel;

import java.awt.Color;
import java.awt.Dimension;
import java.util.Iterator;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import javax.swing.JOptionPane;

/**
 * This class generates technologies from technology libraries.
 */
public class Generate
{

	static class LayerInfo
	{
		EGraphics desc;
		Layer.Function fun;
		String cif;
		String dxf;
		String gds;
		double spires;
		double spicap;
		double spiecap;
		double height3d;
		double thick3d;

		LayerInfo()
		{
		}

		/*
		 * Method to build the appropriate descriptive information for a layer into
		 * cell "np".  The color is "colorindex"; the stipple array is in "stip"; the
		 * layer style is in "style", the CIF layer is in "ciflayer"; the function is
		 * in "functionindex"; the layer letters are in "layerletters"; the DXF layer name(s)
		 * are "dxf"; the Calma GDS-II layer is in "gds"; the SPICE resistance is in "spires",
		 * the SPICE capacitance is in "spicap", the SPICE edge capacitance is in "spiecap",
		 * the 3D height is in "height3d", and the 3D thickness is in "thick3d".
		 */
		void us_tecedmakelayer(Cell np)
		{
			NodeInst laycolor = null, laystipple = null, laypatcontrol = null;
			for(Iterator it = np.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				Variable var = ni.getVar(EDTEC_OPTION);
				if (var == null) continue;
				switch (((Integer)var.getObject()).intValue())
				{
					case LAYERCOLOR:     laycolor = ni;      break;
					case LAYERPATTERN:   laystipple = ni;    break;
					case LAYERPATCONT:   laypatcontrol = ni; break;
				}
			}
		
			// create the color information if it is not there
			if (laycolor == null)
			{
				// create the graphic color object
				NodeInst nicolor = NodeInst.makeInstance(Artwork.tech.filledBoxNode, new Point2D.Double(-15000/SCALEALL,15000/SCALEALL),
					10000/SCALEALL, 10000/SCALEALL, np);
				if (nicolor == null) return;
				Create.us_teceditsetpatch(nicolor, desc);
				np.newVar("EDTEC_colornode", nicolor);

				// create the text color object
				NodeInst ni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, new Point2D.Double(28000/SCALEALL, 24000/SCALEALL), 0, 0, np);
				if (ni == null) return;
				String infstr = TECEDNODETEXTCOLOR;
				if (desc.getTransparentLayer() > 0) infstr += EGraphics.getColorIndexName(EGraphics.makeIndex(desc.getTransparentLayer())); else
					infstr += EGraphics.getColorIndexName(EGraphics.makeIndex(desc.getColor()));
				Variable var = ni.newVar(Artwork.ART_MESSAGE, infstr);
				if (var != null) var.setDisplay(true);
				ni.newVar(EDTEC_OPTION, new Integer(LAYERCOLOR));
			}
		
			// create the stipple pattern objects if none are there
			int [] stip = desc.getPattern();
			if (laystipple == null)
			{
				for(int x=0; x<16; x++) for(int y=0; y<16; y++)
				{
					Point2D ctr = new Point2D.Double((x*2000-39000)/SCALEALL, (5000-y*2000)/SCALEALL);
					NodeInst ni = NodeInst.makeInstance(Artwork.tech.filledBoxNode, ctr, 2000/SCALEALL, 2000/SCALEALL, np);
					if (ni == null) return;
					if ((stip[y] & (1 << (15-x))) == 0)
					{
						Short [] spattern = new Short[16];
						for(int i=0; i<16; i++) spattern[i] = new Short((short)0);
						ni.newVar(Artwork.ART_PATTERN, spattern);
					}
					ni.newVar(EDTEC_OPTION, new Integer(LAYERPATTERN));
				}
				NodeInst ni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, new Point2D.Double(-24000/SCALEALL,7000/SCALEALL), 0, 0, np);
				if (ni == null) return;
				Variable var = ni.newVar(Artwork.ART_MESSAGE, "Stipple Pattern");
				if (var != null)
				{
					var.setDisplay(true);
					var.setRelSize(0.5);
				}
			}
		
			// create the patch control object
			if (laypatcontrol == null)
			{
				NodeInst ni = NodeInst.makeInstance(Generic.tech.invisiblePinNode,
					new Point2D.Double((16000-40000)/SCALEALL, (4000-16*2000)/SCALEALL), 0, 0, np);
				if (ni == null) return;
				Variable var = ni.newVar(Artwork.ART_MESSAGE, "Stipple Pattern Operations");
				if (var != null) var.setDisplay(true);
				ni.newVar(EDTEC_OPTION, new Integer(LAYERPATCONT));
			}
		
			// load up the structure with the current values
			for(int i=0; i<us_tecedlayertexttable.length; i++)
			{
				switch (us_tecedlayertexttable[i].funct)
				{
					case LAYERSTYLE:
//						us_tecedlayertexttable[i].value = style;
						break;
					case LAYERCIF:
						us_tecedlayertexttable[i].value = cif;
						break;
					case LAYERDXF:
						us_tecedlayertexttable[i].value = dxf;
						break;
					case LAYERGDS:
						us_tecedlayertexttable[i].value = gds;
						break;
					case LAYERFUNCTION:
						us_tecedlayertexttable[i].value = fun;
						break;
					case LAYERLETTERS:
//						us_tecedlayertexttable[i].value = layerletters;
						break;
					case LAYERSPIRES:
						us_tecedlayertexttable[i].value = new Double(spires);
						break;
					case LAYERSPICAP:
						us_tecedlayertexttable[i].value = new Double(spicap);
						break;
					case LAYERSPIECAP:
						us_tecedlayertexttable[i].value = new Double(spiecap);
						break;
					case LAYER3DHEIGHT:
						us_tecedlayertexttable[i].value = new Double(height3d);
						break;
					case LAYER3DTHICK:
						us_tecedlayertexttable[i].value = new Double(thick3d);
						break;
					case LAYERPRINTCOL:
//						us_tecedlayertexttable[i].value = printcolors;
						break;
				}
			}
		
			// now create those text objects
			us_tecedcreatespecialtext(np, us_tecedlayertexttable);
		}
		
		/**
		 * Method to parse the layer cell in "np" and fill these reference descriptors:
		 *   "desc" (a GRAPHICS structure)
		 *   "cif" (the name of the CIF layer)
		 *   "dxf" (the name of the DXF layer)
		 *   "func" (the integer function number)
		 *   "layerletters" (the letters associated with this layer),
		 *   "gds" (the Calma GDS-II layer number)
		 *   "spires" (the SPICE resistance)
		 *   "spicap" (the SPICE capacitance)
		 *   "spiecap" (the SPICE edge capacitance)
		 *   "drcminwid" (the DRC minimum width)
		 *   "height3d" (the 3D height)
		 *   "thick3d" (the 3D thickness)
		 *   "printcol" (the printer colors)
		 * All of the reference parameters except "func", "spires", "spicap", and "spiecap"
		 * get allocated.  Returns true on error.
		 */
		static LayerInfo us_teceditgetlayerinfo(Cell np)
		{
			// create and initialize the GRAPHICS structure
			LayerInfo li = new LayerInfo();
			li.desc = new EGraphics(EGraphics.SOLID, EGraphics.PATTERNED, 0, 0, 0, 0, 1, false, null);
		
			// look at all nodes in the layer description cell
			int patterncount = 0;
			Rectangle2D patternBounds = null;
			for(Iterator it = np.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				Variable varkey = ni.getVar(EDTEC_OPTION);
				if (varkey == null) continue;
				Variable var = ni.getVar(Artwork.ART_MESSAGE);
				String str = "";
				if (var != null)
				{
					str = (String)var.getObject();
					int colonPos = str.indexOf(':');
					if (colonPos >= 0) str = str.substring(colonPos+2);
				}
		
				switch (((Integer)varkey.getObject()).intValue())
				{
					case LAYERPATTERN:
						if (patterncount == 0)
						{
							patternBounds = ni.getBounds();
						} else
						{
							Rectangle2D.union(patternBounds, ni.getBounds(), patternBounds);
						}
						patterncount++;
						break;
					case LAYERCOLOR:
//						color = getecolor(str);
//						if (color < 0)
//						{
//							ttyputerr(_("Unknown color '%s' in %s"), str, describenodeproto(np));
//							return(TRUE);
//						}
//						desc.col = color;
//						switch (color)
//						{
//							case COLORT1: desc.bits = LAYERT1; break;
//							case COLORT2: desc.bits = LAYERT2; break;
//							case COLORT3: desc.bits = LAYERT3; break;
//							case COLORT4: desc.bits = LAYERT4; break;
//							case COLORT5: desc.bits = LAYERT5; break;
//							default:      desc.bits = LAYERO;  break;
//						}
						break;
					case LAYERSTYLE:
						if (str.equalsIgnoreCase("solid"))
						{
							li.desc.setPatternedOnDisplay(false);
							li.desc.setPatternedOnPrinter(false);
						} else if (str.equalsIgnoreCase("patterned"))
						{
							li.desc.setPatternedOnDisplay(true);
							li.desc.setPatternedOnPrinter(true);
							li.desc.setOutlinedOnDisplay(false);
							li.desc.setOutlinedOnPrinter(false);
						} else if (str.equalsIgnoreCase("patterned/outlined"))
						{
							li.desc.setPatternedOnDisplay(true);
							li.desc.setPatternedOnPrinter(true);
							li.desc.setOutlinedOnDisplay(true);
							li.desc.setOutlinedOnPrinter(true);
						}
						break;
					case LAYERCIF:
						li.cif = str;
						break;
					case LAYERDXF:
						li.dxf = str;
						break;
					case LAYERGDS:
						if (str.equals("-1")) str = "";
						li.gds = str;
						break;
					case LAYERFUNCTION:
//						*func = us_teceditparsefun(str);
						break;
					case LAYERLETTERS:
//						if (allocstring(layerletters, str, us_tool.cluster)) return(TRUE);
						break;
					case LAYERSPIRES:
						li.spires = TextUtils.atof(str);
						break;
					case LAYERSPICAP:
						li.spicap = TextUtils.atof(str);
						break;
					case LAYERSPIECAP:
						li.spiecap = TextUtils.atof(str);
						break;
					case LAYERDRCMINWID:
//						*drcminwid = atofr(str);
						break;
					case LAYER3DHEIGHT:
						li.height3d = TextUtils.atof(str);
						break;
					case LAYER3DTHICK:
						li.thick3d = TextUtils.atof(str);
						break;
					case LAYERPRINTCOL:
//						us_teceditgetprintcol(var, &printcol[0], &printcol[1], &printcol[2],
//							&printcol[3], &printcol[4]);
						break;
				}
			}
		
			if (patterncount != 16*16 && patterncount != 16*8)
			{
				System.out.println("Incorrect number of pattern boxes in " + np.describe() +
					" (has " + patterncount + ", not " + (16*16) + ")");
				return null;
			}
		
			// construct the pattern
			int [] newPat = new int[16];
			for(Iterator it = np.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				if (ni.getProto() != Artwork.tech.filledBoxNode) continue;
				Variable var = ni.getVar(EDTEC_OPTION);
				if (var == null) continue;
				if (((Integer)var.getObject()).intValue() != LAYERPATTERN) continue;
				var = ni.getVar(Artwork.ART_PATTERN);
				if (var != null)
				{
					Short [] pat = (Short [])var.getObject();
					boolean nonzero = false;
					for(int i=0; i<pat.length; i++) if (pat[i].shortValue() != 0) { nonzero = true;   break; }
					if (!nonzero) continue;
				}
				Rectangle2D niBounds = ni.getBounds();
				int x = (int)((niBounds.getMinX() - patternBounds.getMinX()) / (patternBounds.getWidth() / 16));
				int y = (int)((niBounds.getMaxY() - patternBounds.getMaxY()) / (patternBounds.getHeight() / 16));
				newPat[y] |= (1 << (15-x));
			}
			if (patterncount == 16*8)
			{
				// older, half-height pattern: replicate it
				for(int y=0; y<8; y++)
					newPat[y+8] = newPat[y];
			}
			li.desc.setPattern(newPat);
			return li;
		}
		/*
		 * routine to parse the layer function string "str" and return the
		 * actual function codes
		 */
		private Layer.Function us_teceditparsefun(String str)
		{
//			REGISTER INTBIG func, save, i;
//			REGISTER CHAR *pt;
		
//			List allFuncs = Layer.Function.getFunctions();
//			func = 0;
//			for(;;)
//			{
//				// find the next layer function name
//				pt = str;
//				while (*pt != 0 && *pt != ',') pt++;
//		
//				// parse the name
//				save = *pt;
//				*pt = 0;
//				for(i=0; us_teclayer_functions[i].name != 0; i++)
//					if (namesame(str, us_teclayer_functions[i].name) == 0) break;
//				*pt = (CHAR)save;
//				if (us_teclayer_functions[i].name == 0)
//				{
//					ttyputerr(_("Unknown layer function: %s"), str);
//					return(0);
//				}
//		
//				// mix in the layer function
//				if (us_teclayer_functions[i].value <= LFTYPE)
//				{
//					if (func != 0)
//					{
//						ttyputerr(_("Cannot be both function %s and %s"),
//							us_teclayer_functions[func&LFTYPE].name, us_teclayer_functions[i].name);
//						func = 0;
//					}
//					func = us_teclayer_functions[i].value;
//				} else func |= us_teclayer_functions[i].value;
//		
//				// advance to the next layer function name
//				if (*pt == 0) break;
//				str = pt + 1;
//			}
//			return(func);
			return null;
		}
	}

	static final double SCALEALL = 2000;

	/* the meaning of "EDTEC_option" on nodes */
	static final int LAYERCOLOR     =  1;					/* color (layer cell) */
	static final int LAYERSTYLE     =  2;					/* style (layer cell) */
	static final int LAYERCIF       =  3;					/* CIF name (layer cell) */
	static final int LAYERFUNCTION  =  4;					/* function (layer cell) */
	static final int LAYERLETTERS   =  5;					/* letters (layer cell) */
	static final int LAYERPATTERN   =  6;					/* pattern (layer cell) */
	static final int LAYERPATCONT   =  7;					/* pattern control (layer cell) */
	static final int LAYERPATCH     =  8;					/* patch of layer (node/arc cell) */
	static final int ARCFUNCTION    =  9;					/* function (arc cell) */
	static final int NODEFUNCTION   = 10;					/* function (node cell) */
	static final int ARCFIXANG      = 11;					/* fixed-angle (arc cell) */
	static final int ARCWIPESPINS   = 12;					/* wipes pins (arc cell) */
	static final int ARCNOEXTEND    = 13;					/* end extension (arc cell) */
	static final int TECHLAMBDA     = 14;					/* lambda (info cell) */
	static final int TECHDESCRIPT   = 15;					/* description (info cell) */
	static final int NODESERPENTINE = 16;					/* serpentine MOS trans (node cell) */
	static final int LAYERDRCMINWID = 17;					/* DRC minimum width (layer cell, OBSOLETE) */
	static final int PORTOBJ        = 18;					/* port object (node cell) */
	static final int HIGHLIGHTOBJ   = 19;					/* highlight object (node/arc cell) */
	static final int LAYERGDS       = 20;					/* Calma GDS-II layer (layer cell) */
	static final int NODESQUARE     = 21;					/* square node (node cell) */
	static final int NODEWIPES      = 22;					/* pin node can disappear (node cell) */
	static final int ARCINC         = 23;					/* increment for arc angles (arc cell) */
	static final int NODEMULTICUT   = 24;					/* separation of multiple contact cuts (node cell) */
	static final int NODELOCKABLE   = 25;					/* lockable primitive (node cell) */
	static final int CENTEROBJ      = 26;					/* grab point object (node cell) */
	static final int LAYERSPIRES    = 27;					/* SPICE resistance (layer cell) */
	static final int LAYERSPICAP    = 28;					/* SPICE capacitance (layer cell) */
	static final int LAYERSPIECAP   = 29;					/* SPICE edge capacitance (layer cell) */
	static final int LAYERDXF       = 30;					/* DXF layer (layer cell) */
	static final int LAYER3DHEIGHT  = 31;					/* 3D height (layer cell) */
	static final int LAYER3DTHICK   = 32;					/* 3D thickness (layer cell) */
	static final int LAYERPRINTCOL  = 33;					/* print colors (layer cell) */

	/** key of Variable holding option information. */		public static final Variable.Key EDTEC_OPTION = ElectricObject.newKey("EDTEC_option");
	/** key of Variable holding layer information. */		public static final Variable.Key EDTEC_LAYER = ElectricObject.newKey("EDTEC_layer");

	/* the strings that appear in the technology editor */
	static final String TECEDNODETEXTCOLOR      = "Color: ";
	static final String TECEDNODETEXTSTYLE      = "Style: ";
	static final String TECEDNODETEXTFUNCTION   = "Function: ";
	static final String TECEDNODETEXTLETTERS    = "Layer letters: ";
	static final String TECEDNODETEXTGDS        = "GDS-II Layer: ";
	static final String TECEDNODETEXTCIF        = "CIF Layer: ";
	static final String TECEDNODETEXTDXF        = "DXF Layer(s): ";
	static final String TECEDNODETEXTSPICERES   = "SPICE Resistance: ";
	static final String TECEDNODETEXTSPICECAP   = "SPICE Capacitance: ";
	static final String TECEDNODETEXTSPICEECAP  = "SPICE Edge Capacitance: ";
	static final String TECEDNODETEXTDRCMINWID  = "DRC Minimum Width: ";
	static final String TECEDNODETEXT3DHEIGHT   = "3D Height: ";
	static final String TECEDNODETEXT3DTHICK    = "3D Thickness: ";
	static final String TECEDNODETEXTPRINTCOL   = "Print colors: ";

	/* additional technology variables */
	static class TechVar
	{
		String    varname;
		TechVar   nexttechvar;
		boolean   changed;
		int       ival;
		float     fval;
		String    sval;
		int       vartype;
		String    description;

		TechVar(String name, String desc)
		{
			varname = name;
			description = desc;
		}
	};
	
//	#define MAXNAMELEN 25		/* max chars in a new name */
//	
//	INTBIG us_teceddrclayers = 0;
//	CHAR **us_teceddrclayernames = 0;
	
	/* the known technology variables */
	static TechVar [] us_knownvars =
	{
		new TechVar("DRC_ecad_deck",             /*VSTRING|VISARRAY,*/ "Dracula design-rule deck"),
		new TechVar("IO_cif_polypoints",         /*VINTEGER,*/         "Maximum points in a CIF polygon"),
		new TechVar("IO_cif_resolution",         /*VINTEGER,*/         "Minimum resolution of CIF coordinates"),
		new TechVar("IO_gds_polypoints",         /*VINTEGER,*/         "Maximum points in a GDS-II polygon"),
		new TechVar("SIM_spice_min_resistance",  /*VFLOAT,*/           "Minimum resistance of SPICE elements"),
		new TechVar("SIM_spice_min_capacitance", /*VFLOAT,*/           "Minimum capacitance of SPICE elements"),
		new TechVar("SIM_spice_mask_scale",      /*VFLOAT,*/           "Scaling factor for SPICE decks"),
		new TechVar("SIM_spice_header_level1",   /*VSTRING|VISARRAY,*/ "Level 1 header for SPICE decks"),
		new TechVar("SIM_spice_header_level2",   /*VSTRING|VISARRAY,*/ "Level 2 header for SPICE decks"),
		new TechVar("SIM_spice_header_level3",   /*VSTRING|VISARRAY,*/ "Level 3 header for SPICE decks"),
		new TechVar("SIM_spice_model_file",      /*VSTRING,*/          "Disk file with SPICE header cards"),
		new TechVar("SIM_spice_trailer_file",    /*VSTRING,*/          "Disk file with SPICE trailer cards")
	};

//	typedef struct Ilist
//	{
//		CHAR  *name;
//		CHAR  *constant;
//		INTBIG value;
//	} LIST;
//
//
//	#define NOSAMPLE ((SAMPLE *)-1)
//
//	typedef struct Isample
//	{
//		NODEINST        *node;					/* true node used for sample */
//		NODEPROTO       *layer;					/* type of node used for sample */
//		INTBIG           xpos, ypos;			/* center of sample */
//		struct Isample  *assoc;					/* associated sample in first example */
//		struct Irule    *rule;					/* rule associated with this sample */
//		struct Iexample *parent;				/* example containing this sample */
//		struct Isample  *nextsample;			/* next sample in list */
//	} SAMPLE;
//
//
//	#define NOEXAMPLE ((EXAMPLE *)-1)
//
//	typedef struct Iexample
//	{
//		SAMPLE          *firstsample;			/* head of list of samples in example */
//		SAMPLE          *studysample;			/* sample under analysis */
//		INTBIG           lx, hx, ly, hy;		/* bounding box of example */
//		struct Iexample *nextexample;			/* next example in list */
//	} EXAMPLE;
//
//
//	/* port connections */
//	#define NOPCON ((PCON *)-1)
//
//	typedef struct Ipcon
//	{
//		INTBIG       *connects;
//		INTBIG       *assoc;
//		INTBIG        total;
//		INTBIG        pcindex;
//		struct Ipcon *nextpcon;
//	} PCON;
//
//
//	/* rectangle rules */
//	#define NORULE ((RULE *)-1)
//
//	typedef struct Irule
//	{
//		INTBIG       *value;					/* data points for rule */
//		INTBIG        count;					/* number of points in rule */
//		INTBIG        istext;					/* nonzero if text at end of rule */
//		INTBIG        rindex;					/* identifier for this rule */
//		BOOLEAN       used;						/* nonzero if actually used */
//		BOOLEAN       multicut;					/* nonzero if this is multiple cut */
//		INTBIG        multixs, multiys;			/* size of multicut */
//		INTBIG        multiindent, multisep;	/* indent and separation of multicuts */
//		struct Irule *nextrule;
//	} RULE;
//
//
//	/* the meaning of "us_tecflags" */
//	#define HASDRCMINWID         01				/* has DRC minimum width information */
//	#define HASDRCMINWIDR        02				/* has DRC minimum width information */
//	#define HASCOLORMAP          04				/* has color map */
//	#define HASARCWID           010				/* has arc width offset factors */
//	#define HASCIF              020				/* has CIF layers */
//	#define HASDXF              040				/* has DXF layers */
//	#define HASGDS             0100				/* has Calma GDS-II layers */
//	#define HASGRAB            0200				/* has grab point information */
//	#define HASSPIRES          0400				/* has SPICE resistance information */
//	#define HASSPICAP         01000				/* has SPICE capacitance information */
//	#define HASSPIECAP        02000				/* has SPICE edge capacitance information */
//	#define HAS3DINFO         04000				/* has 3D height/thickness information */
//	#define HASCONDRC        010000				/* has connected design rules */
//	#define HASCONDRCR       020000				/* has connected design rules reasons */
//	#define HASUNCONDRC      040000				/* has unconnected design rules */
//	#define HASUNCONDRCR    0100000				/* has unconnected design rules reasons */
//	#define HASCONDRCW      0200000				/* has connected wide design rules */
//	#define HASCONDRCWR     0400000				/* has connected wide design rules reasons */
//	#define HASUNCONDRCW   01000000				/* has unconnected wide design rules */
//	#define HASUNCONDRCWR  02000000				/* has unconnected wide design rules reasons */
//	#define HASCONDRCM     04000000				/* has connected multicut design rules */
//	#define HASCONDRCMR   010000000				/* has connected multicut design rules reasons */
//	#define HASUNCONDRCM  020000000				/* has unconnected multicut design rules */
//	#define HASUNCONDRCMR 040000000				/* has unconnected multicut design rules reasons */
//	#define HASEDGEDRC   0100000000				/* has edge design rules */
//	#define HASEDGEDRCR  0200000000				/* has edge design rules reasons */
//	#define HASMINNODE   0400000000				/* has minimum node size */
//	#define HASMINNODER 01000000000				/* has minimum node size reasons */
//	#define HASPRINTCOL 02000000000				/* has print colors */

	/* for describing special text in a cell */
	static class SpecialTextDescr
	{
		NodeInst ni;
		Object   value;
		double   x, y;
		int      funct;

		SpecialTextDescr(Object value, double x, double y, int funct)
		{
			ni = null;
			this.value = value;
			this.x = x;
			this.y = y;
			this.funct = funct;
		}
	};

//	/* the globals that define a technology */
//	extern INTBIG           us_teceddrclayers;
//	extern CHAR           **us_teceddrclayernames;
//
//	extern LIST             us_teclayer_functions[];

	static SpecialTextDescr [] us_tecedmisctexttable =
	{
		new SpecialTextDescr(null, 0/SCALEALL, 6000/SCALEALL, TECHLAMBDA),
		new SpecialTextDescr(null, 0/SCALEALL,    0/SCALEALL, TECHDESCRIPT),
		new SpecialTextDescr(null, 0/SCALEALL,    0/SCALEALL, 0)
	};
	
	static SpecialTextDescr [] us_tecedlayertexttable =
	{
		new SpecialTextDescr(null, 28000/SCALEALL,  18000/SCALEALL, LAYERSTYLE),
		new SpecialTextDescr(null, 28000/SCALEALL,  12000/SCALEALL, LAYERCIF),
		new SpecialTextDescr(null, 28000/SCALEALL,   6000/SCALEALL, LAYERDXF),
		new SpecialTextDescr(null, 28000/SCALEALL,      0/SCALEALL, LAYERGDS),
		new SpecialTextDescr(null, 28000/SCALEALL,  -6000/SCALEALL, LAYERFUNCTION),
		new SpecialTextDescr(null, 28000/SCALEALL, -12000/SCALEALL, LAYERLETTERS),
		new SpecialTextDescr(null, 28000/SCALEALL, -18000/SCALEALL, LAYERSPIRES),
		new SpecialTextDescr(null, 28000/SCALEALL, -24000/SCALEALL, LAYERSPICAP),
		new SpecialTextDescr(null, 28000/SCALEALL, -30000/SCALEALL, LAYERSPIECAP),
		new SpecialTextDescr(null, 28000/SCALEALL, -36000/SCALEALL, LAYER3DHEIGHT),
		new SpecialTextDescr(null, 28000/SCALEALL, -42000/SCALEALL, LAYER3DTHICK),
		new SpecialTextDescr(null, 28000/SCALEALL, -48000/SCALEALL, LAYERPRINTCOL)
	};
	
	static SpecialTextDescr [] us_tecedarctexttable =
	{
		new SpecialTextDescr(null, 0/SCALEALL, 30000/SCALEALL, ARCFUNCTION),
		new SpecialTextDescr(null, 0/SCALEALL, 24000/SCALEALL, ARCFIXANG),
		new SpecialTextDescr(null, 0/SCALEALL, 18000/SCALEALL, ARCWIPESPINS),
		new SpecialTextDescr(null, 0/SCALEALL, 12000/SCALEALL, ARCNOEXTEND),
		new SpecialTextDescr(null, 0/SCALEALL,  6000/SCALEALL, ARCINC)
	};
	
	static SpecialTextDescr [] us_tecednodetexttable =
	{
		new SpecialTextDescr(null, 0/SCALEALL, 36000/SCALEALL, NODEFUNCTION),
		new SpecialTextDescr(null, 0/SCALEALL, 30000/SCALEALL, NODESERPENTINE),
		new SpecialTextDescr(null, 0/SCALEALL, 24000/SCALEALL, NODESQUARE),
		new SpecialTextDescr(null, 0/SCALEALL, 18000/SCALEALL, NODEWIPES),
		new SpecialTextDescr(null, 0/SCALEALL, 12000/SCALEALL, NODELOCKABLE),
		new SpecialTextDescr(null, 0/SCALEALL,  6000/SCALEALL, NODEMULTICUT)
	};

	public static void makeLibFromTech()
	{
		List techs = new ArrayList();
		for(Iterator it = Technology.getTechnologies(); it.hasNext(); )
		{
			Technology tech = (Technology)it.next();
			if (tech.isNonStandard()) continue;
			techs.add(tech);
		}
		String [] techChoices = new String[techs.size()];
		for(int i=0; i<techs.size(); i++)
			techChoices[i] = ((Technology)techs.get(i)).getTechName();
        String chosen = (String)JOptionPane.showInputDialog(TopLevel.getCurrentJFrame(), "Technology to Edit",
            "Choose a technology to edit", JOptionPane.QUESTION_MESSAGE, null, techChoices, Technology.getCurrent().getTechName());
        if (chosen == null) return;
        Technology tech = Technology.findTechnology(chosen);
        LibFromTechJob job = new LibFromTechJob(tech);
	}

    /**
     * Class to create a technology-library from a technology.
     */
    public static class LibFromTechJob extends Job
	{
		private Technology tech;

		public LibFromTechJob(Technology tech)
		{
			super("Make Technology Library from Technology", User.tool, Job.Type.CHANGE, null, null, Job.Priority.USER);
			this.tech = tech;
			startJob();
		}

		public boolean doIt()
		{
	        Library lib = us_tecedmakelibfromtech(tech);
	        if (lib == null) return false;

	        // switch to the library and show a cell
	        lib.setCurrent();
			return true;
		}
	}

	/*
	 * convert technology "tech" into a library and return that library.
	 * Returns NOLIBRARY on error
	 */
	static Library us_tecedmakelibfromtech(Technology tech)
	{
//		REGISTER CHAR *lay, *dxf, **sequence, **varnames, *fname, *gds;
//		REGISTER INTBIG i, j, k, e, xs, ys, oldlam, *newmap, *mapptr, *minnodesize,
//			tcon, func, nodexpos, bits, layertotal, arctotal, nodetotal, multicutsep,
//			height3d, thick3d, lambda, min2x, min2y, nlx, nhx, nly, nhy, wid, xoff,
//			*printcolors, *colors;
//		INTBIG lx, hx, ly, hy, xpos[4], ypos[4], xsc[4], ysc[4], lxo, hxo, lyo, hyo,
//			lxp, hxp, lyp, hyp, blx, bhx, bly, bhy;
//		REGISTER BOOLEAN serp, square, wipes, lockable, first;
//		float spires, spicap, spiecap;
//		CHAR gdsbuf[50];
//		REGISTER void *infstr;
//		REGISTER NODEPROTO *np, **nplist, *pnp, **aplist;
//		REGISTER VARIABLE *var, *var2, *var3, *var5, *var6, *var7, *var8,
//			*var10, *var11, *varred, *vargreen, *varblue;
//		REGISTER LIBRARY *lib;
//		REGISTER NODEINST *ni, *oni, *nni;
//		REGISTER ARCINST *ai;
//		REGISTER PORTPROTO *pp, *opp;
//		REGISTER ARCPROTO *ap;
//		REGISTER GRAPHICS *desc;
//		static POLYGON *poly = NOPOLYGON;
//		REGISTER DRCRULES *rules;
//		REGISTER TECH_POLYGON *ll;
//		REGISTER TECH_NODES *techn;
//		REGISTER TECH_COLORMAP *colmap;
//		NODEINST node;
//		ARCINST arc;

		Library lib = Library.newInstance(tech.getTechName(), null);
		if (lib == null)
		{
			System.out.println("Cannot create library " + tech.getTechName());
			return null;
		}
		System.out.println("Created library " + tech.getTechName() + "...");
	
		// create the information node
		Cell fNp = Cell.makeInstance(lib, "factors");
		if (fNp == null) return null;
		fNp.setInTechnologyLibrary();
	
//		// modify this technology's lambda value to match the current one
//		oldlam = lib.lambda[tech.techindex];
//		lambda = lib.lambda[art_tech.techindex];
//		lib.lambda[tech.techindex] = lambda;
	
		// create the miscellaneous info cell (called "factors")
		us_tecedmakeinfo(fNp, tech.getTechDesc());
	
		// copy any miscellaneous variables and make a list of their names
		int varCount = 0;
//		for(int i=0; i<us_knownvars.length; i++)
//		{
//			us_knownvars[i].ival = 0;
//			Variable var = tech.getVar(us_knownvars[i].varname);
//			if (var == null) continue;
//			us_knownvars[i].ival = 1;
//			varCount++;
//			lib.newVar(us_knownvars[i].varname, var.getObject());
//		}
		if (varCount > 0)
		{
			String [] varnames = new String[varCount];
			varCount = 0;
			for(int i=0; i<us_knownvars.length; i++)
				if (us_knownvars[i].ival != 0) varnames[varCount++] = us_knownvars[i].varname;
			lib.newVar("EDTEC_variable_list", varnames);
		}
	
		// create the layer node names
		int layertotal = tech.getNumLayers();
		HashMap layerCells = new HashMap();
	
		// create the layer nodes
		System.out.println("Creating the layers...");
		for(int i=0; i<layertotal; i++)
		{
			Layer layer = tech.getLayer(i);
			EGraphics desc = layer.getGraphics();
			String fname = "layer-" + layer.getName();
	
			// make sure the layer doesn't exist
			if (lib.findNodeProto(fname) != null)
			{
				System.out.println("Warning: multiple layers named '" + fname + "'");
				break;
			}

			Cell lNp = Cell.makeInstance(lib, fname);
			if (lNp == null) return null;
			lNp.setInTechnologyLibrary();
			layerCells.put(layer, lNp);

			LayerInfo li = new LayerInfo();
			li.fun = layer.getFunction();
			li.desc = desc;

			// compute foreign file formats
			li.cif = layer.getCIFLayer();
			li.gds = layer.getGDSLayer();
			li.dxf = layer.getDXFLayer();
	
			// compute the SPICE information
			li.spires = layer.getResistance();
			li.spicap = layer.getCapacitance();
			li.spiecap = layer.getEdgeCapacitance();
	
			// compute the 3D information
			li.height3d = layer.getDepth();
			li.thick3d = layer.getThickness();
	
			// build the layer cell
			li.us_tecedmakelayer(lNp);
		}
	
		// save the layer sequence
		String [] layerSequence = new String[layertotal];
		int layIndex = 0;
		for(Iterator it = lib.getCells(); it.hasNext(); )
		{
			Cell lNp = (Cell)it.next();
			if (lNp.getName().startsWith("layer-"))
				layerSequence[layIndex++] = lNp.getName();
		}
		lib.newVar("EDTEC_layersequence", layerSequence);
	
		// create the arc cells
		System.out.println("Creating the arcs...");
		int arctotal = 0;
		for(Iterator it = tech.getArcs(); it.hasNext(); )
		{
			PrimitiveArc ap = (PrimitiveArc)it.next();
//			ap.temp1 = (INTBIG)NONODEPROTO;
			if (ap.isNotUsed()) continue;
			String fname = "arc-" + ap.getName();
	
			// make sure the arc doesn't exist
			if (lib.findNodeProto(fname) != null)
			{
				System.out.println("Warning: multiple arcs named '" + fname + "'");
				break;
			}
	
			Cell aNp = Cell.makeInstance(lib, fname);
			if (aNp == null) return null;
			aNp.setInTechnologyLibrary();
//			ap.temp1 = (INTBIG)aNp;
//			var = getvalkey((INTBIG)ap, VARCPROTO, VINTEGER, us_arcstylekey);
//			if (var != NOVARIABLE) bits = var.addr; else
//				bits = ap.userbits;
			us_tecedmakearc(aNp, ap.getFunction(), ap.isFixedAngle(), ap.isWipable(), ap.isExtended(), ap.getAngleIncrement());
	
			// now create the arc layers
			double wid = ap.getDefaultWidth() - ap.getWidthOffset();
            ArcInst ai = ArcInst.makeDummyInstance(ap, wid*4);
            Poly [] polys = tech.getShapeOfArc(ai);
			double xoff = wid*2 + wid/2 + ap.getWidthOffset()/2;
            for(int i=0; i<polys.length; i++)
			{
            	Poly poly = polys[i];
            	Layer arcLayer = poly.getLayer();
            	if (arcLayer == null) continue;
            	EGraphics arcDesc = arcLayer.getGraphics();
//				if (arcDesc.bits == LAYERN) continue;
	
				// scale the arc geometry appropriately
            	Point2D [] points = poly.getPoints();
				for(int k=0; k<points.length; k++)
					points[k] = new Point2D.Double(points[k].getX() - xoff - 40000/SCALEALL, points[k].getY() - 10000/SCALEALL);
	
				// create the node to describe this layer
				NodeInst ni = us_tecedplacegeom(poly, aNp);
				if (ni == null) continue;
	
				// get graphics for this layer
				Create.us_teceditsetpatch(ni, arcDesc);
				Cell layerCell = (Cell)layerCells.get(arcLayer);
				if (layerCell != null) ni.newVar(EDTEC_LAYER, layerCell);
				ni.newVar(EDTEC_OPTION, new Integer(LAYERPATCH));
			}
            double i = ai.getProto().getWidthOffset() / 2;
			NodeInst ni = NodeInst.makeInstance(Artwork.tech.boxNode, new Point2D.Double(-40000/SCALEALL - wid*2.5 - i, -10000/SCALEALL), wid*5, wid, aNp);
			if (ni == null) return null;
			ni.newVar(Artwork.ART_COLOR, new Integer(EGraphics.WHITE));
			ni.newVar(EDTEC_LAYER, null);
			ni.newVar(EDTEC_OPTION, new Integer(LAYERPATCH));
			arctotal++;

			// compact it accordingly
//			us_tecedcompact(aNp);
		}
	
		// save the arc sequence
		String [] arcSequence = new String[arctotal];
		int arcIndex = 0;
		for(Iterator it = tech.getArcs(); it.hasNext(); )
		{
			ArcProto ap = (ArcProto)it.next();
			if (ap.isNotUsed()) continue;
			arcSequence[arcIndex++] = ap.getName();
		}
		lib.newVar("EDTEC_arcsequence", arcSequence);
	
		// create the node cells
		System.out.println("Creating the nodes...");
		int nodetotal = 0;
		for(Iterator it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode pnp = (PrimitiveNode)it.next();
			if (!pnp.isNotUsed()) nodetotal++;
		}
		double [] minnodesize = new double[nodetotal * 2];
		String [] nodeSequence = new String[nodetotal];
		int nodeIndex = 0;
		for(Iterator it = tech.getNodes(); it.hasNext(); )
		{
			PrimitiveNode pnp = (PrimitiveNode)it.next();
//			pnp.temp1 = 0;
			if (pnp.isNotUsed()) continue;
			nodeSequence[nodeIndex++] = pnp.getName();
			boolean first = true;
	
			// create the node layers
            NodeInst oNi = NodeInst.makeDummyInstance(pnp);
			double xs = pnp.getDefWidth() * 2;
			double ys = pnp.getDefHeight() * 2;
			if (xs < 3) xs = 3;
			if (ys < 3) ys = 3;
			double nodexpos = -xs*2;
			Point2D [] pos = new Point2D[4];
			pos[0] = new Point2D.Double(nodexpos - xs, -10000/SCALEALL + ys);
			pos[1] = new Point2D.Double(nodexpos + xs, -10000/SCALEALL + ys);
			pos[2] = new Point2D.Double(nodexpos - xs, -10000/SCALEALL - ys);
			pos[3] = new Point2D.Double(nodexpos + xs, -10000/SCALEALL - ys);

			SizeOffset so = pnp.getProtoSizeOffset();
			xs = pnp.getDefWidth() - so.getLowXOffset() - so.getHighXOffset();
			ys = pnp.getDefHeight() - so.getLowYOffset() - so.getHighYOffset();
			double [] xsc = new double[4];
			double [] ysc = new double[4];
			xsc[0] = xs*1;   ysc[0] = ys*1;
			xsc[1] = xs*2;   ysc[1] = ys*1;
			xsc[2] = xs*1;   ysc[2] = ys*2;
			xsc[3] = xs*2;   ysc[3] = ys*2;
	
			// for multicut contacts, make large size be just right for 2 cuts
			if (pnp.getSpecialType() == PrimitiveNode.MULTICUT)
			{
				double [] values = pnp.getSpecialValues();
				double min2x = values[0]*2 + values[2]*2 + values[4];
				double min2y = values[1]*2 + values[3]*2 + values[4];
				xsc[1] = min2x;
				xsc[3] = min2x;
				ysc[2] = min2y;
				ysc[3] = min2y;
			}
			Cell nNp = null;
			for(int e=0; e<4; e++)
			{
				// do not create node if main example had no polygons
				if (e != 0 && first) continue;
	
				// square nodes have only two examples
				if (pnp.isSquare() && (e == 1 || e == 2)) continue;
				double dX = pos[e].getX() - oNi.getAnchorCenterX();
				double dY = pos[e].getY() - oNi.getAnchorCenterY();
				double dXSize = xsc[e] + so.getLowXOffset() + so.getHighXOffset() - oNi.getXSize();
				double dYSize = ysc[e] + so.getLowYOffset() + so.getHighYOffset() - oNi.getYSize();
				oNi.lowLevelModify(dX, dY, dXSize, dYSize, 0);
				Poly [] polys = tech.getShapeOfNode(oNi);
				int j = polys.length;
				for(int i=0; i<j; i++)
				{
					Poly poly = polys[i];
					Layer nodeLayer = poly.getLayer();
					if (nodeLayer == null) continue;
					EGraphics desc = nodeLayer.getGraphics();
//					if (desc.bits == LAYERN) continue;

					// accumulate total size of main example
//					if (e == 0)
//					{
//						getbbox(poly, &blx, &bhx, &bly, &bhy);
//						if (i == 0)
//						{
//							nlx = blx;   nhx = bhx;
//							nly = bly;   nhy = bhy;
//						} else
//						{
//							if (blx < nlx) nlx = blx;
//							if (bhx > nhx) nhx = bhx;
//							if (bly < nly) nly = bly;
//							if (bhy > nhy) nhy = bhy;
//						}
//					}
	
					// create the node cell on the first valid layer
					if (first)
					{
						first = false;
						String fName = "node-" + pnp.getName();
	
						// make sure the node doesn't exist
						if (lib.findNodeProto(fName) != null)
						{
							System.out.println("Warning: multiple nodes named '" + fName + "'");
							break;
						}

						nNp = Cell.makeInstance(lib, fName);
						if (nNp == null) return null;
						nNp.setInTechnologyLibrary();
						PrimitiveNode.Function func = pnp.getFunction();
						boolean serp = false;
						if ((func == PrimitiveNode.Function.TRANMOS || func == PrimitiveNode.Function.TRAPMOS || func == PrimitiveNode.Function.TRADMOS) &&
							pnp.isHoldsOutline()) serp = true;
						boolean square = pnp.isSquare();
						boolean wipes = pnp.isWipeOn1or2();
						boolean lockable = pnp.isLockedPrim();
						double multiCutSep = 0;
						if (pnp.getSpecialType() == PrimitiveNode.MULTICUT)
						{
							double [] values = pnp.getSpecialValues();
							multiCutSep = values[4];
						}
						us_tecedmakenode(nNp, func, serp, square, wipes, lockable, multiCutSep);
//						pnp.temp1 = (INTBIG)nNp;
					}

					// create the node to describe this layer
					NodeInst ni = us_tecedplacegeom(poly, nNp);
					if (ni == null) return null;

					// get graphics for this layer
					Create.us_teceditsetpatch(ni, desc);
					Cell layerCell = (Cell)layerCells.get(nodeLayer);
					if (layerCell != null) ni.newVar(EDTEC_LAYER, layerCell);
					ni.newVar(EDTEC_OPTION, new Integer(LAYERPATCH));
	
					// set minimum polygon factor on smallest example
					if (e != 0) continue;
//					if (i >= tech.nodeprotos[pnp.primindex-1].layercount) continue;
//					ll = tech.nodeprotos[pnp.primindex-1].layerlist;
//					if (ll == 0) continue;
//					if (ll[i].representation != MINBOX) continue;
//					var = setval((INTBIG)ni, VNODEINST, x_("EDTEC_minbox"), (INTBIG)x_("MIN"), VSTRING|VDISPLAY);
//					if (var != NOVARIABLE)
//						defaulttextsize(3, var.textdescript);
				}
				if (first) continue;
	
				// create the highlight node
				NodeInst ni = NodeInst.makeInstance(Artwork.tech.boxNode, pos[e], xsc[e], ysc[e], nNp);
				if (ni == null) return null;
				ni.newVar(Artwork.ART_COLOR, new Integer(EGraphics.makeIndex(Color.WHITE)));
				ni.newVar(EDTEC_LAYER, null);
				ni.newVar(EDTEC_OPTION, new Integer(LAYERPATCH));
	
				// create a grab node (only in main example)
//				if (e == 0)
//				{
//					var = getvalkey((INTBIG)pnp, VNODEPROTO, VINTEGER|VISARRAY, el_prototype_center_key);
//					if (var != NOVARIABLE)
//					{
//						lx = hx = xpos[0] + ((INTBIG *)var.addr)[0];
//						ly = hy = ypos[0] + ((INTBIG *)var.addr)[1];
//						lx = muldiv(lx, lambda, oldlam);
//						hx = muldiv(hx, lambda, oldlam);
//						ly = muldiv(ly, lambda, oldlam);
//						hy = muldiv(hy, lambda, oldlam);
//						nodeprotosizeoffset(gen_cellcenterprim, &lxo, &lyo, &hxo, &hyo, np);
//						ni = newnodeinst(gen_cellcenterprim, lx-lxo, hx+hxo, ly-lyo, hy+hyo, 0, 0, np);
//						if (ni == null) return(NOLIBRARY);
//					}
//				}
	
				// also draw ports
//				for(pp = pnp.firstportproto; pp != NOPORTPROTO; pp = pp.nextportproto)
//				{
//					shapeportpoly(oni, pp, poly, FALSE);
//					getbbox(poly, &lx, &hx, &ly, &hy);
//					lx = muldiv(lx, lambda, oldlam);
//					hx = muldiv(hx, lambda, oldlam);
//					ly = muldiv(ly, lambda, oldlam);
//					hy = muldiv(hy, lambda, oldlam);
//					nodeprotosizeoffset(gen_portprim, &lxo, &lyo, &hxo, &hyo, np);
//					ni = newnodeinst(gen_portprim, lx-lxo, hx+hxo, ly-lyo, hy+hyo, 0, 0, np);
//					if (ni == null) return(NOLIBRARY);
//					pp.temp1 = (INTBIG)ni;
//					(void)setvalkey((INTBIG)ni, VNODEINST, us_edtec_option_key, LAYERPATCH, VINTEGER);
//					var = setval((INTBIG)ni, VNODEINST, x_("EDTEC_portname"), (INTBIG)pp.protoname,
//						VSTRING|VDISPLAY);
//					if (var != NOVARIABLE)
//						defaulttextsize(3, var.textdescript);
//					endobjectchange((INTBIG)ni, VNODEINST);
//	
//					// on the first sample, also show angle and connection
//					if (e != 0) continue;
//					if (((pp.userbits&PORTANGLE)>>PORTANGLESH) != 0 ||
//						((pp.userbits&PORTARANGE)>>PORTARANGESH) != 180)
//					{
//						(void)setval((INTBIG)ni, VNODEINST, x_("EDTEC_portangle"),
//							(pp.userbits&PORTANGLE)>>PORTANGLESH, VINTEGER);
//						(void)setval((INTBIG)ni, VNODEINST, x_("EDTEC_portrange"),
//							(pp.userbits&PORTARANGE)>>PORTARANGESH, VINTEGER);
//					}
//	
//					// add in the "local" port connections (from this tech)
//					for(tcon=i=0; pp.connects[i] != NOARCPROTO; i++)
//						if (pp.connects[i].tech == tech) tcon++;
//					if (tcon != 0)
//					{
//						aplist = (NODEPROTO **)emalloc((tcon * (sizeof (NODEPROTO *))), el_tempcluster);
//						if (aplist == 0) return(NOLIBRARY);
//						for(j=i=0; pp.connects[i] != NOARCPROTO; i++)
//						{
//							if (pp.connects[i].tech != tech) continue;
//							aplist[j] = (NODEPROTO *)pp.connects[i].temp1;
//							if (aplist[j] != NONODEPROTO) j++;
//						}
//						(void)setval((INTBIG)ni, VNODEINST, x_("EDTEC_connects"),
//							(INTBIG)aplist, VNODEPROTO|VISARRAY|(j<<VLENGTHSH));
//						efree((CHAR *)aplist);
//					}
//	
//					// connect the connected ports
//					for(opp = pnp.firstportproto; opp != pp; opp = opp.nextportproto)
//					{
//						if (opp.network != pp.network) continue;
//						nni = (NODEINST *)opp.temp1;
//						if (nni == null) continue;
//						if (newarcinst(gen_universalarc, 0, 0, ni, ni.proto.firstportproto,
//							(ni.highx+ni.lowx)/2, (ni.highy+ni.lowy)/2, nni,
//								nni.proto.firstportproto, (nni.highx+nni.lowx)/2,
//									(nni.highy+nni.lowy)/2, np) == NOARCINST) return(NOLIBRARY);
//						break;
//					}
//				}
			}
//			minnodesize[nodetotal*2] = (nhx - nlx) * WHOLE / lambda;
//			minnodesize[nodetotal*2+1] = (nhy - nly) * WHOLE / lambda;
			nodetotal++;
	
			// compact it accordingly
//			us_tecedcompact(np);
		}
	
		// save the node sequence
		lib.newVar("EDTEC_nodesequence", nodeSequence);
	
		// create the color map information
//		System.out.println("Adding color map and design rules...");
//		Variable var2 = getval((INTBIG)tech, VTECHNOLOGY, VCHAR|VISARRAY, x_("USER_color_map"));
//		Variable varred = getvalkey((INTBIG)us_tool, VTOOL, VINTEGER|VISARRAY, us_colormap_red_key);
//		Variable vargreen = getvalkey((INTBIG)us_tool, VTOOL, VINTEGER|VISARRAY, us_colormap_green_key);
//		Variable varblue = getvalkey((INTBIG)us_tool, VTOOL, VINTEGER|VISARRAY, us_colormap_blue_key);
//		if (varred != NOVARIABLE && vargreen != NOVARIABLE && varblue != NOVARIABLE &&
//			var2 != NOVARIABLE)
//		{
//			newmap = emalloc((256*3*SIZEOFINTBIG), el_tempcluster);
//			if (newmap == 0) return(NOLIBRARY);
//			mapptr = newmap;
//			colmap = (TECH_COLORMAP *)var2.addr;
//			for(i=0; i<256; i++)
//			{
//				*mapptr++ = ((INTBIG *)varred.addr)[i];
//				*mapptr++ = ((INTBIG *)vargreen.addr)[i];
//				*mapptr++ = ((INTBIG *)varblue.addr)[i];
//			}
//			for(i=0; i<32; i++)
//			{
//				newmap[(i<<2)*3]   = colmap[i].red;
//				newmap[(i<<2)*3+1] = colmap[i].green;
//				newmap[(i<<2)*3+2] = colmap[i].blue;
//			}
//			(void)setval((INTBIG)lib, VLIBRARY, x_("EDTEC_colormap"), (INTBIG)newmap,
//				VINTEGER|VISARRAY|((256*3)<<VLENGTHSH));
//			efree((CHAR *)newmap);
//		}
	
//		// create the design rule information
//		rules = dr_allocaterules(layertotal, nodetotal, tech.techname);
//		if (rules == NODRCRULES) return(NOLIBRARY);
//		for(i=0; i<layertotal; i++)
//			(void)allocstring(&rules.layernames[i], layername(tech, i), el_tempcluster);
//		i = 0;
//		for(np = tech.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//			if (np.temp1 != 0)
//				(void)allocstring(&rules.nodenames[i++],  &((NODEPROTO *)np.temp1).protoname[5],
//					el_tempcluster);
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, dr_min_widthkey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.numlayers; i++) rules.minwidth[i] = ((INTBIG *)var.addr)[i];
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VSTRING|VISARRAY, dr_min_width_rulekey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.numlayers; i++)
//				(void)reallocstring(&rules.minwidthR[i], ((CHAR **)var.addr)[i], el_tempcluster);
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, dr_connected_distanceskey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++) rules.conlist[i] = ((INTBIG *)var.addr)[i];
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VSTRING|VISARRAY, dr_connected_distances_rulekey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++)
//				(void)reallocstring(&rules.conlistR[i], ((CHAR **)var.addr)[i], el_tempcluster);
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, dr_unconnected_distanceskey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++) rules.unconlist[i] = ((INTBIG *)var.addr)[i];
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VSTRING|VISARRAY, dr_unconnected_distances_rulekey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++)
//				(void)reallocstring(&rules.unconlistR[i], ((CHAR **)var.addr)[i], el_tempcluster);
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, dr_connected_distancesWkey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++) rules.conlistW[i] = ((INTBIG *)var.addr)[i];
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VSTRING|VISARRAY, dr_connected_distancesW_rulekey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++)
//				(void)reallocstring(&rules.conlistWR[i], ((CHAR **)var.addr)[i], el_tempcluster);
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, dr_unconnected_distancesWkey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++) rules.unconlistW[i] = ((INTBIG *)var.addr)[i];
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VSTRING|VISARRAY, dr_unconnected_distancesW_rulekey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++)
//				(void)reallocstring(&rules.unconlistWR[i], ((CHAR **)var.addr)[i], el_tempcluster);
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, dr_connected_distancesMkey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++) rules.conlistM[i] = ((INTBIG *)var.addr)[i];
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VSTRING|VISARRAY, dr_connected_distancesM_rulekey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++)
//				(void)reallocstring(&rules.conlistMR[i], ((CHAR **)var.addr)[i], el_tempcluster);
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, dr_unconnected_distancesMkey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++) rules.unconlistM[i] = ((INTBIG *)var.addr)[i];
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VSTRING|VISARRAY, dr_unconnected_distancesM_rulekey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++)
//				(void)reallocstring(&rules.unconlistMR[i], ((CHAR **)var.addr)[i], el_tempcluster);
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, dr_edge_distanceskey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++) rules.edgelist[i] = ((INTBIG *)var.addr)[i];
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VSTRING|VISARRAY, dr_edge_distances_rulekey);
//		if (var != NOVARIABLE)
//			for(i=0; i<rules.utsize; i++)
//				(void)reallocstring(&rules.edgelistR[i], ((CHAR **)var.addr)[i], el_tempcluster);
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT, dr_wide_limitkey);
//		if (var != NOVARIABLE) rules.widelimit = var.addr;
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VFRACT|VISARRAY, dr_min_node_sizekey);
//		if (var != NOVARIABLE)
//		{
//			i = j = 0;
//			for(np = tech.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//			{
//				if (np.temp1 != 0)
//				{
//					rules.minnodesize[i*2] = ((INTBIG *)var.addr)[j*2];
//					rules.minnodesize[i*2+1] = ((INTBIG *)var.addr)[j*2+1];
//	
//					// if rule is valid, make sure it is no larger than actual size
//					if (rules.minnodesize[i*2] > 0 && rules.minnodesize[i*2+1] > 0)
//					{
//						if (rules.minnodesize[i*2] > minnodesize[i*2])
//							rules.minnodesize[i*2] = minnodesize[i*2];
//						if (rules.minnodesize[i*2+1] > minnodesize[i*2+1])
//							rules.minnodesize[i*2+1] = minnodesize[i*2+1];
//					}
//					i++;
//				}
//				j++;
//			}
//		}
//		var = getvalkey((INTBIG)tech, VTECHNOLOGY, VSTRING|VISARRAY, dr_min_node_size_rulekey);
//		if (var != NOVARIABLE)
//		{
//			i = j = 0;
//			for(np = tech.firstnodeproto; np != NONODEPROTO; np = np.nextnodeproto)
//			{
//				if (np.temp1 != 0)
//				{
//					reallocstring(&rules.minnodesizeR[i], ((CHAR **)var.addr)[j], el_tempcluster);
//					i++;
//				}
//				j++;
//			}
//		}
//	
//		us_tecedloaddrcmessage(rules, lib);
//		dr_freerules(rules);
	
		// clean up
		System.out.println("Done.");
//		efree((CHAR *)minnodesize);
//		lib.lambda[tech.techindex] = oldlam;
		return(lib);
	}
	
	/*************************** CELL CREATION HELPERS ***************************/
	
	/*
	 * Method to build the appropriate descriptive information for the information
	 * cell "np".
	 */
	private static void us_tecedmakeinfo(Cell np, String description)
	{
		// load up the structure with the current values
		for(int i=0; i < us_tecedmisctexttable.length; i++)
		{
			switch (us_tecedmisctexttable[i].funct)
			{
				case TECHLAMBDA:
					us_tecedmisctexttable[i].value = new Integer(1);
					break;
				case TECHDESCRIPT:
					us_tecedmisctexttable[i].value = description;
					break;
			}
		}
	
		// now create those text objects
		us_tecedcreatespecialtext(np, us_tecedmisctexttable);
	}

	/*
	 * Method to build the appropriate descriptive information for an arc into
	 * cell "np".  The function is in "func"; the arc is fixed-angle if "fixang"
	 * is nonzero; the arc wipes pins if "wipes" is nonzero; and the arc does
	 * not extend its ends if "noextend" is nonzero.  The angle increment is
	 * in "anginc".
	 */
	static void us_tecedmakearc(Cell np, ArcProto.Function func, boolean fixang, boolean wipes, boolean noextend, int anginc)
	{
		// load up the structure with the current values
		for(int i=0; i<us_tecedarctexttable.length; i++)
		{
			switch (us_tecedarctexttable[i].funct)
			{
				case ARCFUNCTION:
					us_tecedarctexttable[i].value = func;
					break;
				case ARCFIXANG:
					us_tecedarctexttable[i].value = new Boolean(fixang);
					break;
				case ARCWIPESPINS:
					us_tecedarctexttable[i].value = new Boolean(wipes);
					break;
				case ARCNOEXTEND:
					us_tecedarctexttable[i].value = new Boolean(noextend);
					break;
				case ARCINC:
					us_tecedarctexttable[i].value = new Integer(anginc);
					break;
			}
		}
	
		// now create those text objects
		us_tecedcreatespecialtext(np, us_tecedarctexttable);
	}
	
	/*
	 * Method to build the appropriate descriptive information for a node into
	 * cell "np".  The function is in "func", the serpentine transistor factor
	 * is in "serp", the node is square if "square" is true, the node
	 * is invisible on 1 or 2 arcs if "wipes" is true, and the node is lockable
	 * if "lockable" is true.
	 */
	static void us_tecedmakenode(Cell np, PrimitiveNode.Function func, boolean serp, boolean square, boolean wipes,
			boolean lockable, double multicutsep)
	{	
		// load up the structure with the current values
		for(int i=0; i < us_tecednodetexttable.length; i++)
		{
			switch (us_tecednodetexttable[i].funct)
			{
				case NODEFUNCTION:
					us_tecednodetexttable[i].value = func;
					break;
				case NODESERPENTINE:
					us_tecednodetexttable[i].value = new Boolean(serp);
					break;
				case NODESQUARE:
					us_tecednodetexttable[i].value = new Boolean(square);
					break;
				case NODEWIPES:
					us_tecednodetexttable[i].value = new Boolean(wipes);
					break;
				case NODELOCKABLE:
					us_tecednodetexttable[i].value = new Boolean(lockable);
					break;
				case NODEMULTICUT:
					us_tecednodetexttable[i].value = new Double(multicutsep);
					break;
			}
		}
	
		// now create those text objects
		us_tecedcreatespecialtext(np, us_tecednodetexttable);
	}
	
	/*
	 * Method to add the text corresponding to the layer function in "func"
	 * to the current infinite string
	 */
	static String us_tecedaddfunstring(int func)
	{
//		String res = us_teclayer_functions[func&LFTYPE].name;
//		for(int i=0; us_teclayer_functions[i].name != 0; i++)
//		{
//			if (us_teclayer_functions[i].value <= LFTYPE) continue;
//			if ((func&us_teclayer_functions[i].value) == 0) continue;
//			func &= ~us_teclayer_functions[i].value;
//			addtoinfstr(infstr, ',');
//			addstringtoinfstr(infstr, us_teclayer_functions[i].name);
//		}
		return "";
	}
	
	/*
	 * Method to create special text geometry described by "table" in cell "np".
	 */
	static void us_tecedcreatespecialtext(Cell np, SpecialTextDescr [] table)
	{
		us_tecedfindspecialtext(np, table);
		for(int i=0; i < table.length; i++)
		{
			NodeInst ni = table[i].ni;
			if (ni == null)
			{
				ni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, new Point2D.Double(table[i].x, table[i].y), 0, 0, np);
				if (ni == null) return;
				String str = null;
				switch (table[i].funct)
				{
					case TECHLAMBDA:
						str = "Lambda: " + ((Integer)table[i].value).intValue();
						break;
					case TECHDESCRIPT:
						str = "Description: " + (String)table[i].value;
						break;
					case LAYERSTYLE:
//						infstr = initinfstr();
//						addstringtoinfstr(infstr, TECEDNODETEXTSTYLE);
//						if (((INTBIG)table[i].value&NATURE) == SOLIDC) addstringtoinfstr(infstr, x_("solid")); else
//						{
//							if (((INTBIG)table[i].value&OUTLINEPAT) != 0)
//								addstringtoinfstr(infstr, x_("patterned/outlined")); else
//									addstringtoinfstr(infstr, x_("patterned"));
//						}
//						allocstring(&allocatedname, returninfstr(infstr), el_tempcluster);
						str = TECEDNODETEXTSTYLE + "solid";
						break;
					case LAYERCIF:
						str = TECEDNODETEXTCIF + (String)table[i].value;
						break;
					case LAYERDXF:
						str = TECEDNODETEXTDXF + (String)table[i].value;
						break;
					case LAYERGDS:
						str = TECEDNODETEXTGDS + (String)table[i].value;
						break;
					case LAYERFUNCTION:
						str = TECEDNODETEXTFUNCTION + ((Layer.Function)table[i].value).toString();
						break;
					case LAYERLETTERS:
						str = TECEDNODETEXTLETTERS + (String)table[i].value;
						break;
					case LAYERSPIRES:
						str = TECEDNODETEXTSPICERES + ((Double)table[i].value).doubleValue();
						break;
					case LAYERSPICAP:
						str = TECEDNODETEXTSPICECAP + ((Double)table[i].value).doubleValue();
						break;
					case LAYERSPIECAP:
						str = TECEDNODETEXTSPICEECAP + ((Double)table[i].value).doubleValue();
						break;
					case LAYER3DHEIGHT:
						str = TECEDNODETEXT3DHEIGHT + ((Double)table[i].value).doubleValue();
						break;
					case LAYER3DTHICK:
						str = TECEDNODETEXT3DTHICK + ((Double)table[i].value).doubleValue();
						break;
					case LAYERPRINTCOL:
						str = TECEDNODETEXTPRINTCOL + "r,g,b, o,off";
//						infstr = initinfstr();
//						pc = (INTBIG *)table[i].value;
//						formatinfstr(infstr, x_("%s%ld,%ld,%ld, %ld,%s"), TECEDNODETEXTPRINTCOL,
//							pc[0], pc[1], pc[2], pc[3], (pc[4]==0 ? x_("off") : x_("on")));
//						allocstring(&allocatedname, returninfstr(infstr), el_tempcluster);
//						str = allocatedname;
						break;
					case ARCFUNCTION:
						str = TECEDNODETEXTFUNCTION + ((ArcProto.Function)table[i].value).toString();
						break;
					case ARCFIXANG:
						str = "Fixed-angle: " + (((Boolean)table[i].value).booleanValue() ? "Yes" : "No");
						break;
					case ARCWIPESPINS:
						str = "Wipes pins: "  + (((Boolean)table[i].value).booleanValue() ? "Yes" : "No");
						break;
					case ARCNOEXTEND:
						str = "Extend arcs: " + (((Boolean)table[i].value).booleanValue() ? "Yes" : "No");
						break;
					case ARCINC:
						str = "Angle increment: " + ((Integer)table[i].value).intValue();
						break;
					case NODEFUNCTION:
						str = TECEDNODETEXTFUNCTION + ((PrimitiveNode.Function)table[i].value).toString();
						break;
					case NODESERPENTINE:
						str = "Serpentine transistor: " + (((Boolean)table[i].value).booleanValue() ? "Yes" : "No");
						break;
					case NODESQUARE:
						str = "Square node: " + (((Boolean)table[i].value).booleanValue() ? "Yes" : "No");
						break;
					case NODEWIPES:
						str = "Invisible with 1 or 2 arcs: " + (((Boolean)table[i].value).booleanValue() ? "Yes" : "No");
						break;
					case NODELOCKABLE:
						str = "Lockable: " + (((Boolean)table[i].value).booleanValue() ? "Yes" : "No");
						break;
					case NODEMULTICUT:
						str = "Multicut separation: " + ((Double)table[i].value).doubleValue();
						break;
				}
				Variable var = ni.newVar(Artwork.ART_MESSAGE, str);
				if (var != null)
					var.setDisplay(true);
				var = ni.newVar(EDTEC_OPTION, new Integer(table[i].funct));
			}
		}
	}
	
	/**
	 * Method to locate the nodes with the special node-cell text.  In cell "np", finds
	 * the relevant text nodes in "table" and loads them into the structure.
	 */
	static void us_tecedfindspecialtext(Cell np, SpecialTextDescr [] table)
	{
		// clear the node assignments
		for(int i=0; i < table.length; i++)
			table[i].ni = null;
	
		// determine the number of special texts here
		for(Iterator it = np.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			Variable var = ni.getVar(EDTEC_OPTION);
			if (var == null) continue;
			int opt = ((Integer)var.getObject()).intValue();
			for(int i=0; i < table.length; i++)
			{
				if (opt != table[i].funct) continue;
				table[i].ni = ni;
				break;
			}
		}
	}
	
	static NodeInst us_tecedplacegeom(Poly poly, Cell np)
	{
		Rectangle2D box = poly.getBox();
		Poly.Type style = poly.getStyle();
		if (style == Poly.Type.FILLED)
		{
			if (box != null)
			{
				return NodeInst.makeInstance(Artwork.tech.filledBoxNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
					box.getWidth(), box.getHeight(), np);
			} else
			{
				box = poly.getBounds2D();
				NodeInst nni = NodeInst.makeInstance(Artwork.tech.filledPolygonNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
					box.getWidth(), box.getHeight(), np);
				if (nni == null) return null;
				nni.setTrace(poly.getPoints());
				return nni;
			}
		}
		if (style == Poly.Type.CLOSED)
		{
			if (box != null)
			{
				return NodeInst.makeInstance(Artwork.tech.boxNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
					box.getWidth(), box.getHeight(), np);
			} else
			{
				box = poly.getBounds2D();
				NodeInst nni = NodeInst.makeInstance(Artwork.tech.closedPolygonNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
					box.getWidth(), box.getHeight(), np);
				if (nni == null) return null;
				nni.setTrace(poly.getPoints());
				return nni;
			}
		}
		if (style == Poly.Type.CROSSED)
		{
			if (box == null) box = poly.getBounds2D();
			NodeInst nni = NodeInst.makeInstance(Artwork.tech.crossedBoxNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			return nni;
		}
		if (style == Poly.Type.OPENED)
		{
			NodeInst nni = NodeInst.makeInstance(Artwork.tech.openedPolygonNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			nni.setTrace(poly.getPoints());
			return nni;
		}
		if (style == Poly.Type.OPENEDT1)
		{
			NodeInst nni = NodeInst.makeInstance(Artwork.tech.openedDottedPolygonNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			nni.setTrace(poly.getPoints());
			return nni;
		}
		if (style == Poly.Type.OPENEDT2)
		{
			NodeInst nni = NodeInst.makeInstance(Artwork.tech.openedDashedPolygonNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			nni.setTrace(poly.getPoints());
			return nni;
		}
		if (style == Poly.Type.OPENEDT3)
		{
			NodeInst nni = NodeInst.makeInstance(Artwork.tech.openedThickerPolygonNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			nni.setTrace(poly.getPoints());
			return nni;
		}
		if (style == Poly.Type.CIRCLE)
		{
			NodeInst nni = NodeInst.makeInstance(Artwork.tech.circleNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			return nni;
		}
		if (style == Poly.Type.THICKCIRCLE)
		{
			NodeInst nni = NodeInst.makeInstance(Artwork.tech.thickCircleNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			return nni;
		}
		if (style == Poly.Type.DISC)
		{
			NodeInst nni = NodeInst.makeInstance(Artwork.tech.filledCircleNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			return nni;
		}
		if (style == Poly.Type.CIRCLEARC)
		{
			NodeInst nni = NodeInst.makeInstance(Artwork.tech.circleNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			nni.setArcDegrees(0.0, 45.0*Math.PI/180.0);
			return nni;
		}
		if (style == Poly.Type.THICKCIRCLEARC)
		{
			NodeInst nni = NodeInst.makeInstance(Artwork.tech.thickCircleNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			nni.setArcDegrees(0.0, 45.0*Math.PI/180.0);
			return nni;
		}
		if (style == Poly.Type.TEXTCENT)
		{
			NodeInst nni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			Variable var = nni.newVar(Artwork.ART_MESSAGE, poly.getString());
			if (var != null)
			{
				var.setDisplay(true);
				var.setPos(TextDescriptor.Position.CENT);
			}
			return nni;
		}
		if (style == Poly.Type.TEXTBOTLEFT)
		{
			NodeInst nni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			Variable var = nni.newVar(Artwork.ART_MESSAGE, poly.getString());
			if (var != null)
			{
				var.setDisplay(true);
				var.setPos(TextDescriptor.Position.UPRIGHT);
			}
			return nni;
		}
		if (style == Poly.Type.TEXTBOTRIGHT)
		{
			NodeInst nni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			Variable var = nni.newVar(Artwork.ART_MESSAGE, poly.getString());
			if (var != null)
			{
				var.setDisplay(true);
				var.setPos(TextDescriptor.Position.UPLEFT);
			}
			return nni;
		}
		if (style == Poly.Type.TEXTBOX)
		{
			NodeInst nni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, new Point2D.Double(box.getCenterX(), box.getCenterY()),
				box.getWidth(), box.getHeight(), np);
			if (nni == null) return null;
			Variable var = nni.newVar(Artwork.ART_MESSAGE, poly.getString());
			if (var != null)
			{
				var.setDisplay(true);
				var.setPos(TextDescriptor.Position.BOXED);
			}
			return nni;
		}
		return(null);
	}
}
