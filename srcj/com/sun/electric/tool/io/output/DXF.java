/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: DXF.java
 * Input/output tool: DXF output
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
package com.sun.electric.tool.io.output;

import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.variable.ElectricObject;
import com.sun.electric.database.variable.VarContext;
import com.sun.electric.database.variable.Variable;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.tool.io.IOTool;

import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * This is the netlister for DXF.
 */
public class DXF extends Output
{
	/** key of Variable holding DXF layer name. */			public static final Variable.Key DXF_LAYER_KEY = Variable.newKey("IO_dxf_layer");
	/** key of Variable holding DXF header text. */			public static final Variable.Key DXF_HEADER_TEXT_KEY = Variable.newKey("IO_dxf_header_text");
	/** key of Variable holding DXF header information. */	public static final Variable.Key DXF_HEADER_ID_KEY = Variable.newKey("IO_dxf_header_ID");
	private int dxfEntityHandle;
	private Set<Cell> cellsSeen;
	private TextUtils.UnitScale dxfDispUnit;
	private static String [] ignorefromheader = {"$DWGCODEPAGE", "$HANDSEED", "$SAVEIMAGES"};

	/**
	 * The main entry point for DXF deck writing.
     * @param cell the top-level cell to write.
	 * @param filePath the disk file to create.
	 */
	public static void writeDXFFile(Cell cell, String filePath)
	{
		DXF out = new DXF();
		if (out.openTextOutputStream(filePath)) return;

		out.writeDXF(cell);

		if (out.closeTextOutputStream()) return;
		System.out.println(filePath + " written");
	}

	/**
	 * Creates a new instance of the DXF netlister.
	 */
	DXF()
	{
	}

	private void writeDXF(Cell cell)
	{
		// set the scale
		dxfDispUnit = TextUtils.UnitScale.findFromIndex(IOTool.getDXFScale());

		// write the header
		Variable varheadertext = cell.getLibrary().getVar(DXF_HEADER_TEXT_KEY);
		Variable varheaderid = cell.getLibrary().getVar(DXF_HEADER_ID_KEY);
		if (varheadertext != null && varheaderid != null)
		{
			printWriter.print("  0\nSECTION\n");
			printWriter.print("  2\nHEADER\n");
			int len = Math.min(varheadertext.getLength(), varheaderid.getLength());
			for(int i=0; i<len; i++)
			{
				// remove entries that confuse the issues
				String pt = (String)varheadertext.getObject(i);
				int code = ((Integer)varheaderid.getObject(i)).intValue();
//				String pt = ((String [])varheadertext.getObject())[i];
//				int code = ((Integer [])varheaderid.getObject())[i].intValue();
				if (code == 9 && i <= len-2)
				{
					boolean found = false;
					for(int j=0; j<ignorefromheader.length; j++)
					{
						if (pt.equals(ignorefromheader[j]) || pt.substring(1).equals(ignorefromheader[j]))
						{
							found = true;
							break;
						}
					}
					if (found)
					{
						i++;
						continue;
					}
				}

				// make sure Autocad version is correct
				if (pt.equals("$ACADVER") && i <= len-2)
				{
					printWriter.print(getThreeDigit(code) + "\n" + pt + "\n");
					printWriter.print("  1\nAC1009\n");
					i++;
					continue;
				}

				printWriter.print(getThreeDigit(code) + "\n" + pt + "\n");
			}
			printWriter.print("  0\nENDSEC\n");
		}

		// write any subcells
		dxfEntityHandle = 0x100;
		printWriter.print("  0\nSECTION\n");
		printWriter.print("  2\nBLOCKS\n");

		cellsSeen = new HashSet<Cell>();
		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			if (ni.isCellInstance())
			{
				NodeProto np = ni.getProto();
				if (!cellsSeen.contains(np)) writeDXFCell((Cell)np, true);
			}
		}

		printWriter.print("  0\nENDSEC\n");
		printWriter.print("  0\nSECTION\n");
		printWriter.print("  2\nENTITIES\n");
		writeDXFCell(cell, false);
		printWriter.print("  0\nENDSEC\n");
		printWriter.print("  0\nEOF\n");
	}

	/**
	 * Method to write the contents of cell "np".  If "subCells" is nonzero, do a recursive
	 * descent through the subcells in this cell, writing out "block" definitions.
	 */
	private void writeDXFCell(Cell cell, boolean subCells)
	{
		if (subCells)
		{
			cellsSeen.add(cell);
			for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
			{
				NodeInst ni = (NodeInst)it.next();
				NodeProto np = ni.getProto();
				if (ni.isCellInstance() && !cellsSeen.contains(np))
					writeDXFCell((Cell)np, true);
			}
			printWriter.print("  0\nBLOCK\n");
			printWriter.print("  2\n" + getDXFCellName(cell) + "\n");
			printWriter.print(" 10\n0\n");
			printWriter.print(" 20\n0\n");
			printWriter.print(" 30\n0\n");
			printWriter.print(" 70\n0\n");
		}

		for(Iterator<NodeInst> it = cell.getNodes(); it.hasNext(); )
		{
			NodeInst ni = (NodeInst)it.next();
			NodeProto np = ni.getProto();

			// handle instances
			if (ni.isCellInstance())
			{
				Cell subCell = (Cell)np;
				printWriter.print("  0\nINSERT\n");
				printWriter.print("  8\nUNKNOWN\n");
				printWriter.print("  5\n" + getThreeDigitHex(dxfEntityHandle++) + "\n");
				printWriter.print("  2\n" + getDXFCellName(subCell) + "\n");
				Rectangle2D cellBounds = subCell.getBounds();
				double xC = TextUtils.convertDistance(ni.getAnchorCenterX() - cellBounds.getCenterX(), Technology.getCurrent(), dxfDispUnit);
				double yC = TextUtils.convertDistance(ni.getAnchorCenterY() - cellBounds.getCenterY(), Technology.getCurrent(), dxfDispUnit);
				printWriter.print(" 10\n" + TextUtils.formatDouble(xC) + "\n");
				printWriter.print(" 20\n" + TextUtils.formatDouble(yC) + "\n");
				printWriter.print(" 30\n0\n");
				double rot = ni.getAngle() / 10.0;
				printWriter.print(" 50\n" + TextUtils.formatDouble(rot) + "\n");
				continue;
			}

			// determine layer name for this node
			String layerName = "UNKNOWN";
			Variable var = ni.getVar(DXF_LAYER_KEY);
			if (var != null) layerName = var.getPureValue(-1); else
			{
				// examine technology for proper layer name
				if (!ni.isCellInstance())
				{
					PrimitiveNode pNp = (PrimitiveNode)ni.getProto();
					Technology.NodeLayer [] nodeLayers = pNp.getLayers();
					for(int i=0; i<nodeLayers.length; i++)
					{
						Technology.NodeLayer nl = nodeLayers[i];
						Layer layer = nl.getLayer();
						String dxfLayerName = layer.getDXFLayer();
						if (dxfLayerName != null && dxfLayerName.length() > 0)
						{
							layerName = dxfLayerName;
							break;
						}
					}
				}
			}

			// get the node center
			double xC = ni.getAnchorCenterX();
			double yC = ni.getAnchorCenterY();

			// handle circles and arcs
			if (np == Artwork.tech.circleNode || np == Artwork.tech.thickCircleNode)
			{
				double [] angles = ni.getArcDegrees();
				double startOffset = angles[0];
				double endAngle = angles[1];
				if (startOffset != 0.0 || endAngle != 0.0) printWriter.print("  0\nARC\n"); else
					printWriter.print("  0\nCIRCLE\n");
				printWriter.print("  8\n" + layerName + "\n");
				printWriter.print("  5\n" + getThreeDigitHex(dxfEntityHandle++) + "\n");
				xC = TextUtils.convertDistance(xC, Technology.getCurrent(), dxfDispUnit);
				yC = TextUtils.convertDistance(yC, Technology.getCurrent(), dxfDispUnit);
				double zC = TextUtils.convertDistance(ni.getYSize() / 2, Technology.getCurrent(), dxfDispUnit);
				printWriter.print(" 10\n" + TextUtils.formatDouble(xC) + "\n");
				printWriter.print(" 20\n" + TextUtils.formatDouble(yC) + "\n");
				printWriter.print(" 30\n0\n");
				printWriter.print(" 40\n" + TextUtils.formatDouble(zC) + "\n");
				if (startOffset != 0.0 || endAngle != 0.0)
				{
					startOffset = startOffset * 180.0 / Math.PI;
					endAngle = endAngle * 180.0 / Math.PI;
					double startAngle = (double)ni.getAngle() / 10.0 + startOffset;
					if (ni.isXMirrored() != ni.isYMirrored())
					{
						startAngle = 270.0 - startAngle - endAngle;
						if (startAngle < 0.0) startAngle += 360.0;
					}
					endAngle += startAngle;
					if (endAngle >= 360.0) endAngle -= 360.0;
					printWriter.print(" 50\n" + TextUtils.formatDouble(startAngle) + "\n");
					printWriter.print(" 51\n" + TextUtils.formatDouble(endAngle) + "\n");
				}
				continue;
			}

			// handle polygons
			if (np == Artwork.tech.openedPolygonNode || np == Artwork.tech.openedDashedPolygonNode ||
				np == Artwork.tech.closedPolygonNode)
			{
				AffineTransform trans = ni.rotateOut();
				Point2D [] points = ni.getTrace();
				int len = points.length;
				if (len == 2 && np != Artwork.tech.closedPolygonNode)
				{
					// line
					printWriter.print("  0\nLINE\n");
					printWriter.print("  8\n" + layerName + "\n");
					printWriter.print("  5\n" + getThreeDigitHex(dxfEntityHandle++) + "\n");
					Point2D pt = new Point2D.Double(points[0].getX() + xC, points[0].getY() + yC);
					trans.transform(pt, pt);
					double x = TextUtils.convertDistance(pt.getX(), Technology.getCurrent(), dxfDispUnit);
					double y = TextUtils.convertDistance(pt.getY(), Technology.getCurrent(), dxfDispUnit);
					printWriter.print(" 10\n" + TextUtils.formatDouble(x) + "\n");
					printWriter.print(" 20\n" + TextUtils.formatDouble(y) + "\n");
					printWriter.print(" 30\n0\n");
					pt = new Point2D.Double(points[1].getX() + xC, points[1].getY() + yC);
					trans.transform(pt, pt);
					x = TextUtils.convertDistance(pt.getX(), Technology.getCurrent(), dxfDispUnit);
					y = TextUtils.convertDistance(pt.getY(), Technology.getCurrent(), dxfDispUnit);
					printWriter.print(" 11\n" + TextUtils.formatDouble(x) + "\n");
					printWriter.print(" 21\n" + TextUtils.formatDouble(y) + "\n");
					printWriter.print(" 31\n0\n");
				} else
				{
					// should write a polyline here
					for(int i=0; i<len-1; i++)
					{
						// line
						printWriter.print("  0\nLINE\n");
						printWriter.print("  8\n" + layerName + "\n");
						printWriter.print("  5\n" + getThreeDigitHex(dxfEntityHandle++) + "\n");
						Point2D pt = new Point2D.Double(points[i].getX() + xC, points[i].getY() + yC);
						trans.transform(pt, pt);
						double x = TextUtils.convertDistance(pt.getX(), Technology.getCurrent(), dxfDispUnit);
						double y = TextUtils.convertDistance(pt.getY(), Technology.getCurrent(), dxfDispUnit);
						printWriter.print(" 10\n" + TextUtils.formatDouble(x) + "\n");
						printWriter.print(" 20\n" + TextUtils.formatDouble(y) + "\n");
						printWriter.print(" 30\n0\n");
						pt = new Point2D.Double(points[i+1].getX() + xC, points[i+1].getY() + yC);
						trans.transform(pt, pt);
						x = TextUtils.convertDistance(pt.getX(), Technology.getCurrent(), dxfDispUnit);
						y = TextUtils.convertDistance(pt.getY(), Technology.getCurrent(), dxfDispUnit);
						printWriter.print(" 11\n" + TextUtils.formatDouble(x) + "\n");
						printWriter.print(" 21\n" + TextUtils.formatDouble(y) + "\n");
						printWriter.print(" 31\n0\n");
					}
					if (np == Artwork.tech.closedPolygonNode)
					{
						printWriter.print("  0\nLINE\n");
						printWriter.print("  8\n" + layerName + "\n");
						printWriter.print("  5\n" + getThreeDigitHex(dxfEntityHandle++) + "\n");
						Point2D pt = new Point2D.Double(points[len-1].getX() + xC, points[len-1].getY() + yC);
						trans.transform(pt, pt);
						double x = TextUtils.convertDistance(pt.getX(), Technology.getCurrent(), dxfDispUnit);
						double y = TextUtils.convertDistance(pt.getY(), Technology.getCurrent(), dxfDispUnit);
						printWriter.print(" 10\n" + TextUtils.formatDouble(x) + "\n");
						printWriter.print(" 20\n" + TextUtils.formatDouble(y) + "\n");
						printWriter.print(" 30\n0\n");
						pt = new Point2D.Double(points[0].getX() + xC, points[0].getY() + yC);
						trans.transform(pt, pt);
						x = TextUtils.convertDistance(pt.getX(), Technology.getCurrent(), dxfDispUnit);
						y = TextUtils.convertDistance(pt.getY(), Technology.getCurrent(), dxfDispUnit);
						printWriter.print(" 11\n" + TextUtils.formatDouble(x) + "\n");
						printWriter.print(" 21\n" + TextUtils.formatDouble(y) + "\n");
						printWriter.print(" 31\n0\n");
					}
				}
			}
		}
		if (subCells)
			printWriter.print("  0\nENDBLK\n");
	}

	private String getDXFCellName(Cell cell)
	{
		if (cell.getName().equalsIgnoreCase(cell.getLibrary().getName()))
		{
			// use another name
			String buf = null;
			for(int i=1; i<1000; i++)
			{
				buf = cell.getName() + i;
				boolean found = false;
				for(Iterator<Cell> it = cell.getLibrary().getCells(); it.hasNext(); )
				{
					Cell oCell = (Cell)it.next();
					if (oCell.getName().equalsIgnoreCase(buf)) { found = true;   break; }
				}
				if (!found) break;
			}
			return buf;
		}

		// just return the cell name
		return cell.getName();
	}

	private String getThreeDigit(int value)
	{
		String result = Integer.toString(value);
		while (result.length() < 3) result = " " + result;
		return result;
	}

	private String getThreeDigitHex(int value)
	{
		String result = Integer.toHexString(value).toUpperCase();
		while (result.length() < 3) result = " " + result;
		return result;
	}
}
