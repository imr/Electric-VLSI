/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: CIF.java
 * Input/output tool: CIF input
 * Original C CIF Parser (front end) by Robert W. Hon, Schlumberger Palo Alto Research
 * and its interface to C Electric (back end) by Robert Winstanley, University of Calgary.
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

import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.Orientation;
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
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

/**
 * This class reads files in CIF files.
 */
public class CIF extends Input
{
	/** max depth of minmax stack */		private static final int MAXMMSTACK = 50;
	/** max value that can add extra digit */private static final int BIGSIGNED = ((0X7FFFFFFF-9)/10);

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

	// types for FrontTransformLists
	static class FrontTransformType {}
	private FrontTransformType MIRROR = new FrontTransformType();
	private FrontTransformType TRANSLATE = new FrontTransformType();
	private FrontTransformType ROTATE = new FrontTransformType();

	// error codes for reporting errors
	private static final int FATALINTERNAL = 0;
	private static final int FATALSYNTAX   = 1;
	private static final int FATALSEMANTIC = 2;
	private static final int FATALOUTPUT   = 3;
	private static final int ADVISORY      = 4;
	private static final int OTHER         = 5;			/* OTHER must be last */

	private static final int TIDENT     = 0;
	private static final int TROTATE    = 1;
	private static final int TTRANSLATE = 2;
	private static final int TMIRROR    = 4;

	// values for BackCIFList->identity
	private static final int CSTART =   0;
	private static final int CEND =     1;
	private static final int CWIRE =    2;
	private static final int CFLASH =   3;
	private static final int CBOX =     4;
	private static final int CPOLY =    5;
	private static final int CCOMMAND = 6;
	private static final int CGNAME =   7;
	private static final int CLABEL =   8;
	private static final int CCALL =    9;

	static class BackCIFCell
	{
		/** cell index given in the define statement */	int  cIndex;
		/** bounding box of cell */						int  l, r, t, b;
		/** the address of the cif cell */				Cell addr;
	};

	static class BackCIFList
	{
		/** specifies the nature of the entry */	int         identity;
		/** will point to member's structure */		Object      member;
		/** next entry in list */					BackCIFList next;
	};

	static class BackCIFStart
	{
		/** cell index */							int    cIndex;
		/** cell name */							String name;
		/** bounding box of cell */					int    l, r, t, b;
	};

	static class BackCIFBox
	{
		/** the corresponding layer number */		Layer lay;
		/** dimensions of box */					int   length, width;
		/** center point of box */					int   cenX, cenY;
		/** box direction */						int   xRot, yRot;
	};

	static class BackCIFPoly
	{
		/** the corresponding layer number */		Layer  lay;
		/** list of points */						int [] x, y;
		/** number of points in list */				int    lim;
	};

	static class BackCIFGeomName
	{
		/** the corresponding layer number */		Layer  lay;
	};

	static class BackCIFLabel
	{
		/** location of label */					int    x, y;
		/** the label */							String label;
	};

	static class BackCIFCall
	{
		/** index of cell called */					int              cIndex;
		/** name of cell called */					String           name;
		/** list of transformations */				BackCIFTransform list;
	};

	// values for the transformation type
	/** mirror in x */	private static final int MIRX  = 1;
	/** mirror in y */	private static final int MIRY  = 2;
	/** translation */	private static final int TRANS = 3;
	/** rotation */		private static final int ROT   = 4;

	static class BackCIFTransform
	{
		/** type of transformation */				int              type;
		/** not required for the mirror types */	int              x, y;
		/** next element in list */					BackCIFTransform next;
	};

	static class FrontTransformEntry
	{
		FrontTransformType kind;
		boolean            xCoord;
		int                xt, yt;
		int                xRot, yRot;
	};

	/** data types for transformation package */
	static class FrontMatrix
	{
		double a11, a12, a21, a22, a31, a32, a33;
		FrontMatrix prev, next;
		int type;
		boolean multiplied;
	};

	/** bounding box */
	static class FrontBBox
	{
		int l, r, b, t;
	};

	static class FrontSymbol
	{
		/** symbol number for this entry */					int symNumber;
		boolean expanded;
		boolean defined;
		boolean dumped;
		/** bb as if this symbol were called by itself */	FrontBBox bounds;
		/** flag for rebuilding bounding box */				boolean boundsValid;
		/** name of this symbol */							String name;
		/** number of calls made by this symbol */			int numCalls;
		/** pointer to linked list of objects */			FrontObjBase guts;
		FrontSymbol(int num)
		{
			bounds = new FrontBBox();
			symNumber = num;
			expanded = false;
			defined = false;
			dumped = false;
			numCalls = 0;
			boundsValid = false;
			name = null;
			guts = null;
		}
	};

	static class FrontLinkedPoint
	{
		Point pValue;
		FrontLinkedPoint pNext;
	};

	static class FrontPath
	{
		FrontLinkedPoint pFirst, pLast;
		int pLength;

		FrontPath()
		{
			pFirst = null;
			pLast = null;
			pLength = 0;
		}
	};

	static class FrontLinkedTransform
	{
		FrontTransformEntry tValue;
		FrontLinkedTransform tNext;
	};

	static class FrontTransformList
	{
		FrontLinkedTransform tFirst, tLast;
		int tLength;

		FrontTransformList()
		{
			tFirst = null;
			tLast = null;
			tLength = 0;
		}
	};

	/** items in item tree */
	static class FrontItem
	{
		/** links for tree */					FrontItem same;
		/** pointer into symbol structure */	FrontObjBase   what;
	};

	/** hack structure for referencing first fields of any object */
	static class FrontObjBase
	{
		/** bounding box */				FrontBBox bb;
		/** for ll */					FrontObjBase  next;
		/** layer for this object */	Layer layer;

		FrontObjBase()
		{
			bb = new FrontBBox();
		}
	};

	/** symbol call object */
	static class FrontCall extends FrontObjBase
	{
		/** rest is noncritical */				int symNumber;
		FrontSymbol unID;
		FrontMatrix matrix;
		/** trans list for this call */			FrontTransformList transList;
	};

	static class FrontGeomName extends FrontObjBase
	{
	};

	static class FrontLabel extends FrontObjBase
	{
		String name;
		Point pos;
	};

	static class FrontBox extends FrontObjBase
	{
		int length, width;
		Point center;
		int xRot, yRot;
	};

	static class FrontManBox extends FrontObjBase
	{
	};

	static class FrontFlash extends FrontObjBase
	{
		Point center;
		int diameter;
	};

	static class FrontPoly extends FrontObjBase
	{
		/** array of points in path */			Point [] points;
	};

	static class FrontWire extends FrontObjBase
	{
		/** width of wire */					int width;
		/** array of points in wire */			Point [] points;
	};

	/** current transformation */			private BackCIFTransform currentCTrans;
	/** head of the list */					private BackCIFList      currentFrontList = null;
	/** current location in list */			private BackCIFList      currentFrontElement;
	/** head of item list */				private FrontItem        currentItemList;
	/** A/B from DS */						private double           cellScaleFactor;
	/** current symbol being defined */		private FrontSymbol      currentFrontSymbol;
	/** place to save layer during def */	private Layer            backupLayer;
	/** symbol has been named */			private boolean          symbolNamed;
	/** flag for error encountered */		private boolean          errorFound;
	/** what it was */						private int              errorType;
	/** definition in progress flag */		private boolean          isInCellDefinition;
	/** end command flag */					private boolean          endIsSeen;
	/** number of chars in buffer */		private int              charactersRead;
	/** flag to reset buffer */				private boolean          resetInputBuffer;
	/** number of "fatal" errors */			private int              numFatalErrors;
	/** null layer errors encountered */	private boolean          numNullLayerErrors;
	/** ignore statements until DF */		private boolean          ignoreStatements;
	/** 91 pending */						private boolean          namePending;
	/** end command flag */					private boolean          endCommandFound;
	/** current layer */					private Layer            currentLayer;
	/** symbol table */						private HashMap<Integer,FrontSymbol> symbolTable;
	/** the top of stack */					private FrontMatrix      matrixStackTop;
	/** lookahead character */				private int              nextInputCharacter;
	/** # statements since 91 com */		private boolean          statementsSince91;
	/** min/max stack pointer */			private int              minMaxStackPtr;
	/** min/max stack: left edge */			private int []           minMaxStackLeft;
	/** min/max stack: right edge */		private int []           minMaxStackRight;
	/** min/max stack: bottom edge */		private int []           minMaxStackBottom;
	/** min/max stack: top edge */			private int []           minMaxStackTop;
	/** map from cell numbers to cells */	private HashMap<Integer,BackCIFCell> cifCellMap;
	/** the current cell */					private BackCIFCell      currentBackCell;
	/** current technology for layers */	private Technology       curTech;
	/** map from layer names to layers */	private HashMap<String,Layer> cifLayerNames;
	/** set of unknown layers */			private HashSet<String>  unknownLayerNames;
	/** address of cell being defined */	private Cell             cellBeingBuilt;
	/** name of the current cell */			private String           currentNodeProtoName;
	/** the line being read */				private StringBuffer     inputBuffer;

	/**
	 * Method to import a library from disk.
	 * @param lib the library to fill
	 * @return true on error.
	 */
	protected boolean importALibrary(Library lib)
	{
        setProgressNote("Reading CIF file");

        // initialize all lists and the searching routines
		cifCellMap = new HashMap<Integer,BackCIFCell>();

		if (initFind()) return true;

		// parse the cif and create a listing
		if (interpret()) return true;

		// instantiate the cif as nodes
        setProgressNote("Storing CIF in database...");
		if (listToNodes(lib)) return true;

		// clean up
		doneInterpreter();

		return false;
	}

	private boolean listToNodes(Library lib)
	{
		cellBeingBuilt = null;
		for(currentFrontElement = currentFrontList; currentFrontElement != null; currentFrontElement = currentFrontElement.next)
		{
			if (currentFrontElement.identity == CSTART)
			{
				cellBeingBuilt = nodesStart(lib);
				if (cellBeingBuilt == null) return true;
				continue;
			}
			if (currentFrontElement.identity == CEND)
			{
//				lib.setCurCell(cellBeingBuilt);		// THIS TAKES WAY TOO LONG
				cellBeingBuilt = null;
				continue;
			}
			if (cellBeingBuilt == null)
			{
				// circuitry found at the top level: create a fake cell for it
				cellBeingBuilt = lib.findNodeProto("TOP_LEVEL_UNNAMED{lay}");
				if (cellBeingBuilt == null)
				{
					cellBeingBuilt = Cell.newInstance(lib, "TOP_LEVEL_UNNAMED{lay}");
					if (cellBeingBuilt == null) break;
					currentBackCell = makeBackCIFCell(9999);
				}
				currentBackCell.addr = cellBeingBuilt;
			}
			if (currentFrontElement.identity == CBOX)
			{
				if (nodesBox()) return true;
			} else if (currentFrontElement.identity == CPOLY)
			{
				if (nodesPoly()) return true;
			} else if (currentFrontElement.identity == CCALL)
			{
				if (nodesCall()) return true;
			}
		}
		return false;
	}

	private Cell nodesStart(Library lib)
	{
		BackCIFStart cs = (BackCIFStart)currentFrontElement.member;
		BackCIFCell cifCell = makeBackCIFCell(cs.cIndex);
		cifCell.l = cs.l;   cifCell.r = cs.r;
		cifCell.b = cs.b;   cifCell.t = cs.t;
		currentNodeProtoName = cs.name;

		// remove illegal characters
		StringBuffer properName = new StringBuffer();
		for(int i=0; i<currentNodeProtoName.length(); i++)
		{
			char chr = currentNodeProtoName.charAt(i);
			if (Character.isWhitespace(chr) || chr == ':' || chr == ';')
			{
				chr = 'X';
			}
			properName.append(chr);
		}
		currentNodeProtoName = properName.toString();
		cifCell.addr = Cell.newInstance(lib, currentNodeProtoName + "{lay}");
		if (cifCell.addr == null)
		{
			System.out.println("Cannot create the cell " + currentNodeProtoName);
			return null;
		}
		return cifCell.addr;
	}

	private boolean nodesCall()
	{
		BackCIFCall cc = (BackCIFCall)currentFrontElement.member;
		BackCIFCell cell = findBackCIFCell(cc.cIndex);
		if (cell == null)
		{
			System.out.println("Referencing an undefined cell");
			return true;
		}
		int rot = 0;
		boolean trans = false;
		int l = cell.l;    int r = cell.r;    int b = cell.b;    int t = cell.t;
		for(BackCIFTransform ctrans = cc.list; ctrans != null; ctrans = ctrans.next)
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
					int cenX = (l+r)/2;   int cenY = (b+t)/2;
					Point pt = new Point(cenX, cenY);
					rotateLayer(pt, deg);
					cenX = pt.x;   cenY = pt.y;
					l = cenX - hlen;   r = cenX + hlen;
					b = cenY - hwid;   t = cenY + hwid;
					rot += ((trans) ? -deg : deg);
				}
		}
		while (rot >= 3600) rot -= 3600;
		while (rot < 0) rot += 3600;
		double lX = convertFromCentimicrons(l);
		double hX = convertFromCentimicrons(r);
		double lY = convertFromCentimicrons(b);
		double hY = convertFromCentimicrons(t);
		double x = (lX + hX) / 2;
		double y = (lY + hY) / 2;
		double sX = hX - lX;
		double sY = hY - lY;
		Rectangle2D bounds = cell.addr.getBounds();
		sX = bounds.getWidth();
		sY = bounds.getHeight();

		// transform is rotation and transpose: convert to rotation/MX/MY
		Orientation or = Orientation.fromC(rot, trans);
//		if (or.isXMirrored()) sX = -sX;
//		if (or.isYMirrored()) sY = -sY;
//		rot = or.getAngle();

		// special code to account for rotation of cell centers
		AffineTransform ctrTrans = or.rotateAbout(x, y);
//		AffineTransform ctrTrans = NodeInst.rotateAbout(rot, x, y, sX, sY);
		Point2D spin = new Point2D.Double(x - bounds.getCenterX(), y - bounds.getCenterY());
		ctrTrans.transform(spin, spin);
		x = spin.getX();   y = spin.getY();

		// create the node
		NodeInst ni = NodeInst.makeInstance(cell.addr, new Point2D.Double(x, y), sX, sY, currentBackCell.addr, or, null, 0);
//		NodeInst ni = NodeInst.makeInstance(cell.addr, new Point2D.Double(x, y), sX, sY, currentBackCell.addr, rot, null, 0);
		if (ni == null)
		{
			System.out.println("Problems creating an instance of " + cell.addr + " in " + currentBackCell.addr);
			return true;
		}
		return false;
	}

	private boolean nodesPoly()
	{
		BackCIFPoly cp = (BackCIFPoly)currentFrontElement.member;
		if (cp.lim == 0) return false;
		NodeProto np = findPrototype(cp.lay);
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
		double cmCX = (lx + hx) / 2;
		double cmCY = (ly + hy) / 2;

		// convert from centimicrons
		double lowX = convertFromCentimicrons(lx);
		double highX = convertFromCentimicrons(hx);
		double lowY = convertFromCentimicrons(ly);
		double highY = convertFromCentimicrons(hy);
		double x = (lowX + highX) / 2;
		double y = (lowY + highY) / 2;
		double sX = highX - lowX;
		double sY = highY - lowY;
		NodeInst newni = NodeInst.makeInstance(np, new Point2D.Double(x, y), sX, sY, currentBackCell.addr);
		if (newni == null)
		{
			System.out.println("Problems creating a polygon on layer " + cp.lay + " in " + currentBackCell.addr);
			return true;
		}

		// store the trace information
		EPoint [] points = new EPoint[cp.lim];
		for(int i=0; i<cp.lim; i++)
		{
			points[i] = new EPoint(convertFromCentimicrons(cp.x[i]-cmCX), convertFromCentimicrons(cp.y[i]-cmCY));
		}

		// store the trace information
		newni.newVar(NodeInst.TRACE, points);

		return false;
	}

	private void rotateLayer(Point pt, int deg)
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

	private void outputPolygon(Layer lay, FrontPath pPath)
	{
		int lim = pPath.pLength;
		if (lim < 3) return;

		placeCIFList(CPOLY);
		BackCIFPoly cp = (BackCIFPoly)currentFrontElement.member;
		cp.lay = lay;
		cp.x = new int[lim];
		cp.y = new int[lim];

		cp.lim = lim;
		for (int i = 0; i < lim; i++)
		{
			Point temp = removePoint(pPath);
			cp.x[i] = temp.x;
			cp.y[i] = temp.y;
		}
	}

	private double convertFromCentimicrons(double v)
	{
		return TextUtils.convertFromDistance(v/100, curTech, TextUtils.UnitScale.MICRO);
	}

	private boolean nodesBox()
	{
		BackCIFBox cb = (BackCIFBox)currentFrontElement.member;
		NodeProto node = findPrototype(cb.lay);
		if (node == null)
		{
			String layname = cb.lay.getName();
			System.out.println("Cannot find primitive to use for layer '" + layname + "' (number " + cb.lay + ")");
			return true;
		}
		int r = GenMath.figureAngle(new Point2D.Double(0, 0), new Point2D.Double(cb.xRot, cb.yRot));
		double x = convertFromCentimicrons(cb.cenX);
		double y = convertFromCentimicrons(cb.cenY);
		double len = convertFromCentimicrons(cb.length);
		double wid = convertFromCentimicrons(cb.width);
        Orientation orient = Orientation.fromAngle(r);
		NodeInst ni = NodeInst.makeInstance(node, new Point2D.Double(x, y), len, wid, currentBackCell.addr, orient, null, 0);
//		NodeInst ni = NodeInst.makeInstance(node, new Point2D.Double(x, y), len, wid, currentBackCell.addr, r, null, 0);
		if (ni == null)
		{
			String layname = cb.lay.getName();
			System.out.println("Problems creating a box on layer " + layname + " in " + currentBackCell.addr);
			return true;
		}
		return false;
	}

	private boolean interpret()
	{
		initParser();
		initInterpreter();
		inFromFile();
		int comcount = parseFile();		// read in the cif
		doneParser();

		if (numFatalErrors > 0) return true;
		if (unknownLayerNames.size() > 0)
		{
			System.out.println("Error: these layers appear in the CIF file but are not assigned to Electric layers:");
			for(String str : unknownLayerNames)
			{
				System.out.println("    " + str);
			}
		}

		Rectangle box = getInterpreterBounds();

		// construct a list: first step in the conversion
		createList();
		return false;
	}

	private BackCIFCell findBackCIFCell(int cIndex)
	{
		return cifCellMap.get(new Integer(cIndex));
	}

	private BackCIFCell makeBackCIFCell(int cIndex)
	{
		BackCIFCell newCC = new BackCIFCell();
		newCC.addr = null;
		newCC.cIndex = cIndex;
		cifCellMap.put(new Integer(newCC.cIndex), newCC);

		currentBackCell = newCC;
		return newCC;
	}

	private NodeProto findPrototype(Layer lay)
	{
		return lay.getNonPseudoLayer().getPureLayerNode();
	}

	private boolean initFind()
	{
		// get the array of CIF names
		cifLayerNames = new HashMap<String,Layer>();
		unknownLayerNames = new HashSet<String>();
		boolean valid = false;
		curTech = Technology.getCurrent();
		for(Iterator<Layer> it = curTech.getLayers(); it.hasNext(); )
		{
			Layer layer = it.next();
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

	private void initInterpreter()
	{
		numNullLayerErrors = false;
		isInCellDefinition = false;
		ignoreStatements = false;
		namePending = false;
		endCommandFound = false;
		currentLayer = null;
		currentItemList = null;
		initUtilities();
		initMatrices();
	}

	private void initParser()
	{
		errorFound = false;
		errorType = NOERROR;
		isInCellDefinition = false;
		endIsSeen = false;
		initInput();
		initErrors();
	}

	private void doneParser()
	{
		if (!endIsSeen) errorReport("missing End command", FATALSYNTAX);
	}

	private int parseFile()
	{
		int comCount = 1;
		for(;;)
		{
			int com = parseStatement();
			if (com == END || com == ENDFILE) break;
			comCount++;
		}
		return comCount;
	}

	private void doneInterpreter()
	{
		if (numNullLayerErrors)
		{
			System.out.println("Warning: some CIF objects were not read");
		}
	}

	private Rectangle getInterpreterBounds()
	{
		FrontItem h = currentItemList;
		boolean first = true;

		if (h == null)
		{
			errorReport("item list is empty!", ADVISORY);
			return null;
		}

		pushTransform();
		while (h != null)
		{
			FrontObjBase obj = h.what;
			Point temp = new Point();
			temp.x = obj.bb.l;
			temp.y = obj.bb.b;
			Point comperror = transformPoint(temp);
			initMinMax(comperror);
			temp.x = obj.bb.r;
			comperror = transformPoint(temp);
			minMax(comperror);
			temp.y = obj.bb.t;
			comperror = transformPoint(temp);
			minMax(comperror);
			temp.x = obj.bb.l;
			comperror = transformPoint(temp);
			minMax(comperror);

			int left = getMinMaxMinX();
			int right = getMinMaxMaxX();
			int bottom = getMinMaxMinY();
			int top = getMinMaxMaxY();
			doneMinMax();
			temp.x = left;   temp.y = bottom;
			if (first) {first = false; initMinMax(temp);} else minMax(temp);
			temp.x = right;   temp.y = top;
			minMax(temp);
			h = h.same;
		}
		Rectangle ret = new Rectangle(getMinMaxMinX(), getMinMaxMinY(),
			getMinMaxMaxX()-getMinMaxMinX(), getMinMaxMaxY()-getMinMaxMinY());
		doneMinMax();
		popTransform();
		return ret;
	}

	private void createList()
	{
		if (!isEndSeen()) System.out.println("missing End command, assumed");
		if (numFatalErrors > 0) return;
		sendList(currentItemList);		// sendlist deletes nodes
		currentItemList = null;
	}

	private void sendList(FrontItem list)
	{
		FrontItem h = list;
		while (h != null)
		{
			FrontItem save = h.same;
			outItem(h);
			h = save;
		}
	}

	/**
	 * spit out an item
	 */
	private void outItem(FrontItem thing)
	{
		if (thing.what instanceof FrontPoly)
		{
			FrontPoly po = (FrontPoly)thing.what;
			FrontPath pPath = new FrontPath();
			for (int i = 0; i < po.points.length; i++)
				appendPoint(pPath, po.points[i]);
			outputPolygon(po.layer, pPath);
			return;
		}
		if (thing.what instanceof FrontWire)
		{
			FrontWire wi = (FrontWire)thing.what;
			FrontPath pPath = new FrontPath();
			for (int i = 0; i < wi.points.length; i++)
				appendPoint(pPath, wi.points[i]);
			outputWire(wi.layer, wi.width, pPath);
			return;
		}
		if (thing.what instanceof FrontFlash)
		{
			FrontFlash fl = (FrontFlash)thing.what;
			outputFlash(fl.layer, fl.diameter, fl.center);
			return;
		}
		if (thing.what instanceof FrontBox)
		{
			FrontBox bo = (FrontBox)thing.what;
			outputBox(bo.layer, bo.length, bo.width, bo.center, bo.xRot, bo.yRot);
			return;
		}
		if (thing.what instanceof FrontManBox)
		{
			Point temp = new Point();
			FrontManBox mb = (FrontManBox)thing.what;
			temp.x = (mb.bb.r + mb.bb.l)/2;
			temp.y = (mb.bb.t + mb.bb.b)/2;
			outputBox(mb.layer, mb.bb.r-mb.bb.l, mb.bb.t-mb.bb.b, temp, 1, 0);
			return;
		}
		if (thing.what instanceof FrontCall)
		{
			pushTransform();
			FrontCall sc = (FrontCall)thing.what;
			applyLocal(sc.matrix);
			FrontTransformList tList = new FrontTransformList();
			dupTransformList(sc.transList, tList);
			dumpDefinition(sc.unID);
			outputCall(sc.symNumber, sc.unID.name, tList);
			popTransform();
			return;
		}
		if (thing.what instanceof FrontGeomName)
		{
			FrontGeomName gn = (FrontGeomName)thing.what;
			outputGeomName(gn.layer);
			return;
		}
		if (thing.what instanceof FrontLabel)
		{
			FrontLabel la = (FrontLabel)thing.what;
			outputLabel(la.name, la.pos);
			return;
		}
	}

	private void outputLabel(String name, Point pt)
	{
		placeCIFList(CLABEL);
		BackCIFLabel cl = (BackCIFLabel)currentFrontElement.member;
		cl.label = name;
		cl.x = pt.x;   cl.y = pt.y;
	}

	private void outputGeomName(Layer lay)
	{
		placeCIFList(CGNAME);
		BackCIFGeomName cg = (BackCIFGeomName)currentFrontElement.member;
		cg.lay = lay;
	}

	private void outputCall(int number, String name, FrontTransformList list)
	{
		placeCIFList(CCALL);
		BackCIFCall cc = (BackCIFCall)currentFrontElement.member;
		cc.cIndex = number;
		cc.name = name;
		cc.list = currentCTrans = null;
		for(int i = getFrontTransformListLength(list); i>0; i--)
		{
			if (newBackCIFTransform() == null) return;
			FrontTransformEntry temp = removeFrontTransformEntry(list);
			if (temp.kind == MIRROR)
			{
				if (temp.xCoord) currentCTrans.type = MIRX; else
					currentCTrans.type = MIRY;
			} else if (temp.kind == TRANSLATE)
			{
				currentCTrans.type = TRANS;
				currentCTrans.x = temp.xt;
				currentCTrans.y = temp.yt;
			} else if (temp.kind == ROTATE)
			{
				currentCTrans.type = ROT;
				currentCTrans.x = temp.xRot;
				currentCTrans.y = temp.yRot;
			}
		}
	}

	private BackCIFTransform newBackCIFTransform()
	{
		BackCIFTransform newCT = new BackCIFTransform();
		newCT.next = null;
		BackCIFCall cc = (BackCIFCall)currentFrontElement.member;
		if (cc.list == null) cc.list = newCT; else
			currentCTrans.next = newCT;
		currentCTrans = newCT;
		return newCT;
	}

	private void dumpDefinition(FrontSymbol sym)
	{
		if (sym.dumped) return;		// already done
		if (sym.numCalls > 0)			// dump all children
		{
			int count = sym.numCalls;

			FrontObjBase ro = sym.guts;
			while (ro != null && count > 0)
			{
				if (ro instanceof FrontCall)
				{
					dumpDefinition(((FrontCall)ro).unID);
					count--;
				}
				ro = ro.next;
			}
		}
		shipContents(sym);
		sym.dumped = true;
	}

	private void shipContents(FrontSymbol sym)
	{
		FrontObjBase ro = sym.guts;
		outputDefinitionStart(sym.symNumber, sym.name, sym.bounds.l, sym.bounds.r, sym.bounds.b, sym.bounds.t);
		while (ro != null)
		{
			if (ro instanceof FrontPoly)
			{
				FrontPoly po = (FrontPoly)ro;
				FrontPath pPath = new FrontPath();
				for (int i = 0; i < po.points.length; i++)
					appendPoint(pPath, po.points[i]);
				outputPolygon(po.layer, pPath);
			} else if (ro instanceof FrontWire)
			{
				FrontWire wi = (FrontWire)ro;
				FrontPath pPath = new FrontPath();
				for (int i = 0; i < wi.points.length; i++)
					appendPoint(pPath, wi.points[i]);
				outputWire(wi.layer, wi.width, pPath);
			} else if (ro instanceof FrontFlash)
			{
				FrontFlash fl = (FrontFlash)ro;
				outputFlash(fl.layer, fl.diameter, fl.center);
			} else if (ro instanceof FrontBox)
			{
				FrontBox bo = (FrontBox)ro;
				outputBox(bo.layer, bo.length, bo.width, bo.center, bo.xRot, bo.yRot);
			} else if (ro instanceof FrontManBox)
			{
				FrontManBox mb = (FrontManBox)ro;
				Point temp = new Point();
				temp.x = (((FrontManBox)ro).bb.r + ((FrontManBox) ro).bb.l)/2;
				temp.y = (((FrontManBox)ro).bb.t + ((FrontManBox) ro).bb.b)/2;
				outputBox(mb.layer, mb.bb.r-mb.bb.l, mb.bb.t-mb.bb.b, temp, 1, 0);
			} else if (ro instanceof FrontCall)
			{
				FrontCall sc = (FrontCall)ro;
				FrontTransformList tList = new FrontTransformList();
				dupTransformList(sc.transList, tList);
				outputCall(sc.symNumber, sc.unID.name, tList);
			} else if (ro instanceof FrontGeomName)
			{
				FrontGeomName gn = (FrontGeomName)ro;
				outputGeomName(gn.layer);
			} else if (ro instanceof FrontLabel)
			{
				FrontLabel la = (FrontLabel)ro;
				outputLabel(la.name, la.pos);
			}
			ro = ro.next;
		}
		outputDefinitionEnd();
	}

	private void outputDefinitionEnd()
	{
		placeCIFList(CEND);
	}

	private void outputDefinitionStart(int number, String name, int l, int r, int b, int t)
	{
		placeCIFList(CSTART);
		BackCIFStart cs = (BackCIFStart)currentFrontElement.member;
		cs.cIndex = number;
		cs.name = name;
		cs.l = l;   cs.r = r;
		cs.b = b;   cs.t = t;
	}

	private void dupTransformList(FrontTransformList src, FrontTransformList dest)
	{
		if (src == null || dest == null) return;
		FrontLinkedTransform node = src.tFirst;
		while (node != null)
		{
			appendTransformEntry(dest, node.tValue);
			node = node.tNext;
		}
	}

	private void outputBox(Layer lay, int length, int width, Point center, int xRotation, int yRotation)
	{
		if (length == 0 && width == 0) return;	// ignore null boxes
		placeCIFList(CBOX);
		BackCIFBox cb = (BackCIFBox)currentFrontElement.member;
		cb.lay = lay;
		cb.length = length;	cb.width = width;
		cb.cenX = center.x;	cb.cenY = center.y;
		cb.xRot = xRotation;	cb.yRot = yRotation;
	}

	private void placeCIFList(int id)
	{
		BackCIFList cl = newBackCIFList(id);
		if (cl == null) return;
		if (currentFrontList == null) currentFrontList = currentFrontElement = cl; else
		{
			while(currentFrontElement.next != null)
				currentFrontElement = currentFrontElement.next;
			currentFrontElement.next = cl;
			currentFrontElement = currentFrontElement.next;
		}
	}

	private BackCIFList newBackCIFList(int id)
	{
		BackCIFList newCL = new BackCIFList();
		newCL.next = null;
		newCL.identity = id;
		switch (id)
		{
			case CSTART:
				BackCIFStart cs = new BackCIFStart();
				newCL.member = cs;
				cs.name = null;
				break;
			case CBOX:
				newCL.member = new BackCIFBox();
				break;
			case CPOLY:
				newCL.member = new BackCIFPoly();
				break;
			case CGNAME:
				newCL.member = new BackCIFGeomName();
				break;
			case CLABEL:
				newCL.member = new BackCIFLabel();
				break;
			case CCALL:
				BackCIFCall cc = new BackCIFCall();
				newCL.member = cc;
				cc.name = null;
				break;
		}
		return newCL;
	}

	private void outputFlash(Layer lay, int diameter, Point center)
	{
		// flash approximated by an octagon
		int radius = diameter/2;
		double fCX = center.x;
		double fCY = center.y;
		double offset = ((diameter)/2.0f)*0.414213f;
		FrontPath fpath = new FrontPath();
		Point temp = new Point();

		temp.x = center.x-radius;
		temp.y = (int)(fCY+offset);
		appendPoint(fpath, temp);
		temp.y = (int)(fCY-offset);
		appendPoint(fpath, temp);
		temp.x = (int)(fCX-offset);
		temp.y = center.y-radius;
		appendPoint(fpath, temp);
		temp.x = (int)(fCX+offset);
		appendPoint(fpath, temp);
		temp.x = center.x+radius;
		temp.y = (int)(fCY-offset);
		appendPoint(fpath, temp);
		temp.y = (int)(fCY+offset);
		appendPoint(fpath, temp);
		temp.x = (int)(fCX+offset);
		temp.y = center.y+radius;
		appendPoint(fpath, temp);
		temp.x = (int)(fCX-offset);
		appendPoint(fpath, temp);

		outputPolygon(lay, fpath);
	}

	/**
	 * convert wires to boxes and flashes
	 */
	private void outputWire(Layer lay, int width, FrontPath wpath)
	{
		int lim = wpath.pLength;
		Point prev = removePoint(wpath);

		// do not use roundflashes with zero-width wires
		if (width != 0 && !IOTool.isCIFInSquaresWires())
		{
			boundsFlash(width, prev);
			outputFlash(lay, width, prev);
		}
		for (int i = 1; i < lim; i++)
		{
			Point curr = removePoint(wpath);

			// do not use roundflashes with zero-width wires
			if (width != 0 && !IOTool.isCIFInSquaresWires())
			{
				boundsFlash(width, curr);
				outputFlash(lay, width, curr);
			}
			int xr = curr.x-prev.x;
			int yr = curr.y-prev.y;
			int len = (int)new Point2D.Double(0, 0).distance(new Point2D.Double(xr, yr));
			if (IOTool.isCIFInSquaresWires()) len += width;
			Point center = new Point((curr.x+prev.x)/2, (curr.y+prev.y)/2);
			boundsBox(len, width, center, xr, yr);
			outputBox(lay, len, width, center, xr, yr);
			prev = curr;
		}
	}

	private boolean isEndSeen()
	{
		return endCommandFound;
	}

	private void initInput()
	{
		charactersRead = 0;
		resetInputBuffer = true;
	}

	private void initErrors()
	{
		numFatalErrors = 0;
	}

	private void initUtilities()
	{
		minMaxStackLeft = new int[MAXMMSTACK];
		minMaxStackRight = new int[MAXMMSTACK];
		minMaxStackBottom = new int[MAXMMSTACK];
		minMaxStackTop = new int[MAXMMSTACK];
		symbolTable = new HashMap<Integer,FrontSymbol>();
		minMaxStackPtr = -1;			// minmax stack pointer
	}

	private void initMatrices()
	{
		matrixStackTop = new FrontMatrix();
		clearMatrix(matrixStackTop);
		matrixStackTop.next = null;
		matrixStackTop.prev = null;
		matrixStackTop.multiplied = true;
	}

	private void clearMatrix(FrontMatrix mat)
	{
		mat.a11 = 1.0;   mat.a12 = 0.0;
		mat.a21 = 0.0;   mat.a22 = 1.0;
		mat.a31 = 0.0;   mat.a32 = 0.0;   mat.a33 = 1.0;
		mat.type = TIDENT;   mat.multiplied = false;
	}

	private void inFromFile()
	{
		try
		{
			nextInputCharacter = lineReader.read();
			updateProgressDialog(1);
		} catch (IOException e)
		{
			nextInputCharacter = -1;
		}
	}

	private char getNextCharacter()
	{
		if (resetInputBuffer)
		{
			resetInputBuffer = false;
			inputBuffer = new StringBuffer();
			charactersRead = 0;
		}

		int c = nextInputCharacter;
		if (c >= 0)
		{
			if (c != '\n')
			{
				charactersRead++;
				inputBuffer.append((char)c);
			} else resetInputBuffer = true;
			try
			{
				nextInputCharacter = lineReader.read();
				updateProgressDialog(1);
			} catch (IOException e)
			{
				nextInputCharacter = -1;
			}
		}
		return (char)c;
	}

	private char peekNextCharacter()
	{
		return (char)nextInputCharacter;
	}

	private boolean atEndOfFile()
	{
		return nextInputCharacter < 0;
	}

	private int flushInput(char breakchar)
	{
		int c;
		while ((c = peekNextCharacter()) >= 0 && c != breakchar) getNextCharacter();
		return c;
	}

	private void skipBlanks()
	{
		for(;;)
		{
			if (atEndOfFile()) break;
			int c = peekNextCharacter();
			if (TextUtils.isDigit((char)c) || Character.isUpperCase((char)c)) break;
			if (c == '(' || c == ')' || c == ';' || c == '-') break;
			getNextCharacter();
		}
	}

	private int parseStatement()
	{
		if (atEndOfFile()) return ENDFILE;

		skipBlanks();		// flush initial junk

		int curChar = getNextCharacter();
		int command = 0;
		int xRotate=0, yRotate=0, length=0, width=0, diameter=0, symbolNumber=0, multiplier=0, divisor=0, userCommand=0;
		Point center = null, namePoint = null;
		FrontTransformList curTList = null;
		FrontPath curPath = null;
		String lName = null, nameText = null, userText = null;
		switch (curChar)
		{
			case 'P':
				command = POLYCOM;
				curPath = new FrontPath();
				getPath(curPath); if (errorFound) return reportError();
				break;

			case 'B':
				command = BOXCOM;
				xRotate = 1; yRotate = 0;
				length = getNumber(); if (errorFound) return reportError();
				width = getNumber(); if (errorFound) return reportError();
				center = getPoint(); if (errorFound) return reportError();
				skipSeparators();
				if (((curChar = peekNextCharacter()) >= '0' && curChar <= '9') || curChar == '-')
				{
					xRotate = getSignedInteger(); if (errorFound) return reportError();
					yRotate = getSignedInteger(); if (errorFound) return reportError();
				}
				break;

			case 'R':
				command = FLASHCOM;
				diameter = getNumber(); if (errorFound) return reportError();
				center = getPoint(); if (errorFound) return reportError();
				break;

			case 'W':
				command = WIRECOM;
				width = getNumber(); if (errorFound) return reportError();
				curPath = new FrontPath();
				getPath(curPath); if (errorFound) return reportError();
				break;

			case 'L':
				command = LAYER;
				skipBlanks();
				StringBuffer layerName = new StringBuffer();
				for (int i = 0; i<4; i++)
				{
					int chr = peekNextCharacter();
					if (!Character.isUpperCase((char)chr) && !TextUtils.isDigit((char)chr)) break;
					layerName.append(getNextCharacter());
				}
				if (layerName.length() == 0) {errorFound = true; errorType = NOLAYER; return reportError();}
				lName = layerName.toString();
				break;

			case 'D':
				skipBlanks();
				switch (getNextCharacter())
				{
					case 'S':
						command = DEFSTART;
						symbolNumber = getNumber(); if (errorFound) return reportError();
						skipSeparators(); multiplier = divisor = 1;
						if (TextUtils.isDigit((char)peekNextCharacter()))
						{
							multiplier = getNumber(); if (errorFound) return reportError();
							divisor = getNumber(); if (errorFound) return reportError();
						}
						if (isInCellDefinition)
						{
							errorFound = true;
							errorType = NESTDEF;
							return reportError();
						}
						isInCellDefinition = true;
						break;
					case 'F':
						command = DEFEND;
						if (!isInCellDefinition)
						{
							errorFound = true;
							errorType = NODEFSTART;
							return reportError();
						}
						isInCellDefinition = false;
						break;
					case 'D':
						command = DELETEDEF;
						symbolNumber = getNumber(); if (errorFound) return reportError();
						if (isInCellDefinition)
						{
							errorFound = true;
							errorType = NESTDD;
							return reportError();
						}
						break;
					default:
						errorFound = true;
						errorType = BADDEF;
						return reportError();
				}
				break;

			case 'C':
				command = CALLCOM;
				symbolNumber = getNumber(); if (errorFound) return reportError();
				skipBlanks();
				curTList = new FrontTransformList();
				for(;;)
				{
					FrontTransformEntry trans = new FrontTransformEntry();
					int val = peekNextCharacter();
					if (val == ';') break;
					switch (peekNextCharacter())
					{
						case 'T':
							getNextCharacter();
							trans.kind = TRANSLATE;
							trans.xt = getSignedInteger(); if (errorFound) return reportError();
							trans.yt = getSignedInteger(); if (errorFound) return reportError();
							appendTransformEntry(curTList, trans);
							break;

						case 'M':
							trans.kind = MIRROR;
							getNextCharacter(); skipBlanks();
							switch (getNextCharacter())
							{
								case 'X': trans.xCoord = true; break;
								case 'Y': trans.xCoord = false; break;
								default:  errorFound = true; errorType = BADAXIS; return reportError();
							}
							appendTransformEntry(curTList, trans);
							break;

						case 'R':
							trans.kind = ROTATE;
							getNextCharacter();
							trans.xRot = getSignedInteger(); if (errorFound) return reportError();
							trans.yRot = getSignedInteger(); if (errorFound) return reportError();
							appendTransformEntry(curTList, trans);
							break;

						default:
							errorFound = true; errorType = BADTRANS; return reportError();
					}
					skipBlanks();		// between transformation commands
				}	// end of while (1) loop
				break;

			case '(':
				{
					int level = 1;
					command = COMMENT;
					StringBuffer comment = new StringBuffer();
					while (level != 0)
					{
						curChar = getNextCharacter();
						switch (curChar)
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
								errorFound = true; errorType = BADCOMMENT; return reportError();
							default:
								comment.append(curChar);
						}
					}
				}
				break;

			case 'E':
				skipBlanks();
				if (isInCellDefinition)
				{
					errorFound = true;
					errorType = NESTEND;
					return reportError();
				}
				if (!atEndOfFile()) errorReport("more text follows end command", ADVISORY);
				endIsSeen = true;
				processEnd();
				return END;

			case ';':
				return NULLCOMMAND;

			default:
				if (TextUtils.isDigit((char)curChar))
				{
					userCommand = curChar - '0';
					if (userCommand == 9)
					{
						curChar = peekNextCharacter();
						if (curChar == ' ' || curChar == '\t' || curChar == '1' || curChar == '2' || curChar == '3')
						{
							switch (getNextCharacter())
							{
								case ' ':
								case '\t':
									skipSpaces(); nameText = parseName(); if (errorFound) return reportError();
									command = SYMNAME;
									break;
								case '1':
								case '2':
								case '3':
									if (!skipSpaces())
									{
										errorFound = true; errorType = NOSPACE;
										return reportError();
									}
									nameText = parseName(); if (errorFound) return reportError();
									switch (curChar)
									{
										case '1':
											command = INSTNAME;
											break;
										case '2':
										{
											command = GEONAME;
											namePoint = getPoint(); if (errorFound) return reportError();
											skipBlanks();
											StringBuffer layName = new StringBuffer();
											for (int i = 0; i<4; i++)
											{
												int chr = peekNextCharacter();
												if (!Character.isUpperCase((char)chr) && !TextUtils.isDigit((char)chr)) break;
												layName.append(getNextCharacter());
											}
											lName = layName.toString();
											break;
										}
										case '3':
											command = LABELCOM;
											namePoint = getPoint(); if (errorFound) return reportError();
											break;
									}
									break;
							}
						} else
						{
							command = USERS;
							userText = getUserText();
							if (atEndOfFile())
							{
								errorFound = true; errorType = BADUSER; return reportError();
							}
						}
					}
				} else
				{
					errorFound = true;
					errorType = BADCOMMAND;
					return reportError();
				}
		}

		// by now we have a syntactically valid command although it might be missing a semi-colon
		switch (command)
		{
			case WIRECOM:
				makeWire(width, curPath);
				break;
			case DEFSTART:
				makeStartDefinition(symbolNumber, multiplier, divisor);
				break;
			case DEFEND:
				makeEndDefinition();
				break;
			case DELETEDEF:
				makeDeleteDefinition(symbolNumber);
				break;
			case CALLCOM:
				makeCall(symbolNumber, curTList);
				break;
			case LAYER:
				makeLayer(lName);
				break;
			case FLASHCOM:
				makeFlash(diameter, center);
				break;
			case POLYCOM:
				makePolygon(curPath);
				break;
			case BOXCOM:
				makeBox(length, width, center, xRotate, yRotate);
				break;
			case COMMENT:
				break;
			case USERS:
				makeUserComment(userCommand, userText);
				break;
			case SYMNAME:
				makeSymbolName(nameText);
				break;
			case INSTNAME:
				makeInstanceName(nameText);
				break;
			case GEONAME:
				makeGeomName(nameText, namePoint, curTech.findLayer(lName));
				break;
			case LABELCOM:
				makeLabel(nameText, namePoint);
				break;
			default:
				errorFound = true;
				errorType = INTERNAL;
				return reportError();
		}
		if (!skipSemicolon()) {errorFound = true; errorType = NOSEMI; return reportError();}

		return command;
	}

	private void makeLabel(String name, Point pt)
	{
		statementsSince91 = true;
		if (ignoreStatements) return;
		if (name.length() == 0)
		{
			errorReport("null label ignored", ADVISORY);
			return;
		}
		FrontLabel obj = new FrontLabel();
		if (isInCellDefinition && cellScaleFactor != 1.0)
		{
			pt.x = (int)(cellScaleFactor * pt.x);
			pt.y = (int)(cellScaleFactor * pt.y);
		}
		obj.pos = pt;
		obj.name = name;

		pushTransform();
		Point temp = transformPoint(pt);
		popTransform();
		obj.bb.l = temp.x;
		obj.bb.r = temp.x;
		obj.bb.b = temp.y;
		obj.bb.t = temp.y;

		if (isInCellDefinition)
		{
			// insert into symbol's guts
			obj.next = currentFrontSymbol.guts;
			currentFrontSymbol.guts = obj;
		} else topLevelItem(obj);		// stick into item list
	}

	private void makeGeomName(String name, Point pt, Layer lay)
	{
		statementsSince91 = true;
		if (ignoreStatements) return;
		if (name.length() == 0)
		{
			errorReport("null geometry name ignored", ADVISORY);
			return;
		}
		FrontGeomName obj = new FrontGeomName();
		obj.layer = lay;
		if (isInCellDefinition && cellScaleFactor != 1.0)
		{
			pt.x = (int)(cellScaleFactor * pt.x);
			pt.y = (int)(cellScaleFactor * pt.y);
		}

		pushTransform();
		Point temp = transformPoint(pt);
		popTransform();
		obj.bb.l = temp.x;
		obj.bb.r = temp.x;
		obj.bb.b = temp.y;
		obj.bb.t = temp.y;

		if (isInCellDefinition)
		{
			// insert into symbol's guts
			obj.next = currentFrontSymbol.guts;
			currentFrontSymbol.guts = obj;
		} else topLevelItem(obj);		// stick into item list
	}

	private void makeSymbolName(String name)
	{
		statementsSince91 = true;
		if (ignoreStatements) return;
		if (!isInCellDefinition)
		{
			errorReport("no symbol to name", FATALSEMANTIC);
			return;
		}
		if (name.length() == 0)
		{
			errorReport("null symbol name ignored", ADVISORY);
			return;
		}
		if (symbolNamed)
		{
			errorReport("symbol is already named, new name ignored", FATALSEMANTIC);
			return;
		}
		symbolNamed = true;
		currentFrontSymbol.name = name;
	}

	private void makeUserComment(int command, String text)
	{
		statementsSince91 = true;
		if (ignoreStatements) return;
	}

	private void makeBox(int length, int width, Point center, int xr, int yr)
	{
		statementsSince91 = true;
		if (ignoreStatements) return;
		if (currentLayer == null)
		{
			numNullLayerErrors = true;
			return;
		}
		if (length == 0 || width == 0)
		{
			errorReport("box with null length or width specified, ignored", ADVISORY);
			return;
		}

		if (isInCellDefinition && cellScaleFactor != 1.0)
		{
			length = (int) (cellScaleFactor * length);
			width = (int) (cellScaleFactor * width);
			center.x = (int) (cellScaleFactor * center.x);
			center.y = (int) (cellScaleFactor * center.y);
		}

		Rectangle box = boundsBox(length, width, center, xr, yr);
		int tl = box.x;   int tr = box.x + box.width;
		int tb = box.y;   int tt = box.y + box.height;

		// check for manhattan box
		int halfW = width/2;
		int halfL = length/2;
		if (
			(yr == 0 && (length%2) == 0 && (width%2) == 0 &&
			(center.x-halfL) == tl && (center.x+halfL) == tr &&
			(center.y-halfW) == tb && (center.y+halfW) == tt)
			||
			(xr == 0 && (length%2) == 0 && (width%2) == 0 &&
			(center.x-halfW) == tl && (center.x+halfW) == tr &&
			(center.y-halfL) == tb && (center.y+halfL) == tt)
		)
		{
			// a manhattan box
			FrontManBox obj = new FrontManBox();
			obj.layer = currentLayer;
			if (yr == 0)
			{
				obj.bb.l = tl;
				obj.bb.r = tr;
				obj.bb.b = tb;
				obj.bb.t = tt;
			} else
			{
				// this assumes that bb is unaffected by rotation
				obj.bb.l = center.x-halfW;
				obj.bb.r = center.x+halfW;
				obj.bb.b = center.y-halfL;
				obj.bb.t = center.y+halfL;
			}
			if (isInCellDefinition)
			{
				// insert into symbol's guts
				obj.next = currentFrontSymbol.guts;
				currentFrontSymbol.guts = obj;
			} else topLevelItem(obj);		// stick into item list
		} else
		{
			FrontBox obj = new FrontBox();
			obj.layer = currentLayer;
			obj.length = length;
			obj.width = width;
			obj.center = center;
			obj.xRot = xr;
			obj.yRot = yr;

			obj.bb.l = tl;
			obj.bb.r = tr;
			obj.bb.b = tb;
			obj.bb.t = tt;
			if (isInCellDefinition)
			{
				// insert into symbol's guts
				obj.next = currentFrontSymbol.guts;
				currentFrontSymbol.guts = obj;
			} else topLevelItem(obj);		// stick into item list
		}
	}

	private void makeFlash(int diameter, Point center)
	{
		statementsSince91 = true;
		if (ignoreStatements) return;
		if (currentLayer == null)
		{
			numNullLayerErrors = true;
			return;
		}
		if (diameter == 0)
		{
			errorReport("flash with null diamter, ignored", ADVISORY);
			return;
		}

		FrontFlash obj = new FrontFlash();
		obj.layer = currentLayer;
		if (isInCellDefinition && cellScaleFactor != 1.0)
		{
			diameter = (int) (cellScaleFactor * diameter);
			center.x = (int) (cellScaleFactor * center.x);
			center.y = (int) (cellScaleFactor * center.y);
		}
		obj.diameter = diameter;
		obj.center = center;

		Rectangle box = boundsFlash(diameter, center);
		obj.bb.l = box.x;
		obj.bb.r = box.x + box.width;
		obj.bb.b = box.y;
		obj.bb.t = box.y + box.height;

		if (isInCellDefinition)
		{
			// insert into symbol's guts
			obj.next = currentFrontSymbol.guts;
			currentFrontSymbol.guts = obj;
		} else topLevelItem(obj);		// stick into item list
	}

	private Rectangle boundsFlash(int diameter, Point center)
	{
		return boundsBox(diameter, diameter, center, 1, 0);
	}

	private Rectangle boundsBox(int length, int width, Point center, int xr, int yr)
	{
		int dx = length/2;
		int dy = width/2;

		pushTransform();	// newtrans
		rotateMatrix(xr, yr);
		translateMatrix(center.x, center.y);
		Point temp = new Point(dx, dy);
		initMinMax(transformPoint(temp));
		temp.y = -dy;
		minMax(transformPoint(temp));
		temp.x = -dx;
		minMax(transformPoint(temp));
		temp.y = dy;
		minMax(transformPoint(temp));
		popTransform();
		Rectangle ret = new Rectangle(getMinMaxMinX(), getMinMaxMinY(),
			getMinMaxMaxX()-getMinMaxMinX(), getMinMaxMaxY()-getMinMaxMinY());
		doneMinMax();
		return ret;
	}

	private void makeLayer(String lName)
	{
		statementsSince91 = true;
		if (ignoreStatements) return;
		currentLayer = cifLayerNames.get(lName);
		if (currentLayer == null)
		{
			unknownLayerNames.add(lName);
		}
	}

	private void makeCall(int symbol, FrontTransformList list)
	{
		if (ignoreStatements) return;
		int j = getFrontTransformListLength(list);
		FrontTransformList newtlist = null;
		if (j != 0) newtlist = new FrontTransformList();

		pushTransform();		// get new frame of reference
		for (int i = 1; i <=j; i++)
		{
			// build up incremental transformations
			FrontTransformEntry temp = removeFrontTransformEntry(list);
			if (temp.kind == MIRROR)
			{
				mirrorMatrix(temp.xCoord);
			} else if (temp.kind == TRANSLATE)
			{
				if (isInCellDefinition && cellScaleFactor != 1.0)
				{
					temp.xt = (int)(cellScaleFactor * temp.xt);
					temp.yt = (int)(cellScaleFactor * temp.yt);
				}
				translateMatrix(temp.xt, temp.yt);
			} else if (temp.kind == ROTATE)
			{
				rotateMatrix(temp.xRot, temp.yRot);
			} else
			{
				errorReport("interpreter: no such transformation", FATALINTERNAL);
			}
			appendTransformEntry(newtlist, temp);	// copy the list
		}

		FrontCall obj = new FrontCall();

		// must make a copy of the matrix
		obj.matrix = new FrontMatrix();
		obj.matrix.a11 = matrixStackTop.a11;
		obj.matrix.a12 = matrixStackTop.a12;
		obj.matrix.a21 = matrixStackTop.a21;
		obj.matrix.a22 = matrixStackTop.a22;
		obj.matrix.a31 = matrixStackTop.a31;
		obj.matrix.a32 = matrixStackTop.a32;
		obj.matrix.a33 = matrixStackTop.a33;
		obj.matrix.type = matrixStackTop.type;
		obj.matrix.multiplied = matrixStackTop.multiplied;

		popTransform();		// return to previous state

		obj.symNumber = symbol;
		obj.unID = null;
		obj.transList = newtlist;

		if (namePending)
		{
			if (statementsSince91)
				errorReport("statements between name and instance", ADVISORY);
			namePending = false;
		}
		if (isInCellDefinition)
		{
			// insert into guts of symbol
			obj.next = currentFrontSymbol.guts;
			currentFrontSymbol.guts = obj;
			currentFrontSymbol.numCalls++;
		} else
		{
			topLevelItem(obj);
		}
	}

	private void rotateMatrix(int xRot, int yRot)
	{
		double si = yRot;
		double co = xRot;
		if (yRot == 0 && xRot >= 0) return;

		matrixStackTop.type |= TROTATE;
		if (xRot == 0)
		{
			double temp = matrixStackTop.a11;
			matrixStackTop.a11 = -matrixStackTop.a12;
			matrixStackTop.a12 = temp;

			temp = matrixStackTop.a21;
			matrixStackTop.a21 = -matrixStackTop.a22;
			matrixStackTop.a22 = temp;

			temp = matrixStackTop.a31;
			matrixStackTop.a31 = -matrixStackTop.a32;
			matrixStackTop.a32 = temp;
			if (yRot < 0) matrixStackTop.a33 = -matrixStackTop.a33;
		} else
			if (yRot == 0) matrixStackTop.a33 = -matrixStackTop.a33;	// xRot < 0
		else
		{
			double temp = matrixStackTop.a11*co - matrixStackTop.a12*si;
			matrixStackTop.a12 = matrixStackTop.a11*si + matrixStackTop.a12*co;
			matrixStackTop.a11 = temp;
			temp = matrixStackTop.a21*co - matrixStackTop.a22*si;
			matrixStackTop.a22 = matrixStackTop.a21*si + matrixStackTop.a22*co;
			matrixStackTop.a21 = temp;
			temp = matrixStackTop.a31*co - matrixStackTop.a32*si;
			matrixStackTop.a32 = matrixStackTop.a31*si + matrixStackTop.a32*co;
			matrixStackTop.a31 = temp;
			matrixStackTop.a33 = new Point2D.Double(0, 0).distance(new Point2D.Double(co, si));
		}
	}

	private void translateMatrix(int xtrans, int ytrans)
	{
		if (xtrans != 0 || ytrans != 0)
		{
			matrixStackTop.a31 += matrixStackTop.a33*xtrans;
			matrixStackTop.a32 += matrixStackTop.a33*ytrans;
			matrixStackTop.type |= TTRANSLATE;
		}
	}

	private void mirrorMatrix(boolean xCoord)
	{
		if (xCoord)
		{
			matrixStackTop.a11 = -matrixStackTop.a11;
			matrixStackTop.a21 = -matrixStackTop.a21;
			matrixStackTop.a31 = -matrixStackTop.a31;
		} else
		{
			matrixStackTop.a12 = -matrixStackTop.a12;
			matrixStackTop.a22 = -matrixStackTop.a22;
			matrixStackTop.a32 = -matrixStackTop.a32;
		}
		matrixStackTop.type |= TMIRROR;
	}

	private int getFrontTransformListLength(FrontTransformList a)
	{
		if (a == null) return 0;
		return a.tLength;
	}

	private FrontTransformEntry removeFrontTransformEntry(FrontTransformList a)
	{
		if (a.tFirst == null)
		{
			// added extra code to initialize "ans" to a dummy value
			FrontTransformEntry ans = new FrontTransformEntry();
			ans.kind = TRANSLATE;
			ans.xt = ans.yt = 0;
			return ans;
		}
		FrontLinkedTransform temp = a.tFirst.tNext;
		FrontTransformEntry ans = a.tFirst.tValue;
		a.tFirst = temp;
		if (a.tFirst == null) a.tLast = null;
		a.tLength -= 1;
		return ans;
	}

	private void makeDeleteDefinition(int n)
	{
		statementsSince91 = true;
		errorReport("DD not supported (ignored)", ADVISORY);
	}

	private void makeEndDefinition()
	{
		statementsSince91 = true;
		if (ignoreStatements)
		{
			ignoreStatements = false;
			return;
		}
		isInCellDefinition = false;
		currentLayer = backupLayer;		// restore old layer
		if (!symbolNamed)
		{
			String s = "SYM" + currentFrontSymbol.symNumber;
			currentFrontSymbol.name = s;
		}
		currentFrontSymbol.defined = true;
	}

	private void makeStartDefinition(int symbol, int mtl, int div)
	{
		statementsSince91 = true;
		currentFrontSymbol = lookupSymbol(symbol);
		if (currentFrontSymbol.defined)
		{
			// redefining this symbol
			String mess = "attempt to redefine symbol " + symbol + " (ignored)";
			errorReport(mess, ADVISORY);
			ignoreStatements = true;
			return;
		}

		isInCellDefinition = true;
		if (mtl != 0 && div != 0) cellScaleFactor = ((float) mtl)/((float) div); else
		{
			errorReport("illegal scale factor, ignored", ADVISORY);
			cellScaleFactor = 1.0;
		}
		backupLayer = currentLayer;	// save current layer
		currentLayer = null;
		symbolNamed = false;					// symbol not named
	}

	private void makeWire(int width, FrontPath a)
	{
		int length = a.pLength;
		statementsSince91 = true;
		if (ignoreStatements) return;
		if (currentLayer == null)
		{
			numNullLayerErrors = true;
			return;
		}
		FrontWire obj = new FrontWire();
		FrontPath tPath = a;
		FrontPath sPath = null;			// path in case of scaling
		obj.layer = currentLayer;
		if (isInCellDefinition && cellScaleFactor != 1.0)
		{
			sPath = new FrontPath();
			scalePath(a, sPath);		// scale all points
			width = (int)(cellScaleFactor * width);
			tPath = sPath;
		}
		obj.width = width;

		FrontPath bbpath = new FrontPath();		// get a new path for bb use
		copyPath(tPath, bbpath);
		Rectangle box = boundsWire(width, bbpath);
		obj.points = new Point[length];
		for (int i = 0; i < length; i++) obj.points[i] = removePoint(tPath);

		if (isInCellDefinition)
		{
			// insert into symbol's guts
			obj.next = currentFrontSymbol.guts;
			currentFrontSymbol.guts = obj;
		} else topLevelItem(obj);		// stick into item list
	}

	private Rectangle boundsWire(int width, FrontPath pPath)
	{
		int half = (width+1)/2;
		int limit = pPath.pLength;

		pushTransform();	// newtrans
		initMinMax(transformPoint(removePoint(pPath)));
		for (int i = 1; i < limit; i++)
		{
			minMax(transformPoint(removePoint(pPath)));
		}
		popTransform();
		Rectangle rect = new Rectangle(getMinMaxMinX()-half, getMinMaxMinY()-half,
			getMinMaxMaxX()-getMinMaxMinX()+half*2, getMinMaxMaxY()-getMinMaxMinY()+half*2);
		doneMinMax();
		return rect;
	}

	private String getUserText()
	{
		StringBuffer user = new StringBuffer();
		for(;;)
		{
			if (atEndOfFile()) break;
			if (peekNextCharacter() == ';') break;
			user.append((char)getNextCharacter());
		}
		return user.toString();
	}

	private String parseName()
	{
		StringBuffer nText = new StringBuffer();
		boolean noChar = true;
		for(;;)
		{
			if (atEndOfFile()) break;
			int c = peekNextCharacter();
			if (c == ';' || c == ' ' || c == '\t' || c == '{' || c == '}') break;
			noChar = false;
			getNextCharacter();
			nText.append((char)c);
		}
		if (noChar) logIt(NONAME);
		return nText.toString();
	}

	private void appendTransformEntry(FrontTransformList a, FrontTransformEntry p)
	{
		FrontLinkedTransform newT = new FrontLinkedTransform();
		if (newT == null) return;

		FrontLinkedTransform temp = a.tLast;
		a.tLast = newT;
		if (temp != null) temp.tNext = a.tLast;
		a.tLast.tValue = p;
		a.tLast.tNext = null;
		if (a.tFirst == null) a.tFirst = a.tLast;
		a.tLength += 1;
	}

	private int getNumber()
	{
		boolean somedigit = false;
		int ans = 0;
		skipSpaces();

		while (ans < BIGSIGNED && TextUtils.isDigit((char)peekNextCharacter()))
		{
			ans *= 10; ans += getNextCharacter() - '0';
			somedigit = true;
		}

		if (!somedigit)
		{
			logIt(NOUNSIGNED);
			return 0;
		}
		if (TextUtils.isDigit((char)peekNextCharacter()))
		{
			logIt(NUMTOOBIG);
			return 0XFFFFFFFF;
		}
		return ans;
	}

	private boolean skipSemicolon()
	{
		boolean ans = false;
		skipBlanks();
		if (peekNextCharacter() == ';') { getNextCharacter(); ans = true; skipBlanks(); }
		return ans;
	}

	private boolean skipSpaces()
	{
		boolean ans = false;
		for(;;)
		{
			int c = peekNextCharacter();
			if (c != ' ' && c != '\t') break;
			getNextCharacter();
			ans = true;
		}
		return ans;
	}

	private void makePolygon(FrontPath a)
	{
		int length = a.pLength;
		statementsSince91 = true;
		if (ignoreStatements) return;
		if (currentLayer == null)
		{
			numNullLayerErrors = true;
			return;
		}
		if (length < 3)
		{
			errorReport("polygon with < 3 pts in path, ignored", ADVISORY);
			return;
		}

		FrontPoly obj = new FrontPoly();
		FrontPath tPath = a;
		obj.layer = currentLayer;
		if (isInCellDefinition && cellScaleFactor != 1.0)
		{
			FrontPath sPath = new FrontPath();
			scalePath(a, sPath);		// scale all points
			tPath = sPath;
		}

		FrontPath bbpath = new FrontPath();		// get a new path for bb use
		copyPath(tPath, bbpath);
		Rectangle box = getPolyBounds(bbpath);
		obj.bb.l = box.x;
		obj.bb.r = box.x + box.width;
		obj.bb.b = box.y;
		obj.bb.t = box.y + box.height;
		obj.points = new Point[length];
		for (int i = 0; i < length; i++) obj.points[i] = removePoint(tPath);

		if (isInCellDefinition)
		{
			// insert into symbol's guts
			obj.next = currentFrontSymbol.guts;
			currentFrontSymbol.guts = obj;
		} else topLevelItem(obj);		// stick into item list
	}

	/**
	 * a bare item has been found
	 */
	private void topLevelItem(FrontObjBase object)
	{
		if (object == null)
		{
			errorReport("item: null object", FATALINTERNAL);
			return;
		}

		FrontItem newItem = new FrontItem();
		newItem.same = currentItemList;		// hook into linked list
		currentItemList = newItem;
		newItem.what = object;

		// symbol calls only
		if (object instanceof FrontCall) findCallBounds((FrontCall)object);
	}

	/**
	 * find the bb for this particular call
	 */
	private void findCallBounds(FrontCall object)
	{
		FrontSymbol thisST = lookupSymbol(object.symNumber);
		if (!thisST.defined)
		{
			String mess = "call to undefined symbol " + thisST.symNumber;
			errorReport(mess, FATALSEMANTIC);
			return;
		}
		if (thisST.expanded)
		{
			String mess = "recursive call on symbol " + thisST.symNumber;
			errorReport(mess, FATALSEMANTIC);
			return;
		}
		thisST.expanded = true;		// mark as under expansion

		findBounds(thisST);		// get the bb of the symbol in its FrontSymbol
		object.unID = thisST;	// get this symbol's id

		pushTransform();			// set up a new frame of reference
		applyLocal(object.matrix);
		Point temp = new Point();
		temp.x = thisST.bounds.l;   temp.y = thisST.bounds.b;	// ll
		Point comperror = transformPoint(temp);
		initMinMax(comperror);
		temp.x = thisST.bounds.r;
		comperror = transformPoint(temp);
		minMax(comperror);
		temp.y = thisST.bounds.t;	// ur
		comperror = transformPoint(temp);
		minMax(comperror);
		temp.x = thisST.bounds.l;
		comperror = transformPoint(temp);
		minMax(comperror);

		object.bb.l = getMinMaxMinX();   object.bb.r = getMinMaxMaxX();
		object.bb.b = getMinMaxMinY();   object.bb.t = getMinMaxMaxY();
		doneMinMax();		// object now has transformed bb of the symbol
		popTransform();

		thisST.expanded = false;
	}

	/**
	 * find bb for sym
	 */
	private void findBounds(FrontSymbol sym)
	{
		boolean first = true;
		FrontObjBase ob = sym.guts;
		if (sym.boundsValid) return;			// already done
		if (ob == null)			// empty symbol
		{
			String name = sym.name;
			if (name == null) name = "#" + sym.symNumber;
			System.out.println("Warning: cell " + name + " has no geometry in it");
			sym.bounds.l = 0;   sym.bounds.r = 0;
			sym.bounds.b = 0;   sym.bounds.t = 0;
			sym.boundsValid = true;
			return;
		}

		while (ob != null)
		{
			// find bb for symbol calls, all primitive are done already
			if (ob instanceof FrontCall) findCallBounds((FrontCall)ob);
			Point temp = new Point();
			temp.x = ob.bb.l;   temp.y = ob.bb.b;
			if (first) {first = false; initMinMax(temp);}
				else minMax(temp);
			temp.x = ob.bb.r;   temp.y = ob.bb.t;
			minMax(temp);
			ob = ob.next;
		}
		sym.bounds.l = getMinMaxMinX();   sym.bounds.r = getMinMaxMaxX();
		sym.bounds.b = getMinMaxMinY();   sym.bounds.t = getMinMaxMaxY();
		sym.boundsValid = true;
		doneMinMax();
	}

	/**
	 * Method to find a given symbol.
	 * If none, make a blank entry.
	 * @return a pointer to whichever.
	 */
	private FrontSymbol lookupSymbol(int sym)
	{
		FrontSymbol val = symbolTable.get(new Integer(sym));
		if (val == null)
		{
			// create a new entry
			val = new FrontSymbol(sym);
			symbolTable.put(new Integer(sym), val);
		}
		return val;
	}

	private void applyLocal(FrontMatrix tm)
	{
		assignMatrix(tm, matrixStackTop);
	}

	private void scalePath(FrontPath src, FrontPath dest)
	{
		int limit = src.pLength;
		for (int i = 0; i < limit; i++)
		{
			Point temp = removePoint(src);

			temp.x = (int)(cellScaleFactor * temp.x);
			temp.y = (int)(cellScaleFactor * temp.y);
			appendPoint(dest, temp);
		}
	}

	private void copyPath(FrontPath src, FrontPath dest)
	{
		FrontLinkedPoint temp = src.pFirst;
		if (src == dest) return;
		while (temp != null)
		{
			appendPoint(dest, temp.pValue);
			temp = temp.pNext;
		}
	}

	private Rectangle getPolyBounds(FrontPath pPath)
	{
		int limit = pPath.pLength;

		pushTransform();	// newtrans
		initMinMax(transformPoint(removePoint(pPath)));
		for (int i = 1; i < limit; i++)
		{
			minMax(transformPoint(removePoint(pPath)));
		}
		popTransform();
		Rectangle ret = new Rectangle(getMinMaxMinX(), getMinMaxMinY(), getMinMaxMaxX()-getMinMaxMinX(), getMinMaxMaxY()-getMinMaxMinY());
		doneMinMax();
		return ret;
	}

	private void minMax(Point foo)
	{
		if (foo.x > minMaxStackRight[minMaxStackPtr]) minMaxStackRight[minMaxStackPtr] = foo.x;
			else {if (foo.x < minMaxStackLeft[minMaxStackPtr]) minMaxStackLeft[minMaxStackPtr] = foo.x;}
		if (foo.y > minMaxStackTop[minMaxStackPtr]) minMaxStackTop[minMaxStackPtr] = foo.y;
			else {if (foo.y < minMaxStackBottom[minMaxStackPtr]) minMaxStackBottom[minMaxStackPtr] = foo.y;}
	}

	private void initMinMax(Point foo)
	{
		if (++minMaxStackPtr >= MAXMMSTACK)
		{
			errorReport("initMinMax: out of stack", FATALINTERNAL);
			return;
		}
		minMaxStackLeft[minMaxStackPtr] = foo.x;   minMaxStackRight[minMaxStackPtr] = foo.x;
		minMaxStackBottom[minMaxStackPtr] = foo.y; minMaxStackTop[minMaxStackPtr] = foo.y;
	}

	private void doneMinMax()
	{
		if (minMaxStackPtr < 0) errorReport("doneMinMax: pop from empty stack", FATALINTERNAL);
			else minMaxStackPtr--;
	}

	private int getMinMaxMinX()
	{
		return minMaxStackLeft[minMaxStackPtr];
	}

	private int getMinMaxMinY()
	{
		return minMaxStackBottom[minMaxStackPtr];
	}

	private int getMinMaxMaxX()
	{
		return minMaxStackRight[minMaxStackPtr];
	}

	private int getMinMaxMaxY()
	{
		return minMaxStackTop[minMaxStackPtr];
	}

	private void pushTransform()
	{
		if (matrixStackTop.next == null)
		{
			matrixStackTop.next = new FrontMatrix();
			clearMatrix(matrixStackTop.next);
			matrixStackTop.next.prev = matrixStackTop;
			matrixStackTop = matrixStackTop.next;
			matrixStackTop.next = null;
		} else
		{
			matrixStackTop = matrixStackTop.next;
			clearMatrix(matrixStackTop);
		}
	}

	private void popTransform()
	{
		if (matrixStackTop.prev != null) matrixStackTop = matrixStackTop.prev;
			else errorReport("pop, empty trans stack", FATALINTERNAL);
	}

	private Point transformPoint(Point foo)
	{
		Point ans = new Point();

		if (!matrixStackTop.multiplied)
		{
			matrixMult(matrixStackTop, matrixStackTop.prev, matrixStackTop);
		}
		switch (matrixStackTop.type)
		{
			case TIDENT:
				return foo;
			case TTRANSLATE:
				ans.x = (int)matrixStackTop.a31;
				ans.y = (int)matrixStackTop.a32;
				ans.x += foo.x;   ans.y += foo.y;
				return ans;
			case TMIRROR:
				ans.x = (matrixStackTop.a11 < 0) ? -foo.x : foo.x;
				ans.y = (matrixStackTop.a22 < 0) ? -foo.y : foo.y;
				return ans;
			case TROTATE:
				ans.x = (int)(((float) foo.x)*matrixStackTop.a11+((float) foo.y)*matrixStackTop.a21);
				ans.y = (int)(((float) foo.x)*matrixStackTop.a12+((float) foo.y)*matrixStackTop.a22);
				return ans;
			default:
				ans.x = (int)(matrixStackTop.a31 + ((float) foo.x)*matrixStackTop.a11+
					((float) foo.y)*matrixStackTop.a21);
				ans.y = (int)(matrixStackTop.a32 + ((float) foo.x)*matrixStackTop.a12+
					((float) foo.y)*matrixStackTop.a22);
		}
		return ans;
	}

	private void matrixMult(FrontMatrix l, FrontMatrix r, FrontMatrix result)
	{
		if (l == null || r == null || result == null)
			errorReport("null arg to matrixMult", FATALINTERNAL);
		if (result.multiplied)
		{
			errorReport("can't re-mult matrix", FATALINTERNAL);
			return;
		}
		if (!r.multiplied)
		{
			FrontMatrix temp = new FrontMatrix();
			temp.multiplied = false;
			matrixMult(r, r.prev, temp);
			matrixMultCore(l, temp, result);
		} else matrixMultCore(l, r, result);
	}

	private void matrixMultCore(FrontMatrix l, FrontMatrix r, FrontMatrix result)
	{
		if (l == null || r == null || result == null)
		{
			errorReport("null arg to matrixMultCore", FATALINTERNAL);
			return;
		}
		if (l.type == TIDENT) assignMatrix(r, result); else
			if (r.type == TIDENT) assignMatrix(l, result); else
		{
			FrontMatrix temp = new FrontMatrix();
			temp.a11 = l.a11 * r.a11 + l.a12 * r.a21;
			temp.a12 = l.a11 * r.a12 + l.a12 * r.a22;
			temp.a21 = l.a21 * r.a11 + l.a22 * r.a21;
			temp.a22 = l.a21 * r.a12 + l.a22 * r.a22;
			temp.a31 = l.a31 * r.a11 + l.a32 * r.a21 + l.a33 * r.a31;
			temp.a32 = l.a31 * r.a12 + l.a32 * r.a22 + l.a33 * r.a32;
			temp.a33 = l.a33*r.a33;
			temp.type = l.type | r.type;
			assignMatrix(temp, result);
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

	private void assignMatrix(FrontMatrix src, FrontMatrix dest)
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

	private Point removePoint(FrontPath a)
	{
		if (a.pFirst == null)
		{
			// added code to initialize return value with dummy numbers
			return new Point(0, 0);
		}
		FrontLinkedPoint temp = a.pFirst.pNext;
		Point ans = a.pFirst.pValue;
		a.pFirst = temp;
		if (a.pFirst == null) a.pLast = null;
		a.pLength -= 1;
		return ans;
	}

	private void makeInstanceName(String name)
	{
		if (ignoreStatements) return;
		if (name.length() == 0)
		{
			errorReport("null instance name ignored", ADVISORY);
			return;
		}
		if (namePending)
		{
			errorReport("there is already a name pending, new name replaces it", ADVISORY);
		}
		namePending = true;
		statementsSince91 = false;
	}

	private void processEnd()
	{
		statementsSince91 = true;
		endCommandFound = true;
		if (namePending)
		{
			errorReport("no instance to match name command", ADVISORY);
			namePending = false;
		}
	}

	private int getSignedInteger()
	{
		boolean sign = false;
		int ans = 0;
		skipSeparators();

		if (peekNextCharacter() == '-') { sign = true;   getNextCharacter(); }

		boolean someDigit = false;
		while (ans < BIGSIGNED && TextUtils.isDigit((char)peekNextCharacter()))
		{
			ans *= 10; ans += getNextCharacter() - '0';
			someDigit = true;
		}

		if (!someDigit) { logIt(NOSIGNED);   return 0; }
		if (TextUtils.isDigit((char)peekNextCharacter()))
		{
			logIt(NUMTOOBIG);
			return sign ? -0X7FFFFFFF : 0X7FFFFFFF;
		}
		return sign ? -ans : ans;
	}

	private void logIt(int thing)
	{
		errorFound = true;
		errorType = thing;
	}

	private Point getPoint()
	{
		int x = getSignedInteger();
		int y = getSignedInteger();
		return new Point(x, y);
	}

	private void getPath(FrontPath a)
	{
		skipSeparators();
		for(;;)
		{
			int c = peekNextCharacter();
			if (!TextUtils.isDigit((char)c) && c != '-') break;
			Point temp = getPoint();	  if (errorFound) break;
			appendPoint(a, temp);
			skipSeparators();
		}
		if (a.pLength == 0) logIt(NOPATH);
	}

	private void appendPoint(FrontPath a, Point p)
	{
		FrontLinkedPoint temp;

		temp = a.pLast;
		a.pLast = new FrontLinkedPoint();
		if (temp != null) temp.pNext = a.pLast;
		a.pLast.pValue = p;
		a.pLast.pNext = null;
		if (a.pFirst == null) a.pFirst = a.pLast;
		a.pLength += 1;
	}

	private void skipSeparators()
	{
		for(;;)
		{
			int c = peekNextCharacter();
			switch (c)
			{
				case '(':
				case ')':
				case ';':
				case '-':
				case -1:
					return;
				default:
					if (TextUtils.isDigit((char)c)) return;
					getNextCharacter();
			}
		}
	}

	private int reportError()
	{
		switch (errorType)
		{
			case NUMTOOBIG:  errorReport("number too large", FATALSYNTAX); break;
			case NOUNSIGNED: errorReport("unsigned integer expected", FATALSYNTAX); break;
			case NOSIGNED:   errorReport("signed integer expected", FATALSYNTAX); break;
			case NOSEMI:     errorReport("missing ';' inserted", FATALSYNTAX); break;
			case NOPATH:     errorReport("no points in path", FATALSYNTAX); break;
			case BADTRANS:   errorReport("no such transformation command", FATALSYNTAX); break;
			case BADUSER:    errorReport("end of file inside user command", FATALSYNTAX); break;
			case BADCOMMAND: errorReport("unknown command encountered", FATALSYNTAX); break;
			case INTERNAL:   errorReport("parser can't find i routine", FATALINTERNAL); break;
			case BADDEF:     errorReport("no such define command", FATALSYNTAX); break;
			case NOLAYER:    errorReport("layer name expected", FATALSYNTAX); break;
			case BADCOMMENT: errorReport("end of file inside a comment", FATALSYNTAX); break;
			case BADAXIS:    errorReport("no such axis in mirror command", FATALSYNTAX); break;
			case NESTDEF:    errorReport("symbol definitions can't nest", FATALSYNTAX); break;
			case NODEFSTART: errorReport("DF without DS", FATALSYNTAX); break;
			case NESTDD:     errorReport("DD can't appear inside symbol definition", FATALSYNTAX); break;
			case NOSPACE:    errorReport("missing space in name command", FATALSYNTAX); break;
			case NONAME:     errorReport("no name in name command", FATALSYNTAX); break;
			case NESTEND:    errorReport("End command inside symbol definition", FATALSYNTAX); break;
			case NOERROR:    errorReport("error signaled but not reported", FATALINTERNAL); break;
			default:         errorReport("uncaught error", FATALSYNTAX);
		}
		if (errorType != INTERNAL && errorType != NOSEMI && flushInput(';') < 0)
			errorReport("unexpected end of input file", FATALSYNTAX);
				else skipBlanks();
		errorFound = false;
		errorType = NOERROR;
		return SYNTAXERROR;
	}

	private void errorReport(String mess, int kind)
	{
		if (charactersRead > 0)
		{
			System.out.println("line " + (lineReader.getLineNumber()-(resetInputBuffer?1:0)) + ": " + inputBuffer.toString());
		}
		if (kind == FATALINTERNAL || kind == FATALINTERNAL ||
			kind == FATALSEMANTIC || kind == FATALOUTPUT) numFatalErrors++;
		switch (kind)
		{
			case FATALINTERNAL: System.out.println("Fatal internal error: " + mess);  break;
			case FATALSYNTAX:   System.out.println("Syntax error: " + mess);          break;
			case FATALSEMANTIC: System.out.println("Error: " + mess);                 break;
			case FATALOUTPUT:   System.out.println("Output error: " + mess);          break;
			case ADVISORY:      System.out.println("Warning: " + mess);               break;
			default:            System.out.println(mess);                             break;
		}
	}
}
