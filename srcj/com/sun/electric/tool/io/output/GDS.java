/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: GDS.java
 * Input/output tool: GDS output
 * Original C Code written by Sid Penstone, Queens University
 * Translated to Java by Steven M. Rubin, Sun Microsystems.
 *
 * Copyright (c) 2003 Sun Microsystems and Static Free Software
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
package com.sun.electric.tool.io.output;

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.geometry.PolyBase;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Export;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.PortOriginal;
import com.sun.electric.database.prototype.PortProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.PortInst;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.technology.*;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.io.IOTool;
import com.sun.electric.tool.io.GDSLayers;
import com.sun.electric.tool.ncc.basic.NccCellAnnotations;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * This class writes files in GDS format.
 */
public class GDS extends Geometry
{
	private static final int GDSVERSION        =      3;
	private static final int BYTEMASK          =   0xFF;
	private static final int DSIZE             =    512;		/* data block */
	private static final int MAXPOINTS         =    510;		/* maximum points in a polygon */
	private static final int EXPORTPRESENTATION=      0;		/* centered (was 8 for bottomleft) */

	// GDSII bit assignments in STRANS record
	private static final int STRANS_REFLX      = 0x8000;
//	private static final int STRANS_ABSA       =    0x2;

	// data type codes
	private static final int DTYP_NONE         =      0;

	// header codes
	private static final short HDR_HEADER      = 0x0002;
	private static final short HDR_BGNLIB      = 0x0102;
	private static final short HDR_LIBNAME     = 0x0206;
	private static final short HDR_UNITS       = 0x0305;
	private static final short HDR_ENDLIB      = 0x0400;
	private static final short HDR_BGNSTR      = 0x0502;
	private static final short HDR_STRNAME     = 0x0606;
	private static final short HDR_ENDSTR      = 0x0700;
	private static final short HDR_BOUNDARY    = 0x0800;
	private static final short HDR_PATH        = 0x0900;
	private static final short HDR_SREF        = 0x0A00;
//	private static final short HDR_AREF        = 0x0B00;
	private static final short HDR_TEXT        = 0x0C00;
	private static final short HDR_LAYER       = 0x0D02;
	private static final short HDR_DATATYPE    = 0x0E02;
	private static final short HDR_XY          = 0x1003;
	private static final short HDR_ENDEL       = 0x1100;
	private static final short HDR_SNAME       = 0x1206;
	private static final short HDR_TEXTTYPE    = 0x1602;
	private static final short HDR_PRESENTATION= 0x1701;
	private static final short HDR_STRING	   = 0x1906;
	private static final short HDR_STRANS      = 0x1A01;
	private static final short HDR_MAG         = 0x1B05;
	private static final short HDR_ANGLE       = 0x1C05;
//    private static final short HDR_PROPATTR    = 0x2B02;
//    private static final short HDR_PROPVALUE   = 0x2C06;

	// Header byte counts
	private static final short HDR_N_BGNLIB    =     28;
	private static final short HDR_N_UNITS     =     20;
	private static final short HDR_N_ANGLE     =     12;
	private static final short HDR_N_MAG       =     12;

	// Maximum string sizes
	private static final int HDR_M_SNAME       =     32;
	//private static final int HDR_M_STRNAME     =     32; // replace by preference IOTool.getGDSCellNameMaxLen
	private static final int HDR_M_ASCII       =    256;

	// contour gathering thresholds for polygon accumulation
//	private static final double BESTTHRESH     =   0.001;		/* 1/1000 of a millimeter */
//	private static final double WORSTTHRESH    =   0.1;			/* 1/10 of a millimeter */

	/** for buffering output data */			private static byte [] dataBufferGDS = new byte[DSIZE];
	/** for buffering output data */			private static byte [] emptyBuffer = new byte[DSIZE];
	/** Current layer for gds output */			private static GDSLayers currentLayerNumbers;
	/** Position of next byte in the buffer */	private static int bufferPosition;					
	/** Number data buffers output so far */	private static int blockCount;				
	/** constant for GDS units */				private static double scaleFactor;				
	/** cell naming map */						private HashMap<Cell,String> cellNames;
	/** layer number map */						private HashMap<Layer,GDSLayers> layerNumbers;
    /** separator string for lib + cell concatanated cell names */  public static final String concatStr = ".";
    /** Name remapping if NCC annotation */     private HashMap<String,Set<String>> nameRemapping;

	/**
	 * Main entry point for GDS output.
     * @param cell the top-level cell to write.
     * @param context the hierarchical context to the cell.
	 * @param filePath the disk file to create.
	 */
	public static GDS writeGDSFile(Cell cell, VarContext context, String filePath)
	{
		if (cell.getView() != View.LAYOUT)
		{
			System.out.println("Can only write GDS for layout cells");
			return null;
		}
		GDS out = new GDS();
		if (out.openBinaryOutputStream(filePath)) return null;
		BloatVisitor visitor = out.makeBloatVisitor(getMaxHierDepth(cell));
		if (out.writeCell(cell, context, visitor)) return null;
		if (out.closeBinaryOutputStream()) return null;
		System.out.println(filePath + " written");

		// warn if library name was changed
		String topCellName = cell.getName();
		String mangledTopCellName = makeGDSName(topCellName, HDR_M_ASCII);
		if (!topCellName.equals(mangledTopCellName))
			System.out.println("Warning: library name in this file is " + mangledTopCellName +
				" (special characters were changed)");
        return out;
	}

	/** Creates a new instance of GDS */
	GDS()
	{
	}

	protected void start()
	{
		initOutput();
		outputBeginLibrary(topCell);
	}

	protected void done()
	{
		outputHeader(HDR_ENDLIB, 0);
		doneWritingOutput();
	}

	/** Method to write cellGeom */
	protected void writeCellGeom(CellGeom cellGeom)
	{
		// write this cell
		Cell cell = cellGeom.cell;
        Foundry foundry = cell.getTechnology().getSelectedFoundry();
		outputBeginStruct(cell);
        boolean renamePins = (cell == topCell && IOTool.getGDSConvertNCCExportsConnectedByParentPins());
        boolean colapseGndVddNames = (cell == topCell && IOTool.isGDSColapseVddGndPinNames());

        if (renamePins) {
            // rename pins to allow external LVS programs to virtually connect nets as specified
            // by the NCC annotation exportsConnectedByParent
            NccCellAnnotations annotations = NccCellAnnotations.getAnnotations(cell);
            if (annotations == null)
                renamePins = false;
            else
                nameRemapping = createExportNameMap(annotations, cell);
        }

		// write all polys by Layer
		Set<Layer> layers = cellGeom.polyMap.keySet();
		for (Layer layer : layers)
		{
            // No technology associated, case when art elements are added in layout
            // r.getTechnology() == Generic.tech for layer Glyph
            if (layer == null || layer.getTechnology() == null || layer.getTechnology() == Generic.tech) continue;
			if (!selectLayer(foundry, layer))
            {
                System.out.println("Skipping " + layer + " in GDS:writeCellGeom");
                continue;
            }
			List<Object> polyList = cellGeom.polyMap.get(layer);
			for (Object obj : polyList)
			{
				PolyBase poly = (PolyBase)obj;
				Integer firstLayer = (Integer)currentLayerNumbers.getFirstLayer();
				int layerNum = firstLayer.intValue() & 0xFFFF;
				int layerType = (firstLayer.intValue() >> 16) & 0xFFFF;
				writePoly(poly, layerNum, layerType);
			}
		}

		// write all instances
		for (Nodable no : cellGeom.nodables)
		{
			writeNodable(no);
		}

		// now write exports
		if (IOTool.getGDSOutDefaultTextLayer() >= 0)
		{
			for(Iterator<PortProto> it = cell.getPorts(); it.hasNext(); )
			{
				Export pp = (Export)it.next();

				// find the node at the bottom of this export
				PortOriginal fp = new PortOriginal(pp.getOriginalPort());
				PortInst bottomPort = fp.getBottomPort();
				NodeInst bottomNi = bottomPort.getNodeInst();
//				AffineTransform trans = fp.getTransformToTop();

				// find the layer associated with this node
				PrimitiveNode pNp = (PrimitiveNode)bottomNi.getProto();
				Technology.NodeLayer [] nLay = pNp.getLayers();
				Layer layer = nLay[0].getLayer().getNonPseudoLayer();
				selectLayer(foundry, layer);

				int textLayer = -1, pinLayer = -1, textType = 0, pinType = 0;
				textLayer = pinLayer = IOTool.getGDSOutDefaultTextLayer();
				if (currentLayerNumbers.getTextLayer() != -1)
				{
					textLayer = currentLayerNumbers.getTextLayer() & 0xFFFF;
					textType = (currentLayerNumbers.getTextLayer() >> 16) & 0xFFFF;
				}
				if (currentLayerNumbers.getPinLayer() != -1)
				{
					pinLayer = currentLayerNumbers.getPinLayer() & 0xFFFF;
					pinType = (currentLayerNumbers.getPinLayer() >> 16) & 0xFFFF;
				}

				// put out a pin if requested
				if (IOTool.isGDSOutWritesExportPins())
                {
					writeExportOnLayer(pp, pinLayer, pinType, renamePins, colapseGndVddNames);

                    // write the text
                    //writeExportOnLayer(pp, textLayer, textType);
                }
			}
		}
		outputHeader(HDR_ENDSTR, 0);
	}

    private HashMap<String,Set<String>> createExportNameMap(NccCellAnnotations ann, Cell cell) {
        HashMap<String,Set<String>> nameMap = new HashMap<String,Set<String>>();
        for (Iterator<List<NccCellAnnotations.NamePattern>> it2 = ann.getExportsConnected(); it2.hasNext(); )
		{
            List<NccCellAnnotations.NamePattern> list = it2.next();
            // list of all patterns that should be connected
            Set<String> connectedExports = new TreeSet<String>(new StringComparator());
            for (NccCellAnnotations.NamePattern pat : list)
			{
                for (Iterator<PortProto> it = cell.getPorts(); it.hasNext(); )
				{
                    Export e = (Export)it.next();
                    String name = e.getName();
                    if (pat.matches(name))
					{
                        connectedExports.add(name);
                        nameMap.put(name, connectedExports);
                    }
                }
            }
        }
        return nameMap;
    }
    private static class StringComparator implements Comparator<String>
    {
		/**
		 * Method to sort Objects by their string name.
		 */
        public int compare(String s1, String s2)
		{
            return s1.compareTo(s2);
        }
        public boolean equals(Object obj)
		{
            return (this == obj);
        }
    }
	private void writeExportOnLayer(Export pp, int layer, int type, boolean remapNames, boolean colapseGndVddNames)
	{
		outputHeader(HDR_TEXT, 0);
		outputHeader(HDR_LAYER, layer);
		outputHeader(HDR_TEXTTYPE, type);
		outputHeader(HDR_PRESENTATION, EXPORTPRESENTATION);

		// now the orientation
		NodeInst ni = pp.getOriginalPort().getNodeInst();
		int transValue = 0;
		int angle = ni.getAngle();
		if (ni.isXMirrored() != ni.isYMirrored()) transValue |= STRANS_REFLX;
		if (ni.isYMirrored()) angle = (3600 - angle)%3600;
		if (ni.isXMirrored()) angle = (1800 - angle)%3600;
		outputHeader(HDR_STRANS, transValue);

		// reduce the size of export text by a factor of 2
		outputMag(0.5);
		outputAngle(angle);
		outputShort((short)12);
		outputShort(HDR_XY);
		Poly portPoly = pp.getOriginalPort().getPoly();
		outputInt((int)(scaleDBUnit(portPoly.getCenterX())));
		outputInt((int)(scaleDBUnit(portPoly.getCenterY())));

		// now the string
		String str = pp.getName();
        if (remapNames) {
            Set<String> nameSet = nameRemapping.get(str);
            if (nameSet != null) {
                str = nameSet.iterator().next();
                str = str + ":" + str;
                //System.out.println("Remapping export "+pp.getName()+" to "+str);
            }
        }
        if (IOTool.getGDSOutputConvertsBracketsInExports()) {
            // convert brackets to underscores
            str = str.replaceAll("[\\[\\]]", "_");
        }
        if (colapseGndVddNames)
        {
            String tmp = str.toLowerCase();
            // Detecting string in lower case and later search for "_"
            if (tmp.startsWith("vdd_") || tmp.startsWith("gnd_"))
                str = str.substring(0, str.indexOf("_"));
        }
        outputString(str, HDR_STRING);
		outputHeader(HDR_ENDEL, 0);
	}

	/**
	 * Method to determine whether or not to merge geometry.
	 */
	protected boolean mergeGeom(int hierLevelsFromBottom)
	{
		return IOTool.isGDSOutMergesBoxes();
	}
	   
    /**
     * Method to determine whether or not to include the original Geometric with a Poly
     */
    protected boolean includeGeometric() { return false; }

	private boolean selectLayer(Foundry foundry, Layer layer)
	{
		boolean validLayer = true;
		GDSLayers numbers = layerNumbers.get(layer);
		if (numbers == null)
		{
//			String layerName = layer.getGDSLayer();
            String layerName = foundry.getGDSLayer(layer);
			if (layerName == null)
			{
				numbers = new GDSLayers();
//				validLayer = false;
			} else
			{
				numbers = GDSLayers.parseLayerString(layerName);
			}
			layerNumbers.put(layer, numbers);
		}
        // validLayer false if layerName = "" like for pseudo metals
        validLayer = numbers.getNumLayers() > 0;
		currentLayerNumbers = numbers;
		return validLayer;
	}

	protected void writePoly(PolyBase poly, int layerNumber, int layerType)
	{
		// ignore negative layer numbers
		if (layerNumber < 0) return;

		Point2D [] points = poly.getPoints();
		if (poly.getStyle() == Poly.Type.DISC)
		{
			// Make a square of the size of the diameter 
			double r = points[0].distance(points[1]);
			if (r <= 0) return;
			Poly newPoly = new Poly(points[0].getX(), points[0].getY(), r*2, r*2);
			outputBoundary(newPoly, layerNumber, layerType);
			return;
		}

		Rectangle2D polyBounds = poly.getBox();
		if (polyBounds != null)
		{
			// rectangular manhattan shape: make sure it has positive area
			if (polyBounds.getWidth() == 0 || polyBounds.getHeight() == 0) return;

			outputBoundary(poly, layerNumber, layerType);
			return;
		}

		// non-manhattan or worse .. direct output
		if (points.length == 1)
		{
			System.out.println("WARNING: Single point cannot be written in GDS-II");
			return;
		}
		if (points.length > 200)
		{
			System.out.println("WARNING: GDS-II Polygons may not have more than 200 points (this has " + points.length + ")");
			return;
		}
		if (points.length == 2) outputPath(poly, layerNumber, layerType); else
		outputBoundary(poly, layerNumber, layerType);
	}

	protected void writeNodable(Nodable no)
	{
		NodeInst ni = (NodeInst)no; // In layout cell all Nodables are NodeInsts
		Cell subCell = (Cell)ni.getProto();

		// figure out transformation
		int transValue = 0;
		int angle = ni.getAngle();
		if (ni.isXMirrored() != ni.isYMirrored()) transValue |= STRANS_REFLX;
		if (ni.isYMirrored()) angle = (3600 - angle)%3600;
		if (ni.isXMirrored()) angle = (1800 - angle)%3600;

		// write a call to a cell
		outputHeader(HDR_SREF, 0);
		String name = cellNames.get(subCell);
		outputName(HDR_SNAME, name, HDR_M_SNAME);
		outputHeader(HDR_STRANS, transValue);
		outputAngle(angle);
		outputShort((short)12);
		outputShort(HDR_XY);
		outputInt((int)(scaleDBUnit(ni.getAnchorCenterX())));
		outputInt((int)(scaleDBUnit(ni.getAnchorCenterY())));
		outputHeader(HDR_ENDEL, 0);
	}

	/****************************** VISITOR SUBCLASS ******************************/

	private BloatVisitor makeBloatVisitor(int maxDepth)
	{
		BloatVisitor visitor = new BloatVisitor(this, maxDepth);
		return visitor;
	}

	/**
	 * Class to override the Geometry visitor and add bloating to all polygons.
	 * Currently, no bloating is being done.
	 */
	private class BloatVisitor extends Geometry.Visitor
	{
		BloatVisitor(Geometry outGeom, int maxHierDepth)
		{
			super(outGeom, maxHierDepth);
		}

		public void addNodeInst(NodeInst ni, AffineTransform trans)
		{
			PrimitiveNode prim = (PrimitiveNode)ni.getProto();
			Technology tech = prim.getTechnology();
            Foundry foundry = tech.getSelectedFoundry();
			Poly [] polys = tech.getShapeOfNode(ni);
			Layer firstLayer = null;
			for (int i=0; i<polys.length; i++)
			{
				Poly poly = polys[i];
				Layer thisLayer = poly.getLayer();
				if (thisLayer != null && firstLayer == null) firstLayer = thisLayer;
				if (poly.getStyle().isText())
				{
					// dump this text field
					outputHeader(HDR_TEXT, 0);
					if (firstLayer != null) selectLayer(foundry, firstLayer);
					Integer firstLayerVal = (Integer)currentLayerNumbers.getFirstLayer();
					int layerNum = firstLayerVal.intValue() & 0xFFFF;
					int layerType = (firstLayerVal.intValue() >> 16) & 0xFFFF;
					outputHeader(HDR_LAYER, layerNum);
					outputHeader(HDR_TEXTTYPE, layerType);
					outputHeader(HDR_PRESENTATION, EXPORTPRESENTATION);

					// figure out transformation
					int transValue = 0;
					int angle = ni.getAngle();
					if (ni.isXMirrored() != ni.isYMirrored()) transValue |= STRANS_REFLX;
					if (ni.isYMirrored()) angle = (3600 - angle)%3600;
					if (ni.isXMirrored()) angle = (1800 - angle)%3600;

					outputHeader(HDR_STRANS, transValue);
					outputAngle(angle);
					outputShort((short)12);
					outputShort(HDR_XY);
					Point2D [] points = poly.getPoints();
					outputInt((int)(scaleDBUnit(points[0].getX())));
					outputInt((int)(scaleDBUnit(points[0].getY())));

					// now the string
					String str = poly.getString();
					outputString(str, HDR_STRING);
					outputHeader(HDR_ENDEL, 0);
				}
				poly.transform(trans);
			}
			cellGeom.addPolys(polys, ni);
		}

		public void addArcInst(ArcInst ai)
		{
			ArcProto ap = ai.getProto();
			Technology tech = ap.getTechnology();
			Poly [] polys = tech.getShapeOfArc(ai);
			cellGeom.addPolys(polys, ai);
		}
	}

	/*************************** GDS OUTPUT ROUTINES ***************************/

	// Initialize various fields, get some standard values
	private void initOutput()
	{
		blockCount = 0;
		bufferPosition = 0;

		// all zeroes
		for (int i=0; i<DSIZE; i++) emptyBuffer[i] = 0;

		Technology tech = topCell.getTechnology();
		scaleFactor = tech.getScale();
		layerNumbers = new HashMap<Layer,GDSLayers>();
        nameRemapping = new HashMap<String,Set<String>>();

		// precache the layers in this technology
		boolean foundValid = false;
        Foundry foundry = tech.getSelectedFoundry();
		for(Iterator<Layer> it = tech.getLayers(); it.hasNext(); )
		{
			Layer layer = it.next();
			if (selectLayer(foundry, layer)) foundValid = true;
		}
		if (!foundValid)
		{
			System.out.println("Warning: there are no GDS II layers defined for the " +
			tech.getTechName() + " technology");
		}

		// make a hashmap of all names to use for cells
		cellNames = new HashMap<Cell,String>();
        buildUniqueNames(topCell, cellNames);
	}

    /**
     * Recursive method to add all cells in the hierarchy to the hashMap
     * with unique names.
     * @param cell the cell whose nodes and subnode cells will be given unique names.
     * @param cellNames a hashmap, key: cell, value: unique name (String).
     */
    public static void buildUniqueNames(Cell cell, HashMap<Cell,String> cellNames) {
        if (!cellNames.containsKey(cell))
            cellNames.put(cell, makeUniqueName(cell, cellNames));
        for (Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); ) {
            NodeInst ni = it.next();
            if (ni.isCellInstance()) {
                Cell c = (Cell)ni.getProto();
                Cell cproto = c.contentsView();
                if (cproto == null) cproto = c;
                if (!cellNames.containsKey(cproto))
                    cellNames.put(cproto, makeUniqueName(cproto, cellNames));
                buildUniqueNames(cproto, cellNames);
            }
        }
    }

	public static String makeUniqueName(Cell cell, HashMap<Cell,String> cellNames)
	{
		String name = makeGDSName(cell.getName(), IOTool.getGDSCellNameLenMax());
		if (cell.getNewestVersion() != cell)
			name += "_" + cell.getVersion();

		// see if the name is unique
		String baseName = name;
		Collection existing = cellNames.values();
        // try prepending the library name first
        if (existing.contains(name)) {
            int liblen = IOTool.getGDSCellNameLenMax() - (name.length() + concatStr.length());  // space for lib name
            if (liblen > 0) {
                String lib = cell.getLibrary().getName();
                liblen = (liblen > lib.length()) ? lib.length() : liblen;
                String libname = lib.substring(0, liblen) + concatStr + name;
                if (!existing.contains(libname)) {
                    System.out.println("Warning: GDSII out renaming cell "+cell.describe(false)+" to "+libname);
                    return libname;
                } else {
                    baseName = libname;
                }
            }
        }
		for(int index = 1; ; index++)
		{
			if (!existing.contains(name)) break;
			name = baseName + "_" + index;
            int extra = name.length() - IOTool.getGDSCellNameLenMax();
            if (extra > 0)
			{
				name = baseName.substring(0, baseName.length()-extra);
                name +="_" + index;
			}
		}

        if (!name.equals(cell.getName()))
            System.out.println("Warning: GDSII out renaming cell "+cell.describe(false)+" to "+name);
		return name;
	}

	/**
	 * function to create proper GDSII names with restricted character set
	 * from input string str.
	 * Uses only 'A'-'Z', '_', $, ?, and '0'-'9'
	 */
	private static String makeGDSName(String str, int maxLen)
	{
		// filter the name string for the GDS output cell
		StringBuffer ret = new StringBuffer();
		int max = str.length();
		if (max > maxLen-3) max = maxLen-3;
		for(int k=0; k<max; k++)
		{
			char ch = str.charAt(k);
			if (IOTool.isGDSOutUpperCase()) ch = Character.toUpperCase(ch);
			if (ch != '$' && !TextUtils.isDigit(ch) && ch != '?' && !Character.isLetter(ch))
				ch = '_';
			ret.append(ch);
		}
		return ret.toString();
	}

    /**
     * Get the name map. GDS output may mangle cell names
     * because of all cells occupy the same name space (no libraries).
     */
    public HashMap<Cell,String> getCellNames() { return cellNames; }

	/*
	 * Close the file, pad to make the file match the tape format
	 */
	private void doneWritingOutput()
	{
		try
		{
			// Write out the current buffer
			if (bufferPosition > 0)
			{
				// Pack with zeroes
				for (int i = bufferPosition; i < DSIZE; i++) dataBufferGDS[i] = 0;
				dataOutputStream.write(dataBufferGDS, 0, DSIZE);
				blockCount++;
			}

			//  Pad to 2048 
			while (blockCount%4 != 0)
			{
				dataOutputStream.write(emptyBuffer, 0, DSIZE);
				blockCount++;
			}
		} catch (IOException e)
		{
			System.out.println("End of file reached while finishing GDS");
		}
	}

	// Write a library header, get the date information
	private void outputBeginLibrary(Cell cell)
	{
		outputHeader(HDR_HEADER, GDSVERSION);
		outputHeader(HDR_BGNLIB, 0);
		outputDate(cell.getCreationDate());
		outputDate(cell.getRevisionDate());
		outputName(HDR_LIBNAME, makeGDSName(cell.getName(), HDR_M_ASCII), HDR_M_ASCII);
		outputShort(HDR_N_UNITS);
		outputShort(HDR_UNITS);

		/* GDS floating point values - -
		 * 0x3E418937,0x4BC6A7EF = 0.001
		 * 0x3944B82F,0xA09B5A53 = 1e-9
		 * 0x3F28F5C2,0x8F5C28F6 = 0.01
		 * 0x3A2AF31D,0xC4611874 = 1e-8
		 */

		// set units
		outputDouble(1e-3);
		outputDouble(1.0e-9);
	}

	void outputBeginStruct(Cell cell)
	{
		outputHeader(HDR_BGNSTR, 0);
		outputDate(cell.getCreationDate());
		outputDate(cell.getRevisionDate());

		String name = cellNames.get(cell);
        if (name == null) {
            System.out.println("Warning, sub"+cell+" in hierarchy is not the same view" +
                    " as top level cell");
            name = makeUniqueName(cell, cellNames);
            cellNames.put(cell, name);
        }
		outputName(HDR_STRNAME, name, IOTool.getGDSCellNameLenMax());
	}

	// Output date information
	private void outputDate(Date val)
	{
		short [] date = new short[6];

		Calendar cal = Calendar.getInstance();
		cal.setTime(val);
		int year = cal.get(Calendar.YEAR) - 1900;
		date[0] = (short)year;
		date[1] = (short)cal.get(Calendar.MONTH);
		date[2] = (short)cal.get(Calendar.DAY_OF_MONTH);
		date[3] = (short)cal.get(Calendar.HOUR);
		date[4] = (short)cal.get(Calendar.MINUTE);
		date[5] = (short)cal.get(Calendar.SECOND);
		outputShortArray(date, 6);
	}

	/*
	 * Write a simple header, with a fixed length
	 * Enter with the header as argument, the routine will output
	 * the count, the header, and the argument (if present) in p1.
	 */
	private void outputHeader(short header, int p1)
	{
		int type = header & BYTEMASK;
		short count = 4;
		if (type != DTYP_NONE)
		{
			switch (header)
			{
				case HDR_HEADER:
				case HDR_LAYER:
				case HDR_DATATYPE:
				case HDR_TEXTTYPE:
				case HDR_STRANS:
				case HDR_PRESENTATION:
					count = 6;
					break;
				case HDR_BGNSTR:
				case HDR_BGNLIB:
					count = HDR_N_BGNLIB;
					break;
				case HDR_UNITS:
					count = HDR_N_UNITS;
					break;
				default:
					System.out.println("No entry for header " + header);
					return;
			}
		}
		outputShort(count);
		outputShort(header);
		if (type == DTYP_NONE) return;
		if (count == 6) outputShort((short)p1);
		if (count == 8) outputInt(p1);
	}

	/*
	 * Add a name (STRNAME, LIBNAME, etc.) to the file. The header
	 * to be used is in header; the string starts at p1
	 * if there is an odd number of bytes, then output the 0 at
	 * the end of the string as a pad. The maximum length of string is "max"
	 */
	private void outputName(short header, String p1, int max)
	{
		outputString(p1, header, max);
	}

	// Output an angle as part of a STRANS
	private void outputAngle(int ang)
	{
		double gdfloat = ang / 10.0;
		outputShort(HDR_N_ANGLE);
		outputShort(HDR_ANGLE);
		outputDouble(gdfloat);
	}

	// Output a magnification as part of a STRANS
	private void outputMag(double scale)
	{
		outputShort(HDR_N_MAG);
		outputShort(HDR_MAG);
		outputDouble(scale);
	}

	// Output the pairs of XY points to the file 
	private void outputBoundary(PolyBase poly, int layerNumber, int layerType)
	{
		Point2D [] points = poly.getPoints();

		// remove redundant points
		Point2D [] newPoints = new Point2D[points.length];
		int count = 0;
		newPoints[count++] = points[0];
		for(int i=1; i<points.length; i++)
		{
			if (points[i].equals(points[i-1])) continue;
			newPoints[count++] = points[i];
		}
		points = newPoints;

		if (count > MAXPOINTS)
		{
//			getbbox(poly, &lx, &hx, &ly, &hy);
//			if (hx-lx > hy-ly)
//			{
//				if (polysplitvert((lx+hx)/2, poly, &side1, &side2)) return;
//			} else
//			{
//				if (polysplithoriz((ly+hy)/2, poly, &side1, &side2)) return;
//			}
//			outputBoundary(side1, layerNumber);
//			outputBoundary(side2, layerNumber);
//			freepolygon(side1);
//			freepolygon(side2);
			return;
		}

		int start = 0;
		for(;;)
		{
			// look for a closed section
			int sofar = start+1;
			for( ; sofar<count; sofar++)
				if (points[sofar].getX() == points[start].getX() && points[sofar].getY() == points[start].getY()) break;
			if (sofar < count) sofar++;
			outputHeader(HDR_BOUNDARY, 0);
			outputHeader(HDR_LAYER, layerNumber);
			outputHeader(HDR_DATATYPE, layerType);
			outputShort((short)(8 * (sofar+1) + 4));
			outputShort(HDR_XY);
			for (int i = start; i <= sofar; i++)
			{
				int j = i;
				if (i == sofar) j = 0;
				outputInt((int)(scaleDBUnit(points[j].getX())));
				outputInt((int)(scaleDBUnit(points[j].getY())));
			}
			outputHeader(HDR_ENDEL, 0);
			if (sofar >= count) break;
			count -= sofar;
			start = sofar;
		}
	}

	private void outputPath(PolyBase poly, int layerNumber, int layerType)
	{
		outputHeader(HDR_PATH, 0);
		outputHeader(HDR_LAYER, layerNumber);
		outputHeader(HDR_DATATYPE, layerType);
		Point2D [] points = poly.getPoints();
		int count = 8 * points.length + 4;
		outputShort((short)count);
		outputShort(HDR_XY);
		for (int i = 0; i < points.length; i ++)
		{
			outputInt((int)(scaleDBUnit(points[i].getX())));
			outputInt((int)(scaleDBUnit(points[i].getY())));
		}
		outputHeader(HDR_ENDEL, 0);
	}

	// Add one byte to the file
	private void outputByte(byte val)
	{
		dataBufferGDS[bufferPosition++] = val;
		if (bufferPosition >= DSIZE)
		{
			try
			{
				dataOutputStream.write(dataBufferGDS, 0, DSIZE);
			} catch (IOException e)
			{
				System.out.println("End of file reached while writing GDS");
			}
			blockCount++;
			bufferPosition = 0;
		}
	}

    private int scaleDBUnit(double dbunit) {
        // scale according to technology
        double scaled = scaleFactor*dbunit;
        // round to nearest nanometer
        int unit = (int)Math.round(scaled);
        return unit;
    }

	/*************************** GDS LOW-LEVEL OUTPUT ROUTINES ***************************/

	// Add a 2-byte integer
	private void outputShort(short val)
	{
		outputByte((byte)((val>>8)&BYTEMASK));
		outputByte((byte)(val&BYTEMASK));
	}

	// Four byte integer
	private void outputInt(int val)
	{
		outputShort((short)(val>>16));
		outputShort((short)val);
	}

	// Array of 2 byte integers in array ptr, count n
	private void outputShortArray(short [] ptr, int n)
	{
		for (int i = 0; i < n; i++) outputShort(ptr[i]);
	}

	// Array of 4-byte integers or floating numbers in array  ptr, count n
	private void outputIntArray(int [] ptr, int n)
	{
		for (int i = 0; i < n; i++) outputInt(ptr[i]);
	}

    private void outputString(String str, short header) {
        // The usual maximum length for string is 512, though names etc may need to be shorter
        outputString(str, header, 512);
    }

    /**
     * String of n bytes, starting at ptr
     * Revised 90-11-23 to convert to upper case (SRP)
     */
    private void outputString(String str, short header, int max) {
        int j = str.length();
        if (j > max) j = max;

        // round up string length to the nearest integer
        if ((j % 2) != 0) {
            j = (int)(j / 2)*2 + 2;
        }
        // pad with a blank
        outputShort((short)(4+j));
        outputShort(header);

        assert( (j%2) == 0);
        //System.out.println("Writing string "+str+" (length "+str.length()+") using "+j+" bytes");
		int i = 0;
		if (IOTool.isGDSOutUpperCase())
		{
			// convert to upper case
			for( ; i<str.length(); i++)
				outputByte((byte)Character.toUpperCase(str.charAt(i)));
		} else
		{
			for( ; i<str.length(); i++)
				outputByte((byte)str.charAt(i));
		}
		for ( ; i < j; i++)
			outputByte((byte)0);
	}

	/**
	 * Method to write a GDSII representation of a double.
	 * New conversion code contributed by Tom Valine <tomv@transmeta.com>.
	 * @param data the double to process.
	 */
	public void outputDouble(double data)
	{
		if (data == 0.0)
		{
			for(int i=0; i<8; i++) outputByte((byte)0);
			return;
		}
		BigDecimal reg = new BigDecimal((double)data).setScale(64, BigDecimal.ROUND_HALF_EVEN);

		boolean negSign = false;
		if (reg.doubleValue() < 0)
		{
			negSign = true;
			reg = reg.negate();
		}

		int exponent = 64;
		for(; (reg.doubleValue() < 0.0625) && (exponent > 0); exponent--)
			reg = reg.multiply(new BigDecimal(16.0));
		if (exponent == 0) System.out.println("Exponent underflow");
		for(; (reg.doubleValue() >= 1) && (exponent < 128); exponent++)
			reg = reg.divide(new BigDecimal(16.0), BigDecimal.ROUND_HALF_EVEN);
		if (exponent > 127) System.out.println("Exponent overflow");
		if (negSign) exponent |= 0x00000080;
		BigDecimal f_mantissa = reg.subtract(new BigDecimal(reg.intValue()));
		for(int i = 0; i < 56; i++)
			f_mantissa = f_mantissa.multiply(new BigDecimal(2.0));
		long mantissa = f_mantissa.longValue();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(exponent);
		for(int i = 6; i >= 0; i--)
			baos.write((int)((mantissa >> (i * 8)) & 0xFF));
		byte [] result = baos.toByteArray();
		for(int i=0; i<8; i++) outputByte(result[i]);
	}

//	/**
//	 * Method to write a GDSII representation of a double.
//	 * Original C-Electric code (no longer used).
//	 * @param data the double to process.
//	 */
//	private void outputDouble(double a)
//	{
//		int [] ret = new int[2];
//
//		// handle default
//		if (a == 0)
//		{
//			ret[0] = 0x40000000;
//			ret[1] = 0;
//			outputIntArray(ret, 2);
//			return;
//		}
//
//		// identify sign
//		double temp = a;
//		boolean negsign = false;
//		if (temp < 0)
//		{
//			negsign = true;
//			temp = -temp;
//		}
//
//		// establish the excess-64 exponent value
//		int exponent = 64;
//
//		// scale the exponent and mantissa
//		for (; temp < 0.0625 && exponent > 0; exponent--) temp *= 16.0;
//
//		if (exponent == 0) System.out.println("Exponent underflow");
//
//		for (; temp >= 1 && exponent < 128; exponent++) temp /= 16.0;
//
//		if (exponent > 127) System.out.println("Exponent overflow");
//
//		// set the sign
//		if (negsign) exponent |= 0x80;
//
//		// convert temp to 7-byte binary integer
//		double top = temp;
//		for (int i = 0; i < 24; i++) top *= 2;
//		int highmantissa = (int)top;
//		double frac = top - highmantissa;
//		for (int i = 0; i < 32; i++) frac *= 2;
//		ret[0] = highmantissa | (exponent<<24);
//		ret[1] = (int)frac;
//		outputIntArray(ret, 2);
//	}
}
