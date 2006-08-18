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

import com.sun.electric.database.geometry.*;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.Name;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.MutableTextDescriptor;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.ImmutableNodeInst;
import com.sun.electric.technology.*;
import com.sun.electric.technology.Technology.NodeLayer;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.GDSLayers;
import com.sun.electric.tool.io.IOTool;

import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

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

	private Library          theLibrary;
	private CellBuilder      theCell;
	private NodeProto        theNodeProto;
	private PrimitiveNode    layerNodeProto;
    private PrimitiveNode    pinNodeProto;
	private boolean          layerUsed;
	private boolean          layerIsPin;
	private Technology       curTech;
	private int              recordCount;
	private GSymbol          theToken;
	private DatatypeSymbol   valuetype;
	private int              tokenFlags;
	private int              tokenValue16;
	private int              tokenValue32;
	private double           tokenValueDouble;
	private String           tokenString;
	private Point2D []       theVertices;
	private double           theScale;
	private HashMap<Integer,Layer> layerNames;
	private HashSet<Integer> pinLayers;
	private PolyMerge        merge;
	private boolean          mergeThisCell;

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

    private static class CellBuilder {
		private static HashMap<Cell,CellBuilder> allBuilders;


        Cell cell;
        List<MakeInstance> insts = new ArrayList<MakeInstance>();
        
        private CellBuilder(Cell cell) {
            this.cell = cell;
            allBuilders.put(cell, this);
        }
         
		private void makeInstance(NodeProto proto, Point2D loc, Orientation orient, double wid, double hei, EPoint[] points) {
            MakeInstance mi = new MakeInstance(proto, loc, orient, wid, hei, points, null, null, null);
            insts.add(mi);
        }
        
		private void makeExport(NodeProto proto, Point2D loc, Orientation orient, double wid, double hei, String exportName) {
            MakeInstance mi = new MakeInstance(proto, loc, orient, wid, hei, null, exportName, null, null);
            insts.add(mi);
        }
        
		private void makeText(NodeProto proto, Point2D loc, String text, TextDescriptor textDescriptor) {
            MakeInstance mi = new MakeInstance(proto, loc, Orientation.IDENT, 0, 0, null, null, Name.findName(text), textDescriptor);
            insts.add(mi);
        }
        
		private static void init() { allBuilders = new HashMap<Cell,CellBuilder>(); }

		private static void term() { allBuilders = null; }

		private void makeInstances(Set<Cell> builtCells)
		{
            if (builtCells.contains(cell)) return;
            builtCells.add(cell);
			boolean countOff = false;
			if (Job.getDebug())
			{
				int size = insts.size();
				System.out.println("Building cell " + this.cell.describe(false) +
					" with " + size + " instances");
				if (size >= 100000) countOff = true;
			}

// ignore internal contents when cell is very large (for testing only)
//if (countOff)
//{
//	MakeInstance ll = null, ul = null, lr = null, ur = null;
//	for(MakeInstance mi : insts)
//	{
//		if (ll == null) ll = ul = lr = ur = mi;
//		if (mi.loc.getX() <= ll.loc.getX() && mi.loc.getY() <= ll.loc.getY()) ll = mi;
//		if (mi.loc.getX() <= ul.loc.getX() && mi.loc.getY() >= ul.loc.getY()) ul = mi;
//		if (mi.loc.getX() >= lr.loc.getX() && mi.loc.getY() <= lr.loc.getY()) lr = mi;
//		if (mi.loc.getX() >= ur.loc.getX() && mi.loc.getY() >= ur.loc.getY()) ur = mi;
//	}
//	insts.clear();
//	insts.add(ll);
//	if (!insts.contains(ul)) insts.add(ul);
//	if (!insts.contains(lr)) insts.add(lr);
//	if (!insts.contains(ur)) insts.add(ur);
//}
            nameInstances(countOff);
           	Collections.sort(insts);
			int count = 0;
			for(MakeInstance mi : insts)
			{
				if (countOff && ((++count % 1000) == 0))
					System.out.println("        Made " + count + " instances");
                if (mi.proto instanceof Cell) {
                    Cell subCell = (Cell)mi.proto;
                    CellBuilder cellBuilder = allBuilders.get(subCell);
                    if (cellBuilder != null)
                        cellBuilder.makeInstances(builtCells);
                }

				// make the instance
                mi.instantiate(this.cell);
			}
            if (IOTool.isGDSInSimplifyCells())
                simplifyNodes(this.cell);
			builtCells.add(this.cell);
		}

        /** Method to see if existing primitive nodes could be merged and define more complex nodes
         * such as contacts
         */
        private void simplifyNodes(Cell cell)
        {
            HashMap<Layer, List<NodeInst>> map = new HashMap<Layer, List<NodeInst>>();

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

            Technology tech = cell.getTechnology();
            List<Layer.Function> tmpList = new ArrayList<Layer.Function>();
            Set<NodeInst> toDelete = new HashSet<NodeInst>();
            Set<NodeInst> viaToDelete = new HashSet<NodeInst>();
            List<Geometric> geomList = new ArrayList<Geometric>();

            for (Iterator<PrimitiveNode> itPn = tech.getNodes(); itPn.hasNext();)
            {
                PrimitiveNode pn = itPn.next();
                boolean allFound = true;
                if (pn.getFunction() != PrimitiveNode.Function.CONTACT) continue; // only dealing with metal contacts for now.

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
                tmpList.clear();
                tmpList.add(viaLayer.getFunction());
                List<NodeInst> viasList = map.get(viaLayer);

                for (NodeInst ni : list)
                {
                    Poly[] polys = tech.getShapeOfNode(ni, null, null, true, false, null);
                    assert(polys.length == 1); // it must be only 1
                    Poly m1P = polys[0];
                    List<NodeInst> nList = map.get(m2Layer);
                    if (nList == null) continue; // nothing found in m2Layer
                    for (NodeInst n : nList)
                    {
                        Poly[] otherPolys = tech.getShapeOfNode(n, null, null, true, false, null);
                        assert(otherPolys.length == 1); // it must be only 1
                        Poly m2P = otherPolys[0];
                        if (!m2P.getBounds2D().equals(m1P.getBounds2D())) continue; // no match

                        ImmutableNodeInst d = (ImmutableNodeInst)ni.getImmutable();
                        String name = ni.getName();
                        int atIndex = name.indexOf('@');
                        if (atIndex < 0) name += "tmp"; else
                        	name = name.substring(0, atIndex) + "tmp" + name.substring(atIndex);                        	
                        NodeInst newNi = NodeInst.makeInstance(pn, d.anchor,
                                m2P.getBounds2D().getWidth() + so.getLowXOffset() + so.getHighXOffset(),
                                m2P.getBounds2D().getHeight() + so.getLowYOffset() + so.getHighYOffset(),
                                ni.getParent(), ni.getOrient(), name, 0);

                        // Searching for vias to delete
                        assert(viasList != null);
                        Poly[] viaPolys = tech.getShapeOfNode(newNi, null, null, true, false, tmpList);
                        boolean found = false;

                        // Can be more than 1 due to MxN cuts
                        viaToDelete.clear();
                        for (int i = 0; i < viaPolys.length; i++)
                        {
                            Poly poly = viaPolys[i];
                            Rectangle2D bb = poly.getBounds2D();
                            bb.setRect(ERectangle.snap(bb));
                            found = false;

                            for (NodeInst viaNi : viasList)
                            {
                                Poly[] thisViaList = tech.getShapeOfNode(viaNi, null, null, true, false, tmpList);
                                assert(thisViaList.length == 1);
                                // hack to get rid of the resolution issue
                                Poly p = thisViaList[0];
                                Rectangle2D b = p.getBounds2D();
                                b.setRect(ERectangle.snap(b));
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
                            if (Job.getDebug())
                                System.out.println("Adding " + newNi.getName());
                            toDelete.clear();
                            geomList.clear();
                            toDelete.add(ni);
                            toDelete.add(n);
                            toDelete.addAll(viaToDelete);
                            String message = toDelete.size() + " nodes were replaced for more complex primitives in cell '" + cell.getName() + "'";
                            geomList.add(newNi);
                            errorLogger.logWarning(message, geomList, null, null, null, null, cell, -1);
                            // Deleting now replaced pure primitives
                            for (NodeInst toDel : toDelete)
                            {
                                if (Job.getDebug())
                                    System.out.println("Deleting " + ni.getName());
                                toDel.kill();
                            }
                        }
                    }
                }
            }
        }

        private void nameInstances(boolean countOff) {
            HashMap<String,GenMath.MutableInteger> maxSuffixes = new HashMap<String,GenMath.MutableInteger>();
            HashSet<String> userNames = new HashSet<String>();
            int count = 0;
            for (MakeInstance mi: insts)
            {
				if (countOff && ((++count % 2000) == 0))
					System.out.println("        Named " + count + " instances");
                if (mi.nodeName != null) {
                    if (!validGdsNodeName(mi.nodeName)) {
                        System.out.println("  Warning: Node name '" + mi.nodeName + "' in cell " + cell.describe(false) +
                                " is bad (" + Name.checkName(mi.nodeName.toString()) + ")...ignoring the name");
                    } else if (!userNames.contains(mi.nodeName.toString())) {
                        userNames.add(mi.nodeName.toString());
                        continue;
                    }
                }
                Name baseName;
                if (mi.proto instanceof Cell) {
                    baseName = ((Cell)mi.proto).getBasename();
                } else {
                    PrimitiveNode np = (PrimitiveNode)mi.proto;
                    baseName = np.getTechnology().getPrimitiveFunction(np, 0).getBasename();
                }
                String basenameString = baseName.canonicString();
                GenMath.MutableInteger maxSuffix = maxSuffixes.get(basenameString);
                if (maxSuffix == null) {
                    maxSuffix = new GenMath.MutableInteger(-1);
                    maxSuffixes.put(basenameString, maxSuffix);
                }
                maxSuffix.increment();
                mi.nodeName = baseName.findSuffixed(maxSuffix.intValue());
            }
        }
        
        private boolean validGdsNodeName(Name name) {
            return name.isValid() && !name.hasEmptySubnames() && !name.isBus() || !name.isTempname();
        }
         
		private static void buildInstances()
		{
			Set<Cell> builtCells = new HashSet<Cell>();
			for(CellBuilder cellBuilder : allBuilders.values())
				cellBuilder.makeInstances(builtCells);
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
        private TextDescriptor textDescriptor; // text

		private MakeInstance(NodeProto proto, Point2D loc, Orientation orient, double wid, double hei, EPoint[] points, String exportName, Name nodeName, TextDescriptor textDescriptor)
		{
			this.proto = proto;
			this.loc = loc;
            this.orient = orient;
            this.wid = DBMath.round(wid);
            this.hei = DBMath.round(hei);
            this.points = points;
            this.exportName = exportName;
            this.nodeName = nodeName;
            this.textDescriptor = textDescriptor;
		}
        
        public int compareTo(MakeInstance that) {
            return TextUtils.STRING_NUMBER_ORDER.compare(this.nodeName.toString(), that.nodeName.toString());
        }
        
        private void instantiate(Cell parent) {
        	String name = nodeName.toString();
            NodeInst ni = NodeInst.makeInstance(proto, loc, wid, hei, parent, orient, nodeName.toString(), 0);

            if (ni == null) return;
            if (ni.getNameKey() != nodeName) {
                System.out.println("GDS name " + name + " renamed to " + ni.getName());
            }
            if (IOTool.isGDSInExpandsCells() && ni.isCellInstance())
                ni.setExpanded();
            if (points != null)
                ni.newVar(NodeInst.TRACE, points);
            if (exportName != null)
            {
        		if (parent.findExport(exportName) != null)
        		{
                    String newName = ElectricObject.uniqueObjectName(exportName, parent, PortProto.class, true);
                    System.out.println("  Warning: Multiple exports called '" + exportName + "' in cell " +
                    	parent.describe(false) + " (renamed to " + newName + ")");
                    exportName = newName;
        		}
                Export.newInstance(parent, ni.getPortInst(0), exportName);
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
		CellBuilder.init();
		theLibrary = lib;

		try
		{
			loadFile();
		} catch (IOException e)
		{
			System.out.println("ERROR reading GDS file");
		}

		// now build all instances recursively
		CellBuilder.buildInstances();
		CellBuilder.term();
		return false;
	}

	private void initialize()
	{
		layerNodeProto = Generic.tech.drcNode;

		theVertices = new Point2D[MAXPOINTS];
		for(int i=0; i<MAXPOINTS; i++) theVertices[i] = new Point2D.Double();
		recordCount = 0;

		// get the array of GDS names
		layerNames = new HashMap<Integer,Layer>();
		pinLayers = new HashSet<Integer>();
		boolean valid = false;
		curTech = Technology.getCurrent();
        Foundry foundry = curTech.getSelectedFoundry();
		for(Iterator<Layer> it = curTech.getLayers(); it.hasNext(); )
		{
			Layer layer = it.next();
            String gdsName = foundry.getGDSLayer(layer);
			if (gdsName != null && gdsName.length() > 0)
			{
				GDSLayers gdsl = GDSLayers.parseLayerString(gdsName);
				for(Iterator<Integer> lIt = gdsl.getLayers(); lIt.hasNext(); )
				{
					Integer lVal = lIt.next();
					Integer lay = new Integer(lVal.intValue());
					if (layerNames.get(lay) == null) layerNames.put(lay, layer);
				}
				if (gdsl.getPinLayer() != -1)
				{
					pinLayers.add(new Integer(gdsl.getPinLayer()));
					layerNames.put(new Integer(gdsl.getPinLayer()), layer);
				}
				if (gdsl.getTextLayer() != -1)
					layerNames.put(new Integer(gdsl.getTextLayer()), layer);
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
	private List<Integer> parseLayerNumbers(String layerNumbers)
	{
		String [] numberStrings = layerNumbers.split(",");
		List<Integer> numbers = new ArrayList<Integer>();
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
		double microScale = TextUtils.convertFromDistance(1, curTech, TextUtils.UnitScale.MICRO);
		theScale = meterUnit * 1000000.0 * microScale;

		// round the scale
		double shift = 1;
		double roundedScale = theScale;
		while (roundedScale < 1)
		{
			roundedScale *= 10;
			shift *= 10;
		}
		roundedScale = DBMath.round(roundedScale) / shift;
		theScale = roundedScale;
	}

	private void readStructure()
		throws IOException
	{
		beginStructure();
		getToken();
		mergeThisCell = IOTool.isGDSInMergesBoxes();
		if (mergeThisCell)
		{
			// initialize merge if merging this cell
    		merge = new PolyMerge();
		}

		// read the cell
		while (theToken != GDS_ENDSTR)
		{
			getElement();
			getToken();
		}
		if (mergeThisCell)
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
    						points[i] = new EPoint(pPoints[i].getX() - ctr.getX(), pPoints[i].getY() - ctr.getY());
    					}

    					// store the trace information
                        theCell.makeInstance(pnp, ctr, Orientation.IDENT, box.getWidth(), box.getHeight(), points);
    				} else
    				{
    					Point2D ctr = new EPoint(box.getCenterX(), box.getCenterY());
                        theCell.makeInstance(pnp, ctr, Orientation.IDENT, box.getWidth(), box.getHeight(), null);
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
		String createTime = determineTime();
		String modTime = determineTime();
		if (theToken != GDS_STRNAME) handleError("Strname statement is missing");

		getToken();
		if (theToken != GDS_IDENT) handleError("Structure name is missing");

		// look for this nodeproto
		Cell cell = findCell(tokenString);
		if (cell == null)
		{
			// create the proto
			cell = Cell.newInstance(theLibrary, tokenString+"{"+View.LAYOUT.getAbbreviation()+"}");
			if (cell == null) handleError("Failed to create structure");
			System.out.println("Reading " + tokenString);
			if (Job.getUserInterface().getCurrentCell(theLibrary) == null)
				Job.getUserInterface().setCurrentCell(theLibrary, cell);
		}
        theCell = new CellBuilder(cell);
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
			colInterval.setLocation((theVertices[1].getX() - theVertices[0].getX()) / nCols, (theVertices[1].getY() - theVertices[0].getY()) / nCols);
//			makeTransform(colInterval, angle, trans);
		}
		Point2D rowInterval = new Point2D.Double(0, 0);
		if (nRows != 1)
		{
			rowInterval.setLocation((theVertices[2].getX() - theVertices[0].getX()) / nRows, (theVertices[2].getY() - theVertices[0].getY()) / nRows);
//			makeTransform(rowInterval, angle, trans);
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
					theCell.makeInstance((Cell)theNodeProto, loc, Orientation.fromJava(angle, mX, mY), 0, 0, null);
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
        Orientation orient = Orientation.fromC(angle, trans);
        AffineTransform xform = orient.pureRotate();
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
		theCell.makeInstance((Cell)theNodeProto, loc, Orientation.fromJava(angle, false, mY), 0, 0, null);
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
				if (mergeThisCell)
				{
					PrimitiveNode plnp = (PrimitiveNode)layerNodeProto;
					NodeLayer [] layers = plnp.getLayers();
					merge.addPolygon(layers[0].getLayer(), new Poly(ctr.getX(), ctr.getY(), sX, sY));
				} else
				{
                    theCell.makeInstance(layerNodeProto, ctr, Orientation.IDENT, sX, sY, null);
				}
			}
			return;
		}

		if (oclass == SHAPEOBLIQUE || oclass == SHAPEPOLY)
		{
			if (!layerUsed) return;
			if (mergeThisCell)
			{
				PrimitiveNode plnp = (PrimitiveNode)layerNodeProto;
				NodeLayer [] layers = plnp.getLayers();
				merge.addPolygon(layers[0].getLayer(), new Poly(theVertices)); // ??? npts
			} else
			{
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

				// store the trace information
				double cx = (hx + lx) / 2;
				double cy = (hy + ly) / 2;
				EPoint [] points = new EPoint[npts];
				for(int i=0; i<npts; i++)
				{
					points[i] = new EPoint(theVertices[i].getX() - cx, theVertices[i].getY() - cy);
				}

				// now create the node
                theCell.makeInstance(layerNodeProto, new EPoint((lx+hx)/2, (ly+hy)/2), Orientation.IDENT, hx-lx, hy-ly, points);
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
					Poly poly = Poly.makeEndPointPoly(length, width, GenMath.figureAngle(fromPt, toPt), fromPt, fextend, toPt, textend, Poly.Type.FILLED);

					if (mergeThisCell)
					{
						PrimitiveNode plnp = (PrimitiveNode)layerNodeProto;
						NodeLayer [] layers = plnp.getLayers();
						merge.addPolygon(layers[0].getLayer(), poly);
					} else
					{
						// make the node for this segment
						Rectangle2D polyBox = poly.getBox();
						if (polyBox != null)
						{
                            theCell.makeInstance(layerNodeProto, new EPoint(polyBox.getCenterX(), polyBox.getCenterY()), Orientation.IDENT,
                                    polyBox.getWidth(), polyBox.getHeight(), null);
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
								points[j] = new EPoint(polyPoints[j].getX() - cx, polyPoints[j].getY() - cy);
							}

							// store the trace information
                            theCell.makeInstance(layerNodeProto, new EPoint(cx, cy), Orientation.IDENT, polyBox.getWidth(), polyBox.getHeight(), points);
						}
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

		// create the node

		if (mergeThisCell)
		{
		} else
		{
            theCell.makeInstance(layerNodeProto, new Point2D.Double(theVertices[0].getX(), theVertices[0].getY()),
            	Orientation.IDENT, 0, 0, null);
		}
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
			NodeProto np = pinNodeProto;
            if (np.getNumPorts() > 0)
            {
                theCell.makeExport(np, new Point2D.Double(theVertices[0].getX(), theVertices[0].getY()),
                	Orientation.IDENT, np.getDefWidth(), np.getDefHeight(), charstring);
            }
			return;
		}

		// stop of not handling text in GDS
		if (!IOTool.isGDSInIncludesText()) return;
		double x = theVertices[0].getX() + MINFONTWIDTH * charstring.length();
		double y = theVertices[0].getY() + MINFONTHEIGHT;
		theVertices[1].setLocation(x, y);

		// create a holding node
        Orientation orient = Orientation.fromAngle(angle);

		// set the text size and orientation
		MutableTextDescriptor td = MutableTextDescriptor.getNodeTextDescriptor();
		double size = scale;
		if (size <= 0) size = 2;
		if (size > TextDescriptor.Size.TXTMAXQGRID) size = TextDescriptor.Size.TXTMAXQGRID;
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
        	charstring, TextDescriptor.newTextDescriptor(td));
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
			// create the box
			if (mergeThisCell)
			{
			} else
			{
                theCell.makeInstance(layerNodeProto, new Point2D.Double(theVertices[0].getX(), theVertices[0].getY()),
                	Orientation.IDENT, 0, 0, null);
			}
		}
	}

	private void setLayer(int layerNum, int layerType)
	{
		layerUsed = true;
		layerIsPin = false;
		Integer layerInt = new Integer(layerNum + (layerType<<16));
		Layer layer = layerNames.get(layerInt);
		if (layer == null)
		{
            String message = (IOTool.isGDSInIgnoresUnknownLayers()) ?
                "GDS layer " + layerNum + ", type " + layerType + " unknown in cell '" + theCell.cell.getName() + "', ignoring it" :
                "GDS layer " + layerNum + ", type " + layerType + " unknown '" + theCell.cell.getName() + "', using Generic:DRC";
            errorLogger.logWarning(message, theCell.cell, 0);
            System.out.println(message);
			layerNames.put(layerInt, Generic.tech.drcLay);
			layerUsed = false;
			layerNodeProto = null;
		} else
		{
			layerNodeProto = layer.getNonPseudoLayer().getPureLayerNode();
			if (layer == Generic.tech.drcLay && IOTool.isGDSInIgnoresUnknownLayers())
				layerUsed = false;
            pinNodeProto = Generic.tech.universalPinNode;
			if (pinLayers.contains(layerInt)) {
                layerIsPin = true;
                for (Iterator<ArcProto> it = layer.getTechnology().getArcs(); it.hasNext(); ) {
                    ArcProto arc = it.next();
                    PortProto pp = layerNodeProto.getPort(0);
                    if (pp != null && pp.connectsTo(arc)) {
                        pinNodeProto = arc.findOverridablePinProto();
                        break;
                    }
                }
            }
			if (layerNodeProto == null)
			{
                String message = "Error: no pure layer node for layer "+layer.getName() + " in cell '" + theCell.cell.getName() + "', ignoring it";
				System.out.println(message);
                errorLogger.logError(message, theCell.cell, 0);
				layerNames.put(layerInt, Generic.tech.drcLay);
				layerUsed = false;
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
			np = Cell.newInstance(theLibrary, tokenString+"{"+View.LAYOUT.getAbbreviation()+"}");
			if (np == null) handleError("Failed to create SREF proto");
            setProgressValue(0);
            setProgressNote("Reading " + tokenString);
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
        String message = "Error: " + msg + " at byte " + byteCount;
		System.out.println(message);
        errorLogger.logError(message, theCell.cell, 0);
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
