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
package com.sun.electric.tool.io.output;

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
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
	private String defaultDXFLayerName;
	private static String [] ignorefromheader = {"$DWGCODEPAGE", "$HANDSEED", "$SAVEIMAGES"};

	private DXFPreferences localPrefs;

	public static class DXFPreferences extends OutputPreferences
    {
        // DXF Settings
		int dxfScale = IOTool.getDXFScale();
        public Technology tech;

        public DXFPreferences(boolean factory) {
            super(factory);
            tech = Technology.getCurrent();
        }

        @Override
        public Output doOutput(Cell cell, VarContext context, String filePath)
        {
    		DXF out = new DXF(this);
    		if (out.openTextOutputStream(filePath)) return out.finishWrite();

    		out.writeDXF(cell);

    		if (out.closeTextOutputStream()) return out.finishWrite();
    		System.out.println(filePath + " written");
            return out.finishWrite();
        }
    }

	/**
	 * Creates a new instance of the DXF netlister.
	 */
	DXF(DXFPreferences dp) { localPrefs = dp; }

	private void writeDXF(Cell cell)
	{
		// set the scale
		dxfDispUnit = TextUtils.UnitScale.findFromIndex(localPrefs.dxfScale);

		// write the header
		Variable varheadertext = cell.getLibrary().getVar(DXF_HEADER_TEXT_KEY);
		Variable varheaderid = cell.getLibrary().getVar(DXF_HEADER_ID_KEY);
		Layer defLay = Artwork.tech().findLayer("Graphics");
		defaultDXFLayerName = defLay.getDXFLayer();
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
			NodeInst ni = it.next();
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
				NodeInst ni = it.next();
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
			NodeInst ni = it.next();
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
				double xC = TextUtils.convertDistance(ni.getAnchorCenterX() - cellBounds.getCenterX(), localPrefs.tech, dxfDispUnit);
				double yC = TextUtils.convertDistance(ni.getAnchorCenterY() - cellBounds.getCenterY(), localPrefs.tech, dxfDispUnit);
				printWriter.print(" 10\n" + TextUtils.formatDouble(xC) + "\n");
				printWriter.print(" 20\n" + TextUtils.formatDouble(yC) + "\n");
				printWriter.print(" 30\n0\n");
				double rot = ni.getAngle() / 10.0;
				printWriter.print(" 50\n" + TextUtils.formatDouble(rot) + "\n");
				continue;
			}

			// determine layer name for this node
			String layerName = defaultDXFLayerName;
			Variable var = ni.getVar(DXF_LAYER_KEY);
			if (var != null) layerName = var.getPureValue(-1); else
			{
				// examine technology for proper layer name
				if (!ni.isCellInstance())
				{
					PrimitiveNode pNp = (PrimitiveNode)ni.getProto();
					Technology.NodeLayer [] nodeLayers = pNp.getNodeLayers();
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
			if (np == Artwork.tech().circleNode || np == Artwork.tech().thickCircleNode)
			{
				double [] angles = ni.getArcDegrees();
				double startOffset = angles[0];
				double endAngle = angles[1];
				if (startOffset != 0.0 || endAngle != 0.0) printWriter.print("  0\nARC\n"); else
					printWriter.print("  0\nCIRCLE\n");
				printWriter.print("  8\n" + layerName + "\n");
				printWriter.print("  5\n" + getThreeDigitHex(dxfEntityHandle++) + "\n");
				xC = TextUtils.convertDistance(xC, localPrefs.tech, dxfDispUnit);
                yC = TextUtils.convertDistance(yC, localPrefs.tech, dxfDispUnit);
				double zC = TextUtils.convertDistance(ni.getYSize() / 2, localPrefs.tech, dxfDispUnit);
				printWriter.print(" 10\n" + TextUtils.formatDouble(xC) + "\n");
				printWriter.print(" 20\n" + TextUtils.formatDouble(yC) + "\n");
				printWriter.print(" 30\n0\n");
				printWriter.print(" 40\n" + TextUtils.formatDouble(zC) + "\n");
				if (startOffset != 0.0 || endAngle != 0.0)
				{
					startOffset = startOffset * 180.0 / Math.PI;
					endAngle = endAngle * 180.0 / Math.PI;
					double startAngle = ni.getAngle() / 10.0 + startOffset;
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
			if (np == Artwork.tech().openedPolygonNode || np == Artwork.tech().openedDashedPolygonNode ||
				np == Artwork.tech().closedPolygonNode)
			{
				AffineTransform trans = ni.rotateOut();
				Point2D [] points = ni.getTrace();
				int len = points.length;
				if (len == 2 && np != Artwork.tech().closedPolygonNode)
				{
					// line
					printWriter.print("  0\nLINE\n");
					printWriter.print("  8\n" + layerName + "\n");
					printWriter.print("  5\n" + getThreeDigitHex(dxfEntityHandle++) + "\n");
					Point2D pt = new Point2D.Double(points[0].getX() + xC, points[0].getY() + yC);
					trans.transform(pt, pt);
					double x = TextUtils.convertDistance(pt.getX(), localPrefs.tech, dxfDispUnit);
					double y = TextUtils.convertDistance(pt.getY(), localPrefs.tech, dxfDispUnit);
					printWriter.print(" 10\n" + TextUtils.formatDouble(x) + "\n");
					printWriter.print(" 20\n" + TextUtils.formatDouble(y) + "\n");
					printWriter.print(" 30\n0\n");
					pt = new Point2D.Double(points[1].getX() + xC, points[1].getY() + yC);
					trans.transform(pt, pt);
					x = TextUtils.convertDistance(pt.getX(), localPrefs.tech, dxfDispUnit);
					y = TextUtils.convertDistance(pt.getY(), localPrefs.tech, dxfDispUnit);
					printWriter.print(" 11\n" + TextUtils.formatDouble(x) + "\n");
					printWriter.print(" 21\n" + TextUtils.formatDouble(y) + "\n");
					printWriter.print(" 31\n0\n");
				} else
				{
					// should write a polyline here
					for(int i=0; i<len-1; i++)
					{
						// line
						if (points[i] == null || points[i+1] == null) continue;
						printWriter.print("  0\nLINE\n");
						printWriter.print("  8\n" + layerName + "\n");
						printWriter.print("  5\n" + getThreeDigitHex(dxfEntityHandle++) + "\n");
						Point2D pt = new Point2D.Double(points[i].getX() + xC, points[i].getY() + yC);
						trans.transform(pt, pt);
						double x = TextUtils.convertDistance(pt.getX(), localPrefs.tech, dxfDispUnit);
						double y = TextUtils.convertDistance(pt.getY(), localPrefs.tech, dxfDispUnit);
						printWriter.print(" 10\n" + TextUtils.formatDouble(x) + "\n");
						printWriter.print(" 20\n" + TextUtils.formatDouble(y) + "\n");
						printWriter.print(" 30\n0\n");
						pt = new Point2D.Double(points[i+1].getX() + xC, points[i+1].getY() + yC);
						trans.transform(pt, pt);
						x = TextUtils.convertDistance(pt.getX(), localPrefs.tech, dxfDispUnit);
						y = TextUtils.convertDistance(pt.getY(), localPrefs.tech, dxfDispUnit);
						printWriter.print(" 11\n" + TextUtils.formatDouble(x) + "\n");
						printWriter.print(" 21\n" + TextUtils.formatDouble(y) + "\n");
						printWriter.print(" 31\n0\n");
					}
					if (np == Artwork.tech().closedPolygonNode)
					{
						printWriter.print("  0\nLINE\n");
						printWriter.print("  8\n" + layerName + "\n");
						printWriter.print("  5\n" + getThreeDigitHex(dxfEntityHandle++) + "\n");
						Point2D pt = new Point2D.Double(points[len-1].getX() + xC, points[len-1].getY() + yC);
						trans.transform(pt, pt);
						double x = TextUtils.convertDistance(pt.getX(), localPrefs.tech, dxfDispUnit);
						double y = TextUtils.convertDistance(pt.getY(), localPrefs.tech, dxfDispUnit);
						printWriter.print(" 10\n" + TextUtils.formatDouble(x) + "\n");
						printWriter.print(" 20\n" + TextUtils.formatDouble(y) + "\n");
						printWriter.print(" 30\n0\n");
						pt = new Point2D.Double(points[0].getX() + xC, points[0].getY() + yC);
						trans.transform(pt, pt);
						x = TextUtils.convertDistance(pt.getX(), localPrefs.tech, dxfDispUnit);
						y = TextUtils.convertDistance(pt.getY(), localPrefs.tech, dxfDispUnit);
						printWriter.print(" 11\n" + TextUtils.formatDouble(x) + "\n");
						printWriter.print(" 21\n" + TextUtils.formatDouble(y) + "\n");
						printWriter.print(" 31\n0\n");
					}
				}
				continue;
			}

			// write all other nodes
			Poly [] polys = ni.getProto().getTechnology().getShapeOfNode(ni);
			AffineTransform trans = ni.rotateOut();
			for(int i=0; i<polys.length; i++)
			{
				Poly poly = polys[i];
				poly.transform(trans);
				if (poly.getStyle() == Poly.Type.FILLED)
				{
					printWriter.print("  0\nSOLID\n");
					printWriter.print("  8\n" + layerName + "\n");
					Point2D [] points = poly.getPoints();
					for(int j=0; j<points.length; j++)
					{
						printWriter.print(" 1" + j + "\n" + TextUtils.formatDouble(points[j].getX()) + "\n");
						printWriter.print(" 2" + j + "\n" + TextUtils.formatDouble(points[j].getY()) + "\n");
						printWriter.print(" 3" + j + "\n0\n");
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
					Cell oCell = it.next();
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
