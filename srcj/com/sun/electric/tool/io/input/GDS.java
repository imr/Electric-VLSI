/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GDS.java
 * Input/output tool: GDS input
 * Original C code written by Glen M. Lawson, S-MOS Systems, Inc.
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
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.MutableTextDescriptor;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.io.IOTool;

import java.awt.Point;
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
 * This class reads files in GDS files.
 * <BR>
 * Notes:
 * <UL>
 * <LI>Case sensitive.</LI>
 * <LI>NODEs, TEXTNODEs, BOXs - don't have an example.</LI>
 * <LI>PATHTYPE 1 - rounded ends on paths, not supported.</LI>
 * <LI>Path dogears - no cleanup yet, simpler to map paths into arcs.</LI>
 * <LI>Absolute angle - ???</LI>
 * <LI>SUBLAYERS or XXXTYPE fields are not supported.</LI>
 * <LI>PROPERTIES are not supported.</LI>
 * <LI>REFLIBS are not supported.</LI>
 * <LI>PATH-ARC mapping - should be done, problem is that any layer can be a path, only connection layers in Electric are arcs.
 *	Someone could make a GDS mapping technology for this purpose, defaults could be taken from this technology.</LI>
 * <LI>Misc. fields mapped to variables - should be done.</LI>
 * <LI>AREFs - could build into a separate NODEPROTO, and instance, thus preserving the original intent and space.</LI>
 * <LI>MAG - no scaling is possible, must create a separate object for each value, don't scale.  (TEXT does scale.)</LI>
 * </UL>
 */
public class GDS extends Input
{
	// data declarations
	private static final int MAXPOINTS     = 4096;
	private static final int MINFONTWIDTH  =  130;
	private static final int MINFONTHEIGHT =  190;

	private static class ShapeType {}
	private static final ShapeType SHAPEPOLY      = new ShapeType();
	private static final ShapeType SHAPERECTANGLE = new ShapeType();
	private static final ShapeType SHAPEOBLIQUE   = new ShapeType();
	private static final ShapeType SHAPELINE      = new ShapeType();
	private static final ShapeType SHAPECLOSED    = new ShapeType();

	private static class DatatypeSymbol {}
	private static final DatatypeSymbol TYPEERR    = new DatatypeSymbol();
	private static final DatatypeSymbol TYPENONE   = new DatatypeSymbol();
	private static final DatatypeSymbol TYPEFLAGS  = new DatatypeSymbol();
	private static final DatatypeSymbol TYPESHORT  = new DatatypeSymbol();
	private static final DatatypeSymbol TYPELONG   = new DatatypeSymbol();
	private static final DatatypeSymbol TYPEFLOAT  = new DatatypeSymbol();
	private static final DatatypeSymbol TYPEDOUBLE = new DatatypeSymbol();
	private static final DatatypeSymbol TYPESTRING = new DatatypeSymbol();

	private final static double twoTo32 = makePower(2, 32);
	private final static double twoToNeg56 = 1.0 / makePower (2, 56);

	private Library        theLibrary;
	private Cell           theCell;
	private NodeProto      theNodeProto;
	private NodeProto      layerNodeProto;
	private boolean        layerUsed;
	private boolean        layerIsPin;
	private Technology     curTech;
	private int            recordCount;
	private GSymbol        theToken;
	private DatatypeSymbol valuetype;
	private int            tokenFlags;
	private int            tokenValue16;
	private int            tokenValue32;
	private double         tokenValueDouble;
	private String         tokenString;
	private Point2D []     theVertices;
	private double         theScale;
	private HashMap        layerNames;
	private HashSet        pinLayers;

	private static class GSymbol
	{
		private int value;
		private static List symbols = new ArrayList();

		private GSymbol(int value)
		{
			this.value = value;
			symbols.add(this);
		}

		private static GSymbol findSymbol(int value)
		{
			for(Iterator it = symbols.iterator(); it.hasNext(); )
			{
				GSymbol gs = (GSymbol)it.next();
				if (gs.value == value) return gs;
			}
			return null;
		}
	}
	private static final GSymbol GDS_HEADER       = new GSymbol(0);
	private static final GSymbol GDS_BGNLIB       = new GSymbol(1);
	private static final GSymbol GDS_LIBNAME      = new GSymbol(2);
	private static final GSymbol GDS_UNITS        = new GSymbol(3);
	private static final GSymbol GDS_ENDLIB       = new GSymbol(4);
	private static final GSymbol GDS_BGNSTR       = new GSymbol(5);
	private static final GSymbol GDS_STRNAME      = new GSymbol(6);
	private static final GSymbol GDS_ENDSTR       = new GSymbol(7);
	private static final GSymbol GDS_BOUNDARY     = new GSymbol(8);
	private static final GSymbol GDS_PATH         = new GSymbol(9);
	private static final GSymbol GDS_SREF         = new GSymbol(10);
	private static final GSymbol GDS_AREF         = new GSymbol(11);
	private static final GSymbol GDS_TEXTSYM      = new GSymbol(12);
	private static final GSymbol GDS_LAYER        = new GSymbol(13);
	private static final GSymbol GDS_DATATYPSYM   = new GSymbol(14);
	private static final GSymbol GDS_WIDTH        = new GSymbol(15);
	private static final GSymbol GDS_XY           = new GSymbol(16);
	private static final GSymbol GDS_ENDEL        = new GSymbol(17);
	private static final GSymbol GDS_SNAME        = new GSymbol(18);
	private static final GSymbol GDS_COLROW       = new GSymbol(19);
	private static final GSymbol GDS_TEXTNODE     = new GSymbol(20);
	private static final GSymbol GDS_NODE         = new GSymbol(21);
	private static final GSymbol GDS_TEXTTYPE     = new GSymbol(22);
	private static final GSymbol GDS_PRESENTATION = new GSymbol(23);
	private static final GSymbol GDS_SPACING      = new GSymbol(24);
	private static final GSymbol GDS_STRING       = new GSymbol(25);
	private static final GSymbol GDS_STRANS       = new GSymbol(26);
	private static final GSymbol GDS_MAG          = new GSymbol(27);
	private static final GSymbol GDS_ANGLE        = new GSymbol(28);
	private static final GSymbol GDS_UINTEGER     = new GSymbol(29);
	private static final GSymbol GDS_USTRING      = new GSymbol(30);
	private static final GSymbol GDS_REFLIBS      = new GSymbol(31);
	private static final GSymbol GDS_FONTS        = new GSymbol(32);
	private static final GSymbol GDS_PATHTYPE     = new GSymbol(33);
	private static final GSymbol GDS_GENERATIONS  = new GSymbol(34);
	private static final GSymbol GDS_ATTRTABLE    = new GSymbol(35);
	private static final GSymbol GDS_STYPTABLE    = new GSymbol(36);
	private static final GSymbol GDS_STRTYPE      = new GSymbol(37);
	private static final GSymbol GDS_ELFLAGS      = new GSymbol(38);
	private static final GSymbol GDS_ELKEY        = new GSymbol(39);
	private static final GSymbol GDS_LINKTYPE     = new GSymbol(40);
	private static final GSymbol GDS_LINKKEYS     = new GSymbol(41);
	private static final GSymbol GDS_NODETYPE     = new GSymbol(42);
	private static final GSymbol GDS_PROPATTR     = new GSymbol(43);
	private static final GSymbol GDS_PROPVALUE    = new GSymbol(44);
	private static final GSymbol GDS_BOX          = new GSymbol(45);
	private static final GSymbol GDS_BOXTYPE      = new GSymbol(46);
	private static final GSymbol GDS_PLEX         = new GSymbol(47);
	private static final GSymbol GDS_BGNEXTN      = new GSymbol(48);
	private static final GSymbol GDS_ENDEXTN      = new GSymbol(49);
	private static final GSymbol GDS_TAPENUM      = new GSymbol(50);
	private static final GSymbol GDS_TAPECODE     = new GSymbol(51);
	private static final GSymbol GDS_STRCLASS     = new GSymbol(52);
	private static final GSymbol GDS_NUMTYPES     = new GSymbol(53);
	private static final GSymbol GDS_IDENT        = new GSymbol(54);
	private static final GSymbol GDS_REALNUM      = new GSymbol(55);
	private static final GSymbol GDS_SHORT_NUMBER = new GSymbol(56);
	private static final GSymbol GDS_NUMBER       = new GSymbol(57);
	private static final GSymbol GDS_FLAGSYM      = new GSymbol(58);
	private static final GSymbol GDS_FORMAT       = new GSymbol(59);
	private static final GSymbol GDS_MASK         = new GSymbol(60);
	private static final GSymbol GDS_ENDMASKS     = new GSymbol(61);

	private static GSymbol [] optionSet = {GDS_ATTRTABLE, GDS_REFLIBS, GDS_FONTS, GDS_GENERATIONS};
	private static GSymbol [] shapeSet = {GDS_AREF, GDS_SREF, GDS_BOUNDARY, GDS_PATH, GDS_NODE, GDS_TEXTSYM, GDS_BOX};
	private static GSymbol [] goodOpSet = {GDS_HEADER, GDS_BGNLIB, GDS_LIBNAME, GDS_UNITS, GDS_ENDLIB, GDS_BGNSTR, GDS_STRNAME,
		GDS_ENDSTR, GDS_BOUNDARY, GDS_PATH, GDS_SREF, GDS_AREF, GDS_TEXTSYM, GDS_LAYER, GDS_DATATYPSYM, GDS_WIDTH, GDS_XY, GDS_ENDEL,
		GDS_SNAME, GDS_COLROW, GDS_TEXTNODE, GDS_NODE, GDS_TEXTTYPE, GDS_PRESENTATION, GDS_STRING, GDS_STRANS, GDS_MAG, GDS_ANGLE,
		GDS_REFLIBS, GDS_FONTS, GDS_PATHTYPE, GDS_GENERATIONS, GDS_ATTRTABLE, GDS_NODETYPE, GDS_PROPATTR, GDS_PROPVALUE, GDS_BOX,
		GDS_BOXTYPE, GDS_FORMAT, GDS_MASK, GDS_ENDMASKS};
	private static GSymbol [] maskSet = {GDS_DATATYPSYM, GDS_TEXTTYPE, GDS_BOXTYPE, GDS_NODETYPE};
	private static GSymbol [] unsupportedSet = {GDS_ELFLAGS, GDS_PLEX};

	private static class MakeInstance
	{
		private static HashMap allInstances;

		private Cell parent;
		private Cell subCell;
		private Point2D loc;
		private boolean mX, mY;
		private int angle;
		private boolean instantiated;

		private MakeInstance(Cell parent, Cell subCell, Point2D loc, boolean mX, boolean mY, int angle)
		{
			this.parent = parent;
			this.subCell = subCell;
			this.loc = loc;
			this.mX = mX;
			this.mY = mY;
			this.angle = angle;
			this.instantiated = false;

			List instancesInCell = (List)allInstances.get(parent);
			if (instancesInCell == null)
			{
				instancesInCell = new ArrayList();
				allInstances.put(parent, instancesInCell);
			}
			instancesInCell.add(this);
		}

		private static void init() { allInstances = new HashMap(); }

		private static void term() { allInstances = null; }

		private static void makeCellInstances(Cell cell)
		{
			List instancesInCell = (List)allInstances.get(cell);
			if (instancesInCell == null) return;
			for(Iterator iIt = instancesInCell.iterator(); iIt.hasNext(); )
			{
				MakeInstance mi = (MakeInstance)iIt.next();
				if (mi.instantiated) continue;
				Cell subCell = mi.subCell;
				makeCellInstances(subCell);

				// make the instance
				Rectangle2D bounds = mi.subCell.getBounds();
				double wid = bounds.getWidth();
				double hei = bounds.getHeight();
				if (mi.mX) wid = -wid;
				if (mi.mY) hei = -hei;

				AffineTransform xform = NodeInst.pureRotate(mi.angle, mi.mX, mi.mY);
				double cX = bounds.getCenterX();
				double cY = bounds.getCenterY();
				Point2D ctr = new Point2D.Double(0, 0);
				xform.transform(ctr, ctr);
				double oX = ctr.getX() - cX;
				double oY = ctr.getY() - cY;
				mi.loc.setLocation(mi.loc.getX() + cX + oX, mi.loc.getY() + cY + oY);
				NodeInst ni = NodeInst.makeInstance(mi.subCell, mi.loc, wid, hei, mi.parent, mi.angle, null, 0);
				if (ni == null) return;
				if (IOTool.isGDSInExpandsCells())
					ni.setExpanded();
				mi.instantiated = true;
			}
		}

		private static void buildInstances()
		{
			for(Iterator it = allInstances.keySet().iterator(); it.hasNext(); )
			{
				Cell cell = (Cell)it.next();
				makeCellInstances(cell);
			}
		}
	}

	/**
	 * Method to import a library from disk.
	 * @param lib the library to fill
	 * @return true on error.
	 */
	protected boolean importALibrary(Library lib)
	{
		// initialize
		MakeInstance.init();
		theLibrary = lib;

		try
		{
			loadFile();
		} catch (IOException e)
		{
			System.out.println("ERROR reading GDS file");
		}

		// now build all instances recursively
		MakeInstance.buildInstances();
		MakeInstance.term();
		return false;
	}

	private void initialize()
	{
		layerNodeProto = Generic.tech.drcNode;

		theVertices = new Point2D[MAXPOINTS];
		for(int i=0; i<MAXPOINTS; i++) theVertices[i] = new Point2D.Double();
		recordCount = 0;

		// get the array of GDS names
		layerNames = new HashMap();
		pinLayers = new HashSet();
		boolean valid = false;
		curTech = Technology.getCurrent();
		for(Iterator it = curTech.getLayers(); it.hasNext(); )
		{
			Layer layer = (Layer)it.next();
			String gdsName = layer.getGDSLayer();
			if (gdsName != null && gdsName.length() > 0)
			{
				String [] numberStrings = gdsName.split(",");
				for(int i=0; i<numberStrings.length; i++)
				{
					String numberString = numberStrings[i].trim();
					if (numberString.length() == 0) continue;
					int layNum = TextUtils.atoi(numberString);
					if (layNum == 0 && !numberString.equals("0")) continue;
					Integer lay = new Integer(layNum);
					if (layerNames.get(lay) == null) layerNames.put(lay, layer);
					if (numberString.endsWith("p")) pinLayers.add(lay);
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
	private List parseLayerNumbers(String layerNumbers)
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

	private void loadFile()
		throws IOException
	{
		initialize();

		getToken();
		readHeader();
		getToken();
		readLibrary();
		getToken();
		while (isMember(theToken, optionSet))
		{
			if (theToken == GDS_REFLIBS) readRefLibs(); else
				if (theToken == GDS_FONTS) readFonts(); else
					if (theToken == GDS_ATTRTABLE) readAttrTable(); else
						if (theToken == GDS_GENERATIONS) readGenerations();
		}
		while (theToken != GDS_UNITS)
			getToken();
		readUnits();
		getToken();
		while (theToken != GDS_ENDLIB)
		{
			readStructure();
			getToken();
		}
	}

	private void readHeader()
		throws IOException
	{
		if (theToken != GDS_HEADER) handleError("GDS II header statement is missing");

		getToken();
		if (theToken != GDS_SHORT_NUMBER) handleError("GDS II version number is not decipherable");

		// version "tokenValue16"
	}

	private void readLibrary()
		throws IOException
	{
		if (theToken != GDS_BGNLIB) handleError("Begin library statement is missing");

		getToken();
		String createTime = determineTime();
		String modTime = determineTime();
		if (theToken == GDS_LIBNAME)
		{
			getToken();
			if (theToken != GDS_IDENT) handleError("Library name is missing");
			String libraryName = tokenString;
		}
	}

	private void readRefLibs()
		throws IOException
	{
		getToken();
		getToken();
	}

	private void readFonts()
		throws IOException
	{
		getToken();
		getToken();
	}

	private void readAttrTable()
		throws IOException
	{
		getToken();
		if (theToken == GDS_IDENT)
		{
			getToken();
		}
	}

	private void readUnits()
		throws IOException
	{
		if (theToken != GDS_UNITS) handleError("Units statement is missing");

		getToken();
		if (theToken != GDS_REALNUM) handleError("Units statement has invalid number format");

		double dbUnit = tokenValueDouble;
		getToken();
		double meterUnit = tokenValueDouble;
		theScale = meterUnit * 1000000.0 * TextUtils.convertFromDistance(1, curTech, TextUtils.UnitScale.MICRO);
	}

	private void readStructure()
		throws IOException
	{
		beginStructure();
		getToken();
		while (theToken != GDS_ENDSTR)
		{
			getElement();
			getToken();
		}
	}

	private void beginStructure()
		throws IOException
	{
		if (theToken != GDS_BGNSTR) handleError("Begin structure statement is missing");

		getToken();
		String createTime = determineTime();
		String modTime = determineTime();
		if (theToken != GDS_STRNAME) handleError("Strname statement is missing");

		getToken();
		if (theToken != GDS_IDENT) handleError("Structure name is missing");

		// look for this nodeproto
		theCell = findCell(tokenString);
		if (theCell == null)
		{
			// create the proto
			theCell = Cell.newInstance(theLibrary, tokenString);
			if (theCell == null) handleError("Failed to create structure");
			System.out.println("Reading " + tokenString);
			if (theLibrary.getCurCell() == null)
				theLibrary.setCurCell(theCell);
		}
	}

	private Cell findCell(String name)
	{
		return theLibrary.findNodeProto(name);
	}

	private void getElement()
		throws IOException
	{
		while (isMember(theToken, shapeSet))
		{
			if (theToken == GDS_AREF) determineARef(); else
				if (theToken == GDS_SREF) determineSRef(); else
					if (theToken == GDS_BOUNDARY) determineShape(); else
						if (theToken == GDS_PATH) determinePath(); else
							if (theToken == GDS_NODE) determineNode(); else
								if (theToken == GDS_TEXTSYM) determineText(); else
									if (theToken == GDS_BOX) determineBox();
		}

		while (theToken == GDS_PROPATTR)
			determineProperty();
		if (theToken != GDS_ENDEL) handleError("Element end statement is missing");
	}

	private void determineARef()
		throws IOException
	{
		getToken();
		readUnsupported(unsupportedSet);
		if (theToken != GDS_SNAME) handleError("Array reference name is missing");

		getToken();

		// get this nodeproto
		getPrototype(tokenString);
		getToken();
		int angle = 0;
		boolean trans = false;
		if (theToken == GDS_STRANS)
		{
			ReadOrientation ro = new ReadOrientation();
			ro.doIt();
			angle = ro.angle;
			trans = ro.trans;
		}
		int nCols = 0, nRows = 0;
		if (theToken == GDS_COLROW)
		{
			getToken();
			nCols = tokenValue16;
			getToken();
			nRows = tokenValue16;
			getToken();
		}
		if (theToken != GDS_XY) handleError("Array reference has no parameters");
		getToken();
		int n = determinePoints(3, 3);

		Point2D colInterval = new Point2D.Double(0, 0);
		if (nCols != 1)
		{
			colInterval.setLocation((theVertices[1].getX() - theVertices[0].getX()) / nCols, (theVertices[1].getY() - theVertices[0].getY()) / nCols);
			makeTransform(colInterval, angle, trans);
		}
		Point2D rowInterval = new Point2D.Double(0, 0);
		if (nRows != 1)
		{
			rowInterval.setLocation((theVertices[2].getX() - theVertices[0].getX()) / nRows, (theVertices[2].getY() - theVertices[0].getY()) / nRows);
			makeTransform(rowInterval, angle, trans);
		}

		boolean mY = false;
		boolean mX = false;
		if (trans)
		{
			mY = true;
			angle = (angle + 900) % 3600;
		}

		// now generate the array
		double ptcX = theVertices[0].getX();
		double ptcY = theVertices[0].getY();
		for (int ic = 0; ic < nCols; ic++)
		{
			double ptX = ptcX;
			double ptY = ptcY;
			for (int ir = 0; ir < nRows; ir++)
			{
				// create the node
				if (IOTool.isGDSInInstantiatesArrays() ||
					(ir == 0 && ic == 0) ||
						(ir == (nRows-1) && ic == (nCols-1)))
				{
					Point2D loc = new Point2D.Double(ptX, ptY);
					new MakeInstance(theCell, (Cell)theNodeProto, loc, mX, mY, angle);
				}

				// add the row displacement
				ptX += rowInterval.getX();   ptY += rowInterval.getY();
			}

			// add displacement
			ptcX += colInterval.getX();   ptcY += colInterval.getY();
		}
	}

	private void makeTransform(Point2D delta, int angle, boolean trans)
	{
		AffineTransform xform = NodeInst.pureRotate(angle, trans);
		xform.transform(delta, delta);
	}

	private class ReadOrientation
	{
		private int angle;
		private boolean trans;
		private double scale;

		private void doIt()
			throws IOException
		{
			double anglevalue = 0.0;
			scale = 1.0;
			boolean mirror_x = false;
			getToken();
			if (theToken != GDS_FLAGSYM) handleError("Structure reference is missing its flags field");
			if ((tokenFlags&0100000) != 0) mirror_x = true;
			getToken();
			if (theToken == GDS_MAG)
			{
				getToken();
				scale = tokenValueDouble;
				getToken();
			}
			if (theToken == GDS_ANGLE)
			{
				getToken();
				anglevalue = tokenValueDouble * 10;
				getToken();
			}
			angle = ((int)anglevalue) % 3600;
			trans = mirror_x;
			if (trans)
				angle = (2700 - angle) % 3600;

			// should not happen...*/
			if (angle < 0) angle = angle + 3600;
		}
	}

	private void determineSRef()
		throws IOException
	{
		getToken();
		readUnsupported(unsupportedSet);
		if (theToken != GDS_SNAME) handleError("Structure reference name is missing");

		getToken();
		getPrototype(tokenString);
		getToken();
		int angle = 0;
		boolean trans = false;
		if (theToken == GDS_STRANS)
		{
			ReadOrientation ro = new ReadOrientation();
			ro.doIt();
			angle = ro.angle;
			trans = ro.trans;
		}
		if (theToken != GDS_XY) handleError("Structure reference has no translation value");
		getToken();
		int n = determinePoints(1, 1);

		Point2D loc = new Point2D.Double(theVertices[0].getX(), theVertices[0].getY());
		boolean mY = false;
		if (trans)
		{
			mY = true;
			angle = (angle + 900) % 3600;
		}
		new MakeInstance(theCell, (Cell)theNodeProto, loc, false, mY, angle);
	}

	private void determineShape()
		throws IOException
	{
		getToken();
		readUnsupported(unsupportedSet);
		determineLayer();
		getToken();
		if (theToken != GDS_XY) handleError("Boundary has no points");

		getToken();
		int n = determinePoints(3, MAXPOINTS);
		determineBoundary(n);
	}

	private void determineBoundary(int npts)
		throws IOException
	{
		boolean is90 = true;
		boolean is45 = true;
		for (int i=0; i<npts-1 && i<MAXPOINTS-1; i++)
		{
			double dx = theVertices[i+1].getX() - theVertices[i].getX();
			double dy = theVertices[i+1].getY() - theVertices[i].getY();
			if (dx != 0 && dy != 0)
			{
				is90 = false;
				if (Math.abs(dx) != Math.abs(dy)) is45 = false;
			}
		}

		ShapeType perimeter = SHAPELINE;
		if (theVertices[0].getX() == theVertices[npts-1].getX() &&
			theVertices[0].getY() == theVertices[npts-1].getY())
				perimeter = SHAPECLOSED;
		ShapeType oclass = SHAPEOBLIQUE;
		if (perimeter == SHAPECLOSED && (is90 || is45))
			oclass = SHAPEPOLY;
		if (npts == 5 && is90 && perimeter == SHAPECLOSED)
			oclass = SHAPERECTANGLE;

		if (oclass == SHAPERECTANGLE)
		{
			readBox();

			// create the rectangle
			if (layerUsed)
			{
				Point2D ctr = new Point2D.Double((theVertices[0].getX()+theVertices[1].getX())/2, (theVertices[0].getY()+theVertices[1].getY())/2);
				double sX = Math.abs(theVertices[1].getX() - theVertices[0].getX());
				double sY = Math.abs(theVertices[1].getY() - theVertices[0].getY());
				NodeInst ni = NodeInst.makeInstance(layerNodeProto, ctr, sX, sY, theCell);
				if (ni == null) handleError("Failed to create RECTANGLE");
			}
			return;
		}

		if (oclass == SHAPEOBLIQUE || oclass == SHAPEPOLY)
		{
			if (!layerUsed) return;

			// determine the bounds of the polygon
			double lx = theVertices[0].getX();
			double hx = theVertices[0].getX();
			double ly = theVertices[0].getY();
			double hy = theVertices[0].getY();
			for (int i=1; i<npts;i++)
			{
				if (lx > theVertices[i].getX()) lx = theVertices[i].getX();
				if (hx < theVertices[i].getX()) hx = theVertices[i].getX();
				if (ly > theVertices[i].getY()) ly = theVertices[i].getY();
				if (hy < theVertices[i].getY()) hy = theVertices[i].getY();
			}

			// now create the node
			NodeInst ni = NodeInst.makeInstance(layerNodeProto, new Point2D.Double((lx+hx)/2, (ly+hy)/2), hx-lx, hy-ly, theCell);
			if (ni == null) handleError("Failed to create POLYGON");

			// store the trace information
			double cx = (hx + lx) / 2;
			double cy = (hy + ly) / 2;
			Point2D [] points = new Point2D[npts];
			for(int i=0; i<npts; i++)
			{
				points[i] = new Point2D.Double(theVertices[i].getX() - cx, theVertices[i].getY() - cy);
			}

			// store the trace information
			ni.newVar(NodeInst.TRACE, points);
			return;
		}
	}

	private void readBox()
	{
		double pxm = theVertices[4].getX();
		double pxs = theVertices[4].getX();
		double pym = theVertices[4].getY();
		double pys = theVertices[4].getY();
		for (int i = 0; i<4; i++)
		{
			if (theVertices[i].getX() > pxm) pxm = theVertices[i].getX();
			if (theVertices[i].getX() < pxs) pxs = theVertices[i].getX();
			if (theVertices[i].getY() > pym) pym = theVertices[i].getY();
			if (theVertices[i].getY() < pys) pys = theVertices[i].getY();
		}
		theVertices[0].setLocation(pxs, pys);
		theVertices[1].setLocation(pxm, pym);
	}

	private void determinePath()
		throws IOException
	{
		int endcode = 0;
		getToken();
		readUnsupported(unsupportedSet);
		determineLayer();
		getToken();
		if (theToken == GDS_PATHTYPE)
		{
			getToken();
			endcode = tokenValue16;
			getToken();
		}
		double width = 0;
		if (theToken == GDS_WIDTH)
		{
			getToken();
			width = tokenValue32 * theScale;
			getToken();
		}
		double bgnextend = (endcode == 0 || endcode == 4 ? 0 : width/2);
		double endextend = bgnextend;
		if (theToken == GDS_BGNEXTN)
		{
			getToken();
			if (endcode == 4)
				bgnextend = tokenValue32 * theScale;
			getToken();
		}
		if (theToken == GDS_ENDEXTN)
		{
			getToken();
			if (endcode == 4)
				endextend = tokenValue32 * theScale;
			getToken();
		}
		if (theToken == GDS_XY)
		{
			getToken();
			int n = determinePoints(2, MAXPOINTS);

			// construct the path
			for (int i=0; i < n-1; i++)
			{
				Point2D fromPt = theVertices[i];
				Point2D toPt = theVertices[i+1];

				// determine whether either end needs to be shrunk
				double fextend = width / 2;
				double textend = fextend;
				int thisAngle = GenMath.figureAngle(fromPt, toPt);
				if (i > 0)
				{
					Point2D prevPoint = theVertices[i-1];
					int lastAngle = GenMath.figureAngle(prevPoint, fromPt);
					if (Math.abs(thisAngle-lastAngle) % 900 != 0)
					{
						int ang = Math.abs(thisAngle-lastAngle) / 10;
						if (ang > 180) ang = 360 - ang;
						if (ang > 90) ang = 180 - ang;
						fextend = ArcInst.getExtendFactor(width, ang);
					}
				} else
				{
					fextend = bgnextend;
				}
				if (i+1 < n-1)
				{
					Point2D nextPoint = theVertices[i+2];
					int nextAngle = GenMath.figureAngle(toPt, nextPoint);
					if (Math.abs(thisAngle-nextAngle) % 900 != 0)
					{
						int ang = Math.abs(thisAngle-nextAngle) / 10;
						if (ang > 180) ang = 360 - ang;
						if (ang > 90) ang = 180 - ang;
						textend = ArcInst.getExtendFactor(width, ang);
					}
				} else
				{
					textend = endextend;
				}

				// handle arbitrary angle path segment
				if (layerUsed)
				{
					// determine shape of segment
					double length = fromPt.distance(toPt);
					Poly poly = Poly.makeEndPointPoly(length, width, GenMath.figureAngle(fromPt, toPt), fromPt, fextend, toPt, textend);

					// make the node for this segment
					Rectangle2D polyBox = poly.getBox();
					if (polyBox != null)
					{
						NodeInst ni = NodeInst.makeInstance(layerNodeProto, new Point2D.Double(polyBox.getCenterX(), polyBox.getCenterY()),
							polyBox.getWidth(), polyBox.getHeight(), theCell);
						if (ni == null) handleError("Failed to create outline");
					} else
					{
						polyBox = poly.getBounds2D();
						double cx = polyBox.getCenterX();
						double cy = polyBox.getCenterY();
						NodeInst ni = NodeInst.makeInstance(layerNodeProto, new Point2D.Double(cx, cy),
							polyBox.getWidth(), polyBox.getHeight(), theCell);
						if (ni == null) handleError("Failed to create outline");

						// store the trace information
						Point2D [] polyPoints = poly.getPoints();
						Point2D [] points = new Point2D[polyPoints.length];
						for(int j=0; j<polyPoints.length; j++)
						{
							points[j] = new Point2D.Double(polyPoints[j].getX() - cx, polyPoints[j].getY() - cy);
						}

						// store the trace information
						ni.newVar(NodeInst.TRACE, points);
					}
				}
			}
		} else
		{
			handleError("Path element has no points");
		}
	}

	private void determineNode()
		throws IOException
	{
		getToken();
		readUnsupported(unsupportedSet);
		if (theToken != GDS_LAYER) handleError("Boundary has no points");
		getToken();
		if (theToken == GDS_SHORT_NUMBER)
		{
			setLayer(tokenValue16);
			getToken();
		}

		// should do something with node type???
		if (theToken == GDS_NODETYPE)
		{
			getToken();
			getToken();
		}

		// make a dot
		if (theToken != GDS_XY) handleError("Boundary has no points");

		getToken();
		determinePoints(1, 1);

		// create the node
		NodeInst ni = NodeInst.makeInstance(layerNodeProto, theVertices[0], 0, 0, theCell);
		if (ni == null) handleError("Failed to create NODE");
	}

	private void determineText()
		throws IOException
	{
		getToken();
		readUnsupported(unsupportedSet);
		determineLayer();
		getToken();
		int vert_just = -1;
		int horiz_just = -1;
		if (theToken == GDS_PRESENTATION)
		{
			Point just = determineJustification();
			vert_just = just.x;
			horiz_just = just.y;
		}
		if (theToken == GDS_PATHTYPE)
		{
			getToken();
			// pathcode = tokenValue16;
			getToken();
		}
		if (theToken == GDS_WIDTH)
		{
			getToken();
			// pathwidth = tokenValue32 * theScale;
			getToken();
		}
		int angle = 0;
		boolean trans = false;
		double scale = 1.0;
		String textString = "";
		for(;;)
		{
			if (theToken == GDS_STRANS)
			{
				ReadOrientation ro = new ReadOrientation();
				ro.doIt();
				angle = ro.angle;
				trans = ro.trans;
				scale = ro.scale;
				continue;
			}
			if (theToken == GDS_XY)
			{
				getToken();
				int n = determinePoints(1, 1);
				continue;
			}
			if (theToken == GDS_ANGLE)
			{
				getToken();
				angle = (int)(tokenValueDouble * 10.0);
				getToken();
				continue;
			}
			if (theToken == GDS_STRING)
			{
				if (recordCount != 0)
				{
					getToken();
					textString = tokenString;
				}
				getToken();
				break;
			}
			if (theToken == GDS_MAG)
			{
				getToken();
				getToken();
				continue;
			}
			handleError("Text element has no reference point");
			break;
		}
		readText(textString, vert_just, horiz_just, angle, trans, scale);
	}

	private void readText(String charstring, int vjust, int hjust, int angle, boolean trans, double scale)
		throws IOException
	{
		// stop if layer invalid
		if (!layerUsed) return;

		// handle pins specially 
		if (layerIsPin)
		{
//			NodeProto np = layerNodeProto;
			NodeProto np = Generic.tech.universalPinNode;
			NodeInst ni = NodeInst.makeInstance(np, theVertices[0], np.getDefWidth(), np.getDefHeight(), theCell);
			if (ni == null) handleError("Could not create pin marker");
			if (ni.getNumPortInsts() > 0)
			{
				PortInst pi = ni.getPortInst(0);
				Export.newInstance(theCell, pi, charstring);
			}
			return;
		}

		// stop of not handling text in GDS
		if (!IOTool.isGDSInIncludesText()) return;
		double x = theVertices[0].getX() + MINFONTWIDTH * charstring.length();
		double y = theVertices[0].getY() + MINFONTHEIGHT;
		theVertices[1].setLocation(x, y);

		// create a holding node
		NodeInst ni = NodeInst.makeInstance(layerNodeProto, theVertices[0],
			0, 0, theCell, angle, charstring, 0);
		if (ni == null) handleError("Could not create text marker");

		// set the text size and orientation
		MutableTextDescriptor td = ni.getMutableTextDescriptor(NodeInst.NODE_NAME_TD);
		int size = (int)scale;
		if (size <= 0) size = 8;
		if (size > TextDescriptor.Size.TXTMAXPOINTS) size = TextDescriptor.Size.TXTMAXPOINTS;
		td.setAbsSize(size);

		// determine the presentation
		td.setPos(TextDescriptor.Position.CENT);
		switch (vjust)
		{
			case 1:		// top
				switch (hjust)
				{
					case 1:  td.setPos(TextDescriptor.Position.UPRIGHT); break;
					case 2:  td.setPos(TextDescriptor.Position.UPLEFT);  break;
					default: td.setPos(TextDescriptor.Position.UP);      break;
				}
				break;

			case 2:		// bottom
				switch (hjust)
				{
					case 1:  td.setPos(TextDescriptor.Position.DOWNRIGHT); break;
					case 2:  td.setPos(TextDescriptor.Position.DOWNLEFT);  break;
					default: td.setPos(TextDescriptor.Position.DOWN);      break;
				}
				break;

			default:	// centered
				switch (hjust)
				{
					case 1:  td.setPos(TextDescriptor.Position.RIGHT); break;
					case 2:  td.setPos(TextDescriptor.Position.LEFT);  break;
					default: td.setPos(TextDescriptor.Position.CENT);  break;
				}
		}
		ni.setTextDescriptor(NodeInst.NODE_NAME_TD, td);
	}

	/**
	 * untested feature, I don't have a box type
	 */
	private void determineBox()
		throws IOException
	{
		getToken();
		readUnsupported(unsupportedSet);
		determineLayer();
		if (theToken != GDS_XY) handleError("Boundary has no points");

		getToken();
		determinePoints(2, MAXPOINTS);
		if (layerUsed)
		{
if (layerIsPin) System.out.println("PIN in BOX");
			// create the box
			NodeInst ni = NodeInst.makeInstance(layerNodeProto, theVertices[0], 0, 0, theCell);
			if (ni == null) handleError("Failed to create box");
		}
	}

	private void setLayer(int layerNum)
	{
		layerUsed = true;
		layerIsPin = false;
		Integer layerInt = new Integer(layerNum);
		Layer layer = (Layer)layerNames.get(layerInt);
		if (layer == null)
		{
			if (IOTool.isGDSInIgnoresUnknownLayers())
			{
				System.out.println("GDS layer " + layerNum + " unknown, ignoring it");
			} else
			{
				System.out.println("GDS layer " + layerNum + " unknown, using Generic:DRC");
			}
			layerNames.put(new Integer(layerNum), Generic.tech.drc_lay);
			layerUsed = false;
		}
		if (layer == null) layerNodeProto = null; else
		{
			if (layer == Generic.tech.drc_lay)
			{
				if (IOTool.isGDSInIgnoresUnknownLayers()) layerUsed = false;
			}
			if (pinLayers.contains(layerInt)) layerIsPin = true;
			layerNodeProto = layer.getPureLayerNode();
		}
	}

	private void determineLayer()
		throws IOException
	{
		if (theToken != GDS_LAYER) handleError("Layer statement is missing");

		getToken();
		if (theToken != GDS_SHORT_NUMBER) handleError("Invalid layer number");

		setLayer(tokenValue16);
		getToken();
		if (!isMember(theToken, maskSet)) handleError("No datatype field");

		getToken();
		if (tokenValue16 != 0)
		{
			// ignored value
			int sublayer = tokenValue16;
		}
	}

	/**
	 * Method to get the justification information into a Point.
	 * @return a point whose "x" is the vertical justification and whose
	 * "y" is the horizontal justification.
	 */
	private Point determineJustification()
		throws IOException
	{
		Point just = new Point();
		getToken();
		if (theToken != GDS_FLAGSYM) handleError("Array reference has no parameters");

		int font_libno = tokenFlags & 0x0030;
		font_libno = font_libno >> 4;
		just.x = tokenFlags & 0x000C;
		just.x = just.x >> 2;
		just.y = tokenFlags & 0x0003;
		getToken();
		return just;
	}

	private void determineProperty()
		throws IOException
	{
		getToken();
		getToken();
		if (theToken != GDS_PROPVALUE) handleError("Property has no value");

		getToken();
		String propvalue = tokenString;

		// add to the current structure as a variable?
		getToken();
	}

	private void getPrototype(String name)
		throws IOException
	{
		// scan for this proto
		Cell np = findCell(name);
		if (np == null)
		{
			// FILO order, create this nodeproto
			np = Cell.newInstance(theLibrary, tokenString);
			if (np == null) handleError("Failed to create SREF proto");
            progress.setNote("Reading " + tokenString);
		}

		// set the reference node proto
		theNodeProto = np;
	}

	private void readGenerations()
		throws IOException
	{
		getToken();
		if (theToken != GDS_SHORT_NUMBER) handleError("Generations value is invalid");
		getToken();
	}

	private boolean isMember(GSymbol tok, GSymbol [] set)
	{
		for(int i=0; i<set.length; i++)
			if (set[i] == tok) return true;
		return false;
	}

	private void getToken()
		throws IOException
	{
		if (recordCount == 0)
		{
			valuetype = readRecord();
		} else
		{
			if (valuetype == TYPEFLAGS)
			{
				tokenFlags = getWord();
				theToken = GDS_FLAGSYM;
				return;
			}
			if (valuetype == TYPESHORT)
			{
				tokenValue16 = getWord();
				theToken = GDS_SHORT_NUMBER;
				return;
			}
			if (valuetype == TYPELONG)
			{
				tokenValue32 = getInteger();
				theToken = GDS_NUMBER;
				return;
			}
			if (valuetype == TYPEFLOAT)
			{
				tokenValueDouble = getFloat();
				theToken = GDS_REALNUM;
				return;
			}
			if (valuetype == TYPEDOUBLE)
			{
				tokenValueDouble = getDouble();
				theToken = GDS_REALNUM;
				return;
			}
			if (valuetype == TYPESTRING)
			{
				tokenString = getString();
				theToken = GDS_IDENT;
				return;
			}
			if (valuetype == TYPEERR) handleError("Invalid GDS II datatype");
		}
	}

	private DatatypeSymbol readRecord()
		throws IOException
	{
		int dataword = getWord();
		recordCount = dataword - 2;
		int recordtype = getByte() & 0xFF;
		theToken = GSymbol.findSymbol(recordtype);

		DatatypeSymbol datatypeSymbol = TYPEERR;
		switch (getByte())
		{
			case 0:  datatypeSymbol = TYPENONE;     break;
			case 1:  datatypeSymbol = TYPEFLAGS;    break;
			case 2:  datatypeSymbol = TYPESHORT;    break;
			case 3:  datatypeSymbol = TYPELONG;     break;
			case 4:  datatypeSymbol = TYPEFLOAT;    break;
			case 5:  datatypeSymbol = TYPEDOUBLE;   break;
			case 6:  datatypeSymbol = TYPESTRING;   break;
		}
		return datatypeSymbol;
	}

	private void handleError(String msg)
		throws IOException
	{
		System.out.println("Error: " + msg + " at byte " + byteCount);
		throw new IOException();
	}

	private void readUnsupported(GSymbol bad_op_set[])
		throws IOException
	{
		if (isMember(theToken, bad_op_set))
		{
			do
			{
				getToken();
			} while (!isMember(theToken, goodOpSet));
		}
	}

	private int determinePoints(int min_points, int max_points)
		throws IOException
	{
		int point_counter = 0;
		while (theToken == GDS_NUMBER)
		{
			double x = tokenValue32 * theScale;
			getToken();
			double y = tokenValue32 * theScale;
			theVertices[point_counter].setLocation(x, y);
			point_counter++;
			if (point_counter > max_points)
			{
				System.out.println("Found " + point_counter + " points (too many)");
				handleError("Too many points in the shape");
			}
			getToken();
		}
		if (point_counter < min_points)
		{
			System.out.println("Found " + point_counter + " points (too few)");
			handleError("Not enough points in the shape");
		}
		return point_counter;
	}

	private String determineTime()
		throws IOException
	{
		String [] time_array = new String[7];
		for (int i = 0; i < 6; i++)
		{
			if (theToken != GDS_SHORT_NUMBER) handleError("Date value is not a valid number");

			if (i == 0 && tokenValue16 < 1900)
			{
				// handle Y2K date issues
				if (tokenValue16 > 60) tokenValue16 += 1900; else
					tokenValue16 += 2000;
			}
			time_array[i] = Integer.toString(tokenValue16);
			getToken();
		}
		return time_array[1] + "-" + time_array[2] + "-" + time_array[0] + " at " + time_array[3] + ":" + time_array[4] + ":" + time_array[5];
	}

	private float getFloat()
		throws IOException
	{
		int reg = getByte() & 0xFF;
		int sign = 1;
		if ((reg & 0x00000080) != 0) sign = -1;
		reg = reg & 0x0000007F;

		// generate the exponent, currently in Excess-64 representation
		int binary_exponent = (reg - 64) << 2;
		reg = (getByte() & 0xFF) << 16;
		int dataword = getWord();
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
			return (float)(sign * reg * makePower(2, binary_exponent));
		}
		if (binary_exponent < 0)
			return (float)(sign * reg / makePower(2, -binary_exponent));
		return (float)(sign * reg);
	}

	private static double makePower(int val, int power)
	{
		double result = 1.0;
		for(int count=0; count<power; count++)
			result *= val;
		return result;
	}

	private double getDouble()
		throws IOException
	{
		// first byte is the exponent field (hex)
		int register1 = getByte() & 0xFF;

		// plus sign bit
		double realValue = 1;
		if ((register1 & 0x00000080) != 0) realValue = -1.0;

		// the hex exponent is in excess 64 format
		register1 = register1 & 0x0000007F;
		int exponent = register1 - 64;

		// bytes 2-4 are the high ordered bits of the mantissa
		register1 = (getByte() & 0xFF) << 16;
		int dataword = getWord();
		register1 = register1 + dataword;

		// next word completes the matissa (1/16 to 1)
		int long_integer = getInteger();
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
		realValue *= (register1 * twoTo32 + register2) * twoToNeg56;
		if (exponent > 0)
		{
			realValue = realValue * makePower(16, exponent);
		} else
		{
			if (exponent < 0)
				realValue = realValue / makePower(16, -exponent);
		}
		return realValue;
	}

	private String getString()
		throws IOException
	{
		StringBuffer sb = new StringBuffer();
		while (recordCount != 0)
		{
			char letter = (char)getByte();
			if (letter != 0) sb.append(letter);
		}
		return sb.toString();
	}

	private int getInteger()
		throws IOException
	{
		int highWord = getWord();
		int lowWord = getWord();
		return (highWord << 16) | lowWord;
	}

	private int getWord()
		throws IOException
	{
		int highByte = getByte() & 0xFF;
		int lowByte = getByte() & 0xFF;
		return (highByte << 8) | lowByte;
	}

	private byte getByte()
		throws IOException
	{
		byte b = dataInputStream.readByte();
		updateProgressDialog(1);
		recordCount--;
		return b;
	}

}
