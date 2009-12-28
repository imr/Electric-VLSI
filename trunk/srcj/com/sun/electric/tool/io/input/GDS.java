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
 * the Free Software Foundation; either version 3 of the License, or
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

import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.database.geometry.DBMath;
import com.sun.electric.database.geometry.EPoint;
import com.sun.electric.database.geometry.ERectangle;
import com.sun.electric.database.geometry.GenMath;
import com.sun.electric.database.geometry.GeometryHandler;
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.geometry.PolyMerge;
import com.sun.electric.database.geometry.PolySweepMerge;
import com.sun.electric.database.geometry.GenMath.MutableInteger;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.EDatabase;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.id.CellId;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.Geometric;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.RTBounds;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.MutableTextDescriptor;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.technology.ArcProto;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.SizeOffset;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.Technology.NodeLayer;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.GDSLayers;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.ncc.basic.NccCellAnnotations;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

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
 * <LI>MAG - no scaling is possible, must create a separate object for each value, don't scale.  (TEXT does scale.)</LI>
 * </UL>
 */
public class GDS extends Input
{
	private static final boolean DEBUGALL = false;				/* true for debugging */
	private static final boolean SHOWPROGRESS = false;			/* true for debugging */
	private static final boolean IGNOREIMMENSECELLS = false;	/* true for debugging */
	private static final boolean TALLYCONTENTS = false;			/* true for debugging */

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

	private int              countBox, countText, countNode, countPath, countShape, countSRef, countARef, countATotal;
	private Library          theLibrary;
    private Map<Library,Cell> currentCells;
	private CellBuilder      theCell;
	private NodeProto        theNodeProto;
	private PrimitiveNode    layerNodeProto;
	private UnknownLayerMessage currentUnknownLayerMessage;
    private PrimitiveNode    pinNodeProto;
	private int              randomLayerSelection;
	private boolean          layerIsPin;
	private Technology       curTech;
	private int              recordCount;
	private int              curLayerNum, curLayerType;
	private GSymbol          theToken;
	private DatatypeSymbol   valuetype;
	private int              tokenFlags;
	private int              tokenValue16;
	private int              tokenValue32;
	private double           tokenValueDouble;
	private String           tokenString;
	private Point2D []       theVertices;
	private int              numVertices;
	private double           theScale;
	private Map<Integer,Layer> layerNames;
	private Map<Integer,UnknownLayerMessage> layerErrorMessages;
	private static Map<UnknownLayerMessage,Set<Cell>> cellLayerErrors;
	private Set<Integer>     pinLayers;
	private PolyMerge        merge;
	private static boolean   arraySimplificationUseful;

	private static class GSymbol
	{
		private int value;
		private static List<GSymbol> symbols = new ArrayList<GSymbol>();

		private GSymbol(int value)
		{
			this.value = value;
			symbols.add(this);
		}

		private static GSymbol findSymbol(int value)
		{
			for(GSymbol gs : symbols)
			{
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
//	private static final GSymbol GDS_SPACING      = new GSymbol(24);
	private static final GSymbol GDS_STRING       = new GSymbol(25);
	private static final GSymbol GDS_STRANS       = new GSymbol(26);
	private static final GSymbol GDS_MAG          = new GSymbol(27);
	private static final GSymbol GDS_ANGLE        = new GSymbol(28);
//	private static final GSymbol GDS_UINTEGER     = new GSymbol(29);
//	private static final GSymbol GDS_USTRING      = new GSymbol(30);
	private static final GSymbol GDS_REFLIBS      = new GSymbol(31);
	private static final GSymbol GDS_FONTS        = new GSymbol(32);
	private static final GSymbol GDS_PATHTYPE     = new GSymbol(33);
	private static final GSymbol GDS_GENERATIONS  = new GSymbol(34);
	private static final GSymbol GDS_ATTRTABLE    = new GSymbol(35);
//	private static final GSymbol GDS_STYPTABLE    = new GSymbol(36);
//	private static final GSymbol GDS_STRTYPE      = new GSymbol(37);
	private static final GSymbol GDS_ELFLAGS      = new GSymbol(38);
//	private static final GSymbol GDS_ELKEY        = new GSymbol(39);
//	private static final GSymbol GDS_LINKTYPE     = new GSymbol(40);
//	private static final GSymbol GDS_LINKKEYS     = new GSymbol(41);
	private static final GSymbol GDS_NODETYPE     = new GSymbol(42);
	private static final GSymbol GDS_PROPATTR     = new GSymbol(43);
	private static final GSymbol GDS_PROPVALUE    = new GSymbol(44);
	private static final GSymbol GDS_BOX          = new GSymbol(45);
	private static final GSymbol GDS_BOXTYPE      = new GSymbol(46);
	private static final GSymbol GDS_PLEX         = new GSymbol(47);
	private static final GSymbol GDS_BGNEXTN      = new GSymbol(48);
	private static final GSymbol GDS_ENDEXTN      = new GSymbol(49);
//	private static final GSymbol GDS_TAPENUM      = new GSymbol(50);
//	private static final GSymbol GDS_TAPECODE     = new GSymbol(51);
//	private static final GSymbol GDS_STRCLASS     = new GSymbol(52);
//	private static final GSymbol GDS_NUMTYPES     = new GSymbol(53);
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

	private GDSPreferences localPrefs;

	public static class GDSPreferences extends InputPreferences
    {
		public double inputScale;
		public boolean simplifyCells;
		public int arraySimplification;
		public boolean instantiateArrays;
		public boolean expandCells;
		public boolean mergeBoxes;
		public boolean includeText;
		public int unknownLayerHandling;
		public boolean cadenceCompatibility;

		public GDSPreferences(boolean factory) { super(factory); }

		public void initFromUserDefaults()
		{
			inputScale = IOTool.getGDSInputScale();
			simplifyCells = IOTool.isGDSInSimplifyCells();
			arraySimplification = IOTool.getGDSArraySimplification();
			instantiateArrays = IOTool.isGDSInInstantiatesArrays();
			expandCells = IOTool.isGDSInExpandsCells();
			mergeBoxes = IOTool.isGDSInMergesBoxes();
			includeText = IOTool.isGDSInIncludesText();
			unknownLayerHandling = IOTool.getGDSInUnknownLayerHandling();
			cadenceCompatibility = IOTool.isGDSCadenceCompatibility();
		}

        @Override
        public Library doInput(URL fileURL, Library lib, Technology tech, Map<Library,Cell> currentCells, Map<CellId,BitSet> nodesToExpand, Job job)
        {
        	GDS in = new GDS(this);
			if (in.openBinaryInput(fileURL)) return null;

            // Librarys before loading
            HashSet oldLibs = new HashSet();
            for (Iterator<Library> it = Library.getLibraries(); it.hasNext(); )
                oldLibs.add(it.next());
            oldLibs.remove(lib);

			lib = in.importALibrary(lib, tech, currentCells);
			in.closeInput();

            if (expandCells) {
                // Expand subCells
                EDatabase database = EDatabase.currentDatabase();
                for (Iterator<Library> it = Library.getLibraries(); it.hasNext(); ) {
                    Library l = it.next();
                    if (oldLibs.contains(l)) continue;
                    for (Iterator<Cell> cit = l.getCells(); cit.hasNext(); ) {
                        Cell cell =cit.next();
                        for (Iterator<NodeInst> nit = cell.getNodes(); nit.hasNext(); ) {
                            NodeInst ni = nit.next();
                            if (ni.isCellInstance())
                                database.addToNodes(nodesToExpand, ni);
                        }
                    }
                }
            }

			return lib;
        }
    }

	/**
	 * Creates a new instance of GDS.
	 */
	GDS(GDSPreferences ap) { localPrefs = ap; }

	/**
	 * Method to import a library from disk.
	 * @param lib the library to fill
     * @param currentCells this map will be filled with currentCells in Libraries found in library file
	 * @return the created library (null on error).
	 */
    @Override
	protected Library importALibrary(Library lib, Technology tech, Map<Library,Cell> currentCells)
	{
		// initialize
        this.currentCells = currentCells;
		arraySimplificationUseful = false;
		CellBuilder.init();
		theLibrary = lib;
		curTech = tech;
		initialize();

		try
		{
			loadFile();
        } catch (IllegalArgumentException e)
        {
            System.out.println("ERROR reading GDS file: " + e.getMessage());
            if (Job.getDebug())
                e.printStackTrace();
            return null;
        }
        catch (Exception e)
		{
            System.out.println("ERROR reading GDS file: check input file, " + e.getMessage());
            if (Job.getDebug())
                e.printStackTrace();
            return null;
        }

		// now build all instances recursively
		CellBuilder.buildInstances();
		CellBuilder.term();

		// show unknown error messages
		for(UnknownLayerMessage message : cellLayerErrors.keySet())
		{
			Set<Cell> cellList = cellLayerErrors.get(message);
			System.out.println(message.message + " in cells:");
			String prev = "    ";
            int count = 0;
            for(Cell cell : cellList)
			{
				System.out.print(prev + cell.describe(false));
				prev = ", ";
                // break into lines otherwise the message line is too long
                if (count > 10)
                {
                    count = 0;
                    System.out.print("\n\t");
                }
                count++;
            }
			System.out.println();
		}

		if (arraySimplificationUseful)
		{
			System.out.println("NOTE: Found array references that could be simplified to save space and time");
			System.out.println("   To simplify arrays, set the 'Input array simplification' in GDS Preferences");
		}
		return lib;
	}

	private void initialize()
	{
		layerNodeProto = Generic.tech().drcNode;

		theVertices = new Point2D[MAXPOINTS];
		for(int i=0; i<MAXPOINTS; i++) theVertices[i] = new Point2D.Double();
		recordCount = 0;

		// get the array of GDS names
		layerNames = new HashMap<Integer,Layer>();
		layerErrorMessages = new HashMap<Integer,UnknownLayerMessage>();
		cellLayerErrors = new HashMap<UnknownLayerMessage,Set<Cell>>();
		pinLayers = new HashSet<Integer>();
		randomLayerSelection = 0;
		boolean valid = false;

        for(Map.Entry<Layer,String> e: curTech.getGDSLayers().entrySet())
		{
			Layer layer = e.getKey();
            String gdsName = e.getValue();
            GDSLayers gdsl = GDSLayers.parseLayerString(gdsName);
            for(Iterator<Integer> lIt = gdsl.getLayers(); lIt.hasNext(); ) {
                Integer lVal = lIt.next();
                Integer lay = new Integer(lVal.intValue());
                if (layerNames.get(lay) == null)
                	layerNames.put(lay, layer);
            }
            if (gdsl.getPinLayer() != -1) {
                pinLayers.add(new Integer(gdsl.getPinLayer()));
                layerNames.put(new Integer(gdsl.getPinLayer()), layer);
            }
            if (gdsl.getTextLayer() != -1)
                layerNames.put(new Integer(gdsl.getTextLayer()), layer);
            valid = true;
		}
		if (!valid)
		{
			System.out.println("There are no GDS layer names assigned in the " + curTech.getTechName() + " technology");
		}
	}

    private static class CellBuilder
    {
		private static Map<Cell,CellBuilder> allBuilders;
		private static Set<Cell>        cellsTooComplex;
		private GDSPreferences localPrefs;
		private Technology tech;
		private Cell cell;
        private List<MakeInstance> insts = new ArrayList<MakeInstance>();
        private Map<UnknownLayerMessage,List<MakeInstance>> allErrorInsts = new HashMap<UnknownLayerMessage,List<MakeInstance>>();
        private List<MakeInstanceArray> instArrays = new ArrayList<MakeInstanceArray>();

        private CellBuilder(Cell cell, Technology tech, GDSPreferences localPrefs) {
            this.cell = cell;
            this.tech = tech;
            this.localPrefs = localPrefs;
            allBuilders.put(cell, this);
        }

		private void makeInstance(NodeProto proto, Point2D loc, Orientation orient, double wid, double hei,
			EPoint[] points, UnknownLayerMessage ulm)
		{
			MakeInstance mi = null;
			if (proto != null)
			{
	            mi = new MakeInstance(proto, loc, orient, wid, hei, points, null, null, localPrefs);
				if (ulm == null)
				{
					insts.add(mi);
					return;
				}
			}
			List<MakeInstance> errorList = allErrorInsts.get(ulm);
			if (errorList == null) allErrorInsts.put(ulm, errorList = new ArrayList<MakeInstance>());
			errorList.add(mi);
        }

		private void makeInstanceArray(NodeProto proto, int nCols, int nRows, Orientation orient,
			Point2D startLoc, Point2D rowOffset, Point2D colOffset)
		{
            MakeInstanceArray mia = new MakeInstanceArray(proto, nCols, nRows, orient,
            	new Point2D.Double(startLoc.getX(), startLoc.getY()), rowOffset, colOffset, localPrefs);
            instArrays.add(mia);
        }

		private void makeExport(NodeProto proto, Point2D loc, Orientation orient,
			String exportName, UnknownLayerMessage ulm)
		{
			MakeInstance mi = null;
			if (proto != null && proto.getNumPorts() > 0)
			{
				double wid = proto.getDefWidth();
				double hei = proto.getDefHeight();
	            mi = new MakeInstance(proto, loc, orient, wid, hei, null, exportName, null, localPrefs);
				if (ulm == null)
				{
		            insts.add(mi);
		            return;
				}
			}
			List<MakeInstance> errorList = allErrorInsts.get(ulm);
			if (errorList == null) allErrorInsts.put(ulm, errorList = new ArrayList<MakeInstance>());
			errorList.add(mi);
        }

		private void makeText(NodeProto proto, Point2D loc, String text,
			TextDescriptor textDescriptor, UnknownLayerMessage ulm)
		{
			MakeInstance mi = null;
			if (proto != null)
			{
				mi = new MakeInstance(proto, loc, Orientation.IDENT, 0, 0, null, null, Name.findName(text), localPrefs);
				if (ulm == null)
				{
					insts.add(mi);
					return;
				}
			}
			List<MakeInstance> errorList = allErrorInsts.get(ulm);
			if (errorList == null) allErrorInsts.put(ulm, errorList = new ArrayList<MakeInstance>());
			errorList.add(mi);
        }

		private static void init()
		{
			allBuilders = new HashMap<Cell,CellBuilder>();
			cellsTooComplex = new HashSet<Cell>();
		}

		private static void term()
		{
			allBuilders = null;
			if (cellsTooComplex.size() > 0)
			{
				System.out.print("THESE CELLS WERE TOO COMPLEX AND NOT FULLY READ:");
				for(Cell cell : cellsTooComplex) System.out.print(" " + cell.describe(false));
				System.out.println();
			}
		}

		private void makeInstances(Set<Cell> builtCells)
		{
            if (builtCells.contains(cell)) return;
            builtCells.add(cell);
			boolean countOff = false;
			if (SHOWPROGRESS || IGNOREIMMENSECELLS)
			{
				int size = insts.size();
				int arraySize = instArrays.size();
				System.out.println("Building cell " + this.cell.describe(false) +
					" with " + size + " single instances and " + arraySize + " arrayed instances");
				if (size+arraySize >= 100000)
				{
					countOff = true;

					// ignore internal contents when cell is very large (for testing only)
					if (IGNOREIMMENSECELLS)
					{
						cellsTooComplex.add(cell);
						MakeInstance ll = null, ul = null, lr = null, ur = null;
						for(MakeInstance mi : insts)
						{
							if (ll == null) ll = ul = lr = ur = mi;
							if (mi.loc.getX() <= ll.loc.getX() && mi.loc.getY() <= ll.loc.getY()) ll = mi;
							if (mi.loc.getX() <= ul.loc.getX() && mi.loc.getY() >= ul.loc.getY()) ul = mi;
							if (mi.loc.getX() >= lr.loc.getX() && mi.loc.getY() <= lr.loc.getY()) lr = mi;
							if (mi.loc.getX() >= ur.loc.getX() && mi.loc.getY() >= ur.loc.getY()) ur = mi;
						}
						insts.clear();
						instArrays.clear();
						insts.add(ll);
						if (!insts.contains(ul)) insts.add(ul);
						if (!insts.contains(lr)) insts.add(lr);
						if (!insts.contains(ur)) insts.add(ur);
					}
				}
			}

            nameInstances(countOff);
           	Collections.sort(insts);

           	// make a set of export names
           	Set<String> exportNames = new HashSet<String>();
			for(MakeInstance mi : insts)
				if (mi.exportName != null) exportNames.add(mi.exportName);
			for(UnknownLayerMessage ulm : allErrorInsts.keySet())
			{
				List<MakeInstance> errorList = allErrorInsts.get(ulm);
				for(MakeInstance mi : errorList)
					if (mi != null && mi.exportName != null) exportNames.add(mi.exportName);
			}

			int count = 0;
			int renamed = 0;
			Map<String,String> exportUnify = new HashMap<String,String>();

			// first make the geometry and instances
			for(MakeInstance mi : insts)
			{
				if (mi.exportName != null) continue;
				if (countOff && ((++count % 1000) == 0))
					System.out.println("        Made " + count + " instances");
                if (mi.proto instanceof Cell) {
                    Cell subCell = (Cell)mi.proto;
                    CellBuilder cellBuilder = allBuilders.get(subCell);
                    if (cellBuilder != null)
                        cellBuilder.makeInstances(builtCells);
                }

				// make the instance
                if (mi.instantiate(this.cell, exportUnify, exportNames, null)) renamed++;
			}

			// next make the exports
			for(MakeInstance mi : insts)
			{
				if (mi.exportName == null) continue;
				if (countOff && ((++count % 1000) == 0))
					System.out.println("        Made " + count + " instances");
                if (mi.proto instanceof Cell) {
                    Cell subCell = (Cell)mi.proto;
                    CellBuilder cellBuilder = allBuilders.get(subCell);
                    if (cellBuilder != null)
                        cellBuilder.makeInstances(builtCells);
                }

                if (localPrefs.cadenceCompatibility)
                {
	                // center the export if possible
	                ArcProto theArc = mi.proto.getPort(0).getBasePort().getConnections()[0];
	                Layer theLayer = theArc.getLayer(0);
	                NodeProto theProto = theLayer.getPureLayerNode();
	                Rectangle2D search = new Rectangle2D.Double(mi.loc.getX(), mi.loc.getY(), 0, 0);
	        		for(Iterator<RTBounds> it = this.cell.searchIterator(search); it.hasNext(); )
	        		{
	        			Geometric geom = (Geometric)it.next();
	        			if (geom instanceof NodeInst)
	        			{
	        				NodeInst ni = (NodeInst)geom;
	        				if (ni.getProto() != theProto) continue;
							Rectangle2D pointBounds = ni.getBounds();
							double cX = pointBounds.getCenterX();
							double cY = pointBounds.getCenterY();
	        				EPoint [] trace = ni.getTrace();
	        				if (trace != null)
	        				{
								Point2D [] newPoints = new Point2D[trace.length];
								for(int i=0; i<trace.length; i++)
								{
									if (trace[i] != null)
										newPoints[i] = new Point2D.Double(trace[i].getX()+cX, trace[i].getY()+cY);
								}
								PolyBase poly = new PolyBase(newPoints);
								poly.transform(ni.rotateOut());
	        					if (poly.contains(mi.loc))
	        					{
	        						GeometryHandler thisMerge = GeometryHandler.createGeometryHandler(GeometryHandler.GHMode.ALGO_SWEEP, 1);
	        						thisMerge.add(theLayer, poly);
	        						thisMerge.postProcess(true);
	        			            Collection<PolyBase> set = ((PolySweepMerge)thisMerge).getPolyPartition(theLayer);
	        						for(PolyBase simplePoly : set)
	        						{
	        							Rectangle2D polyBounds = simplePoly.getBounds2D();
	        							if (polyBounds.contains(mi.loc))
	        							{
	    	        						mi.loc.setLocation(polyBounds.getCenterX(), polyBounds.getCenterY());
	    	        						break;
	        							}
	        						}
	        						break;
	        					}
	        				} else
	        				{
	        					if (pointBounds.contains(mi.loc))
	        					{
	        						mi.loc.setLocation(cX, cY);
	        						break;
	        					}
	        				}
	        			}
	        		}

	        		// grid-align the export location
	        		double scaledResolution = tech.getFactoryScaledResolution();
	        		if (scaledResolution > 0)
	        		{
						double x = Math.round(mi.loc.getX() / scaledResolution) * scaledResolution;
						double y = Math.round(mi.loc.getY() / scaledResolution) * scaledResolution;
						mi.loc.setLocation(x, y);
	        		}
                }

                // make the instance
                if (mi.instantiate(this.cell, exportUnify, exportNames, null)) renamed++;
			}

			for(UnknownLayerMessage ulm : allErrorInsts.keySet())
			{
        		List<Geometric> instantiated = new ArrayList<Geometric>();
				List<MakeInstance> errorList = allErrorInsts.get(ulm);
				for(MakeInstance mi : errorList)
				{
					if (mi == null) continue;
					if (countOff && ((++count % 1000) == 0))
						System.out.println("        Made " + count + " instances");
	                if (mi.proto instanceof Cell) {
	                    Cell subCell = (Cell)mi.proto;
	                    CellBuilder cellBuilder = allBuilders.get(subCell);
	                    if (cellBuilder != null)
	                        cellBuilder.makeInstances(builtCells);
	                }

					// make the instance
	                if (mi.instantiate(this.cell, exportUnify, exportNames, instantiated)) renamed++;
				}
				String msg = "Cell " + this.cell.noLibDescribe() + ": " + ulm.message;
				Set<Cell> cellsWithError = cellLayerErrors.get(ulm);
				if (cellsWithError == null) cellLayerErrors.put(ulm, cellsWithError = new TreeSet<Cell>());
				cellsWithError.add(cell);
//				System.out.println(msg);
				errorLogger.logMessage(msg, instantiated, cell, -1, true);
			}

			Map<NodeProto,List<EPoint>> massiveMerge = new HashMap<NodeProto,List<EPoint>>();
			for(MakeInstanceArray mia : instArrays)
			{
				if (countOff && ((++count % 1000) == 0))
					System.out.println("        Made " + count + " instances");
                if (mia.proto instanceof Cell) {
                    Cell subCell = (Cell)mia.proto;
                    CellBuilder cellBuilder = allBuilders.get(subCell);
                    if (cellBuilder != null)
                        cellBuilder.makeInstances(builtCells);
                }

				// make the instance array
                mia.instantiate(this, this.cell, massiveMerge);
			}
			List<NodeProto> mergeNodeSet = new ArrayList<NodeProto>();
			for(NodeProto np : massiveMerge.keySet()) mergeNodeSet.add(np);
			for(NodeProto np : mergeNodeSet)
			{
				// place a pure-layer node that embodies all arrays for the whole cell
				List<EPoint> points = massiveMerge.get(np);
				buildComplexNode(points, np, this.cell);
			}
			if (renamed > 0)
			{
				System.out.println("Cell " + this.cell.describe(false) + ": Renamed and NCC-unified " + renamed +
					" exports with duplicate names");
				Map<String,String> unifyStrings = new HashMap<String,String>();
				Set<String> finalNames = exportUnify.keySet();
				for(String finalName : finalNames)
				{
					String singleName = exportUnify.get(finalName);
					String us = unifyStrings.get(singleName);
					if (us == null) us = singleName;
					us += " " + finalName;
					unifyStrings.put(singleName, us);
				}
				List<String> annotations = new ArrayList<String>();
				for(String us : unifyStrings.keySet())
					annotations.add("exportsConnectedByParent " + unifyStrings.get(us));
				if (annotations.size() > 0)
				{
					String [] anArr = new String[annotations.size()];
					for(int i=0; i<annotations.size(); i++) anArr[i] = annotations.get(i);
					TextDescriptor td = TextDescriptor.getCellTextDescriptor().withInterior(true).withDispPart(TextDescriptor.DispPos.NAMEVALUE);
					this.cell.newVar(NccCellAnnotations.NCC_ANNOTATION_KEY, anArr, td);
				}
			}
            if (localPrefs.simplifyCells)
                simplifyNodes(this.cell, tech);
			builtCells.add(this.cell);
		}

        /** Method to see if existing primitive nodes could be merged and define more complex nodes
         * such as contacts
         */
        private void simplifyNodes(Cell cell, Technology tech)
        {
            Map<Layer, List<NodeInst>> map = new HashMap<Layer, List<NodeInst>>();

            for (Iterator<NodeInst> itNi = cell.getNodes(); itNi.hasNext();)
            {
                NodeInst ni = itNi.next();
                if (!(ni.getProto() instanceof PrimitiveNode)) continue; // not primitive
                PrimitiveNode pn = (PrimitiveNode)ni.getProto();
                if (pn.getFunction() != PrimitiveNode.Function.NODE) continue; // not pure layer node.
                Layer layer = pn.getLayerIterator().next(); // they are supposed to have only 1
                List<NodeInst> list = map.get(layer);

                if (list == null) // first time
                {
                    list = new ArrayList<NodeInst>();
                    map.put(layer, list);
                }
                list.add(ni);
            }

            Set<NodeInst> toDelete = new HashSet<NodeInst>();
            Set<NodeInst> viaToDelete = new HashSet<NodeInst>();
            List<Geometric> geomList = new ArrayList<Geometric>();

            for (Iterator<PrimitiveNode> itPn = tech.getNodes(); itPn.hasNext();)
            {
                PrimitiveNode pn = itPn.next();
                boolean allFound = true;
                if (!pn.getFunction().isContact()) continue; // only dealing with metal contacts for now.

                Layer m1Layer = null, m2Layer = null;
                Layer viaLayer = null;
                SizeOffset so = pn.getProtoSizeOffset();

                for (Iterator<Layer> itLa = pn.getLayerIterator(); itLa.hasNext();)
                {
                    Layer l = itLa.next();
                    if (map.get(l) == null)
                    {
                        allFound = false;
                        break;
                    }
                    if (l.getFunction().isMetal())
                    {
                        if (m1Layer == null)
                            m1Layer = l;
                        else
                            m2Layer = l;
                    }
                    else if (l.getFunction().isContact())
                        viaLayer = l;
                }
                if (!allFound) continue; // not all layers for this particular node found
                if (viaLayer == null) continue; // not metal contact
                assert(m1Layer != null);
                List<NodeInst> list = map.get(m1Layer);
                assert(list != null);
                Layer.Function.Set thisLayer = new Layer.Function.Set(viaLayer.getFunction());
                List<NodeInst> viasList = map.get(viaLayer);

                for (NodeInst ni : list)
                {
                    Poly[] polys = tech.getShapeOfNode(ni, true, false, null);
                    assert(polys.length == 1); // it must be only 1
                    Poly m1P = polys[0];
                    List<NodeInst> nList = map.get(m2Layer);
                    if (nList == null) continue; // nothing found in m2Layer
                    for (NodeInst n : nList)
                    {
                        Poly[] otherPolys = tech.getShapeOfNode(n, true, false, null);
                        assert(otherPolys.length == 1); // it must be only 1
                        Poly m2P = otherPolys[0];
                        if (!m2P.getBounds2D().equals(m1P.getBounds2D())) continue; // no match

                        ImmutableNodeInst d = ni.getD();
                        String name = ni.getName();
                        int atIndex = name.indexOf('@');
                        if (atIndex < 0) name += "tmp"; else
                        	name = name.substring(0, atIndex) + "tmp" + name.substring(atIndex);
                        NodeInst newNi = NodeInst.makeInstance(pn, d.anchor,
                                m2P.getBounds2D().getWidth() + so.getLowXOffset() + so.getHighXOffset(),
                                m2P.getBounds2D().getHeight() + so.getLowYOffset() + so.getHighYOffset(),
                                ni.getParent(), ni.getOrient(), name);
                        if (newNi == null) continue;

                        // Searching for vias to delete
                        assert(viasList != null);
                        Poly[] viaPolys = tech.getShapeOfNode(newNi, true, false, thisLayer);
                        boolean found = false;

                        // Can be more than 1 due to MxN cuts
                        viaToDelete.clear();
                        for (int i = 0; i < viaPolys.length; i++)
                        {
                            Poly poly = viaPolys[i];
                            Rectangle2D bb = poly.getBounds2D();
                            bb.setRect(ERectangle.fromLambda(bb));
                            found = false;

                            for (NodeInst viaNi : viasList)
                            {
                                Poly[] thisViaList = tech.getShapeOfNode(viaNi, true, false, thisLayer);
                                assert(thisViaList.length == 1);
                                // hack to get rid of the resolution issue
                                Poly p = thisViaList[0];
                                Rectangle2D b = p.getBounds2D();
                                b.setRect(ERectangle.fromLambda(b));
                                if (thisViaList[0].polySame(poly))
                                {
                                    viaToDelete.add(viaNi);
                                    assert(!found);
                                    found = true;
                                }
                            }
                            if (!found)
                            {
                                break; // fail to find all nodes
                            }
                        }
                        if (!found) // rolling back new node
                        {
                            newNi.kill();
                        }
                        else
                        {
                            if (SHOWPROGRESS)
                                System.out.println("Adding " + newNi.getName());
                            toDelete.clear();
                            geomList.clear();
                            toDelete.add(ni);
                            toDelete.add(n);
                            toDelete.addAll(viaToDelete);
                            String message = toDelete.size() + " nodes were replaced for more complex primitives in cell '" + cell.getName() + "'";
                            geomList.add(newNi);
                            errorLogger.logMessage(message, geomList, cell, -1, false);
                            // Deleting now replaced pure primitives
                            cell.killNodes(toDelete);
                        }
                    }
                }
            }
        }

        private void nameInstances(boolean countOff)
        {
            Map<String,GenMath.MutableInteger> maxSuffixes = new HashMap<String,GenMath.MutableInteger>();
            Set<String> userNames = new HashSet<String>();
            MutableInteger count = new MutableInteger(0);
            for (MakeInstance mi: insts)
            	nameInstance(mi, countOff, count, userNames, maxSuffixes);
			for(UnknownLayerMessage ulm : allErrorInsts.keySet())
			{
				List<MakeInstance> errorList = allErrorInsts.get(ulm);
				for(MakeInstance mi : errorList)
	            	if (mi != null) nameInstance(mi, countOff, count, userNames, maxSuffixes);
			}
        }

        private void nameInstance(MakeInstance mi, boolean countOff, MutableInteger count,
        	Set<String> userNames, Map<String,GenMath.MutableInteger> maxSuffixes)
        {
        	count.increment();
			if (countOff && ((count.intValue() % 2000) == 0))
				System.out.println("        Named " + count + " instances");
            if (mi.nodeName != null)
            {
                if (!validGdsNodeName(mi.nodeName))
                {
                    System.out.println("  Warning: Node name '" + mi.nodeName + "' in cell " + cell.describe(false) +
                        " is bad (" + Name.checkName(mi.nodeName.toString()) + ")...ignoring the name");
                } else if (!userNames.contains(mi.nodeName.toString()))
                {
                    userNames.add(mi.nodeName.toString());
                    return;
                }
            }
            Name baseName;
            if (mi.proto instanceof Cell) baseName = ((Cell)mi.proto).getBasename(); else
            {
                PrimitiveNode np = (PrimitiveNode)mi.proto;
                baseName = np.getFunction().getBasename();
            }
            String basenameString = baseName.toString();
            GenMath.MutableInteger maxSuffix = maxSuffixes.get(basenameString);
            if (maxSuffix == null)
            {
                maxSuffix = new GenMath.MutableInteger(-1);
                maxSuffixes.put(basenameString, maxSuffix);
            }
            maxSuffix.increment();
            mi.nodeName = baseName.findSuffixed(maxSuffix.intValue());
        }

        private boolean validGdsNodeName(Name name)
        {
            return name.isValid() && !name.hasEmptySubnames() && !name.isBus() || !name.isTempname();
        }

		private static void buildInstances()
		{
			Set<Cell> builtCells = new HashSet<Cell>();
			for(CellBuilder cellBuilder : allBuilders.values())
				cellBuilder.makeInstances(builtCells);
		}
    }

    /**
     * Class to save instance array information.
     */
    private static class MakeInstanceArray
    {
    	private NodeProto proto;
    	private int nCols, nRows;
    	private Orientation orient;
    	private Point2D startLoc, rowOffset, colOffset;
    	private GDSPreferences localPrefs;

    	private MakeInstanceArray(NodeProto proto, int nCols, int nRows, Orientation orient, Point2D startLoc,
    		Point2D rowOffset, Point2D colOffset, GDSPreferences localPrefs)
    	{
    		this.proto = proto;
    		this.nCols = nCols;
    		this.nRows = nRows;
    		this.orient = orient;
    		this.startLoc = startLoc;
    		this.rowOffset = rowOffset;
    		this.colOffset = colOffset;
    		this.localPrefs = localPrefs;
    	}

    	/**
         * Method to instantiate an array of cell instances.
         * @param parent the Cell in which to create the geometry.
         */
        private void instantiate(CellBuilder theCell, Cell parent, Map<NodeProto,List<EPoint>> massiveMerge)
        {
        	int arraySimplification = localPrefs.arraySimplification;
            NodeInst subNi = null;
            Cell subCell = (Cell)proto;
            int numArcs = subCell.getNumArcs();
            int numNodes = subCell.getNumNodes();
            int numExports = subCell.getNumPorts();
            if (numArcs == 0 && numExports == 0 && numNodes == 1)
            {
                subNi = subCell.getNode(0);
                if (subNi.getProto().getFunction() != PrimitiveNode.Function.NODE)
                    subNi = null;
            }
            if (subNi != null && subNi.getTrace() != null) subNi = null;
            if (subNi != null)
            {
                if (arraySimplification > 0)
                {
                    List<EPoint> points = buildArray();
                    if (arraySimplification == 2)
                    {
                        // add the array's geometry the layer's outline
                        List<EPoint> soFar = massiveMerge.get(subNi.getProto());
                        if (soFar == null)
                        {
                            soFar = new ArrayList<EPoint>();
                            massiveMerge.put(subNi.getProto(), soFar);
                        }
                        if (soFar.size() > 0) soFar.add(null);
                        for(EPoint ep : points) soFar.add(ep);
                    } else
                    {
                        // place a pure-layer node that embodies the array
                        buildComplexNode(points, subNi.getProto(), parent);
                    }
                    return;
                } else
                {
                    // remember that array simplification would have helped
                    arraySimplificationUseful = true;
                }
            }

    		// generate an array
    		double ptcX = startLoc.getX();
    		double ptcY = startLoc.getY();
    		for (int ic = 0; ic < nCols; ic++)
    		{
    			double ptX = ptcX;
    			double ptY = ptcY;
    			for (int ir = 0; ir < nRows; ir++)
    			{
    				// create the node
    				if (localPrefs.instantiateArrays ||
    					(ir == 0 && ic == 0) ||
    						(ir == (nRows-1) && ic == (nCols-1)))
    				{
    					Point2D loc = new Point2D.Double(ptX, ptY);
    		            NodeInst ni = NodeInst.makeInstance(proto, loc, proto.getDefWidth(), proto.getDefHeight(), parent, orient, null);
//    		            if (ni != null)
//    		            {
//	    		            if (localPrefs.expandCells && ni.isCellInstance())
//	    		                ni.setExpanded(true);
//    		            }
    				}

    				// add the row displacement
    				ptX += rowOffset.getX();   ptY += rowOffset.getY();
    			}

    			// add displacement
    			ptcX += colOffset.getX();   ptcY += colOffset.getY();
    		}
        }

        private List<EPoint> buildArray()
        {
			List<EPoint> points = new ArrayList<EPoint>();
			Rectangle2D bounds = ((Cell)proto).getBounds();
			Rectangle2D boundCopy = new Rectangle2D.Double(bounds.getMinX(), bounds.getMinY(), bounds.getWidth(), bounds.getHeight());
			DBMath.transformRect(boundCopy, orient.pureRotate());
    		double ptcX = startLoc.getX();
    		double ptcY = startLoc.getY();
    		for (int ic = 0; ic < nCols; ic++)
    		{
    			double ptX = ptcX;
    			double ptY = ptcY;
    			for (int ir = 0; ir < nRows; ir++)
    			{
    	    		points.add(new EPoint(ptX+boundCopy.getMinX(), ptY+boundCopy.getMinY()));
    	    		points.add(new EPoint(ptX+boundCopy.getMaxX(), ptY+boundCopy.getMinY()));
    	    		points.add(new EPoint(ptX+boundCopy.getMaxX(), ptY+boundCopy.getMaxY()));
    	    		points.add(new EPoint(ptX+boundCopy.getMinX(), ptY+boundCopy.getMaxY()));

    	    		// insert a "break" marker to start a new polygon
    	    		if (ic < nCols-1 || ir < nRows-1) points.add(null);

    				// add the row displacement
    				ptX += rowOffset.getX();   ptY += rowOffset.getY();
    			}

    			// add displacement
    			ptcX += colOffset.getX();   ptcY += colOffset.getY();
    		}
			return points;
        }
    }

    private static class MakeInstance implements Comparable<MakeInstance>
	{
		private NodeProto proto;
		private Point2D loc;
		private Orientation orient;
        private double wid, hei;
        private EPoint[] points; // trace
        private String exportName; // export
        private Name nodeName; // text
        private String origNodeName; // original text with invalid name
        private GDSPreferences localPrefs;

        private MakeInstance(NodeProto proto, Point2D loc, Orientation orient, double wid, double hei, EPoint[] points,
        	String exportName, Name nodeName, GDSPreferences localPrefs)
		{
			this.proto = proto;
			this.loc = loc;
            this.orient = orient;
            this.wid = DBMath.round(wid);
            this.hei = DBMath.round(hei);
            this.points = points;
            this.exportName = exportName;
            if (nodeName != null && !nodeName.isValid())
            {
                origNodeName = nodeName.toString();
            	if (origNodeName.equals("[@instanceName]"))
            	{
            		origNodeName = null;
            		nodeName = null;
            	} else if (origNodeName.endsWith(":"))
            	{
            		nodeName = Name.findName(origNodeName.substring(0, origNodeName.length()-1));
            		if (nodeName.isValid()) origNodeName = null;
            	} else
            	{
            		nodeName = null;
            	}
            }
            this.nodeName = nodeName;
            this.localPrefs = localPrefs;
		}

        public int compareTo(MakeInstance that) {
            return TextUtils.STRING_NUMBER_ORDER.compare(this.nodeName.toString(), that.nodeName.toString());
        }

        /**
         * Method to instantiate a node/export in a Cell.
         * @param parent the Cell in which to create the geometry.
         * @param exportUnify a map that shows how renamed exports connect.
         * @param saveHere a list of Geometrics to save this instance in.
         * @return true if the export had to be renamed.
         */
        private boolean instantiate(Cell parent, Map<String,String> exportUnify, Set<String> exportNames, List<Geometric> saveHere)
        {
        	String name = null;
        	if (nodeName != null) name = nodeName.toString();
            NodeInst ni = NodeInst.makeInstance(proto, loc, wid, hei, parent, orient, name);
            String errorMsg = null;
            if (ni == null) return false;
            if (saveHere != null) saveHere.add(ni);

            if (ni.getNameKey() != nodeName)
            {
                errorMsg = "Cell " + parent.describe(false) + ": GDS name '" + name + "' renamed to '" + ni.getName() + "'";
            }
            else if (origNodeName != null)
            {
           		errorMsg = "Cell " + parent.describe(false) + ": Original GDS name of '" + name + "' was '" + origNodeName + "'";
            }

            if (errorMsg != null)
            {
                List<Geometric> geomList = new ArrayList<Geometric>(1);
                geomList.add(ni);
                errorLogger.logMessage(errorMsg, geomList, parent, -1, false);
                System.out.println(errorMsg);
            }

//            if (localPrefs.expandCells && ni.isCellInstance())
//                ni.setExpanded(true);
            if (points != null && GenMath.getAreaOfPoints(points) != wid*hei)
                ni.setTrace(points);
            boolean renamed = false;
            if (exportName != null)
            {
            	if (exportName.endsWith(":"))
            		exportName = exportName.substring(0, exportName.length()-1);
        		if (parent.findExport(exportName) != null)
        		{
                    String newName = ElectricObject.uniqueObjectName(exportName, parent, PortProto.class, true, true);
//                	while (exportNames.contains(newName))
//                	{
//                		int lastUnder = newName.lastIndexOf('_');
//                		if (lastUnder > 0 && TextUtils.isANumber(newName.substring(lastUnder+1)))
//                		{
//                			newName = newName.substring(0, lastUnder+1) + (TextUtils.atoi(newName.substring(lastUnder+1))+1);
//                		} else
//                		{
//                			newName += "_1";
//                		}
//                	}
//System.out.println("  Warning: Multiple exports called '" + exportName + "' in cell " +
//	parent.describe(false) + " (renamed to " + newName + ")");
                    exportUnify.put(newName, exportName);
                    exportName = newName;
                    renamed = true;
        		}
                Export.newInstance(parent, ni.getPortInst(0), exportName);
            }
            return renamed;
        }
	}

	private void loadFile()
		throws IOException
	{
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
		determineTime();		// creation time
		determineTime();		// modification time
		if (theToken == GDS_LIBNAME)
		{
			getToken();
			if (theToken != GDS_IDENT) handleError("Library name is missing");
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

		// get the meter unit
		getToken();
		double meterUnit = tokenValueDouble;

		// round the meter unit
		double shift = 1;
		double roundedScale = meterUnit;
		while (roundedScale < 1)
		{
			roundedScale *= 10;
			shift *= 10;
		}
		roundedScale = DBMath.round(roundedScale) / shift;
		meterUnit = roundedScale;

		// compute the scale
		double microScale = TextUtils.convertFromDistance(1, curTech, TextUtils.UnitScale.MICRO);
		theScale = meterUnit * 1000000.0 * microScale * localPrefs.inputScale;

//		// round the scale
//		double shift = 1;
//		double roundedScale = theScale;
//		while (roundedScale < 1)
//		{
//			roundedScale *= 10;
//			shift *= 10;
//		}
//		roundedScale = DBMath.round(roundedScale) / shift;
//		theScale = roundedScale;
	}

	private double scaleValue(double value)
	{
		double result = value * theScale;
		return result;
	}

	private void showResultsOfCell()
	{
		System.out.print("**** Cell "+theCell.cell.describe(false)+" has");
		if (countBox > 0) System.out.print(" "+countBox+" boxes");
		if (countText > 0) System.out.print(" "+countText+" texts");
		if (countNode > 0) System.out.print(" "+countNode+" nodes");
		if (countPath > 0) System.out.print(" "+countPath+" paths");
		if (countShape > 0) System.out.print(" "+countShape+" shapes");
		if (countSRef > 0) System.out.print(" "+countSRef+" instances");
		if (countARef > 0)
			System.out.print(" "+countARef+" arrays with "+countATotal+" elements");
		System.out.println();
	}

	private void readStructure()
		throws IOException
	{
		beginStructure();
		getToken();
		if (localPrefs.mergeBoxes)
		{
			// initialize merge if merging this cell
    		merge = new PolyMerge();
		}

		// read the cell
		countBox = countText = countNode = countPath = countShape = countSRef = countARef = countATotal = 0;
		while (theToken != GDS_ENDSTR)
		{
            getElement();
			getToken();
		}
		if (TALLYCONTENTS) showResultsOfCell();
		if (localPrefs.mergeBoxes)
		{
			// extract merge information for this cell
    		for(Layer layer : merge.getKeySet())
    		{
    			Layer primLayer = layer;
				PrimitiveNode pnp = primLayer.getPureLayerNode();
    			List<PolyBase> polys = merge.getMergedPoints(layer, false);
    			for(PolyBase poly : polys)
    			{
    				Rectangle2D box = poly.getBox();
    				if (box == null)
    				{
        				box = poly.getBounds2D();
    					Point2D ctr = new EPoint(box.getCenterX(), box.getCenterY());

    					// store the trace information
    					Point2D [] pPoints = poly.getPoints();
    					EPoint [] points = new EPoint[pPoints.length];
    					for(int i=0; i<pPoints.length; i++)
    					{
    						points[i] = new EPoint(pPoints[i].getX(), pPoints[i].getY());
    					}

    					// store the trace information
                        theCell.makeInstance(pnp, ctr, Orientation.IDENT, box.getWidth(), box.getHeight(), points, null);
    				} else
    				{
    					Point2D ctr = new EPoint(box.getCenterX(), box.getCenterY());
                        theCell.makeInstance(pnp, ctr, Orientation.IDENT, box.getWidth(), box.getHeight(), null, null);
    				}
    			}
    		}
		}
	}

	private void beginStructure()
		throws IOException
	{
		if (theToken != GDS_BGNSTR) handleError("Begin structure statement is missing");

		getToken();
		determineTime();	// creation time
		determineTime();	// modification time
		if (theToken != GDS_STRNAME) handleError("Strname statement is missing");

		getToken();
		if (theToken != GDS_IDENT) handleError("Structure name is missing");

		// look for this nodeproto
		String name = tokenString + "{lay}";
		Cell cell = findCell(name);
		if (cell == null)
		{
			// create the proto
			cell = Cell.newInstance(theLibrary, name);
			if (cell == null) handleError("Failed to create structure");
			System.out.println("Reading " + name);
			if (!currentCells.containsKey(theLibrary))
				currentCells.put(theLibrary, cell);
		}
        theCell = new CellBuilder(cell, curTech, localPrefs);
	}

	private Cell findCell(String name)
	{
		return theLibrary.findNodeProto(name);
	}

	/**
	 * Method to create a pure-layer node with a complex outline.
	 * @param points the outline description.
	 * @param pureType the type of the pure-layer node.
	 * @param parent the Cell in which to create the node.
	 */
	private static void buildComplexNode(List<EPoint> points, NodeProto pureType, Cell parent)
	{
		EPoint [] pointArray = new EPoint[points.size()];
		double lX=0, hX=0, lY=0, hY=0;
		for(int i=0; i<points.size(); i++)
		{
			pointArray[i] = points.get(i);
			if (pointArray[i] == null) continue;
			if (i == 0)
			{
				lX = hX = pointArray[i].getX();
				lY = hY = pointArray[i].getY();
			} else
			{
				if (pointArray[i].getX() < lX) lX = pointArray[i].getX();
				if (pointArray[i].getX() > hX) hX = pointArray[i].getX();
				if (pointArray[i].getY() < lY) lY = pointArray[i].getY();
				if (pointArray[i].getY() > hY) hY = pointArray[i].getY();
			}
		}
        NodeInst ni = NodeInst.makeInstance(pureType, new Point2D.Double((lX+hX)/2, (lY+hY)/2), hX-lX, hY-lY,
        	parent, Orientation.IDENT, null);
        if (ni != null && GenMath.getAreaOfPoints(pointArray) != (hX-lX)*(hY-lY))
        	ni.setTrace(pointArray);
	}

	private void getElement()
		throws IOException
	{
		while (isMember(theToken, shapeSet))
		{
			if (theToken == GDS_AREF)
			{
				determineARef();
			} else if (theToken == GDS_SREF)
			{
				determineSRef();
			} else if (theToken == GDS_BOUNDARY)
			{
				determineShape();
			} else if (theToken == GDS_PATH)
			{
				determinePath();
			} else if (theToken == GDS_NODE)
			{
				determineNode();
			} else if (theToken == GDS_TEXTSYM)
			{
				determineText();
			} else if (theToken == GDS_BOX)
			{
				determineBox();
			}
		}

		while (theToken == GDS_PROPATTR)
			determineProperty();
		if (theToken != GDS_ENDEL)
		{
			showResultsOfCell();
			handleError("Element end statement is missing");
		}
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
		determinePoints(3, 3);

		// see if the instance is a single object
		if (TALLYCONTENTS)
		{
			countARef++;
			countATotal += nCols*nRows;
			return;
		}
		boolean mY = false;
		boolean mX = false;
		if (trans)
		{
			mY = true;
			angle = (angle + 900) % 3600;
		}

		Point2D colInterval = new Point2D.Double(0, 0);
		if (nCols != 1)
		{
			colInterval.setLocation((theVertices[1].getX() - theVertices[0].getX()) / nCols,
				(theVertices[1].getY() - theVertices[0].getY()) / nCols);
		}
		Point2D rowInterval = new Point2D.Double(0, 0);
		if (nRows != 1)
		{
			rowInterval.setLocation((theVertices[2].getX() - theVertices[0].getX()) / nRows,
				(theVertices[2].getY() - theVertices[0].getY()) / nRows);
		}

		theCell.makeInstanceArray(theNodeProto, nCols, nRows, Orientation.fromJava(angle, mX, mY),
			theVertices[0], rowInterval, colInterval);
        if (DEBUGALL) System.out.println("---- Array Reference: " + nCols + "x" + nRows + " of " + theNodeProto.describe(false));
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
		determinePoints(1, 1);

		if (TALLYCONTENTS)
		{
			countSRef++;
			return;
		}

		Point2D loc = new Point2D.Double(theVertices[0].getX(), theVertices[0].getY());
		boolean mY = false;
		if (trans)
		{
			mY = true;
			angle = (angle + 900) % 3600;
		}
		theCell.makeInstance(theNodeProto, loc, Orientation.fromJava(angle, false, mY), 0, 0, null, null);
        if (DEBUGALL) System.out.println("---- Instance of " + theNodeProto.describe(false) +
        	" at (" + loc.getX() + "," + loc.getY() + ")");
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
		determinePoints(3, MAXPOINTS);
		if (TALLYCONTENTS)
		{
			countShape++;
			return;
		}
		determineBoundary();
        if (DEBUGALL) System.out.println("---- Shape on " + (layerIsPin ? "pin " : "") + "layer " + curLayerNum + "/" + curLayerType +
        	" (" + layerNodeProto + ") has " + numVertices + " points");
	}

	private void determineBoundary()
	{
		boolean is90 = true;
		boolean is45 = true;
		for (int i=0; i<numVertices-1 && i<MAXPOINTS-1; i++)
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
		if (theVertices[0].getX() == theVertices[numVertices-1].getX() &&
			theVertices[0].getY() == theVertices[numVertices-1].getY())
				perimeter = SHAPECLOSED;
		ShapeType oclass = SHAPEOBLIQUE;
		if (perimeter == SHAPECLOSED && (is90 || is45))
			oclass = SHAPEPOLY;
		if (numVertices == 5 && is90 && perimeter == SHAPECLOSED)
			oclass = SHAPERECTANGLE;

		if (oclass == SHAPERECTANGLE)
		{
			readBox();

			// create the rectangle
			Point2D ctr = new Point2D.Double((theVertices[0].getX()+theVertices[1].getX())/2,
				(theVertices[0].getY()+theVertices[1].getY())/2);
			double sX = Math.abs(theVertices[1].getX() - theVertices[0].getX());
			double sY = Math.abs(theVertices[1].getY() - theVertices[0].getY());
			if (localPrefs.mergeBoxes)
			{
				if (layerNodeProto != null)
				{
					PrimitiveNode plnp = layerNodeProto;
					NodeLayer [] layers = plnp.getNodeLayers();
					merge.addPolygon(layers[0].getLayer(), new Poly(ctr.getX(), ctr.getY(), sX, sY));
				}
			} else
			{
                theCell.makeInstance(layerNodeProto, ctr, Orientation.IDENT, sX, sY, null, currentUnknownLayerMessage);
			}
			return;
		}

		if (oclass == SHAPEOBLIQUE || oclass == SHAPEPOLY)
		{
			if (localPrefs.mergeBoxes)
			{
				NodeLayer [] layers = layerNodeProto.getNodeLayers();
				if (layerNodeProto != null)
					merge.addPolygon(layers[0].getLayer(), new Poly(theVertices)); // ??? npts
			} else
			{
				// determine the bounds of the polygon
				double lx = theVertices[0].getX();
				double hx = theVertices[0].getX();
				double ly = theVertices[0].getY();
				double hy = theVertices[0].getY();
				for (int i=1; i<numVertices;i++)
				{
					if (lx > theVertices[i].getX()) lx = theVertices[i].getX();
					if (hx < theVertices[i].getX()) hx = theVertices[i].getX();
					if (ly > theVertices[i].getY()) ly = theVertices[i].getY();
					if (hy < theVertices[i].getY()) hy = theVertices[i].getY();
				}

				// store the trace information
				EPoint [] points = new EPoint[numVertices];
				for(int i=0; i<numVertices; i++)
				{
					points[i] = new EPoint(theVertices[i].getX(), theVertices[i].getY());
				}

				// now create the node
                theCell.makeInstance(layerNodeProto, new EPoint((lx+hx)/2, (ly+hy)/2),
                	Orientation.IDENT, hx-lx, hy-ly, points, currentUnknownLayerMessage);
			}
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
			width = scaleValue(tokenValue32);
			getToken();
		}
		double bgnextend = (endcode == 0 || endcode == 4 ? 0 : width/2);
		double endextend = bgnextend;
		if (theToken == GDS_BGNEXTN)
		{
			getToken();
			if (endcode == 4)
				bgnextend = scaleValue(tokenValue32);
			getToken();
		}
		if (theToken == GDS_ENDEXTN)
		{
			getToken();
			if (endcode == 4)
				endextend = scaleValue(tokenValue32);
			getToken();
		}
		if (theToken == GDS_XY)
		{
			getToken();
			determinePoints(2, MAXPOINTS);

			if (TALLYCONTENTS)
			{
				countPath++;
				return;
			}

			// construct the path
			for (int i=0; i < numVertices-1; i++)
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
						fextend = Poly.getExtendFactor(width, ang);
					}
				} else
				{
					fextend = bgnextend;
				}
				if (i+1 < numVertices-1)
				{
					Point2D nextPoint = theVertices[i+2];
					int nextAngle = GenMath.figureAngle(toPt, nextPoint);
					if (Math.abs(thisAngle-nextAngle) % 900 != 0)
					{
						int ang = Math.abs(thisAngle-nextAngle) / 10;
						if (ang > 180) ang = 360 - ang;
						if (ang > 90) ang = 180 - ang;
						textend = Poly.getExtendFactor(width, ang);
					}
				} else
				{
					textend = endextend;
				}

				// handle arbitrary angle path segment
				double length = fromPt.distance(toPt);
				Poly poly = Poly.makeEndPointPoly(length, width, GenMath.figureAngle(toPt, fromPt),
					fromPt, fextend, toPt, textend, Poly.Type.FILLED);

				if (localPrefs.mergeBoxes)
				{
					if (layerNodeProto != null)
					{
						NodeLayer [] layers = layerNodeProto.getNodeLayers();
						merge.addPolygon(layers[0].getLayer(), poly);
					}
				} else
				{
					// make the node for this segment
					Rectangle2D polyBox = poly.getBox();
					if (polyBox != null)
					{
                        theCell.makeInstance(layerNodeProto, new EPoint(polyBox.getCenterX(),
                        	polyBox.getCenterY()), Orientation.IDENT, polyBox.getWidth(), polyBox.getHeight(), null, currentUnknownLayerMessage);
					} else
					{
						polyBox = poly.getBounds2D();
						double cx = polyBox.getCenterX();
						double cy = polyBox.getCenterY();

						// store the trace information
						Point2D [] polyPoints = poly.getPoints();
						EPoint [] points = new EPoint[polyPoints.length];
						for(int j=0; j<polyPoints.length; j++)
						{
							points[j] = new EPoint(polyPoints[j].getX(), polyPoints[j].getY());
						}

						// store the trace information
                        theCell.makeInstance(layerNodeProto, new EPoint(cx, cy), Orientation.IDENT,
                        	polyBox.getWidth(), polyBox.getHeight(), points, currentUnknownLayerMessage);
					}
				}
			}
		} else
		{
			handleError("Path element has no points");
		}
        if (DEBUGALL) System.out.println("---- Path on " + (layerIsPin ? "pin " : "") + "layer " + curLayerNum + "/" + curLayerType +
        	" (" + layerNodeProto + ") has " + numVertices + " points");
	}

	private void determineNode()
		throws IOException
	{
		getToken();
		readUnsupported(unsupportedSet);
		if (theToken != GDS_LAYER) handleError("Boundary has no points");
		getToken();
		int layerNum = tokenValue16;
		if (theToken == GDS_SHORT_NUMBER)
		{
			getToken();
		}

		// also get node type
		int layerType = tokenValue16;
		if (theToken == GDS_NODETYPE)
		{
			getToken();
			getToken();
		}
		setLayer(layerNum, layerType);

		// make a dot
		if (theToken != GDS_XY) handleError("Boundary has no points");

		getToken();
		determinePoints(1, 1);

		if (TALLYCONTENTS)
		{
			countNode++;
			return;
		}

		// create the node
		if (localPrefs.mergeBoxes)
		{
		} else
		{
            theCell.makeInstance(layerNodeProto, new Point2D.Double(theVertices[0].getX(), theVertices[0].getY()),
            	Orientation.IDENT, 0, 0, null, currentUnknownLayerMessage);
		}
        if (DEBUGALL) System.out.println("---- Node on " + (layerIsPin ? "pin " : "") + "layer " + curLayerNum + "/" + curLayerType +
        	" (" + layerNodeProto + ") at (" + theVertices[0].getX() + "," + theVertices[0].getY() + ")");
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
				determinePoints(1, 1);
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
		if (TALLYCONTENTS)
		{
			countText++;
			return;
		}
		readText(textString, vert_just, horiz_just, angle, trans, scale);
        if (DEBUGALL) System.out.println("---- Text " + (layerIsPin ? "(pin) " : "") + "'" + textString.replace('\n', '/') +
        	"' at (" + theVertices[0].getX() + "," + theVertices[0].getY() + ")");
	}

	private void readText(String charstring, int vjust, int hjust, int angle, boolean trans, double scale)
	{
		// handle pins specially
		if (layerIsPin)
		{
            theCell.makeExport(pinNodeProto, new Point2D.Double(theVertices[0].getX(), theVertices[0].getY()),
            	Orientation.IDENT, charstring, currentUnknownLayerMessage);
			return;
		}

		// stop if not handling text in GDS
		if (!localPrefs.includeText) return;
		double x = theVertices[0].getX() + MINFONTWIDTH * charstring.length();
		double y = theVertices[0].getY() + MINFONTHEIGHT;
		theVertices[1].setLocation(x, y);

		// set the text size and orientation
		MutableTextDescriptor td = MutableTextDescriptor.getNodeTextDescriptor();
		double size = scale;
		if (size <= 0) size = 2;
		if (size > TextDescriptor.Size.TXTMAXQGRID) size = TextDescriptor.Size.TXTMAXQGRID;
		if (size < TextDescriptor.Size.TXTMINQGRID) size = TextDescriptor.Size.TXTMINQGRID;
		td.setRelSize(size);

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
        theCell.makeText(layerNodeProto, new Point2D.Double(theVertices[0].getX(), theVertices[0].getY()),
        	charstring, TextDescriptor.newTextDescriptor(td), currentUnknownLayerMessage);
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
		if (TALLYCONTENTS)
		{
			countBox++;
			return;
		}

		// create the box
		if (localPrefs.mergeBoxes)
		{
			if (layerNodeProto != null)
			{
			}
		} else
		{
            theCell.makeInstance(layerNodeProto, new Point2D.Double(theVertices[0].getX(), theVertices[0].getY()),
            	Orientation.IDENT, 0, 0, null, currentUnknownLayerMessage);
		}
        if (DEBUGALL) System.out.println("---- Box on " + (layerIsPin ? "pin " : "") + "layer " + curLayerNum + "/" + curLayerType +
        	" (" + layerNodeProto + ") has " + numVertices + " points");
	}

	private static class UnknownLayerMessage
	{
		String message;

		UnknownLayerMessage(String message)
		{
			this.message = message;
		}
	}

	private void setLayer(int layerNum, int layerType)
	{
		curLayerNum = layerNum;
		curLayerType = layerType;
		layerIsPin = false;
		currentUnknownLayerMessage = null;
		Integer layerInt = new Integer(layerNum + (layerType<<16));
		Layer layer = layerNames.get(layerInt);
		if (layer == null)
		{
			layer = Generic.tech().drcLay;
			if (localPrefs.unknownLayerHandling == IOTool.GDSUNKNOWNLAYERUSERANDOM)
			{
				// assign an unused layer here
				for(Iterator<Layer> it = curTech.getLayers(); it.hasNext(); )
				{
					Layer l = it.next();
					if (layerNames.values().contains(l)) continue;
					layer = l;
					break;
				}
				if (layer == null)
				{
					// no unused layers: start picking at random
					if (randomLayerSelection >= curTech.getNumLayers()) randomLayerSelection = 0;
					layer = curTech.getLayer(randomLayerSelection);
					randomLayerSelection++;
				}
			}
			layerNames.put(layerInt, layer);
			String message = "GDS layer " + layerNum + ", type " + layerType + " unknown, ";
			switch (localPrefs.unknownLayerHandling)
			{
				case IOTool.GDSUNKNOWNLAYERIGNORE:    message += "ignoring it";                    break;
				case IOTool.GDSUNKNOWNLAYERUSEDRC:    message += "using Generic:DRC layer";        break;
				case IOTool.GDSUNKNOWNLAYERUSERANDOM: message += "using layer " + layer.getName(); break;
			}
//			if (layer != null)
//			{
				currentUnknownLayerMessage = layerErrorMessages.get(layerInt);
				if (currentUnknownLayerMessage == null)
				{
					currentUnknownLayerMessage = new UnknownLayerMessage(message);
					layerErrorMessages.put(layerInt, currentUnknownLayerMessage);
				}
//			} else
//			{
//				errorLogger.logWarning(message, theCell.cell, 0);
//				List<Cell> cellsWithError = cellLayerErrors.get(message);
//				if (cellsWithError == null) cellLayerErrors.put(message, cellsWithError = new ArrayList<Cell>());
//				cellsWithError.add(theCell.cell);
//				System.out.println(message);
//			}
		}
		if (layer != null)
		{
			currentUnknownLayerMessage = layerErrorMessages.get(layerInt);
			if (layer == Generic.tech().drcLay && localPrefs.unknownLayerHandling == IOTool.GDSUNKNOWNLAYERIGNORE)
			{
				layerNodeProto = null;
				pinNodeProto = null;
				return;
			}
			layerNodeProto = layer.getNonPseudoLayer().getPureLayerNode();
			pinNodeProto = Generic.tech().universalPinNode;
			if (pinLayers.contains(layerInt))
			{
				layerIsPin = true;
				if (layerNodeProto != null && layerNodeProto.getNumPorts() > 0)
				{
					PortProto pp = layerNodeProto.getPort(0);
					for (Iterator<ArcProto> it = layer.getTechnology().getArcs(); it.hasNext(); )
					{
						ArcProto arc = it.next();
						if (pp.connectsTo(arc))
						{
							pinNodeProto = arc.findOverridablePinProto(ep);
							break;
						}
					}
				}
			}
			if (layerNodeProto == null)
			{
				String message = "Error: no pure layer node for layer '" + layer.getName() + "', ignoring it";
				layerNames.put(layerInt, Generic.tech().drcLay);
				currentUnknownLayerMessage = new UnknownLayerMessage(message);
				layerErrorMessages.put(layerInt, currentUnknownLayerMessage);
			}
		}
	}

	private void determineLayer()
		throws IOException
	{
		if (theToken != GDS_LAYER) handleError("Layer statement is missing");

		getToken();
		if (theToken != GDS_SHORT_NUMBER) handleError("Invalid layer number");

		int layerNum = tokenValue16;
		getToken();
		if (!isMember(theToken, maskSet)) handleError("No datatype field");

		getToken();
		setLayer(layerNum, tokenValue16);
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

		// add to the current structure as a variable?
		getToken();
	}

	private void getPrototype(String name)
		throws IOException
	{
		// scan for this proto
		name = name + "{lay}";
		Cell np = findCell(name);
		if (np == null)
		{
			// FILO order, create this nodeproto
			np = Cell.newInstance(theLibrary, name);
			if (np == null) handleError("Failed to create SREF proto");
			setProgressValue(0);
			setProgressNote("Reading " + name);
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
        String message = "Error: " + msg + " at byte " + byteCount +
            " in '" + filePath + "'";
        Cell cell = theCell != null ? theCell.cell : null;
        System.out.println(message);
        errorLogger.logError(message, cell, 0);
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

	private void determinePoints(int min_points, int max_points)
		throws IOException
	{
		numVertices = 0;
		while (theToken == GDS_NUMBER)
		{
			double x = scaleValue(tokenValue32);
			getToken();
			double y = scaleValue(tokenValue32);
			theVertices[numVertices].setLocation(x, y);
			numVertices++;
			if (numVertices > max_points)
			{
				System.out.println("Found " + numVertices + " points (too many)");
				handleError("Too many points in the shape");
			}
			getToken();
		}
		if (numVertices < min_points)
		{
			System.out.println("Found " + numVertices + " points (too few)");
			handleError("Not enough points in the shape");
		}
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
		return time_array[1] + "-" + time_array[2] + "-" + time_array[0] + " at " +
			time_array[3] + ":" + time_array[4] + ":" + time_array[5];
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
		return sign * reg;
	}

	private static double makePower(int val, int power)
	{
		return Math.pow(val, power);
//		double result = 1.0;
//		for(int count=0; count<power; count++)
//			result *= val;
//		return result;
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
			double pow =  makePower(16, exponent);
			realValue *= pow;
		} else
		{
			if (exponent < 0)
			{
				double pow =  makePower(16, -exponent);
				realValue /= pow;
			}
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
