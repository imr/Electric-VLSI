/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CIF.java
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
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.CellName;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.text.Version;
import com.sun.electric.database.text.Pref;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.lib.LibFile;
import com.sun.electric.technology.PrimitiveArc;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.PrimitivePort;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Tool;
import com.sun.electric.tool.io.ELIBConstants;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.user.dialogs.OpenFile;
import com.sun.electric.tool.user.ErrorLogger;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import java.io.LineNumberReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Iterator;

/**
 * This class reads files in CIF files.
 */
public class CIF extends Input
{
	static class CIFCELL
	{
		/** cell index given in the define statement */	int  cindex;
		/** bounding box of cell */						int  l, r, t, b;
		/** the address of the cif cell */				Cell addr;
	};

	// values for CIFLIST->identity
	private static final int C_START =   0;
	private static final int C_END =     1;
	private static final int C_WIRE =    2;
	private static final int C_FLASH =   3;
	private static final int C_BOX =     4;
	private static final int C_POLY =    5;
	private static final int C_COMMAND = 6;
	private static final int C_GNAME =   7;
	private static final int C_LABEL =   8;
	private static final int C_CALL =    9;

	static class CIFLIST
	{
		/** specifies the nature of the entry */	int     identity;
		/** will point to member's structure */		Object  member;
		/** next entry in list */					CIFLIST next;
	};

	static class CFSTART
	{
		/** cell index */							int    cindex;
		/** cell name */							String name;
		/** bounding box of cell */					int    l, r, t, b;
	};

	static class CBOX
	{
		/** the corresponding layer number */		Layer lay;
		/** dimensions of box */					int   length, width;
		/** center point of box */					int   cenx, ceny;
		/** box direction */						int   xrot, yrot;
	};

	static class CPOLY
	{
		/** the corresponding layer number */		Layer  lay;
		/** list of points */						int [] x, y;
		/** number of points in list */				int    lim;
	};

	static class CGNAME
	{
		/** the corresponding layer number */		Layer  lay;
		/** location of name */						int    x, y;
		/** the geo name */							String geoname;
	};

	static class CLABEL
	{
		/** location of label */					int    x, y;
		/** the label */							String label;
	};

	static class CCALL
	{
		/** index of cell called */					int    cindex;
		/** name of cell called */					String name;
		/** list of transformations */				CTRANS list;
	};

	// values for the transformation type
	/** mirror in x */	private static final int MIRX  = 1;
	/** mirror in y */	private static final int MIRY  = 2;
	/** translation */	private static final int TRANS = 3;
	/** rotation */		private static final int ROT   = 4;

	static class CTRANS
	{
		/** type of transformation */				int    type;
		/** not required for the mirror types */	int    x, y;
		/** next element in list */					CTRANS next;
	};

	private CTRANS io_curctrans;				/** current transformation description */

	//	specific syntax errors
	private static final int NOERROR    = 100;
	private static final int NUMTOOBIG  = 101;
	private static final int NOUNSIGNED = 102;
	private static final int NOSIGNED   = 103;
	private static final int NOSEMI     = 104;
	private static final int NOPATH     = 105;
	private static final int BADTRANS   = 106;
	private static final int BADUSER    = 107;
	private static final int BADCOMMAND = 108;
	private static final int INTERNAL   = 109;
	private static final int BADDEF     = 110;
	private static final int NOLAYER    = 111;
	private static final int BADCOMMENT = 112;
	private static final int BADAXIS    = 113;
	private static final int NESTDEF    = 114;
	private static final int NESTDD     = 115;
	private static final int NODEFSTART = 116;
	private static final int NESTEND    = 117;
	private static final int NOSPACE    = 118;
	private static final int NONAME     = 119;

	// enumerated types for cif 2.0 parser
	private static final int SEMANTICERROR = 0;
	private static final int SYNTAXERROR   = 1;
	private static final int WIRECOM       = 2;
	private static final int BOXCOM        = 3;
	private static final int POLYCOM       = 4;
	private static final int FLASHCOM      = 5;
	private static final int DEFSTART      = 6;
	private static final int DEFEND        = 7;
	private static final int DELETEDEF     = 8;
	private static final int LAYER         = 9;
	private static final int CALLCOM       = 10;
	private static final int COMMENT       = 11;
	private static final int NULLCOMMAND   = 12;
	private static final int USERS         = 13;
	private static final int END           = 14;
	private static final int ENDFILE       = 15;
	private static final int SYMNAME       = 16;
	private static final int INSTNAME      = 17;
	private static final int GEONAME       = 18;
	private static final int LABELCOM      = 19;

	// types for tlists
	static class ttype {}
	private ttype MIRROR = new ttype();
	private ttype TRANSLATE = new ttype();
	private ttype ROTATE = new ttype();

	static class tentry
	{
		ttype kind;
		boolean xcoord;
		int xt, yt;
		int xrot, yrot;
	};

	// error codes for reporting errors
	private static final int FATALINTERNAL = 0;
	private static final int FATALSYNTAX   = 1;
	private static final int FATALSEMANTIC = 2;
	private static final int FATALOUTPUT   = 3;
	private static final int ADVISORY      = 4;
	private static final int OTHER         = 5;			/* OTHER must be last */

	// structures for the interpreter
	private static final int REL       = 0;
	private static final int NOREL     = 1;
	private static final int SAME      = 2;
	private static final int DONTCARE  = 3;

	private static final int TIDENT     = 0;
	private static final int TROTATE    = 1;
	private static final int TTRANSLATE = 2;
	private static final int TMIRROR    = 4;

	/** data types for transformation package */
	static class tmatrix
	{
		double a11, a12, a21, a22, a31, a32, a33;
		tmatrix prev, next;
		int type;
		boolean multiplied;
	};

	/** bounding box */
	static class bbrecord
	{
		int l,r,b,t;
	};

	static class stentry
	{
		/** symbol number for this entry */					int st_symnumber;
		boolean st_expanded, st_frozen, st_defined, st_dumped;
		/** bb as if this symbol were called by itself */	bbrecord st_bb;
		boolean st_bbvalid;
		String st_name;
		/** number of calls made by this symbol */			int st_ncalls;
		/** pointer to linked list of objects */			CifBase st_guts;
		stentry()
		{
			st_bb = new bbrecord();
		}
	};

	static class linkedpoint
	{
		Point pvalue;
		linkedpoint pnext;
	};

	static class path
	{
		linkedpoint pfirst,plast;
		int plength;
	};

	static class linkedtentry
	{
		tentry tvalue;
		linkedtentry tnext;
	};

	static class tlist
	{
		linkedtentry tfirst,tlast;
		int tlength;
	};

	/** items in item tree */
	static class itementry
	{
		/** links for tree */					itementry it_rel;
		/** links for tree */					itementry it_norel;
		/** links for tree */					itementry it_same;
		/** pointer into symbol structure */	CifBase   it_what;
		/** level of nest, from root (= 0) */	int       it_level;
		/** what trans context to use */		int       it_context;
		/** bb on chip */						int       it_left;
		/** bb on chip */						int       it_right;
		/** bb on chip */						int       it_bottom;
		/** bb on chip */						int       it_top;
	};

	static class context
	{
		/** bottom of stack */		tmatrix base;
		/** not inuse if = 0 */		int     refcount;
		/** current top */			tmatrix ctop;
	};

	/** hack structure for referencing first fields of any object */
	static class CifBase
	{
		/** bounding box */				bbrecord bb;
		/** for ll */					CifBase  next;
		/** layer for this object */	Layer layer;
		CifBase()
		{
			bb = new bbrecord();
		}
	};

	/** symbol call object */
	static class symcall extends CifBase
	{
		/** rest is noncritical */				int sy_symnumber;
		stentry sy_unid;
		String sy_name;
		tmatrix sy_tm;
		/** trans list for this call */			tlist sy_tlist;
	};

	static class gname extends CifBase
	{
		String gn_name;
		Point gn_pos;
	};

	static class label extends CifBase
	{
		String la_name;
		Point la_pos;
	};

	static class box extends CifBase
	{
		/** then layer */			Layer bo_layer;
		int bo_length, bo_width;
		Point bo_center;
		int bo_xrot, bo_yrot;
	};

	static class mbox extends CifBase
	{
	};

	static class flash extends CifBase
	{
		Point fl_center;
		int fl_diameter;
	};

	static class polygon extends CifBase
	{
		/** array of points in path */			Point [] po_p;
	};

	static class wire extends CifBase
	{
		int wi_width;
		/** length of path, points follow */	int wi_numpts;
		/** array of points in path */			Point [] wi_p;
	};

	/** initial number of contexts */		private static final int CONTSIZE = 50;
	/** max depth of minmax stack */		private static final int MAXMMSTACK = 50;

	/** head of the list */					private CIFLIST io_ciflist = null;
	/** current location in list */			private CIFLIST io_curlist;
	/** pointer to the first context */		private context  [] io_cifcarray;
	/** head of item list */				private itementry io_cifilist;
	/** A/B from DS */						private double io_cifscalefactor;
	/** current symbol being defined */		private stentry io_cifcurrent;
	/** place to save layer during def */	private Layer io_cifbackuplayer;
	/** inst count for default names */		private int io_cifbinst;
	/** symbol has been named */			private boolean   io_cifnamed;
	/** flag for error encountered */		private boolean io_ciferrorfound;
	/** what it was */						private int io_ciferrortype;
	/** definition in progress flag */		private boolean   io_cifdefinprog;
	/** end command flag */					private boolean   io_cifendisseen;
	/** line count */						private int  io_ciflinecount;
	/** number of chars in buffer */		private int  io_cifcharcount;
	/** flag to reset buffer */				private boolean    io_cifresetbuffer;
											private int [] io_cifecounts;
	/** null layer errors encountered */	private int io_nulllayererrors;
	/** ignore statements until DF */		private boolean io_cifignore;
	/** 91 pending */						private boolean   io_cifnamepending;
	/** end command flag */					private boolean   io_cifendcom;
	/** current layer */					private Layer io_cifcurlayer;
	/** inst count for default names */		private int io_cifinstance;
	/** stack pointer */					private int io_cifmmptr;
	/** symbol table */						private HashMap io_cifstable;
	/** the top of stack */					private tmatrix io_cifstacktop;
	/** lookahead character */				private int  io_cifnextchar;
	/** # statements since 91 com */		private int io_cifstatesince;
	/** name saved from 91 */				private String  io_cifnstored;
	private int [] io_cifmmleft;
	private int [] io_cifmmright;
	private int [] io_cifmmbottom;
	private int [] io_cifmmtop;
	/** current context */					private int    io_cifcurcontext;

	private HashMap cifCellMap;
	/** the current cell */					private CIFCELL  io_curcell;
	private Technology curTech;
	private HashMap cifLayerNames;
	/** address of cell being defined */	private Cell  io_incell;
	/** name of the current cell */			private String io_curnodeprotoname;

	private StringBuffer io_ciflinebuffer;

	/**
	 * Method to import a library from disk.
	 * @param lib the library to fill
	 * @return true on error.
	 */
	protected boolean importALibrary(Library lib)
	{
		// initialize all lists and the searching routines
		cifCellMap = new HashMap();

		if (io_initfind()) return true;

		// parse the cif and create a listing
		if (io_interpret()) return true;

		// instantiate the cif as nodes
		if (io_listtonodes(lib)) return true;

		// clean up
		io_doneinterpreter();

		return false;
	}

	private boolean io_listtonodes(Library lib)
	{
		io_incell = null;
		for(io_curlist = io_ciflist; io_curlist != null; io_curlist = io_curlist.next)
		{
			if (io_incell != null || io_curlist.identity == C_START)
				switch (io_curlist.identity)
			{
				case C_START:
					io_incell = io_nodes_start(lib);
					if (io_incell == null) return true;
					break;
				case C_END:
					lib.setCurCell(io_incell);
					io_incell = null;
					break;
				case C_BOX:
					if (io_nodes_box()) return true;
					break;
				case C_POLY:
					if (io_nodes_poly()) return true;
					break;
				case C_CALL:
					if (io_nodes_call()) return true;
					break;
			}
		}
		return false;
	}

	private boolean io_nodes_call()
	{
		CCALL cc = (CCALL)io_curlist.member;
		CIFCELL cell = io_findcifcell(cc.cindex);
		if (cell == null)
		{
			System.out.println("Referencing an undefined cell");
			return true;
		}
		int rot = 0;
		boolean trans = false;
		int l = cell.l;    int r = cell.r;    int b = cell.b;    int t = cell.t;
		for(CTRANS ctrans = cc.list; ctrans != null; ctrans = ctrans.next)
			switch (ctrans.type)
		{
			case MIRX:
				int temp = l;   l = -r;   r = -temp;
				rot = (trans) ? ((rot+2700) % 3600) : ((rot+900) % 3600);
				trans = !trans;
				break;
			case MIRY:
				temp = t;   t = -b;   b = -temp;
				rot = (trans) ? ((rot+900) % 3600) : ((rot+2700) % 3600);
				trans = !trans;
				break;
			case TRANS:
				l += ctrans.x;   r += ctrans.x;
				b += ctrans.y;   t += ctrans.y;
				break;
			case ROT:
				int deg = GenMath.figureAngle(new Point2D.Double(0, 0), new Point2D.Double(ctrans.x, ctrans.y));
				if (deg != 0)
				{
					int hlen = Math.abs(((l-r)/2));   int hwid = Math.abs(((b-t)/2));
					int cenx = (l+r)/2;   int ceny = (b+t)/2;
					Point pt = new Point(cenx, ceny);
					io_rotatelayer(pt, deg);
					cenx = pt.x;   ceny = pt.y;
					l = cenx - hlen;   r = cenx + hlen;
					b = ceny - hwid;   t = ceny + hwid;
					rot += ((trans) ? -deg : deg);
				}
		}
		// TODO: should account for mirroring (have trans factor, use it!)
		while (rot >= 3600) rot -= 3600;
		while (rot < 0) rot += 3600;
		double x = TextUtils.convertFromDistance((l + r) / 2, curTech, TextUtils.UnitScale.MICRO) / 100;
		double y = TextUtils.convertFromDistance((b + t) / 2, curTech, TextUtils.UnitScale.MICRO) / 100;
		double sX = TextUtils.convertFromDistance(r-l, curTech, TextUtils.UnitScale.MICRO) / 100;
		double sY = TextUtils.convertFromDistance(t-b, curTech, TextUtils.UnitScale.MICRO) / 100;
		NodeInst ni = NodeInst.makeInstance(cell.addr, new Point2D.Double(x, y), sX, sY, io_curcell.addr, rot, null, 0);
		if (ni == null)
		{
			System.out.println("Problems creating an instance of cell " + cell.addr.describe() + " in cell " + io_curcell.addr.describe());
			return true;
		}
		return false;
	}

	private boolean io_nodes_poly()
	{
		CPOLY cp = (CPOLY)io_curlist.member;
		if (cp.lim == 0) return false;
		NodeProto np = io_findprotonode(cp.lay);
		int lx = cp.x[0];
		int hx = cp.x[0];
		int ly = cp.y[0];
		int hy = cp.y[0];
		for(int i=1; i<cp.lim; i++)
		{
			if (cp.x[i] < lx) lx = cp.x[i];
			if (cp.x[i] > hx) hx = cp.x[i];
			if (cp.y[i] < ly) ly = cp.y[i];
			if (cp.y[i] > hy) hy = cp.y[i];
		}
		double x = TextUtils.convertFromDistance((lx + hx) / 2, curTech, TextUtils.UnitScale.MICRO) / 100;
		double y = TextUtils.convertFromDistance((ly + hy) / 2, curTech, TextUtils.UnitScale.MICRO) / 100;
		double sX = TextUtils.convertFromDistance(hx - lx, curTech, TextUtils.UnitScale.MICRO) / 100;
		double sY = TextUtils.convertFromDistance(hy - ly, curTech, TextUtils.UnitScale.MICRO) / 100;
		NodeInst newni = NodeInst.makeInstance(np, new Point2D.Double(x, y), sX, sY, io_curcell.addr);
		if (newni == null)
		{
			System.out.println("Problems creating a polygon on layer " + cp.lay + " in cell " + io_curcell.addr.describe());
			return true;
		}

		// store the trace information
//		pt = trace = emalloc((cp.lim*2*SIZEOFINTBIG), io_tool.cluster);
//		if (trace == 0) return true;
//		cx = (hx + lx) / 2;   cy = (hy + ly) / 2;
//		for(i=0; i<cp.lim; i++)
//		{
//			*pt++ = scalefromdispunit((float)(cp.x[i] - cx), DISPUNITCMIC);
//			*pt++ = scalefromdispunit((float)(cp.y[i] - cy), DISPUNITCMIC);
//		}
//
//		// store the trace information
//		(void)setvalkey((INTBIG)newni, VNODEINST, el_trace_key, (INTBIG)trace,
//			VINTEGER|VISARRAY|((cp.lim*2)<<VLENGTHSH));

		return false;
	}

	private Cell io_nodes_start(Library lib)
	{
		CFSTART cs = (CFSTART)io_curlist.member;
		CIFCELL cifcell = io_newcifcell(cs.cindex);
		cifcell.l = cs.l;   cifcell.r = cs.r;
		cifcell.b = cs.b;   cifcell.t = cs.t;
		io_curnodeprotoname = cs.name;

		// remove illegal characters
//		for(opt = io_curnodeprotoname; *opt != 0; opt++)
//			if (*opt <= ' ' || *opt == ':' || *opt == ';' || *opt >= 0177)
//				*opt = 'X';
		cifcell.addr = Cell.newInstance(lib, io_curnodeprotoname);
		if (cifcell.addr == null)
		{
			System.out.println("Cannot create the cell " + io_curnodeprotoname);
			return null;
		}
		return cifcell.addr;
	}

	private void io_rotatelayer(Point pt, int deg)
	{
		// trivial test to prevent atan2 domain errors
		if (pt.x == 0 && pt.y == 0) return;
		switch (deg)	// do the manhattan cases directly
		{
			case 0:
			case 3600:	// just in case
				break;
			case 900:
				int temp = pt.x;   pt.x = -pt.y;   pt.y = temp;
				break;
			case 1800:
				pt.x = -pt.x;   pt.y = -pt.y;
				break;
			case 2700:
				temp = pt.x;   pt.x = pt.y;   pt.y = -temp;
				break;
			default: // this old code only permits rotation by integer angles
				double factx = 1, facty = 1;
				while(Math.abs(pt.x/factx) > 1000) factx *= 10.0;
				while(Math.abs(pt.y/facty) > 1000) facty *= 10.0;
				double fact = (factx > facty) ? facty : factx;
				double fx = pt.x / fact;		  double fy = pt.y / fact;
				double vlen = fact * Math.sqrt(fx*fx + fy*fy);
				double vang = (deg + GenMath.figureAngle(new Point2D.Double(0, 0), new Point2D.Double(pt.x, pt.y))) / 10.0 / (45.0 / Math.atan(1.0));
				pt.x = (int)(vlen * Math.cos(vang));
				pt.y = (int)(vlen * Math.sin(vang));
				break;
		}
	}

	private void io_outputpolygon(Layer lay, path ppath)
	{
		int lim = io_pathlength(ppath);
		if (lim < 3) return;

		io_placeciflist(C_POLY);
		CPOLY cp = (CPOLY)io_curlist.member;
		cp.lay = lay;
		cp.x = new int[lim];
		cp.y = new int[lim];

		cp.lim = lim;
		for (int i = 0; i < lim; i++)
		{
			Point temp = io_removepoint(ppath);
			cp.x[i] = temp.x;
			cp.y[i] = temp.y;
		}
	}

	private boolean io_nodes_box()
	{
		CBOX cb = (CBOX)io_curlist.member;
		NodeProto node = io_findprotonode(cb.lay);
		if (node == null)
		{
			String layname = cb.lay.getName();
			System.out.println("Cannot find primitive to use for layer '" + layname + "' (number " + cb.lay + ")");
			return true;
		}
		int l = cb.length;        int w = cb.width;
		int lx = cb.cenx - l/2;   int ly = cb.ceny - w/2;
		int hx = cb.cenx + l/2;   int hy = cb.ceny + w/2;
		int r = GenMath.figureAngle(new Point2D.Double(0, 0), new Point2D.Double(cb.xrot, cb.yrot));
		double x = TextUtils.convertFromDistance(cb.cenx, curTech, TextUtils.UnitScale.MICRO) / 100;
		double y = TextUtils.convertFromDistance(cb.ceny, curTech, TextUtils.UnitScale.MICRO) / 100;
		NodeInst ni = NodeInst.makeInstance(node, new Point2D.Double(x, y), cb.length, cb.width, io_curcell.addr, r, null, 0);
		if (ni == null)
		{
			String layname = cb.lay.getName();
			System.out.println("Problems creating a box on layer " + layname + " in cell " + io_curcell.addr.describe());
			return true;
		}
		return false;
	}

	private boolean io_interpret()
	{
		io_initparser();
		io_initinterpreter();
		io_infromfile();
		int comcount = io_parsefile();		// read in the cif
		io_doneparser();

		if (io_fatalerrors() > 0) return true;

		Rectangle box = io_iboundbox();

		// construct a list: first step in the conversion
		io_createlist();
		return false;
	}

	private CIFCELL io_findcifcell(int cindex)
	{
		return (CIFCELL)cifCellMap.get(new Integer(cindex));
	}

	private CIFCELL io_newcifcell(int cindex)
	{
		CIFCELL newcc = new CIFCELL();
		newcc.addr = null;
		newcc.cindex = cindex;
		cifCellMap.put(new Integer(newcc.cindex), newcc);

		io_curcell = newcc;
		return newcc;
	}

	private NodeProto io_findprotonode(Layer lay)
	{
		return lay.getPureLayerNode();
	}

	private boolean io_initfind()
	{
		// get the array of CIF names
		cifLayerNames = new HashMap();
		boolean valid = false;
		curTech = Technology.getCurrent();
		for(Iterator it = curTech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			String cifName = layer.getCIFLayer();
			if (cifName != null && cifName.length() > 0)
			{
				cifLayerNames.put(cifName, layer);
				valid = true;
			}
		}
		if (!valid)
		{
			System.out.println("There are no CIF layer names assigned in the " + curTech.getTechName() + " technology");
			return true;
		}

		return false;
	}

	private void io_initinterpreter()
	{
		io_nulllayererrors = 0;
		io_cifdefinprog = false;
		io_cifignore = false;
		io_cifnamepending = false;
		io_cifendcom = false;
		io_cifcurlayer = null;
		io_cifinstance = 1;
		io_cifilist = null;
		io_initutil();
		io_inittrans();
	}

	private void io_initparser()
	{
		io_ciferrorfound = false;
		io_ciferrortype = NOERROR;
		io_cifdefinprog = false;
		io_cifendisseen = false;
		io_initinput();
		io_initerror();
	}

	private void io_doneparser()
	{
		if (!io_cifendisseen) io_report("missing End command", FATALSYNTAX);
	}

	private int io_parsefile()
	{
		int comcount = 1;
		for(;;)
		{
			int com = io_parsestatement();
			if (com == END || com == ENDFILE) break;
			comcount++;
		}
		return comcount;
	}

	private int io_fatalerrors()
	{
		return(io_cifecounts[FATALINTERNAL]+io_cifecounts[FATALSYNTAX]+
			io_cifecounts[FATALSEMANTIC]+io_cifecounts[FATALOUTPUT]);
	}

	private void io_doneinterpreter()
	{
		if (io_nulllayererrors != 0)
		{
			io_report("output on null layer", FATALSEMANTIC);
		}
	}

	private Rectangle io_iboundbox()
	{
		itementry h = io_cifilist;
		boolean first = true;

		if (h == null)
		{
			io_report("item list is empty!", ADVISORY);
			return null;
		}

		io_swapcontext(0);
		io_pushtrans();
		while (h != null)
		{
			CifBase obj = h.it_what;
			Point temp = new Point();
			temp.x = obj.bb.l;
			temp.y = obj.bb.b;
			Point comperror = io_transpoint(temp);
			io_initmm(comperror);
			temp.x = obj.bb.r;
			comperror = io_transpoint(temp);
			io_minmax(comperror);
			temp.y = obj.bb.t;
			comperror = io_transpoint(temp);
			io_minmax(comperror);
			temp.x = obj.bb.l;
			comperror = io_transpoint(temp);
			io_minmax(comperror);

			h.it_left = io_minx();
			h.it_right = io_maxx();
			h.it_bottom = io_miny();
			h.it_top = io_maxy();
			io_donemm();
			temp.x = h.it_left;   temp.y = h.it_bottom;
			if (first) {first = false; io_initmm(temp);} else io_minmax(temp);
			temp.x = h.it_right;   temp.y = h.it_top;
			io_minmax(temp);
			h = h.it_same;
		}
		Rectangle ret = new Rectangle(io_minx(), io_miny(), io_maxx()-io_minx(), io_maxy()-io_miny());
		io_donemm();
		io_poptrans();
		io_swapcontext(0);		// always leave in context 0
		return ret;
	}

	private void io_createlist()
	{
		if (!io_endseen()) System.out.println("missing End command, assumed");
		if (io_fatalerrors() > 0) return;
		io_sendlist(io_cifilist);		// sendlist deletes nodes
		io_cifilist = null;
		io_swapcontext(0);
	}

	private void io_sendlist(itementry list)
	{
		itementry h = list;
		while (h != null)
		{
			itementry save = h.it_same;
			io_outitem(h);
			h = save;
		}
	}

	/**
	 * spit out an item
	 */
	private void io_outitem(itementry thing)
	{
		if (thing.it_what instanceof polygon)
		{
			io_swapcontext(thing.it_context);
			polygon po = (polygon)thing.it_what;
			path ppath = io_makepath();
			for (int i = 0; i < po.po_p.length; i++)
				if (io_appendpoint(ppath, po.po_p[i])) return;
			io_outputpolygon(po.layer, ppath);
			io_decrefcount(thing.it_context);
			return;
		}
		if (thing.it_what instanceof wire)
		{
			io_swapcontext(thing.it_context);
			wire wi = (wire)thing.it_what;
			int length = wi.wi_numpts;
			path ppath = io_makepath();
			for (int i = 0; i < length; i++)
				if (io_appendpoint(ppath, wi.wi_p[i])) return;
			io_outputwire(wi.layer, wi.wi_width, ppath);
			io_decrefcount(thing.it_context);
			return;
		}
		if (thing.it_what instanceof flash)
		{
			io_swapcontext(thing.it_context);
			flash fl = (flash)thing.it_what;
			io_outputflash(fl.layer, fl.fl_diameter, fl.fl_center);
			io_decrefcount(thing.it_context);
			return;
		}
		if (thing.it_what instanceof box)
		{
			io_swapcontext(thing.it_context); // get right frame of ref
			box bo = (box)thing.it_what;
			io_outputbox(bo.layer, bo.bo_length, bo.bo_width, bo.bo_center, bo.bo_xrot, bo.bo_yrot);
			io_decrefcount(thing.it_context);
			return;
		}
		if (thing.it_what instanceof mbox)
		{
			io_swapcontext(thing.it_context); // get right frame of ref
			Point temp = new Point();
			mbox mb = (mbox)thing.it_what;
			temp.x = (mb.bb.r + mb.bb.l)/2;
			temp.y = (mb.bb.t + mb.bb.b)/2;
			io_outputbox(mb.layer, mb.bb.r-mb.bb.l, mb.bb.t-mb.bb.b, temp, 1, 0);
			io_decrefcount(thing.it_context);
			return;
		}
		if (thing.it_what instanceof symcall)
		{
			io_swapcontext(thing.it_context);
			io_pushtrans();
			symcall sc = (symcall)thing.it_what;
			io_applylocal(sc.sy_tm);
			tlist t_list = io_maketlist();
			io_dupetlist((tlist)sc.sy_tlist, (tlist)t_list);
			io_dumpdef(sc.sy_unid);
			io_outputcall(sc.sy_symnumber, sc.sy_unid.st_name, t_list);
			io_swapcontext(thing.it_context);
			io_poptrans();
			io_decrefcount(thing.it_context);
			return;
		}
		if (thing.it_what instanceof gname)
		{
			io_swapcontext(thing.it_context);
			gname gn = (gname)thing.it_what;
			io_outputgeoname(gn.gn_name, gn.gn_pos, gn.layer);
			io_decrefcount(thing.it_context);
			return;
		}
		if (thing.it_what instanceof label)
		{
			io_swapcontext(thing.it_context);
			label la = (label)thing.it_what;
			io_outputlabel(la.la_name, la.la_pos);
			io_decrefcount(thing.it_context);
			return;
		}
	}

	private void io_outputlabel(String name, Point pt)
	{
		io_placeciflist(C_LABEL);
		CLABEL cl = (CLABEL)io_curlist.member;
		cl.label = name;
		cl.x = pt.x;   cl.y = pt.y;
	}

	private void io_outputgeoname(String name, Point pt, Layer lay)
	{
		io_placeciflist(C_GNAME);
		CGNAME cg = (CGNAME)io_curlist.member;
		cg.lay = lay;
		cg.geoname = name;
		cg.x = pt.x;   cg.y = pt.y;
	}

	private void io_outputcall(int number, String name, tlist list)
	{
		io_placeciflist(C_CALL);
		CCALL cc = (CCALL)io_curlist.member;
		cc.cindex = number;
		cc.name = name;
		cc.list = io_curctrans = null;
		for(int i = io_tlistlength(list); i>0; i--)
		{
			if (io_newctrans() == null) return;
			tentry temp = io_removetentry((tlist)list);
			if (temp.kind == MIRROR)
			{
				if (temp.xcoord) io_curctrans.type = MIRX; else
					io_curctrans.type = MIRY;
			} else if (temp.kind == TRANSLATE)
			{
				io_curctrans.type = TRANS;
				io_curctrans.x = temp.xt;
				io_curctrans.y = temp.yt;
			} else if (temp.kind == ROTATE)
			{
				io_curctrans.type = ROT;
				io_curctrans.x = temp.xrot;
				io_curctrans.y = temp.yrot;
			}
		}
	}

	private CTRANS io_newctrans()
	{
		CTRANS newct = new CTRANS();
		newct.next = null;
		CCALL cc = (CCALL)io_curlist.member;
		if (cc.list == null) cc.list = newct; else
			io_curctrans.next = newct;
		io_curctrans = newct;
		return newct;
	}

	private void io_dumpdef(stentry sym)
	{
		if (sym.st_dumped) return;		// already done
		if (sym.st_ncalls > 0)			// dump all children
		{
			int count = sym.st_ncalls;

			CifBase ro = sym.st_guts;
			while (ro != null && count > 0)
			{
				if (ro instanceof symcall)
				{
					io_dumpdef(((symcall)ro).sy_unid);
					count--;
				}
				ro = ro.next;
			}
		}
		io_shipcontents(sym);
		sym.st_dumped = true;
	}

	private void io_shipcontents(stentry sym)
	{
		CifBase ro = sym.st_guts;
		io_outputds(sym.st_symnumber, sym.st_name, sym.st_bb.l, sym.st_bb.r, sym.st_bb.b, sym.st_bb.t);
		while (ro != null)
		{
			if (ro instanceof polygon)
			{
				polygon po = (polygon)ro;
				path ppath = io_makepath();
				for (int i = 0; i < po.po_p.length; i++)
					if (io_appendpoint(ppath, po.po_p[i])) return;
				io_outputpolygon(po.layer, ppath);
			} else if (ro instanceof wire)
			{
				wire wi = (wire)ro;
				int length = wi.wi_numpts;
				path ppath = io_makepath();
				for (int i = 0; i < length; i++)
					if (io_appendpoint(ppath, wi.wi_p[i])) return;
				io_outputwire(wi.layer, wi.wi_width, ppath);
			} else if (ro instanceof flash)
			{
				flash fl = (flash)ro;
				io_outputflash(fl.layer, fl.fl_diameter, fl.fl_center);
			} else if (ro instanceof box)
			{
				box bo = (box)ro;
				io_outputbox(bo.layer, bo.bo_length, bo.bo_width, bo.bo_center, bo.bo_xrot, bo.bo_yrot);
			} else if (ro instanceof mbox)
			{
				mbox mb = (mbox)ro;
				Point temp = new Point();
				temp.x = (((mbox)ro).bb.r + ((mbox) ro).bb.l)/2;
				temp.y = (((mbox)ro).bb.t + ((mbox) ro).bb.b)/2;
				io_outputbox(mb.layer, mb.bb.r-mb.bb.l, mb.bb.t-mb.bb.b, temp, 1, 0);
			} else if (ro instanceof symcall)
			{
				symcall sc = (symcall)ro;
				tlist t_list = io_maketlist();
				io_dupetlist((tlist)sc.sy_tlist, (tlist)t_list);
				io_outputcall(sc.sy_symnumber, sc.sy_unid.st_name, t_list);
			} else if (ro instanceof gname)
			{
				gname gn = (gname)ro;
				io_outputgeoname(gn.gn_name, gn.gn_pos, gn.layer);
			} else if (ro instanceof label)
			{
				label la = (label)ro;
				io_outputlabel(la.la_name, la.la_pos);
			}
			ro = ro.next;
		}
		io_outputdf();
	}

	private void io_outputdf()
	{
		io_placeciflist(C_END);
	}

	private void io_outputds(int number, String name, int l, int r, int b, int t)
	{
		io_placeciflist(C_START);
		CFSTART cs = (CFSTART)io_curlist.member;
		cs.cindex = number;
		cs.name = name;
		cs.l = l;   cs.r = r;
		cs.b = b;   cs.t = t;
	}

	private void io_dupetlist(tlist src, tlist dest)
	{
		if (src == null || dest == null) return;
		linkedtentry node = src.tfirst;
		while (node != null)
		{
			io_appendtentry(dest, node.tvalue);
			node = node.tnext;
		}
	}

	private void io_outputbox(Layer lay, int length, int width, Point center, int xrotation, int yrotation)
	{
		if (length == 0 && width == 0) return;	// ignore null boxes
		io_placeciflist(C_BOX);
		CBOX cb = (CBOX)io_curlist.member;
		cb.lay = lay;
		cb.length = length;	cb.width = width;
		cb.cenx = center.x;	cb.ceny = center.y;
		cb.xrot = xrotation;	cb.yrot = yrotation;
	}

	private void io_placeciflist(int id)
	{
		CIFLIST cl = io_newciflist(id);

		if (cl == null) return;
		if (io_ciflist == null) io_ciflist = io_curlist = cl; else
		{
			while(io_curlist.next != null)
				io_curlist = io_curlist.next;
			io_curlist.next = cl;
			io_curlist = io_curlist.next;
		}
	}

	private CIFLIST io_newciflist(int id)
	{
		CIFLIST newcl = new CIFLIST();
		newcl.next = null;
		newcl.identity = id;
		switch (id)
		{
			case C_START:
				CFSTART cs = new CFSTART();
				newcl.member = cs;
				cs.name = null;
				break;
			case C_BOX:
				newcl.member = new CBOX();
				break;
			case C_POLY:
				newcl.member = new CPOLY();
				break;
			case C_GNAME:
				newcl.member = new CGNAME();
				break;
			case C_LABEL:
				newcl.member = new CLABEL();
				break;
			case C_CALL:
				CCALL cc = new CCALL();
				newcl.member = cc;
				cc.name = null;
				break;
		}
		return newcl;
	}

	private void io_outputflash(Layer lay, int diameter, Point center)
	{
		// flash approximated by an octagon
		int radius = diameter/2;
		double fcx = center.x;
		double fcy = center.y;
		double offset = ((diameter)/2.0f)*0.414213f;
		path fpath = io_makepath();
		Point temp = new Point();

		temp.x = center.x-radius;
		temp.y = (int)(fcy+offset);
		if (io_appendpoint(fpath, temp)) return;
		temp.y = (int)(fcy-offset);
		if (io_appendpoint(fpath, temp)) return;
		temp.x = (int)(fcx-offset);
		temp.y = center.y-radius;
		if (io_appendpoint(fpath, temp)) return;
		temp.x = (int)(fcx+offset);
		if (io_appendpoint(fpath, temp)) return;
		temp.x = center.x+radius;
		temp.y = (int)(fcy-offset);
		if (io_appendpoint(fpath, temp)) return;
		temp.y = (int)(fcy+offset);
		if (io_appendpoint(fpath, temp)) return;
		temp.x = (int)(fcx+offset);
		temp.y = center.y+radius;
		if (io_appendpoint(fpath, temp)) return;
		temp.x = (int)(fcx-offset);
		if (io_appendpoint(fpath, temp)) return;

		io_outputpolygon(lay, fpath);
	}

	/**
	 * convert wires to boxes and flashes
	 */
	private void io_outputwire(Layer lay, int width, path wpath)
	{
		int lim = io_pathlength(wpath);
		Point prev = io_removepoint(wpath);

		// do not use roundflashes with zero-width wires
		if (width != 0 && !IOTool.isCIFInSquaresWires())
		{
			io_bbflash(width, prev);
			io_outputflash(lay, width, prev);
		}
		for (int i = 1; i < lim; i++)
		{
			Point curr = io_removepoint(wpath);

			// do not use roundflashes with zero-width wires
			if (width != 0 && !IOTool.isCIFInSquaresWires())
			{
				io_bbflash(width, curr);
				io_outputflash(lay, width, curr);
			}
			int xr = curr.x-prev.x;   int yr = curr.y-prev.y;
			int len = (int)new Point2D.Double(0, 0).distance(new Point2D.Double(xr, yr));
			if (IOTool.isCIFInSquaresWires()) len += width;
			Point center = new Point((curr.x+prev.x)/2, (curr.y+prev.y)/2);
			io_bbbox(len, width, center, xr, yr);
			io_outputbox(lay, len, width, center, xr, yr);
			prev = curr;
		}
	}

	private void io_decrefcount(int id)
	{
		if (id >= CONTSIZE || io_cifcarray[id].refcount == 0)
		{
			io_report("illegal context: io_decrefcount", FATALINTERNAL);
			return;
		}
		io_cifcarray[id].refcount--;
	}

	private boolean io_endseen()
	{
		return io_cifendcom;
	}

	private void io_swapcontext(int id)
	{
		if (id >= CONTSIZE || io_cifcarray[id].refcount == 0)
		{
			io_report("illegal swap context", FATALINTERNAL);
			return;
		}
		if (io_cifcurcontext == id) return;
		io_cifcarray[io_cifcurcontext].ctop = io_cifstacktop;		// save current context
		io_cifcurcontext = id;
		io_cifstacktop = io_cifcarray[io_cifcurcontext].ctop;
	}

	private void io_initinput()
	{
		io_ciflinecount = 1;
		io_cifcharcount = 0;
		io_cifresetbuffer = true;
	}

	private void io_initerror()
	{
		io_cifecounts = new int[OTHER+1];
		for (int i = 0; i <= OTHER; i++) io_cifecounts[i] = 0;
	}

	private void io_initutil()
	{
		io_cifmmleft = new int[MAXMMSTACK];
		io_cifmmright = new int[MAXMMSTACK];
		io_cifmmbottom = new int[MAXMMSTACK];
		io_cifmmtop = new int[MAXMMSTACK];
		io_cifstable = new HashMap();
		io_cifmmptr = -1;			// minmax stack pointer
	}

	private void io_inittrans()
	{
		io_cifstacktop = new tmatrix();
		io_clear(io_cifstacktop);
		io_cifstacktop.next = null;
		io_cifstacktop.prev = null;
		io_cifstacktop.multiplied = true;

		io_cifcarray = new context[CONTSIZE];
		for(int i=0; i<CONTSIZE; i++) io_cifcarray[i] = new context();
		io_cifcarray[0].base = io_cifstacktop;
		io_cifcarray[0].refcount = 1;	// this context will never go away
		io_cifcarray[0].ctop = io_cifstacktop;
		for (int i=1; i < CONTSIZE; i++)
		{
			io_cifcarray[i].base = null;
			io_cifcarray[i].refcount = 0;
			io_cifcarray[i].ctop = null;
		}
		io_cifcurcontext = 0;		// where we are now
	}

	private void io_clear(tmatrix mat)
	{
		mat.a11 = 1.0;   mat.a12 = 0.0;
		mat.a21 = 0.0;   mat.a22 = 1.0;
		mat.a31 = 0.0;   mat.a32 = 0.0;   mat.a33 = 1.0;
		mat.type = TIDENT;   mat.multiplied = false;
	}

	private void io_infromfile()
	{
		try
		{
			io_cifnextchar = lineReader.read();
		} catch (IOException e)
		{
			io_cifnextchar = -1;
		}
	}

	private char io_getch()
	{
		if (io_cifresetbuffer)
		{
			io_cifresetbuffer = false;
			io_ciflinebuffer = new StringBuffer();
			io_cifcharcount = 0;
		}

		int c = io_cifnextchar;
		if (c >= 0)
		{
			if (c == '\n') io_ciflinecount++;
			if (c != '\n')
			{
				io_cifcharcount++;
				io_ciflinebuffer.append((char)c);
			} else io_cifresetbuffer = true;
			try
			{
				io_cifnextchar = lineReader.read();
			} catch (IOException e)
			{
				io_cifnextchar = -1;
			}
		}
		return (char)c;
	}

	private char io_peek()
	{
		return (char)io_cifnextchar;
	}

	private boolean io_endoffile()
	{
		return io_cifnextchar < 0;
	}

	private int io_flush(char breakchar)
	{
		int c;
		while ((c = io_peek()) >= 0 && c != breakchar) io_getch();
		return c;
	}

	private void io_blank()
	{
		for(;;)
		{
			int c = io_peek();
			switch (c)
			{
				case '(':
				case ')':
				case ';':
				case '-':
				case -1:
					return;
				default:
					if (Character.isDigit((char)c) || Character.isUpperCase((char)c)) return;
					io_getch();
			}
		}
	}

	private int io_parsestatement()
	{
		io_blank();		// flush initial junk

		int curchar = io_getch();
		int command = 0;
		int xrotate=0, yrotate=0, length=0, width=0, diameter=0, symbolnumber=0, multiplier=0, divisor=0, usercommand=0;
		Point center = null, namepoint = null;
		tlist curtlist = null;
		path curpath = null;
		String lname = null, nametext = null, usertext = null;
		switch (curchar)
		{
			case 'P':
				command = POLYCOM;
				curpath = io_makepath();
				io_getpath(curpath); if (io_ciferrorfound) return reportError();
				break;

			case 'B':
				command = BOXCOM;
				xrotate = 1; yrotate = 0;
				length = io_cardinal(); if (io_ciferrorfound) return reportError();
				width = io_cardinal(); if (io_ciferrorfound) return reportError();
				center = io_getpoint(); if (io_ciferrorfound) return reportError(); io_sep();
				if (((curchar = io_peek()) >= '0' && curchar <= '9') || curchar == '-')
				{
					xrotate = io_signed(); if (io_ciferrorfound) return reportError();
					yrotate = io_signed(); if (io_ciferrorfound) return reportError();
				}
				break;

			case 'R':
				command = FLASHCOM;
				diameter = io_cardinal(); if (io_ciferrorfound) return reportError();
				center = io_getpoint(); if (io_ciferrorfound) return reportError();
				break;

			case 'W':
				command = WIRECOM;
				width = io_cardinal(); if (io_ciferrorfound) return reportError();
				curpath = io_makepath();
				io_getpath(curpath); if (io_ciferrorfound) return reportError();
				break;

			case 'L':
				command = LAYER;
				io_blank();
				StringBuffer layerName = new StringBuffer();
				for (int i = 0; i<4; i++)
				{
					int chr = io_peek();
					if (!Character.isUpperCase((char)chr) && !Character.isDigit((char)chr)) break;
					layerName.append(io_getch());
				}
				if (layerName.length() == 0) {io_ciferrorfound = true; io_ciferrortype = NOLAYER; return reportError();}
				lname = layerName.toString();
				break;

			case 'D':
				io_blank();
				switch (io_getch())
				{
					case 'S':
						command = DEFSTART;
						symbolnumber = io_cardinal(); if (io_ciferrorfound) return reportError();
						io_sep(); multiplier = divisor = 1;
						if (Character.isDigit((char)io_peek()))
						{
							multiplier = io_cardinal(); if (io_ciferrorfound) return reportError();
							divisor = io_cardinal(); if (io_ciferrorfound) return reportError();
						}
						if (io_cifdefinprog)
						{
							io_ciferrorfound = true;
							io_ciferrortype = NESTDEF;
							return reportError();
						}
						io_cifdefinprog = true;
						break;
					case 'F':
						command = DEFEND;
						if (!io_cifdefinprog)
						{
							io_ciferrorfound = true;
							io_ciferrortype = NODEFSTART;
							return reportError();
						}
						io_cifdefinprog = false;
						break;
					case 'D':
						command = DELETEDEF;
						symbolnumber = io_cardinal(); if (io_ciferrorfound) return reportError();
						if (io_cifdefinprog)
						{
							io_ciferrorfound = true;
							io_ciferrortype = NESTDD;
							return reportError();
						}
						break;
					default:
						io_ciferrorfound = true;
						io_ciferrortype = BADDEF;
						return reportError();
				}
				break;

			case 'C':
				command = CALLCOM;
				symbolnumber = io_cardinal(); if (io_ciferrorfound) return reportError();
				io_blank();
				curtlist = io_maketlist();
				for(;;)
				{
					tentry trans = new tentry();
					int val = io_peek();
					if (val == ';') break;
					switch (io_peek())
					{
						case 'T':
							io_getch();
							trans.kind = TRANSLATE;
							trans.xt = io_signed(); if (io_ciferrorfound) return reportError();
							trans.yt = io_signed(); if (io_ciferrorfound) return reportError();
							io_appendtentry(curtlist, trans);
							break;

						case 'M':
							trans.kind = MIRROR;
							io_getch(); io_blank();
							switch (io_getch())
							{
								case 'X': trans.xcoord = true; break;
								case 'Y': trans.xcoord = false; break;
								default:  io_ciferrorfound = true; io_ciferrortype = BADAXIS; return reportError();
							}
							io_appendtentry(curtlist, trans);
							break;

						case 'R':
							trans.kind = ROTATE;
							io_getch();
							trans.xrot = io_signed(); if (io_ciferrorfound) return reportError();
							trans.yrot = io_signed(); if (io_ciferrorfound) return reportError();
							io_appendtentry(curtlist, trans);
							break;

						default:
							io_ciferrorfound = true; io_ciferrortype = BADTRANS; return reportError();
					}
					io_blank();		// between transformation commands
				}	// end of while (1) loop
				break;

			case '(':
				{
					int level = 1;
					command = COMMENT;
					StringBuffer comment = new StringBuffer();
					while (level != 0)
					{
						curchar = io_getch();
						switch (curchar)
						{
							case '(':
								level++;
								comment.append('(');
								break;
							case ')':
								level--;
								if (level != 0) comment.append(')');
								break;
							case -1:
								io_ciferrorfound = true; io_ciferrortype = BADCOMMENT; return reportError();
							default:
								comment.append(curchar);
						}
					}
				}
				break;

			case 'E':
				io_blank();
				if (io_cifdefinprog)
				{
					io_ciferrorfound = true;
					io_ciferrortype = NESTEND;
					return reportError();
				}
				if (!io_endoffile()) io_report("more text follows end command", ADVISORY);
				io_cifendisseen = true;
				io_iend();
				return END;

			case ';':
				return NULLCOMMAND;

			case -1:
				return ENDFILE;

			default:
				if (Character.isDigit((char)curchar))
				{
					usercommand = curchar - '0';
					if (usercommand == 9)
					{
						curchar = io_peek();
						if (curchar == ' ' || curchar == '\t' || curchar == '1' || curchar == '2' || curchar == '3')
						{
							switch (io_getch())
							{
								case ' ':
								case '\t':
									io_sp(); nametext = io_name(); if (io_ciferrorfound) return reportError();
									command = SYMNAME;
									break;
								case '1':
								case '2':
								case '3':
									if (!io_sp())
									{
										io_ciferrorfound = true; io_ciferrortype = NOSPACE;
										return reportError();
									}
									nametext = io_name(); if (io_ciferrorfound) return reportError();
									switch (curchar)
									{
										case '1':
											command = INSTNAME;
											break;
										case '2':
										{
											command = GEONAME;
											namepoint = io_getpoint(); if (io_ciferrorfound) return reportError();
											io_blank();
											StringBuffer layName = new StringBuffer();
											for (int i = 0; i<4; i++)
											{
												int chr = io_peek();
												if (!Character.isUpperCase((char)chr) && !Character.isDigit((char)chr)) break;
												layName.append(io_getch());
											}
											lname = layName.toString();
											break;
										}
										case '3':
											command = LABELCOM;
											namepoint = io_getpoint(); if (io_ciferrorfound) return reportError();
											break;
									}
									break;
							}
						}
					} else
					{
						command = USERS;
						usertext = io_utext();
						if (io_endoffile())
						{
							io_ciferrorfound = true; io_ciferrortype = BADUSER; return reportError();
						}
					}
				} else
				{
					io_ciferrorfound = true;
					io_ciferrortype = BADCOMMAND;
					return reportError();
				}
		}

		// by now we have a syntactically valid command although it might be missing a semi-colon
		switch (command)
		{
			case WIRECOM:
				io_iwire(width, curpath);
				break;
			case DEFSTART:
				io_idefstart(symbolnumber, multiplier, divisor);
				break;
			case DEFEND:
				io_idefend();
				break;
			case DELETEDEF:
				io_ideletedef(symbolnumber);
				break;
			case CALLCOM:
				io_icall(symbolnumber, curtlist);
				break;
			case LAYER:
				io_ilayer(lname);
				break;
			case FLASHCOM:
				io_iflash(diameter, center);
				break;
			case POLYCOM:
				io_ipolygon(curpath);
				break;
			case BOXCOM:
				io_ibox(length, width, center, xrotate, yrotate);
				break;
			case COMMENT:
				break;
			case USERS:
				io_iusercommand(usercommand, usertext);
				break;
			case SYMNAME:
				io_isymname(nametext);
				break;
			case INSTNAME:
				io_iinstname(nametext);
				break;
			case GEONAME:
				io_igeoname(nametext, namepoint, curTech.findLayer(lname));
				break;
			case LABELCOM:
				io_ilabel(nametext, namepoint);
				break;
			default:
				io_ciferrorfound = true;
				io_ciferrortype = INTERNAL;
				return reportError();
		}
		if (!io_semi()) {io_ciferrorfound = true; io_ciferrortype = NOSEMI; return reportError();}

		return(command);
	}

	private void io_ilabel(String name, Point pt)
	{
		io_cifstatesince++;
		if (io_cifignore) return;
		if (name.length() == 0)
		{
			io_report("null label ignored", ADVISORY);
			return;
		}
		label obj = new label();
		if (io_cifdefinprog && io_cifscalefactor != 1.0)
		{
			pt.x = (int)(io_cifscalefactor * pt.x);
			pt.y = (int)(io_cifscalefactor * pt.y);
		}
		obj.la_pos = pt;
		obj.la_name = name;

		io_pushtrans();
		Point temp = io_transpoint(pt);
		io_poptrans();
		obj.bb.l = temp.x;
		obj.bb.r = temp.x;
		obj.bb.b = temp.y;
		obj.bb.t = temp.y;

		if (io_cifdefinprog)
		{
			// insert into symbol's guts
			obj.next = io_cifcurrent.st_guts;
			io_cifcurrent.st_guts = obj;
		} else io_item(obj);		// stick into item list
	}

	private void io_igeoname(String name, Point pt, Layer lay)
	{
		io_cifstatesince++;
		if (io_cifignore) return;
		if (name.length() == 0)
		{
			io_report("null geometry name ignored", ADVISORY);
			return;
		}
		gname obj = new gname();
		obj.layer = lay;
		if (io_cifdefinprog && io_cifscalefactor != 1.0)
		{
			pt.x = (int)(io_cifscalefactor * pt.x);
			pt.y = (int)(io_cifscalefactor * pt.y);
		}
		obj.gn_pos = pt;
		obj.gn_name = name;

		io_pushtrans();
		Point temp = io_transpoint(pt);
		io_poptrans();
		obj.bb.l = temp.x;
		obj.bb.r = temp.x;
		obj.bb.b = temp.y;
		obj.bb.t = temp.y;

		if (io_cifdefinprog)
		{
			// insert into symbol's guts
			obj.next = io_cifcurrent.st_guts;
			io_cifcurrent.st_guts = obj;
		} else io_item(obj);		// stick into item list
	}

	private void io_isymname(String name)
	{
		io_cifstatesince++;
		if (io_cifignore) return;
		if (!io_cifdefinprog)
		{
			io_report("no symbol to name", FATALSEMANTIC);
			return;
		}
		if (name.length() == 0)
		{
			io_report("null symbol name ignored", ADVISORY);
			return;
		}
		if (io_cifnamed)
		{
			io_report("symbol is already named, new name ignored", FATALSEMANTIC);
			return;
		}
		io_cifnamed = true;
		io_cifcurrent.st_name = name;
	}

	private void io_iusercommand(int command, String text)
	{
		io_cifstatesince++;
		if (io_cifignore) return;
	}

	private void io_ibox(int length, int width, Point center, int xr, int yr)
	{
		io_cifstatesince++;
		if (io_cifignore) return;
		if (io_cifcurlayer == null)
		{
			io_nulllayererrors++;
			return;
		}
		if (length == 0 || width == 0)
		{
			io_report("box with null length or width specified, ignored", ADVISORY);
			return;
		}

		if (io_cifdefinprog && io_cifscalefactor != 1.0)
		{
			length = (int) (io_cifscalefactor * length);
			width = (int) (io_cifscalefactor * width);
			center.x = (int) (io_cifscalefactor * center.x);
			center.y = (int) (io_cifscalefactor * center.y);
		}

		Rectangle box = io_bbbox(length, width, center, xr, yr);
		int tl = box.x;   int tr = box.x + box.width;
		int tb = box.y;   int tt = box.y + box.height;

		// check for manhattan box
		int halfw = width/2;
		int halfl = length/2;
		if (
			(yr == 0 && (length%2) == 0 && (width%2) == 0 &&
			(center.x-halfl) == tl && (center.x+halfl) == tr &&
			(center.y-halfw) == tb && (center.y+halfw) == tt)
			||
			(xr == 0 && (length%2) == 0 && (width%2) == 0 &&
			(center.x-halfw) == tl && (center.x+halfw) == tr &&
			(center.y-halfl) == tb && (center.y+halfl) == tt)
		)
		{
			mbox obj = new mbox();
			obj.layer = io_cifcurlayer;
			if (yr == 0)
			{
				obj.bb.l = tl;
				obj.bb.r = tr;
				obj.bb.b = tb;
				obj.bb.t = tt;
			} else
			{
				// this assumes that bb is unaffected by rotation
				obj.bb.l = center.x-halfw;
				obj.bb.r = center.x+halfw;
				obj.bb.b = center.y-halfl;
				obj.bb.t = center.y+halfl;
			}
			if (io_cifdefinprog)
			{
				// insert into symbol's guts
				obj.next = io_cifcurrent.st_guts;
				io_cifcurrent.st_guts = obj;
			} else io_item(obj);		// stick into item list
		} else
		{
			box obj = new box();
			obj.bo_layer = io_cifcurlayer;
			obj.bo_length = length;
			obj.bo_width = width;
			obj.bo_center = center;
			obj.bo_xrot = xr;
			obj.bo_yrot = yr;

			obj.bb.l = tl;
			obj.bb.r = tr;
			obj.bb.b = tb;
			obj.bb.t = tt;
			if (io_cifdefinprog)
			{
				// insert into symbol's guts
				obj.next = io_cifcurrent.st_guts;
				io_cifcurrent.st_guts = obj;
			} else io_item(obj);		// stick into item list
		}
	}

	private void io_iflash(int diameter, Point center)
	{
		io_cifstatesince++;
		if (io_cifignore) return;
		if (io_cifcurlayer == null)
		{
			io_nulllayererrors++;
			return;
		}
		if (diameter == 0)
		{
			io_report("flash with null diamter, ignored", ADVISORY);
			return;
		}

		flash obj = new flash();
		obj.layer = io_cifcurlayer;
		if (io_cifdefinprog && io_cifscalefactor != 1.0)
		{
			diameter = (int) (io_cifscalefactor * diameter);
			center.x = (int) (io_cifscalefactor * center.x);
			center.y = (int) (io_cifscalefactor * center.y);
		}
		obj.fl_diameter = diameter;
		obj.fl_center = center;

		Rectangle box = io_bbflash(diameter, center);
		obj.bb.l = box.x;
		obj.bb.r = box.x + box.width;
		obj.bb.b = box.y;
		obj.bb.t = box.y + box.height;

		if (io_cifdefinprog)
		{
			// insert into symbol's guts
			obj.next = io_cifcurrent.st_guts;
			io_cifcurrent.st_guts = obj;
		} else io_item(obj);		// stick into item list
	}

	private Rectangle io_bbflash(int diameter, Point center)
	{
		return io_bbbox(diameter, diameter, center, 1, 0);
	}

	private Rectangle io_bbbox(int length, int width, Point center, int xr, int yr)
	{
		int dx = length/2;
		int dy = width/2;

		io_pushtrans();	// newtrans
		io_rotate(xr, yr);
		io_translate(center.x, center.y);
		Point temp = new Point(dx, dy);
		io_initmm(io_transpoint(temp));
		temp.y = -dy;
		io_minmax(io_transpoint(temp));
		temp.x = -dx;
		io_minmax(io_transpoint(temp));
		temp.y = dy;
		io_minmax(io_transpoint(temp));
		io_poptrans();
		Rectangle ret = new Rectangle(io_minx(), io_miny(), io_maxx()-io_minx(), io_maxy()-io_miny());
		io_donemm();
		return ret;
	}

	private void io_ilayer(String lname)
	{
		io_cifstatesince++;
		if (io_cifignore) return;
		io_cifcurlayer = curTech.findLayer(lname);
	}

	private void io_icall(int symbol, tlist list)
	{
		if (io_cifignore) return;
		int j = io_tlistlength(list);
		tlist newtlist = null;
		if (j != 0) newtlist = io_maketlist();

		io_pushtrans();		// get new frame of reference
		for (int i = 1; i <=j; i++)
		{
			// build up incremental transformations
			tentry temp = io_removetentry(list);
			if (temp.kind == MIRROR)
			{
				io_mirror(temp.xcoord);
			} else if (temp.kind == TRANSLATE)
			{
				if (io_cifdefinprog && io_cifscalefactor != 1.0)
				{
					temp.xt = (int)(io_cifscalefactor * temp.xt);
					temp.yt = (int)(io_cifscalefactor * temp.yt);
				}
				io_translate(temp.xt, temp.yt);
			} else if (temp.kind == ROTATE)
			{
				io_rotate(temp.xrot, temp.yrot);
			} else
			{
				io_report("interpreter: no such transformation", FATALINTERNAL);
			}
			io_appendtentry(newtlist, temp);	// copy the list
		}

		symcall obj = new symcall();
		obj.sy_tm = io_getlocal();
		io_poptrans();		// return to previous state

		obj.sy_symnumber = symbol;
		obj.sy_unid = null;
		obj.sy_tlist = newtlist;

		io_cifinstance++;		// increment the instance count for names
		if (io_cifnamepending)
		{
			if (io_cifstatesince != 0)
				io_report("statements between name and instance", ADVISORY);
			obj.sy_name = io_cifnstored;
			io_cifnamepending = false;
		} else
		{
			obj.sy_name = io_makeinstname(io_cifinstance);
		}
		if (io_cifdefinprog)
		{
			// insert into guts of symbol
			obj.next = io_cifcurrent.st_guts;
			io_cifcurrent.st_guts = obj;
			io_cifcurrent.st_ncalls++;
		} else io_item(obj);
	}

	private void io_rotate(int xrot,int yrot)
	{
		double si = yrot;
		double co = xrot;
		double temp;

		if (yrot == 0 && xrot >= 0) return;

		io_cifstacktop.type |= TROTATE;
		if (xrot == 0)
		{
			temp = io_cifstacktop.a11;
			io_cifstacktop.a11 = -io_cifstacktop.a12;
			io_cifstacktop.a12 = temp;

			temp = io_cifstacktop.a21;
			io_cifstacktop.a21 = -io_cifstacktop.a22;
			io_cifstacktop.a22 = temp;

			temp = io_cifstacktop.a31;
			io_cifstacktop.a31 = -io_cifstacktop.a32;
			io_cifstacktop.a32 = temp;
			if (yrot < 0) io_cifstacktop.a33 = -io_cifstacktop.a33;
		} else
			if (yrot == 0) io_cifstacktop.a33 = -io_cifstacktop.a33;	// xrot < 0
		else
		{
			temp = io_cifstacktop.a11*co - io_cifstacktop.a12*si;
			io_cifstacktop.a12 = io_cifstacktop.a11*si + io_cifstacktop.a12*co;
			io_cifstacktop.a11 = temp;
			temp = io_cifstacktop.a21*co - io_cifstacktop.a22*si;
			io_cifstacktop.a22 = io_cifstacktop.a21*si + io_cifstacktop.a22*co;
			io_cifstacktop.a21 = temp;
			temp = io_cifstacktop.a31*co - io_cifstacktop.a32*si;
			io_cifstacktop.a32 = io_cifstacktop.a31*si + io_cifstacktop.a32*co;
			io_cifstacktop.a31 = temp;
			io_cifstacktop.a33 = new Point2D.Double(0, 0).distance(new Point2D.Double(co, si));
		}
	}

	private void io_translate(int xtrans, int ytrans)
	{
		if (xtrans != 0 || ytrans != 0)
		{
			io_cifstacktop.a31 += io_cifstacktop.a33*xtrans;
			io_cifstacktop.a32 += io_cifstacktop.a33*ytrans;
			io_cifstacktop.type |= TTRANSLATE;
		}
	}

	private void io_mirror(boolean xcoord)
	{
		if (xcoord)
		{
			io_cifstacktop.a11 = -io_cifstacktop.a11;
			io_cifstacktop.a21 = -io_cifstacktop.a21;
			io_cifstacktop.a31 = -io_cifstacktop.a31;
		} else
		{
			io_cifstacktop.a12 = -io_cifstacktop.a12;
			io_cifstacktop.a22 = -io_cifstacktop.a22;
			io_cifstacktop.a32 = -io_cifstacktop.a32;
		}
		io_cifstacktop.type |= TMIRROR;
	}

	private int io_tlistlength(tlist a)
	{
		if (a == null) return 0;
		return a.tlength;
	}

	private String io_makeinstname(int n)
	{
		return "INST<" + n + ">";
	}

	private tentry io_removetentry(tlist a)
	{
		if (a.tfirst == null)
		{
			// added extra code to initialize "ans" to a dummy value
			tentry ans = new tentry();
			ans.kind = TRANSLATE;
			ans.xt = ans.yt = 0;
			return ans;
		}
		linkedtentry temp = a.tfirst.tnext;
		tentry ans = a.tfirst.tvalue;
		a.tfirst = temp;
		if (a.tfirst == null) a.tlast = null;
		a.tlength -= 1;
		return ans;
	}

	private tmatrix io_getlocal()
	{
		return io_cifstacktop;
	}

	private void io_ideletedef(int n)
	{
		io_cifstatesince++;
		io_report("DD not supported (ignored)", ADVISORY);
	}

	private void io_idefend()
	{
		io_cifstatesince++;
		if (io_cifignore)
		{
			io_cifignore = false;
			return;
		}
		io_cifdefinprog = false;
		io_cifcurlayer = io_cifbackuplayer;		// restore old layer
		io_cifinstance = io_cifbinst;
		if (!io_cifnamed)
		{
			String s = io_makesymname(io_cifcurrent.st_symnumber);
			io_cifcurrent.st_name = s;
		}
		io_cifcurrent.st_defined = true;
	}

	private String io_makesymname(int n)
	{
		return "SYM" + n;
	}

	private void io_idefstart(int symbol, int mtl, int div)
	{
		io_cifstatesince++;
		io_cifcurrent = io_lookupsym(symbol);
		if (io_cifcurrent.st_defined)
		{
			// redefining this symbol
			String mess = "attempt to redefine symbol " + symbol + " (ignored)";
			io_report(mess, ADVISORY);
			io_cifignore = true;
			return;
		}

		io_cifdefinprog = true;
		if (mtl != 0 && div != 0) io_cifscalefactor = ((float) mtl)/((float) div); else
		{
			io_report("illegal scale factor, ignored", ADVISORY);
			io_cifscalefactor = 1.0;
		}
		io_cifbackuplayer = io_cifcurlayer;	// save current layer
		io_cifbinst = io_cifinstance;		// save instance count
		io_cifcurlayer = null;
		io_cifnamed = false;					// symbol not named
		io_cifinstance = 0;					// no calls in symbol yet
	}

	private void io_iwire(int width, path a)
	{
		int length = io_pathlength(a);
		io_cifstatesince++;
		if (io_cifignore) return;
		if (io_cifcurlayer == null)
		{
			io_nulllayererrors++;
			return;
		}
		wire obj = new wire();
		path tpath = a;
		path spath = null;			// path in case of scaling
		obj.layer = io_cifcurlayer;
		obj.wi_numpts = length;
		if (io_cifdefinprog && io_cifscalefactor != 1.0)
		{
			spath = io_makepath();		// create a new path
			io_scalepath(a, spath);		// scale all points
			width = (int)(io_cifscalefactor * width);
			tpath = spath;
		}
		obj.wi_width = width;

		path bbpath = io_makepath();		// get a new path for bb use
		io_copypath(tpath, bbpath);
		Rectangle box = io_bbwire(width, bbpath);
		obj.wi_p = new Point[length];
		for (int i = 0; i < length; i++) obj.wi_p[i] = io_removepoint(tpath);

		if (io_cifdefinprog)
		{
			// insert into symbol's guts
			obj.next = io_cifcurrent.st_guts;
			io_cifcurrent.st_guts = obj;
		} else io_item(obj);		// stick into item list
	}

	private Rectangle io_bbwire(int width, path ppath)
	{
		int half = (width+1)/2;
		int limit = io_pathlength(ppath);

		io_pushtrans();	// newtrans
		io_initmm(io_transpoint(io_removepoint(ppath)));
		for (int i = 1; i < limit; i++)
		{
			io_minmax(io_transpoint(io_removepoint(ppath)));
		}
		io_poptrans();
		Rectangle rect = new Rectangle(io_minx()-half, io_miny()-half, io_maxx()-io_minx()+half*2, io_maxy()-io_miny()+half*2);
		io_donemm();
		return rect;
	}

	private String io_utext()
	{
		StringBuffer user = new StringBuffer();
		for(;;)
		{
			if (io_endoffile()) break;
			if (io_peek() == ';') break;
			user.append((char)io_getch());
		}
		return user.toString();
	}

	private String io_name()
	{
		StringBuffer ntext = new StringBuffer();
		boolean nochar = true;
		for(;;)
		{
			if (io_endoffile()) break;
			int c = io_peek();
			if (c == ';' || c == ' ' || c == '\t' || c == '{' || c == '}') break;
			nochar = false;
			io_getch();
			ntext.append((char)c);
		}
		if (nochar) io_logit(NONAME);
		return ntext.toString();
	}

	private void io_appendtentry(tlist a, tentry p)
	{
		linkedtentry newt = io_gettnode();
		if (newt == null) return;

		linkedtentry temp = a.tlast;
		a.tlast = newt;
		if (temp != null) temp.tnext = a.tlast;
		a.tlast.tvalue = p;
		a.tlast.tnext = null;
		if (a.tfirst == null) a.tfirst = a.tlast;
		a.tlength += 1;
	}

	private linkedtentry io_gettnode()
	{
		return new linkedtentry();
	}

	private tlist io_maketlist()
	{
		tlist a = new tlist();
		a.tfirst = null;
		a.tlast = null;
		a.tlength = 0;
		return a;
	}

	private int io_cardinal()
	{
		boolean somedigit = false;
		int ans = 0;
		io_sp();

		while (ans < Integer.MAX_VALUE && Character.isDigit((char)io_peek()))
		{
			ans *= 10; ans += io_getch() - '0';
			somedigit = true;
		}

		if (!somedigit)
		{
			io_logit(NOUNSIGNED);
			return(0);
		}
		if (Character.isDigit((char)io_peek()))
		{
			io_logit(NUMTOOBIG);
			return(0XFFFFFFFF);
		}
		return(ans);
	}

	private boolean io_semi()
	{
		boolean ans = false;
		io_blank();
		if (io_peek() == ';') { io_getch(); ans = true; io_blank(); }
		return ans;
	}

	private boolean io_sp()
	{
		boolean ans = false;
		for(;;)
		{
			int c = io_peek();
			if (c != ' ' && c != '\t') break;
			io_getch();
			ans = true;
		}
		return ans;
	}

	private void io_ipolygon(path a)
	{
		int length = io_pathlength(a);
		io_cifstatesince++;
		if (io_cifignore) return;
		if (io_cifcurlayer == null)
		{
			io_nulllayererrors++;
			return;
		}
		if (length < 3)
		{
			io_report("polygon with < 3 pts in path, ignored", ADVISORY);
			return;
		}

		polygon obj = new polygon();
		path tpath = a;
		path spath = null;			// path in case of scaling
		obj.layer = io_cifcurlayer;
		if (io_cifdefinprog && io_cifscalefactor != 1.0)
		{
			spath = io_makepath();		// create a new path
			io_scalepath(a, spath);		// scale all points
			tpath = spath;
		}

		path bbpath = io_makepath();		// get a new path for bb use
		io_copypath(tpath, bbpath);
		Rectangle box = io_bbpolygon(bbpath);
		obj.bb.l = box.x;
		obj.bb.r = box.x + box.width;
		obj.bb.b = box.y;
		obj.bb.t = box.y + box.height;
		obj.po_p = new Point[length];
		for (int i = 0; i < length; i++) obj.po_p[i] = io_removepoint(tpath);

		if (io_cifdefinprog)
		{
			// insert into symbol's guts
			obj.next = io_cifcurrent.st_guts;
			io_cifcurrent.st_guts = obj;
		} else io_item(obj);		// stick into item list
	}

	/**
	 * a bare item has been found
	 */
	private void io_item(CifBase object)
	{
		if (object == null)
		{
			io_report("item: null object", FATALINTERNAL);
			return;
		}

		itementry newitem = io_newnode();
		newitem.it_rel = null;
		newitem.it_norel = null;
		newitem.it_same = io_cifilist;		// hook into linked list
		io_cifilist = newitem;
		newitem.it_what = object;
		newitem.it_level = 0;		// bare item
		newitem.it_context = 0;		// outermost context
		io_increfcount(0);

		// symbol calls only
		if (object instanceof symcall) io_findcallbb((symcall)object);
	}

	/**
	 * find the bb for this particular call
	 */
	private void io_findcallbb(symcall object)
	{
		stentry thisst = io_lookupsym(object.sy_symnumber);

		if (!thisst.st_defined)
		{
			String mess = "call to undefined symbol " + thisst.st_symnumber;
			io_report(mess, FATALSEMANTIC);
			return;
		}
		if (thisst.st_expanded)
		{
			String mess = "recursive call on symbol " + thisst.st_symnumber;
			io_report(mess, FATALSEMANTIC);
			return;
		}
		thisst.st_expanded = true;		// mark as under expansion

		if (!thisst.st_frozen) thisst.st_frozen = true;

		io_findbb(thisst);		// get the bb of the symbol in its stentry
		object.sy_unid = thisst;	// get this symbol's id

		io_pushtrans();			// set up a new frame of reference
		io_applylocal(object.sy_tm);
		Point temp = new Point();
		temp.x = thisst.st_bb.l;   temp.y = thisst.st_bb.b;	// ll
		Point comperror = io_transpoint(temp);
		io_initmm(comperror);
		temp.x = thisst.st_bb.r;
		comperror = io_transpoint(temp);
		io_minmax(comperror);
		temp.y = thisst.st_bb.t;	// ur
		comperror = io_transpoint(temp);
		io_minmax(comperror);
		temp.x = thisst.st_bb.l;
		comperror = io_transpoint(temp);
		io_minmax(comperror);

		object.bb.l = io_minx();   object.bb.r = io_maxx();
		object.bb.b = io_miny();   object.bb.t = io_maxy();
		io_donemm();		// object now has transformed bb of the symbol
		io_poptrans();

		thisst.st_expanded = false;
	}

	/**
	 * find bb for sym
	 */
	private void io_findbb(stentry sym)
	{
		boolean first = true;
		CifBase ob = sym.st_guts;

		if (sym.st_bbvalid) return;			// already done
		if (ob == null)			// empty symbol
		{
			String mess = "symbol " + sym.st_symnumber + " has no geometry in it";
			io_report(mess, ADVISORY);
			sym.st_bb.l = 0;   sym.st_bb.r = 0;
			sym.st_bb.b = 0;   sym.st_bb.t = 0;
			sym.st_bbvalid = true;
			return;
		}

		while (ob != null)
		{
			// find bb for symbol calls, all primitive are done already
			if (ob instanceof symcall) io_findcallbb((symcall)ob);
			Point temp = new Point();
			temp.x = ob.bb.l;   temp.y = ob.bb.b;
			if (first) {first = false; io_initmm(temp);}
				else io_minmax(temp);
			temp.x = ob.bb.r;   temp.y = ob.bb.t;
			io_minmax(temp);
			ob = ob.next;
		}
		sym.st_bb.l = io_minx();   sym.st_bb.r = io_maxx();
		sym.st_bb.b = io_miny();   sym.st_bb.t = io_maxy();
		sym.st_bbvalid = true;
		io_donemm();
	}

	/**
	 * io_lookupsym(sym), if none, make
	 * a blank entry. return a pointer to whichever
	 */
	private stentry io_lookupsym(int sym)
	{
		stentry val = (stentry)io_cifstable.get(new Integer(sym));
		if (val == null)
		{
			// create a new entry
			val = new stentry();
			io_newstentry(val, sym);
			io_cifstable.put(new Integer(sym), val);
		}
		return val;
	}

	/**
	 * initialize the entry pointed to by ptr
	 */
	private void io_newstentry(stentry ptr, int num)
	{
		ptr.st_symnumber = num;
		ptr.st_expanded = false;
		ptr.st_frozen = false;
		ptr.st_defined = false;
		ptr.st_dumped = false;		// added for winstanley
		ptr.st_ncalls = 0;		// ditto
		ptr.st_bbvalid = false;
		ptr.st_name = null;
		ptr.st_guts = null;
	}

	private void io_applylocal(tmatrix tm)
	{
		io_assign(tm, io_cifstacktop);
	}

	private void io_increfcount(int id)
	{
		if (id >= CONTSIZE)
		{
			io_report("illegal context: io_increfcount", FATALINTERNAL);
			return;
		}
		io_cifcarray[id].refcount++;
	}

	/**
	 * allocate and free item nodes, come not from interpreter storage,
	 * since they don't need to be saved with compiled stuff.
	 */
	private itementry io_newnode()		// return a new itementry
	{
		return new itementry();
	}

	private void io_scalepath(path src, path dest)
	{
		int limit = io_pathlength(src);
		for (int i = 0; i < limit; i++)
		{
			Point temp = io_removepoint(src);

			temp.x = (int)(io_cifscalefactor * temp.x);
			temp.y = (int)(io_cifscalefactor * temp.y);
			if (io_appendpoint(dest, temp)) break;
		}
	}

	private void io_copypath(path src, path dest)
	{
		linkedpoint temp = src.pfirst;
		if (src == dest) return;
		while (temp != null)
		{
			if (io_appendpoint(dest, temp.pvalue)) break;
			temp = temp.pnext;
		}
	}

	private Rectangle io_bbpolygon(path ppath)
	{
		int limit = io_pathlength(ppath);

		io_pushtrans();	// newtrans
		io_initmm(io_transpoint(io_removepoint(ppath)));
		for (int i = 1; i < limit; i++)
		{
			io_minmax(io_transpoint(io_removepoint(ppath)));
		}
		io_poptrans();
		Rectangle ret = new Rectangle(io_minx(), io_miny(), io_maxx()-io_minx(), io_maxy()-io_miny());
		io_donemm();
		return ret;
	}

	private void io_minmax(Point foo)
	{
		if (foo.x > io_cifmmright[io_cifmmptr]) io_cifmmright[io_cifmmptr] = foo.x;
			else {if (foo.x < io_cifmmleft[io_cifmmptr]) io_cifmmleft[io_cifmmptr] = foo.x;}
		if (foo.y > io_cifmmtop[io_cifmmptr]) io_cifmmtop[io_cifmmptr] = foo.y;
			else {if (foo.y < io_cifmmbottom[io_cifmmptr]) io_cifmmbottom[io_cifmmptr] = foo.y;}
	}

	private void io_initmm(Point foo)
	{
		if (++io_cifmmptr >= MAXMMSTACK)
		{
			io_report("io_initmm: out of stack", FATALINTERNAL);
			return;
		}
		io_cifmmleft[io_cifmmptr] = foo.x;   io_cifmmright[io_cifmmptr] = foo.x;
		io_cifmmbottom[io_cifmmptr] = foo.y; io_cifmmtop[io_cifmmptr] = foo.y;
	}

	private void io_donemm()
	{
		if (io_cifmmptr < 0) io_report("io_donemm: pop from empty stack", FATALINTERNAL);
			else io_cifmmptr--;
	}

	private int io_minx()
	{
		return(io_cifmmleft[io_cifmmptr]);
	}

	private int io_miny()
	{
		return(io_cifmmbottom[io_cifmmptr]);
	}

	private int io_maxx()
	{
		return(io_cifmmright[io_cifmmptr]);
	}

	private int io_maxy()
	{
		return(io_cifmmtop[io_cifmmptr]);
	}

	private void io_pushtrans()
	{
		if (io_cifstacktop.next == null)
		{
			io_cifstacktop.next = new tmatrix();
			io_clear(io_cifstacktop.next);
			io_cifstacktop.next.prev = io_cifstacktop;
			io_cifstacktop = io_cifstacktop.next;
			io_cifstacktop.next = null;
		} else
		{
			io_cifstacktop = io_cifstacktop.next;
			io_clear(io_cifstacktop);
		}
	}

	private void io_poptrans()
	{
		if (io_cifstacktop.prev != null) io_cifstacktop = io_cifstacktop.prev;
			else io_report("pop, empty trans stack", FATALINTERNAL);
	}

	private Point io_transpoint(Point foo)
	{
		Point ans = new Point();

		if (!io_cifstacktop.multiplied)
		{
			io_matmult(io_cifstacktop, io_cifstacktop.prev, io_cifstacktop);
		}
		switch (io_cifstacktop.type)
		{
			case TIDENT:
				return(foo);
			case TTRANSLATE:
				ans.x = (int)io_cifstacktop.a31;
				ans.y = (int)io_cifstacktop.a32;
				ans.x += foo.x;   ans.y += foo.y;
				return(ans);
			case TMIRROR:
				ans.x = (io_cifstacktop.a11 < 0) ? -foo.x : foo.x;
				ans.y = (io_cifstacktop.a22 < 0) ? -foo.y : foo.y;
				return(ans);
			case TROTATE:
				ans.x = (int)(((float) foo.x)*io_cifstacktop.a11+((float) foo.y)*io_cifstacktop.a21);
				ans.y = (int)(((float) foo.x)*io_cifstacktop.a12+((float) foo.y)*io_cifstacktop.a22);
				return(ans);
			default:
				ans.x = (int)(io_cifstacktop.a31 + ((float) foo.x)*io_cifstacktop.a11+
					((float) foo.y)*io_cifstacktop.a21);
				ans.y = (int)(io_cifstacktop.a32 + ((float) foo.x)*io_cifstacktop.a12+
					((float) foo.y)*io_cifstacktop.a22);
		}
		return(ans);
	}

	private void io_matmult(tmatrix l, tmatrix r, tmatrix result)
	{
		if (l == null || r == null || result == null)
			io_report("null arg to io_matmult", FATALINTERNAL);
		if (result.multiplied)
		{
			io_report("can't re-mult tmatrix", FATALINTERNAL);
			return;
		}
		if (!r.multiplied)
		{
			tmatrix temp = new tmatrix();
			temp.multiplied = false;
			io_matmult(r, r.prev, temp);
			io_mmult(l, temp, result);
		} else io_mmult(l, r, result);
	}

	private void io_mmult(tmatrix l, tmatrix r, tmatrix result)
	{
		if (l == null || r == null || result == null)
		{
			io_report("null arg to io_mmult", FATALINTERNAL);
			return;
		}
		if (l.type == TIDENT) io_assign(r, result); else
			if (r.type == TIDENT) io_assign(l, result); else
		{
			tmatrix temp = new tmatrix();
			temp.a11 = l.a11 * r.a11 + l.a12 * r.a21;
			temp.a12 = l.a11 * r.a12 + l.a12 * r.a22;
			temp.a21 = l.a21 * r.a11 + l.a22 * r.a21;
			temp.a22 = l.a21 * r.a12 + l.a22 * r.a22;
			temp.a31 = l.a31 * r.a11 + l.a32 * r.a21 + l.a33 * r.a31;
			temp.a32 = l.a31 * r.a12 + l.a32 * r.a22 + l.a33 * r.a32;
			temp.a33 = l.a33*r.a33;
			temp.type = l.type | r.type;
			io_assign(temp, result);
		}
		if (result.a33 != 1.0)
		{
			// divide by a33
			result.a11 /= result.a33;
			result.a12 /= result.a33;
			result.a21 /= result.a33;
			result.a22 /= result.a33;
			result.a31 /= result.a33;
			result.a32 /= result.a33;
			result.a33 = 1.0;
		}
		result.multiplied = true;
	}

	private void io_assign(tmatrix src, tmatrix dest)
	{
		dest.a11 = src.a11;
		dest.a12 = src.a12;
		dest.a21 = src.a21;
		dest.a22 = src.a22;
		dest.a31 = src.a31;
		dest.a32 = src.a32;
		dest.a33 = src.a33;
		dest.type = src.type;
		dest.multiplied = src.multiplied;
	}

	private Point io_removepoint(path a)
	{
		if (a.pfirst == null)
		{
			// added code to initialize return value with dummy numbers
			return new Point(0, 0);
		}
		linkedpoint temp = a.pfirst.pnext;
		Point ans = a.pfirst.pvalue;
		a.pfirst = temp;
		if (a.pfirst == null) a.plast = null;
		a.plength -= 1;
		return ans;
	}

	private void io_iinstname(String name)
	{
		if (io_cifignore) return;
		if (name.length() == 0)
		{
			io_report("null instance name ignored", ADVISORY);
			return;
		}
		if (io_cifnamepending)
		{
			io_report("there is already a name pending, new name replaces it", ADVISORY);
		}
		io_cifnamepending = true;
		io_cifstatesince = 0;
		io_cifnstored = name;
	}

	private void io_iend()
	{
		io_cifstatesince++;
		io_cifendcom = true;
		if (io_cifnamepending)
		{
			io_report("no instance to match name command", ADVISORY);
			io_cifnamepending = false;
		}
	}

	private int io_signed()
	{
		boolean sign = false;
		int ans = 0;
		io_sep();

		if (io_peek() == '-') {sign = true; io_getch();}

		boolean somedigit = false;
		while (ans < Integer.MAX_VALUE && Character.isDigit((char)io_peek()))
		{
			ans *= 10; ans += io_getch() - '0';
			somedigit = true;
		}

		if (!somedigit) { io_logit(NOSIGNED); return(0);}
		if (Character.isDigit((char)io_peek()))
		{
			io_logit(NUMTOOBIG);
			return(sign ? -0X7FFFFFFF:0X7FFFFFFF);
		}
		return(sign ? -ans:ans);
	}

	private void io_logit(int thing)
	{
		io_ciferrorfound = true;
		io_ciferrortype = thing;
	}

	private Point io_getpoint()
	{
		int x = io_signed();
		int y = io_signed();
		Point ans = new Point(x, y);
		return ans;
	}

	private path io_makepath()
	{
		path a = new path();
		a.pfirst = null;
		a.plast = null;
		a.plength = 0;
		return a;
	}

	private void io_getpath(path a)
	{
		io_sep();

		for(;;)
		{
			int c = io_peek();
			if (!Character.isDigit((char)c) && c != '-') break;
			Point temp = io_getpoint();	// hack because of compiler error
			if (io_appendpoint(a, temp)) break;
			io_sep();
		}
		if (io_pathlength(a) == 0) io_logit(NOPATH);
	}

	private int io_pathlength(path a)
	{
		return a.plength;
	}

	/**
	 * returns true on memory error
	 */
	private boolean io_appendpoint(path a, Point p)
	{
		linkedpoint temp;

		temp = a.plast;
		a.plast = io_getpnode();
		if (a.plast == null) return true;
		if (temp != null) temp.pnext = a.plast;
		a.plast.pvalue = p;
		a.plast.pnext = null;
		if (a.pfirst == null) a.pfirst = a.plast;
		a.plength += 1;
		return false;
	}

	private linkedpoint io_getpnode()
	{
		return new linkedpoint();
	}

	private void io_sep()
	{
		for(;;)
		{
			int c = io_peek();
			switch (c)
			{
				case '(':
				case ')':
				case ';':
				case '-':
				case -1:
					return;
				default:
					if (Character.isDigit((char)c)) return;
					io_getch();
			}
		}
	}

	private int reportError()
	{
		switch (io_ciferrortype)
		{
			case NUMTOOBIG:  io_report("number too large", FATALSYNTAX); break;
			case NOUNSIGNED: io_report("unsigned integer expected", FATALSYNTAX); break;
			case NOSIGNED:   io_report("signed integer expected", FATALSYNTAX); break;
			case NOSEMI:     io_report("missing ';' inserted", FATALSYNTAX); break;
			case NOPATH:     io_report("no points in path", FATALSYNTAX); break;
			case BADTRANS:   io_report("no such transformation command", FATALSYNTAX); break;
			case BADUSER:    io_report("end of file inside user command", FATALSYNTAX); break;
			case BADCOMMAND: io_report("unknown command encountered", FATALSYNTAX); break;
			case INTERNAL:   io_report("parser can't find i routine", FATALINTERNAL); break;
			case BADDEF:     io_report("no such define command", FATALSYNTAX); break;
			case NOLAYER:    io_report("layer name expected", FATALSYNTAX); break;
			case BADCOMMENT: io_report("end of file inside a comment", FATALSYNTAX); break;
			case BADAXIS:    io_report("no such axis in mirror command", FATALSYNTAX); break;
			case NESTDEF:    io_report("symbol definitions can't nest", FATALSYNTAX); break;
			case NODEFSTART: io_report("DF without DS", FATALSYNTAX); break;
			case NESTDD:     io_report("DD can't appear inside symbol definition", FATALSYNTAX); break;
			case NOSPACE:    io_report("missing space in name command", FATALSYNTAX); break;
			case NONAME:     io_report("no name in name command", FATALSYNTAX); break;
			case NESTEND:    io_report("End command inside symbol definition", FATALSYNTAX); break;
			case NOERROR:    io_report("error signaled but not reported", FATALINTERNAL); break;
			default:         io_report("uncaught error", FATALSYNTAX);
		}
		if (io_ciferrortype != INTERNAL && io_ciferrortype != NOSEMI && io_flush(';') < 0)
			io_report("unexpected end of input file", FATALSYNTAX);
				else io_blank();
		io_ciferrorfound = false;
		io_ciferrortype = NOERROR;
		return SYNTAXERROR;
	}

	private void io_report(String mess, int kind)
	{
		io_identify();
		io_cifecounts[kind]++;

		switch (kind)
		{
			case FATALINTERNAL: System.out.println("Fatal internal error: " + mess);  break;
			case FATALSYNTAX:   System.out.println("Syntax error: " + mess);          break;
			case FATALSEMANTIC: System.out.println("Error: " + mess);                 break;
			case FATALOUTPUT:   System.out.println("Output error: " + mess);          break;
			case ADVISORY:      System.out.println("Warning: " + mess);               break;
			default:            System.out.println(mess);                             break;
		}

//		if (kind == FATALINTERNAL) longjmp(io_filerror, 1);
	}

	private void io_identify()
	{
		if (io_cifcharcount > 0)
		{
			System.out.println("line " + (io_ciflinecount-(io_cifresetbuffer?1:0)) + ": " + io_ciflinebuffer.toString());
		}
	}
}
