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
import com.sun.electric.database.geometry.Geometric;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortCharacteristic;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.Connection;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.technology.technologies.Schematics;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.user.ViewChanges;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

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

	private static final int SHEETWIDTH = 32;
	private static final int SHEETHEIGHT = 20;

	// name table for layers
	private static class NAMETABLE
	{
		/** the original MASK layer */	private String original;
		/** the replacement layer */	private String replace;
		/** the basic electric node */	private NodeProto node;
		/** the basic arc type */		private ArcProto arc;
		/** default text height */		private int textheight;
		/** default justification */	private TextDescriptor.Position justification;
		/** layer is visible */			private boolean visible;
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

	private static class EDPORT
	{
		String name;
		String reference;
		PortCharacteristic direction;
		int arrayx, arrayy;
		EDPORT next;
	};

	private static class EDNETPORT
	{
		/** unique node port is attached to */	private NodeInst ni;					
		/** port type */						private PortProto pp;					
		/** member for an array */				private int member;						
		/** next common to a net */				private EDNETPORT next;		
	}

	private static class EDVENDOR {}
	private static final EDVENDOR EVUNKNOWN    = new EDVENDOR();
	private static final EDVENDOR EVCADENCE    = new EDVENDOR();
	private static final EDVENDOR EVVIEWLOGIC  = new EDVENDOR();

	private static class EDPROPERTY
	{
		String name;
		Object val;
		EDPROPERTY next;
	}

	// view information ...
	/** the schematic page number */			private int pageno;
	/** indicates we are in a NETLIST view */	private VTYPES active_view;
	/** the current vendor type */				private EDVENDOR vendor;

	// parser variables ...
	/** the current parser state */				private EDIFKEY state;
	/** the read buffer */						private String buffer;
	/** the position within the buffer */		private int pos;
	/** no update flag */						private boolean ignoreblock;
	/** load status */							private int errors, warnings;

	// electric context data ...
	/** the new library */						private Library library;
	/** the active technology */				private Technology technology;
	/** the current active cell */				private Cell current_cell;
	/** the current active instance */			private NodeInst current_node;
	/** the current active arc */				private ArcInst current_arc;
	/** the current figure group node */		private NodeProto figure_group;
	/** the current (if exists) arc type */		private ArcProto arc_type;
	/** the cellRef type */						private NodeProto cellRefProto;
	/** the current port proto */				private PortProto current_port;

	// general geometry information ...
	/** the current geometry type */			private GTYPES geometry;
	/** the list of points */					private List points;
	/** the orientation of the structure */		private OTYPES orientation;
	/** port direction */						private PortCharacteristic direction;

	// geometric path constructors ...
	/** the width of the path */				private int path_width;
	/** extend path end flag */					private boolean extend_end;

	// array variables ...
	/** set if truely an array */				private boolean isarray;
	/** the bounds of the array */				private int arrayx, arrayy;
	/** offsets in x and y for X increment */	private double deltaxX, deltaxY;
	/** offsets in x and y for Y increment */	private double deltayX, deltayY;
	/** which offset flag */					private int deltapts;
	/** the element of the array */				private int memberx, membery;

	// text variables ...
	/** Text string */							private String string;
	/** the current default text height */		private int textheight;
	/** the current text justificaton */		private TextDescriptor.Position justification;
	/** is stringdisplay visible */				private boolean visible;
	/** origin x and y */						private List save_points;
	/** the height of text */					private int save_textheight;
	/** justification of the text */			private TextDescriptor.Position save_justification;

	// technology data ...
	/** scaling value */						private double scale;

	// current name and rename of EDIF objects ...
	/** the current cell name */				private String cell_reference;
	/** the current cell name (original) */		private String cell_name;
	/** the current port name */				private String port_reference;
	/** the current port name (original) */		private String port_name;
	/** the current instance name */			private String instance_reference;
	/** the current instance name (original) */	private String instance_name;
	/** the current bundle name */				private String bundle_reference;
	/** the current bundle name (original) */	private String bundle_name;
	/** the current net name */					private String net_reference;
	/** the current net name (original) */		private String net_name;
	/** the current property name */			private String property_reference;
	/** the current property name (original) */	private String property_name;

	/** property value */						private Object pval;

	/** the name of the object */				private String name;
	/** the original name of the object */		private String original;

	private HashSet builtCells;

	// layer or figure information ...
	/** pointer to layer table entry */			private int layer_ptr;
	/** the current name mapping table */		private NAMETABLE [] nametbl = new NAMETABLE[MAXLAYERS];
	/** the current name table entry */			private NAMETABLE cur_nametbl;

	/** cell name lookup (from rename) */ 		private HashMap celltbl;

	// port data for port exporting
	/** active port list */						private EDPORT ports;

	// property data for all objects
	/** active property list */					private EDPROPERTY properties;

	// net constructors
	/** the list of ports on a net */			private EDNETPORT firstnetport;
	/** the last in the list on a net */		private EDNETPORT lastnetport;

	// view NETLIST layout
	/** current sheet position */				private int sh_xpos, sh_ypos;
	/** next x offset */						private int sh_offset;
	/** current position pointers */			private int ipos, bpos, opos;

	private static HashMap edifKeys = new HashMap();

	/* EDIF keyword stack, this stack matches the current depth of the EDIF file,
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

	/**
	 * Method to import a library from disk.
	 * @param lib the library to fill
	 * @return true on error.
	 */
	protected boolean importALibrary(Library lib)
	{
		KARRAY.stateArray = new EDIFKEY [] {KINSTANCE, KPORT, KNET};
		KAUTHOR.stateArray = new EDIFKEY [] {KWRITTEN};
		KBOUNDINGBOX.stateArray = new EDIFKEY [] {KSYMBOL, KCONTENTS};
		KCELL.stateArray = new EDIFKEY [] {KEXTERNAL, KLIBRARY};
		KCELLREF.stateArray = new EDIFKEY [] {KDESIGN, KVIEWREF, KINSTANCE};
		KCELLTYPE.stateArray = new EDIFKEY [] {KCELL};
		KCONTENTS.stateArray = new EDIFKEY [] {KVIEW};
		KDESIGN.stateArray = new EDIFKEY [] {KEDIF};
		KDIRECTION.stateArray = new EDIFKEY [] {KPORT};
		KEDIF.stateArray = new EDIFKEY [] {KINIT};
		KEDIFLEVEL.stateArray = new EDIFKEY [] {KEDIF, KEXTERNAL, KLIBRARY};
		KEXTERNAL.stateArray = new EDIFKEY [] {KEDIF};
		KINSTANCE.stateArray = new EDIFKEY [] {KCONTENTS, KPAGE, KPORTIMPLEMENTATION, KCOMMENTGRAPHICS};
		KINSTANCEREF.stateArray = new EDIFKEY [] {KINSTANCEREF, KPORTREF};
		KINTERFACE.stateArray = new EDIFKEY [] {KVIEW};
		KJOINED.stateArray = new EDIFKEY [] {KINTERFACE, KNET};
		KEDIFKEYMAP.stateArray = new EDIFKEY [] {KEDIF};
		KLIBRARYREF.stateArray = new EDIFKEY [] {KCELLREF};
		KLISTOFNETS.stateArray = new EDIFKEY [] {KNETBUNDLE};
		KNET.stateArray = new EDIFKEY [] {KCONTENTS, KPAGE, KLISTOFNETS};
		KNETBUNDLE.stateArray = new EDIFKEY [] {KCONTENTS, KPAGE};
		KNUMBERDEFINITION.stateArray = new EDIFKEY [] {KTECHNOLOGY};
		KPORT.stateArray = new EDIFKEY [] {KINTERFACE, KLISTOFPORTS};
		KSCALE.stateArray = new EDIFKEY [] {KNUMBERDEFINITION};
		KSTATUS.stateArray = new EDIFKEY [] {KCELL, KDESIGN, KEDIF, KEXTERNAL, KLIBRARY, KVIEW};
		KSYMBOL.stateArray = new EDIFKEY [] {KINTERFACE};
		KTECHNOLOGY.stateArray = new EDIFKEY [] {KEXTERNAL, KLIBRARY};
		KTIMESTAMP.stateArray = new EDIFKEY [] {KWRITTEN};
		KUNIT.stateArray = new EDIFKEY [] {KPROPERTY, KSCALE};
		KVIEW.stateArray = new EDIFKEY [] {KPROPERTY, KCELL};
		KVIEWREF.stateArray = new EDIFKEY [] {KINSTANCE, KINSTANCEREF, KPORTREF};
		KVIEWTYPE.stateArray = new EDIFKEY [] {KVIEW};
		KWRITTEN.stateArray = new EDIFKEY [] {KSTATUS};

		// inits
		firstnetport = lastnetport = null;
		pval = null;
		points = new ArrayList();
		save_points = new ArrayList();

		// parser inits
		state = KINIT;
		buffer = "";
		pos = 0;
		errors = warnings = 0;
		ignoreblock = false;
		vendor = EVUNKNOWN;
		builtCells = new HashSet();

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
		justification = TextDescriptor.Position.DOWNRIGHT;
		visible = true;
		io_edfreesavedptlist();

		sh_xpos = -1;
		sh_ypos = -1;

		kstack_ptr = 0;

		default_port = Schematics.tech.wirePinNode.findPortProto("wire");
		default_iconport = Schematics.tech.wirePinNode.getPort(0);
		default_busport = Schematics.tech.busPinNode.findPortProto("bus");
		default_input = Schematics.tech.offpageNode.findPortProto("y");
		default_output = Schematics.tech.offpageNode.findPortProto("a");

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

		if (library.getCurCell() == null && library.getCells().hasNext())
			library.setCurCell((Cell)library.getCells().next());

		return false;
	}

	/**
	 * Method to load the edif netlist into memory
	 * Does a simple keyword lookup to load the lisp structured
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
			EDIFKEY key = (EDIFKEY)edifKeys.get(token.toLowerCase());
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
						System.out.println("error, line #" + lineReader.getLineNumber() + ": illegal state (" + state.name + ") for keyword <" + token + ">");
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

	/**
	 * get a keyword routine
	 */
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
				io_edpop_stack();
			} else
			{
				if (TextUtils.isANumber(buffer)) io_edprocess_integer(TextUtils.atoi(buffer));
			}
		}
		return io_edget_token((char)0);
	}

	/**
	 * Method to do cleanup processing of an integer argument such as in (array (...) 1 2)
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

	/**
	 * Method to allocate net ports
	 */
	private void io_edallocnetport()
	{
		EDNETPORT netport = new EDNETPORT();

		if (firstnetport == null) firstnetport = netport; else
			lastnetport.next = netport;

		lastnetport = netport;
		netport.next = null;
		netport.ni = null;
		netport.pp = null;
		netport.member = 0;
	}

	/**
	 * Method to free the netport list
	 */
	private void io_edfreenetports()
	{
		firstnetport = lastnetport = null;
	}

	/**
	 * Method to position to the next token
	 * @return true on EOF.
	 */
	private boolean io_edpos_token()
		throws IOException
	{
		for(;;)
		{
			if (buffer == null) return true;
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

	/**
	 * Method to get a delimeter routine
	 */
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

	/**
	 * Method to get a token
	 */
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
		justification = nametbl[pos].justification = TextDescriptor.Position.DOWNRIGHT;
		visible = nametbl[pos].visible = true;

		// allow new definitions
		cur_nametbl = nametbl[pos];

		// now sort the list
		layer_ptr++;
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

	/**
	 * Method to allocate ports
	 */
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

	private void io_edallocproperty(String aName,  Object obj)
	{
		EDPROPERTY property = new EDPROPERTY();
		property.next = properties;
		properties = property;
		property.name = "ATTR_" + aName;
		property.val = obj;
	}

	private Point2D getSizeAndMirror(NodeProto np)
	{
		double sX = np.getDefWidth();
		double sY = np.getDefHeight();
		if (np instanceof Cell)
		{
			Rectangle2D bounds = ((Cell)np).getBounds();
			sX = bounds.getWidth();
			sY = bounds.getHeight();
		}
		if (getMirrorX(orientation)) sX = -sX;
		if (getMirrorY(orientation)) sY = -sY;
		return new Point2D.Double(sX, sY);
	}

	/**
	 * Method to return the text height to use when it says "textheight" units in the EDIF file.
	 */
	private double io_ediftextsize(double textheight)
	{
		double i = textheight;
		if (i < 0) i = 4;
		if (i > TextDescriptor.Size.TXTMAXQGRID) i = TextDescriptor.Size.TXTMAXQGRID;
		return i;
	}

	private void io_ededif_arc(double [] x, double [] y)
	{
		GenMath.MutableDouble [] A = new GenMath.MutableDouble[2];
		GenMath.MutableDouble [] B = new GenMath.MutableDouble[2];
		GenMath.MutableDouble [] C = new GenMath.MutableDouble[2];

		// get line equations of perpendicular bi-sectors of p1 to p2
		double px1 = (x[0] + x[1]) / 2.0;
		double py1 = (y[0] + y[1]) / 2.0;

		// now rotate end point 90 degrees
		double px0 = px1 - (y[0] - py1);
		double py0 = py1 + (x[0] - px1);
		io_edeq_of_a_line(px0, py0, px1, py1, A[0], B[0], C[0]);

		// get line equations of perpendicular bi-sectors of p2 to p3
		px1 = (x[2] + x[1]) / 2.0;
		py1 = (y[2] + y[1]) / 2.0;

		// now rotate end point 90 degrees
		double px2 = px1 - (y[2] - py1);
		double py2 = py1 + (x[2] - px1);
		io_edeq_of_a_line(px1, py1, px2, py2, A[1], B[1], C[1]);

		// determine the point of intersection
		Point2D ctr = io_eddetermine_intersection(A, B, C);

		double ixc = ctr.getX();
		double iyc = ctr.getY();

		double dx = ixc - x[0];
		double dy = iyc - y[0];
		double r = Math.sqrt(dx * dx + dy * dy);

		// now calculate the angle to the start and endpoint
		dx = x[0] - ixc;  dy = y[0] - iyc;
		if (dx == 0.0 && dy == 0.0)
		{
			System.out.println("Domain error doing arc computation");
			return;
		}
		double a0 = Math.atan2(dy, dx) * 1800.0 / Math.PI;
		if (a0 < 0.0) a0 += 3600.0;

		dx = x[2] - ixc;  dy = y[2] - iyc;
		if (dx == 0.0 && dy == 0.0)
		{
			System.out.println("Domain error doing arc computation");
			return;
		}
		double a2 = Math.atan2(dy, dx) * 1800.0 / Math.PI;
		if (a2 < 0.0) a2 += 3600.0;

		// determine the angle of rotation, object orientation, and direction
		// calculate x1*y2 + x2*y3 + x3*y1 - y1*x2 - y2*x3 - y3*x1
		double area = x[0]*y[1] + x[1]*y[2] + x[2]*y[0] - y[0]*x[1] - y[1]*x[2] - y[2]*x[0];
		double ar = 0, so = 0;
		int rot = 0;
		boolean trans = false;
		if (area > 0.0)
		{
			// counter clockwise
			if (a2 < a0) a2 += 3600.0;
			ar = a2 - a0;
			rot = (int)a0;
			so = (a0 - rot) * Math.PI / 1800.0;
		} else
		{
			// clockwise
			if (a0 > 2700)
			{
				rot = 3600 - (int)a0 + 2700;
				so = ((3600.0 - a0 + 2700.0) - rot) * Math.PI / 1800.0;
			} else
			{
				rot = 2700 - (int)a0;
				so = ((2700.0 - a0) - rot) * Math.PI / 1800.0;
			}
			if (a0 < a2) a0 += 3600;
			ar = a0 - a2;
			trans = true;
		}

		// get the bounds of the circle
		double sX = r*2;
		double sY = r*2;
		if (trans)
		{
			rot = (rot + 900) % 3600;
			sY = -sY;
		}
		NodeInst ni = NodeInst.makeInstance(figure_group != null && figure_group != Artwork.tech.boxNode ?
			figure_group : Artwork.tech.circleNode, new Point2D.Double(ixc, iyc), sX, sY, current_cell, rot, null, 0);
		if (ni == null)
		{
			System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create arc");
			errors++;
		} else
		{
			// store the angle of the arc
			ni.setArcDegrees(so, ar*Math.PI/1800.0);
		}
	}

	/**
	 * Method to determine the intersection of two lines
	 * Inputs: Ax + By + C = 0 form
	 * Outputs: x, y - the point of intersection
	 * returns 1 if found, 0 if non-intersecting
	 */
	private Point2D io_eddetermine_intersection(GenMath.MutableDouble [] A, GenMath.MutableDouble [] B, GenMath.MutableDouble [] C)
	{
		// check for parallel lines
		double A1B2 = A[0].doubleValue() * B[1].doubleValue();
		double A2B1 = A[1].doubleValue() * B[0].doubleValue();
		if (A1B2 == A2B1)
		{
			// check for coincident lines
			if (C[0].doubleValue() == C[1].doubleValue()) return null;
			return null;
		}
		double A1C2 = A[0].doubleValue() * C[1].doubleValue();
		double A2C1 = A[1].doubleValue() * C[0].doubleValue();

		if (A[0].doubleValue() != 0)
		{
			double Y = (A2C1 - A1C2) / (A1B2 - A2B1);
			double X = -(B[0].doubleValue() * Y + C[0].doubleValue()) / A[0].doubleValue();
			return new Point2D.Double(X, Y);
		} else
		{
			double Y = (A1C2 - A2C1) / (A2B1 - A1B2);
			double X = -(B[1].doubleValue() * Y + C[1].doubleValue()) / A[1].doubleValue();
			return new Point2D.Double(X, Y);
		}
	}

	/**
	 * Method to calculate the equation of a line (Ax + By + C = 0)
	 * Inputs:  sx, sy  - Start point
	 *          ex, ey  - End point
	 *          px, py  - Point line should pass through
	 * Outputs: A, B, C - constants for the line
	 */
	private void io_edeq_of_a_line(double sx, double sy, double ex, double ey, GenMath.MutableDouble A, GenMath.MutableDouble B, GenMath.MutableDouble C)
	{
		if (sx == ex)
		{
			A.setValue(1.0);
			B.setValue(0.0);
			C.setValue(-ex);
		} else if (sy == ey)
		{
			A.setValue(0.0);
			B.setValue(1.0);
			C.setValue(-ey);
		} else
		{
			// let B = 1 then
			B.setValue(1.0);
			if (sx != 0.0)
			{
				// Ax1 + y1 + C = 0    =>    A = -(C + y1) / x1
				// C = -Ax2 - y2       =>    C = (Cx2 + y1x2) / x1 - y2   =>   Cx1 - Cx2 = y1x2 - y2x1
				// C = (y2x1 - y1x2) / (x2 - x1)
				C.setValue((ey * sx - sy * ex) / (ex - sx));
				A.setValue(-(C.doubleValue() + sy) / sx);
			} else
			{
				C.setValue((sy * ex - ey * sx) / (sx - ex));
				A.setValue(-(C.doubleValue() + ey) / ex);
			}
		}
	}

	private void io_ednamearc(ArcInst ai)
	{
		if (isarray)
		{
			// name the bus
			if (net_reference.length() > 0)
			{
				ai.newVar("EDIF_name", net_reference);
			}

			// a typical foreign array bus will have some range value appended
			// to the name. This will cause some odd names with duplicate values
			StringBuffer aName = new StringBuffer();
			for (int Ix = 0; Ix < arrayx; Ix++)
			{
				for (int Iy = 0; Iy < arrayy; Iy++)
				{
					if (aName.length() > 0) aName.append(',');
					if (arrayx > 1)
					{
						if (arrayy > 1)
							aName.append(net_name + "[" + Ix + "," + Iy + "]");
						else
							aName.append(net_name + "[" + Ix + "]");
					}
					else aName.append(net_name + "[" + Iy + "]");
				}
			}
			ai.setName(aName.toString());
		} else
		{
			if (net_reference.length() > 0)
			{
				ai.newVar("EDIF_name", net_name);
			}
			if (net_name.length() > 0)
			{
				// set name of arc but don't display name
				ai.setName(net_name);
			}
		}
	}

	/**
	 * Method to locate the nodeinstance and portproto corresponding to
	 * to a direct intersection with the given point.
	 * inputs:
	 * cell - cell to search
	 * x, y  - the point to exam
	 * ap    - the arc used to connect port (must match pp)
	 */
	private List io_edfindport(Cell cell, double x, double y, ArcProto ap)
	{
		List ports = new ArrayList();
		PortInst bestPi = null;
		Point2D pt = new Point2D.Double(x, y);
		double bestDist = Double.MAX_VALUE;
		ArcInst ai = null;
		for(Geometric.Search sea = new Geometric.Search(new Rectangle2D.Double(x, y, 0, 0), cell); sea.hasNext(); )
		{
			Geometric geom = (Geometric)sea.next();

			if (geom instanceof NodeInst)
			{
				// now locate a portproto
				NodeInst ni = (NodeInst)geom;
				for(Iterator it = ni.getPortInsts(); it.hasNext(); )
				{
					PortInst pi = (PortInst)it.next();
					Poly poly = pi.getPoly();
					if (poly.isInside(pt))
					{
						// check if port connects to arc ...*/
						if (pi.getPortProto().getBasePort().connectsTo(ap))
							ports.add(pi);
					} else
					{
						double dist = pt.distance(new Point2D.Double(poly.getCenterX(), poly.getCenterY()));
						if (bestPi == null || dist < bestDist) bestPi = pi;
					}
				}
			} else
			{
				// only accept valid wires
				ArcInst oAi = (ArcInst)geom;
				if (oAi.getProto().getTechnology() == Schematics.tech)
					ai = oAi;
			}
		}

		// special case: make it connect to anything that is near
		if (ports.size() == 0 && bestPi != null)
		{
			// create a new node in the proper position
			PrimitiveNode np = ((PrimitiveArc)ap).findPinProto();
			NodeInst ni = io_edifiplacepin(np, x, y, np.getDefWidth(), np.getDefHeight(), 0, cell);
			PortInst head = ni.getOnlyPortInst();
			ArcInst newai = ArcInst.makeInstance(ap, ap.getDefaultWidth(), head, bestPi);
			ports.add(head);
		}

		if (ports.size() == 0 && ai != null)
		{
			// direct hit on an arc, verify connection
			ArcProto nap = ai.getProto();
			PrimitiveNode np = ((PrimitiveArc)nap).findPinProto();
			if (np == null) return ports;
			PortProto pp = np.getPort(0);
			if (!pp.getBasePort().connectsTo(ap)) return ports;

			// try to split arc (from us_getnodeonarcinst)*/
			// break is at (prefx, prefy): save information about the arcinst
			PortInst fPi = ai.getHead().getPortInst();
			NodeInst fno = fPi.getNodeInst();
			PortProto fpt = fPi.getPortProto();
			Point2D fPt = ai.getHead().getLocation();
			PortInst tPi = ai.getTail().getPortInst();
			NodeInst tno = tPi.getNodeInst();
			PortProto tpt = tPi.getPortProto();
			Point2D tPt = ai.getTail().getLocation();

			// create the splitting pin
			NodeInst ni = io_edifiplacepin(np, x, y, np.getDefWidth(), np.getDefHeight(), 0, cell);
			if (ni == null)
			{
				System.out.println("Cannot create splitting pin");
				return ports;
			}

			// set the node, and port
			PortInst pi = ni.findPortInstFromProto(pp);
			ports.add(pi);

			// create the two new arcinsts
			ArcInst ar1 = ArcInst.makeInstance(nap, nap.getDefaultWidth(), fPi, pi, fPt, pt, null);
			ArcInst ar2 = ArcInst.makeInstance(nap, nap.getDefaultWidth(), pi, tPi, pt, tPt, null);
			if (ar1 == null || ar2 == null)
			{
				System.out.println("Error creating the split arc parts");
				return ports;
			}
			ar1.copyPropertiesFrom(ai);
			if (ai.getHead().isNegated()) ar1.getHead().setNegated(true);
			if (ai.getTail().isNegated()) ar2.getTail().setNegated(true);

			// delete the old arcinst
			ai.kill();
		}

		return ports;
	}

	private void io_edcheck_busnames(ArcInst ai, String base, HashSet seenArcs)
	{
		if (ai.getProto() != Schematics.tech.bus_arc)
		{
			// verify the name
			String aName = ai.getName();
			{
				if (aName.startsWith(base))
				{
					if (aName.length() > base.length() && TextUtils.isDigit(aName.charAt(base.length())))
					{
						String newName = base + "[" + aName.substring(base.length()) + "]";
						ai.setName(newName);
					}
				}
			}
		}
		seenArcs.add(ai);
		for (int i = 0; i < 2; i++)
		{
			Connection con = ai.getConnection(i);
			NodeInst ni = con.getPortInst().getNodeInst();
			if (ni.getFunction() == PrimitiveNode.Function.PIN)
			{
				// scan through this nodes portarcinst's
				for(Iterator it = ni.getConnections(); it.hasNext(); )
				{
					Connection oCon = (Connection)it.next();
					ArcInst pAi = oCon.getArc();
					if (!seenArcs.contains(pAi))
						io_edcheck_busnames(pAi, base, seenArcs);
				}
			}
		}
	}

	/**
	 * pop the keyword state stack, called when ')' is encountered.
	 * Note this method needs to broken into a simple method call structure.
	 */
	private void io_edpop_stack()
		throws IOException
	{
		if (kstack_ptr != 0)
		{
			if (!ignoreblock)
			{
				if (state == KFIGURE)
				{
					cur_nametbl = null;
					visible = true;
					justification = TextDescriptor.Position.DOWNRIGHT;
					textheight = 0;
				} else if (state == KBOUNDINGBOX)
				{
					figure_group = null;
				} else if (state == KTECHNOLOGY)
				{
					// for all layers assign their GDS number
					for (Iterator it = technology.getLayers(); it.hasNext(); )
					{
						Layer layer = (Layer)it.next();
						String gdsLayer = layer.getGDSLayer();
						if (gdsLayer == null || gdsLayer.length() == 0) continue;

						// search for this layer
						boolean found = false;
						for(int i=0; i<MAXLAYERS; i++)
						{
							NAMETABLE nt = nametbl[i];
							if (nt == null) continue;
							if (nt.replace.equalsIgnoreCase(layer.getName())) { found = true;   break; }
						}
						if (!found)
						{
							// add to the list
							int pos = layer_ptr;
							nametbl[pos] = new NAMETABLE();
							nametbl[pos].original = "layer_" + gdsLayer;
							nametbl[pos].replace = layer.getName();
							figure_group = nametbl[pos].node = Artwork.tech.boxNode;
							arc_type = nametbl[pos].arc = Schematics.tech.wire_arc;
							textheight = nametbl[pos].textheight = 0;
							justification = nametbl[pos].justification = TextDescriptor.Position.DOWNRIGHT;
							visible = nametbl[pos].visible = true;
							layer_ptr++;
						}
					}

					// now look for nodes to map MASK layers to
					for (int eindex = 0; eindex < layer_ptr; eindex++)
					{
						NAMETABLE nt = nametbl[eindex];
						String nodeName = nt.replace + "-node";
						NodeProto np = technology.findNodeProto(nodeName);
						if (np == null) np = Artwork.tech.boxNode;
						nt.node = np;
						nt.arc = technology.findArcProto(nt.replace);;
					}
				} else if (state == KINTERFACE)
				{
					if (active_view == VNETLIST)
					{
						// create a black-box symbol at the current scale
						Cell nnp = ViewChanges.makeIconForCell(current_cell);
						if (nnp == null)
						{
							System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create icon <" + current_cell.describe() + ">");
							errors++;
						}
					}
				} else if (state == KVIEW)
				{
					if (vendor == EVVIEWLOGIC && active_view != VNETLIST)
					{
						// now scan for BUS nets, and verify all wires connected to bus
						HashSet seenArcs = new HashSet();
						for(Iterator it = current_cell.getArcs(); it.hasNext(); )
						{
							ArcInst ai = (ArcInst)it.next();
							if (!seenArcs.contains(ai) && ai.getProto() == Schematics.tech.bus_arc)
							{
								// get name of arc
								String baseName = ai.getName();
								int openPos = baseName.indexOf('[');
								if (openPos >= 0) baseName = baseName.substring(0, openPos);

								// expand this arc, and locate non-bracketed names
								io_edcheck_busnames(ai, baseName, seenArcs);
							}
						}
					}

					ports = null;
				} else if (state == KPORT)
				{
					io_edallocport();
					double cX = 0, cY = 0;
					PortProto fpp = default_input;
					if (ports.direction == PortCharacteristic.IN)
					{
						cY = ipos;
						ipos += INCH;
					} else if (ports.direction == PortCharacteristic.BIDIR)
					{
						cX = 3*INCH;
						cY = bpos;
						bpos += INCH;
					} else if (ports.direction == PortCharacteristic.OUT)
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
						ppt.setCharacteristic(ports.direction);
					}
					port_reference = "";

					// move the property list to the free list
					for (EDPROPERTY property = properties; property != null; property = property.next)
						ppt.newVar(property.name, property.val);
					properties = null;
				} else if (state == KINSTANCE)
				{
					if (active_view == VNETLIST)
					{
						for (int Ix = 0; Ix < arrayx; Ix++)
						{
							for (int Iy = 0; Iy < arrayy; Iy++)
							{
								// create this instance in the current sheet
								double width = cellRefProto.getDefWidth();
								double height = cellRefProto.getDefHeight();
								width = (width + INCH - 1) / INCH;
								height = (height + INCH - 1) / INCH;

								// verify room for the icon
								if (sh_xpos != -1)
								{
									if ((sh_ypos + (height + 1)) >= SHEETHEIGHT)
									{
										sh_ypos = 1;
										if ((sh_xpos += sh_offset) >= SHEETWIDTH)
												sh_xpos = sh_ypos = -1; else
													sh_offset = 2;
									}
								}
								if (sh_xpos == -1)
								{
									// create the new page
									String nodename = cell_name + "{sch}";
									current_cell = Cell.makeInstance(library, nodename);
									if (current_cell == null) throw new IOException("Error creating cell");
									builtCells.add(current_cell);
									sh_xpos = sh_ypos = 1;
									sh_offset = 2;
								}

								// create this instance
								// find out where true center moves
								double cx = ((sh_xpos - (SHEETWIDTH >> 1)) * INCH);
								double cy = ((sh_ypos - (SHEETHEIGHT >> 1)) * INCH);
								Point2D size = getSizeAndMirror(cellRefProto);
								NodeInst ni = NodeInst.makeInstance(cellRefProto, new Point2D.Double(cx, cy), size.getX(), size.getY(), current_cell,
									io_edgettrans(orientation), null, 0);
								current_node = ni;
								if (ni == null)
								{
									System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create instance");
									errors++;
									break;
								} else
								{
									if (cellRefProto instanceof Cell)
									{
										if (((Cell)cellRefProto).isWantExpanded())
											ni.setExpanded();
									}

									// update the current position
									if ((width + 2) > sh_offset)
										sh_offset = (int)width + 2;
									if ((sh_ypos += (height + 1)) >= SHEETHEIGHT)
									{
										sh_ypos = 1;
										if ((sh_xpos += sh_offset) >= SHEETWIDTH)
											sh_xpos = sh_ypos = -1; else
												sh_offset = 2;
									}

									// name the instance
									if (instance_reference.length() > 0)
									{
										// if single element or array with no offset
										// construct the representative extended EDIF name (includes [...])
										String nodename = instance_reference;
										if ((arrayx > 1 || arrayy > 1) &&
											(deltaxX != 0 || deltaxY != 0 || deltayX != 0 || deltayY != 0))
										{
											// if array in the x dimension
											if (arrayx > 1)
											{
												if (arrayy > 1)
													nodename = instance_reference + "[" + Ix + "," + Iy + "]";
												else
													nodename = instance_reference + "[" + Ix + "]";
											} else if (arrayy > 1)
												nodename = instance_reference + "[" + Iy + "]";
										}

										// check for array element descriptor
										if (arrayx > 1 || arrayy > 1)
										{
											// array descriptor is of the form index:index:range index:index:range
											String baseName = Ix + ":" + ((deltaxX == 0 && deltayX == 0) ? arrayx-1:Ix) + ":" + arrayx +
												" " + Iy + ":" + ((deltaxY == 0 && deltayY == 0) ? arrayy-1:Iy) + ":" + arrayy;
											ni.newVar("EDIF_array", baseName);
										}

										/* now set the name of the component (note that Electric allows any string
										 * of characters as a name, this name is open to the user, for ECO and other
										 * consistancies, the EDIF_name is saved on a variable)
										 */
										if (instance_reference.equalsIgnoreCase(instance_name))
										{
											ni.setName(nodename);
										} else
										{
											// now add the original name as the displayed name (only to the first element)
											if (Ix == 0 && Iy == 0)
											{
												ni.setName(instance_name);
											}

											// now save the EDIF name (not displayed)
											ni.newVar("EDIF_name", nodename);
										}
									}
								}
							}
						}
					}

					// move the property list to the free list
					for (EDPROPERTY property = properties; property != null; property = property.next)
					{
						if (current_node != null)
							current_node.newVar(property.name, property.val);
					}
					properties = null;
					instance_reference = "";
					current_node = null;
					io_edfreesavedptlist();
				} else if (state == KNET)
				{
					// move the property list to the free list
					for (EDPROPERTY property = properties; property != null; property = property.next)
					{
						if (current_arc != null)
							current_arc.newVar(property.name, property.val);
					}
					properties = null;
					net_reference = "";
					current_arc = null;
					if (geometry != GBUS) geometry = GUNKNOWN;
					io_edfreesavedptlist();
				} else if (state == KNETBUNDLE)
				{
					bundle_reference = "";
					current_arc = null;
					geometry = GUNKNOWN;
					io_edfreesavedptlist();
				} else if (state == KPROPERTY)
				{
					if (active_view == VNETLIST || active_view == VSCHEMATIC)
					{
						// add as a variable to the current object
						Cell np = null;
						if (kstack[kstack_ptr - 1] == KINTERFACE)
						{
							// add to the {sch} view nodeproto
							String desiredName = cell_name + "{sch}";
							np = library.findNodeProto(desiredName);
							if (np == null)
							{
								// allocate the cell
								np = Cell.makeInstance(library, desiredName);
								if (np == null) throw new IOException("Error creating cell");
								builtCells.add(np);
								np.newVar(property_reference, pval);
							}
						} else if (kstack[kstack_ptr - 1] == KINSTANCE || kstack[kstack_ptr - 1] == KNET ||
							kstack[kstack_ptr - 1] == KPORT)
						{
							// add to the current property list, will be added latter
							io_edallocproperty(property_reference, pval);
						}
					}
					property_reference = "";
					io_edfreesavedptlist();
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

					if (instcount == 1 && cellRefProto != null)
					{
						for (int Ix = 0; Ix < arrayx; Ix++)
						{
							double lx = instptx + Ix * deltaxX;
							double ly = instpty + Ix * deltaxY;
							for (int Iy = 0; Iy < arrayy; Iy++)
							{
								Point2D size = getSizeAndMirror(cellRefProto);
								NodeInst ni = NodeInst.makeInstance(cellRefProto, new Point2D.Double(lx, ly),
									size.getX(), size.getY(), current_cell, io_edgetrot(orientation), null, 0);
								current_node = ni;
								if (ni == null)
								{
									System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create instance");
									errors++;

									// and exit for loop
									Ix = arrayx;
									Iy = arrayy;
								} else
								{
									if (cellRefProto instanceof Cell)
									{
										if (((Cell)cellRefProto).isWantExpanded())
											ni.setExpanded();
									}
								}
								if (geometry == GPIN && lx == 0 && ly == 0)
								{
									// determine an appropriate port name
									String portname = port_name;
									for (int dup = 0; ; dup++)
									{
										PortProto ppt = current_cell.findPortProto(portname);
										if (ppt == null) break;
										portname = port_name + "_" + (dup+1);
									}

									// only once
									Ix = arrayx;
									Iy = arrayy;
									PortInst pi = ni.findPortInstFromProto(default_iconport);
									Export ppt = Export.newInstance(current_cell, pi, portname);
									if (ppt == null)
									{
										System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create port <" + portname + ">");
										errors++;
									} else
									{
										// locate the direction of the port
										for (EDPORT eport = ports; eport != null; eport = eport.next)
										{
											if (eport.reference.equalsIgnoreCase(port_reference))
											{
												// set the direction
												ppt.setCharacteristic(eport.direction);
												break;
											}
										}
									}
								} else
								{
									// name the instance
									if (instance_reference.length() > 0)
									{
										String nodename = instance_reference;
										if ((arrayx > 1 || arrayy > 1) &&
											(deltaxX != 0 || deltaxY != 0 || deltayX != 0 || deltayY != 0))
										{
											// if array in the x dimension
											if (arrayx > 1)
											{
												if (arrayy > 1)
													nodename = instance_reference + "[" + Ix + "," + Iy + "]";
												else
													nodename = instance_reference + "[" + Ix + "]";
											} else if (arrayy > 1)
												nodename = instance_reference + "[" + Iy + "]";
										}

										// check for array element descriptor
										if (arrayx > 1 || arrayy > 1)
										{
											// array descriptor is of the form index:index:range index:index:range
											String baseName = Ix + ":" + ((deltaxX == 0 && deltayX == 0) ? arrayx-1:Ix) + ":" + arrayx +
												" " + Iy + ":" + ((deltaxY == 0 && deltayY == 0) ? arrayy-1:Iy) + ":" + arrayy;
											ni.newVar("EDIF_array", baseName);
										}

										/* now set the name of the component (note that Electric allows any string
				   						 * of characters as a name, this name is open to the user, for ECO and other
										 * consistancies, the EDIF_name is saved on a variable)
										 */
										TextDescriptor td = null;
										if (instance_reference.equalsIgnoreCase(instance_name))
										{
											ni.setName(nodename);
											td = ni.getNameTextDescriptor();
										} else
										{
											// now add the original name as the displayed name (only to the first element)
											if (Ix == 0 && Iy == 0) ni.setName(instance_name);

											// now save the EDIF name (not displayed)
											Variable var = ni.newVar("EDIF_name", nodename);
											td = var.getTextDescriptor();
										}

										// now check for saved name attributes
										if (save_points.size() != 0)
										{
											// now set the position, relative to the center of the current object
											EPT sP0 = (EPT)save_points.get(0);
											double xoff = sP0.x - ni.getAnchorCenterX();
											double yoff = sP0.y - ni.getAnchorCenterY();

											// convert to quarter lambda units
											xoff = 4 * xoff;
											yoff = 4 * yoff;

											/*
											 * determine the size of text, 0.0278 in == 2 points or 36 (2xpixels) == 1 in
											 * fonts range from 4 to 20 points
											 */
											td.setRelSize(io_ediftextsize(save_textheight));
											td.setOff(xoff, yoff);
											td.setPos(save_justification);
										}
									}
								}
								if (deltayX == 0 && deltayY == 0) break;

								// bump the y delta and x deltas
								lx += deltayX;
								ly += deltayY;
							}
							if (deltaxX == 0 && deltaxY == 0) break;
						}
					}
					io_edfreeptlist();
				} else if (state == KPORTREF)
				{
					// check for the last pin
					NodeInst fni = current_node;
					PortProto fpp = current_port;
					if (port_reference.length() > 0)
					{
						// For internal pins of an instance, determine the base port location and other pin assignments
						ArcProto ap = Generic.tech.unrouted_arc;
						NodeInst ni = null;
						PortProto pp = null;
						if (instance_reference.length() > 0)
						{
							String nodename = instance_reference;

							// locate the node and and port
							if (active_view == VNETLIST)
							{
								// scan all pages for this nodeinst
								for(Iterator it = library.getCells(); it.hasNext(); )
								{
									Cell cell = (Cell)it.next();
									if (!cell.getName().equalsIgnoreCase(cell_name)) continue;
									for(Iterator nIt = cell.getNodes(); nIt.hasNext(); )
									{
										NodeInst oNi = (NodeInst)nIt.next();
										Variable var = oNi.getVar("EDIF_name");
										if (var != null && var.getPureValue(-1).equalsIgnoreCase(nodename)) { ni = oNi;   break; }
										String name = oNi.getName();
										if (name.equalsIgnoreCase(nodename)) { ni = oNi;   break; }
									}
									if (ni != null) break;
								}
								if (ni == null)
								{
									System.out.println("error, line #" + lineReader.getLineNumber() + ": could not locate netlist node (" + nodename + ")");
									return;
								}
							} else
							{
								// net always references the current page
								for(Iterator nIt = current_cell.getNodes(); nIt.hasNext(); )
								{
									NodeInst oNi = (NodeInst)nIt.next();
									Variable var = oNi.getVar("EDIF_name");
									if (var != null && var.getPureValue(-1).equalsIgnoreCase(nodename)) { ni = oNi;   break; }
									String name = oNi.getName();
									if (name.equalsIgnoreCase(nodename)) { ni = oNi;   break; }
								}
								if (ni == null)
								{
									System.out.println("error, line #" + lineReader.getLineNumber() + ": could not locate schematic node '" +
										nodename + "' in cell " + current_cell.describe());
									return;
								}
								if (!isarray) ap = Schematics.tech.wire_arc; else
									ap = Schematics.tech.bus_arc;
							}

							// locate the port for this portref
							pp = ni.getProto().findPortProto(port_reference);
							if (pp == null)
							{
								System.out.println("error, line #" + lineReader.getLineNumber() + ": could not locate port (" +
									port_reference + ") on node (" + nodename + ")");
								return;
							}

							// we have both, set global variable
							current_node = ni;
							current_port = pp;
							Cell np = ni.getParent();

							// create extensions for net labels on single pin nets (externals), and placeholder for auto-routing later
							if (active_view == VNETLIST)
							{
								// route all pins with an extension
								if (ni != null)
								{
									Poly portPoly = ni.findPortInstFromProto(pp).getPoly();
									double hx = portPoly.getCenterX();
									double hy = portPoly.getCenterY();
									double lx = hx;
									double ly = hy;
									if (pp.getCharacteristic() == PortCharacteristic.IN) lx = hx - (INCH/10); else
										if (pp.getCharacteristic() == PortCharacteristic.BIDIR) ly = hy - (INCH/10); else
											if (pp.getCharacteristic() == PortCharacteristic.OUT) lx = hx + (INCH/10);

									// need to create a destination for the wire
									ArcProto lap = Schematics.tech.bus_arc;
									PortProto lpp = default_busport;
									NodeInst lni = null;
									if (isarray)
									{
										lni = io_edifiplacepin(Schematics.tech.busPinNode, (lx+hx)/2, (ly+hy)/2, hx-lx, hy-ly, 0, np);
										if (lni == null)
										{
											System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create bus pin");
											return;
										}
									} else
									{
										lni = io_edifiplacepin(Schematics.tech.wirePinNode, (lx+hx)/2, (ly+hy)/2, hx-lx, hy-ly, 0, np);
										if (lni == null)
										{
											System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create wire pin");
											return;
										}
										lpp = default_port;
										lap = Schematics.tech.wire_arc;
									}
									PortInst head = lni.findPortInstFromProto(lpp);
									PortInst tail = ni.findPortInstFromProto(pp);
									current_arc = ArcInst.makeInstance(lap, lap.getDefaultWidth(), head, tail);
									if (current_arc == null)
										System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create auto-path");
									else
										io_ednamearc(current_arc);
								}
							}
						} else
						{
							// external port reference, look for a off-page reference in {sch} with this port name
							Cell np = null;
							for(Iterator it = library.getCells(); it.hasNext(); )
							{
								Cell cell = (Cell)it.next();
								if (cell.getName().equalsIgnoreCase(cell_name) && cell.getView() == View.SCHEMATIC)
								{
									np = cell;
									break;
								}
							}
							if (np == null)
							{
								System.out.println("error, line #" + lineReader.getLineNumber() + ": could not locate top level schematic");
								return;
							}

							// now look for an instance with the correct port name
							pp = np.findPortProto(port_reference);
							if (pp == null)
							{
								if (original.length() > 0)
									pp = np.findPortProto(original);
								if (pp == null)
								{
									System.out.println("error, line #" + lineReader.getLineNumber() + ": could not locate port '" + port_reference + "'");
									return;
								}
							}
							fni = ((Export)pp).getOriginalPort().getNodeInst();
							fpp = ((Export)pp).getOriginalPort().getPortProto();
							Poly portPoly = fni.findPortInstFromProto(fpp).getPoly();
							double lx = portPoly.getCenterX();
							double ly = portPoly.getCenterY();

							// determine x position by original placement
							if (pp.getCharacteristic() == PortCharacteristic.IN) lx = 1*INCH; else
								if (pp.getCharacteristic() == PortCharacteristic.BIDIR) ly = 4*INCH; else
									if (pp.getCharacteristic() == PortCharacteristic.OUT) lx = 5*INCH;

							// need to create a destination for the wire
							if (isarray)
							{
								ni = io_edifiplacepin(Schematics.tech.busPinNode, lx, ly,
									Schematics.tech.busPinNode.getDefWidth(), Schematics.tech.busPinNode.getDefHeight(), 0, np);
								if (ni == null)
								{
									System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create bus pin");
									return;
								}
								pp = default_busport;
							} else
							{
								ni = io_edifiplacepin(Schematics.tech.wirePinNode, lx, ly,
									Schematics.tech.wirePinNode.getDefWidth(), Schematics.tech.wirePinNode.getDefHeight(), 0, np);
								if (ni == null)
								{
									System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create wire pin");
									return;
								}
								pp = default_port;
							}
							if (!isarray) ap = Schematics.tech.wire_arc; else
								ap = Schematics.tech.bus_arc;
						}

						// now connect if we have from node and port
						if (fni != null && fpp != null)
						{
							if (fni.getParent() != ni.getParent())
							{
								System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create path (arc) between cells " +
									fni.getParent().describe() + " and " + ni.getParent().describe());
							} else
							{
								// locate the position of the new ports
								Poly fPortPoly = fni.findPortInstFromProto(fpp).getPoly();
								double lx = fPortPoly.getCenterX();
								double ly = fPortPoly.getCenterY();
								Poly portPoly = ni.findPortInstFromProto(pp).getPoly();
								double hx = portPoly.getCenterX();
								double hy = portPoly.getCenterY();

								// for nets with no physical representation ...
								current_arc = null;
								double dist = 0;
								PortInst head = fni.findPortInstFromProto(fpp);
								Point2D headPt = new Point2D.Double(lx, ly);
								PortInst tail = ni.findPortInstFromProto(pp);
								Point2D tailPt = new Point2D.Double(hx, hy);
								if (lx != hx || ly != hy) dist = headPt.distance(tailPt);
								if (dist <= 1)
								{
									current_arc = ArcInst.makeInstance(ap, ap.getDefaultWidth(), head, tail, headPt, tailPt, null);
									if (current_arc == null)
									{
										System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create path (arc)");
									}
								} else if (active_view == VNETLIST)
								{
									// use unrouted connection for NETLIST views
									current_arc = ArcInst.makeInstance(ap, ap.getDefaultWidth(), head, tail, headPt, tailPt, null);
									if (current_arc == null)
									{
										System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create auto-path in portRef");
									}
								}

								// add the net name
								if (current_arc != null)
									io_ednamearc(current_arc);
							}
						}
					}
				} else if (state == KINSTANCEREF)
				{
				} else if (state == KPATH)
				{
					// check for openShape type path
//					if (path_width == 0 &&
//						geometry != GNET && geometry != GBUS) goto dopoly;
					List fList = new ArrayList();
					NodeProto np = Schematics.tech.wirePinNode;
					if (geometry == GBUS || isarray) np = Schematics.tech.busPinNode;
					for(int i=0; i<points.size()-1; i++)
					{
						EPT point = (EPT)points.get(i);
						EPT nextPoint = (EPT)points.get(i+1);
						if (geometry == GNET || geometry == GBUS)
						{
							// create a pin to pin connection
							if (fList.size() == 0)
							{
								// look for the first pin
								fList = io_edfindport(current_cell, point.x, point.y, Schematics.tech.wire_arc);
								if (fList.size() == 0)
								{
									// create the "from" pin
									NodeInst ni = io_edifiplacepin(np, point.x, point.y, np.getDefWidth(), np.getDefHeight(), 0, current_cell);
									if (ni != null)
									{
										PortInst pi = ni.findPortInstFromProto(geometry == GBUS || isarray ?
											default_busport : default_port);
										fList.add(pi);
									}
								}
							}
							// now the second ...
							List tList = io_edfindport(current_cell, nextPoint.x, nextPoint.y, Schematics.tech.wire_arc);
							if (tList.size() == 0)
							{
								// create the "to" pin
								NodeInst ni = io_edifiplacepin(np, nextPoint.x, nextPoint.y, np.getDefWidth(), np.getDefHeight(), 0, current_cell);
								if (ni != null)
								{
									PortInst pi = ni.findPortInstFromProto(geometry == GBUS || isarray ?
										default_busport : default_port);
									tList.add(pi);
								}
							}

							if (fList.size() == 0 || tList.size() == 0)
							{
								System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create path");
								errors++;
							} else
							{
								// connect it
								ArcInst ai = null;
								for (int count = 0; count < fList.size() || count < tList.size(); count++)
								{
									boolean fbus = false;
									boolean tbus = false;
									PortInst lastPi = null;
									PortInst pi = null;
									if (count < fList.size())
									{
										lastPi = (PortInst)fList.get(count);
										NodeInst lastpin = pi.getNodeInst();
										PortProto lastport = pi.getPortProto();

										// check node for array variable
										Variable var = lastpin.getVar("EDIF_array");
										if (var != null) fbus = true; else
										{
											if (lastport.getName().endsWith("]")) fbus = true;
										}
									}
									if (count < tList.size())
									{
										pi = (PortInst)fList.get(count);
										NodeInst ni = pi.getNodeInst();
										PortProto pp = pi.getPortProto();

										// check node for array variable
										Variable var = ni.getVar("EDIF_array");
										if (var != null) tbus = true; else
										{
											if (pp.getName().endsWith("]")) tbus = true;
										}
									}

									// if bus to bus
									ArcProto ap = Schematics.tech.wire_arc;
									if ((lastPi.getPortProto() == default_busport || fbus) &&
										(pi.getPortProto() == default_busport || tbus)) ap = Schematics.tech.bus_arc;

									ai = ArcInst.makeInstance(ap, ap.getDefaultWidth(),
										lastPi, pi, new Point2D.Double(point.x, point.y), new Point2D.Double(nextPoint.x, nextPoint.y), null);
									if (ai == null)
									{
										System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create path (arc)");
										errors++;
									} else if (geometry == GNET && i == 0)
									{
										if (net_reference.length() > 0)
										{
											ai.newVar("EDIF_name", net_reference);
										}
										if (net_name.length() > 0)
										{
											// set name of arc but don't display name
											ai.setName(net_name);
										}
									} else if (geometry == GBUS && points == point)
									{
										if (bundle_reference.length() > 0)
										{
											ai.newVar("EDIF_name", bundle_reference);
										}
										if (bundle_name.length() > 0)
										{
											// set bus' EDIF name but don't display name
											ai.setName(bundle_name);
										}
									}
								}
								if (ai != null) current_arc = ai;
								fList = tList;
							}
						} else
						{
							// rectalinear paths with some width
							// create a path from here to there (orthogonal only now)
							double lx = point.x;
							double ly = point.y;
							double hx = lx;
							double hy = ly;
							if (lx > nextPoint.x) lx = nextPoint.x;
							if (hx < nextPoint.x) hx = nextPoint.x;
							if (ly > nextPoint.y) ly = nextPoint.y;
							if (hy < nextPoint.y) hy = nextPoint.y;
							if (ly == hy || extend_end)
							{
								ly -= path_width/2;
								hy += path_width/2;
							}
							if (lx == hx || extend_end)
							{
								lx -= path_width/2;
								hx += path_width/2;
							}
							NodeInst ni = io_edifiplacepin(figure_group, (lx+hx)/2, (ly+hy)/2, hx-lx, hy-ly,
								io_edgetrot(orientation), current_cell);
							if (ni == null)
							{
								System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create path");
								errors++;
							}
						}
					}
					io_edfreeptlist();
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
					string = "";
					save_points = points;
					points = new ArrayList();
					save_textheight = textheight;
					textheight = 0;
					save_justification = justification;
					justification = TextDescriptor.Position.DOWNRIGHT;
					orientation = OR0;
					visible = true;
				} else if (state == KSTRINGDISPLAY)
				{
					if (kstack_ptr <= 1)
					{
						System.out.println("error, line #" + lineReader.getLineNumber() + ": bad location for \"stringDisplay\"");
						errors++;
					} else if (kstack[kstack_ptr-1] == KRENAME)
					{
						// save the data and break
						io_edfreesavedptlist();
						string = "";
						save_points = points;
						points = new ArrayList();
						save_textheight = textheight;
						textheight = 0;
						save_justification = justification;
						justification = TextDescriptor.Position.DOWNRIGHT;
						orientation = OR0;
						visible = true;
					}

					// output the data for annotate (display graphics) and string (on properties)
					else if (kstack[kstack_ptr-1] == KANNOTATE || kstack[kstack_ptr-1] == KSTRING)
					{
						// supress this if it starts with "["
						if (!string.startsWith("[") && points.size() != 0)
						{
							// see if a pre-existing node or arc exists to add text
							ArcInst ai = null;
							NodeInst ni = null;
							String key = property_reference;
							EPT p0 = (EPT)points.get(0);
							double xoff = 0, yoff = 0;
							if (property_reference.length() > 0 && current_node != null)
							{
								ni = current_node;
								xoff = p0.x - ni.getAnchorCenterX();
								yoff = p0.y - ni.getAnchorCenterY();
							}
							else if (property_reference.length() > 0 && current_arc != null)
							{
								ai = current_arc;
								xoff = p0.x - (ai.getHead().getLocation().getX() + ai.getTail().getLocation().getX()) / 2;
								yoff = p0.y - (ai.getHead().getLocation().getY() + ai.getTail().getLocation().getY()) / 2;
							} else
							{
								// create the node instance
								ni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, new Point2D.Double(p0.x, p0.y), 0, 0, current_cell);
								key = "EDIF_annotate";
							}
							if (ni != null || ai != null)
							{
								Variable var = null;
								if (ni != null)
								{
									var = ni.newVar(key, string);
									if (var != null && visible) var.setDisplay(true);

									// now set the position, relative to the center of the current object
									xoff = p0.x - ni.getAnchorCenterX();
									yoff = p0.y - ni.getAnchorCenterY();
								} else
								{
									var = ai.newVar(key, string);
									if (var != null && visible) var.setDisplay(true);

									// now set the position, relative to the center of the current object
									xoff = p0.x - (ai.getHead().getLocation().getX() + ai.getTail().getLocation().getX()) / 2;
									yoff = p0.y - (ai.getHead().getLocation().getY() + ai.getTail().getLocation().getY()) / 2;
								}

								// convert to quarter lambda units
								xoff = 4 * xoff;
								yoff = 4 * yoff;

								// determine the size of text, 0.0278 in == 2 points or 36 (2xpixels) == 1 in fonts range from 4 to 31
								TextDescriptor td = var.getTextDescriptor();
								td.setRelSize(io_ediftextsize(textheight));
								td.setOff(xoff, yoff);
								td.setPos(justification);
							} else
							{
								System.out.println("error, line #" + lineReader.getLineNumber() + ": nothing to attach text to");
								errors++;
							}
						}
					}

					// clean up DISPLAY attributes
					io_edfreeptlist();
					cur_nametbl = null;
					visible = true;
					justification = TextDescriptor.Position.DOWNRIGHT;
					textheight = 0;
				} else if (state == KDOT)
				{
					if (geometry == GPIN)
					{
						EDPORT eport = null;
						for (EDPORT e = ports; e != null; e = e.next)
						{
							if (e.reference.equalsIgnoreCase(name)) { eport = e;   break; }
						}
						if (eport != null)
						{
							// create a internal wire port using the pin-proto, and create the port and export
							EPT p0 = (EPT)points.get(0);
							Point2D size = getSizeAndMirror(cellRefProto);
							NodeInst ni = io_edifiplacepin(cellRefProto, p0.x, p0.y, size.getX(), size.getY(), io_edgetrot(orientation), current_cell);
							if (ni == null)
							{
								System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create pin");
								errors++;
							}
							String portname = eport.name;
							for (int dup = 0; ; dup++)
							{
								PortProto ppt = current_cell.findPortProto(portname);
								if (ppt == null) break;
								portname = eport.name + "_" + (dup+1);
							}
							PortInst pi = ni.findPortInstFromProto(default_iconport);
							Export ppt = Export.newInstance(current_cell, pi, portname);
							if (ppt == null)
							{
								System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create port <" + portname + ">");
								errors++;
							} else
							{
								// set the direction
								ppt.setCharacteristic(eport.direction);
							}
						}
					} else
					{
						// create the node instance
						EPT p0 = (EPT)points.get(0);
						NodeInst ni = NodeInst.makeInstance(figure_group != null ? figure_group : Artwork.tech.boxNode,
							new Point2D.Double(p0.x, p0.y), 0, 0, current_cell);
						if (ni == null)
						{
							System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create rectangle");
							errors++;
						}
					}
					io_edfreeptlist();
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
						} else if (figure_group == Artwork.tech.openedDottedPolygonNode)
						{
							double cx = (p0.x + p1.x) / 2;
							double cy = (p0.y + p1.y) / 2;
							Point2D [] pts = new Point2D[5];
							pts[0] = new Point2D.Double(p0.x-cx, p0.y-cy);
							pts[1] = new Point2D.Double(p0.x-cx, p1.y-cy);
							pts[2] = new Point2D.Double(p1.x-cx, p1.y-cy);
							pts[3] = new Point2D.Double(p1.x-cx, p0.y-cy);
							pts[4] = new Point2D.Double(p0.x-cx, p0.y-cy);

							// store the trace information
							ni.newVar(NodeInst.TRACE, pts);
						}
						else if (geometry == GPIN)
						{
							// create a rectangle using the pin-proto, and create the port and export ensure full sized port
							Point2D size = getSizeAndMirror(cellRefProto);
							ni = io_edifiplacepin(cellRefProto, (lx+hx)/2, (ly+hy)/2, size.getX(), size.getY(),
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
					// set array values
					if (points.size() >= 3)
					{
						double [] x = new double[3];
						double [] y = new double[3];
						EPT p0 = (EPT)points.get(0);
						EPT p1 = (EPT)points.get(1);
						EPT p2 = (EPT)points.get(2);
						x[0] = p0.x;   y[0] = p0.y;
						x[1] = p1.x;   y[1] = p1.y;
						x[2] = p2.x;   y[2] = p2.y;
						io_ededif_arc(x, y);
					}
					io_edfreeptlist();
				} else if (state == KSYMBOL || state == KPAGE)
				{
					active_view = VNULL;
				} else if (state == KOPENSHAPE || state == KPOLYGON)
				{
//	dopoly:
					if (points.size() == 0) return;

					// get the bounds of the poly
					EPT p0 = (EPT)points.get(0);
					double lx = p0.x;
					double ly = p0.y;
					double hx = lx;
					double hy = ly;
					for(int i=1; i<points.size(); i++)
					{
						EPT point = (EPT)points.get(i);
						if (lx > point.x) lx = point.x;
						if (hx < point.x) hx = point.x;
						if (ly > point.y) ly = point.y;
						if (hy < point.y) hy = point.y;
					}
					if (lx != hx || ly != hy)
					{
						NodeProto np = figure_group;
						if (figure_group == null || figure_group == Artwork.tech.boxNode)
						{
							if (state == KPOLYGON) np = Artwork.tech.closedPolygonNode; else
								np = Artwork.tech.openedPolygonNode;
						}
						Point2D size = getSizeAndMirror(np);
						NodeInst ni = NodeInst.makeInstance(np, new Point2D.Double((lx+hx)/2, (ly+hy)/2), size.getX(), size.getY(),
							current_cell, io_edgetrot(orientation), null, 0);
						if (ni == null)
						{
							System.out.println("error, line #" + lineReader.getLineNumber() + ": could not create polygon");
							errors++;
						} else
						{
							Point2D [] trace = new Point2D[points.size()];
							double cx = (hx + lx) / 2;
							double cy = (hy + ly) / 2;
							for(int i=0; i<points.size(); i++)
							{
								EPT point = (EPT)points.get(i);
								trace[i] = new Point2D.Double(point.x - cx, point.y - cy);
							}

							// store the trace information
							ni.newVar(NodeInst.TRACE, trace);
						}
					}
					io_edfreeptlist();
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
						library.setCurCell((Cell)cellRefProto);
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

	private class EDIFKEY
	{
		private String name;							/* the name of the keyword */
		private EDIFKEY [] stateArray;						/* edif state */

		private EDIFKEY(String name)
		{
			this.name = name;
			edifKeys.put(name.toLowerCase(), this);
		}

		protected void func() throws IOException {}
	};

	private EDIFKEY KUNKNOWN = new EDIFKEY("");

	private EDIFKEY KINIT = new EDIFKEY("");

	private EDIFKEY KANNOTATE = new EDIFKEY("annotate");

	private EDIFKEY KARC = new EDIFKEY("arc");

	private EDIFKEY KARRAY = new KeyArray();
	private class KeyArray extends EDIFKEY
	{
		private KeyArray() { super("array"); }
		protected void func()
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
		}
	}

	private EDIFKEY KAUTHOR = new EDIFKEY ("author");

	private EDIFKEY KBOUNDINGBOX = new KeyBoundingBox();
	private class KeyBoundingBox extends EDIFKEY
	{
		private KeyBoundingBox() { super("boundingBox"); }
		protected void func()
			throws IOException
		{
			figure_group = Artwork.tech.openedDottedPolygonNode;
		}
	}

	private EDIFKEY KCELL = new KeyCell();
	private class KeyCell extends EDIFKEY
	{
		private KeyCell() { super("cell"); }
		protected void func()
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
		}
	}

	private EDIFKEY KCELLREF = new KeyCellRef();
	private class KeyCellRef extends EDIFKEY
	{
		private KeyCellRef() { super("cellRef"); }
		protected void func()
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
				return;
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
				builtCells.add(proto);
			}

			// set the parent
			cellRefProto = proto;
		}
	}

	private EDIFKEY KCELLTYPE = new EDIFKEY("cellType");

	private EDIFKEY KCIRCLE = new KeyCircle();
	private class KeyCircle extends EDIFKEY
	{
		private KeyCircle() { super("circle"); }
		protected void func()
			throws IOException
		{
			io_edfreeptlist();
			orientation = OR0;
		}
	}

	private EDIFKEY KCOLOR = new EDIFKEY("color");

	private EDIFKEY KCOMMENT = new EDIFKEY("comment");

	private EDIFKEY KCOMMENTGRAPHICS = new EDIFKEY("commentGraphics");

	private EDIFKEY KCONNECTLOCATION = new EDIFKEY("connectLocation");

	private EDIFKEY KCONTENTS = new KeyContents();
	private class KeyContents extends EDIFKEY { private KeyContents() { super("contents"); } }

	private EDIFKEY KCORNERTYPE = new KeyCornerType();
	private class KeyCornerType extends EDIFKEY
	{
		private KeyCornerType() { super("cornerType"); }
		protected void func()
			throws IOException
		{
			// get the endtype
			io_edget_token((char)0);
		}
	}

	private EDIFKEY KCURVE = new EDIFKEY("curve");

	private EDIFKEY KDATAORIGIN = new EDIFKEY("dataOrigin");

	private EDIFKEY KDCFANOUTLOAD = new EDIFKEY("dcFanoutLoad");

	private EDIFKEY KDCMAXFANOUT = new EDIFKEY("dcMaxFanout");

	private EDIFKEY KDELTA = new KeyDelta();
	private class KeyDelta extends EDIFKEY
	{
		private KeyDelta() { super("delta"); }
		protected void func()
			throws IOException
		{
			deltaxX = deltaxY = 0;
			deltayX = deltayY = 0;
			deltapts = 0;
		}
	}

	private EDIFKEY KDESIGN = new KeyDesign();
	private class KeyDesign extends EDIFKEY
	{
		private KeyDesign() { super("design"); }
		protected void func()
			throws IOException
		{
			// get the name of the cell
			String aName = io_edget_token((char)0);
		}
	}

	private EDIFKEY KDESIGNATOR = new EDIFKEY("designator");

	private EDIFKEY KDIRECTION = new KeyDirection();
	private class KeyDirection extends EDIFKEY
	{
		private KeyDirection() { super("direction"); }
		protected void func()
			throws IOException
		{
			// get the direction
			String aName = io_edget_token((char)0);

			if (aName.equalsIgnoreCase("INOUT")) direction = PortCharacteristic.IN; else
				if (aName.equalsIgnoreCase("INPUT")) direction = PortCharacteristic.BIDIR; else
					if (aName.equalsIgnoreCase("OUTPUT")) direction = PortCharacteristic.OUT;
		}
	}

	private EDIFKEY KDISPLAY = new KeyDisplay();
	private class KeyDisplay extends EDIFKEY
	{
		private KeyDisplay() { super("display"); }
		protected void func()
			throws IOException
		{
			io_edfigure();
		}
	}

	private EDIFKEY KDOT = new KeyDot();
	private class KeyDot extends EDIFKEY
	{
		private KeyDot() { super("dot"); }
		protected void func()
			throws IOException
		{
			io_edfreeptlist();
			orientation = OR0;
		}
	}

	private EDIFKEY KSCALEDINTEGER = new EDIFKEY("e");

	private EDIFKEY KEDIF = new EDIFKEY("edif");

	private EDIFKEY KEDIFLEVEL = new EDIFKEY("edifLevel");

	private EDIFKEY KEDIFVERSION = new EDIFKEY("edifVersion");

	private EDIFKEY KENDTYPE = new KeyEndType();
	private class KeyEndType extends EDIFKEY
	{
		private KeyEndType() { super("endType"); }
		protected void func()
			throws IOException
		{
			// get the endtype
			String type = io_edget_token((char)0);

			if (type.equalsIgnoreCase("EXTEND")) extend_end = true;
		}
	}

	private EDIFKEY KEXTERNAL = new KeyExternal();
	private class KeyExternal extends EDIFKEY
	{
		private KeyExternal() { super("external"); }
		protected void func()
			throws IOException
		{
			// get the name of the library
			String aName = io_edget_token((char)0);
		}
	}

	private EDIFKEY KFABRICATE = new KeyFabricate();
	private class KeyFabricate extends EDIFKEY
	{
		private KeyFabricate() { super("fabricate"); }
		protected void func()
			throws IOException
		{
			int pos = layer_ptr;

			nametbl[pos] = new NAMETABLE();

			// first get the original and replacement layers
			nametbl[pos].original = io_edget_token((char) 0);
			nametbl[pos].replace = io_edget_token((char) 0);
			nametbl[pos].textheight = 0;
			nametbl[pos].justification = TextDescriptor.Position.DOWNRIGHT;
			nametbl[pos].visible = true;

			// now bump the position
			layer_ptr++;
		}
	}

	private EDIFKEY KFALSE = new KeyFalse();
	private class KeyFalse extends EDIFKEY
	{
		private KeyFalse() { super("false"); }
		protected void func()
			throws IOException
		{
			// check previous keyword
			if (kstack_ptr > 1 && kstack[kstack_ptr-1] == KVISIBLE)
			{
				visible = false;
				if (cur_nametbl != null)
					cur_nametbl.visible = false;
			}
		}
	}

	private EDIFKEY KFIGURE = new KeyFigure();
	private class KeyFigure extends EDIFKEY
	{
		private KeyFigure() { super("figure"); }
		protected void func()
			throws IOException
		{
			io_edfigure();
		}
	}

	private EDIFKEY KFIGUREGROUP = new EDIFKEY("figureGroup");

	private EDIFKEY KFIGUREGROUPOVERRIDE = new KeyFigureGroupOverride();
	private class KeyFigureGroupOverride extends EDIFKEY
	{
		private KeyFigureGroupOverride() { super("figureGroupOverride"); }
		protected void func()
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
					return;
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
			justification = nametbl[pos].justification = TextDescriptor.Position.DOWNRIGHT;
			visible = nametbl[pos].visible = true;

			// now sort the list
			layer_ptr++;
		}
	}

	private EDIFKEY KFILLPATTERN = new EDIFKEY("fillpattern");

	private EDIFKEY KGRIDMAP = new EDIFKEY("gridMap");

	private EDIFKEY KINSTANCE = new KeyInstance();
	private class KeyInstance extends EDIFKEY
	{
		private KeyInstance() { super("instance"); }
		protected void func()
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
		}
	}

	private EDIFKEY KINSTANCEREF = new KeyInstanceRef();
	private class KeyInstanceRef extends EDIFKEY
	{
		private KeyInstanceRef() { super("instanceRef"); }
		protected void func()
			throws IOException
		{
			instance_reference = "";
			if (io_edcheck_name())
			{
				instance_reference = name;
			}
		}
	}

	private EDIFKEY KINTEGER = new KeyInteger();
	private class KeyInteger extends EDIFKEY
	{
		private KeyInteger() { super("integer"); }
		protected void func()
			throws IOException
		{
			String value = io_edget_token((char)0);

			pval = new Integer(TextUtils.atoi(value));
		}
	}

	private EDIFKEY KINTERFACE = new KeyInterface();
	private class KeyInterface extends EDIFKEY
	{
		private KeyInterface() { super("interface"); }
		protected void func()
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
				builtCells.add(current_cell);
			}
			else current_cell = np;

			// now set the current position in the schematic page
			ipos = bpos = opos = 0;
		}
	}

	private EDIFKEY KJOINED = new EDIFKEY("joined");

	private EDIFKEY KJUSTIFY = new KeyJustify();
	private class KeyJustify extends EDIFKEY
	{
		private KeyJustify() { super("justify"); }
		protected void func()
			throws IOException
		{
			// get the textheight value of the point
			String val = io_edget_token((char)0);
			if (val.equalsIgnoreCase("UPPERLEFT")) justification = TextDescriptor.Position.UPRIGHT;
			else if (val.equalsIgnoreCase("UPPERCENTER")) justification = TextDescriptor.Position.UP;
			else if (val.equalsIgnoreCase("UPPERRIGHT")) justification = TextDescriptor.Position.UPLEFT;
			else if (val.equalsIgnoreCase("CENTERLEFT")) justification = TextDescriptor.Position.RIGHT;
			else if (val.equalsIgnoreCase("CENTERCENTER")) justification = TextDescriptor.Position.CENT;
			else if (val.equalsIgnoreCase("CENTERRIGHT")) justification = TextDescriptor.Position.LEFT;
			else if (val.equalsIgnoreCase("LOWERLEFT")) justification = TextDescriptor.Position.DOWNRIGHT;
			else if (val.equalsIgnoreCase("LOWERCENTER")) justification = TextDescriptor.Position.DOWN;
			else if (val.equalsIgnoreCase("LOWERRIGHT")) justification = TextDescriptor.Position.DOWNLEFT;
			else
			{
				System.out.println("warning, line #" + lineReader.getLineNumber() + ": unknown keyword <" + val + ">");
				warnings++;
				return;
			}

			if (cur_nametbl != null)
				cur_nametbl.justification = justification;
		}
	}

	private EDIFKEY KKEYWORDDISPLAY = new EDIFKEY("keywordDisplay");

	private EDIFKEY KEDIFKEYLEVEL = new EDIFKEY("keywordLevel");

	private EDIFKEY KEDIFKEYMAP = new EDIFKEY("keywordMap");

	private EDIFKEY KLIBRARY = new KeyLibrary();
	private class KeyLibrary extends EDIFKEY
	{
		private KeyLibrary() { super("library"); }
		protected void func()
			throws IOException
		{
			// get the name of the library
			String aName = io_edget_token((char)0);
		}
	}

	private EDIFKEY KLIBRARYREF = new EDIFKEY("libraryRef");

	private EDIFKEY KLISTOFNETS = new EDIFKEY("listOfNets");

	private EDIFKEY KLISTOFPORTS = new EDIFKEY("listOfPorts");

	private EDIFKEY KMEMBER = new KeyMember();
	private class KeyMember extends EDIFKEY
	{
		private KeyMember() { super("member"); }
		protected void func()
			throws IOException
		{
			memberx = -1; // no member
			membery = -1; // no member
			if (io_edcheck_name())
			{
				if (kstack[kstack_ptr-1] == KPORTREF) port_reference = name; else
					if (kstack[kstack_ptr-1] == KINSTANCEREF) instance_reference = name;
			}
		}
	}

	private EDIFKEY KNAME = new KeyName();
	private class KeyName extends EDIFKEY
	{
		private KeyName() { super("name"); }
		protected void func()
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
				justification = TextDescriptor.Position.DOWNRIGHT;
				textheight = 0;
				string = name;
			}
		}
	}

	/**
	 * net:  Define a net name followed by edifgbl and local pin list
	 *		(net NAME
	 *			(joined
	 *			(portRef NAME (instanceRef NAME))
	 *			(portRef NAME)))
	 */
	private EDIFKEY KNET = new KeyNet();
	private class KeyNet extends EDIFKEY
	{
		private KeyNet() { super("net"); }
		protected void func()
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
		}
	}

	private EDIFKEY KNETBUNDLE = new KeyNetBundle();
	private class KeyNetBundle extends EDIFKEY
	{
		private KeyNetBundle() { super("netBundle"); }
		protected void func()
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
		}
	}

	private EDIFKEY KNUMBER = new KeyNumber();
	private class KeyNumber extends EDIFKEY
	{
		private KeyNumber() { super("number"); }
		protected void func()
			throws IOException
		{
			pval = new Double(io_edgetnumber());
		}
	}

	private EDIFKEY KNUMBERDEFINITION = new EDIFKEY("numberDefinition");

	private EDIFKEY KOPENSHAPE = new KeyOpenShape();
	private class KeyOpenShape extends EDIFKEY
	{
		private KeyOpenShape() { super("openShape"); }
		protected void func()
			throws IOException
		{
			io_edfreeptlist();
			orientation = OR0;
		}
	}

	private EDIFKEY KORIENTATION = new KeyOrientation();
	private class KeyOrientation extends EDIFKEY
	{
		private KeyOrientation() { super("orientation"); }
		protected void func()
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
		}
	}

	private EDIFKEY KORIGIN = new EDIFKEY("origin");

	private EDIFKEY KOWNER = new EDIFKEY("owner");

	private EDIFKEY KPAGE = new KeyPage();
	private class KeyPage extends EDIFKEY
	{
		private KeyPage() { super("page"); }
		protected void func()
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
				builtCells.add(proto);
			} else
			{
				if (!builtCells.contains(proto)) ignoreblock = true;
			}

			current_cell = proto;
		}
	}

	private EDIFKEY KPAGESIZE = new EDIFKEY("pageSize");

	private EDIFKEY KPATH = new KeyPath();
	private class KeyPath extends EDIFKEY
	{
		private KeyPath() { super("path"); }
		protected void func()
			throws IOException
		{
			io_edfreeptlist();
			orientation = OR0;
			path_width = 0;
		}
	}

	private EDIFKEY KPATHWIDTH = new KeyPathWidth();
	private class KeyPathWidth extends EDIFKEY
	{
		private KeyPathWidth() { super("pathWidth"); }
		protected void func()
			throws IOException
		{
			// get the width string
			String width = io_edget_token((char)0);
			path_width = TextUtils.atoi(width);
		}
	}

	private EDIFKEY KPOINT = new EDIFKEY("point");

	private EDIFKEY KPOINTLIST = new EDIFKEY("pointList");

	private EDIFKEY KPOLYGON = new KeyPolygon();
	private class KeyPolygon extends EDIFKEY
	{
		private KeyPolygon() { super("polygon"); }
		protected void func()
			throws IOException
		{
			io_edfreeptlist();
			orientation = OR0;
		}
	}

	private EDIFKEY KPORT = new KeyPort();
	private class KeyPort extends EDIFKEY
	{
		private KeyPort() { super("port"); }
		protected void func()
			throws IOException
		{
			name = "";   original = "";
			direction = PortCharacteristic.IN;
			port_reference = "";
			isarray = false;
			arrayx = arrayy = 1;
			if (io_edcheck_name())
			{
				port_reference = name;
				port_name = name;
			}
		}
	}

	private EDIFKEY KPORTBUNDLE = new EDIFKEY("portBundle");

	private EDIFKEY KPORTIMPLEMENTATION = new KeyPortImplementation();
	private class KeyPortImplementation extends EDIFKEY
	{
		private KeyPortImplementation() { super("portImplementation"); }
		protected void func()
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
		}
	}

	private EDIFKEY KPORTINSTANCE = new EDIFKEY("portInstance");

	private EDIFKEY KPORTLIST = new EDIFKEY("portList");

	private EDIFKEY KPORTREF = new KeyPortRef();
	private class KeyPortRef extends EDIFKEY
	{
		private KeyPortRef() { super("portRef"); }
		protected void func()
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
		}
	}

	private EDIFKEY KPROGRAM = new KeyProgram();
	private class KeyProgram extends EDIFKEY
	{
		private KeyProgram() { super("program"); }
		protected void func()
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
		}
	}

	private EDIFKEY KPROPERTY = new KeyProperty();
	private class KeyProperty extends EDIFKEY
	{
		private KeyProperty() { super("property"); }
		protected void func()
			throws IOException
		{
			property_reference = "";
			name = "";   original = "";
			pval = null;
			if (io_edcheck_name())
			{
				property_reference = name;
				property_name = name;
			}
		}
	}

	private EDIFKEY KPROPERTYDISPLAY = new EDIFKEY("propertyDisplay");

	private EDIFKEY KPT = new KeyPt();
	private class KeyPt extends EDIFKEY
	{
		private KeyPt() { super("pt"); }
		protected void func()
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
		}
	}

	private EDIFKEY KRECTANGLE = new KeyRectangle();
	private class KeyRectangle extends EDIFKEY
	{
		private KeyRectangle() { super("rectangle"); }
		protected void func()
			throws IOException
		{
			io_edfreeptlist();
			orientation = OR0;
		}
	}

	private EDIFKEY KRENAME = new KeyRename();
	private class KeyRename extends EDIFKEY
	{
		private KeyRename() { super("rename"); }
		protected void func()
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
		}
	}

	private EDIFKEY KSCALE = new EDIFKEY("scale");

	private EDIFKEY KSCALEX = new EDIFKEY("scaleX");

	private EDIFKEY KSCALEY = new EDIFKEY("scaleY");

	private EDIFKEY KSHAPE = new EDIFKEY("shape");

	private EDIFKEY KSTATUS = new EDIFKEY("status");

	private EDIFKEY KSTRING = new KeyString();
	private class KeyString extends EDIFKEY
	{
		private KeyString() { super("string"); }
		protected void func()
			throws IOException
		{
			io_edpos_token();
			char chr = buffer.charAt(pos);

			if (chr != '(' && chr != ')')
			{
				String value = io_edget_token((char)0);
				if (value == null) throw new IOException("Unexpected end-of-file");

				pval = value.substring(1, value.length()-1);
			}
		}
	}

	private EDIFKEY KSTRINGDISPLAY = new KeyStringDisplay();
	private class KeyStringDisplay extends EDIFKEY
	{
		private KeyStringDisplay() { super("stringDisplay"); }
		protected void func()
			throws IOException
		{
			// init the point lists
			io_edfreeptlist();
			orientation = OR0;
			visible = true;
			justification = TextDescriptor.Position.DOWNRIGHT;
			textheight = 0;

			// get the string, remove the quote
			io_edget_delim('\"');
			String string = io_edget_token('\"');
			if (string == null) throw new IOException("Unexpected end-of-file");

			// check for RENAME
			if (kstack[kstack_ptr-1] != KRENAME) return;
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
		}
	}

	private EDIFKEY KSYMBOL = new KeySymbol();
	private class KeySymbol extends EDIFKEY
	{
		private KeySymbol() { super("symbol"); }
		protected void func()
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
				builtCells.add(proto);
			} else
			{
				if (!builtCells.contains(proto)) ignoreblock = true;
			}

			current_cell = proto;
			figure_group = null;
		}
	}

	private EDIFKEY KTECHNOLOGY = new EDIFKEY("technology");

	private EDIFKEY KTEXTHEIGHT = new KeyTextHeight();
	private class KeyTextHeight extends EDIFKEY
	{
		private KeyTextHeight() { super("textHeight"); }
		protected void func()
			throws IOException
		{
			// get the textheight value of the point
			String val = io_edget_token((char)0);
			textheight = (int)(TextUtils.atoi(val) * scale);

			if (cur_nametbl != null)
				cur_nametbl.textheight = textheight;
		}
	}

	private EDIFKEY KTIMESTAMP = new EDIFKEY("timestamp");

	private EDIFKEY KTRANSFORM = new EDIFKEY("transform");

	private EDIFKEY KTRUE = new KeyTrue();
	private class KeyTrue extends EDIFKEY
	{
		private KeyTrue() { super("true"); }
		protected void func()
			throws IOException
		{
			// check previous keyword
			if (kstack_ptr > 1 && kstack[kstack_ptr-1] == KVISIBLE)
			{
				visible = true;
				if (cur_nametbl != null)
					cur_nametbl.visible = true;
			}
		}
	}

	private EDIFKEY KUNIT = new KeyUnit();
	private class KeyUnit extends EDIFKEY
	{
		private KeyUnit() { super("unit"); }
		protected void func()
			throws IOException
		{
			String type = io_edget_token((char)0);
			if (kstack[kstack_ptr-1] == KSCALE && type.equalsIgnoreCase("DISTANCE"))
			{
				// just make the scale be so that the specified number of database units becomes 1 lambda
				scale = 1;
				scale *= IOTool.getEDIFInputScale();
			}
		}
	}

	private EDIFKEY KUSERDATA = new EDIFKEY("userData");

	private EDIFKEY KVERSION = new EDIFKEY("version");

	private EDIFKEY KVIEW = new KeyView();
	private class KeyView extends EDIFKEY
	{
		private KeyView() { super("view"); }
		protected void func()
			throws IOException
		{
			active_view = VNULL;
		}
	}

	private EDIFKEY KVIEWREF = new EDIFKEY("viewRef");

	/**
	 * viewType:  Indicates the view style for this cell, ie
	 *	BEHAVIOR, DOCUMENT, GRAPHIC, LOGICMODEL, MASKLAYOUT, NETLIST,
	 *  	PCBLAYOUT, SCHEMATIC, STRANGER, SYMBOLIC
	 * we are only concerned about the viewType NETLIST.
	 */
	private EDIFKEY KVIEWTYPE = new KeyViewType();
	private class KeyViewType extends EDIFKEY
	{
		private KeyViewType() { super("viewType"); }
		protected void func()
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
					builtCells.add(proto);
				} else
				{
					if (!builtCells.contains(proto)) ignoreblock = true;
				}

				current_cell = proto;
			}
			else current_cell = null;

			// add the name to the celltbl
			NAMETABLE nt = new NAMETABLE();
			nt.original = cell_reference;
			nt.replace = cell_name;
			celltbl.put(cell_reference, nt);
		}
	}

	private EDIFKEY KVISIBLE = new EDIFKEY("visible");

	private EDIFKEY KWRITTEN = new EDIFKEY("written");
}
