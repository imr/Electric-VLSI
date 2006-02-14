/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DXF.java
 * Input/output tool: DXF input
 * Written by Steven M. Rubin, Sun Microsystems.
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
import com.sun.electric.database.geometry.Orientation;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.View;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.TextDescriptor;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.technology.technologies.Generic;
import com.sun.electric.tool.Job;
import com.sun.electric.tool.io.IOTool;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * This class reads files in DEF files.
 */
public class DXF extends Input
{
	private static class DXFLayer
	{
		private String   layerName;
		private int		 layerColor;
		private double   layerRed, layerGreen, layerBlue;
		private DXFLayer next;
	}

	private static class ForwardRef
	{
		private String     refName;
		private Cell       parent;
		private double     x, y;
		private int        rot;
		private int        xRep, yRep;
		private double     xSpa, ySpa;
		private double     xSca, ySca;
		private ForwardRef nextForwardRef;
	}

	private static class PolyPoint
	{
		double x, y, z;
		double bulge;
	}

	private int                 lastGroupID;
	private String              lastText;
	private boolean             lastPairValid;
	private int                 ignoredPoints, ignoredAttributeDefs, ignoredAttributes;
	private int                 readPolyLines, readLines, readCircles, readSolids,
								read3DFaces, readArcs, readInserts, readTexts;
	private DXFLayer            firstLayer;
	private ForwardRef          firstForwardRef;
	private Cell                mainCell;
	private Cell                curCell;
	private List<String>        headerText;
	private List<Integer>       headerID;
	private int                 inputMode;			/* 0: pairs not read, 1: normal pairs, 2: blank lines */
	private HashSet<String>     validLayerNames;
	private HashSet<String>     ignoredLayerNames;
	private TextUtils.UnitScale dispUnit;
	private int                 groupID;
	private String              text;
	/** key of Variable holding DXF layer name. */			public static final Variable.Key DXF_LAYER_KEY = Variable.newKey("IO_dxf_layer");
	/** key of Variable holding DXF header text. */			public static final Variable.Key DXF_HEADER_TEXT_KEY = Variable.newKey("IO_dxf_header_text");
	/** key of Variable holding DXF header information. */	public static final Variable.Key DXF_HEADER_ID_KEY = Variable.newKey("IO_dxf_header_ID");

	/**
	 * Method to import a library from disk.
	 * @param lib the library to fill
	 * @return true on error.
	 */
	protected boolean importALibrary(Library lib)
	{
		boolean ret = false;
		try
		{
			ret = readLibrary(lib);
		} catch (IOException e)
		{
		}
		return ret;
	}

	/**
	 * Method to read the DXF file into library "lib".  Returns true on error.
	 */
	private boolean readLibrary(Library lib)
		throws IOException
	{
		// set the scale
		setCurUnits();

		// examine technology for acceptable DXF layer names
		getAcceptableLayers();

		// make the only cell in this library
		mainCell = Cell.makeInstance(lib, lib.getName());
		if (mainCell == null) return true;
		Job.getUserInterface().setCurrentCell(lib, mainCell);
		curCell = mainCell;
		headerID = new ArrayList<Integer>();
		headerText = new ArrayList<String>();
		ignoredLayerNames = new HashSet<String>();

		// read the file
		lastPairValid = false;
		boolean err = false;
		firstLayer = null;
		firstForwardRef = null;
		ignoredPoints = ignoredAttributes = ignoredAttributeDefs = 0;
		readPolyLines = readLines = readCircles = readSolids = 0;
		read3DFaces = readArcs = readInserts = readTexts = 0;
		inputMode = 0;
		for(;;)
		{
			if (getNextPair()) break;

			// must have section change here
			if (groupID != 0)
			{
				System.out.println("Expected group 0 (start section) at line " + lineReader.getLineNumber());
				break;
			}

			if (text.equals("EOF")) break;

			if (text.equals("SECTION"))
			{
				// see what section is coming
				if (getNextPair()) break;
				if (groupID != 2)
				{
					System.out.println("Expected group 2 (name) at line " + lineReader.getLineNumber());
					err = true;
					break;
				}
				if (text.equals("HEADER"))
				{
					if (err = readHeaderSection()) break;
					continue;
				}
				if (text.equals("TABLES"))
				{
					if (err = readTablesSection()) break;
					continue;
				}
				if (text.equals("BLOCKS"))
				{
					if (err = readEntities(lib)) break;
					continue;
				}
				if (text.equals("ENTITIES"))
				{
					if (err = readEntities(lib)) break;
					continue;
				}
				if (text.equals("CLASSES"))
				{
					if (err = ignoreSection()) break;
					continue;
				}
				if (text.equals("OBJECTS"))
				{
					if (err = ignoreSection()) break;
					continue;
				}
			}
			System.out.println("Unknown section name (" + text + ") at line " + lineReader.getLineNumber());
			err = true;
			break;
		}

		// insert forward references
		for(ForwardRef fr = firstForwardRef; fr != null; fr = fr.nextForwardRef)
		{
			// have to search by hand because of weird prototype names
			Cell found = null;
			for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
			{
				Cell cell = it.next();
				if (cell.getName().equals(fr.refName)) { found = cell;   break; }
			}
			if (found == null)
			{
				System.out.println("Cannot find block '" + fr.refName + "'");
				continue;
			}
			if (IOTool.isDXFInputFlattensHierarchy())
			{
				if (extractInsert(found, fr.x, fr.y, fr.xSca, fr.ySca, fr.rot, fr.parent)) return true;
			} else
			{
				if (fr.xSca != 1.0 || fr.ySca != 1.0)
				{
					found = getScaledCell(found, fr.xSca, fr.ySca);
					if (found == null) return true;
				}
				Rectangle2D bounds = found.getBounds();
                Orientation orient = Orientation.fromAngle(fr.rot*10);
				NodeInst ni = NodeInst.makeInstance(found, new Point2D.Double(fr.x, fr.y), bounds.getWidth(), bounds.getHeight(), fr.parent, orient, null, 0);
//				NodeInst ni = NodeInst.makeInstance(found, new Point2D.Double(fr.x, fr.y), bounds.getWidth(), bounds.getHeight(), fr.parent, fr.rot*10, null, 0);
				if (ni == null) return true;
				ni.setExpanded();
			}
		}

		// save header with library
		if (headerID.size() > 0)
		{
			int len = headerID.size();
			Integer [] headerIDs = new Integer[len];
			for(int i=0; i<len; i++) headerIDs[i] = headerID.get(i);
			lib.newVar(DXF_HEADER_ID_KEY, headerIDs);
		}
		if (headerText.size() > 0)
		{
			int len = headerText.size();
			String [] headerTexts = new String[len];
			for(int i=0; i<len; i++) headerTexts[i] = headerText.get(i);
			lib.newVar(DXF_HEADER_TEXT_KEY, headerTexts);
		}

		if (readPolyLines > 0 || readLines > 0 || readCircles > 0 ||
			readSolids > 0 || read3DFaces > 0 || readArcs > 0 ||
			readTexts > 0 || readInserts > 0)
		{
			String warning = "Read";
			boolean first = true;
			if (readPolyLines > 0)
			{
				if (first) warning += ",";	first = false;
				warning += " " + readPolyLines + " polylines";
			}
			if (readLines > 0)
			{
				if (first) warning += ",";	first = false;
				warning += " " + readLines + " lines";
			}
			if (readCircles > 0)
			{
				if (first) warning += ",";	first = false;
				warning += " " + readCircles + " circles";
			}
			if (readSolids > 0)
			{
				if (first) warning += ",";	first = false;
				warning += " " + readSolids + " solids";
			}
			if (read3DFaces > 0)
			{
				if (first) warning += ",";	first = false;
				warning += " " + read3DFaces + " 3d faces";
			}
			if (readArcs > 0)
			{
				if (first) warning += ",";	first = false;
				warning += " " + readArcs + " arcs";
			}
			if (readTexts > 0)
			{
				if (first) warning += ",";	first = false;
				warning += " " + readTexts + " texts";
			}
			if (readInserts > 0)
			{
				if (first) warning += ",";	first = false;
				warning += " " + readInserts + " inserts";
			}
			System.out.println(warning);
		}

		if (ignoredPoints > 0 || ignoredAttributes > 0 || ignoredAttributeDefs > 0)
		{
			String warning = "Ignored";
			boolean first = true;
			if (ignoredPoints > 0)
			{
				if (first) warning += ",";	first = false;
				warning += " " + ignoredPoints + " points";
			}
			if (ignoredAttributes > 0)
			{
				if (first) warning += ",";	first = false;
				warning += " " + ignoredAttributes + " attributes";
			}
			if (ignoredAttributeDefs > 0)
			{
				if (first) warning += ",";	first = false;
				warning += " " + ignoredAttributeDefs + " attribute definitions";
			}
			System.out.println(warning);
		}

		// say which layers were ignored
		if (ignoredLayerNames.size() > 0)
		{
			String warning = "Ignored layers ";
			boolean first = true;
			for(String name : ignoredLayerNames)
			{
				if (!first) warning += ", ";
				first = false;
				warning += "'" + name + "'";
			}
			System.out.println(warning);
		}

		return err;
	}

	/**
	 * Method to read the next group ID and content pair from the file.
	 * Returns true on end-of-file.
	 */
	private boolean getNextPair()
		throws IOException
	{
		if (lastPairValid)
		{
			text = lastText;
			groupID = lastGroupID;
			lastPairValid = false;
			return false;
		}

		for(;;)
		{
			// read a line and get the group ID
			lastText = getNextLine(false);
			if (lastText == null)
			{
				System.out.println("Unexpected end-of-file at line " + lineReader.getLineNumber());
				return true;
			}
			String groupLine = lastText.trim();
			if (!TextUtils.isANumber(groupLine))
			{
				System.out.println("Invalid group ID on line " + lineReader.getLineNumber() + " (" + lastText + ")");
				return true;
			}
			groupID = TextUtils.atoi(groupLine);

			// ignore blank line if file is double-spaced
			if (inputMode == 2) getNextLine(true);

			// read a line and get the text
			lastText = getNextLine(true);
			if (lastText == null)
			{
				System.out.println("Unexpected end-of-file at line " + lineReader.getLineNumber());
				return true;
			}
			text = lastText.trim();

			// ignore blank line if file is double-spaced
			if (inputMode == 2) getNextLine(true);

			if (inputMode == 0)
			{
				// see if file is single or double spaced
				if (lastText.length() != 0) inputMode = 1; else
				{
					inputMode = 2;
					lastText = getNextLine(true);
					if (lastText == null)
					{
						System.out.println("Unexpected end-of-file at line " + lineReader.getLineNumber());
						return true;
					}
					text = lastText;
					getNextLine(true);
				}
			}

			// continue reading if a comment, otherwise quit
			if (groupID != 999) break;
		}

		return false;
	}

	private String getNextLine(boolean canBeBlank)
		throws IOException
	{
		for(;;)
		{
			String text = lineReader.readLine();
			if (canBeBlank || text.length() != 0) return text;
		}
	}

	/****************************************** READING SECTIONS ******************************************/

	private boolean readHeaderSection()
		throws IOException
	{
		// just save everything until the end-of-section
		for(int line=0; ; line++)
		{
			if (getNextPair()) return true;
			if (groupID == 0 && text.equals("ENDSEC")) break;

			// save it
			headerID.add(new Integer(groupID));
			headerText.add(text);
		}
		return false;
	}

	private boolean readTablesSection()
		throws IOException
	{
		// just ignore everything until the end-of-section
		for(;;)
		{
			if (getNextPair()) return true;

			// quit now if at the end of the table section
			if (groupID == 0 && text.equals("ENDSEC")) break;

			// must be a 'TABLE' declaration
			if (groupID != 0 || !text.equals("TABLE")) continue;

			// a table: see what kind it is
			if (getNextPair()) return true;
			if (groupID != 2 || !text.equals("LAYER")) continue;

			// a layer table: ignore the size information
			if (getNextPair()) return true;
			if (groupID != 70) continue;

			// read the layers
			DXFLayer layer = null;
			for(;;)
			{
				if (getNextPair()) return true;
				if (groupID == 0 && text.equals("ENDTAB")) break;
				if (groupID == 0 && text.equals("LAYER"))
				{
					// make a new layer
					layer = new DXFLayer();
					layer.layerName = null;
					layer.layerColor = -1;
					layer.layerRed = 1.0;
					layer.layerGreen = 1.0;
					layer.layerBlue = 1.0;
					layer.next = firstLayer;
					firstLayer = layer;
				}
				if (groupID == 2 && layer != null)
				{
					layer.layerName = text;
				}
				if (groupID == 62 && layer != null)
				{
					layer.layerColor = TextUtils.atoi(text);
					DXFLayer found = null;
					for(DXFLayer l = firstLayer; l != null; l = l.next)
					{
						if (l == layer) continue;
						if (l.layerColor == layer.layerColor) { found = l;   break; }
					}
					if (found != null)
					{
						layer.layerRed = found.layerRed;
						layer.layerGreen = found.layerGreen;
						layer.layerBlue = found.layerBlue;
					} else
					{
						switch (layer.layerColor)
						{
							case 1:			    // red
								layer.layerRed = 1.0;    layer.layerGreen = 0.0;    layer.layerBlue = 0.0;
								break;
							case 2:			    // yellow
								layer.layerRed = 1.0;    layer.layerGreen = 1.0;    layer.layerBlue = 0.0;
								break;
							case 3:			    // green
								layer.layerRed = 0.0;    layer.layerGreen = 1.0;    layer.layerBlue = 0.0;
								break;
							case 4:			    // cyan
								layer.layerRed = 0.0;    layer.layerGreen = 1.0;    layer.layerBlue = 1.0;
								break;
							case 5:			    // blue
								layer.layerRed = 0.0;    layer.layerGreen = 0.0;    layer.layerBlue = 1.0;
								break;
							case 6:			    // magenta
								layer.layerRed = 1.0;    layer.layerGreen = 0.0;    layer.layerBlue = 1.0;
								break;
							case 7:			    // white (well, gray)
								layer.layerRed = 0.75;    layer.layerGreen = 0.75;    layer.layerBlue = 0.75;
								break;
							default:			// unknown layer
								layer.layerRed = Math.random();
								layer.layerGreen = Math.random();
								layer.layerBlue = Math.random();
								break;
						}
					}
				}
			}
		}
		return false;
	}

	private boolean ignoreSection()
		throws IOException
	{
		// just ignore everything until the end-of-section
		for(;;)
		{
			if (getNextPair()) return true;
			if (groupID == 0 && text.equals("ENDSEC")) break;
		}
		return false;
	}

	/****************************************** READING ENTITIES ******************************************/

	private boolean readEntities(Library lib)
		throws IOException
	{
		// read the blocks/entities section
		for(;;)
		{
			if (getNextPair()) return true;
			if (groupID != 0)
			{
				System.out.println("Unknown group code (" + groupID + ") at line " + lineReader.getLineNumber());
				return true;
			}
			if (text.equals("ARC"))
			{
				if (readArcEntity()) return true;
				continue;
			}
			if (text.equals("ATTDEF"))
			{
				ignoreEntity();
				ignoredAttributeDefs++;
				continue;
			}
			if (text.equals("ATTRIB"))
			{
				ignoreEntity();
				ignoredAttributes++;
				continue;
			}
			if (text.equals("BLOCK"))
			{
				String msg = readBlock();
				if (msg == null) return true;
				curCell = Cell.makeInstance(lib, makeBlockName(msg));
				if (curCell == null) return true;
				continue;
			}
			if (text.equals("CIRCLE"))
			{
				if (readCircleEntity()) return true;
				continue;
			}
			if (text.equals("ENDBLK"))
			{
				ignoreEntity();
				curCell = mainCell;
				continue;
			}
			if (text.equals("ENDSEC"))
			{
				break;
			}
			if (text.equals("INSERT"))
			{
				if (readInsertEntity(lib)) return true;
				continue;
			}
			if (text.equals("LINE"))
			{
				if (readLineEntity()) return true;
				continue;
			}
			if (text.equals("POINT"))
			{
				ignoreEntity();
				ignoredPoints++;
				continue;
			}
			if (text.equals("POLYLINE"))
			{
				if (readPolyLineEntity()) return true;
				continue;
			}
			if (text.equals("SEQEND"))
			{
				ignoreEntity();
				continue;
			}
			if (text.equals("SOLID"))
			{
				if (readSolidEntity()) return true;
				continue;
			}
			if (text.equals("TEXT"))
			{
				if (readTextEntity()) return true;
				continue;
			}
			if (text.equals("VIEWPORT"))
			{
				ignoreEntity();
				continue;
			}
			if (text.equals("3DFACE"))
			{
				if (read3DFaceEntity()) return true;
				continue;
			}
			System.out.println("Unknown entity type (" + text + ") at line " + lineReader.getLineNumber());
			return true;
		}
		return false;
	}

	private double scaleString(String text)
	{
		double v = TextUtils.atof(text);
		return TextUtils.convertFromDistance(v, Artwork.tech, dispUnit);
	}

	private boolean readArcEntity()
		throws IOException
	{
		DXFLayer layer = null;
		double x = 0, y = 0, z = 0;
		double rad = 0;
		double sAngle = 0, eAngle = 0;
		for(;;)
		{
			if (getNextPair()) return true;
			switch (groupID)
			{
				case 8:  layer = getLayer(text);          break;
				case 10: x = scaleString(text);           break;
				case 20: y = scaleString(text);           break;
				case 30: z = scaleString(text);           break;
				case 40: rad = scaleString(text);         break;
				case 50: sAngle = TextUtils.atof(text);   break;
				case 51: eAngle = TextUtils.atof(text);   break;
			}
			if (groupID == 0)
			{
				pushPair(groupID, text);
				break;
			}
		}
		if (!isAcceptableLayer(layer)) return false;
		if (sAngle >= 360.0) sAngle -= 360.0;
		int iAngle = (int)(sAngle * 10.0);
		Orientation orient = Orientation.fromAngle(iAngle);
		NodeInst ni = NodeInst.makeInstance(Artwork.tech.circleNode, new Point2D.Double(x, y), rad*2, rad*2, curCell, orient, null, 0);
//		NodeInst ni = NodeInst.makeInstance(Artwork.tech.circleNode, new Point2D.Double(x, y), rad*2, rad*2, curCell, iAngle%3600, null, 0);
		if (ni == null) return true;
		if (sAngle > eAngle) eAngle += 360.0;
		double startOffset = sAngle;
		startOffset -= (double)iAngle / 10.0;
		ni.setArcDegrees(startOffset * Math.PI / 1800.0, (eAngle-sAngle) * Math.PI / 180.0);
		ni.newVar(DXF_LAYER_KEY, layer.layerName);
		readArcs++;
		return false;
	}

	private String readBlock()
		throws IOException
	{
		String saveMsg = null;
		for(;;)
		{
			if (getNextPair()) return null;
			if (groupID == 2) saveMsg = text; else
			if (groupID == 0)
			{
				pushPair(groupID, text);
				break;
			}
		}
		return saveMsg;
	}

	private boolean readCircleEntity()
		throws IOException
	{
		DXFLayer layer = null;
		double x = 0, y = 0, z = 0;
		double rad = 0;
		for(;;)
		{
			if (getNextPair()) return true;
			switch (groupID)
			{
				case 8:  layer = getLayer(text);    break;
				case 10: x = scaleString(text);     break;
				case 20: y = scaleString(text);     break;
				case 30: z = scaleString(text);     break;
				case 40: rad = scaleString(text);   break;
			}
			if (groupID == 0)
			{
				pushPair(groupID, text);
				break;
			}
		}
		if (!isAcceptableLayer(layer)) return false;
		NodeInst ni = NodeInst.makeInstance(Artwork.tech.circleNode, new Point2D.Double(x, y), rad*2, rad*2, curCell);
		if (ni == null) return true;
		ni.newVar(DXF_LAYER_KEY, layer.layerName);
		readCircles++;
		return false;
	}

	private boolean readInsertEntity(Library lib)
		throws IOException
	{
		DXFLayer layer = null;
		int rot = 0;
		String name = null;
		int xRep = 1, yRep = 1;
		double x = 0, y = 0, z = 0;
		double xSpa = 0, ySpa = 0;
		double xSca = 1, ySca = 1;
		for(;;)
		{
			if (getNextPair()) return true;
			switch (groupID)
			{
				case 8:  layer = getLayer(text);          break;
				case 10: x = scaleString(text);           break;
				case 20: y = scaleString(text);           break;
				case 30: z = scaleString(text);           break;
				case 50: rot = TextUtils.atoi(text);      break;
				case 41: xSca = TextUtils.atof(text);     break;
				case 42: ySca = TextUtils.atof(text);     break;
				case 70: xRep = TextUtils.atoi(text);     break;
				case 71: yRep = TextUtils.atoi(text);     break;
				case 44: xSpa = scaleString(text);        break;
				case 45: ySpa = scaleString(text);        break;
				case 2:  name = text;                     break;
			}
			if (groupID == 0)
			{
				pushPair(groupID, text);
				break;
			}
		}

		String pt = makeBlockName(name);
		if (pt != null)
		{
			if (xRep != 1 || yRep != 1)
			{
				System.out.println("Cannot insert block '" + pt + "' repeated " + xRep + "x" + yRep + " times");
				return false;
			}

			// have to search by hand because of weird prototype names
			Cell found = null;
			for(Iterator<Cell> it = lib.getCells(); it.hasNext(); )
			{
				Cell np = it.next();
				if (np.getName().equals(pt)) { found = np;   break; }
			}
			if (found == null)
			{
				ForwardRef fr = new ForwardRef();
				fr.refName = pt;
				fr.parent = curCell;
				fr.x = x;		fr.y = y;
				fr.rot = rot;
				fr.xRep = xRep;	fr.yRep = yRep;
				fr.xSpa = xSpa;	fr.ySpa = ySpa;
				fr.xSca = xSca;	fr.ySca = ySca;
				fr.nextForwardRef = firstForwardRef;
				firstForwardRef = fr;
				return false;
			}

			if (IOTool.isDXFInputFlattensHierarchy())
			{
				if (extractInsert(found, x, y, xSca, ySca, rot, curCell)) return true;
			} else
			{
				if (xSca != 1.0 || ySca != 1.0)
				{
					found = getScaledCell(found, xSca, ySca);
					if (found == null) return true;
				}
				double sX = found.getDefWidth();
				double sY = found.getDefHeight();
                Orientation orient = Orientation.fromAngle(rot*10);
				NodeInst ni = NodeInst.makeInstance(found, new Point2D.Double(x, y), sX, sY, curCell, orient, null, 0);
//				NodeInst ni = NodeInst.makeInstance(found, new Point2D.Double(x, y), sX, sY, curCell, rot*10, null, 0);
				if (ni == null) return true;
				ni.setExpanded();
			}
		}
		readInserts++;
		return false;
	}

	private boolean readLineEntity()
		throws IOException
	{
		DXFLayer layer = null;
		int lineType = 0;
		double x1 = 0, y1 = 0, z1 = 0;
		double x2 = 0, y2 = 0, z2 = 0;
		for(;;)
		{
			if (getNextPair()) return true;
			switch (groupID)
			{
				case 8:  layer = getLayer(text);     break;
				case 10: x1 = scaleString(text);     break;
				case 20: y1 = scaleString(text);     break;
				case 30: z1 = scaleString(text);     break;
				case 11: x2 = scaleString(text);     break;
				case 21: y2 = scaleString(text);     break;
				case 31: z2 = scaleString(text);     break;
			}
			if (groupID == 0)
			{
				pushPair(groupID, text);
				break;
			}
		}
		if (!isAcceptableLayer(layer)) return false;
		double cX = (x1 + x2) / 2;
		double cY = (y1 + y2) / 2;
		double sX = Math.abs(x1 - x2);
		double sY = Math.abs(y1 - y2);
		NodeProto np = Artwork.tech.openedDashedPolygonNode;
		if (lineType == 0) np = Artwork.tech.openedPolygonNode;
		NodeInst ni = NodeInst.makeInstance(np, new Point2D.Double(cX, cY), sX, sY, curCell);
		if (ni == null) return true;
		EPoint [] points = new EPoint[2];
		points[0] = new EPoint(x1 - cX, y1 - cY);
		points[1] = new EPoint(x2 - cX, y2 - cY);
		ni.newVar(NodeInst.TRACE, points);
		ni.newVar(DXF_LAYER_KEY, layer.layerName);
		readLines++;
		return false;
	}

	private boolean readPolyLineEntity()
		throws IOException
	{
		boolean closed = false;
		DXFLayer layer = null;
		int lineType = 0;
		boolean inEnd = false;
		List<PolyPoint> polyPoints = new ArrayList<PolyPoint>();
		PolyPoint curPP = null;
		boolean hasBulgeInfo = false;
		for(;;)
		{
			if (getNextPair()) return true;
			if (groupID == 8)
			{
				layer = getLayer(text);
				continue;
			}

			if (groupID == 10)
			{
				if (curPP != null) curPP.x = scaleString(text);
				continue;
			}
			if (groupID == 20)
			{
				if (curPP != null) curPP.y = scaleString(text);
				continue;
			}
			if (groupID == 30)
			{
				if (curPP != null) curPP.z = scaleString(text);
				continue;
			}
			if (groupID == 42)
			{
				if (curPP != null)
				{
					curPP.bulge = TextUtils.atof(text);
					if (curPP.bulge != 0) hasBulgeInfo = true;
				}
				continue;
			}
			if (groupID == 70)
			{
				int i = TextUtils.atoi(text);
				if ((i&1) != 0) closed = true;
				continue;
			}

			if (groupID == 0)
			{
				if (inEnd)
				{
					pushPair(groupID, text);
					break;
				}
				if (text.equals("SEQEND"))
				{
					inEnd = true;
					continue;
				}
				if (text.equals("VERTEX"))
				{
					curPP = new PolyPoint();
					curPP.bulge = 0;
					polyPoints.add(curPP);
				}
				continue;
			}
		}

		int count = polyPoints.size();
		if (isAcceptableLayer(layer) && count >= 3)
		{
			// see if there is bulge information
			if (hasBulgeInfo)
			{
				// handle bulges
				int start = 1;
				if (closed) start = 0;
				for(int i=start; i<count; i++)
				{
					int last = i - 1;
					if (i == 0) last = count-1;
					PolyPoint pp = polyPoints.get(i);
					PolyPoint lastPp = polyPoints.get(last);
					double x1 = lastPp.x;   double y1 = lastPp.y;
					double x2 = pp.x;       double y2 = pp.y;
					if (lastPp.bulge != 0.0)
					{
						// special case the semicircle bulges
						if (Math.abs(lastPp.bulge) == 1.0)
						{
							double cX = (x1 + x2) / 2;
							double cY = (y1 + y2) / 2;
							if ((y1 == cY && x1 == cX) || (y2 == cY && x2 == cX))
							{
								System.out.println("Domain error in polyline bulge computation");
								continue;
							}
							double sA = Math.atan2(y1-cY, x1-cX);
							double eA = Math.atan2(y2-cY, x2-cX);
							if (lastPp.bulge < 0.0)
							{
								double r2 = sA;   sA = eA;   eA = r2;
							}
							if (sA < 0.0) sA += 2.0 * Math.PI;
							sA = sA * 1800.0 / Math.PI;
							int iAngle = (int)sA;
							double rad = new Point2D.Double(cX, cY).distance(new Point2D.Double(x1, y1));
                            Orientation orient = Orientation.fromAngle(iAngle);
							NodeInst ni = NodeInst.makeInstance(Artwork.tech.circleNode, new Point2D.Double(cX, cY), rad*2, rad*2, curCell, orient, null, 0);
//							NodeInst ni = NodeInst.makeInstance(Artwork.tech.circleNode, new Point2D.Double(cX, cY), rad*2, rad*2, curCell, iAngle, null, 0);
							if (ni == null) return true;
							double startOffset = sA;
							startOffset -= iAngle;
							ni.setArcDegrees(startOffset * Math.PI / 1800.0, Math.PI);
							ni.newVar(DXF_LAYER_KEY, layer.layerName);
							continue;
						}

						// compute half distance between the points
						double x01 = x1;   double y01 = y1;
						double x02 = x2;   double y02 = y2;
						double dx = x02 - x01;   double dy = y02 - y01;
						double dist = Math.sqrt(dx*dx + dy*dy);

						// compute radius of arc (bulge is tangent of 1/4 of included arc angle)
						double incAngle = Math.atan(lastPp.bulge) * 4.0;
						double arcRad = Math.abs((dist / 2.0) / Math.sin(incAngle / 2.0));
						double rad = arcRad;

						// prepare to compute the two circle centers
						double r2 = arcRad*arcRad;
						double delta_1 = -dist / 2.0;
						double delta_12 = delta_1 * delta_1;
						double delta_2 = Math.sqrt(r2 - delta_12);

						// pick one center, according to bulge sign
						double bulgeSign = lastPp.bulge;
						if (Math.abs(bulgeSign) > 1.0) bulgeSign = -bulgeSign;
						double xcf = 0, ycf = 0;
						if (bulgeSign > 0.0)
						{
							xcf = x02 + ((delta_1 * (x02-x01)) + (delta_2 * (y01-y02))) / dist;
							ycf = y02 + ((delta_1 * (y02-y01)) + (delta_2 * (x02-x01))) / dist;
						} else
						{
							xcf = x02 + ((delta_1 * (x02-x01)) + (delta_2 * (y02-y01))) / dist;
							ycf = y02 + ((delta_1 * (y02-y01)) + (delta_2 * (x01-x02))) / dist;
						}
						x1 = xcf;   y1 = ycf;

						// compute angles to the arc endpoints
						if ((y01 == ycf && x01 == xcf) || (y02 == ycf && x02 == xcf))
						{
							System.out.println("Domain error in polyline computation");
							continue;
						}
						double sA = Math.atan2(y01-ycf, x01-xcf);
						double eA = Math.atan2(y02-ycf, x02-xcf);
						if (lastPp.bulge < 0.0)
						{
							r2 = sA;   sA = eA;   eA = r2;
						}
						if (sA < 0.0) sA += 2.0 * Math.PI;
						if (eA < 0.0) eA += 2.0 * Math.PI;
						sA = sA * 1800.0 / Math.PI;
						eA = eA * 1800.0 / Math.PI;

						// create the arc node
						int iAngle = (int)sA;
                        Orientation orient = Orientation.fromAngle(iAngle);
						NodeInst ni = NodeInst.makeInstance(Artwork.tech.circleNode, new Point2D.Double(x1, y1), rad*2, rad*2, curCell, orient, null, 0);
//						NodeInst ni = NodeInst.makeInstance(Artwork.tech.circleNode, new Point2D.Double(x1, y1), rad*2, rad*2, curCell, iAngle%3600, null, 0);
						if (ni == null) return true;
						if (sA > eA) eA += 3600.0;
						double startOffset = sA;
						startOffset -= (double)iAngle;
						ni.setArcDegrees(startOffset * Math.PI / 1800.0, (eA-sA) * Math.PI / 1800.0);
						ni.newVar(DXF_LAYER_KEY, layer.layerName);
						continue;
					}

					// this segment has no bulge
					double cX = (x1 + x2) / 2;
					double cY = (y1 + y2) / 2;
					NodeProto np = Artwork.tech.openedDashedPolygonNode;
					if (lineType == 0) np = Artwork.tech.openedPolygonNode;
					NodeInst ni = NodeInst.makeInstance(np, new Point2D.Double(cX, cY), Math.abs(x1 - x2), Math.abs(y1 - y2), curCell);
					if (ni == null) return true;
					Point2D [] points = new Point2D[2];
					points[0] = new Point2D.Double(x1 - cX, y1 - cY);
					points[1] = new Point2D.Double(x2 - cX, y2 - cY);
					ni.newVar(NodeInst.TRACE, points);
					ni.newVar(DXF_LAYER_KEY, layer.layerName);
				}
			} else
			{
				// no bulges: do simple polygon
				double lX = 0, hX = 0;
				double lY = 0, hY = 0;
				for(int i=0; i<count; i++)
				{
					PolyPoint pp = polyPoints.get(i);
					if (i == 0)
					{
						lX = hX = pp.x;
						lY = hY = pp.y;
					} else
					{
						if (pp.x < lX) lX = pp.x;
						if (pp.x > hX) hX = pp.x;
						if (pp.y < lY) lY = pp.y;
						if (pp.y > hY) hY = pp.y;
					}
				}
				double cX = (lX + hX) / 2;
				double cY = (lY + hY) / 2;
				NodeProto np = Artwork.tech.closedPolygonNode;
				if (!closed)
				{
					if (lineType == 0) np = Artwork.tech.openedPolygonNode; else
						np = Artwork.tech.openedDashedPolygonNode;
				}
				NodeInst ni = NodeInst.makeInstance(np, new Point2D.Double(cX, cY), hX-lX, hY-lY, curCell);
				if (ni == null) return true;
				Point2D [] points = new Point2D[count];
				for(int i=0; i<count; i++)
				{
					PolyPoint pp = polyPoints.get(i);
					points[i] = new Point2D.Double(pp.x - cX, pp.y - cY);
				}
				ni.newVar(NodeInst.TRACE, points);
				ni.newVar(DXF_LAYER_KEY, layer.layerName);
			}
		}
		readPolyLines++;
		return false;
	}

	private boolean readSolidEntity()
		throws IOException
	{
		DXFLayer layer = null;
		double factor = 1.0;
		double x1 = 0, y1 = 0, z1 = 0;
		double x2 = 0, y2 = 0, z2 = 0;
		double x3 = 0, y3 = 0, z3 = 0;
		double x4 = 0, y4 = 0, z4 = 0;
		for(;;)
		{
			if (getNextPair()) return true;
			switch (groupID)
			{
				case 8:  layer = getLayer(text);     break;
				case 10: x1 = scaleString(text);     break;
				case 20: y1 = scaleString(text);     break;
				case 30: z1 = scaleString(text);     break;
				case 11: x2 = scaleString(text);     break;
				case 21: y2 = scaleString(text);     break;
				case 31: z2 = scaleString(text);     break;
				case 12: x3 = scaleString(text);     break;
				case 22: y3 = scaleString(text);     break;
				case 32: z3 = scaleString(text);     break;
				case 13: x4 = scaleString(text);     break;
				case 23: y4 = scaleString(text);     break;
				case 33: z4 = scaleString(text);     break;
				case 230:
					factor = TextUtils.atof(text);
					break;
			}
			if (groupID == 0)
			{
				pushPair(groupID, text);
				break;
			}
		}
		x1 = x1 * factor;
		x2 = x2 * factor;
		x3 = x3 * factor;
		x4 = x4 * factor;
		if (!isAcceptableLayer(layer)) return false;
		double lX = Math.min(Math.min(x1, x2), Math.min(x3, x4));
		double hX = Math.max(Math.max(x1, x2), Math.max(x3, x4));
		double lY = Math.min(Math.min(y1, y2), Math.min(y3, y4));
		double hY = Math.max(Math.max(y1, y2), Math.max(y3, y4));
		double cX = (lX + hX) / 2;
		double cY = (lY + hY) / 2;
		NodeInst ni = NodeInst.makeInstance(Artwork.tech.filledPolygonNode, new Point2D.Double(cX, cY), hX-lX, hY-lY, curCell);
		if (ni == null) return true;
		Point2D [] points = new Point2D[4];
		points[0] = new Point2D.Double(x1 - cX, y1 - cY);
		points[1] = new Point2D.Double(x2 - cX, y2 - cY);
		points[2] = new Point2D.Double(x3 - cX, y3 - cY);
		points[3] = new Point2D.Double(x4 - cX, y4 - cY);
		ni.newVar(NodeInst.TRACE, points);
		ni.newVar(DXF_LAYER_KEY, layer.layerName);
		readSolids++;
		return false;
	}

	private boolean readTextEntity()
		throws IOException
	{
		DXFLayer layer = null;
		String msg = null;
		double x = 0, y = 0;
		double height = 0, xAlign = 0;
		boolean gotXA = false;
		for(;;)
		{
			if (getNextPair()) return true;
			switch (groupID)
			{
				case 8:  layer = getLayer(text);                       break;
				case 10: x = scaleString(text);                        break;
				case 20: y = scaleString(text);                        break;
				case 40: height = scaleString(text);                   break;
				case 11: xAlign = scaleString(text);   gotXA = true;   break;
				case 1:  msg = text;                                   break;
			}
			if (groupID == 0)
			{
				pushPair(groupID, text);
				break;
			}
		}
		double lX = x, hX = x;
		double lY = y, hY = y;
		if (gotXA)
		{
			lX = Math.min(x, xAlign);
			hX = lX + Math.abs(xAlign-x) * 2;
			lY = y;
			hY = y + height;
		} else
		{
			if (msg != null)
			{
				double h = msg.length();
//				UserInterface ui = Main.getUserInterface();
//				EditWindow_ wnd = ui.getCurrentEditWindow_();
//				if (wnd != null) h = wnd.getTextScreenSize(height);
				lX = x;	hX = x + height * h;
				lY = y;	hY = y + height;
			}
		}
		if (!isAcceptableLayer(layer)) return false;
		if (msg != null)
		{
			NodeInst ni = NodeInst.makeInstance(Generic.tech.invisiblePinNode, new Point2D.Double((lX+hX)/2, (lY+hY)/2), hX-lX, hY-lY, curCell);
			if (ni == null) return true;
            TextDescriptor td = TextDescriptor.getNodeTextDescriptor().withPos(TextDescriptor.Position.BOXED).withAbsSize(TextDescriptor.Size.TXTMAXPOINTS);
            ni.newVar(Artwork.ART_MESSAGE, msg, td);
//			Variable var = ni.newDisplayVar(Artwork.ART_MESSAGE, msg);
//			if (var != null)
//			{
////				var.setDisplay(true);
//				var.setPos(TextDescriptor.Position.BOXED);
//				var.setAbsSize(TextDescriptor.Size.TXTMAXPOINTS);
//			}
			ni.newVar(DXF_LAYER_KEY, layer.layerName);
			readTexts++;
		}
		return false;
	}

	private boolean read3DFaceEntity()
		throws IOException
	{
		DXFLayer layer = null;
		double x1 = 0, y1 = 0, z1 = 0;
		double x2 = 0, y2 = 0, z2 = 0;
		double x3 = 0, y3 = 0, z3 = 0;
		double x4 = 0, y4 = 0, z4 = 0;
		for(;;)
		{
			if (getNextPair()) return true;
			switch (groupID)
			{
				case 8:  layer = getLayer(text);   break;
				case 10: x1 = scaleString(text);   break;
				case 20: y1 = scaleString(text);   break;
				case 30: z1 = scaleString(text);   break;

				case 11: x2 = scaleString(text);   break;
				case 21: y2 = scaleString(text);   break;
				case 31: z2 = scaleString(text);   break;

				case 12: x3 = scaleString(text);   break;
				case 22: y3 = scaleString(text);   break;
				case 32: z3 = scaleString(text);   break;

				case 13: x4 = scaleString(text);   break;
				case 23: y4 = scaleString(text);   break;
				case 33: z4 = scaleString(text);   break;
			}
			if (groupID == 0)
			{
				pushPair(groupID, text);
				break;
			}
		}
		if (!isAcceptableLayer(layer)) return false;
		double lX = Math.min(Math.min(x1, x2), Math.min(x3, x4));
		double hX = Math.max(Math.max(x1, x2), Math.max(x3, x4));
		double lY = Math.min(Math.min(y1, y2), Math.min(y3, y4));
		double hY = Math.max(Math.max(y1, y2), Math.max(y3, y4));
		double cX = (lX + hX) / 2;
		double cY = (lY + hY) / 2;
		NodeInst ni = NodeInst.makeInstance(Artwork.tech.closedPolygonNode, new Point2D.Double(cX, cY), hX-lX, hY-lY, curCell);
		if (ni == null) return true;
		Point2D [] points = new Point2D[4];
		points[0] = new Point2D.Double(x1 - cX, y1 - cY);
		points[1] = new Point2D.Double(x2 - cX, y2 - cY);
		points[2] = new Point2D.Double(x3 - cX, y3 - cY);
		points[3] = new Point2D.Double(x4 - cX, y4 - cY);
		ni.newVar(NodeInst.TRACE, points);
		ni.newVar(DXF_LAYER_KEY, layer.layerName);
		read3DFaces++;
		return false;
	}

	private void ignoreEntity()
		throws IOException
	{
		for(;;)
		{
			if (getNextPair()) break;
			if (groupID == 0) break;
		}
		pushPair(groupID, text);
	}

	/****************************************** READING SUPPORT ******************************************/

	private boolean isAcceptableLayer(DXFLayer layer)
	{
		if (layer == null) return false;
		if (IOTool.isDXFInputReadsAllLayers()) return true;
		if (validLayerNames.contains(layer.layerName)) return true;

		// add this to the list of layer names that were ignored
		ignoredLayerNames.add(layer.layerName);
		return false;
	}

	private boolean extractInsert(Cell onp, double x, double y, double xSca, double ySca, int rot, Cell np)
	{
		// rotate "rot*10" about point [(onp->lowx+onp->highx)/2+x, (onp->lowy+onp->highy)/2+y]
        Orientation orient = Orientation.fromAngle(rot*10);
        AffineTransform trans = orient.pureRotate();
//		AffineTransform trans = NodeInst.pureRotate(rot*10, false, false);
		double m00 = trans.getScaleX();
		double m01 = trans.getShearX();
		double m11 = trans.getScaleY();
		double m10 = trans.getShearY();
		Rectangle2D bounds = onp.getBounds();
		double m02 = bounds.getCenterX() + x;
		double m12 = bounds.getCenterY() + y;
		trans.setTransform(m00, m10, m01, m11, m02, m12);
		Point2D pt = new Point2D.Double(-m02, -m12);
		trans.transform(pt, pt);
		trans.setTransform(m00, m10, m01, m11, pt.getX(), pt.getY());

		for(Iterator<NodeInst> it = onp.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (ni.isCellInstance())
			{
				System.out.println("Cannot insert block '" + onp + "'...it has inserts in it");
				return true;
			}
			if (ni.getProto() == Generic.tech.cellCenterNode) continue;
			double sX = ni.getXSize() * xSca;
			double sY = ni.getYSize() * ySca;
			double cX = x + ni.getAnchorCenterX() * xSca;
			double cY = y + ni.getAnchorCenterY() * ySca;
			Point2D tPt = new Point2D.Double(cX, cY);
			trans.transform(tPt, tPt);
			NodeInst nNi = NodeInst.makeInstance(ni.getProto(), tPt, sX, sY, np, orient.concatenate(ni.getOrient()), null, 0);
//			if (ni.isXMirrored()) sX = -sX;
//			if (ni.isYMirrored()) sY = -sY;
//			NodeInst nNi = NodeInst.makeInstance(ni.getProto(), tPt, sX, sY, np, (ni.getAngle()+rot*10)%3600, null, 0);
			if (nNi == null) return true;
			if (ni.getProto() == Artwork.tech.closedPolygonNode || ni.getProto() == Artwork.tech.filledPolygonNode ||
				ni.getProto() == Artwork.tech.openedPolygonNode || ni.getProto() == Artwork.tech.openedDashedPolygonNode)
			{
				// copy trace information
				Point2D [] oldTrace = ni.getTrace();
				if (oldTrace != null)
				{
					int len = oldTrace.length;
					Point2D [] newTrace = new Point2D[len];
					for(int i=0; i<len; i++)
						newTrace[i] = new Point2D.Double(oldTrace[i].getX() * xSca, oldTrace[i].getY() * ySca);
					nNi.newVar(NodeInst.TRACE, newTrace);
				}
			} else if (ni.getProto() == Generic.tech.invisiblePinNode)
			{
				// copy text information
				Variable var = ni.getVar(Artwork.ART_MESSAGE);
				if (var != null)
				{
					Variable newVar = nNi.newVar(Artwork.ART_MESSAGE, var.getObject(), var.getTextDescriptor());
				}
			} else if (ni.getProto() == Artwork.tech.circleNode || ni.getProto() == Artwork.tech.thickCircleNode)
			{
				// copy arc information
				double [] curvature = ni.getArcDegrees();
				nNi.setArcDegrees(curvature[0], curvature[1]);
			}

			// copy other information
			Variable var = ni.getVar(DXF_LAYER_KEY);
			if (var != null) nNi.newVar(DXF_LAYER_KEY, var.getObject());
			var = ni.getVar(Artwork.ART_COLOR);
			if (var != null) nNi.newVar(Artwork.ART_COLOR, var.getObject());
		}
		return false;
	}

	private Cell getScaledCell(Cell onp, double xSca, double ySca)
	{
		String fViewName = "scaled" + xSca + "x" + ySca;
		String sViewName = "s" + xSca + "x" + ySca;
		View view = View.findView(fViewName);
		if (view == null)
		{
			view = View.newInstance(fViewName, sViewName);
			if (view == null) return null;
		}

		// find the view of this cell
		Cell rightView = onp.otherView(view);
		if (rightView != null) return rightView;

		// not found: create it
		String cellName = onp.getName() + "{" + sViewName + "}";
		Cell np = Cell.makeInstance(onp.getLibrary(), cellName);
		if (np == null) return null;

		for(Iterator<NodeInst> it = onp.getNodes(); it.hasNext(); )
		{
			NodeInst ni = it.next();
			if (ni.isCellInstance())
			{
				System.out.println("Cannot insert block '" + onp + "'...it has inserts in it");
				return null;
			}
			NodeInst nNi = NodeInst.makeInstance(ni.getProto(), ni.getAnchorCenter(),
				ni.getXSize()*xSca, ni.getYSize()*ySca, np, ni.getOrient(), null, 0);
//			NodeInst nNi = NodeInst.makeInstance(ni.getProto(), ni.getAnchorCenter(),
//				ni.getXSizeWithMirror()*xSca, ni.getYSizeWithMirror()*ySca, np, ni.getAngle(), null, 0);
			if (nNi == null) return null;
			if (ni.getProto() == Artwork.tech.closedPolygonNode || ni.getProto() == Artwork.tech.filledPolygonNode ||
				ni.getProto() == Artwork.tech.openedPolygonNode || ni.getProto() == Artwork.tech.openedDashedPolygonNode)
			{
				// copy trace information
				Point2D [] oldTrace = ni.getTrace();
				if (oldTrace != null)
				{
					int len = oldTrace.length;
					Point2D [] newTrace = new Point2D[len];
					for(int i=0; i<len; i++)
						newTrace[i] = new Point2D.Double(oldTrace[i].getX() * xSca, oldTrace[i].getY() * ySca);
					nNi.newVar(NodeInst.TRACE, newTrace);
				}
			} else if (ni.getProto() == Generic.tech.invisiblePinNode)
			{
				// copy text information
				Variable var = ni.getVar(Artwork.ART_MESSAGE);
				if (var != null)
				{
					Variable newVar = nNi.newVar(Artwork.ART_MESSAGE, var.getObject(), var.getTextDescriptor());
				}
			} else if (ni.getProto() == Artwork.tech.circleNode || ni.getProto() == Artwork.tech.thickCircleNode)
			{
				// copy arc information
				double [] curvature = ni.getArcDegrees();
				nNi.setArcDegrees(curvature[0], curvature[1]);
			}

			// copy layer information
			Variable var = ni.getVar(DXF_LAYER_KEY);
			if (var != null) nNi.newVar(DXF_LAYER_KEY, var.getObject());
		}
		return np;
	}

	private DXFLayer getLayer(String name)
	{
		for(DXFLayer layer = firstLayer; layer != null; layer = layer.next)
			if (name.equals(layer.layerName)) return layer;

		// create a new one
		DXFLayer layer = new DXFLayer();
		layer.layerName = name;
		layer.layerColor = -1;
		layer.layerRed = 1.0;
		layer.layerGreen = 1.0;
		layer.layerBlue = 1.0;
		layer.next = firstLayer;
		firstLayer = layer;
		return layer;
	}

	private void pushPair(int groupID, String text)
	{
		lastGroupID = groupID;
		lastText = text;
		lastPairValid = true;
	}

	/**
	 * Method to examine the layer names on the artwork technology and obtain
	 * a list of acceptable layer names and numbers.
	 */
	private void getAcceptableLayers()
	{
		validLayerNames = new HashSet<String>();
		for(Iterator<Layer> it = Artwork.tech.getLayers(); it.hasNext(); )
		{
			Layer lay = it.next();
			String layNames = lay.getDXFLayer();
			if (layNames == null) continue;
			while (layNames.length() > 0)
			{
				int commaPos = layNames.indexOf(',');
				if (commaPos < 0) commaPos = layNames.length();
				String oneName = layNames.substring(0, commaPos);
				validLayerNames.add(oneName);
				layNames = layNames.substring(oneName.length());
				if (layNames.startsWith(",")) layNames = layNames.substring(1);
			}
		}
	}

	/**
	 * Method to convert a block name "name" into a valid Electric cell name (converts
	 * bad characters).
	 */
	private String makeBlockName(String name)
	{
		StringBuffer infstr = new StringBuffer();
		for(int i=0; i<name.length(); i++)
		{
			char chr = name.charAt(i);
			if (chr == '$' || chr == '{' || chr == '}' || chr == ':') chr = '_';
			infstr.append(chr);
		}
		return infstr.toString();
	}

	/**
	 * Method to set the conversion units between DXF files and real distance.
	 * The value is stored in the global "dispUnit".
	 */
	private void setCurUnits()
	{
		int units = IOTool.getDXFScale();
		switch (units)
		{
			case -3: dispUnit = TextUtils.UnitScale.GIGA;   break;
			case -2: dispUnit = TextUtils.UnitScale.MEGA;   break;
			case -1: dispUnit = TextUtils.UnitScale.KILO;   break;
			case  0: dispUnit = TextUtils.UnitScale.NONE;   break;
			case  1: dispUnit = TextUtils.UnitScale.MILLI;  break;
			case  2: dispUnit = TextUtils.UnitScale.MICRO;  break;
			case  3: dispUnit = TextUtils.UnitScale.NANO;   break;
			case  4: dispUnit = TextUtils.UnitScale.PICO;   break;
			case  5: dispUnit = TextUtils.UnitScale.FEMTO;  break;
		}
	}
}
