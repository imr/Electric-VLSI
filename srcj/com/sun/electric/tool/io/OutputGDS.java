/* -*- tab-width: 4 -*-
 *
 * Electric(tm) VLSI Design System
 *
 * File: OutputGDS.java
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
package com.sun.electric.tool.io;

import com.sun.electric.database.geometry.Poly;
import com.sun.electric.database.hierarchy.Cell;
import com.sun.electric.database.hierarchy.Library;
import com.sun.electric.database.hierarchy.Nodable;
import com.sun.electric.database.hierarchy.HierarchyEnumerator;
import com.sun.electric.database.prototype.NodeProto;
import com.sun.electric.database.prototype.ArcProto;
import com.sun.electric.database.text.TextUtils;
import com.sun.electric.database.topology.NodeInst;
import com.sun.electric.database.topology.ArcInst;
import com.sun.electric.database.variable.FlagSet;
import com.sun.electric.technology.Technology;
import com.sun.electric.technology.PrimitiveNode;
import com.sun.electric.technology.Layer;
import com.sun.electric.technology.technologies.Artwork;
import com.sun.electric.tool.io.OutputGeometry;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;
import java.util.List;
import java.util.prefs.Preferences;
import java.util.prefs.BackingStoreException;

/**
 * This class writes files in GDS format.
 */
public class OutputGDS extends OutputGeometry
{

	private static final int GDSVERSION        =    3;
	private static final int BYTEMASK          = 0xFF;
	private static final int DSIZE             =  512;		/* data block */
	private static final int MAXPOINTS         =  510;		/* maximum points in a polygon */
	private static final int EXPORTPRESENTATION=    0;		/* centered (was 8 for bottomleft) */

	/* GDSII bit assignments in STRANS record */
	private static final int STRANS_REFLX     =0x8000;
	private static final int STRANS_ABSA      =   0x2;

	/* data type codes */
	private static final int DTYP_NONE         =    0;

	/* header codes */
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
	private static final short HDR_AREF        = 0x0B00;
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

	/* Header byte counts */
	private static final short HDR_N_BGNLIB      =   28;
	private static final short HDR_N_UNITS       =   20;
	private static final short HDR_N_ANGLE       =   12;
	private static final short HDR_N_MAG          =  12;

	/* Maximum string sizes  */
	private static final int HDR_M_SNAME       =   32;
	private static final int HDR_M_STRNAME     =   32;
	private static final int HDR_M_ASCII       =  256;

	/* contour gathering thresholds for polygon accumulation */
	private static final double BESTTHRESH         =  0.001;			/* 1/1000 of a millimeter */
	private static final double WORSTTHRESH        =  0.1;			/* 1/10 of a millimeter */

	/** for buffering output data */			private static byte [] dataBufferGDS = new byte[DSIZE];
	/** for buffering output data */			private static byte [] emptyBuffer = new byte[DSIZE];
	/* Current layer for gds output */			private static int [] currentLayerNumber;
	/* Position of next byte in the buffer */	private static int bufferPosition;					
	/* Number data buffers output so far */		private static int blockCount;				
	/* constant for GDS units */				private static double scaleFactor;				
    /** cell naming map */						private HashMap cellNames;
    /** layer number map */						private HashMap layerNumbers;
//	static int  io_gds_exportlayer;			/* layer for export text */

	/****************************** USING OutputGeometry ******************************/

	public static boolean writeGDSFile(Cell cell, String filePath)
	{
		boolean error = false;
		OutputGDS out = new OutputGDS();
		if (out.openBinaryOutputStream(filePath)) error = true;
		BloatVisitor visitor = out.makeBloatVisitor(getMaxHierDepth(cell));
		if (out.writeCell(cell, visitor)) error = true;
		if (out.closeBinaryOutputStream()) error = true;
		if (!error) System.out.println(filePath + " written");
		return error;
	}

	private BloatVisitor makeBloatVisitor(int maxDepth)
	{
		BloatVisitor visitor = new BloatVisitor(this, maxDepth);
		return visitor;
	}

	/**
	 * Class to override the OutputGeometry visitor and add bloating to all polygons.
	 * Currently, no bloating is being done.
	 */
    private class BloatVisitor extends OutputGeometry.Visitor
    {
		BloatVisitor(OutputGeometry outGeom, int maxHierDepth)
		{
			super(outGeom, maxHierDepth);
		}

		public void addNodeInst(NodeInst ni, AffineTransform trans)
		{
			PrimitiveNode prim = (PrimitiveNode)ni.getProto();
			Technology tech = prim.getTechnology();
			Poly [] polys = tech.getShapeOfNode(ni, null);
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
					if (firstLayer != null) writeLayer(firstLayer);
					outputHeader(HDR_LAYER, currentLayerNumber[0]);
					outputHeader(HDR_TEXTTYPE, 0);
					outputHeader(HDR_PRESENTATION, EXPORTPRESENTATION);

					// now the orientation
					int transvalue = 0;
					int angle = ni.getAngle();
//					if (subno->transpose != 0)
//					{
//						// Angles are reversed 
//						transvalue |= STRANS_REFLX;
//						angle = (3600 - angle)%3600;
//					}
					outputHeader(HDR_STRANS, transvalue);
					outputAngle(angle);
					outputShort((short)12);
					outputShort(HDR_XY);
					Point2D [] points = poly.getPoints();
					outputInt((int)(scaleFactor*points[0].getX()));
					outputInt((int)(scaleFactor*points[0].getY()));

					// now the string
					String str = poly.getString();
					int j = str.length();
					if (j > 512) j = 512;
					outputShort((short)(4+j));
					outputShort(HDR_STRING);
					outputString(str, j);
					outputHeader(HDR_ENDEL, 0);
				}
				poly.transform(trans);
			}
			cellGeom.addPolys(polys);
		}

		public void addArcInst(ArcInst ai)
		{
			ArcProto ap = ai.getProto();
			Technology tech = ap.getTechnology();
			Poly [] polys = tech.getShapeOfArc(ai, null);
			cellGeom.addPolys(polys);
		}
	}

    /** Creates a new instance of OutputGDS */
    OutputGDS()
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
		outputBeginStruct(cell);
        
        // write all polys by Layer
        Set layers = cellGeom.polyMap.keySet();
        for (Iterator it = layers.iterator(); it.hasNext();)
		{
            Layer layer = (Layer)it.next();
            writeLayer(layer);
            List polyList = (List)cellGeom.polyMap.get(layer);
            for (Iterator polyIt = polyList.iterator(); polyIt.hasNext(); )
			{
                Poly poly = (Poly)polyIt.next();
                writePoly(poly);
            }
        }
 
		// write all instances
        for (Iterator noIt = cellGeom.nodables.iterator(); noIt.hasNext(); )
		{
            Nodable no = (Nodable)noIt.next();
            writeNodable(no);
        }

//		// see if there are any outline nodes (opened polygon or circle arcs)
//		boolean gatherpolygon = false;
//		for(Iterator it = cell.getNodes(); it.hasNext(); )
//		{
//			NodeInst subno = (NodeInst)it.next();
//			subno.clearBit(nodeMark);
//			if (subno.getProto() == Artwork.tech.openedPolygonNode || subno.getProto() == Artwork.tech.circleNode ||
//				subno.getProto() == Artwork.tech.thickCircleNode)
//					gatherpolygon = true;
//		}
//		if (gatherpolygon)
//		{
//			bestthresh = scalefromdispunit((float)BESTTHRESH, DISPUNITMM);
//			worstthresh = scalefromdispunit((float)WORSTTHRESH, DISPUNITMM);
//			contourlist = gathercontours(cell, 0, bestthresh, worstthresh);
//
//			// write the contour polygons
//			for(con = contourlist; con != NOCONTOUR; con = con->nextcontour)
//			{
//				poly->count = 0;
//				poly->style = OPENED;
//				poly->layer = 0;
//
//				// add in first point of contour
//				if (poly->count+1 >= poly->limit)
//					(void)extendpolygon(poly, poly->count+1);
//				poly->xv[poly->count] = con->firstcontourelement->sx;
//				poly->yv[poly->count] = con->firstcontourelement->sy;
//				poly->count++;
//				layeroverride = -1;
//				for(conel = con->firstcontourelement; conel != NOCONTOURELEMENT; conel = conel->nextcontourelement)
//				{
//					conel->ni->temp1 = 1;
//					switch (conel->elementtype)
//					{
//						case ARCSEGMENTTYPE:
//						case REVARCSEGMENTTYPE:
//						case CIRCLESEGMENTTYPE:
//							initcontoursegmentgeneration(conel);
//							for(;;)
//							{
//								if (nextcontoursegmentgeneration(&fx, &fy, &tx, &ty)) break;
//								if (poly->count+1 >= poly->limit)
//									(void)extendpolygon(poly, poly->count+1);
//								poly->xv[poly->count] = tx;
//								poly->yv[poly->count] = ty;
//								poly->count++;
//							}
//							break;
//						case LINESEGMENTTYPE:
//							if (poly->count+1 >= poly->limit)
//								(void)extendpolygon(poly, poly->count+1);
//							poly->xv[poly->count] = conel->ex;
//							poly->yv[poly->count] = conel->ey;
//							poly->count++;
//							break;
//						default:
//							break;
//					}
//				}
//				io_outputgdspoly(art_tech, poly, 0, offx, offy, el_matid, layeroverride);
//			}
//		}
		outputHeader(HDR_ENDSTR, 0);
   }

	protected void writeLayer(Layer layer)
	{
		int [] numbers = (int [])layerNumbers.get(layer);
		if (numbers == null)
		{
			String layerName = layer.getGDSLayer();
			numbers = parseLayerString(layerName);
			layerNumbers.put(layer, numbers);
		}
		currentLayerNumber = numbers;
    }

    protected void writePoly(Poly poly)
	{
		if (poly.getStyle() == Poly.Type.DISC)
		{
			// Make a square of the size of the diameter 
//			xformpoly(poly, trans);
//			r = computedistance(poly->xv[0], poly->yv[0], poly->xv[1], poly->yv[1]);
//			if (r <= 0) break;
//			xform(poly->xv[0], poly->yv[0], &xpos, &ypos, trans);
//			xl = xpos - offx - (r + bloat/2);
//			xh = xl + 2*r + bloat;
//			yl = ypos - offy - (r + bloat/2);
//			yh = yl + 2*r + bloat;
//			makerectpoly(xl, xh, yl, yh, poly2);
//			outputBoundary(poly2);
			return;
		}

		Rectangle2D polyBounds = poly.getBox();
		if (polyBounds != null)
		{
			// rectangular manhattan shape: make sure it has positive area
			if (polyBounds.getWidth() == 0 || polyBounds.getHeight() == 0) return;

			outputBoundary(poly);
			return;
		}

		// non-manhattan or worse .. direct output
		Point2D [] points = poly.getPoints();
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
		if (points.length == 2) outputPath(poly); else
			outputBoundary(poly);
    }

    protected void writeNodable(Nodable no)
	{
        NodeProto np = no.getProto();
		NodeInst ni = (NodeInst)no; // In layout cell all Nodables are NodeInsts
		Cell subCell = (Cell)np;

		// write a call to a cell
		// now origin, normal placement
//		Rectangle2D niBounds = ni.getBounds();
//		Rectangle2D cellBounds = subCell.getBounds();
//		AffineTransform trans = ni.rotateOut();
//		Point2D pt = new Point2D.Double(niBounds.getMinX() - cellBounds.getMinX(), niBounds.getMinY() - cellBounds.getMinY());
//		trans.transform(pt, pt);
		Point2D pt = ni.getGrabCenter();

		// Generate a symbol reference 
		outputHeader(HDR_SREF, 0);
		String name = (String)cellNames.get(subCell);
		outputName(HDR_SNAME, name, HDR_M_SNAME);
		int transvalue = 0;
		if (ni.isXMirrored() != ni.isYMirrored()) transvalue |= STRANS_REFLX;

//System.out.println("instance at ("+pt.getX()+","+pt.getY()+"), angle "+angle+", trans "+transvalue);
		// always output the angle and transvalue
		outputHeader(HDR_STRANS, transvalue);
		outputAngle(ni.getAngle());
		outputShort((short)12);
		outputShort(HDR_XY);
		outputInt((int)(scaleFactor*pt.getX()));
		outputInt((int)(scaleFactor*pt.getY()));
		outputHeader(HDR_ENDEL, 0);
    }

	/*************************** GDS OUTPUT ROUTINES ***************************/

	// Initialize various fields, get some standard values
	private void initOutput()
	{
		blockCount = 0;
		bufferPosition = 0;

		// all zeroes
		for (int i=0; i<DSIZE; i++) emptyBuffer[i] = 0;

		scaleFactor = Technology.getCurrent().getScale();
		layerNumbers = new HashMap();

		// make a hashmap of all names to use for cells
        cellNames = new HashMap();
		for(Iterator it = Library.getCurrent().getCells(); it.hasNext(); )
		{
			Cell cell = (Cell)it.next();
			cellNames.put(cell, makeUniqueName(cell));
		}
		for(Iterator it = Library.getLibraries(); it.hasNext(); )
		{
			Library lib = (Library)it.next();
			if (lib == Library.getCurrent()) continue;
			if (lib.isHidden()) continue;
			for(Iterator cIt = lib.getCells(); cIt.hasNext(); )
			{
				Cell cell = (Cell)cIt.next();
				cellNames.put(cell, makeUniqueName(cell));
			}
		}
	}

	String makeUniqueName(Cell cell)
	{
		String name = makeGDSName(cell.getProtoName());
		if (cell.getNewestVersion() != cell)
			name += "_" + cell.getVersion();

		// see if the name is unique
		String baseName = name;
		for(int index = 1; ; index++)
		{
			boolean found = false;
			for(Iterator it = cellNames.values().iterator(); it.hasNext(); )
			{
				String str = (String)it.next();
				if (str.equals(name)) { found = true;   break; }
			}
			if (!found) break;
			name = baseName + "_" + index;
		}

		// add this name to the list
		cellNames.put(cell, name);
		return name;
	}

	/*
	 * function to create proper GDSII names with restricted character set
	 * from input string str.
	 * Uses only 'A'-'Z', '_', $, ?, and '0'-'9'
	 */
	private String makeGDSName(String str)
	{
		// filter the name string for the GDS output cell
		String ret = "";
		for(int k=0; k<str.length(); k++)
		{
			char ch = str.charAt(k);
			if (isUpperCase()) ch = Character.toUpperCase(ch);
			if (ch != '$' && !Character.isDigit(ch) && ch != '?' && !Character.isLetter(ch))
				ch = '_';
			ret += ch;
		}
		return ret;
	}

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
		/* GDS floating point values - -
		 * 0x3E418937,0x4BC6A7EF = 0.001
		 * 0x3944B82F,0xA09B5A53 = 1e-9
		 * 0x3F28F5C2,0x8F5C28F6 = 0.01
		 * 0x3A2AF31D,0xC4611874 = 1e-8
		 */

		// set units
		int [] units01 = convertFloatToArray(1e-3);
		int [] units23 = convertFloatToArray(1.0e-9);

		outputHeader(HDR_HEADER, GDSVERSION);
		outputHeader(HDR_BGNLIB, 0);
		outputDate(cell.getCreationDate());
		outputDate(cell.getRevisionDate());
		outputName(HDR_LIBNAME, makeGDSName(cell.getProtoName()), HDR_M_ASCII);
		outputShort(HDR_N_UNITS);
		outputShort(HDR_UNITS);
		outputIntArray(units01, 2);
		outputIntArray(units23, 2);
	}

	void outputBeginStruct(Cell cell)
	{
		outputHeader(HDR_BGNSTR, 0);
		outputDate(cell.getCreationDate());
		outputDate(cell.getRevisionDate());

		String name = (String)cellNames.get(cell);
		outputName(HDR_STRNAME, name, HDR_M_STRNAME);
	}

	// Output date information
	private void outputDate(Date val)
	{
		short [] date = new short[6];

		Calendar cal = Calendar.getInstance();
		cal.setTime(val);
		date[0] = (short)cal.get(Calendar.YEAR);
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
	private void outputName(int header, String p1, int max)
	{
		int count = Math.min(p1.length(), max);
		if ((count&1) != 0) count++;
		outputShort((short)(count+4));
		outputShort((short)header);
		outputString(p1, count);
	}

	// Output an angle as part of a STRANS
	private void outputAngle(int ang)
	{
		double gdfloat = ang / 10.0;
		outputShort(HDR_N_ANGLE);
		outputShort(HDR_ANGLE);
		int [] units = convertFloatToArray(gdfloat);
		outputIntArray(units, 2);
	}

	// Output a magnification as part of a STRANS
	private void outputMag(double scale)
	{
		outputShort(HDR_N_MAG);
		outputShort(HDR_MAG);
		int [] units = convertFloatToArray(scale);
		outputIntArray(units, 2);
	}

	// Output the pairs of XY points to the file 
	private void outputBoundary(Poly poly)
	{
		Point2D [] points = poly.getPoints();
		int count = points.length;
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
//			outputBoundary(side1);
//			outputBoundary(side2);
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
			outputHeader(HDR_LAYER, currentLayerNumber[0]);
			outputHeader(HDR_DATATYPE, 0);
			outputShort((short)(8 * (sofar+1) + 4));
			outputShort(HDR_XY);
			for (int i = start; i <= sofar; i++)
			{
				int j = i;
				if (i == sofar) j = 0;
				outputInt((int)(scaleFactor*points[j].getX()));
				outputInt((int)(scaleFactor*points[j].getY()));
			}
			outputHeader(HDR_ENDEL, 0);
			if (sofar >= count) break;
			count -= sofar;
			start = sofar;
		}
	}

	private void outputPath(Poly poly)
	{
		outputHeader(HDR_PATH, 0);
		outputHeader(HDR_LAYER, currentLayerNumber[0]);
		outputHeader(HDR_DATATYPE, 0);
		Point2D [] points = poly.getPoints();
		int count = 8 * points.length + 4;
		outputShort((short)count);
		outputShort(HDR_XY);
		for (int i = 0; i < points.length; i ++)
		{
			outputInt((int)(scaleFactor*points[i].getX()));
			outputInt((int)(scaleFactor*points[i].getY()));
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

	/*
	 * String of n bytes, starting at ptr
	 * Revised 90-11-23 to convert to upper case (SRP)
	 */
	private void outputString(String ptr, int n)
	{
		int i = 0;
		if (isUpperCase())
		{
			// convert to upper case
			for( ; i<ptr.length(); i++)
				outputByte((byte)Character.toUpperCase(ptr.charAt(i)));
		} else
		{
			for( ; i<ptr.length(); i++)
				outputByte((byte)ptr.charAt(i));
		}
		for ( ; i < n; i++)
			outputByte((byte)0);
	}

	/*
	 * Create 8-byte GDS representation of a floating point number
	 */
	private int [] convertFloatToArray(double a)
	{
		int [] ret = new int[2];

		// handle default
		if (a == 0)
		{
			ret[0] = 0x40000000;
			ret[1] = 0;
			return ret;
		}

		// identify sign
		double temp = a;
		boolean negsign = false;
		if (temp < 0)
		{
			negsign = true;
			temp = -temp;
		}

		// establish the excess-64 exponent value
		int exponent = 64;

		// scale the exponent and mantissa
		for (; temp < 0.0625 && exponent > 0; exponent--) temp *= 16.0;

		if (exponent == 0) System.out.println("Exponent underflow");

		for (; temp >= 1 && exponent < 128; exponent++) temp /= 16.0;

		if (exponent > 127) System.out.println("Exponent overflow");

		// set the sign
		if (negsign) exponent |= 0x80;

		// convert temp to 7-byte binary integer
		double top = temp;
		for (int i = 0; i < 24; i++) top *= 2;
		int highmantissa = (int)top;
		double frac = top - highmantissa;
		for (int i = 0; i < 32; i++) frac *= 2;
		ret[0] = highmantissa | (exponent<<24);
		ret[1] = (int)frac;
		return ret;
	}

	/**
	 * Method to parse the GDS layer string and get the 3 layer numbers (plain, text, and pin).
	 * @param string the GDS layer string, of the form NUM[,NUMt][,NUMp]
	 * @return an array of 3 integers:
	 * [0] is the regular layer number;
	 * [1] is the pin layer number;
	 * [2] is the text layer number.
	 */
	public static int [] parseLayerString(String string)
	{
		int [] answers = new int[3];
		answers[0] = answers[1] = answers[2] = -1;
		for(;;)
		{
			String trimmed = string.trim();
			if (trimmed.length() == 0) break;
			int number = TextUtils.atoi(trimmed);
			int endPos = trimmed.indexOf(',');
			if (endPos < 0) endPos = trimmed.length();
			char lastch = trimmed.charAt(endPos-1);
			if (lastch == 't')
			{
				answers[2] = number;
			} else if (lastch == 'p')
			{
				answers[1] = number;
			} else
			{
				answers[0] = number;
			}
			if (endPos == trimmed.length()) break;
			string = trimmed.substring(endPos+1);
		}
		return answers;
	}

	/****************************** GDS OUTPUT PREFERENCES ******************************/

	/**
	 * Method to tell whether GDS Output merges boxes into complex polygons.
	 * This takes more time but produces a smaller output file.
	 * The default is "false".
	 * @return true if GDS Output merges boxes into complex polygons.
	 */
	private static boolean cacheMergesBoxes = tool.prefs.getBoolean("GDSMergesBoxes", false);
	public static boolean isMergesBoxes() { return cacheMergesBoxes; }
	/**
	 * Method to set whether GDS Output merges boxes into complex polygons.
	 * This takes more time but produces a smaller output file.
	 * @param on true if nGDS Output merges boxes into complex polygons.
	 */
	public static void setMergesBoxes(boolean on)
	{
		tool.prefs.putBoolean("GDSMergesBoxes", cacheMergesBoxes = on);
		flushOptions();
	}

	/**
	 * Method to tell whether GDS Output writes pins at Export locations.
	 * Some systems can use this information to reconstruct export locations.
	 * The default is "false".
	 * @return true if GDS Output writes pins at Export locations.
	 */
	private static boolean cacheWritesExportPins = tool.prefs.getBoolean("GDSWritesExportPins", false);
	public static boolean isWritesExportPins() { return cacheWritesExportPins; }
	/**
	 * Method to set whether GDS Output writes pins at Export locations.
	 * Some systems can use this information to reconstruct export locations.
	 * @param on true if nGDS Output writes pins at Export locations.
	 */
	public static void setWritesExportPins(boolean on)
	{
		tool.prefs.putBoolean("GDSWritesExportPins", cacheWritesExportPins = on);
		flushOptions();
	}

	/**
	 * Method to tell whether GDS Output makes all text upper-case.
	 * Some systems insist on this.
	 * The default is "false".
	 * @return true if GDS Output makes all text upper-case.
	 */
	private static boolean cacheUpperCase = tool.prefs.getBoolean("GDSOutputUpperCase", false);
	public static boolean isUpperCase() { return cacheUpperCase; }
	/**
	 * Method to set whether GDS Output makes all text upper-case.
	 * Some systems insist on this.
	 * @param on true if nGDS Output makes all text upper-case.
	 */
	public static void setUpperCase(boolean on)
	{
		tool.prefs.putBoolean("GDSOutputUpperCase", cacheUpperCase = on);
		flushOptions();
	}
}
