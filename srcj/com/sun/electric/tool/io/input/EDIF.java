/* -*- tab-width: 4 -*-
*
* Electric(tm) VLSI Design System
*
* File: EDIF.java
* Input/output tool: EDIF 2.0.0 input
* Original EDIF reader by Glen Lawson.
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
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.io.IOTool;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

/**
 * This class reads files in EDIF files.
 * <BR>
 * Notes:
 *	I have tried EDIF files from CADENCE and VALID only.
 *	Does not fully support portbundles
 *	Multiple ports of the same name are named port_x (x is 1 to n duplicate)
 *	Keywords such as ARRAY have unnamed parameters, ie (array (name..) 5 6)
 *	this is handled in the io_edprocess_integer function called by io_edget_keyword,
 *	this is a hack to fix this problem, a real table driven parser should be used.
 *	Use circle arcs instead of splines.
 *	Support text justifications and text height
 * 	Better NAME/RENAME/STRINGDISPLAY/ANNOTATE text handling.
 *  ANSI prototypes
 *	Changed arcs to simple polygons plus ARC attribute
 *  Can read NETLIST views
 */
public class EDIF extends Input
{
	private static final double INCH = 10;
	private static final int MAXLAYERS = 256;

	private static class TEXTJUST {}
	private static final TEXTJUST UPPERLEFT    = new TEXTJUST();
	private static final TEXTJUST UPPERCENTER  = new TEXTJUST();
	private static final TEXTJUST UPPERRIGHT   = new TEXTJUST();
	private static final TEXTJUST CENTERLEFT   = new TEXTJUST();
	private static final TEXTJUST CENTERCENTER = new TEXTJUST();
	private static final TEXTJUST CENTERRIGHT  = new TEXTJUST();
	private static final TEXTJUST LOWERLEFT    = new TEXTJUST();
	private static final TEXTJUST LOWERCENTER  = new TEXTJUST();
	private static final TEXTJUST LOWERRIGHT   = new TEXTJUST();

	// name table for layers
	private static class NAMETABLE
	{
		String original;					/* the original MASK layer */
		String replace;					/* the replacement layer */
		NodeProto node;				/* the basic electric node */
		ArcProto arc;					/* the basic arc type */
		int textheight;					/* default text height */
		TEXTJUST justification;			/* default justification */
		boolean visible; 					/* layer is visible */
	};

	// Edif viewtypes ...
	private static class VTYPES {}
	private static final VTYPES VNULL       = new VTYPES();
	private static final VTYPES VBEHAVIOR   = new VTYPES();
	private static final VTYPES VDOCUMENT   = new VTYPES();
	private static final VTYPES VGRAPHIC    = new VTYPES();
	private static final VTYPES VLOGICMODEL = new VTYPES();
	private static final VTYPES VMASKLAYOUT = new VTYPES();
	private static final VTYPES VNETLIST    = new VTYPES();
	private static final VTYPES VPCBLAYOUT  = new VTYPES();
	private static final VTYPES VSCHEMATIC  = new VTYPES();
	private static final VTYPES VSTRANGER   = new VTYPES();
	private static final VTYPES VSYMBOLIC   = new VTYPES();
	private static final VTYPES VSYMBOL     = new VTYPES();	/* not a real EDIF view, but electric has one */

	// Edif geometry types ...
	private static class GTYPES {}
	private static final GTYPES GUNKNOWN   = new GTYPES();
	private static final GTYPES GRECTANGLE = new GTYPES();
	private static final GTYPES GPOLYGON   = new GTYPES();
	private static final GTYPES GSHAPE     = new GTYPES();
	private static final GTYPES GOPENSHAPE = new GTYPES();
	private static final GTYPES GTEXT      = new GTYPES();
	private static final GTYPES GPATH      = new GTYPES();
	private static final GTYPES GINSTANCE  = new GTYPES();
	private static final GTYPES GCIRCLE    = new GTYPES();
	private static final GTYPES GARC       = new GTYPES();
	private static final GTYPES GPIN       = new GTYPES();
	private static final GTYPES GNET       = new GTYPES();
	private static final GTYPES GBUS       = new GTYPES();

	// 8 standard orientations
	private static class OTYPES {}
	private static final OTYPES OUNKNOWN = new OTYPES();
	private static final OTYPES OR0      = new OTYPES();
	private static final OTYPES OR90     = new OTYPES();
	private static final OTYPES OR180    = new OTYPES();
	private static final OTYPES OR270    = new OTYPES();
	private static final OTYPES OMX      = new OTYPES();
	private static final OTYPES OMY      = new OTYPES();
	private static final OTYPES OMYR90   = new OTYPES();
	private static final OTYPES OMXR90   = new OTYPES();

	private static class EPT
	{
		double x, y;
		EPT nextpt;
	};

	private static class EDPORTDIR {}
	private static final EDPORTDIR INOUT   = new EDPORTDIR();
	private static final EDPORTDIR INPUTE  = new EDPORTDIR();
	private static final EDPORTDIR OUTPUTE = new EDPORTDIR();

	private static class EDPORT
	{
		String name;
		String reference;
		EDPORTDIR direction;
		int arrayx, arrayy;
		EDPORT next;
	};

	private static class EDNETPORT
	{
		NodeInst ni;					/* unique node port is attached to */
		PortProto pp;					/* port type (no portarc yet) */
		int member;						/* member for an array */
		EDNETPORT next;		/* next common to a net */
	}
	
	private static class EDVENDOR {}
	private static final EDVENDOR EVUNKNOWN    = new EDVENDOR();
	private static final EDVENDOR EVCADENCE    = new EDVENDOR();
	private static final EDVENDOR EVVALID      = new EDVENDOR();
	private static final EDVENDOR EVSYNOPSYS   = new EDVENDOR();
	private static final EDVENDOR EVMENTOR     = new EDVENDOR();
	private static final EDVENDOR EVVIEWLOGIC  = new EDVENDOR();

	private static class PROPERTY_TYPE {}
	private static final PROPERTY_TYPE PUNKNOWN      = new PROPERTY_TYPE();
	private static final PROPERTY_TYPE PSTRING       = new PROPERTY_TYPE();
	private static final PROPERTY_TYPE PINTEGER      = new PROPERTY_TYPE();
	private static final PROPERTY_TYPE PNUMBER       = new PROPERTY_TYPE();
	
	private static class EDPROPERTY
	{
		String name;
//		Object val;
//		union
//		{
			int integer;
			float number;
			String string;
//		} val;
		PROPERTY_TYPE type;
		EDPROPERTY next;
	}

	/* view information ... */
	private int pageno;						/* the schematic page number */
	private VTYPES active_view;				/* indicates we are in a NETLIST view */
	private EDVENDOR vendor;				/* the current vendor type */
	
	/* parser variables ... */
	private EDIFKEY state;			    	/* the current parser state */
	private String buffer;         	/* the read buffer */
	private int pos;                   	/* the position within the buffer */
	private boolean ignoreblock;				/* no update flag */
	private int errors, warnings;			/* load status */
	
	/* electric context data ... */
	private Library library;				/* the new library */
	private Technology technology;			/* the active technology */
	private Cell current_cell;		/* the current active cell */
	private NodeInst current_node;			/* the current active instance */
	private ArcInst current_arc;			/* the current active arc */
	private NodeProto figure_group;		/* the current figure group node */
	private ArcProto arc_type;				/* the current (if exists) arc type */
	private NodeProto cellRefProto;				/* the cellRef type */
	private PortProto current_port;		/* the current port proto */
	
	/* general geometry information ... */
	private GTYPES geometry;				/* the current geometry type */
	private List points;					/* the list of points */
	private OTYPES orientation;				/* the orientation of the structure */
	private EDPORTDIR direction;			/* port direction */
	
	/* geometric path constructors ... */
	private int path_width;					/* the width of the path */
	private boolean extend_end;              	/* extend path end flag */
	
	/* array variables ... */
	private boolean isarray;					/* set if truely an array */
	private int arrayx, arrayy;				/* the bounds of the array */
	private double deltaxX, deltaxY;			/* offsets in x and y for an X increment */
	private double deltayX, deltayY;			/* offsets in x and y for an Y increment */
	private int deltapts;					/* which offset flag */
	private int memberx, membery;			/* the element of the array */

	/* text variables ... */
	private String string;			/* Text string */
	private int textheight;					/* the current default text height */
	private TEXTJUST justification;			/* the current text justificaton */
	private boolean visible;					/* is stringdisplay visible */
//	struct {						/* save block for name and rename strings */
		private String save_string;		/* the saved string, if NULL not saved */
		private List save_points;				/* origin x and y */
		private int save_textheight;				/* the height of text */
		private boolean save_visible;				/* visiblity of the text */
		private TEXTJUST save_justification; 	/* justification of the text */
		private OTYPES save_orientation;			/* orientation of the text */
//	} save_text;
	
	/* technology data ... */
	private double scale;					/* scaling value */

	/* current name and rename of EDIF objects ... */
	private String cell_reference;	/* the current cell name */
	private String cell_name;			/* the current cell name (original) */
	private String port_reference;	/* the current port name */
	private String port_name;			/* the current port name (original) */
	private String instance_reference; /* the current instance name */
	private String instance_name;		/* the current instance name (original) */
	private String bundle_reference;	/* the current bundle name */
	private String bundle_name;		/* the current bundle name (original) */
	private String net_reference;		/* the current net name */
	private String net_name;			/* the current net name (original) */
	private String property_reference; /* the current property name */
	private String property_name;		/* the current property name (original) */

	private PROPERTY_TYPE ptype;			/* the type of property */
	private String pval_string;				/* string buffer */
	private int pval_integer;				/* integer buffer */
	private double pval_number;				/* number buffer */
	
	private String name;				/* the name of the object */
	private String original;			/* the original name of the object */

	/* layer or figure information ... */
	private int layer_ptr;					/* pointer to layer table entry */
	private NAMETABLE [] nametbl = new NAMETABLE[MAXLAYERS]; /* the current name mapping table */
	private NAMETABLE cur_nametbl; 		/* the current name table entry */
	
	/* cell name lookup (from rename) */
	private HashMap celltbl;
	
	/* port data for port exporting */
	private EDPORT ports;				/* active port list */

	/* property data for all objects */
	private EDPROPERTY properties;		/* active property list */

	/* net constructors */
	private EDNETPORT firstnetport;		/* the list of ports on a net */
	private EDNETPORT lastnetport; 		/* the last in the list on a net */
	
		/* view NETLIST layout */
	private int sh_xpos, sh_ypos;			/* current sheet position */
	private int sh_offset;					/* next x offset */
	private int ipos, bpos, opos;			/* current position pointers */

//	/* keyword parser entry points, only these keywords are supported by this parser,
//	   if you need more, add them. Each entry point can have an exit point in the
//	   function io_edpop_stack. This is processed when ')' is encountered in the EDIF
//	   file.
//	 */
	
	/* edif keyword stack, this stack matches the current depth of the EDIF file,
	   for the keyword instance it would be KEDIF, KLIBRARY, KCELL, KCONTENTS, KINSTANCE
	 */
	private EDIFKEY [] kstack = new EDIFKEY[1000];
	private int kstack_ptr;
	
	// some standard artwork primitivies
	private PortProto default_port;
	private PortProto default_iconport;
	private PortProto default_busport;
	private PortProto default_input;
	private PortProto default_output;
//	       INTBIG     EDIF_name_key = 0;
//	static INTBIG     EDIF_array_key = 0;
//	static INTBIG     EDIF_annotate_key = 0;
//	
//	#define RET_NOMEMORY() { ttyputnomemory(); return(1); }

	private static final int SHEETWIDTH = 32;
	private static final int SHEETHEIGHT = 20;

	/**
	 * Method to import a library from disk.
	 * @param lib the library to fill
	 * @return true on error.
	 */
	protected boolean importALibrary(Library lib)
	{
		// inits
		firstnetport = lastnetport = null;
		ptype = PUNKNOWN;
		points = new ArrayList();
		save_points = new ArrayList();
	
		// parser inits
		state = KINIT;
		buffer = "";
		pos = 0;
		errors = warnings = 0;
		ignoreblock = false;
		vendor = EVUNKNOWN;
	
		// general inits
		scale = IOTool.getEDIFInputScale();
		library = lib;
		technology = Schematics.tech;
		celltbl = new HashMap();
		ports = null;
		properties = null;
		io_edfreenetports();
	
		// active database inits
		current_cell = null;
		current_node = null;
		current_arc = null;
		current_port = null;
		figure_group = null;
		arc_type = null;
		cellRefProto = null;
	
		// name inits
		cell_reference = "";
		port_reference = "";
		instance_reference = "";
		bundle_reference = "";
		net_reference = "";
		property_reference = "";
	
		// geometry inits
		layer_ptr = 0;
		cur_nametbl = null;
		io_edfreeptlist();
	
		// text inits
		textheight = 0;
		justification = LOWERLEFT;
		visible = true;
		save_string = "";
		io_edfreesavedptlist();
	
		sh_xpos = -1;
		sh_ypos = -1;
	
		kstack_ptr = 0;
	
		default_port = Schematics.tech.wirePinNode.findPortProto("wire");
		default_iconport = Schematics.tech.wirePinNode.getPort(0);
		default_busport = Schematics.tech.busPinNode.findPortProto("bus");
		default_input = Schematics.tech.offpageNode.findPortProto("y");
		default_output = Schematics.tech.offpageNode.findPortProto("a");
	
//		if (EDIF_name_key == 0) EDIF_name_key = makekey(x_("EDIF_name"));
//		if (EDIF_array_key == 0) EDIF_array_key = makekey(x_("EDIF_array"));
//		if (EDIF_annotate_key == 0) EDIF_annotate_key = makekey(x_("EDIF_annotate"));

		// parse the file
		try
		{
			io_edload_edif();
		} catch (IOException e)
		{
			System.out.println("line #" + lineReader.getLineNumber() + ": " + e.getMessage());
			return true;
		}

		if (errors != 0 || warnings != 0)
			System.out.println("info: a total of "+ errors + " errors, and " + warnings + "warnings encountered during load");
	
//		if (library->curnodeproto == NONODEPROTO)
//			library->curnodeproto = library->firstnodeproto;
//	
//		for (np = library->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//			net_examinenodeproto(np);
		return false;
	}
	
	/**
	 * Module: io_edload_edif
	 * Function:  Will load the edif netlist into memory
	 * Method:  Does a simple keyword lookup to load the lisp structured
	 *		 EDIF language into Electric's database
	 */
	private void io_edload_edif()
		throws IOException
	{
		int savestack = -1;
	
		// now read and parse the edif netlist
		for(;;)
		{
			String token = io_edget_keyword();
			if (token == null) break;

			// locate the keyword, and execute the function
			EDIFKEY key = (EDIFKEY)edifKeys.get(token);
			if (key == null)
			{
				System.out.println("warning, line #" + lineReader.getLineNumber() + ": unknown keyword <" + token + ">");
				warnings++;
				kstack[kstack_ptr++] = state;
				state = KUNKNOWN;
			} else
			{
				// found the function, check state
				if (key.stateArray != null && state != KUNKNOWN)
				{
					boolean found = false;
					for(int i=0; i<key.stateArray.length; i++)
						if (key.stateArray[i] == state) { found = true;   break; }
					if (!found)
					{
						System.out.println("error, line #" + lineReader.getLineNumber() + ": illegal state for keyword <" + token + ">");
						errors++;
					}
				}

				// call the function
				kstack[kstack_ptr++] = state;
				state = key;
				if (savestack >= kstack_ptr)
				{
					savestack = -1;
					ignoreblock = false;
				}
				if (!ignoreblock) key.func();
				if (ignoreblock)
				{
					if (savestack == -1) savestack = kstack_ptr;
				}
			}
		}
		if (state != KINIT)
		{
			System.out.println("line #" + lineReader.getLineNumber() + ": unexpected end-of-file encountered");
			errors++;
		}
	}
	
	/* get a keyword routine */
	private String io_edget_keyword()
		throws IOException
	{
		// look for a '(' before the edif keyword
		for(;;)
		{
			String p = io_edget_token((char)0);
			if (p == null) break;
			if (p.equals("(")) break;
			if (p.equals(")"))
			{
//				io_edpop_stack();
			} else
			{
				if (TextUtils.isANumber(buffer)) io_edprocess_integer(TextUtils.atoi(buffer));
			}
		}
		return io_edget_token((char)0);
	}
	
	/* module: io_edprocess_integer
	   function: will do cleanup processing of an integer argument such as in (array (...) 1 2)
	 */
	private void io_edprocess_integer(int value)
	{
		if (kstack_ptr > 0)
		{
			if (!ignoreblock)
			{
				if (state == KARRAY)
				{
					if (arrayx == 0) arrayx = value;
						else if (arrayy == 0) arrayy = value;
				} else if (state == KMEMBER)
				{
					if (memberx == -1) memberx = value;
						else if (membery == -1) membery = value;
				}
			}
		}
	}

	/* This function allocates net ports */
	private void io_edallocnetport()
	{
		EDNETPORT netport = new EDNETPORT();
	
		if (firstnetport == null)firstnetport = netport; else
			lastnetport.next = netport;
	
		lastnetport = netport;
		netport.next = null;
		netport.ni = null;
		netport.pp = null;
		netport.member = 0;
	}
	
	/* free the netport list */
	private void io_edfreenetports()
	{
		firstnetport = lastnetport = null;
	}

	/**
	 * Module: io_edpos_token
	 * Function: special routine to position to the next token
	 * @return true on EOF.
	 */
	private boolean io_edpos_token()
		throws IOException
	{
		for(;;)
		{
			if (pos >= buffer.length())
			{
				buffer = lineReader.readLine();
				if (buffer == null) return true;
				pos = 0;
				continue;
			}
			char chr = buffer.charAt(pos);
			if (chr == ' ' || chr == '\t')
			{
				pos++;
				continue;
			}
			return false;
		}
	}
	
	/* get a delimeter routine */
	private void io_edget_delim(char delim)
		throws IOException
	{
		if (io_edpos_token())
		{
			throw new IOException("Unexpected end-of-file");
		}
		char chr = buffer.charAt(pos);
		if (chr != delim)
		{
			throw new IOException("Illegal delimeter");
		} else
		{
			pos++;
		}
	}

	/* get a token routine */
	private String io_edget_token(char idelim)
		throws IOException
	{
		// locate the first non-white space character for non-delimited searches
		char delim = idelim;
		if (delim == 0)
		{
			if (io_edpos_token()) return null;
		}

		// set up the string copy
		StringBuffer sptr = new StringBuffer();
		char chr = buffer.charAt(pos);
		if (delim == 0 && chr == '"')
		{
			// string search
			delim = '"';
			sptr.append(chr);
			pos++;
		}
	
		// non locate the next white space or the delimiter
		for(;;)
		{
			if (pos >= buffer.length())
			{
				// end of string, get the next line
				buffer = lineReader.readLine();
				if (buffer == null)
				{
					// end-of-file, if no delimiter return NULL, otherwise the string
					if (delim != 0) break;
					return sptr.toString();
				}
				pos = 0;
			} else if (delim == 0)
			{
				chr = buffer.charAt(pos);
				switch (chr)
				{
					case ' ':
					case '\t':
						// skip to the next character
						pos++;
						return sptr.toString();
						// special EDIF delimiters
					case '(':
					case ')':
						if (sptr.length() == 0)
						{
							sptr.append(chr);
							pos++;
						}
						return sptr.toString();
					default:
						sptr.append(chr);
						pos++;
						break;
				}
			} else
			{
				// check for user specified delimiter
				chr = buffer.charAt(pos);
				if (chr == delim)
				{
					// skip the delimiter, unless string
					if (delim != idelim) sptr.append(chr);
					pos++;
					return sptr.toString();
				} else
				{
					sptr.append(chr);
					pos++;
				}
			}
		}
		return null;
	}

	private void io_edfigure()
		throws IOException
	{
		// get the layer name; check for figuregroup override
		if (io_edpos_token()) return;
		if (buffer.charAt(pos) == '(') return;
		String layer = io_edget_token((char)0);
	
		// now look for this layer in the list of layers
		int low = 0;
		int high = layer_ptr - 1;
		for(int i=0; i<MAXLAYERS; i++)
		{
			NAMETABLE nt = nametbl[i];
			if (nt == null) continue;
			if (nt.original.equalsIgnoreCase(layer))
			{
				// found the layer
				figure_group = nametbl[i].node;
				arc_type = nametbl[i].arc;
				textheight = nametbl[i].textheight;
				justification = nametbl[i].justification;
				visible = nametbl[i].visible;
	
				// allow new definitions
				cur_nametbl = nametbl[i];
				return;
			}
		}

		int pos = layer_ptr;
		nametbl[pos] = new NAMETABLE();
		nametbl[pos].original = layer;
		nametbl[pos].replace = nametbl[pos].original;
		figure_group = nametbl[pos].node = Artwork.tech.boxNode;
		arc_type = nametbl[pos].arc = Schematics.tech.wire_arc;
		textheight = nametbl[pos].textheight = 0;
		justification = nametbl[pos].justification = LOWERLEFT;
		visible = nametbl[pos].visible = true;

		// allow new definitions
		cur_nametbl = nametbl[pos];

		// now sort the list
		layer_ptr++;
//		esort(nametbl, layer_ptr, sizeof(NAMETABLE), io_edcompare_name);
	}
	
	private boolean io_edcheck_name()
		throws IOException
	{
		io_edpos_token();
		char chr = buffer.charAt(pos);
		if (chr != '(' && chr != ')')
		{
			String aName = io_edget_token((char)0);
			name = aName.startsWith("&") ? aName.substring(1) : aName;
			return true;
		}
		return false;
	}

	private void io_edfreeptlist()
	{
		points = new ArrayList();
	}
	
	private void io_edfreesavedptlist()
	{
		save_points = new ArrayList();
	}

	private double io_edgetnumber()
		throws IOException
	{
		String value = io_edget_token((char)0);
		if (value == null) throw new IOException("No integer value");

		if (value.startsWith("("))
		{
			// must be in e notation
			value = io_edget_token((char)0);
			if (value == null) throw new IOException("Illegal number value");
			if (value.equalsIgnoreCase("e")) throw new IOException("Illegal number value");
			
	
			// now the matissa
			value = io_edget_token((char)0);
			if (value == null) throw new IOException("No matissa value");
			double matissa = TextUtils.atof(value);
	
			// now the exponent
			value = io_edget_token((char)0);
			if (value == null) throw new IOException("No exponent value");
			double exponent = TextUtils.atof(value);
			io_edget_delim(')');
			return matissa * Math.pow(10.0, exponent);
		}
		return TextUtils.atof(value);
	}
	
	/* This function allocates ports */
	private void io_edallocport()
	{
		EDPORT port = new EDPORT();
	
		port.next = ports;
		ports = port;
		port.reference = port_reference;
		port.name = port_name;
	
		port.direction = direction;
		port.arrayx = arrayx;
		port.arrayy = arrayy;
	}
	
	private int io_edgetrot(OTYPES o)
	{
		if (o == OR0)    return 0;
		if (o == OR90)   return 900;
		if (o == OR180)  return 1800;
		if (o == OR270)  return 2700;
		if (o == OMX)    return 2700;
		if (o == OMY)    return 900;
		if (o == OMXR90) return 1800;
		if (o == OMYR90) return 0;
		return 0;
	}
	
	private int io_edgettrans(OTYPES o)
	{
		if (o == OR0)    return 0;
		if (o == OR90)   return 0;
		if (o == OR180)  return 0;
		if (o == OR270)  return 0;
		if (o == OMY)    return 1;
		if (o == OMX)    return 1;
		if (o == OMYR90) return 1;
		if (o == OMXR90) return 1;
		return 0;
	}
	
	private boolean getMirrorX(OTYPES o)
	{
		if (o == OR0)    return false;
		if (o == OR90)   return false;
		if (o == OR180)  return false;
		if (o == OR270)  return false;
		if (o == OMY)    return false;
		if (o == OMX)    return true;
		if (o == OMYR90) return false;
		if (o == OMXR90) return true;
		return false;
	}
	
	private boolean getMirrorY(OTYPES o)
	{
		if (o == OR0)    return false;
		if (o == OR90)   return false;
		if (o == OR180)  return false;
		if (o == OR270)  return false;
		if (o == OMY)    return true;
		if (o == OMX)    return false;
		if (o == OMYR90) return true;
		if (o == OMXR90) return false;
		return false;
	}
	
	private NodeInst io_edifiplacepin(NodeProto np, double cX, double cY, double sX, double sY,
		int angle, Cell parent)
	{
		for(Iterator it = parent.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.getProto() != np) continue;
			if (ni.getAngle() != angle) continue;
			if (ni.isXMirrored() != (sX < 0)) continue;
			if (ni.isYMirrored() != (sY < 0)) continue;
			if (ni.getAnchorCenterX() != cX) continue;
			if (ni.getAnchorCenterY() != cY) continue;
			return ni;
		}
		NodeInst ni = NodeInst.makeInstance(np, new Point2D.Double(cX, cY), sX, sY, parent, angle, null, 0);
		return ni;
	}

	/* pop the keyword state stack, called when ')' is encountered. Note this function
	   needs to broken into a simple function call structure. */
//	#define MAXBUSPINS 256
	private void io_edpop_stack()
	{
//		INTBIG eindex, key, lambda;
//		INTBIG lx, ly, hx, hy, cx, cy, cnt, Ix, Iy, gx, gy, xoff, yoff, dist;
//		INTBIG *trace, *pt, pts[26], fcnt, tcnt, psx, psy;
//		INTBIG width, height, x[3], y[3], radius, trans;
//		INTBIG count, user_max, i, j, dup, fbus, tbus, instcount, instptx, instpty;
//		UINTBIG descript[TEXTDESCRIPTSIZE];
//		CHAR nodename[WORD+1], portname[WORD+1], basename[WORD+1], orig[WORD+1];
//		CHAR **layers, **layer_numbers;
//		NODEPROTO *np, *nnp;
//		ARCPROTO *ap, *lap;
//		ARCINST *ai;
//		PORTPROTO *ppt, *fpp, *lpp, *pp, *lastport;
//		NODEINST *ni, *lastpin, *fni, *lni;
//		EPT_PTR point;
//		EDPROPERTY_PTR property, nproperty;
//		double ar, so;
//		VARIABLE *var;
//		XARRAY rot;
//		EDPORT_PTR eport, neport;
//		// bus connection variables
//		PORTPROTO *fpps[MAXBUSPINS], *tpps[MAXBUSPINS];
//		NODEINST *fnis[MAXBUSPINS], *tnis[MAXBUSPINS];
//		REGISTER void *infstr;
	
//		lambda = el_curlib->lambda[technology->techindex];
		if (kstack_ptr != 0)
		{
			if (!ignoreblock)
			{
				if (state == KFIGURE)
				{
					cur_nametbl = null;
					visible = true;
					justification = LOWERLEFT;
					textheight = 0;
				} else if (state == KBOUNDINGBOX)
				{
					figure_group = null;
				} else if (state == KTECHNOLOGY)
				{
//					var = getval((INTBIG)technology, VTECHNOLOGY, VSTRING|VISARRAY,
//						x_("IO_gds_layer_numbers"));
//					if (var != NOVARIABLE)
//					{
//						layer_numbers = (CHAR **)var->addr;
//						count = getlength(var);
//						var = getval((INTBIG)technology, VTECHNOLOGY, VSTRING|VISARRAY,
//							x_("TECH_layer_names"));
//						if (var != NOVARIABLE)
//						{
//							layers = (CHAR **) var->addr;
//							user_max = layer_ptr;
//							// for all layers assign their GDS number
//							for (i=0; i<count; i++)
//							{
//								if (*layer_numbers[i] != 0)
//								{
//									// search for this layer
//									for (j=0; j<user_max; j++)
//									{
//										if (!namesame(nametbl[j].replace, layers[i])) break;
//									}
//									if (user_max == j)
//									{
//										// add to the list
//										io_edifgbl.nametbl[layer_ptr] = (NAMETABLE_PTR)
//											emalloc(sizeof(NAMETABLE), io_tool->cluster);
//										if (io_edifgbl.nametbl[layer_ptr] == NONAMETABLE) RET_NOMEMORY();
//										if (allocstring(&io_edifgbl.nametbl[layer_ptr]->replace, layers[i],
//											io_tool->cluster)) RET_NOMEMORY();
//										esnprintf(orig, WORD+1, x_("layer_%s"), layer_numbers[i]);
//										if (allocstring(&io_edifgbl.nametbl[layer_ptr]->original,
//											orig, io_tool->cluster)) RET_NOMEMORY();
//										io_edifgbl.nametbl[layer_ptr]->textheight = 0;
//										io_edifgbl.nametbl[layer_ptr]->justification = LOWERLEFT;
//										layer_ptr++;
//									}
//								}
//							}
//						}
//					}
//	
//					// sort the layer list
//					esort(io_edifgbl.nametbl, layer_ptr, sizeof(NAMETABLE_PTR),
//						io_edcompare_name);
//	
//					// now look for nodes to map MASK layers to
//					for (eindex = 0; eindex < layer_ptr; eindex++)
//					{
//						esnprintf(nodename, WORD+1, x_("%s-node"), io_edifgbl.nametbl[eindex]->replace);
//						for (np = io_edifgbl.technology->firstnodeproto; np != NONODEPROTO;
//							np = np->nextnodeproto)
//						{
//							if (!namesame(nodename, np->protoname)) break;
//						}
//						if (np == NONODEPROTO)
//						{
//							np = art_boxprim;
//						}
//						for (ap = technology->firstarcproto; ap != NOARCPROTO;
//							ap = ap->nextarcproto)
//						{
//							if (!namesame(ap->protoname, io_edifgbl.nametbl[eindex]->replace))
//								break;
//						}
//						io_edifgbl.nametbl[eindex]->node = np;
//						io_edifgbl.nametbl[eindex]->arc = ap;
//					}
				} else if (state == KINTERFACE)
				{
					if (active_view == VNETLIST)
					{
						// create a black-box symbol at the current scale
//						String nodename = current_cell.getName() + "{ic}";
//						nnp = io_edmakeiconcell(current_cell->firstportproto, current_cell->protoname,
//							nodename, library);
//						if (nnp == NONODEPROTO)
//						{
//							System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create icon <" + nodename + ">");
//							errors++;
//						} else
//						{
//							// now compute the bounds of this cell
//							db_boundcell(nnp, &nnp->lowx, &nnp->highx, &nnp->lowy, &nnp->highy);
//						}
					}
				} else if (state == KVIEW)
				{
//					if (vendor == EVVIEWLOGIC && active_view != VNETLIST)
//					{
//						// fixup incorrect bus nets
//						for (ai = current_cell->firstarcinst; ai != NOARCINST;
//							ai = ai->nextarcinst)
//						{
//							ai->temp1 = ai->temp2 = 0;
//						}
//	
//						// now scan for BUS nets, and verify all wires connected to bus
//						for (ai = current_cell->firstarcinst; ai != NOARCINST;
//							ai = ai->nextarcinst)
//						{
//							if (ai->temp1 == 0 && ai->proto == sch_busarc)
//							{
//								// get name of arc
//								var = getvalkey((INTBIG)ai, VARCINST, VSTRING, el_arc_name_key);
//								if (var != NOVARIABLE && ((aName = (CHAR *)var->addr) != NULL))
//								{
//									// create the basename
//									(void)estrcpy(basename, aName);
//									aName = estrrchr(basename, '[');
//									if (aName != NULL) *aName = 0;
//	
//									// expand this arc, and locate non-bracketed names
//									io_edcheck_busnames(ai, basename);
//								}
//							}
//						}
//					}
//	
//					ports = null;
				} else if (state == KPORT)
				{
					io_edallocport();
					double cX = 0, cY = 0;
					PortProto fpp = default_input;
					if (ports.direction == INPUTE)
					{
						cY = ipos;
						ipos += INCH;
					} else if (ports.direction == INOUT)
					{
						cX = 3*INCH;
						cY = bpos;
						bpos += INCH;
					} else if (ports.direction == OUTPUTE)
					{
						cX = 6*INCH;
						cY = opos;
						opos += INCH;
						fpp = default_output;
					}
	
					// now create the off-page reference
					double psx = Schematics.tech.offpageNode.getDefWidth();
					double psy = Schematics.tech.offpageNode.getDefHeight();
					NodeInst ni = NodeInst.makeInstance(Schematics.tech.offpageNode, new Point2D.Double(cX, cY), psx, psy, current_cell);
					if (ni == null)
					{
						System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create external port");
						errors++;
						return;
					}
	
					// now create the port
					PortInst pi = ni.findPortInstFromProto(fpp);
					Export ppt = Export.newInstance(current_cell, pi, ports.name);
					if (ppt == null)
					{
						System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create port <" + ports.name + ">");
						errors++;
					} else
					{
						if (ports.direction == INPUTE) ppt.setCharacteristic(PortCharacteristic.IN); else
							if (ports.direction == OUTPUTE) ppt.setCharacteristic(PortCharacteristic.OUT); else
								if (ports.direction == INOUT) ppt.setCharacteristic(PortCharacteristic.BIDIR);
					}
					port_reference = "";
	
					// move the property list to the free list
//					for (EDPROPERTY property = properties; property != null; property = nproperty)
//					{
//						key = makekey(property->name);
//						switch (property->type)
//						{
//							case PINTEGER:
//								var = setvalkey((INTBIG)ppt, VPORTPROTO, key, (INTBIG)property->val.integer,
//									VINTEGER);
//								break;
//							case PNUMBER:
//								var = setvalkey((INTBIG)ppt, VPORTPROTO, key, castint(property->val.number),
//									VFLOAT);
//								break;
//							case PSTRING:
//								var = setvalkey((INTBIG)ppt, VPORTPROTO, key, (INTBIG)property->val.string,
//									VSTRING);
//								break;
//							default:
//								break;
//						}
//						nproperty = property.next;
//					}
					properties = null;
				} else if (state == KINSTANCE)
				{
//					if (active_view == VNETLIST)
//					{
//						for (int Ix = 0; Ix < arrayx; Ix++)
//						{
//							for (int Iy = 0; Iy < arrayy; Iy++)
//							{
//								// create this instance in the current sheet
//								width = cellRefProto->highx - cellRefProto->lowx;
//								height = cellRefProto->highy - cellRefProto->lowy;
//								width = (width + INCH - 1) / INCH;
//								height = (height + INCH - 1) / INCH;
//	
//								// verify room for the icon
//								if (sh_xpos != -1)
//								{
//									if ((sh_ypos + (height + 1)) >= SHEETHEIGHT)
//									{
//										sh_ypos = 1;
//										if ((sh_xpos += sh_offset) >= SHEETWIDTH)
//												sh_xpos = sh_ypos = -1; else
//													sh_offset = 2;
//									}
//								}
//								if (sh_xpos == -1)
//								{
//									// create the new page
//									String nodename = cell_name + "{" + (++pageno) + "}";
//									current_cell = us_newnodeproto(nodename, library);
//									if (current_cell == NONODEPROTO) throw new IOException("Error creating cell");
//									current_cell->temp1 = 0;
//									sh_xpos = sh_ypos = 1;
//									sh_offset = 2;
//								}
//	
//								// create this instance
//								// find out where true center moves
//								cx = ((cellRefProto->lowx+cellRefProto->highx) >> 1) +
//									((sh_xpos - (SHEETWIDTH >> 1)) * INCH);
//								cy = ((cellRefProto->lowy+cellRefProto->highy) >> 1) +
//									((sh_ypos - (SHEETHEIGHT >> 1)) * INCH);
//								current_node = ni = newnodeinst(cellRefProto,
//									cellRefProto->lowx + cx,
//									cellRefProto->highx + cx,
//									cellRefProto->lowy + cy,
//									cellRefProto->highy + cy,
//									io_edgettrans(orientation),
//									io_edgetrot(orientation),
//									current_cell);
//								if (ni == NONODEINST)
//								{
//									System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create instance");
//									errors++;
//									break;
//								} else
//								{
//									if (cellRefProto->userbits & WANTNEXPAND)
//										ni->userbits |= NEXPAND;
//	
//									// update the current position
//									if ((width + 2) > sh_offset)
//										sh_offset = width + 2;
//									if ((sh_ypos += (height + 1)) >= SHEETHEIGHT)
//									{
//										sh_ypos = 1;
//										if ((sh_xpos += sh_offset) >= SHEETWIDTH)
//											sh_xpos = sh_ypos = -1; else
//												sh_offset = 2;
//									}
//	
//									// name the instance
//									if (instance_reference.length() > 0)
//									{
//										// if single element or array with no offset
//										// construct the representative extended EDIF name (includes [...])
//										if ((arrayx == 1 && arrayy == 1) ||
//											(deltaxX == 0 && deltaxY == 0 &&
//											deltayX == 0 && deltayY == 0))
//												nodename = instance_reference;
//											// if array in the x dimension
//										else if (arrayx > 1)
//										{
//											if (arrayy > 1)
//												nodename = instance_refrence + "[" + Ix + "," + Iy + "]";
//											else
//												nodename = instance_reference + "[" + Ix + "]";
//										}
//										// if array in the y dimension
//										else if (arrayy > 1)
//											nodename = instance_reference + "[" + Iy + "]";
//	
//										// check for array element descriptor
//										if (arrayx > 1 || arrayy > 1)
//										{
//											// array descriptor is of the form index:index:range index:index:range
//											(void)esnprintf(basename, WORD+1, x_("%ld:%ld:%d %ld:%ld:%d"), Ix,
//												(deltaxX == 0 && deltayX == 0) ?
//												arrayx-1:Ix, arrayx, Iy,
//												(deltaxY == 0 && deltayY == 0) ?
//												arrayy-1:Iy, arrayy);
//											var = setvalkey((INTBIG)ni, VNODEINST, EDIF_array_key, (INTBIG)basename,
//												VSTRING);
//	
//										}
//	
//										/* now set the name of the component (note that Electric allows any string
//										 * of characters as a name, this name is open to the user, for ECO and other
//										 * consistancies, the EDIF_name is saved on a variable)
//										 */
//										if (!namesame(instance_reference, instance_name))
//										{
//											var = setvalkey((INTBIG)ni, VNODEINST, el_node_name_key,
//												(INTBIG)nodename, VSTRING|VDISPLAY);
//											if (var != NOVARIABLE)
//												defaulttextsize(3, var->textdescript);
//										} else
//										{
//											// now add the original name as the displayed name (only to the first element)
//											if (Ix == 0 && Iy == 0)
//											{
//												var = setvalkey((INTBIG)ni, VNODEINST, el_node_name_key,
//													(INTBIG)instance_name, VSTRING|VDISPLAY);
//												if (var != NOVARIABLE)
//													defaulttextsize(3, var->textdescript);
//											}
//											// now save the EDIF name (not displayed)
//											var = setvalkey((INTBIG)ni, VNODEINST, EDIF_name_key,
//												(INTBIG)nodename, VSTRING);
//										}
//									}
//								}
//							}
//						}
//					}
//	
//					// move the property list to the free list
//					for (property = properties; property != NOEDPROPERTY; property = nproperty)
//					{
//						if (current_node != null)
//						{
//							key = makekey(property->name);
//							switch (property->type)
//							{
//								case PINTEGER:
//									var = setvalkey((INTBIG)current_node, VNODEINST, key,
//										(INTBIG)property->val.integer, VINTEGER);
//									break;
//								case PNUMBER:
//									var = setvalkey((INTBIG)current_node, VNODEINST, key,
//										castint(property->val.number), VFLOAT);
//									break;
//								case PSTRING:
//									var = setvalkey((INTBIG)current_node, VNODEINST, key,
//										(INTBIG)property->val.string, VSTRING);
//									break;
//								default:
//									break;
//							}
//						}
//						nproperty = property->next;
//					}
//					properties = null;
//					instance_reference = "";
//					current_node = null;
//					io_edfreesavedptlist();
				} else if (state == KNET)
				{
//					// move the property list to the free list
//					for (property = properties; property != NOEDPROPERTY; property = nproperty)
//					{
//						if (current_arc != null)
//						{
//							key = makekey(property->name);
//							switch (property->type)
//							{
//								case PINTEGER:
//									var = setvalkey((INTBIG)current_arc, VARCINST, key,
//										(INTBIG)property->val.integer, VINTEGER);
//									break;
//								case PNUMBER:
//									var = setvalkey((INTBIG)current_arc, VARCINST, key,
//										castint(property->val.number), VFLOAT);
//									break;
//								case PSTRING:
//									var = setvalkey((INTBIG)current_arc, VARCINST, key,
//										(INTBIG)property->val.string, VSTRING);
//									break;
//								default:
//									break;
//							}
//						}
//						nproperty = property->next;
//					}
//					properties = null;
//					net_reference = "";
//					current_arc = null;
//					if (geometry != GBUS) geometry = GUNKNOWN;
//					io_edfreesavedptlist();
				} else if (state == KNETBUNDLE)
				{
					bundle_reference = "";
					current_arc = null;
					geometry = GUNKNOWN;
					io_edfreesavedptlist();
				} else if (state == KPROPERTY)
				{
//					if (active_view == VNETLIST || active_view == VSCHEMATIC)
//					{
//						// add as a variable to the current object
//						i = 0;
//						switch (kstack[kstack_ptr - 1])
//						{
//							case KINTERFACE:
//								// add to the {sch} view nodeproto
//								for (np = library->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//								{
//									if (!namesame(np->protoname, cell_name) &&
//										!namesame(np->cellview->sviewname, x_("sch"))) break;
//								}
//								if (np == NONODEPROTO)
//								{
//									// allocate the cell
//									String nodename = cell_name + "{sch}";
//									np = us_newnodeproto(nodename, library);
//									if (np == null) throw new IOException("Error creating cell");
//									np->temp1 = 0;
//								}
//								i = (INTBIG) np;
//								j = VNODEPROTO;
//								break;
//							case KINSTANCE:
//							case KNET:
//							case KPORT:
//								i = -1;
//								break;
//							default:
//								i = 0;
//								break;
//						}
//						if (i > 0)
//						{
//							key = makekey(property_reference);
//							switch (ptype)
//							{
//								case PINTEGER:
//									var = setvalkey(i, j, key, (INTBIG)pval_integer, VINTEGER);
//									break;
//								case PNUMBER:
//									var = setvalkey(i, j, key, castint(pval_number), VFLOAT);
//									break;
//								case PSTRING:
//									var = setvalkey(i, j, key, (INTBIG)pval_string, VSTRING);
//									break;
//								default:
//									break;
//							}
//						} else if (i == -1)
//						{
//							// add to the current property list, will be added latter
//							switch (ptype)
//							{
//								case PINTEGER:
//									(void)io_edallocproperty(property_reference, ptype,
//										(INTBIG)pval_integer, 0.0, NULL);
//									break;
//								case PNUMBER:
//									(void)io_edallocproperty(property_reference, ptype,
//										0, pval_number, NULL);
//									break;
//								case PSTRING:
//									(void)io_edallocproperty(property_reference, ptype,
//										0, 0.0, pval_string);
//									break;
//								default:
//									break;
//							}
//						}
//					}
//					property_reference = "";
//					io_edfreesavedptlist();
				} else if (state == KPORTIMPLEMENTATION)
				{
					geometry = GUNKNOWN;
				} else if (state == KTRANSFORM)
				{
					if (kstack_ptr <= 1 || kstack[kstack_ptr-1] != KINSTANCE)
					{
						io_edfreeptlist();
						return;
					}
	
					// get the corner offset
					double instptx = 0, instpty = 0;
					int instcount = points.size();
					if (instcount > 0)
					{
						EPT point = (EPT)points.get(0);
						instptx = point.x;
						instpty = point.y;
					}
	
					// if no points are specified, presume the origin
					if (instcount == 0) instcount = 1;
	
					// create node instance rotations about the origin not center
					AffineTransform rot = NodeInst.pureRotate(io_edgetrot(orientation), getMirrorX(orientation), getMirrorY(orientation));
	
//					if (instcount == 1 && cellRefProto != null)
//					{
//						for (Ix = 0; Ix < arrayx; Ix++)
//						{
//							lx = instptx + Ix * deltaxX;
//							ly = instpty + Ix * deltaxY;
//							for (Iy = 0; Iy < arrayy; Iy++)
//							{
//								// find out where true center moves
//								cx = (cellRefProto->lowx+cellRefProto->highx)/2;
//								cy = (cellRefProto->lowy+cellRefProto->highy)/2;
//								xform(cx, cy, &gx, &gy, rot);
//	
//								// now calculate the delta movement of the center
//								cx = gx - cx;
//								cy = gy - cy;
//								current_node = ni = newnodeinst(cellRefProto,
//									lx + cellRefProto->lowx + cx,
//									lx + cellRefProto->highx + cx,
//									ly + cellRefProto->lowy + cy,
//									ly + cellRefProto->highy + cy,
//									io_edgettrans(orientation),
//									io_edgetrot(orientation),
//									current_cell);
//								if (ni == NONODEINST)
//								{
//									System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create instance");
//									errors++;
//	
//									// and exit for loop
//									Ix = arrayx;
//									Iy = arrayy;
//								} else
//								{
//									if (cellRefProto->userbits & WANTNEXPAND)
//										ni->userbits |= NEXPAND;
//								}
//								if (geometry == GPIN && lx == 0 && ly == 0)
//								{
//									// determine an appropriate port name
//									portname = port_name;
//	
//									// check if port already exists (will occur for multiple portImplementation statements
//									for (dup = 0, (void)estrcpy(basename, portname);
//										(ppt = getportproto(ni->parent, portname)) != NOPORTPROTO;
//										dup++)
//									{
//										if (dup == 0) pp = ppt;
//										(void)esnprintf(portname, WORD+1, x_("%s_%ld"), basename, dup+1);
//									}
//	
//									// only once
//									Ix = arrayx;
//									Iy = arrayy;
//									ppt = newportproto(ni->parent, ni, default_iconport, portname);
//									if (ppt == NOPORTPROTO)
//									{
//										System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create port <" + portname + ">");
//										errors++;
//									} else
//									{
//										// locate the direction of the port
//										for (eport = ports; eport != NOEDPORT; eport = eport->next)
//										{
//											if (!namesame(eport->reference, port_reference))
//											{
//												// set the direction
//												switch (eport->direction)
//												{
//													case INPUTE:
//														ppt->userbits = (ppt->userbits & ~STATEBITS) | INPORT;
//														break;
//													case OUTPUTE:
//														ppt->userbits = (ppt->userbits & ~STATEBITS) | OUTPORT;
//														break;
//													case INOUT:
//														ppt->userbits = (ppt->userbits & ~STATEBITS) | BIDIRPORT;
//														break;
//												}
//												break;
//											}
//										}
//									}
//								} else
//								{
//									// name the instance
//									if (instance_reference.length() > 0)
//									{
//										// if single element or array with no offset
//										// construct the representative extended EDIF name (includes [...])
//										if ((arrayx == 1 && arrayy == 1) ||
//											(deltaxX == 0 && deltaxY == 0 &&
//											deltayX == 0 && deltayY == 0))
//											nodename = instance_reference;
//											// if array in the x dimension
//										else if (arrayx > 1)
//										{
//											if (arrayy > 1)
//												nodename = instance_reference + "[" + Ix + "," + Iy + "]";
//											else
//												nodename = instance_reference + "[" + Ix + "]";
//										}
//										// if array in the y dimension
//										else if (arrayy > 1)
//											nodename = instance_reference + "[" + Iy + "]";
//	
//										// check for array element descriptor
//										if (arrayx > 1 || arrayy > 1)
//										{
//											// array descriptor is of the form index:index:range index:index:range
//											(void)esnprintf(basename, WORD+1, x_("%ld:%ld:%d %ld:%ld:%d"),
//												Ix, (deltaxX == 0 && deltayX == 0) ? arrayx-1:Ix,
//												arrayx, Iy,
//												(deltaxY == 0 && deltayY == 0) ? arrayy-1:Iy,
//												arrayy);
//											var = setvalkey((INTBIG)ni, VNODEINST, EDIF_array_key, (INTBIG)basename,
//												VSTRING);
//										}
//	
//										/* now set the name of the component (note that Electric allows any string
//				   						 * of characters as a name, this name is open to the user, for ECO and other
//										 * consistancies, the EDIF_name is saved on a variable)
//										 */
//										if (!namesame(instance_reference, instance_name))
//										{
//											var = setvalkey((INTBIG)ni, VNODEINST, el_node_name_key,
//												(INTBIG)nodename, VSTRING|VDISPLAY);
//											if (var != NOVARIABLE)
//												defaulttextsize(3, var->textdescript);
//										} else
//										{
//											// now add the original name as the displayed name (only to the first element)
//											if (Ix == 0 && Iy == 0)
//											{
//												var = setvalkey((INTBIG)ni, VNODEINST, el_node_name_key,
//													(INTBIG)instance_name, VSTRING|VDISPLAY);
//												if (var != NOVARIABLE)
//													defaulttextsize(3, var->textdescript);
//											}
//	
//											// now save the EDIF name (not displayed)
//											var = setvalkey((INTBIG)ni, VNODEINST, EDIF_name_key,
//												(INTBIG)nodename, VSTRING);
//										}
//	
//										// now check for saved name attributes
//										if (save_points.size() != 0)
//										{
//											// now set the position, relative to the center of the current object
//											xoff = save_points->x - ((ni->highx+ni->lowx)>>1);
//											yoff = save_points->y - ((ni->highy+ni->lowy)>>1);
//	
//											// convert to quarter lambda units
//											xoff = 4*xoff / lambda;
//											yoff = 4*yoff / lambda;
//	
//											/*
//											 * determine the size of text, 0.0278 in == 2 points or 36 (2xpixels) == 1 in
//											 * fonts range from 4 to 20 points
//											 */
//											if (save_textheight == 0) i = TXTSETQLAMBDA(4); else
//											{
//												i = io_ediftextsize(save_textheight);
//											}
//											TDCOPY(descript, var->textdescript);
//											TDSETOFF(descript, xoff, yoff);
//											TDSETSIZE(descript, i);
//											TDSETPOS(descript, VTPOSCENT);
//											switch (save_justification)
//											{
//												case UPPERLEFT:
//													TDSETPOS(descript, VTPOSUPRIGHT);
//													break;
//												case UPPERCENTER:
//													TDSETPOS(descript, VTPOSUP);
//													break;
//												case UPPERRIGHT:
//													TDSETPOS(descript, VTPOSUPLEFT);
//													break;
//												case CENTERLEFT:
//													TDSETPOS(descript, VTPOSRIGHT);
//													break;
//												case CENTERCENTER:
//													TDSETPOS(descript, VTPOSCENT);
//													break;
//												case CENTERRIGHT:
//													TDSETPOS(descript, VTPOSLEFT);
//													break;
//												case LOWERLEFT:
//													TDSETPOS(descript, VTPOSDOWNRIGHT);
//													break;
//												case LOWERCENTER:
//													TDSETPOS(descript, VTPOSDOWN);
//													break;
//												case LOWERRIGHT:
//													TDSETPOS(descript, VTPOSDOWNLEFT);
//													break;
//											}
//											TDCOPY(var->textdescript, descript);
//										}
//									}
//								}
//								if (deltayX == 0 && deltayY == 0) break;
//	
//								// bump the y delta and x deltas
//								lx += deltayX;
//								ly += deltayY;
//							}
//							if (deltaxX == 0 && deltaxY == 0) break;
//						}
//					}
//					io_edfreeptlist();
				} else if (state == KPORTREF)
				{
//					// check for the last pin
//					fni = current_node;
//					fpp = current_port;
//					if (port_reference.length() > 0)
//					{
//						// For internal pins of an instance, determine the base port location and other pin assignments
//						if (instance_reference.length() > 0)
//						{
//							nodename = instance_reference;
//	
//							// locate the node and and port
//							if (active_view == VNETLIST)
//							{
//								// scan all pages for this nodeinst
//								ni = NONODEINST;
//								for (np = library->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//								{
//									if (namesame(np->protoname, cell_name) != 0) continue;
//									for (ni = np->firstnodeinst; ni != NONODEINST; ni = ni->nextnodeinst)
//									{
//										if ((var = getvalkey((INTBIG)ni, VNODEINST, VSTRING, EDIF_name_key)) == NOVARIABLE &&
//											(var = getvalkey((INTBIG)ni, VNODEINST, VSTRING, el_node_name_key)) == NOVARIABLE)
//												continue;
//										if (!namesame((CHAR *)var->addr, nodename)) break;
//									}
//									if (ni != NONODEINST) break;
//								}
//								if (ni == NONODEINST)
//								{
//									(void)ttyputmsg(_("error, line #%d: could not locate netlist node (%s)"),
//										io_edifgbl.lineno, nodename);
//									break;
//								}
//								ap = gen_unroutedarc;
//							} else
//							{
//								// net always references the current page
//								for (ni = current_cell->firstnodeinst; ni != NONODEINST;
//									ni = ni->nextnodeinst)
//								{
//									if ((var = getvalkey((INTBIG)ni, VNODEINST, VSTRING, EDIF_name_key)) == NOVARIABLE &&
//										(var = getvalkey((INTBIG)ni, VNODEINST, VSTRING, el_node_name_key)) == NOVARIABLE)
//											continue;
//									if (!namesame((CHAR *)var->addr, nodename)) break;
//								}
//								if (ni == NONODEINST)
//								{
//									(void)ttyputmsg(_("error, line #%d: could not locate schematic node '%s' in cell %s"),
//										io_edifgbl.lineno, nodename, describenodeproto(current_cell));
//									break;
//								}
//								if (!isarray) ap = sch_wirearc; else
//									ap = sch_busarc;
//							}
//	
//							// locate the port for this portref
//							pp = getportproto(ni->proto, port_reference);
//							if (pp == NOPORTPROTO)
//							{
//								(void)ttyputmsg(_("error, line #%d: could not locate port (%s) on node (%s)"),
//									io_edifgbl.lineno, port_reference, nodename);
//								break;
//							}
//	
//							// we have both, set global variable
//							current_node = ni;
//							current_port = pp;
//							np = ni->parent;
//	
//							// create extensions for net labels on single pin nets (externals), and placeholder for auto-routing later
//							if (active_view == VNETLIST)
//							{
//								// route all pins with an extension
//								if (ni != NONODEINST)
//								{
//									portposition(ni, pp, &hx, &hy);
//									lx = hx;
//									ly = hy;
//									switch (pp->userbits&STATEBITS)
//									{
//										case INPORT:
//											lx = hx - (INCH/10);
//											break;
//										case BIDIRPORT:
//											ly = hy - (INCH/10);
//											break;
//										case OUTPORT:
//											lx = hx + (INCH/10);
//											break;
//									}
//	
//									// need to create a destination for the wire
//									if (isarray)
//									{
//										lni = io_edifiplacepin(sch_buspinprim, lx + sch_buspinprim->lowx,
//											lx + sch_buspinprim->highx, ly + sch_buspinprim->lowy,
//											ly + sch_buspinprim->highy, 0, 0, np);
//										if (lni == NONODEINST)
//										{
//											ttyputmsg(_("error, line#%d: could not create bus pin"),
//												io_edifgbl.lineno);
//											break;
//										}
//										lpp = default_busport;
//										lap = sch_busarc;
//									} else
//									{
//										lni = io_edifiplacepin(sch_wirepinprim, lx + sch_wirepinprim->lowx,
//											lx + sch_wirepinprim->highx, ly + sch_wirepinprim->lowy,
//											ly + sch_wirepinprim->highy, 0, 0, np);
//										if (lni == NONODEINST)
//										{
//											ttyputmsg(_("error, line#%d: could not create wire pin"),
//												io_edifgbl.lineno);
//											break;
//										}
//										lpp = default_port;
//										lap = sch_wirearc;
//									}
//									current_arc = newarcinst(lap, defaultarcwidth(lap), CANTSLIDE,
//										lni, lpp, lx, ly, ni, pp, hx, hy, np);
//									if (current_arc == NOARCINST)
//										ttyputmsg(_("error, line #%d: could not create auto-path"),
//											io_edifgbl.lineno);
//									else
//										io_ednamearc(current_arc);
//								}
//							}
//						} else
//						{
//							// external port reference, look for a off-page reference in {sch} with this port name
//							for (np = library->firstnodeproto; np != NONODEPROTO; np = np->nextnodeproto)
//							{
//								if (namesame(np->protoname, cell_name) != 0) continue;
//								if (np->cellview == el_schematicview) break;
//							}
//							if (np == NONODEPROTO)
//							{
//								ttyputmsg(_("error, line #%d: could not locate top level schematic"),
//									io_edifgbl.lineno);
//								break;
//							}
//	
//							// now look for an instance with the correct port name
//							pp = getportproto(np, port_reference);
//							if (pp == NOPORTPROTO)
//							{
//								if (original.length() > 0)
//									pp = getportproto(np, original);
//								if (pp == NOPORTPROTO)
//								{
//									ttyputmsg(_("error, line #%d: could not locate port '%s'"), io_edifgbl.lineno,
//										port_reference);
//									break;
//								}
//							}
//							fni = pp->subnodeinst;
//							fpp = pp->subportproto;
//							portposition(fni, fpp, &lx, &ly);
//	
//							// determine x position by original placement
//							switch (pp->userbits&STATEBITS)
//							{
//								case INPORT:
//									lx = 1*INCH;
//									break;
//								case BIDIRPORT:
//									lx = 4*INCH;
//									break;
//								case OUTPORT:
//									lx = 5*INCH;
//									break;
//							}
//	
//							// need to create a destination for the wire
//							if (isarray)
//							{
//								ni = io_edifiplacepin(sch_buspinprim, lx + sch_buspinprim->lowx,
//									lx + sch_buspinprim->highx, ly + sch_buspinprim->lowy,
//									ly + sch_buspinprim->highy, 0, 0, np);
//								if (ni == NONODEINST)
//								{
//									ttyputmsg(_("error, line#%d: could not create bus pin"),
//										io_edifgbl.lineno);
//									break;
//								}
//								pp = default_busport;
//							} else
//							{
//								ni = io_edifiplacepin(sch_wirepinprim, lx + sch_wirepinprim->lowx,
//									lx + sch_wirepinprim->highx, ly + sch_wirepinprim->lowy,
//									ly + sch_wirepinprim->highy, 0, 0, np);
//								if (ni == NONODEINST)
//								{
//									ttyputmsg(_("error, line#%d: could not create wire pin"),
//										io_edifgbl.lineno);
//									break;
//								}
//								pp = default_port;
//							}
//							if (!isarray) ap = sch_wirearc; else
//								ap = sch_busarc;
//						}
//	
//						// now connect if we have from node and port
//						if (fni != NONODEINST && fpp != NOPORTPROTO)
//						{
//							if (fni->parent != ni->parent)
//							{
//								ttyputmsg(_("error, line #%d: could not create path (arc) between cells %s and %s"),
//									io_edifgbl.lineno, describenodeproto(fni->parent), describenodeproto(ni->parent));
//							} else
//							{
//								// locate the position of the new ports
//								portposition(fni, fpp, &lx, &ly);
//								portposition(ni, pp, &hx, &hy);
//	
//								// for nets with no physical representation ...
//								current_arc = null;
//								if (lx == hx && ly == hy) dist = 0; else
//									dist = computedistance(lx, ly, hx, hy);
//								if (dist <= 1)
//								{
//									current_arc = newarcinst(ap, defaultarcwidth(ap),
//										FIXANG|CANTSLIDE, fni, fpp, lx, ly, ni, pp, hx, hy, np);
//									if (current_arc == null)
//									{
//										ttyputmsg(_("error, line #%d: could not create path (arc)"),
//											io_edifgbl.lineno);
//									}
//								}
//								// use unrouted connection for NETLIST views
//								else if (active_view == VNETLIST)
//								{
//									current_arc = newarcinst(ap, defaultarcwidth(ap),
//										CANTSLIDE, fni, fpp, lx, ly, ni, pp, hx, hy, np);
//									if (current_arc == null)
//									{
//										ttyputmsg(_("error, line #%d: could not create auto-path"),
//											io_edifgbl.lineno);
//									}
//								}
//	
//								// add the net name
//								if (current_arc != null)
//									io_ednamearc(current_arc);
//							}
//						}
//					}
				} else if (state == KINSTANCEREF)
				{
				} else if (state == KPATH)
				{
//					// check for openShape type path
//					if (path_width == 0 &&
//						geometry != GNET && geometry != GBUS) goto dopoly;
//					fcnt = 0;
//					if (geometry == GBUS || isarray) np = sch_buspinprim;
//					else np = sch_wirepinprim;
//					if (points.size() == 0) break;
//					for (point = points; point->nextpt != NOEPT; point = point->nextpt)
//					{
//						if (geometry == GNET || geometry == GBUS)
//						{
//							// create a pin to pin connection
//							if (fcnt == 0)
//							{
//								// look for the first pin
//								fcnt = io_edfindport(current_cell, point->x,
//									point->y, sch_wirearc, fnis, fpps);
//								if (fcnt == 0)
//								{
//									// create the "from" pin
//									fnis[0] = io_edifiplacepin(np, point->x + np->lowx, point->x + np->highx,
//										point->y + np->lowy, point->y + np->highy,
//										0, 0, current_cell);
//									if (fnis[0] == NONODEINST) fcnt = 0; else
//									{
//										fpps[0] = (geometry == GBUS || isarray) ?
//											default_busport : default_port;
//										fcnt = 1;
//									}
//								}
//							}
//							// now the second ...
//							tcnt = io_edfindport(current_cell, point->nextpt->x,
//								point->nextpt->y, sch_wirearc, tnis, tpps);
//							if (tcnt == 0)
//							{
//								// create the "to" pin
//								tnis[0] = io_edifiplacepin(np, point->nextpt->x + np->lowx,
//									point->nextpt->x + np->highx,
//									point->nextpt->y + np->lowy,
//									point->nextpt->y + np->highy,
//									0, 0, current_cell);
//								if (tnis[0] == NONODEINST) tcnt = 0; else
//								{
//									tpps[0] = (geometry == GBUS || isarray) ?
//										default_busport : default_port;
//									tcnt = 1;
//								}
//							}
//	
//							if (tcnt == 0 || fcnt == 0)
//							{
//								System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create path");
//								errors++;
//							} else
//							{
//								// connect it
//								for (count = 0; count < fcnt || count < tcnt; count++)
//								{
//									if (count < fcnt)
//									{
//										lastpin = fnis[count];
//										lastport = fpps[count];
//	
//										// check node for array variable
//										var = getvalkey((INTBIG)lastpin, VNODEINST, VSTRING, EDIF_array_key);
//										if (var != NOVARIABLE) fbus = 1; else
//											if (lastport->protoname[estrlen(lastport->protoname)-1] == ']') fbus = 1; else
//												fbus = 0;
//									}
//									if (count < tcnt)
//									{
//										ni = tnis[count];
//										pp = tpps[count];
//	
//										// check node for array variable
//										var = getvalkey((INTBIG)ni, VNODEINST, VSTRING, EDIF_array_key);
//										if (var != NOVARIABLE) tbus = 1; else
//											if (pp->protoname[estrlen(pp->protoname)-1] == ']') tbus = 1; else
//												tbus = 0;
//									}
//	
//									// if bus to bus
//									if ((lastport == default_busport || fbus) &&
//										(pp == default_busport || tbus)) ap = sch_busarc;
//											// wire to other
//									else ap = sch_wirearc;
//	
//									ai = newarcinst(ap, defaultarcwidth(ap), FIXANG|CANTSLIDE,
//										lastpin, lastport, point->x, point->y,
//										ni, pp, point->nextpt->x, point->nextpt->y,
//										current_cell);
//									if (ai == NOARCINST)
//									{
//										System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create path (arc)");
//										errors++;
//									} else if (geometry == GNET && points == point)
//									{
//										if (net_reference.length() > 0)
//										{
//											var = setvalkey((INTBIG)ai, VARCINST, EDIF_name_key,
//												(INTBIG)net_reference, VSTRING);
//										}
//										if (net_name.length() > 0)
//										{
//											// set name of arc but don't display name
//											var = setvalkey((INTBIG)ai, VARCINST, el_arc_name_key,
//												(INTBIG)net_name, VSTRING);
//											if (var != NOVARIABLE)
//												defaulttextsize(4, var->textdescript);
//										}
//									} else if (geometry == GBUS && points == point)
//									{
//										if (bundle_reference.length() > 0)
//										{
//											var = setvalkey((INTBIG)ai, VARCINST, EDIF_name_key,
//												(INTBIG)bundle_reference, VSTRING);
//										}
//										if (bundle_name.length() > 0)
//										{
//											// set bus' EDIF name but don't display name
//											var = setvalkey((INTBIG)ai, VARCINST, el_arc_name_key,
//												(INTBIG)bundle_name, VSTRING);
//											if (var != NOVARIABLE)
//												defaulttextsize(4, var->textdescript);
//										}
//									}
//								}
//								if (ai != NOARCINST) current_arc = ai;
//								for (count = 0; count < tcnt; count++)
//								{
//									fnis[count] = tnis[count];
//									fpps[count] = tpps[count];
//								}
//								fcnt = tcnt;
//							}
//						} else
//						{
//							// rectalinear paths with some width
//							// create a path from here to there (orthogonal only now)
//							lx = hx = point->x;
//							ly = hy = point->y;
//							if (lx > point->nextpt->x) lx = point->nextpt->x;
//							if (hx < point->nextpt->x) hx = point->nextpt->x;
//							if (ly > point->nextpt->y) ly = point->nextpt->y;
//							if (hy < point->nextpt->y) hy = point->nextpt->y;
//							if (ly == hy || extend_end)
//							{
//								ly -= path_width/2;
//								hy += path_width/2;
//							}
//							if (lx == hx || extend_end)
//							{
//								lx -= path_width/2;
//								hx += path_width/2;
//							}
//							ni = io_edifiplacepin(figure_group, lx, hx, ly, hy,
//								io_edgettrans(orientation),
//								io_edgetrot(orientation),
//								current_cell);
//							if (ni == NONODEINST)
//							{
//								System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create path");
//								errors++;
//							}
//						}
//					}
//					io_edfreeptlist();
				} else if (state == KCIRCLE)
				{
					if (points.size() == 2)
					{
						EPT p0 = (EPT)points.get(0);
						EPT p1 = (EPT)points.get(1);
						double lx = Math.min(p0.x, p1.x);
						double hx = Math.max(p0.x, p1.x);
						double ly = Math.min(p0.y, p1.y);
						double hy = Math.max(p0.y, p1.y);
						if (lx == hx)
						{
							lx -= (hy - ly) / 2;
							hx += (hy - ly) / 2;
						} else
						{
							ly -= (hx - lx) / 2;
							hy += (hx - lx) / 2;
						}
						double sX = hx - lx;
						double sY = hy - ly;
						if (getMirrorX(orientation)) sX = -sX;
						if (getMirrorY(orientation)) sY = -sY;

						// create the node instance
						NodeInst ni = NodeInst.makeInstance(Artwork.tech.circleNode, new Point2D.Double((lx+hx)/2,(ly+hy)/2),
							sX, sY, current_cell, io_edgetrot(orientation), null, 0);
						if (ni == null)
						{
							System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create circle");
							errors++;
						}
					}
					io_edfreeptlist();
				} else if (state == KNAME)
				{
					// save the data and break
					io_edfreesavedptlist();
					save_string = string;
					string = "";
					save_points = points;
					points = new ArrayList();
					save_textheight = textheight;
					textheight = 0;
					save_justification = justification;
					justification = LOWERLEFT;
					save_orientation = orientation;
					orientation = OR0;
					save_visible = visible;
					visible = true;
				} else if (state == KSTRINGDISPLAY)
				{
//					if (kstack_ptr <= 1)
//					{
//						System.out.println("error, line #" + lineReader.getLineNumber() + ": bad location for \"stringDisplay\"");
//						errors++;
//					}
//					else if (kstack[kstack_ptr-1] == KRENAME)
//					{
//						// save the data and break
//						io_edfreesavedptlist();
//						save_string = string;
//						string = "";
//						save_points = points;
//						points = new ArrayList();
//						save_textheight = textheight;
//						textheight = 0;
//						save_justification = justification;
//						justification = LOWERLEFT;
//						save_orientation = orientation;
//						orientation = OR0;
//						save_visible = visible;
//						visible = true;
//					}
//	
//					// output the data for annotate (display graphics) and string (on properties)
//					else if (kstack[kstack_ptr-1] == KANNOTATE || kstack[kstack_ptr-1] == KSTRING)
//					{
//						// supress this if it starts with "["
//						if (!string.startsWith("[") && points.size() != 0)
//						{
//							// see if a pre-existing node or arc exists to add text
//							ai = NOARCINST;
//							ni = NONODEINST;
//							if (property_reference.length() > 0 && current_node != null)
//							{
//								ni = current_node;
//								key = makekey(property_reference);
//								xoff = points->x - ((ni->highx+ni->lowx)>>1);
//								yoff = points->y - ((ni->highy+ni->lowy)>>1);
//							}
//							else if (property_reference.length() > 0 && current_arc != null)
//							{
//								ai = current_arc;
//								key = makekey(property_reference);
//								xoff = points->x - ((ai->end[0].xpos + ai->end[1].xpos)>>1);
//								yoff = points->y - ((ai->end[0].ypos + ai->end[1].ypos)>>1);
//							} else
//							{
//								// create the node instance
//								xoff = yoff = 0;
//								ni = newnodeinst(gen_invispinprim,
//									points->x, points->x,
//									points->y, points->y, 0, 0,
//									current_cell);
//								key = EDIF_annotate_key;
//							}
//							if (ni != NONODEINST || ai != NOARCINST)
//							{
//								if (ni != NONODEINST)
//								{
//									var = setvalkey((INTBIG)ni, VNODEINST, key, (INTBIG)string,
//										(visible ? VSTRING|VDISPLAY : VSTRING));
//	
//									// now set the position, relative to the center of the current object
//									xoff = points->x - ((ni->highx+ni->lowx)>>1);
//									yoff = points->y - ((ni->highy+ni->lowy)>>1);
//								} else
//								{
//									var = setvalkey((INTBIG)ai, VARCINST, key, (INTBIG)string,
//										(visible ? VSTRING|VDISPLAY : VSTRING));
//	
//									// now set the position, relative to the center of the current object
//									xoff = points->x - ((ai->end[0].xpos + ai->end[1].xpos)>>1);
//									yoff = points->y - ((ai->end[0].ypos + ai->end[1].ypos)>>1);
//								}
//	
//								// convert to quarter lambda units
//								xoff = 4*xoff / lambda;
//								yoff = 4*yoff / lambda;
//	
//								// determine the size of text, 0.0278 in == 2 points or 36 (2xpixels) == 1 in fonts range from 4 to 31
//								if (textheight == 0) i = TXTSETQLAMBDA(4); else
//								{
//									i = io_ediftextsize(textheight);
//								}
//								TDCOPY(descript, var->textdescript);
//								TDSETSIZE(descript, i);
//								TDSETOFF(descript, xoff, yoff);
//								TDSETPOS(descript, VTPOSCENT);
//								switch (justification)
//								{
//									case UPPERLEFT:    TDSETPOS(descript, VTPOSUPRIGHT);    break;
//									case UPPERCENTER:  TDSETPOS(descript, VTPOSUP);         break;
//									case UPPERRIGHT:   TDSETPOS(descript, VTPOSUPLEFT);     break;
//									case CENTERLEFT:   TDSETPOS(descript, VTPOSRIGHT);      break;
//									case CENTERCENTER: TDSETPOS(descript, VTPOSCENT);       break;
//									case CENTERRIGHT:  TDSETPOS(descript, VTPOSLEFT);       break;
//									case LOWERLEFT:    TDSETPOS(descript, VTPOSDOWNRIGHT);  break;
//									case LOWERCENTER:  TDSETPOS(descript, VTPOSDOWN);       break;
//									case LOWERRIGHT:   TDSETPOS(descript, VTPOSDOWNLEFT);   break;
//								}
//								TDCOPY(var->textdescript, descript);
//							} else
//							{
//								System.out.println("error, line #" + lineReader.getLineNumber() + ": nothing to attach text to");
//								errors++;
//							}
//						}
//					}
//	
//					// clean up DISPLAY attributes
//					io_edfreeptlist();
//					cur_nametbl = NONAMETABLE;
//					visible = true;
//					justification = LOWERLEFT;
//					textheight = 0;
				} else if (state == KDOT)
				{
//					if (geometry == GPIN)
//					{
//						for (eport = ports; eport != NOEDPORT; eport = eport->next)
//						{
//							if (!namesame(eport->reference, name)) break;
//						}
//						if (eport != NOEDPORT)
//						{
//							// create a internal wire port using the pin-proto, and create the port and export
//							ni = io_edifiplacepin(cellRefProto,
//								points->x+cellRefProto->lowx,
//								points->x+cellRefProto->highx,
//								points->y+cellRefProto->lowy,
//								points->y+cellRefProto->highy,
//								io_edgettrans(orientation),
//								io_edgetrot(orientation),
//								current_cell);
//							if (ni == NONODEINST)
//							{
//								System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create pin");
//								errors++;
//							}
//							(void)estrcpy(portname, eport->name);
//							for (dup = 0, (void)estrcpy(basename, portname);
//								(ppt = getportproto(ni->parent, portname)) != NOPORTPROTO; dup++)
//							{
//								if (dup == 0) pp = ppt;
//								(void)esnprintf(portname, WORD+1, x_("%s_%ld"), basename, dup+1);
//							}
//							ppt = newportproto(ni->parent, ni, default_iconport, portname);
//							if (ppt == NOPORTPROTO)
//							{
//								System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create port <" + portname + ">");
//								errors++;
//							} else
//							{
//								// set the direction
//								switch (eport->direction)
//								{
//									case INPUTE:
//										ppt->userbits = (ppt->userbits & ~STATEBITS) | INPORT;
//										break;
//									case OUTPUTE:
//										ppt->userbits = (ppt->userbits & ~STATEBITS) | OUTPORT;
//										break;
//									case INOUT:
//										ppt->userbits = (ppt->userbits & ~STATEBITS) | BIDIRPORT;
//										break;
//								}
//							}
//						}
//					} else
//					{
//						// create the node instance
//						ni = newnodeinst(figure_group != null ? figure_group : art_boxprim,
//							points->x, points->x,
//							points->y, points->y,
//							io_edgettrans(orientation),
//							io_edgetrot(orientation),
//							current_cell);
//						if (ni == NONODEINST)
//						{
//							System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create rectangle");
//							errors++;
//						}
//					}
//					io_edfreeptlist();
				} else if (state == KRECTANGLE)
				{
					if (kstack_ptr > 1 && (kstack[kstack_ptr-1] == KPAGESIZE ||
						kstack[kstack_ptr-1] == KBOUNDINGBOX)) return;
					if (points.size() == 2)
					{
						// create the node instance
						EPT p0 = (EPT)points.get(0);
						EPT p1 = (EPT)points.get(1);
						double hx = p1.x;
						double lx = p0.x;
						if (p0.x > p1.x)
						{
							lx = p1.x;
							hx = p0.x;
						}
						double hy = p1.y;
						double ly = p0.y;
						if (p0.y > p1.y)
						{
							ly = p1.y;
							hy = p0.y;
						}
						double sX = hx - lx;
						double sY = hy - ly;
						if (getMirrorX(orientation)) sX = -sX;
						if (getMirrorY(orientation)) sY = -sY;
						NodeInst ni = NodeInst.makeInstance(figure_group != null ? figure_group : Artwork.tech.boxNode,
							new Point2D.Double((lx+hx)/2, (ly+hy)/2), sX, sY, current_cell, io_edgetrot(orientation), null, 0);
						if (ni == null)
						{
							System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create rectangle");
							errors++;
						}
						else if (figure_group == Artwork.tech.openedDottedPolygonNode)
						{
//							cnt = 5;
//							cx = (p0.x + p1.x) / 2;
//							cy = (p0.y + p1.y) / 2;
//							pts[0] = p0.x-cx;
//							pts[1] = p0.y-cy;
//							pts[2] = p0.x-cx;
//							pts[3] = p1.y-cy;
//							pts[4] = p1.x-cx;
//							pts[5] = p1.y-cy;
//							pts[6] = p1.x-cx;
//							pts[7] = p0.y-cy;
//							pts[8] = p0.x-cx;
//							pts[9] = p0.y-cy;
//	
//							// store the trace information
//							(void)setvalkey((INTBIG)ni, VNODEINST, el_trace_key, (INTBIG)pts,
//								VINTEGER|VISARRAY|((cnt*2)<<VLENGTHSH));
						}
						else if (geometry == GPIN)
						{
							// create a rectangle using the pin-proto, and create the port and export ensure full sized port
							sX = cellRefProto.getDefWidth();
							sY = cellRefProto.getDefHeight();
							if (getMirrorX(orientation)) sX = -sX;
							if (getMirrorY(orientation)) sY = -sY;
							ni = io_edifiplacepin(cellRefProto, (lx+hx)/2, (ly+hy)/2, sX, sY,
								io_edgetrot(orientation), current_cell);
							if (ni == null)
							{
								System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create pin");
								errors++;
							}
							PortProto ppt = current_cell.findPortProto(name);
							if (ppt == null)
							{
								PortInst pi = ni.findPortInstFromProto(default_iconport);
								ppt = Export.newInstance(current_cell, pi, name);
							}
							if (ppt == null)
							{
								System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create port <" + name + ">");
								errors++;
							}
						}
					}
					io_edfreeptlist();
				} else if (state == KARC)
				{
//					// set array values
//					for (point = points, i = 0; point && i < 3; point = point->nextpt,i++)
//					{
//						x[i] = point->x;
//						y[i] = point->y;
//					}
//					io_ededif_arc(x, y, &cx, &cy, &radius, &so, &ar, &j, &trans);
//					lx = cx - radius;   hx = cx + radius;
//					ly = cy - radius;   hy = cy + radius;
//	
//					// get the bounds of the circle
//					ni = newnodeinst(figure_group != null && figure_group != art_boxprim ?
//						figure_group : art_circleprim, lx, hx, ly, hy,
//						trans, j, current_cell);
//					if (ni == NONODEINST)
//					{
//						System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create arc");
//						errors++;
//					} else
//					{
//						// store the angle of the arc
//						setarcdegrees(ni, so, ar*EPI/1800.0);
//					}
//					io_edfreeptlist();
				} else if (state == KSYMBOL || state == KPAGE)
				{
//					np = current_cell;
//					if (np != NONODEPROTO)
//					{
//						// now compute the bounds of this cell
//						db_boundcell(np, &np->lowx, &np->highx, &np->lowy, &np->highy);
//					}
					active_view = VNULL;
				} else if (state == KOPENSHAPE || state == KPOLYGON)
				{
//	dopoly:
//					if (points.size() == 0) break;
//	
//					// get the bounds of the poly
//					lx = hx = points->x;
//					ly = hy = points->y;
//					point = points->nextpt;
//					while (point)
//					{
//						if (lx > point->x) lx = point->x;
//						if (hx < point->x) hx = point->x;
//						if (ly > point->y) ly = point->y;
//						if (hy < point->y) hy = point->y;
//						point = point->nextpt;
//					}
//					if (lx != hx || ly != hy)
//					{
//						if (figure_group != null && figure_group != art_boxprim)
//							np = figure_group; else
//						{
//							if (state == KPOLYGON) np = art_closedpolygonprim; else
//								np = art_openedpolygonprim;
//						}
//						ni = newnodeinst(np, lx, hx, ly, hy, io_edgettrans(orientation),
//							io_edgetrot(orientation), current_cell);
//						if (ni == NONODEINST)
//						{
//							System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create polygon");
//							errors++;
//						} else
//						{
//							pt = trace = emalloc((points.size()*2*SIZEOFINTBIG), el_tempcluster);
//							if (trace == 0) RET_NOMEMORY();
//							cx = (hx + lx) / 2;
//							cy = (hy + ly) / 2;
//							point = points;
//							while (point != NOEPT)
//							{
//								*pt++ = point->x - cx;
//								*pt++ = point->y - cy;
//								point = point->nextpt;
//							}
//	
//							// store the trace information
//							(void)setvalkey((INTBIG)ni, VNODEINST, el_trace_key, (INTBIG)trace,
//								VINTEGER|VISARRAY|((points.size()*2)<<VLENGTHSH));
//	
//							// free the polygon memory
//							efree((CHAR *)trace);
//						}
//					}
//					io_edfreeptlist();
				} else if (state == KARRAY)
				{
					if (arrayx == 0) arrayx = 1;
					if (arrayy == 0) arrayy = 1;
				} else if (state == KMEMBER)
				{
					if (memberx != -1)
					{
						// adjust the name of the current INSTANCE/NET/PORT(/VALUE)
						String baseName = "[" + memberx + "]";
						if (membery != -1) baseName = "[" + memberx + "," + membery + "]";
						if (kstack[kstack_ptr-1] == KINSTANCEREF) instance_reference = baseName; else
							if (kstack[kstack_ptr-1] == KPORTREF) port_reference = baseName;
					}
				} else if (state == KDESIGN)
				{
					if (cellRefProto != null)
					{
//						library->curnodeproto = cellRefProto;
					}
				} else if (state == KEDIF)
				{
					// free the netport list
					io_edfreenetports();
				}
			}
			if (kstack_ptr != 0) state = kstack[--kstack_ptr]; else
				state = KINIT;
			return;
		}
	}

	/****************************************** PARSING TABLE ******************************************/

	private static HashMap edifKeys = new HashMap();

	private class EDIFKEY
	{
		private String name;							/* the name of the keyword */
		private EDIFKEY [] stateArray;						/* edif state */

		private EDIFKEY(String name, EDIFKEY [] stateArray)
		{
			this.name = name;
			this.stateArray = stateArray;
			edifKeys.put(name, this);
		}

		private int func() throws IOException { return 0; }
	};

	private EDIFKEY KUNKNOWN = new EDIFKEY("", null);

	private EDIFKEY KINIT = new EDIFKEY("", null);
	
	private EDIFKEY KANNOTATE = new EDIFKEY("annotate", null);

	private EDIFKEY KARC = new EDIFKEY("arc", null);

	private EDIFKEY KARRAY = new KeyArray();
	private class KeyArray extends EDIFKEY
	{
		private KeyArray() { super("array", new EDIFKEY [] {KINSTANCE, KPORT, KNET}); }
		private int func()
			throws IOException
		{
			// note array is a special process integer value function
			isarray = true;
			arrayx = arrayy = 0;
			deltaxX = deltaxY = 0;
			deltayX = deltayY = 0;
			deltapts = 0;
			if (io_edcheck_name())
			{
				if (kstack[kstack_ptr-1] == KCELL)
				{
					cell_reference = name;
					cell_name = name;
				} else if (kstack[kstack_ptr-1] == KPORT)
				{
					port_name = name;
					port_reference = name;
				} else if (kstack[kstack_ptr-1] == KINSTANCE)
				{
					instance_name = name;
					instance_reference = name;
				} else if (kstack[kstack_ptr-1] == KNET)
				{
					net_reference = name;
					net_name = name;
				} else if (kstack[kstack_ptr-1] == KPROPERTY)
				{
					property_reference = name;
					property_name = name;
				}
			}
			return 0;
		}
	}

	private EDIFKEY KAUTHOR = new KeyAuthor();
	private class KeyAuthor extends EDIFKEY { private KeyAuthor() { super("author", new EDIFKEY [] {KWRITTEN}); } }

	private EDIFKEY KBOUNDINGBOX = new KeyBoundingBox();
	private class KeyBoundingBox extends EDIFKEY
	{
		private KeyBoundingBox() { super("boundingBox", new EDIFKEY [] {KSYMBOL, KCONTENTS}); }
		private int func()
			throws IOException
		{
			figure_group = Artwork.tech.openedDottedPolygonNode;
			return 0;
		}
	}

	private EDIFKEY KCELL = new KeyCell();
	private class KeyCell extends EDIFKEY
	{
		private KeyCell() { super("cell", new EDIFKEY [] {KEXTERNAL, KLIBRARY}); }
		private int func()
			throws IOException
		{
			active_view = VNULL;
			name = "";   original = "";
			pageno = 0;
			sh_xpos = sh_ypos = -1;
			if (io_edcheck_name())
			{
				cell_reference = name;
				cell_name = name;
			}
			return 0;
		}
	}

	private EDIFKEY KCELLREF = new KeyCellRef();
	private class KeyCellRef extends EDIFKEY
	{
		private KeyCellRef() { super("cellRef", new EDIFKEY [] {KDESIGN, KVIEWREF, KINSTANCE}); }
		private int func()
			throws IOException
		{
			// get the name of the cell
			String aName = io_edget_token((char)0);

			String view = "lay";
			if (active_view != VMASKLAYOUT) 
			{
				if (kstack[kstack_ptr - 1] == KDESIGN) view = "p1"; else
					view = "ic";
			}
			if (vendor == EVVIEWLOGIC && aName.equalsIgnoreCase("SPLITTER"))
			{
				cellRefProto = null;
				return(0);
			}
		
			// look for this cell name in the cell list
			NAMETABLE nt = (NAMETABLE)celltbl.get(aName);
			if (nt != null)
			{
				aName = nt.replace;
			} else
			{
				System.out.println("could not find cellRef <" + aName + ">");
			}

			// now look for this cell in the list of cells, if not found create it
			// locate this in the list of cells
			Cell proto = null;
			for(Iterator it = library.getCells(); it.hasNext(); )
			{
				Cell cell = (Cell)it.next();
				if (cell.getName().equalsIgnoreCase(aName) &&
					cell.getView().getAbbreviation().equalsIgnoreCase(view)) { proto = cell;   break; }
			}
			if (proto == null)
			{
				// allocate the cell
				if (view.length() > 0) aName = "{" + view + "}";
				proto = Cell.makeInstance(library, aName);
				if (proto == null) throw new IOException("Error creating cell");
//				proto->temp1 = 0;
			}
		
			// set the parent
			cellRefProto = proto;
			return 0;
		}
	}

	private EDIFKEY KCELLTYPE = new EDIFKEY("cellType", new EDIFKEY [] {KCELL});

	private EDIFKEY KCIRCLE = new KeyCircle();
	private class KeyCircle extends EDIFKEY
	{
		private KeyCircle() { super("circle", null); }
		private int func()
			throws IOException
		{
			io_edfreeptlist();
			orientation = OR0;
			return 0;
		}
	}

	private EDIFKEY KCOLOR = new EDIFKEY("color", null);

	private EDIFKEY KCOMMENT = new EDIFKEY("comment", null);

	private EDIFKEY KCOMMENTGRAPHICS = new EDIFKEY("commentGraphics", null);

	private EDIFKEY KCONNECTLOCATION = new EDIFKEY("connectLocation", null);

	private EDIFKEY KCONTENTS = new KeyContents();
	private class KeyContents extends EDIFKEY { private KeyContents() { super("contents", new EDIFKEY [] {KVIEW}); } }

	private EDIFKEY KCORNERTYPE = new KeyCornerType();
	private class KeyCornerType extends EDIFKEY
	{
		private KeyCornerType() { super("cornerType", null); }
		private int func()
			throws IOException
		{
			// get the endtype
			io_edget_token((char)0);
			return 0;
		}
	}

	private EDIFKEY KCURVE = new EDIFKEY("curve", null);

	private EDIFKEY KDATAORIGIN = new EDIFKEY("dataOrigin", null);

	private EDIFKEY KDCFANOUTLOAD = new EDIFKEY("dcFanoutLoad", null);

	private EDIFKEY KDCMAXFANOUT = new EDIFKEY("dcMaxFanout", null);

	private EDIFKEY KDELTA = new KeyDelta();
	private class KeyDelta extends EDIFKEY
	{
		private KeyDelta() { super("delta", null); }
		private int func()
			throws IOException
		{
			deltaxX = deltaxY = 0;
			deltayX = deltayY = 0;
			deltapts = 0;
			return 0;
		}
	}

	private EDIFKEY KDESIGN = new KeyDesign();
	private class KeyDesign extends EDIFKEY
	{
		private KeyDesign() { super("design", new EDIFKEY [] {KEDIF}); }
		private int func()
			throws IOException
		{
			// get the name of the cell
			String aName = io_edget_token((char)0);
			return 0;
		}
	}

	private EDIFKEY KDESIGNATOR = new EDIFKEY("designator", null);

	private EDIFKEY KDIRECTION = new KeyDirection();
	private class KeyDirection extends EDIFKEY
	{
		private KeyDirection() { super("direction", new EDIFKEY [] {KPORT}); }
		private int func()
			throws IOException
		{
			// get the direction
			String aName = io_edget_token((char)0);
			
			if (aName.equalsIgnoreCase("INOUT")) direction = INOUT; else
				if (aName.equalsIgnoreCase("INPUT")) direction = INPUTE; else
					if (aName.equalsIgnoreCase("OUTPUT")) direction = OUTPUTE;
			return 0;
		}
	}

	private EDIFKEY KDISPLAY = new KeyDisplay();
	private class KeyDisplay extends EDIFKEY
	{
		private KeyDisplay() { super("display", null); }
		private int func()
			throws IOException
		{
			io_edfigure();
			return 0;
		}
	}

	private EDIFKEY KDOT = new KeyDot();
	private class KeyDot extends EDIFKEY
	{
		private KeyDot() { super("dot", null); }
		private int func()
			throws IOException
		{
			io_edfreeptlist();
			orientation = OR0;
			return 0;
		}
	}

	private EDIFKEY KSCALEDINTEGER = new EDIFKEY("e", null);

	private EDIFKEY KEDIF = new KeyEDIF();
	private class KeyEDIF extends EDIFKEY { private KeyEDIF() { super("edif", new EDIFKEY [] {KINIT}); } }

	private EDIFKEY KEDIFLEVEL = new KeyEDIFLevel();
	private class KeyEDIFLevel extends EDIFKEY { private KeyEDIFLevel() { super("edifLevel", new EDIFKEY [] {KEDIF, KEXTERNAL, KLIBRARY}); } }

	private EDIFKEY KEDIFVERSION = new EDIFKEY("edifVersion", new EDIFKEY [] {KEDIF});

	private EDIFKEY KENDTYPE = new KeyEndType();
	private class KeyEndType extends EDIFKEY
	{
		private KeyEndType() { super("endType", null); }
		private int func()
			throws IOException
		{
			// get the endtype
			String type = io_edget_token((char)0);
			
			if (type.equalsIgnoreCase("EXTEND")) extend_end = true;
			return 0;
		}
	}

	private EDIFKEY KEXTERNAL = new KeyExternal();
	private class KeyExternal extends EDIFKEY
	{
		private KeyExternal() { super("external", new EDIFKEY [] {KEDIF}); }
		private int func()
			throws IOException
		{
			// get the name of the library
			String aName = io_edget_token((char)0);
			return 0;
		}
	}

	private EDIFKEY KFABRICATE = new KeyFabricate();
	private class KeyFabricate extends EDIFKEY
	{
		private KeyFabricate() { super("fabricate", null); }
		private int func()
			throws IOException
		{
			int pos = layer_ptr;
		
			nametbl[pos] = new NAMETABLE();
		
			// first get the original and replacement layers
			nametbl[pos].original = io_edget_token((char) 0);
			nametbl[pos].replace = io_edget_token((char) 0);
			nametbl[pos].textheight = 0;
			nametbl[pos].justification = LOWERLEFT;
			nametbl[pos].visible = true;
		
			// now bump the position
			layer_ptr++;
			return 0;
		}
	}

	private EDIFKEY KFALSE = new KeyFalse();
	private class KeyFalse extends EDIFKEY
	{
		private KeyFalse() { super("false", null); }
		private int func()
			throws IOException
		{
			// check previous keyword
			if (kstack_ptr > 1 && kstack[kstack_ptr-1] == KVISIBLE)
			{
				visible = false;
				if (cur_nametbl != null)
					cur_nametbl.visible = false;
			}
			return 0;
		}
	}

	private EDIFKEY KFIGURE = new KeyFigure();
	private class KeyFigure extends EDIFKEY
	{
		private KeyFigure() { super("figure", null); }
		private int func()
			throws IOException
		{
			io_edfigure();
			return 0;
		}
	}

	private EDIFKEY KFIGUREGROUP = new EDIFKEY("figureGroup", null);

	private EDIFKEY KFIGUREGROUPOVERRIDE = new KeyFigureGroupOverride();
	private class KeyFigureGroupOverride extends EDIFKEY
	{
		private KeyFigureGroupOverride() { super("figureGroupOverride", null); }
		private int func()
			throws IOException
		{
			// get the layer name
			String layer = io_edget_token((char)0);
		
			// now look for this layer in the list of layers
			for(int i=0; i<MAXLAYERS; i++)
			{
				NAMETABLE nt = nametbl[i];
				if (nt == null) continue;
				if (nt.original.equalsIgnoreCase(layer))
				{
					// found the layer
					figure_group = nametbl[i].node;
					arc_type = nametbl[i].arc;
					textheight = nametbl[i].textheight;
					justification = nametbl[i].justification;
					visible = nametbl[i].visible;
					return 0;
				}
			}
		
			// insert and resort the list
			pos = layer_ptr;
	
			nametbl[pos] = new NAMETABLE();
			nametbl[pos].original = layer;
	
			nametbl[pos].replace = nametbl[pos].original;
			figure_group = nametbl[pos].node = Artwork.tech.boxNode;
			arc_type = nametbl[pos].arc = Schematics.tech.wire_arc;
			textheight = nametbl[pos].textheight = 0;
			justification = nametbl[pos].justification = LOWERLEFT;
			visible = nametbl[pos].visible = true;
	
			// now sort the list
			layer_ptr++;
//			esort(nametbl, layer_ptr, sizeof(NAMETABLE_PTR), io_edcompare_name);
			return 0;
		}
	}

	private EDIFKEY KFILLPATTERN = new EDIFKEY("fillpattern", null);

	private EDIFKEY KGRIDMAP = new EDIFKEY("gridMap", null);

	private EDIFKEY KINSTANCE = new KeyInstance();
	private class KeyInstance extends EDIFKEY
	{
		private KeyInstance() { super("instance", new EDIFKEY [] {KCONTENTS, KPAGE, KPORTIMPLEMENTATION, KCOMMENTGRAPHICS}); }
		private int func()
			throws IOException
		{
			// set the current geometry type
			io_edfreeptlist();
			cellRefProto = null;
			geometry = GINSTANCE;
			orientation = OR0;
			isarray = false;
			arrayx = arrayy = 1;
			name = "";   original = "";
			instance_reference = "";
			current_node = null;
			if (io_edcheck_name())
			{
				instance_reference = name;
				instance_name = name;
			}
			return 0;
		}
	}

	private EDIFKEY KINSTANCEREF = new KeyInstanceRef();
	private class KeyInstanceRef extends EDIFKEY
	{
		private KeyInstanceRef() { super("instanceRef", new EDIFKEY [] {KINSTANCEREF, KPORTREF}); }
		private int func()
			throws IOException
		{
			instance_reference = "";
			if (io_edcheck_name())
			{
				instance_reference = name;
			}
			return 0;
		}
	}

	private EDIFKEY KINTEGER = new KeyInteger();
	private class KeyInteger extends EDIFKEY
	{
		private KeyInteger() { super("integer", null); }
		private int func()
			throws IOException
		{
			String value = io_edget_token((char)0);
			
			pval_integer = TextUtils.atoi(value);
			ptype = PINTEGER;
			return 0;
		}
	}

	private EDIFKEY KINTERFACE = new KeyInterface();
	private class KeyInterface extends EDIFKEY
	{
		private KeyInterface() { super("interface", new EDIFKEY [] {KVIEW}); }
		private int func()
			throws IOException
		{
			// create schematic page 1 to represent all I/O for this schematic; locate this in the list of cells
			Cell np = null;
			for(Iterator it = library.getCells(); it.hasNext(); )
			{
				Cell cell = (Cell)it.next();
				if (cell.getName().equals(cell_name) &&
					cell.getView() == View.SCHEMATIC) { np = cell;   break; }
			}
			if (np == null)
			{
				// allocate the cell
				String nodename = cell_name + "{sch}";
				
				current_cell = Cell.makeInstance(library, nodename);
				if (current_cell == null) throw new IOException("Error creating cell");
//				current_cell->temp1 = 0;
			}
			else current_cell = np;
		
			// now set the current position in the schematic page
			ipos = bpos = opos = 0;
			return 0;
		}
	}

	private EDIFKEY KJOINED = new KeyJoined();
	private class KeyJoined extends EDIFKEY { private KeyJoined() { super("joined", new EDIFKEY [] {KINTERFACE, KNET}); }}

	private EDIFKEY KJUSTIFY = new KeyJustify();
	private class KeyJustify extends EDIFKEY
	{
		private KeyJustify() { super("justify", null); }
		private int func()
			throws IOException
		{
			// get the textheight value of the point
			String val = io_edget_token((char)0);
			if (val.equalsIgnoreCase("UPPERLEFT")) justification = UPPERLEFT;
			else if (val.equalsIgnoreCase("UPPERCENTER")) justification = UPPERCENTER;
			else if (val.equalsIgnoreCase("UPPERRIGHT")) justification = UPPERRIGHT;
			else if (val.equalsIgnoreCase("CENTERLEFT")) justification = CENTERLEFT;
			else if (val.equalsIgnoreCase("CENTERCENTER")) justification = CENTERCENTER;
			else if (val.equalsIgnoreCase("CENTERRIGHT")) justification = CENTERRIGHT;
			else if (val.equalsIgnoreCase("LOWERLEFT")) justification = LOWERLEFT;
			else if (val.equalsIgnoreCase("LOWERCENTER")) justification = LOWERCENTER;
			else if (val.equalsIgnoreCase("LOWERRIGHT")) justification = LOWERRIGHT;
			else
			{
				System.out.println("warning, line #" + lineReader.getLineNumber() + ": unknown keyword <" + val + ">");
				warnings++;
				return 0;
			}
		
			if (cur_nametbl != null)
				cur_nametbl.justification = justification;
			return 0;
		}
	}

	private EDIFKEY KKEYWORDDISPLAY = new EDIFKEY("keywordDisplay", null);

	private EDIFKEY KEDIFKEYLEVEL = new EDIFKEY("keywordLevel", null);

	private EDIFKEY KEDIFKEYMAP = new EDIFKEY("keywordMap", new EDIFKEY [] {KEDIF});

	private EDIFKEY KLIBRARY = new KeyLibrary();
	private class KeyLibrary extends EDIFKEY
	{
		private KeyLibrary() { super("library", new EDIFKEY [] {KEDIF}); }
		private int func()
			throws IOException
		{
			// get the name of the library
			String aName = io_edget_token((char)0);
			return 0;
		}
	}

	private EDIFKEY KLIBRARYREF = new EDIFKEY("libraryRef", new EDIFKEY [] {KCELLREF});

	private EDIFKEY KLISTOFNETS = new KeyListOfNets();
	private class KeyListOfNets extends EDIFKEY { private KeyListOfNets() { super("listOfNets", new EDIFKEY [] {KNETBUNDLE}); } }

	private EDIFKEY KLISTOFPORTS = new EDIFKEY("listOfPorts", null);

	private EDIFKEY KMEMBER = new KeyMember();
	private class KeyMember extends EDIFKEY
	{
		private KeyMember() { super("member", null); }
		private int func()
			throws IOException
		{
			memberx = -1; // no member
			membery = -1; // no member
			if (io_edcheck_name())
			{
				if (kstack[kstack_ptr-1] == KPORTREF) port_reference = name; else
					if (kstack[kstack_ptr-1] == KINSTANCEREF) instance_reference = name;
			}
			return 0;
		}
	}

	private EDIFKEY KNAME = new KeyName();
	private class KeyName extends EDIFKEY
	{
		private KeyName() { super("name", null); }
		private int func()
			throws IOException
		{
			int kptr = kstack_ptr - 1;
			if (kstack[kstack_ptr-1] == KARRAY || kstack[kstack_ptr-1] == KMEMBER) kptr = kstack_ptr-2;
			if (io_edcheck_name())
			{
				if (kstack[kptr] == KCELL)
				{
					cell_reference = name;
					cell_name = name;
				} else if (kstack[kptr] == KPORTIMPLEMENTATION || kstack[kptr] == KPORT)
				{
					port_name = name;
					port_reference = name;
				} else if (kstack[kptr] == KPORTREF)
				{
					port_reference = name;
				} else if (kstack[kptr] == KINSTANCE)
				{
					instance_name = name;
					instance_reference = name;
				} else if (kstack[kptr] == KINSTANCEREF)
				{
					instance_reference = name;
				} else if (kstack[kptr] == KNET)
				{
					net_reference = name;
					net_name = name;
				} else if (kstack[kptr] == KPROPERTY)
				{
					property_reference = name;
					property_name = name;
				}
		
				// init the point lists
				io_edfreeptlist();
				orientation = OR0;
				visible = true;
				justification = LOWERLEFT;
				textheight = 0;
				string = name;
			}
			return 0;
		}
	}

	/* net:  Define a net name followed by edifgbl and local pin list
	 *		(net NAME
	 *			(joined
	 *			(portRef NAME (instanceRef NAME))
	 *			(portRef NAME)))
	 */
	private EDIFKEY KNET = new KeyNet();
	private class KeyNet extends EDIFKEY
	{
		private KeyNet() { super("net", new EDIFKEY [] {KCONTENTS, KPAGE, KLISTOFNETS}); }
		private int func()
			throws IOException
		{
			net_reference = "";
			name = "";   original = "";
			current_arc = null;
			current_node = null;
			current_port = null;
			io_edfreenetports();
			isarray = false;
			arrayx = arrayy = 1;
			if (kstack[kstack_ptr-2] != KNETBUNDLE) geometry = GNET;
			if (io_edcheck_name())
			{
				net_reference = name;
				net_name = name;
			}
			return 0;
		}
	}

	private EDIFKEY KNETBUNDLE = new KeyNetBundle();
	private class KeyNetBundle extends EDIFKEY
	{
		private KeyNetBundle() { super("netBundle", new EDIFKEY [] {KCONTENTS, KPAGE}); }
		private int func()
			throws IOException
		{
			geometry = GBUS;
			bundle_reference = "";
			name = "";   original = "";
			isarray = false;
			if (io_edcheck_name())
			{
				bundle_reference = name;
				bundle_name = name;
			}
			return 0;
		}
	}

	private EDIFKEY KNUMBER = new KeyNumber();
	private class KeyNumber extends EDIFKEY
	{
		private KeyNumber() { super("number", null); }
		private int func()
			throws IOException
		{
			pval_number = io_edgetnumber();
			ptype = PNUMBER;
			return 0;
		}
	}

	private EDIFKEY KNUMBERDEFINITION = new KeyNumberDefinition();
	private class KeyNumberDefinition extends EDIFKEY { private KeyNumberDefinition() { super("numberDefinition", new EDIFKEY [] {KTECHNOLOGY}); } }

	private EDIFKEY KOPENSHAPE = new KeyOpenShape();
	private class KeyOpenShape extends EDIFKEY
	{
		private KeyOpenShape() { super("openShape", null); }
		private int func()
			throws IOException
		{
			io_edfreeptlist();
			orientation = OR0;
			return 0;
		}
	}

	private EDIFKEY KORIENTATION = new KeyOrientation();
	private class KeyOrientation extends EDIFKEY
	{
		private KeyOrientation() { super("orientation", null); }
		private int func()
			throws IOException
		{
			// get the orientation keyword
			String orient = io_edget_token((char)0);
		
			if (orient.equalsIgnoreCase("R0")) orientation = OR0;
			else if (orient.equalsIgnoreCase("R90")) orientation = OR90;
			else if (orient.equalsIgnoreCase("R180")) orientation = OR180;
			else if (orient.equalsIgnoreCase("R270")) orientation = OR270;
			else if (orient.equalsIgnoreCase("MY")) orientation = OMY;
			else if (orient.equalsIgnoreCase("MX")) orientation = OMX;
			else if (orient.equalsIgnoreCase("MYR90")) orientation = OMYR90;
			else if (orient.equalsIgnoreCase("MXR90")) orientation = OMXR90;
			else
			{
				System.out.println("warning, line #" + lineReader.getLineNumber() + ": unknown orientation value <" + orient + ">");
				warnings++;
			}
			return 0;
		}
	}

	private EDIFKEY KORIGIN = new EDIFKEY("origin", null);

	private EDIFKEY KOWNER = new EDIFKEY("owner", null);

	private EDIFKEY KPAGE = new KeyPage();
	private class KeyPage extends EDIFKEY
	{
		private KeyPage() { super("page", null); }
		private int func()
			throws IOException
		{
			// check for the name
			io_edcheck_name();
		
			String view = "p" + (++pageno);
		
			// locate this in the list of cells
			Cell proto = null;
			for(Iterator it = library.getCells(); it.hasNext(); )
			{
				Cell cell = (Cell)it.next();
				if (cell.getName().equalsIgnoreCase(cell_name) &&
					cell.getView().getAbbreviation().equalsIgnoreCase(view))
						{ proto = cell;   break; }
			}
			if (proto == null)
			{
				// allocate the cell
				String cname = cell_name + "{" + view + "}";
				proto = Cell.makeInstance(library, cname);
				if (proto == null) throw new IOException("Error creating cell");
//				proto->temp1 = 0;
			} else
			{
//				if (proto->temp1) ignoreblock = true;
			}
		
			current_cell = proto;
			return 0;
		}
	}

	private EDIFKEY KPAGESIZE = new EDIFKEY("pageSize", null);

	private EDIFKEY KPATH = new KeyPath();
	private class KeyPath extends EDIFKEY
	{
		private KeyPath() { super("path", null); }
		private int func()
			throws IOException
		{
			io_edfreeptlist();
			orientation = OR0;
			path_width = 0;
			return 0;
		}
	}

	private EDIFKEY KPATHWIDTH = new KeyPathWidth();
	private class KeyPathWidth extends EDIFKEY
	{
		private KeyPathWidth() { super("pathWidth", null); }
		private int func()
			throws IOException
		{
			// get the width string
			String width = io_edget_token((char)0);
			path_width = TextUtils.atoi(width);
			return 0;
		}
	}

	private EDIFKEY KPOINT = new EDIFKEY("point", null);

	private EDIFKEY KPOINTLIST = new EDIFKEY("pointList", null);

	private EDIFKEY KPOLYGON = new KeyPolygon();
	private class KeyPolygon extends EDIFKEY
	{
		private KeyPolygon() { super("polygon", null); }
		private int func()
			throws IOException
		{
			io_edfreeptlist();
			orientation = OR0;
			return 0;
		}
	}

	private EDIFKEY KPORT = new KeyPort();
	private class KeyPort extends EDIFKEY
	{
		private KeyPort() { super("port", new EDIFKEY [] {KINTERFACE, KLISTOFPORTS}); }
		private int func()
			throws IOException
		{
			name = "";   original = "";
			direction = INPUTE;
			port_reference = "";
			isarray = false;
			arrayx = arrayy = 1;
			if (io_edcheck_name())
			{
				port_reference = name;
				port_name = name;
			}
			return 0;
		}
	}

	private EDIFKEY KPORTBUNDLE = new EDIFKEY("portBundle", null);

	private EDIFKEY KPORTIMPLEMENTATION = new KeyPortImplementation();
	private class KeyPortImplementation extends EDIFKEY
	{
		private KeyPortImplementation() { super("portImplementation", null); }
		private int func()
			throws IOException
		{
			// set the current geometry type
			io_edfreeptlist();
			cellRefProto = Schematics.tech.wirePinNode;
			geometry = GPIN;
			orientation = OR0;
			isarray = false;
			arrayx = arrayy = 1;
			name = "";   original = "";
			io_edcheck_name();
			return 0;
		}
	}

	private EDIFKEY KPORTINSTANCE = new EDIFKEY("portInstance", null);

	private EDIFKEY KPORTLIST = new EDIFKEY("portList", null);

	private EDIFKEY KPORTREF = new KeyPortRef();
	private class KeyPortRef extends EDIFKEY
	{
		private KeyPortRef() { super("portRef", null); }
		private int func()
			throws IOException
		{
			port_reference = "";
			instance_reference = "";
			if (io_edcheck_name())
			{
				port_reference = name;
			}
		
			// allocate a netport
			io_edallocnetport();
			return 0;
		}
	}

	private EDIFKEY KPROGRAM = new KeyProgram();
	private class KeyProgram extends EDIFKEY
	{
		private KeyProgram() { super("program", null); }
		private int func()
			throws IOException
		{
			String program = io_edget_token((char)0);
			if (program.substring(1).startsWith("VIEWlogic"))
			{
				vendor = EVVIEWLOGIC;
			} else if (program.substring(1).startsWith("edifout"))
			{
				vendor = EVCADENCE;
			}
			return 0;
		}
	}

	private EDIFKEY KPROPERTY = new KeyProperty();
	private class KeyProperty extends EDIFKEY
	{
		private KeyProperty() { super("property", null); }
		private int func()
			throws IOException
		{
			property_reference = "";
			name = "";   original = "";
			ptype = PUNKNOWN;
			if (io_edcheck_name())
			{
				property_reference = name;
				property_name = name;
			}
			return 0;
		}
	}

	private EDIFKEY KPROPERTYDISPLAY = new EDIFKEY("propertyDisplay", null);

	private EDIFKEY KPT = new KeyPt();
	private class KeyPt extends EDIFKEY
	{
		private KeyPt() { super("pt", null); }
		private int func()
			throws IOException
		{
			// get the x and y values of the point
			String xstr = io_edget_token((char)0);
			if (xstr == null) throw new IOException("Unexpected end-of-file");
			String ystr = io_edget_token((char)0);
			if (ystr == null) throw new IOException("Unexpected end-of-file");
		
			if (kstack_ptr > 1 && kstack[kstack_ptr-1] == KDELTA)
			{
				double x = TextUtils.atof(xstr) * scale;
				double y = TextUtils.atof(ystr) * scale;
		
				// transform the points per orientation
				if (orientation == OR90)   { double s = x;   x = -y;  y = s; } else
				if (orientation == OR180)  { x = -x;  y = -y; } else
				if (orientation == OR270)  { double s = x;   x = y;   y = -s; } else
				if (orientation == OMY)    { x = -x; } else
				if (orientation == OMX)    { y = -y; } else
				if (orientation == OMYR90) { double s = y;   y = -x;  x = -s; } else
				if (orientation == OMXR90) { double s = x;   x = y;   y = s; }
		
				// set the array deltas
				if (deltapts == 0)
				{
					deltaxX = x;
					deltaxY = y;
				} else
				{
					deltayX = x;
					deltayY = y;
				}
				deltapts++;
			} else
			{
				// allocate a point to read
				EPT point = new EPT();
		
				// and set the values
				point.x = TextUtils.atof(xstr);
				if (point.x > 0) point.x = point.x * scale + 0.5; else
					point.x = point.x * scale - 0.5;
				point.y = TextUtils.atof(ystr);
				if (point.y > 0) point.y = point.y * scale + 0.5; else
					point.y = point.y * scale - 0.5;
				point.nextpt = null;
		
				// add it to the list of points
				points.add(point);
			}
			return 0;
		}
	}

	private EDIFKEY KRECTANGLE = new KeyRectangle();
	private class KeyRectangle extends EDIFKEY
	{
		private KeyRectangle() { super("rectangle", null); }
		private int func()
			throws IOException
		{
			io_edfreeptlist();
			orientation = OR0;
			return 0;
		}
	}

	private EDIFKEY KRENAME = new KeyRename();
	private class KeyRename extends EDIFKEY
	{
		private KeyRename() { super("rename", null); }
		private int func()
			throws IOException
		{
			// get the name of the object
			String aName = io_edget_token((char)0);
			name = aName.startsWith("&") ? aName.substring(1) : aName;
		
			// and the original name
			io_edpos_token();
			char chr = buffer.charAt(pos);
			if (chr == '(')
			{
				// must be stringDisplay, copy name to original
				original = name;
			} else
			{
				aName = io_edget_token((char)0);
		
				// copy name without quotes
				original = aName.substring(1, aName.length()-1);
			}
			int kptr = kstack_ptr;
			if (kstack[kstack_ptr - 1] == KNAME) kptr = kstack_ptr - 1;
			if (kstack[kptr - 1] == KARRAY) kptr = kptr - 2; else
				kptr = kptr -1;
			if (kstack[kptr] == KCELL)
			{
				cell_reference = name;
				cell_name = original;
			} else if (kstack[kptr] == KPORT)
			{
				port_reference = name;
				port_name = original;
			} else if (kstack[kptr] == KINSTANCE)
			{
				instance_reference = name;
				instance_name = original;
			} else if (kstack[kptr] == KNETBUNDLE)
			{
				bundle_reference = name;
				bundle_name = original;
			} else if (kstack[kptr] == KNET)
			{
				net_reference = name;
				net_name = original;
			} else if (kstack[kptr] == KPROPERTY)
			{
				property_reference = name;
				property_name = original;
			}
			return 0;
		}
	}

	private EDIFKEY KSCALE = new EDIFKEY("scale", new EDIFKEY [] {KNUMBERDEFINITION});

	private EDIFKEY KSCALEX = new EDIFKEY("scaleX", null);

	private EDIFKEY KSCALEY = new EDIFKEY("scaleY", null);

	private EDIFKEY KSHAPE = new EDIFKEY("shape", null);

	private EDIFKEY KSTATUS = new KeyStatus();
	private class KeyStatus extends EDIFKEY { private KeyStatus() { super("status", new EDIFKEY [] {KCELL, KDESIGN, KEDIF, KEXTERNAL, KLIBRARY, KVIEW}); } }

	private EDIFKEY KSTRING = new KeyString();
	private class KeyString extends EDIFKEY
	{
		private KeyString() { super("string", null); }
		private int func()
			throws IOException
		{
			io_edpos_token();
			char chr = buffer.charAt(pos);

			if (chr != '(' && chr != ')')
			{
				String value = io_edget_token((char)0);
				if (value == null) throw new IOException("Unexpected end-of-file");
	
				pval_string = value.substring(1, value.length()-1);
				ptype = PSTRING;
			}
			return 0;
		}
	}

	private EDIFKEY KSTRINGDISPLAY = new KeyStringDisplay();
	private class KeyStringDisplay extends EDIFKEY
	{
		private KeyStringDisplay() { super("stringDisplay", null); }
		private int func()
			throws IOException
		{
			// init the point lists
			io_edfreeptlist();
			orientation = OR0;
			visible = true;
			justification = LOWERLEFT;
			textheight = 0;
		
			// get the string, remove the quote
			io_edget_delim('\"');
			String string = io_edget_token('\"');
			if (string == null) throw new IOException("Unexpected end-of-file");
		
			// check for RENAME
			if (kstack[kstack_ptr-1] != KRENAME) return 0;
			original = string;
			int kptr = kstack_ptr - 2;
			if (kstack[kstack_ptr - 2] == KARRAY) kptr = kstack_ptr - 3;
			if (kstack[kptr] == KCELL)
			{
				cell_name = original;
			} else if (kstack[kptr] == KPORT)
			{
				port_name = original;
			} else if (kstack[kptr] == KINSTANCE)
			{
				instance_name = original;
			} else if (kstack[kptr] == KNET)
			{
				net_name = original;
			} else if (kstack[kptr] == KNETBUNDLE)
			{
				bundle_name = original;
			} else if (kstack[kptr] == KPROPERTY)
			{
				property_name = original;
			}
			return 0;
		}
	}

	private EDIFKEY KSYMBOL = new KeySymbol();
	private class KeySymbol extends EDIFKEY
	{
		private KeySymbol() { super("symbol", new EDIFKEY [] {KINTERFACE}); }
		private int func()
			throws IOException
		{
			active_view = VSYMBOL;
		
			// locate this in the list of cells
			Cell proto = null;
			for(Iterator it = library.getCells(); it.hasNext(); )
			{
				Cell cell = (Cell)it.next();
				if (cell.getName().equalsIgnoreCase(cell_name) &&
					cell.getView() == View.ICON) { proto = cell;   break; }
			}
			if (proto == null)
			{
				// allocate the cell
				String cname = cell_name + "{ic}";
				proto = Cell.makeInstance(library, cname);
				if (proto == null) throw new IOException("Error creating cell");
				proto.setWantExpanded();
//				proto->temp1 = 0;
		
			} else
			{
//				if (proto->temp1) ignoreblock = true;
			}
		
			current_cell = proto;
			figure_group = null;
			return 0;
		}
	}

	private EDIFKEY KTECHNOLOGY = new EDIFKEY("technology", new EDIFKEY [] {KEXTERNAL, KLIBRARY});

	private EDIFKEY KTEXTHEIGHT = new KeyTextHeight();
	private class KeyTextHeight extends EDIFKEY
	{
		private KeyTextHeight() { super("textHeight", null); }
		private int func()
			throws IOException
		{
			// get the textheight value of the point
			String val = io_edget_token((char)0);
			textheight = (int)(TextUtils.atoi(val) * scale);
		
			if (cur_nametbl != null)
				cur_nametbl.textheight = textheight;
			return 0;
		}
	}

	private EDIFKEY KTIMESTAMP = new KeyTimeStamp();
	private class KeyTimeStamp extends EDIFKEY { private KeyTimeStamp() { super("timestamp", new EDIFKEY [] {KWRITTEN}); } }

	private EDIFKEY KTRANSFORM = new EDIFKEY("transform", null);

	private EDIFKEY KTRUE = new KeyTrue();
	private class KeyTrue extends EDIFKEY
	{
		private KeyTrue() { super("true", null); }
		private int func()
			throws IOException
		{
			// check previous keyword
			if (kstack_ptr > 1 && kstack[kstack_ptr-1] == KVISIBLE)
			{
				visible = true;
				if (cur_nametbl != null)
					cur_nametbl.visible = true;
			}
			return 0;
		}
	}

	private EDIFKEY KUNIT = new KeyUnit();
	private class KeyUnit extends EDIFKEY
	{
		private KeyUnit() { super("unit", new EDIFKEY [] {KPROPERTY, KSCALE}); }
		private int func()
			throws IOException
		{
			String type = io_edget_token((char)0);
			if (kstack[kstack_ptr-1] == KSCALE && type.equalsIgnoreCase("DISTANCE"))
			{
				// just make the scale be so that the specified number of database units becomes 1 lambda
				scale = 1;
				scale *= IOTool.getEDIFInputScale();
			}
			return 0;
		}
	}

	private EDIFKEY KUSERDATA = new EDIFKEY("userData", null);

	private EDIFKEY KVERSION = new EDIFKEY("version", null);

	private EDIFKEY KVIEW = new KeyView();
	private class KeyView extends EDIFKEY
	{
		private KeyView() { super("view", new EDIFKEY [] {KCELL}); }
		private int func()
			throws IOException
		{
			active_view = VNULL;
			return 0;
		}
	}

	private EDIFKEY KVIEWREF = new KeyViewRef();
	private class KeyViewRef extends EDIFKEY { private KeyViewRef() { super("viewRef", new EDIFKEY [] {KINSTANCE, KINSTANCEREF, KPORTREF}); } }

	/* viewType:  Indicates the view style for this cell, ie
	 *	BEHAVIOR, DOCUMENT, GRAPHIC, LOGICMODEL, MASKLAYOUT, NETLIST,
	 *  	PCBLAYOUT, SCHEMATIC, STRANGER, SYMBOLIC
	 * we are only concerned about the viewType NETLIST.
	 */
	private EDIFKEY KVIEWTYPE = new KeyViewType();
	private class KeyViewType extends EDIFKEY
	{
		private KeyViewType() { super("viewType", new EDIFKEY [] {KVIEW}); }
		private int func()
			throws IOException
		{
			// get the viewType
			String aName = io_edget_token((char)0);

			String view = "";
			if (aName.equalsIgnoreCase("BEHAVIOR")) active_view = VBEHAVIOR;
			else if (aName.equalsIgnoreCase("DOCUMENT")) active_view = VDOCUMENT;
			else if (aName.equalsIgnoreCase("GRAPHIC"))
			{
				active_view = VGRAPHIC;
			}
			else if (aName.equalsIgnoreCase("LOGICMODEL")) active_view = VLOGICMODEL;
			else if (aName.equalsIgnoreCase("MASKLAYOUT"))
			{
				active_view = VMASKLAYOUT;
				view = "lay";
			}
			else if (aName.equalsIgnoreCase("NETLIST"))
			{
				active_view = VNETLIST;
				view = "sch";
			}
			else if (aName.equalsIgnoreCase("PCBLAYOUT")) active_view = VPCBLAYOUT;
			else if (aName.equalsIgnoreCase("SCHEMATIC"))
			{
				active_view = VSCHEMATIC;
				view = "sch";
			}
			else if (aName.equalsIgnoreCase("STRANGER")) active_view = VSTRANGER;
			else if (aName.equalsIgnoreCase("SYMBOLIC")) active_view = VSYMBOLIC;
		
			// immediately allocate MASKLAYOUT and VGRAPHIC viewtypes
			if (active_view == VMASKLAYOUT || active_view == VGRAPHIC ||
				active_view == VNETLIST || active_view == VSCHEMATIC)
			{
				// locate this in the list of cells
				Cell proto = null;
				for(Iterator it = library.getCells(); it.hasNext(); )
				{
					Cell cell = (Cell)it.next();
					if (cell.getName().equalsIgnoreCase(cell_name) &&
						cell.getView().getAbbreviation().equalsIgnoreCase(view)) { proto = cell;   break; }
				}
				if (proto == null)
				{
					// allocate the cell
					String cname = cell_name + "{" + view + "}";
					proto = Cell.makeInstance(library, cname);
					if (proto == null) throw new IOException("Error creating cell");
//					proto->temp1 = 0;
				} else
				{
//					if (proto->temp1) ignoreblock = true;
				}
		
				current_cell = proto;
			}
			else current_cell = null;
		
			// add the name to the celltbl
			NAMETABLE nt = new NAMETABLE();
			nt.original = cell_reference;
			nt.replace = cell_name;
			celltbl.put(cell_reference, nt);
			return 0;
		}
	}

	private EDIFKEY KVISIBLE = new EDIFKEY("visible", null);

	private EDIFKEY KWRITTEN = new EDIFKEY("written", new EDIFKEY [] {KSTATUS});

}
//	
//	/* general utilities ... */
//	static void io_edcheck_busnames(ARCINST *ai, CHAR *base)
//	{
//		PORTARCINST *pai;
//		NODEINST *ni;
//		INTBIG i, l;
//		VARIABLE *var;
//		CHAR newname[WORD+1];
//	
//		if (ai->proto != sch_busarc)
//		{
//			// verify the name
//			var = getvalkey((INTBIG)ai, VARCINST, VSTRING, el_arc_name_key);
//			if (var != NOVARIABLE && ((aName = (CHAR *)var->addr) != NULL))
//			{
//				l = estrlen(base);
//				if (!namesamen(aName, base, l) && isdigit(aName[l]))
//				{
//					(void)esnprintf(newname, WORD+1, x_("%s[%s]"), base, &aName[l]);
//					var = setvalkey((INTBIG)ai, VARCINST, el_arc_name_key, (INTBIG)newname, VSTRING);
//					if (var != NOVARIABLE)
//						defaulttextsize(4, var->textdescript);
//				}
//			}
//		}
//		ai->temp1 = 1;
//		for (i = 0; i < 2; i++)
//		{
//			ni = ai->end[i].nodeinst;
//			if (((ni->proto->userbits & NFUNCTION) >> NFUNCTIONSH) == NPPIN)
//			{
//				// scan through this nodes portarcinst's
//				for (pai = ni->firstportarcinst; pai != NOPORTARCINST; pai = pai->nextportarcinst)
//				{
//					if (pai->conarcinst->temp1 == 0)
//						io_edcheck_busnames(pai->conarcinst, base);
//				}
//			}
//		}
//	}
//	
//	/*
//	 * Routine to return the text height to use when it says "textheight" units in the EDIF file.
//	 */
//	INTBIG io_ediftextsize(INTBIG textheight)
//	{
//		REGISTER INTBIG i, lambda;
//	
//		lambda = el_curlib->lambda[technology->techindex];
//		i = textheight / lambda;
//		if (i < 0) i = 4;
//		if (i > TXTMAXQLAMBDA) i = TXTMAXQLAMBDA;
//		i = TXTSETQLAMBDA(i);
//		return(i);
//	}
//	
//	/* helper routine for "esort" */
//	int io_edcompare_name(const void *name1, const void *name2)
//	{
//		REGISTER NAMETABLE_PTR *n1, *n2;
//	
//		n1 = (NAMETABLE_PTR *)name1;
//		n2 = (NAMETABLE_PTR *)name2;
//		return(namesame((*n1)->original, (*n2)->original));
//	}
//	
//	/*
//	 * routine to read a line from file "file" and place it in "line" (which has
//	 * up to "limit" characters).  Returns FALSE on end-of-file.
//	 */
//	BOOLEAN io_edget_line(CHAR *line, INTBIG limit, FILE *file)
//	{
//		REGISTER CHAR *pp;
//		REGISTER INTSML c;
//		REGISTER INTBIG total;
//	
//		pp = line;
//		total = 1;
//		for(;;)
//		{
//			c = xgetc(file);
//			if (c == EOF)
//			{
//				if (pp == line) return(FALSE);
//				break;
//			}
//			filepos++;
//			*pp = (CHAR)c;
//			if (*pp == '\n')
//			{
//				pp++;
//				break;
//			}
//			pp++;
//			if ((++total) >= limit) break;
//		}
//		*pp = 0;
//		return(TRUE);
//	}
//	
//	/************ INPUT SUPPORT *********/
//	
//	INTBIG io_edallocproperty(CHAR *aName, PROPERTY_TYPE type, int integer,
//		float number, CHAR *str)
//	{
//		REGISTER void *infstr;
//	
//		EDPROPERTY property = new EDPROPERTY();
//	
//		property.next = properties;
//		properties = property;
//		infstr = initinfstr();
//		formatinfstr(infstr, x_("ATTR_%s"), aName);
//		if (allocstring(&property->name, returninfstr(infstr), io_tool->cluster))
//			RET_NOMEMORY();
//		property->type = type;
//		switch (property->type)
//		{
//			case PINTEGER:
//				property->val.integer = integer;
//				break;
//			case PNUMBER:
//				property->val.number = number;
//				break;
//			case PSTRING:
//				if (allocstring(&property->val.string, str, io_tool->cluster))
//					RET_NOMEMORY();
//				break;
//			default:
//				break;
//		}
//		return(0);
//	}
//	
//	/*
//	 * Module: io_edeq_of_a_line
//	 * Function: Calculates the equation of a line (Ax + By + C = 0)
//	 * Inputs:  sx, sy  - Start point
//	 *          ex, ey  - End point
//	 *          px, py  - Point line should pass through
//	 * Outputs: A, B, C - constants for the line
//	 */
//	void io_edeq_of_a_line(double sx, double sy, double ex, double ey, double *A, double *B, double *C)
//	{
//		if (sx == ex)
//		{
//			*A = 1.0;
//			*B = 0.0;
//			*C = -ex;
//		} else if (sy == ey)
//		{
//			*A = 0.0;
//			*B = 1.0;
//			*C = -ey;
//		} else
//		{
//			// let B = 1 then
//			*B = 1.0;
//			if (sx != 0.0)
//			{
//				// Ax1 + y1 + C = 0    =>    A = -(C + y1) / x1
//				// C = -Ax2 - y2       =>    C = (Cx2 + y1x2) / x1 - y2   =>   Cx1 - Cx2 = y1x2 - y2x1 
//				// C = (y2x1 - y1x2) / (x2 - x1)
//				*C = (ey * sx - sy * ex) / (ex - sx);
//				*A = -(*C + sy) / sx;
//			} else
//			{
//				*C = (sy * ex - ey * sx) / (sx - ex);
//				*A = -(*C + ey) / ex;
//			}
//		}
//	}
//	
//	/*
//	 * Module: io_eddetermine_intersection
//	 * Function: Will determine the intersection of two lines
//	 * Inputs: Ax + By + C = 0 form
//	 * Outputs: x, y - the point of intersection
//	 * returns 1 if found, 0 if non-intersecting
//	 */
//	INTBIG io_eddetermine_intersection(double A[2], double B[2], double C[2], double *x, double *y)
//	{
//		double A1B2, A2B1;
//		double A2C1, A1C2;
//		double X, Y;
//	
//		// check for parallel lines
//		if ((A1B2 = A[0] * B[1]) == (A2B1 = A[1] * B[0]))
//		{
//			// check for coincident lines
//			if (C[0] == C[1]) return(-1);
//			return(0);
//		}
//		A1C2 = A[0] * C[1];
//		A2C1 = A[1] * C[0];
//	
//		if (A[0])
//		{
//			Y = (A2C1 - A1C2) / (A1B2 - A2B1);
//			X = -(B[0] * Y + C[0]) / A[0];
//		} else
//		{
//			Y = (A1C2 - A2C1) / (A2B1 - A1B2);
//			X = -(B[1] * Y + C[1]) / A[1];
//		}
//		*y = Y;
//		*x = X;
//		return(1);
//	}
//	
//	void io_ededif_arc(INTBIG ix[3], INTBIG iy[3], INTBIG *ixc, INTBIG *iyc, INTBIG *r, double *so,
//		double *ar, INTBIG *rot, INTBIG *trans)
//	{
//		double x[3], y[3], px[3], py[3], a[3];
//		double A[2], B[2], C[2];
//		double dx, dy, R, area, xc, yc;
//		INTBIG i;
//	
//		for(i=0; i<3; i++)
//		{
//			x[i] = (double)ix[i];
//			y[i] = (double)iy[i];
//		}
//	
//		// get line equations of perpendicular bi-sectors of p1 to p2
//		px[1] = (x[0] + x[1]) / 2.0;
//		py[1] = (y[0] + y[1]) / 2.0;
//	
//		// now rotate end point 90 degrees
//		px[0] = px[1] - (y[0] - py[1]);
//		py[0] = py[1] + (x[0] - px[1]);
//		io_edeq_of_a_line(px[0], py[0], px[1], py[1], &A[0], &B[0], &C[0]);
//	
//		// get line equations of perpendicular bi-sectors of p2 to p3
//		px[1] = (x[2] + x[1]) / 2.0;
//		py[1] = (y[2] + y[1]) / 2.0;
//	
//		// now rotate end point 90 degrees
//		px[2] = px[1] - (y[2] - py[1]);
//		py[2] = py[1] + (x[2] - px[1]);
//		io_edeq_of_a_line(px[1], py[1], px[2], py[2], &A[1], &B[1], &C[1]);
//	
//		// determine the point of intersection
//		(void)io_eddetermine_intersection(A, B, C, &xc, &yc);
//	
//		*ixc = rounddouble(xc);
//		*iyc = rounddouble(yc);
//	
//		dx = ((double)*ixc) - x[0];
//		dy = ((double)*iyc) - y[0];
//		R = sqrt(dx * dx + dy * dy);
//		*r = rounddouble(R);
//	
//		// now calculate the angle to the start and endpoint
//		dx = x[0] - xc;  dy = y[0] - yc;
//		if (dx == 0.0 && dy == 0.0)
//		{
//			ttyputerr(_("Domain error doing arc computation"));
//			return;
//		}
//		a[0] = atan2(dy, dx) * 1800.0 / EPI;
//		if (a[0] < 0.0) a[0] += 3600.0;
//	
//		dx = x[2] - xc;  dy = y[2] - yc;
//		if (dx == 0.0 && dy == 0.0)
//		{
//			ttyputerr(_("Domain error doing arc computation"));
//			return;
//		}
//		a[2] = atan2(dy, dx) * 1800.0 / EPI;
//		if (a[2] < 0.0) a[2] += 3600.0;
//	
//		// determine the angle of rotation and object orientation
//		// determine the direction
//		// calculate x1*y2 + x2*y3 + x3*y1 - y1*x2 - y2*x3 - y3*x1
//		area = x[0]*y[1] + x[1]*y[2] + x[2]*y[0] - y[0]*x[1] - y[1]*x[2] - y[2]*x[0];
//		if (area > 0.0)
//		{
//			// counter clockwise
//			if (a[2] < a[0]) a[2] += 3600.0;
//			*ar = a[2] - a[0];
//			*rot = rounddouble(a[0]);
//			*so = (a[0] - (double)*rot) * EPI / 1800.0;
//			*trans = 0;
//		} else
//		{
//			// clockwise
//			if (a[0] > 2700)
//			{
//				*rot = 3600 - rounddouble(a[0]) + 2700;
//				*so = ((3600.0 - a[0] + 2700.0) - (double)*rot) * EPI / 1800.0;
//			} else
//			{
//				*rot = 2700 - rounddouble(a[0]);
//				*so = ((2700.0 - a[0]) - (double)*rot) * EPI / 1800.0;
//			}
//			if (a[0] < a[2]) a[0] += 3600;
//			*ar = a[0] - a[2];
//			*trans = 1;
//		}
//	}
//	
//	/*
//	 * routine to generate an icon in library "lib" with name "iconname" from the
//	 * port list in "fpp".  The icon cell is called "pt".  The icon cell is
//	 * returned (NONODEPROTO on error). (Note: copied and adapted from us_makeiconcell)
//	 */
//	NODEPROTO *io_edmakeiconcell(PORTPROTO *fpp, CHAR *iconname, CHAR *pt, LIBRARY *lib)
//	{
//		REGISTER NODEPROTO *np, *bbproto, *pinproto, *buspproto, *pintype;
//		REGISTER NODEINST *bbni, *pinni;
//		REGISTER PORTPROTO *pp, *port, *inputport, *outputport, *bidirport,
//			*topport, *whichport, *bpp;
//		REGISTER ARCPROTO *wireproto, *busproto, *wiretype;
//		REGISTER INTBIG inputside, outputside, bidirside, topside;
//		REGISTER INTBIG eindex, xsize, ysize, xpos, ypos, xbbpos, ybbpos, spacing,
//			lambda;
//		REGISTER UINTBIG character;
//		REGISTER VARIABLE *var;
//	
//		// get the necessary symbols
//		pinproto = sch_wirepinprim;
//		buspproto = sch_buspinprim;
//		bbproto = sch_bboxprim;
//		wireproto = sch_wirearc;
//		busproto = sch_busarc;
//		outputport = bbproto->firstportproto;
//		topport = outputport->nextportproto;
//		inputport = topport->nextportproto;
//		bidirport = inputport->nextportproto;
//	
//		// create the new icon cell
//		lambda = lib->lambda[sch_tech->techindex];
//		np = us_newnodeproto(pt, lib);
//		if (np == NONODEPROTO)
//		{
//			ttyputerr(_("Cannot create icon %s"), pt);
//			return(NONODEPROTO);
//		}
//		np->userbits |= WANTNEXPAND;
//	
//		// determine number of inputs and outputs
//		inputside = outputside = bidirside = topside = 0;
//		for(pp = fpp; pp != NOPORTPROTO; pp = pp->nextportproto)
//		{
//			if ((pp->userbits&BODYONLY) != 0) continue;
//			character = pp->userbits & STATEBITS;
//	
//			// special detection for power and ground ports
//			if (portispower(pp) || portisground(pp)) character = GNDPORT;
//	
//			// make a count of the types of ports and save it on the ports
//			switch (character)
//			{
//				case OUTPORT:
//					pp->temp1 = outputside++;
//					break;
//				case BIDIRPORT:
//					pp->temp1 = bidirside++;
//					break;
//				case PWRPORT:
//				case GNDPORT:
//					pp->temp1 = topside++;
//					break;
//				default:		// INPORT, unlabeled, and all CLOCK ports
//					pp->temp1 = inputside++;
//					break;
//			}
//		}
//	
//		// create the Black Box with the correct size
//		ysize = maxi(maxi(inputside, outputside), 5) * 2 * lambda;
//		xsize = maxi(maxi(topside, bidirside), 3) * 2 * lambda;
//	
//		// create the Black Box instance
//		bbni = newnodeinst(bbproto, 0, xsize, 0, ysize, 0, 0, np);
//		if (bbni == NONODEINST) return(NONODEPROTO);
//	
//		// put the original cell name on the Black Box
//		var = setval((INTBIG)bbni, VNODEINST, x_("SCHEM_function"), (INTBIG)iconname, VSTRING|VDISPLAY);
//		if (var != NOVARIABLE) defaulttextdescript(var->textdescript, bbni->geom);
//	
//		// place pins around the Black Box
//		for(pp = fpp; pp != NOPORTPROTO; pp = pp->nextportproto)
//		{
//			if ((pp->userbits&BODYONLY) != 0) continue;
//			character = pp->userbits & STATEBITS;
//	
//			// special detection for power and ground ports
//			if (portispower(pp) || portisground(pp)) character = GNDPORT;
//	
//			// make a count of the types of ports and save it on the ports
//			switch (character)
//			{
//				case OUTPORT:
//					xpos = xsize + 2 * lambda;
//					xbbpos = xsize;
//					eindex = pp->temp1;
//					spacing = 2 * lambda;
//					if (outputside*2 < inputside) spacing = 4 * lambda;
//					ybbpos = ypos = ysize - ((ysize - (outputside-1)*spacing) / 2 + eindex * spacing);
//					whichport = outputport;
//					break;
//				case BIDIRPORT:
//					eindex = pp->temp1;
//					spacing = 2 * lambda;
//					if (bidirside*2 < topside) spacing = 4 * lambda;
//					xbbpos = xpos = xsize - ((xsize - (bidirside-1)*spacing) / 2 + eindex * spacing);
//					ypos = -2 * lambda;
//					ybbpos = 0;
//					whichport = bidirport;
//					break;
//				case PWRPORT:
//				case GNDPORT:
//					eindex = pp->temp1;
//					spacing = 2 * lambda;
//					if (topside*2 < bidirside) spacing = 4 * lambda;
//					xbbpos = xpos = xsize - ((xsize - (topside-1)*spacing) / 2 + eindex * spacing);
//					ypos = ysize + 2 * lambda;
//					ybbpos = ysize;
//					whichport = topport;
//					break;
//				default:		// INPORT, unlabeled, and all CLOCK ports
//					xpos = -2 * lambda;
//					xbbpos = 0;
//					eindex = pp->temp1;
//					spacing = 2 * lambda;
//					if (inputside*2 < outputside) spacing = 4 * lambda;
//					ybbpos = ypos = ysize - ((ysize - (inputside-1)*spacing) / 2 + eindex * spacing);
//					whichport = inputport;
//					break;
//			}
//	
//			// determine type of pin
//			pintype = pinproto;
//			wiretype = wireproto;
//			if (pp->subnodeinst != NONODEINST)
//			{
//				bpp = pp;
//				while (bpp->subnodeinst->proto->primindex == 0) bpp = bpp->subportproto;
//				if (bpp->subnodeinst->proto == buspproto)
//				{
//					pintype = buspproto;
//					wiretype = busproto;
//				}
//			}
//	
//			// create the pin
//			pinni = io_edifiplacepin(pintype, xpos, xpos, ypos, ypos, 0, 0, np);
//			if (pinni == NONODEINST) return(NONODEPROTO);
//	
//			// export the port that should be on this pin
//			port = newportproto(np, pinni, pintype->firstportproto, pp->protoname);
//			if (port != NOPORTPROTO)
//				port->userbits = pp->userbits | PORTDRAWN;
//	
//			// wire this pin to the black box
//			(void)newarcinst(wiretype, defaultarcwidth(wiretype),
//				us_makearcuserbits(wiretype), pinni, pintype->firstportproto,
//				xpos, ypos, bbni, whichport, xbbpos, ybbpos, np);
//		}
//		return(np);
//	}
//	
//	void io_ednamearc(ARCINST *ai)
//	{
//		int Ix, Iy;
//		REGISTER VARIABLE *var;
//		CHAR basename[WORD+1] ;
//	
//		if (isarray)
//		{
//			// name the bus
//			if (net_reference.length() > 0)
//			{
//				(void)setvalkey((INTBIG)ai, VARCINST, EDIF_name_key,
//					(INTBIG)net_reference, VSTRING);
//			}
//	
//			// a typical foreign array bus will have some range value appended
//			// to the name. This will cause some odd names with duplicate values
//			aName = basename;
//			*aName = 0;
//			for (Ix = 0; Ix < arrayx; Ix++)
//			{
//				for (Iy = 0; Iy < arrayy; Iy++)
//				{
//					if (aName != basename) *aName++ = ',';
//					if (arrayx > 1)
//					{
//						if (arrayy > 1)
//							aName = net_name + "[" + Ix + "," + Iy + "]";
//						else
//							aName = net_name + "[" + Ix + "]";
//					}
//					else aName = net_name + "[" + Iy + "]";
//					aName += estrlen(aName);
//				}
//			}
//			var = setvalkey((INTBIG)ai, VARCINST, el_arc_name_key, (INTBIG)basename, VSTRING);
//			if (var != NOVARIABLE)
//				defaulttextsize(4, var->textdescript);
//		} else
//		{
//			if (net_reference.length() > 0)
//			{
//				(void)setvalkey((INTBIG)ai, VARCINST, EDIF_name_key,
//					(INTBIG)net_reference, VSTRING);
//			}
//			if (net_name.length() > 0)
//			{
//				// set name of arc but don't display name
//				var = setvalkey((INTBIG)ai, VARCINST, el_arc_name_key,
//					(INTBIG)net_name, VSTRING);
//				if (var != NOVARIABLE)
//					defaulttextsize(4, var->textdescript);
//			}
//		}
//	}
//	
//	INTBIG io_edfindport(NODEPROTO *cell, INTBIG x, INTBIG y, ARCPROTO *ap, NODEINST *nis[],
//		PORTPROTO *pps[])
//	{
//		INTBIG cnt;
//		ARCINST *ai = NOARCINST, *ar1,*ar2;
//		ARCPROTO *nap;
//		NODEINST *ni, *fno, *tno;
//		PORTPROTO *fpt, *tpt, *pp;
//		NODEPROTO *np, *pnt;
//		INTBIG wid, bits1, bits2, lx, hx, ly, hy, j;
//		INTBIG fendx, fendy, tendx, tendy;
//	
//		cnt = io_edfindport_geom(cell, x, y, ap, nis, pps, &ai);
//	
//		if (cnt == 0 && ai != NOARCINST)
//		{
//			// direct hit on an arc, verify connection
//			nap = ai->proto;
//			np = getpinproto(nap);
//			if (np == NONODEPROTO) return(0);
//			pp = np->firstportproto;
//			for (j = 0; pp->connects[j] != NOARCPROTO; j++)
//				if (pp->connects[j] == ap) break;
//			if (pp->connects[j] == NOARCPROTO) return(0);
//	
//			// try to split arc (from us_getnodeonarcinst)*/
//			// break is at (prefx, prefy): save information about the arcinst
//			fno = ai->end[0].nodeinst;	fpt = ai->end[0].portarcinst->proto;
//			tno = ai->end[1].nodeinst;	tpt = ai->end[1].portarcinst->proto;
//			fendx = ai->end[0].xpos;	  fendy = ai->end[0].ypos;
//			tendx = ai->end[1].xpos;	  tendy = ai->end[1].ypos;
//			wid = ai->width;  pnt = ai->parent;
//			bits1 = bits2 = ai->userbits;
//			if ((bits1&ISNEGATED) != 0)
//			{
//				if ((bits1&REVERSEEND) == 0) bits2 &= ~ISNEGATED; else
//					bits1 &= ~ISNEGATED;
//			}
//			if (figureangle(fendx,fendy, x,y) != figureangle(x,y, tendx, tendy))
//			{
//				bits1 &= ~FIXANG;
//				bits2 &= ~FIXANG;
//			}
//	
//			// create the splitting pin
//			lx = x - (np->highx-np->lowx)/2;   hx = lx + np->highx-np->lowx;
//			ly = y - (np->highy-np->lowy)/2;   hy = ly + np->highy-np->lowy;
//			ni = io_edifiplacepin(np, lx,hx, ly,hy, 0, 0, pnt);
//			if (ni == NONODEINST)
//			{
//				ttyputerr(_("Cannot create splitting pin"));
//				return(0);
//			}
//			endobjectchange((INTBIG)ni, VNODEINST);
//	
//			// set the node, and port
//			nis[cnt] = ni;
//			pps[cnt++] = pp;
//	
//			// create the two new arcinsts
//			ar1 = newarcinst(nap, wid, bits1, fno, fpt, fendx, fendy, ni, pp, x, y, pnt);
//			ar2 = newarcinst(nap, wid, bits2, ni, pp, x, y, tno, tpt, tendx, tendy, pnt);
//			if (ar1 == NOARCINST || ar2 == NOARCINST)
//			{
//				ttyputerr(_("Error creating the split arc parts"));
//				return(cnt);
//			}
//			(void)copyvars((INTBIG)ai, VARCINST, (INTBIG)ar1, VARCINST, FALSE);
//			endobjectchange((INTBIG)ar1, VARCINST);
//			endobjectchange((INTBIG)ar2, VARCINST);
//	
//			// delete the old arcinst
//			startobjectchange((INTBIG)ai, VARCINST);
//			if (killarcinst(ai))
//				ttyputerr(_("Error deleting original arc"));
//		}
//	
//		return(cnt);
//	}
//	
//	/* module: ro_mazefindport
//	   function: will locate the nodeinstance and portproto corresponding to
//	   to a direct intersection with the given point.
//	   inputs:
//	   cell - cell to search
//	   x, y  - the point to exam
//	   ap    - the arc used to connect port (must match pp)
//	   nis   - pointer to ni pointer buffer.
//	   pps   - pointer to portproto pointer buffer.
//	   outputs:
//	   returns cnt if found, 0 not found, -1 on error
//	   ni = found ni instance
//	   pp = found pp proto.
//	 */
//	INTBIG io_edfindport_geom(NODEPROTO *cell, INTBIG x, INTBIG y, ARCPROTO *ap,
//		NODEINST *nis[], PORTPROTO *pps[], ARCINST **ai)
//	{
//		REGISTER INTBIG j, cnt, sea, dist, bestdist, bestpx, bestpy, bits, wid,
//			lx, hx, ly, hy, xs, ys;
//		INTBIG px, py;
//		REGISTER GEOM *geom;
//		REGISTER ARCINST *newai;
//		static POLYGON *poly = NOPOLYGON;
//		REGISTER PORTPROTO *pp, *bestpp;
//		REGISTER NODEINST *ni, *bestni;
//		NODEPROTO *np;
//	
//		(void)needstaticpolygon(&poly, 4, io_tool->cluster);
//	
//		cnt = 0;
//		bestni = NONODEINST;
//		sea = initsearch(x, x, y, y, cell);
//		for(;;)
//		{
//			geom = nextobject(sea);
//			if (geom == NOGEOM) break;
//	
//			switch (geom->entryisnode)
//			{
//				case TRUE:
//					// now locate a portproto
//					ni = geom->entryaddr.ni;
//					for (pp = ni->proto->firstportproto; pp != NOPORTPROTO; pp = pp->nextportproto)
//					{
//						shapeportpoly(ni, pp, poly, FALSE);
//						if (isinside(x, y, poly))
//						{
//							// check if port connects to arc ...*/
//							for (j = 0; pp->connects[j] != NOARCPROTO; j++)
//								if (pp->connects[j] == ap)
//							{
//								nis[cnt] = ni;
//								pps[cnt] = pp;
//								cnt++;
//							}
//						} else
//						{
//							portposition(ni, pp, &px, &py);
//							dist = computedistance(x, y, px, py);
//							if (bestni == NONODEINST || dist < bestdist)
//							{
//								bestni = ni;
//								bestpp = pp;
//								bestdist = dist;
//								bestpx = px;
//								bestpy = py;
//							}
//						}
//					}
//					break;
//	
//				case FALSE:
//					// only accept valid wires
//					if (geom->entryaddr.ai->proto->tech == sch_tech)
//						*ai = geom->entryaddr.ai;
//					break;
//			}
//		}
//	
//		// special case: make it connect to anything that is near
//		if (cnt == 0 && bestni != NONODEINST)
//		{
//			// create a new node in the proper position
//			np = getpinproto(ap);
//			xs = np->highx - np->lowx;   lx = x - xs/2;   hx = lx + xs;
//			ys = np->highy - np->lowy;   ly = y - ys/2;   hy = ly + ys;
//			ni = io_edifiplacepin(np, lx, hx, ly, hy, 0, 0, cell);
//			wid = ap->nominalwidth;
//			bits = us_makearcuserbits(ap);
//			newai = newarcinst(ap, wid, bits, ni, ni->proto->firstportproto, x, y, bestni, bestpp, bestpx, bestpy, cell);
//			nis[cnt] = ni;
//			pps[cnt] = ni->proto->firstportproto;
//			cnt++;
//		}
//		return(cnt);
//	}
